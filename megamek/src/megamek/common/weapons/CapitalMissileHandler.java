/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.Building;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.options.OptionsConstants;
import megamek.server.gameManager.GameManager;

/**
 * @author Jay Lawson
 */
public class CapitalMissileHandler extends AmmoWeaponHandler {
    private static final long serialVersionUID = -1618484541772117621L;
    boolean advancedPD = false;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public CapitalMissileHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
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

        // are we a glancing hit?  Check for this here, report it later
        if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_GLANCING_BLOWS)) {
            if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)) {
                if (getParentBayHandler() != null) {
                    //Use the to-hit value for the bay handler, otherwise toHit is set to Automatic Success
                    WeaponHandler bayHandler = getParentBayHandler();
                    bGlancing = (roll.getIntValue() == bayHandler.toHit.getValue());
                    bLowProfileGlancing = isLowProfileGlancingBlow(entityTarget, bayHandler.toHit);
                }
            } else {
                setGlancingBlowFlags(entityTarget);
            }
        }

        // Set Margin of Success/Failure and check for Direct Blows
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)
                && getParentBayHandler() != null) {
            //Use the to-hit value for the bay handler, otherwise toHit is set to Automatic Success
            WeaponHandler bayHandler = getParentBayHandler();
            toHit.setMoS(roll.getIntValue() - Math.max(2, bayHandler.toHit.getValue()));
        } else {
            toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));
        }
        bDirect = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_DIRECT_BLOW)
                && ((toHit.getMoS() / 3) >= 1) && (entityTarget != null);

        // Used when using a grounded DropShip with individual weapons
        // or a fighter squadron loaded with ASM or Alamo bombs.
        nDamPerHit = calcDamagePerHit();

        // Point Defense fire vs Capital Missiles
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)
                && getParentBayHandler() != null) {
            WeaponHandler bayHandler = getParentBayHandler();
            CounterAV = bayHandler.getCounterAV();
        } else {
            // This gets used if you're shooting at an airborne DropShip. It can defend with PD bays.
            attackValue = calcAttackValue();
        }
        // CalcAttackValue triggers counterfire, so now we can safely get this
        CapMissileAMSMod = getCapMissileAMSMod();

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
            nDamPerHit = 0;
        }
        //use this if PD counterfire destroys all the Capital missiles
        if (pdBayEngagedCap && (CapMissileArmor <= 0)) {
            r = new Report(3355);
            r.indent();
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            nDamPerHit = 0;
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
        if (!bMissed) {
         // for each cluster of hits, do a chunk of damage
            while (hits > 0) {
                int nDamage;
                // targeting a hex for igniting
                if ((target.getTargetType() == Targetable.TYPE_HEX_IGNITE)
                        || (target.getTargetType() == Targetable.TYPE_BLDG_IGNITE)) {
                    handleIgnitionDamage(vPhaseReport, bldg, hits);
                    return false;
                }
                // targeting a hex for clearing
                if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {
                    nDamage = nDamPerHit * hits;
                    handleClearDamage(vPhaseReport, bldg, nDamage);
                    return false;
                }
                // Targeting a building.
                if (target.getTargetType() == Targetable.TYPE_BUILDING) {
                    // The building takes the full brunt of the attack.
                    nDamage = nDamPerHit * hits;
                    handleBuildingDamage(vPhaseReport, bldg, nDamage,
                            target.getPosition());
                    // And we're done!
                    return false;
                }
                if (entityTarget != null) {
                    handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                            nCluster, bldgAbsorbs);
                    gameManager.creditKill(entityTarget, ae);
                    hits -= nCluster;
                    firstHit = false;
                }
            } // Handle the next cluster.
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
        AmmoType atype = (AmmoType) ammo.getType();
        int av = 0;
        double counterAV = calcCounterAV();
        int armor = wtype.getMissileArmor();
        //AR10 munitions
        if (atype != null) {
            if (atype.getAmmoType() == AmmoType.T_AR10) {
                if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                    av = 4;
                    armor = 40;
                } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                    av = 3;
                    armor = 30;
                } else if (atype.hasFlag(AmmoType.F_PEACEMAKER)) {
                    av = 1000;
                    armor = 40;
                } else if (atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
                    av = 100;
                    armor = 30;
                } else {
                    av = 2;
                    armor = 20;
                }
            } else {
                int range = RangeType.rangeBracket(nRange, wtype.getATRanges(),
                        true, false);
                if (range == WeaponType.RANGE_SHORT) {
                    av = wtype.getRoundShortAV();
                } else if (range == WeaponType.RANGE_MED) {
                    av = wtype.getRoundMedAV();
                } else if (range == WeaponType.RANGE_LONG) {
                    av = wtype.getRoundLongAV();
                } else if (range == WeaponType.RANGE_EXT) {
                    av = wtype.getRoundExtAV();
                }
            }
            //Nuclear Warheads for non-AR10 missiles
            if (atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
                av = 100;
            } else if (atype.hasFlag(AmmoType.F_PEACEMAKER)) {
                av = 1000;
            }
            nukeS2S = atype.hasFlag(AmmoType.F_NUCLEAR);
        }
        // For squadrons, total the missile armor for the launched volley
        if (ae.isCapitalFighter()) {
            armor = armor * nweapons;
        }
        CapMissileArmor = armor - (int) counterAV;
        CapMissileAMSMod = calcCapMissileAMSMod();

        if (bDirect) {
            av = Math.min(av + (toHit.getMoS() / 3), av * 2);
        }

        av = applyGlancingBlowModifier(av, false);
        av = (int) Math.floor(getBracketingMultiplier() * av);

        return av;
    }

    /**
     * Calculate the damage per hit.
     *
     * @return an <code>int</code> representing the damage dealt per hit.
     */
    @Override
    protected int calcDamagePerHit() {
        AmmoType atype = (AmmoType) ammo.getType();
        double toReturn = wtype.getDamage(nRange);

        //AR10 munitions
        if (atype != null) {
            if (atype.getAmmoType() == AmmoType.T_AR10) {
                if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                    toReturn = 4;
                } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                    toReturn = 3;
                } else if (atype.hasFlag(AmmoType.F_PEACEMAKER)) {
                    toReturn = 1000;
                } else if (atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
                    toReturn = 100;
                } else {
                    toReturn = 2;
                }
            }
            //Nuclear Warheads for non-AR10 missiles
            if (atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
                toReturn = 100;
            } else if (atype.hasFlag(AmmoType.F_PEACEMAKER)) {
                toReturn = 1000;
            }
            nukeS2S = atype.hasFlag(AmmoType.F_NUCLEAR);
        }

        // we default to direct fire weapons for anti-infantry damage
        if (bDirect) {
            toReturn = Math.min(toReturn + (toHit.getMoS() / 3), toReturn * 2);
        }

        toReturn = applyGlancingBlowModifier(toReturn, false);

        return (int) toReturn;
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

    @Override
    protected int getCapMisMod() {
        AmmoType atype = (AmmoType) ammo.getType();
        return getCritMod(atype);
    }

    /*
     * get the cap mis mod given a single ammo type
     */
    protected int getCritMod(AmmoType atype) {
        if (atype == null || atype.getAmmoType() == AmmoType.T_PIRANHA
                || atype.getAmmoType() == AmmoType.T_AAA_MISSILE
                || atype.getAmmoType() == AmmoType.T_ASEW_MISSILE
                || atype.getAmmoType() == AmmoType.T_LAA_MISSILE) {
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

    /**
     * Checks to see if this point defense/AMS bay can engage a capital missile
     * This should return true. Only when handling capital missile attacks can this be false.
     */
    @Override
    protected boolean canEngageCapitalMissile(Mounted counter) {
        if (counter.getBayWeapons().size() < 2) {
            return false;
        } else {
            return true;
        }
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
}
