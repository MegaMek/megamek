/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

/**
 * Interface of the settable option. The settable option is used for game options which relate to game behavior and as
 * well as pilot options which relate to specific stats about a pilot, such as advantages. Game options are the same
 * across all clients. A settable option's primary purpose is to store a value for the option. Its secondary purpose is
 * to give the desired options dialog enough data to allow the user to set the option.
 *
 * @author Ben
 * @since April 26, 2002, 10:50 AM
 */
public interface IOption extends IBasicOption, IOptionInfo {

    /**
     * Boolean option type
     */
    int BOOLEAN = 0;

    /**
     * Integer option type
     */
    int INTEGER = 1;

    /**
     * Float option type
     */
    int FLOAT = 2;

    /**
     * String option type
     */
    int STRING = 3;

    /**
     * Choice option type
     */
    int CHOICE = 4;

    /**
     * Returns this option container - GameOptions, PilotOptions etc
     *
     * @return option container
     */
    IGameOptions getOwner();

    /**
     * Returns option type.
     *
     * @return option type
     *
     * @see IOption#BOOLEAN etc
     */
    int getType();

    /**
     * Returns default option value
     *
     * @return default option value
     */
    Object getDefault();

    /**
     * Return the value as the <code>boolean</code>
     */
    boolean booleanValue();

    /**
     * Return the value as the <code>int</code>
     */
    int intValue();

    /**
     * Return the value as the <code>float</code>
     */
    float floatValue();

    /**
     * Return the value as the <code>String</code>
     */
    String stringValue();

    /**
     * Sets the value
     *
     * @param value value to set
     */
    void setValue(Object value);

    /**
     * Sets the <code>String</code> value
     *
     * @param value value to set
     */
    void setValue(String value);

    /**
     * Sets the <code>boolean</code> value
     *
     * @param value value to set
     */
    void setValue(boolean value);

    /**
     * Sets the <code>int</code> value
     *
     * @param value value to set
     */
    void setValue(int value);

    /**
     * Sets the <code>float</code> value
     *
     * @param value value to set
     */
    void setValue(float value);

    /**
     * Resets the value to its "default" state
     */
    void clearValue();

}
