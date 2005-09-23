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

import megamek.common.Compute;
import megamek.common.IGame;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 *
 */
public class StreakSRMHandler extends SRMHandler {
    
    boolean isAngelECMAffected = Compute.isAffectedByAngelECM(ae, ae.getPosition(), target.getPosition());

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public StreakSRMHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    protected int calcHits(Vector vPhaseReport) {
        int missilesHit = wtype.getRackSize();
        int newMissilesHit = missilesHit - getAMSShotDown(vPhaseReport);
        if (amsShotDownTotal > 0) {
            for (int i=0; i < amsShotDown.length; i++) {
                int shotDown = Math.min(amsShotDown[i], missilesHit);
                r = new Report(3350);
                r.indent();
                r.subject = subjectId;
                r.add(amsShotDown[i]);
                r.add(shotDown);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
            Report.addNewline(vPhaseReport);
            if (newMissilesHit < 1) {
                //all missiles shot down
                r = new Report(3355);
                r.indent();
                r.subject = subjectId;
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                r = new Report(3360);
                r.indent();
                r.subject = subjectId;
                r.add(newMissilesHit);
                r.newlines = 0;
                vPhaseReport.addElement(r);
            }
        }
        if (newMissilesHit > 0) {
            r = new Report(3325);
            r.subject = subjectId;
            r.add(newMissilesHit);
            r.add(sSalvoType);
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        r = new Report(3345);
        r.newlines = 0;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return newMissilesHit;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#UseAmmo()
     */
    protected void useAmmo() {
        checkAmmo();
        if (ammo == null) {// Can't happen. w/o legal ammo, the weapon
                            // *shouldn't* fire.
            System.out.println("Handler can't find any ammo!  Oh no!");
        }
        if (ammo.getShotsLeft() <= 0) {
            ae.loadWeapon(weapon);
            ammo = weapon.getLinked();
        }
        if (this.roll >= this.toHit.getValue()) {
            ammo.setShotsLeft(ammo.getShotsLeft() - 1);
            setDone();
        }
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#reportMiss(java.util.Vector)
     */
    protected void reportMiss(Vector vPhaseReport) {
        if (!isAngelECMAffected) {
            //no lock
            Report r = new Report(3215);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        } else {
            super.reportMiss(vPhaseReport);
        }
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#addHeat()
     */
    protected void addHeat() {
        if (!(toHit.getValue() == TargetRoll.IMPOSSIBLE)
                && this.roll >= this.toHit.getValue()) {
            ae.heatBuildup += (wtype.getHeat());
        }
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#allShotsHit()
     */
    protected boolean allShotsHit() {
        return super.allShotsHit() || !isAngelECMAffected;
    }
}
