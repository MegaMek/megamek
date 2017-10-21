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

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author MKerensky
 */
public class ASEWMissileWeaponHandler extends ThunderBoltWeaponHandler {

    /**
     *
     */
    private static final long serialVersionUID = 6359291710822171023L;

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public ASEWMissileWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
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
        int nDamage;
        missed = false;

        hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                toHit.getSideTable(), waa.getAimedLocation(),
                waa.getAimingMode(), toHit.getCover());
        hit.setGeneralDamageType(generalDamageType);
        hit.setCapital(wtype.isCapital());
        hit.setBoxCars(roll == 12);
        hit.setCapMisCritMod(getCapMisMod());
        hit.setFirstHit(firstHit);
        hit.setAttackerId(getAttackerId());
        if (weapon.isWeaponGroup()) {
            hit.setSingleAV(attackValue);
        }
        if (entityTarget instanceof Aero) {
            Aero a = (Aero) entityTarget;
                a.setASEWAffected(hit.getLocation(), 2);
        }
        
        if (!bSalvo) {
            // Each hit in the salvo get's its own hit location.
            Report r = new Report(3405);
            r.subject = subjectId;
            r.add(toHit.getTableDesc());
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.addElement(r);
            if (weapon.isRapidfire()) {
                r.newlines = 0;
                r = new Report(3225);
                r.subject = subjectId;
                r.add(numRapidFireHits * 3);
                vPhaseReport.add(r);
            }
        } else {
            Report.addNewline(vPhaseReport);
        }

        // for non-salvo shots, report that the aimed shot was successfull
        // before applying damage
        if (hit.hitAimedLocation() && !bSalvo) {
            Report r = new Report(3410);
            r.subject = subjectId;
            vPhaseReport.lastElement().newlines = 0;
            vPhaseReport.addElement(r);
        }
        // Resolve damage normally.
        nDamage = nDamPerHit * Math.min(nCluster, hits);

        if (bDirect) {
            hit.makeDirectBlow(toHit.getMoS() / 3);
        }
        
        // Report calcDmgPerHitReports here
        if (calcDmgPerHitReport.size() > 0) {
            vPhaseReport.addAll(calcDmgPerHitReport);
        }
    
        // A building may be damaged, even if the squad is not.
        if (bldgAbsorbs > 0) {
            int toBldg = Math.min(bldgAbsorbs, nDamage);
            nDamage -= toBldg;
            Report.addNewline(vPhaseReport);
            Vector<Report> buildingReport = server.damageBuilding(bldg, toBldg,
                    entityTarget.getPosition());
            for (Report report : buildingReport) {
                report.subject = subjectId;
            }
            vPhaseReport.addAll(buildingReport);
        // Units on same level, report building absorbs no damage
        } else if (bldgAbsorbs == Integer.MIN_VALUE) {
            Report.addNewline(vPhaseReport);
            Report r = new Report(9976);
            r.subject = ae.getId();
            r.indent(2);
            vPhaseReport.add(r);
        // Cases where absorbed damage doesn't reduce incoming damage
        } else if (bldgAbsorbs < 0) {
            int toBldg = -bldgAbsorbs;
            Report.addNewline(vPhaseReport);
            Vector<Report> buildingReport = server.damageBuilding(bldg, toBldg,
                    entityTarget.getPosition());
            for (Report report : buildingReport) {
                report.subject = subjectId;
            }
            vPhaseReport.addAll(buildingReport);
        }

        nDamage = checkTerrain(nDamage, entityTarget, vPhaseReport);
        nDamage = checkLI(nDamage, entityTarget, vPhaseReport);

        // some buildings scale remaining damage that is not absorbed
        // TODO: this isn't quite right for castles brian
        if (null != bldg) {
            nDamage = (int) Math.floor(bldg.getDamageToScale() * nDamage);
        }

        // A building may absorb the entire shot.
        if (nDamage == 0) {
            Report r = new Report(3415);
            r.subject = subjectId;
            r.indent(2);
            r.addDesc(entityTarget);
            vPhaseReport.addElement(r);
            missed = true;
        } else {
            if (bGlancing) {
                hit.makeGlancingBlow();
            }
            vPhaseReport
                    .addAll(server.damageEntity(entityTarget, hit, nDamage,
                            false, ae.getSwarmTargetId() == entityTarget
                                    .getId() ? DamageType.IGNORE_PASSENGER
                                    : damageType, false, false, throughFront,
                            underWater, nukeS2S));
            // for salvo shots, report that the aimed location was hit after
            // applying damage, because the location is first reported when
            // dealing the damage
            if (hit.hitAimedLocation() && bSalvo) {
                Report r = new Report(3410);
                r.subject = subjectId;
                vPhaseReport.lastElement().newlines = 0;
                vPhaseReport.addElement(r);
            }
        }
        // If a BA squad is shooting at infantry, damage may be random and need
        // to be rerolled for the next hit (if any) from the same attack.
        if ((ae instanceof BattleArmor) && (target instanceof Infantry)) {
            nDamPerHit = calcDamagePerHit();
        }
    }
    
    /**
     * Calculate the attack value based on range
     *
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {
        calcCounterAV();
        int av = 0;
        return av;
    }

}
