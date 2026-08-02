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

package megamek.client.bot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import megamek.common.force.Force;
import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * Works out which of a bot's trailers should be hitched behind which of its tractors.
 * <p>
 * A bot never uses the lobby's "Connect as Train" action, so a trailer handed to one just sits there. A trailer has
 * no engine, so an unhitched trailer is a unit the bot can never move and never fire from cover. This decides the
 * pairing so the bot can ask the server to build the trains, using the same request a human client sends.
 * </p>
 * <p>
 * Trailers go to a tractor in their own force first, which is exact for a force built as a battery. Whatever is left
 * over is offered to each tractor in turn while towing capacity allows, so a hand-built list still ends up usable.
 * </p>
 */
public final class BotTrainPlanner {

    private BotTrainPlanner() {
    }

    /**
     * Plans the trains a player should build out of the units it owns that are not already hitched together.
     * <p>
     * The result only ever names units that are loose, so replanning after the trains are built produces nothing and
     * a train the player built deliberately is never disturbed.
     * </p>
     *
     * @param game     the game to read units from
     * @param playerId the player whose units should be paired up
     *
     * @return tractor id to the trailers to hitch behind it, front to back; tractors with nothing to tow are left out
     */
    public static Map<Integer, List<Integer>> planTrains(Game game, int playerId) {
        List<Entity> tractors = new ArrayList<>();
        List<Entity> looseTrailers = new ArrayList<>();

        for (Entity entity : game.getEntitiesVector()) {
            if (entity.getOwnerId() != playerId) {
                continue;
            }

            // Test for a trailer first. A carriage carries a hitch so it can pass another trailer down the train,
            // which also makes isTractor true for it, but it has no engine and can never head a train.
            if (entity.isTrailer()) {
                if ((entity.getTractor() == Entity.NONE) && (entity.getTowedBy() == Entity.NONE)) {
                    looseTrailers.add(entity);
                }
            } else if (entity.isTractor()
                  && (entity.getTractor() == Entity.NONE)
                  && (entity.getTowing() == Entity.NONE)) {
                // A tractor that already heads a train is left alone: a build request covers a whole train at once
                // and the server refuses one whose tractor is already towing.
                tractors.add(entity);
            }
        }

        Map<Integer, List<Integer>> plannedTrains = new LinkedHashMap<>();

        if (tractors.isEmpty() || looseTrailers.isEmpty()) {
            return plannedTrains;
        }

        // Narrowest match first, so the strongest evidence of what belongs together wins. A trailer claimed by an
        // earlier pass is gone, so a later, looser pass can never steal it.
        List<BiPredicate<Entity, Entity>> passes = List.of(
              (tractor, trailer) -> inSameForce(tractor, trailer) && isSameDesign(tractor, trailer),
              BotTrainPlanner::inSameForce,
              BotTrainPlanner::isSameDesign,
              (tractor, trailer) -> true);

        for (BiPredicate<Entity, Entity> pass : passes) {
            for (Entity tractor : tractors) {
                claimTrailers(tractor, looseTrailers, plannedTrains, pass);
            }
        }

        return plannedTrains;
    }

    /** Both units are in the same force, which is the player saying outright that they belong together. */
    private static boolean inSameForce(Entity tractor, Entity trailer) {
        return (tractor.getForceId() != Force.NO_FORCE) && (tractor.getForceId() == trailer.getForceId());
    }

    /**
     * The trailer is a carriage built for this exact tractor.
     * <p>
     * Carriages share their tractor's chassis and extend its model: a Mobile Long Tom Artillery LT-MOB-25 is
     * accompanied by "LT-MOB-25 (Ammunition Carriage)". Chassis alone is not enough, because every variant of the
     * design shares it, so an LT-MOB-25 and an LT-MOB-95 in the same lobby would trade carriages.
     * </p>
     * <p>
     * The model has to match on a boundary rather than as a bare prefix, or an LT-MOB-25 would claim
     * "LT-MOB-25F (Ammunition Carriage)" as its own.
     * </p>
     */
    private static boolean isSameDesign(Entity tractor, Entity trailer) {
        String tractorChassis = (tractor.getChassis() == null) ? "" : tractor.getChassis().trim();
        String trailerChassis = (trailer.getChassis() == null) ? "" : trailer.getChassis().trim();

        if (tractorChassis.isEmpty() || !tractorChassis.equalsIgnoreCase(trailerChassis)) {
            return false;
        }

        String tractorModel = (tractor.getModel() == null) ? "" : tractor.getModel().trim();
        String trailerModel = (trailer.getModel() == null) ? "" : trailer.getModel().trim();

        if (tractorModel.isEmpty()
              || !trailerModel.regionMatches(true, 0, tractorModel, 0, tractorModel.length())) {
            return false;
        }
        if (trailerModel.length() == tractorModel.length()) {
            return true;
        }

        // Anything that continues the model name, such as the F in LT-MOB-25F, means a different variant.
        return !Character.isLetterOrDigit(trailerModel.charAt(tractorModel.length()));
    }

    /**
     * Adds trailers to one tractor's train while its towing capacity allows, removing each one taken from
     * {@code looseTrailers} so no other tractor is offered it.
     *
     * @param matches decides which trailers this pass will consider for this tractor
     */
    private static void claimTrailers(Entity tractor, List<Entity> looseTrailers,
          Map<Integer, List<Integer>> plannedTrains, BiPredicate<Entity, Entity> matches) {
        List<Integer> train = plannedTrains.computeIfAbsent(tractor.getId(), id -> new ArrayList<>());

        // "Tractors may pull one or more Trailers whose combined weight is less than or equal to the Tractor's own
        // weight" (TM, Tractors). Track the running total, since the request is all or nothing: one trailer over the
        // limit and the server rejects the whole train.
        double towedWeight = 0;
        for (int trailerId : train) {
            Entity alreadyPlanned = tractor.getGame().getEntity(trailerId);
            if (alreadyPlanned != null) {
                towedWeight += alreadyPlanned.getWeight();
            }
        }

        Iterator<Entity> candidates = looseTrailers.iterator();
        while (candidates.hasNext()) {
            Entity trailer = candidates.next();

            if (!matches.test(tractor, trailer)) {
                continue;
            }
            if ((towedWeight + trailer.getWeight()) > tractor.getWeight()) {
                continue;
            }

            towedWeight += trailer.getWeight();
            train.add(trailer.getId());
            candidates.remove();
        }

        if (train.isEmpty()) {
            plannedTrains.remove(tractor.getId());
        }
    }
}
