/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.common.autoresolve.acar;

public class Settings {
    public static final String longName = "Abstract Combat Auto Resolution";
    public static final String shortName = "ACAR";
    public static final String longVersion = "Alpha Strike 2025.01";
    public static final String shortVersion = "AS.2025.01";

    public static String getFullVersion() {
        return longName + " - " + longVersion;
    }

    public static String getShortVersion() {
        return shortName + " - " + shortVersion;
    }
}
