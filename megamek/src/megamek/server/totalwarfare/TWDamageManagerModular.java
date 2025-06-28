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

package megamek.server.totalwarfare;

import java.util.List;
import java.util.Vector;

import megamek.common.*;
import megamek.common.AmmoType.Munitions;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.Atmosphere;
import megamek.common.weapons.DamageType;
import megamek.logging.MMLogger;
import megamek.server.IDamageManager;
import megamek.server.ServerHelper;

public class TWDamageManagerModular extends TWDamageManager implements IDamageManager {
    private static final MMLogger logger = MMLogger.create(TWDamageManagerModular.class);

    public TWDamageManagerModular() {
        super();
    }

    public TWDamageManagerModular(TWGameManager manager) {
        super(manager);
    }

    public TWDamageManagerModular(TWGameManager manager, Game game) {
        super(manager, game);
    }

    /**
     * Top-level damage function; calls specialized functions to deal with damage to specific unit types.
     *
     * @param entity        Entity being damaged
     * @param hit           HitData recording aspects of the incoming damage
     * @param damage        Actual amount of incoming damage
     * @param ammoExplosion Whether damage was caused by an ammo explosion
     * @param damageType    Type of damage, mainly used for specialized armor
     * @param damageIS      Whether damage is going straight to the internal structure
     * @param areaSatArty   Whether damage is caused by AE attack
     * @param throughFront  Through front arc or no, for some specialized armors
     * @param underWater    Whether damage is being dealt underwater, for breach check
     * @param nukeS2S       Whether damage is from a nuclear weapon
     * @param reportVec     Vector of Reports containing prior reports; usually modded and returned
     *
     * @return A Vector of {@link Report} objects
     */
    @Override
    public Vector<Report> damageEntity(Entity entity, HitData hit, int damage, boolean ammoExplosion,
          DamageType damageType, boolean damageIS, boolean areaSatArty, boolean throughFront, boolean underWater,
          boolean nukeS2S, Vector<Report> reportVec) {

        Report report;
        int entityId = entity.getId();

        // if this is a fighter squadron, then pick an active fighter and pass on the damage
        if (entity instanceof FighterSquadron) {
            damageSquadronFighter(reportVec,
                  entity,
                  hit,
                  damage,
                  ammoExplosion,
                  damageType,
                  damageIS,
                  areaSatArty,
                  throughFront,
                  underWater,
                  nukeS2S);
            return reportVec;
        }

        // show Locations which have rerolled with Edge
        HitData undoneLocation = hit.getUndoneLocation();
        while (undoneLocation != null) {
            report = new Report(6500);
            report.subject = entityId;
            report.indent(2);
            report.addDesc(entity);
            report.add(entity.getLocationAbbr(undoneLocation));
            reportVec.addElement(report);
            undoneLocation = undoneLocation.getUndoneLocation();
        }
        // if Edge was used, give overview of remaining Edge at the end
        if (hit.getUndoneLocation() != null) {
            report = new Report(6510);
            report.subject = entityId;
            report.indent(2);
            report.addDesc(entity);
            report.add(entity.getCrew().getOptions().intOption(OptionsConstants.EDGE));
            reportVec.addElement(report);
        }

        // TACs from the hit location table
        int crits;
        if ((hit.getEffect() & HitData.EFFECT_CRITICAL) == HitData.EFFECT_CRITICAL) {
            crits = 1;
        } else {
            crits = 0;
        }

        // Store information to pass around
        ModsInfo mods = createDamageModifiers(entity, hit, damageIS, damage, crits);
        mods.critBonus = calcCritBonus(game.getEntity(hit.getAttackerId()), entity, damage, areaSatArty);

        // Battle Armor takes full damage to each trooper from area-effect.
        if (areaSatArty && (entity instanceof BattleArmor)) {
            damageMultipleBAs(reportVec,
                  entity,
                  hit,
                  damage,
                  ammoExplosion,
                  damageType,
                  damageIS,
                  areaSatArty,
                  throughFront,
                  underWater,
                  nukeS2S,
                  mods);
            return reportVec;
        }

        // Some "hits" on a ProtoMek are actually misses.
        if ((entity instanceof ProtoMek proto) && (hit.getLocation() == ProtoMek.LOC_NMISS)) {
            report = new Report(6035);
            report.subject = entity.getId();
            report.indent(2);
            if (proto.isGlider()) {
                report.messageId = 6036;
                proto.setWingHits(proto.getWingHits() + 1);
            }
            reportVec.add(report);
            return reportVec;
        }


        // Allocate the damage
        // Use different damageX methods to deal damage here
        // Don't pass reportVec back and forth, that's wasteful.
        if (entity instanceof ProtoMek teCast) {
            damageProtoMek(reportVec,
                  teCast,
                  hit,
                  damage,
                  ammoExplosion,
                  damageType,
                  areaSatArty,
                  throughFront,
                  underWater,
                  nukeS2S,
                  mods);
        } else if (entity instanceof Mek teCast) {
            damageMek(reportVec,
                  teCast,
                  hit,
                  damage,
                  ammoExplosion,
                  damageType,
                  areaSatArty,
                  throughFront,
                  underWater,
                  nukeS2S,
                  mods);
        } else if (entity instanceof Aero teCast) {
            damageAeroSpace(reportVec,
                  teCast,
                  hit,
                  damage,
                  ammoExplosion,
                  damageType,
                  areaSatArty,
                  throughFront,
                  underWater,
                  nukeS2S,
                  mods);
        } else if (entity instanceof Tank teCast) {
            damageTank(reportVec,
                  teCast,
                  hit,
                  damage,
                  ammoExplosion,
                  damageType,
                  areaSatArty,
                  throughFront,
                  underWater,
                  nukeS2S,
                  mods);
        } else if (entity instanceof BattleArmor teCast) {
            damageBA(reportVec,
                  teCast,
                  hit,
                  damage,
                  ammoExplosion,
                  damageType,
                  areaSatArty,
                  throughFront,
                  underWater,
                  nukeS2S,
                  mods);
        } else if (entity instanceof Infantry teCast && teCast.isConventionalInfantry()) {
            damageInfantry(reportVec,
                  teCast,
                  hit,
                  damage,
                  ammoExplosion,
                  damageType,
                  areaSatArty,
                  throughFront,
                  underWater,
                  nukeS2S,
                  mods);
        } else {
            logger.error(new UnknownEntityTypeException(entity.toString()));
        }

        boolean tookInternalDamage = mods.tookInternalDamage;

        // Meks using EI implants take pilot damage each time a hit
        // inflicts IS damage
        if (tookInternalDamage &&
                  ((entity instanceof Mek) || (entity instanceof ProtoMek)) &&
                  entity.hasActiveEiCockpit()) {
            Report.addNewline(reportVec);
            Roll diceRoll = Compute.rollD6(2);
            report = new Report(5075);
            report.subject = entity.getId();
            report.addDesc(entity);
            report.add(7);
            report.add(diceRoll);
            report.choose(diceRoll.getIntValue() >= 7);
            report.indent(2);
            reportVec.add(report);
            if (diceRoll.getIntValue() < 7) {
                reportVec.addAll(manager.damageCrew(entity, 1));
            }
        }

        // if using VDNI (but not buffered), check for damage on an internal hit
        if (tookInternalDamage &&
                  entity.hasAbility(OptionsConstants.MD_VDNI) &&
                  !entity.hasAbility(OptionsConstants.MD_BVDNI) &&
                  !entity.hasAbility(OptionsConstants.MD_PAIN_SHUNT)) {
            Report.addNewline(reportVec);
            Roll diceRoll = Compute.rollD6(2);
            report = new Report(3580);
            report.subject = entity.getId();
            report.addDesc(entity);
            report.add(7);
            report.add(diceRoll);
            report.choose(diceRoll.getIntValue() >= 8);
            report.indent(2);
            reportVec.add(report);

            if (diceRoll.getIntValue() >= 8) {
                reportVec.addAll(manager.damageCrew(entity, 1));
            }
        }

        // TacOps p.78 Ammo booms can hurt other units in the same and adjacent hexes, But this does not apply to
        // CASE'd units, and it only applies if the ammo explosion destroyed the unit.
        // For `Meks we care whether there was CASE specifically in the location that went boom...
        // ...but vehicles and ASFs just have one CASE item for the whole unit, so we need to look at whether there's
        // CASE anywhere at all.
        if (ammoExplosion &&
                  game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_AMMUNITION) &&
                  !(entity.locationHasCase(hit.getLocation()) || entity.hasCASEII(hit.getLocation())) &&
                  !(((entity instanceof Tank) || (entity instanceof Aero)) && entity.hasCase()) &&
                  (entity.isDestroyed() || entity.isDoomed()) &&
                  (damage > 0) &&
                  ((damage / 10) > 0)) {
            Report.addNewline(reportVec);
            report = new Report(5068, Report.PUBLIC);
            report.subject = entity.getId();
            report.addDesc(entity);
            report.indent(2);
            reportVec.add(report);
            Report.addNewline(reportVec);
            report = new Report(5400, Report.PUBLIC);
            report.subject = entity.getId();
            report.indent(2);
            reportVec.add(report);
            int[] damages = { (int) Math.floor(damage / 10.0), (int) Math.floor(damage / 20.0) };
            manager.doExplosion(damages, false, entity.getPosition(), 0, true,
                  reportVec,
                  null,
                  5,
                  entity.getId(),
                  false,
                  false);
            Report.addNewline(reportVec);
            report = new Report(5410, Report.PUBLIC);
            report.subject = entity.getId();
            report.indent(2);
            reportVec.add(report);
        }

