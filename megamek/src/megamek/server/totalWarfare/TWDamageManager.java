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

package megamek.server.totalWarfare;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import megamek.common.CriticalSlot;
import megamek.common.DamageInfo;
import megamek.common.Hex;
import megamek.common.HexTarget;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.SuicideImplantsAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.enums.BombType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.*;
import megamek.common.weapons.DamageType;
import megamek.common.weapons.TeleMissile;
import megamek.server.IDamageManager;
import megamek.server.ServerHelper;

public class TWDamageManager implements IDamageManager {
    protected TWGameManager manager = null;
    protected Game game = null;
    protected boolean initialized = false;

    // Remove requirement that
    public TWDamageManager() {}

    public TWDamageManager(TWGameManager manager) {
        setManager(manager);
    }

    public TWDamageManager(TWGameManager manager, Game game) {
        setManager(manager);
        setGame(game);
    }

    public void setGame(Game game) {
        this.game = game;
        initialized = (manager != null);
    }

    public void setManager(TWGameManager manager) {
        this.manager = manager;
        initialized = (game != null);
    }

    /**
     * Unpacks DamageInfo Record into arguments and checks that the Game Manager and Game are initialized
     *
     * @param damageInfo {@link megamek.common.DamageInfo}  for details
     *
     * @return Vector of reports detailing damage dealt.
     */
    @Override
    public Vector<Report> damageEntity(DamageInfo damageInfo) {
        if (!initialized) {
            String message = (game == null && manager == null) ? "Game Manager and Game not initialized" :
                  (manager == null) ? "Game Manager not initialized" : "Game not initialized";
            throw new RuntimeException(message);
        }

        Vector<Report> vDesc = new Vector<>();
        return damageEntity(
              damageInfo.entity(),
              damageInfo.hit(),
              damageInfo.damage(),
              damageInfo.ammoExplosion(),
              damageInfo.damageType(),
              damageInfo.damageIS(),
              damageInfo.areaSatArty(),
              damageInfo.throughFront(),
              damageInfo.underWater(),
              damageInfo.nukeS2S(),
              vDesc
        );
    }

    /**
     * Deals the listed damage to an entity. Returns a vector of Reports for the phase report
     *
     * @param entity        the target entity
     * @param hit           the hit data for the location hit
     * @param damage        the damage to apply
     * @param ammoExplosion ammo explosion type damage is applied directly to the IS, hurts the pilot, causes
     *                      auto-ejects, and can blow the unit to smithereens
     * @param damageType    The DamageType of the attack.
     * @param damageIS      Should the target location's internal structure be damaged directly?
     * @param areaSatArty   Is the damage from an area saturating artillery attack?
     * @param throughFront  Is the damage coming through the hex the unit is facing?
     * @param underWater    Is the damage coming from an underwater attack?
     * @param nukeS2S       is this a ship-to-ship nuke?
     * @param reportVec     Vector of Reports containing prior reports; usually modded and returned
     *
     * @return a <code>Vector</code> of <code>Report</code>s
     */
    public Vector<Report> damageEntity(Entity entity, HitData hit, int damage, boolean ammoExplosion,
          DamageType damageType, boolean damageIS, boolean areaSatArty, boolean throughFront, boolean underWater,
          boolean nukeS2S, Vector<Report> reportVec) {

        Report report;
        int entityId = entity.getId();
        boolean baTookCrit = false; // Track if BA took a crit for VDNI/BVDNI feedback

        // if this is a fighter squadron then pick an active fighter and pass on
        // the damage
        if (entity instanceof FighterSquadron) {
            List<Entity> fighters = entity.getActiveSubEntities();

            if (fighters.isEmpty()) {
                return reportVec;
            }
            Entity fighter = fighters.get(hit.getLocation());
            HitData newHit = fighter.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
            newHit.setBoxCars(hit.rolledBoxCars());
            newHit.setGeneralDamageType(hit.getGeneralDamageType());
            newHit.setCapital(hit.isCapital());
            newHit.setCapMisCritMod(hit.getCapMisCritMod());
            newHit.setSingleAV(hit.getSingleAV());
            newHit.setAttackerId(hit.getAttackerId());
            return damageEntity(fighter,
                  newHit,
                  damage,
                  ammoExplosion,
                  damageType,
                  damageIS,
                  areaSatArty,
                  throughFront,
                  underWater,
                  nukeS2S,
                  reportVec
            );
        }

        // Battle Armor takes full damage to each trooper from area-effect.
        if (areaSatArty && (entity instanceof BattleArmor)) {
            report = new Report(6044);
            report.subject = entity.getId();
            report.indent(2);
            reportVec.add(report);
            for (int i = 0; i < ((BattleArmor) entity).getTroopers(); i++) {
                hit.setLocation(BattleArmor.LOC_TROOPER_1 + i);
                if (entity.getInternal(hit) > 0) {
                    reportVec.addAll(
                          damageEntity(entity,
                                hit,
                                damage,
                                ammoExplosion,
                                damageType,
                                damageIS,
                                false,
                                throughFront,
                                underWater,
                                nukeS2S,
                                reportVec
                          )
                    );
                }
            }
            return reportVec;
        }

        // This is good for shields if a shield absorbs the hit it shouldn't
        // affect the pilot.
        // TC SRM's that hit the head do external and internal damage but its
        // one hit and shouldn't cause
        // 2 hits to the pilot.
        boolean isHeadHit = (entity instanceof Mek) &&
              (((Mek) entity).getCockpitType() != Mek.COCKPIT_TORSO_MOUNTED) &&
              (hit.getLocation() == Mek.LOC_HEAD) &&
              ((hit.getEffect() & HitData.EFFECT_NO_CRITICAL_SLOTS) != HitData.EFFECT_NO_CRITICAL_SLOTS);

        // booleans to indicate criticalSlots for AT2
        boolean critSI = false;
        boolean critThresh = false;

        // Per SO p.116: For weapon groups/bays, threshold critical checks use only the
        // damage of a SINGLE weapon (singleAV), not the combined damage from all weapons
        // that hit. This prevents massed small weapons from always triggering threshold
        // criticals. The full damage is still applied to the target.
        int threshDamage = damage;
        if ((hit.getSingleAV() > -1) && !game.getOptions()
              .booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
            threshDamage = hit.getSingleAV();
        }

        // is this capital-scale damage
        boolean isCapital = hit.isCapital();

        // check capital/standard damage
        if (isCapital &&
              (!entity.isCapitalScale() ||
                    game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY))) {
            damage = 10 * damage;
            threshDamage = 10 * threshDamage;
        }
        if (!isCapital &&
              entity.isCapitalScale() &&
              !game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
            damage = (int) Math.round(damage / 10.0);
            threshDamage = (int) Math.round(threshDamage / 10.0);
        }

