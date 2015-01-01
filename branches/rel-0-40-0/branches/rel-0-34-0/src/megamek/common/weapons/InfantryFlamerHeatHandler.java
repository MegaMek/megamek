/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class InfantryFlamerHeatHandler extends InfantryFlamerHandler {

    /**
     * 
     */
    private static final long serialVersionUID = -4808077901373647109L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public InfantryFlamerHeatHandler(ToHitData t, WeaponAttackAction w,
            IGame g, Server s) {
        super(t, w, g, s);
    }
    
    

    /*
     * (non-Javadoc)
     * @see megamek.common.weapons.InfantryWeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        int troopersHit = Compute.missilesHit(((Infantry) ae)
                .getShootingStrength());
        return damage[troopersHit - 1];
    }
    
    /*
     * (non-Javadoc)
     * @see megamek.common.weapons.InfantryWeaponHandler#calcHits(java.util.Vector)
     */
    protected int calcHits(Vector<Report> vPhaseReport) {
        return 1;
    }

    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int nDamPerHit, int bldgAbsorbs) {
        if (entityTarget instanceof Mech
                && game.getOptions().booleanOption("flamer_heat")) {
            // heat
            HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                    toHit.getSideTable(), waa.getAimedLocation(), waa
                            .getAimingMode());

            if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit
                    .getCover(), Compute.targetSideTable(ae, entityTarget))) {
                // Weapon strikes Partial Cover.
                r = new Report(3460);
                r.subject = subjectId;
                r.add(entityTarget.getShortName());
                r.add(entityTarget.getLocationAbbr(hit));
                r.newlines = 0;
                r.indent(2);
                vPhaseReport.addElement(r);
                missed = true;
                return;
            }
            r = new Report(3400);
            r.subject = subjectId;
            r.indent(2);
            r.add(hits);
            r.newlines = 0;
            r.choose(true);
            vPhaseReport.addElement(r);
            entityTarget.heatFromExternal += hits;
        } else {
            super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                    nCluster, nDamPerHit, bldgAbsorbs);
        }
    }
}
