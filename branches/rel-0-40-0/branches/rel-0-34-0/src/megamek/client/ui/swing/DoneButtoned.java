/*
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

package megamek.client.ui.swing;

import javax.swing.JButton;

/**
 * Classes that implement this interface define a "Done" button that they do not
 * display. Windows that show objects of this type should retrieve this "Done"
 * button and display it at an appropriate location.
 */
public interface DoneButtoned {

    /**
     * Retrieve the "Done" button of this object.
     * 
     * @return the <code>javax.swing.JButton</code> that activates this
     *         object's "Done" action.
     */
    public JButton getDoneButton();
}
