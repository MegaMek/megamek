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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import megamek.common.options.GameOptions;
import org.junit.jupiter.api.Test;

/**
 * Covers issue #9151: the role filter boxes on the Force Generator tab could not be clicked.
 *
 * <p>The "Role Filters:" label inherited a four-column span from the section above it, so it stretched across
 * the whole row and lay on top of the boxes. A click on a box went to the label and the box never changed.</p>
 */
class ForceGeneratorRoleFilterClickTest {

    @Test
    void clickOnEachRoleFilterBox_reachesThatBox() {
        ForceGeneratorOptionsView view = new ForceGeneratorOptionsView(forceDescriptor -> { }, new GameOptions());
        view.setSize(1000, view.getPreferredSize().height);
        layOut(view);

        List<JCheckBox> boxes = new ArrayList<>();
        collectRoleFilterBoxes(view, boxes, false);
        assertFalse(boxes.isEmpty(), "the ground filters should be on show for the default unit type");

        int iconWidth = UIManager.getIcon("CheckBox.icon").getIconWidth();
        for (JCheckBox box : boxes) {
            Point iconCentre = SwingUtilities.convertPoint(box,
                  box.getInsets().left + (iconWidth / 2), box.getHeight() / 2, view);
            Component clicked = SwingUtilities.getDeepestComponentAt(view, iconCentre.x, iconCentre.y);
            assertSame(box, clicked, "a click on the " + box.getText() + " box should reach it, not "
                  + clicked.getClass().getSimpleName());
        }
    }

    /** Lays the panel out top-down without a window, the way it would be when shown. */
    private static void layOut(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layOut(nested);
            }
        }
    }

    private static void collectRoleFilterBoxes(Container container, List<JCheckBox> boxes, boolean insideFilters) {
        for (Component child : container.getComponents()) {
            boolean inside = insideFilters || (child instanceof MissionRoleFilterPanel);
            if (inside && child.isVisible() && (child instanceof JCheckBox checkBox)) {
                boxes.add(checkBox);
            }
            if (child.isVisible() && (child instanceof Container nested)) {
                collectRoleFilterBoxes(nested, boxes, inside);
            }
        }
    }
}
