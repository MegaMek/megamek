/*
 * Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2012-2026 The MegaMek Team. All Rights Reserved.
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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.panels.DialogOptionComponentYPanel;
import megamek.client.ui.panels.OptionFilterBar;
import megamek.client.ui.panels.OptionFilterBarLabels;
import megamek.client.ui.panels.OptionRowLayout;
import megamek.client.ui.panels.OptionSearchFilter;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.UIUtil.FixedYPanel;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.Mounted;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.options.QuirkCatalog;
import megamek.common.options.QuirkCatalogEntry;
import megamek.common.options.QuirkImplementationStatus;
import megamek.common.options.QuirkKind;
import megamek.common.options.QuirkPlaceholder;
import megamek.common.options.Quirks;
import megamek.common.options.WeaponQuirks;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * The quirks tab of {@link CustomMekDialog}: the unit's chassis quirks in a positive and a negative column, and the
 * quirks of each weapon it carries.
 *
 * <p>Each quirk carries its MegaMek implementation status from {@link QuirkCatalog}: a quirk MegaMek ignores is
 * grayed and marked " (NI)", one it only partly honors " (P)", with the status, rules reference and description in
 * the tooltip. A quirk MegaMek ignores stays fully editable - canon unit files set these quirks, saved games keep
 * them, and MekHQ reads several of them - the styling only says the MegaMek engine will not act on it. Book quirks
 * MegaMek has no option for at all appear as disabled {@link QuirkPlaceholder} rows.</p>
 *
 * <p>The "show unimplemented" toggle hides every quirk MegaMek will not act on, of either sort. A quirk the unit
 * actually has is always listed regardless, so hiding can never conceal something set on this unit.</p>
 *
 * <p>A search field filters all three columns at once by quirk name and description. Filtering only changes which
 * rows are laid out and never rebuilds the row components, so in-progress edits and selections survive.</p>
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 2012-03-05
 */
public class QuirksPanel extends JPanel implements DialogOptionListener {
    @Serial
    private static final long serialVersionUID = -8360885055638738148L;
    private static final MMLogger LOGGER = MMLogger.create(QuirksPanel.class);
    private static final boolean SORT_QUIRKS_ALPHABETICALLY = true;
    /** Fallback width used when no row has reported a preferred width yet. */
    private static final int FALLBACK_ITEM_WIDTH = 150;

    /**
     * One row in a quirk panel: a live {@link DialogOptionComponentYPanel} or a grayed placeholder panel, with its
     * sort key and the text the search field matches against.
     *
     * @param component    the row's Swing component, reused across filter passes
     * @param sortKey      the normalized display name, for alphabetical ordering
     * @param searchText   the normalized name and description, matched against the filter
     * @param placeholder  {@code true} for a book quirk MegaMek has no option for
     * @param noGameEffect {@code true} when MegaMek will not act on this quirk, whether because it ignores the
     *                     option or because it has none. These are the rows the "show unimplemented" toggle hides.
     */
    private record QuirkRow(JComponent component, String sortKey, String searchText, boolean placeholder,
                            boolean noGameEffect) {

        boolean matches(String normalizedFilter) {
            return OptionSearchFilter.matches(searchText, normalizedFilter);
        }

        /**
         * @return {@code true} if this row carries a value the player has set. A placeholder can never be set, and
         *       a live row is read from its component so the answer tracks edits made in the dialog.
         */
        boolean isSet() {
            return (component instanceof DialogOptionComponentYPanel optionComp) && !optionComp.isDefaultValue();
        }
    }

    private final Entity entity;
    private List<DialogOptionComponentYPanel> quirkComps;
    /** Equipment number -> that weapon's quirk row components. */
    private final Map<Integer, List<DialogOptionComponentYPanel>> weaponQuirkComps = new HashMap<>();
    /** Equipment number -> that weapon's quirks, as handed in by the dialog. */
    private final HashMap<Integer, WeaponQuirks> weaponQuirks;
    private final Quirks quirks;
    private final boolean editable;
    private final DialogOptionListener parent;

