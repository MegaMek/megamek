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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
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

import static megamek.client.ui.Messages.getString;
import static megamek.client.ui.util.FontHandler.symbolIcon;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.UIManager;

import megamek.client.ui.settings.SettingsBadge;
import megamek.client.ui.settings.SettingsHeaderPanel;
import megamek.client.ui.settings.SettingsNavigationPanel;
import megamek.client.ui.settings.SettingsNavigationText;
import megamek.client.ui.settings.SettingsPagePanel;
import megamek.client.ui.settings.SettingsPane;
import megamek.client.ui.settings.SettingsRoute;
import megamek.client.ui.settings.SettingsTextProvider;
import megamek.client.ui.util.UIUtil;

/** Settings-tree presentation for the shared MegaMek client preferences. */
public class CommonSettingsPane extends JPanel {
    private static final int START_HEIGHT = 800;
    private static final int DETAILS_ICON = 0xE88E;
    private static final int ADVANCED_ICON = 0xE8B8;
    private static final SettingsTextProvider TEXT = SettingsTextProvider.megaMek();
    private static final SettingsBadge DETAILS_BADGE = new SettingsBadge(DETAILS_ICON, null,
          getString("CommonSettingsDialog.legend.details"));
    private static final SettingsBadge ADVANCED_BADGE = new SettingsBadge(ADVANCED_ICON, null,
          getString("CommonSettingsDialog.legend.advanced"));

    private final SettingsPane settingsPane;

    public CommonSettingsPane(List<OptionPage> pages) {
        super(new BorderLayout());
        setName("commonSettingsPane");

        List<SettingsRoute> routes = new ArrayList<>();
        Map<String, Supplier<Component>> pageFactories = new LinkedHashMap<>();
        for (OptionPage page : pages) {
            List<String> searchAliases = new ArrayList<>();
            page.sections().forEach(section -> {
                searchAliases.add(section.title());
                searchAliases.add(section.summary());
                searchAliases.addAll(collectSearchAliases(section.content()));
            });
            routes.add(new SettingsRoute(page.id(), page.path(), searchAliases, true));
            pageFactories.put(page.id(), () -> createPage(page));
        }

        SettingsNavigationText navigationText = new SettingsNavigationText(
              getString("CommonSettingsDialog.Search"),
              getString("CommonSettingsDialog.SearchToolTip"),
              getString("CommonSettingsDialog.SearchNoMatches"),
              getString("CommonSettingsDialog.SearchMatches"),
              getString("SettingsPagePanel.expandAll.text"),
              getString("SettingsPagePanel.collapseAll.text"));
        settingsPane = new SettingsPane(routes, pageFactories, navigationText,
              getString("CommonSettingsDialog.optionDescriptionHint"));
        add(settingsPane, BorderLayout.CENTER);
    }

    private static SettingsPagePanel createPage(OptionPage page) {
        Icon icon = pageIcon(page.id());
        SettingsPagePanel.Builder builder = SettingsPagePanel.builder(page.pageName(), TEXT,
                    "CommonSettingsDialog.title", icon)
              .header(new SettingsHeaderPanel(page.pageName(), page.path().getLast(), icon))
              .showDetailsPanel(page.sections().stream().anyMatch(section -> containsHelpText(section.content())))
              .sectionsExpandedByDefault(page.sections().size() == 1)
              .standardContentWidth();

        for (OptionSection section : page.sections()) {
            List<String> aliases = collectSearchAliases(section.content());
            List<SettingsBadge> badges = new ArrayList<>();
            if (containsHelpText(section.content())) {
                badges.add(DETAILS_BADGE);
            }
            if (section.advanced()) {
                badges.add(ADVANCED_BADGE);
            }
            builder.literalSection(section.title(), section.summary(), section.content(), badges, aliases);
        }
        return builder.build();
    }

    public void setFilterText(String filterText) {
        settingsPane.setFilterText(filterText);
    }

