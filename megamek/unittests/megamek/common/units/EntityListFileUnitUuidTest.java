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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.icons.Camouflage;
import megamek.common.loaders.MULParser;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that the unit-file UUID - the primary MUL lookup key - is written for ordinary (non-asset) units when present
 * and omitted when absent (so old, UUID-less units keep resolving by chassis/model name).
 */
class EntityListFileUnitUuidTest {

    private static final String UNIT_UUID = "0191b3e2-9999-7c3d-8e4f-abcabcabcabc";

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
        infantry.setModel("UUID");
        infantry.setOwner(game.getPlayer(0));
        infantry.setCrew(new Crew(CrewType.INFANTRY_CREW));
        infantry.setPrimaryWeapon((InfantryWeapon) EquipmentType.get("InfantryAssaultRifle"));
        infantry.autoSetInternal();
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        return infantry;
    }

    private static String toMul(Entity entity) throws Exception {
        StringWriter writer = new StringWriter();
        ArrayList<Entity> list = new ArrayList<>();
        list.add(entity);
        EntityListFile.writeEntityList(writer, list);
        return writer.toString();
    }

    @Test
    @DisplayName("a non-asset unit with a UUID writes it as the primary lookup key")
    void unitWithUuidWritesIt() throws Exception {
        ConvInfantry infantry = createInfantry();
        infantry.setUnitFileUUID(UNIT_UUID);

        String xml = toMul(infantry);

        assertTrue(xml.contains(MULParser.ATTR_UNIT_FILE_UUID + "=\"" + UNIT_UUID + "\""),
              "MUL should include the unit-file UUID for a non-asset unit: " + xml);
    }

    @Test
    @DisplayName("every unit writes its unit-file UUID (units always carry one)")
    void unitAlwaysWritesItsUuid() throws Exception {
        ConvInfantry infantry = createInfantry(); // carries a default (regenerated) UUID

        String xml = toMul(infantry);

        assertTrue(xml.contains(MULParser.ATTR_UNIT_FILE_UUID + "=\"" + infantry.getUnitFileUUID() + "\""),
              "MUL should write each unit's unit-file UUID as the primary lookup key: " + xml);
    }

    @Test
    void legacyCamouflageWithoutOverlayFieldsWritesSafely() throws ReflectiveOperationException {
        ConvInfantry infantry = createInfantry();
        Camouflage camouflage = new Camouflage();
        setOverlayField(camouflage, "overlayColor", null);
        setOverlayField(camouflage, "overlayDirection", null);
        setOverlayField(camouflage, "overlayStyle", null);
        infantry.setCamouflage(camouflage);

        String xml = assertDoesNotThrow(() -> toMul(infantry));

        assertFalse(xml.contains(MULParser.ATTR_CAMO_OVERLAY_STYLE));
        assertFalse(xml.contains(MULParser.ATTR_CAMO_OVERLAY_DIRECTION));
        assertFalse(xml.contains(MULParser.ATTR_CAMO_OVERLAY_COLOR));
    }

    private static void setOverlayField(Camouflage camouflage, String fieldName, Object value)
          throws ReflectiveOperationException {
        Field field = Camouflage.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(camouflage, value);
    }
}
