/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common.equipment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.interfaces.ITechnology;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.ProtoMek;
import megamek.common.units.SmallCraft;

public class ArmorType extends MiscType {

    private static final double BASE_POINTS_PER_TON = 16.0;

    private static final Map<Integer, ArmorType> armorTypeLookupIS = new HashMap<>();
    private static final Map<Integer, ArmorType> armorTypeLookupClan = new HashMap<>();

    private static final List<ArmorType> allTypes = new ArrayList<>();

    public static final int STEALTH_ARMOR_HEAT = 10;

    public static ArmorType of(int type, boolean clan) {
        if (armorTypeLookupClan.isEmpty() && armorTypeLookupIS.isEmpty()) {
            initializeTypes();
        }

        ArmorType armor = clan ? armorTypeLookupClan.get(type) : armorTypeLookupIS.get(type);
        // Some mixed tech unit files use the unit tech base instead of the armor tech base.
        if (armor == null) {
            armor = of(type, !clan);
        }

        return armor;
    }

    public static ArmorType svArmor(int bar) {
        return of(T_ARMOR_SV_BAR_2 - 2 + bar, false);
    }

    public static ArmorType forEntity(Entity entity, int loc) {
        return ArmorType.of(entity.getArmorType(loc), TechConstants.isClan(entity.getArmorTechLevel(loc)));
    }

    public static ArmorType forEntity(Entity entity) {
        return forEntity(entity, entity.firstArmorIndex());
    }

    public static List<ArmorType> allArmorTypes() {
        return Collections.unmodifiableList(allTypes);
    }

    public static List<String> allArmorNames() {
        return allTypes.stream().map(ArmorType::getName).distinct().collect(Collectors.toList());
    }

    public static Map<Integer, String> getAllArmorCodeName() {
        Map<Integer, String> result = new HashMap<>();

        for (ArmorType armorType : allTypes) {
            result.put(armorType.getArmorType(), getArmorTypeName(armorType.getArmorType()));
        }

        return result;
    }

    public static void initializeTypes() {
        addArmorType(createStandardArmor());
        addArmorType(createISFerroFibrous());
        addArmorType(createClanFerroFibrous());
        addArmorType(createLightFerroFibrous());
        addArmorType(createHeavyFerroFibrous());
        addArmorType(createFerroFibrousPrototype());
        addArmorType(createISFerroAluminum());
        addArmorType(createClanFerroAluminum());
        addArmorType(createLightFerroAluminum());
        addArmorType(createHeavyFerroAluminum());
        addArmorType(createFerroAluminumPrototype());
        addArmorType(createCommercialArmor());
        addArmorType(createIndustrialArmor());
        addArmorType(createHeavyIndustrialArmor());
        addArmorType(createISReactive());
        addArmorType(createClanReactive());
        addArmorType(createISReflective());
        addArmorType(createClanReflective());
        addArmorType(createHardenedArmor());
        addArmorType(createMekStealth());
        addArmorType(createVehicularStealth());
        addArmorType(createFerroLamellorArmor());
        addArmorType(createImpactResistant());
        addArmorType(createHeatDissipating());
        addArmorType(createAntiPenetrativeAblation());
        addArmorType(createBallisticReinforced());
        addArmorType(createPrimitiveArmor());
        addArmorType(createPrimitiveFighterArmor());

        addArmorType(createSVArmorBAR2());
        addArmorType(createSVArmorBAR3());
        addArmorType(createSVArmorBAR4());
        addArmorType(createSVArmorBAR5());
        addArmorType(createSVArmorBAR6());
        addArmorType(createSVArmorBAR7());
        addArmorType(createSVArmorBAR8());
        addArmorType(createSVArmorBAR9());
        addArmorType(createSVArmorBAR10());

        addArmorType(createISAeroSpaceArmor());
        addArmorType(createClanAeroSpaceArmor());
        addArmorType(createISImpFerroAluminumArmor());
        addArmorType(createClanImpFerroAluminumArmor());
        addArmorType(createISFerroCarbideArmor());
        addArmorType(createClanFerroCarbideArmor());
        addArmorType(createISLamellorFerroCarbideArmor());
        addArmorType(createClanLamellorFerroCarbideArmor());
        addArmorType(createPrimitiveLCAerospaceArmor());

        addArmorType(createStandardProtomekArmor());
        addArmorType(createElectricDischargeArmor());

        addArmorType(createISBAStandardArmor());
        addArmorType(createClanBAStandardArmor());
        addArmorType(createISBAStandardPrototypeArmor());
        addArmorType(createISBAAdvancedArmor());
        addArmorType(createISBABasicStealth());
        addArmorType(createClanBABasicStealth());
        addArmorType(createISBAStandardStealth());
        addArmorType(createClanBAStandardStealth());
        addArmorType(createISBAImprovedStealth());
        addArmorType(createClanBAImprovedStealth());
        addArmorType(createISBAStealthPrototype());
        addArmorType(createClanBAFireResistantArmor());
        addArmorType(createISBAMimeticCamo());
        addArmorType(createISBAReflectiveArmor());
        addArmorType(createClanBAReflectiveArmor());
        addArmorType(createISBAReactiveArmor());
        addArmorType(createClanBAReactiveArmor());

        addArmorType(createPatchworkArmor());
        addArmorType(createNoArmor());
    }

    private static void addArmorType(ArmorType at) {
        if ((at.getArmorType() != T_ARMOR_PATCHWORK) && (at.getArmorType() != T_ARMOR_UNKNOWN)) {
            EquipmentType.addType(at);
            allTypes.add(at);
        }
        if ((at.techAdvancement.getTechBase() == TechBase.IS)
              || (at.techAdvancement.getTechBase() == TechBase.ALL)) {
            armorTypeLookupIS.put(at.armorType, at);
        }
        if ((at.techAdvancement.getTechBase() == TechBase.CLAN)
              || (at.techAdvancement.getTechBase() == TechBase.ALL)) {
            armorTypeLookupClan.put(at.armorType, at);
        }
    }

    private static final int[] spheroidDSThresholds = {
          12500, 20000, 35000, 50000, 65000
    };
    private static final int[] aerodyneDSThresholds = {
          6000, 9500, 12500, 17500, 25000
    };
    private static final int[] capitalShipThresholds = {
          150000, 250000
    };

    private int armorType = T_ARMOR_UNKNOWN;
    private int fighterSlots = 0;
    private int patchworkSlotsMekSV = 0;
    private int patchworkSlotsCVFtr = 0;
    private double pptMultiplier = 1.0;
    private double weightPerPoint = 0.0;
    private double[] pptDropship = {};
    private double[] pptCapital = {};
    private final EnumMap<TechRating, Double> weightPerPointSV = new EnumMap<>(TechRating.class);
    private int bar = 10;

    public ArmorType() {
        hittable = false;
        omniFixedOnly = true;
        spreadable = true;
        bv = 0;
        criticalSlots = 0;
        tankSlots = 0;
        svSlots = 0;
    }

    /**
     * @return The cost of this armor per ton, or per point for BA and protomek armor.
     */
    public double getCost() {
        return cost;
    }

    /**
     * @return The EquipmentType.T_ARMOR_* constant for this armor.
     */
    public int getArmorType() {
        return armorType;
    }

    /**
     * @return The number of weapon slots taken by the armor on an aerospace fighter.
     */
    public int getFighterSlots() {
        return fighterSlots;
    }

    /**
     * Space required by patchwork armor for Meks and support vehicles
     *
     * @return The number of critical/item slots taken by the armor per location as patchwork armor.
     */
    public int getPatchworkSlotsMekSV() {
        return patchworkSlotsMekSV;
    }

    /**
     * Space required by patchwork armor for combat vehicles and conventional/aerospace fighters
     *
     * @return The number of item/weapon slots taken by the armor per location as patchwork armor.
     */
    public int getPatchworkSlotsCVFtr() {
        return patchworkSlotsCVFtr;
    }

    @Override
    public int getSupportVeeSlots(Entity entity) {
        // Support vehicle armor takes slots like CV ferro-fibrous at BAR 10/TL E/F
        if ((entity != null) && (getArmorType() == T_ARMOR_SV_BAR_10)) {
            if (entity.getArmorTechRating() == TechRating.E) {
                return ArmorType.of(T_ARMOR_FERRO_FIBROUS, false).svSlots;
            } else if (entity.getArmorTechRating() == TechRating.F) {
                return ArmorType.of(T_ARMOR_FERRO_FIBROUS, true).svSlots;
            }
        }
        return svSlots;
    }

    /**
     * Used for entities that do not vary the coverage by tonnage. For large craft, use
     * {@link ArmorType#getPointsPerTon(Entity)}.
     *
     * @return The number of armor points per ton of armor.
     */
    public double getPointsPerTon() {
        return BASE_POINTS_PER_TON * pptMultiplier;
    }

