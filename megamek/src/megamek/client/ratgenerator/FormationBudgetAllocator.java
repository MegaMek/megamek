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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * Reassigns the formation types across a force to match a requested {@link FormationMix}.
 *
 * <p>Runs after the force tree is built and before any unit is drawn, so every formation has been assigned and every
 * node knows what its rule offered, but nothing has been picked yet. Reassigning in that window rather than
 * intercepting each weighted pick as it happens is what makes a percentage possible at all: a pick made node by node
 * has no idea how many nodes remain or what the whole force is going to look like.</p>
 *
 * <p>Only formations the node's own rule offered are ever assigned to it, so a lance can never be given a formation
 * the ruleset would not have allowed it. Nodes the mix does not claim keep whatever the ruleset rolled for them,
 * which is what lets a partial mix shift only what was asked for.</p>
 *
 * <p>Every formation remains a legal Campaign Operations formation and still picks its own units under its own
 * constraints. This decides which legal formation each node gets, nothing more.</p>
 */
final class FormationBudgetAllocator {

    private static final MMLogger LOGGER = MMLogger.create(FormationBudgetAllocator.class);

    private FormationBudgetAllocator() {}

    /**
     * Reassigns formation types across the given force to match its requested mix.
     *
     * <p>A force whose mix is empty is left completely untouched, which is what keeps untouched controls generating
     * exactly as they did before the mix existed.</p>
     *
     * @param root the root of a built force tree
     *
     * @return what was requested against what was placed, or {@code null} when no mix was applied
     */
    static FormationMixReport allocate(ForceDescriptor root) {
        if (root == null) {
            return null;
        }
        FormationMix requestedMix = root.getFormationMix();
        if (requestedMix.isEmpty()) {
            return null;
        }
        FormationMixPreview preview = FormationMixPreview.of(root);
        List<String> warnings = new ArrayList<>();

        // Requests this force can never supply are dropped before the arithmetic, so the formations that can be
        // placed get the share they were promised rather than losing part of it to one that was never achievable.
        FormationMix mix = requestedMix;
        if (!requestedMix.allowUnofferedFormations()) {
            Set<String> unavailable = requestedMix.unavailableIn(preview);
            unavailable.forEach(formationName -> warnings.add(unavailableWarning(formationName)));
            mix = requestedMix.restrictedTo(preview);
        }

        List<ForceDescriptor> tweakable = tweakableNodes(root);
        if (mix.isEmpty() || tweakable.isEmpty()) {
            LOGGER.debug("[ForceGen][FormationMix] nothing to place: {} tweakable node(s), {} supportable request(s)",
                  tweakable.size(), mix.requestedFormations().size());
            return new FormationMixReport(preview, Map.of(), Map.of(), warnings);
        }

        Map<String, Integer> requested = integerQuotas(mix, tweakable.size());
        Map<String, Integer> assigned = new TreeMap<>();
        Map<ForceDescriptor, String> claimed = new HashMap<>();

        for (String formationName : rarestFirst(tweakable, requested.keySet())) {
            int quota = requested.get(formationName);
            // Lances the ruleset already rolled into this formation count towards the request before any are
            // moved. Asking for six Recon when the roll produced four should move two lances, not six.
            int placed = claimAlreadyMatching(tweakable, claimed, formationName, quota);
            placed += assignFormation(tweakable, claimed, formationName, quota - placed,
                  mix.allowUnofferedFormations());
            if (placed > 0) {
                assigned.put(formationName, placed);
            }
        }
        displaceSurplus(tweakable, claimed, requested.keySet());
        // Deliberately no shortfall warning here. A formation assigned now can still fail its own Campaign
        // Operations requirements once units are drawn, at which point buildFormation drops it back to an ordinary
        // lance - so what was placed is not yet what will be delivered. Shortfalls are worked out in
        // tallyAchieved, after generation, against what the player will actually see.

        applyClaims(claimed);
        logOutcome(tweakable.size(), requested, assigned);
        return new FormationMixReport(preview, requested, assigned, warnings);
    }

    /**
     * Recounts what the force actually ended up with, once its units have been drawn.
     *
     * <p>Assignment is not delivery. A formation the ruleset offered can still fail its own Campaign Operations
     * requirements when the units are picked - a Battle Lance needs three of Brawler, Sniper or Skirmisher and half
     * its units Heavy or better, which a Light lance cannot supply - and
     * {@code ForceDescriptor.buildFormation} then drops it back to an ordinary lance. Counting at assignment time
     * would report those as delivered and read as a mix that quietly under-performed.</p>
     *
     * @param root             the generated force
     * @param assignmentReport what the allocator asked for, or {@code null} if no mix was applied
     *
     * @return the report with real achieved counts and the shortfalls that actually occurred
     */
    static FormationMixReport tallyAchieved(ForceDescriptor root, FormationMixReport assignmentReport) {
        if ((root == null) || (assignmentReport == null)) {
            return assignmentReport;
        }
        Map<String, Integer> achieved = new TreeMap<>();
        countSurviving(root, achieved);

        List<String> warnings = new ArrayList<>(assignmentReport.warnings());
        assignmentReport.requestedNodes().forEach((formationName, requested) -> {
            int delivered = achieved.getOrDefault(formationName, 0);
            if (delivered < requested) {
                warnings.add(shortfallWarning(formationName, requested, delivered));
            }
        });
        LOGGER.info("[ForceGen][FormationMix] delivered {} against requested {}",
              achieved, assignmentReport.requestedNodes());
        return new FormationMixReport(assignmentReport.preview(), assignmentReport.requestedNodes(), achieved,
              warnings);
    }

