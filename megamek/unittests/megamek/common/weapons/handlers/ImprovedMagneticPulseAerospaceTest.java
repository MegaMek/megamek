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
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.weapons.Weapon;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * IMP iATM missiles hit fighters as they hit 'Meks: 1 point per missile, and the interference effects per 3 missiles
 * that strike (IO p. 67). Against an airborne target the missiles used to go uncounted, so the attack did no damage
 * and had no effect. Aerospace units cannot load IMP ({@code AmmoType.canAeroUse()}), so the attacker here is a
 * 'Mek firing ground-to-air.
 */
class ImprovedMagneticPulseAerospaceTest extends GameBoardTestCase {

    private static final int REPORT_NO_DAMAGE = 3365;
    private static final int REPORT_IMP_INTERFERENCE = 3349;
    /** An iATM 12 fired directly hits with all 12 missiles, 1 point each. */
    private static final int IATM_12_IMP_DAMAGE = 12;
    /** 12 missiles is four sets of 3, capped at +2 to-hit (IO p. 67). */
    private static final int MAXIMUM_IMP_TO_HIT_MODIFIER = 2;
    private static final int STARTING_ARMOR = 30;

    static {
        initializeBoard("IMP_AEROSPACE_BOARD", """
              size 16 17
              hex 0501 0 "" ""
              hex 0505 0 "" ""
              end"""
        );
    }

    private TWGameManager gameManager;
    private Game game;
    private Player targetOwner;
    private BipedMek attacker;
    private Mounted<?> launcher;

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
        game.setBoard(getBoard("IMP_AEROSPACE_BOARD"));
        Player attackerOwner = new Player(0, "Attacker");
        targetOwner = new Player(1, "Defender");
        attackerOwner.setTeam(1);
        targetOwner.setTeam(2);
        game.addPlayer(0, attackerOwner);
        game.addPlayer(1, targetOwner);

        attacker = createMek(attackerOwner, "Attacker");
        launcher = attacker.addEquipment(EquipmentType.get("CLiATM12"), Mek.LOC_RIGHT_TORSO);
        Mounted<?> ammo = attacker.addEquipment(EquipmentType.get("Clan Ammo iATM-12 IMP"), Mek.LOC_LEFT_TORSO);
        launcher.setLinked(ammo);
        game.addEntity(attacker);
        attacker.setPosition(new Coords(4, 0));
        attacker.setFacing(3);
        attacker.setDeployed(true);
    }

    private BipedMek createMek(Player owner, String model) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(game.getNextEntityId());
        mek.setChassis("Test Mek");
        mek.setModel(model);
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.setOwner(owner);
        mek.setWeight(50.0);
        mek.autoSetInternal();
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(STARTING_ARMOR, location);
            if (mek.hasRearArmor(location)) {
                mek.initializeRearArmor(STARTING_ARMOR, location);
            }
        }
        return mek;
    }

    private AeroSpaceFighter createFighter(Player owner, String model) {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.setGame(game);
        fighter.setId(game.getNextEntityId());
        fighter.setChassis("Test Fighter");
        fighter.setModel(model);
        fighter.setCrew(new Crew(CrewType.SINGLE));
        fighter.setOwner(owner);
        fighter.setWeight(50);
        return fighter;
    }

    private void placeTarget(Entity target) {
        game.addEntity(target);
        target.setPosition(new Coords(4, 4));
        target.setFacing(0);
        target.setDeployed(true);
    }

    private Vector<Report> fireImp(Entity target) {
        WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), target.getId(),
              attacker.getEquipmentNum(launcher));
        AttackHandler handler = ((Weapon) launcher.getType())
              .getCorrectHandler(new ToHitData(2, "test"), attack, game, gameManager);
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

    private static int remainingArmor(Entity entity) {
        int remaining = 0;
        for (int location = 0; location < entity.locations(); location++) {
            remaining += Math.max(0, entity.getArmor(location));
            if (entity.hasRearArmor(location)) {
                remaining += Math.max(0, entity.getArmor(location, true));
            }
        }
        return remaining;
    }

    @Test
    void groundToAirImpHitsAFighterWithDamageAndInterference() {
        AeroSpaceFighter target = createFighter(targetOwner, "Target");
        for (int location = 0; location < target.locations(); location++) {
            target.initializeArmor(STARTING_ARMOR, location);
        }
        target.setAltitude(3);
        placeTarget(target);
        int armorBefore = remainingArmor(target);

        Vector<Report> reports = fireImp(target);

        assertTrue(target.isAirborne(), "the target is airborne");
        assertEquals(IATM_12_IMP_DAMAGE, armorBefore - remainingArmor(target), "1 point per missile");
        assertTrue(hasReport(reports, REPORT_IMP_INTERFERENCE), "the IMP interference is reported");
        assertEquals(MAXIMUM_IMP_TO_HIT_MODIFIER, target.getImpToHitModifier(), "12 missiles reach the +2 cap");
        assertFalse(hasReport(reports, REPORT_NO_DAMAGE), "the attack is not reported as doing no damage");
    }

    @Test
    void impAgainstAMekIsUnchanged() {
        BipedMek target = createMek(targetOwner, "Target");
        placeTarget(target);
        int armorBefore = remainingArmor(target);

        Vector<Report> reports = fireImp(target);

        assertEquals(IATM_12_IMP_DAMAGE, armorBefore - remainingArmor(target), "1 point per missile");
        assertTrue(hasReport(reports, REPORT_IMP_INTERFERENCE), "the IMP interference is reported");
        assertEquals(MAXIMUM_IMP_TO_HIT_MODIFIER, target.getImpToHitModifier(), "12 missiles reach the +2 cap");
    }
}
