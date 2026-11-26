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

import jakarta.annotation.Nullable;
import megamek.common.Report;
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
 * with the side's loss. Each of those has several wordings and one is chosen at random, so a siege that runs for
 * turns does not read the same every time.</p>
 *
 * <p>The leads fit the size of the building: a {@link BuildingScale#SMALL} one, such as a gun emplacement, is a door
 * and a room, while a {@link BuildingScale#LARGE} one has corridors, stairwells and floors. The trait clauses fit
 * either. A side of one person gets its loss in the singular.</p>
 */
class InfantryActionNarrator extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(InfantryActionNarrator.class);

    /** How the roll went, in the order the resolution decides it. */
    enum Outcome {
        /** Nothing of the attacking force survived. */
        ATTACKERS_ELIMINATED(5730),
        /** The garrison is gone and the building taken. */
        DEFENDERS_ELIMINATED(5740),
        /** An R result: the attackers are thrown out. */
        REPULSED(5750),
        /** The attackers chose to leave this turn. */
        WITHDRAWAL(5760),
        /** A P result: the attackers hold most of the building. */
        PENETRATION(5770),
        /** Both sides still stand where they stood. */
        ENGAGED(5780),
        /** House rule: the defending infantry chose to leave, giving up the building. */
        DEFENDERS_WITHDRAW(5790);

        private final int firstLargeBuildingLead;

        Outcome(int firstLargeBuildingLead) {
            this.firstLargeBuildingLead = firstLargeBuildingLead;
        }

        /**
         * @param scale the size of the building fought over
         *
         * @return the first report id of this outcome's leads for a building that size
         */
        int firstLead(BuildingScale scale) {
            return firstLargeBuildingLead + scale.leadOffset;
        }
    }

    /** The size of the building fought over, which decides the set of leads the story draws from. */
    enum BuildingScale {
        /** Several hexes or more than {@link #SMALL_MAX_LEVELS} levels: corridors, stairwells and floors. */
        LARGE(0),
        /** One hex of no more than {@link #SMALL_MAX_LEVELS} levels, such as a gun emplacement: a door and a room. */
        SMALL(70);

        /** A one-hex building is small up to this many levels. */
        static final int SMALL_MAX_LEVELS = 2;

        private final int leadOffset;

        BuildingScale(int leadOffset) {
            this.leadOffset = leadOffset;
        }

        /**
         * @param building the building fought over
         *
         * @return {@link #SMALL} for a one-hex building of one or two levels, otherwise {@link #LARGE}
         */
        static BuildingScale of(AbstractBuildingEntity building) {
            int hexCount = building.getInternalBuilding().getCoordsList().size();
            int levels = building.getInternalBuilding().getBuildingHeight();
            return ((hexCount <= 1) && (levels <= SMALL_MAX_LEVELS)) ? SMALL : LARGE;
        }
    }

    /**
     * One side's part in the story.
     *
     * @param units            the units that counted for the side, in order, named in the story
     * @param traits           what the side brought, the union over its units
     * @param headCount        the people in those units before this roll's losses
     * @param marinePointsLost the Marine Points the side lost this roll
     * @param ownStrength      the side's own strength the loss is measured against
     * @param eliminated       {@code true} when nothing of the side remains
     */
    record Side(List<Entity> units, Set<MarinePointsTrait> traits, int headCount, int marinePointsLost,
          int ownStrength, boolean eliminated) {

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
            int headCount = 0;
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
                headCount += breakdown.headCount();
            }
            return new Side(List.copyOf(units), traits, headCount, lost, own, eliminated);
        }

        /**
         * @param eliminated whether the loss leaves nobody, once the losses are known
         *
         * @return this side with that answer
         */
        Side withEliminated(boolean eliminated) {
            return new Side(units, traits, headCount, marinePointsLost, ownStrength, eliminated);
        }

        /** @return {@code true} for a side of one person, whose loss is told in the singular */
        boolean isLoneFighter() {
            return headCount == 1;
        }
    }

    /** Every outcome has this many leads to choose from, for each {@link BuildingScale}. */
    static final int LEADS_PER_OUTCOME = 5;
    /** Every trait, and a side with none, has this many clauses to choose from. */
    static final int CLAUSES_PER_TRAIT = 3;
    /** Every loss fragment has this many wordings to choose from. */
    static final int WORDINGS_PER_LOSS = 3;
    /** The building fell because nothing was committed to its defence. */
    static final int NO_DEFENCE = 5685;
    /** The first clause for a side with no trait worth a mention. */
    static final int PLAIN_TROOPERS = 5945;
    /** The side lost a share of its strength, given as a short tag after the clause. First wording. */
    static final int LOST_SHARE = 5950;
    /** None of the side survived. First wording. */
    static final int NONE_SURVIVED = 5955;
    /** The side lost nothing. First wording. */
    static final int LOST_NOBODY = 5960;
    /** Added to a loss fragment for a side of one person, whose wordings are in the singular. */
    static final int LONE_FIGHTER_OFFSET = 15;
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
    /** A building's crew when they open the sentence, so with a capital. */
    static final int BUILDING_CREW_NAME_FIRST = 5724;
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

    private final IntUnaryOperator variantPicker;

    InfantryActionNarrator(TWGameManager gameManager) {
        this(gameManager, Compute::randomInt);
    }

    /**
     * @param variantPicker chooses a wording by index, given how many there are; tests pass a fixed choice
     */
    InfantryActionNarrator(TWGameManager gameManager, IntUnaryOperator variantPicker) {
        super(gameManager);
        this.variantPicker = variantPicker;
    }

    /** One of the wordings that start at a report id, by the picker's choice wrapped into their number. */
    private int pick(int firstWording, int wordings) {
        return firstWording + Math.floorMod(variantPicker.applyAsInt(wordings), wordings);
    }

    /**
     * Writes the story of one roll as one paragraph: the lead for the outcome, then a sentence for each side.
     *
     * @param outcome   how the roll went
     * @param attackers the attackers' part
     * @param defenders the defenders' part
     * @param building  the building fought over
     * @param captured  {@code true} when the building falls to the attackers on this roll
     */
    void narrate(Outcome outcome, Side attackers, Side defenders, AbstractBuildingEntity building,
          boolean captured) {
        BuildingScale scale = BuildingScale.of(building);
        int lead = pick(outcome.firstLead(scale), LEADS_PER_OUTCOME);
        LOGGER.info("[InfantryAction] narrative: {} in a {} building, lead {}, attackers {} ({} people) {}, "
                    + "defenders {} ({} people) {}", outcome, scale, lead, attackers.units().size(),
              attackers.headCount(), attackers.traits(), defenders.units().size(), defenders.headCount(),
              defenders.traits());
        AbstractBuildingEntity capturedBuilding = captured ? building : null;
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
    private List<Report> sideSentence(Side side, int fallbackSubject,
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
            sentence.add(nameOf(side.units().get(index), everyone, index == 0));
        }
        sentence.add(new Report(pick(clauseFor(strongestTrait(side.traits())), CLAUSES_PER_TRAIT)));
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
    private static Report nameOf(Entity entity, List<Entity> everyone, boolean opensSentence) {
        int crewName = opensSentence ? BUILDING_CREW_NAME_FIRST : BUILDING_CREW_NAME;
        Report name = new Report((entity instanceof AbstractBuildingEntity) ? crewName : UNIT_NAME);
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

    /** The side's loss in one of its wordings, in the singular for a side of one person. */
    private Report lossFragment(Side side) {
        int loneFighterOffset = side.isLoneFighter() ? LONE_FIGHTER_OFFSET : 0;
        if (side.eliminated()) {
            return new Report(pick(NONE_SURVIVED + loneFighterOffset, WORDINGS_PER_LOSS));
        }
        if (side.marinePointsLost() <= 0) {
            return new Report(pick(LOST_NOBODY + loneFighterOffset, WORDINGS_PER_LOSS));
        }
        Report loss = new Report(pick(LOST_SHARE + loneFighterOffset, WORDINGS_PER_LOSS));
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
        return Math.clamp(percent, 1, WHOLE_PERCENT);
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

    /**
     * The first of the {@link #CLAUSES_PER_TRAIT} report messages for a side's strongest trait: a clause that
     * follows the side's names.
     */
    static int clauseFor(@Nullable MarinePointsTrait trait) {
        if (trait == null) {
            return PLAIN_TROOPERS;
        }
        return switch (trait) {
            case ELEMENTALS -> 5870;
            case INNER_SPHERE_BATTLE_ARMOR -> 5875;
            case MARINES -> 5880;
            case LINE_INFANTRY -> 5885;
            case ARMORED_INFANTRY -> 5890;
            case BUILDING_CREW -> 5895;
            case CIVILIANS -> 5900;
            case HEAVY_SUITS -> 5905;
            case LIGHT_SUITS -> 5910;
            case BURST_FIRE -> 5915;
            case FLAME -> 5920;
            case CLAWS -> 5925;
            case TORCHES_OR_DRILLS -> 5930;
            case ANTI_PERSONNEL_MOUNTS -> 5935;
            case MAGNETIC_CLAMPS -> 5940;
        };
    }
}
