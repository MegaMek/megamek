/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.swing.util.CommandAction;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.*;
import megamek.common.Configuration;
import megamek.common.Coords;
import megamek.common.event.*;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableAdapter;
import megamek.common.util.fileUtils.MegaMekFile;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URI;

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;

public abstract class AbstractPhaseDisplay extends SkinnedJPanel implements
        BoardViewListener, GameListener, Distractable {
    private static final long serialVersionUID = 4421205210788230341L;

    public static final int DONE_BUTTON_WIDTH = 125;
    // Distraction implementation.
    protected DistractableAdapter distracted = new DistractableAdapter();

    protected MegamekButton butDone;

    protected ClientGUI clientgui;

    ImageIcon backgroundIcon = null;

    protected AbstractPhaseDisplay(ClientGUI cg) {
        this(cg, SkinSpecification.UIComponents.PhaseDisplay.getComp(),
                SkinSpecification.UIComponents.PhaseDisplayDoneButton.getComp());
    }

    protected AbstractPhaseDisplay(ClientGUI cg, String borderSkinComp,
            String buttonSkinComp) {
        super(borderSkinComp, 0);
        this.clientgui = cg;
        SkinSpecification pdSkinSpec = SkinXMLHandler.getSkin(borderSkinComp);

        try {
            if (!pdSkinSpec.backgrounds.isEmpty()) {
                File file = new MegaMekFile(Configuration.widgetsDir(),
                        pdSkinSpec.backgrounds.get(0)).getFile();
                URI imgURL = file.toURI();
                if (!file.exists()) {
                    LogManager.getLogger().error("PhaseDisplay icon doesn't exist: " + file.getAbsolutePath());
                } else {
                    backgroundIcon = new ImageIcon(imgURL.toURL());
                }
            }
        } catch (Exception e) {
            LogManager.getLogger().error("Error loading PhaseDisplay background image!", e);
        }

        setBorder(new MegamekBorder(borderSkinComp));
        butDone = new MegamekButton("DONE", buttonSkinComp);
        String f = guiScaledFontHTML(UIUtil.uiLightViolet()) +  KeyCommandBind.getDesc(KeyCommandBind.DONE)+ "</FONT>";
        butDone.setToolTipText("<html><body>" + f + "</body></html>");
        butDone.setActionCommand("doneButton");
        if (clientgui != null) {
            butDone.addActionListener(new AbstractAction() {
                private static final long serialVersionUID = -5034474968902280850L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isIgnoringEvents()) {
                        return;
                    }
                    if ((clientgui.getClient().isMyTurn())
                            || (clientgui.getClient().getGame().getTurn() == null)
                            || (clientgui.getClient().getGame().getPhase().isReport())) {
                        ready();
                        // When the turn is ended, we could miss a key release
                        // event
                        // This will ensure no repeating keys are stuck down
                        clientgui.controller.stopAllRepeating();
                    }
                }
            });

            final AbstractPhaseDisplay display = this;
            // Register the action for DONE
            clientgui.controller.registerCommandAction(KeyCommandBind.DONE.cmd,
                    new CommandAction() {

                        @Override
                        public boolean shouldPerformAction() {
                            if (((!clientgui.getClient().isMyTurn()
                                    && (clientgui.getClient().getGame().getTurn() != null)
                                    && (!clientgui.getClient().getGame().getPhase().isReport())))
                                    || clientgui.getBoardView().getChatterBoxActive()
                                    || display.isIgnoringEvents()
                                    || !display.isVisible()
                                    || !butDone.isEnabled()) {
                                return false;
                            } else {
                                return true;
                            }
                        }

                        @Override
                        public void performAction() {
                            ready();
                        }
                    });
        }
    }

    /**
     * Determine if the listener is currently distracted.
     *
     * @return <code>true</code> if the listener is ignoring events.
     */
    @Override
    public boolean isIgnoringEvents() {
        return distracted.isIgnoringEvents();
    }

    /**
     * Specify if the listener should be distracted.
     *
     * @param distracted
     *            <code>true</code> if the listener should ignore events
     *            <code>false</code> if the listener should pay attention
     *            again. Events that occurred while the listener was distracted
     *            NOT going to be processed.
     */
    @Override
    public void setIgnoringEvents(boolean distracted) {
        this.distracted.setIgnoringEvents(distracted);
    }

    //
    // BoardListener
    //
    @Override
    public void hexMoused(BoardViewEvent b) {
        //noaction default
    }

    @Override
    public void hexSelected(BoardViewEvent b) {
        //noaction default
    }

    @Override
    public void hexCursor(BoardViewEvent b) {
        //noaction default
    }

    @Override
    public void boardHexHighlighted(BoardViewEvent b) {
        //noaction default
    }

    @Override
    public void firstLOSHex(BoardViewEvent b) {
        //noaction default
    }

    @Override
    public void secondLOSHex(BoardViewEvent b, Coords c) {
        //noaction default
    }

    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        //noaction default
    }

    @Override
    public void unitSelected(BoardViewEvent b) {
        //noaction default
    }

    // GameListener
    //

    @Override
    public void gamePlayerConnected(GamePlayerConnectedEvent e) {
        //noaction default
    }

    @Override
    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
        //noaction default
    }

    @Override
    public void gamePlayerChange(GamePlayerChangeEvent e) {
        //noaction default
    }

    @Override
    public void gamePlayerChat(GamePlayerChatEvent e) {
        //noaction default
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        //noaction default
    }

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
        //noaction default
    }

    @Override
    public void gameReport(GameReportEvent e) {
        //noaction default
    }

    @Override
    public void gameEnd(GameEndEvent e) {
        //noaction default
    }

    @Override
    public void gameBoardNew(GameBoardNewEvent e) {
        //noaction default
    }

    @Override
    public void gameBoardChanged(GameBoardChangeEvent e) {
        //noaction default
    }

    @Override
    public void gameSettingsChange(GameSettingsChangeEvent e) {
        //noaction default
    }

    @Override
    public void gameMapQuery(GameMapQueryEvent e) {
        //noaction default
    }

    @Override
    public void gameEntityNew(GameEntityNewEvent e) {
        //noaction default
    }

    @Override
    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
        //noaction default
    }

    @Override
    public void gameEntityRemove(GameEntityRemoveEvent e) {
        //noaction default
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent e) {
        //noaction default
    }

    @Override
    public void gameNewAction(GameNewActionEvent e) {
        //noaction default
    }

    @Override
    public void gameClientFeedbackRequest(GameCFREvent evt) {
        //noaction default
    }

    @Override
    public void gameVictory(GameVictoryEvent e) {
        //noaction default
    }

    public void ready() {
    }
    // needed for turn timer to add timer display to GUI
    public ClientGUI getClientgui() {
        return clientgui;
    }
}
