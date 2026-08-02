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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitRole;
import megamek.logging.MMLogger;

/**
 * Distributes a {@link RoleMix} across the unit slots a force can actually shape.
 *
 * <p>Runs after the force tree is built and before any unit is picked, so every slot's weight class is final and
 * nothing has been rolled yet. That window is what makes the routing in {@link #assignRole} possible: the allocator
 * can see the whole force at once and place each role where the tables can supply it, instead of each slot
 * discovering on its own that what it was asked for does not exist.</p>
 *
 * <p>Role availability is strongly conditioned on weight class - Juggernaut and Skirmisher appear in no Light table
 * measured, Striker in no Assault one - so a mix spread evenly across weight bands would be unsatisfiable most of the
 * time. Routing each role's quota to the bands that hold it turns most of those failures into ordinary assignments.</p>
 *
 * <p>Weight class is never traded away to satisfy a role. A slot keeps the tonnage the ruleset gave it and takes a
 * substitute role instead, because Target Weight, the ruleset's own weight distributions and any
 * {@code <weightTarget>} block all exist to protect that profile.</p>
 */
final class RoleBudgetAllocator {

    private static final MMLogger LOGGER = MMLogger.create(RoleBudgetAllocator.class);

    private RoleBudgetAllocator() {}

    /**
     * Identifies slots that draw from the same unit table, so the table is built and histogrammed once per group.
     *
     * @param unitType    the slots' unit type
     * @param weightClass the slots' weight class, or {@code null} when the type has none
     * @param rating      the slots' equipment rating
     */
    private record BucketKey(int unitType, Integer weightClass, String rating) {}

    /** The slots of one bucket and which roles their shared table can supply. */
    static final class Bucket {
        final List<ForceDescriptor> slots = new ArrayList<>();
        Map<UnitRole, Integer> availableRoles = Map.of();

        boolean canSupply(UnitRole role) {
            return availableRoles.getOrDefault(role, 0) > 0;
        }
    }

    /**
     * Assigns a target role to the governed slots of the given force.
     *
     * <p>A force whose mix is empty is left completely untouched, which is what keeps an untouched spinner grid
     * generating exactly as it did before role targeting existed.</p>
     *
     * @param root the root of a built force tree
     *
     * @return what was requested against what was assigned, or {@code null} when no mix was applied
     */
    static RoleAssignmentReport allocate(ForceDescriptor root) {
        if (root == null) {
            return null;
        }
        RoleMix mix = root.getRoleMix();
        if (mix.isEmpty()) {
            return null;
        }
        RoleSlotSurvey.SurveyResult survey = RoleSlotSurvey.collect(root);
        List<ForceDescriptor> governedSlots = survey.governedLeaves();
        List<String> warnings = new ArrayList<>();
        if (governedSlots.isEmpty()) {
            LOGGER.debug("[ForceGen][RoleTarget] mix requested {} but no slot is governable; leaving the force alone",
                  mix.percentages());
            warnings.add(noGovernableSlotsWarning(survey.report()));
            return new RoleAssignmentReport(survey.report(), Map.of(), Map.of(), warnings);
        }

        Map<BucketKey, Bucket> buckets = bucketSlots(governedSlots);
        buckets.forEach(RoleBudgetAllocator::histogram);

        Map<UnitRole, Integer> requested = integerQuotas(mix, governedSlots.size());
        Map<UnitRole, Integer> assigned = new EnumMap<>(UnitRole.class);

        for (UnitRole role : scarcestFirst(buckets.values(), requested.keySet())) {
            int placed = assignRole(buckets.values(), role, requested.get(role));
            if (placed > 0) {
                assigned.put(role, placed);
            }
            if (placed < requested.get(role)) {
                warnings.add(shortfallWarning(role, requested.get(role), placed));
            }
        }

        logOutcome(mix, governedSlots.size(), requested, assigned);
        return new RoleAssignmentReport(survey.report(), requested, assigned, warnings);
    }

