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
    extends Dialog implements ActionListener, ItemListener
{
    private Client client;
    private MapSettings mapSettings;
    

    
    private Panel panMapSize = new Panel();

    private Label labBoardSize = new Label("Board Size (hexes):", Label.RIGHT);
    private Label labBoardDivider = new Label("x", Label.CENTER);
    private TextField texBoardWidth = new TextField(2);
    private TextField texBoardHeight = new TextField(2);
    
    private Label labMapSize = new Label("Map Size (boards):", Label.RIGHT);
    private Label labMapDivider = new Label("x", Label.CENTER);
    private TextField texMapWidth = new TextField(2);
    private TextField texMapHeight = new TextField(2);
    
    private ScrollPane scrMapButtons = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
    private Panel panMapButtons = new Panel();
    
    private Panel panBoardsSelected = new Panel();
    private Label labBoardsSelected = new Label("Maps Selected:", Label.CENTER);
    private java.awt.List lisBoardsSelected = new java.awt.List(10, true);
    private Checkbox chkSelectAll = new Checkbox("Select All");

    private Button butChange = new Button("<<");

    private Panel panBoardsAvailable = new Panel();
    private Label labBoardsAvailable = new Label("Maps Available :", Label.CENTER);
    private java.awt.List lisBoardsAvailable = new java.awt.List(12);
    
    private Panel panButtons = new Panel();
    private Button butUpdate = new Button("Update Size Settings");
    private Label labButtonSpace = new Label("", Label.CENTER);
    private Button butOkay = new Button("Okay");
    private Button butCancel = new Button("Cancel");
    

    /** Creates new BoardSelectionDialog */
    public BoardSelectionDialog(Client client) {
        super(client.frame, "Edit Board Layout...", true);
        this.client = client;
        this.mapSettings = (MapSettings)client.getMapSettings().clone();
        setResizable(true);
        
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
        
        
        pack();
        setLocation(client.frame.getLocation().x + client.frame.getSize().width/2 - getSize().width/2,
                    client.frame.getLocation().y + client.frame.getSize().height/2 - getSize().height/2);
    }
    
    /**
     * Set up the map size panel
     */
    private void setupMapSize() {
        refreshMapSize();
        refreshMapButtons();
        
        texBoardWidth.setEnabled(false);
        texBoardHeight.setEnabled(false);
        
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
    }
    
    
    private void setupButtons() {
        butUpdate.addActionListener(this);
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);
        
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
            lisBoardsSelected.add((index++) + ": " + (String)i.nextElement());
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
            lisBoardsSelected.replaceItem(selected[i] + ": " + board, selected[i]);
            mapSettings.getBoardsSelectedVector().setElementAt(board, selected[i]);
            lisBoardsSelected.select(selected[i]);
        }
    }
    
    /**
     * Applies the currently selected map size settings and refreshes the list
     * of maps from the server.
     */
    private void apply() {
        int boardWidth = Integer.parseInt(texBoardWidth.getText());
        int boardHeight = Integer.parseInt(texBoardHeight.getText());
        int mapWidth = Integer.parseInt(texMapWidth.getText());
        int mapHeight = Integer.parseInt(texMapHeight.getText());
        
        // check settings
        if (boardHeight <= 0 || boardHeight <= 0 || mapWidth <= 0 || mapHeight <= 0) {
            // alert...
            
            return;
        }
        
        mapSettings.setBoardSize(boardWidth, boardHeight);
        mapSettings.setMapSize(mapWidth, mapHeight);
        
        refreshMapSize();
        refreshMapButtons();
        
        lisBoardsSelected.removeAll();
        lisBoardsSelected.add("Updating...");
        
        lisBoardsAvailable.removeAll();
        lisBoardsAvailable.add("Updating...");
        
        client.sendMapQuery(mapSettings);
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
    }
    
    public void actionPerformed(ActionEvent e) {
        
        if (e.getSource() == butChange || e.getSource() == lisBoardsAvailable) {
            if (lisBoardsAvailable.getSelectedIndex() != -1) {
                change(lisBoardsAvailable.getSelectedItem());
            }
        } else if (e.getSource() == butUpdate) {
            apply();
        } else if (e.getSource() == butOkay) {
            client.sendMapSettings(mapSettings);
            this.setVisible(false);
        } else if (e.getSource() == butCancel) {
            this.setVisible(false);
        } else {
            // number button?
            try {
                // TODO: can't this be easier?
                if (e.getModifiers() == 0) {
                    final int[] selected = lisBoardsSelected.getSelectedIndexes();
                    for (int i = 0; i < selected.length; i++) {
                        lisBoardsSelected.deselect(selected[i]);
                    }
                }
                lisBoardsSelected.select(Integer.parseInt(e.getActionCommand()));
                refreshSelectAllCheck();
            } catch (NumberFormatException ex) {
            }
        }
    }

    public void itemStateChanged(java.awt.event.ItemEvent itemEvent) {
        if (itemEvent.getSource() == chkSelectAll) {
            for (int i = 0; i < lisBoardsSelected.getItemCount(); i++) {
                if (chkSelectAll.getState()) {
                    lisBoardsSelected.select(i);
                } else {
                    lisBoardsSelected.deselect(i);
                }
            }
        } else if (itemEvent.getSource() == lisBoardsSelected) {
            refreshSelectAllCheck();
        }
    }
    
}
