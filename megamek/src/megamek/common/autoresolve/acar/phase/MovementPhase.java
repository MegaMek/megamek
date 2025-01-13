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

import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.alphaStrike.ASRange;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.Action;
import megamek.common.autoresolve.acar.action.MoveAction;
import megamek.common.autoresolve.acar.action.MoveToCoverAction;
import megamek.common.autoresolve.acar.handler.MoveActionHandler;
import megamek.common.autoresolve.acar.handler.MoveToCoverActionHandler;
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
        var action = makeMoveAction(activeFormation, totalMP);
        if (action instanceof MoveAction moveAction) {
            new MoveActionHandler(moveAction, getSimulationManager()).handle();
        } else if (action instanceof MoveToCoverAction moveToCoverAction) {
            new MoveToCoverActionHandler(moveToCoverAction, getSimulationManager()).handle();
        }
    }

    private Action makeMoveAction(Formation activeFormation, int totalMP) {
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

    private Action normalMovement(Formation activeFormation, int totalMP) {

        var target = this.selectTarget(activeFormation);

        if (target.isEmpty()) {
            // Could choose to move left or right based on position, or just return a "no movement"
            int boardMid = getContext().getBoardSize() / 2;
            int direction = (activeFormation.getPosition().coords().getX() > boardMid) ? -1 : 1;
            var destination = new Coords(
                activeFormation.getPosition().coords().getX() + (totalMP * direction),
                0
            );
            return new MoveAction(activeFormation.getId(), Entity.NONE, destination);
        }

        var role = activeFormation.getRole();
        // 3. We have a target
        Formation targetFormation = target.get();
        int targetId = targetFormation.getId();

        // 4. Compute distance to target
        int distToTarget = activeFormation.getPosition().coords()
            .distance(targetFormation.getPosition().coords());
        var pastRound = getContext().getCurrentRound() - 1;

        var lastTurnItTookDamage = activeFormation.getMemory().getInt("wasDamagedAtRound").orElse(-1);
        var lastTurnItTookDamageTarget = activeFormation.getMemory().getInt("lastAttackerId").orElse(-1);
        // 5. If the formation is damaged and the role wants to disengage, consider running away
        if (lastTurnItTookDamage == pastRound && role.disengageIfDamaged() && lastTurnItTookDamageTarget == targetFormation.getId()) {
            // Move away from target
            int direction = (targetFormation.getPosition().coords().getX()
                < activeFormation.getPosition().coords().getX()) ? 1 : -1;

            var destination = new Coords(
                activeFormation.getPosition().coords().getX() + (totalMP * direction),
                0
            );
            return new MoveAction(activeFormation.getId(), targetId, destination);
        }

            // 6. Check if formation is damaged and role wants to drop to cover
        if (activeFormation.getMemory().getBoolean("wasDamagedAtRound").orElse(false) && role.dropToCoverIfDamaged()) {
            // Attempt to find a tile that provides cover within movement range

            var coverCoords = findCoverTile(activeFormation, totalMP);
            if (coverCoords != null) {
                return new MoveToCoverAction(activeFormation.getId(), targetId, coverCoords);
            }
        }

        // 7. Use the role’s preferred range to position ourselves relative to target
        ASRange preferredRange = role.preferredRange();
        ASRange currentRange = ASRange.fromDistance(distToTarget);

        // If we are too far, move closer; if too close, move away, etc.
        int directionToTarget = (targetFormation.getPosition().coords().getX()
            - activeFormation.getPosition().coords().getX()) > 0 ? 1 : -1;

        // Basic example logic: try to "fix" distance to stay in the preferred bracket
        int moveDistance = 0;
        if (currentRange != preferredRange) {
            // Decide to move closer or further to match the bracket
            boolean shouldCloseIn = currentRange.ordinal() > preferredRange.ordinal();
            // The difference in range brackets can be used to guess how many tiles to move
            // For a finer approach, you might measure exact distances for each bracket
            int bracketDiff = Math.abs(currentRange.ordinal() - preferredRange.ordinal()) * 6;

            // Bound our movement by totalMP, and also by the actual distance
            moveDistance = Math.min(totalMP, bracketDiff);

            // If we’re closing in, we move in the direction of the target; otherwise away
            if (!shouldCloseIn) {
                directionToTarget = -directionToTarget;
            }
        }

        // 8. Check if we should move through cover or avoid it
        // If the role does not like to move through cover at the current range, pick an alternate path
        if (!role.moveThroughCover(currentRange)) {
            // Pseudo-logic: attempt to find a path that does not cross cover
            var safePathCoords = calculatePathAvoidingCover(
                activeFormation.getPosition().coords(),
                directionToTarget,
                moveDistance,
                totalMP
            );
            if (safePathCoords != null) {
                return new MoveToCoverAction(activeFormation.getId(), targetId, safePathCoords);
            }
        }

        int finalX = activeFormation.getPosition().coords().getX() + (moveDistance * directionToTarget);
        var destination = new Coords(finalX, 0);

        return new MoveAction(activeFormation.getId(), targetId, destination);
    }

    private Coords calculatePathAvoidingCover(Coords coords, int directionToTarget, int moveDistance, int totalMP) {
        if (totalMP == 0) {
            return null;
        }
        var newDistance = Math.min(totalMP / 2, moveDistance);
        return new Coords(coords.getX() + (newDistance * directionToTarget), 0);
    }

    private Coords findCoverTile(Formation activeFormation, int totalMP) {
        var coord = activeFormation.getEntity().getPosition();
        var direction = Compute.randomInt(2) == 0 ? -1 : 1;
        if (totalMP > 0) {
            return new Coords(coord.getX() + ((int) (totalMP/3.0 * 2.0)) * direction, 0);
        }
        return null;
    }

    private Optional<Formation> selectTarget(Formation actingFormation) {
        var game = getSimulationManager().getGame();
        var player = game.getPlayer(actingFormation.getOwnerId());
        var role = actingFormation.getRole(); // The new Role interface

        // Gather possible targets
        var canBeTargets = getSimulationManager().getGame().getActiveDeployedFormations().stream()
            .filter(f -> actingFormation.getTargetFormationId() == Entity.NONE
                || f.getId() == actingFormation.getTargetFormationId())
            .filter(SBFFormation::isDeployed)
            .filter(f -> game.getPlayer(f.getOwnerId()).isEnemyOf(player))
            .collect(Collectors.toList());

        if (canBeTargets.isEmpty()) {
            return Optional.empty();
        }

        if (role.targetsLastAttacker() && actingFormation.getMemory().getInt("lastAttackerId").orElse(Entity.NONE) != Entity.NONE) {
            var lastAttackerId = actingFormation.getMemory().getInt("lastAttackerId").orElse(Entity.NONE);
            var lastAttacker = canBeTargets.stream()
                .filter(f -> f.getId() == lastAttackerId)
                .findFirst();
            if (lastAttacker.isPresent()) {
                var distance = actingFormation.getPosition().coords().distance(lastAttacker.get().getPosition().coords());
                if (actingFormation.getRole().preferredRange().insideRange(distance)) {
                    return lastAttacker;
                }
            }
        }

        // 2. Sort the candidates by distance (closest first, or some logic you already have)
        canBeTargets.sort((f1, f2) -> {
            var d1 = actingFormation.getPosition().coords().distance(f1.getPosition().coords());
            var d2 = actingFormation.getPosition().coords().distance(f2.getPosition().coords());
            return Double.compare(d1, d2);
        });

        // Possibly reduce to a smaller set
        canBeTargets = canBeTargets.subList(0, Math.min(3, canBeTargets.size()));

        // 3. Insert logic for preferring certain enemy roles
        //    For example, if your formations have a getUnitRole() or something similar:
        List<Formation> preferred = new ArrayList<>();
        List<Formation> normal = new ArrayList<>();
        for (var f : canBeTargets) {
            // Suppose each Formation can reveal its role via f.getRole()
            // If the acting formation’s role says this type is a preferred target, add it to 'preferred'
            if (role.preferredTarget(f.getRole().getRole())) {
                preferred.add(f);
            } else {
                normal.add(f);
            }
        }

        // 4. Shuffle or re-sort the sub-lists to add unpredictability if needed
        Collections.shuffle(preferred);
        Collections.shuffle(normal);

        // 5. Possibly factor in "previous target" from your existing logic
        var previousTargetId = actingFormation.getTargetFormationId();
        Optional<Formation> previousTarget = preferred.stream()
            .filter(f -> f.getId() == previousTargetId)
            .findFirst();
        if (previousTarget.isEmpty()) {
            // If not found among 'preferred', maybe look in 'normal'
            previousTarget = normal.stream()
                .filter(f -> f.getId() == previousTargetId)
                .findFirst();
        }

        // 6. Return in an order that prioritizes:
        //    1) previous target if it’s in the list
        //    2) any in 'preferred'
        //    3) fallback to 'normal'
        if (previousTarget.isPresent()) {
            return previousTarget;
        } else if (!preferred.isEmpty()) {
            return Optional.of(preferred.get(0));
        } else if (!normal.isEmpty()) {
            return Optional.of(normal.get(0));
        }

        return Optional.empty();
    }


    private Optional<Formation> selectTargetOld(Formation actingFormation) {
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
