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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.EjectedCrew;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.MekWarrior;
import megamek.common.units.Tank;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests what a crew is holding, and what skill they fire it with, once the server has put them on the board.
 * <p>
 * Driven through {@code ejectEntity} rather than the constructor, so that the option gates, the ejected flag the
 * skill switch hangs on, and the shared crew object are all exercised the way a game exercises them.
 */
class CrewSidearmEjectionTest {

    private static final String AUTO_PISTOL = "Auto-Pistol";
    private static final String AUTO_RIFLE = "InfantryAssaultRifle";
    private static final String BOARD_DATA = """
          size 4 4
          hex 0101 0 "" ""
          hex 0102 0 "" ""
          hex 0103 0 "" ""
          hex 0104 0 "" ""
          end""";

    private TWGameManager gameManager;
    private Game game;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).entityUpdate(anyInt());

        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Test"));
        Board board = BoardLoader.initializeBoard(BOARD_DATA);
        game.setBoard(board);

        game.getOptions().getOption(OptionsConstants.RPG_COMBAT_SUITS).setValue(true);
        game.getOptions().getOption(OptionsConstants.ADVANCED_ARMED_MEKWARRIORS).setValue(true);
        game.getOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(3025);
    }

    private <T extends Entity> T deploy(T entity, int entityId, @Nullable String sidearmName) {
        entity.setId(entityId);
        entity.setOwner(game.getPlayer(0));
        entity.getCrew().setSidearmName(sidearmName, 0);
        game.addEntity(entity);
        entity.setPosition(new Coords(0, 0));
        entity.setDeployed(true);
        return entity;
    }

    /**
     * A Mek built the way ejection expects to find one: with internal structure and a cockpit slot to destroy.
     */
    private Mek deployedMek(int entityId, @Nullable String sidearmName) {
        Mek mek = new BipedMek();
        mek.autoSetInternal();
        mek.addCockpit();
        return deploy(mek, entityId, sidearmName);
    }

    private EjectedCrew crewOnFootFrom(Entity ride) {
        for (Entity entity : game.getEntitiesVector()) {
            if ((entity instanceof EjectedCrew ejected) && (ejected.getOriginalRideId() == ride.getId())) {
                return ejected;
            }
        }
        throw new AssertionError("no crew on foot was created for " + ride.getDisplayName());
    }

    private static String primaryWeaponOf(EjectedCrew crew) {
        return (crew.getPrimaryWeapon() == null) ? null : crew.getPrimaryWeapon().getInternalName();
    }

    @Test
    void aMekWarriorIssuedAPistolLeavesHoldingIt() {
        Mek mek = deployedMek(1, AUTO_PISTOL);

        gameManager.ejectEntity(mek, false, false);
        EjectedCrew pilot = crewOnFootFrom(mek);

        assertTrue(pilot instanceof MekWarrior);
        assertEquals(AUTO_PISTOL, primaryWeaponOf(pilot));
        assertEquals(0.21, pilot.getDamagePerTrooper(), 0.001, "an auto-pistol does 0.21 per trooper");
        assertTrue(pilot.getWeaponList().stream().anyMatch(weapon -> weapon.getType().getInternalName()
              .equals(AUTO_PISTOL)), "the pistol must be mounted, or it cannot be fired");
    }

    @Test
    void aMekWarriorIssuedNothingGetsTheDefaultPistol() {
        Mek mek = deployedMek(2, null);

        gameManager.ejectEntity(mek, false, false);

        assertEquals(AUTO_PISTOL, primaryWeaponOf(crewOnFootFrom(mek)),
              "with the rule on, a MekWarrior nobody equipped still steps out with a pistol");
    }

    @Test
    void aMekWarriorIsUnarmedWithArmedMekWarriorsOff() {
        game.getOptions().getOption(OptionsConstants.ADVANCED_ARMED_MEKWARRIORS).setValue(false);
        Mek mek = deployedMek(3, AUTO_PISTOL);

        gameManager.ejectEntity(mek, false, false);

        assertNull(primaryWeaponOf(crewOnFootFrom(mek)),
              "a pistol on the roster does not arm a pilot the unofficial option says is unarmed");
    }

    @Test
    void thePistolIsIgnoredWithCrewPersonalEquipmentOff() {
        game.getOptions().getOption(OptionsConstants.RPG_COMBAT_SUITS).setValue(false);
        Mek mek = deployedMek(4, AUTO_PISTOL);

        gameManager.ejectEntity(mek, false, false);

        assertEquals(AUTO_RIFLE, primaryWeaponOf(crewOnFootFrom(mek)),
              "with the rule off the sidearm is invisible and the old rifle applies");
    }

    @Test
    void aVehicleCrewIssuedAPistolAbandonsHoldingIt() {
        Tank tank = deploy(new Tank(), 5, AUTO_PISTOL);

        gameManager.ejectEntity(tank, false, false);

        assertEquals(AUTO_PISTOL, primaryWeaponOf(crewOnFootFrom(tank)));
    }

    @Test
    void aVehicleCrewIssuedNothingGetsTheDefaultSubmachineGun() {
        Tank tank = deploy(new Tank(), 6, null);

        gameManager.ejectEntity(tank, false, false);

        assertEquals("Submachine Gun", primaryWeaponOf(crewOnFootFrom(tank)),
              "a vehicle crew has room for more than a pistol");
    }

    @Test
    void withCrewPersonalEquipmentOffEveryCrewStillGetsTheRifle() {
        game.getOptions().getOption(OptionsConstants.RPG_COMBAT_SUITS).setValue(false);
        Mek mek = deployedMek(14, null);
        Tank tank = deploy(new Tank(), 15, null);

        gameManager.ejectEntity(mek, false, false);
        gameManager.ejectEntity(tank, false, false);

        assertEquals(AUTO_RIFLE, primaryWeaponOf(crewOnFootFrom(mek)), "the old behaviour, untouched");
        assertEquals(AUTO_RIFLE, primaryWeaponOf(crewOnFootFrom(tank)));
    }

    @Test
    void aPilotWithSmallArmsRecordedShootsWithItOnceOnFoot() {
        Mek mek = deployedMek(7, AUTO_PISTOL);
        mek.getCrew().setGunnery(2, 0);
        mek.getCrew().setSmallArms(6, 0);
        assertEquals(2, mek.getCrew().getGunnery(), "aboard the Mek, gunnery is what fires the Mek's weapons");

        gameManager.ejectEntity(mek, false, false);
        EjectedCrew pilot = crewOnFootFrom(mek);

        assertTrue(pilot.getCrew().isEjected());
        assertEquals(6, pilot.getCrew().getGunnery(), "on foot, the pistol is fired with Small Arms");
        assertEquals(6, pilot.getCrew().getGunneryB(), "and so is every gunnery flavour RPG gunnery could ask for");
    }

    @Test
    void aPilotWithNoSmallArmsRecordedShootsWithTheMekWarriorDefault() {
        Mek mek = deployedMek(8, AUTO_PISTOL);
        mek.getCrew().setGunnery(2, 0);

        gameManager.ejectEntity(mek, false, false);

        assertEquals(6, crewOnFootFrom(mek).getCrew().getGunnery(),
              "a MekWarrior nobody entered a skill for is trained to 6 with a sidearm, not to their Mek gunnery");
    }

    @Test
    void aVehicleCrewWithNoSmallArmsRecordedShootsWithTheVehicleDefault() {
        Tank tank = deploy(new Tank(), 12, null);
        tank.getCrew().setGunnery(3, 0);

        gameManager.ejectEntity(tank, false, false);

        assertEquals(5, crewOnFootFrom(tank).getCrew().getGunnery(), "vehicle crews are trained to 5");
    }

    @Test
    void withCrewPersonalEquipmentOffACrewShootsWithGunneryAsBefore() {
        game.getOptions().getOption(OptionsConstants.RPG_COMBAT_SUITS).setValue(false);
        Mek mek = deployedMek(13, null);
        mek.getCrew().setGunnery(2, 0);

        gameManager.ejectEntity(mek, false, false);

        assertEquals(2, crewOnFootFrom(mek).getCrew().getGunnery(),
              "with the rule off nothing about ejection changes, default included");
    }

    @Test
    void theSmallArmsSkillIsOnlyUsedOnceEjected() {
        Mek mek = deployedMek(9, AUTO_PISTOL);
        mek.getCrew().setGunnery(2, 0);
        mek.getCrew().setSmallArms(6, 0);

        assertEquals(2, mek.getCrew().getGunnery(), "a recorded Small Arms skill must not touch the Mek's gunnery");
        assertNotNull(mek.getCrew());
    }

    /**
     * The client computes the to-hit number it shows from its own copy of the crew, and that copy is whatever the
     * server sent when it added the pilot. If the crew is marked ejected only after that packet goes out, the client
     * shows the pistol fired on Mek gunnery while the server resolves it on Small Arms.
     */
    @Test
    void thePilotIsAlreadyMarkedEjectedWhenSentToClients() {
        List<Boolean> ejectedWhenSent = new ArrayList<>();
        Mockito.doAnswer(invocation -> {
            Packet packet = invocation.getArgument(0);
            if (packet.command() == PacketCommand.ENTITY_ADD) {
                for (Entity added : entitiesIn(packet)) {
                    if (added instanceof MekWarrior pilot) {
                        ejectedWhenSent.add(pilot.getCrew().isEjected());
                    }
                }
            }
            return null;
        }).when(gameManager).send(any(Packet.class));
        Mek mek = deployedMek(10, AUTO_PISTOL);

        gameManager.ejectEntity(mek, false, false);

        assertEquals(List.of(true), ejectedWhenSent,
              "the one packet that adds the pilot must already carry the ejected flag");
    }

    @Test
    void anAbandoningVehicleCrewIsAlsoSentAlreadyMarkedEjected() {
        List<Boolean> ejectedWhenSent = new ArrayList<>();
        Mockito.doAnswer(invocation -> {
            Packet packet = invocation.getArgument(0);
            if (packet.command() == PacketCommand.ENTITY_ADD) {
                for (Entity added : entitiesIn(packet)) {
                    if (added instanceof EjectedCrew crew) {
                        ejectedWhenSent.add(crew.getCrew().isEjected());
                    }
                }
            }
            return null;
        }).when(gameManager).send(any(Packet.class));
        Tank tank = deploy(new Tank(), 11, AUTO_PISTOL);

        gameManager.ejectEntity(tank, false, false);

        assertEquals(List.of(true), ejectedWhenSent);
    }

    @SuppressWarnings("unchecked")
    private static List<Entity> entitiesIn(Packet packet) {
        return (List<Entity>) packet.getObject(0);
    }
}
