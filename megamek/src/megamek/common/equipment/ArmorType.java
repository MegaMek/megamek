/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.common.equipment;

import megamek.common.*;

import java.util.HashMap;
import java.util.Map;

public class ArmorType extends MiscType {

    private int armorType = T_ARMOR_UNKNOWN;

    private static final Map<Integer, ArmorType> armorTypeLookupIS = new HashMap<>();
    private static final Map<Integer, ArmorType> armorTypeLookupClan = new HashMap<>();

    public static ArmorType of(int type, boolean clan) {
        return clan ? armorTypeLookupClan.get(type) : armorTypeLookupIS.get(type);
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

        addArmorType(createISAeroSpaceArmor());
        addArmorType(createClanAeroSpaceArmor());
        addArmorType(createISImpFerroAluminumArmor());
        addArmorType(createClanImpFerroAluminumArmor());
        addArmorType(createISFerroCarbideArmor());
        addArmorType(createClanFerroCarbideArmor());
        addArmorType(createISLamellorFerroCarbideArmor());
        addArmorType(createClanLamellorFerroCarbideArmor());
        addArmorType(createPrimitiveLCAerospaceArmor());

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
    }

    private static void addArmorType(ArmorType at) {
        EquipmentType.addType(at);
        if ((at.techAdvancement.getTechBase() == TechAdvancement.TECH_BASE_IS)
                || (at.techAdvancement.getTechBase() == TechAdvancement.TECH_BASE_ALL)) {
            armorTypeLookupIS.put(at.armorType, at);
        }
        if ((at.techAdvancement.getTechBase() == TechAdvancement.TECH_BASE_CLAN)
                || (at.techAdvancement.getTechBase() == TechAdvancement.TECH_BASE_ALL)) {
            armorTypeLookupClan.put(at.armorType, at);
        }
    }

    private ArmorType() {
        hittable = false;
        omniFixedOnly = true;
        spreadable = true;
        bv = 0;
    }

    private static ArmorType createStandardArmor() {
        ArmorType armor = new ArmorType();

        armor.name = "Standard";
        armor.setInternalName("Standard Armor");
        armor.addLookupName(EquipmentType.getArmorTypeName(T_ARMOR_STANDARD, false));
        armor.addLookupName(EquipmentType.getArmorTypeName(T_ARMOR_STANDARD, true));
        armor.addLookupName("Regular");
        armor.addLookupName("IS Standard Armor");
        armor.addLookupName("Clan Standard Armor");
        armor.flags = armor.flags.or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_PROTOMECH_EQUIPMENT);
        armor.criticals = 0;

        armor.techAdvancement = new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(2460, 2470, 2470).setApproximate(true, false, false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_C, RATING_C, RATING_B)
                .setStaticTechLevel(SimpleTechLevel.INTRO);

        armor.armorType = T_ARMOR_STANDARD;

