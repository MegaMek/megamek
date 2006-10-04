/*
 * MegaMek - 
 *  Copyright (C) 2000,2001,2002,2003,2004,2005,2006 Ben Mazur (bmazur@sev.org)
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

import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.TestBot;
import megamek.client.bot.ui.swing.BotGUI;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.IGame;
import megamek.common.IStartingPositions;
import megamek.common.Infantry;
import megamek.common.MechSummaryCache;
import megamek.common.Player;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.event.GameEntityNewEvent;
import megamek.common.event.GameEntityRemoveEvent;
import megamek.common.event.GameListener;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GamePlayerChangeEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableAdapter;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

public class ChatLounge
        extends AbstractPhaseDisplay
        implements ActionListener, ItemListener, BoardViewListener, GameListener, DoneButtoned, Distractable, ListSelectionListener {
    // Distraction implementation.
    private DistractableAdapter distracted = new DistractableAdapter();

    // parent Client
    private Client client;
    private ClientGUI clientgui;

    // The camo selection dialog.
    private CamoChoiceDialog camoDialog;

    // buttons & such
    private JPanel panPlayerInfo;
    private JLabel labPlayerInfo;
    private JList lisPlayerInfo;

    private JLabel labTeam;
    private JComboBox choTeam;

    private JLabel labCamo;
    private JButton butCamo;

    private JPanel panMinefield;
    private JLabel labMinefield;
    private JList lisMinefield;
    private JLabel labConventional;
    private JLabel labCommandDetonated;
    private JLabel labVibrabomb;
    private JTextField fldConventional;
    private JTextField fldCommandDetonated;
    private JTextField fldVibrabomb;
    private JButton butMinefield;

    private JButton butOptions;

    private JLabel labBoardSize;
    private JLabel labMapSize;
    private JList lisBoardsSelected;
    private JButton butChangeBoard;
    private JPanel panBoardSettings;

    private JButton butLoadList;
    //      private JLabel  lblPlaceholder;
    private JButton butSaveList;
    private JButton butDeleteAll;

    private JButton butLoad;
    private JButton butLoadCustomBA;
    private JButton butDelete;
    private JButton butCustom;
    private JButton butMechReadout;
    private JButton butViewGroup;
    private JList lisEntities;
    private int[] entityCorrespondance;
    private JPanel panEntities;

    private JLabel labStarts;
    private JList lisStarts;
    private JPanel panStarts;
    private JButton butChangeStart;

    private JLabel labBVs;
    private JList lisBVs;
    private ButtonGroup bvCbg;
    private JRadioButton chkBV;
    private JRadioButton chkTons;
    private JRadioButton chkCost;

    private JPanel panBVs;

    private JTabbedPane panTabs;
    private JPanel panMain;

    private JPanel panUnits;
    private JPanel panTop;

    private JLabel labStatus;
    private JButton butDone;

    private JButton butAddBot;
    private JButton butRemoveBot;

    private MechSummaryCache.Listener mechSummaryCacheListener = new MechSummaryCache.Listener() {
        public void doneLoading() {
            butLoad.setEnabled(true);
            butLoadCustomBA.setEnabled(true);
        }
    };

    /**
     * Creates a new chat lounge for the client.
     */
    public ChatLounge(ClientGUI clientgui) {
        client = clientgui.getClient();
        this.clientgui = clientgui;

        // Create a tabbed panel to hold our components.
        panTabs = new JTabbedPane();
        Font tabPanelFont = new Font("Helvetica", Font.BOLD, //$NON-NLS-1$
                GUIPreferences.getInstance().getInt("AdvancedChatLoungeTabFontSize"));
        panTabs.setFont(tabPanelFont);
        
        // Create a new camo selection dialog.
        camoDialog = new CamoChoiceDialog(clientgui.getFrame());

        client.game.addGameListener(this);
        clientgui.getBoardView().addBoardViewListener(this);

        butOptions = new JButton(Messages.getString("ChatLounge.butOptions")); //$NON-NLS-1$
        butOptions.addActionListener(this);

        butDone = new JButton(Messages.getString("ChatLounge.butDone")); //$NON-NLS-1$
        Font font = null;
        try {
            font = new Font("sanserif", Font.BOLD, 12); //$NON-NLS-1$
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        if (font == null) {
            System.err.println("Couldn't find the new font for the 'Done' button."); //$NON-NLS-1$
        } else {
            butDone.setFont(font);
        }
        butDone.setActionCommand("ready"); //$NON-NLS-1$
        butDone.addActionListener(this);

        setupPlayerInfo();
        setupMinefield();

        setupBoardSettings();
        refreshGameSettings();

        setupEntities();
        refreshEntities();

        setupBVs();
        refreshBVs();

        setupStarts();
        refreshStarts();

        setupMainPanel();

        labStatus = new JLabel("", JLabel.CENTER); //$NON-NLS-1$

        // layout main thing
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridwidth = GridBagConstraints.REMAINDER;
        if (GUIPreferences.getInstance().getChatLoungeTabs()) {
            addBag(panTabs, gridbag, c);
        } else {
            addBag(panMain, gridbag, c);
        }

        //         c.weightx = 1.0;    c.weighty = 0.0;
        //         addBag(labStatus, gridbag, c);

        //          c.gridwidth = 1;
        //          c.weightx = 1.0;    c.weighty = 0.0;
        //          addBag(client.cb.getComponent(), gridbag, c);

        //          c.gridwidth = 1;
        //          c.anchor = GridBagConstraints.EAST;
        //          c.weightx = 0.0;    c.weighty = 0.0;
        //          c.ipady = 10;
        //          addBag(butDone, gridbag, c);
        validate();
    }

    private void addBag(JComponent comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
    }

    /**
     * Sets up the player info (team, camo) panel
     */
    private void setupPlayerInfo() {
        Player player = client.getLocalPlayer();

        panPlayerInfo = new JPanel();

        labPlayerInfo = new JLabel(Messages.getString("ChatLounge.labPlayerInfo")); //$NON-NLS-1$

        lisPlayerInfo = new JList(new DefaultListModel());
        lisPlayerInfo.addListSelectionListener(this);

        butAddBot = new JButton(Messages.getString("ChatLounge.butAddBot")); //$NON-NLS-1$
        butAddBot.setActionCommand("add_bot"); //$NON-NLS-1$
        butAddBot.addActionListener(this);

        butRemoveBot = new JButton(Messages.getString("ChatLounge.butRemoveBot")); //$NON-NLS-1$
        butRemoveBot.setEnabled(false);
        butRemoveBot.setActionCommand("remove_bot"); //$NON-NLS-1$
        butRemoveBot.addActionListener(this);

        labTeam = new JLabel(Messages.getString("ChatLounge.labTeam"), JLabel.RIGHT); //$NON-NLS-1$
        labCamo = new JLabel(Messages.getString("ChatLounge.labCamo"), JLabel.RIGHT); //$NON-NLS-1$

        choTeam = new JComboBox();
        choTeam.addItemListener(this);
        setupTeams();

        butCamo = new JButton();
        butCamo.setText(Messages.getString("ChatLounge.noCamo")); //$NON-NLS-1$
        butCamo.setPreferredSize(new Dimension(84, 72));
        butCamo.setActionCommand("camo"); //$NON-NLS-1$
        butCamo.addActionListener(this);
        camoDialog.addItemListener(new CamoChoiceListener(camoDialog, butCamo, butOptions.getBackground(), player.getId(), client));
        refreshCamos();

        // If we have a camo pattern, use it.  Otherwise set a background.
        Image[] images = (Image[]) camoDialog.getSelectedObjects();
        if (images != null) {
            butCamo.setIcon(new ImageIcon(images[0]));
        } else {
            butCamo.setBackground(PlayerColors.getColor(player.getColorIndex()));
        }

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panPlayerInfo.setLayout(gridbag);

        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labPlayerInfo, c);
        panPlayerInfo.add(labPlayerInfo);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        gridbag.setConstraints(lisPlayerInfo, c);
        panPlayerInfo.add(lisPlayerInfo);

        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(labTeam, c);
        panPlayerInfo.add(labTeam);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(choTeam, c);
        panPlayerInfo.add(choTeam);

        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(labCamo, c);
        panPlayerInfo.add(labCamo);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(butCamo, c);
        panPlayerInfo.add(butCamo);

        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(butAddBot, c);
        panPlayerInfo.add(butAddBot);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(butRemoveBot, c);
        panPlayerInfo.add(butRemoveBot);

        refreshPlayerInfo();
    }

    /**
     * Sets up the minefield panel
     */
    private void setupMinefield() {
        panMinefield = new JPanel();

        labMinefield = new JLabel(Messages.getString("ChatLounge.labMinefield")); //$NON-NLS-1$

        lisMinefield = new JList(new DefaultListModel());

        labConventional = new JLabel(Messages.getString("ChatLounge.labConventional"), JLabel.RIGHT); //$NON-NLS-1$
        labCommandDetonated = new JLabel(Messages.getString("ChatLounge.labCommandDetonated"), JLabel.RIGHT); //$NON-NLS-1$
        labVibrabomb = new JLabel(Messages.getString("ChatLounge.labVibrabomb"), JLabel.RIGHT); //$NON-NLS-1$

        fldConventional = new JTextField(1);
        fldCommandDetonated = new JTextField(1);
        fldVibrabomb = new JTextField(1);

        butMinefield = new JButton(Messages.getString("ChatLounge.butMinefield")); //$NON-NLS-1$
        butMinefield.addActionListener(this);

        enableMinefields(client.game.getOptions().booleanOption("minefields")); //$NON-NLS-1$

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panMinefield.setLayout(gridbag);

        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labMinefield, c);
        panMinefield.add(labMinefield);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        gridbag.setConstraints(lisMinefield, c);
        panMinefield.add(lisMinefield);

        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(labConventional, c);
        panMinefield.add(labConventional);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(fldConventional, c);
        panMinefield.add(fldConventional);

        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(labCommandDetonated, c);
        panMinefield.add(labCommandDetonated);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(fldCommandDetonated, c);
        panMinefield.add(fldCommandDetonated);

        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(labVibrabomb, c);
        panMinefield.add(labVibrabomb);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(fldVibrabomb, c);
        panMinefield.add(fldVibrabomb);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(butMinefield, c);
        panMinefield.add(butMinefield);

        refreshMinefield();
    }

    public void enableMinefields(boolean enable) {
        fldConventional.setEnabled(enable);
        labConventional.setEnabled(enable);

        fldCommandDetonated.setEnabled(false);
        labCommandDetonated.setEnabled(false);

        fldVibrabomb.setEnabled(enable);
        labVibrabomb.setEnabled(enable);

        butMinefield.setEnabled(enable);
    }

    /**
     * Sets up the board settings panel
     */
    private void setupBoardSettings() {
        labBoardSize = new JLabel(Messages.getString("ChatLounge.labBoardSize"), JLabel.CENTER); //$NON-NLS-1$
        labMapSize = new JLabel(Messages.getString("ChatLounge.labMapSize"), JLabel.CENTER); //$NON-NLS-1$

        lisBoardsSelected = new JList(new DefaultListModel());
        lisBoardsSelected.addListSelectionListener(this);

        butChangeBoard = new JButton(Messages.getString("ChatLounge.butChangeBoard")); //$NON-NLS-1$
        butChangeBoard.setActionCommand("change_board"); //$NON-NLS-1$
        butChangeBoard.addActionListener(this);

        panBoardSettings = new JPanel();

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panBoardSettings.setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labBoardSize, c);
        panBoardSettings.add(labBoardSize);

        gridbag.setConstraints(labMapSize, c);
        panBoardSettings.add(labMapSize);

        c.weightx = 1.0;
        c.weighty = 1.0;
        gridbag.setConstraints(lisBoardsSelected, c);
        panBoardSettings.add(lisBoardsSelected);

        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(butChangeBoard, c);
        panBoardSettings.add(butChangeBoard);

        refreshBoardSettings();
    }

    private void refreshBoardSettings() {
        labBoardSize.setText(Messages.getString("ChatLounge.BoardSize", //$NON-NLS-1$
                new Object[]{new Integer(client.getMapSettings().getBoardWidth()),
                             new Integer(client.getMapSettings().getBoardHeight())}));
        labMapSize.setText(Messages.getString("ChatLounge.MapSize", //$NON-NLS-1$
                new Object[]{new Integer(client.getMapSettings().getMapWidth()),
                             new Integer(client.getMapSettings().getMapHeight())}));

        ((DefaultListModel) lisBoardsSelected.getModel()).removeAllElements();
        int index = 0;
        for (Enumeration i = client.getMapSettings().getBoardsSelected(); i.hasMoreElements();) {
            ((DefaultListModel) lisBoardsSelected.getModel()).addElement((index++) + ": " + i.nextElement()); //$NON-NLS-1$
        }
    }

    private void setupMainPanel() {

        panUnits = new JPanel(new BorderLayout());
        panUnits.add(panEntities, BorderLayout.CENTER);
        panUnits.add(panBVs, BorderLayout.EAST);

        setupTop();

        panMain = new JPanel();

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panMain.setLayout(gridbag);

        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butOptions, c);
        panMain.add(butOptions);

        c.weighty = 1.0;
        gridbag.setConstraints(panUnits, c);
        panMain.add(panUnits);

        // Should we display the panels in tabs?
        if (GUIPreferences.getInstance().getChatLoungeTabs()) {
            panTabs.add("Select Units", panMain); //$NON-NLS-1$
            panTabs.add("Configure Game", panTop); //$NON-NLS-1$
        } else {
            c.weighty = 0.0;
            gridbag.setConstraints(panTop, c);
            panMain.add(panTop);
        }
    }

    /**
     * Sets up the top panel with the player info, map info and starting
     * positions
     */
    private void setupTop() {
        panTop = new JPanel(new BorderLayout());

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panTop.setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(6, 6, 1, 6);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 1;
        gridbag.setConstraints(panBoardSettings, c);
        panTop.add(panBoardSettings);

        gridbag.setConstraints(panStarts, c);
        panTop.add(panStarts);

        gridbag.setConstraints(panPlayerInfo, c);
        panTop.add(panPlayerInfo);

        gridbag.setConstraints(panMinefield, c);
        panTop.add(panMinefield);
    }

    /**
     * Sets up the entities panel
     */
    private void setupEntities() {
        lisEntities = new JList(new DefaultListModel());
        lisEntities.addListSelectionListener(this);

        butLoadList = new JButton(Messages.getString("ChatLounge.butLoadList")); //$NON-NLS-1$
        butLoadList.setActionCommand("load_list"); //$NON-NLS-1$
        butLoadList.addActionListener(this);

        //          lblPlaceholder = new JLabel();

        butSaveList = new JButton(Messages.getString("ChatLounge.butSaveList")); //$NON-NLS-1$
        butSaveList.setActionCommand("save_list"); //$NON-NLS-1$
        butSaveList.addActionListener(this);
        butSaveList.setEnabled(false);

        butLoad = new JButton(Messages.getString("ChatLounge.butLoad")); //$NON-NLS-1$
        butLoadCustomBA = new JButton(Messages.getString("ChatLounge.butLoadCustomBA"));

        MechSummaryCache mechSummaryCache = MechSummaryCache.getInstance();
        mechSummaryCache.addListener(mechSummaryCacheListener);
        butLoad.setEnabled(mechSummaryCache.isInitialized());
        butLoadCustomBA.setEnabled(mechSummaryCache.isInitialized());

        Font font = new Font("sanserif", Font.BOLD, 18); //$NON-NLS-1$
        if (font == null) {
            System.err.println("Couldn't find the new font for the 'Add a Unit' button."); //$NON-NLS-1$
        } else {
            butLoad.setFont(font);
        }
        butLoad.setActionCommand("load_mech"); //$NON-NLS-1$
        butLoad.addActionListener(this);
        butLoadCustomBA.setActionCommand("load_custom_ba"); //$NON-NLS-1$
        butLoadCustomBA.addActionListener(this);

        butCustom = new JButton(Messages.getString("ChatLounge.butCustom")); //$NON-NLS-1$
        butCustom.setActionCommand("custom_mech"); //$NON-NLS-1$
        butCustom.addActionListener(this);
        butCustom.setEnabled(false);

        butMechReadout = new JButton(Messages.getString("ChatLounge.butMechReadout")); //$NON-NLS-1$
        butMechReadout.setActionCommand("Mech_readout"); //$NON-NLS-1$
        butMechReadout.addActionListener(this);
        butMechReadout.setEnabled(false);

        butViewGroup = new JButton(Messages.getString("ChatLounge.butViewGroup")); //$NON-NLS-1$
        butViewGroup.setActionCommand("view_group"); //$NON-NLS-1$
        butViewGroup.addActionListener(this);
        butViewGroup.setEnabled(false);

        butDelete = new JButton(Messages.getString("ChatLounge.butDelete")); //$NON-NLS-1$
        butDelete.setActionCommand("delete_mech"); //$NON-NLS-1$
        butDelete.addActionListener(this);
        butDelete.setEnabled(false);

        butDeleteAll = new JButton(Messages.getString("ChatLounge.butDeleteAll")); //$NON-NLS-1$
        butDeleteAll.setActionCommand("delete_all"); //$NON-NLS-1$
        butDeleteAll.addActionListener(this);
        butDeleteAll.setEnabled(false);

        panEntities = new JPanel();

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panEntities.setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(lisEntities, c);
        panEntities.add(lisEntities);

        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridheight = 2;
        gridbag.setConstraints(butLoad, c);
        panEntities.add(butLoad);

        c.gridheight = 1;
        gridbag.setConstraints(butCustom, c);
        panEntities.add(butCustom);

        gridbag.setConstraints(butLoadList, c);
        panEntities.add(butLoadList);

        gridbag.setConstraints(butMechReadout, c);
        panEntities.add(butMechReadout);

        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butDelete, c);
        panEntities.add(butDelete);

        c.gridwidth = 1;
        c.gridy = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(butLoadCustomBA, c);
        panEntities.add(butLoadCustomBA);

        //          c.gridwidth = 1;
        //          gridbag.setConstraints( lblPlaceholder, c );
        //          panEntities.add( lblPlaceholder );

        c.gridwidth = 1;
        gridbag.setConstraints(butSaveList, c);
        panEntities.add(butSaveList);

        c.gridwidth = 1;
        gridbag.setConstraints(butViewGroup, c);
        panEntities.add(butViewGroup);

        c.gridwidth = 1;
        gridbag.setConstraints(butDeleteAll, c);
        panEntities.add(butDeleteAll);
    }

    /**
     * Sets up the battle values panel
     */
    private void setupBVs() {
        labBVs = new JLabel(Messages.getString("ChatLounge.labBVs.BV"), JLabel.CENTER); //$NON-NLS-1$

        lisBVs = new JList(new DefaultListModel());

        panBVs = new JPanel();

        bvCbg = new ButtonGroup();
        chkBV = new JRadioButton(Messages.getString("ChatLounge.chkBV"), true); //$NON-NLS-1$
        bvCbg.add(chkBV);
        chkBV.addItemListener(this);
        chkTons = new JRadioButton(Messages.getString("ChatLounge.chkTons"), false); //$NON-NLS-1$
        bvCbg.add(chkTons);
        chkTons.addItemListener(this);
        chkCost = new JRadioButton(Messages.getString("ChatLounge.chkCost"), false);
        bvCbg.add(chkCost);
        chkCost.addItemListener(this);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panBVs.setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labBVs, c);
        panBVs.add(labBVs);

        c.weightx = 1.0;
        c.weighty = 1.0;
        gridbag.setConstraints(lisBVs, c);
        panBVs.add(lisBVs);

        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 1;
        gridbag.setConstraints(lisBVs, c);
        panBVs.add(chkBV);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(lisBVs, c);
        panBVs.add(chkTons);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(lisBVs, c);
        panBVs.add(chkCost);
    }

    /**
     * Sets up the starting positions panel
     */
    private void setupStarts() {
        labStarts = new JLabel(Messages.getString("ChatLounge.labStarts"), JLabel.CENTER); //$NON-NLS-1$

        lisStarts = new JList(new DefaultListModel());

        butChangeStart = new JButton(Messages.getString("ChatLounge.butChangeStart")); //$NON-NLS-1$
        butChangeStart.addActionListener(this);

        panStarts = new JPanel();

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panStarts.setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labStarts, c);
        panStarts.add(labStarts);

        c.weightx = 1.0;
        c.weighty = 1.0;
        gridbag.setConstraints(lisStarts, c);
        panStarts.add(lisStarts);

        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(butChangeStart, c);
        panStarts.add(butChangeStart);
    }

    /**
     * Refreshes the game settings with new info from the client
     */
    private void refreshGameSettings() {
        refreshTeams();
        refreshDoneButton();
    }

    /**
     * Refreshes the entities from the client
     */
    public void refreshEntities() {
        ((DefaultListModel) lisEntities.getModel()).removeAllElements();
        int listIndex = 0;
        boolean localUnits = false;
        entityCorrespondance = new int[client.game.getNoOfEntities()];

        /* We will attempt to sort by the following criteria: My units
        first, then my teamates units, then other teams units.  We
        will also sort by player name within the forementioned
        categories.  Finally, a players units will be sorted by the
        order they were "added" to the list.
        */
        LinkedList sortedEntities = new LinkedList();
        for (Enumeration i = client.getEntities(); i.hasMoreElements();) {
            Entity entity = (Entity) i.nextElement();
            sortedEntities.add(entity);
        }
        Collections.sort(sortedEntities, new Comparator() {
            public int compare(Object aa, Object bb) {
                Entity a = (Entity) aa;
                Entity b = (Entity) bb;
                Player p_a = a.getOwner();
                Player p_b = b.getOwner();
                int t_a = p_a.getTeam();
                int t_b = p_b.getTeam();
                if (p_a.equals(client.getLocalPlayer()) &&
                        !p_b.equals(client.getLocalPlayer())) {
                    return -1;
                } else if (p_b.equals(client.getLocalPlayer()) &&
                        !p_a.equals(client.getLocalPlayer())) {
                    return 1;
                } else if (t_a == client.getLocalPlayer().getTeam()
                        && t_b != client.getLocalPlayer().getTeam()) {
                    return -1;
                } else if (t_b == client.getLocalPlayer().getTeam()
                        && t_a != client.getLocalPlayer().getTeam()) {
                    return 1;
                } else if (t_a != t_b) {
                    return t_a - t_b;
                } else if (!p_a.equals(p_b)) {
                    return p_a.getName().compareTo(p_b.getName());
                } else {
                    return a.getId() - b.getId();
                }
            }
        });

        for (Iterator i = sortedEntities.iterator(); i.hasNext();) {
            Entity entity = (Entity) i.next();

            // Remember if the local player has units.
            if (!localUnits && entity.getOwner().equals(client.getLocalPlayer())) {
                localUnits = true;
            }

            if (!client.game.getOptions().booleanOption("pilot_advantages")) { //$NON-NLS-1$
                entity.getCrew().clearAdvantages();
            }

            // Handle the "Blind Drop" option.
            if (!entity.getOwner().equals(client.getLocalPlayer())
                    && client.game.getOptions().booleanOption("blind_drop") //$NON-NLS-1$
                    && !client.game.getOptions().booleanOption("real_blind_drop")) { //$NON-NLS-1$

                ((DefaultListModel) lisEntities.getModel()).addElement(formatUnit(entity, true));
                entityCorrespondance[listIndex++] = entity.getId();
            } else if (entity.getOwner().equals(client.getLocalPlayer())
                    || (!client.game.getOptions().booleanOption("blind_drop") //$NON-NLS-1$
                    && !client.game.getOptions().booleanOption("real_blind_drop"))) { //$NON-NLS-1$
                ((DefaultListModel) lisEntities.getModel()).addElement(formatUnit(entity, false));
                entityCorrespondance[listIndex++] = entity.getId();
            }
        }

        // Enable the "Save Unit List..." and "Delete All"
        // buttons if the local player has units.
        butSaveList.setEnabled(localUnits);
        butDeleteAll.setEnabled(localUnits);

        butViewGroup.setEnabled(lisEntities.getModel().getSize() != 0);

        // Disable the "must select" buttons.
        butCustom.setEnabled(false);
        butMechReadout.setEnabled(false);
        butDelete.setEnabled(false);
    }

    public static String formatUnit(Entity entity, boolean blindDrop) {
        String value;

        // Reset the tree strings.
        String strTreeSet = ""; //$NON-NLS-1$
        String strTreeView = ""; //$NON-NLS-1$

        // Set the tree strings based on C3 settings for the unit.
        if (entity.hasC3i()) {
            if (entity.calculateFreeC3Nodes() == 5)
                strTreeSet = "**"; //$NON-NLS-1$
            strTreeView = " (" + entity.getC3NetId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        } else if (entity.hasC3()) {
            if (entity.getC3Master() == null) {
                if (entity.hasC3S())
                    strTreeSet = "***"; //$NON-NLS-1$
                else
                    strTreeSet = "*"; //$NON-NLS-1$
            } else if (!entity.C3MasterIs(entity)) {
                strTreeSet = ">"; //$NON-NLS-1$
                if (entity.getC3Master().getC3Master() != null
                        && !entity.getC3Master().C3MasterIs(entity.getC3Master()))
                    strTreeSet = ">>"; //$NON-NLS-1$
                strTreeView = " -> " + entity.getC3Master().getDisplayName(); //$NON-NLS-1$
            }
        }

        int crewAdvCount = entity.getCrew().countAdvantages();

        if (blindDrop) {
            String unitClass; //$NON-NLS-1$
            if (entity instanceof Infantry) {
                unitClass = Messages.getString("ChatLounge.0"); //$NON-NLS-1$
            } else if (entity instanceof Protomech) {
                unitClass = Messages.getString("ChatLounge.1"); //$NON-NLS-1$
            } else if (entity instanceof GunEmplacement) {
                unitClass = Messages.getString("ChatLounge.2"); //$NON-NLS-1$
            } else {
                unitClass = entity.getWeightClassName();
                if (entity instanceof Tank) {
                    unitClass += Messages.getString("ChatLounge.6"); //$NON-NLS-1$
                }
            }
            value = Messages.getString("ChatLounge.EntityListEntry1", new Object[]{//$NON-NLS-1$
                entity.getOwner().getName(),
                new Integer(entity.getCrew().getGunnery()),
                new Integer(entity.getCrew().getPiloting()),
                (crewAdvCount > 0 ? " <" + crewAdvCount + Messages.getString("ChatLounge.advs") : ""), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                unitClass,
                ((entity.isOffBoard()) ? Messages.getString("ChatLounge.deploysOffBoard") : ""), //$NON-NLS-1$ //$NON-NLS-2$
                ((entity.getDeployRound() > 0) ? Messages.getString("ChatLounge.deploysAfterRound") + entity.getDeployRound() : "")}); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            value = strTreeSet
                    + Messages.getString("ChatLounge.EntityListEntry2", new Object[]{//$NON-NLS-1$
                        entity.getDisplayName(),
                        new Integer(entity.getCrew().getGunnery()),
                        new Integer(entity.getCrew().getPiloting()),
                        (crewAdvCount > 0 ? " <" + crewAdvCount + Messages.getString("ChatLounge.advs") : ""), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        new Integer(entity.calculateBattleValue()),
                        strTreeView,
                        ((entity.isOffBoard()) ? Messages.getString("ChatLounge.deploysOffBoard") : ""), //$NON-NLS-1$ //$NON-NLS-2$
                        ((entity.getDeployRound() > 0) ? Messages.getString("ChatLounge.deploysAfterRound") + entity.getDeployRound() : ""), //$NON-NLS-1$ //$NON-NLS-2$
                        (entity.isDesignValid() ? "" : Messages.getString("ChatLounge.invalidDesign"))}); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return value;
    }

    /**
     * Refreshes the player info
     */
    private void refreshPlayerInfo() {
        ((DefaultListModel) lisPlayerInfo.getModel()).removeAllElements();
        for (Enumeration i = client.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player) i.nextElement();
            if (player != null) {
                StringBuffer pi = new StringBuffer();
                pi.append(player.getName()).append(" : "); //$NON-NLS-1$
                pi.append(Player.teamNames[player.getTeam()]);

                String plyrCamo = player.getCamoFileName();

                if ((plyrCamo == null) || Player.NO_CAMO.equals(plyrCamo)) {
                    pi.append(", ").append(Player.colorNames[player.getColorIndex()]); //$NON-NLS-1$
                } else {
                    pi.append(", ").append(player.getCamoFileName()); //$NON-NLS-1$
                }

                ((DefaultListModel) lisPlayerInfo.getModel()).addElement(pi.toString());
            }
        }
    }

    /**
     * Refreshes the minefield
     */
    private void refreshMinefield() {
        ((DefaultListModel) lisMinefield.getModel()).removeAllElements();
        for (Enumeration i = client.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player) i.nextElement();
            if (player != null) {
                StringBuffer pi = new StringBuffer();
                pi.append(player.getName()).append(" : "); //$NON-NLS-1$
                pi.append(player.getNbrMFConventional()).append("/"); //$NON-NLS-1$
                pi.append(player.getNbrMFCommand()).append("/"); //$NON-NLS-1$
                pi.append(player.getNbrMFVibra());

                ((DefaultListModel) lisMinefield.getModel()).addElement(pi.toString());
            }
        }
        int nbr = client.getLocalPlayer().getNbrMFConventional();
        fldConventional.setText(Integer.toString(nbr));

        nbr = client.getLocalPlayer().getNbrMFCommand();
        fldCommandDetonated.setText(Integer.toString(nbr));

        nbr = client.getLocalPlayer().getNbrMFVibra();
        fldVibrabomb.setText(Integer.toString(nbr));
    }

    /**
     * Refreshes the battle values/tons from the client
     */
    private void refreshBVs() {
        final boolean useBv = chkBV.isSelected();
        final boolean useCost = chkCost.isSelected();

        ((DefaultListModel) lisBVs.getModel()).removeAllElements();
        for (Enumeration i = client.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player) i.nextElement();
            if (player == null) {
                continue;
            }
            float playerValue = 0;
            for (Enumeration j = client.getEntities(); j.hasMoreElements();) {
                Entity entity = (Entity) j.nextElement();
                if (entity.getOwner().equals(player)) {
                    if (useBv)
                        playerValue += entity.calculateBattleValue();
                    else if (useCost) {
                        playerValue += entity.getCost();
                    } else {
                        playerValue += entity.getWeight();
                    }
                }
            }
            if (useBv) {
                ((DefaultListModel) lisBVs.getModel()).addElement(player.getName() + Messages.getString("ChatLounge.BV") + (int) playerValue); //$NON-NLS-1$
            } else if (useCost) {
                ((DefaultListModel) lisBVs.getModel()).addElement(player.getName() + Messages.getString("ChatLounge.Cost") + (int) playerValue);
            } else {
                ((DefaultListModel) lisBVs.getModel()).addElement(player.getName() + Messages.getString("ChatLounge.Tons") + playerValue); //$NON-NLS-1$
            }
        }
    }

    private void refreshCamos() {
        // Get the local player's selected camo.
        String curCat = client.getLocalPlayer().getCamoCategory();
        String curItem = client.getLocalPlayer().getCamoFileName();

        // If the player has no camo selected, show his color.
        if (curItem == null) {
            curCat = Player.NO_CAMO;
            curItem = Player.colorNames[client.getLocalPlayer().getColorIndex()];
        }

        // Now update the camo selection dialog.
        camoDialog.setCategory(curCat);
        camoDialog.setItemName(curItem);
    }

    /**
     * Refreshes the starting positions
     */
    private void refreshStarts() {
        ((DefaultListModel) lisStarts.getModel()).removeAllElements();
        for (Enumeration i = client.getPlayers(); i.hasMoreElements();) {
            Player player = (Player) i.nextElement();
            if (player != null) {
                StringBuffer ssb = new StringBuffer();
                ssb.append(player.getName()).append(" : "); //$NON-NLS-1$
                ssb.append(IStartingPositions.START_LOCATION_NAMES[player.getStartingPos()]);
                ((DefaultListModel) lisStarts.getModel()).addElement(ssb.toString());
            }
        }
    }

    /**
     * Setup the team choice box
     */
    private void setupTeams() {
        choTeam.removeAll();
        for (int i = 0; i < Player.MAX_TEAMS; i++) {
            choTeam.addItem(Player.teamNames[i]);
        }
        if (client.getLocalPlayer() != null) {
            choTeam.setSelectedIndex(client.getLocalPlayer().getTeam());
        } else {
            choTeam.setSelectedIndex(0);
        }
    }

    /**
     * Highlight the team the player is playing on.
     */
    private void refreshTeams() {
        choTeam.setSelectedIndex(client.getLocalPlayer().getTeam());
    }

    /**
     * Refreshes the done button.  The label will say the opposite of the
     * player's "done" status, indicating that clicking it will reverse the
     * condition.
     */
    private void refreshDoneButton(boolean done) {
        butDone.setText(done ? Messages.getString("ChatLounge.notDone") : Messages.getString("ChatLounge.imDone")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void refreshDoneButton() {
        refreshDoneButton(client.getLocalPlayer().isDone());
    }

    /**
     * Change local player team.
     */
    private void changeTeam(int team) {
        Client c = getPlayerListSelected(lisPlayerInfo);
        if (c != null && c.getLocalPlayer().getTeam() != team) {
            c.getLocalPlayer().setTeam(team);
            c.sendPlayerInfo();
        }
    }

    private void updateMinefield() {
        String conv = fldConventional.getText();
        String cmd = fldCommandDetonated.getText();
        String vibra = fldVibrabomb.getText();

        int nbrConv = 0;
        int nbrCmd = 0;
        int nbrVibra = 0;

        try {
            if (conv != null && conv.length() != 0) {
                nbrConv = Integer.parseInt(conv);
            }
            if (cmd != null && cmd.length() != 0) {
                nbrCmd = Integer.parseInt(cmd);
            }
            if (vibra != null && vibra.length() != 0) {
                nbrVibra = Integer.parseInt(vibra);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(clientgui.frame, Messages.getString("ChatLounge.MinefieldAlert.message"), Messages.getString("ChatLounge.MinefieldAlert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        if (nbrConv < 0 || nbrCmd < 0 || nbrVibra < 0) {
            JOptionPane.showMessageDialog(clientgui.frame, Messages.getString("ChatLounge.MinefieldAlert.message"), Messages.getString("ChatLounge.MinefieldAlert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }
        Client c = getPlayerListSelected(lisMinefield);
        c.getLocalPlayer().setNbrMFConventional(nbrConv);
        c.getLocalPlayer().setNbrMFCommand(nbrCmd);
        c.getLocalPlayer().setNbrMFVibra(nbrVibra);
        c.sendPlayerInfo();
    }

    /**
     * Pop up the customize mech dialog
     */

    private void customizeMech() {
        if (lisEntities.getSelectedIndex() == -1) {
            return;
        }
        Entity entity = client.game.getEntity(entityCorrespondance[lisEntities.getSelectedIndex()]);
        customizeMech(entity);
    }

    public void customizeMech(Entity entity) {
        boolean editable = clientgui.getBots().get(entity.getOwner().getName()) != null;
        Client c;
        if (editable) {
            c = (Client) clientgui.getBots().get(entity.getOwner().getName());
        } else {
            editable |= entity.getOwnerId() == client.getLocalPlayer().getId();
            c = client;
        }
        // When we customize a single entity's C3 network setting,
        // **ALL** members of the network may get changed.
        Entity c3master = entity.getC3Master();
        ArrayList<Entity> c3members = new ArrayList<Entity>();
        Enumeration playerUnits = c.game.getPlayerEntities(c.getLocalPlayer()).elements();
        while (playerUnits.hasMoreElements()) {
            Entity unit = (Entity) playerUnits.nextElement();
            if (!entity.equals(unit) && entity.onSameC3NetworkAs(unit)) {
                c3members.add(unit);
            }
        }

        // display dialog
        CustomMechDialog cmd = new CustomMechDialog(clientgui, c, entity, editable);
        cmd.refreshOptions();
        cmd.setTitle(entity.getShortName());
        cmd.setVisible(true);
        if (editable && cmd.isOkay()) {
            // send changes
            c.sendUpdateEntity(entity);

            // Do we need to update the members of our C3 network?
            if ((c3master != null && !c3master.equals(entity.getC3Master()))
                    || (c3master == null && entity.getC3Master() != null)) {
                for (Entity unit : c3members) {
                    c.sendUpdateEntity(unit);
                }
            }
        }
    }

    /**
     * Pop up the view mech dialog
     */
    private void mechReadout() {
        if (lisEntities.getSelectedIndex() == -1) {
            return;
        }
        Entity entity = client.game.getEntity(entityCorrespondance[lisEntities.getSelectedIndex()]);
        MechView mechView = new MechView(entity);
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12)); //$NON-NLS-1$
        ta.setText(mechView.getMechReadout());
        final JDialog dialog = new JDialog(clientgui.frame, Messages.getString("ChatLounge.quickView"), false); //$NON-NLS-1$
        JButton btn = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
        dialog.add("South", btn); //$NON-NLS-1$
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        });
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dialog.setVisible(false);
            }
        });
        dialog.add("Center", ta); //$NON-NLS-1$

        // Preview image of the Mech...
        JLabel panPreview = new JLabel();
        panPreview.setPreferredSize(new Dimension(84, 72));
        clientgui.loadPreviewImage(panPreview, entity);
        dialog.add("North", panPreview); //$NON-NLS-1$

        dialog.setLocation(clientgui.frame.getLocation().x + clientgui.frame.getSize().width / 2 - dialog.getSize().width / 2,
                clientgui.frame.getLocation().y + clientgui.frame.getSize().height / 5 - dialog.getSize().height / 2);
        dialog.setSize(300, 450);

        dialog.validate();
        dialog.setVisible(true);
    }

    /**
     * Pop up the dialog to load a mech
     */
    private void loadMech() {
        clientgui.getMechSelectorDialog().setVisible(true);
    }

    private void loadCustomBA() {
        clientgui.getCustomBADialog().setVisible(true);
    }

    private void viewGroup() {
        new MechGroupView(clientgui.getFrame(), client, entityCorrespondance).setVisible(true);
    }

    //
    // GameListener
    //
    public void gamePlayerChange(GamePlayerChangeEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        refreshDoneButton();
        refreshBVs();
        refreshPlayerInfo();
        refreshStarts();
        refreshCamos();
        refreshMinefield();
    }

    public void gamePhaseChange(GamePhaseChangeEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        if (client.game.getPhase() == IGame.PHASE_LOUNGE) {
            refreshDoneButton();
            refreshGameSettings();
            refreshPlayerInfo();
            refreshTeams();
            refreshCamos();
            refreshMinefield();
            refreshEntities();
            refreshBVs();
            refreshStarts();
            refreshBoardSettings();
        }
    }

    public void gameEntityNew(GameEntityNewEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        refreshEntities();
        refreshBVs();
    }

    public void gameEntityRemove(GameEntityRemoveEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        refreshEntities();
        refreshBVs();
    }

    public void gameSettingsChange(GameSettingsChangeEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        refreshGameSettings();
        refreshBoardSettings();
        refreshEntities();
    }

    /*
     * NOTE: On linux, this gets called even when programatically updating the
     * list box selected item.  Do not let this go into an infinite loop.  Do not
     * update the selected item (even indirectly, by sending player info) if 
     * it is already selected.
     */
    public void itemStateChanged(ItemEvent ev) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (ev.getSource().equals(choTeam)) {
            changeTeam(choTeam.getSelectedIndex());
        } else if (ev.getSource().equals(chkBV) || ev.getSource().equals(chkTons) || ev.getSource().equals(chkCost)) {
            refreshBVs();
            if (ev.getSource().equals(chkBV)) {
                labBVs.setText(Messages.getString("ChatLounge.labBVs.BV"));
            } else if (ev.getSource().equals(chkTons)) {
                labBVs.setText(Messages.getString("ChatLounge.labBVs.Tons"));
            } else {
                labBVs.setText(Messages.getString("ChatLounge.labBVs.Cost"));
            }
        }

    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (ev.getSource().equals(butDone)) {

            // enforce exclusive deployment zones in double blind
            if (client.game.getOptions().booleanOption("double_blind") &&
                    client.game.getOptions().booleanOption("exclusive_db_deployment")) {
                int i = client.getLocalPlayer().getStartingPos();
                if (i == 0) {
                    clientgui.doAlertDialog("Starting Position not allowed", "In Double Blind play, you cannot choose 'Any' as starting position.");
                    return;
                }
                for (Enumeration e = client.game.getPlayers(); e.hasMoreElements();) {
                    Player player = (Player) e.nextElement();
                    if (player.getStartingPos() == 0) {
                        continue;
                    }
                    // check for overlapping starting directions
                    if ((player.getStartingPos() == i ||
                            player.getStartingPos() + 1 == i ||
                            player.getStartingPos() - 1 == i) &&
                            player.getId() != client.getLocalPlayer().getId()) {
                        clientgui.doAlertDialog("Must choose exclusive deployment zone", "When using double blind, each player needs to have an exclusive deployment zone.");
                        return;
                    }
                }
            }
            boolean anyDelayed = false;

            Enumeration iter = client.getEntities();

            while (iter.hasMoreElements()) {
                Entity en = (Entity) iter.nextElement();

                if (en.getDeployRound() > 0) {
                    anyDelayed = true;
                    break;
                }
            }

            boolean done = !client.getLocalPlayer().isDone();
            client.sendDone(done);
            refreshDoneButton(done);
            for (Iterator i = clientgui.getBots().values().iterator(); i.hasNext();) {
                ((Client) i.next()).sendDone(done);
            }
        } else if (ev.getSource().equals(butLoad)) {
            loadMech();
        } else if (ev.getSource().equals(butLoadCustomBA)) {
            loadCustomBA();
        } else if (ev.getSource().equals(butCustom) || ev.getSource().equals(lisEntities)) {
            customizeMech();
        } else if (ev.getSource().equals(butDelete)) {
            // delete mech
            Entity e = client.getEntity(entityCorrespondance[lisEntities.getSelectedIndex()]);
            Client c = (Client) clientgui.getBots().get(e.getOwner().getName());
            if (c == null) {
                c = client;
            }
            if (lisEntities.getSelectedIndex() != -1) {
                c.sendDeleteEntity(entityCorrespondance[lisEntities.getSelectedIndex()]);
            }
        } else if (ev.getSource().equals(butDeleteAll)) {
            // Build a Vector of this player's entities.
            Vector currentUnits = client.game.getPlayerEntities(client.getLocalPlayer());

            // Walk through the vector, deleting the entities.
            Enumeration entities = currentUnits.elements();
            while (entities.hasMoreElements()) {
                final Entity entity = (Entity) entities.nextElement();
                client.sendDeleteEntity(entity.getId());
            }
        } else if (ev.getSource().equals(butChangeBoard) || ev.getSource().equals(lisBoardsSelected)) {
            // board settings
            clientgui.getBoardSelectionDialog().update(client.getMapSettings(), true);
            clientgui.getBoardSelectionDialog().setVisible(true);
        } else if (ev.getSource().equals(butOptions)) {
            // Make sure the game options dialog is editable.
            if (!clientgui.getGameOptionsDialog().isEditable()) {
                clientgui.getGameOptionsDialog().setEditable(true);
            }
            // Display the game options dialog.
            clientgui.getGameOptionsDialog().update(client.game.getOptions());
            clientgui.getGameOptionsDialog().setVisible(true);
        } else if (ev.getSource().equals(butChangeStart) || ev.getSource().equals(lisStarts)) {
            clientgui.getStartingPositionDialog().update();
            Client c = getPlayerListSelected(lisStarts);
            if (c == null) {
                clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"), Messages.getString("ChatLounge.SelectBotOrPlayer")); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            clientgui.getStartingPositionDialog().setClient(c);
            clientgui.getStartingPositionDialog().setVisible(true);
        } else if (ev.getSource().equals(butMechReadout)) {
            mechReadout();
        } else if (ev.getSource().equals(butViewGroup)) {
            viewGroup();
        } else if (ev.getSource().equals(butLoadList)) {
            // Allow the player to replace their current
            // list of entities with a list from a file.
            clientgui.loadListFile();
        } else if (ev.getSource().equals(butSaveList)) {
            // Allow the player to save their current
            // list of entities to a file.
            clientgui.saveListFile(client.game.getPlayerEntities(client.getLocalPlayer()));
        } else if (ev.getSource().equals(butMinefield)) {
            updateMinefield();
        } else if (ev.getSource().equals(butCamo)) {
            camoDialog.setVisible(true);
        } else if (ev.getSource().equals(butAddBot)) {
            String name = "Bot" + lisPlayerInfo.getModel().getSize(); //$NON-NLS-1$
            name = (String) JOptionPane.showInputDialog(clientgui.frame, Messages.getString("ChatLounge.Name"),
                    Messages.getString("ChatLounge.ChooseBotName"), JOptionPane.QUESTION_MESSAGE,
                    null, null, name);
            if (name == null) {
                return;
            }
            if ("".equals(name.trim())) {
                name = "Bot" + lisPlayerInfo.getModel().getSize(); //$NON-NLS-1$
            }

            BotClient c = new TestBot(name, client.getHost(), client.getPort());
            c.game.addGameListener(new BotGUI(c));
            try {
                c.connect();
            } catch (Exception e) {
                clientgui.doAlertDialog(Messages.getString("ChatLounge.AlertBot.title"), Messages.getString("ChatLounge.AlertBot.message")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            c.retrieveServerInfo();
            clientgui.getBots().put(name, c);
        } else if (ev.getSource().equals(butRemoveBot)) {
            Client c = getPlayerListSelected(lisPlayerInfo);
            if (c == null || c.equals(client)) {
                clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"), Messages.getString("ChatLounge.SelectBo")); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            c.die();
            clientgui.getBots().remove(c.getName());
        }
    }

    private Client getPlayerListSelected(JList l) {
        if (l.getSelectedIndex() == -1) {
            return client;
        }
        String name = ((String) l.getSelectedValue()).substring(0, Math.max(0, ((String) l.getSelectedValue()).indexOf(" :"))); //$NON-NLS-1$
        BotClient c = (BotClient) clientgui.getBots().get(name);
        if (c == null && client.getName().equals(name)) {
            return client;
        }
        return c;
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
     * @param distracted <code>true</code> if the listener should ignore events
     *                   <code>false</code> if the listener should pay attention again.
     *                   Events that occured while the listener was distracted NOT
     *                   going to be processed.
     */
    public void setIgnoringEvents(boolean distracted) {
        this.distracted.setIgnoringEvents(distracted);
    }

    /**
     * Retrieve the "Done" button of this object.
     *
     * @return the <code>javax.swing.JButton</code> that activates this
     *         object's "Done" action.
     */
    public JButton getDoneButton() {
        return butDone;
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        client.game.removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
    }

    /**
     * Get the secondary display section of this phase.
     *
     * @return the <code>Component</code> which is displayed in the
     *         secondary section during this phase.
     */
    public JComponent getSecondaryDisplay() {
        return labStatus;
    }

    //TODO Is there a better solution?
    //This is required because the ChatLounge adds the listener to the
    //MechSummaryCache that must be removed explicitly.            
    public void die() {
        MechSummaryCache.getInstance().removeListener(mechSummaryCacheListener);
    }

    public void valueChanged(ListSelectionEvent event) {
        if (event.getSource().equals(butRemoveBot)) {
            butRemoveBot.setEnabled(false);
            Client c = getPlayerListSelected(lisPlayerInfo);
            if (c == null) {
                lisPlayerInfo.setSelectedIndex(-1);
                return;
            }
            if (c instanceof BotClient) {
                butRemoveBot.setEnabled(true);
            }
            choTeam.setSelectedIndex(c.getLocalPlayer().getTeam());
        } else if (event.getSource().equals(lisEntities)) {
            boolean selected = lisEntities.getSelectedIndex() != -1;
            butCustom.setEnabled(selected);

            // Handle "Blind drop" option.
            if (selected && client.game.getOptions().booleanOption("blind_drop")) { //$NON-NLS-1$
                Entity entity = client.game.getEntity(entityCorrespondance[lisEntities.getSelectedIndex()]);
                butMechReadout.setEnabled(entity.getOwner().equals(client.getLocalPlayer()));
                butCustom.setEnabled(entity.getOwner().equals(client.getLocalPlayer()));
            } else {
                butMechReadout.setEnabled(selected);
            }
            butDelete.setEnabled(selected);
        } else if (event.getSource() == lisPlayerInfo) {
            butRemoveBot.setEnabled(false);
            Client c = getPlayerListSelected(lisPlayerInfo);
            if (c == null) {
                lisPlayerInfo.setSelectedIndex(-1);
                return;
            }
            if (c instanceof BotClient) {
                butRemoveBot.setEnabled(true);
            }
            choTeam.setSelectedIndex(c.getLocalPlayer().getTeam());
        }
    }
}
