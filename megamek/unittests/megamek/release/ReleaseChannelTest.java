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
 */
package megamek.release;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ReleaseChannelTest {
    @Test
    void parsesKeysIgnoringCaseAndWhitespace() {
        assertEquals(ReleaseChannel.CORE, ReleaseChannel.fromKey("core"));
        assertEquals(ReleaseChannel.EXPERIMENTAL, ReleaseChannel.fromKey(" Experimental "));
    }

    @Test
    void rejectsAbsentBlankAndUnknownKeys() {
        assertNull(ReleaseChannel.fromKey(null));
        assertNull(ReleaseChannel.fromKey(" "));
        assertNull(ReleaseChannel.fromKey("stable"));
    }

    @Test
    void keysRoundTrip() {
        for (ReleaseChannel channel : ReleaseChannel.values()) {
            assertEquals(channel, ReleaseChannel.fromKey(channel.getKey()));
            assertEquals(channel.getKey(), channel.toString());
        }
    }
}
