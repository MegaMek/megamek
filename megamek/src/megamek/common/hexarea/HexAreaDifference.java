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

package megamek.common.hexarea;

import java.util.Set;

import megamek.common.Board;
import megamek.common.Coords;

/**
 * This class represents the subtraction (difference) of two HexAreaShapes. The order of the two given shapes is
 * relevant. A coord is contained in the resulting area when it is present in the first but not present in the second.
 * This means that the second area only has an effect on the first area where it is present on the same boards.
 * <p>
 * Note that this HexArea itself has no board IDs. The result of this hex area is defined only by the two subtracted
 * areas.
 */
public class HexAreaDifference extends AbstractHexArea {

    private final HexArea firstShape;
    private final HexArea secondShape;

    /**
     * Creates an area that is the difference between the two given hex areas, firstShape - secondShape.
     *
     * @param firstShape  The first of the two shapes; the ordering of the shapes is relevant
     * @param secondShape The second of the two shapes
     */
    public HexAreaDifference(HexArea firstShape, HexArea secondShape) {
        this.firstShape = firstShape;
        this.secondShape = secondShape;
    }

    @Override
    public boolean containsCoords(Coords coords, Board board) {
        return firstShape.containsCoords(coords, board) && !secondShape.containsCoords(coords, board);
    }

    @Override
    public boolean isSmall() {
        return (firstShape instanceof AbstractHexArea firstAbstractHexArea) && firstAbstractHexArea.isSmall()
              && (secondShape instanceof AbstractHexArea secondAbstractHexArea) && secondAbstractHexArea.isSmall();
    }

    @Override
    public Set<Coords> getCoords() {
        if (isSmall()) {
            Set<Coords> result = ((AbstractHexArea) firstShape).getCoords();
            result.removeAll(((AbstractHexArea) secondShape).getCoords());
            return result;
        } else {
            return super.getCoords();
        }
    }
}
