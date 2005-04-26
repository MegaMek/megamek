/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.common.Coords;

public interface IBoardView {

    public static final int BOARD_HEX_CLICK = 1;

    public static final int BOARD_HEX_DOUBLECLICK = 2;

    public static final int BOARD_HEX_DRAG = 3;

    /**
     * @param lastCursor The lastCursor to set.
     */
    public abstract void setLastCursor(Coords lastCursor);

    /**
     * @return Returns the lastCursor.
     */
    public abstract Coords getLastCursor();

    /**
     * @param highlighted The highlighted to set.
     */
    public abstract void setHighlighted(Coords highlighted);

    /**
     * @return Returns the highlighted.
     */
    public abstract Coords getHighlighted();

    /**
     * @param selected The selected to set.
     */
    public abstract void setSelected(Coords selected);

    /**
     * @return Returns the selected.
     */
    public abstract Coords getSelected();

    /**
     * @param firstLOS The firstLOS to set.
     */
    public abstract void setFirstLOS(Coords firstLOS);

    /**
     * @return Returns the firstLOS.
     */
    public abstract Coords getFirstLOS();

    /**
     * Determines if this Board contains the Coords,
     * and if so, "selects" that Coords.
     *
     * @param coords the Coords.
     */
    public abstract void select(Coords coords);

    /**
     * "Selects" the specified Coords.
     *
     * @param x the x coordinate.
     * @param y the y coordinate.
     */
    public abstract void select(int x, int y);

    /**
     * Determines if this Board contains the Coords,
     * and if so, highlights that Coords.
     *
     * @param coords the Coords.
     */
    public abstract void highlight(Coords coords);

    /**
     * Highlights the specified Coords.
     *
     * @param x the x coordinate.
     * @param y the y coordinate.
     */
    public abstract void highlight(int x, int y);

    /**
     * Determines if this Board contains the Coords,
     * and if so, "cursors" that Coords.
     *
     * @param coords the Coords.
     */
    public abstract void cursor(Coords coords);

    /**
     * "Cursors" the specified Coords.
     *
     * @param x the x coordinate.
     * @param y the y coordinate.
     */
    public abstract void cursor(int x, int y);

    public abstract void checkLOS(Coords c);

    /**
     * Determines if this Board contains the (x, y) Coords,
     * and if so, notifies listeners about the specified mouse
     * action.
     */
    public abstract void mouseAction(int x, int y, int mtype, int modifiers);

    /**
     * Notifies listeners about the specified mouse action.
     *
     * @param coords the Coords.
     */
    public abstract void mouseAction(Coords coords, int mtype, int modifiers);

    /**
     * Adds the specified board listener to receive
     * board events from this board.
     *
     * @param listener the board listener.
     */
    public abstract void addBoardViewListener(BoardViewListener listener);

    /**
     * Removes the specified board listener.
     *
     * @param listener the board listener.
     */
    public abstract void removeBoardViewListener(BoardViewListener listener);

    /**
     * Notifies attached board listeners of the event.
     *
     * @param event the board event.
     */
    public abstract void processBoardViewEvent(BoardViewEvent event);
}