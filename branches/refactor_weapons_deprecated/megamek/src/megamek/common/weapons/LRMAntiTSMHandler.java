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
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Report;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 *
 */
public class LRMAntiTSMHandler extends LRMHandler {

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public LRMAntiTSMHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
        sSalvoType = " anti-TSM missile(s) ";
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            if (ae instanceof BattleArmor) {
                bSalvo = true;
                return ((BattleArmor)ae).getShootingStrength();
            }
            return 1;
        }
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
        int missilesHit;
        int nGlancing = 0;
        int nMissilesModifier = 0;
        boolean maxtechmissiles = game.getOptions().booleanOption("maxtech_mslhitpen");
        if (maxtechmissiles) {
            if (nRange<=1) {
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
        AmmoType atype = (AmmoType)ammo.getType();
        if ( (mLinker != null && mLinker.getType() instanceof MiscType &&
                !mLinker.isDestroyed() && !mLinker.isMissing() &&
                !mLinker.isBreached() && 
                mLinker.getType().hasFlag(MiscType.F_ARTEMIS) ) &&
                atype.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE &&
                !bECMAffected) {
            nSalvoBonus += 2;
        } else if (entityTarget != null && 
                (entityTarget.isNarcedBy(ae.getOwner().getTeam()) || 
                 entityTarget.isINarcedBy(ae.getOwner().getTeam()))) {
            // only apply Narc bonus if we're not suffering ECM effect
            // and we are using narc ammo.
            if (!bECMAffected
                    && !bMekStealthActive
                    && ((atype.getAmmoType() == AmmoType.T_LRM) || (atype.getAmmoType() == AmmoType.T_SRM))
                    && atype.getMunitionType() == AmmoType.M_NARC_CAPABLE) {
                nSalvoBonus += 2;
            }
        }
        if (bGlancing) {
            nGlancing -=4;
        }
        // anti tsm hit with half the normal number, round up
        missilesHit = Compute.missilesHit(wtype.getRackSize(), nSalvoBonus + nGlancing + nMissilesModifier , bGlancing || maxtechmissiles);
        missilesHit = (int)Math.ceil((double)missilesHit/2);
        r = new Report(3325);
        r.subject = subjectId;
        r.add(missilesHit);
        r.add(sSalvoType);
        r.add(toHit.getTableDesc());
        r.newlines = 0;
        vPhaseReport.addElement(r);
        if (bECMAffected) {
            //ECM prevents bonus
            r = new Report(3330);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        else if (bMekStealthActive) {
            //stealth prevents bonus
            r = new Report(3335);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        if (nSalvoBonus > 0) {
            r = new Report(3340);
            r.subject = subjectId;
            r.add(nSalvoBonus);
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        r = new Report(3345);
        r.newlines = 0;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#handleEntityDamage(megamek.common.Entity, java.util.Vector, megamek.common.Building, int, int, int, int)
     */
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int nDamPerHit, int bldgAbsorbs) {
        int nDamage;
        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(), toHit
                .getSideTable(), waa.getAimedLocation(), waa.getAimingMode());

        if (!bSalvo) {
            // Each hit in the salvo get's its own hit location.
            r = new Report(3405);
            r.subject = subjectId;
            r.add(toHit.getTableDesc());
            r.add(entityTarget.getLocationAbbr(hit));
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }

        if (hit.hitAimedLocation()) {
            r = new Report(3410);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        // Resolve damage normally.
        nDamage = nDamPerHit * Math.min(nCluster, hits);

        // A building may be damaged, even if the squad is not.
        if ( bldgAbsorbs > 0 ) {
            int toBldg = Math.min( bldgAbsorbs, nDamage );
            nDamage -= toBldg;
            Report.addNewline(vPhaseReport);
            Report buildingReport = server.damageBuilding( bldg, toBldg );
            buildingReport.indent(2);
            buildingReport.subject = subjectId;
            vPhaseReport.addElement(buildingReport);
        }

        // A building may absorb the entire shot.
        if ( nDamage == 0 ) {
            r = new Report(3415);
            r.subject = subjectId;
            r.indent(2);
            r.addDesc(entityTarget);
            r.newlines = 0;
            vPhaseReport.addElement(r);
        } else {
            if (bGlancing) {
                hit.makeGlancingBlow();
            }
            entityTarget.hitThisRoundByAntiTSM = true;
            vPhaseReport.addAll(
                    server.damageEntity(entityTarget, hit, nDamage, false, 0, false, false, throughFront));
        }
    }
}
