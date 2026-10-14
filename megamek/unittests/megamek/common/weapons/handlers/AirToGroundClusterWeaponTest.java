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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import megamek.common.units.ConvInfantry;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;
import megamek.common.weapons.Weapon;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * An airborne fighter's cluster weapons fired at a ground unit apply their Attack Value in groups, in place of a roll
 * on the Cluster Hits Table (TW pp. 244-245), the same as its missiles do (issue #9218).
 */
class AirToGroundClusterWeaponTest extends GameBoardTestCase {

    private static final int REPORT_HITS_ATTACK_VALUE = 3391;
    private static final int REPORT_CLUSTER_HITS = 3325;
    private static final Pattern ATTACK_VALUE = Pattern.compile("attack value (\\d+)");
    private static final int LB_10X_ATTACK_VALUE = 6;
    private static final int LB_10X_SLUG_DAMAGE = 10;

    static {
        initializeBoard("AIR_TO_GROUND_CLUSTER_BOARD", """
              size 16 17
              hex 0501 0 "" ""
              hex 0505 0 "" ""
              end"""
        );
    }

    private TWGameManager gameManager;
    private Game game;
    private AeroSpaceFighter attacker;
    private Player targetOwner;

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
        game.setBoard(getBoard("AIR_TO_GROUND_CLUSTER_BOARD"));
        Player attackerOwner = new Player(0, "Attacker");
        targetOwner = new Player(1, "Defender");
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
        game.addEntity(attacker);
        attacker.setPosition(new Coords(4, 0));
        attacker.setAltitude(3);
        attacker.setDeployed(true);
    }

    private BipedMek addMekTarget() {
        BipedMek target = new BipedMek();
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
            if (target.hasRearArmor(location)) {
                target.initializeRearArmor(20, location);
            }
        }
        addTarget(target);
        return target;
    }

    private ConvInfantry addInfantryTarget() {
        ConvInfantry target = new ConvInfantry();
        target.setGame(game);
        target.setId(game.getNextEntityId());
        target.setChassis("Test Platoon");
        target.setModel("Target");
        target.setMovementMode(EntityMovementMode.INF_LEG);
        target.setSquadSize(7);
        target.setSquadCount(4);
        target.autoSetInternal();
        target.setOwner(targetOwner);
        addTarget(target);
        return target;
    }

    private void addTarget(Entity target) {
        game.addEntity(target);
        target.setPosition(new Coords(4, 4));
        target.setFacing(0);
        target.setDeployed(true);
    }

    private Mounted<?> mountWeapon(String weaponName, String ammoName) throws Exception {
        Mounted<?> weapon = attacker.addEquipment(EquipmentType.get(weaponName), AeroSpaceFighter.LOC_NOSE);
        Mounted<?> ammo = attacker.addEquipment(EquipmentType.get(ammoName), AeroSpaceFighter.LOC_NOSE);
        weapon.setLinked(ammo);
        return weapon;
    }

    /**
     * Fires the weapon with the handler the game itself would pick for its loaded ammo.
     */
    private Vector<Report> fire(Mounted<?> weapon, Entity target) {
        WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), target.getId(),
              attacker.getEquipmentNum(weapon));
        AttackHandler handler = ((Weapon) weapon.getType())
              .getCorrectHandler(new ToHitData(2, "test"), attack, game, gameManager);
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

    private static int reportedAttackValue(Vector<Report> reports) {
        Report report = findReport(reports, REPORT_HITS_ATTACK_VALUE);
        assertNotNull(report, "the hit line reports the attack value");
        Matcher matcher = ATTACK_VALUE.matcher(report.text());
        assertTrue(matcher.find(), "the line shows a number: " + report.text());
        return Integer.parseInt(matcher.group(1));
    }

    private static int remainingArmorAndStructure(Mek mek) {
        int remaining = 0;
        for (int location = 0; location < mek.locations(); location++) {
            remaining += mek.getArmor(location) + mek.getInternal(location);
            if (mek.hasRearArmor(location)) {
                remaining += mek.getArmor(location, true);
            }
        }
        return remaining;
    }

    @Test
    void lbxClusterUsesTheAttackValueAgainstAMek() throws Exception {
        Mounted<?> lbx = mountWeapon("ISLBXAC10", "IS LB 10-X Cluster Ammo");
        BipedMek target = addMekTarget();
        int before = remainingArmorAndStructure(target);

        Vector<Report> reports = fire(lbx, target);

        assertFalse(hasReport(reports, REPORT_CLUSTER_HITS), "no roll on the Cluster Hits Table");
        assertEquals(LB_10X_ATTACK_VALUE, reportedAttackValue(reports), "the LB 10-X attack value is shown");
        assertEquals(LB_10X_ATTACK_VALUE, before - remainingArmorAndStructure(target), "exactly the attack value is dealt");
    }

    @Test
    void lbxSlugIsUnchanged() throws Exception {
        Mounted<?> lbx = mountWeapon("ISLBXAC10", "IS LB 10-X AC Ammo");
        BipedMek target = addMekTarget();
        int before = remainingArmorAndStructure(target);

        Vector<Report> reports = fire(lbx, target);

        assertFalse(hasReport(reports, REPORT_HITS_ATTACK_VALUE), "slug ammo is not a cluster weapon");
        assertEquals(LB_10X_SLUG_DAMAGE, before - remainingArmorAndStructure(target), "a slug still does its full damage");
    }

    @Test
    void aeroSanityKeepsTheClusterRoll() throws Exception {
        game.getOptions().getOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY).setValue(true);
        Mounted<?> lbx = mountWeapon("ISLBXAC10", "IS LB 10-X Cluster Ammo");
        BipedMek target = addMekTarget();

        Vector<Report> reports = fire(lbx, target);

        assertTrue(hasReport(reports, REPORT_CLUSTER_HITS), "Aero Sanity rolls on the Cluster Hits Table");
        assertFalse(hasReport(reports, REPORT_HITS_ATTACK_VALUE), "no attack value line under Aero Sanity");
    }

    /**
     * Attacks on conventional infantry use the Non-Infantry Weapon Damage Against Infantry Table (TW p. 215), so they
     * keep the ground resolution.
     */
    @Test
    void conventionalInfantryKeepTheInfantryTable() throws Exception {
        Mounted<?> lbx = mountWeapon("ISLBXAC10", "IS LB 10-X Cluster Ammo");
        ConvInfantry target = addInfantryTarget();

        Vector<Report> reports = fire(lbx, target);

        assertFalse(hasReport(reports, REPORT_HITS_ATTACK_VALUE), "infantry are not hit with the attack value");
    }

    @Test
    void ultraAutocannonUsesTheAttackValueAgainstAMek() throws Exception {
        Mounted<?> ultra = mountWeapon("ISUltraAC10", "IS Ultra AC/10 Ammo");
        BipedMek target = addMekTarget();
        int before = remainingArmorAndStructure(target);

        Vector<Report> reports = fire(ultra, target);

        assertFalse(hasReport(reports, REPORT_CLUSTER_HITS), "no roll on the Cluster Hits Table");
        assertEquals(reportedAttackValue(reports), before - remainingArmorAndStructure(target),
              "exactly the attack value is dealt");
    }

    private static boolean hasReport(Vector<Report> reports, int messageId) {
        return findReport(reports, messageId) != null;
    }
}
