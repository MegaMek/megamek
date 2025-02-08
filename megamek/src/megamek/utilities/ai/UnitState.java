package megamek.utilities.ai;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.UnitRole;

public record UnitState(int id, int teamId, int round, int playerId, String chassis, String model, String type, UnitRole role,
                        int x, int y, int facing, double mp, double heat, boolean prone, boolean airborne,
                        boolean offBoard, boolean crippled, boolean destroyed, double armorP,
                        double internalP, boolean done, int maxRange, int totalDamage, int turnsWithoutMovement, Entity entity) {
    public Coords position() {
        return new Coords(x, y);
    }
}
