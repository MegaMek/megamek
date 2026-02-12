/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.autoResolve.acar.phase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import megamek.common.alphaStrike.ASRange;
import megamek.common.autoResolve.acar.SimulationManager;
import megamek.common.autoResolve.acar.action.Action;
import megamek.common.autoResolve.acar.action.StandardUnitAttack;
import megamek.common.autoResolve.component.Formation;
import megamek.common.autoResolve.component.FormationTurn;
import megamek.common.enums.GamePhase;

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

    private record AttackRecord(Formation actingFormation, Formation target, List<Integer> attackingUnits) {}

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
                    case ATTACK_TARGET_NOT_WITHDRAWING ->
                          getSimulationManager().getGame().getActiveDeployedFormations().stream()
                                .filter(f -> game.getPlayer(f.getOwnerId()).isEnemyOf(player))
                                .filter(f -> !f.isWithdrawing())
                                .forEach(canBeTargets::add);

                    case ATTACK_TARGET_WITHDRAWING ->
                          getSimulationManager().getGame().getActiveDeployedFormations().stream()
                                .filter(f -> game.getPlayer(f.getOwnerId()).isEnemyOf(player))
                                .filter(Formation::isWithdrawing)
                                .forEach(canBeTargets::add);

                    default -> { } // intentionally no action
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

            if (distance < 42 && dmg.L().hasDamage() && distance >= 24) {
                if (wasPreviousTarget) {
                    priorityTarget = Optional.of(f);
                }
                pickTarget.add(f);
            } else if (dmg.M().hasDamage() && distance >= 6) {
                if (wasPreviousTarget) {
                    priorityTarget = Optional.of(f);
                }
                pickTarget.add(f);
            } else if (dmg.S().hasDamage() && distance >= 0) {
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
