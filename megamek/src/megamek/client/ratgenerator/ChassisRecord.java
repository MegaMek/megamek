/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ratgenerator;

import java.util.*;

import megamek.common.EntityMovementMode;
import megamek.logging.MMLogger;

/**
 * The ChassisRecord tracks all available variants and determines how much total
 * weight
 * is to be distributed among the various models.
 *
 * @author Neoancient
 */
public class ChassisRecord extends AbstractUnitRecord {
    private final static MMLogger logger = MMLogger.create(ChassisRecord.class);

    protected HashSet<ModelRecord> models;

    public ChassisRecord(String chassis) {
        super(chassis);
        models = new HashSet<>();
    }

    public void addModel(ModelRecord model) {
        models.add(model);
        if (introYear == 0 || model.getIntroYear() < getIntroYear()) {
            introYear = model.getIntroYear();
        }
    }

    public HashSet<ModelRecord> getModels() {
        return models;
    }

    public List<ModelRecord> getSortedModels() {
        List<ModelRecord> sortedModels = new ArrayList<>(models);
        sortedModels.sort(Comparator.comparing(ModelRecord::getModel));
        return sortedModels;

    }

    /**
     * Generate a list of models for this chassis based on certain criteria.
     * Early prototypes may be available one year before official introduction.
     * @param exactYear     game year
     * @param validWeightClasses restrict weight class to one or more classes
     * @param movementModes movement mode types, may be null or empty
     * @param networkMask   specific C3 network equipment
     * @return    set of models which pass the filter requirements, may be empty
     */
    public HashSet<ModelRecord> getFilteredModels(int exactYear,
                                                  Collection<Integer> validWeightClasses,
                                                  Collection<EntityMovementMode> movementModes,
                                                  int networkMask) {

        HashSet<ModelRecord> filteredModels = new HashSet<>();

        for (ModelRecord curModel : models) {
            // Introduction date should be at most 1 year away for pre-production prototypes
            if (curModel.introYear > exactYear + 1) {
                continue;
            }

            // Weight class check
            if (validWeightClasses != null && !validWeightClasses.isEmpty() && !validWeightClasses.contains(curModel.getWeightClass())) {
                continue;
            }

            // Movement mode check
            if (movementModes != null && !movementModes.isEmpty()) {
                if (!movementModes.contains(curModel.getMovementMode())) {
                    continue;
                }
            }

            // C3 network equipment check
            if ((networkMask & curModel.getNetworkMask()) != networkMask) {
                continue;
            }

            filteredModels.add(curModel);
        }

        return filteredModels;
    }

    /**
     * Total the weights of all models for this chassis, including modifiers for
     * +/- dynamic adjustment, intro year adjustment, interpolation, and role
     * modifications.
     * @param validModels models to add up
     * @param currentEra  year for current era
     * @param exactYear   current year in game
     * @param nextEra     start date of next era after the current one
     * @param fRec        faction data
     * @param roles       roles selected for generation, may be null or empty
     * @param roleStrictness positive number, higher applies heavier role adjustments
     * @param equipRating    equipment rating to generate for
     * @param numRatingLevels how many rating levels are present
     * @return           sum of calculated weights of all models of this chassis
     */
    public double totalModelWeight(HashSet<ModelRecord> validModels,
                                   int currentEra,
                                   int exactYear,
                                   int nextEra,
                                   FactionRecord fRec,
                                   Collection<MissionRole> roles,
                                   int roleStrictness,
                                   int equipRating,
                                   int numRatingLevels,
                                   HashMap<String,Double> weightData) {

        RATGenerator ratGen = RATGenerator.getInstance();
        AvailabilityRating avRating, nextAvRating;
        double retVal = 0;
        double adjRating;
        double nextRating;
        Number roleRating;

        // Clear any pre-existing weighting data - this should only cover
        // the current set of models
        weightData.clear();

        // For each model
        for (ModelRecord curModel : validModels) {

            if (curModel.factionIsExcluded(fRec)) {
                continue;
            }

            // Get the availability rating for the provided faction and year,
            // skip processing if not available
            avRating = ratGen.findModelAvailabilityRecord(currentEra, curModel.getKey(), fRec);
            if (avRating == null || avRating.getAvailability() <= 0) {
                continue;
            }

            // If required, interpolate availability between era start or intro date
            // (whichever is later), and start of next era
            if (exactYear > currentEra && currentEra != nextEra) {
                nextAvRating = ratGen.findModelAvailabilityRecord(nextEra,
                    curModel.getKey(), fRec);

                int interpolationStart = Math.max(currentEra, Math.min(exactYear, curModel.introYear));

                adjRating = curModel.calcAvailability(avRating,
                    equipRating, numRatingLevels, interpolationStart);

                nextRating = 0.0;
                if (nextAvRating != null) {
                    nextRating = curModel.calcAvailability(nextAvRating,
                        equipRating, numRatingLevels, nextEra);
                }

                if (adjRating != nextRating) {
                    adjRating = adjRating +
                        (nextRating - adjRating) * (exactYear - interpolationStart) / (nextEra - interpolationStart);
                }

            } else {
                // Adjust availability for +/- dynamic and intro year
                adjRating = curModel.calcAvailability(avRating, equipRating, numRatingLevels, exactYear);
            }

            if (adjRating <= 0) {
                continue;
            }

            // Adjust availability for roles. Method may return null as a filtering mechanism.
            roleRating = MissionRole.adjustAvailabilityByRole(adjRating,
                roles, curModel, exactYear, roleStrictness);

            if (roleRating == null || roleRating.doubleValue() <= 0) {
                continue;
            }

            // Calculate the weight and add it to the total
            adjRating = AvailabilityRating.calcWeight(roleRating.doubleValue());
            retVal += adjRating;

            // Cache the final availability rating
            weightData.put(curModel.getKey(), adjRating);
        }

        return retVal;
    }

    public double totalModelWeight(int era, String fKey) {
        FactionRecord fRec = RATGenerator.getInstance().getFaction(fKey);
        if (fRec == null) {
            logger.warn("Attempt to find totalModelWeight for non-existent faction " + fKey);
            return 0;
        }
        return totalModelWeight(era, fRec);
    }

    public double totalModelWeight(int era, FactionRecord fRec) {
        double retVal = 0;
        RATGenerator ratGen = RATGenerator.getInstance();

        for (ModelRecord curModel : models) {
            AvailabilityRating avRating = ratGen.findModelAvailabilityRecord(era,
                    curModel.getKey(), fRec);
            if (avRating != null) {
                retVal += AvailabilityRating.calcWeight(avRating.getAvailability());
            }
        }

        return retVal;
    }
}
