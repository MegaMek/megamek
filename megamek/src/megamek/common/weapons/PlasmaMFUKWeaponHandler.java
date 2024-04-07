/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.ArmorType;
import megamek.server.gameManager.GameManager;
import megamek.server.Server;

import java.util.Vector;

public class PlasmaMFUKWeaponHandler extends EnergyWeaponHandler {
    private static final long serialVersionUID = -6816799343788643259L;

    public PlasmaMFUKWeaponHandler(ToHitData toHit, WeaponAttackAction waa, Game g, GameManager m) {
        super(toHit, waa, g, m);
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport,
                                      Building bldg, int hits, int nCluster, int bldgAbsorbs) {
        if (entityTarget instanceof Mech) {
            if (!bSalvo) {
                // hits
                Report r = new Report(3390);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
            }
            super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);

            if (missed) {
                return;
            }

            Report r = new Report(3400);
            r.subject = subjectId;
            r.indent(2);
            if ((entityTarget.getArmor(hit) > 0)
                    && ((entityTarget.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HEAT_DISSIPATING)
                    || (entityTarget.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_REFLECTIVE))) {
                entityTarget.heatFromExternal += 2;
                r.add(2);
                r.choose(true);
                r.messageId=3406;
                r.add(5);
                r.add(ArmorType.forEntity(entityTarget, hit.getLocation()).getName());
            } else {
                entityTarget.heatFromExternal += 5;
                r.add(5);
                r.choose(true);
            }
            vPhaseReport.addElement(r);
        } else {
            super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
        }
    }

    @Override
    protected void handleIgnitionDamage(Vector<Report> vPhaseReport, Building bldg, int hits) {
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
    protected void handleClearDamage(Vector<Report> vPhaseReport, Building bldg, int nDamage) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        // report that damage was "applied" to terrain
        Report r = new Report(3385);
        r.indent(2);
        r.subject = subjectId;
        r.add(nDamage);
        vPhaseReport.addElement(r);

        // Any clear attempt can result in accidental ignition, even
        // weapons that can't normally start fires. that's weird.
        // Buildings can't be accidentally ignited.
        // TODO : change this for TacOps - now you roll another 2d6 first and on a 5 or less
        // TODO : you do a normal ignition as though for intentional fires
        if ((bldg != null)
                && gameManager.tryIgniteHex(target.getPosition(), subjectId, true, false,
                        new TargetRoll(wtype.getFireTN(), wtype.getName()), 5, vPhaseReport)) {
            return;
        }
        Vector<Report> clearReports = gameManager.tryClearHex(target.getPosition(), nDamage, subjectId);
        if (!clearReports.isEmpty()) {
            vPhaseReport.lastElement().newlines = 0;
        }
        vPhaseReport.addAll(clearReports);
    }
}
