package network;

import config.Config;
import config.ConfigSupport;
import engine.Move;
import engine.MoveExecutor;
import engine.MoveValidator;
import engine.NobleAssigner;
import engine.TurnPostProcessor;
import engine.TurnProgressionService;
import engine.TurnManager;
import engine.TurnAdvanceResult;
import engine.WinnerChecker;
import model.GameState;
import model.Player;
import setup.GameStateFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server for multiplayer Splendor games.
 * Manages game state, client connections, and game flow.
 */
public class GameServer {
    private final Config config = buildConfig();
    private final int port;
    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new ArrayList<>();
    private GameState gameState;
    private boolean gameStarted = false;
    private final MoveValidator validator = new MoveValidator(config);
    private final MoveExecutor executor = new MoveExecutor(config);
    private final NobleAssigner nobleAssigner = new NobleAssigner();
    private final TurnPostProcessor turnPostProcessor = new TurnPostProcessor(config, nobleAssigner);
    private final WinnerChecker winnerChecker = new WinnerChecker(config);
    private final TurnManager turnManager = new TurnManager();
    private final TurnProgressionService turnProgressionService = new TurnProgressionService();

    /**
     * Creates a server that listens on the supplied port.
     *
     * @param port TCP port to bind
     */
    public GameServer(int port) {
        this.port = port;
    }

    private static Config buildConfig() {
        return ConfigSupport.loadDefaultConfig();
    }

