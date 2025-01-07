/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.autoresolve.acar.phase;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.alphaStrike.ASRange;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.MoveAction;
import megamek.common.autoresolve.acar.handler.MoveActionHandler;
import megamek.common.autoresolve.component.EngagementControl;
import megamek.common.autoresolve.component.Formation;
import megamek.common.autoresolve.component.FormationTurn;
import megamek.common.enums.GamePhase;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.util.weightedMaps.WeightedDoubleMap;

import java.util.*;
import java.util.stream.Collectors;

public class MovementPhase extends PhaseHandler {

    private static final WeightedDoubleMap<EngagementControl> normal = WeightedDoubleMap.of(
        EngagementControl.FORCED_ENGAGEMENT, 1.0,
        EngagementControl.EVADE, 0.0,
        EngagementControl.STANDARD, 1.0,
        EngagementControl.OVERRUN, 0.5,
        EngagementControl.NONE, 0.0
    );

    private static final WeightedDoubleMap<EngagementControl> unsteady =  WeightedDoubleMap.of(
        EngagementControl.FORCED_ENGAGEMENT, 0.5,
        EngagementControl.EVADE, 0.02,
        EngagementControl.STANDARD, 1.0,
        EngagementControl.OVERRUN, 0.1,
        EngagementControl.NONE, 0.01
    );

    private static final WeightedDoubleMap<EngagementControl> shaken =  WeightedDoubleMap.of(
        EngagementControl.FORCED_ENGAGEMENT, 0.2,
        EngagementControl.EVADE, 0.1,
        EngagementControl.STANDARD, 0.8,
        EngagementControl.OVERRUN, 0.05,
        EngagementControl.NONE, 0.01
    );

    private static final WeightedDoubleMap<EngagementControl> broken = WeightedDoubleMap.of(
        EngagementControl.FORCED_ENGAGEMENT, 0.05,
        EngagementControl.EVADE, 1.0,
        EngagementControl.STANDARD, 0.5,
        EngagementControl.OVERRUN, 0.05,
        EngagementControl.NONE, 0.3
    );

    private static final WeightedDoubleMap<EngagementControl> routed = WeightedDoubleMap.of(
        EngagementControl.NONE, 1.0
    );

    private static final Map<Formation.MoraleStatus, WeightedDoubleMap<EngagementControl>> engagementControlOptions = Map.of(
        Formation.MoraleStatus.NORMAL, normal,
        Formation.MoraleStatus.UNSTEADY, unsteady,
        Formation.MoraleStatus.SHAKEN, shaken,
        Formation.MoraleStatus.BROKEN, broken,
        Formation.MoraleStatus.ROUTED, routed
    );

    public MovementPhase(SimulationManager gameManager) {
        super(gameManager, GamePhase.MOVEMENT);
    }

    @Override
    protected void executePhase() {

        while (getSimulationManager().getGame().hasMoreTurns()) {

            var optTurn = getSimulationManager().getGame().changeToNextTurn();
            if (optTurn.isEmpty()) {
                break;
            }
            var turn = optTurn.get();

            if (turn instanceof FormationTurn formationTurn) {
                var player = getSimulationManager().getGame().getPlayer(formationTurn.playerId());
                getSimulationManager().getGame().getActiveFormations(player)
                    .stream()
                    .filter(f -> f.isEligibleForPhase(getSimulationManager().getGame().getPhase())) // only eligible formations
                    .findAny()
                    .ifPresent(this::engage);
            }
        }
    }

    private void engage(Formation activeFormation) {
        activeFormation.setDone(true);
        var noMP = activeFormation.getCurrentMovement() <= 0;
        if (noMP) {
            return;
        }

        moveUnit(activeFormation);
    }

    private void moveUnit(Formation activeFormation) {
        var totalMP = activeFormation.getCurrentMovement();
        var moveAction = makeMoveAction(activeFormation, totalMP);
        new MoveActionHandler(moveAction, getSimulationManager()).handle();
    }

    private MoveAction makeMoveAction(Formation activeFormation, int totalMP) {
        if (activeFormation.isWithdrawing()) {
            return withdrawingMovement(activeFormation, totalMP);
        } else {
            return normalMovement(activeFormation, totalMP);
        }
    }

    private MoveAction withdrawingMovement(Formation activeFormation, int totalMP) {
        var direction = -1;
        if (activeFormation.getPosition().coords().getX() > getContext().getBoardSize() / 2) {
            direction = 1;
        }

        var destination = new Coords(activeFormation.getPosition().coords().getX() + (totalMP * direction), 0);
        return new MoveAction(
            activeFormation.getId(),
            -1,
            destination);
    }

