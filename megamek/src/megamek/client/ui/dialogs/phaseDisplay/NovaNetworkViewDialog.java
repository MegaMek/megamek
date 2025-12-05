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
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.util.UIUtil;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

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
    private static final MMLogger logger = MMLogger.create(NovaNetworkViewDialog.class);
    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final int PADDING_SMALL = UIUtil.scaleForGUI(5);

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
        logger.debug("Opening Nova CEWS Network View Dialog");
        this.clientGUI = clientGUI;
        this.game = clientGUI.getClient().getGame();
        this.localPlayerId = clientGUI.getClient().getLocalPlayer().getId();

        this.entityMap = new HashMap<>();

        initializeData();
        initializeUI();

        pack();
        setLocationRelativeTo(parent);
        logger.debug("Nova CEWS View Dialog initialized: {} player units, {} allied units",
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
        setLayout(new BorderLayout(PADDING, PADDING));
        setResizable(false);

        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(PADDING, PADDING));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        // Top: Instructions
        JLabel instructions = new JLabel(Messages.getString("NovaNetworkViewDialog.instructions"));
        instructions.setBorder(BorderFactory.createEmptyBorder(0, 0, PADDING, 0));
        mainPanel.add(instructions, BorderLayout.NORTH);

        // Center: Unit list
        JPanel centerPanel = new JPanel(new BorderLayout(PADDING_SMALL, PADDING_SMALL));

        JLabel unitListLabel = new JLabel(Messages.getString("NovaNetworkViewDialog.unitListLabel"));
        centerPanel.add(unitListLabel, BorderLayout.NORTH);

        unitListModel = new DefaultListModel<>();
        populateUnitList();

        unitList = new JList<>(unitListModel);
        unitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        unitList.setVisibleRowCount(10);

        JScrollPane scrollPane = new JScrollPane(unitList);
        scrollPane.setPreferredSize(UIUtil.scaleForGUI(500, 200));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom: Close button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));

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
            unitListModel.addElement(Messages.getString("NovaNetworkDialog.sectionHeader",
                  Messages.getString("NovaNetworkViewDialog.yourUnits")));
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
                  Messages.getString("NovaNetworkViewDialog.alliedUnits")));
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
        StringBuilder entityDisplayText = new StringBuilder();

        // ID and name
        entityDisplayText.append(Messages.getString("NovaNetworkDialog.entityIdFormat",
              entity.getId(), entity.getShortName()));

        // Current network
        String currentNetwork = entity.getC3NetId();

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
              .filter(other -> other.hasNovaCEWS())
              .filter(other -> other.getId() != entity.getId())
              .anyMatch(other -> networkId.equals(other.getC3NetId()));
    }

    /**
     * Gets the size of a network.
     */
    private int getNetworkSize(String networkId) {
        if (networkId == null) {
            return 0;
        }

        return (int) game.getEntitiesVector().stream()
              .filter(other -> other.hasNovaCEWS())
              .filter(other -> networkId.equals(other.getC3NetId()))
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
        for (Entity networkMember : game.getEntitiesVector()) {
            // Skip self
            if (networkMember.getId() == currentEntity.getId()) {
                continue;
            }
            // Check if in same network
            if (networkMember.hasNovaCEWS() && networkId.equals(networkMember.getC3NetId())) {
                memberIds.add(Messages.getString("NovaNetworkDialog.memberId", networkMember.getId()));
            }
        }

        if (memberIds.isEmpty()) {
            return Messages.getString("NovaNetworkDialog.noOtherMembers");
        } else {
            return String.join(", ", memberIds);
        }
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() == btnClose) {
            logger.debug("Closing Nova CEWS View Dialog");
            dispose();
        }
    }
}
