/*
  Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
