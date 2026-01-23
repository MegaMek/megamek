/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Minefield;
import megamek.common.equipment.enums.BombType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.game.Game;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.*;
import megamek.common.weapons.DamageType;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Class containing functionality that helps out with area effect weapons.
 *
 * @author NickAragua
 */
public class AreaEffectHelper {
    private static final MMLogger logger = MMLogger.create(AreaEffectHelper.class);

    // maps equipment name to blast radius index for fuel-air ordnance
    private static Map<String, Integer> fuelAirBlastRadiusIndex;
    private static final int[] fuelAirDamage = { 5, 10, 20, 30 };

    private static Map<Integer, NukeStats> nukeStats;

    /**
     * Worker function that initializes blast radius data for fuel-air explosives of various types.
     */
    private static void initializeFuelAirBlastRadiusIndexData() {
        fuelAirBlastRadiusIndex = new HashMap<>();

        fuelAirBlastRadiusIndex.put(BombTypeEnum.FAE_SMALL.getInternalName(), 2);
        fuelAirBlastRadiusIndex.put(BombTypeEnum.FAE_LARGE.getInternalName(), 3);

        // the following ammo types have the capability to load FAE munitions:
        // Arrow IV, Thumper, Sniper, Long Tom
        addFuelAirBlastRadiusIndex(AmmoTypeEnum.ARROW_IV, 2);
        addFuelAirBlastRadiusIndex(AmmoTypeEnum.THUMPER, 1);
        addFuelAirBlastRadiusIndex(AmmoTypeEnum.THUMPER_CANNON, 1);
        addFuelAirBlastRadiusIndex(AmmoTypeEnum.SNIPER, 2);
        addFuelAirBlastRadiusIndex(AmmoTypeEnum.SNIPER_CANNON, 2);
        addFuelAirBlastRadiusIndex(AmmoTypeEnum.LONG_TOM_CANNON, 3);
        addFuelAirBlastRadiusIndex(AmmoTypeEnum.LONG_TOM, 3);
    }

    /**
     * Worker function that initializes data for NUCLEAR WEAPONS
     */
    private static void initializeNukeStats() {
        nukeStats = new HashMap<>();

        NukeStats nukeEntry = new NukeStats();
        nukeEntry.baseDamage = 100;
        nukeEntry.degradation = 5;
        nukeEntry.secondaryRadius = 40;
        nukeEntry.craterDepth = 0;

        nukeStats.put(0, nukeEntry);
        nukeStats.put(1, nukeEntry);

        nukeEntry = new NukeStats();
        nukeEntry.baseDamage = 1000;
        nukeEntry.degradation = 23;
        nukeEntry.secondaryRadius = 86;
        nukeEntry.craterDepth = 1;

        nukeStats.put(2, nukeEntry);

        nukeEntry = new NukeStats();
        nukeEntry.baseDamage = 10000;
        nukeEntry.degradation = 109;
        nukeEntry.secondaryRadius = 184;
        nukeEntry.craterDepth = 3;

        nukeStats.put(3, nukeEntry);

        nukeEntry = new NukeStats();
        nukeEntry.baseDamage = 100000;
        nukeEntry.degradation = 505;
        nukeEntry.secondaryRadius = 396;
        nukeEntry.craterDepth = 5;

        nukeStats.put(4, nukeEntry);
    }

    /**
     * Helper function that adds elements to the fuel blast radius index
     */
    private static void addFuelAirBlastRadiusIndex(AmmoTypeEnum ammoType, int blastRadius) {
        // this is relatively inefficient, but probably the least inefficient of the
        // options
        // to acquire a list of the ammo types
        for (AmmoType at : AmmoType.getMunitionsFor(ammoType)) {
            if (at.getMunitionType().contains(AmmoType.Munitions.M_FAE)) {
                fuelAirBlastRadiusIndex.put(at.getInternalName(), blastRadius);
            }
        }
    }

    /**
     * Get the blast radius of a particular equipment type, given the internal name.
     */
    public static int getFuelAirBlastRadiusIndex(String name) {
        if (fuelAirBlastRadiusIndex == null) {
            initializeFuelAirBlastRadiusIndexData();
        }

        return fuelAirBlastRadiusIndex.getOrDefault(name, 0);
    }

    /**
     * Helper function that processes damage for fuel-air explosives. Single-entity version.
     */
    public static void processFuelAirDamage(Entity target, Coords center, EquipmentType ordnanceType, Entity attacker,
          Vector<Report> vPhaseReport, TWGameManager gameManager) {
        Game game = attacker.getGame();
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        boolean thinAir = conditions.getAtmosphere().isThin();
        // sanity check: if this attack is happening in vacuum through very thin atmosphere,
        // add that to the phase report and terminate early

        if (game.getBoard().isSpace()
              || conditions.getAtmosphere().isLighterThan(Atmosphere.THIN)) {
            Report r = new Report(9986);
            r.indent(1);
            r.subject = attacker.getId();
            r.newlines = 1;
            vPhaseReport.addElement(r);
            return;
        }

        int blastRadius = getFuelAirBlastRadiusIndex(ordnanceType.getInternalName());

        if (thinAir) {
            Report r = new Report(9990);
            r.indent(1);
            r.subject = attacker.getId();
            r.newlines = 1;
            vPhaseReport.addElement(r);
        }

        Vector<Integer> entitiesToExclude = new Vector<>();

        // determine distance to entity
        // look up damage on radius chart
        // (divided by half, round up for thin atmosphere)
        // not here, but in artilleryDamageEntity, make sure to 2x damage for infantry
        // outside of building
        // not here, but in artilleryDamageEntity, make sure to 1.5x damage for light
        // building or unit with armor BAR < 10
        // not here, but in artilleryDamageEntity, make sure to .5x damage for "castle
        // brian" or "armored" building
        // if any attacked unit is infantry or BA, roll 2d6 + current distance. Inf dies
        // on 9-, BA dies on 7-
        int distFromCenter = center.distance(target.getPosition());
        int damageBracket = blastRadius - distFromCenter;
        if (damageBracket < 0) {
            return;
        }

        int damage = AreaEffectHelper.fuelAirDamage[damageBracket];
        if (conditions.getAtmosphere().isThin()) {
            damage = (int) Math.ceil(damage / 2.0);
        }

        checkInfantryDestruction(target, distFromCenter, attacker, entitiesToExclude, vPhaseReport, game, gameManager);

        artilleryDamageEntity(target, damage, null, 0, false, false, false, 0, center, (AmmoType) ordnanceType,
              target.getPosition(), true,
              attacker, null, attacker.getId(), vPhaseReport, gameManager);
    }

