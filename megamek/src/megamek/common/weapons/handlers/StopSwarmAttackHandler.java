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
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Andrew Hunter
 * @since Sep 23, 2004
 */
public class StopSwarmAttackHandler extends WeaponHandler {
    @Serial
    private static final long serialVersionUID = 7078803294398264979L;

    /**
     *
     */
    public StopSwarmAttackHandler(ToHitData toHit, WeaponAttackAction waa, Game g, TWGameManager m) {
        super(toHit, waa, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.AttackHandler#handle(int, java.util.Vector)
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        Entity entityTarget = (Entity) target;
        // ... but only as their *only* attack action.
        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            Report r = new Report(3105);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        }
        // swarming ended successfully
        Report report = new Report(3110);
        report.subject = subjectId;
        vPhaseReport.addElement(report);
        // Only apply the "stop swarm 'attack'" to the swarmed Mek.
        if (attackingEntity.getSwarmTargetId() != target.getId()) {
            Entity other = game.getEntity(attackingEntity.getSwarmTargetId());

            if (other != null) {
                other.setSwarmAttackerId(Entity.NONE);
            }
        } else {
            entityTarget.setSwarmAttackerId(Entity.NONE);
        }
        attackingEntity.setSwarmTargetId(Entity.NONE);
        return false;
    }
}
