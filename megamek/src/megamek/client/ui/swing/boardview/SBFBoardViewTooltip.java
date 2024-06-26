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

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.tooltip.SBFFormationTooltip;
import megamek.common.Coords;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFGame;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;

public class SBFBoardViewTooltip implements BoardViewTooltipProvider {

    private final GUIPreferences GUIP = GUIPreferences.getInstance();

    private final SBFGame game;
    private final BoardView bv;

    public SBFBoardViewTooltip(SBFGame game, BoardView boardView) {
        this.game = game;
        this.bv = boardView;
    }

    @Override
    public String getTooltip(Point point, Coords movementTarget) {
        final Coords coords = bv.getCoordsAt(point);
        if (!game.getBoard().contains(coords)) {
            return null;
        }
        StringBuilder tooltip = new StringBuilder();

        List<SBFFormation> formations = game.getInGameObjects().stream()
                .filter(SBFFormation.class::isInstance)
                .map(f -> (SBFFormation) f)
                .filter(f -> f.getPosition().isAtCoords(coords))
                .collect(Collectors.toList());

        for (SBFFormation formation : formations) {
            tooltip.append(SBFFormationTooltip.getTooltip(formation, game));
            tooltip.append("\n");
        }
        return tooltip.toString();
    }
}
