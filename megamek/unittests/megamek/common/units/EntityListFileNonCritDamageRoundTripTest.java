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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import megamek.common.CriticalSlot;
import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.loaders.MULParser;
import megamek.common.loaders.MekFileParser;
import megamek.common.weapons.autoCannons.ACWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Round-trips the two non-crit-slot combat damage flags through the MUL/entity-XML that MekHQ uses to transfer scenario
 * results: a CORE-rules autocannon that has taken its first (non-destroying) critical hit (issue #9761), and a
 * destroyed Directional Torso Mount that locks a still-firing weapon's arc (issue #9762). Neither is a crit-slot hit,
 * so without dedicated serialization they are silently dropped when a scenario is resolved from an exported MUL.
 */
class EntityListFileNonCritDamageRoundTripTest {

    @TempDir
    Path tempDir;

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
    }

    /** An Atlas AS7-D carries a ten-slot AC/20, exercising the once-per-mount write for a multi-slot weapon. */
    private Entity createAtlas() throws Exception {
        Entity entity = new MekFileParser(new File("testresources/data/mekfiles/Atlas AS7-D.mtf")).getEntity();
        entity.setGame(game);
        entity.setId(game.getNextEntityId());
        entity.setOwner(game.getPlayer(0));
        return entity;
    }

    private WeaponMounted autocannon(Entity entity) {
        return entity.getWeaponList().stream()
              .filter(weapon -> weapon.getType() instanceof ACWeapon)
              .findFirst()
              .orElseThrow(() -> new AssertionError("Test unit has no autocannon"));
    }

    private Entity roundTrip(Entity entity) throws Exception {
        File file = tempDir.resolve("non-crit-damage.mul").toFile();
        EntityListFile.saveTo(file, new ArrayList<>(List.of(entity)), true);

        MULParser parser = new MULParser(file, null);
        Vector<Entity> parsed = parser.getEntities();
        assertEquals(1, parsed.size(), "expected exactly one entity back from the MUL");
        return parsed.firstElement();
    }

    @Test
    @DisplayName("a first-crit autocannon flag survives a MUL round trip without becoming a crit-slot hit")
    void autocannonHitRoundTrips() throws Exception {
        Entity atlas = createAtlas();
        WeaponMounted ac = autocannon(atlas);
        ac.setAutocannonHit(true);

        int acEquipmentNum = atlas.getEquipmentNum(ac);

        Entity reloaded = roundTrip(atlas);
        WeaponMounted reloadedAc = (WeaponMounted) reloaded.getEquipment(acEquipmentNum);

        assertTrue(reloadedAc.isAutocannonHit(), "autocannon-hit flag should survive the MUL round trip");
        // The first crit does not destroy the weapon or damage a crit slot.
        assertFalse(reloadedAc.isDestroyed(), "a first-crit autocannon must not come back destroyed");
        assertFalse(reloadedAc.isHit(), "a first-crit autocannon must not come back hit");
        assertEquals(0,
              reloaded.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, acEquipmentNum, reloadedAc.getLocation()),
              "a first-crit autocannon must have no damaged crit slots");
    }

    @Test
    @DisplayName("the ten-slot AC/20 writes its first-crit flag exactly once")
    void autocannonHitWrittenOncePerMount() throws Exception {
        Entity atlas = createAtlas();
        autocannon(atlas).setAutocannonHit(true);

        File file = tempDir.resolve("non-crit-damage-once.mul").toFile();
        EntityListFile.saveTo(file, new ArrayList<>(List.of(atlas)), true);
        String xml = Files.readString(file.toPath());

        int occurrences = xml.split(MULParser.ATTR_AUTOCANNON_HIT + "=\"true\"", -1).length - 1;
        assertEquals(1, occurrences, "the flag should be written once even for a multi-slot weapon: " + xml);
    }

    @Test
    @DisplayName("a locked Directional Torso Mount flag survives a MUL round trip")
    void directionalMountLockedRoundTrips() throws Exception {
        Entity atlas = createAtlas();
        WeaponMounted weapon = autocannon(atlas);
        weapon.setDirectionalMountLocked(true);
        int equipmentNum = atlas.getEquipmentNum(weapon);

        Entity reloaded = roundTrip(atlas);
        WeaponMounted reloadedWeapon = (WeaponMounted) reloaded.getEquipment(equipmentNum);

        assertTrue(reloadedWeapon.isDirectionalMountLocked(),
              "directional-mount-locked flag should survive the MUL round trip");
        assertFalse(reloadedWeapon.isDestroyed(), "a locked directional mount must not come back destroyed");
    }

    @Test
    @DisplayName("an undamaged unit writes neither flag")
    void undamagedWritesNoFlags() throws Exception {
        File file = tempDir.resolve("non-crit-damage-clean.mul").toFile();
        EntityListFile.saveTo(file, new ArrayList<>(List.of(createAtlas())), true);
        String xml = Files.readString(file.toPath());

        assertFalse(xml.contains(MULParser.ATTR_AUTOCANNON_HIT), "clean unit should not write the autocannon flag");
        assertFalse(xml.contains(MULParser.ATTR_DIRECTIONAL_MOUNT_LOCKED),
              "clean unit should not write the directional-mount flag");
    }
}
