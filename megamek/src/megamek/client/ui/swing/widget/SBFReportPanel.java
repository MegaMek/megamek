/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
package megamek.client.ui.swing.widget;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;
import java.util.List;
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
import javax.swing.text.Document;

import megamek.client.SBFClient;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.SBFClientGUI;
import megamek.client.ui.swing.util.BASE64ToolKit;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Player;
import megamek.common.Report;
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

/**
 * Shows reports, with an Okay JButton
 */
public class SBFReportPanel extends JPanel implements ActionListener, HyperlinkListener, IPreferenceChangeListener {
    private JButton butSwitchLocation;
    private JTabbedPane tabs;
    private JButton butPlayerSearchUp;
    private JButton butPlayerSearchDown;
    private JButton butEntitySearchUp;
    private JButton butEntitySearchDown;
    private JButton butQuickSearchUp;
    private JButton butQuickSearchDown;
    private JComboBox<String> comboPlayer = new JComboBox<>();
    private JComboBox<String> comboEntity = new JComboBox<>();
    private JComboBox<String> comboQuick = new JComboBox<>();
    private SBFClientGUI currentClientgui;
    private SBFClient currentClient;
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final ClientPreferences CP =  PreferenceManager.getClientPreferences();

    private static final int MRD_MAXNAMELENGHT = 60;

