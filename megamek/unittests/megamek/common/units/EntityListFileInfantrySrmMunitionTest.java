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
import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.loaders.MULParser;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that a conventional infantry platoon's pre-battle SRM munition declaration (TW p. 143) is written to the MUL
 * used by save games and MekHQ. The declaration is not part of the unit design, so without this it would fall back
 * to the default on every load.
 */
class EntityListFileInfantrySrmMunitionTest {

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

    private ConvInfantry createInfantry(@Nullable String secondaryWeapon) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setGame(game);
        infantry.setId(game.getNextEntityId());
        infantry.setChassis("Test Platoon");
        infantry.setModel("SRM");
        infantry.setOwner(game.getPlayer(0));
        infantry.setCrew(new Crew(CrewType.INFANTRY_CREW));
        infantry.setPrimaryWeapon((InfantryWeapon) EquipmentType.get("InfantryAssaultRifle"));
        if (secondaryWeapon != null) {
            infantry.setSecondaryWeapon((InfantryWeapon) EquipmentType.get(secondaryWeapon));
        }
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
    @DisplayName("an SRM platoon that declared Inferno writes Inferno")
    void infernoDeclarationIsSerialized() throws Exception {
        ConvInfantry infantry = createInfantry("InfantryHeavySRM");
        infantry.setInfernoSrmsDeclared(true);

        String xml = toMul(infantry);

        assertTrue(xml.contains(MULParser.ATTR_SRM_MUNITION + "=\"" + MULParser.VALUE_SRM_MUNITION_INFERNO + "\""),
              "MUL output should record the Inferno declaration: " + xml);
    }

    @Test
    @DisplayName("an Inferno launcher platoon switched to standard writes Standard, so a load keeps the choice")
    void standardDeclarationIsSerialized() throws Exception {
        ConvInfantry infantry = createInfantry("InfantryStandardSRMInferno");
        infantry.setInfernoSrmsDeclared(false);

        String xml = toMul(infantry);

        assertTrue(xml.contains(MULParser.ATTR_SRM_MUNITION + "=\"" + MULParser.VALUE_SRM_MUNITION_STANDARD + "\""),
              "MUL output should record the standard declaration: " + xml);
    }

    @Test
    @DisplayName("a platoon without an SRM launcher writes no SRM munition attribute")
    void noSrmLauncherWritesNothing() throws Exception {
        String xml = toMul(createInfantry(null));

        assertFalse(xml.contains(MULParser.ATTR_SRM_MUNITION + "=\""),
              "MUL output should not include an SRM munition attribute without an SRM launcher: " + xml);
    }
}
