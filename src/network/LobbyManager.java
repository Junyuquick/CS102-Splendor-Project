package network;

import config.Config;
import model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the current multiplayer lobby roster and the helper logic that
 * goes with it.
 *
 * This keeps player-list bookkeeping out of GameServer so the server
 * can focus on connection flow and turn handling.
 */
public class LobbyManager {
    private final Config config;
    private final List<ClientHandler> clients = new ArrayList<>();

    /**
     * Creates a lobby manager backed by the supplied configuration.
     *
     * @param config game configuration used for lobby size rules
     */
    public LobbyManager(Config config) {
        this.config = config;
    }

    /**
     * Returns true when another client can still join the lobby.
     *
     * @return true if the lobby is below the configured maximum size
     */
    public boolean canAcceptMoreClients() {
        return clients.size() < config.getMaxPlayers();
    }

    /**
     * Adds a newly accepted client to the lobby.
     *
     * @param client client to add
     */
    public void addClient(ClientHandler client) {
        clients.add(client);
    }

    /**
     * Removes a client from the lobby.
     *
     * @param client client to remove
     */
    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    /**
     * Returns the current number of clients in the lobby.
     *
     * @return connected client count
     */
    public int getClientCount() {
        return clients.size();
    }

    /**
     * Returns true when the supplied client is the host.
     *
     * The first connected client is treated as the host for start-game
     * requests.
     *
     * @param client client to check
     * @return true if the client is the host
     */
    public boolean isHost(ClientHandler client) {
        return !clients.isEmpty() && clients.get(0) == client;
    }

    /**
     * Returns true when the lobby has enough players to begin a game.
     *
     * @return true if the minimum player requirement is met
     */
    public boolean hasMinimumPlayers() {
        return clients.size() >= config.getMinPlayers();
    }

    /**
     * Returns the lobby slot of the supplied client.
     *
     * @param client client to locate
     * @return zero-based lobby index, or -1 if missing
     */
    public int indexOf(ClientHandler client) {
        return clients.indexOf(client);
    }

    /**
     * Assigns each client its player index for the game that is about
     * to start.
     */
    public void assignPlayerIndexes() {
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).setPlayerIndex(i);
        }
    }

    /**
     * Builds the player objects used to create a new GameState.
     *
     * If a client does not yet have a usable name, a readable fallback
     * is generated so the game can still start.
     *
     * @return players in lobby order
     */
    public List<Player> buildPlayers() {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < clients.size(); i++) {
            String name = clients.get(i).getPlayerName();
            if (name == null || name.isBlank()) {
                name = "Player " + (i + 1);
            }
            players.add(new Player(name));
        }
        return players;
    }

    /**
     * Returns the names that should be shown in the lobby UI.
     *
     * @return player names in lobby order
     */
    public List<String> buildLobbyPlayerNames() {
        List<String> playerNames = new ArrayList<>();
        for (int i = 0; i < clients.size(); i++) {
            String name = clients.get(i).getPlayerName();
            if (name == null || name.isBlank()) {
                name = "Player " + (i + 1);
            }
            playerNames.add(name);
        }
        return playerNames;
    }

    /**
     * Returns a snapshot of the current client list.
     *
     * @return copy of the connected clients
     */
    public List<ClientHandler> getClients() {
        return new ArrayList<>(clients);
    }
}