    public SBFReportPanel(SBFClientGUI clientgui) {

        if (clientgui == null) {
            return;
        }

        currentClientgui = clientgui;
        currentClient = clientgui.getClient();
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
        p.setBorder(new EmptyBorder(2,2,15,2));
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
                if (comp instanceof JTextPane) {
                    try {
                        JTextPane textPane = (JTextPane) comp;
                        Document doc = textPane.getDocument();
                        String text = doc.getText(0, doc.getLength()).toUpperCase();
                        int currentPos = textPane.getCaretPosition();

                        if (currentPos > text.length() - searchPattern.length()) {
                            textPane.setCaretPosition(0);
                            currentPos = 0;
                        }

                        int newPos = -1;

                        if (searchDown){
                            newPos = text.indexOf(searchPattern, currentPos);

                            if (newPos == -1) {
                                newPos = text.indexOf(searchPattern, 0);
                            }

                        }
                        else {
                            newPos = text.lastIndexOf(searchPattern, currentPos-searchPattern.length()-1);

                            if (newPos == -1) {
                                newPos = text.lastIndexOf(searchPattern, text.length()-searchPattern.length()-1);
                            }
                        }

                        if (newPos != -1) {
                            Rectangle2D r = textPane.modelToView2D(newPos);
                            int y = UIUtil.calculateCenter(v.getExtentSize().height, v.getViewSize().height, (int) r.getHeight(), (int) r.getY());
                            v.setViewPosition(new Point(0,y));
                            textPane.setCaretPosition(newPos);
                            textPane.moveCaretPosition(newPos + searchPattern.length());
                            textPane.getCaret().setSelectionVisible(true);
                        }
                    } catch (Exception e) {
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

    private String addEntity(JComboBox<String> comboBox, String name) {
        boolean found = false;
        int len = (name.length() < MRD_MAXNAMELENGHT ? name.length() : MRD_MAXNAMELENGHT);
        String displayName = String.format("%-12s", name).substring(0, len);
        found = false;
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (comboBox.getItemAt(i).equals(displayName)) {
                found = true;
                break;
            }
        }
        if (!found) {
            comboBox.addItem(displayName);
        }
        return displayName;
    }

//    private void updateEntityChoice() {
//        String lastChoice = (String) comboEntity.getSelectedItem();
//        comboEntity.removeAllItems();
//        comboEntity.setEnabled(true);
//        String displayNane = "";
//        for (Iterator<Entity> ents = currentClient.getGame().getEntities(); ents.hasNext();) {
//            Entity entity = ents.next();
//            if (entity.getOwner().equals(currentClient.getLocalPlayer())) {
//                displayNane = addEntity(comboEntity, entity.getShortName());
//            }
//        }
//        lastChoice = (lastChoice != null ? lastChoice : displayNane);
//        comboEntity.setSelectedItem(lastChoice);
//        if (comboEntity.getItemCount() <= 1) {
//            comboEntity.setEnabled(false);
//        } else if (comboEntity.getSelectedIndex() < 0) {
//            comboEntity.setSelectedIndex(0);
//        }
//    }

    private void updateQuickChoice() {
        String lastChoice = (String) comboQuick.getSelectedItem();
        lastChoice = (lastChoice != null) ? lastChoice : Messages.getString("MiniReportDisplay.Damage");
        comboQuick.removeAllItems();
        comboQuick.setEnabled(true);
        String[] keywords =  CP.getReportKeywords().split("\n");
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
        if (ae.getSource().equals(butSwitchLocation)) {
            GUIP.toggleMiniReportLocation();
        } else if (ae.getSource().equals(butPlayerSearchDown)) {
            String searchPattern = comboPlayer.getSelectedItem().toString().trim();
            searchTextPane(searchPattern, true);
        } else if (ae.getSource().equals(butPlayerSearchUp)) {
            String searchPattern = comboPlayer.getSelectedItem().toString().trim();
            searchTextPane(searchPattern, false);
        } else if (ae.getSource().equals(butEntitySearchDown)) {
            String searchPattern = comboEntity.getSelectedItem().toString().trim();
            searchTextPane(searchPattern, true);
        } else if (ae.getSource().equals(butEntitySearchUp)) {
            String searchPattern = comboEntity.getSelectedItem().toString().trim();
            searchTextPane(searchPattern,false);
        } else if (ae.getSource().equals(butQuickSearchDown)) {
            String searchPattern = comboQuick.getSelectedItem().toString().trim();
            searchTextPane(searchPattern, true);
        } else if (ae.getSource().equals(butQuickSearchUp)) {
            String searchPattern = comboQuick.getSelectedItem().toString().trim();
            searchTextPane(searchPattern,false);
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
            if (report.hasReportsforRound(round)) {
            String text=
                report.get(round).stream().map(r -> r.text()).collect(Collectors.joining());
            tabs.add(Messages.getString("MiniReportDisplay.Round") + " " + round, loadHtmlScrollPane(text));
            }

//            String text = currentClient.receiveReport(currentClient.getGame().getReports(round));
        }

        // add the new current phase tab
        tabs.add(Messages.getString("MiniReportDisplay.Phase"), loadHtmlScrollPane(currentClient.phaseReport));

        tabs.setSelectedIndex(tabs.getTabCount() - 1);
        tabs.setMinimumSize(new Dimension(0, 0));
    }

    private JComponent activePane() {
        return (JComponent) ((JScrollPane) tabs.getSelectedComponent()).getViewport().getView();
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent evt) {
//        String evtDesc = evt.getDescription();
//        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
//            if (evtDesc.startsWith(Report.ENTITY_LINK)) {
//                String idString = evtDesc.substring(Report.ENTITY_LINK.length());
//                int id;
//                try {
//                    id = Integer.parseInt(idString);
//                } catch (Exception ex) {
//                    id = -1;
//                }
//                Entity ent = currentClientgui.getClient().getGame().getEntity(id);
//                if (ent != null) {
//                    currentClientgui.getUnitDisplay().displayEntity(ent);
//                    GUIP.setUnitDisplayEnabled(true);
//                    if (ent.isDeployed() && !ent.isOffBoard() && ent.getPosition() != null) {
//                        currentClientgui.getBoardView().centerOnHex(ent.getPosition());
//                    }
//                }
//            } else if (evtDesc.startsWith(Report.TOOLTIP_LINK)) {
//                String desc = evtDesc.substring(Report.TOOLTIP_LINK.length());
//                JOptionPane.showMessageDialog(currentClientgui.getFrame(), desc,
//                        Messages.getString("MiniReportDisplay.Details"), JOptionPane.PLAIN_MESSAGE);
//            }
//        } else if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
//            if (evtDesc.startsWith(Report.TOOLTIP_LINK)) {
//                String desc = evtDesc.substring(Report.TOOLTIP_LINK.length());
//                activePane().setToolTipText(desc);
//            }
//        } else if (evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
//            activePane().setToolTipText(null);
//        }
    }

    private GameListener gameListener = new GameListenerAdapter() {
        @Override
        public void gamePhaseChange(GamePhaseChangeEvent e) {
            switch (e.getOldPhase()) {
                case VICTORY:
                    setVisible(false);
                    break;
                default:
                    if ((!e.getNewPhase().equals((e.getOldPhase())))
                            && ((e.getNewPhase().isReport()) || ((e.getNewPhase().isOnMap()) && (tabs.getTabCount() == 0)))){
                        addReportPages(e.getNewPhase());
                        updatePlayerChoice();
//                        updateEntityChoice();
                    }
            }
        }
    };

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(ClientPreferences.REPORT_KEYWORDS)) {
            updateQuickChoice();
        }
    }
}
