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

package megamek.common.units;

import megamek.common.board.Coords;
import megamek.common.game.Game;

/** Connected surface/subsurface parts retain separate designs and share a physical foundation (TO:AR p.139). */
public final class BuildingFoundationRules {
    private BuildingFoundationRules() { }

    public static AbstractBuildingEntity below(Game game, IBuilding surface, Coords coords) {
        if (!(surface instanceof AbstractBuildingEntity upper) || upper instanceof MobileStructure
              || upper.getDesign().getSite() != BuildingDesign.Site.SURFACE || BuildingElevation.base(upper, coords) != 0
              || game == null || !game.hasBoardLocation(coords, upper.getBoardId())) {
            return null;
        }
        return game.getBoard(upper).getBuildingsAt(coords).stream()
              .filter(b -> b instanceof AbstractBuildingEntity && b != upper && !(b instanceof MobileStructure))
              .map(AbstractBuildingEntity.class::cast)
              .filter(b -> b.getDesign().getSite() != BuildingDesign.Site.SURFACE
                    && BuildingElevation.roof(b, coords) == 0 && b.getCurrentCF(coords) > 0)
              .findFirst().orElse(null);
    }

    public static boolean stronger(IBuilding lower, IBuilding upper, Coords coords) {
        return lower != null && lower.getBuildingType().getTypeValue() > upper.getBuildingType().getTypeValue()
              && lower.getLoadCapacity(coords) > upper.getLoadCapacity(coords);
    }
}