    /** The search row above the columns; its "show unimplemented" state is persisted in {@link GUIPreferences}. */
    private final OptionFilterBar filterBar;

    // Quirk panel -> all of its rows (visible or not), in sorted order
    private final Map<JPanel, List<QuirkRow>> panelRows = new LinkedHashMap<>();
    private final Map<JPanel, List<QuirkRow>> panelVisibleRows = new LinkedHashMap<>();
    private final Map<JPanel, Integer> panelLastCalculatedCols = new HashMap<>();
    /** The weapon panels, which get a responsive column count; the chassis panels stay single-column. */
    private final Set<JPanel> responsivePanels = new LinkedHashSet<>();
    private final Map<DialogOptionComponentYPanel, Dimension> originalPreferredSizes = new HashMap<>();
    /** Rows for quirks MegaMek ignores, which sit at the disabled foreground colour while unset. */
    private final Set<DialogOptionComponentYPanel> noGameEffectComps = new LinkedHashSet<>();
    private int globalMaxItemWidth = 0;
    /**
     * Holds the three quirk columns. {@link #refreshQuirks()} rebuilds only this, so the filter bar above it - and
     * the listeners registered on it - are built exactly once.
     */
    private final JPanel quirksContainer = new JPanel(new GridBagLayout());

    public QuirksPanel(Entity entity, Quirks quirks, boolean editable, DialogOptionListener parent,
          HashMap<Integer, WeaponQuirks> weaponQuirks) {
        this.entity = entity;
        this.quirks = quirks;
        this.editable = editable;
        this.parent = parent;
        this.weaponQuirks = weaponQuirks;
        this.filterBar = buildFilterBar();

        setLayout(new GridBagLayout());
        add(filterBar, GBC.eol().fill(GridBagConstraints.HORIZONTAL).weightX(1.0));
        add(quirksContainer, GBC.eol().fill().weightX(1.0).weighty(1.0));

        // Add resize listener for responsive column layout
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                triggerRelayoutCheck();
            }
        });

        refreshQuirks();
    }

    public void refreshQuirks() {
        // Cleanup - only the quirk columns; the filter bar and its listeners survive
        quirksContainer.removeAll();
        quirkComps = new ArrayList<>();
        for (Integer equipmentNumber : weaponQuirks.keySet()) {
            weaponQuirkComps.put(equipmentNumber, new ArrayList<>());
        }
        panelRows.clear();
        panelVisibleRows.clear();
        panelLastCalculatedCols.clear();
        responsivePanels.clear();
        originalPreferredSizes.clear();
        noGameEffectComps.clear();
        globalMaxItemWidth = 0;

        List<DialogOptionComponentYPanel> allQuirks = new ArrayList<>();
        List<QuirkRow> positiveQuirkRows = new ArrayList<>();
        List<QuirkRow> negativeQuirkRows = new ArrayList<>();

        JPanel positiveQuirksPanel = new JPanel(new GridBagLayout());
        positiveQuirksPanel.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("CustomMekDialog.quirksPositiveTitle")));
        JPanel negativeQuirksPanel = new JPanel(new GridBagLayout());
        negativeQuirksPanel.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("CustomMekDialog.quirksNegativeTitle")));

        collectChassisQuirks(positiveQuirkRows, negativeQuirkRows, allQuirks);
        addPlaceholderRows(Quirks.POS_QUIRKS, positiveQuirkRows);
        addPlaceholderRows(Quirks.NEG_QUIRKS, negativeQuirkRows);

        if (SORT_QUIRKS_ALPHABETICALLY) {
            positiveQuirkRows.sort(Comparator.comparing(QuirkRow::sortKey));
            negativeQuirkRows.sort(Comparator.comparing(QuirkRow::sortKey));
        }

        JPanel weaponQuirksContainer = new JPanel(new GridBagLayout());
        weaponQuirksContainer.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("CustomMekDialog.quirksWeaponTitle")));
        collectWeaponQuirks(weaponQuirksContainer, allQuirks);

        calculateGlobalMaxWidth(allQuirks);

        // Note: only the weapon panels are registered as responsive, so the chassis columns stay single-column
        panelRows.put(positiveQuirksPanel, positiveQuirkRows);
        panelRows.put(negativeQuirksPanel, negativeQuirkRows);

        JScrollPane positiveScrollPane = new JScrollPane(positiveQuirksPanel);
        positiveScrollPane.setBorder(null);
        JScrollPane negativeScrollPane = new JScrollPane(negativeQuirksPanel);
        negativeScrollPane.setBorder(null);
        JScrollPane weaponScrollPane = new JScrollPane(weaponQuirksContainer);
        weaponScrollPane.setBorder(null);

        // Create nested split panes for three-way horizontal split
        JSplitPane leftSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
              positiveScrollPane, negativeScrollPane);
        leftSplitPane.setResizeWeight(0.5);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
              leftSplitPane, weaponScrollPane);
        mainSplitPane.setResizeWeight(0.67);

        quirksContainer.add(mainSplitPane, GBC.eol().fill().weightX(1.0).weighty(1.0));

        applyFilter();
        logQuirkListComposition();
        validate();
        repaint();

        // Set divider locations and trigger layout after component is shown
        if (isShowing()) {
            SwingUtilities.invokeLater(() -> {
                leftSplitPane.setDividerLocation(0.5);
                mainSplitPane.setDividerLocation(0.67);
                // Small delay to ensure dividers are positioned before responsive layout
                SwingUtilities.invokeLater(this::triggerRelayoutCheck);
            });
        } else {
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent event) {
                    leftSplitPane.setDividerLocation(0.5);
                    mainSplitPane.setDividerLocation(0.67);
                    // Small delay to ensure dividers are positioned before responsive layout
                    SwingUtilities.invokeLater(() -> triggerRelayoutCheck());
                    removeComponentListener(this);
                }
            });
        }
    }

    /**
     * Records what the freshly built quirk list contains, so a playtest report of "quirks are missing from the
     * tab" can be answered from megamek.log: the toggle hides every quirk MegaMek will not act on, and a unit's
     * legal quirk set already varies by chassis type.
     */
    private void logQuirkListComposition() {
        int totalRows = 0;
        int hiddenRows = 0;
        for (List<QuirkRow> rows : panelRows.values()) {
            totalRows += rows.size();
            for (QuirkRow row : rows) {
                if (isHiddenByToggle(row, filterBar.isShowUnimplementedSelected())) {
                    hiddenRows++;
                }
            }
        }
        LOGGER.debug("[QuirkSearch] {}: {} quirk rows built, {} hidden (show-unimplemented={}, filter='{}')",
              entity.getShortName(), totalRows, hiddenRows, filterBar.isShowUnimplementedSelected(),
              filterBar.getFilterText());
    }

    /** Builds the search row shown above the three columns. */
    private OptionFilterBar buildFilterBar() {
        OptionFilterBarLabels labels = new OptionFilterBarLabels("txtQuirkFilter",
              Messages.getString("CustomMekDialog.quirkFilterPlaceholder"),
              Messages.getString("CustomMekDialog.quirkFilterTooltip"),
              "chkShowUnimplementedQuirks",
              Messages.getString("CustomMekDialog.showUnimplementedQuirks"),
              Messages.getString("CustomMekDialog.showUnimplementedQuirksTooltip"),
              "lblQuirkMatchCount",
              Messages.getString("CustomMekDialog.quirkMatchCount"));
        return new OptionFilterBar(labels, GUIPreferences.getInstance().getShowUnimplementedQuirks(),
              this::applyFilter,
              showUnimplemented -> {
                  GUIPreferences.getInstance().setShowUnimplementedQuirks(showUnimplemented);
                  applyFilter();
              });
    }

    /** Collects the unit's legal chassis quirks into the positive and negative row lists. */
    private void collectChassisQuirks(List<QuirkRow> positiveQuirkRows, List<QuirkRow> negativeQuirkRows,
          List<DialogOptionComponentYPanel> allQuirks) {
        for (Enumeration<IOptionGroup> groups = quirks.getGroups(); groups.hasMoreElements(); ) {
            IOptionGroup group = groups.nextElement();
            boolean isPositive = Quirks.POS_QUIRKS.equals(group.getKey());
            boolean isNegative = Quirks.NEG_QUIRKS.equals(group.getKey());
            if (!isPositive && !isNegative) {
                continue;
            }

            List<QuirkRow> targetRows = isPositive ? positiveQuirkRows : negativeQuirkRows;
            for (Enumeration<IOption> groupOptions = group.getSortedOptions(); groupOptions.hasMoreElements(); ) {
                IOption option = groupOptions.nextElement();
                if ((null == option) || Quirks.isQuirkDisallowed(option, entity)) {
                    continue;
                }

                DialogOptionComponentYPanel optionComp = buildQuirkComponent(option, QuirkKind.UNIT);
                quirkComps.add(optionComp);
                allQuirks.add(optionComp);
                targetRows.add(toQuirkRow(option, optionComp, QuirkKind.UNIT));
            }
        }
    }

    /** Builds one titled panel per weapon that has legal quirks, each responsive to the column's width. */
    private void collectWeaponQuirks(JPanel weaponQuirksContainer, List<DialogOptionComponentYPanel> allQuirks) {
        GridBagConstraints weaponConstraints = new GridBagConstraints();
        weaponConstraints.gridx = 0;
        weaponConstraints.gridy = 0;
        weaponConstraints.weightx = 1.0;
        weaponConstraints.weighty = 0;
        weaponConstraints.fill = GridBagConstraints.HORIZONTAL;
        weaponConstraints.anchor = GridBagConstraints.NORTHWEST;
        weaponConstraints.insets = new Insets(2, 2, 2, 2);

        for (int equipmentNumber : weaponQuirks.keySet()) {
            Mounted<?> mounted = entity.getEquipment(equipmentNumber);
            WeaponQuirks mountedQuirks = weaponQuirks.get(equipmentNumber);
            List<QuirkRow> weaponQuirkRows = new ArrayList<>();

            for (Enumeration<IOptionGroup> groups = mountedQuirks.getGroups(); groups.hasMoreElements(); ) {
                IOptionGroup group = groups.nextElement();
                for (Enumeration<IOption> groupOptions = group.getSortedOptions(); groupOptions.hasMoreElements(); ) {
                    IOption option = groupOptions.nextElement();
                    if (WeaponQuirks.isQuirkDisallowed(option, entity, mounted.getType())) {
                        continue;
                    }

                    DialogOptionComponentYPanel optionComp = buildQuirkComponent(option, QuirkKind.WEAPON);
                    weaponQuirkComps.get(equipmentNumber).add(optionComp);
                    allQuirks.add(optionComp);
                    weaponQuirkRows.add(toQuirkRow(option, optionComp, QuirkKind.WEAPON));
                }
            }

            if (weaponQuirkRows.isEmpty()) {
                continue;
            }
            if (SORT_QUIRKS_ALPHABETICALLY) {
                weaponQuirkRows.sort(Comparator.comparing(QuirkRow::sortKey));
            }

            JPanel weaponPanel = new JPanel(new GridBagLayout());
            weaponPanel.setBorder(BorderFactory.createTitledBorder(
                  Messages.getString("CustomMekDialog.quirksWeaponPanelTitle",
                        mounted.getName(), entity.getLocationName(mounted.getLocation()))));
            weaponQuirksContainer.add(weaponPanel, weaponConstraints);
            weaponConstraints.gridy++;

            panelRows.put(weaponPanel, weaponQuirkRows);
            responsivePanels.add(weaponPanel);
        }

        // Add vertical glue to push weapon groups to the top
        weaponConstraints.weighty = 1.0;
        weaponConstraints.fill = GridBagConstraints.BOTH;
        weaponQuirksContainer.add(new JPanel(), weaponConstraints);
    }

    /** Creates a quirk's row component and decorates it with its implementation status. */
    private DialogOptionComponentYPanel buildQuirkComponent(IOption option, QuirkKind kind) {
        DialogOptionComponentYPanel optionComp = new DialogOptionComponentYPanel(this, option, editable);
        QuirkCatalog.getEntry(kind, option.getName()).ifPresent(catalogEntry ->
              decorateWithStatus(optionComp, catalogEntry));
        // Measured after decoration, so the status suffix and italics are part of the width the columns are sized on
        originalPreferredSizes.put(optionComp, optionComp.getPreferredSize());
        updateQuirkFontStyle(optionComp, isQuirkSet(optionComp, option));
        return optionComp;
    }

    /**
     * @return the searchable row wrapping a live quirk component. The filter matches the quirk's name and its
     *       description, so searching for an effect ("heat", "to hit") finds the quirks that mention it.
     */
    private QuirkRow toQuirkRow(IOption option, DialogOptionComponentYPanel optionComp, QuirkKind kind) {
        StringBuilder searchText = new StringBuilder(option.getDisplayableName());
        if (option.getDescription() != null) {
            searchText.append(' ').append(option.getDescription());
        }
        QuirkCatalog.getEntry(kind, option.getName())
              .map(QuirkCatalogEntry::getRulesReference)
              .ifPresent(rulesReference -> searchText.append(' ').append(rulesReference));
        boolean noGameEffect = QuirkCatalog.getEntry(kind, option.getName())
              .map(catalogEntry -> catalogEntry.status().hasNoGameEffect())
              .orElse(false);
        return new QuirkRow(optionComp, OptionSearchFilter.normalize(option.getDisplayableName()),
              OptionSearchFilter.normalize(searchText.toString()), false, noGameEffect);
    }

    /** Adds the grayed-out rows for the book quirks MegaMek has no option for in the given group. */
    private void addPlaceholderRows(String groupKey, List<QuirkRow> targetRows) {
        if (entity.isBuildingEntityOrGunEmplacement()) {
            return;
        }
        for (QuirkPlaceholder placeholder : QuirkCatalog.getPlaceholders(groupKey)) {
            targetRows.add(buildPlaceholderRow(placeholder));
        }
    }

    /**
     * Builds the grayed-out row for a book quirk MegaMek has no option for: a permanently disabled checkbox, an
     * italic name label and a status marker, with the book data in the tooltip.
     */
    private QuirkRow buildPlaceholderRow(QuirkPlaceholder placeholder) {
        String displayName = placeholder.getDisplayableName();
        String description = placeholder.getDescription();
        String tooltip = buildTooltip(QuirkImplementationStatus.NOT_IN_MEGAMEK, placeholder.getRulesReference(),
              description);
        Color disabledColor = disabledForeground();

        JPanel placeholderPanel = new FixedYPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        placeholderPanel.setToolTipText(tooltip);

        JCheckBox placeholderCheckbox = new JCheckBox();
        placeholderCheckbox.setEnabled(false);
        placeholderCheckbox.setToolTipText(tooltip);

        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.ITALIC));
        nameLabel.setForeground(disabledColor);
        nameLabel.setToolTipText(tooltip);

        JLabel marker = new JLabel(Messages.getString("CustomMekDialog.quirkMarkerNotInMegaMek"));
        marker.setName(OptionRowLayout.STATUS_MARKER_NAME);
        marker.setForeground(disabledColor);
        marker.setToolTipText(tooltip);

        placeholderPanel.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(10)));
        placeholderPanel.add(placeholderCheckbox);
        placeholderPanel.add(nameLabel);
        placeholderPanel.add(marker);

        // Rules reference included so a placeholder is findable by book and page, like the real quirk rows
        String searchText = displayName + ' ' + description + ' ' + placeholder.getRulesReference();
        return new QuirkRow(placeholderPanel, OptionSearchFilter.normalize(displayName),
              OptionSearchFilter.normalize(searchText), true, true);
    }

    /**
     * Marks a quirk whose rule MegaMek does not fully apply and replaces the row's tooltips with one carrying the
     * status, the rules reference and the quirk's description.
     *
     * <p>A quirk MegaMek ignores is grayed and italicized like the pilot tab's unimplemented rows, but unlike them
     * it stays editable: canon unit files set these quirks, saved games keep them, and MekHQ acts on several. The
     * styling says the engine will not act on it, not that it cannot be chosen.</p>
     */
    private void decorateWithStatus(DialogOptionComponentYPanel optionComp, QuirkCatalogEntry catalogEntry) {
        QuirkImplementationStatus status = catalogEntry.status();
        String tooltip = buildTooltip(status, catalogEntry.getRulesReference(),
              optionComp.getOption().getDescription());
        for (Component child : optionComp.getComponents()) {
            ((JComponent) child).setToolTipText(tooltip);
        }
        optionComp.setToolTipText(tooltip);

        if (status == QuirkImplementationStatus.PARTIAL) {
            optionComp.setNameSuffix(Messages.getString("CustomMekDialog.quirkPartialSuffix"));
        } else if (status == QuirkImplementationStatus.NOT_IMPLEMENTED) {
            optionComp.setNameSuffix(Messages.getString("CustomMekDialog.quirkNotImplementedSuffix"));
            grayOutRow(optionComp);
        }
    }

    /**
     * Applies the pilot tab's unimplemented-row look to a live quirk row: italic name in the disabled foreground
     * colour. The row's controls stay enabled; only its appearance changes.
     */
    private void grayOutRow(DialogOptionComponentYPanel optionComp) {
        noGameEffectComps.add(optionComp);
        for (Component child : optionComp.getComponents()) {
            if (child instanceof JLabel label) {
                label.setFont(label.getFont().deriveFont(Font.ITALIC));
            }
        }
    }

    /**
     * @param status         the quirk's implementation status
     * @param rulesReference the "Book p.Page" citation, or {@code null} when the bundle carries none
     * @param description    the quirk's own rules description, or {@code null}
     *
     * @return the HTML tooltip shown on every control of the row
     */
    private String buildTooltip(QuirkImplementationStatus status, @Nullable String rulesReference,
          @Nullable String description) {
        String statusLine = (rulesReference == null)
              ? status.getDisplayableName()
              : Messages.getString("CustomMekDialog.quirkTooltipStatusLine",
                    status.getDisplayableName(), rulesReference);
        StringBuilder tooltip = new StringBuilder("<html><div width=500><b>")
              .append(statusLine)
              .append("</b>");
        if (status == QuirkImplementationStatus.NOT_IMPLEMENTED) {
            tooltip.append("<br><i>")
                  .append(Messages.getString("CustomMekDialog.quirkTooltipNoGameEffect"))
                  .append("</i>");
        }
        if ((description != null) && !description.isBlank()) {
            tooltip.append("<br><br>").append(description.replace("\n", "<br>"));
        }
        return tooltip.append("</div></html>").toString();
    }

    /**
     * @param row               a row in one of the quirk panels
     * @param showUnimplemented whether the "show unimplemented" toggle is on
     *
     * @return {@code true} if the toggle should hide this row. With the toggle off, every quirk MegaMek will not
     *       act on drops out of the list - both the ones it ignores and the book quirks it has no option for. A
     *       quirk the unit actually has is the exception and always stays listed, so unticking the box can never
     *       hide something that is set on this unit.
     */
    private static boolean isHiddenByToggle(QuirkRow row, boolean showUnimplemented) {
        boolean isHidingUnimplemented = !showUnimplemented;
        boolean quirkDoesNothingInMegaMek = row.noGameEffect();
        boolean quirkIsSetOnThisUnit = row.isSet();
        return isHidingUnimplemented && quirkDoesNothingInMegaMek && !quirkIsSetOnThisUnit;
    }

    /** Recomputes which rows are visible under the current filter text and the toggle, then lays out. */
    private void applyFilter() {
        String normalizedFilter = OptionSearchFilter.normalize(filterBar.getFilterText());
        boolean showUnimplemented = filterBar.isShowUnimplementedSelected();

        int totalRows = 0;
        int shownRows = 0;
        for (Map.Entry<JPanel, List<QuirkRow>> panelEntry : panelRows.entrySet()) {
            JPanel quirkPanel = panelEntry.getKey();
            List<QuirkRow> visibleRows = new ArrayList<>();
            for (QuirkRow row : panelEntry.getValue()) {
                // Placeholders are only ever offered as extra information, so they do not inflate the total
                if (!row.placeholder()) {
                    totalRows++;
                }
                if (row.matches(normalizedFilter) && !isHiddenByToggle(row, showUnimplemented)) {
                    visibleRows.add(row);
                    if (!row.placeholder()) {
                        shownRows++;
                    }
                }
            }
            panelVisibleRows.put(quirkPanel, visibleRows);
            int columns = calculateColumnsFor(quirkPanel);
            panelLastCalculatedCols.put(quirkPanel, columns);
            relayoutPanel(quirkPanel, visibleRows, columns);
        }

        filterBar.setMatchCount(shownRows, totalRows);
        revalidate();
        repaint();
    }

    /**
     * Updates the font style and color of a quirk component based on its selection state. Selected quirks are
     * highlighted in {@link UIUtil#uiQuirksColor()}, the same colour the lobby uses to mark a unit that has quirks.
     * A quirk MegaMek ignores sits at the disabled foreground colour when it is not set, matching the grayed-out
     * rows on the pilot tab, and still highlights when it is set - a quirk the unit actually has must stay visible
     * even though the engine will not act on it.
     */
    private void updateQuirkFontStyle(DialogOptionComponentYPanel optionComp, boolean selected) {
        // Read per call rather than cached in a constant, so the colour follows the current look and feel
        Color selectedColor = UIUtil.uiQuirksColor();
        Color unselectedColor = noGameEffectComps.contains(optionComp) ? disabledForeground() : null;
        for (Component child : optionComp.getComponents()) {
            if (OptionRowLayout.STATUS_MARKER_NAME.equals(child.getName())) {
                continue;
            }
            if (child.getFont() != null) {
                child.setForeground(selected ? selectedColor : unselectedColor);
            }
        }
        optionComp.invalidate();
        optionComp.repaint();
    }

    /** @return the look and feel's disabled text colour, used to gray out quirks with no game effect */
    private static Color disabledForeground() {
        return UIManager.getColor("Label.disabledForeground");
    }

    /** @return {@code true} if the quirk is switched on, or holds a value other than its default */
    private boolean isQuirkSet(DialogOptionComponentYPanel optionComp, IOption option) {
        if (option.getType() == IOption.BOOLEAN) {
            return option.booleanValue();
        }
        return !optionComp.isDefaultValue();
    }

    /**
     * Calculates the maximum width of all quirks for responsive layout.
     */
    private void calculateGlobalMaxWidth(List<DialogOptionComponentYPanel> allQuirks) {
        globalMaxItemWidth = 0;
        for (DialogOptionComponentYPanel optionComp : allQuirks) {
            Dimension originalSize = originalPreferredSizes.get(optionComp);
            globalMaxItemWidth = Math.max(globalMaxItemWidth,
                  (originalSize != null) ? originalSize.width : optionComp.getPreferredSize().width);
        }
        if (globalMaxItemWidth <= 0) {
            globalMaxItemWidth = FALLBACK_ITEM_WIDTH;
        }
    }

    /**
     * @return how many columns the panel should use: one for the chassis columns, and as many as fit for the weapon
     *       panels
     */
    private int calculateColumnsFor(JPanel quirkPanel) {
        if (!responsivePanels.contains(quirkPanel)) {
            return 1;
        }
        return OptionRowLayout.calculateColumns(calculateAvailableWidthInPanel(quirkPanel), globalMaxItemWidth);
    }

    /**
     * Calculates the usable width inside a panel's content area.
     */
    private int calculateAvailableWidthInPanel(JPanel quirkPanel) {
        Container scrollPaneParent = quirkPanel.getParent();
        if (scrollPaneParent instanceof JViewport viewport) {
            int viewportWidth = viewport.getWidth();
            Insets panelInsets = quirkPanel.getInsets();
            return viewportWidth - panelInsets.left - panelInsets.right;
        }
        return quirkPanel.getWidth() - quirkPanel.getInsets().left - quirkPanel.getInsets().right;
    }

    /**
     * Checks if relayout is needed based on width changes and triggers it for each responsive panel.
     */
    private void triggerRelayoutCheck() {
        if (!isShowing() || responsivePanels.isEmpty() || (globalMaxItemWidth <= 0)) {
            return;
        }

        for (JPanel quirkPanel : responsivePanels) {
            List<QuirkRow> visibleRows = panelVisibleRows.get(quirkPanel);
            if (visibleRows == null) {
                continue;
            }

            int currentColumns = calculateColumnsFor(quirkPanel);
            Integer lastColumns = panelLastCalculatedCols.get(quirkPanel);

            // Only relayout if the number of columns needs to change
            if ((lastColumns == null) || (currentColumns != lastColumns)) {
                panelLastCalculatedCols.put(quirkPanel, currentColumns);
                relayoutPanel(quirkPanel, visibleRows, currentColumns);
            }
        }
    }

    /** Arranges the visible rows of one panel into a grid, reusing the existing row components. */
    private void relayoutPanel(JPanel quirkPanel, List<QuirkRow> visibleRows, int columns) {
        OptionRowLayout.relayout(quirkPanel, visibleRows.stream().map(QuirkRow::component).toList(), columns);
    }

    @Override
    public void optionClicked(DialogOptionComponentYPanel optionComp, IOption option, boolean state) {
        if (option.getType() == IOption.BOOLEAN) {
            option.setValue(state);
            updateQuirkFontStyle(optionComp, state);
        } else {
            // For non-boolean options (INTEGER, STRING, etc.), don't set value here.
            // The actual value is saved in setQuirks() using optionComp.getValue().
            // Just update the font style based on whether value differs from default.
            Object value = optionComp.getValue();
            boolean isSet = (value != null) && !value.equals(option.getDefault());
            updateQuirkFontStyle(optionComp, isSet);
        }
        if (parent != null) {
            parent.optionClicked(optionComp, option, state);
        }
    }

    @Override
    public void optionSwitched(DialogOptionComponentYPanel optionComp, IOption option, int value) {
        if (parent != null) {
            parent.optionSwitched(optionComp, option, value);
        }
    }

    /** Writes every quirk row's current value back onto the entity and its weapon mounts. */
    public void setQuirks() {
        IOption option;
        for (final DialogOptionComponentYPanel optionComp : quirkComps) {
            option = optionComp.getOption();
            if (Messages.getString("CustomMekDialog.None").equals(optionComp.getValue())) {
                entity.getQuirks().getOption(option.getName()).setValue("None");
            } else if (option.getName().equals(OptionsConstants.QUIRK_POS_INTERNAL_BOMB)) {
                // Need to set the quirk, and only then force re-computing bomb bay space for
                // Aero-derived units
                entity.getQuirks().getOption(option.getName()).setValue(optionComp.getValue());
                if (entity.isAero()) {
                    ((Aero) entity).autoSetMaxBombPoints();
                }
            } else {
                entity.getQuirks().getOption(option.getName()).setValue(optionComp.getValue());
            }
        }

        // Recalculate tech advancement to pick up any quirk changes that affect it
        // (e.g., Obsolete quirk adds an extinction date)
        entity.recalculateTechAdvancement();

        // now for weapon quirks
        for (Integer equipmentNumber : weaponQuirkComps.keySet()) {
            Mounted<?> mounted = entity.getEquipment(equipmentNumber);
            for (final DialogOptionComponentYPanel optionComp : weaponQuirkComps.get(equipmentNumber)) {
                option = optionComp.getOption();
                if (Messages.getString("CustomMekDialog.None").equals(optionComp.getValue())) {
                    mounted.getQuirks().getOption(option.getName()).setValue("None");
                } else {
                    mounted.getQuirks().getOption(option.getName()).setValue(optionComp.getValue());
                }
            }
        }
    }
}
