/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class BayWeaponHandler extends WeaponHandler {

    /**
     *
     */

    private static final long serialVersionUID = -1618484541772117621L;
    Mounted ammo;

    protected BayWeaponHandler() {
        // deserialization only
    }

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public BayWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
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

        for (int wId : weapon.getBayWeapons()) {
            Mounted m = ae.getEquipment(wId);
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
        if (bGlancing) {
            av = (int) Math.floor(av / 2.0);

        }
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
                for (int wId : weapon.getBayWeapons()) {
                    Mounted m = ae.getEquipment(wId);
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
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {

        if(game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
            return handleAeroSanity(phase, vPhaseReport);
        }

        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;

        if ((((null == entityTarget) || entityTarget.isAirborne()) 
                && (target.getTargetType() != Targetable.TYPE_HEX_CLEAR 
                &&  target.getTargetType() != Targetable.TYPE_HEX_IGNITE
                &&  target.getTargetType() != Targetable.TYPE_BUILDING))
                || game.getBoard().inSpace()) {
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
            r.add(toHit.getValue());
            vPhaseReport.addElement(r);
        }

        // dice have been rolled, thanks
        r = new Report(3155);
        r.newlines = 0;
        r.subject = subjectId;
        r.add(roll);
        vPhaseReport.addElement(r);

        // do we hit?
        bMissed = roll < toHit.getValue();

        // are we a glancing hit?
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS)) {
            if (roll == toHit.getValue()) {
                bGlancing = true;
                r = new Report(3186);
                r.subject = ae.getId();
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                bGlancing = false;
            }
        } else {
            bGlancing = false;
        }

        // Set Margin of Success/Failure.
        toHit.setMoS(roll - Math.max(2, toHit.getValue()));
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
            if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg,
                    vPhaseReport)) {
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
            handleBuildingDamage(vPhaseReport, bldg, nDamPerHit,
                    target.getPosition());
            return false;
        }

        Report.addNewline(vPhaseReport);
        // loop through weapons in bay and do damage
        int range = RangeType.rangeBracket(nRange, wtype.getATRanges(), true, false);
        int hits = 1;
        int nCluster = 1;
        for (int wId : weapon.getBayWeapons()) {
            double av = 0;
            Mounted m = ae.getEquipment(wId);
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
                        nDamage = -wtype.getDamage(nRange)
                                * Math.min(nCluster, hits);
                    }
                    bldgAbsorbs = (int) Math.round(nDamage
                            * bldg.getInfDmgFromInside());
                } else {
                    // Used later to indicate a special report
                    bldgAbsorbs = Integer.MIN_VALUE;
                }
            }

            handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                    nCluster, bldgAbsorbs);
            server.creditKill(entityTarget, ae);
        } // Handle the next weapon in the vay
        Report.addNewline(vPhaseReport);
        return false;
    }

    public boolean handleAeroSanity(IGame.Phase phase, Vector<Report> vPhaseReport) {
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
            r.add(toHit.getValue());
            vPhaseReport.addElement(r);
        }

        // dice have been rolled, thanks
        r = new Report(3155);
        r.newlines = 0;
        r.subject = subjectId;
        r.add(roll);
        vPhaseReport.addElement(r);

        // do we hit?
        bMissed = roll < toHit.getValue();

        // are we a glancing hit?
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS)) {
            if (roll == toHit.getValue()) {
                bGlancing = true;
                r = new Report(3186);
                r.subject = ae.getId();
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                bGlancing = false;
            }
        } else {
            bGlancing = false;
        }

        // Set Margin of Success/Failure.
        toHit.setMoS(roll - Math.max(2, toHit.getValue()));
        bDirect = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);
        if (bDirect) {
            r = new Report(3189);
            r.subject = ae.getId();
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }

       //Don't add heat here, because that will be handled by individual weapons (even if heat by arc)
        
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
        if (bMissed){
            reportMiss(vPhaseReport);
            if (!handleSpecialMiss(entityTarget, bldgDamagedOnMiss, bldg,
                    vPhaseReport)) {
                return false;
            }
        }

        Report.addNewline(vPhaseReport);
        toHit.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "if the bay hits, all bay weapons hit");
        int replaceReport;
        for (int wId : weapon.getBayWeapons()) {
            Mounted m = ae.getEquipment(wId);
            if (!m.isBreached() && !m.isDestroyed() && !m.isJammed()) {
                WeaponType bayWType = ((WeaponType) m.getType());
                if(bayWType instanceof Weapon) {
                    replaceReport = vPhaseReport.size();
                    WeaponAttackAction bayWaa = new WeaponAttackAction(waa.getEntityId(), waa.getTargetType(), waa.getTargetId(), wId);
                    AttackHandler bayWHandler = ((Weapon)bayWType).getCorrectHandler(toHit, bayWaa, game, server);
                    bayWHandler.setAnnouncedEntityFiring(false);
                    bayWHandler.handle(phase, vPhaseReport);
                    if(vPhaseReport.size() > replaceReport) {
                        //fix the reporting - is there a better way to do this
                        if(vPhaseReport.size() > replaceReport) {
                            Report currentReport = vPhaseReport.get(replaceReport);
                            while(null != currentReport) {
                                vPhaseReport.remove(replaceReport);
                                if(currentReport.newlines > 0 || vPhaseReport.size() <= replaceReport) {
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
        } // Handle the next weapon in the bay
        Report.addNewline(vPhaseReport);
        return false;
    }

}
