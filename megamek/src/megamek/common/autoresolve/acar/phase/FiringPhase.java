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

import megamek.common.alphaStrike.ASRange;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.Action;
import megamek.common.autoresolve.acar.action.StandardUnitAttack;
import megamek.common.autoresolve.component.Formation;
import megamek.common.autoresolve.component.FormationTurn;
import megamek.common.enums.GamePhase;

import java.util.*;
import java.util.stream.Collectors;

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
                var player = getSimulationManager().getGame().getPlayer(formationTurn.playerId());

                getSimulationManager().getGame().getActiveFormations(player)
                    .stream()
                    .filter(f -> f.isEligibleForPhase(getSimulationManager().getGame().getPhase())) // only eligible formations
                    .findAny()
                    .map(this::attack)
                    .stream()
                    .flatMap(Collection::stream)
                    .forEach(this::standardUnitAttacks); // add engage and control action
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
            getSimulationManager().addAttack(attacks, actingFormation);
        }
    }

    private record AttackRecord(Formation actingFormation, Formation target, List<Integer> attackingUnits) { }

    private List<AttackRecord> attack(Formation actingFormation) {
        var target = this.selectTarget(actingFormation);

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
        var canBeTargets = getSimulationManager().getGame().getActiveFormations().stream()
            .filter(Formation::isDeployed)
            .filter(f -> game.getPlayer(f.getOwnerId()).isEnemyOf(player))
            .filter(f -> f.getId() != actingFormation.getTargetFormationId())
            .collect(Collectors.toList());
        Collections.shuffle(canBeTargets);

        var mandatoryTarget = game.getFormation(actingFormation.getTargetFormationId());
        mandatoryTarget.ifPresent(formation -> canBeTargets.add(0, formation));

        if (canBeTargets.isEmpty()) {
            return List.of();
        }

        if (canBeTargets.size() == 1) {
            return canBeTargets;
        }

        return bestTargetOrPreviousTarget(actingFormation, canBeTargets);
    }

    private List<Formation> bestTargetOrPreviousTarget(Formation actingFormation, List<Formation> targets) {
        var game = getSimulationManager().getGame();
        var previousTarget = game.getFormation(actingFormation.getTargetFormationId());

        if (previousTarget.isEmpty()) {
            return List.of(targets.get(0));
        }

        var previousTargetFormation = previousTarget.get();

        var pickTarget = new ArrayList<Formation>();
        var previousTargetId = previousTargetFormation.getId();
        Optional<Formation> priorityTarget = Optional.empty();
        for (var f : targets) {
            var distance = actingFormation.getPosition().coords().distance(f.getPosition().coords());
            var dmg = actingFormation.getStdDamage();

            var wasPreviousTarget = f.getId() == previousTargetId;

            if (distance >= 42) {
                // No extreme range on ACAR
            } else if (dmg.L.hasDamage() && distance >= 24) {
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

        return List.of(targets.get(0));
    }
}
