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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;

/**
 * Swaps one ship in a generated force for a different design that does the same job.
 *
 * <p>A player who does not want the Overlord the roll gave them can ask for another hull without rolling the whole
 * force again. The replacement is drawn from the same faction table as the original and must be up to the same
 * work: every kind of unit bay the current hull has, the candidate has at between half and double the capacity,
 * and it keeps enough docking collars for the DropShips already docked to it. The same design is never drawn
 * again unless it is the only one in the table that qualifies, and the log says when that happened.</p>
 *
 * <p>What hangs under the ship follows it: docked DropShips stay docked, and a fighter complement is generated
 * afresh to the new hull's bays, since a Union's two fighters would rattle around an Overlord's six.</p>
 */
public final class ShipReroller {

    private static final MMLogger LOGGER = MMLogger.create(ShipReroller.class);

    /** The bay kinds a replacement need not match: a fighter complement is regenerated to fit the new hull. */
    private static final List<Integer> REGENERATED_BAYS = List.of(UnitType.AEROSPACE_FIGHTER, UnitType.SMALL_CRAFT);

    /** How far, either way, a replacement's bays may differ from the current hull's, as a factor. */
    private static final int SAME_JOB_SPREAD = 2;

    /**
     * What a reroll did.
     *
     * @param before             the design that was replaced
     * @param after              the design drawn in its place
     * @param sameDesignKept     {@code true} when nothing else in the table could do the job, so the same design was
     *                           drawn again
     * @param fightersRegenerated how many fighters the new hull carries, when the old one carried a complement
     */
    public record Outcome(String before, String after, boolean sameDesignKept, int fightersRegenerated) {
    }

    private ShipReroller() {
    }

    /**
     * Replaces the ship's design with another that does the same job, and refits what hangs under it.
     *
     * @param ship the ship element to reroll
     *
     * @return what changed, or {@code null} when the node is not a ship or the table holds nothing that could take
     *       its place
     */
    public static @Nullable Outcome reroll(ForceDescriptor ship) {
        if (!isShip(ship)) {
            LOGGER.debug("[ForceGen][Reroll] '{}' is not a ship; nothing to reroll", ship.parseName());
            return null;
        }
        MekSummary current = MekSummaryCache.getInstance().getMek(ship.getModelName());
        if (current == null) {
            LOGGER.warn("[ForceGen][Reroll] the unit cache has no design named '{}'; cannot reroll it",
                  ship.getModelName());
            return null;
        }
        int unitType = ship.getUnitType();
        UnitTable table = UnitTable.findTable(ship.getFactionRec(), unitType, ship.getYear(),
              ship.ratGeneratorRating(), null, ModelRecord.NETWORK_NONE, EnumSet.noneOf(EntityMovementMode.class),
              EnumSet.noneOf(MissionRole.class), 0);

        Map<Integer, Integer> currentBays = TransportCalculator.bays(current);
        int dockedDropShips = countAttachedOfType(ship, UnitType.DROPSHIP);
        UnitTable.UnitFilter doesTheSameJob = candidate -> isReplacementFor(currentBays,
              TransportCalculator.bays(candidate), TransportCalculator.dockingCollars(candidate), dockedDropShips);

        MekSummary replacement = table.generateUnit(candidate -> doesTheSameJob.include(candidate)
              && !candidate.getName().equals(current.getName()));
        boolean sameDesignKept = false;
        if (replacement == null) {
            replacement = table.generateUnit(doesTheSameJob);
            sameDesignKept = replacement != null;
        }
        if (replacement == null) {
            LOGGER.warn("[ForceGen][Reroll] nothing in the {} {} table can take the place of '{}'; left as it is",
                  ship.getFaction(), ship.getYear(), current.getName());
            return null;
        }

        ship.setUnit(RATGenerator.getInstance().getModelRecord(replacement.getName()));
        // Docked DropShips come off first and go back on at the end, untouched: the complement pass would
        // otherwise fill their bays a second time, and the loader would rebuild their entities and everything
        // nested under them.
        List<ForceDescriptor> docked = detachAttachedOfType(ship, UnitType.DROPSHIP);
        boolean hadComplement = detachFighterGroups(ship) > 0;
        int fighters = 0;
        if (hadComplement) {
            ship.addFighterComplement();
            fighters = countNestedFighters(ship);
        }
        ship.loadEntities(null, 0);
        docked.forEach(ship::addAttached);

        LOGGER.info("[ForceGen][Reroll] '{}' -> '{}'{}{}", current.getName(), replacement.getName(),
              sameDesignKept ? " (the only design in the table for the job)" : "",
              hadComplement ? " with " + fighters + " fighter(s) generated for the new bays" : "");
        return new Outcome(current.getName(), replacement.getName(), sameDesignKept, fighters);
    }