    public static Vector<Integer> processFuelAirDamage(
          Coords center, int height, AmmoType ammo, Entity attacker,
          Vector<Report> vPhaseReport, TWGameManager gameManager
    ) {
        //LEGACY use boardId version
        return processFuelAirDamage(center, 0, height, ammo, attacker, vPhaseReport, gameManager);
    }

    /**
     * Helper function that processes damage for fuel-air explosives.
     */
    public static Vector<Integer> processFuelAirDamage(
          Coords center, int boardId, int height, AmmoType ammo, Entity attacker,
          Vector<Report> vPhaseReport, TWGameManager gameManager
    ) {
        Game game = attacker.getGame();
        Vector<Integer> entitiesToExclude = new Vector<>();
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        boolean thinAir = conditions.getAtmosphere().isThin();

        // sanity check: if this attack is happening in vacuum through very thin atmosphere,
        // add that to the phase report and terminate early
        if (game.getBoard(boardId).isSpace()
              || conditions.getAtmosphere().isLighterThan(Atmosphere.THIN)) {
            Report r = new Report(9986);
            r.indent(1);
            r.subject = attacker.getId();
            r.newlines = 1;
            vPhaseReport.addElement(r);
            return entitiesToExclude;
        }

        if (thinAir) {
            Report r = new Report(9990);
            r.indent(1);
            r.subject = attacker.getId();
            r.newlines = 1;
            vPhaseReport.addElement(r);
        }

        // assemble collection of <level, Coords> 3D locations at ranges 0 to radius
        // for each hex, invoke artilleryDamageHex, with the damage set according to
        // the blast shape (damage is divided by half, round up for thin atmosphere)
        // if any attacked unit is infantry or BA, roll 2d6 + current distance. Inf dies
        // on 9-, BA dies on 7-
        // Use DamageFalloff to signal building damage factor
        DamageFalloff falloff = calculateDamageFallOff(ammo, 0, false);
        HashMap<Entry<Integer, Coords>, Integer> blastShape = shapeBlast(
              ammo, center, falloff, height, !(ammo instanceof BombType), false, false, game, false
        );
        ArrayList<Coords> checkedAdditional = new ArrayList<>();

        for (Entry<Integer, Coords> entry : blastShape.keySet()) {
            Coords bCoords = entry.getValue();
            int bLevel = entry.getKey();
            int distance = bCoords.distance(center);
            int damage = blastShape.get(entry);
            if (thinAir) {
                damage = (int) Math.ceil(damage / 2.0);
            }
            gameManager.artilleryDamageHex(bCoords, boardId, center, damage, ammo, attacker.getId(),
                  attacker, null, false, bLevel, height, vPhaseReport, false,
                  entitiesToExclude, false, falloff);

            if (!checkedAdditional.contains(bCoords)) {
                // Perform additional destruction / ignition / minefield checks once per coordinate
                checkInfantryDestruction(bCoords, distance, attacker, entitiesToExclude, vPhaseReport, game,
                      gameManager);

                TargetRoll fireRoll = new TargetRoll(7, "fuel-air ordnance");
                gameManager.tryIgniteHex(bCoords, boardId, attacker.getId(), false, false, fireRoll, true, -1,
                      vPhaseReport);

                clearMineFields(bCoords, Minefield.CLEAR_NUMBER_WEAPON_ACCIDENT, attacker, vPhaseReport, game,
                      gameManager);

                checkedAdditional.add(bCoords);
            }
        }
        return entitiesToExclude;
    }

    /**
     * Worker function that checks for and implements instant infantry destruction due to fuel air ordnance, if
     * necessary. Checks all units at given coords.
     */
    public static void checkInfantryDestruction(Coords coords, int distFromCenter, Entity attacker,
          Vector<Integer> alreadyHit,
          Vector<Report> vPhaseReport, Game game, TWGameManager gameManager) {
        for (Entity entity : game.getEntitiesVector(coords)) {
            checkInfantryDestruction(entity, distFromCenter, attacker, alreadyHit, vPhaseReport, game, gameManager);
        }
    }

