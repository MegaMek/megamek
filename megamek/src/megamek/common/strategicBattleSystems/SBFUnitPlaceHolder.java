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

package megamek.common.strategicBattleSystems;

import megamek.common.BoardLocation;
import megamek.common.InGameObject;

import java.io.Serializable;

public class SBFUnitPlaceHolder implements InGameObject, Serializable {

    private int id;
    private int ownerId;
    private final BoardLocation position;

    public SBFUnitPlaceHolder(SBFFormation formation) {
        id = formation.getId();
        ownerId = formation.getOwnerId();
        position = formation.getPosition();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int newId) {
        id = newId;
    }

    @Override
    public int getOwnerId() {
        return ownerId;
    }

    @Override
    public void setOwnerId(int newOwnerId) {
        ownerId = newOwnerId;
    }

    @Override
    public int getStrength() {
        return 0;
    }

    @Override
    public String generalName() {
        return "Unknown Formation";
    }

    @Override
    public String specificName() {
        return "";
    }

    public BoardLocation getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "[" + getClass().getSimpleName() + "] id: " + id + ", ownerId: " + ownerId;
    }
}
