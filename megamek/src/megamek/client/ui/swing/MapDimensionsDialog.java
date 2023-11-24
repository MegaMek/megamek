/*
 * MegaMek - Copyright (C) 2002-2003 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.MapSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Display a small dialog with adjustable settings for the dimensions of the playing board
 * 
 * @author Taharqa
 * @since February 12, 2010
 */
public class MapDimensionsDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -6941422625466067948L;

    private ClientGUI clientGUI;
    private MapSettings mapSettings;
    
    private JPanel panMapSize = new JPanel();
    private JLabel labBoardSize = new JLabel(Messages.getString("BoardSelectionDialog.BoardSize"),
            SwingConstants.RIGHT);
    private JLabel labBoardDivider = new JLabel("x", SwingConstants.CENTER);
    private JTextField texBoardWidth = new JTextField(2);
    private JTextField texBoardHeight = new JTextField(2);

    private JLabel labMapSize = new JLabel(Messages.getString("BoardSelectionDialog.MapSize"),
            SwingConstants.RIGHT);
    private JLabel labMapDivider = new JLabel("x", SwingConstants.CENTER);
    private JSpinner spnMapWidth = new JSpinner();
    private JSpinner spnMapHeight = new JSpinner();
    
    private JPanel panButtons = new JPanel();
    private JButton butOkay = new JButton(Messages.getString("Okay"));
    private JButton butCancel = new JButton(Messages.getString("Cancel"));
    
    public MapDimensionsDialog(ClientGUI clientGUI, MapSettings mapSettings) {
        super(clientGUI.frame, Messages.getString("MapDimensionsDialog.MapDimensions"), true);
        this.clientGUI = clientGUI;
        this.mapSettings = MapSettings.getInstance(mapSettings);
     
        setupMapSize();
        setupButtons();
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        getContentPane().setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        gridbag.setConstraints(panMapSize, c);
        add(panMapSize);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        gridbag.setConstraints(panButtons, c);
        add(panButtons);
        
        pack();
        setResizable(false);
        setLocation(clientGUI.frame.getLocation().x
                + clientGUI.frame.getSize().width / 2 - getSize().width / 2,
                clientGUI.frame.getLocation().y
                        + clientGUI.frame.getSize().height / 2
                        - getSize().height / 2);
    }
    
    private void setupMapSize() {
        
        SpinnerModel boardHeightModel = new SpinnerNumberModel(mapSettings.getMapHeight(), 1, 15, 1);
        SpinnerModel boardWidthModel = new SpinnerNumberModel(mapSettings.getMapWidth(), 1, 15, 1);
        spnMapHeight = new JSpinner(boardHeightModel);
        spnMapWidth = new JSpinner(boardWidthModel);
        texBoardWidth.setText(Integer.toString(mapSettings.getBoardWidth()));
        texBoardHeight.setText(Integer.toString(mapSettings.getBoardHeight()));
        texBoardWidth.addActionListener(this);
        texBoardHeight.addActionListener(this);
        
        if (mapSettings.getMedium() == MapSettings.MEDIUM_SPACE) {
            spnMapHeight.setEnabled(false);
            spnMapWidth.setEnabled(false);
        }
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panMapSize.setLayout(gridbag);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1; 
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(labMapSize, c);
        panMapSize.add(labBoardSize);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1; 
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(texBoardWidth, c);
        panMapSize.add(texBoardWidth);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 2;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1; 
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(labBoardDivider, c);
        panMapSize.add(labBoardDivider);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 3;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1; 
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(texBoardHeight, c);
        panMapSize.add(texBoardHeight);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1; 
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(labMapSize, c);
        panMapSize.add(labMapSize);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1; 
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(spnMapHeight, c);
        panMapSize.add(spnMapHeight);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1; 
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(labMapDivider, c);
        panMapSize.add(labMapDivider);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 3;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1; 
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(spnMapWidth, c);
        panMapSize.add(spnMapWidth);
    }
    
    private void setupButtons() {
        
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panButtons.setLayout(gridbag);

        c.insets = new Insets(1, 1, 1, 1);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        gridbag.setConstraints(butOkay, c);
        panButtons.add(butOkay);

        c.insets = new Insets(1, 1, 1, 1);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        gridbag.setConstraints(butCancel, c);
        panButtons.add(butCancel);
        
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
            mapWidth = (Integer) spnMapWidth.getModel().getValue();
            mapHeight = (Integer) spnMapHeight.getModel().getValue();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(clientGUI.frame, Messages
                    .getString("BoardSelectionDialog.InvalidNumberOfMaps"),
                    Messages.getString("BoardSelectionDialog.InvalidMapSize"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // check settings
        if ((boardWidth <= 0) || (boardHeight <= 0) || (mapWidth <= 0)
                || (mapHeight <= 0)) {
            JOptionPane.showMessageDialog(clientGUI.frame, Messages
                    .getString("BoardSelectionDialog.MapSizeMustBeGreaterThan0"),
                    Messages.getString("BoardSelectionDialog.InvalidMapSize"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        mapSettings.setBoardSize(boardWidth, boardHeight);
        mapSettings.setMapSize(mapWidth, mapHeight);
        clientGUI.getClient().sendMapDimensions(mapSettings);

        for (Entity entity : clientGUI.getClient().getGame().getEntitiesVector()) {
            if (entity.getStartingAnyNWx() >= boardWidth) {
                entity.setStartingAnyNWx(boardWidth - 1);
            }
            if (entity.getStartingAnyNWy() >= boardHeight) {
                entity.setStartingAnyNWy(boardHeight - 1);
            }
            if (entity.getStartingAnySEx() >= boardWidth) {
                entity.setStartingAnySEx(boardWidth - 1);
            }
            if (entity.getStartingAnySEy() >= boardHeight) {
                entity.setStartingAnySEy(boardHeight - 1);
            }
        }
    }
    
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(butOkay)) {
            apply();
            setVisible(false);
        } else if (e.getSource().equals(butCancel)) {
            setVisible(false);
        }   
    }
    
    @Override
    public void setVisible(boolean b) {
        if (b) {
            adaptToGUIScale();;
            pack();
        }
        super.setVisible(b);
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this,  UIUtil.FONT_SCALE1);
    }
}