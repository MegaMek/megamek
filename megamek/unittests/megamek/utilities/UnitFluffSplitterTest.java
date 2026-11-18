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

package megamek.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.UnitFluffLoader;
import megamek.common.units.EntityFluff;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UnitFluffSplitterTest {

    private static final String UUID = "019f583e-c25b-7e1d-a2f6-f2b3763ac718";

    @TempDir
    Path tempDir;

    private Path unitDirectory;
    private Path fluffDirectory;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws IOException {
        unitDirectory = Files.createDirectories(tempDir.resolve("mekfiles"));
        fluffDirectory = tempDir.resolve("unitfluff");
    }

    @Test
    void splitMovesTheFluffAndSucceeds() throws IOException {
        Path unitFile = writeUnit("Locust LCT-1V.mtf", "uuid:" + UUID + "\n");

        assertTrue(split());

        String unitContent = Files.readString(unitFile, StandardCharsets.UTF_8);
        assertFalse(unitContent.contains("overview:"));
        assertFalse(unitContent.contains("manufacturer:"));
        assertTrue(unitContent.contains("chassis:Locust"));
        EntityFluff fluff = UnitFluffLoader.readFluffFile(fluffDirectory.resolve(UUID + ".yml").toFile());
        assertEquals("<p>A fast scout.</p>", fluff.getOverview());
        assertEquals("Bergan Industries", fluff.getManufacturer());
        assertEquals(List.of(unitFile), filesIn(unitDirectory));
    }

    @Test
    void unitWithoutUUIDIsLeftUnchangedAndFailsTheRun() throws IOException {
        Path unitFile = writeUnit("Locust LCT-1V.mtf", "");
        String original = Files.readString(unitFile, StandardCharsets.UTF_8);

        assertFalse(split());

        assertEquals(original, Files.readString(unitFile, StandardCharsets.UTF_8));
        assertEquals(List.of(), filesIn(fluffDirectory));
    }

    @Test
    void unitsSharingAUUIDAreLeftUnchangedAndFailTheRun() throws IOException {
        Path first = writeUnit("Locust LCT-1V.mtf", "uuid:" + UUID + "\n");
        Path second = writeUnit("Locust LCT-1V copy.mtf", "uuid:" + UUID + "\n");
        String original = Files.readString(first, StandardCharsets.UTF_8);

        assertFalse(split());

        assertEquals(original, Files.readString(first, StandardCharsets.UTF_8));
        assertEquals(original, Files.readString(second, StandardCharsets.UTF_8));
        assertEquals(List.of(), filesIn(fluffDirectory));
    }

    private boolean split() throws IOException {
        var output = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        return new UnitFluffSplitter(unitDirectory, fluffDirectory, false, output).run();
    }

    private Path writeUnit(String fileName, String uuidLine) throws IOException {
        Path unitFile = unitDirectory.resolve(fileName);
        Files.writeString(unitFile, locust(uuidLine), StandardCharsets.UTF_8);
        return unitFile;
    }

    private static List<Path> filesIn(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.sorted().toList();
        }
    }

    private static String locust(String uuidLine) {
        return uuidLine + """
              chassis:Locust
              model:LCT-1V
              mul id:1901

              Config:Biped
              techbase:Inner Sphere
              era:2499
              source:TR:3039
              rules level:1

              mass:20
              engine:160 Fusion Engine
              structure:IS Standard
              myomer:Standard

              heat sinks:10 Single
              walk mp:8
              jump mp:0

              armor:Standard(Inner Sphere)
              LA armor:4
              RA armor:4
              LT armor:8
              RT armor:8
              CT armor:10
              HD armor:8
              LL armor:8
              RL armor:8
              RTL armor:2
              RTR armor:2
              RTC armor:2

              Weapons:3
              Machine Gun, Left Arm
              Machine Gun, Right Arm
              Medium Laser, Center Torso

              Left Arm:
              Shoulder
              Upper Arm Actuator
              Machine Gun
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-

              Right Arm:
              Shoulder
              Upper Arm Actuator
              Machine Gun
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-

              Left Torso:
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-

              Right Torso:
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-

              Center Torso:
              Fusion Engine
              Fusion Engine
              Fusion Engine
              Gyro
              Gyro
              Gyro
              Gyro
              Fusion Engine
              Fusion Engine
              Fusion Engine
              Medium Laser
              IS Ammo MG - Full

              Head:
              Life Support
              Sensors
              Cockpit
              -Empty-
              Sensors
              Life Support
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-

              Left Leg:
              Hip
              Upper Leg Actuator
              Lower Leg Actuator
              Foot Actuator
              Heat Sink
              Heat Sink
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-

              Right Leg:
              Hip
              Upper Leg Actuator
              Lower Leg Actuator
              Foot Actuator
              Heat Sink
              Heat Sink
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-
              -Empty-

              overview:<p>A fast scout.</p>

              manufacturer:Bergan Industries

              primaryfactory:Alshain
              """;
    }
}
