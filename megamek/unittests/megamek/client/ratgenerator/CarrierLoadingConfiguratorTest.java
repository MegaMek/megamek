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
package megamek.client.ratgenerator;

import static megamek.testUtilities.MMTestUtilities.getEntityForUnitTesting;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.Player;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers marking the units a generated force nests under a ship as carried by it.
 *
 * <p>The tree used throughout is the shape the generator produces: a lance of Meks on the ground, and under the
 * transport branch a Leopard with a Flight of two fighters attached to it, the fighter complement the Carried
 * Fighter Complement option adds.</p>
 */
class CarrierLoadingConfiguratorTest {

    private static final String DROPSHIP = "Leopard (2537)";
    private static final String FIGHTER = "Cheetah F-11";
    private static final String MEK = "Atlas AS7-D";
    private static final String TANK = "Bulldog Medium Tank";
    private static final String WARSHIP = "Aegis Heavy Cruiser (2372)";

    private Player owner;
    private ForceDescriptor root;
    private ForceDescriptor flight;
    private Entity leopard;
    private Entity firstFighter;
    private Entity secondFighter;
    private Entity atlas;

    @BeforeEach
    void buildForce() throws Exception {
        owner = new Player(0, "Owner");
        leopard = load(DROPSHIP, true);
        firstFighter = load(FIGHTER, true);
        secondFighter = load(FIGHTER, true);
        atlas = load(MEK, false);

        root = formation("Battalion");
        ForceDescriptor lance = formation("Lance");
        lance.addSubForce(unit(atlas));
        root.addSubForce(lance);

        ForceDescriptor transports = formation("Naval Units");
        ForceDescriptor ship = unit(leopard);
        flight = formation("Flight 1");
        flight.addSubForce(unit(firstFighter));
        flight.addAttached(unit(secondFighter));
        ship.addAttached(flight);
        transports.addSubForce(ship);
        root.addAttached(transports);
    }

    @Test
    void fightersUnderTheShipAreMarkedAsCarriedByIt() {
        int carried = CarrierLoadingConfigurator.configure(root, entity -> true);

        assertEquals(2, carried);
        assertEquals(leopard.getId(), firstFighter.getTransportId(), "The first fighter names the Leopard");
        assertEquals(leopard.getId(), secondFighter.getTransportId(), "The attached fighter names the Leopard too");
        assertEquals(Entity.NONE, atlas.getTransportId(), "A Mek the tree never put aboard stays on the ground");
    }

    @Test
    void everyUnitInTheBatchGetsItsOwnClientId() {
        CarrierLoadingConfigurator.configure(root, entity -> true);

        Set<Integer> ids = new HashSet<>();
        for (Entity entity : List.of(leopard, firstFighter, secondFighter, atlas)) {
            assertNotEquals(Entity.NONE, entity.getId(), entity.getShortName() + " has an id the server can map");
            ids.add(entity.getId());
        }
        assertEquals(4, ids.size(), "Ids are distinct, so the server's id map cannot confuse two units");
    }

    @Test
    void aShipTheUserLeftOutCarriesNothing() {
        // First run puts them aboard; the second, without the ship, must take them off again.
        CarrierLoadingConfigurator.configure(root, entity -> true);

        int carried = CarrierLoadingConfigurator.configure(root, entity -> entity != leopard);

        assertEquals(0, carried);
        assertEquals(Entity.NONE, firstFighter.getTransportId());
        assertEquals(Entity.NONE, secondFighter.getTransportId());
    }

    @Test
    void aFighterTheUserLeftOutIsNotMarked() {
        int carried = CarrierLoadingConfigurator.configure(root, entity -> entity != secondFighter);

        assertEquals(1, carried);
        assertEquals(leopard.getId(), firstFighter.getTransportId());
        assertEquals(Entity.NONE, secondFighter.getTransportId());
    }

