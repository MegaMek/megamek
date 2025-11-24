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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.*;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Dialog for managing Nova CEWS networks during End Phase.
 * Per IO: Alternate Eras p.60: "A unit wishing to link with another unit must declare the
 * connection in the End Phase. Beginning in the next turn, the two units
 * are linked and operate per the rules for C3i."
 *
 * @author MegaMek Team (with Claude Code assistance)
 */
public class NovaNetworkDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final MMLogger logger = MMLogger.create(NovaNetworkDialog.class);
    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final int PADDING_SMALL = UIUtil.scaleForGUI(5);

    private final ClientGUI clientGUI;
    private final Game game;
    private final int localPlayerId;

    // UI Components
    private JList<String> unitList;
    private DefaultListModel<String> unitListModel;
    private JTextArea pendingChangesArea;
    private JButton btnLink;
    private JButton btnUnlink;
    private JButton btnApply;
    private JButton btnRevert;
    private JButton btnCancel;

    // Data structures
    private List<Entity> playerNovaUnits;
    private List<Entity> alliedNovaUnits;
    private Map<Integer, Entity> entityMap; // Index to Entity mapping
    private Map<Integer, String> pendingChanges = new HashMap<>(); // Entity ID -> target network ID

    public NovaNetworkDialog(JFrame parent, ClientGUI clientGUI) {
        super(parent, Messages.getString("NovaNetworkDialog.title"), true);
        logger.debug("Opening Nova CEWS Network Management Dialog");
        this.clientGUI = clientGUI;
        this.game = clientGUI.getClient().getGame();
        this.localPlayerId = clientGUI.getClient().getLocalPlayer().getId();

        this.entityMap = new HashMap<>();

        initializeData();
        initializeUI();
        updatePendingChanges();

        pack();
        setLocationRelativeTo(parent);

        // Add window listener to clear highlighting when dialog closes
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                clearHighlighting();
            }
        });

        logger.debug("Nova CEWS Dialog initialized: {} player units, {} allied units",
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
        setLayout(new BorderLayout(PADDING, PADDING));
        setResizable(false);

        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(PADDING, PADDING));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        // Top: Instructions
        JLabel instructions = new JLabel(Messages.getString("NovaNetworkDialog.instructions"));
        instructions.setBorder(BorderFactory.createEmptyBorder(0, 0, PADDING, 0));
        mainPanel.add(instructions, BorderLayout.NORTH);

        // Center: Unit list
        JPanel centerPanel = new JPanel(new BorderLayout(PADDING_SMALL, PADDING_SMALL));

        JLabel unitListLabel = new JLabel(Messages.getString("NovaNetworkDialog.unitListLabel"));
        centerPanel.add(unitListLabel, BorderLayout.NORTH);

        unitListModel = new DefaultListModel<>();
        populateUnitList();

        unitList = new JList<>(unitListModel);
        unitList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        unitList.setVisibleRowCount(10);
        unitList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateEntityHighlighting();
            }
        });

        JScrollPane scrollPane = new JScrollPane(unitList);
        scrollPane.setPreferredSize(UIUtil.scaleForGUI(500, 200));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom: Pending changes and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout(PADDING_SMALL, PADDING_SMALL));

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
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));

        btnLink = new JButton(Messages.getString("NovaNetworkDialog.btnLink"));
        btnLink.addActionListener(this);
        btnLink.setToolTipText(Messages.getString("NovaNetworkDialog.btnLink.tooltip"));

        btnUnlink = new JButton(Messages.getString("NovaNetworkDialog.btnUnlink"));
        btnUnlink.addActionListener(this);
        btnUnlink.setToolTipText(Messages.getString("NovaNetworkDialog.btnUnlink.tooltip"));

        btnApply = new JButton(Messages.getString("NovaNetworkDialog.btnApply"));
        btnApply.addActionListener(this);
        btnApply.setToolTipText(Messages.getString("NovaNetworkDialog.btnApply.tooltip"));

        btnRevert = new JButton(Messages.getString("NovaNetworkDialog.btnRevert"));
        btnRevert.addActionListener(this);
        btnRevert.setToolTipText(Messages.getString("NovaNetworkDialog.btnRevert.tooltip"));

        btnCancel = new JButton(Messages.getString("NovaNetworkDialog.btnCancel"));
        btnCancel.addActionListener(this);

        buttonPanel.add(btnLink);
        buttonPanel.add(btnUnlink);
        buttonPanel.add(btnApply);
        buttonPanel.add(btnRevert);
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
            unitListModel.addElement(Messages.getString("NovaNetworkDialog.sectionHeader",
                  Messages.getString("NovaNetworkDialog.yourUnits")));
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

            unitListModel.addElement(Messages.getString("NovaNetworkDialog.sectionHeader",
                  Messages.getString("NovaNetworkDialog.alliedUnits")));
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
        StringBuilder entityDisplayText = new StringBuilder();

        // ID and name
        entityDisplayText.append(Messages.getString("NovaNetworkDialog.entityIdFormat",
              entity.getId(), entity.getShortName()));

        // Current network
        String currentNetwork = entity.getC3NetId();
        String originalNetwork = entity.getOriginalNovaC3NetId();
        String pendingNetwork = entity.getNewRoundNovaNetworkString();

        logger.debug("Entity {}: currentNetwork={}, originalNetwork={}, pendingNetwork={}, isNetworked={}",
            entity.getId(), currentNetwork, originalNetwork, pendingNetwork, isEntityNetworked(entity));

        if (isEntityNetworked(entity)) {
            int networkSize = getNetworkSize(currentNetwork);
            String networkMembers = getNetworkMembersDisplay(entity, currentNetwork);
            int freeNodes = 3 - networkSize;
            String availability = (freeNodes == 0)
                  ? Messages.getString("NovaNetworkDialog.networkFull")
                  : Messages.getString("NovaNetworkDialog.networkAvailable");

            entityDisplayText.append(Messages.getString("NovaNetworkDialog.networkStatus",
                  networkMembers, networkSize, availability));
        } else {
            entityDisplayText.append(Messages.getString("NovaNetworkDialog.unlinked"));
        }

        // Show if pending change
        if (pendingNetwork != null && !pendingNetwork.equals(currentNetwork)) {
            String pendingDisplay;
            if (pendingNetwork.equals(entity.getOriginalNovaC3NetId())) {
                pendingDisplay = Messages.getString("NovaNetworkDialog.pendingUnlinked");
            } else {
                pendingDisplay = getNetworkMembersDisplay(entity, pendingNetwork);
            }
            entityDisplayText.append(Messages.getString("NovaNetworkDialog.pendingChange", pendingDisplay));
        }

        // Owner info for allied units
        if (entity.getOwnerId() != localPlayerId) {
            entityDisplayText.append(Messages.getString("NovaNetworkDialog.ownerSuffix",
                  game.getPlayer(entity.getOwnerId()).getName()));
        }

        return entityDisplayText.toString();
    }

    /**
     * Checks if an entity is currently networked with other units.
     * An entity is considered networked if at least one other Nova CEWS unit
     * shares its network ID.
     */
    private boolean isEntityNetworked(Entity entity) {
        String networkId = entity.getC3NetId();
        if (networkId == null) {
            return false;
        }

        // Entity is networked if at least one other unit shares this network ID
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
     * Gets a human-readable display of network members (excluding the current entity).
     * Returns a comma-separated list of IDs like "ID2, ID3" or "No other members".
     */
    private String getNetworkMembersDisplay(Entity currentEntity, String networkId) {
        if (networkId == null) {
            return Messages.getString("NovaNetworkDialog.unknownNetwork");
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
                memberIds.add(Messages.getString("NovaNetworkDialog.memberId", entity.getId()));
            }
        }

        if (memberIds.isEmpty()) {
            return Messages.getString("NovaNetworkDialog.noOtherMembers");
        } else {
            return String.join(", ", memberIds);
        }
    }

    /**
     * Updates the pending changes display area.
     */
    private void updatePendingChanges() {
        StringBuilder pendingChangesText = new StringBuilder();
        boolean hasPending = false;

        for (Map.Entry<Integer, String> entry : pendingChanges.entrySet()) {
            Entity entity = game.getEntity(entry.getKey());
            if (entity != null) {
                String currentNetwork = entity.getC3NetId();
                String targetNetwork = entry.getValue();

                if (!currentNetwork.equals(targetNetwork)) {
                    hasPending = true;
                    pendingChangesText.append(Messages.getString("NovaNetworkDialog.pendingChangeFormat",
                          entity.getShortName(),
                          getNetworkDisplayName(currentNetwork),
                          getNetworkDisplayName(targetNetwork)));
                    pendingChangesText.append("\n");
                }
            }
        }

        if (hasPending) {
            pendingChangesArea.setText(pendingChangesText.toString());
        } else {
            pendingChangesArea.setText(Messages.getString("NovaNetworkDialog.noPendingChanges"));
        }
    }

    /**
     * Gets a user-friendly display name for a network ID.
     *
     * @param networkId The network ID (e.g., "C3Nova.5")
     * @return User-friendly display name
     */
    private String getNetworkDisplayName(String networkId) {
        if (networkId.contains(".")) {
            String idPart = networkId.substring(networkId.indexOf('.') + 1);
            try {
                int entityId = Integer.parseInt(idPart);
                Entity entity = game.getEntity(entityId);
                if (entity != null) {
                    return Messages.getString("NovaNetworkDialog.networkOf", entity.getShortName());
                }
            } catch (NumberFormatException ignored) {
                // Fall through to default
            }
        }
        return networkId;
    }

    /**
     * Handles button clicks.
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() == btnLink) {
            linkSelectedUnits();
        } else if (actionEvent.getSource() == btnUnlink) {
            unlinkSelectedUnits();
        } else if (actionEvent.getSource() == btnApply) {
            applyPendingChanges();
        } else if (actionEvent.getSource() == btnRevert) {
            revertPendingChanges();
        } else if (actionEvent.getSource() == btnCancel) {
            if (!pendingChanges.isEmpty()) {
                int result = JOptionPane.showConfirmDialog(this,
                    Messages.getString("NovaNetworkDialog.discardChanges"),
                    Messages.getString("NovaNetworkDialog.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

                if (result != JOptionPane.YES_OPTION) {
                    return;  // Don't close if user cancels
                }
            }

            pendingChanges.clear();
            clearHighlighting();
            dispose();
        }
    }

    /**
     * Links the selected units into a network.
     */
    private void linkSelectedUnits() {
        List<Entity> selectedEntities = getSelectedEntities();
        logger.debug("Link action: {} units selected", selectedEntities.size());

        if (selectedEntities.isEmpty()) {
            logger.warn("Link action failed: No units selected");
            showError(Messages.getString("NovaNetworkDialog.error.noSelection"));
            return;
        }

        // Validate: can only link units owned by local player
        boolean canModify = selectedEntities.stream().anyMatch(e -> e.getOwnerId() == localPlayerId);
        if (!canModify) {
            logger.warn("Link action failed: No units owned by local player");
            showError(Messages.getString("NovaNetworkDialog.error.notYourUnits"));
            return;
        }

        // Determine target network based on selection:
        // - If ALL selected units share the same network (all networked, same network): Keep that network
        // - Otherwise (mixed networked/unlinked, different networks, all unlinked): Create new network
        logger.debug("Link action: Analyzing {} selected units", selectedEntities.size());

        String targetNetworkId;
        List<String> existingNetworks = selectedEntities.stream()
            .filter(this::isEntityNetworked)
            .map(Entity::getC3NetId)
            .distinct()
            .collect(Collectors.toList());

        long networkedCount = selectedEntities.stream()
            .filter(this::isEntityNetworked)
            .count();

        logger.debug("Selected units breakdown: {} networked, {} unlinked",
            networkedCount, selectedEntities.size() - networkedCount);
        logger.debug("Unique networks in selection: {}", existingNetworks);

        // Check if ALL selected units are networked AND in the same network
        if ((networkedCount == selectedEntities.size()) && (existingNetworks.size() == 1)) {
            // All selected units are in the same network - preserve it
            targetNetworkId = existingNetworks.get(0);
            logger.debug("Decision: Preserving existing network (all units from same network): {}", targetNetworkId);

            // Check if this is a no-op (all units already in target network)
            boolean anyChanges = selectedEntities.stream()
                .anyMatch(e -> !targetNetworkId.equals(e.getC3NetId()));

            if (!anyChanges) {
                logger.debug("No action needed: All selected units already in network {}", targetNetworkId);
                showInfo(Messages.getString("NovaNetworkDialog.info.alreadyNetworked", targetNetworkId));
                return;  // Exit without making changes
            }
        } else {
            // Mixed selection or different networks - create new network
            // Find a network ID from selected units that won't include unintended units
            targetNetworkId = selectedEntities.stream()
                .map(Entity::getOriginalNovaC3NetId)
                .filter(netId -> {
                    // Count how many units (not in selection) currently use this network
                    long othersUsingNetwork = game.getEntitiesVector().stream()
                        .filter(entity -> entity.hasActiveNovaCEWS())
                        .filter(entity -> netId.equals(entity.getC3NetId()))
                        .filter(entity -> selectedEntities.stream()
                            .noneMatch(sel -> sel.getId() == entity.getId()))
                        .count();
                    return othersUsingNetwork == 0; // No other units using it
                })
                .findFirst()
                .orElse(selectedEntities.get(0).getOriginalNovaC3NetId()); // Fallback

            logger.debug("Decision: Creating new network from available ID (mixed selection): {}", targetNetworkId);
            logger.debug("Selected network ID {} to avoid including unintended units", targetNetworkId);
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

        logger.debug("Existing units in network: {}, Selected units: {}, Resulting size: {}",
            existingUnitsInNetwork, selectedEntities.size(), resultingNetworkSize);

        // IO: Alternate Eras p.60: "link up to two other units" = max 3 total
        if (resultingNetworkSize > 3) {
            logger.warn("Link action failed: Resulting network would have {} units (max 3)", resultingNetworkSize);
            showError(Messages.getString("NovaNetworkDialog.error.tooManyUnitsResult", resultingNetworkSize));
            return;
        }

        // Queue link actions for all selected units
        for (Entity entity : selectedEntities) {
            logger.debug("Queuing link for entity {} ({}) to network {}",
                entity.getId(), entity.getShortName(), targetNetworkId);
            pendingChanges.put(entity.getId(), targetNetworkId);
        }

        // Refresh display
        populateUnitList();
        updatePendingChanges();

        logger.debug("Link action completed successfully: {} units now in network", resultingNetworkSize);
    }

    /**
     * Unlinks the selected units from their networks.
     */
    private void unlinkSelectedUnits() {
        List<Entity> selectedEntities = getSelectedEntities();
        logger.debug("Unlink action: {} units selected", selectedEntities.size());

        if (selectedEntities.isEmpty()) {
            logger.warn("Unlink action failed: No units selected");
            showError(Messages.getString("NovaNetworkDialog.error.noSelection"));
            return;
        }

        // Validate: can only modify units owned by local player
        boolean canModify = selectedEntities.stream().anyMatch(e -> e.getOwnerId() == localPlayerId);
        if (!canModify) {
            logger.warn("Unlink action failed: No units owned by local player");
            showError(Messages.getString("NovaNetworkDialog.error.notYourUnits"));
            return;
        }

        // Queue unlink actions for all selected units
        // Each unit reverts to its own network (C3Nova.X based on unit ID)
        for (Entity entity : selectedEntities) {
            String currentNetworkId = entity.getC3NetId();
            String targetNetworkId = entity.getOriginalNovaC3NetId();

            logger.debug("Queuing unlink for entity {} ({}) from network {} to original network {}",
                entity.getId(), entity.getShortName(), currentNetworkId, targetNetworkId);

            pendingChanges.put(entity.getId(), targetNetworkId);
        }

        // Refresh display
        populateUnitList();
        updatePendingChanges();

        logger.debug("Unlink action completed successfully");
    }

    /**
     * Applies all pending network changes by sending them to the server.
     */
    private void applyPendingChanges() {
        if (pendingChanges.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                Messages.getString("NovaNetworkDialog.noPendingChanges"),
                Messages.getString("NovaNetworkDialog.title"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Send all pending changes to server
        for (Map.Entry<Integer, String> entry : pendingChanges.entrySet()) {
            int entityId = entry.getKey();
            String targetNetwork = entry.getValue();
            Entity entity = game.getEntity(entityId);

            if (entity != null) {
                logger.info("Applying network change for entity {} ({}): {} -> {}",
                    entityId, entity.getShortName(),
                    entity.getC3NetId(), targetNetwork);

                entity.setNewRoundNovaNetworkString(targetNetwork);
                clientGUI.getClient().sendNovaChange(entityId, targetNetwork);
            }
        }

        pendingChanges.clear();
        clearHighlighting();
        dispose();
    }

    /**
     * Clears all pending network changes without applying them.
     */
    private void revertPendingChanges() {
        pendingChanges.clear();
        populateUnitList();
        updatePendingChanges();
        // Visual feedback in UI is sufficient - no dialog needed
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
     * Updates the board view to highlight the currently selected entities.
     * Highlights both the entity name tags and hex borders.
     */
    private void updateEntityHighlighting() {
        List<Entity> selectedEntities = getSelectedEntities();
        BoardView boardView = clientGUI.getBoardView();

        // Highlight entity name tags
        boardView.highlightSelectedEntities(selectedEntities);

        // Highlight hex borders
        List<Coords> hexesToHighlight = selectedEntities.stream()
                .map(Entity::getPosition)
                .collect(Collectors.toList());
        boardView.setHighlightedEntityHexes(hexesToHighlight);

        boardView.repaint();
    }

    /**
     * Clears all entity highlighting on the board view.
     * Clears both entity name tag highlights and hex border highlights.
     */
    private void clearHighlighting() {
        BoardView boardView = clientGUI.getBoardView();

        // Clear entity name tag highlights
        boardView.highlightSelectedEntities(new ArrayList<>());

        // Clear hex border highlights
        boardView.setHighlightedEntityHexes(new ArrayList<>());

        boardView.repaint();
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
