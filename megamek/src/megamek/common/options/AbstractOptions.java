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

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 * Parent class for options settings
 */
public abstract class AbstractOptions implements IOptions, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 6406883135074654379L;
    private Hashtable<String, IOption> optionsHash = new Hashtable<String, IOption>();

    protected AbstractOptions() {
        initialize();
        getOptionsInfoImp().finish();
    }

    protected abstract void initialize();

    /**
     * Returns a count of all options in this object.
     * @return Option count.
     */
    public int count() { 
        return count(null);
    }
    
    /**
     * Returns a count of all options in this object with the given group key.
     * @param groupKey the group key to filter on. Null signifies to return all options indiscriminately.
     * @return Option count.
     */
    public int count(String groupKey) {
        int count = 0;
        
        for (Enumeration<IOptionGroup> i = getGroups(); i
                .hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            if ((groupKey != null) && !group.getKey().equalsIgnoreCase(groupKey)) {
                continue;
            }
            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements(); ) {
                IOption option = j.nextElement();

                if (null != option && option.booleanValue()) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    /**
     * Returns a string of all the quirk "codes" for this entity, using sep as
     * the separator
     * @param separator The separator to insert between codes, in addition to a space
     */
    public String getOptionList(String separator) {
        return getOptionListString(separator, null);
    }
    
    /**
     * Returns a string of all the quirk "codes" for this entity, using sep as
     * the separator, filtering on a specific group key.
     * @param separator The separator to insert between codes, in addition to a space
     * @param groupKey The group key to use to filter options. Null signifies to return all options indiscriminately.
     */
    public String getOptionListString(String separator, String groupKey) {
        StringBuffer listBuilder = new StringBuffer();
        
        if (null == separator) {
            separator = "";
        }

        for (Enumeration<IOptionGroup> i = getGroups(); i.hasMoreElements();) {
            IOptionGroup group = i.nextElement();
            
            if ((groupKey != null) && !group.getKey().equalsIgnoreCase(groupKey)) {
                continue;
            }
            
            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements();) {
                IOption option = j.nextElement();
                if (option != null && option.booleanValue()) {
                    if (listBuilder.length() > 0) {
                        listBuilder.append(separator);
                    }
                    listBuilder.append(option.getName());
                    if ((option.getType() == IOption.STRING)
                            || (option.getType() == IOption.CHOICE)
                            || (option.getType() == IOption.INTEGER)) {
                        listBuilder.append(" ").append(option.stringValue());
                    }
                }
            }
        }
        return listBuilder.toString();
    }
    
    public Enumeration<IOptionGroup> getGroups() {
        return new GroupsEnumeration();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IOptions#getOptions()
     */
    public Enumeration<IOption> getOptions() {
        return optionsHash.elements();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.IOptions#getOptionInfo(java.lang.String)
     */
    public IOptionInfo getOptionInfo(String name) {
        return getOptionsInfo().getOptionInfo(name);
    }

    public IOption getOption(String name) {
        return optionsHash.get(name);
    }

    public boolean booleanOption(String name) {
        IOption opt = getOption(name);
        if (opt == null){
            return false;
        } else {
            return opt.booleanValue();
        }
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

    protected IBasicOptionGroup addGroup(String groupName) {
        return getOptionsInfoImp().addGroup(groupName);
    }

    protected IBasicOptionGroup addGroup(String groupName, String key) {
        return getOptionsInfoImp().addGroup(groupName, key);
    }

    protected void addOption(IBasicOptionGroup group, String name,
            String defaultValue) {
        addOption(group, name, IOption.STRING, defaultValue);
    }

    protected void addOption(IBasicOptionGroup group, String name,
            boolean defaultValue) {
        addOption(group, name, IOption.BOOLEAN, new Boolean(defaultValue));
    }

    protected void addOption(IBasicOptionGroup group, String name,
            int defaultValue) {
        addOption(group, name, IOption.INTEGER, Integer.valueOf(defaultValue));
    }

    protected void addOption(IBasicOptionGroup group, String name,
            float defaultValue) {
        addOption(group, name, IOption.FLOAT, new Float(defaultValue));
    }

    protected void addOption(IBasicOptionGroup group, String name, Vector<String> defaultValue) {
        addOption(group, name, IOption.CHOICE, ""); //$NON-NLS-1$
    }

    protected void addOption(IBasicOptionGroup group, String name, int type,
            Object defaultValue) {
        optionsHash.put(name, new Option(this, name, type, defaultValue));
        getOptionsInfoImp().addOptionInfo(group, name);
    }

    protected class GroupsEnumeration implements Enumeration<IOptionGroup> {

        private Enumeration<IBasicOptionGroup> groups;

        GroupsEnumeration() {
            groups = getOptionsInfo().getGroups();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Enumeration#hasMoreElements()
         */
        public boolean hasMoreElements() {
            return groups.hasMoreElements();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Enumeration#nextElement()
         */
        public IOptionGroup nextElement() {
            return new GroupProxy(groups.nextElement());
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
                return getOptionsInfoImp().getGroupDisplayableName(
                        group.getName());
            }

            public Enumeration<String> getOptionNames() {
                return group.getOptionNames();
            }

            public Enumeration<IOption> getOptions() {
                return new OptionsEnumeration();
            }

            public Enumeration<IOption> getSortedOptions() {
                OptionsEnumeration oe = new OptionsEnumeration();
                oe.sortOptions();
                return oe;
            }

            protected class OptionsEnumeration implements Enumeration<IOption> {

                private Enumeration<String> optionNames;

                OptionsEnumeration() {
                    optionNames = group.getOptionNames();
                }

                /*
                 * (non-Javadoc)
                 *
                 * @see java.util.Enumeration#hasMoreElements()
                 */
                public boolean hasMoreElements() {
                    return optionNames.hasMoreElements();
                }

                /*
                 * (non-Javadoc)
                 *
                 * @see java.util.Enumeration#nextElement()
                 */
                public IOption nextElement() {
                    return getOption(optionNames.nextElement());
                }

                public void sortOptions() {
                    List<String> names = Collections.list(optionNames);
                    Collections.sort(names);
                    optionNames = Collections.enumeration(names);
                }

            }

        }
    }

}
