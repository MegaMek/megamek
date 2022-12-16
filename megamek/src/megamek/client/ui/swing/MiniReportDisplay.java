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
package megamek.client.ui.swing;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.BASE64ToolKit;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.Player;
import megamek.common.Entity;
import megamek.common.Report;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.preference.PreferenceManager;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;

/**
 * Shows reports, with an Okay JButton
 */
public class MiniReportDisplay extends JDialog implements ActionListener, HyperlinkListener, IPreferenceChangeListener {
    private JButton butOkay;
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
    private ClientGUI currentClientgui;
    private Client currentClient;

    private static final String MSG_TITLE = Messages.getString("MiniReportDisplay.title");
    private static final String MSG_ROUND = Messages.getString("MiniReportDisplay.Round");
    private static final String MSG_PHASE = Messages.getString("MiniReportDisplay.Phase");
    private static final String MSG_DAMAGE = Messages.getString("MiniReportDisplay.Damage");
    private static final String MSG_ARROWUP = Messages.getString("MiniReportDisplay.ArrowUp");
    private static final String MSG_ARROWDOWN = Messages.getString("MiniReportDisplay.ArrowDown");
    private static final String MSG_DETAILS = Messages.getString("MiniReportDisplay.Details");
    private static final String MSG_OKAY= Messages.getString("Okay");

    private static final int MRD_MAXNAMELENGHT = 60;

