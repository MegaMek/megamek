/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
*/
package megamek.client.bot.princess;

import megamek.client.bot.princess.FireControl.FireControlType;
import megamek.common.*;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;

import java.util.List;

public class InfantryPathRanker extends BasicPathRanker {
    private final static MMLogger logger = MMLogger.create(InfantryPathRanker.class);

    public InfantryPathRanker(Princess princess) {
        super(princess);

        setPathEnumerator(princess.getPrecognition().getPathEnumerator());
    }

    @Override
    protected RankedPath rankPath(MovePath path, Game game, int maxRange, double fallTolerance,
            List<Entity> enemies, Coords friendsCoords) {
        Entity movingUnit = path.getEntity();
        StringBuilder formula = new StringBuilder();

        // Copy the path to avoid inadvertent changes.
        MovePath pathCopy = path.clone();

        // look at all of my enemies
        FiringPhysicalDamage damageEstimate = new FiringPhysicalDamage();

        double expectedDamageTaken = checkPathForHazards(pathCopy, movingUnit, game);
        boolean extremeRange = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE);
        boolean losRange = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE);
        for (Entity enemy : enemies) {
            // Skip ejected pilots.
            if (enemy instanceof MekWarrior) {
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
            // TODO: Always consider Aero's to have moved, as right now we
            // don't try to predict their movement.
            if (!enemy.isSelectableThisTurn() || enemy.isImmobile() || enemy.isAero()) {
                eval = evaluateMovedEnemy(enemy, pathCopy, game);
            } else { //for units that have not moved this round
                eval = evaluateUnmovedEnemy(enemy, pathCopy, extremeRange, losRange);
            }

            if (damageEstimate.firingDamage < eval.getMyEstimatedDamage()) {
                damageEstimate.firingDamage = eval.getMyEstimatedDamage();
            }

            expectedDamageTaken += eval.getEstimatedEnemyDamage();
        }

        calcDamageToStrategicTargets(pathCopy, game, getOwner().getFireControlState(), damageEstimate);
        double maximumDamageDone = damageEstimate.firingDamage;

        // My bravery modifier is based on my chance of getting to the
        // firing position (successProbability), how much damage I can do
        // (weighted by bravery), less the damage I might take.
        double braveryValue = getOwner().getBehaviorSettings().getBraveryValue();
        double braveryMod = (maximumDamageDone * braveryValue) - expectedDamageTaken;

        // If an infantry unit is not in range to do damage,
        // then we want it to move closer. Otherwise, let's avoid charging up to unmoved units,
        // that's not going to end well.
        var aggressionMod = calculateAggressionMod(movingUnit, pathCopy, game);

        // The further I am from my teammates, the lower this path
        // ranks (weighted by Herd Mentality).
        var herdingMod = calculateHerdingMod(friendsCoords, pathCopy);

        // If I need to flee the board, I want to get closer to my home edge.
        var selfPreservationMod = calculateSelfPreservationMod(movingUnit, pathCopy, game);

        double utility = braveryMod;
        utility -= aggressionMod;
        utility -= herdingMod;
        utility -= selfPreservationMod;

        formula.append("Calculation: {braveryMod [")
            .append(LOG_DECIMAL.format(braveryMod)).append(" = ")
            .append("((")
            .append(LOG_DECIMAL.format(maximumDamageDone)).append(" * ")
            .append(LOG_DECIMAL.format(braveryValue)).append(") - ")
            .append(LOG_DECIMAL.format(expectedDamageTaken)).append("]")
            .append(")] - aggressionMod [").append(aggressionMod).append(" = ")
            .append(distanceToClosestEnemy(movingUnit, path.getFinalCoords(), game)).append(" * ")
            .append(getOwner().getBehaviorSettings().getHyperAggressionValue()).append("] - herdingMod [")
            .append(herdingMod).append(" = ").append(distanceToClosestEnemy(movingUnit, path.getFinalCoords(), game))
            .append(" * ").append(getOwner().getBehaviorSettings().getHerdMentalityValue()).append("] + selfPreservationMod [")
            .append(selfPreservationMod).append("]}");

        logger.trace("Calculation: {braveryMod [{}] = (({} * {}) - {})] - aggressionMod [{}] = {} * {}] - herdingMod [{}] = {} * {}] + selfPreservationMod [{}]}",
            LOG_DECIMAL.format(braveryMod),
            LOG_DECIMAL.format(maximumDamageDone),
            LOG_DECIMAL.format(braveryValue),
            LOG_DECIMAL.format(expectedDamageTaken),
            aggressionMod,
            distanceToClosestEnemy(movingUnit, path.getFinalCoords(), game), getOwner().getBehaviorSettings().getHyperAggressionValue(),
            herdingMod,
            distanceToClosestEnemy(movingUnit, path.getFinalCoords(), game), getOwner().getBehaviorSettings().getHerdMentalityValue(),
            selfPreservationMod);

        RankedPath rankedPath = new RankedPath(utility, pathCopy, formula.toString());
        rankedPath.setExpectedDamage(maximumDamageDone);
        return rankedPath;
    }

    @Override
    public EntityEvaluationResponse evaluateUnmovedEnemy(Entity enemy, MovePath path, boolean useExtremeRange, boolean useLOSRange) {
        //some preliminary calculations
        final double damageDiscount = 0.25;
        EntityEvaluationResponse returnResponse =
                new EntityEvaluationResponse();

        //Aero's always move after other units, and would require an
        // entirely different evaluation
        //TODO (low priority) implement a way to see if I can dodge aero units
        if (enemy.isAirborneAeroOnGroundMap()) {
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
        // assume that an enemy unit is highly unlikely to stand there and let you swarm them
        if (range <= 0) {
            range = 1;
        }

        MovePath blankEnemyPath = new MovePath(getOwner().getGame(), enemy);

        // for infantry, facing doesn't matter because you rotate for free
        // (unless you're using "dig in" rules, but we're not there yet)
        returnResponse.addToMyEstimatedDamage(
                    ((InfantryFireControl) getFireControl(path.getEntity())).getMaxDamageAtRange(
                        path,
                        blankEnemyPath,
                        range,
                        useExtremeRange,
                        useLOSRange) * damageDiscount);

        //in general if an enemy can end its position in range, it can hit me
        returnResponse.addToEstimatedEnemyDamage(
                ((InfantryFireControl) getOwner().getFireControl(FireControlType.Infantry)).getMaxDamageAtRange(
                        blankEnemyPath,
                        path,
                        range,
                        useExtremeRange,
                        useLOSRange) * damageDiscount);

        //It is especially embarrassing if the enemy can move behind or flank me and then kick me
        if (canFlankAndKick(enemy, behind, leftFlank, rightFlank, myFacing)) {
            returnResponse.addToEstimatedEnemyDamage(
                    Math.ceil(enemy.getWeight() / 5.0) *
                    damageDiscount);
        }
        return returnResponse;
    }
}
