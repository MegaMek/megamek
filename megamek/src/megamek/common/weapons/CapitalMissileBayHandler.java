/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.server.GameManager;
import org.apache.logging.log4j.LogManager;

import java.util.Vector;

/**
 * @author Jay Lawson
 */
public class CapitalMissileBayHandler extends AmmoBayWeaponHandler {
    private static final long serialVersionUID = -1618484541772117621L;
    boolean advancedPD = false;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public CapitalMissileBayHandler(ToHitData t, WeaponAttackAction w, Game g,
            GameManager m) {
        super(t, w, g, m);
        advancedPD = g.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADV_POINTDEF);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.AttackHandler#handle(int, java.util.Vector)
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {

        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            return handleAeroSanity(phase, vPhaseReport);
        }

        int numAttacks = 1;

        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;

        if (entityTarget != null) {
            ae.setLastTarget(entityTarget.getId());
            ae.setLastTargetDisplayName(entityTarget.getDisplayName());
        }
        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt(target.getPosition());
        String number = nweapons > 1 ? " (" + nweapons + ")" : "";
        for (int i = numAttacks; i > 0; i--) {
            // Report weapon attack and its to-hit value.
            Report r = new Report(3115);
            r.indent();
            r.newlines = 0;
            r.subject = subjectId;
            r.add(wtype.getName() + number);
            if (entityTarget != null) {
                if ((wtype.getAmmoType() != AmmoType.T_NA)
                        && (weapon.getLinked() != null)
                        && (weapon.getLinked().getType() instanceof AmmoType)) {
                    AmmoType atype = (AmmoType) weapon.getLinked().getType();
                    if (!atype.getMunitionType().contains(AmmoType.Munitions.M_STANDARD)) {
                        r.messageId = 3116;
                        r.add(atype.getSubMunitionName());
                    }
                }
                r.addDesc(entityTarget);
            } else {
                r.messageId = 3120;
                r.add(target.getDisplayName(), true);
            }
            vPhaseReport.addElement(r);

        //Point Defense fire vs Capital Missiles

        // are we a glancing hit?  Check for this here, report it later
            setGlancingBlowFlags(entityTarget);

        // Set Margin of Success/Failure and check for Direct Blows
        toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));
        bDirect = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);

        //This has to be up here so that we don't screw up glancing/direct blow reports
        attackValue = calcAttackValue();

        //CalcAttackValue triggers counterfire, so now we can safely get this
        CapMissileAMSMod = getCapMissileAMSMod();

        //Only do this if the missile wasn't destroyed
        if (CapMissileAMSMod > 0 && CapMissileArmor > 0) {
            toHit.addModifier(CapMissileAMSMod, "Damage from Point Defenses");
            if (roll.getIntValue() < toHit.getValue()) {
                CapMissileMissed = true;
            }
        }

        // Report any AMS bay action against Capital missiles that doesn't destroy them all.
        if (amsBayEngagedCap && CapMissileArmor > 0) {
            r = new Report(3358);
            r.add(CapMissileAMSMod);
            r.subject = subjectId;
            vPhaseReport.addElement(r);

        // Report any PD bay action against Capital missiles that doesn't destroy them all.
        } else if (pdBayEngagedCap && CapMissileArmor > 0) {
            r = new Report(3357);
            r.add(CapMissileAMSMod);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report (3135);
            r.subject = subjectId;
            r.add(" " + target.getPosition(), true);
            vPhaseReport.addElement(r);
            return false;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            r = new Report(3140);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            r = new Report(3145);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else {
            // roll to hit
            r = new Report(3150);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit);
            vPhaseReport.addElement(r);
        }

        // dice have been rolled, thanks
        r = new Report(3155);
        r.newlines = 0;
        r.subject = subjectId;
        r.add(roll);
        vPhaseReport.addElement(r);

        // do we hit?
        bMissed = roll.getIntValue() < toHit.getValue();

        //Report Glancing/Direct Blow here because of Capital Missile weirdness
        if (!(amsBayEngagedCap || pdBayEngagedCap)) {
            addGlancingBlowReports(vPhaseReport);

            if (bDirect) {
                r = new Report(3189);
                r.subject = ae.getId();
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        }

        CounterAV = getCounterAV();
        //use this if AMS counterfire destroys all the Capital missiles
        if (amsBayEngagedCap && (CapMissileArmor <= 0)) {
            r = new Report(3356);
            r.indent();
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            bMissed = true;
        }
        //use this if PD counterfire destroys all the Capital missiles
        if (pdBayEngagedCap && (CapMissileArmor <= 0)) {
            r = new Report(3355);
            r.indent();
            r.subject = subjectId;
            vPhaseReport.addElement(r);
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
        int nCluster = calcnCluster();
        int id = vPhaseReport.size();
        int hits = calcHits(vPhaseReport);

        if (target.isAirborne() || game.getBoard().inSpace() || ae.usesWeaponBays()) {
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

        //Capital missiles shouldn't be able to target buildings, being space-only weapons
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
            r = new Report(3365);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            return false;
        }
        if (!bMissed && (entityTarget != null)) {
            handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                    nCluster, bldgAbsorbs);
            gameManager.creditKill(entityTarget, ae);
        } else if (!bMissed) { // Hex is targeted, need to report a hit
            r = new Report(3390);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
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
        int weaponarmor = 0;
        int range = RangeType.rangeBracket(nRange, wtype.getATRanges(), true, false);

        for (WeaponMounted bayW : weapon.getBayWeapons()) {
            // check the currently loaded ammo
            AmmoMounted bayWAmmo = bayW.getLinkedAmmo();
            if (null == bayWAmmo || bayWAmmo.getUsableShotsLeft() < 1) {
                // try loading something else
                ae.loadWeaponWithSameAmmo(bayW);
                bayWAmmo = bayW.getLinkedAmmo();
            }
            if (!bayW.isBreached()
                    && !bayW.isDestroyed()
                    && !bayW.isJammed()
                    && bayWAmmo != null
                    && ae.getTotalAmmoOfType(bayWAmmo.getType()) >= bayW.getCurrentShots()) {
                WeaponType bayWType = ((WeaponType) bayW.getType());
                // need to cycle through weapons and add av
                double current_av = 0;

                AmmoType atype = (AmmoType) bayWAmmo.getType();
                if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                        && (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)
                                || atype.hasFlag(AmmoType.F_PEACEMAKER))) {
                    weaponarmor = 40;
                } else if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                        && (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)
                                || atype.hasFlag(AmmoType.F_SANTA_ANNA))) {
                    weaponarmor = 30;
                } else if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                        && atype.hasFlag(AmmoType.F_AR10_BARRACUDA)) {
                    weaponarmor = 20;
                } else {
                weaponarmor = bayWType.getMissileArmor();
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

                if (atype.hasFlag(AmmoType.F_NUCLEAR)) {
                    nukeS2S = true;
                }

                current_av = updateAVforAmmo(current_av, atype, bayWType,
                        range, bayW.getEquipmentNum());
                av = av + current_av;
                armor = armor + weaponarmor;
                // now use the ammo that we had loaded
                if (current_av > 0) {
                    int shots = bayW.getCurrentShots();
                    for (int i = 0; i < shots; i++) {
                        if (null == bayWAmmo
                                || bayWAmmo.getUsableShotsLeft() < 1) {
                            // try loading something else
                            ae.loadWeaponWithSameAmmo(bayW);
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
                av = Math.min(av + (toHit.getMoS() / 3), av * 2);
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

    @Override
    protected int getCapMissileAMSMod() {
        return CapMissileAMSMod;
    }

    /**
     * Calculate the starting armor value of a flight of Capital Missiles
     * Used for Aero Sanity. This is done in calcAttackValue() otherwise
     *
     */
    @Override
    protected int initializeCapMissileArmor() {
        int armor = 0;
        for (WeaponMounted bayW : weapon.getBayWeapons()) {
            int curr_armor = 0;
            // check the currently loaded ammo
            AmmoMounted bayWAmmo = bayW.getLinkedAmmo();
            AmmoType atype = bayWAmmo.getType();
            WeaponType bayWType = bayW.getType();
            if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                    && (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)
                            || atype.hasFlag(AmmoType.F_PEACEMAKER))) {
                curr_armor = 40;
            } else if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                    && (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)
                            || atype.hasFlag(AmmoType.F_SANTA_ANNA))) {
                curr_armor = 30;
            } else if (bayWType.getAtClass() == (WeaponType.CLASS_AR10)
                    && atype.hasFlag(AmmoType.F_AR10_BARRACUDA)) {
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
            int curr_mod = 0;
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
    protected int getCritMod(AmmoType atype) {
        if (atype == null || atype.getAmmoType() == AmmoType.T_PIRANHA) {
            return 0;
        }
        if (atype.getAmmoType() == AmmoType.T_WHITE_SHARK
                || atype.getAmmoType() == AmmoType.T_WHITE_SHARK_T
                || atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)
                // Santa Anna, per IO rules
                || atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
            return 9;
        } else if (atype.getAmmoType() == AmmoType.T_KRAKEN_T
                || atype.getAmmoType() == AmmoType.T_KRAKENM
                // Peacemaker, per IO rules
                || atype.hasFlag(AmmoType.F_PEACEMAKER)) {
            return 8;
        } else if (atype.getAmmoType() == AmmoType.T_KILLER_WHALE
                || atype.getAmmoType() == AmmoType.T_KILLER_WHALE_T
                || atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)
                || atype.getAmmoType() == AmmoType.T_MANTA_RAY
                || atype.getAmmoType() == AmmoType.T_ALAMO) {
            return 10;
        } else if (atype.getAmmoType() == AmmoType.T_STINGRAY) {
            return 12;
        } else {
            return 11;
        }
    }

    @Override
    protected double updateAVforAmmo(double current_av, AmmoType atype,
            WeaponType bayWType, int range, int wId) {
        //AR10 munitions
        if (atype.getAmmoType() == AmmoType.T_AR10) {
            if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                current_av = 4;
            } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                current_av = 3;
            } else if (atype.hasFlag(AmmoType.F_PEACEMAKER)) {
                current_av = 1000;
            } else if (atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
                current_av = 100;
            } else {
                current_av = 2;
            }
        }
        // Nuclear Warheads for non-AR10 missiles
        if (atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
            current_av = 100;
        } else if (atype.hasFlag(AmmoType.F_PEACEMAKER)) {
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
            Mounted bayW = ae.getEquipment(wId);
            WeaponAttackAction newWaa = new WeaponAttackAction(ae.getId(),
                    waa.getTargetId(), wId);
            Weapon w = (Weapon) bayW.getType();
            // increase ammo by one, we'll use one that we shouldn't use
            // in the next line
            Vector<Report> newReports = new Vector<>();
            bayW.getLinked().setShotsLeft(
                    bayW.getLinked().getBaseShotsLeft() + 1);
            (w.fire(newWaa, game, gameManager)).handle(phase, newReports);
            for (Report r : newReports) {
                r.indent();
            }
            vPhaseReport.addAll(newReports);
        }
    }

    /**
     * Checks to see if this point defense/AMS bay can engage a capital missile
     * This should return true. Only when handling capital missile attacks can this be false.
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
                && ae.getPosition().distance(target.getPosition()) <= 1;

        if (entityTarget != null) {
            ae.setLastTarget(entityTarget.getId());
            ae.setLastTargetDisplayName(entityTarget.getDisplayName());
        }
        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt(target.getPosition());
        // Report weapon attack and its to-hit value.
        Report r = new Report(3115);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        r.add(wtype.getName());
        if (entityTarget != null) {
            if ((wtype.getAmmoType() != AmmoType.T_NA)
                    && (weapon.getLinked() != null)
                    && (weapon.getLinked().getType() instanceof AmmoType)) {
                AmmoType atype = (AmmoType) weapon.getLinked().getType();
                if (!atype.getMunitionType().contains(AmmoType.Munitions.M_STANDARD)) {
                    r.messageId = 3116;
                    r.add(atype.getSubMunitionName());
                }
            }
            r.addDesc(entityTarget);
        } else {
            r.messageId = 3120;
            r.add(target.getDisplayName(), true);
        }
        vPhaseReport.addElement(r);

        // are we a glancing hit?  Check for this here, report it later
        setGlancingBlowFlags(entityTarget);

        // Set Margin of Success/Failure and check for Direct Blows
        toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));
        bDirect = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
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

        // Report any AMS bay action against Capital missiles that doesn't destroy them all.
        if (amsBayEngagedCap && CapMissileArmor > 0) {
            r = new Report(3358);
            r.add(CapMissileAMSMod);
            r.subject = subjectId;
            vPhaseReport.addElement(r);

        // Report any PD bay action against Capital missiles that doesn't destroy them all.
        } else if (pdBayEngagedCap && CapMissileArmor > 0) {
            r = new Report(3357);
            r.add(CapMissileAMSMod);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(3135);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            r = new Report(3140);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            r = new Report(3145);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else {
            // roll to hit
            r = new Report(3150);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit);
            vPhaseReport.addElement(r);
        }

        // dice have been rolled, thanks
        r = new Report(3155);
        r.newlines = 0;
        r.subject = subjectId;
        r.add(roll);
        vPhaseReport.addElement(r);

        // do we hit?
        bMissed = roll.getIntValue() < toHit.getValue();

        // Report Glancing/Direct Blow here because of Capital Missile weirdness
        if (!(amsBayEngagedCap || pdBayEngagedCap)) {
            addGlancingBlowReports(vPhaseReport);

            if (bDirect) {
                r = new Report(3189);
                r.subject = ae.getId();
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        }

        // use this if AMS counterfire destroys all the Capital missiles
        if (amsBayEngagedCap && (CapMissileArmor <= 0)) {
            r = new Report(3356);
            r.indent();
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            return false;
        }
        // use this if PD counterfire destroys all the Capital missiles
        if (pdBayEngagedCap && (CapMissileArmor <= 0)) {
            r = new Report(3355);
            r.indent();
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            return false;
        }

        // Don't add heat here, because that will be handled by individual weapons (even if heat by arc)

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
        // New toHit data to hold our bay auto hit. We want to be able to get glancing/direct blow
        // data from the 'real' toHit data of this bay handler
        ToHitData autoHit = new ToHitData();
        autoHit.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "if the bay hits, all bay weapons hit");
        int replaceReport;
        for (WeaponMounted m : weapon.getBayWeapons()) {
            if (!m.isBreached() && !m.isDestroyed() && !m.isJammed()) {
                WeaponType bayWType = ((WeaponType) m.getType());
                if (bayWType instanceof Weapon) {
                    replaceReport = vPhaseReport.size();
                    WeaponAttackAction bayWaa = new WeaponAttackAction(waa.getEntityId(), waa.getTargetType(),
                            waa.getTargetId(), m.getEquipmentNum());
                    AttackHandler bayWHandler = ((Weapon) bayWType).getCorrectHandler(autoHit, bayWaa, game, gameManager);
                    bayWHandler.setAnnouncedEntityFiring(false);
                    // This should always be true. Maybe there's a better way to write this?
                    if (bayWHandler instanceof WeaponHandler) {
                        WeaponHandler wHandler = (WeaponHandler) bayWHandler;
                        wHandler.setParentBayHandler(this);
                    } else {
                        LogManager.getLogger().error("bayWHandler " +  bayWHandler.getClass()
                                + " is not a weapon handler! Cannot set parent bay handler.");
                        continue;
                    }
                    bayWHandler.handle(phase, vPhaseReport);
                    if (vPhaseReport.size() > replaceReport) {
                        //fix the reporting - is there a better way to do this
                        Report currentReport = vPhaseReport.get(replaceReport);
                        while (null != currentReport) {
                            vPhaseReport.remove(replaceReport);
                            if ((currentReport.newlines > 0) || (vPhaseReport.size() <= replaceReport)) {
                                currentReport = null;
                            } else {
                                currentReport = vPhaseReport.get(replaceReport);
                            }
                        }
                        r = new Report(3115);
                        r.indent(2);
                        r.newlines = 1;
                        r.subject = subjectId;
                        r.add(bayWType.getName());
                        if (entityTarget != null) {
                            r.addDesc(entityTarget);
                        } else {
                            r.messageId = 3120;
                            r.add(target.getDisplayName(), true);
                        }
                        vPhaseReport.add(replaceReport, r);
                    }
                }
            }
        }
        Report.addNewline(vPhaseReport);
        return false;
    }
}
