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
        // Shaft top: typically 0-15 (building floors)
        spinnerShaftTop = new JSpinner(new SpinnerNumberModel(0, -100, 100, 1));
        // Capacity: 0-2550 tons (stored as tens, so 0-255 * 10)
        spinnerCapacity = new JSpinner(new SpinnerNumberModel(100, 0, 2550, 10));

        // Build the panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(
              UIUtil.scaleForGUI(10),
              UIUtil.scaleForGUI(15),
              UIUtil.scaleForGUI(10),
              UIUtil.scaleForGUI(15)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(
              UIUtil.scaleForGUI(5),
              UIUtil.scaleForGUI(5),
              UIUtil.scaleForGUI(5),
              UIUtil.scaleForGUI(5));
        gbc.anchor = GridBagConstraints.WEST;

        // Shaft Top label and spinner
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel(Messages.getString("BoardEditor.IndustrialElevatorDialog.shaftTop")), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(spinnerShaftTop, gbc);

        // Capacity label and spinner
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel(Messages.getString("BoardEditor.IndustrialElevatorDialog.capacity")), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(spinnerCapacity, gbc);

        // Help text
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel helpLabel = new JLabel(Messages.getString("BoardEditor.IndustrialElevatorDialog.help"));
        helpLabel.setFont(helpLabel.getFont().deriveFont(helpLabel.getFont().getSize() * 0.9f));
        mainPanel.add(helpLabel, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton butOk = new JButton(Messages.getString("Okay"));
        JButton butCancel = new JButton(Messages.getString("Cancel"));

        butOk.addActionListener(e -> {
            confirmed = true;
            setVisible(false);
        });
        butCancel.addActionListener(e -> setVisible(false));

        buttonPanel.add(butOk);
        buttonPanel.add(butCancel);

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
        int shaftTop = (exits >> 8) & 0xFF;
        int capacityTens = exits & 0xFF;
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
        int shaftTop = (Integer) spinnerShaftTop.getValue();
        int capacityTons = (Integer) spinnerCapacity.getValue();
        int capacityTens = capacityTons / IndustrialElevator.CAPACITY_MULTIPLIER;

        return (shaftTop << 8) | (capacityTens & 0xFF);
    }

    /**
     * Gets the shaft top value.
     *
     * @return The shaft top level
     */
    public int getShaftTop() {
        return (Integer) spinnerShaftTop.getValue();
    }

    /**
     * Gets the capacity value in tons.
     *
     * @return The capacity in tons
     */
    public int getCapacityTons() {
        return (Integer) spinnerCapacity.getValue();
    }

    /**
     * Shows the dialog and returns whether the user confirmed.
     *
     * @return true if the user clicked OK, false if cancelled
     */
    public boolean showDialog() {
        confirmed = false;
        setVisible(true);
        return confirmed;
    }
}
