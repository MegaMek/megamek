/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 * Created on Oct 20, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;


/**
 * @author Andrew Hunter
 *
 */
public class MissileHandler extends AmmoWeaponHandler {
    
    
    protected void handleAccidentalBuildingDamage(StringBuffer phaseReport,
            Building bldg, int hits, int nDamPerHit) {
//      Reduce the number of hits by AMS hits.
        if (this.amsShotDownTotal > 0) {
            for (int i=0; i < this.amsShotDown.length; i++) {
                int shotDown = Math.min(this.amsShotDown[i], hits);
                phaseReport.append("\tAMS shoots down ")
                        .append(shotDown).append(" missile(s).\n");
            }
            hits -= this.amsShotDownTotal;
        }
        super.handleAccidentalBuildingDamage(phaseReport, bldg, hits,
                nDamPerHit);
    }
    int[] amsShotDown = new int[0];
    public int amsShotDownTotal = 0;
    
    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public MissileHandler(ToHitData t, WeaponAttackAction w, Game g, Server s) {
        super(t, w, g, s);
        handleAMS();
    }
    
    protected void handleAMS() {
        final Entity te = game.getEntity(waa.getTargetId());

        // any AMS attacks by the target?
        Vector vCounters = waa.getCounterEquipment();
        if (null != vCounters) {
            // resolve AMS counter-fire
            this.amsShotDown = new int[vCounters.size()];
            for (int x = 0; x < vCounters.size(); x++) {
                this.amsShotDown[x] = 0;

                Mounted counter = (Mounted)vCounters.elementAt(x);
                Mounted mAmmo = counter.getLinked();
                if (!(counter.getType() instanceof WeaponType)
                || ((WeaponType)counter.getType()).getAmmoType() != AmmoType.T_AMS
                || !counter.isReady() || counter.isMissing()) {
                    continue;
                }
                // roll hits
                int amsHits = Compute.d6(((WeaponType)counter.getType()).getDamage());

                // build up some heat (assume target is ams owner)
                te.heatBuildup += ((WeaponType)counter.getType()).getHeat();

                // decrement the ammo
                mAmmo.setShotsLeft(Math.max(0, mAmmo.getShotsLeft() - amsHits));

                // set the ams as having fired
                counter.setUsedThisRound(true);

                this.amsShotDown[x]    = amsHits;
                this.amsShotDownTotal += amsHits;
            };
        };
    }
    

	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#allShotsHit()
	 */
	protected boolean allShotsHit() {
		if( ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
                target.getTargetType() == Targetable.TYPE_BUILDING ) &&
              ae.getPosition().distance( target.getPosition() ) <= 1 ) {
			return true;
		}
		return false;
	}
	protected int calcDamagePerHit() {
		return 1;
	}
	protected int calcHits() {
	    int missiles=((MissileWeapon)wtype).getNumMissiles();
	    int missilesHit=allShotsHit()? missiles : Compute.missilesHit(missiles);
	    game.getPhaseReport().append(missilesHit + " missile(s) hit");
	    return ((MissileWeapon)wtype).getDamagePerMissile()*missilesHit;
	}
	protected int calcnCluster() {
		return ((MissileWeapon)wtype).getDamageCluster();
	}
	

    protected boolean handleSpecialMiss(Entity entityTarget,
            boolean targetInBuilding, Building bldg) {
        // Report any AMS action.
        for (int i=0; i < this.amsShotDown.length; i++) {
            if (this.amsShotDown[i] > 0) {
                game.getPhaseReport().append( "\tAMS activates, firing " )
                    .append( this.amsShotDown[i] )
                    .append( " shot(s).\n" );
            }
        }

        // Figure out the maximum number of missile hits.
        // TODO: handle this in a different place.
        int maxMissiles = wtype.getRackSize();
        
        // If the AMS shot down *all* incoming missiles, if
        // the shot is an automatic failure, or if it's from
        // a Streak rack, then Infernos can't ignite the hex
        // and any building is safe from damage.
        if ( (this.amsShotDownTotal >= maxMissiles)) {
            return false;
        }

        
        return super.handleSpecialMiss(entityTarget, targetInBuilding, bldg);
    }
}
