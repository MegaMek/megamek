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
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.util.Vector;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityMovementMode;
import megamek.common.weapons.handlers.srm.SRMHandler;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * An airborne fighter's missiles against conventional infantry use the Non-Infantry Weapon Damage Against Infantry
 * Table (TW p. 215; aerospace example TW pp. 244-245), not the attack value (issue #9219).
 */
class AirToGroundMissileInfantryTest extends GameBoardTestCase {

    private static final int REPORT_HITS_ATTACK_VALUE = 3391;
    private static final int REPORT_MISSILES_HIT = 3325;
    private static final int PLATOON_SIZE = 28;
    /** SRM-6: 12 maximum damage / 5 = 3 troopers, doubled in clear terrain (TW p. 216). */
    private static final int SRM_6_TROOPERS_LOST_IN_THE_OPEN = 6;

    static {
        initializeBoard("AIR_TO_GROUND_INFANTRY_BOARD", """
              size 16 17
              hex 0501 0 "" ""
              hex 0505 0 "" ""
              end"""
        );
    }

    private TWGameManager gameManager;
    private Game game;
    private AeroSpaceFighter attacker;
    private ConvInfantry target;
    private Mounted<?> srm;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).entityUpdate(anyInt());
        game = gameManager.getGame();
        game.setBoard(getBoard("AIR_TO_GROUND_INFANTRY_BOARD"));
        Player attackerOwner = new Player(0, "Attacker");
        Player targetOwner = new Player(1, "Defender");
        attackerOwner.setTeam(1);
        targetOwner.setTeam(2);
        game.addPlayer(0, attackerOwner);
        game.addPlayer(1, targetOwner);

        attacker = new AeroSpaceFighter();
        attacker.setGame(game);
        attacker.setId(game.getNextEntityId());
        attacker.setChassis("Test Fighter");
        attacker.setModel("Attacker");
        attacker.setCrew(new Crew(CrewType.SINGLE));
        attacker.setOwner(attackerOwner);
        attacker.setWeight(50);
        srm = attacker.addEquipment(EquipmentType.get("ISSRM6"), AeroSpaceFighter.LOC_NOSE);
        Mounted<?> ammo = attacker.addEquipment(EquipmentType.get("IS Ammo SRM-6"), AeroSpaceFighter.LOC_NOSE);
        srm.setLinked(ammo);
        game.addEntity(attacker);
        attacker.setPosition(new Coords(4, 0));
        attacker.setAltitude(3);
        attacker.setDeployed(true);

        target = new ConvInfantry();
        target.setGame(game);
        target.setId(game.getNextEntityId());
        target.setChassis("Test Platoon");
        target.setModel("Target");
        target.setMovementMode(EntityMovementMode.INF_LEG);
        target.setSquadSize(7);
        target.setSquadCount(4);
        target.autoSetInternal();
        target.setOwner(targetOwner);
        game.addEntity(target);
        target.setPosition(new Coords(4, 4));
        target.setDeployed(true);
    }

    private Vector<Report> fireSrm() throws Exception {
        WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), target.getId(),
              attacker.getEquipmentNum(srm));
        SRMHandler handler = new SRMHandler(new ToHitData(2, "test"), attack, game, gameManager);
        Vector<Report> reports = new Vector<>();
        handler.handle(GamePhase.FIRING, reports);
        return reports;
    }

    private static boolean hasReport(Vector<Report> reports, int messageId) {
        for (Report report : reports) {
            if (report.messageId == messageId) {
                return true;
            }
        }
        return false;
    }

    @Test
    void airToGroundMissilesUseTheInfantryTable() throws Exception {
        assertTrue(attacker.isAirborne(), "the fighter is airborne");
        assertEquals(PLATOON_SIZE, target.getInternal(0), "the platoon starts at full strength");

        Vector<Report> reports = fireSrm();

        assertEquals(PLATOON_SIZE - SRM_6_TROOPERS_LOST_IN_THE_OPEN, target.getInternal(0),
              "an SRM-6 kills 3 troopers, doubled in the open");
        assertTrue(hasReport(reports, REPORT_MISSILES_HIT), "the hit line counts missiles, as for a ground unit");
        assertFalse(hasReport(reports, REPORT_HITS_ATTACK_VALUE), "the attack value is not used against infantry");
    }

    @Test
    void aeroSanityGivesTheSameInfantryDamage() throws Exception {
        game.getOptions().getOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY).setValue(true);

        fireSrm();

        assertEquals(PLATOON_SIZE - SRM_6_TROOPERS_LOST_IN_THE_OPEN, target.getInternal(0),
              "Aero Sanity uses the infantry table too");
    }
}
