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
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.Game;
import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.planetaryconditions.Wind;
import megamek.common.weapons.bayweapons.BayWeapon;

class ComputeAbilityMods {

    static ToHitData processAttackerQuirks(ToHitData toHit, Entity ae, Targetable target, Mounted<?> weapon) {

        // Anti-air targeting quirk vs airborne unit
        if (ae.hasQuirk(OptionsConstants.QUIRK_POS_ANTI_AIR) && (target instanceof Entity)) {
            if (target.isAirborneVTOLorWIGE() || target.isAirborne()) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.AaVsAir"));
            }
        }

        // Sensor ghosts quirk
        if (ae.hasQuirk(OptionsConstants.QUIRK_NEG_SENSOR_GHOSTS)) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.SensorGhosts"));
        }

        if (null != weapon) {

            // Flat -1 for Accurate Weapon
            if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_POS_ACCURATE)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.AccWeapon"));
            }
            // Flat +1 for Inaccurate Weapon
            if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_INACCURATE)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.InAccWeapon"));
            }
            // Stable Weapon - Reduces running/flanking penalty by 1
            if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_POS_STABLE_WEAPON) &&
                      (ae.moved == EntityMovementType.MOVE_RUN)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.StableWeapon"));
            }
            // +1 for a Misrepaired Weapon - See StratOps Partial Repairs
            if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_MISREPAIRED)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.MisrepairedWeapon"));
            }
            // +1 for a Misreplaced Weapon - See StratOps Partial Repairs
            if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_MISREPLACED)) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.MisreplacedWeapon"));
            }
        }
        return toHit;
    }

    static ToHitData processAttackerSPAs(ToHitData toHit, Entity ae, Targetable target, WeaponMounted weapon,
          Game game) {
        PlanetaryConditions conditions = game.getPlanetaryConditions();

        // blood stalker SPA
        if (ae.getBloodStalkerTarget() > Entity.NONE) {
            // Issue #5275 - Attacker with bloodstalker SPA, `target` can be null if a
            // building etc.
            if ((target != null) && (ae.getBloodStalkerTarget() == target.getId())) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.BloodStalkerTarget"));
            } else {
                toHit.addModifier(+2, Messages.getString("WeaponAttackAction.BloodStalkerNonTarget"));
            }
        }

        WeaponType wtype = (weapon != null) ? weapon.getType() : null;

        if (wtype != null) {
            // Unofficial weapon class specialist - Does not have an unspecialized penalty
            if (ae.hasAbility(OptionsConstants.UNOFF_GUNNERY_LASER) && wtype.hasFlag(WeaponType.F_ENERGY)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.GunLSkill"));
            }

            if (ae.hasAbility(OptionsConstants.UNOFF_GUNNERY_BALLISTIC) && wtype.hasFlag(WeaponType.F_BALLISTIC)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.GunBSkill"));
            }

            if (ae.hasAbility(OptionsConstants.UNOFF_GUNNERY_MISSILE) && wtype.hasFlag(WeaponType.F_MISSILE)) {
                toHit.addModifier(-1, Messages.getString("WeaponAttackAction.GunMSkill"));
            }

            // Is the pilot a weapon specialist?
            if (wtype instanceof BayWeapon &&
                      weapon.getBayWeapons()
                            .stream()
                            .allMatch(w -> ae.hasAbility(OptionsConstants.GUNNERY_WEAPON_SPECIALIST, w.getName()))) {
                // All weapons in a bay must match the specialization
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.WeaponSpec"));
            } else if (ae.hasAbility(OptionsConstants.GUNNERY_WEAPON_SPECIALIST, wtype.getName())) {
                toHit.addModifier(-2, Messages.getString("WeaponAttackAction.WeaponSpec"));
            } else if (ae.hasAbility(OptionsConstants.GUNNERY_SPECIALIST)) {
                // aToW style gunnery specialist: -1 to specialized weapon and +1 to all other
                // weapons
                // Note that weapon specialist supersedes gunnery specialization, so if you have
                // a specialization in Medium Lasers and a Laser specialization, you only get
                // the -2 specialization mod
                if (wtype.hasFlag(WeaponType.F_ENERGY)) {
                    if (ae.hasAbility(OptionsConstants.GUNNERY_SPECIALIST, Crew.SPECIAL_ENERGY)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.EnergySpec"));
                    } else {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Unspec"));
                    }
                } else if (wtype.hasFlag(WeaponType.F_BALLISTIC)) {
                    if (ae.hasAbility(OptionsConstants.GUNNERY_SPECIALIST, Crew.SPECIAL_BALLISTIC)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.BallisticSpec"));
                    } else {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Unspec"));
                    }
                } else if (wtype.hasFlag(WeaponType.F_MISSILE)) {
                    if (ae.hasAbility(OptionsConstants.GUNNERY_SPECIALIST, Crew.SPECIAL_MISSILE)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.MissileSpec"));
                    } else {
                        toHit.addModifier(+1, Messages.getString("WeaponAttackAction.Unspec"));
                    }
                }
            }

            // SPA Environmental Specialist
            // Could be pattern-matching instanceof in Java 17
            if (target instanceof Entity) {
                Entity te = (Entity) target;

                // Fog Specialist
                if (ae.getCrew()
                          .getOptions()
                          .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                          .equals(Crew.ENVSPC_FOG) &&
                          wtype.hasFlag(WeaponType.F_ENERGY) &&
                          !te.isSpaceborne() &&
                          conditions.getFog().isFogHeavy()) {
                    toHit.addModifier(-1, Messages.getString("WeaponAttackAction.FogSpec"));
                }

                // Light Specialist
                if (ae.getCrew()
                          .getOptions()
                          .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                          .equals(Crew.ENVSPC_LIGHT)) {
                    if (!te.isIlluminated() &&
                              conditions.getLight().isDuskOrFullMoonOrGlareOrMoonlessOrSolarFlareOrPitchBack()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.LightSpec"));
                    } else if (te.isIlluminated() && conditions.getLight().isPitchBack()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.LightSpec"));
                    }
                }

                // Rain Specialist
                if (ae.getCrew()
                          .getOptions()
                          .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                          .equals(Crew.ENVSPC_RAIN)) {
                    if (conditions.getWeather().isLightRain() && ae.isConventionalInfantry()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.RainSpec"));
                    }

                    if (conditions.getWeather().isModerateRainOrHeavyRainOrGustingRainOrDownpourOrLightningStorm()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.RainSpec"));
                    }
                }

                // Snow Specialist
                if (ae.getCrew()
                          .getOptions()
                          .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                          .equals(Crew.ENVSPC_SNOW)) {
                    if (conditions.getWeather().isLightSnow() && ae.isConventionalInfantry()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.SnowSpec"));
                    }

                    if (conditions.getWeather().isIceStorm() && wtype.hasFlag(WeaponType.F_MISSILE)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.SnowSpec"));
                    }

                    if (conditions.getWeather().isModerateSnowOrHeavySnowOrSnowFlurriesOrSleet()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.SnowSpec"));
                    }
                }

                // Wind Specialist
                if (ae.getCrew()
                          .getOptions()
                          .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
                          .equals(Crew.ENVSPC_WIND)) {
                    if (conditions.getWind().isModerateGale() && wtype.hasFlag(WeaponType.F_MISSILE)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.SnowSpec"));
                    }

                    if (wtype.hasFlag(WeaponType.F_MISSILE) &&
                              wtype.hasFlag(WeaponType.F_BALLISTIC) &&
                              conditions.getWind().isStrongGaleOrStorm()) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.WindSpec"));
                    }

                    if (conditions.getWind().isStrongerThan(Wind.STORM)) {
                        toHit.addModifier(-1, Messages.getString("WeaponAttackAction.WindSpec"));
                    }
                }
            }
        }

        return toHit;
    }

    static ToHitData processDefenderSPAs(ToHitData toHit, Entity ae, Entity te, Mounted<?> weapon, Game game) {

        if (null == te) {
            return toHit;
        }

        // Shaky Stick - Target gets a +1 bonus against Ground-to-Air attacks
        if (te.hasAbility(OptionsConstants.PILOT_SHAKY_STICK) &&
                  (te.isAirborne() || te.isAirborneVTOLorWIGE()) &&
                  !ae.isAirborne() &&
                  !ae.isAirborneVTOLorWIGE()) {
            toHit.addModifier(+1, Messages.getString("WeaponAttackAction.ShakyStick"));
        }
        // Terrain Abilities - Offboard units don't have a hex, so if an offboard unit has one of these
        // it will cause a NPE. Let's check to make sure the target entity is on the board:
        if (game.hasBoardLocationOf(te)) {
            // Urban Guerrilla - Target gets a +1 bonus in any sort of urban terrain
            if (te.hasAbility(OptionsConstants.INFANTRY_URBAN_GUERRILLA) &&
                      (game.getHexOf(te).containsTerrain(Terrains.PAVEMENT) ||
                             game.getHexOf(te).containsTerrain(Terrains.ROAD) ||
                             game.getHexOf(te).containsTerrain(Terrains.RUBBLE) ||
                             game.getHexOf(te).containsTerrain(Terrains.BUILDING) ||
                             game.getHexOf(te).containsTerrain(Terrains.ROUGH))) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.UrbanGuerilla"));
            }
            // Forest Ranger - Target gets a +1 bonus in wooded terrain when moving at
            // walking speed or greater
            if (te.hasAbility(OptionsConstants.PILOT_TM_FOREST_RANGER) &&
                      (game.getHexOf(te).containsTerrain(Terrains.WOODS) ||
                             game.getHexOf(te).containsTerrain(Terrains.JUNGLE)) &&
                      te.moved == EntityMovementType.MOVE_WALK) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.ForestRanger"));
            }
            // Swamp Beast - Target gets a +1 bonus in mud/swamp terrain when
            // running/flanking
            if (te.hasAbility(OptionsConstants.PILOT_TM_SWAMP_BEAST) &&
                      (game.getHexOf(te).containsTerrain(Terrains.MUD) ||
                             game.getHexOf(te).containsTerrain(Terrains.SWAMP)) &&
                      te.moved == EntityMovementType.MOVE_RUN) {
                toHit.addModifier(+1, Messages.getString("WeaponAttackAction.SwampBeast"));
            }
        }

        return toHit;
    }

    private ComputeAbilityMods() { }
}
