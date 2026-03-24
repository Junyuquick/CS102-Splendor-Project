package network;

import config.Config;
import config.ConfigSupport;
import engine.Move;
import engine.MoveExecutor;
import engine.MoveValidator;
import engine.NobleAssigner;
import engine.TurnManager;
import engine.WinnerChecker;
import io.CardLoader;
import io.NobleLoader;
import model.Board;
import model.Cost;
import model.DevelopmentCard;
import model.GameState;
import model.GemBank;
import model.GemColor;
import model.NobleTile;
import model.Player;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            int playerCount = players.size();
            GemBank bank = createBank(playerCount);
            Board board = createBoard(playerCount, bank);
            gameState = new GameState(players, board, bank);
        } catch (IOException e) {
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

    private Board createBoard(int playerCount, GemBank bank) throws IOException {
        Map<Integer, List<DevelopmentCard>> decks = buildDecks(config);
        List<NobleTile> nobles = buildNobles(playerCount);

        Map<GemColor, Integer> initialGems = new EnumMap<>(GemColor.class);
        for (GemColor color : GemColor.values()) {
            int amount = color == GemColor.GOLD
                    ? config.getInitialGoldGemCount(playerCount)
                    : config.getInitialNormalGemCount(playerCount);
            initialGems.put(color, amount);
        }

        return new Board(decks, nobles, initialGems, bank, config.getOpenCardsPerLevel());
    }

    private GemBank createBank(int playerCount) {
        GemBank bank = new GemBank();
        for (GemColor color : GemColor.values()) {
            Map<GemColor, Integer> delta = new EnumMap<>(GemColor.class);
            int amount = color == GemColor.GOLD
                    ? config.getInitialGoldGemCount(playerCount)
                    : config.getInitialNormalGemCount(playerCount);
            delta.put(color, amount);
            bank.addTokens(delta);
        }
        return bank;
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
        resolveTokenCapIfNeeded(currentPlayer);

        // Handle noble assignment
        assignConfiguredNobles(currentPlayer);

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

    private static Map<Integer, List<DevelopmentCard>> buildDecks(Config config) {
        try {
            return new CardLoader().load(
                    config.getCardsPath(1),
                    config.getCardsPath(2),
                    config.getCardsPath(3)
            );
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Failed to load cards from configured CSV paths. Falling back to sample deck. Reason: " + e.getMessage());
        }

        Map<Integer, List<DevelopmentCard>> decks = new HashMap<>();

        decks.put(1, shuffled(List.of(
                card(1, 0, GemColor.WHITE, mapCost(0, 1, 1, 1, 1)),
                card(1, 0, GemColor.BLUE, mapCost(1, 0, 1, 1, 1)),
                card(1, 0, GemColor.GREEN, mapCost(1, 1, 0, 1, 1)),
                card(1, 0, GemColor.RED, mapCost(1, 1, 1, 0, 1)),
                card(1, 0, GemColor.BLACK, mapCost(1, 1, 1, 1, 0)),
                card(1, 1, GemColor.WHITE, mapCost(0, 0, 2, 2, 0)),
                card(1, 1, GemColor.BLUE, mapCost(2, 0, 0, 2, 0)),
                card(1, 1, GemColor.GREEN, mapCost(2, 2, 0, 0, 0))
        )));

        decks.put(2, shuffled(List.of(
                card(2, 1, GemColor.WHITE, mapCost(0, 2, 2, 2, 0)),
                card(2, 1, GemColor.BLUE, mapCost(0, 0, 3, 2, 2)),
                card(2, 1, GemColor.GREEN, mapCost(2, 0, 0, 3, 2)),
                card(2, 2, GemColor.RED, mapCost(0, 3, 0, 2, 3)),
                card(2, 2, GemColor.BLACK, mapCost(3, 2, 0, 0, 3)),
                card(2, 2, GemColor.WHITE, mapCost(3, 0, 3, 2, 0))
        )));

        decks.put(3, shuffled(List.of(
                card(3, 3, GemColor.WHITE, mapCost(0, 3, 3, 5, 3)),
                card(3, 3, GemColor.BLUE, mapCost(3, 0, 3, 3, 5)),
                card(3, 4, GemColor.GREEN, mapCost(3, 3, 0, 3, 6)),
                card(3, 4, GemColor.RED, mapCost(6, 3, 3, 0, 3)),
                card(3, 5, GemColor.BLACK, mapCost(3, 6, 3, 3, 0))
        )));

        return decks;
    }

    private List<NobleTile> buildNobles(int playerCount) {
        Path csv = config.getNoblesPath();
        try {
            List<NobleTile> nobles = new NobleLoader().load(csv);
            Collections.shuffle(nobles);
            int nobleCount = Math.min(config.getNoblesCount(playerCount), nobles.size());
            return new ArrayList<>(nobles.subList(0, nobleCount));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Failed to load nobles from " + csv + ". Falling back to sample nobles. Reason: " + e.getMessage());
        }

        List<NobleTile> fallback = new ArrayList<>(List.of(
                noble(1, 3, mapCost(3, 3, 3, 0, 0)),
                noble(2, 3, mapCost(0, 3, 3, 3, 0)),
                noble(3, 3, mapCost(0, 0, 3, 3, 3)),
                noble(4, 3, mapCost(3, 0, 0, 3, 3))
        ));
        Collections.shuffle(fallback);
        int nobleCount = Math.min(config.getNoblesCount(playerCount), fallback.size());
        return new ArrayList<>(fallback.subList(0, nobleCount));
    }

    private static DevelopmentCard card(int level, int points, GemColor bonus, Map<GemColor, Integer> costs) {
        Cost cost = new Cost();
        for (Map.Entry<GemColor, Integer> entry : costs.entrySet()) {
            cost.set(entry.getKey(), entry.getValue());
        }
        return new DevelopmentCard(0, level, points, bonus, cost);
    }

    private static NobleTile noble(int id, int points, Map<GemColor, Integer> reqs) {
        Cost cost = new Cost();
        for (Map.Entry<GemColor, Integer> entry : reqs.entrySet()) {
            cost.set(entry.getKey(), entry.getValue());
        }
        return new NobleTile(id, points, cost);
    }

    private static Map<GemColor, Integer> mapCost(int w, int b, int g, int r, int k) {
        Map<GemColor, Integer> m = new EnumMap<>(GemColor.class);
        if (w > 0) m.put(GemColor.WHITE, w);
        if (b > 0) m.put(GemColor.BLUE, b);
        if (g > 0) m.put(GemColor.GREEN, g);
        if (r > 0) m.put(GemColor.RED, r);
        if (k > 0) m.put(GemColor.BLACK, k);
        return m;
    }

    private static List<DevelopmentCard> shuffled(List<DevelopmentCard> cards) {
        List<DevelopmentCard> copy = new ArrayList<>(cards);
        Collections.shuffle(copy);
        return copy;
    }

    private void resolveTokenCapIfNeeded(Player player) {
        int maxTokens = config.getMaxTokensPerPlayer();
        while (player.getTotalTokens() > maxTokens) {
            int excess = player.getTotalTokens() - maxTokens;
            Map<GemColor, Integer> discard = autoDiscard(player, excess);
            player.removeTokens(discard);
            gameState.getBank().addTokens(discard);
        }
    }

    private Map<GemColor, Integer> autoDiscard(Player player, int excess) {
        Map<GemColor, Integer> discard = new EnumMap<>(GemColor.class);
        Map<GemColor, Integer> working = new EnumMap<>(GemColor.class);
        working.putAll(player.getTokens());
        while (excess > 0) {
            GemColor candidate = null;
            int max = 0;
            for (Map.Entry<GemColor, Integer> entry : working.entrySet()) {
                if (entry.getValue() > max) {
                    max = entry.getValue();
                    candidate = entry.getKey();
                }
            }
            if (candidate == null || max == 0) {
                break;
            }
            discard.put(candidate, discard.getOrDefault(candidate, 0) + 1);
            working.put(candidate, max - 1);
            excess--;
        }
        return discard;
    }

    private void assignConfiguredNobles(Player player) {
        List<NobleTile> eligibleNobles = new ArrayList<>(nobleAssigner.findEligibleNobles(gameState, player));
        int noblesThisTurn = Math.min(config.getMaxNoblesPerTurn(), eligibleNobles.size());
        for (int i = 0; i < noblesThisTurn; i++) {
            NobleTile chosen = chooseBestNoble(eligibleNobles);
            nobleAssigner.assignNoble(gameState, player, chosen);
            eligibleNobles.remove(chosen);
        }
    }

    private NobleTile chooseBestNoble(List<NobleTile> eligibleNobles) {
        return eligibleNobles.stream()
                .max((a, b) -> {
                    int points = Integer.compare(a.getPrestigePoints(), b.getPrestigePoints());
                    if (points != 0) {
                        return points;
                    }
                    int aReq = a.getRequirement().asMap().values().stream().mapToInt(Integer::intValue).sum();
                    int bReq = b.getRequirement().asMap().values().stream().mapToInt(Integer::intValue).sum();
                    return Integer.compare(bReq, aReq);
                })
                .orElse(eligibleNobles.get(0));
    }
}
