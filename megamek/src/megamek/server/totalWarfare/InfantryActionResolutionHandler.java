/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.server.totalWarfare;

import java.util.ArrayList;
import java.util.List;

import megamek.common.HitData;
import megamek.common.InfantryCombatResult;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.InfantryCombatCasualties;
import megamek.common.compute.InfantryCombatTables;
import megamek.common.compute.MarinePointsScoreCalculator;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.InfantryActionTracker.InfantryAction;

/**
 * Resolves one infantry vs. infantry action roll during the End Phase (TO:AR pp. 172-174).
 *
 * <p>Each side's Marine Points Score sets the odds column; the 2D6 result gives each side a percentage of the
 * <em>other</em> side's strength to lose, rounded up. A repulsed attacker loses double, a defender in full control
 * loses half, and an attacker who announced a withdrawal loses half and cannot be eliminated this roll. The Marine
 * Points lost are then converted back into troopers and crew by the share of the side's own strength they represent,
 * rounding each unit down (p. 174).</p>
 */
class InfantryActionResolutionHandler extends AbstractTWRuleHandler {
    private static final MMLogger LOGGER = MMLogger.create(InfantryActionResolutionHandler.class);

    /** Standard-scale damage that eliminates one battle armor trooper in the complex conversion (TO:AR p. 174). */
    private static final int DAMAGE_PER_TROOPER = 10;

    /** A structure damage roll of this value damages every hex of the building (TO:AR p. 172). */
    private static final int STRUCTURE_DAMAGE_ROLL = 12;

    private static final int ELIMINATED_PERCENT = 100;

    private final InfantryActionTracker tracker;
    private final InfantryActionReporter reporter;

    InfantryActionResolutionHandler(TWGameManager gameManager, InfantryActionTracker tracker) {
        super(gameManager);
        this.tracker = tracker;
        this.reporter = new InfantryActionReporter(gameManager);
    }

    /**
     * Rolls 2D6 and resolves the action.
     *
     * @param combat the action to resolve
     */
    void resolve(InfantryAction combat) {
        resolve(combat, Compute.d6(2));
    }

