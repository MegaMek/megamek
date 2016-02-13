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

package megamek.common.weapons.infantry;

import java.util.Vector;

import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IGame;
import megamek.common.Mech;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class InfantryHeatWeaponHandler extends InfantryWeaponHandler {

  
    /**
     * 
     */
    private static final long serialVersionUID = 8430370552107061610L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public InfantryHeatWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
        bSalvo = true;
    }
    
    @Override
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        if ((entityTarget instanceof Mech)
                && game.getOptions().booleanOption("flamer_heat")) {
            // heat
            hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                    toHit.getSideTable(), waa.getAimedLocation(), waa
                            .getAimingMode(), toHit.getCover());
            hit.setAttackerId(getAttackerId());

            if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit
                    .getCover(), Compute.targetSideTable(ae, entityTarget, weapon.getCalledShot().getCall()))) {           
                // Weapon strikes Partial Cover.            
                handlePartialCoverHit(entityTarget, vPhaseReport, hit, bldg, hits,
                        nCluster, bldgAbsorbs);
                return;
            }
            Report r = new Report(3400);
            r.subject = subjectId;
            r.indent(2);
            if (entityTarget.getArmor(hit) > 0 && 
                    (entityTarget.getArmorType(hit.getLocation()) == 
                    EquipmentType.T_ARMOR_HEAT_DISSIPATING)){
                entityTarget.heatFromExternal += nDamPerHit/2;
                r.add(nDamPerHit/2);
                r.choose(true);
                r.messageId=3406;
                r.add(EquipmentType.armorNames
                        [entityTarget.getArmorType(hit.getLocation())]);
            } else {
                entityTarget.heatFromExternal += nDamPerHit;
                r.add(nDamPerHit);
                r.choose(true);
            }
            vPhaseReport.addElement(r);
        } else {
            super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                    nCluster, bldgAbsorbs);
        }
    }
}
