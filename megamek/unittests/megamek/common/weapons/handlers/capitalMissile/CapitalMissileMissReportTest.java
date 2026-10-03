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
package megamek.common.weapons.handlers.capitalMissile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Vector;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.weapons.AlamoMissileWeapon;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A capital missile that misses must not also report "hits". The handler used to add the "hits." line (report 3390)
 * on every miss.
 */
class CapitalMissileMissReportTest {

    private static final int REPORT_MISSES = 3220;
    private static final int REPORT_HITS = 3390;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static AeroSpaceFighter fighter(Game game, Player owner, Coords position) {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.setGame(game);
        fighter.setId(game.getNextEntityId());
        fighter.setChassis("Test Fighter");
        fighter.setModel(owner.getName());
        fighter.setCrew(new Crew(CrewType.SINGLE));
        fighter.setOwner(owner);
        fighter.setWeight(50);
        fighter.autoSetMaxBombPoints();
        fighter.setPosition(position);
        fighter.setDeployed(true);
        game.addEntity(fighter);
        return fighter;
    }

    @Test
    void missedAlamoReportsTheMissOnly() throws Exception {
        TWGameManager gameManager = new TWGameManager();
        Game game = gameManager.getGame();
        game.getOptions().getOption(OptionsConstants.ALLOWED_TECH_LEVEL).setValue("Experimental");
        game.getOptions().getOption(OptionsConstants.ADVANCED_AERO_RULES_AT2_NUKES).setValue(true);
        game.setBoard(Board.getSpaceBoard(16, 17));
        Player attackerOwner = new Player(0, "Attacker");
        Player targetOwner = new Player(1, "Defender");
        attackerOwner.setTeam(1);
        targetOwner.setTeam(2);
        game.addPlayer(0, attackerOwner);
        game.addPlayer(1, targetOwner);

        AeroSpaceFighter attacker = fighter(game, attackerOwner, new Coords(3, 3));
        AeroSpaceFighter target = fighter(game, targetOwner, new Coords(3, 8));
        BombLoadout loadout = new BombLoadout();
        loadout.put(BombTypeEnum.ALAMO, 1);
        attacker.setExtBombChoices(loadout);
        attacker.applyBombs();

        WeaponMounted alamo = null;
        for (WeaponMounted weapon : attacker.getWeaponList()) {
            if (weapon.getType() instanceof AlamoMissileWeapon) {
                alamo = weapon;
            }
        }
        assertNotNull(alamo, "the fighter carries an Alamo");

        WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), target.getId(),
              attacker.getEquipmentNum(alamo));
        ToHitData certainMiss = new ToHitData(TargetRoll.AUTOMATIC_FAIL, "test");
        CapitalMissileHandler handler = new CapitalMissileHandler(certainMiss, attack, game, gameManager);
        Vector<Report> reports = new Vector<>();
        handler.handle(GamePhase.FIRING, reports);

        boolean reportedMiss = false;
        boolean reportedHit = false;
        for (Report report : reports) {
            reportedMiss |= (report.messageId == REPORT_MISSES);
            reportedHit |= (report.messageId == REPORT_HITS);
        }
        assertTrue(reportedMiss, "the miss is reported");
        assertFalse(reportedHit, "a miss must not also report hits");
    }
}
