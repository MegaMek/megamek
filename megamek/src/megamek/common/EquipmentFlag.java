/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
 *
 */

package megamek.common;

public enum EquipmentFlag {
    F_HEAT_SINK (0),
    F_DOUBLE_HEAT_SINK (1),
    F_JUMP_JET (2),
    F_CASE (3),
    F_MASC (4),
    F_TSM (5),
    F_LASER_HEAT_SINK (6),
    F_C3S (7),
    F_C3I (8),
    F_ARTEMIS (9),
    F_TARGCOMP (10),
    F_ANGEL_ECM (11),
    F_BOARDING_CLAW (12),
    F_VACUUM_PROTECTION (13),
    F_MAGNET_CLAW (14),
    F_FIRE_RESISTANT (15),
    F_STEALTH (16),
    F_MINE (17),
    F_TOOLS (18),
    F_MAGNETIC_CLAMP (19),
    F_PARAFOIL (20),
    F_FERRO_FIBROUS (21),
    F_ENDO_STEEL (22),
    F_AP_POD (23),
    F_SEARCHLIGHT (24),
    F_CLUB (25),
    F_HAND_WEAPON (26),
    F_COWL (27),
    F_JUMP_BOOSTER (28),
    F_HARJEL (29),
    F_UMU (30),
    F_BA_VTOL (31),
    F_SPIKES (32),
    F_COMMUNICATIONS (33),
    F_PPC_CAPACITOR (34),
    F_REFLECTIVE (35),
    F_REACTIVE (36),
    F_CASEII (37),
    F_LIFTHOIST (38),
    F_ENVIRONMENTAL_SEALING (39),
    F_ARMORED_CHASSIS (40),
    F_TRACTOR_MODIFICATION (41),
    F_ACTUATOR_ENHANCEMENT_SYSTEM (42),
    F_ECM (43),
    F_BAP (44),
    F_MODULAR_ARMOR (45),
    F_TALON (46),
    F_VISUAL_CAMO (47),
    F_APOLLO (48),
    F_INDUSTRIAL_TSM (49),
    F_NULLSIG (50),
    F_VOIDSIG (51),
    F_CHAMELEON_SHIELD (52),
    F_VIBROCLAW (53),
    F_SINGLE_HEX_ECM (54),
    F_EJECTION_SEAT (55),
    F_SALVAGE_ARM (56),
    F_PARTIAL_WING (57),
    F_FERRO_LAMELLOR (58),
    F_ARTEMIS_V (59),
    // TODO: Implement me, so far only construction data
    F_TRACKS (60),
    // TODO: Implement me, so far only construction data
    F_MASS (61),
    // TODO: Implement me, so far only construction data
    F_CARGO (62),
    // TODO: Implement me, so far only construction data
    F_DUMPER (63),
    // TODO: Implement me, so far only construction data
    F_MASH (64),
    F_BA_EQUIPMENT (65),
    F_MEK_EQUIPMENT (66),
    F_TANK_EQUIPMENT (67),
    F_FIGHTER_EQUIPMENT (68),
    F_SUPPORT_TANK_EQUIPMENT (69),
    F_PROTOMEK_EQUIPMENT (70),

    // Moved the unit types to the top of the list.
    F_ARMORED_GLOVE (71),
    F_BASIC_MANIPULATOR (72),
    F_BATTLE_CLAW (73),
    F_AP_MOUNT (74),
    F_MAST_MOUNT (75),
    F_FUEL (76),
    F_BLUE_SHIELD (77),
    F_BASIC_FIRECONTROL (78),
    F_ADVANCED_FIRECONTROL (79),
    F_ENDO_COMPOSITE (80),
    F_LASER_INSULATOR (81),
    F_LIQUID_CARGO (82),
    F_WATCHDOG (83),
    F_EW_EQUIPMENT (84),
    F_CCM (85),
    F_HITCH (86),
    F_FLOTATION_HULL (87),
    F_LIMITED_AMPHIBIOUS (88),
    F_FULLY_AMPHIBIOUS (89),
    F_DUNE_BUGGY (90),
    F_SHOULDER_TURRET (91),
    F_HEAD_TURRET (92),
    F_QUAD_TURRET (93),
    F_SPACE_ADAPTATION (94),
    F_CUTTING_TORCH (95),
    F_OFF_ROAD (96),
    F_C3SBS (97),
    F_VTOL_EQUIPMENT (98),
    F_NAVAL_C3 (99),
    F_MINESWEEPER (100),
    F_MOBILE_HPG (101),
    F_FIELD_KITCHEN (102),
    F_MOBILE_FIELD_BASE (103),
    // TODO: add game rules for the following imagers/radars, construction data
    // only
    F_HIRES_IMAGER (104),
    F_HYPERSPECTRAL_IMAGER (105),
    F_INFRARED_IMAGER (106),
    F_LOOKDOWN_RADAR (107),

