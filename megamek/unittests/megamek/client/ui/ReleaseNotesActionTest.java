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
package megamek.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.Version;
import org.junit.jupiter.api.Test;

class ReleaseNotesActionTest {

    @Test
    void versionWithRevisionOpensItsReleasePage() {
        Version version = new Version("0", "51", "1", "5", null);

        assertEquals("https://github.com/MegaMek/mekhq/releases/tag/v0.51.01.5",
              ReleaseNotesAction.releaseNotesUrl("mekhq", version));
    }

    @Test
    void versionWithoutRevisionOpensTheReleaseList() {
        Version version = new Version("0", "52", "0", null, null);

        assertEquals("https://github.com/MegaMek/megamek/releases",
              ReleaseNotesAction.releaseNotesUrl("megamek", version));
    }

    @Test
    void buildInformationIsNotPartOfTheTag() {
        Version version = new Version("0", "51", "1", "5", "main-abc1234");

        assertEquals("https://github.com/MegaMek/megameklab/releases/tag/v0.51.01.5",
              ReleaseNotesAction.releaseNotesUrl("megameklab", version));
    }
}
