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

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Abstract base class for Singletons representing Options' static information such as displayable name, description
 * etc. The derived classes must implement the Singleton pattern
 */
public class AbstractOptionsInfo implements IOptionsInfo {
    protected static final String GROUP_SUFFIX = ".group.";
    protected static final String OPTION_SUFFIX = ".option.";
    protected static final String DISPLAYABLE_NAME_SUFFIX = ".displayableName";
    protected static final String DESCRIPTION_SUFFIX = ".description";
    protected static final String TEXT_FIELD_LENGTH_SUFFIX = ".textFieldLength";
    protected static final String LABEL_BEFORE_TEXTFIELD_SUFFIX = ".labelBeforeTextField";

    /**
     * The OptionsInfo name that must be unique. Every instance of the AbstractOptionsInfo must have unique name, it's
     * used to query the NLS dependent information from the common resource bundle.
     *
     * @see #getOptionDisplayableName
     * @see #getGroupDisplayableName
     * @see #getOptionDescription
     */
    private final String name;

    /**
     * Hashtable of the <code>OptionInfo</code> used to store/find option info.
     */
    private final Hashtable<String, OptionInfo> optionsHash = new Hashtable<>();

    /**
     * List of option groups. The order of groups is important. The first group added by <code>addGroup</code> is the
     * first in the
     * <code>Enumeration</code> returned by <code>getGroups</code>
     */
    private final Vector<IBasicOptionGroup> groups = new Vector<>();

    /**
     * Flag that indicates that this filling the options info data is completed. <code>addGroup</code> and
     * <code>addOptionInfo</code> will have no effect if it's <code>true</code>
     *
     * @see #finish
     * @see #addGroup
     * @see #addOptionInfo
     */
    private boolean finished = false;

    /**
     * The <code>HashSet</code> used to check if the options info is already registered
     *
     * @see AbstractOptionsInfo()
     */
    private static final HashSet<String> names = new HashSet<>();

    /**
     * Protected constructor. It is called only by descendants. The name must be unique because it's used to query the
     * NLS dependent information from the resource bundle.
     *
     * @param name options info name
     */
    protected AbstractOptionsInfo(String name) {
        if (names.contains(name)) {
            throw new IllegalArgumentException("OptionsInfo '" + name + "' is already registered");
        }
        this.name = name;
        names.add(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.options.IOptionsInfo#getOptionInfo(java.lang.String)
     */
    @Override
    public IOptionInfo getOptionInfo(String name) {
        return optionsHash.get(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.options.IOptionsInfo#getGroups()
     */
    @Override
    public Enumeration<IBasicOptionGroup> getGroups() {
        return groups.elements();
    }

    IBasicOptionGroup addGroup(String name) {
        return addGroup(name, null);
    }

    IBasicOptionGroup addGroup(String name, String key) {
        IBasicOptionGroup group = null;
        if (!finished) {
            for (int i = 0; i < groups.size(); i++) {
                IBasicOptionGroup g = groups.elementAt(i);
                if (g != null && g.getName().equals(name)) {
                    group = groups.elementAt(i);
                    break;
                }
            }
            if (group == null) {
                group = (key == null ? new OptionGroup(name) : new OptionGroup(
                      name, key));
                groups.addElement(group);
            }
        }
        return group;
    }

    void addOptionInfo(IBasicOptionGroup group, String name) {
        if (!finished) {
            ((OptionGroup) group).addOptionName(name);
            setOptionInfo(name, new OptionInfo(name));
        }
    }

    /**
     * Returns the user-friendly NLS dependent name suitable for displaying in the options editor dialogs etc.
     *
     * @param groupName the group name which we want the display format
     *
     * @return group displayable name
     */
    protected String getGroupDisplayableName(String groupName) {
        for (int i = 0; i < groups.size(); i++) {
            IBasicOptionGroup g = groups.elementAt(i);
            if (g != null && g.getName().equals(groupName)) {
                return Messages.getString(name + GROUP_SUFFIX + groupName
                      + DISPLAYABLE_NAME_SUFFIX);
            }
        }
        return null;
    }

    /**
     * Records that filling of this structure is finished. <code>addGroup</code> and <code>addOptionInfo</code> will
     * have no effect after call of this function
     *
     * @see #addGroup
     * @see #addOptionInfo
     */
    void finish() {
        finished = true;
    }

    boolean finished() {
        return finished;
    }

    private void setOptionInfo(String name, OptionInfo info) {
        optionsHash.put(name, info);
    }

    private String getOptionDisplayableName(String optionName) {
        return Messages.getString(name + OPTION_SUFFIX + optionName + DISPLAYABLE_NAME_SUFFIX);
    }

    private String getOptionDescription(String optionName) {
        return Messages.getString(name + OPTION_SUFFIX + optionName + DESCRIPTION_SUFFIX);
    }

    private int getOptionTextFieldLength(String optionName, int defaultValue) {
        String key = name + OPTION_SUFFIX + optionName + TEXT_FIELD_LENGTH_SUFFIX;
        String value = Messages.getString(key);
        // Messages.getString returns the key itself if not found
        if (value.equals(key)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean getOptionLabelBeforeTextField(String optionName, boolean defaultValue) {
        String key = name + OPTION_SUFFIX + optionName + LABEL_BEFORE_TEXTFIELD_SUFFIX;
        String value = Messages.getString(key);
        // Messages.getString returns the key itself if not found
        if (value.equals(key)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Private model class to store the option info
     *
     * @see #addOptionInfo
     * @see #getOptionInfo
     */
    private class OptionInfo implements IOptionInfo {
        private final String name;

        public OptionInfo(String optionName) {
            this.name = optionName;
        }

        @Override
        public String getDisplayableName() {
            return getOptionDisplayableName(name);
        }

        @Override
        public String getDisplayableNameWithValue() {
            return getOptionDisplayableName(name);
        }

        @Override
        public String getDescription() {
            return getOptionDescription(name);
        }

        @Override
        public int getTextFieldLength() {
            return getOptionTextFieldLength(name, 3);
        }

        @Override
        public boolean isLabelBeforeTextField() {
            return getOptionLabelBeforeTextField(name, false);
        }
    }
}
