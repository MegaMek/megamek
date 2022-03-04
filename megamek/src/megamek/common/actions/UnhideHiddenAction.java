/*
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

package megamek.common.actions;

/**
 * Concrete implementation of and entity action for unhiding hidden entities
 * <code>Enity.NONE</code> <em>is</em> a valid value for entityId.
 */
public class UnhideHiddenAction extends AbstractEntityAction {
    /**
     *
     */
    private static final long serialVersionUID = -8319076127334875298L;
    private int playerId;

    public UnhideHiddenAction(int playerId, int entityId) {
        super(entityId);
        this.playerId = playerId;
    }

    public int getPlayerId() {
        return playerId;
    }
}
