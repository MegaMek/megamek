/*
 * Copyright (c) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021, 2024 - The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;
import static megamek.client.ui.swing.util.UIUtil.uiLightViolet;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.KeyBindReceiver;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.TurnTimer;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.MegaMekButton;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.common.IGame;
import megamek.common.KeyBindParser;
import megamek.common.Player;
import megamek.common.PlayerTurn;
import megamek.common.annotations.Nullable;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;

/**
 * This is a parent class for the button display for each phase.  Every phase has a panel of control
 * buttons along with a Done button. Each button corresponds to a command that can be carried out in
 * the current phase. This class formats the button panel, the done button, and a status display area.
 * Control buttons are grouped and the groups can be cycled through.
 *
 * @see AbstractPhaseDisplay
 */
public abstract class StatusBarPhaseDisplay extends AbstractPhaseDisplay
        implements ActionListener, IPreferenceChangeListener, KeyBindReceiver {

    protected static final GUIPreferences GUIP = GUIPreferences.getInstance();
    protected static final Dimension MIN_BUTTON_SIZE = new Dimension(32, 32);

    private static final int BUTTON_ROWS = 2;
    private static final String SBPD_KEY_CLEARBUTTON = "clearButton";

    protected final IClientGUI clientgui;

    /**
     * timer that ends turn if time limit set in options is over
     */
    private TurnTimer turnTimer;

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
    public static class CommandComparator implements Comparator<PhaseCommand> {
        @Override
        public int compare(PhaseCommand c1, PhaseCommand c2) {
            return c1.getPriority() - c2.getPriority();
        }
    }

    private JLabel labStatus;
    protected JPanel panStatus = new JPanel();
    protected JPanel panButtons = new JPanel();

    private UIUtil.FixedXPanel donePanel;

    /** The button group that is currently displayed */
    protected int currentButtonGroup = 0;

    /** The number of button groups there are, needs to be computed in a child class. */
    protected int numButtonGroups;

    protected int buttonsPerRow = GUIP.getButtonsPerRow();

    protected int buttonsPerGroup = BUTTON_ROWS * buttonsPerRow;

    protected StatusBarPhaseDisplay(IClientGUI cg) {
        super(cg);
        clientgui = cg;
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), SBPD_KEY_CLEARBUTTON);
        getActionMap().put(SBPD_KEY_CLEARBUTTON, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isIgnoringEvents()) {
                    return;
                }
                if (clientgui.isChatBoxActive()) {
                    clientgui.clearChatBox();
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
        panStatus.setOpaque(false);

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(panButtons);
        add(panStatus);

        GUIP.addPreferenceChangeListener(this);
        KeyBindParser.addPreferenceChangeListener(this);

        MegaMekGUI.getKeyDispatcher().registerCommandAction(KeyCommandBind.EXTEND_TURN_TIMER, this, this::extendTimer);
    }


    /** Returns the list of buttons that should be displayed. */
    protected abstract List<MegaMekButton> getButtonList();

    /** set button that should be displayed. */
    protected abstract void setButtons();

    protected MegaMekButton createButton(String cmd, String keyPrefix) {
        String title = Messages.getString(keyPrefix + cmd);
        MegaMekButton newButton = new MegaMekButton(title, SkinSpecification.UIComponents.PhaseDisplayButton.getComp());
        newButton.addActionListener(this);
        newButton.setActionCommand(cmd);
        newButton.setEnabled(false);
        return newButton;
    }

    /** set button tool tips that should be displayed. */
    protected abstract void setButtonsTooltips();

    protected String createToolTip(String cmd, String keyPrefix, String hotKeyDesc) {
        String result  = "";
        String ttKey = keyPrefix + cmd + ".tooltip";
        String toolTip = hotKeyDesc;
        if (!toolTip.isEmpty()) {
            String title = Messages.getString(keyPrefix + cmd);
            toolTip = guiScaledFontHTML(uiLightViolet()) + title + ": " + toolTip + "</FONT>";
            toolTip += "<BR>";
        }
        if (Messages.keyExists(ttKey)) {
            String msg_key = Messages.getString(ttKey);
            toolTip += guiScaledFontHTML() + msg_key + "</FONT>";
        }
        if (!toolTip.isEmpty()) {
            String b = "<BODY>" + toolTip + "</BODY>";
            result = "<HTML>" + b + "</HTML>";
        }
        return result;
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
        List<MegaMekButton> buttonList = getButtonList();

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
                MegaMekButton button = buttonList.get(index);
                button.setPreferredSize(MIN_BUTTON_SIZE);
                buttonsPanel.add(button);
                ToolTipManager.sharedInstance().registerComponent(button);
            } else {
                buttonsPanel.add(Box.createHorizontalGlue());
            }
        }

        var donePanel = setupDonePanel();

        panButtons.add(buttonsPanel);
        panButtons.add(donePanel);
        adaptToGUIScale();
        panButtons.validate();
        panButtons.repaint();
    }

    protected UIUtil.FixedXPanel setupDonePanel() {
        donePanel = new UIUtil.FixedXPanel();
        donePanel.setPreferredSize(new Dimension(
                UIUtil.scaleForGUI(DONE_BUTTON_WIDTH + 5), MIN_BUTTON_SIZE.height * 2 + 5));
        donePanel.setOpaque(false);
        donePanel.setBackground(Color.DARK_GRAY);
        donePanel.setBorder(new EmptyBorder(0, 10, 0, 0));
        donePanel.setLayout(new GridBagLayout());
        addToDonePanel(donePanel, butDone);
        return donePanel;
    }

    protected void addToDonePanel(JPanel donePanel, JComponent item) {
        item.setPreferredSize(new Dimension(UIUtil.scaleForGUI(DONE_BUTTON_WIDTH), MIN_BUTTON_SIZE.height));
        butDone.setAlignmentX(LEFT_ALIGNMENT);
        donePanel.add(item, GBC.eol().fill(GridBagConstraints.BOTH).weighty(1));
    }

    /**
     * Clears the actions of this phase. Called usually when the ESC key is pressed.
     */
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
        donePanel.setPreferredSize(new Dimension(UIUtil.scaleForGUI(DONE_BUTTON_WIDTH), MIN_BUTTON_SIZE.height));
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.BUTTONS_PER_ROW)) {
            buttonsPerRow = GUIP.getButtonsPerRow();
            buttonsPerGroup = BUTTON_ROWS * buttonsPerRow;
            setupButtonPanel();
        } else if (e.getName().equals(KeyBindParser.KEYBINDS_CHANGED)) {
            setButtonsTooltips();
        }

        adaptToGUIScale();
    }

    @Override
    public boolean shouldReceiveKeyCommands() {
        return clientgui.getClient().isMyTurn()
                && !clientgui.isChatBoxActive()
                && !isIgnoringEvents() && isVisible();
    }

    public void startTimer() {
        turnTimer = TurnTimer.init(this, clientgui.getClient());
    }

    public void stopTimer() {
        if (turnTimer != null) {
            turnTimer.stopTimer();
            turnTimer = null;
        }
    }

    public void extendTimer() {
        if (turnTimer != null) {
            turnTimer.setExtendTimer();
        }
    }

    /**
     * @return True when there is a turn timer and it has expired, false when there was no turn timer or
     * it has not yet expired.
     */
    public boolean isTimerExpired() {
        return (turnTimer != null) && turnTimer.isTimerExpired();
    }

    public String getRemainingPlayerWithTurns() {
        String result = "";
        int playerCountToShow = GUIP.getPlayersRemainingToShow();
        IGame game = clientgui.getClient().getGame();
        List<String> nextPlayerNames = new ArrayList<>();
        int turnIndex = game.getTurnIndex();
        List<? extends PlayerTurn> gameTurns = game.getTurnsList();
        for (int i = turnIndex + 1; (i < gameTurns.size()) && (nextPlayerNames.size() < playerCountToShow); i++) {
            nextPlayerNames.add(game.getPlayer(gameTurns.get(i).playerId()).getName());
        }
        if (!nextPlayerNames.isEmpty()) {
            String playerList = String.join(", ", nextPlayerNames);
            playerList += (gameTurns.size() - turnIndex - 1 > playerCountToShow) ? ", ..." : "";
            String msg_turns = Messages.getString("StatusBarPhaseDisplay.nextPlayerTurns");
            result = "  " + msg_turns + " [" + playerList + "]";
        }
        return result;
    }

    public void setStatusBarWithNotDonePlayers() {
        IGame game = clientgui.getClient().getGame();
        if (game.getPhase().isReport()) {
            int playerCountToShow = GUIP.getPlayersRemainingToShow();
            List<Player> remainingPlayers = game.getPlayersList().stream()
                    .filter(p -> !p.isBot() && !p.isObserver() && !p.isDone())
                    .sorted(Comparator.comparingInt(Player::getId))
                    .collect(Collectors.toList());
            if (!remainingPlayers.isEmpty()) {
                String playersText = remainingPlayers.stream()
                        .limit(playerCountToShow)
                        .map(Player::getName)
                        .collect(Collectors.joining(", "));
                if (remainingPlayers.size() > playerCountToShow) {
                    playersText += ", ...";
                }
                String msg_notdone = Messages.getString("StatusBarPhaseDisplay.notDone");
                setStatusBarText(game.getPhase() + "  " + msg_notdone + " [" + playersText + "]");
            } else {
                setStatusBarText(game.getPhase().toString());
            }
        }
    }

    protected String playerNameOrUnknown(@Nullable Player player) {
        return (player == null) ? "Unknown" : player.getName();
    }
}