    F_COMMAND_CONSOLE (108),
    F_VSTOL_CHASSIS (109),
    F_STOL_CHASSIS (110),
    F_SPONSON_TURRET (111),
    F_ARMORED_MOTIVE_SYSTEM (112),
    F_CHASSIS_MODIFICATION (113),
    F_CHAFF_POD (114),
    F_DRONE_CARRIER_CONTROL (115),
    F_VARIABLE_SIZE (116),
    F_BA_MISSION_EQUIPMENT (117),
    F_JET_BOOSTER (118),
    F_SENSOR_DISPENSER (119),
    F_DRONE_OPERATING_SYSTEM (120),
    F_RECON_CAMERA (121),
    F_COMBAT_VEHICLE_ESCAPE_POD (122),
    F_DETACHABLE_WEAPON_PACK (123),
    F_HEAT_SENSOR (124),
    F_EXTENDED_LIFESUPPORT (125),
    F_SPRAYER (126),
    F_ELECTRIC_DISCHARGE_ARMOR (127),
    F_MECHANICAL_JUMP_BOOSTER (128),
    F_TRAILER_MODIFICATION (129),
    F_LARGE_COMM_SCANNER_SUITE (130),
    F_SMALL_COMM_SCANNER_SUITE (131),
    F_LIGHT_BRIDGE_LAYER (132),
    F_MEDIUM_BRIDGE_LAYER (133),
    F_HEAVY_BRIDGE_LAYER (134),
    F_BA_SEARCHLIGHT (135),
    F_BOOBY_TRAP (136),
    F_SPLITABLE (137),
    F_REFUELING_DROGUE (138),
    F_BULLDOZER (139),
    F_EXTERNAL_STORES_HARDPOINT (140),
    F_COMPACT_HEAT_SINK (141),
    F_MANIPULATOR (142),
    F_CARGOLIFTER (143),
    F_PINTLE_TURRET (144),
    F_IS_DOUBLE_HEAT_SINK_PROTOTYPE (145),
    F_NAVAL_TUG_ADAPTOR (146),
    F_AMPHIBIOUS (147),
    F_PROP (148),
    F_ULTRA_LIGHT (149),
    F_SPACE_MINE_DISPENSER (150),
    F_VEHICLE_MINE_DISPENSER (151),
    F_LIGHT_FERRO (152),
    F_HEAVY_FERRO (153),
    F_FERRO_FIBROUS_PROTO (154),
    F_REINFORCED (155),
    F_COMPOSITE (156),
    F_INDUSTRIAL_STRUCTURE (157),
    F_ENDO_STEEL_PROTO (158),
    F_INDUSTRIAL_ARMOR (159),
    F_HEAVY_INDUSTRIAL_ARMOR (160),
    F_PRIMITIVE_ARMOR (161),
    F_HARDENED_ARMOR (162),
    F_COMMERCIAL_ARMOR (163),
    F_C3EM (164),
    F_ANTI_PENETRATIVE_ABLATIVE (165),
    F_HEAT_DISSIPATING (166),
    F_IMPACT_RESISTANT (167),
    F_BALLISTIC_REINFORCED (168),
    F_HARJEL_II (169),
    F_HARJEL_III (170),
    F_RADICAL_HEATSINK (171),
    F_BA_MANIPULATOR (172),
    F_NOVA (173),
    F_BOMB_BAY (174),
    F_LIGHT_FLUID_SUCTION_SYSTEM (175),
    F_MONOCYCLE (176),
    F_BICYCLE (177),
    F_CONVERTIBLE (178),
    F_BATTLEMEK_NIU (179),
    F_SNOWMOBILE (180),
    F_LADDER (181),
    F_LIFEBOAT (182),
    F_FLUID_SUCTION_SYSTEM (183),
    F_HYDROFOIL (184),
    F_SUBMERSIBLE (185),

