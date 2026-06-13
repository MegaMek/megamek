/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.weapons.handlers;

import java.io.Serial;
import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.rolls.Roll;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.server.totalWarfare.TWGameManager;

public class MekTaserHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = 1308895663099714573L;

    public MekTaserHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
        generalDamageType = HitData.DAMAGE_ENERGY;
    }

    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget) {
        boolean done = false;

        if (bMissed) {
            return false;
        }
        Report report = new Report(3700);
        Roll diceRoll = Compute.rollD6(2);
        report.add(diceRoll);
        report.newlines = 0;
        vPhaseReport.add(report);

        if (entityTarget.getWeight() > 100) {
            return false;
        }

        if (entityTarget instanceof BattleArmor) {
            report = new Report(3706);
            report.addDesc(entityTarget);
            // shut down for rest of scenario, so we actually kill it
            // TODO: fix for salvage purposes
            report.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.add(report);
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
                    report = new Report(3705);
                    report.addDesc(entityTarget);
                    report.add(4);
                    entityTarget.taserShutdown(4, false);
                } else {
                    // suffer +2 to piloting and gunnery for 4 rounds
                    report = new Report(3710);
                    report.addDesc(entityTarget);
                    report.add(2);
                    report.add(4);
                    entityTarget.setTaserInterference(2, 4, true);
                }
            } else {
                if (diceRoll.getIntValue() >= 11) {
                    report = new Report(3705);
                    report.addDesc(entityTarget);
                    report.add(3);
                    vPhaseReport.add(report);
                    entityTarget.taserShutdown(3, false);
                } else {
                    report = new Report(3710);
                    report.addDesc(entityTarget);
                    report.add(2);
                    report.add(3);
                    vPhaseReport.add(report);
                    entityTarget.setTaserInterference(2, 3, true);
                }
            }
        } else if ((entityTarget instanceof ProtoMek)
              || (entityTarget instanceof Tank)
              || (entityTarget instanceof Aero)) {
            if (diceRoll.getIntValue() >= 8) {
                report = new Report(3705);
                report.addDesc(entityTarget);
                report.add(4);
                vPhaseReport.add(report);
                entityTarget.taserShutdown(4, false);
            } else {
                report = new Report(3710);
                report.addDesc(entityTarget);
                report.add(2);
                report.add(4);
                vPhaseReport.add(report);
                entityTarget.setTaserInterference(2, 4, false);
            }
        }
        return done;
    }
}
