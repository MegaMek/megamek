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

package megamek.client.ui.dialogs.customMek;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.panels.DialogOptionComponentYPanel;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.UIUtil.FixedYPanel;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.options.SpaCatalog;
import megamek.common.options.SpaCatalogEntry;
import megamek.common.options.SpaImplementationStatus;
import megamek.common.units.Entity;

/**
 * The pilot options ("Special Pilot Abilities") portion of the pilot tab in {@link CustomMekDialog}, styled after
 * {@link QuirksPanel}: one titled panel per option group, responsive multi-column layout, alphabetically sorted
 * rows, and selected options highlighted.
 *
 * <p>On top of the live options, the panel shows the CamOps SPAs MegaMek does not implement as grayed-out,
 * searchable placeholder rows, marks partially implemented SPAs with a " (P)" name suffix, and carries each
 * ability's status, book reference, and missing-rules text in its tooltip - all driven by {@link SpaCatalog}. A
 * filter field narrows all groups by name and effect text; filtering only changes which rows are laid out and
 * never rebuilds the row components, so in-progress edits survive.</p>
 *
 * <p>This panel owns presentation only. Cross-option rules (implant exclusivity, equipment sync) stay with the
 * dialog, which receives every {@link DialogOptionListener} event; per-option extras (choice values, inline
 * prosthetic controls) are injected through the {@link OptionRowConfigurator} the dialog passes in.</p>
 */
public class PilotOptionsPanel extends JPanel implements DialogOptionListener {
    @Serial
    private static final long serialVersionUID = 4171284214461943479L;

    /** Horizontal gap between columns, added to the widest row when computing the column count. */
    private static final int COLUMN_GAP = 8;
    /** The grid cell's own horizontal insets, subtracted from a cell's share before label wrapping. */
    private static final int CELL_INSET_WIDTH = 8;
    private static final Color SELECTED_OPTION_COLOR = Color.YELLOW;
    private static final String STATUS_MARKER_NAME = "spaStatusMarker";

    /**
     * Hook for the dialog to finish building one option row: adding choice values, selection state, and inline
     * controls that belong to dialog-level rules.
     */
    @FunctionalInterface
    public interface OptionRowConfigurator {
        void configure(IOption option, DialogOptionComponentYPanel optionComp);
    }

    /**
     * One row in a group panel: a live {@link DialogOptionComponentYPanel} or a placeholder panel, with its sort
     * key and searchable text.
     */
    private record OptionRow(JComponent component, String sortKey, String searchText, boolean placeholder) {

        boolean matches(String normalizedFilter) {
            return matchesFilter(searchText, normalizedFilter);
        }
    }

    /**
     * @param normalizedSearchText a row's search text, already {@link #normalize(String) normalized}
     * @param normalizedFilter     the filter text, already normalized
     *
     * @return {@code true} if the row should stay visible: an empty filter matches everything, otherwise the
     *       search text must contain the filter
     */
    static boolean matchesFilter(String normalizedSearchText, String normalizedFilter) {
        return normalizedFilter.isEmpty() || normalizedSearchText.contains(normalizedFilter);
    }

    private final Entity entity;
    private final boolean editable;
    private final GameOptions gameOptions;
    private final DialogOptionListener parentListener;
    private final OptionRowConfigurator rowConfigurator;

    private final List<DialogOptionComponentYPanel> optionComps = new ArrayList<>();

    private final JTextField filterField = new JTextField();
    private final JLabel matchCountLabel = new JLabel();
    private final JCheckBox showUnimplementedCheck =
          new JCheckBox(Messages.getString("CustomMekDialog.showUnimplemented"), true);
    /** The search field row; hidden when the game options leave no ability group to search. */
    private JPanel filterBar;
    /**
     * Holds the visible columns side by side in equal shares of the width - normally halves: the advantages group
     * and the Edge column (Edge with the implant groups tucked under it). {@link #applyFilter()} re-adds only the
     * non-empty columns, so a filtered-out group gives its share to the others instead of leaving a blank column.
     */
    private final JPanel groupsContainer = new JPanel(new GridLayout(1, 0, UIUtil.scaleForGUI(8), 0));

