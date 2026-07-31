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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListModel;

import megamek.client.ui.settings.SettingsHeaderPanel;
import megamek.client.ui.settings.SettingsNavigationText;
import megamek.client.ui.settings.SettingsPagePanel;
import megamek.client.ui.settings.SettingsPane;
import megamek.client.ui.settings.SettingsRoute;
import megamek.client.ui.settings.SettingsTextProvider;

/** Settings-tree presentation for the shared MegaMek client preferences. */
public class CommonSettingsPane extends JPanel {
    private static final SettingsTextProvider TEXT = SettingsTextProvider.megaMek();

    private final SettingsPane settingsPane;

    public CommonSettingsPane(List<OptionPage> pages) {
        super(new BorderLayout());
        setName("commonSettingsPane");

        List<SettingsRoute> routes = new ArrayList<>();
        Map<String, Supplier<Component>> pageFactories = new LinkedHashMap<>();
        for (OptionPage definition : pages) {
            String routeId = "clientOptions." + definition.id();
            SettingsRoute route = new SettingsRoute(routeId, List.of(definition.displayName()),
                  collectSearchAliases(definition.content()), true);
            SettingsPagePanel page = SettingsPagePanel.builder(definition.id(), TEXT,
                        "CommonSettingsDialog.title", null)
                  .header(new SettingsHeaderPanel(definition.id(), definition.displayName(), null))
                  .component(definition.content())
                  .showDetailsPanel(true)
                  .build();
            routes.add(route);
            pageFactories.put(routeId, () -> page);
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

    public void setFilterText(String filterText) {
        settingsPane.setFilterText(filterText);
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

    /** One client-options navigation route and its existing settings panel. */
    public record OptionPage(String id, String displayName, JComponent content) {
        public OptionPage {
            Objects.requireNonNull(id);
            Objects.requireNonNull(displayName);
            Objects.requireNonNull(content);
        }
    }
}