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

import static megamek.common.weapons.handlers.AreaEffectHelper.calculateDamageFallOff;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 23, 2004
 */
public class MicroBombHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = -2995118961278208244L;

    /**
     *
     */
    public MicroBombHandler(ToHitData toHit, WeaponAttackAction waa, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(toHit, waa, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.handlers.WeaponHandler#specialResolution(java.util.Vector,
     * megamek.common.units.Entity, boolean)
     */
    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget) {
        Coords coords = target.getPosition();
        if (!bMissed) {
            Report r = new Report(3190);
            r.subject = subjectId;
            r.add(coords.getBoardNum());
            vPhaseReport.add(r);
        } else {
            // magic number - BA-launched micro bombs only scatter 1 hex per TW-2018 p 228
            coords = Compute.scatter(coords, 1);
            if (game.getBoard().contains(coords)) {
                Report report = new Report(3195);
                report.subject = subjectId;
                report.add(coords.getBoardNum());
                vPhaseReport.add(report);
            } else {
                Report report = new Report(3200);
                report.subject = subjectId;
                vPhaseReport.add(report);
                return !bMissed;
            }
        }
        // Not mine clearing if we are shooting an entity
        Infantry ba = (Infantry) attackingEntity;
        DamageFalloff falloff = calculateDamageFallOff(ammo.getType(), ba.getShootingStrength(), false);
        gameManager.artilleryDamageArea(coords, ammo.getType(), subjectId,
              attackingEntity, falloff, false, 0, vPhaseReport, false);
        return true;
    }
}
