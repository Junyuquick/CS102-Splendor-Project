package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores all state that belongs to exactly one Splendor player.
 *
 * A player tracks tokens, purchased cards, reserved cards, nobles,
 * prestige points, and permanent bonuses.
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

    /**
     * Returns the token count for one color.
     *
     * @param color the gem color to query
     * @return the token count for that color, or 0 if absent
     */
    public int getTokenCount(GemColor color) {
        return tokens.getOrDefault(color, 0);
    }

    /**
     * Returns a copy of the player's full token counts.
     *
     * @return a copy of the token map
     */
    public Map<GemColor, Integer> getTokens() {
        return new HashMap<>(tokens);
    }

    /**
     * Increases the player's token counts by the provided amounts.
     *
     * @param delta token increases keyed by color
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
     *
     * @param delta token decreases keyed by color
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
     * Returns the total number of tokens the player holds.
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

    /**
     * Adds a bought development card to the player's purchased cards.
     *
     * @param card the development card to add
     */
    public void addPurchasedCard(DevelopmentCard card) {
        purchasedCards.add(card);
    }

    /**
     * Returns an unmodifiable view of the player's purchased cards.
     *
     * @return an unmodifiable view of purchased cards
     */
    public List<DevelopmentCard> getPurchasedCards() {
        return Collections.unmodifiableList(purchasedCards);
    }

    /**
     * Adds a development card to the player's reserved cards.
     *
     * @param card the development card to reserve
     */
    public void addReservedCard(DevelopmentCard card) {
        reservedCards.add(card);
    }

    /**
     * Removes a reserved card.
     *
     * @param card the development card to remove
     * @return true if the card was removed, false if it was not found
     */
    public boolean removeReservedCard(DevelopmentCard card) {
        return reservedCards.remove(card);
    }

    /**
     * Returns an unmodifiable view of the player's reserved cards.
     *
     * @return an unmodifiable view of reserved cards
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
     * @return an unmodifiable view of nobles
     */
    public List<NobleTile> getNobles() {
        return Collections.unmodifiableList(nobles);
    }

    /**
     * Returns how many permanent bonuses the player has for one color.
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
     *
     * @return the total prestige points
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
     *
     * @return the count of purchased development cards
     */
    public int getDevelopmentCardCount() {
        return purchasedCards.size();
    }
}
