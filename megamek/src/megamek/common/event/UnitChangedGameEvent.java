/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.event;

import megamek.common.game.InGameObject;
import megamek.common.annotations.Nullable;

public class UnitChangedGameEvent extends GameEvent {

    protected InGameObject oldUnit;
    protected InGameObject newUnit;

    public UnitChangedGameEvent(Object source, @Nullable InGameObject oldUnit, InGameObject newUnit) {
        super(source);
        this.oldUnit = oldUnit;
        this.newUnit = newUnit;
    }

    public InGameObject getOldUnit() {
        return oldUnit;
    }

    @Override
    public String toString() {
        return "[" + getClass().getSimpleName() + "] oldUnit: " + oldUnit;
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gameUnitChange(this);
    }

    @Override
    public String getEventName() {
        return "Entity Change";
    }
}
