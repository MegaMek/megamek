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
import java.util.stream.Collectors;

/**
 * Dialog for managing Nova CEWS networks during End Phase.
 * Per IO p.197: "A unit wishing to link with another unit must declare the
 * connection in the End Phase. Beginning in the next turn, the two units
 * are linked and operate per the rules for C3i."
 *
 * @author MegaMek Team (with Claude Code assistance)
 */
public class NovaNetworkDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LogManager.getLogger();

    private final ClientGUI clientGUI;
    private final Game game;
    private final int localPlayerId;

    // UI Components
    private JList<String> unitList;
    private DefaultListModel<String> unitListModel;
    private JTextArea pendingChangesArea;
    private JButton btnLink;
    private JButton btnUnlink;
    private JButton btnCancel;

    // Data structures
    private List<Entity> playerNovaUnits;
    private List<Entity> alliedNovaUnits;
    private Map<Integer, Entity> entityMap; // Index to Entity mapping

    public NovaNetworkDialog(JFrame parent, ClientGUI clientGUI) {
        super(parent, Messages.getString("NovaNetworkDialog.title"), true);
        LOGGER.info("Opening Nova CEWS Network Management Dialog");
        this.clientGUI = clientGUI;
        this.game = clientGUI.getClient().getGame();
        this.localPlayerId = clientGUI.getClient().getLocalPlayer().getId();

        this.entityMap = new HashMap<>();

        initializeData();
        initializeUI();
        updatePendingChanges();

        pack();
        setLocationRelativeTo(parent);
        LOGGER.debug("Nova CEWS Dialog initialized: {} player units, {} allied units",
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
                // Allied units (can link with teammates per /nova command)
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
        JLabel instructions = new JLabel(Messages.getString("NovaNetworkDialog.instructions"));
        instructions.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        mainPanel.add(instructions, BorderLayout.NORTH);

        // Center: Unit list
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        JLabel unitListLabel = new JLabel(Messages.getString("NovaNetworkDialog.unitListLabel"));
        centerPanel.add(unitListLabel, BorderLayout.NORTH);

        unitListModel = new DefaultListModel<>();
        populateUnitList();

        unitList = new JList<>(unitListModel);
        unitList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        unitList.setVisibleRowCount(10);

        JScrollPane scrollPane = new JScrollPane(unitList);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom: Pending changes and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        // Pending changes area
        JLabel pendingLabel = new JLabel(Messages.getString("NovaNetworkDialog.pendingLabel"));
        bottomPanel.add(pendingLabel, BorderLayout.NORTH);

        pendingChangesArea = new JTextArea(4, 50);
        pendingChangesArea.setEditable(false);
        pendingChangesArea.setLineWrap(true);
        pendingChangesArea.setWrapStyleWord(true);
        pendingChangesArea.setBackground(getBackground());

        JScrollPane pendingScrollPane = new JScrollPane(pendingChangesArea);
        bottomPanel.add(pendingScrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        btnLink = new JButton(Messages.getString("NovaNetworkDialog.btnLink"));
        btnLink.addActionListener(this);
        btnLink.setToolTipText(Messages.getString("NovaNetworkDialog.btnLink.tooltip"));

        btnUnlink = new JButton(Messages.getString("NovaNetworkDialog.btnUnlink"));
        btnUnlink.addActionListener(this);
        btnUnlink.setToolTipText(Messages.getString("NovaNetworkDialog.btnUnlink.tooltip"));

        btnCancel = new JButton(Messages.getString("NovaNetworkDialog.btnCancel"));
        btnCancel.addActionListener(this);

        buttonPanel.add(btnLink);
        buttonPanel.add(btnUnlink);
        buttonPanel.add(btnCancel);

        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

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
            unitListModel.addElement("=== " + Messages.getString("NovaNetworkDialog.yourUnits") + " ===");
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

            unitListModel.addElement("=== " + Messages.getString("NovaNetworkDialog.alliedUnits") + " ===");
            entityMap.put(index++, null); // Header, no entity

            for (Entity entity : alliedNovaUnits) {
                String displayText = formatEntityDisplay(entity);
                unitListModel.addElement(displayText);
                entityMap.put(index++, entity);
            }
        }
    }

    /**
     * Formats an entity for display in the list.
     */
    private String formatEntityDisplay(Entity entity) {
        StringBuilder sb = new StringBuilder();

        // ID and name
        sb.append("ID ").append(entity.getId()).append(": ").append(entity.getShortName());

        // Current network
        String currentNetwork = entity.getC3NetId();
        String originalNetwork = entity.getOriginalNovaC3NetId();
        String pendingNetwork = entity.getNewRoundNovaNetworkString();

        LOGGER.debug("Entity {}: currentNetwork={}, originalNetwork={}, pendingNetwork={}, isNetworked={}",
            entity.getId(), currentNetwork, originalNetwork, pendingNetwork, isEntityNetworked(entity));

        if (isEntityNetworked(entity)) {
            int networkSize = getNetworkSize(currentNetwork);
            sb.append(" [Network: ").append(currentNetwork);
            sb.append(" (").append(networkSize).append("/3)]");
        } else {
            sb.append(" [Unlinked]");
        }

        // Show if pending change
        if (pendingNetwork != null && !pendingNetwork.equals(currentNetwork)) {
            sb.append(" → [Next turn: ");
            if (pendingNetwork.equals(entity.getOriginalNovaC3NetId())) {
                sb.append("Unlinked");
            } else {
                sb.append(pendingNetwork);
            }
            sb.append("]");
        }

        // Owner info for allied units
        if (entity.getOwnerId() != localPlayerId) {
            sb.append(" (").append(game.getPlayer(entity.getOwnerId()).getName()).append(")");
        }

        return sb.toString();
    }

    /**
     * Checks if an entity is currently networked with other units.
     */
    private boolean isEntityNetworked(Entity entity) {
        String networkId = entity.getC3NetId();
        if (networkId == null || networkId.equals(entity.getOriginalNovaC3NetId())) {
            return false; // Using own network ID = unlinked
        }

        // Check if at least one other unit shares this network
        return game.getEntitiesVector().stream()
                .filter(e -> e.hasActiveNovaCEWS())
                .filter(e -> e.getId() != entity.getId())
                .anyMatch(e -> networkId.equals(e.getC3NetId()));
    }

    /**
     * Gets the number of units in a network.
     */
    private int getNetworkSize(String networkId) {
        return (int) game.getEntitiesVector().stream()
                .filter(e -> e.hasActiveNovaCEWS())
                .filter(e -> networkId.equals(e.getC3NetId()))
                .count();
    }

    /**
     * Updates the pending changes display area.
     */
    private void updatePendingChanges() {
        StringBuilder sb = new StringBuilder();

        List<Entity> allNovaUnits = new ArrayList<>();
        allNovaUnits.addAll(playerNovaUnits);
        allNovaUnits.addAll(alliedNovaUnits);

        boolean hasPending = false;

        for (Entity entity : allNovaUnits) {
            String currentNetwork = entity.getC3NetId();
            String pendingNetwork = entity.getNewRoundNovaNetworkString();

            if (pendingNetwork != null && !pendingNetwork.equals(currentNetwork)) {
                hasPending = true;
                sb.append("ID ").append(entity.getId()).append(" (").append(entity.getShortName()).append("): ");

                if (pendingNetwork.equals(entity.getOriginalNovaC3NetId())) {
                    sb.append("Will disconnect from network");
                } else {
                    sb.append("Will join network ").append(pendingNetwork);
                }

                sb.append("\n");
            }
        }

        if (!hasPending) {
            sb.append(Messages.getString("NovaNetworkDialog.noPendingChanges"));
        }

        pendingChangesArea.setText(sb.toString());
    }

    /**
     * Handles button clicks.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnLink) {
            linkSelectedUnits();
        } else if (e.getSource() == btnUnlink) {
            unlinkSelectedUnits();
        } else if (e.getSource() == btnCancel) {
            dispose();
        }
    }

    /**
     * Links the selected units into a network.
     */
    private void linkSelectedUnits() {
        List<Entity> selectedEntities = getSelectedEntities();
        LOGGER.info("Link action: {} units selected", selectedEntities.size());

        if (selectedEntities.isEmpty()) {
            LOGGER.warn("Link action failed: No units selected");
            showError(Messages.getString("NovaNetworkDialog.error.noSelection"));
            return;
        }

        // Validate: can only link units owned by local player
        boolean canModify = selectedEntities.stream().anyMatch(e -> e.getOwnerId() == localPlayerId);
        if (!canModify) {
            LOGGER.warn("Link action failed: No units owned by local player");
            showError(Messages.getString("NovaNetworkDialog.error.notYourUnits"));
            return;
        }

        // Determine target network: Prefer existing network, otherwise use first unit's original network
        // Find first unit that's already networked (if any)
        Entity networkedUnit = selectedEntities.stream()
            .filter(this::isEntityNetworked)
            .findFirst()
            .orElse(null);

        String targetNetworkId;
        if (networkedUnit != null) {
            // Join the existing network
            targetNetworkId = networkedUnit.getC3NetId();
            LOGGER.info("Target network ID (from existing network): {}", targetNetworkId);
        } else {
            // All units are unlinked - create new network using first unit's ID
            targetNetworkId = selectedEntities.get(0).getOriginalNovaC3NetId();
            LOGGER.info("Target network ID (new network): {}", targetNetworkId);
        }

        // Calculate resulting network size
        // Start with units already in the target network (excluding selected units that will be moved)
        long existingUnitsInNetwork = game.getEntitiesVector().stream()
            .filter(e -> e.hasActiveNovaCEWS())
            .filter(e -> targetNetworkId.equals(e.getC3NetId()))
            .filter(e -> selectedEntities.stream().noneMatch(sel -> sel.getId() == e.getId()))
            .count();

        // Add the selected units
        int resultingNetworkSize = (int) existingUnitsInNetwork + selectedEntities.size();

        LOGGER.debug("Existing units in network: {}, Selected units: {}, Resulting size: {}",
            existingUnitsInNetwork, selectedEntities.size(), resultingNetworkSize);

        // IO p.197: "link up to two other units" = max 3 total
        if (resultingNetworkSize > 3) {
            LOGGER.warn("Link action failed: Resulting network would have {} units (max 3)", resultingNetworkSize);
            showError("Cannot link: Resulting network would have " + resultingNetworkSize +
                " units. Maximum is 3 units per network (IO p.197).");
            return;
        }

        // Link all selected units to this network
        for (Entity entity : selectedEntities) {
            LOGGER.debug("Linking entity {} ({}) to network {}",
                entity.getId(), entity.getShortName(), targetNetworkId);
            entity.setNewRoundNovaNetworkString(targetNetworkId);
            clientGUI.getClient().sendNovaChange(entity.getId(), targetNetworkId);
        }

        // Refresh display
        populateUnitList();
        updatePendingChanges();

        LOGGER.info("Link action completed successfully: {} units now in network", resultingNetworkSize);
        showInfo(Messages.getString("NovaNetworkDialog.info.linked"));
    }

    /**
     * Unlinks the selected units from their networks.
     */
    private void unlinkSelectedUnits() {
        List<Entity> selectedEntities = getSelectedEntities();
        LOGGER.info("Unlink action: {} units selected", selectedEntities.size());

        if (selectedEntities.isEmpty()) {
            LOGGER.warn("Unlink action failed: No units selected");
            showError(Messages.getString("NovaNetworkDialog.error.noSelection"));
            return;
        }

        // Validate: can only modify units owned by local player
        boolean canModify = selectedEntities.stream().anyMatch(e -> e.getOwnerId() == localPlayerId);
        if (!canModify) {
            LOGGER.warn("Unlink action failed: No units owned by local player");
            showError(Messages.getString("NovaNetworkDialog.error.notYourUnits"));
            return;
        }

        // Unlink each unit to its own network
        for (Entity entity : selectedEntities) {
            String originalNetwork = entity.getOriginalNovaC3NetId();
            LOGGER.debug("Unlinking entity {} ({}) to original network {}",
                entity.getId(), entity.getShortName(), originalNetwork);
            entity.setNewRoundNovaNetworkString(originalNetwork);
            clientGUI.getClient().sendNovaChange(entity.getId(), originalNetwork);
        }

        // Refresh display
        populateUnitList();
        updatePendingChanges();

        LOGGER.info("Unlink action completed successfully");
        showInfo(Messages.getString("NovaNetworkDialog.info.unlinked"));
    }

    /**
     * Gets the entities corresponding to the selected list items.
     */
    private List<Entity> getSelectedEntities() {
        int[] selectedIndices = unitList.getSelectedIndices();
        List<Entity> entities = new ArrayList<>();

        for (int index : selectedIndices) {
            Entity entity = entityMap.get(index);
            if (entity != null) {
                entities.add(entity);
            }
        }

        return entities;
    }

    /**
     * Shows an error dialog.
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message,
                Messages.getString("NovaNetworkDialog.error.title"),
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows an info dialog.
     */
    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message,
                Messages.getString("NovaNetworkDialog.info.title"),
                JOptionPane.INFORMATION_MESSAGE);
    }
}
