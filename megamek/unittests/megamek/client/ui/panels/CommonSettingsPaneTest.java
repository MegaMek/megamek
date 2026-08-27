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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import megamek.client.ui.Messages;
import megamek.client.ui.settings.CollapsibleSectionPanel;
import megamek.client.ui.settings.SettingsBadge;
import megamek.client.ui.settings.SettingsCheckBox;
import megamek.client.ui.settings.SettingsNavigationPanel;
import megamek.client.ui.settings.SettingsPagePanel;
import megamek.client.ui.settings.SettingsTextProvider;
import megamek.client.ui.util.UIUtil;
import org.junit.jupiter.api.Test;

class CommonSettingsPaneTest {

    @Test
    void searchIndexesNestedLabelsAndTooltips() throws Exception {
        runOnEdt(() -> {
            JPanel main = new JPanel();
            main.add(new JLabel("Display scale"));
            JPanel audio = new JPanel();
            JCheckBox mute = new JCheckBox("Mute notifications");
            mute.setToolTipText("Silence chat alerts");
            audio.add(mute);
            CommonSettingsPane pane = pane(main, audio);

            pane.setFilterText("chat alerts");

            assertEquals("Audio", selectedTreeLabel(pane));
        });
    }

    @Test
    void searchIndexesListEntries() throws Exception {
        runOnEdt(() -> {
            JPanel main = new JPanel();
            main.add(new JList<>(new String[] { "General option", "Experimental pathfinder" }));
            CommonSettingsPane pane = pane(main, new JPanel());

            pane.setFilterText("pathfinder");

            assertEquals("Main", selectedTreeLabel(pane));
        });
    }

    @Test
    void nestedPageUsesCollapsedSectionsAndStandardSize() throws Exception {
        runOnEdt(() -> {
            JCheckBox detailed = new JCheckBox("Detailed option");
            detailed.setToolTipText("Contextual details");
            CommonSettingsPane pane = new CommonSettingsPane(List.of(
                  new CommonSettingsPane.OptionPage("gameBoard.general", List.of("Game Board", "General"),
                        "GameBoardGeneral", List.of(
                              new CommonSettingsPane.OptionSection("display", "Display", "Board display", detailed,
                            false),
                              new CommonSettingsPane.OptionSection("controls", "Controls", "Board controls",
                                new JPanel(), true)))));

            List<CollapsibleSectionPanel> sections = findSections(pane);
            assertEquals(2, sections.size());
            assertFalse(sections.get(0).isExpanded());
            assertFalse(sections.get(1).isExpanded());
            assertTrue(findComponent(pane, SettingsPagePanel.class).shouldShowDetailsPanel());
            assertEquals("Display", sectionAccessibleName(sections.get(0)));
            SettingsBadge advancedBadge = CommonSettingsPane.legendEntries().stream()
                .filter(badge -> badge.codePoint() == 0xE8B8)
                .findFirst()
                .orElseThrow();
            assertTrue(sectionAccessibleName(sections.get(1)).contains(advancedBadge.toHtml()));
            assertTrue(pane.getPreferredSize().width >= UIUtil.scaleForGUI(
                    SettingsNavigationPanel.DEFAULT_NAVIGATION_WIDTH + SettingsPagePanel.DEFAULT_MAXIMUM_PAGE_WIDTH));
            assertTrue(pane.getPreferredSize().height >= UIUtil.scaleForGUI(800));
        });
    }

    @Test
    void usesStandardWidthForBehaviorPageAndPaneFloor() throws Exception {
        runOnEdt(() -> {
            CommonSettingsPane behaviorPane = new CommonSettingsPane(List.of(
                  new CommonSettingsPane.OptionPage("main.behavior", List.of("Main", "Behavior"),
                        "Behavior", List.of(new CommonSettingsPane.OptionSection(
                              "behavior", "Behavior", "Behavior settings", new JPanel(), false)))));
            SettingsPagePanel behaviorPage = findComponent(behaviorPane, SettingsPagePanel.class);
            CollapsibleSectionPanel behaviorSection = findSections(behaviorPage).getFirst();
            int standardSectionWidth = UIUtil.scaleForGUI(SettingsPagePanel.DEFAULT_SECTION_STACK_WIDTH);
            int standardPageWidth = UIUtil.scaleForGUI(SettingsPagePanel.DEFAULT_MAXIMUM_PAGE_WIDTH);
            int standardPaneWidth = UIUtil.scaleForGUI(SettingsNavigationPanel.DEFAULT_NAVIGATION_WIDTH)
                  + standardPageWidth;
            int widenedPaneWidth = UIUtil.scaleForGUI(SettingsNavigationPanel.DEFAULT_NAVIGATION_WIDTH)
                  + UIUtil.scaleForGUI(SettingsPagePanel.DEFAULT_MAXIMUM_PAGE_WIDTH + 100);

            assertEquals(standardSectionWidth, behaviorSection.getParent().getPreferredSize().width);
            assertEquals(standardSectionWidth, behaviorPage.getPreferredSize().width);
            assertEquals(standardPaneWidth, behaviorPane.getPreferredSize().width);
            assertTrue(behaviorPane.getPreferredSize().width < widenedPaneWidth);
        });
    }

