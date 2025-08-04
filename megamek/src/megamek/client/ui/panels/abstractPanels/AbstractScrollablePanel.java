/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels.abstractPanels;

import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.util.ResourceBundle;
import javax.swing.JFrame;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;


/**
 * This is the default Scrollable Panel, implementing Scrollable and designed to be used within a ScrollPane, preferably
 * AbstractScrollPane. It handles preferences, resources, and the frame.
 * <p>
 * Inheriting classes must call initialize() in their constructors and override initialize().
 * <p>
 * This is directly tied to MekHQ's AbstractMHQScrollablePanel, and any changes here MUST be verified there.
 */
public abstract class AbstractScrollablePanel extends AbstractPanel implements Scrollable {
    //region Variable Declarations
    // Track the width and re-size as needed by default
    private boolean trackViewportWidth = true;
    //endregion Variable Declarations

    //region Constructors

    /**
     * This creates an AbstractScrollablePanel using the default resource bundle.
     *
     * @see AbstractPanel#AbstractPanel(JFrame, String)
     */
    protected AbstractScrollablePanel(final JFrame frame, final String name) {
        super(frame, name);
    }

    /**
     * This creates an AbstractScrollablePanel using the default resource bundle and specified double buffered boolean.
     *
     * @see AbstractPanel#AbstractPanel(JFrame, String, boolean)
     */
    protected AbstractScrollablePanel(final JFrame frame, final String name,
          final boolean isDoubleBuffered) {
        super(frame, name, isDoubleBuffered);
    }

    /**
     * This creates an AbstractScrollablePanel using the default resource bundle and specified layout manager.
     *
     * @see AbstractPanel#AbstractPanel(JFrame, String, LayoutManager)
     */
    protected AbstractScrollablePanel(final JFrame frame, final String name,
          final LayoutManager layoutManager) {
        super(frame, name, layoutManager);
    }

    /**
     * This creates an AbstractScrollablePanel using the default resource bundle and specified layout manager and double
     * buffered boolean.
     *
     * @see AbstractPanel#AbstractPanel(JFrame, String, LayoutManager, boolean)
     */
    protected AbstractScrollablePanel(final JFrame frame, final String name,
          final LayoutManager layoutManager,
          final boolean isDoubleBuffered) {
        super(frame, name, layoutManager, isDoubleBuffered);
    }

    /**
     * This creates an AbstractScrollablePanel using the specified resource bundle, layout manager, and double buffered
     * boolean. This is not recommended by default.
     *
     * @see AbstractPanel#AbstractPanel(JFrame, ResourceBundle, String, LayoutManager, boolean)
     */
    protected AbstractScrollablePanel(final JFrame frame, final ResourceBundle resources,
          final String name, final LayoutManager layoutManager,
          final boolean isDoubleBuffered) {
        super(frame, resources, name, layoutManager, isDoubleBuffered);
    }
    //endregion Constructors

    //region Setters
    public void setTracksViewportWidth(final boolean trackViewportWidth) {
        this.trackViewportWidth = trackViewportWidth;
    }
    //endregion Setters

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        // tell the JScrollPane that we want to be our 'preferredSize'
        // but later, we'll say that vertically, it should scroll.
        return super.getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation,
          final int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(final Rectangle visible, final int orientation,
          final int direction) {
        return (orientation == SwingConstants.VERTICAL) ? visible.height : visible.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return trackViewportWidth;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        // We don't want to track the height, because we want to scroll vertically.
        return false;
    }
}
