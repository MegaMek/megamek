/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.client.ui.widget;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import megamek.client.SBFClient;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.SBFClientGUI;
import megamek.client.ui.util.BASE64ToolKit;
import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.SBFFullGameReport;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.strategicBattleSystems.SBFReportEntry;
import megamek.logging.MMLogger;

/**
 * Shows reports, with an Okay JButton
 */
public class SBFReportPanel extends JPanel implements ActionListener, HyperlinkListener, IPreferenceChangeListener {
    private final static MMLogger LOGGER = MMLogger.create(SBFReportPanel.class);

    private JButton butSwitchLocation;
    private JTabbedPane tabs;
    private JButton butPlayerSearchUp;
    private JButton butPlayerSearchDown;
    private JButton butEntitySearchUp;
    private JButton butEntitySearchDown;
    private JButton butQuickSearchUp;
    private JButton butQuickSearchDown;
    private final JComboBox<String> comboPlayer = new JComboBox<>();
    private final JComboBox<String> comboEntity = new JComboBox<>();
    private final JComboBox<String> comboQuick = new JComboBox<>();
    private SBFClient currentClient;
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final ClientPreferences CP = PreferenceManager.getClientPreferences();

    public SBFReportPanel(SBFClientGUI clientGUI) {

        if (clientGUI == null) {
            return;
        }

        currentClient = clientGUI.getClient();
        //                        updateEntityChoice();
        GameListener gameListener = new GameListenerAdapter() {
            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                if (Objects.requireNonNull(e.getOldPhase()) == GamePhase.VICTORY) {
                    setVisible(false);
                } else {
                    if ((!e.getNewPhase().equals((e.getOldPhase())))
                          && ((e.getNewPhase().isReport()) || ((e.getNewPhase().isOnMap()) && (tabs.getTabCount()
                          == 0)))) {
                        addReportPages(e.getNewPhase());
                        updatePlayerChoice();
                        //                        updateEntityChoice();
                    }
                }
            }
        };
        currentClient.getGame().addGameListener(gameListener);

        butSwitchLocation = new JButton(Messages.getString("MiniReportDisplay.SwitchLocation"));
        butSwitchLocation.addActionListener(this);
        butPlayerSearchUp = new JButton(Messages.getString("MiniReportDisplay.ArrowUp"));
        butPlayerSearchUp.addActionListener(this);
        butPlayerSearchDown = new JButton(Messages.getString("MiniReportDisplay.ArrowDown"));
        butPlayerSearchDown.addActionListener(this);
        butEntitySearchUp = new JButton(Messages.getString("MiniReportDisplay.ArrowUp"));
        butEntitySearchUp.addActionListener(this);
        butEntitySearchDown = new JButton(Messages.getString("MiniReportDisplay.ArrowDown"));
        butEntitySearchDown.addActionListener(this);
        butQuickSearchUp = new JButton(Messages.getString("MiniReportDisplay.ArrowUp"));
        butQuickSearchUp.addActionListener(this);
        butQuickSearchDown = new JButton(Messages.getString("MiniReportDisplay.ArrowDown"));
        butQuickSearchDown.addActionListener(this);

        setLayout(new BorderLayout());

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setBorder(new EmptyBorder(2, 2, 15, 2));
        p.add(comboPlayer);
        p.add(butPlayerSearchUp);
        p.add(butPlayerSearchDown);
        p.add(comboEntity);
        p.add(butEntitySearchUp);
        p.add(butEntitySearchDown);
        p.add(comboQuick);
        p.add(butQuickSearchUp);
        p.add(butQuickSearchDown);
        p.add(butSwitchLocation);

        JScrollPane sp = new JScrollPane(p);
        JPanel panelMain = new JPanel(new BorderLayout());

        tabs = new JTabbedPane();
        panelMain.add(tabs, BorderLayout.CENTER);
        panelMain.add(sp, BorderLayout.SOUTH);
        panelMain.setMinimumSize(new Dimension(0, 0));
        add(panelMain, BorderLayout.CENTER);

