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
package megamek.common.util;

import java.security.SecureRandom;
import java.util.UUID;

public final class UUIDUtil {
    private static final long MAX_TIMESTAMP = 0xFFFFFFFFFFFFL;
    private static final long MAX_RANDOM_HIGH = 0xFFFL;
    private static final long MAX_RANDOM_LOW = 0x3FFFFFFFFFFFFFFFL;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static long lastTimestamp = -1;
    private static long randomHigh;
    private static long randomLow;

    private UUIDUtil() {
    }

    /**
     * Creates a monotonically increasing UUID version 7 as defined by RFC 9562.
     *
     * @return a new UUID version 7
     */
    public static synchronized UUID newUUIDv7() {
        long currentTimestamp = System.currentTimeMillis() & MAX_TIMESTAMP;
        if (currentTimestamp > lastTimestamp) {
            lastTimestamp = currentTimestamp;
            randomHigh = RANDOM.nextLong() & MAX_RANDOM_HIGH;
            randomLow = RANDOM.nextLong() & MAX_RANDOM_LOW;
        } else {
            incrementRandomPayload();
        }

        long mostSignificantBits = (lastTimestamp << 16)
              | 0x7000L
              | randomHigh;
        long leastSignificantBits = randomLow
              | 0x8000000000000000L;
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    private static void incrementRandomPayload() {
        if (randomLow < MAX_RANDOM_LOW) {
            randomLow++;
        } else if (randomHigh < MAX_RANDOM_HIGH) {
            randomHigh++;
            randomLow = 0;
        } else {
            if (lastTimestamp == MAX_TIMESTAMP) {
                throw new IllegalStateException("UUID version 7 timestamp space exhausted");
            }
            lastTimestamp++;
            randomHigh = RANDOM.nextLong() & MAX_RANDOM_HIGH;
            randomLow = RANDOM.nextLong() & MAX_RANDOM_LOW;
        }
    }
}