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

package megamek.client.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyListener;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.event.MechDisplayListener;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.MovePath;
import megamek.common.Player;
import megamek.common.actions.AttackAction;

public interface IBoardView extends MechDisplayListener {

    public void addAttack(AttackAction saa);
    public void refreshAttacks();
    public void removeAttacksFor(Entity ce);

    public void refreshMinefields();
    public void markDeploymentHexesFor(Player p);
    public void redrawEntity(Entity ce);

    public void drawMovementData(Entity ce, MovePath cmd);
    public void clearMovementData();
    public boolean isMovingUnits();

    public void addDisplayable(IDisplayable d);
    public void refreshDisplayables();

    public void drawRuler(Coords start, Coords end, Color startColor, Color endColor);

    public ITilesetManager getTilesetManager();

    public Player getLocalPlayer();
    public void setLocalPlayer(Player localPlayer);

    public void zoomIn();
    public void zoomOut();
    public void centerOnHex(Coords position);

    // it's a hack that the popup is Object, but we use this interface
    // for both AWT and swing, and AWT's PopupMenu and Swing's JPopupMenu
    // don't inherit from a common class apart from Object
    public void showPopup(Object popup, Coords c);
    public void hideTooltip();

    public void addKeyListener(KeyListener k);

    public Component getComponent();

    /**
     * @return Returns the lastCursor.
     */
    public abstract Coords getLastCursor();

    /**
     * @return Returns the selected.
     */
    public abstract Coords getSelected();

    /**
     * @param firstLOS The firstLOS to set.
     */
    public abstract void setFirstLOS(Coords firstLOS);

    /**
     * Determines if this Board contains the Coords, and if so, "selects" that
     * Coords.
     * 
     * @param coords the Coords.
     */
    public abstract void select(Coords coords);

    /**
     * Determines if this Board contains the Coords, and if so, highlights that
     * Coords.
     * 
     * @param coords the Coords.
     */
    public abstract void highlight(Coords coords);

    /**
     * Determines if this Board contains the Coords, and if so, "cursors" that
     * Coords.
     * 
     * @param coords the Coords.
     */
    public abstract void cursor(Coords coords);

    public abstract void checkLOS(Coords c);

    /**
     * Adds the specified board view listener to receive events from this view.
     * 
     * @param listener the board listener.
     * @see BoardViewListener
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