/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.enums;

import java.util.ArrayList;
import java.util.List;

import megamek.client.generator.RandomGenderGenerator;
import megamek.logging.MMLogger;

/**
 * In this context, sex relates to the character's capacity to incubate offspring. While this is a very limited view of
 * the broad spectrum of human genders, the needs of programming dictate that we need to take a more binary view of
 * things. To this end, males can't birth children, females can. 'Other' genders are used for characters that fall
 * outside the gender binary, with the sex following 'other' determining their capacity to birth children.
 */
public enum Gender {
    // region Enum Declarations
    MALE(false, "Male"),
    FEMALE(false, "Female"),
    OTHER_MALE(true, "Other (M)"),
    OTHER_FEMALE(true, "Other (F)"),
    RANDOMIZE(true);
    // endregion Enum Declarations

    // region Variable Declarations
    private final boolean internal;
    private final String displayName;
    // endregion Variable Declarations

    // region Constructors
    Gender(boolean internal) {
        this(internal, "");
    }

    Gender(boolean internal, String displayName) {
        this.internal = internal;
        this.displayName = displayName;
    }
    // endregion Constructors

    // region Boolean Checks

    /**
     * @return true if the person's biological gender is male, otherwise false
     */
    public boolean isMale() {
        return (this == MALE) || (this == OTHER_MALE);
    }

    /**
     * @return true if the person's biological gender is female, otherwise false
     */
    public boolean isFemale() {
        return (this == FEMALE) || (this == OTHER_FEMALE);
    }

    /**
     * @return {@code true} if the person uses gender-neutral pronouns, otherwise {@code false}.
     */
    public boolean isGenderNeutral() {
        return (this == OTHER_MALE) || (this == OTHER_FEMALE);
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
    // endregion Boolean Checks

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
     *
     * @return the gender defined by the input, or a randomly generated string if the string isn't a proper value
     */
    public static Gender parseFromString(String input) {
        try {
            return valueOf(input);
        } catch (Exception ignored) {

        }

        try {
            return switch (Integer.parseInt(input)) {
                case 0 -> MALE;
                case 1 -> FEMALE;
                case 2 -> OTHER_MALE;
                case 3 -> OTHER_FEMALE;
                default -> RandomGenderGenerator.generate();
            };
        } catch (Exception ignored) {

        }

        MMLogger.create(Gender.class)
              .error("Failed to parse the gender value from input String {}. Returning a newly generated gender.",
                    input);
        return RandomGenderGenerator.generate();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
