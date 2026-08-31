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
package megamek.client.ui.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Image;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import megamek.client.ui.widget.MekPanelTabStrip.TabDescriptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the data-driven tab strip: it is built from descriptors, keeps its images per instance, and the classic and
 * control layouts are two lists over the same class.
 */
@DisplayName("MekPanelTabStrip")
class MekPanelTabStripTest {

    private static final List<TabDescriptor> TWO_TABS = List.of(
          new TabDescriptor("first", "a_idle.gif", "a_active.gif"),
          new TabDescriptor("second", "b_idle.gif", "b_active.gif"));

    @Test
    @DisplayName("Tab images are never shared between strips")
    void imagesAreInstanceState() {
        for (Field field : MekPanelTabStrip.class.getDeclaredFields()) {
            boolean holdsImages = Image.class.isAssignableFrom(field.getType())
                  || (field.getType().isArray() && Image.class.isAssignableFrom(field.getType().getComponentType()));
            if (holdsImages) {
                assertFalse(Modifier.isStatic(field.getModifiers()),
                      field.getName() + " must not be static: two strips of different lengths would share it");
            }
        }
    }

    @Test
    @DisplayName("The classic layout has six tabs in the F1-F6 order, the control layout three")
    void layoutsAreListsOfDescriptors() {
        UnitDisplaySkinSpecification skin = new UnitDisplaySkinSpecification();

        List<TabDescriptor> classic = MekPanelTabStrip.classicTabs(skin);
        List<TabDescriptor> control = MekPanelTabStrip.controlTabs(skin);

        assertEquals(6, classic.size());
        assertEquals(MekPanelTabStrip.SUMMARY, classic.get(0).cardName());
        assertEquals(MekPanelTabStrip.PILOT, classic.get(1).cardName());
        assertEquals(MekPanelTabStrip.ARMOR, classic.get(2).cardName());
        assertEquals(MekPanelTabStrip.WEAPONS, classic.get(3).cardName());
        assertEquals(MekPanelTabStrip.SYSTEMS, classic.get(4).cardName());
        assertEquals(MekPanelTabStrip.EXTRAS, classic.get(5).cardName());
        assertEquals(List.of(MekPanelTabStrip.SUMMARY, MekPanelTabStrip.WEAPONS, MekPanelTabStrip.CONTROL),
              control.stream().map(TabDescriptor::cardName).toList());
    }

    @Test
    @DisplayName("A skin that names no control tab art falls back to the systems tab images")
    void controlTabFallsBackToSystemsImages() {
        UnitDisplaySkinSpecification skin = new UnitDisplaySkinSpecification();

        TabDescriptor controlTab = MekPanelTabStrip.controlTabs(skin).get(2);

        assertEquals(skin.getSystemsTabIdle(), controlTab.idleImage());
        assertEquals(skin.getSystemsTabActive(), controlTab.activeImage());
    }

    @Test
    @DisplayName("Selecting a tab clamps to the strip and can go by card name")
    void selectionClampsAndResolvesByName() {
        MekPanelTabStrip strip = new MekPanelTabStrip(null, TWO_TABS);

        strip.setTab(7);
        assertEquals(1, strip.getActiveTab(), "past the end selects the last tab");
        strip.setTab(-3);
        assertEquals(0, strip.getActiveTab(), "before the start selects the first tab");

        assertTrue(strip.setTab("second"));
        assertEquals(1, strip.getActiveTab());
        assertFalse(strip.setTab("no such card"), "an unknown card selects nothing");
        assertEquals(1, strip.getActiveTab(), "and leaves the selection alone");
        assertEquals(2, strip.getTabCount());
        assertEquals("first", strip.getCardName(0));
    }

    @Test
    @DisplayName("A strip needs at least one tab")
    void emptyStripIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MekPanelTabStrip(null, List.of()));
    }
}
