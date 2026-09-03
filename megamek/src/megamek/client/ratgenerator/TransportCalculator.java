/*
 * Copyright (C) 2018-2026 The MegaMek Team. All Rights Reserved.
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.bays.*;
import megamek.common.compute.Compute;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Infantry;
import megamek.common.units.PlatoonType;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;

/**
 * Generates drop-ships and jump ships to fulfill transport requirements for a unit.
 *
 * @author Neoancient
 */
public class TransportCalculator {

    private static final MMLogger LOGGER = MMLogger.create(TransportCalculator.class);

    /**
     * The bays each kind of unit may ride in: its own first, then the larger bays it also fits. A light vehicle fits
     * a heavy or super-heavy vehicle bay and a heavy vehicle a super-heavy one; everything else needs its own kind.
     */
    private static final Map<Integer, List<Integer>> BAYS_THAT_FIT = Map.of(
          UnitType.VTOL, List.of(UnitType.VTOL, UnitType.TANK, UnitType.NAVAL),
          UnitType.TANK, List.of(UnitType.TANK, UnitType.NAVAL));

    /**
     * The order lift is arranged in. The vehicles with the fewest bay options claim theirs first, so a light vehicle
     * only takes a heavy bay the heavy vehicles left over rather than one they still need.
     */
    static final List<Integer> LIFT_ORDER = List.of(UnitType.NAVAL, UnitType.TANK, UnitType.VTOL,
          UnitType.MEK, UnitType.PROTOMEK, UnitType.BATTLE_ARMOR, UnitType.INFANTRY, UnitType.AEROSPACE_FIGHTER,
          UnitType.SMALL_CRAFT);

    /**
     * A hull is worth drawing when it carries at least this share of what is still needed, so a battalion is lifted
     * by Unions and Overlords rather than a string of Leopards.
     */
    private static final int SMALLEST_USEFUL_SHARE = 3;

    /**
     * At least this fraction of a hull's bays, across every kind it has, must be put to use by what the force still
     * needs. A lone tank company then gets vehicle carriers, not an Overlord Combined Arms with twenty-four Mek
     * bays and six fighter bays sailing empty; the same hull is a fine draw for a battalion with an armour company.
     */
    private static final int MIN_HULL_USE = 2;

    /**
     * The chance, in percent, that a draw takes the one hull that is exactly the size of what is still needed
     * rather than building the lift up from smaller hulls.
     */
    private static final int EXACT_FIT_CHANCE = 50;

    /**
     * When no hull fits the need, the fallback draws from hulls up to this many times the size of the smallest that
     * would do, or down to this fraction of the largest there is, so the fallback keeps some variety.
     */
    private static final int FALLBACK_SPREAD = 2;

    // In order to determine the transport capacity of generated units we need to load the Entity and look at the
    // bays and docking hard points. Since this is a relatively expensive operation we will cache the results.
    private static final Map<MekSummary, Map<Integer, Integer>> bayTypeCache = new HashMap<>();

    /** Cargo capacity is kept out of {@link #bayTypeCache} because it is tonnage, not a unit count. */
    private static final Map<MekSummary, CargoCapacity> cargoCapacityCache = new HashMap<>();

    public static void dispose() {
        bayTypeCache.clear();
        cargoCapacityCache.clear();
        dockingCollarCache.clear();
    }

    /**
     * The cargo a craft can haul, split the same way MekHQ tracks it: liquid bays are counted
     * separately because liquid cargo cannot be stowed in a dry hold and vice versa, so a hull with
     * plenty of one is no help to a command that needs the other.
     *
     * @param solidTons  dry cargo tonnage - general, insulated, refrigerated and livestock holds
     * @param liquidTons liquid cargo tonnage
     */
    public record CargoCapacity(double solidTons, double liquidTons) {

        public static final CargoCapacity NONE = new CargoCapacity(0, 0);

        /** @return the combined capacity of this and {@code other} */
        public CargoCapacity plus(CargoCapacity other) {
            return new CargoCapacity(solidTons + other.solidTons, liquidTons + other.liquidTons);
        }

        /** @return {@code true} when this craft can haul no cargo of either kind */
        public boolean isEmpty() {
            return (solidTons <= 0) && (liquidTons <= 0);
        }
    }

