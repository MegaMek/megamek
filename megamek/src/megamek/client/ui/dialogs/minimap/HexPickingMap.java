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
package megamek.client.ui.dialogs.minimap;

import java.awt.Component;
import java.util.List;
import java.util.function.Consumer;

import megamek.common.board.Board;
import megamek.common.board.Coords;

/**
 * A map the player can pick hexes on by clicking, for a window that has no full board view. The Commander window's
 * minimap is one.
 */
public interface HexPickingMap {

    /**
     * Starts telling the listener which hex the player clicked. A drag to pan the map is not a click.
     *
     * @param listener called with the clicked hex
     */
    void addHexClickListener(Consumer<Coords> listener);

    /**
     * Stops telling the listener about clicks.
     *
     * @param listener a listener added with {@link #addHexClickListener(Consumer)}
     */
    void removeHexClickListener(Consumer<Coords> listener);

    /**
     * Marks the picked hexes on the map, replacing any marked before. An empty list clears the marks.
     *
     * @param hexes    the picked hexes, in the order they were picked
     * @param numbered {@code true} to number them in that order, for a route
     */
    void showPickedHexes(List<Coords> hexes, boolean numbered);

    /**
     * @return the board the map shows, used to turn picked hexes into hex numbers
     */
    Board getPickingBoard();

    /**
     * @return the map's component, used to place the picking dialog
     */
    Component getPickingComponent();
}
