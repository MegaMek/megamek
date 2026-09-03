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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.bays.Bay;
import megamek.common.equipment.DockingCollar;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;

/**
 * The lift a force already owns before a new one is generated: the bays and docking collars on its ships that
 * nothing it has still needs.
 *
 * <p>A player builds a command in layers - a Mek battalion with its DropShips, then an armour company - and each
 * layer should start from the ships the earlier ones brought rather than draw a fresh set. So a later roll is handed
 * what is free: every bay on the existing ships, less a berth for every unit already there that has no ship yet,
 * and every docking collar less one for each DropShip that has no JumpShip yet. The transport stage then generates
 * only the shortfall.</p>
 *
 * @param freeBays           bays with nobody to fill them, by the unit type each bay is built for (infantry bays in
 *                           tons); never {@code null}
 * @param freeDockingCollars docking collars with no DropShip to take them
 */
public record ExistingLift(Map<Integer, Integer> freeBays, int freeDockingCollars) {

    /** Nothing owned yet: the first layer of a command, or a standalone force. */
    public static final ExistingLift NONE = new ExistingLift(Map.of(), 0);

    public ExistingLift {
        freeBays = Map.copyOf(freeBays);
    }

    /**
     * Works out what is free among the given units, wherever they live: a game, a campaign hangar, or the command
     * model accumulated so far.
     *
     * <p>A unit aboard a ship already has its berth, so it is neither demand nor, for a DropShip docked to a
     * JumpShip, a collar to find. Everything else with no ship counts against the bays that could take it, most
     * restrictive kind first, exactly as the transport stage books lift.</p>
     *
     * @param units every unit the force already has, ships included
     *
     * @return the free lift among them; {@link #NONE} when there is none
     */
    public static ExistingLift of(Collection<Entity> units) {
        List<Entity> unberthed = new ArrayList<>();
        int freeCollars = 0;
        Map<Integer, Integer> bays = new HashMap<>();
        for (Entity unit : units) {
            for (Bay bay : unit.getTransportBays()) {
                int bayType = TransportCalculator.bayType(bay);
                if (bayType != TransportCalculator.NOT_A_UNIT_BAY) {
                    bays.merge(bayType, (int) bay.getUnused(), Integer::sum);
                }
            }
            for (DockingCollar collar : unit.getDockingCollars()) {
                freeCollars += (int) collar.getUnused();
            }
            if (unit.getTransportId() == Entity.NONE) {
                unberthed.add(unit);
            }
        }

        Map<Integer, Integer> demand = TransportCalculator.liftDemand(unberthed);
        TransportCalculator.BayLedger ledger = new TransportCalculator.BayLedger(demand);
        ledger.add(bays);
        for (int unitType : TransportCalculator.LIFT_ORDER) {
            ledger.claim(unitType, Math.min(demand.getOrDefault(unitType, 0), ledger.free(unitType)));
        }
        int undockedDropships = demand.getOrDefault(UnitType.DROPSHIP, 0);
        return new ExistingLift(ledger.freeByBayType(), Math.max(0, freeCollars - undockedDropships));
    }

    /**
     * @param force a generated force, or {@code null} for none
     *
     * @return the free lift among the units in the force
     */
    public static ExistingLift of(@Nullable ForceDescriptor force) {
        if (force == null) {
            return NONE;
        }
        List<Entity> units = new ArrayList<>();
        force.addAllEntities(units);
        return of(units);
    }

    /**
     * @return this lift and the other added together
     */
    public ExistingLift plus(ExistingLift other) {
        Map<Integer, Integer> bays = new HashMap<>(freeBays);
        other.freeBays.forEach((bayType, count) -> bays.merge(bayType, count, Integer::sum));
        return new ExistingLift(bays, freeDockingCollars + other.freeDockingCollars);
    }

    /**
     * @return {@code true} when there is nothing free to start from
     */
    public boolean isEmpty() {
        boolean noBays = freeBays.values().stream().allMatch(count -> count <= 0);
        return noBays && (freeDockingCollars <= 0);
    }
}
