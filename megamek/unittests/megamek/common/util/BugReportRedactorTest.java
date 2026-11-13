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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests that the bug report redactor removes the player's folders, login name, computer name and IP addresses, and
 * leaves ordinary log text alone.
 */
class BugReportRedactorTest {

    private static final String WINDOWS_HOME = "C:\\Users\\Dave Smith";
    private static final String LOGIN_NAME = "dsmith";
    private static final String HOST_NAME = "DESKTOP-AB12CD";

    private static BugReportRedactor windowsRedactor() {
        return new BugReportRedactor(WINDOWS_HOME, LOGIN_NAME, List.of(HOST_NAME));
    }

    private static BugReportRedactor noMachineRedactor() {
        return new BugReportRedactor(null, null, List.of());
    }

    // Paths

    @Test
    void windowsHomeFolderIsReplacedInAStackTraceLine() {
        String line = "java.io.FileNotFoundException: C:\\Users\\Dave Smith\\mekhq\\data\\x.mtf (Access is denied)";

        assertEquals("java.io.FileNotFoundException: [home]\\mekhq\\data\\x.mtf (Access is denied)",
              windowsRedactor().redact(line));
    }

    @Test
    void homeFolderMatchesForwardSlashesEscapedBackslashesAndOtherCase() {
        BugReportRedactor redactor = windowsRedactor();

        assertEquals("[home]/mekhq", redactor.redact("C:/Users/Dave Smith/mekhq"));
        assertEquals("[home]\\\\mekhq", redactor.redact("C:\\\\Users\\\\Dave Smith\\\\mekhq"));
        assertEquals("[home]\\mekhq", redactor.redact("c:\\users\\dave smith\\mekhq"));
    }

    @Test
    void shortHomeFolderDoesNotEatTheStartOfALongerName() {
        BugReportRedactor redactor = new BugReportRedactor("/home/al", null, List.of());

        assertEquals("[home]/megamek", redactor.redact("/home/al/megamek"));
        // Not "[home]ice": the profile rule still hides the other name, but whole.
        assertEquals("/home/[user]/megamek", redactor.redact("/home/alice/megamek"));
    }

    @Test
    void anyProfileFolderIsReplacedOnEveryPlatform() {
        BugReportRedactor redactor = noMachineRedactor();

        assertEquals("C:\\Users\\[user]\\Documents\\mm", redactor.redact("C:\\Users\\Jane Doe\\Documents\\mm"));
        assertEquals("/Users/[user]/Library/megamek", redactor.redact("/Users/jdoe/Library/megamek"));
        assertEquals("/home/[user]/games", redactor.redact("/home/jdoe/games"));
        assertEquals("C:\\Documents and Settings\\[user]\\x", redactor.redact("C:\\Documents and Settings\\jdoe\\x"));
    }

    @Test
    void profileFolderAtTheEndOfASentenceKeepsTheRestOfTheSentence() {
        assertEquals("Saved to C:\\Users\\[user] and closed",
              noMachineRedactor().redact("Saved to C:\\Users\\Bob and closed"));
    }

    @Test
    void loginNameIsReplacedAsAFolderButNotInsideOrdinaryText() {
        BugReportRedactor redactor = windowsRedactor();

        assertEquals("D:\\[user]\\Games\\MegaMek", redactor.redact("D:\\dsmith\\Games\\MegaMek"));
        assertEquals("Loaded dsmith's Lance", redactor.redact("Loaded dsmith's Lance"));
    }

    // Host names

    @Test
    void hostNameIsReplacedButALongerNameIsNot() {
        BugReportRedactor redactor = windowsRedactor();

        assertEquals("s: hostname = '[host]' port = 2346",
              redactor.redact("s: hostname = 'DESKTOP-AB12CD' port = 2346"));
        assertEquals("DESKTOP-AB12CDE", redactor.redact("DESKTOP-AB12CDE"));
    }

    @Test
    void fullHostNameIsReplacedWholeBeforeItsShortForm() {
        BugReportRedactor redactor = new BugReportRedactor(null, null, List.of("dave-pc", "dave-pc.lan"));

        assertEquals("[host]/[ip]", redactor.redact("dave-pc.lan/192.168.1.20"));
    }

    @Test
    void localhostIsNeverTreatedAsAHostName() {
        BugReportRedactor redactor = new BugReportRedactor(null, null, List.of("localhost"));

        assertEquals("Connecting to localhost:2346", redactor.redact("Connecting to localhost:2346"));
    }

    // IP addresses

    @Test
    void privateAndPublicIpv4AddressesAreReplaced() {
        BugReportRedactor redactor = noMachineRedactor();

        assertEquals("s: hosting on address = [ip]", redactor.redact("s: hosting on address = 10.0.0.134"));
        assertEquals("[ip], [ip]", redactor.redact("172.19.176.1, 203.0.113.9"));
        assertEquals("ends at [ip].", redactor.redact("ends at 10.0.0.1."));
    }

    @Test
    void loopbackAnyAddressAndVersionNumbersAreKept() {
        BugReportRedactor redactor = noMachineRedactor();
        String text = "127.0.0.1 0.0.0.0 MegaMek v0.50.12 Java 21.0.5 build 1.2.3.4.5 bad 999.1.1.1";

        assertEquals(text, redactor.redact(text));
    }

    @Test
    void graphicsDriverVersionThatLooksLikeAnAddressIsKept() {
        String line = "    GPU: Old Card (1 GB VRAM, driver 6.14.10.13)";

        assertEquals(line, noMachineRedactor().redact(line));
    }

    @Test
    void ipv6AddressesAreReplaced() {
        BugReportRedactor redactor = noMachineRedactor();

        assertEquals("[ip]", redactor.redact("fe80::1c2d:3e4f:5a6b:7c8d%12"));
        assertEquals("addr [ip] up", redactor.redact("addr 2001:0db8:85a3:0000:0000:8a2e:0370:7334 up"));
        assertEquals("::ffff:[ip]", redactor.redact("::ffff:10.1.2.3"));
    }

    @Test
    void timestampsMethodReferencesAndIpv6LoopbackAreKept() {
        BugReportRedactor redactor = noMachineRedactor();
        String text = "21:20:07,123 INFO Foo::bar ::1 deadbeef 12:34:56";

        assertEquals(text, redactor.redact(text));
    }

    // Counting

    @Test
    void countsWhatWasReplaced() {
        BugReportRedactor redactor = windowsRedactor();

        redactor.redact("""
              C:\\Users\\Dave Smith\\mekhq
              C:\\Users\\Other\\mekhq
              s: hostname = 'DESKTOP-AB12CD'
              10.0.0.134 fe80::1
              """);

        assertEquals(2, redactor.pathCount());
        assertEquals(1, redactor.hostNameCount());
        assertEquals(2, redactor.ipAddressCount());
    }

    @Test
    void textWithNothingToHideIsReturnedUnchanged() {
        String text = "Princess moved Atlas AS7-D to hex 0512; MegaMek v0.50.12\r\n$1 \\ done";

        assertEquals(text, windowsRedactor().redact(text));
    }
}
