/*
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
package megamek.common.event;

import megamek.common.InGameObject;
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
