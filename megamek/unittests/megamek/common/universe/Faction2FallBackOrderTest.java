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
package megamek.common.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A faction's fallback factions are tried in the order its file lists them, so loading must keep that order. It used
 * to come back in hash order: the Raven Alliance lists Clan Snow Raven before the Outworlds Alliance, but loaded with
 * the Outworlds Alliance first, so the force generator gave it the Outworlds rules and no navy (MegaMek/mekhq#10376
 * item 22).
 */
class Faction2FallBackOrderTest {

    @TempDir
    Path factionsDirectory;

    @Test
    void fallBackFactionsKeepTheOrderTheFileGives() throws Exception {
        Files.writeString(factionsDirectory.resolve("RA_test.yml"), """
              key: RA
              name: Raven Alliance
              yearsActive:
                - start: 3083
              ratingLevels:
                - Front Line
              fallBackFactions:
                - CSR
                - OA
                - CLAN.IS
              """, StandardCharsets.UTF_8);

        Optional<Faction2> ravenAlliance = new Factions2(factionsDirectory.toString()).getFaction("RA");

        assertTrue(ravenAlliance.isPresent());
        assertEquals(List.of("CSR", "OA", "CLAN.IS"), new ArrayList<>(ravenAlliance.get().getFallBackFactions()));
    }
}
