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

import megamek.common.Aero;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.HitData;
import megamek.common.Game;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.ArmorType;
import megamek.common.weapons.ppc.CLPlasmaCannon;
import megamek.common.weapons.ppc.ISPlasmaRifle;
import megamek.server.GameManager;

public class PlasmaBayWeaponHandler extends AmmoBayWeaponHandler {
    private static final long serialVersionUID = -4718048077136686433L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public PlasmaBayWeaponHandler(ToHitData toHit, WeaponAttackAction waa, Game g, GameManager m) {
        super(toHit, waa, g, m);
        generalDamageType = HitData.DAMAGE_ENERGY;
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
        super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                nCluster, bldgAbsorbs);
        if (!missed
                && ((entityTarget instanceof Mech) || (entityTarget instanceof Aero))) {
            int extraHeat = 0;
            for (int wId : weapon.getBayWeapons()) {
                Mounted m = ae.getEquipment(wId);
                if (!m.isBreached() && !m.isDestroyed() && !m.isJammed()) {
                    WeaponType bayWType = ((WeaponType) m.getType());
                    if (bayWType instanceof ISPlasmaRifle) {
                        extraHeat += Compute.d6();
                    } else if (bayWType instanceof CLPlasmaCannon) {
                        extraHeat += Compute.d6(2);
                    }
                }
            }

            if (extraHeat > 0) {
                Report r = new Report(3400);
                r.subject = subjectId;
                r.indent(2);
                if (entityTarget.getArmor(hit) > 0 &&                        
                        (entityTarget.getArmorType(hit.getLocation()) == 
                           EquipmentType.T_ARMOR_REFLECTIVE)) {
                   entityTarget.heatFromExternal += Math.max(1, extraHeat / 2);
                   r.messageId = 3406;
                   r.add(Math.max(1, extraHeat / 2));
                   r.choose(true);
                   r.add(extraHeat);
                   r.add(ArmorType.forEntity(entityTarget, hit.getLocation()).getName());
                } else if (entityTarget.getArmor(hit) > 0 &&
                       (entityTarget.getArmorType(hit.getLocation()) == 
                           EquipmentType.T_ARMOR_HEAT_DISSIPATING)) {
                    entityTarget.heatFromExternal += extraHeat / 2;
                    r.messageId = 3406;
                    r.add(extraHeat / 2);
                    r.choose(true);
                    r.add(extraHeat);
                    r.add(ArmorType.forEntity(entityTarget, hit.getLocation()).getName());
                } else {
                    entityTarget.heatFromExternal += extraHeat;
                    r.add(extraHeat);
                    r.choose(true);
                }                
                vPhaseReport.addElement(r);                
            }
        }
    }

    @Override
    protected void handleIgnitionDamage(Vector<Report> vPhaseReport,
            Building bldg, int hits) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        TargetRoll tn = new TargetRoll(wtype.getFireTN(), wtype.getName());
        if (tn.getValue() != TargetRoll.IMPOSSIBLE) {
            Report.addNewline(vPhaseReport);
            gameManager.tryIgniteHex(target.getPosition(), subjectId, true, false,
                    tn, true, -1, vPhaseReport);
        }
    }

    @Override
    protected void handleClearDamage(Vector<Report> vPhaseReport,
            Building bldg, int nDamage) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }

        nDamage *= 2; // Plasma weapons deal double damage to woods.

        // report that damage was "applied" to terrain
        Report r = new Report(3385);
        r.indent(2);
        r.subject = subjectId;
        r.add(nDamage);
        vPhaseReport.addElement(r);

        // Any clear attempt can result in accidental ignition, even
        // weapons that can't normally start fires. that's weird.
        // Buildings can't be accidentally ignited.
        // TODO: change this for TacOps - now you roll another 2d6 first and on
        // a 5 or less
        // you do a normal ignition as though for intentional fires
        if ((bldg != null)
                && gameManager.tryIgniteHex(target.getPosition(), subjectId, true,
                        false,
                        new TargetRoll(wtype.getFireTN(), wtype.getName()), 5,
                        vPhaseReport)) {
            return;
        }
        Vector<Report> clearReports = gameManager.tryClearHex(target.getPosition(),
                nDamage, subjectId);
        if (!clearReports.isEmpty()) {
            vPhaseReport.lastElement().newlines = 0;
        }
        vPhaseReport.addAll(clearReports);
    }

    @Override
    protected void handleBuildingDamage(Vector<Report> vPhaseReport,
            Building bldg, int nDamage, Coords coords) {
        // Plasma weapons deal double damage to buildings.
        super.handleBuildingDamage(vPhaseReport, bldg, nDamage * 2, coords);
    }
}
