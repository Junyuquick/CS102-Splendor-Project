package model;

import java.util.Objects;

public class DevelopmentCard {

    private final CardLevel level;
    private final int prestigePoints;
    private final GemColor bonusColor;
    private final Cost cost;

    /**
     * Constructs a DevelopmentCard.
     */
    public DevelopmentCard(CardLevel level,
                           int prestigePoints,
                           GemColor bonusColor,
                           Cost cost) {

        this.level = Objects.requireNonNull(level, "CardLevel cannot be null");
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
     * Returns the card level (LEVEL1, LEVEL2, LEVEL3).
     */
    public CardLevel getLevel() {
        return level;
    }

    /**
     * Returns the prestige points granted by this card.
     */
    public int getPrestigePoints() {
        return prestigePoints;
    }

    /**
     * Returns the permanent bonus gem color provided by this card.
     */
    public GemColor getBonusColor() {
        return bonusColor;
    }

    /**
     * Returns the cost required to purchase this card.
     */
    public Cost getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return "Level: " + level +
               ", Points: " + prestigePoints +
               ", Bonus: " + bonusColor +
               ", Cost: " + cost;
    }
}