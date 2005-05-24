/*
 * MegaMek - Copyright (C) 2002-2003 Ben Mazur (bmazur@sev.org)
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

/*
 * BoardSelectionDialog.java
 *
 * Created on March 25, 2002, 6:28 PM
 */

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import megamek.common.*;

/**
 *
 * @author  Ben
 * @version 
 */
public class BoardSelectionDialog 
    extends Dialog implements ActionListener, ItemListener, KeyListener
{
    private ClientGUI client;
    private MapSettings mapSettings;
    
    private RandomMapDialog randomMapDialog;
    
    private Panel panMapSize = new Panel();

    private Label labBoardSize = new Label(Messages.getString("BoardSelectionDialog.BoardSize"), Label.RIGHT); //$NON-NLS-1$
    private Label labBoardDivider = new Label("x", Label.CENTER); //$NON-NLS-1$
    private TextField texBoardWidth = new TextField(2);
    private TextField texBoardHeight = new TextField(2);
    
    private Label labMapSize = new Label(Messages.getString("BoardSelectionDialog.MapSize"), Label.RIGHT); //$NON-NLS-1$
    private Label labMapDivider = new Label("x", Label.CENTER); //$NON-NLS-1$
    private TextField texMapWidth = new TextField(2);
    private TextField texMapHeight = new TextField(2);
    
    private ScrollPane scrMapButtons = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
    private Panel panMapButtons = new Panel();
    
    private Panel panBoardsSelected = new Panel();
    private Label labBoardsSelected = new Label(Messages.getString("BoardSelectionDialog.MapsSelected"), Label.CENTER); //$NON-NLS-1$
    private java.awt.List lisBoardsSelected = new java.awt.List(10);
    private Checkbox chkSelectAll = new Checkbox(Messages.getString("BoardSelectionDialog.SelectAll")); //$NON-NLS-1$

    private Button butChange = new Button("<<"); //$NON-NLS-1$

    private Panel panBoardsAvailable = new Panel();
    private Label labBoardsAvailable = new Label(Messages.getString("BoardSelectionDialog.mapsAvailable"), Label.CENTER); //$NON-NLS-1$
    private java.awt.List lisBoardsAvailable = new java.awt.List(10);
    private Checkbox chkRotateBoard = new Checkbox(Messages.getString("BoardSelectionDialog.RotateBoard")); //$NON-NLS-1$
    
    private Panel panButtons = new Panel();
    private Button butUpdate = new Button(Messages.getString("BoardSelectionDialog.UpdateSize")); //$NON-NLS-1$
    private Button butRandomMap = new Button(Messages.getString("BoardSelectionDialog.GeneratedMapSettings")); //$NON-NLS-1$
    private Label labButtonSpace = new Label("", Label.CENTER); //$NON-NLS-1$
    private Button butOkay = new Button(Messages.getString("Okay")); //$NON-NLS-1$
    private Button butCancel = new Button(Messages.getString("Cancel")); //$NON-NLS-1$
    
    private boolean bDelayedSingleSelect = false;
    

    /** Creates new BoardSelectionDialog */
    public BoardSelectionDialog(ClientGUI client) {
        super(client.frame, Messages.getString("BoardSelectionDialog.EditBoardLaout"), true); //$NON-NLS-1$
        this.client = client;
        this.mapSettings = (MapSettings)client.getClient().getMapSettings().clone();
        setResizable(true);
        
        randomMapDialog = new RandomMapDialog(client.frame, this, mapSettings);

        setupMapSize();
        setupSelected();
        setupAvailable();
        setupButtons();
        
        butChange.addActionListener(this);
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        this.setLayout(gridbag);
            
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 1.0;    c.weighty = 1.0;
        c.gridwidth = 1;
        gridbag.setConstraints(panMapSize, c);
        this.add(panMapSize);
            
        gridbag.setConstraints(panBoardsSelected, c);
        this.add(panBoardsSelected);
            
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(butChange, c);
        this.add(butChange);
            
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(panBoardsAvailable, c);
        this.add(panBoardsAvailable);
            
        gridbag.setConstraints(panButtons, c);
        this.add(panButtons);        
        
        addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) { setVisible(false); }
    });
        
        pack();
        setResizable(false);
        setLocation(client.frame.getLocation().x + client.frame.getSize().width/2 - getSize().width/2,
                    client.frame.getLocation().y + client.frame.getSize().height/2 - getSize().height/2);
    }
    
    /**
     * Set up the map size panel
     */
    private void setupMapSize() {
        refreshMapSize();
        refreshMapButtons();
        
        scrMapButtons.add(panMapButtons);
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panMapSize.setLayout(gridbag);
            
        c.insets = new Insets(1, 1, 1, 1);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = 1;
        gridbag.setConstraints(labBoardSize, c);
        panMapSize.add(labBoardSize);
            
        gridbag.setConstraints(texBoardWidth, c);
        panMapSize.add(texBoardWidth);
            
        gridbag.setConstraints(labBoardDivider, c);
        panMapSize.add(labBoardDivider);
            
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(texBoardHeight, c);
        panMapSize.add(texBoardHeight);
            
        c.gridwidth = 1;
        gridbag.setConstraints(labMapSize, c);
        panMapSize.add(labMapSize);
            
        gridbag.setConstraints(texMapWidth, c);
        panMapSize.add(texMapWidth);
            
        gridbag.setConstraints(labMapDivider, c);
        panMapSize.add(labMapDivider);
            
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(texMapHeight, c);
        panMapSize.add(texMapHeight);
            
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;    c.weighty = 1.0;
        gridbag.setConstraints(scrMapButtons, c);
        panMapSize.add(scrMapButtons);
    }
    
    private void setupSelected() {
        refreshBoardsSelected();
        lisBoardsSelected.addItemListener(this);
        lisBoardsSelected.addKeyListener(this);
        chkSelectAll.addItemListener(this);
        
        panBoardsSelected.setLayout(new BorderLayout());
        
        panBoardsSelected.add(labBoardsSelected, BorderLayout.NORTH);
        panBoardsSelected.add(lisBoardsSelected, BorderLayout.CENTER);
        panBoardsSelected.add(chkSelectAll, BorderLayout.SOUTH);
    }
    
    private void setupAvailable() {
        refreshBoardsAvailable();
        lisBoardsAvailable.addActionListener(this);

        panBoardsAvailable.setLayout(new BorderLayout());
        
        panBoardsAvailable.add(labBoardsAvailable, BorderLayout.NORTH);
        panBoardsAvailable.add(lisBoardsAvailable, BorderLayout.CENTER);
        panBoardsAvailable.add(chkRotateBoard, BorderLayout.SOUTH);
    }
    
    
    private void setupButtons() {
        butUpdate.addActionListener(this);
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);
        butRandomMap.addActionListener(this);
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panButtons.setLayout(gridbag);
            
        c.insets = new Insets(5, 5, 0, 0);
        c.weightx = 0.0;    c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;    c.ipady = 5;
        c.gridwidth = 1;
        gridbag.setConstraints(butUpdate, c);
        panButtons.add(butUpdate);
        
        gridbag.setConstraints(butRandomMap, c);
        panButtons.add(butRandomMap);

        c.weightx = 1.0;    c.weighty = 1.0;
        gridbag.setConstraints(labButtonSpace, c);
        panButtons.add(labButtonSpace);

        c.weightx = 0.0;    c.weighty = 1.0;
        gridbag.setConstraints(butOkay, c);
        panButtons.add(butOkay);
            
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butCancel, c);
        panButtons.add(butCancel);
    }
    
    private void refreshMapSize() {
        texBoardWidth.setText(new Integer(mapSettings.getBoardWidth()).toString());
        texBoardHeight.setText(new Integer(mapSettings.getBoardHeight()).toString());
        texMapWidth.setText(new Integer(mapSettings.getMapWidth()).toString());
        texMapHeight.setText(new Integer(mapSettings.getMapHeight()).toString());
    }
    
    /**
     * Fills the Map Buttons scroll pane with the appropriate amount of buttons
     * in the appropriate layout
     */
    private void refreshMapButtons() {
        panMapButtons.removeAll();
        
        panMapButtons.setLayout(new GridLayout(mapSettings.getMapHeight(), mapSettings.getMapWidth()));
        
        for (int i = 0; i < mapSettings.getMapHeight(); i++) {
            for (int j = 0; j < mapSettings.getMapWidth(); j++) {
                Button button = new Button(new Integer(i * mapSettings.getMapWidth() + j).toString());
                button.addActionListener(this);
                panMapButtons.add(button);
            }
        }
        
        scrMapButtons.validate();
    }
    
    private void refreshBoardsSelected() {
        lisBoardsSelected.removeAll();
        int index = 0;
        for (Enumeration i = mapSettings.getBoardsSelected(); i.hasMoreElements();) {
            lisBoardsSelected.add((index++) + ": " + (String)i.nextElement()); //$NON-NLS-1$
        }
        lisBoardsSelected.select(0);
        refreshSelectAllCheck();
    }
    
    private void refreshSelectAllCheck() {
        chkSelectAll.setState(lisBoardsSelected.getSelectedIndexes().length == lisBoardsSelected.getItemCount());
    }
    
    private void refreshBoardsAvailable() {
        lisBoardsAvailable.removeAll();
        for (Enumeration i = mapSettings.getBoardsAvailable(); i.hasMoreElements();) {
            lisBoardsAvailable.add((String)i.nextElement());
        }
    }
    
    /**
     * Changes all selected boards to be the specified board
     */
    private void change(String board) {
        int[] selected = lisBoardsSelected.getSelectedIndexes();
        for (int i = 0; i < selected.length; i++) {
            String name = board;
            if ( !MapSettings.BOARD_RANDOM.equals(name) &&
                 !MapSettings.BOARD_SURPRISE.equals(name) &&
                 chkRotateBoard.getState() ) {
                name = Board.BOARD_REQUEST_ROTATION + name;
            }
            lisBoardsSelected.replaceItem(selected[i] + ": " + name, selected[i]); //$NON-NLS-1$
            mapSettings.getBoardsSelectedVector().setElementAt(name, selected[i]);
            lisBoardsSelected.select(selected[i]);
        }
    }
    
    /**
     * Applies the currently selected map size settings and refreshes the list
     * of maps from the server.
     */
    private void apply() {
        int boardWidth;
        int boardHeight;
        int mapWidth;
        int mapHeight;
        
        // read map size settings
        try {
            boardWidth = Integer.parseInt(texBoardWidth.getText());
            boardHeight = Integer.parseInt(texBoardHeight.getText());
            mapWidth = Integer.parseInt(texMapWidth.getText());
            mapHeight = Integer.parseInt(texMapHeight.getText());
        } catch (NumberFormatException ex) {
            new AlertDialog(client.frame, Messages.getString("BoardSelectionDialog.InvalidMapSize"), Messages.getString("BoardSelectionDialog.InvalidNumberOfmaps")).show(); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }
        
        // check settings
        if (boardHeight <= 0 || boardHeight <= 0 || mapWidth <= 0 || mapHeight <= 0) {
            new AlertDialog(client.frame, Messages.getString("BoardSelectionDialog.InvalidMapSize"), Messages.getString("BoardSelectionDialog.MapSizeMustBeGreateter0")).show(); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }
        
        butOkay.setEnabled(false);
        
        mapSettings.setBoardSize(boardWidth, boardHeight);
        mapSettings.setMapSize(mapWidth, mapHeight);
        
        randomMapDialog.setMapSettings(mapSettings);

        refreshMapSize();
        refreshMapButtons();
        
        lisBoardsSelected.removeAll();
        lisBoardsSelected.add(Messages.getString("BoardSelectionDialog.Updating")); //$NON-NLS-1$
        
        lisBoardsAvailable.removeAll();
        lisBoardsAvailable.add(Messages.getString("BoardSelectionDialog.Updating")); //$NON-NLS-1$
        
        client.getClient().sendMapQuery(mapSettings);
    }
    
    /**
     * Updates to show the map settings that have, presumably, just been sent
     * by the server.
     */
    public void update(MapSettings mapSettings, boolean updateSize) {
        this.mapSettings = (MapSettings)mapSettings.clone();
        if (updateSize) {
            refreshMapSize();
            refreshMapButtons();
        }
        refreshBoardsSelected();
        refreshBoardsAvailable();
        butOkay.setEnabled(true);
    }
    
    /**
     * Checks and sends the new map settings to the server
     */
    public void send() {
        // check that they haven't modified the map size settings
        if (!texBoardWidth.getText().equals(Integer.toString(mapSettings.getBoardWidth()))
        || !texBoardHeight.getText().equals(Integer.toString(mapSettings.getBoardHeight()))
        || !texMapWidth.getText().equals(Integer.toString(mapSettings.getMapWidth()))
        || !texMapHeight.getText().equals(Integer.toString(mapSettings.getMapHeight()))) {
            new AlertDialog(client.frame, Messages.getString("BoardSelectionDialog.UpdateMapSize.title"), Messages.getString("BoardSelectionDialog.UpdateMapSize.message")).show(); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }
        
        if (mapSettings.getBoardsAvailableVector().size() <= 0) {
            new AlertDialog(client.frame, Messages.getString("BoardSelectionDialog.NoBoardOfSelectedSize.title"), Messages.getString("BoardSelectionDialog.NoBoardOfSelectedSize.message")).show(); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }
        
        client.getClient().sendMapSettings(mapSettings);
        this.setVisible(false);
    }
    
    public void actionPerformed(ActionEvent e) {
        
        if (e.getSource() == butChange || e.getSource() == lisBoardsAvailable) {
            if (lisBoardsAvailable.getSelectedIndex() != -1) {
                change(lisBoardsAvailable.getSelectedItem());
            }
        } else if (e.getSource() == butUpdate) {
            apply();
        } else if (e.getSource() == butOkay) {
            send();
        } else if (e.getSource() == butCancel) {
            this.setVisible(false);
        } else if (e.getSource() == butRandomMap) {
            randomMapDialog.setVisible(true);
        } 
    }

    public void itemStateChanged(java.awt.event.ItemEvent itemEvent) {
        if (itemEvent.getSource() == chkSelectAll) {
            lisBoardsSelected.setMultipleMode(chkSelectAll.getState());
            for (int i = 0; i < lisBoardsSelected.getItemCount(); i++) {
                if (chkSelectAll.getState()) {
                    lisBoardsSelected.select(i);
                } else {
                    lisBoardsSelected.deselect(i);
                }
            }
        } else if (itemEvent.getSource() == lisBoardsSelected) {
//            System.out.println(itemEvent.paramString());
//            System.out.flush();
//            final int[] selected = lisBoardsSelected.getSelectedIndexes();
//            for (int i = 0; i < selected.length; i++) {
//                lisBoardsSelected.deselect(selected[i]);
//            }
            System.out.println("Selected!"); //$NON-NLS-1$
            if (bDelayedSingleSelect) {
                lisBoardsSelected.setMultipleMode(false);
            }
            refreshSelectAllCheck();
        }
    }
    
    public void updateMapSettings(MapSettings mapSettings) {
        this.mapSettings = mapSettings;
        refreshMapSize();
        refreshMapButtons();
        
        lisBoardsSelected.removeAll();
        lisBoardsSelected.add(Messages.getString("BoardSelectionDialog.Updating")); //$NON-NLS-1$
        
        lisBoardsAvailable.removeAll();
        lisBoardsAvailable.add(Messages.getString("BoardSelectionDialog.Updating")); //$NON-NLS-1$
        
        client.getClient().sendMapQuery(mapSettings);
    }
    
    /**
     * I hate AWT. -jy
     * This is a hacked up version of a simple select list that supports
     * holding control down to select multiple items.  AWT Lists don't
     * support this natively.
     * 
     * The trick is to turn on multiple mode on the list if the user presses
     * control.  But we can't turn multi mode off as soon as they release, or
     * any existing multi-select will be wiped out.  Instead we set a flag
     * to indicate any later selection should trigger a set to single-select 
     */

    public void keyPressed(KeyEvent ev) {
        if (ev.getKeyCode() == KeyEvent.VK_CONTROL) {
            System.out.println("Multiple on!"); //$NON-NLS-1$
            lisBoardsSelected.setMultipleMode(true);
            bDelayedSingleSelect = false;
        }
    }


    public void keyReleased(KeyEvent ev) {
        if (ev.getKeyCode() == KeyEvent.VK_CONTROL) {
            System.out.println("Multiple off!"); //$NON-NLS-1$
            bDelayedSingleSelect = true;
        }
    }

    public void keyTyped(KeyEvent arg0) {
    }
}
