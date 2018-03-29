package megamek.client.bot.princess;

import java.util.List;

import megamek.client.bot.princess.BotGeometry.HexLine;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.MechWarrior;
import megamek.common.MovePath;
import megamek.common.Targetable;
import megamek.common.options.OptionsConstants;

public class InfantryPathRanker extends BasicPathRanker implements IPathRanker {

    public InfantryPathRanker(Princess princess) {
        super(princess);

        setFireControl(princess.getFireControl());
        setPathEnumerator(princess.getPrecognition().getPathEnumerator());
    }

    @Override
    protected RankedPath rankPath(MovePath path, IGame game, int maxRange, double fallTolerance, int distanceHome,
            List<Entity> enemies, Coords friendsCoords) {
        final String METHOD_NAME = "rankPath(MovePath, IGame, Targetable, int, " +
                "double, int, int, List<Entity>, Coords)";

        getOwner().methodBegin(getClass(), METHOD_NAME);
        
        Entity movingUnit = path.getEntity();
        StringBuilder formula = new StringBuilder("Calculation: {");
        
        try {
            // Copy the path to avoid inadvertent changes.
            MovePath pathCopy = path.clone();
                       
            // look at all of my enemies
            double maximumDamageDone = 0;

            double expectedDamageTaken = checkPathForHazards(pathCopy,
                                                      movingUnit,
                                                      game);
            boolean extremeRange = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE);
            boolean losRange = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE);
            for (Entity enemy : enemies) {
           
                // Skip ejected pilots.
                if (enemy instanceof MechWarrior) {
                    continue;
                }
                
                // Skip units not actually on the board.
                if (enemy.isOffBoard() || (enemy.getPosition() == null) 
                        || !game.getBoard().contains(enemy.getPosition())) {
                    continue;
                }
                
                //skip broken enemies
                if (getOwner().getHonorUtil().isEnemyBroken(enemy.getId(),
                     enemy.getOwnerId(), getOwner().getForcedWithdrawal())) {
                    continue;
                }
                
                EntityEvaluationResponse eval;
                // For units that have already moved
                // TODO: Always consider Aeros to have moved, as right now we
                // don't try to predict their movement.
                if ((!enemy.isSelectableThisTurn()) || enemy.isImmobile() || enemy.isAero()) { 
                    eval = evaluateMovedEnemy(enemy, pathCopy, game);
                } else { //for units that have not moved this round
                    eval = evaluateUnmovedEnemy(enemy, path, extremeRange, losRange);
                }
                
                if (maximumDamageDone < eval.getMyEstimatedDamage()) {
                    maximumDamageDone = eval.getMyEstimatedDamage();
                }
                
                expectedDamageTaken += eval.getEstimatedEnemyDamage();
            }
            
            // Include damage I can do to strategic targets
            for (int i = 0; i < getOwner().getFireControl()
                                   .getAdditionalTargets().size(); i++) {
                Targetable target = getOwner().getFireControl()
                                           .getAdditionalTargets().get(i);
                if (target.isOffBoard() || (target.getPosition() == null)
                 || !game.getBoard().contains(target.getPosition())) {
                    continue; // Skip targets not actually on the board.
                }
                
                FiringPlanCalculationParameters guess =
                     new FiringPlanCalculationParameters.Builder()
                             .buildGuess(path.getEntity(),
                                         new EntityState(path),
                                         target,
                                         null,
                                         FireControl.DOES_NOT_TRACK_HEAT,
                                         null);
                
                FiringPlan myFiringPlan = getOwner().getFireControl().determineBestFiringPlan(guess);
                double myDamagePotential = myFiringPlan.getUtility();
                if (myDamagePotential > maximumDamageDone) {
                    maximumDamageDone = myDamagePotential;
                }
            }
                    
            // My bravery modifier is based on my chance of getting to the 
            // firing position (successProbability), how much damage I can do 
            // (weighted by bravery), less the damage I might take.
            double braveryValue =
             getOwner().getBehaviorSettings().getBraveryValue();
            double braveryMod = (maximumDamageDone * braveryValue) - expectedDamageTaken;
            formula.append(" + braveryMod [")
            .append(LOG_DECIMAL.format(braveryMod)).append(" = ")
            .append("((")
            .append(LOG_DECIMAL.format(maximumDamageDone)).append(" * ")
            .append(LOG_DECIMAL.format(braveryValue)).append(") - ")
            .append(LOG_DECIMAL.format(expectedDamageTaken)).append("]");
            double utility = braveryMod;
            
            // The further I am from a target, the lower this path ranks 
            // (weighted by Hyper Aggression.
            utility -= calculateAggressionMod(movingUnit, pathCopy, game, formula);
            
            // The further I am from my teammates, the lower this path 
            // ranks (weighted by Herd Mentality).
            utility -= calculateHerdingMod(friendsCoords, pathCopy, formula);
            
            // If I need to flee the board, I want to get closer to my home edge.
            utility -= calculateSelfPreservationMod(movingUnit, pathCopy, game,
                                             formula);
            
            RankedPath rankedPath = new RankedPath(utility, pathCopy, formula.toString());
            rankedPath.setExpectedDamage(maximumDamageDone);
            return rankedPath;
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }
    
    @Override
    EntityEvaluationResponse evaluateMovedEnemy(Entity enemy, MovePath path,
            IGame game) {
        return super.evaluateMovedEnemy(enemy, path, game);
    }
    
    EntityEvaluationResponse evaluateUnmovedEnemy(Entity enemy, MovePath path, 
            boolean useExtremeRange, boolean useLOSRange) {
        
        final String METHOD_NAME =
                "EntityEvaluationResponse evaluateUnmovedEnemy(Entity," +
                "MovePath,IGame)";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            //some preliminary calculations
            final double damageDiscount = 0.25;
            EntityEvaluationResponse returnResponse =
                    new EntityEvaluationResponse();

            //Aeros always move after other units, and would require an 
            // entirely different evaluation
            //TODO (low priority) implement a way to see if I can dodge aero units
            if (enemy.isAero()) {
                return returnResponse;
            }
            
            Coords finalCoords = path.getFinalCoords();
            int myFacing = path.getFinalFacing();
            Coords behind = finalCoords.translated((myFacing + 3) % 6);
            Coords leftFlank = finalCoords.translated((myFacing + 2) % 6);
            Coords rightFlank = finalCoords.translated((myFacing + 4) % 6);
            Coords closest = getClosestCoordsTo(enemy.getId(), finalCoords);
            if (closest == null) {
                return returnResponse;
            }
            int range = closest.distance(finalCoords);

            // for infantry, facing doesn't matter (unless you're using "dig in" rules, but we're not there yet)
            returnResponse.addToMyEstimatedDamage(
                        getMaxDamageAtRange(getFireControl(),
                                            path.getEntity(),
                                            range,
                                            useExtremeRange,
                                            useLOSRange) * damageDiscount);

            //in general if an enemy can end its position in range, it can hit me
            returnResponse.addToEstimatedEnemyDamage(
                    getMaxDamageAtRange(getFireControl(),
                                        enemy,
                                        range,
                                        useExtremeRange,
                                        useLOSRange)
                                                     * damageDiscount);

            //It is especially embarrassing if the enemy can move behind or flank me and then kick me
            if (canFlankAndKick(enemy, behind, leftFlank, rightFlank, myFacing)) {
                returnResponse.addToEstimatedEnemyDamage(
                        Math.ceil(enemy.getWeight() / 5.0) *
                        damageDiscount);
            }
            return returnResponse;
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }
}