        doLayout();

        GUIP.addPreferenceChangeListener(this);
        CP.addPreferenceChangeListener(this);
    }

    private void searchTextPane(String searchPattern, Boolean searchDown) {
        Component selCom = tabs.getSelectedComponent();
        searchPattern = searchPattern.toUpperCase();

        if (selCom instanceof JScrollPane
              && ((JScrollPane) selCom).getViewport().getView() instanceof JComponent) {
            JViewport v = ((JScrollPane) selCom).getViewport();
            for (Component comp : v.getComponents()) {
                if (comp instanceof JTextPane textPane) {
                    Document doc = textPane.getDocument();
                    String text = "";

                    try {
                        text = doc.getText(0, doc.getLength()).toUpperCase();
                    } catch (BadLocationException exception) {
                        LOGGER.error(exception, "SearchText - getText - BadLocationException : {}",
                              exception.getMessage());
                    }

                    int currentPos = textPane.getCaretPosition();

                    if (currentPos > text.length() - searchPattern.length()) {
                        textPane.setCaretPosition(0);
                        currentPos = 0;
                    }

                    int newPos;

                    if (searchDown) {
                        newPos = text.indexOf(searchPattern, currentPos);

                        if (newPos == -1) {
                            newPos = text.indexOf(searchPattern);
                        }

                    } else {
                        newPos = text.lastIndexOf(searchPattern, currentPos - searchPattern.length() - 1);

                        if (newPos == -1) {
                            newPos = text.lastIndexOf(searchPattern, text.length() - searchPattern.length() - 1);
                        }
                    }

                    if (newPos != -1) {
                        try {

                            Rectangle2D rectangle2D = textPane.modelToView2D(newPos);
                            int y = UIUtil.calculateCenter(v.getExtentSize().height,
                                  v.getViewSize().height,
                                  (int) rectangle2D.getHeight(),
                                  (int) rectangle2D.getY());
                            v.setViewPosition(new Point(0, y));
                            textPane.setCaretPosition(newPos);
                            textPane.moveCaretPosition(newPos + searchPattern.length());
                            textPane.getCaret().setSelectionVisible(true);
                        } catch (BadLocationException exception) {
                            LOGGER.error(exception, "Search Text - modelToView - BadLocationException : {}",
                                  exception.getMessage());
                        }
                    }

                    break;
                }
            }
        }
    }

    private void updatePlayerChoice() {
        String name = String.format("%-12s", currentClient.getName());
        String lastChoice = (String) comboPlayer.getSelectedItem();
        lastChoice = (lastChoice != null ? lastChoice : name);
        comboPlayer.removeAllItems();
        comboPlayer.setEnabled(true);
        List<Player> sortedPlayerList = currentClient.getGame().getPlayersList();
        sortedPlayerList.sort(Comparator.comparingInt(Player::getId));
        for (Player player : sortedPlayerList) {
            String playerDisplay = String.format("%-12s", player.getName());
            comboPlayer.addItem(playerDisplay);
        }
        comboPlayer.setSelectedItem(lastChoice);
        if (comboPlayer.getItemCount() <= 1) {
            comboPlayer.setEnabled(false);
        } else if (comboPlayer.getSelectedIndex() < 0) {
            comboPlayer.setSelectedIndex(0);
        }
    }

    private void updateQuickChoice() {
        String lastChoice = (String) comboQuick.getSelectedItem();
        lastChoice = (lastChoice != null) ? lastChoice : Messages.getString("MiniReportDisplay.Damage");
        comboQuick.removeAllItems();
        comboQuick.setEnabled(true);
        String[] keywords = CP.getReportKeywords().split("\n");
        for (String keyword : keywords) {
            comboQuick.addItem(keyword);
        }
        comboQuick.setSelectedItem(lastChoice);
        if (comboQuick.getItemCount() <= 1) {
            comboQuick.setEnabled(false);
        } else if (comboQuick.getSelectedIndex() < 0) {
            comboQuick.setSelectedIndex(0);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            updatePlayerChoice();
            //            updateEntityChoice();
            updateQuickChoice();
        }
        super.setVisible(visible);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        String selectedPlayer = (String) comboPlayer.getSelectedItem();
        String selectedEntity = (String) comboEntity.getSelectedItem();
        String selectedQuick = (String) comboQuick.getSelectedItem();

        if (ae.getSource().equals(butSwitchLocation)) {
            GUIP.toggleMiniReportLocation();
        } else if (ae.getSource().equals(butPlayerSearchDown)) {
            if (selectedPlayer != null) {
                String searchPattern = selectedPlayer.trim();
                searchTextPane(searchPattern, true);
            }
        } else if (ae.getSource().equals(butPlayerSearchUp)) {
            if (selectedPlayer != null) {
                String searchPattern = selectedPlayer.trim();
                searchTextPane(searchPattern, false);
            }
        } else if (ae.getSource().equals(butEntitySearchDown)) {
            if (selectedEntity != null) {
                String searchPattern = selectedEntity.trim();
                searchTextPane(searchPattern, true);
            }
        } else if (ae.getSource().equals(butEntitySearchUp)) {
            if (selectedEntity != null) {
                String searchPattern = selectedEntity.trim();
                searchTextPane(searchPattern, false);
            }
        } else if (ae.getSource().equals(butQuickSearchDown)) {
            if (selectedQuick != null) {
                String searchPattern = selectedQuick.trim();
                searchTextPane(searchPattern, true);
            }
        } else if (ae.getSource().equals(butQuickSearchUp)) {
            if (selectedQuick != null) {
                String searchPattern = selectedQuick.trim();
                searchTextPane(searchPattern, false);
            }
        }
    }

    private JScrollPane loadHtmlScrollPane(String t) {
        JTextPane ta = new JTextPane();
        SBFReportEntry.setupStylesheet(ta);
        ta.addHyperlinkListener(this);
        BASE64ToolKit toolKit = new BASE64ToolKit();
        ta.setEditorKit(toolKit);
        ta.setText("<pre>" + t + "</pre>");
        ta.setEditable(false);
        ta.setOpaque(false);
        ta.setCaretPosition(0);
        return new JScrollPane(ta);
    }

    public void addReportPages(GamePhase phase) {
        int numRounds = currentClient.getGame().getCurrentRound();
        int startIndex = 1;

        // only reload what has changed
        if (numRounds < 2 || phase.isVictory()) {
            tabs.removeAll();
        } else if (tabs.getTabCount() > 1) {
            tabs.removeTabAt(tabs.getTabCount() - 1);
            // don't remove on round change
            if (tabs.getTabCount() == numRounds) {
                tabs.removeTabAt(tabs.getTabCount() - 1);
            }
            startIndex = tabs.getTabCount() + 1;
        }

        SBFGame game = currentClient.getGame();
        SBFFullGameReport report = game.getGameReport();
        for (int round = startIndex; round <= numRounds; round++) {
            if (report.hasReportsForRound(round)) {
                String text =
                      report.get(round).stream().map(SBFReportEntry::text).collect(Collectors.joining());
                tabs.add(Messages.getString("MiniReportDisplay.Round") + " " + round, loadHtmlScrollPane(text));
            }

            //            String text = currentClient.receiveReport(currentClient.getGame().getReports(round));
        }

        // add the new current phase tab
        tabs.add(Messages.getString("MiniReportDisplay.Phase"), loadHtmlScrollPane(currentClient.phaseReport));

        tabs.setSelectedIndex(tabs.getTabCount() - 1);
        tabs.setMinimumSize(new Dimension(0, 0));
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent evt) {
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(ClientPreferences.REPORT_KEYWORDS)) {
            updateQuickChoice();
        }
    }
}
