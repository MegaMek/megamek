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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;

/**
 * @author Andrew Hunter
 *
 */
public class ACFlechetteHandler extends AmmoWeaponHandler {
	/**
	 * @param t
	 * @param w
	 * @param g
	 */
	public ACFlechetteHandler(ToHitData t, WeaponAttackAction w, Game g) {
		super(t, w, g);
	}
	
	/* (non-Javadoc)
	 * @see megamek.common.weapons.WeaponHandler#handleEntityDamage(megamek.common.Entity, java.lang.StringBuffer, megamek.common.Building, int, int, int, int)
	 */
	protected void handleEntityDamage(Entity entityTarget,
			StringBuffer phaseReport, Building bldg, int hits, int nCluster,
			int nDamPerHit, int bldgAbsorbs) {
		int nDamage;
		HitData hit = entityTarget.rollHitLocation
		     ( toHit.getHitTable(),
		       toHit.getSideTable(),
		       waa.getAimedLocation(),
		       waa.getAimingMode() );


		// Each hit in the salvo get's its own hit location.
		    phaseReport.append("hits" ).append( toHit.getTableDesc() ).append( " " ).
		            append( entityTarget.getLocationAbbr(hit));
		    if (hit.hitAimedLocation()) {
		    	phaseReport.append("(hit aimed location)");
		    }
		
		    // Resolve damage normally.
		    nDamage = nDamPerHit * Math.min(nCluster, hits);

		    // A building may be damaged, even if the squad is not.
		    if ( bldgAbsorbs > 0 ) {
		        int toBldg = Math.min( bldgAbsorbs, nDamage );
		        nDamage -= toBldg;
		        phaseReport.append( "\n        " )
		            .append( server.damageBuilding( bldg, toBldg ) );
		    }

		    // A building may absorb the entire shot.
		    if ( nDamage == 0 ) {
		        phaseReport.append( "\n        " )
		            .append( entityTarget.getDisplayName() )
		            .append( " suffers no damage." );
		    } else {	                    	            	
		    	phaseReport.append( server.damageEntity(entityTarget, hit, nDamage,false,2) );
		    }
	}
}
