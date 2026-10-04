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
package megamek.client.bot;

import megamek.common.compute.Compute;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.weapons.Weapon;
import megamek.logging.MMLogger;

/**
 * Manages the equipment on a bot's own units whose only cost is heat, switching it off when a unit runs
 * hot and back on once it has cooled.
 *
 * <p>Four concealment systems and the radical heat sink all sit on ordinary equipment modes, and a human
 * player flips them by hand every turn. Only stealth armor was ever managed by the bot; the other three
 * concealment systems ran permanently, and the radical heat sink was never switched on at all, so a unit
 * carrying one fought on roughly half the cooling it was built with. See MegaMek/megamek#8802.</p>
 *
 * <p>Two rules run the whole class. Concealment is shed at {@link #SHED_HEAT_THRESHOLD}, where shutdown
 * rolls begin, and restored at or below {@link #RESTORE_HEAT_THRESHOLD}, below the first movement
 * penalty. The gap between the two is deliberate: a single threshold would switch a unit hovering on it
 * on and off every turn. The radical heat sink is gambled only while the unit needs the cooling and the
 * odds are still better than even.</p>
 *
 * <p>This runs for Princess and CASPAR alike, because it hangs off {@link BotClient}.</p>
 */
public class BotHeatEquipmentManager {

    private static final MMLogger LOGGER = MMLogger.create(BotHeatEquipmentManager.class);

    /**
     * Heat at which a Mek begins rolling to avoid shutdown, and therefore the point at which shedding a
     * voluntary heat load is worth losing what it buys.
     */
    static final int SHED_HEAT_THRESHOLD = 14;

    /**
     * Heat at or below which a unit is cool enough to switch its concealment back on. Set below the first
     * movement penalty so a unit is genuinely comfortable before it starts paying again, and far enough
     * under {@link #SHED_HEAT_THRESHOLD} that a unit sitting near the shed point cannot flap between the
     * two states on consecutive turns.
     */
    static final int RESTORE_HEAT_THRESHOLD = 5;

    /**
     * Highest 2d6 target number still better than even odds. A radical heat sink is gambled only while the
     * next use's target is this or lower, which by the table in {@code radicalHeatSinkSuccessTarget} means
     * three consecutive uses (3+, 5+, 7+) and then a turn of rest, rather than rolling at 10+ and losing
     * the system permanently.
     */
    static final int BEST_ODDS_TARGET_NUMBER = 7;

    /** Concealment systems this class manages, in the order they are shed. */
    private static final MiscTypeFlag[] CONCEALMENT_FLAGS = {
          MiscType.F_NULL_SIG, MiscType.F_VOID_SIG, MiscType.F_CHAMELEON_SHIELD
    };

    /** What to do with a piece of switchable equipment this turn. */
    private enum HeatLoadAction {
        SWITCH_OFF, SWITCH_ON, LEAVE_ALONE
    }

    private final BotClient botClient;

    public BotHeatEquipmentManager(BotClient botClient) {
        this.botClient = botClient;
    }

    /**
     * Reviews every unit this bot owns and switches its heat-generating equipment on or off as its heat
     * requires. Called once per turn from the end phase, where a human player would make the same calls.
     */
    public void manageOwnedUnits() {
        Game game = botClient.getGame();
        for (Entity entity : game.getEntitiesVector()) {
            if (entity.getOwnerId() != botClient.getLocalPlayerNumber()) {
                continue;
            }
            toggleStealthArmor(entity);
            toggleConcealmentSystems(entity);
            toggleNovaCEWS(entity);
            manageRadicalHeatSink(entity);
        }
    }

    /**
     * Switches stealth armor off when most of the enemy is close enough that the to-hit penalty is worth
     * less than the heat sinks it ties up, or when the unit is running hot enough to risk shutting down.
     *
     * @param entity the unit being reviewed
     */
    private void toggleStealthArmor(Entity entity) {
        if (!entity.hasStealth()) {
            return;
        }

        for (Mounted<?> equipment : entity.getMisc()) {
            MiscType equipmentType = (MiscType) equipment.getType();
            if (!equipmentType.hasFlag(MiscType.F_STEALTH)) {
                continue;
            }

            int newStealthMode = desiredStealthMode(entity);
            equipment.setMode(newStealthMode);
            botClient.sendModeChange(entity.getId(), entity.getEquipmentNum(equipment), newStealthMode);
            return;
        }
    }

