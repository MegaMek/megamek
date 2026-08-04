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

package megamek.client.ui.panels.phaseDisplay.lobby;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.equipment.Transporter;
import megamek.common.units.Entity;

/**
 * Works out how a selection of units should be loaded into one another.
 * <p>
 * Selecting a JumpShip, a DropShip, some fighters and a lance of Meks and loading them by hand means finding the right
 * carrier for every unit and doing it in the right order. This decides the whole arrangement in one go, putting each
 * unit in the innermost carrier that will take it, so the fighters and Meks end up in the DropShip and the DropShip
 * ends up in the JumpShip rather than everything piling into the largest hull.
 * </p>
 * <p>
 * Nothing is loaded here. The planner only says what should happen, so the caller can carry the steps out and report
 * whatever did not fit. Capacity is taken from the units' own transporters, so a plan never asks for a load the game
 * would refuse.
 * </p>
 */
public final class CarrierAssemblyPlanner {

    /** Stops a malformed transport loop from spinning when working out how deeply a carrier nests. */
    private static final int MAX_NESTING_DEPTH = 8;

    /** One unit to load into one carrier. Bay {@code -1} lets the server pick, as the load menu does. */
    public record LoadStep(int carriedId, int carrierId) {
    }

    /** A unit the plan could not place, and the reason to show the player. */
    public record UnplacedUnit(int unitId, String reason) {
    }

    /**
     * The whole arrangement: the loads to carry out, innermost first, and anything left over.
     *
     * @param steps    loads in the order they must be carried out
     * @param unplaced units that found no carrier, each with a reason
     */
    public record AssemblyPlan(List<LoadStep> steps, List<UnplacedUnit> unplaced) {
        public boolean isEmpty() {
            return steps.isEmpty();
        }
    }

    private CarrierAssemblyPlanner() {
    }

    /**
     * Plans how the given units should be loaded into one another.
     *
     * @param selectedUnits the units the player selected, carriers and cargo together
     *
     * @return the loads to carry out and the units that did not fit
     */
    public static AssemblyPlan planAssembly(Collection<Entity> selectedUnits) {
        List<Entity> units = new ArrayList<>(selectedUnits);
        Map<Integer, Integer> nestingDepths = nestingDepths(units);
        Map<Integer, Double> remainingCapacity = new HashMap<>();

        // Deepest carriers are filled first, so a Mek takes a DropShip bay before the DropShip is asked to fit into
        // the JumpShip. Within one level, fill a carrier before starting the next.
        List<Entity> cargoFirst = new ArrayList<>(units);
        cargoFirst.sort(Comparator.comparingInt((Entity unit) -> -nestingDepths.getOrDefault(unit.getId(), 0))
              .thenComparingInt(Entity::getId));

        List<LoadStep> steps = new ArrayList<>();
        List<UnplacedUnit> unplaced = new ArrayList<>();

        for (Entity unit : cargoFirst) {
            if (isOutermost(unit, units, nestingDepths)) {
                // Nothing in the selection can carry it, so it is the top of a stack rather than cargo.
                continue;
            }

            Entity carrier = bestCarrierFor(unit, units, nestingDepths, remainingCapacity);

            if (carrier == null) {
                unplaced.add(new UnplacedUnit(unit.getId(), "no carrier in the selection has room for it"));
                continue;
            }

            steps.add(new LoadStep(unit.getId(), carrier.getId()));
        }

        // Load from the inside out: a DropShip has to take its Meks before it docks with the JumpShip.
        steps.sort(Comparator.comparingInt(step -> -nestingDepths.getOrDefault(step.carriedId(), 0)));

        return new AssemblyPlan(steps, unplaced);
    }

    /**
     * How deeply each unit sits in the stack: 0 for a unit nothing in the selection can carry, 1 for a unit only those
     * can carry, and so on.
     */
    private static Map<Integer, Integer> nestingDepths(List<Entity> units) {
        Map<Integer, Integer> depths = new HashMap<>();

        for (Entity unit : units) {
            depths.put(unit.getId(), depthOf(unit, units, 0));
        }

        return depths;
    }

    private static int depthOf(Entity unit, List<Entity> units, int depthSoFar) {
        if (depthSoFar >= MAX_NESTING_DEPTH) {
            return depthSoFar;
        }

        int deepest = 0;
        for (Entity candidate : units) {
            if ((candidate != unit) && couldCarry(candidate, unit)) {
                deepest = Math.max(deepest, depthOf(candidate, units, depthSoFar + 1) + 1);
            }
        }

        return deepest;
    }

    /** True when nothing else in the selection could carry this unit, so it heads its own stack. */
    private static boolean isOutermost(Entity unit, List<Entity> units, Map<Integer, Integer> nestingDepths) {
        return nestingDepths.getOrDefault(unit.getId(), 0) == 0;
    }

    /**
     * Picks the carrier for a unit: the innermost one that can still take it, and among equals the one that comes
     * first, so a carrier is filled before the next is started.
     */
    private static Entity bestCarrierFor(Entity unit, List<Entity> units, Map<Integer, Integer> nestingDepths,
          Map<Integer, Double> remainingCapacity) {
        Entity best = null;
        int bestDepth = -1;

        for (Entity candidate : units) {
            if ((candidate == unit) || !couldCarry(candidate, unit)) {
                continue;
            }
            if (!hasRoomFor(candidate, unit, remainingCapacity)) {
                continue;
            }

            int candidateDepth = nestingDepths.getOrDefault(candidate.getId(), 0);
            if ((candidateDepth > bestDepth) || ((candidateDepth == bestDepth) && (best != null)
                  && (candidate.getId() < best.getId()))) {
                best = candidate;
                bestDepth = candidateDepth;
            }
        }

        if (best != null) {
            consumeCapacity(best, unit, remainingCapacity);
        }

        return best;
    }

    /** True when any transporter on the carrier accepts this kind of unit, ignoring how full it currently is. */
    private static boolean couldCarry(Entity carrier, Entity unit) {
        for (Transporter transporter : carrier.getTransports()) {
            if (transporter.canLoad(unit)) {
                return true;
            }
        }

        return false;
    }

    /** True when the carrier still has room once the loads already planned are taken into account. */
    private static boolean hasRoomFor(Entity carrier, Entity unit, Map<Integer, Double> remainingCapacity) {
        for (Transporter transporter : carrier.getTransports()) {
            if (!transporter.canLoad(unit)) {
                continue;
            }

            double remaining = remainingCapacity.computeIfAbsent(capacityKey(carrier, transporter),
                  key -> transporter.getUnused());
            if (remaining >= 1.0) {
                return true;
            }
        }

        return false;
    }

    /** Books a unit into the first transporter on the carrier that can take it. */
    private static void consumeCapacity(Entity carrier, Entity unit, Map<Integer, Double> remainingCapacity) {
        for (Transporter transporter : carrier.getTransports()) {
            if (!transporter.canLoad(unit)) {
                continue;
            }

            int key = capacityKey(carrier, transporter);
            double remaining = remainingCapacity.computeIfAbsent(key, ignored -> transporter.getUnused());

            if (remaining >= 1.0) {
                remainingCapacity.put(key, remaining - 1.0);
                return;
            }
        }
    }

    /** Identifies one transporter on one carrier, so capacity is tracked per bay rather than per unit. */
    private static int capacityKey(Entity carrier, Transporter transporter) {
        return (carrier.getId() * 31) + System.identityHashCode(transporter);
    }
}