    public MiniReportDisplay(JFrame parent, ClientGUI clientgui) {
        super(parent, MSG_TITLE, false);

        if (clientgui == null) {
            return;
        }

        currentClientgui = clientgui;
        currentClient = clientgui.getClient();
        currentClient.getGame().addGameListener(gameListener);

        butOkay = new JButton(MSG_OKAY);
        butOkay.addActionListener(this);
        butPlayerSearchUp = new JButton(MSG_ARROWUP);
        butPlayerSearchUp.addActionListener(this);
        butPlayerSearchDown = new JButton(MSG_ARROWDOWN);
        butPlayerSearchDown.addActionListener(this);
        butEntitySearchUp = new JButton(MSG_ARROWUP);
        butEntitySearchUp.addActionListener(this);
        butEntitySearchDown = new JButton(MSG_ARROWDOWN);
        butEntitySearchDown.addActionListener(this);
        butQuickSearchUp = new JButton(MSG_ARROWUP);
        butQuickSearchUp.addActionListener(this);
        butQuickSearchDown = new JButton(MSG_ARROWDOWN);
        butQuickSearchDown.addActionListener(this);

        setLayout(new BorderLayout());

        JPanel p = new JPanel();
        p.add(BorderLayout.EAST, comboPlayer);
        p.add(BorderLayout.EAST, butPlayerSearchUp);
        p.add(BorderLayout.EAST, butPlayerSearchDown);
        p.add(BorderLayout.EAST, comboEntity);
        p.add(BorderLayout.EAST, butEntitySearchUp);
        p.add(BorderLayout.EAST, butEntitySearchDown);
        p.add(BorderLayout.EAST, comboQuick);
        p.add(BorderLayout.EAST, butQuickSearchUp);
        p.add(BorderLayout.EAST, butQuickSearchDown);

        p.add(BorderLayout.WEST, butOkay);
        JScrollPane sp = new JScrollPane(p);
        add(BorderLayout.SOUTH, sp);
        
        setupReportTabs();
                
        setSize(GUIPreferences.getInstance().getMiniReportSizeWidth(),
                GUIPreferences.getInstance().getMiniReportSizeHeight());
        doLayout();
        setLocation(GUIPreferences.getInstance().getMiniReportPosX(),
                GUIPreferences.getInstance().getMiniReportPosY());

        // closing the window is the same as hitting butOkay
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                actionPerformed(new ActionEvent(butOkay,
                        ActionEvent.ACTION_PERFORMED, butOkay.getText()));
            }
        });

        adaptToGUIScale();
        GUIPreferences.getInstance().addPreferenceChangeListener(this);
        PreferenceManager.getClientPreferences().addPreferenceChangeListener(this);
        butOkay.requestFocus();
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
                            Rectangle r = (Rectangle) textPane.modelToView2D(newPos);
                            int y = UIUtil.calculateCenter(v.getExtentSize().height, v.getViewSize().height, r.height, r.y);
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
        for (Player player  : currentClient.getGame().getPlayersVectorSorted()) {
            String playerDisplay = String.format("%-12s", player.getName());
            comboPlayer.addItem(playerDisplay);
        }
        if (comboPlayer.getItemCount() == 1) {
            comboPlayer.setEnabled(false);
        }
        comboPlayer.setSelectedItem(lastChoice);
        if (comboPlayer.getSelectedIndex() < 0) {
            comboPlayer.setSelectedIndex(0);
        }
    }

    private String addEntity(JComboBox comboBox, String name) {
        boolean found = false;
        int len = (name.length() < MRD_MAXNAMELENGHT ? name.length() : MRD_MAXNAMELENGHT);
        String displayNane = String.format("%-12s", name).substring(0, len);
        found = false;
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (comboBox.getItemAt(i).equals(displayNane)) {
                found = true;
                break;
            }
        }
        if (!found) {
            comboBox.addItem(displayNane);
        }
        return displayNane;
    }

    private void updateEntityChoice() {
        String lastChoice = (String) comboEntity.getSelectedItem();
        comboEntity.removeAllItems();
        comboEntity.setEnabled(true);
        String displayNane = "";
        for (Iterator<Entity> ents = currentClient.getGame().getEntities(); ents.hasNext();) {
            Entity entity = ents.next();
            if (entity.getOwner().equals(currentClient.getLocalPlayer())) {
                displayNane = addEntity(comboEntity, entity.getShortName());
            }
        }
        lastChoice = (lastChoice != null ? lastChoice : displayNane);
        comboEntity.setSelectedItem(lastChoice);
        if (comboEntity.getItemCount() <= 1) {
            comboEntity.setEnabled(false);
        } else {
            if (comboEntity.getSelectedIndex() < 0) {
                comboEntity.setSelectedIndex(0);
            }
        }
    }

    private void updateQuickChoice() {
        String lastChoice = (String) comboQuick.getSelectedItem();
        lastChoice = (lastChoice != null ? lastChoice : MSG_DAMAGE);
        comboQuick.removeAllItems();
        comboQuick.setEnabled(true);
        String[] keywords =  PreferenceManager.getClientPreferences().getReportKeywords().split("\n");
        for (String keyword : keywords) {
            comboQuick.addItem(keyword);
        }
        comboQuick.setSelectedItem(lastChoice);
        if (comboQuick.getItemCount() <= 1) {
            comboQuick.setEnabled(false);
        } else {
            if (comboQuick.getSelectedIndex() < 0) {
                comboQuick.setSelectedIndex(0);
            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            updatePlayerChoice();
            updateEntityChoice();
            updateQuickChoice();
        }
        super.setVisible(visible);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(butOkay)) {
            savePref();
            setVisible(false);
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

    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if ((e.getID() == WindowEvent.WINDOW_DEACTIVATED) || (e.getID() == WindowEvent.WINDOW_CLOSING)) {
            savePref();
        }
    }


    private void setupReportTabs() {
        tabs = new JTabbedPane();

        addReportPages();
        
        add(BorderLayout.CENTER, tabs);
    }

    private void savePref() {
        GUIPreferences.getInstance().setMiniReportSizeWidth(getSize().width);
        GUIPreferences.getInstance().setMiniReportSizeHeight(getSize().height);
        GUIPreferences.getInstance().setMiniReportPosX(getLocation().x);
        GUIPreferences.getInstance().setMiniReportPosY(getLocation().y);
    }

    public void addReportPages() {
        int numRounds = currentClient.getGame().getRoundCount();
        tabs.removeAll();

        for (int round = 1; round <= numRounds; round++) {
            String text = currentClient.receiveReport(currentClient.getGame().getReports(round));
            JTextPane ta = new JTextPane();
            ReportDisplay.setupStylesheet(ta);
            ta.addHyperlinkListener(this);
            BASE64ToolKit toolKit = new BASE64ToolKit();
            ta.setEditorKit(toolKit);
            ta.setText("<pre>" + text + "</pre>");
            ta.setEditable(false);
            ta.setOpaque(false);
            ta.setCaretPosition(0);
            JScrollPane sp = new JScrollPane(ta);
            tabs.add(MSG_ROUND + " " + round, sp);
        }

        // add the new current phase tab
        JTextPane ta = new JTextPane();
        ReportDisplay.setupStylesheet(ta);
        ta.addHyperlinkListener(this);

        BASE64ToolKit toolKit = new BASE64ToolKit();
        ta.setEditorKit(toolKit);
        ta.setText("<pre>" + currentClient.phaseReport + "</pre>");
        ta.setEditable(false);
        ta.setOpaque(false);
        ta.setCaretPosition(0);

        JScrollPane sp = new JScrollPane(ta);
        tabs.add(MSG_PHASE, sp);

        tabs.setSelectedIndex(tabs.getTabCount() - 1);
    }

    private JComponent activePane() {
        return (JComponent) ((JScrollPane) tabs.getSelectedComponent()).getViewport().getView();
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent evt) {
        String evtDesc = evt.getDescription();
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (evtDesc.startsWith(Report.ENTITY_LINK)) {
                String idString = evtDesc.substring(Report.ENTITY_LINK.length());
                int id;
                try {
                    id = Integer.parseInt(idString);
                } catch (Exception ex) {
                    id = -1;
                }
                Entity ent = currentClientgui.getClient().getGame().getEntity(id);
                if (ent != null) {
                    currentClientgui.getUnitDisplay().displayEntity(ent);
                    currentClientgui.setUnitDisplayVisible(true);
                }
            } else if (evtDesc.startsWith(Report.TOOLTIP_LINK)) {
                String desc = evtDesc.substring(Report.TOOLTIP_LINK.length());
                JOptionPane.showMessageDialog(currentClientgui, desc, MSG_DETAILS,
                        JOptionPane.PLAIN_MESSAGE);
            }
        } else if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            if (evtDesc.startsWith(Report.TOOLTIP_LINK)) {
                String desc = evtDesc.substring(Report.TOOLTIP_LINK.length());
                activePane().setToolTipText(desc);
            }
        } else if (evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
            activePane().setToolTipText(null);
        }
    }

    private GameListener gameListener = new GameListenerAdapter() {
        @Override
        public void gamePhaseChange(GamePhaseChangeEvent e) {
            switch (e.getOldPhase()) {
                case VICTORY:
                    savePref();
                    setVisible(false);
                    break;
                default:
                    if (!e.getNewPhase().equals((e.getOldPhase()))) {
                        addReportPages();
                        updatePlayerChoice();
                        updateEntityChoice();
                    }
            }
        }
    };

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);

        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component cp = tabs.getComponentAt(i);
            if (cp instanceof JScrollPane) {
                Component pane = ((JScrollPane) cp).getViewport().getView();
                if (pane instanceof JTextPane) {
                    JTextPane tp = (JTextPane) pane;
                    ReportDisplay.setupStylesheet(tp);
                    tp.setText(tp.getText());
                }
            }
        }
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        // Update the text size when the GUI scaling changes
        if (e.getName().equals(GUIPreferences.GUI_SCALE)) {
            adaptToGUIScale();
        } else if (e.getName().equals(ClientPreferences.REPORT_KEYWORDS)) {
            updateQuickChoice();
        }
    }
}
