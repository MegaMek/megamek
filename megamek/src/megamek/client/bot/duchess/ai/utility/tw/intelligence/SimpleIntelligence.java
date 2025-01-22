/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.client.bot.duchess.ai.utility.tw.intelligence;

import megamek.ai.utility.Decision;
import megamek.ai.utility.DecisionMaker;
import megamek.ai.utility.Intelligence;
import megamek.ai.utility.ScoredDecision;
import megamek.client.bot.princess.*;
import megamek.common.*;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.logging.MMLogger;
import org.apache.logging.log4j.Level;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

/**
 * SimpleIntelligence does not implement memory or learning in any way, has no stickyness for decisions, and does not
 * implement any special behaviors, uses BEST for decision process.
 *
 * @author Luana Coppio
 */
public class SimpleIntelligence extends BasicPathRanker implements Intelligence<Entity, Entity> {

    private final static MMLogger logger = MMLogger.create(SimpleIntelligence.class);

    private final List<Decision<Entity, Entity>> decisions = new ArrayList<>();
    private final DecisionMaker<Entity, Entity> decisionMaker = new DecisionMaker<>() {
        @Override
        public Optional<Decision<Entity, Entity>> pickOne(PriorityQueue<ScoredDecision<Entity, Entity>> scoredDecisions) {
            return DecisionMaker.super.pickOne(scoredDecisions);
        }

        @Override
        public double getBonusFactor(Decision<Entity, Entity> scoreEvaluator) {
            return 0;
        }
    };

    private SimpleIntelligence(Princess princess) {
        super(princess);
        // empty constructor
    }

    @Override
    public void update(Intelligence<Entity, Entity> intelligence) {
        // there is nothing to update
    }

    @Override
    public void addDecisionScoreEvaluator(Decision<Entity, Entity> decision) {
        decisions.add(decision);
    }

    @Override
    public List<Decision<Entity, Entity>> getDecisions() {
        return decisions;
    }

    @Override
    public DecisionMaker<Entity, Entity> getDecisionMaker() {
        return decisionMaker;
    }

    @Override
    public double getBonusFactor(Decision<Entity, Entity> scoreEvaluator) {
        return 0;
    }

    public static SimpleIntelligence create(Princess princess) {
        return new SimpleIntelligence(princess);
    }

    public ArrayList<RankedPath> rankPaths(List<MovePath> movePaths, Game game, int maxRange,
                                           double fallTolerance, List<Entity> enemies,
                                           List<Entity> friends) {
        // No point in ranking an empty list.
//        if (movePaths.isEmpty()) {
//            return new ArrayList<>();
//        }
//
//        // the cached path probability data is really only relevant for one iteration
//        // through this method
//        getPathRankerState().getPathSuccessProbabilities().clear();
//
//        // Let's try to whittle down this list.
//        List<MovePath> validPaths = validatePaths(movePaths, game, maxRange, fallTolerance);
//
//        // If the heat map of friendly activity has sufficient data, use the nearest hot
//        // spot as
//        // the anchor point
//        Coords allyCenter = getOwner().getFriendlyHotSpot(movePaths.get(0).getEntity().getPosition());
//        if (allyCenter == null) {
//            allyCenter = calcAllyCenter(movePaths.get(0).getEntity().getId(), friends, game);
//        }
//
//        ArrayList<RankedPath> returnPaths = new ArrayList<>(validPaths.size());
//
//        try {
//            final BigDecimal numberPaths = new BigDecimal(validPaths.size());
//            BigDecimal count = BigDecimal.ZERO;
//            BigDecimal interval = new BigDecimal(5);
//
//            boolean pathsHaveExpectedDamage = false;
//
//            for (MovePath path : validPaths) {
//                count = count.add(BigDecimal.ONE);
//
//                RankedPath rankedPath = rankPath(path, game, maxRange, fallTolerance, enemies, allyCenter);
//
//                returnPaths.add(rankedPath);
//
//                // we want to keep track of if any of the paths we've considered have some kind
//                // of damage potential
//                pathsHaveExpectedDamage |= (rankedPath.getExpectedDamage() > 0);
//
//                BigDecimal percent = count.divide(numberPaths, 2, RoundingMode.DOWN).multiply(new BigDecimal(100))
//                    .round(new MathContext(0, RoundingMode.DOWN));
//                if (percent.compareTo(interval) >= 0) {
//                    if (logger.isLevelLessSpecificThan(Level.INFO)) {
//                        getOwner().sendChat("... " + percent.intValue() + "% complete.");
//                    }
//                    interval = percent.add(new BigDecimal(5));
//                }
//            }
//
//            Entity mover = movePaths.get(0).getEntity();
//            UnitBehavior behaviorTracker = getOwner().getUnitBehaviorTracker();
//            boolean noDamageButCanDoDamage = !pathsHaveExpectedDamage
//                && (FireControl.getMaxDamageAtRange(mover, 1, false, false) > 0);
//
//            // if we're trying to fight, but aren't going to be doing any damage no matter
//            // how we move
//            // then let's try to get closer
//            if (noDamageButCanDoDamage
//                && (behaviorTracker.getBehaviorType(mover, getOwner()) == UnitBehavior.BehaviorType.Engaged)) {
//                behaviorTracker.overrideBehaviorType(mover, UnitBehavior.BehaviorType.MoveToContact);
//                return rankPaths(getOwner().getMovePathsAndSetNecessaryTargets(mover, true),
//                    game, maxRange, fallTolerance, enemies, friends);
//            }
//        } catch (Exception ignored) {
//            logger.error(ignored, ignored.getMessage());
//            return returnPaths;
//        }

        return null;
    }

