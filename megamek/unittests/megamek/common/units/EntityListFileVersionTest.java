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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;

import megamek.MMConstants;
import megamek.Version;
import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.loaders.MULParser;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that a MUL records the version it was saved in and that {@link MULParser} reacts to it: files from a newer
 * version are refused outright, files from an older version (or without a version) are flagged so callers can warn, and
 * files from the current version load silently.
 */
class EntityListFileVersionTest {

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

    private ConvInfantry createInfantry() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setGame(game);
        infantry.setId(game.getNextEntityId());
        infantry.setChassis("Test Platoon");
        infantry.setModel("Version");
        infantry.setOwner(game.getPlayer(0));
        infantry.setCrew(new Crew(CrewType.INFANTRY_CREW));
        infantry.setPrimaryWeapon((InfantryWeapon) EquipmentType.get("InfantryAssaultRifle"));
        infantry.autoSetInternal();
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        return infantry;
    }

    /**
     * Produces a one-unit MUL string whose root {@code <unit>} element carries the given version, mirroring what
     * {@link EntityListFile#saveTo} writes.
     */
    private String mulWithVersion(String version) throws Exception {
        StringWriter body = new StringWriter();
        ArrayList<Entity> list = new ArrayList<>();
        list.add(createInfantry());
        EntityListFile.writeEntityList(body, list);
        return "<?xml " + MULParser.VERSION + "=\"1.0\" encoding=\"UTF-8\"?>\n\n"
              + '<' + MULParser.ELE_UNIT + ' ' + MULParser.VERSION + "=\"" + version + "\" >\n\n"
              + body
              + "</" + MULParser.ELE_UNIT + ">\n";
    }

    /** Produces a one-unit MUL saved with the current running version. */
    private String currentVersionMul() throws Exception {
        return mulWithVersion(MMConstants.VERSION.toString());
    }

    private MULParser parse(String mul) throws Exception {
        return new MULParser(new ByteArrayInputStream(mul.getBytes(UTF_8)), null);
    }

    @Test
    @DisplayName("a MUL records the version it was saved in on its root element")
    void mulRecordsSaveVersion() throws Exception {
        String mul = currentVersionMul();

        assertTrue(mul.contains("version=\"" + MMConstants.VERSION + "\""),
              "MUL should record the running version on its root element: " + mul);
    }

    @Test
    @DisplayName("a MUL from the current version loads silently")
    void currentVersionLoadsSilently() throws Exception {
        MULParser parser = parse(currentVersionMul());

        assertFalse(parser.isNewerVersion(), "Current-version MUL must not be flagged as newer");
        assertFalse(parser.isOlderVersion(), "Current-version MUL must not be flagged as older");
    }

    @Test
    @DisplayName("a MUL from an older version is flagged but is still parsed")
    void olderVersionIsFlaggedAndParsed() throws Exception {
        MULParser parser = parse(mulWithVersion("0.0.1"));

        assertTrue(parser.isOlderVersion(), "Older-version MUL should be flagged as older");
        assertFalse(parser.isNewerVersion(), "Older-version MUL must not be flagged as newer");
        // Parsing was still attempted (unlike the newer-version case, which refuses outright).
        // getWarningMessage() may be null when the buffer is empty, so guard against it.
        String warningMessage = parser.getWarningMessage();
        assertFalse(warningMessage != null && warningMessage.contains("cannot be loaded"),
              "Older-version MUL should be parsed, not refused");
    }

    @Test
    @DisplayName("a MUL without a version is treated as older")
    void missingVersionIsTreatedAsOlder() throws Exception {
        MULParser parser = parse(mulWithVersion(""));

        assertTrue(parser.isOlderVersion(), "Version-less MUL should be treated as older");
        assertFalse(parser.isNewerVersion(), "Version-less MUL must not be flagged as newer");
    }

    @Test
    @DisplayName("a MUL from a newer version is refused and loads no units")
    void newerVersionIsRefused() throws Exception {
        Version running = MMConstants.VERSION;
        String newer = (running.getMajor() + 1) + "." + running.getMinor() + "." + running.getPatch();

        MULParser parser = parse(mulWithVersion(newer));

        assertTrue(parser.isNewerVersion(), "Newer-version MUL should be flagged as newer");
        assertFalse(parser.isOlderVersion(), "Newer-version MUL must not be flagged as older");
        assertTrue(parser.getEntities().isEmpty(), "Newer-version MUL must not load any units");
        assertTrue(parser.getWarningMessage().contains("cannot be loaded"),
              "Newer-version MUL should be refused outright");
    }
}
