/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.battleArmor;

import java.io.Serial;
import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.rolls.Roll;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.common.weapons.handlers.AmmoWeaponHandler;
import megamek.server.totalwarfare.TWGameManager;

public class BATaserHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = 1308895663099714573L;

    public BATaserHandler(ToHitData toHitData, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager twGameManager) {
        super(toHitData, weaponAttackAction, game, twGameManager);
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
        if (entityTarget instanceof BattleArmor) {
            if (diceRoll.getIntValue() >= 9) {
                initHit(entityTarget);

                report = new Report(3706);
                report.addDesc(entityTarget);
                // shut down for rest of scenario, so we actually kill it
                // TODO: fix for salvage purposes
                report.add(entityTarget.getLocationAbbr(hit));
                vPhaseReport.add(report);
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
                    report = new Report(3705);
                    report.addDesc(entityTarget);
                    report.add(3);
                    vPhaseReport.add(report);
                    entityTarget.taserShutdown(3, true);
                } else {
                    report = new Report(3710);
                    report.addDesc(entityTarget);
                    report.add(1);
                    report.add(3);
                    vPhaseReport.add(report);
                    entityTarget.setTaserInterference(1, 3, true);
                }
            }
        } else if ((entityTarget instanceof ProtoMek)
              || (entityTarget instanceof Tank)
              || (entityTarget instanceof Aero)) {
            if (diceRoll.getIntValue() >= 11) {
                report = new Report(3705);
                report.addDesc(entityTarget);
                report.add(3);
                vPhaseReport.add(report);
                entityTarget.taserShutdown(3, true);
            } else {
                report = new Report(3710);
                report.addDesc(entityTarget);
                report.add(1);
                report.add(3);
                vPhaseReport.add(report);
                entityTarget.setTaserInterference(1, 3, true);
            }
        }


        Roll diceRoll2 = Compute.rollD6(2);
        report = new Report(3715);
        report.addDesc(attackingEntity);
        report.add(diceRoll2);
        report.newlines = 0;
        report.indent(2);
        vPhaseReport.add(report);
        if (diceRoll2.getIntValue() >= 7) {
            report = new Report(3720);
            vPhaseReport.add(report);
            // +1 to-hit for 3 turns
            attackingEntity.setTaserFeedback(3);
        } else {
            report = new Report(3725);
            vPhaseReport.add(report);
            // kill the firing trooper
            // TODO: should just be shut down for remainder of scenario
            vPhaseReport.addAll(gameManager.criticalEntity(attackingEntity, weapon.getLocation(),
                  false, 0, false, false, 0));
        }
        return done;
    }
}