    // Flag for BattleArmor Modular Equipment Adaptor
    F_BA_MEA (186),

    // Flag for Infantry Equipment
    F_INF_EQUIPMENT (187),
    F_SCM (188),
    F_VIRAL_JAMMER_HOMING (189),
    F_VIRAL_JAMMER_DECOY (190),
    F_DRONE_CONTROL_CONSOLE (191),
    F_RISC_LASER_PULSE_MODULE (192),
    F_REMOTE_DRONE_COMMAND_CONSOLE (193),
    F_EMERGENCY_COOLANT_SYSTEM (194),
    F_BADC (195),
    F_REUSABLE (196),

    F_BLOODHOUND (197),
    F_ARMOR_KIT (198),

    // Flags for Large Craft Systems
    F_STORAGE_BATTERY (199),
    F_LIGHT_SAIL (200),

    // Prototype Stuff
    F_ARTEMIS_PROTO (201),
    F_CASEP (202),

    F_VEEDC (203),
    F_SC_EQUIPMENT (204),
    F_DS_EQUIPMENT (205),
    F_JS_EQUIPMENT (206),
    F_WS_EQUIPMENT (207),
    F_SS_EQUIPMENT (208),
    F_CAPITAL_ARMOR (209),
    F_FERRO_CARBIDE (210),
    F_IMP_FERRO (211),
    // Not usable by small support vehicles
    F_HEAVY_EQUIPMENT (212),
    // Drone Equipment for Large Craft
    F_SRCS (213),
    F_SASRCS (214),
    F_CASPAR (215),
    F_CASPARII (216),
    F_ATAC (217),
    F_DTAC (218),
    F_SDS_DESTRUCT (219),
    F_SDS_JAMMER (220),
    F_LF_STORAGE_BATTERY (221),
    F_PROTOMEK_MELEE (222),
    F_EXTERNAL_POWER_PICKUP (223),
    F_RAM_PLATE (224),
    F_PROTOTYPE (225),
    // Fortify Equipment
    F_TRENCH_CAPABLE (226),
    F_SUPPORT_VEE_BAR_ARMOR (227),

    //////////////////////////

    // ammo flags
    F_MG_AMMO(0),
    F_BATTLEARMOR_ONLY(1), // only used by BA squads
    F_PROTOMEK_ONLY(2), // only used by ProtoMeks
    F_HOTLOAD(3), // Ammo can be hotloaded

    // BA can't jump or make anti-mek until dumped
    F_ENCUMBERING(4),

    F_MML_LRM(5), // LRM type
    F_AR10_WHITE_SHARK(6), // White shark type
    F_AR10_KILLER_WHALE(7), // Killer Whale type
    F_AR10_BARRACUDA(8), // barracuda type
    F_NUCLEAR(9), // Nuclear missile
    F_SANTA_ANNA(14), // Santa Anna Missile
    F_PEACEMAKER(15), // Peacemaker Missile
    F_TELE_MISSILE(10), // Tele-Missile
    F_CAP_MISSILE(11), // Other Capital-Missile
    F_SPACE_BOMB_AMMO(12), // can be used to space bomb

    // can be used to ground bomb
    F_GROUND_BOMB(13),
    F_MML_SRM(14), // SRM type

// Numbers 14-15 out of order. See nuclear missiles, above

    // For tag, rl pods, missiles and the like
    F_OTHER_BOMB(16),

    // Used by MHQ for loading ammo bins
    F_CRUISE_MISSILE_AMMO(17),

    // Used by MHQ for loading ammo bins
    F_SCREEN(18),

    // Used for Internal Bomb Bay bombs; to differentiate them from
    F_INTERNAL_BOMB(19),

    //////////////////////////


    // marks any weapon affected by a targeting computer
    F_DIRECT_FIRE(0),
    F_FLAMER(1),
    // Glaze armor
    F_LASER(2),
    F_PPC(3),
    // for weapons that target Automatically (AMS)
    F_AUTO_TARGET(4),
    // can not start fires
    F_NO_FIRES(5),
    // must be only weapon attacking
    F_SOLO_ATTACK(7),
    F_VGL(8),
    // MGL for rapid fire setup
    F_MG(9),
    // Inferno weapon
    F_INFERNO(10),
    // Infantry caliber weapon, damage based on # of men shooting
    F_INFANTRY(11),
    // use missile rules for # of hits
    F_MISSILE_HITS(13),
    F_ONESHOT(14),
    F_ARTILLERY(15),