    private MoveAction normalMovement(Formation activeFormation, int totalMP) {
        /**
         * Scout
         * Ambusher
         * Cavalry
         * Hybrid
         * Linebreaker
         * Battleline
         * - 4/6 - high defense - brawlers usually / long or medium range
         * - most damage is in the brawl range, 6 or less (short)
         * - take and receive damage
         * Bodyguard
         * Ranged
         * - 4/6 3/5 -
         */


        var target = this.selectTarget(activeFormation);
        int distToTarget = target.map(f -> activeFormation.getPosition().coords().distance(f.getPosition().coords())).orElse(0);

        if (distToTarget > totalMP) {
            distToTarget = totalMP;
        } else if (distToTarget == 0) {
            distToTarget = totalMP;
        }
        var targetId = -1;
        var direction = 1;
        if (target.isPresent()) {
            direction = target.map(f -> f.getPosition().coords().getX() - activeFormation.getPosition().coords().getX()).orElse(0) > 0 ? 1 : -1;
            // how much the target can move?
            var targetTotalMP = target.get().getCurrentMovement();
            targetId = target.get().getId();
            // I want to keep the enemy in a distance bracket that allows me to dish max amount of damage while minimizing the damage I take
            // and according to my role, the distance I want to keep changes
            var distance = ASRange.fromDistance(distToTarget);
            switch (distance) {
                case EXTREME -> {
                    if (targetTotalMP > 0) {
                        distToTarget = Math.min(distToTarget, targetTotalMP);
                    }
                }
                case LONG -> {
                    if (targetTotalMP > 0) {
                        distToTarget = Math.min(distToTarget, targetTotalMP);
                    }
                }
                case MEDIUM -> {
                    if (targetTotalMP > 0) {
                        distToTarget = Math.min(distToTarget, targetTotalMP);
                    }
                }
                case SHORT -> {
                    if (targetTotalMP > 0) {
                        distToTarget = Math.min(distToTarget, targetTotalMP);
                    }
                }
            }
        } else {
            if (activeFormation.getPosition().coords().getX() > getContext().getBoardSize() / 2) {
                direction = -1;
            }
        }

        var destination = new Coords((activeFormation.getPosition().coords().getX() + (distToTarget * direction)), 0);
        return new MoveAction(activeFormation.getId(), targetId, destination);
    }

    private Optional<Formation> selectTarget(Formation actingFormation) {
        var game = getSimulationManager().getGame();
        var player = game.getPlayer(actingFormation.getOwnerId());
        var canBeTargets = getSimulationManager().getGame().getActiveDeployedFormations().stream()
            .filter(f -> actingFormation.getTargetFormationId() == Entity.NONE || f.getId() == actingFormation.getTargetFormationId())
            .filter(SBFFormation::isDeployed)
            .filter(f -> game.getPlayer(f.getOwnerId()).isEnemyOf(player))
            .collect(Collectors.toList());

        if (canBeTargets.isEmpty()) {
            return Optional.empty();
        }

        canBeTargets.sort((f1, f2) -> {
            var d1 = actingFormation.getPosition().coords().distance(f1.getPosition().coords());
            var d2 = actingFormation.getPosition().coords().distance(f2.getPosition().coords());
            return Double.compare(d1, d2);
        });

        canBeTargets = canBeTargets.subList(0, Math.min(3, canBeTargets.size()));
        var previousTargetId = actingFormation.getTargetFormationId();
        var pickTarget = new ArrayList<Formation>();
        Optional<Formation> previousTarget = Optional.empty();

        for (var f : canBeTargets) {
            var distance = actingFormation.getPosition().coords().distance(f.getPosition().coords());
            var dmg = actingFormation.getStdDamage();

            var wasPreviousTarget = f.getId() == previousTargetId;

            if (dmg.L.hasDamage() && distance >= 24 && distance < 42) {
                if (wasPreviousTarget) {
                    previousTarget = Optional.of(f);
                }
                pickTarget.add(f);
            } else if (dmg.M.hasDamage() && distance >= 6) {
                if (wasPreviousTarget) {
                    previousTarget = Optional.of(f);
                }
                pickTarget.add(f);
            } else if (dmg.S.hasDamage() && distance >= 0) {
                if (wasPreviousTarget) {
                    previousTarget = Optional.of(f);
                }
                pickTarget.add(f);
            }
        }

        Collections.shuffle(pickTarget);

        List<Formation> finalCanBeTargets = canBeTargets;

        return previousTarget.or(() -> pickTarget.stream().findAny()).or(() -> finalCanBeTargets.stream().findAny());
    }
}
