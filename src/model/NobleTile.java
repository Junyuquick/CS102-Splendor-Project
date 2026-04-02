package model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of a noble tile that can be awarded to
 * a player.
 */
public class NobleTile implements Serializable {

    private final int id;
    private final int prestigePoints;
    private final Cost requirement;

    /**
     * Creates a noble tile.
     *
     * @param id stable noble identifier
     * @param prestigePoints prestige points granted by the noble
     * @param requirement permanent bonus requirements needed to claim
     *     the noble
     */
    public NobleTile(int id, int prestigePoints, Cost requirement) {
        this.id = id;
        if (prestigePoints < 0) {
            throw new IllegalArgumentException(
                    "Prestige points cannot be negative"
            );
        }
        this.prestigePoints = prestigePoints;
        this.requirement = Objects.requireNonNull(
                requirement,
                "Requirement cannot be null"
        );
    }

    /**
     * Returns the noble identifier.
     *
     * @return the noble identifier
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the prestige points awarded by the noble.
     *
     * @return the prestige point value
     */
    public int getPrestigePoints() {
        return prestigePoints;
    }

    /**
     * Returns the permanent bonus requirements needed to claim the noble.
     *
     * @return the noble requirement
     */
    public Cost getRequirement() {
        return requirement;
    }

    /**
     * Returns the noble requirements as a map keyed by color.
     *
     * @return an unmodifiable view of the noble requirements
     */
    public Map<GemColor, Integer> getRequirements() {
        return Collections.unmodifiableMap(requirement.asMap());
    }

    /**
     * Returns a concise textual summary of the noble tile.
     *
     * @return string form of this noble
     */
    @Override
    public String toString() {
        return "NobleTile{"
                + "id=" + id
                + ", prestigePoints=" + prestigePoints
                + ", requirement=" + requirement
                + '}';
    }
}
