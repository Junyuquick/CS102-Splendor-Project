package model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of a noble.
 */
public class NobleTile implements Serializable {

    private final int id;
    private final int prestigePoints;
    private final Cost requirement;

    /**
     * Creates a noble tile.
     *
     * @param id noble identifier
     * @param prestigePoints prestige points given by noble
     * @param requirement permanent bonus points for each color required in order to purchase the noble
     */
    public NobleTile(int id, int prestigePoints, Cost requirement) {
        this.id = id;
        if (prestigePoints < 0) {
            throw new IllegalArgumentException("Prestige points cannot be negative");
        }
        this.prestigePoints = prestigePoints;
        this.requirement = Objects.requireNonNull(requirement, "Requirement cannot be null");
    }

    /**
     * Returns noble identifier.
     *
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the prestige points given by noble.
     *
     */
    public int getPrestigePoints() {
        return prestigePoints;
    }

    /**
     * Returns the bonus points needed to claim the noble
     *
     * @return the noble requirement
     */
    public Cost getRequirement() {
        return requirement;
    }

    /**
     * Returns the noble requirements as a map with color as its key
     *
     * @return an unmodifiable view of the noble requirements
     */
    public Map<GemColor, Integer> getRequirements() {
        return Collections.unmodifiableMap(requirement.asMap());
    }

    /**
     * Returns all the attributes of the noble(identifier, pretige points, requirement)
     *
     */
    @Override
    public String toString() {
        return "NobleTile{" +
                "id=" + id +
                ", prestigePoints=" + prestigePoints +
                ", requirement=" + requirement +
                '}';
    }
}
