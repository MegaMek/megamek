/*
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

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Enumeration;
import java.io.*;
import megamek.client.util.widget.*;

import megamek.common.*;

public class ChatLounge extends AbstractPhaseDisplay
    implements ActionListener, ItemListener, BoardListener, GameListener
{
    public static final String START_LOCATION_NAMES[] = {"Any", "NW", "N", "NE", "E", "SE", "S", "SW", "W"};
    
    // parent Client
    private Client client;
        
    // buttons & such
    private Panel panPlayerInfo;
    private Label labPlayerInfo;
    private List lisPlayerInfo;
    private Label labColor;
    private Label labTeam;            
    private Choice choColor;
    private Choice choTeam;
    
    private Label labCamo;
    private Choice choCamo;
    
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
    private Label  lblPlaceholder;
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
      
    private Panel panMain;
    
    private Panel panUnits;
    private Panel panTop;
        
    private Label labStatus;
    private Button butDone;
    
    /**
     * Creates a new chat lounge for the client.
     */
    public ChatLounge(Client client) {
        super();
        this.client = client;
            
        client.addGameListener(this);
        client.game.board.addBoardListener(this);

        butOptions = new Button("Game Options...");
        butOptions.addActionListener(this);
        
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
        
        labStatus = new Label("", Label.CENTER);
                
        butDone = new Button("I'm Done");
        butDone.setActionCommand("ready");
        butDone.addActionListener(this);
                
        // layout main thing
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
                
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;    c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(panMain, gridbag, c);

        c.weightx = 1.0;    c.weighty = 0.0;
        addBag(labStatus, gridbag, c);

        c.gridwidth = 1;
        c.weightx = 1.0;    c.weighty = 0.0;
        addBag(client.cb.getComponent(), gridbag, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;    c.weighty = 0.0;
        addBag(butDone, gridbag, c);
        
        validate();
    }
    
    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
    }
    
    /**
     * Sets up the player info (color, team) panel
     */
    private void setupPlayerInfo() {
        panPlayerInfo = new Panel();
        
        labPlayerInfo = new Label("Player Setup");
        
        lisPlayerInfo = new List(5);
        
        labColor = new Label("Color:", Label.RIGHT);
        labTeam = new Label("Team:", Label.RIGHT);
        labCamo = new Label("Camo:", Label.RIGHT);
                
        choColor = new Choice();
        choColor.addItemListener(this);
        setupColors();
                
        choTeam = new Choice();
        choTeam.addItemListener(this);
        setupTeams();
        
        choCamo = new Choice();
        choCamo.addItemListener(this);
        setupCamos();
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panPlayerInfo.setLayout(gridbag);
            
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labPlayerInfo, c);
        panPlayerInfo.add(labPlayerInfo);
        
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;    c.weighty = 1.0;
        gridbag.setConstraints(lisPlayerInfo, c);
        panPlayerInfo.add(lisPlayerInfo);

        c.gridwidth = 1;
        c.weightx = 0.0;    c.weighty = 0.0;
        gridbag.setConstraints(labColor, c);
        panPlayerInfo.add(labColor);
            
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;    c.weighty = 0.0;
        gridbag.setConstraints(choColor, c);
        panPlayerInfo.add(choColor);
            
        c.gridwidth = 1;
        c.weightx = 0.0;    c.weighty = 0.0;
        gridbag.setConstraints(labTeam, c);
        panPlayerInfo.add(labTeam);
            
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;    c.weighty = 0.0;
        gridbag.setConstraints(choTeam, c);
        panPlayerInfo.add(choTeam);
        
        c.gridwidth = 1;
        c.weightx = 0.0;    c.weighty = 0.0;
        gridbag.setConstraints(labCamo, c);
        panPlayerInfo.add(labCamo);
            
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;    c.weighty = 0.0;
        gridbag.setConstraints(choCamo, c);
        panPlayerInfo.add(choCamo);

        refreshPlayerInfo();
    }
  
  
    /**
     * Sets up the minefield panel
     */
    private void setupMinefield() {
		panMinefield = new Panel();
		
		labMinefield = new Label("Minefields");
		
		lisMinefield = new List(4);
		
		labConventional = new Label("Conventional:", Label.RIGHT);
		labCommandDetonated = new Label("Command-detonated:", Label.RIGHT);
		labVibrabomb = new Label("Vibrabomb:", Label.RIGHT);

		fldConventional = new TextField(1);
		fldCommandDetonated = new TextField(1);
		fldVibrabomb = new TextField(1);
		
		butMinefield = new Button("Update");
		butMinefield.addActionListener(this);

		enableMinefields(client.game.getOptions().booleanOption("minefields"));

		// layout
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		panMinefield.setLayout(gridbag);
		    
		c.fill = GridBagConstraints.VERTICAL;
		c.insets = new Insets(1, 1, 1, 1);
		c.weightx = 1.0;    c.weighty = 0.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(labMinefield, c);
		panMinefield.add(labMinefield);
		
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;    c.weighty = 1.0;
		gridbag.setConstraints(lisMinefield, c);
		panMinefield.add(lisMinefield);
		
		c.gridwidth = 1;
		c.weightx = 0.0;    c.weighty = 0.0;
		gridbag.setConstraints(labConventional, c);
		panMinefield.add(labConventional);
		    
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;    c.weighty = 0.0;
		gridbag.setConstraints(fldConventional, c);
		panMinefield.add(fldConventional);
		    
		c.gridwidth = 1;
		c.weightx = 0.0;    c.weighty = 0.0;
		gridbag.setConstraints(labCommandDetonated, c);
		panMinefield.add(labCommandDetonated);
		    
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;    c.weighty = 0.0;
		gridbag.setConstraints(fldCommandDetonated, c);
		panMinefield.add(fldCommandDetonated);
		
		c.gridwidth = 1;
		c.weightx = 0.0;    c.weighty = 0.0;
		gridbag.setConstraints(labVibrabomb, c);
		panMinefield.add(labVibrabomb);
		    
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;    c.weighty = 0.0;
		gridbag.setConstraints(fldVibrabomb, c);
		panMinefield.add(fldVibrabomb);
		
		c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.0;    c.weighty = 0.0;
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
        labBoardSize = new Label("Board Size: # x # hexes", Label.CENTER);
        labMapSize = new Label("Map Size: # x # boards", Label.CENTER);
        
        lisBoardsSelected = new List(5);
        lisBoardsSelected.addActionListener(this);
            
        butChangeBoard = new Button("Edit / View Map...");
        butChangeBoard.setActionCommand("change_board");
        butChangeBoard.addActionListener(this);
            
        panBoardSettings = new Panel();
            
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panBoardSettings.setLayout(gridbag);
            
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labBoardSize, c);
        panBoardSettings.add(labBoardSize);
            
        gridbag.setConstraints(labMapSize, c);
        panBoardSettings.add(labMapSize);
            
        c.weightx = 1.0;    c.weighty = 1.0;
        gridbag.setConstraints(lisBoardsSelected, c);
        panBoardSettings.add(lisBoardsSelected);
            
        c.weightx = 1.0;    c.weighty = 0.0;
        gridbag.setConstraints(butChangeBoard, c);
        panBoardSettings.add(butChangeBoard);
     
        refreshBoardSettings();
    }
    
    private void refreshBoardSettings() {
        labBoardSize.setText("Board Size: " + client.getMapSettings().getBoardWidth() + " x " + client.getMapSettings().getBoardHeight() + " hexes");
        labMapSize.setText("Map Size: " + client.getMapSettings().getMapWidth() + " x " + client.getMapSettings().getMapHeight() + " boards");
        lisBoardsSelected.removeAll();
        int index = 0;
        for (Enumeration i = client.getMapSettings().getBoardsSelected(); i.hasMoreElements();) {
            lisBoardsSelected.add((index++) + ": " + (String)i.nextElement());
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
        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butOptions, c);
        panMain.add(butOptions);
            
        c.weightx = 1.0;    c.weighty = 1.0;
        gridbag.setConstraints(panTop, c);
        panMain.add(panTop);
            
        gridbag.setConstraints(panUnits, c);
        panMain.add(panUnits);
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
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;    c.weighty = 1.0;
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

        butLoadList = new Button("Load Unit List...");
        butLoadList.setActionCommand("load_list");
        butLoadList.addActionListener(this);

        lblPlaceholder = new Label();

        butSaveList = new Button("Save Unit List...");
        butSaveList.setActionCommand("save_list");
        butSaveList.addActionListener(this);
        butSaveList.setEnabled(false);

        butLoad = new Button("Add A Mech...");
        butLoad.setActionCommand("load_mech");
        butLoad.addActionListener(this);

        butCustom = new Button("Configure Mech...");
        butCustom.setActionCommand("custom_mech");
        butCustom.addActionListener(this);
        butCustom.setEnabled(false);
            
        butMechReadout = new Button("View Mech...");
        butMechReadout.setActionCommand("Mech_readout");
        butMechReadout.addActionListener(this);
        butMechReadout.setEnabled(false);
            
        butDelete = new Button("Delete Mech");
        butDelete.setActionCommand("delete_mech");
        butDelete.addActionListener(this);
        butDelete.setEnabled(false);
            
        butDeleteAll = new Button("Delete All");
        butDeleteAll.setActionCommand("delete_all");
        butDeleteAll.addActionListener(this);
        butDeleteAll.setEnabled(false);

        panEntities = new Panel();
            
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panEntities.setLayout(gridbag);
            
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;    c.weighty = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(lisEntities, c);
        panEntities.add(lisEntities);
            
        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = 1;
        gridbag.setConstraints(butLoad, c);
        panEntities.add(butLoad);
            
        gridbag.setConstraints(butCustom, c);
        panEntities.add(butCustom);
            
        gridbag.setConstraints(butMechReadout, c);
        panEntities.add(butMechReadout);
            
        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butDelete, c);
        panEntities.add(butDelete);

        c.gridwidth = 1;
        c.gridy = GridBagConstraints.RELATIVE;
        gridbag.setConstraints( butLoadList, c );
        panEntities.add( butLoadList );

        c.gridwidth = 1;
        gridbag.setConstraints( lblPlaceholder, c );
        panEntities.add( lblPlaceholder );

        c.gridwidth = 1;
        gridbag.setConstraints( butSaveList, c );
        panEntities.add( butSaveList );

        c.gridwidth = 1;
        gridbag.setConstraints( butDeleteAll, c );
        panEntities.add( butDeleteAll );
    }
    
    /**
     * Sets up the battle values panel
     */
    private void setupBVs() {
        labBVs = new Label("Total Battle Values", Label.CENTER);

        lisBVs = new List(5);
            
        panBVs = new Panel();
            
        bvCbg = new CheckboxGroup();
        chkBV = new Checkbox("BV", bvCbg, true);
        chkBV.addItemListener(this);
        chkTons = new Checkbox("Tons", bvCbg, false);
        chkTons.addItemListener(this);
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panBVs.setLayout(gridbag);
            
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labBVs, c);
        panBVs.add(labBVs);
            
        c.weightx = 1.0;    c.weighty = 1.0;
        gridbag.setConstraints(lisBVs, c);
        panBVs.add(lisBVs);
        
        c.weightx = 1.0;    c.weighty = 1.0;
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
        labStarts = new Label("Starting Positions", Label.CENTER);
        
        lisStarts = new List(5);
        lisStarts.addActionListener(this);

        butChangeStart = new Button("Change Start...");
        butChangeStart.addActionListener(this);
        
        panStarts = new Panel();
            
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panStarts.setLayout(gridbag);
            
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labStarts, c);
        panStarts.add(labStarts);
            
        c.weightx = 1.0;    c.weighty = 1.0;
        gridbag.setConstraints(lisStarts, c);
        panStarts.add(lisStarts);
        
        c.weightx = 1.0;    c.weighty = 0.0;
        gridbag.setConstraints(butChangeStart, c);
        panStarts.add(butChangeStart);
    }

    /**
     * Refreshes the game settings with new info from the client
     */
    private void refreshGameSettings() {
    }
  
    /**
     * Refreshes the entities from the client
     */
    private void refreshEntities() {
        lisEntities.removeAll();
        int listIndex = 0;
        String strTreeSet = "";
        String strTreeView = "";
        boolean localUnits = false;
        entityCorrespondance = new int[client.game.getNoOfEntities()];
        for (Enumeration i = client.getEntities(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();

            // Remember if the local player has units.
            if ( !localUnits &&
                 entity.getOwner().equals(client.getLocalPlayer()) ) {
                localUnits = true;
            }

            if(entity.hasC3i()) {
                strTreeSet = "";
                if(entity.calculateFreeC3Nodes() == 5) strTreeSet = "**";
                strTreeView = " (" + entity.getC3NetId() + ")";
            }
            else if(entity.hasC3()) {
                if(entity.getC3Master() == null) {
                    if(entity.hasC3S())
                        strTreeSet = "***";
                    else
                        strTreeSet = "*";
                    strTreeView = "";
                }
                else {
                    strTreeSet = "";
                    if(!entity.C3MasterIs(entity)) {
                        strTreeSet = ">";
                        if(entity.getC3Master().getC3Master() != null && !entity.getC3Master().C3MasterIs(entity.getC3Master()))
                            strTreeSet = ">>";
                        strTreeView = " -> " + entity.getC3Master().getDisplayName();
                    }
                }
            }
            else {
                strTreeSet = "";
                strTreeView = "";
            }            

            if ( !client.game.getOptions().booleanOption("pilot_advantages") ) {
              entity.getCrew().clearAdvantages();
            }
            
            int crewAdvCount = entity.getCrew().countAdvantages();
            
            // Handle the "Blind Drop" option.
            if ( !entity.getOwner().equals(client.getLocalPlayer()) &&
                 client.game.getOptions().booleanOption("blind_drop") )
            {
                int weigth = entity.getWeightClass();
                String weight = "";
                switch (weigth)
                {
                    case Entity.WEIGHT_LIGHT   : weight = "Light";   break;
                    case Entity.WEIGHT_MEDIUM  : weight = "Medium";  break;
                    case Entity.WEIGHT_HEAVY   : weight = "Heavy";   break;
                    case Entity.WEIGHT_ASSAULT : weight = "Assault"; break;
                }
            
                lisEntities.add(entity.getOwner().getName() 
                + " (" + entity.getCrew().getGunnery()
                + "/" + entity.getCrew().getPiloting() + " pilot" + (crewAdvCount > 0 ? " <" + crewAdvCount + " advs>" : "") + ")"
                + " Class: " + weight
                + ((entity.getDeployRound() > 0) ? " - Deploy after round " + entity.getDeployRound() : ""));
            }
            else
            {
                lisEntities.add(strTreeSet + entity.getDisplayName()
                + " (" + entity.getCrew().getGunnery()
                + "/" + entity.getCrew().getPiloting() + " pilot" + (crewAdvCount > 0 ? " <" + crewAdvCount + " advs>" : "") + ")"
                + " BV=" + entity.calculateBattleValue() + strTreeView
                + ((entity.getDeployRound() > 0) ? " - Deploy after round " + entity.getDeployRound() : ""));
            }
            
            
            entityCorrespondance[listIndex++] = entity.getId();
        }

        // Enable the "Save Unit List..." and "Delete All"
        // buttons if the local player has units.
        butSaveList.setEnabled( localUnits );
        butDeleteAll.setEnabled( localUnits );

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
            final Player player = (Player)i.nextElement();
            if(player != null) {
              StringBuffer pi = new StringBuffer();
              pi.append(player.getName()).append(" : ");
              pi.append(Player.teamNames[player.getTeam()]);

              String plyrCamo = player.getCamoFileName();
                
              if ( (null == plyrCamo) || Player.NO_CAMO.equals(plyrCamo) ) {
                pi.append(", ").append(Player.colorNames[player.getColorIndex()]);
              } else {
                pi.append(", ").append(player.getCamoFileName());
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
            final Player player = (Player)i.nextElement();
            if(player != null) {
              StringBuffer pi = new StringBuffer();
              pi.append(player.getName()).append(" : ");
              pi.append(player.getNbrMFConventional()).append("/");
              pi.append(player.getNbrMFCommand()).append("/");
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
            final Player player = (Player)i.nextElement();
            if(player == null) {
                continue;
            }
            float playerValue = 0;
            for (Enumeration j = client.getEntities(); j.hasMoreElements();) {
                Entity entity = (Entity)j.nextElement();
                if (entity.getOwner().equals(player)) {
                    if (useBv) {
                        playerValue += entity.calculateBattleValue();
                    } else {
                        playerValue += entity.getWeight();
                    }
                }
            }
            if (useBv) {
                lisBVs.add(player.getName() + " BV=" + (int)playerValue);
            } else {
                lisBVs.add(player.getName() + " Tons=" + playerValue);
            }
        }
    }
    
    private void setupCamos() {
        choCamo.removeAll();
        
        Vector camoList = getCamoList();

        for (int i = 0; i < camoList.size(); i++) {
          choCamo.add((String)camoList.elementAt(i));
        }
        
        String selCamo = client.getLocalPlayer().getCamoFileName();
        
        if ( (null == selCamo) || (selCamo.equals(Player.NO_CAMO)) )
          choCamo.select(Player.NO_CAMO);
        else
          choCamo.select(client.getLocalPlayer().getCamoFileName());
    }
    
    private void refreshCamos() {
      String selCamo = client.getLocalPlayer().getCamoFileName();
      
      if ( (null == selCamo) || (selCamo.equals(Player.NO_CAMO)) )
        choCamo.select(Player.NO_CAMO);
      else
        choCamo.select(client.getLocalPlayer().getCamoFileName());
    }

    public static Vector getCamoList() {
      Vector camoList = new Vector();
      
      camoList.addElement(Player.NO_CAMO);

      File camoLib = new File("data/camo");
      String[] fileList = camoLib.list();
      for (int i = 0; i < fileList.length; i++) {
        if (fileList[i].endsWith(".jpg")) {
          camoList.addElement(fileList[i].substring(0, fileList[i].indexOf(".jpg")));
        }
      }

      return camoList;
    }
    
    /**
     * Refreshes the starting positions
     */
    private void refreshStarts() {
        lisStarts.removeAll();
        for (Enumeration i = client.getPlayers(); i.hasMoreElements();) {
            Player player = (Player)i.nextElement();
            if(player != null) {
                StringBuffer ssb = new StringBuffer();
                ssb.append(player.getName()).append(" : ");
                ssb.append(START_LOCATION_NAMES[player.getStartingPos()]);
                lisStarts.add(ssb.toString());
            }
        }
    }
    
    /**
     * Setup the color choice box
     */
    private void setupColors() {
        choColor.removeAll();
        for (int i = 0; i < Player.colorNames.length; i++) {
            choColor.add(Player.colorNames[i]);
        }
        choColor.select(Player.colorNames[client.getLocalPlayer().getColorIndex()]);
    }
  
    /**
     * Refresh the color choice box
     */
    private void refreshColors() {
        choColor.select(Player.colorNames[client.getLocalPlayer().getColorIndex()]);
    }
  
    /**
     * Setup the team choice box
     */
    private void setupTeams() {
        choTeam.removeAll();
        for (int i = 0; i < Player.MAX_TEAMS; i++) {
            choTeam.add(Player.teamNames[i]);
        }
        choTeam.select(client.getLocalPlayer().getTeam());
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
        butDone.setLabel(done ? "Not Done" : "I'm Done");
    }
    private void refreshDoneButton() {
        refreshDoneButton(client.getLocalPlayer().isDone());
    }
    
    /**
     * Change local player color.
     */
    public void changeColor(int nc) {
        if (client.getLocalPlayer().getColorIndex() != nc) {
            client.getLocalPlayer().setColorIndex(nc);
            client.sendPlayerInfo();
        }
    }
  
    /**
     * Change local player team.
     */
    public void changeTeam(int team) {
        if (client.getLocalPlayer().getTeam() != team) {
            client.getLocalPlayer().setTeam(team);
            client.sendPlayerInfo();
        }
    }
    
    public void changeCamo(String camo) {
      String curCamo = client.getLocalPlayer().getCamoFileName();
        if ((camo == null && curCamo != null)
            || (camo != null && !camo.equals(curCamo))) {
      client.getLocalPlayer().setCamoFileName(camo);
      client.sendPlayerInfo();
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
			AlertDialog ad = new AlertDialog(client.frame,
											"Minefield",
											"Only positive integers allowed");
			ad.show();
			return;
		}
		
		if (nbrConv < 0 || nbrCmd < 0 || nbrVibra < 0) {
			AlertDialog ad = new AlertDialog(client.frame,
											"Minefield",
											"Only positive integers allowed");
			ad.show();
			return;
		}
		client.getLocalPlayer().setNbrMFConventional(nbrConv);
		client.getLocalPlayer().setNbrMFCommand(nbrCmd);
		client.getLocalPlayer().setNbrMFVibra(nbrVibra);
		client.sendPlayerInfo();
	}

    /**
     * Pop up the customize mech dialog
     */
    public void customizeMech() {
        if (lisEntities.getSelectedIndex() == -1) {
            return;
        }
        Entity entity = client.game.getEntity(entityCorrespondance[lisEntities.getSelectedIndex()]);
        boolean editable = entity.getOwnerId() == client.getLocalPlayer().getId();
        // display dialog
        CustomMechDialog cmd = new CustomMechDialog(client, entity, editable);
        cmd.refreshOptions();
        cmd.show();
        if (editable && cmd.isOkay()) {
            // send changes
            client.sendUpdateEntity(entity);
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
  ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
  ta.setText(mechView.getMechReadout());
        final Dialog dialog = new Dialog(client.frame, "Mech Quick View", false);
        Button btn = new Button("Ok");
        dialog.add("South", btn);
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
        dialog.add("Center", ta);
        
        // Preview image of the Mech...
		BufferedPanel panPreview = new BufferedPanel();
		panPreview.setPreferredSize(84, 72);
		client.loadPreviewImage(panPreview, entity);
        dialog.add("North", panPreview);

        dialog.setLocation(client.frame.getLocation().x + client.frame.getSize().width/2 - dialog.getSize().width/2,
                    client.frame.getLocation().y + client.frame.getSize().height/5 - dialog.getSize().height/2);
        dialog.setSize(300, 450);

		dialog.validate();
        dialog.show();
    }
    
    /**
     * Pop up the dialog to load a mech
     */
    public void loadMech() throws Exception {
        client.mechSelectorDialogThread.join();
        client.getMechSelectorDialog().show();
    }
  
    //
    // GameListener
    //
    public void gamePlayerStatusChange(GameEvent ev) {
        refreshDoneButton();
        refreshBVs();
        refreshPlayerInfo();
        refreshStarts();
        refreshColors();
        refreshCamos();
        refreshMinefield();
    }
    public void gamePhaseChange(GameEvent ev) {
        if (client.game.getPhase() !=  Game.PHASE_LOUNGE) {
            // unregister stuff.
            client.removeGameListener(this);
            client.game.board.removeBoardListener(this);
        }
    }
    public void gameNewEntities(GameEvent ev) {
        refreshEntities();
        refreshBVs();
    }
    public void gameNewSettings(GameEvent ev) {
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
        if (ev.getSource() == choColor) {
            changeColor(choColor.getSelectedIndex());
        } else if (ev.getSource() == choTeam) {
            changeTeam(choTeam.getSelectedIndex());
        } else if (ev.getSource() == choCamo) {
          if (choCamo.getSelectedIndex() != 0) {
            changeCamo(choCamo.getSelectedItem());
          } else {
            changeCamo(null);
          }
        } else if (ev.getSource() == chkBV || ev.getSource() == chkTons) {
            refreshBVs();
        } else if (ev.getSource() == lisEntities) {
            boolean selected = lisEntities.getSelectedIndex() != -1;
            butCustom.setEnabled(selected);

            // Handle "Blind drop" option.
            if ( selected &&
                 client.game.getOptions().booleanOption("blind_drop") ) {
                Entity entity = client.game.getEntity
                    ( entityCorrespondance[lisEntities.getSelectedIndex()] );
                butMechReadout.setEnabled
                    ( entity.getOwner().equals(client.getLocalPlayer()) );
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
        if (ev.getSource() == butDone) {
          boolean anyDelayed = false;
          
          Enumeration enum = client.getEntities();
          
          while ( enum.hasMoreElements() ) {
            Entity en = (Entity)enum.nextElement();
            
            if ( en.getDeployRound() > 0 ) {
              anyDelayed = true;
              break;
            }
          }
          
          if ( anyDelayed && (client.getLocalPlayer().getStartingPos() == 0) ) {
            new AlertDialog(client.frame, "Need deployment", "Players with delayed deployment can not use the 'any' starting position. Please select a direction to start from.").show();
            return;
          }
           
            boolean done = !client.getLocalPlayer().isDone();
            client.sendDone(done);
            refreshDoneButton(done);
        } else if (ev.getSource() == butLoad) {
            try {
                loadMech();
            } catch (Exception e) {
                System.out.println("Thread.join() exception in ChatLounge.loadMech()");
            }
        } else if (ev.getSource() == butCustom ||  ev.getSource() == lisEntities) {
            customizeMech();
        } else if (ev.getSource() == butDelete) {
            // delete mech
            if (lisEntities.getSelectedIndex() != -1) {
                client.sendDeleteEntity(entityCorrespondance[lisEntities.getSelectedIndex()]);
            }
        } else if (ev.getSource() == butDeleteAll) {
            // Build a Vector of this player's entities.
            Vector currentUnits = client.game.getPlayerEntities
                ( client.getLocalPlayer() );

            // Walk through the vector, deleting the entities.
            Enumeration entities = currentUnits.elements();
            while ( entities.hasMoreElements() ) {
                final Entity entity = (Entity) entities.nextElement();
                client.sendDeleteEntity( entity.getId() );
            }
        } else if (ev.getSource() == butChangeBoard || ev.getSource() == lisBoardsSelected) {
            // board settings
            client.getBoardSelectionDialog().update(client.getMapSettings(), true);
            client.getBoardSelectionDialog().show();
        } else if (ev.getSource() == butOptions) {
            // game options
            client.getGameOptionsDialog().update(client.game.getOptions());
            client.getGameOptionsDialog().show();
        } else if (ev.getSource() == butChangeStart || ev.getSource() == lisStarts) {
            client.getStartingPositionDialog().update();
            client.getStartingPositionDialog().show();
        } else if (ev.getSource() == butMechReadout) {
            mechReadout();
        }
        else if ( ev.getSource() == butLoadList ) {
            // Allow the player to replace their current
            // list of entities with a list from a file.
            client.loadListFile();
        }
        else if ( ev.getSource() == butSaveList ) {
            // Allow the player to save their current
            // list of entities to a file.
            client.saveListFile
                ( client.game.getPlayerEntities(client.getLocalPlayer()) );
        } else if (ev.getSource() == butMinefield) {
        	updateMinefield();
        }
    }

}
