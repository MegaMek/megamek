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

import megamek.common.Hex;
import megamek.common.ToHitData;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;

/**
 * Resolves which hit-location table a physical attack lands on, from the attacker's and target's (possibly
 * hypothetical) positions and elevations. This mirrors the rules-engine logic in
 * {@link megamek.common.actions.KickAttackAction#toHit}, {@link megamek.common.actions.PunchAttackAction#toHit},
 * and {@link megamek.common.actions.ClubAttackAction#toHit} so Princess's damage estimates use the same table
 * the attack would actually resolve on: a kick delivered from one level above a standing Mek lands on the
 * punch table (head reachable), a punch delivered at the target's base level lands on the kick table (legs),
 * and physicals against height-zero targets use the full body table. Keep this in sync with those three
 * action classes when their elevation rules change.
 *
 * <p>Special cases the rules engine handles that this estimate deliberately does not: converted QuadVees,
 * grounded DropShips, and hull-down attackers (prone and hull-down attackers are rejected as illegal by the
 * guess-level legality checks before table selection matters).</p>
 */
final class PhysicalHitTable {

    private PhysicalHitTable() {
    }

    /**
     * Returns the height the entity would have in the given (possibly hypothetical) stance.
     * {@link Entity#getHeight()} reads the CURRENT stance - a currently-prone Mek reports 0 even when the
     * projected state stands it up, and a currently-standing superheavy reports 2 that a projected prone
     * state should flatten. Only when the projected stance differs from the current one is the Mek height
     * reconstructed (superheavies stand two levels tall); otherwise the entity's own height - including
     * exotic overrides like LAMs in fighter mode - is trusted as-is.
     *
     * @param entity         the entity whose height is wanted
     * @param projectedProne whether the projected state has the entity prone (or hull-down)
     *
     * @return the height above its base level the entity would have in the projected stance
     */
    static int projectedHeight(Entity entity, boolean projectedProne) {
        if ((entity instanceof Mek mek) && (projectedProne != mek.isProne())) {
            return projectedProne ? 0 : (mek.isSuperHeavy() ? 2 : 1);
        }
        return entity.getHeight();
    }

    /**
     * Resolves the hit-location table for the given physical attack.
     *
     * @param attackType   the punch, kick, or physical-weapon attack being evaluated
     * @param shooter      the attacking entity
     * @param shooterState the attacker's (possibly hypothetical) position and elevation
     * @param target       the target of the attack
     * @param targetState  the target's (possibly hypothetical) position and elevation
     * @param game         the current game, for hex levels
     *
     * @return one of {@link ToHitData#HIT_PUNCH}, {@link ToHitData#HIT_KICK}, or {@link ToHitData#HIT_NORMAL}
     */
    static int resolve(PhysicalAttackType attackType, Entity shooter, EntityState shooterState,
          Targetable target, EntityState targetState, Game game) {
        if (!(target instanceof Entity targetEntity)) {
            // buildings and other non-entity targets have no location table
            return ToHitData.HIT_NORMAL;
        }

        // Without board data (or off-board positions) fall back to the attack type's usual table, which is
        // what the estimate assumed before elevation was modeled at all. Physical weapons default to the
        // full-body table, their on-the-level result.
        int fallbackTable;
        if (attackType.isPunch()) {
            fallbackTable = ToHitData.HIT_PUNCH;
        } else if (attackType.isKick()) {
            fallbackTable = ToHitData.HIT_KICK;
        } else {
            fallbackTable = ToHitData.HIT_NORMAL;
        }
        var board = game.getBoard(target);
        if (board == null) {
            return fallbackTable;
        }
        Hex attackerHex = board.getHex(shooterState.getPosition());
        Hex targetHex = board.getHex(targetState.getPosition());
        if ((attackerHex == null) || (targetHex == null)) {
            return fallbackTable;
        }

        // Absolute levels: hex floor plus the unit's elevation above it, with heights taken in the
        // PROJECTED stance (see projectedHeight).
        int targetHeightAboveFloor = projectedHeight(targetEntity, targetState.isProne());
        int targetBase = targetState.getElevation() + targetHex.getLevel();
        int targetTop = targetBase + targetHeightAboveFloor;

        if (attackType.isPunch()) {
            // Arms level: the attacker's top in its projected stance. Prone attackers are rejected as
            // illegal before table selection matters.
            int attackerArms = shooterState.getElevation() + attackerHex.getLevel()
                  + projectedHeight(shooter, shooterState.isProne());
            if (attackerArms == targetBase) {
                // punching at the target's feet: legs, or the full body of a height-zero target
                return (targetHeightAboveFloor == 0) ? ToHitData.HIT_NORMAL : ToHitData.HIT_KICK;
            }
            return ToHitData.HIT_PUNCH;
        }

        int attackerBase = shooterState.getElevation() + attackerHex.getLevel();

        if (attackType == PhysicalAttackType.WEAPON) {
            // Physical weapons (hatchets, swords, clubs) per ClubAttackAction: on the level they sweep the
            // full body; swung up at a taller target they reach the legs; swung down they land on the punch
            // table.
            if (attackerBase == targetBase) {
                return ToHitData.HIT_NORMAL;
            }
            if (attackerBase < targetBase) {
                return (targetHeightAboveFloor == 0) ? ToHitData.HIT_NORMAL : ToHitData.HIT_KICK;
            }
            return ToHitData.HIT_PUNCH;
        }

        // Kick: feet at the attacker's base level.
        if (attackerBase < targetTop) {
            return ToHitData.HIT_KICK;
        }
        // Feet level with (or above) the target's top: the kick reaches torso and head.
        return (targetHeightAboveFloor > 0) ? ToHitData.HIT_PUNCH : ToHitData.HIT_NORMAL;
    }
}
