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
package megamek.client.ui.settings;

import static megamek.client.ui.util.FontHandler.symbolIcon;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.ui.FastJScrollPane;

/** Searchable tree navigation for a settings pane. */
public class SettingsNavigationPanel extends JPanel {
    public static final int DEFAULT_NAVIGATION_WIDTH = 240;

    private static final int CONTENT_GAP = 6;
    private static final int CONTROL_GAP = 4;

    private final List<SettingsRoute> routes;
    private final Consumer<SettingsRoute> routeSelectionListener;
    private final SettingsNavigationText text;
    private final JTextField filterField = new JTextField();
    private final JLabel filterStatusLabel = new JLabel();
    private final JTree navigationTree = new JTree();
    private SettingsRoute currentRoute;
    private boolean syncingSelection;
    private Runnable searchIndexInitializer;
    private Consumer<String> filterChangeListener = filter -> { };

    public SettingsNavigationPanel(List<SettingsRoute> routes, Consumer<SettingsRoute> routeSelectionListener,
          SettingsNavigationText text) {
        super(new BorderLayout(0, UIUtil.scaleForGUI(CONTROL_GAP)));
        this.routes = List.copyOf(routes);
        this.routeSelectionListener = Objects.requireNonNull(routeSelectionListener);
        this.text = Objects.requireNonNull(text);

        setName("settingsNavigationPanel");
        Border frameBorder = UIManager.getBorder("ScrollPane.border");
        if (frameBorder == null) {
            frameBorder = BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"),
                  UIUtil.scaleForGUI(1));
        }
        int contentGap = UIUtil.scaleForGUI(CONTENT_GAP);
        setBorder(BorderFactory.createCompoundBorder(frameBorder,
              BorderFactory.createEmptyBorder(contentGap, contentGap, contentGap, contentGap)));
        setPreferredSize(new Dimension(UIUtil.scaleForGUI(DEFAULT_NAVIGATION_WIDTH), 1));

        configureFilterField();
        configureNavigationTree();

        JScrollPane navigationScrollPane = new FastJScrollPane(navigationTree,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navigationScrollPane.setName("settingsNavigationScrollPane");

        int controlGap = UIUtil.scaleForGUI(CONTROL_GAP);
        JPanel searchRow = new JPanel(new BorderLayout(controlGap, 0));
        searchRow.setName("settingsSearchRow");
        searchRow.add(filterField, BorderLayout.CENTER);
        if (routes.stream().anyMatch(route -> !route.isTopLevelRoute())) {
            searchRow.add(createTreeControls(), BorderLayout.EAST);
        }

        JPanel filterPanel = new JPanel(new BorderLayout(0, controlGap));
        filterPanel.setName("settingsFilterPanel");
        filterPanel.add(searchRow, BorderLayout.NORTH);
        filterPanel.add(filterStatusLabel, BorderLayout.SOUTH);
        add(filterPanel, BorderLayout.NORTH);
        add(navigationScrollPane, BorderLayout.CENTER);
        buildNavigationTree("");
    }

