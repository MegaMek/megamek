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
import megamek.common.RangeType;
import megamek.common.TargetRollModifier;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;
import megamek.common.units.TripodMek;
import megamek.common.units.UnitType;

/**
 * Predicts whether a shot would be blocked or shortened by water, for a shooter and target that are not
 * actually standing where the bot is considering putting them.
 *
 * <p>The game already enforces the underwater fire rules (TW p.107-109): a submerged weapon can only fire at
 * targets that are also in the water, most weapons cannot fire underwater at all, and the ones that can reach a
 * fraction of their surface range. {@link megamek.common.compute.Compute#getRangeMods} applies all of that - but
 * it reads the shooter's real location status, which the server only sets after a unit actually moves. The bot
 * evaluates thousands of positions a unit is <i>not</i> in, so its damage estimate never saw these rules. The
 * result was that deep water looked like a one-way firing position: the bot priced the protection water gives
 * (the enemy cannot hit a submerged unit) but not the cost (the submerged unit cannot shoot back), and units
 * would sit in a lake all game believing they had their full arsenal.</p>
 *
 * <p>This class is that missing prediction. {@link #isWeaponUnderwater} mirrors how the server assigns wet
 * locations when a unit enters water, applied to the hypothetical position instead of the real one: a Mek
 * standing in depth 1 has only its legs underwater, anything deeper or prone submerges the whole unit unless its
 * top still clears the surface, and a jumping unit stays dry on the turn it lands. {@link #check} then applies
 * the same shot restrictions the server would, in the same order, and hands back the underwater range table when
 * the shot is legal but shortened.</p>
 *
 * <p>Both directions matter. A submerged shooter overestimating its own guns is what makes water a fake bunker;
 * a dry shooter overestimating its guns against a submerged target is what makes the rest of the force waste
 * turns shooting at that bunker.</p>
 *
 * @param blocked          why the shot is impossible, or {@code null} if it can be attempted
 * @param underwaterRanges the range table to use instead of the weapon's normal one, or {@code null} to keep
 *                         the normal table; only set when the shot passes through water and is still legal
 */
public record UnderwaterFire(@Nullable TargetRollModifier blocked, @Nullable int[] underwaterRanges) {

