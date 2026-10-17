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

import jakarta.annotation.Nullable;
import megamek.common.LosEffects;
import megamek.common.TargetRollModifier;
import megamek.common.ToHitData;
import megamek.common.board.Board;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;

/**
 * The bot's estimate of a firing-phase (direct) artillery shot, written to match how the engine resolves one in
 * {@code ComputeToHit.artilleryDirectToHit} and the direct-fire range checks in {@code ComputeToHitIsImpossible}.
 *
 * <p>The engine treats such a shot very differently from an ordinary weapon: the to-hit number is the gunner's skill,
 * the attacker's movement and a flat base modifier of 4, plus the modifiers for terrain between the two units when
 * there is line of sight. There is no range modifier, nothing for the target's movement or the terrain in its hex,
 * and no environmental modifier. The shot needs no line of sight at all: without it the line-of-sight modifiers are
 * simply dropped. It is impossible at 6 hexes or less and beyond 17 hexes.</p>
 *
 * <p>Air-defense ammo, homing ammo and flak shots at airborne units have their own rules and are left to the
 * ordinary estimate, as are artillery shots outside the firing phase.</p>
 */
final class DirectArtilleryToHitGuess {

    /** A direct artillery shot against a ground target at this range or less is impossible (TO:AR p.153). */
    static final int MAXIMUM_IMPOSSIBLE_DISTANCE = 6;

    /** A direct artillery shot cannot reach further than one mapsheet. */
    static final int MAXIMUM_DIRECT_DISTANCE = Board.DEFAULT_BOARD_HEIGHT;

    static final String TH_DIRECT_ARTILLERY = "direct artillery";
    static final String TH_ARTILLERY_SKILL = "artillery skill";

    static final TargetRollModifier TH_DIRECT_ARTILLERY_TOO_CLOSE = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
          "too close for direct artillery");
    static final TargetRollModifier TH_DIRECT_ARTILLERY_TOO_FAR = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
          "too far for direct artillery");
    static final TargetRollModifier TH_ARTILLERY_HEX_ONLY = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
          "this artillery ammo can only target a hex");

    private DirectArtilleryToHitGuess() {
    }

    /**
     * Whether a shot should be estimated with the direct artillery rules rather than as an ordinary weapon.
     *
     * @param weaponType the weapon being fired
     * @param ammo       the ammo it would fire, {@code null} if none is loaded
     * @param shooter    the unit firing
     * @param target     what it fires at
     * @param game       the current game
     *
     * @return {@code true} for a firing-phase artillery shot at a ground target with ammo that is neither
     *       air-defense nor homing
     */
    static boolean appliesTo(WeaponType weaponType, @Nullable Mounted<?> ammo, Entity shooter, Targetable target,
          Game game) {
        if (!weaponType.hasFlag(WeaponType.F_ARTILLERY)) {
            return false;
        }
        // Targeting and Off-board phase artillery is indirect, which the engine resolves differently.
        if (game.getPhase().isTargeting() || game.getPhase().isOffboard()) {
            return false;
        }
        if (shooter.isAirborne() || shooter.isOffBoard()) {
            return false;
        }
        // Flak at airborne units keeps its own estimate.
        if (target.isAirborne() || target.isAirborneVTOLorWIGE()) {
            return false;
        }
        if (!(ammo instanceof AmmoMounted ammoMounted) || !(ammoMounted.getType() instanceof AmmoType ammoType)) {
            return false;
        }
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ADA)) {
            return false;
        }
        return !ammoMounted.isHomingAmmoInHomingMode();
    }

    /**
     * Whether a unit carries a working artillery weapon, which the engine lets fire in the firing phase at a target
     * it cannot see.
     *
     * @param shooter the unit to check
     *
     * @return {@code true} if any of its artillery weapons can fire
     */
    static boolean canFireWithoutLineOfSight(Entity shooter) {
        for (WeaponMounted weapon : shooter.getWeaponList()) {
            if (weapon.getType().hasFlag(WeaponType.F_ARTILLERY) && weapon.canFire()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Estimates the to-hit number of a direct artillery shot.
     *
     * @param game                     the current game
     * @param shooter                  the unit firing
     * @param target                   what it fires at
     * @param weapon                   the artillery weapon
     * @param ammo                     the ammo it fires; {@link #appliesTo} has already checked it
     * @param distance                 hexes between the shooter's and the target's estimated positions
     * @param attackerMovementModifier the modifier for the shooter's movement this turn
     * @param weaponDamageModifiers    the modifiers for damage to the shooter that affect this weapon
     * @param losEffects               line of sight between the estimated positions
     *
     * @return the estimated to-hit data, or an impossible roll when the engine would refuse the shot
     */
    static ToHitData guess(Game game, Entity shooter, Targetable target, WeaponMounted weapon, AmmoMounted ammo,
          int distance, ToHitData attackerMovementModifier, ToHitData weaponDamageModifiers, LosEffects losEffects) {
        if (distance <= MAXIMUM_IMPOSSIBLE_DISTANCE) {
            return new ToHitData(TH_DIRECT_ARTILLERY_TOO_CLOSE);
        }
        if (distance > MAXIMUM_DIRECT_DISTANCE) {
            return new ToHitData(TH_DIRECT_ARTILLERY_TOO_FAR);
        }

        AmmoType ammoType = ammo.getType();
        // Only ammo that also works as flak (standard and cluster) may target a unit rather than a hex.
        if ((target instanceof Entity) && !ammoType.countsAsFlak()) {
            return new ToHitData(TH_ARTILLERY_HEX_ONLY);
        }

        ToHitData toHit = baseSkill(game, shooter);
        toHit.append(attackerMovementModifier);
        toHit.addModifier(Game.rulesManager.getRulesArtillery().computeArtilleryBaseMod(MAXIMUM_DIRECT_DISTANCE,
              true, false), TH_DIRECT_ARTILLERY);

        // Without line of sight the engine still allows the shot; it only drops the line-of-sight modifiers.
        ToHitData losModifiers = losEffects.losModifiers(game);
        if (!losModifiers.cannotSucceed()) {
            toHit.append(losModifiers);
        }

        toHit.append(weaponDamageModifiers);
        if (shooter.getHeatFiringModifier() != 0) {
            toHit.addModifier(shooter.getHeatFiringModifier(), FireControl.TH_HEAT);
        }
        WeaponType weaponType = weapon.getType();
        if (weaponType.getToHitModifier(weapon) != 0) {
            toHit.addModifier(weaponType.getToHitModifier(weapon), FireControl.TH_WEAPON_MOD);
        }
        if (ammoType.getToHitModifier() != 0) {
            toHit.addModifier(ammoType.getToHitModifier(), FireControl.TH_AMMO_MOD);
        }
        if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAPON_POS_ACCURATE)) {
            toHit.addModifier(FireControl.TH_ACCURATE_WEAPON);
        }
        if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAPON_NEG_INACCURATE)) {
            toHit.addModifier(FireControl.TH_INACCURATE_WEAPON);
        }
        return toHit;
    }

    private static ToHitData baseSkill(Game game, Entity shooter) {
        if (game.getOptions().booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL)) {
            return new ToHitData(shooter.getCrew().getArtillery(), TH_ARTILLERY_SKILL);
        }
        return new ToHitData(shooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
    }
}
