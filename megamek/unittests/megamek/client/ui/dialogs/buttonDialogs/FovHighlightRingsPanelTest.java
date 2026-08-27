/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version.
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
package megamek.client.ui.dialogs.buttonDialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.Dimension;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JSpinner;

import org.junit.jupiter.api.Test;

class FovHighlightRingsPanelTest {

    @Test
    void exposesOnlyRendererEffectiveRadiusColourPairs() {
        FovHighlightRingsPanel panel = new FovHighlightRingsPanel(
            "5 10 15 20 25",
              "0.3 1.0 1.0 ; 0.45 1.0 1.0 ; 0.6 1.0 1.0 ; 0.75 1.0 1.0 ; "
                    + "0.9 1.0 1.0 ; 1.05 1.0 1.0",
              () -> { });

        assertEquals(5, panel.getRangeCount());
        assertEquals("5 10 15 20 25", panel.getRadiiValue());
        assertEquals("0.3 1.0 1.0 ; 0.45 1.0 1.0 ; 0.6 1.0 1.0 ; 0.75 1.0 1.0 ; 0.9 1.0 1.0",
            panel.getColoursValue());
    }

    @Test
    void keepsEditedRangesOrderedAndSerializedTogether() {
        AtomicInteger changes = new AtomicInteger();
        FovHighlightRingsPanel panel = new FovHighlightRingsPanel(
              "5 10", "0.3 1.0 1.0 ; 0.6 1.0 1.0", changes::incrementAndGet);

        panel.setDistance(1, 3);
        panel.setColour(0, Color.BLUE);
        panel.addRange();
        panel.removeRange(1);

        assertEquals(2, panel.getRangeCount());
        assertEquals("3 10", panel.getRadiiValue());
        assertEquals(Color.BLUE, panel.getColour(0));
        assertEquals(4, changes.get());
    }

    @Test
    void recoversUsableRangesFromMalformedLegacyValues() {
        FovHighlightRingsPanel panel = new FovHighlightRingsPanel(
            "20 invalid 5 90",
            "0.2 1 1 ; invalid ; 0.4 1 1 ; 0.8 1 1",
              () -> { });

        assertEquals(3, panel.getRangeCount());
        assertEquals("5 20 60", panel.getRadiiValue());
    }

    @Test
    void rejectsDistanceThatClampsOntoExistingRange() {
        AtomicInteger changes = new AtomicInteger();
        FovHighlightRingsPanel panel = new FovHighlightRingsPanel(
              "5 60", "0.3 1.0 1.0 ; 0.6 1.0 1.0", changes::incrementAndGet);

        panel.setDistance(0, 999);

        assertEquals("5 60", panel.getRadiiValue());
        assertEquals(0, changes.get());
    }

    @Test
    void sizesIconButtonsToTheAdjacentControlHeight() {
        JButton button = new JButton();
        JSpinner control = new JSpinner();

        FovHighlightRingsPanel.sizeIconButtonToControlHeight(button, control);

        int side = control.getPreferredSize().height;
        Dimension expectedSize = new Dimension(side, side);
        assertEquals(expectedSize, button.getPreferredSize());
        assertEquals(expectedSize, button.getMinimumSize());
        assertEquals(expectedSize, button.getMaximumSize());
    }
}
