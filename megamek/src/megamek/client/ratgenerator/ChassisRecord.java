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

import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import org.apache.logging.log4j.LogManager;

import java.util.*;

/**
 * The ChassisRecord tracks all available variants and determines how much total weight
 * is to be distributed among the various models.
 *
 * @author Neoancient
 */
public class ChassisRecord extends AbstractUnitRecord {

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

    public int totalModelWeight(int era, String fKey) {
        FactionRecord fRec = RATGenerator.getInstance().getFaction(fKey);
        if (fRec == null) {
            LogManager.getLogger().warn("Attempt to find totalModelWeight for non-existent faction " + fKey);
            return 0;
        }
        return totalModelWeight(era, fRec);
    }

    /**
     * Calculate the total 'bucket weight' of all models for this chassis, first converting
     * availability numbers to actual weight values (typically 2 ^ (AV / 2.0) ).
     * Models which are introduced close to the era year may be included, even when the model's
     * introduction date is later.
     * @param era    Year designator for the era to check e.g. 2765, 3058, 3067
     * @param fRec   RAT building data for the desired faction
     * @return       sum of all converted weights
     */

    public int totalModelWeight(int era, FactionRecord fRec) {
        int retVal = 0;
        RATGenerator rg = RATGenerator.getInstance();

        for (ModelRecord mr : models) {
            AvailabilityRating ar = rg.findModelAvailabilityRecord(era,
                    mr.getKey(), fRec);
            if (ar != null) {
                retVal += AvailabilityRating.calcWeight(ar.getAvailability());
            }
        }

        return retVal;
    }


    /**
     *  Calculate the total 'bucket weight' of models for this chassis, first converting
     *  availability numbers to actual weight values (typically 2 ^ (AV / 2.0) ).
     *  This overload allows a total weight to reflect various filter requirements, with models
     *  that don't meet filter requirements excluded from the total.
     *
     * @param era              Year designator for the era to check e.g. 2765, 3058, 3067
     * @param factionEraData   RAT building data for the desired faction
     * @param rating           Formation rating
     * @param year             Specific year to check
     * @param weightFilter     {@link EntityWeightClass} constants with valid weight classes, or
     *                         empty set to accept all
     * @param networkFilter    Mask for checking various C3 systems
     * @param movementFilter   Model must use at least one of these movement types
     * @param roleStrictness
     * @param rolesExcludeFilter  Model may not have any of these roles
     * @param roles            Roles to select for
     * @return                 int with total value of weight of all units which meet the filter
     *                         requirements
     */
    public int totalFilteredModelWeight (int era,
                                         FactionRecord factionEraData,
                                         int rating,
                                         int year,
                                         Collection<Integer> weightFilter,
                                         int networkFilter,
                                         Collection<EntityMovementMode> movementFilter,
                                         int roleStrictness,
                                         Collection<MissionRole> roles,
                                         Collection<MissionRole> rolesExcludeFilter) {
        double totalWeight = 0;
        RATGenerator generator = RATGenerator.getInstance();

        for (ModelRecord curModel : models) {

            // Models introduced more than 2 years from the current date are ignored. Those within
            // two years are considered field testing/prototypes.
            if (year < curModel.getIntroYear() - 2) {
                continue;
            }

            // Weight class
            if (!weightFilter.isEmpty() && !weightFilter.contains(curModel.getWeightClass())) {
                continue;
            }

            // Movement types
            if (!movementFilter.isEmpty() && !movementFilter.contains(curModel.getMovementMode())) {
                continue;
            }

            // Invalid roles
            if (!rolesExcludeFilter.isEmpty() && curModel.getRoles().stream().anyMatch(rolesExcludeFilter::contains)) {
                continue;
            }

            // C3 systems
            if ((networkFilter & curModel.getNetworkMask()) != networkFilter) {
                continue;
            }

            // If the model is considered available, convert the value to a weight and add it
            // to the total
            AvailabilityRating modelAvRating = generator.findModelAvailabilityRecord(era,
                    curModel.getKey(), factionEraData);
            if (modelAvRating != null) {

                Double adjustedAvRating = (double) curModel.calcAvailability(modelAvRating,
                        rating, 5, year);

                adjustedAvRating = MissionRole.adjustAvailabilityByRole(adjustedAvRating, roles, curModel, year, roleStrictness);
                if (adjustedAvRating != null && adjustedAvRating > 0) {
                    totalWeight += AvailabilityRating.calcWeight(adjustedAvRating);
                }
            }

        }

        return (int) Math.floor(totalWeight);
    }

}
