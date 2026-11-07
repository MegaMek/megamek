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
package megamek.common.net.marshalling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ObjectInputFilter;
import java.io.ObjectInputFilter.FilterInfo;
import java.io.ObjectInputFilter.Status;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;

import megamek.common.Player;
import megamek.common.net.packets.Packet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Characterization tests for the deserialization allow list. The filter is the only guard between the socket and
 * {@code ObjectInputStream}, so its exact behaviour - including the places where it is looser than it reads - is
 * pinned here before it is hardened.
 */
class SanityInputFilterTest {

    private static final SanityInputFilter FILTER = new SanityInputFilter();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Status check(Class<?> serialClass) {
        FilterInfo info = mock(FilterInfo.class);
        when(info.serialClass()).thenReturn((Class) serialClass);
        return FILTER.checkInput(info);
    }

    @Test
    void coreTypesAreAllowed() {
        assertEquals(Status.ALLOWED, check(String.class));
        assertEquals(Status.ALLOWED, check(Integer.class));
        assertEquals(Status.ALLOWED, check(Boolean.class));
        assertEquals(Status.ALLOWED, check(ArrayList.class));
        assertEquals(Status.ALLOWED, check(Object[].class));
        assertEquals(Status.ALLOWED, check(int[].class));
    }

    @Test
    void anyMegaMekClassIsAllowed() {
        // The "megamek.*" pattern admits the entire application classpath, not just packets and game state.
        assertEquals(Status.ALLOWED, check(Packet.class));
        assertEquals(Status.ALLOWED, check(Player.class));
        assertEquals(Status.ALLOWED, check(SanityInputFilter.class));
    }

    @Test
    void classesNotOnTheListAreRejected() {
        assertEquals(Status.REJECTED, check(Date.class));
        assertEquals(Status.REJECTED, check(ProcessBuilder.class));
        assertEquals(Status.REJECTED, check(Thread.class));
    }

    @Test
    void aNullClassIsLeftUndecided() {
        // Depth, array-length and reference checks call the filter with a null class; it does not limit any of them.
        FilterInfo info = mock(FilterInfo.class);
        when(info.serialClass()).thenReturn(null);
        when(info.depth()).thenReturn(1_000_000L);
        when(info.arrayLength()).thenReturn(1_000_000_000L);

        assertEquals(Status.UNDECIDED, FILTER.checkInput(info));
    }

    @ParameterizedTest
    @ValueSource(classes = { StringBuilder.class, AbstractMap.SimpleEntry.class })
    void patternsMatchAsUnanchoredPrefixesSoNeighbouringClassesSlipThrough(Class<?> serialClass) {
        // "java\.lang\.String" is matched with find(), so java.lang.StringBuilder is admitted although it is not
        // listed; likewise the AbstractMap pattern admits AbstractMap$SimpleEntry. Recorded, not endorsed.
        assertEquals(Status.ALLOWED, check(serialClass));
    }

    @Test
    void theFilterImplementsTheStandardInterface() {
        // BasicGameTest installs it process-wide with ObjectInputFilter.Config.setSerialFilter; keep that possible.
        ObjectInputFilter asStandard = FILTER;
        assertSame(FILTER, asStandard);
    }
}
