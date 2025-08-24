/*
 * Copyright (C) 2002-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2010-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import javax.swing.*;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.common.loaders.MapSettings;

/**
 * Display a small dialog with adjustable settings for the dimensions of the playing board
 *
 * @author Taharqa
 * @since February 12, 2010
 */
public class MapDimensionsDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = -6941422625466067948L;

    private final ClientGUI clientGUI;
    private final MapSettings mapSettings;

    private final JPanel panMapSize = new JPanel();
    private final JLabel labBoardSize = new JLabel(Messages.getString("BoardSelectionDialog.BoardSize"),
          SwingConstants.RIGHT);
    private final JLabel labBoardDivider = new JLabel("x", SwingConstants.CENTER);
    private final JTextField texBoardWidth = new JTextField(2);
    private final JTextField texBoardHeight = new JTextField(2);

    private final JLabel labMapSize = new JLabel(Messages.getString("BoardSelectionDialog.MapSize"),
          SwingConstants.RIGHT);
    private final JLabel labMapDivider = new JLabel("x", SwingConstants.CENTER);
    private JSpinner spnMapWidth = new JSpinner();
    private JSpinner spnMapHeight = new JSpinner();

    private final JPanel panButtons = new JPanel();
    private final JButton butOkay = new JButton(Messages.getString("Okay"));
    private final JButton butCancel = new JButton(Messages.getString("Cancel"));

    public MapDimensionsDialog(ClientGUI clientGUI, MapSettings mapSettings) {
        super(clientGUI.getFrame(), Messages.getString("MapDimensionsDialog.MapDimensions"), true);
        this.clientGUI = clientGUI;
        this.mapSettings = MapSettings.getInstance(mapSettings);

        setupMapSize();
        setupButtons();

        // layout
        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        getContentPane().setLayout(gridBagLayout);

        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagLayout.setConstraints(panMapSize, gridBagConstraints);
        add(panMapSize);

        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagLayout.setConstraints(panButtons, gridBagConstraints);
        add(panButtons);

        pack();
        setResizable(false);
        setLocation(clientGUI.getFrame().getLocation().x
                    + clientGUI.getFrame().getSize().width / 2 - getSize().width / 2,
              clientGUI.getFrame().getLocation().y
                    + clientGUI.getFrame().getSize().height / 2
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

        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        panMapSize.setLayout(gridBagLayout);

        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagLayout.setConstraints(labMapSize, gridBagConstraints);
        panMapSize.add(labBoardSize);

        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagLayout.setConstraints(texBoardWidth, gridBagConstraints);
        panMapSize.add(texBoardWidth);

        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagLayout.setConstraints(labBoardDivider, gridBagConstraints);
        panMapSize.add(labBoardDivider);

        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagLayout.setConstraints(texBoardHeight, gridBagConstraints);
        panMapSize.add(texBoardHeight);

        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagLayout.setConstraints(labMapSize, gridBagConstraints);
        panMapSize.add(labMapSize);

        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagLayout.setConstraints(spnMapHeight, gridBagConstraints);
        panMapSize.add(spnMapHeight);

        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagLayout.setConstraints(labMapDivider, gridBagConstraints);
        panMapSize.add(labMapDivider);

        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagLayout.setConstraints(spnMapWidth, gridBagConstraints);
        panMapSize.add(spnMapWidth);
    }

    private void setupButtons() {

        butOkay.addActionListener(this);
        butCancel.addActionListener(this);

        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        panButtons.setLayout(gridBagLayout);

        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagLayout.setConstraints(butOkay, gridBagConstraints);
        panButtons.add(butOkay);

        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        gridBagLayout.setConstraints(butCancel, gridBagConstraints);
        panButtons.add(butCancel);

    }

    /**
     * Applies the currently selected map size settings and refreshes the list of maps from the server.
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
            JOptionPane.showMessageDialog(clientGUI.getFrame(), Messages
                        .getString("BoardSelectionDialog.InvalidNumberOfMaps"),
                  Messages.getString("BoardSelectionDialog.InvalidMapSize"),
                  JOptionPane.ERROR_MESSAGE);
            return;
        }

        // check settings
        if ((boardWidth <= 0) || (boardHeight <= 0) || (mapWidth <= 0)
              || (mapHeight <= 0)) {
            JOptionPane.showMessageDialog(clientGUI.getFrame(), Messages
                        .getString("BoardSelectionDialog.MapSizeMustBeGreaterThan0"),
                  Messages.getString("BoardSelectionDialog.InvalidMapSize"),
                  JOptionPane.ERROR_MESSAGE);
            return;
        }

        mapSettings.setBoardSize(boardWidth, boardHeight);
        mapSettings.setMapSize(mapWidth, mapHeight);
        clientGUI.getClient().sendMapDimensions(mapSettings);
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
}
