/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.baseComponents;

import java.awt.Component;
import java.awt.Graphics2D;
import javax.swing.JButton;
import javax.swing.UIManager;

import com.formdev.flatlaf.icons.FlatAbstractIcon;

/**
 * This is an icon button with a three-stacked-lines icon that indicates a menu of some kind. Usable e.g. as an action
 * button in an EnhancedTabbedPane.
 *
 * @see megamek.common.EnhancedTabbedPane
 */
public class MenuButton extends JButton {

    public MenuButton() {
        super(new MenuIcon());
        setFocusable(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
    }

    /**
     * This is an icon showing three stacked horizontal lines icon that usually indicates a general menu of some kind.
     */
    public static class MenuIcon extends FlatAbstractIcon {

        public MenuIcon() {
            super(16, 16, UIManager.getColor("Actions.Grey"));
        }

        @Override
        protected void paintIcon(Component c, Graphics2D g) {
            g.fillRoundRect(2, 3, 12, 2, 1, 1);
            g.fillRoundRect(2, 7, 12, 2, 1, 1);
            g.fillRoundRect(2, 11, 12, 2, 1, 1);
        }
    }
}
