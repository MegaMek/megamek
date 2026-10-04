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
package megamek.client.ui.dialogs.advancedsearch;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.border.Border;

import com.formdev.flatlaf.FlatClientProperties;
import megamek.client.ui.Messages;
import megamek.client.ui.WrapLayout;
import megamek.common.SourceBookCode;

/** A compact multi-select dropdown that renders the selected sourcebooks as removable pills. */
class RulesRefPicker extends JPanel {

    private static final Comparator<SourceBookCode> BOOK_ORDER = Comparator.comparing(SourceBookCode::getAbbrev,
          String.CASE_INSENSITIVE_ORDER);

    private final List<SourceBookCode> choices = new ArrayList<>();
    private final Set<SourceBookCode> selectedBooks = new LinkedHashSet<>();
    private final JButton openButton = new JButton(Messages.getString("MekSelectorDialog.Search.RulesRefs.select"));

    RulesRefPicker() {
        super(new WrapLayout(FlowLayout.LEFT, 4, 3));
        setOpaque(true);
        if (UIManager.getColor("TextField.background") != null) {
            setBackground(UIManager.getColor("TextField.background"));
        }
        Border padding = BorderFactory.createEmptyBorder(2, 4, 2, 4);
        Border textFieldBorder = UIManager.getBorder("TextField.border");
        setBorder((textFieldBorder == null) ? padding : BorderFactory.createCompoundBorder(textFieldBorder, padding));

        openButton.addActionListener(event -> showChoices());
        refreshPills();
    }

    void setChoices(Collection<SourceBookCode> sourceBooks) {
        choices.clear();
        choices.addAll(sourceBooks.stream().distinct().sorted(BOOK_ORDER).toList());
        selectedBooks.retainAll(choices);
        refreshPills();
    }

    Set<SourceBookCode> getSelectedBooks() {
        return Set.copyOf(selectedBooks);
    }

    List<String> getSelectedAbbreviations() {
        return selectedBooks.stream().map(SourceBookCode::getAbbrev).toList();
    }

    void setSelectedAbbreviations(Collection<String> abbreviations) {
        selectedBooks.clear();
        for (SourceBookCode sourceBook : SourceBookCode.values()) {
            if (abbreviations.stream().anyMatch(value -> sourceBook.getAbbrev().equalsIgnoreCase(value)
                  || sourceBook.name().equalsIgnoreCase(value))) {
                selectedBooks.add(sourceBook);
            }
        }
        refreshPills();
    }

    void clearSelection() {
        selectedBooks.clear();
        refreshPills();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();
        if (getWidth() > 0) {
            preferredSize.width = Math.min(preferredSize.width, getWidth());
        }
        return preferredSize;
    }

    private void showChoices() {
        JPopupMenu popup = new JPopupMenu();
        if (choices.isEmpty()) {
            JMenuItem emptyItem = new JMenuItem(Messages.getString("MekSelectorDialog.Search.RulesRefs.none"));
            emptyItem.setEnabled(false);
            popup.add(emptyItem);
        } else {
            for (SourceBookCode sourceBook : choices) {
                JCheckBox choice = new JCheckBox(sourceBook.getAbbrev(), selectedBooks.contains(sourceBook));
                choice.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 12));
                choice.addActionListener(event -> {
                    if (choice.isSelected()) {
                        selectedBooks.add(sourceBook);
                    } else {
                        selectedBooks.remove(sourceBook);
                    }
                    refreshPills();
                });
                popup.add(choice);
            }
            popup.addSeparator();
            JMenuItem clearItem = new JMenuItem(Messages.getString("MekSelectorDialog.Search.RulesRefs.clear"));
            clearItem.setEnabled(!selectedBooks.isEmpty());
            clearItem.addActionListener(event -> clearSelection());
            popup.add(clearItem);
        }
        popup.show(openButton, 0, openButton.getHeight());
    }

    private void refreshPills() {
        removeAll();
        if (selectedBooks.isEmpty()) {
            add(new JLabel(Messages.getString("MekSelectorDialog.Search.Any")));
        } else {
            for (SourceBookCode sourceBook : selectedBooks) {
                JButton pill = new JButton(sourceBook.getAbbrev() + " \u00d7");
                pill.setFocusable(false);
                pill.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");
                pill.putClientProperty(FlatClientProperties.STYLE, "arc: 999; margin: 1, 6, 1, 6");
                pill.addActionListener(event -> {
                    selectedBooks.remove(sourceBook);
                    refreshPills();
                });
                add(pill);
            }
        }
        add(openButton);
        revalidate();
        repaint();
    }
}
