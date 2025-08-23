/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brocks Created on Oct 15, 2004
 */
public class MPodHandler extends LBXHandler {
    @Serial
    private static final long serialVersionUID = -1591751929178217495L;

    /**
     *
     */
    public MPodHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) {
        super(t, w, g, m);
        sSalvoType = " pellet(s) ";
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcHits(Vector<Report>
     * vPhaseReport)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs do one lump of damage per BA suit
        if (target.isConventionalInfantry()) {
            return 1;
        }
        int shots = 15;
        if (nRange == 2) {
            shots = 10;
        } else if (nRange == 3) {
            shots = 5;
        } else if (nRange == 4) {
            shots = 2;
        }

        int hitMod = 0;
        if (bGlancing) {
            hitMod -= 4;
        }

        if (bLowProfileGlancing) {
            hitMod -= 4;
        }

        PlanetaryConditions conditions = game.getPlanetaryConditions();
        if (conditions.getEMI().isEMI()) {
            hitMod -= 2;
        }

        int shotsHit = allShotsHit() ? shots : Compute.missilesHit(shots,
              hitMod);

        Report r = new Report(3325);
        r.subject = subjectId;
        r.add(shotsHit);
        r.add(sSalvoType);
        r.add(toHit.getTableDesc());
        r.newlines = 0;
        vPhaseReport.addElement(r);
        r = new Report(3345);
        r.subject = subjectId;
        vPhaseReport.addElement(r);
        bSalvo = true;
        return shotsHit;
    }
}
