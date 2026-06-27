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

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Dialog for activating or deactivating minesweepers during the End Phase (TO:AUE p.138). Only an activated minesweeper
 * clears mines when its vehicle enters a mined hex. The change is made in the End Phase and takes effect on the next
 * turn.
 */
public class MinesweeperActivationDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final MMLogger logger = MMLogger.create(MinesweeperActivationDialog.class);
    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final int PADDING_SMALL = UIUtil.scaleForGUI(5);

    // Mode names declared in MiscType.createISMineSweeper(): setModes("On", "Off")
    private static final String MODE_ON = "On";
    private static final String MODE_OFF = "Off";
    private static final String MODE_NONE = "None";

    private final ClientGUI clientGUI;
    private final Game game;
    private final int localPlayerId;

    private JButton btnApply;
    private JButton btnCancel;

    private List<Entity> playerUnits;
    private final Map<Integer, JRadioButton> onModeButtons = new HashMap<>();

    /**
     * {@code true} once Apply sent at least one minesweeper mode change, so the caller can confirm a declaration was
     * made.
     */
    private boolean applied;

    public MinesweeperActivationDialog(JFrame parent, ClientGUI clientGUI) {
        super(parent, Messages.getString("MinesweeperActivationDialog.title"), true);
        this.clientGUI = clientGUI;
        Client client = clientGUI.getClient();
        this.game = client.getGame();
        Player localPlayer = client.getLocalPlayer();
        this.localPlayerId = (localPlayer != null) ? localPlayer.getId() : Player.PLAYER_NONE;

        initializeData();
        initializeUI();

        pack();
        setLocationRelativeTo(parent);

        // Clear highlighting when dialog is closed via X button
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                clearHighlighting();
                dispose();
            }
        });

        logger.debug("Minesweeper Activation Dialog initialized: {} units", playerUnits.size());
    }

    /**
     * Collects the local player's units that mount a minesweeper.
     */
    private void initializeData() {
        playerUnits = new ArrayList<>();

        for (Entity entity : game.getEntitiesVector()) {
            if ((entity.getOwnerId() == localPlayerId) && entity.hasMinesweeper()) {
                playerUnits.add(entity);
            }
        }
    }

    /**
     * Returns the entity's first operable minesweeper, or {@code null} if it has none.
     */
    private static @Nullable MiscMounted getMinesweeper(Entity entity) {
        for (MiscMounted mounted : entity.getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_MINESWEEPER) && mounted.isOperable()) {
                return mounted;
            }
        }
        return null;
    }

    private void initializeUI() {
        setLayout(new BorderLayout(PADDING, PADDING));
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(PADDING, PADDING));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        JLabel instructions = new JLabel(Messages.getString("MinesweeperActivationDialog.instructions"));
        instructions.setBorder(BorderFactory.createEmptyBorder(0, 0, PADDING, 0));
        mainPanel.add(instructions, BorderLayout.NORTH);

        JPanel unitPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(PADDING_SMALL, PADDING_SMALL, PADDING_SMALL, PADDING_SMALL);
        constraints.anchor = GridBagConstraints.WEST;

        // Header row (markup lives in the resource bundle)
        constraints.gridx = 0;
        constraints.gridy = 0;
        unitPanel.add(new JLabel(Messages.getString("MinesweeperActivationDialog.unitHeader")), constraints);

        constraints.gridx = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        unitPanel.add(new JLabel(Messages.getString("MinesweeperActivationDialog.currentStateHeader")), constraints);

        constraints.gridx = 2;
        unitPanel.add(new JLabel(Messages.getString("MinesweeperActivationDialog.newStateHeader")), constraints);
        constraints.anchor = GridBagConstraints.WEST;

        int row = 1;
        for (Entity entity : playerUnits) {
            MiscMounted minesweeper = getMinesweeper(entity);
            if (minesweeper == null) {
                continue;
            }
            constraints.gridy = row++;

            // Unit name
            constraints.gridx = 0;
            unitPanel.add(new JLabel(entity.getShortName()), constraints);

            // Current state (center aligned)
            constraints.gridx = 1;
            constraints.anchor = GridBagConstraints.CENTER;
            unitPanel.add(new JLabel(getStateDisplayText(minesweeper)), constraints);

            // State selection (center aligned)
            constraints.gridx = 2;
            JPanel statePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING_SMALL, 0));
            ButtonGroup stateGroup = new ButtonGroup();

            JRadioButton onButton = new JRadioButton(Messages.getString("MinesweeperActivationDialog.stateOn"));
            JRadioButton offButton = new JRadioButton(Messages.getString("MinesweeperActivationDialog.stateOff"));
            onButton.setToolTipText(Messages.getString("MinesweeperActivationDialog.stateOn.tooltip"));
            offButton.setToolTipText(Messages.getString("MinesweeperActivationDialog.stateOff.tooltip"));

            ActionListener highlightListener = event -> highlightEntity(entity);
            onButton.addActionListener(highlightListener);
            offButton.addActionListener(highlightListener);

            stateGroup.add(onButton);
            stateGroup.add(offButton);

            // Select based on the pending state if any, otherwise the current state
            if (isEffectivelyOff(minesweeper)) {
                offButton.setSelected(true);
            } else {
                onButton.setSelected(true);
            }

            statePanel.add(onButton);
            statePanel.add(offButton);
            unitPanel.add(statePanel, constraints);
            constraints.anchor = GridBagConstraints.WEST;

            onModeButtons.put(entity.getId(), onButton);
        }

        JScrollPane scrollPane = new JScrollPane(unitPanel);

        int headerRowHeight = UIUtil.scaleForGUI(35);
        int unitRowHeight = UIUtil.scaleForGUI(30);
        int contentHeight = headerRowHeight + (playerUnits.size() * unitRowHeight);
        int minHeight = UIUtil.scaleForGUI(95);
        int maxHeight = UIUtil.scaleForGUI(335);
        int scrollHeight = Math.clamp(contentHeight, minHeight, maxHeight);

        scrollPane.setPreferredSize(UIUtil.scaleForGUI(600, scrollHeight));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));

        btnApply = new JButton(Messages.getString("MinesweeperActivationDialog.btnApply"));
        btnApply.addActionListener(this);
        btnApply.setToolTipText(Messages.getString("MinesweeperActivationDialog.btnApply.tooltip"));

        btnCancel = new JButton(Messages.getString("MinesweeperActivationDialog.btnCancel"));
        btnCancel.addActionListener(this);

        buttonPanel.add(btnApply);
        buttonPanel.add(btnCancel);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * @return {@code true} if the minesweeper's effective state (pending if set, otherwise current) is Off.
     */
    private static boolean isEffectivelyOff(MiscMounted minesweeper) {
        if (!minesweeper.pendingMode().equals(MODE_NONE)) {
            return minesweeper.pendingMode().isOff();
        }
        return minesweeper.curMode().isOff();
    }

    /**
     * Builds the "current -> pending" state text for a minesweeper.
     */
    private String getStateDisplayText(MiscMounted minesweeper) {
        String current = minesweeper.curMode().isOff()
              ? Messages.getString("MinesweeperActivationDialog.stateOff")
              : Messages.getString("MinesweeperActivationDialog.stateOn");

        boolean hasPending = !minesweeper.pendingMode().equals(MODE_NONE)
              && !minesweeper.pendingMode().equals(minesweeper.curMode().getName());
        if (hasPending) {
            String pending = minesweeper.pendingMode().isOff()
                  ? Messages.getString("MinesweeperActivationDialog.stateOff")
                  : Messages.getString("MinesweeperActivationDialog.stateOn");
            current += Messages.getString("MinesweeperActivationDialog.stateTransition") + pending;
        }
        return current;
    }

    private void highlightEntity(Entity entity) {
        BoardView boardView = clientGUI.getBoardView();
        boardView.highlightSelectedEntities(Collections.singletonList(entity));
        if (entity.getPosition() != null) {
            boardView.setHighlightedEntityHexes(Collections.singletonList(entity.getPosition()));
        }
        boardView.repaint();
    }

    private void clearHighlighting() {
        BoardView boardView = clientGUI.getBoardView();
        boardView.highlightSelectedEntities(Collections.emptyList());
        boardView.setHighlightedEntityHexes(Collections.emptyList());
        boardView.repaint();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == btnApply) {
            applyChanges();
            clearHighlighting();
            dispose();
        } else if (event.getSource() == btnCancel) {
            clearHighlighting();
            dispose();
        }
    }

    /**
     * Sends a mode change for every minesweeper whose selected state differs from its effective state. The server
     * applies it as a pending mode that takes effect next turn (instantModeSwitch = false).
     */
    private void applyChanges() {
        for (Entity entity : playerUnits) {
            JRadioButton onButton = onModeButtons.get(entity.getId());
            MiscMounted minesweeper = getMinesweeper(entity);
            if ((onButton == null) || (minesweeper == null)) {
                continue;
            }

            boolean selectOff = !onButton.isSelected();
            if (selectOff == isEffectivelyOff(minesweeper)) {
                continue; // no change
            }

            // Derive the mode index from its name so the dialog never couples to the declared mode order.
            int newMode = minesweeper.setMode(selectOff ? MODE_OFF : MODE_ON);
            if (newMode != -1) {
                logger.debug("Entity {} switching minesweeper to {}", entity.getId(), selectOff ? MODE_OFF : MODE_ON);
                clientGUI.getClient().sendModeChange(entity.getId(), entity.getEquipmentNum(minesweeper), newMode);
                applied = true;
            }
        }
    }

    /**
     * @return {@code true} if Apply sent at least one minesweeper mode change this time the dialog was shown
     */
    public boolean wasApplied() {
        return applied;
    }
}
