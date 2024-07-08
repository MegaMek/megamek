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
}
