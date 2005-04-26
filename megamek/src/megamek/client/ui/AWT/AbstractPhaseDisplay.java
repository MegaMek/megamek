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

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.common.*;
import megamek.common.event.GameAttackEvent;
import megamek.common.event.GameBoardChangeEvent;
import megamek.common.event.GameBoardNewEvent;
import megamek.common.event.GameEndEvent;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GameEntityNewEvent;
import megamek.common.event.GameEntityNewOffboardEvent;
import megamek.common.event.GameEntityRemoveEvent;
import megamek.common.event.GameListener;
import megamek.common.event.GameMapQueryEvent;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GamePlayerChangeEvent;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.GamePlayerConnectedEvent;
import megamek.common.event.GamePlayerDisconnectedEvent;
import megamek.common.event.GameReportEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.event.GameTurnChangeEvent;

public class AbstractPhaseDisplay extends Panel implements BoardViewListener, GameListener {
    
    //
    // BoardListener
    //
    public void boardHexMoused(BoardViewEvent b) {
    }
    
    public void boardHexSelected(BoardViewEvent b) {
    }
    
    public void boardHexCursor(BoardViewEvent b) {
    }
    
    public void boardHexHighlighted(BoardViewEvent b) {
    }
    
    public void boardFirstLOSHex(BoardViewEvent b) {
    }
    
    public void boardSecondLOSHex(BoardViewEvent b, Coords c) {
    }
    
    /* (non-Javadoc)
     * @see megamek.client.BoardViewListener#finishedMovingUnits(megamek.client.BoardViewEvent)
     */
    public void finishedMovingUnits(BoardViewEvent b) {
    }
    
    /* (non-Javadoc)
     * @see megamek.client.BoardViewListener#selectUnit(megamek.client.BoardViewEvent)
     */
    public void selectUnit(BoardViewEvent b) {
    }
    
    // GameListener
    //
    
    /* (non-Javadoc)
     * @see megamek.common.GameListener#gamePlayerConnected(megamek.common.GamePlayerConnectedEvent)
     */
    public void gamePlayerConnected(GamePlayerConnectedEvent e) {
    }
    
    /* (non-Javadoc)
     * @see megamek.common.GameListener#gamePlayerDisconnected(megamek.common.GamePlayerDisconnectedEvent)
     */
    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
    }
    
    /* (non-Javadoc)
     * @see megamek.common.GameListener#gamePlayerChange(megamek.common.GamePlayerChangeEvent)
     */
    public void gamePlayerChange(GamePlayerChangeEvent e) {
    }
    
    /* (non-Javadoc)
     * @see megamek.common.GameListener#gamePlayerChat(megamek.common.GamePlayerChatEvent)
     */
    public void gamePlayerChat(GamePlayerChatEvent e) {
    }
    
    /* (non-Javadoc)
     * @see megamek.common.GameListener#gamePhaseChange(megamek.common.GamePhaseChangeEvent)
     */
    public void gamePhaseChange(GamePhaseChangeEvent e) {
    }
    
    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameTurnChange(megamek.common.GameTurnChangeEvent)
     */
    public void gameTurnChange(GameTurnChangeEvent e) {
    }
    
    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameReport(megamek.common.GameReportEvent)
     */
    public void gameReport(GameReportEvent e) {
    }
    
    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameEnd(megamek.common.GameEndEvent)
     */
    public void gameEnd(GameEndEvent e) {
    }
    
    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameBoardNew(megamek.common.GameBoardNewEvent)
     */
    public void gameBoardNew(GameBoardNewEvent e) {
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameBoardChanged(megamek.common.GameBoardChangeEvent)
     */
    public void gameBoardChanged(GameBoardChangeEvent e) {
    }
    
    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameSettingsChange(megamek.common.GameSettingsChangeEvent)
     */
    public void gameSettingsChange(GameSettingsChangeEvent e) {
    }
    
    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameMapQuery(megamek.common.GameMapQueryEvent)
     */
    public void gameMapQuery(GameMapQueryEvent e) {
    }
    
    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameEntityNew(megamek.common.GameEntityNewEvent)
     */
    public void gameEntityNew(GameEntityNewEvent e) {
    }
    
    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameEntityNewOffboard(megamek.common.GameEntityNewOffboardEvent)
     */
    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
    }
    
    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameEntityRemove(megamek.common.GameEntityRemoveEvent)
     */
    public void gameEntityRemove(GameEntityRemoveEvent e) {
    }

    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameEntityChange(megamek.common.GameEntityChangeEvent)
     */
    public void gameEntityChange(GameEntityChangeEvent e) {
    }
    
    /* (non-Javadoc)
     * @see megamek.common.GameListener#gameAttack(megamek.common.GameAttackEvent)
     */
    public void gameAttack(GameAttackEvent e) {
    }
    
}
