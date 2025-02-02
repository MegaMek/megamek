package megamek.client.bot.princess;

import megamek.common.Coords;
import megamek.common.Entity;

public class CoverageValidator {

    private final Princess owner;

    public CoverageValidator(Princess owner) {
        this.owner = owner;
    }

    public boolean validateUnitCoverage(Entity unit, Coords newPosition) {
        // Check 0.6x coverage from at least one ally
        return owner.getFriendEntities().stream()
            .filter(e -> !e.equals(unit))
            .anyMatch(ally -> ally.getPosition().distance(newPosition) <=
                ally.getMaxWeaponRange() * 0.6);
    }

    public boolean isPositionCovered(Entity unit) {
        return validateUnitCoverage(unit, unit.getPosition());
    }

}
