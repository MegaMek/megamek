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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.Sensor;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.rules.RulesManager;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.common.weapons.DamageType;
import megamek.common.weapons.autoCannons.clan.CLProtoMekAC2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RecordSheetUnitRulesTest {
    private RulesManager previousRules;
    private TWGameManager manager;
    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        previousRules = Game.rulesManager;
        manager = spy(new TWGameManager());
        doNothing().when(manager).send(any(Packet.class));
        game = manager.getGame();
        game.setBoard(new Board(1, 1, new Hex[] { new Hex() }));
        game.addPlayer(0, new Player(0, "Test"));
    }

    @AfterEach
    void restoreRules() {
        Game.rulesManager = previousRules;
    }

    private void deploy(Entity unit, String rules) {
        game.initializeRulesManager(rules);
        unit.setOwner(game.getPlayer(0));
        unit.setId(game.getNextEntityId());
        unit.setPosition(new Coords(0, 0));
        game.addEntity(unit);
    }

    @ParameterizedTest
    @ValueSource(strings = { OptionsConstants.RULES_CORE, OptionsConstants.RULES_TW })
    void vehicleAmmoExplosionVentsThroughRearArmorOnlyWithCase(String rules) throws LocationFullException {
        for (boolean cased : new boolean[] { false, true }) {
            Tank tank = new Tank();
            deploy(tank, rules);
            tank.initializeInternal(50, Tank.LOC_FRONT);
            tank.initializeArmor(50, Tank.LOC_FRONT);
            tank.initializeArmor(50, Tank.LOC_REAR);
            if (cased) {
                tank.addEquipment(EquipmentType.get(EquipmentTypeLookup.IS_CASE), Tank.LOC_BODY);
            }
            manager.damageEntity(tank, new HitData(Tank.LOC_FRONT), 30, true, DamageType.NONE, false);
            assertEquals(cased ? 50 : 20, tank.getInternal(Tank.LOC_FRONT));
            assertEquals(50, tank.getArmor(Tank.LOC_FRONT));
            assertEquals(cased ? 20 : 50, tank.getArmor(Tank.LOC_REAR));
            if (cased) {
                assertTrue(tank.getStunnedTurns() > 0);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { OptionsConstants.RULES_CORE, OptionsConstants.RULES_TW })
    void firstProtoMekTorsoWeaponCriticalDisablesItsAutocannon(String rules) throws LocationFullException {
        ProtoMek proto = new ProtoMek();
        proto.setWeight(5);
        deploy(proto, rules);
        Mounted<?> weapon = proto.addEquipment(new CLProtoMekAC2(), ProtoMek.LOC_TORSO);
        CriticalSlot critical = new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSO_WEAPON_A);
        var reports = manager.applyCriticalHit(proto, ProtoMek.LOC_TORSO, critical, true, 0, false);
        assertTrue(weapon.isHit());
        assertTrue(critical.isHit());
        assertFalse(weapon.isAutocannonHit());
        assertTrue(reports.stream().mapToInt(report -> report.messageId).anyMatch(id -> id == 6245));
    }

    @ParameterizedTest
    @ValueSource(strings = { OptionsConstants.RULES_CORE, OptionsConstants.RULES_TW })
    void novaSensorBracketUsesSelectedRuleset(String rules) {
        game.initializeRulesManager(rules);
        assertEquals(rules.equals(OptionsConstants.RULES_CORE) ? 15 : 9,
              new Sensor(Sensor.TYPE_NOVA).getRangeByBracket());
    }

    @ParameterizedTest
    @ValueSource(strings = { OptionsConstants.RULES_CORE, OptionsConstants.RULES_TW })
    void watchdogUsesRulesetProbeRangeAndSharedSensorAndEcmRanges(String rules) throws LocationFullException {
        Mek mek = new BipedMek();
        deploy(mek, rules);
        mek.addEquipment(EquipmentType.get(Sensor.WATCHDOG), Mek.LOC_RIGHT_TORSO);
        assertEquals(rules.equals(OptionsConstants.RULES_CORE) ? 5 : 3, mek.getBAPRange());
        mek.getCrew().setPiloting(5, 0);
        assertEquals(rules.equals(OptionsConstants.RULES_CORE) ? 6 : 7,
              Game.rulesManager.getRulesScanning().scanTargetRoll(mek, null).getValue());
        assertEquals(6, mek.getECMRange());
        assertEquals(9, Game.rulesManager.getRulesEquipment().getSensorRanges(Sensor.TYPE_WATCHDOG));
    }
}
