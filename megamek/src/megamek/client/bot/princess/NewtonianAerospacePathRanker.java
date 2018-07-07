package megamek.client.bot.princess;

import java.util.List;

import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.LosEffects;
import megamek.common.MovePath;

public class NewtonianAerospacePathRanker extends BasicPathRanker implements IPathRanker {
    
    public NewtonianAerospacePathRanker(Princess owningPrincess) {
        super(owningPrincess);
    }
    
    /**
     * Find the closest enemy to a unit with a path that ends at the given position.
     */
    @Override
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
    
    /**
     * Calculate the damage potential 
     */
    @Override
    double calculateMyDamagePotential(MovePath path, Entity enemy,
            int distance, IGame game) {
        Entity me = path.getEntity();

        int maxRange = me.getMaxWeaponRange();
        if (distance > maxRange) {
            return 0;
        }

        // If I don't have LoS, I can't do damage. We're on a space map so this probably is unnecessary.
        LosEffects losEffects = 
        LosEffects.calculateLos(game, me.getId(), enemy, path.getFinalCoords(), enemy.getPosition(), false);
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
     * Guesses a number of things about an enemy that has not yet moved
     */
    EntityEvaluationResponse evaluateUnmovedEnemy(Entity enemy, MovePath path,
                                                  boolean useExtremeRange,
                                                  boolean useLOSRange) {
        final String METHOD_NAME =
                "EntityEvaluationResponse evaluateUnmovedEnemy(Entity," +
                "MovePath,IGame)";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            EntityEvaluationResponse returnResponse =
                    new EntityEvaluationResponse();
            
            Coords finalCoords = path.getFinalCoords();
            Coords closest = getClosestCoordsTo(enemy.getId(), finalCoords);
            if (closest == null) {
                return returnResponse;
            }
            int range = closest.distance(finalCoords);
            if(range == 0) {
                range = 1;
            }

            
            // placeholder logic:
            // if we are a spheroid, we can fire viably in any direction
            // if we are a fighter or aerodyne dropship, our most effective arc is forward
            int arcToUse = ((Aero) path.getEntity()).isSpheroid() ? Compute.ARC_360 : Compute.ARC_NOSE;
            double vertexCoverage = 1.0;
            
            // the idea here is that, if we have a limited firing arc, the target
            // will likely make an effort to move out of the arc, so it reduces our expected damage
            // we calculate the proportion by looking at the number of "enemy movable area" vertices
            // that are in our main firing arc, compared to the max (6).
            if(arcToUse != Compute.ARC_360) {
                int inArcVertexCount = 0;
                
                for(int vertexNum = 0; vertexNum < 6; vertexNum++) {
                    Coords vertex = getPathEnumerator().getUnitMovableAreas().get(enemy.getId()).getVertexNum(vertexNum);
                    
                    if(Compute.isInArc(finalCoords, path.getFinalFacing(), vertex, arcToUse)) {
                        inArcVertexCount++;
                    }
                }
                
                vertexCoverage = inArcVertexCount / 6;
            }
                
            double myDamageDiscount = Compute.oddsAbove(path.getEntity().getCrew().getGunnery()) / 100 * vertexCoverage;
            
            // my estimated damage is my max damage at the 
            returnResponse.addToMyEstimatedDamage(
                        getMaxDamageAtRange(getFireControl(),
                                            path.getEntity(),
                                            range,
                                            useExtremeRange,
                                            useLOSRange) * myDamageDiscount);

            double enemyDamageDiscount = Compute.oddsAbove(enemy.getCrew().getGunnery()) / 100;
            //in general if an enemy can end its position in range, it can hit me
            returnResponse.addToEstimatedEnemyDamage(
                    getMaxDamageAtRange(getFireControl(),
                                        enemy,
                                        range,
                                        useExtremeRange,
                                        useLOSRange) * enemyDamageDiscount);

            return returnResponse;
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }
    
    /**
     * Worker function that determines if a given enemy entity should be evaluated as if it has moved.
     */
    @Override
    protected boolean evaluateAsMoved(Entity enemy) {
        return !enemy.isSelectableThisTurn() || enemy.isImmobile();
    }
}
