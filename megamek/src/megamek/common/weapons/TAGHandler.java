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

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.server.gameManager.GameManager;

public class TAGHandler extends WeaponHandler {
    private static final long serialVersionUID = -967656770476044773L;

    public TAGHandler(ToHitData toHit, WeaponAttackAction waa, Game g, GameManager m) {
        super(toHit, waa, g, m);
    }

    @Override
    protected int calcDamagePerHit() {
        return 0;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport,
                                      Building bldg, int hits, int nCluster, int bldgAbsorbs) {
        if (entityTarget == null) {
            Report r = new Report(3187);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        } else {

            TagInfo info = new TagInfo(ae.getId(), Targetable.TYPE_ENTITY,
                    entityTarget, false);
            game.addTagInfo(info);
            entityTarget.setTaggedBy(ae.getId());

            if (weapon.isInternalBomb()) {
                // Firing an internally-mounted TAG pod counts for bomb bay explosion check
                ((IBomber) ae).increaseUsedInternalBombs(1);
            }

            // per errata, being painted by a TAG also spots the target for indirect fire
            ae.setSpotting(true);
            ae.setSpotTargetId(entityTarget.getId());

            Report r = new Report(3188);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
    }

    @Override
    protected boolean handleSpecialMiss(Entity entityTarget, boolean bldgDamagedOnMiss,
                                        Building bldg, Vector<Report> vPhaseReport) {
        // add even misses, as they waste homing missiles.
        TagInfo info = new TagInfo(ae.getId(), target.getTargetType(), target, true);
        game.addTagInfo(info);
        return false;
    }

    @Override
    public boolean cares(GamePhase phase) {
        return phase.isOffboard();
    }
}
