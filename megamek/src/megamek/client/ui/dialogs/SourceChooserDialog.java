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

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class SourceChooserDialog {

    private static final SourceBooks SOURCE_BOOKS = new SourceBooks();
    private static final Map<String, String> BOOKS = new HashMap<>();

    private static void loadBooks() {
        BOOKS.clear();
        BOOKS.putAll(SOURCE_BOOKS.availableSourcebooks()
              .stream()
              .map(SOURCE_BOOKS::loadSourceBook)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toMap(SourceBook::getAbbrev, SourceBook::getTitle)));
    }

    /**
     * Shows a dialog where the user can select multiple sourcebooks. The returned value is a comma-separated source
     * list suitable for writing to unit files.
     *
     * @param parent              a parent frame for this dialog
     * @param showManualTextfield When true, shows the option to enter the source list manually
     * @param selectedSources     a comma-separated source list to preselect
     *
     * @return the chosen source list, or null if canceled
     */
    public static String showMultiChoiceDialog(@Nullable Component parent, boolean showManualTextfield,
          String selectedSources) {
        loadBooks();

        List<String> sortedBookList = BOOKS.keySet().stream().sorted().toList();
        List<String> selectedSourceList = SourceBooks.splitSourceList(selectedSources);
        List<String> selectedBookKeys = selectedSourceList.stream()
              .map(SourceChooserDialog::sourceListEntryToBookKey)
              .flatMap(Optional::stream)
              .toList();
        boolean hasUnknownSource = selectedSourceList.stream()
              .anyMatch(source -> sourceListEntryToBookKey(source).isEmpty());

        JTextField filterField = createBookFilterField();

        JPanel bookPanel = new JPanel();
        bookPanel.setLayout(new BoxLayout(bookPanel, BoxLayout.Y_AXIS));
        Map<String, JCheckBox> bookChecks = new HashMap<>();
        JCheckBox firstSelectedCheckBox = null;
        for (String sourceName : sortedBookList) {
            JCheckBox checkBox = new JCheckBox(BOOKS.getOrDefault(sourceName, sourceName));
            checkBox.setSelected(selectedBookKeys.contains(sourceName));
            checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            checkBox.addActionListener(e -> updateBookCheckFilter(bookChecks, filterField.getText()));
            bookChecks.put(sourceName, checkBox);
            bookPanel.add(checkBox);
            if (firstSelectedCheckBox == null && checkBox.isSelected()) {
                firstSelectedCheckBox = checkBox;
            }
        }
        filterField.getDocument().addDocumentListener(createFilterDocumentListener(
              () -> updateBookCheckFilter(bookChecks, filterField.getText())));
        updateBookCheckFilter(bookChecks, filterField.getText());

        JScrollPane bookScrollPane = new JScrollPane(bookPanel);
        bookScrollPane.setPreferredSize(new Dimension(360, 240));

        JTextField manualField = new JTextField(SourceBooks.normalizeSourceList(selectedSources), 24);
        boolean useManual = showManualTextfield && hasUnknownSource;
        JRadioButton rbList = new JRadioButton(Messages.getString("SourceChooser.list"), !useManual);
        JRadioButton rbManual = new JRadioButton(Messages.getString("SourceChooser.manual"), useManual);

        ButtonGroup group = new ButtonGroup();
        group.add(rbList);
        group.add(rbManual);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        if (showManualTextfield) {
            mainPanel.add(rbList, gbc);
            gbc.gridy++;
        }

        gbc.insets = new Insets(0, showManualTextfield ? 20 : 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(createBookFilterPanel(filterField), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, showManualTextfield ? 20 : 0, 0, 0);
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(bookScrollPane, gbc);

        if (showManualTextfield) {
            gbc.gridy++;
            gbc.insets = new Insets(20, 0, 0, 0);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            var orLabel = new JLabel("- %s -".formatted(Messages.getString("SourceChooser.or")), SwingConstants.CENTER);
            mainPanel.add(orLabel, gbc);
            gbc.gridy++;
            gbc.fill = GridBagConstraints.NONE;
            mainPanel.add(rbManual, gbc);
            gbc.gridy++;
            gbc.insets = new Insets(0, 20, 0, 0);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            mainPanel.add(manualField, gbc);
        }

        setBookListEnabled(filterField, bookChecks, !useManual);
        manualField.setEnabled(useManual);

        rbList.addActionListener(e -> {
            setBookListEnabled(filterField, bookChecks, true);
            manualField.setEnabled(false);
            filterField.requestFocusInWindow();
        });
        rbManual.addActionListener(e -> {
            setBookListEnabled(filterField, bookChecks, false);
            manualField.setEnabled(true);
            manualField.requestFocusInWindow();
        });

        JOptionPane optionPane = new JOptionPane(mainPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = optionPane.createDialog(parent, Messages.getString("SourceChooser.title"));
        if (firstSelectedCheckBox != null) {
            JCheckBox selectedCheckBox = firstSelectedCheckBox;
            SwingUtilities.invokeLater(() -> bookScrollPane.getViewport().setViewPosition(selectedCheckBox.getLocation()));
        }
        dialog.setVisible(true);

        Object value = optionPane.getValue();
        if (value instanceof Integer selectedValue && selectedValue == JOptionPane.OK_OPTION) {
            if (!showManualTextfield || rbList.isSelected()) {
                return SourceBooks.formatSourceList(sortedBookList.stream()
                      .filter(sourceName -> bookChecks.get(sourceName).isSelected())
                      .toList());
            } else {
                return SourceBooks.normalizeSourceList(manualField.getText());
            }
        }
        return null;
    }

    private static JTextField createBookFilterField() {
        JTextField filterField = new JTextField(24);
        filterField.setName("sourceBookFilter");
        return filterField;
    }

    private static JPanel createBookFilterPanel(JTextField filterField) {
        JLabel filterLabel = new JLabel(Messages.getString("SourceChooser.filter") + ":");
        filterLabel.setLabelFor(filterField);

        JPanel filterPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        filterPanel.add(filterLabel, gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 5, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        filterPanel.add(filterField, gbc);
        return filterPanel;
    }

    private static DocumentListener createFilterDocumentListener(Runnable filterBooks) {
        return new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                filterBooks.run();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                filterBooks.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterBooks.run();
            }
        };
    }

    private static void updateBookCheckFilter(Map<String, JCheckBox> bookChecks, String filterText) {
        bookChecks.forEach((sourceName, checkBox) -> checkBox.setVisible(checkBox.isSelected()
              || bookMatchesFilter(sourceName, filterText)));
        bookChecks.values().stream()
              .findFirst()
              .map(Component::getParent)
              .ifPresent(parent -> {
                  parent.revalidate();
                  parent.repaint();
              });
    }

    private static boolean bookMatchesFilter(String sourceName, String filterText) {
        String normalizedFilter = normalizeBookSearchText(filterText);
        if (normalizedFilter.isBlank()) {
            return true;
        }

        String searchableText = normalizeBookSearchText(sourceName + " " + BOOKS.getOrDefault(sourceName, ""));
        for (String filterPart : normalizedFilter.split("\\s+")) {
            if (!searchableText.contains(filterPart)) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeBookSearchText(String text) {
        return Optional.ofNullable(text)
              .orElse("")
              .toLowerCase(Locale.ROOT)
              .trim();
    }

    private static Optional<String> sourceListEntryToBookKey(String sourceName) {
        if (BOOKS.containsKey(sourceName)) {
            return Optional.of(sourceName);
        }

        String compactSourceName = sourceName.replaceAll(":\\s+", ":");
        if (BOOKS.containsKey(compactSourceName)) {
            return Optional.of(compactSourceName);
        }

        return SOURCE_BOOKS.loadSourceBook(sourceName)
              .map(SourceBook::getAbbrev)
              .filter(BOOKS::containsKey);
    }

    private static void setBookChecksEnabled(Map<String, JCheckBox> bookChecks, boolean enabled) {
        bookChecks.values().forEach(checkBox -> checkBox.setEnabled(enabled));
    }

    private static void setBookListEnabled(JTextField filterField, Map<String, JCheckBox> bookChecks,
          boolean enabled) {
        filterField.setEnabled(enabled);
        setBookChecksEnabled(bookChecks, enabled);
    }
}
