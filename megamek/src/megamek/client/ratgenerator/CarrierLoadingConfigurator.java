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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import megamek.common.annotations.Nullable;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Puts the units a generated force nests under a ship aboard that ship when the force is added to a game.
 *
 * <p>Generation nests a carrier's fighter complement under the carrier, but the tree is only a picture: the
 * units are sent to the server as a flat list and arrive standing beside the ship. The server can restore
 * loading for a batch it receives - a MUL carries the transport ids the saving game used, and
 * {@code TWGameManager.receiveEntityAdd} translates them through its client-to-server id map and loads the
 * units - so this writes the same thing a MUL would. Every included unit gets a client-side id, and a unit
 * beneath a carrier names that carrier as its transport. The server does the loading, and clears the
 * transport id of anything that does not fit once it has counted the bays.</p>
 *
 * <p>What the tree nests under a carrier goes aboard, and a DropShip with no ship of its own docks to a JumpShip
 * or WarShip in the batch that has a collar free. The troopships the DropShip Percentage setting adds are sized
 * to lift the force, but the tree never says which Mek rides in which hull, so those units stay on the ground
 * where a lobby game expects them.</p>
 */
public final class CarrierLoadingConfigurator {

    private static final MMLogger LOGGER = MMLogger.create(CarrierLoadingConfigurator.class);

    /** The first client-side id handed out. The server replaces any id a unit it already holds is using. */
    private static final int FIRST_CLIENT_ID = 1;

    private CarrierLoadingConfigurator() {
    }

    /**
     * Marks every included unit beneath a carrier as carried by it.
     *
     * <p>Call after the units have their owner, because a carrier only takes units on its own side, and
     * before the batch is sent, because the ids written here are what the server translates.</p>
     *
     * @param root       the generated force; {@code null} is ignored
     * @param isIncluded decides whether a unit is part of the batch - a caller showing a selection loads only
     *                   what the user actually took, and a carrier the user left out carries nothing
     *
     * @return how many units were marked as carried
     */
    public static int configure(@Nullable ForceDescriptor root, Predicate<Entity> isIncluded) {
        if (root == null) {
            return 0;
        }
        List<Entity> included = new ArrayList<>();
        collectIncluded(root, isIncluded, included);
        // Fresh ids every time: a unit added to a game once already carries ids from that batch, and a
        // transport id that names a carrier outside this batch would be dropped by the server anyway.
        int nextClientId = FIRST_CLIENT_ID;
        for (Entity entity : included) {
            entity.setId(nextClientId++);
            entity.setTransportId(Entity.NONE);
        }

        int carried = boardBeneath(root, null, isIncluded);
        carried += dock(included);
        if (carried > 0) {
            LOGGER.info("[ForceGen][Carrier] {} unit(s) under '{}' will board their carrier when added to the game",
                  carried, root.parseName());
        } else {
            LOGGER.debug("[ForceGen][Carrier] nothing under '{}' is nested beneath a carrier; nothing boards",
                  root.parseName());
        }
        return carried;
    }

    private static void collectIncluded(ForceDescriptor node, Predicate<Entity> isIncluded, List<Entity> into) {
        Entity entity = node.getEntity();
        if ((entity != null) && isIncluded.test(entity)) {
            into.add(entity);
        }
        for (ForceDescriptor child : node.getSubForces()) {
            collectIncluded(child, isIncluded, into);
        }
        for (ForceDescriptor child : node.getAttached()) {
            collectIncluded(child, isIncluded, into);
        }
    }

    /**
     * Walks the subtree, boarding each included unit on the nearest included carrier above it.
     *
     * @param node       the node to walk
     * @param carrier    the nearest carrier above this node that is part of the batch, or {@code null} when
     *                   there is none
     * @param isIncluded see {@link #configure(ForceDescriptor, Predicate)}
     *
     * @return how many units beneath (and including) this node boarded a carrier
     */
    private static int boardBeneath(ForceDescriptor node, @Nullable Entity carrier, Predicate<Entity> isIncluded) {
        Entity carrierForChildren = carrier;
        int carried = 0;

        Entity entity = node.getEntity();
        boolean isInBatch = (entity != null) && isIncluded.test(entity);
        if (isInBatch) {
            if ((carrier != null) && board(entity, carrier)) {
                carried++;
            }
            // A unit with bays of its own takes what the tree nests under it, whether or not it is itself
            // being carried.
            if (!entity.getTransports().isEmpty()) {
                carrierForChildren = entity;
            }
        }

        for (ForceDescriptor child : node.getSubForces()) {
            carried += boardBeneath(child, carrierForChildren, isIncluded);
        }
        for (ForceDescriptor child : node.getAttached()) {
            carried += boardBeneath(child, carrierForChildren, isIncluded);
        }
        return carried;
    }

    /**
     * Docks every DropShip still without a ship to a JumpShip or WarShip in the batch with a collar to spare, in
     * the order they appear. DropShips the tree nests under a ship were docked to it by {@link #boardBeneath};
     * their collars are taken before any loose DropShip is given one, so a ship is never docked past its collars.
     *
     * @param included every unit in the batch, in tree order
     *
     * @return how many DropShips were docked
     */
    private static int dock(List<Entity> included) {
        Map<Entity, Integer> freeCollars = new LinkedHashMap<>();
        Map<Integer, Entity> collarShipsById = new HashMap<>();
        for (Entity entity : included) {
            if (!entity.getDockingCollars().isEmpty()) {
                freeCollars.put(entity, entity.getDockingCollars().size());
                collarShipsById.put(entity.getId(), entity);
            }
        }
        if (freeCollars.isEmpty()) {
            return 0;
        }
        for (Entity entity : included) {
            boolean isDockedDropship = (entity instanceof Dropship) && (entity.getTransportId() != Entity.NONE);
            Entity carrier = isDockedDropship ? collarShipsById.get(entity.getTransportId()) : null;
            if (carrier != null) {
                freeCollars.merge(carrier, -1, Integer::sum);
            }
        }
        int docked = 0;
        for (Entity entity : included) {
            boolean isUndockedDropship = (entity instanceof Dropship) && (entity.getTransportId() == Entity.NONE);
            if (!isUndockedDropship) {
                continue;
            }
            for (Map.Entry<Entity, Integer> ship : freeCollars.entrySet()) {
                boolean hasCollarFree = ship.getValue() > 0;
                if (hasCollarFree && ship.getKey().canLoad(entity, false)) {
                    entity.setTransportId(ship.getKey().getId());
                    ship.setValue(ship.getValue() - 1);
                    docked++;
                    break;
                }
            }
        }
        return docked;
    }

    /**
     * @return {@code true} when the unit was marked as carried by the carrier
     */
    private static boolean board(Entity unit, Entity carrier) {
        if (!carrier.canLoad(unit, false)) {
            LOGGER.debug("[ForceGen][Carrier] '{}' cannot take '{}'; it stays outside the ship",
                  carrier.getShortName(), unit.getShortName());
            return false;
        }
        unit.setTransportId(carrier.getId());
        return true;
    }
}
