/*
 * MegaMek - Copyright (C) 2000-2002,2006 Ben Mazur (bmazur@sev.org)
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

package megamek.common.options;

import java.io.*;
import java.util.*;

public class OptionGroup implements IBasicOptionGroup, Serializable {
    static final long serialVersionUID = 6445683666789832313L;
    
    private Vector optionNames = new Vector();
    
    private String name;
    private String key;

    /**
     * Creates new OptionGroup
     * @param name group name
     * @param key optional key
     */
    public OptionGroup(String name, String key) {
        this.name = name;
        this.key = key;     
    }
    
    /**
     * Creates new OptionGroup with empty key
     * @param name option name
     */
    public OptionGroup(String name) {
      this(name, ""); //$NON-NLS-1$
    }

    public String getName() {
        return name;
    }
    
    public void setKey(String key) {
      this.key = key;
    }
    
    public String getKey() {
      return key;
    }
        
    public Enumeration getOptionNames() {
        return optionNames.elements();
    }
    
    /**
     * Adds new option name to this group. The option names are unique, 
     * so if there is already an option <code>optionName</code> this
     * function does nothing. 
     * @param optionName new option name
     */
    public void addOptionName(String optionName) {
        //This check is a performance penalty, but we don't 
        //allow duplicate option names 
        if (!optionNames.contains(optionName)) {
            optionNames.addElement(optionName);
        }
    }

}
