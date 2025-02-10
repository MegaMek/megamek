/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * without ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.client.bot.caspar.ai.utility.tw.intelligence;

import megamek.ai.utility.*;
import megamek.client.bot.caspar.Caspar;
import megamek.client.bot.princess.BasicPathRanker;
import megamek.client.bot.princess.RankedPath;
import megamek.client.bot.caspar.ai.utility.tw.decision.TWDecisionContext;
import megamek.client.bot.caspar.ai.utility.tw.profile.TWProfile;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.MovePath;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

import java.util.*;

/**
 * SimpleIntelligence does not implement memory or learning in any way, has no stickyness for decisions, and does not
 * implement any special behaviors, uses BEST for decision process.
 *
 * @author Luana Coppio
 */
public class SimpleIntelligence extends BasicPathRanker implements Intelligence<Entity, Entity, RankedPath> {
    private final static MMLogger logger = MMLogger.create(SimpleIntelligence.class);
    private final List<Decision<Entity, Entity>> decisions = new ArrayList<>();
    private final DecisionMaker<Entity, Entity, RankedPath> decisionMaker = new DecisionScorer();
    private final PathRankerUtilCalculator pathRankerUtilCalculator;
    private final Memory memory;

    public SimpleIntelligence(Caspar caspar, TWProfile profile) {
        super(caspar);
        this.memory = caspar.getMemory();
        this.decisions.addAll(profile.getDecisions());
        pathRankerUtilCalculator = new SimplePathRankerUtilCalculator(caspar, caspar.getGame(), this);
    }

    @Override
    public void update(Intelligence<Entity, Entity, RankedPath> intelligence) {
        // there is nothing to update
    }

    @Override
    public List<Decision<Entity, Entity>> getDecisions() {
        return decisions;
    }

    @Override
    public DecisionMaker<Entity, Entity, RankedPath> getDecisionMaker() {
        return decisionMaker;
    }

    @Override
    public double getBonusFactor(Entity entity, MovePath movePath) {
        // Can be used to control flow on the battlefield, negate areas, make sure that the unit does not
        // do something specific like jumping or sprinting when we dont want to
        // this would allow to create a list of denied actions
        // For now, all it does is check if the current movePath being taken move as many hexes as
        // it did in the previous movePath, if it did not, it will be penalized
        if (!entity.isProne()) {
            var pastRankedPathOpt = getPastRankedPath(entity);
            if (pastRankedPathOpt.isPresent()) {
                var pastRankedPath = pastRankedPathOpt.get();
                if (pastRankedPath.getPath().getHexesMoved() > movePath.getHexesMoved()) {
                    return 0.9;
                }
            }
        }
        return 1.0;
    }

    @Override
    public TreeSet<RankedPath> rankPaths(List<MovePath> movePaths, Game game, int maxRange,
                                         double fallTolerance, List<Entity> enemies,
                                         List<Entity> friends) {
        if (movePaths.isEmpty()) {
            return new TreeSet<>();
        }
        cachedPilotBaseRoll.clear();
        getPathRankerState().getPathSuccessProbabilities().clear();
        var validPaths = validatePaths(movePaths, game, maxRange, fallTolerance);
        var currentEntity = movePaths.get(0).getEntity();
        var decisionContexts = createDecisionContexts(validPaths, currentEntity, enemies);
        var returnPaths = scoreAllDecisions(decisionContexts);
        logger.info("Ranked paths: {}", returnPaths.size());
        return returnPaths;
    }

    private List<DecisionContext<Entity, Entity>> createDecisionContexts(List<MovePath> validPaths, Entity currentEntity, List<Entity> enemies) {
        List<DecisionContext<Entity, Entity>> decisionContexts = new ArrayList<>(validPaths.size());
        Caspar caspar = getOwner();
        var decisionContextBuilder = TWDecisionContext.TWDecisionContextBuilder.aTWDecisionContext()
            .withSharedDamageCache()
            .withBehaviorSettings(caspar.getBehaviorSettings())
            .withCurrentUnit(currentEntity)
            .withFireControlState(caspar.getFireControlState())
            .withIntelligence(this)
            .withMemories(this.memory)
            .withWorld(caspar.getWorld())
            .withTargetUnits(enemies)
            .withWaypoint(caspar.getUnitBehaviorTracker().getWaypointForEntity(currentEntity).orElse(null))
            .withUnitBehavior(caspar.getUnitBehaviorTracker().getBehaviorType(currentEntity, caspar))
            .withPathRankerUtilCalculator(pathRankerUtilCalculator);

        for (var path : validPaths) {
            var decisionContext = decisionContextBuilder.withMovePath(path).build();
            decisionContexts.add(decisionContext);
        }
        return decisionContexts;
    }

    private final Map<Integer, RankedPath> memoryOfPastPathsTaken = new HashMap<>();

    @Override
    public Optional<RankedPath> getPastRankedPath(Entity entity) {
        return Optional.ofNullable(memoryOfPastPathsTaken.get(entity.getId()));
    }

    @Override
    public @Nullable RankedPath getBestPath(TreeSet<RankedPath> rankedPaths) {
        var picked = getDecisionMaker().pickOne(rankedPaths);
        if (picked.isEmpty()) {
            return null;
        } else {
            var bestPath = picked.get();
            memoryOfPastPathsTaken.put(bestPath.getPath().getEntity().getId(), bestPath);
            return bestPath;
        }
    }

    @Override
    protected Caspar getOwner() {
        return (Caspar) super.getOwner();
    }
}
