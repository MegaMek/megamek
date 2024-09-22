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
package megamek.common.weapons.battlearmor;

import java.io.Serial;
import java.util.Vector;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AmmoWeaponHandler;
import megamek.server.totalwarfare.TWGameManager;

public class BATaserHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = 1308895663099714573L;

    public BATaserHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) {
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
        if (entityTarget instanceof BattleArmor) {
            if (diceRoll.getIntValue() >= 9) {
                initHit(entityTarget);

                r = new Report(3706);
                r.addDesc(entityTarget);
                // shut down for rest of scenario, so we actually kill it
                // TODO: fix for salvage purposes
                r.add(entityTarget.getLocationAbbr(hit));
                vPhaseReport.add(r);
                entityTarget.destroyLocation(hit.getLocation());
                // Check to see if the squad has been eliminated
                if (entityTarget.getTransferLocation(hit).getLocation() ==
                        Entity.LOC_DESTROYED) {
                    vPhaseReport.addAll(gameManager.destroyEntity(entityTarget,
                            "all troopers eliminated", false));
                }
                done = true;
            }
        } else if (entityTarget instanceof Mek) {
            if (((Mek) entityTarget).isIndustrial()) {
                if (diceRoll.getIntValue() >= 11) {
                    entityTarget.taserShutdown(3, true);
                } else {
                    // suffer +1 to piloting and gunnery for 3 rounds
                    entityTarget.setTaserInterference(1, 3, true);
                }
            } else {
                if (diceRoll.getIntValue() >= 12) {
                    r = new Report(3705);
                    r.addDesc(entityTarget);
                    r.add(3);
                    vPhaseReport.add(r);
                    entityTarget.taserShutdown(3, true);
                } else {
                    r = new Report(3710);
                    r.addDesc(entityTarget);
                    r.add(1);
                    r.add(3);
                    vPhaseReport.add(r);
                    entityTarget.setTaserInterference(1, 3, true);
                }
            }
        } else if ((entityTarget instanceof ProtoMek)
                || (entityTarget instanceof Tank)
                || (entityTarget instanceof Aero)) {
            if (diceRoll.getIntValue() >= 11) {
                r = new Report(3705);
                r.addDesc(entityTarget);
                r.add(3);
                vPhaseReport.add(r);
                entityTarget.taserShutdown(3, true);
            } else {
                r = new Report(3710);
                r.addDesc(entityTarget);
                r.add(1);
                r.add(3);
                vPhaseReport.add(r);
                entityTarget.setTaserInterference(1, 3, true);
            }
        }


        Roll diceRoll2 = Compute.rollD6(2);
        r = new Report(3715);
        r.addDesc(ae);
        r.add(diceRoll2);
        r.newlines = 0;
        r.indent(2);
        vPhaseReport.add(r);
        if (diceRoll2.getIntValue() >= 7) {
            r = new Report(3720);
            vPhaseReport.add(r);
            // +1 to-hit for 3 turns
            ae.setTaserFeedback(3);
        } else {
            r = new Report(3725);
            vPhaseReport.add(r);
            // kill the firing trooper
            // TODO: should just be shut down for remainder of scenario
            vPhaseReport.addAll(gameManager.criticalEntity(ae, weapon.getLocation(),
                    false, 0, false, false, 0));
        }
        return done;
    }
}