    /**
     * Starts accepting client connections until a game begins or the socket is closed.
     *
     * @throws IOException if the server socket cannot be opened or accepted from
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);
        System.out.println("Waiting for players to join...");

        ExecutorService executor = Executors.newCachedThreadPool();
        while (!gameStarted) {
            Socket clientSocket = serverSocket.accept();
            if (clients.size() >= config.getMaxPlayers()) {
                rejectConnection(clientSocket, "Lobby is full (max " + config.getMaxPlayers() + " players)");
                continue;
            }
            ClientHandler handler = new ClientHandler(clientSocket, this);
            clients.add(handler);
            executor.submit(handler);
            System.out.println("Player joined. Total players: " + clients.size());
        }
    }

    /**
     * Builds and broadcasts a fresh game state for the current lobby.
     */
    private void startGame() {
        System.out.println("Starting game with " + clients.size() + " players");

        List<Player> players = new ArrayList<>();
        for (int i = 0; i < clients.size(); i++) {
            String name = clients.get(i).getPlayerName();
            if (name == null) name = "Player " + (i + 1);
            players.add(new Player(name));
        }

        try {
            gameState = new GameStateFactory(config, GameStateFactory.FallbackProfile.SERVER)
                    .createGame(players, message -> System.err.println(message));
        } catch (RuntimeException e) {
            System.err.println("Failed to initialize game: " + e.getMessage());
            return;
        }

        gameStarted = true;

        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).setPlayerIndex(i);
            clients.get(i).sendMessage(NetworkMessage.gameStart(gameState, i));
        }

        System.out.println("Game started!");
    }

    /**
     * Handles a host request to start the current lobby as a game.
     *
     * @param client client that sent the request
     */
    public synchronized void handleStartRequest(ClientHandler client) {
        if (gameStarted) {
            client.sendMessage(NetworkMessage.error("Game already started"));
            return;
        }

        if (clients.isEmpty() || clients.get(0) != client) {
            client.sendMessage(NetworkMessage.error("Only host can start the game"));
            return;
        }

        if (clients.size() < config.getMinPlayers()) {
            client.sendMessage(NetworkMessage.error("Need at least " + config.getMinPlayers() + " players to start"));
            broadcastLobbyState();
            return;
        }

        System.out.println("Host requested start. Starting game...");
        startGame();
    }

    /**
     * Finalizes a newly joined client and broadcasts the updated lobby state.
     *
     * @param client client that completed the join handshake
     */
    public synchronized void handleJoin(ClientHandler client) {
        int lobbyIndex = clients.indexOf(client);
        if (lobbyIndex < 0) {
            return;
        }
        client.sendMessage(NetworkMessage.joinAck(lobbyIndex));
        broadcastLobbyState();
    }

    /**
     * Validates and applies a move sent by one of the connected clients.
     *
     * @param client client that sent the move
     * @param move requested move
     */
    public synchronized void handleMove(ClientHandler client, Move move) {
        if (!gameStarted || gameState == null) {
            client.sendMessage(NetworkMessage.error("Game not started"));
            return;
        }

        int playerIndex = client.getPlayerIndex();
        if (playerIndex != gameState.getCurrentPlayerIndex()) {
            client.sendMessage(NetworkMessage.error("Not your turn"));
            return;
        }

        Player currentPlayer = gameState.getCurrentPlayer();
        String error = validator.validate(gameState, currentPlayer, move);
        if (error != null) {
            client.sendMessage(NetworkMessage.error(error));
            return;
        }

        executor.execute(gameState, currentPlayer, move);
        turnPostProcessor.enforceTokenLimit(gameState, currentPlayer);
        turnPostProcessor.assignBestAvailableNobles(gameState, currentPlayer);

        TurnAdvanceResult turnResult = turnProgressionService.progressTurn(gameState, winnerChecker, turnManager);
        if (turnResult.getWinner() != null) {
            Player winner = turnResult.getWinner();
            System.out.println("Game over! Winner: " + winner.getName());
            broadcastState();
            broadcastGameOver("Game over! Winner: " + winner.getName() + " (" + winner.getPrestigePoints() + " points)");
            gameStarted = false;
            gameState = null;
            turnManager.resetFinalRound();
            broadcastLobbyState();
            return;
        }

        broadcastState();
    }

    /**
     * Broadcasts the latest game-state snapshot to every connected client.
     */
    private void broadcastState() {
        NetworkMessage msg = NetworkMessage.stateUpdate(gameState);
        for (ClientHandler c : clients) {
            c.sendMessage(msg);
        }
    }

    /**
     * Broadcasts a game-over message to every connected client.
     *
     * @param message human-readable game result
     */
    private void broadcastGameOver(String message) {
        NetworkMessage msg = NetworkMessage.gameOver(message);
        for (ClientHandler client : clients) {
            client.sendMessage(msg);
        }
    }

    /**
     * Removes a disconnected client and updates the lobby if no game is active.
     *
     * @param client disconnected client
     */
    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client disconnected. Remaining: " + clients.size());
        if (!gameStarted) {
            broadcastLobbyState();
        }
        if (clients.isEmpty()) {
            System.out.println("All clients disconnected. Shutting down.");
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Ignore close failures while the server is already shutting down.
            }
        }
    }

    /**
     * Broadcasts the current lobby roster and host information.
     */
    private void broadcastLobbyState() {
        List<String> playerNames = new ArrayList<>();
        for (int i = 0; i < clients.size(); i++) {
            String name = clients.get(i).getPlayerName();
            if (name == null || name.isBlank()) {
                name = "Player " + (i + 1);
            }
            playerNames.add(name);
        }

        NetworkMessage message = NetworkMessage.lobbyUpdate(playerNames, 0, config.getMinPlayers());
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    /**
     * Rejects a connection attempt by sending an error and closing the socket.
     *
     * @param socket socket to reject
     * @param message rejection reason
     */
    private void rejectConnection(Socket socket, String message) {
        try (socket; ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(NetworkMessage.error(message));
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to reject connection cleanly: " + e.getMessage());
        }
    }

    /**
     * Starts a standalone multiplayer server.
     *
     * @param args optional first argument specifying the listening port
     */
    public static void main(String[] args) {
        int port = 12345;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                return;
            }
        }

        GameServer server = new GameServer(port);
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

}
