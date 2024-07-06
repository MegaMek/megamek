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
package megamek.client.ui.swing.boardview;

import megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip;
import megamek.common.BoardLocation;
import megamek.common.Coords;
import megamek.common.InGameObject;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.strategicBattleSystems.SBFUnitPlaceHolder;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SBFBoardViewTooltip implements BoardViewTooltipProvider {

    private final SBFGame game;
    private final BoardView bv;

    public SBFBoardViewTooltip(SBFGame game, BoardView bv) {
        this.game = game;
        this.bv = bv;
    }

    @Override
    public String getTooltip(Point point, Coords movementTarget) {
        final Coords coords = bv.getCoordsAt(point);
        if (!game.getBoard().contains(coords)) {
            return null;
        }
        var location = new BoardLocation(coords, 0); //TODO should not be fixed to board 0

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
