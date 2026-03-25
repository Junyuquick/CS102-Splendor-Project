package model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of a development card on the board or in a deck.
 */
public class DevelopmentCard implements Serializable {

    private final int id;
    private final int level;
    private final int prestigePoints;
    private final GemColor bonusColor;
    private final Cost cost;

    /**
     * Creates a development card.
     *
     * @param id stable card identifier
     * @param level card tier, from 1 to 3
     * @param prestigePoints prestige points awarded when purchased
     * @param bonusColor permanent bonus color granted by the card
     * @param cost token cost required to buy the card
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
     * @return the card identifier
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the card tier.
     *
     * @return the card level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Returns the prestige points granted by the card.
     *
     * @return the prestige point value
     */
    public int getPrestigePoints() {
        return prestigePoints;
    }

    /**
     * Returns the permanent bonus color granted after purchase.
     *
     * @return the bonus color
     */
    public GemColor getBonusColor() {
        return bonusColor;
    }

    /**
     * Returns the purchase cost of the card.
     *
     * @return an unmodifiable view of the token cost by color
     */
    public Map<GemColor, Integer> getCost() {
        return Collections.unmodifiableMap(cost.asMap());
    }

    /**
     * Returns a concise textual summary of the card.
     *
     * @return string form of this card
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
     * Compares this card with another object using card identity fields.
     *
     * @param obj object to compare against
     * @return {@code true} when both objects describe the same card
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
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return hash code for this card
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, level, prestigePoints, bonusColor, cost);
    }
}
