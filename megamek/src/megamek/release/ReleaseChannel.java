/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
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
package megamek.release;

import java.util.Locale;

import jakarta.annotation.Nullable;

/**
 * The suite release channel a build was published on. Core is the stable line players get by default; Experimental
 * is the opt-in line built from {@code main}. Both channels share one version number sequence per line, so the channel
 * cannot be derived from a version and is stamped into the packaged {@code Version.properties} by release builds.
 */
public enum ReleaseChannel {
    CORE("core"),
    EXPERIMENTAL("experimental");

    private final String key;

    ReleaseChannel(final String key) {
        this.key = key;
    }

    /**
     * @return the lowercase name used in {@code Version.properties}, suite records and save files
     */
    public String getKey() {
        return key;
    }

    /**
     * Parses a channel key as written by {@link #getKey()}, ignoring case and surrounding whitespace.
     *
     * @param key the key to parse; may be {@code null}
     *
     * @return the matching channel, or {@code null} when the key is absent, blank or unknown
     */
    public static @Nullable ReleaseChannel fromKey(final @Nullable String key) {
        if (key == null) {
            return null;
        }
        final String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (ReleaseChannel channel : values()) {
            if (channel.key.equals(normalized)) {
                return channel;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return key;
    }
}
