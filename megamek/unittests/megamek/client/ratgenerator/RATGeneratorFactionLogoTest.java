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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.universe.Faction2;
import megamek.common.universe.Factions2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

class RATGeneratorFactionLogoTest {
    private static final String DIRECTORY = "data/images/universe/factions/";

    @TempDir
    Path userDirectory;

    private final Map<String, Faction2> factionData = new HashMap<>();
    private MockedStatic<Factions2> factionsMock;
    private MockedStatic<PreferenceManager> preferencesMock;

    @BeforeEach
    void setUp() throws IOException {
        Factions2 factions = mock(Factions2.class);
        when(factions.getFaction(anyString())).thenAnswer(invocation ->
              Optional.ofNullable(factionData.get(invocation.getArgument(0, String.class))));
        factionsMock = mockStatic(Factions2.class);
        factionsMock.when(Factions2::getInstance).thenReturn(factions);
        ClientPreferences preferences = mock(ClientPreferences.class);
        when(preferences.getUserDir()).thenReturn(userDirectory.toString());
        preferencesMock = mockStatic(PreferenceManager.class);
        preferencesMock.when(PreferenceManager::getClientPreferences).thenReturn(preferences);

        registerFaction("""
              key: FS
              name: Federated Suns
              icon: logo_federated_suns.png
              """);
        registerFaction("""
              key: CGB
              name: Clan Ghost Bear
              tags: [CLAN]
              icon: logo_clan_ghost_bear.png
              iconChanges:
                3060: logo_rasalhague_dominion.png
              """);
        registerFaction("""
              key: CDS
              name: Clan Sea Fox
              tags: [CLAN]
              icon: logo_clan_sea_fox.png
              iconChanges:
                2984: logo_clan_diamond_sharks.png
                3100: logo_clan_sea_fox.png
              """);
        registerFaction("""
              key: FS.DAV
              name: Davion Guards
              """);
        registerFaction("""
              key: FS.DAV.1st
              name: 1st Davion Guards
              """);
        registerFaction("""
              key: FS.CRG
              name: Crucis Lancers
              icon: logo_crucis_lancers.png
              """);
        registerFaction("""
              key: FS.CRG.1st
              name: 1st Crucis Lancers
              """);
        registerFaction("""
              key: CGB.FRR
              name: Ghost Bear Rasalhague Galaxy
              """);
        registerFaction("""
              key: CLANLESS.GC
              name: Iconless Clan Command
              tags: [CLAN]
              """);
        registerFaction("""
              key: CLANLESS
              name: Iconless Clan
              tags: [CLAN]
              """);
        registerFaction("""
              key: MINORLESS
              name: Iconless Minor
              tags: [MINOR]
              icon: '  '
              """);
    }

    @AfterEach
    void tearDown() {
        preferencesMock.close();
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
          "3100, CDS, logo_clan_sea_fox"
    })
    void iconFromFactionDataHonorsYearChanges(int year, String code, String image) {
        String expected = DIRECTORY + image + ".png";

        assertEquals(expected, RATGenerator.getCanonicalFactionLogoAddress(year, code));
        assertEquals(expected, RATGenerator.getFactionLogoAddress(year, code));
    }

    @ParameterizedTest
    @CsvSource({
          "3025, FS.DAV, logo_federated_suns",
          "3025, FS.DAV.1st, logo_federated_suns",
          "3025, FS.CRG, logo_crucis_lancers",
          "3025, FS.CRG.1st, logo_crucis_lancers",
          "3059, CGB.FRR, logo_clan_ghost_bear",
          "3060, CGB.FRR, logo_rasalhague_dominion"
    })
    void commandWithoutIconInheritsFromParent(int year, String code, String image) {
        assertEquals(DIRECTORY + image + ".png", RATGenerator.getCanonicalFactionLogoAddress(year, code));
    }

    @ParameterizedTest
    @CsvSource({
          "3025, FS.UNLISTED, logo_federated_suns",
          "3025, FS.CRG.UNLISTED, logo_crucis_lancers",
          "3025, FS.UNLISTED.1st, logo_federated_suns",
          "3060, CGB.UNLISTED, logo_rasalhague_dominion"
    })
    void commandWithoutFactionDataInheritsFromParent(int year, String code, String image) {
        // Commands such as these exist only in the RAT generator data, which the availability panel lists
        assertEquals(DIRECTORY + image + ".png", RATGenerator.getCanonicalFactionLogoAddress(year, code));
    }

    @Test
    void commandWithoutFactionDataUnderUnknownFactionHasNoDedicatedArtwork() {
        assertNull(RATGenerator.getCanonicalFactionLogoAddress(3025, "CUSTOM.UNLISTED"));
    }

    @Test
    void commandWithoutIconInAnyParentFallsBackToGenericLogo() {
        assertNull(RATGenerator.getCanonicalFactionLogoAddress(3025, "CLANLESS.GC"));
        assertEquals(DIRECTORY + "logo_clan_generic.png", RATGenerator.getFactionLogoAddress(3025, "CLANLESS.GC"));
    }

    @Test
    void factionWithoutIconHasNoDedicatedArtwork() {
        assertNull(RATGenerator.getCanonicalFactionLogoAddress(3025, "CLANLESS"));
        assertNull(RATGenerator.getCanonicalFactionLogoAddress(3025, "MINORLESS"));
    }

    @Test
    void factionWithoutIconFallsBackToGenericLogo() {
        assertEquals(DIRECTORY + "logo_clan_generic.png", RATGenerator.getFactionLogoAddress(3025, "CLANLESS"));
        assertEquals(DIRECTORY + "logo_mercenaries.png", RATGenerator.getFactionLogoAddress(3025, "MINORLESS"));
    }

    @Test
    void unknownFactionHasNoDedicatedArtwork() {
        assertNull(RATGenerator.getCanonicalFactionLogoAddress(3025, "CUSTOM"));
    }

    @Test
    void userDirectoryIconTakesPrecedence() throws IOException {
        Path userIcon = writeUserImage("logo_crucis_lancers.png");

        assertEquals(userIcon.toFile(), RATGenerator.getFactionLogoFile(3025, "FS.CRG.1st"));
    }

    @Test
    void missingIconFallsBackToGenericLogo() {
        // logo_crucis_lancers.png is neither installed nor in the user directory
        assertEquals(new File(DIRECTORY + "logo_mercenaries.png"), RATGenerator.getFactionLogoFile(3025, "FS.CRG"));
    }

    @Test
    void userDirectoryGenericLogoTakesPrecedence() throws IOException {
        Path userGenericLogo = writeUserImage("logo_clan_generic.png");

        assertEquals(userGenericLogo.toFile(), RATGenerator.getFactionLogoFile(3025, "CLANLESS"));
        assertEquals(userGenericLogo.toFile(), RATGenerator.getGenericFactionLogoFile("CLANLESS"));
    }

    private Path writeUserImage(String fileName) throws IOException {
        Path path = userDirectory.resolve(DIRECTORY).resolve(fileName);
        Files.createDirectories(path.getParent());
        assertTrue(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), "png", path.toFile()));
        return path;
    }

    private void registerFaction(String yaml) throws IOException {
        Faction2 faction = new ObjectMapper(new YAMLFactory()).readValue(yaml, Faction2.class);
        factionData.put(faction.getKey(), faction);
    }
}
