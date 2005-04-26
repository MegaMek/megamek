/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Vector;

import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.LinkedList;
import com.sun.java.util.collections.Comparator;
import com.sun.java.util.collections.Collections;

import gov.nist.gui.TabPanel;

import megamek.client.bot.BotClient;
import megamek.client.bot.BotGUI;
import megamek.client.bot.TestBot;
import megamek.client.util.PlayerColors;
import megamek.client.util.widget.BufferedPanel;
import megamek.client.util.widget.ImageButton;
import megamek.common.*;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableAdapter;

public class ChatLounge
    extends AbstractPhaseDisplay
    implements ActionListener, ItemListener, BoardListener, GameListener, DoneButtoned, Distractable {
    // Distraction implementation.
    private DistractableAdapter distracted = new DistractableAdapter();

    // parent Client
    private Client client;
    private ClientGUI clientgui;

    // The camo selection dialog.
    private CamoChoiceDialog camoDialog;

    // buttons & such
    private Panel panPlayerInfo;
    private Label labPlayerInfo;
    private List lisPlayerInfo;

    private Label labTeam;
    private Choice choTeam;

    private Label labCamo;
    private ImageButton butCamo;

    private Panel panMinefield;
    private Label labMinefield;
    private List lisMinefield;
    private Label labConventional;
    private Label labCommandDetonated;
    private Label labVibrabomb;
    private TextField fldConventional;
    private TextField fldCommandDetonated;
    private TextField fldVibrabomb;
    private Button butMinefield;

    private Button butOptions;

    private Label labBoardSize;
    private Label labMapSize;
    private List lisBoardsSelected;
    private Button butChangeBoard;
    private Panel panBoardSettings;

    private Button butLoadList;
    //      private Label  lblPlaceholder;
    private Button butSaveList;
    private Button butDeleteAll;

    private Button butLoad;
    private Button butDelete;
    private Button butCustom;
    private Button butMechReadout;
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
    private Panel panBVs;

    private TabPanel panTabs;
    private Panel panMain;

    private Panel panUnits;
    private Panel panTop;

    private Label labStatus;
    private Button butDone;
    
    private Button butAddBot;
    private Button butRemoveBot;
    
    /**
     * Creates a new chat lounge for the client.
     */
    public ChatLounge(ClientGUI clientgui) {
        super();
        this.client = clientgui.getClient();
        this.clientgui = clientgui;

        // Create a tabbed panel to hold our components.
        panTabs = new TabPanel();
        Font tabPanelFont = new Font ("Helvetica",Font.BOLD, //$NON-NLS-1$
                GUIPreferences.getInstance().getChatLoungeTabFontSize());
        panTabs.setTabFont (tabPanelFont);
        
        // Create a new camo selection dialog.
        camoDialog = new CamoChoiceDialog(clientgui.getFrame());

        client.addGameListener(this);
        client.game.board.addBoardListener(this);

        butOptions = new Button(Messages.getString("ChatLounge.butOptions")); //$NON-NLS-1$
        butOptions.addActionListener(this);

        butDone = new Button(Messages.getString("ChatLounge.butDone")); //$NON-NLS-1$
        Font font = null;
        try {
            font = new Font("sanserif", Font.BOLD, 12); //$NON-NLS-1$
        }
        catch (Exception exp) {
            exp.printStackTrace();
        }
        if (null == font) {
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
            addBag (panTabs, gridbag, c);
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

    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
    }

    /**
     * Sets up the player info (team, camo) panel
     */
    private void setupPlayerInfo() {
        Player player = client.getLocalPlayer();

        panPlayerInfo = new Panel();

        labPlayerInfo = new Label(Messages.getString("ChatLounge.labPlayerInfo")); //$NON-NLS-1$

        lisPlayerInfo = new List(5);
        lisPlayerInfo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                butRemoveBot.setEnabled(false);
                Client c = getPlayerListSelected(lisPlayerInfo);
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
		
        labTeam = new Label(Messages.getString("ChatLounge.labTeam"), Label.RIGHT); //$NON-NLS-1$
        labCamo = new Label(Messages.getString("ChatLounge.labCamo"), Label.RIGHT); //$NON-NLS-1$

        choTeam = new Choice();
        choTeam.addItemListener(this);
        setupTeams();

        butCamo = new ImageButton();
        butCamo.setLabel(Messages.getString("ChatLounge.noCamo")); //$NON-NLS-1$
        butCamo.setPreferredSize(84, 72);
        butCamo.setActionCommand("camo"); //$NON-NLS-1$
        butCamo.addActionListener(this);
        camoDialog.addItemListener(
            new CamoChoiceListener(camoDialog, butCamo, butOptions.getBackground(), player.getId(), client));
        refreshCamos();

        // If we have a camo pattern, use it.  Otherwise set a background.
        Image[] images = (Image[]) camoDialog.getSelectedObjects();
        if (null != images) {
            butCamo.setImage(images[0]);
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
        panMinefield = new Panel();

        labMinefield = new Label(Messages.getString("ChatLounge.labMinefield")); //$NON-NLS-1$

        lisMinefield = new List(2);

        labConventional = new Label(Messages.getString("ChatLounge.labConventional"), Label.RIGHT); //$NON-NLS-1$
        labCommandDetonated = new Label(Messages.getString("ChatLounge.labCommandDetonated"), Label.RIGHT); //$NON-NLS-1$
        labVibrabomb = new Label(Messages.getString("ChatLounge.labVibrabomb"), Label.RIGHT); //$NON-NLS-1$

        fldConventional = new TextField(1);
        fldCommandDetonated = new TextField(1);
        fldVibrabomb = new TextField(1);

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
        labBoardSize = new Label(Messages.getString("ChatLounge.labBoardSize"), Label.CENTER); //$NON-NLS-1$
        labMapSize = new Label(Messages.getString("ChatLounge.labMapSize"), Label.CENTER); //$NON-NLS-1$

        lisBoardsSelected = new List(5);
        lisBoardsSelected.addActionListener(this);

        butChangeBoard = new Button(Messages.getString("ChatLounge.butChangeBoard")); //$NON-NLS-1$
        butChangeBoard.setActionCommand("change_board"); //$NON-NLS-1$
        butChangeBoard.addActionListener(this);

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
        labMapSize.setText(Messages.getString("ChatLounge.BoardSize", //$NON-NLS-1$
                new Object[]{new Integer(client.getMapSettings().getMapWidth()),
                new Integer(client.getMapSettings().getMapHeight())}));

        lisBoardsSelected.removeAll();
        int index = 0;
        for (Enumeration i = client.getMapSettings().getBoardsSelected(); i.hasMoreElements();) {
            lisBoardsSelected.add((index++) + ": " + (String) i.nextElement()); //$NON-NLS-1$
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
            this.panTabs.add ("Select Units", panMain); //$NON-NLS-1$
            this.panTabs.add ("Configure", panTop); //$NON-NLS-1$
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

        //          lblPlaceholder = new Label();

        butSaveList = new Button(Messages.getString("ChatLounge.butSaveList")); //$NON-NLS-1$
        butSaveList.setActionCommand("save_list"); //$NON-NLS-1$
        butSaveList.addActionListener(this);
        butSaveList.setEnabled(false);

        butLoad = new Button(Messages.getString("ChatLounge.butLoad")); //$NON-NLS-1$
        MechSummaryCache.addListener(new MechSummaryCache.Listener() {
            public void doneLoading() {
                butLoad.setEnabled(true);
                MechSummaryCache.removeListener(this);
            }
        });
        butLoad.setEnabled(MechSummaryCache.isInitialized());
        Font font = new Font("sanserif", Font.BOLD, 18); //$NON-NLS-1$
        if (null == font) {
            System.err.println("Couldn't find the new font for the 'Add a Unit' button."); //$NON-NLS-1$
        } else {
            butLoad.setFont(font);
        }
        butLoad.setActionCommand("load_mech"); //$NON-NLS-1$
        butLoad.addActionListener(this);

        butCustom = new Button(Messages.getString("ChatLounge.butCustom")); //$NON-NLS-1$
        butCustom.setActionCommand("custom_mech"); //$NON-NLS-1$
        butCustom.addActionListener(this);
        butCustom.setEnabled(false);

        butMechReadout = new Button(Messages.getString("ChatLounge.butMechReadout")); //$NON-NLS-1$
        butMechReadout.setActionCommand("Mech_readout"); //$NON-NLS-1$
        butMechReadout.addActionListener(this);
        butMechReadout.setEnabled(false);

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
        c.gridheight = 2;
        gridbag.setConstraints(butLoad, c);
        panEntities.add(butLoad);

        c.gridheight = 1;
        gridbag.setConstraints(butCustom, c);
        panEntities.add(butCustom);

        gridbag.setConstraints(butMechReadout, c);
        panEntities.add(butMechReadout);

        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butDelete, c);
        panEntities.add(butDelete);

        c.gridwidth = 1;
        c.gridy = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(butLoadList, c);
        panEntities.add(butLoadList);

        //          c.gridwidth = 1;
        //          gridbag.setConstraints( lblPlaceholder, c );
        //          panEntities.add( lblPlaceholder );

        c.gridwidth = 1;
        gridbag.setConstraints(butSaveList, c);
        panEntities.add(butSaveList);

        c.gridwidth = 1;
        gridbag.setConstraints(butDeleteAll, c);
        panEntities.add(butDeleteAll);
    }

    /**
     * Sets up the battle values panel
     */
    private void setupBVs() {
        labBVs = new Label(Messages.getString("ChatLounge.labBVs"), Label.CENTER); //$NON-NLS-1$

        lisBVs = new List(5);

        panBVs = new Panel();

        bvCbg = new CheckboxGroup();
        chkBV = new Checkbox(Messages.getString("ChatLounge.chkBV"), bvCbg, true); //$NON-NLS-1$
        chkBV.addItemListener(this);
        chkTons = new Checkbox(Messages.getString("ChatLounge.chkTons"), bvCbg, false); //$NON-NLS-1$
        chkTons.addItemListener(this);

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
    }

    /**
     * Sets up the starting positions panel
     */
    private void setupStarts() {
        labStarts = new Label(Messages.getString("ChatLounge.labStarts"), Label.CENTER); //$NON-NLS-1$

        lisStarts = new List(5);
        lisStarts.addActionListener(this);

        butChangeStart = new Button(Messages.getString("ChatLounge.butChangeStart")); //$NON-NLS-1$
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
    private void refreshEntities() {
        lisEntities.removeAll();
        int listIndex = 0;
        String strTreeSet = ""; //$NON-NLS-1$
        String strTreeView = ""; //$NON-NLS-1$
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
        Collections.sort( sortedEntities, new Comparator() {
                public int compare( Object aa, Object bb ) {
                    Entity a = (Entity)aa;
                    Entity b = (Entity)bb;
                    Player p_a = (Player)a.getOwner();
                    Player p_b = (Player)b.getOwner();
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
            } );

        for (Iterator i = sortedEntities.iterator(); i.hasNext();) {
            Entity entity = (Entity) i.next();

            // Remember if the local player has units.
            if (!localUnits && entity.getOwner().equals(client.getLocalPlayer())) {
                localUnits = true;
            }

            // Reset the tree strings.
            strTreeSet = ""; //$NON-NLS-1$
            strTreeView = ""; //$NON-NLS-1$

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

            if (!client.game.getOptions().booleanOption("pilot_advantages")) { //$NON-NLS-1$
                entity.getCrew().clearAdvantages();
            }

            int crewAdvCount = entity.getCrew().countAdvantages();

            // Handle the "Blind Drop" option.
            if (!entity.getOwner().equals(client.getLocalPlayer())
                && client.game.getOptions().booleanOption("blind_drop") //$NON-NLS-1$
                && !client.game.getOptions().booleanOption("real_blind_drop")) { //$NON-NLS-1$
                String unitClass = ""; //$NON-NLS-1$
                if (entity instanceof Infantry) {
                    unitClass = Messages.getString("ChatLounge.0"); //$NON-NLS-1$
                } else if (entity instanceof Protomech) {
                    unitClass = Messages.getString("ChatLounge.1"); //$NON-NLS-1$
                } else {
                    int weight = entity.getWeightClass();
                    switch (weight) {
                        case Entity.WEIGHT_LIGHT :
                            unitClass = Messages.getString("ChatLounge.2"); //$NON-NLS-1$
                            break;
                        case Entity.WEIGHT_MEDIUM :
                            unitClass = Messages.getString("ChatLounge.3"); //$NON-NLS-1$
                            break;
                        case Entity.WEIGHT_HEAVY :
                            unitClass = Messages.getString("ChatLounge.4"); //$NON-NLS-1$
                            break;
                        case Entity.WEIGHT_ASSAULT :
                            unitClass = Messages.getString("ChatLounge.5"); //$NON-NLS-1$
                            break;
                    }
                    if (entity instanceof Tank) {
                        unitClass += Messages.getString("ChatLounge.6"); //$NON-NLS-1$
                    }
                }
                lisEntities.add(
                        Messages.getString("ChatLounge.EntityListEntry1", new Object[]{ //$NON-NLS-1$
                                entity.getOwner().getName(),
                                new Integer(entity.getCrew().getGunnery()),
                                new Integer(entity.getCrew().getPiloting()),
                                (crewAdvCount > 0 ? " <" + crewAdvCount + Messages.getString("ChatLounge.advs") : ""), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                unitClass,
                                ((entity.isOffBoard()) ? Messages.getString("ChatLounge.deploysOffBoard") : ""), //$NON-NLS-1$ //$NON-NLS-2$
                                ((entity.getDeployRound() > 0) ? Messages.getString("ChatLounge.deploysAfterRound") + entity.getDeployRound() : "")})); //$NON-NLS-1$ //$NON-NLS-2$
                entityCorrespondance[listIndex++] = entity.getId();
            } else if (entity.getOwner().equals(client.getLocalPlayer())
                       || (!client.game.getOptions().booleanOption("blind_drop") //$NON-NLS-1$
                       && !client.game.getOptions().booleanOption("real_blind_drop"))) { //$NON-NLS-1$
                lisEntities.add(
                    strTreeSet
                    + Messages.getString("ChatLounge.EntityListEntry2", new Object[]{ //$NON-NLS-1$
                            entity.getDisplayName(),
                            new Integer(entity.getCrew().getGunnery()),
                            new Integer(entity.getCrew().getPiloting()),
                            (crewAdvCount > 0 ? " <" + crewAdvCount + Messages.getString("ChatLounge.advs") : ""), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            new Integer(entity.calculateBattleValue()),
                            strTreeView,
                            ((entity.isOffBoard()) ? Messages.getString("ChatLounge.deploysOffBoard") : ""), //$NON-NLS-1$ //$NON-NLS-2$
                            ((entity.getDeployRound() > 0) ? Messages.getString("ChatLounge.deploysAfterRound") + entity.getDeployRound() : "")})); //$NON-NLS-1$ //$NON-NLS-2$
                entityCorrespondance[listIndex++] = entity.getId();
            }
        }

        // Enable the "Save Unit List..." and "Delete All"
        // buttons if the local player has units.
        butSaveList.setEnabled(localUnits);
        butDeleteAll.setEnabled(localUnits);

        // Disable the "must select" buttons.
        butCustom.setEnabled(false);
        butMechReadout.setEnabled(false);
        butDelete.setEnabled(false);
    }

    /**
     * Refreshes the player info
     */
    private void refreshPlayerInfo() {
        lisPlayerInfo.removeAll();
        for (Enumeration i = client.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player) i.nextElement();
            if (player != null) {
                StringBuffer pi = new StringBuffer();
                pi.append(player.getName()).append(" : "); //$NON-NLS-1$
                pi.append(Player.teamNames[player.getTeam()]);

                String plyrCamo = player.getCamoFileName();

                if ((null == plyrCamo) || Player.NO_CAMO.equals(plyrCamo)) {
                    pi.append(", ").append(Player.colorNames[player.getColorIndex()]); //$NON-NLS-1$
                } else {
                    pi.append(", ").append(player.getCamoFileName()); //$NON-NLS-1$
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
        for (Enumeration i = client.getPlayers(); i.hasMoreElements();) {
            final Player player = (Player) i.nextElement();
            if (player != null) {
                StringBuffer pi = new StringBuffer();
                pi.append(player.getName()).append(" : "); //$NON-NLS-1$
                pi.append(player.getNbrMFConventional()).append("/"); //$NON-NLS-1$
                pi.append(player.getNbrMFCommand()).append("/"); //$NON-NLS-1$
                pi.append(player.getNbrMFVibra());

                lisMinefield.add(pi.toString());
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
        final boolean useBv = chkBV.getState();

        lisBVs.removeAll();
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
                	else{
                		playerValue += entity.getWeight();     
                	}
               }
            }
            if (useBv) {
                lisBVs.add(player.getName() + Messages.getString("ChatLounge.BV") + (int) playerValue); //$NON-NLS-1$
            } else {
                lisBVs.add(player.getName() + Messages.getString("ChatLounge.Tons") + playerValue); //$NON-NLS-1$
            }
        }
    }
    
    private void refreshCamos() {
        // Get the local player's selected camo.
        String curCat = client.getLocalPlayer().getCamoCategory();
        String curItem = client.getLocalPlayer().getCamoFileName();

        // If the player has no camo selected, show his color.
        if (null == curItem) {
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
        lisStarts.removeAll();
        for (Enumeration i = client.getPlayers(); i.hasMoreElements();) {
            Player player = (Player) i.nextElement();
            if (player != null) {
                StringBuffer ssb = new StringBuffer();
                ssb.append(player.getName()).append(" : "); //$NON-NLS-1$
                ssb.append(IStartingPositions.START_LOCATION_NAMES[player.getStartingPos()]);
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
     * Refreshes the done button.  The label will say the opposite of the
     * player's "done" status, indicating that clicking it will reverse the
     * condition.
     */
    private void refreshDoneButton(boolean done) {
        butDone.setLabel(done ? Messages.getString("ChatLounge.notDone") : Messages.getString("ChatLounge.imDone")); //$NON-NLS-1$ //$NON-NLS-2$
    }
    private void refreshDoneButton() {
        refreshDoneButton(client.getLocalPlayer().isDone());
    }

    /**
     * Change local player team.
     */
    public void changeTeam(int team) {
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
            AlertDialog ad = new AlertDialog(clientgui.frame, Messages.getString("ChatLounge.MinefieldAlert.title"), Messages.getString("ChatLounge.MinefieldAlert.message")); //$NON-NLS-1$ //$NON-NLS-2$
            ad.show();
            return;
        }

        if (nbrConv < 0 || nbrCmd < 0 || nbrVibra < 0) {
            AlertDialog ad = new AlertDialog(clientgui.frame, Messages.getString("ChatLounge.MinefieldAlert.title"), Messages.getString("ChatLounge.MinefieldAlert.message")); //$NON-NLS-1$ //$NON-NLS-2$
            ad.show();
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
    public void customizeMech() {
        if (lisEntities.getSelectedIndex() == -1) {
            return;
        }
        Entity entity = client.game.getEntity(entityCorrespondance[lisEntities.getSelectedIndex()]);
        boolean editable = clientgui.getBots().get(entity.getOwner().getName()) != null;
		Client c = null;
        if (editable) {
			c = (Client)clientgui.getBots().get(entity.getOwner().getName());
        } else {
            editable |= entity.getOwnerId() == client.getLocalPlayer().getId();
            c = client;
        }
        // When we customize a single entity's C3 network setting,
        // **ALL** members of the network may get changed.
        Entity c3master = entity.getC3Master();
        Vector c3members = new Vector();
        Enumeration playerUnits = c.game.getPlayerEntities(c.getLocalPlayer()).elements();
        while (playerUnits.hasMoreElements()) {
            Entity unit = (Entity) playerUnits.nextElement();
            if (!entity.equals(unit) && entity.onSameC3NetworkAs(unit)) {
                c3members.addElement(unit);
            }
        }

        // display dialog
        CustomMechDialog cmd = new CustomMechDialog(clientgui, c, entity, editable);
        cmd.refreshOptions();
        cmd.show();
        if (editable && cmd.isOkay()) {
            // send changes
            c.sendUpdateEntity(entity);

            // Do we need to update the members of our C3 network?
            if ((c3master != null && !c3master.equals(entity.getC3Master()))
                || (c3master == null && entity.getC3Master() != null)) {
                playerUnits = c3members.elements();
                while (playerUnits.hasMoreElements()) {
                    Entity unit = (Entity) playerUnits.nextElement();
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
        Entity entity = client.game.getEntity(entityCorrespondance[lisEntities.getSelectedIndex()]);
        MechView mechView = new MechView(entity);
        TextArea ta = new TextArea();
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12)); //$NON-NLS-1$
        ta.setText(mechView.getMechReadout());
        final Dialog dialog = new Dialog(clientgui.frame, Messages.getString("ChatLounge.quickView"), false); //$NON-NLS-1$
        Button btn = new Button(Messages.getString("Okay")); //$NON-NLS-1$
        dialog.add(Messages.getString("ChatLounge.South"), btn); //$NON-NLS-1$
        btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        });
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dialog.setVisible(false);
            }
        });
        dialog.add(Messages.getString("ChatLounge.Center"), ta); //$NON-NLS-1$

        // Preview image of the Mech...
        BufferedPanel panPreview = new BufferedPanel();
        panPreview.setPreferredSize(84, 72);
        clientgui.loadPreviewImage(panPreview, entity);
        dialog.add(Messages.getString("ChatLounge.North"), panPreview); //$NON-NLS-1$

        dialog.setLocation(
            clientgui.frame.getLocation().x + clientgui.frame.getSize().width / 2 - dialog.getSize().width / 2,
            clientgui.frame.getLocation().y + clientgui.frame.getSize().height / 5 - dialog.getSize().height / 2);
        dialog.setSize(300, 450);

        dialog.validate();
        dialog.show();
    }

    /**
     * Pop up the dialog to load a mech
     */
    public void loadMech() {
        clientgui.getMechSelectorDialog().show();
    }

    //
    // GameListener
    //
    public void gamePlayerStatusChange(GameEvent ev) {
        // Are we ignoring events?
        if (this.isIgnoringEvents()) {
            return;
        }
        refreshDoneButton();
        refreshBVs();
        refreshPlayerInfo();
        refreshStarts();
        refreshCamos();
        refreshMinefield();
    }
    public void gamePhaseChange(GameEvent ev) {
        // Are we ignoring events?
        if (this.isIgnoringEvents()) {
            return;
        }
        if (client.game.getPhase() == Game.PHASE_LOUNGE) {
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
    public void gameNewEntities(GameEvent ev) {
        // Are we ignoring events?
        if (this.isIgnoringEvents()) {
            return;
        }
        refreshEntities();
        refreshBVs();
    }
    public void gameNewSettings(GameEvent ev) {
        // Are we ignoring events?
        if (this.isIgnoringEvents()) {
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
        if (this.isIgnoringEvents()) {
            return;
        }

        if (ev.getSource() == choTeam) {
            changeTeam(choTeam.getSelectedIndex());
        } else if (ev.getSource() == chkBV || ev.getSource() == chkTons) {
            refreshBVs();
        } else if (ev.getSource() == lisEntities) {
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
        }

    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {

        // Are we ignoring events?
        if (this.isIgnoringEvents()) {
            return;
        }

        if (ev.getSource() == butDone) {
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
                ((Client)i.next()).sendDone(done);
            }
        } else if (ev.getSource() == butLoad) {
            loadMech();
        } else if (ev.getSource() == butCustom || ev.getSource() == lisEntities) {
            customizeMech();
        } else if (ev.getSource() == butDelete) {
            // delete mech
			Entity e = client.getEntity(entityCorrespondance[lisEntities.getSelectedIndex()]);
            Client c = (Client)clientgui.getBots().get(e.getOwner().getName());
			if (c == null) {
			    c = client;
			}
            if (lisEntities.getSelectedIndex() != -1) {
                c.sendDeleteEntity(entityCorrespondance[lisEntities.getSelectedIndex()]);
            }
        } else if (ev.getSource() == butDeleteAll) {
            // Build a Vector of this player's entities.
            Vector currentUnits = client.game.getPlayerEntities(client.getLocalPlayer());

            // Walk through the vector, deleting the entities.
            Enumeration entities = currentUnits.elements();
            while (entities.hasMoreElements()) {
                final Entity entity = (Entity) entities.nextElement();
                client.sendDeleteEntity(entity.getId());
            }
        } else if (ev.getSource() == butChangeBoard || ev.getSource() == lisBoardsSelected) {
            // board settings
            clientgui.getBoardSelectionDialog().update(client.getMapSettings(), true);
            clientgui.getBoardSelectionDialog().show();
        } else if (ev.getSource() == butOptions) {
            // Make sure the game options dialog is editable.
            if (!clientgui.getGameOptionsDialog().isEditable()) {
                clientgui.getGameOptionsDialog().setEditable(true);
            }
            // Display the game options dialog.
            clientgui.getGameOptionsDialog().update(client.game.getOptions());
            clientgui.getGameOptionsDialog().show();
        } else if (ev.getSource() == butChangeStart || ev.getSource() == lisStarts) {
            clientgui.getStartingPositionDialog().update();
            Client c = getPlayerListSelected(lisStarts);
            if (c == null) {
				clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"), Messages.getString("ChatLounge.SelectBotOrPlayer")); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
			clientgui.getStartingPositionDialog().setClient(c);
            clientgui.getStartingPositionDialog().show();
        } else if (ev.getSource() == butMechReadout) {
            mechReadout();
        } else if (ev.getSource() == butLoadList) {
            // Allow the player to replace their current
            // list of entities with a list from a file.
            clientgui.loadListFile();
        } else if (ev.getSource() == butSaveList) {
            // Allow the player to save their current
            // list of entities to a file.
            clientgui.saveListFile(client.game.getPlayerEntities(client.getLocalPlayer()));
        } else if (ev.getSource() == butMinefield) {
            updateMinefield();
        } else if (ev.getSource() == butCamo) {
            camoDialog.show();
        } else if (ev.getSource() == butAddBot) {
            String name = name = "Bot" + lisPlayerInfo.getItemCount(); //$NON-NLS-1$
            Prompt p = new Prompt(clientgui.frame, Messages.getString("ChatLounge.ChooseBotName"), Messages.getString("ChatLounge.Name"), name, 15); //$NON-NLS-1$ //$NON-NLS-2$
            if (!p.showDialog()){
                return;
            }
            if (p.getText().trim().equals("")) { //$NON-NLS-1$
                name = "Bot" + lisPlayerInfo.getItemCount(); //$NON-NLS-1$
            } else {
                name = p.getText();
            }
			BotClient c = new TestBot(name, client.getHost(), client.getPort());
			c.addGameListener(new BotGUI(c));
			try {
				c.connect();
			} catch (Exception e) {
				clientgui.doAlertDialog(Messages.getString("ChatLounge.AlertBot.title"), Messages.getString("ChatLounge.AlertBot.message")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			c.retrieveServerInfo();
			clientgui.getBots().put(name, c);
        } else if (ev.getSource() == butRemoveBot) {
            Client c = getPlayerListSelected(lisPlayerInfo);
            if (c == null || c == client) {
				clientgui.doAlertDialog(Messages.getString("ChatLounge.ImproperCommand"), Messages.getString("ChatLounge.SelectBo")); //$NON-NLS-1$ //$NON-NLS-2$
				return;
            } 
            c.die();
            clientgui.getBots().remove(c.getName());
        }
    }
    
    private Client getPlayerListSelected(List l) {
        if (l.getSelectedIndex() == -1) {
            return client;
        }
		String name = l.getSelectedItem().substring(0, Math.max(0,l.getSelectedItem().indexOf(" :"))); //$NON-NLS-1$
	    BotClient c = (BotClient)clientgui.getBots().get(name);
	    if (c == null && client.getName().equals(name)) {
	        return client;
	    }
	    return c;
    }
    
    /**
     * Determine if the listener is currently distracted.
     *
     * @return  <code>true</code> if the listener is ignoring events.
     */
    public boolean isIgnoringEvents() {
        return this.distracted.isIgnoringEvents();
    }

    /**
     * Specify if the listener should be distracted.
     *
     * @param   distract <code>true</code> if the listener should ignore events
     *          <code>false</code> if the listener should pay attention again.
     *          Events that occured while the listener was distracted NOT
     *          going to be processed.
     */
    public void setIgnoringEvents(boolean distracted) {
        this.distracted.setIgnoringEvents(distracted);
    }

    /**
     * Retrieve the "Done" button of this object.
     *
     * @return  the <code>java.awt.Button</code> that activates this
     *          object's "Done" action.
     */
    public Button getDoneButton() {
        return butDone;
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        client.removeGameListener(this);
        client.game.board.removeBoardListener(this);
    }

    /**
     * Get the secondary display section of this phase.
     *
     * @return  the <code>Component</code> which is displayed in the
     *          secondary section during this phase.
     */
    public Component getSecondaryDisplay() {
        return labStatus;
    }

}
