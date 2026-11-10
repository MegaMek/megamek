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
package megamek.client.ui.dialogs.releaseNotes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

class ReleaseNotesDialogTest {

    @Test
    void htmlEscapesTitlesAndLinksPullRequests() {
        ReleaseNotes notes = new ReleaseNotes("megamek", URI.create("https://github.com/MegaMek/megamek/releases"),
              null, List.of(new ReleaseNotes.Section("Bug Fixes", List.of(
                    new ReleaseNotes.Change("Fix <b>bold</b> & 'quotes'", 12,
                          URI.create("https://github.com/MegaMek/megamek/pull/12")),
                    new ReleaseNotes.Change("Written by hand", 0, null)))));

        String html = ReleaseNotesDialog.html(notes);

        assertTrue(html.contains("<h3>Bug Fixes</h3>"));
        assertTrue(html.contains("<li>Fix &lt;b&gt;bold&lt;/b&gt; &amp; &#39;quotes&#39; "
              + "<a href='https://github.com/MegaMek/megamek/pull/12'>#12</a></li>"));
        assertTrue(html.contains("<li>Written by hand</li>"));
        assertFalse(html.contains("<b>"));
    }

    @Test
    void onlyMegaMekPagesOnGitHubAreSafe() {
        assertTrue(ReleaseNotesDialog.isSafe("https://github.com/MegaMek/megamek/pull/12"));
        assertFalse(ReleaseNotesDialog.isSafe(null));
        assertFalse(ReleaseNotesDialog.isSafe("https://example.com/MegaMek/"));
        assertFalse(ReleaseNotesDialog.isSafe("http://github.com/MegaMek/megamek"));
        assertFalse(ReleaseNotesDialog.isSafe("https://github.com/MegaMek/../evil"));
        assertFalse(ReleaseNotesDialog.isSafe("https://github.com/MegaMek/x@evil.example"));
    }

    @Test
    void productNamesFollowTheRepository() {
        assertEquals("MegaMek", ReleaseNotesDialog.productName("megamek"));
        assertEquals("MegaMekLab", ReleaseNotesDialog.productName("megameklab"));
        assertEquals("MekHQ", ReleaseNotesDialog.productName("mekhq"));
    }
}
