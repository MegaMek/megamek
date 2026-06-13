/*
 * Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.options;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import megamek.common.annotations.Nullable;

/**
 * Parent class for options settings
 */
public abstract class AbstractOptions implements Serializable, IGameOptions {

    @Serial
    private static final long serialVersionUID = 6406883135074654379L;
    protected final Hashtable<String, IOption> optionsHash = new Hashtable<>();
    private static final Object lock = new Object();

    protected AbstractOptions() {
        if (!getOptionsInfoImp().finished()) {
            synchronized (lock) {
                initialize();
                getOptionsInfoImp().finish();
            }
        } else {
            initialize();
        }
    }

    protected abstract void initialize();

    public Map<String, IOption> getOptionMap() {
        return optionsHash;
    }

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

        for (Enumeration<IOptionGroup> i = getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if ((groupKey != null) && !group.getKey().equalsIgnoreCase(groupKey)) {
                continue;
            }

            for (Enumeration<IOption> j = group.getOptions(); j
                  .hasMoreElements(); ) {
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
