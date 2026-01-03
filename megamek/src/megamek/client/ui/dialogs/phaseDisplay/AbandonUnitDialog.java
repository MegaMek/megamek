/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
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
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.UIUtil;
import megamek.common.game.Game;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;

/**
 * Dialog for selecting units to abandon during End Phase.
 * <p>
 * Per TW/TacOps rules: - Meks must be prone and shutdown; crew exits during End Phase of following turn - Vehicles can
 * be abandoned at any time; crew exits during End Phase of following turn
 * <p>
 * Both leave the unit intact (unlike ejection which destroys Mek cockpit).
 */
public class AbandonUnitDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final MMLogger logger = MMLogger.create(AbandonUnitDialog.class);
    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final int PADDING_SMALL = UIUtil.scaleForGUI(5);

    private final ClientGUI clientGUI;
    private final Game game;
    private final int localPlayerId;

    // UI Components
    private JButton btnConfirm;
    private JButton btnCancel;

    // Data structures
    private List<Entity> abandonableUnits;
    private final Map<Integer, JCheckBox> unitCheckboxes = new HashMap<>();

    public AbandonUnitDialog(JFrame parent, ClientGUI clientGUI) {
        super(parent, Messages.getString("AbandonUnitDialog.title"), true);
        logger.debug("Opening Abandon Unit Dialog");
        this.clientGUI = clientGUI;
        this.game = clientGUI.getClient().getGame();
        this.localPlayerId = clientGUI.getClient().getLocalPlayer().getId();

        initializeData();
        initializeUI();

        pack();
        setLocationRelativeTo(parent);

        // Clear highlighting when dialog is closed via X button
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                clearHighlighting();
            }
        });

        logger.debug("Abandon Unit Dialog initialized: {} units", abandonableUnits.size());
    }

    /**
     * Initializes the data structures with player's abandonable units.
     */
    private void initializeData() {
        abandonableUnits = new ArrayList<>();

        for (Entity entity : game.getEntitiesVector()) {
            if (entity.getOwnerId() != localPlayerId) {
                continue;
            }

            // Check if this unit can be abandoned
            if (entity instanceof Mek mek && mek.canAbandon()) {
                abandonableUnits.add(mek);
            } else if (entity instanceof Tank tank && tank.canAbandon()) {
                abandonableUnits.add(tank);
            }
        }
    }

    /**
     * Initializes the UI components.
     */
    private void initializeUI() {
        setLayout(new BorderLayout(PADDING, PADDING));
        setResizable(false);

        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(PADDING, PADDING));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        // Top: Instructions
        JLabel instructions = new JLabel(Messages.getString("AbandonUnitDialog.instructions"));
        instructions.setBorder(BorderFactory.createEmptyBorder(0, 0, PADDING, 0));
        mainPanel.add(instructions, BorderLayout.NORTH);

        // Center: Unit list with checkboxes
        JPanel unitPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(PADDING_SMALL, PADDING_SMALL, PADDING_SMALL, PADDING_SMALL);
        gbc.anchor = GridBagConstraints.WEST;

        // Header row
        gbc.gridx = 0;
        gbc.gridy = 0;
        unitPanel.add(new JLabel("<html><b>"
              + Messages.getString("AbandonUnitDialog.selectHeader")
              + "</b></html>"), gbc);

        gbc.gridx = 1;
        unitPanel.add(new JLabel("<html><b>"
              + Messages.getString("AbandonUnitDialog.unitHeader")
              + "</b></html>"), gbc);

        gbc.gridx = 2;
        unitPanel.add(new JLabel("<html><b>"
              + Messages.getString("AbandonUnitDialog.crewHeader")
              + "</b></html>"), gbc);

        // Unit rows
        int row = 1;
        for (Entity unit : abandonableUnits) {
            gbc.gridy = row++;

            // Checkbox
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.CENTER;
            JCheckBox checkbox = new JCheckBox();
            checkbox.addActionListener(e -> highlightEntity(unit));
            unitPanel.add(checkbox, gbc);
            unitCheckboxes.put(unit.getId(), checkbox);

            // Unit name
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            JLabel nameLabel = new JLabel(unit.getShortName());
            unitPanel.add(nameLabel, gbc);

            // Crew name(s)
            gbc.gridx = 2;
            String crewNames = getCrewNames(unit);
            JLabel crewLabel = new JLabel(crewNames);
            unitPanel.add(crewLabel, gbc);
        }

        JScrollPane scrollPane = new JScrollPane(unitPanel);

        // Calculate dynamic height based on unit count
        int headerRowHeight = UIUtil.scaleForGUI(35);
        int unitRowHeight = UIUtil.scaleForGUI(30);
        int contentHeight = headerRowHeight + (abandonableUnits.size() * unitRowHeight);

        // Clamp between min and max
        int minHeight = UIUtil.scaleForGUI(95);
        int maxHeight = UIUtil.scaleForGUI(335);
        int scrollHeight = Math.max(minHeight, Math.min(contentHeight, maxHeight));

        scrollPane.setPreferredSize(UIUtil.scaleForGUI(500, scrollHeight));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));

        btnConfirm = new JButton(Messages.getString("AbandonUnitDialog.btnConfirm"));
        btnConfirm.addActionListener(this);
        btnConfirm.setToolTipText(Messages.getString("AbandonUnitDialog.btnConfirm.tooltip"));

        btnCancel = new JButton(Messages.getString("AbandonUnitDialog.btnCancel"));
        btnCancel.addActionListener(this);

        buttonPanel.add(btnConfirm);
        buttonPanel.add(btnCancel);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * Gets a display string of all crew names for an entity.
     */
    private String getCrewNames(Entity entity) {
        Crew crew = entity.getCrew();
        if (crew == null) {
            return "";
        }

        int crewCount = crew.getSlotCount();

        if (crewCount == 1) {
            return crew.getName();
        }

        StringBuilder names = new StringBuilder();
        for (int i = 0; i < crewCount; i++) {
            if (i > 0) {
                names.append(", ");
            }
            names.append(crew.getName(i));
        }
        return names.toString();
    }

    /**
     * Highlights the specified entity on the board view.
     */
    private void highlightEntity(Entity entity) {
        BoardView boardView = clientGUI.getBoardView();

        boardView.highlightSelectedEntities(Collections.singletonList(entity));

        if (entity.getPosition() != null) {
            boardView.setHighlightedEntityHexes(Collections.singletonList(entity.getPosition()));
        }

        boardView.repaint();
    }

    /**
     * Clears all highlighting from the board view.
     */
    private void clearHighlighting() {
        BoardView boardView = clientGUI.getBoardView();
        boardView.highlightSelectedEntities(Collections.emptyList());
        boardView.setHighlightedEntityHexes(Collections.emptyList());
        boardView.repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnConfirm) {
            // Send abandonment announcements for all selected units
            for (Map.Entry<Integer, JCheckBox> entry : unitCheckboxes.entrySet()) {
                if (entry.getValue().isSelected()) {
                    clientGUI.getClient().sendUnitAbandonmentAnnouncement(entry.getKey());
                    logger.debug("Sent abandonment announcement for unit ID: {}", entry.getKey());
                }
            }
            clearHighlighting();
            dispose();
        } else if (e.getSource() == btnCancel) {
            clearHighlighting();
            dispose();
        }
    }
}