    /**
     * @param entity the unit whose stealth armor is being decided
     *
     * @return 1 to switch stealth armor on, 0 to switch it off
     */
    private int desiredStealthMode(Entity entity) {
        if (!entity.tracksHeat()) {
            // Always activate stealth if the heat doesn't matter.
            return 1;
        }

        // If the Mek is in danger of shutting down (14+ heat), consider shutting off the armor.
        int triggerHeat = 13 + Compute.randomInt(7);
        if (entity.heat > triggerHeat) {
            return 0;
        }

        if (entity.getPosition() == null) {
            // Off-board entities that do track heat should be stealthing up before they come back on-board.
            return 1;
        }

        if (wantsStealthHeatForTsm(entity)) {
            // A Mek with heat-activated Triple-Strength Myomer uses stealth armor's heat to reach the TSM
            // activation threshold while it closes, and stays cloaked during the approach. Once it is
            // adjacent to an enemy, though, it drops stealth: at melee it needs its heat sinks free to fire
            // weapons (keeping its own heat up for TSM) while it makes doubled physical attacks, and
            // stealth's defensive value against an adjacent foe is small.
            boolean adjacentToEnemy = isAdjacentToEnemy(entity);
            LOGGER.debug("[HeatTSM] {}: stealth armor {} for TSM ({})",
                  entity.getShortName(),
                  adjacentToEnemy ? "off" : "on",
                  adjacentToEnemy ? "adjacent - firing/melee" : "closing");
            return adjacentToEnemy ? 0 : 1;
        }

        // The Mek is not in danger of shutting down soon; if most of the enemy is right next to it,
        // deactivate the armor to free up heat sinks for weapons fire.
        int totalBattleValue = 0;
        int knownBattleValue = 0;
        int knownRange = 0;
        int knownCount = 0;

        for (Entity enemy : botClient.getGame().getEntitiesVector()) {
            if (!entity.isEnemyOf(enemy)) {
                continue;
            }
            totalBattleValue += enemy.calculateBattleValue();
            // Skip enemies without a position (off-board, not yet deployed, in transport, etc.) - we can't
            // measure distance to them, and including them in the count/BV would skew the average range.
            if ((enemy.getPosition() != null) && enemy.isVisibleToEnemy()) {
                knownCount++;
                knownBattleValue += enemy.calculateBattleValue();
                knownRange += Compute.effectiveDistance(botClient.getGame(), entity, enemy);
            }
        }

        // If no or few enemy units are visible they are hiding; default to stealth armor on.
        if ((knownCount == 0) || (knownBattleValue < (totalBattleValue / 2))) {
            return 1;
        }

        return ((knownRange / knownCount) <= (5 + Compute.randomInt(5))) ? 0 : 1;
    }

    /**
     * Switches a Null Signature System, Void Signature System or Chameleon Light Polarization Shield off
     * while the unit is running hot, and back on once it has cooled. Unlike stealth armor these do not
     * depend on an ECM suite, so the decision is heat and nothing else.
     *
     * @param entity the unit being reviewed
     */
    private void toggleConcealmentSystems(Entity entity) {
        for (MiscTypeFlag concealmentFlag : CONCEALMENT_FLAGS) {
            for (MiscMounted equipment : entity.getMisc()) {
                if (!equipment.getType().hasFlag(concealmentFlag) || !equipment.isOperable()) {
                    continue;
                }
                applyHeatLoadAction(entity, equipment, decideHeatLoadAction(entity, equipment));
                break;
            }
        }
    }

    /**
     * Switches an active Nova CEWS off while the unit is running hot. This goes second, after the
     * concealment systems: switching a Nova off costs the unit its network link, which is a real tactical
     * loss, and at 2 heat a turn it buys much less relief than the 6 to 10 a concealment system does.
     *
     * @param entity the unit being reviewed
     */
    private void toggleNovaCEWS(Entity entity) {
        if (hasActiveConcealment(entity)) {
            // Something cheaper to lose is still switched on; shed that first and reconsider next turn.
            return;
        }

        for (MiscMounted equipment : entity.getMisc()) {
            if (!equipment.getType().hasFlag(MiscType.F_NOVA) || !equipment.isOperable()) {
                continue;
            }
            applyHeatLoadAction(entity, equipment, decideHeatLoadAction(entity, equipment));
            return;
        }
    }

    /**
     * Switches a radical heat sink on while the unit needs the cooling and the gamble is still worth
     * taking, and off again otherwise so its stress counter falls back.
     *
     * <p>Failure destroys the system permanently, and the target number climbs with every consecutive
     * turn of use, so this stops at {@link #BEST_ODDS_TARGET_NUMBER} rather than rolling on odds the unit
     * is more likely to lose than win.</p>
     *
     * @param entity the unit being reviewed
     */
    private void manageRadicalHeatSink(Entity entity) {
        if (!entity.hasWorkingRadicalHS()) {
            return;
        }

        boolean needsTheCooling = entity.tracksHeat() && (entity.getHeat() >= SHED_HEAT_THRESHOLD);
        int nextTargetNumber = Game.rulesManager.getRulesEquipment()
              .radicalHeatSinkSuccessTarget(entity.getConsecutiveRHSUses() + 1);
        boolean oddsAreStillGood = nextTargetNumber <= BEST_ODDS_TARGET_NUMBER;
        boolean shouldBeActive = needsTheCooling && oddsAreStillGood;

        if (shouldBeActive == entity.hasActivatedRadicalHS()) {
            return;
        }

        for (MiscMounted equipment : entity.getMisc()) {
            if (!equipment.getType().hasFlag(MiscType.F_RADICAL_HEATSINK)) {
                continue;
            }
            applyHeatLoadAction(entity,
                  equipment,
                  shouldBeActive ? HeatLoadAction.SWITCH_ON : HeatLoadAction.SWITCH_OFF);
            if (shouldBeActive) {
                LOGGER.debug("[HeatVent] {}: radical heat sink on at {} heat, next failure roll needs {}",
                      entity.getShortName(), entity.getHeat(), nextTargetNumber);
            }
            // A unit can only carry one radical heat sink.
            return;
        }
    }

