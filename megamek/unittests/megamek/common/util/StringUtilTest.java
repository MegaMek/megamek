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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringUtilTest {
    @Test
    void testWrapLinesShort() {
        String in = "-1 to-hit when making punches with hands.";
        String out = StringUtil.wrapLines(in, 85);
        assertEquals(in, out);
    }

    @Test
    void testWrapLinesOneSplit() {
        String s = StringUtil.wrapLines(
              "The unit is so common in the Clans that parts are easily available. -2 TN to locate replacement parts.",
              85);
        assertEquals("The unit is so common in the Clans that parts are easily available. -2 TN to locate\n" +
              "replacement parts.", s);
    }

    @Test
    void testWrapLinesLongWord() {
        String s = StringUtil.wrapLines(
              "This tooltip has a very long word. hippopotomonstrosesquippedaliophobia.",
              20);
        assertEquals("""
              This tooltip has a
              very long word.
              hippopotomonstrosesq
              uippedaliophobia.""", s);
    }

    @Test
    void testWrapLinesPreservesNewlines() {
        String s = StringUtil.wrapLines(
              "This tooltip has\nextra\nhippopotomonstrosesquippedaliophobia\nnewlines.",
              20);
        assertEquals("""
              This tooltip has
              extra
              hippopotomonstrosesq
              uippedaliophobia
              newlines.""", s);
    }
}
