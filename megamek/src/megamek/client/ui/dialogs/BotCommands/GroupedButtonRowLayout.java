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
package megamek.client.ui.dialogs.BotCommands;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lays out buttons in one row, all the same width, with a wider gap in front of each button that starts a new group.
 * The docked Bot Commands panel uses it to set the CASPAR buttons, the Princess buttons and the buttons for every bot
 * apart from one another, which a {@link java.awt.GridLayout} cannot do: its gaps are all the same size.
 *
 * <p>Components that are not visible take no room.</p>
 */
class GroupedButtonRowLayout implements LayoutManager {

    private final int buttonGap;
    private final int groupGap;
    private final Set<Component> groupStarts;

    /**
     * @param buttonGap   the gap between two buttons of one group, in pixels
     * @param groupGap    the gap in front of a button that starts a group, in pixels
     * @param groupStarts the buttons that each start a group; the first button of the row needs no entry
     */
    GroupedButtonRowLayout(int buttonGap, int groupGap, Collection<? extends Component> groupStarts) {
        this.buttonGap = buttonGap;
        this.groupGap = groupGap;
        this.groupStarts = new HashSet<>(groupStarts);
    }

    @Override
    public void addLayoutComponent(String name, Component component) {
        // nothing to track: the row is the container's visible components, in order
    }

    @Override
    public void removeLayoutComponent(Component component) {
        // nothing to track: the row is the container's visible components, in order
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return rowSize(parent, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return rowSize(parent, false);
    }

    @Override
    public void layoutContainer(Container parent) {
        List<Component> buttons = visibleComponents(parent);
        if (buttons.isEmpty()) {
            return;
        }
        Insets insets = parent.getInsets();
        int rowWidth = parent.getWidth() - insets.left - insets.right;
        int rowHeight = parent.getHeight() - insets.top - insets.bottom;
        int widthForButtons = Math.max(0, rowWidth - totalGapWidth(buttons));
        int buttonWidth = widthForButtons / buttons.size();
        // the pixels left over by the division go to the first buttons, one each, so the row ends flush
        int widerButtonCount = widthForButtons % buttons.size();
        int nextLeftEdge = insets.left;
        for (int position = 0; position < buttons.size(); position++) {
            Component button = buttons.get(position);
            nextLeftEdge += gapBefore(button, position);
            int width = buttonWidth + ((position < widerButtonCount) ? 1 : 0);
            button.setBounds(nextLeftEdge, insets.top, width, rowHeight);
            nextLeftEdge += width;
        }
    }

    /**
     * @param parent      the container holding the row
     * @param isPreferred {@code true} for the preferred size, {@code false} for the minimum size
     *
     * @return the size of the row when every button is as wide as the widest one
     */
    private Dimension rowSize(Container parent, boolean isPreferred) {
        List<Component> buttons = visibleComponents(parent);
        int widestButton = 0;
        int tallestButton = 0;
        for (Component button : buttons) {
            Dimension buttonSize = isPreferred ? button.getPreferredSize() : button.getMinimumSize();
            widestButton = Math.max(widestButton, buttonSize.width);
            tallestButton = Math.max(tallestButton, buttonSize.height);
        }
        Insets insets = parent.getInsets();
        int width = (widestButton * buttons.size()) + totalGapWidth(buttons) + insets.left + insets.right;
        return new Dimension(width, tallestButton + insets.top + insets.bottom);
    }

    /**
     * @param buttons the row's buttons, in order
     *
     * @return the width of all the gaps between them
     */
    private int totalGapWidth(List<Component> buttons) {
        int totalGapWidth = 0;
        for (int position = 0; position < buttons.size(); position++) {
            totalGapWidth += gapBefore(buttons.get(position), position);
        }
        return totalGapWidth;
    }

    /**
     * @param button   a button of the row
     * @param position its place in the row, from 0
     *
     * @return the gap to leave in front of the button: none for the first button, the group gap for a button that
     *       starts a group, the button gap otherwise
     */
    private int gapBefore(Component button, int position) {
        if (position == 0) {
            return 0;
        }
        return groupStarts.contains(button) ? groupGap : buttonGap;
    }

    /**
     * @param parent the container holding the row
     *
     * @return the container's visible components, in the order they were added
     */
    private List<Component> visibleComponents(Container parent) {
        List<Component> visibleComponents = new ArrayList<>();
        for (Component component : parent.getComponents()) {
            if (component.isVisible()) {
                visibleComponents.add(component);
            }
        }
        return visibleComponents;
    }
}
