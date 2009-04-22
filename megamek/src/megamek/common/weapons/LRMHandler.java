/**
 * MegaMek - Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Minefield;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class LRMHandler extends MissileWeaponHandler {

    /**
     *
     */
    private static final long serialVersionUID = -9160255801810263821L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public LRMHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#specialResolution(java.util.Vector,
     *      megamek.common.Entity, boolean)
     */
    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport,
            Entity entityTarget, boolean bMissed) {
        if (!bMissed && (target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR)) {
            // minefield clearance attempt
            r = new Report(3255);
            r.indent(1);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
            Coords coords = target.getPosition();

            Enumeration<Minefield> minefields = game.getMinefields(coords).elements();
            ArrayList<Minefield> mfRemoved = new ArrayList<Minefield>();
            while (minefields.hasMoreElements()) {
                Minefield mf = minefields.nextElement();
                if(server.clearMinefield(mf, ae, Minefield.CLEAR_NUMBER_WEAPON, vPhaseReport)) {
                    mfRemoved.add(mf);
                }
            }
            //we have to do it this way to avoid a concurrent error problem
            for(Minefield mf : mfRemoved) {
                server.removeMinefield(mf);
            }
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
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
                return ((BattleArmor) ae).getShootingStrength();
            }
            r = new Report(3325);
            r.subject = subjectId;
            r.add(wtype.getRackSize());
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            vPhaseReport.add(r);
            return 1;
        }
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
        int missilesHit;
        int nMissilesModifier = nSalvoBonus;

        if ( game.getOptions().booleanOption("tacops_range") && (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG]) ) {
            nMissilesModifier -= 2;
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

        if (((mLinker != null) && (mLinker.getType() instanceof MiscType)
                && !mLinker.isDestroyed() && !mLinker.isMissing()
                && !mLinker.isBreached() && mLinker.getType().hasFlag(
                MiscType.F_ARTEMIS))
                && (atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE)) {
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
            } else {
                nMissilesModifier += 2;
            }
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
            } else {
                nMissilesModifier += 2;
            }
        } else if ((entityTarget != null)
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
                 ((atype.getAmmoType() == AmmoType.T_MML)
                    && (atype.getMunitionType() == AmmoType.M_NARC_CAPABLE)
                    && ((weapon.curMode() == null) || !weapon.curMode().equals(
                            "Indirect")))) {
                if (bTargetECMAffected) {
                    // ECM prevents bonus
                    r = new Report(3330);
                    r.subject = subjectId;
                    r.newlines = 0;
                    vPhaseReport.addElement(r);
                } else {
                    nMissilesModifier += 2;
                }
            }
        }
        if (bGlancing) {
            nMissilesModifier -= 4;
        }

        if ( bDirect ){
            nMissilesModifier += (toHit.getMoS()/3)*2;
        }

        if(game.getPlanetaryConditions().hasEMI()) {
            nMissilesModifier -= 2;
        }

        // add AMS mods
        nMissilesModifier += getAMSHitsMod(vPhaseReport);

        if (allShotsHit()) {
            missilesHit = wtype.getRackSize();
        } else {
            if (ae instanceof BattleArmor) {
                missilesHit = Compute.missilesHit(wtype.getRackSize()
                        * ((BattleArmor) ae).getShootingStrength(),
                        nMissilesModifier, weapon.isHotLoaded(), false, advancedAMS);
            } else {
                missilesHit = Compute.missilesHit(wtype.getRackSize(),
                        nMissilesModifier, weapon.isHotLoaded(), false, advancedAMS);
            }
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
                if (nMissilesModifier > 0) {
                    r = new Report(3340);
                } else {
                    r = new Report(3341);
                }
                r.subject = subjectId;
                r.add(nMissilesModifier);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        }
        r = new Report(3345);
        r.subject = subjectId;
        r.newlines = 0;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }
}
