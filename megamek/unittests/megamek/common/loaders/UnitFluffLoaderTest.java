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

package megamek.common.loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import megamek.common.units.EntityFluff;
import megamek.common.units.System;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UnitFluffLoaderTest {

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @ValueSource(strings = {
          "<p>Plain text with 'quotes', \"double quotes\", colons: and # hashes.</p>",
          "Trailing space ",
          " Leading space",
          "Two\nlines",
          "Tab\tinside",
          "Back\\slash",
          "- starts like a list",
          "Unicode: ÄÖÜ ß — “curly” \u2028 separator",
          "null",
          "123",
          "|"
    })
    void textsReadBackUnchanged(String text) throws IOException {
        EntityFluff fluff = new EntityFluff();
        fluff.setOverview(text);
        fluff.setCapabilities(text);
        fluff.setDeployment(text);
        fluff.setHistory(text);
        fluff.setManufacturer(text);
        fluff.setPrimaryFactory(text);
        fluff.setFluffDate(text);
        fluff.setSystemManufacturer(System.ENGINE, text);
        fluff.setSystemModel(System.ENGINE, text);

        EntityFluff readBack = roundTrip(fluff);

        assertEquals(text, readBack.getOverview());
        assertEquals(text, readBack.getCapabilities());
        assertEquals(text, readBack.getDeployment());
        assertEquals(text, readBack.getHistory());
        assertEquals(text, readBack.getManufacturer());
        assertEquals(text, readBack.getPrimaryFactory());
        assertEquals(text, readBack.getFluffDate());
        assertEquals(text, readBack.getSystemManufacturer(System.ENGINE));
        assertEquals(text, readBack.getSystemModel(System.ENGINE));
    }

    @Test
    void readableTextIsWrittenAsLiteralBlock() {
        EntityFluff fluff = new EntityFluff();
        fluff.setOverview("<p>The Atlas: an assault unit's.</p>");
        fluff.setSystemManufacturer(System.JUMP_JET, "Rawlings");

        String yaml = UnitFluffLoader.toYaml("Atlas", "AS7-D", fluff);

        assertEquals("""
              chassis: "Atlas"
              model: "AS7-D"
              overview: |-
                <p>The Atlas: an assault unit's.</p>
              systemManufacturers:
                JUMP_JET: "Rawlings"
              """, yaml);
    }

    @Test
    void onlyFileFluffIsWrittenAndRead() throws IOException {
        EntityFluff fluff = new EntityFluff();
        fluff.setNotes("Notes stay in the unit file");
        assertFalse(UnitFluffLoader.hasFileFluff(fluff));

        fluff.setFluffDate("2026-01-01 10:00:00");
        assertTrue(UnitFluffLoader.hasFileFluff(fluff));
        fluff.setSystemModel(System.ARMOR, "Durallex Heavy");

        EntityFluff readBack = roundTrip(fluff);
        assertEquals("", readBack.getNotes());
        assertEquals("2026-01-01 10:00:00", readBack.getFluffDate());
        assertEquals("Durallex Heavy", readBack.getSystemModel(System.ARMOR));
    }

    @Test
    void unknownSystemsAndEntriesAreIgnored() throws IOException {
        File file = tempDir.resolve("fluff.yml").toFile();
        Files.writeString(file.toPath(), """
              chassis: "Atlas"
              notes: "not read from fluff files"
              overview: "Read"
              systemManufacturers:
                FLUX_CAPACITOR: "Doc"
                jump jet: "Rawlings"
              """, StandardCharsets.UTF_8);

        EntityFluff fluff = UnitFluffLoader.readFluffFile(file);

        assertEquals("Read", fluff.getOverview());
        assertEquals("", fluff.getNotes());
        assertEquals("Rawlings", fluff.getSystemManufacturer(System.JUMP_JET));
    }

    @Test
    void invalidUUIDsAreNotLookedUp() {
        assertNull(UnitFluffLoader.externalFluff(null));
        assertNull(UnitFluffLoader.externalFluff("../../secret"));
        assertNull(UnitFluffLoader.externalFluff("not-a-uuid"));
    }

    @Test
    void ownFluffComesFirst() {
        EntityFluff own = new EntityFluff();
        own.setOverview("Own overview");
        own.setNotes("Own notes");
        EntityFluff external = new EntityFluff();
        external.setOverview("External overview");
        external.setHistory("External history");
        external.setNotes("External notes");
        external.setFluffDate("2026-01-01");

        EntityFluff merged = own.withFallback(external);

        assertEquals("Own overview", merged.getOverview());
        assertEquals("External history", merged.getHistory());
        assertEquals("Own notes", merged.getNotes());
        assertEquals("2026-01-01", merged.getFluffDate());
        assertEquals("Own overview", own.getOverview());
        assertEquals("", own.getHistory());
    }

    @Test
    void manufacturerAndFactoryAreTakenTogether() {
        EntityFluff own = new EntityFluff();
        own.setManufacturer("Own manufacturer");
        EntityFluff external = new EntityFluff();
        external.setManufacturer("External manufacturer|Second");
        external.setPrimaryFactory("External factory|Second factory");

        EntityFluff merged = own.withFallback(external);
        assertEquals("Own manufacturer", merged.getManufacturer());
        assertEquals("", merged.getPrimaryFactory());

        merged = new EntityFluff().withFallback(external);
        assertEquals("External manufacturer|Second", merged.getManufacturer());
        assertEquals("External factory|Second factory", merged.getPrimaryFactory());
    }

    @Test
    void systemManufacturerAndModelAreTakenTogether() {
        EntityFluff own = new EntityFluff();
        own.setSystemModel(System.ENGINE, "Own engine");
        EntityFluff external = new EntityFluff();
        external.setSystemManufacturer(System.ENGINE, "External maker");
        external.setSystemModel(System.ENGINE, "External engine");
        external.setSystemManufacturer(System.ARMOR, "External armor maker");

        EntityFluff merged = own.withFallback(external);

        assertEquals("", merged.getSystemManufacturer(System.ENGINE));
        assertEquals("Own engine", merged.getSystemModel(System.ENGINE));
        assertEquals("External armor maker", merged.getSystemManufacturer(System.ARMOR));
    }

    @Test
    void noFallbackKeepsOwnFluff() {
        EntityFluff own = new EntityFluff();
        assertSame(own, own.withFallback(null));
    }

    @Test
    void missingFluffDateFromOlderSavedGamesUsesTheFallback() throws ReflectiveOperationException {
        EntityFluff own = new EntityFluff();
        Field fluffDate = EntityFluff.class.getDeclaredField("fluffDate");
        fluffDate.setAccessible(true);
        fluffDate.set(own, null);
        EntityFluff external = new EntityFluff();
        external.setFluffDate("2026-01-01");

        assertEquals("2026-01-01", own.withFallback(external).getFluffDate());
    }

    private EntityFluff roundTrip(EntityFluff fluff) throws IOException {
        File file = tempDir.resolve("roundtrip.yml").toFile();
        Files.writeString(file.toPath(), UnitFluffLoader.toYaml("Chassis", "Model", fluff), StandardCharsets.UTF_8);
        return UnitFluffLoader.readFluffFile(file);
    }
}
