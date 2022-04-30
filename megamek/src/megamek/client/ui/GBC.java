/*
* MegaMek -
* Copyright (C) 2008 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.client.ui;

import java.awt.GridBagConstraints;
import java.awt.Insets;

/**
 * A helper class for setting line-wise GridBagLayouts
 * Do not use this if you need a Component to span two rows
 * @author beerockxs
 */
public class GBC extends GridBagConstraints {
    private static final long serialVersionUID = 6653886439201996453L;

    private GBC() {
        anchor = WEST;
    }

    /**
     * @return a standard <code>GridBagConstraints</code>,
     *         anchored to <code>GridBagConstraints.WEST</code>
     */
    public static GBC std() {
        return new GBC();
    }

    /**
     * @return a <code>GridBagConstraints</code> that will have a component
     *         fill a line
     * @see GridBagConstraints#REMAINDER
     */
    public static GBC eol() {
        GBC c = std();
        c.gridwidth = REMAINDER;
        return c;
    }

    /**
     * @return a <code>GridBagConstraints</code> that will have a component fill
     *         a line, and have a 10 pixel inset to the south (ie. a paragraph)
     */
    public static GBC eop() {
        return eol().insets(0, 0, 0, 10);
    }

    /**
     * change the anchor of this <code>GridBagConstraints</code>
     * @param a the <code>int</code> anchor to set
     * @return <code>this</code>
     * @see GridBagConstraints#anchor
     */
    public GBC anchor(int a) {
        anchor = a;
        return this;
    }

    /**
     * change the insets of this <code>GridBagConstraints</code>
     * @param left
     * @param top
     * @param right
     * @param bottom
     * @return <code>this</code>
     * @see GridBagConstraints#insets
     */
    public GBC insets(int left, int top, int right, int bottom) {
        insets = new Insets(top, left, bottom, right);
        return this;
    }

    /**
     * set this <code>GridBagConstraints</code> so that the corresponding
     * Component will fill horizontally and vertically.
     * @return <code>this</code>
     */
    public GBC fill() {
        return fill(BOTH);
    }

    /**
     * set this <code>GridBagConstraints</code> so that the corresponding
     * will fill according to the
     * @param value
     * either <code>GridBagConstraints.HORIZONTAL</code>,
     * <code>GridBagConstraints.VERTICAL</code> or
     * <code>GridBagConstraints.BOTH</code> and
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
     * @param padx the <code>int</code> ipadx to set
     * @param pady the <code>int</code> ipady to set
     * @return <code>this</code>
     * @see GridBagConstraints#ipadx
     * @see GridBagConstraints#ipady
     */
    public GBC pad(int padx, int pady) {
        ipadx = padx;
        ipady = pady;
        return this;
    }

    /**
     * Set the gridx of this <code>GridBagConstraints</code>
     * @param gridx the <code>int</code> gridx to set
     * @return <code>this</code>
     * @see GridBagConstraints#gridx
     */
    public GBC gridx(int gridx) {
        this.gridx = gridx;
        return this;
    }

    /**
     * Set the gridy of this <code>GridBagConstraints</code>
     * @param gridy the <code>int</code> gridy to set
     * @return <code>this</code>
     * @see GridBagConstraints#gridy
     */
    public GBC gridy(int gridy) {
        this.gridy = gridy;
        return this;
    }

    /**
     * Set the gridheight of  this <code>GridBagConstraints</code>
     * @param height the <code>int</code> gridheight to set
     * @return <code>this</code>
     * @see GridBagConstraints#gridheight
     */
    public GBC gridheight(int height) {
        gridheight = height;
        return this;
    }

    /**
     * Set the gridwidth of  this <code>GridBagConstraints</code>
     * @param width the <code>int</code> gridheight to set
     * @return <code>this</code>
     * @see GridBagConstraints#gridwidth
     */
    public GBC gridwidth(int width) {
        gridwidth = width;
        return this;
    }

    /**
     * Set the weightx of this <code>GridBagConstraints</code>
     * @param weight the <code>double</code> weightx to set
     * @return <code>this</code>
     * @see GridBagConstraints#weightx
     */
    public GBC weightx(double weight) {
        weightx = weight;
        return this;
    }

    /**
     * Set the weighty of this <code>GridBagConstraints</code>
     * @param weight the <code>double</code> weighty to set
     * @return <code>this</code>
     * @see GridBagConstraints#weighty
     */
    public GBC weighty(double weight) {
        weighty = weight;
        return this;
    }
}
