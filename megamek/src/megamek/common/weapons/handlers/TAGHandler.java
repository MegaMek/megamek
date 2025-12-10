/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
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
import megamek.common.TagInfo;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.units.IBomber;
import megamek.common.units.IBuilding;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;

public class TAGHandler extends WeaponHandler {
    @Serial
    private static final long serialVersionUID = -967656770476044773L;

    public TAGHandler(ToHitData toHit, WeaponAttackAction waa, Game g, TWGameManager m) throws EntityLoadingException {
        super(toHit, waa, g, m);
    }

    @Override
    protected int calcDamagePerHit() {
        return 0;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport,
          IBuilding bldg, int hits, int nCluster, int bldgAbsorbs) {
        if (entityTarget == null) {
            Report r = new Report(3187);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        } else {

            TagInfo info = new TagInfo(attackingEntity.getId(), Targetable.TYPE_ENTITY,
                  entityTarget, false);
            game.addTagInfo(info);
            entityTarget.setTaggedBy(attackingEntity.getId());

            if (weapon.isInternalBomb()) {
                // Firing an internally-mounted TAG pod counts for bomb bay explosion check
                ((IBomber) attackingEntity).increaseUsedInternalBombs(1);
            }

            // per errata, being painted by a TAG also spots the target for indirect fire
            attackingEntity.setSpotting(true);
            attackingEntity.setSpotTargetId(entityTarget.getId());

            Report r = new Report(3188);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
    }

    @Override
    protected boolean handleSpecialMiss(Entity entityTarget, boolean bldgDamagedOnMiss,
          IBuilding bldg, Vector<Report> vPhaseReport) {
        // add even misses, as they waste homing missiles.
        TagInfo info = new TagInfo(attackingEntity.getId(), target.getTargetType(), target, true);
        game.addTagInfo(info);
        return false;
    }

    @Override
    public boolean cares(GamePhase phase) {
        return phase.isOffboard();
    }
}
