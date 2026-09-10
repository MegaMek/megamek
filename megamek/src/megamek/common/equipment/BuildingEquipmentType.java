/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package megamek.common.equipment;

import megamek.common.SimpleTechLevel;
import megamek.common.SourceBookCode;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.units.Entity;
import megamek.common.util.RoundWeight;

/** Building facilities use ordinary equipment/size blocks, rather than parallel storage fields. */
public class BuildingEquipmentType extends MiscType {
    public enum Facility {
        UNSPECIFIED("Unspecified Building Equipment", "Unspecified equipment", 0, 0),
        FLIGHT_DECK("Building Flight Deck", "Flight deck", 1500, 1000000),
        HELIPAD("Building Helipad", "Helipad", 500, 200000),
        LANDING_DECK("Building Landing Deck", "Landing deck", 500, 500000);

        private final String id;
        private final String label;
        private final double weight;
        private final double cost;

        Facility(String id, String label, double weight, double cost) {
            this.id = id;
            this.label = label;
            this.weight = weight;
            this.cost = cost;
        }

        public String internalName() {
            return id;
        }

        public boolean isRoof() {
            return this == FLIGHT_DECK || this == HELIPAD || this == LANDING_DECK;
        }

        public boolean isSpreadable() {
            return this == FLIGHT_DECK || this == LANDING_DECK;
        }
    }

    private final Facility facility;

    private BuildingEquipmentType(Facility facility) {
        this.facility = facility;
        name = facility.label;
        setInternalName(facility.id);
        tonnage = TONNAGE_VARIABLE;
        cost = COST_VARIABLE;
        criticalSlots = 1;
        bv = 0;
        spreadable = facility.isSpreadable();
        if (facility.weight == 0 || facility == Facility.LANDING_DECK) {
            flags = flags.or(F_VARIABLE_SIZE);
        }
        rulesRefs = facility.isRoof() ? rulesRefs(SourceBookCode.TO_AUE, 124, 131)
              : rulesRefs(SourceBookCode.TO_AR, 129, 133, 208);
        techAdvancement.setTechBase(TechBase.ALL).setAdvancement(DATE_PS, DATE_PS, DATE_PS)
              .setTechRating(TechRating.B).setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        if (facility == Facility.LANDING_DECK) {
            techAdvancement.setAdvancement(DATE_ES, DATE_ES, DATE_ES)
                  .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C);
        }
    }

    public static void initializeTypes() {
        for (var facility : Facility.values()) {
            EquipmentType.addType(new BuildingEquipmentType(facility));
        }
    }

    public Facility getFacility() {
        return facility;
    }

    @Override
    public double getTonnage(Entity entity, int location, double size, RoundWeight rounding) {
        return facility == Facility.LANDING_DECK ? facility.weight * size : facility.weight == 0 ? size : facility.weight;
    }

    @Override
    public double getCost(Entity entity, boolean armored, int location, double size) {
        // Unspecified-equipment cost is charged once per occupied hex, independently of the number of floors/items.
        return facility.weight == 0 || facility == Facility.LANDING_DECK ? facility.cost * size : facility.cost;
    }

    @Override
    public Double variableStepSize() {
        return facility == Facility.LANDING_DECK ? 1.0 : .5;
    }

    @Override
    public Double variableMaxSize() {
        return facility == Facility.LANDING_DECK ? 37.0 : null;
    }

    @Override
    public double getBV(Entity entity) {
        return 0;
    }
}
