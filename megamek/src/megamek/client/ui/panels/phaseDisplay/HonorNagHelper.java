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
package megamek.client.ui.panels.phaseDisplay;

import megamek.common.Player;
import megamek.common.actions.AbstractAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.SearchlightAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;

/**
 * Helper that mirrors, from the human player's point of view, the honor rules that a Princess bot applies in
 * {@link megamek.client.bot.princess.HonorUtil} / {@code Princess.checkForDishonoredEnemies}. It is used to warn the
 * player before they commit an attack that would <em>newly</em> cause a bot following the Forced Withdrawal rules to
 * consider them dishonored.
 *
 * <p>A bot flags the attacking player as dishonored when, with Forced Withdrawal in effect, one of the following
 * happens against one of the bot's units:</p>
 * <ul>
 *     <li>a crippled ("broken") unit keeps fighting instead of withdrawing, or a civilian unit attacks; or</li>
 *     <li>the target is one of the bot's crippled ("broken") units that would otherwise be allowed to withdraw.</li>
 * </ul>
 *
 * <p>Once a bot already considers the player dishonored, further attacks against that bot change nothing, so no warning
 * is shown. Each Princess bot reports its dishonored-players list through
 * {@link megamek.common.net.enums.PacketCommand#PRINCESS_DISHONORED}; the server relays it to every client, which
 * stores it on its {@link Game}. This works the same for locally and remotely hosted bots.</p>
 *
 * <p>Honor is only tracked by Princess opponents, so warnings are limited to attacks against enemy bot-owned units.
 * The bot's per-Princess Forced Withdrawal setting is not visible to this client; it is assumed to be on (the Princess
 * default), so this is a best-effort warning that the player can disable.</p>
 */
final class HonorNagHelper {

    private HonorNagHelper() {}

    /**
     * @return true if any of the given attacks would newly cause an enemy bot following the Forced Withdrawal rules to
     *       consider the attacking player dishonored.
     */
    static boolean wouldBeDishonored(Game game, Iterable<EntityAction> attacks) {
        for (EntityAction action : attacks) {
            if ((action instanceof AbstractAttackAction attackAction)
                  && isOffensiveAttack(attackAction)
                  && wouldBeDishonored(game, attackAction.getEntity(game), attackAction.getTarget(game))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Optimistically records, on the client's game, that every enemy bot targeted by a dishonoring attack in the list
     * now considers the player dishonored. Call this once the player confirms the warning and commits the attacks, so
     * the rest of the turn is not re-warned before that bot's authoritative {@code PRINCESS_DISHONORED} report arrives
     * (that report replaces the optimistic guess next round).
     */
    static void recordDishonor(Game game, Iterable<EntityAction> attacks) {
        for (EntityAction action : attacks) {
            if ((action instanceof AbstractAttackAction attackAction) && isOffensiveAttack(attackAction)) {
                recordDishonor(game, attackAction.getEntity(game), attackAction.getTarget(game));
            }
        }
    }

    /**
     * Optimistically records that the enemy bot owning the target now considers the attacker's owner dishonored, if the
     * given attack dishonors them. See {@link #recordDishonor(Game, Iterable)}.
     */
    static void recordDishonor(Game game, @Nullable Entity attacker, @Nullable Targetable target) {
        if (wouldBeDishonored(game, attacker, target)) {
            // wouldBeDishonored guarantees a non-null attacker and an enemy bot-owned Entity target.
            game.addDishonoredPlayer(((Entity) target).getOwner().getId(), attacker.getOwner().getId());
        }
    }

    /**
     * @return true if the action is an attack that deals damage to its target - i.e. the kind of attack a bot reacts to
     *       (everything the server routes through {@code resolvePhysicalAttack}/{@code WeaponHandler} records via
     *       {@code Entity.addAttackedByThisTurn}, which is what {@code Princess.checkForDishonoredEnemies} reads).
     *
     *       <p>Every {@link AbstractAttackAction} a player can declare against an enemy unit is a damaging attack,
     *       with one exception: the searchlight, which only illuminates its target. So this excludes the searchlight
     *       rather than enumerating every offensive type - which also means a newly added damaging attack is covered
     *       automatically instead of being silently missed. The other non-damaging actions that share these attack
     *       lists are already filtered upstream: spotting, find-club, torso-twist, arm-flip and pod triggers are
     *       {@code AbstractEntityAction}s (not {@link AbstractAttackAction}), and woods-clearing / lay-explosives
     *       target a hex or building, so the {@code target instanceof Entity} check in {@link #wouldBeDishonored} drops
     *       them. Charge, death-from-above and ram are declared in the movement phase and handled there through the
     *       {@code (attacker, target)} overload, so they never reach this list.</p>
     */
    private static boolean isOffensiveAttack(AbstractAttackAction action) {
        return !(action instanceof SearchlightAttackAction);
    }

    /**
     * @return true if the given attacker striking the given target would newly cause an enemy bot following the Forced
     *       Withdrawal rules to consider the attacking player dishonored. Useful for movement-phase attacks (charge,
     *       death-from-above, ram) that are committed before an {@link megamek.common.actions.AttackAction} exists.
     */
    static boolean wouldBeDishonored(Game game, @Nullable Entity attacker, @Nullable Targetable target) {
        if ((attacker == null) || !(target instanceof Entity targetEntity)) {
            return false;
        }

        // Honor is only tracked by Princess opponents following the Forced Withdrawal rules.
        Player targetOwner = targetEntity.getOwner();
        Player attackerOwner = attacker.getOwner();
        if ((targetOwner == null) || (attackerOwner == null)
              || !targetOwner.isBot() || !targetOwner.isEnemyOf(attackerOwner)) {
            return false;
        }

        // Dishonored by fighting on while broken (crippled) or by attacking as a civilian, or by attacking an enemy
        // that is itself broken (crippled) and would otherwise be allowed to withdraw.
        boolean matchesDishonorConditions = attacker.isCrippled()
              || !attacker.isMilitary()
              || targetEntity.isCrippled();
        if (!matchesDishonorConditions) {
            return false;
        }

        // If that bot already considers the player dishonored (as it last reported to us), nothing is left to warn
        // about. This covers pirate bots too, which have no honor to give and so consider everyone dishonored.
        return !game.isPlayerDishonoredBy(targetOwner.getId(), attackerOwner.getId());
    }
}
