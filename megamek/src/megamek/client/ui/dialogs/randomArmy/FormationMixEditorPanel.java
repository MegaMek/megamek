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
package megamek.client.ui.dialogs.randomArmy;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.client.ratgenerator.FormationMix;
import megamek.client.ratgenerator.FormationMixPreview;
import megamek.client.ratgenerator.FormationType;
import megamek.client.ui.Messages;
import megamek.common.annotations.Nullable;

/**
 * Lets the player ask for more of one kind of lance and less of another.
 *
 * <p>Shows only the formations the force currently described by the options actually offers, grouped the way the
 * Formation Builder groups them - Light, Medium and Heavy Battle and their variants under Battle - so a couple of
 * dozen formations read as a handful of families rather than a flat list.</p>
 *
 * <p>Each row carries the share the ruleset would give that formation on its own, so the player is adjusting real
 * odds rather than guessing against an abstraction. Leaving every spinner at zero requests nothing and generates
 * exactly as the ruleset would.</p>
 */
public class FormationMixEditorPanel extends JPanel {

    /** How many formation families sit side by side before wrapping to a new column. */
    private static final int CATEGORY_COLUMNS = 3;

    /** Left inset on a formation row, so members sit under their family heading rather than beside it. */
    private static final int MEMBER_INDENT = 16;

    private final Map<String, JSpinner> spinners = new TreeMap<>();
    private final FormationMixPreview preview;

    /**
     * Builds an editor for the formations a force offers.
     *
     * @param preview what the force offers, from a structure-only build
     */
    public FormationMixEditorPanel(FormationMixPreview preview) {
        this.preview = (preview == null) ? FormationMixPreview.EMPTY : preview;
        setLayout(new GridBagLayout());
        build();
    }

    private void build() {
        Set<String> offered = preview.offeredFormations();
        if (offered.isEmpty()) {
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(6, 6, 6, 6);
            add(new JLabel(Messages.getString("ForceGeneratorDialog.formationMix.nothingToTweak")), constraints);
            return;
        }

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(1, 4, 1, 4);
        Map<String, Set<String>> families = groupByFamily(offered);
        // Count the family headings as rows too, or the columns come out lopsided - a couple of dozen formations in
        // ten families is nearly forty rows, not twenty-seven.
        int rowsPerColumn = rowsPerColumn(offered.size() + families.size());
        int column = 0;
        int row = 0;
        for (Map.Entry<String, Set<String>> family : families.entrySet()) {
            // A family whose only member shares its name - Urban, Ranger, and every aerospace squadron - would
            // otherwise print that name twice, once as a heading and once as the row beneath it. Show one row.
            boolean hasOwnHeading = !(family.getValue().size() == 1
                  && family.getValue().contains(family.getKey()));
            if (hasOwnHeading) {
                constraints.gridx = column * 3;
                constraints.gridy = row++;
                constraints.gridwidth = 3;
                constraints.insets = new Insets(1, 4, 1, 4);
                JLabel familyLabel = new JLabel(family.getKey());
                familyLabel.setFont(familyLabel.getFont().deriveFont(java.awt.Font.BOLD));
                add(familyLabel, constraints);
                constraints.gridwidth = 1;
            }
            // Members sit under their heading rather than level with it, so the families read as blocks. A family
            // shown as a single row has no heading to sit under, so it keeps the outer margin.
            constraints.insets = new Insets(1, hasOwnHeading ? (4 + MEMBER_INDENT) : 4, 1, 4);

            for (String formationName : family.getValue()) {
                FormationType formationType = FormationType.getFormationType(formationName);
                String displayName = (formationType == null) ? formationName : formationType.getNameWithFaction();

                JLabel label = new JLabel(displayName);
                JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 5));
                spinner.setName("spnFormationMix" + formationName.replace(" ", ""));
                String tooltip = tooltipFor(formationType);
                String defaultShare = Messages.getString("ForceGeneratorDialog.formationMix.defaultShare",
                      Math.round(preview.defaultShareFor(formationName)));
                spinner.setToolTipText((tooltip == null) ? defaultShare : defaultShare + " " + tooltip);
                label.setToolTipText(spinner.getToolTipText());
                label.setLabelFor(spinner);
                spinners.put(formationName, spinner);

                constraints.gridx = column * 3;
                constraints.gridy = row;
                constraints.weightx = 0.0;
                add(label, constraints);
                // Only the label carries the indent; the spinner and its default share line up across families.
                constraints.insets = new Insets(1, 4, 1, 4);
                constraints.gridx = (column * 3) + 1;
                constraints.weightx = 1.0;
                add(spinner, constraints);
                constraints.gridx = (column * 3) + 2;
                constraints.weightx = 0.0;
                add(new JLabel(String.format("(%d%%)", Math.round(preview.defaultShareFor(formationName)))),
                      constraints);
                constraints.insets = new Insets(1, hasOwnHeading ? (4 + MEMBER_INDENT) : 4, 1, 4);
                row++;
            }

