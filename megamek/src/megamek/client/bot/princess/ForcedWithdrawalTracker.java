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
package megamek.client.bot.princess;

import java.util.HashSet;
import java.util.Set;

import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Decides which of a Princess bot's units withdraw under Forced Withdrawal, and keeps the list of them the bot judges
 * each turn's attacks against. CASPAR inherits it through Princess.
 *
 * <p>A unit withdraws when it is crippled and its bot follows Forced Withdrawal, or when a gamemaster ordered it to,
 * and never when a gamemaster ordered it to fight to the death (see
 * {@link megamek.common.enums.ForcedWithdrawalOrder}). Every withdrawal decision the bot makes asks
 * {@link #isWithdrawing(Entity)}, so its movement, firing and honor rules always agree.</p>
 *
 * <p>The list itself lives in {@link BotMemory}, the bot's one store of what it carries between turns. It is taken
 * at the end of each turn rather than read live, because the bot judges a turn's attacks against the units that
 * were withdrawing when that turn began.</p>
 */
public class ForcedWithdrawalTracker {

    private static final MMLogger LOGGER = MMLogger.create(ForcedWithdrawalTracker.class);

    private final Princess owner;

    /**
     * @param owner the bot whose units this tracks
     */
    ForcedWithdrawalTracker(Princess owner) {
        this.owner = owner;
    }

    /**
     * Returns whether one of the bot's units withdraws under Forced Withdrawal right now.
     *
     * @param entity the unit
     *
     * @return {@code true} if the unit withdraws: crippled under the bot's rules, counting crew damage, or ordered to
     *       by a gamemaster
     */
    public boolean isWithdrawing(Entity entity) {
        return entity.getForcedWithdrawalOrder().isWithdrawing(owner.getForcedWithdrawal(), entity.isCrippled(true));
    }

    /**
     * Retakes the list of the bot's withdrawing units, at the start of the bot's first game and at the end of every
     * turn. Taken even when the bot ignores Forced Withdrawal, since a gamemaster can still order one of its units
     * off the field.
     */
    void refreshWithdrawingUnits() {
        Set<Integer> withdrawingUnitIds = new HashSet<>();
        for (Entity entity : owner.getEntitiesOwned()) {
            if (isWithdrawing(entity)) {
                withdrawingUnitIds.add(entity.getId());
            }
        }
        owner.getMemory().setCrippledUnits(withdrawingUnitIds);
        LOGGER.debug("[ForcedWithdrawal] {} withdrawing units now {}", owner.getName(), withdrawingUnitIds);
    }

    /**
     * Forgets which units were withdrawing, when a game ends and the players return to the lobby. Unit IDs start
     * again in the next game, so a kept list would have the bot treat whichever new unit reuses a withdrawing unit's
     * ID as fleeing for the whole first round.
     */
    void forgetWithdrawingUnits() {
        LOGGER.debug("[ForcedWithdrawal] {} back in the lobby; forgetting withdrawing units {}", owner.getName(),
              owner.getMemory().crippledUnitIds());
        owner.getMemory().setCrippledUnits(Set.of());
        owner.getMemory().forgetForfeitedProtection();
    }

    /**
     * Returns whether attacking one of the bot's units dishonors the attacker. That is a unit withdrawing as of the
     * last refresh that has not fired while withdrawing. A unit that fires has chosen to fight, so it gives up the
     * protection for the rest of the game, though it still heads for its edge.
     *
     * @param unitId one of the bot's units
     *
     * @return {@code true} if the unit is withdrawing and has not given up its protection
     */
    boolean isProtected(int unitId) {
        return owner.getMemory().isCrippled(unitId) && !owner.getMemory().hasForfeitedProtection(unitId);
    }

    /**
     * Notes which of the bot's withdrawing units attacked this turn, so they give up their protection. Without this
     * a withdrawing unit could fire on a player who could not fire back without being dishonored (issue #9595).
     *
     * <p>Only units already withdrawing when this turn began count. Attacks are declared at the same time, so a unit
     * crippled this turn fired as a healthy unit.</p>
     *
     * @param unitsWithdrawingAtStartOfTurn ids of this bot's units that were withdrawing when this turn began
     */
    void updateForfeitedProtection(Set<Integer> unitsWithdrawingAtStartOfTurn) {
        if (unitsWithdrawingAtStartOfTurn.isEmpty()) {
            return;
        }
        Set<Integer> attackerIds = attackersThisTurn();
        for (int unitId : unitsWithdrawingAtStartOfTurn) {
            if (attackerIds.contains(unitId) && owner.getMemory().rememberForfeitedProtection(unitId)) {
                Entity unit = owner.getGame().getEntity(unitId);
                LOGGER.info("[ForcedWithdrawal] {} attacked while withdrawing; it may now be attacked without "
                      + "dishonor.", (unit == null) ? "Unit " + unitId : unit.getDisplayName());
            }
        }
    }

    /**
     * @return the id of every unit that attacked anything this turn, read from the targets' attack records. Targets
     *       destroyed this turn are counted too, since a kill is still an attack.
     */
    private Set<Integer> attackersThisTurn() {
        Set<Integer> attackerIds = new HashSet<>();
        for (Entity target : owner.getGame().getEntitiesVector()) {
            attackerIds.addAll(target.getAttackedByThisTurn());
        }
        for (Entity target : owner.getGame().getOutOfGameEntitiesVector()) {
            attackerIds.addAll(target.getAttackedByThisTurn());
        }
        return attackerIds;
    }

    /**
     * @return the bot's units that are protected as of the last refresh (see {@link #isProtected(int)}), which the
     *       bot reports to clients so they tag those units and warn before attacking them
     */
    Set<Integer> withdrawingUnitIds() {
        Set<Integer> protectedUnitIds = new HashSet<>();
        for (int unitId : owner.getMemory().crippledUnitIds()) {
            if (isProtected(unitId)) {
                protectedUnitIds.add(unitId);
            }
        }
        return protectedUnitIds;
    }
}
