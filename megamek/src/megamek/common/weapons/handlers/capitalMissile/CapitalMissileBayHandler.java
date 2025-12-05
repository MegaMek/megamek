/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers.capitalMissile;

import static java.lang.Math.floor;

import java.io.Serial;
import java.util.Vector;

import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Targetable;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.handlers.AmmoBayWeaponHandler;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.WeaponHandler;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jay Lawson
 */
public class CapitalMissileBayHandler extends AmmoBayWeaponHandler {
    private static final MMLogger LOGGER = MMLogger.create(CapitalMissileBayHandler.class);

    @Serial
    private static final long serialVersionUID = -1618484541772117621L;
    boolean advancedPD;

    /**
     *
     */
    public CapitalMissileBayHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
        advancedPD = g.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADV_POINT_DEFENSE);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.AttackHandler#handle(int, java.util.Vector)
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)) {
            return handleAeroSanity(phase, vPhaseReport);
        }

        int numAttacks = 1;

        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
              : null;

        if (entityTarget != null) {
            attackingEntity.setLastTarget(entityTarget.getId());
            attackingEntity.setLastTargetDisplayName(entityTarget.getDisplayName());
        }
        // Which building takes the damage?
        IBuilding bldg = game.getBoard().getBuildingAt(target.getPosition());
        String number = numWeapons > 1 ? " (" + numWeapons + ")" : "";
        for (int i = numAttacks; i > 0; i--) {
            // Report weapon attack and its to-hit value.
            Report report = new Report(3115);
            report.indent();
            report.newlines = 0;
            report.subject = subjectId;
            report.add(weaponType.getName() + number);
            if (entityTarget != null) {
                if ((weaponType.getAmmoType() != AmmoType.AmmoTypeEnum.NA)
                      && (weapon.getLinked() != null)
                      && (weapon.getLinked().getType() instanceof AmmoType ammoType)) {
                    if (!ammoType.getMunitionType().contains(AmmoType.Munitions.M_STANDARD)) {
                        report.messageId = 3116;
                        report.add(ammoType.getSubMunitionName());
                    }
                }
                report.addDesc(entityTarget);
            } else {
                report.messageId = 3120;
                report.add(target.getDisplayName(), true);
            }
            vPhaseReport.addElement(report);

            // Point Defense fire vs Capital Missiles

            // are we a glancing hit? Check for this here, report it later
            setGlancingBlowFlags(entityTarget);

            // Set Margin of Success/Failure and check for Direct Blows
            toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));
            bDirect = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DIRECT_BLOW)
                  && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);

            // This has to be up here so that we don't screw up glancing/direct blow reports
            attackValue = calcAttackValue();

            // CalcAttackValue triggers counterfire, so now we can safely get this
            CapMissileAMSMod = getCapMissileAMSMod();

            // Only do this if the missile wasn't destroyed
            if (CapMissileAMSMod > 0 && CapMissileArmor > 0) {
                toHit.addModifier(CapMissileAMSMod, "Damage from Point Defenses");
                if (roll.getIntValue() < toHit.getValue()) {
                    CapMissileMissed = true;
                }
            }

            // Report any AMS bay action against Capital missiles that doesn't destroy them
            // all.
            if (amsBayEngagedCap && CapMissileArmor > 0) {
                report = new Report(3358);
                report.add(CapMissileAMSMod);
                report.subject = subjectId;
                vPhaseReport.addElement(report);

                // Report any PD bay action against Capital missiles that doesn't destroy them
                // all.
            } else if (pdBayEngagedCap && CapMissileArmor > 0) {
                report = new Report(3357);
                report.add(CapMissileAMSMod);
                report.subject = subjectId;
                vPhaseReport.addElement(report);
            }

            if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                report = new Report(3135);
                report.subject = subjectId;
                report.add(" " + target.getPosition(), true);
                vPhaseReport.addElement(report);
                return false;
            } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                report = new Report(3140);
                report.newlines = 0;
                report.subject = subjectId;
                report.add(toHit.getDesc());
                vPhaseReport.addElement(report);
            } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
                report = new Report(3145);
                report.newlines = 0;
                report.subject = subjectId;
                report.add(toHit.getDesc());
                vPhaseReport.addElement(report);
            } else {
                // roll to hit
                report = new Report(3150);
                report.newlines = 0;
                report.subject = subjectId;
                report.add(toHit);
                vPhaseReport.addElement(report);
            }

            // dice have been rolled, thanks
            report = new Report(3155);
            report.newlines = 0;
            report.subject = subjectId;
            report.add(roll);
            vPhaseReport.addElement(report);

            // do we hit?
            bMissed = roll.getIntValue() < toHit.getValue();

            // Report Glancing/Direct Blow here because of Capital Missile weirdness
            if (!(amsBayEngagedCap || pdBayEngagedCap)) {
                addGlancingBlowReports(vPhaseReport);

                if (bDirect) {
                    report = new Report(3189);
                    report.subject = attackingEntity.getId();
                    report.newlines = 0;
                    vPhaseReport.addElement(report);
                }
            }

            CounterAV = getCounterAV();
            // use this if AMS counterfire destroys all the Capital missiles
            if (amsBayEngagedCap && (CapMissileArmor <= 0)) {
                report = new Report(3356);
                report.indent();
                report.subject = subjectId;
                vPhaseReport.addElement(report);
                bMissed = true;
            }
            // use this if PD counterfire destroys all the Capital missiles
            if (pdBayEngagedCap && (CapMissileArmor <= 0)) {
                report = new Report(3355);
                report.indent();
                report.subject = subjectId;
                vPhaseReport.addElement(report);
                bMissed = true;
            }

            // Any necessary PSRs, jam checks, etc.
            // If this boolean is true, don't report
            // the miss later, as we already reported
            // it in doChecks
            boolean missReported = doChecks(vPhaseReport);
            if (missReported) {
                bMissed = true;
            }
            if (bMissed && !missReported) {
                reportMiss(vPhaseReport);
            }
            // Handle damage.
            int nCluster = calculateNumCluster();
            int id = vPhaseReport.size();
            int hits = calcHits(vPhaseReport);

            if (target.isAirborne() || game.getBoard().isSpace() || attackingEntity.usesWeaponBays()) {
                // if we added a line to the phase report for calc hits, remove
                // it now
                while (vPhaseReport.size() > id) {
                    vPhaseReport.removeElementAt(vPhaseReport.size() - 1);
                }
                int[] aeroResults = calcAeroDamage(entityTarget, vPhaseReport);
                hits = aeroResults[0];
                // If our capital missile was destroyed, it shouldn't hit
                if ((amsBayEngagedCap || pdBayEngagedCap) && (CapMissileArmor <= 0)) {
                    hits = 0;
                }
                nCluster = aeroResults[1];
            }

            // Capital missiles shouldn't be able to target buildings, being space-only
            // weapons
            // but if they aren't defined, handleEntityDamage() doesn't work.
            int bldgAbsorbs = 0;

            // We have to adjust the reports on a miss, so they line up
            if (bMissed && id != vPhaseReport.size()) {
                vPhaseReport.get(id - 1).newlines--;
                vPhaseReport.get(id).indent(2);
                vPhaseReport.get(vPhaseReport.size() - 1).newlines++;
            }

            // Make sure the player knows when his attack causes no damage.
            if (nDamPerHit == 0) {
                report = new Report(3365);
                report.subject = subjectId;
                vPhaseReport.addElement(report);
                return false;
            }
            if (!bMissed && (entityTarget != null)) {
                handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                      nCluster, bldgAbsorbs);
                gameManager.creditKill(entityTarget, attackingEntity);
            } else if (!bMissed) { // Hex is targeted, need to report a hit
                report = new Report(3390);
                report.subject = subjectId;
                vPhaseReport.addElement(report);
            }
        }
        Report.addNewline(vPhaseReport);
        return false;
    }

    @Override
    protected int calcAttackValue() {

        double av = 0;
        double counterAV = calcCounterAV();
        int armor = 0;
        int weaponArmor;
        int range = RangeType.rangeBracket(nRange, weaponType.getATRanges(), true, false);

        for (WeaponMounted bayW : weapon.getBayWeapons()) {
            // check the currently loaded ammo
            AmmoMounted bayWAmmo = bayW.getLinkedAmmo();
            if (null == bayWAmmo || bayWAmmo.getUsableShotsLeft() < 1) {
                // try loading something else
                attackingEntity.loadWeaponWithSameAmmo(bayW);
                bayWAmmo = bayW.getLinkedAmmo();
            }
            if (!bayW.isBreached()
                  && !bayW.isDestroyed()
                  && !bayW.isJammed()
                  && bayWAmmo != null
                  && attackingEntity.getTotalAmmoOfType(bayWAmmo.getType()) >= bayW.getCurrentShots()) {
                WeaponType bayWType = bayW.getType();
                // need to cycle through weapons and add av
                double current_av = 0;

                AmmoType ammoType = bayWAmmo.getType();
                if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                      && (ammoType.hasFlag(AmmoType.F_AR10_KILLER_WHALE)
                      || ammoType.hasFlag(AmmoType.F_PEACEMAKER))) {
                    weaponArmor = 40;
                } else if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                      && (ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK)
                      || ammoType.hasFlag(AmmoType.F_SANTA_ANNA))) {
                    weaponArmor = 30;
                } else if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                      && ammoType.hasFlag(AmmoType.F_AR10_BARRACUDA)) {
                    weaponArmor = 20;
                } else {
                    weaponArmor = bayWType.getMissileArmor();
                }
                if (range == WeaponType.RANGE_SHORT) {
                    current_av = bayWType.getShortAV();
                } else if (range == WeaponType.RANGE_MED) {
                    current_av = bayWType.getMedAV();
                } else if (range == WeaponType.RANGE_LONG) {
                    current_av = bayWType.getLongAV();
                } else if (range == WeaponType.RANGE_EXT) {
                    current_av = bayWType.getExtAV();
                }

                if (ammoType.hasFlag(AmmoType.F_NUCLEAR)) {
                    nukeS2S = true;
                }

                current_av = updateAVForAmmo(current_av, ammoType, bayWType,
                      range, bayW.getEquipmentNum());
                av = av + current_av;
                armor = armor + weaponArmor;
                // now use the ammo that we had loaded
                if (current_av > 0) {
                    int shots = bayW.getCurrentShots();
                    for (int i = 0; i < shots; i++) {
                        if (null == bayWAmmo
                              || bayWAmmo.getUsableShotsLeft() < 1) {
                            // try loading something else
                            attackingEntity.loadWeaponWithSameAmmo(bayW);
                            bayWAmmo = bayW.getLinkedAmmo();
                        }
                        if (null != bayWAmmo) {
                            bayWAmmo.setShotsLeft(bayWAmmo.getBaseShotsLeft() - 1);
                        }
                    }
                }
            }
        }

        CapMissileArmor = armor - (int) counterAV;
        CapMissileAMSMod = calcCapMissileAMSMod();

        if (bDirect) {
            av = Math.min(av + (int) floor(toHit.getMoS() / 3.0), av * 2);
        }
        av = applyGlancingBlowModifier(av, false);
        av = (int) Math.floor(getBracketingMultiplier() * av);
        return (int) Math.ceil(av);
        // }
    }

    @Override
    protected int calcCapMissileAMSMod() {
        CapMissileAMSMod = (int) Math.ceil(CounterAV / 10.0);
        return CapMissileAMSMod;
    }

    /**
     * Calculate the starting armor value of a flight of Capital Missiles Used for Aero Sanity. This is done in
     * calcAttackValue() otherwise
     */
    @Override
    protected int initializeCapMissileArmor() {
        int armor = 0;
        for (WeaponMounted bayW : weapon.getBayWeapons()) {
            int curr_armor;
            // check the currently loaded ammo
            AmmoMounted bayWAmmo = bayW.getLinkedAmmo();
            AmmoType ammoType = bayWAmmo.getType();
            WeaponType bayWType = bayW.getType();
            if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                  && (ammoType.hasFlag(AmmoType.F_AR10_KILLER_WHALE)
                  || ammoType.hasFlag(AmmoType.F_PEACEMAKER))) {
                curr_armor = 40;
            } else if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                  && (ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK)
                  || ammoType.hasFlag(AmmoType.F_SANTA_ANNA))) {
                curr_armor = 30;
            } else if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                  && ammoType.hasFlag(AmmoType.F_AR10_BARRACUDA)) {
                curr_armor = 20;
            } else {
                curr_armor = bayWType.getMissileArmor();
            }
            armor = armor + curr_armor;
        }
        return armor;
    }

    @Override
    protected int getCapMisMod() {
        int mod = 0;
        for (WeaponMounted bayW : weapon.getBayWeapons()) {
            int curr_mod;
            // check the currently loaded ammo
            AmmoMounted bayWAmmo = bayW.getLinkedAmmo();
            curr_mod = getCritMod(bayWAmmo.getType());
            if (curr_mod > mod) {
                mod = curr_mod;
            }
        }
        return mod;
    }

    /*
     * get the cap mis mod given a single ammo type
     */
    protected int getCritMod(AmmoType ammoType) {
        if (ammoType == null || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.PIRANHA) {
            return 0;
        }
        if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.WHITE_SHARK
              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.WHITE_SHARK_T
              || ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK)
              // Santa Anna, per IO rules
              || ammoType.hasFlag(AmmoType.F_SANTA_ANNA)) {
            return 9;
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.KRAKEN_T
              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.KRAKENM
              // Peacemaker, per IO rules
              || ammoType.hasFlag(AmmoType.F_PEACEMAKER)) {
            return 8;
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.KILLER_WHALE
              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.KILLER_WHALE_T
              || ammoType.hasFlag(AmmoType.F_AR10_KILLER_WHALE)
              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.MANTA_RAY
              || ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.ALAMO) {
            return 10;
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.STINGRAY) {
            return 12;
        } else {
            return 11;
        }
    }

    @Override
    protected double updateAVForAmmo(double current_av, AmmoType ammoType, WeaponType bayWType, int range, int wId) {
        // AR10 munitions
        if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.AR10) {
            if (ammoType.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                current_av = 4;
            } else if (ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                current_av = 3;
            } else if (ammoType.hasFlag(AmmoType.F_PEACEMAKER)) {
                current_av = 1000;
            } else if (ammoType.hasFlag(AmmoType.F_SANTA_ANNA)) {
                current_av = 100;
            } else {
                current_av = 2;
            }
        }
        // Nuclear Warheads for non-AR10 missiles
        if (ammoType.hasFlag(AmmoType.F_SANTA_ANNA)) {
            current_av = 100;
        } else if (ammoType.hasFlag(AmmoType.F_PEACEMAKER)) {
            current_av = 1000;
        }
        return current_av;
    }

    /**
     * Insert any additional attacks that should occur before this attack
     */
    @Override
    protected void insertAttacks(GamePhase phase, Vector<Report> vPhaseReport) {
        // If there are no other missiles in the bay that aren't inserted
        // attacks, there will be a spurious "no damage" report
        if (attackValue < 1) {
            vPhaseReport.clear();
        }

        for (int wId : insertedAttacks) {
            Mounted<?> bayW = attackingEntity.getEquipment(wId);
            WeaponAttackAction newWaa = new WeaponAttackAction(attackingEntity.getId(),
                  weaponAttackAction.getTargetId(),
                  wId);
            Weapon w = (Weapon) bayW.getType();
            // increase ammo by one, we'll use one that we shouldn't use
            // in the next line
            Vector<Report> newReports = new Vector<>();
            bayW.getLinked().setShotsLeft(bayW.getLinked().getBaseShotsLeft() + 1);

            (w.fire(newWaa, game, gameManager)).handle(phase, newReports);

            for (Report report : newReports) {
                report.indent();
            }

            vPhaseReport.addAll(newReports);
        }
    }

    /**
     * Checks to see if this point defense/AMS bay can engage a capital missile This should return true. Only when
     * handling capital missile attacks can this be false.
     */
    @Override
    protected boolean canEngageCapitalMissile(WeaponMounted counter) {
        return counter.getBayWeapons().size() >= 2;
    }

    /**
     * Sets the appropriate AMS Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setAMSBayReportingFlag() {
        amsBayEngagedCap = true;
    }

    /**
     * Sets the appropriate PD Bay reporting flag depending on what type of missile this is
     */
    @Override
    protected void setPDBayReportingFlag() {
        pdBayEngagedCap = true;
    }

    @Override
    public boolean handleAeroSanity(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }

        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
              : null;
        final boolean targetInBuilding = Compute.isInBuilding(game,
              entityTarget);
        final boolean bldgDamagedOnMiss = targetInBuilding
              && !(target instanceof Infantry)
              && attackingEntity.getPosition().distance(target.getPosition()) <= 1;

        if (entityTarget != null) {
            attackingEntity.setLastTarget(entityTarget.getId());
            attackingEntity.setLastTargetDisplayName(entityTarget.getDisplayName());
        }
        // Which building takes the damage?
        IBuilding bldg = game.getBoard().getBuildingAt(target.getPosition());
        // Report weapon attack and its to-hit value.
        Report report = new Report(3115);
        report.indent();
        report.newlines = 0;
        report.subject = subjectId;
        report.add(weaponType.getName());
        if (entityTarget != null) {
            if ((weaponType.getAmmoType() != AmmoType.AmmoTypeEnum.NA)
                  && (weapon.getLinked() != null)
                  && (weapon.getLinked().getType() instanceof AmmoType ammoType)) {
                if (!ammoType.getMunitionType().contains(AmmoType.Munitions.M_STANDARD)) {
                    report.messageId = 3116;
                    report.add(ammoType.getSubMunitionName());
                }
            }
            report.addDesc(entityTarget);
        } else {
            report.messageId = 3120;
            report.add(target.getDisplayName(), true);
        }
        vPhaseReport.addElement(report);

        // are we a glancing hit? Check for this here, report it later
        setGlancingBlowFlags(entityTarget);

        // Set Margin of Success/Failure and check for Direct Blows
        toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));
        bDirect = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DIRECT_BLOW)
              && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);

        // Point Defense fire vs Capital Missiles
        CounterAV = calcCounterAV();

        // CalcAttackValue triggers counterfire, so now we can safely get this
        CapMissileAMSMod = calcCapMissileAMSMod();

        // Set up Capital Missile armor
        CapMissileArmor = initializeCapMissileArmor();

        // and now damage it
        CapMissileArmor = (CapMissileArmor - CounterAV);

        // Only do this if the missile wasn't destroyed
        if (CapMissileAMSMod > 0 && CapMissileArmor > 0) {
            toHit.addModifier(CapMissileAMSMod, "Damage from Point Defenses");
            if (roll.getIntValue() < toHit.getValue()) {
                CapMissileMissed = true;
            }
        }

        // Report any AMS bay action against Capital missiles that doesn't destroy them
        // all.
        if (amsBayEngagedCap && CapMissileArmor > 0) {
            report = new Report(3358);
            report.add(CapMissileAMSMod);
            report.subject = subjectId;
            vPhaseReport.addElement(report);

            // Report any PD bay action against Capital missiles that doesn't destroy them
            // all.
        } else if (pdBayEngagedCap && CapMissileArmor > 0) {
            report = new Report(3357);
            report.add(CapMissileAMSMod);
            report.subject = subjectId;
            vPhaseReport.addElement(report);
        }

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            report = new Report(3135);
            report.subject = subjectId;
            report.add(toHit.getDesc());
            vPhaseReport.addElement(report);
            return false;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            report = new Report(3140);
            report.newlines = 0;
            report.subject = subjectId;
            report.add(toHit.getDesc());
            vPhaseReport.addElement(report);
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            report = new Report(3145);
            report.newlines = 0;
            report.subject = subjectId;
            report.add(toHit.getDesc());
            vPhaseReport.addElement(report);
        } else {
            // roll to hit
            report = new Report(3150);
            report.newlines = 0;
            report.subject = subjectId;
            report.add(toHit);
            vPhaseReport.addElement(report);
        }

        // dice have been rolled, thanks
        report = new Report(3155);
        report.newlines = 0;
        report.subject = subjectId;
        report.add(roll);
        vPhaseReport.addElement(report);

        // do we hit?
        bMissed = roll.getIntValue() < toHit.getValue();

        // Report Glancing/Direct Blow here because of Capital Missile weirdness
        if (!(amsBayEngagedCap || pdBayEngagedCap)) {
            addGlancingBlowReports(vPhaseReport);

            if (bDirect) {
                report = new Report(3189);
                report.subject = attackingEntity.getId();
                report.newlines = 0;
                vPhaseReport.addElement(report);
            }
        }

        // use this if AMS counterfire destroys all the Capital missiles
        if (amsBayEngagedCap && (CapMissileArmor <= 0)) {
            report = new Report(3356);
            report.indent();
            report.subject = subjectId;
            vPhaseReport.addElement(report);
            return false;
        }
        // use this if PD counterfire destroys all the Capital missiles
        if (pdBayEngagedCap && (CapMissileArmor <= 0)) {
            report = new Report(3355);
            report.indent();
            report.subject = subjectId;
            vPhaseReport.addElement(report);
            return false;
        }

        // Don't add heat here, because that will be handled by individual weapons (even
        // if heat by arc)

        // Any necessary PSRs, jam checks, etc.
        // If this boolean is true, don't report
        // the miss later, as we already reported
        // it in doChecks
        boolean missReported = doChecks(vPhaseReport);
        if (missReported) {
            bMissed = true;
        }

        // Do we need some sort of special resolution (minefields,
        // artillery,
        if (specialResolution(vPhaseReport, entityTarget)) {
            return false;
        }

        // We have to adjust the reports on a miss, so they line up
        if (bMissed) {
            reportMiss(vPhaseReport);
            if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg, vPhaseReport)) {
                return false;
            }
        }

        Report.addNewline(vPhaseReport);
        // New toHit data to hold our bay auto hit. We want to be able to get
        // glancing/direct blow
        // data from the 'real' toHit data of this bay handler
        ToHitData autoHit = new ToHitData();
        autoHit.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "if the bay hits, all bay weapons hit");
        int replaceReport;
        for (WeaponMounted m : weapon.getBayWeapons()) {
            if (!m.isBreached() && !m.isDestroyed() && !m.isJammed()) {
                WeaponType bayWType = m.getType();
                if (bayWType instanceof Weapon) {
                    replaceReport = vPhaseReport.size();
                    WeaponAttackAction bayWaa = new WeaponAttackAction(weaponAttackAction.getEntityId(),
                          weaponAttackAction.getTargetType(),
                          weaponAttackAction.getTargetId(),
                          m.getEquipmentNum());
                    AttackHandler bayWHandler = ((Weapon) bayWType).getCorrectHandler(autoHit, bayWaa, game,
                          gameManager);
                    bayWHandler.setAnnouncedEntityFiring(false);
                    // This should always be true. Maybe there's a better way to write this?
                    if (bayWHandler instanceof WeaponHandler wHandler) {
                        wHandler.setParentBayHandler(this);
                    } else {
                        LOGGER.error("bayWHandler {} is not a weapon handler! Cannot set parent bay handler.",
                              bayWHandler.getClass());
                        continue;
                    }
                    bayWHandler.handle(phase, vPhaseReport);
                    if (vPhaseReport.size() > replaceReport) {
                        // fix the reporting - is there a better way to do this
                        Report currentReport = vPhaseReport.get(replaceReport);
                        while (null != currentReport) {
                            vPhaseReport.remove(replaceReport);
                            if ((currentReport.newlines > 0) || (vPhaseReport.size() <= replaceReport)) {
                                currentReport = null;
                            } else {
                                currentReport = vPhaseReport.get(replaceReport);
                            }
                        }
                        report = new Report(3115);
                        report.indent(2);
                        report.newlines = 1;
                        report.subject = subjectId;
                        report.add(bayWType.getName());
                        if (entityTarget != null) {
                            report.addDesc(entityTarget);
                        } else {
                            report.messageId = 3120;
                            report.add(target.getDisplayName(), true);
                        }
                        vPhaseReport.add(replaceReport, report);
                    }
                }
            }
        }
        Report.addNewline(vPhaseReport);
        return false;
    }
}
