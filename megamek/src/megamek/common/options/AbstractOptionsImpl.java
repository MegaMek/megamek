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

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * Parent class for options settings
 */
public abstract class AbstractOptionsImpl implements Serializable, IGameOptions {

    @Serial
    private static final long serialVersionUID = 6406883135074654379L;
    private final Hashtable<String, IOption> optionsHash = new Hashtable<>();

    protected AbstractOptionsImpl() {
        initialize();
        getOptionsInfoImp().finish();
    }

    protected abstract void initialize();

    @Override
    public int count() {
        return count(null);
    }

    @Override
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

    @Override
    public String getOptionList(String separator) {
        return getOptionListString(separator, null);
    }

    @Override
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
                    if (!listBuilder.isEmpty()) {
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

    @Override
    public Enumeration<IOptionGroup> getGroups() {
        return new GroupsEnumeration();
    }

    @Override
    public Enumeration<IOption> getOptions() {
        return optionsHash.elements();
    }

    @Override
    public Collection<IOption> getOptionsList() {
        return Collections.unmodifiableCollection(optionsHash.values());
    }

    @Override
    public IOptionInfo getOptionInfo(String name) {
        return getOptionsInfo().getOptionInfo(name);
    }

    @Nullable
    @Override
    public IOption getOption(String name) {
        return optionsHash.get(name);
    }

    @Override
    public boolean booleanOption(String name) {
        IOption opt = getOption(name);
        if (opt == null) {
            return false;
        } else {
            return opt.booleanValue();
        }
    }

    @Override
    public int intOption(String name) {
        return getOption(name).intValue();
    }

    @Override
    public float floatOption(String name) {
        return getOption(name).floatValue();
    }

    @Override
    public String stringOption(String name) {
        return getOption(name).stringValue();
    }

    @Override
    public IOptionsInfo getOptionsInfo() {
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

        private final Enumeration<IBasicOptionGroup> groups;

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

            private final IBasicOptionGroup group;

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