    /** Counts the formation each tweakable node actually kept once its units were drawn. */
    private static void countSurviving(ForceDescriptor node, Map<String, Integer> achieved) {
        if ((node.getEligibleFormations().size() > 1) && (node.getFormation() != null)) {
            achieved.merge(node.getFormation().getName(), 1, Integer::sum);
        }
        node.getSubForces().forEach(subForce -> countSurviving(subForce, achieved));
        node.getAttached().forEach(attachedForce -> countSurviving(attachedForce, achieved));
    }

    /**
     * The nodes a mix may reassign: those whose rule offered more than one formation.
     *
     * <p>A node offered exactly one had no choice to make - a command lance is almost always this - and reassigning
     * it would be inventing a formation the ruleset never allowed there.</p>
     */
    private static List<ForceDescriptor> tweakableNodes(ForceDescriptor root) {
        List<ForceDescriptor> tweakable = new ArrayList<>();
        collectTweakable(root, tweakable);
        return tweakable;
    }

    private static void collectTweakable(ForceDescriptor node, List<ForceDescriptor> tweakable) {
        if (node.getEligibleFormations().size() > 1) {
            tweakable.add(node);
        }
        node.getSubForces().forEach(subForce -> collectTweakable(subForce, tweakable));
        node.getAttached().forEach(attachedForce -> collectTweakable(attachedForce, tweakable));
    }

    /**
     * Orders formations by how few nodes can take them, rarest first.
     *
     * <p>Load-bearing rather than tidy. A formation offered almost everywhere and one offered by three nodes are not
     * interchangeable: if the common one chooses first it can take the very nodes the rare one depended on and leave
     * it unplaceable when it did not have to be.</p>
     *
     * @param tweakable      the nodes available
     * @param formationNames the formations to order
     *
     * @return the formations, fewest eligible nodes first
     */
    static List<String> rarestFirst(List<ForceDescriptor> tweakable, Iterable<String> formationNames) {
        List<String> ordered = new ArrayList<>();
        formationNames.forEach(ordered::add);
        ordered.sort(Comparator.comparingInt(formationName -> eligibleNodeCount(tweakable, formationName)));
        return ordered;
    }

    /**
     * Claims lances that already hold the requested formation, up to the quota.
     *
     * <p>Run before any lance is moved, so a request is measured against what the roll already produced. Without
     * this a request for six Recon lances is six <em>more</em> Recon lances, on top of however many came up on
     * their own - the number the player typed would not be the number they got.</p>
     *
     * @param tweakable     every lance the mix may reassign
     * @param claimed       lances already claimed this pass, added to here
     * @param formationName the formation being requested
     * @param quota         how many lances are wanted in total
     *
     * @return how many were already in place and have now been claimed
     */
    private static int claimAlreadyMatching(List<ForceDescriptor> tweakable, Map<ForceDescriptor, String> claimed,
          String formationName, int quota) {
        int claimedHere = 0;
        for (ForceDescriptor node : tweakable) {
            if (claimedHere >= quota) {
                break;
            }
            if (claimed.containsKey(node) || !holdsFormation(node, formationName)) {
                continue;
            }
            claimed.put(node, formationName);
            claimedHere++;
        }
        return claimedHere;
    }

    /**
     * Moves lances off a requested formation once that request is full.
     *
     * <p>The other half of counting what the roll already produced. If seven lances rolled Recon and the player
     * asked for four, three have to be moved off it or the force delivers seven and the number means nothing. Each
     * is given the likeliest of the other formations its own rule offered, so it stays a lance the ruleset would
     * have allowed.</p>
     *
     * @param tweakable every lance the mix may reassign
     * @param claimed   lances claimed so far, added to here
     * @param targeted  the formations the player asked for by name
     */
    private static void displaceSurplus(List<ForceDescriptor> tweakable, Map<ForceDescriptor, String> claimed,
          Set<String> targeted) {
        for (ForceDescriptor node : tweakable) {
            if (claimed.containsKey(node)) {
                continue;
            }
            String current = currentFormationName(node);
            if ((current == null) || !targeted.contains(current)) {
                continue;
            }
            String replacement = node.getEligibleFormations()
                  .entrySet()
                  .stream()
                  .filter(offer -> !targeted.contains(offer.getKey()))
                  .max(Map.Entry.comparingByValue())
                  .map(Map.Entry::getKey)
                  .orElse(null);
            if (replacement == null) {
                LOGGER.debug("[ForceGen][FormationMix] {} rolled {} and the request for it is full, but its rule"
                            + " offers nothing else; leaving it", node.parseName(), current);
                continue;
            }
            claimed.put(node, replacement);
        }
    }

