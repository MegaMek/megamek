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

package megamek.common;

import java.io.*;
import java.util.*;

import megamek.common.options.*;

/**
 * Parent class for options settings
 *
 * @author Ben
 */
public abstract class Options implements Serializable {
    
    private Vector groups = new Vector();
    private Vector allOptions = new Vector();
    private Hashtable optionsHash = new Hashtable();
    
    public abstract void initialize();
    
    protected void addGroup(OptionGroup group) {
        groups.addElement(group);
    }
    
    public Enumeration groups() {
        return groups.elements();
    }
    
    protected void addOption(OptionGroup group, GameOption option) {
        group.addOption(option);
        allOptions.addElement(option);
        optionsHash.put(option.getShortName(), option);
    }
    
    public GameOption getOption(String name) {
        return (GameOption)optionsHash.get(name);
    }
    
    public boolean booleanOption(String name) {
        return getOption(name).booleanValue();
    }
    
    public int intOption(String name) {
        return getOption(name).intValue();
    }
    
    public float floatOption(String name) {
        return getOption(name).floatValue();
    }

    public String stringOption(String name) {
        return getOption(name).stringValue();
    }
}
