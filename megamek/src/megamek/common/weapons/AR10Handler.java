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
import megamek.common.Building;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.options.OptionsConstants;
import megamek.server.GameManager;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class AR10Handler extends AmmoWeaponHandler {

    /**
     *
     */
    private static final long serialVersionUID = -2536312899803153911L;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public AR10Handler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.AttackHandler#handle(int, java.util.Vector)
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
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
            toHit.setMoS(roll - Math.max(2, toHit.getValue()));
            bDirect = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                    && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);

            //This has to be up here so that we don't screw up glancing/direct blow reports
            attackValue = calcAttackValue();

            //CalcAttackValue triggers counterfire, so now we can safely get this
            CapMissileAMSMod = getCapMissileAMSMod();

            //Only do this if the missile wasn't destroyed
            if (CapMissileAMSMod > 0 && CapMissileArmor > 0) {
                toHit.addModifier(CapMissileAMSMod, "Damage from Point Defenses");
                if (roll < toHit.getValue()) {
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
            r.addDataWithTooltip(String.valueOf(roll), rollReport);
            vPhaseReport.addElement(r);

            // do we hit?
            bMissed = roll < toHit.getValue();

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
            }
            //use this if PD counterfire destroys all the Capital missiles
            if (pdBayEngagedCap && (CapMissileArmor <= 0)) {
                r = new Report(3355);
                r.indent();
                r.subject = subjectId;
                vPhaseReport.addElement(r);
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

    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {
        int av = 0;
        AmmoType atype = (AmmoType) ammo.getType();
        if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
            av = 4;
        } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
            av = 3;
        } else {
            av = 2;
        }
        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }
        av = applyGlancingBlowModifier(av, false);
        av = (int) Math.floor(getBracketingMultiplier() * av);
        return av;
    }

    @Override
    protected int getCapMisMod() {
        int mod = 0;
        AmmoType atype = (AmmoType) ammo.getType();
        if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
            mod = 10;
        } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
            mod = 9;
        } else {
            mod = 11;
        }
        return mod;
    }

}
