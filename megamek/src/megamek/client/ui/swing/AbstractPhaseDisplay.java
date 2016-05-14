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

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URI;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.swing.util.CommandAction;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.widget.MegamekBorder;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.common.Configuration;
import megamek.common.Coords;
import megamek.common.event.GameBoardChangeEvent;
import megamek.common.event.GameBoardNewEvent;
import megamek.common.event.GameCFREvent;
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
import megamek.common.event.GameVictoryEvent;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableAdapter;

public abstract class AbstractPhaseDisplay extends JPanel implements 
        BoardViewListener, GameListener, Distractable {

    /**
     *
     */
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
        this.clientgui = cg;
        SkinSpecification pdSkinSpec = SkinXMLHandler.getSkin(borderSkinComp);

        try {
            if (pdSkinSpec.backgrounds.size() > 0){
                File file = new File(Configuration.widgetsDir(), 
                        pdSkinSpec.backgrounds.get(0));
                URI imgURL = file.toURI();
                if (!file.exists()){
                    System.err.println("PhaseDisplay Error: icon doesn't exist: "
                            + file.getAbsolutePath());
                } else {
                    backgroundIcon = new ImageIcon(imgURL.toURL());
                }
            }
        } catch (Exception e){
            System.out.println("Error loading PhaseDisplay background image!");
            System.out.println(e.getMessage());
        }
        
        setBorder(new MegamekBorder(borderSkinComp));
        butDone = new MegamekButton("",buttonSkinComp);
        butDone.setActionCommand("doneButton");
        if (clientgui != null) {
            butDone.addActionListener(new AbstractAction() {
                private static final long serialVersionUID = -5034474968902280850L;

                public void actionPerformed(ActionEvent e) {
                    if (isIgnoringEvents()) {
                        return;
                    }
                    if (clientgui.getClient().isMyTurn()
                            || (clientgui.getClient().getGame().getTurn() == null)) {
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
                            if ((!clientgui.getClient().isMyTurn() && (clientgui
                                    .getClient().getGame().getTurn() != null))
                                    || clientgui.bv.getChatterBoxActive()
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
    
    protected void paintComponent(Graphics g) {
        if (backgroundIcon == null){
            super.paintComponent(g);
            return;
        }
        int w = getWidth();
        int h = getHeight();
        int iW = backgroundIcon.getIconWidth();
        int iH = backgroundIcon.getIconHeight();
        // If the image isn't loaded, prevent an infinite loop
        if ((iW < 1) || (iH < 1)) {
            return;
        }
        for (int x = 0; x < w; x+=iW){
            for (int y = 0; y < h; y+=iH){
                g.drawImage(backgroundIcon.getImage(), x, y, 
                        backgroundIcon.getImageObserver());
            }
        }
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
    
    @Override
    public void gameClientFeedbackRquest(GameCFREvent evt) {
        //noaction default
    }
    
    @Override
    public void gameVictory(GameVictoryEvent e) {
        //noaction default
    }

    public void ready() {
    }
}