    /**
     * Groups slots that share a unit table.
     *
     * @param governedSlots the slots the mix may shape
     *
     * @return one bucket per distinct table, in encounter order
     */
    private static Map<BucketKey, Bucket> bucketSlots(List<ForceDescriptor> governedSlots) {
        Map<BucketKey, Bucket> buckets = new LinkedHashMap<>();
        for (ForceDescriptor slot : governedSlots) {
            BucketKey key = new BucketKey(slot.getUnitType(), slot.getWeightClass(), slot.ratGeneratorRating());
            buckets.computeIfAbsent(key, ignored -> new Bucket()).slots.add(slot);
        }
        return buckets;
    }

    /**
     * Counts how many entries of each role the bucket's table holds. A role with no entries cannot be assigned here
     * however much of it was asked for.
     */
    private static void histogram(BucketKey key, Bucket bucket) {
        UnitTable table = bucket.slots.getFirst().primaryUnitTable();
        if (table == null) {
            LOGGER.debug("[ForceGen][RoleTarget] bucket {}/{}/{} has no unit table; its {} slot(s) stay unconstrained",
                  ForceDescriptor.describeUnitType(key.unitType()), describeWeightClass(key.weightClass()),
                  key.rating(), bucket.slots.size());
            return;
        }
        Map<UnitRole, Integer> counts = new EnumMap<>(UnitRole.class);
        for (int entry = 0; entry < table.getNumEntries(); entry++) {
            MekSummary unit = table.getMekSummary(entry);
            if (unit != null) {
                counts.merge(unit.getRole(), 1, Integer::sum);
            }
        }
        bucket.availableRoles = counts;
        LOGGER.debug("[ForceGen][RoleTarget] bucket {}/{}/{}: {} slot(s), {} table entries, roles available {}",
              ForceDescriptor.describeUnitType(key.unitType()), describeWeightClass(key.weightClass()), key.rating(),
              bucket.slots.size(), table.getNumEntries(), counts);
    }

    /**
     * Places up to {@code quota} slots of one role, preferring the buckets whose tables hold the most of it.
     *
     * <p>Only buckets that can actually supply the role are considered, so a Light lance is never handed a Juggernaut
     * it cannot fill. Slots left unassigned here fall to the generation ladder, which walks the role's fallback chain
     * inside the slot's own weight class.</p>
     *
     * @return how many slots were assigned
     */
    static int assignRole(Collection<Bucket> buckets, UnitRole role, int quota) {
        List<Bucket> viable = buckets
              .stream()
              .filter(bucket -> bucket.canSupply(role))
              .sorted(Comparator.comparingInt((Bucket bucket) -> bucket.availableRoles.getOrDefault(role, 0))
                    .reversed())
              .toList();
        int placed = 0;
        for (Bucket bucket : viable) {
            for (ForceDescriptor slot : bucket.slots) {
                if (placed >= quota) {
                    return placed;
                }
                if (slot.getTargetUnitRole() == null) {
                    slot.setTargetUnitRole(role);
                    placed++;
                }
            }
        }
        return placed;
    }

    /**
     * Orders roles by how few buckets can supply them, scarcest first.
     *
     * <p>This ordering is load-bearing rather than cosmetic. Juggernaut exists only in the heavier bands while
     * Brawler exists in all of them, so letting the abundant role pick first can consume the very slots the scarce
     * one depends on and leave it unfillable when it did not have to be.</p>
     *
     * @param buckets the buckets the force was grouped into
     * @param roles   the roles to order
     *
     * @return the roles, fewest viable buckets first
     */
    static List<UnitRole> scarcestFirst(Collection<Bucket> buckets, Collection<UnitRole> roles) {
        List<UnitRole> ordered = new ArrayList<>(roles);
        ordered.sort(Comparator.comparingInt(role -> viableBucketCount(buckets, role)));
        return ordered;
    }

