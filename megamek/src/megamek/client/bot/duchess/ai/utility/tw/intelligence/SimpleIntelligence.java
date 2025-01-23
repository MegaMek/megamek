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

import megamek.ai.utility.*;
import megamek.client.bot.duchess.Duchess;
import megamek.client.bot.duchess.ai.utility.tw.ClusteringService;
import megamek.client.bot.duchess.ai.utility.tw.context.TWWorld;
import megamek.client.bot.duchess.ai.utility.tw.decision.TWDecisionContext;
import megamek.client.bot.duchess.ai.utility.tw.profile.TWProfile;
import megamek.client.bot.princess.*;
import megamek.common.*;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;

import java.util.*;

/**
 * SimpleIntelligence does not implement memory or learning in any way, has no stickyness for decisions, and does not
 * implement any special behaviors, uses BEST for decision process.
 *
 * @author Luana Coppio
 */
public class SimpleIntelligence extends BasicPathRanker implements Intelligence<Entity, Entity>, PathRankerUtilCalculator {

    private final static MMLogger logger = MMLogger.create(SimpleIntelligence.class);
    private final List<Decision<Entity, Entity>> decisions = new ArrayList<>();
    private final DecisionMaker<Entity, Entity> decisionMaker = scoreEvaluator -> 0;
    private final TWWorld world;

    public SimpleIntelligence(Princess princess, TWProfile profile) {
        super(princess);
        this.world = new TWWorld(princess.getGame(), princess, new ClusteringService(15d, 6));
        if (profile != null) {
            this.decisions.addAll(profile.getDecisions());
        }
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

    public TreeSet<RankedPath> rankPaths(List<MovePath> movePaths, Game game, int maxRange,
                                           double fallTolerance, List<Entity> enemies,
                                           List<Entity> friends) {
        if (movePaths.isEmpty()) {
          return new TreeSet<>(Collections.reverseOrder());
        }
        cachedPilotBaseRoll.clear();
        getPathRankerState().getPathSuccessProbabilities().clear();
        List<MovePath> validPaths = validatePaths(movePaths, game, maxRange, fallTolerance);
        var currentEntity = movePaths.get(0).getEntity();
        List<DecisionContext<Entity, Entity>> decisionContexts = new ArrayList<>(validPaths.size());

        if (getOwner() instanceof Duchess duchess) {
            for (var path : validPaths) {
                var decisionContext = new TWDecisionContext(duchess, duchess.getWorld(), currentEntity, enemies, path, this);
                decisionContexts.add(decisionContext);
            }
        }

        TreeSet<RankedPath> returnPaths = new TreeSet<>(Collections.reverseOrder());
        DecisionMaker<Entity, Entity> simpleDecisionMaker = new DecisionMaker<>() {
            @Override
            public double getBonusFactor(Decision<Entity, Entity> scoreEvaluator) {
                return 0;
            }

            @Override
            public void scoreAllDecisions(List<Decision<Entity, Entity>> decisions, List<DecisionContext<Entity, Entity>> contexts) {
                TWDecisionContext previousContext = null;
                for (var context : contexts) {
                    TWDecisionContext decisionContext = (TWDecisionContext) context;
                    double cutoff = 0.0d;
                    for (var decision : decisions) {
                        decision.setDecisionContext(decisionContext);
                        decision.setScore(0d);
                        double bonus = decisionContext.getBonusFactor(previousContext);
                        if (bonus < cutoff) {
                            continue;
                        }
                        var decisionScoreEvaluator = decision.getDecisionScoreEvaluator();
                        var debugReporter = new DebugReporter();

                        var score = decisionScoreEvaluator.score(decisionContext, getBonusFactor(decision), 0.0d, debugReporter);
                        returnPaths.add(new RankedPath(score, decisionContext.getMovePath(), debugReporter.getReport(), decisionContext.getExpectedDamage()));
                    }
                }
            }
        };

        simpleDecisionMaker.scoreAllDecisions(decisions, decisionContexts);

        return returnPaths;
    }

    @Override
    public FiringPhysicalDamage damageCalculator(MovePath path, List<Entity> enemies) {
        Entity movingUnit = path.getEntity();
        MovePath pathCopy = path.clone();

        double expectedDamageTaken = calculateMovePathPSRDamage(movingUnit, pathCopy, new StringBuilder());
        expectedDamageTaken += checkPathForHazards(pathCopy, movingUnit, world.getGame());
        expectedDamageTaken += MinefieldUtil.checkPathForMinefieldHazards(pathCopy);

        BasicPathRanker.FiringPhysicalDamage damageEstimate = new BasicPathRanker.FiringPhysicalDamage();

        boolean extremeRange = world.useBooleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE);
        boolean losRange = world.useBooleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE);
        for (Entity enemy : enemies) {
            // Skip ejected pilots.
            if (enemy instanceof MekWarrior) {
                continue;
            }

            // Skip units not actually on the board.
            if (enemy.isOffBoard() || (enemy.getPosition() == null)
                || !world.contains(enemy.getPosition())) {
                continue;
            }

            // Skip broken enemies
            if (getOwner().getHonorUtil().isEnemyBroken(enemy.getId(), enemy.getOwnerId(),
                getOwner().getForcedWithdrawal())) {
                continue;
            }

            EntityEvaluationResponse eval;

            if (evaluateAsMoved(enemy)) {
                // For units that have already moved
                eval = evaluateMovedEnemy(enemy, pathCopy, world.getGame());
            } else {
                // For units that have not moved this round
                eval = evaluateUnmovedEnemy(enemy, path, extremeRange, losRange);
            }

            // if we're not ignoring the enemy, we consider damage that we may do to them;
            // however, just because we're ignoring them doesn't mean they won't shoot at
            // us.
            if (!getOwner().getBehaviorSettings().getIgnoredUnitTargets().contains(enemy.getId())) {
                if (damageEstimate.firingDamage < eval.getMyEstimatedDamage()) {
                    damageEstimate.firingDamage = eval.getMyEstimatedDamage();
                }
                if (damageEstimate.physicalDamage < eval.getMyEstimatedPhysicalDamage()) {
                    damageEstimate.physicalDamage = eval.getMyEstimatedPhysicalDamage();
                }
            }

            expectedDamageTaken += eval.getEstimatedEnemyDamage();
        }

