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

package megamek.common.strategicBattleSystems;

import java.io.Serializable;

import megamek.common.BoardLocation;
import megamek.common.InGameObject;

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
