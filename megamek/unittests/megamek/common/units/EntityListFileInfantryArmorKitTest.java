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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.ArrayList;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.game.Game;
import megamek.common.loaders.MULParser;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that the armor kit worn by a conventional infantry platoon is written to the MUL/entity-XML used by save games
 * and by MekHQ scenario transfer. Only the kit's effects (such as the encumbering flag) used to be saved, so a platoon
 * issued a snow suit came back with no kit, and was doomed on deployment into extreme temperatures.
 */
class EntityListFileInfantryArmorKitTest {

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
        infantry.setModel("Kit");
        infantry.setOwner(game.getPlayer(0));
        infantry.setCrew(new Crew(CrewType.INFANTRY_CREW));
        infantry.setPrimaryWeapon((InfantryWeapon) EquipmentType.get("InfantryAssaultRifle"));
        infantry.autoSetInternal();
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        return infantry;
    }

    private static String toMul(ConvInfantry infantry) throws Exception {
        StringWriter writer = new StringWriter();
        ArrayList<Entity> list = new ArrayList<>();
        list.add(infantry);
        EntityListFile.writeEntityList(writer, list);
        return writer.toString();
    }

    @Test
    @DisplayName("the platoon's armor kit is written to the MUL output")
    void armorKitIsSerialized() throws Exception {
        ConvInfantry infantry = createInfantry();
        MiscType snowSuit = MiscType.createSnowSuitInfArmor();
        infantry.setArmorKit(snowSuit);

        String xml = toMul(infantry);

        assertTrue(xml.contains(MULParser.ATTR_INF_ARMOR_KIT + "=\"" + snowSuit.getInternalName() + "\""),
              "MUL output should name the armor kit: " + xml);
    }

    @Test
    @DisplayName("a platoon with no armor kit writes no kit attribute")
    void noKitWritesNothing() throws Exception {
        String xml = toMul(createInfantry());

        assertFalse(xml.contains(MULParser.ATTR_INF_ARMOR_KIT + "=\""),
              "MUL output should not include a kit attribute when none is worn");
    }
}
