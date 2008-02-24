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

import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Sebastian Brocks
 *
 */
public class LRMAntiTSMHandler extends LRMHandler {

    /**
     * 
     */
    private static final long serialVersionUID = 5702089152489814687L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public LRMAntiTSMHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
        sSalvoType = " anti-TSM missile(s) ";
        damageType = DamageType.ANTI_TSM;
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
        int missilesHit;
        int nMissilesModifier = 0;
        boolean bWeather = false;
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
        if (bGlancing) {
            nMissilesModifier -=4;
        }
        
        // weather checks
        if (game.getOptions().booleanOption("blizzard") && wtype.hasFlag(WeaponType.F_MISSILE)) {
            nMissilesModifier -= 4;
            bWeather = true;
        }

        if (game.getOptions().booleanOption("moderate_winds") && wtype.hasFlag(WeaponType.F_MISSILE)) {
            nMissilesModifier -= 2;
            bWeather = true;
        }
        
        if (game.getOptions().booleanOption("high_winds")  && wtype.hasFlag(WeaponType.F_MISSILE)) {
            nMissilesModifier -= 4;
            bWeather = true;
        }
        
        //AMS mod
        nMissilesModifier += getAMSHitsMod(vPhaseReport);
        if (allShotsHit())
            missilesHit = wtype.getRackSize();            
        else {
            // anti tsm hit with half the normal number, round up
            missilesHit = Compute.missilesHit(wtype.getRackSize(), nMissilesModifier , bWeather || bGlancing || maxtechmissiles);
            missilesHit = (int)Math.ceil((double)missilesHit/2);
        }
        r = new Report(3325);
        r.subject = subjectId;
        r.add(missilesHit);
        r.add(sSalvoType);
        r.add(toHit.getTableDesc());
        r.newlines = 0;
        vPhaseReport.addElement(r);
        if (bMekStealthActive) {
            //stealth prevents bonus
            r = new Report(3335);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
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
        r = new Report(3345);
        r.newlines = 0;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return missilesHit;
    }
}
