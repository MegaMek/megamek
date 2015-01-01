/*
 * MegaMek -
 *  Copyright (C) 2000,2001,2002,2003,20042005,2006 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.AWT;

import gov.nist.gui.TabPanel;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.TestBot;
import megamek.client.bot.ui.AWT.BotGUI;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.MechView;
import megamek.client.ui.Messages;
import megamek.client.ui.AWT.util.PlayerColors;
import megamek.client.ui.AWT.widget.BufferedPanel;
import megamek.client.ui.AWT.widget.ImageButton;
import megamek.common.Entity;
import megamek.common.FighterSquadron;
import megamek.common.GunEmplacement;
import megamek.common.IGame;
import megamek.common.IStartingPositions;
import megamek.common.Infantry;
import megamek.common.MapSettings;
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

public class ChatLounge extends AbstractPhaseDisplay implements ActionListener,
        ItemListener, BoardViewListener, GameListener, DoneButtoned {
    /**
     *
     */
    private static final long serialVersionUID = -2899398109433232077L;

    Client client;
    private ClientGUI clientgui;

    // The camo selection dialog.
    private CamoChoiceDialog camoDialog;

    // buttons & such
    private Panel panPlayerInfo;
    private Label labPlayerInfo;
    List lisPlayerInfo;

    private Label labTeam;
    Choice choTeam;

    private Label labCamo;
    private ImageButton butCamo;

    private Button butInit;

    private Panel panMinefield;
    private Label labMinefield;
    private List lisMinefield;
    private Label labConventional;
    private Label labCommandDetonated;
    private Label labVibrabomb;
    private Label labActive;
    private Label labInferno;
    private TextField fldConventional;
    private TextField fldCommandDetonated;
    private TextField fldVibrabomb;
    private TextField fldActive;
    private TextField fldInferno;
    private Button butMinefield;

    private Button butOptions;

    private Label labMapType;
    private Label labBoardSize;
    private Label labMapSize;
    private List lisBoardsSelected;
    private Button butChangeBoard;
    private Panel panBoardSettings;
    private Button butConditions;

    private Button butLoadList;
    //private Label lblPlaceholder;
    private Button butSaveList;
    private Button butDeleteAll;

    Button butLoad;
    Button butArmy;
    Button butSkills;
    Button butLoadCustomBA;
    private Button butLoadCustomFS;
    private Button butDelete;
    private Button butCustom;
    private Button butMechReadout;
    private Button butViewGroup;
    private List lisEntities;
    private int[] entityCorrespondance;
    private Panel panEntities;

    private Label labStarts;
    private List lisStarts;
    private Panel panStarts;
    private Button butChangeStart;

    private Label labBVs;
    private List lisBVs;
    private CheckboxGroup bvCbg;
    private Checkbox chkBV;
    private Checkbox chkTons;
    private Checkbox chkCost;

    private Panel panBVs;

    private TabPanel panTabs;
    private Panel panMain;

    private Panel panUnits;
    private Panel panTop;

    private Label labStatus;
    private Button butDone;

    private Button butAddBot;
    Button butRemoveBot;

    MechSummaryCache.Listener mechSummaryCacheListener = new MechSummaryCache.Listener() {
        public void doneLoading() {
            butLoad.setEnabled(true);
            butArmy.setEnabled(true);
            butSkills.setEnabled(true);
            butLoadCustomBA.setEnabled(true);
            butLoadCustomFS.setEnabled(true);
        }
    };

    /**
     * Creates a new chat lounge for the client.
     */
    public ChatLounge(ClientGUI clientgui) {
        super();
        client = clientgui.getClient();
        this.clientgui = clientgui;

        // Create a tabbed panel to hold our components.
        panTabs = new TabPanel();
        Font tabPanelFont = new Font("Dialog", Font.BOLD, //$NON-NLS-1$
                GUIPreferences.getInstance().getInt(
                        "AdvancedChatLoungeTabFontSize"));
        panTabs.setTabFont(tabPanelFont);

        // Create a new camo selection dialog.
        camoDialog = new CamoChoiceDialog(clientgui.getFrame());

        client.game.addGameListener(this);
        clientgui.getBoardView().addBoardViewListener(this);

        butOptions = new Button(Messages.getString("ChatLounge.butOptions")); //$NON-NLS-1$
        butOptions.addActionListener(this);

        butDone = new Button(Messages.getString("ChatLounge.butDone")); //$NON-NLS-1$
        Font font = null;
        try {
            font = new Font("sanserif", Font.BOLD, 12); //$NON-NLS-1$
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        if (null == font) {
            System.err
                    .println("Couldn't find the new font for the 'Done' button."); //$NON-NLS-1$
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

        labStatus = new Label("", Label.CENTER); //$NON-NLS-1$

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

        // c.weightx = 1.0; c.weighty = 0.0;
        // addBag(labStatus, gridbag, c);

        // c.gridwidth = 1;
        // c.weightx = 1.0; c.weighty = 0.0;
        // addBag(client.cb.getComponent(), gridbag, c);

        // c.gridwidth = 1;
        // c.anchor = GridBagConstraints.EAST;
        // c.weightx = 0.0; c.weighty = 0.0;
        // c.ipady = 10;
        // addBag(butDone, gridbag, c);
        validate();
    }

    private void addBag(Component comp, GridBagLayout gridbag,
            GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
    }

    /**
     * Sets up the player info (team, camo) panel
     */
    private void setupPlayerInfo() {
        Player player = client.getLocalPlayer();

        panPlayerInfo = new Panel();

        labPlayerInfo = new Label(Messages
                .getString("ChatLounge.labPlayerInfo")); //$NON-NLS-1$

        lisPlayerInfo = new List(5);
        lisPlayerInfo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                butRemoveBot.setEnabled(false);
                Client c = getPlayerListSelected(lisPlayerInfo);
                refreshCamos();
                if (c == null) {
                    lisPlayerInfo.select(-1);
                    return;
                }
                if (c instanceof BotClient) {
                    butRemoveBot.setEnabled(true);
                }
                choTeam.select(c.getLocalPlayer().getTeam());
            }
        });

        butAddBot = new Button(Messages.getString("ChatLounge.butAddBot")); //$NON-NLS-1$
        butAddBot.setActionCommand("add_bot"); //$NON-NLS-1$
        butAddBot.addActionListener(this);

        butRemoveBot = new Button(Messages.getString("ChatLounge.butRemoveBot")); //$NON-NLS-1$
        butRemoveBot.setEnabled(false);
        butRemoveBot.setActionCommand("remove_bot"); //$NON-NLS-1$
        butRemoveBot.addActionListener(this);

        labTeam = new Label(
                Messages.getString("ChatLounge.labTeam"), Label.RIGHT); //$NON-NLS-1$
        labCamo = new Label(
                Messages.getString("ChatLounge.labCamo"), Label.RIGHT); //$NON-NLS-1$

        choTeam = new Choice();
        setupTeams();
        choTeam.addItemListener(this);

        butCamo = new ImageButton();
        butCamo.setLabel(Messages.getString("ChatLounge.noCamo")); //$NON-NLS-1$
        butCamo.setPreferredSize(84, 72);
        butCamo.setActionCommand("camo"); //$NON-NLS-1$
        butCamo.addActionListener(this);
        camoDialog.addItemListener(new CamoChoiceListener(camoDialog, butCamo,
                butOptions.getBackground(), this));
        refreshCamos();

        // If we have a camo pattern, use it. Otherwise set a background.
        Image[] images = (Image[]) camoDialog.getSelectedObjects();
        if (null != images) {
            butCamo.setImage(images[0]);
        } else {
            butCamo
                    .setBackground(PlayerColors
                            .getColor(player.getColorIndex()));
        }

        butInit = new Button(Messages.getString("ChatLounge.butInit")); //$NON-NLS-1$
        butInit.setEnabled(true);
        butInit.setActionCommand("custom_init"); //$NON-NLS-1$
        butInit.addActionListener(this);

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

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(butInit, c);
        panPlayerInfo.add(butInit);

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
        panMinefield = new Panel();

        labMinefield = new Label(Messages.getString("ChatLounge.labMinefield")); //$NON-NLS-1$

        lisMinefield = new List(2);

        labConventional = new Label(Messages
                .getString("ChatLounge.labConventional"), Label.RIGHT); //$NON-NLS-1$
        labCommandDetonated = new Label(Messages
                .getString("ChatLounge.labCommandDetonated"), Label.RIGHT); //$NON-NLS-1$
        labVibrabomb = new Label(
                Messages.getString("ChatLounge.labVibrabomb"), Label.RIGHT); //$NON-NLS-1$
        labActive = new Label(
                Messages.getString("ChatLounge.labActive"), Label.RIGHT); //$NON-NLS-1$
        labInferno = new Label(
                Messages.getString("ChatLounge.labInferno"), Label.RIGHT); //$NON-NLS-1$

        fldConventional = new TextField(1);
        fldCommandDetonated = new TextField(1);
        fldVibrabomb = new TextField(1);
        fldActive = new TextField(1);
        fldInferno = new TextField(1);

        butMinefield = new Button(Messages.getString("ChatLounge.butMinefield")); //$NON-NLS-1$
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

        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(labActive, c);
        panMinefield.add(labActive);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(fldActive, c);
        panMinefield.add(fldActive);

        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(labInferno, c);
        panMinefield.add(labInferno);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(fldInferno, c);
        panMinefield.add(fldInferno);

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

        fldActive.setEnabled(enable);
        labActive.setEnabled(enable);

        fldInferno.setEnabled(enable);
        labInferno.setEnabled(enable);

        butMinefield.setEnabled(enable);
    }

    /**
     * Sets up the board settings panel
     */
    private void setupBoardSettings() {
        labMapType = new Label(
                Messages.getString("ChatLounge.labMapType"), Label.CENTER); //$NON-NLS-1$
        labBoardSize = new Label(
                Messages.getString("ChatLounge.labBoardSize"), Label.CENTER); //$NON-NLS-1$
        labMapSize = new Label(
                Messages.getString("ChatLounge.labMapSize"), Label.CENTER); //$NON-NLS-1$

        lisBoardsSelected = new List(5);
        lisBoardsSelected.addActionListener(this);

        butChangeBoard = new Button(Messages
                .getString("ChatLounge.butChangeBoard")); //$NON-NLS-1$
        butChangeBoard.setActionCommand("change_board"); //$NON-NLS-1$
        butChangeBoard.addActionListener(this);

        butConditions = new Button(Messages.getString("ChatLounge.butConditions")); //$NON-NLS-1$
        butConditions.addActionListener(this);

        panBoardSettings = new Panel();

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panBoardSettings.setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labMapType, c);
        panBoardSettings.add(labMapType);

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

        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(butConditions, c);
        panBoardSettings.add(butConditions);

        refreshBoardSettings();
    }

    private void refreshBoardSettings() {
        labMapType.setText(Messages.getString("ChatLounge.MapType") + " " + MapSettings.getMediumName(client.getMapSettings().getMedium()));
        labBoardSize.setText(Messages
                .getString("ChatLounge.BoardSize", //$NON-NLS-1$
                        new Object[] {
                                new Integer(client.getMapSettings()
                                        .getBoardWidth()),
                                new Integer(client.getMapSettings()
                                        .getBoardHeight()) }));
        labMapSize.setText(Messages.getString("ChatLounge.MapSize", //$NON-NLS-1$
                new Object[] {
                        new Integer(client.getMapSettings().getMapWidth()),
                        new Integer(client.getMapSettings().getMapHeight()) }));

        lisBoardsSelected.removeAll();
        int index = 0;

        for (Iterator<String> i = client.getMapSettings().getBoardsSelected(); i.hasNext();) {
            if(client.getMapSettings().getMedium() == MapSettings.MEDIUM_SPACE) {
              lisBoardsSelected.add((index++) + ": " + Messages.getString("ChatLounge.SPACE")); //$NON-NLS-1$
              i.next();
            } else {
                lisBoardsSelected.add((index++) + ": " + i.next()); //$NON-NLS-1$
            }
        }
    }

    private void setupMainPanel() {

        panUnits = new Panel(new BorderLayout());
        panUnits.add(panEntities, BorderLayout.CENTER);
        panUnits.add(panBVs, BorderLayout.EAST);

        setupTop();

        panMain = new Panel();

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
        panTop = new Panel(new BorderLayout());

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
        lisEntities = new List(10);
        lisEntities.addActionListener(this);
        lisEntities.addItemListener(this);

        butLoadList = new Button(Messages.getString("ChatLounge.butLoadList")); //$NON-NLS-1$
        butLoadList.setActionCommand("load_list"); //$NON-NLS-1$
        butLoadList.addActionListener(this);

        //lblPlaceholder = new Label();

        butSaveList = new Button(Messages.getString("ChatLounge.butSaveList")); //$NON-NLS-1$
        butSaveList.setActionCommand("save_list"); //$NON-NLS-1$
        butSaveList.addActionListener(this);
        butSaveList.setEnabled(false);

        butLoad = new Button(Messages.getString("ChatLounge.butLoad")); //$NON-NLS-1$
        butArmy = new Button(Messages.getString("ChatLounge.butArmy")); //$NON-NLS-1$
        butSkills = new Button(Messages.getString("ChatLounge.butSkills")); //$NON-NLS-1$
        butLoadCustomBA = new Button(Messages
                .getString("ChatLounge.butLoadCustomBA"));
        butLoadCustomFS = new Button(Messages.getString("ChatLounge.butLoadCustomFS"));

        MechSummaryCache mechSummaryCache = MechSummaryCache.getInstance();
        mechSummaryCache.addListener(mechSummaryCacheListener);
        butLoad.setEnabled(mechSummaryCache.isInitialized());
        butArmy.setEnabled(mechSummaryCache.isInitialized());
        butLoadCustomBA.setEnabled(mechSummaryCache.isInitialized());
        butLoadCustomFS.setEnabled(mechSummaryCache.isInitialized());

        Font font = new Font("sanserif", Font.BOLD, 18); //$NON-NLS-1$
        butLoad.setFont(font);

        butLoad.setActionCommand("load_mech"); //$NON-NLS-1$
        butLoad.addActionListener(this);
        butArmy.addActionListener(this);
        butSkills.addActionListener(this);
        butLoadCustomBA.setActionCommand("load_custom_ba"); //$NON-NLS-1$
        butLoadCustomBA.addActionListener(this);
        butLoadCustomFS.setActionCommand("load_custom_fs"); //$NON-NLS-1$
        butLoadCustomFS.addActionListener(this);

        butCustom = new Button(Messages.getString("ChatLounge.butCustom")); //$NON-NLS-1$
        butCustom.setActionCommand("custom_mech"); //$NON-NLS-1$
        butCustom.addActionListener(this);
        butCustom.setEnabled(false);

        butMechReadout = new Button(Messages
                .getString("ChatLounge.butMechReadout")); //$NON-NLS-1$
        butMechReadout.setActionCommand("Mech_readout"); //$NON-NLS-1$
        butMechReadout.addActionListener(this);
        butMechReadout.setEnabled(false);

        butViewGroup = new Button(Messages.getString("ChatLounge.butViewGroup")); //$NON-NLS-1$
        butViewGroup.setActionCommand("view_group"); //$NON-NLS-1$
        butViewGroup.addActionListener(this);
        butViewGroup.setEnabled(false);

        butDelete = new Button(Messages.getString("ChatLounge.butDelete")); //$NON-NLS-1$
        butDelete.setActionCommand("delete_mech"); //$NON-NLS-1$
        butDelete.addActionListener(this);
        butDelete.setEnabled(false);

        butDeleteAll = new Button(Messages.getString("ChatLounge.butDeleteAll")); //$NON-NLS-1$
        butDeleteAll.setActionCommand("delete_all"); //$NON-NLS-1$
        butDeleteAll.addActionListener(this);
        butDeleteAll.setEnabled(false);

        panEntities = new Panel();

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
        c.gridheight = 1;
        gridbag.setConstraints(butLoad, c);
        panEntities.add(butLoad);

        gridbag.setConstraints(butCustom, c);
        panEntities.add(butCustom);

        gridbag.setConstraints(butMechReadout, c);
        panEntities.add(butMechReadout);

        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butDelete, c);
        panEntities.add(butDelete);

        c.gridy = GridBagConstraints.RELATIVE;

        c.gridwidth = 1;
        gridbag.setConstraints(butArmy, c);
        panEntities.add(butArmy);

        c.gridwidth = 1;
        gridbag.setConstraints(butSkills, c);
        panEntities.add(butSkills);

        c.gridwidth = 1;
        gridbag.setConstraints(butLoadCustomBA, c);
        panEntities.add(butLoadCustomBA);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butLoadCustomFS, c);
        panEntities.add(butLoadCustomFS);

        c.gridy = GridBagConstraints.RELATIVE;

        c.gridwidth = 1;
        gridbag.setConstraints(butLoadList, c);
        panEntities.add(butLoadList);

        c.gridwidth = 1;
        gridbag.setConstraints(butSaveList, c);
        panEntities.add(butSaveList);

        c.gridwidth = 1;
        gridbag.setConstraints(butViewGroup, c);
        panEntities.add(butViewGroup);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butDeleteAll, c);
        panEntities.add(butDeleteAll);
    }

    /**
     * Sets up the battle values panel
     */
    private void setupBVs() {
        labBVs = new Label(
                Messages.getString("ChatLounge.labBVs.BV"), Label.CENTER); //$NON-NLS-1$

        lisBVs = new List(5);

        panBVs = new Panel();

        bvCbg = new CheckboxGroup();
        chkBV = new Checkbox(
                Messages.getString("ChatLounge.chkBV"), bvCbg, true); //$NON-NLS-1$
        chkBV.addItemListener(this);
        chkTons = new Checkbox(
                Messages.getString("ChatLounge.chkTons"), bvCbg, false); //$NON-NLS-1$
        chkTons.addItemListener(this);
        chkCost = new Checkbox(Messages.getString("ChatLounge.chkCost"), bvCbg,
                false);
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
        labStarts = new Label(
                Messages.getString("ChatLounge.labStarts"), Label.CENTER); //$NON-NLS-1$

        lisStarts = new List(5);
        lisStarts.addActionListener(this);

        butChangeStart = new Button(Messages
                .getString("ChatLounge.butChangeStart")); //$NON-NLS-1$
        butChangeStart.addActionListener(this);

        panStarts = new Panel();

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
        lisEntities.removeAll();
        int listIndex = 0;
        boolean localUnits = false;
        entityCorrespondance = new int[client.game.getNoOfEntities()];

        /*
         * We will attempt to sort by the following criteria: My units first,
         * then my teamates units, then other teams units. We will also sort by
         * player name within the forementioned categories. Finally, a players
         * units will be sorted by the order they were "added" to the list.
         */
        ArrayList<Entity> sortedEntities = new ArrayList<Entity>();
        for (Enumeration<Entity> i = client.getEntities(); i.hasMoreElements();) {
            Entity entity = i.nextElement();
            sortedEntities.add(entity);
        }
        Collections.sort(sortedEntities, new Comparator<Entity>() {
            public int compare(Entity a, Entity b) {
                Player p_a = a.getOwner();
                Player p_b = b.getOwner();
                int t_a = p_a.getTeam();
                int t_b = p_b.getTeam();
                if (p_a.equals(client.getLocalPlayer())
                        && !p_b.equals(client.getLocalPlayer())) {
                    return -1;
                } else if (p_b.equals(client.getLocalPlayer())
                        && !p_a.equals(client.getLocalPlayer())) {
                    return 1;
                } else if ((t_a == client.getLocalPlayer().getTeam())
                        && (t_b != client.getLocalPlayer().getTeam())) {
                    return -1;
                } else if ((t_b == client.getLocalPlayer().getTeam())
                        && (t_a != client.getLocalPlayer().getTeam())) {
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

        for (Iterator<Entity> i = sortedEntities.iterator(); i.hasNext();) {
            Entity entity = i.next();

            // Remember if the local player has units.
            if (!localUnits
                    && entity.getOwner().equals(client.getLocalPlayer())) {
                localUnits = true;
            }

            if (!client.game.getOptions().booleanOption("pilot_advantages")) { //$NON-NLS-1$
                entity.getCrew().clearAdvantages();
            }

            boolean rpgSkills = client.game.getOptions().booleanOption(
                    "rpg_gunnery");

            // Handle the "Blind Drop" option.
            if (!entity.getOwner().equals(client.getLocalPlayer())
                    && client.game.getOptions().booleanOption("blind_drop") //$NON-NLS-1$
                    && !client.game.getOptions().booleanOption(
                            "real_blind_drop")) { //$NON-NLS-1$

                lisEntities.add(ChatLounge.formatUnit(entity, true, rpgSkills));
                entityCorrespondance[listIndex++] = entity.getId();
            } else if (entity.getOwner().equals(client.getLocalPlayer())
                    || (!client.game.getOptions().booleanOption("blind_drop") //$NON-NLS-1$
                    && !client.game.getOptions().booleanOption(
                            "real_blind_drop"))) { //$NON-NLS-1$
                lisEntities
                        .add(ChatLounge.formatUnit(entity, false, rpgSkills));
                entityCorrespondance[listIndex++] = entity.getId();
            }
        }

        // Enable the "Save Unit List..." and "Delete All"
        // buttons if the local player has units.
        butSaveList.setEnabled(localUnits);
        butDeleteAll.setEnabled(localUnits);

        butViewGroup.setEnabled(lisEntities.getItemCount() != 0);

        // Disable the "must select" buttons.
        butCustom.setEnabled(false);
        butMechReadout.setEnabled(false);
        butDelete.setEnabled(false);
    }

    public static String formatUnit(Entity entity, boolean blindDrop,
            boolean rpgSkills) {
        String value = new String();

        // Reset the tree strings.
        String strTreeSet = ""; //$NON-NLS-1$
        String strTreeView = ""; //$NON-NLS-1$

        // Set the tree strings based on C3 settings for the unit.
        if (entity.hasC3i()) {
            if (entity.calculateFreeC3Nodes() == 5) {
                strTreeSet = "**"; //$NON-NLS-1$
            }
            strTreeView = " (" + entity.getC3NetId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        } else if (entity.hasC3()) {
            if (entity.getC3Master() == null) {
                if (entity.hasC3S()) {
                    strTreeSet = "***"; //$NON-NLS-1$
                } else {
                    strTreeSet = "*"; //$NON-NLS-1$
                }
            } else if (!entity.C3MasterIs(entity)) {
                strTreeSet = ">"; //$NON-NLS-1$
                if ((entity.getC3Master().getC3Master() != null)
                        && !entity.getC3Master().C3MasterIs(
                                entity.getC3Master())) {
                    strTreeSet = ">>"; //$NON-NLS-1$
                }
                strTreeView = " -> " + entity.getC3Master().getDisplayName(); //$NON-NLS-1$
            }
        }

        int crewAdvCount = entity.getCrew().countAdvantages();
        boolean isManeiDomini = entity.getCrew().countMDImplants() > 0;

        String gunnery = Integer.toString(entity.getCrew().getGunnery());
        if (rpgSkills) {
            gunnery = entity.getCrew().getGunneryRPG();
        }

        if (blindDrop) {
            String unitClass = ""; //$NON-NLS-1$
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
            value = Messages
                    .getString(
                            "ChatLounge.EntityListEntry1", new Object[] { //$NON-NLS-1$
                                    entity.getOwner().getName(),
                                    gunnery,
                                    new Integer(entity.getCrew().getPiloting()),
                                    (crewAdvCount > 0 ? " <" + crewAdvCount + Messages.getString("ChatLounge.advs") : ""), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                    (isManeiDomini ? Messages
                                            .getString("ChatLounge.md") : ""), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                    unitClass,
                                    ((entity.isOffBoard()) ? Messages
                                            .getString("ChatLounge.deploysOffBoard") : ""), //$NON-NLS-1$ //$NON-NLS-2$
                                    ((entity.getDeployRound() > 0) ? Messages
                                            .getString("ChatLounge.deploysAfterRound") + entity.getDeployRound() : "") }); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            value = strTreeSet
                    + Messages
                            .getString(
                                    "ChatLounge.EntityListEntry2", new Object[] { //$NON-NLS-1$
                                            entity.getDisplayName(),
                                            gunnery,
                                            new Integer(entity.getCrew()
                                                    .getPiloting()),
                                            (crewAdvCount > 0 ? " <" + crewAdvCount + Messages.getString("ChatLounge.advs") : ""), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                            (isManeiDomini ? Messages
                                                    .getString("ChatLounge.md") : ""), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                            new Integer(entity
                                                    .calculateBattleValue()),
                                            strTreeView,
                                            ((entity.isOffBoard()) ? Messages
                                                    .getString("ChatLounge.deploysOffBoard") : ""), //$NON-NLS-1$ //$NON-NLS-2$
                                            ((entity.getDeployRound() > 0) ? Messages
                                                    .getString("ChatLounge.deploysAfterRound") + entity.getDeployRound() : ""), //$NON-NLS-1$ //$NON-NLS-2$
                                            (entity.isDesignValid() ? "" : Messages.getString("ChatLounge.invalidDesign")) }); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return value;
    }

    /**
     * Refreshes the player info
     */
    private void refreshPlayerInfo() {
        lisPlayerInfo.removeAll();
        for (Enumeration<Player> i = client.getPlayers(); i.hasMoreElements();) {
            final Player player = i.nextElement();
            if (player != null) {
                StringBuffer pi = new StringBuffer();
                pi.append(player.getName()).append(" : "); //$NON-NLS-1$
                pi.append(Player.teamNames[player.getTeam()]);

                String plyrCamo = player.getCamoFileName();

                if ((null == plyrCamo) || Player.NO_CAMO.equals(plyrCamo)) {
                    pi
                            .append(", ").append(Player.colorNames[player.getColorIndex()]); //$NON-NLS-1$
                } else {
                    pi.append(", ").append(player.getCamoFileName()); //$NON-NLS-1$
                }

                pi.append(", INIT: ");
                if (player.getConstantInitBonus() >= 0) {
                    pi.append(" +").append(
                            Integer.toString(player.getConstantInitBonus()));
                } else {
                    pi.append(" ").append(
                            Integer.toString(player.getConstantInitBonus()));
                }

                lisPlayerInfo.add(pi.toString());
            }
        }
    }

    /**
     * Refreshes the minefield
     */
    private void refreshMinefield() {
        lisMinefield.removeAll();
        for (Enumeration<Player> i = client.getPlayers(); i.hasMoreElements();) {
            final Player player = i.nextElement();
            if (player != null) {
                StringBuffer pi = new StringBuffer();
                pi.append(player.getName()).append(" : "); //$NON-NLS-1$
                pi.append(player.getNbrMFConventional()).append("/"); //$NON-NLS-1$
                pi.append(player.getNbrMFCommand()).append("/"); //$NON-NLS-1$
                pi.append(player.getNbrMFVibra()).append("/");
                pi.append(player.getNbrMFActive()).append("/");
                pi.append(player.getNbrMFInferno());

                lisMinefield.add(pi.toString());
            }
        }
        int nbr = client.getLocalPlayer().getNbrMFConventional();
        fldConventional.setText(Integer.toString(nbr));

        nbr = client.getLocalPlayer().getNbrMFCommand();
        fldCommandDetonated.setText(Integer.toString(nbr));

        nbr = client.getLocalPlayer().getNbrMFVibra();
        fldVibrabomb.setText(Integer.toString(nbr));

        nbr = client.getLocalPlayer().getNbrMFActive();
        fldActive.setText(Integer.toString(nbr));

        nbr = client.getLocalPlayer().getNbrMFInferno();
        fldInferno.setText(Integer.toString(nbr));
    }

    /**
     * Refreshes the battle values/tons from the client
     */
    private void refreshBVs() {
        final boolean useBv = chkBV.getState();
        final boolean useCost = chkCost.getState();

        lisBVs.removeAll();
        for (Enumeration<Player> i = client.getPlayers(); i.hasMoreElements();) {
            final Player player = i.nextElement();
            if (player == null) {
                continue;
            }
            float playerValue = 0;
            for (Enumeration<Entity> j = client.getEntities(); j
                    .hasMoreElements();) {
                Entity entity = j.nextElement();
                if (entity.getOwner().equals(player)) {
                    if (useBv) {
                        playerValue += entity.calculateBattleValue();
                    } else if (useCost) {
                        playerValue += entity.getCost();
                    } else {
                        playerValue += entity.getWeight();
                    }
                }
            }
            if (client.game.getOptions().booleanOption("real_blind_drop")
                    && (player.getId() != client.getLocalPlayer().getId())) {
                playerValue = playerValue > 0 ? 9999 : 0;
            }
            if (useBv) {
                lisBVs
                        .add(player.getName()
                                + Messages.getString("ChatLounge.BV") + (int) playerValue + " (FM:" + (int) (playerValue * player.getForceSizeBVMod()) + ")"); //$NON-NLS-1$
            } else if (useCost) {
                lisBVs.add(player.getName()
                        + Messages.getString("ChatLounge.Cost")
                        + (int) playerValue);
            } else {
                lisBVs.add(player.getName()
                        + Messages.getString("ChatLounge.Tons") + playerValue); //$NON-NLS-1$
            }
        }
    }

    void refreshCamos() {
        // Get the seleted player's selected camo.
        Client c = getPlayerListSelected(lisPlayerInfo);
        String curCat = c.getLocalPlayer().getCamoCategory();
        String curItem = c.getLocalPlayer().getCamoFileName();

        // If the player has no camo selected, show his color.
        if (null == curItem) {
            curCat = Player.NO_CAMO;
            curItem = Player.colorNames[c.getLocalPlayer().getColorIndex()];
        }

        // Now update the camo selection dialog.
        camoDialog.setCategory(curCat);
        camoDialog.setItemName(curItem);

        // TODO argoCult Programing at its best. I have only a vague idea what
        // the section below does, but I do know it needs cleanup.
        // however it is working.
        Image image = null;
        Image[] array = (Image[]) camoDialog.getSelectedObjects();
        if (null != array) {
            image = array[0];
        }

        if (null == image) {
            for (int color = 0; color < Player.colorNames.length; color++) {
                if (Player.colorNames[color].equals(curItem)) {
                    butCamo.setLabel(Messages
                            .getString("CamoChoiceListener.NoCammo")); //$NON-NLS-1$
                    butCamo.setBackground(PlayerColors.getColor(color));
                    break;
                }
            }
        }
        // We need to copy the image to make it appear.
        else {
            butCamo.setLabel(""); //$NON-NLS-1$
            butCamo.setBackground(butOptions.getBackground()); // butOptions.getBackground()
                                                                // == default
                                                                // background.
                                                                // This needs to
                                                                // be cleaned
                                                                // up.
        }

        // Update the butCamo's image.
        butCamo.setImage(image);
    }

    /**
     * Refreshes the starting positions
     */
    private void refreshStarts() {
        lisStarts.removeAll();
        for (Enumeration<Player> i = client.getPlayers(); i.hasMoreElements();) {
            Player player = i.nextElement();
            if (player != null) {
                StringBuffer ssb = new StringBuffer();
                ssb.append(player.getName()).append(" : "); //$NON-NLS-1$
                ssb.append(IStartingPositions.START_LOCATION_NAMES[player
                        .getStartingPos()]);
                lisStarts.add(ssb.toString());
            }
        }
    }

    /**
     * Setup the team choice box
     */
    private void setupTeams() {
        choTeam.removeAll();
        for (int i = 0; i < Player.MAX_TEAMS; i++) {
            choTeam.add(Player.teamNames[i]);
        }
        if (null != client.getLocalPlayer()) {
            choTeam.select(client.getLocalPlayer().getTeam());
        } else {
            choTeam.select(0);
        }
    }

    /**
     * Highlight the team the player is playing on.
     */
    private void refreshTeams() {
        choTeam.select(client.getLocalPlayer().getTeam());
    }

    /**
     * Refreshes the done button. The label will say the opposite of the
     * player's "done" status, indicating that clicking it will reverse the
     * condition.
     */
    private void refreshDoneButton(boolean done) {
        butDone
                .setLabel(done ? Messages.getString("ChatLounge.notDone") : Messages.getString("ChatLounge.imDone")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void refreshDoneButton() {
        refreshDoneButton(client.getLocalPlayer().isDone());
    }

    /**
     * Change local player team.
     */
    public void changeTeam(int team) {
        Client c = getPlayerListSelected(lisPlayerInfo);
        if ((c != null) && (c.getLocalPlayer().getTeam() != team)) {
            c.getLocalPlayer().setTeam(team);
            c.sendPlayerInfo();
        }
    }

    private void updateMinefield() {
        String conv = fldConventional.getText();
        String cmd = fldCommandDetonated.getText();
        String vibra = fldVibrabomb.getText();
        String active = fldActive.getText();
        String inferno = fldInferno.getText();

        int nbrConv = 0;
        int nbrCmd = 0;
        int nbrVibra = 0;
        int nbrActive = 0;
        int nbrInferno = 0;

        try {
            if ((conv != null) && (conv.length() != 0)) {
                nbrConv = Integer.parseInt(conv);
            }
            if ((cmd != null) && (cmd.length() != 0)) {
                nbrCmd = Integer.parseInt(cmd);
            }
            if ((vibra != null) && (vibra.length() != 0)) {
                nbrVibra = Integer.parseInt(vibra);
            }
            if ((active != null) && (active.length() != 0)) {
                nbrActive = Integer.parseInt(active);
            }
            if ((inferno != null) && (inferno.length() != 0)) {
                nbrInferno = Integer.parseInt(inferno);
            }
        } catch (NumberFormatException e) {
            AlertDialog ad = new AlertDialog(
                    clientgui.frame,
                    Messages.getString("ChatLounge.MinefieldAlert.title"), Messages.getString("ChatLounge.MinefieldAlert.message")); //$NON-NLS-1$ //$NON-NLS-2$
            ad.setVisible(true);
            return;
        }

        if ((nbrConv < 0) || (nbrCmd < 0) || (nbrVibra < 0) || (nbrActive < 0) || (nbrInferno < 0)) {
            AlertDialog ad = new AlertDialog(
                    clientgui.frame,
                    Messages.getString("ChatLounge.MinefieldAlert.title"), Messages.getString("ChatLounge.MinefieldAlert.message")); //$NON-NLS-1$ //$NON-NLS-2$
            ad.setVisible(true);
            return;
        }
        Client c = getPlayerListSelected(lisMinefield);
        c.getLocalPlayer().setNbrMFConventional(nbrConv);
        c.getLocalPlayer().setNbrMFCommand(nbrCmd);
        c.getLocalPlayer().setNbrMFVibra(nbrVibra);
        c.getLocalPlayer().setNbrMFActive(nbrActive);
        c.getLocalPlayer().setNbrMFInferno(nbrInferno);
        c.sendPlayerInfo();
    }

    /**
     * Pop up the customize mech dialog
     */

    public void customizeMech() {
        if (lisEntities.getSelectedIndex() == -1) {
            return;
        }
        Entity entity = client.game.getEntity(entityCorrespondance[lisEntities
                .getSelectedIndex()]);
        customizeMech(entity);
    }

    public void customizeMech(Entity entity) {
        boolean editable = clientgui.getBots().get(entity.getOwner().getName()) != null;
        Client c = null;
        if (editable) {
            c = clientgui.getBots().get(entity.getOwner().getName());
        } else {
            editable |= entity.getOwnerId() == client.getLocalPlayer().getId();
            c = client;
        }
        // When we customize a single entity's C3 network setting,
        // **ALL** members of the network may get changed.
        Entity c3master = entity.getC3Master();
        Vector<Entity> c3members = new Vector<Entity>();
        Iterator<Entity> playerUnits = c.game.getPlayerEntities(
                c.getLocalPlayer(), false).iterator();
        while (playerUnits.hasNext()) {
            Entity unit = playerUnits.next();
            if (!entity.equals(unit) && entity.onSameC3NetworkAs(unit)) {
                c3members.addElement(unit);
            }
        }

        // display dialog
        CustomMechDialog cmd = new CustomMechDialog(clientgui, c, entity,
                editable);
        cmd.refreshOptions();
        cmd.setTitle(entity.getShortName());
        cmd.setVisible(true);
        if (editable && cmd.isOkay()) {
            // send changes
            c.sendUpdateEntity(entity);

            // Do we need to update the members of our C3 network?
            if (((c3master != null) && !c3master.equals(entity.getC3Master()))
                    || ((c3master == null) && (entity.getC3Master() != null))) {
                Enumeration<Entity> c3Units = c3members.elements();
                while (c3Units.hasMoreElements()) {
                    Entity unit = c3Units.nextElement();
                    c.sendUpdateEntity(unit);
                }
            }
        }
    }

    /**
     * Pop up the view mech dialog
     */
    public void mechReadout() {
        if (lisEntities.getSelectedIndex() == -1) {
            return;
        }
        Entity entity = client.game.getEntity(entityCorrespondance[lisEntities
                .getSelectedIndex()]);
        MechView mechView = new MechView(entity, client.game.getOptions().booleanOption("show_bay_detail"));
        TextArea ta = new TextArea();
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12)); //$NON-NLS-1$
        ta.setText(mechView.getMechReadout());
        final Dialog dialog = new Dialog(clientgui.frame, Messages
                .getString("ChatLounge.quickView"), false); //$NON-NLS-1$
        Button btn = new Button(Messages.getString("Okay")); //$NON-NLS-1$
        dialog.add("South", btn); //$NON-NLS-1$
        btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        });
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dialog.setVisible(false);
            }
        });
        dialog.add("Center", ta); //$NON-NLS-1$

        // Preview image of the Mech...
        BufferedPanel panPreview = new BufferedPanel();
        panPreview.setPreferredSize(84, 72);
        clientgui.loadPreviewImage(panPreview, entity);
        dialog.add("North", panPreview); //$NON-NLS-1$

        dialog.setLocation(clientgui.frame.getLocation().x
                + clientgui.frame.getSize().width / 2 - dialog.getSize().width
                / 2, clientgui.frame.getLocation().y
                + clientgui.frame.getSize().height / 5
                - dialog.getSize().height / 2);
        dialog.setSize(300, 450);

        dialog.validate();
        dialog.setVisible(true);
    }

    /**
     * Pop up the dialog to load a mech
     */
    public void loadMech() {
        clientgui.getMechSelectorDialog().setVisible(true);
    }

    public void loadArmy() {
        clientgui.getRandomArmyDialog().setVisible(true);
    }

    public void loadRandomSkills() {
        clientgui.getRandomSkillDialog().setVisible(true);
    }

    public void loadCustomBA() {
        clientgui.getCustomBADialog().setVisible(true);
    }
    /*
     * This button will now just load an empty fighter squadron which must be loaded with
     * fighters during the deployment phase
     */
    public void loadCustomFS() {
        FighterSquadron fs = new FighterSquadron();
        fs.setOwner(client.getLocalPlayer());
        client.sendAddEntity(fs);
        /*
        clientgui.getCustomFSDialog().setVisible(true);
        */
    }

    public void viewGroup() {
        new MechGroupView(clientgui.getFrame(), client, entityCorrespondance)
                .setVisible(true);
    }

    //
    // GameListener
    //
    @Override
    public void gamePlayerChange(GamePlayerChangeEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        refreshDoneButton();
        clientgui.client.game.setupTeams();
        refreshBVs();
        refreshPlayerInfo();
        refreshStarts();
        refreshCamos();
        refreshMinefield();
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        if (client.game.getPhase() == IGame.Phase.PHASE_LOUNGE) {
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

    @Override
    public void gameEntityNew(GameEntityNewEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        refreshEntities();
        refreshBVs();
    }

    @Override
    public void gameEntityRemove(GameEntityRemoveEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        refreshEntities();
        refreshBVs();
    }

    @Override
    public void gameSettingsChange(GameSettingsChangeEvent e) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        refreshGameSettings();
        refreshBoardSettings();
        refreshEntities();
        refreshBVs();
    }

    /*
     * NOTE: On linux, this gets called even when programatically updating the
     * list box selected item. Do not let this go into an infinite loop. Do not
     * update the selected item (even indirectly, by sending player info) if it
     * is already selected. A Simple fix would be to ignore events while
     * changing the state.
     */
    public void itemStateChanged(ItemEvent ev) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (ev.getSource() == choTeam) {
            changeTeam(choTeam.getSelectedIndex());
        } else if ((ev.getSource() == chkBV) || (ev.getSource() == chkTons)
                || (ev.getSource() == chkCost)) {
            refreshBVs();
            if (ev.getSource() == chkBV) {
                labBVs.setText(Messages.getString("ChatLounge.labBVs.BV"));
            } else if (ev.getSource() == chkTons) {
                labBVs.setText(Messages.getString("ChatLounge.labBVs.Tons"));
            } else {
                labBVs.setText(Messages.getString("ChatLounge.labBVs.Cost"));
            }
        } else if (ev.getSource() == lisEntities) {
            boolean selected = lisEntities.getSelectedIndex() != -1;
            butCustom.setEnabled(selected);

            // Handle "Blind drop" option.
            if (selected
                    && client.game.getOptions().booleanOption("blind_drop")) { //$NON-NLS-1$
                Entity entity = client.game
                        .getEntity(entityCorrespondance[lisEntities
                                .getSelectedIndex()]);
                butMechReadout.setEnabled(entity.getOwner().equals(
                        client.getLocalPlayer()));
                butCustom.setEnabled(entity.getOwner().equals(
                        client.getLocalPlayer()));
            } else {
                butMechReadout.setEnabled(selected);
            }

            butDelete.setEnabled(selected);
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

        if (ev.getSource() == butDone) {
            // enforce exclusive deployment zones in double blind
            if (client.game.getOptions().booleanOption("double_blind")
                    && client.game.getOptions().booleanOption(
                            "exclusive_db_deployment")) {
                int i = client.getLocalPlayer().getStartingPos();
                if (i == 0) {
                    clientgui
                            .doAlertDialog("Starting Position not allowed",
                                    "In Double Blind play, you cannot choose 'Any' as starting position.");
                    return;
                }
                for (Enumeration<Player> e = client.game.getPlayers(); e
                        .hasMoreElements();) {
                    Player player = e.nextElement();
                    if (player.getStartingPos() == 0) {
                        continue;
                    }
                    // CTR and EDG don't overlap
                    if (((player.getStartingPos() == 9) && (i == 10))
                        || ((player.getStartingPos() == 10) && (i == 9))) {
                        continue;
                    }
                    // check for overlapping starting directions
                    if (((player.getStartingPos() == i)
                            || (player.getStartingPos() + 1 == i) || (player
                            .getStartingPos() - 1 == i))
                            && (player.getId() != client.getLocalPlayer()
                                    .getId())) {
                        clientgui
                                .doAlertDialog(
                                        "Must choose exclusive deployment zone",
                                        "When using double blind, each player needs to have an exclusive deployment zone.");
                        return;
                    }
                }
            }

            boolean done = !client.getLocalPlayer().isDone();
            client.sendDone(done);
            refreshDoneButton(done);
            for (Iterator<Client> i = clientgui.getBots().values().iterator(); i
                    .hasNext();) {
                i.next().sendDone(done);
            }
        } else if (ev.getSource() == butLoad) {
            loadMech();
        } else if (ev.getSource() == butArmy) {
            loadArmy();
        } else if (ev.getSource() == butSkills) {
            loadRandomSkills();
        } else if (ev.getSource() == butLoadCustomBA) {
            loadCustomBA();
        } else if (ev.getSource() == butLoadCustomFS) {
            loadCustomFS();
        } else if ((ev.getSource() == butCustom) || (ev.getSource() == lisEntities)) {
            customizeMech();
        } else if (ev.getSource() == butDelete) {
            // delete mech
            Entity e = client.getEntity(entityCorrespondance[lisEntities
                    .getSelectedIndex()]);
            Client c = clientgui.getBots().get(e.getOwner().getName());
            if (c == null) {
                c = client;
            }
            if (lisEntities.getSelectedIndex() != -1) {
                c.sendDeleteEntity(entityCorrespondance[lisEntities
                        .getSelectedIndex()]);
            }
        } else if (ev.getSource() == butDeleteAll) {
            // Build a Vector of this player's entities.
            ArrayList<Entity> currentUnits = client.game
                    .getPlayerEntities(client.getLocalPlayer(), false);

            // Walk through the vector, deleting the entities.
            Iterator<Entity> entities = currentUnits.iterator();
            while (entities.hasNext()) {
                final Entity entity = entities.next();
                client.sendDeleteEntity(entity.getId());
            }
        } else if ((ev.getSource() == butChangeBoard)
                || (ev.getSource() == lisBoardsSelected)) {
            // board settings
            clientgui.getBoardSelectionDialog().update(client.getMapSettings(),
                    true);
            clientgui.getBoardSelectionDialog().setVisible(true);
        } else if (ev.getSource() == butOptions) {
            // Make sure the game options dialog is editable.
            if (!clientgui.getGameOptionsDialog().isEditable()) {
                clientgui.getGameOptionsDialog().setEditable(true);
            }
            // Display the game options dialog.
            clientgui.getGameOptionsDialog().update(client.game.getOptions());
            clientgui.getGameOptionsDialog().setVisible(true);
        } else if ((ev.getSource() == butChangeStart)
                || (ev.getSource() == lisStarts)) {
            Client c = getPlayerListSelected(lisStarts);
            if (c == null) {
                clientgui
                        .doAlertDialog(
                                Messages
                                        .getString("ChatLounge.ImproperCommand"), Messages.getString("ChatLounge.SelectBotOrPlayer")); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            clientgui.getStartingPositionDialog().setClient(c);
            clientgui.getStartingPositionDialog().update();
            clientgui.getStartingPositionDialog().setVisible(true);
        } else if (ev.getSource() == butMechReadout) {
            mechReadout();
        } else if (ev.getSource() == butViewGroup) {
            viewGroup();
        } else if (ev.getSource() == butLoadList) {
            // Allow the player to replace their current
            // list of entities with a list from a file.
            clientgui.loadListFile();
        } else if (ev.getSource() == butSaveList) {
            // Allow the player to save their current
            // list of entities to a file.
            clientgui.saveListFile(client.game.getPlayerEntities(client
                    .getLocalPlayer(), false));
        } else if (ev.getSource() == butMinefield) {
            updateMinefield();
        } else if (ev.getSource() == butCamo) {
            camoDialog.setVisible(true);
        } else if (ev.getSource() == butInit) {
            // alert about teams
            if (clientgui.client.game.getOptions().booleanOption(
                    "team_initiative")) {
                AlertDialog id = new AlertDialog(
                        clientgui.frame,
                        Messages.getString("ChatLounge.InitiativeAlert.title"), Messages.getString("ChatLounge.InitiativeAlert.message")); //$NON-NLS-1$ //$NON-NLS-2$
                id.setVisible(true);
            }
            Client c = getPlayerListSelected(lisPlayerInfo);
            if (c == null) {
                clientgui
                        .doAlertDialog(
                                Messages
                                        .getString("ChatLounge.ImproperCommand"), Messages.getString("ChatLounge.SelectBotOrPlayer")); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            clientgui.getCustomInitiativeDialog().setClient(c);
            clientgui.getCustomInitiativeDialog().updateValues();
            clientgui.getCustomInitiativeDialog().setVisible(true);
        } else if (ev.getSource() == butAddBot) {
            String name = "Bot" + lisPlayerInfo.getItemCount(); //$NON-NLS-1$
            Prompt p = new Prompt(
                    clientgui.frame,
                    Messages.getString("ChatLounge.ChooseBotName"), Messages.getString("ChatLounge.Name"), name, 15); //$NON-NLS-1$ //$NON-NLS-2$
            if (!p.showDialog()) {
                return;
            }
            if (p.getText().trim().equals("")) { //$NON-NLS-1$
                name = "Bot" + lisPlayerInfo.getItemCount(); //$NON-NLS-1$
            } else {
                name = p.getText();
            }
            BotClient c = new TestBot(name, client.getHost(), client.getPort());
            c.game.addGameListener(new BotGUI(c));
            try {
                c.connect();
            } catch (Exception e) {
                clientgui
                        .doAlertDialog(
                                Messages.getString("ChatLounge.AlertBot.title"), Messages.getString("ChatLounge.AlertBot.message")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            c.retrieveServerInfo();
            clientgui.getBots().put(name, c);
        } else if (ev.getSource() == butRemoveBot) {
            Client c = getPlayerListSelected(lisPlayerInfo);
            if ((c == null) || (c == client)) {
                clientgui
                        .doAlertDialog(
                                Messages
                                        .getString("ChatLounge.ImproperCommand"), Messages.getString("ChatLounge.SelectBo")); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            c.die();
            clientgui.getBots().remove(c.getName());
        } else if (ev.getSource() == butConditions) {
            // Display the game options dialog.
            clientgui.getPlanetaryConditionsDialog().update(client.game.getPlanetaryConditions());
            clientgui.getPlanetaryConditionsDialog().setVisible(true);
        }
    }

    Client getPlayerListSelected(List l) {
        if (l.getSelectedIndex() == -1) {
            return client;
        }
        String name = l.getSelectedItem().substring(0,
                Math.max(0, l.getSelectedItem().indexOf(" :"))); //$NON-NLS-1$
        BotClient c = (BotClient) clientgui.getBots().get(name);
        if ((c == null) && client.getName().equals(name)) {
            return client;
        }
        return c;
    }

    /**
     * Allow others to see what player is currently selected. Nessecary for
     * CameoChoieListener.
     *
     * @return
     */
    protected Client getPlayerListSelectedClient() {
        if (lisPlayerInfo.getSelectedIndex() == -1) {
            return client;
        }
        String name = lisPlayerInfo.getSelectedItem().substring(0,
                Math.max(0, lisPlayerInfo.getSelectedItem().indexOf(" :"))); //$NON-NLS-1$
        BotClient c = (BotClient) clientgui.getBots().get(name);
        if ((c == null) && client.getName().equals(name)) {
            return client;
        }
        return c;
    }

    /**
     * Retrieve the "Done" button of this object.
     *
     * @return the <code>java.awt.Button</code> that activates this object's
     *         "Done" action.
     */
    public Button getDoneButton() {
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
     * @return the <code>Component</code> which is displayed in the secondary
     *         section during this phase.
     */
    public Component getSecondaryDisplay() {
        return labStatus;
    }

    // TODO Is there a better solution?
    // This is required because the ChatLounge adds the listener to the
    // MechSummaryCache that must be removed explicitly.
    public void die() {
        MechSummaryCache.getInstance().removeListener(mechSummaryCacheListener);
    }

}
