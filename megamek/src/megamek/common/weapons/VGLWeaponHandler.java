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
package megamek.common.weapons;

import java.util.ArrayList;
import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.BuildingTarget;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.HexTarget;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Report;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * Weapon handler for vehicular grenade launchers.  Rather than have a separate
 * handler for each ammo type, all ammo types are handled here.
 * 
 * @author arlith
 */
public class VGLWeaponHandler extends AmmoWeaponHandler {

    private static final long serialVersionUID = -4934490646657484486L;

    protected VGLWeaponHandler() {
    }

    /**
     * @param t
     * @param w
     * @param g
     */
    public VGLWeaponHandler(ToHitData t, WeaponAttackAction w, IGame g,
            Server s) {
        super(t, w, g, s);
        generalDamageType = HitData.DAMAGE_NONE;
    }
    
    /**
     * handle this weapons firing
     *
     * @return a <code>boolean</code> value indicating whether this should be
     * kept or not
     */
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }
        // VGLs automatically hit the three adjacent hex in their side
        
        
        // Determine what coords get hit
        AmmoType atype = (AmmoType) ammo.getType();
        int facing = weapon.getFacing();
        ArrayList<Coords> affectedCoords = new ArrayList<Coords>(3);
        affectedCoords.add(ae.getPosition().translated(ae.getFacing() + facing));
        affectedCoords.add(ae.getPosition().translated((ae.getFacing() + facing + 1) % 6));
        affectedCoords.add(ae.getPosition().translated((ae.getFacing() + facing + 5) % 6));
        
        Report r = new Report(3117);
        r.indent();
        r.subject = subjectId;
        r.add(wtype.getName());
        r.add(atype.getSubMunitionName());
        r.add(affectedCoords.get(0).getBoardNum());
        r.add(affectedCoords.get(1).getBoardNum());
        r.add(affectedCoords.get(2).getBoardNum());
        vPhaseReport.add(r);
        
        
        for (Coords c : affectedCoords) {
            Building bldg = game.getBoard().getBuildingAt(c);
            if (atype.getMunitionType() == AmmoType.M_SMOKEGRENADE) {
                server.deliverSmokeGrenade(c, vPhaseReport);
            } else if (atype.getMunitionType() == AmmoType.M_CHAFF) {
                server.deliverChaffGrenade(c, vPhaseReport);
            } else if (atype.getMunitionType() == AmmoType.M_INCENDIARY) {
                Vector<Report> dmgReports;
                // Delivery an inferno to the hex
                Targetable grenadeTarget = new HexTarget(c, game.getBoard(),
                        Targetable.TYPE_HEX_IGNITE);
                dmgReports = server
                        .deliverInfernoMissiles(ae, grenadeTarget, 1);
                r = new Report(3372);
                r.add("Hex " + c.getBoardNum());
                r.indent();
                dmgReports.insertElementAt(r, 0);
                dmgReports.get(dmgReports.size() - 1).newlines++;
                for (Report dr : dmgReports) {
                    dr.indent();
                }
                vPhaseReport.addAll(dmgReports);
                // If there's a building, delivery an inferno to it
                if (bldg != null) {
                    grenadeTarget = new BuildingTarget(c, game.getBoard(),
                            Targetable.TYPE_BLDG_IGNITE);
                    dmgReports = server.deliverInfernoMissiles(ae,
                            grenadeTarget, 1);
                    r = new  Report(3372);
                    r.add(bldg.getName());
                    r.indent();
                    dmgReports.insertElementAt(r, 0);
                    dmgReports.get(dmgReports.size() - 1).newlines++;
                    for (Report dr : dmgReports) {
                        dr.indent();
                    }
                    vPhaseReport.addAll(dmgReports);
                }
                // Delivery an inferno to each entity in the affected hex
                for (Entity entTarget : game.getEntitiesVector(c)) {
                    // Infantry in a building take damage when the building is
                    //  targeted, so should be ignored here
                    if (bldg != null && (entTarget instanceof Infantry) 
                            && Compute.isInBuilding(game, entTarget)){
                        continue;
                    }
                    dmgReports = server
                            .deliverInfernoMissiles(ae, entTarget, 1);
                    r = new  Report(3371);
                    r.addDesc(entTarget);
                    r.indent();
                    dmgReports.insertElementAt(r, 0);
                    dmgReports.get(dmgReports.size() - 1).newlines++;
                    for (Report dr : dmgReports) {
                        dr.indent();
                    }
                    vPhaseReport.addAll(dmgReports);
                }
            } else { // Assume fragmentation grenade
                // Damage each Entity in the target coord
                for (Entity entTarget : game.getEntitiesVector(c)) {
                    boolean inBuilding = (bldg != null) 
                            && Compute.isInBuilding(game, entTarget, c);
                
                    hit = entTarget.rollHitLocation(toHit.getHitTable(),
                            toHit.getSideTable(), waa.getAimedLocation(),
                            waa.getAimingMode(), toHit.getCover());
                    hit.setAttackerId(getAttackerId());
                    
                    Vector<Report> dmgReports = new Vector<Report>();
                    // Infantry take 2D6 burst damage
                    if (!inBuilding && (entTarget instanceof Infantry) 
                            && !(entTarget instanceof BattleArmor)) {
                        int infDmg = Compute.directBlowInfantryDamage(0, 0,
                                WeaponType.WEAPON_BURST_2D6,
                                ((Infantry) entTarget).isMechanized(),
                                toHit.getThruBldg() != null);
                        dmgReports = 
                                server.damageEntity(entTarget, hit, infDmg);
                    } else if (inBuilding && (entTarget instanceof Infantry) 
                            && !(entTarget instanceof BattleArmor)) {
                        r = new Report(3417);
                        r.addDesc(entTarget);
                        r.indent(2);
                        dmgReports.add(r);
                    } else if (entTarget.getBARRating(hit.getLocation()) < 5) {
                        int dmg = 5 - entTarget.getBARRating(hit.getLocation());
                        dmgReports = server.damageEntity(entTarget, hit, dmg);
                    } else {
                        r = new Report(3416);
                        r.addDesc(entTarget);
                        r.indent(2);
                        dmgReports.add(r);
                    }
                    dmgReports.get(dmgReports.size() - 1).newlines++;
                    vPhaseReport.addAll(dmgReports);
                }
            }
        }        
        
        return false;
    }

}

