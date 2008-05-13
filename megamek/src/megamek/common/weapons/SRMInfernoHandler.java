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

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.BuildingTarget;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Report;
import megamek.common.Tank;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class SRMInfernoHandler extends SRMHandler {

    /**
     * 
     */
    private static final long serialVersionUID = 826674238068613732L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public SRMInfernoHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
        sSalvoType = " inferno missile(s) ";
        bSalvo = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#reportMiss(java.util.Vector)
     */
    protected void reportMiss(Vector<Report> vPhaseReport) {
        super.reportMiss(vPhaseReport);
        server.tryIgniteHex(target.getPosition(), ae.getId(), true, 11,
                vPhaseReport);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#handleAccidentalBuildingDamage(java.util.Vector,
     *      megamek.common.Building, int, int)
     */
    protected void handleAccidentalBuildingDamage(Vector<Report> vPhaseReport,
            Building bldg, int hits, int nDamPerHit) {
        // Is the building hit by Inferno rounds?
        if (hits > 0) {
            vPhaseReport.addAll(server.deliverInfernoMissiles(ae, new BuildingTarget(this.waa
                    .getTarget(game).getPosition(), game.getBoard(), false),
                    hits));
        }
    }

    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {
        if (!this.cares(phase)) {
            return true;
        }
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
        final boolean targetInBuilding = Compute.isInBuilding(game,
                entityTarget);

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt(target.getPosition());

        // Report weapon attack and its to-hit value.
        r = new Report(3115);
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
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            r = new Report(3135);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_FAIL) {
            r = new Report(3140);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
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
        if (game.getOptions().booleanOption("maxtech_glancing_blows")) {
            if (roll == toHit.getValue()) {
                bGlancing = true;
                r = new Report(3186);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                bGlancing = false;
            }
        } else {
            bGlancing = false;
        }

        // Do this stuff first, because some weapon's miss report reference the
        // amount of shots fired and stuff.
        addHeat();

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
            // Works out fire setting, AMS shots, and whether continuation is
            // necessary.
            if (!handleSpecialMiss(entityTarget, targetInBuilding, bldg,
                    vPhaseReport)) {
                return false;
            }
        }

        // yeech. handle damage. . different weapons do this in very different
        // ways
        int hits = calcHits(vPhaseReport);
        Report.addNewline(vPhaseReport);

        // We've calculated how many hits. At this point, any missed
        // shots damage the building instead of the target.
        if (bMissed) {
            if (targetInBuilding && bldg != null) {
                handleAccidentalBuildingDamage(vPhaseReport, bldg, hits,
                        nDamPerHit);
            } // End missed-target-in-building
            return false;

        } // End missed-target

        // light inferno missiles all at once, if not missed
        if(!bMissed) {
            vPhaseReport.addAll(server.deliverInfernoMissiles(ae, target, hits));
        }
        return false;
    }
    
    /*
     * (non-Javadoc)
     * @see megamek.common.weapons.MissileWeaponHandler#calcHits(java.util.Vector)
     */
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit with all missiles
        // BAs do one lump of damage per BA suit
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            if (ae instanceof BattleArmor) {
                bSalvo = true;
                r = new Report(3325);
                r.subject = subjectId;
                r.add(wtype.getRackSize()
                        * ((BattleArmor) ae).getShootingStrength());
                r.add(sSalvoType);
                r.add(toHit.getTableDesc());
                r.newlines = 0;
                vPhaseReport.add(r);
                return ((BattleArmor) ae).getShootingStrength()*wtype.getRackSize();
            }
            r = new Report(3325);
            r.subject = subjectId;
            r.add(wtype.getRackSize());
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            vPhaseReport.add(r);
            return wtype.getRackSize();
        }
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
        int missilesHit;
        int nMissilesModifier = nSalvoBonus;
        boolean bWeather = false;
        boolean maxtechmissiles = game.getOptions().booleanOption(
                "maxtech_mslhitpen");
        if (maxtechmissiles) {
            if (nRange <= 1) {
                nMissilesModifier += 1;
            } else if (nRange <= wtype.getShortRange()) {
                nMissilesModifier += 0;
            } else if (nRange <= wtype.getMediumRange()) {
                nMissilesModifier -= 1;
            } else {
                nMissilesModifier -= 2;
            }
        }
        boolean bMekStealthActive = false;
        if (ae instanceof Mech) {
            bMekStealthActive = ae.isStealthActive();
        }
        Mounted mLinker = weapon.getLinkedBy();
        AmmoType atype = (AmmoType) ammo.getType();
        // is any hex in the flight path of the missile ECM affected?
        boolean bECMAffected = false;
        // if the attacker is affected by ECM or the target is protected by ECM
        // then
        // act as if effected.
        if (Compute.isAffectedByECM(ae, ae.getPosition(), target.getPosition())) {
            bECMAffected = true;
        }
        
        if ((mLinker != null && mLinker.getType() instanceof MiscType
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(
                MiscType.F_ARTEMIS))
                && atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE) {
            if (bECMAffected) {
                // ECM prevents bonus
                r = new Report(3330);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else if (bMekStealthActive) {
                // stealth prevents bonus
                r = new Report(3335);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else
                nMissilesModifier += 2;
        } else if (atype.getAmmoType() == AmmoType.T_ATM) {
            if (bECMAffected) {
                // ECM prevents bonus
                r = new Report(3330);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else if (bMekStealthActive) {
                // stealth prevents bonus
                r = new Report(3335);
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else
                nMissilesModifier += 2;
        } else if (entityTarget != null
                && (entityTarget.isNarcedBy(ae.getOwner().getTeam()) || entityTarget
                        .isINarcedBy(ae.getOwner().getTeam()))) {
            // only apply Narc bonus if we're not suffering ECM effect
            // and we are using narc ammo, and we're not firing indirectly.
            // narc capable missiles are only affected if the narc pod, which 
            // sits on the target, is ECM affected
            boolean bTargetECMAffected = false;
            bTargetECMAffected = Compute.isAffectedByECM(ae, 
                    target.getPosition(), target.getPosition());
            if (((atype.getAmmoType() == AmmoType.T_LRM) ||
                 (atype.getAmmoType() == AmmoType.T_SRM)) ||
                 (atype.getAmmoType() == AmmoType.T_MML)
                    && atype.getMunitionType() == AmmoType.M_NARC_CAPABLE
                    && (weapon.curMode() == null || !weapon.curMode().equals(
                            "Indirect"))) {
                if (bTargetECMAffected) {
                    // ECM prevents bonus
                    r = new Report(3330);
                    r.subject = subjectId;
                    r.newlines = 0;
                    vPhaseReport.addElement(r);
                } else
                    nMissilesModifier += 2;
            }
        }
        if (bGlancing) {
            nMissilesModifier -= 4;
        }

        // weather checks
        if (game.getOptions().booleanOption("blizzard")
                && wtype.hasFlag(WeaponType.F_MISSILE)) {
            nMissilesModifier -= 4;
            bWeather = true;
        }

        if (game.getOptions().booleanOption("moderate_winds")
                && wtype.hasFlag(WeaponType.F_MISSILE)) {
            nMissilesModifier -= 2;
            bWeather = true;
        }

        if (game.getOptions().booleanOption("high_winds")
                && wtype.hasFlag(WeaponType.F_MISSILE)) {
            nMissilesModifier -= 4;
            bWeather = true;
        }

        // add AMS mods
        nMissilesModifier += getAMSHitsMod(vPhaseReport);

        if (allShotsHit())
            missilesHit = wtype.getRackSize();
        else {
            if (ae instanceof BattleArmor)
                missilesHit = Compute.missilesHit(wtype.getRackSize()
                        * ((BattleArmor) ae).getShootingStrength(),
                        nMissilesModifier, bWeather || bGlancing
                                || maxtechmissiles, weapon.isHotLoaded());
            else
                missilesHit = Compute.missilesHit(wtype.getRackSize(),
                        nMissilesModifier, bWeather || bGlancing
                                || maxtechmissiles, weapon.isHotLoaded());
        }

        if (missilesHit > 0) {
            r = new Report(3325);
            r.subject = subjectId;
            r.add(missilesHit);
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            vPhaseReport.addElement(r);
            if (nMissilesModifier != 0) {
                if (nMissilesModifier > 0)
                    r = new Report(3340);
                else
                    r = new Report(3341);
                r.subject = subjectId;
                r.add(nMissilesModifier);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        }
        r = new Report(3345);
        r.newlines = 0;
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }
}
