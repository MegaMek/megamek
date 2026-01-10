/*
 * Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2012-2025 The MegaMek Team. All Rights Reserved.
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.panels.DialogOptionComponentYPanel;
import megamek.common.equipment.Mounted;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.Quirks;
import megamek.common.options.WeaponQuirks;
import megamek.common.units.Aero;
import megamek.common.units.Entity;

/**
 * This class loads the default quirks list from the mmconf/cannonUnitQuirks.xml file.
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 2012-03-05
 */
public class QuirksPanel extends JPanel implements DialogOptionListener {
    @Serial
    private static final long serialVersionUID = -8360885055638738148L;
    private static final boolean SORT_QUIRKS_ALPHABETICALLY = true;

    private final Entity entity;
    private List<DialogOptionComponentYPanel> quirkComps;
    private final HashMap<Integer, ArrayList<DialogOptionComponentYPanel>> h_wpnQuirkComps = new HashMap<>();
    private final HashMap<Integer, WeaponQuirks> h_wpnQuirks;
    private final Quirks quirks;
    private final boolean editable;
    private final DialogOptionListener parent;

    private JPanel positiveQuirksPanel;
    private JPanel negativeQuirksPanel;
    private JPanel weaponQuirksPanel;

    // Responsive layout tracking
    private final Map<JPanel, List<DialogOptionComponentYPanel>> panelQuirksMap = new LinkedHashMap<>();
    private final Map<DialogOptionComponentYPanel, Dimension> originalPreferredSizes = new HashMap<>();
    private final Map<JPanel, Integer> panelLastCalculatedCols = new HashMap<>();
    private int globalMaxItemWidth = 0;

