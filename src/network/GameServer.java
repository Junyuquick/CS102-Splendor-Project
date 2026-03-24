package network;

import config.Config;
import config.ConfigSupport;
import engine.Move;
import engine.MoveExecutor;
import engine.MoveValidator;
import engine.NobleAssigner;
import engine.TurnPostProcessor;
import engine.TurnManager;
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

    public GameServer(int port) {
        this.port = port;
    }

    private static Config buildConfig() {
        return ConfigSupport.loadDefaultConfig();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);
        System.out.println("Waiting for players to join...");

        // Accept clients until server is shut down
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
            // Do not auto-start. wait for host request.
            // Host is the first connected client.
        }
    }

    private void startGame() {
        System.out.println("Starting game with " + clients.size() + " players");

        // Create players
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < clients.size(); i++) {
            String name = clients.get(i).getPlayerName();
            if (name == null) name = "Player " + (i + 1);
            players.add(new Player(name));
        }

        // Initialize game state
        try {
            gameState = new GameStateFactory(config, GameStateFactory.FallbackProfile.SERVER)
                    .createGame(players, message -> System.err.println(message));
        } catch (RuntimeException e) {
            System.err.println("Failed to initialize game: " + e.getMessage());
            return;
        }

        gameStarted = true;

        // Assign player indices and send game start
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).setPlayerIndex(i);
            clients.get(i).sendMessage(NetworkMessage.gameStart(gameState, i));
        }

        System.out.println("Game started!");
    }

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

    public synchronized void handleJoin(ClientHandler client) {
        int lobbyIndex = clients.indexOf(client);
        if (lobbyIndex < 0) {
            return;
        }
        client.sendMessage(NetworkMessage.joinAck(lobbyIndex));
        broadcastLobbyState();
    }

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

        // Execute the move
        executor.execute(gameState, currentPlayer, move);
        turnPostProcessor.enforceTokenLimit(gameState, currentPlayer);

        // Handle noble assignment
        turnPostProcessor.assignBestAvailableNobles(gameState, currentPlayer);

        // Check for final round
        if (winnerChecker.shouldTriggerFinalRound(gameState)) {
            turnManager.markFinalRound(gameState);
        }

        // Advance turn
        turnManager.advanceTurn(gameState);

        // Check for game over
        if (turnManager.hasFinalRoundCompleted(gameState)) {
            Player winner = winnerChecker.determineWinner(gameState);
            System.out.println("Game over! Winner: " + winner.getName());
            broadcastState();
            broadcastGameOver("Game over! Winner: " + winner.getName() + " (" + winner.getPrestigePoints() + " points)");
            gameStarted = false;
            gameState = null;
            turnManager.resetFinalRound();
            broadcastLobbyState();
            return;
        }

        // Broadcast updated state to all clients
        broadcastState();
    }

    private void broadcastState() {
        NetworkMessage msg = NetworkMessage.stateUpdate(gameState);
        for (ClientHandler c : clients) {
            c.sendMessage(msg);
        }
    }

    private void broadcastGameOver(String message) {
        NetworkMessage msg = NetworkMessage.gameOver(message);
        for (ClientHandler client : clients) {
            client.sendMessage(msg);
        }
    }

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
                // ignore
            }
        }
    }

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

    private void rejectConnection(Socket socket, String message) {
        try (socket; ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(NetworkMessage.error(message));
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to reject connection cleanly: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = 12345; // default port
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