    /**
     * Resolves the action for an already-rolled result. Exposed for tests.
     *
     * @param combat the action to resolve
     * @param roll   the 2D6 result on the Infantry vs. Infantry Action Table
     */
    void resolve(InfantryAction combat, int roll) {
        Entity targetEntity = getGame().getEntity(combat.targetId);
        if (!(targetEntity instanceof AbstractBuildingEntity building)) {
            LOGGER.debug("[InfantryAction] target {} is gone; ending the action", combat.targetId);
            cleanupCombat(combat);
            return;
        }
        boolean withdrawing = attackersAreWithdrawing(combat);

        // Only the defender's score takes the building modifier (TO:AR p. 171); the conversion back to people
        // uses each side's score before the modifier (TO:AR p. 174).
        int attackerStrength = totalMarinePoints(combat.attackerIds, null);
        int defenderStrength = totalMarinePoints(combat.defenderIds, building);
        int attackerOwnStrength = attackerStrength;
        int defenderOwnStrength = totalMarinePoints(combat.defenderIds, null);
        LOGGER.debug("[InfantryAction] {}: attackers {} (own {}), defenders {} (own {}), withdrawing {}",
              building.getShortName(), attackerStrength, attackerOwnStrength, defenderStrength,
              defenderOwnStrength, withdrawing);

        if (attackerStrength <= 0) {
            reportCombatHeader(building);
            reportSideEliminated(combat, true);
            return;
        }
        if (defenderStrength <= 0) {
            reportCombatHeader(building);
            reportSideEliminated(combat, false);
            return;
        }

        String ratio = InfantryCombatTables.calculateRatio(attackerStrength, defenderStrength);
        InfantryCombatResult result = InfantryCombatTables.resolveAction(ratio, roll);
        reportCombatHeader(building);
        reporter.reportSides(combat.attackerIds, combat.defenderIds, building);
        reportCombatRatio(attackerStrength, defenderStrength, ratio);
        reportCombatRoll(roll, result);

        boolean attackerEliminated = result.getAttackerCasualtiesPercent() >= ELIMINATED_PERCENT;
        boolean defenderEliminated = result.isDefenderEliminated();
        int attackerPercent = result.getAttackerCasualtiesPercent();
        int defenderPercent = result.getDefenderCasualtiesPercent();
        if (withdrawing) {
            // TO:AR p. 172: the roll is still made, the attacker suffers only half damage, and any E result becomes
            // a P result. The table prints no percentage for an eliminated side, so the converted E uses the
            // highest percentage printed in that column for that side.
            if (attackerEliminated) {
                attackerPercent = InfantryCombatTables.highestListedPercent(ratio, true);
                attackerEliminated = false;
            }
            if (defenderEliminated) {
                defenderPercent = InfantryCombatTables.highestListedPercent(ratio, false);
                defenderEliminated = false;
            }
            reportWithdrawal(attackerPercent, defenderPercent);
        }

        // TO:AR p. 172-173: the defenders take full damage from the roll that gives the attackers partial control
        boolean defendersInFullControl = !combat.hasPartialControl;
        if (result.isPartialControl() || defenderEliminated) {
            combat.hasPartialControl = true;
            defendersInFullControl = false;
        }

        // A withdrawing attacker is already leaving, so a repulse does not double its casualties on top of the halving
        boolean attackerRepulsed = result.isAttackerRepulsed() && !withdrawing;
        int attackerLost = attackerEliminated ? attackerOwnStrength
              : InfantryCombatCasualties.marinePointsLost(defenderStrength, attackerPercent, attackerRepulsed,
                    withdrawing);
        int defenderLost = defenderEliminated ? defenderOwnStrength
              : InfantryCombatCasualties.marinePointsLost(attackerStrength, defenderPercent, false,
                    defendersInFullControl);
        reportMarinePointsLost(5642, attackerLost, attackerOwnStrength);
        reportMarinePointsLost(5643, defenderLost, defenderOwnStrength);
        LOGGER.info("[InfantryAction] {}: ratio {} roll {} result {}; attackers lose {} of {}, defenders lose {} of {}",
              building.getShortName(), ratio, roll, result, attackerLost, attackerOwnStrength, defenderLost,
              defenderOwnStrength);

        int attackerPersonnelLost = applySideLosses(combat, true, attackerLost, attackerOwnStrength);
        int defenderPersonnelLost = applySideLosses(combat, false, defenderLost, defenderOwnStrength);
        reportPersonnelLost(5633, attackerPersonnelLost);
        reportPersonnelLost(5634, defenderPersonnelLost);
        checkAndApplyStructureDamage(building);

        // Units with nobody left were dropped from their side while the losses were applied
        boolean attackersGone = attackerEliminated || combat.attackerIds.isEmpty();
        boolean defendersGone = defenderEliminated || combat.defenderIds.isEmpty();
        if (attackersGone) {
            reportSideEliminated(combat, true);
        } else if (defendersGone) {
            reportSideEliminated(combat, false);
        } else if (result.isAttackerRepulsed()) {
            reportSideRepulsed(combat);
        } else if (withdrawing) {
            reportWithdrawal(combat, building);
        }
    }