    /**
     * Whether a candidate hull can take over a ship's job.
     *
     * @param currentBays      the current hull's unit bays by the kind each is built for
     * @param candidateBays    the candidate's, the same way
     * @param candidateCollars how many docking collars the candidate has
     * @param dockedDropShips  how many DropShips are docked to the current ship and must stay docked
     *
     * @return {@code true} when every kind of unit bay the current hull has, the candidate has at between half and
     *       double the capacity (fighter and small craft bays aside, since that complement is regenerated), and
     *       it has collars enough for what is docked
     */
    static boolean isReplacementFor(Map<Integer, Integer> currentBays, Map<Integer, Integer> candidateBays,
          int candidateCollars, int dockedDropShips) {
        for (Map.Entry<Integer, Integer> bay : currentBays.entrySet()) {
            if (REGENERATED_BAYS.contains(bay.getKey())) {
                continue;
            }
            int wanted = bay.getValue();
            int offered = candidateBays.getOrDefault(bay.getKey(), 0);
            boolean tooSmall = (offered * SAME_JOB_SPREAD) < wanted;
            boolean tooBig = offered > (wanted * SAME_JOB_SPREAD);
            if (tooSmall || tooBig) {
                return false;
            }
        }
        return candidateCollars >= dockedDropShips;
    }

    /**
     * @return {@code true} for a ship element: a DropShip, JumpShip, WarShip or space station
     */
    public static boolean isShip(ForceDescriptor node) {
        Integer unitType = node.getUnitType();
        if (!node.isElement() || (unitType == null)) {
            return false;
        }
        return (unitType == UnitType.DROPSHIP) || (unitType == UnitType.JUMPSHIP) || (unitType == UnitType.WARSHIP)
              || (unitType == UnitType.SPACE_STATION);
    }

    private static int countAttachedOfType(ForceDescriptor ship, int unitType) {
        int count = 0;
        for (ForceDescriptor attached : ship.getAttached()) {
            if ((attached.getUnitType() != null) && (attached.getUnitType() == unitType)) {
                count++;
            }
        }
        return count;
    }

    private static List<ForceDescriptor> detachAttachedOfType(ForceDescriptor ship, int unitType) {
        List<ForceDescriptor> detached = new ArrayList<>();
        for (ForceDescriptor attached : new ArrayList<>(ship.getAttached())) {
            if ((attached.getUnitType() != null) && (attached.getUnitType() == unitType)) {
                ship.getAttached().remove(attached);
                detached.add(attached);
            }
        }
        return detached;
    }

    /**
     * Removes the ship's own fighter complement: the flights, squadrons and groups the complement pass attached to
     * it, which are its fighter-typed children. Call with docked DropShips already detached, so their fighters
     * are not counted as the ship's.
     *
     * @return how many fighters were removed
     */
    private static int detachFighterGroups(ForceDescriptor ship) {
        int fighters = countNestedFighters(ship);
        detachAttachedOfType(ship, UnitType.AEROSPACE_FIGHTER);
        return fighters;
    }

    private static int countNestedFighters(ForceDescriptor node) {
        int count = 0;
        for (ForceDescriptor attached : node.getAttached()) {
            count += countFightersUnder(attached);
        }
        return count;
    }

    private static int countFightersUnder(ForceDescriptor node) {
        boolean isFighter = node.isElement() && (node.getUnitType() != null)
              && (node.getUnitType() == UnitType.AEROSPACE_FIGHTER);
        int count = isFighter ? 1 : 0;
        for (ForceDescriptor child : node.getSubForces()) {
            count += countFightersUnder(child);
        }
        for (ForceDescriptor child : node.getAttached()) {
            count += countFightersUnder(child);
        }
        return count;
    }
}
