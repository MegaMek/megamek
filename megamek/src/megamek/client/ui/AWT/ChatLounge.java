/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

import megamek.common.*;

public class ChatLounge extends AbstractPhaseDisplay
    implements ActionListener, ItemListener, BoardListener, GameListener
{
    private static final String startNames[] = {"NW", "N", "NE", "E", "SE", "S", "SW", "W"};
    
    // parent Client
    private Client client;
        
    // buttons & such
    private Label labColor, labTeam;            
    private Choice choColor, choTeam;
    private Panel panColor;
      
    private Label labBoardSize;
    private Label labMapSize;
    private List lisBoardsSelected;
    private Button butChangeBoard;
    private Panel panBoardSettings;
      
    private Button butLoad;
    private Button butDelete;
    private Button butCustom;
    private List lisEntities;
    private int[] entityCorrespondance;
    private Panel panEntities;
    
    private Label labStarts;
    private Button[] butPos;
    private Label labMiddlePosition;
    private Panel panStartButtons;
    private List lisStarts;
    private Panel panStarts;
      
    private Label labBVs;
    private List lisBVs;
    private CheckboxGroup bvCbg;
    private Checkbox chkBV;
    private Checkbox chkTons;
    private Panel panBVs;
      
    private Panel panMain;
        
    private Label labStatus;
    private Button butReady;
    
    /**
     * Creates a new chat lounge for the client.
     */
    public ChatLounge(Client client) {
        super();
        this.client = client;
            
        client.addGameListener(this);
        client.game.board.addBoardListener(this);
                
        ChatterBox cb = client.cb;
                
        panColor = new Panel();
            
        labColor = new Label("Color:", Label.RIGHT);
        labTeam = new Label("Team:", Label.RIGHT);
                
        choColor = new Choice();
        choColor.addItemListener(this);
                
        setupColors();
                
        choTeam = new Choice();
        choTeam.addItem("Not Functional");
        choTeam.setEnabled(false);
            
        setupBoardSettings();
        refreshGameSettings();
            
        setupEntities();
        refreshEntities();
            
        setupBVs();
        refreshBVs();
        
        setupStarts();
        refreshStarts();
            
        panMain = new Panel();
        panMain.setLayout(new GridBagLayout());
            
        panMain.add(panBoardSettings);
        panMain.add(panStarts);
        panMain.add(panEntities);
        panMain.add(panBVs);
            
        labStatus = new Label("", Label.CENTER);
                
        butReady = new Button("I'm Ready.");
        butReady.setActionCommand("ready");
        butReady.addActionListener(this);
                
        // layout colors
        panColor.add(labColor);
        panColor.add(choColor);
        panColor.add(labTeam);
        panColor.add(choTeam);        
                
        // layout main thing
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
                
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;    c.weighty = 0.0;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(panColor, gridbag, c);

        c.weightx = 1.0;    c.weighty = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(panMain, gridbag, c);

        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(labStatus, gridbag, c);

        c.gridwidth = 1;
        c.weightx = 1.0;    c.weighty = 0.0;
        addBag(client.cb.getComponent(), gridbag, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;    c.weighty = 0.0;
        addBag(butReady, gridbag, c);
    }
    
    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
    }
  
  
    /**
     * Sets up the board settings panel
     */
    private void setupBoardSettings() {
        labBoardSize = new Label("Board Size: # x # hexes", Label.CENTER);
        labMapSize = new Label("Map Size: # x # boards", Label.CENTER);
        
        lisBoardsSelected = new List(5);
            
        butChangeBoard = new Button("Edit / View");
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
        lisBoardsSelected.select(0);
    }
  
    /**
     * Sets up the entities panel
     */
    private void setupEntities() {
        lisEntities = new List(10);

        butLoad = new Button("Open Mech File...");
        butLoad.setActionCommand("load_mech");
        butLoad.addActionListener(this);
            
        butCustom = new Button("View/Edit Pilot...");
        butCustom.setActionCommand("custom_mech");
        butCustom.addActionListener(this);
            
        butDelete = new Button("Delete Mech");
        butDelete.setActionCommand("delete_mech");
        butDelete.addActionListener(this);
            
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
            
        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butDelete, c);
        panEntities.add(butDelete);
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
        
        butPos = new Button[8];
        for (int i = 0; i < 8; i++) {
            butPos[i] = new Button(startNames[i]);
            butPos[i].setActionCommand("starting_pos_" + i);
            butPos[i].addActionListener(this);
        }
        
        labMiddlePosition = new Label("", Label.CENTER);
            
        panStartButtons = new Panel();

        lisStarts = new List(5);
            
        panStarts = new Panel();
            
        // layout
        panStartButtons.setLayout(new GridLayout(3, 3));
        panStartButtons.add(butPos[0]);
        panStartButtons.add(butPos[1]);
        panStartButtons.add(butPos[2]);
        panStartButtons.add(butPos[7]);
        panStartButtons.add(labMiddlePosition);
        panStartButtons.add(butPos[3]);
        panStartButtons.add(butPos[6]);
        panStartButtons.add(butPos[5]);
        panStartButtons.add(butPos[4]);
        
        panStarts.setLayout(new BorderLayout());
        panStarts.add(labStarts, BorderLayout.NORTH);
        panStarts.add(panStartButtons, BorderLayout.WEST);
        panStarts.add(lisStarts, BorderLayout.CENTER);
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
        entityCorrespondance = new int[client.game.getNoOfEntities()];
        for (Enumeration i = client.getEntities(); i.hasMoreElements();) {
            Entity entity = (Entity)i.nextElement();
            lisEntities.add(entity.getDisplayName() 
                            + " (" + entity.getCrew().getGunnery() 
                            + "/" + entity.getCrew().getPiloting() + " pilot)"
                            + " BV=" + entity.calculateBattleValue());
            entityCorrespondance[listIndex++] = entity.getId();
        }
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
    
    /**
     * Refreshes the starting positions
     */
    private void refreshStarts() {
        lisStarts.removeAll();
        for (Enumeration i = client.getPlayers(); i.hasMoreElements();) {
            Player player = (Player)i.nextElement();
            if(player != null) {
                lisStarts.add(player.getName() + " : " + startNames[player.getStartingPos()]);
            }
        }
    }
    
    /**
     * Setup the color choice box
     */
    private void setupColors() {
        choColor.removeAll();
        for(int i = 0; i < Player.colorNames.length; i++) {
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
     * Refreshes the "ready" status of the ready button
     */
    private void refreshReadyButton() {
        butReady.setLabel(client.getLocalPlayer().isReady() ? "Cancel Ready" : "I'm Ready.");
    }
    
    /**
     * Change local player color.
     */
    public void changeColor(int nc) {
        client.getLocalPlayer().setColorIndex(nc);
        client.sendPlayerInfo();
    }
  
    //
    // GameListener
    //
    public void gamePlayerStatusChange(GameEvent ev) {
        refreshReadyButton();
        refreshBVs();
        refreshStarts();
        refreshColors();
    }
    public void gamePhaseChange(GameEvent ev) {
        if (client.game.phase !=  Game.PHASE_LOUNGE) {
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
    }
    
    //
    // ItemListener
    //
    public void itemStateChanged(ItemEvent ev) {
        if (ev.getSource() == choColor) {
            changeColor(choColor.getSelectedIndex());
        } else if (ev.getSource() ==chkBV || ev.getSource() == chkTons) {
            refreshBVs();
        }
        
    }


    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource() == butReady) {
            client.sendReady(!client.getLocalPlayer().isReady());
            refreshReadyButton();
        } else if (ev.getSource() == butLoad) {
            // add (load) mech
            FileDialog fd = new FileDialog(client.frame, 
                                           "Select a .mep file...",
                                           FileDialog.LOAD);
            fd.setDirectory("data" + File.separator + "mep");
            fd.show();
            if (fd.getFile() == null) {
                return;
            }
            // read the file
            File file = new File(fd.getDirectory(), fd.getFile());
            MepFile mf = new MepFile(file);
            if (!file.exists()) {
                // error reading file
                new AlertDialog(client.frame, "Open", "Error: could not read file or file not found.").show();
            } else {
                Mech mech = mf.getMech();
                if (mech == null) {
                    // error making mech
                    new AlertDialog(client.frame, "Open", "Error: could not make mech from file (possibly not 3025.)").show();
                } else {
                    mech.setOwner(client.getLocalPlayer());
                    client.sendAddEntity(mech);
                }
            }
        } else if (ev.getSource() == butCustom) {
            if (lisEntities.getSelectedIndex() != -1) {
                Entity entity = client.game.getEntity(entityCorrespondance[lisEntities.getSelectedIndex()]);
                boolean editable = entity.getOwnerId() == client.getLocalPlayer().getId();
                // display dialog
                CustomMechDialog cmd = new CustomMechDialog(client.frame, entity, editable);
                cmd.show();
                if (editable && cmd.isOkay()) {
                    // send changes
                    
                    client.sendUpdateEntity(entity);
                }
            }
        } else if (ev.getSource() == butDelete) {
            // delete mech
            if (lisEntities.getSelectedIndex() != -1) {
                client.sendDeleteEntity(entityCorrespondance[lisEntities.getSelectedIndex()]);
            }
        } else if (ev.getSource() == butChangeBoard) {
            // board settings 
            client.getBoardSelectionDialog().update(client.getMapSettings(), true);
            client.getBoardSelectionDialog().show();
        } else if (ev.getActionCommand().startsWith("starting_pos_")) {
            // starting position
            client.getLocalPlayer().setStartingPos(Integer.parseInt(ev.getActionCommand().substring(13)));
            client.sendPlayerInfo();
        }
    }
    
}
