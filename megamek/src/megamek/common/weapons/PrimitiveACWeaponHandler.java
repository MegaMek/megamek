/*
 * MegaMek - Copyright (C) 2000-2007 Ben Mazur (bmazur@sev.org)
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

import megamek.common.Game;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.totalwarfare.TWGameManager;

/**
 * Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public class PrimitiveACWeaponHandler extends ACWeaponHandler {
    @Serial
    private static final long serialVersionUID = -3686194077871525280L;

    public PrimitiveACWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) {
        super(t, w, g, m);
    }

    @Override
    protected boolean doChecks(Vector<Report> vPhaseReport) {
        if (super.doChecks(vPhaseReport)) {
            return true;
        }

        if ((roll.getIntValue() == 2) && !ae.isConventionalInfantry()) {
            Report r = new Report(3161);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
            weapon.setJammed(true);
            weapon.setHit(true);
        }
        return false;
    }
}
