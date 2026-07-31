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

import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.enums.AugmentedUnitType;
import megamek.common.enums.ManeiDominiAugmentationRank;
import megamek.common.enums.ManeiDominiImplants;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.universe.RankSystem;
import megamek.common.universe.Ranks;
import megamek.logging.MMLogger;

/**
 * Fits the crews of a generated Word of Blake Shadow Division with their cybernetics.
 *
 * <p>Every Manei Domini receives implants, and how many and how advanced depends on their rank
 * (<i>Jihad Hot Spots: 3072</i>, pp. 121, 123-124). The chart and the choosing live in
 * {@link ManeiDominiImplants}, shared with MekHQ, so a Shadow Division raised in either program is
 * augmented the same way; what is here is finding the crews and deciding what each one is.</p>
 */
public final class ManeiDominiCrewAugmentor {

    private static final MMLogger LOGGER = MMLogger.create(ManeiDominiCrewAugmentor.class);

    /** The RAT Generator faction key for the Word of Blake Shadow Divisions. */
    public static final String SHADOW_DIVISION_FACTION_KEY = "WOB.SD";

    /** Fallback when the warrior's rank table cannot be read, being the length of the usual table. */
    private static final int DEFAULT_HIGHEST_RANK_INDEX = 50;

    private ManeiDominiCrewAugmentor() {
    }

    /**
     * Fits every crew in the generated force, where the force is a Shadow Division and the game allows
     * Manei Domini.
     *
     * @param root        the generated force; {@code null} is ignored
     * @param gameOptions the options the force is being generated for, read for the Manei Domini rule
     */
    public static void augment(@Nullable ForceDescriptor root, @Nullable IGameOptions gameOptions) {
        if (root == null) {
            return;
        }
        String faction = root.getFaction();
        boolean isShadowDivision = SHADOW_DIVISION_FACTION_KEY.equalsIgnoreCase(faction);
        boolean gameAllowsImplants = (gameOptions != null)
              && gameOptions.booleanOption(OptionsConstants.RPG_MANEI_DOMINI);
        LOGGER.info("[ManeiDomini] ENTER: faction='{}' (Shadow Divisions key '{}'),"
                    + " Manei Domini rule={}", faction, SHADOW_DIVISION_FACTION_KEY, gameAllowsImplants);

        if (!isShadowDivision) {
            LOGGER.info("[ManeiDomini] SKIPPED - '{}' is not the Word of Blake Shadow Divisions."
                        + " Only a Shadow Division is Manei Domini; pick it in the sub-faction list"
                        + " beneath Word of Blake.", faction);
            return;
        }
        if (!gameAllowsImplants) {
            LOGGER.info("[ManeiDomini] SKIPPED - the Manei Domini rule is off in this game's options,"
                        + " so any implants fitted would be stripped when the unit reached the lobby.");
            return;
        }

        int augmented = augmentTree(root, 0);
        LOGGER.info("[ManeiDomini] DONE: augmented {} crew(s)", augmented);
    }

    /**
     * Fits the crews of this force and everything under it.
     *
     * @return how many crews were augmented
     */
    private static int augmentTree(ForceDescriptor formation, int depth) {
        int augmented = 0;
        Entity entity = formation.getEntity();
        if ((entity != null) && (entity.getCrew() != null)) {
            augmented += augmentCrew(formation, entity, formation.getFaction()) ? 1 : 0;
        }
        for (ForceDescriptor subFormation : formation.getSubForces()) {
            augmented += augmentTree(subFormation, depth + 1);
        }
        for (ForceDescriptor attached : formation.getAttached()) {
            augmented += augmentTree(attached, depth + 1);
        }
        return augmented;
    }

    /**
     * Fits one crew.
     *
     * @return {@code true} if implants were fitted
     */
    private static boolean augmentCrew(ForceDescriptor formation, Entity entity, String factionCode) {
        CrewDescriptor commander = formation.getCo();
        int rankIndex = (commander == null) ? 0 : commander.getRank();
        ManeiDominiAugmentationRank rank =
              ManeiDominiAugmentationRank.forRankIndex(rankIndex, highestRankIndexFor(formation));
        // The construction rules are written against unit types, and the difference matters: a
        // battle armour trooper may carry a neural interface where a foot trooper may not.
        AugmentedUnitType unitType = AugmentedUnitType.forEntity(entity);

        List<String> fitted = ManeiDominiImplants.fitTo(entity.getCrew().getOptions(), rank, unitType, factionCode);
        LOGGER.info("[ManeiDomini]   '{}' ({}): rank index {} -> {}, unit type {} -> {} implant(s) {}",
              entity.getCrew().getName(), entity.getShortName(), rankIndex, rank,
              unitType, fitted.size(), fitted);
        return true;
    }

    /**
     * The highest rank index the warrior's own rank table holds, which is what their standing is
     * measured against. Falls back to the length of the usual table when the ruleset named no rank
     * system or the table cannot be read.
     */
    private static int highestRankIndexFor(ForceDescriptor formation) {
        Integer rankSystemIndex = formation.getRankSystem();
        if (rankSystemIndex == null) {
            return DEFAULT_HIGHEST_RANK_INDEX;
        }
        return Ranks.getInstance()
                     .getByRatgenIndex(rankSystemIndex)
                     .map(RankSystem::ranks)
                     .map(ranks -> Math.max(1, ranks.size() - 1))
                     .orElse(DEFAULT_HIGHEST_RANK_INDEX);
    }
}
