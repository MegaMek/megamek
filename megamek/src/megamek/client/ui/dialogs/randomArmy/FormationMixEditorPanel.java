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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
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

        FamilyGrouping grouping = groupByFamily(offered);
        Map<String, Set<String>> families = grouping.families();
        Set<String> aerospaceFamilies = grouping.aerospaceFamilies();

        // The aerospace block takes a column to itself, so the ground families have one fewer to spread across.
        int groundColumns = aerospaceFamilies.isEmpty() ? CATEGORY_COLUMNS : (CATEGORY_COLUMNS - 1);
        int groundFamilies = families.size() - aerospaceFamilies.size();
        int groundFormations = offered.size() - aerospaceFamilies.stream()
              .mapToInt(family -> families.get(family).size())
              .sum();
        // Count the family headings as rows too, or the columns come out lopsided - a couple of dozen formations in
        // ten families is nearly forty rows, not twenty-seven.
        int rowsPerColumn = rowsPerColumn(groundFormations + groundFamilies, groundColumns);

        // Each column is a panel of its own rather than a slice of one big grid, so the three read as separate
        // lists. In a single grid the eye tracks across the gap and pairs a formation in one column with the
        // spinner of the next.
        List<JPanel> columns = new ArrayList<>();
        JPanel currentColumn = newColumnPanel();
        int row = 1;
        boolean inAerospace = false;
        for (Map.Entry<String, Set<String>> family : families.entrySet()) {
            boolean familyIsAerospace = aerospaceFamilies.contains(family.getKey());
            // The aerospace formations start a column of their own under their own heading. They answer a different
            // question from the ground ones, and a combined-arms force offers both, so running them on from the end
            // of the ground list gives no clue where one ends and the other begins.
            boolean startsAerospaceBlock = familyIsAerospace && !inAerospace;
            boolean columnIsFull = (row > 1) && (row >= rowsPerColumn) && (columns.size() < groundColumns - 1);
            if (startsAerospaceBlock || columnIsFull) {
                columns.add(closeColumn(currentColumn, row));
                currentColumn = newColumnPanel();
                row = 1;
            }
            if (startsAerospaceBlock) {
                row = addDomainHeading(currentColumn, "ForceGeneratorDialog.formationMix.domain.aerospace", row);
                inAerospace = true;
            }
            row = addFamily(currentColumn, family.getKey(), family.getValue(), row);
        }
        columns.add(closeColumn(currentColumn, row));

        setLayout(new GridLayout(1, columns.size(), 8, 0));
        columns.forEach(this::add);
    }

    /** A column of the grid, bordered so it reads as its own list, with the shared header row already in place. */
    private JPanel newColumnPanel() {
        JPanel column = new JPanel(new GridBagLayout());
        column.setBorder(BorderFactory.createEtchedBorder());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(2, 4, 4, 4);
        constraints.gridy = 0;

        constraints.gridx = 0;
        constraints.weightx = 1.0;
        column.add(headerLabel("ForceGeneratorDialog.formationMix.header.formation"), constraints);
        constraints.weightx = 0.0;
        constraints.gridx = 1;
        column.add(headerLabel("ForceGeneratorDialog.formationMix.header.requested"), constraints);
        constraints.gridx = 2;
        column.add(headerLabel("ForceGeneratorDialog.formationMix.header.current"), constraints);
        return column;
    }

    /**
     * Pushes a column's rows to the top of the panel.
     *
     * <p>The columns rarely come out the same length, and without this the shorter ones float in the middle of their
     * border with their heading level with the middle of the neighbouring list.</p>
     *
     * @param column  the finished column
     * @param nextRow the first free grid row
     *
     * @return the same column, for chaining
     */
    private static JPanel closeColumn(JPanel column, int nextRow) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = nextRow;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.VERTICAL;
        column.add(Box.createVerticalGlue(), constraints);
        return column;
    }

    private static JLabel headerLabel(String messageKey) {
        JLabel header = new JLabel(Messages.getString(messageKey));
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        return header;
    }

    /**
     * Adds one family and its formations to a column.
     *
     * @param column     the column panel to add to
     * @param familyName the family heading
     * @param members    the formations in the family
     * @param startRow   the grid row to start at
     *
     * @return the next free grid row
     */
    private int addFamily(JPanel column, String familyName, Set<String> members, int startRow) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        int row = startRow;

        // A family whose only member shares its name - Urban, Ranger, and every aerospace squadron - would
        // otherwise print that name twice, once as a heading and once as the row beneath it. Show one row.
        boolean hasOwnHeading = !((members.size() == 1) && members.contains(familyName));
        if (hasOwnHeading) {
            constraints.gridx = 0;
            constraints.gridy = row++;
            constraints.gridwidth = 3;
            constraints.insets = new Insets(3, 4, 1, 4);
            JLabel familyLabel = new JLabel(familyName);
            familyLabel.setFont(familyLabel.getFont().deriveFont(Font.BOLD));
            // The family is itself a Campaign Operations formation in most cases, so its own description belongs on
            // the heading - a reader hovering "Battle" should learn what a Battle Lance is.
            familyLabel.setToolTipText(describeFormation(FormationType.getFormationType(familyName), familyName, -1));
            column.add(familyLabel, constraints);
            constraints.gridwidth = 1;
        }

        for (String formationName : members) {
            FormationType formationType = FormationType.getFormationType(formationName);
            String displayName = (formationType == null) ? formationName : formationType.getNameWithFaction();

            JLabel label = new JLabel(displayName);
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 5));
            spinner.setName("spnFormationMix" + formationName.replace(" ", ""));
            String tooltip = describeFormation(formationType, displayName, preview.defaultShareFor(formationName));
            spinner.setToolTipText(tooltip);
            label.setToolTipText(tooltip);
            label.setLabelFor(spinner);
            spinners.put(formationName, spinner);

            // Members sit under their heading rather than level with it, so the families read as blocks. A family
            // shown as a single row has no heading to sit under, so it keeps the outer margin.
            constraints.gridx = 0;
            constraints.gridy = row++;
            constraints.weightx = 1.0;
            constraints.insets = new Insets(1, hasOwnHeading ? (4 + MEMBER_INDENT) : 4, 1, 4);
            column.add(label, constraints);

            // The spinner and the share it is adjusting sit together, with only a hair between them, so the pair
            // reads as one control rather than two columns to scan between.
            constraints.weightx = 0.0;
            constraints.gridx = 1;
            constraints.insets = new Insets(1, 4, 1, 1);
            column.add(spinner, constraints);
            constraints.gridx = 2;
            constraints.insets = new Insets(1, 1, 1, 4);
            column.add(new JLabel(String.format("%d%%", Math.round(preview.defaultShareFor(formationName)))),
                  constraints);
        }
        return row;
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
     * @return the families in display order, and which of them are aerospace
     */
    private static FamilyGrouping groupByFamily(Set<String> offered) {
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
        return new FamilyGrouping(ordered, aerospace.keySet());
    }

    /**
     * The offered formations grouped into families, with the ground/aerospace split recorded.
     *
     * @param families         family name to the formations in it, ground families first
     * @param aerospaceFamilies the families in {@code families} that are aerospace rather than ground
     */
    private record FamilyGrouping(Map<String, Set<String>> families, Set<String> aerospaceFamilies) {}

    /**
     * Rows to fill before starting a new column, so the families spread evenly rather than running down one side.
     *
     * @param totalRows the rows to distribute
     * @param columns   the columns to distribute them across
     *
     * @return the rows each column should hold
     */
    private static int rowsPerColumn(int totalRows, int columns) {
        return Math.max(1, (totalRows + columns - 1) / Math.max(1, columns));
    }

    /**
     * Adds a heading naming the domain the formations beneath it belong to.
     *
     * @param column     the column panel to add to
     * @param messageKey the resource key of the heading text
     * @param startRow   the grid row to start at
     *
     * @return the next free grid row
     */
    private static int addDomainHeading(JPanel column, String messageKey, int startRow) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = startRow;
        constraints.gridwidth = 3;
        constraints.insets = new Insets(4, 4, 2, 4);
        column.add(headerLabel(messageKey), constraints);
        return startRow + 1;
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
     * Builds the hover text for a formation: its name, what Campaign Operations says it is, and what share the
     * ruleset gives it here.
     *
     * <p>Laid out over several lines and wrapped to a fixed width. The descriptions run to a full sentence with a
     * rules citation, and as one unbroken line they render as a tooltip wider than the dialog.</p>
     *
     * @param formationType the formation, or {@code null} for a name with no registered type
     * @param displayName   the name to head the tooltip with
     * @param defaultShare  the ruleset's share of this force for the formation, or negative to omit it
     *
     * @return the hover text, or {@code null} when there is nothing to say
     */
    private static @Nullable String describeFormation(@Nullable FormationType formationType, String displayName,
          double defaultShare) {
        String description = tooltipFor(formationType);
        if ((description == null) && (defaultShare < 0)) {
            return null;
        }
        StringBuilder text = new StringBuilder("<html><body style='width:")
              .append(TOOLTIP_WIDTH_PIXELS)
              .append("px'><b>")
              .append(displayName)
              .append("</b>");
        if (description != null) {
            text.append("<br>").append(description);
        }
        if (defaultShare >= 0) {
            text.append("<br><i>")
                  .append(Messages.getString("ForceGeneratorDialog.formationMix.defaultShare",
                        Math.round(defaultShare)))
                  .append("</i>");
        }
        return text.append("</body></html>").toString();
    }

    /** Width the hover text wraps at. The Campaign Operations descriptions are a full sentence plus a citation. */
    private static final int TOOLTIP_WIDTH_PIXELS = 280;

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
