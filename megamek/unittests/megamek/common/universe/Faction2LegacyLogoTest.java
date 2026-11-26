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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

/**
 * A faction with only a MekHQ {@code logo:} line gets the generic logo in MegaMek, which reads only {@code icon:}.
 * These tests cover how such factions are found for the load-time warning (issue #9298).
 */
class Faction2LegacyLogoTest {

    @Test
    void customFactionWithOnlyMekHQLogoIsFlagged() throws IOException {
        Faction2 faction = parseFaction("""
              key: MYFAC
              name: My Custom Faction
              logo: Mercenaries/my_custom_faction.png
              """);

        assertTrue(faction.hasLegacyLogoWithoutIcon());
    }

    @Test
    void customFactionWithOnlyMekHQLogoChangesIsFlagged() throws IOException {
        Faction2 faction = parseFaction("""
              key: MYFAC
              name: My Custom Faction
              logoChanges:
                3050: Mercenaries/my_custom_faction.png
              """);

        assertTrue(faction.hasLegacyLogoWithoutIcon());
    }

    @Test
    void blankIconDoesNotCountAsAnIcon() throws IOException {
        Faction2 faction = parseFaction("""
              key: MYFAC
              name: My Custom Faction
              icon: '  '
              logo: Mercenaries/my_custom_faction.png
              """);

        assertTrue(faction.hasLegacyLogoWithoutIcon());
    }

    @Test
    void factionWithIconAndMekHQLogoIsNotFlagged() throws IOException {
        Faction2 faction = parseFaction("""
              key: MYFAC
              name: My Custom Faction
              icon: logo_my_custom_faction.png
              logo: Mercenaries/my_custom_faction.png
              """);

        assertFalse(faction.hasLegacyLogoWithoutIcon());
    }

    @Test
    void factionWithIconChangesAndMekHQLogoIsNotFlagged() throws IOException {
        Faction2 faction = parseFaction("""
              key: MYFAC
              name: My Custom Faction
              iconChanges:
                3050: logo_my_custom_faction.png
              logo: Mercenaries/my_custom_faction.png
              """);

        assertFalse(faction.hasLegacyLogoWithoutIcon());
    }

    @Test
    void factionWithNoLogoAtAllIsNotFlagged() throws IOException {
        Faction2 faction = parseFaction("""
              key: TFR
              name: Tikonov Free Republic
              """);

        // Such a faction has no artwork anywhere, so there is nothing for the user to fix
        assertFalse(faction.hasLegacyLogoWithoutIcon());
    }

    @Test
    void onlyFlaggedFactionsAreListedInCodeOrder() throws IOException {
        List<Faction2> factions = List.of(
              parseFaction("""
                    key: ZED
                    name: Zed Custom
                    logo: Mercenaries/zed.png
                    """),
              parseFaction("""
                    key: FS
                    name: Federated Suns
                    icon: logo_federated_suns.png
                    """),
              parseFaction("""
                    key: ABC
                    name: Abc Custom
                    logo: Mercenaries/abc.png
                    """));

        assertEquals(List.of("ABC", "ZED"), Factions2.findFactionsWithLegacyLogoOnly(factions));
    }

    private static Faction2 parseFaction(String yaml) throws IOException {
        return new ObjectMapper(new YAMLFactory()).readValue(yaml, Faction2.class);
    }
}
