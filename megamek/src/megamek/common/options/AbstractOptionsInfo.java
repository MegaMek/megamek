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

package megamek.common.options;

import java.util.Enumeration;


import com.sun.java.util.collections.Hashtable;


/**
 * Abstract base class for Singletons representing Options' static 
 * information such as displayable name, description etc.
 * The derived classes must implement the Singleton pattern  
 */

public abstract class AbstractOptionsInfo implements IOptionsInfo {
    
    private Hashtable optionsHash = new Hashtable();

    private Hashtable groups = new Hashtable();

    private boolean finished;

    public IOptionInfo getOptionInfo(String name) {
        return ((IOptionInfo)optionsHash.get(name));
    }

    public Enumeration getGroups() {
        return groups.elements();
    }

    OptionGroup addGroup(String name) {
        return addGroup(name,null); 
    }

    OptionGroup addGroup(String name, String key) {
        OptionGroup group = null;
        if (!finished) {
            group = (OptionGroup)groups.get(name);
            if (group == null) {
                group = key == null? new OptionGroup(name):new OptionGroup(name, key);
                groups.put(name,group);
            }
        }
        return group;
    }

    void addOptionInfo(OptionGroup group, String name, String displayableName, String description) {
        if (!finished) {
            group.addOptionName(name);
            setOptionInfo(name, new OptionInfo(displayableName, description));
        }
    }

    void setOptionInfo(String name, IOptionInfo info) {
        optionsHash.put(name, info);
    }
    
    void finish() {
        finished = true;
    }
    
    protected static class OptionInfo implements IOptionInfo {

        private String displayableName;
        
        private String description;

        private int textFieldLength = 2;
        
        private boolean labelBeforeTextField = false;
        
        public OptionInfo(String displayableName, String description) {
            this.displayableName = displayableName;
            this.description = description;
        }

        public String getDisplayableName() {
            return displayableName;
        }

        public String getDescription() {
            return description;
        }

        public int getTextFieldLength() {
            return textFieldLength;
        }

        public boolean isLabelBeforeTextField() {
            return labelBeforeTextField;
        }
                
    }
    
}
