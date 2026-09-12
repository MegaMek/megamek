package megamek.scratch;

import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import org.junit.jupiter.api.Test;

class Probe8931 {

    @Test
    void probe() throws Exception {
        Game game = SaveProbe.load(
              "C:/Users/drivi/AppData/Local/Temp/mmnight/f8931/fighter-stuck.sav.gz");
        System.out.println("phase=" + game.getPhase() + " round=" + game.getCurrentRound());
        for (Entity entity : game.getEntitiesVector()) {
            if (!entity.isAero()) {
                continue;
            }
            IAero aero = (IAero) entity;
            System.out.printf(
                  "%-26s alt=%d vel=%d velNext=%d airborne=%s spaceborne=%s done=%s "
                        + "outControl=%s pos=%s deployed=%s destroyed=%s%n",
                  entity.getShortName(), entity.getAltitude(), aero.getCurrentVelocity(),
                  aero.getNextVelocity(), entity.isAirborne(), entity.isSpaceborne(), entity.isDone(),
                  aero.isOutControlTotal(), entity.getPosition(), entity.isDeployed(), entity.isDestroyed());
        }
    }
}
