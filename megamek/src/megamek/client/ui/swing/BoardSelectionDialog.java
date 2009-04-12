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

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.ui.Messages;
import megamek.common.Board;
import megamek.common.IBoard;
import megamek.common.MapSettings;
import megamek.common.util.BoardUtilities;

/**
 * @author Ben
 */
public class BoardSelectionDialog extends JDialog implements ActionListener,
        IMapSettingsObserver, ListSelectionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -4591325591152076483L;
    private ClientGUI client;
    private MapSettings mapSettings;

    private RandomMapDialog randomMapDialog;

    private JPanel panTypeChooser = new JPanel();
    private Choice typeChooser = new Choice();

    private JPanel panMapSize = new JPanel();

    private JLabel labBoardSize = new JLabel(Messages
            .getString("BoardSelectionDialog.BoardSize"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JLabel labBoardDivider = new JLabel("x", SwingConstants.CENTER); //$NON-NLS-1$
    private JTextField texBoardWidth = new JTextField(2);
    private JTextField texBoardHeight = new JTextField(2);

    private JLabel labMapSize = new JLabel(Messages
            .getString("BoardSelectionDialog.MapSize"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JLabel labMapDivider = new JLabel("x", SwingConstants.CENTER); //$NON-NLS-1$
    private JTextField texMapWidth = new JTextField(2);
    private JTextField texMapHeight = new JTextField(2);

    private JScrollPane scrMapButtons;
    private JPanel panMapButtons = new JPanel();

    private JPanel panBoardsSelected = new JPanel();
    private JLabel labBoardsSelected = new JLabel(
            Messages.getString("BoardSelectionDialog.MapsSelected"), SwingConstants.CENTER); //$NON-NLS-1$
    private JList lisBoardsSelected = new JList(new DefaultListModel());
    private JCheckBox chkSelectAll = new JCheckBox(Messages
            .getString("BoardSelectionDialog.SelectAll")); //$NON-NLS-1$

    private JButton butChange = new JButton("<<"); //$NON-NLS-1$

    private JPanel panBoardsAvailable = new JPanel();
    private JLabel labBoardsAvailable = new JLabel(
            Messages.getString("BoardSelectionDialog.mapsAvailable"), SwingConstants.CENTER); //$NON-NLS-1$
    private JList lisBoardsAvailable = new JList(new DefaultListModel());
    private JCheckBox chkRotateBoard = new JCheckBox(Messages
            .getString("BoardSelectionDialog.RotateBoard")); //$NON-NLS-1$

    private JPanel panButtons = new JPanel();
    private JButton butUpdate = new JButton(Messages
            .getString("BoardSelectionDialog.UpdateSize")); //$NON-NLS-1$
    private JButton butRandomMap = new JButton(Messages
            .getString("BoardSelectionDialog.GeneratedMapSettings")); //$NON-NLS-1$
    private JLabel labButtonSpace = new JLabel("", SwingConstants.CENTER); //$NON-NLS-1$
    private JButton butOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JButton butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
    private JButton butPreview = new JButton(Messages
            .getString("BoardSelectionDialog.Preview")); //$NON-NLS-1$

    JDialog mapPreviewW;

    /**
     * Creates new BoardSelectionDialog
     */
    public BoardSelectionDialog(ClientGUI client) {
        super(client.frame, Messages
                .getString("BoardSelectionDialog.EditBoardLaout"), true); //$NON-NLS-1$
        this.client = client;
        mapSettings = (MapSettings) client.getClient().getMapSettings().clone();
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
        getContentPane().setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 1;
        gridbag.setConstraints(panTypeChooser, c);
        getContentPane().add(panTypeChooser);

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

        mapPreviewW = new JDialog(this.client.frame, Messages
                .getString("BoardSelectionDialog.MapPreview"), false); //$NON-NLS-1$

        mapPreviewW.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
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
        typeChooser.add(MapSettings
                .getMediumName(MapSettings.MEDIUM_ATMOSPHERE));
        typeChooser.add(MapSettings.getMediumName(MapSettings.MEDIUM_SPACE));
        refreshMapChoice();

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panTypeChooser.setLayout(gridbag);

        c.insets = new Insets(1, 1, 1, 1);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(typeChooser, c);
        panTypeChooser.add(typeChooser);

    }

    /**
     * Set up the map size panel
     */
    private void setupMapSize() {
        scrMapButtons = new JScrollPane(panMapButtons);

        refreshMapSize();
        refreshMapButtons();

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
        lisBoardsSelected.addListSelectionListener(this);
        lisBoardsSelected
                .setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        chkSelectAll.addActionListener(this);

        panBoardsSelected.setLayout(new BorderLayout());

        panBoardsSelected.add(labBoardsSelected, BorderLayout.NORTH);
        panBoardsSelected.add(new JScrollPane(lisBoardsSelected),
                BorderLayout.CENTER);
        panBoardsSelected.add(chkSelectAll, BorderLayout.SOUTH);
    }

    private void setupAvailable() {
        refreshBoardsAvailable();
        lisBoardsAvailable.addListSelectionListener(this);
        lisBoardsAvailable
                .setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        panBoardsAvailable.setLayout(new BorderLayout());

        panBoardsAvailable.add(labBoardsAvailable, BorderLayout.NORTH);
        panBoardsAvailable.add(new JScrollPane(lisBoardsAvailable),
                BorderLayout.CENTER);
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

        panMapButtons.setLayout(new GridLayout(mapSettings.getMapHeight(),
                mapSettings.getMapWidth()));

        for (int i = 0; i < mapSettings.getMapHeight(); i++) {
            for (int j = 0; j < mapSettings.getMapWidth(); j++) {
                JButton button = new JButton(Integer.toString(i
                        * mapSettings.getMapWidth() + j));
                button.addActionListener(this);
                panMapButtons.add(button);
            }
        }

        scrMapButtons.validate();
    }

    private void refreshBoardsSelected() {
        ((DefaultListModel) lisBoardsSelected.getModel()).removeAllElements();
        int index = 0;
        for (Iterator<String> i = mapSettings.getBoardsSelected(); i.hasNext();) {
            ((DefaultListModel) lisBoardsSelected.getModel())
                    .addElement(index++ + ": " + i.next()); //$NON-NLS-1$
        }
        lisBoardsSelected.setSelectedIndex(0);
        refreshSelectAllCheck();
    }

    private void refreshSelectAllCheck() {
        boolean newVal = lisBoardsSelected.getSelectedIndices().length == lisBoardsSelected
                .getModel().getSize();
        if (chkSelectAll.isSelected() != newVal) {
            chkSelectAll.setSelected(newVal);
        }
    }

    private void refreshBoardsAvailable() {
        ((DefaultListModel) lisBoardsAvailable.getModel()).removeAllElements();
        for (Iterator<String> i = mapSettings.getBoardsAvailable(); i.hasNext();) {
            ((DefaultListModel) lisBoardsAvailable.getModel()).addElement(i
                    .next());
        }
    }

    /**
     * Changes all selected boards to be the specified board
     */
    private void change(String board) {
        int[] selected = lisBoardsSelected.getSelectedIndices();
        for (final int newVar : selected) {
            String name = board;
            if (!MapSettings.BOARD_RANDOM.equals(name)
                    && !MapSettings.BOARD_SURPRISE.equals(name)
                    && chkRotateBoard.isSelected()) {
                name = Board.BOARD_REQUEST_ROTATION + name;
            }
            ((DefaultListModel) lisBoardsSelected.getModel()).setElementAt(
                    newVar + ": " + name, newVar); //$NON-NLS-1$
            mapSettings.getBoardsSelectedVector().set(newVar, name);
        }
        lisBoardsSelected.setSelectedIndices(selected);
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
            JOptionPane.showMessageDialog(client.frame, Messages
                    .getString("BoardSelectionDialog.InvalidNumberOfmaps"),
                    Messages.getString("BoardSelectionDialog.InvalidMapSize"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // check settings
        if (boardWidth <= 0 || boardHeight <= 0 || mapWidth <= 0
                || mapHeight <= 0) {
            JOptionPane.showMessageDialog(client.frame, Messages
                    .getString("BoardSelectionDialog.MapSizeMustBeGreateter0"),
                    Messages.getString("BoardSelectionDialog.InvalidMapSize"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        butOkay.setEnabled(false);

        mapSettings.setBoardSize(boardWidth, boardHeight);
        mapSettings.setMapSize(mapWidth, mapHeight);

        randomMapDialog.setMapSettings(mapSettings);

        refreshMapSize();
        refreshMapButtons();

        ((DefaultListModel) lisBoardsSelected.getModel()).removeAllElements();
        ((DefaultListModel) lisBoardsSelected.getModel()).addElement(Messages
                .getString("BoardSelectionDialog.Updating")); //$NON-NLS-1$

        ((DefaultListModel) lisBoardsAvailable.getModel()).removeAllElements();
        ((DefaultListModel) lisBoardsAvailable.getModel()).addElement(Messages
                .getString("BoardSelectionDialog.Updating")); //$NON-NLS-1$

        client.getClient().sendMapQuery(mapSettings);
    }

    /**
     * Updates to show the map settings that have, presumably, just been sent by
     * the server.
     */
    public void update(MapSettings newSettings, boolean updateSize) {
        mapSettings = (MapSettings) newSettings.clone();
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
        if (!texBoardWidth.getText().equals(
                Integer.toString(mapSettings.getBoardWidth()))
                || !texBoardHeight.getText().equals(
                        Integer.toString(mapSettings.getBoardHeight()))
                || !texMapWidth.getText().equals(
                        Integer.toString(mapSettings.getMapWidth()))
                || !texMapHeight.getText().equals(
                        Integer.toString(mapSettings.getMapHeight()))) {
            JOptionPane
                    .showMessageDialog(
                            client.frame,
                            Messages
                                    .getString("BoardSelectionDialog.UpdateMapSize.message"),
                            Messages
                                    .getString("BoardSelectionDialog.UpdateMapSize.title"),
                            JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (mapSettings.getBoardsAvailableVector().size() <= 0) {
            JOptionPane
                    .showMessageDialog(
                            client.frame,
                            Messages
                                    .getString("BoardSelectionDialog.NoBoardOfSelectedSize.message"),
                            Messages
                                    .getString("BoardSelectionDialog.NoBoardOfSelectedSize.title"),
                            JOptionPane.ERROR_MESSAGE);
            return;
        }

        // change the type - probably not the right place for this but I can't
        // get it to work elsewhere
        if (typeChooser.getSelectedIndex() == 2) {
            mapSettings.setMedium(MapSettings.MEDIUM_SPACE);
        } else if (typeChooser.getSelectedIndex() == 1) {
            mapSettings.setMedium(MapSettings.MEDIUM_ATMOSPHERE);
        } else if (typeChooser.getSelectedIndex() == 0) {
            mapSettings.setMedium(MapSettings.MEDIUM_GROUND);
        }

        client.getClient().sendMapSettings(mapSettings);
        setVisible(false);
        mapPreviewW.setVisible(false);
    }

    public void previewBoard() {
        String boardName = (String) lisBoardsAvailable.getSelectedValue();
        if (lisBoardsAvailable.getSelectedIndex() > 2) {
            IBoard board = new Board(Integer.parseInt(texBoardWidth.getText()),
                    Integer.parseInt(texBoardHeight.getText()));
            board.load(boardName + ".board");
            if (chkRotateBoard.isSelected()) {
                BoardUtilities.flip(board, true, true);
            }
            MapPreview mapPreview = null;
            try {
                mapPreview = new MapPreview(mapPreviewW, board);
                mapPreviewW.removeAll();
                mapPreviewW.add(mapPreview);
                mapPreviewW.setVisible(true);
                mapPreview.initializeMap();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource().equals(butChange)
                || e.getSource().equals(lisBoardsAvailable)) {
            if (lisBoardsAvailable.getSelectedIndex() != -1) {
                change((String) lisBoardsAvailable.getSelectedValue());
            }
        } else if (e.getSource().equals(butUpdate)) {
            apply();
        } else if (e.getSource().equals(butOkay)) {
            send();
        } else if (e.getSource().equals(butCancel)) {
            setVisible(false);
        } else if (e.getSource().equals(butRandomMap)) {
            randomMapDialog.setVisible(true);
        } else if (e.getSource().equals(butPreview)) {
            previewBoard();
        } else if (e.getSource().equals(chkSelectAll)) {
            if (!chkSelectAll.isSelected()) {
                lisBoardsSelected.setSelectedIndex(0);
                refreshSelectAllCheck();
                return;
            }
            int[] selected = new int[lisBoardsSelected.getModel().getSize()];
            for (int i = 0; i < lisBoardsSelected.getModel().getSize(); i++) {
                selected[i] = i;
            }
            lisBoardsSelected.setSelectedIndices(selected);
        } else {
            try {
                int board = Integer.parseInt(e.getActionCommand());
                lisBoardsSelected.setSelectedIndex(board);
            } catch (NumberFormatException n) {
                // ignore
            } catch (ArrayIndexOutOfBoundsException a) {
                // ignore
            }
        }
    }

    public void updateMapSettings(MapSettings newSettings) {
        mapSettings = newSettings;
        refreshMapSize();
        refreshMapButtons();

        ((DefaultListModel) lisBoardsSelected.getModel()).removeAllElements();
        ((DefaultListModel) lisBoardsSelected.getModel()).addElement(Messages
                .getString("BoardSelectionDialog.Updating")); //$NON-NLS-1$

        ((DefaultListModel) lisBoardsAvailable.getModel()).removeAllElements();
        ((DefaultListModel) lisBoardsAvailable.getModel()).addElement(Messages
                .getString("BoardSelectionDialog.Updating")); //$NON-NLS-1$

        client.getClient().sendMapQuery(newSettings);
    }

    public void valueChanged(ListSelectionEvent event) {
        if (event.getSource().equals(lisBoardsSelected)) {
            refreshSelectAllCheck();
        }
    }
}
