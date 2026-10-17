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

import megamek.common.Player;
import megamek.common.equipment.DockingCollar;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.loaders.MULParser;
import megamek.common.loaders.MekFileParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Damage to a JumpShip's or WarShip's drive components, grav decks and docking collars survives a save and reload.
 * Before, only the K-F drive and sail integrity were saved, so a reload repaired the rest for free (issue #9226).
 */
class EntityListFileJumpshipDamageRoundTripTest {
    private static final int DAMAGED_GRAV_DECK = 1;

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

    /** The Aegis Heavy Cruiser (2372) has a K-F drive, two grav decks and four docking collars. */
    private Warship createAegis() throws Exception {
        Entity entity = new MekFileParser(new File("testresources/data/mekfiles/Aegis Heavy Cruiser (2372).blk"))
                              .getEntity();
        entity.setGame(game);
        entity.setId(game.getNextEntityId());
        entity.setOwner(game.getPlayer(0));
        return (Warship) entity;
    }

    private File save(Entity entity) throws Exception {
        File file = tempDir.resolve("jumpship-damage.mul").toFile();
        EntityListFile.saveTo(file, new ArrayList<>(List.of(entity)), true);
        return file;
    }

    private Jumpship reload(File file) throws Exception {
        Vector<Entity> parsed = new MULParser(file, null).getEntities();
        assertEquals(1, parsed.size(), "expected exactly one entity back from the MUL");
        return (Jumpship) parsed.firstElement();
    }

    @Test
    void damagedDriveComponentsGravDecksAndCollarsSurviveAReload() throws Exception {
        Warship aegis = createAegis();
        aegis.setKFHeliumTankHit(true);
        aegis.setKFDriveCoilHit(true);
        aegis.setKFFieldInitiatorHit(true);
        aegis.setKFChargingSystemHit(true);
        aegis.setKFDriveControllerHit(true);
        aegis.setLFBatteryHit(true);
        aegis.setGravDeckDamageFlag(DAMAGED_GRAV_DECK, 1);
        DockingCollar damagedCollar = aegis.getDockingCollars().get(1);
        damagedCollar.setDamaged(true);

        Jumpship reloaded = reload(save(aegis));

        assertTrue(reloaded.getKFHeliumTankHit(), "helium tank");
        assertTrue(reloaded.getKFDriveCoilHit(), "drive coil");
        assertTrue(reloaded.getKFFieldInitiatorHit(), "field initiator");
        assertTrue(reloaded.getKFChargingSystemHit(), "charging system");
        assertTrue(reloaded.getKFDriveControllerHit(), "drive controller");
        assertTrue(reloaded.getLFBatteryHit(), "L-F battery");
        assertEquals(1, reloaded.getGravDeckDamageFlag(DAMAGED_GRAV_DECK), "the damaged grav deck");
        assertEquals(0, reloaded.getGravDeckDamageFlag(0), "the other grav deck");
        assertTrue(reloaded.getCollarById(damagedCollar.getCollarNumber()).isDamaged(), "the damaged collar");
        assertEquals(1, reloaded.getTotalDamagedDockingCollars(), "only that collar");
    }

    @Test
    void anUndamagedShipSavesNoExtraDamageAndReloadsUndamaged() throws Exception {
        File file = save(createAegis());

        assertFalse(Files.readString(file.toPath()).contains(MULParser.ELE_JUMPSHIP_CRIT),
              "nothing is written when nothing is damaged");
        Jumpship reloaded = reload(file);
        assertFalse(reloaded.getKFDriveCoilHit());
        assertEquals(0, reloaded.getTotalDamagedGravDeck());
        assertEquals(0, reloaded.getTotalDamagedDockingCollars());
    }
}
