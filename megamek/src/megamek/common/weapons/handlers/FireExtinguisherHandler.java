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
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 23, 2004
 */
public class FireExtinguisherHandler extends WeaponHandler {
    @Serial
    private static final long serialVersionUID = -7047033962986081773L;

    public FireExtinguisherHandler(ToHitData toHit, WeaponAttackAction waa, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(toHit, waa, g, m);
    }

    @Override
    protected boolean specialResolution(Vector<Report> vPhaseReport, Entity entityTarget) {
        if (!bMissed) {
            Report r = new Report(2270);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.add(r);
            if (Targetable.TYPE_HEX_EXTINGUISH == target.getTargetType()) {
                r = new Report(3540);
                r.subject = subjectId;
                r.add(target.getPosition().getBoardNum());
                r.indent(3);
                vPhaseReport.add(r);
                game.getBoard().getHex(target.getPosition()).removeTerrain(Terrains.FIRE);
                gameManager.sendChangedHex(target.getPosition());
                game.getBoard().removeInfernoFrom(target.getPosition());
            } else if (target instanceof Entity) {
                if (entityTarget.infernos.isStillBurning()
                      || (target instanceof Tank && ((Tank) target).isOnFire())) {
                    r = new Report(3550);
                    r.subject = subjectId;
                    r.addDesc(entityTarget);
                    r.indent(3);
                    vPhaseReport.add(r);
                }
                entityTarget.infernos.clear();
                if (target instanceof Tank) {
                    for (int i = 0; i < entityTarget.locations(); i++) {
                        ((Tank) target).extinguishAll();
                    }
                }
            }
        }
        return true;
    }
}
