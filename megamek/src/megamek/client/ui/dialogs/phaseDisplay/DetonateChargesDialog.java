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
import megamek.client.ui.clientGUI.boardview.overlay.ToastLevel;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.DemolitionCharge;
import megamek.common.units.IBuilding;
import megamek.logging.MMLogger;

/**
 * Dialog for detonating demolition charges during the End Phase. Per TO:AUE p.152-153: once a Demolition Engineer
 * platoon has finished setting its charges, the controlling player may announce during any subsequent End Phase that
 * the charges are detonated, inflicting the computed damage on the rigged structure hex.
 */
public class DetonateChargesDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final MMLogger logger = MMLogger.create(DetonateChargesDialog.class);
    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final int PADDING_SMALL = UIUtil.scaleForGUI(5);

    private final ClientGUI clientGUI;
    private final Game game;
    private final int localPlayerId;

    // UI Components
    private JButton btnDetonate;
    private JButton btnCancel;

    /**
     * One row of the dialog: a placed demolition charge and the building it is rigged to.
     *
     * @param charge   the demolition charge
     * @param building the building the charge is placed in
     */
    private record ChargeEntry(DemolitionCharge charge, IBuilding building) {}

    // Data structures
    private List<ChargeEntry> playerCharges;
    private final Map<DemolitionCharge, JCheckBox> chargeCheckboxes = new HashMap<>();

    public DetonateChargesDialog(JFrame parent, ClientGUI clientGUI) {
        super(parent, Messages.getString("DetonateChargesDialog.title"), true);
        logger.debug("Opening Detonate Charges Dialog");
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

        logger.debug("Detonate Charges Dialog initialized: {} charges", playerCharges.size());
    }

    /**
     * Collects all demolition charges owned by the local player from all buildings on all boards.
     */
    private void initializeData() {
        playerCharges = new ArrayList<>();

        for (Board board : game.getBoards().values()) {
            for (IBuilding building : board.getBuildingsVector()) {
                for (DemolitionCharge charge : building.getDemolitionCharges()) {
                    if (charge.playerId == localPlayerId) {
                        playerCharges.add(new ChargeEntry(charge, building));
                    }
                }
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
        JLabel instructions = new JLabel(Messages.getString("DetonateChargesDialog.instructions"));
        instructions.setBorder(BorderFactory.createEmptyBorder(0, 0, PADDING, 0));
        mainPanel.add(instructions, BorderLayout.NORTH);

        // Center: Charge list with checkboxes
        JPanel chargePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(PADDING_SMALL, PADDING_SMALL, PADDING_SMALL, PADDING_SMALL);
        gbc.anchor = GridBagConstraints.WEST;

        // Header row
        gbc.gridx = 0;
        gbc.gridy = 0;
        chargePanel.add(new JLabel("<html><b>"
              + Messages.getString("DetonateChargesDialog.selectHeader")
              + "</b></html>"), gbc);

        gbc.gridx = 1;
        chargePanel.add(new JLabel("<html><b>"
              + Messages.getString("DetonateChargesDialog.buildingHeader")
              + "</b></html>"), gbc);

        gbc.gridx = 2;
        chargePanel.add(new JLabel("<html><b>"
              + Messages.getString("DetonateChargesDialog.hexHeader")
              + "</b></html>"), gbc);

        gbc.gridx = 3;
        chargePanel.add(new JLabel("<html><b>"
              + Messages.getString("DetonateChargesDialog.damageHeader")
              + "</b></html>"), gbc);

        // Charge rows
        int row = 1;
        for (ChargeEntry entry : playerCharges) {
            gbc.gridy = row++;

            // Checkbox
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.CENTER;
            JCheckBox checkbox = new JCheckBox();
            checkbox.addActionListener(e -> highlightSelectedCharges());
            chargePanel.add(checkbox, gbc);
            chargeCheckboxes.put(entry.charge(), checkbox);

            // Building name
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            chargePanel.add(new JLabel(entry.building().getName()), gbc);

            // Hex
            gbc.gridx = 2;
            chargePanel.add(new JLabel(entry.charge().pos.getBoardNum()), gbc);

            // Damage
            gbc.gridx = 3;
            chargePanel.add(new JLabel(Integer.toString(entry.charge().damage)), gbc);
        }

        JScrollPane scrollPane = new JScrollPane(chargePanel);

        // Calculate dynamic height based on charge count
        int headerRowHeight = UIUtil.scaleForGUI(35);
        int chargeRowHeight = UIUtil.scaleForGUI(30);
        int contentHeight = headerRowHeight + (playerCharges.size() * chargeRowHeight);

        // Clamp between min and max
        int minHeight = UIUtil.scaleForGUI(95);
        int maxHeight = UIUtil.scaleForGUI(335);
        int scrollHeight = Math.clamp(contentHeight, minHeight, maxHeight);

        scrollPane.setPreferredSize(UIUtil.scaleForGUI(500, scrollHeight));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));

        btnDetonate = new JButton(Messages.getString("DetonateChargesDialog.btnDetonate"));
        btnDetonate.addActionListener(this);
        btnDetonate.setToolTipText(Messages.getString("DetonateChargesDialog.btnDetonate.tooltip"));

        btnCancel = new JButton(Messages.getString("DetonateChargesDialog.btnCancel"));
        btnCancel.addActionListener(this);

        buttonPanel.add(btnDetonate);
        buttonPanel.add(btnCancel);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * Highlights the hexes of every currently-checked charge on the board view. The board view replaces its highlight
     * list on each call, so the full set of selected charges is rebuilt here rather than adding one hex at a time -
     * otherwise only the last-clicked charge would stay highlighted.
     */
    private void highlightSelectedCharges() {
        List<Coords> selectedHexes = new ArrayList<>();
        for (Map.Entry<DemolitionCharge, JCheckBox> entry : chargeCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedHexes.add(entry.getKey().pos);
            }
        }
        BoardView boardView = clientGUI.getBoardView();
        boardView.setDemolitionChargeHighlightHexes(selectedHexes);
        boardView.repaint();
    }

    /**
     * Clears all highlighting from the board view.
     */
    private void clearHighlighting() {
        BoardView boardView = clientGUI.getBoardView();
        boardView.setDemolitionChargeHighlightHexes(Collections.emptyList());
        boardView.repaint();
    }

    /** {@code true} once Detonate announced at least one charge, so the caller can confirm a declaration was made. */
    private boolean applied;

    /**
     * @return {@code true} if Detonate announced at least one charge this time the dialog was shown
     */
    public boolean wasApplied() {
        return applied;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnDetonate) {
            // Send detonation announcements for all selected charges; the server resolves them in the End Phase
            int announcedCharges = 0;
            for (Map.Entry<DemolitionCharge, JCheckBox> entry : chargeCheckboxes.entrySet()) {
                if (entry.getValue().isSelected()) {
                    clientGUI.getClient().sendExplodeBuilding(entry.getKey());
                    logger.debug("Sent detonation announcement for charge at {}", entry.getKey().pos);
                    announcedCharges++;
                }
            }
            if (announcedCharges > 0) {
                applied = true;
                clientGUI.addToast(ToastLevel.SUCCESS,
                      Messages.getString("DetonateChargesDialog.toast.announced", announcedCharges));
            }
            clearHighlighting();
            dispose();
        } else if (e.getSource() == btnCancel) {
            clearHighlighting();
            dispose();
        }
    }
}
