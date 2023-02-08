/*
 * Copyright (c) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.*;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.*;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.TurnTimer;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.*;
import megamek.common.GameTurn;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.KeyBindParser;
import megamek.common.preference.*;

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;
import static megamek.client.ui.swing.util.UIUtil.uiLightViolet;

/**
 * This is a parent class for the button display for each phase.  Every phase 
 * has a panel of control buttons along with a Done button. Each button
 * correspondes to a command that can be carried out in the current phase.  
 * This class formats the button panel, the done button, and a status display area. 
 * Control buttons are grouped and the groups can be cycled through.
 */
public abstract class StatusBarPhaseDisplay extends AbstractPhaseDisplay
        implements ActionListener, MouseListener, KeyListener, IPreferenceChangeListener {
    
    protected static final Dimension MIN_BUTTON_SIZE = new Dimension(32, 32);
    protected static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final int BUTTON_ROWS = 2;
    private static final String SBPD_KEY_CLEARBUTTON = "clearButton";

    /**
     * timer that ends turn if time limit set in options is over
     */
    private TurnTimer tt;

    /**
     * Interface that defines what a command for a phase is.
     * @author arlith
     */
    public interface PhaseCommand {
        String getCmd();
        int getPriority();
        void setPriority(int p);
    }
    
    /**
     * Comparator for comparing the priority of two commands, used to determine
     * button order.
     * @author arlith
     */
    public static class CommandComparator implements Comparator<PhaseCommand>
    {
        @Override
        public int compare(PhaseCommand c1, PhaseCommand c2) {
            return c1.getPriority() - c2.getPriority();            
        }
    }
    
    private JLabel labStatus;
    protected JPanel panStatus = new JPanel();
    protected JPanel panButtons = new JPanel();  
    
    /** The button group that is currently displayed */
    protected int currentButtonGroup = 0;
    
    /** The number of button groups there are, needs to be computed in a child class. */
    protected int numButtonGroups;
    
    protected int buttonsPerRow = GUIP.getInt(GUIPreferences.ADVANCED_BUTTONS_PER_ROW);
    protected int buttonsPerGroup = BUTTON_ROWS * buttonsPerRow;

    protected StatusBarPhaseDisplay(ClientGUI cg) {
        super(cg);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), SBPD_KEY_CLEARBUTTON);
        getActionMap().put(SBPD_KEY_CLEARBUTTON, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isIgnoringEvents()) {
                    return;
                }
                if (clientgui.getBoardView().getChatterBoxActive()) {
                    clientgui.getBoardView().setChatterBoxActive(false);
                    clientgui.cb2.clearMessage();
                } else if (clientgui.getClient().isMyTurn() || (e.getSource() instanceof MovementDisplay)) {
                    // Users can draw movement envelope during the movement phase 
                    // even if it's not their turn, so we always want to be able
                    // to clear. MovementDisplay.clear() can handle this case
                    clear();
                }
            }
        });
        
        panButtons.setLayout(new BoxLayout(panButtons, BoxLayout.LINE_AXIS));
        panButtons.setOpaque(false);        
        panButtons.addKeyListener(this);
        panStatus.setOpaque(false);
        panStatus.addKeyListener(this);
        
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(panButtons);
        add(panStatus);
        
        GUIP.addPreferenceChangeListener(this);
        KeyBindParser.addPreferenceChangeListener(this);
        ToolTipManager.sharedInstance().registerComponent(this);
    }
    
    
    /** Returns the list of buttons that should be displayed. */
    protected abstract ArrayList<MegamekButton> getButtonList();

    /** set button that should be displayed. */
    protected abstract void setButtons();

    protected MegamekButton createButton(String cmd, String keyPrefix){
        String title = Messages.getString(keyPrefix + cmd);
        MegamekButton newButton = new MegamekButton(title, SkinSpecification.UIComponents.PhaseDisplayButton.getComp());
        newButton.addActionListener(this);
        newButton.setActionCommand(cmd);
        newButton.setEnabled(false);
        return newButton;
    }

    /** set button tool tips that should be displayed. */
    protected abstract void setButtonsTooltips();

    protected String createToolTip(String cmd, String keyPrefix, String hotKeyDesc) {
        String h  = "";
        String ttKey = keyPrefix + cmd + ".tooltip";
        String tt = hotKeyDesc;
        if (!tt.isEmpty()) {
            String title = Messages.getString(keyPrefix + cmd);
            tt = guiScaledFontHTML(uiLightViolet()) + title + ": " + tt + "</FONT>";
            tt += "<BR>";
        }
        if (Messages.keyExists(ttKey)) {
            String msg_key = Messages.getString(ttKey);
            tt += guiScaledFontHTML() + msg_key + "</FONT>";
        }
        if (!tt.isEmpty()) {
            String b = "<BODY>" + tt + "</BODY>";
            h = "<HTML>" + b + "</HTML>";
        }
        return h;
    }

    /**
     * Adds buttons to the button panel.  The buttons to be added are retrieved
     * with the <code>getButtonList()</code> method.  The number of buttons to
     * display is defined in <code>buttonsPerGroup</code> and which group of
     * buttons will be displayed is set by <code>currentButtonGroup</code>.
     */
    public void setupButtonPanel() {
        panButtons.removeAll();
        
        var buttonsPanel = new JPanel();
        buttonsPanel.setOpaque(false);
        buttonsPanel.setLayout(new GridLayout(BUTTON_ROWS, buttonsPerRow));
        List<MegamekButton> buttonList = getButtonList();
        
        // Unless it's the first group of buttons, skip any button group if all of its buttons are disabled
        if (currentButtonGroup > 0) {
            int index = currentButtonGroup * buttonsPerGroup;
            while (index < buttonList.size()) {
                if (buttonList.get(index) != null && buttonList.get(index).isEnabled()) {
                    currentButtonGroup = index / buttonsPerGroup;
                    break;
                }
                index++;
            }
            if (index >= buttonList.size()) {
                // Reached the end of the button list without finding an enabled button: Show the first button group
                currentButtonGroup = 0;
            }
        }
        
        int startIndex = currentButtonGroup * buttonsPerGroup;
        int endIndex = startIndex + buttonsPerGroup - 1;
        for (int index = startIndex; index <= endIndex; index++) {
            if ((index < buttonList.size()) && buttonList.get(index) != null) {
                MegamekButton button = buttonList.get(index);
                button.setPreferredSize(MIN_BUTTON_SIZE);
                buttonsPanel.add(button);
                ToolTipManager.sharedInstance().registerComponent(button);
            } else {
                buttonsPanel.add(Box.createHorizontalGlue());
            }
        }         
           
        var donePanel = new UIUtil.FixedXPanel();
        donePanel.setOpaque(false);
        donePanel.add(butDone);
        butDone.setPreferredSize(new Dimension(DONE_BUTTON_WIDTH, MIN_BUTTON_SIZE.height * 2));
                
        panButtons.add(buttonsPanel);
        panButtons.add(donePanel);
        adaptToGUIScale();
        panButtons.validate();
        panButtons.repaint();
    }
    
    /** Clears the actions of this phase. */
    public abstract void clear();
    
    /** Sets up the status bar. It usually displays info on the current phase and if it's the local player's turn. */
    protected void setupStatusBar(String statusInfo) {
        SkinSpecification pdSkinSpec = SkinXMLHandler.getSkin(SkinSpecification.UIComponents.PhaseDisplay.getComp());
        labStatus = new JLabel(statusInfo, SwingConstants.CENTER);
        labStatus.setForeground(pdSkinSpec.fontColors.get(0));
        panStatus.add(labStatus);
    }

    protected void setStatusBarText(String text) {
        labStatus.setText(text);
    }

    private void adaptToGUIScale() {
        UIUtil.adjustContainer(panButtons, UIUtil.FONT_SCALE1);
        UIUtil.adjustContainer(panStatus, UIUtil.FONT_SCALE2);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.ADVANCED_BUTTONS_PER_ROW)) {
            buttonsPerRow = GUIP.getInt(GUIPreferences.ADVANCED_BUTTONS_PER_ROW);
            buttonsPerGroup = 2 * buttonsPerRow;
            setupButtonPanel();
        } else if (e.getName().equals(GUIPreferences.GUI_SCALE)) {
            adaptToGUIScale();
        } else if (e.getName().equals(KeyBindParser.KEYBINDS_CHANGED)) {
            setButtonsTooltips();
        }
    }

    @Override
    public void keyPressed(KeyEvent evt) { }

    @Override
    public void keyReleased(KeyEvent evt) { }

    @Override
    public void keyTyped(KeyEvent evt) { }

    @Override
    public void actionPerformed(ActionEvent e) { }

    @Override
    public void mouseClicked(MouseEvent e) { }

    @Override
    public void mousePressed(MouseEvent e) { }

    @Override
    public void mouseReleased(MouseEvent e) { }

    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) { }

    public void startTimer() {
        // check if there should be a turn timer running
        tt = TurnTimer.init(this, clientgui.getClient());
    }

    public void stopTimer() {
        //get rid of still running timer, if turn is concluded before time is up
        if (tt != null) {
            tt.stopTimer();
            tt = null;
        }
    }

    public String getRemainingPlayerWithTurns() {
        String s = "";
        int r = GUIP.getAdvancedPlayersRemainingToShow();
        if (r > 0) {
            String m = "";
            int gti = clientgui.getClient().getGame().getTurnIndex();
            List<GameTurn> gtv = clientgui.getClient().getGame().getTurnVector();
            int j = 0;
            for (int i = gti + 1; i < gtv.size(); i++) {
                GameTurn nt = gtv.get(i);
                Player p = clientgui.getClient().getGame().getPlayer(nt.getPlayerNum());
                s += p.getName() + ", ";
                j++;
                if (j >= r) {
                    if (gtv.size() > r) {
                        m = ",...";
                    }
                    break;
                }
            }
            if (!s.isEmpty()) {
                String msg_turns = Messages.getString("StatusBarPhaseDisplay.nextPlayerTurns");
                s = "  " + msg_turns + " [" + s.substring(0, s.length() - 2) + m + "]";
            }
        }
        return s;
    }

    public void setStatusBarWithNotDonePlayers() {
        GamePhase phase = clientgui.getClient().getGame().getPhase();
        if (phase.isReport()) {
            int r = GUIP.getAdvancedPlayersRemainingToShow();
            if (r > 0) {
                List<Player> playerList = clientgui.getClient().getGame().getPlayersList().stream().filter(p -> ((!p.isBot()) && (!p.isObserver()) && (!p.isDone()))).collect(Collectors.toList());
                playerList.sort(Comparator.comparingInt(Player::getId));
                String s = "";
                String m = "";
                int j = 0;
                for (Player player : playerList) {
                    s += player.getName() + ", ";
                    j++;
                    if (j >= r) {
                        if (playerList.size() > r) {
                            m = ",...";
                        }
                        break;
                    }
                }
                if (!s.isEmpty()) {
                    String msg_notdone = Messages.getString("StatusBarPhaseDisplay.notDone");
                    s = "  " + msg_notdone + " [" + s.substring(0, s.length() - 2) + m + "]";
                }
                setStatusBarText(phase.toString() + s);
            }
        }
    }
}