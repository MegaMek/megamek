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

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.DamageType;
import megamek.common.weapons.Weapon;
import megamek.server.GameManager;
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
    public InfantryHeatWeaponHandler(ToHitData t, WeaponAttackAction w, Game g,
            GameManager m) {
        super(t, w, g, m);
        bSalvo = true;
    }
    
    @Override
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        if (entityTarget.tracksHeat()) {
            Report.addNewline(vPhaseReport);
            // heat
            hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                    toHit.getSideTable(), waa.getAimedLocation(), waa
                            .getAimingMode(), toHit.getCover());
            hit.setAttackerId(getAttackerId());

            Hex targetHex = game.getBoard().getHex(target.getPosition());
            boolean indirect = weapon.hasModes() && weapon.curMode().getName().equals(Weapon.MODE_INDIRECT_HEAT);
            boolean partialCoverForIndirectFire = indirect &&
                    (unitGainsPartialCoverFromWater(targetHex, entityTarget)
                            || (WeaponAttackAction.targetInShortCoverBuilding(target) && entityTarget.locationIsLeg(hit.getLocation())));

            if ((!indirect || partialCoverForIndirectFire)
                    && entityTarget.removePartialCoverHits(hit.getLocation(), toHit
                    .getCover(), Compute.targetSideTable(ae, entityTarget, weapon.getCalledShot().getCall()))) {           
                // Weapon strikes Partial Cover.            
                handlePartialCoverHit(entityTarget, vPhaseReport, hit, bldg, hits,
                        nCluster, bldgAbsorbs);
                return;
            }

            Report r = new Report(3400);
            r.subject = subjectId;
            r.indent(2);
            int nDamage = nDamPerHit * Math.min(nCluster, hits);
            // Building may absorb some damage.
            boolean targetStickingOutOfBuilding = unitStickingOutOfBuilding(targetHex, entityTarget);
            nDamage = absorbBuildingDamage(nDamage, entityTarget, bldgAbsorbs,
                    vPhaseReport, bldg, targetStickingOutOfBuilding);
            nDamage = checkTerrain(nDamage, entityTarget, vPhaseReport);
            nDamage = checkLI(nDamage, entityTarget, vPhaseReport);
            if ((bldg != null) && !targetStickingOutOfBuilding) {
                nDamage = (int) Math.floor(bldg.getDamageToScale() * nDamage);
            }

            // If using BMM heat option, do damage as well as heat
            if (game.getOptions().booleanOption(OptionsConstants.BASE_INFANTRY_DAMAGE_HEAT)) {
                vPhaseReport.addAll(gameManager.damageEntity(entityTarget, hit, nDamage, false,
                        ae.getSwarmTargetId() == entityTarget.getId() ? DamageType.IGNORE_PASSENGER : damageType,
                        false, false, throughFront, underWater, nukeS2S));
            }
            if (entityTarget.getArmor(hit) > 0 &&
                    (entityTarget.getArmorType(hit.getLocation()) == 
                    EquipmentType.T_ARMOR_HEAT_DISSIPATING)) {
                entityTarget.heatFromExternal += nDamage / 2;
                r.add(nDamage / 2);
                r.choose(true);
                r.messageId=3406;
                r.add(EquipmentType.armorNames
                        [entityTarget.getArmorType(hit.getLocation())]);
            } else {
                entityTarget.heatFromExternal += nDamage;
                r.add(nDamage);
                r.choose(true);
            }
            vPhaseReport.addElement(r);
        } else {
            super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                    nCluster, bldgAbsorbs);
        }
    }
}
