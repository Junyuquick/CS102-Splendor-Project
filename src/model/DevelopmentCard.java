package model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable model of a development card on the board or in a deck.
 */
public class DevelopmentCard implements Serializable {

    private final int id;
    private final int level;
    private final int prestigePoints;
    private final GemColor bonusColor;
    private final Cost cost;

    /**
     * Creates a DevelopementCard.
     *
     * @param id card identifier
     * @param level card tier (1-3)
     * @param prestigePoints prestige points value of the card
     * @param bonusColor permanent bonus color granted by the card
     * @param cost token cost needed to buy the card
     */
    public DevelopmentCard(int id,
                           int level,
                           int prestigePoints,
                           GemColor bonusColor,
                           Cost cost) {
        this.id = id;
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("Card level must be 1, 2, or 3");
        }
        this.level = level;

        this.cost = Objects.requireNonNull(cost, "Cost cannot be null");

        if (prestigePoints < 0) {
            throw new IllegalArgumentException("Prestige points cannot be negative");
        }

        if (bonusColor == null || bonusColor == GemColor.GOLD) {
            throw new IllegalArgumentException("Invalid bonus color");
        }

        this.prestigePoints = prestigePoints;
        this.bonusColor = bonusColor;
    }

    /**
     * Returns the card identifier.
     *
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the card tier.
     *
     */
    public int getLevel() {
        return level;
    }

    /**
     * Returns the prestige points of the card.
     *
     */
    public int getPrestigePoints() {
        return prestigePoints;
    }

    /**
     * Returns the permanent bonus color points given to the player after a card is bought.
     *
     */
    public GemColor getBonusColor() {
        return bonusColor;
    }

    /**
     * Returns the token cost per color for the card (immutable)
     *
     */
    public Map<GemColor, Integer> getCost() {
        return Collections.unmodifiableMap(cost.asMap());
    }

    /**
     * Returns all the attributes of the card (id, level, pretige points, bonus points, cost)
     *
     */
    @Override
    public String toString() {
        return "ID: " + id +
               ", Level: " + level +
               ", Points: " + prestigePoints +
               ", Bonus: " + bonusColor +
               ", Cost: " + cost;
    }

    /**
     * Compares this card with another card object using card attributes (id, level, pretige points, bonus points, cost)
     *
     * @param obj another card
     * @return true when both cards have the same attributes (same card)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DevelopmentCard other)) return false;
        return id == other.id
                && level == other.level
                && prestigePoints == other.prestigePoints
                && bonusColor == other.bonusColor
                && Objects.equals(cost, other.cost);
    }

    /**
     * Returns a hash code that is in line with the .equals method above.
     *
     * @return hash code for this card
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, level, prestigePoints, bonusColor, cost);
    }
}
