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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.weapons.handlers.srm.SRMHandler;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * An airborne fighter's missiles against a ground unit make no cluster roll: the attack value is applied in 5-point
 * groups. The "hits" line must say what that attack value was, since there is no missile count to report (issue
 * #9183).
 */
class AirToGroundMissileReportTest extends GameBoardTestCase {

    private static final int REPORT_HITS = 3390;
    private static final int REPORT_HITS_ATTACK_VALUE = 3391;
    private static final int REPORT_MISSILES_HIT = 3325;

    static {
        initializeBoard("AIR_TO_GROUND_BOARD", """
              size 16 17
              hex 0501 0 "" ""
              hex 0505 0 "" ""
              end"""
        );
    }

    private TWGameManager gameManager;
    private Game game;
    private AeroSpaceFighter attacker;
    private BipedMek target;
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
        game.setBoard(getBoard("AIR_TO_GROUND_BOARD"));
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

        target = new BipedMek();
        target.setGame(game);
        target.setId(game.getNextEntityId());
        target.setChassis("Test Mek");
        target.setModel("Target");
        target.setCrew(new Crew(CrewType.SINGLE));
        target.setOwner(targetOwner);
        target.setWeight(50.0);
        target.autoSetInternal();
        for (int location = 0; location < target.locations(); location++) {
            target.initializeArmor(20, location);
        }
        game.addEntity(target);
        target.setPosition(new Coords(4, 4));
        target.setFacing(0);
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

    private static Report findReport(Vector<Report> reports, int messageId) {
        for (Report report : reports) {
            if (report.messageId == messageId) {
                return report;
            }
        }
        return null;
    }

    private static boolean hasReport(Vector<Report> reports, int messageId) {
        return findReport(reports, messageId) != null;
    }

    /**
     * An SRM-6 has a short range attack value of 8, so the line should read "hits (attack value 8)."
     */
    @Test
    void airToGroundHitReportsTheAttackValue() throws Exception {
        assertTrue(attacker.isAirborne(), "the fighter is airborne");

        Vector<Report> reports = fireSrm();

        Report attackValueReport = findReport(reports, REPORT_HITS_ATTACK_VALUE);
        assertNotNull(attackValueReport, "the hit line reports the attack value");
        assertTrue(attackValueReport.text().contains("attack value 8"),
              "the line shows the SRM-6 attack value: " + attackValueReport.text());
        assertFalse(hasReport(reports, REPORT_HITS), "the plain hit line is replaced, not repeated");
    }

    @Test
    void aeroSanityKeepsTheClusterRollReport() throws Exception {
        game.getOptions().getOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY).setValue(true);

        Vector<Report> reports = fireSrm();

        assertTrue(hasReport(reports, REPORT_MISSILES_HIT), "Aero Sanity rolls on the cluster table");
        assertFalse(hasReport(reports, REPORT_HITS_ATTACK_VALUE), "no attack value line under Aero Sanity");
    }
}