    private void configureFilterField() {
        filterField.setName("txtSettingsFilter");
        filterField.putClientProperty("JTextField.placeholderText", text.filterPlaceholder());
        filterField.setToolTipText(text.filterTooltip());
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                filterNavigation();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                filterNavigation();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                filterNavigation();
            }
        });
        filterStatusLabel.setName("lblSettingsFilterStatus");
        filterStatusLabel.setHorizontalAlignment(SwingConstants.LEADING);
        filterStatusLabel.setVisible(false);
    }

    private void configureNavigationTree() {
        navigationTree.setName("settingsNavigationTree");
        navigationTree.setRootVisible(false);
        navigationTree.setShowsRootHandles(true);
        navigationTree.addTreeSelectionListener(event -> notifySelectedRoute());
        navigationTree.setCellRenderer(new NavigationTreeCellRenderer());
    }

    private JPanel createTreeControls() {
        JButton expandAll = createTreeControlButton("btnSettingsExpandAll", 0xE5D7,
              UIUtil.scaleForGUI(18), text.expandAllTooltip());
        expandAll.addActionListener(event -> setAllNodesExpanded(true));
        JButton collapseAll = createTreeControlButton("btnSettingsCollapseAll", 0xE5D6,
              UIUtil.scaleForGUI(20), text.collapseAllTooltip());
        collapseAll.addActionListener(event -> setAllNodesExpanded(false));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, UIUtil.scaleForGUI(2), 0));
        controls.setName("settingsNavigationControls");
        controls.setOpaque(false);
        controls.add(expandAll);
        controls.add(collapseAll);
        return controls;
    }

    private JButton createTreeControlButton(String name, int codePoint, int iconSize, String tooltip) {
        JButton button = new JButton();
        button.setName(name);
        button.setFocusable(false);
        button.setIcon(symbolIcon(codePoint, iconSize, button.getForeground()));
        button.setToolTipText(tooltip);
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.putClientProperty("JComponent.sizeVariant", "small");
        int side = UIUtil.scaleForGUI(28);
        button.setPreferredSize(new Dimension(side, side));
        return button;
    }

    private void setAllNodesExpanded(boolean expanded) {
        if (expanded) {
            for (int row = 0; row < navigationTree.getRowCount(); row++) {
                navigationTree.expandRow(row);
            }
        } else {
            for (int row = navigationTree.getRowCount() - 1; row >= 0; row--) {
                navigationTree.collapseRow(row);
            }
        }
    }

    public void selectRoute(SettingsRoute route) {
        currentRoute = Objects.requireNonNull(route);
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) navigationTree.getModel().getRoot();
        DefaultMutableTreeNode routeNode = findNavigationNode(root, route);
        if (routeNode == null) {
            return;
        }
        TreePath treePath = new TreePath(routeNode.getPath());
        if (!treePath.equals(navigationTree.getSelectionPath())) {
            syncingSelection = true;
            try {
                navigationTree.setSelectionPath(treePath);
            } finally {
                syncingSelection = false;
            }
        }
    }

    private void notifySelectedRoute() {
        if (syncingSelection) {
            return;
        }
        SettingsRoute selectedRoute = getSelectedRoute();
        if (selectedRoute != null) {
            currentRoute = selectedRoute;
            routeSelectionListener.accept(selectedRoute);
        }
    }

    public void setSearchIndexInitializer(@Nullable Runnable initializer) {
        searchIndexInitializer = initializer;
    }

    public void setFilterChangeListener(@Nullable Consumer<String> listener) {
        filterChangeListener = listener == null ? filter -> { } : listener;
    }

    public void refreshFilter() {
        buildNavigationTree(filterField.getText());
    }

    /** @return the normalized active search filter */
    public String getActiveFilter() {
        return SettingsRoute.normalizeSearchText(filterField.getText());
    }

    /** @return the search field's unmodified display text */
    public String getFilterText() {
        return filterField.getText();
    }

    /** Sets the search field's display text; {@code null} clears it and listeners receive its normalized form. */
    public void setFilterText(@Nullable String filterText) {
        filterField.setText(Objects.requireNonNullElse(filterText, ""));
    }

    public void focusSearchField() {
        filterField.requestFocusInWindow();
        filterField.selectAll();
    }

    private void filterNavigation() {
        if (searchIndexInitializer != null) {
            searchIndexInitializer.run();
        }
        buildNavigationTree(filterField.getText());
        filterChangeListener.accept(getActiveFilter());
    }

    private void buildNavigationTree(String filterText) {
        String normalizedFilter = SettingsRoute.normalizeSearchText(Objects.requireNonNullElse(filterText, ""));
        List<SettingsRoute> matchingRoutes = matchingRoutes(normalizedFilter);
                DefaultMutableTreeNode root = new DefaultMutableTreeNode(
              new NavigationTreeNode("SettingsRoot", "SettingsRoot", null));
        for (SettingsRoute route : matchingRoutes) {
            addNavigationTarget(root, route);
        }
        if (!normalizedFilter.isBlank() && matchingRoutes.isEmpty()) {
            root.add(new DefaultMutableTreeNode(new NavigationTreeNode("SettingsNoMatches", text.noMatches(), null)));
        }
        navigationTree.setModel(new DefaultTreeModel(root));
        for (int row = 0; row < navigationTree.getRowCount(); row++) {
            navigationTree.expandRow(row);
        }
        updateFilterStatus(normalizedFilter, matchingRoutes.size());
        if (currentRoute != null) {
            selectRoute(currentRoute);
            if (findNavigationNode(root, currentRoute) == null) {
                navigationTree.clearSelection();
            }
        }
    }

    private List<SettingsRoute> matchingRoutes(String normalizedFilter) {
        if (normalizedFilter.isBlank()) {
            return routes;
        }
        List<SettingsRoute> matching = new ArrayList<>();
        for (SettingsRoute route : routes) {
            if (route.matches(normalizedFilter)) {
                matching.add(route);
            }
        }
        return matching;
    }

    private void updateFilterStatus(String normalizedFilter, int matchCount) {
        if (normalizedFilter.isBlank()) {
            filterStatusLabel.setVisible(false);
            filterStatusLabel.setText("");
            return;
        }
        filterStatusLabel.setText(matchCount == 0
              ? text.noMatches()
              : String.format(Locale.ROOT, text.matchesFormat(), matchCount));
        filterStatusLabel.setVisible(true);
    }

    private void addNavigationTarget(DefaultMutableTreeNode root, SettingsRoute route) {
        DefaultMutableTreeNode currentNode = root;
        List<String> path = route.getPath();
        List<String> pathIds = route.getPathIds();
        for (int index = 0; index < path.size(); index++) {
            currentNode = getOrCreateNavigationNode(currentNode, pathIds.get(index), path.get(index));
            if (index == path.size() - 1) {
                ((NavigationTreeNode) currentNode.getUserObject()).route = route;
            }
        }
    }

    private DefaultMutableTreeNode getOrCreateNavigationNode(DefaultMutableTreeNode parent, String id, String label) {
        for (int index = 0; index < parent.getChildCount(); index++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(index);
            if (child.getUserObject() instanceof NavigationTreeNode node && node.id.equals(id)) {
                return child;
            }
        }
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(new NavigationTreeNode(id, label, null));
        parent.add(child);
        return child;
    }

    private @Nullable SettingsRoute getSelectedRoute() {
        TreePath selectionPath = navigationTree.getSelectionPath();
        if (selectionPath == null) {
            return null;
        }
        Object selected = selectionPath.getLastPathComponent();
        if (selected instanceof DefaultMutableTreeNode treeNode
              && treeNode.getUserObject() instanceof NavigationTreeNode navigationNode) {
            return navigationNode.route;
        }
        return null;
    }

    private @Nullable DefaultMutableTreeNode findNavigationNode(DefaultMutableTreeNode node, SettingsRoute route) {
        if (node.getUserObject() instanceof NavigationTreeNode treeNode && treeNode.route != null
              && treeNode.route.getId().equals(route.getId())) {
            return node;
        }
        for (int index = 0; index < node.getChildCount(); index++) {
            DefaultMutableTreeNode result = findNavigationNode((DefaultMutableTreeNode) node.getChildAt(index), route);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static class NavigationTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
              boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            Font baseFont = tree.getFont();
            if (baseFont != null) {
                boolean topLevel = value instanceof DefaultMutableTreeNode node && node.getLevel() == 1;
                int style = topLevel ? baseFont.getStyle() | Font.BOLD : baseFont.getStyle() & ~Font.BOLD;
                setFont(baseFont.deriveFont(style));
            }
            return this;
        }
    }

    private static class NavigationTreeNode {
        private final String id;
        private final String label;
        private SettingsRoute route;

        private NavigationTreeNode(String id, String label, @Nullable SettingsRoute route) {
            this.id = id;
            this.label = label;
            this.route = route;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
