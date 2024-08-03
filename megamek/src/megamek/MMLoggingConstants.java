/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
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
  public static final String SC_STARTING_DEDICATED_SERVER = "Starting Dedicated Server. %s";
  public static final String SC_STARTING_HOST_SERVER = "Starting Host Server. %s";
  public static final String SC_STARTING_CLIENT_SERVER = "Starting Client Server. %s";
  // endregion Starting Servers/Clients

  // region Version Constants
  public static final String VERSION_ERROR_CANNOT_PARSE_VERSION_FROM_STRING = "Cannot parse the version from %s. This may lead to severe issues that cannot be otherwise explained.";
  public static final String VERSION_ILLEGAL_VERSION_FORMAT = "Version text %s is in an illegal version format. Versions should be in the format 'release.major.minor-SNAPSHOT', with the snapshot being an optional inclusion. This may lead to severe issues that cannot be otherwise explained.";
  public static final String VERSION_FAILED_TO_PARSE_RELEASE = "Failed to parse the release value from Version text %s. This may lead to severe issues that cannot be otherwise explained.";
  public static final String VERSION_FAILED_TO_PARSE_MAJOR = "Failed to parse the major value from Version text %s. This may lead to severe issues that cannot be otherwise explained.";
  public static final String VERSION_FAILED_TO_PARSE_MINOR = "Failed to parse the minor value from Version text %s. This may lead to severe issues that cannot be otherwise explained.";
  // endregion Version Constants
}
