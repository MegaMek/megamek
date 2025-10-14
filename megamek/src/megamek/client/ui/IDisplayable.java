/*
 * Copyright (C) 2000-2006 Ben Mazur (bmazur@sev.org)
 * This file (C) 2008 Jörg Walter <j.walter@syntax-k.de>
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Used for {@link megamek.client.ui.clientGUI.boardview.BoardView} overlays that don't move when the map is scrolled
 * (such as the chat window). Use BoardView1.addDisplayable() to add it to the
 * {@link megamek.client.ui.clientGUI.boardview.BoardView}.
 *
 * @author jwalt
 */
public interface IDisplayable {

    /**
     * Returns true when this IDisplayable is being dragged or resized using mouse movement. This will prevent the
     * {@link megamek.client.ui.clientGUI.boardview.BoardView} from reacting to this mouse action. The default for this
     * method will always return false.
     */
    default boolean isBeingDragged() {
        return false;
    }

    /**
     * Returns true when this IDisplayable is dragged or resized using mouse dragging. This will prevent the
     * {@link megamek.client.ui.clientGUI.boardview.BoardView} from reacting to this mouse action. The default for this
     * method will always return false.
     */
    default boolean isDragged(Point point, Dimension backSize) {
        return false;
    }

    /**
     * Returns true when the mouse position point is considered "within" this IDisplayable. This is called when a mouse
     * button is pressed. The default for this method will always return false.
     */
    default boolean isHit(Point point, Dimension size) {
        return false;
    }

    /**
     * Returns true when the mouse position point is considered "within" this IDisplayable. This is called when the
     * mouse or mouse wheel is moved. backSize is the pixel size of the
     * {@link megamek.client.ui.clientGUI.boardview.BoardView}. The default for this method will always return false.
     */
    default boolean isMouseOver(Point point, Dimension backSize) {
        return false;
    }

    default boolean isReleased() {
        return false;
    }

    /**
     * Draw this IDisplayable to the Graphics graph, which is the
     * {@link megamek.client.ui.clientGUI.boardview.BoardView} graphics. The currently visible part of the
     * {@link megamek.client.ui.clientGUI.boardview.BoardView} is given by the Rectangle rect, so the upper left corner
     * of the visible {@link megamek.client.ui.clientGUI.boardview.BoardView} is rect.x, rect.y.
     */
    void draw(Graphics graph, Rectangle rect);

    /**
     * Return true while sliding. "Sliding" means that this IDisplayable is in the process of opening, closing moving or
     * fading. The {@link megamek.client.ui.clientGUI.boardview.BoardView} will repaint at some fps while an
     * IDisplayable is sliding. The default for this method will always return false.
     */
    default boolean isSliding() {
        return false;
    }

    /**
     * The {@link megamek.client.ui.clientGUI.boardview.BoardView} calls this to pass on the elapsed time elTime to the
     * IDisplayable. when add is true, elTime is usually the elapsed time since the last call to setIdleTime and should
     * be added to a stored elapsed time. When add is false, elTime should replace the previously stored elapsed time
     * (this is usually used with elTime = 0 to reset the elapsed time). Can be used to make this IDisplayable "slide"
     * after some elapsed time, see slide(). See ChatterBox2 for examples. The default for this method will do nothing.
     */
    default void setIdleTime(long elTime, boolean add) {}

    /**
     * Conducts a frame update when sliding. "Sliding" means that this IDisplayable is in the process of opening,
     * closing moving or fading. The {@link megamek.client.ui.clientGUI.boardview.BoardView} will repaint at some fps
     * while an IDisplayable is sliding. Return true as long as the slide process is not finished. See ChatterBox2 and
     * KeyBindingsOverlay for examples. The default for this method will always return false.
     */
    default boolean slide() {
        return false;
    }
}