        // if we're not in the air, we may get hit by friendly artillery
        if (!path.getEntity().isAirborne() && !path.getEntity().isAirborneVTOLorWIGE()) {
            double friendlyArtilleryDamage = 0;
            Map<Coords, Double> artyDamage = getOwner().getPathRankerState().getIncomingFriendlyArtilleryDamage();

            if (!artyDamage.containsKey(path.getFinalCoords())) {
                friendlyArtilleryDamage = ArtilleryTargetingControl
                    .evaluateIncomingArtilleryDamage(path.getFinalCoords(), getOwner());
                artyDamage.put(path.getFinalCoords(), friendlyArtilleryDamage);
            } else {
                friendlyArtilleryDamage = artyDamage.get(path.getFinalCoords());
            }

            expectedDamageTaken += friendlyArtilleryDamage;
        }

        calcDamageToStrategicTargets(pathCopy, world.getGame(), getOwner().getFireControlState(), damageEstimate);

        // If I cannot kick because I am a clan unit and "No physical attacks for the
        // clans"
        // is enabled, set maximum physical damage for this path to zero.
        if (world.useBooleanOption(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL)
            && path.getEntity().getCrew().isClanPilot()) {
            damageEstimate.physicalDamage = 0;
        }

        return new FiringPhysicalDamage().withTakenDamage(expectedDamageTaken).withFiringDamage(damageEstimate.firingDamage)
            .withPhysicalDamage(damageEstimate.physicalDamage);
    }

    public record FiringPhysicalDamage(double firingDamage, double physicalDamage, double takenDamage) {
        public FiringPhysicalDamage() {
            this(0, 0, 0);
        }

        public FiringPhysicalDamage withFiringDamage(double firingDamage) {
            return new FiringPhysicalDamage(firingDamage, physicalDamage, takenDamage);
        }

        public FiringPhysicalDamage withPhysicalDamage(double physicalDamage) {
            return new FiringPhysicalDamage(firingDamage, physicalDamage, takenDamage);
        }

        public FiringPhysicalDamage withTakenDamage(double takenDamage) {
            return new FiringPhysicalDamage(firingDamage, physicalDamage, takenDamage);
        }
    }

}
