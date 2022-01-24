/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.Game;
import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Andrew Hunter
 * @since Sep 25, 2004
 */
public class ACAPHandler extends ACWeaponHandler {
    private static final long serialVersionUID = -4251291510045646817L;

    /**
     * @param t
     * @param w
     * @param g
     */
    public ACAPHandler(ToHitData t, WeaponAttackAction w, Game g, Server s) {
        super(t, w, g, s);
        generalDamageType = HitData.DAMAGE_ARMOR_PIERCING;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.WeaponHandler#handleEntityDamage(megamek.common
     * .Entity, java.util.Vector, megamek.common.Building, int, int, int, int)
     */
    @Override
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        AmmoType atype = (AmmoType) weapon.getLinked().getType();
        int nDamage;
        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                toHit.getSideTable(), waa.getAimedLocation(),
                waa.getAimingMode(), toHit.getCover());
        hit.setGeneralDamageType(generalDamageType);
        hit.setAttackerId(getAttackerId());
        if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit
                .getCover(), Compute.targetSideTable(ae, entityTarget, weapon
                .getCalledShot().getCall()))) {
            // Weapon strikes Partial Cover.
            handlePartialCoverHit(entityTarget, vPhaseReport, hit, bldg, hits,
                                  nCluster, bldgAbsorbs);
            return;
        }

        // Each hit in the salvo get's its own hit location.
        Report r = new Report(3405);
        r.subject = subjectId;
        r.add(toHit.getTableDesc());
        r.add(entityTarget.getLocationAbbr(hit));
        vPhaseReport.addElement(r);
        if (hit.hitAimedLocation()) {
            r = new Report(3410);
            r.subject = subjectId;
            vPhaseReport.lastElement().newlines = 0;
            vPhaseReport.addElement(r);
        }
        // Resolve damage normally.
        nDamage = nDamPerHit * Math.min(nCluster, hits);
        if (bDirect && !target.isConventionalInfantry()) {
            hit.makeDirectBlow(toHit.getMoS() / 3);
        }

        // Report calcDmgPerHitReports here
        if (calcDmgPerHitReport.size() > 0) {
            vPhaseReport.addAll(calcDmgPerHitReport);
        }


        // if the target was in partial cover, then we already handled
        // damage absorption by the partial cover, if it would have happened
        Hex targetHex = game.getBoard().getHex(target.getPosition());
        boolean targetStickingOutOfBuilding = unitStickingOutOfBuilding(targetHex, entityTarget);
                
        nDamage = absorbBuildingDamage(nDamage, entityTarget, bldgAbsorbs, 
                vPhaseReport, bldg, targetStickingOutOfBuilding);

        nDamage = checkTerrain(nDamage, entityTarget, vPhaseReport);

        // some buildings scale remaining damage that is not absorbed
        // TODO: this isn't quite right for castles brian
        if ((null != bldg) && !targetStickingOutOfBuilding) {
            nDamage = (int) Math.floor(bldg.getDamageToScale() * nDamage);
        }

        // A building may absorb the entire shot.
        if (nDamage == 0) {
            r = new Report(3415);
            r.subject = subjectId;
            r.indent(2);
            r.addDesc(entityTarget);
            r.newlines = 0;
            vPhaseReport.addElement(r);
        } else {
            int critModifier = 0;
            if (bGlancing) {
                hit.makeGlancingBlow();
                critModifier -= 2;
            }
            
            if (bLowProfileGlancing) {
                hit.makeGlancingBlow();
                critModifier -= 2;
            }
            
            if (bDirect) {
                critModifier += toHit.getMoS() / 3;
            }
            hit.makeArmorPiercing(atype, critModifier);
            vPhaseReport
                    .addAll(server.damageEntity(entityTarget, hit, nDamage,
                            false, ae.getSwarmTargetId() == entityTarget
                                    .getId() ? DamageType.IGNORE_PASSENGER
                                    : damageType, false, false, throughFront,
                            underWater));
        }
    }
}
