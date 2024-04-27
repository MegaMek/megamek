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
import megamek.server.Server;
import org.apache.logging.log4j.LogManager;

import java.util.Vector;

/**
 * @author Jay Lawson
 */
public class BayWeaponHandler extends WeaponHandler {
    private static final long serialVersionUID = -1618484541772117621L;
    AmmoMounted ammo;

    protected BayWeaponHandler() {
        // deserialization only
    }

    public BayWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
    }

    /**
     * Calculate the attack value based on range
     * 
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {
        double av = 0;
        int range = RangeType.rangeBracket(nRange, wtype.getATRanges(), true, false);

        for (WeaponMounted m : weapon.getBayWeapons()) {
            if (!m.isBreached() && !m.isDestroyed() && !m.isJammed()) {
                WeaponType bayWType = ((WeaponType) m.getType());
                // need to cycle through weapons and add av
                if (range == WeaponType.RANGE_SHORT) {
                    av = av + bayWType.getShortAV();
                } else if (range == WeaponType.RANGE_MED) {
                    av = av + bayWType.getMedAV();
                } else if (range == WeaponType.RANGE_LONG) {
                    av = av + bayWType.getLongAV();
                } else if (range == WeaponType.RANGE_EXT) {
                    av = av + bayWType.getExtAV();
                }
            }
        }
        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }
        av = applyGlancingBlowModifier(av, false);
        av = (int) Math.floor(getBracketingMultiplier() * av);
        return (int) Math.ceil(av);
    }
    

    @Override
    protected void addHeat() {
        // Only add heat for first shot in strafe
        if (isStrafing && !isStrafingFirstShot()) {
            return;
        }        
        if (!(toHit.getValue() == TargetRoll.IMPOSSIBLE)) {
            if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_HEAT_BY_BAY)) {
                for (WeaponMounted m : weapon.getBayWeapons()) {
                    ae.heatBuildup += m.getCurrentHeat();
                }
            } else {
                int loc = weapon.getLocation();
                boolean rearMount = weapon.isRearMounted();
                if (!ae.hasArcFired(loc, rearMount)) {
                    ae.heatBuildup += ae.getHeatInArc(loc, rearMount);
                    ae.setArcFired(loc, rearMount);
                }
            }
        }
    }

    /**
     * Sigh, according to the ruling linked below, when weapon bays are fired at
     * ground targets, they should make one to-hit roll, but the AV of each
     * weapon should be applied separately as damage - that needs a special
     * handler
     * 
     * @return a <code>boolean</code> value indicating whether this should be
     *         kept or not
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {

        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            return handleAeroSanity(phase, vPhaseReport);
        }

        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;

        if ((((null == entityTarget) || entityTarget.isAirborne()) 
                && (target.getTargetType() != Targetable.TYPE_HEX_CLEAR 
                &&  target.getTargetType() != Targetable.TYPE_HEX_IGNITE
                &&  target.getTargetType() != Targetable.TYPE_BUILDING)) 
                || game.getBoard().inSpace()
                // Capital missile launchers should return the root handler...
                || (wtype.getAtClass() == (WeaponType.CLASS_CAPITAL_MISSILE))
                || (wtype.getAtClass() == (WeaponType.CLASS_AR10))) {
            return super.handle(phase, vPhaseReport);
        }

        // then we have a ground target, so we need to handle it in a special
        // way
        insertAttacks(phase, vPhaseReport);

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
        String number = nweapons > 1 ? " (" + nweapons + ")" : "";

        // Report weapon attack and its to-hit value.
        Report r = new Report(3115);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        r.add(wtype.getName() + number);
        if (entityTarget != null) {
            r.addDesc(entityTarget);
        } else {
            r.messageId = 3120;
            r.add(target.getDisplayName(), true);
        }

        vPhaseReport.addElement(r);
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

        // are we a glancing hit?
        setGlancingBlowFlags(entityTarget);
        addGlancingBlowReports(vPhaseReport);

        // Set Margin of Success/Failure.
        toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));
        bDirect = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);
        if (bDirect) {
            r = new Report(3189);
            r.subject = ae.getId();
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }

        // Do this stuff first, because some weapon's miss report reference the
        // amount of shots fired and stuff.
        nDamPerHit = calcAttackValue();
        addHeat();

        // Any necessary PSRs, jam checks, etc.
        // If this boolean is true, don't report
        // the miss later, as we already reported
        // it in doChecks
        boolean missReported = doChecks(vPhaseReport);
        if (missReported) {
            bMissed = true;
        }

        // Do we need some sort of special resolution (minefields, artillery,
        if (specialResolution(vPhaseReport, entityTarget)) {
            return false;
        }

        if (bMissed && !missReported) {
            reportMiss(vPhaseReport);

            // Works out fire setting, AMS shots, and whether continuation is
            // necessary.
            if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg, vPhaseReport)) {
                return false;
            }
        }

        if (bMissed) {
            return false;

        } // End missed-target

        if ((target.getTargetType() == Targetable.TYPE_HEX_IGNITE)
                || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)) {
            handleIgnitionDamage(vPhaseReport, bldg, 1);
            return false;
        }
        if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {
            handleClearDamage(vPhaseReport, bldg, nDamPerHit);
            return false;
        }
        // Targeting a building.
        if (target.getTargetType() == Targetable.TYPE_BUILDING) {
            // The building takes the full brunt of the attack
            handleBuildingDamage(vPhaseReport, bldg, nDamPerHit, target.getPosition());
            return false;
        }

        Report.addNewline(vPhaseReport);
        // loop through weapons in bay and do damage
        int range = RangeType.rangeBracket(nRange, wtype.getATRanges(), true, false);
        int hits = 1;
        int nCluster = 1;
        for (WeaponMounted m : weapon.getBayWeapons()) {
            double av = 0;
            if (!m.isBreached() && !m.isDestroyed() && !m.isJammed()) {
                WeaponType bayWType = ((WeaponType) m.getType());
                // need to cycle through weapons and add av
                if (range == WeaponType.RANGE_SHORT) {
                    av = bayWType.getShortAV();
                } else if (range == WeaponType.RANGE_MED) {
                    av = bayWType.getMedAV();
                } else if (range == WeaponType.RANGE_LONG) {
                    av = bayWType.getLongAV();
                } else if (range == WeaponType.RANGE_EXT) {
                    av = bayWType.getExtAV();
                }
            }
            nDamPerHit = (int) Math.ceil(av);
            if (nDamPerHit <= 0) {
                continue;
            }
            bSalvo = true;

            // Buildings shield all units from a certain amount of damage.
            // Amount is based upon the building's CF at the phase's start.
            int bldgAbsorbs = 0;
            if (targetInBuilding && (bldg != null)
                    && (toHit.getThruBldg() == null)) {
                bldgAbsorbs = bldg.getAbsorbtion(target.getPosition());
            }
            
            // Attacking infantry in buildings from same building
            if (targetInBuilding && (bldg != null)
                    && (toHit.getThruBldg() != null)
                    && (entityTarget instanceof Infantry)) {
                // If elevation is the same, building doesn't absorb
                if (ae.getElevation() != entityTarget.getElevation()) {
                    int dmgClass = wtype.getInfantryDamageClass();
                    int nDamage;
                    if (dmgClass < WeaponType.WEAPON_BURST_1D6) {
                        nDamage = nDamPerHit * Math.min(nCluster, hits);
                    } else {
                        // Need to indicate to handleEntityDamage that the
                        // absorbed damage shouldn't reduce incoming damage,
                        // since the incoming damage was reduced in
                        // Compute.directBlowInfantryDamage
                        nDamage = -wtype.getDamage(nRange) * Math.min(nCluster, hits);
                    }
                    bldgAbsorbs = (int) Math.round(nDamage * bldg.getInfDmgFromInside());
                } else {
                    // Used later to indicate a special report
                    bldgAbsorbs = Integer.MIN_VALUE;
                }
            }

            handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
            gameManager.creditKill(entityTarget, ae);
        } // Handle the next weapon in the bay
        Report.addNewline(vPhaseReport);
        return false;
    }
    
    /**
     * Calculate the starting armor value of a flight of Capital Missiles
     * Used for Aero Sanity. This is done in calcAttackValue() otherwise
     */
    protected int initializeCapMissileArmor() {
        return 0;
    }

    public boolean handleAeroSanity(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }

        insertAttacks(phase, vPhaseReport);

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
            r.addDesc(entityTarget);
        } else {
            r.messageId = 3120;
            r.add(target.getDisplayName(), true);
        }
        vPhaseReport.addElement(r);
        
        // Handle point defense fire. For cluster hit missile launchers, we'll report later.
        CounterAV = calcCounterAV();
        
        // We need this for thunderbolt bays
        CapMissileAMSMod = calcCapMissileAMSMod();
        
        // Set up Capital Missile (thunderbolt) armor
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

        // are we a glancing hit?
        setGlancingBlowFlags(entityTarget);
        addGlancingBlowReports(vPhaseReport);

        // Set Margin of Success/Failure.
        toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));
        bDirect = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);
        if (bDirect) {
            r = new Report(3189);
            r.subject = ae.getId();
            r.newlines = 0;
            vPhaseReport.addElement(r);
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
        
        // Large missiles
        // use this if AMS counterfire destroys all the missiles
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

        // We have to adjust the reports on a miss, so they line up
        if (bMissed) {
            reportMiss(vPhaseReport);
            if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg, vPhaseReport)) {
                return false;
            }
        }
        
        // Report point defense effects
        // Set up a cluster hits table modifier
        double counterAVMod = getCounterAV();
        // Report a failure due to overheating
        if (pdOverheated
                && (!(amsBayEngaged
                        || amsBayEngagedCap
                        || amsBayEngagedMissile
                        || pdBayEngaged
                        || pdBayEngagedCap
                        || pdBayEngagedMissile))) {
            r = new Report (3359);
            r.subject = subjectId;
            r.indent();
            vPhaseReport.addElement(r);
        } else if (pdOverheated) {
            // Report a partial failure
            r = new Report (3361);
            r.subject = subjectId;
            r.indent();
            vPhaseReport.addElement(r);
            // Halve the effectiveness of cluster hits modification
            counterAVMod /= 2.0;
        }
        // Now report the effects, if any
        // Missiles using the cluster hits table
        if (amsBayEngaged || pdBayEngaged) {
            r = new Report(3366);
            r.indent();
            r.subject = subjectId;
            r.add((int) counterAVMod);
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }

        Report.addNewline(vPhaseReport);
        // New toHit data to hold our bay auto hit. We want to be able to get glacing/direct blow
        // data from the 'real' toHit data of this bay handler
        ToHitData autoHit = new ToHitData();
        autoHit.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "if the bay hits, all bay weapons hit");
        int replaceReport;
        for (WeaponMounted m : weapon.getBayWeapons()) {
            if (!m.isBreached() && !m.isDestroyed() && !m.isJammed()) {
                WeaponType bayWType = m.getType();
                if (bayWType instanceof Weapon) {
                    replaceReport = vPhaseReport.size();
                    WeaponAttackAction bayWaa = new WeaponAttackAction(waa.getEntityId(), waa.getTargetType(),
                            waa.getTargetId(), m.getEquipmentNum());
                    AttackHandler bayWHandler = ((Weapon) bayWType).getCorrectHandler(autoHit, bayWaa, game, gameManager);
                    bayWHandler.setAnnouncedEntityFiring(false);
                    // This should always be true
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
