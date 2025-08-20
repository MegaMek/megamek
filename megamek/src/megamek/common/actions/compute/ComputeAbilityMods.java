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
package megamek.common.actions.compute;

import static megamek.common.units.Terrains.BUILDING;
import static megamek.common.units.Terrains.MUD;
import static megamek.common.units.Terrains.PAVEMENT;
import static megamek.common.units.Terrains.ROAD;
import static megamek.common.units.Terrains.ROUGH;
import static megamek.common.units.Terrains.RUBBLE;
import static megamek.common.units.Terrains.SWAMP;

import megamek.client.ui.Messages;
import megamek.common.Hex;
import megamek.common.ToHitData;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.planetaryConditions.Wind;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Targetable;
import megamek.common.weapons.bayweapons.BayWeapon;

class ComputeAbilityMods {

    static void processAttackerQuirks(ToHitData toHit, Entity attacker, Targetable target, Mounted<?> weapon) {

        // Anti-air targeting quirk vs airborne unit
        if (attacker.hasQuirk(OptionsConstants.QUIRK_POS_ANTI_AIR) && (target instanceof Entity)) {
            if (target.isAirborneVTOLorWIGE() || target.isAirborne()) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.AaVsAir"));
            }
        }

        // Sensor ghosts quirk
        if (attacker.hasQuirk(OptionsConstants.QUIRK_NEG_SENSOR_GHOSTS)) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.SensorGhosts"));
        }

        if (null != weapon) {

            // Flat -1 for Accurate Weapon
            if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAPON_POS_ACCURATE)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.AccWeapon"));
            }
            // Flat +1 for Inaccurate Weapon
            if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAPON_NEG_INACCURATE)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.InAccWeapon"));
            }
            // Stable Weapon - Reduces running/flanking penalty by 1
            if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAPON_POS_STABLE_WEAPON) &&
                  (attacker.moved == EntityMovementType.MOVE_RUN)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.StableWeapon"));
            }
            // +1 for a Mis-repaired Weapon - See StratOps Partial Repairs
            if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAPON_NEG_MISREPAIRED)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.MisrepairedWeapon"));
            }
            // +1 for a Mis-replaced Weapon - See StratOps Partial Repairs
            if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAPON_NEG_MIS_REPLACED)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.MisreplacedWeapon"));
            }
        }
    }

    static void processAttackerSPAs(ToHitData toHit, Entity attacker, @Nullable Targetable target, WeaponMounted weapon,
          Game game) {
        PlanetaryConditions conditions = game.getPlanetaryConditions();

        // blood stalker SPA
        if (attacker.getBloodStalkerTarget() > Entity.NONE) {
            // Attacker with bloodstalker SPA, target can be null if a building etc.
            if ((target != null) && (attacker.getBloodStalkerTarget() == target.getId())) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.BloodStalkerTarget"));
            } else {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.BloodStalkerNonTarget"));
            }
        }


        if (weapon != null) {
            WeaponType weaponType = weapon.getType();

            // Unofficial weapon class specialist - Does not have an unspecialized penalty
            if (attacker.hasAbility(OptionsConstants.UNOFFICIAL_GUNNERY_LASER)
                  && weaponType.hasFlag(WeaponType.F_ENERGY)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.GunLSkill"));
            }

            if (attacker.hasAbility(OptionsConstants.UNOFFICIAL_GUNNERY_BALLISTIC)
                  && weaponType.hasFlag(WeaponType.F_BALLISTIC)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.GunBSkill"));
            }

            if (attacker.hasAbility(OptionsConstants.UNOFFICIAL_GUNNERY_MISSILE)
                  && weaponType.hasFlag(WeaponType.F_MISSILE)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.GunMSkill"));
            }

            // Is the pilot a weapon specialist?
            if ((weaponType instanceof BayWeapon) && isSpecialistForAllBayWeapons(attacker, weapon)) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.WeaponSpec"));

            } else if (attacker.hasAbility(OptionsConstants.GUNNERY_WEAPON_SPECIALIST, weaponType.getName())) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.WeaponSpec"));

            } else if (attacker.hasAbility(OptionsConstants.GUNNERY_SPECIALIST)) {
                // aToW style gunnery specialist: -1 to specialized weapon and +1 to all other weapons
                // Note that weapon specialist supersedes gunnery specialization, so if you have a specialization in
                // Medium Lasers and a Laser specialization, you only get the -2 specialization mod
                if (weaponType.hasFlag(WeaponType.F_ENERGY)) {
                    if (attacker.hasAbility(OptionsConstants.GUNNERY_SPECIALIST, Crew.SPECIAL_ENERGY)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.EnergySpec"));
                    } else {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Unspec"));
                    }
                } else if (weaponType.hasFlag(WeaponType.F_BALLISTIC)) {
                    if (attacker.hasAbility(OptionsConstants.GUNNERY_SPECIALIST, Crew.SPECIAL_BALLISTIC)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.BallisticSpec"));
                    } else {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Unspec"));
                    }
                } else if (weaponType.hasFlag(WeaponType.F_MISSILE)) {
                    if (attacker.hasAbility(OptionsConstants.GUNNERY_SPECIALIST, Crew.SPECIAL_MISSILE)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.MissileSpec"));
                    } else {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Unspec"));
                    }
                }
            }

            // SPA Environmental Specialist
            if (target instanceof Entity targetEntity) {

                // Fog Specialist
                if (attacker.getCrew()
                      .getOptions()
                      .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                      .equals(Crew.ENVSPC_FOG)
                      && weaponType.hasFlag(WeaponType.F_ENERGY)
                      && !targetEntity.isSpaceborne()
                      && conditions.getFog().isFogHeavy()) {
                    toHit.addModifier(-1, Messages.getString("WeaponAttackAction.FogSpec"));
                }

                // Light Specialist
                if (attacker.getCrew()
                      .getOptions()
                      .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                      .equals(Crew.ENVSPC_LIGHT)) {
                    if (!targetEntity.isIlluminated() &&
                          conditions.getLight().isDuskOrFullMoonOrGlareOrMoonlessOrSolarFlareOrPitchBack()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.LightSpec"));
                    } else if (targetEntity.isIlluminated() && conditions.getLight().isPitchBack()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.LightSpec"));
                    }
                }

                // Rain Specialist
                if (attacker.getCrew()
                      .getOptions()
                      .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                      .equals(Crew.ENVSPC_RAIN)) {
                    if (conditions.getWeather().isLightRain() && attacker.isConventionalInfantry()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.RainSpec"));
                    }

                    if (conditions.getWeather().isModerateRainOrHeavyRainOrGustingRainOrDownpourOrLightningStorm()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.RainSpec"));
                    }
                }

                // Snow Specialist
                if (attacker.getCrew()
                      .getOptions()
                      .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                      .equals(Crew.ENVSPC_SNOW)) {
                    if (conditions.getWeather().isLightSnow() && attacker.isConventionalInfantry()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.SnowSpec"));
                    }

                    if (conditions.getWeather().isIceStorm() && weaponType.hasFlag(WeaponType.F_MISSILE)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.SnowSpec"));
                    }

                    if (conditions.getWeather().isModerateSnowOrHeavySnowOrSnowFlurriesOrSleet()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.SnowSpec"));
                    }
                }

                // Wind Specialist
                if (attacker.getCrew()
                      .getOptions()
                      .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                      .equals(Crew.ENVSPC_WIND)) {
                    if (conditions.getWind().isModerateGale() && weaponType.hasFlag(WeaponType.F_MISSILE)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.SnowSpec"));
                    }

                    if (weaponType.hasFlag(WeaponType.F_MISSILE) && weaponType.hasFlag(WeaponType.F_BALLISTIC)
                          && conditions.getWind().isStrongGaleOrStorm()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.WindSpec"));
                    }

                    if (conditions.getWind().isStrongerThan(Wind.STORM)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.WindSpec"));
                    }
                }
            }
        }
    }

    private static boolean isSpecialistForAllBayWeapons(Entity attacker, WeaponMounted weapon) {
        if (!(weapon.getType() instanceof BayWeapon)) {
            throw new IllegalArgumentException("Only call for BayWeapons!");
        }
        return weapon.getBayWeapons()
              .stream()
              .allMatch(w ->
                    attacker.hasAbility(OptionsConstants.GUNNERY_WEAPON_SPECIALIST, w.getName()));
    }

    static void processDefenderSPAs(ToHitData toHit, Entity attacker, Entity target, Game game) {

        if (null == target) {
            return;
        }

        // Shaky Stick - Target gets a +1 bonus against Ground-to-Air attacks
        if (target.hasAbility(OptionsConstants.PILOT_SHAKY_STICK)
              && (target.isAirborne() || target.isAirborneVTOLorWIGE())
              && !attacker.isAirborne()
              && !attacker.isAirborneVTOLorWIGE()) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.ShakyStick"));
        }
        // Terrain Abilities - Offboard units don't have a hex, so if an offboard unit has one of these
        // it will cause a NPE. Let's check to make sure the target entity is on the board:
        if (game.hasBoardLocationOf(target)) {

            // Urban Guerrilla - Target gets a +1 bonus in any sort of urban terrain
            Hex targetHex = game.getHexOf(target);
            if (target.hasAbility(OptionsConstants.INFANTRY_URBAN_GUERRILLA)
                  && targetHex.containsAnyTerrainOf(PAVEMENT, ROAD, RUBBLE, BUILDING, ROUGH)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.UrbanGuerilla"));
            }

            // Forest Ranger - Target gets a +1 bonus in wooded terrain when moving at walking speed or greater
            if (target.hasAbility(OptionsConstants.PILOT_TM_FOREST_RANGER)
                  && targetHex.hasVegetation() && (target.moved == EntityMovementType.MOVE_WALK)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.ForestRanger"));
            }

            // Swamp Beast - Target gets a +1 bonus in mud/swamp terrain when running/flanking
            if (target.hasAbility(OptionsConstants.PILOT_TM_SWAMP_BEAST)
                  && targetHex.containsAnyTerrainOf(MUD, SWAMP)
                  && (target.moved == EntityMovementType.MOVE_RUN)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.SwampBeast"));
            }
        }
    }

    private ComputeAbilityMods() {}
}