    @Test
    void legendExplainsImportantAndAdvancedMarkers() {
        List<SettingsBadge> entries = CommonSettingsPane.legendEntries();

        assertEquals(List.of(0xE002, 0xE8B8), entries.stream().map(SettingsBadge::codePoint).toList());
        assertEquals(Messages.getString("CommonSettingsDialog.legend.important"), entries.get(0).description());
        assertEquals(Messages.getString("CommonSettingsDialog.legend.advanced"), entries.get(1).description());
    }

    @Test
    void settingsHelpProviderMakesOptionDetailsEligible() throws Exception {
        runOnEdt(() -> {
            SettingsBadge importantBadge = CommonSettingsPane.legendEntries().getFirst();
            SettingsCheckBox showIpAddresses = new SettingsCheckBox(SettingsTextProvider.megaMek(),
                  "CommonSettingsDialog.showIPAddressesInChat", List.of(importantBadge));
            JPanel main = new JPanel();
            main.add(showIpAddresses);
            CommonSettingsPane pane = pane(main, new JPanel());

            assertTrue(findComponent(pane, SettingsPagePanel.class).shouldShowDetailsPanel());
            assertEquals(Messages.getString("CommonSettingsDialog.showIPAddressesInChat.tooltip"),
                showIpAddresses.getSettingsHelpText());
        });
    }

    @Test
    void nestedSearchFindsControlTextAndKeepsParentPath() throws Exception {
        runOnEdt(() -> {
            JPanel content = new JPanel();
            content.add(new JLabel("Experimental pathfinder"));
            CommonSettingsPane pane = new CommonSettingsPane(List.of(
                  new CommonSettingsPane.OptionPage("gameBoard.general", List.of("Game Board", "General"),
                        "GameBoardGeneral", List.of(new CommonSettingsPane.OptionSection(
                              "pathfinder", "Pathfinder", "Movement paths", content, false)))));

            pane.setFilterText("experimental pathfinder");

            JTree tree = findComponent(pane, "settingsNavigationTree", JTree.class);
            assertEquals(2, tree.getRowCount());
            assertEquals("General", tree.getPathForRow(1).getLastPathComponent().toString());
        });
    }

    @Test
    void factionLogoMappingsResolveToSharedAssets() {
        File factionsDir = factionAssetsDirectory();

        assertTrue(factionsDir.isDirectory(), "Faction logo directory does not exist: " + factionsDir);
        assertFalse(CommonSettingsPane.factionLogos().isEmpty());
        CommonSettingsPane.factionLogos().forEach((page, logo) ->
              assertTrue(new File(factionsDir, logo).isFile(), page + " logo does not exist: " + logo));
        assertTrue(new File(factionsDir, "logo_star_league.png").isFile());
    }

    private static File factionAssetsDirectory() {
        List<File> candidates = List.of(
            new File("../../mm-data/data/images/universe/factions"),
            new File("data/images/universe/factions"),
            new File("megamek/data/images/universe/factions"));
        return candidates.stream()
            .filter(File::isDirectory)
            .findFirst()
            .orElse(candidates.getFirst());
    }

    private static CommonSettingsPane pane(JPanel main, JPanel audio) {
        return new CommonSettingsPane(List.of(
            new CommonSettingsPane.OptionPage("main", "Main", main),
            new CommonSettingsPane.OptionPage("audio", "Audio", audio)));
    }

    private static String selectedTreeLabel(Container root) {
        JTree tree = findComponent(root, "settingsNavigationTree", JTree.class);
        assertEquals(1, tree.getRowCount());
        TreePath path = tree.getPathForRow(0);
        return path.getLastPathComponent().toString();
    }

    private static <T extends Component> T findComponent(Container root, String name, Class<T> type) {
        return findComponentOptional(root, name, type)
            .orElseThrow(() -> new AssertionError("No " + type.getSimpleName() + " named " + name));
    }

    private static <T extends Component> T findComponent(Container root, Class<T> type) {
        if (type.isInstance(root)) {
            return type.cast(root);
        }
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                Optional<T> result = findComponentOptional(container, type);
                if (result.isPresent()) {
                    return result.get();
                }
            }
        }
        throw new AssertionError("No " + type.getSimpleName());
    }

    private static <T extends Component> Optional<T> findComponentOptional(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                return Optional.of(type.cast(child));
            }
            if (child instanceof Container container) {
                Optional<T> result = findComponentOptional(container, type);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty();
    }

    private static <T extends Component> Optional<T> findComponentOptional(Container root, String name,
        Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && name.equals(child.getName())) {
                return Optional.of(type.cast(child));
            }
            if (child instanceof Container container) {
                Optional<T> result = findComponentOptional(container, name, type);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty();
    }

    private static List<CollapsibleSectionPanel> findSections(Container root) {
        java.util.ArrayList<CollapsibleSectionPanel> sections = new java.util.ArrayList<>();
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

    private static String sectionAccessibleName(CollapsibleSectionPanel section) {
        return section.getComponent(0).getAccessibleContext().getAccessibleName();
    }

    private static void runOnEdt(Runnable test) throws Exception {
        try {
            SwingUtilities.invokeAndWait(test);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof Error error) {
                throw error;
            }
            if (exception.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw exception;
        }
    }
}
