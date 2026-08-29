/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.dialogs.phaseDisplay;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.util.UIUtil;
import megamek.common.game.Game;
import megamek.common.units.AutomaticEjectionRules;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Warns that the conditions will kill any crew that ejects, and lets the player switch automatic ejection off unit by
 * unit before deploying.
 * <p>
 * The setting is otherwise reachable only from the lobby's unit configuration, which cannot be opened once play has
 * begun. Without this the warning would be telling a player about a problem they had no way left to fix.
 */
public class AutomaticEjectionDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final MMLogger LOGGER = MMLogger.create(AutomaticEjectionDialog.class);
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final int PADDING_SMALL = UIUtil.scaleForGUI(5);
    private static final int HEADER_ROW_HEIGHT = 35;
    private static final int UNIT_ROW_HEIGHT = 30;
    private static final int MINIMUM_LIST_HEIGHT = 95;
    private static final int MAXIMUM_LIST_HEIGHT = 335;
    private static final int LIST_WIDTH = 520;

    private final ClientGUI clientGUI;
    private final Game game;
    private final List<Entity> unitsWithEjectionSystems;
    private final Map<Integer, JCheckBox> ejectionBoxes = new HashMap<>();
    private final Map<Integer, Boolean> settingsOnOpening = new HashMap<>();
    private final JCheckBox dontAskAgain =
          new JCheckBox(Messages.getString("AutomaticEjectionDialog.dontAskAgain"));

    private JButton deployButton;
    private JButton cancelButton;
    private boolean deploymentCancelled = true;

    /**
     * @param parent                   the frame to centre the dialog on
     * @param clientGUI                the client that any changes are sent through
     * @param unitsWithEjectionSystems the player's units that have an ejection system, whether it is on or off
     */
    public AutomaticEjectionDialog(JFrame parent, ClientGUI clientGUI, List<Entity> unitsWithEjectionSystems) {
        super(parent, Messages.getString("AutomaticEjectionDialog.title"), true);
        this.clientGUI = clientGUI;
        this.game = clientGUI.getClient().getGame();
        this.unitsWithEjectionSystems = unitsWithEjectionSystems;

        initializeUI();
        pack();
        setLocationRelativeTo(parent);
        LOGGER.debug("[EnvironmentalSealing] Automatic ejection dialog opened for {} unit(s)",
              unitsWithEjectionSystems.size());
    }

    private void initializeUI() {
        setLayout(new BorderLayout(PADDING, PADDING));
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(PADDING, PADDING));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        JLabel instructions = new JLabel(Messages.getString("AutomaticEjectionDialog.instructions"));
        instructions.setBorder(BorderFactory.createEmptyBorder(0, 0, PADDING, 0));
        mainPanel.add(instructions, BorderLayout.NORTH);

        mainPanel.add(buildUnitList(), BorderLayout.CENTER);
        mainPanel.add(buildButtonPanel(), BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * Builds the unit rows: one tick box each, showing whether that unit is currently set to eject.
     *
     * @return the scrollable list of units
     */
    private JScrollPane buildUnitList() {
        JPanel unitPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(PADDING_SMALL, PADDING_SMALL, PADDING_SMALL, PADDING_SMALL);
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridx = 0;
        constraints.gridy = 0;
        unitPanel.add(new JLabel("<html><b>"
              + Messages.getString("AutomaticEjectionDialog.unitHeader")
              + "</b></html>"), constraints);
        constraints.gridx = 1;
        unitPanel.add(new JLabel("<html><b>"
              + Messages.getString("AutomaticEjectionDialog.ejectionHeader")
              + "</b></html>"), constraints);

        int row = 1;
        for (Entity entity : unitsWithEjectionSystems) {
            constraints.gridy = row++;

            constraints.gridx = 0;
            unitPanel.add(new JLabel(entity.getShortName()), constraints);

            constraints.gridx = 1;
            boolean willEject = AutomaticEjectionRules.willEjectAutomatically(entity, game);
            JCheckBox ejectionBox = new JCheckBox(Messages.getString("AutomaticEjectionDialog.ejectAutomatically"));
            ejectionBox.setSelected(willEject);
            ejectionBox.setToolTipText(Messages.getString("AutomaticEjectionDialog.ejectAutomatically.tooltip"));
            unitPanel.add(ejectionBox, constraints);
            ejectionBoxes.put(entity.getId(), ejectionBox);
            settingsOnOpening.put(entity.getId(), willEject);
        }

        JScrollPane scrollPane = new JScrollPane(unitPanel);
        int contentHeight = HEADER_ROW_HEIGHT + (unitsWithEjectionSystems.size() * UNIT_ROW_HEIGHT);
        int listHeight = Math.clamp(contentHeight, MINIMUM_LIST_HEIGHT, MAXIMUM_LIST_HEIGHT);
        scrollPane.setPreferredSize(UIUtil.scaleForGUI(LIST_WIDTH, listHeight));
        return scrollPane;
    }

    /**
     * Builds the tick box that turns the warning off for good, and the two buttons under it.
     *
     * @return the panel that sits along the bottom of the dialog
     */
    private JPanel buildButtonPanel() {
        JPanel southPanel = new JPanel(new BorderLayout());

        JPanel nagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, PADDING, 0));
        nagPanel.add(dontAskAgain);
        southPanel.add(nagPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));
        deployButton = new JButton(Messages.getString("AutomaticEjectionDialog.deploy"));
        deployButton.addActionListener(this);
        cancelButton = new JButton(Messages.getString("AutomaticEjectionDialog.cancel"));
        cancelButton.addActionListener(this);
        buttonPanel.add(deployButton);
        buttonPanel.add(cancelButton);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);

        return southPanel;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == deployButton) {
            applyEjectionChanges();
            deploymentCancelled = false;
        }
        if (dontAskAgain.isSelected()) {
            GUIP.setNagForAutoEject(false);
        }
        dispose();
    }

    /**
     * Sends the server every setting the player changed, in either direction. Only what changed is sent, because the
     * server's copy of the unit is the one that decides whether a crew is thrown clear.
     */
    private void applyEjectionChanges() {
        for (Entity entity : unitsWithEjectionSystems) {
            JCheckBox ejectionBox = ejectionBoxes.get(entity.getId());
            if (ejectionBox == null) {
                continue;
            }
            boolean shouldEject = ejectionBox.isSelected();
            if (shouldEject != settingsOnOpening.get(entity.getId())) {
                LOGGER.debug("[EnvironmentalSealing] {}: automatic ejection set to {} at the player's request",
                      entity.getShortName(), shouldEject);
                clientGUI.getClient().sendEjectionSettingChange(entity.getId(), shouldEject);
            }
        }
    }

    /**
     * @return {@code true} if the player backed out rather than deploying
     */
    public boolean isDeploymentCancelled() {
        return deploymentCancelled;
    }
}