    @Override
    protected RankedPath rankPath(MovePath path, Game game, int maxRange, double fallTolerance, List<Entity> enemies, Coords friendsCoords) {
        /** here goes the considerations calculation **/
//        Entity movingUnit = path.getEntity();
//        StringBuilder formula = new StringBuilder("Calculation: {");
//
//        if (blackIce == -1) {
//            blackIce = ((game.getOptions().booleanOption(OptionsConstants.ADVANCED_BLACK_ICE)
//                && game.getPlanetaryConditions().getTemperature() <= PlanetaryConditions.BLACK_ICE_TEMP)
//                || game.getPlanetaryConditions().getWeather().isIceStorm()) ? 1 : 0;
//        }
//
//        // Copy the path to avoid inadvertent changes.
//        MovePath pathCopy = path.clone();
//
//        // Worry about failed piloting rolls (weighted by Fall Shame).
//        double successProbability = getMovePathSuccessProbability(pathCopy, formula);
//        double utility = - 1; // calculateFallMod(successProbability, formula);
//
//        // Worry about how badly we can damage ourselves on this path!
//        double expectedDamageTaken = calculateMovePathPSRDamage(movingUnit, pathCopy, formula);
//        expectedDamageTaken += checkPathForHazards(pathCopy, movingUnit, game);
//        expectedDamageTaken += MinefieldUtil.checkPathForMinefieldHazards(pathCopy);
//
//        // look at all of my enemies
//        FiringPhysicalDamage damageEstimate = new FiringPhysicalDamage();
//
//        boolean extremeRange = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE);
//        boolean losRange = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE);
//        for (Entity enemy : enemies) {
//            // Skip ejected pilots.
//            if (enemy instanceof MekWarrior) {
//                continue;
//            }
//
//            // Skip units not actually on the board.
//            if (enemy.isOffBoard() || (enemy.getPosition() == null)
//                || !game.getBoard().contains(enemy.getPosition())) {
//                continue;
//            }
//
//            // Skip broken enemies
//            if (getOwner().getHonorUtil().isEnemyBroken(enemy.getId(), enemy.getOwnerId(),
//                getOwner().getForcedWithdrawal())) {
//                continue;
//            }
//
//            EntityEvaluationResponse eval;
//
//            if (evaluateAsMoved(enemy)) {
//                // For units that have already moved
//                eval = evaluateMovedEnemy(enemy, pathCopy, game);
//            } else {
//                // For units that have not moved this round
//                eval = evaluateUnmovedEnemy(enemy, path, extremeRange, losRange);
//            }
//
//            // if we're not ignoring the enemy, we consider damage that we may do to them;
//            // however, just because we're ignoring them doesn't mean they won't shoot at
//            // us.
//            if (!getOwner().getBehaviorSettings().getIgnoredUnitTargets().contains(enemy.getId())) {
//                if (damageEstimate.firingDamage < eval.getMyEstimatedDamage()) {
//                    damageEstimate.firingDamage = eval.getMyEstimatedDamage();
//                }
//                if (damageEstimate.physicalDamage < eval.getMyEstimatedPhysicalDamage()) {
//                    damageEstimate.physicalDamage = eval.getMyEstimatedPhysicalDamage();
//                }
//            }
//
//            expectedDamageTaken += eval.getEstimatedEnemyDamage();
//        }
//
//        // if we're not in the air, we may get hit by friendly artillery
//        if (!path.getEntity().isAirborne() && !path.getEntity().isAirborneVTOLorWIGE()) {
//            double friendlyArtilleryDamage = 0;
//            Map<Coords, Double> artyDamage = getOwner().getPathRankerState().getIncomingFriendlyArtilleryDamage();
//
//            if (!artyDamage.containsKey(path.getFinalCoords())) {
//                friendlyArtilleryDamage = ArtilleryTargetingControl
//                    .evaluateIncomingArtilleryDamage(path.getFinalCoords(), getOwner());
//                artyDamage.put(path.getFinalCoords(), friendlyArtilleryDamage);
//            } else {
//                friendlyArtilleryDamage = artyDamage.get(path.getFinalCoords());
//            }
//
//            expectedDamageTaken += friendlyArtilleryDamage;
//        }
//
//        calcDamageToStrategicTargets(pathCopy, game, getOwner().getFireControlState(), damageEstimate);
//
//        // If I cannot kick because I am a clan unit and "No physical attacks for the
//        // clans"
//        // is enabled, set maximum physical damage for this path to zero.
//        if (game.getOptions().booleanOption(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL)
//            && path.getEntity().getCrew().isClanPilot()) {
//            damageEstimate.physicalDamage = 0;
//        }
//
//        // I can kick a different target than I shoot, so add physical to
//        // total damage after I've looked at all enemies
//        double maximumDamageDone = damageEstimate.firingDamage + damageEstimate.physicalDamage;
//
//        // My bravery modifier is based on my chance of getting to the
//        // firing position (successProbability), how much damage I can do
//        // (weighted by bravery), less the damage I might take.
//        double braveryValue = getOwner().getBehaviorSettings().getBraveryValue();
//        double braveryMod = (successProbability * (maximumDamageDone * braveryValue)) - expectedDamageTaken;
//        formula.append(" + braveryMod [")
//            .append(LOG_DECIMAL.format(braveryMod)).append(" = ")
//            .append(LOG_PERCENT.format(successProbability))
//            .append(" * ((")
//            .append(LOG_DECIMAL.format(maximumDamageDone)).append(" * ")
//            .append(LOG_DECIMAL.format(braveryValue)).append(") - ")
//            .append(LOG_DECIMAL.format(expectedDamageTaken)).append("]");
//        utility += braveryMod;
//
//        // the only critters not subject to aggression and herding mods are
//        // airborne aeros on ground maps, as they move incredibly fast
//        if (!path.getEntity().isAirborneAeroOnGroundMap()) {
//            // The further I am from a target, the lower this path ranks
//            // (weighted by Aggression slider).
//            utility -= calculateAggressionMod(movingUnit, pathCopy, game, formula);
//
//            // The further I am from my teammates, the lower this path
//            // ranks (weighted by Herd Mentality).
//            utility -= calculateHerdingMod(friendsCoords, pathCopy, formula);
//        }
//
//        // Try to face the enemy.
//        double facingMod = calculateFacingMod(movingUnit, game, pathCopy, formula);
//        if (facingMod == -10000) {
//            return new RankedPath(facingMod, pathCopy, formula.toString());
//        }
//        utility -= facingMod;
//
//        // If I need to flee the board, I want to get closer to my home edge.
//        utility -= calculateSelfPreservationMod(movingUnit, pathCopy, game, formula);
//
//        // if we're an aircraft, we want to de-value paths that will force us off the
//        // board
//        // on the subsequent turn.
//        utility -= utility * calculateOffBoardMod(pathCopy);
//
//        RankedPath rankedPath = new RankedPath(utility, pathCopy, formula.toString());
//        rankedPath.setExpectedDamage(maximumDamageDone);
        return null;
    }

    @Override
    public double distanceToClosestEnemy(Entity entity, Coords position, Game game) {
        return 0;
    }
}