    // for Gunnery/Ballistic
    F_BALLISTIC(16),
    // for Gunnery/Energy
    F_ENERGY(17),
    // for Gunnery/Missile
    F_MISSILE(18),

    // fires
    F_PLASMA(19),
    F_INCENDIARY_NEEDLES(20),

    // War of 3039 prototypes
    F_PROTOTYPE_WEAPON(21),
    // Variable heat, heat is listed in dice, not points
    F_HEATASDICE(22),
    // AMS
    F_AMS(23),

    // may only target Infantry
    F_INFANTRY_ONLY(25),

    F_TAG(26),
    // C3 Master with Target Acquisition gear
    F_C3M(27),

    // Plasma Rifle
    F_PLASMA_MFUK(28),
    // fire Extinguisher
    F_EXTINGUISHER(29),
    F_PULSE(30),
    // Full Damage vs. Infantry
    F_BURST_FIRE(31),
    // Machine Gun Array
    F_MGA(32),
    F_NO_AIM(33),
    F_BOMBAST_LASER(34),
    F_CRUISE_MISSILE(35),
    F_B_POD(36),
    F_TASER(37),

    // Anti-ship missiles
    F_ANTI_SHIP(38),
    F_SPACE_BOMB(39),
    F_M_POD(40),
    F_DIVE_BOMB(41),
    F_ALT_BOMB(42),

    // Currently only used by MML
    F_BA_WEAPON(43),
    F_MEK_WEAPON(44),
    F_AERO_WEAPON(45),
    F_PROTO_WEAPON(46),
    F_TANK_WEAPON(47),

    F_INFANTRY_ATTACK(48),
    F_INF_BURST(49),
    F_INF_AA(50),
    F_INF_NONPENETRATING(51),
    F_INF_POINT_BLANK(52),
    F_INF_SUPPORT(53),
    F_INF_ENCUMBER(54),
    F_INF_ARCHAIC(55),

    // TODO Add game rules IO pg 84
    F_INF_CLIMBINGCLAWS(63),

    // C3 Master Booster System
    F_C3MBS(56),

    // Naval Mass Drivers
    F_MASS_DRIVER(58),

    F_CWS(59),

    F_MEK_MORTAR(60),

    // Weapon required to make a bomb type function
    F_BOMB_WEAPON(61),

    F_BA_INDIVIDUAL(62),
// Next one's out of order. See F_INF_CLIMBINGCLAWS

    // AMS and Point Defense Bays - Have to work differently from code using the
// F_AMS flag
    F_PDBAY(64),
    F_AMSBAY(65),

    // Thunderbolt and similar large missiles, for use with AMS resolution
    F_LARGEMISSILE(66),

    // Hyper-Laser
    F_HYPER(67),

    // Fusillade works like a one-shot weapon but has a second round.
    F_DOUBLE_ONESHOT(68),
    // ER flamers do half damage in heat mode,
    F_ER_FLAMER(69), // Nice
    /** Missile weapon that can be linked to an Artemis fire control system */
    F_ARTEMIS_COMPATIBLE(70),

    /**
     * This flag is used by mortar-type weapons that allow indirect fire without a
     * spotter and/or with LOS.
     */
    F_MORTARTYPE_INDIRECT(71),

    // Used for TSEMP Weapons.
    F_TSEMP(57),
    F_REPEATING(72);


    private final int flagPosition;

    EquipmentFlag(int flagPosition) {
        this.flagPosition = flagPosition;
    }

    public int getFlagPosition() {
        return flagPosition;
    }

    public EquipmentFlagBitSet asEquipmentFlagSet() {
        EquipmentFlagBitSet bitSet = new EquipmentFlagBitSet();
        bitSet.set(flagPosition);
        return bitSet;
    }

    public EquipmentFlagBitSet not() {
        var bitSet = asEquipmentFlagSet();
        bitSet.flip(0, EquipmentFlag.values().length);
        return bitSet;
    }
}
