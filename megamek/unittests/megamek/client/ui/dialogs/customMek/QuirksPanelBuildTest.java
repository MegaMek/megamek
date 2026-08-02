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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.text.AbstractDocument;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.options.OptionsConstants;
import megamek.common.options.QuirkCatalog;
import megamek.common.options.QuirkPlaceholder;
import megamek.common.options.Quirks;
import megamek.common.options.WeaponQuirks;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Builds a real {@link QuirksPanel} over a real unit and drives its search box.
 *
 * <p>The pure-predicate tests in {@link QuirksPanelTest} cannot see resource keys, tooltip assembly or the
 * placeholder rows, so this test exercises the assembled panel: a missing message key, a {@code null} status or a
 * broken placeholder row shows up here as a failure rather than in play.</p>
 */
@DisplayName("Quirks panel construction")
class QuirksPanelBuildTest {

    private boolean originalShowUnimplemented;

    @BeforeAll
    static void requireDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "QuirksPanel needs a display");
    }

    @BeforeEach
    void rememberPreference() {
        originalShowUnimplemented = GUIPreferences.getInstance().getShowUnimplementedQuirks();
    }

    @AfterEach
    void restorePreference() {
        GUIPreferences.getInstance().setShowUnimplementedQuirks(originalShowUnimplemented);
    }

    private static QuirksPanel buildPanel() {
        Entity entity = new BipedMek();
        HashMap<Integer, WeaponQuirks> noWeaponQuirks = new HashMap<>();
        return new QuirksPanel(entity, new Quirks(), true, null, noWeaponQuirks);
    }

    /** The toggle is off by default, which hides the quirks MegaMek ignores; these tests need them listed. */
    private static QuirksPanel buildPanelShowingUnimplemented() {
        GUIPreferences.getInstance().setShowUnimplementedQuirks(true);
        return buildPanel();
    }

    /** Recursively collects every component of the given type below the container. */
    private static <T extends Component> List<T> findAll(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Container childContainer) {
                found.addAll(findAll(childContainer, type));
            }
        }
        return found;
    }

    private static JTextField filterFieldOf(QuirksPanel panel) {
        return findAll(panel, JTextField.class).stream()
              .filter(field -> "txtQuirkFilter".equals(field.getName()))
              .findFirst()
              .orElseThrow(() -> new AssertionError("The quirks panel has no filter field"));
    }

    private static JLabel matchCountLabelOf(QuirksPanel panel) {
        return findAll(panel, JLabel.class).stream()
              .filter(label -> "lblQuirkMatchCount".equals(label.getName()))
              .findFirst()
              .orElseThrow(() -> new AssertionError("The quirks panel has no match count label"));
    }

    private static JCheckBox unimplementedToggleOf(QuirksPanel panel) {
        return findAll(panel, JCheckBox.class).stream()
              .filter(box -> "chkShowUnimplementedQuirks".equals(box.getName()))
              .findFirst()
              .orElseThrow(() -> new AssertionError("The quirks panel has no unimplemented toggle"));
    }

    @Test
    @DisplayName("The panel builds with a filter bar and quirk rows")
    void panelBuildsWithFilterBar() {
        QuirksPanel panel = buildPanel();

        assertNotNull(filterFieldOf(panel));
        assertNotNull(unimplementedToggleOf(panel));
        assertFalse(matchCountLabelOf(panel).isVisible(), "The match count is hidden until a filter is typed");
        assertFalse(findAll(panel, JCheckBox.class).isEmpty(), "The panel should have quirk checkboxes");
    }

    @Test
    @DisplayName("Typing in the search box updates the match count")
    void filteringUpdatesTheMatchCount() {
        QuirksPanel panel = buildPanel();
        JLabel matchCountLabel = matchCountLabelOf(panel);

        filterFieldOf(panel).setText("targeting");

        assertTrue(matchCountLabel.isVisible(), "The match count shows once a filter is typed");
        assertFalse(matchCountLabel.getText().isBlank());
        assertFalse(matchCountLabel.getText().startsWith("!"),
              "Missing resource key for the quirk match count: " + matchCountLabel.getText());
    }

    @Test
    @DisplayName("Every quirk row resolves its status tooltip")
    void quirkRowsHaveResolvedTooltips() {
        QuirksPanel panel = buildPanel();

        int checkedTooltips = 0;
        for (JCheckBox checkBox : findAll(panel, JCheckBox.class)) {
            String tooltip = checkBox.getToolTipText();
            if (tooltip == null) {
                continue;
            }
            assertFalse(tooltip.contains("!QuirkImplementationStatus."),
                  "Unresolved status resource in tooltip: " + tooltip);
            assertFalse(tooltip.contains("!QuirkCatalog."),
                  "Unresolved catalog resource in tooltip: " + tooltip);
            checkedTooltips++;
        }
        assertTrue(checkedTooltips > 0, "No quirk row carried a tooltip");
    }

    @Test
    @DisplayName("A quirk MegaMek ignores is marked but stays editable")
    void unimplementedQuirksAreMarkedYetEditable() {
        QuirksPanel panel = buildPanelShowingUnimplemented();
        String marker = Messages.getString("CustomMekDialog.quirkNotImplementedSuffix").trim();

        List<JLabel> markedLabels = findAll(panel, JLabel.class).stream()
              .filter(label -> (label.getText() != null) && label.getText().contains(marker))
              .toList();
        assertFalse(markedLabels.isEmpty(),
              "No quirk was marked '" + marker + "'; the catalog should flag the quirks MegaMek ignores");

        // The point of the marker: it is informational only, the quirk can still be set on the unit
        long editableCheckboxes = findAll(panel, JCheckBox.class).stream()
              .filter(box -> !"chkShowUnimplementedQuirks".equals(box.getName()))
              .filter(JCheckBox::isEnabled)
              .count();
        assertTrue(editableCheckboxes > 0, "Marked quirks must remain editable");
    }

    @Test
    @DisplayName("A quirk MegaMek ignores is grayed and italic like the pilot tab's rows")
    void unimplementedQuirksAreGrayedOut() {
        QuirksPanel panel = buildPanelShowingUnimplemented();
        String marker = Messages.getString("CustomMekDialog.quirkNotImplementedSuffix").trim();
        Color disabledColor = UIManager.getColor("Label.disabledForeground");

        List<JLabel> markedLabels = findAll(panel, JLabel.class).stream()
              .filter(label -> (label.getText() != null) && label.getText().contains(marker))
              .toList();
        assertFalse(markedLabels.isEmpty(), "Expected at least one quirk marked '" + marker + "'");

        for (JLabel label : markedLabels) {
            assertTrue(label.getFont().isItalic(),
                  "'" + label.getText() + "' should be italic like the pilot tab's unimplemented rows");
            assertEquals(disabledColor, label.getForeground(),
                  "'" + label.getText() + "' should sit at the disabled foreground colour while unset");
        }
    }

    @Test
    @DisplayName("The toggle hides and reveals the quirks MegaMek ignores")
    void toggleHidesUnimplementedQuirks() {
        String marker = Messages.getString("CustomMekDialog.quirkNotImplementedSuffix").trim();
        QuirksPanel panel = buildPanel();
        JCheckBox toggle = unimplementedToggleOf(panel);
        if (toggle.isSelected()) {
            toggle.doClick();
        }

        assertEquals(0, countLabelsContaining(panel, marker),
              "No '" + marker + "' quirk should be listed while the toggle is off");

        toggle.doClick();

        assertTrue(countLabelsContaining(panel, marker) > 0,
              "The '" + marker + "' quirks should reappear once the toggle is on");
    }

    @Test
    @DisplayName("A placeholder is findable by its rules reference, like the real quirk rows")
    void placeholdersAreSearchableByRulesReference() {
        List<QuirkPlaceholder> negativePlaceholders = QuirkCatalog.getPlaceholders(Quirks.NEG_QUIRKS);
        assumeFalse(negativePlaceholders.isEmpty(), "No negative placeholder quirks to search for");
        QuirkPlaceholder placeholder = negativePlaceholders.getFirst();

        QuirksPanel panel = buildPanelShowingUnimplemented();
        filterFieldOf(panel).setText(placeholder.getRulesReference());

        assertTrue(findAll(panel, JLabel.class).stream()
                    .anyMatch(label -> placeholder.getDisplayableName().equals(label.getText())),
              "Searching for '" + placeholder.getRulesReference() + "' should still list '"
                    + placeholder.getDisplayableName() + "'");
    }

    @Test
    @DisplayName("A quirk the unit has stays listed even with the toggle off")
    void setQuirksSurviveTheToggle() {
        String marker = Messages.getString("CustomMekDialog.quirkNotImplementedSuffix").trim();

        // Give the unit a quirk MegaMek ignores, then build the panel with the toggle off
        Entity entity = new BipedMek();
        entity.getQuirks().getOption(OptionsConstants.QUIRK_POS_NIMBLE_JUMPER).setValue(true);
        GUIPreferences.getInstance().setShowUnimplementedQuirks(false);
        QuirksPanel panel = new QuirksPanel(entity, entity.getQuirks(), true, null, new HashMap<>());

        assertFalse(unimplementedToggleOf(panel).isSelected(), "This test needs the toggle off");
        String nimbleJumperName =
              entity.getQuirks().getOption(OptionsConstants.QUIRK_POS_NIMBLE_JUMPER).getDisplayableName();
        assertTrue(countLabelsContaining(panel, nimbleJumperName) > 0,
              "'" + nimbleJumperName + "' is set on this unit, so hiding unimplemented quirks must not hide it");
        assertTrue(countLabelsContaining(panel, marker) > 0,
              "The set quirk should still carry its '" + marker + "' marker");
    }

    private static long countLabelsContaining(QuirksPanel panel, String text) {
        return findAll(panel, JLabel.class).stream()
              .filter(label -> (label.getText() != null) && label.getText().contains(text))
              .count();
    }

    @Test
    @DisplayName("The toggle reveals the book quirks MegaMek has no option for")
    void toggleRevealsPlaceholderRows() {
        List<QuirkPlaceholder> negativePlaceholders = QuirkCatalog.getPlaceholders(Quirks.NEG_QUIRKS);
        assumeFalse(negativePlaceholders.isEmpty(), "No negative placeholder quirks to show");
        String placeholderName = negativePlaceholders.getFirst().getDisplayableName();

        QuirksPanel panel = buildPanel();
        JCheckBox toggle = unimplementedToggleOf(panel);
        if (toggle.isSelected()) {
            toggle.doClick();
        }
        assertTrue(findAll(panel, JLabel.class).stream()
                    .noneMatch(label -> placeholderName.equals(label.getText())),
              "'" + placeholderName + "' should be hidden while the toggle is off");

        toggle.doClick();

        assertTrue(toggle.isSelected(), "The toggle should now be on");
        assertTrue(findAll(panel, JLabel.class).stream()
                    .anyMatch(label -> placeholderName.equals(label.getText())),
              "'" + placeholderName + "' should be listed once the toggle is on");
    }

    @Test
    @DisplayName("Refreshing does not stack duplicate filter listeners")
    void refreshDoesNotStackListeners() {
        QuirksPanel panel = buildPanel();
        JTextField filterField = filterFieldOf(panel);
        int listenersBefore = ((AbstractDocument) filterField.getDocument()).getDocumentListeners().length;

        panel.refreshQuirks();
        panel.refreshQuirks();

        int listenersAfter = ((AbstractDocument) filterField.getDocument()).getDocumentListeners().length;
        assertTrue(listenersAfter == listenersBefore,
              "refreshQuirks() must not register another document listener (was " + listenersBefore
                    + ", now " + listenersAfter + ")");
    }
}
