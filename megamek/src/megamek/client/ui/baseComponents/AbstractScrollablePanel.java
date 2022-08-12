/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.baseComponents;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;


/**
 * This is the default Scrollable Panel, implementing Scrollable and designed to be used within a
 * ScrollPane, preferably AbstractScrollPane. It handles preferences, resources, and the frame.
 *
 * Inheriting classes must call initialize() in their constructors and override initialize().
 *
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
     * @see AbstractPanel#AbstractPanel(JFrame, String)
     */
    protected AbstractScrollablePanel(final JFrame frame, final String name) {
        super(frame, name);
    }

    /**
     * This creates an AbstractScrollablePanel using the default resource bundle and specified
     * double buffered boolean.
     * @see AbstractPanel#AbstractPanel(JFrame, String, boolean)
     */
    protected AbstractScrollablePanel(final JFrame frame, final String name,
                                      final boolean isDoubleBuffered) {
        super(frame, name, isDoubleBuffered);
    }

    /**
     * This creates an AbstractScrollablePanel using the default resource bundle and specified
     * layout manager.
     * @see AbstractPanel#AbstractPanel(JFrame, String, LayoutManager)
     */
    protected AbstractScrollablePanel(final JFrame frame, final String name,
                                      final LayoutManager layoutManager) {
        super(frame, name, layoutManager);
    }

    /**
     * This creates an AbstractScrollablePanel using the default MHQ resource bundle and specified layout
     * manager and double buffered boolean.
     * @see AbstractPanel#AbstractPanel(JFrame, String, LayoutManager, boolean)
     */
    protected AbstractScrollablePanel(final JFrame frame, final String name,
                                      final LayoutManager layoutManager,
                                      final boolean isDoubleBuffered) {
        super(frame, name, layoutManager, isDoubleBuffered);
    }

    /**
     * This creates an AbstractScrollablePanel using the specified resource bundle, layout manager,
     * and double buffered boolean. This is not recommended by default.
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
