/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.options;

import megamek.common.annotations.Nullable;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 * Parent class for options settings
 */
public abstract class AbstractOptions implements Serializable {
    private static final long serialVersionUID = 6406883135074654379L;
    private Hashtable<String, IOption> optionsHash = new Hashtable<>();

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
        
        for (Enumeration<IOptionGroup> i = getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            if ((groupKey != null) && !group.getKey().equalsIgnoreCase(groupKey)) {
                continue;
            }
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
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
        StringBuilder listBuilder = new StringBuilder();
        
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

    /**
     * Returns the <code>Enumeration</code> of the option groups in thioptions container.
     *
     * @return <code>Enumeration</code> of the <code>IOptionGroup</code>
     */
    public Enumeration<IOptionGroup> getGroups() {
        return new GroupsEnumeration();
    }

    /**
     * Returns the <code>Enumeration</code> of the options in this options container. The order of
     * options is not specified.
     *
     * @return <code>Enumeration</code> of the <code>IOption</code>
     */
    public Enumeration<IOption> getOptions() {
        return optionsHash.elements();
    }

    /**
     * Returns the UI specific data to allow the user to set the option
     *
     * @param name option name
     * @return UI specific data
     * @see IOptionInfo
     */
    public IOptionInfo getOptionInfo(String name) {
        return getOptionsInfo().getOptionInfo(name);
    }

    /**
     * Returns the option by name or <code>null</code> if there is no such option
     *
     * @param name option name
     * @return the option or <code>null</code> if there is no such option
     */
    public @Nullable IOption getOption(String name) {
        return optionsHash.get(name);
    }

    /**
     * Returns the value of the desired option as the <code>boolean</code>
     *
     * @param name option name
     * @return the value of the desired option as the <code>boolean</code>
     */
    public boolean booleanOption(String name) {
        IOption opt = getOption(name);
        if (opt == null) {
            return false;
        } else {
            return opt.booleanValue();
        }
    }

    /**
     * Returns the value of the desired option as the <code>int</code>
     *
     * @param name option name
     * @return the value of the desired option as the <code>int</code>
     */
    public int intOption(String name) {
        return getOption(name).intValue();
    }

    /**
     * Returns the value of the desired option as the <code>float</code>
     *
     * @param name option name
     * @return the value of the desired option as the <code>float</code>
     */
    public float floatOption(String name) {
        return getOption(name).floatValue();
    }

    /**
     * Returns the value of the desired option as the <code>String</code>
     *
     * @param name option name
     * @return the value of the desired option as the <code>String</code>
     */
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
        addOption(group, name, IOption.BOOLEAN, defaultValue);
    }

    protected void addOption(IBasicOptionGroup group, String name,
            int defaultValue) {
        addOption(group, name, IOption.INTEGER, defaultValue);
    }

    protected void addOption(IBasicOptionGroup group, String name,
            float defaultValue) {
        addOption(group, name, IOption.FLOAT, defaultValue);
    }

    protected void addOption(IBasicOptionGroup group, String name, Vector<String> defaultValue) {
        addOption(group, name, IOption.CHOICE, "");
    }

    protected void addOption(IBasicOptionGroup group, String name, int type, Object defaultValue) {
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
        @Override
        public boolean hasMoreElements() {
            return groups.hasMoreElements();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Enumeration#nextElement()
         */
        @Override
        public IOptionGroup nextElement() {
            return new GroupProxy(groups.nextElement());
        }

        protected class GroupProxy implements IOptionGroup {

            private IBasicOptionGroup group;

            GroupProxy(IBasicOptionGroup group) {
                this.group = group;
            }

            @Override
            public String getKey() {
                return group.getKey();
            }

            @Override
            public String getName() {
                return group.getName();
            }

            @Override
            public String getDisplayableName() {
                return getOptionsInfoImp().getGroupDisplayableName(
                        group.getName());
            }

            @Override
            public Enumeration<String> getOptionNames() {
                return group.getOptionNames();
            }

            @Override
            public Enumeration<IOption> getOptions() {
                return new OptionsEnumeration();
            }

            @Override
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
                @Override
                public boolean hasMoreElements() {
                    return optionNames.hasMoreElements();
                }

                /*
                 * (non-Javadoc)
                 *
                 * @see java.util.Enumeration#nextElement()
                 */
                @Override
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
