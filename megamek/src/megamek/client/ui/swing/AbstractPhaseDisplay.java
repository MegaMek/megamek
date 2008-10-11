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

package megamek.client.ui.swing;

import javax.swing.JPanel;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.common.Coords;
import megamek.common.event.GameBoardChangeEvent;
import megamek.common.event.GameBoardNewEvent;
import megamek.common.event.GameEndEvent;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GameEntityNewEvent;
import megamek.common.event.GameEntityNewOffboardEvent;
import megamek.common.event.GameEntityRemoveEvent;
import megamek.common.event.GameListener;
import megamek.common.event.GameMapQueryEvent;
import megamek.common.event.GameNewActionEvent;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GamePlayerChangeEvent;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.GamePlayerConnectedEvent;
import megamek.common.event.GamePlayerDisconnectedEvent;
import megamek.common.event.GameReportEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableAdapter;

public abstract class AbstractPhaseDisplay extends JPanel implements BoardViewListener,
        GameListener, Distractable {

    /**
     * 
     */
    private static final long serialVersionUID = 4421205210788230341L;

    // Distraction implementation.
    protected DistractableAdapter distracted = new DistractableAdapter();

    /**
     * Determine if the listener is currently distracted.
     * 
     * @return <code>true</code> if the listener is ignoring events.
     */
    public boolean isIgnoringEvents() {
        return this.distracted.isIgnoringEvents();
    }

    /**
     * Specify if the listener should be distracted.
     * 
     * @param distracted
     *            <code>true</code> if the listener should ignore events
     *            <code>false</code> if the listener should pay attention
     *            again. Events that occured while the listener was distracted
     *            NOT going to be processed.
     */
    public void setIgnoringEvents(boolean distracted) {
        this.distracted.setIgnoringEvents(distracted);
    }

    //
    // BoardListener
    //
    public void hexMoused(BoardViewEvent b) {
    }

    public void hexSelected(BoardViewEvent b) {
    }

    public void hexCursor(BoardViewEvent b) {
    }

    public void boardHexHighlighted(BoardViewEvent b) {
    }

    public void firstLOSHex(BoardViewEvent b) {
    }

    public void secondLOSHex(BoardViewEvent b, Coords c) {
    }

    public void finishedMovingUnits(BoardViewEvent b) {
    }

    public void unitSelected(BoardViewEvent b) {
    }

    // GameListener
    //

    public void gamePlayerConnected(GamePlayerConnectedEvent e) {
    }

    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
    }

    public void gamePlayerChange(GamePlayerChangeEvent e) {
    }

    public void gamePlayerChat(GamePlayerChatEvent e) {
    }

    public void gamePhaseChange(GamePhaseChangeEvent e) {
    }

    public void gameTurnChange(GameTurnChangeEvent e) {
    }

    public void gameReport(GameReportEvent e) {
    }

    public void gameEnd(GameEndEvent e) {
    }

    public void gameBoardNew(GameBoardNewEvent e) {
    }

    public void gameBoardChanged(GameBoardChangeEvent e) {
    }

    public void gameSettingsChange(GameSettingsChangeEvent e) {
    }

    public void gameMapQuery(GameMapQueryEvent e) {
    }

    public void gameEntityNew(GameEntityNewEvent e) {
    }

    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
    }

    public void gameEntityRemove(GameEntityRemoveEvent e) {
    }

    public void gameEntityChange(GameEntityChangeEvent e) {
    }

    public void gameNewAction(GameNewActionEvent e) {
    }

}
