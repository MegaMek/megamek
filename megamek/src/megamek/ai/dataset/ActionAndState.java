/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.ai.dataset;


import megamek.common.Entity;

import java.util.List;

/**
 * Represents an action and the state of the board after the action is performed.
 * @param round game round
 * @param unitAction unit action performed
 * @param boardUnitState state of the board when the action is performed
 * @author Luana Coppio
 */
public record ActionAndState(int round, UnitAction unitAction, List<UnitState> boardUnitState) {
    public Entity getEntity() {
        var unitStateOpt = boardUnitState.stream().filter(unitState -> unitState.id() == unitAction.id()).findFirst();
        return unitStateOpt.map(UnitState::entity).orElse(null);
    }
}
