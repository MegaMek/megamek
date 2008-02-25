/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.preference;

import java.util.EventObject;

public class PreferenceChangeEvent extends EventObject {

    /**
     * 
     */
    private static final long serialVersionUID = 8514064293889126724L;
    protected String name;
    protected Object oldValue;
    protected Object newValue;

    public PreferenceChangeEvent(Object source, String name, Object oldValue,
            Object newValue) {
        super(source);
        this.name = name;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getName() {
        return name;
    }

    public Object getOldvalue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }
}
