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
package megamek.server.totalWarfare;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.compute.MarinePointsScoreCalculator;
import megamek.common.compute.MarinePointsTrait;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Tells the story of an infantry vs. infantry action's roll in a few sentences under the numbers: how it went, what
 * each side brought to it, and what each side paid. The numbers are the shares of each side's own strength that the
 * resolution already worked out, so the story never disagrees with the lines above it.
 *
 * <p>The text is three sentences at most: a lead for the outcome, then one sentence per side built from that side's
 * strongest {@link MarinePointsTrait} and its loss. Each outcome has several leads and one is chosen at random, so a
 * siege that runs for turns does not read the same every time.</p>
 */
class InfantryActionNarrator extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(InfantryActionNarrator.class);

    /** How the roll went, in the order the resolution decides it. */
    enum Outcome {
        /** Nothing of the attacking force survived. */
        ATTACKERS_ELIMINATED(5667),
        /** The garrison is gone and the building taken. */
        DEFENDERS_ELIMINATED(5670),
        /** An R result: the attackers are thrown out. */
        REPULSED(5673),
        /** The attackers chose to leave this turn. */
        WITHDRAWAL(5682),
        /** A P result: the attackers hold most of the building. */
        PENETRATION(5679),
        /** Both sides still stand where they stood. */
        ENGAGED(5676);

        private final int firstLead;

        Outcome(int firstLead) {
            this.firstLead = firstLead;
        }
    }

    /**
     * One side's part in the story.
     *
     * @param marinePointsLost the Marine Points the side lost this roll
     * @param ownStrength      the side's own strength the loss is measured against
     * @param eliminated       {@code true} when nothing of the side remains
     * @param traits           what the side brought, the union over its units
     */
    record Side(int marinePointsLost, int ownStrength, boolean eliminated, Set<MarinePointsTrait> traits) {}

    /** Every outcome has this many leads to choose from. */
    static final int LEADS_PER_OUTCOME = 3;
    /** The building fell because nothing was committed to its defence. */
    static final int NO_DEFENCE = 5685;
    /** The clause for a side with no trait worth a mention. */
    static final int PLAIN_TROOPERS = 5701;
    /** The attackers lost a share of their strength. */
    static final int ATTACKERS_LOST_SHARE = 5702;
    /** The attackers were wiped out. */
    static final int ATTACKERS_LOST_ALL = 5703;
    /** The attackers lost nothing. */
    static final int ATTACKERS_LOST_NOBODY = 5704;
    /** The defenders lost a share of their strength. */
    static final int DEFENDERS_LOST_SHARE = 5705;
    /** The defenders were wiped out. */
    static final int DEFENDERS_LOST_ALL = 5706;
    /** The defenders lost nothing. */
    static final int DEFENDERS_LOST_NOBODY = 5707;

    private static final int NO_LINE_BREAK = 0;
    private static final int WHOLE_PERCENT = 100;

    private final IntUnaryOperator leadPicker;

    InfantryActionNarrator(TWGameManager gameManager) {
        this(gameManager, Compute::randomInt);
    }

    /**
     * @param leadPicker chooses a lead by index, given how many there are; tests pass a fixed choice
     */
    InfantryActionNarrator(TWGameManager gameManager, IntUnaryOperator leadPicker) {
        super(gameManager);
        this.leadPicker = leadPicker;
    }

    /**
     * The traits of a side: everything its units still in the fight brought to it.
     *
     * @param entityIds the side's units
     * @param building  the building whose modifier applies to this side, or {@code null} for none
     *
     * @return the union of the units' traits
     */
    Set<MarinePointsTrait> traitsOf(List<Integer> entityIds, @Nullable AbstractBuildingEntity building) {
        EnumSet<MarinePointsTrait> traits = EnumSet.noneOf(MarinePointsTrait.class);
        for (int entityId : entityIds) {
            Entity entity = getGame().getEntity(entityId);
            if ((entity != null) && !InfantryActionReporter.isOutOfTheFight(entity)) {
                traits.addAll(MarinePointsScoreCalculator.breakdown(entity, building).traits());
            }
        }
        return traits;
    }

    /**
     * Writes the story of one roll: the lead for the outcome, then a sentence for each side.
     *
     * @param outcome   how the roll went
     * @param attackers the attackers' part
     * @param defenders the defenders' part
     */
    void narrate(Outcome outcome, Side attackers, Side defenders) {
        int lead = outcome.firstLead + Math.floorMod(leadPicker.applyAsInt(LEADS_PER_OUTCOME), LEADS_PER_OUTCOME);
        LOGGER.debug("[InfantryAction] narrative: {} lead {}, attackers {}, defenders {}", outcome, lead,
              attackers.traits(), defenders.traits());
        Report leadReport = new Report(lead);
        leadReport.indent(InfantryActionReporter.SIDE_LINE_INDENT);
        leadReport.newlines = NO_LINE_BREAK;
        addReport(leadReport);
        narrateSide(attackers, ATTACKERS_LOST_SHARE, ATTACKERS_LOST_ALL, ATTACKERS_LOST_NOBODY, NO_LINE_BREAK);
        narrateSide(defenders, DEFENDERS_LOST_SHARE, DEFENDERS_LOST_ALL, DEFENDERS_LOST_NOBODY, 1);
    }

    /** The one line for a building that fell because nothing was committed to its defence. */
    void narrateNoDefence() {
        Report report = new Report(NO_DEFENCE);
        report.indent(InfantryActionReporter.SIDE_LINE_INDENT);
        addReport(report);
    }

    private void narrateSide(Side side, int lostShareId, int lostAllId, int lostNobodyId, int newlinesAfter) {
        Report clause = new Report(clauseFor(strongestTrait(side.traits())));
        clause.newlines = NO_LINE_BREAK;
        addReport(clause);

        Report loss;
        if (side.eliminated()) {
            loss = new Report(lostAllId);
        } else if (side.marinePointsLost() <= 0) {
            loss = new Report(lostNobodyId);
        } else {
            loss = new Report(lostShareId);
            loss.add(percentOfOwnStrength(side));
        }
        loss.newlines = newlinesAfter;
        addReport(loss);
    }

    /** The share of a side's own strength it lost, as a whole percentage of at least one. */
    static int percentOfOwnStrength(Side side) {
        if (side.ownStrength() <= 0) {
            return WHOLE_PERCENT;
        }
        long percent = Math.round((double) side.marinePointsLost() * WHOLE_PERCENT / side.ownStrength());
        return (int) Math.max(1, Math.min(WHOLE_PERCENT, percent));
    }

    /** The trait that most shaped the fighting, by the enum's order; {@code null} for a side with none. */
    static @Nullable MarinePointsTrait strongestTrait(Set<MarinePointsTrait> traits) {
        for (MarinePointsTrait trait : MarinePointsTrait.values()) {
            if (traits.contains(trait)) {
                return trait;
            }
        }
        return null;
    }

    /** The report message for a side's strongest trait. */
    static int clauseFor(@Nullable MarinePointsTrait trait) {
        if (trait == null) {
            return PLAIN_TROOPERS;
        }
        return switch (trait) {
            case ELEMENTALS -> 5686;
            case INNER_SPHERE_BATTLE_ARMOR -> 5687;
            case MARINES -> 5688;
            case LINE_INFANTRY -> 5689;
            case ARMORED_INFANTRY -> 5690;
            case BUILDING_CREW -> 5691;
            case CIVILIANS -> 5692;
            case HEAVY_SUITS -> 5693;
            case LIGHT_SUITS -> 5694;
            case BURST_FIRE -> 5695;
            case FLAME -> 5696;
            case CLAWS -> 5697;
            case TORCHES_OR_DRILLS -> 5698;
            case ANTI_PERSONNEL_MOUNTS -> 5699;
            case MAGNETIC_CLAMPS -> 5700;
        };
    }
}