    /**
     * @return how many buckets hold at least one unit of the given role
     */
    private static int viableBucketCount(Collection<Bucket> buckets, UnitRole role) {
        return (int) buckets.stream().filter(bucket -> bucket.canSupply(role)).count();
    }

    /**
     * Converts requested percentages into whole slots by the largest-remainder method, so a three-way 33/33/33 split
     * across ten slots comes out 4/3/3 rather than 3/3/3 with a slot silently lost.
     *
     * <p>Percentages need not total 100. Whatever is left over stays unassigned and rolls without a role constraint,
     * which is what lets a mix ask for "at least some snipers" instead of having to account for every slot.</p>
     *
     * @param mix        the requested distribution
     * @param slotCount  how many slots the mix may shape
     *
     * @return slots per role, omitting roles that round down to nothing
     */
    static Map<UnitRole, Integer> integerQuotas(RoleMix mix, int slotCount) {
        Map<UnitRole, Integer> quotas = new EnumMap<>(UnitRole.class);
        Map<UnitRole, Double> remainders = new HashMap<>();
        // An over-subscribed mix cannot have every role in full, so scale them all back proportionally rather than
        // letting the roles that happen to be processed first take everything. 80/80 becomes 50/50, which is the
        // ratio the user expressed even though the totals were not achievable.
        double oversubscriptionScale = Math.min(1.0, 100.0 / Math.max(1, mix.totalPercent()));
        int allocated = 0;
        for (UnitRole role : mix.requestedRoles()) {
            double exact = slotCount * mix.percentFor(role) * oversubscriptionScale / 100.0;
            int whole = (int) Math.floor(exact);
            quotas.put(role, whole);
            remainders.put(role, exact - whole);
            allocated += whole;
        }
        // Hand out the slots lost to flooring, largest fractional part first, without exceeding what was asked for.
        // The target is rounded rather than floored so a mix totalling 99% over ten slots claims all ten instead of
        // surrendering one to the unconstrained remainder over a rounding artefact.
        long requestedTotal = Math.round(slotCount * Math.min(100, mix.totalPercent()) / 100.0);
        List<UnitRole> byRemainder = new ArrayList<>(remainders.keySet());
        byRemainder.sort(Comparator.comparingDouble((UnitRole role) -> remainders.get(role)).reversed());
        for (UnitRole role : byRemainder) {
            if (allocated >= Math.min(requestedTotal, slotCount)) {
                break;
            }
            quotas.merge(role, 1, Integer::sum);
            allocated++;
        }
        quotas.values().removeIf(count -> count == 0);
        return quotas;
    }

    private static String noGovernableSlotsWarning(RoleCoverageReport coverage) {
        return String.format("No unit slot in this force can take a role target: %d of %d are set by formations,"
                    + " %d belong to support detachments and %d have no weight class.",
              coverage.slotsSetByFormation(), coverage.totalUnitSlots(),
              coverage.slotsInAttachedForces(), coverage.slotsExcludedByUnitType());
    }

    private static String shortfallWarning(UnitRole role, int requested, int placed) {
        return String.format("%s: asked for %d slot(s), placed %d - the rest use the nearest available role.",
              role, requested, placed);
    }

    private static void logOutcome(RoleMix mix, int governedSlots, Map<UnitRole, Integer> requested,
          Map<UnitRole, Integer> assigned) {
        int assignedTotal = assigned.values().stream().mapToInt(Integer::intValue).sum();
        LOGGER.info("[ForceGen][RoleTarget] mix {} over {} governed slot(s): requested {}, assigned {},"
                    + " {} left unconstrained",
              mix.percentages(), governedSlots, requested, assigned, governedSlots - assignedTotal);
    }

    /**
     * Renders a weight class for diagnostics without unboxing a {@code null}.
     *
     * @param weightClass the weight class, or {@code null} for a unit type that has none
     *
     * @return the class name, or {@code "none"}
     */
    private static String describeWeightClass(@Nullable Integer weightClass) {
        return (weightClass == null) ? "none" : EntityWeightClass.getClassName(weightClass);
    }
}