        return armor;
    }

    private static ArmorType createISFerroFibrous() {
        ArmorType armor = new ArmorType();
        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS, false));
        armor.addLookupName("IS Ferro-Fibrous Armor");
        armor.addLookupName("IS Ferro Fibre");
        armor.tonnage = TONNAGE_VARIABLE;
        armor.criticals = CRITICALS_VARIABLE;
        armor.flags = armor.flags.or(F_FERRO_FIBROUS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        armor.bv = 0;
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_D, RATING_C).setISAdvancement(2557, 2571, 3055, 2810, 3040)
                .setISApproximate(false, false, false, true, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC);

        armor.armorType = T_ARMOR_FERRO_FIBROUS;

        return armor;
    }

    private static ArmorType createClanFerroFibrous() {
        ArmorType armor = new ArmorType();
        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS, true));
        armor.addLookupName("Clan Ferro-Fibrous Armor");
        armor.addLookupName("Clan Ferro Fibre");
        armor.tonnage = TONNAGE_VARIABLE;
        armor.criticals = CRITICALS_VARIABLE;
        armor.flags = armor.flags.or(F_FERRO_FIBROUS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        armor.bv = 0;
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_E, RATING_D, RATING_C)
                .setClanAdvancement(2820, 2825, 2830, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CSR)
                .setProductionFactions(F_CSR);

        armor.armorType = T_ARMOR_FERRO_FIBROUS;

        return armor;
    }

    private static ArmorType createLightFerroFibrous() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LIGHT_FERRO);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LIGHT_FERRO, false));
        armor.addLookupName("IS Light Ferro-Fibrous Armor");
        armor.addLookupName("IS LightFerro");
        armor.tonnage = TONNAGE_VARIABLE;
        armor.criticals = CRITICALS_VARIABLE;
        armor.flags = armor.flags.or(F_LIGHT_FERRO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3067, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_FW);

        armor.armorType = T_ARMOR_LIGHT_FERRO;

        return armor;
    }

    private static ArmorType createHeavyFerroFibrous() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_FERRO);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_FERRO, false));
        armor.addLookupName("IS Heavy Ferro-Fibrous Armor");
        armor.tonnage = TONNAGE_VARIABLE;
        armor.criticals = CRITICALS_VARIABLE;
        armor.flags = armor.flags.or(F_HEAVY_FERRO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3056, 3069, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_LC);

        armor.armorType = T_ARMOR_HEAVY_FERRO;

        return armor;
    }

    private static ArmorType createFerroFibrousPrototype() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO, false));
        armor.addLookupName("IS Ferro-Fibrous Armor Prototype");
        armor.tonnage = TONNAGE_VARIABLE;
        armor.criticals = CRITICALS_VARIABLE;
        armor.flags = armor.flags.or(F_FERRO_FIBROUS_PROTO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_VTOL_EQUIPMENT);
        armor.rulesRefs = "72, IO";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_X, RATING_X)
                .setISAdvancement(2557, DATE_NONE, DATE_NONE, 2571, 3034)
                .setISApproximate(true, false, false, true, true).setPrototypeFactions(F_TH)
                .setReintroductionFactions(F_LC, F_DC);

        armor.armorType = T_ARMOR_FERRO_FIBROUS_PROTO;

        return armor;
    }

    private static ArmorType createISFerroAluminum() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_ALUM);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_ALUM, false));
        armor.addLookupName("IS Ferro-Aluminum Armor");
        armor.tonnage = TONNAGE_VARIABLE;
        armor.flags = armor.flags.or(F_FERRO_FIBROUS).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_D, RATING_C).setISAdvancement(2557, 2571, 3055, 2810, 3040)
                .setISApproximate(false, false, false, true, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC);

        armor.armorType = T_ARMOR_ALUM;

        return armor;
    }

    private static ArmorType createClanFerroAluminum() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_ALUM);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_ALUM, true));
        armor.addLookupName("Clan Ferro-Aluminum Armor");
        armor.tonnage = TONNAGE_VARIABLE;
        armor.flags = armor.flags.or(F_FERRO_FIBROUS).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_E, RATING_D, RATING_C)
                .setClanAdvancement(2820, 2825, 2830, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CSR)
                .setProductionFactions(F_CSR);

        armor.armorType = T_ARMOR_ALUM;

        return armor;
    }

    private static ArmorType createLightFerroAluminum() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LIGHT_ALUM);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LIGHT_ALUM, false));
        armor.addLookupName("IS Light Ferro-Aluminum Armor");
        armor.tonnage = TONNAGE_VARIABLE;
        armor.flags = armor.flags.or(F_LIGHT_FERRO).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3067, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_FW);

        armor.armorType = T_ARMOR_LIGHT_ALUM;

        return armor;
    }

    private static ArmorType createHeavyFerroAluminum() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_ALUM);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_ALUM, false));
        armor.addLookupName("IS Heavy Ferro-Aluminum Armor");
        armor.tonnage = TONNAGE_VARIABLE;
        armor.flags = armor.flags.or(F_HEAVY_FERRO).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3056, 3069, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_LC);

        armor.armorType = T_ARMOR_HEAVY_ALUM;

        return armor;
    }

    private static ArmorType createFerroAluminumPrototype() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_ALUM_PROTO);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_ALUM_PROTO, false));
        armor.addLookupName("IS Ferro-Alum Armor Prototype");
        armor.tonnage = TONNAGE_VARIABLE;
        armor.flags = armor.flags.or(F_FERRO_FIBROUS_PROTO).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        armor.rulesRefs = "72, IO";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_X, RATING_X)
                .setISAdvancement(2557, DATE_NONE, DATE_NONE, 2571, 3034)
                .setISApproximate(true, false, false, true, true).setPrototypeFactions(F_TH)
                .setReintroductionFactions(F_LC, F_DC);

        armor.armorType = EquipmentType.T_ARMOR_FERRO_ALUM_PROTO;

        return armor;
    }

    private static ArmorType createCommercialArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_COMMERCIAL);
        armor.setInternalName(armor.name);
        armor.addLookupName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_COMMERCIAL, false));
        armor.addLookupName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_COMMERCIAL, true));
        armor.tonnage = TONNAGE_VARIABLE;
        armor.industrial = true;
        armor.flags = armor.flags.or(F_COMMERCIAL_ARMOR).or(F_MECH_EQUIPMENT);
        armor.omniFixedOnly = true;
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_B)
                .setAvailability(RATING_B, RATING_B, RATING_A, RATING_A)
                .setAdvancement(2290, 2300, 2310)
                .setApproximate(true, true, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);

        armor.armorType = T_ARMOR_COMMERCIAL;

        return armor;
    }

    private static ArmorType createIndustrialArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_INDUSTRIAL);
        armor.setInternalName(armor.name);
        armor.addLookupName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_INDUSTRIAL, false));
        armor.addLookupName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_INDUSTRIAL, true));
        armor.addLookupName("Clan Industrial Armor");
        armor.tonnage = TONNAGE_VARIABLE;
        armor.industrial = true;
        armor.flags = armor.flags.or(F_INDUSTRIAL_ARMOR).or(F_MECH_EQUIPMENT);
        armor.omniFixedOnly = true;
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_C, RATING_B, RATING_B)
                .setAdvancement(2430, 2439, 2439)
                .setApproximate(true, true, true).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);

        armor.armorType = T_ARMOR_INDUSTRIAL;

        return armor;
    }

    private static ArmorType createHeavyIndustrialArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL, false));
        armor.addLookupName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL, true));
        armor.tonnage = TONNAGE_VARIABLE;
        armor.industrial = true;
        armor.flags = armor.flags.or(F_HEAVY_INDUSTRIAL_ARMOR).or(F_MECH_EQUIPMENT);
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_B)
                .setAdvancement(2460, 2470, 2470)
                .setApproximate(false, true, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);

        armor.armorType = T_ARMOR_HEAVY_INDUSTRIAL;

        return armor;
    }

    private static ArmorType createPrimitiveArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PRIMITIVE);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PRIMITIVE, false));
        armor.tonnage = TONNAGE_VARIABLE;
        armor.industrial = true;
        armor.flags = armor.flags.or(F_PRIMITIVE_ARMOR).or(F_MECH_EQUIPMENT);
        armor.rulesRefs = "125, IO";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_C, RATING_B, RATING_B)
                .setISAdvancement(DATE_ES, 2290, 2315, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, true, false, false)
                .setClanAdvancement(DATE_ES, 2290, 2315, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, true, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);

        armor.armorType = T_ARMOR_PRIMITIVE;

        return armor;
    }

    private static ArmorType createPrimitiveFighterArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PRIMITIVE_FIGHTER);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PRIMITIVE_FIGHTER, false));
        armor.tonnage = TONNAGE_VARIABLE;
        armor.industrial = true;
        armor.flags = armor.flags.or(F_PRIMITIVE_ARMOR).or(F_FIGHTER_EQUIPMENT);
        armor.omniFixedOnly = true;
        armor.rulesRefs = "125, IO";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_C, RATING_B, RATING_B)
                .setISAdvancement(DATE_ES, 2300, 2315).setISApproximate(false, true, true)
                .setClanApproximate(false, true, true, false, false)
                .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_PRIMITIVE_FIGHTER;

        return armor;
    }

    private static ArmorType createISReactive() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REACTIVE);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REACTIVE, false));
        armor.addLookupName("IS Reactive Armor");
        armor.addLookupName("IS Reactive");
        armor.tonnage = 0;
        armor.criticals = CRITICALS_VARIABLE;
        armor.flags = armor.flags.or(F_REACTIVE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        armor.bv = 0;
        armor.rulesRefs = "94, TO: AU&E";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3063, 3081, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_REACTIVE;

        return armor;
    }

    private static ArmorType createClanReactive() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REACTIVE);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REACTIVE, true));
        armor.addLookupName("Clan Reactive Armor");
        armor.addLookupName("Clan Reactive");
        armor.tonnage = 0;
        armor.criticals = CRITICALS_VARIABLE;
        armor.flags = armor.flags.or(F_REACTIVE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        armor.rulesRefs = "94, TO: AU&E";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3065, 3081, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CGB)
                .setProductionFactions(F_CGB).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_REACTIVE;

        return armor;
    }

    private static ArmorType createISReflective() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REFLECTIVE);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REFLECTIVE, false));
        armor.addLookupName("IS Reflective Armor");
        armor.addLookupName("IS Reflective");
        armor.tonnage = 0;
        armor.criticals = CRITICALS_VARIABLE;
        armor.flags = armor.flags.or(F_REFLECTIVE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        armor.rulesRefs = "93, TO: AU&E";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3058, 3080, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_REFLECTIVE;

        return armor;
    }

    private static ArmorType createClanReflective() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REFLECTIVE);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REFLECTIVE, true));
        armor.addLookupName("Clan Reflective Armor");
        armor.addLookupName("Clan Reflective");
        armor.tonnage = 0;
        armor.criticals = CRITICALS_VARIABLE;
        armor.flags = armor.flags.or(F_REFLECTIVE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        armor.rulesRefs = "93, TO: AU&E";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3061, 3080, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CJF)
                .setProductionFactions(F_CJF).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_REFLECTIVE;

        return armor;
    }

    private static ArmorType createHardenedArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HARDENED);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HARDENED, false));
        armor.addLookupName("Clan Hardened");
        armor.tonnage = TONNAGE_VARIABLE;
        armor.criticals = 0;
        armor.flags = armor.flags.or(F_HARDENED_ARMOR).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT);
        armor.rulesRefs = "93, TO: AU&E";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3047, 3081, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(3061, 3081, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_FS, F_LC, F_CGB)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_HARDENED;

        return armor;
    }

    private static ArmorType createMekStealth() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_STEALTH);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_STEALTH, false));
        armor.addLookupName("IS Stealth Armor");
        armor.tonnage = TONNAGE_VARIABLE;
        armor.criticals = 12;
        armor.tankslots = 0;
        armor.flags = armor.flags.or(F_STEALTH).or(F_MECH_EQUIPMENT);
        String[] saModes = { "Off", "On" };
        armor.setModes(saModes);
        armor.setInstantModeSwitch(false);
        armor.bv = 0;
        armor.rulesRefs = "206, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3051, 3063, 3072, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC);

        armor.armorType = T_ARMOR_STEALTH;

        return armor;
    }

    private static ArmorType createVehicularStealth() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_STEALTH_VEHICLE);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_STEALTH_VEHICLE, false));
        armor.addLookupName("IS Vehicular Stealth Armor");
        armor.shortName = "Stealth";
        armor.tonnage = TONNAGE_VARIABLE;
        // Has to be 1, because we allocate 2 of them, so 2*1=2, which is
        // correct
        // When this was 2, it was ending up as 2*2=4 slots used on the tank.
        // Bad juju.
        armor.tankslots = 1;
        armor.flags = armor.flags.or(F_STEALTH).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_VTOL_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        String[] saModes = { "Off", "On" };
        armor.setModes(saModes);
        armor.setInstantModeSwitch(false);
        armor.rulesRefs = "94, TO: AU&E";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(DATE_NONE, 3067, 3084, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false).setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.STANDARD);

        armor.armorType = T_ARMOR_STEALTH_VEHICLE;

        return armor;
    }

    private static ArmorType createFerroLamellorArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_LAMELLOR);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_LAMELLOR, true));
        armor.tonnage = TONNAGE_VARIABLE;
        armor.criticals = CRITICALS_VARIABLE;
        armor.flags = armor.flags.or(F_FERRO_LAMELLOR).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        armor.rulesRefs = "92, TO: AU&E";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3070, 3109, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CSR)
                .setProductionFactions(F_CSR).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_FERRO_LAMELLOR;

        return armor;
    }

    private static ArmorType createHeatDissipating() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAT_DISSIPATING);
        armor.setInternalName("IS " + armor.name);
        armor.addLookupName("Clan Heat-Dissipating");
        armor.tonnage = TONNAGE_VARIABLE;
        armor.criticals = CRITICALS_VARIABLE;
        armor.hittable = false;
        armor.spreadable = true;
        armor.flags = armor.flags.or(F_HEAT_DISSIPATING).or(F_MECH_EQUIPMENT);
        armor.omniFixedOnly = true;
        armor.bv = 0;
        armor.rulesRefs = "87, IO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                .setISAdvancement(3115, 3123, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(3115, 3126, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_HEAT_DISSIPATING;

        return armor;
    }

    private static ArmorType createImpactResistant() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_IMPACT_RESISTANT);
        armor.setInternalName("IS " + armor.name);
        armor.tonnage = TONNAGE_VARIABLE;
        armor.criticals = CRITICALS_VARIABLE;
        armor.flags = armor.flags.or(F_IMPACT_RESISTANT).or(F_MECH_EQUIPMENT);
        armor.rulesRefs = "87, IO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                .setISAdvancement(3090, 3103, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_IMPACT_RESISTANT;

        return armor;
    }

    private static ArmorType createAntiPenetrativeAblation() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION);
        armor.setInternalName("IS " + armor.name);
        armor.tonnage = TONNAGE_VARIABLE;
        armor.criticals = CRITICALS_VARIABLE;
        armor.flags = armor.flags.or(F_ANTI_PENETRATIVE_ABLATIVE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        armor.rulesRefs = "86, IO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                .setISAdvancement(3100, 3114,  DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_ANTI_PENETRATIVE_ABLATION;

        return armor;
    }

    private static ArmorType createBallisticReinforced() {
        ArmorType misc = new ArmorType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BALLISTIC_REINFORCED);
        misc.setInternalName("IS " + misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.flags = misc.flags.or(F_BALLISTIC_REINFORCED).or(F_MECH_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.rulesRefs = "87, IO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                .setISAdvancement(3120, 3131, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        misc.armorType = T_ARMOR_BALLISTIC_REINFORCED;

        return misc;
    }

    // Separate IS/Clan standard aerospace armor, which provides different points per ton.
    private static ArmorType createISAeroSpaceArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_AEROSPACE);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_AEROSPACE, false));
        armor.flags = armor.flags.or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
                .or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_B)
                .setISAdvancement(2460, 2470, 2470).setISApproximate(true, false, false)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);

        armor.armorType = T_ARMOR_AEROSPACE;

        return armor;
    }

    private static ArmorType createClanAeroSpaceArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_AEROSPACE);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_AEROSPACE, true));
        armor.flags = armor.flags.or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
                .or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        armor.rulesRefs = "205, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_B)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 2470)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);

        armor.armorType = T_ARMOR_AEROSPACE;

        return armor;
    }

    private static ArmorType createISImpFerroAluminumArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_IMP);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_IMP, false));
        armor.addLookupName(armor.name);
        armor.addLookupName("ImprovedFerroAluminum");
        armor.bv = 0;
        armor.flags = armor.flags.or(F_CAPITAL_ARMOR).or(F_IMP_FERRO).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        armor.rulesRefs = "152, SO";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(2500, 2520, DATE_NONE, 2950, 3052)
                .setISApproximate(true, true, false, false, false)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_LC_FERRO_IMP;

        return armor;
    }

    private static ArmorType createClanImpFerroAluminumArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_IMP);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_IMP, true));
        armor.flags = armor.flags.or(F_CAPITAL_ARMOR).or(F_IMP_FERRO).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        armor.rulesRefs = "152, SO";
        armor.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(2500, 2520).setClanApproximate(false, true)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_LC_FERRO_IMP;

        return armor;
    }

    private static ArmorType createISFerroCarbideArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_CARBIDE);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_CARBIDE, false));
        armor.addLookupName("Ferro-Carbide");
        armor.tonnage = 0;
        armor.flags = armor.flags.or(F_CAPITAL_ARMOR).or(F_FERRO_CARBIDE).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        armor.rulesRefs = "152, SO";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(2550, 2570, DATE_NONE, 2950, 3055).setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC, F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_LC_FERRO_CARBIDE;

        return armor;
    }

    private static ArmorType createClanFerroCarbideArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_CARBIDE);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_CARBIDE, true));
        armor.flags = armor.flags.or(F_CAPITAL_ARMOR).or(F_FERRO_CARBIDE).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        armor.rulesRefs = "152, SO";
        armor.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(2550, 2570, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_LC_FERRO_CARBIDE;

        return armor;
    }

    private static ArmorType createISLamellorFerroCarbideArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE, false));
        armor.addLookupName(armor.name);
        armor.flags = armor.flags.or(F_CAPITAL_ARMOR).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        armor.rulesRefs = "152, SO";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(2600, 2615, DATE_NONE, 2950, 3055).setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2600, 2615, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_FW, F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE;

        return armor;
    }

    private static ArmorType createClanLamellorFerroCarbideArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE, true));
        armor.flags = armor.flags.or(F_CAPITAL_ARMOR).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        armor.rulesRefs = "152, SO";
        armor.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                .setClanAdvancement(2600, 2615, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE;

        return armor;
    }

    private static ArmorType createPrimitiveLCAerospaceArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PRIMITIVE_AERO);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PRIMITIVE_AERO, false));
        armor.shortName = "Primitive";
        armor.tonnage = TONNAGE_VARIABLE;
        armor.industrial = true;
        armor.flags = armor.flags.or(F_PRIMITIVE_ARMOR).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
                .or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        armor.rulesRefs = "125, IO";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_C, RATING_B, RATING_B)
                .setISAdvancement(DATE_ES, 2300, 2315).setISApproximate(false, true, true)
                .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.STANDARD);

        armor.armorType = T_ARMOR_PRIMITIVE_AERO;

        return armor;
    }

    private static ArmorType createElectricDischargeArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(T_ARMOR_EDP);
        armor.setInternalName("CLEDPArmor");
        armor.addLookupName(EquipmentType.getArmorTypeName(T_ARMOR_EDP, true));
        armor.addLookupName(EquipmentType.getArmorTypeName(T_ARMOR_EDP));
        armor.shortName = "EDP";
        armor.flags = armor.flags.or(F_PROTOMECH_EQUIPMENT).or(F_ELECTRIC_DISCHARGE_ARMOR);
        armor.bv = 32;
        String[] modes = { "not charging", "charging" };
        armor.setModes(modes);
        armor.rulesRefs = "64, IO";
        armor.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                .setClanAdvancement(3071, DATE_NONE, DATE_NONE, 3085, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CFM)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        armor.armorType = T_ARMOR_EDP;

        return armor;
    }

    private static ArmorType createISBAStandardArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STANDARD);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STANDARD, false));
        armor.addLookupName("IS BA Standard (Basic)");
        armor.shortName = "Standard (Basic)";
        armor.criticals = 0;
        armor.flags = armor.flags.or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(2680, DATE_NONE, 3054, DATE_NONE, 3050)
                .setISApproximate(true, false, true, false, false)
                .setPrototypeFactions(F_TH)
                .setReintroductionFactions(F_FS, F_LC, F_DC).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);

        armor.armorType = T_ARMOR_BA_STANDARD;

        return armor;
    }

    private static ArmorType createClanBAStandardArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STANDARD);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STANDARD, true));
        armor.addLookupName("Clan BA Standard (Basic)");
        armor.shortName = "Standard (Basic)";
        armor.criticals = 0;
        armor.flags = armor.flags.or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setClanAdvancement(DATE_NONE, 2868, 3054).setClanApproximate(true, true, false)
                .setProductionFactions(F_CWF).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);

        armor.armorType = T_ARMOR_BA_STANDARD;

        return armor;
    }

    private static ArmorType createISBAStandardPrototypeArmor() {
        ArmorType misc = new ArmorType();

        misc.name = BattleArmor.STANDARD_PROTOTYPE;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STANDARD_PROTOTYPE));
        misc.addLookupName("IS BA Standard (Prototype)");
        misc.shortName = "Standard (Prototype)";
        misc.criticals = 4;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT);
        misc.techAdvancement.setTechBase(TECH_BASE_IS)
                .setISAdvancement(3050, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH, F_FS, F_LC, F_DC)
                .setProductionFactions(F_TH, F_FS, F_LC, F_DC).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        misc.armorType = T_ARMOR_BA_STANDARD_PROTOTYPE;

        return misc;
    }

    private static ArmorType createISBAAdvancedArmor() {
        ArmorType armor = new ArmorType();

        armor.name = BattleArmor.ADVANCED_ARMOR;
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STANDARD_ADVANCED));
        armor.addLookupName("IS BA Advanced");
        armor.shortName = "Advanced";
        armor.criticals = 5;
        armor.flags = armor.flags.or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(DATE_NONE, 3057, 3060)
                .setProductionFactions(F_FW).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);

        armor.armorType = T_ARMOR_BA_STANDARD_ADVANCED;

        return armor;
    }

    private static ArmorType createClanBAFireResistantArmor() {
        ArmorType armor = new ArmorType();

        armor.name = BattleArmor.FIRE_RESISTANT;
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_FIRE_RESIST));
        armor.addLookupName("Clan BA Fire Resistant");
        armor.shortName = "Fire Resistant";
        armor.criticals = 5;
        armor.flags = armor.flags.or(F_FIRE_RESISTANT).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "253, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3052, 3058, 3065)
                .setClanApproximate(true, false, false).setPrototypeFactions(F_CFM)
                .setProductionFactions(F_CFM).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);

        armor.armorType = T_ARMOR_BA_FIRE_RESIST;

        return armor;
    }

    private static ArmorType createISBAStealthPrototype() {
        ArmorType armor = new ArmorType();

        armor.name = BattleArmor.STEALTH_PROTOTYPE;
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE));
        armor.addLookupName("IS BA Stealth (Prototype)");
        armor.addLookupName("Clan BA Stealth (Prototype)");
        armor.shortName = "Stealth (Prototype)";
        armor.criticals = 4;
        armor.flags = armor.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3050, 3052, 3054, 3055, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X);

        armor.armorType = T_ARMOR_BA_STEALTH_PROTOTYPE;

        return armor;
    }

    private static ArmorType createISBABasicStealth() {
        ArmorType armor = new ArmorType();

        armor.name = BattleArmor.BASIC_STEALTH_ARMOR;
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH_BASIC, false));
        armor.addLookupName("IS BA Stealth (Basic)");
        armor.shortName = "Stealth (Basic)";
        armor.criticals = 3;
        armor.flags = armor.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(2700, 2710, 3054, 2770, 3052)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_E, RATING_D);

        armor.armorType = T_ARMOR_BA_STEALTH_BASIC;

        return armor;
    }

    private static ArmorType createClanBABasicStealth() {
        ArmorType armor = new ArmorType();

        armor.name = BattleArmor.BASIC_STEALTH_ARMOR;
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH_BASIC, true));
        armor.addLookupName("Clan BA Stealth (Basic)");
        armor.shortName = "Stealth (Basic)";
        armor.criticals = 3;
        armor.flags = armor.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3054).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_E, RATING_D);

        armor.armorType = T_ARMOR_BA_STEALTH_BASIC;

        return armor;
    }

    private static ArmorType createISBAStandardStealth() {
        ArmorType armor = new ArmorType();

        armor.name = BattleArmor.STANDARD_STEALTH_ARMOR;
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH, false));
        armor.addLookupName("IS BA Stealth (Standard)");
        armor.addLookupName("IS BA Stealth");
        armor.shortName = "Stealth";
        armor.criticals = 4;
        armor.flags = armor.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(2710, 2720, 3055, 2770, 3053)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_X, RATING_E, RATING_D);

        armor.armorType = T_ARMOR_BA_STEALTH;

        return armor;
    }

    private static ArmorType createClanBAStandardStealth() {
        ArmorType armor = new ArmorType();

        armor.name = BattleArmor.STANDARD_STEALTH_ARMOR;
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH, true));
        armor.addLookupName("Clan BA Stealth (Standard)");
        armor.addLookupName("Clan BA Stealth");
        armor.shortName = "Stealth";
        armor.criticals = 4;
        armor.flags = armor.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3055).setPrototypeFactions(F_TH)
                .setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_X, RATING_E, RATING_D);

        armor.armorType = T_ARMOR_BA_STEALTH;

        return armor;
    }

    private static ArmorType createISBAImprovedStealth() {
        ArmorType armor = new ArmorType();

        armor.name = BattleArmor.IMPROVED_STEALTH_ARMOR;
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH_IMP, false));
        armor.addLookupName("IS BA Stealth (Improved)");
        armor.shortName = "Stealth (Improved)";
        armor.criticals = 5;
        armor.flags = armor.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";

        armor.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3055, 3057, 3059)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FW, F_WB)
                .setProductionFactions(F_FW, F_WB).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);

        armor.armorType = T_ARMOR_BA_STEALTH_IMP;

        return armor;
    }

    private static ArmorType createClanBAImprovedStealth() {
        ArmorType armor = new ArmorType();

        armor.name = BattleArmor.IMPROVED_STEALTH_ARMOR;
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH_IMP, true));
        armor.addLookupName("Clan BA Stealth (Improved)");
        armor.shortName = "Stealth (Improved)";
        armor.criticals = 5;
        armor.flags = armor.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "252, TM";

        armor.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setClanAdvancement(DATE_NONE, 3058, 3059)
                .setProductionFactions(F_CSR).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);

        armor.armorType = T_ARMOR_BA_STEALTH_IMP;

        return armor;
    }

    private static ArmorType createISBAMimeticCamo() {
        ArmorType armor = new ArmorType();

        armor.name = BattleArmor.MIMETIC_ARMOR;
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_MIMETIC));
        armor.addLookupName("IS BA Mimetic");
        armor.shortName = "Mimetic";
        armor.criticals = 7;
        armor.flags = armor.flags.or(F_STEALTH).or(F_VISUAL_CAMO).or(F_BA_EQUIPMENT);
        armor.rulesRefs = "253, TM";
        armor.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3058, 3061, 3065, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CS, F_WB)
                .setProductionFactions(F_WB).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);

        armor.armorType = T_ARMOR_BA_MIMETIC;

        return armor;
    }

    private static ArmorType createISBAReactiveArmor() {
        ArmorType armor = new ArmorType();
        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REACTIVE);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REACTIVE, false));
        armor.addLookupName("IS BA Reactive (Blazer)");
        armor.addLookupName("IS BA Reactive");
        armor.shortName = "Reactive";
        armor.criticals = 7;
        armor.flags = armor.flags.or(F_BA_EQUIPMENT).or(F_REACTIVE);
        armor.rulesRefs = "93, TO: AU&E";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TECH_BASE_IS)
                .setISAdvancement(3075, 3110, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setProductionFactions(F_RS).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_BA_REACTIVE;

        return armor;
    }

    private static ArmorType createClanBAReactiveArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REACTIVE);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REACTIVE, true));
        armor.addLookupName("Clan BA Reactive (Blazer)");
        armor.addLookupName("Clan BA Reactive");
        armor.shortName = "Reactive";
        armor.criticals = 7;
        armor.flags = armor.flags.or(F_BA_EQUIPMENT).or(F_REACTIVE);
        armor.rulesRefs = "94, TO: AU&E";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setClanAdvancement(3075, 3110, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CSF)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_BA_REACTIVE;

        return armor;
    }

    private static ArmorType createISBAReflectiveArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REFLECTIVE);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REFLECTIVE, false));
        armor.addLookupName("IS BA Laser Reflective (Reflec/Glazed)");
        armor.addLookupName("IS BA Reflective");
        armor.shortName = "Reflective";
        armor.criticals = 7;
        armor.flags = armor.flags.or(F_BA_EQUIPMENT).or(F_REFLECTIVE);
        armor.rulesRefs = "93, TO: AU&E";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TECH_BASE_IS)
                .setISAdvancement(3074, 3110, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setProductionFactions(F_DC).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_BA_REFLECTIVE;

        return armor;
    }

    private static ArmorType createClanBAReflectiveArmor() {
        ArmorType armor = new ArmorType();

        armor.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REFLECTIVE);
        armor.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REFLECTIVE, true));
        armor.addLookupName("Clan BA Laser Reflective (Reflec/Glazed)");
        armor.addLookupName("Clan BA Reflective");
        armor.shortName = "Reflective";
        armor.criticals = 7;
        armor.flags = armor.flags.or(F_BA_EQUIPMENT).or(F_REFLECTIVE);
        armor.rulesRefs = "93, TO: AU&E";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        armor.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setClanAdvancement(3074, 3110, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CNC).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        armor.armorType = T_ARMOR_BA_REFLECTIVE;

        return armor;
    }
}