    /**
     * Reads the cargo holds of a craft. Quarters and seating are deliberately excluded: they are
     * separate {@link Bay} types rather than subclasses of {@link CargoBay}, so berths for the crew
     * are never mistaken for space to stow spares.
     *
     * @param mekSummary the craft to measure
     *
     * @return its cargo capacity, or {@link CargoCapacity#NONE} when it has none or cannot be loaded
     */
    public static CargoCapacity cargoCapacity(MekSummary mekSummary) {
        return cargoCapacityCache.computeIfAbsent(mekSummary, summary -> {
            try {
                Entity entity = new MekFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
                double solid = 0;
                double liquid = 0;
                for (Bay bay : entity.getTransportBays()) {
                    if (bay instanceof LiquidCargoBay) {
                        liquid += bay.getCapacity();
                    } else if ((bay instanceof CargoBay)
                          || (bay instanceof InsulatedCargoBay)
                          || (bay instanceof RefrigeratedCargoBay)
                          || (bay instanceof LivestockCargoBay)) {
                        solid += bay.getCapacity();
                    }
                }
                return new CargoCapacity(solid, liquid);
            } catch (EntityLoadingException exception) {
                // Cache the failure so a unit that cannot be loaded is not re-parsed on every call.
                return CargoCapacity.NONE;
            }
        });
    }

    /** The faction whose ship tables are drawn from; {@code null} for the general tables. */
    private final FactionRecord factionRecord;
    /** The faction's name for the log; {@code null} when unknown. */
    private final String factionName;
    private final int year;
    /** The force's rating; {@code null} for any. */
    private final String rating;
    private final Map<Integer, Integer> unitCounts;
    /** The lift the force already owns before this run; only the shortfall is generated. */
    private final ExistingLift existingLift;

    public TransportCalculator(ForceDescriptor fd) {
        this(fd.getFactionRec(), fd.getFaction(), fd.getYear(), fd.ratGeneratorRating(), unitsOf(fd),
              fd.getExistingLift());
    }

    /**
     * Sizes lift for units that are not a generated force - the whole hangar of a campaign, say - starting from the
     * lift they already own.
     *
     * @param factionRecord the faction whose ship tables are drawn from; {@code null} for the general tables
     * @param factionName   the faction's name, for the log; {@code null} when unknown
     * @param year          the year the ships are drawn for
     * @param rating        the force's rating, or {@code null} for any
     * @param units         the units wanting lift; ships among them are counted for docking collars, not bays
     * @param existingLift  the free bays and docking collars already owned
     */
    public TransportCalculator(@Nullable FactionRecord factionRecord, @Nullable String factionName, int year,
          @Nullable String rating, Collection<Entity> units, ExistingLift existingLift) {
        this.factionRecord = factionRecord;
        this.factionName = factionName;
        this.year = year;
        this.rating = rating;
        this.unitCounts = liftDemand(units);
        this.existingLift = existingLift;
    }

    private static List<Entity> unitsOf(ForceDescriptor force) {
        List<Entity> allUnits = new ArrayList<>();
        force.addAllEntities(allUnits);
        return allUnits;
    }

