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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for MissileWeaponHandler.
 * <p>
 * Test to verify that with Playtest 3, AMS can engage twice.
 *
 * @since 2025-12-14
 */
public class MissileWeaponsHandlerTest {
    
    private Game game;
    private int nextEntityId = 1;
    private TWGameManager gameManager;
    private ToHitData toHit;
    private WeaponAttackAction weaponAttack;
    private HitData hitData;
    private WeaponType weaponType = (WeaponType) EquipmentType.get("ISLRM20");
    private WeaponType AMSWeaponType = (WeaponType) EquipmentType.get("ISAMS");
    private AmmoType AMSammo = (AmmoType) EquipmentType.get("IS Ammo AMS");
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
        
        // create the game
        game = new Game();
        nextEntityId=1;
        
        gameManager = new TWGameManager();
        game = gameManager.getGame();

        // Set Playtest3 option to True to be able to test AMS shooting twice
        game.getOptions().getOption(OptionsConstants.PLAYTEST_3).setValue(true);

        // Instantiate the players
        aPlayer = new Player(0, "Attacker");
        dPlayer = new Player(1, "Defender");
        game.addPlayer(aPlayer.getId(), aPlayer);
        game.addPlayer(dPlayer.getId(), dPlayer);
     }

     // Use to create the attacker
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

        // Add three LRM20s
        try {
            lrmOne = entity.addEquipment(weaponType, Mek.LOC_RIGHT_TORSO);
            lrmTwo = entity.addEquipment(weaponType, Mek.LOC_LEFT_TORSO);
            lrmThree = entity.addEquipment(weaponType, Mek.LOC_RIGHT_TORSO);
        } catch (Exception e) {
            fail("Failed to add LRMs: " + e.getMessage());
        }

        return entity;
    }

    // Create the target that mounts AMS
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
        
        try {
            // Add AMS, ammo for the AMS, and link the two
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
     * Test for AMS with playtest three
     */
    @Test
    void testAMSWorksForPlaytestThree() throws EntityLoadingException {

        // Create the meks and set their positions
        attacker = createAttackerEntity();
        target = createTargetEntity();
        game.addEntity(attacker);
        game.addEntity(target);
        
        Coords attackerPosition = new Coords(1,1);
        Coords targetPosition = new Coords(1,8);
        
        attacker.setPosition(attackerPosition);
        target.setPosition(targetPosition);
        attacker.setFacing(0);
        target.setFacing(3);

        hitData = new HitData(Mek.LOC_CENTER_TORSO, false);
        toHit = new ToHitData();
        
        // Setup first weapon attack and AMS ready.
        weaponAttack = new WeaponAttackAction(attacker.getId(), target.getId(), attacker.getEquipmentNum(lrmOne));
        weaponAttack.addCounterEquipment(amsMount);
        
        MissileWeaponHandler handler = new MissileWeaponHandler(toHit, weaponAttack, game, gameManager);
        Vector<Report> reports = new Vector<>();
        
        // Call getAMSHitsMod, which is what determines if AMS can shoot or not
        int AMSmod = 0;
        AMSmod = handler.getAMSHitsMod(reports);

        // first call should return -4
        assertEquals(-4, AMSmod, "AMS did not engage");
        // End first AMS test
        
        // Setup second AMS test shot. Will pass with Playtest 3, and fail without
        weaponAttack = new WeaponAttackAction(attacker.getId(), target.getId(), attacker.getEquipmentNum(lrmTwo));
        weaponAttack.addCounterEquipment(amsMount);
        
        handler = new MissileWeaponHandler(toHit, weaponAttack, game, gameManager);
        AMSmod = handler.getAMSHitsMod(reports);

        // second call should return -4. If 0 is returned, the playtest did not work or is not enabled.
        assertEquals( -4, AMSmod, "AMS did not engage a 2nd time");

        // Setup 3rd AMS test. This should always return 0 (no AMS) if multiAMS is not enabled.
        weaponAttack = new WeaponAttackAction(attacker.getId(), target.getId(), attacker.getEquipmentNum(lrmThree));
        weaponAttack.addCounterEquipment(amsMount);

        handler = new MissileWeaponHandler(toHit, weaponAttack, game, gameManager);
        AMSmod = handler.getAMSHitsMod(reports);
        
        // This should return 0, showing we did not engage.
        assertEquals(0, AMSmod, "AMS triggered when it shouldn't have");

        // End testing of AMS for playtest3
    }
}
