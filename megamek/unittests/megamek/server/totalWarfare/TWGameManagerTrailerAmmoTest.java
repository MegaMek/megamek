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

import static megamek.testUtilities.MMTestUtilities.getEntityForUnitTesting;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for trailer ammo sharing (issue #8520).
 *
 * <p>A unit towing an ammo trailer may fire the trailer's ammo. The ammo change packet therefore has to name the unit
 * that carries the bin, because the bin's equipment number is only meaningful on its own carrier. Before the fix the
 * client sent an equipment number looked up on the firing unit, which produced -1 for any trailer bin, so the server
 * dropped the selection and kept firing whatever was already linked.</p>
 *
 * <p>The server must also refuse a carrier that is not part of the same train, since the equipment number arrives
 * from the client.</p>
 */
class TWGameManagerTrailerAmmoTest {

    private static final int TRACTOR_ID = 1;
    private static final int TRAILER_ID = 2;
    private static final int UNCONNECTED_ID = 3;
    private static final int OWNER_CONNECTION_ID = 0;
    private static final int NO_REPORT = 0;

    private Game game;
    private TWGameManager gameManager;
    private Entity tractor;
    private Entity trailer;
    private Entity unconnected;

    @BeforeEach
    void setUp() throws Exception {
        Player owner = new Player(OWNER_CONNECTION_ID, "Owner");
        owner.setTeam(1);

        game = new Game();
        game.setPhase(GamePhase.FIRING);
        game.addPlayer(OWNER_CONNECTION_ID, owner);

        tractor = loadBulldog(TRACTOR_ID, owner, false);
        trailer = loadBulldog(TRAILER_ID, owner, true);
        unconnected = loadBulldog(UNCONNECTED_ID, owner, true);

        game.addEntity(tractor);
        game.addEntity(trailer);
        game.addEntity(unconnected);

        // Hook the trailer up the way a game does, so train membership is populated as well as the neighbour links.
        tractor.towUnit(trailer.getId());

        gameManager = mock(TWGameManager.class);
        doNothing().when(gameManager).entityUpdate(anyInt());
        when(gameManager.getGame()).thenReturn(game);
        doCallRealMethod().when(gameManager).setGame(any(Game.class));
        doCallRealMethod().when(gameManager).handlePacket(anyInt(), any(Packet.class));
        gameManager.setGame(game);
    }

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

    /** The first weapon on the unit with at least two bins it can draw from, so the choice is meaningful. */
    private WeaponMounted multiBinWeapon(Entity entity) {
        for (WeaponMounted weapon : entity.getTotalWeaponList()) {
            if (compatibleBins(entity, weapon).size() >= 2) {
                return weapon;
            }
        }
        throw new AssertionError("No weapon with two compatible ammo bins on " + entity.getDisplayName());
    }

    /** Every bin on the given unit that the weapon could actually be loaded with. */
    private List<AmmoMounted> compatibleBins(Entity entity, WeaponMounted weapon) {
        List<AmmoMounted> bins = new ArrayList<>();
        for (AmmoMounted ammo : entity.getAmmo()) {
            if (AmmoType.isAmmoValid(ammo, weapon.getType())) {
                bins.add(ammo);
            }
        }
        return bins;
    }

    private void sendAmmoChange(Entity shooter, WeaponMounted weapon, Entity ammoCarrier, AmmoMounted ammo) {
        gameManager.handlePacket(OWNER_CONNECTION_ID, new Packet(PacketCommand.ENTITY_AMMO_CHANGE,
              shooter.getId(),
              shooter.getEquipmentNum(weapon),
              ammoCarrier.getEquipmentNum(ammo),
              ammoCarrier.getId(),
              NO_REPORT));
    }

    @Test
    void trailerAmmoIsLinkedToTheTractorWeapon() {
        WeaponMounted launcher = multiBinWeapon(tractor);
        AmmoMounted trailerAmmo = compatibleBins(trailer, launcher).get(0);

        sendAmmoChange(tractor, launcher, trailer, trailerAmmo);

        assertSame(trailerAmmo, launcher.getLinkedAmmo(),
              "Weapon should fire the selected trailer bin");
    }

    @Test
    void ammoAheadInTheTrainIsLinkedToTheTrailerWeapon() {
        WeaponMounted launcher = multiBinWeapon(trailer);
        AmmoMounted tractorAmmo = compatibleBins(tractor, launcher).get(1);

        sendAmmoChange(trailer, launcher, tractor, tractorAmmo);

        assertSame(tractorAmmo, launcher.getLinkedAmmo(),
              "A trailer may draw from the unit towing it");
    }

    @Test
    void ownAmmoStillChanges() {
        WeaponMounted launcher = multiBinWeapon(tractor);
        AmmoMounted otherOwnBin = null;
        for (AmmoMounted bin : compatibleBins(tractor, launcher)) {
            if (bin != launcher.getLinkedAmmo()) {
                otherOwnBin = bin;
                break;
            }
        }
        assertNotNull(otherOwnBin, "Expected a second compatible bin on the tractor");

        sendAmmoChange(tractor, launcher, tractor, otherOwnBin);

        assertSame(otherOwnBin, launcher.getLinkedAmmo(),
              "Selecting a bin on the firing unit itself must keep working");
    }

    @Test
    void ammoOnAnUnconnectedUnitIsRejected() {
        WeaponMounted launcher = multiBinWeapon(tractor);
        AmmoMounted originalAmmo = launcher.getLinkedAmmo();
        AmmoMounted foreignAmmo = compatibleBins(unconnected, launcher).get(0);

        sendAmmoChange(tractor, launcher, unconnected, foreignAmmo);

        assertSame(originalAmmo, launcher.getLinkedAmmo(),
              "A unit not in the train must not supply ammo");
    }

    @Test
    void unknownCarrierIsRejected() {
        WeaponMounted launcher = multiBinWeapon(tractor);
        AmmoMounted originalAmmo = launcher.getLinkedAmmo();

        gameManager.handlePacket(OWNER_CONNECTION_ID, new Packet(PacketCommand.ENTITY_AMMO_CHANGE,
              tractor.getId(),
              tractor.getEquipmentNum(launcher),
              0,
              99,
              NO_REPORT));

        assertSame(originalAmmo, launcher.getLinkedAmmo(),
              "An unknown carrier id must leave the link alone");
    }

    @Test
    void unknownBinOnAValidCarrierIsRejected() {
        WeaponMounted launcher = multiBinWeapon(tractor);
        AmmoMounted originalAmmo = launcher.getLinkedAmmo();

        gameManager.handlePacket(OWNER_CONNECTION_ID, new Packet(PacketCommand.ENTITY_AMMO_CHANGE,
              tractor.getId(),
              tractor.getEquipmentNum(launcher),
              -1,
              trailer.getId(),
              NO_REPORT));

        assertSame(originalAmmo, launcher.getLinkedAmmo(),
              "An unresolvable bin must leave the link alone");
    }

    @Test
    void nonWeaponEquipmentIndexIsRejectedWithoutThrowing() {
        WeaponMounted launcher = multiBinWeapon(tractor);
        AmmoMounted originalAmmo = launcher.getLinkedAmmo();
        // Point the weapon index at an ammo bin. Both indices come from the client, so a bad one must be
        // rejected rather than escaping as a ClassCastException - nothing between here and the packet pump
        // catches RuntimeException, so a throw would kill the server's packet thread.
        int ammoBinIndex = tractor.getEquipmentNum(compatibleBins(tractor, launcher).get(0));

        gameManager.handlePacket(OWNER_CONNECTION_ID, new Packet(PacketCommand.ENTITY_AMMO_CHANGE,
              tractor.getId(),
              ammoBinIndex,
              ammoBinIndex,
              tractor.getId(),
              NO_REPORT));

        assertSame(originalAmmo, launcher.getLinkedAmmo(),
              "A non-weapon equipment index must leave the link alone");
    }

    @Test
    void trailerAmmoSurvivesRelinkOnTheTractor() {
        WeaponMounted launcher = multiBinWeapon(tractor);
        AmmoMounted trailerAmmo = compatibleBins(trailer, launcher).get(0);
        sendAmmoChange(tractor, launcher, trailer, trailerAmmo);

        // A server-side setGame pass must not disturb a link that already points at the canonical bin.
        tractor.setGame(game);

        assertSame(trailerAmmo, launcher.getLinkedAmmo(),
              "Relinking must be a no-op when the bin is already canonical");
        assertEquals(TRAILER_ID, launcher.getLinkedAmmo().getEntity().getId());
    }
}
