package megamek.client.bot.princess;

import java.util.List;
import java.util.Set;

import megamek.client.bot.princess.BotGeometry.CoordFacingCombo;
import megamek.client.bot.princess.BotGeometry.HexLine;
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
            
            // this path ends with a set of coordinates and a direction that I'm facing. 
            // we examine the vertices of the area into which this enemy could potentially move, and make an 
            // attempt to evaluate how much of that area is out of my main weapons arc
            // a reasonable estimate is the percentage of vertices that are not in the main weapons arc
            // that number will be the multiplier for the amount of damage *I* can do to the enemy.
            // we will assume that the enemy will always move in such a way as to maximize their damage to me.
            // due to characteristics of space combat (no minimum range) however, if their "optimal firing point"
            // will result in me getting hit in a low-armor location, we apply a multiplier to the damage, 
            // as it is really bad to get hit in an un-armored location
            
            // don't feel like doing this tonight, though, thanks wife.
            
            Coords finalCoords = path.getFinalCoords();
            Coords closest = getClosestCoordsTo(enemy.getId(), finalCoords);
            if (closest == null) {
                return returnResponse;
            }
            int range = closest.distance(finalCoords);
            if(range == 0) {
                range = 1;
            }

            returnResponse.addToMyEstimatedDamage(
                        getMaxDamageAtRange(getFireControl(),
                                            path.getEntity(),
                                            range,
                                            useExtremeRange,
                                            useLOSRange));

            //in general if an enemy can end its position in range, it can hit me
            returnResponse.addToEstimatedEnemyDamage(
                    getMaxDamageAtRange(getFireControl(),
                                        enemy,
                                        range,
                                        useExtremeRange,
                                        useLOSRange));


            // check the potential places the other unit could get to.
            // for each potential place, check the max damage I can do to the enemy, and the max damage he can do to me
            // assume that the enemy will move in such a way as to do the most damage to me while taking the least damage
            /*Set<CoordFacingCombo> potentialEnemyLocations = getPathEnumerator().getUnitPotentialLocations().get(enemy.getId());
            getPathEnumerator().getUnitMovableAreas()
            
            EntityState myState = new EntityState(path);
            double maxEnemyUtility = -9999;
            
            myState.getPosition().
            
            for(CoordFacingCombo potentialEnemyLocation: potentialEnemyLocations) {
                EntityState enemyState = new EntityState(enemy, potentialEnemyLocation);
                
                // if the hex is entirely out of range, we may skip all the expensive calculations 
                if(enemyState.getPosition().distance(myState.getPosition()) > enemy.getMaxWeaponRange()) {
                    
                    if(maxEnemyUtility < 0) {
                        maxEnemyUtility = 0;
                    }
                    
                    continue;
                }
                
                // figure out the best shot the enemy an take from where they are
                FiringPlanCalculationParameters enemyGuess =
                        new FiringPlanCalculationParameters.Builder()
                                .buildGuess(enemy,
                                        enemyState,
                                        path.getEntity(),
                                        myState,
                                        enemy.getHeatCapacity(),
                                        null);
                
                FiringPlan projectedEnemyFiringPlan = getFireControl().determineBestFiringPlan(enemyGuess);

                
                // figure out the best shot I can take at the enemy from where I am
                FiringPlanCalculationParameters myGuess =
                        new FiringPlanCalculationParameters.Builder()
                                .buildGuess(enemy,
                                        enemyState,
                                        path.getEntity(),
                                        myState,
                                        enemy.getHeatCapacity(),
                                        null);
                
                FiringPlan projectedMyFiringPlan = getFireControl().determineBestFiringPlan(myGuess);
                
                if(projectedMyFiringPlan.getUtility() > 0) {
                    int alpha = 1;
                }
                
                // assume that, after I move, the enemy will move in such a way as to maximize their outgoing damage to me while
                // minimizing the damage I do
                double combinedUtilityToEnemy = projectedEnemyFiringPlan.getUtility() - projectedMyFiringPlan.getUtility();
                if(combinedUtilityToEnemy > maxEnemyUtility) {
                    maxEnemyUtility = combinedUtilityToEnemy;
                    returnResponse.setEstimatedEnemyDamage(projectedEnemyFiringPlan.getUtility());
                    returnResponse.setMyEstimatedDamage(projectedMyFiringPlan.getUtility());
                }
            }*/

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