    /**
     * Decides what to do with one piece of switchable equipment, given how hot its unit is running.
     * Between the two thresholds the answer is to leave it alone, which is what stops a unit sitting near
     * the shed point from switching the same system on and off every turn.
     *
     * @param entity    the unit carrying the equipment
     * @param equipment the equipment being decided
     *
     * @return the action to apply this turn
     */
    private HeatLoadAction decideHeatLoadAction(Entity entity, Mounted<?> equipment) {
        boolean currentlyOn = !equipment.isModeTurnedOff();

        if (!entity.tracksHeat()) {
            // Heat costs this unit nothing, so there is never a reason to give up what the system buys.
            return currentlyOn ? HeatLoadAction.LEAVE_ALONE : HeatLoadAction.SWITCH_ON;
        }

        if (entity.getHeat() >= SHED_HEAT_THRESHOLD) {
            return currentlyOn ? HeatLoadAction.SWITCH_OFF : HeatLoadAction.LEAVE_ALONE;
        }

        if (entity.getHeat() <= RESTORE_HEAT_THRESHOLD) {
            return currentlyOn ? HeatLoadAction.LEAVE_ALONE : HeatLoadAction.SWITCH_ON;
        }

        return HeatLoadAction.LEAVE_ALONE;
    }

    /**
     * Applies a decision to one piece of equipment and tells the server about it, the same way the bot
     * already changes AMS modes.
     *
     * @param entity    the unit carrying the equipment
     * @param equipment the equipment to switch
     * @param action    what to do with it
     */
    private void applyHeatLoadAction(Entity entity, Mounted<?> equipment, HeatLoadAction action) {
        if (action == HeatLoadAction.LEAVE_ALONE) {
            return;
        }

        String newModeName = (action == HeatLoadAction.SWITCH_ON) ? Weapon.MODE_AMS_ON : Weapon.MODE_AMS_OFF;
        int newModeNumber = equipment.setMode(newModeName);
        if (newModeNumber == -1) {
            return;
        }

        botClient.sendModeChange(entity.getId(), entity.getEquipmentNum(equipment), newModeNumber);
        LOGGER.debug("[HeatVent] {}: {} switched {} at {} heat",
              entity.getShortName(), equipment.getType().getName(), newModeName.toLowerCase(),
              entity.getHeat());
    }

    /**
     * @param entity the unit being checked
     *
     * @return {@code true} if any of the three managed concealment systems is currently switched on
     */
    private boolean hasActiveConcealment(Entity entity) {
        return entity.isNullSigOn() || entity.isVoidSigOn() || entity.isChameleonShieldOn();
    }

    /**
     * Reports whether keeping stealth armor active benefits this unit's Triple-Strength Myomer. A Mek with
     * heat-activated standard TSM (which switches on at elevated heat) wants the extra heat stealth armor
     * generates to reach and hold the activation threshold, so it should not shed stealth to free heat
     * sinks. Prototype and industrial TSM are always on and do not use the heat threshold, so they gain
     * nothing here.
     *
     * @param entity the unit whose stealth armor is being toggled
     *
     * @return {@code true} if the unit has heat-activated standard TSM, otherwise {@code false}
     */
    static boolean wantsStealthHeatForTsm(Entity entity) {
        return (entity instanceof Mek mek) && mek.hasTSM(false);
    }

    /**
     * @param entity the unit whose surroundings are being checked
     *
     * @return {@code true} if any enemy of {@code entity} occupies a hex adjacent to it (melee range),
     *       otherwise {@code false}
     */
    private boolean isAdjacentToEnemy(Entity entity) {
        if (entity.getPosition() == null) {
            return false;
        }
        Game game = botClient.getGame();
        for (Entity other : game.getEntitiesVector()) {
            if (entity.isEnemyOf(other) && (other.getPosition() != null)
                  && (Compute.effectiveDistance(game, entity, other) <= 1)) {
                return true;
            }
        }
        return false;
    }
}
