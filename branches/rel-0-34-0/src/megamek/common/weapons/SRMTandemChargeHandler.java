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

import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IArmorState;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Report;
import megamek.common.Tank;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Jason Tighe
 */
public class SRMTandemChargeHandler extends SRMHandler {

    /**
     *
     */
    private static final long serialVersionUID = 6292692766500970690L;
    protected int generalDamageType = HitData.DAMAGE_ARMOR_PIERCING_MISSILE;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public SRMTandemChargeHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
        sSalvoType = " tandem charge missile(s) ";
    }

    /**
     * Handle damage against an entity, called once per hit by default.
     *
     * @param entityTarget
     * @param vPhaseReport
     * @param bldg
     * @param hits
     * @param nCluster
     * @param nDamPerHit
     * @param bldgAbsorbs
     */
    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster, int nDamPerHit, int bldgAbsorbs) {
        int nDamage;
        missed = false;

        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(), toHit.getSideTable(), waa.getAimedLocation(), waa.getAimingMode());
        hit.setGeneralDamageType(generalDamageType);
        if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit.getCover(), Compute.targetSideTable(ae, entityTarget))) {
            // Weapon strikes Partial Cover.
            r = new Report(3460);
            r.subject = subjectId;
            r.add(entityTarget.getShortName());
            r.add(entityTarget.getLocationAbbr(hit));
            r.newlines = 0;
            r.indent(2);
            vPhaseReport.addElement(r);
            nDamage = 0;
            missed = true;
            return;
        }

        if (!bSalvo) {
            // Each hit in the salvo get's its own hit location.
            r = new Report(3405);
            r.subject = subjectId;
            r.add(toHit.getTableDesc());
            r.add(entityTarget.getLocationAbbr(hit));
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        // Resolve damage normally.
        nDamage = nDamPerHit * Math.min(nCluster, hits);

        // A building may be damaged, even if the squad is not.
        if (bldgAbsorbs > 0) {
            int toBldg = Math.min(bldgAbsorbs, nDamage);
            nDamage -= toBldg;
            Report.addNewline(vPhaseReport);
            Vector<Report> buildingReport = server.damageBuilding(bldg, toBldg, entityTarget.getPosition());
            for (Report report : buildingReport) {
                report.subject = subjectId;
            }
            vPhaseReport.addAll(buildingReport);
        }

        nDamage = checkTerrain(nDamage, entityTarget,vPhaseReport);

        // A building may absorb the entire shot.
        if (nDamage == 0) {
            r = new Report(3415);
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
            if ( bDirect && (!(target instanceof Infantry) || (target instanceof BattleArmor))){
                hit.makeDirectBlow(toHit.getMoS()/3);
            }

            if ( (target instanceof BattleArmor) && (((BattleArmor)target).getInternal(hit.getLocation()) != IArmorState.ARMOR_DOOMED) ){
                int roll = Compute.d6(2);
                int loc = hit.getLocation();
                if ( roll >= 10 ){
                    hit = new HitData(loc, false, HitData.EFFECT_CRITICAL);
                }
            } else if ((target instanceof Tank) || (target instanceof Mech)) {

                if ( bGlancing ) {
                    hit.setSpecCritmod(-4);
                } else if ( bDirect ) {
                    hit.setSpecCritmod((toHit.getMoS()/3)-2);
                } else {
                    hit.setSpecCritmod(-2);
                }
                if (entityTarget.hasBARArmor()) {
                    hit.setSpecCritmod(hit.getSpecCritMod()+2);
                }
            }
            vPhaseReport.addAll(server.damageEntity(entityTarget, hit, nDamage, false, ae.getSwarmTargetId() == entityTarget.getId() ? DamageType.IGNORE_PASSENGER : DamageType.NONE, false, false, throughFront, underWater));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)) {
            double toReturn = Compute.directBlowInfantryDamage(wtype.getRackSize(), bDirect ? toHit.getMoS() / 3 : 0, Compute.WEAPON_CLUSTER_MISSILE, ((Infantry)target).isMechanized());
            if (bGlancing) {
                toReturn /= 2;
            }
            return (int)Math.floor(toReturn);
        }
        return 2;
    }

}
