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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.compute.MarinePointsBreakdown;
import megamek.common.compute.MarinePointsScoreCalculator;
import megamek.common.compute.MarinePointsTrait;
import megamek.common.game.Game;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Tells the story of an infantry vs. infantry action's roll in a few sentences under the action's header: how it
 * went, who fought on each side, what they did and what they paid. The numbers are the shares of each side's own
 * strength that the resolution worked out, so the story never disagrees with the working below it.
 *
 * <p>The text is three sentences: a lead for the outcome, then one sentence per side that names the side's units
 * with their report links, follows them with a clause for the side's strongest {@link MarinePointsTrait}, and ends
 * with the side's loss. Each outcome has several leads and one is chosen at random, so a siege that runs for turns
 * does not read the same every time.</p>
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
        ENGAGED(5676),
        /** House rule: the defending infantry chose to leave, giving up the building. */
        DEFENDERS_WITHDRAW(5718);

        private final int firstLead;

        Outcome(int firstLead) {
            this.firstLead = firstLead;
        }
    }

    /**
     * One side's part in the story.
     *
     * @param units            the units that counted for the side, in order, named in the story
     * @param traits           what the side brought, the union over its units
     * @param marinePointsLost the Marine Points the side lost this roll
     * @param ownStrength      the side's own strength the loss is measured against
     * @param eliminated       {@code true} when nothing of the side remains
     */
    record Side(List<Entity> units, Set<MarinePointsTrait> traits, int marinePointsLost, int ownStrength,
          boolean eliminated) {

        /**
         * A side read from the game before its losses are applied: the units still in the fight that score
         * anything, and everything they brought.
         *
         * @param game       the game
         * @param entityIds  the side's units
         * @param building   the building whose modifier applies to this side, or {@code null} for none
         * @param lost       the Marine Points the side loses this roll
         * @param own        the side's own strength
         * @param eliminated whether the loss leaves nobody
         *
         * @return the side
         */
        static Side of(Game game, List<Integer> entityIds, @Nullable AbstractBuildingEntity building, int lost,
              int own, boolean eliminated) {
            List<Entity> units = new ArrayList<>();
            EnumSet<MarinePointsTrait> traits = EnumSet.noneOf(MarinePointsTrait.class);
            for (int entityId : entityIds) {
                Entity entity = game.getEntity(entityId);
                if ((entity == null) || InfantryActionReporter.isOutOfTheFight(entity)) {
                    continue;
                }
                MarinePointsBreakdown breakdown = MarinePointsScoreCalculator.breakdown(entity, building);
                if (breakdown.modifiedScore() <= 0) {
                    continue;
                }
                units.add(entity);
                traits.addAll(breakdown.traits());
            }
            return new Side(List.copyOf(units), traits, lost, own, eliminated);
        }

        /**
         * @param eliminated whether the loss leaves nobody, once the losses are known
         *
         * @return this side with that answer
         */
        Side withEliminated(boolean eliminated) {
            return new Side(units, traits, marinePointsLost, ownStrength, eliminated);
        }
    }

    /** Every outcome has this many leads to choose from. */
    static final int LEADS_PER_OUTCOME = 3;
    /** The building fell because nothing was committed to its defence. */
    static final int NO_DEFENCE = 5685;
    /** The clause for a side with no trait worth a mention. */
    static final int PLAIN_TROOPERS = 5701;
    /** The side lost a share of its strength; a platoon or a squad is a body of troops, so always "their". */
    static final int LOST_SHARE = 5702;
    /** None of the side survived. */
    static final int NONE_SURVIVED = 5703;
    /** The side lost nothing. */
    static final int LOST_NOBODY = 5704;
    /** A unit's linked name inside the sentence. */
    static final int UNIT_NAME = 5707;
    /** Joins the captured building on to the defenders' sentence. */
    static final int AND_THE_BUILDING = 5708;
    /** A comma between names. */
    static final int COMMA = 5709;
    /** "and" before the last name. */
    static final int AND = 5710;
    /** The building falls and its uncommitted crew surrender. */
    static final int BUILDING_FALLS = 5711;
    /** A building's crew, named as the unit that fought. */
    static final int BUILDING_CREW_NAME = 5715;
    /** The building falls with no crew left to surrender. */
    static final int BUILDING_FALLS_NOBODY_LEFT = 5716;
    /** Ends a side's sentence. */
    static final int FULL_STOP = 5712;
    /** The attackers, when none of them can be named. */
    static final int THE_ATTACKERS = 5713;
    /** The defenders, when none of them can be named. */
    static final int THE_DEFENDERS = 5714;

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
     * Writes the story of one roll as one paragraph: the lead for the outcome, then a sentence for each side.
     *
     * @param outcome          how the roll went
     * @param attackers        the attackers' part
     * @param defenders        the defenders' part
     * @param capturedBuilding the building that falls to the attackers on this roll, or {@code null} for none
     */
    void narrate(Outcome outcome, Side attackers, Side defenders, @Nullable AbstractBuildingEntity capturedBuilding) {
        int lead = outcome.firstLead + Math.floorMod(leadPicker.applyAsInt(LEADS_PER_OUTCOME), LEADS_PER_OUTCOME);
        LOGGER.debug("[InfantryAction] narrative: {} lead {}, attackers {} {}, defenders {} {}", outcome, lead,
              attackers.units().size(), attackers.traits(), defenders.units().size(), defenders.traits());
        List<Entity> everyone = new ArrayList<>(attackers.units());
        everyone.addAll(defenders.units());
        if (capturedBuilding != null) {
            everyone.add(capturedBuilding);
        }
        List<Report> story = new ArrayList<>();
        Report leadReport = new Report(lead);
        leadReport.indent(InfantryActionReporter.SIDE_LINE_INDENT);
        story.add(leadReport);
        story.addAll(sideSentence(attackers, THE_ATTACKERS, null, everyone));
        story.addAll(sideSentence(defenders, THE_DEFENDERS, capturedBuilding, everyone));
        addParagraph(story);
    }

    /** The one line for a building that fell because nothing was committed to its defence. */
    void narrateNoDefence() {
        Report report = new Report(NO_DEFENCE);
        report.indent(InfantryActionReporter.SIDE_LINE_INDENT);
        addReport(report);
    }

    /** The names, the clause for the strongest trait, the loss, and the full stop or the building's fall. */
    private static List<Report> sideSentence(Side side, int fallbackSubject,
          @Nullable AbstractBuildingEntity capturedBuilding, List<Entity> everyone) {
        List<Report> sentence = new ArrayList<>();
        if (side.units().isEmpty()) {
            sentence.add(new Report(fallbackSubject));
        }
        int last = side.units().size() - 1;
        for (int index = 0; index <= last; index++) {
            if (index > 0) {
                sentence.add(new Report((index == last) ? AND : COMMA));
            }
            sentence.add(nameOf(side.units().get(index), everyone));
        }
        sentence.add(new Report(clauseFor(strongestTrait(side.traits()))));
        sentence.add(lossFragment(side));
        if (capturedBuilding != null) {
            sentence.add(new Report(AND_THE_BUILDING));
            sentence.add(plainNameOf(capturedBuilding, everyone));
            int uncommittedCrew = capturedBuilding.getCrew().getCurrentSize() - capturedBuilding.getCommittedCrew();
            sentence.add(new Report((uncommittedCrew > 0) ? BUILDING_FALLS : BUILDING_FALLS_NOBODY_LEFT));
        } else {
            sentence.add(new Report(FULL_STOP));
        }
        return sentence;
    }

    /** A unit's linked name; a building is named by its crew, since the crew are who fought. */
    private static Report nameOf(Entity entity, List<Entity> everyone) {
        Report name = new Report((entity instanceof AbstractBuildingEntity) ? BUILDING_CREW_NAME : UNIT_NAME);
        name.subject = entity.getId();
        name.addEntityName(entity, storyName(entity, everyone));
        return name;
    }

    private static Report plainNameOf(Entity entity, List<Entity> everyone) {
        Report name = new Report(UNIT_NAME);
        name.subject = entity.getId();
        name.addEntityName(entity, storyName(entity, everyone));
        return name;
    }

    /**
     * The name a unit goes by in prose: its chassis alone, unless another unit in the same story shares the
     * chassis, when the full short name keeps the two apart.
     *
     * @param entity  the unit
     * @param company every unit the story names
     *
     * @return the display name
     */
    static String storyName(Entity entity, List<Entity> company) {
        String chassis = entity.getChassis();
        if ((chassis == null) || chassis.isBlank()) {
            return entity.getShortName();
        }
        for (Entity other : company) {
            boolean sameChassisOtherUnit = (other.getId() != entity.getId()) && chassis.equals(other.getChassis());
            if (sameChassisOtherUnit) {
                return entity.getShortName();
            }
        }
        return chassis;
    }

    private static Report lossFragment(Side side) {
        if (side.eliminated()) {
            return new Report(NONE_SURVIVED);
        }
        if (side.marinePointsLost() <= 0) {
            return new Report(LOST_NOBODY);
        }
        Report loss = new Report(LOST_SHARE);
        loss.add(percentOfOwnStrength(side));
        return loss;
    }

    /** Runs the reports on as one line: no line break until the last of them. */
    private void addParagraph(List<Report> reports) {
        for (Report report : reports) {
            report.newlines = NO_LINE_BREAK;
        }
        reports.getLast().newlines = 1;
        for (Report report : reports) {
            addReport(report);
        }
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

    /** The report message for a side's strongest trait: a clause that follows the side's names. */
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
