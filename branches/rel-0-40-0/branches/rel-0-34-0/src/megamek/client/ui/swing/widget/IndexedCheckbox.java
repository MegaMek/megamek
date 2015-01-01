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

package megamek.client.ui.swing.widget;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;

public class IndexedCheckbox extends JCheckBox {
    /**
     * 
     */
    private static final long serialVersionUID = 5224809762401860469L;
    private int index;

    public IndexedCheckbox(String label, boolean state, ButtonGroup group,
            int index) {
        super(label, state);
        group.add(this);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
