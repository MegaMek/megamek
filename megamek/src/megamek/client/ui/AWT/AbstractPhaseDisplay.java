/**
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

import java.awt.*;

import megamek.common.*;

public abstract class AbstractPhaseDisplay extends Panel implements BoardListener, GameListener {

    //
    // BoardListener
    //
    public void boardHexMoused(BoardEvent b) {
        ;
    }
    public void boardHexSelected(BoardEvent b) {
        ;
    }
    public void boardHexCursor(BoardEvent b) {
        ;
    }
    public void boardHexHighlighted(BoardEvent b) {
        ;
    }
    public void boardChangedHex(BoardEvent b) {
        ;
    }
    public void boardNewVis(BoardEvent b) {
        ;
    }
    public void boardNewBoard(BoardEvent b) {
        ;
    }
    public void boardFirstLOSHex(BoardEvent b) {
        ;
    }
    public void boardSecondLOSHex(BoardEvent b, Coords c) {
        ;
    }

    // GameListener
    //
    public void gamePlayerChat(GameEvent ev) {
        ;
    }
    public void gamePlayerStatusChange(GameEvent ev) {
        ;
    }
    public void gameTurnChange(GameEvent ev) {
        ;
    }
    public void gamePhaseChange(GameEvent ev) {
        ;
    }
    public void gameNewEntities(GameEvent ev) {
        ;
    }
    public void gameNewSettings(GameEvent ev) {
        ;
    }
    public void gameDisconnected(GameEvent e) {
        ;
    }
    public void gameBoardChanged(GameEvent e) {
        ;
    }
    public void gameEnd(GameEvent e) {
        ;
    }

    public void boardChangedEntity(BoardEvent b) {
        ;
    }

    public void boardNewAttack(BoardEvent a) {
        ;
    }

    public void gameReport(GameEvent e) {
        ;
    }

    public void gameMapQuery(GameEvent e) {
        ;
    }

}
