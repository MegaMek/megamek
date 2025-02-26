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
import megamek.common.alphaStrike.ASRange;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.Action;
import megamek.common.autoresolve.acar.action.StandardUnitAttack;
import megamek.common.autoresolve.component.Formation;
import megamek.common.autoresolve.component.FormationTurn;
import megamek.common.enums.GamePhase;

import java.util.*;

public class FiringPhase extends PhaseHandler {

    public FiringPhase(SimulationManager gameManager) {
        super(gameManager, GamePhase.FIRING);
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
                for (var formation : getContext().getActiveFormations(formationTurn.playerId())) {
                    if (!formation.isEligibleForPhase(getContext().getPhase())) {
                        continue;
                    }

                    for (var atk : attack(formation)) {
                        standardUnitAttacks(atk);
                    }
                }
            }
        }
    }

    private void standardUnitAttacks(AttackRecord attackRecord) {
        var actingFormation = attackRecord.actingFormation;
        var target = attackRecord.target;

        var attacks = new ArrayList<Action>();

        if (!actingFormation.isRangeSet(target.getId())) {
            var distance = actingFormation.getPosition().coords().distance(target.getPosition().coords());

            var range = ASRange.fromDistance(distance);

            target.setRange(actingFormation.getId(), range);
            actingFormation.setRange(target.getId(), range);
        }

        var range = actingFormation.getRange(target.getId());
        var maxAttacks = actingFormation.getUnits().size();
        if (range.ordinal() < ASRange.EXTREME.ordinal()) {
            for (int i = 0; (i < maxAttacks) && (i < attackRecord.attackingUnits.size()); i++) {
                var unitIndex = attackRecord.attackingUnits.get(i);
                var attack = new StandardUnitAttack(actingFormation.getId(), unitIndex, target.getId(), range);
                attacks.add(attack);
            }
            target.addBeingTargetedBy(actingFormation);
            getSimulationManager().addAttack(attacks, actingFormation);
        }
    }

    private record AttackRecord(Formation actingFormation, Formation target, List<Integer> attackingUnits) { }

    private List<AttackRecord> attack(Formation actingFormation) {
        var target = this.selectTarget(actingFormation);

        if (target.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> unitIds = new ArrayList<>();
        for (int i = 0; i < actingFormation.getUnits().size(); i++) {
            unitIds.add(i);
        }

        var ret = new ArrayList<AttackRecord>();

        ret.add(new AttackRecord(actingFormation, target.get(0), unitIds));
        return ret;
    }

    private List<Formation> selectTarget(Formation actingFormation) {
        var game = getSimulationManager().getGame();
        var player = game.getPlayer(actingFormation.getOwnerId());
        var canBeTargets = new HashSet<Formation>();
        var orders = getContext().getOrders().getOrders(actingFormation.getOwnerId());

        for (var order : orders) {
            if (order.isEligible(getContext())) {
                switch (order.getOrderType()) {
                    case ATTACK_TARGET -> {
                        if (order.getTargetId() != -1) {
                            var target = game.getFormation(order.getTargetId());
                            target.ifPresent(canBeTargets::add);
                        }
                    }
                    case ATTACK_TARGET_NOT_WITHDRAWING -> getSimulationManager().getGame().getActiveDeployedFormations().stream()
                        .filter(f -> game.getPlayer(f.getOwnerId()).isEnemyOf(player))
                        .filter(f -> !f.isWithdrawing())
                        .forEach(canBeTargets::add);

                    case ATTACK_TARGET_WITHDRAWING -> getSimulationManager().getGame().getActiveDeployedFormations().stream()
                        .filter(f -> game.getPlayer(f.getOwnerId()).isEnemyOf(player))
                        .filter(Formation::isWithdrawing)
                        .forEach(canBeTargets::add);
                }
            }
        }

        // sticky target
        game.getFormation(actingFormation.getTargetFormationId())
            .ifPresent(canBeTargets::add);

        if (canBeTargets.size() < 4) {
            getSimulationManager().getGame().getActiveDeployedFormations().stream()
                .filter(f -> game.getPlayer(f.getOwnerId()).isEnemyOf(player))
                .forEach(canBeTargets::add);
        }

        if (canBeTargets.isEmpty()) {
            return Collections.emptyList();
        }

        if (canBeTargets.size() == 1) {
            return new ArrayList<>(canBeTargets);
        }

        return bestTargetOrPreviousTarget(actingFormation, canBeTargets);

    }

    private List<Formation> bestTargetOrPreviousTarget(Formation actingFormation, Set<Formation> targets) {
        var previousTargetId = actingFormation.getTargetFormationId();

        var pickTarget = new LinkedList<Formation>();

        Optional<Formation> priorityTarget = Optional.empty();
        for (var f : targets) {
            var distance = actingFormation.getPosition().coords().distance(f.getPosition().coords());
            var dmg = actingFormation.getStdDamage();

            var wasPreviousTarget = f.getId() == previousTargetId;

            if (distance < 42 && dmg.L.hasDamage() && distance >= 24) {
                if (wasPreviousTarget) {
                    priorityTarget = Optional.of(f);
                }
                pickTarget.add(f);
            } else if (dmg.M.hasDamage() && distance >= 6) {
                if (wasPreviousTarget) {
                    priorityTarget = Optional.of(f);
                }
                pickTarget.add(f);
            } else if (dmg.S.hasDamage() && distance >= 0) {
                if (wasPreviousTarget) {
                    priorityTarget = Optional.of(f);
                }
                pickTarget.add(f);
            }
        }

        Collections.shuffle(pickTarget);
        priorityTarget.ifPresent(formation -> pickTarget.add(0, formation));

        var iterator = pickTarget.iterator();
        while (iterator.hasNext()) {
            var target = iterator.next();
            if (target.beingTargetByHowMany() > 2 && iterator.hasNext()) {
                continue;
            }
            return List.of(target);
        }
        return Collections.emptyList();
    }
}
