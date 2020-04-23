/*
 * Copyright (C) 2020 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.generator;

import megamek.common.Compute;
import megamek.common.enums.Gender;

/**
 * Author's Note: This is for generating Biological Gender (strictly speaking, the term is Sex) only,
 * with the other-style generation being implemented for future gender identity use
 */
public class RandomGenderGenerator {
    //region Variable Declarations
    private static int percentFemale = 50;
    //endregion Variable Declarations

    //region Constructors
    public RandomGenderGenerator() {

    }
    //endregion Constructors

    //region Getters/Setters
    public static int getPercentFemale() {
        return percentFemale;
    }

    public static void setPercentFemale(int i) {
        percentFemale = i;
    }
    //endregion Getters/Setters

    //region Generators
    /**
     * Generates a random gender based on the default percent female
     * @return the gender generated, of either Gender.MALE or Gender.FEMALE
     */
    public static Gender generate() {
        return generate(getPercentFemale());
    }

    /**
     * @param percentFemale the specified percent female to generate with
     * @return the gender generated, of either Gender.MALE or Gender.FEMALE
     */
    public static Gender generate(int percentFemale) {
        return (Compute.randomInt(100) < percentFemale) ? Gender.FEMALE : Gender.MALE;
    }

    /**
     * Generates a random biological gender based on the default percent female for MekHQ Gender Identity Flagged Personnel
     * @return the gender generated, of either Gender.OTHER_MALE or Gender.OTHER_FEMALE
     */
    public static Gender generateOther() {
        return generateOther(getPercentFemale());
    }

    /**
     * This is used to generate the biological gender for the MekHQ Gender Identity Flagged Personnel
     * @param percentFemale the specified percent female to generate with
     * @return the gender generated, of either Gender.MALE or Gender.FEMALE
     */
    public static Gender generateOther(int percentFemale) {
        return (Compute.randomInt(100) < percentFemale) ? Gender.OTHER_FEMALE : Gender.OTHER_MALE;
    }
    //endregion Generators
}
