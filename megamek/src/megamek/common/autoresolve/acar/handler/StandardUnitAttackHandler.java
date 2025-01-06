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
package megamek.common.autoresolve.acar.handler;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Roll;
import megamek.common.alphaStrike.ASDamage;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.ASRange;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.AttackToHitData;
import megamek.common.autoresolve.acar.action.StandardUnitAttack;
import megamek.common.autoresolve.acar.report.AttackReporter;
import megamek.common.autoresolve.component.EngagementControl;
import megamek.common.autoresolve.component.Formation;
import megamek.common.strategicBattleSystems.SBFUnit;
import megamek.common.util.weightedMaps.WeightedDoubleMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StandardUnitAttackHandler extends AbstractActionHandler {

    private final AttackReporter reporter;

    public StandardUnitAttackHandler(StandardUnitAttack action, SimulationManager gameManager) {
        super(action, gameManager);
        this.reporter = new AttackReporter(game(), this::addReport);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isFiring();
    }

    @Override
    public void execute() {
        var attack = (StandardUnitAttack) getAction();
        var attackerOpt = game().getFormation(attack.getEntityId());
        var targetOpt = game().getFormation(attack.getTargetId());
        var attacker = attackerOpt.orElseThrow();
        var target = targetOpt.orElseThrow();

        // Using simplified damage as of Interstellar Operations (BETA) page 241
        var targetUnitOpt = ObjectUtility.getRandomItemSafe(target.getUnits());
        var targetUnit = targetUnitOpt.orElseThrow();
        resolveAttack(attacker, attack, target, targetUnit);
    }

    private void resolveAttack(Formation attacker, StandardUnitAttack attack, Formation target, SBFUnit targetUnit) {
        var attackingUnit = attacker.getUnits().get(attack.getUnitNumber());
        var toHit = AttackToHitData.compileToHit(game(), attack);

        // Start of attack report
        reporter.reportAttackStart(attacker, attack.getUnitNumber(), target);

        if (toHit.cannotSucceed()) {
            reporter.reportCannotSucceed(toHit.getDesc());
        } else {
            reporter.reportToHitValue(toHit);
            Roll roll = Compute.rollD6(2);
            reporter.reportAttackRoll(roll, attacker);

            if (roll.getIntValue() < toHit.getValue()) {
                reporter.reportAttackMiss();
            } else {
                reporter.reportAttackHit();
                var damage = calculateDamage(attacker, attack, attackingUnit, target);


                applyDamage(target, targetUnit, damage, attackingUnit);
            }
        }
    }

    private void applyDamage(Formation target, SBFUnit targetUnit, int[] damage, SBFUnit attackingUnit) {
        var elements = new ArrayList<>(targetUnit.getElements());

        var totalDamageApplied = 0;
        // distribute the damage between elements
        for (int dmg : damage) {
            if (dmg == 0) {
                continue;
            }

            // shuffle so the damage is applied to a random element sequence.
            Collections.shuffle(elements);

            // a single element can only cause damage to a single element at a time
            // so here we try to hit one, if it has armor we apply damage to its armor, if
            // the armor is 0 and it has structure and there is damage left we hit its structure,
            // we then stop here.
            for (AlphaStrikeElement element : elements) {
                var elementStructure = element.getCurrentStructure();
                if (elementStructure == 0) {
                    continue;
                }

                var elementArmor = element.getCurrentArmor();
                if (elementArmor > 0) {
                    var elementDamage = Math.min(dmg, elementArmor);
                    element.setCurrentArmor(elementArmor - elementDamage);
                    dmg -= elementDamage;
                    totalDamageApplied += elementDamage;
                }
                if (elementStructure > 0 && elementArmor == 0) {
                    var elementDamage = Math.min(dmg, elementStructure);
                    element.setCurrentStructure(elementStructure - elementDamage);
                    totalDamageApplied += elementDamage;
                }
                // if damage was applied to an element, we stop here and move to the next damage attack thingy
                // a single attack cannot deal damage to more than one element at a time
                if (totalDamageApplied > 0) {
                    break;
                }
            }
        }

        targetUnit.setCurrentArmor(Math.max(0, targetUnit.getCurrentArmor() - totalDamageApplied));

        reporter.reportDamageDealt(targetUnit, totalDamageApplied, targetUnit.getCurrentArmor());

        if (targetUnit.getCurrentArmor() * 2 < totalDamageApplied) {
            target.setHighStressEpisode();
            reporter.reportStressEpisode();
        }

        if (target.isCrippled() && targetUnit.getCurrentArmor() > 0) {
            reporter.reportStressEpisode();
            target.setHighStressEpisode();
            reporter.reportUnitCrippled();
        }

        if (targetUnit.getCurrentArmor() <= 0) {
            // Destroyed
            reporter.reportUnitDestroyed();
            target.setHighStressEpisode();
            countKill(attackingUnit, targetUnit);
        } else {
            // Check for critical hits if armor is now less than half original
            if (targetUnit.getCurrentArmor() * 2 < targetUnit.getArmor()) {
                reporter.reportCriticalCheck();
                var critRoll = Compute.rollD6(2);
                var criticalRollResult = critRoll.getIntValue();
                handleCrits(target, targetUnit, criticalRollResult, attackingUnit);
            }
        }
    }

    private int[] calculateDamage(Formation attacker, StandardUnitAttack attack,
                                SBFUnit attackingUnit, Formation target) {
        int bonusDamage = 0;
        if (attack.getManeuverResult().equals(StandardUnitAttack.ManeuverResult.SUCCESS)) {
            bonusDamage += 1;
        }

        var damage = attackingUnit.getElements().stream().mapToInt(e -> getDamage(e, attack.getRange())).toArray();
        return processDamageByEngagementControl(attacker, target, bonusDamage, damage);
    }

    private enum ArcSelection {
        FRONT, LEFT, RIGHT, REAR
    }

    private int getDamage(AlphaStrikeElement element, ASRange range) {
        var stdDamage = element.getStandardDamage();
        var specialDmgVectors = element.getSpecialAbilities().getInternalRepr().values().stream()
            .filter(o -> o instanceof ASDamageVector).map(o -> (ASDamageVector) o).toList();

        if (stdDamage.hasDamage()) {
            return stdDamage.getDamage(range).damage;
        } else if (!specialDmgVectors.isEmpty()) {
            return specialDmgVectors.stream().mapToInt(d -> d.getDamage(range).damage).sum();
        }

        var frontArcDamages = element.getFrontArc().getInternalRepr().values().stream().filter(o -> o instanceof ASDamageVector)
            .map(o -> (ASDamageVector) o).toList();
        var leftArcDamages = element.getLeftArc().getInternalRepr().values().stream().filter(o -> o instanceof ASDamageVector)
            .map(o -> (ASDamageVector) o).toList();
        var rightArcDamages = element.getRightArc().getInternalRepr().values().stream().filter(o -> o instanceof ASDamageVector)
            .map(o -> (ASDamageVector) o).toList();
        var rearArcDamages = element.getRearArc().getInternalRepr().values().stream().filter(o -> o instanceof ASDamageVector)
            .map(o -> (ASDamageVector) o).toList();

        var frontArcDmgTotal = frontArcDamages.stream().mapToInt(d -> d.getDamage(range).damage).sum();
        var leftArcDmgTotal = leftArcDamages.stream().mapToInt(d -> d.getDamage(range).damage).sum();
        var rightArcDmgTotal = rightArcDamages.stream().mapToInt(d -> d.getDamage(range).damage).sum();
        var rearArcDmgTotal = rearArcDamages.stream().mapToInt(d -> d.getDamage(range).damage).sum();

        var arcSelectionWeightedDoubleMap = WeightedDoubleMap.of(
            ArcSelection.FRONT, frontArcDmgTotal,
            ArcSelection.LEFT, leftArcDmgTotal,
            ArcSelection.RIGHT, rightArcDmgTotal,
            ArcSelection.REAR, rearArcDmgTotal);

        if (!arcSelectionWeightedDoubleMap.isEmpty()) {
            switch (arcSelectionWeightedDoubleMap.randomItem()) {
                case FRONT -> {
                    return frontArcDmgTotal;
                }
                case LEFT -> {
                    return leftArcDmgTotal;
                }
                case RIGHT -> {
                    return rightArcDmgTotal;
                }
                case REAR -> {
                    return rearArcDmgTotal;
                }
            }
        }

        return 0;
    }

    private void handleCrits(Formation target, SBFUnit targetUnit, int criticalRollResult, SBFUnit attackingUnit) {
        switch (criticalRollResult) {
            case 2, 3, 4 -> reporter.reportNoCrit();
            case 5, 6, 7 -> {
                targetUnit.addTargetingCrit();
                reporter.reportTargetingCrit(targetUnit);
            }
            case 8, 9 -> {
                targetUnit.addDamageCrit();
                reporter.reportDamageCrit(targetUnit);
            }
            case 10, 11 -> {
                targetUnit.addTargetingCrit();
                targetUnit.addDamageCrit();
                reporter.reportTargetingCrit(targetUnit);
                reporter.reportDamageCrit(targetUnit);
            }
            default -> {
                countKill(attackingUnit, targetUnit);
                targetUnit.setCurrentArmor(0);
                target.setHighStressEpisode();
                reporter.reportUnitDestroyed();
            }
        }
    }

    private void countKill(SBFUnit attackingUnit, SBFUnit targetUnit) {
        var killers = attackingUnit.getElements().stream().map(AlphaStrikeElement::getId)
            .map(e -> simulationManager().getGame().getEntity(e)).filter(Optional::isPresent).map(Optional::get).toList();
        var targets = targetUnit.getElements().stream().map(AlphaStrikeElement::getId)
            .map(e -> simulationManager().getGame().getEntity(e)).filter(Optional::isPresent).map(Optional::get).toList();
        for (var target : targets) {
            ObjectUtility.getRandomItemSafe(killers).ifPresent(e -> e.addKill(target));
        }
    }

    private int[] processDamageByEngagementControl(Formation attacker, Formation target, int bonusDamage, int[] damage) {
        var engagementControlMemories = attacker.getMemory().getMemories("engagementControl");
        var engagement = engagementControlMemories
            .stream()
            .filter(f -> f.getOrDefault("targetFormationId", Entity.NONE).equals(target.getId()))
            .findFirst(); // there should be only one engagement control memory for this target

        if (damage.length > 0) {
            damage[0] += bonusDamage;
        }

        if (engagement.isEmpty()) {
            return damage;
        }

        var isAttacker = (boolean) engagement.get().getOrDefault("attacker", false);

        if (!isAttacker) {
            return damage;
        }

        var wonEngagement = (boolean) engagement.get().getOrDefault("wonEngagementControl", false);
        var engagementControl = (EngagementControl) engagement.get().getOrDefault("engagementControl", EngagementControl.NONE);
        if (wonEngagement) {
            return processDamageEngagementControlVictory(engagementControl, damage);
        }
        return processDamageEngagementControlDefeat(engagementControl, damage);
    }

    private int[] processDamageEngagementControlDefeat(EngagementControl engagementControl, int[] damage) {
        if (engagementControl == EngagementControl.EVADE) {
            for (int i = 0; i < damage.length; i++) {
                damage[i] = (int) ((double) damage[i] * 0.5);
            }
        }
        return damage;
    }

    private int[] processDamageEngagementControlVictory(EngagementControl engagementControl, int[] damage) {
        return switch(engagementControl) {
            case OVERRUN -> {
                for (var i = 0; i < damage.length; i++) {
                    damage[i] = (int) ((double) damage[i] * 0.25);
                }
                yield damage;
            }
            case FORCED_ENGAGEMENT -> {
                for (var i = 0; i < damage.length; i++) {
                    damage[i] = (int) ((double) damage[i] * 0.5);
                }
                yield damage;
            }
            default -> damage;
        };
    }
}
