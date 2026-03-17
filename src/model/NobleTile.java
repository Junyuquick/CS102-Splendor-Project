package model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class NobleTile {

    private final int id;
    private final int prestigePoints;
    private final Cost requirement;

    public NobleTile(int id, int prestigePoints, Cost requirement) {
        this.id = id;
        if (prestigePoints < 0) {
            throw new IllegalArgumentException("Prestige points cannot be negative");
        }
        this.prestigePoints = prestigePoints;
        this.requirement = Objects.requireNonNull(requirement, "Requirement cannot be null");
    }

    public int getId() {
        return id;
    }

    public int getPrestigePoints() {
        return prestigePoints;
    }

    public Cost getRequirement() {
        return requirement;
    }

    // Compatibility accessor for engine services that read noble requirements as a map.
    public Map<GemColor, Integer> getRequirements() {
        return Collections.unmodifiableMap(requirement.asMap());
    }

    @Override
    public String toString() {
        return "NobleTile{" +
                "id=" + id +
                ", prestigePoints=" + prestigePoints +
                ", requirement=" + requirement +
                '}';
    }
}
