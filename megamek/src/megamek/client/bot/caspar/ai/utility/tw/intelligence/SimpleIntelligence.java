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
import megamek.client.bot.caspar.ai.utility.tw.decision.TWDecisionContext;
import megamek.client.bot.caspar.ai.utility.tw.profile.TWProfile;
import megamek.client.bot.princess.BasicPathRanker;
import megamek.client.bot.princess.FiringPhysicalDamage;
import megamek.client.bot.princess.RankedPath;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.MovePath;
import megamek.common.Targetable;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

import java.util.*;

/**
 * SimpleIntelligence does not implement memory or learning in any way, has no stickiness for decisions, and does not
 * implement any special behaviors, uses BEST for decision process.
 *
 * @author Luana Coppio
 */
public class SimpleIntelligence extends BasicPathRanker implements Intelligence {
    private final static MMLogger logger = MMLogger.create(SimpleIntelligence.class);
    private final List<Decision> decisions = new ArrayList<>();
    private final DecisionMaker decisionMaker = new DecisionScorer();
    private final PathRankerUtilCalculator pathRankerUtilCalculator;

    public SimpleIntelligence(Caspar caspar, TWProfile profile) {
        super(caspar);
        this.decisions.addAll(profile.getDecisions());
        pathRankerUtilCalculator = new SimplePathRankerUtilCalculator(caspar, caspar.getGame(), this, getPathRankerState());
    }

    @Override
    public void update(Intelligence intelligence) {
        // there is nothing to update
    }

    @Override
    public List<Decision> getDecisions() {
        return decisions;
    }

    @Override
    public DecisionMaker getDecisionMaker() {
        return decisionMaker;
    }

    @Override
    public double getBonusFactor(Targetable entity) {
        // Anything can be a bonus, we just return 1.0 here as a kind of "no op"
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

    private List<DecisionContext> createDecisionContexts(List<MovePath> validPaths, Entity currentEntity, List<Entity> enemies) {
        List<DecisionContext> decisionContexts = new ArrayList<>(validPaths.size());
        Caspar caspar = getOwner();
        var decisionContextBuilder = TWDecisionContext.TWDecisionContextBuilder.aTWDecisionContext()
            .withBehaviorSettings(caspar.getBehaviorSettings())
            .withCurrentUnit(currentEntity)
            .withFireControlState(caspar.getFireControlState())
            .withIntelligence(this)
            .withWorld(caspar.getWorld())
            .withWaypoint(caspar.getUnitBehaviorTracker().getWaypointForEntity(currentEntity).orElse(null))
            .withUnitBehaviorType(caspar.getUnitBehaviorTracker().getBehaviorType(currentEntity, caspar))
            .withPathRankerUtilCalculator(pathRankerUtilCalculator)
            .withCachedDamage(new FiringPhysicalDamage())
            .withDamageCache(new HashMap<>())
            .withStrategicGoalsManager(caspar.getStrategicGoalsManager());


        for (var path : validPaths) {
            var decisionContext = decisionContextBuilder.withMovePath(path).withWaypoint(path.getWaypoint()).build();
            decisionContexts.add(decisionContext);
        }
        return decisionContexts;
    }

    private final Map<Integer, RankedPath> memoryOfPastPathsTaken = new HashMap<>();

    @Override
    public Optional<RankedPath> getPastRankedPath(Targetable entity) {
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
