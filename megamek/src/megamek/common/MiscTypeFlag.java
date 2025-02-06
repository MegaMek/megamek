/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common;

/**
 * Set of flags that can be used to determine special equipment properties
 * and behaviors.
 * Every type of equipment has its own flag.
 * @author Luana Coppio
 */
public enum MiscTypeFlag implements EquipmentFlag {
    // Heat sink family
    F_HEAT_SINK,
    F_DOUBLE_HEAT_SINK,
    F_LASER_HEAT_SINK,
    F_COMPACT_HEAT_SINK,
    F_IS_DOUBLE_HEAT_SINK_PROTOTYPE,
    F_RADICAL_HEATSINK,
    F_LASER_INSULATOR,
    F_HEAT_DISSIPATING,

    // Limit which type of unit can install the equipment
    F_BA_EQUIPMENT,
    F_MEK_EQUIPMENT,
    F_TANK_EQUIPMENT,
    F_FIGHTER_EQUIPMENT,
    F_SUPPORT_TANK_EQUIPMENT,
    F_PROTOMEK_EQUIPMENT,

    F_JUMP_JET,
    F_JUMP_BOOSTER,
    F_MECHANICAL_JUMP_BOOSTER,

    F_CASE,
    F_CASEII,
    F_CASEP,

    F_MASC,
    F_TSM,
    F_INDUSTRIAL_TSM,

    F_C3S,
    F_C3I,
    F_C3EM,
    F_C3SBS,
    F_NAVAL_C3,

    F_ARTEMIS,
    F_TARGCOMP,
    F_ARTEMIS_V,

    F_BATTLEMEK_NIU,

    F_SEARCHLIGHT,
    F_BA_SEARCHLIGHT,

    F_ANGEL_ECM,
    F_ECM,
    F_WATCHDOG,
    F_EW_EQUIPMENT,
    F_CCM,
    F_SINGLE_HEX_ECM,

    F_MAGNETIC_CLAMP,
    F_BOARDING_CLAW,
    F_MAGNET_CLAW,
    F_BA_MANIPULATOR,
    F_ARMORED_GLOVE,
    F_BASIC_MANIPULATOR,
    F_BATTLE_CLAW,

    F_PROTOMEK_MELEE,

    F_BOOBY_TRAP,
    F_MINE,
    F_SPACE_MINE_DISPENSER,
    F_VEHICLE_MINE_DISPENSER,
    F_MINESWEEPER,

    F_AP_POD,
    F_CLUB,
    F_HAND_WEAPON,
    F_COWL,
    F_BA_VTOL,
    F_UMU,
    F_TALON,
    F_SPIKES,
    F_COMMUNICATIONS,

    F_PPC_CAPACITOR,

    F_ENVIRONMENTAL_SEALING,
    F_ARMORED_CHASSIS,
    F_TRACTOR_MODIFICATION,
    F_ACTUATOR_ENHANCEMENT_SYSTEM,

    F_BAP,
    F_VISUAL_CAMO,
    F_APOLLO,

    F_NULLSIG,
    F_VOIDSIG,
    F_LIFTHOIST,
    F_CHAMELEON_SHIELD,
    F_VIBROCLAW,

    F_EJECTION_SEAT,
    F_SALVAGE_ARM,
    F_PARTIAL_WING,

    F_TRACKS, // TODO: Implement me, so far only construction data
    F_MASS, // TODO: Implement me, so far only construction data
    F_CARGO, // TODO: Implement me, so far only construction data
    F_DUMPER, // TODO: Implement me, so far only construction data
    F_MASH, // TODO: Implement me, so far only construction data


    F_AP_MOUNT,
    F_MAST_MOUNT,
    F_FUEL,
    F_BLUE_SHIELD,
    F_BASIC_FIRECONTROL,
    F_ADVANCED_FIRECONTROL,
    F_LIQUID_CARGO,
    F_HITCH,
    F_FLOTATION_HULL,
    F_LIMITED_AMPHIBIOUS,
    F_FULLY_AMPHIBIOUS,
    F_DUNE_BUGGY,
    F_SPACE_ADAPTATION,
    F_VACUUM_PROTECTION,
    F_CUTTING_TORCH,
    F_OFF_ROAD,
    F_VTOL_EQUIPMENT,


    F_MOBILE_HPG,
    F_MOBILE_FIELD_BASE,
    F_COMMAND_CONSOLE,
    F_VSTOL_CHASSIS,
    F_STOL_CHASSIS,
    F_ARMORED_MOTIVE_SYSTEM,
    F_CHASSIS_MODIFICATION,
    F_CHAFF_POD,
    F_DRONE_CARRIER_CONTROL,
    F_BA_MISSION_EQUIPMENT,
    F_JET_BOOSTER,
    F_SENSOR_DISPENSER,
    F_DRONE_OPERATING_SYSTEM,
    F_RECON_CAMERA,
    F_COMBAT_VEHICLE_ESCAPE_POD,
    F_DETACHABLE_WEAPON_PACK,
    F_SPRAYER,
    F_EXTENDED_LIFESUPPORT,

