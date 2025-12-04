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
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.UIUtil;
import megamek.common.enums.VariableRangeTargetingMode;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Dialog for managing Variable Range Targeting modes during End Phase. Per BMM pg. 86: Player selects
 * Long or Short mode during End Phase for the NEXT turn.
 * <p>
 * - LONG mode: -1 TN at long range, +1 TN at short range - SHORT mode: -1 TN at short range, +1 TN at long range -
 * Medium range is unaffected by either mode
 */
public class VariableRangeTargetingDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final MMLogger logger = MMLogger.create(VariableRangeTargetingDialog.class);
    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final int PADDING_SMALL = UIUtil.scaleForGUI(5);

    private final ClientGUI clientGUI;
    private final Game game;
    private final int localPlayerId;

    // UI Components
    private JButton btnApply;
    private JButton btnCancel;

    // Data structures
    private List<Entity> playerUnits;
    private final Map<Integer, JRadioButton> longModeButtons = new HashMap<>();

    public VariableRangeTargetingDialog(JFrame parent, ClientGUI clientGUI) {
        super(parent, Messages.getString("VariableRangeTargetingDialog.title"), true);
        logger.debug("Opening Variable Range Targeting Dialog");
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

        logger.debug("Variable Range Targeting Dialog initialized: {} units", playerUnits.size());
    }

    /**
     * Initializes the data structures with player's Variable Range Targeting units.
     */
    private void initializeData() {
        playerUnits = new ArrayList<>();

        for (Entity entity : game.getEntitiesVector()) {
            if (!entity.hasVariableRangeTargeting()) {
                continue;
            }

            if (entity.getOwnerId() == localPlayerId) {
                playerUnits.add(entity);
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
        JLabel instructions = new JLabel(Messages.getString("VariableRangeTargetingDialog.instructions"));
        instructions.setBorder(BorderFactory.createEmptyBorder(0, 0, PADDING, 0));
        mainPanel.add(instructions, BorderLayout.NORTH);

        // Center: Unit list with mode selection
        JPanel unitPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(PADDING_SMALL, PADDING_SMALL, PADDING_SMALL, PADDING_SMALL);
        gbc.anchor = GridBagConstraints.WEST;

        // Header row (bold headers, center aligned for columns 1 and 2)
        gbc.gridx = 0;
        gbc.gridy = 0;
        unitPanel.add(new JLabel("<html><b>"
              + Messages.getString("VariableRangeTargetingDialog.unitHeader")
              + "</b></html>"), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        unitPanel.add(new JLabel("<html><b>"
              + Messages.getString("VariableRangeTargetingDialog.currentModeHeader")
              + "</b></html>"), gbc);

        gbc.gridx = 2;
        unitPanel.add(new JLabel("<html><b>"
              + Messages.getString("VariableRangeTargetingDialog.newModeHeader")
              + "</b></html>"), gbc);
        gbc.anchor = GridBagConstraints.WEST;

        // Unit rows
        int row = 1;
        for (Entity entity : playerUnits) {
            gbc.gridy = row++;

            // Unit name
            gbc.gridx = 0;
            unitPanel.add(new JLabel(entity.getShortName()), gbc);

            // Current mode (center aligned)
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.CENTER;
            VariableRangeTargetingMode currentMode = entity.getVariableRangeTargetingMode();
            VariableRangeTargetingMode pendingMode = entity.getPendingVariableRangeTargetingMode();
            String modeDisplay = getModeDisplayText(currentMode, pendingMode);
            unitPanel.add(new JLabel(modeDisplay), gbc);

            // Mode selection (center aligned)
            gbc.gridx = 2;
            JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING_SMALL, 0));
            ButtonGroup modeGroup = new ButtonGroup();

            JRadioButton longButton = new JRadioButton(Messages.getString("VariableRangeTargetingDialog.modeLong"));
            JRadioButton shortButton = new JRadioButton(Messages.getString("VariableRangeTargetingDialog.modeShort"));

            // Set tooltips
            longButton.setToolTipText(Messages.getString("VariableRangeTargetingDialog.modeLong.tooltip"));
            shortButton.setToolTipText(Messages.getString("VariableRangeTargetingDialog.modeShort.tooltip"));

            // Add highlighting when radio button is clicked
            ActionListener highlightListener = e -> highlightEntity(entity);
            longButton.addActionListener(highlightListener);
            shortButton.addActionListener(highlightListener);

            modeGroup.add(longButton);
            modeGroup.add(shortButton);

            // Select based on pending mode or current mode
            VariableRangeTargetingMode effectiveMode = (pendingMode != null) ? pendingMode : currentMode;
            if (effectiveMode.isLong()) {
                longButton.setSelected(true);
            } else {
                shortButton.setSelected(true);
            }

            modePanel.add(longButton);
            modePanel.add(shortButton);
            unitPanel.add(modePanel, gbc);
            gbc.anchor = GridBagConstraints.WEST;

            // Store reference for apply logic
            longModeButtons.put(entity.getId(), longButton);
        }

        JScrollPane scrollPane = new JScrollPane(unitPanel);

        // Calculate dynamic height based on unit count
        int headerRowHeight = UIUtil.scaleForGUI(35);  // Column headers
        int unitRowHeight = UIUtil.scaleForGUI(30);    // Each unit row
        int contentHeight = headerRowHeight + (playerUnits.size() * unitRowHeight);

        // Clamp between min (2 units worth) and max (10 units worth)
        int minHeight = UIUtil.scaleForGUI(95);   // ~2 units
        int maxHeight = UIUtil.scaleForGUI(335);  // ~10 units, then scroll
        int scrollHeight = Math.max(minHeight, Math.min(contentHeight, maxHeight));

        scrollPane.setPreferredSize(UIUtil.scaleForGUI(600, scrollHeight));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));

        btnApply = new JButton(Messages.getString("VariableRangeTargetingDialog.btnApply"));
        btnApply.addActionListener(this);
        btnApply.setToolTipText(Messages.getString("VariableRangeTargetingDialog.btnApply.tooltip"));

        btnCancel = new JButton(Messages.getString("VariableRangeTargetingDialog.btnCancel"));
        btnCancel.addActionListener(this);

        buttonPanel.add(btnApply);
        buttonPanel.add(btnCancel);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * Gets display text for the current mode and any pending change.
     */
    private String getModeDisplayText(VariableRangeTargetingMode currentMode, VariableRangeTargetingMode pendingMode) {
        String modeText = currentMode.isLong()
              ? Messages.getString("VariableRangeTargetingDialog.modeLong")
              : Messages.getString("VariableRangeTargetingDialog.modeShort");

        if (pendingMode != null && pendingMode != currentMode) {
            String pendingText = pendingMode.isLong()
                  ? Messages.getString("VariableRangeTargetingDialog.modeLong")
                  : Messages.getString("VariableRangeTargetingDialog.modeShort");
            modeText += Messages.getString("VariableRangeTargetingDialog.modeTransition") + pendingText;
        }

        return modeText;
    }

    /**
     * Highlights the specified entity on the board view.
     */
    private void highlightEntity(Entity entity) {
        BoardView boardView = clientGUI.getBoardView();

        // Highlight entity name tag
        boardView.highlightSelectedEntities(Collections.singletonList(entity));

        // Highlight hex border
        if (entity.getPosition() != null) {
            boardView.setHighlightedEntityHexes(Collections.singletonList(entity.getPosition()));
        }

        boardView.repaint();
    }

    /**
     * Clears all entity highlighting on the board view.
     */
    private void clearHighlighting() {
        BoardView boardView = clientGUI.getBoardView();
        boardView.highlightSelectedEntities(Collections.emptyList());
        boardView.setHighlightedEntityHexes(Collections.emptyList());
        boardView.repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnApply) {
            applyChanges();
            clearHighlighting();
            dispose();
        } else if (e.getSource() == btnCancel) {
            clearHighlighting();
            dispose();
        }
    }

    /**
     * Applies the selected mode changes to all units.
     */
    private void applyChanges() {
        for (Entity entity : playerUnits) {
            int entityId = entity.getId();
            JRadioButton longButton = longModeButtons.get(entityId);

            if (longButton != null) {
                VariableRangeTargetingMode selectedMode = longButton.isSelected()
                      ? VariableRangeTargetingMode.LONG
                      : VariableRangeTargetingMode.SHORT;

                // Only send if mode is actually changing
                VariableRangeTargetingMode currentMode = entity.getVariableRangeTargetingMode();
                VariableRangeTargetingMode pendingMode = entity.getPendingVariableRangeTargetingMode();
                VariableRangeTargetingMode effectiveMode = (pendingMode != null) ? pendingMode : currentMode;

                if (selectedMode != effectiveMode) {
                    logger.debug("Entity {} changing Variable Range Targeting mode from {} to {}",
                          entityId, effectiveMode, selectedMode);
                    clientGUI.getClient().sendVariableRangeTargetingModeChange(entityId, selectedMode);
                }
            }
        }
    }
}
