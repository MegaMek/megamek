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

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

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

    protected JButton butDone;

    protected ClientGUI clientgui;

    protected AbstractPhaseDisplay() {
        butDone = new JButton();
        butDone.setActionCommand("doneButton");
        butDone.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = -5034474968902280850L;

            public void actionPerformed(ActionEvent e) {
                if (isIgnoringEvents()) {
                    return;
                }
                if (clientgui.getClient().isMyTurn() || (clientgui.getClient().getGame().getTurn() == null)) {
                    ready();
                }
            }
        });
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,InputEvent.CTRL_DOWN_MASK),
        "doneButton");

        getActionMap().put("doneButton", new AbstractAction() {
            private static final long serialVersionUID = -5034474968902280850L;

            public void actionPerformed(ActionEvent e) {
                if (isIgnoringEvents()) {
                    return;
                }
                if (clientgui.getClient().isMyTurn() || (clientgui.getClient().getGame().getTurn() == null)) {
                    ready();
                }
            }
        });
    }

    /**
     * Determine if the listener is currently distracted.
     *
     * @return <code>true</code> if the listener is ignoring events.
     */
    public boolean isIgnoringEvents() {
        return distracted.isIgnoringEvents();
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
        //noaction default
    }

    public void hexSelected(BoardViewEvent b) {
        //noaction default
    }

    public void hexCursor(BoardViewEvent b) {
        //noaction default
    }

    public void boardHexHighlighted(BoardViewEvent b) {
        //noaction default
    }

    public void firstLOSHex(BoardViewEvent b) {
        //noaction default
    }

    public void secondLOSHex(BoardViewEvent b, Coords c) {
        //noaction default
    }

    public void finishedMovingUnits(BoardViewEvent b) {
        //noaction default
    }

    public void unitSelected(BoardViewEvent b) {
        //noaction default
    }

    // GameListener
    //

    public void gamePlayerConnected(GamePlayerConnectedEvent e) {
        //noaction default
    }

    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
        //noaction default
    }

    public void gamePlayerChange(GamePlayerChangeEvent e) {
        //noaction default
    }

    public void gamePlayerChat(GamePlayerChatEvent e) {
        //noaction default
    }

    public void gamePhaseChange(GamePhaseChangeEvent e) {
        //noaction default
    }

    public void gameTurnChange(GameTurnChangeEvent e) {
        //noaction default
    }

    public void gameReport(GameReportEvent e) {
        //noaction default
    }

    public void gameEnd(GameEndEvent e) {
        //noaction default
    }

    public void gameBoardNew(GameBoardNewEvent e) {
        //noaction default
    }

    public void gameBoardChanged(GameBoardChangeEvent e) {
        //noaction default
    }

    public void gameSettingsChange(GameSettingsChangeEvent e) {
        //noaction default
    }

    public void gameMapQuery(GameMapQueryEvent e) {
        //noaction default
    }

    public void gameEntityNew(GameEntityNewEvent e) {
        //noaction default
    }

    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
        //noaction default
    }

    public void gameEntityRemove(GameEntityRemoveEvent e) {
        //noaction default
    }

    public void gameEntityChange(GameEntityChangeEvent e) {
        //noaction default
    }

    public void gameNewAction(GameNewActionEvent e) {
        //noaction default
    }

    public void ready() {
    }
}
