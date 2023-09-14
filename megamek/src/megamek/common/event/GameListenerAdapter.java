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

package megamek.common.event;

/**
 * This adapter class provides default implementations for the methods described
 * by the <code>GameListener</code> interface.
 * <p>
 * Classes that wish to deal with <code>GamedEvent</code>s can extend this
 * class and override only the methods which they are interested in.
 * </p>
 * 
 * @see GameListener
 * @see GameEvent
 */
public class GameListenerAdapter implements GameListener {

    @Override
    public void gamePlayerConnected(GamePlayerConnectedEvent e) {
    }

    @Override
    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
    }

    @Override
    public void gamePlayerChange(GamePlayerChangeEvent e) {
    }

    @Override
    public void gamePlayerChat(GamePlayerChatEvent e) {
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
    }

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
    }

    @Override
    public void gameReport(GameReportEvent e) {
    }

    @Override
    public void gameEnd(GameEndEvent e) {
    }

    @Override
    public void gameBoardNew(GameBoardNewEvent e) {
    }

    @Override
    public void gameBoardChanged(GameBoardChangeEvent e) {
    }

    @Override
    public void gameSettingsChange(GameSettingsChangeEvent e) {
    }

    @Override
    public void gameMapQuery(GameMapQueryEvent e) {
    }

    @Override
    public void gameEntityNew(GameEntityNewEvent e) {
    }

    @Override
    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
    }

    @Override
    public void gameEntityRemove(GameEntityRemoveEvent e) {
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent e) {
    }

    @Override
    public void gameNewAction(GameNewActionEvent e) {
    }
    
    @Override
    public void gameClientFeedbackRequest(GameCFREvent evt) {
    }

    @Override
    public void gameVictory(GameVictoryEvent e) {        
    }    

    @Override
    public void gameUnitDied(GameUnitDiedEvent e){
    }
}
