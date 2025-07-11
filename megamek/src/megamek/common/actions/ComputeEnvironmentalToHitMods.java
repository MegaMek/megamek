/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.actions;

import megamek.client.ui.Messages;
import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;

class ComputeEnvironmentalToHitMods {

    /**
     * Convenience method that compiles the ToHit modifiers applicable to the weather or other special environmental
     * effects. These affect everyone on the board.
     *
     * @param game                The current {@link Game}
     * @param attacker            The attacking entity
     * @param target              The Targetable object being attacked
     * @param wtype               The WeaponType of the weapon being used
     * @param atype               The AmmoType being used for this attack
     * @param toHit               The running total ToHitData for this WeaponAttackAction
     * @param isArtilleryIndirect flag that indicates whether this is an indirect-fire artillery attack
     */
    static ToHitData compileEnvironmentalToHitMods(Game game, Entity attacker, Targetable target, WeaponType wtype,
          AmmoType atype, ToHitData toHit, boolean isArtilleryIndirect) {

        PlanetaryConditions conditions = game.getPlanetaryConditions();

        if (toHit == null) {
            // Without valid toHit data, the rest of this will fail
            toHit = new ToHitData();
        }

        // Night combat modifiers
        if (!isArtilleryIndirect) {
            toHit.append(AbstractAttackAction.nightModifiers(game, target, atype, attacker, true));
        }

        TargetRoll weatherToHitMods = new TargetRoll();

        boolean isEnergy = (wtype != null) && wtype.hasFlag(WeaponType.F_ENERGY);
        boolean isMissile = (wtype != null) && wtype.hasFlag(WeaponType.F_MISSILE);
        boolean isDirectFireBallistic = (wtype != null) &&
                                              wtype.hasFlag(WeaponType.F_BALLISTIC) &&
                                              wtype.hasFlag(WeaponType.F_DIRECT_FIRE);

        if (!attacker.isSpaceborne()) {
            // weather mods
            int weatherMod = conditions.getWeatherHitPenalty(attacker);
            if (weatherMod != 0) {
                weatherToHitMods.addModifier(weatherMod, conditions.getWeather().toString());
            }

            // wind mods, TO:AR p.59
            if (conditions.getWind().isModerateGale()) {
                if (isMissile) {
                    weatherToHitMods.addModifier(1, conditions.getWind().toString());
                }
            } else if (conditions.getWind().isStrongGale()) {
                if (isDirectFireBallistic) {
                    weatherToHitMods.addModifier(1, conditions.getWind().toString());
                } else if (isMissile) {
                    weatherToHitMods.addModifier(2, conditions.getWind().toString());
                }
            } else if (conditions.getWind().isStorm()) {
                if (isDirectFireBallistic) {
                    weatherToHitMods.addModifier(2, conditions.getWind().toString());
                } else if (isMissile) {
                    weatherToHitMods.addModifier(3, conditions.getWind().toString());
                }
            } else if (conditions.getWind().isTornadoF1ToF3()) {
                if (isEnergy) {
                    weatherToHitMods.addModifier(2, conditions.getWind().toString());
                } else if (isDirectFireBallistic) {
                    weatherToHitMods.addModifier(3, conditions.getWind().toString());
                }
            } else if (conditions.getWind().isTornadoF4()) {
                weatherToHitMods.addModifier(3, conditions.getWind().toString());
            }

            // fog mods
            if (isEnergy && conditions.getFog().isFogHeavy()) {
                weatherToHitMods.addModifier(1, Messages.getString("WeaponAttackAction.HeavyFog"));
            }

            // blowing sand mods
            if (isEnergy && conditions.isBlowingSandActive()) {
                weatherToHitMods.addModifier(1, Messages.getString("WeaponAttackAction.BlowingSand"));
            }
        }

        if (weatherToHitMods.getValue() > 0) {
            if ((attacker.getCrew() != null) && attacker.hasAbility(OptionsConstants.UNOFF_WEATHERED)) {
                weatherToHitMods.addModifier(-1, Messages.getString("WeaponAttackAction.Weathered"));
            }
            toHit.append(weatherToHitMods);
        }

        // gravity mods (not in space)
        if (!attacker.isSpaceborne()) {
            int mod = (int) Math.floor(Math.abs((conditions.getGravity() - 1.0f) / 0.2f));
            if ((mod != 0) && (isDirectFireBallistic || isMissile)) {
                toHit.addModifier(mod, Messages.getString("WeaponAttackAction.Gravity"));
            }
        }

        // Electro-Magnetic Interference
        if (conditions.getEMI().isEMI() && !attacker.isConventionalInfantry()) {
            toHit.addModifier(2, Messages.getString("WeaponAttackAction.EMI"));
        }
        return toHit;
    }

    private ComputeEnvironmentalToHitMods() { }
}
