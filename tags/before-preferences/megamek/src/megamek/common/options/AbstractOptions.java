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

import java.io.*;
import java.util.*;


/**
 * Parent class for options settings
 */
public abstract class AbstractOptions implements IOptions, Serializable {
    
    private Hashtable optionsHash = new Hashtable();
    
    protected AbstractOptions() {
        optionsHash = new Hashtable();
        initialize();
        getOptionsInfoImp().finish();
    }

    protected abstract void initialize();
    
    public Enumeration getGroups() {
        return new GroupsEnumeration();
    }
    
    /* (non-Javadoc)
     * @see megamek.common.IOptions#getOptions()
     */
    public Enumeration getOptions() {
        return optionsHash.elements();
    }    
    
    
    /* (non-Javadoc)
     * @see megamek.common.IOptions#getOptionInfo(java.lang.String)
     */
    public IOptionInfo getOptionInfo(String name) {
        return getOptionsInfo().getOptionInfo(name);
    }
    
    public IOption getOption(String name) {
        return ((IOption)optionsHash.get(name));
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

    IOptionsInfo getOptionsInfo() {
        return getOptionsInfoImp();
    }

    protected abstract AbstractOptionsInfo getOptionsInfoImp();
    
    protected OptionGroup addGroup(String groupName) {
        return getOptionsInfoImp().addGroup(groupName);
    }

    protected OptionGroup addGroup(String groupName, String key) {
        return getOptionsInfoImp().addGroup(groupName, key);
    }

    protected void addOption(OptionGroup group, String name, String defaultValue) {
        addOption(group, name, IOption.STRING, defaultValue);
    }
    protected void addOption(OptionGroup group, String name, boolean defaultValue) {
        addOption(group, name, IOption.BOOLEAN, new Boolean(defaultValue));
    }
    protected void addOption(OptionGroup group, String name, int defaultValue) {
        addOption(group, name, IOption.INTEGER, new Integer(defaultValue));
    }
    protected void addOption(OptionGroup group, String name, float defaultValue) {
        addOption(group, name, IOption.FLOAT, new Float(defaultValue));
    }
    protected void addOption(OptionGroup group, String name, Vector defaultValue) {
        addOption(group, name, IOption.CHOICE, ""); //$NON-NLS-1$
    }

    protected void addOption (OptionGroup group, String name, int type, Object defaultValue) {
        optionsHash.put(name, new Option(this,name, type, defaultValue));
        getOptionsInfoImp().addOptionInfo(group, name);
    }

    protected class GroupsEnumeration implements Enumeration {

        private Enumeration groups;

        GroupsEnumeration() {
            groups = getOptionsInfo().getGroups();
        }

        /* (non-Javadoc)
         * @see java.util.Enumeration#hasMoreElements()
         */
        public boolean hasMoreElements() {
            return groups.hasMoreElements();
        }

        /* (non-Javadoc)
         * @see java.util.Enumeration#nextElement()
         */
        public Object nextElement() {
            return new GroupProxy((IBasicOptionGroup)groups.nextElement());
        }

        protected class GroupProxy implements IOptionGroup {

            private IBasicOptionGroup group;
            
            GroupProxy(IBasicOptionGroup group) {
                this.group = group;
            }

            public String getKey() {
                return group.getKey();
            }

            public String getName() {
                return group.getName();
            }

            public String getDisplayableName() {
                return getOptionsInfoImp().getGroupDisplayableName(group.getName());
            }

            public Enumeration getOptionNames() {
                return group.getOptionNames();
            }

            public Enumeration getOptions() {
                return new OptionsEnumeration();
            }

            protected class OptionsEnumeration implements Enumeration {

                private Enumeration optionNames;

                OptionsEnumeration() {
                    this.optionNames = group.getOptionNames();
                }

                /* (non-Javadoc)
                 * @see java.util.Enumeration#hasMoreElements()
                 */
                public boolean hasMoreElements() {
                    return optionNames.hasMoreElements();
                }

                /* (non-Javadoc)
                 * @see java.util.Enumeration#nextElement()
                 */
                public Object nextElement() {
                    return getOption((String)optionNames.nextElement());
                }        
                
            }

        }        
    }

}