    // Group panel -> all of its rows (visible or not), in sorted order
    private final Map<JPanel, List<OptionRow>> panelRows = new LinkedHashMap<>();
    private final Map<JPanel, Integer> panelMaxRowWidth = new HashMap<>();
    private final Map<JPanel, List<OptionRow>> panelVisibleRows = new HashMap<>();
    private final Map<JPanel, Integer> panelLastCalculatedCols = new HashMap<>();
    private final Map<JPanel, Integer> panelLastLayoutWidth = new HashMap<>();

    // Edge and the implant groups (Manei Domini, EI) share one column - Edge on top, MD and EI tucked under it -
    // so the options area reads as halves: advantages | edge with implants below
    private JPanel edgeGroupPanel;
    private JPanel eiGroupPanel;
    private JPanel mdGroupPanel;

    public PilotOptionsPanel(Entity entity, boolean editable, GameOptions gameOptions,
          DialogOptionListener parentListener, OptionRowConfigurator rowConfigurator) {
        this.entity = entity;
        this.editable = editable;
        this.gameOptions = gameOptions;
        this.parentListener = parentListener;
        this.rowConfigurator = rowConfigurator;

        setLayout(new GridBagLayout());
        add(buildFilterBar(), GBC.eol().fill(GridBagConstraints.HORIZONTAL).weightX(1.0));
        add(groupsContainer, GBC.eol().fill(GridBagConstraints.HORIZONTAL).weightX(1.0));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                refreshGroupLayouts();
            }
        });
    }

    private JPanel buildFilterBar() {
        filterField.setName("txtSpaFilter");
        filterField.putClientProperty("JTextField.placeholderText",
              Messages.getString("CustomMekDialog.spaFilterPlaceholder"));
        filterField.setToolTipText(Messages.getString("CustomMekDialog.spaFilterTooltip"));
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                applyFilter();
            }
        });

        showUnimplementedCheck.setName("chkShowUnimplementedSpas");
        showUnimplementedCheck.setToolTipText(Messages.getString("CustomMekDialog.showUnimplementedTooltip"));
        showUnimplementedCheck.addActionListener(event -> applyFilter());

        matchCountLabel.setName("lblSpaMatchCount");
        matchCountLabel.setVisible(false);

        filterBar = new FixedYPanel(new FlowLayout(FlowLayout.LEFT, UIUtil.scaleForGUI(5), 2));
        filterField.setColumns(20);
        filterBar.add(filterField);
        filterBar.add(showUnimplementedCheck);
        filterBar.add(matchCountLabel);
        return filterBar;
    }

    /**
     * Rebuilds every group panel from the given options. Called when the dialog (re)loads pilot options; a running
     * filter is re-applied to the fresh rows.
     */
    public void refreshOptions(PilotOptions options) {
        groupsContainer.removeAll();
        optionComps.clear();
        panelRows.clear();
        panelMaxRowWidth.clear();
        panelVisibleRows.clear();
        panelLastCalculatedCols.clear();
        panelLastLayoutWidth.clear();
        edgeGroupPanel = null;
        eiGroupPanel = null;
        mdGroupPanel = null;

        for (Enumeration<IOptionGroup> groups = options.getGroups(); groups.hasMoreElements(); ) {
            IOptionGroup group = groups.nextElement();
            if (isGroupHidden(group)) {
                continue;
            }

            List<OptionRow> rows = new ArrayList<>();
            for (Enumeration<IOption> groupOptions = group.getSortedOptions(); groupOptions.hasMoreElements(); ) {
                IOption option = groupOptions.nextElement();
                if (isOptionHidden(option)) {
                    continue;
                }
                rows.add(buildOptionRow(option));
            }

            if (PilotOptions.LVL3_ADVANTAGES.equals(group.getKey())
                  && !entity.isBuildingEntityOrGunEmplacement()) {
                for (SpaCatalogEntry placeholder : SpaCatalog.getPlaceholders()) {
                    rows.add(buildPlaceholderRow(placeholder));
                }
            }

            if (rows.isEmpty()) {
                continue;
            }
            rows.sort(Comparator.comparing(OptionRow::sortKey));

            int borderPadding = UIUtil.scaleForGUI(5);
            JPanel groupPanel = new JPanel(new GridBagLayout());
            groupPanel.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createTitledBorder(group.getDisplayableName()),
                  BorderFactory.createEmptyBorder(borderPadding, borderPadding, borderPadding, borderPadding)));
            // Membership of groupsContainer is managed by applyFilter(), which adds the non-empty groups
            if (PilotOptions.EDGE_ADVANTAGES.equalsIgnoreCase(group.getKey())) {
                edgeGroupPanel = groupPanel;
            } else if (PilotOptions.EI_ADVANTAGES.equalsIgnoreCase(group.getKey())) {
                eiGroupPanel = groupPanel;
            } else if (PilotOptions.MD_ADVANTAGES.equalsIgnoreCase(group.getKey())) {
                mdGroupPanel = groupPanel;
            }

            panelRows.put(groupPanel, rows);
            int maxRowWidth = 0;
            for (OptionRow row : rows) {
                maxRowWidth = Math.max(maxRowWidth, row.component().getPreferredSize().width);
            }
            panelMaxRowWidth.put(groupPanel, Math.max(maxRowWidth, 1));
        }

        // No point offering a search when the game options leave nothing to search
        filterBar.setVisible(!panelRows.isEmpty());

        applyFilter();
        revalidate();
        repaint();
    }

    /** @return the live option components, for the dialog's cross-option rules and for saving values */
    public List<DialogOptionComponentYPanel> getOptionComponents() {
        return optionComps;
    }

    /**
     * @return {@code true} if the whole option group is switched off by the game options (RPG pilot advantages, edge,
     *       Manei Domini, neural interfaces)
     */
    private boolean isGroupHidden(IOptionGroup group) {
        if (group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES)) {
            return !gameOptions.booleanOption(OptionsConstants.RPG_PILOT_ADVANTAGES);
        }
        if (group.getKey().equalsIgnoreCase(PilotOptions.EDGE_ADVANTAGES)) {
            return !gameOptions.booleanOption(OptionsConstants.EDGE);
        }
        if (group.getKey().equalsIgnoreCase(PilotOptions.MD_ADVANTAGES)) {
            return !gameOptions.booleanOption(OptionsConstants.RPG_MANEI_DOMINI);
        }
        if (group.getKey().equalsIgnoreCase(PilotOptions.EI_ADVANTAGES)) {
            IOption neuralInterfaceOption = gameOptions.getOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE);
            String neuralInterfaceMode = (neuralInterfaceOption == null)
                  ? OptionsConstants.NEURAL_INTERFACE_MODE_OFF
                  : neuralInterfaceOption.stringValue();
            return OptionsConstants.NEURAL_INTERFACE_MODE_OFF.equals(neuralInterfaceMode);
        }
        return false;
    }

    /** @return {@code true} if this option does not apply to the entity being customized */
    private boolean isOptionHidden(IOption option) {
        if (entity.isBuildingEntityOrGunEmplacement()) {
            return true;
        }

        String optionName = option.getName();

        // Direct neural interfaces make no sense for conventional infantry
        if (entity.isConventionalInfantry()
              && (optionName.equals(OptionsConstants.MD_VDNI) || optionName.equals(OptionsConstants.MD_BVDNI))) {
            return true;
        }

        // Prosthetic enhancements (IO p.84), sensory implants, and gas effusers (IO p.79) are infantry-only
        boolean isInfantryOnlyOption = switch (optionName) {
            case OptionsConstants.MD_PL_ENHANCED,
                 OptionsConstants.MD_PL_I_ENHANCED,
                 OptionsConstants.MD_PL_MASC,
                 OptionsConstants.MD_CYBER_IMP_AUDIO,
                 OptionsConstants.MD_CYBER_IMP_VISUAL,
                 OptionsConstants.MD_CYBER_IMP_LASER,
                 OptionsConstants.MD_CYBER_IMP_TELE,
                 OptionsConstants.MD_GAS_EFFUSER_PHEROMONE,
                 OptionsConstants.MD_GAS_EFFUSER_TOXIN -> true;
            default -> false;
        };
        return !entity.isConventionalInfantry() && isInfantryOnlyOption;
    }

    private OptionRow buildOptionRow(IOption option) {
        DialogOptionComponentYPanel optionComp = new DialogOptionComponentYPanel(this, option, editable);
        rowConfigurator.configure(option, optionComp);
        optionComps.add(optionComp);

        StringBuilder searchText = new StringBuilder(option.getDisplayableName());
        SpaCatalog.getEntry(option.getName()).ifPresent(catalogEntry -> {
            decorateWithStatus(optionComp, catalogEntry);
            searchText.append(' ').append(catalogEntry.getAbbreviatedEffect());
        });
        if (option.getDescription() != null) {
            searchText.append(' ').append(option.getDescription());
        }

        updateOptionFontStyle(optionComp, isOptionSet(optionComp, option));
        return new OptionRow(optionComp, normalize(option.getDisplayableName()), normalize(searchText.toString()),
              false);
    }

    /**
     * Builds the grayed-out row for a CamOps SPA MegaMek does not implement: a permanently disabled checkbox, an
     * italic name label, and a status marker, with the book data in the tooltip.
     */
    private OptionRow buildPlaceholderRow(SpaCatalogEntry catalogEntry) {
        String displayName = catalogEntry.getDisplayableName();
        String tooltip = buildCatalogTooltip(catalogEntry, null);
        Color disabledColor = UIManager.getColor("Label.disabledForeground");

        JPanel placeholderPanel = new FixedYPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        placeholderPanel.setToolTipText(tooltip);

        JCheckBox placeholderCheckbox = new JCheckBox();
        placeholderCheckbox.setEnabled(false);
        placeholderCheckbox.setToolTipText(tooltip);

        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.ITALIC));
        nameLabel.setForeground(disabledColor);
        nameLabel.setToolTipText(tooltip);

        JLabel marker = new JLabel(Messages.getString("CustomMekDialog.spaMarkerNotImplemented"));
        marker.setName(STATUS_MARKER_NAME);
        marker.setForeground(disabledColor);
        marker.setToolTipText(tooltip);

        placeholderPanel.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(10)));
        placeholderPanel.add(placeholderCheckbox);
        placeholderPanel.add(nameLabel);
        placeholderPanel.add(marker);

        String searchText = displayName + ' ' + catalogEntry.getAbbreviatedEffect();
        return new OptionRow(placeholderPanel, normalize(displayName), normalize(searchText), true);
    }

    /**
     * Replaces the row's tooltips with the catalog tooltip: status, book reference, effect summary, and - for
     * abilities with gaps - exactly which parts of the rule MegaMek does not honor. Implementation status lives
     * entirely in the tooltip; the row itself carries no status marker.
     */
    private void decorateWithStatus(DialogOptionComponentYPanel optionComp, SpaCatalogEntry catalogEntry) {
        String tooltip = buildCatalogTooltip(catalogEntry, optionComp.getOption().getDescription());
        for (Component child : optionComp.getComponents()) {
            ((JComponent) child).setToolTipText(tooltip);
        }
        optionComp.setToolTipText(tooltip);
        if (catalogEntry.status() == SpaImplementationStatus.PARTIAL) {
            optionComp.setNameSuffix(Messages.getString("CustomMekDialog.spaPartialSuffix"));
        }
    }

    private String buildCatalogTooltip(SpaCatalogEntry catalogEntry, String optionDescription) {
        StringBuilder tooltip = new StringBuilder("<html><div width=500><b>")
              .append(Messages.getString("CustomMekDialog.spaTooltipBookLine",
                    catalogEntry.status().getDisplayableName(), catalogEntry.camOpsPage(),
                    catalogEntry.costText()))
              .append("</b><br><i>")
              .append(catalogEntry.getAbbreviatedEffect())
              .append("</i>");
        String missingText = catalogEntry.getMissingText();
        if (missingText != null) {
            tooltip.append("<br><b>")
                  .append(Messages.getString("CustomMekDialog.spaTooltipMissing"))
                  .append("</b> ")
                  .append(missingText);
        }
        if ((optionDescription != null) && !optionDescription.isBlank()) {
            tooltip.append("<br><br>").append(optionDescription.replace("\n", "<br>"));
        }
        return tooltip.append("</div></html>").toString();
    }

    /** Highlights an option whose value differs from the default, matching the quirks panel treatment. */
    private void updateOptionFontStyle(DialogOptionComponentYPanel optionComp, boolean selected) {
        for (Component child : optionComp.getComponents()) {
            if (STATUS_MARKER_NAME.equals(child.getName())) {
                continue;
            }
            if (child.getFont() != null) {
                child.setForeground(selected ? SELECTED_OPTION_COLOR : null);
            }
        }
        optionComp.repaint();
    }

    private boolean isOptionSet(DialogOptionComponentYPanel optionComp, IOption option) {
        if (option.getType() == IOption.BOOLEAN) {
            return option.booleanValue();
        }
        return !optionComp.isDefaultValue();
    }

    static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT);
    }

    /** Recomputes which rows are visible under the current filter text and unimplemented toggle, then lays out. */
    private void applyFilter() {
        String normalizedFilter = normalize(filterField.getText().trim());
        boolean showUnimplemented = showUnimplementedCheck.isSelected();

        int totalRows = 0;
        int shownRows = 0;
        groupsContainer.removeAll();
        for (Map.Entry<JPanel, List<OptionRow>> panelEntry : panelRows.entrySet()) {
            JPanel groupPanel = panelEntry.getKey();
            List<OptionRow> visibleRows = new ArrayList<>();
            for (OptionRow row : panelEntry.getValue()) {
                totalRows++;
                if (row.matches(normalizedFilter) && (showUnimplemented || !row.placeholder())) {
                    visibleRows.add(row);
                }
            }
            shownRows += visibleRows.size();
            panelVisibleRows.put(groupPanel, visibleRows);
            boolean isStackedGroup = (groupPanel == edgeGroupPanel) || (groupPanel == eiGroupPanel)
                  || (groupPanel == mdGroupPanel);
            if (!visibleRows.isEmpty() && !isStackedGroup) {
                groupsContainer.add(groupPanel);
            }
            int columns = calculateColumns(groupPanel);
            panelLastCalculatedCols.put(groupPanel, columns);
            panelLastLayoutWidth.put(groupPanel, calculateAvailableWidth(groupPanel));
            relayoutGroupPanel(groupPanel, visibleRows, columns);
        }
        addEdgeColumn();

        matchCountLabel.setText(Messages.getString("CustomMekDialog.spaMatchCount", shownRows, totalRows));
        matchCountLabel.setVisible(!normalizedFilter.isEmpty());
        revalidate();
        repaint();
        // The widths used above are from BEFORE this filter pass changed which columns groupsContainer holds
        // (e.g. a filtered-out Edge column hands its half to the advantages group). The resize listener cannot
        // catch that - this panel's own size does not change - so recheck once the pending layout has run.
        SwingUtilities.invokeLater(this::refreshGroupLayouts);
    }

    /**
     * Adds the Edge column: Edge on top with the Manei Domini and EI groups tucked under it, each in its own
     * titled panel. Sharing one column keeps the options area at halves - advantages | edge and implants - instead
     * of every option-gated group claiming a column of its own.
     */
    private void addEdgeColumn() {
        List<JPanel> stackedPanels = new ArrayList<>();
        for (JPanel stackedCandidate : new JPanel[] { edgeGroupPanel, mdGroupPanel, eiGroupPanel }) {
            if ((stackedCandidate != null) && !panelVisibleRows.get(stackedCandidate).isEmpty()) {
                stackedPanels.add(stackedCandidate);
            }
        }
        if (stackedPanels.isEmpty()) {
            return;
        }

        JPanel edgeColumn = new JPanel(new GridBagLayout());
        for (JPanel stackedPanel : stackedPanels) {
            edgeColumn.add(stackedPanel, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(0, 0, 0, 8));
        }
        edgeColumn.add(new JPanel(), GBC.eol().fill(GridBagConstraints.BOTH));
        groupsContainer.add(edgeColumn);
    }

    /**
     * Brings every group in line with its current width: a changed column count gets a full relayout; a changed
     * width alone (same column count) gets its label wrap widths refreshed, so wrapped names track the width
     * even when no column boundary is crossed.
     */
    private void refreshGroupLayouts() {
        for (Map.Entry<JPanel, List<OptionRow>> panelEntry : panelVisibleRows.entrySet()) {
            JPanel groupPanel = panelEntry.getKey();
            int columns = calculateColumns(groupPanel);
            int availableWidth = calculateAvailableWidth(groupPanel);
            Integer lastColumns = panelLastCalculatedCols.get(groupPanel);
            Integer lastWidth = panelLastLayoutWidth.get(groupPanel);
            boolean columnsChanged = (lastColumns == null) || (columns != lastColumns);
            boolean widthChanged = (lastWidth == null) || (availableWidth != lastWidth);
            if (!columnsChanged && !widthChanged) {
                continue;
            }
            panelLastCalculatedCols.put(groupPanel, columns);
            panelLastLayoutWidth.put(groupPanel, availableWidth);
            if (columnsChanged) {
                relayoutGroupPanel(groupPanel, panelEntry.getValue(), columns);
            } else {
                applyLabelWrapWidths(groupPanel, panelEntry.getValue(), columns);
            }
        }
    }

    /** @return the width inside the group panel's border, estimated pre-layout from the panel's usual share */
    private int calculateAvailableWidth(JPanel groupPanel) {
        int availableWidth = groupPanel.getWidth();
        if (availableWidth <= 0) {
            // Not laid out yet - estimate this group's share of the usual two-column arrangement
            availableWidth = getWidth() / 2;
        }
        Insets groupInsets = groupPanel.getInsets();
        return availableWidth - groupInsets.left - groupInsets.right;
    }

    private int calculateColumns(JPanel groupPanel) {
        int availableWidth = calculateAvailableWidth(groupPanel);
        int maxRowWidth = panelMaxRowWidth.getOrDefault(groupPanel, 1);
        if ((availableWidth <= 0) || (maxRowWidth <= 0)) {
            return 1;
        }
        return Math.max(1, availableWidth / (maxRowWidth + COLUMN_GAP));
    }

    /**
     * Wraps option names whose row is wider than its column (long Edge triggers, choice rows with wide combo
     * boxes) instead of clipping them; each row subtracts its own controls from the budget. Cheap to repeat: the
     * labels no-op when their text would not change.
     */
    private void applyLabelWrapWidths(JPanel groupPanel, List<OptionRow> visibleRows, int columns) {
        if (columns <= 0) {
            return;
        }
        int rowWidth = (calculateAvailableWidth(groupPanel) / columns) - UIUtil.scaleForGUI(CELL_INSET_WIDTH);
        for (OptionRow row : visibleRows) {
            if (row.component() instanceof DialogOptionComponentYPanel optionComp) {
                optionComp.setNameLabelWrapWidth(rowWidth);
            }
        }
    }

    /** Arranges the visible rows of one group into a grid, reusing the existing row components. */
    private void relayoutGroupPanel(JPanel groupPanel, List<OptionRow> visibleRows, int columns) {
        groupPanel.removeAll();
        if (!visibleRows.isEmpty() && (columns > 0)) {
            applyLabelWrapWidths(groupPanel, visibleRows, columns);

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.weightx = 1.0;
            constraints.insets = new Insets(0, 2, 0, 2);

            int currentColumn = 0;
            for (OptionRow row : visibleRows) {
                constraints.gridx = currentColumn;
                groupPanel.add(row.component(), constraints);
                currentColumn++;
                if (currentColumn >= columns) {
                    currentColumn = 0;
                    constraints.gridy++;
                }
            }

            // Bottom glue keeps the rows at the top when the side-by-side groups get equal heights
            constraints.gridx = 0;
            constraints.gridy++;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.weighty = 1.0;
            constraints.fill = GridBagConstraints.BOTH;
            groupPanel.add(new JPanel(), constraints);
        }
        groupPanel.revalidate();
        groupPanel.repaint();
    }

    /**
     * Reports a nominal width instead of the option columns' natural width (three times the widest row, which can
     * exceed any viewport). The columns adapt to whatever width they are given; without this cap their natural
     * width would make the width-tracking crew tab starve the sections above, crushing the name and skill fields
     * to slivers.
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(UIUtil.scaleForGUI(300), super.getPreferredSize().height);
    }

    @Override
    public void optionClicked(DialogOptionComponentYPanel optionComp, IOption option, boolean state) {
        if (option.getType() == IOption.BOOLEAN) {
            updateOptionFontStyle(optionComp, state);
        } else {
            Object value = optionComp.getValue();
            updateOptionFontStyle(optionComp, (value != null) && !value.equals(option.getDefault()));
        }
        if (parentListener != null) {
            parentListener.optionClicked(optionComp, option, state);
        }
    }

    @Override
    public void optionSwitched(DialogOptionComponentYPanel optionComp, IOption option, int value) {
        if (parentListener != null) {
            parentListener.optionSwitched(optionComp, option, value);
        }
    }
}
