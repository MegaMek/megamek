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
package megamek.client.ui.clientGUI.boardview.toolTip;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.tooltip.SBFInGameObjectTooltip;
import megamek.common.BoardLocation;
import megamek.common.Coords;
import megamek.common.InGameObject;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.strategicBattleSystems.SBFUnitPlaceHolder;

public record SBFBoardViewTooltip(SBFGame game, BoardView bv) implements BoardViewTooltipProvider {

    @Override
    public String getTooltip(Point point, Coords movementTarget) {
        final Coords coords = bv.getCoordsAt(point);
        if (!game.getBoard().contains(coords)) {
            return null;
        }
        var location = BoardLocation.of(coords, 0); //TODO should not be fixed to board 0

        StringBuilder tooltip = new StringBuilder("<HTML>");
        // HEAD - styles for all content must go here
        tooltip.append("<HEAD><STYLE>");
        tooltip.append(SBFInGameObjectTooltip.styles());
        tooltip.append("</STYLE></HEAD>");

        // BODY
        tooltip.append("<BODY>");

        List<InGameObject> units = new ArrayList<>(game.getActiveFormationsAt(location));
        game.getInGameObjects().stream()
              .filter(u -> u instanceof SBFUnitPlaceHolder)
              .map(u -> (SBFUnitPlaceHolder) u)
              .filter(u -> location.equals(u.getPosition()))
              .forEach(units::add);

        //TODO: when showing hex info, this must be replaced
        if (units.isEmpty()) {
            return null;
        }

        for (InGameObject unit : units) {
            tooltip.append(SBFInGameObjectTooltip.getTooltip(unit, game));
        }

        tooltip.append("</BODY></HTML>");
        return tooltip.toString();
    }
}
