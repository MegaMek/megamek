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
package megamek.client.ui.widget.picmap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that a picture standing for a location reports a click on it the way the polygon areas of the Mek and vehicle
 * diagrams do, so battle armor troopers and infantry figures can be clicked.
 */
@DisplayName("PMPicArea location clicks")
class PMPicAreaTest {

    /** Records what it is told, and answers single or double clicks as asked. */
    private static final class RecordingListener implements LocationSelectListener {
        final List<Integer> selected = new ArrayList<>();
        final boolean singleClick;

        RecordingListener(boolean singleClick) {
            this.singleClick = singleClick;
        }

        @Override
        public void locationSelected(int location) {
            selected.add(location);
        }

        @Override
        public boolean selectsOnSingleClick() {
            return singleClick;
        }
    }

    private static MouseEvent click(int clickCount) {
        return new MouseEvent(new JPanel(), MouseEvent.MOUSE_CLICKED, 0, 0, 5, 5, clickCount, false);
    }

    private static BufferedImage picture() {
        return new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
    }

    @Test
    @DisplayName("A single click reports the location to a listener that takes single clicks")
    void singleClickReportsLocation() {
        RecordingListener listener = new RecordingListener(true);
        PMPicArea trooper = new PMPicArea(picture(), listener, BattleArmor.LOC_TROOPER_3);

        trooper.onMouseClick(click(1));

        assertEquals(List.of(BattleArmor.LOC_TROOPER_3), listener.selected);
        assertEquals(BattleArmor.LOC_TROOPER_3, trooper.getLocation());
    }

    @Test
    @DisplayName("A listener that wants double clicks ignores single ones")
    void doubleClickListenerIgnoresSingleClick() {
        RecordingListener listener = new RecordingListener(false);
        PMPicArea trooper = new PMPicArea(picture(), listener, BattleArmor.LOC_TROOPER_1);

        trooper.onMouseClick(click(1));
        assertTrue(listener.selected.isEmpty(), "one click is not enough for this listener");

        trooper.onMouseClick(click(2));
        assertEquals(List.of(BattleArmor.LOC_TROOPER_1), listener.selected);
    }

    @Test
    @DisplayName("A picture that stands for no location stays silent")
    void pictureWithoutLocationIsSilent() {
        PMPicArea decoration = new PMPicArea(picture());

        decoration.onMouseClick(click(2));

        assertEquals(Entity.LOC_NONE, decoration.getLocation());
    }
}
