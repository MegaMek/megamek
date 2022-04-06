/*
 * Copyright (C) 2020 - The MegaMek Team. All Rights Reserved
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.enums;

import megamek.client.generator.RandomGenderGenerator;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Author's Note: This is for Biological Gender (strictly speaking, the term is Sex) only,
 * with the two OTHER-? flags being implemented here for MekHQ usage.
 */
public enum Gender {
    //region Enum Declarations
    MALE(false, "Male"),
    FEMALE(false, "Female"),
    OTHER_MALE(true, "Male"),
    OTHER_FEMALE(true, "Female"),
    RANDOMIZE(true);
    //endregion Enum Declarations

    //region Variable Declarations
    private final boolean internal;
    private final String displayName;
    //endregion Variable Declarations

    //region Constructors
    Gender(boolean internal) {
        this(internal, "");
    }

    Gender(boolean internal, String displayName) {
        this.internal = internal;
        this.displayName = displayName;
    }
    //endregion Constructors

    //region Boolean Checks
    /**
     * @return true is the person's biological gender is male, otherwise false
     */
    public boolean isMale() {
        return (this == MALE) || (this == OTHER_MALE);
    }

    /**
     * @return true is the person's biological gender is female, otherwise false
     */
    public boolean isFemale() {
        return (this == FEMALE) || (this == OTHER_FEMALE);
    }

    /**
     * @return true if the enum value can be shown externally
     */
    public boolean isExternal() {
        return !internal;
    }

    /**
     * @return true if the enum value is only for internal use
     */
    public boolean isInternal() {
        return internal;
    }
    //endregion Boolean Checks

    /**
     * @return a list of all external-facing gender options
     */
    public static List<Gender> getExternalOptions() {
        List<Gender> externalOptions = new ArrayList<>();

        for (Gender gender : values()) {
            if (gender.isExternal()) {
                externalOptions.add(gender);
            }
        }

        return externalOptions;
    }

    /**
     * @return the external form of the internal gender
     */
    public Gender getExternalVariant() {
        return (this == OTHER_MALE) ? MALE : FEMALE;
    }

    /**
     * @return the internal form of the external gender
     */
    public Gender getInternalVariant() {
        return (this == MALE) ? OTHER_MALE : OTHER_FEMALE;
    }

    /**
     * @param input the string to parse
     * @return the gender defined by the input, or a randomly generated string if the string isn't a
     * proper value
     */
    public static Gender parseFromString(String input) {
        try {
            return valueOf(input);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(input)) {
                case 0:
                    return MALE;
                case 1:
                    return FEMALE;
                case -1:
                default:
                    return RandomGenderGenerator.generate();
            }
        } catch (Exception ignored) {

        }

        LogManager.getLogger().error("Failed to parse the gender value from input String " + input
                        + ". Returning a newly generated gender.");
        return RandomGenderGenerator.generate();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
