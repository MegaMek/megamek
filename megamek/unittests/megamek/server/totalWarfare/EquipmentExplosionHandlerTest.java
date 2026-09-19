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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import megamek.common.CriticalSlot;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.options.GameOptions;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.server.Server;
import megamek.testUtilities.MMTestUtilities;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The gamemaster's explode request runs the same server resolution a critical hit does, so these tests drive a
 * real game manager: the bin empties, its slots are hit, the location takes the damage and the pilot the hits.
 */
class EquipmentExplosionHandlerTest {

    private TWGameManager gameManager;
    private Server server;
    private Entity mek;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws IOException {
        gameManager = new TWGameManager();
        Game game = gameManager.getGame();
        game.setOptions(new GameOptions());
        server = ServerFactory.createServer(gameManager);
        Player owner = new Player(0, "Owner");
        game.addPlayer(0, owner);

        mek = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(mek, "Test unit could not be loaded");
        mek.setGame(game);
        mek.setId(game.getNextEntityId());
        mek.setOwner(owner);
        game.addEntity(mek);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.die();
        }
    }

    private EquipmentExplosionHandler.Outcome explode(Mounted<?> mounted) {
        return new EquipmentExplosionHandler(gameManager).explode(mek, mounted);
    }

    /** A loaded ammo bin of the test unit. */
    private AmmoMounted loadedAmmoBin() {
        for (AmmoMounted ammoBin : mek.getAmmo()) {
            if (ammoBin.getUsableShotsLeft() > 0) {
                return ammoBin;
            }
        }
        throw new AssertionError("The test unit must carry a loaded ammo bin");
    }

    /** A weapon of the test unit that nothing can explode: a laser. */
    private WeaponMounted laser() {
        for (WeaponMounted weapon : mek.getWeaponList()) {
            if (weapon.getType().hasFlag(WeaponType.F_ENERGY)) {
                return weapon;
            }
        }
        throw new AssertionError("The test unit must carry a laser");
    }

    @Test
    void anAmmoBinExplodesLikeACriticalHit() {
        AmmoMounted ammoBin = loadedAmmoBin();
        int location = ammoBin.getLocation();
        int internalBefore = mek.getInternal(location);
        assertTrue(ammoBin.wouldExplodeWhenHit(), "A loaded bin is what the button is offered for");

        EquipmentExplosionHandler.Outcome outcome = explode(ammoBin);

        assertEquals(EquipmentExplosionHandler.Outcome.EXPLODED, outcome);
        assertEquals(0, ammoBin.getBaseShotsLeft(), "The explosion empties the bin");
        assertTrue(ammoBin.isHit(), "The bin is marked hit, as a critical hit marks it");
        assertTrue(binSlotsHit(ammoBin), "The bin's critical slots are marked hit");
        assertTrue(mek.getInternal(location) < internalBefore,
              "The explosion damage lands on the location's internal structure");
        assertTrue(mek.getCrew().getHits() > 0, "The pilot takes the explosion's hits");
        assertFalse(gameManager.getMainPhaseReport().isEmpty(), "The explosion is reported");
    }

    /** Puts the game in the movement phase with the given turns, the first of them current. */
    private void startMovementPhase(GameTurn... turns) {
        Game game = gameManager.getGame();
        game.setPhase(GamePhase.MOVEMENT);
        game.setTurnVector(List.of(turns));
        game.setTurnIndex(0, Player.PLAYER_NONE);
        assertNotNull(game.getTurn());
        assertTrue(game.getTurn().isValid(mek.getOwnerId(), mek, game), "The unit can move before the explosion");
    }

    /** Adds a second unit to the game for the given player, in place in the turn order. */
    private Entity addSecondUnit(Player owner) {
        Entity secondUnit = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(secondUnit, "Second test unit could not be loaded");
        Game game = gameManager.getGame();
        secondUnit.setGame(game);
        secondUnit.setId(game.getNextEntityId());
        secondUnit.setOwner(owner);
        game.addEntity(secondUnit);
        return secondUnit;
    }

    /** Pads the test unit so that an ammo explosion cannot reach its center torso. */
    private void padStructure() {
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(200, location);
            mek.initializeInternal(200, location);
        }
    }

    @Test
    void aSurvivedExplosionDuringTheUnitsOwnMovementTurnLeavesItsTurnValid() {
        startMovementPhase(new GameTurn(mek.getOwnerId()));
        padStructure();
        // with auto-eject on, an ammo explosion ejects the pilot, which is the other case below
        ((Mek) mek).setAutoEject(false);

        explode(loadedAmmoBin());

        Game game = gameManager.getGame();
        assertFalse(mek.isDoomed(), "The padded unit survives the blast");
        assertEquals(0, game.getTurnIndex(), "The unit keeps its turn");
        assertTrue(game.getTurn().isValid(mek.getOwnerId(), mek, game),
              "A unit that survives an explosion can still take its movement turn");
    }

    @Test
    void anEjectingExplosionHandsTheTurnOnWhenTheOwnerHasNoOtherUnit() {
        Player otherPlayer = new Player(1, "Other");
        Game game = gameManager.getGame();
        game.addPlayer(1, otherPlayer);
        Entity otherUnit = addSecondUnit(otherPlayer);
        startMovementPhase(new GameTurn(mek.getOwnerId()), new GameTurn(otherPlayer.getId()));
        padStructure();
        assertTrue(((Mek) mek).isAutoEject(), "Auto-eject is on by default, so the ammo explosion ejects the pilot");

        explode(loadedAmmoBin());

        assertTrue(mek.getCrew().isEjected(), "The pilot ejected");
        assertFalse(mek.isSelectableThisTurn(), "An ejected Mek is abandoned and cannot take a turn");
        assertEquals(1, game.getTurnIndex(),
              "Nobody of the owner's could take the turn, so it was skipped on to the other player");
        assertEquals(otherUnit, game.getFirstEntity(), "The other player's unit is now up");
    }

    @Test
    void anEjectingExplosionLeavesTheOwnersOtherUnitItsTurn() {
        Game game = gameManager.getGame();
        Entity lanceMate = addSecondUnit(game.getPlayer(0));
        startMovementPhase(new GameTurn(mek.getOwnerId()), new GameTurn(mek.getOwnerId()));
        padStructure();

        explode(loadedAmmoBin());

        assertTrue(mek.getCrew().isEjected(), "The pilot ejected");
        assertEquals(0, game.getTurnIndex(), "The lance mate can take the current turn, so it stays");
        assertEquals(lanceMate, game.getFirstEntity(), "The lance mate is the unit left to take it");
        assertEquals(1, game.getTurnsList().size(),
              "The dead unit's own later turn is dropped, so the owner is not asked to move it");
    }

    @Test
    void anEmptyBinIsRefused() {
        AmmoMounted ammoBin = loadedAmmoBin();
        ammoBin.setShotsLeft(0);
        int internalBefore = mek.getInternal(ammoBin.getLocation());

        EquipmentExplosionHandler.Outcome outcome = explode(ammoBin);

        assertEquals(EquipmentExplosionHandler.Outcome.NOT_EXPLOSIVE, outcome,
              "An empty bin would explode for nothing");
        assertEquals(internalBefore, mek.getInternal(ammoBin.getLocation()));
        assertFalse(ammoBin.isHit());
    }

    @Test
    void aLaserIsRefused() {
        WeaponMounted laser = laser();
        assertFalse(laser.wouldExplodeWhenHit(), "Nothing explodes a laser, so no button is offered for it");

        EquipmentExplosionHandler.Outcome outcome = explode(laser);

        assertEquals(EquipmentExplosionHandler.Outcome.NOT_EXPLOSIVE, outcome);
        assertFalse(laser.isHit(), "A refused explosion leaves the weapon untouched");
    }

    @Test
    void destroyedEquipmentIsRefused() {
        AmmoMounted ammoBin = loadedAmmoBin();
        ammoBin.setDestroyed(true);
        int shotsBefore = ammoBin.getBaseShotsLeft();

        EquipmentExplosionHandler.Outcome outcome = explode(ammoBin);

        assertEquals(EquipmentExplosionHandler.Outcome.ALREADY_DESTROYED, outcome);
        assertEquals(shotsBefore, ammoBin.getBaseShotsLeft(), "A refused explosion changes nothing");
    }

    /** Whether every critical slot holding the bin is marked hit. */
    private boolean binSlotsHit(AmmoMounted ammoBin) {
        int location = ammoBin.getLocation();
        boolean foundSlot = false;
        for (int slot = 0; slot < mek.getNumberOfCriticalSlots(location); slot++) {
            CriticalSlot criticalSlot = mek.getCritical(location, slot);
            if ((criticalSlot != null) && (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT)
                  && ammoBin.equals(criticalSlot.getMount())) {
                foundSlot = true;
                if (!criticalSlot.isHit()) {
                    return false;
                }
            }
        }
        return foundSlot;
    }
}
