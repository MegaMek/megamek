package megamek.client.bot.princess;

import java.util.List;

import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.LosEffects;
import megamek.common.MovePath;

public class NewtonianAerospacePathRanker extends BasicPathRanker implements IPathRanker {

    public NewtonianAerospacePathRanker(Princess owningPrincess) {
        super(owningPrincess);
    }
    
    /**
     * Find the closest enemy to a unit with a path that ends at the given position.
     */
    public Entity findClosestEnemy(Entity me, Coords position, IGame game) {
        final String METHOD_NAME = "findClosestEnemy(Entity, Coords, IGame)";
        getOwner().methodBegin(PathRanker.class, METHOD_NAME);

        try {
            int range = 9999;
            Entity closest = null;
            List<Entity> enemies = getOwner().getEnemyEntities();
            for (Entity e : enemies) {
                // Also, skip withdrawing enemy bot units, to avoid humping disabled tanks and ejected mechwarriors
                if (getOwner().getHonorUtil().isEnemyBroken(e.getTargetId(), e.getOwnerId(), getOwner().getForcedWithdrawal())) {
                    continue;
                }

                // If a unit has not moved, assume it will move away from me.
                int unmovedDistMod = 0;
                if (e.isSelectableThisTurn() && !e.isImmobile()) {
                    unmovedDistMod = e.getWalkMP(true, false, false);
                }

                if ((position.distance(e.getPosition()) + unmovedDistMod) < range) {
                    range = position.distance(e.getPosition());
                    closest = e;
                }
            }
            return closest;
        } finally {
            getOwner().methodEnd(PathRanker.class, METHOD_NAME);
        }
    }
    
    double calculateMyDamagePotential(MovePath path, Entity enemy,
            int distance, IGame game) {
        Entity me = path.getEntity();
        Coords pathFinalPosition = Compute.getFinalPosition(me.getPosition(), path.getFinalVectors());

        int maxRange = me.getMaxWeaponRange();
        if (distance > maxRange) {
            return 0;
        }

        // If I don't have LoS, I can't do damage. We're on a space map so this probably is unnecessary.
        LosEffects losEffects = 
        LosEffects.calculateLos(game, me.getId(), enemy, pathFinalPosition, enemy.getPosition(), false);
        if (!losEffects.canSee()) {
            return 0;
        }

        FiringPlan myFiringPlan;
        
        FiringPlanCalculationParameters guess =
        new FiringPlanCalculationParameters.Builder()
          .buildGuess(path.getEntity(),
                      new EntityState(path),
                      enemy,
                      null,
                      FireControl.DOES_NOT_TRACK_HEAT,
                      null);
        myFiringPlan = getFireControl().determineBestFiringPlan(guess);
        
        return myFiringPlan.getUtility();
    }
    
    /**
     * Worker method for the distance calculation between a considered move path and a set of coordinates.
     * @param path The move path being considered.
     * @param coords The coordinates being considered.
     * @return
     */
    @Override
    protected int calculateDistance(MovePath path, Coords coords) {
        return coords.distance(Compute.getFinalPosition(path.getEntity().getPosition(), path.getFinalVectors()));
    }
}
