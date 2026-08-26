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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.UIManager;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import megamek.client.ratgenerator.FormationMix;
import megamek.client.ratgenerator.FormationMixPreview;
import megamek.client.ratgenerator.FormationType;
import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
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
public class FormationMixEditorPanel extends JPanel implements Scrollable {

    /** How many formation families sit side by side before wrapping to a new column. */
    private static final int CATEGORY_COLUMNS = 3;

    /** Left inset on a formation row, so members sit under their family heading rather than beside it. */
    private static final int MEMBER_INDENT = 16;

    /** How narrow a formation name may be squeezed before the column stops giving ground. */
    private static final int NAME_MINIMUM_WIDTH = 40;

    /** Horizontal gap between two neighbouring columns. */
    private static final int COLUMN_GAP = 8;

    /** Pixels scrolled per mouse-wheel notch, about one formation row. */
    private static final int SCROLL_UNIT_INCREMENT = 16;

    private final Map<String, JRadioButton> choices = new TreeMap<>();
    private final ButtonGroup selection = new ButtonGroup();
    private final List<Runnable> selectionListeners = new ArrayList<>();
    private String selectedFormation;

    private final FormationMixPreview preview;

    /**
     * Builds a palette of the formations a force offers.
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
            constraints.insets = new Insets(UIUtil.scaleForGUI(6), UIUtil.scaleForGUI(6),
                  UIUtil.scaleForGUI(6), UIUtil.scaleForGUI(6));
            add(new JLabel(Messages.getString("ForceGeneratorDialog.formationMix.nothingToTweak")), constraints);
            return;
        }

        FamilyGrouping grouping = groupByFamily(offered);
        Map<String, Set<String>> families = grouping.families();
        Set<String> aerospaceFamilies = grouping.aerospaceFamilies();

        // Ground and aerospace families share all the columns. Giving the aerospace block a column of its own
        // left that column mostly empty while the ground list overran the other two and hid its last families
        // behind a scrollbar. Now the ground families fill the columns and the aerospace block follows them,
        // kept whole under its own heading, so it ends up at the foot of the last column.
        // The headings count as rows too, or the columns come out lopsided - a couple of dozen formations in ten
        // families is nearly forty rows, not twenty-seven.
        int aerospaceBlockRows = aerospaceFamilies.isEmpty() ? 0 : 1;
        int groundRows = 0;
        for (Map.Entry<String, Set<String>> family : families.entrySet()) {
            int familyRows = familyRowCount(family.getKey(), family.getValue());
            if (aerospaceFamilies.contains(family.getKey())) {
                aerospaceBlockRows += familyRows;
            } else {
                groundRows += familyRows;
            }
        }
        int rowsPerColumn = rowsPerColumn(groundRows + aerospaceBlockRows, CATEGORY_COLUMNS);

        // Each column is a panel of its own rather than a slice of one big grid, so the three read as separate
        // lists. In a single grid the eye tracks across the gap and pairs a formation in one column with the
        // spinner of the next.
        List<JPanel> columns = new ArrayList<>();
        JPanel currentColumn = newColumnPanel();
        int row = 0;
        boolean inAerospace = false;
        for (Map.Entry<String, Set<String>> family : families.entrySet()) {
            boolean familyIsAerospace = aerospaceFamilies.contains(family.getKey());
            // The aerospace formations follow the ground ones under a heading of their own. They answer a different
            // question from the ground ones, and a combined-arms force offers both, so running them on from the end
            // of the ground list gives no clue where one ends and the other begins.
            boolean startsAerospaceBlock = familyIsAerospace && !inAerospace;
            // Look at what would be added before deciding, not at what the column already holds. Breaking only
            // once the target is passed lets a family that starts just under it overshoot by its whole height:
            // Assault, Battle and Command came to eleven against a target of twelve, so Fire's six rows went on
            // top and the column ran to seventeen while the last one held a single formation. The aerospace block
            // is measured as a whole and never split, so its heading cannot land in one column and its
            // squadrons in the next.
            int rowsToAdd = startsAerospaceBlock
                  ? aerospaceBlockRows
                  : familyRowCount(family.getKey(), family.getValue());
            boolean columnIsFull = (row > 0)
                  && !inAerospace
                  && ((row + rowsToAdd) > rowsPerColumn)
                  && (columns.size() < CATEGORY_COLUMNS - 1);
            if (columnIsFull) {
                columns.add(closeColumn(currentColumn, row));
                currentColumn = newColumnPanel();
                row = 0;
            }
            if (startsAerospaceBlock) {
                row = addDomainHeading(currentColumn, "ForceGeneratorDialog.formationMix.domain.aerospace", row);
                inAerospace = true;
            }
            row = addFamily(currentColumn, family.getKey(), family.getValue(), row);
        }
        columns.add(closeColumn(currentColumn, row));

        layOutColumns(columns);
    }

    /**
     * Places the finished columns side by side, each given room in proportion to what it holds.
     *
     * <p>Equal columns waste width on the short ground names ("Fire", "Hunter") while truncating the long aerospace
     * ones ("Aerospace Superiority Squadron"), so the share each column gets follows the width it actually wants.</p>
     *
     * @param columns the finished column panels, in display order
     */
    private void layOutColumns(List<JPanel> columns) {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(0, 0, 0, UIUtil.scaleForGUI(COLUMN_GAP));
        for (int index = 0; index < columns.size(); index++) {
            JPanel column = columns.get(index);
            constraints.gridx = index;
            constraints.weightx = Math.max(1, column.getPreferredSize().width);
            if (index == (columns.size() - 1)) {
                constraints.insets = new Insets(0, 0, 0, 0);
            }
            add(column, constraints);
        }
    }