    /**
     * Worker function that checks for and implements instant infantry destruction due to fuel air ordnance, if
     * necessary. Single-entity version.
     */
    public static void checkInfantryDestruction(Entity entity, int distFromCenter, Entity attacker,
          Vector<Integer> alreadyHit, Vector<Report> vPhaseReport, Game game, TWGameManager gameManager) {
        int rollTarget;
        if (entity instanceof BattleArmor) {
            rollTarget = 7;
        } else if (entity instanceof Infantry) {
            rollTarget = 9;
        } else {
            return;
        }

        Roll diceRoll = Compute.rollD6(2);
        int rollValue = diceRoll.getIntValue() + distFromCenter;
        String rollCalc = rollValue + " [" + diceRoll.getIntValue() + " + " + distFromCenter + "]";
        boolean destroyed = rollValue <= rollTarget;

        Report report = new Report(9987);
        report.indent(1);
        report.subject = attacker.getId();
        report.newlines = 1;
        report.add(rollTarget);
        report.addDataWithTooltip(rollCalc, diceRoll.getReport());
        report.add(distFromCenter);
        report.choose(destroyed);
        vPhaseReport.addElement(report);

        if (destroyed) {
            vPhaseReport.addAll(gameManager.destroyEntity(entity, "fuel-air ordnance detonation", false, false));
            alreadyHit.add(entity.getId());
        }
    }

    /**
     * Worker function that clears minefields.
     */
    public static void clearMineFields(Coords targetPos, int targetNum, Entity ae, Vector<Report> vPhaseReport,
          Game game, TWGameManager gameManager) {
        Enumeration<Minefield> minefields = game.getMinefields(targetPos).elements();
        ArrayList<Minefield> mfRemoved = new ArrayList<>();
        while (minefields.hasMoreElements()) {
            Minefield mf = minefields.nextElement();
            if (gameManager.clearMinefield(mf, ae, targetNum, vPhaseReport)) {
                mfRemoved.add(mf);
            }
        }
        // we have to do it this way to avoid a concurrent error problem
        for (Minefield mf : mfRemoved) {
            gameManager.removeMinefield(mf);
        }
    }

    /**
     * Worker function that does artillery damage to an entity. Extracted from Server.artilleryDamageHex()
     *
     * @param entity         The entity to damage
     * @param damage         The amount of damage to do
     * @param bldg           The building, if any, in that hex
     * @param bldgAbsorbs    How much damage, if any, the building will absorb
     * @param variableDamage Whether to roll a d6 for the number of hits
     * @param asfFlak        Whether we are making a flak attack against an aerospace unit
     * @param flak           Whether we are making a flak attack
     * @param altitude       Altitude of the attack
     * @param attackSource   The coordinates at which the shell was targeted, for hit table resolution
     * @param ammo           The ammo type used
     * @param coords         The coordinates where the shell actually landed
     * @param isFuelAirBomb  Whether we are making a fuel-air attack
     * @param killer         The entity that initiated the attack
     * @param hex            The hex, if any, where the shell landed
     * @param subjectId      The ID of the entity carrying out the attack, for reporting in double-blind games
     * @param vPhaseReport   Vector of reports to which we append reports
     * @param gameManager    GameManager object for invocation of various methods
     */
    public static void artilleryDamageEntity(Entity entity, int damage, IBuilding bldg, int bldgAbsorbs,
          boolean variableDamage, boolean asfFlak, boolean flak, int altitude, Coords attackSource, AmmoType ammo,
          Coords coords, boolean isFuelAirBomb, Entity killer, Hex hex, int subjectId, Vector<Report> vPhaseReport,
          TWGameManager gameManager) {
        Report report;

        int hits = damage;
        if (variableDamage) {
            hits = Compute.d6(damage);
        }
        ToHitData toHit = new ToHitData();
        if (entity instanceof ProtoMek) {
            toHit.setHitTable(ToHitData.HIT_SPECIAL_PROTO);
        }
        int cluster = 5;
        int effectiveLevel = (hex != null) ? hex.getLevel() : 0;
        int entityLevel = (entity.isAirborne()) ? entity.getAltitude() : entity.getElevation() + effectiveLevel;

        // Check: is entity inside building?
        if ((bldg != null) && (bldgAbsorbs > 0) && hex != null
              && (entity.getElevation() < hex.terrainLevel(Terrains.BLDG_ELEV))) {
            cluster -= bldgAbsorbs;
            // some buildings scale remaining damage that is not absorbed
            // TODO : this isn't quite right for castles brian
            cluster = (int) Math.floor(bldg.getDamageToScale() * cluster);
            if (entity instanceof Infantry) {
                return; // took its damage already from building damage
            } else if (cluster <= 0) {
                // entity takes no damage
                report = new Report(6426);
                report.subject = subjectId;
                report.addDesc(entity);
                vPhaseReport.add(report);
                return;
            } else {
                report = new Report(6425);
                report.subject = subjectId;
                report.add(bldgAbsorbs);
                vPhaseReport.add(report);
            }
        }

        // flak against ASF should only hit Aerospace, because their elevation
        // is actually altitude, so shouldn't hit VTOLs
        if (asfFlak && !(entity.isAirborne())) {
            return;
        }

        // Flak should only hit VTOLs or similar craft.
        // Correct elevation/altitude checks should happen elsewhere
        if (flak) {
            // Check: is entity not a VTOL in flight or an ASF
            if (!((entity instanceof VTOL)
                  || (entity.getMovementMode() == EntityMovementMode.VTOL)
                  || entity.isAero())) {
                return;
            }
            // "Altitude" here is either true Altitude (Aerospace) or objective levels from 0;
            // entityLevel will then either be Altitude or hex level + elevation
            if (entityLevel != altitude) {
                return;
            }
        }

        // Work out hit table to use
        if (attackSource != null) {
            toHit.setSideTable(entity.sideTable(attackSource));
            if (ammo != null) {
                if (ammo.getMunitionType().contains(AmmoType.Munitions.M_ADA)) {
                    if (entity.isAero()) {
                        toHit.setHitTable(ToHitData.HIT_BELOW);
                    }
                    // Also update cluster value to be identical to damage;
                    // this should also be done for homing, I believe
                    cluster = damage;
                } else if (ammo.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)
                      && attackSource.equals(coords)) {
                    if (entity instanceof Mek) {
                        toHit.setHitTable(ToHitData.HIT_ABOVE);
                    } else if (entity instanceof Tank) {
                        toHit.setSideTable(ToHitData.SIDE_FRONT);
                        toHit.addModifier(2, "cluster artillery hitting a Tank");
                    }
                }

                // According to TW, need to set attack to hit either front or back, 50/50
                if (ammo instanceof BombType) {
                    toHit.setSideTable((Compute.d6() <= 3) ? ToHitData.SIDE_FRONT : ToHitData.SIDE_REAR);
                }
            }
        }

