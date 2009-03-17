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
 * Created on Sep 25, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Andrew Hunter
 */
public class ACAPHandler extends AmmoWeaponHandler {
    /**
     *
     */
    private static final long serialVersionUID = -4251291510045646817L;
    protected int generalDamageType = HitData.DAMAGE_ARMOR_PIERCING;

    /**
     * @param t
     * @param w
     * @param g
     */
    public ACAPHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }


    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn = wtype.getDamage();
        // during a swarm, all damage gets applied as one block to one
        // location
        if (ae instanceof BattleArmor && weapon.getLocation() == BattleArmor.LOC_SQUAD && (ae.getSwarmTargetId() == target.getTargetId())) {
            toReturn *= ((BattleArmor) ae).getShootingStrength();
        }
        // we default to direct fire weapons for anti-infantry damage
        if (target instanceof Infantry && !(target instanceof BattleArmor)) {
            toReturn = Compute.directBlowInfantryDamage(toReturn, bDirect ? toHit.getMoS()/3 : 0, Compute.WEAPON_DIRECT_FIRE, ((Infantry)target).isMechanized());
        } else if (bDirect){
            toReturn = Math.min(toReturn+(toHit.getMoS()/3), toReturn*2);
        }


        if (bGlancing) {
            toReturn = (int) Math.floor(toReturn / 2.0);
        }
        if (game.getOptions().booleanOption("tacops_range") && nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG]) {
            toReturn = (int) Math.floor(toReturn * .75);
        }

        return (int) toReturn;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#handleEntityDamage(megamek.common.Entity,
     *      java.util.Vector, megamek.common.Building, int, int, int, int)
     */
    @Override
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int nDamPerHit, int bldgAbsorbs) {
        AmmoType atype = (AmmoType) weapon.getLinked().getType();
        int nDamage;
        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(), toHit
                .getSideTable(), waa.getAimedLocation(), waa.getAimingMode());
        hit.setGeneralDamageType(generalDamageType);

        // Each hit in the salvo get's its own hit location.
        r = new Report(3405);
        r.subject = subjectId;
        r.add(toHit.getTableDesc());
        r.add(entityTarget.getLocationAbbr(hit));
        r.newlines = 0;
        vPhaseReport.addElement(r);
        if (hit.hitAimedLocation()) {
            r = new Report(3410);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        // Resolve damage normally.
        nDamage = nDamPerHit * Math.min(nCluster, hits);
        if ( bDirect && (!(target instanceof Infantry) || target instanceof BattleArmor) ){
            hit.makeDirectBlow(toHit.getMoS()/3);
        }

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
        } else {

            int critModifer = 0;


            if (bGlancing) {
                hit.makeGlancingBlow();
                critModifer -= 2;
            }else if ( bDirect ) {
                critModifer += toHit.getMoS()/3;

            }

            hit.makeArmorPiercing(atype,critModifer);
            vPhaseReport
                    .addAll(server.damageEntity(entityTarget, hit, nDamage,
                            false, ae.getSwarmTargetId() == entityTarget
                                    .getId() ? DamageType.IGNORE_PASSENGER
                                    : damageType, false, false, throughFront, underWater));
        }
    }
}
