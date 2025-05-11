/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.ai.neuralnetwork;

/**
 * FailedToParseFeatureNormalizationLine is a custom exception that is thrown when there is an error parsing a line
 * from the feature normalization CSV file.
 * @author Luana Coppio
 */
public class FailedToParseFeatureNormalizationLine extends RuntimeException {
    // This makes it easily accessible the error message for the I18n class
    public static final String BUNDLE_NAME = "megamek.ai.neuralnetwork.error";

    /**
     * Creates a new FailedToParseFeatureNormalizationLine exception with the given message and throwable.
     * @param message the error message
     * @param throwable the cause of the error
     */
    public FailedToParseFeatureNormalizationLine(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Creates a new FailedToParseFeatureNormalizationLine exception with the given message.
     * @param message the error message
     */
    public FailedToParseFeatureNormalizationLine(String message) {
        super(message);
    }
}
