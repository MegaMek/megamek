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
import java.awt.event.MouseEvent;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.common.board.Coords;
import megamek.common.annotations.Nullable;

/**
 * This interface is implemented by classes that can be added to a BoardView to provide the tooltip contents for the
 * BoardView.
 *
 * @see BoardView#setTooltipProvider(BoardViewTooltipProvider)
 */
public interface BoardViewTooltipProvider {

    /**
     * Returns a String that is displayed as the tooltip for a point according to the mouse event's point in the
     * BoardView's drawing area. The String may be an HTML string. It is passed to Swing's TooltipManager without
     * further modification, i.e. if it is an HTML string all tags must already be present. This method forwards to
     * {@link #getTooltip(Point, Coords)} and should not be overridden.
     *
     * @param event A mouse event associated with a position in the BoardView
     *
     * @return The tooltip to display
     */
    default String getTooltip(MouseEvent event) {
        return getTooltip(event, null);
    }

    /**
     * Returns a String that is displayed as the tooltip for a point according to the mouse event's point in the
     * BoardView's drawing area. The String may be an HTML string. It is passed to Swing's TooltipManager without
     * further modification, i.e. if it is an HTML string all tags must already be present. The String may also be null
     * to show no tooltip at all. The given movementTarget coordinates may be used to display information about another
     * hex or field in that BoardView. This method forwards to {@link #getTooltip(Point, Coords)} and should not be
     * overridden.
     *
     * @param event          A mouse event associated with a position in the BoardView
     * @param movementTarget Another coordinate to display additional information about
     *
     * @return The tooltip to display
     */
    default String getTooltip(MouseEvent event, @Nullable Coords movementTarget) {
        return getTooltip(event.getPoint(), movementTarget);
    }

    /**
     * Returns a String that is displayed as the tooltip for the given point in the BoardView's drawing area. The String
     * may be an HTML string. It is passed to Swing's TooltipManager without further modification, i.e. if it is an HTML
     * string all tags must already be present. The given movementTarget coordinates may be used to display information
     * about another hex or field in that BoardView.
     *
     * @param point          The point in the BoardView's drawing area as returned e.g. with a MouseEvent
     * @param movementTarget Another coordinate to display additional information about
     *
     * @return The tooltip to display
     */
    String getTooltip(Point point, @Nullable Coords movementTarget);
}
