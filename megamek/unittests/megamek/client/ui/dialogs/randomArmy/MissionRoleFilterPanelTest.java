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
package megamek.client.ui.dialogs.randomArmy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Covers the mission-role filters.
 *
 * <p>The claim worth holding is that a filter the player can tick always does something. Escort and Bomber were
 * drawn on the aerospace group for years without ever being read back, so ticking them did nothing at all.</p>
 */
class MissionRoleFilterPanelTest {

    private static List<JCheckBox> checkBoxesIn(Container container) {
        List<JCheckBox> found = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (child instanceof JCheckBox checkBox) {
                found.add(checkBox);
            } else if (child instanceof Container nested) {
                found.addAll(checkBoxesIn(nested));
            }
        }
        return found;
    }

    /** The checkboxes on whichever group is currently visible. */
    private static List<JCheckBox> visibleCheckBoxes(MissionRoleFilterPanel panel) {
        List<JCheckBox> found = new ArrayList<>();
        for (Component group : panel.getComponents()) {
            if (group.isVisible() && (group instanceof Container container)) {
                found.addAll(checkBoxesIn(container));
            }
        }
        return found;
    }

    private static ForceDescriptor descriptorFor(int unitType) {
        ForceDescriptor forceDescriptor = new ForceDescriptor();
        forceDescriptor.setUnitType(unitType);
        return forceDescriptor;
    }

    /**
     * The regression test for the dead controls: tick everything the group shows, and every one of them must turn
     * into a role. A checkbox the player can see and tick must not be ignored.
     */
    @ParameterizedTest
    @ValueSource(ints = { UnitType.MEK, UnitType.TANK, UnitType.INFANTRY, UnitType.BATTLE_ARMOR,
                          UnitType.AEROSPACE_FIGHTER, UnitType.CONV_FIGHTER })
    void everyFilterShownForAUnitTypeIsAlsoRead(int unitType) {
        MissionRoleFilterPanel panel = new MissionRoleFilterPanel();
        panel.showFor(unitType);

        List<JCheckBox> shown = visibleCheckBoxes(panel);
        assertFalse(shown.isEmpty(), "unit type " + unitType + " must show some filters");
        shown.forEach(checkBox -> checkBox.setSelected(true));

        ForceDescriptor forceDescriptor = descriptorFor(unitType);
        panel.applyTo(forceDescriptor, unitType);

        assertEquals(shown.size(), forceDescriptor.getRoles().size(),
              "every filter shown for unit type " + unitType + " must contribute a role when ticked");
    }

    @Test
    void anUntickedPanelContributesNothing() {
        MissionRoleFilterPanel panel = new MissionRoleFilterPanel();
        panel.showFor(UnitType.MEK);

        ForceDescriptor forceDescriptor = descriptorFor(UnitType.MEK);
        panel.applyTo(forceDescriptor, UnitType.MEK);

        assertTrue(forceDescriptor.getRoles().isEmpty(), "nothing ticked means no roles");
    }

    /** Only one group is ever on screen, so the player is never shown filters for a unit type they did not pick. */
    @Test
    void exactlyOneGroupIsVisibleAtATime() {
        MissionRoleFilterPanel panel = new MissionRoleFilterPanel();
        for (int unitType : new int[] { UnitType.MEK, UnitType.INFANTRY, UnitType.AEROSPACE_FIGHTER }) {
            panel.showFor(unitType);
            long visible = 0;
            for (Component group : panel.getComponents()) {
                if (group.isVisible()) {
                    visible++;
                }
            }
            assertEquals(1, visible, "unit type " + unitType + " must show exactly one group");
        }
    }

    /**
     * A filter ticked on the ground group must not follow the player to an infantry force, or a force would be
     * filtered by something the player can no longer see to untick.
     */
    @Test
    void filtersFromAHiddenGroupDoNotLeakIntoAnotherUnitType() {
        MissionRoleFilterPanel panel = new MissionRoleFilterPanel();
        panel.showFor(UnitType.MEK);
        visibleCheckBoxes(panel).forEach(checkBox -> checkBox.setSelected(true));

        panel.showFor(UnitType.INFANTRY);
        ForceDescriptor forceDescriptor = descriptorFor(UnitType.INFANTRY);
        panel.applyTo(forceDescriptor, UnitType.INFANTRY);

        assertTrue(forceDescriptor.getRoles().isEmpty(),
              "ground filters must not apply to an infantry force");
    }

    @Test
    void clearingUnticksEveryGroup() {
        MissionRoleFilterPanel panel = new MissionRoleFilterPanel();
        for (int unitType : new int[] { UnitType.MEK, UnitType.INFANTRY, UnitType.AEROSPACE_FIGHTER }) {
            panel.showFor(unitType);
            visibleCheckBoxes(panel).forEach(checkBox -> checkBox.setSelected(true));
        }

        panel.clearSelections();

        for (int unitType : new int[] { UnitType.MEK, UnitType.INFANTRY, UnitType.AEROSPACE_FIGHTER }) {
            panel.showFor(unitType);
            ForceDescriptor forceDescriptor = descriptorFor(unitType);
            panel.applyTo(forceDescriptor, unitType);
            assertTrue(forceDescriptor.getRoles().isEmpty(),
                  "clearing must leave nothing ticked for unit type " + unitType);
        }
    }

    @Test
    void aNullUnitTypeContributesNothing() {
        MissionRoleFilterPanel panel = new MissionRoleFilterPanel();
        panel.showFor(UnitType.MEK);
        visibleCheckBoxes(panel).forEach(checkBox -> checkBox.setSelected(true));

        ForceDescriptor forceDescriptor = new ForceDescriptor();
        panel.applyTo(forceDescriptor, null);

        assertTrue(forceDescriptor.getRoles().isEmpty(), "a combined-arms force adds no unit-type filters");
    }
}
