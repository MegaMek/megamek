package megamek.client.ui.swing.boardview;

import megamek.client.event.BoardViewListener;
import megamek.client.ui.IDisplayable;
import megamek.common.Coords;

import javax.swing.*;
import java.awt.*;
import java.awt.image.RenderedImage;

public interface IBoardView {

    void draw(Graphics graphics);

    /**
     * Zooms out the board (shows more of it at smaller size), if the minimum zoom has not been reached already.
     */
    void zoomOut();

    /**
     * Zooms in the board (shows less of it at bigger size), if the maximum zoom has not been reached already.
     */
    void zoomIn();

    Component getComponent();

    void setDisplayInvalidFields(boolean displayInvalidFields);

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
     * Placeholder: this is only an idea; can we make draw modifications modular?
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

    void centerOn(Coords coords);
}
