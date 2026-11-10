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
package megamek.server;

import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.codeUtilities.StringUtility;

/**
 * Describes a connection the server turned away because the joining client runs a different version.
 *
 * <p>The server tells every player already in the game about it, so they know a player was refused rather than
 * finding out only when that player complains. Only the name and versions go to the players; the address is kept for
 * the server log.</p>
 *
 * @param playerName    the name the joining player asked for, or {@code null} when their client is too old to send
 *                      one along with its version
 * @param address       the network address the connection came from, without the leading slash Java adds
 * @param clientVersion the version the joining player is running
 * @param serverVersion the version the server is running
 *
 * @author Illiani
 * @since 0.51.01
 */
public record RefusedConnection(@Nullable String playerName, String address, Version clientVersion,
      Version serverVersion) {

    /**
     * The longest player name passed on to the host. A name is only ever a few words; anything past this is cut,
     * so a hostile client cannot fill the host's screen with one.
     */
    static final int MAXIMUM_PLAYER_NAME_LENGTH = 64;

    /**
     * Builds the notice from what the server received, tidying the raw values on the way in.
     *
     * @param rawPlayerName the name object sent alongside the version, which may be absent or not a string at all
     * @param rawAddress    the address as the connection reports it, such as {@code /203.0.113.7}
     * @param clientVersion the version the joining player is running
     * @param serverVersion the version the server is running
     *
     * @return the notice to pass to the host
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static RefusedConnection fromReceived(@Nullable Object rawPlayerName, @Nullable String rawAddress,
          Version clientVersion, Version serverVersion) {
        return new RefusedConnection(cleanPlayerName(rawPlayerName), cleanAddress(rawAddress), clientVersion,
              serverVersion);
    }

    /**
     * @return {@code true} if the joining client told us who they are, otherwise {@code false}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean hasPlayerName() {
        return playerName != null;
    }

    /**
     * Reduces the name a client sent to something safe to show: a trimmed, single-line string of sensible length, or
     * {@code null} when there is nothing usable. Used by the server on receipt, and again by each client before it
     * shows the name, since neither end should trust what came over the wire.
     *
     * @param rawPlayerName the name object sent alongside the version
     *
     * @return the cleaned name, or {@code null} when none was sent
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable String cleanPlayerName(@Nullable Object rawPlayerName) {
        if (!(rawPlayerName instanceof String nameText)) {
            return null;
        }

        final String singleLineName = nameText.replaceAll("\\s+", " ").trim();
        if (StringUtility.isNullOrBlank(singleLineName)) {
            return null;
        }

        if (singleLineName.length() > MAXIMUM_PLAYER_NAME_LENGTH) {
            return singleLineName.substring(0, MAXIMUM_PLAYER_NAME_LENGTH);
        }

        return singleLineName;
    }

    /**
     * @param rawAddress the address as the connection reports it, such as {@code /203.0.113.7}
     *
     * @return the address without Java's leading slash, or {@code "Unknown"} when there is none
     *
     * @author Illiani
     * @since 0.51.01
     */
    static String cleanAddress(@Nullable String rawAddress) {
        if (StringUtility.isNullOrBlank(rawAddress)) {
            return "Unknown";
        }

        final String trimmedAddress = rawAddress.trim();
        return trimmedAddress.startsWith("/") ? trimmedAddress.substring(1) : trimmedAddress;
    }
}
