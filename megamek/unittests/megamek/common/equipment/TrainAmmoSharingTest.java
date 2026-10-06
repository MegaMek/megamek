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

package megamek.common.equipment;

import static megamek.testUtilities.MMTestUtilities.getEntityForUnitTesting;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for ammunition shared along a tractor-and-trailer train (issue #8520).
 *
 * <p>Two behaviors are covered. First, which units may supply ammo to which - the client offers this set in the ammo
 * dropdown and the server validates against it. Second, the repair of weapon links that point at another unit's ammo
 * bin: packets are Java-serialized object graphs and a {@link Mounted} holds a hard reference to its owning entity, so
 * such a link arrives as a detached duplicate of the carrier rather than the unit the game knows about.</p>
 */
class TrainAmmoSharingTest {

    private static final int TRACTOR_ID = 1;
    private static final int TRAILER_ID = 2;
    private static final int UNCONNECTED_ID = 3;

    private Game game;
    private Entity tractor;
    private Entity canonicalTrailer;
    private Entity unconnected;

    @BeforeEach
    void setUp() throws Exception {
        Player owner = new Player(0, "Owner");

        game = new Game();
        game.addPlayer(0, owner);

        tractor = loadBulldog(TRACTOR_ID, owner, false);
        canonicalTrailer = loadBulldog(TRAILER_ID, owner, true);
        unconnected = loadBulldog(UNCONNECTED_ID, owner, true);

        game.addEntity(tractor);
        game.addEntity(canonicalTrailer);
        game.addEntity(unconnected);

        // Hitch through towUnit rather than setting the neighbour links by hand, so the train membership the sharing
        // rule reads is populated the same way a game populates it.
        tractor.towUnit(canonicalTrailer.getId());
    }

    /** A Bulldog fitted with a trailer hitch, so it can take part in a train. */
    private Entity loadBulldog(int entityId, Player owner, boolean isTrailer) throws Exception {
        Entity entity = getEntityForUnitTesting("Bulldog Medium Tank", true);
        assertNotNull(entity, "Bulldog Medium Tank not found");
        entity.setId(entityId);
        entity.setOwner(owner);
        if (entity instanceof Tank vehicle) {
            vehicle.setTrailer(isTrailer);
            vehicle.addEquipment(EquipmentType.get(EquipmentTypeLookup.HITCH), Tank.LOC_BODY);
            vehicle.setTrailerHitches();
        }
        return entity;
    }

    private WeaponMounted ammoUsingWeapon(Entity entity) {
        for (WeaponMounted weapon : entity.getTotalWeaponList()) {
            if (!entity.getAmmo(weapon).isEmpty()) {
                return weapon;
            }
        }
        throw new AssertionError("No ammo-using weapon found on " + entity.getDisplayName());
    }

    /** A second instance of the trailer with the same id, standing in for one that arrived inside a packet. */
    private Entity duplicateTrailer() throws Exception {
        Entity duplicate = loadBulldog(TRAILER_ID, game.getPlayer(0), true);
        assertNotSame(canonicalTrailer, duplicate);
        return duplicate;
    }

    // --- which units may share ammo ---

    @Test
    void aUnitMayAlwaysUseItsOwnAmmo() {
        assertTrue(TrainAmmoSharing.canShareAmmoWith(tractor, tractor));
    }

    @Test
    void connectedTrailersMayShareBothWays() {
        assertTrue(TrainAmmoSharing.canShareAmmoWith(tractor, canonicalTrailer),
              "A tractor may draw from the trailer it tows");
        assertTrue(TrainAmmoSharing.canShareAmmoWith(canonicalTrailer, tractor),
              "A trailer may draw from the unit towing it");
    }

    @Test
    void anUnconnectedUnitMayNotShare() {
        assertFalse(TrainAmmoSharing.canShareAmmoWith(tractor, unconnected));
        assertFalse(TrainAmmoSharing.canShareAmmoWith(unconnected, tractor));
    }

    @Test
    void sharedAmmoListsOwnBinsFirstThenTheConnectedUnit() {
        List<AmmoMounted> shared = TrainAmmoSharing.getSharedAmmo(tractor);

        int ownBinCount = tractor.getAmmo().size();
        assertEquals(ownBinCount + canonicalTrailer.getAmmo().size(), shared.size());
        for (int index = 0; index < ownBinCount; index++) {
            assertSame(tractor.getAmmo().get(index), shared.get(index), "Own bins come first");
        }
        assertSame(canonicalTrailer.getAmmo().get(0), shared.get(ownBinCount),
              "The towed unit's bins follow");
    }

    @Test
    void sharingReachesOneCouplingAndNoFurther() throws Exception {
        // Gun, then three ammunition carriages, as a Mobile Long Tom convoy is built. Only the carriage hitched to
        // the gun can feed it: "Only vehicles directly coupled can share ammo" (Xotl, forum topic 74296). The other
        // two carry rounds the gun cannot reach, which is the ruling rather than a defect.
        Entity secondCarriage = loadBulldog(4, game.getPlayer(0), true);
        Entity thirdCarriage = loadBulldog(5, game.getPlayer(0), true);
        game.addEntity(secondCarriage);
        game.addEntity(thirdCarriage);
        tractor.towUnit(secondCarriage.getId());
        tractor.towUnit(thirdCarriage.getId());

        assertFalse(TrainAmmoSharing.canShareAmmoWith(tractor, secondCarriage),
              "The gun does not reach the second carriage, two hops back");
        assertFalse(TrainAmmoSharing.canShareAmmoWith(tractor, thirdCarriage),
              "The gun does not reach the third carriage, three hops back");
        assertFalse(TrainAmmoSharing.canShareAmmoWith(thirdCarriage, tractor),
              "Nor does the far carriage reach the gun");
        assertFalse(TrainAmmoSharing.canShareAmmoWith(canonicalTrailer, thirdCarriage),
              "One trailer may not pull from another it is not hitched to");

        assertTrue(TrainAmmoSharing.canShareAmmoWith(canonicalTrailer, secondCarriage),
              "Carriages hitched to each other still share");

        List<AmmoMounted> shared = TrainAmmoSharing.getSharedAmmo(tractor);
        assertEquals(tractor.getAmmo().size() + canonicalTrailer.getAmmo().size(), shared.size(),
              "Only the first carriage's bins are offered to the gun");
    }

    @Test
    void aUnitInAnotherTrainIsStillOutOfReach() throws Exception {
        Entity otherTractor = loadBulldog(6, game.getPlayer(0), false);
        Entity otherTrailer = loadBulldog(7, game.getPlayer(0), true);
        game.addEntity(otherTractor);
        game.addEntity(otherTrailer);
        otherTractor.towUnit(otherTrailer.getId());

        assertFalse(TrainAmmoSharing.canShareAmmoWith(tractor, otherTractor),
              "A separate train is not a shared supply");
        assertFalse(TrainAmmoSharing.canShareAmmoWith(tractor, otherTrailer));
    }

    @Test
    void sharedAmmoOfALoneUnitIsJustItsOwn() {
        List<AmmoMounted> shared = TrainAmmoSharing.getSharedAmmo(unconnected);

        assertEquals(unconnected.getAmmo().size(), shared.size());
    }

    // --- repairing links that crossed a packet boundary ---

    @Test
    void linkToDuplicateCarrierIsRepointedAtTheCanonicalBin() throws Exception {
        Entity duplicate = duplicateTrailer();
        WeaponMounted launcher = ammoUsingWeapon(tractor);
        AmmoMounted staleBin = duplicate.getAmmo().get(0);
        launcher.setLinked(staleBin);

        tractor.setGame(game);

        AmmoMounted relinked = ammoUsingWeapon(tractor).getLinkedAmmo();
        assertNotSame(staleBin, relinked, "The detached copy must not survive");
        assertSame(canonicalTrailer.getAmmo().get(0), relinked,
              "The link must point at the bin the game knows about");
        assertSame(canonicalTrailer, relinked.getEntity());
    }

    @Test
    void linkToOwnAmmoIsLeftAlone() {
        WeaponMounted launcher = ammoUsingWeapon(tractor);
        AmmoMounted ownBin = tractor.getAmmo().get(1);
        launcher.setLinked(ownBin);

        tractor.setGame(game);

        assertSame(ownBin, ammoUsingWeapon(tractor).getLinkedAmmo(),
              "A link to this unit's own ammo must be untouched");
    }

    @Test
    void linkToDepartedCarrierIsCleared() throws Exception {
        Entity duplicate = duplicateTrailer();
        WeaponMounted launcher = ammoUsingWeapon(tractor);
        launcher.setLinked(duplicate.getAmmo().get(0));

        // The trailer is destroyed and removed before this unit's update is applied.
        game.removeEntity(TRAILER_ID, IEntityRemovalConditions.REMOVE_DEVASTATED);

        tractor.setGame(game);

        assertNull(ammoUsingWeapon(tractor).getLinkedAmmo(),
              "A link to a unit that has left the game must be dropped");
    }

    // --- links left behind when the train uncouples ---

    @Test
    void aLinkToATrailersAmmoIsDroppedWhenTheTrainUncouples() {
        WeaponMounted launcher = ammoUsingWeapon(tractor);
        AmmoMounted trailerBin = canonicalTrailer.getAmmo(launcher).get(0);
        launcher.setLinked(trailerBin);

        tractor.disconnectUnit(canonicalTrailer.getId());

        assertNotSame(trailerBin, launcher.getLinkedAmmo(),
              "An uncoupled trailer's ammo must not stay loaded");
        assertSame(tractor, launcher.getLinkedAmmo().getEntity(),
              "The weapon falls back to a bin this unit still owns");
        assertEquals(trailerBin.getType(), launcher.getLinkedAmmo().getType(),
              "The replacement bin holds the munition that was loaded");
    }

    @Test
    void aTrailerLosesItsLinkToTheTractorsAmmoToo() {
        WeaponMounted launcher = ammoUsingWeapon(canonicalTrailer);
        AmmoMounted tractorBin = tractor.getAmmo(launcher).get(0);
        launcher.setLinked(tractorBin);

        tractor.disconnectUnit(canonicalTrailer.getId());

        assertNotSame(tractorBin, launcher.getLinkedAmmo(),
              "The split is checked from both sides, not just the tractor's");
    }

    @Test
    void aLinkIsClearedWhenNothingLegalIsLeftToFire() {
        WeaponMounted launcher = ammoUsingWeapon(tractor);
        launcher.setLinked(canonicalTrailer.getAmmo(launcher).get(0));
        // The tractor is a gun carriage with no rounds of its own, so nothing can replace the trailer's bin.
        for (AmmoMounted ownBin : tractor.getAmmo()) {
            ownBin.setShotsLeft(0);
        }

        tractor.disconnectUnit(canonicalTrailer.getId());

        assertNull(launcher.getLinkedAmmo(),
              "With no legal bin the weapon is left unloaded rather than firing a unit that has gone");
    }

    /** A guard against over-reaching: this one passes with the drop removed, the other three do not. */
    @Test
    void aLinkWithinTheRemainingTrainSurvivesTheSplit() throws Exception {
        Entity secondCarriage = loadBulldog(4, game.getPlayer(0), true);
        game.addEntity(secondCarriage);
        tractor.towUnit(secondCarriage.getId());

        WeaponMounted launcher = ammoUsingWeapon(tractor);
        AmmoMounted stillCoupledBin = canonicalTrailer.getAmmo(launcher).get(0);
        launcher.setLinked(stillCoupledBin);

        // Drop only the rear carriage. The first trailer stays hitched.
        tractor.disconnectUnit(secondCarriage.getId());

        assertSame(stillCoupledBin, launcher.getLinkedAmmo(),
              "Ammo on a unit that is still coupled must stay loaded");
    }
}
