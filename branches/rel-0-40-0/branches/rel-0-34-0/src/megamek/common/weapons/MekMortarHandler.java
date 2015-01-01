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

import java.util.Vector;

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jason Tighe
 */
public class MekMortarHandler extends AmmoWeaponHandler {

    /**
     *
     */
    private static final long serialVersionUID = -2073773899108954657L;
    String sSalvoType =" shell(s) ";

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public MekMortarHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
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
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            return 1;
        }

        boolean targetHex = target.getTargetType() == Targetable.TYPE_HEX_CLEAR
                            || target.getTargetType() == Targetable.TYPE_HEX_IGNITE;
        int missilesHit;
        int nMissilesModifier = 0;

        if (targetHex){
            missilesHit = wtype.getRackSize();
        }
        else {
            if (game.getOptions().booleanOption("tacops_range") && nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG]) {
                nMissilesModifier -= 2;
            }

            if (bGlancing) {
                nMissilesModifier -= 4;
            }else if (bDirect) {
                nMissilesModifier += (toHit.getMoS() / 3) * 2;
            }

            missilesHit = Compute.missilesHit(wtype.getRackSize(), nMissilesModifier);
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

    /**
     * Calculate the clustering of the hits
     *
     * @return a <code>int</code> value saying how much hits are in each
     *         cluster of damage.
     */
    @Override
    protected int calcnCluster() {
        return 2;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#specialResolution(java.util.Vector, megamek.common.Entity, boolean)
     */
    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget, boolean bMissed) {
        // targeting a hex for clearing
        if (target.getTargetType() == Targetable.TYPE_HEX_CLEAR) {
            int nDamage = nDamPerHit *calcHits(vPhaseReport);
            int[] damages = { nDamage };
            Building bldg = game.getBoard().getBuildingAt(target.getPosition());
            handleClearDamage(vPhaseReport, bldg, nDamage, bSalvo);
            server.doExplosion(damages, false, target.getPosition(), true, vPhaseReport, null, calcnCluster());
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            double toReturn = Compute.directBlowInfantryDamage(wtype.getRackSize(), bDirect ? toHit.getMoS()/3 : 0, Compute.WEAPON_CLUSTER_MISSILE, ((Infantry)target).isMechanized());
            if (bGlancing) {
                toReturn /= 2;
            }
            return (int)Math.floor(toReturn);
        }
        return 2;
    }


}
