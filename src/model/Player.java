package model;

import java.io.Serializable;
import java.util.*;

/**
 * Stores all state that belongs to exactly one Splendor player.
 * 
 * Responsibilities:
 * - Manages player tokens (gem colors + gold wildcard)
 * - Tracks purchased development cards (contribute prestige and bonuses)
 * - Tracks reserved development cards
 * - Tracks nobles claimed to the player (contribute prestige)
 * - Computes prestige points and bonus counts per Splendor rules
 */
public class Player implements Serializable {
    
    private String name;
    private Map<GemColor, Integer> tokens;
    private List<DevelopmentCard> purchasedCards;
    private List<DevelopmentCard> reservedCards;
    private List<NobleTile> nobles;
    
    /**
     * Creates a new player with the given name.
     * Initializes all collections as empty.
     * 
     * @param name the player's display name or identifier
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
     * @return the player name
     */
    public String getName() {
        return name;
    }
    
    // ============ TOKEN MANAGEMENT ============
    
    /**
     * Returns how many tokens of the given color the player currently holds.
     * 
     * @param color the gem color to query
     * @return the token count for that color (0 if not present)
     */
    public int getTokenCount(GemColor color) {
        return tokens.getOrDefault(color, 0);
    }
    
    /**
     * Returns a defensive copy of the player's full token counts.
     * Prevents external code from mutating the internal map.
     * 
     * @return a copy of the token map
     */
    public Map<GemColor, Integer> getTokens() {
        return new HashMap<>(tokens);
    }
    
    /**
     * Increases the player's token counts by the provided amounts.
     * Used when the player takes tokens or receives gold from reserve.
     * 
     * @param delta a map of color → count increases to apply
     */
    public void addTokens(Map<GemColor, Integer> delta) {
        for (Map.Entry<GemColor, Integer> entry : delta.entrySet()) {
            GemColor color = entry.getKey();
            int amount = entry.getValue();
            tokens.put(color, tokens.getOrDefault(color, 0) + amount);
        }
    }
    
    /**
     * Decreases the player's token counts by the provided amounts.
     * Used when paying for a card, including spending GOLD.
     * 
     * @param delta a map of color → count decreases to apply
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
     * Used by the engine to enforce the maximum tokens per player rule.
     * 
     * @return the sum of all token counts
     */
    public int getTotalTokens() {
        int total = 0;
        for (int count : tokens.values()) {
            total += count;
        }
        return total;
    }
    
    // ============ PURCHASED CARDS ============
    
    /**
     * Adds a bought development card to the player's purchased cards.
     * Affects prestige calculations and permanent bonus calculations.
     * 
     * @param card the development card to add
     */
    public void addPurchasedCard(DevelopmentCard card) {
        purchasedCards.add(card);
    }
    
    /**
     * Returns an unmodifiable view of the player's purchased development cards.
     * Prevents external code from mutating the list.
     * 
     * @return an unmodifiable view of purchased cards
     */
    public List<DevelopmentCard> getPurchasedCards() {
        return Collections.unmodifiableList(purchasedCards);
    }
    
    // ============ RESERVED CARDS ============
    
    /**
     * Adds a development card to the player's reserved cards.
     * Used when reserving from the board or from a deck.
     * 
     * @param card the development card to reserve
     */
    public void addReservedCard(DevelopmentCard card) {
        reservedCards.add(card);
    }
    
    /**
     * Removes a reserved card, usually when the player buys it.
     * 
     * @param card the development card to remove
     * @return true if the card was removed, false if it was not found
     */
    public boolean removeReservedCard(DevelopmentCard card) {
        return reservedCards.remove(card);
    }
    
    /**
     * Returns an unmodifiable view of the player's reserved development cards.
     * Prevents external code from mutating the list.
     * 
     * @return an unmodifiable view of reserved cards
     */
    public List<DevelopmentCard> getReservedCards() {
        return Collections.unmodifiableList(reservedCards);
    }
    
    // ============ NOBLES ============
    
    /**
     * Adds a noble tile to the player's collection of nobles.
     * Increases prestige points.
     * 
     * @param noble the noble tile to add
     */
    public void addNoble(NobleTile noble) {
        nobles.add(noble);
    }
    
    /**
     * Returns the list of nobles the player has collected.
     * 
     * @return an unmodifiable view of nobles
     */
    public List<NobleTile> getNobles() {
        return Collections.unmodifiableList(nobles);
    }
    
    // ============ COMPUTED PROPERTIES ============
    
    /**
     * Returns how many permanent bonuses the player has for a given color.
     * Computed as the number of purchased development cards whose bonus color matches the parameter.
     * GOLD is not treated as a bonus color.
     * 
     * @param bonusColor the gem color bonus to count
     * @return the number of cards providing this bonus
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
     * Returns the total prestige points earned by this player.
     * Prestige comes from purchased development cards and collected nobles.
     * Tokens do not contribute points.
     * 
     * @return the total prestige points
     */
    public int getPrestigePoints() {
        int prestige = 0;
        
        // Add prestige from purchased cards
        for (DevelopmentCard card : purchasedCards) {
            prestige += card.getPrestigePoints();
        }
        
        // Add prestige from nobles
        for (NobleTile noble : nobles) {
            prestige += noble.getPrestigePoints();
        }
        
        return prestige;
    }
    
    /**
     * Returns the number of purchased development cards.
     * Used for tie-breaking: fewer development cards wins when prestige is tied.
     * 
     * @return the count of purchased development cards
     */
    public int getDevelopmentCardCount() {
        return purchasedCards.size();
    }
}