    public static List<SettingsBadge> legendEntries() {
        return List.of(DETAILS_BADGE, ADVANCED_BADGE);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferred = super.getPreferredSize();
        int floorWidth = UIUtil.scaleForGUI(SettingsNavigationPanel.DEFAULT_NAVIGATION_WIDTH)
              + UIUtil.scaleForGUI(SettingsPagePanel.DEFAULT_MAXIMUM_PAGE_WIDTH);
        int floorHeight = UIUtil.scaleForGUI(START_HEIGHT);
        return new Dimension(Math.max(preferred.width, floorWidth), Math.max(preferred.height, floorHeight));
    }

    private static List<String> collectSearchAliases(Component root) {
        Set<String> aliases = new LinkedHashSet<>();
        collectSearchAliases(root, aliases);
        return List.copyOf(aliases);
    }

    private static void collectSearchAliases(Component component, Set<String> aliases) {
        if (component.getName() != null) {
            aliases.add(component.getName());
        }
        if (component instanceof JLabel label && label.getText() != null) {
            aliases.add(label.getText());
        } else if (component instanceof AbstractButton button && button.getText() != null) {
            aliases.add(button.getText());
        }
        if (component instanceof JComponent swingComponent && swingComponent.getToolTipText() != null) {
            aliases.add(swingComponent.getToolTipText());
        }
        if (component instanceof JList<?> list) {
            ListModel<?> model = list.getModel();
            for (int index = 0; index < model.getSize(); index++) {
                Object item = model.getElementAt(index);
                if (item != null) {
                    aliases.add(item.toString());
                }
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectSearchAliases(child, aliases);
            }
        }
    }

    private static boolean containsHelpText(Component component) {
        if (component instanceof JComponent swingComponent
              && swingComponent.getToolTipText() != null
              && !swingComponent.getToolTipText().isBlank()) {
            return true;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (containsHelpText(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Icon pageIcon(String id) {
        int codePoint;
        if (id.startsWith("main")) {
            codePoint = 0xE8B8;
        } else if (id.startsWith("audio")) {
            codePoint = 0xE050;
        } else if (id.startsWith("keyBinds")) {
            codePoint = 0xE312;
        } else if (id.startsWith("gameBoard") || id.startsWith("miniMap")) {
            codePoint = 0xE55B;
        } else if (id.startsWith("unitDisplay")) {
            codePoint = 0xE30D;
        } else if (id.startsWith("report")) {
            codePoint = 0xE873;
        } else if (id.startsWith("overlays")) {
            codePoint = 0xE3F4;
        } else if (id.startsWith("buttonOrder")) {
            codePoint = 0xE8D5;
        } else if (id.startsWith("autoDisplay")) {
            codePoint = 0xE8F4;
        } else if (id.startsWith("aiDisplay")) {
            codePoint = 0xE195;
        } else {
            codePoint = ADVANCED_ICON;
        }
        return symbolIcon(codePoint, UIUtil.scaleForGUI(48), UIManager.getColor("Label.foreground"));
    }

    /** Existing row content split into logical groups before being wrapped in settings sections. */
    public static final class SectionedContent extends JPanel {
        private final List<JComponent> groups;

        public SectionedContent(List<JComponent> groups) {
            setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            setOpaque(false);
            this.groups = List.copyOf(groups);
            this.groups.forEach(this::add);
        }

        public List<JComponent> groups() {
            return groups;
        }
    }

    public record OptionSection(String id, String title, String summary, JComponent content, boolean advanced) {
        public OptionSection {
            Objects.requireNonNull(id);
            Objects.requireNonNull(title);
            Objects.requireNonNull(summary);
            Objects.requireNonNull(content);
        }
    }

    public record OptionPage(String id, List<String> path, String pageName, List<OptionSection> sections) {
        public OptionPage(String id, String displayName, JComponent content) {
            this(id, List.of(displayName), id,
                  List.of(new OptionSection(id + ".settings", displayName,
                        getString("CommonSettingsDialog.section.settings.summary"), content, false)));
        }

        public OptionPage {
            Objects.requireNonNull(id);
            path = List.copyOf(path);
            Objects.requireNonNull(pageName);
            sections = List.copyOf(sections);
            if (path.isEmpty() || sections.isEmpty()) {
                throw new IllegalArgumentException("A client settings page requires a path and at least one section");
            }
        }
    }
}
