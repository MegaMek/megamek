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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Vector;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.loaders.MULParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that a crew's sidearm and Small Arms skill survive the two trips a crew makes: Java serialization to the
 * server and into save games, and the unit list that MekHQ hands a campaign's people across on.
 */
class CrewPersonalEquipmentRoundTripTest {

    private static final String AUTO_PISTOL = "Auto-Pistol";
    private static final String SNOWSUIT = "Snowsuit";

    private Game game;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
    }

    /**
     * A pilot already on foot. The unit list reloads most units from the cached unit files by chassis and model,
     * which a unit made up for a test is not in; an ejected crew is rebuilt from its name alone, so it is the one
     * kind of unit whose crew attributes can be written and read back without the cache.
     */
    private MekWarrior pilotOnFoot() {
        MekWarrior pilot = new MekWarrior();
        pilot.setGame(game);
        pilot.setId(game.getNextEntityId());
        pilot.setModel("Test Pilot");
        pilot.setOwner(game.getPlayer(0));
        return pilot;
    }

    private static Crew throughJavaSerialization(Crew crew) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(crew);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (Crew) in.readObject();
        }
    }

    private static String toMul(Entity entity) throws Exception {
        StringWriter writer = new StringWriter();
        ArrayList<Entity> entities = new ArrayList<>();
        entities.add(entity);
        EntityListFile.writeEntityList(writer, entities);
        return writer.toString();
    }

    private static Crew crewReadBackFrom(String mul) throws Exception {
        MULParser parser = new MULParser(new ByteArrayInputStream(mul.getBytes(StandardCharsets.UTF_8)), null);
        Vector<Entity> entities = parser.getEntities();
        assertEquals(1, entities.size(), "one unit was written, so one should come back: " + parser.getWarningMessage());
        return entities.firstElement().getCrew();
    }

    @Test
    void sidearmAndSmallArmsSurviveJavaSerialization() throws Exception {
        Crew crew = new Crew(CrewType.SINGLE);
        crew.setSidearmName(AUTO_PISTOL, 0);
        crew.setSmallArms(6, 0);
        crew.setArmorKitName(SNOWSUIT, 0);

        Crew restored = throughJavaSerialization(crew);

        assertEquals(AUTO_PISTOL, restored.getSidearmName(0));
        assertEquals(6, restored.getSmallArms(0));
        assertTrue(restored.hasSmallArms(0));
        assertEquals(SNOWSUIT, restored.getArmorKitName(0));
    }

    @Test
    void aCrewIssuedNothingRestoresAsIssuedNothing() throws Exception {
        Crew restored = throughJavaSerialization(new Crew(CrewType.SINGLE));

        assertNull(restored.getSidearmName(0));
        assertNull(restored.getAnySidearmName());
        assertFalse(restored.hasSmallArms(0));
        assertEquals(Crew.SMALL_ARMS_UNSET, restored.getSmallArms(0));
    }

    @Test
    void sidearmAndSmallArmsAreWrittenToTheUnitList() throws Exception {
        MekWarrior pilot = pilotOnFoot();
        pilot.getCrew().setSidearmName(AUTO_PISTOL, 0);
        pilot.getCrew().setSmallArms(5, 0);

        String mul = toMul(pilot);

        assertTrue(mul.contains(MULParser.ATTR_SIDEARM + "=\"" + AUTO_PISTOL + "\""),
              "the unit list must carry the sidearm by name: " + mul);
        assertTrue(mul.contains(MULParser.ATTR_SMALL_ARMS + "=\"5\""),
              "the unit list must carry the Small Arms skill: " + mul);
    }

    @Test
    void anUnsetSmallArmsSkillWritesNoAttribute() throws Exception {
        String mul = toMul(pilotOnFoot());

        assertFalse(mul.contains(MULParser.ATTR_SMALL_ARMS + "=\""),
              "a skill nobody entered must not be invented on the way out: " + mul);
    }

    @Test
    void sidearmAndSmallArmsComeBackFromTheUnitList() throws Exception {
        MekWarrior pilot = pilotOnFoot();
        pilot.getCrew().setSidearmName(AUTO_PISTOL, 0);
        pilot.getCrew().setSmallArms(5, 0);

        Crew readBack = crewReadBackFrom(toMul(pilot));

        assertEquals(AUTO_PISTOL, readBack.getSidearmName(0));
        assertEquals(5, readBack.getSmallArms(0));
    }

    @Test
    void aUnitListWithoutTheAttributesLeavesTheCrewUnarmedAndUnskilled() throws Exception {
        Crew readBack = crewReadBackFrom(toMul(pilotOnFoot()));

        assertNull(readBack.getSidearmName(0), "an older file simply has no attribute");
        assertFalse(readBack.hasSmallArms(0), "and the crew fires on foot with their gunnery, as before");
    }
}