    private static @Nullable String currentFormationName(ForceDescriptor node) {
        return (node.getFormation() == null) ? null : node.getFormation().getName();
    }

    private static boolean holdsFormation(ForceDescriptor node, String formationName) {
        return formationName.equals(currentFormationName(node));
    }

    private static int eligibleNodeCount(List<ForceDescriptor> tweakable, String formationName) {
        return (int) tweakable.stream()
              .filter(node -> node.getEligibleFormations().containsKey(formationName))
              .count();
    }

    /**
     * Claims up to {@code quota} nodes for one formation.
     *
     * <p>Among the nodes that could take it, the most constrained go first - those with the fewest alternatives of
     * their own. A node offering two formations is hard to use for anything else, while one offering eleven can
     * still serve almost any later request, so spending the flexible node first would strand the narrow one.</p>
     *
     * @param tweakable      every node the mix may reassign
     * @param claimed        nodes already claimed this pass, added to here
     * @param formationName  the formation to place
     * @param quota          how many nodes to claim
     *
     * @return how many nodes were claimed
     */
    static int assignFormation(List<ForceDescriptor> tweakable, Map<ForceDescriptor, String> claimed,
          String formationName, int quota) {
        return assignFormation(tweakable, claimed, formationName, quota, false);
    }

    /**
     * Claims up to {@code quota} nodes for one formation.
     *
     * @param tweakable              every node the mix may reassign
     * @param claimed                nodes already claimed this pass, added to here
     * @param formationName          the formation to place
     * @param quota                  how many nodes to claim
     * @param allowUnofferedFormations {@code true} to use any free node rather than only those offered this formation
     *
     * @return how many nodes were claimed
     */
    static int assignFormation(List<ForceDescriptor> tweakable, Map<ForceDescriptor, String> claimed,
          String formationName, int quota, boolean allowUnofferedFormations) {
        List<ForceDescriptor> candidates = tweakable.stream()
              .filter(node -> !claimed.containsKey(node))
              .filter(node -> allowUnofferedFormations
                    || node.getEligibleFormations().containsKey(formationName))
              // Nodes the ruleset already offered this formation are used before ones being overridden onto it, so
              // an override departs from the ruleset only as far as the request actually requires.
              .sorted(Comparator
                    .comparing((ForceDescriptor node) ->
                          node.getEligibleFormations().containsKey(formationName) ? 0 : 1)
                    .thenComparingInt(node -> node.getEligibleFormations().size()))
              .toList();
        int placed = 0;
        for (ForceDescriptor node : candidates) {
            if (placed >= quota) {
                break;
            }
            claimed.put(node, formationName);
            placed++;
        }
        return placed;
    }

    /**
     * Writes the claims onto the tree. Nodes with no claim are left exactly as the ruleset rolled them, which is what
     * lets a mix that asks for 30% of one formation leave the other 70% alone.
     */
    private static void applyClaims(Map<ForceDescriptor, String> claimed) {
        claimed.forEach((node, formationName) -> {
            FormationType formationType = FormationType.getFormationType(formationName);
            if (formationType == null) {
                LOGGER.warn("[ForceGen][FormationMix] '{}' is not a registered formation type; leaving {} as rolled",
                      formationName, node.parseName());
                return;
            }
            node.setFormationType(formationType);
        });
    }

    /**
     * Trims the requested lance counts to what the force can actually hold.
     *
     * <p>The player types lances directly, so there is nothing to convert. All this has to settle is a request that
     * asks for more lances than the force has: rather than letting whichever formation is processed first take
     * everything, every request is scaled back in proportion, so asking for 30 Recon and 10 Fire from a 20 lance
     * force yields 15 and 5 - the ratio that was expressed, at the size that fits.</p>
     *
     * <p>Scaling back uses the largest-remainder method so the trimmed counts still add up to the force: a
     * three-way even split of ten lances comes out 4/3/3 rather than 3/3/3 with a lance silently lost.</p>
     *
     * @param mix       the requested distribution, already restricted to what the force offers
     * @param nodeCount how many lances the mix may reassign
     *
     * @return lances per formation, omitting any that scale down to nothing
     */
    static Map<String, Integer> integerQuotas(FormationMix mix, int nodeCount) {
        return new TreeMap<>(mix.scaledTo(nodeCount).lances());
    }

    private static String unavailableWarning(String formationName) {
        return String.format("%s: this force never offers that formation, so none were placed.", formationName);
    }

    private static String shortfallWarning(String formationName, int requested, int placed) {
        return String.format("%s: asked for %d, placed %d - too few formations can take it.",
              formationName, requested, placed);
    }

    private static void logOutcome(int tweakableNodes, Map<String, Integer> requested, Map<String, Integer> assigned) {
        int assignedTotal = assigned.values().stream().mapToInt(Integer::intValue).sum();
        LOGGER.info("[ForceGen][FormationMix] {} tweakable formation(s): requested {}, placed {},"
                    + " {} left as the ruleset rolled them",
              tweakableNodes, requested, assigned, tweakableNodes - assignedTotal);
    }
}
