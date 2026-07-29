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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.ListResourceBundle;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.util.UIUtil;
import org.junit.jupiter.api.Test;

class SettingsPagePanelTest {
    private static final SettingsTextProvider TEXT = SettingsTextProvider.fromResourceBundle(new ListResourceBundle() {
        @Override
        protected Object[][] getContents() {
            return new Object[][] {
                  { "header", "Header" },
                  { "apostropheHeader", "Pilot's {notes}" }
            };
        }
    });

    @Test
    void sectionSearchTextConcatenatesLiteralTitlesAndSummaries() {
        SettingsPagePanel page = SettingsPagePanel.builder("Test", TEXT, "header", null)
              .literalSection("Alpha Section", "alpha summary", new JLabel())
              .literalSection("Beta Section", null, new JLabel())
              .build();

        String searchText = page.getSectionSearchText();

        assertTrue(searchText.contains("Alpha Section"), searchText);
        assertTrue(searchText.contains("alpha summary"), searchText);
        assertTrue(searchText.contains("Beta Section"), searchText);
        assertFalse(searchText.contains("null"), searchText);
    }

    @Test
    void sectionSearchTextIsEmptyWithoutSections() {
        SettingsPagePanel page = SettingsPagePanel.builder("Test", TEXT, "header", null).build();

        assertEquals("", page.getSectionSearchText());
    }

    @Test
    void headerTextIsNotFormattedWithoutArguments() {
        SettingsPagePanel page = SettingsPagePanel.builder("Test", TEXT, "apostropheHeader", null).build();

        assertTrue(containsLabelText(page, "Pilot's {notes}"));
    }

    @Test
    void expandSectionsMatchingRevealsOnlyMatchingCollapsedSection() {
        SettingsPagePanel page = SettingsPagePanel.builder("Test", TEXT, "header", null)
              .sectionsExpandedByDefault(false)
              .literalSection("Alpha Section", "alpha summary", new JLabel())
              .literalSection("Beta Section", "beta summary", new JLabel())
              .build();
        List<CollapsibleSectionPanel> sections = findSections(page);

        boolean matched = page.expandSectionsMatching(text -> text.contains("Beta Section"));

        assertTrue(matched);
        assertEquals(2, sections.size());
        assertFalse(sections.get(0).isExpanded());
        assertTrue(sections.get(1).isExpanded());
    }

    @Test
    void wideSectionRetainsItsNaturalWidthAndVisibleBounds() {
        JLabel wideContent = new JLabel();
        wideContent.setPreferredSize(new Dimension(2_000, 20));
        SettingsPagePanel page = SettingsPagePanel.builder("Test", TEXT, "header", null)
              .sectionsExpandedByDefault(true)
              .maximumPageWidth(100)
              .literalSection("Wide Section", null, wideContent)
              .build();
        page.setSize(page.getPreferredSize());

        layoutTree(page);

        Component pageBody = page.getComponent(0);
        CollapsibleSectionPanel section = findSections(page).get(0);
        assertTrue(pageBody.getWidth() > page.getWidth());
        assertTrue(section.getWidth() > 0);
        assertTrue(section.getHeight() > 0);
        assertTrue(section.isShowing() || section.isVisible());
    }

    @Test
    void sectionStackUsesScaledMinimumWidth() {
        SettingsPagePanel page = SettingsPagePanel.builder("Test", TEXT, "header", null)
              .sectionStackWidth(700)
              .literalSection("Section", null, new JLabel())
              .build();
        CollapsibleSectionPanel section = findSections(page).get(0);

        assertEquals(UIUtil.scaleForGUI(700), section.getParent().getPreferredSize().width);
    }

    @Test
    void sectionControlsHaveScaledTopSpacing() {
        SettingsPagePanel page = SettingsPagePanel.builder("Test", TEXT, "header", null)
              .literalSection("Section", null, new JLabel())
              .build();

        JPanel controls = findNamedPanel(page, "settingsSectionControls");

        assertEquals(UIUtil.scaleForGUI(8), controls.getBorder().getBorderInsets(controls).top);
        assertEquals(0, controls.getBorder().getBorderInsets(controls).bottom);
        assertEquals(UIUtil.scaleForGUI(5), ((FlowLayout) controls.getLayout()).getHgap());
    }

    @Test
    void headerUsesScaledComponentPadding() {
        SettingsHeaderPanel header = new SettingsHeaderPanel("Test", "Header", null);

        GridBagLayout layout = (GridBagLayout) header.getLayout();
        GridBagConstraints constraints = layout.getConstraints(header.getComponent(0));
        Insets insets = constraints.insets;
        assertEquals(UIUtil.scaleForGUI(5), insets.top);
        assertEquals(UIUtil.scaleForGUI(5), insets.left);
        assertEquals(UIUtil.scaleForGUI(5), insets.bottom);
        assertEquals(UIUtil.scaleForGUI(5), insets.right);
    }

    private static void layoutTree(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) {
            if (child instanceof Container container) {
                layoutTree(container);
            }
        }
    }

    private static boolean containsLabelText(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label && label.getText().contains(text)) {
                return true;
            }
            if (child instanceof Container container && containsLabelText(container, text)) {
                return true;
            }
        }
        return false;
    }

    private static List<CollapsibleSectionPanel> findSections(Container root) {
        List<CollapsibleSectionPanel> sections = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (child instanceof CollapsibleSectionPanel section) {
                sections.add(section);
            }
            if (child instanceof Container container) {
                sections.addAll(findSections(container));
            }
        }
        return sections;
    }

    private static JPanel findNamedPanel(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (child instanceof JPanel panel && name.equals(panel.getName())) {
                return panel;
            }
            if (child instanceof Container container) {
                JPanel result = findNamedPanelOrNull(container, name);
                if (result != null) {
                    return result;
                }
            }
        }
        throw new AssertionError("No panel named " + name);
    }

    private static JPanel findNamedPanelOrNull(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (child instanceof JPanel panel && name.equals(panel.getName())) {
                return panel;
            }
            if (child instanceof Container container) {
                JPanel result = findNamedPanelOrNull(container, name);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
