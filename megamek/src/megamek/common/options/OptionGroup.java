/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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
import java.util.Enumeration;
import java.util.Vector;

public class OptionGroup implements IBasicOptionGroup, Serializable {

    @Serial
    private static final long serialVersionUID = 6445683666789832313L;

    private final Vector<String> optionNames = new Vector<>();

    private final String name;
    private String key;

    /**
     * Creates new OptionGroup
     *
     * @param name group name
     * @param key  optional key
     */
    public OptionGroup(String name, String key) {
        this.name = name;
        this.key = key;
    }

    /**
     * Creates new OptionGroup with empty key
     *
     * @param name option name
     */
    public OptionGroup(String name) {
        this(name, "");
    }

    @Override
    public String getName() {
        return name;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Enumeration<String> getOptionNames() {
        return optionNames.elements();
    }

    /**
     * Adds new option name to this group. The option names are unique, so if there is already an option
     * <code>optionName</code> this function does nothing.
     *
     * @param optionName new option name
     */
    public void addOptionName(String optionName) {
        // This check is a performance penalty, but we don't
        // allow duplicate option names
        if (!optionNames.contains(optionName)) {
            optionNames.addElement(optionName);
        }
    }

}
