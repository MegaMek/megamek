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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Builds a real {@link PilotOptionsPanel} and drives its search box.
 *
 * <p>Guards the panel against regressions from the shared filter-bar and row-layout helpers it composes: this panel
 * shipped before those existed, so a change to {@code OptionFilterBar} or {@code OptionRowLayout} that broke it
 * would otherwise only show up in play.</p>
 */
@DisplayName("Pilot options panel construction")
class PilotOptionsPanelBuildTest {

    private boolean originalShowUnimplemented;

    @BeforeAll
    static void requireDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "PilotOptionsPanel needs a display");
    }

    @BeforeEach
    void rememberPreference() {
        originalShowUnimplemented = GUIPreferences.getInstance().getShowUnimplementedSpas();
    }

    @AfterEach
    void restorePreference() {
        GUIPreferences.getInstance().setShowUnimplementedSpas(originalShowUnimplemented);
    }

    private static PilotOptionsPanel buildPanel() {
        Entity entity = new BipedMek();
        GameOptions gameOptions = new GameOptions();
        // The advantages group is what the search is for; without it the panel has nothing to list
        gameOptions.getOption(OptionsConstants.RPG_PILOT_ADVANTAGES).setValue(true);

        PilotOptionsPanel panel = new PilotOptionsPanel(entity, true, gameOptions, null,
              (option, optionComp) -> { });
        panel.refreshOptions(new PilotOptions());
        return panel;
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

    private static <T extends Component> T findNamed(PilotOptionsPanel panel, Class<T> type, String name) {
        return findAll(panel, type).stream()
              .filter(component -> name.equals(component.getName()))
              .findFirst()
              .orElseThrow(() -> new AssertionError("The pilot options panel has no " + name));
    }

    @Test
    @DisplayName("The panel builds with a filter bar and ability rows")
    void panelBuildsWithFilterBar() {
        PilotOptionsPanel panel = buildPanel();

        assertNotNull(findNamed(panel, JTextField.class, "txtSpaFilter"));
        assertNotNull(findNamed(panel, JCheckBox.class, "chkShowUnimplementedSpas"));
        assertFalse(findNamed(panel, JLabel.class, "lblSpaMatchCount").isVisible(),
              "The match count is hidden until a filter is typed");
        assertFalse(panel.getOptionComponents().isEmpty(), "The panel should have ability rows");
    }

    @Test
    @DisplayName("Typing in the search box updates the match count")
    void filteringUpdatesTheMatchCount() {
        PilotOptionsPanel panel = buildPanel();
        JLabel matchCountLabel = findNamed(panel, JLabel.class, "lblSpaMatchCount");

        findNamed(panel, JTextField.class, "txtSpaFilter").setText("sniper");

        assertTrue(matchCountLabel.isVisible(), "The match count shows once a filter is typed");
        assertFalse(matchCountLabel.getText().isBlank());
        assertFalse(matchCountLabel.getText().startsWith("!"),
              "Missing resource key for the SPA match count: " + matchCountLabel.getText());
    }

    @Test
    @DisplayName("The toggle reveals the CamOps abilities MegaMek does not implement")
    void toggleRevealsPlaceholderRows() {
        PilotOptionsPanel panel = buildPanel();
        JCheckBox toggle = findNamed(panel, JCheckBox.class, "chkShowUnimplementedSpas");
        if (toggle.isSelected()) {
            toggle.doClick();
        }
        int rowsWithToggleOff = findAll(panel, JCheckBox.class).size();

        toggle.doClick();

        assertTrue(toggle.isSelected(), "The toggle should now be on");
        assertTrue(findAll(panel, JCheckBox.class).size() > rowsWithToggleOff,
              "Turning the toggle on should add the unimplemented ability rows");
    }
}