    public QuirksPanel(Entity entity, Quirks quirks, boolean editable, DialogOptionListener parent,
          HashMap<Integer, WeaponQuirks> h_wpnQuirks) {
        this.entity = entity;
        this.quirks = quirks;
        this.editable = editable;
        this.parent = parent;
        this.h_wpnQuirks = h_wpnQuirks;
        setLayout(new GridBagLayout());

        // Add resize listener for responsive column layout
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                triggerRelayoutCheck();
            }
        });

        refreshQuirks();
    }

    public void refreshQuirks() {
        // Cleanup
        removeAll();
        quirkComps = new ArrayList<>();
        for (Integer eqNum : h_wpnQuirks.keySet()) {
            h_wpnQuirkComps.put(eqNum, new ArrayList<>());
        }
        panelQuirksMap.clear();
        originalPreferredSizes.clear();
        panelLastCalculatedCols.clear();
        globalMaxItemWidth = 0;

        // Collect all quirks into lists for max width calculation
        List<DialogOptionComponentYPanel> allQuirks = new ArrayList<>();
        List<DialogOptionComponentYPanel> positiveQuirksList = new ArrayList<>();
        List<DialogOptionComponentYPanel> negativeQuirksList = new ArrayList<>();

        // Create positive and negative quirks panels
        positiveQuirksPanel = createTopAlignedPanel();
        positiveQuirksPanel.setBorder(BorderFactory.createTitledBorder("Chassis Quirks (Positive)"));
        negativeQuirksPanel = createTopAlignedPanel();
        negativeQuirksPanel.setBorder(BorderFactory.createTitledBorder("Chassis Quirks (Negative)"));

        // Collect chassis quirks and separate into positive/negative
        for (Enumeration<IOptionGroup> i = quirks.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            boolean isPositive = Quirks.POS_QUIRKS.equals(group.getKey());
            boolean isNegative = Quirks.NEG_QUIRKS.equals(group.getKey());

            if (isPositive || isNegative) {
                List<DialogOptionComponentYPanel> targetList = isPositive ? positiveQuirksList : negativeQuirksList;

                for (Enumeration<IOption> j = group.getSortedOptions(); j.hasMoreElements(); ) {
                    IOption option = j.nextElement();

                    if (null == option || Quirks.isQuirkDisallowed(option, entity)) {
                        continue;
                    }

                    addQuirk(option, editable, isPositive ? positiveQuirksPanel : negativeQuirksPanel, targetList);
                    allQuirks.add(targetList.get(targetList.size() - 1));
                }
            }
        }

        // Sort quirks alphabetically if enabled
        if (SORT_QUIRKS_ALPHABETICALLY) {
            positiveQuirksList.sort(Comparator.comparing(comp -> comp.getOption().getDisplayableName()));
            negativeQuirksList.sort(Comparator.comparing(comp -> comp.getOption().getDisplayableName()));
        }

        // Create main weapon quirks container panel with vertical layout for weapon groups
        JPanel weaponQuirksContainer = new JPanel(new GridBagLayout());
        weaponQuirksContainer.setBorder(BorderFactory.createTitledBorder("Weapon Quirks"));

        GridBagConstraints weaponGbc = new GridBagConstraints();
        weaponGbc.gridx = 0;
        weaponGbc.gridy = 0;
        weaponGbc.weightx = 1.0;
        weaponGbc.weighty = 0;
        weaponGbc.fill = GridBagConstraints.HORIZONTAL;
        weaponGbc.anchor = GridBagConstraints.NORTHWEST;
        weaponGbc.insets = new Insets(2, 2, 2, 2);

        // Process weapon quirks - each weapon gets its own titled panel
        Set<Integer> weaponKeys = h_wpnQuirks.keySet();
        for (int key : weaponKeys) {
            Mounted<?> m = entity.getEquipment(key);
            WeaponQuirks wpnQuirks = h_wpnQuirks.get(key);
            List<DialogOptionComponentYPanel> weaponQuirksList = new ArrayList<>();

            // Collect quirks for this weapon
            for (Enumeration<IOptionGroup> i = wpnQuirks.getGroups(); i.hasMoreElements(); ) {
                IOptionGroup group = i.nextElement();
                for (Enumeration<IOption> j = group.getSortedOptions(); j.hasMoreElements(); ) {
                    IOption option = j.nextElement();
                    if (WeaponQuirks.isQuirkDisallowed(option, entity, m.getType())) {
                        continue;
                    }

                    DialogOptionComponentYPanel optionComp = new DialogOptionComponentYPanel(this, option, editable);
                    originalPreferredSizes.put(optionComp, optionComp.getPreferredSize());
                    updateQuirkFontStyle(optionComp, option.booleanValue());
                    weaponQuirksList.add(optionComp);
                    h_wpnQuirkComps.get(key).add(optionComp);
                    allQuirks.add(optionComp);
                }
            }

            // Sort weapon quirks if enabled
            if (SORT_QUIRKS_ALPHABETICALLY && !weaponQuirksList.isEmpty()) {
                weaponQuirksList.sort(Comparator.comparing(comp -> comp.getOption().getDisplayableName()));
            }

            // Create a titled panel for this weapon if it has quirks
            if (!weaponQuirksList.isEmpty()) {
                JPanel weaponPanel = new JPanel(new GridBagLayout());
                String weaponTitle = m.getName() + " (" + entity.getLocationName(m.getLocation()) + ")";
                weaponPanel.setBorder(BorderFactory.createTitledBorder(weaponTitle));

                // Initially populate with single-column layout
                relayoutPanel(weaponPanel, weaponQuirksList, 1);

                weaponQuirksContainer.add(weaponPanel, weaponGbc);
                weaponGbc.gridy++;

                // Track this weapon panel for responsive layout
                panelQuirksMap.put(weaponPanel, weaponQuirksList);
            }
        }

        // Add vertical glue to push weapon groups to the top
        weaponGbc.weighty = 1.0;
        weaponGbc.fill = GridBagConstraints.BOTH;
        weaponQuirksContainer.add(new JPanel(), weaponGbc);

        // Calculate global max width across ALL quirks
        calculateGlobalMaxWidth(allQuirks);

        // Initially populate chassis quirk panels with single column (no responsive layout)
        relayoutPanel(positiveQuirksPanel, positiveQuirksList, 1);
        relayoutPanel(negativeQuirksPanel, negativeQuirksList, 1);

        // Note: Chassis quirk panels are NOT added to panelQuirksMap, so they stay single-column
        // Only weapon panels get responsive multi-column layout

        // Wrap panels in scroll panes
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

        // Add the split pane to the main panel
        setLayout(new GridBagLayout());
        add(mainSplitPane, GBC.eol().fill().weightX(1.0).weighty(1.0));

        validate();
        repaint();

        // Set divider locations and trigger layout after component is shown
        if (isShowing()) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                leftSplitPane.setDividerLocation(0.5);
                mainSplitPane.setDividerLocation(0.67);
                // Small delay to ensure dividers are positioned before responsive layout
                javax.swing.SwingUtilities.invokeLater(this::triggerRelayoutCheck);
            });
        } else {
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    leftSplitPane.setDividerLocation(0.5);
                    mainSplitPane.setDividerLocation(0.67);
                    // Small delay to ensure dividers are positioned before responsive layout
                    javax.swing.SwingUtilities.invokeLater(() -> triggerRelayoutCheck());
                    removeComponentListener(this);
                }
            });
        }
    }

    /**
     * Creates a panel configured for top-aligned content with multi-column responsive layout.
     */
    private JPanel createTopAlignedPanel() {
        return new JPanel(new GridBagLayout());
    }

    /**
     * Adds a quirk to the specified panel and tracks it for responsive layout.
     */
    private void addQuirk(IOption option, boolean editable, JPanel targetPanel, List<DialogOptionComponentYPanel> quirksList) {
        DialogOptionComponentYPanel optionComp = new DialogOptionComponentYPanel(this, option, editable);
        originalPreferredSizes.put(optionComp, optionComp.getPreferredSize());
        updateQuirkFontStyle(optionComp, option.booleanValue());
        quirksList.add(optionComp);
        quirkComps.add(optionComp);
    }

    /**
     * Updates the font style and color of a quirk component based on its selection state.
     * Selected quirks are highlighted in yellow.
     */
    private void updateQuirkFontStyle(DialogOptionComponentYPanel comp, boolean selected) {
        for (Component child : comp.getComponents()) {
            if (child.getFont() != null) {
                if (selected) {
                    child.setForeground(Color.YELLOW);
                } else {
                    child.setForeground(null);
                }
            }
        }
        comp.invalidate();
        comp.repaint();
    }

    /**
     * Calculates the maximum width of all quirks for responsive layout.
     */
    private void calculateGlobalMaxWidth(List<DialogOptionComponentYPanel> allQuirks) {
        globalMaxItemWidth = 0;
        for (DialogOptionComponentYPanel comp : allQuirks) {
            Dimension originalSize = originalPreferredSizes.get(comp);
            globalMaxItemWidth = Math.max(globalMaxItemWidth,
                (originalSize != null) ? originalSize.width : comp.getPreferredSize().width);
        }
        if (globalMaxItemWidth <= 0) {
            globalMaxItemWidth = 150; // Fallback width
        }
    }

    /**
     * Calculates the number of columns based on available width and max item width.
     */
    private int calculateNumberOfColumns(int containerWidth, int maxItemWidth) {
        if (containerWidth <= 0 || maxItemWidth <= 0) {
            return 1;
        }
        return Math.max(1, containerWidth / (maxItemWidth + 8));
    }

    /**
     * Gets the width of the visible area (viewport or panel itself).
     */
    private int getVisibleContainerWidth() {
        Container parent = getParent();
        if (parent instanceof JViewport) {
            return parent.getWidth();
        } else {
            return getWidth();
        }
    }

    /**
     * Calculates the usable width inside a panel's content area.
     */
    private int calculateAvailableWidthInPanel(JPanel panel) {
        Container scrollPaneParent = panel.getParent();
        if (scrollPaneParent instanceof JViewport) {
            JViewport viewport = (JViewport) scrollPaneParent;
            int viewportWidth = viewport.getWidth();
            Insets panelInsets = panel.getInsets();
            return viewportWidth - panelInsets.left - panelInsets.right;
        }
        return panel.getWidth() - panel.getInsets().left - panel.getInsets().right;
    }

    /**
     * Checks if relayout is needed based on width changes and triggers it for each panel.
     */
    private void triggerRelayoutCheck() {
        if (!isShowing() || panelQuirksMap.isEmpty() || globalMaxItemWidth <= 0) {
            return;
        }

        for (Map.Entry<JPanel, List<DialogOptionComponentYPanel>> entry : panelQuirksMap.entrySet()) {
            JPanel panel = entry.getKey();
            List<DialogOptionComponentYPanel> quirks = entry.getValue();

            int availableWidth = calculateAvailableWidthInPanel(panel);
            if (availableWidth <= 0) {
                continue;
            }

            int currentNumCols = calculateNumberOfColumns(availableWidth, globalMaxItemWidth);
            Integer lastNumCols = panelLastCalculatedCols.get(panel);

            // Only relayout if the number of columns needs to change
            if (lastNumCols == null || currentNumCols != lastNumCols) {
                panelLastCalculatedCols.put(panel, currentNumCols);
                relayoutPanel(panel, quirks, currentNumCols);
            }
        }
    }

    /**
     * Arranges quirks within a panel using responsive multi-column grid layout.
     */
    private void relayoutPanel(JPanel panel, List<DialogOptionComponentYPanel> quirks, int numCols) {
        panel.removeAll();
        if (!quirks.isEmpty() && (numCols > 0)) {
            panel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.weightx = 1.0;
            gbc.insets = new Insets(0, 2, 0, 2);

            int currentCol = 0;
            for (DialogOptionComponentYPanel quirk : quirks) {
                gbc.gridx = currentCol;
                panel.add(quirk, gbc);

                currentCol++;
                if (currentCol >= numCols) {
                    currentCol = 0;
                    gbc.gridy++;
                }
            }

            // Add vertical glue to push content to top
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = numCols;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            panel.add(new JPanel(), gbc);
        }
        panel.revalidate();
        panel.repaint();
    }

    @Override
    public void optionClicked(DialogOptionComponentYPanel comp, IOption option, boolean state) {
        if (option.getType() == IOption.BOOLEAN) {
            option.setValue(state);
            updateQuirkFontStyle(comp, state);
        } else {
            // For non-boolean options (INTEGER, STRING, etc.), don't set value here.
            // The actual value is saved in setQuirks() using comp.getValue().
            // Just update the font style based on whether value differs from default.
            Object value = comp.getValue();
            boolean isSet = value != null && !value.equals(option.getDefault());
            updateQuirkFontStyle(comp, isSet);
        }
        if (parent != null) {
            parent.optionClicked(comp, option, state);
        }
    }

    @Override
    public void optionSwitched(DialogOptionComponentYPanel comp, IOption option, int value) {
        if (parent != null) {
            parent.optionSwitched(comp, option, value);
        }
    }

    public void setQuirks() {
        IOption option;
        for (final DialogOptionComponentYPanel newVar : quirkComps) {
            option = newVar.getOption();
            if ((newVar.getValue() == Messages.getString("CustomMekDialog.None"))) {
                entity.getQuirks().getOption(option.getName()).setValue("None");
            } else if (option.getName().equals("internal_bomb")) {
                // Need to set the quirk, and only then force re-computing bomb bay space for
                // Aero-derived units
                entity.getQuirks().getOption(option.getName()).setValue(newVar.getValue());
                if (entity.isAero()) {
                    ((Aero) entity).autoSetMaxBombPoints();
                }
            } else {
                entity.getQuirks().getOption(option.getName()).setValue(newVar.getValue());
            }
        }

        // Recalculate tech advancement to pick up any quirk changes that affect it
        // (e.g., Obsolete quirk adds an extinction date)
        entity.recalculateTechAdvancement();

        // now for weapon quirks
        Set<Integer> set = h_wpnQuirkComps.keySet();
        for (Integer key : set) {
            Mounted<?> m = entity.getEquipment(key);
            ArrayList<DialogOptionComponentYPanel> wpnQuirkComps = h_wpnQuirkComps.get(key);
            for (final DialogOptionComponentYPanel newVar : wpnQuirkComps) {
                option = newVar.getOption();
                if ((newVar.getValue() == Messages.getString("CustomMekDialog.None"))) {
                    m.getQuirks().getOption(option.getName()).setValue("None");
                } else {
                    m.getQuirks().getOption(option.getName()).setValue(newVar.getValue());
                }
            }
        }
    }
}
