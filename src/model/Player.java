package model;

import java.io.Serializable;
import java.util.*;

/**
 * Stores the player state.
 * 
 * What it does:
 * - Manages player tokens (gem colors and  gold wildcards)
 * - Tracks purchased development cards (for the prestige points and bonuses )
 * - Tracks reserved development cards by player
 * - Tracks nobles claimed to the player (for prestige points)
 * - Tracks prestige points and bonus points of the player
 */
public class Player implements Serializable {
    
    private String name;
    private Map<GemColor, Integer> tokens;
    private List<DevelopmentCard> purchasedCards;
    private List<DevelopmentCard> reservedCards;
    private List<NobleTile> nobles;
    
    /**
     * Creates a new player.
     * 
     */
    public Player(String name) {
        this.name = name;
        this.tokens = new HashMap<>();
        this.purchasedCards = new ArrayList<>();
        this.reservedCards = new ArrayList<>();
        this.nobles = new ArrayList<>();
    }
    
    /**
     * Returns the player's name.
     * 
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns how many tokens of a color the player currently holds.
     * 
     * @param color token color
     */
    public int getTokenCount(GemColor color) {
        return tokens.getOrDefault(color, 0);
    }
    
    /**
     * Returns an immutable overview of the players tokens by color
     */
    public Map<GemColor, Integer> getTokens() {
        return new HashMap<>(tokens);
    }
    
    /**
     * Increases the player's token counts
     * (Includes wild cards)
     * 
     */
    public void addTokens(Map<GemColor, Integer> delta) {
        for (Map.Entry<GemColor, Integer> entry : delta.entrySet()) {
            GemColor color = entry.getKey();
            int amount = entry.getValue();
            tokens.put(color, tokens.getOrDefault(color, 0) + amount);
        }
    }
    
    /**
     * Decreases the player's token counts
     * (Includes wild cards)
     */
    public void removeTokens(Map<GemColor, Integer> delta) {
        for (Map.Entry<GemColor, Integer> entry : delta.entrySet()) {
            GemColor color = entry.getKey();
            int amount = entry.getValue();
            int newCount = tokens.getOrDefault(color, 0) - amount;
            if (newCount <= 0) {
                tokens.remove(color);
            } else {
                tokens.put(color, newCount);
            }
        }
    }
    
    /**
     * Returns the total number of tokens across all colors, including gold.
     * (Used by the game engine to enforce maximum token count for each player)
     * 
     */
    public int getTotalTokens() {
        int total = 0;
        for (int count : tokens.values()) {
            total += count;
        }
        return total;
    }
    
    /**
     * Adds a purchased DevelopementCard to the players card collection
     * 
     */
    public void addPurchasedCard(DevelopmentCard card) {
        purchasedCards.add(card);
    }
    
    /**
     * Returns an immutable overview of the players purchased cards
     * 
     */
    public List<DevelopmentCard> getPurchasedCards() {
        return Collections.unmodifiableList(purchasedCards);
    }
    
    /**
     * Adds a DevelopementCard to the player's reserved cards.
     * 
     */
    public void addReservedCard(DevelopmentCard card) {
        reservedCards.add(card);
    }
    
    /**
     * Removes a reserved card, usually when the player buys it.
     * 
     * @return true if the card is removed, false otherwise
     */
    public boolean removeReservedCard(DevelopmentCard card) {
        return reservedCards.remove(card);
    }
    
    /**
     * Returns an immutable overview of the player's reserved development cards.
     * 
     */
    public List<DevelopmentCard> getReservedCards() {
        return Collections.unmodifiableList(reservedCards);
    }
    
    /**
     * Adds a noble tile to the player's collection of nobles.
     * 
     * @param noble the noble tile to add
     */
    public void addNoble(NobleTile noble) {
        nobles.add(noble);
    }
    
    /**
     * Returns the list of nobles the player has collected.
     *
     */
    public List<NobleTile> getNobles() {
        return Collections.unmodifiableList(nobles);
    }
    
    /**
     * Returns how many permanent bonus points the player has for a color.
     * Gold is NOT treated as a bonus color.
     * 
     */
    public int getBonusCount(GemColor bonusColor) {
        int count = 0;
        for (DevelopmentCard card : purchasedCards) {
            if (card.getBonusColor() == bonusColor) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Returns the total prestige points earned by this player (From purchased DevelopmentCards and claimed nobles)
     * 
     */
    public int getPrestigePoints() {
        int prestige = 0;
        
        for (DevelopmentCard card : purchasedCards) {
            prestige += card.getPrestigePoints();
        }

        for (NobleTile noble : nobles) {
            prestige += noble.getPrestigePoints();
        }
        
        return prestige;
    }
    
    /**
     * Returns the number of purchased development cards.
     * (Used for tie breaking: When 2 winning players have the same prestige points, the player with lesser development cards wins)
     */
    public int getDevelopmentCardCount() {
        return purchasedCards.size();
    }
}