    private boolean attackersAreWithdrawing(InfantryAction combat) {
        for (int attackerId : combat.attackerIds) {
            Entity attacker = getGame().getEntity(attackerId);
            if ((attacker != null) && attacker.isInfantryCombatWantsWithdrawal()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds up a side's Marine Points, rounding the total up to a whole number ("round all fractions up", TO:AR
     * p. 171).
     *
     * @param building the building whose modifier applies, or {@code null} for the score before the modifier
     */
    private int totalMarinePoints(List<Integer> entityIds, @Nullable AbstractBuildingEntity building) {
        double total = 0;
        for (int entityId : entityIds) {
            Entity entity = getGame().getEntity(entityId);
            if (entity == null) {
                continue;
            }
            if (InfantryActionReporter.isOutOfTheFight(entity)) {
                LOGGER.debug("[InfantryAction] {} is out of the fight and contributes 0", entity.getShortName());
                continue;
            }
            total += MarinePointsScoreCalculator.calculateScore(entity, building);
        }
        return (int) Math.ceil(total);
    }

    /**
     * Converts a side's Marine Points loss back into casualties on each of its units (TO:AR p. 174) and applies
     * them. Units that no longer have anyone left drop out of the action.
     *
     * @return the people lost across the side, for the report
     */
    private int applySideLosses(InfantryAction combat, boolean isAttacker, int marinePointsLost, int ownStrength) {
        if (marinePointsLost <= 0) {
            return 0;
        }
        double casualtyFraction = InfantryCombatCasualties.casualtyFraction(marinePointsLost, ownStrength);
        List<Integer> sideIds = isAttacker ? combat.attackerIds : combat.defenderIds;
        int personnelLost = 0;
        for (int entityId : new ArrayList<>(sideIds)) {
            Entity entity = getGame().getEntity(entityId);
            if ((entity == null) || entity.isDestroyed() || entity.isDoomed() || entity.isCarcass()) {
                continue;
            }
            boolean eliminated;
            switch (entity) {
                case BattleArmor battleArmor -> {
                    int activeTroopers = battleArmor.getNumberActiveTroopers();
                    int troopersLost = InfantryCombatCasualties.personnelLost(activeTroopers, casualtyFraction, false);
                    reporter.reportUnitLoss(battleArmor, activeTroopers, marinePointsLost, ownStrength, troopersLost,
                          false);
                    applyBattleArmorLosses(battleArmor, troopersLost);
                    personnelLost += troopersLost;
                    eliminated = battleArmor.isDestroyed() || (battleArmor.getNumberActiveTroopers() <= 0);
                }
                case ConvInfantry platoon -> {
                    // The casualties are already people, so they come straight off the live trooper count. Going
                    // through the weapon damage routine would divide by the armour divisor and double for units in
                    // the open, neither of which applies to a casualty figure from the action table.
                    int troopers = platoon.getInternal(ConvInfantry.LOC_INFANTRY);
                    boolean armoured = platoon.calcDamageDivisor() >= 2.0;
                    int troopersLost = InfantryCombatCasualties.personnelLost(troopers, casualtyFraction, armoured);
                    reporter.reportUnitLoss(platoon, troopers, marinePointsLost, ownStrength, troopersLost, false);
                    if (troopersLost > 0) {
                        platoon.setInternal(Math.max(0, troopers - troopersLost), ConvInfantry.LOC_INFANTRY);
                    }
                    personnelLost += troopersLost;
                    eliminated = platoon.getInternal(ConvInfantry.LOC_INFANTRY) <= 0;
                    if (eliminated && !platoon.isDestroyed()) {
                        addReport(gameManager.destroyEntity(platoon, "infantry action casualties"));
                    }
                }
                case AbstractBuildingEntity building -> {
                    int crewBefore = building.getCrew().getCurrentSize();
                    int crewLost = applyCrewLosses(building, casualtyFraction);
                    reporter.reportUnitLoss(building, crewBefore, marinePointsLost, ownStrength, crewLost, true);
                    personnelLost += crewLost;
                    eliminated = building.getCrew().getCurrentSize() <= 0;
                }
                default -> eliminated = false;
            }
            if (eliminated) {
                LOGGER.debug("[InfantryAction] {} has no one left and leaves the action", entity.getShortName());
                entity.clearInfantryCombatState();
                sideIds.remove(Integer.valueOf(entityId));
            }
        }
        return personnelLost;
    }

    /**
     * Complex conversion for battle armor (TO:AR p. 174): ten points of standard-scale damage per eliminated trooper,
     * applied one point at a time. A squad holds one weight class, so the lightest-first rule reduces to random
     * trooper selection within the squad.
     */
    private void applyBattleArmorLosses(BattleArmor battleArmor, int troopersLost) {
        int remainingDamage = troopersLost * DAMAGE_PER_TROOPER;
        while ((remainingDamage > 0) && (battleArmor.getNumberActiveTroopers() > 0)) {
            HitData hit = battleArmor.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
            addReport(gameManager.damageEntity(battleArmor, hit, 1));
            remainingDamage--;
        }
    }

    /**
     * Building crew casualties (TO:AR p. 174): the crew shrinks by the casualty fraction and the Crew Casualties
     * Table turns the cumulative loss into crew hits. A crew reduced to nobody is doomed, which makes the building a
     * carcass at the next phase change.
     *
     * @return the crew lost
     */
    /** Report ids 5647 and 5648 are the "lose everything" versions of 5642 and 5643. */
    private static final int WIPED_OUT_MESSAGE_OFFSET = 5;
    /** Report ids 5649 and 5650 are the "nobody lost" versions of 5633 and 5634. */
    private static final int NOBODY_LOST_MESSAGE_OFFSET = 16;

    private int applyCrewLosses(AbstractBuildingEntity building, double casualtyFraction) {
        Crew crew = building.getCrew();
        int crewBefore = crew.getCurrentSize();
        int crewLost = InfantryCombatCasualties.personnelLost(crewBefore, casualtyFraction, false);
        crew.setCurrentSize(Math.max(0, crewBefore - crewLost));
        int oldHits = crew.getHits();
        int newHits = crew.calculateHits();
        for (int slot = 0; slot < crew.getSlotCount(); slot++) {
            crew.setHits(newHits, slot);
        }
        if (newHits > oldHits) {
            Report report = new Report(5635);
            report.add(building.getDisplayName());
            report.add(newHits - oldHits);
            addReport(report);
        }
        if (crew.getCurrentSize() <= 0) {
            crew.setDoomed(true);
        }
        return crewLost;
    }

    /**
     * Collateral damage (TO:AR p. 172): after every action roll, a 2D6 result of 12 costs every hex of the building
     * one point of Construction Factor.
     */
    private void checkAndApplyStructureDamage(AbstractBuildingEntity building) {
        int structureRoll = Compute.d6(2);
        if (structureRoll != STRUCTURE_DAMAGE_ROLL) {
            return;
        }
        for (Coords coords : building.getCoordsList()) {
            int currentCF = building.getCurrentCF(coords);
            if (currentCF > 0) {
                building.setCurrentCF(currentCF - 1, coords);
            }
        }
        Report report = new Report(5646);
        report.add(building.getDisplayName());
        report.add(structureRoll);
        addReport(report);
        LOGGER.info("[InfantryAction] {}: structure damaged by the fighting inside (rolled {})",
              building.getShortName(), structureRoll);
    }

    private void reportCombatHeader(AbstractBuildingEntity building) {
        Report report = new Report(5630);
        report.add(building.getDisplayName());
        addReport(report);
    }

    private void reportCombatRatio(int attackerStrength, int defenderStrength, String ratio) {
        Report report = new Report(5631);
        report.add(attackerStrength);
        report.add(defenderStrength);
        report.add(ratio);
        addReport(report);
    }

    /** The percentages actually applied after the withdrawal adjustments, so the report matches the arithmetic. */
    private void reportWithdrawal(int attackerPercent, int defenderPercent) {
        Report report = new Report(5644);
        report.add(attackerPercent);
        report.add(defenderPercent);
        addReport(report);
    }

    private void reportCombatRoll(int roll, InfantryCombatResult result) {
        Report report = new Report(5632);
        report.add(roll);
        report.add(result.toString());
        addReport(report);
    }

    /** A side cannot lose more than it has; a loss at or over its strength is reported as losing everything. */
    private void reportMarinePointsLost(int messageId, int marinePointsLost, int ownStrength) {
        if (marinePointsLost <= 0) {
            return;
        }
        if (marinePointsLost >= ownStrength) {
            Report report = new Report(messageId + WIPED_OUT_MESSAGE_OFFSET);
            report.add(ownStrength);
            addReport(report);
            return;
        }
        Report report = new Report(messageId);
        report.add(marinePointsLost);
        report.add(ownStrength);
        addReport(report);
    }

    /** A loss too small to cost a whole trooper is said out loud, so the report does not look like it forgot a side. */
    private void reportPersonnelLost(int messageId, int personnelLost) {
        if (personnelLost <= 0) {
            addReport(new Report(messageId + NOBODY_LOST_MESSAGE_OFFSET));
            return;
        }
        Report report = new Report(messageId);
        report.add(personnelLost);
        addReport(report);
    }

    private void reportSideEliminated(InfantryAction combat, boolean attackersEliminated) {
        addReport(new Report(attackersEliminated ? 5636 : 5638));
        cleanupCombat(combat);
    }

    private void reportSideRepulsed(InfantryAction combat) {
        addReport(new Report(5637));
        cleanupCombat(combat);
    }

    private void reportWithdrawal(InfantryAction combat, AbstractBuildingEntity building) {
        Report report = new Report(5639);
        report.add(building.getDisplayName());
        addReport(report);
        cleanupCombat(combat);
    }

    private void cleanupCombat(InfantryAction combat) {
        for (int entityId : combat.attackerIds) {
            Entity entity = getGame().getEntity(entityId);
            if (entity != null) {
                entity.clearInfantryCombatState();
            }
        }
        for (int entityId : combat.defenderIds) {
            Entity entity = getGame().getEntity(entityId);
            if (entity != null) {
                entity.clearInfantryCombatState();
            }
        }
        tracker.removeCombat(combat.targetId);
    }
}