    /**
     * Calculates the armor coverage per ton of armor. For large craft, this takes tonnage of the craft into account.
     *
     * @param entity The Entity the armor is mounted on
     *
     * @return The number of armor points per ton of armor.
     */
    public double getPointsPerTon(Entity entity) {
        int[] threshold = null;
        double[] ppt = null;
        if (entity instanceof Jumpship) {
            threshold = capitalShipThresholds;
            ppt = pptCapital;
        } else if (entity instanceof SmallCraft) {
            threshold = entity.isSpheroid() ? spheroidDSThresholds : aerodyneDSThresholds;
            ppt = pptDropship;
        }
        if ((threshold != null) && (ppt.length > threshold.length)) {
            for (int i = 0; i < threshold.length; i++) {
                if (entity.getWeight() < threshold[i]) {
                    return ppt[i];
                }
            }
            return ppt[ppt.length - 1];
        }
        return getPointsPerTon();
    }

    public double getArmorPointsMultiplier() {
        return pptMultiplier;
    }

    public double getWeightPerPoint() {
        return weightPerPoint;
    }

    /**
     * @param techRating The support vehicle's armor tech rating
     *
     * @return The weight in tons of each point of armor. A value of 0.0 means that the armor does not exist at that
     *       tech level (or it is not SV BAR armor).
     */
    public double getSVWeightPerPoint(TechRating techRating) {
        return weightPerPointSV.getOrDefault(techRating, 0.0);
    }

    public int getBAR() {
        return bar;
    }

    private static ArmorType createStandardArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Standard";
        armor.setInternalName("Standard Armor");
        armor.addLookupName("IS Standard");
        armor.addLookupName("Clan Standard");
        armor.addLookupName("Regular");
        armor.addLookupName("IS Standard Armor");
        armor.addLookupName("Clan Standard Armor");
        armor.flags = armor.flags.or(F_MEK_EQUIPMENT)
              .or(F_TANK_EQUIPMENT)
              .or(F_SUPPORT_TANK_EQUIPMENT)
              .or(F_VTOL_EQUIPMENT)
              .or(F_FIGHTER_EQUIPMENT);
        armor.criticalSlots = 0;
        armor.cost = 10000.0;

        armor.techAdvancement = new TechAdvancement(TechBase.ALL)
              .setAdvancement(2460, 2470, 2470)
              .setApproximate(true, false, false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.B)
              .setStaticTechLevel(SimpleTechLevel.INTRO);

        armor.armorType = T_ARMOR_STANDARD;
        armor.weightPerPoint = 0.050; // when used as protomek armor

        return armor;
    }

    private static ArmorType createISFerroFibrous() {
        ArmorType armor = new ArmorType();

        armor.name = "Ferro-Fibrous";
        armor.setInternalName("IS Ferro-Fibrous");
        armor.addLookupName("IS Ferro-Fibrous Armor");
        armor.addLookupName("IS Ferro Fibre");
        armor.cost = 20000.0;
        armor.criticalSlots = CRITICAL_SLOTS_VARIABLE;
        armor.tankSlots = 2;
        armor.svSlots = 2;
        armor.patchworkSlotsMekSV = 2;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_FERRO_FIBROUS).or(F_MEK_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        armor.bv = 0;
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TechBase.ALL)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
              .setISAdvancement(2557, 2571, 3055, 2810, 3040)
              .setISApproximate(false, false, false, true, false)