        // This flag indicates the hit was directly to IS
        if (mods.wasDamageIS) {
            Report.addNewline(reportVec);
        }
        return reportVec;
    }


    public void damageProtoMek(Vector<Report> reportVec, ProtoMek proto, HitData hit, int damage, boolean ammoExplosion,
          DamageType damageType, boolean areaSatArty, boolean throughFront, boolean underWater, boolean nukeS2S,
          ModsInfo mods) {
        int entityId = proto.getId();
        Report report;
        boolean autoEject = false;
        HitData nextHit = null;
        boolean damageIS = mods.damageIS;

        while (damage > 0) {
            // Apply damage to armor
            damage = applyEntityArmorDamage(proto, hit, damage, ammoExplosion, damageIS, areaSatArty, reportVec, mods);

            // is there damage remaining?
            if (damage > 0) {

                // is there internal structure in the location hit?
                if (proto.getInternal(hit) > 0) {

                    // Now we need to consider alternate structure types!
                    if ((proto.getInternal(hit) > damage)) {
                        // internal structure absorbs all damage
                        proto.setInternal(proto.getInternal(hit) - damage, hit);
                        // Triggers a critical hit on Vehicles and Meks.
                        mods.crits++;
                        mods.tookInternalDamage = true;
                        // Alternate structures don't affect our damage total
                        // for later PSR purposes, so use the previously stored
                        // value here as necessary.
                        proto.damageThisPhase += damage;
                        damage = 0;
                        report = new Report(6100);
                        report.subject = entityId;
                        report.indent(3);
                        report.add(proto.getInternal(hit));
                        reportVec.addElement(report);
                    } else {
                        // Triggers a critical hit on Vehicles and Meks.
                        mods.crits++;
                        // damage transfers, maybe
                        int absorbed = Math.max(proto.getInternal(hit), 0);

                        // Handle ProtoMek pilot damage
                        // due to location destruction
                        int hits = ProtoMek.POSSIBLE_PILOT_DAMAGE[hit.getLocation()] -
                                         proto.getPilotDamageTaken(hit.getLocation());
                        if (hits > 0) {
                            reportVec.addAll(manager.damageCrew(proto, hits));
                            proto.setPilotDamageTaken(hit.getLocation(),
                                  ProtoMek.POSSIBLE_PILOT_DAMAGE[hit.getLocation()]);
                        }

                        // Platoon, Trooper, or Section Destroyed message
                        report = new Report(1210);
                        report.subject = entityId;
                        report.messageId = 6115;
                        report.indent(3);
                        reportVec.addElement(report);

                        // Mark off the internal structure here, but *don't* destroy the location just yet -- there
                        // are checks still to run!
                        proto.setInternal(0, hit);
                        proto.damageThisPhase += absorbed;
                        damage -= absorbed;
                    }
                }

                if (proto.getInternal(hit) <= 0) {
                    // the internal structure is gone, what are the transfer potentials?
                    nextHit = proto.getTransferLocation(hit);
                    if (nextHit.getLocation() == Entity.LOC_DESTROYED) {

                        // No engine explosions for ProtoMeks
                        // Entity destroyed.
                        // Ammo explosions are neither survivable nor salvageable.
                        // Only ammo explosions in the CT are devastating.
                        reportVec.addAll(manager.destroyEntity(proto,
                              "damage",
                              !ammoExplosion,
                              !(ammoExplosion || areaSatArty)));
                        // If the head is destroyed, kill the crew.

                        if ((hit.getLocation() == Mek.LOC_HEAD) ||
                                  ((hit.getLocation() == Mek.LOC_CT) &&
                                         ((ammoExplosion && !autoEject) || areaSatArty))) {
                            proto.getCrew().setDoomed(true);
                        }
                        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_AUTO_ABANDON_UNIT)) {
                            reportVec.addAll(manager.abandonEntity(proto));
                        }

                        // nowhere for further damage to go
                        damage = 0;
                    } else if (nextHit.getLocation() == Entity.LOC_NONE) {
                        // The rest of the damage is wasted.
                        damage = 0;
                    } else if (ammoExplosion && proto.locationHasCase(hit.getLocation())) {
                        // Remaining damage prevented by CASE
                        report = new Report(6125);
                        report.subject = entityId;
                        report.add(damage);
                        report.indent(3);
                        reportVec.addElement(report);

                        // The target takes no more damage from the explosion.
                        damage = 0;
                    } else if (damage > 0) {
                        // remaining damage transfers
                        report = new Report(6130);
                        report.subject = entityId;
                        report.indent(2);
                        report.add(damage);
                        report.add(proto.getLocationAbbr(nextHit));
                        reportVec.addElement(report);

                        // If there are split weapons in this location, mark it as a hit, even if it took no criticals.
                        for (WeaponMounted m : proto.getWeaponList()) {
                            if (m.isSplit()) {
                                if ((m.getLocation() == hit.getLocation()) ||
                                          (m.getLocation() == nextHit.getLocation())) {
                                    proto.setWeaponHit(m);
                                }
                            }
                        }
                        // if this is damage from a nail/rivet gun, and we transfer to a location that has armor, and
                        // BAR >=5, no damage
                        if ((damageType == DamageType.NAIL_RIVET) &&
                                  (proto.getArmor(nextHit.getLocation()) > 0) &&
                                  (proto.getBARRating(nextHit.getLocation()) >= 5)) {
                            damage = 0;
                            report = new Report(6065);
                            report.subject = entityId;
                            report.indent(2);
                            reportVec.add(report);
                        }
                    }
                }
            } else if (hit.getSpecCrit()) {
                // ok, we dealt damage but didn't go on to internal
                // we get a chance of a crit, using Armor Piercing.
                // but only if we don't have hardened, Ferro-Lamellor, or reactive armor
                if (!(mods.hardenedArmor || mods.ferroLamellorArmor || mods.reactiveArmor)) {
                    mods.specCrits = mods.specCrits + 1;
                }
            }

            // check for breaching
            reportVec.addAll(manager.breachCheck(proto, hit.getLocation(), null, underWater));

            // Deal special effect damage and crits
            dealSpecialCritEffects(proto, reportVec, hit, mods, underWater, damageType);

            // If the location has run out of internal structure, finally actually
            // destroy it here.
            //
            // *EXCEPTION:* Aero units have 0 internal structures in every location by default and are handled
            // elsewhere, so they get a bye.
            if ((proto.getInternal(hit) <= 0)) {
                proto.destroyLocation(hit.getLocation());
            }

            // If damage remains, loop to the next location; if not, be sure to stop here because we may need to refer
            // back to the last *damaged* location again later. (This is safe because at damage <= 0 the loop
            // terminates anyway.)
            if (damage > 0) {
                hit = nextHit;
                updateArmorTypeMap(mods, proto, hit);
            }
            if (damageIS) {
                mods.wasDamageIS = true;
                damageIS = false;
            }
        }

    }

    public void damageMek(Vector<Report> reportVec, Mek mek, HitData hit, int damage, boolean ammoExplosion,
          DamageType damageType, boolean areaSatArty, boolean throughFront, boolean underWater, boolean nukeS2S,
          ModsInfo mods) {
        // This is good for shields if a shield absorbs the hit it shouldn't affect the pilot. TC SRM's that hit the
        // head do external and internal damage, but it's one hit and shouldn't cause 2 hits to the pilot.
        mods.isHeadHit = ((mek.getCockpitType() != Mek.COCKPIT_TORSO_MOUNTED) &&
                                (hit.getLocation() == Mek.LOC_HEAD) &&
                                ((hit.getEffect() & HitData.EFFECT_NO_CRITICALS) != HitData.EFFECT_NO_CRITICALS));
        int entityId = mek.getId();
        Entity attacker = game.getEntity(hit.getAttackerId());
        Report report;
        boolean autoEject = false;
        boolean damageIS = mods.damageIS;

        if (ammoExplosion) {
            if (mek.isAutoEject() &&
                      (!game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) ||
                             (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) &&
                                    mek.isCondEjectAmmo()))) {
                autoEject = true;
                reportVec.addAll(manager.ejectEntity(mek, true));
            }
        }

        HitData nextHit = null;

        damage = manageDamageTypeReports(mek, reportVec, damage, damageType, hit, false, mods);


        // Allocate the damage
        while (damage > 0) {
            
            // damage some cargo if we're taking damage
            // maybe move past "exterior passenger" check
            if (!ammoExplosion) {
                int damageLeftToCargo = damage;

                for (ICarryable cargo : mek.getDistinctCarriedObjects()) {
                    if (cargo.isInvulnerable()) {
                        continue;
                    }

                    double tonnage = cargo.getTonnage();
                    boolean cargoDestroyed = cargo.damage(damageLeftToCargo);
                    damageLeftToCargo -= (int) Math.ceil(tonnage);

                    // if we have destroyed the cargo, remove it, add a report
                    // and move on to the next piece of cargo
                    if (cargoDestroyed) {
                        mek.dropGroundObject(cargo, false);

                        report = new Report(6721);
                        report.subject = entityId;
                        report.indent(2);
                        report.add(cargo.generalName());
                        reportVec.addElement(report);
                        // we have not destroyed the cargo means there is no damage left to report and stop destroying
                        // cargo
                    } else {
                        report = new Report(6720);
                        report.subject = entityId;
                        report.indent(2);
                        report.add(cargo.generalName());
                        report.add(Double.toString(cargo.getTonnage()));
                        break;
                    }
                }
            }

            // Report this either way
            report = new Report(6065);
            report.subject = entityId;
            report.indent(2);
            report.addDesc(mek);
            report.add(damage);
            if (damageIS) {
                report.messageId = 6070;
            }
            report.add(mek.getLocationAbbr(hit));
            reportVec.addElement(report);

            if (ammoExplosion) {
                if (mek instanceof LandAirMek lam) {
                    // LAMs eject if the CT-destroyed switch is on
                    if (lam.isAutoEject() &&
                              (!game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) ||
                                     (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) &&
                                            lam.isCondEjectCTDest()))) {
                        reportVec.addAll(manager.ejectEntity(mek, true, false));
                    }
                }
            }

            // was the section destroyed earlier this phase?
            if (mek.getInternal(hit) == IArmorState.ARMOR_DOOMED) {
                // cannot transfer a through armor crit if so
                mods.crits = 0;
            }

            // here goes the fun :)
            // Shields take damage first, then cowls, then armor where Shield does not protect from ammo explosions
            // or falls.
            if (!ammoExplosion &&
                      !hit.isFallDamage() &&
                      !damageIS &&
                      mek.hasShield() &&
                      ((hit.getEffect() & HitData.EFFECT_NO_CRITICALS) != HitData.EFFECT_NO_CRITICALS)) {
                int damageNew = mek.shieldAbsorptionDamage(damage, hit.getLocation(), hit.isRear());
                // if a shield absorbed the damage, then let's tell the world about it.
                if (damageNew != damage) {
                    int absorb = damage - damageNew;
                    mek.damageThisPhase += absorb;
                    damage = damageNew;

                    report = new Report(3530);
                    report.subject = entityId;
                    report.indent(3);
                    report.add(absorb);
                    reportVec.addElement(report);

                    if (damage <= 0) {
                        mods.crits = 0;
                        mods.specCrits = 0;
                        mods.isHeadHit = false;
                    }
                }
            }

            // Armored Cowl may absorb some damage from a hit
            if (mek.hasCowl() &&
                      (hit.getLocation() == Mek.LOC_HEAD) &&
                      ((mek.getPosition() == null) ||
                             (attacker == null) ||
                             !mek.getPosition().isOnHexRow(mek.getSecondaryFacing(), attacker.getPosition()))) {
                int excessDamage = mek.damageCowl(damage);
                int blockedByCowl = damage - excessDamage;
                report = new Report(3520).subject(entityId).indent(3).add(blockedByCowl);
                reportVec.addElement(report);
                mek.damageThisPhase += blockedByCowl;
                damage = excessDamage;
            }

            damage = applyModularArmor(mek, hit, damage, ammoExplosion, damageIS, reportVec);

            // Destroy searchlights on 7+ (torso hits on meks)
            if (mek.hasSearchlight()) {
                boolean spotlightHittable = true;
                int loc = hit.getLocation();
                if ((loc != Mek.LOC_CT) && (loc != Mek.LOC_LT) && (loc != Mek.LOC_RT)) {
                    spotlightHittable = false;
                }

                if (spotlightHittable) {
                    Roll diceRoll = Compute.rollD6(2);
                    report = new Report(6072);
                    report.indent(2);
                    report.subject = entityId;
                    report.add("7+");
                    report.add("Searchlight");
                    report.add(diceRoll);
                    reportVec.addElement(report);

                    if (diceRoll.getIntValue() >= 7) {
                        report = new Report(6071);
                        report.subject = entityId;
                        report.indent(2);
                        report.add("Searchlight");
                        reportVec.addElement(report);
                        mek.destroyOneSearchlight();
                    }
                }
            }

            if (!damageIS) {
                // Does an exterior passenger absorb some damage?
                damage = handleExternalPassengerDamage(mek, hit, damage, ammoExplosion, damageType, reportVec);
                if (damage == 0) {
                    // Return
                    return;
                }
                // is this a mek dumping ammo being hit in the rear torso?
                if (List.of(Mek.LOC_CT, Mek.LOC_RT, Mek.LOC_LT).contains(hit.getLocation())) {
                    for (Mounted<?> mAmmo : mek.getAmmo()) {
                        if (mAmmo.isDumping() && !mAmmo.isDestroyed() && !mAmmo.isHit()) {
                            // doh. explode it
                            reportVec.addAll(manager.explodeEquipment(mek, mAmmo.getLocation(), mAmmo));
                            mAmmo.setHit(true);
                        }
                    }
                }
            }

            // Apply damage to armor
            damage = applyEntityArmorDamage(mek, hit, damage, ammoExplosion, damageIS, areaSatArty, reportVec, mods);

            // Apply CASE II first
            damage = applyCASEIIDamageReduction(mek, hit, damage, ammoExplosion, reportVec);

            // if damage has not all been absorbed, continue dealing with damage internally
            if (damage > 0) {
                // is there internal structure in the location hit?
                if (mek.getInternal(hit) > 0) {

                    // Now we need to consider alternate structure types!
                    int tmpDamageHold = -1;
                    if (mek.hasCompositeStructure()) {
                        tmpDamageHold = damage;
                        damage *= 2;
                        report = new Report(6091);
                        report.subject = entityId;
                        report.indent(3);
                        reportVec.add(report);
                    }
                    if (mek.hasReinforcedStructure()) {
                        tmpDamageHold = damage;
                        damage /= 2;
                        damage += tmpDamageHold % 2;
                        report = new Report(6092);
                        report.subject = entityId;
                        report.indent(3);
                        reportVec.add(report);
                    }
                    if ((mek.getInternal(hit) > damage) && (damage > 0)) {
                        // internal structure absorbs all damage
                        mek.setInternal(mek.getInternal(hit) - damage, hit);
                        // Triggers a critical hit on Vehicles and Meks.
                        mods.crits++;
                        mods.tookInternalDamage = true;
                        // Alternate structures don't affect our damage total
                        // for later PSR purposes, so use the previously stored
                        // value here as necessary.
                        mek.damageThisPhase += (tmpDamageHold > -1) ? tmpDamageHold : damage;
                        damage = 0;
                        report = new Report(6100);
                        report.subject = entityId;
                        report.indent(3);
                        // Infantry platoons have men not "Internals".
                        report.add(mek.getInternal(hit));
                        reportVec.addElement(report);
                    } else if (damage > 0) {
                        // Triggers a critical hit on Vehicles and Meks.
                        mods.crits++;
                        // damage transfers, maybe
                        int absorbed = Math.max(mek.getInternal(hit), 0);

                        // Platoon, Trooper, or Section Destroyed message
                        report = new Report(1210);
                        report.subject = entityId;
                        report.messageId = 6115;
                        report.indent(3);
                        reportVec.addElement(report);

                        // If a side torso got destroyed, and the corresponding arm is not yet destroyed, add it as a
                        // club to that hex (p.35 BMRr)
                        if ((((hit.getLocation() == Mek.LOC_RT) && (mek.getInternal(Mek.LOC_RARM) > 0)) ||
                                   ((hit.getLocation() == Mek.LOC_LT) && (mek.getInternal(Mek.LOC_LARM) > 0)))) {
                            int blownOffLocation;
                            if (hit.getLocation() == Mek.LOC_RT) {
                                blownOffLocation = Mek.LOC_RARM;
                            } else {
                                blownOffLocation = Mek.LOC_LARM;
                            }
                            mek.destroyLocation(blownOffLocation, true);
                            report = new Report(6120);
                            report.subject = entityId;
                            report.add(mek.getLocationName(blownOffLocation));
                            reportVec.addElement(report);
                            Hex h = game.getBoard().getHex(mek.getPosition());
                            if (null != h) {
                                if (mek instanceof BipedMek) {
                                    if (!h.containsTerrain(Terrains.ARMS)) {
                                        h.addTerrain(new Terrain(Terrains.ARMS, 1));
                                    } else {
                                        h.addTerrain(new Terrain(Terrains.ARMS, h.terrainLevel(Terrains.ARMS) + 1));
                                    }
                                } else if (!h.containsTerrain(Terrains.LEGS)) {
                                    h.addTerrain(new Terrain(Terrains.LEGS, 1));
                                } else {
                                    h.addTerrain(new Terrain(Terrains.LEGS, h.terrainLevel(Terrains.LEGS) + 1));
                                }
                                manager.sendChangedHex(mek.getPosition());
                            }
                        }

                        // Troopers riding on a location
                        // all die when the location is destroyed.
                        Entity passenger = mek.getExteriorUnitAt(hit.getLocation(), hit.isRear());
                        if ((null != passenger) && !passenger.isDoomed()) {
                            HitData passHit = passenger.getTrooperAtLocation(hit, mek);
                            // ensures a kill
                            passHit.setEffect(HitData.EFFECT_CRITICAL);
                            if (passenger.getInternal(passHit) > 0) {
                                reportVec.addAll(damageEntity(new DamageInfo(passenger, passHit, damage)));
                            }
                            passHit = new HitData(hit.getLocation(), !hit.isRear());
                            passHit = passenger.getTrooperAtLocation(passHit, mek);
                            // ensures a kill
                            passHit.setEffect(HitData.EFFECT_CRITICAL);
                            if (passenger.getInternal(passHit) > 0) {
                                reportVec.addAll(damageEntity(new DamageInfo(passenger, passHit, damage)));
                            }
                        }

                        // Mark off the internal structure here, but *don't*
                        // destroy the location just yet -- there are checks
                        // still to run!
                        mek.setInternal(0, hit);
                        mek.damageThisPhase += absorbed;
                        damage -= absorbed;

                        // Now we need to consider alternate structure types!
                        if (tmpDamageHold > 0) {
                            if (mek.hasCompositeStructure()) {
                                // If there's a remainder, we can actually
                                // ignore it.
                                damage /= 2;
                            } else if (mek.hasReinforcedStructure()) {
                                damage *= 2;
                                damage -= tmpDamageHold % 2;
                            }
                        }
                    }
                }

                if (mek.getInternal(hit) <= 0) {
                    // the internal structure is gone, what are the transfer potentials?
                    nextHit = mek.getTransferLocation(hit);
                    if (nextHit.getLocation() == Entity.LOC_DESTROYED) {
                        // Start with the number of engine crits in this location, if any...
                        mek.engineHitsThisPhase += mek.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM,
                              Mek.SYSTEM_ENGINE,
                              hit.getLocation());
                        // ...then deduct the ones destroyed previously or critically hit this round already. That
                        // leaves the ones actually destroyed with the location.
                        mek.engineHitsThisPhase -= mek.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                              Mek.SYSTEM_ENGINE,
                              hit.getLocation());

                        boolean engineExploded = manager.checkEngineExplosion(mek, reportVec, mek.engineHitsThisPhase);

                        if (!engineExploded) {
                            // Entity destroyed. Ammo explosions are
                            // neither survivable nor salvageable.
                            // Only ammo explosions in the CT are devastating.
                            reportVec.addAll(manager.destroyEntity(mek,
                                  "damage",
                                  !ammoExplosion,
                                  !((ammoExplosion || areaSatArty) && (hit.getLocation() == Mek.LOC_CT))));
                            // If the head is destroyed, kill the crew.
                            if ((hit.getLocation() == Mek.LOC_HEAD) &&
                                      !mek.getCrew().isDead() &&
                                      !mek.getCrew().isDoomed() &&
                                      game.getOptions()
                                            .booleanOption(OptionsConstants.ADVANCED_TACOPS_SKIN_OF_THE_TEETH_EJECTION)) {
                                if (mek.isAutoEject() &&
                                          (!game.getOptions()
                                                  .booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) ||
                                                 (game.getOptions()
                                                        .booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) &&
                                                        mek.isCondEjectHeadshot()))) {
                                    autoEject = true;
                                    reportVec.addAll(manager.ejectEntity(mek, true, true));
                                }
                            }

                            if ((hit.getLocation() == Mek.LOC_CT) &&
                                      !mek.getCrew().isDead() &&
                                      !mek.getCrew().isDoomed()) {
                                if (mek.isAutoEject() &&
                                          game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) &&
                                          mek.isCondEjectCTDest()) {
                                    if (mek.getCrew().getHits() < 5) {
                                        Report.addNewline(reportVec);
                                        mek.setDoomed(false);
                                        mek.setDoomed(true);
                                    }
                                    autoEject = true;
                                    reportVec.addAll(manager.ejectEntity(mek, true));
                                }
                            }

                            if ((hit.getLocation() == Mek.LOC_HEAD) ||
                                      ((hit.getLocation() == Mek.LOC_CT) &&
                                             ((ammoExplosion && !autoEject) || areaSatArty))) {
                                mek.getCrew().setDoomed(true);
                            }
                            if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_AUTO_ABANDON_UNIT)) {
                                reportVec.addAll(manager.abandonEntity(mek));
                            }
                        }

                        // nowhere for further damage to go
                        damage = 0;
                    } else if (nextHit.getLocation() == Entity.LOC_NONE) {
                        // The rest of the damage is wasted.
                        damage = 0;
                    } else if (ammoExplosion && mek.locationHasCase(hit.getLocation())) {
                        // Remaining damage prevented by CASE
                        report = new Report(6125);
                        report.subject = entityId;
                        report.add(damage);
                        report.indent(3);
                        reportVec.addElement(report);

                        // The target takes no more damage from the explosion.
                        damage = 0;
                    } else if (damage > 0) {
                        // remaining damage transfers
                        report = new Report(6130);
                        report.subject = entityId;
                        report.indent(2);
                        report.add(damage);
                        report.add(mek.getLocationAbbr(nextHit));
                        reportVec.addElement(report);

                        // If there are split weapons in this location, mark it as a hit, even if it took no criticals.
                        for (WeaponMounted m : mek.getWeaponList()) {
                            if (m.isSplit()) {
                                if ((m.getLocation() == hit.getLocation()) ||
                                          (m.getLocation() == nextHit.getLocation())) {
                                    mek.setWeaponHit(m);
                                }
                            }
                        }
                        // if this is damage from a nail/rivet gun, and we transfer to a location that has armor, and
                        // BAR >=5, no damage
                        if ((damageType == DamageType.NAIL_RIVET) &&
                                  (mek.getArmor(nextHit.getLocation()) > 0) &&
                                  (mek.getBARRating(nextHit.getLocation()) >= 5)) {
                            damage = 0;
                            report = new Report(6065);
                            report.subject = entityId;
                            report.indent(2);
                            reportVec.add(report);
                        }
                    }
                }
            } else if (hit.getSpecCrit()) {
                // ok, we dealt damage but didn't go on to internal
                // we get a chance of a crit, using Armor Piercing.
                // but only if we don't have hardened, Ferro-Lamellor, or reactive armor
                if (!(mods.hardenedArmor || mods.ferroLamellorArmor || mods.reactiveArmor)) {
                    mods.specCrits = mods.specCrits + 1;
                }
            }

            // check for breaching
            reportVec.addAll(manager.breachCheck(mek, hit.getLocation(), null, underWater));

            // Deal special effect damage and crits
            dealSpecialCritEffects(mek, reportVec, hit, mods, underWater, damageType);

            if (mods.isHeadHit && !mek.hasAbility(OptionsConstants.MD_DERMAL_ARMOR)) {
                Report.addNewline(reportVec);
                reportVec.addAll(manager.damageCrew(mek, 1));
            }

            // If the location has run out of internal structure, finally
            // actually
            // destroy it here.
            if ((mek.getInternal(hit) <= 0)) {
                mek.destroyLocation(hit.getLocation());

                // Check for possible engine destruction here
                if (((hit.getLocation() == Mek.LOC_RT) || (hit.getLocation() == Mek.LOC_LT))) {

                    int numEngineHits = mek.getEngineHits();
                    boolean engineExploded = manager.checkEngineExplosion(mek, reportVec, numEngineHits);

                    int hitsToDestroy = 3;
                    if (mek.isSuperHeavy() &&
                              mek.hasEngine() &&
                              (mek.getEngine().getEngineType() == Engine.COMPACT_ENGINE)) {
                        hitsToDestroy = 2;
                    }

                    if (!engineExploded && (numEngineHits >= hitsToDestroy)) {
                        // the third engine hit
                        reportVec.addAll(manager.destroyEntity(mek, "engine destruction"));
                        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_AUTO_ABANDON_UNIT)) {
                            reportVec.addAll(manager.abandonEntity(mek));
                        }
                        mek.setSelfDestructing(false);
                        mek.setSelfDestructInitiated(false);
                    }

                    // Torso destruction in airborne LAM causes immediate crash.
                    if ((mek instanceof LandAirMek) && !mek.isDestroyed() && !mek.isDoomed()) {
                        report = new Report(9710);
                        report.subject = mek.getId();
                        report.addDesc(mek);
                        if (mek.isAirborneVTOLorWIGE()) {
                            reportVec.add(report);
                            manager.crashAirMek(mek,
                                  new PilotingRollData(mek.getId(), TargetRoll.AUTOMATIC_FAIL, "side torso destroyed"),
                                  reportVec);
                        } else if (mek.isAirborne() && mek.isAero()) {
                            reportVec.add(report);
                            reportVec.addAll(manager.processCrash(mek,
                                  ((IAero) mek).getCurrentVelocity(),
                                  mek.getPosition()));
                        }
                    }
                }
            }

            // If damage remains, loop to the next location; if not, be sure to stop here because we may need to
            // refer back to the last *damaged* location again later. (This is safe because at damage <= 0 the loop
            // terminates anyway.)
            if (damage > 0) {
                hit = nextHit;
                updateArmorTypeMap(mods, mek, hit);
            }
            if (damageIS) {
                mods.wasDamageIS = true;
                damageIS = false;
            }
        }
    }

    public void damageAeroSpace(Vector<Report> reportVec, Aero aero, HitData hit, int damage, boolean ammoExplosion,
          DamageType damageType, boolean areaSatArty, boolean throughFront, boolean underWater, boolean nukeS2S,
          ModsInfo mods) {
        int entityId = aero.getId();
        Report report;
        boolean damageIS = mods.damageIS;

        if (ammoExplosion) {
            if (aero.isAutoEject() &&
                      (!game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) ||
                             (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) &&
                                    aero.isCondEjectAmmo()))) {
                reportVec.addAll(manager.ejectEntity(aero, true, false));
            }
        }

        // booleans to indicate criticals for AT2
        boolean critSI = false;
        boolean critThresh = false;

        // save the relevant damage for damage thresholding
        int damageThisAttack = aero.damageThisPhase;

        // weapon groups only get the damage of one weapon
        if ((hit.getSingleAV() > -1) && !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            damage = hit.getSingleAV();
        }

        // is this capital-scale damage
        boolean isCapital = hit.isCapital();

        // check capital/standard damage
        if (isCapital &&
                  (!aero.isCapitalScale() ||
                         game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY))) {
            damage = 10 * damage;
        }
        if (!isCapital &&
                  aero.isCapitalScale() &&
                  !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            damage = (int) Math.round(damage / 10.0);
        }
        int damage_orig = damage;

        damage = manageDamageTypeReports(aero, reportVec, damage, damageType, hit, false, mods);

        if (ammoExplosion && aero.hasCase()) {
            // damage should be reduced by a factor of 2 for ammo explosions, according to p. 161, TW
            damage /= 2;
            report = new Report(9010);
            report.subject = entityId;
            report.add(damage);
            report.indent(3);
            reportVec.addElement(report);
        }

        damage = applyModularArmor(aero, hit, damage, ammoExplosion, damageIS, reportVec);

        // Allocate the damage
        // first check for ammo explosions on aeros separately, because it must be done before standard to
        // capital damage conversions
        if ((hit.getLocation() == Aero.LOC_AFT) && !damageIS) {
            for (Mounted<?> mAmmo : aero.getAmmo()) {
                if (mAmmo.isDumping() &&
                          !mAmmo.isDestroyed() &&
                          !mAmmo.isHit() &&
                          !(mAmmo.getType() instanceof BombType)) {
                    // doh. explode it
                    reportVec.addAll(manager.explodeEquipment(aero, mAmmo.getLocation(), mAmmo));
                    mAmmo.setHit(true);
                }
            }
        }

        // Report this either way
        if (!ammoExplosion) {
            report = new Report(6065);
            report.subject = entityId;
            report.indent(2);
            report.addDesc(aero);
            report.add(damage);
            if (damageIS) {
                report.messageId = 6070;
            }
            report.add(aero.getLocationAbbr(hit));
            reportVec.addElement(report);
        }

        // Capital fighters receive damage differently
        if (aero.isCapitalFighter()) {
            aero.setCurrentDamage(aero.getCurrentDamage() + damage);
            aero.setCapArmor(aero.getCapArmor() - damage);
            report = new Report(9065);
            report.subject = entityId;
            report.indent(2);
            report.newlines = 0;
            report.addDesc(aero);
            report.add(damage);
            reportVec.addElement(report);
            report = new Report(6085);
            report.subject = entityId;
            report.add(Math.max(aero.getCapArmor(), 0));
            reportVec.addElement(report);
            // check to see if this destroyed the entity
            if (aero.getCapArmor() <= 0) {
                // Lets auto-eject if we can!
                // Aeros eject if the SI Destroyed switch is on
                if (aero.isAutoEject() &&
                          (!game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) ||
                                 (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) &&
                                        aero.isCondEjectSIDest()))) {
                    reportVec.addAll(manager.ejectEntity(aero, true, false));
                }
                reportVec.addAll(manager.destroyEntity(aero, "Structural Integrity Collapse"));
                aero.doDisbandDamage();
                aero.setCapArmor(0);
                if (hit.getAttackerId() != Entity.NONE) {
                    manager.creditKill(aero, game.getEntity(hit.getAttackerId()));
                }
            }
            // check for aero crits from natural 12 or threshold; LAMs take damage as meks
            manager.checkAeroCrits(reportVec, aero, hit, damage_orig, critThresh, critSI, ammoExplosion, nukeS2S);
            
            // Return early
            return;
        }

        damage = applyEntityArmorDamage(aero, hit, damage, ammoExplosion, damageIS, areaSatArty, reportVec, mods);

        if (damage > 0) {
            // If this is an Aero, then I need to apply internal damage
            // to the SI after halving it. Return from here to prevent
            // further processing

            // check for large craft ammo explosions here: damage vented through armor, excess
            // dissipating, much like Tank CASE.
            if (ammoExplosion && aero.isLargeCraft()) {
                aero.damageThisPhase += damage;
                report = new Report(6128);
                report.subject = entityId;
                report.indent(2);
                report.add(damage);
                int loc = hit.getLocation();
                //Roll for broadside weapons so fore/aft side armor facing takes the damage
                if (loc == Warship.LOC_LBS) {
                    int locRoll = Compute.d6();
                    if (locRoll < 4) {
                        loc = Jumpship.LOC_FLS;
                    } else {
                        loc = Jumpship.LOC_ALS;
                    }
                }
                if (loc == Warship.LOC_RBS) {
                    int locRoll = Compute.d6();
                    if (locRoll < 4) {
                        loc = Jumpship.LOC_FRS;
                    } else {
                        loc = Jumpship.LOC_ARS;
                    }
                }
                report.add(aero.getLocationAbbr(loc));
                reportVec.add(report);
                if (damage > aero.getArmor(loc)) {
                    aero.setArmor(IArmorState.ARMOR_DESTROYED, loc);
                    report = new Report(6090);
                } else {
                    aero.setArmor(aero.getArmor(loc) - damage, loc);
                    report = new Report(6085);
                    report.add(aero.getArmor(loc));
                }
                report.subject = entityId;
                report.indent(3);
                reportVec.add(report);
                damage = 0;
            }

            // check for overpenetration
            if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_OVER_PENETRATE)) {
                int opRoll = Compute.d6(1);
                if (((aero instanceof Jumpship) && !(aero instanceof Warship) && (opRoll > 3)) ||
                          ((aero instanceof Dropship) && (opRoll > 4)) ||
                          ((aero instanceof Warship) && (aero.getOSI() <= 30) && (opRoll > 5))) {
                    // over-penetration happened
                    report = new Report(9090);
                    report.subject = entityId;
                    report.newlines = 0;
                    reportVec.addElement(report);
                    int new_loc = aero.getOppositeLocation(hit.getLocation());
                    damage = Math.min(damage, aero.getArmor(new_loc));
                    // We don't want to deal negative damage
                    damage = Math.max(damage, 0);
                    report = new Report(6065);
                    report.subject = entityId;
                    report.indent(2);
                    report.newlines = 0;
                    report.addDesc(aero);
                    report.add(damage);
                    report.add(aero.getLocationAbbr(new_loc));
                    reportVec.addElement(report);
                    aero.setArmor(aero.getArmor(new_loc) - damage, new_loc);
                    if ((aero instanceof Warship) || (aero instanceof Dropship)) {
                        damage = 2;
                    } else {
                        damage = 0;
                    }
                }
            }

            // divide damage in half
            // do not divide by half if it is an ammo explosion
            // Minimum SI damage is now 1 (per errata: https://bg.battletech.com/forums/index.php?topic=81913.0 )
            if (!ammoExplosion &&
                      !nukeS2S &&
                      !game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
                damage = (int) Math.round(damage / 2.0);
                critSI = true;
            }

            // Now apply damage to the structural integrity
            aero.setSI(aero.getSI() - damage);
            aero.damageThisPhase += damage;
            // send the report
            report = new Report(1210);
            report.subject = entityId;
            report.newlines = 1;
            if (!ammoExplosion) {
                report.messageId = 9005;
            }
            //Only for fighters
            if (ammoExplosion && !aero.isLargeCraft()) {
                report.messageId = 9006;
            }
            report.add(damage);
            report.add(Math.max(aero.getSI(), 0));
            reportVec.addElement(report);
            // check to see if this would destroy the ASF
            if (aero.getSI() <= 0) {
                // Lets auto-eject if we can!
                if (aero.isAutoEject() &&
                          (!game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) ||
                                 (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) &&
                                        aero.isCondEjectSIDest()))) {
                    reportVec.addAll(manager.ejectEntity(aero, true, false));
                } else {
                    reportVec.addAll(manager.destroyEntity(aero,
                          "Structural Integrity Collapse",
                          damageType != DamageType.CRASH));
                }
                aero.setSI(0);
                if (hit.getAttackerId() != Entity.NONE) {
                    manager.creditKill(aero, game.getEntity(hit.getAttackerId()));
                }
            }
            // All damage should have been applied by now
            damage = 0;
        }

        // Damage _applied_ by this attack should be original damageThisPhase minus current value
        damageThisAttack = aero.damageThisPhase - damageThisAttack;

        // chance of critical hit if total suffered damage greater than a threshold
        if ((damageThisAttack > aero.getThresh(hit.getLocation()))) {
            critThresh = true;
            aero.setCritThresh(true);
        }

        manager.checkAeroCrits(reportVec, aero, hit, damageThisAttack, critThresh, critSI, ammoExplosion, nukeS2S);
    }

    public void damageTank(Vector<Report> reportVec, Tank tank, HitData hit, int damage, boolean ammoExplosion,
          DamageType damageType, boolean areaSatArty, boolean throughFront, boolean underWater, boolean nukeS2S,
          ModsInfo mods) {
        int entityId = tank.getId();
        boolean damageIS = mods.damageIS;
        Report report;

        HitData nextHit = null;

        damage = manageDamageTypeReports(tank, reportVec, damage, damageType, hit, false, mods);

        // adjust VTOL rotor damage
        if ((tank instanceof VTOL) &&
                  (hit.getLocation() == VTOL.LOC_ROTOR) &&
                  (hit.getGeneralDamageType() != HitData.DAMAGE_PHYSICAL) &&
                  !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_FULL_ROTOR_HITS)) {
            damage = (damage + 9) / 10;
        }

        // Allocate the damage
        while (damage > 0) {

            // Report this either way
            report = new Report(6065);
            report.subject = entityId;
            report.indent(2);
            report.addDesc(tank);
            report.add(damage);
            if (damageIS) {
                report.messageId = 6070;
            }
            report.add(tank.getLocationAbbr(hit));
            reportVec.addElement(report);

            damage = applyModularArmor(tank, hit, damage, ammoExplosion, damageIS, reportVec);

            // Destroy searchlights on 7+ (torso hits on meks)
            if (tank.hasSearchlight()) {
                boolean spotlightHittable = isSpotlightHittable(tank, hit);
                if (spotlightHittable) {
                    Roll diceRoll = Compute.rollD6(2);
                    report = new Report(6072);
                    report.indent(2);
                    report.subject = entityId;
                    report.add("7+");
                    report.add("Searchlight");
                    report.add(diceRoll);
                    reportVec.addElement(report);

                    if (diceRoll.getIntValue() >= 7) {
                        report = new Report(6071);
                        report.subject = entityId;
                        report.indent(2);
                        report.add("Searchlight");
                        reportVec.addElement(report);
                        tank.destroyOneSearchlight();
                    }
                }
            }

            if (!damageIS) {
                // Does an exterior passenger absorb some damage?
                damage = handleExternalPassengerDamage(tank, hit, damage, ammoExplosion, damageType, reportVec);
                if (damage == 0) {
                    // Return our description.
                    return;
                }
                // is this a mek/tank dumping ammo being hit in the rear torso?
                if (hit.getLocation() == (tank instanceof SuperHeavyTank ? SuperHeavyTank.LOC_REAR : Tank.LOC_REAR)) {
                    for (Mounted<?> mAmmo : tank.getAmmo()) {
                        if (mAmmo.isDumping() && !mAmmo.isDestroyed() && !mAmmo.isHit()) {
                            // doh. explode it
                            reportVec.addAll(manager.explodeEquipment(tank, mAmmo.getLocation(), mAmmo));
                            mAmmo.setHit(true);
                        }
                    }
                }
            }

            damage = applyEntityArmorDamage(tank, hit, damage, ammoExplosion, damageIS, areaSatArty, reportVec, mods);

            // For optional tank damage thresholds, the `overthresh` flag won't be set if the internal structure is
            // damaged, so set it here.
            if (((tank.getArmor(hit) < 1) || damageIS) &&
                      game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD) &&
                      !((tank instanceof VTOL) || (tank instanceof GunEmplacement))) {
                tank.setOverThresh(true);
            }

            // Apply CASE II first
            damage = applyCASEIIDamageReduction(tank, hit, damage, ammoExplosion, reportVec);

            // Apply Tank CASE here
            damage = applyTankCASEDamageReduction(tank, hit, damage, ammoExplosion, reportVec);

            // is there damage remaining?
            if (damage > 0) {

                // is there internal structure in the location hit?
                if (tank.getInternal(hit) > 0) {

                    // Now we need to consider alternate structure types!
                    if ((tank.getInternal(hit) > damage)) {
                        // internal structure absorbs all damage
                        tank.setInternal(tank.getInternal(hit) - damage, hit);
                        // Triggers a critical hit on Vehicles and Meks.
                        mods.crits++;
                        mods.tookInternalDamage = true;
                        // Alternate structures don't affect our damage total
                        // for later PSR purposes, so use the previously stored
                        // value here as necessary.
                        tank.damageThisPhase += damage;
                        damage = 0;
                        report = new Report(6100);
                        report.subject = entityId;
                        report.indent(3);
                        report.add(tank.getInternal(hit));
                        reportVec.addElement(report);
                    } else {
                        // Triggers a critical hit on Vehicles and Meks.
                        mods.crits++;
                        // damage transfers, maybe
                        int absorbed = Math.max(tank.getInternal(hit), 0);

                        // Platoon, Trooper, or Section Destroyed message
                        report = new Report(1210);
                        report.subject = entityId;
                        report.messageId = 6115;
                        report.indent(3);
                        reportVec.addElement(report);

                        // Troopers riding on a location
                        // all die when the location is destroyed.
                        Entity passenger = tank.getExteriorUnitAt(hit.getLocation(), hit.isRear());
                        if ((null != passenger) && !passenger.isDoomed()) {
                            HitData passHit = passenger.getTrooperAtLocation(hit, tank);
                            // ensures a kill
                            passHit.setEffect(HitData.EFFECT_CRITICAL);
                            if (passenger.getInternal(passHit) > 0) {
                                reportVec.addAll(damageEntity(new DamageInfo(passenger, passHit, damage)));
                            }
                            passHit = new HitData(hit.getLocation(), !hit.isRear());
                            passHit = passenger.getTrooperAtLocation(passHit, tank);
                            // ensures a kill
                            passHit.setEffect(HitData.EFFECT_CRITICAL);
                            if (passenger.getInternal(passHit) > 0) {
                                reportVec.addAll(damageEntity(new DamageInfo(passenger, passHit, damage)));
                            }
                        }

                        // Mark off the internal structure here, but *don't*
                        // destroy the location just yet -- there are checks
                        // still to run!
                        tank.setInternal(0, hit);
                        tank.damageThisPhase += absorbed;
                        damage -= absorbed;
                    }
                }

                if (tank.getInternal(hit) <= 0) {
                    // the internal structure is gone, what are the transfer potentials?
                    nextHit = tank.getTransferLocation(hit);
                    if (nextHit.getLocation() == Entity.LOC_DESTROYED) {

                        boolean engineExploded = manager.checkEngineExplosion(tank,
                              reportVec,
                              tank.engineHitsThisPhase);

                        if (!engineExploded) {
                            // Entity destroyed. Ammo explosions are
                            // neither survivable nor salvageable.
                            // Only ammo explosions in the CT are devastating.
                            reportVec.addAll(manager.destroyEntity(tank,
                                  "damage",
                                  !ammoExplosion,
                                  !((ammoExplosion || areaSatArty))));

                            if ((hit.getLocation() == Mek.LOC_HEAD) ||
                                      ((hit.getLocation() == Mek.LOC_CT) && (ammoExplosion || areaSatArty))) {
                                tank.getCrew().setDoomed(true);
                            }
                            if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_AUTO_ABANDON_UNIT)) {
                                reportVec.addAll(manager.abandonEntity(tank));
                            }
                        }

                        // nowhere for further damage to go
                        damage = 0;
                    } else if (nextHit.getLocation() == Entity.LOC_NONE) {
                        // The rest of the damage is wasted.
                        damage = 0;
                    } else if (ammoExplosion && tank.locationHasCase(hit.getLocation())) {
                        // Remaining damage prevented by CASE
                        report = new Report(6125);
                        report.subject = entityId;
                        report.add(damage);
                        report.indent(3);
                        reportVec.addElement(report);

                        // The target takes no more damage from the explosion.
                        damage = 0;
                    } else if (damage > 0) {
                        // remaining damage transfers
                        report = new Report(6130);
                        report.subject = entityId;
                        report.indent(2);
                        report.add(damage);
                        report.add(tank.getLocationAbbr(nextHit));
                        reportVec.addElement(report);

                        // If there are split weapons in this location, mark it as a hit, even if it took no criticals.
                        for (WeaponMounted m : tank.getWeaponList()) {
                            if (m.isSplit()) {
                                if ((m.getLocation() == hit.getLocation()) ||
                                          (m.getLocation() == nextHit.getLocation())) {
                                    tank.setWeaponHit(m);
                                }
                            }
                        }
                        // if this is damage from a nail/rivet gun, and we transfer to a location that has armor, and
                        // BAR >=5, no damage
                        if ((damageType == DamageType.NAIL_RIVET) &&
                                  (tank.getArmor(nextHit.getLocation()) > 0) &&
                                  (tank.getBARRating(nextHit.getLocation()) >= 5)) {
                            damage = 0;
                            report = new Report(6065);
                            report.subject = entityId;
                            report.indent(2);
                            reportVec.add(report);
                        }
                    }
                }
            } else if (hit.getSpecCrit()) {
                // ok, we dealt damage but didn't go on to internal
                // we get a chance of a crit, using Armor Piercing.
                // but only if we don't have hardened, Ferro-Lamellor, or reactive armor
                if (!(mods.hardenedArmor || mods.ferroLamellorArmor || mods.reactiveArmor)) {
                    mods.specCrits = mods.specCrits + 1;
                }
            }

            // check for breaching
            reportVec.addAll(manager.breachCheck(tank, hit.getLocation(), null, underWater));

            // Deal special effect damage and crits
            dealSpecialCritEffects(tank, reportVec, hit, mods, underWater, damageType);

            // If the location has run out of internal structure, finally actually destroy it here. *EXCEPTION:* Aero
            // units have 0 internal structure in every location by default and are handled elsewhere, so they get a
            // bye.
            if ((tank.getInternal(hit) <= 0)) {
                tank.destroyLocation(hit.getLocation());
            }

            // If damage remains, loop to the next location; if not, be sure to stop here because we may need to
            // refer back to the last *damaged* location again later. (This is safe because at damage <= 0 the loop
            // terminates anyway.)
            if (damage > 0) {
                hit = nextHit;
                updateArmorTypeMap(mods, tank, hit);
            }
            if (damageIS) {
                mods.wasDamageIS = true;
                damageIS = false;
            }
        }
    }

    private static boolean isSpotlightHittable(Tank tank, HitData hit) {
        boolean spotlightHittable = true;
        int loc = hit.getLocation();
        if (tank instanceof SuperHeavyTank) {
            if ((loc != Tank.LOC_FRONT) &&
                      (loc != SuperHeavyTank.LOC_FRONTRIGHT) &&
                      (loc != SuperHeavyTank.LOC_FRONTLEFT) &&
                      (loc != SuperHeavyTank.LOC_REARRIGHT) &&
                      (loc != SuperHeavyTank.LOC_REARLEFT)) {
                spotlightHittable = false;
            }
        } else if (tank instanceof LargeSupportTank) {
            if ((loc != Tank.LOC_FRONT) &&
                      (loc != LargeSupportTank.LOC_FRONTRIGHT) &&
                      (loc != LargeSupportTank.LOC_FRONTLEFT) &&
                      (loc != LargeSupportTank.LOC_REARRIGHT) &&
                      (loc != LargeSupportTank.LOC_REARLEFT)) {
                spotlightHittable = false;
            }
        } else {
            if ((loc != Tank.LOC_FRONT) && (loc != Tank.LOC_RIGHT) && (loc != Tank.LOC_LEFT)) {
                spotlightHittable = false;
            }
        }
        return spotlightHittable;
    }

    public void damageSquadronFighter(Vector<Report> reportVec, Entity entity, HitData hit, int damage,
          boolean ammoExplosion, DamageType damageType, boolean damageIS, boolean areaSatArty, boolean throughFront,
          boolean underWater, boolean nukeS2S) {
        List<Entity> fighters = entity.getActiveSubEntities();

        if (fighters.isEmpty()) {
            return;
        }
        Entity fighter = fighters.get(hit.getLocation());
        HitData new_hit = fighter.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
        new_hit.setBoxCars(hit.rolledBoxCars());
        new_hit.setGeneralDamageType(hit.getGeneralDamageType());
        new_hit.setCapital(hit.isCapital());
        new_hit.setCapMisCritMod(hit.getCapMisCritMod());
        new_hit.setSingleAV(hit.getSingleAV());
        new_hit.setAttackerId(hit.getAttackerId());
        reportVec.addAll(damageEntity(fighter,
              new_hit,
              damage,
              ammoExplosion,
              damageType,
              damageIS,
              areaSatArty,
              throughFront,
              underWater,
              nukeS2S,
              reportVec));
    }

    public void damageMultipleBAs(Vector<Report> reportVec, Entity entity, HitData hit, int damage,
          boolean ammoExplosion, DamageType damageType, boolean damageIS, boolean areaSatArty, boolean throughFront,
          boolean underWater, boolean nukeS2S, ModsInfo mods) {
        BattleArmor battleArmor = (BattleArmor) entity;
        Report report;
        report = new Report(6044);
        report.subject = entity.getId();
        report.indent(2);
        reportVec.add(report);
        for (int i = 0; i < (battleArmor).getTroopers(); i++) {
            hit.setLocation(BattleArmor.LOC_TROOPER_1 + i);
            if (battleArmor.getInternal(hit) > 0) {
                // damageBA writes to reportVec on its own.
                damageBA(reportVec,
                      battleArmor,
                      hit,
                      damage,
                      ammoExplosion,
                      damageType,
                      areaSatArty,
                      throughFront,
                      underWater,
                      nukeS2S,
                      mods);
            }
        }
    }

    public void damageBA(Vector<Report> reportVec, BattleArmor battleArmor, HitData hit, int damage,
          boolean ammoExplosion, DamageType damageType, boolean areaSatArty, boolean throughFront, boolean underWater,
          boolean nukeS2S, ModsInfo mods) {
        final boolean eiStatus = battleArmor.hasActiveEiCockpit();
        final boolean vacuum = game.getPlanetaryConditions().getAtmosphere().isLighterThan(Atmosphere.THIN);
        int entityId = battleArmor.getId();
        boolean damageIS = mods.damageIS;
        HitData nextHit = null;
        Report report;

        // check for critical hit/miss vs. a BA
        // TODO: discover why BA reports are causing OOM errors.
        if (mods.crits > 0) {
            // possible critical miss if the rerolled location isn't alive
            if ((hit.getLocation() >= battleArmor.locations()) || (battleArmor.getInternal(hit.getLocation()) <= 0)) {
                report = new Report(6037);
                report.add(hit.getLocation());
                report.subject = entityId;
                report.indent(2);
                reportVec.addElement(report);
                return;
            }
            // otherwise critical hit
            report = new Report(6225);
            report.add(battleArmor.getLocationAbbr(hit));
            report.subject = entityId;
            report.indent(2);
            reportVec.addElement(report);

            mods.crits = 0;

            damage = Math.max(battleArmor.getInternal(hit.getLocation()) + battleArmor.getArmor(hit.getLocation()),
                  damage);
        }

        // Is the squad in vacuum?
        if (!battleArmor.isDestroyed() && !battleArmor.isDoomed() && vacuum) {
            // PBI. Double damage.
            damage *= 2;
            report = new Report(6041);
            report.subject = entityId;
            report.indent(2);
            reportVec.addElement(report);
        }

        damage = manageDamageTypeReports(battleArmor, reportVec, damage, damageType, hit, false, mods);

        // BA using EI implants receive +1 damage from attacks
        damage += (eiStatus) ? 1 : 0;

        // Allocate the damage
        while (damage > 0) {

            // was the section destroyed earlier this phase?
            if (battleArmor.getInternal(hit) == IArmorState.ARMOR_DOOMED) {
                // cannot transfer a through armor crit if so
                mods.crits = 0;
            }
            if (!ammoExplosion && (battleArmor.getArmor(hit) > 0) && !damageIS) {
                damage = applyEntityArmorDamage(battleArmor,
                      hit,
                      damage,
                      false,
                      damageIS,
                      areaSatArty,
                      reportVec,
                      mods);
            }

            if (damage > 0) {

                // Report this either way
                report = new Report(6065);
                report.subject = entityId;
                report.indent(2);
                report.addDesc(battleArmor);
                report.add(damage);
                if (damageIS) {
                    report.messageId = 6070;
                }
                report.add(battleArmor.getLocationAbbr(hit));
                reportVec.addElement(report);

                // is there internal structure in the location hit?
                if (battleArmor.getInternal(hit) > 0) {
                    // Now we need to consider alternate structure types!
                    if ((battleArmor.getInternal(hit) > damage)) {
                        // internal structure absorbs all damage
                        battleArmor.setInternal(battleArmor.getInternal(hit) - damage, hit);
                        mods.tookInternalDamage = true;
                        battleArmor.damageThisPhase = damage;
                        damage = 0;
                        report = new Report(6100);
                        report.subject = entityId;
                        report.indent(3);
                        report.add(battleArmor.getInternal(hit));
                        reportVec.addElement(report);
                    } else {
                        // damage transfers, maybe
                        int absorbed = Math.max(battleArmor.getInternal(hit), 0);

                        // Platoon, Trooper, or Section Destroyed message
                        report = new Report(1210);
                        report.subject = entityId;
                        report.messageId = 6110;
                        report.indent(3);
                        reportVec.addElement(report);

                        // BA inferno explosions
                        int infernos = 0;
                        for (Mounted<?> m : battleArmor.getEquipment()) {
                            if (m.getType() instanceof AmmoType at) {
                                if (((at.getAmmoType() == AmmoType.AmmoTypeEnum.SRM) || (at.getAmmoType() == AmmoType.AmmoTypeEnum.MML)) &&
                                          (at.getMunitionType().contains(Munitions.M_INFERNO))) {
                                    infernos += at.getRackSize() * m.getHittableShotsLeft();
                                }
                            } else if (m.getType().hasFlag(MiscType.F_FIRE_RESISTANT)) {
                                // immune to inferno explosion
                                infernos = 0;
                                break;
                            }
                        }
                        if (infernos > 0) {
                            Roll diceRoll = Compute.rollD6(2);
                            report = new Report(6680);
                            report.add(diceRoll);
                            reportVec.add(report);

                            if (diceRoll.getIntValue() >= 8) {
                                Coords c = battleArmor.getPosition();
                                if (c == null) {
                                    Entity transport = game.getEntity(battleArmor.getTransportId());
                                    if (transport != null) {
                                        c = transport.getPosition();
                                    }
                                    manager.getMainPhaseReport()
                                          .addAll(manager.deliverInfernoMissiles(battleArmor, battleArmor, infernos));
                                }
                                if (c != null) {
                                    manager.getMainPhaseReport()
                                          .addAll(manager.deliverInfernoMissiles(battleArmor,
                                                new HexTarget(c, Targetable.TYPE_HEX_ARTILLERY),
                                                infernos));
                                }
                            }
                        }

                        // Mark off the internal structure here, but *don't*
                        // destroy the location just yet -- there are checks
                        // still to run!
                        battleArmor.setInternal(0, hit);
                        battleArmor.damageThisPhase += absorbed;
                        damage -= absorbed;
                    }
                }
                if (battleArmor.getInternal(hit) <= 0) {
                    // the internal structure is gone, what are the transfer
                    // potentials?
                    nextHit = battleArmor.getTransferLocation(hit);
                    if (nextHit.getLocation() == Entity.LOC_DESTROYED) {

                        // Entity destroyed.
                        reportVec.addAll(
                              manager.destroyEntity(battleArmor, "damage", true, false)
                        );

                        // nowhere for further damage to go
                        damage = 0;
                    } else if (nextHit.getLocation() == Entity.LOC_NONE) {
                        // The rest of the damage is wasted.
                        damage = 0;
                    } else if (ammoExplosion && battleArmor.locationHasCase(hit.getLocation())) {
                        // Remaining damage prevented by CASE
                        report = new Report(6125);
                        report.subject = entityId;
                        report.add(damage);
                        report.indent(3);
                        reportVec.addElement(report);

                        // The target takes no more damage from the explosion.
                        damage = 0;
                    } else if (damage > 0) {
                        // remaining damage transfers
                        report = new Report(6130);
                        report.subject = entityId;
                        report.indent(2);
                        report.add(damage);
                        report.add(battleArmor.getLocationAbbr(nextHit));
                        reportVec.addElement(report);

                        // If there are split weapons in this location, mark it
                        // as a hit, even if it took no criticals.
                        for (WeaponMounted m : battleArmor.getWeaponList()) {
                            if (m.isSplit()) {
                                if ((m.getLocation() == hit.getLocation()) ||
                                          (m.getLocation() == nextHit.getLocation())) {
                                    battleArmor.setWeaponHit(m);
                                }
                            }
                        }
                        // if this is damage from a nail/rivet gun, and we
                        // transfer
                        // to a location that has armor, and BAR >=5, no damage
                        if ((damageType == DamageType.NAIL_RIVET) &&
                                  (battleArmor.getArmor(nextHit.getLocation()) > 0) &&
                                  (battleArmor.getBARRating(nextHit.getLocation()) >= 5)) {
                            damage = 0;
                            report = new Report(6065);
                            report.subject = entityId;
                            report.indent(2);
                            reportVec.add(report);
                        }
                    }
                }
            } else if (hit.getSpecCrit()) {
                // ok, we dealt damage but didn't go on to internal
                // we get a chance of a crit, using Armor Piercing.
                // but only if we don't have hardened, Ferro-Lamellor, or reactive armor
                if (!(mods.hardenedArmor || mods.ferroLamellorArmor || mods.reactiveArmor)) {
                    mods.specCrits = mods.specCrits + 1;
                }
            }
            // check for breaching
            reportVec.addAll(manager.breachCheck(battleArmor, hit.getLocation(), null, underWater));

            // Special crits
            dealSpecialCritEffects(battleArmor, reportVec, hit, mods, underWater, damageType);

            // If the location has run out of internal structure, finally actually destroy it here. *EXCEPTION:* Aero
            // units have 0 internal structure in every location by default and are handled elsewhere, so they get a
            // bye.
            if ((battleArmor.getInternal(hit) <= 0)) {
                battleArmor.destroyLocation(hit.getLocation());
            }

            // If damage remains, loop to the next location; if not, be sure to stop here because we may need to
            // refer back to the last *damaged* location again later. (This is safe because at damage <= 0 the loop
            // terminates anyway.)
            if (damage > 0) {
                hit = nextHit;
                // Need to update armor status for the new location
                updateArmorTypeMap(mods, battleArmor, hit);
            }
            if (damageIS) {
                mods.wasDamageIS = true;
                damageIS = false;
            }
        }
    }

    public void damageInfantry(Vector<Report> reportVec, Infantry infantry, HitData hit, int damage,
          boolean ammoExplosion, DamageType damageType, boolean areaSatArty, boolean throughFront, boolean underWater,
          boolean nukeS2S, ModsInfo mods) {
        boolean isPlatoon = true;
        int entityId = infantry.getId();
        Hex te_hex = game.getBoard().getHex(infantry.getPosition());
        boolean damageIS = mods.damageIS;
        Report report;

        // Infantry with TSM implants get 2d6 burst damage from ATSM munitions
        if (damageType.equals(DamageType.ANTI_TSM) &&
                  infantry.isConventionalInfantry() &&
                  infantry.antiTSMVulnerable()) {
            Roll diceRoll = Compute.rollD6(2);
            report = new Report(6434);
            report.subject = entityId;
            report.add(diceRoll);
            report.indent(2);
            reportVec.addElement(report);
            damage += diceRoll.getIntValue();
        }

        // area effect against infantry is double damage
        if (areaSatArty) {
            // PBI. Double damage.
            damage *= 2;
            report = new Report(6039);
            report.subject = entityId;
            report.indent(2);
            reportVec.addElement(report);
        }

        // Is the infantry in the open?
        if (ServerHelper.infantryInOpen(infantry,
              te_hex,
              game,
              isPlatoon,
              ammoExplosion,
              hit.isIgnoreInfantryDoubleDamage())) {
            // PBI. Damage is doubled.
            damage *= 2;
            report = new Report(6040);
            report.subject = entityId;
            report.indent(2);
            reportVec.addElement(report);
        }

        // Is the infantry in vacuum?
        if (!infantry.isDestroyed() &&
                  !infantry.isDoomed() &&
                  game.getPlanetaryConditions().getAtmosphere().isLighterThan(Atmosphere.THIN)) {
            // PBI. Double damage.
            damage *= 2;
            report = new Report(6041);
            report.subject = entityId;
            report.indent(2);
            reportVec.addElement(report);
        }

        damage = manageDamageTypeReports(infantry, reportVec, damage, damageType, hit, true, mods);

        // infantry armor can reduce damage
        if (infantry.calcDamageDivisor() != 1.0) {
            report = new Report(6074);
            report.subject = entityId;
            report.indent(2);
            report.add(damage);
            damage = (int) Math.ceil((damage) / infantry.calcDamageDivisor());
            report.add(damage);
            reportVec.addElement(report);
        }

        HitData nextHit;

        // Allocate the damage
        while (damage > 0) {
            int tmpDamageHold = -1;

            // Report this either way
            report = new Report(6065);
            report.subject = entityId;
            report.indent(2);
            report.addDesc(infantry);
            report.add(damage);
            if (damageIS) {
                report.messageId = 6070;
            }
            report.add(infantry.getLocationAbbr(hit));
            reportVec.addElement(report);

            if (!ammoExplosion && (infantry.getArmor(hit) > 0) && !damageIS) {

                // infantry armor works differently
                int armor = infantry.getArmor(hit);
                int men = infantry.getInternal(hit);
                tmpDamageHold = damage % 2;
                damage /= 2;
                if ((tmpDamageHold == 1) && (armor >= men)) {
                    // extra 1 point of damage to armor
                    tmpDamageHold = damage;
                    damage++;
                } else {
                    // extra 0 or 1 points of damage to men
                    tmpDamageHold += damage;
                }

                int armorThreshold = infantry.getArmor(hit);
                if (armorThreshold >= damage) {
                    infantry.setArmor(infantry.getArmor(hit) - damage, hit);

                    infantry.damageThisPhase += tmpDamageHold;
                    damage = 0;
                    if (!infantry.isHardenedArmorDamaged(hit)) {
                        report = new Report(6085);
                    } else {
                        report = new Report(6086);
                    }

                    report.subject = entityId;
                    report.indent(3);
                    report.add(infantry.getArmor(hit));
                    reportVec.addElement(report);
                }

                if ((tmpDamageHold > 0)) {
                    damage = tmpDamageHold;
                }
            }

            // is there damage remaining?
            if (damage > 0) {

                // is there internal structure in the location hit?
                if (infantry.getInternal(hit) > 0) {

                    // Now we need to consider alternate structure types!
                    if (infantry.getInternal(hit) > damage) {
                        // internal structure absorbs all damage
                        infantry.setInternal(infantry.getInternal(hit) - damage, hit);
                        mods.tookInternalDamage = true;
                        // Alternate structures don't affect our damage total
                        // for later PSR purposes, so use the previously stored
                        // value here as necessary.
                        infantry.damageThisPhase += (tmpDamageHold > -1) ? tmpDamageHold : damage;
                        damage = 0;
                        report = new Report(6095);
                        report.subject = entityId;
                        report.indent(3);
                        report.add(infantry.getInternal(hit));
                        reportVec.addElement(report);
                    } else {
                        // damage transfers, maybe
                        int absorbed = Math.max(infantry.getInternal(hit), 0);

                        // Platoon, Trooper, or Section Destroyed message
                        report = new Report(1210);
                        report.subject = entityId;
                        // Infantry have only one section, and are therefore destroyed.
                        if (infantry.isSquad()) {
                            report.messageId = 6106; // Squad Killed
                        } else {
                            report.messageId = 6105; // Platoon Killed
                        }
                        report.indent(3);
                        reportVec.addElement(report);

                        // Mark off the internal structure here, but *don't*
                        // destroy the location just yet -- there are checks
                        // still to run!
                        infantry.setInternal(0, hit);
                        infantry.damageThisPhase += absorbed;
                        damage -= absorbed;
                    }
                }

                if (infantry.getInternal(hit) <= 0) {
                    // the internal structure is gone, what are the transfer
                    // potentials?
                    nextHit = infantry.getTransferLocation(hit);
                    if (nextHit.getLocation() == Entity.LOC_DESTROYED) {
                        // No engine explosions for infantry
                        // Entity destroyed. Ammo explosions are
                        // neither survivable nor salvageable.
                        // Only ammo explosions in the CT are devastating.
                        reportVec.addAll(manager.destroyEntity(infantry,
                              "damage",
                              !ammoExplosion,
                              !((ammoExplosion || areaSatArty))));
                        // If the head is destroyed, kill the crew.

                        if ((hit.getLocation() == Mek.LOC_HEAD) ||
                                  ((hit.getLocation() == Mek.LOC_CT) && (ammoExplosion || areaSatArty))) {
                            infantry.getCrew().setDoomed(true);
                        }
                        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_AUTO_ABANDON_UNIT)) {
                            reportVec.addAll(manager.abandonEntity(infantry));
                        }

                        // nowhere for further damage to go
                        damage = 0;
                    } else if (nextHit.getLocation() == Entity.LOC_NONE) {
                        // The rest of the damage is wasted.
                        damage = 0;
                    } else if (damage > 0) {
                        // remaining damage transfers
                        report = new Report(6130);
                        report.subject = entityId;
                        report.indent(2);
                        report.add(damage);
                        report.add(infantry.getLocationAbbr(nextHit));
                        reportVec.addElement(report);

                        // If there are split weapons in this location, mark it as a hit, even if it took no criticals.
                        for (WeaponMounted m : infantry.getWeaponList()) {
                            if (m.isSplit()) {
                                if ((m.getLocation() == hit.getLocation()) ||
                                          (m.getLocation() == nextHit.getLocation())) {
                                    infantry.setWeaponHit(m);
                                }
                            }
                        }
                        // if this is damage from a nail/rivet gun, and we transfer to a location that has armor, and
                        // BAR >=5, no damage
                        if ((damageType == DamageType.NAIL_RIVET) &&
                                  (infantry.getArmor(nextHit.getLocation()) > 0) &&
                                  (infantry.getBARRating(nextHit.getLocation()) >= 5)) {
                            damage = 0;
                            report = new Report(6065);
                            report.subject = entityId;
                            report.indent(2);
                            reportVec.add(report);
                        }
                    }
                }
            } else if (hit.getSpecCrit()) {
                // ok, we dealt damage but didn't go on to internal
                // we get a chance of a crit, using Armor Piercing.
                // but only if we don't have hardened, Ferro-Lamellor, or reactive armor
                if (!(mods.hardenedArmor || mods.ferroLamellorArmor || mods.reactiveArmor)) {
                    mods.specCrits = mods.specCrits + 1;
                }
            }
        }
    }

    public int calcCritBonus(Entity attacker, Entity entity, int damageOriginal, boolean areaSatArty) {
        // the bonus to the crit roll if using the
        // "advanced determining critical hits rule"
        int critBonus = 0;
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CRIT_ROLL) &&
                  (damageOriginal > 0) &&
                  ((entity instanceof Mek) || (entity instanceof ProtoMek))) {
            critBonus = Math.min((damageOriginal - 1) / 5, 4);
        }

        // Find out if Human TRO plays a part it crit bonus
        if ((attacker != null) && !areaSatArty) {
            if ((entity instanceof Mek) && attacker.hasAbility(OptionsConstants.MISC_HUMAN_TRO, Crew.HUMANTRO_MEK)) {
                critBonus += 1;
            } else if ((entity instanceof Aero) &&
                             attacker.hasAbility(OptionsConstants.MISC_HUMAN_TRO, Crew.HUMANTRO_AERO)) {
                critBonus += 1;
            } else if ((entity instanceof Tank) &&
                             attacker.hasAbility(OptionsConstants.MISC_HUMAN_TRO, Crew.HUMANTRO_VEE)) {
                critBonus += 1;
            } else if ((entity instanceof BattleArmor) &&
                             attacker.hasAbility(OptionsConstants.MISC_HUMAN_TRO, Crew.HUMANTRO_BA)) {
                critBonus += 1;
            }
        }
        return critBonus;
    }

    public boolean checkFerroFibrous(Entity entity, HitData hit) {
        return ((entity.getArmor(hit) > 0) &&
                      ((entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_FERRO_FIBROUS) ||
                             (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_LIGHT_FERRO) ||
                             (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HEAVY_FERRO)));
    }

    public int applyModularArmor(Entity entity, HitData hit, int damage, boolean ammoExplosion, boolean damageIS,
          Vector<Report> reportVec) {

        // modular armor might absorb some damage if the location mounts any.
        if (!ammoExplosion &&
                  !damageIS &&
                  ((hit.getEffect() & HitData.EFFECT_NO_CRITICALS) != HitData.EFFECT_NO_CRITICALS)) {
            int damageNew = entity.getDamageReductionFromModularArmor(hit, damage, reportVec);
            int damageDiff = damage - damageNew;
            entity.damageThisPhase += damageDiff;
            damage = damageNew;
        }
        return damage;
    }

    public int applyTankCASEDamageReduction(Tank tank, HitData hit, int damage, boolean ammoExplosion,
          Vector<Report> reportVec) {
        // check for tank CASE here: damage to rear armor, excess
        // dissipating, and a crew stunned crit
        int entityId = tank.getId();
        Report report;

        if (ammoExplosion && tank.locationHasCase(Tank.LOC_BODY)) {
            tank.damageThisPhase += damage;
            report = new Report(6124);
            report.subject = entityId;
            report.indent(2);
            report.add(damage);
            reportVec.add(report);
            int loc = (tank instanceof SuperHeavyTank) ?
                            SuperHeavyTank.LOC_REAR :
                            (tank instanceof LargeSupportTank) ? LargeSupportTank.LOC_REAR : Tank.LOC_REAR;
            if (damage > tank.getArmor(loc)) {
                tank.setArmor(IArmorState.ARMOR_DESTROYED, loc);
                report = new Report(6090);
            } else {
                tank.setArmor(tank.getArmor(loc) - damage, loc);
                report = new Report(6085);
                report.add(tank.getArmor(loc));
            }
            report.subject = entityId;
            report.indent(3);
            reportVec.add(report);
            damage = 0;
            int critIndex;
            if (tank.isCommanderHit() && tank.isDriverHit()) {
                critIndex = Tank.CRIT_CREW_KILLED;
            } else {
                critIndex = Tank.CRIT_CREW_STUNNED;
            }
            reportVec.addAll(manager.applyCriticalHit(tank,
                  Entity.NONE,
                  new CriticalSlot(0, critIndex),
                  true,
                  0,
                  false));
        }
        return damage;
    }

    public int applyCASEIIDamageReduction(Entity entity, HitData hit, int damage, boolean ammoExplosion,
          Vector<Report> reportVec) {
        // Check for CASE II right away. If so, reduce damage to 1 and let it hit the IS. Also, remove as much of the
        // rear armor as allowed by the damage. If arm/leg/head, Then they lose all their armor if it's less than the
        // explosion damage.
        int entityId = entity.getId();
        Report report;

        if (ammoExplosion && entity.hasCASEII(hit.getLocation())) {
            // 1 point of damage goes to IS
            damage--;
            // Remaining damage prevented by CASE II
            report = new Report(6126);
            report.subject = entityId;
            report.add(damage);
            report.indent(3);
            reportVec.addElement(report);
            int loc = hit.getLocation();
            if ((entity instanceof Mek) &&
                      ((loc == Mek.LOC_HEAD) || ((Mek) entity).isArm(loc) || entity.locationIsLeg(loc))) {
                int half = (int) Math.ceil(entity.getOArmor(loc, false) / 2.0);
                if (damage > half) {
                    damage = half;
                }
                if (damage >= entity.getArmor(loc, false)) {
                    entity.setArmor(IArmorState.ARMOR_DESTROYED, loc, false);
                } else {
                    entity.setArmor(entity.getArmor(loc, false) - damage, loc, false);
                }
            } else {
                if (damage >= entity.getArmor(loc, true)) {
                    entity.setArmor(IArmorState.ARMOR_DESTROYED, loc, true);
                } else {
                    entity.setArmor(entity.getArmor(loc, true) - damage, loc, true);
                }
            }

            if (entity.getInternal(hit) > 0) {
                // Mek takes 1 point of IS damage
                damage = 1;
            } else {
                damage = 0;
            }

            entity.damageThisPhase += damage;

            Roll diceRoll = Compute.rollD6(2);
            report = new Report(6127);
            report.subject = entity.getId();
            report.add(diceRoll);
            reportVec.add(report);

            if (diceRoll.getIntValue() >= 8) {
                hit.setEffect(HitData.EFFECT_NO_CRITICALS);
            }
        }

        return damage;
    }

    public void dealSpecialCritEffects(Entity entity, Vector<Report> reportVec, HitData hit, ModsInfo mods,
          boolean underWater, DamageType damageType) {

        int crits = mods.crits;

        // resolve special results
        if ((hit.getEffect() & HitData.EFFECT_VEHICLE_MOVE_DAMAGED) == HitData.EFFECT_VEHICLE_MOVE_DAMAGED) {
            reportVec.addAll(manager.vehicleMotiveDamage((Tank) entity, hit.getMotiveMod()));
        }
        // Damage from any source can break spikes
        if (entity.hasWorkingMisc(MiscType.F_SPIKES, -1, hit.getLocation())) {
            reportVec.add(manager.checkBreakSpikes(entity, hit.getLocation()));
        }

        // roll all critical hits against this location
        // unless the section destroyed in a previous phase?
        // Cause a crit.
        if ((entity.getInternal(hit) != IArmorState.ARMOR_DESTROYED) &&
                  ((hit.getEffect() & HitData.EFFECT_NO_CRITICALS) != HitData.EFFECT_NO_CRITICALS)) {
            for (int i = 0; i < crits; i++) {
                reportVec.addAll(manager.criticalEntity(entity,
                      hit.getLocation(),
                      hit.isRear(),
                      hit.glancingMod() + mods.critBonus,
                      mods.damageOriginal,
                      damageType));
            }
            crits = 0;

            for (int i = 0; i < mods.specCrits; i++) {
                // against BAR or reflective armor, we get a +2 mod
                int critMod = entity.hasBARArmor(hit.getLocation()) ? 2 : 0;
                critMod += ((mods.reflectiveArmor) && !(mods.isBattleArmor)) ? 2 : 0; // BA
                // against impact armor, we get a +1 mod
                critMod += (mods.impactArmor) ? 1 : 0;
                // hardened armour has no crit penalty
                if (!mods.hardenedArmor) {
                    // non-hardened armor gets modifiers
                    // the -2 for hardened is handled in the critBonus
                    // variable
                    critMod += hit.getSpecCritMod();
                    critMod += hit.glancingMod();
                }
                reportVec.addAll(manager.criticalEntity(entity,
                      hit.getLocation(),
                      hit.isRear(),
                      critMod + mods.critBonus,
                      mods.damageOriginal));
            }
            mods.specCrits = 0;
        }
        mods.crits = crits;
    }

    protected ModsInfo createDamageModifiers(Entity entity, HitData hit, boolean damageIS, int damageOriginal,
          int crits) {
        // Map that stores various values for passing and mutating.
        ModsInfo mods = new ModsInfo();

        mods.crits = crits;
        mods.damageOriginal = damageOriginal;
        mods.damageIS = damageIS;
        mods.tookInternalDamage = damageIS;
        updateArmorTypeMap(mods, entity, hit);
        return mods;
    }

    protected void updateArmorTypeMap(ModsInfo mods, Entity entity, HitData hit) {
        boolean isBattleArmor = (entity instanceof BattleArmor);

        mods.isBattleArmor = isBattleArmor;

        mods.ferroFibrousArmor = (checkFerroFibrous(entity, hit));
        mods.bar5 = (entity.getBARRating(hit.getLocation()) <= 5);
        mods.ballisticArmor = ((entity instanceof Mek) || (entity instanceof Tank) || (entity instanceof Aero)) &&
                                    (entity.getArmorType(hit.getLocation()) ==
                                           EquipmentType.T_ARMOR_BALLISTIC_REINFORCED);
        mods.ferroLamellorArmor = ((entity instanceof Mek) || (entity instanceof Tank) || (entity instanceof Aero)) &&
                                        (entity.getArmorType(hit.getLocation()) ==
                                               EquipmentType.T_ARMOR_FERRO_LAMELLOR);
        mods.hardenedArmor = ((entity instanceof Mek) || (entity instanceof Tank)) &&
                                   (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HARDENED);
        mods.impactArmor = (entity instanceof Mek) &&
                                 (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_IMPACT_RESISTANT);
        mods.reactiveArmor = (((entity instanceof Mek) || (entity instanceof Tank) || (entity instanceof Aero)) &&
                                    (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_REACTIVE)) ||
                                   (isBattleArmor &&
                                          (entity.getArmorType(hit.getLocation()) ==
                                                 EquipmentType.T_ARMOR_BA_REACTIVE));
        mods.reflectiveArmor = (((entity instanceof Mek) || (entity instanceof Tank) || (entity instanceof Aero)) &&
                                      (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_REFLECTIVE)) ||
                                     isBattleArmor &&
                                           (entity.getArmorType(hit.getLocation()) ==
                                                  EquipmentType.T_ARMOR_BA_REFLECTIVE);
    }

    public int manageDamageTypeReports(Entity entity, Vector<Report> reportVec, int damage, DamageType damageType,
          HitData hit, boolean isPlatoon, ModsInfo mods) {
        Report report;
        int entityId = entity.getId();

        switch (damageType) {
            case FRAGMENTATION:
                // Fragmentation missiles deal full damage to conventional infantry (only) and no damage to other
                // target types.
                if (!isPlatoon) {
                    damage = 0;
                    report = new Report(6050); // For some reason this report never
                    // actually shows up...
                } else {
                    report = new Report(6045); // ...but this one displays just fine.
                }

                report.subject = entityId;
                report.indent(2);
                reportVec.addElement(report);
                break;
            case NONPENETRATING:
                if (!isPlatoon) {
                    damage = 0;
                    report = new Report(6051);
                    report.subject = entityId;
                    report.indent(2);
                    reportVec.addElement(report);
                }
                break;
            case FLECHETTE:
                // Fléchette ammo deals full damage to conventional infantry and half-damage to other targets
                // (including battle armor).
                if (!isPlatoon) {
                    damage /= 2;
                    report = new Report(6060);
                } else {
                    report = new Report(6055);
                }

                report.subject = entityId;
                report.indent(2);
                reportVec.addElement(report);
                break;
            case ACID:
                if (mods.ferroFibrousArmor ||
                          mods.reactiveArmor ||
                          mods.reflectiveArmor ||
                          mods.ferroLamellorArmor ||
                          mods.bar5) {
                    if (entity.getArmor(hit) <= 0) {
                        break; // hitting IS, not acid-affected armor
                    }
                    damage = Math.min(entity.getArmor(hit), 3);
                    report = new Report(6061);
                    report.subject = entityId;
                    report.indent(2);
                    report.add(damage);
                    reportVec.addElement(report);
                } else if (isPlatoon) {
                    damage = (int) Math.ceil(damage * 1.5);
                    report = new Report(6062);
                    report.subject = entityId;
                    report.indent(2);
                    reportVec.addElement(report);
                }
                break;
            case INCENDIARY:
                // Incendiary AC ammo does +2 damage to unarmored infantry
                if (isPlatoon) {
                    damage += 2;
                    report = new Report(6064);
                    report.subject = entityId;
                    report.indent(2);
                    reportVec.addElement(report);
                }
                break;
            case NAIL_RIVET:
                // no damage against armor of BAR rating >=5
                if ((entity.getBARRating(hit.getLocation()) >= 5) && (entity.getArmor(hit.getLocation()) > 0)) {
                    damage = 0;
                    report = new Report(6063);
                    report.subject = entityId;
                    report.indent(2);
                    reportVec.add(report);
                }
                break;
            default:
                // We can ignore this.
                break;
        }
        return damage;
    }

    public int handleExternalPassengerDamage(Entity entity, HitData hit, int damage, boolean ammoExplosion,
          DamageType damageType, Vector<Report> reportVec) {
        int entityId = entity.getId();
        Report report;
        int extantDamage = damage;
        int nLoc = hit.getLocation();
        Entity passenger = entity.getExteriorUnitAt(nLoc, hit.isRear());
        // Does an exterior passenger absorb some damage?
        if (!ammoExplosion &&
                  (null != passenger) &&
                  !passenger.isDoomed() &&
                  (damageType != DamageType.IGNORE_PASSENGER)) {
            extantDamage = manager.damageExternalPassenger(entity, hit, damage, reportVec, passenger);
        }

        boolean bTorso = (nLoc == Mek.LOC_CT) || (nLoc == Mek.LOC_RT) || (nLoc == Mek.LOC_LT);

        // Does a swarming unit absorb damage?
        int swarmer = entity.getSwarmAttackerId();
        if ((!(entity instanceof Mek) || bTorso) &&
                  (swarmer != Entity.NONE) &&
                  ((hit.getEffect() & HitData.EFFECT_CRITICAL) == 0) &&
                  (Compute.d6() >= 5) &&
                  (damageType != DamageType.IGNORE_PASSENGER) &&
                  !ammoExplosion) {
            Entity swarm = game.getEntity(swarmer);

            if (swarm != null) {
                // Yup. Roll up some hit data for that passenger.
                report = new Report(6076);
                report.subject = swarmer;
                report.indent(3);
                report.addDesc(swarm);
                reportVec.addElement(report);

                HitData passHit = swarm.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);

                // How much damage will the swarm absorb?
                int absorb = 0;
                HitData nextPassHit = passHit;
                do {
                    if (0 < swarm.getArmor(nextPassHit)) {
                        absorb += swarm.getArmor(nextPassHit);
                    }
                    if (0 < swarm.getInternal(nextPassHit)) {
                        absorb += swarm.getInternal(nextPassHit);
                    }
                    nextPassHit = swarm.getTransferLocation(nextPassHit);
                } while ((extantDamage > absorb) && (nextPassHit.getLocation() >= 0));


                // Damage the swarm.
                int absorbedDamage = Math.min(extantDamage, absorb);
                Vector<Report> newReports = damageEntity(new DamageInfo(swarm, passHit, absorbedDamage));
                for (Report newReport : newReports) {
                    newReport.indent(2);
                }
                reportVec.addAll(newReports);

                // Did some damage pass on?
                if (extantDamage > absorb) {
                    // Yup. Remove the absorbed damage.
                    extantDamage -= absorb;
                    report = new Report(6080);
                    report.subject = entityId;
                    report.indent(2);
                    report.add(extantDamage);
                    report.addDesc(entity);
                    reportVec.addElement(report);
                }
            }
        }
        return Math.max(extantDamage, 0);
    }

    /**
     * Apply damage to the armor in the hit location; remaining damage will be applied elsewhere.
     *
     * @param entity        Entity being damaged
     * @param hit           HitData recording aspects of the incoming damage
     * @param damage        Actual amount of incoming damage
     * @param ammoExplosion Whether damage was caused by an ammo explosion
     * @param damageIS      Whether damage is going straight to the internal structure
     * @param areaSatArty   Whether damage is caused by AE attack
     * @param reportVec     Vector of Reports containing prior reports; usually modded and returned
     *
     * @return Remaining damage not absorbed by armor
     */
    public int applyEntityArmorDamage(Entity entity, HitData hit, int damage, boolean ammoExplosion, boolean damageIS,
          boolean areaSatArty, Vector<Report> reportVec, ModsInfo mods) {
        boolean ferroLamellorArmor = mods.ferroLamellorArmor;
        boolean ballisticArmor = mods.ballisticArmor;
        boolean hardenedArmor = mods.hardenedArmor;
        boolean impactArmor = mods.impactArmor;
        boolean reflectiveArmor = mods.reflectiveArmor;
        boolean reactiveArmor = mods.reactiveArmor;
        boolean isBattleArmor = (entity instanceof BattleArmor);
        int damageOriginal = mods.damageOriginal;
        int critBonus = mods.critBonus;

        int entityId = entity.getId();
        Report report;

        // is there armor in the location hit?
        if (!ammoExplosion && (entity.getArmor(hit) > 0) && !damageIS) {
            int tmpDamageHold = -1;
            int origDamage = damage;

            if (ferroLamellorArmor &&
                      (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING) &&
                      (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING_MISSILE) &&
                      (hit.getGeneralDamageType() != HitData.DAMAGE_IGNORES_DMG_REDUCTION) &&
                      (hit.getGeneralDamageType() != HitData.DAMAGE_AX)) {
                tmpDamageHold = damage;
                damage = (int) Math.floor((((double) damage) * 4) / 5);
                if (damage <= 0) {
                    mods.isHeadHit = false;
                    mods.crits = 0;
                }
                report = new Report(6073);
                report.subject = entityId;
                report.indent(3);
                report.add(damage);
                reportVec.addElement(report);
            } else if (ballisticArmor &&
                             ((hit.getGeneralDamageType() == HitData.DAMAGE_ARMOR_PIERCING_MISSILE) ||
                                    (hit.getGeneralDamageType() == HitData.DAMAGE_ARMOR_PIERCING) ||
                                    (hit.getGeneralDamageType() == HitData.DAMAGE_BALLISTIC) ||
                                    (hit.getGeneralDamageType() == HitData.DAMAGE_AX)
                                    //AX doesn't affect ballistic-reinforced armor, TO:AUE (6th), pg. 179
                                    ||
                                    (hit.getGeneralDamageType() == HitData.DAMAGE_MISSILE))) {
                tmpDamageHold = damage;
                damage = Math.max(1, damage / 2);
                report = new Report(6088);
                report.subject = entityId;
                report.indent(3);
                report.add(damage);
                reportVec.addElement(report);
            } else if (impactArmor && (hit.getGeneralDamageType() == HitData.DAMAGE_PHYSICAL)) {
                // As long as there is even 1 point of armor in this location, reduce _all_ damage
                // to 2 points for every whole 3 points applied (IntOps pg 88).
                damage = Math.max(1, (2 * (damage / 3)) + (damage % 3));
                report = new Report(6089);
                report.subject = entityId;
                report.indent(3);
                report.add(damage);
                reportVec.addElement(report);
            } else if (reflectiveArmor &&
                             (hit.getGeneralDamageType() == HitData.DAMAGE_PHYSICAL) &&
                             !isBattleArmor) { // BA reflect does not receive extra physical damage
                tmpDamageHold = damage;
                int currArmor = entity.getArmor(hit);
                int dmgToDouble = Math.min(damage, currArmor / 2);
                damage += dmgToDouble;
                report = new Report(6066);
                report.subject = entityId;
                report.indent(3);
                report.add(currArmor);
                report.add(tmpDamageHold);
                report.add(dmgToDouble);
                report.add(damage);
                reportVec.addElement(report);
            } else if (reflectiveArmor && areaSatArty && !isBattleArmor) {
                tmpDamageHold = damage; // BA reflect does not receive extra AE damage
                int currArmor = entity.getArmor(hit);
                int dmgToDouble = Math.min(damage, currArmor / 2);
                damage += dmgToDouble;
                report = new Report(6087);
                report.subject = entityId;
                report.indent(3);
                report.add(currArmor);
                report.add(tmpDamageHold);
                report.add(dmgToDouble);
                report.add(damage);
                reportVec.addElement(report);
            } else if (reflectiveArmor && (hit.getGeneralDamageType() == HitData.DAMAGE_ENERGY)) {
                tmpDamageHold = damage;
                damage = (int) Math.floor(((double) damage) / 2);
                if (tmpDamageHold == 1) {
                    damage = 1;
                }
                report = new Report(6067);
                report.subject = entityId;
                report.indent(3);
                report.add(damage);
                reportVec.addElement(report);
            } else if (reactiveArmor &&
                             ((hit.getGeneralDamageType() == HitData.DAMAGE_MISSILE) ||
                                    (hit.getGeneralDamageType() == HitData.DAMAGE_ARMOR_PIERCING_MISSILE) ||
                                    areaSatArty)) {
                tmpDamageHold = damage;
                damage = (int) Math.floor(((double) damage) / 2);
                if (tmpDamageHold == 1) {
                    damage = 1;
                }
                report = new Report(6068);
                report.subject = entityId;
                report.indent(3);
                report.add(damage);
                reportVec.addElement(report);
            }

            // If we're using optional tank damage thresholds, set up our hit
            // effects now...
            if ((entity instanceof Tank) &&
                      game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD) &&
                      !((entity instanceof VTOL) || (entity instanceof GunEmplacement))) {
                int thresh = (int) Math.ceil((game.getOptions()
                                                    .booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_VARIABLE) ?
                                                    entity.getArmor(hit) :
                                                    entity.getOArmor(hit)) /
                                                   (double) game.getOptions()
                                                                  .intOption(OptionsConstants.ADVCOMBAT_VEHICLES_THRESHOLD_DIVISOR));

                // adjust for hardened armor
                if (hardenedArmor &&
                          (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING) &&
                          (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING_MISSILE) &&
                          (hit.getGeneralDamageType() != HitData.DAMAGE_IGNORES_DMG_REDUCTION)) {
                    thresh *= 2;
                }

                if ((damage > thresh) || (entity.getArmor(hit) < damage)) {
                    hit.setEffect(((Tank) entity).getPotCrit());
                    ((Tank) entity).setOverThresh(true);
                    // TACs from the hit location table
                    mods.crits = (((hit.getEffect() & HitData.EFFECT_CRITICAL) == HitData.EFFECT_CRITICAL) ? 1 : 0);
                } else {
                    ((Tank) entity).setOverThresh(false);
                    mods.crits = 0;
                }
            }

            // if there's a mast mount in the rotor, it and all other equipment on it get destroyed if it takes
            // any amount of damage (0 is no damage)
            if ((entity instanceof VTOL) &&
                      (hit.getLocation() == VTOL.LOC_ROTOR) &&
                      entity.hasWorkingMisc(MiscType.F_MAST_MOUNT, -1, VTOL.LOC_ROTOR) &&
                      (damage > 0)) {
                report = new Report(6081);
                report.subject = entityId;
                report.indent(2);
                reportVec.addElement(report);
                for (Mounted<?> mount : entity.getMisc()) {
                    if (mount.getLocation() == VTOL.LOC_ROTOR) {
                        mount.setHit(true);
                    }
                }
            }
            // Need to account for the possibility of hardened armor here
            int armorThreshold = entity.getArmor(hit);
            if (hardenedArmor &&
                      (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING) &&
                      (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING_MISSILE) &&
                      (hit.getGeneralDamageType() != HitData.DAMAGE_IGNORES_DMG_REDUCTION)) {
                armorThreshold *= 2;
                armorThreshold -= (entity.isHardenedArmorDamaged(hit)) ? 1 : 0;
                reportVec.lastElement().newlines = 0;
                report = new Report(6069);
                report.subject = entityId;
                report.indent(3);
                int reportedDamage = damage / 2;
                if ((damage % 2) > 0) {
                    report.add(reportedDamage + ".5");
                } else {
                    report.add(reportedDamage);
                }

                reportVec.addElement(report);
            }

            if (armorThreshold >= damage) {

                // armor absorbs all damage
                // Hardened armor deals with damage in its own fashion...
                if (hardenedArmor &&
                          (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING) &&
                          (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING_MISSILE) &&
                          (hit.getGeneralDamageType() != HitData.DAMAGE_IGNORES_DMG_REDUCTION)) {
                    armorThreshold -= damage;
                    entity.setHardenedArmorDamaged(hit, (armorThreshold % 2) > 0);
                    entity.setArmor((armorThreshold / 2) + (armorThreshold % 2), hit);
                    // Halve damage for hardened armor here so PSRs work correctly
                    damage = damage / 2;
                } else {
                    entity.setArmor(entity.getArmor(hit) - damage, hit);
                }

                // set "armor damage" flag for HarJel II/III
                // we only care about this if there is armor remaining,
                // so don't worry about the case where damage exceeds
                // armorThreshold
                if ((entity instanceof Mek) && (damage > 0)) {
                    ((Mek) entity).setArmorDamagedThisTurn(hit.getLocation(), true);
                }

                // if the armor is hardened, any penetrating crits are
                // rolled at -2
                if (hardenedArmor) {
                    mods.critBonus = critBonus - 2;
                }

                // We should only record the applied damage, although original and tmpDamageHold
                // damage may be needed for other calculations
                entity.damageThisPhase += damage;

                damage = 0;
                if (!entity.isHardenedArmorDamaged(hit)) {
                    report = new Report(6085);
                } else {
                    report = new Report(6086);
                }

                report.subject = entityId;
                report.indent(3);
                report.add(entity.getArmor(hit));
                reportVec.addElement(report);

                // teleMissiles are destroyed if they lose all armor
                if ((entity instanceof TeleMissile) && (entity.getArmor(hit) == damage)) {
                    reportVec.addAll(manager.destroyEntity(entity, "damage", false));
                }

            } else {
                // damage goes on to internal
                int absorbed = Math.max(entity.getArmor(hit), 0);
                if (hardenedArmor &&
                          (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING) &&
                          (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING_MISSILE)) {
                    absorbed = (absorbed * 2) - ((entity.isHardenedArmorDamaged(hit)) ? 1 : 0);
                }
                if (reflectiveArmor && (hit.getGeneralDamageType() == HitData.DAMAGE_PHYSICAL) && !isBattleArmor) {
                    absorbed = (int) Math.ceil(absorbed / 2.0);
                    damage = tmpDamageHold;
                    tmpDamageHold = 0;
                }
                entity.setArmor(IArmorState.ARMOR_DESTROYED, hit);
                if (tmpDamageHold >= 0) {
                    entity.damageThisPhase += 2 * absorbed;
                } else {
                    entity.damageThisPhase += absorbed;
                }
                damage -= absorbed;
                report = new Report(6090);
                report.subject = entityId;
                report.indent(3);
                reportVec.addElement(report);
                if (entity instanceof GunEmplacement) {
                    // gun emplacements have no internal,
                    // destroy the section
                    entity.destroyLocation(hit.getLocation());
                    report = new Report(6115);
                    report.subject = entityId;
                    reportVec.addElement(report);

                    if (entity.getTransferLocation(hit).getLocation() == Entity.LOC_DESTROYED) {
                        reportVec.addAll(manager.destroyEntity(entity, "damage", false));
                    }
                }
            }

            // targets with BAR armor get crits, depending on damage and BAR rating
            if (entity.hasBARArmor(hit.getLocation())) {
                if (origDamage > entity.getBARRating(hit.getLocation())) {
                    if (entity.hasArmoredChassis()) {
                        // crit roll with -1 mod
                        reportVec.addAll(manager.criticalEntity(entity,
                              hit.getLocation(),
                              hit.isRear(),
                              -1 + critBonus,
                              damageOriginal));
                    } else {
                        reportVec.addAll(manager.criticalEntity(entity,
                              hit.getLocation(),
                              hit.isRear(),
                              critBonus,
                              damageOriginal));
                    }
                }
            }
        }
        return damage;
    }

    public static class ModsInfo {
        public boolean ballisticArmor = false;
        public boolean ferroFibrousArmor = false;
        public boolean ferroLamellorArmor = false;
        public boolean hardenedArmor = false;
        public boolean impactArmor = false;
        public boolean reactiveArmor = false;
        public boolean reflectiveArmor = false;
        public boolean isBattleArmor = false;
        public boolean isHeadHit = false;
        public boolean damageIS = false;
        public boolean wasDamageIS = false;
        public boolean bar5 = false;
        public boolean tookInternalDamage = false;
        public int critBonus = 0;
        public int crits = 0;
        public int specCrits = 0;
        public int damageOriginal = 0;
    }
}
