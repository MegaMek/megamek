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

import megamek.client.ui.Messages;
import megamek.common.SourceBook;
import megamek.common.SourceBooks;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.util.HashMap;
import java.awt.*;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

public class SourceChooserDialog {

    private static final SourceBooks SOURCE_BOOKS = new SourceBooks();
    private static final Map<String, String> BOOKS = new HashMap<>();

    /**
     * Shows a dialog where the user can select a sourcebook from a combobox. When showManualTextfield is true, the user
     * can also enter a manual value in a text field.
     *
     * @param parent              a parent frame for this dialog
     * @param showManualTextfield When true, shows the option to enter the source manually
     *
     * @return the chosen value (either from combobox or text field), or null if canceled
     */
    public static String showChoiceDialog(@Nullable Component parent, boolean showManualTextfield) {
        if (BOOKS.isEmpty()) {
            BOOKS.putAll(SOURCE_BOOKS.availableSourcebooks()
                  .stream()
                  .map(SOURCE_BOOKS::loadSourceBook)
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .collect(Collectors.toMap(SourceBook::getAbbrev, SourceBook::getTitle)));
        }

        Vector<String> sortedBookList = BOOKS.keySet().stream().sorted().collect(Collectors.toCollection(Vector::new));
        JComboBox<String> comboBox = new JComboBox<>(sortedBookList);
        comboBox.setRenderer(titleRenderer);

        JTextField manualField = new JTextField(15);
        JRadioButton rbCombo = new JRadioButton(Messages.getString("SourceChooser.list"), true);
        JRadioButton rbManual = new JRadioButton(Messages.getString("SourceChooser.manual"));

        ButtonGroup group = new ButtonGroup();
        group.add(rbCombo);
        group.add(rbManual);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        if (showManualTextfield) {
            mainPanel.add(rbCombo, gbc);
        }

        gbc.insets = new Insets(0, 20, 0, 0); // indent combo box
        mainPanel.add(comboBox, gbc);

        if (showManualTextfield) {
            gbc.insets = new Insets(20, 0, 0, 0);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            var orLabel = new JLabel("- %s -".formatted(Messages.getString("SourceChooser.or")), SwingConstants.CENTER);
            mainPanel.add(orLabel, gbc);
            gbc.fill = GridBagConstraints.NONE;
            mainPanel.add(rbManual, gbc);
            gbc.insets = new Insets(0, 20, 0, 0); // indent text field
            mainPanel.add(manualField, gbc);
        }

        comboBox.setEnabled(true);
        manualField.setEnabled(false);

        rbCombo.addActionListener(e -> {
            comboBox.setEnabled(true);
            manualField.setEnabled(false);
        });
        rbManual.addActionListener(e -> {
            comboBox.setEnabled(false);
            manualField.setEnabled(true);
            manualField.requestFocusInWindow();
        });

        JOptionPane optionPane = new JOptionPane(mainPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = optionPane.createDialog(parent, Messages.getString("SourceChooser.title"));

        // Close dialog immediately when selecting from comboBox (if "Choose from list" is selected)
        comboBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if (rbCombo.isSelected()) {
                    optionPane.setValue(JOptionPane.OK_OPTION);
                    dialog.setVisible(false);
                }
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        dialog.setVisible(true);

        Object value = optionPane.getValue();
        if (value != null && (int) value == JOptionPane.OK_OPTION) {
            if (rbCombo.isSelected()) {
                return (String) comboBox.getSelectedItem();
            } else {
                return manualField.getText().trim();
            }
        }
        return null;
    }

    private static final DefaultListCellRenderer titleRenderer = new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
        boolean cellHasFocus) {
            if (value instanceof String string && BOOKS.containsKey(string)) {
                // replace the source short name with the title if available
                value = BOOKS.get(string);
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    };
}