    static final TargetRollModifier TH_WEAPON_UNDERWATER = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
          "weapon underwater, but not target");
    static final TargetRollModifier TH_WEAPON_NO_UNDERWATER_RANGE = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
          "weapon cannot fire underwater");
    static final TargetRollModifier TH_TARGET_UNDERWATER = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
          "target underwater, but not weapon");
    static final TargetRollModifier TH_TORPEDO_ABOVE_WATER = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
          "torpedoes can only fire underwater");
    static final TargetRollModifier TH_LEG_WEAPONS_AT_SURFACE_NAVAL = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
          "partially submerged mek cannot fire leg weapons at surface naval vessels");

    /** No restriction: the shot does not involve water at all. */
    static final UnderwaterFire CLEAR = new UnderwaterFire(null, null);

    /**
     * Applies the underwater fire restrictions to a hypothetical shot, mirroring the checks in
     * {@link megamek.common.compute.Compute#getRangeMods} but reading the positions the bot is considering
     * rather than the ones the units are really in.
     *
     * @param shooter      the unit firing
     * @param shooterState the position and elevation the shooter would fire from
     * @param shooterHex   the hex at that position, or {@code null} if it is off the board
     * @param target       what is being shot at
     * @param targetState  the position and elevation the target is expected at
     * @param targetHex    the hex at that position, or {@code null} if it is off the board
     * @param weapon       the weapon being fired
     * @param firingAmmo   the ammo it would fire, or {@code null} for weapons without ammo
     *
     * @return the restriction on this shot; {@link #blocked()} is {@code null} when the shot may proceed
     */
    static UnderwaterFire check(Entity shooter, EntityState shooterState, @Nullable Hex shooterHex,
          Targetable target, EntityState targetState, @Nullable Hex targetHex,
          WeaponMounted weapon, @Nullable Mounted<?> firingAmmo) {

        boolean weaponUnderwater = isWeaponUnderwater(shooter, shooterState, shooterHex, weapon);

        Entity targetEntity = (target instanceof Entity entity) ? entity : null;
        int targetBottom = targetState.getElevation();
        int targetTop = targetBottom + ((null == targetEntity) ? 0 : targetEntity.height());
        boolean targetInPartialWater = false;
        boolean targetUnderwater = false;
        if ((null != targetEntity) && (null != targetHex)
              && targetHex.containsTerrain(Terrains.WATER) && (targetBottom < 0)) {
            if (targetTop >= 0) {
                targetInPartialWater = true;
            } else {
                targetUnderwater = true;
            }
        }

        // A naval unit on the surface can be attacked from above or below.
        if ((null != targetEntity) && (0 == targetBottom) && (UnitType.NAVAL == targetEntity.getUnitType())) {
            targetInPartialWater = true;
        }

        WeaponType weaponType = weapon.getType();
        boolean torpedo = (null != weaponType.getAmmoType()) && weaponType.getAmmoType().isTorpedo();

        // Naval units may target underwater units - torpedo tubes are mounted underwater.
        if ((targetUnderwater || torpedo) && (UnitType.NAVAL == shooter.getUnitType())) {
            weaponUnderwater = true;
        }

        if (!weaponUnderwater) {
            if (targetUnderwater) {
                return new UnderwaterFire(TH_TARGET_UNDERWATER, null);
            }
            if (torpedo) {
                return new UnderwaterFire(TH_TORPEDO_ABOVE_WATER, null);
            }
            return CLEAR;
        }

        // Underwater the weapon uses its underwater range table. Torpedo and multi-purpose missiles keep
        // their surface ranges, and multi-purpose missiles are the one exception allowed to hit dry targets.
        int[] ranges = weaponType.getWRanges();
        boolean multiPurposeMissile = false;
        if ((null != firingAmmo) && (firingAmmo.getType() instanceof AmmoType ammoType)
              && isTorpedoCapableLauncher(weaponType.getAmmoType())) {
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_TORPEDO)) {
                ranges = weaponType.getRanges(weapon);
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_MULTI_PURPOSE)) {
                ranges = weaponType.getRanges(weapon);
                multiPurposeMissile = true;
            }
        }

        if (0 == ranges[RangeType.RANGE_SHORT]) {
            return new UnderwaterFire(TH_WEAPON_NO_UNDERWATER_RANGE, null);
        }
        if (!targetUnderwater && !targetInPartialWater && !multiPurposeMissile) {
            return new UnderwaterFire(TH_WEAPON_UNDERWATER, null);
        }
        // A Mek standing in depth 1 only has its leg weapons underwater, and those cannot reach a vessel
        // sitting on the surface above them.
        if ((null != targetEntity) && (UnitType.NAVAL == targetEntity.getUnitType())
              && (shooter instanceof Mek) && (shooter.height() > 0) && (-1 == shooterState.getElevation())) {
            return new UnderwaterFire(TH_LEG_WEAPONS_AT_SURFACE_NAVAL, null);
        }
        return new UnderwaterFire(null, ranges);
    }

    /**
     * Predicts whether this weapon would be underwater with the shooter at the given hypothetical position,
     * mirroring how the server assigns wet locations when a unit really enters water
     * ({@code TWGameManager.doSetLocationsExposure}): a standing Mek in water up to its partial-water depth has
     * only its legs wet, anything deeper or prone submerges every location unless the unit's top still clears
     * the surface, and a jumping unit is dry on the turn it lands.
     *
     * @param shooter      the unit firing
     * @param shooterState the position and elevation the shooter would fire from
     * @param shooterHex   the hex at that position, or {@code null} if it is off the board
     * @param weapon       the weapon being fired
     *
     * @return {@code true} if the weapon would be underwater at that position
     */
    static boolean isWeaponUnderwater(Entity shooter, EntityState shooterState, @Nullable Hex shooterHex,
          WeaponMounted weapon) {
        if ((null == shooterHex) || !shooterHex.containsTerrain(Terrains.WATER)) {
            return false;
        }
        int depth = shooterHex.terrainLevel(Terrains.WATER);
        if ((depth <= 0) || shooterState.isJumping() || (shooterState.getElevation() >= 0)) {
            return false;
        }

        int partialWaterDepth = ((shooter instanceof Mek) && shooter.isSuperHeavy()) ? 2 : 1;
        if ((shooter instanceof Mek) && !shooterState.isProne() && (depth <= partialWaterDepth)) {
            return isLegLocation(shooter, weapon.getLocation());
        }

        // Fully in the water: every location is wet unless the unit's top still clears the surface.
        int topElevation = shooterState.getElevation() + hypotheticalHeight(shooter, shooterState);
        return topElevation < 0;
    }

    /**
     * The unit's height in the pose being evaluated. {@code Entity.height()} reads the entity's CURRENT
     * prone state, which may differ from the pose of the path being ranked - a fallen Mek evaluating its
     * stand-up paths is the common case - so a Mek's standing height is computed from what it is, not from
     * how it currently lies.
     */
    private static int hypotheticalHeight(Entity shooter, EntityState shooterState) {
        if (shooterState.isProne()) {
            return 0;
        }
        if (shooter instanceof Mek) {
            return shooter.isSuperHeavy() ? 2 : 1;
        }
        return shooter.height();
    }

    /** The locations a standing Mek has underwater in partial-depth water: legs, and for quads the arms too. */
    private static boolean isLegLocation(Entity shooter, int location) {
        if ((Mek.LOC_RIGHT_LEG == location) || (Mek.LOC_LEFT_LEG == location)) {
            return true;
        }
        if ((shooter instanceof QuadMek) && ((Mek.LOC_RIGHT_ARM == location) || (Mek.LOC_LEFT_ARM == location))) {
            return true;
        }
        return (shooter instanceof TripodMek) && (Mek.LOC_CENTER_LEG == location);
    }

    /** The launcher families whose ammo choice (torpedo, multi-purpose) changes what they can do underwater. */
    private static boolean isTorpedoCapableLauncher(AmmoType.AmmoTypeEnum ammoType) {
        return (AmmoType.AmmoTypeEnum.SRM == ammoType)
              || (AmmoType.AmmoTypeEnum.SRM_IMP == ammoType)
              || (AmmoType.AmmoTypeEnum.MRM == ammoType)
              || (AmmoType.AmmoTypeEnum.LRM == ammoType)
              || (AmmoType.AmmoTypeEnum.LRM_IMP == ammoType)
              || (AmmoType.AmmoTypeEnum.MML == ammoType);
    }
}
