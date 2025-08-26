/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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


package megamek;

/**
 * These are constants related to logging messages to for output.
 */
public final class MMLoggingConstants {
    private MMLoggingConstants() {
        throw new IllegalStateException("MMLoggingConstants");
    }

    // region General Constants
    public static final String UNHANDLED_EXCEPTION = "Uncaught %s detected. Please open up an issue containing all logs, the game save file, and customs at https://github.com/MegaMek/megamek/issues . If Sentry is enabled, the error has also been logged.";
    public static final String UNHANDLED_EXCEPTION_TITLE = "Uncaught Exception %s";
    public static final String VERSION_PARSE_FAILURE = "Version Parsing Failure";
    // endregion General Constants

    // region Argument Parser
    public static final String AP_INCORRECT_ARGUMENTS = "Incorrect arguments: %s%n%s";
    // endregion Argument Parser

    // region Starting Servers/Clients
    public static final String SC_STARTING_DEDICATED_SERVER = "Starting Dedicated Server. {}";
    public static final String SC_STARTING_HOST_SERVER = "Starting Host Server. {}";
    public static final String SC_STARTING_CLIENT_SERVER = "Starting Client Server. {}";
    // endregion Starting Servers/Clients

    // region Version Constants
    public static final String VERSION_ERROR_CANNOT_PARSE_VERSION_FROM_STRING = "Cannot parse the version from %s. This may lead to severe issues that cannot be otherwise explained.";
    public static final String VERSION_ILLEGAL_VERSION_FORMAT = "Version text %s is in an illegal version format. Versions should be in the format 'release.major.minor-SNAPSHOT', with the snapshot being an optional inclusion. This may lead to severe issues that cannot be otherwise explained.";
    public static final String VERSION_FAILED_TO_PARSE_RELEASE = "Failed to parse the release value from Version text %s. This may lead to severe issues that cannot be otherwise explained.";
    public static final String VERSION_FAILED_TO_PARSE_MAJOR = "Failed to parse the major value from Version text %s. This may lead to severe issues that cannot be otherwise explained.";
    public static final String VERSION_FAILED_TO_PARSE_MINOR = "Failed to parse the minor value from Version text %s. This may lead to severe issues that cannot be otherwise explained.";
    // endregion Version Constants
}
