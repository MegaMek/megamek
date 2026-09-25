/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.moves;

import java.util.Set;

import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.pathfinder.CachedEntityState;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.WallTarget;

/** One level of movement along an explicitly selected wall face; the top remains a named hexside. */
class WallStep implements PhasePass {
    @Override
    public Set<MoveStepType> getTypesOfInterest() {
        return Set.of(MoveStepType.WALL_ASCEND, MoveStepType.WALL_DESCEND, MoveStepType.WALL_LAND);
    }

    @Override
    public PhasePassResult preCompilation(MoveStep step, Game game, Entity entity, MoveStep prev,
          CachedEntityState cachedEntityState) {
        if (step.getTarget(game) instanceof WallTarget wall) {
            step.setOccupiedWall(wall);
        }
        if (step.getType() == MoveStepType.WALL_LAND) {
            var segment = step.getOccupiedWall() == null ? null : step.getOccupiedWall().segment(game);
            if (segment != null) {
                step.setElevation(segment.topAltitude() - game.getBoard(step.getBoardId())
                      .getHex(step.getPosition()).getLevel());
            }
            step.setMp(0);
            return PhasePassResult.BREAK;
        }
        step.setElevation(prev.getElevation() + (step.getType() == MoveStepType.WALL_ASCEND ? 1 : -1));
        var segment = step.getOccupiedWall() == null ? null : step.getOccupiedWall().segment(game);
        step.setMp(entity instanceof Infantry ? segment != null && segment.fence()
              ? Math.min(2, Math.max(1, cachedEntityState.getWalkMP())) : 1 : entity instanceof Mek mek
              ? ClimbingHelper.getClimbingMPCostPerLevel(mek) : 2);
        if (step.getType() == MoveStepType.WALL_DESCEND && step.getElevation()
              == -game.getBoard(step.getBoardId()).getHex(step.getPosition()).depth()) {
            step.setOccupiedWall(null);
        }
        return PhasePassResult.BREAK;
    }
}