    /**
     * Determines number of each type of unit based on transport requirements.
     *
     * @param units the units wanting lift
     *
     * @return The number of units of each type mapped to its UnitType. UnitType.VTOL is used for light vehicle bays and
     *       UnitType.NAVAL for super heavy vehicles; infantry is counted in tons of bay.
     */
    static Map<Integer, Integer> liftDemand(Collection<Entity> units) {
        Map<Integer, Integer> unitCounts = new HashMap<>();
        for (Entity unit : units) {
            if (unit.hasETypeFlag(Entity.ETYPE_MEK)) {
                unitCounts.merge(UnitType.MEK, 1, Integer::sum);
            } else if (unit.hasETypeFlag(Entity.ETYPE_PROTOMEK)) {
                unitCounts.merge(UnitType.PROTOMEK, 1, Integer::sum);
            } else if (unit.hasETypeFlag(Entity.ETYPE_TANK)) {
                if (unit.getWeight() > 100) {
                    unitCounts.merge(UnitType.NAVAL, 1, Integer::sum);
                } else if (unit.getWeight() > 50) {
                    unitCounts.merge(UnitType.TANK, 1, Integer::sum);
                } else {
                    unitCounts.merge(UnitType.VTOL, 1, Integer::sum);
                }
            } else if (unit.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)) {
                unitCounts.merge(UnitType.BATTLE_ARMOR, 1, Integer::sum);
            } else if (unit instanceof Infantry infantry) {
                // Here we need to count the transport weight of the platoon rather than just the number
                unitCounts.merge(UnitType.INFANTRY, infantryLiftTons(infantry), Integer::sum);
            } else if (unit.hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
                unitCounts.merge(UnitType.DROPSHIP, 1, Integer::sum);
            } else if (unit.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
                unitCounts.merge(UnitType.SMALL_CRAFT, 1, Integer::sum);
            } else if (unit.isFighter()) {
                unitCounts.merge(UnitType.AEROSPACE_FIGHTER, 1, Integer::sum);
            }
        }
        return unitCounts;
    }

    /**
     * The bay tonnage a platoon takes, the way {@code InfantryBay} charges for it: foot, jump and motorized platoons
     * take one cubicle of their type's weight, while a mechanized platoon takes one per squad. An infantry bay is
     * built for one type but, by tonnage, carries any of them.
     *
     * @param infantry the platoon
     *
     * @return the tons of infantry bay it needs
     */
    static int infantryLiftTons(Infantry infantry) {
        PlatoonType type = PlatoonType.getPlatoonType(infantry);
        if (type == PlatoonType.MECHANIZED) {
            return type.getWeight() * infantry.getSquadCount();
        }
        return type.getWeight();
    }

    /**
     * Generates dropships to provide enough capacity to transport the given ratio of the formation.
     *
     * <p>Lift is arranged one kind of unit at a time in {@link #LIFT_ORDER}. Each hull drawn is booked in a
     * {@link BayLedger}, so bays one kind of unit leaves free carry the kinds that fit them: the heavy vehicle bays
     * an armour company does not fill take its light vehicles, and no second hull is drawn for them. Hulls are
     * drawn to suit what is still needed, see {@link #drawHullFor(UnitTable, int, BayLedger)}.</p>
     *
     * @param ratio The ratio of dropships to generate to the total needs of the unit
     *
     * @return A list of generated dropships
     */
    public List<MekSummary> calcDropships(double ratio) {
        UnitTable table = UnitTable.findTable(factionRecord,
              UnitType.DROPSHIP,
              year,
              rating,
              null,
              ModelRecord.NETWORK_NONE,
              EnumSet.noneOf(EntityMovementMode.class),
              EnumSet.noneOf(MissionRole.class),
              0);
        List<MekSummary> retVal = new ArrayList<>();
        // DropShips were counted for the JumpShip calculation; they need docking collars, not bays, and are not in
        // the lift order.
        Map<Integer, Integer> needed = new HashMap<>();
        for (int unitType : LIFT_ORDER) {
            int count = (int) Math.ceil(unitCounts.getOrDefault(unitType, 0) * ratio);
            if (count > 0) {
                needed.put(unitType, count);
            }
        }
        BayLedger ledger = new BayLedger(needed);
        // A later layer of a command starts from the ships the earlier layers brought.
        ledger.add(existingLift.freeBays());
        if (!existingLift.isEmpty()) {
            LOGGER.info("[ForceGen][Lift] starting from existing lift: free bays {} and {} free docking collar(s)",
                  existingLift.freeBays(), existingLift.freeDockingCollars());
        }
        StringBuilder summary = new StringBuilder();
        for (int unitType : LIFT_ORDER) {
            if (!needed.containsKey(unitType)) {
                continue;
            }
            int hullsBefore = retVal.size();
            while (ledger.unmet(unitType) > 0) {
                MekSummary dropship = drawHullFor(table, unitType, ledger);
                if (null == dropship) {
                    LOGGER.debug("[ForceGen][Lift] no DropShip in the {} {} table carries {}; "
                                + "{} of {} left without lift",
                          factionName, year, UnitType.getTypeName(unitType),
                          ledger.unmet(unitType), needed.get(unitType));
                    break;
                }
                ledger.add(baysOf(dropship));
                retVal.add(dropship);
            }
            int freeBeforeClaim = ledger.free(unitType);
            ledger.claim(unitType, Math.min(needed.get(unitType), freeBeforeClaim));
            summary.append(UnitType.getTypeName(unitType)).append(": needed ").append(needed.get(unitType))
                  .append(", had ").append(freeBeforeClaim).append(", drew ").append(retVal.size() - hullsBefore)
                  .append(" hull(s); ");
        }
        LOGGER.info("[ForceGen][Lift] {} DropShip(s) for {}% lift - {}", retVal.size(), Math.round(ratio * 100),
              summary);
        LOGGER.info("[ForceGen][Lift] hulls drawn: {}", () -> describeHulls(retVal));
        return retVal;
    }

    /**
     * @return the hulls by name, for the log
     */
    private static String describeHulls(List<MekSummary> hulls) {
        StringBuilder description = new StringBuilder();
        for (MekSummary hull : hulls) {
            if (!description.isEmpty()) {
                description.append(", ");
            }
            description.append(hull.getName());
        }
        return description.toString();
    }

    /**
     * Draws a hull that suits what is still to be carried.
     *
     * <p>Sizes are tried from the best fit outward: a hull that carries exactly what is still needed (half the
     * time, see {@link #EXACT_FIT_CHANCE}); a {@link #isReasonableFit(int, int) reasonable fit}; the smallest hulls
     * that cover the need on their own, for the one vehicle left over that no hull is small enough for; the
     * largest hulls that carry the kind at all, for a need bigger than any single hull. Every size is first tried
     * among hulls the force would {@link BayLedger#wouldMostlyUse(Map) mostly use} and only then among the rest, so
     * an Intruder whose bays the platoons fill beats three Achilles that would each sail with a fighter deck empty.
     * Faction availability weights the draw inside every try. Any hull that carries the kind is accepted only when
     * the table holds no unit entries to size against.</p>
     *
     * <p>A hull's own era availability code (the D/X-E-D-D line of its record sheet) is not consulted: MegaMek
     * composes it from the DropShip construction rules and the hull's components, and the construction rules rate
     * every DropShip "very rare" for the whole Succession Wars, so in 3025 a Union and an Excalibur carry the same
     * code. The faction availability numbers in the force generator's era files are what tell them apart, and the
     * table is already weighted by those.</p>
     *
     * @param table    the DropShip table for the faction and year
     * @param unitType the kind of unit lift is wanted for
     * @param ledger   what has been drawn and berthed so far, and what is still needed
     *
     * @return the hull drawn, or {@code null} when nothing in the table carries the kind
     */
    private @Nullable MekSummary drawHullFor(UnitTable table, int unitType, BayLedger ledger) {
        int stillNeeded = ledger.unmet(unitType);
        List<UnitTable.UnitFilter> sizes = new ArrayList<>();
        // Half the time, one hull that is exactly the job - an Overlord for a battalion, a Union for a company;
        // the other half the lift is built up from smaller hulls that are each filled, so a battalion is not always
        // an Overlord nor always three Unions.
        if (Compute.randomInt(100) < EXACT_FIT_CHANCE) {
            sizes.add(candidate -> capacityFor(candidate, unitType) == stillNeeded);
        }
        sizes.add(candidate -> isReasonableFit(capacityFor(candidate, unitType), stillNeeded));

        int smallestCover = Integer.MAX_VALUE;
        int largest = 0;
        for (int index = 0; index < table.getNumEntries(); index++) {
            MekSummary entry = table.getMekSummary(index);
            if ((entry == null) || !carries(entry, unitType)) {
                continue;
            }
            int capacity = capacityFor(entry, unitType);
            largest = Math.max(largest, capacity);
            if (capacity >= stillNeeded) {
                smallestCover = Math.min(smallestCover, capacity);
            }
        }
        if (smallestCover < Integer.MAX_VALUE) {
            int ceiling = smallestCover * FALLBACK_SPREAD;
            sizes.add(candidate -> (capacityFor(candidate, unitType) >= stillNeeded)
                  && (capacityFor(candidate, unitType) <= ceiling));
        } else if (largest > 0) {
            int floor = largest / FALLBACK_SPREAD;
            sizes.add(candidate -> capacityFor(candidate, unitType) >= floor);
        }

        for (UnitTable.UnitFilter size : sizes) {
            MekSummary hull = table.generateUnit(candidate -> carries(candidate, unitType) && size.include(candidate)
                  && ledger.wouldMostlyUse(baysOf(candidate)));
            if (hull != null) {
                return hull;
            }
        }
        for (UnitTable.UnitFilter size : sizes) {
            MekSummary hull = table.generateUnit(candidate -> carries(candidate, unitType) && size.include(candidate));
            if (hull != null) {
                return hull;
            }
        }
        return table.generateUnit(candidate -> carries(candidate, unitType));
    }

    /**
     * Whether a hull is a sensible size for what is still to be carried: it carries at least a
     * {@link #SMALLEST_USEFUL_SHARE third} of it and no more than all of it. Hulls that would sail with empty bays
     * are left alone, so a force is lifted by two or three hulls it fills rather than one it rattles around in; a
     * single big hull is drawn only when it is the size of the whole job.
     *
     * @param hullCapacity how many of the kind the hull carries
     * @param stillNeeded  how many of the kind have no bay yet
     *
     * @return {@code true} when the hull is worth drawing for that need
     */
    static boolean isReasonableFit(int hullCapacity, int stillNeeded) {
        boolean carriesAUsefulShare = (hullCapacity * SMALLEST_USEFUL_SHARE) >= stillNeeded;
        boolean wouldBeFilled = hullCapacity <= stillNeeded;
        return carriesAUsefulShare && wouldBeFilled;
    }

    /**
     * Books the bays of the hulls drawn so far against the units given a berth, honouring which bays each kind of
     * unit may ride in, and keeps count of what the force still needs.
     */
    static final class BayLedger {

        private final Map<Integer, Integer> needed;
        private final Map<Integer, Integer> capacity = new HashMap<>();
        private final Map<Integer, Integer> used = new HashMap<>();
        private final Map<Integer, Integer> berthed = new HashMap<>();

        /**
         * @param needed how many of each kind of unit want a berth
         */
        BayLedger(Map<Integer, Integer> needed) {
            this.needed = needed;
        }

        /**
         * Adds a hull's bays.
         *
         * @param bays bay capacity by the unit type the bay is built for
         */
        void add(Map<Integer, Integer> bays) {
            bays.forEach((bayType, count) -> capacity.merge(bayType, count, Integer::sum));
        }

        /**
         * @param unitType the kind of unit
         *
         * @return how many of that kind have neither a berth nor a bay waiting for them
         */
        int unmet(int unitType) {
            int outstanding = needed.getOrDefault(unitType, 0) - berthed.getOrDefault(unitType, 0);
            return Math.max(0, outstanding - free(unitType));
        }

        /**
         * Whether the force would put at least {@link #MIN_HULL_USE half} of a hull's bays, across every kind it
         * has, to use.
         *
         * @param hullBays the hull's bay capacity by the unit type each bay is built for
         *
         * @return {@code true} when enough of the hull would be used to be worth drawing
         */
        boolean wouldMostlyUse(Map<Integer, Integer> hullBays) {
            int berths = 0;
            int berthsUsed = 0;
            for (Map.Entry<Integer, Integer> bay : hullBays.entrySet()) {
                int bayBerths = asBerths(bay.getKey(), bay.getValue());
                berths += bayBerths;
                berthsUsed += Math.min(bayBerths, asBerths(bay.getKey(), unmetThatFit(bay.getKey())));
            }
            return (berthsUsed * MIN_HULL_USE) >= berths;
        }

        /**
         * Infantry bays and infantry demand are counted in tons while every other kind is a berth per unit. A foot
         * platoon's tonnage stands for one berth, so a hull's bays of every kind can be added up together.
         *
         * @param bayType the kind of bay
         * @param amount  a capacity or a demand in that bay's own unit of measure
         *
         * @return the same amount in berths
         */
        private static int asBerths(int bayType, int amount) {
            if (bayType == UnitType.INFANTRY) {
                return (int) Math.ceil(amount / (double) PlatoonType.FOOT.getWeight());
            }
            return amount;
        }

        /**
         * @return how many units without a bay could ride in the given kind of bay
         */
        private int unmetThatFit(int bayType) {
            int fit = 0;
            for (int unitType : needed.keySet()) {
                if (baysThatFit(unitType).contains(bayType)) {
                    fit += unmet(unitType);
                }
            }
            return fit;
        }

        /**
         * @param unitType the kind of unit
         *
         * @return how many of that kind still have a bay available, counting every bay type it fits
         */
        int free(int unitType) {
            int free = 0;
            for (int bayType : baysThatFit(unitType)) {
                free += capacity.getOrDefault(bayType, 0) - used.getOrDefault(bayType, 0);
            }
            return free;
        }

        /**
         * @return the bays nothing has claimed, by the unit type each bay is built for
         */
        Map<Integer, Integer> freeByBayType() {
            Map<Integer, Integer> free = new HashMap<>();
            capacity.forEach((bayType, count) -> {
                int unclaimed = count - used.getOrDefault(bayType, 0);
                if (unclaimed > 0) {
                    free.put(bayType, unclaimed);
                }
            });
            return free;
        }

        /**
         * Gives units berths, filling their own kind of bay before borrowing a larger one.
         *
         * @param unitType the kind of unit
         * @param count    how many to berth; no more than {@link #free(int)}
         */
        void claim(int unitType, int count) {
            berthed.merge(unitType, count, Integer::sum);
            int remaining = count;
            for (int bayType : baysThatFit(unitType)) {
                if (remaining == 0) {
                    return;
                }
                int available = capacity.getOrDefault(bayType, 0) - used.getOrDefault(bayType, 0);
                int taken = Math.min(available, remaining);
                used.merge(bayType, taken, Integer::sum);
                remaining -= taken;
            }
        }
    }

    /**
     * @return the bay types a kind of unit may ride in, its own first
     */
    private static List<Integer> baysThatFit(int unitType) {
        return BAYS_THAT_FIT.getOrDefault(unitType, List.of(unitType));
    }

    /**
     * Generates jump ships to provide enough docking collars to transport the given ratio of dropships.
     *
     * @param ratio            The ratio of jump ships to generate to the total needs of the unit
     * @param transportCollars The number of dropships generated for transport
     *
     * @return A list of generated jump ships
     */
    public List<MekSummary> calcJumpShips(double ratio, int transportCollars) {
        UnitTable table = UnitTable.findTable(factionRecord,
              UnitType.JUMPSHIP,
              year,
              rating,
              null,
              ModelRecord.NETWORK_NONE,
              EnumSet.noneOf(EntityMovementMode.class),
              EnumSet.noneOf(MissionRole.class),
              0);
        List<MekSummary> retVal = new ArrayList<>();
        int currentCapacity = 0;
        transportCollars = collarsStillNeeded(transportCollars);

        while (transportCollars * ratio > (double) currentCapacity) {
            // It's possible to have a jump ship with no docking collars, e.g. for scout use
            MekSummary jumpship = table.generateUnit(ms -> countHardpoints(ms) > 0);

            if (null == jumpship) {
                break; // Could not find any transport for the unit type; skip
            }

            currentCapacity += countHardpoints(jumpship);
            retVal.add(jumpship);
        }
        LOGGER.info("[ForceGen][Lift] {} JumpShip(s) with {} collar(s) for {} collar(s) still needed at {}%"
                    + " ({} free collar(s) already owned): {}",
              retVal.size(), currentCapacity, transportCollars, Math.round(ratio * 100),
              existingLift.freeDockingCollars(), describeHulls(retVal));
        return retVal;
    }

    /**
     * Generates WarShips to provide additional docking collars for transporting DropShips. WarShips also serve as
     * combat vessels in Clan toumans and IS naval fleets; this method only sizes the fleet to the docking-collar
     * requirement implied by the ratio. The ratio is independent of the jumpship ratio, so callers can request, for
     * example, 50% WarShip coverage + 50% JumpShip coverage to split the fleet evenly.
     *
     * @param ratio            The fraction (0.0–1.0+) of total DropShip docking demand to fulfill via WarShips
     * @param transportCollars The number of DropShips that need docking-collar capacity
     *
     * @return The list of generated WarShips. May be empty if no WarShip is available for the faction/year/rating.
     */
    public List<MekSummary> calcWarShips(double ratio, int transportCollars) {
        if (ratio <= 0) {
            return new ArrayList<>();
        }
        UnitTable table = UnitTable.findTable(factionRecord,
              UnitType.WARSHIP,
              year,
              rating,
              null,
              ModelRecord.NETWORK_NONE,
              EnumSet.noneOf(EntityMovementMode.class),
              EnumSet.noneOf(MissionRole.class),
              0);
        List<MekSummary> retVal = new ArrayList<>();
        int currentCapacity = 0;
        transportCollars = collarsStillNeeded(transportCollars);

        while (transportCollars * ratio > (double) currentCapacity) {
            MekSummary warship = table.generateUnit(ms -> countHardpoints(ms) > 0);
            if (null == warship) {
                break; // No WarShips available for this faction/year/rating
            }
            currentCapacity += countHardpoints(warship);
            retVal.add(warship);
        }
        LOGGER.info("[ForceGen][Lift] {} WarShip(s) with {} collar(s) for {} collar(s) still needed at {}%: {}",
              retVal.size(), currentCapacity, transportCollars, Math.round(ratio * 100), describeHulls(retVal));
        return retVal;
    }

    /**
     * The docking collars still to be found for the DropShips generated here and those the force already had,
     * after the collars the force already owns are used.
     *
     * @param generatedDropships how many DropShips the transport stage drew
     *
     * @return how many docking collars JumpShips and WarShips have to provide
     */
    private int collarsStillNeeded(int generatedDropships) {
        int dropships = generatedDropships + unitCounts.getOrDefault(UnitType.DROPSHIP, 0);
        return Math.max(0, dropships - existingLift.freeDockingCollars());
    }

    /**
     * Whether a craft is a liquid tanker rather than a transport that happens to carry some liquid.
     *
     * <p>A tanker can qualify on a stray unit bay and then be drawn over and over, because each one
     * contributes almost nothing toward the unit count being covered - a run that needed lift for a
     * regiment produced sixteen Aqueducts and twenty-six thousand tons of liquid tankage nobody asked
     * for. A hull whose holds are mostly liquid is not a troop transport, so it is kept out of the
     * unit-transport draw; the cargo lift selects it separately when liquid capacity is what is
     * wanted.</p>
     *
     * @param mekSummary the craft to test
     *
     * @return {@code true} when the craft's liquid capacity exceeds its dry capacity
     */
    private static boolean isPredominantlyTanker(MekSummary mekSummary) {
        CargoCapacity capacity = cargoCapacity(mekSummary);
        return capacity.liquidTons() > capacity.solidTons();
    }

    /**
     * @return {@code true} when the hull is a troop transport with at least one bay the kind of unit fits
     */
    private boolean carries(MekSummary hull, int unitType) {
        return (capacityFor(hull, unitType) > 0) && !isPredominantlyTanker(hull);
    }

    /**
     * @return how many of the kind of unit the hull carries, counting every bay type it fits
     */
    private int capacityFor(MekSummary hull, int unitType) {
        int capacity = 0;
        for (int bayType : baysThatFit(unitType)) {
            capacity += getBayCount(hull, bayType);
        }
        return capacity;
    }

    /**
     * Returns the Aerospace Fighter bay capacity of a unit (how many fighters it can carry), loading the Entity once
     * and caching the result. Used to size the carried fighter complement of WarShips, DropShips, JumpShips, and Space
     * Stations.
     *
     * @param mekSummary the carrier unit
     *
     * @return number of fighters the unit can carry, or 0 if none / could not be loaded
     */
    public static int fighterBayCapacity(MekSummary mekSummary) {
        if (!bayTypeCache.containsKey(mekSummary)) {
            try {
                Entity entity = new MekFileParser(mekSummary.getSourceFile(), mekSummary.getEntryName()).getEntity();
                bayTypeCache.put(mekSummary, countBays(entity));
            } catch (EntityLoadingException ex) {
                // Cache the failure as an empty bay map so we do not re-parse and re-throw this unit
                // on every subsequent call.
                bayTypeCache.put(mekSummary, Map.of());
            }
        }
        return bayTypeCache.get(mekSummary).getOrDefault(UnitType.AEROSPACE_FIGHTER, 0);
    }

    private int getBayCount(MekSummary ms, int unitType) {
        return baysOf(ms).getOrDefault(unitType, 0);
    }

    /**
     * @return the hull's bay capacity by the unit type each bay is built for; empty when the unit cannot be loaded
     */
    private Map<Integer, Integer> baysOf(MekSummary hull) {
        return bays(hull);
    }

    /**
     * The unit bays a design carries, loaded once and cached.
     *
     * @param hull the design
     *
     * @return its bay capacity by the unit type each bay is built for (infantry bays in tons); empty when the
     *       unit cannot be loaded
     */
    public static Map<Integer, Integer> bays(MekSummary hull) {
        if (bayTypeCache.containsKey(hull) || countBays(hull)) {
            return bayTypeCache.get(hull);
        }
        return Map.of();
    }

    /**
     * Loads the entity, counts the unit type transport capacity, and adds to the cache.
     *
     * @param ms The unit to load
     *
     * @return true if the Entity can be loaded and counted, false if there was an EntityLoadingException
     */
    private static boolean countBays(MekSummary ms) {
        try {
            Entity entity = new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            bayTypeCache.put(ms, countBays(entity));
            return true;
        } catch (EntityLoadingException ex) {
            return false;
        }
    }

    /**
     * Counts the unit type transport capacity, and adds to the cache.
     *
     * @param entity The transporting unit
     *
     * @return a Mapping of unit types with counts.
     */
    private static Map<Integer, Integer> countBays(Entity entity) {
        Map<Integer, Integer> bayCount = new HashMap<>();
        for (Bay bay : entity.getTransportBays()) {
            int bayType = bayType(bay);
            if (bayType != NOT_A_UNIT_BAY) {
                bayCount.merge(bayType, (int) bay.getCapacity(), Integer::sum);
            }
        }
        return bayCount;
    }

    /** What {@link #bayType(Bay)} returns for a bay that holds no units: cargo, quarters and the like. */
    static final int NOT_A_UNIT_BAY = -1;

    /**
     * The kind of unit a bay is built for, as the {@link UnitType} the lift ledger keys on.
     *
     * @param bay the bay
     *
     * @return the unit type key, or {@link #NOT_A_UNIT_BAY} when the bay holds no units
     */
    static int bayType(Bay bay) {
        // Infantry bays are measured in tons, the same unit the platoon demand is counted in.
        return switch (bay) {
            case MekBay mekBay -> UnitType.MEK;
            case ProtoMekBay protoMekBay -> UnitType.PROTOMEK;
            case HeavyVehicleBay heavyVehicleBay -> UnitType.TANK;
            case LightVehicleBay lightVehicleBay -> UnitType.VTOL;
            case SuperHeavyVehicleBay superHeavyVehicleBay -> UnitType.NAVAL;
            case BattleArmorBay battleArmorBay -> UnitType.BATTLE_ARMOR;
            case InfantryBay infantryBay -> UnitType.INFANTRY;
            case ASFBay fighterBay -> UnitType.AEROSPACE_FIGHTER;
            case SmallCraftBay smallCraftBay -> UnitType.SMALL_CRAFT;
            default -> NOT_A_UNIT_BAY;
        };
    }

    /**
     * Loads the Entity and counts the number of docking hard points.
     *
     * @param ms The unit to load
     *
     * @return The number of docking hard points on the unit.
     */
    private int countHardpoints(MekSummary ms) {
        return dockingCollars(ms);
    }

    /** Docking collars per hull, loaded once; a hull that cannot be loaded counts as having none. */
    private static final Map<MekSummary, Integer> dockingCollarCache = new HashMap<>();

    /**
     * @param hull the ship
     *
     * @return how many docking collars it has, or 0 when it has none or cannot be loaded
     */
    public static int dockingCollars(MekSummary hull) {
        return dockingCollarCache.computeIfAbsent(hull, summary -> {
            try {
                Entity entity = new MekFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
                // TODO: count drop shuttle bays
                return entity.getDockingCollars().size();
            } catch (EntityLoadingException exception) {
                return 0;
            }
        });
    }

}
