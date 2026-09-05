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
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.loaders.MULParser;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that a conventional infantry platoon's Disposable Weapon (TO:AuE p.116, Corrected Sixth Printing) is written
 * to the MUL/entity-XML used by save games and by MekHQ scenario transfer. Without this the disposable - which is not
 * part of the cached unit design - is lost on round-trip and arrives in MegaMek as a null weapon.
 */
class EntityListFileDisposableWeaponTest {

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
        infantry.setModel("Disposable");
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
    @DisplayName("the disposable weapon is written to the MUL output")
    void disposableWeaponIsSerialized() throws Exception {
        ConvInfantry infantry = createInfantry();
        infantry.equipDisposableWeapon((InfantryWeapon) EquipmentType.get("Rocket Launcher (LAW)"));

        String xml = toMul(infantry);

        assertTrue(xml.contains(MULParser.ATTR_DISPOSABLE_WEAPON + "=\"Rocket Launcher (LAW)\""),
              "MUL output should include the disposable weapon: " + xml);
    }

    @Test
    @DisplayName("a platoon with no disposable weapon writes no disposable attribute")
    void noDisposableWritesNothing() throws Exception {
        String xml = toMul(createInfantry());

        assertFalse(xml.contains(MULParser.ATTR_DISPOSABLE_WEAPON + "=\""),
              "MUL output should not include a disposable weapon attribute when none is equipped");
    }

    @Test
    @DisplayName("a fired disposable weapon is recorded so consumption survives the round-trip")
    void firedDisposableIsSerialized() throws Exception {
        ConvInfantry infantry = createInfantry();
        infantry.equipDisposableWeapon((InfantryWeapon) EquipmentType.get("Rocket Launcher (LAW)"));
        infantry.getWeaponList()
              .stream()
              .filter(WeaponMounted::isDisposableWeapon)
              .findFirst()
              .ifPresent(weaponMounted -> weaponMounted.setFired(true));

        String xml = toMul(infantry);

        assertTrue(xml.contains(MULParser.ATTR_DISPOSABLE_WEAPON_FIRED + "=\"1"),
              "MUL output should record the disposable as fired: " + xml);
    }
}
