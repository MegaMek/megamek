/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.dialogs.phaseDisplay;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only dialog for viewing Nova CEWS network configurations.
 * Displays current network status without allowing modifications.
 * Can be opened at any time during the game from View menu.
 *
 * @author MegaMek Team (with Claude Code assistance)
 */
public class NovaNetworkViewDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LogManager.getLogger();

    private final ClientGUI clientGUI;
    private final Game game;
    private final int localPlayerId;

    // UI Components
    private JList<String> unitList;
    private DefaultListModel<String> unitListModel;
    private JButton btnClose;

    // Data structures
    private List<Entity> playerNovaUnits;
    private List<Entity> alliedNovaUnits;
    private Map<Integer, Entity> entityMap; // Index to Entity mapping

    public NovaNetworkViewDialog(JFrame parent, ClientGUI clientGUI) {
        super(parent, Messages.getString("NovaNetworkViewDialog.title"), true);
        LOGGER.debug("Opening Nova CEWS Network View Dialog");
        this.clientGUI = clientGUI;
        this.game = clientGUI.getClient().getGame();
        this.localPlayerId = clientGUI.getClient().getLocalPlayer().getId();

        this.entityMap = new HashMap<>();

        initializeData();
        initializeUI();

        pack();
        setLocationRelativeTo(parent);
        LOGGER.debug("Nova CEWS View Dialog initialized: {} player units, {} allied units",
            playerNovaUnits.size(), alliedNovaUnits.size());
    }

    /**
     * Initializes the data structures with player's and allied Nova CEWS units.
     */
    private void initializeData() {
        playerNovaUnits = new ArrayList<>();
        alliedNovaUnits = new ArrayList<>();

        for (Entity entity : game.getEntitiesVector()) {
            if (!entity.hasNovaCEWS()) {
                continue;
            }

            if (entity.getOwnerId() == localPlayerId) {
                playerNovaUnits.add(entity);
            } else if (!entity.getOwner().isEnemyOf(game.getPlayer(localPlayerId))) {
                // Allied units (can link with teammates per IO: AE p.60)
                alliedNovaUnits.add(entity);
            }
        }
    }

    /**
     * Initializes the UI components.
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top: Instructions
        JLabel instructions = new JLabel(Messages.getString("NovaNetworkViewDialog.instructions"));
        instructions.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        mainPanel.add(instructions, BorderLayout.NORTH);

        // Center: Unit list
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        JLabel unitListLabel = new JLabel(Messages.getString("NovaNetworkViewDialog.unitListLabel"));
        centerPanel.add(unitListLabel, BorderLayout.NORTH);

        unitListModel = new DefaultListModel<>();
        populateUnitList();

        unitList = new JList<>(unitListModel);
        unitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        unitList.setVisibleRowCount(10);

        JScrollPane scrollPane = new JScrollPane(unitList);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom: Close button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        btnClose = new JButton(Messages.getString("NovaNetworkViewDialog.btnClose"));
        btnClose.addActionListener(this);

        buttonPanel.add(btnClose);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * Populates the unit list with player's and allied Nova CEWS units.
     */
    private void populateUnitList() {
        unitListModel.clear();
        entityMap.clear();

        int index = 0;

        // Add player's units
        if (!playerNovaUnits.isEmpty()) {
            unitListModel.addElement("=== " + Messages.getString("NovaNetworkViewDialog.yourUnits") + " ===");
            entityMap.put(index++, null); // Header, no entity

            for (Entity entity : playerNovaUnits) {
                String displayText = formatEntityDisplay(entity);
                unitListModel.addElement(displayText);
                entityMap.put(index++, entity);
            }
        }

        // Add allied units
        if (!alliedNovaUnits.isEmpty()) {
            if (!playerNovaUnits.isEmpty()) {
                unitListModel.addElement(""); // Spacer
                entityMap.put(index++, null);
            }

            unitListModel.addElement("=== " + Messages.getString("NovaNetworkViewDialog.alliedUnits") + " ===");
            entityMap.put(index++, null); // Header, no entity

            for (Entity entity : alliedNovaUnits) {
                String displayText = formatEntityDisplay(entity);
                unitListModel.addElement(displayText);
                entityMap.put(index++, entity);
            }
        }
    }

    /**
     * Formats an entity for display in the list (read-only, no pending changes).
     */
    private String formatEntityDisplay(Entity entity) {
        StringBuilder sb = new StringBuilder();

        // ID and name
        sb.append("ID ").append(entity.getId()).append(": ").append(entity.getShortName());

        // Current network
        String currentNetwork = entity.getC3NetId();

        if (isEntityNetworked(entity)) {
            int networkSize = getNetworkSize(currentNetwork);
            String networkMembers = getNetworkMembersDisplay(entity, currentNetwork);
            sb.append(" [Network: ").append(networkMembers);
            sb.append(" (").append(networkSize).append("/3)]");
        } else {
            sb.append(" [Unlinked]");
        }

        // Owner info for allied units
        if (entity.getOwnerId() != localPlayerId) {
            sb.append(" (").append(game.getPlayer(entity.getOwnerId()).getName()).append(")");
        }

        return sb.toString();
    }

    /**
     * Checks if an entity is currently networked.
     */
    private boolean isEntityNetworked(Entity entity) {
        String networkId = entity.getC3NetId();
        String originalId = entity.getOriginalNovaC3NetId();

        // Networked if network ID differs from original solo ID
        return networkId != null && !networkId.equals(originalId);
    }

    /**
     * Gets the size of a network.
     */
    private int getNetworkSize(String networkId) {
        if (networkId == null) {
            return 0;
        }

        return (int) game.getEntitiesVector().stream()
                .filter(e -> e.hasNovaCEWS())
                .filter(e -> networkId.equals(e.getC3NetId()))
                .count();
    }

    /**
     * Gets a human-readable display of network members (excluding the current entity).
     * Returns a comma-separated list of IDs like "ID2, ID3" or "No other members".
     */
    private String getNetworkMembersDisplay(Entity currentEntity, String networkId) {
        if (networkId == null) {
            return "Unknown network";
        }

        // Find all OTHER entities in the same network
        List<String> memberIds = new ArrayList<>();
        for (Entity entity : game.getEntitiesVector()) {
            // Skip self
            if (entity.getId() == currentEntity.getId()) {
                continue;
            }
            // Check if in same network
            if (entity.hasNovaCEWS() && networkId.equals(entity.getC3NetId())) {
                memberIds.add("ID" + entity.getId());
            }
        }

        if (memberIds.isEmpty()) {
            return "No other members";
        } else {
            return String.join(", ", memberIds);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnClose) {
            LOGGER.debug("Closing Nova CEWS View Dialog");
            dispose();
        }
    }
}
