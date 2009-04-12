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

package megamek.client.ui.AWT;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Iterator;

import megamek.client.ui.Messages;
import megamek.common.Board;
import megamek.common.IBoard;
import megamek.common.MapSettings;
import megamek.common.util.BoardUtilities;

/**
 * @author Ben
 * @version
 */
public class BoardSelectionDialog extends Dialog implements ActionListener,
        ItemListener, KeyListener, IMapSettingsObserver {
    private static final long serialVersionUID = 1498160432750299823L;
    private ClientGUI client;
    private MapSettings mapSettings;

    private RandomMapDialog randomMapDialog;

    private Panel panTypeChooser = new Panel();
    private Choice typeChooser = new Choice();
    
    private Panel panMapSize = new Panel();

    private Label labBoardSize = new Label(Messages
            .getString("BoardSelectionDialog.BoardSize"), Label.RIGHT); //$NON-NLS-1$
    private Label labBoardDivider = new Label("x", Label.CENTER); //$NON-NLS-1$
    private TextField texBoardWidth = new TextField(2);
    private TextField texBoardHeight = new TextField(2);

    private Label labMapSize = new Label(Messages
            .getString("BoardSelectionDialog.MapSize"), Label.RIGHT); //$NON-NLS-1$
    private Label labMapDivider = new Label("x", Label.CENTER); //$NON-NLS-1$
    private TextField texMapWidth = new TextField(2);
    private TextField texMapHeight = new TextField(2);

    private ScrollPane scrMapButtons = new ScrollPane(
            ScrollPane.SCROLLBARS_AS_NEEDED);
    private Panel panMapButtons = new Panel();

    private Panel panBoardsSelected = new Panel();
    private Label labBoardsSelected = new Label(Messages
            .getString("BoardSelectionDialog.MapsSelected"), Label.CENTER); //$NON-NLS-1$
    private java.awt.List lisBoardsSelected = new java.awt.List(10);
    private Checkbox chkSelectAll = new Checkbox(Messages
            .getString("BoardSelectionDialog.SelectAll")); //$NON-NLS-1$

    private Button butChange = new Button("<<"); //$NON-NLS-1$

    private Panel panBoardsAvailable = new Panel();
    private Label labBoardsAvailable = new Label(Messages
            .getString("BoardSelectionDialog.mapsAvailable"), Label.CENTER); //$NON-NLS-1$
    private java.awt.List lisBoardsAvailable = new java.awt.List(10);
    private Checkbox chkRotateBoard = new Checkbox(Messages
            .getString("BoardSelectionDialog.RotateBoard")); //$NON-NLS-1$

    private Panel panButtons = new Panel();
    private Button butUpdate = new Button(Messages
            .getString("BoardSelectionDialog.UpdateSize")); //$NON-NLS-1$
    private Button butRandomMap = new Button(Messages
            .getString("BoardSelectionDialog.GeneratedMapSettings")); //$NON-NLS-1$
    private Label labButtonSpace = new Label("", Label.CENTER); //$NON-NLS-1$
    private Button butOkay = new Button(Messages.getString("Okay")); //$NON-NLS-1$
    private Button butCancel = new Button(Messages.getString("Cancel")); //$NON-NLS-1$
    private Button butPreview = new Button(Messages
            .getString("BoardSelectionDialog.Preview")); //$NON-NLS-1$
    
    Dialog mapPreviewW;

    private boolean bDelayedSingleSelect = false;

    /** Creates new BoardSelectionDialog */
    public BoardSelectionDialog(ClientGUI client) {
        super(client.frame, Messages
                .getString("BoardSelectionDialog.EditBoardLaout"), true); //$NON-NLS-1$
        this.client = client;
        this.mapSettings = (MapSettings) client.getClient().getMapSettings()
                .clone();
        setResizable(true);

        randomMapDialog = new RandomMapDialog(client.frame, this, mapSettings);

        setupMapChoice();
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
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 1;
        gridbag.setConstraints(panTypeChooser, c);
        this.add(panTypeChooser);
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

        mapPreviewW = new Dialog(this.client.frame, Messages
                .getString("BoardSelectionDialog.MapPreview"), false); //$NON-NLS-1$

        mapPreviewW.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                mapPreviewW.setVisible(false);
            }
        });

        pack();
        setResizable(false);
        setLocation(client.frame.getLocation().x + client.frame.getSize().width
                / 2 - getSize().width / 2, client.frame.getLocation().y
                + client.frame.getSize().height / 2 - getSize().height / 2);
    }

    /**
     * Set up the map chooser panel
     */
    private void setupMapChoice() {
        typeChooser.add(MapSettings.getMediumName(MapSettings.MEDIUM_GROUND));
        typeChooser.add(MapSettings.getMediumName(MapSettings.MEDIUM_ATMOSPHERE));
        typeChooser.add(MapSettings.getMediumName(MapSettings.MEDIUM_SPACE));
        typeChooser.addItemListener(this);
        refreshMapChoice();
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panTypeChooser.setLayout(gridbag);

        c.insets = new Insets(1, 1, 1, 1);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(typeChooser,c);
        panTypeChooser.add(typeChooser);
        
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
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(typeChooser,c);
        panMapSize.add(typeChooser);
        
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
        c.weightx = 1.0;
        c.weighty = 1.0;
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
        butPreview.addActionListener(this);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panButtons.setLayout(gridbag);

        c.insets = new Insets(5, 5, 0, 0);
        c.weightx = 0.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;
        c.ipady = 5;
        c.gridwidth = 1;
        gridbag.setConstraints(butUpdate, c);
        panButtons.add(butUpdate);

        gridbag.setConstraints(butRandomMap, c);
        panButtons.add(butRandomMap);

        gridbag.setConstraints(butPreview, c);
        panButtons.add(butPreview);

        c.weightx = 1.0;
        c.weighty = 1.0;
        gridbag.setConstraints(labButtonSpace, c);
        panButtons.add(labButtonSpace);

        c.weightx = 0.0;
        c.weighty = 1.0;
        gridbag.setConstraints(butOkay, c);
        panButtons.add(butOkay);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butCancel, c);
        panButtons.add(butCancel);
    }

    private void refreshMapChoice() {
        typeChooser.select(mapSettings.getMedium());
    }
    
    private void refreshMapSize() {
        texBoardWidth.setText(new Integer(mapSettings.getBoardWidth())
                .toString());
        texBoardHeight.setText(new Integer(mapSettings.getBoardHeight())
                .toString());
        texMapWidth.setText(new Integer(mapSettings.getMapWidth()).toString());
        texMapHeight
                .setText(new Integer(mapSettings.getMapHeight()).toString());
    }

    /**
     * Fills the Map Buttons scroll pane with the appropriate amount of buttons
     * in the appropriate layout
     */
    private void refreshMapButtons() {
        panMapButtons.removeAll();

        panMapButtons.setLayout(new GridLayout(mapSettings.getMapHeight(),
                mapSettings.getMapWidth()));

        for (int i = 0; i < mapSettings.getMapHeight(); i++) {
            for (int j = 0; j < mapSettings.getMapWidth(); j++) {
                Button button = new Button(new Integer(i
                        * mapSettings.getMapWidth() + j).toString());
                button.addActionListener(this);
                panMapButtons.add(button);
            }
        }

        scrMapButtons.validate();
    }

    private void refreshBoardsSelected() {
        lisBoardsSelected.removeAll();
        int index = 0;
        for (Iterator<String> i = mapSettings.getBoardsSelected(); i.hasNext();) {
            lisBoardsSelected.add((index++) + ": " + i.next()); //$NON-NLS-1$
        }
        lisBoardsSelected.select(0);
        refreshSelectAllCheck();
    }

    private void refreshSelectAllCheck() {
        chkSelectAll
                .setState(lisBoardsSelected.getSelectedIndexes().length == lisBoardsSelected
                        .getItemCount());
    }

    private void refreshBoardsAvailable() {
        lisBoardsAvailable.removeAll();
        for (Iterator<String> i = mapSettings.getBoardsAvailable(); i.hasNext();) {
            lisBoardsAvailable.add(i.next());
        }
    }

    /**
     * Changes all selected boards to be the specified board
     */
    private void change(String board) {
        int[] selected = lisBoardsSelected.getSelectedIndexes();
        for (int i = 0; i < selected.length; i++) {
            String name = board;
            if (!MapSettings.BOARD_RANDOM.equals(name)
                    && !MapSettings.BOARD_SURPRISE.equals(name)
                    && chkRotateBoard.getState()) {
                name = Board.BOARD_REQUEST_ROTATION + name;
            }
            lisBoardsSelected.replaceItem(
                    selected[i] + ": " + name, selected[i]); //$NON-NLS-1$
            mapSettings.getBoardsSelectedVector().set(selected[i], name);
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
            new AlertDialog(
                    client.frame,
                    Messages.getString("BoardSelectionDialog.InvalidMapSize"), Messages.getString("BoardSelectionDialog.InvalidNumberOfmaps")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }
        
        // check settings
        if (boardWidth <= 0 || boardHeight <= 0 || mapWidth <= 0
                || mapHeight <= 0) {
            new AlertDialog(
                    client.frame,
                    Messages.getString("BoardSelectionDialog.InvalidMapSize"), Messages.getString("BoardSelectionDialog.MapSizeMustBeGreateter0")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        butOkay.setEnabled(false);
        
        mapSettings.setBoardSize(boardWidth, boardHeight);
        mapSettings.setMapSize(mapWidth, mapHeight);
        
        randomMapDialog.setMapSettings(mapSettings);

        refreshMapSize();
        refreshMapButtons();

        lisBoardsSelected.removeAll();
        lisBoardsSelected.add(Messages
                .getString("BoardSelectionDialog.Updating")); //$NON-NLS-1$

        lisBoardsAvailable.removeAll();
        lisBoardsAvailable.add(Messages
                .getString("BoardSelectionDialog.Updating")); //$NON-NLS-1$

        client.getClient().sendMapQuery(mapSettings);
    }

    /**
     * Updates to show the map settings that have, presumably, just been sent by
     * the server.
     */
    public void update(MapSettings mapSettings, boolean updateSize) {
        this.mapSettings = (MapSettings) mapSettings.clone();
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
        if (!texBoardWidth.getText().equals(
                Integer.toString(mapSettings.getBoardWidth()))
                || !texBoardHeight.getText().equals(
                        Integer.toString(mapSettings.getBoardHeight()))
                || !texMapWidth.getText().equals(
                        Integer.toString(mapSettings.getMapWidth()))
                || !texMapHeight.getText().equals(
                        Integer.toString(mapSettings.getMapHeight()))) {
            new AlertDialog(
                    client.frame,
                    Messages
                            .getString("BoardSelectionDialog.UpdateMapSize.title"), Messages.getString("BoardSelectionDialog.UpdateMapSize.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        if (mapSettings.getBoardsAvailableVector().size() <= 0) {
            new AlertDialog(
                    client.frame,
                    Messages
                            .getString("BoardSelectionDialog.NoBoardOfSelectedSize.title"), Messages.getString("BoardSelectionDialog.NoBoardOfSelectedSize.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }
        
        //change the type - probably not the right place for this but I can't get it to work elsewhere
        if(typeChooser.getSelectedIndex() == 2) {
            mapSettings.setMedium(MapSettings.MEDIUM_SPACE);
        } else if(typeChooser.getSelectedIndex() == 1) {
            mapSettings.setMedium(MapSettings.MEDIUM_ATMOSPHERE);
        } else if(typeChooser.getSelectedIndex() == 0) {
            mapSettings.setMedium(MapSettings.MEDIUM_GROUND);
        }
        
        client.getClient().sendMapSettings(mapSettings);
        this.setVisible(false);
        mapPreviewW.setVisible(false);
    }

    public void previewBoard() {
        String boardName = lisBoardsAvailable.getSelectedItem();
        if (lisBoardsAvailable.getSelectedIndex() > 2) {
            IBoard board = new Board(new Integer(texBoardWidth.getText()),
                    new Integer(texBoardHeight.getText()));
            board.load(boardName + ".board");
            if (chkRotateBoard.getState()) {
                BoardUtilities.flip(board, true, true);
            }
            MapPreview mapPreview = null;
            try {
                mapPreview = new MapPreview(mapPreviewW, board);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mapPreviewW.removeAll();
            mapPreviewW.add(mapPreview);
            mapPreviewW.setVisible(true);
            mapPreview.initializeMap();
        }
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
            mapPreviewW.setVisible(false);
        } else if (e.getSource() == butRandomMap) {
            randomMapDialog.setVisible(true);
        } else if (e.getSource() == butPreview) {
            previewBoard();
        } else {
            try {
                int board = Integer.parseInt(e.getActionCommand());
                this.lisBoardsSelected.select(board);
            } catch (NumberFormatException n) {
            } catch (ArrayIndexOutOfBoundsException a) {
            }
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
            // System.out.println(itemEvent.paramString());
            // System.out.flush();
            // final int[] selected = lisBoardsSelected.getSelectedIndexes();
            // for (int i = 0; i < selected.length; i++) {
            // lisBoardsSelected.deselect(selected[i]);
            // }
            if (bDelayedSingleSelect) {
                lisBoardsSelected.setMultipleMode(false);
            }
            refreshSelectAllCheck();
        } else if(itemEvent.getSource() == typeChooser) {
            //don't disable board selection, in case of null board
            if(typeChooser.getSelectedIndex() == 2) {
                //panBoardsSelected.setEnabled(false);
                //panBoardsAvailable.setEnabled(false);
                //butChange.setEnabled(false);
            } else if(typeChooser.getSelectedIndex() == 1) { 
                //panBoardsSelected.setEnabled(true);
                //panBoardsAvailable.setEnabled(true);
                //butChange.setEnabled(true);
            } else if(typeChooser.getSelectedIndex() == 0){
                //panBoardsSelected.setEnabled(true);
                //panBoardsAvailable.setEnabled(true);
                //butChange.setEnabled(true);
            }
        }
    }

    public void updateMapSettings(MapSettings mapSettings) {
        this.mapSettings = mapSettings;
        refreshMapSize();
        refreshMapButtons();

        lisBoardsSelected.removeAll();
        lisBoardsSelected.add(Messages
                .getString("BoardSelectionDialog.Updating")); //$NON-NLS-1$

        lisBoardsAvailable.removeAll();
        lisBoardsAvailable.add(Messages
                .getString("BoardSelectionDialog.Updating")); //$NON-NLS-1$

        client.getClient().sendMapQuery(mapSettings);
    }

    /**
     * I hate AWT. -jy This is a hacked up version of a simple select list that
     * supports holding control down to select multiple items. AWT Lists don't
     * support this natively. The trick is to turn on multiple mode on the list
     * if the user presses control. But we can't turn multi mode off as soon as
     * they release, or any existing multi-select will be wiped out. Instead we
     * set a flag to indicate any later selection should trigger a set to
     * single-select
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
