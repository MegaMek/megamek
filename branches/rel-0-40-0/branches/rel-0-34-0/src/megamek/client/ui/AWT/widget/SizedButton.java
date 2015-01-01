/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

package megamek.client.ui.AWT.widget;

import java.awt.Button;
import java.awt.Dimension;
import java.io.Serializable;

/**
 * This class implements buttons that have a preferred size that is different
 * than the smallest size needed to show its contents.
 * 
 * @author James Damour
 */
public class SizedButton extends Button implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -3034809147641614294L;
    /**
     * The preferred size of this button.
     */
    private Dimension prefSize = new Dimension();

    /**
     * Construct a button which can have its preferred size set.
     */
    public SizedButton() {
        super();
        setPreferredSize(super.getPreferredSize());
    }

    /**
     * Construct a button with the given name that can have its preferred size
     * set.
     * 
     * @param label - the <code>String</code> label for the button.
     */
    public SizedButton(String label) {
        super(label);
        setPreferredSize(super.getPreferredSize());
    }

    /**
     * Construct a button with the given preferred size.
     * 
     * @param size - the preferred <code>Dimension</code> for this button. The
     *            input parameter can be safely reused after this call.
     */
    public SizedButton(Dimension size) {
        super();
        setPreferredSize(size);
    }

    /**
     * Construct a button with the given label and preferred size.
     * 
     * @param label - the <code>String</code> label for the button.
     * @param size - the preferred <code>Dimension</code> for this button. The
     *            input parameter can be safely reused after this call.
     */
    public SizedButton(String label, Dimension size) {
        super(label);
        setPreferredSize(size);
    }

    /**
     * Get the preferred size of this button. <p/> Overrides
     * <code>java.awt.Component#getPreferredSize().
     *
     * @return  the <code>Dimension</code> preferred by this button.
     *          This object can be safely reused after this call.
     */
    public Dimension getPreferredSize() {
        return new Dimension(prefSize);
    }

    // /**
    // * Get the preferred size of this button.
    // * <p/>
    // * Overrides <code>java.awt.Component#preferredSize().
    // *
    // * @return the <code>Dimension</code> preferred by this button.
    // * This object can be safely reused after this call.
    // * @deprecated
    // */
    // public Dimension preferredSize() {
    // return new Dimension( prefSize );
    // }

    /**
     * Set the preferred size of this button. If the requested value is less
     * than the minimum size of the button in either dimension, the minimum in
     * that dimension will be used instead.
     * 
     * @param size - the <code>Dimension</code> preferred by this button. This
     *            object can be safely reused after this call.
     */
    public void setPreferredSize(Dimension size) {
        setPreferredSize(size.width, size.height);
    }

    /**
     * Set the preferred size of this button. If the requested value is less
     * than the minimum size of the button in either dimension, the minimum in
     * that dimension will be used instead.
     * 
     * @param width - the <code>int</code> width preferred by this button.
     * @param height - the <code>int</code> height preferred by this button.
     */
    public void setPreferredSize(int width, int height) {

        // Get the minimum size of this button.
        Dimension minimum = getMinimumSize();

        // Use the greater of the two widths.
        if (minimum.width > width) {
            prefSize.width = minimum.width;
        } else {
            prefSize.width = width;
        }

        // Use the greater of the two heights.
        if (minimum.height > height) {
            prefSize.height = minimum.height;
        } else {
            prefSize.height = height;
        }

    }

}