    @Test
    void aUnitTheShipHasNoBayForStaysOutside() throws Exception {
        // A Leopard has Mek and fighter bays and nothing for a vehicle.
        Entity tank = load(TANK, true);
        flight.addSubForce(unit(tank));

        int carried = CarrierLoadingConfigurator.configure(root, entity -> true);

        assertEquals(2, carried);
        assertEquals(Entity.NONE, tank.getTransportId());
    }

    @Test
    void dropShipsDockToAJumpShipWithCollarsToSpare() throws Exception {
        // An Aegis has four collars. Five DropShips: four dock, the fifth is left in space.
        ForceDescriptor jumpShips = formation("JumpShips");
        Entity aegis = load(WARSHIP, true);
        jumpShips.addSubForce(unit(aegis));
        root.getAttached().get(0).addSubForce(jumpShips);
        ForceDescriptor flotilla = formation("Flotilla");
        List<Entity> leopards = new ArrayList<>();
        for (int count = 0; count < 4; count++) {
            Entity anotherLeopard = load(DROPSHIP, true);
            leopards.add(anotherLeopard);
            flotilla.addSubForce(unit(anotherLeopard));
        }
        root.getAttached().get(0).addSubForce(flotilla);

        int carried = CarrierLoadingConfigurator.configure(root, entity -> true);

        assertEquals(2 + 4, carried, "two fighters aboard the Leopard, four DropShips docked");
        assertEquals(aegis.getId(), leopard.getTransportId(), "the first Leopard docks");
        long docked = leopards.stream().filter(ship -> ship.getTransportId() == aegis.getId()).count();
        assertEquals(3, docked, "three more fill the collars");
        assertEquals(1, leopards.stream().filter(ship -> ship.getTransportId() == Entity.NONE).count(),
              "the fifth has no collar");
    }

    @Test
    void aCollarTakenByANestedDropShipIsNotGivenAway() throws Exception {
        // An Aegis with four DropShips docked beneath it in the tree has no collar left for a loose fifth.
        ForceDescriptor jumpShips = formation("JumpShips");
        Entity aegis = load(WARSHIP, true);
        ForceDescriptor aegisNode = unit(aegis);
        List<Entity> dockedLeopards = new ArrayList<>();
        for (int count = 0; count < 4; count++) {
            Entity dockedLeopard = load(DROPSHIP, true);
            dockedLeopards.add(dockedLeopard);
            aegisNode.addAttached(unit(dockedLeopard));
        }
        jumpShips.addSubForce(aegisNode);
        root.getAttached().get(0).addSubForce(jumpShips);

        int carried = CarrierLoadingConfigurator.configure(root, entity -> true);

        assertEquals(2 + 4, carried, "two fighters aboard the loose Leopard, four DropShips docked by the tree");
        assertEquals(4, dockedLeopards.stream().filter(ship -> ship.getTransportId() == aegis.getId()).count());
        assertEquals(Entity.NONE, leopard.getTransportId(), "the loose Leopard finds every collar taken");
    }

    @Test
    void aMissingForceIsIgnored() {
        assertEquals(0, CarrierLoadingConfigurator.configure(null, entity -> true));
    }

    private Entity load(String unitName, boolean isBlk) {
        Entity entity = getEntityForUnitTesting(unitName, isBlk);
        assertNotNull(entity, unitName + " not found in the test data");
        entity.setOwner(owner);
        return entity;
    }

    private static ForceDescriptor formation(String name) {
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.setName(name);
        return descriptor;
    }

    /** {@code ForceDescriptor.entity} is only ever set by generation, so the test injects it. */
    private static ForceDescriptor unit(Entity entity) throws Exception {
        ForceDescriptor descriptor = new ForceDescriptor();
        descriptor.setName(entity.getShortName());
        descriptor.setElement(true);
        Field field = ForceDescriptor.class.getDeclaredField("entity");
        field.setAccessible(true);
        field.set(descriptor, entity);
        return descriptor;
    }
}
