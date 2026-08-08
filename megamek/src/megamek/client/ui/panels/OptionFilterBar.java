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

package megamek.client.ui.panels;

import java.awt.FlowLayout;
import java.io.Serial;
import java.text.MessageFormat;
import java.util.function.Consumer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.UIUtil.FixedYPanel;

/**
 * The search row above an option list: a filter field, a "show unimplemented" check box, and a match counter.
 *
 * <p>Shared by the special-pilot-ability and quirk lists, which word their prompts differently but behave the same.
 * The bar owns its widgets and reports changes through the callbacks given to the constructor; it never decides what
 * is visible, so each list keeps its own filtering rules.</p>
 *
 * <p>Build one per panel and keep it - constructing it registers listeners, so rebuilding it on every refresh would
 * stack duplicates and fire the filter callback once per copy.</p>
 */
public class OptionFilterBar extends FixedYPanel {
    @Serial
    private static final long serialVersionUID = 3821960503048871079L;

    private static final int FILTER_FIELD_COLUMNS = 20;

    private final JTextField filterField = new JTextField();
    private final JCheckBox showUnimplementedCheck;
    private final JLabel matchCountLabel = new JLabel();
    private final String matchCountPattern;

    /**
     * @param labels                 the localized text and component names for this bar
     * @param showUnimplemented      initial state of the check box, normally restored from preferences
     * @param onFilterTextChanged    run whenever the search text changes
     * @param onShowUnimplemented    accepts the check box's new state whenever the player toggles it; use it to
     *                               persist the preference and re-apply the filter
     */
    public OptionFilterBar(OptionFilterBarLabels labels, boolean showUnimplemented, Runnable onFilterTextChanged,
          Consumer<Boolean> onShowUnimplemented) {
        super(new FlowLayout(FlowLayout.LEFT, UIUtil.scaleForGUI(5), 2));
        matchCountPattern = labels.matchCountPattern();

        filterField.setName(labels.filterFieldName());
        filterField.putClientProperty("JTextField.placeholderText", labels.filterPlaceholder());
        filterField.setToolTipText(labels.filterTooltip());
        filterField.setColumns(FILTER_FIELD_COLUMNS);
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                onFilterTextChanged.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                onFilterTextChanged.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                onFilterTextChanged.run();
            }
        });

        showUnimplementedCheck = new JCheckBox(labels.toggleText(), showUnimplemented);
        showUnimplementedCheck.setName(labels.toggleName());
        showUnimplementedCheck.setToolTipText(labels.toggleTooltip());
        showUnimplementedCheck.addActionListener(
              event -> onShowUnimplemented.accept(showUnimplementedCheck.isSelected()));

        matchCountLabel.setName(labels.matchCountName());
        matchCountLabel.setVisible(false);

        add(filterField);
        add(showUnimplementedCheck);
        add(matchCountLabel);
    }

    /** @return the current search text, trimmed; empty when the player has not typed anything */
    public String getFilterText() {
        return filterField.getText().trim();
    }

    /** @return {@code true} if the list should also show the options that are not implemented */
    public boolean isShowUnimplementedSelected() {
        return showUnimplementedCheck.isSelected();
    }

    /**
     * Updates the match counter. It is shown only while a search is active, since "93 of 93 match" tells the player
     * nothing when the field is empty.
     *
     * @param shownCount how many options currently match
     * @param totalCount how many options there are in total
     */
    public void setMatchCount(int shownCount, int totalCount) {
        matchCountLabel.setText(MessageFormat.format(matchCountPattern, shownCount, totalCount));
        matchCountLabel.setVisible(!getFilterText().isEmpty());
    }
}