        // Conventional infantry take x2 in the open and
        // an additional x2 damage from AE weapons in general but
        // this is handled in main damageEntity() function now.
        if (entity.isConventionalInfantry() && isFuelAirBomb) {
            // if it's fuel-air, we take more damage!
            hits *= 2;
        }

        boolean specialCaseFlechette = false;

        // Entity/ammo specific damage modifiers
        if (ammo != null) {
            if (ammo.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)) {
                if (hex != null && hex.containsTerrain(Terrains.FORTIFIED) && entity.isConventionalInfantry()) {
                    hits *= 2;
                }
            }
            // fuel-air bombs do an additional 2x damage to infantry
            else if (ammo.getMunitionType().contains(AmmoType.Munitions.M_FLECHETTE)) {

                // wheeled and hover tanks take movement critical
                if ((entity instanceof Tank)
                      && ((entity.getMovementMode() == EntityMovementMode.WHEELED)
                      || (entity.getMovementMode() == EntityMovementMode.HOVER))) {
                    report = new Report(6480);
                    report.subject = entity.getId();
                    report.addDesc(entity);
                    report.add(toHit.getTableDesc());
                    report.add(0);
                    vPhaseReport.add(report);
                    vPhaseReport.addAll(gameManager.vehicleMotiveDamage((Tank) entity, 0));
                    return;
                }

                // only infantry and support vees with bar < 5 are affected
                if ((entity instanceof BattleArmor) || ((entity instanceof SupportTank)
                      && !entity.hasPatchworkArmor() && (entity.getBARRating(1) > 4))) {
                    return;
                }
                if (entity instanceof Infantry) {
                    hits = Compute.d6(damage);
                    hits *= 2;
                } else {
                    if ((entity.getBARRating(1) < 5) && !entity.hasPatchworkArmor()) {
                        switch (ammo.getAmmoType()) {
                            case LONG_TOM:
                                // hack: check if damage is still at 4, so we're in the center hex. otherwise, do no
                                // damage
                                if (damage != 4) {
                                    return;
                                }

                                break;
                            case SNIPER:
                                // hack: check if damage is still at 2, so we're in the center hex. otherwise, do no
                                // damage
                                if (damage != 2) {
                                    return;
                                }
                                break;
                            case THUMPER:
                                // no need to check for damage, because falloff = damage for the thumper
                                break;
                            default:
                                // other types require no special treatment
                                break;
                        }
                    } else {
                        // ugh, patchwork armor
                        // rules as written don't deal with this reset the damage to standard arty damage when we
                        // have each cluster's hit location, we'll multiply by the BAR-difference to BAR 5, per a
                        // rules question email
                        specialCaseFlechette = true;
                        switch (ammo.getAmmoType()) {
                            case LONG_TOM:
                                // hack: check if damage is still at 4, so
                                // we're in the center hex. otherwise, do no damage
                                if (damage != 4) {
                                    return;
                                }
                                break;
                            case SNIPER:
                                // hack: check if damage is still at 2, so we're in the center hex. otherwise, do no
                                // damage
                                if (damage != 2) {
                                    return;
                                }
                                break;
                            case THUMPER:
                                // no need to check for damage, because
                                // falloff = damage for the thumper
                                break;
                            default:
                                // other types require no special treatment
                                break;
                        }
                    }
                }
            }
        }

        // Do the damage
        report = new Report(6480);
        report.subject = entity.getId();
        report.addDesc(entity);
        report.add(toHit.getTableDesc());
        report.add(hits);
        vPhaseReport.add(report);

        // Reveal hidden entity caught in the blast (per TW pg. 259)
        if (entity.isHidden()) {
            entity.setHidden(false);
            report = new Report(9963);
            report.subject = entity.getId();
            report.addDesc(entity);
            vPhaseReport.add(report);
        }

        if (entity instanceof BattleArmor) {
            // BA take full damage to each trooper, ouch!
            for (int loc = 0; loc < entity.locations(); loc++) {
                if (entity.getInternal(loc) > 0) {
                    HitData hit = new HitData(loc);
                    vPhaseReport.addAll(gameManager.damageEntity(entity, hit, hits,
                          false, DamageType.NONE, false, false, false));
                }
            }
        } else {
            while (hits > 0) {
                int damageToDeal = Math.min(cluster, hits);
                HitData hit = entity.rollHitLocation(toHit.getHitTable(), toHit.getSideTable());
                // per a rules question, for patchwork armor being attacked by fléchette ammo, we multiply the damage
                // done by 5 - the BAR rating of the hit location
                if (specialCaseFlechette && !(entity instanceof Infantry)) {
                    damageToDeal *= (5 - entity.getBARRating(hit.getLocation()));
                    // fuel-air bombs do 1.5x damage to locations hit that have a BAR rating of less than 10.
                } else if (isFuelAirBomb && !(entity instanceof Infantry)
                      && (entity.getBARRating(hit.getLocation()) < 10)) {
                    damageToDeal = (int) Math.ceil(damageToDeal * 1.5);

                    report = new Report(9991);
                    report.indent(1);
                    report.subject = killer.getId();
                    report.newlines = 1;
                    vPhaseReport.addElement(report);
                }
                vPhaseReport.addAll(gameManager.damageEntity(entity,
                      hit,
                      damageToDeal,
                      false,
                      DamageType.NONE,
                      false,
                      true,
                      false));
                hits -= Math.min(cluster, hits);
            }
        }
        if (killer != null) {
            gameManager.creditKill(entity, killer);
        }
    }

    /**
     * Calculate the damage and falloff for a particular ammo type, taking into account whether the attack is being
     * carried out by a battle armor squad or a mine clearance attack. Also sets a "cluster munitions" flag as
     * appropriate.
     *
     * @param ammo        AmmoType being used for the attack
     * @param attackingBA How many BA suits are in the squad if this is a BA Tube arty attack, -1 otherwise
     * @param mineClear   Whether we're clearing a minefield
     *
     * @return A DamageFalloff object containing the damage and falloff values and if it is cluster or not
     */
    public static DamageFalloff calculateDamageFallOff(AmmoType ammo, int attackingBA, boolean mineClear) {
        if (ammo == null) {
            logger.error("Attempting to calculate damage fall-off with null ammo.\n\n{}",
                  Arrays.toString(Thread.currentThread().getStackTrace()));

            DamageFalloff empty = new DamageFalloff();
            empty.damage = 0;
            empty.falloff = 10;
            empty.clusterMunitionsFlag = false;
            return empty;
        }

        int damage = (ammo instanceof BombType) ? ammo.getDamagePerShot() : ammo.getRackSize();
        int falloff = 10;
        int radius = 0;
        boolean clusterMunitionsFlag = false;

        if (List.of(
              AmmoType.AmmoTypeEnum.LONG_TOM,
              AmmoType.AmmoTypeEnum.LONG_TOM_PRIM,
              AmmoType.AmmoTypeEnum.LONG_TOM_CANNON,
              AmmoType.AmmoTypeEnum.SNIPER,
              AmmoType.AmmoTypeEnum.SNIPER_CANNON,
              AmmoType.AmmoTypeEnum.THUMPER,
              AmmoType.AmmoTypeEnum.THUMPER_CANNON,
              AmmoType.AmmoTypeEnum.ARROW_IV,
              AmmoType.AmmoTypeEnum.ARROWIV_PROTO
        ).contains(ammo.getAmmoType())
        ) {
            radius = (int) (Math.ceil(1.0 * damage / falloff) - 1);
            // Fuel-Air munitions get special radius
            if (ammo.getMunitionType().contains(Munitions.M_FAE)) {
                damage = switch (ammo.getAmmoType()) {
                    case LONG_TOM, LONG_TOM_CANNON, LONG_TOM_PRIM -> 30;
                    case SNIPER, SNIPER_CANNON, ARROW_IV, ARROWIV_PROTO -> 20;
                    case THUMPER, THUMPER_CANNON -> 10;
                    default -> 0; // Should not happen...
                };
                radius = getFuelAirBlastRadiusIndex(ammo.getInternalName());
            }
        }

        // Capital and Sub-capital missiles
        if (ammo.getAmmoType() == AmmoType.AmmoTypeEnum.KRAKEN_T
              || ammo.getAmmoType() == AmmoType.AmmoTypeEnum.KRAKENM
              || ammo.getAmmoType() == AmmoType.AmmoTypeEnum.MANTA_RAY) {
            damage = 50;
            falloff = 25;
            radius = 1;
        }
        if (ammo.getAmmoType() == AmmoType.AmmoTypeEnum.KILLER_WHALE
              || ammo.getAmmoType() == AmmoType.AmmoTypeEnum.KILLER_WHALE_T
              || ammo.getAmmoType() == AmmoType.AmmoTypeEnum.SWORDFISH
              || ammo.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
            damage = 40;
            falloff = 20;
            radius = 1;
        }
        if (ammo.getAmmoType() == AmmoType.AmmoTypeEnum.STINGRAY) {
            damage = 35;
            falloff = 17;
            radius = 2;
        }
        if (ammo.getAmmoType() == AmmoType.AmmoTypeEnum.WHITE_SHARK
              || ammo.getAmmoType() == AmmoType.AmmoTypeEnum.WHITE_SHARK_T
              || ammo.getAmmoType() == AmmoType.AmmoTypeEnum.PIRANHA
              || ammo.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
            damage = 30;
            falloff = 15;
            radius = 1;
        }
        if (ammo.getAmmoType() == AmmoType.AmmoTypeEnum.BARRACUDA
              || ammo.getAmmoType() == AmmoType.AmmoTypeEnum.BARRACUDA_T
              || ammo.hasFlag(AmmoType.F_AR10_BARRACUDA)) {
            damage = 20;
            falloff = 10;
            radius = 1;
        }
        if (ammo.getAmmoType() == AmmoType.AmmoTypeEnum.CRUISE_MISSILE) {
            falloff = 25;
            radius = (int) (Math.ceil(1.0 * damage / falloff) - 1);
        }

        // BA specific
        if (ammo.getAmmoType() == AmmoType.AmmoTypeEnum.BA_TUBE) {
            damage *= attackingBA;
            falloff = 2 * attackingBA;
            // All BA Tube attacks are R1
            radius = 1;
        }
        if (ammo.getAmmoType() == AmmoType.AmmoTypeEnum.BA_MICRO_BOMB) {
            damage = 2 * attackingBA;
            falloff = 2 * attackingBA;
            // All BA Tube attacks are R1
            radius = 1;
            clusterMunitionsFlag = true;
        }

        // Air-Defense Arrow IV missiles
        if (ammo.getAmmoType() == AmmoType.AmmoTypeEnum.ARROW_IV
              && ammo.getMunitionType().contains(AmmoType.Munitions.M_ADA)) {
            falloff = damage;
            // ADA does not have a radius as per normal Artillery / AE weapons.
            radius = -1;
        }

        // Bombs require specific handling
        if (ammo instanceof BombType bomb) {
            if (List.of(BombTypeEnum.FAE_SMALL, BombTypeEnum.FAE_LARGE).contains(bomb.getBombType())) {
                radius = getFuelAirBlastRadiusIndex((bomb.getInternalName()));
            }
            if (bomb.getBombType() == BombTypeEnum.CLUSTER) {
                damage = 5;
                falloff = 0;
                radius = 1;
                clusterMunitionsFlag = true;
            }
            if (bomb.getBombType() == BombTypeEnum.ARROW) {
                damage = bomb.getRackSize();
                falloff = 10;
                radius = 1;
            }
            if (bomb.getBombType() == BombTypeEnum.HOMING) {
                damage = bomb.getRackSize();
                falloff = 20;
                radius = -1;
            }
        }

        if (ammo.getMunitionType().contains(Munitions.M_CLUSTER)) {
            // non-arrow-iv cluster does 5 less than standard
            if (ammo.getAmmoType() != AmmoType.AmmoTypeEnum.ARROW_IV) {
                damage -= 5;
            }
            // thumper gets falloff 9 for 1 damage at 1 hex range
            if (ammo.getAmmoType() == AmmoType.AmmoTypeEnum.THUMPER) {
                falloff = 9;
            }

            // All Cluster munitions are R1.
            radius = 1;
            clusterMunitionsFlag = true;

        } else if (ammo.getMunitionType().contains(Munitions.M_FLECHETTE)) {
            // TODO: update to current TacOps rules (damage differs between armor types)
            falloff = switch (ammo.getAmmoType()) {
                // for fléchette, damage and falloff is number of d6, not absolute
                // damage
                case LONG_TOM -> {
                    damage = 4;
                    radius = 2;
                    yield 2;
                }
                case SNIPER -> {
                    damage = 2;
                    radius = 1;
                    yield 1;
                }
                case THUMPER -> {
                    damage = 1;
                    radius = 1;
                    yield 1;
                }
                default -> falloff;
            };
            // if this was a mine clearance, then it only affects the hex hit
        } else if (mineClear) {
            falloff = damage;
            radius = 0;
        }

        DamageFalloff retVal = new DamageFalloff();
        retVal.damage = damage;
        retVal.falloff = falloff;
        retVal.radius = radius;
        retVal.clusterMunitionsFlag = clusterMunitionsFlag;

        return retVal;
    }

    /**
     * Abbreviated nuclear explosion logic when the weapon is targeted at a single off-board entity.
     */
    public static void doNuclearExplosion(Entity entity, Coords coords, int nukeType, Vector<Report> vPhaseReport,
          TWGameManager gameManager) {

        // this +1 is necessary because the drawNuke method subtracts 1 from them
        int[] nukeArgs = { coords.getX() + 1, coords.getY() + 1 };
        gameManager.drawNukeHitOnBoard(nukeArgs);

        NukeStats nukeStats = getNukeStats(nukeType);

        if (nukeStats == null) {
            Report r = new Report(9998);
            r.add(nukeType);
            vPhaseReport.add(r);
            return;
        }

        Report r = new Report(1215, Report.PUBLIC);

        r.indent();
        r.add("offboard");
        vPhaseReport.add(r);

        // crater radius is crater depth x2 as per Server.doNuclearExplosion
        int craterRadius = nukeStats.craterDepth * 2;
        int blastDistance = entity.getPosition().distance(coords);

        // if the entity is in the crater radius, bye
        if (blastDistance < craterRadius) {
            vPhaseReport.addAll(gameManager.destroyEntity(entity, "nuclear explosion proximity", false, false));
            // Kill the crew
            entity.getCrew().setDoomed(true);
            // no need to do any more damage, it's already destroyed.
            return;
        }

        // calculate the damage to the entity based on the range to the nuke
        int damageToEntity = nukeStats.baseDamage - (blastDistance * nukeStats.degradation);
        if (damageToEntity < 0) {
            return;
        } else {
            applyExplosionClusterDamageToEntity(entity, damageToEntity, 5, coords, vPhaseReport, gameManager);
        }

        // if the entity hasn't been blown up yet,
        // Apply secondary effects against the entity if it's within the secondary blast
        // radius
        // Since the effects are unit-dependent, we'll just define it in the
        // entity.
        if (!entity.isDestroyed() && (blastDistance <= nukeStats.secondaryRadius)) {
            gameManager.applySecondaryNuclearEffects(entity, coords, vPhaseReport);
        }

    }

    /**
     * Apply a series of cluster hits to the given entity, as from an explosion at a particular position. Generate
     * reports for each cluster.
     */
    public static void applyExplosionClusterDamageToEntity(Entity entity, int damage, int clusterAmt,
          Coords position, Vector<Report> vDesc,
          TWGameManager gameManager) {
        Report r = new Report(6175);
        r.subject = entity.getId();
        r.indent(2);
        r.addDesc(entity);
        r.add(damage);
        vDesc.addElement(r);

        // Reveal hidden entity caught in the blast (per TW pg. 259)
        if (entity.isHidden()) {
            entity.setHidden(false);
            Report revealReport = new Report(9963);
            revealReport.subject = entity.getId();
            revealReport.addDesc(entity);
            vDesc.addElement(revealReport);
        }

        while (damage > 0) {
            int cluster = Math.min(clusterAmt, damage);
            if (entity instanceof Infantry) {
                cluster = damage;
            }
            int table = ToHitData.HIT_NORMAL;
            if (entity instanceof ProtoMek) {
                table = ToHitData.HIT_SPECIAL_PROTO;
            }
            HitData hit = entity.rollHitLocation(table, entity.sideTable(position));
            vDesc.addAll(gameManager.damageEntity(entity, hit, cluster, false,
                  DamageType.IGNORE_PASSENGER, false, true));

            // If there is nothing left to destroy in the unit
            if (
                  entity.isDoomed()
                        && (!entity.hasUndamagedCriticalSlots()
                        || entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED)
            ) {
                break;
            }
            damage -= cluster;
        }
    }

    /**
     * Helper function that retrieves the stat block for a particular type of nuke, or null if such a type is not
     * defined.
     */
    public static NukeStats getNukeStats(int nukeType) {
        if (nukeStats == null) {
            initializeNukeStats();
        }

        if (nukeStats.containsKey(nukeType)) {
            return nukeStats.get(nukeType);
        }

        return null;
    }

    /**
     * Computes the semi-3D blast shapes of AE and Artillery explosions using rules from: - Total Warfare pp. 173, XXX,
     * and - Tactical Operations: Advanced Rules pp. 150, YYY
     * <p>
     * The shape of an Area Effect explosion always includes a ring, but may have radius 0. In this case only units in
     * the hit hex are affected. Additionally, an AE attack that hits a building or water hex deals damage above and
     * below the level that was hit: - For R0 AE attacks: 1/2 to the level above, and 1/2 damage to the level below, the
     * level that was hit/targeted in the central hex.  E.g. a WiGE vehicle at elevation 1 in a water hex would take 1/2
     * damage from any AE attack that targeted and hit that hex.  Any underwater vehicle at depth 1 below the surface,
     * likewise. - For R1 and larger: full damage to 1 level above and below the targeted level in that hex. 1/2 damage
     * to the levels 2 above and 2 below the level hit. Also, 1/2 damage in an R1 ring at 1 level above and 1 level
     * below.
     * <p>
     * Further, cumulatively, Artillery attacks deal extra damage above the hexes they hit no matter what type of hex
     * they are (except Building and Mobile Structures): - For any Artillery: apply base damage - (10 * level) for every
     * level above the base hex until no damage would be dealt. Each unit hit can only be hit once by this damage - For
     * Flak Artillery: apply base damage - (10 * level) for every level below the target's elevation as well.
     * <p>
     * We do not currently support artillery flak fire at Low-Altitude units.
     * <p>
     * Specific edge cases to consider: 1. Counter-battery fire - only worry about the ring at surface level.
     *
     * @param ammo          AmmoType of the attack.
     * @param center        Coordinates of center of blast.
     * @param height        Elevation/level of target, or Altitude if firing on ASFs
     * @param artillery     true if artillery attack; false if other AE attack
     * @param flak          true if flak attack.
     * @param asfFlak       true if flak attack on an Aerospace unit.
     * @param game          Reference to game, for terrain checks
     * @param excludeCenter Used for creating all height, hex values but the center
     *
     * @return (height, Coords): damage map.
     */
    public static HashMap<Entry<Integer, Coords>, Integer> shapeBlast(
          AmmoType ammo, Coords center, int height, boolean artillery,
          boolean flak, boolean asfFlak, Game game, boolean excludeCenter
    ) {
        // Use default falloff for this ammo.
        DamageFalloff falloff = calculateDamageFallOff(ammo, 0, false);
        return shapeBlast(
              ammo, center, falloff, height, artillery, flak, asfFlak, game, excludeCenter
        );
    }

    public static HashMap<Entry<Integer, Coords>, Integer> shapeBlast(
          @Nullable AmmoType ammo, Coords center, DamageFalloff falloff, int height, boolean artillery,
          boolean flak, boolean asfFlak, Game game, boolean excludeCenter) {
        // LEGACY - replace with boardId version
        return shapeBlast(ammo, center, Game.DEFAULT_BOARD_ID, falloff, height, artillery, flak, asfFlak, game,
              excludeCenter);
    }

    /**
     * @param ammo          AmmoType of the attack.
     * @param center        Coordinates of center of blast.
     * @param falloff       Preset falloff to use for this blast; mainly for BA Tube attack.
     * @param height        Elevation/level of target, or Altitude if firing on ASFs
     * @param artillery     true if artillery attack; false if other AE attack
     * @param flak          true if flak attack.
     * @param asfFlak       true if flak attack on an Aerospace unit.
     * @param game          Reference to game, for terrain checks
     * @param excludeCenter Used for creating all height, hex values but the center
     *
     * @return (height, Coords): damage map.
     */
    public static HashMap<Entry<Integer, Coords>, Integer> shapeBlast(
          @Nullable AmmoType ammo, Coords center, int boardId, DamageFalloff falloff, int height, boolean artillery,
          boolean flak, boolean asfFlak, Game game, boolean excludeCenter) {

        HashMap<Entry<Integer, Coords>, Integer> blastShape = new LinkedHashMap<>();

        if (game == null) {
            // Nothing to be done
            return blastShape;
        }

        // Falloff is defined separately for each weapon and ammo type, unfortunately.
        int baseDamage = falloff.damage;
        int radius = falloff.radius;
        boolean isCruiseMissile = (ammo != null) && (ammo.getAmmoType() == AmmoType.AmmoTypeEnum.CRUISE_MISSILE);
        boolean isFaeAmmo = (ammo != null) && ammo.getMunitionType().contains(AmmoType.Munitions.M_FAE);
        boolean isFaeBomb = (ammo instanceof BombType bombType) && bombType.isFaeBomb();

        // We may want to calculate the blast zone without the center hex, for separate handling.
        if (!excludeCenter) {
            blastShape.put(Map.entry(height, center), baseDamage);
            if (asfFlak) {
                // Only the central hex matters for altitude-level attacks
                return blastShape;
            }
            if (radius == -1) {
                // No blast effects at all, only central hex affected
                // e.g. Arrow IV Homing
                return blastShape;
            }
        }

        Hex hex = game.getHex(center, boardId);
        boolean effectivelyAE = (hex != null && hex.containsAnyTerrainOf(Set.of(Terrains.BUILDING, Terrains.WATER)));
        // 1. Handle Artillery-specific blast column (N levels up from _any_ hit where N
        // is base damage / 10 for most artillery, damage / 25 for Cruise Missiles)
        // Note that this falloff is separate from horizontal blast falloff, above.
        // Also deal damage downward for Flak shots against VTOLs.
        if (artillery) {
            int levelMinus = isCruiseMissile ? 25 : 10;
            if (flak || !effectivelyAE) {
                // If non-Flak artillery is hitting a building or water hex, use the AE column rules
                for (int d = (baseDamage - levelMinus), l = height + 1; d > 0; d -= levelMinus, l++) {
                    blastShape.put(Map.entry(l, center), d);
                }
            }
            if (flak) {
                for (int d = (baseDamage - levelMinus), l = height - 1; d > 0; d -= levelMinus, l--) {
                    blastShape.put(Map.entry(l, center), d);
                }
            }
        }

        // 2. Handle normal blast shaping.  Applies to both Artillery and AE in general.
        // This is a donut of a set radius around the center hex coordinates.
        // This can be applied to off-board units (see: counter-battery fire)
        // so we don't check for terrain type here.
        // Always exclude the center here: either we already made it, above, or we don't want it.
        blastShape.putAll(AreaEffectHelper.shapeBlastRing(
              center, falloff, height, true
        ));

        // 2.1 For FAE munitions, add a ring of 5 damage
        if (isFaeAmmo || isFaeBomb) {
            List<Coords> ringCoords = center.allAtDistance(radius);
            for (Coords c : ringCoords) {
                blastShape.put(Map.entry(height, c), 5);
            }
        }

        // 3. Handle additional AE blast shaping.
        // If this is the center of an AE explosion hitting a building or water hex,
        // also deal damage 1 (or 2) levels up, and 1 (or 2) levels down.  TW: pp. 113, 172, 173.
        // TODO: implement MOF-based building level drift, building target level selection for user.
        if (effectivelyAE) {
            // Artillery can only target ground level, Homing target units, or Flak target entities so
            // Calculate the center height and depth.
            if (radius > 0) {
                blastShape.put(Map.entry(height + 1, center), baseDamage);
                blastShape.put(Map.entry(height + 2, center), (int) Math.ceil(baseDamage / 2.0));
                blastShape.put(Map.entry(height - 1, center), baseDamage);
                blastShape.put(Map.entry(height - 2, center), (int) Math.ceil(baseDamage / 2.0));
            } else {
                blastShape.put(Map.entry(height + 1, center), (int) Math.ceil(baseDamage / 2.0));
                blastShape.put(Map.entry(height - 1, center), (int) Math.ceil(baseDamage / 2.0));
            }

            // R1+ AE attacks also generate 1/2-damage rings around the +1 and -1 levels of the center hex.
            if (radius > 0) {
                // Upper blast ring looks like >> | 1/2 damage || center || 1/2 damage | << so we need a different
                // falloff value.  We automatically subtract the falloff value for each radius outside 1 so
                // double the computed damage and set the falloff to equal it.
                blastShape.putAll(AreaEffectHelper.shapeBlastRing(
                      center, falloff, height + 1, true
                ));
                blastShape.putAll(AreaEffectHelper.shapeBlastRing(
                      center, falloff, height - 1, true
                ));
            }

        }

        return blastShape;
    }

    public static HashMap<Entry<Integer, Coords>, Integer> shapeBlastRing(
          Coords center, DamageFalloff falloff, int height, boolean excludeCenter
    ) {
        HashMap<Entry<Integer, Coords>, Integer> blastRing = new HashMap<>();

        // We may want to calculate the blast zone without the center hex, for separate handling.
        if (!excludeCenter) {
            blastRing.put(Map.entry(height, center), falloff.damage);
        }

        // Note that Cluster Bombs have no damage falloff (5/5 damage) but only R1.
        int blastDamage = falloff.damage - falloff.falloff;
        for (int ring = 1; blastDamage > 0 && ring <= falloff.radius; ring++, blastDamage -= falloff.falloff) {
            List<Coords> ringCoords = center.allAtDistance(ring);
            for (Coords c : ringCoords) {
                blastRing.put(Map.entry(height, c), blastDamage);
            }
        }

        return blastRing;
    }
}
