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

import megamek.common.board.Board;
import megamek.common.compute.ArtilleryRange;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.weapons.bayWeapons.BayWeapon;

/**
 * Artillery reach in hexes, as the bots need it.
 *
 * <p>Artillery weapons rate their range in map sheets (IS Arrow IV 8, Thumper 21, Long Tom 30), so reading
 * {@link WeaponType#getLongRange()} as hexes gets every on-board gun wrong. The engine allows two kinds of artillery
 * attack from the board, and each answers a different question:</p>
 * <ul>
 *     <li>In the Weapon Attack phase the gun fires directly, at 7 to 17 hexes. That is how far it can hurt something
 *     this turn ({@link #maxWeaponRange}).</li>
 *     <li>In the Targeting phase it fires indirectly, beyond 17 hexes, out to its full rated range. That is how far
 *     away it can stand and still be in the fight ({@link #engagementRange}).</li>
 * </ul>
 *
 * <p>Air-Defense Arrow and cruise missiles are not direct-fire artillery and keep their existing handling. So do
 * artillery bays on DropShips and Small Craft: a bay's range is not a map-sheet range, and its ammo sits on the bay's
 * member weapons rather than the bay itself.</p>
 */
public final class ArtilleryReach {

    /** Furthest a direct-fire artillery attack may reach: one map sheet (TO:AR p.153). */
    public static final int DIRECT_FIRE_MAXIMUM_RANGE = Board.DEFAULT_BOARD_HEIGHT;

    /** Closest a direct-fire artillery attack may reach; the engine refuses six hexes or less (TO:AR p.153). */
    public static final int DIRECT_FIRE_MINIMUM_RANGE = 7;

    private ArtilleryReach() {}

    /**
     * @param weapon the weapon to check
     *
     * @return {@code true} if the weapon makes direct-fire artillery attacks in the Weapon Attack phase: an artillery
     *       weapon that is neither a cruise missile nor loaded with Air-Defense Arrow ammunition
     */
    public static boolean isDirectFireArtillery(WeaponMounted weapon) {
        return isIndirectFireArtillery(weapon) && !weapon.getType().hasFlag(WeaponType.F_CRUISE_MISSILE);
    }

    /**
     * @param weapon the weapon to check
     *
     * @return {@code true} if the weapon can make indirect artillery attacks: an artillery weapon, not a weapon bay,
     *       not loaded with Air-Defense Arrow ammunition, which only engages airborne targets
     */
    private static boolean isIndirectFireArtillery(WeaponMounted weapon) {
        if (!weapon.getType().hasFlag(WeaponType.F_ARTILLERY) || (weapon.getType() instanceof BayWeapon)) {
            return false;
        }
        AmmoMounted linkedAmmo = weapon.getLinkedAmmo();
        return (linkedAmmo == null) || !linkedAmmo.getType().getMunitionType().contains(AmmoType.Munitions.M_ADA);
    }

    /**
     * @param distance the distance to the target, in hexes
     *
     * @return {@code true} if a direct-fire artillery attack can reach that far
     */
    public static boolean isInDirectFireRange(int distance) {
        return (distance >= DIRECT_FIRE_MINIMUM_RANGE) && (distance <= DIRECT_FIRE_MAXIMUM_RANGE);
    }

    /**
     * The damage the unit's working direct-fire artillery could deal, ignoring range and to-hit. Each gun counts its
     * rack size, the same value {@link FireControl#getMaxDamageAtRange} uses for it at 7 to 17 hexes.
     *
     * @param entity the unit
     *
     * @return the summed rack size of the unit's working direct-fire artillery, or 0 if it is airborne
     */
    public static double directFireArtilleryDamage(Entity entity) {
        if (entity.isAirborne()) {
            return 0;
        }
        double damage = 0;
        for (WeaponMounted weapon : entity.getWeaponList()) {
            if (!weapon.isCrippled() && isDirectFireArtillery(weapon)) {
                damage += Math.max(0, weapon.getType().getRackSize());
            }
        }
        return damage;
    }

    /**
     * The furthest the unit can attack this turn, in hexes. This matches {@link Entity#getMaxWeaponRange(boolean)}
     * except that direct-fire artillery counts as {@link #DIRECT_FIRE_MAXIMUM_RANGE} rather than its range in map
     * sheets. Airborne units and attacks on airborne targets use the entity's own answer unchanged.
     *
     * @param entity         the unit
     * @param airborneTarget whether the target is airborne
     *
     * @return the unit's maximum weapon range, in hexes
     */
    public static int maxWeaponRange(Entity entity, boolean airborneTarget) {
        if (entity.isAirborne() || airborneTarget) {
            return entity.getMaxWeaponRange(airborneTarget);
        }

        long entityType = entity.getEntityType();
        int maxRange = 0;
        if ((entityType == Entity.ETYPE_MEK) || (entityType == Entity.ETYPE_INFANTRY)
              || (entityType == Entity.ETYPE_PROTOMEK)) {
            // account for physical attacks
            maxRange = 1;
        }

        Game game = entity.getGame();
        boolean useExtremeRange = (game != null)
              && game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE);
        for (WeaponMounted weapon : entity.getWeaponList()) {
            if (!weapon.isReady()) {
                continue;
            }
            int range;
            if (isDirectFireArtillery(weapon)) {
                range = DIRECT_FIRE_MAXIMUM_RANGE;
            } else {
                WeaponType weaponType = weapon.getType();
                range = useExtremeRange ? weaponType.getExtremeRange() : weaponType.getLongRange();
            }
            maxRange = Math.max(maxRange, range);
        }
        return maxRange;
    }

    /**
     * How far from the enemy the unit can stand and still take part in the fight, in hexes. For most units this is
     * their weapon range. A unit carrying artillery can also fire indirectly, out to its full rated range, so it is
     * not out of the fight just because it is beyond direct-fire range.
     *
     * @param game        the current game, for the planetary gravity that shortens indirect fire
     * @param entity      the unit
     * @param weaponRange the unit's maximum weapon range, in hexes
     *
     * @return the larger of the weapon range and the unit's longest indirect artillery reach
     */
    public static int engagementRange(Game game, Entity entity, int weaponRange) {
        int engagementRange = weaponRange;
        for (WeaponMounted weapon : entity.getWeaponList()) {
            if (!weapon.isReady() || !isIndirectFireArtillery(weapon)) {
                continue;
            }
            int indirectRange = ArtilleryRange.maximumIndirectRangeInHexes(game, entity, weapon.getType(), weapon);
            engagementRange = Math.max(engagementRange, indirectRange);
        }
        return engagementRange;
    }
}
