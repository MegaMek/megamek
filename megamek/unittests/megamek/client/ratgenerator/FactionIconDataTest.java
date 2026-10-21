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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import megamek.MMConstants;
import megamek.common.universe.Faction2;
import megamek.common.universe.Factions2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

/**
 * Checks the faction icons of the real faction data, so that changes to the data keep the historical logo
 * transitions and only name icon files that exist.
 */
class FactionIconDataTest {
    private static Factions2 factions;
    private static MockedStatic<Factions2> factionsMock;

    @BeforeAll
    static void setUp() {
        factions = new Factions2(MMConstants.FACTIONS_DIR);
        factionsMock = mockStatic(Factions2.class);
        factionsMock.when(Factions2::getInstance).thenReturn(factions);
    }

    @AfterAll
    static void tearDown() {
        factionsMock.close();
    }

    @ParameterizedTest
    @CsvSource({
          "3025, FS, logo_federated_suns",
          "3059, CGB, logo_clan_ghost_bear",
          "3060, CGB, logo_rasalhague_dominion",
          "2983, CDS, logo_clan_sea_fox",
          "2984, CDS, logo_clan_diamond_sharks",
          "3099, CDS, logo_clan_diamond_sharks",
          "3100, CDS, logo_clan_sea_fox",
          "3025, MBA, logo_clan_sea_fox",
          "3025, CGS, logo_clan_goliath_scorpion",
          "3080, CEI, logo_escorpion_imperio",
          "3150, SE, logo_escorpion_imperio",
          "3025, MERC, logo_mercenaries",
          "3025, IND, logo_security_forces",
          "3025, PIR, logo_pirates",
          "3025, PSI, logo_pirates",
          "3025, MRB, logo_comstar",
          "3025, SLIE, logo_star_league"
    })
    void factionDataKeepsLogosAndHistoricalTransitions(int year, String code, String image) {
        String expected = RATGenerator.FACTION_LOGO_DIRECTORY + image + ".png";

        assertEquals(expected, RATGenerator.getCanonicalFactionLogoAddress(year, code));
        assertEquals(expected, RATGenerator.getFactionLogoAddress(year, code));
    }

    @Test
    void unknownFactionHasNoDedicatedArtwork() {
        assertNull(RATGenerator.getCanonicalFactionLogoAddress(3025, "CUSTOM"));
    }

    @Test
    void allFactionIconsExist() {
        List<String> missingIcons = new ArrayList<>();
        for (Faction2 faction : factions.getFactions()) {
            List<String> icons = new ArrayList<>(faction.getIconChanges().values());
            icons.add(faction.getIcon());
            for (String icon : icons) {
                if ((icon != null) && !new File(RATGenerator.FACTION_LOGO_DIRECTORY, icon).isFile()) {
                    missingIcons.add(faction.getKey() + ": " + icon);
                }
            }
        }

        assertTrue(missingIcons.isEmpty(), "Missing faction icons: " + missingIcons);
    }
}
