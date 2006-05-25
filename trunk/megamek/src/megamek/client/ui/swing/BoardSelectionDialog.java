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

package megamek.client.ui.swing;

import megamek.common.Board;
import megamek.common.MapSettings;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.List;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;

/**
 * @author Ben
 */
public class BoardSelectionDialog
        extends JDialog implements ActionListener, ItemListener, KeyListener, IMapSettingsObserver {
    private ClientGUI client;
    private MapSettings mapSettings;

    private RandomMapDialog randomMapDialog;

    private JPanel panMapSize = new JPanel();

    private JLabel labBoardSize = new JLabel(Messages.getString("BoardSelectionDialog.BoardSize"), JLabel.RIGHT); //$NON-NLS-1$
    private JLabel labBoardDivider = new JLabel("x", JLabel.CENTER); //$NON-NLS-1$
    private JTextField texBoardWidth = new JTextField(2);
    private JTextField texBoardHeight = new JTextField(2);

    private JLabel labMapSize = new JLabel(Messages.getString("BoardSelectionDialog.MapSize"), JLabel.RIGHT); //$NON-NLS-1$
    private JLabel labMapDivider = new JLabel("x", JLabel.CENTER); //$NON-NLS-1$
    private JTextField texMapWidth = new JTextField(2);
    private JTextField texMapHeight = new JTextField(2);

    private ScrollPane scrMapButtons = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
    private JPanel panMapButtons = new JPanel();

    private JPanel panBoardsSelected = new JPanel();
    private JLabel labBoardsSelected = new JLabel(Messages.getString("BoardSelectionDialog.MapsSelected"), JLabel.CENTER); //$NON-NLS-1$
    private List lisBoardsSelected = new List(10);
    private JCheckBox chkSelectAll = new JCheckBox(Messages.getString("BoardSelectionDialog.SelectAll")); //$NON-NLS-1$

    private JButton butChange = new JButton("<<"); //$NON-NLS-1$

    private JPanel panBoardsAvailable = new JPanel();
    private JLabel labBoardsAvailable = new JLabel(Messages.getString("BoardSelectionDialog.mapsAvailable"), JLabel.CENTER); //$NON-NLS-1$
    private List lisBoardsAvailable = new List(10);
    private JCheckBox chkRotateBoard = new JCheckBox(Messages.getString("BoardSelectionDialog.RotateBoard")); //$NON-NLS-1$

    private JPanel panButtons = new JPanel();
    private JButton butUpdate = new JButton(Messages.getString("BoardSelectionDialog.UpdateSize")); //$NON-NLS-1$
    private JButton butRandomMap = new JButton(Messages.getString("BoardSelectionDialog.GeneratedMapSettings")); //$NON-NLS-1$
    private JLabel labButtonSpace = new JLabel("", JLabel.CENTER); //$NON-NLS-1$
    private JButton butOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JButton butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$

    private boolean bDelayedSingleSelect;

    /**
     * Creates new BoardSelectionDialog
     */
    public BoardSelectionDialog(ClientGUI client) {
        super(client.frame, Messages.getString("BoardSelectionDialog.EditBoardLaout"), true); //$NON-NLS-1$
        this.client = client;
        mapSettings = (MapSettings) client.getClient().getMapSettings().clone();
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
        getContentPane().setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 1;
        gridbag.setConstraints(panMapSize, c);
        getContentPane().add(panMapSize);

        gridbag.setConstraints(panBoardsSelected, c);
        getContentPane().add(panBoardsSelected);

        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(butChange, c);
        getContentPane().add(butChange);

        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(panBoardsAvailable, c);
        getContentPane().add(panBoardsAvailable);

        gridbag.setConstraints(panButtons, c);
        getContentPane().add(panButtons);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();
        setResizable(false);
        setLocation(client.frame.getLocation().x + client.frame.getSize().width / 2 - getSize().width / 2,
                client.frame.getLocation().y + client.frame.getSize().height / 2 - getSize().height / 2);
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

    private void refreshMapSize() {
        texBoardWidth.setText(Integer.toString(mapSettings.getBoardWidth()));
        texBoardHeight.setText(Integer.toString(mapSettings.getBoardHeight()));
        texMapWidth.setText(Integer.toString(mapSettings.getMapWidth()));
        texMapHeight.setText(Integer.toString(mapSettings.getMapHeight()));
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
                JButton button = new JButton(Integer.toString(i * mapSettings.getMapWidth() + j));
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
            lisBoardsSelected.add(index++ + ": " + i.nextElement()); //$NON-NLS-1$
        }
        lisBoardsSelected.select(0);
        refreshSelectAllCheck();
    }

    private void refreshSelectAllCheck() {
        chkSelectAll.setSelected(lisBoardsSelected.getSelectedIndexes().length == lisBoardsSelected.getItemCount());
    }

    private void refreshBoardsAvailable() {
        lisBoardsAvailable.removeAll();
        for (Enumeration i = mapSettings.getBoardsAvailable(); i.hasMoreElements();) {
            lisBoardsAvailable.add((String) i.nextElement());
        }
    }

    /**
     * Changes all selected boards to be the specified board
     */
    private void change(String board) {
        int[] selected = lisBoardsSelected.getSelectedIndexes();
        for (final int newVar : selected) {
            String name = board;
            if (!MapSettings.BOARD_RANDOM.equals(name) &&
                    !MapSettings.BOARD_SURPRISE.equals(name) &&
                    chkRotateBoard.isSelected()) {
                name = Board.BOARD_REQUEST_ROTATION + name;
            }
            lisBoardsSelected.replaceItem(newVar + ": " + name, newVar); //$NON-NLS-1$
            mapSettings.getBoardsSelectedVector().setElementAt(name, newVar);
            lisBoardsSelected.select(newVar);
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
            new AlertDialog(client.frame, Messages.getString("BoardSelectionDialog.InvalidMapSize"), Messages.getString("BoardSelectionDialog.InvalidNumberOfmaps")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }
        
        // check settings
        if (boardHeight <= 0 || boardHeight <= 0 || mapWidth <= 0 || mapHeight <= 0) {
            new AlertDialog(client.frame, Messages.getString("BoardSelectionDialog.InvalidMapSize"), Messages.getString("BoardSelectionDialog.MapSizeMustBeGreateter0")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
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
    private void send() {
        // check that they haven't modified the map size settings
        if (!texBoardWidth.getText().equals(Integer.toString(mapSettings.getBoardWidth()))
                || !texBoardHeight.getText().equals(Integer.toString(mapSettings.getBoardHeight()))
                || !texMapWidth.getText().equals(Integer.toString(mapSettings.getMapWidth()))
                || !texMapHeight.getText().equals(Integer.toString(mapSettings.getMapHeight()))) {
            new AlertDialog(client.frame, Messages.getString("BoardSelectionDialog.UpdateMapSize.title"), Messages.getString("BoardSelectionDialog.UpdateMapSize.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        if (mapSettings.getBoardsAvailableVector().size() <= 0) {
            new AlertDialog(client.frame, Messages.getString("BoardSelectionDialog.NoBoardOfSelectedSize.title"), Messages.getString("BoardSelectionDialog.NoBoardOfSelectedSize.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        client.getClient().sendMapSettings(mapSettings);
        setVisible(false);
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource().equals(butChange) || e.getSource().equals(lisBoardsAvailable)) {
            if (lisBoardsAvailable.getSelectedIndex() != -1) {
                change(lisBoardsAvailable.getSelectedItem());
            }
        } else if (e.getSource().equals(butUpdate)) {
            apply();
        } else if (e.getSource().equals(butOkay)) {
            send();
        } else if (e.getSource().equals(butCancel)) {
            setVisible(false);
        } else if (e.getSource().equals(butRandomMap)) {
            randomMapDialog.setVisible(true);
        }
    }

    public void itemStateChanged(ItemEvent itemEvent) {
        if (itemEvent.getSource().equals(chkSelectAll)) {
            lisBoardsSelected.setMultipleMode(chkSelectAll.isSelected());
            for (int i = 0; i < lisBoardsSelected.getItemCount(); i++) {
                if (chkSelectAll.isSelected()) {
                    lisBoardsSelected.select(i);
                } else {
                    lisBoardsSelected.deselect(i);
                }
            }
        } else if (itemEvent.getSource().equals(lisBoardsSelected)) {
//            System.out.println(itemEvent.paramString());
//            System.out.flush();
//            final int[] selected = lisBoardsSelected.getSelectedIndexes();
//            for (int i = 0; i < selected.length; i++) {
//                lisBoardsSelected.deselect(selected[i]);
//            }
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
     * <p/>
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
