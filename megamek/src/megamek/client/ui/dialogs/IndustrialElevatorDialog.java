/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.IndustrialElevator;

/**
 * Dialog for configuring industrial elevator terrain properties in the board editor.
 * <p>
 * Industrial elevators encode their data in the terrain's level and exits fields:
 * <ul>
 *   <li>level = shaft bottom elevation</li>
 *   <li>exits = (shaftTop &lt;&lt; 8) | capacityTens</li>
 * </ul>
 * <p>
 * This dialog provides user-friendly spinners for shaft top and capacity,
 * automatically encoding/decoding the exits value.
 */
public class IndustrialElevatorDialog extends JDialog {

    @Serial
    private static final long serialVersionUID = 1L;

    private final JSpinner spinnerShaftTop;
    private final JSpinner spinnerCapacity;

    private boolean confirmed = false;

    /**
     * Creates a new industrial elevator configuration dialog.
     *
     * @param frame The parent frame
     */
    public IndustrialElevatorDialog(JFrame frame) {
        super(frame, Messages.getString("BoardEditor.IndustrialElevatorDialog.title"), true);
        setResizable(false);

        // Create spinners
        // Shaft top: typically 0-15 (building floors); the exits encoding stores it as an unsigned
        // byte, so negative values cannot be represented (a basement shaft uses a negative bottom
        // level with a top at or above 0)
        spinnerShaftTop = new JSpinner(new SpinnerNumberModel(0, 0, IndustrialElevator.CAPACITY_MASK, 1));
        spinnerShaftTop.setToolTipText(Messages.getString("BoardEditor.IndustrialElevatorDialog.shaftTop.tooltip"));
        // Capacity: 0-2550 tons (stored as tens, so 0-255 * 10)
        spinnerCapacity = new JSpinner(new SpinnerNumberModel(100, 0, 2550, 10));
        spinnerCapacity.setToolTipText(Messages.getString("BoardEditor.IndustrialElevatorDialog.capacity.tooltip"));

        // Build the panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(
              UIUtil.scaleForGUI(10),
              UIUtil.scaleForGUI(15),
              UIUtil.scaleForGUI(10),
              UIUtil.scaleForGUI(15)));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(
              UIUtil.scaleForGUI(5),
              UIUtil.scaleForGUI(5),
              UIUtil.scaleForGUI(5),
              UIUtil.scaleForGUI(5));
        constraints.anchor = GridBagConstraints.WEST;

        // Shaft Top label and spinner
        constraints.gridx = 0;
        constraints.gridy = 0;
        mainPanel.add(new JLabel(Messages.getString("BoardEditor.IndustrialElevatorDialog.shaftTop")), constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(spinnerShaftTop, constraints);

        // Capacity label and spinner
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel(Messages.getString("BoardEditor.IndustrialElevatorDialog.capacity")), constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(spinnerCapacity, constraints);

        // Help text
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        JLabel helpLabel = new JLabel(Messages.getString("BoardEditor.IndustrialElevatorDialog.help"));
        helpLabel.setFont(helpLabel.getFont().deriveFont(helpLabel.getFont().getSize() * 0.9f));
        mainPanel.add(helpLabel, constraints);

        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton(Messages.getString("Okay"));
        JButton cancelButton = new JButton(Messages.getString("Cancel"));

        okButton.addActionListener(event -> {
            confirmed = true;
            setVisible(false);
        });
        cancelButton.addActionListener(event -> setVisible(false));

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // Layout
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(frame);
    }

    /**
     * Sets the dialog values from an encoded exits value.
     *
     * @param exits The exits value encoding shaft top and capacity
     */
    public void setExits(int exits) {
        int shaftTop = (exits >> IndustrialElevator.SHAFT_TOP_SHIFT) & IndustrialElevator.CAPACITY_MASK;
        int capacityTens = exits & IndustrialElevator.CAPACITY_MASK;
        int capacityTons = capacityTens * IndustrialElevator.CAPACITY_MULTIPLIER;

        spinnerShaftTop.setValue(shaftTop);
        spinnerCapacity.setValue(capacityTons);
    }

    /**
     * Gets the encoded exits value from the dialog settings.
     *
     * @return The exits value encoding shaft top and capacity
     */
    public int getExits() {
        int shaftTop = (int) spinnerShaftTop.getValue();
        int capacityTons = (int) spinnerCapacity.getValue();
        int capacityTens = capacityTons / IndustrialElevator.CAPACITY_MULTIPLIER;

        return (shaftTop << IndustrialElevator.SHAFT_TOP_SHIFT) | (capacityTens & IndustrialElevator.CAPACITY_MASK);
    }

    /**
     * Gets the shaft top value.
     *
     * @return The shaft top level
     */
    public int getShaftTop() {
        return (int) spinnerShaftTop.getValue();
    }

    /**
     * Gets the capacity value in tons.
     *
     * @return The capacity in tons
     */
    public int getCapacityTons() {
        return (int) spinnerCapacity.getValue();
    }

    /**
     * Shows the dialog and returns whether the user confirmed.
     *
     * @return {@code true} if the user clicked OK, {@code false} if cancelled
     */
    public boolean showDialog() {
        confirmed = false;
        setVisible(true);
        return confirmed;
    }
}
