/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.equipment.*;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for MissileWeaponHandler.
 * <p>
 * Test to verify that with Playtest 3, AMS can engage twice
 *
 * @since 2025-12-14
 */
public class MissileWeaponsHandlerTest {
    
    private Game game;
    private int nextEntityId = 1;
    private TWGameManager gameManager;
    private ToHitData toHit;
    private WeaponAttackAction weaponAttack;
    private Coords targetCoords;
    private HitData hitData;
    private WeaponMounted mountedWeapon;
    private WeaponMounted mockWeaponTwo;
    private WeaponType weaponType = (WeaponType) EquipmentType.get("ISLRM20");
    private WeaponType AMSWeaponType = (WeaponType) EquipmentType.get("ISAMS");
    private AmmoType AMSammo = (AmmoType) EquipmentType.get("IS Ammo AMS");
    private WeaponMounted mountedAMS;
    private Entity attacker;
    private Entity target;
    private Mounted<?> lrmOne;
    private Mounted<?> lrmTwo;
    private Mounted<?> lrmThree;
    private WeaponMounted amsMount;

    private Player aPlayer;
    private Player dPlayer;
    

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        nextEntityId=1;
        
        gameManager = new TWGameManager();
        game = gameManager.getGame();

        game.getOptions().getOption(OptionsConstants.PLAYTEST_3).setValue(true);

        aPlayer = new Player(0, "Attacker");
        dPlayer = new Player(1, "Defender");
        game.addPlayer(aPlayer.getId(), aPlayer);
        game.addPlayer(dPlayer.getId(), dPlayer);
        // Configure board
        Board mockBoard = new Board();
        Hex mockHex = new Hex();
        
    }

    private Entity createAttackerEntity() {
        Entity entity = new BipedMek();
        entity.setGame(game);
        entity.setId(nextEntityId++);
        entity.setChassis("Test Mek Attacker");
        entity.setModel("Attacker");

        Crew crew = new Crew(CrewType.SINGLE);
        entity.setCrew(crew);
        entity.setOwner(game.getPlayer(0));
        entity.setWeight(50.0);
        entity.setOriginalWalkMP(5);

        // Add two LRM20s
        try {
            lrmOne = entity.addEquipment(weaponType, Mek.LOC_RIGHT_TORSO);
            lrmTwo = entity.addEquipment(weaponType, Mek.LOC_LEFT_TORSO);
            lrmThree = entity.addEquipment(weaponType, Mek.LOC_RIGHT_TORSO);
        } catch (Exception e) {
            fail("Failed to add LRMs: " + e.getMessage());
        }

        return entity;
    }

    private Entity createTargetEntity() {
        Entity entity = new BipedMek();
        entity.setGame(game);
        entity.setId(nextEntityId++);
        entity.setChassis("Target Mek");
        entity.setModel("Target");

        Crew crew = new Crew(CrewType.SINGLE);
        entity.setCrew(crew);
        entity.setOwner(game.getPlayer(1));
        entity.setWeight(50.0);
        entity.setOriginalWalkMP(5);

        // Add AMS
        try {
            //AmmoType amsAmmo = AmmoType.get("AMMO");
            Mounted<?> amsMounted = entity.addEquipment(AMSWeaponType, Mek.LOC_CENTER_TORSO);
            amsMount = (WeaponMounted) amsMounted;
            Mounted<?> amsAmmoMount = entity.addEquipment(AMSammo, Mek.LOC_LEFT_TORSO);
            amsMount.setLinked(amsAmmoMount);
        } catch (Exception e) {
            fail("Failed to add AMS: " + e.getMessage());
        }

        return entity;
    }
    /**
     * Test that zero-damage swarm attacks against Meks still roll for critical hits. Per BattleTech rules, "critical
     * hits are a separate outcome from damage".
     */
    @Test
    void testAMSWorksForPlaytestThree() throws EntityLoadingException {

        attacker = createAttackerEntity();
        target = createTargetEntity();
        game.addEntity(attacker);
        game.addEntity(target);
        // create to-hit, create attack action
        
        Coords attackerPostion = new Coords(1,1);
        Coords targetPositon = new Coords(1,8);
        
        attacker.setPosition(attackerPostion);
        target.setPosition(targetPositon);
        attacker.setFacing(0);
        target.setFacing(3);
        
        weaponAttack = new WeaponAttackAction(attacker.getId(), target.getId(), attacker.getEquipmentNum(lrmOne));
        weaponAttack.addCounterEquipment(amsMount);
        hitData = new HitData(Mek.LOC_CENTER_TORSO, false);
        
        toHit = new ToHitData();
                
        MissileWeaponHandler handler = new MissileWeaponHandler(toHit, weaponAttack, game, gameManager);

        Vector<Report> reports = new Vector<>();
        
        
        // Call handle for getAMSHitsMod.
        int AMSmod = 0;
        AMSmod = handler.getAMSHitsMod(reports);
        
        assertEquals(-4, AMSmod, "AMS did not engage");
        
        // first call should return -4

        weaponAttack = new WeaponAttackAction(attacker.getId(), target.getId(), attacker.getEquipmentNum(lrmTwo));
        weaponAttack.addCounterEquipment(amsMount);
        
        handler = new MissileWeaponHandler(toHit, weaponAttack, game, gameManager);
        AMSmod = handler.getAMSHitsMod(reports);
        
        assertEquals( -4, AMSmod, "AMS did not engage a 2nd time");
        // second call should return -4

        weaponAttack = new WeaponAttackAction(attacker.getId(), target.getId(), attacker.getEquipmentNum(lrmThree));
        weaponAttack.addCounterEquipment(amsMount);

        handler = new MissileWeaponHandler(toHit, weaponAttack, game, gameManager);
        AMSmod = handler.getAMSHitsMod(reports);
        
        assertEquals(0, AMSmod, "AMS triggered when it shouldn't have");
        // third call should return 0.
    }
}