//              .setClanAdvancement(DATE_NONE, DATE_NONE, 2820, DATE_NONE, DATE_NONE)
//              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.DC);

        armor.armorType = T_ARMOR_FERRO_FIBROUS;
        armor.pptMultiplier = 1.12;

        return armor;
    }

    private static ArmorType createClanFerroFibrous() {
        ArmorType armor = new ArmorType();

        armor.name = "Ferro-Fibrous";
        armor.setInternalName("Clan Ferro-Fibrous");
        armor.addLookupName("Clan Ferro-Fibrous Armor");
        armor.addLookupName("Clan Ferro Fibre");
        armor.cost = 20000.0;
        armor.criticalSlots = CRITICAL_SLOTS_VARIABLE;
        armor.tankSlots = 1;
        armor.svSlots = 1;
        armor.patchworkSlotsMekSV = 1;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_FERRO_FIBROUS).or(F_MEK_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        armor.bv = 0;
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.C)
              .setClanAdvancement(2820, 2825, 2830, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false).setPrototypeFactions(Faction.CSR)
              .setProductionFactions(Faction.CSR);

        armor.armorType = T_ARMOR_FERRO_FIBROUS;
        armor.pptMultiplier = 1.2;

        return armor;
    }

    private static ArmorType createLightFerroFibrous() {
        ArmorType armor = new ArmorType();

        armor.name = "Light Ferro-Fibrous";
        armor.setInternalName("IS Light Ferro-Fibrous");
        armor.addLookupName("IS Light Ferro-Fibrous Armor");
        armor.addLookupName("IS LightFerro");
        armor.cost = 15000.0;
        armor.criticalSlots = CRITICAL_SLOTS_VARIABLE;
        armor.tankSlots = 1;
        armor.svSlots = 1;
        armor.patchworkSlotsMekSV = 1;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_LIGHT_FERRO).or(F_MEK_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3067, 3070, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false).setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.FW);

        armor.armorType = T_ARMOR_LIGHT_FERRO;
        armor.pptMultiplier = 1.06;

        return armor;
    }

    private static ArmorType createHeavyFerroFibrous() {
        ArmorType armor = new ArmorType();

        armor.name = "Heavy Ferro-Fibrous";
        armor.setInternalName("IS Heavy Ferro-Fibrous");
        armor.addLookupName("IS Heavy Ferro-Fibrous Armor");
        armor.cost = 25000.0;
        armor.criticalSlots = CRITICAL_SLOTS_VARIABLE;
        armor.tankSlots = 3;
        armor.svSlots = 3;
        armor.patchworkSlotsMekSV = 3;
        armor.patchworkSlotsCVFtr = 2;
        armor.flags = armor.flags.or(F_HEAVY_FERRO).or(F_MEK_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3056, 3069, 3070, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false).setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.LC);

        armor.armorType = T_ARMOR_HEAVY_FERRO;
        armor.pptMultiplier = 1.24;

        return armor;
    }

    private static ArmorType createFerroFibrousPrototype() {
        ArmorType armor = new ArmorType();

        armor.name = "Ferro-Fibrous Prototype";
        armor.setInternalName("IS Ferro-Fibrous Prototype");
        armor.addLookupName("IS Ferro-Fibrous Armor Prototype");
        armor.cost = 60000.0;
        armor.criticalSlots = CRITICAL_SLOTS_VARIABLE;
        armor.tankSlots = 3;
        armor.svSlots = 3;
        armor.patchworkSlotsMekSV = 2;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_FERRO_FIBROUS_PROTO).or(F_MEK_EQUIPMENT).or(F_TANK_EQUIPMENT)
              .or(F_VTOL_EQUIPMENT);
        armor.rulesRefs = "66, IO:AE";
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2557, DATE_NONE, DATE_NONE, 2571, 3034)
              .setISApproximate(true, false, false, true, true).setPrototypeFactions(Faction.TH)
              .setReintroductionFactions(Faction.LC, Faction.DC);

        armor.armorType = T_ARMOR_FERRO_FIBROUS_PROTO;
        armor.pptMultiplier = 1.12;

        return armor;
    }

    private static ArmorType createISFerroAluminum() {
        ArmorType armor = new ArmorType();

        armor.name = "Ferro-Aluminum";
        armor.setInternalName("IS Ferro-Aluminum");
        armor.addLookupName("IS Ferro-Aluminum Armor");
        armor.cost = 20000.0;
        armor.fighterSlots = 2;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_FERRO_FIBROUS).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.C)
              .setISAdvancement(2557, 2571, 3055, 2810, 3040)
              .setISApproximate(false, false, false, true, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.DC);

        armor.armorType = T_ARMOR_ALUM;
        armor.pptMultiplier = 1.12;
        armor.pptDropship = new double[] { 17.92, 15.68, 13.44, 11.2, 8.96, 6.72 };

        return armor;
    }

    private static ArmorType createClanFerroAluminum() {
        ArmorType armor = new ArmorType();

        armor.name = "Ferro-Aluminum";
        armor.setInternalName("Clan Ferro-Aluminum");
        armor.addLookupName("Clan Ferro-Aluminum Armor");
        armor.cost = 20000.0;
        armor.fighterSlots = 2;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_FERRO_FIBROUS).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.C)
              .setClanAdvancement(2820, 2825, 2830, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, true, false, false, false).setPrototypeFactions(Faction.CSR)
              .setProductionFactions(Faction.CSR);

        armor.armorType = T_ARMOR_ALUM;
        armor.pptMultiplier = 1.2;
        armor.pptDropship = new double[] { 24.0, 20.4, 16.8, 14.4, 12.0, 8.4 };

        return armor;
    }

    private static ArmorType createLightFerroAluminum() {
        ArmorType armor = new ArmorType();

        armor.name = "Light Ferro-Aluminum";
        armor.setInternalName("IS Light Ferro-Aluminum");
        armor.addLookupName("IS Light Ferro-Aluminum Armor");
        armor.cost = 15000.0;
        armor.fighterSlots = 1;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_LIGHT_FERRO).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3055, 3067, 3070, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false).setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.FW);

        armor.armorType = T_ARMOR_LIGHT_ALUM;
        armor.pptMultiplier = 1.06;
        armor.pptDropship = new double[] { 16.96, 14.84, 12.72, 10.6, 8.48, 6.36 };

        return armor;
    }

    private static ArmorType createHeavyFerroAluminum() {
        ArmorType armor = new ArmorType();

        armor.name = "Heavy Ferro-Aluminum";
        armor.setInternalName("IS Heavy Ferro-Aluminum");
        armor.addLookupName("IS Heavy Ferro-Aluminum Armor");
        armor.cost = 25000.0;
        armor.fighterSlots = 4;
        armor.patchworkSlotsCVFtr = 2;
        armor.flags = armor.flags.or(F_HEAVY_FERRO).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3056, 3069, 3070, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false).setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.LC);

        armor.armorType = T_ARMOR_HEAVY_ALUM;
        armor.pptMultiplier = 1.24;
        armor.pptDropship = new double[] { 19.84, 17.36, 14.88, 12.4, 9.92, 7.44 };

        return armor;
    }

    private static ArmorType createFerroAluminumPrototype() {
        ArmorType armor = new ArmorType();

        armor.name = "Prototype Ferro-Aluminum";
        armor.setInternalName("IS Prototype Ferro-Aluminum");
        armor.addLookupName("IS Ferro-Alum Armor Prototype");
        armor.cost = 60000.0;
        armor.fighterSlots = 3;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_FERRO_FIBROUS_PROTO).or(F_FIGHTER_EQUIPMENT)
              .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        armor.rulesRefs = "66, IO:AE";
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(2557, DATE_NONE, DATE_NONE, 2571, 3034)
              .setISApproximate(true, false, false, true, true).setPrototypeFactions(Faction.TH)
              .setReintroductionFactions(Faction.LC, Faction.DC);

        armor.armorType = EquipmentType.T_ARMOR_FERRO_ALUM_PROTO;
        armor.pptMultiplier = 1.12;
        armor.pptDropship = new double[] { 17.92, 15.68, 13.44, 11.2, 8.96, 6.72 };

        return armor;
    }

    private static ArmorType createCommercialArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Commercial";
        armor.setInternalName(armor.name);
        armor.addLookupName("IS Commercial");
        armor.addLookupName("Clan Commercial");
        armor.cost = 3000.0;
        armor.industrial = true;
        armor.flags = armor.flags.or(F_COMMERCIAL_ARMOR).or(F_MEK_EQUIPMENT);
        armor.omniFixedOnly = true;
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TechBase.ALL).setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.A, AvailabilityValue.A)
              .setAdvancement(2290, 2300, 2310)
              .setApproximate(true, true, false).setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA);

        armor.armorType = T_ARMOR_COMMERCIAL;
        armor.pptMultiplier = 1.5;
        armor.bar = 5;

        return armor;
    }

    private static ArmorType createIndustrialArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Industrial "; // extra space at the end on purpose
        armor.setInternalName(armor.name);
        armor.addLookupName("IS Industrial");
        armor.addLookupName("Clan Industrial");
        armor.addLookupName("Clan Industrial Armor");
        armor.cost = 5000.0;
        armor.industrial = true;
        armor.flags = armor.flags.or(F_INDUSTRIAL_ARMOR).or(F_MEK_EQUIPMENT);
        armor.omniFixedOnly = true;
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TechBase.ALL).setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B, AvailabilityValue.B)
              .setAdvancement(2430, 2439, 2439)
              .setApproximate(true, true, true).setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);

        armor.armorType = T_ARMOR_INDUSTRIAL;
        armor.pptMultiplier = 0.67;

        return armor;
    }

    private static ArmorType createHeavyIndustrialArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Heavy Industrial";
        armor.setInternalName("IS Heavy Industrial");
        armor.addLookupName("Clan Heavy Industrial");
        armor.cost = 10000.0;
        armor.industrial = true;
        armor.flags = armor.flags.or(F_HEAVY_INDUSTRIAL_ARMOR).or(F_MEK_EQUIPMENT);
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TechBase.ALL).setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.B)
              .setAdvancement(2460, 2470, 2470)
              .setApproximate(false, true, false).setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);

        armor.armorType = T_ARMOR_HEAVY_INDUSTRIAL;

        return armor;
    }

    private static ArmorType createPrimitiveArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Primitive";
        armor.setInternalName("IS Primitive");
        armor.cost = 5000.0;
        armor.industrial = true;
        armor.flags = armor.flags.or(F_PRIMITIVE_ARMOR).or(F_MEK_EQUIPMENT);
        armor.rulesRefs = "118, IO:AE";
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B, AvailabilityValue.B)
              .setISAdvancement(DATE_ES, 2290, 2315, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, true, false, false)
              .setClanAdvancement(DATE_ES, 2290, 2315, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, true, false, false).setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);

        armor.armorType = T_ARMOR_PRIMITIVE;
        armor.pptMultiplier = 0.67;

        return armor;
    }

    private static ArmorType createPrimitiveFighterArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Primitive Fighter";
        armor.setInternalName("IS Primitive Fighter");
        armor.shortName = "Primitive";
        armor.cost = 5000.0;
        armor.industrial = true;
        armor.flags = armor.flags.or(F_PRIMITIVE_ARMOR).or(F_FIGHTER_EQUIPMENT);
        armor.omniFixedOnly = true;
        armor.rulesRefs = "119, IO:AE";
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B, AvailabilityValue.B)
              .setISAdvancement(DATE_ES, 2300, 2315).setISApproximate(false, true, true)
              .setClanApproximate(false, true, true, false, false)
              .setProductionFactions(Faction.TH).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_PRIMITIVE_FIGHTER;
        armor.pptMultiplier = 0.67;

        return armor;
    }

    private static ArmorType createISReactive() {
        ArmorType armor = new ArmorType();

        armor.name = "Reactive";
        armor.setInternalName("IS Reactive");
        armor.addLookupName("IS Reactive Armor");
        armor.addLookupName("IS Reactive");
        armor.cost = 30000.0;
        armor.criticalSlots = CRITICAL_SLOTS_VARIABLE;
        armor.tankSlots = 2;
        armor.svSlots = 2;
        armor.fighterSlots = 3;
        armor.patchworkSlotsMekSV = 2;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_REACTIVE).or(F_MEK_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT)
              .or(F_SUPPORT_TANK_EQUIPMENT);
        armor.bv = 0;
        armor.rulesRefs = "94, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3063, 3081, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false).setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_REACTIVE;

        return armor;
    }

    private static ArmorType createClanReactive() {
        ArmorType armor = new ArmorType();

        armor.name = "Reactive";
        armor.setInternalName("Clan Reactive");
        armor.addLookupName("Clan Reactive Armor");
        armor.addLookupName("Clan Reactive");
        armor.cost = 30000.0;
        armor.criticalSlots = CRITICAL_SLOTS_VARIABLE;
        armor.tankSlots = 1;
        armor.svSlots = 1;
        armor.fighterSlots = 1;
        armor.patchworkSlotsMekSV = 1;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_REACTIVE).or(F_MEK_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT)
              .or(F_SUPPORT_TANK_EQUIPMENT);
        armor.rulesRefs = "94, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(3065, 3081, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false).setPrototypeFactions(Faction.CGB)
              .setProductionFactions(Faction.CGB).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_REACTIVE;

        return armor;
    }

    private static ArmorType createISReflective() {
        ArmorType armor = new ArmorType();

        armor.name = "Reflective";
        armor.setInternalName("IS Reflective");
        armor.addLookupName("IS Reflective Armor");
        armor.addLookupName("IS Reflective");
        armor.cost = 30000.0;
        armor.criticalSlots = CRITICAL_SLOTS_VARIABLE;
        armor.tankSlots = 1;
        armor.svSlots = 1;
        armor.fighterSlots = 1;
        armor.patchworkSlotsMekSV = 2;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_REFLECTIVE).or(F_MEK_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT)
              .or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        armor.rulesRefs = "93, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3058, 3080, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false).setPrototypeFactions(Faction.LC)
              .setProductionFactions(Faction.LC).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_REFLECTIVE;

        return armor;
    }

    private static ArmorType createClanReflective() {
        ArmorType armor = new ArmorType();

        armor.name = "Reflective";
        armor.setInternalName("Clan Reflective");
        armor.addLookupName("Clan Reflective Armor");
        armor.addLookupName("Clan Reflective");
        armor.cost = 30000.0;
        armor.criticalSlots = CRITICAL_SLOTS_VARIABLE;
        armor.tankSlots = 1;
        armor.svSlots = 1;
        armor.fighterSlots = 1;
        armor.patchworkSlotsMekSV = 1;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_REFLECTIVE).or(F_MEK_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT)
              .or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        armor.rulesRefs = "93, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(3061, 3080, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false).setPrototypeFactions(Faction.CJF)
              .setProductionFactions(Faction.CJF).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_REFLECTIVE;

        return armor;
    }

    private static ArmorType createHardenedArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Hardened";
        armor.setInternalName("IS Hardened");
        armor.addLookupName("Clan Hardened");
        armor.cost = 15000.0;
        armor.criticalSlots = 0;
        armor.tankSlots = 1;
        armor.flags = armor.flags.or(F_HARDENED_ARMOR).or(F_MEK_EQUIPMENT).or(F_TANK_EQUIPMENT);
        armor.rulesRefs = "93, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TechBase.ALL)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3047, 3081, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setClanAdvancement(3061, 3081, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.FS, Faction.LC, Faction.CGB)
              .setProductionFactions(Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_HARDENED;
        armor.pptMultiplier = 0.5;

        return armor;
    }

    private static ArmorType createMekStealth() {
        ArmorType armor = new ArmorType();

        armor.name = "Stealth";
        armor.setInternalName("IS Stealth");
        armor.addLookupName("IS Stealth Armor");
        armor.cost = 50000.0;
        armor.criticalSlots = 12;
        armor.patchworkSlotsMekSV = 2;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_STEALTH).or(F_MEK_EQUIPMENT);
        String[] saModes = { "Off", "On" };
        armor.setModes(saModes);
        armor.setInstantModeSwitch(false);
        armor.bv = 0;
        armor.rulesRefs = "206, TM";
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3051, 3063, 3072, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.CC)
              .setProductionFactions(Faction.CC);

        armor.armorType = T_ARMOR_STEALTH;

        return armor;
    }

    private static ArmorType createVehicularStealth() {
        ArmorType armor = new ArmorType();

        armor.name = "Vehicular Stealth";
        armor.setInternalName("IS Vehicular Stealth");
        armor.addLookupName("IS Vehicular Stealth Armor");
        armor.shortName = "Stealth";
        armor.cost = 50000.0;
        armor.tankSlots = 2;
        armor.svSlots = 2;
        armor.fighterSlots = 2;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_STEALTH).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_VTOL_EQUIPMENT)
              .or(F_SUPPORT_TANK_EQUIPMENT);
        String[] saModes = { "Off", "On" };
        armor.setModes(saModes);
        armor.setInstantModeSwitch(false);
        armor.rulesRefs = "94, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(DATE_NONE, 3067, 3084, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, true, false, false).setPrototypeFactions(Faction.CC)
              .setProductionFactions(Faction.CC).setStaticTechLevel(SimpleTechLevel.STANDARD);

        armor.armorType = T_ARMOR_STEALTH_VEHICLE;

        return armor;
    }

    private static ArmorType createFerroLamellorArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Ferro-Lamellor";
        armor.setInternalName("Clan Ferro-Lamellor");
        armor.cost = 35000.0;
        armor.criticalSlots = CRITICAL_SLOTS_VARIABLE;
        armor.tankSlots = 1;
        armor.svSlots = 1;
        armor.fighterSlots = 2;
        armor.patchworkSlotsMekSV = 2;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_FERRO_LAMELLOR).or(F_MEK_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        armor.rulesRefs = "92, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setClanAdvancement(3070, 3109, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false).setPrototypeFactions(Faction.CSR)
              .setProductionFactions(Faction.CSR).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_FERRO_LAMELLOR;
        armor.pptMultiplier = 0.875;

        return armor;
    }

    private static ArmorType createHeatDissipating() {
        ArmorType armor = new ArmorType();

        armor.name = "Heat-Dissipating";
        armor.setInternalName("IS Heat-Dissipating");
        armor.addLookupName("Clan Heat-Dissipating");
        armor.cost = 25000.0;
        armor.criticalSlots = CRITICAL_SLOTS_VARIABLE;
        armor.patchworkSlotsMekSV = 1;
        armor.flags = armor.flags.or(F_HEAT_DISSIPATING).or(F_MEK_EQUIPMENT);
        armor.rulesRefs = "81, IO:AE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TechBase.ALL).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E)
              .setISAdvancement(3115, 3123, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(3115, 3126, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false).setPrototypeFactions(Faction.CC)
              .setProductionFactions(Faction.CC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_HEAT_DISSIPATING;
        armor.pptMultiplier = 0.625;

        return armor;
    }

    private static ArmorType createImpactResistant() {
        ArmorType armor = new ArmorType();

        armor.name = "Impact-Resistant";
        armor.setInternalName("IS Impact-Resistant");
        armor.cost = 20000.0;
        armor.criticalSlots = CRITICAL_SLOTS_VARIABLE;
        armor.patchworkSlotsMekSV = 2;
        armor.flags = armor.flags.or(F_IMPACT_RESISTANT).or(F_MEK_EQUIPMENT);
        armor.rulesRefs = "81, IO:AE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E)
              .setISAdvancement(3090, 3103, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false).setPrototypeFactions(Faction.LC)
              .setProductionFactions(Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_IMPACT_RESISTANT;
        armor.pptMultiplier = 0.875;

        return armor;
    }

    private static ArmorType createAntiPenetrativeAblation() {
        ArmorType armor = new ArmorType();

        armor.name = "Anti-Penetrative Ablation";
        armor.setInternalName("IS Anti-Penetrative Ablation");
        armor.cost = 15000.0;
        armor.criticalSlots = CRITICAL_SLOTS_VARIABLE;
        armor.tankSlots = 1;
        armor.svSlots = 1;
        armor.patchworkSlotsMekSV = 1;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_ANTI_PENETRATIVE_ABLATIVE).or(F_MEK_EQUIPMENT).or(F_TANK_EQUIPMENT)
              .or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        armor.rulesRefs = "80, IO:AE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E)
              .setISAdvancement(3100, 3114, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false).setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_ANTI_PENETRATIVE_ABLATION;
        armor.pptMultiplier = 0.75;

        return armor;
    }

    private static ArmorType createBallisticReinforced() {
        ArmorType armor = new ArmorType();

        armor.name = "Ballistic-Reinforced";
        armor.setInternalName("IS Ballistic-Reinforced");
        armor.cost = 25000.0;
        armor.criticalSlots = CRITICAL_SLOTS_VARIABLE;
        armor.tankSlots = 1;
        armor.svSlots = 1;
        armor.fighterSlots = 2;
        armor.patchworkSlotsMekSV = 2;
        armor.patchworkSlotsCVFtr = 1;
        armor.flags = armor.flags.or(F_BALLISTIC_REINFORCED).or(F_MEK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
              .or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        armor.rulesRefs = "81, IO:AE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E)
              .setISAdvancement(3120, 3131, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false).setPrototypeFactions(Faction.DC)
              .setProductionFactions(Faction.DC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_BALLISTIC_REINFORCED;
        armor.pptMultiplier = 0.75;

        return armor;
    }

    // Separate IS/Clan standard aerospace armor, which provides different points
    // per ton.
    private static ArmorType createISAeroSpaceArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Standard Aerospace";
        armor.setInternalName("IS Standard Aerospace");
        armor.flags = armor.flags.or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
              .or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        armor.cost = 10000.0;
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.B)
              .setISAdvancement(2460, 2470, 2470).setISApproximate(true, false, false)
              .setPrototypeFactions(Faction.TH).setProductionFactions(Faction.TH)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);

        armor.armorType = T_ARMOR_AEROSPACE;
        armor.pptDropship = new double[] { 16.0, 14.0, 12.0, 10.0, 8.0, 6.0 };
        armor.pptCapital = new double[] { 0.8, 0.6, 0.4 };

        return armor;
    }

    private static ArmorType createClanAeroSpaceArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Standard Aerospace";
        armor.setInternalName("Clan Standard Aerospace");
        armor.flags = armor.flags.or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
              .or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        armor.cost = 10000.0;
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.B)
              .setClanAdvancement(DATE_NONE, DATE_NONE, 2470)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);

        armor.armorType = T_ARMOR_AEROSPACE;
        armor.pptDropship = new double[] { 20.0, 17.0, 14.0, 12.0, 10.0, 7.0 };
        armor.pptCapital = new double[] { 1.0, 0.7, 0.5 };

        return armor;
    }

    private static ArmorType createISImpFerroAluminumArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Improved Ferro-Aluminum";
        armor.setInternalName("IS Improved Ferro-Aluminum");
        armor.addLookupName(armor.name);
        armor.addLookupName("ImprovedFerroAluminum");
        armor.flags = armor.flags.or(F_CAPITAL_ARMOR).or(F_IMP_FERRO).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT)
              .or(F_SS_EQUIPMENT);
        armor.cost = 50000.0;
        armor.rulesRefs = "152, SO";
        armor.techAdvancement.setTechBase(TechBase.IS)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(2500, 2520, DATE_NONE, 2950, 3052)
              .setISApproximate(true, true, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FS, Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_LC_FERRO_IMP;
        armor.pptCapital = new double[] { 1.0, 0.8, 0.6 };

        return armor;
    }

    private static ArmorType createClanImpFerroAluminumArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Improved Ferro-Aluminum";
        armor.setInternalName("Clan Improved Ferro-Aluminum");
        armor.flags = armor.flags.or(F_CAPITAL_ARMOR).or(F_IMP_FERRO).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT)
              .or(F_SS_EQUIPMENT);
        armor.cost = 50000.0;
        armor.rulesRefs = "152, SO";
        armor.techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setClanAdvancement(2500, 2520).setClanApproximate(false, true)
              .setPrototypeFactions(Faction.TH).setProductionFactions(Faction.TH)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_LC_FERRO_IMP;
        armor.pptCapital = new double[] { 1.2, 0.9, 0.7 };

        return armor;
    }

    private static ArmorType createISFerroCarbideArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Ferro-Carbide";
        armor.setInternalName("IS Ferro-Carbide");
        armor.addLookupName("Ferro-Carbide");
        armor.cost = 75000.0;
        armor.flags = armor.flags.or(F_CAPITAL_ARMOR).or(F_FERRO_CARBIDE).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT)
              .or(F_SS_EQUIPMENT);
        armor.rulesRefs = "152, SO";
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(2550, 2570, DATE_NONE, 2950, 3055).setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH).setReintroductionFactions(Faction.DC, Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_LC_FERRO_CARBIDE;
        armor.pptCapital = new double[] { 1.2, 1.0, 0.8 };

        return armor;
    }

    private static ArmorType createClanFerroCarbideArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Ferro-Carbide";
        armor.setInternalName("Clan Ferro-Carbide");
        armor.flags = armor.flags.or(F_CAPITAL_ARMOR).or(F_FERRO_CARBIDE).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT)
              .or(F_SS_EQUIPMENT);
        armor.cost = 75000.0;
        armor.rulesRefs = "152, SO";
        armor.techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setClanAdvancement(2550, 2570, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false).setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_LC_FERRO_CARBIDE;
        armor.pptCapital = new double[] { 1.4, 1.1, 0.9 };

        return armor;
    }

    private static ArmorType createISLamellorFerroCarbideArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Lamellor Ferro-Carbide";
        armor.setInternalName("IS Lamellor Ferro-Carbide");
        armor.addLookupName(armor.name);
        armor.flags = armor.flags.or(F_CAPITAL_ARMOR).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        armor.cost = 100000.0;
        armor.rulesRefs = "152, SO";
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(2600, 2615, DATE_NONE, 2950, 3055).setISApproximate(true, false, false, false, false)
              .setClanAdvancement(2600, 2615, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false).setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH).setReintroductionFactions(Faction.FS, Faction.FW, Faction.LC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE;
        armor.pptCapital = new double[] { 1.4, 1.2, 1.0 };

        return armor;
    }

    private static ArmorType createClanLamellorFerroCarbideArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Lamellor Ferro-Carbide";
        armor.setInternalName("Clan Lamellor Ferro-Carbide");
        armor.flags = armor.flags.or(F_CAPITAL_ARMOR).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        armor.cost = 100000.0;
        armor.rulesRefs = "152, SO";
        armor.techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setClanAdvancement(2600, 2615, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false).setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE;
        armor.pptCapital = new double[] { 1.6, 1.3, 1.1 };

        return armor;
    }

    private static ArmorType createPrimitiveLCAerospaceArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Primitive Aerospace";
        armor.setInternalName("IS Primitive Aerospace");
        armor.shortName = "Primitive";
        armor.cost = 5000.0;
        armor.industrial = true;
        armor.flags = armor.flags.or(F_PRIMITIVE_ARMOR).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
              .or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        armor.rulesRefs = "120, IO:AE";
        armor.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B, AvailabilityValue.B)
              .setISAdvancement(DATE_ES, 2300, 2315).setISApproximate(false, true, true)
              .setProductionFactions(Faction.TH).setStaticTechLevel(SimpleTechLevel.STANDARD);

        armor.armorType = T_ARMOR_PRIMITIVE_AERO;
        armor.pptDropship = new double[] { 10.56, 9.24, 7.92, 6.6, 5.28, 3.96 };
        armor.pptCapital = new double[] { 0.528, 0.396, 0.264 };

        return armor;
    }

    private static ArmorType createStandardProtomekArmor() {
        ArmorType armor = new ArmorType();
        // CHECKSTYLE IGNORE ForbiddenWords FOR 3 LINES
        armor.name = "Standard ProtoMech";
        armor.setInternalName(armor.name);
        armor.addLookupName("Clan Standard ProtoMech");
        armor.shortName = "Standard";
        armor.flags = armor.flags.or(F_PROTOMEK_EQUIPMENT);
        armor.criticalSlots = 0;
        armor.cost = 625.0;

        armor.techAdvancement = ProtoMek.TA_STANDARD_PROTOMEK;

        armor.armorType = T_ARMOR_STANDARD_PROTOMEK;
        armor.weightPerPoint = 0.050;

        return armor;
    }

    private static ArmorType createElectricDischargeArmor() {
        ArmorType armor = new ArmorType();
        // CHECKSTYLE IGNORE ForbiddenWords FOR 2 LINES
        armor.name = "Electric Discharge ProtoMech";
        armor.setInternalName("Clan Electric Discharge ProtoMech");
        armor.addLookupName("CLEDPArmor");
        armor.shortName = "EDP";
        armor.cost = 1250.0;
        armor.criticalSlots = 1;
        armor.flags = armor.flags.or(F_PROTOMEK_EQUIPMENT).or(F_ELECTRIC_DISCHARGE_ARMOR);
        armor.bv = 32;
        String[] modes = { "not charging", "charging" };
        armor.setModes(modes);
        armor.rulesRefs = "59, IO:AE";
        armor.techAdvancement.setTechBase(TechBase.CLAN).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.X)
              .setClanAdvancement(3071, DATE_NONE, DATE_NONE, 3085, DATE_NONE)
              .setClanApproximate(true, false, false, false, false).setPrototypeFactions(Faction.CFM)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        armor.armorType = T_ARMOR_EDP;
        armor.weightPerPoint = 0.075;

        return armor;
    }

    private static ArmorType createISBAStandardArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Standard (Basic)";
        armor.setInternalName("IS BA Standard (Basic)");
        armor.shortName = "Standard (Basic)";
        armor.cost = 10000.0;
        armor.criticalSlots = 0;
        armor.flags = armor.flags.or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";
        armor.techAdvancement.setTechBase(TechBase.ALL)
              .setISAdvancement(2680, DATE_NONE, 3054, DATE_NONE, 3050)
              .setISApproximate(true, false, true, false, false)
              .setPrototypeFactions(Faction.TH)
              .setReintroductionFactions(Faction.FS, Faction.LC, Faction.DC)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);

        armor.armorType = T_ARMOR_BA_STANDARD;
        armor.weightPerPoint = 0.050;

        return armor;
    }

    private static ArmorType createClanBAStandardArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Standard (Basic)";
        armor.setInternalName("Clan BA Standard (Basic)");
        armor.shortName = "Standard (Basic)";
        armor.cost = 10000.0;
        armor.criticalSlots = 0;
        armor.flags = armor.flags.or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";
        armor.techAdvancement.setTechBase(TechBase.CLAN)
              .setClanAdvancement(DATE_NONE, 2868, 3054).setClanApproximate(true, true, false)
              .setProductionFactions(Faction.CWF).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);

        armor.armorType = T_ARMOR_BA_STANDARD;
        armor.weightPerPoint = 0.025;

        return armor;
    }

    private static ArmorType createISBAStandardPrototypeArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Standard (Prototype)";
        armor.setInternalName("IS BA Standard (Prototype)");
        armor.shortName = "Standard (Prototype)";
        armor.cost = 10000.0;
        armor.criticalSlots = 4;
        armor.flags = armor.flags.or(F_BA_EQUIPMENT);
        armor.techAdvancement.setTechBase(TechBase.IS)
              .setISAdvancement(3050, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TH, Faction.FS, Faction.LC, Faction.DC)
              .setProductionFactions(Faction.TH, Faction.FS, Faction.LC, Faction.DC)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        armor.armorType = T_ARMOR_BA_STANDARD_PROTOTYPE;
        armor.weightPerPoint = 0.100;

        return armor;
    }

    private static ArmorType createISBAAdvancedArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Advanced";
        armor.setInternalName("IS BA Advanced");
        armor.shortName = "Advanced";
        armor.cost = 12500.0;
        armor.criticalSlots = 5;
        armor.flags = armor.flags.or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";
        armor.techAdvancement.setTechBase(TechBase.IS).setISAdvancement(DATE_NONE, 3057, 3060)
              .setProductionFactions(Faction.FW).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E);

        armor.armorType = T_ARMOR_BA_STANDARD_ADVANCED;
        armor.weightPerPoint = 0.040;

        return armor;
    }

    private static ArmorType createClanBAFireResistantArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Fire Resistant";
        armor.setInternalName("Clan BA Fire Resistant");
        armor.shortName = "Fire Resistant";
        armor.cost = 10000.0;
        armor.criticalSlots = 5;
        armor.flags = armor.flags.or(F_FIRE_RESISTANT).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "253, TM";
        armor.techAdvancement.setTechBase(TechBase.CLAN).setClanAdvancement(3052, 3058, 3065)
              .setClanApproximate(true, false, false).setPrototypeFactions(Faction.CFM)
              .setProductionFactions(Faction.CFM).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E);

        armor.armorType = T_ARMOR_BA_FIRE_RESIST;
        armor.weightPerPoint = 0.030;

        return armor;
    }

    private static ArmorType createISBAStealthPrototype() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Stealth (Prototype)";
        armor.setInternalName("IS BA Stealth (Prototype)");
        armor.addLookupName("Clan BA Stealth (Prototype)");
        armor.shortName = "Stealth (Prototype)";
        armor.cost = 50000.0;
        armor.criticalSlots = 4;
        armor.flags = armor.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";
        armor.techAdvancement.setTechBase(TechBase.IS).setISAdvancement(3050, 3052, 3054, 3055, DATE_NONE)
              .setISApproximate(false, false, false, false, false).setPrototypeFactions(Faction.FS, Faction.LC)
              .setProductionFactions(Faction.FS, Faction.LC).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.X);

        armor.armorType = T_ARMOR_BA_STEALTH_PROTOTYPE;
        armor.weightPerPoint = 0.100;

        return armor;
    }

    private static ArmorType createISBABasicStealth() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Stealth (Basic)";
        armor.setInternalName("IS BA Stealth (Basic)");
        armor.shortName = "Stealth (Basic)";
        armor.cost = 12000.0;
        armor.criticalSlots = 3;
        armor.flags = armor.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";
        armor.techAdvancement.setTechBase(TechBase.IS).setISAdvancement(2700, 2710, 3054, 2770, 3052)
              .setISApproximate(true, false, false, false, false).setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH).setReintroductionFactions(Faction.DC).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D);

        armor.armorType = T_ARMOR_BA_STEALTH_BASIC;
        armor.weightPerPoint = 0.055;

        return armor;
    }

    private static ArmorType createClanBABasicStealth() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Stealth (Basic)";
        armor.setInternalName("Clan BA Stealth (Basic)");
        armor.shortName = "Stealth (Basic)";
        armor.cost = 12000.0;
        armor.criticalSlots = 3;
        armor.flags = armor.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";
        armor.techAdvancement.setTechBase(TechBase.CLAN)
              .setClanAdvancement(DATE_NONE, DATE_NONE, 3054).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D);

        armor.armorType = T_ARMOR_BA_STEALTH_BASIC;
        armor.weightPerPoint = 0.030;

        return armor;
    }

    private static ArmorType createISBAStandardStealth() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Stealth (Standard)";
        armor.setInternalName("IS BA Stealth (Standard)");
        armor.addLookupName("IS BA Stealth");
        armor.shortName = "Stealth";
        armor.cost = 15000.0;
        armor.criticalSlots = 4;
        armor.flags = armor.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";
        armor.techAdvancement.setTechBase(TechBase.IS).setISAdvancement(2710, 2720, 3055, 2770, 3053)
              .setISApproximate(true, false, false, false, false).setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH).setReintroductionFactions(Faction.DC).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D);

        armor.armorType = T_ARMOR_BA_STEALTH;
        armor.weightPerPoint = 0.060;

        return armor;
    }

    private static ArmorType createClanBAStandardStealth() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Stealth (Standard)";
        armor.setInternalName("Clan BA Stealth (Standard)");
        armor.addLookupName("Clan BA Stealth");
        armor.shortName = "Stealth";
        armor.cost = 15000.0;
        armor.criticalSlots = 4;
        armor.flags = armor.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";
        armor.techAdvancement.setTechBase(TechBase.CLAN)
              .setClanAdvancement(DATE_NONE, DATE_NONE, 3055).setPrototypeFactions(Faction.TH)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D);

        armor.armorType = T_ARMOR_BA_STEALTH;
        armor.weightPerPoint = 0.035;

        return armor;
    }

    private static ArmorType createISBAImprovedStealth() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Stealth (Improved)";
        armor.setInternalName("IS BA Stealth (Improved)");
        armor.shortName = "Stealth (Improved)";
        armor.cost = 20000.0;
        armor.criticalSlots = 5;
        armor.flags = armor.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";

        armor.techAdvancement.setTechBase(TechBase.IS).setISAdvancement(3055, 3057, 3059)
              .setISApproximate(true, false, false, false, false).setPrototypeFactions(Faction.FW, Faction.WB)
              .setProductionFactions(Faction.FW, Faction.WB).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E);

        armor.armorType = T_ARMOR_BA_STEALTH_IMP;
        armor.weightPerPoint = 0.060;

        return armor;
    }

    private static ArmorType createClanBAImprovedStealth() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Stealth (Improved)";
        armor.setInternalName("Clan BA Stealth (Improved)");
        armor.shortName = "Stealth (Improved)";
        armor.cost = 20000.0;
        armor.criticalSlots = 5;
        armor.flags = armor.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";

        armor.techAdvancement.setTechBase(TechBase.CLAN)
              .setClanAdvancement(DATE_NONE, 3058, 3059)
              .setProductionFactions(Faction.CSR).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E);

        armor.armorType = T_ARMOR_BA_STEALTH_IMP;
        armor.weightPerPoint = 0.035;

        return armor;
    }

    private static ArmorType createISBAMimeticCamo() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Mimetic";
        armor.setInternalName("IS BA Mimetic");
        armor.shortName = "Mimetic";
        armor.cost = 15000.0;
        armor.criticalSlots = 7;
        armor.flags = armor.flags.or(F_STEALTH).or(F_VISUAL_CAMO).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "253, TM";
        armor.techAdvancement.setTechBase(TechBase.IS).setISAdvancement(3058, 3061, 3065, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false).setPrototypeFactions(Faction.CS, Faction.WB)
              .setProductionFactions(Faction.WB).setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E);

        armor.armorType = T_ARMOR_BA_MIMETIC;
        armor.weightPerPoint = 0.050;

        return armor;
    }

    private static ArmorType createISBAReactiveArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Reactive (Blazer)";
        armor.setInternalName("IS BA Reactive (Blazer)");
        armor.addLookupName("IS BA Reactive");
        armor.shortName = "Reactive";
        armor.cost = 37000.0;
        armor.criticalSlots = 7;
        armor.flags = armor.flags.or(F_BA_EQUIPMENT).or(F_REACTIVE);
        armor.rulesRefs = "93, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TechBase.IS)
              .setISAdvancement(3075, 3110, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.RS).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_BA_REACTIVE;
        armor.weightPerPoint = 0.060;

        return armor;
    }

    private static ArmorType createClanBAReactiveArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Reactive (Blazer)";
        armor.setInternalName("Clan BA Reactive (Blazer)");
        armor.addLookupName("Clan BA Reactive");
        armor.shortName = "Reactive";
        armor.cost = 37000.0;
        armor.criticalSlots = 7;
        armor.flags = armor.flags.or(F_BA_EQUIPMENT).or(F_REACTIVE);
        armor.rulesRefs = "94, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TechBase.CLAN)
              .setClanAdvancement(3075, 3110, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_BA_REACTIVE;
        armor.weightPerPoint = 0.035;

        return armor;
    }

    private static ArmorType createISBAReflectiveArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Laser Reflective (Reflec/Glazed)";
        armor.setInternalName("IS BA Laser Reflective (Reflec/Glazed)");
        armor.addLookupName("IS BA Reflective");
        armor.shortName = "Reflective";
        armor.cost = 37000.0;
        armor.criticalSlots = 7;
        armor.flags = armor.flags.or(F_BA_EQUIPMENT).or(F_REFLECTIVE);
        armor.rulesRefs = "93, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TechBase.IS)
              .setISAdvancement(3074, 3110, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setProductionFactions(Faction.DC).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_BA_REFLECTIVE;
        armor.weightPerPoint = 0.055;

        return armor;
    }

    private static ArmorType createClanBAReflectiveArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "BA Laser Reflective (Reflec/Glazed)";
        armor.setInternalName("Clan BA Laser Reflective (Reflec/Glazed)");
        armor.addLookupName("Clan BA Reflective");
        armor.shortName = "Reflective";
        armor.cost = 37000.0;
        armor.criticalSlots = 7;
        armor.flags = armor.flags.or(F_BA_EQUIPMENT).or(F_REFLECTIVE);
        armor.rulesRefs = "93, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TechBase.CLAN)
              .setClanAdvancement(3074, 3110, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, true, false, false, false)
              .setPrototypeFactions(Faction.CSF)
              .setProductionFactions(Faction.CNC).setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_BA_REFLECTIVE;
        armor.weightPerPoint = 0.030;

        return armor;
    }

    private static ArmorType createSVArmorBAR2() {
        ArmorType armor = new ArmorType();

        armor.name = "BAR 2 Armor";
        armor.setInternalName(armor.name);
        armor.shortName = "BAR 2";
        armor.cost = 50.0;
        armor.flags = armor.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_SUPPORT_VEE_BAR_ARMOR);
        armor.techAdvancement = new TechAdvancement(TechBase.ALL).setAdvancement(DATE_PS, DATE_PS, DATE_PS)
              .setTechRating(TechRating.A)
              .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        armor.rulesRefs = "134, TM";
        armor.bar = 2;

        armor.armorType = T_ARMOR_SV_BAR_2;
        armor.weightPerPointSV.put(TechRating.A, 0.040);
        armor.weightPerPointSV.put(TechRating.B, 0.025);
        armor.weightPerPointSV.put(TechRating.C, 0.016);
        armor.weightPerPointSV.put(TechRating.D, 0.013);
        armor.weightPerPointSV.put(TechRating.E, 0.012);
        armor.weightPerPointSV.put(TechRating.F, 0.011);
        return armor;
    }

    private static ArmorType createSVArmorBAR3() {
        ArmorType armor = new ArmorType();

        armor.name = "BAR 3 Armor";
        armor.setInternalName(armor.name);
        armor.shortName = "BAR 3";
        armor.cost = 100.0;
        armor.flags = armor.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_SUPPORT_VEE_BAR_ARMOR);
        armor.techAdvancement = new TechAdvancement(TechBase.ALL).setAdvancement(DATE_PS, DATE_PS, DATE_PS)
              .setTechRating(TechRating.A)
              .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        armor.rulesRefs = "134, TM";
        armor.bar = 3;

        armor.armorType = T_ARMOR_SV_BAR_3;
        armor.weightPerPointSV.put(TechRating.A, 0.060);
        armor.weightPerPointSV.put(TechRating.B, 0.038);
        armor.weightPerPointSV.put(TechRating.C, 0.024);
        armor.weightPerPointSV.put(TechRating.D, 0.019);
        armor.weightPerPointSV.put(TechRating.E, 0.017);
        armor.weightPerPointSV.put(TechRating.F, 0.016);

        return armor;
    }

    private static ArmorType createSVArmorBAR4() {
        ArmorType armor = new ArmorType();

        armor.name = "BAR 4 Armor";
        armor.setInternalName(armor.name);
        armor.shortName = "BAR 4";
        armor.cost = 150.0;
        armor.flags = armor.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_SUPPORT_VEE_BAR_ARMOR);
        armor.techAdvancement = new TechAdvancement(TechBase.ALL).setAdvancement(DATE_PS, DATE_PS, DATE_PS)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.A, AvailabilityValue.A)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        armor.rulesRefs = "134, TM";
        armor.bar = 4;

        armor.armorType = T_ARMOR_SV_BAR_4;
        armor.weightPerPointSV.put(TechRating.B, 0.050);
        armor.weightPerPointSV.put(TechRating.C, 0.032);
        armor.weightPerPointSV.put(TechRating.D, 0.026);
        armor.weightPerPointSV.put(TechRating.E, 0.023);
        armor.weightPerPointSV.put(TechRating.F, 0.021);

        return armor;
    }

    private static ArmorType createSVArmorBAR5() {
        ArmorType armor = new ArmorType();

        armor.name = "BAR 5 Armor";
        armor.setInternalName(armor.name);
        armor.shortName = "BAR 5";
        armor.cost = 200.0;
        armor.flags = armor.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_SUPPORT_VEE_BAR_ARMOR);
        armor.techAdvancement = new TechAdvancement(TechBase.ALL).setAdvancement(DATE_ES, DATE_ES, DATE_ES)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.A)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        armor.rulesRefs = "134, TM";
        armor.bar = 5;

        armor.armorType = T_ARMOR_SV_BAR_5;
        armor.weightPerPointSV.put(TechRating.B, 0.063);
        armor.weightPerPointSV.put(TechRating.C, 0.040);
        armor.weightPerPointSV.put(TechRating.D, 0.032);
        armor.weightPerPointSV.put(TechRating.E, 0.028);
        armor.weightPerPointSV.put(TechRating.F, 0.026);

        return armor;
    }

    private static ArmorType createSVArmorBAR6() {
        ArmorType armor = new ArmorType();

        armor.name = "BAR 6 Armor";
        armor.setInternalName(armor.name);
        armor.shortName = "BAR 6";
        armor.cost = 250.0;
        armor.flags = armor.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_SUPPORT_VEE_BAR_ARMOR);
        armor.techAdvancement = new TechAdvancement(TechBase.ALL).setAdvancement(DATE_ES, DATE_ES, DATE_ES)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.A)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        armor.rulesRefs = "134, TM";
        armor.bar = 6;

        armor.armorType = T_ARMOR_SV_BAR_6;
        armor.weightPerPointSV.put(TechRating.C, 0.048);
        armor.weightPerPointSV.put(TechRating.D, 0.038);
        armor.weightPerPointSV.put(TechRating.E, 0.034);
        armor.weightPerPointSV.put(TechRating.F, 0.032);

        return armor;
    }

    private static ArmorType createSVArmorBAR7() {
        ArmorType armor = new ArmorType();

        armor.name = "BAR 7 Armor";
        armor.setInternalName(armor.name);
        armor.shortName = "BAR 7";
        armor.cost = 300.0;
        armor.flags = armor.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_SUPPORT_VEE_BAR_ARMOR);
        armor.techAdvancement = new TechAdvancement(TechBase.ALL)
              .setAdvancement(2250, 2300, 2305)
              .setApproximate(true, true, false)
              .setPrototypeFactions(Faction.TA)
              .setProductionFactions(Faction.TA).setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        armor.rulesRefs = "134, TM";
        armor.bar = 7;

        armor.armorType = T_ARMOR_SV_BAR_7;
        armor.weightPerPointSV.put(TechRating.C, 0.056);
        armor.weightPerPointSV.put(TechRating.D, 0.045);
        armor.weightPerPointSV.put(TechRating.E, 0.040);
        armor.weightPerPointSV.put(TechRating.F, 0.037);

        return armor;
    }

    private static ArmorType createSVArmorBAR8() {
        ArmorType armor = new ArmorType();

        armor.name = "BAR 8 Armor";
        armor.setInternalName(armor.name);
        armor.shortName = "BAR 8";
        armor.cost = 400.0;
        armor.flags = armor.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_SUPPORT_VEE_BAR_ARMOR);
        armor.techAdvancement = new TechAdvancement(TechBase.ALL)
              .setAdvancement(2420, 2430, 2435)
              .setApproximate(true, false, false)
              .setPrototypeFactions(Faction.TH).setProductionFactions(Faction.TH)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.B, AvailabilityValue.B)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        armor.rulesRefs = "134, TM";
        armor.bar = 8;

        armor.armorType = T_ARMOR_SV_BAR_8;
        armor.weightPerPointSV.put(TechRating.D, 0.051);
        armor.weightPerPointSV.put(TechRating.E, 0.045);
        armor.weightPerPointSV.put(TechRating.F, 0.042);

        return armor;
    }

    private static ArmorType createSVArmorBAR9() {
        ArmorType armor = new ArmorType();

        armor.name = "BAR 9 Armor";
        armor.setInternalName(armor.name);
        armor.shortName = "BAR 9";
        armor.cost = 500.0;
        armor.flags = armor.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_SUPPORT_VEE_BAR_ARMOR);
        armor.techAdvancement = new TechAdvancement(TechBase.ALL)
              .setAdvancement(2420, 2430, 2435)
              .setApproximate(true, false, false)
              .setPrototypeFactions(Faction.TH).setProductionFactions(Faction.TH)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.B)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        armor.rulesRefs = "134, TM";
        armor.bar = 9;

        armor.armorType = T_ARMOR_SV_BAR_9;
        armor.weightPerPointSV.put(TechRating.D, 0.057);
        armor.weightPerPointSV.put(TechRating.E, 0.051);
        armor.weightPerPointSV.put(TechRating.F, 0.047);
        return armor;
    }

    private static ArmorType createSVArmorBAR10() {
        ArmorType armor = new ArmorType();

        armor.name = "BAR 10 Armor";
        armor.setInternalName(armor.name);
        armor.shortName = "BAR 10";
        armor.cost = 625.0;
        armor.flags = armor.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_SUPPORT_VEE_BAR_ARMOR);
        armor.techAdvancement = new TechAdvancement(TechBase.ALL)
              .setAdvancement(2420, 2430, 2435)
              .setPrototypeFactions(Faction.TH).setProductionFactions(Faction.TH)
              .setApproximate(true, false, false).setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.C)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
        armor.rulesRefs = "134, TM";
        armor.bar = 10;

        armor.armorType = T_ARMOR_SV_BAR_10;
        armor.weightPerPointSV.put(TechRating.D, 0.063);
        armor.weightPerPointSV.put(TechRating.E, 0.056);
        armor.weightPerPointSV.put(TechRating.F, 0.052);

        return armor;
    }

    // Not a true armor type
    private static ArmorType createPatchworkArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Patchwork";
        armor.setInternalName(armor.name);
        armor.cost = 0.0;
        armor.flags = armor.flags.or(F_MEK_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
              .or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        armor.techAdvancement = new TechAdvancement(TechBase.ALL)
              .setAdvancement(ITechnology.DATE_PS, 3075, 3080)
              .setApproximate(false, false, true)
              .setTechRating(TechRating.A)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.E)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_PATCHWORK;

        return armor;
    }

    /*
     * Placeholder for turrets and units that don't have a legal armor type such as
     * exoskeletons before
     * the introduction of BA armor.
     */
    private static ArmorType createNoArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "No Armor";
        armor.setInternalName(armor.name);
        armor.addLookupName("Unknown");
        armor.techAdvancement = new TechAdvancement();

        armor.armorType = T_ARMOR_UNKNOWN;

        return armor;
    }

    @Override
    public String toString() {
        return "[Armor] " + internalName;
    }

    @Override
    protected String getYamlTypeName() {
        return "armor";
    }

    // ArmorType uses MiscType's addFlags() which uses MiscTypeFlag

    @Override
    public Map<String, Object> getYamlData() {
        Map<String, Object> data = super.getYamlData();
        Map<String, Object> armorDetails = new java.util.LinkedHashMap<>();

        armorDetails.put("type", getArmorTypeIdName(armorType));

        if (fighterSlots != 0) {
            armorDetails.put("fighterSlots", fighterSlots);
        }
        if (patchworkSlotsMekSV != 0) {
            armorDetails.put("patchworkSlotsMekSV", patchworkSlotsMekSV);
        }
        if (patchworkSlotsCVFtr != 0) {
            armorDetails.put("patchworkSlotsCVFtr", patchworkSlotsCVFtr);
        }
        if (bar != 10) {
            armorDetails.put("bar", bar);
        }
        if (pptMultiplier != 1.0) {
            armorDetails.put("pptMultiplier", pptMultiplier);
        }
        if (weightPerPoint != 0.0) {
            armorDetails.put("weightPerPoint", weightPerPoint);
        }
        if (pptDropship != null && pptDropship.length > 0) {
            armorDetails.put("pptDropship", pptDropship);
        }
        if (pptCapital != null && pptCapital.length > 0) {
            armorDetails.put("pptCapital", pptCapital);
        }
        if (!weightPerPointSV.isEmpty()) {
            // Convert EnumMap to a simpler Map<String, Double> for YAML
            Map<String, Double> svWeights = new java.util.LinkedHashMap<>();
            for (Map.Entry<TechRating, Double> entry : weightPerPointSV.entrySet()) {
                svWeights.put(entry.getKey().name(), entry.getValue());
            }
            armorDetails.put("weightPerPointSV", svWeights);
        }

        data.put("armor", armorDetails);
        return data;
    }

    private static String getArmorTypeIdName(int armorId) {
        return switch (armorId) {
            case T_ARMOR_STANDARD -> "STANDARD";
            case T_ARMOR_FERRO_FIBROUS -> "FERRO_FIBROUS";
            case T_ARMOR_REACTIVE -> "REACTIVE";
            case T_ARMOR_REFLECTIVE -> "REFLECTIVE";
            case T_ARMOR_HARDENED -> "HARDENED";
            case T_ARMOR_LIGHT_FERRO -> "LIGHT_FERRO";
            case T_ARMOR_HEAVY_FERRO -> "HEAVY_FERRO";
            case T_ARMOR_PATCHWORK -> "PATCHWORK";
            case T_ARMOR_STEALTH -> "STEALTH";
            case T_ARMOR_FERRO_FIBROUS_PROTO -> "FERRO_FIBROUS_PROTO";
            case T_ARMOR_COMMERCIAL -> "COMMERCIAL";
            case T_ARMOR_LC_FERRO_CARBIDE -> "LC_FERRO_CARBIDE";
            case T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE -> "LC_LAMELLOR_FERRO_CARBIDE";
            case T_ARMOR_LC_FERRO_IMP -> "LC_FERRO_IMP";
            case T_ARMOR_INDUSTRIAL -> "INDUSTRIAL";
            case T_ARMOR_HEAVY_INDUSTRIAL -> "HEAVY_INDUSTRIAL";
            case T_ARMOR_FERRO_LAMELLOR -> "FERRO_LAMELLOR";
            case T_ARMOR_PRIMITIVE -> "PRIMITIVE";
            case T_ARMOR_EDP -> "EDP";
            case T_ARMOR_ALUM -> "ALUM";
            case T_ARMOR_HEAVY_ALUM -> "HEAVY_ALUM";
            case T_ARMOR_LIGHT_ALUM -> "LIGHT_ALUM";
            case T_ARMOR_STEALTH_VEHICLE -> "STEALTH_VEHICLE";
            case T_ARMOR_ANTI_PENETRATIVE_ABLATION -> "ANTI_PENETRATIVE_ABLATION";
            case T_ARMOR_HEAT_DISSIPATING -> "HEAT_DISSIPATING";
            case T_ARMOR_IMPACT_RESISTANT -> "IMPACT_RESISTANT";
            case T_ARMOR_BALLISTIC_REINFORCED -> "BALLISTIC_REINFORCED";
            case T_ARMOR_FERRO_ALUM_PROTO -> "FERRO_ALUM_PROTO";
            case T_ARMOR_BA_STANDARD -> "BA_STANDARD";
            case T_ARMOR_BA_STANDARD_PROTOTYPE -> "BA_STANDARD_PROTOTYPE";
            case T_ARMOR_BA_STANDARD_ADVANCED -> "BA_STANDARD_ADVANCED";
            case T_ARMOR_BA_STEALTH_BASIC -> "BA_STEALTH_BASIC";
            case T_ARMOR_BA_STEALTH -> "BA_STEALTH";
            case T_ARMOR_BA_STEALTH_IMP -> "BA_STEALTH_IMP";
            case T_ARMOR_BA_STEALTH_PROTOTYPE -> "BA_STEALTH_PROTOTYPE";
            case T_ARMOR_BA_FIRE_RESIST -> "BA_FIRE_RESIST";
            case T_ARMOR_BA_MIMETIC -> "BA_MIMETIC";
            case T_ARMOR_BA_REFLECTIVE -> "BA_REFLECTIVE";
            case T_ARMOR_BA_REACTIVE -> "BA_REACTIVE";
            case T_ARMOR_PRIMITIVE_FIGHTER -> "PRIMITIVE_FIGHTER";
            case T_ARMOR_PRIMITIVE_AERO -> "PRIMITIVE_AERO";
            case T_ARMOR_AEROSPACE -> "AEROSPACE";
            case T_ARMOR_STANDARD_PROTOMEK -> "STANDARD_PROTOMEK";
            case T_ARMOR_SV_BAR_2 -> "SV_BAR_2";
            case T_ARMOR_SV_BAR_3 -> "SV_BAR_3";
            case T_ARMOR_SV_BAR_4 -> "SV_BAR_4";
            case T_ARMOR_SV_BAR_5 -> "SV_BAR_5";
            case T_ARMOR_SV_BAR_6 -> "SV_BAR_6";
            case T_ARMOR_SV_BAR_7 -> "SV_BAR_7";
            case T_ARMOR_SV_BAR_8 -> "SV_BAR_8";
            case T_ARMOR_SV_BAR_9 -> "SV_BAR_9";
            case T_ARMOR_SV_BAR_10 -> "SV_BAR_10";
            default -> "UNKNOWN";
        };
    }
}
