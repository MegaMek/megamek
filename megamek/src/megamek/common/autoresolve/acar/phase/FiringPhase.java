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
import megamek.common.Entity;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.ASRange;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.Action;
import megamek.common.autoresolve.acar.action.ManeuverToHitData;
import megamek.common.autoresolve.acar.action.StandardUnitAttack;
import megamek.common.autoresolve.component.EngagementControl;
import megamek.common.autoresolve.component.Formation;
import megamek.common.autoresolve.component.FormationTurn;
import megamek.common.enums.GamePhase;
import megamek.common.util.weightedMaps.WeightedDoubleMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

        var actingFormationToHit = ManeuverToHitData.compileToHit(actingFormation);
        var targetFormationToHit = ManeuverToHitData.compileToHit(target);

        ASRange range = ASRange.LONG;
        StandardUnitAttack.ManeuverResult manueverModifier = StandardUnitAttack.ManeuverResult.DRAW;
        if (!actingFormation.isRangeSet(target.getId())) {
            var actingFormationMos = Compute.rollD6(2).getMarginOfSuccess(actingFormationToHit);
            var targetFormationMos = Compute.rollD6(2).getMarginOfSuccess(targetFormationToHit);

            if (actingFormationMos > targetFormationMos) {
                if (EngagementControl.OVERRUN.equals(actingFormation.getEngagementControl())
                    && !actingFormation.isEngagementControlFailed()) {
                    range = ASRange.SHORT;
                } else {
                    range = selectBestRange(actingFormation, range);

                }
                manueverModifier = StandardUnitAttack.ManeuverResult.SUCCESS;
                target.setRange(actingFormation.getId(), range);
            } else if (actingFormationMos < targetFormationMos) {
                range = selectBestRange(target, range);
                manueverModifier = StandardUnitAttack.ManeuverResult.FAILURE;
                target.setRange(actingFormation.getId(), range);
            }

            actingFormation.setRange(target.getId(), range);
        }

        range = actingFormation.getRange(target.getId());

        var maxAttacksOnFailure = Math.max(
            Math.round(actingFormation.getUnits().size() / 2.0),
            Math.min(actingFormation.getUnits().size(), 1));
        var maxAttacksNormally = actingFormation.getUnits().size();

        var maxAttacks = manueverModifier == StandardUnitAttack.ManeuverResult.FAILURE ? maxAttacksOnFailure : maxAttacksNormally;

        for (int i = 0; (i < maxAttacks) && (i < attackRecord.attackingUnits.size()); i++) {
            var unitIndex = attackRecord.attackingUnits.get(i);
            var attack = new StandardUnitAttack(actingFormation.getId(), unitIndex, target.getId(), range, manueverModifier);
            attacks.add(attack);
        }

        getSimulationManager().addAttack(attacks, actingFormation);
    }

    private static ASRange selectBestRange(Formation actingFormation, ASRange range) {
        var stdDamage = actingFormation.getStdDamage();
        if (stdDamage.hasDamage()) {
            var rangeMap = WeightedDoubleMap.of(
                ASRange.LONG, actingFormation.getStdDamage().L.damage,
                ASRange.MEDIUM, actingFormation.getStdDamage().M.damage,
                ASRange.SHORT, actingFormation.getStdDamage().S.damage
            );
            if (!rangeMap.isEmpty()) {
                range = rangeMap.randomItem();
            }
            return range;
        }
        for (var unit : actingFormation.getUnits()) {
            for (var element : unit.getElements()) {
                var elementRange = selectBestRange(element);
                if (elementRange != null) {
                    return elementRange;
                }
            }
        }

        return ASRange.LONG;
    }

    @Nullable
    private static ASRange selectBestRange(AlphaStrikeElement element) {
        var stdDamage = element.getStdDamage();

        if (stdDamage.hasDamage()) {
            var rangeMap = WeightedDoubleMap.of(
                ASRange.LONG, element.getStdDamage().L.damage,
                ASRange.MEDIUM, element.getStdDamage().M.damage,
                ASRange.SHORT, element.getStdDamage().S.damage
            );
            if (!rangeMap.isEmpty()) {
                return rangeMap.randomItem();
            }
        } else {

            var damages = element.getFrontArc().getInternalRepr().values().stream().filter(o -> o instanceof ASDamageVector)
                .map(o -> (ASDamageVector) o).collect(Collectors.toList());
            damages.addAll(element.getLeftArc().getInternalRepr().values().stream().filter(o -> o instanceof ASDamageVector)
                .map(o -> (ASDamageVector) o).toList());
            damages.addAll(element.getRightArc().getInternalRepr().values().stream().filter(o -> o instanceof ASDamageVector)
                .map(o -> (ASDamageVector) o).toList());
            damages.addAll(element.getRearArc().getInternalRepr().values().stream().filter(o -> o instanceof ASDamageVector)
                .map(o -> (ASDamageVector) o).toList());

            damages.add(element.getFrontArc().getStdDamage());
            damages.add(element.getLeftArc().getStdDamage());
            damages.add(element.getRightArc().getStdDamage());
            damages.add(element.getRearArc().getStdDamage());

            var longRangeDamage = damages.stream().mapToDouble(d -> d.L.damage).sum();
            var mediumRangeDamage = damages.stream().mapToDouble(d -> d.M.damage).sum();
            var shortRangeDamage = damages.stream().mapToDouble(d -> d.S.damage).sum();

            var ranges = WeightedDoubleMap.of(
                ASRange.LONG, longRangeDamage,
                ASRange.MEDIUM, mediumRangeDamage,
                ASRange.SHORT, shortRangeDamage);

            if (!ranges.isEmpty()) {
                return ranges.randomItem();
            }
        }

        return null;
    }

    private record AttackRecord(Formation actingFormation, Formation target, List<Integer> attackingUnits) { }

    private List<AttackRecord> attack(Formation actingFormation) {
        var target = this.selectTarget(actingFormation);

        List<Integer> unitIds = new ArrayList<>();
        for (int i = 0; i < actingFormation.getUnits().size(); i++) {
            unitIds.add(i);
        }

        var ret = new ArrayList<AttackRecord>();

        if (target.size() == 2 && unitIds.size() > 1) {
            var sizeOfGroupA = Compute.randomInt(unitIds.size());
            var sizeOfGroupB = unitIds.size() - sizeOfGroupA;

            Collections.shuffle(unitIds);

            var groupA = unitIds.subList(0, sizeOfGroupA);
            var groupB = unitIds.subList(sizeOfGroupA, unitIds.size());

            if (sizeOfGroupA > 0) {
                ret.add(new AttackRecord(actingFormation, target.get(0), groupA));
            }
            if (sizeOfGroupB > 0) {
                ret.add(new AttackRecord(actingFormation, target.get(1), groupB));
            }
        } else if (target.size() == 1) {
            ret.add(new AttackRecord(actingFormation, target.get(0), unitIds));
        }

        return ret;
    }

    private List<Formation> selectTarget(Formation actingFormation) {
        var game = getSimulationManager().getGame();
        var player = game.getPlayer(actingFormation.getOwnerId());
        var canBeTargets = getSimulationManager().getGame().getActiveFormations().stream()
            .filter(Formation::isDeployed)
            .filter(f -> game.getPlayer(f.getOwnerId()).isEnemyOf(player))
            .filter(f -> f.getId() != actingFormation.getTargetFormationId())
            .filter( f -> !formationWasOverrunByFormation(actingFormation, f))
            .filter(f -> !formationWasEvadedByFormation(actingFormation, f))
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

        if (formationWasForcedByFormation(actingFormation, canBeTargets.get(0))) {
            return List.of(canBeTargets.get(0));
        }

        var targetCount = Compute.randomInt(2) + 1;
        return canBeTargets.subList(0, targetCount);
    }

    private boolean formationWasOverrunByFormation(Formation actingFormation, Formation target) {
        return engagementAndControlResult(actingFormation, target, EngagementControl.OVERRUN);
    }

    private boolean formationWasEvadedByFormation(Formation actingFormation, Formation target) {
        return engagementAndControlResult(actingFormation, target, EngagementControl.EVADE);
    }

    private boolean formationWasForcedByFormation(Formation actingFormation, Formation target) {
        return engagementAndControlResult(actingFormation, target, EngagementControl.FORCED_ENGAGEMENT);
    }

    private boolean engagementAndControlResult(Formation actingFormation, Formation target, EngagementControl engagementControl) {
        var engagementControlMemories = actingFormation.getMemory().getMemories("engagementControl");
        var engagement = engagementControlMemories.stream().filter(f -> f.getOrDefault("targetFormationId", Entity.NONE).equals(target.getId()))
            .findFirst(); // there should be only one engagement control memory for this target
        if (engagement.isEmpty()) {
            return false;
        }

        var isAttacker = (boolean) engagement.get().getOrDefault("attacker", false);
        var lostEngagementControl = (boolean) engagement.get().getOrDefault("wonEngagementControl", false);
        var enc = (EngagementControl) engagement.get().get("engagementControl");
        return enc.equals(engagementControl) && lostEngagementControl && !isAttacker;
    }
}