            if (row >= rowsPerColumn) {
                column++;
                row = 0;
            }
        }
    }

    /**
     * Groups the offered formations under their family, so variants sit beneath the formation they vary.
     *
     * <p>Ground families come first and aerospace families after, each alphabetically within its own block. A
     * combined-arms force offers both, and interleaving them alphabetically put Aerospace Superiority Squadron above
     * Assault and Interceptor Squadron between Fire and Pursuit, which reads as noise rather than structure.</p>
     *
     * @param offered the formations this force offers
     *
     * @return family name to the formations in it, ground families first
     */
    private static Map<String, Set<String>> groupByFamily(Set<String> offered) {
        Map<String, Set<String>> ground = new TreeMap<>();
        Map<String, Set<String>> aerospace = new TreeMap<>();
        for (String formationName : offered) {
            FormationType formationType = FormationType.getFormationType(formationName);
            String family = (formationType == null) ? formationName : formationType.getCategory();
            // A family is ground or aerospace as a whole; ask the family itself where it can be resolved, and fall
            // back to the member when the family name is not a registered formation in its own right.
            FormationType familyType = FormationType.getFormationType(family);
            boolean isGround = (familyType != null)
                  ? familyType.isGround()
                  : ((formationType == null) || formationType.isGround());
            Map<String, Set<String>> target = isGround ? ground : aerospace;
            target.computeIfAbsent(family, ignored -> new TreeSet<>()).add(formationName);
        }
        Map<String, Set<String>> ordered = new LinkedHashMap<>(ground);
        ordered.putAll(aerospace);
        return ordered;
    }

    /** Rows to fill before starting a new column, so the families spread evenly rather than running down one side. */
    private static int rowsPerColumn(int totalRows) {
        return Math.max(1, (totalRows + CATEGORY_COLUMNS - 1) / CATEGORY_COLUMNS);
    }

    /**
     * The formation's own requirements, as the Formation Builder shows them.
     *
     * @param formationType the formation to describe, may be {@code null} for an unregistered name
     *
     * @return the description, or {@code null} when the formation has none
     */
    public static @Nullable String tooltipFor(@Nullable FormationType formationType) {
        if (formationType == null) {
            return null;
        }
        String key = formationType.getTooltipKey();
        return Messages.keyExists(key) ? Messages.getString(key) : null;
    }

    /**
     * @return the requested mix, empty when every spinner is left at zero
     */
    public FormationMix getMix() {
        Map<String, Integer> percentages = new LinkedHashMap<>();
        spinners.forEach((formationName, spinner) -> percentages.put(formationName, (Integer) spinner.getValue()));
        return new FormationMix(percentages);
    }

    /**
     * Restores a previously entered mix. Formations this force does not offer are ignored.
     *
     * @param mix the mix to show, or {@code null} to clear
     */
    public void setMix(@Nullable FormationMix mix) {
        FormationMix toShow = (mix == null) ? FormationMix.EMPTY : mix;
        spinners.forEach((formationName, spinner) -> spinner.setValue(toShow.percentFor(formationName)));
    }

    /** Clears every request. */
    public void reset() {
        spinners.values().forEach(spinner -> spinner.setValue(0));
    }

    /**
     * Registers a listener notified whenever the requested mix changes.
     *
     * <p>For a host showing the editor inline, where there is no OK button to read the mix on.</p>
     *
     * @param listener run after any change
     */
    public void addMixChangeListener(Runnable listener) {
        spinners.values().forEach(spinner -> spinner.addChangeListener(event -> listener.run()));
    }

    /**
     * @return {@code true} when this force offers nothing that could be adjusted
     */
    public boolean isEmpty() {
        return spinners.isEmpty();
    }
}
