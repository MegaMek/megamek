/*
 * Copyright (C) 2008 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.Serial;

/**
 * A helper class for setting line-wise GridBagLayouts Do not use this if you need a Component to span two rows
 *
 * @author beerockxs
 */
public class GBC extends GridBagConstraints {
    @Serial
    private static final long serialVersionUID = 6653886439201996453L;

    private GBC() {
        anchor = WEST;
    }

    /**
     * @return a standard <code>GridBagConstraints</code>, anchored to <code>GridBagConstraints.WEST</code>
     */
    public static GBC std() {
        return new GBC();
    }

    /**
     * @return a <code>GridBagConstraints</code> that will have a component fill a line
     *
     * @see GridBagConstraints#REMAINDER
     */
    public static GBC eol() {
        GBC c = std();
        c.gridwidth = REMAINDER;
        return c;
    }

    /**
     * @return a <code>GridBagConstraints</code> that will have a component fill a line, and have a 10 pixel inset to
     *       the south (i.e. a paragraph)
     */
    public static GBC eop() {
        return eol().insets(0, 0, 0, 10);
    }

    /**
     * change the anchor of this <code>GridBagConstraints</code>
     *
     * @param a the <code>int</code> anchor to set
     *
     * @return <code>this</code>
     *
     * @see GridBagConstraints#anchor
     */
    public GBC anchor(int a) {
        anchor = a;
        return this;
    }

    /**
     * change the insets of this <code>GridBagConstraints</code>
     *
     * @return <code>this</code>
     *
     * @see GridBagConstraints#insets
     */
    public GBC insets(int left, int top, int right, int bottom) {
        insets = new Insets(top, left, bottom, right);
        return this;
    }

    /**
     * set this <code>GridBagConstraints</code> so that the corresponding Component will fill horizontally and
     * vertically.
     *
     * @return <code>this</code>
     */
    public GBC fill() {
        return fill(BOTH);
    }

    /**
     * set this <code>GridBagConstraints</code> so that the corresponding will fill according to the
     *
     * @param value either <code>GridBagConstraints.HORIZONTAL</code>,
     *              <code>GridBagConstraints.VERTICAL</code> or
     *              <code>GridBagConstraints.BOTH</code> and
     *
     * @return <code>this</code>
     */
    public GBC fill(int value) {
        fill = value;
        if ((value == HORIZONTAL) || (value == BOTH)) {
            weightx = 1.0;
        }
        if ((value == VERTICAL) || (value == BOTH)) {
            weighty = 1.0;
        }
        return this;
    }

    /**
     * Set the padding of this <code>GridBagConstraints</code>
     *
     * @param padX the <code>int</code> padX to set
     * @param padY the <code>int</code> padY to set
     *
     * @return <code>this</code>
     *
     * @see GridBagConstraints#ipadx
     * @see GridBagConstraints#ipady
     */
    public GBC pad(int padX, int padY) {
        ipadx = padX;
        ipady = padY;
        return this;
    }

    /**
     * Set the gridX of this <code>GridBagConstraints</code>
     *
     * @param gridX the <code>int</code> gridX to set
     *
     * @return <code>this</code>
     *
     * @see GridBagConstraints#gridx
     */
    public GBC gridX(int gridX) {
        this.gridx = gridX;
        return this;
    }

    /**
     * Set the gridY of this <code>GridBagConstraints</code>
     *
     * @param gridY the <code>int</code> gridY to set
     *
     * @return <code>this</code>
     *
     * @see GridBagConstraints#gridy
     */
    public GBC gridY(int gridY) {
        this.gridy = gridY;
        return this;
    }

    /**
     * Set the grid height of  this <code>GridBagConstraints</code>
     *
     * @param height the <code>int</code> grid height to set
     *
     * @return <code>this</code>
     *
     * @see GridBagConstraints#gridheight
     */
    public GBC gridHeight(int height) {
        gridheight = height;
        return this;
    }

    /**
     * Set the grid width of  this <code>GridBagConstraints</code>
     *
     * @param width the <code>int</code> grid width to set
     *
     * @return <code>this</code>
     *
     * @see GridBagConstraints#gridwidth
     */
    public GBC gridWidth(int width) {
        gridwidth = width;
        return this;
    }

    /**
     * Set the weightX of this <code>GridBagConstraints</code>
     *
     * @param weight the <code>double</code> weightX to set
     *
     * @return <code>this</code>
     *
     * @see GridBagConstraints#weightx
     */
    public GBC weightX(double weight) {
        weightx = weight;
        return this;
    }

    /**
     * Set the weighty of this <code>GridBagConstraints</code>
     *
     * @param weight the <code>double</code> weighty to set
     *
     * @return <code>this</code>
     *
     * @see GridBagConstraints#weighty
     */
    public GBC weighty(double weight) {
        weighty = weight;
        return this;
    }
}
