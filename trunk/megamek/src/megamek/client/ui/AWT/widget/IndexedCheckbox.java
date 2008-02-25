/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

import java.awt.Checkbox;
import java.awt.CheckboxGroup;

public class IndexedCheckbox extends Checkbox {
    /**
     * 
     */
    private static final long serialVersionUID = -8591352017935140738L;
    private int index;

    public IndexedCheckbox(String label, boolean state, CheckboxGroup group,
            int index) {
        super(label, state, group);

        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
