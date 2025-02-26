/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

import java.io.Serial;
import java.util.Vector;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.totalwarfare.TWGameManager;

public class MekTaserHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = 1308895663099714573L;

    public MekTaserHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) {
        super(t, w, g, m);
        generalDamageType = HitData.DAMAGE_ENERGY;
    }

    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget) {
        boolean done = false;
        if (bMissed) {
            return done;
        }
        Report r = new Report(3700);
        Roll diceRoll = Compute.rollD6(2);
        r.add(diceRoll);
        r.newlines = 0;
        vPhaseReport.add(r);
        if (entityTarget.getWeight() > 100) {
            return done;
        }
        if (entityTarget instanceof BattleArmor) {
            r = new Report(3706);
            r.addDesc(entityTarget);
            // shut down for rest of scenario, so we actually kill it
            // TODO: fix for salvage purposes
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.add(r);
            entityTarget.destroyLocation(hit.getLocation());
            // Check to see if the squad has been eliminated
            if (entityTarget.getTransferLocation(hit).getLocation() == Entity.LOC_DESTROYED) {
                vPhaseReport.addAll(gameManager.destroyEntity(entityTarget,
                        "all troopers eliminated", false));
            }
            done = true;
        } else if (entityTarget instanceof Mek) {
            if (((Mek) entityTarget).isIndustrial()) {
                if (diceRoll.getIntValue() >= 8) {
                    r = new Report(3705);
                    r.addDesc(entityTarget);
                    r.add(4);
                    entityTarget.taserShutdown(4, false);
                } else {
                    // suffer +2 to piloting and gunnery for 4 rounds
                    r = new Report(3710);
                    r.addDesc(entityTarget);
                    r.add(2);
                    r.add(4);
                    entityTarget.setTaserInterference(2, 4, true);
                }
            } else {
                if (diceRoll.getIntValue() >= 11) {
                    r = new Report(3705);
                    r.addDesc(entityTarget);
                    r.add(3);
                    vPhaseReport.add(r);
                    entityTarget.taserShutdown(3, false);
                } else {
                    r = new Report(3710);
                    r.addDesc(entityTarget);
                    r.add(2);
                    r.add(3);
                    vPhaseReport.add(r);
                    entityTarget.setTaserInterference(2, 3, true);
                }
            }
        } else if ((entityTarget instanceof ProtoMek)
                || (entityTarget instanceof Tank)
                || (entityTarget instanceof Aero)) {
            if (diceRoll.getIntValue() >= 8) {
                r = new Report(3705);
                r.addDesc(entityTarget);
                r.add(4);
                vPhaseReport.add(r);
                entityTarget.taserShutdown(4, false);
            } else {
                r = new Report(3710);
                r.addDesc(entityTarget);
                r.add(2);
                r.add(4);
                vPhaseReport.add(r);
                entityTarget.setTaserInterference(2, 4, false);
            }
        }
        return done;
    }
}
