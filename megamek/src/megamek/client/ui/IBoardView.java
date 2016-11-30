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
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.event.MechDisplayListener;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IPlayer;
import megamek.common.MovePath;
import megamek.common.actions.AttackAction;
import megamek.common.util.FiringSolution;

public interface IBoardView extends MechDisplayListener {

    public void addAttack(AttackAction saa);
    public void refreshAttacks();
    public void removeAttacksFor(Entity ce);

    public void refreshMinefields();
    public void markDeploymentHexesFor(Entity ce);
    public void redrawEntity(Entity ce);

    public Entity getDeployingEntity();

    public void drawMovementData(Entity ce, MovePath cmd);
    public void clearMovementData();
    public void setFiringSolutions(Entity attacker,
            Map<Integer,FiringSolution> firingSolutions);
    public void setMovementEnvelope(Map<Coords,Integer> mvEnvData,
            int walk, int run, int jump, int gear);
    public void clearFiringSolutionData();
    public void clearMovementEnvelope();
    public boolean isMovingUnits();

    public void addDisplayable(IDisplayable d);
    public void refreshDisplayables();

    public void drawRuler(Coords start, Coords end, Color startColor, Color endColor);

    public ITilesetManager getTilesetManager();

    public IPlayer getLocalPlayer();
    public void setLocalPlayer(IPlayer localPlayer);

    public void zoomIn();
    public void zoomOut();
    public boolean toggleIsometric();
    public void centerOnHex(Coords position);
    public void centerOnPointRel(double xrel, double yrel);
    public double[] getVisibleArea();
    
    public void stopSoftCentering();

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
     * Sets the color of the highlight cursor.
     * 
     * @param c  The color of the highlight cursor.
     */
    public abstract void setHighlightColor(Color c);

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

    /**
     * This method creates an image the size of the entire board (all
     * mapsheets), draws the hexes onto it, and returns that image.
     *
     * @param drawOnlyBoard If true, no units are drawn, only the board
     * @return
     */
    public abstract BufferedImage getEntireBoardImage(boolean ignoreUnits);

    /**
     * Sets the BoardView's currently selected entity.
     * @param e
     */
    public abstract void selectEntity(Entity e);

    public void die();
    
    /**
     * Returns true if the BoardView has an active chatter box else false.
     * @return
     */
    public boolean getChatterBoxActive();

    /**
     * Sets whether the BoardView has an active chatter box or not.
     * @param cba
     */
    public void setChatterBoxActive(boolean cba);

    /**
     * Returns any Entities that are flying over the given Coords.
     * @param c
     * @return
     */
    public List<Entity> getEntitiesFlyingOver(Coords c);

}