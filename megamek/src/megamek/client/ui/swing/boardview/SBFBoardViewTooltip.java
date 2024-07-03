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

import megamek.client.ui.swing.tooltip.SBFFormationTooltip;
import megamek.common.BoardLocation;
import megamek.common.Coords;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFGame;

import java.awt.*;
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
        StringBuilder tooltip = new StringBuilder();

        List<SBFFormation> formations = game.getActiveFormationsAt(new BoardLocation(coords, 0));
        if (formations.isEmpty()) {
            return null;
        }
        tooltip.append(SBFFormationTooltip.getTooltip(formations, game));

        return tooltip.toString();
    }
}