    F_SHOULDER_TURRET,
    F_HEAD_TURRET,
    F_QUAD_TURRET,
    F_PINTLE_TURRET,
    F_SPONSON_TURRET,

    F_TRAILER_MODIFICATION,
    F_LARGE_COMM_SCANNER_SUITE,
    F_SMALL_COMM_SCANNER_SUITE,

    F_LIGHT_BRIDGE_LAYER,
    F_MEDIUM_BRIDGE_LAYER,
    F_HEAVY_BRIDGE_LAYER,

    F_REFUELING_DROGUE,
    F_BULLDOZER,
    F_EXTERNAL_STORES_HARDPOINT,
    F_MANIPULATOR,
    F_CARGOLIFTER,
    F_NAVAL_TUG_ADAPTOR,
    F_AMPHIBIOUS,
    F_PROP,
    F_PARAFOIL,
    F_LIGHT_SAIL,
    F_ULTRA_LIGHT,


    F_ELECTRIC_DISCHARGE_ARMOR,
    F_ENDO_STEEL,
    F_FERRO_FIBROUS,
    F_LIGHT_FERRO,
    F_HEAVY_FERRO,
    F_FERRO_FIBROUS_PROTO,
    F_REINFORCED,
    F_COMPOSITE,
    F_INDUSTRIAL_STRUCTURE,
    F_ENDO_STEEL_PROTO,
    F_INDUSTRIAL_ARMOR,
    F_HEAVY_INDUSTRIAL_ARMOR,
    F_PRIMITIVE_ARMOR,
    F_HARDENED_ARMOR,
    F_COMMERCIAL_ARMOR,
    F_IMPACT_RESISTANT,
    F_BALLISTIC_REINFORCED,
    F_ANTI_PENETRATIVE_ABLATIVE,
    F_FIRE_RESISTANT,
    F_STEALTH,
    F_MODULAR_ARMOR,
    F_FERRO_LAMELLOR,
    F_ENDO_COMPOSITE,
    F_REFLECTIVE,
    F_REACTIVE,

    F_HARJEL,
    F_HARJEL_II,
    F_HARJEL_III,

    F_NOVA,
    F_BOMB_BAY,

    F_MONOCYCLE,
    F_BICYCLE,
    F_CONVERTIBLE,

    F_SNOWMOBILE,

    F_LADDER,
    F_LIFEBOAT,
    F_HYDROFOIL,
    F_SUBMERSIBLE,
    F_HEAT_SENSOR,


    F_BA_MEA, // Flag for BattleArmor Modular Equipment Adaptor
    F_INF_EQUIPMENT, // Flag for Infantry Equipment

    F_SCM,

    F_VIRAL_JAMMER_HOMING,
    F_VIRAL_JAMMER_DECOY,
    F_DRONE_CONTROL_CONSOLE,
    F_RISC_LASER_PULSE_MODULE,
    F_REMOTE_DRONE_COMMAND_CONSOLE,
    F_EMERGENCY_COOLANT_SYSTEM,
    F_BADC,
    F_REUSABLE,
    F_BLOODHOUND,
    F_ARMOR_KIT,

    F_ARTEMIS_PROTO, // Prototype Stuff
    F_STORAGE_BATTERY, // Flags for Large Craft Systems

    F_VEEDC,
    F_SC_EQUIPMENT,
    F_DS_EQUIPMENT,
    F_JS_EQUIPMENT,
    F_WS_EQUIPMENT,
    F_SS_EQUIPMENT,
    F_CAPITAL_ARMOR,

    F_FERRO_CARBIDE,
    F_IMP_FERRO,
    F_HEAVY_EQUIPMENT, // Not usable by small support vehicles
    F_SRCS, // Drone Equipment for Large Craft
    F_SASRCS,

    F_CASPAR,
    F_CASPARII,

    F_ATAC,
    F_DTAC,
    F_SDS_DESTRUCT,
    F_SDS_JAMMER,
    F_LF_STORAGE_BATTERY,

    F_TRENCH_CAPABLE, // Fortify Equipment
    F_EXTERNAL_POWER_PICKUP,
    F_PROTOTYPE,
    F_TOOLS,
    F_FLUID_SUCTION_SYSTEM,
    F_LIGHT_FLUID_SUCTION_SYSTEM,
    F_RAM_PLATE,
    F_SUPPORT_VEE_BAR_ARMOR,
    F_FIELD_KITCHEN,


    F_SPLITABLE, // Marks the equipament as something that can be split betwen multiple crit slots
    F_VARIABLE_SIZE, // marks the equipment as being something with a size that can be changed at will

    // Satellite Equipment
    F_HIRES_IMAGER, // TODO: add game rules for the following imagers/radars, construction data only
    F_HYPERSPECTRAL_IMAGER, // TODO: add game rules for the following imagers/radars, construction data only
    F_INFRARED_IMAGER, // TODO: add game rules for the following imagers/radars, construction data only
    F_LOOKDOWN_RADAR, // TODO: add game rules for the following imagers/radars, construction data only
}
