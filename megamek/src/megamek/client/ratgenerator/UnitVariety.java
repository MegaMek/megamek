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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.annotation.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.logging.MMLogger;

/**
 * Makes repeat units less likely while a force is generated, for hosts that ask for variety.
 *
 * <p>The unit tables are drawn with replacement, and a faction's tables are top-heavy: the commonest chassis can
 * carry several times the weight of a typical one. Left alone, a company often holds two or three of the same unit,
 * which players read as the generator repeating itself. While one of these is active, every draw from a unit table
 * scales each candidate's weight down by what the force already holds:</p>
 *
 * <ul>
 *     <li>{@value #SAME_MODEL_IN_FORMATION} for each copy of the exact model already in the same formation (the
 *     lance, Star or Level II the slot belongs to);</li>
 *     <li>{@value #SAME_CHASSIS_IN_FORMATION} for each other variant of the same chassis already in that
 *     formation;</li>
 *     <li>{@value #SAME_CHASSIS_ELSEWHERE} for each copy of the chassis anywhere else in the command, earlier rolls
 *     included, never falling below {@value #ELSEWHERE_FLOOR} so a faction's signature units still turn up
 *     often.</li>
 * </ul>
 *
 * <p>Weights are only scaled, never removed, so a draw that has a single candidate - a model pinned by a
 * {@code generate="model"} block, an artillery battery, a matched fighter pair - is unaffected, and a lance can still
 * repeat a unit when its table genuinely offers nothing else.</p>
 *
 * <p>One instance lives for one generation call: a roll, an added formation or a re-rolled one. It is held in a
 * thread local for that call so the unit tables can consult it without every draw being handed it. With none
 * active, the tables draw exactly as they always have.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class UnitVariety {
    private static final MMLogger LOGGER = MMLogger.create(UnitVariety.class);

    /** Weight kept per copy of the exact model already in the same formation. */
    static final double SAME_MODEL_IN_FORMATION = 0.1;
    /** Weight kept per other variant of the same chassis already in the same formation. */
    static final double SAME_CHASSIS_IN_FORMATION = 0.25;
    /** Weight kept per copy of the chassis elsewhere in the command. */
    static final double SAME_CHASSIS_ELSEWHERE = 0.6;
    /** The least weight the command-wide penalty leaves, however often the chassis already appears. */
    static final double ELSEWHERE_FLOOR = 0.15;

    /** How many of the most repeated chassis the per-roll summary names. */
    private static final int SUMMARY_REPEATS_NAMED = 5;

    private static final ThreadLocal<UnitVariety> ACTIVE = new ThreadLocal<>();

    /**
     * One unit placed in the force.
     *
     * @param chassis   the unit's chassis
     * @param model     the unit's model name
     * @param formation the formation it sits in, or {@code null} for a unit from an earlier roll
     */
    record Pick(String chassis, String model, @Nullable ForceDescriptor formation) {}

    /** Keyed by the slot, so a slot given a unit twice - as generateLance and then its own pass do - counts once. */
    private final Map<ForceDescriptor, Pick> picksBySlot = new IdentityHashMap<>();
    private final List<Pick> earlierPicks = new ArrayList<>();
    private @Nullable ForceDescriptor currentFormation;

    private UnitVariety() {
    }

    /** @return the variety in force for the generation running on this thread, or {@code null} for none */
    static @Nullable UnitVariety active() {
        return ACTIVE.get();
    }

    /**
     * Prepares variety for a generation starting at {@code node}, when the force it belongs to asked for it.
     *
     * <p>The units already in the force - the rest of the command when one formation is added or re-rolled - and
     * the units of earlier rolls are counted from the start.</p>
     *
     * @param node the node generation starts at
     *
     * @return the variety to use, or {@code null} when the force did not ask for any
     */
    static @Nullable UnitVariety forGeneration(ForceDescriptor node) {
        boolean wanted = false;
        ForceDescriptor top = node;
        for (ForceDescriptor current = node; current != null; current = current.getParent()) {
            wanted |= current.isUnitVarietyBias();
            top = current;
        }
        if (!wanted) {
            return null;
        }
        UnitVariety variety = new UnitVariety();
        forEachElement(top, element -> {
            ModelRecord unit = RATGenerator.getInstance().getModelRecord(element.getModelName());
            if (unit != null) {
                variety.record(element, unit);
            }
        });
        for (ForceDescriptor current = node; current != null; current = current.getParent()) {
            variety.addEarlierUnits(current.getEarlierUnits());
        }
        return variety;
    }

    /**
     * Runs a generation with this variety in force, and logs what came of it.
     *
     * @param generated  the node being generated, for the summary
     * @param generation the generation to run
     */
    void runFor(ForceDescriptor generated, Runnable generation) {
        ACTIVE.set(this);
        try {
            generation.run();
        } finally {
            ACTIVE.remove();
        }
        logSummary(generated, true);
    }

    /**
     * Counts units from earlier rolls of the same command toward the command-wide penalty.
     *
     * @param modelNames the model names of those units
     */
    void addEarlierUnits(Collection<String> modelNames) {
        for (String modelName : modelNames) {
            ModelRecord unit = RATGenerator.getInstance().getModelRecord(modelName);
            if (unit != null) {
                earlierPicks.add(new Pick(unit.getChassis(), unit.getKey(), null));
            }
        }
    }

    /**
     * Records the unit given to a slot.
     *
     * @param slot the slot, a leaf of the force tree
     * @param unit the unit it was given
     */
    void record(ForceDescriptor slot, ModelRecord unit) {
        picksBySlot.put(slot, new Pick(unit.getChassis(), unit.getKey(), slot.getParent()));
    }

    /**
     * Runs a step of generation with draws counted against one formation.
     *
     * @param formation the formation whose units the draws are compared with, or {@code null} for none
     * @param step      the step to run
     *
     * @return what the step returned
     */
    <T> T inFormation(@Nullable ForceDescriptor formation, Supplier<T> step) {
        ForceDescriptor previous = currentFormation;
        currentFormation = formation;
        try {
            return step.get();
        } finally {
            currentFormation = previous;
        }
    }

    /**
     * Counts what the force holds for one draw.
     *
     * @param drawnThisCall units already drawn earlier in the same multi-unit draw, which will join the current
     *                      formation together
     *
     * @return the counts the draw scales each candidate by
     */
    DrawWeights weightsForDraw(List<MekSummary> drawnThisCall) {
        Map<String, Integer> formationModels = new HashMap<>();
        Map<String, Integer> formationChassis = new HashMap<>();
        Map<String, Integer> elsewhereChassis = new HashMap<>();
        for (Pick pick : picksBySlot.values()) {
            if ((currentFormation != null) && (pick.formation() == currentFormation)) {
                formationModels.merge(pick.model(), 1, Integer::sum);
                formationChassis.merge(pick.chassis(), 1, Integer::sum);
            } else {
                elsewhereChassis.merge(pick.chassis(), 1, Integer::sum);
            }
        }
        for (Pick pick : earlierPicks) {
            elsewhereChassis.merge(pick.chassis(), 1, Integer::sum);
        }
        for (MekSummary drawn : drawnThisCall) {
            formationModels.merge(drawn.getName(), 1, Integer::sum);
            formationChassis.merge(drawn.getChassis(), 1, Integer::sum);
        }
        return new DrawWeights(formationModels, formationChassis, elsewhereChassis);
    }

    /**
     * What one draw scales its candidates by.
     *
     * @param formationModels  copies of each model already in the formation
     * @param formationChassis copies of each chassis already in the formation, every variant counted
     * @param elsewhereChassis copies of each chassis elsewhere in the command
     */
    record DrawWeights(Map<String, Integer> formationModels, Map<String, Integer> formationChassis,
          Map<String, Integer> elsewhereChassis) {

        /**
         * @param candidate a unit the table could draw
         *
         * @return the share of its table weight the candidate keeps, above zero and at most one
         */
        double factor(MekSummary candidate) {
            int sameModel = formationModels.getOrDefault(candidate.getName(), 0);
            int sameChassis = formationChassis.getOrDefault(candidate.getChassis(), 0);
            int elsewhere = elsewhereChassis.getOrDefault(candidate.getChassis(), 0);
            return weightFactor(sameModel, sameChassis - sameModel, elsewhere);
        }
    }

    /**
     * The share of its table weight a candidate keeps.
     *
     * @param sameModelInFormation     copies of the exact model already in the formation
     * @param otherVariantsInFormation other variants of its chassis already in the formation
     * @param sameChassisElsewhere     copies of its chassis elsewhere in the command
     *
     * @return a factor above zero and at most one
     */
    static double weightFactor(int sameModelInFormation, int otherVariantsInFormation, int sameChassisElsewhere) {
        double inFormation = Math.pow(SAME_MODEL_IN_FORMATION, Math.max(0, sameModelInFormation))
              * Math.pow(SAME_CHASSIS_IN_FORMATION, Math.max(0, otherVariantsInFormation));
        double elsewhere = Math.max(Math.pow(SAME_CHASSIS_ELSEWHERE, Math.max(0, sameChassisElsewhere)),
              ELSEWHERE_FLOOR);
        return inFormation * elsewhere;
    }

    /**
     * Visits every slot under a node that holds a unit, attached forces included.
     *
     * @param node    the node to start from
     * @param visitor told each slot
     */
    static void forEachElement(ForceDescriptor node, Consumer<ForceDescriptor> visitor) {
        if (node.isElement() && !node.getModels().isEmpty()) {
            visitor.accept(node);
        }
        for (ForceDescriptor sub : node.getSubForces()) {
            forEachElement(sub, visitor);
        }
        for (ForceDescriptor attachedForce : node.getAttached()) {
            forEachElement(attachedForce, visitor);
        }
    }

    /**
     * Logs, at DEBUG, how varied a generated force came out: its units, distinct chassis and models, how many units
     * repeat a chassis already in their own formation, and the most repeated chassis. One line per generation, so a
     * player's report of repeat units can be checked against the log.
     *
     * @param generated the node that was generated
     * @param varietyOn whether variety was in force while it was
     */
    static void logSummary(ForceDescriptor generated, boolean varietyOn) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }
        Map<String, Integer> chassisCounts = new HashMap<>();
        Map<String, Integer> modelCounts = new HashMap<>();
        Map<ForceDescriptor, Map<String, Integer>> chassisByFormation = new IdentityHashMap<>();
        int[] units = { 0 };
        forEachElement(generated, element -> {
            ModelRecord unit = RATGenerator.getInstance().getModelRecord(element.getModelName());
            String chassis = (unit == null) ? element.getModelName() : unit.getChassis();
            units[0]++;
            chassisCounts.merge(chassis, 1, Integer::sum);
            modelCounts.merge(element.getModelName(), 1, Integer::sum);
            chassisByFormation.computeIfAbsent(element.getParent(), key -> new HashMap<>())
                  .merge(chassis, 1, Integer::sum);
        });
        if (units[0] == 0) {
            return;
        }
        int repeatsInFormation = 0;
        for (Map<String, Integer> formation : chassisByFormation.values()) {
            for (int count : formation.values()) {
                repeatsInFormation += count - 1;
            }
        }
        List<Map.Entry<String, Integer>> repeated = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : chassisCounts.entrySet()) {
            if (entry.getValue() > 1) {
                repeated.add(entry);
            }
        }
        repeated.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        List<String> named = new ArrayList<>();
        for (int index = 0; (index < repeated.size()) && (index < SUMMARY_REPEATS_NAMED); index++) {
            named.add(repeated.get(index).getKey() + " x" + repeated.get(index).getValue());
        }
        LOGGER.debug("[ForceGen][Variety] '{}': {} unit(s), {} distinct chassis, {} distinct model(s); {} repeat a"
                    + " chassis already in their own formation; most repeated: {} (variety bias {})",
              generated.parseName(), units[0], chassisCounts.size(), modelCounts.size(), repeatsInFormation,
              named.isEmpty() ? "none" : String.join(", ", named), varietyOn ? "on" : "off");
    }
}