        int damage_orig = damage;

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
        } // while
        // if edge was uses, give at end overview of remaining
        if (hit.getUndoneLocation() != null) {
            report = new Report(6510);
            report.subject = entityId;
            report.indent(2);
            report.addDesc(entity);
            report.add(entity.getCrew().getOptions().intOption(OptionsConstants.EDGE));
            reportVec.addElement(report);
        } // if

        boolean autoEject = false;
        if (ammoExplosion) {
            if (entity instanceof Mek mek) {
                if (mek.isAutoEject() &&
                      (!game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) ||
                            (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) &&
                                  mek.isCondEjectAmmo()))) {
                    autoEject = true;
                    reportVec.addAll(manager.ejectEntity(entity, true));
                }
            } else if (entity instanceof Aero aero) {
                if (aero.isAutoEject() &&
                      (!game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) ||
                            (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) &&
                                  aero.isCondEjectAmmo()))) {
                    autoEject = true;
                    reportVec.addAll(manager.ejectEntity(entity, true));
                }
            }
        }
        boolean isBattleArmor = entity instanceof BattleArmor;
        boolean isPlatoon = !isBattleArmor && (entity instanceof Infantry);
        boolean isFerroFibrousTarget = false;
        boolean wasDamageIS = false;
        boolean tookInternalDamage = damageIS;
        boolean tookAnyDamage = damage > 0; // Track if any damage was applied for Proto DNI feedback
        Hex te_hex = null;

        // Track initial trooper count for suicide implant reactive damage (IO pg 83)
        // Only track if: conventional infantry, has suicide implants, and this is NOT reactive damage
        int initialTroopers = -1;
        boolean hasSuicideImplants = entity.hasAbility(OptionsConstants.MD_SUICIDE_IMPLANTS);
        boolean checkSuicideImplantReaction = isPlatoon
              && hasSuicideImplants
              && !damageType.equals(DamageType.SUICIDE_IMPLANT_REACTION);

        if (checkSuicideImplantReaction) {
            initialTroopers = entity.getInternal(Infantry.LOC_INFANTRY);
        }

        boolean hardenedArmor = ((entity instanceof Mek) || (entity instanceof Tank)) &&
              (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HARDENED);
        boolean ferroLamellorArmor = ((entity instanceof Mek) || (entity instanceof Tank) || (entity instanceof Aero))
              &&
              (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_FERRO_LAMELLOR);
        boolean reflectiveArmor = (((entity instanceof Mek) || (entity instanceof Tank) || (entity instanceof Aero)) &&
              (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_REFLECTIVE)) ||
              (isBattleArmor &&
                    (entity.getArmorType(hit.getLocation()) ==
                          EquipmentType.T_ARMOR_BA_REFLECTIVE));
        boolean reactiveArmor = (((entity instanceof Mek) || (entity instanceof Tank) || (entity instanceof Aero)) &&
              (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_REACTIVE)) ||
              (isBattleArmor &&
                    (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_BA_REACTIVE));
        boolean ballisticArmor = ((entity instanceof Mek) || (entity instanceof Tank) || (entity instanceof Aero)) &&
              (entity.getArmorType(hit.getLocation()) ==
                    EquipmentType.T_ARMOR_BALLISTIC_REINFORCED);
        boolean impactArmor = (entity instanceof Mek) &&
              (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_IMPACT_RESISTANT);
        boolean bar5 = entity.getBARRating(hit.getLocation()) <= 5;
        boolean heatArmor =
              (entity instanceof Mek) && (entity.getArmorType(hit.getLocation())
                    == EquipmentType.T_ARMOR_HEAT_DISSIPATING);
        boolean abaArmor = (entity instanceof Mek) && (entity.getArmorType(hit.getLocation()) ==
              EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION);

        // TACs from the hit location table
        int crits;
        if ((hit.getEffect() & HitData.EFFECT_CRITICAL) == HitData.EFFECT_CRITICAL) {
            crits = 1;
        } else {
            crits = 0;
        }

        // this is for special crits, like AP and tandem-charge
        int specCrits = 0;

        // the bonus to the crit roll if using the
        // "advanced determining critical hits rule"
        int critBonus = 0;
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CRIT_ROLL) &&
              (damage_orig > 0) &&
              ((entity instanceof Mek) || (entity instanceof ProtoMek))) {
            critBonus = Math.min((damage_orig - 1) / 5, 4);
        }

        // Find out if Human TRO plays a part it crit bonus
        Entity attacker = game.getEntity(hit.getAttackerId());
        if ((attacker != null) && !areaSatArty) {
            if ((entity instanceof Mek) && attacker.hasAbility(OptionsConstants.MISC_HUMAN_TRO, Crew.HUMAN_TRO_MEK)) {
                critBonus += 1;
            } else if ((entity instanceof Aero) && attacker.hasAbility(OptionsConstants.MISC_HUMAN_TRO,
                  Crew.HUMAN_TRO_AERO)) {
                critBonus += 1;
            } else if ((entity instanceof Tank) && attacker.hasAbility(OptionsConstants.MISC_HUMAN_TRO,
                  Crew.HUMAN_TRO_VEE)) {
                critBonus += 1;
            } else if ((entity instanceof BattleArmor) &&
                  attacker.hasAbility(OptionsConstants.MISC_HUMAN_TRO, Crew.HUMAN_TRO_BA)) {
                critBonus += 1;
            }
        }

        HitData nextHit = null;

        // Some "hits" on a ProtoMek are actually misses.
        if ((entity instanceof ProtoMek proto) && (hit.getLocation() == ProtoMek.LOC_NEAR_MISS)) {
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

        // check for critical hit/miss vs. a BA
        if ((crits > 0) && (entity instanceof BattleArmor)) {
            // possible critical miss if the rerolled location isn't alive
            if ((hit.getLocation() >= entity.locations()) || (entity.getInternal(hit.getLocation()) <= 0)) {
                report = new Report(6037);
                report.add(hit.getLocation());
                report.subject = entityId;
                report.indent(2);
                reportVec.addElement(report);
                return reportVec;
            }
            // otherwise critical hit
            report = new Report(6225);
            report.add(entity.getLocationAbbr(hit));
            report.subject = entityId;
            report.indent(2);
            reportVec.addElement(report);
            baTookCrit = true;

            crits = 0;
            damage = Math.max(entity.getInternal(hit.getLocation()) + entity.getArmor(hit.getLocation()), damage);
        }

        if ((entity.getArmor(hit) > 0) &&
              ((entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_FERRO_FIBROUS) ||
                    (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_LIGHT_FERRO) ||
                    (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HEAVY_FERRO))) {
            isFerroFibrousTarget = true;
        }

        // Infantry with TSM implants get 2d6 burst damage from ATSM munitions
        if (damageType.equals(DamageType.ANTI_TSM) && entity.isConventionalInfantry() && entity.antiTSMVulnerable()) {
            Roll diceRoll = Compute.rollD6(2);
            report = new Report(6434);
            report.subject = entityId;
            report.add(diceRoll);
            report.indent(2);
            reportVec.addElement(report);
            damage += diceRoll.getIntValue();
        }

        // area effect against infantry is double damage
        if (isPlatoon && areaSatArty) {
            // PBI. Double damage.
            damage *= 2;
            report = new Report(6039);
            report.subject = entityId;
            report.indent(2);
            reportVec.addElement(report);
        }

        // Handle damage type effects (flechette, fragmentation, etc.) before situational modifiers
        switch (damageType) {
            case FRAGMENTATION:
                // Fragmentation missiles deal full damage to conventional
                // infantry
                // (only) and no damage to other target types.
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
                // Fléchette ammo deals full damage to conventional infantry and
                // half damage to other targets (including battle armor).
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
                if (isFerroFibrousTarget || reactiveArmor || reflectiveArmor || ferroLamellorArmor || bar5) {
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
                // Incendiary AC ammo does +2 damage to unarmoured infantry
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

        // Is the infantry in the open?
        if (ServerHelper.infantryInOpen(entity,
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
        boolean platoonOrBattleArmor = isPlatoon || isBattleArmor;
        if (platoonOrBattleArmor &&
              !entity.isDestroyed() &&
              !entity.isDoomed() &&
              game.getPlanetaryConditions().getAtmosphere().isLighterThan(Atmosphere.THIN)) {
            // PBI. Double damage.
            damage *= 2;
            report = new Report(6041);
            report.subject = entityId;
            report.indent(2);
            reportVec.addElement(report);
        }

        // adjust VTOL rotor damage
        if ((entity instanceof VTOL) &&
              (hit.getLocation() == VTOL.LOC_ROTOR) &&
              (hit.getGeneralDamageType() != HitData.DAMAGE_PHYSICAL) &&
              !game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_FULL_ROTOR_HITS)) {
            damage = (damage + 9) / 10;
        }

        // save EI status, in case sensors crit destroys it
        final boolean eiStatus = entity.hasActiveEiCockpit();
        // BA using EI implants receive +1 damage from attacks
        if (!(entity instanceof Mek) && !(entity instanceof ProtoMek) && eiStatus) {
            damage += 1;
        }

        // check for case on Aerospace
        if (entity instanceof Aero a) {
            if (ammoExplosion && a.hasCase()) {
                // damage should be reduced by a factor of 2 for ammo explosions
                // according to p. 161, TW
                damage /= 2;
                report = new Report(9010);
                report.subject = entityId;
                report.add(damage);
                report.indent(3);
                reportVec.addElement(report);
            }
        }

        // infantry armor can reduce damage
        if (isPlatoon && (((Infantry) entity).calcDamageDivisor() != 1.0)) {
            report = new Report(6074);
            report.subject = entityId;
            report.indent(2);
            report.add(damage);
            damage = (int) Math.ceil((damage) / ((Infantry) entity).calcDamageDivisor());
            report.add(damage);
            reportVec.addElement(report);
        }

        // Allocate the damage
        while (damage > 0) {

            // damage some cargo if we're taking damage
            // maybe move past "exterior passenger" check
            if (!ammoExplosion) {
                int damageLeftToCargo = damage;

                for (ICarryable cargo : entity.getDistinctCarriedObjects()) {
                    if (cargo.isInvulnerable()) {
                        continue;
                    }

                    double tonnage = cargo.getTonnage();
                    boolean cargoDestroyed = cargo.damage(damageLeftToCargo);
                    damageLeftToCargo -= (int) Math.ceil(tonnage);

                    // if we have destroyed the cargo, remove it, add a report
                    // and move on to the next piece of cargo
                    if (cargoDestroyed) {
                        entity.dropCarriedObject(cargo, false);

                        report = new Report(6721);
                        report.subject = entityId;
                        report.indent(2);
                        report.add(cargo.generalName());
                        reportVec.addElement(report);
                        // we have not destroyed the cargo means there is no damage left
                        // report and stop destroying cargo
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

            // first check for ammo explosions on aerospace separately, because it
            // must be done before
            // standard to capital damage conversions
            if ((entity instanceof Aero) && (hit.getLocation() == Aero.LOC_AFT) && !damageIS) {
                for (Mounted<?> mAmmo : entity.getAmmo()) {
                    if (mAmmo.isDumping() &&
                          !mAmmo.isDestroyed() &&
                          !mAmmo.isHit() &&
                          !(mAmmo.getType() instanceof BombType)) {
                        // doh. explode it
                        reportVec.addAll(manager.explodeEquipment(entity, mAmmo.getLocation(), mAmmo));
                        mAmmo.setHit(true);
                    }
                }
            }

            if (entity.isAero()) {
                // chance of a critical if damage exceeds threshold
                IAero a = (IAero) entity;
                if (threshDamage > a.getThresh(hit.getLocation())) {
                    critThresh = true;
                    a.setCritThresh(true);
                }
            }

            // Capital fighters receive damage differently
            if (entity.isCapitalFighter()) {
                IAero a = (IAero) entity;
                a.setCurrentDamage(a.getCurrentDamage() + damage);
                a.setCapArmor(a.getCapArmor() - damage);
                report = new Report(9065);
                report.subject = entityId;
                report.indent(2);
                report.newlines = 0;
                report.addDesc(entity);
                report.add(damage);
                reportVec.addElement(report);
                report = new Report(6085);
                report.subject = entityId;
                report.add(Math.max(a.getCapArmor(), 0));
                reportVec.addElement(report);
                // check to see if this destroyed the entity
                if (a.getCapArmor() <= 0) {
                    // Lets auto-eject if we can!
                    if (a instanceof LandAirMek lam) {
                        // LAMs eject if the CT destroyed switch is on
                        if (lam.isAutoEject() &&
                              (!game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) ||
                                    (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) &&
                                          lam.isCondEjectCTDest()))) {
                            reportVec.addAll(manager.ejectEntity(entity, true, false));
                        }
                    } else {
                        // Aerospace eject if the SI Destroyed switch is on
                        Aero aero = (Aero) a;
                        if (aero.isAutoEject() &&
                              (!game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) ||
                                    (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) &&
                                          aero.isCondEjectSIDest()))) {
                            reportVec.addAll(manager.ejectEntity(entity, true, false));
                        }
                    }
                    reportVec.addAll(manager.destroyEntity(entity, "Structural Integrity Collapse"));
                    a.doDisbandDamage();
                    a.setCapArmor(0);
                    if (hit.getAttackerId() != Entity.NONE) {
                        manager.creditKill(entity, game.getEntity(hit.getAttackerId()));
                    }
                }
                // check for aero crits from natural 12 or threshold; LAMs take damage as meks
                if (entity instanceof Aero) {
                    manager.checkAeroCrits(reportVec,
                          (Aero) entity,
                          hit,
                          damage_orig,
                          critThresh,
                          critSI,
                          ammoExplosion,
                          nukeS2S);
                }
                return reportVec;
            }

            if (!((entity instanceof Aero) && ammoExplosion)) {
                // report something different for Aero ammo explosions
                report = new Report(6065);
                report.subject = entityId;
                report.indent(2);
                report.addDesc(entity);
                report.add(damage);
                if (damageIS) {
                    report.messageId = 6070;
                }
                report.add(entity.getLocationAbbr(hit));
                reportVec.addElement(report);
            }

            // was the section destroyed earlier this phase?
            if (entity.getInternal(hit) == IArmorState.ARMOR_DOOMED) {
                // cannot transfer a through armor crit if so
                crits = 0;
            }

            // here goes the fun :)
            // Shields take damage first then cowls then armor whee
            // Shield does not protect from ammo explosions or falls.
            if (!ammoExplosion &&
                  !hit.isFallDamage() &&
                  !damageIS &&
                  entity.hasShield() &&
                  ((hit.getEffect() & HitData.EFFECT_NO_CRITICAL_SLOTS) != HitData.EFFECT_NO_CRITICAL_SLOTS)) {
                Mek me = (Mek) entity;
                int damageNew = me.shieldAbsorptionDamage(damage, hit.getLocation(), hit.isRear());
                // if a shield absorbed the damage then lets tell the world
                // about it.
                if (damageNew != damage) {
                    int absorb = damage - damageNew;
                    entity.damageThisPhase += absorb;
                    damage = damageNew;

                    report = new Report(3530);
                    report.subject = entityId;
                    report.indent(3);
                    report.add(absorb);
                    reportVec.addElement(report);

                    if (damage <= 0) {
                        crits = 0;
                        specCrits = 0;
                        isHeadHit = false;
                    }
                }
            }

            // Armored Cowl may absorb some damage from hit
            if ((entity instanceof Mek targetMek) &&
                  targetMek.hasCowl() &&
                  (hit.getLocation() == Mek.LOC_HEAD) &&
                  ((targetMek.getPosition() == null) ||
                        (attacker == null) ||
                        !targetMek.getPosition().isOnHexRow(targetMek.getSecondaryFacing(), attacker.getPosition()))) {
                int excessDamage = targetMek.damageCowl(damage);
                int blockedByCowl = damage - excessDamage;
                report = new Report(3520).subject(entityId).indent(3).add(blockedByCowl);
                reportVec.addElement(report);
                targetMek.damageThisPhase += blockedByCowl;
                damage = excessDamage;
            }

            // So might modular armor, if the location mounts any.
            if (!ammoExplosion &&
                  !damageIS &&
                  ((hit.getEffect() & HitData.EFFECT_NO_CRITICAL_SLOTS) != HitData.EFFECT_NO_CRITICAL_SLOTS)) {
                int damageNew = entity.getDamageReductionFromModularArmor(hit, damage, reportVec);
                int damageDiff = damage - damageNew;
                entity.damageThisPhase += damageDiff;
                damage = damageNew;
            }

            // Destroy searchlights on 7+ (torso hits on meks)
            if (entity.hasSearchlight()) {
                boolean spotlightHittable = isSpotlightHittable(entity, hit);
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
                        entity.destroyOneSearchlight();
                    }
                }
            }

            // Does an exterior passenger absorb some of the damage?
            if (!damageIS) {
                int nLoc = hit.getLocation();
                Entity passenger = entity.getExteriorUnitAt(nLoc, hit.isRear());
                // Does an exterior passenger absorb some of the damage?
                if (!ammoExplosion &&
                      (null != passenger) &&
                      !passenger.isDoomed() &&
                      (damageType != DamageType.IGNORE_PASSENGER)) {
                    damage = manager.damageExternalPassenger(entity, hit, damage, reportVec, passenger);
                }

                boolean bTorso = (nLoc == Mek.LOC_CENTER_TORSO) || (nLoc == Mek.LOC_RIGHT_TORSO) || (nLoc
                      == Mek.LOC_LEFT_TORSO);

                // Does a swarming unit absorb damage?
                int swarmer = entity.getSwarmAttackerId();
                if ((!(entity instanceof Mek) || bTorso) &&
                      (swarmer != Entity.NONE) &&
                      ((hit.getEffect() & HitData.EFFECT_CRITICAL) == 0) &&
                      (Compute.d6() >= 5) &&
                      (damageType != DamageType.IGNORE_PASSENGER) &&
                      !ammoExplosion) {
                    Entity swarm = game.getEntity(swarmer);
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
                    } while ((damage > absorb) && (nextPassHit.getLocation() >= 0));

                    // Damage the swarm.
                    int absorbedDamage = Math.min(damage, absorb);
                    Vector<Report> newReports = damageEntity(new DamageInfo(swarm, passHit, absorbedDamage));
                    for (Report newReport : newReports) {
                        newReport.indent(2);
                    }
                    reportVec.addAll(newReports);

                    // Did some damage pass on?
                    if (damage > absorb) {
                        // Yup. Remove the absorbed damage.
                        damage -= absorb;
                        report = new Report(6080);
                        report.subject = entityId;
                        report.indent(2);
                        report.add(damage);
                        report.addDesc(entity);
                        reportVec.addElement(report);
                    } else {
                        // Nope. Return our description.
                        return reportVec;
                    }
                }

                // is this a mek/tank dumping ammo being hit in the rear torso?
                if (((entity instanceof Mek) && hit.isRear() && bTorso) ||
                      ((entity instanceof Tank) &&
                            (hit.getLocation() ==
                                  (entity instanceof SuperHeavyTank ? SuperHeavyTank.LOC_REAR : Tank.LOC_REAR)))) {
                    for (Mounted<?> mAmmo : entity.getAmmo()) {
                        if (mAmmo.isDumping() && !mAmmo.isDestroyed() && !mAmmo.isHit()) {
                            // doh. explode it
                            reportVec.addAll(manager.explodeEquipment(entity, mAmmo.getLocation(), mAmmo));
                            mAmmo.setHit(true);
                        }
                    }
                }
            }
            // is there armor in the location hit?
            if (!ammoExplosion && (entity.getArmor(hit) > 0) && !damageIS) {
                int tmpDamageHold = -1;
                int origDamage = damage;

                if (isPlatoon) {
                    // infantry armour works differently
                    int armor = entity.getArmor(hit);
                    int men = entity.getInternal(hit);
                    tmpDamageHold = damage % 2;
                    damage /= 2;
                    if ((tmpDamageHold == 1) && (armor >= men)) {
                        // extra 1 point of damage to armor
                        tmpDamageHold = damage;
                        damage++;
                    } else {
                        // extra 0 or 1 point of damage to men
                        tmpDamageHold += damage;
                    }
                    // If the target has Ferro-Lamellor armor, we need to adjust
                    // damage. (4/5ths rounded down),
                    // Also check to eliminate crit chances for damage reduced
                    // to 0
                } else if (ferroLamellorArmor &&
                      (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING) &&
                      (hit.getGeneralDamageType() != HitData.DAMAGE_ARMOR_PIERCING_MISSILE) &&
                      (hit.getGeneralDamageType() != HitData.DAMAGE_IGNORES_DMG_REDUCTION) &&
                      (hit.getGeneralDamageType() != HitData.DAMAGE_AX)) {
                    tmpDamageHold = damage;
                    damage = (int) Math.floor((((double) damage) * 4) / 5);
                    if (damage <= 0) {
                        isHeadHit = false;
                        crits = 0;
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
                            // AX doesn't affect
                            // ballistic-reinforced armor,
                            // TO:AUE (6th), pg. 179
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
                    tmpDamageHold = damage;
                    damage -= (int) Math.ceil((double) damage / 3);
                    damage = Math.max(1, damage);
                    report = new Report(6089);
                    report.subject = entityId;
                    report.indent(3);
                    report.add(damage);
                    reportVec.addElement(report);
                } else if (reflectiveArmor &&
                      (hit.getGeneralDamageType() == HitData.DAMAGE_PHYSICAL) &&
                      !isBattleArmor) { // BA reflec does not receive extra physical damage
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
                    tmpDamageHold = damage; // BA reflec does not receive extra AE damage
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
                } else if (heatArmor && hit.getHeatWeapon() && game.getOptions()
                      .booleanOption(OptionsConstants.PLAYTEST_3)) {
                    // PLAYTEST3 only applies if heat_weapon is true in hitdata, which can only occur when playtest 
                    // is on.
                    tmpDamageHold = damage;
                    damage = (int) Math.ceil((((double) damage) / 2));
                    if (tmpDamageHold == 1) {
                        damage = 1;
                    }
                    report = new Report(6093);
                    report.subject = entityId;
                    report.indent(3);
                    report.add(damage);
                    reportVec.addElement(report);
                }

                // if there's a mast mount in the rotor, it and all other
                // equipment
                // on it get destroyed
                if ((entity instanceof VTOL) &&
                      (hit.getLocation() == VTOL.LOC_ROTOR) &&
                      entity.hasWorkingMisc(MiscType.F_MAST_MOUNT, null, VTOL.LOC_ROTOR) &&
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
                        critBonus -= 2;
                    }

                    if (tmpDamageHold >= 0) {
                        entity.damageThisPhase += tmpDamageHold;
                    } else {
                        entity.damageThisPhase += damage;
                    }
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

                    // telemissiles are destroyed if they lose all armor
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
                    if (entity.isBuildingEntityOrGunEmplacement()) {
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

                // targets with BAR armor get crits, depending on damage and BAR
                // rating
                if (entity.hasBARArmor(hit.getLocation())) {
                    if (origDamage > entity.getBARRating(hit.getLocation())) {
                        if (entity.hasArmoredChassis()) {
                            // crit roll with -1 mod
                            reportVec.addAll(manager.criticalEntity(entity,
                                  hit.getLocation(),
                                  hit.isRear(),
                                  -1 + critBonus,
                                  damage_orig));
                        } else {
                            reportVec.addAll(manager.criticalEntity(entity,
                                  hit.getLocation(),
                                  hit.isRear(),
                                  critBonus,
                                  damage_orig));
                        }
                    }
                }

                if ((tmpDamageHold > 0) && isPlatoon) {
                    damage = tmpDamageHold;
                }
            }

            // is there damage remaining?
            if (damage > 0) {

                // if this is an Aero then I need to apply internal damage
                // to the SI after halving it. Return from here to prevent
                // further processing
                if (entity instanceof Aero a) {

                    // check for large craft ammo explosions here: damage vented through armor,
                    // excess
                    // dissipating, much like Tank CASE.
                    if (ammoExplosion && entity.isLargeCraft()) {
                        entity.damageThisPhase += damage;
                        report = new Report(6128);
                        report.subject = entityId;
                        report.indent(2);
                        report.add(damage);
                        int loc = hit.getLocation();
                        // Roll for broadside weapons so fore/aft side armor facing takes the damage
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
                        report.add(entity.getLocationAbbr(loc));
                        reportVec.add(report);
                        if (damage > entity.getArmor(loc)) {
                            entity.setArmor(IArmorState.ARMOR_DESTROYED, loc);
                            report = new Report(6090);
                        } else {
                            entity.setArmor(entity.getArmor(loc) - damage, loc);
                            report = new Report(6085);
                            report.add(entity.getArmor(loc));
                        }
                        report.subject = entityId;
                        report.indent(3);
                        reportVec.add(report);
                        damage = 0;
                    }

                    // check for overpenetration
                    if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_OVER_PENETRATE)) {
                        int opRoll = Compute.d6(1);
                        if (((entity instanceof Jumpship) && !(entity instanceof Warship) && (opRoll > 3)) ||
                              ((entity instanceof Dropship) && (opRoll > 4)) ||
                              ((entity instanceof Warship) && (a.getOSI() <= 30) && (opRoll > 5))) {
                            // over-penetration happened
                            report = new Report(9090);
                            report.subject = entityId;
                            report.newlines = 0;
                            reportVec.addElement(report);
                            int new_loc = a.getOppositeLocation(hit.getLocation());
                            damage = Math.min(damage, entity.getArmor(new_loc));
                            // We don't want to deal negative damage
                            damage = Math.max(damage, 0);
                            report = new Report(6065);
                            report.subject = entityId;
                            report.indent(2);
                            report.newlines = 0;
                            report.addDesc(entity);
                            report.add(damage);
                            report.add(entity.getLocationAbbr(new_loc));
                            reportVec.addElement(report);
                            entity.setArmor(entity.getArmor(new_loc) - damage, new_loc);
                            if ((entity instanceof Warship) || (entity instanceof Dropship)) {
                                damage = 2;
                            } else {
                                damage = 0;
                            }
                        }
                    }

                    // divide damage in half
                    // do not divide by half if it is an ammo explosion
                    // Minimum SI damage is now 1 (per errata:
                    // https://bg.battletech.com/forums/index.php?topic=81913.0 )
                    if (!ammoExplosion &&
                          !nukeS2S &&
                          !game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
                        damage = (int) Math.round(damage / 2.0);
                        critSI = true;
                    }

                    // Now apply damage to the structural integrity
                    a.setSI(a.getSI() - damage);
                    entity.damageThisPhase += damage;
                    // send the report
                    report = new Report(1210);
                    report.subject = entityId;
                    report.newlines = 1;
                    if (!ammoExplosion) {
                        report.messageId = 9005;
                    }
                    // Only for fighters
                    if (ammoExplosion && !a.isLargeCraft()) {
                        report.messageId = 9006;
                    }
                    report.add(damage);
                    report.add(Math.max(a.getSI(), 0));
                    reportVec.addElement(report);
                    // check to see if this would destroy the ASF
                    if (a.getSI() <= 0) {
                        // Lets auto-eject if we can!
                        if (a.isAutoEject() &&
                              (!game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) ||
                                    (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) &&
                                          a.isCondEjectSIDest()))) {
                            reportVec.addAll(manager.ejectEntity(entity, true, false));
                        } else {
                            reportVec.addAll(manager.destroyEntity(entity,
                                  "Structural Integrity Collapse",
                                  damageType != DamageType.CRASH));
                        }
                        a.setSI(0);
                        if (hit.getAttackerId() != Entity.NONE) {
                            manager.creditKill(a, game.getEntity(hit.getAttackerId()));
                        }
                    }
                    manager.checkAeroCrits(reportVec, a, hit, damage_orig, critThresh, critSI, ammoExplosion, nukeS2S);
                    return reportVec;
                }

                // Check for CASE II right away. if so reduce damage to 1
                // and let it hit the IS.
                // Also remove as much of the rear armor as allowed by the
                // damage. If arm/leg/head
                // Then they lose all their armor if its less then the
                // explosion damage.
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
                        hit.setEffect(HitData.EFFECT_NO_CRITICAL_SLOTS);
                    }
                }
                // check for tank CASE here: damage to rear armor, excess
                // dissipating, and a crew stunned crit
                if (ammoExplosion && (entity instanceof Tank) && entity.locationHasCase(Tank.LOC_BODY)) {
                    entity.damageThisPhase += damage;
                    report = new Report(6124);
                    report.subject = entityId;
                    report.indent(2);
                    report.add(damage);
                    reportVec.add(report);
                    int loc = (entity instanceof SuperHeavyTank) ?
                          SuperHeavyTank.LOC_REAR :
                          (entity instanceof LargeSupportTank) ? LargeSupportTank.LOC_REAR : Tank.LOC_REAR;
                    if (damage > entity.getArmor(loc)) {
                        entity.setArmor(IArmorState.ARMOR_DESTROYED, loc);
                        report = new Report(6090);
                    } else {
                        entity.setArmor(entity.getArmor(loc) - damage, loc);
                        report = new Report(6085);
                        report.add(entity.getArmor(loc));
                    }
                    report.subject = entityId;
                    report.indent(3);
                    reportVec.add(report);
                    damage = 0;
                    int critIndex;
                    if (((Tank) entity).isCommanderHit() && ((Tank) entity).isDriverHit()) {
                        critIndex = Tank.CRIT_CREW_KILLED;
                    } else {
                        critIndex = Tank.CRIT_CREW_STUNNED;
                    }
                    reportVec.addAll(manager.applyCriticalHit(entity,
                          Entity.NONE,
                          new CriticalSlot(0, critIndex),
                          true,
                          0,
                          false));
                }

                // is there internal structure in the location hit?
                if (entity.getInternal(hit) > 0) {

                    // Now we need to consider alternate structure types!
                    int tmpDamageHold = -1;
                    if ((entity instanceof Mek) && ((Mek) entity).hasCompositeStructure()) {
                        tmpDamageHold = damage;
                        damage *= 2;
                        report = new Report(6091);
                        report.subject = entityId;
                        report.indent(3);
                        reportVec.add(report);
                    }
                    if ((entity instanceof Mek) && ((Mek) entity).hasReinforcedStructure()) {
                        tmpDamageHold = damage;
                        damage /= 2;
                        damage += tmpDamageHold % 2;
                        report = new Report(6092);
                        report.subject = entityId;
                        report.indent(3);
                        reportVec.add(report);
                    }
                    if ((entity.getInternal(hit) > damage) && (damage > 0)) {
                        // internal structure absorbs all damage
                        entity.setInternal(entity.getInternal(hit) - damage, hit);
                        // Triggers a critical hit on Vehicles and Meks.
                        if (!isPlatoon && !isBattleArmor) {
                            crits++;
                        }
                        tookInternalDamage = true;
                        // Alternate structures don't affect our damage total
                        // for later PSR purposes, so use the previously stored
                        // value here as necessary.
                        entity.damageThisPhase += (tmpDamageHold > -1) ? tmpDamageHold : damage;
                        damage = 0;
                        report = new Report(6100);
                        report.subject = entityId;
                        report.indent(3);
                        // Infantry platoons have men not "Internals".
                        if (isPlatoon) {
                            report.messageId = 6095;
                        }
                        report.add(entity.getInternal(hit));
                        reportVec.addElement(report);
                    } else if (damage > 0) {
                        // Triggers a critical hit on Vehicles and Meks.
                        if (!isPlatoon && !isBattleArmor) {
                            crits++;
                        }
                        // damage transfers, maybe
                        int absorbed = Math.max(entity.getInternal(hit), 0);

                        // Handle ProtoMek pilot damage
                        // due to location destruction
                        if (entity instanceof ProtoMek) {
                            int hits = ProtoMek.POSSIBLE_PILOT_DAMAGE[hit.getLocation()] -
                                  ((ProtoMek) entity).getPilotDamageTaken(hit.getLocation());
                            if (hits > 0) {
                                reportVec.addAll(manager.damageCrew(entity, hits));
                                ((ProtoMek) entity).setPilotDamageTaken(hit.getLocation(),
                                      ProtoMek.POSSIBLE_PILOT_DAMAGE[hit.getLocation()]);
                            }
                        }

                        // Platoon, Trooper, or Section destroyed message
                        report = new Report(1210);
                        report.subject = entityId;
                        if (isPlatoon) {
                            // Infantry have only one section, and
                            // are therefore destroyed.
                            if (((Infantry) entity).isSquad()) {
                                report.messageId = 6106; // Squad Killed
                            } else {
                                report.messageId = 6105; // Platoon Killed
                            }
                        } else if (isBattleArmor) {
                            report.messageId = 6110;
                        } else {
                            report.messageId = 6115;
                        }
                        report.indent(3);
                        reportVec.addElement(report);

                        // If a side torso got destroyed, and the
                        // corresponding arm is not yet destroyed, add
                        // it as a club to that hex (p.35 BMRr)
                        if ((entity instanceof Mek) &&
                              (((hit.getLocation() == Mek.LOC_RIGHT_TORSO) && (entity.getInternal(Mek.LOC_RIGHT_ARM)
                                    > 0)) ||
                                    ((hit.getLocation() == Mek.LOC_LEFT_TORSO) && (entity.getInternal(Mek.LOC_LEFT_ARM)
                                          > 0)))) {
                            int blownOffLocation;
                            if (hit.getLocation() == Mek.LOC_RIGHT_TORSO) {
                                blownOffLocation = Mek.LOC_RIGHT_ARM;
                            } else {
                                blownOffLocation = Mek.LOC_LEFT_ARM;
                            }
                            entity.destroyLocation(blownOffLocation, true);
                            report = new Report(6120);
                            report.subject = entityId;
                            report.add(entity.getLocationName(blownOffLocation));
                            reportVec.addElement(report);
                            Hex h = game.getBoard().getHex(entity.getPosition());
                            if (null != h) {
                                if (entity instanceof BipedMek) {
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
                                manager.sendChangedHex(entity.getPosition());
                            }
                        }

                        // Troopers riding on a location
                        // all die when the location is destroyed.
                        if ((entity instanceof Mek) || (entity instanceof Tank)) {
                            Entity passenger = entity.getExteriorUnitAt(hit.getLocation(), hit.isRear());
                            if ((null != passenger) && !passenger.isDoomed()) {
                                HitData passHit = passenger.getTrooperAtLocation(hit, entity);
                                // ensures a kill
                                passHit.setEffect(HitData.EFFECT_CRITICAL);
                                if (passenger.getInternal(passHit) > 0) {
                                    reportVec.addAll(damageEntity(new DamageInfo(passenger, passHit, damage)));
                                }
                                passHit = new HitData(hit.getLocation(), !hit.isRear());
                                passHit = passenger.getTrooperAtLocation(passHit, entity);
                                // ensures a kill
                                passHit.setEffect(HitData.EFFECT_CRITICAL);
                                if (passenger.getInternal(passHit) > 0) {
                                    reportVec.addAll(damageEntity(new DamageInfo(passenger, passHit, damage)));
                                }
                            }
                        }

                        // BA inferno explosions
                        if (entity instanceof BattleArmor) {
                            int infernos = 0;
                            for (Mounted<?> m : entity.getEquipment()) {
                                if (m.getType() instanceof AmmoType at) {
                                    if (((at.getAmmoType() == AmmoType.AmmoTypeEnum.SRM) ||
                                          (at.getAmmoType() == AmmoType.AmmoTypeEnum.MML)) &&
                                          (at.getMunitionType().contains(AmmoType.Munitions.M_INFERNO))) {
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
                                    Coords c = entity.getPosition();
                                    if (c == null) {
                                        Entity transport = game.getEntity(entity.getTransportId());
                                        if (transport != null) {
                                            c = transport.getPosition();
                                        }
                                        manager.getMainPhaseReport().addAll(manager.deliverInfernoMissiles(entity,
                                              entity,
                                              infernos));
                                    }
                                    if (c != null) {
                                        manager.getMainPhaseReport().addAll(manager.deliverInfernoMissiles(entity,
                                              new HexTarget(c, Targetable.TYPE_HEX_ARTILLERY),
                                              infernos));
                                    }
                                }
                            }
                        }

                        // Mark off the internal structure here, but *don't*
                        // destroy the location just yet -- there are checks
                        // still to run!
                        entity.setInternal(0, hit);
                        entity.damageThisPhase += absorbed;
                        damage -= absorbed;

                        // Now we need to consider alternate structure types!
                        if (tmpDamageHold > 0) {
                            if (((Mek) entity).hasCompositeStructure()) {
                                // If there's a remainder, we can actually
                                // ignore it.
                                damage /= 2;
                            } else if (((Mek) entity).hasReinforcedStructure()) {
                                damage *= 2;
                                damage -= tmpDamageHold % 2;
                            }
                        }
                    }
                }
                if (entity.getInternal(hit) <= 0) {
                    // internal structure is gone, what are the transfer
                    // potentials?
                    nextHit = entity.getTransferLocation(hit);
                    if (nextHit.getLocation() == Entity.LOC_DESTROYED) {
                        if (entity instanceof Mek) {
                            // Start with the number of engine crits in this
                            // location, if any...
                            entity.engineHitsThisPhase += entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                                  Mek.SYSTEM_ENGINE,
                                  hit.getLocation());
                            // ...then deduct the ones destroyed previously or
                            // critically
                            // hit this round already. That leaves the ones
                            // actually
                            // destroyed with the location.
                            entity.engineHitsThisPhase -= entity.getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                                  Mek.SYSTEM_ENGINE,
                                  hit.getLocation());
                        }

                        boolean engineExploded = manager.checkEngineExplosion(entity,
                              reportVec,
                              entity.engineHitsThisPhase);

                        if (!engineExploded) {
                            // Entity destroyed. Ammo explosions are
                            // neither survivable nor salvageable.
                            // Only ammo explosions in the CT are devastating.
                            reportVec.addAll(manager.destroyEntity(entity,
                                  "damage",
                                  !ammoExplosion,
                                  !((ammoExplosion || areaSatArty) &&
                                        ((entity instanceof Tank) ||
                                              ((entity instanceof Mek) && (hit.getLocation()
                                                    == Mek.LOC_CENTER_TORSO))))));
                            // If the head is destroyed, kill the crew.

                            if ((entity instanceof Mek mek) &&
                                  (hit.getLocation() == Mek.LOC_HEAD) &&
                                  !entity.getCrew().isDead() &&
                                  !entity.getCrew().isDoomed() &&
                                  game.getOptions()
                                        .booleanOption(OptionsConstants.ADVANCED_TAC_OPS_SKIN_OF_THE_TEETH_EJECTION)) {
                                if (mek.isAutoEject() &&
                                      (!game.getOptions()
                                            .booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) ||
                                            (game.getOptions()
                                                  .booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) &&
                                                  mek.isCondEjectHeadshot()))) {
                                    autoEject = true;
                                    reportVec.addAll(manager.ejectEntity(entity, true, true));
                                }
                            }

                            if ((entity instanceof Mek mek) &&
                                  (hit.getLocation() == Mek.LOC_CENTER_TORSO) &&
                                  !entity.getCrew().isDead() &&
                                  !entity.getCrew().isDoomed()) {
                                if (mek.isAutoEject() &&
                                      game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) &&
                                      mek.isCondEjectCTDest()) {
                                    if (mek.getCrew().getHits() < 5) {
                                        Report.addNewline(reportVec);
                                        mek.setDoomed(false);
                                        mek.setDoomed(true);
                                    }
                                    autoEject = true;
                                    reportVec.addAll(manager.ejectEntity(entity, true));
                                }
                            }

                            if ((hit.getLocation() == Mek.LOC_HEAD) ||
                                  ((hit.getLocation() == Mek.LOC_CENTER_TORSO) &&
                                        ((ammoExplosion && !autoEject) || areaSatArty))) {
                                entity.getCrew().setDoomed(true);
                            }
                            if (manager.shouldAutoEjectOnDestruction()) {
                                reportVec.addAll(manager.abandonEntity(entity));
                            }
                        }

                        // nowhere for further damage to go
                        damage = 0;
                    } else if (nextHit.getLocation() == Entity.LOC_NONE) {
                        // Rest of the damage is wasted.
                        damage = 0;
                    } else if (ammoExplosion && entity.locationHasCase(hit.getLocation())) {
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
                        report.add(entity.getLocationAbbr(nextHit));
                        reportVec.addElement(report);

                        // If there are split weapons in this location, mark it
                        // as hit, even if it took no criticalSlots.
                        for (WeaponMounted m : entity.getWeaponList()) {
                            if (m.isSplit()) {
                                if ((m.getLocation() == hit.getLocation()) ||
                                      (m.getLocation() == nextHit.getLocation())) {
                                    entity.setWeaponHit(m);
                                }
                            }
                        }
                        // if this is damage from a nail/rivet gun, and we
                        // transfer
                        // to a location that has armor, and BAR >=5, no damage
                        if ((damageType == DamageType.NAIL_RIVET) &&
                              (entity.getArmor(nextHit.getLocation()) > 0) &&
                              (entity.getBARRating(nextHit.getLocation()) >= 5)) {
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
                // PLAYTEST3 no penetrating crits with ABA, ferroLam doesn't prevent them
                if (game.getOptions().booleanOption(OptionsConstants.PLAYTEST_3)) {
                    if (!hardenedArmor && !abaArmor) {
                        specCrits++;
                    }
                } else {
                    if (!hardenedArmor && !ferroLamellorArmor && !reactiveArmor) {
                        specCrits++;
                    }
                }
            }
            // check for breaching
            reportVec.addAll(manager.breachCheck(entity, hit.getLocation(), null, underWater));

            // resolve special results
            if ((hit.getEffect() & HitData.EFFECT_VEHICLE_MOVE_DAMAGED) == HitData.EFFECT_VEHICLE_MOVE_DAMAGED) {
                reportVec.addAll(manager.vehicleMotiveDamage((Tank) entity, hit.getMotiveMod()));
            }
            // Damage from any source can break spikes
            if (entity.hasWorkingMisc(MiscType.F_SPIKES, null, hit.getLocation())) {
                reportVec.add(manager.checkBreakSpikes(entity, hit.getLocation()));
            }

            // roll all critical hits against this location
            // unless the section destroyed in a previous phase?
            // Cause a crit.
            if ((entity.getInternal(hit) != IArmorState.ARMOR_DESTROYED) &&
                  ((hit.getEffect() & HitData.EFFECT_NO_CRITICAL_SLOTS) != HitData.EFFECT_NO_CRITICAL_SLOTS)) {
                for (int i = 0; i < crits; i++) {
                    reportVec.addAll(manager.criticalEntity(entity,
                          hit.getLocation(),
                          hit.isRear(),
                          hit.glancingMod() + critBonus,
                          damage_orig,
                          damageType));
                }
                crits = 0;

                for (int i = 0; i < specCrits; i++) {
                    // against BAR or reflective armor, we get a +2 mod
                    int critMod = entity.hasBARArmor(hit.getLocation()) ? 2 : 0;
                    critMod += (reflectiveArmor && !isBattleArmor) ? 2 : 0; // BA
                    // against impact armor, we get a +1 mod
                    // PLAYTEST3 no longer has penalty for impact.
                    if (!game.getOptions().booleanOption(OptionsConstants.PLAYTEST_3)) {
                        critMod += impactArmor ? 1 : 0;
                    }
                    // hardened armour has no crit penalty
                    if (!hardenedArmor) {
                        // non-hardened armor gets modifiers
                        // the -2 for hardened is handled in the critBonus
                        // variable
                        critMod += hit.getSpecCritMod();
                        critMod += hit.glancingMod();
                    }
                    reportVec.addAll(manager.criticalEntity(entity,
                          hit.getLocation(),
                          hit.isRear(),
                          critMod + critBonus,
                          damage_orig));
                }
                specCrits = 0;
            }

            // resolve Aero crits
            if (entity instanceof Aero) {
                manager.checkAeroCrits(reportVec,
                      (Aero) entity,
                      hit,
                      damage_orig,
                      critThresh,
                      critSI,
                      ammoExplosion,
                      nukeS2S);
            }

            if (isHeadHit) {
                if (entity.hasAbility(OptionsConstants.MD_DERMAL_ARMOR)
                      || entity.hasAbility(OptionsConstants.MD_DERMAL_CAMO_ARMOR)) {
                    Report r = new Report(6651);
                    r.subject = entity.getId();
                    reportVec.add(r);
                } else {
                    Report.addNewline(reportVec);
                    reportVec.addAll(manager.damageCrew(entity, 1));
                }
            }

            // If the location has run out of internal structure, finally
            // actually
            // destroy it here. *EXCEPTION:* Aero units have 0 internal
            // structure
            // in every location by default and are handled elsewhere, so they
            // get a bye.
            if (!(entity instanceof Aero) && (entity.getInternal(hit) <= 0)) {
                entity.destroyLocation(hit.getLocation());

                // Check for possible engine destruction here
                if ((entity instanceof Mek) && ((hit.getLocation() == Mek.LOC_RIGHT_TORSO) || (hit.getLocation()
                      == Mek.LOC_LEFT_TORSO))) {

                    int numEngineHits = entity.getEngineHits();
                    boolean engineExploded = manager.checkEngineExplosion(entity, reportVec, numEngineHits);

                    int hitsToDestroy = 3;
                    if ((entity instanceof Mek) &&
                          entity.isSuperHeavy() &&
                          entity.hasEngine() &&
                          (entity.getEngine().getEngineType() == Engine.COMPACT_ENGINE)) {
                        hitsToDestroy = 2;
                    }

                    if (!engineExploded && (numEngineHits >= hitsToDestroy)) {
                        // third engine hit
                        reportVec.addAll(manager.destroyEntity(entity, "engine destruction"));
                        if (manager.shouldAutoEjectOnDestruction()) {
                            reportVec.addAll(manager.abandonEntity(entity));
                        }
                        entity.setSelfDestructing(false);
                        entity.setSelfDestructInitiated(false);
                    }

                    // Torso destruction in airborne LAM causes immediate crash.
                    if ((entity instanceof LandAirMek) && !entity.isDestroyed() && !entity.isDoomed()) {
                        report = new Report(9710);
                        report.subject = entity.getId();
                        report.addDesc(entity);
                        if (entity.isAirborneVTOLorWIGE()) {
                            reportVec.add(report);
                            manager.crashAirMek(entity,
                                  new PilotingRollData(entity.getId(),
                                        TargetRoll.AUTOMATIC_FAIL,
                                        "side torso destroyed"),
                                  reportVec);
                        } else if (entity.isAirborne() && entity.isAero()) {
                            reportVec.add(report);
                            reportVec.addAll(manager.processCrash(entity,
                                  ((IAero) entity).getCurrentVelocity(),
                                  entity.getPosition()));
                        }
                    }
                }

            }

            // If damage remains, loop to next location; if not, be sure to stop
            // here because we may need to refer back to the last *damaged*
            // location again later. (This is safe because at damage <= 0 the
            // loop terminates anyway.)
            if (damage > 0) {
                hit = nextHit;
                // Need to update armor status for the new location
                hardenedArmor = ((entity instanceof Mek) || (entity instanceof Tank)) &&
                      (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HARDENED);
                ferroLamellorArmor = ((entity instanceof Mek) || (entity instanceof Tank) || (entity instanceof Aero))
                      &&
                      (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_FERRO_LAMELLOR);
                reflectiveArmor = (((entity instanceof Mek) || (entity instanceof Tank) || (entity instanceof Aero)) &&
                      (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_REFLECTIVE)) ||
                      (isBattleArmor &&
                            (entity.getArmorType(hit.getLocation()) ==
                                  EquipmentType.T_ARMOR_BA_REFLECTIVE));
                reactiveArmor = (((entity instanceof Mek) || (entity instanceof Tank) || (entity instanceof Aero)) &&
                      (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_REACTIVE)) ||
                      (isBattleArmor &&
                            (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_BA_REACTIVE));
                ballisticArmor = ((entity instanceof Mek) || (entity instanceof Tank) || (entity instanceof Aero)) &&
                      (entity.getArmorType(hit.getLocation()) ==
                            EquipmentType.T_ARMOR_BALLISTIC_REINFORCED);
                impactArmor = (entity instanceof Mek) &&
                      (entity.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_IMPACT_RESISTANT);
            }
            if (damageIS) {
                wasDamageIS = true;
                damageIS = false;
            }
        }
        // Meks using EI implants take pilot damage each time a hit
        // inflicts IS damage
        if (tookInternalDamage
              && ((entity instanceof Mek) || (entity instanceof ProtoMek))
              && entity.hasActiveEiCockpit()) {
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

        // VDNI feedback on internal damage - Meks only (IO pg 71)
        // Per IO rules: Only Meks/IndustrialMeks get feedback on internal structure damage.
        // Vehicles get feedback on specific critical hits only (handled in applyTankCritical).
        // Fighters get feedback on any critical hit (handled in applyAeroCritical).
        // Battle Armor gets no feedback at all.
        // Proto DNI takes precedence and handles its own feedback separately.
        if (tookInternalDamage &&
              (entity instanceof Mek) &&
              entity.hasAbility(OptionsConstants.MD_VDNI) &&
              !entity.hasAbility(OptionsConstants.MD_BVDNI) &&
              !entity.hasAbility(OptionsConstants.MD_PROTO_DNI) &&
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
        } else if (tookInternalDamage &&
              (entity instanceof Mek) &&
              entity.hasAbility(OptionsConstants.MD_VDNI) &&
              !entity.hasAbility(OptionsConstants.MD_BVDNI) &&
              entity.hasAbility(OptionsConstants.MD_PAIN_SHUNT)) {
            // Pain Shunt blocks VDNI feedback - show message for clarity
            Report.addNewline(reportVec);
            report = new Report(3585);
            report.subject = entity.getId();
            report.addDesc(entity);
            report.indent(2);
            reportVec.add(report);
        }

        // Prototype DNI feedback on ANY damage (IO pg 83)
        // TN 6 for armor-only hits, TN 8 for internal damage or critical hits
        // Only applies to BattleMeks (not IndustrialMeks)
        if (tookAnyDamage &&
              (entity instanceof Mek) &&
              !entity.isIndustrialMek() &&
              entity.hasAbility(OptionsConstants.MD_PROTO_DNI) &&
              !entity.hasAbility(OptionsConstants.MD_PAIN_SHUNT)) {
            Report.addNewline(reportVec);
            Roll diceRoll = Compute.rollD6(2);
            int targetNumber = tookInternalDamage ? 8 : 6;
            report = new Report(3589);
            report.subject = entity.getId();
            report.indent(2);  // Indent BEFORE addDesc to suppress unit icon
            report.addDesc(entity);
            report.add(targetNumber - 1); // Display as "needs X or less"
            report.add(diceRoll);
            report.choose(diceRoll.getIntValue() >= targetNumber);
            reportVec.add(report);

            if (diceRoll.getIntValue() >= targetNumber) {
                reportVec.addAll(manager.damageCrew(entity, 1));
            }
        }

        // TacOps p.78 Ammo booms can hurt other units in same and adjacent hexes
        // But, this does not apply to CASE'd units, and it only applies if the
        // ammo explosion
        // destroyed the unit
        if (ammoExplosion &&
              game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_AMMUNITION)
              // For 'Meks we care whether there was CASE specifically in the
              // location that went boom...
              &&
              !(entity.locationHasCase(hit.getLocation()) || entity.hasCASEII(hit.getLocation()))
              // ...but vehicles and ASFs just have one CASE item for the
              // whole unit, so we need to look whether there's CASE anywhere
              // at all.
              &&
              !(((entity instanceof Tank) || (entity instanceof Aero)) && entity.hasCase()) &&
              (entity.isDestroyed() || entity.isDoomed()) &&
              (damage_orig > 0) &&
              ((damage_orig / 10) > 0)) {
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
            int[] damages = { (int) Math.floor(damage_orig / 10.0), (int) Math.floor(damage_orig / 20.0) };
            manager.doExplosion(damages, false, entity.getPosition(), 0, true, reportVec, null, 5, entity.getId(),
                  false, false);
            Report.addNewline(reportVec);
            report = new Report(5410, Report.PUBLIC);
            report.subject = entity.getId();
            report.indent(2);
            reportVec.add(report);
        }

        // This flag indicates the hit was directly to IS
        if (wasDamageIS) {
            Report.addNewline(reportVec);
        }

        // BA VDNI/BVDNI immunity feedback - track that crit happened (IO pg 71)
        // Actual message is printed after all attacks complete in handleAttacks()
        if (baTookCrit &&
              (entity.hasAbility(OptionsConstants.MD_VDNI) || entity.hasAbility(OptionsConstants.MD_BVDNI)) &&
              !entity.reportedVDNIFeedbackThisPhase) {
            entity.baVDNINeedsFeedbackMessage = true;
        }

        // Suicide Implant Reactive Damage (IO pg 83)
        // When conventional infantry with suicide implants loses troopers, they automatically
        // deal 0.57 damage per dead trooper to all opposing units in the hex
        if (checkSuicideImplantReaction && initialTroopers > 0) {
            int currentTroopers = Math.max(0, entity.getInternal(Infantry.LOC_INFANTRY));
            int deadTroopers = initialTroopers - currentTroopers;
            if (deadTroopers > 0) {
                reportVec.addAll(applySuicideImplantReaction(entity, deadTroopers));
            }
        }

        return reportVec;
    }

    /**
     * Applies suicide implant reactive damage when conventional infantry troopers are killed. Per IO pg 83:
     * "Conventional infantry equipped with suicide implants will also deliver an automatic 'attack' against all
     * opposing units in the same hex for every trooper they lose during the same attack."
     *
     * @param infantry     The infantry unit that lost troopers
     * @param deadTroopers The number of troopers killed
     *
     * @return Vector of reports describing the reactive damage
     */
    protected Vector<Report> applySuicideImplantReaction(Entity infantry, int deadTroopers) {
        Vector<Report> reports = new Vector<>();
        Coords position = infantry.getPosition();

        if (position == null) {
            return reports;
        }

        // Calculate damage: 0.57 per dead trooper
        int damage = (int) Math.round(deadTroopers * SuicideImplantsAttackAction.DAMAGE_PER_TROOPER);
        if (damage <= 0) {
            return reports;
        }

        // First, find all valid enemy targets in the hex
        List<Entity> validTargets = new ArrayList<>();
        for (Entity target : game.getEntitiesVector(position)) {
            // Skip the infantry itself
            if (target.getId() == infantry.getId()) {
                continue;
            }
            // Only damage opposing (enemy) units
            if (!target.isEnemyOf(infantry)) {
                continue;
            }
            // Skip already destroyed units
            if (target.isDestroyed() || target.isDoomed()) {
                continue;
            }
            validTargets.add(target);
        }

        // If no enemies in hex, reactive detonation has no effect - skip reporting
        if (validTargets.isEmpty()) {
            return reports;
        }

        // Report the reactive detonation
        Report report = new Report(4596);
        report.subject = infantry.getId();
        report.add(deadTroopers);
        report.add(damage);
        reports.add(report);

        // Apply damage to all valid enemy targets
        for (Entity target : validTargets) {
            // Report damage to this target
            Report targetReport = new Report(4583);
            targetReport.subject = target.getId();
            targetReport.indent(2);
            targetReport.add(target.getDisplayName());
            targetReport.add(damage);
            reports.add(targetReport);

            // Apply damage using SUICIDE_IMPLANT_REACTION type to prevent recursion
            HitData hit = new HitData(target.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT).getLocation());
            hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
            reports.addAll(damageEntity(target, hit, damage, false,
                  DamageType.SUICIDE_IMPLANT_REACTION, false, false, true, false, false, new Vector<>()));
        }

        return reports;
    }

    private static boolean isSpotlightHittable(Entity entity, HitData hit) {
        boolean spotlightHittable = true;
        int loc = hit.getLocation();
        if (entity instanceof Mek) {
            if ((loc != Mek.LOC_CENTER_TORSO) && (loc != Mek.LOC_LEFT_TORSO) && (loc != Mek.LOC_RIGHT_TORSO)) {
                spotlightHittable = false;
            }
        } else if (entity instanceof Tank) {
            if (entity instanceof SuperHeavyTank) {
                if ((loc != Tank.LOC_FRONT) &&
                      (loc != SuperHeavyTank.LOC_FRONT_RIGHT) &&
                      (loc != SuperHeavyTank.LOC_FRONT_LEFT) &&
                      (loc != SuperHeavyTank.LOC_REAR_RIGHT) &&
                      (loc != SuperHeavyTank.LOC_REAR_LEFT)) {
                    spotlightHittable = false;
                }
            } else if (entity instanceof LargeSupportTank) {
                if ((loc != Tank.LOC_FRONT) &&
                      (loc != LargeSupportTank.LOC_FRONT_RIGHT) &&
                      (loc != LargeSupportTank.LOC_FRONT_LEFT) &&
                      (loc != LargeSupportTank.LOC_REAR_RIGHT) &&
                      (loc != LargeSupportTank.LOC_REAR_LEFT)) {
                    spotlightHittable = false;
                }
            } else {
                if ((loc != Tank.LOC_FRONT) && (loc != Tank.LOC_RIGHT) && (loc != Tank.LOC_LEFT)) {
                    spotlightHittable = false;
                }
            }

        }
        return spotlightHittable;
    }
}
