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

import java.util.Vector;

import megamek.common.Compute;
import megamek.common.Game;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.gameManager.GameManager;
import megamek.server.Server;

public class InsulatedLaserWeaponHandler extends EnergyWeaponHandler {
    private static final long serialVersionUID = -7951442134048385366L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public InsulatedLaserWeaponHandler(ToHitData toHit, WeaponAttackAction waa,
            Game g, GameManager m) {
        super(toHit, waa, g, m);
    }

    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (roll.getIntValue() == 2) {
            int damageRoll = Compute.d6(2);
            if (damageRoll >= 8) {
                Report r = new Report();
                r.subject = subjectId;
                r.messageId = 3172;
                vPhaseReport.addElement(r);
                weapon.setHit(true);
                return false;
            }
        }
        return false;
    }
}
