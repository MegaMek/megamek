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
 * Created on Sep 23, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.*;
import megamek.server.Server;
/**
 * @author Andrew Hunter
 *
 */
public class FlamerHeatHandler extends WeaponHandler {
	/**
	 * @param toHit
	 * @param waa
	 * @param g
	 */
	public FlamerHeatHandler(ToHitData toHit, WeaponAttackAction waa, Game g,Server s) {
		super(toHit, waa, g,s);
	}
	protected void handleEntityDamage(Entity entityTarget, StringBuffer phaseReport, Building bldg, int hits, int nCluster, int nDamPerHit, int bldgAbsorbs) {
		if(entityTarget instanceof Mech && game.getOptions().booleanOption("flamer_heat")) {
			//heat
			int nDamage = nDamPerHit * hits;
            phaseReport.append( "hits." );
            phaseReport.append("\n        Target gains ").append(nDamage).append(" more heat during heat phase.");
            entityTarget.heatBuildup += nDamage;
            hits = 0;
		} else
		{
			super.handleEntityDamage(entityTarget, phaseReport, bldg, hits, nCluster, nDamPerHit, bldgAbsorbs);
		}
	}
}
