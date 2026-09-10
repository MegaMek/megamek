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

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.InfantryCombatResult;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.InfantryActionStrengths;
import megamek.common.compute.InfantryCombatCasualties;
import megamek.common.compute.InfantryCombatTables;
import megamek.common.compute.MarinePointsScoreCalculator;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Terrains;
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
    private final InfantryActionNarrator narrator;
    private final InfantryActionReporter reporter;

    InfantryActionResolutionHandler(TWGameManager gameManager, InfantryActionTracker tracker) {
        super(gameManager);
        this.tracker = tracker;
        this.reporter = new InfantryActionReporter(gameManager);
        narrator = new InfantryActionNarrator(gameManager);
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
            reportAttackersEliminated(combat);
            return;
        }
        if (defenderStrength <= 0) {
            // Nobody fights for the building this turn: it falls, and its uncommitted crew surrenders
            // (TO:AR p. 172)
            reportCombatHeader(building);
            reportBuildingFalls(combat, building);
            narrator.narrateNoDefence();
            return;
        }

        String ratio = InfantryCombatTables.calculateRatio(attackerStrength, defenderStrength);
        InfantryCombatResult result = InfantryCombatTables.resolveAction(ratio, roll);

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
        LOGGER.info("[InfantryAction] {}: ratio {} roll {} result {}; attackers lose {} of {}, defenders lose {} of {}",
              building.getShortName(), ratio, roll, result, attackerLost, attackerOwnStrength, defenderLost,
              defenderOwnStrength);

        // The losses are planned before anything is applied, so the story and the working come first and the
        // destroyed lines follow them. A side is gone when the result eliminates it or nobody who counted is left;
        // a building whose crew were never committed counts for nothing and cannot keep a side alive (p. 172).
        InfantryActionSideLosses attackerLosses = planSideLosses(combat.attackerIds, attackerLost,
              attackerOwnStrength);
        InfantryActionSideLosses defenderLosses = planSideLosses(combat.defenderIds, defenderLost,
              defenderOwnStrength);
        boolean attackersGone = attackerEliminated || attackerLosses.nobodyLeft();
        boolean defendersGone = defenderEliminated || defenderLosses.nobodyLeft();
        boolean captured = defendersGone && !attackersGone;
        InfantryActionNarrator.Side attackers = InfantryActionNarrator.Side.of(getGame(), combat.attackerIds, null,
              attackerLost, attackerOwnStrength, attackersGone);
        InfantryActionNarrator.Side defenders = InfantryActionNarrator.Side.of(getGame(), combat.defenderIds,
              building, defenderLost, defenderOwnStrength, defendersGone);

        reportCombatHeader(building);
        narrator.narrate(outcomeOf(result, withdrawing, attackersGone, defendersGone), attackers, defenders,
              captured ? building : null);
        reportRoll(ratio, attackerStrength, defenderStrength, roll, result);
        if (withdrawing) {
            reportWithdrawal(attackerPercent, defenderPercent);
        }
        List<Entity> everyone = new ArrayList<>(attackers.units());
        everyone.addAll(defenders.units());
        reporter.reportSideLosses(true, attackerLosses, everyone);
        reporter.reportSideLosses(false, defenderLosses, everyone);
        applySideLosses(combat, true, attackerLosses);
        applySideLosses(combat, false, defenderLosses);
        checkAndApplyStructureDamage(building);

        if (attackersGone) {
            LOGGER.info("[InfantryAction] {}: the attackers are gone", building.getShortName());
            cleanupCombat(combat);
        } else if (defendersGone) {
            captureBuilding(combat, building);
            cleanupCombat(combat);
        } else if (result.isAttackerRepulsed()) {
            reportSideRepulsed(combat);
        } else if (withdrawing) {
            reportWithdrawal(combat, building);
        }
    }

    /** How the roll went, in the order the lines above decide it. */
    private static InfantryActionNarrator.Outcome outcomeOf(InfantryCombatResult result, boolean withdrawing,
          boolean attackersGone, boolean defendersGone) {
        if (attackersGone) {
            return InfantryActionNarrator.Outcome.ATTACKERS_ELIMINATED;
        }
        if (defendersGone) {
            return InfantryActionNarrator.Outcome.DEFENDERS_ELIMINATED;
        }
        if (withdrawing) {
            return InfantryActionNarrator.Outcome.WITHDRAWAL;
        }
        if (result.isAttackerRepulsed()) {
            return InfantryActionNarrator.Outcome.REPULSED;
        }
        if (result.isPartialControl()) {
            return InfantryActionNarrator.Outcome.PENETRATION;
        }
        return InfantryActionNarrator.Outcome.ENGAGED;
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
     * Works out what each unit on a side loses for the side's Marine Points loss (TO:AR p. 174): the side's casualty
     * fraction applied to each unit's head-count, rounded down. Nothing is applied here.
     *
     * @param sideIds          the side's units
     * @param marinePointsLost the side's loss in points
     * @param ownStrength      the side's own strength the loss is measured against
     *
     * @return the planned losses, listing only units that had someone to lose
     */
    private InfantryActionSideLosses planSideLosses(List<Integer> sideIds, int marinePointsLost, int ownStrength) {
        double casualtyFraction = (marinePointsLost <= 0) ? 0
              : InfantryCombatCasualties.casualtyFraction(marinePointsLost, ownStrength);
        List<InfantryActionSideLosses.UnitLoss> units = new ArrayList<>();
        for (int entityId : sideIds) {
            Entity entity = getGame().getEntity(entityId);
            if ((entity == null) || InfantryActionReporter.isOutOfTheFight(entity)) {
                continue;
            }
            InfantryActionSideLosses.UnitLoss loss = switch (entity) {
                case BattleArmor battleArmor -> {
                    int activeTroopers = battleArmor.getNumberActiveTroopers();
                    yield new InfantryActionSideLosses.UnitLoss(battleArmor, activeTroopers,
                          InfantryCombatCasualties.personnelLost(activeTroopers, casualtyFraction, false), false);
                }
                case ConvInfantry platoon -> {
                    int troopers = platoon.getInternal(ConvInfantry.LOC_INFANTRY);
                    boolean armoured = platoon.calcDamageDivisor() >= 2.0;
                    yield new InfantryActionSideLosses.UnitLoss(platoon, troopers,
                          InfantryCombatCasualties.personnelLost(troopers, casualtyFraction, armoured), false);
                }
                case AbstractBuildingEntity crewed -> {
                    int committedCrew = crewed.getCommittedCrew();
                    yield new InfantryActionSideLosses.UnitLoss(crewed, committedCrew,
                          InfantryCombatCasualties.personnelLost(committedCrew, casualtyFraction, false), true);
                }
                default -> null;
            };
            if ((loss != null) && (loss.headCount() > 0)) {
                units.add(loss);
            }
        }
        return new InfantryActionSideLosses(marinePointsLost, ownStrength, units);
    }

    /**
     * Applies a side's planned losses to its units. Units that no longer have anyone left drop out of the action.
     */
    private void applySideLosses(InfantryAction combat, boolean isAttacker, InfantryActionSideLosses losses) {
        List<Integer> sideIds = isAttacker ? combat.attackerIds : combat.defenderIds;
        for (InfantryActionSideLosses.UnitLoss loss : losses.units()) {
            Entity entity = loss.entity();
            boolean eliminated;
            switch (entity) {
                case BattleArmor battleArmor -> {
                    applyBattleArmorLosses(battleArmor, loss.personnelLost());
                    eliminated = battleArmor.isDestroyed() || (battleArmor.getNumberActiveTroopers() <= 0);
                }
                case ConvInfantry platoon -> {
                    // The casualties are already people, so they come straight off the live trooper count. Going
                    // through the weapon damage routine would divide by the armour divisor and double for units in
                    // the open, neither of which applies to a casualty figure from the action table.
                    if (loss.personnelLost() > 0) {
                        platoon.setInternal(Math.max(0, loss.remaining()), ConvInfantry.LOC_INFANTRY);
                    }
                    eliminated = platoon.getInternal(ConvInfantry.LOC_INFANTRY) <= 0;
                    if (eliminated && !platoon.isDestroyed()) {
                        addReport(gameManager.destroyEntity(platoon, "infantry action casualties"));
                    }
                }
                case AbstractBuildingEntity crewed -> {
                    applyCrewLosses(crewed, loss.personnelLost());
                    // A building with crew left to commit stays in the action; only a dead crew leaves it
                    eliminated = crewed.getCrew().getCurrentSize() <= 0;
                }
                default -> eliminated = false;
            }
            if (eliminated) {
                LOGGER.debug("[InfantryAction] {} has no one left and leaves the action", entity.getShortName());
                entity.clearInfantryCombatState();
                sideIds.remove(Integer.valueOf(entity.getId()));
            }
        }
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
     * @param crewLost the crew this building loses, already reported
     */
    /** The defenders committed nothing and the building falls. */
    /** The attackers had nobody left to make the roll. */
    private static final int ATTACKERS_ELIMINATED = 5636;
    private static final int BUILDING_FALLS = 5665;
    /** A unit inside a fallen or captured building that never joined the fight surrenders. */
    private static final int SURRENDERS = 5717;
    /** A unit is moved out of the building after a withdrawal or a repulse. */
    private static final int MOVES_OUT = 5666;
    /** Report ids 5647 and 5648 are the "lose everything" versions of 5642 and 5643. */
    /** Report ids 5649 and 5650 are the "nobody lost" versions of 5633 and 5634. */

    private void applyCrewLosses(AbstractBuildingEntity building, int crewLost) {
        Crew crew = building.getCrew();
        int oldHits = crew.getHits();
        building.loseCommittedCrew(crewLost);
        int newHits = crew.getHits();
        if (newHits > oldHits) {
            Report report = new Report(5635);
            report.indent(2);
            report.addDesc(building);
            report.add(newHits - oldHits);
            addReport(report);
        }
        gameManager.entityUpdate(building.getId());
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
        report.addDesc(building);
        addReport(report);
    }

    /** The odds, both totals and the roll on one line, the way a weapon's to-hit and roll share one line. */
    private void reportRoll(String ratio, int attackerStrength, int defenderStrength, int roll,
          InfantryCombatResult result) {
        Report report = new Report(5631);
        report.indent(InfantryActionReporter.SIDE_LINE_INDENT);
        report.add(ratio);
        report.add(attackerStrength);
        report.add(defenderStrength);
        report.add(roll);
        report.add(result.printedCell());
        addReport(report);
    }

    /** The defenders committed nothing: the building falls to the attackers and everyone left inside surrenders. */
    private void reportBuildingFalls(InfantryAction combat, AbstractBuildingEntity building) {
        Report report = new Report(BUILDING_FALLS);
        report.indent(InfantryActionReporter.SIDE_LINE_INDENT);
        report.addDesc(building);
        addReport(report);
        surrenderEveryoneLeft(combat, building);
        LOGGER.info("[InfantryAction] {} falls: nothing was committed to its defence", building.getShortName());
        cleanupCombat(combat);
    }

    /**
     * The defenders' Marine Points are at zero: the building is captured, and everyone left inside who was not in
     * the fight surrenders with it (TO:AR p. 172, the Castles Brian example).
     */
    private void captureBuilding(InfantryAction combat, AbstractBuildingEntity building) {
        surrenderEveryoneLeft(combat, building);
        LOGGER.info("[InfantryAction] {} is captured: its defenders are at zero Marine Points",
              building.getShortName());
    }

    /**
     * The crew who were not committed and the owner's infantry inside that never joined the fight surrender: the
     * crew are gone from the building, and the infantry leave play as captured units.
     */
    private void surrenderEveryoneLeft(InfantryAction combat, AbstractBuildingEntity building) {
        building.getCrew().setCurrentSize(0);
        building.getCrew().setDoomed(true);
        for (Entity entity : new ArrayList<>(getGame().getEntitiesVector())) {
            boolean uncommittedDefender = (entity instanceof Infantry)
                  && !InfantryActionReporter.isOutOfTheFight(entity)
                  && !combat.attackerIds.contains(entity.getId())
                  && InfantryActionStrengths.defends(entity.getOwner(), building)
                  && InfantryActionStrengths.isInside(entity, building);
            if (uncommittedDefender) {
                surrender(entity);
            }
        }
    }

    private void surrender(Entity entity) {
        Report report = new Report(SURRENDERS);
        report.indent(InfantryActionReporter.SIDE_LINE_INDENT);
        report.subject = entity.getId();
        report.addDesc(entity);
        addReport(report);
        entity.clearInfantryCombatState();
        entity.setCaptured(true);
        getGame().removeEntity(entity.getId(), IEntityRemovalConditions.REMOVE_CAPTURED);
        gameManager.send(gameManager.createRemoveEntityPacket(entity.getId(), IEntityRemovalConditions.REMOVE_CAPTURED));
        LOGGER.info("[InfantryAction] {} surrenders with the building", entity.getShortName());
    }

    /**
     * Units that withdrew last turn are moved to a hex next to the building in this End Phase (TO:AR p. 172), before
     * the actions roll.
     */
    void moveWithdrawnUnitsOut() {
        for (Entity entity : new ArrayList<>(getGame().getEntitiesVector())) {
            if (entity.isInfantryActionLeaving()) {
                entity.setInfantryActionLeaving(false);
                moveOutOfBuilding(entity);
            }
        }
    }

    /** Puts a unit in the nearest hex outside the building it stands in, the first legal one clockwise from north. */
    private void moveOutOfBuilding(Entity unit) {
        Coords from = unit.getPosition();
        if (from == null) {
            return;
        }
        for (Coords candidate : from.allAdjacent()) {
            Hex hex = getGame().getBoard(unit.getBoardId()).getHex(candidate);
            if (hex == null) {
                continue;
            }
            boolean outsideBuilding = !hex.containsTerrain(Terrains.BUILDING);
            boolean allowed = outsideBuilding && !unit.isLocationProhibited(candidate)
                  && (Compute.stackingViolation(getGame(), unit.getId(), candidate, false) == null);
            if (allowed) {
                Report report = new Report(MOVES_OUT);
                report.indent(InfantryActionReporter.SIDE_LINE_INDENT);
                report.addDesc(unit);
                report.add(candidate.getBoardNum());
                addReport(report);
                addReport(gameManager.doEntityDisplacement(unit, from, candidate, null));
                LOGGER.info("[InfantryAction] {} moves out of the building to {}", unit.getShortName(),
                      candidate.getBoardNum());
                return;
            }
        }
        LOGGER.warn("[InfantryAction] {} has no hex to move out to and stays at {}", unit.getShortName(),
              from.getBoardNum());
    }

    /** The percentages actually applied after the withdrawal adjustments, so the report matches the arithmetic. */
    private void reportWithdrawal(int attackerPercent, int defenderPercent) {
        Report report = new Report(5644);
        report.indent(InfantryActionReporter.SIDE_LINE_INDENT);
        report.add(attackerPercent);
        report.add(defenderPercent);
        addReport(report);
    }


    /** The attackers had nobody left before the roll: the action ends without one. */
    private void reportAttackersEliminated(InfantryAction combat) {
        addReport(new Report(ATTACKERS_ELIMINATED).indent(InfantryActionReporter.SIDE_LINE_INDENT));
        cleanupCombat(combat);
    }

    /** Repulsed: the attackers are out of the building at once and the defenders regain full control (p. 173). */
    private void reportSideRepulsed(InfantryAction combat) {
        addReport(new Report(5637).indent(InfantryActionReporter.SIDE_LINE_INDENT));
        List<Entity> attackers = attackersOf(combat);
        cleanupCombat(combat);
        for (Entity attacker : attackers) {
            moveOutOfBuilding(attacker);
        }
    }

    /** Withdrawn: the action ends, and the force is moved out in the following End Phase (p. 172). */
    private void reportWithdrawal(InfantryAction combat, AbstractBuildingEntity building) {
        Report report = new Report(5639);
        report.indent(InfantryActionReporter.SIDE_LINE_INDENT);
        report.addDesc(building);
        addReport(report);
        for (Entity attacker : attackersOf(combat)) {
            attacker.setInfantryActionLeaving(true);
        }
        cleanupCombat(combat);
    }

    private List<Entity> attackersOf(InfantryAction combat) {
        List<Entity> attackers = new ArrayList<>();
        for (int attackerId : combat.attackerIds) {
            Entity attacker = getGame().getEntity(attackerId);
            if (attacker != null) {
                attackers.add(attacker);
            }
        }
        return attackers;
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
            if (entity instanceof AbstractBuildingEntity building) {
                // The action is over, so the commitment penalty ends; the crew actually lost still count
                building.clearCommittedCrew();
                gameManager.entityUpdate(building.getId());
            }
        }
        tracker.removeCombat(combat.targetId);
    }
}
