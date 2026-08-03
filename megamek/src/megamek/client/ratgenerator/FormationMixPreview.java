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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * What a formation mix would have to work with for one force: how many of its formations are a genuine choice, and
 * what the ruleset's own odds for each type are.
 *
 * <p>Read from a structure-only build (see {@link Ruleset#buildStructureOnly}), so it describes the force the current
 * options would actually produce rather than a guess made from the ruleset files.</p>
 *
 * <p>The shares are the ruleset's <em>expected</em> distribution, worked out from the option weights, not a tally of
 * one roll. A single roll of a couple of dozen nodes is far too noisy to present as "the default": the same force
 * generated twice would show the player two different baselines to adjust away from.</p>
 *
 * <p>This is a transient build-time value; it is never serialized into a saved game.</p>
 *
 * @param formationNodes       nodes that were assigned a formation at all
 * @param tweakableNodes       nodes that were offered more than one formation, and so could be assigned differently
 * @param defaultSharePercent  each offered formation against the share of tweakable nodes the ruleset expects it to
 *                             take, as a percentage
 * @param placeableNodes       each offered formation against how many tweakable nodes could actually be given it,
 *                             which is the most a request for that formation can ever deliver
 */
public record FormationMixPreview(int formationNodes,
      int tweakableNodes,
      Map<String, Double> defaultSharePercent,
      Map<String, Integer> placeableNodes) {

    /** A preview of nothing, for a force that has not been built or holds no formations. */
    public static final FormationMixPreview EMPTY = new FormationMixPreview(0, 0, Map.of(), Map.of());

    /** Canonical constructor, taking a defensive immutable copy ordered by formation name. */
    public FormationMixPreview {
        defaultSharePercent = Collections.unmodifiableMap(new TreeMap<>(defaultSharePercent));
        placeableNodes = Collections.unmodifiableMap(new TreeMap<>(placeableNodes));
    }

    /**
     * The most of this force a formation can ever be, as a percentage of the lances that can be reassigned.
     *
     * <p>A lance is only ever given a formation its own ruleset offered it, so a formation no lance is offered
     * cannot be placed at all, and one offered to a third of them tops out at a third. This is the number the
     * player's request has to be measured against; asking beyond it is asking for something no roll can produce.</p>
     *
     * @param formationName the formation type to look up
     *
     * @return the ceiling as a whole percentage, {@code 0} when the force never offers it
     */
    public int ceilingPercentFor(String formationName) {
        if ((formationName == null) || (tweakableNodes <= 0)) {
            return 0;
        }
        int placeable = placeableNodes.getOrDefault(formationName.trim(), 0);
        return (int) Math.floor((100.0 * placeable) / tweakableNodes);
    }

    /**
     * Surveys a built force structure.
     *
     * @param root the root of a force tree built by {@link Ruleset#buildStructureOnly}
     *
     * @return what a mix would have to work with, never {@code null}
     */
    public static FormationMixPreview of(ForceDescriptor root) {
        if (root == null) {
            return EMPTY;
        }
        Tally tally = new Tally();
        walk(root, tally);
        return tally.toPreview();
    }

    /**
     * Combines several samples of the same force into one stable picture.
     *
     * <p>A single build is not representative. Weight class is rolled per node while the tree is being built, and
     * seven out of ten formation options are gated on it, so two builds of the same regiment offer noticeably
     * different formations - one sample of a Draconis Combine regiment offered 27 types and the next 18. Showing the
     * player a list that changes every time they open the editor would be worse than useless.</p>
     *
     * <p>Averaging over samples answers the question they are actually asking: what could this force contain, and
     * how often. Sampling is affordable because a structure-only build costs a twentieth of a generation.</p>
     *
     * @param samples previews of the same force parameters, each from its own build
     *
     * @return the combined picture, or {@link #EMPTY} when there are no samples
     */
    public static FormationMixPreview merged(List<FormationMixPreview> samples) {
        if ((samples == null) || samples.isEmpty()) {
            return EMPTY;
        }
        int formationNodeTotal = 0;
        int tweakableTotal = 0;
        Map<String, Double> shareTotals = new TreeMap<>();
        Map<String, Integer> placeableTotals = new TreeMap<>();
        for (FormationMixPreview sample : samples) {
            formationNodeTotal += sample.formationNodes();
            tweakableTotal += sample.tweakableNodes();
            sample.defaultSharePercent().forEach((formationName, share) ->
                  shareTotals.merge(formationName, share, Double::sum));
            sample.placeableNodes().forEach((formationName, placeable) ->
                  placeableTotals.merge(formationName, placeable, Integer::sum));
        }
        Map<String, Double> averaged = new TreeMap<>();
        shareTotals.forEach((formationName, total) -> averaged.put(formationName, total / samples.size()));
        Map<String, Integer> averagedPlaceable = new TreeMap<>();
        placeableTotals.forEach((formationName, total) ->
              averagedPlaceable.put(formationName, Math.round((float) total / samples.size())));
        return new FormationMixPreview(Math.round((float) formationNodeTotal / samples.size()),
              Math.round((float) tweakableTotal / samples.size()), averaged, averagedPlaceable);
    }

    /**
     * @return {@code true} when at least one formation in this force could be assigned differently
     */
    public boolean hasAnythingToTweak() {
        return tweakableNodes > 0;
    }

    /**
     * @return every formation type on offer anywhere in this force, in name order
     */
    public Set<String> offeredFormations() {
        return defaultSharePercent.keySet();
    }

    /**
     * The share of tweakable nodes the ruleset expects to give one formation type.
     *
     * @param formationName the formation type to look up
     *
     * @return the expected percentage, or {@code 0} when this force never offers that type
     */
    public double defaultShareFor(String formationName) {
        return defaultSharePercent.getOrDefault(formationName, 0.0);
    }

    private static void walk(ForceDescriptor node, Tally tally) {
        tally.classify(node);
        node.getSubForces().forEach(subForce -> walk(subForce, tally));
        node.getAttached().forEach(attachedForce -> walk(attachedForce, tally));
    }

    /** Running counts, assembled into an immutable {@link FormationMixPreview}. */
    private static final class Tally {
        private final Map<String, Double> expectedNodes = new TreeMap<>();
        private final Map<String, Integer> placeableNodes = new TreeMap<>();
        private int formationNodes;
        private int tweakableNodes;

        private void classify(ForceDescriptor node) {
            Map<String, Integer> offered = node.getEligibleFormations();
            if (offered.isEmpty()) {
                return;
            }
            formationNodes++;
            if (offered.size() == 1) {
                // The rule narrowed to one formation, so this node made no choice and a mix cannot change it.
                // Command lances are almost all of this case.
                return;
            }
            tweakableNodes++;
            // Every formation this node was offered is one more node that could be given it, which is what sets
            // the ceiling on a request for that formation.
            offered.keySet().forEach(formationName -> placeableNodes.merge(formationName, 1, Integer::sum));
            int totalWeight = offered.values().stream().mapToInt(Integer::intValue).sum();
            if (totalWeight <= 0) {
                return;
            }
            // Accumulate the probability this node gives each formation. Summed over nodes and divided by their
            // count, that is the distribution the ruleset produces on average - which is the baseline to adjust.
            offered.forEach((formationName, weight) ->
                  expectedNodes.merge(formationName, (double) weight / totalWeight, Double::sum));
        }

        private FormationMixPreview toPreview() {
            Map<String, Double> shares = new TreeMap<>();
            if (tweakableNodes > 0) {
                expectedNodes.forEach((formationName, expected) ->
                      shares.put(formationName, 100.0 * expected / tweakableNodes));
            }
            return new FormationMixPreview(formationNodes, tweakableNodes, shares, placeableNodes);
        }
    }
}