    /** A column of the grid, bordered so it reads as its own list, with the shared header row already in place. */
    private JPanel newColumnPanel() {
        // No column headings: every row is one choice now, so there is nothing to head. The family names inside
        // the column carry the structure that the headings used to.
        JPanel column = new JPanel(new GridBagLayout());
        column.setBorder(BorderFactory.createEtchedBorder());
        return column;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return SCROLL_UNIT_INCREMENT;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return (orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width;
    }

    /**
     * Keeps the editor as wide as its viewport and no wider, so it never asks for a horizontal scrollbar.
     *
     * <p>The columns absorb the difference by squeezing the formation names, which is why those carry a minimum
     * width and a tooltip.</p>
     *
     * @return always {@code true}
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    /**
     * Lets the editor grow past its viewport vertically, so the list scrolls up and down as before.
     *
     * @return always {@code false}
     */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
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
     * How many rows a family takes: one per formation, plus its heading where it has one.
     *
     * @param familyName the family
     * @param members    the formations in it
     *
     * @return the rows it occupies
     */
    private static int familyRowCount(String familyName, Set<String> members) {
        boolean hasOwnHeading = !((members.size() == 1) && members.contains(familyName));
        return members.size() + (hasOwnHeading ? 1 : 0);
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
            constraints.insets = new Insets(UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(4),
                  UIUtil.scaleForGUI(1), UIUtil.scaleForGUI(4));
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

            // One formation is chosen at a time and then applied to whichever node the player right-clicks, so the
            // control is a choice between formations rather than a quantity per formation.
            JRadioButton choice = new JRadioButton(displayName);
            choice.setName("rdoFormation" + formationName.replace(" ", ""));
            choice.setOpaque(false);
            choice.setToolTipText(describeFormation(formationType, displayName,
                  preview.typicalLancesFor(formationName), ceilingExplanation(formationName)));
            choice.addActionListener(event -> selectFormation(formationName));
            selection.add(choice);
            choices.put(formationName, choice);

            // Members sit under their heading rather than level with it, so the families read as blocks. A family
            // shown as a single row has no heading to sit under, so it keeps the outer margin.
            constraints.gridx = 0;
            constraints.gridy = row++;
            constraints.weightx = 1.0;
            constraints.gridwidth = 3;
            constraints.insets = new Insets(UIUtil.scaleForGUI(1),
                  UIUtil.scaleForGUI(hasOwnHeading ? (4 + MEMBER_INDENT) : 4),
                  UIUtil.scaleForGUI(1), UIUtil.scaleForGUI(4));
            constraints.fill = GridBagConstraints.HORIZONTAL;
            choice.setMinimumSize(new Dimension(NAME_MINIMUM_WIDTH, choice.getPreferredSize().height));
            column.add(choice, constraints);
            constraints.fill = GridBagConstraints.NONE;
            constraints.gridwidth = 1;
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
        constraints.insets = new Insets(UIUtil.scaleForGUI(4), UIUtil.scaleForGUI(4),
                  UIUtil.scaleForGUI(2), UIUtil.scaleForGUI(4));
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
     * @param typicalLances the lances the ruleset gives this formation on its own, or negative to omit it
     *
     * @return the hover text, or {@code null} when there is nothing to say
     */
    private static @Nullable String describeFormation(@Nullable FormationType formationType, String displayName,
          int typicalLances) {
        return describeFormation(formationType, displayName, typicalLances, null);
    }

    /**
     * The hover text for one formation: what it is, what the ruleset would give it, and how much of it this force
     * can hold.
     *
     * @param formationType      the formation to describe, may be {@code null} for an unregistered name
     * @param displayName        the name to head the text with
     * @param typicalLances      the lances the ruleset gives it on its own, or negative to leave it out
     * @param ceilingExplanation why the request stops where it does, or {@code null} to leave it out
     *
     * @return the hover text, or {@code null} when there is nothing to say
     */
    private static @Nullable String describeFormation(@Nullable FormationType formationType, String displayName,
          int typicalLances, @Nullable String ceilingExplanation) {
        String description = tooltipFor(formationType);
        if ((description == null) && (typicalLances < 0) && (ceilingExplanation == null)) {
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
        if (typicalLances >= 0) {
            text.append("<br><i>")
                  .append(Messages.getString("ForceGeneratorDialog.formationMix.usuallyBuilt", typicalLances))
                  .append("</i>");
        }
        String requirements = requirementsOf(formationType);
        if (requirements != null) {
            text.append("<br><br>").append(requirements);
        }
        if (formationType != null) {
            text.append("<br>")
                  .append(Messages.getString("ForceGeneratorDialog.formationMix.idealRole",
                        formationType.getIdealRole()));
        }
        if (ceilingExplanation != null) {
            text.append("<br><br>").append(ceilingExplanation);
        }
        return text.append("</body></html>").toString();
    }

    /**
     * How often the rules would reach for this formation in a force like this one.
     *
     * <p>Context rather than a limit. Nothing stops a formation being applied to any node from the tree; this
     * says how ordinary or unusual that choice is for this faction, era and unit type.</p>
     *
     * @param formationName the formation to describe
     *
     * @return the note for the hover text
     */
    private String ceilingExplanation(String formationName) {
        int placeable = preview.maximumLancesFor(formationName);
        return (placeable <= 0)
              ? Messages.getString("ForceGeneratorDialog.formationMix.ceiling.none")
              : Messages.getString("ForceGeneratorDialog.formationMix.ceiling", placeable,
                    preview.tweakableNodes());
    }

    /**
     * What Campaign Operations requires of the units in this formation.
     *
     * <p>Read from the formation's own criteria, the same ones the Formation Builder lists and the same ones that
     * decide whether it can be built at all, so the hover text cannot drift from what actually gets enforced.</p>
     *
     * <p>The counts are quoted against a four-unit formation because most of the criteria are proportional and a
     * bare percentage reads as arithmetic homework. The size is named so a Clan star of five is not misread.</p>
     *
     * @param formationType the formation to describe, may be {@code null}
     *
     * @return the requirements, or {@code null} when the formation asks nothing in particular
     */
    private static @Nullable String requirementsOf(@Nullable FormationType formationType) {
        if (formationType == null) {
            return null;
        }
        List<String> requirements = new ArrayList<>();
        if (formationType.getMainDescription() != null) {
            requirements.add(Messages.getString("ForceGeneratorDialog.formationMix.requirement.all",
                  formationType.getMainDescription()));
        }
        formationType.getOtherCriteria().forEachRemaining(constraint ->
              requirements.add(Messages.getString("ForceGeneratorDialog.formationMix.requirement.some",
                    constraint.getMinimum(NOMINAL_FORMATION_SIZE), constraint.getDescription())));
        return requirements.isEmpty()
              ? Messages.getString("ForceGeneratorDialog.formationMix.requirement.none")
              : Messages.getString("ForceGeneratorDialog.formationMix.requirements",
                    NOMINAL_FORMATION_SIZE, String.join("; ", requirements));
    }

    /** The formation size the quoted requirement counts are worked out against, being a standard lance. */
    private static final int NOMINAL_FORMATION_SIZE = 4;

    /** Width the hover text wraps at. The Campaign Operations descriptions are a full sentence plus a citation. */
    private static final int TOOLTIP_WIDTH_PIXELS = 280;

    /**
     * The formation the player has picked to work with.
     *
     * <p>This is a brush rather than a request: nothing happens to the force until it is applied to a node in the
     * organisation tree.</p>
     *
     * @return the selected formation's name, or {@code null} when nothing is selected
     */
    public @Nullable String getSelectedFormation() {
        return selectedFormation;
    }

    /**
     * Picks a formation, greying the rest so the choice is visible at a glance.
     *
     * @param formationName the formation to select, or {@code null} to select nothing
     */
    public void selectFormation(@Nullable String formationName) {
        selectedFormation = formationName;
        if (formationName == null) {
            selection.clearSelection();
        } else {
            JRadioButton chosen = choices.get(formationName);
            if (chosen != null) {
                chosen.setSelected(true);
            }
        }
        // Everything not chosen is dimmed rather than disabled: it still has to be clickable to become the next
        // choice, but it should not compete with the one in force.
        choices.forEach((name, button) -> button.setForeground(
              ((selectedFormation == null) || selectedFormation.equals(name))
                    ? UIManager.getColor("Label.foreground")
                    : UIManager.getColor("Label.disabledForeground")));
        selectionListeners.forEach(Runnable::run);
    }

    /** Clears the choice, so nothing is selected. */
    public void reset() {
        selectFormation(null);
    }

    /**
     * Registers a listener notified whenever the chosen formation changes.
     *
     * @param listener run after each change
     */
    public void addSelectionListener(Runnable listener) {
        selectionListeners.add(listener);
    }

    /**
     * @return {@code true} when this force offers nothing that could be adjusted
     */
    public boolean isEmpty() {
        return choices.isEmpty();
    }
}
