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

package megamek.common.ui;

import java.awt.Component;
import java.awt.Window;
import java.lang.ref.WeakReference;
import javax.swing.Icon;

/**
 * Information about a detached tab, including its title, icon, and the component it contains.
 */
public class DetachedTabInfo {
    String title;
    Icon icon;
    int originalIndex;
    Component component;
    Window wrapperComponent;
    boolean isCloseableTab;
    boolean reattachAllowed;
    WeakReference<EnhancedTabbedPane> sourcePane;
    String dockGroupId;

    /**
     * Creates a new DetachedTabInfo instance
     *
     * @param title            Title of the tab
     * @param icon             Icon for the tab
     * @param component        Component to be displayed in the tab
     * @param wrapperComponent The window that wraps the tab
     * @param originalIndex    The original index of the tab in the source pane
     * @param isCloseableTab   Whether the tab is closeable
     */
    public DetachedTabInfo(String title, Icon icon, Component component, Window wrapperComponent, int originalIndex,
          boolean isCloseableTab) {
        this.title = title;
        this.icon = icon;
        this.component = component;
        this.wrapperComponent = wrapperComponent;
        this.originalIndex = originalIndex;
        this.isCloseableTab = isCloseableTab;
        this.reattachAllowed = false;
    }

    public Component getComponent() {
        return component;
    }
}
