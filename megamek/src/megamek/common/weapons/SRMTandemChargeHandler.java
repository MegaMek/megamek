/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IArmorState;
import megamek.common.Game;
import megamek.common.Hex;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Report;
import megamek.common.Tank;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.common.enums.DamageType;

/**
 * @author Jason Tighe
 */
public class SRMTandemChargeHandler extends SRMHandler {
    private static final long serialVersionUID = 6292692766500970690L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public SRMTandemChargeHandler(ToHitData t, WeaponAttackAction w, Game g,
            Server s) {
        super(t, w, g, s);
        sSalvoType = " tandem charge missile(s) ";
        generalDamageType = HitData.DAMAGE_ARMOR_PIERCING_MISSILE;
    }

    /**
     * Handle damage against an entity, called once per hit by default.
     * 
     * @param entityTarget
     * @param vPhaseReport
     * @param bldg
     * @param hits
     * @param nCluster
     * @param bldgAbsorbs
     */
    @Override
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        int nDamage;
        missed = false;

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

        if (!bSalvo) {
            // Each hit in the salvo get's its own hit location.
            Report r = new Report(3405);
            r.subject = subjectId;
            r.add(toHit.getTableDesc());
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.addElement(r);
        }
        // Resolve damage normally.
        nDamage = nDamPerHit * Math.min(nCluster, hits);

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
            Report r = new Report(3415);
            r.subject = subjectId;
            r.indent(2);
            r.addDesc(entityTarget);
            r.newlines = 0;
            vPhaseReport.addElement(r);
            missed = true;
        } else {
            if (bGlancing) {
                hit.makeGlancingBlow();
            }
            
            if (bLowProfileGlancing) {
                hit.makeGlancingBlow();
            }
            
            if (bDirect && !target.isConventionalInfantry()) {
                hit.makeDirectBlow(toHit.getMoS() / 3);
            }

            if ((target instanceof BattleArmor)
                    && (((BattleArmor) target).getInternal(hit.getLocation()) != IArmorState.ARMOR_DOOMED)) {
                int critRoll = Compute.d6(2);
                int loc = hit.getLocation();
                if (critRoll >= 10) {
                    hit = new HitData(loc, false, HitData.EFFECT_CRITICAL);
                }
            } else if ((target instanceof Tank) || (target instanceof Mech)) {

                if (bGlancing || bLowProfileGlancing) {
                    // this will be either -4 or -8
                    hit.setSpecCritmod(-2 * (int) getTotalGlancingBlowFactor());
                } else if (bDirect) {
                    hit.setSpecCritmod((toHit.getMoS() / 3) - 2);
                } else {
                    hit.setSpecCritmod(-2);
                }
            }
            vPhaseReport
                    .addAll(server.damageEntity(entityTarget, hit, nDamage,
                            false, ae.getSwarmTargetId() == entityTarget
                                    .getId() ? DamageType.IGNORE_PASSENGER
                                    : DamageType.NONE, false, false,
                            throughFront, underWater));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        if (target.isConventionalInfantry()) {
            double toReturn = Compute.directBlowInfantryDamage(
                    wtype.getRackSize(), bDirect ? toHit.getMoS() / 3 : 0,
                    wtype.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, ae.getId(), calcDmgPerHitReport);

            toReturn = applyGlancingBlowModifier(toReturn, true);

            return (int) Math.floor(toReturn);
        }
        return 2;
    }

}
