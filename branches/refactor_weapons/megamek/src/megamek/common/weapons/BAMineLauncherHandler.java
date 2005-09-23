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
/*
 * Created on Sep 23, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Mech;
import megamek.common.Report;
import megamek.common.Tank;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 * 
 */
public class BAMineLauncherHandler extends AmmoWeaponHandler {
    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public BAMineLauncherHandler(ToHitData toHit, WeaponAttackAction waa,
            IGame g, Server s) {
        super(toHit, waa, g, s);
    }

    protected void handleEntityDamage(Entity entityTarget, Vector vPhaseReport,
            Building bldg, int hits, int nCluster, int nDamPerHit,
            int bldgAbsorbs) {
        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(), toHit
                .getSideTable(), waa.getAimedLocation(), waa.getAimingMode());
        if ( target instanceof Mech ) {
            hit = new HitData( Mech.LOC_CT );
        }
        else { // te instanceof Tank
            hit = new HitData( Tank.LOC_FRONT );
        }
        // Do criticals.
        Vector specialDamageReport = server.criticalEntity(entityTarget, hit
                .getLocation());

        // Replace "no effect" results with 4 points of damage.
        if (((Report) specialDamageReport.lastElement()).messageId == 6005) {
            int damage = 4;
            // ASSUMPTION: buildings CAN'T absorb *this* damage.
            // specialDamage = damageEntity(entityTarget, hit, damage);
            specialDamageReport = server.damageEntity(entityTarget, hit,
                    damage, false, 0, false, false, throughFront);
        } else {
            // add newline _before_ last report
            try {
                ((Report) specialDamageReport.elementAt(specialDamageReport
                        .size() - 2)).newlines++;
            } catch (ArrayIndexOutOfBoundsException aiobe) {
                System.err
                        .println("ERROR: no previous report when trying to add newline");
            }
        }
        // Report the result
        Server.combineVectors(vPhaseReport, specialDamageReport);
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    protected int calcHits(Vector vPhaseReport) {
        int hits = weapon.howManyShots();
        if ( !this.allShotsHit() ) {
            hits = Compute.getBattleArmorHits( hits );
        }
        bSalvo = true;
        String sSalvoType = " mine(s) ";
        r = new Report(3325);
        r.subject = subjectId;
        r.add(hits);
        r.add(sSalvoType);
        r.add(toHit.getTableDesc());
        r.newlines = 0;
        vPhaseReport.addElement(r);
        r = new Report(3345);
        r.newlines = 0;
        vPhaseReport.addElement(r);
        return hits;
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#useAmmo()
     */
    protected void useAmmo() {
        setDone();
        checkAmmo();
        // how many shots are we firing?
        int nShots = weapon.howManyShots();

        // do we need to revert to single shot?
        if (nShots > 1) {
            int nAvail = ae.getTotalAmmoOfType(ammo.getType());
            while (nAvail < nShots) {
                nShots--;
            }
        }

        // use up ammo
        for (int i = 0; i < nShots; i++) {
            if (ammo.getShotsLeft() <= 0) {
                ae.loadWeaponWithSameAmmo(weapon);
                ammo = weapon.getLinked();
            }
            ammo.setShotsLeft(ammo.getShotsLeft() - 1);
        }
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcnCluster()
     */
    protected int calcnCluster() {
        return 1;
    }
}
