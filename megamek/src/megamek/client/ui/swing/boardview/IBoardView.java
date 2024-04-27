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

import megamek.client.event.BoardViewListener;
import megamek.client.ui.IDisplayable;
import megamek.common.Coords;

import javax.swing.*;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.util.Set;

public interface IBoardView {

    /**
     * This method should be overridden to do the actual drawing of the board image into the provided
     * Graphics.
     *
     * @param graphics The Graphics object to draw the board onto
     */
    void draw(Graphics graphics);

    /**
     * Zooms out the board (shows more of it at smaller size), if the minimum zoom has not been reached already.
     */
    void zoomOut();

    /**
     * Zooms in the board (shows less of it at bigger size), if the maximum zoom has not been reached already.
     */
    void zoomIn();

    /**
     * Returns the pixel size of the entire board if drawn at the current zoom level. This should not include
     * any padding, just the board itself.
     *
     * @return The pixel size of the entire board at the current zoom level
     */
    Dimension getBoardSize();

    /**
     * @return a JScrollPane containing the board's panel.
     */
    Component getComponent();

    /**
     * Sets this BoardView to show or hide a warning in fields (hexes) that contain invalid information such
     * as terrains that cannot be used together in a single hex. Usually this warning is shown in the board
     * editor but not in a game.
     *
     * @param displayInvalidFields True when the invaliud marker should be shown
     */
    void setDisplayInvalidFields(boolean displayInvalidFields);

    default boolean displayInvalidFields() {
        return false;
    }

    /**
     * Sets the boardview to use the given player ID as the player in whose client this boardview is shown.
     * This may affect what is shown and what is hidden in this boardview; it will also affect what is
     * considered an enemy and ally.
     *
     * @param playerId The local player's ID as stored in the game.
     */
    void setLocalPlayer(int playerId);

    /**
     * Frees the resources this boardview uses and removes listeners. Call when this boardview is not
     * used anymore. The boardview will no longer be functional after calling this method. When overriding
     * this method, include a call to super.dispose().
     */
    void dispose();

    void setUseLosTool(boolean useLosTool);

    /**
     * @return The JPanel that contains this BoardView.
     */
    JPanel getPanel();

    /**
     * Adds the specified board listener to receive board events from this
     * board.
     *
     * @param listener the board listener.
     */
    void addBoardViewListener(BoardViewListener listener);

    /**
     * Removes the specified board listener.
     *
     * @param listener the board listener.
     */
    void removeBoardViewListener(BoardViewListener listener);

    /**
     * Adds the given overlay to this boardview. Overlays are displayed above the actual board and fixed
     * with respect to the screen, like the chatbox or unit overview.
     *
     * @param overlay The overlay to add
     */
    void addOverlay(IDisplayable overlay);

    /**
     * Removes the given overlay from this boardview.
     *
     * @param overlay The overlay to add
     * @see #addOverlay(IDisplayable)
     */
    void removeOverlay(IDisplayable overlay);

    /**
     * Placeholder: this is only an idea; can we make draw modifications modular? Like field of fire,
     * field of view...
     * Adds the given field modifier to this boardview. Field modifiers modify the drawing of the board's
     * fields, e.g. hexes.
     */
    default void addFieldModifier() {} // (FieldModifier modifier);

    /**
     * Returns an image of the entire board. Depending on parameters, the board may contain any currently present
     * units or other game objects and it may be drawn at zoom 1 or the current zoom level.
     *
     * @param hideUnits If true, no units are drawn, only the board
     * @param useBaseZoom If true, zoom = 1 is used, otherwise the current board zoom
     * @return an image of the whole board
     */
    RenderedImage getEntireBoardImage(boolean hideUnits, boolean useBaseZoom);

    /**
     * Notifies this BoardView to center itself on the given Coords.
     * Override this to be safe for null Coords.
     *
     * @param coords The coordinates to center on
     */
    void centerOnHex(Coords coords);

    /**
     * @return A set of hashCodes of those image that are animated (such as animated tileset images) and
     * therefore should prevent the hex image from being cached.
     */
    Set<Integer> getAnimatedImages();

    /**
     * Override this to provide a return value exactly as the Scrollable interface methodof the same name.
     * @see Scrollable#getScrollableUnitIncrement(Rectangle, int, int)
     */
    int getScrollableUnitIncrement(Rectangle arg0, int arg1, int arg2);

    /**
     * Override this to provide a return value exactly as the Scrollable interface methodof the same name.
     * @see Scrollable#getScrollableBlockIncrement(Rectangle, int, int)
     */
    int getScrollableBlockIncrement(Rectangle arg0, int arg1, int arg2);

    /**
     * @return the coords at the specified point in the BoardView's image area. The point may be given
     * e.g. as part of a MouseEvent.
     */
    Coords getCoordsAt(Point point);

    void setTooltipProvider(BoardViewTooltipProvider provider);
}
