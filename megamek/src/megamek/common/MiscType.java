/*
 * * MegaMek - Copyright (C) 2000-2005 Ben Mazur
 * (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

import java.math.BigInteger;
import java.text.NumberFormat;

import megamek.common.weapons.ppc.CLERPPC;
import megamek.common.weapons.ppc.ISERPPC;
import megamek.common.weapons.ppc.ISHeavyPPC;
import megamek.common.weapons.ppc.ISLightPPC;
import megamek.common.weapons.ppc.ISPPC;
import megamek.common.weapons.ppc.ISSnubNosePPC;

/**
 * @author Ben
 * @since April 2, 2002, 12:15 PM
 */
public class MiscType extends EquipmentType {
  
    // equipment flags (okay, like every type of equipment has its own flag)
    public static final BigInteger F_HEAT_SINK = BigInteger.valueOf(1).shiftLeft(0);
    public static final BigInteger F_DOUBLE_HEAT_SINK = BigInteger.valueOf(1).shiftLeft(1);
    public static final BigInteger F_JUMP_JET = BigInteger.valueOf(1).shiftLeft(2);
    public static final BigInteger F_CASE = BigInteger.valueOf(1).shiftLeft(3);
    public static final BigInteger F_MASC = BigInteger.valueOf(1).shiftLeft(4);
    public static final BigInteger F_TSM = BigInteger.valueOf(1).shiftLeft(5);
    public static final BigInteger F_LASER_HEAT_SINK = BigInteger.valueOf(1).shiftLeft(6);
    public static final BigInteger F_C3S = BigInteger.valueOf(1).shiftLeft(7);
    public static final BigInteger F_C3I = BigInteger.valueOf(1).shiftLeft(8);
    public static final BigInteger F_ARTEMIS = BigInteger.valueOf(1).shiftLeft(9);
    public static final BigInteger F_TARGCOMP = BigInteger.valueOf(1).shiftLeft(10);
    public static final BigInteger F_ANGEL_ECM = BigInteger.valueOf(1).shiftLeft(11);
    public static final BigInteger F_BOARDING_CLAW = BigInteger.valueOf(1).shiftLeft(12);
    public static final BigInteger F_VACUUM_PROTECTION = BigInteger.valueOf(1).shiftLeft(13);
    public static final BigInteger F_MAGNET_CLAW = BigInteger.valueOf(1).shiftLeft(14);
    public static final BigInteger F_FIRE_RESISTANT = BigInteger.valueOf(1).shiftLeft(15);
    public static final BigInteger F_STEALTH = BigInteger.valueOf(1).shiftLeft(16);
    public static final BigInteger F_MINE = BigInteger.valueOf(1).shiftLeft(17);
    public static final BigInteger F_TOOLS = BigInteger.valueOf(1).shiftLeft(18);
    public static final BigInteger F_MAGNETIC_CLAMP = BigInteger.valueOf(1).shiftLeft(19);
    public static final BigInteger F_PARAFOIL = BigInteger.valueOf(1).shiftLeft(20);
    public static final BigInteger F_FERRO_FIBROUS = BigInteger.valueOf(1).shiftLeft(21);
    public static final BigInteger F_ENDO_STEEL = BigInteger.valueOf(1).shiftLeft(22);
    public static final BigInteger F_AP_POD = BigInteger.valueOf(1).shiftLeft(23);
    public static final BigInteger F_SEARCHLIGHT = BigInteger.valueOf(1).shiftLeft(24);
    public static final BigInteger F_CLUB = BigInteger.valueOf(1).shiftLeft(25);
    public static final BigInteger F_HAND_WEAPON = BigInteger.valueOf(1).shiftLeft(26);
    public static final BigInteger F_COWL = BigInteger.valueOf(1).shiftLeft(27);
    public static final BigInteger F_JUMP_BOOSTER = BigInteger.valueOf(1).shiftLeft(28);
    public static final BigInteger F_HARJEL = BigInteger.valueOf(1).shiftLeft(29);
    public static final BigInteger F_UMU = BigInteger.valueOf(1).shiftLeft(30);
    public static final BigInteger F_BA_VTOL = BigInteger.valueOf(1).shiftLeft(31);
    public static final BigInteger F_SPIKES = BigInteger.valueOf(1).shiftLeft(32);
    public static final BigInteger F_COMMUNICATIONS = BigInteger.valueOf(1).shiftLeft(33);
    public static final BigInteger F_PPC_CAPACITOR = BigInteger.valueOf(1).shiftLeft(34);
    public static final BigInteger F_REFLECTIVE = BigInteger.valueOf(1).shiftLeft(35);
    public static final BigInteger F_REACTIVE = BigInteger.valueOf(1).shiftLeft(36);
    public static final BigInteger F_CASEII = BigInteger.valueOf(1).shiftLeft(37);
    public static final BigInteger F_LIFTHOIST = BigInteger.valueOf(1).shiftLeft(38);
    public static final BigInteger F_ENVIRONMENTAL_SEALING = BigInteger.valueOf(1).shiftLeft(39);
    public static final BigInteger F_ARMORED_CHASSIS = BigInteger.valueOf(1).shiftLeft(40);
    public static final BigInteger F_TRACTOR_MODIFICATION = BigInteger.valueOf(1).shiftLeft(41);
    public static final BigInteger F_ACTUATOR_ENHANCEMENT_SYSTEM = BigInteger.valueOf(1).shiftLeft(42);
    public static final BigInteger F_ECM = BigInteger.valueOf(1).shiftLeft(43);
    public static final BigInteger F_BAP = BigInteger.valueOf(1).shiftLeft(44);
    public static final BigInteger F_MODULAR_ARMOR = BigInteger.valueOf(1).shiftLeft(45);
    public static final BigInteger F_TALON = BigInteger.valueOf(1).shiftLeft(46);
    public static final BigInteger F_VISUAL_CAMO = BigInteger.valueOf(1).shiftLeft(47);
    public static final BigInteger F_APOLLO = BigInteger.valueOf(1).shiftLeft(48);
    public static final BigInteger F_INDUSTRIAL_TSM = BigInteger.valueOf(1).shiftLeft(49);
    public static final BigInteger F_NULLSIG = BigInteger.valueOf(1).shiftLeft(50);
    public static final BigInteger F_VOIDSIG = BigInteger.valueOf(1).shiftLeft(51);
    public static final BigInteger F_CHAMELEON_SHIELD = BigInteger.valueOf(1).shiftLeft(52);
    public static final BigInteger F_VIBROCLAW = BigInteger.valueOf(1).shiftLeft(53);
    public static final BigInteger F_SINGLE_HEX_ECM = BigInteger.valueOf(1).shiftLeft(54);
    public static final BigInteger F_EJECTION_SEAT = BigInteger.valueOf(1).shiftLeft(55);
    public static final BigInteger F_SALVAGE_ARM = BigInteger.valueOf(1).shiftLeft(56);
    public static final BigInteger F_PARTIAL_WING = BigInteger.valueOf(1).shiftLeft(57);
    public static final BigInteger F_FERRO_LAMELLOR = BigInteger.valueOf(1).shiftLeft(58);
    public static final BigInteger F_ARTEMIS_V = BigInteger.valueOf(1).shiftLeft(59);
    // TODO: Implement me, so far only construction data
    public static final BigInteger F_TRACKS = BigInteger.valueOf(1).shiftLeft(60);
    // TODO: Implement me, so far only construction data
    public static final BigInteger F_MASS = BigInteger.valueOf(1).shiftLeft(61);
    // TODO: Implement me, so far only construction data
    public static final BigInteger F_CARGO = BigInteger.valueOf(1).shiftLeft(62);
    // TODO: Implement me, so far only construction data
    public static final BigInteger F_DUMPER = BigInteger.valueOf(1).shiftLeft(63);
    // TODO: Implement me, so far only construction data
    public static final BigInteger F_MASH = BigInteger.valueOf(1).shiftLeft(64);
    public static final BigInteger F_BA_EQUIPMENT = BigInteger.valueOf(1).shiftLeft(65);
    public static final BigInteger F_MECH_EQUIPMENT = BigInteger.valueOf(1).shiftLeft(66);
    public static final BigInteger F_TANK_EQUIPMENT = BigInteger.valueOf(1).shiftLeft(67);
    public static final BigInteger F_FIGHTER_EQUIPMENT = BigInteger.valueOf(1).shiftLeft(68);
    public static final BigInteger F_SUPPORT_TANK_EQUIPMENT = BigInteger.valueOf(1).shiftLeft(69);
    public static final BigInteger F_PROTOMECH_EQUIPMENT = BigInteger.valueOf(1).shiftLeft(70);

    //Moved the unit types to the top of the list.
    public static final BigInteger F_ARMORED_GLOVE = BigInteger.valueOf(1).shiftLeft(71);
    public static final BigInteger F_BASIC_MANIPULATOR = BigInteger.valueOf(1).shiftLeft(72);
    public static final BigInteger F_BATTLE_CLAW = BigInteger.valueOf(1).shiftLeft(73);
    public static final BigInteger F_AP_MOUNT = BigInteger.valueOf(1).shiftLeft(74);
    public static final BigInteger F_MAST_MOUNT = BigInteger.valueOf(1).shiftLeft(75);
    public static final BigInteger F_FUEL = BigInteger.valueOf(1).shiftLeft(76);
    public static final BigInteger F_BLUE_SHIELD = BigInteger.valueOf(1).shiftLeft(77);
    public static final BigInteger F_BASIC_FIRECONTROL = BigInteger.valueOf(1).shiftLeft(78);
    public static final BigInteger F_ADVANCED_FIRECONTROL = BigInteger.valueOf(1).shiftLeft(79);
    public static final BigInteger F_ENDO_COMPOSITE = BigInteger.valueOf(1).shiftLeft(80);
    public static final BigInteger F_LASER_INSULATOR = BigInteger.valueOf(1).shiftLeft(81);
    public static final BigInteger F_LIQUID_CARGO = BigInteger.valueOf(1).shiftLeft(82);
    public static final BigInteger F_WATCHDOG = BigInteger.valueOf(1).shiftLeft(83);
    public static final BigInteger F_EW_EQUIPMENT = BigInteger.valueOf(1).shiftLeft(84);
    public static final BigInteger F_CCM = BigInteger.valueOf(1).shiftLeft(85);
    public static final BigInteger F_HITCH = BigInteger.valueOf(1).shiftLeft(86);
    public static final BigInteger F_FLOTATION_HULL = BigInteger.valueOf(1).shiftLeft(87);
    public static final BigInteger F_LIMITED_AMPHIBIOUS = BigInteger.valueOf(1).shiftLeft(88);
    public static final BigInteger F_FULLY_AMPHIBIOUS = BigInteger.valueOf(1).shiftLeft(89);
    public static final BigInteger F_DUNE_BUGGY = BigInteger.valueOf(1).shiftLeft(90);
    public static final BigInteger F_SHOULDER_TURRET = BigInteger.valueOf(1).shiftLeft(91);
    public static final BigInteger F_HEAD_TURRET = BigInteger.valueOf(1).shiftLeft(92);
    public static final BigInteger F_QUAD_TURRET = BigInteger.valueOf(1).shiftLeft(93);
    public static final BigInteger F_SPACE_ADAPTATION = BigInteger.valueOf(1).shiftLeft(94);
    public static final BigInteger F_CUTTING_TORCH = BigInteger.valueOf(1).shiftLeft(95);
    public static final BigInteger F_OFF_ROAD = BigInteger.valueOf(1).shiftLeft(96);
    public static final BigInteger F_C3SBS = BigInteger.valueOf(1).shiftLeft(97);
    public static final BigInteger F_VTOL_EQUIPMENT = BigInteger.valueOf(1).shiftLeft(98);
    public static final BigInteger F_NAVAL_C3 = BigInteger.valueOf(1).shiftLeft(99);
    public static final BigInteger F_MINESWEEPER = BigInteger.valueOf(1).shiftLeft(100);
    public static final BigInteger F_MOBILE_HPG = BigInteger.valueOf(1).shiftLeft(101);
    public static final BigInteger F_FIELD_KITCHEN = BigInteger.valueOf(1).shiftLeft(102);
    public static final BigInteger F_MOBILE_FIELD_BASE = BigInteger.valueOf(1).shiftLeft(103);
    // TODO: add game rules for the following imagers/radars, construction data
    // only
    public static final BigInteger F_HIRES_IMAGER = BigInteger.valueOf(1).shiftLeft(104);
    public static final BigInteger F_HYPERSPECTRAL_IMAGER = BigInteger.valueOf(1).shiftLeft(105);
    public static final BigInteger F_INFRARED_IMAGER = BigInteger.valueOf(1).shiftLeft(106);
    public static final BigInteger F_LOOKDOWN_RADAR = BigInteger.valueOf(1).shiftLeft(107);

    public static final BigInteger F_COMMAND_CONSOLE = BigInteger.valueOf(1).shiftLeft(108);
    public static final BigInteger F_VSTOL_CHASSIS = BigInteger.valueOf(1).shiftLeft(109);
    public static final BigInteger F_STOL_CHASSIS = BigInteger.valueOf(1).shiftLeft(110);
    public static final BigInteger F_SPONSON_TURRET = BigInteger.valueOf(1).shiftLeft(111);
    public static final BigInteger F_ARMORED_MOTIVE_SYSTEM = BigInteger.valueOf(1).shiftLeft(112);
    public static final BigInteger F_CHASSIS_MODIFICATION = BigInteger.valueOf(1).shiftLeft(113);
    public static final BigInteger F_CHAFF_POD = BigInteger.valueOf(1).shiftLeft(114);
    public static final BigInteger F_DRONE_CARRIER_CONTROL = BigInteger.valueOf(1).shiftLeft(115);
    public static final BigInteger F_VARIABLE_SIZE = BigInteger.valueOf(1).shiftLeft(116);
    public static final BigInteger F_BA_MISSION_EQUIPMENT = BigInteger.valueOf(1).shiftLeft(117);
    public static final BigInteger F_JET_BOOSTER = BigInteger.valueOf(1).shiftLeft(118);
    public static final BigInteger F_SENSOR_DISPENSER = BigInteger.valueOf(1).shiftLeft(119);
    public static final BigInteger F_DRONE_OPERATING_SYSTEM = BigInteger.valueOf(1).shiftLeft(120);
    public static final BigInteger F_RECON_CAMERA = BigInteger.valueOf(1).shiftLeft(121);
    public static final BigInteger F_COMBAT_VEHICLE_ESCAPE_POD = BigInteger.valueOf(1).shiftLeft(122);
    public static final BigInteger F_DETACHABLE_WEAPON_PACK = BigInteger.valueOf(1).shiftLeft(123);
    public static final BigInteger F_HEAT_SENSOR = BigInteger.valueOf(1).shiftLeft(124);
    public static final BigInteger F_EXTENDED_LIFESUPPORT = BigInteger.valueOf(1).shiftLeft(125);
    public static final BigInteger F_SPRAYER = BigInteger.valueOf(1).shiftLeft(126);
    public static final BigInteger F_ELECTRIC_DISCHARGE_ARMOR = BigInteger.valueOf(1).shiftLeft(127);
    public static final BigInteger F_MECHANICAL_JUMP_BOOSTER = BigInteger.valueOf(1).shiftLeft(128);
    public static final BigInteger F_TRAILER_MODIFICATION = BigInteger.valueOf(1).shiftLeft(129);
    public static final BigInteger F_LARGE_COMM_SCANNER_SUITE = BigInteger.valueOf(1).shiftLeft(130);
    public static final BigInteger F_SMALL_COMM_SCANNER_SUITE = BigInteger.valueOf(1).shiftLeft(131);
    public static final BigInteger F_LIGHT_BRIDGE_LAYER = BigInteger.valueOf(1).shiftLeft(132);
    public static final BigInteger F_MEDIUM_BRIDGE_LAYER = BigInteger.valueOf(1).shiftLeft(133);
    public static final BigInteger F_HEAVY_BRIDGE_LAYER = BigInteger.valueOf(1).shiftLeft(134);
    public static final BigInteger F_BA_SEARCHLIGHT = BigInteger.valueOf(1).shiftLeft(135);
    public static final BigInteger F_BOOBY_TRAP = BigInteger.valueOf(1).shiftLeft(136);
    public static final BigInteger F_SPLITABLE = BigInteger.valueOf(1).shiftLeft(137);
    public static final BigInteger F_REFUELING_DROGUE = BigInteger.valueOf(1).shiftLeft(138);
    public static final BigInteger F_BULLDOZER = BigInteger.valueOf(1).shiftLeft(139);
    public static final BigInteger F_EXTERNAL_STORES_HARDPOINT = BigInteger.valueOf(1).shiftLeft(140);
    public static final BigInteger F_COMPACT_HEAT_SINK = BigInteger.valueOf(1).shiftLeft(141);
    public static final BigInteger F_MANIPULATOR = BigInteger.valueOf(1).shiftLeft(142);
    public static final BigInteger F_CARGOLIFTER = BigInteger.valueOf(1).shiftLeft(143);
    public static final BigInteger F_PINTLE_TURRET = BigInteger.valueOf(1).shiftLeft(144);
    public static final BigInteger F_IS_DOUBLE_HEAT_SINK_PROTOTYPE = BigInteger.valueOf(1).shiftLeft(145);
    public static final BigInteger F_NAVAL_TUG_ADAPTOR = BigInteger.valueOf(1).shiftLeft(146);
    public static final BigInteger F_AMPHIBIOUS = BigInteger.valueOf(1).shiftLeft(147);
    public static final BigInteger F_PROP = BigInteger.valueOf(1).shiftLeft(148);
    public static final BigInteger F_ULTRA_LIGHT = BigInteger.valueOf(1).shiftLeft(149);
    public static final BigInteger F_SPACE_MINE_DISPENSER = BigInteger.valueOf(1).shiftLeft(150);
    public static final BigInteger F_VEHICLE_MINE_DISPENSER = BigInteger.valueOf(1).shiftLeft(151);
    public static final BigInteger F_LIGHT_FERRO = BigInteger.valueOf(1).shiftLeft(152);
    public static final BigInteger F_HEAVY_FERRO = BigInteger.valueOf(1).shiftLeft(153);
    public static final BigInteger F_FERRO_FIBROUS_PROTO = BigInteger.valueOf(1).shiftLeft(154);
    public static final BigInteger F_REINFORCED = BigInteger.valueOf(1).shiftLeft(155);
    public static final BigInteger F_COMPOSITE = BigInteger.valueOf(1).shiftLeft(156);
    public static final BigInteger F_INDUSTRIAL_STRUCTURE = BigInteger.valueOf(1).shiftLeft(157);
    public static final BigInteger F_ENDO_STEEL_PROTO = BigInteger.valueOf(1).shiftLeft(158);
    public static final BigInteger F_INDUSTRIAL_ARMOR = BigInteger.valueOf(1).shiftLeft(159);
    public static final BigInteger F_HEAVY_INDUSTRIAL_ARMOR = BigInteger.valueOf(1).shiftLeft(160);
    public static final BigInteger F_PRIMITIVE_ARMOR = BigInteger.valueOf(1).shiftLeft(161);
    public static final BigInteger F_HARDENED_ARMOR = BigInteger.valueOf(1).shiftLeft(162);
    public static final BigInteger F_COMMERCIAL_ARMOR = BigInteger.valueOf(1).shiftLeft(163);
    public static final BigInteger F_C3EM = BigInteger.valueOf(1).shiftLeft(164);
    public static final BigInteger F_ANTI_PENETRATIVE_ABLATIVE = BigInteger.valueOf(1).shiftLeft(165);
    public static final BigInteger F_HEAT_DISSIPATING = BigInteger.valueOf(1).shiftLeft(166);
    public static final BigInteger F_IMPACT_RESISTANT = BigInteger.valueOf(1).shiftLeft(167);
    public static final BigInteger F_BALLISTIC_REINFORCED = BigInteger.valueOf(1).shiftLeft(168);
    public static final BigInteger F_HARJEL_II = BigInteger.valueOf(1).shiftLeft(169);
    public static final BigInteger F_HARJEL_III = BigInteger.valueOf(1).shiftLeft(170);
    public static final BigInteger F_RADICAL_HEATSINK = BigInteger.valueOf(1).shiftLeft(171);
    public static final BigInteger F_BA_MANIPULATOR = BigInteger.valueOf(1).shiftLeft(172);
    public static final BigInteger F_NOVA = BigInteger.valueOf(1).shiftLeft(173);
    public static final BigInteger F_BOMB_BAY = BigInteger.valueOf(1).shiftLeft(174);
    public static final BigInteger F_LIGHT_FLUID_SUCTION_SYSTEM = BigInteger.valueOf(1).shiftLeft(175);
    public static final BigInteger F_MONOCYCLE = BigInteger.valueOf(1).shiftLeft(176);
    public static final BigInteger F_BICYCLE = BigInteger.valueOf(1).shiftLeft(177);
    public static final BigInteger F_CONVERTIBLE = BigInteger.valueOf(1).shiftLeft(178);
    public static final BigInteger F_BATTLEMECH_NIU = BigInteger.valueOf(1).shiftLeft(179);
    public static final BigInteger F_SNOWMOBILE = BigInteger.valueOf(1).shiftLeft(180);
    public static final BigInteger F_LADDER = BigInteger.valueOf(1).shiftLeft(181);
    public static final BigInteger F_LIFEBOAT = BigInteger.valueOf(1).shiftLeft(182);
    public static final BigInteger F_FLUID_SUCTION_SYSTEM = BigInteger.valueOf(1).shiftLeft(183);
    public static final BigInteger F_HYDROFOIL = BigInteger.valueOf(1).shiftLeft(184);
    public static final BigInteger F_SUBMERSIBLE = BigInteger.valueOf(1).shiftLeft(185);

    // Flag for BattleArmor Modular Equipment Adaptor
    public static final BigInteger F_BA_MEA = BigInteger.valueOf(1).shiftLeft(186);

    // Flag for Infantry Equipment
    public static final BigInteger F_INF_EQUIPMENT = BigInteger.valueOf(1).shiftLeft(187);
    public static final BigInteger F_SCM = BigInteger.valueOf(1).shiftLeft(188);
    public static final BigInteger F_VIRAL_JAMMER_HOMING = BigInteger.valueOf(1).shiftLeft(189);
    public static final BigInteger F_VIRAL_JAMMER_DECOY = BigInteger.valueOf(1).shiftLeft(190);
    public static final BigInteger F_DRONE_CONTROL_CONSOLE = BigInteger.valueOf(1).shiftLeft(191);
    public static final BigInteger F_RISC_LASER_PULSE_MODULE = BigInteger.valueOf(1).shiftLeft(192);
    public static final BigInteger F_REMOTE_DRONE_COMMAND_CONSOLE = BigInteger.valueOf(1).shiftLeft(193);
    public static final BigInteger F_EMERGENCY_COOLANT_SYSTEM = BigInteger.valueOf(1).shiftLeft(194);
    public static final BigInteger F_BADC = BigInteger.valueOf(1).shiftLeft(195);
    public static final BigInteger F_REUSABLE = BigInteger.valueOf(1).shiftLeft(196);

    public static final BigInteger F_BLOODHOUND = BigInteger.valueOf(1).shiftLeft(197);
    public static final BigInteger F_ARMOR_KIT = BigInteger.valueOf(1).shiftLeft(198);

    // Flags for Large Craft Systems
    public static final BigInteger F_STORAGE_BATTERY = BigInteger.valueOf(1).shiftLeft(199);
    public static final BigInteger F_LIGHT_SAIL = BigInteger.valueOf(1).shiftLeft(200);

    // Prototype Stuff
    public static final BigInteger F_ARTEMIS_PROTO = BigInteger.valueOf(1).shiftLeft(201);
    public static final BigInteger F_CASEP = BigInteger.valueOf(1).shiftLeft(202);
    
    public static final BigInteger F_VEEDC = BigInteger.valueOf(1).shiftLeft(203);
    public static final BigInteger F_SC_EQUIPMENT = BigInteger.valueOf(1).shiftLeft(204);
    public static final BigInteger F_DS_EQUIPMENT = BigInteger.valueOf(1).shiftLeft(205);
    public static final BigInteger F_JS_EQUIPMENT = BigInteger.valueOf(1).shiftLeft(206);
    public static final BigInteger F_WS_EQUIPMENT = BigInteger.valueOf(1).shiftLeft(207);
    public static final BigInteger F_SS_EQUIPMENT = BigInteger.valueOf(1).shiftLeft(208);
    public static final BigInteger F_CAPITAL_ARMOR = BigInteger.valueOf(1).shiftLeft(209);
    public static final BigInteger F_FERRO_CARBIDE = BigInteger.valueOf(1).shiftLeft(210);
    public static final BigInteger F_IMP_FERRO = BigInteger.valueOf(1).shiftLeft(211);
    // Not usable by small support vehicles
    public static final BigInteger F_HEAVY_EQUIPMENT = BigInteger.valueOf(1).shiftLeft(212);
    // Drone Equipment for Large Craft
    public static final BigInteger F_SRCS = BigInteger.valueOf(1).shiftLeft(213);
    public static final BigInteger F_SASRCS = BigInteger.valueOf(1).shiftLeft(214);
    public static final BigInteger F_CASPAR = BigInteger.valueOf(1).shiftLeft(215);
    public static final BigInteger F_CASPARII = BigInteger.valueOf(1).shiftLeft(216);
    public static final BigInteger F_ATAC = BigInteger.valueOf(1).shiftLeft(217);
    public static final BigInteger F_DTAC = BigInteger.valueOf(1).shiftLeft(218);
    public static final BigInteger F_SDS_DESTRUCT = BigInteger.valueOf(1).shiftLeft(219);
    public static final BigInteger F_SDS_JAMMER = BigInteger.valueOf(1).shiftLeft(220);
    public static final BigInteger F_LF_STORAGE_BATTERY = BigInteger.valueOf(1).shiftLeft(221);
    public static final BigInteger F_PROTOMECH_MELEE = BigInteger.valueOf(1).shiftLeft(222);
    public static final BigInteger F_EXTERNAL_POWER_PICKUP = BigInteger.valueOf(1).shiftLeft(223);
    public static final BigInteger F_RAM_PLATE = BigInteger.valueOf(1).shiftLeft(224);
    public static final BigInteger F_PROTOTYPE = BigInteger.valueOf(1).shiftLeft(225);

    // Secondary Flags for Physical Weapons
    public static final long S_CLUB = 1L << 0; // BMR - Indicates an Improvised Club
    public static final long S_TREE_CLUB = 1L << 1;// BMR
    public static final long S_HATCHET = 1L << 2; // BMR
    public static final long S_SWORD = 1L << 3; // BMR
    public static final long S_MACE_THB = 1L << 4; // Unused and Unsupported
    public static final long S_CLAW_THB = 1L << 5; // Unused and Unsupported
    public static final long S_MACE = 1L << 6;
    public static final long S_DUAL_SAW = 1L << 7;
    public static final long S_FLAIL = 1L << 8;
    public static final long S_PILE_DRIVER = 1L << 9;
    public static final long S_SHIELD_SMALL = 1L << 10;
    public static final long S_SHIELD_MEDIUM = 1L << 11;
    public static final long S_SHIELD_LARGE = 1L << 12;
    public static final long S_LANCE = 1L << 13;
    public static final long S_VIBRO_SMALL = 1L << 14;
    public static final long S_VIBRO_MEDIUM = 1L << 15;
    public static final long S_VIBRO_LARGE = 1L << 16;
    public static final long S_WRECKING_BALL = 1L << 17;
    public static final long S_BACKHOE = 1L << 18;
    public static final long S_COMBINE = 1L << 19; // TODO
    public static final long S_CHAINSAW = 1L << 20;
    public static final long S_ROCK_CUTTER = 1L << 21; // TODO
    public static final long S_BUZZSAW = 1L << 22; // Unbound;
    public static final long S_RETRACTABLE_BLADE = 1L << 23;
    public static final long S_CHAIN_WHIP = 1L << 24;
    public static final long S_SPOT_WELDER = 1L << 25; // TODO: add game rules
    public static final long S_MINING_DRILL = 1L << 26; // Miniatures
    
    // ProtoMek physical weapons
    public static final long S_PROTOMECH_WEAPON = 1L << 0;
    public static final long S_PROTO_QMS = 1L << 1;

    public static final String S_ACTIVE_SHIELD = "Active";
    public static final String S_PASSIVE_SHIELD = "Passive";
    public static final String S_NO_SHIELD = "None";

    public static final String S_HARJEL_II_2F0R = "2F/0R";
    public static final String S_HARJEL_II_1F1R = "1F/1R";
    public static final String S_HARJEL_II_0F2R = "0F/2R";

    public static final String S_HARJEL_III_4F0R = "4F/0R";
    public static final String S_HARJEL_III_3F1R = "3F/1R";
    public static final String S_HARJEL_III_2F2R = "2F/2R";
    public static final String S_HARJEL_III_1F3R = "1F/3R";
    public static final String S_HARJEL_III_0F4R = "0F/4R";

    // Secondary damage for hand weapons.
    // These are differentiated from Physical Weapons using the F_CLUB flag
    // because the following weapons are treated as a punch attack, while
    // the above weapons are treated as club or hatchet attacks.
    // these are subtypes of F_HAND_WEAPON
    public static final long S_CLAW = 1L << 0; // Solaris 7
    // Rulebook; TODO

    // Secondary flags for infantry tools
    public static final long S_VIBROSHOVEL = 1L << 0; // can fortify hexes
    public static final long S_DEMOLITION_CHARGE = 1L << 1; // can demolish
    // buildings
    public static final long S_BRIDGE_KIT = 1L << 2; // can build a bridge
    public static final long S_MINESWEEPER = 1L << 3; // can clear mines
    public static final long S_HEAVY_ARMOR = 1L << 4;

    // Secondary flags for MASC
    public static final long S_SUPERCHARGER = 1L << 0;
    // this kind of works like MASC for the double cruise MP, so we will make it
    // a subtype
    public static final long S_JETBOOSTER = 1L << 1;

    // Secondary flags for Jump Jets
    public static final long S_STANDARD = 1L << 0;
    public static final long S_IMPROVED = 1L << 1;
    public static final long S_PROTOTYPE = 1L << 2;
    
    // Secondary flag for robotic constrol systems; standard and improved borrow jj flags
    public static final long S_ELITE = 1L << 2;

    // Secondary flags for infantry armor kits
    public static final long S_DEST = 1L << 0;
    public static final long S_SNEAK_CAMO = 1L << 1;
    public static final long S_SNEAK_IR = 1L << 2;
    public static final long S_SNEAK_ECM = 1L << 3;
    public static final long S_ENCUMBERING = 1L << 4;
    public static final long S_SPACE_SUIT = 1L << 5;
    public static final long S_XCT_VACUUM   = 1L << 6;
    public static final long S_COLD_WEATHER = 1L << 7;
    public static final long S_HOT_WEATHER  = 1L << 8;
    // Unimplemented atmospheric conditions
    public static final long S_HAZARDOUS_LIQ = 1L << 9;
    public static final long S_TAINTED_ATMO = 1L << 10;
    public static final long S_TOXIC_ATMO   = 1L << 11;

    // Secondary flag for tracks
    public static final long S_QUADVEE_WHEELS = 1L;

    // Secondary flags for escape pods and lifeboats
    public static final long S_MARITIME_LIFEBOAT = 1L;
    public static final long S_MARITIME_ESCAPE_POD = 1L << 1;
    public static final long S_ATMOSPHERIC_LIFEBOAT = 1L << 2;

    // New stuff for shields
    protected int baseDamageAbsorptionRate = 0;
    protected int baseDamageCapacity = 0;
    protected int damageTaken = 0;

    private boolean industrial = false;

    // New stuff for infantry kits
    protected double damageDivisor = 1.0;

    /** Creates new MiscType */
    public MiscType() {
    }

    public int getBaseDamageAbsorptionRate() {
        return baseDamageAbsorptionRate;
    }

    public int getBaseDamageCapacity() {
        return baseDamageCapacity;
    }

    public boolean isShield() {
        return hasFlag(F_CLUB) && hasSubType(S_SHIELD_LARGE | S_SHIELD_MEDIUM | S_SHIELD_SMALL);
    }

    public boolean isVibroblade() {
        return hasFlag(F_CLUB) && hasSubType(S_VIBRO_LARGE | S_VIBRO_MEDIUM | S_VIBRO_SMALL);
    }

    public boolean isIndustrial() {
        return industrial;
    }

    public double getDamageDivisor() {
        return damageDivisor;
    }

    private String sizeSuffix(double size, boolean shortName) {
        if (hasFlag(F_VARIABLE_SIZE)) {
            if (hasFlag(MiscType.F_DRONE_CARRIER_CONTROL)
                    || hasFlag(MiscType.F_ATAC) || hasFlag(MiscType.F_DTAC)) {
                return String.format(" (%d %s)", (int) size, size > 1 ?
                        Messages.getString("MiscType.drones") :
                        Messages.getString("MiscType.drone"));
            } else if (hasFlag(MiscType.F_MASH)) {
                return String.format(" (%d %s)", (int) size, size > 1 ?
                        Messages.getString("MiscType.theaters") :
                        Messages.getString("MiscType.theater"));
            } else if (hasFlag(MiscType.F_LADDER)) {
                return String.format(" (%d m)", (int) size);
            } else if (hasFlag(MiscType.F_BA_MISSION_EQUIPMENT)) {
                return String.format(" (%d kg)", (int) size);
            } else if (shortName) {
                // Don't show decimal when not required
                return String.format(":%st", NumberFormat.getInstance().format(size));
            } else {
                return String.format(" (%s %s)", NumberFormat.getInstance().format(size), size == 1 ?
                        Messages.getString("MiscType.ton") :
                        Messages.getString("MiscType.tons"));
            }
        }
        return "";
    }

    @Override
    public boolean isVariableSize() {
        return hasFlag(F_VARIABLE_SIZE);
    }

    @Override
    public Double variableStepSize() {
        if (hasFlag(F_CARGO) || hasFlag(F_LIQUID_CARGO)) {
            return 0.5;
        }
        if (hasFlag(F_LADDER)) {
            return 20.0;
        }
        return super.variableStepSize();
    }

    @Override
    public String getName(double size) {
        return getName() + sizeSuffix(size, false);
    }

    @Override
    public String getShortName(double size) {
        return getShortName() + sizeSuffix(size, true);
    }

    @Override
    public String getDesc(double size) {
        return getDesc() + sizeSuffix(size, false);
    }

    @Override
    public double getTonnage(Entity entity, int location, double size) {
        return getTonnage(entity, location, size, RoundWeight.STANDARD);
    }

    @Override
    public double getTonnage(Entity entity, int location, double size, RoundWeight defaultRounding) {
        if ((tonnage != TONNAGE_VARIABLE) || (null == entity)) {
            return tonnage;
        }
        // check for known formulas
        if (hasFlag(F_JUMP_JET) || hasFlag(F_UMU)) {
            double multiplier = 1.0;
            if (hasSubType(S_IMPROVED)) {
                multiplier = 2.0;
            }
            if (hasSubType(S_PROTOTYPE) && (hasSubType(S_IMPROVED))) {
                multiplier = 1.0;
            }
            if (hasFlag(F_PROTOMECH_EQUIPMENT)) {
                if (entity.getWeight() < 6) {
                    return 0.05 * multiplier;
                } else if (entity.getWeight() < 10) {
                    return 0.1 * multiplier;
                } else {
                    return 0.15 * multiplier;
                }
            } else {
                if (entity.getWeight() <= 55.0) {
                    return 0.5 * multiplier;
                } else if (entity.getWeight() <= 85.0) {
                    return 1.0 * multiplier;
                } else {
                    return 2.0 * multiplier;
                }
            }
        } else if (hasFlag(F_PARTIAL_WING) && hasFlag(F_MECH_EQUIPMENT)) {
            if (isClan()) {
                return defaultRounding.round(entity.getWeight() * 0.05, entity);
            } else {
                return defaultRounding.round(entity.getWeight() * 0.07, entity);
            }
        } else if (hasFlag(F_PARTIAL_WING) && hasFlag(F_PROTOMECH_EQUIPMENT)) {
            return RoundWeight.nearestKg(entity.getWeight() / 5.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_HATCHET)) {
            return RoundWeight.nextTon(entity.getWeight() / 15.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_LANCE)) {
            return RoundWeight.nextTon(entity.getWeight() / 20.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_SWORD)) {
            return defaultRounding.round(entity.getWeight() / 20.0, entity);
        } else if (hasFlag(F_CLUB) && hasSubType(S_MACE)) {
            return RoundWeight.nextTon(entity.getWeight() / 10.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_RETRACTABLE_BLADE)) {
            return 0.5 + defaultRounding.round(entity.getWeight() / 20.0, entity);
        } else if (hasFlag(F_JET_BOOSTER)) {
            // CAW: Moved to before F_MASC to ensure this weight calc is used
            //      for VTOL Jet Boosters (which have both flags set)
            // pg 350, TO
            // 10% of engine weight rounded to the nearest half ton
            Engine e = entity.getEngine();
            if (null == e) {
                return 0;
            }
            return defaultRounding.round(e.getWeightEngine(entity, defaultRounding) / 10.0, entity);
        } else if (hasFlag(F_MASC)) {
            if (entity instanceof Protomech) {
                return RoundWeight.nearestKg(entity.getWeight() * 0.025);
                // Myomer Boosters for BA
            } else if (entity instanceof BattleArmor) {
                // Myomer boosters weight 0.250 tons, however this has to
                // be split across 3 instances, since it's spreadable equipment
                return (0.250 / 3);
            } else {
                if (hasSubType(S_SUPERCHARGER)) {
                    Engine e = entity.getEngine();
                    if (null == e) {
                        return 0;
                    }
                    // pg 344, TO
                    // 10% of engine weight
                    return defaultRounding.round(e.getWeightEngine(entity, defaultRounding) / 10.0, entity);
                }

                return Math.max(RoundWeight.nearestTon(entity.getWeight() * (isClan() ? 0.04 : 0.05)), 1);
            }
        } else if (hasFlag(F_QUAD_TURRET) || hasFlag(F_SHOULDER_TURRET) || hasFlag(F_HEAD_TURRET)) {
            // Turrets weight 10% of the weight of equipment in them, not counting Heat Sinks, Ammo and Armor
            int locationToCheck = location;
            if (hasFlag(F_HEAD_TURRET)) {
                locationToCheck = Mech.LOC_HEAD;
            }
            double equipmentWeight = 0;
            for (Mounted m : entity.getEquipment()) {
                if ((m.getLocation() == locationToCheck)
                        && (m.isMechTurretMounted())
                        && !(m.getType() instanceof AmmoType)
                        && !((m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_HEAT_SINK))
                        && (EquipmentType.getArmorType(m.getType()) == EquipmentType.T_ARMOR_UNKNOWN)) {
                    equipmentWeight += m.getTonnage();
                }
            }
            // round to half ton
            return defaultRounding.round(equipmentWeight / 10.0, entity);
        } else if (hasFlag(F_SPONSON_TURRET)) {
            // For omni vehicles, this should be set as part of the base chassis.
            if ((entity.isOmni() && (entity instanceof Tank)
                    && ((Tank) entity).getBaseChassisSponsonPintleWeight() >= 0)) {
                // Split between the two mounts
                return ((Tank) entity).getBaseChassisSponsonPintleWeight() /
                        entity.countWorkingMisc(MiscType.F_SPONSON_TURRET);
            }
            /* The sponson turret mechanism is equal to 10% of the weight of mounted equipment, rounded
             * up to the half ton. Since the turrets come in pairs, splitting the weight between them
             * may result in a quarter-ton result for a single turret, but the overall unit weight will
             * be correct.
             */
            double equipmentWeight = 0;
            for (Mounted m : entity.getEquipment()) {
                if (m.isSponsonTurretMounted()
                        && !(m.getType() instanceof AmmoType)
                        && !((m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_HEAT_SINK))
                        && (EquipmentType.getArmorType(m.getType()) == EquipmentType.T_ARMOR_UNKNOWN)) {
                    equipmentWeight += m.getTonnage();
                }
            }
            return defaultRounding.round(equipmentWeight / 10.0,
                    entity) / entity.countWorkingMisc(MiscType.F_SPONSON_TURRET);
        } else if (hasFlag(F_PINTLE_TURRET)) {
            // For omnivehicles the weight should be set as chassis fixed weight.
            // Split the weight evenly among the mounts to assure the total weight is correct.
            if ((entity.isOmni() && (entity instanceof Tank)
                    && ((Tank) entity).getBaseChassisSponsonPintleWeight() >= 0)) {
                return ((Tank) entity).getBaseChassisSponsonPintleWeight() /
                        entity.countWorkingMisc(MiscType.F_PINTLE_TURRET);
            }
            double weaponWeight = 0;
            // 5% of linked weapons' weight
            for (Mounted m : entity.getWeaponList()) {
                if (m.isPintleTurretMounted() && (m.getLocation() == location)) {
                    weaponWeight += m.getTonnage();
                }
            }
            return defaultRounding.round(weaponWeight / 20.0, entity);
        } else if (hasFlag(F_ARMORED_MOTIVE_SYSTEM)) {
            if (isClan()) {
                return defaultRounding.round(entity.getWeight() * 0.1, entity);
            } else {
                return defaultRounding.round(entity.getWeight() * 0.15, entity);
            }
        } else if (hasFlag(F_TARGCOMP)) {
            // based on tonnage of direct_fire weaponry
            double fTons = 0.0;
            for (Mounted m : entity.getWeaponList()) {
                WeaponType wt = (WeaponType) m.getType();
                if (wt.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    fTons += m.getTonnage();
                }
            }
            for (Mounted m : entity.getMisc()) {
                MiscType mt = (MiscType) m.getType();
                if (mt.hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    fTons += m.getTonnage();
                }
            }
            if (isClan()) {
                return RoundWeight.nextTon(fTons / 5.0);
            }
            return RoundWeight.nextTon(fTons / 4.0);
        } else if (hasFlag(MiscType.F_FERRO_FIBROUS) || hasFlag(MiscType.F_FERRO_FIBROUS_PROTO)) {
            double tons = 0.0;
            if (!entity.hasPatchworkArmor()) {
                if (entity.isClanArmor(1)) {
                    tons = entity.getTotalOArmor() / (16 * 1.2f);
                } else {
                    tons = entity.getTotalOArmor() / (16 * 1.12f);
                }
                tons = defaultRounding.round(tons, entity);
            } else {
                // TODO
            }
            return tons;
        } else if (hasFlag(MiscType.F_LIGHT_FERRO)) {
            double tons = 0;
            if (!entity.hasPatchworkArmor()) {
                tons = entity.getTotalOArmor() / (16 * 1.06f);
                tons = defaultRounding.round(tons, entity);
            } else {
                // TODO
            }
            return tons;
        } else if (hasFlag(MiscType.F_HEAVY_FERRO)) {
            double tons = 0;
            if (!entity.hasPatchworkArmor()) {
                tons = entity.getTotalOArmor() / (16 * 1.24f);
                tons = defaultRounding.round(tons, entity);
            } else {
                // TODO
            }
            return tons;
        } else if (hasFlag(MiscType.F_FERRO_LAMELLOR)) {
            double tons = 0;
            if (!entity.hasPatchworkArmor()) {
                tons = entity.getTotalOArmor() / (16 * 0.875f);
                tons = defaultRounding.round(tons, entity);
            } else {
                // TODO
            }
            return tons;
        } else if (hasFlag(MiscType.F_INDUSTRIAL_STRUCTURE)) {
            return defaultRounding.round(entity.getWeight() * 0.2, entity);
        } else if (hasFlag(F_ENDO_STEEL) || hasFlag(F_ENDO_STEEL_PROTO)
                || hasFlag(MiscType.F_COMPOSITE)) {
            return defaultRounding.round(entity.getWeight() * 0.05, entity);
        } else if (hasFlag(MiscType.F_REINFORCED)) {
            return defaultRounding.round(entity.getWeight() * 0.2, entity);
        } else if (hasFlag(F_ENDO_COMPOSITE)) {
            return defaultRounding.round(entity.getWeight() * 0.075, entity);
        } else if (hasFlag(F_DUNE_BUGGY)) {
            return entity.getWeight() / 10.0f;
        } else if (hasFlag(F_ENVIRONMENTAL_SEALING)) {
            if (entity.isSupportVehicle()) {
                // SV Chassis mods are accounted for in the structure weight
                return 0;
            } else {
                return RoundWeight.standard(entity.getWeight() / 10.0, entity);
            }

            // Per TO Pg 413 BA Mechanical Jump Boosters weight is 2 times jump
            // movement.
            // but Mechanical Boosters only add 1 Jump MP. So the weight
            // calculations
            // below are calculated according to that 1 Jump MP they give.
        } else if (hasFlag(F_MECHANICAL_JUMP_BOOSTER)) {
            if ((entity.getWeightClass() == EntityWeightClass.WEIGHT_ULTRA_LIGHT)
                    || (entity.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT)) {
                return 2.0 * .025;
            } else if (entity.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                return 2.0 * .05;
            } else if (entity.getWeightClass() == EntityWeightClass.WEIGHT_HEAVY) {
                return 2.0 * .125;
            } else if (entity.getWeightClass() == EntityWeightClass.WEIGHT_ASSAULT) {
                return 2.0 * .250;
            }
        } else if (hasFlag(F_JUMP_BOOSTER)) {
            // This is the 'Mech mechanical jump booster. The BA jump booster has the same flag but
            // has a fixed weight so doesn't get to this point.
            return defaultRounding.round((entity.getWeight() * entity.getOriginalJumpMP()) * 0.05, entity);
        } else if ((hasFlag(F_HAND_WEAPON) && hasSubType(S_CLAW)) || hasFlag(F_TALON)) {
            return RoundWeight.nextTon(entity.getWeight() / 15.0);
        } else if (hasFlag(F_ACTUATOR_ENHANCEMENT_SYSTEM)) {
            if (entity.entityIsQuad()) {
                return defaultRounding.round(entity.getWeight() / 50.0, entity);
            } else {
                return defaultRounding.round(entity.getWeight() / 35.0, entity);
            }
        } else if (hasFlag(F_TRACKS)) {
            if (hasSubType(S_QUADVEE_WHEELS)) {
                return defaultRounding.round(entity.getWeight() * 0.15, entity);
            } else {
                return defaultRounding.round(entity.getWeight() * 0.1, entity);
            }
        } else if (hasFlag(F_LIMITED_AMPHIBIOUS)) {
            return defaultRounding.round(entity.getWeight() / 25.0, entity);
        } else if (hasFlag(F_FULLY_AMPHIBIOUS)) {
            return defaultRounding.round(entity.getWeight() / 10.0, entity);
        } else if (hasFlag(F_BASIC_FIRECONTROL) || hasFlag(F_ADVANCED_FIRECONTROL)) {
            // Omni support vees have a fixed weight for the chassis, which may be
            // higher than what is required for the current configuration
            if (entity.getBaseChassisFireConWeight() > 0) {
                return entity.getBaseChassisFireConWeight();
            }
            // 5% of weapon weight
            double weaponWeight = 0;
            // Don't count weight of AMS or light (e.g. non-support infantry) weapons
            for (Mounted mount : entity.getWeaponList()) {
                if (!mount.getType().hasFlag(WeaponType.F_AMS)
                        && (!mount.getType().hasFlag(WeaponType.F_INFANTRY)
                        || mount.getType().hasFlag(WeaponType.F_INF_SUPPORT))) {
                    weaponWeight += mount.getTonnage();
                }
            }
            double weight = weaponWeight / (hasFlag(F_BASIC_FIRECONTROL) ? 20.0 : 10.0);
            return defaultRounding.round(weight, entity);
        } else if (hasFlag(F_BOOBY_TRAP)) {
            return defaultRounding.round(entity.getWeight() / 10.0, entity);
        } else if (hasFlag(F_DRONE_CARRIER_CONTROL)) {
            return 2 + size * 0.5;
        } else if (hasFlag(MiscType.F_DRONE_OPERATING_SYSTEM)) {
            // 10% of the weight, plus 0.5 tons for the extra sensors
            return (entity.getWeight() / 10f) + 0.5f;
        } else if (hasFlag(MiscType.F_NAVAL_TUG_ADAPTOR)) {
            return (100 + ((entity.getWeight() * 0.1)));
        } else if (hasFlag(MiscType.F_LIGHT_FLUID_SUCTION_SYSTEM)) {
            if (entity instanceof Tank) {
                return 0.015f;
            } else if (entity instanceof Mech) {
                return 0.5f;
            }
        } else if (hasFlag(MiscType.F_LIGHT_SAIL)) {
            return (entity.getWeight() / 10f);
        } else if (hasFlag(MiscType.F_LF_STORAGE_BATTERY)) {
            return (entity.getWeight() / 100f);
        } else if (hasFlag(MiscType.F_NAVAL_C3)) {
            return (entity.getWeight() * .01);
        } else if (hasFlag(MiscType.F_SRCS) || hasFlag(F_SASRCS)) {
            if (entity.getWeight() >= 10) {
                double pct = 0.05;
                if (entity.hasETypeFlag(Entity.ETYPE_DROPSHIP)
                        || entity.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
                    pct = 0.07;
                } else if (entity.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
                    pct = 0.1;
                }
                if (getSubType() == S_IMPROVED) {
                    pct += hasFlag(F_SASRCS) ? 0.01 : 0.02;
                } else if (getSubType() == S_ELITE) { // only shielded
                    pct += 0.03;
                }
                // JumpShip is based on non-drive weight and rounded to ton
                if (entity.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
                    return RoundWeight.nextTon((entity.getWeight() - ((Jumpship) entity).getJumpDriveWeight()) * pct);
                }
                return defaultRounding.round(entity.getWeight() * pct, entity);
            } else if (subType == S_IMPROVED) {
                return 1.0; // no weight for the base system for units < 10 tons, +1 for improved, elite not allowed
            } else {
                return 0;
            }
        } else if (hasFlag(MiscType.F_CASPAR)) {
            double pct = 0.05; // Value for small craft
            if (entity.hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
                pct = 0.04;
            } else if (entity.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
                pct = 0.08;
            } else if (entity.hasETypeFlag(Entity.ETYPE_WARSHIP)) {
                pct = 0.06;
            }
            if (getSubType() == S_IMPROVED) {
                // Add 2% for small craft, 4% for others
                if (pct == 0.05) {
                    pct = 0.07;
                } else {
                    pct += 0.04;
                }
            }
            if (entity.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
                return RoundWeight.nextTon(entity.getWeight() * pct);
            } else {
                return defaultRounding.round(entity.getWeight() * pct, entity);
            }
        } else if (hasFlag(MiscType.F_CASPARII)) {
            double pct = 0.06; // Value for small craft
            if (entity.hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
                pct = 0.08;
            } else if (entity.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
                pct = 0.1;
            } else if (entity.hasETypeFlag(Entity.ETYPE_WARSHIP)) {
                pct = 0.12;
            }
            if (getSubType() == S_IMPROVED) {
                // Add 2% for small craft, 4% for others
                if (pct == 0.06) {
                    pct = 0.08;
                } else {
                    pct += 0.04;
                }
            }
            if (entity.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
                return RoundWeight.nextTon(entity.getWeight() * pct);
            } else {
                return defaultRounding.round(entity.getWeight() * pct, entity);
            }
        } else if (hasFlag(MiscType.F_ATAC)) {
            double tWeight = defaultRounding.round(entity.getWeight() * 0.02, entity);
            return Math.min(tWeight, 50000) + size * 150.0;
        } else if (hasFlag(MiscType.F_DTAC)) {
            return defaultRounding.round(entity.getWeight() * 0.03, entity) + size * 150.0;
        } else if (hasFlag(MiscType.F_SDS_DESTRUCT)) {
            return Math.min(RoundWeight.nextTon(entity.getWeight() * 0.1), 10000);
        } else if (hasFlag(MiscType.F_MASH)) {
            // Each additional theater weighs 1.0 ton. The core component weighs 3.5, including
            // a theater.
            return 2.5 + size;
        }  else if (hasFlag(MiscType.F_MAGNETIC_CLAMP) && hasFlag(MiscType.F_PROTOMECH_EQUIPMENT)) {
            if (entity.getWeight() < 6) {
                return 0.25;
            } else if (entity.getWeight() < 10) {
                return 0.5;
            } else {
                return 1.0;
            }
        } else if (hasFlag(MiscType.F_FUEL)) {
            if (entity.hasEngine()) {
                return defaultRounding.round(entity.getEngine().getWeightEngine(entity) * 0.1, entity);
            } else {
                return 0.0;
            }
        } else if (hasFlag(F_CARGO) || hasFlag(F_LIQUID_CARGO) || hasFlag(F_COMMUNICATIONS)) {
            return defaultRounding.round(size, entity);
        } else if (hasFlag(F_LADDER)) {
            // 0.1 tons per 20 meters
            return RoundWeight.nearestKg(size / 200.0);
        } else if (hasFlag(F_BA_MISSION_EQUIPMENT)) {
            // Size is weight in kg
            return RoundWeight.nearestKg(size / 1000.0);
        } else if (hasFlag(MiscType.F_RAM_PLATE)) {
            return RoundWeight.nextTon(entity.getWeight() / 10.0);
        }
       // okay, I'm out of ideas
        return 1.0f;
    }

    @Override
    public double getCost(Entity entity, boolean isArmored, int loc, double size) {
        double costValue = cost;
        if (costValue == EquipmentType.COST_VARIABLE) {
            if (hasFlag(F_DRONE_CARRIER_CONTROL) || hasFlag(F_MASH)) {
                costValue = getTonnage(entity, loc, size) * 10000;
            } else if (hasFlag(F_ENVIRONMENTAL_SEALING) && (entity instanceof Mech)) {
                costValue = 225 * entity.getWeight();
            } else if (hasFlag(F_FLOTATION_HULL) || hasFlag(F_ENVIRONMENTAL_SEALING) || hasFlag(F_OFF_ROAD)) {
                costValue = 0;
            } else if (hasFlag(F_LIMITED_AMPHIBIOUS) || hasFlag((F_FULLY_AMPHIBIOUS))) {
                costValue = getTonnage(entity, loc) * 10000;
            } else if (hasFlag(F_DUNE_BUGGY)) {
                double totalTons = getTonnage(entity, loc);
                costValue = 10 * totalTons * totalTons;
            } else if (hasFlag(F_MASC) && hasFlag(F_BA_EQUIPMENT)) {
                costValue = entity.getRunMP() * 75000;
            } else if (hasFlag(F_HEAD_TURRET) || hasFlag(F_SHOULDER_TURRET) || hasFlag(F_QUAD_TURRET)) {
                costValue = getTonnage(entity, loc) * 10000;
            } else if (hasFlag(F_SPONSON_TURRET)) {
                costValue = getTonnage(entity, loc) * 4000;
            } else if (hasFlag(F_PINTLE_TURRET)) {
                costValue = getTonnage(entity, loc) * 1000;
            } else if (hasFlag(F_ARMORED_MOTIVE_SYSTEM)) {
                costValue = getTonnage(entity, loc) * 100000;
            } else if (hasFlag(F_DRONE_OPERATING_SYSTEM)) {
                costValue = (getTonnage(entity, loc) * 10000) + 5000;
            } else if (hasFlag(MiscType.F_MASC)) {
                if (entity instanceof Protomech) {
                    costValue = Math.round((entity.hasEngine() ? entity.getEngine().getRating() : 0) * 1000
                            * entity.getWeight() * 0.025f);
                } else if (entity instanceof BattleArmor) {
                    costValue = entity.getOriginalWalkMP() * 75000;
                } else if (hasSubType(MiscType.S_SUPERCHARGER) || hasSubType(MiscType.S_JETBOOSTER)) {
                    Engine e = entity.getEngine();
                    if (e == null) {
                        costValue = 0;
                    } else if (entity.isSupportVehicle()) {
                        costValue = e.getWeightEngine(entity) * 10000;
                    } else {
                        costValue = e.getRating() * 10000;
                    }
                } else {
                    int mascTonnage = 0;
                    if (getInternalName().equals(EquipmentTypeLookup.IS_MASC)) {
                        mascTonnage = (int) Math.round(entity.getWeight() / 20.0f);
                    } else if (getInternalName().equals(EquipmentTypeLookup.CLAN_MASC)) {
                        mascTonnage = (int) Math.round(entity.getWeight() / 25.0f);
                    }
                    costValue = (entity.hasEngine() ? entity.getEngine().getRating() : 0) * mascTonnage * 1000;
                }
            } else if (hasFlag(MiscType.F_TARGCOMP)) {
                int tCompTons = 0;
                double fTons = 0.0f;
                for (Mounted mo : entity.getWeaponList()) {
                    WeaponType wt = (WeaponType) mo.getType();
                    if (wt.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                        fTons += mo.getTonnage();
                    }
                }

                for (Mounted mo : entity.getMisc()) {
                    MiscType mt = (MiscType) mo.getType();
                    if (mt.hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                        fTons += mo.getTonnage();
                    }
                }
                if (getInternalName().equals("ISTargeting Computer")) {
                    tCompTons = (int) Math.ceil(fTons / 4.0f);
                } else if (getInternalName().equals("CLTargeting Computer")) {
                    tCompTons = (int) Math.ceil(fTons / 5.0f);
                }
                costValue = tCompTons * 10000;

            } else if (hasFlag(MiscType.F_BADC)) {
                int tDCCost = 0;
                if (getInternalName().equals("CLBADropChuteStd") || (getInternalName().equals("ISBADropChuteStd"))) {
                    tDCCost = 1000;
                } else if (getInternalName().equals("CLBADropChuteStealth")
                        || (getInternalName().equals("ISBADropChuteStealth"))) {
                    tDCCost = 5000;
                } else if (getInternalName().equals("CLBADropChuteCamo")
                        || (getInternalName().equals("ISBADropChuteCamo"))) {
                    tDCCost = 3000;
                }
                if (hasFlag(MiscType.F_REUSABLE)) {
                    tDCCost = tDCCost * 2;
                    costValue = tDCCost;
                }
            } else if (hasFlag(MiscType.F_CLUB) && hasSubType(MiscType.S_HATCHET)) {
                int hatchetTons = (int) Math.ceil(entity.getWeight() / 15.0);
                costValue = hatchetTons * 5000;
            } else if (hasFlag(MiscType.F_CLUB) && hasSubType(MiscType.S_SWORD)) {
                double swordTons = Math.ceil((entity.getWeight() / 20.0) * 2.0) / 2.0;
                costValue = swordTons * 10000;
            } else if (hasFlag(MiscType.F_CLUB) && hasSubType(MiscType.S_RETRACTABLE_BLADE)) {
                // 10k per ton for the actual blade, plus 10k for the mechanism
                int bladeTons = (int) Math.ceil(entity.getWeight() / 20.0);
                costValue = (1 + bladeTons) * 10000;
            } else if (hasFlag(MiscType.F_TRACKS)) {
                costValue = (int) Math.ceil(((hasSubType(S_QUADVEE_WHEELS) ? 750 : 500)
                        * (entity.hasEngine() ? entity.getEngine().getRating() : 0) * entity.getWeight()) / 75);
            } else if (hasFlag(MiscType.F_TALON)) {
                costValue = (int) Math.ceil(getTonnage(entity, loc) * 300);
            } else if (hasFlag(MiscType.F_SPIKES)) {
                costValue = (int) Math.ceil(entity.getWeight() * 50);
            } else if (hasFlag(MiscType.F_PARTIAL_WING)) {
                costValue = (int) Math.ceil(getTonnage(entity, loc) * 50000);
            } else if (hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)) {
                int multiplier = entity.locationIsLeg(loc) ? 700 : 500;
                costValue = (int) Math.ceil(entity.getWeight() * multiplier);
            } else if (hasFlag(MiscType.F_HAND_WEAPON) && (hasSubType(MiscType.S_CLAW))) {
                costValue = (int) Math.ceil(entity.getWeight() * 200);
            } else if (hasFlag(F_LIGHT_SAIL)) {
                costValue = getTonnage(entity, loc) * 10000;
            } else if (hasFlag(F_NAVAL_C3)) {
                costValue = getTonnage(entity, loc) * 100000;
                
             //TODO NEO- Not sure how to add in the base control weights see IO pg 187   
            } else if (hasFlag(MiscType.F_SRCS)) {
                costValue = (getTonnage(entity, loc) * 10000) + 5000;
            } else if (hasFlag(MiscType.F_SASRCS)) {
                costValue = (getTonnage(entity, loc) * 12500) + 6250;
            } else if (hasFlag(MiscType.F_CASPAR)) {
                costValue = (getTonnage(entity, loc) * 50000) + 500000;
            } else if (hasFlag(MiscType.F_CASPARII)) {
                costValue = (getTonnage(entity, loc) * 20000) + 50000;
            } else if (hasFlag(MiscType.F_ATAC)) {
                costValue = (getTonnage(entity, loc, size) * 100000);
            } else if (hasFlag(MiscType.F_DTAC)) {
                costValue = (getTonnage(entity, loc, size) * 50000);
                             
            } else if (hasFlag(MiscType.F_CLUB) && (hasSubType(MiscType.S_LANCE))) {
                costValue = (int) Math.ceil(entity.getWeight() * 150);
            } else if (hasFlag(F_MECHANICAL_JUMP_BOOSTER)) {
                switch (entity.getWeightClass()) {
                    case EntityWeightClass.WEIGHT_ASSAULT:
                        costValue = 300000;
                        break;
                    case EntityWeightClass.WEIGHT_HEAVY:
                        costValue = 150000;
                        break;
                    case EntityWeightClass.WEIGHT_MEDIUM:
                        costValue = 75000;
                        break;
                    default:
                        costValue = 50000;
                }
            } else if (hasFlag(F_BASIC_FIRECONTROL)) {
                // 5% of weapon cost
                double weaponCost = 0;
                for (Mounted mount : entity.getWeaponList()) {
                    weaponCost += mount.getCost();
                }
                costValue = weaponCost * 0.05;
            } else if (hasFlag(F_ADVANCED_FIRECONTROL)) {
                // 10% of weapon cost
                double weaponCost = 0;
                for (Mounted mount : entity.getWeaponList()) {
                    weaponCost += mount.getCost();
                }
                costValue = weaponCost * 0.1;
            } else if (hasFlag(F_LADDER)) {
                costValue = size * 5;
            } else if (hasFlag(F_COMMUNICATIONS)) {
                costValue = size * 10000;
            } else if (hasFlag(F_RAM_PLATE)) {
                costValue = getTonnage(entity, loc) * 10000;
            }
            
            if (isArmored) {
                double armoredCost = costValue;

                armoredCost += 150000 * getCriticals(entity, size);

                return armoredCost;
            }
        }
        return costValue;
    }

    @Override
    public int getCriticals(Entity entity, double size) {
        if ((criticals != CRITICALS_VARIABLE) || (null == entity)) {
            return criticals;
        }
        // check for known formulas
        if (hasFlag(F_CLUB) && (hasSubType(S_HATCHET) || hasSubType(S_SWORD))) {
            return (int) Math.ceil(entity.getWeight() / 15.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_LANCE)) {
            return (int) Math.ceil(entity.getWeight() / 20.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_MACE)) {
            return (int) Math.ceil(entity.getWeight() / 10.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_RETRACTABLE_BLADE)) {
            return 1 + (int) Math.ceil(entity.getWeight() / 20.0);
        } else if (hasFlag(F_MASC)) {
            if (TechConstants.isClan(getTechLevel(entity.getTechLevelYear()))) {
                return Math.max((int) Math.round(entity.getWeight() / 25.0), 1);
            }
            return Math.max((int) Math.round(entity.getWeight() / 20.0), 1);

        } else if ((entity instanceof Aero)
                && (hasFlag(F_REACTIVE) || hasFlag(F_REFLECTIVE) || hasFlag(F_ANTI_PENETRATIVE_ABLATIVE)
                        || hasFlag(F_BALLISTIC_REINFORCED) || hasFlag(F_FERRO_LAMELLOR))) {
            // Aero armor doesn't take up criticals
            return 0;
        } else if (hasFlag(F_TARGCOMP)) {
            // based on tonnage of direct_fire weaponry
            double fTons = 0.0;
            for (Mounted m : entity.getWeaponList()) {
                WeaponType wt = (WeaponType) m.getType();
                if (wt.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    fTons += m.getTonnage();
                }
            }

            for (Mounted mo : entity.getMisc()) {
                MiscType mt = (MiscType) mo.getType();
                if (mt.hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    fTons += mo.getTonnage();
                }
            }
            if (TechConstants.isClan(getTechLevel(entity.getTechLevelYear()))) {
                return (int) Math.ceil(fTons / 5.0f);
            }
            return (int) Math.ceil(fTons / 4.0f);
        } else if (hasFlag(MiscType.F_FERRO_FIBROUS) || hasFlag(MiscType.F_REACTIVE)) {
            if (entity.isClanArmor(1) && !entity.hasPatchworkArmor()) {
                if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                    return 4;
                } else {
                    return 7;
                }
            } else if (entity.hasPatchworkArmor()) {
                int slots = 0;
                for (int i = 0; i < entity.locations(); i++) {
                    if ((entity.getArmorType(i) == EquipmentType.T_ARMOR_FERRO_FIBROUS)
                            || (entity.getArmorType(i) == EquipmentType.T_ARMOR_REACTIVE)) {
                        if (TechConstants.isClan(entity.getArmorTechLevel(i))) {
                            slots++;
                        } else {
                            slots += 2;
                        }
                    }
                }
                if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                    return (int) Math.ceil(slots / 2.0);
                } else {
                    return slots;
                }
            } else {
                if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                    return 7;
                } else {
                    return 14;
                }
            }
        } else if (hasFlag(MiscType.F_REFLECTIVE)) {
            if (entity.isClanArmor(1) && !entity.hasPatchworkArmor()) {
                if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                    return 3;
                } else {
                    return 5;
                }
            } else if (entity.hasPatchworkArmor()) {
                int slots = 0;
                for (int i = 0; i < entity.locations(); i++) {
                    if (entity.getArmorType(i) == EquipmentType.T_ARMOR_REFLECTIVE) {
                        if (TechConstants.isClan(entity.getArmorTechLevel(i))) {
                            slots++;
                        } else {
                            slots += 2;
                        }
                    }
                }
                if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                    return (int) Math.ceil(slots / 2.0);
                } else {
                    return slots;
                }
            }
            if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                return 5;
            } else {
                return 10;
            }
        } else if (hasFlag(MiscType.F_LIGHT_FERRO)) {
            if (!entity.hasPatchworkArmor()) {
                if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                    return 4;
                } else {
                    return 7;
                }
            } else {
                int slots = 0;
                for (int i = 0; i < entity.locations(); i++) {
                    if (entity.getArmorType(i) == EquipmentType.T_ARMOR_LIGHT_FERRO) {
                        slots++;
                    }
                }
                if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                    return (int) Math.ceil(slots / 2.0);
                } else {
                    return slots;
                }
            }
        } else if (hasFlag(MiscType.F_HEAVY_FERRO)) {
            if (!entity.hasPatchworkArmor()) {
                if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                    return 11;
                } else {
                    return 21;
                }
            } else {
                int slots = 0;
                for (int i = 0; i < entity.locations(); i++) {
                    if (entity.getArmorType(i) == EquipmentType.T_ARMOR_HEAVY_FERRO) {
                        slots += 3;
                    }
                }
                if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                    return (int) Math.ceil(slots / 2.0);
                } else {
                    return slots;
                }
            }
        } else if (hasFlag(MiscType.F_FERRO_LAMELLOR)) {
            if (!entity.hasPatchworkArmor()) {
                if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                    return 6;
                } else {
                    return 12;
                }
            } else {
                int slots = 0;
                for (int i = 0; i < entity.locations(); i++) {
                    if (entity.getArmorType(i) == EquipmentType.T_ARMOR_FERRO_LAMELLOR) {
                        slots += 2;
                    }
                }
                if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                    return (int) Math.ceil(slots / 2.0);
                } else {
                    return slots;
                }
            }
        } else if (hasFlag(MiscType.F_FERRO_FIBROUS_PROTO)) {
            if (!entity.hasPatchworkArmor()) {
                if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                    return 8;
                } else {
                    return 16;
                }
            } else {
                int slots = 0;
                for (int i = 0; i < entity.locations(); i++) {
                    if (entity.getArmorType(i) == EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO) {
                        slots += 2;
                    }
                }
                if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                    return (int) Math.ceil(slots / 2.0);
                } else {
                    return slots;
                }
            }
        } else if (hasFlag(MiscType.F_ANTI_PENETRATIVE_ABLATIVE) || hasFlag(MiscType.F_HEAT_DISSIPATING)) {
            if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                return 3;
            } else {
                return 6;
            }
        } else if (hasFlag(MiscType.F_BALLISTIC_REINFORCED) || hasFlag(MiscType.F_IMPACT_RESISTANT)) {
            if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                return 5;
            } else {
                return 10;
            }
        } else if (hasFlag(F_JUMP_BOOSTER) || hasFlag(F_TALON)) {
            return (entity instanceof QuadMech) ? 8 : 4; // all slots in all
            // legs
        } else if (hasFlag(F_HAND_WEAPON) && hasSubType(S_CLAW)) {
            return (int) Math.ceil(entity.getWeight() / 15);
        } else if (hasFlag(F_ACTUATOR_ENHANCEMENT_SYSTEM)) {
            switch (entity.getWeightClass()) {
                case EntityWeightClass.WEIGHT_LIGHT:
                    return 1;
                case EntityWeightClass.WEIGHT_MEDIUM:
                    return 2;
                case EntityWeightClass.WEIGHT_HEAVY:
                    return 3;
                case EntityWeightClass.WEIGHT_ASSAULT:
                    return 4;
            }
            return entity.getWeightClass();
        } else if (hasFlag(F_TRACKS)) {
            if (entity instanceof QuadMech) {
                return 4;
            }
            if (entity instanceof BipedMech) {
                return 2;
            }
        } else if (hasFlag(F_BLUE_SHIELD)) {
            if (entity instanceof Aero) {
                return 4;
            } else {
                // Mechs: one per location except head
                // Support vehicles: one per location except body
                // Combat vehicles: Handled by getTankSlots
                return entity.locations() - 1;
            }
        } else if (hasFlag(F_ENDO_STEEL)) {
            if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                return 7;
            } else {
                return 14;
            }
            // Clan Endo Steel doesn't have variable crits
        } else if (hasFlag(F_ENDO_COMPOSITE)) {
            if ((entity instanceof Mech) && ((Mech) entity).isSuperHeavy()) {
                return 4;
            } else {
                return 7;
            }
            // Clan Endo Composite doesn't have variable crits
        } else if (hasFlag(F_FUEL)) {
            return (int) Math.ceil(getTonnage(entity));
        } else if (hasFlag(F_CARGO) || hasFlag(F_LIQUID_CARGO) || hasFlag(F_COMMUNICATIONS)) {
            return (int) Math.ceil(size);
        }
        // right, well I'll just guess then
        return 1;
    }

    @Override
    public int getTankSlots(Entity entity) {
        if (hasFlag(MiscType.F_BLUE_SHIELD)) {
            return entity.locations() - 1;
        }
        return super.getTankSlots(entity);
    }

    public double getBV(Entity entity, Mounted linkedTo) {
        if (hasFlag(F_PPC_CAPACITOR) && (linkedTo != null) && (linkedTo.getLinkedBy() != null)) {

            if (linkedTo.getType() instanceof ISLightPPC) {
                return 44;
            }

            if (linkedTo.getType() instanceof ISPPC) {
                return 88;
            }

            if (linkedTo.getType() instanceof ISHeavyPPC) {
                return 53;
            }

            if (linkedTo.getType() instanceof ISSnubNosePPC) {
                return 87;
            }

            if (linkedTo.getType() instanceof ISERPPC) {
                return 114;
            }

            if (linkedTo.getType() instanceof CLERPPC) {
                return 136;
            }
        }

        if (linkedTo != null) {
            return this.getBV(entity, linkedTo.getLocation());
        } else {
            return this.getBV(entity);
        }
    }

    @Override
    public double getBV(Entity entity) {
        return getBV(entity, Entity.LOC_NONE);
    }

    public double getBV(Entity entity, int location) {
        double returnBV = 0.0;
        if ((bv != BV_VARIABLE) || (null == entity)) {
            returnBV = bv;
            // Mast Mounts give extra BV to equipment mounted in the mast
            if ((entity instanceof VTOL) && entity.hasWorkingMisc(MiscType.F_MAST_MOUNT, -1, VTOL.LOC_ROTOR)
                    && (location == VTOL.LOC_ROTOR) && (hasFlag(MiscType.F_ECM) || hasFlag(MiscType.F_BAP)
                            || hasFlag(MiscType.F_C3S) || hasFlag(MiscType.F_C3SBS) || hasFlag(MiscType.F_C3I))) {
                returnBV += 10;
            }
            return returnBV;
        }
        // check for known formulas
        double tsmMod = entity.hasWorkingMisc(F_TSM) ? 2 : 1;
        if (hasFlag(F_CLUB) && hasSubType(S_HATCHET)) {
            returnBV = Math.ceil(entity.getWeight() / 5.0) * 1.5 * tsmMod;
        } else if (hasFlag(F_CLUB) && hasSubType(S_SWORD)) {
            returnBV = Math.ceil((entity.getWeight() / 10.0) + 1) * 1.725 * tsmMod;
        } else if (hasFlag(F_CLUB) && hasSubType(S_LANCE)) {
            returnBV = Math.ceil(entity.getWeight() / 5.0) * tsmMod;
        } else if (hasFlag(F_CLUB) && hasSubType(S_MACE)) {
            returnBV = Math.ceil(entity.getWeight() / 4.0) * tsmMod;
        } else if (hasFlag(F_CLUB) && hasSubType(S_RETRACTABLE_BLADE)) {
            returnBV = Math.ceil(entity.getWeight() / 10.0) * 1.725 * tsmMod;
        } else if (hasFlag(F_HAND_WEAPON) && hasSubType(S_CLAW)) {
            returnBV = (Math.ceil(entity.getWeight() / 7.0)) * 1.275 * tsmMod;
        } else if (hasFlag(F_TALON)) {
            // according to an email from TPTB, Talon BV is the extra damage they
            // do for kicks, so 50% of normal kick damage
            returnBV = Math.round(Math.floor(entity.getWeight() / 5.0) * 0.5) * tsmMod;
        } else if (hasFlag(MiscType.F_RAM_PLATE)) {
            // half the maximum charge damage (rounded down) * 1.1
            int damage = ((int) (entity.getWeight() * entity.getRunMP() * 0.1)) / 2;
            if (entity instanceof Mech) {
                // Spikes located in a torso location increase the charge damage by 2 points
                for (int loc = 0; loc < entity.locations(); loc++) {
                    if (((Mech) entity).locationIsTorso(loc)
                            && entity.hasWorkingMisc(F_SPIKES, -1, loc)) {
                        damage++;
                    }
                }
                returnBV = damage * 1.1;
            }
        }
        // Deal with floating point precision errors
        return Math.round(returnBV * 1000.0) / 1000.0;
    }
    
    @Override
    public int getHeat() {
        if (hasFlag(F_NULLSIG)
                || hasFlag(F_VOIDSIG)) {
            return 10;
        } else if (hasFlag(F_MOBILE_HPG) && hasFlag(F_MECH_EQUIPMENT)) {
            // Ground mobile HPG
            return 20;
        } else if (hasFlag(F_MOBILE_HPG)) {
            // Large craft HPG
            return 40;
        } else if (hasFlag(F_CHAMELEON_SHIELD)) {
            return 6;
        } else if (hasFlag(F_VIRAL_JAMMER_DECOY)
                || hasFlag(F_VIRAL_JAMMER_HOMING)) {
            return 12;
        } else if (hasFlag(F_RISC_LASER_PULSE_MODULE)
                || hasFlag(F_NOVA)
                || (hasFlag(F_CLUB) && hasSubType(S_SPOT_WELDER))) {
            return 2;
        }
        return 0;
    }

    /**
     * Add all the types of misc eq we can create to the list
     */
    public static void initializeTypes() {
        // all tech level 1 stuff
        EquipmentType.addType(MiscType.createHeatSink());
        EquipmentType.addType(MiscType.createJumpJet());
        EquipmentType.addType(MiscType.createTreeClub());
        EquipmentType.addType(MiscType.createGirderClub());
        EquipmentType.addType(MiscType.createLimbClub());
        EquipmentType.addType(MiscType.createHatchet());
        EquipmentType.addType(MiscType.createStandard());

        // Start of Level2 stuff
        EquipmentType.addType(MiscType.createISDoubleHeatSink());
        EquipmentType.addType(MiscType.createISDoubleHeatSinkPrototype());
        EquipmentType.addType(MiscType.createISFreezerPrototype());
        EquipmentType.addType(MiscType.createCLDoubleHeatSink());
        EquipmentType.addType(MiscType.createISCASE());
        EquipmentType.addType(MiscType.createCLCASE());
        EquipmentType.addType(MiscType.createISMASC());
        EquipmentType.addType(MiscType.createCLMASC());
        EquipmentType.addType(MiscType.createTSM());
        EquipmentType.addType(MiscType.createPrototypeTSM());
        EquipmentType.addType(MiscType.createC3S());
        EquipmentType.addType(MiscType.createC3SBS());
        EquipmentType.addType(MiscType.createC3I());
        EquipmentType.addType(MiscType.createNC3());
        EquipmentType.addType(MiscType.createISArtemis());
        EquipmentType.addType(MiscType.createCLArtemis());
        EquipmentType.addType(MiscType.createGECM());
        EquipmentType.addType(MiscType.createGECMPrototype());
        EquipmentType.addType(MiscType.createCLECM());
        EquipmentType.addType(MiscType.createISTargComp());
        EquipmentType.addType(MiscType.createCLTargComp());
        EquipmentType.addType(MiscType.createMekStealth());
        EquipmentType.addType(MiscType.createISFerroFibrous());
        EquipmentType.addType(MiscType.createCLFerroFibrous());
        EquipmentType.addType(MiscType.createISEndoSteel());
        EquipmentType.addType(MiscType.createCLEndoSteel());
        EquipmentType.addType(MiscType.createBeagleActiveProbe());
        EquipmentType.addType(MiscType.createBeagleActiveProbePrototype());
        EquipmentType.addType(MiscType.createBloodhoundActiveProbe());
        EquipmentType.addType(MiscType.createTHBBloodhoundActiveProbe());
        EquipmentType.addType(MiscType.createCLActiveProbe());
        EquipmentType.addType(MiscType.createCLLightActiveProbe());
        EquipmentType.addType(MiscType.createISAPPod());
        EquipmentType.addType(MiscType.createSword());
        EquipmentType.addType(MiscType.createISPPCCapacitor());
        EquipmentType.addType(MiscType.createRetractableBlade());
        EquipmentType.addType(MiscType.createChainWhip());
        EquipmentType.addType(MiscType.createRamPlate());
        EquipmentType.addType(MiscType.createISApolloFCS());
        EquipmentType.addType(MiscType.createIMEjectionSeat());
        EquipmentType.addType(MiscType.createSVEjectionSeat());
        EquipmentType.addType(MiscType.createIndustrialTSM());
        EquipmentType.addType(MiscType.createSalvageArm());
        EquipmentType.addType(MiscType.createSpotWelder());
        EquipmentType.addType(MiscType.createLiftHoist());
        EquipmentType.addType(MiscType.createTracks());
        EquipmentType.addType(MiscType.createQVWheels());
        EquipmentType.addType(MiscType.createISMASS());
        EquipmentType.addType(MiscType.createLightBridgeLayer());
        EquipmentType.addType(MiscType.createMediumBridgeLayer());
        EquipmentType.addType(MiscType.createHeavyBridgeLayer());

        // For industrials and tanks
        EquipmentType.addType(MiscType.createCVEnvironmentalSealedChassis());
        EquipmentType.addType(MiscType.createIndustrialMechEnvironmentalSealing());
        EquipmentType.addType(MiscType.createFieldKitchen());

        EquipmentType.addType(MiscType.createImprovedJumpJet());
        EquipmentType.addType(MiscType.createVehicularJumpJet());
        EquipmentType.addType(MiscType.createJumpBooster());
        EquipmentType.addType(MiscType.createFerroFibrousPrototype());
        EquipmentType.addType(MiscType.createFerroAlumPrototype());
        EquipmentType.addType(MiscType.createLightFerroFibrous());
        EquipmentType.addType(MiscType.createHeavyFerroFibrous());
        EquipmentType.addType(MiscType.createISFerroAlum());
        EquipmentType.addType(MiscType.createCLFerroAlum());
        EquipmentType.addType(MiscType.createHeavyFerroAlum());
        EquipmentType.addType(MiscType.createLightFerroAlum());
        EquipmentType.addType(MiscType.createISHardenedArmor());
        EquipmentType.addType(MiscType.createISIndustrialArmor());
        EquipmentType.addType(MiscType.createISPrimitiveArmor());
        EquipmentType.addType(MiscType.createPrimitiveFighterArmor());
        EquipmentType.addType(MiscType.createISHeavyIndustrialArmor());
        EquipmentType.addType(MiscType.createISCommercialArmor());
        EquipmentType.addType(MiscType.createCLFerroLamellorArmor());
        EquipmentType.addType(MiscType.createISEndoSteelPrototype());
        EquipmentType.addType(MiscType.createReinforcedStructure());
        EquipmentType.addType(MiscType.createISCompositeStructure());
        EquipmentType.addType(MiscType.createIndustrialStructure());
        EquipmentType.addType(MiscType.createIS1CompactHeatSink());
        EquipmentType.addType(MiscType.createIS2CompactHeatSinks());
        EquipmentType.addType(MiscType.createCLLaserHeatSink());
        EquipmentType.addType(MiscType.createArtemisV());
        EquipmentType.addType(MiscType.createISAngelECM());
        EquipmentType.addType(MiscType.createISTHBAngelECM());
        EquipmentType.addType(MiscType.createWatchdogECM());
        EquipmentType.addType(MiscType.createMace());
        EquipmentType.addType(MiscType.createDualSaw());
        EquipmentType.addType(MiscType.createChainsaw());
        EquipmentType.addType(MiscType.createRockCutter());
        EquipmentType.addType(MiscType.createCombine());
        EquipmentType.addType(MiscType.createBackhoe());
        EquipmentType.addType(MiscType.createPileDriver());
        EquipmentType.addType(MiscType.createArmoredCowl());
        EquipmentType.addType(MiscType.createNullSignatureSystem());
        EquipmentType.addType(MiscType.createVoidSignatureSystem());
        EquipmentType.addType(MiscType.createChameleonLightPolarizationShield());
        EquipmentType.addType(MiscType.createLightMinesweeper());
        EquipmentType.addType(MiscType.createBridgeKit());
        EquipmentType.addType(MiscType.createVibroShovel());
        EquipmentType.addType(MiscType.createDemolitionCharge());
        EquipmentType.addType(MiscType.createISSuperCharger());
        EquipmentType.addType(MiscType.createISMediumShield());
        EquipmentType.addType(MiscType.createISSmallShield());
        EquipmentType.addType(MiscType.createISLargeShield());
        EquipmentType.addType(MiscType.createISClaw());
        EquipmentType.addType(MiscType.createISHarJel());
        EquipmentType.addType(MiscType.createISUMU());
        EquipmentType.addType(MiscType.createISLance());
        EquipmentType.addType(MiscType.createISWreckingBall());
        EquipmentType.addType(MiscType.createISFlail());
        EquipmentType.addType(MiscType.createISMediumVibroblade());
        EquipmentType.addType(MiscType.createISSmallVibroblade());
        EquipmentType.addType(MiscType.createISLargeVibroblade());
        EquipmentType.addType(MiscType.createISBuzzsaw());
        EquipmentType.addType(MiscType.createHeavyArmor());
        EquipmentType.addType(MiscType.createSpikes());
        EquipmentType.addType(MiscType.createTalons());
        EquipmentType.addType(MiscType.createISReactive());
        EquipmentType.addType(MiscType.createCLReactive());
        EquipmentType.addType(MiscType.createISReflective());
        EquipmentType.addType(MiscType.createCLReflective());
        EquipmentType.addType(MiscType.createISCASEII());
        EquipmentType.addType(MiscType.createCLCASEII());
        EquipmentType.addType(MiscType.createISAES());
        EquipmentType.addType(MiscType.createISModularArmor());
        EquipmentType.addType(MiscType.createCommsGear());
        EquipmentType.addType(MiscType.createISGroundMobileHPG());
        EquipmentType.addType(MiscType.createISMobileHPG());
        EquipmentType.addType(MiscType.createISPartialWing());
        EquipmentType.addType(MiscType.createCLPartialWing());
        EquipmentType.addType(MiscType.createCargo());
        EquipmentType.addType(MiscType.createLiquidCargo());
        EquipmentType.addType(MiscType.createCargoContainer());
        EquipmentType.addType(MiscType.createMechSprayer());
        EquipmentType.addType(MiscType.createTankSprayer());
        EquipmentType.addType(MiscType.createFrontDumper());
        EquipmentType.addType(MiscType.createRearDumper());
        EquipmentType.addType(MiscType.createLeftDumper());
        EquipmentType.addType(MiscType.createRightDumper());
        EquipmentType.addType(MiscType.createMASH());
        EquipmentType.addType(MiscType.createParamedicEquipment());
        EquipmentType.addType(MiscType.createISMastMount());
        EquipmentType.addType(MiscType.createExtendedFuelTank());
        EquipmentType.addType(MiscType.createBlueShield());
        EquipmentType.addType(MiscType.createISEndoComposite());
        EquipmentType.addType(MiscType.createClanEndoComposite());
        EquipmentType.addType(MiscType.createISLaserInsulator());
        EquipmentType.addType(MiscType.createISEWEquipment());
        EquipmentType.addType(MiscType.createISCollapsibleCommandModule());
        EquipmentType.addType(MiscType.createHitch());
        EquipmentType.addType(MiscType.createISFlotationHull());
        EquipmentType.addType(MiscType.createISLimitedAmphibiousChassis());
        EquipmentType.addType(MiscType.createISFullyAmphibiousChassis());
        EquipmentType.addType(MiscType.createISShoulderTurret());
        EquipmentType.addType(MiscType.createISHeadTurret());
        EquipmentType.addType(MiscType.createISQuadTurret());
        EquipmentType.addType(MiscType.createISTankCommandConsole());
        EquipmentType.addType(MiscType.createISSponsonTurret());
        EquipmentType.addType(MiscType.createPintleTurret());
        EquipmentType.addType(MiscType.createISArmoredMotiveSystem());
        EquipmentType.addType(MiscType.createCLArmoredMotiveSystem());
        EquipmentType.addType(MiscType.createISChaffPod());
        EquipmentType.addType(MiscType.createBC3());
        EquipmentType.addType(MiscType.createBC3i());
        EquipmentType.addType(MiscType.createISHIResImager());
        EquipmentType.addType(MiscType.createISHyperspectralImager());
        EquipmentType.addType(MiscType.createISInfraredImager());
        EquipmentType.addType(MiscType.createISLookDownRadar());
        EquipmentType.addType(MiscType.createISVTOLJetBooster());
        EquipmentType.addType(MiscType.createRemoteSensorDispenser());
        EquipmentType.addType(MiscType.createPrototypeRemoteSensorDispenser());
        EquipmentType.addType(MiscType.createISVehicularMineDispenser());
        EquipmentType.addType(MiscType.createMiningDrill());
        EquipmentType.addType(MiscType.createISReconCamera());
        EquipmentType.addType(MiscType.createISCombatVehicleEscapePod());
        EquipmentType.addType(MiscType.createISSmallNavalCommScannerSuite());
        EquipmentType.addType(MiscType.createISLargeNavalCommScannerSuite());
        EquipmentType.addType(MiscType.createISNavalTugAdaptor());
        EquipmentType.addType(MiscType.createISSpaceMineDispenser());
        EquipmentType.addType(MiscType.createVehicularStealth());
        EquipmentType.addType(MiscType.createEmergencyC3M());
        EquipmentType.addType(MiscType.createNovaCEWS());

        // ProtoMek Stuff
        EquipmentType.addType(MiscType.createCLProtoMyomerBooster());
        EquipmentType.addType(MiscType.createProtoPartialWing());
        EquipmentType.addType(MiscType.createProtomechJumpJet());
        EquipmentType.addType(MiscType.createExtendedJumpJet());
        EquipmentType.addType(MiscType.createProtomechUMU());

        // Start BattleArmor equipment
        EquipmentType.addType(MiscType.createISBAStandardArmor());
        EquipmentType.addType(MiscType.createClanBAStandardArmor());
        EquipmentType.addType(MiscType.createISBAAdvancedArmor());
        EquipmentType.addType(MiscType.createISBAStandardPrototypeArmor());
        EquipmentType.addType(MiscType.createClanBAFireResistantArmor());
        EquipmentType.addType(MiscType.createISBAReactiveArmor());
        EquipmentType.addType(MiscType.createClanBAReactiveArmor());
        EquipmentType.addType(MiscType.createISBAReflectiveArmor());
        EquipmentType.addType(MiscType.createClanBAReflectiveArmor());
        EquipmentType.addType(MiscType.createISBAStealthPrototype());
        EquipmentType.addType(MiscType.createISBABasicStealth());
        EquipmentType.addType(MiscType.createClanBABasicStealth());
        EquipmentType.addType(MiscType.createISBAStandardStealth());
        EquipmentType.addType(MiscType.createClanBAStandardStealth());
        EquipmentType.addType(MiscType.createISBAImprovedStealth());
        EquipmentType.addType(MiscType.createClanBAImprovedStealth());
        EquipmentType.addType(MiscType.createISBAMimeticCamo());
        EquipmentType.addType(MiscType.createMine());
        EquipmentType.addType(MiscType.createBABasicManipulator());
        EquipmentType.addType(MiscType.createBABasicManipulatorMineClearance());
        EquipmentType.addType(MiscType.createBABattleClaw());
        EquipmentType.addType(MiscType.createBABattleClawMagnets());
        EquipmentType.addType(MiscType.createBABattleClawVibro());
        EquipmentType.addType(MiscType.createBACargoLifter());
        EquipmentType.addType(MiscType.createBAHeavyBattleClaw());
        EquipmentType.addType(MiscType.createBAHeavyBattleClawMagnet());
        EquipmentType.addType(MiscType.createBAHeavyBattleClawVibro());
        EquipmentType.addType(MiscType.createBAIndustrialDrill());
        EquipmentType.addType(MiscType.createBASalvageArm());
        EquipmentType.addType(MiscType.createBAModularEquipmentAdaptor());
        EquipmentType.addType(MiscType.createBAArmoredGlove());
        EquipmentType.addType(MiscType.createBAMagneticClamp());
        EquipmentType.addType(MiscType.createBAAPMount());
        EquipmentType.addType(MiscType.createCLBAMyomerBooster());
        EquipmentType.addType(MiscType.createISSingleHexECM());
        EquipmentType.addType(MiscType.createCLSingleHexECM());
        EquipmentType.addType(MiscType.createBattleMechNeuralInterfaceUnit());
        EquipmentType.addType(MiscType.createBAISAngelECM());
        EquipmentType.addType(MiscType.createBACLAngelECM());
        EquipmentType.addType(MiscType.createSimpleCamo());
        EquipmentType.addType(MiscType.createParafoil());
        EquipmentType.addType(MiscType.createSearchlight());
        EquipmentType.addType(MiscType.createBASearchlight());
        EquipmentType.addType(MiscType.createHandheldSearchlight());
        EquipmentType.addType(MiscType.createISImprovedSensors());
        EquipmentType.addType(MiscType.createCLImprovedSensors());
        EquipmentType.addType(MiscType.createISBALightActiveProbe());
        EquipmentType.addType(MiscType.createISBARemoteSensorDispenser());
        EquipmentType.addType(MiscType.createBACuttingTorch());
        EquipmentType.addType(MiscType.createISBASpaceOperationsAdaptation());
        EquipmentType.addType(MiscType.createISDetachableWeaponPack());
        EquipmentType.addType(MiscType.createISBAHeatSensor());
        EquipmentType.addType(MiscType.createISBAExtendedLifeSupport());
        EquipmentType.addType(MiscType.createBAPartialWing());
        EquipmentType.addType(MiscType.createBAJumpJet());
        EquipmentType.addType(MiscType.createBAVTOLEquipment());
        EquipmentType.addType(MiscType.createBAUMU());
        EquipmentType.addType(MiscType.createISBAJumpBooster());
        EquipmentType.addType(MiscType.createISBAMechanicalJumpBooster());
        EquipmentType.addType(MiscType.createISBAFuelTank());
        EquipmentType.addType(MiscType.createBALaserMicrophone());
        EquipmentType.addType(MiscType.createBAPowerPack());
        EquipmentType.addType(MiscType.createBAShotgunMicrophone());
        EquipmentType.addType(MiscType.createISBAMineDispenser());
        EquipmentType.addType(MiscType.createBAMissionEquipStorage());
        EquipmentType.addType(MiscType.createISBADropChuteCamo());
        EquipmentType.addType(MiscType.createISBADropChuteCamo());
        EquipmentType.addType(MiscType.createISBADropChuteStd());
        EquipmentType.addType(MiscType.createISBADropChuteStealth());
        EquipmentType.addType(MiscType.createVeeDropChuteCamo());
        EquipmentType.addType(MiscType.createVeeDropChuteReuse());
        EquipmentType.addType(MiscType.createVeeDropChuteStd());
        EquipmentType.addType(MiscType.createVeeDropChuteStealth());

        /*
         * Included for completeness.
         * EquipmentType.addType(MiscType.createCLBAHarjel());
         * EquipmentType.addType(MiscType.createBACLUMU());
         * EquipmentType.addType(MiscType.createBAJumpJet());
         */

        // Support Vee Chassis stuff
        EquipmentType.addType(MiscType.createAmphibiousChassis());
        EquipmentType.addType(MiscType.createArmoredChassis());
        EquipmentType.addType(MiscType.createBicycleModification());
        EquipmentType.addType(MiscType.createConvertibleModification());
        EquipmentType.addType(MiscType.createISCVDuneBuggyChassis());
        EquipmentType.addType(MiscType.createEnvironmentalSealingChassisMod());
        EquipmentType.addType(MiscType.createExternalPowerPickup());
        EquipmentType.addType(MiscType.createHydroFoilChassisModification());
        EquipmentType.addType(MiscType.createMonocycleModification());
        EquipmentType.addType(MiscType.createISOffRoadChassis());
        EquipmentType.addType(MiscType.createOmniChassisMod());
        EquipmentType.addType(MiscType.createPropChassisModification());
        EquipmentType.addType(MiscType.createSnomobileChassis());
        EquipmentType.addType(MiscType.createSTOLChassisMod());
        EquipmentType.addType(MiscType.createSubmersibleChassisMod());
        EquipmentType.addType(MiscType.createTractorModification());
        EquipmentType.addType(MiscType.createTrailerModification());
        EquipmentType.addType(MiscType.createUltraLightChassisModification());
        EquipmentType.addType(MiscType.createVSTOLChassisMod());
        EquipmentType.addType(MiscType.createISSVDuneBuggyChassis());

        // Support Vee Equipment stuff
        EquipmentType.addType(MiscType.createBasicFireControl());
        EquipmentType.addType(MiscType.createAdvancedFireControl());
        EquipmentType.addType(MiscType.createISMineSweeper());
        EquipmentType.addType(MiscType.createISMobileFieldBase());
        EquipmentType.addType(MiscType.createISPrototypeJumpJet());
        EquipmentType.addType(MiscType.createISPrototypeImprovedJumpJet());
        EquipmentType.addType(MiscType.createBoobyTrap());
        EquipmentType.addType(MiscType.createRefuelingDrogue());
        EquipmentType.addType(MiscType.createBulldozer());
        EquipmentType.addType(MiscType.createExternalStoresHardpoint());
        EquipmentType.addType(MiscType.createManipulator());
        EquipmentType.addType(MiscType.createLadder());
        EquipmentType.addType(MiscType.createMaritimeLifeboat());
        EquipmentType.addType(MiscType.createMaritimeEscapePod());
        EquipmentType.addType(MiscType.createAtmossphericLifeboat());

        // 3145 Stuff
        EquipmentType.addType(MiscType.createAntiPenetrativeAblation());
        EquipmentType.addType(MiscType.createISHeatDissipating());
        EquipmentType.addType(MiscType.createISImpactResistant());
        EquipmentType.addType(MiscType.createISBallisticReinforced());
        EquipmentType.addType(MiscType.createHarJelII());
        EquipmentType.addType(MiscType.createHarJelIII());
        EquipmentType.addType(MiscType.createRadicalHeatSinkSystem());
        EquipmentType.addType(MiscType.createLAMBombBay());
        EquipmentType.addType(MiscType.createLAMAdditionalFuel());
        EquipmentType.addType(MiscType.createLightFluidSuctionSystemMech());
        EquipmentType.addType(MiscType.createLightFluidSuctionSystem());
        EquipmentType.addType(MiscType.createFluidSuctionSystem());
        EquipmentType.addType(MiscType.createRISCSuperCooledMyomer());
        EquipmentType.addType(MiscType.createRISCViralJammerDecoy());
        EquipmentType.addType(MiscType.createRISCViralJammerHoming());
        EquipmentType.addType(MiscType.createRISCLaserPulseModule());
        EquipmentType.addType(MiscType.createRISCEmergencyCoolantSystem());

        // Prototype Stuff
        EquipmentType.addType(MiscType.createISProtoArtemis());
        EquipmentType.addType(MiscType.createCASEPrototype());
        EquipmentType.addType(MiscType.createElectricDischargeArmor());
        EquipmentType.addType(MiscType.createProtoMagneticClamp());
        EquipmentType.addType(MiscType.createProtomechMeleeWeapon());
        EquipmentType.addType(MiscType.createProtoQuadMeleeSystem());

        // Drone and Robotic Systems
        EquipmentType.addType(MiscType.createISRemoteDroneCommandConsole());
        EquipmentType.addType(MiscType.createSmartRoboticControlSystem());
        EquipmentType.addType(MiscType.createImprovedSmartRoboticControlSystem());
        EquipmentType.addType(MiscType.createISDroneCarrierControlSystem());
        EquipmentType.addType(MiscType.createISDroneOperatingSystem());
        EquipmentType.addType(MiscType.createShieldedAeroSRCS());
        EquipmentType.addType(MiscType.createImprovedShieldedAeroSRCS());
        EquipmentType.addType(MiscType.createEliteShieldedAeroSRCS());
        EquipmentType.addType(MiscType.createCasparDroneControlSystem());
        EquipmentType.addType(MiscType.createImprovedCasparDroneControlSystem());
        EquipmentType.addType(MiscType.createCasparIIDroneControlSystem());
        EquipmentType.addType(MiscType.createImprovedCasparIIDroneControlSystem());
        EquipmentType.addType(MiscType.createAutoTacticalAnalysisComputer());
        EquipmentType.addType(MiscType.createDirectTacticalAnalysisSystem());
        EquipmentType.addType(MiscType.createSDSSelfDestructSystem());
        EquipmentType.addType(MiscType.createSDSJammerSystem());

        // Large Craft Systems
        EquipmentType.addType(MiscType.createPCMT());
        EquipmentType.addType(MiscType.createLithiumFusionBattery());
        EquipmentType.addType(MiscType.createLightSail());
        EquipmentType.addType(MiscType.createEnergyStorageBattery());
        EquipmentType.addType(MiscType.createISImpFerroAluminumArmor());
        EquipmentType.addType(MiscType.createClanImpFerroAluminumArmor());
        EquipmentType.addType(MiscType.createPrimitiveLCAerospaceArmor());
        EquipmentType.addType(MiscType.createISAeroSpaceArmor());
        EquipmentType.addType(MiscType.createClanAeroSpaceArmor());
        EquipmentType.addType(MiscType.createISFerroCarbideArmor());
        EquipmentType.addType(MiscType.createClanFerroCarbideArmor());
        EquipmentType.addType(MiscType.createISLamellorFerroCarbideArmor());
        EquipmentType.addType(MiscType.createClanLamellorFerroCarbideArmor());

        // Infantry Equipment Packs
        EquipmentType.addType(MiscType.createISAblativeStandardInfArmor());
        EquipmentType.addType(MiscType.createISAblativeConcealedInfArmor());
        EquipmentType.addType(MiscType.createISAblativeFlakStandardArmorInfArmor());
        EquipmentType.addType(MiscType.createISAblativeFlakConcealedArmorInfArmor());
        EquipmentType.addType(MiscType.createISBallisicPlateStandardInfArmor());
        EquipmentType.addType(MiscType.createBallisicPlateConcealedInfArmor());
        EquipmentType.addType(MiscType.createClothingFatiguesInfArmor());
        EquipmentType.addType(MiscType.createClothingLeatherHideInfArmor());
        EquipmentType.addType(MiscType.createClothingLightInfArmor());
        EquipmentType.addType(MiscType.createISEngineeringSuitInfArmor());
        EquipmentType.addType(MiscType.createISEnvironmentSuitLightInfArmor());
        EquipmentType.addType(MiscType.createISEnvironmentSuitHostileInfArmor());
        EquipmentType.addType(MiscType.createISEnvironmentSuitMarineInfArmor());
        EquipmentType.addType(MiscType.createISFlakStandardInfArmor());
        EquipmentType.addType(MiscType.createISFlakConcealedInfArmor());
        EquipmentType.addType(MiscType.createISHeatSuitInfArmor());
        EquipmentType.addType(MiscType.createISMechWarriorCombatSuitInfArmor());
        EquipmentType.addType(MiscType.createISMechWarriorCoolingSuitInfArmor());
        EquipmentType.addType(MiscType.createMechWarriorCoolingVestInfArmor());
        EquipmentType.addType(MiscType.createMyomerSuitInfArmor());
        EquipmentType.addType(MiscType.createMyomerVestInfArmor());
        EquipmentType.addType(MiscType.createParkaInfArmor());
        EquipmentType.addType(MiscType.createNeoChainMailInfArmor());
        EquipmentType.addType(MiscType.createSnowSuitInfArmor());
        EquipmentType.addType(MiscType.createSpaceSuitInfArmor());
        EquipmentType.addType(MiscType.createSpacesuitCombatInfArmor());
        EquipmentType.addType(MiscType.createCapellanConfederationInfArmor());
        EquipmentType.addType(MiscType.createClanInfArmor());
        EquipmentType.addType(MiscType.createComstarInfArmor());
        EquipmentType.addType(MiscType.createDraconisCombineInfArmor());
        EquipmentType.addType(MiscType.createFedSunsInfArmor());
        EquipmentType.addType(MiscType.createFedComInfArmor());
        EquipmentType.addType(MiscType.createFedSunsLateInfArmor());
        EquipmentType.addType(MiscType.createFRRInfArmor());
        EquipmentType.addType(MiscType.createFWLEarlyInfArmor());
        EquipmentType.addType(MiscType.createFWLLateInfArmor());
        EquipmentType.addType(MiscType.createLyranInfArmor());
        EquipmentType.addType(MiscType.createLyranLateInfArmor());
        EquipmentType.addType(MiscType.createCanopusInfArmor());
        EquipmentType.addType(MiscType.createMarianInfArmor());
        EquipmentType.addType(MiscType.createTaurianInfArmor());
        EquipmentType.addType(MiscType.createWoBInfArmor());
        EquipmentType.addType(MiscType.createGenericInfArmor());
        EquipmentType.addType(MiscType.createISSLDFInfArmor());
        EquipmentType.addType(MiscType.createDESTInfArmor());
        EquipmentType.addType(MiscType.createISSneakCamoSystemInfArmor());
        EquipmentType.addType(MiscType.createISSneakIRSystemInfArmor());
        EquipmentType.addType(MiscType.createISSneakECMSystemInfArmor());
        EquipmentType.addType(MiscType.createISSneakCamoIRInfArmor());
        EquipmentType.addType(MiscType.createISSneakCamoECMInfArmor());
        EquipmentType.addType(MiscType.createISSneakIRECMInfArmor());
        EquipmentType.addType(MiscType.createISSneakThreeSystemInfArmor());

    }

    // Advanced Mech/ProtoMech/Vehicular Motive Systems
    public static MiscType createJumpJet() {
        MiscType misc = new MiscType();

        misc.name = "Jump Jet";
        misc.setInternalName(EquipmentTypeLookup.JUMP_JET);
        misc.addLookupName("JumpJet");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_JUMP_JET).or(F_MECH_EQUIPMENT);
        misc.subType |= S_STANDARD;
        misc.bv = 0;
        misc.rulesRefs = "225, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2464, 2471, 2500, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2464, 2471, 2500, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setStaticTechLevel(SimpleTechLevel.INTRO);
        return misc;
    }

    public static MiscType createImprovedJumpJet() {
        MiscType misc = new MiscType();

        misc.name = "Improved Jump Jet";
        misc.setInternalName(EquipmentTypeLookup.IMPROVED_JUMP_JET);
        misc.addLookupName("IS Improved Jump Jet");
        misc.addLookupName("ISImprovedJump Jet");
        misc.addLookupName("ImprovedJump Jet");
        misc.addLookupName("Clan Improved Jump Jet");
        misc.addLookupName("CLImprovedJump Jet");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 2;
        misc.flags = misc.flags.or(F_JUMP_JET).or(F_MECH_EQUIPMENT);
        misc.subType |= S_IMPROVED;
        misc.bv = 0;
        misc.rulesRefs = "225, TM";
        //Jan 22 - Errata issued by CGL (Greekfire) for IJJs    
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setISAdvancement(3067, 3068, 3069, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(3060, 3068, 3069, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CWX)
                .setProductionFactions(F_CWX, F_CWF, F_LC).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D);
        return misc;
    }

    public static MiscType createISPrototypeImprovedJumpJet() {
        MiscType misc = new MiscType();

        misc.name = "Prototype Improved Jump Jet";
        misc.setInternalName(EquipmentTypeLookup.PROTOTYPE_IMPROVED_JJ);
        misc.shortName = "Prototype Imp. Jump Jet";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.tankslots = 0;
        misc.explosive = true;
        misc.flags = misc.flags.or(F_JUMP_JET).or(F_MECH_EQUIPMENT);
        misc.subType |= S_PROTOTYPE | S_IMPROVED;
        misc.bv = 0;
        misc.rulesRefs = "17,XTRO:SW1";
        // Not included in IO Progression data based on original source.
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3020, DATE_NONE, DATE_NONE, 3069)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_F, RATING_F, RATING_X)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createISPrototypeJumpJet() {
        MiscType misc = new MiscType();

        misc.name = "Primitive Prototype Jump Jet";
        misc.setInternalName(EquipmentTypeLookup.PROTOTYPE_JUMP_JET);
        misc.shortName = "Prototype Jump Jet";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_JUMP_JET).or(F_MECH_EQUIPMENT);
        misc.subType |= S_PROTOTYPE;
        misc.bv = 0;
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2464, DATE_NONE, DATE_NONE, 2471, DATE_NONE)
                .setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2464, DATE_NONE, DATE_NONE, 2471, DATE_NONE)
                .setClanApproximate(true, false, false, true, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createVehicularJumpJet() {
        MiscType misc = new MiscType();

        misc.name = "Jump Jet";
        misc.setInternalName(EquipmentTypeLookup.VEHICLE_JUMP_JET);
        misc.addLookupName("VJJ");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.tankslots = 1;
        misc.flags = misc.flags.or(F_JUMP_JET).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.subType |= S_STANDARD;
        misc.bv = 0;
        misc.rulesRefs = "348, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setAdvancement(2650, 3083, DATE_NONE, 2840, 3083)
            .setApproximate(false, true, false, false, true).setPrototypeFactions(F_TH)
            .setProductionFactions(F_CHH).setTechRating(RATING_E)
            .setAvailability(RATING_E, RATING_X, RATING_F, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    // TODO Protomech Jump Jets See IO, pg 35
    
    public static MiscType createProtomechJumpJet() {
        MiscType misc = new MiscType();
        misc.name = "Jump Jet";
        misc.setInternalName(EquipmentTypeLookup.PROTOMECH_JUMP_JET);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_JUMP_JET).or(F_PROTOMECH_EQUIPMENT);
        misc.subType |= S_STANDARD;
        misc.bv = 0;
        misc.rulesRefs = "225, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3055, 3060, 3060)
                .setClanApproximate(true, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createExtendedJumpJet() {
        MiscType misc = new MiscType();
        // TODO Game Rules.
        misc.name = "Extended Jump Jet System";
        misc.setInternalName(EquipmentTypeLookup.EXTENDED_JUMP_JET_SYSTEM);
        misc.addLookupName("XJJ");
        misc.shortName = "Extended Jump Jet";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_JUMP_JET).or(F_PROTOMECH_EQUIPMENT);
        misc.subType |= S_IMPROVED;
        misc.bv = 0;
        misc.rulesRefs = "65, IO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setClanAdvancement(3071, DATE_NONE, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false).setPrototypeFactions(F_CSR)
                .setProductionFactions(F_CSR).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createProtomechUMU() {
        MiscType misc = new MiscType();
        // TODO Game Rules.
        misc.name = "UMU";
        misc.setInternalName(EquipmentTypeLookup.PROTOMECH_UMU);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_UMU).or(F_PROTOMECH_EQUIPMENT);
        misc.subType |= S_STANDARD;
        misc.bv = 0;
        misc.rulesRefs = "101, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3065, 3075, 3084)
                .setClanApproximate(true, true, false).setPrototypeFactions(F_CBS)
                .setProductionFactions(F_CBS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createCLProtoMyomerBooster() {
        MiscType misc = new MiscType();

        misc.name = "Protomech Myomer Booster";
        misc.setInternalName(EquipmentTypeLookup.PROTOMECH_MYOMER_BOOSTER);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_MASC).or(F_PROTOMECH_EQUIPMENT);
        misc.rulesRefs = "232, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3066, 3068, 3075, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CBS, F_CIH)
                .setProductionFactions(F_CBS, F_CIH).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    // TODO Jump Pack / Mech Drop Pack see IO pg 35

    public static MiscType createISMASC() {
        MiscType misc = new MiscType();

        misc.name = "MASC";
        misc.setInternalName(EquipmentTypeLookup.IS_MASC);
        misc.addLookupName("IS MASC");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_MASC).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        String[] saModes = { "Armed", "Off" };
        misc.setModes(saModes);
        misc.rulesRefs = "225, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(2730, 2740, 3040, 2795, 3035)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_CC).setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_E, RATING_D);
        return misc;
    }

    public static MiscType createCLMASC() {
        MiscType misc = new MiscType();

        misc.name = "MASC";
        misc.setInternalName(EquipmentTypeLookup.CLAN_MASC);
        misc.addLookupName("Clan MASC");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_MASC).or(F_MECH_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.omniFixedOnly = true;
        String[] saModes = { "Armed", "Off" };
        misc.setModes(saModes);

        misc.rulesRefs = "225, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(2820, 2827, 2835, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CIH)
                .setProductionFactions(F_CIH).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_F, RATING_E, RATING_D);
        return misc;
    }

    public static MiscType createJumpBooster() {
        MiscType misc = new MiscType();

        misc.name = "Mech Mechanical Jump Boosters";
        misc.shortName = "Jump Booster (Mech)";
        misc.setInternalName(EquipmentTypeLookup.MECH_JUMP_BOOSTER);
        misc.addLookupName("Jump Booster");
        misc.addLookupName(misc.name);
        misc.shortName = "Jump Booster";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_JUMP_BOOSTER).or(F_MECH_EQUIPMENT);
        misc.spreadable = true;
        misc.rulesRefs = "292, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS)
            .setISAdvancement(DATE_NONE, 3060, 3083, DATE_NONE,DATE_NONE)
            .setISApproximate(false, false, false, false, false)
            .setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS, F_LC).setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createISPartialWing() {
        MiscType misc = new MiscType();

        misc.name = "Partial Wing";
        misc.setInternalName("ISPartialWing");
        misc.addLookupName("IS Partial Wing");
        misc.addLookupName("PartialWing");
        misc.shortName = "Partial Wing";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 8;
        misc.spreadable = true;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_PARTIAL_WING).or(F_MECH_EQUIPMENT);
        misc.rulesRefs = "292, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3067, 3085, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false).setPrototypeFactions(F_MERC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        return misc;
    }

    public static MiscType createCLPartialWing() {
        MiscType misc = new MiscType();

        misc.name = "Partial Wing";
        misc.setInternalName("CLPartialWing");
        misc.addLookupName("Clan Partial Wing");
        misc.addLookupName("Partial Wing (Clan)");
        misc.shortName = "Partial Wing";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 6;
        misc.spreadable = true;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_PARTIAL_WING).or(F_MECH_EQUIPMENT);
        misc.rulesRefs = "292, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(3067, 3085, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CJF)
                .setProductionFactions(F_CJF).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        return misc;
    }

    public static MiscType createProtoPartialWing() {
        MiscType misc = new MiscType();
        misc.name = "Protomech Partial Wing";
        misc.setInternalName("ProtoMechPartialWing");
        misc.shortName = "Partial Wing";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_PROTOMECH_EQUIPMENT).or(F_PARTIAL_WING);

        misc.rulesRefs = "292, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(3067, 3085, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CBS)
                .setProductionFactions(F_CSR).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        return misc;
    }

    public static MiscType createISUMU() {
        MiscType misc = new MiscType();
        misc.name = "UMU";
        misc.setInternalName(EquipmentTypeLookup.MECH_UMU);
        misc.addLookupName("ISUMU");
        misc.addLookupName("CLUMU");
        misc.addLookupName("IS Underwater Maneuvering Unit");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_UMU).or(F_MECH_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "292, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setISAdvancement(DATE_NONE, 3066, 3084, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setClanAdvancement(DATE_NONE, 3061, 3084, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CGS)
                .setProductionFactions(F_LC, F_CWX).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);

        return misc;
    }

    public static MiscType createISVTOLJetBooster() {
        MiscType misc = new MiscType();
        misc.name = "VTOL Jet Booster";
        misc.setInternalName("ISVTOLJetBooster");
        misc.addLookupName("CLVTOLJetBooster");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_JET_BOOSTER).or(F_SUPPORT_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT).or(F_MASC);
        misc.subType |= S_JETBOOSTER;
        misc.criticals = 1;
        misc.omniFixedOnly = true;
        misc.rulesRefs = "350, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setISAdvancement(DATE_NONE, DATE_ES, 3078, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setClanAdvancement(DATE_NONE, DATE_ES, 3078, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false).setPrototypeFactions(F_CHH, F_FS)
                .setProductionFactions(F_FS).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createISSuperCharger() {
        MiscType misc = new MiscType();

        misc.name = "Supercharger";
        misc.setInternalName(misc.name);
        misc.addLookupName("IS Super Charger");
        misc.addLookupName("ISSuperCharger");
        misc.addLookupName("CLSuperCharger");
        misc.addLookupName("CL SuperCharger");
        misc.addLookupName("CL Super Charger");
        misc.addLookupName("Supercharger (Clan)");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_MASC).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.subType |= S_SUPERCHARGER;
        misc.bv = 0;
        String[] saModes = { "Armed", "Off" };
        misc.setModes(saModes);
        misc.rulesRefs = "345, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_F, RATING_F, RATING_D)
                .setISAdvancement(DATE_NONE, DATE_ES, 3078, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setClanAdvancement(DATE_NONE, DATE_ES, 3078, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);

        return misc;
    }

    public static MiscType createTracks() {
        MiscType misc = new MiscType();

        misc.name = "Tracks";
        misc.setInternalName(EquipmentTypeLookup.MECH_TRACKS);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.spreadable = true;
        misc.bv = 0;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_TRACKS).or(F_MECH_EQUIPMENT);

        misc.rulesRefs = "249, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_D, RATING_E, RATING_E, RATING_D)
                .setISAdvancement(2430, 2440, 2500, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2430, 2440, 2500, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);

        return misc;
    }

    public static MiscType createQVWheels() {
        MiscType misc = new MiscType();

        misc.name = "QuadVee Wheels";
        misc.setInternalName(EquipmentTypeLookup.QUADVEE_WHEELS);
        misc.addLookupName("Wheels");
        misc.shortName = "Wheels";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.spreadable = true;
        misc.bv = 0;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_TRACKS).or(F_MECH_EQUIPMENT);
        misc.subType = S_QUADVEE_WHEELS;
        misc.omniFixedOnly = true;
        misc.rulesRefs = "133, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
                .setClanAdvancement(3130, 3135, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true)
                .setPrototypeFactions(F_CHH).setProductionFactions(F_CHH);
        return misc;
    }

    // Armor (Mech/Vehicle/Fighter)

    public static MiscType createISCommercialArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_COMMERCIAL);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_COMMERCIAL, false));
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_COMMERCIAL, true));
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.industrial = true;
        misc.flags = misc.flags.or(F_COMMERCIAL_ARMOR).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "205, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_B)
                .setAvailability(RATING_B, RATING_B, RATING_A, RATING_A)
                .setAdvancement(2290, 2300, 2310)
                .setApproximate(true, true, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);

        return misc;
    }

    public static MiscType createISPrimitiveArmor() {
        //TODO
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PRIMITIVE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PRIMITIVE, false));
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.industrial = true;
        misc.flags = misc.flags.or(F_PRIMITIVE_ARMOR).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "125, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_C, RATING_B, RATING_B)
                .setISAdvancement(DATE_ES, 2290, 2315, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, true, false, false)
                .setClanAdvancement(DATE_ES, 2290, 2315, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, true, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);

        return misc;
    }

    public static MiscType createPrimitiveFighterArmor() {

        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PRIMITIVE_FIGHTER);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PRIMITIVE_FIGHTER, false));
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.industrial = true;
        misc.flags = misc.flags.or(F_PRIMITIVE_ARMOR).or(F_FIGHTER_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "125, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_C, RATING_B, RATING_B)
                .setISAdvancement(DATE_ES, 2300, 2315).setISApproximate(false, true, true)
                .setClanApproximate(false, true, true, false, false)
                .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISIndustrialArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_INDUSTRIAL);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_INDUSTRIAL, false));
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_INDUSTRIAL, true));
        misc.addLookupName("Clan Industrial Armor");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.industrial = true;
        misc.flags = misc.flags.or(F_INDUSTRIAL_ARMOR).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "205, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_C, RATING_B, RATING_B)
                .setAdvancement(2430, 2439, 2439)
                .setApproximate(true, true, true).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    // TODO - At some point the "Standard" below needs to be broken out as they
    // all have Separate Tech Advancement information.

    public static MiscType createStandard() {
        // This is not really a single piece of equipment, it is used to
        // identify "standard" internal structure, armor, whatever.

        MiscType misc = new MiscType();

        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_STANDARD);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_STANDARD));
        misc.addLookupName(EquipmentType.getStructureTypeName(T_STRUCTURE_STANDARD, false));
        misc.addLookupName(EquipmentType.getStructureTypeName(T_STRUCTURE_STANDARD, true));
        misc.addLookupName(EquipmentType.getArmorTypeName(T_ARMOR_STANDARD, false));
        misc.addLookupName(EquipmentType.getArmorTypeName(T_ARMOR_STANDARD, true));
        misc.addLookupName("Regular");
        misc.addLookupName("IS Standard Armor");
        misc.addLookupName("Clan Standard Armor");
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_PROTOMECH_EQUIPMENT);
        misc.criticals = 0;

        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setIntroLevel(true);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createISHeavyIndustrialArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL, false));
        misc.addLookupName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL, true));
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.industrial = true;
        misc.flags = misc.flags.or(F_HEAVY_INDUSTRIAL_ARMOR).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "205, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_B)
                .setAdvancement(2460, 2470, 2470)
                .setApproximate(false, true, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createAntiPenetrativeAblation() {
        MiscType misc = new MiscType();
        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION);
        misc.setInternalName("IS " + misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_ANTI_PENETRATIVE_ABLATIVE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "86, IO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS        
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                .setISAdvancement(3100, 3114,  DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISBallisticReinforced() {
        MiscType misc = new MiscType();
        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BALLISTIC_REINFORCED);
        misc.setInternalName("IS " + misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_BALLISTIC_REINFORCED).or(F_MECH_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "87, IO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS   
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                .setISAdvancement(3120, 3131, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createFerroFibrousPrototype() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO, false));
        misc.addLookupName("IS Ferro-Fibrous Armor Prototype");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_FERRO_FIBROUS_PROTO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_VTOL_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "72, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_X, RATING_X)
                .setISAdvancement(2557, DATE_NONE, DATE_NONE, 2571, 3034)
                .setISApproximate(true, false, false, true, true).setPrototypeFactions(F_TH)
                .setReintroductionFactions(F_LC, F_DC);
        return misc;
    }

    public static MiscType createISFerroFibrous() {
        MiscType misc = new MiscType();
        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS, false));
        misc.addLookupName("IS Ferro-Fibrous Armor");
        misc.addLookupName("IS Ferro Fibre");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_FERRO_FIBROUS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "205, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_D, RATING_C).setISAdvancement(2557, 2571, 3055, 2810, 3040)
                .setISApproximate(false, false, false, true, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC);
        return misc;
    }

    public static MiscType createFerroAlumPrototype() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_ALUM_PROTO);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_ALUM_PROTO, false));
        misc.addLookupName("IS Ferro-Alum Armor Prototype");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_FERRO_FIBROUS_PROTO).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "72, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_X, RATING_X)
                .setISAdvancement(2557, DATE_NONE, DATE_NONE, 2571, 3034)
                .setISApproximate(true, false, false, true, true).setPrototypeFactions(F_TH)
                .setReintroductionFactions(F_LC, F_DC);
        return misc;
    }

    public static MiscType createISFerroAlum() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_ALUM);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_ALUM, false));
        misc.addLookupName("IS Ferro-Aluminum Armor");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_FERRO_FIBROUS).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "205, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_D, RATING_C).setISAdvancement(2557, 2571, 3055, 2810, 3040)
                .setISApproximate(false, false, false, true, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC);
        return misc;
    }

    public static MiscType createCLFerroAlum() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_ALUM);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_ALUM, true));
        misc.addLookupName("Clan Ferro-Aluminum Armor");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_FERRO_FIBROUS).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "205, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_E, RATING_D, RATING_C)
                .setClanAdvancement(2820, 2825, 2830, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CSR)
                .setProductionFactions(F_CSR);
        return misc;
    }

    public static MiscType createCLFerroFibrous() {
        MiscType misc = new MiscType();
        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS, true));
        misc.addLookupName("Clan Ferro-Fibrous Armor");
        misc.addLookupName("Clan Ferro Fibre");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_FERRO_FIBROUS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "205, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_E, RATING_D, RATING_C)
                .setClanAdvancement(2820, 2825, 2830, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CSR)
                .setProductionFactions(F_CSR);

        return misc;
    }

    public static MiscType createLightFerroFibrous() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LIGHT_FERRO);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LIGHT_FERRO, false));
        misc.addLookupName("IS Light Ferro-Fibrous Armor");
        misc.addLookupName("IS LightFerro");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_LIGHT_FERRO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "205, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3067, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_FW);
        return misc;
    }

    public static MiscType createLightFerroAlum() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LIGHT_ALUM);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LIGHT_ALUM, false));
        misc.addLookupName("IS Light Ferro-Aluminum Armor");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_LIGHT_FERRO).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "205, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3067, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_FW);
        return misc;
    }

    public static MiscType createHeavyFerroFibrous() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_FERRO);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_FERRO, false));
        misc.addLookupName("IS Heavy Ferro-Fibrous Armor");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_HEAVY_FERRO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "205, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3056, 3069, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_LC);
        return misc;
    }

    public static MiscType createHeavyFerroAlum() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_ALUM);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_ALUM, false));
        misc.addLookupName("IS Heavy Ferro-Aluminum Armor");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_HEAVY_FERRO).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "205, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3056, 3069, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_LC);
        return misc;
    }

    public static MiscType createCLFerroLamellorArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_LAMELLOR);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_LAMELLOR, true));
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_FERRO_LAMELLOR).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "279, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS   
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3070, 3109, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CSR)
                .setProductionFactions(F_CSR).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISHardenedArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HARDENED);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HARDENED, false));
        misc.addLookupName("Clan Hardened");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_HARDENED_ARMOR).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "280, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3047, 3081, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(3061, 3081, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_FS, F_LC, F_CGB)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISHeatDissipating() {
        MiscType misc = new MiscType();
        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAT_DISSIPATING);
        misc.setInternalName("IS " + misc.name);
        misc.addLookupName("Clan Heat-Dissipating");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_HEAT_DISSIPATING).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "87, IO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS   
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                .setISAdvancement(3115, 3123, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(3115, 3126, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISImpactResistant() {
        MiscType misc = new MiscType();
        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_IMPACT_RESISTANT);
        misc.setInternalName("IS " + misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_IMPACT_RESISTANT).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "87, IO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS   
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                .setISAdvancement(3090, 3103, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISReflective() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REFLECTIVE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REFLECTIVE, false));
        misc.addLookupName("IS Reflective Armor");
        misc.addLookupName("IS Reflective");
        misc.tonnage = 0;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_REFLECTIVE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "280, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3058, 3080, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createCLReflective() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REFLECTIVE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REFLECTIVE, true));
        misc.addLookupName("Clan Reflective Armor");
        misc.addLookupName("Clan Reflective");
        misc.tonnage = 0;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_REFLECTIVE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "280, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3061, 3080, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CJF)
                .setProductionFactions(F_CJF).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        return misc;
    }

    public static MiscType createISModularArmor() {
        MiscType misc = new MiscType();

        misc.name = "Modular Armor";
        misc.setInternalName("ISModularArmor");
        misc.addLookupName("IS Modular Armor");
        misc.addLookupName("CLModularArmor");
        misc.addLookupName("Clan Modular Armor");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 100000;
        misc.flags = misc.flags.or(F_MODULAR_ARMOR).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        misc.bv = BV_VARIABLE;
        misc.damageTaken = 0;
        misc.baseDamageAbsorptionRate = 10;
        misc.baseDamageCapacity = 10;
        misc.rulesRefs = "281, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS   
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3072, 3096, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(3074, 3096, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CS, F_CWX)
                .setProductionFactions(F_RS).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISReactive() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REACTIVE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REACTIVE, false));
        misc.addLookupName("IS Reactive Armor");
        misc.addLookupName("IS Reactive");
        misc.tonnage = 0;
        misc.criticals = CRITICALS_VARIABLE;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_REACTIVE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT)
            .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "282, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3063, 3081, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createCLReactive() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REACTIVE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REACTIVE, true));
        misc.addLookupName("Clan Reactive Armor");
        misc.addLookupName("Clan Reactive");
        misc.tonnage = 0;
        misc.criticals = CRITICALS_VARIABLE;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_REACTIVE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "282, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3065, 3081, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CGB)
                .setProductionFactions(F_CGB).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createMekStealth() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_STEALTH);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_STEALTH, false));
        misc.addLookupName("IS Stealth Armor");
        misc.tonnage = 0; // ???
        misc.criticals = 12;
        misc.tankslots = 0;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_STEALTH).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(false);
        misc.bv = 0;
        misc.rulesRefs = "206, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3051, 3063, 3072, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC);
        return misc;
    }

    public static MiscType createVehicularStealth() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_STEALTH_VEHICLE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_STEALTH_VEHICLE, false));
        misc.addLookupName("IS Vehicular Stealth Armor");
        misc.tonnage = 0; // ???
        // Has to be 1, because we allocate 2 of them, so 2*1=2, which is
        // correct
        // When this was 2, it was ending up as 2*2=4 slots used on the tank.
        // Bad juju.
        misc.tankslots = 1;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_STEALTH).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_VTOL_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(false);
        misc.bv = 0;
        misc.rulesRefs = "282, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(DATE_NONE, 3067, 3084, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false).setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    // Armor (Small Craft, and Large Aerospace Craft)

    public static MiscType createPrimitiveLCAerospaceArmor() {

        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PRIMITIVE_AERO);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PRIMITIVE_AERO, false));
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.industrial = true;
        misc.flags = misc.flags.or(F_PRIMITIVE_ARMOR).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
                .or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "125, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_C, RATING_B, RATING_B)
                .setISAdvancement(DATE_ES, 2300, 2315).setISApproximate(false, true, true)
                .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }
    
    // Separate IS/Clan standard aerospace armor, which provides different points per ton.
    public static MiscType createISAeroSpaceArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_AEROSPACE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_AEROSPACE, false));
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
                .or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "205, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_D)
            .setAvailability(RATING_C, RATING_C, RATING_C, RATING_B)
            .setISAdvancement(2460, 2470, 2470).setISApproximate(true, false, false)
            .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }
    
    public static MiscType createClanAeroSpaceArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_AEROSPACE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_AEROSPACE, true));
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
                .or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "205, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_D)
            .setAvailability(RATING_C, RATING_C, RATING_C, RATING_B)
            .setClanAdvancement(DATE_NONE, DATE_NONE, 2470)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }
    
    public static MiscType createISImpFerroAluminumArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_IMP);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_IMP, false));
        misc.addLookupName(misc.name);
        misc.addLookupName("ImprovedFerroAluminum");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_CAPITAL_ARMOR).or(F_IMP_FERRO).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "152,SO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(2500, 2520, DATE_NONE, 2950, 3052)
                .setISApproximate(true, true, false, false, false)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }
    
    public static MiscType createClanImpFerroAluminumArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_IMP);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_IMP, true));
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_CAPITAL_ARMOR).or(F_IMP_FERRO).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "152,SO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(2500, 2520).setClanApproximate(false, true)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }
    
    public static MiscType createISFerroCarbideArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_CARBIDE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_CARBIDE, false));
        misc.addLookupName("Ferro-Carbide");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_CAPITAL_ARMOR).or(F_FERRO_CARBIDE).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "152,SO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(2550, 2570, DATE_NONE, 2950, 3055).setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC, F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }
    
    public static MiscType createClanFerroCarbideArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_CARBIDE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_CARBIDE, true));
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_CAPITAL_ARMOR).or(F_FERRO_CARBIDE).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "152,SO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(2550, 2570, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }
    
    public static MiscType createISLamellorFerroCarbideArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE, false));
        misc.addLookupName(misc.name);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = true;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_CAPITAL_ARMOR).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "152,SO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_F)
                .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(2600, 2615, DATE_NONE, 2950, 3055).setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2600, 2615, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_FW, F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }
    
    public static MiscType createClanLamellorFerroCarbideArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE, true));
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = true;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_CAPITAL_ARMOR).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "152,SO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                .setClanAdvancement(2600, 2615, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }
    

    // Armor (ProtoMech)

    // TODO Protomech Armor IO pg 36

    public static MiscType createElectricDischargeArmor() {
        // TODO: add game rules for this
        MiscType misc = new MiscType();
        misc.name = EquipmentType.getArmorTypeName(T_ARMOR_EDP);
        misc.setInternalName("CLEDPArmor");
        misc.addLookupName(EquipmentType.getArmorTypeName(T_ARMOR_EDP, true));
        misc.addLookupName(EquipmentType.getArmorTypeName(T_ARMOR_EDP));
        misc.shortName = "EDP";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_PROTOMECH_EQUIPMENT).or(F_ELECTRIC_DISCHARGE_ARMOR);
        misc.omniFixedOnly = true;
        misc.bv = 32;
        String[] modes = { "not charging", "charging" };
        misc.setModes(modes);
        misc.rulesRefs = "64, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                .setClanAdvancement(3071, DATE_NONE, DATE_NONE, 3085, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CFM)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    // Armor (Battle Armor Infantry)

    public static MiscType createISBAStandardArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STANDARD);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STANDARD, false));
        misc.addLookupName("IS BA Standard (Basic)");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "252, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(2680, DATE_NONE, 3054, DATE_NONE, 3050)
                .setISApproximate(true, false, true, false, false)
                .setPrototypeFactions(F_TH)
                .setReintroductionFactions(F_FS, F_LC, F_DC).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);

        return misc;
    }

    public static MiscType createClanBAStandardArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STANDARD);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STANDARD, true));
        misc.addLookupName("Clan BA Standard (Basic)");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "252, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setClanAdvancement(DATE_NONE, 2868, 3054).setClanApproximate(true, true, false)
                .setProductionFactions(F_CWF).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);

        return misc;
    }

    public static MiscType createISBAStandardPrototypeArmor() {
        MiscType misc = new MiscType();

        misc.name = BattleArmor.STANDARD_PROTOTYPE;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STANDARD_PROTOTYPE));
        misc.addLookupName("IS BA Standard (Prototype)");
        // misc.addLookupName("Clan BA Standard (Prototype)");
        misc.tonnage = 0;
        misc.criticals = 4;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.techAdvancement.setTechBase(TECH_BASE_IS)
                .setISAdvancement(3050, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH, F_FS, F_LC, F_DC)
                .setProductionFactions(F_TH, F_FS, F_LC, F_DC).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return misc;
    }

    public static MiscType createISBAAdvancedArmor() {
        MiscType misc = new MiscType();

        misc.name = BattleArmor.ADVANCED_ARMOR;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STANDARD_ADVANCED));
        misc.addLookupName("IS BA Advanced");
        misc.tonnage = 0;
        misc.criticals = 5;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "252, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(DATE_NONE, 3057, 3060)
                .setProductionFactions(F_FW).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);

        return misc;
    }

    public static MiscType createClanBAFireResistantArmor() {
        MiscType misc = new MiscType();

        misc.name = BattleArmor.FIRE_RESISTANT;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_FIRE_RESIST));
        misc.addLookupName("Clan BA Fire Resistant");
        misc.tonnage = 0;
        misc.criticals = 5;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_FIRE_RESISTANT).or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "253, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3052, 3058, 3065)
                .setClanApproximate(true, false, false).setPrototypeFactions(F_CFM)
                .setProductionFactions(F_CFM).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);

        return misc;
    }

    public static MiscType createISBAStealthPrototype() {
        MiscType misc = new MiscType();

        misc.name = BattleArmor.STEALTH_PROTOTYPE;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE));
        misc.addLookupName("IS BA Stealth (Prototype)");
        misc.addLookupName("Clan BA Stealth (Prototype)");
        misc.tonnage = 0;
        misc.criticals = 4;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "252, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3050, 3052, 3054, 3055, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X);

        return misc;
    }

    public static MiscType createISBABasicStealth() {
        MiscType misc = new MiscType();

        misc.name = BattleArmor.BASIC_STEALTH_ARMOR;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH_BASIC, false));
        misc.addLookupName("IS BA Stealth (Basic)");
        misc.tonnage = 0;
        misc.criticals = 3;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "252, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(2700, 2710, 3054, 2770, 3052)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_E, RATING_D);

        return misc;
    }

    public static MiscType createClanBABasicStealth() {
        MiscType misc = new MiscType();

        misc.name = BattleArmor.BASIC_STEALTH_ARMOR;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH_BASIC, true));
        misc.addLookupName("Clan BA Stealth (Basic)");
        misc.tonnage = 0;
        misc.criticals = 3;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "252, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3054).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_E, RATING_D);

        return misc;
    }

    public static MiscType createISBAStandardStealth() {
        MiscType misc = new MiscType();

        misc.name = BattleArmor.STANDARD_STEALTH_ARMOR;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH, false));
        misc.addLookupName("IS BA Stealth (Standard)");
        misc.addLookupName("IS BA Stealth");
        misc.tonnage = 0;
        misc.criticals = 4;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "252, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(2710, 2720, 3055, 2770, 3053)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_X, RATING_E, RATING_D);

        return misc;
    }

    public static MiscType createClanBAStandardStealth() {
        MiscType misc = new MiscType();

        misc.name = BattleArmor.STANDARD_STEALTH_ARMOR;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH, true));
        misc.addLookupName("Clan BA Stealth (Standard)");
        misc.addLookupName("Clan BA Stealth");
        misc.tonnage = 0;
        misc.criticals = 4;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "252, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3055).setPrototypeFactions(F_TH)
                .setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_X, RATING_E, RATING_D);

        return misc;
    }

    public static MiscType createISBAImprovedStealth() {
        MiscType misc = new MiscType();
        misc.name = BattleArmor.IMPROVED_STEALTH_ARMOR;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH_IMP, false));
        misc.addLookupName("IS BA Stealth (Improved)");
        misc.tonnage = 0;
        misc.criticals = 5;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "252, TM";

        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3055, 3057, 3059)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FW, F_WB)
                .setProductionFactions(F_FW, F_WB).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);

        return misc;
    }

    public static MiscType createClanBAImprovedStealth() {
        MiscType misc = new MiscType();
        misc.name = BattleArmor.IMPROVED_STEALTH_ARMOR;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH_IMP, true));
        misc.addLookupName("Clan BA Stealth (Improved)");
        misc.tonnage = 0;
        misc.criticals = 5;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "252, TM";

        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setClanAdvancement(DATE_NONE, 3058, 3059)
                .setProductionFactions(F_CSR).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);

        return misc;
    }

    public static MiscType createISBAMimeticCamo() {
        MiscType misc = new MiscType();
        misc.name = BattleArmor.MIMETIC_ARMOR;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_MIMETIC));
        misc.addLookupName("IS BA Mimetic");
        misc.tonnage = 0;
        misc.criticals = 7;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_STEALTH).or(F_VISUAL_CAMO).or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "253, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3058, 3061, 3065, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CS, F_WB)
                .setProductionFactions(F_WB).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);

        return misc;
    }

    public static MiscType createISBAReactiveArmor() {
        MiscType misc = new MiscType();
        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REACTIVE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REACTIVE, false));
        misc.addLookupName("IS BA Reactive (Blazer)");
        misc.addLookupName("IS BA Reactive");
        misc.tonnage = 0;
        misc.criticals = 7;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_REACTIVE);
        misc.bv = 0;
        misc.rulesRefs = "282, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS)
            .setISAdvancement(3075, 3110, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(false, true, false, false, false)
            .setProductionFactions(F_RS).setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        return misc;
    }

    public static MiscType createClanBAReactiveArmor() {
        MiscType misc = new MiscType();
        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REACTIVE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REACTIVE, true));
        misc.addLookupName("Clan BA Reactive (Blazer)");
        misc.addLookupName("Clan BA Reactive");
        misc.tonnage = 0;
        misc.criticals = 7;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_REACTIVE);
        misc.bv = 0;
        misc.rulesRefs = "282, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setClanAdvancement(3075, 3110, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CSF)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        return misc;
    }

    public static MiscType createISBAReflectiveArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REFLECTIVE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REFLECTIVE, false));
        misc.addLookupName("IS BA Laser Reflective (Reflec/Glazed)");
        misc.addLookupName("IS BA Reflective");
        misc.tonnage = 0;
        misc.criticals = 7;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_REFLECTIVE);
        misc.bv = 0;
        misc.rulesRefs = "280, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS)
            .setISAdvancement(3074, 3110, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(false, true, false, false, false)
            .setProductionFactions(F_DC).setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        return misc;
    }

    public static MiscType createClanBAReflectiveArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REFLECTIVE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REFLECTIVE, true));
        misc.addLookupName("Clan BA Laser Reflective (Reflec/Glazed)");
        misc.addLookupName("Clan BA Reflective");
        misc.tonnage = 0;
        misc.criticals = 7;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_REFLECTIVE);
        misc.bv = 0;
        misc.rulesRefs = "280, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setClanAdvancement(3074, 3110, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CNC).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        return misc;
    }

    // Armor (Conventional Infantry)

    public static MiscType createISAblativeStandardInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Ablative, Standard";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISAblativeStandard");
        misc.damageDivisor = 1.0;
        misc.subType = S_ENCUMBERING;
        misc.cost = 1000;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_A, RATING_B, RATING_A, RATING_A)
                .setISAdvancement(DATE_ES, 2300, 2305, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, 2300, 2305, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setProductionFactions(F_TA);
        return misc;
    }

    public static MiscType createISAblativeConcealedInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Ablative, Concealed";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISAblativeConcealed");
        misc.damageDivisor = 1.0;
        misc.cost = 1500;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_D, RATING_B, RATING_B)
                .setISAdvancement(2390, 2400, 2410, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2390, 2400, 2410, 2820, DATE_NONE)
                .setClanApproximate(true, false, false, true, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);

        return misc;
    }

    public static MiscType createISAblativeFlakStandardArmorInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Ablative/Flak, Standard";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISAblativeFlakStandard");
        misc.addLookupName("CLAblativeFlakStandard");
        misc.damageDivisor = 1.0;
        misc.cost = 800;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";

        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_B, RATING_C, RATING_B, RATING_A)
                .setISAdvancement(2300, 2305, 2310, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2300, 2305, 2310, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return misc;
    }

    public static MiscType createISAblativeFlakConcealedArmorInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Ablative/Flak, Concealed";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISAblativeFlakConcealed");
        misc.addLookupName("CLAblativeFlakConcealed");
        misc.damageDivisor = 1.0;
        misc.cost = 1400;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(2390, 2300, 2305, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2390, 2300, 2305, 2825, DATE_NONE)
                .setClanApproximate(true, false, false, true, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createISBallisicPlateStandardInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Ballistic Plate, Standard";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISBallisticPlateStandard");
        misc.addLookupName("CLBallisticPlateStandard");
        misc.damageDivisor = 2.0;
        misc.subType = S_ENCUMBERING;
        misc.cost = 1600;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2305, 2310, 2315, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2305, 2310, 2315, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return misc;
    }

    public static MiscType createBallisicPlateConcealedInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Ballistic Plate, Concealed";
        misc.setInternalName(misc.name);
        misc.addLookupName("BallisticPlateConcealed");
        misc.damageDivisor = 1.0;
        misc.cost = 2880;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";

        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_F, RATING_D, RATING_D)
                .setISAdvancement(2810, 2820, 2822, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC);
        return misc;
    }

    public static MiscType createClothingFatiguesInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Clothing, Fatigues/Civilian/Non-Armored";
        misc.setInternalName(misc.name);
        misc.addLookupName("Fatigues");
        misc.damageDivisor = 1.0;
        misc.cost = 25;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_A)
                .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_PS, DATE_PS)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_PS, DATE_PS)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createClothingLeatherHideInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Clothing, Leather/Synthetic Hide";
        misc.setInternalName(misc.name);
        misc.addLookupName("ClothingLeather");
        misc.damageDivisor = 1.0;
        misc.cost = 100;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_A)
                .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_PS, DATE_PS)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_PS, DATE_PS)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createClothingLightInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Clothing, Light (e.g. Summer Wear/None)";
        misc.setInternalName(misc.name);
        misc.addLookupName("ClothingLightNone");
        misc.damageDivisor = 0.5;
        misc.cost = 15;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_A)
                .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_PS, DATE_PS)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_PS, DATE_PS)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createISEngineeringSuitInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Engineering Suit";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISEngineeringSuit");
        misc.addLookupName("CLEngineeringSuit");
        misc.damageDivisor = 1.0;
        misc.subType = S_ENCUMBERING | S_SPACE_SUIT;
        misc.cost = 7500;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_D, RATING_D, RATING_D, RATING_C)
                .setISAdvancement(2340, 2350, 2351, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2340, 2350, 2351, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC);

        return misc;
    }

    public static MiscType createISEnvironmentSuitLightInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Environment Suit, Light";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISEnvironmentSuitLight");
        misc.addLookupName("CLEnvironmentSuitLight");
        misc.damageDivisor = 1.0;
        misc.subType = S_ENCUMBERING | S_SPACE_SUIT;
        misc.cost = 200;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_ES, DATE_ES, 2100, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, 2100, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createISEnvironmentSuitHostileInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Environment Suit, Hostile";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISEnvironmentSuitHostile");
        misc.addLookupName("CLEnvironmentSuitHostile");
        misc.damageDivisor = 2.0;
        misc.subType = S_ENCUMBERING | S_SPACE_SUIT | S_XCT_VACUUM | S_COLD_WEATHER | S_HOT_WEATHER;
        misc.cost = 10000;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(DATE_ES, 2300, 2302, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, 2300, 2302, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC);
        return misc;
    }

    public static MiscType createISEnvironmentSuitMarineInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Environment Suit, Marine";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISEnvironmentSuitMarine");
        misc.addLookupName("CLEnvironmentSuitMarine");
        misc.damageDivisor = 2.0;
        misc.subType = S_SPACE_SUIT | S_XCT_VACUUM | S_COLD_WEATHER | S_HOT_WEATHER;
        misc.cost = 15000;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_D, RATING_D, RATING_C)
                .setISAdvancement(2315, 2325, 2330, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2315, 2325, 2330, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TC)
                .setProductionFactions(F_TC);
        return misc;
    }

    public static MiscType createISFlakStandardInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Flak, Standard";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISFlakStandard");
        misc.damageDivisor = 1.0;
        misc.cost = 150;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_ES, DATE_ES, 2200, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, 2200, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createISFlakConcealedInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Flak, Concealed";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISFlakConcealed");
        misc.damageDivisor = 1.0;
        misc.cost = 225;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_D, RATING_C, RATING_B, RATING_B)
                .setISAdvancement(DATE_ES, DATE_ES, 2230, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, 2230, 2825, DATE_NONE)
                .setClanApproximate(false, false, false, true, false);
        return misc;
    }

    public static MiscType createISHeatSuitInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Heat Suit";
        misc.setInternalName(misc.name);
        misc.addLookupName("HeatSuit");
        misc.damageDivisor = 1.0;
        misc.subType = S_HOT_WEATHER;
        misc.cost = 100;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_B)
                .setISAdvancement(2350, 2355, 2358, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2350, 2355, 2358, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);

        return misc;
    }

    public static MiscType createISMechWarriorCombatSuitInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "MechWarrior Combat Suit";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISMechWarriorCombatSuit");
        misc.damageDivisor = 1.0;
        misc.cost = 20000;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(2690, 2790, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2690, 2790, 2820, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_DC);
        return misc;
    }

    public static MiscType createISMechWarriorCoolingSuitInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "MechWarrior Cooling Suit";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISMechWarriorCoolingSuit");
        misc.damageDivisor = 1.0;
        misc.subType = S_HOT_WEATHER;
        misc.cost = 500;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_E, RATING_D).setISAdvancement(2680, 2800, 3065, 2850, 3050)
                .setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2480, 2800, 3065, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_CS);
        return misc;
    }

    public static MiscType createMechWarriorCoolingVestInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "MechWarrior Cooling Vest (Only)";
        misc.setInternalName(misc.name);
        misc.addLookupName("MechWarriorCoolingVest");
        misc.damageDivisor = 0.5;
        misc.cost = 200;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2440, 2460, 2461, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2440, 2460, 2461, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createMyomerSuitInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Myomer, Suit";
        misc.setInternalName(misc.name);
        misc.addLookupName("MyomerSuit");
        misc.damageDivisor = 2.0;
        misc.subType = S_ENCUMBERING;
        misc.cost = 5800;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3045, 3047, 3052, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, true, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC);
        return misc;
    }

    public static MiscType createMyomerVestInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Myomer, Vest";
        misc.setInternalName(misc.name);
        misc.addLookupName("MyomerVest");
        misc.damageDivisor = 2.0;
        misc.cost = 1800;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3044, 3045, 3051, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, true, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC);
        return misc;
    }

    public static MiscType createNeoChainMailInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Neo-Chainmail";
        misc.setInternalName(misc.name);
        misc.addLookupName("NeoChainmail");
        misc.damageDivisor = 1.0;
        misc.cost = 920;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setISAdvancement(3062, 3065, 3066, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, true, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return misc;
    }

    public static MiscType createParkaInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Parka";
        misc.setInternalName(misc.name);
        misc.damageDivisor = 1.0;
        misc.cost = 50;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_PS, DATE_PS)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_PS, DATE_PS)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createSnowSuitInfArmor() {
        MiscType misc = new MiscType();
        misc.name = "Snowsuit";
        misc.setInternalName(misc.name);
        misc.addLookupName("SnowSuit");
        misc.damageDivisor = 1.0;
        misc.subType = S_ENCUMBERING | S_COLD_WEATHER;
        misc.cost = 70;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_PS, DATE_PS)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_PS, DATE_PS)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createSpaceSuitInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Spacesuit";
        misc.setInternalName(misc.name);
        misc.damageDivisor = 1.0;
        misc.subType = S_ENCUMBERING | S_SPACE_SUIT | S_XCT_VACUUM | S_COLD_WEATHER;
        misc.cost = 5000;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_ES, DATE_ES)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_ES, DATE_ES)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createSpacesuitCombatInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Spacesuit, Combat";
        misc.setInternalName(misc.name);
        misc.addLookupName("SpacesuitCombat");
        misc.damageDivisor = 1.0;
        misc.subType = S_ENCUMBERING | S_SPACE_SUIT | S_XCT_VACUUM | S_COLD_WEATHER;
        misc.cost = 7000;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_D, RATING_E, RATING_D)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_ES, 2200, DATE_ES)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_ES, 2200, DATE_ES)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCapellanConfederationInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Capellan Confederation Armor Kit (3050)";
        misc.setInternalName(misc.name);
        misc.addLookupName("LiaoKit");
        misc.damageDivisor = 1.0;
        misc.cost = 450;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_C, RATING_B)
                .setISAdvancement(3045, 3050, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC);
        return misc;
    }

    public static MiscType createClanInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Clan Armor Kit (All)";
        misc.setInternalName(misc.name);
        misc.addLookupName("ClanKit");
        misc.damageDivisor = 2.0;
        misc.cost = 5560;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_E, RATING_E, RATING_C)
                .setClanAdvancement(2850, 2900, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CLAN)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createComstarInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "ComStar Infantry Kit";
        misc.setInternalName(misc.name);
        misc.addLookupName("CSInfKit");
        misc.addLookupName("ComstarKit");
        misc.damageDivisor = 2.0;
        misc.cost = 4280;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_F, RATING_D, RATING_E)
                .setISAdvancement(2825, 2830, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CS)
                .setProductionFactions(F_CS);
        return misc;
    }

    public static MiscType createDraconisCombineInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Draconis Combine Infantry Kit";
        misc.setInternalName(misc.name);
        misc.addLookupName("KuritaKit");
        misc.damageDivisor = 1.0;
        misc.cost = 360;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(2620, 2625, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return misc;
    }

    public static MiscType createFedSunsInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Federated Suns Infantry Kit";
        misc.setInternalName(misc.name);
        misc.addLookupName("DavionKit");
        misc.damageDivisor = 1.0;
        misc.cost = 750;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_B, RATING_B, RATING_F)
                .setISAdvancement(2325, 2330, DATE_NONE, 3035, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return misc;
    }

    public static MiscType createFedComInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Fed Suns/Fed Commonweath Infantry Kit (3030-3066)";
        misc.damageDivisor = 1.0;
        misc.setInternalName(misc.name);
        misc.addLookupName("DavionKit3030");
        misc.addLookupName("EarlyFedComKit");
        misc.damageDivisor = 2;
        misc.cost = 1040;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_D, RATING_B, RATING_E)
                .setISAdvancement(3025, 3030, DATE_NONE, 3070, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC);
        return misc;
    }

    public static MiscType createFedSunsLateInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Fed Suns Infantry Kit (3067+)";
        misc.setInternalName(misc.name);
        misc.addLookupName("DavionKit3067");
        misc.addLookupName("LateDavionKit");
        misc.subType = S_ENCUMBERING;
        misc.damageDivisor = 2.0;
        misc.cost = 2080;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_C)
                .setISAdvancement(3065, 3067, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return misc;
    }

    public static MiscType createFRRInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Free Rasalhague Republic Infantry Kit";
        misc.setInternalName(misc.name);
        misc.addLookupName("FRRKit");
        misc.damageDivisor = 1.0;
        misc.cost = 360;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_B, RATING_B)
                .setISAdvancement(3035, 3040, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_FR)
                .setProductionFactions(F_FR);
        return misc;
    }

    public static MiscType createFWLEarlyInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Free Worlds League Infantry Kit";
        misc.setInternalName(misc.name);
        misc.addLookupName("MarikKit");
        misc.damageDivisor = 1.0;
        misc.cost = 950;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_B, RATING_B, RATING_D)
                .setISAdvancement(2280, 2290, DATE_NONE, 3050, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_FW);
        return misc;
    }

    public static MiscType createFWLLateInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Free Worlds League Infantry Kit (3035+)";
        misc.setInternalName(misc.name);
        misc.addLookupName("MarikKit3035");
        misc.addLookupName("LateMarikKit");
        misc.damageDivisor = 2.0;
        misc.subType = S_ENCUMBERING;
        misc.cost = 360;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_E, RATING_B, RATING_B)
                .setISAdvancement(3030, 3035, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_FW);
        return misc;
    }

    public static MiscType createLyranInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Lyran Commonwealth Infantry Kit";
        misc.setInternalName(misc.name);
        misc.addLookupName("SteinerKit");
        misc.damageDivisor = 1.0;
        misc.cost = 650;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_B, RATING_B, RATING_E)
                .setISAdvancement(2420, 2425, DATE_NONE, 3067, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC);
        return misc;
    }

    public static MiscType createLyranLateInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Lyran Alliance/Lyran Commonwealth (3060+) Infantry Kit";
        misc.setInternalName(misc.name);
        misc.addLookupName("SteinerKit3060");
        misc.damageDivisor = 2.0;
        misc.cost = 730;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_D, RATING_B, RATING_E)
                .setISAdvancement(3058, 3060, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC);
        return misc;
    }

    public static MiscType createCanopusInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Magistracy of Canopus Infantry Kit";
        misc.setInternalName(misc.name);
        misc.addLookupName("CanopianKit");
        misc.damageDivisor = 1.0;
        misc.cost = 400;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(2600, 2610, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_MC)
                .setProductionFactions(F_MC);
        return misc;
    }

    public static MiscType createMarianInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Marian Hegemony Infantry Kit";
        misc.setInternalName(misc.name);
        misc.addLookupName("MarianKit");
        misc.damageDivisor = 2.0;
        misc.cost = 1580;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_F, RATING_B, RATING_B)
                .setISAdvancement(3045, 3049, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                .setProductionFactions(F_MH);
        return misc;
    }

    public static MiscType createISSLDFInfArmor() {
        MiscType misc = new MiscType();

        // Stats converted from ATOW
        misc.name = "SLDF Infantry Kit";
        misc.setInternalName(misc.name);
        misc.addLookupName("SLDFKit");
        misc.damageDivisor = 2.0;
        misc.cost = 5000;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "195, AToW-C";
        //Kit never really goes extinct but should be very rare.
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_E)
                .setAvailability(RATING_C, RATING_E, RATING_F, RATING_F)
                .setISAdvancement(2570, 2575, 2580, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2570, 2575, 2580, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, true, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createTaurianInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Taurian Concordat/Calderon Infantry Kit";
        misc.setInternalName(misc.name);
        misc.addLookupName("TaurianKit");
        misc.damageDivisor = 1.0;
        misc.cost = 370;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_B, RATING_B)
                .setISAdvancement(3045, 3047, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TC)
                .setProductionFactions(F_TC, F_CP);
        return misc;
    }

    public static MiscType createWoBInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Word of Blake Infantry Kit";
        misc.setInternalName(misc.name);
        misc.addLookupName("WoBKit");
        misc.damageDivisor = 2.0;
        misc.cost = 4300;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_F)
                .setISAdvancement(3053, 3055, DATE_NONE, 3081, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_WB)
                .setProductionFactions(F_WB);
        return misc;
    }

    public static MiscType createGenericInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Generic Infantry Kit";
        misc.setInternalName(misc.name);
        misc.addLookupName("GenericKit");
        misc.damageDivisor = 1.0;
        misc.cost = 330;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createVintageBulletProofVest() {
        MiscType misc = new MiscType();

        misc.name = "Vintage Bulletproof Vest";
        misc.setInternalName(misc.name);
        misc.addLookupName("BulletproofVest");
        misc.damageDivisor = 1.0;
        misc.cost = 500;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "195, ATOW-C";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_C, RATING_D, RATING_E)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createVintageBombSuit() {
        MiscType misc = new MiscType();

        misc.name = "Vintage Bomb Suit";
        misc.setInternalName(misc.name);
        misc.addLookupName("BombSuit");
        misc.damageDivisor = 1.0;
        misc.subType = S_ENCUMBERING;
        misc.cost = 750;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "195, ATOW-C";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_C, RATING_E, RATING_E)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    // Sneak Suits
    public static MiscType createDESTInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "DEST Infiltration Suit";
        misc.setInternalName(misc.name);
        misc.addLookupName("DESTSuit");
        misc.subType = S_DEST;
        misc.damageDivisor = 1.0;
        misc.cost = 50000;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_E, RATING_E, RATING_E)
                .setISAdvancement(2785, 2800, DATE_NONE, 2845, 3045).setISApproximate(true, true, false, true, false)
                .setPrototypeFactions(F_DC).setProductionFactions(F_DC);

        return misc;
    }

    public static MiscType createISSneakCamoSystemInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Sneak Suit (Camo)";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISSneakSuitCamo");
        misc.damageDivisor = 1.0;
        misc.subType = S_SNEAK_CAMO;
        misc.cost = 7000;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2430, 2450, 2500, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2430, 2450, 2500, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createISSneakIRSystemInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Sneak Suit (IR)";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISSneakSuitIR");
        misc.damageDivisor = 1.0;
        misc.subType = S_SNEAK_IR;
        misc.cost = 7000;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2430, 2450, 2500, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2430, 2450, 2500, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createISSneakECMSystemInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Sneak Suit (ECM)";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISSneakSuitECM");
        misc.damageDivisor = 1.0;
        misc.subType = S_SNEAK_ECM;
        misc.cost = 7000;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2430, 2450, 2500, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2430, 2450, 2500, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    // Two System Sneak Suits

    public static MiscType createISSneakCamoIRInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Sneak Suit (Camo/IR)";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISSneakSuitCamoIR");
        misc.damageDivisor = 1.0;
        misc.subType = S_SNEAK_CAMO | S_SNEAK_IR;
        misc.cost = 21000;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(2445, 2460, 2505, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2445, 2460, 2505, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createISSneakCamoECMInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Sneak Suit (Camo/ECM)";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISSneakSuitCamoECM");
        misc.damageDivisor = 1.0;
        misc.subType = S_SNEAK_CAMO | S_SNEAK_ECM;
        misc.cost = 21000;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(2445, 2460, 2505, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2445, 2460, 2505, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createISSneakIRECMInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Sneak Suit (IR/ECM)";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISSneakSuitIRECM");
        misc.damageDivisor = 1.0;
        misc.subType = S_SNEAK_IR | S_SNEAK_ECM;
        misc.cost = 21000;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(2445, 2460, 2505, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2445, 2460, 2505, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    // Three System Sneak Suits

    public static MiscType createISSneakThreeSystemInfArmor() {
        MiscType misc = new MiscType();

        misc.name = "Sneak Suit (Camo/IR/ECM)";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISSneakSuitCamoIRECM");
        misc.damageDivisor = 1.0;
        misc.subType = S_SNEAK_CAMO | S_SNEAK_IR | S_SNEAK_ECM;
        misc.cost = 28000;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_D, RATING_E, RATING_E, RATING_E)
                .setISAdvancement(2465, 2475, 2510, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2465, 2475, 2510, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createISArmoredMotiveSystem() {
        MiscType misc = new MiscType();
        misc.name = "Armored Motive System";
        misc.setInternalName("ISArmoredMotiveSystem");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.cost = COST_VARIABLE;
        misc.omniFixedOnly = true;
        misc.flags = misc.flags.or(F_ARMORED_MOTIVE_SYSTEM).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "283, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS        
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3071, 3083, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createCLArmoredMotiveSystem() {
        MiscType misc = new MiscType();
        misc.name = "Armored Motive System";
        misc.setInternalName("CLArmoredMotiveSystem");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.cost = COST_VARIABLE;
        misc.omniFixedOnly = true;
        misc.flags = misc.flags.or(F_ARMORED_MOTIVE_SYSTEM).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "283, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3057, 3083, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    // BattleMech Melee Weapons
    public static MiscType createChainWhip() {
        MiscType misc = new MiscType();

        misc.name = "Chain Whip";
        misc.setInternalName(misc.name);
        misc.tonnage = 3;
        misc.criticals = 2;
        misc.cost = 120000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_CHAIN_WHIP;
        misc.bv = 5.175;
        misc.rulesRefs = "289, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3071, 3084, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_WB)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISClaw() {
        MiscType misc = new MiscType();

        misc.name = "Claw";
        misc.setInternalName("ISClaw");
        misc.addLookupName("ClClaw");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_HAND_WEAPON).or(F_MECH_EQUIPMENT);
        misc.subType |= S_CLAW;
        misc.bv = BV_VARIABLE;
        misc.rulesRefs = "289, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(DATE_NONE, 3050, 3110, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_NONE, 3090, 3110, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_LC, F_CJF)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createISFlail() {
        MiscType misc = new MiscType();
        misc.name = "Flail";
        misc.setInternalName("IS Flail");
        misc.addLookupName("Flail");
        misc.tonnage = 5;
        misc.criticals = 4;
        misc.cost = 110000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT);
        misc.subType |= S_FLAIL;
        misc.bv = 11;
        misc.rulesRefs = "289, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(3057, DATE_NONE, 3079, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createHatchet() {
        MiscType misc = new MiscType();

        misc.name = "Hatchet";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT);
        misc.subType |= S_HATCHET;
        misc.bv = BV_VARIABLE;
        misc.rulesRefs = "220, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS)
                .setTechRating(RATING_B).setAvailability(RATING_X, RATING_F, RATING_D, RATING_C)
                .setISAdvancement(3015, 3022, 3025, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_LC, F_FS)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.INTRO);
        return misc;
    }

    public static MiscType createISLance() {
        MiscType misc = new MiscType();
        misc.name = "Lance";
        misc.setInternalName("IS Lance");
        misc.addLookupName("ISLance");
        misc.addLookupName("Lance");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT);
        misc.subType |= S_LANCE;
        misc.bv = BV_VARIABLE;
        misc.rulesRefs = "290, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3064, 3083, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createMace() {
        MiscType misc = new MiscType();

        misc.name = "Mace";
        misc.setInternalName(misc.name);
        misc.addLookupName("THB Mace");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = 130000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_MACE;
        misc.bv = BV_VARIABLE;
        misc.rulesRefs = "290, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_F, RATING_D, RATING_D)
                .setISAdvancement(DATE_NONE, 3061, 3079, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createRetractableBlade() {
        MiscType misc = new MiscType();

        misc.name = "Retractable Blade";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_RETRACTABLE_BLADE;
        misc.bv = BV_VARIABLE;
        misc.setInstantModeSwitch(true);
        String[] modes = { "retracted", "extended" };
        misc.setModes(modes);
        misc.rulesRefs = "236, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_F, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(2400, 2420, 3075, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createISSmallShield() {
        MiscType misc = new MiscType();

        misc.name = "Shield (Small)";
        misc.setInternalName("ISSmallShield");
        misc.addLookupName("Small Shield");
        misc.sortingName = "Shield B";
        misc.tonnage = 2;
        misc.criticals = 3;
        misc.cost = 50000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT);
        misc.subType |= S_SHIELD_SMALL;
        misc.bv = 50;
        misc.setInstantModeSwitch(true);
        String[] modes = { S_NO_SHIELD, S_ACTIVE_SHIELD, S_PASSIVE_SHIELD };
        misc.setModes(modes);
        misc.damageTaken = 0;
        misc.baseDamageAbsorptionRate = 3;
        misc.baseDamageCapacity = 11;
        misc.rulesRefs = "290, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3067, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISMediumShield() {
        MiscType misc = new MiscType();

        misc.name = "Shield (Medium)";
        misc.setInternalName("ISMediumShield");
        misc.addLookupName("Medium Shield");
        misc.sortingName = "Shield C";
        misc.tonnage = 4;
        misc.criticals = 5;
        misc.cost = 100000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT);
        misc.subType |= S_SHIELD_MEDIUM;
        misc.bv = 135;
        misc.setInstantModeSwitch(true);
        String[] modes = { S_NO_SHIELD, S_ACTIVE_SHIELD, S_PASSIVE_SHIELD };
        misc.setModes(modes);
        misc.damageTaken = 0;
        misc.baseDamageAbsorptionRate = 5;
        misc.baseDamageCapacity = 18;
        misc.rulesRefs = "290, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3067, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISLargeShield() {
        MiscType misc = new MiscType();

        misc.name = "Shield (Large)";
        misc.setInternalName("ISLargeShield");
        misc.addLookupName("Large Shield");
        misc.sortingName = "Shield D";
        misc.tonnage = 6;
        misc.criticals = 7;
        misc.cost = 300000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT);
        misc.subType |= S_SHIELD_LARGE;
        misc.bv = 263;
        misc.setInstantModeSwitch(true);
        String[] modes = { S_NO_SHIELD, S_ACTIVE_SHIELD, S_PASSIVE_SHIELD };
        misc.setModes(modes);
        misc.damageTaken = 0;
        misc.baseDamageAbsorptionRate = 7;
        misc.baseDamageCapacity = 25;
        misc.rulesRefs = "290, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3067, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createSpikes() {
        MiscType misc = new MiscType();

        misc.name = "Spikes";
        misc.setInternalName(misc.name);
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_SPIKES).or(F_MECH_EQUIPMENT);
        misc.bv = 4;
        misc.rulesRefs = "290, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_E, RATING_E, RATING_D)
                .setISAdvancement(3051, 3082, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createSword() {
        MiscType misc = new MiscType();
        misc.name = "Sword";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_SWORD;
        misc.bv = BV_VARIABLE;
        misc.rulesRefs = "237, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_F, RATING_D, RATING_C)
                .setISAdvancement(3050, 3058, 3060).setISApproximate(true, false, false)
                .setPrototypeFactions(F_DC).setProductionFactions(F_DC)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createTalons() {
        MiscType misc = new MiscType();

        misc.name = "Talons";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.spreadable = true;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_TALON).or(F_MECH_EQUIPMENT);
        misc.bv = BV_VARIABLE;
        misc.rulesRefs = "290, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3072, 3087, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CJF)
                .setProductionFactions(F_CJF).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISSmallVibroblade() {
        MiscType misc = new MiscType();
        misc.name = "Vibroblade (Small)";
        misc.setInternalName("ISSmallVibroblade");
        misc.addLookupName("Small Vibroblade");
        misc.sortingName = "Vibro B";
        misc.tonnage = 3;
        misc.criticals = 1;
        misc.cost = 150000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT);
        misc.subType |= S_VIBRO_SMALL;
        misc.bv = 12;
        misc.setInstantModeSwitch(true);
        String[] modes = { "Inactive", "Active" };
        misc.setModes(modes);
        misc.rulesRefs = "292, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3065, 3091, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISMediumVibroblade() {
        MiscType misc = new MiscType();
        misc.name = "Vibroblade (Medium)";
        misc.setInternalName("ISMediumVibroblade");
        misc.addLookupName("Medium Vibroblade");
        misc.sortingName = "Vibro C";
        misc.tonnage = 5;
        misc.criticals = 2;
        misc.cost = 400000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT);
        misc.subType |= S_VIBRO_MEDIUM;
        misc.bv = 17;
        misc.setInstantModeSwitch(true);
        String[] modes = { "Inactive", "Active" };
        misc.setModes(modes);
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3065, 3091, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISLargeVibroblade() {
        MiscType misc = new MiscType();

        misc.name = "Vibroblade (Large)";
        misc.setInternalName("ISLargeVibroblade");
        misc.addLookupName("Large Vibroblade");
        misc.sortingName = "Vibro D";
        misc.tonnage = 7;
        misc.criticals = 4;
        misc.cost = 750000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT);
        misc.subType |= S_VIBRO_LARGE;
        misc.bv = 24;
        misc.setInstantModeSwitch(true);
        String[] modes = { "Inactive", "Active" };
        misc.setModes(modes);
        misc.rulesRefs = "292, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3065, 3091, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    // ADDING THE CLUBS FOUND LAYING AROUND TO THIS SECTION.
    public static MiscType createTreeClub() {
        MiscType misc = new MiscType();

        misc.name = "Tree Club";
        misc.setInternalName(EquipmentTypeLookup.TREE_CLUB);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT);
        misc.subType |= S_TREE_CLUB | S_CLUB;
        misc.bv = 0;

        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_X });
        return misc;
    }

    public static MiscType createGirderClub() {
        MiscType misc = new MiscType();

        misc.name = "Girder Club";
        misc.setInternalName(EquipmentTypeLookup.GIRDER_CLUB);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT);
        misc.subType |= S_CLUB;
        misc.bv = 0;
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_X });
        return misc;
    }

    public static MiscType createLimbClub() {
        MiscType misc = new MiscType();

        misc.name = "Limb Club";
        misc.setInternalName(EquipmentTypeLookup.LIMB_CLUB);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.flags = misc.flags.or(F_CLUB);
        misc.subType |= S_CLUB;
        misc.bv = 0;
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_X });
        return misc;
    }

    // C3 Systems. - Master Units under Weapons.

    public static MiscType createC3S() {
        MiscType misc = new MiscType();

        misc.name = "C3 Computer (Slave)";
        misc.setInternalName("ISC3SlaveUnit");
        misc.addLookupName("IS C3 Slave");
        misc.addLookupName("C3 Computer [Slave]");
        misc.shortName = "C3 Slave";
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 250000;
        misc.flags = misc.flags.or(F_C3S).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "209, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3039, 3050, 3065, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createC3I() {
        MiscType misc = new MiscType();

        misc.name = "Improved C3 Computer (C3I)";
        misc.setInternalName("ISC3iUnit");
        misc.addLookupName("ISImprovedC3CPU");
        misc.addLookupName("IS C3i Computer");
        misc.shortName = "C3i";
        misc.tonnage = 2.5f;
        misc.criticals = 2;
        misc.svslots = 1;
        misc.cost = 750000;
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        //December 2021 - Errata request to change common date
        misc.flags = misc.flags.or(F_C3I).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.bv = 0;
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_X)
                .setISAdvancement(3052, DATE_NONE, 3058, 3085, DATE_NONE)
                .setISApproximate(true, false, false, false, false).
                setPrototypeFactions(F_CS)
                .setProductionFactions(F_CS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createC3SBS() {
        MiscType misc = new MiscType();
        misc.name = "C3 Boosted System (Slave)";
        misc.setInternalName("ISC3BoostedSystemSlaveUnit");
        misc.addLookupName("IS C3 Boosted System Slave");
        misc.addLookupName("C3 Boosted System (C3BS) [Slave]");
        misc.shortName = "C3 Boosted Slave";
        misc.tonnage = 3;
        misc.criticals = 2;
        misc.cost = 500000;
        misc.flags = misc.flags.or(F_C3SBS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "298, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3073, 3100, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createEmergencyC3M() {
        MiscType misc = new MiscType();

        misc.name = "C3 Emergency Master";
        misc.shortName = "C3 Emergency Master";
        misc.setInternalName("ISC3EmergencyMaster");
        misc.addLookupName("Emergency C3 Master");
        misc.addLookupName("C3 Emergency Master (C3EM)");
        misc.tonnage = 2;
        misc.criticals = 2;
        misc.tankslots = 1;
        misc.cost = 2800000;
        // TODO: implement game rules
        misc.flags = misc.flags.or(F_C3EM).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT).or(F_C3S).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "298, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3071, 3099, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    // TODO C3 Remote Sensor Launcher - See IO pg 38 (will likely need to be
    // added as weapon)

    public static MiscType createBC3() {
        MiscType misc = new MiscType();

        misc.name = "Battle Armor C3";
        misc.setInternalName("BattleArmorC3");
        misc.addLookupName("IS BattleArmor C3");
        misc.addLookupName("Battle Armor C3 (BC3)");
        misc.shortName = "BC3";
        misc.tonnage = .250;
        misc.criticals = 1;
        misc.cost = 62500;
        misc.flags = misc.flags.or(F_C3S).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "297, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(3073, 3095, DATE_NONE, DATE_NONE,DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createBC3i() {
        MiscType misc = new MiscType();

        misc.name = "Battle Armor Improved C3";
        misc.setInternalName("ISBC3i");
        misc.addLookupName("IS BC3i");
        misc.addLookupName("IS BattleArmor C3i");
        misc.addLookupName("Battle Armor Improved C3 (BC3I)");
        misc.shortName = "BC3i";
        misc.tonnage = .350;
        misc.criticals = 1;
        misc.cost = 125000;
        misc.flags = misc.flags.or(F_C3I).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "297, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(3063, 3095, DATE_NONE, DATE_NONE,DATE_NONE)
                .setISApproximate(false, true, false, false, false)                
                .setPrototypeFactions(F_WB)
                .setProductionFactions(F_RS).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    // CELLULAR AMMUNITION STORAGE EQUIPMENT (CASE)

    public static MiscType createISCASE() {
        MiscType misc = new MiscType();

        misc.name = "CASE";
        misc.setInternalName("ISCASE");
        misc.addLookupName("IS CASE");
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_CASE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.cost = 50000;
        misc.bv = 0;
        misc.rulesRefs = "210, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_C, RATING_F, RATING_D, RATING_C).setISAdvancement(2452, 2476, 3045, 2840, 3036)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC);
        return misc;
    }

    public static MiscType createCASEPrototype() {
        MiscType misc = new MiscType();
        // TODO Game rules - See IO pg 71 (specifically the explosion part)

        misc.name = "CASE-P (Prototype)";
        misc.setInternalName("ISCASEPrototype");
        misc.addLookupName("Prototype CASE");
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_CASEP).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.cost = 150000;
        misc.bv = 0;
        misc.rulesRefs = "71, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2452, DATE_NONE, DATE_NONE, 2476, DATE_NONE)
                .setISApproximate(true, false, false, true, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createCLCASE() {
        MiscType misc = new MiscType();

        misc.name = "CASE";
        misc.setInternalName(EquipmentTypeLookup.CLAN_CASE);
        misc.addLookupName("Clan CASE");
        misc.tonnage = 0.0f;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_CASE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.cost = 50000;
        misc.bv = 0;
        misc.rulesRefs = "210, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_F, RATING_D, RATING_C)
                .setClanAdvancement(2824, 2825, 2828, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return misc;
    }

    public static MiscType createISCASEII() {
        MiscType misc = new MiscType();

        misc.name = "CASE II";
        misc.setInternalName("ISCASEII");
        misc.addLookupName("IS CASE II");
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_CASEII).or(F_MECH_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.cost = 175000;
        misc.bv = 0;
        misc.rulesRefs = "299, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setISAdvancement(DATE_NONE, 3064, 3082, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_FW)
                .setProductionFactions(F_RS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createCLCASEII() {
        MiscType misc = new MiscType();

        misc.name = "CASE II";
        misc.setInternalName("CLCASEII");
        misc.addLookupName("Clan CASE II");
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_CASEII).or(F_MECH_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.cost = 175000;
        misc.bv = 0;
        misc.rulesRefs = "299, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setClanAdvancement(DATE_NONE, 3062, 3082, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CWF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    // Defensive Pods (B-pods and M-Pods are in common.weapons.defensivepods)

    public static MiscType createISAPPod() {
        MiscType misc = new MiscType();

        misc.name = "Anti-Personnel Pods (A-Pods)";
        misc.setInternalName("ISAntiPersonnelPod");
        misc.addLookupName("ISAPod");
        misc.addLookupName("IS A-Pod");
        misc.addLookupName("IS AP Pod");
        misc.addLookupName("CLAntiPersonnelPod");
        misc.addLookupName("Clan A-Pod");
        misc.addLookupName("CL AP Pod");
        misc.shortName = "A-Pod";
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.cost = 1500;
        misc.flags = misc.flags.or(F_AP_POD).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.bv = 1;
        misc.rulesRefs = "204, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_X, RATING_X, RATING_D, RATING_C)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3055, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2845, 2850, 3055, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CGB)
                .setProductionFactions(F_CGB);
        return misc;
    }

    // Cockpit Systems - Most Cockpit Systems are in the Techconstants.java.

    public static MiscType createISTankCommandConsole() {
        MiscType misc = new MiscType();
        misc.name = "Cockpit Command Console";
        misc.setInternalName("ISTankCockpitCommandConsole");
        misc.addLookupName("CLTankCockpitCommandConsole");
        misc.tonnage = 3;
        misc.cost = 500000;
        misc.flags = misc.flags.or(F_COMMAND_CONSOLE).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "300, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(2625, 2631, DATE_NONE, 2850, 3030)
                .setISApproximate(true, false, false, true, true)
                .setClanAdvancement(2625, 2631, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISMASS() {
        MiscType misc = new MiscType();

        misc.name = "MechWarrior Aquatic Survival System (MASS)";
        misc.setInternalName("ISMASS");
        misc.addLookupName("IS Mass");
        misc.addLookupName("Clan Mass");
        misc.addLookupName("CLMass");
        misc.shortName = "MASS";
        misc.tonnage = 1.5;
        misc.criticals = 1;
        misc.bv = 9;
        misc.cost = 4000;
        misc.flags = misc.flags.or(F_MASS).or(F_MECH_EQUIPMENT);
        misc.rulesRefs = "325, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setISAdvancement(3048, 3083, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(3062, 3083, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_FS, F_CGS).setProductionFactions(F_FS)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createBattleMechNeuralInterfaceUnit() {
        MiscType misc = new MiscType();
        // TODO - not sure how we capturing this in code, Maybe a quirk would be
        // better.

        misc.name = "Direct Neural Interface Cockpit Modification";
        misc.setInternalName("BABattleMechNIU");
        misc.tonnage = 0.0;
        misc.criticals = 0;
        misc.cost = 500000;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_BATTLEMECH_NIU).or(F_BA_EQUIPMENT);

        misc.rulesRefs = "68, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_F)
                .setISAdvancement(3052, 3055, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_WB);
        return misc;
    }

    // TODO - Damage Interupt Circuit - IO pg 39
    // Maybe the helmets should be quirks?
    // TODO - SLDF Advanced Neurohelmet (MechWarrior) - IO pg 40
    // TODO - SLDF Advanced Neurohelmet (Fighter Pilot) - IO pg 40
    // TODO - Virtual Reality Piloting Pod - IO pg 70

    // Drone and Robotic Systems

    public static MiscType createISDroneCarrierControlSystem() {
        // TODO: add game rules for this

        MiscType misc = new MiscType();
        misc.name = "Drone (Remote) Carrier Control System";
        misc.setInternalName("ISDroneCarrierControlSystem");
        misc.addLookupName("CLDroneCarrierControlSystem");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.criticals = 1;
        misc.tankslots = 1;
        misc.flags = misc.flags.or(F_DRONE_CARRIER_CONTROL).or(F_VARIABLE_SIZE)
                .or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.rulesRefs = "305, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_E, RATING_F, RATING_F, RATING_E)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISDroneOperatingSystem() {
        // TODO: add game rules for this

        MiscType misc = new MiscType();
        misc.name = "Drone (Remote) Operating System";
        misc.setInternalName("ISDroneOperatingSystem");
        misc.addLookupName("CLDroneOperatingSystem");
        misc.addLookupName("ISDroneControlConsole");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.criticals = 1;
        misc.tankslots = 1;
        misc.omniFixedOnly = true;
        misc.flags = misc.flags.or(F_DRONE_OPERATING_SYSTEM).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_DS_EQUIPMENT)
                .or(F_JS_EQUIPMENT).or(F_SS_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.rulesRefs = "306, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_E, RATING_F, RATING_F, RATING_E)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISRemoteDroneCommandConsole() {

        MiscType misc = new MiscType();
        misc.name = "Remote Drone Command Console";
        misc.setInternalName("ISRemoteDroneCommandConsole");
        misc.tonnage = 2;
        misc.criticals = 1;
        misc.cost = 50000;
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT).or(F_REMOTE_DRONE_COMMAND_CONSOLE);
        misc.rulesRefs = "90, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
                .setISAdvancement(3125, 3140, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_RS)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createSmartRoboticControlSystem() {
        // TODO Game Rules.
        MiscType misc = new MiscType();
        misc.name = "Smart Robotic Control System (SRCS)";
        misc.setInternalName("SmartRoboticControlSystem");
        misc.shortName = "SRCS";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.rulesRefs = "140, IO";
        misc.flags = misc.flags.or(F_SRCS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_JS_EQUIPMENT).or(F_SS_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.subType = S_STANDARD;
        misc.omniFixedOnly = true;
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
        .setTechRating(RATING_C).setAvailability(RATING_E, RATING_F, RATING_F, RATING_F)
        .setAdvancement(DATE_ES, DATE_ES).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        return misc;
    }

    public static MiscType createImprovedSmartRoboticControlSystem() {
        // TODO Game Rules.
        MiscType misc = new MiscType();
        misc.name = "Smart Robotic Control System (SRCS)(Improved)";
        misc.setInternalName("ImprovedSmartRoboticControlSystem");
        misc.addLookupName("ImprovedSRCS");
        misc.shortName = "Improved SRCS";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.rulesRefs = "140, IO";
        misc.flags = misc.flags.or(F_SRCS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_JS_EQUIPMENT).or(F_SS_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.subType = S_IMPROVED;
        misc.omniFixedOnly = true;
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_C).setAvailability(RATING_E, RATING_F, RATING_F, RATING_F)
                .setAdvancement(DATE_ES, DATE_ES).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        return misc;
    }

    public static MiscType createShieldedAeroSRCS() {
        // TODO Game Rules.
        MiscType misc = new MiscType();
        misc.name = "Shielded Aerospace Smart Robotic Control System";
        misc.setInternalName("ShieldedAeroSRCS");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_SASRCS).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT)
                .or(F_SS_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.subType = S_STANDARD;
        misc.rulesRefs = "141, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_C).setAvailability(RATING_E, RATING_F, RATING_F, RATING_F)
                .setISAdvancement(2755, DATE_ES, DATE_NONE, 2780, 3077)
                .setClanAdvancement(2755, DATE_ES).setReintroductionFactions(F_WB)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return misc;
    }

    public static MiscType createImprovedShieldedAeroSRCS() {
        // TODO Game Rules.
        MiscType misc = new MiscType();
        misc.name = "Shielded Aerospace Smart Robotic Control System (Improved)";
        misc.setInternalName("ImprovedShieldedAeroSRCS");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_SASRCS).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT)
                .or(F_SS_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.subType = S_IMPROVED;
        misc.rulesRefs = "141, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_C).setAvailability(RATING_E, RATING_F, RATING_F, RATING_F)
                .setISAdvancement(2755, DATE_ES, DATE_NONE, 2780, 3077)
                .setClanAdvancement(2755, DATE_ES).setReintroductionFactions(F_WB)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return misc;
    }

    public static MiscType createEliteShieldedAeroSRCS() {
        // TODO Game Rules.
        MiscType misc = new MiscType();
        misc.name = "Shielded Aerospace Smart Robotic Control System (Elite)";
        misc.setInternalName("EliteShieldedAeroSRCS");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_SASRCS).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT)
                .or(F_SS_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.subType = S_ELITE;
        misc.rulesRefs = "141, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_C).setAvailability(RATING_E, RATING_F, RATING_F, RATING_F)
                .setISAdvancement(2755, DATE_ES, DATE_NONE, 2780, 3077)
                .setClanAdvancement(2755, DATE_ES).setReintroductionFactions(F_WB)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return misc;
    }

    public static MiscType createCasparDroneControlSystem() {
        // TODO Game Rules.
        MiscType misc = new MiscType();
        misc.name = "SDS (Caspar) Drone Control System";
        misc.setInternalName("CasparDroneControlSystem");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_CASPAR).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.subType = S_STANDARD;
        misc.rulesRefs = "142, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_F).setAvailability(RATING_E, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2695, DATE_NONE, DATE_NONE, 2780)
                .setClanAdvancement(2695).setPrototypeFactions(F_TH)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return misc;
    }

    public static MiscType createImprovedCasparDroneControlSystem() {
        // TODO Game Rules.
        MiscType misc = new MiscType();
        misc.name = "SDS (Caspar) Drone Control System (Improved)";
        misc.setInternalName("ImprovedCasparDroneControlSystem");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_CASPAR).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.subType = S_IMPROVED;
        misc.rulesRefs = "142, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_F).setAvailability(RATING_E, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2695, DATE_NONE, DATE_NONE, 2780)
                .setClanAdvancement(2695).setPrototypeFactions(F_TH)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return misc;
    }

    public static MiscType createCasparIIDroneControlSystem() {
        // TODO Game Rules.
        MiscType misc = new MiscType();
        misc.name = "Caspar II Advanced Smart Robotic Control System";
        misc.setInternalName("CasparIIDroneControlSystem");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_CASPARII).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.subType = S_STANDARD;
        misc.rulesRefs = "143, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                .setISAdvancement(3064, DATE_NONE, DATE_NONE, 3078, 3082)
                .setPrototypeFactions(F_WB).setReintroductionFactions(F_RS)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return misc;
    }

    public static MiscType createImprovedCasparIIDroneControlSystem() {
        // TODO Game Rules.
        MiscType misc = new MiscType();
        misc.name = "Caspar II Advanced Smart Robotic Control System (Improved)";
        misc.setInternalName("ImprovedCasparIIDroneControlSystem");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_CASPARII).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.subType = S_IMPROVED;
        misc.rulesRefs = "143, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                .setISAdvancement(3064, DATE_NONE, DATE_NONE, 3078, 3082)
                .setPrototypeFactions(F_WB).setReintroductionFactions(F_RS)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return misc;
    }

    public static MiscType createAutoTacticalAnalysisComputer() {
        // TODO Game Rules.
        MiscType misc = new MiscType();
        misc.name = "Autonomous Tactical Analysis Computer (ATAC)";
        misc.shortName = "ATAC";
        misc.setInternalName("AutoTacticalAnalysisComputer");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_ATAC).or(F_VARIABLE_SIZE).or(F_DS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.rulesRefs = "145, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_F, RATING_X, RATING_F, RATING_F)
                .setISAdvancement(2700, DATE_NONE, DATE_NONE, 2780, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2705, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH);

        return misc;
    }

    public static MiscType createDirectTacticalAnalysisSystem() {
        // TODO Game Rules.
        MiscType misc = new MiscType();
        misc.name = "Direct Tactical Analysis Control (DTAC) System";
        misc.shortName = "DTAC";
        misc.setInternalName("DirectTacticalAnalysisSystem");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_DTAC).or(F_VARIABLE_SIZE).or(F_DS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.rulesRefs = "146, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_F)
                .setISAdvancement(3072, DATE_NONE, DATE_NONE, 3078, 3082)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_WB)
                .setReintroductionFactions(F_RS);

        return misc;
    }

    public static MiscType createSDSSelfDestructSystem() {
        // TODO Game Rules.
        MiscType misc = new MiscType();
        misc.name = "SDS Self-Destruct System";
        misc.setInternalName("SDSSelfDestructSystem");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = 1000000;
        misc.flags = misc.flags.or(F_SDS_DESTRUCT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.rulesRefs = "147, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_D, RATING_F, RATING_X, RATING_X)
                .setISAdvancement(2695, DATE_NONE, DATE_NONE, 2780, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2695, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH);

        return misc;
    }

    public static MiscType createSDSJammerSystem() {
        // TODO Game Rules.
        MiscType misc = new MiscType();
        misc.name = "SLDF SDS Jammer System";
        misc.setInternalName("SDSJammerSystem");
        misc.tonnage = 30000;
        misc.criticals = 0;
        misc.cost = 800000000;
        misc.flags = misc.flags.or(F_SDS_JAMMER).or(F_WS_EQUIPMENT);
        misc.rulesRefs = "148, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_F, RATING_F, RATING_X, RATING_X)
                .setISAdvancement(2776, DATE_NONE, DATE_NONE, 2780, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2776, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH);

        return misc;
    }

    // Ejection and Escape Systems

    public static MiscType createISCombatVehicleEscapePod() {
        // TODO: implement game rules
        MiscType misc = new MiscType();
        misc.name = "Combat Vehicle Escape Pod";
        misc.setInternalName("ISCombatVehicleEscapePod");
        misc.tonnage = 4;
        misc.criticals = 0;
        misc.cost = 10000;
        misc.flags = misc.flags.or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT).or(F_COMBAT_VEHICLE_ESCAPE_POD);
        misc.rulesRefs = "309, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(3038, 3079).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    // TODO BattleMech Full-Head Ejection System - IO pg 40 - This was at one
    // point a quirk

    public static MiscType createIMEjectionSeat() {
        MiscType misc = new MiscType();

        misc.name = "Ejection Seat (Industrial Mech)";
        misc.setInternalName(EquipmentTypeLookup.IM_EJECTION_SEAT);
        misc.shortName = "Ejection Seat";
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.cost = 25000;
        misc.flags = misc.flags.or(F_EJECTION_SEAT).or(F_MECH_EQUIPMENT);
        misc.bv = 0;
        misc.industrial = true;
        misc.rulesRefs = "213, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_E, RATING_F, RATING_E)
                .setISAdvancement(2430, 2445, 2490, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2430, 2445, 2490, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createSVEjectionSeat() {
        MiscType misc = new MiscType();

        misc.name = "Ejection Seat (Support Vehicle)";
        misc.setInternalName(misc.name);
        misc.shortName = "Ejection Seat";
        misc.tonnage = 0.1; // M/L SVs round all kg-scale equipment up to the half ton at the end of the calculation.
        misc.tankslots = 1;
        misc.cost = 25000;
        misc.flags = misc.flags.or(F_EJECTION_SEAT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.bv = 0;
        misc.industrial = true;
        misc.rulesRefs = "213, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    // Escape Pod Aerospace - this is currently built as a Small Craft.

    // Down the road it might be better to make this into a separate Small
    // Support Vee,
    // But for now leaving it as equipment.
    public static MiscType createMaritimeEscapePod() {
        MiscType misc = new MiscType();

        misc.name = "Escape Pod (Maritime)";
        misc.setInternalName(misc.name);
        misc.tonnage = 7.0;
        misc.tankslots = 0;
        misc.cost = 5000;
        misc.flags = misc.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_LIFEBOAT);
        misc.subType = S_MARITIME_ESCAPE_POD;
        misc.bv = 0;
        misc.industrial = true;
        misc.rulesRefs = "216, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_C, RATING_C)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createMaritimeLifeboat() {
        MiscType misc = new MiscType();
        misc.name = "Lifeboat (Maritime)";
        misc.setInternalName(misc.name);
        misc.tankslots = 0;
        misc.tonnage = 1;
        misc.cost = 5000;
        misc.bv = 0;
        misc.industrial = true;
        misc.flags = misc.flags.or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_LIFEBOAT);
        misc.subType = S_MARITIME_LIFEBOAT;
        misc.rulesRefs = "227, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    // Down the road it might be better to make this into a separate Small
    // Support Vee,
    // But for now leaving it as equipment.
    public static MiscType createAtmossphericLifeboat() {
        MiscType misc = new MiscType();
        misc.name = "Lifeboat (Atmospheric)";
        misc.setInternalName(misc.name);
        misc.tankslots = 0;
        misc.tonnage = 7;
        misc.cost = 5000;
        misc.bv = 0;
        misc.industrial = true;
        misc.flags = misc.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_LIFEBOAT);
        misc.subType = S_ATMOSPHERIC_LIFEBOAT;
        misc.rulesRefs = "227, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    // Electronic Warfare Systems
    public static MiscType createBeagleActiveProbe() {
        MiscType misc = new MiscType();

        misc.name = "Active Probe (Beagle)";
        misc.setInternalName(Sensor.BAP);
        misc.addLookupName("Beagle Active Probe");
        misc.addLookupName("ISBeagleActiveProbe");
        misc.addLookupName("IS Beagle Active Probe");
        misc.tonnage = 1.5;
        misc.criticals = 2;
        misc.cost = 200000;
        misc.flags = misc.flags.or(F_BAP).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).andNot(F_WS_EQUIPMENT).andNot(F_JS_EQUIPMENT).andNot(F_SS_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.bv = 10;
        misc.rulesRefs = "204, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C).setISAdvancement(2560, 2576, 3048, 2835, 3045)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_CC);
        return misc;
    }

    public static MiscType createBeagleActiveProbePrototype() {
        MiscType misc = new MiscType();

        misc.name = "Active Probe (Beagle) Prototype";
        misc.setInternalName(Sensor.BAPP);
        misc.addLookupName("Beagle Active Probe Prototype");
        misc.shortName = "Active Probe (Beagle)(P)";
        misc.tonnage = 2.0;
        misc.criticals = 3;
        misc.cost = 600000;
        misc.flags = misc.flags.or(F_BAP).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT)
                .or(F_DS_EQUIPMENT).andNot(F_WS_EQUIPMENT).andNot(F_JS_EQUIPMENT)
                .andNot(F_SS_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.bv = 10;
        misc.rulesRefs = "71, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                .setISAdvancement(2560, DATE_NONE, DATE_NONE, 2576, DATE_NONE)
                .setISApproximate(true, false, false, true, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_CC);
        return misc;
    }

    public static MiscType createBloodhoundActiveProbe() {
        MiscType misc = new MiscType();

        misc.name = "Bloodhound Active Probe";
        misc.setInternalName(Sensor.BLOODHOUND);
        misc.addLookupName("Bloodhound Active Probe");
        misc.addLookupName("ISBloodhoundActiveProbe");
        misc.addLookupName("IS Bloodhound Active Probe");
        misc.tonnage = 2;
        misc.criticals = 3;
        misc.cost = 500000;
        misc.flags = misc.flags.or(F_BAP).or(F_BLOODHOUND).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).andNot(F_WS_EQUIPMENT).andNot(F_JS_EQUIPMENT).andNot(F_SS_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.bv = 25;
        misc.rulesRefs = "278, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3058, 3082, 3094, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false).setPrototypeFactions(F_CS)
                .setProductionFactions(F_RS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createTHBBloodhoundActiveProbe() {
        MiscType misc = new MiscType();

        misc.name = "Bloodhound Active Probe (THB)";
        misc.setInternalName("THBBloodhoundActiveProbe");
        misc.addLookupName("THB Bloodhound Active Probe");
        misc.addLookupName("ISTHBBloodhoundActiveProbe");
        misc.addLookupName("IS THB Bloodhound Active Probe");
        misc.tonnage = 5;
        misc.criticals = 2;
        misc.cost = 750000;
        misc.flags = misc.flags.or(F_BAP).or(F_BLOODHOUND).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).andNot(F_WS_EQUIPMENT).andNot(F_JS_EQUIPMENT).andNot(F_SS_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.bv = 25;
        // Since its Tactical Handbook Using TO Values
        misc.rulesRefs = "Unofficial";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
        .setISAdvancement(DATE_NONE, 3058, 3082, DATE_NONE, DATE_NONE)
        .setISApproximate(false, false, false, false, false)
        .setPrototypeFactions(F_CS)
        .setProductionFactions(F_RS)
        .setStaticTechLevel(SimpleTechLevel.UNOFFICIAL);
        return misc;
    }

    public static MiscType createCLActiveProbe() {
        MiscType misc = new MiscType();

        misc.name = "Active Probe";
        misc.setInternalName(Sensor.CLAN_AP);
        misc.addLookupName("Active Probe");
        misc.addLookupName("Clan Active Probe");
        misc.addLookupName("ClActiveProbe");
        misc.addLookupName("Active Probe [Clan]");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 200000;
        misc.flags = misc.flags.or(F_BAP).or(F_MECH_EQUIPMENT).or(F_PROTOMECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.bv = 12;
        misc.rulesRefs = "204, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_E, RATING_D, RATING_C)
                .setClanAdvancement(2830, 2832, 2835, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CGS)
                .setProductionFactions(F_CGS);

        return misc;
    }

    // According to IO pg 40 - The IS has a Light Active probe but I can find no
    // reference to it.
    // May 2017 - looking into it, but at this apoint it appears IO is in error.

    public static MiscType createCLLightActiveProbe() {
        MiscType misc = new MiscType();

        misc.name = "Light Active Probe";
        misc.setInternalName(Sensor.LIGHT_AP);
        misc.addLookupName("CL Light Active Probe");
        misc.addLookupName("Light Active Probe");
        misc.addLookupName("Clan Light Active Probe");
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.cost = 50000;
        misc.flags = misc.flags.or(F_BAP).or(F_MECH_EQUIPMENT).or(F_PROTOMECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.bv = 7;
        misc.rulesRefs = "204, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(2890, 2900, 2905, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return misc;
    }

    public static MiscType createGECM() {
        MiscType misc = new MiscType();

        misc.name = "ECM Suite (Guardian)";
        misc.setInternalName("ISGuardianECMSuite");
        misc.addLookupName("IS Guardian ECM");
        misc.addLookupName("ISGuardianECM");
        misc.addLookupName("IS Guardian ECM Suite");
        misc.tonnage = 1.5f;
        misc.criticals = 2;
        misc.cost = 200000;
        misc.flags = misc.flags.or(F_ECM).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).andNot(F_WS_EQUIPMENT).andNot(F_JS_EQUIPMENT).andNot(F_SS_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.bv = 61;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "213, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C).setISAdvancement(2595, 2597, 3050, 2845, 3045)
                .setISApproximate(false, false, false, false, true).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_CC);
        return misc;
    }

    public static MiscType createGECMPrototype() {
        MiscType misc = new MiscType();

        misc.name = "ECM Suite (Guardian) Prototype";
        misc.setInternalName("ISGuardianECMSuitePrototype");
        misc.addLookupName("IS Prototype Guardian ECM");
        misc.shortName = "ECM Suite (Guardian) (P)";
        misc.tonnage = 2.0f;
        misc.criticals = 3;
        misc.cost = 1000000;
        misc.flags = misc.flags.or(F_ECM).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).andNot(F_WS_EQUIPMENT).andNot(F_JS_EQUIPMENT).andNot(F_SS_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.bv = 61;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "71, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_X, RATING_X)
                .setISAdvancement(2595, DATE_NONE, DATE_NONE, 2597, DATE_NONE)
                .setISApproximate(true, false, false, true, false).setPrototypeFactions(F_TH);
        return misc;
    }

    public static MiscType createCLECM() {
        MiscType misc = new MiscType();

        misc.name = "ECM Suite";
        misc.setInternalName("CLECMSuite");
        misc.addLookupName("Clan ECM Suite");
        misc.addLookupName("ECM Suite [Clan]");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 200000;
        misc.flags = misc.flags.or(F_ECM).or(F_MECH_EQUIPMENT).or(F_PROTOMECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).andNot(F_WS_EQUIPMENT).andNot(F_JS_EQUIPMENT).andNot(F_SS_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.bv = 61;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "213, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_E, RATING_D, RATING_C)
                .setClanAdvancement(2830, 2832, 2835, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return misc;
    }

    public static MiscType createISAngelECM() {
        MiscType misc = new MiscType();

        // Don't forget, this will eventually count double for ECCM.
        misc.name = "Angel ECM Suite";
        misc.setInternalName("ISAngelECMSuite");
        misc.addLookupName("IS Angel ECM Suite");
        misc.addLookupName("ISAngelECM");
        misc.addLookupName("CLAngelECMSuite");
        misc.addLookupName("Clan Angel ECM Suite");
        misc.addLookupName("CLAngelECM");
        misc.addLookupName("Clan Angel ECM Suite");
        misc.tonnage = 2;
        misc.criticals = 2;
        misc.cost = 750000;
        misc.flags = misc.flags.or(F_ECM).or(F_ANGEL_ECM).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_PROTOMECH_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
                .andNot(F_WS_EQUIPMENT).andNot(F_JS_EQUIPMENT).andNot(F_SS_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.bv = 100;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "279, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3057, 3080, 3085, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(3058, 3080, 3085, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_DC, F_CNC)
                .setProductionFactions(F_DC)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createISTHBAngelECM() {
        MiscType misc = new MiscType();

        // Don't forget, this will eventually count double for ECCM.
        misc.name = "THB Angel ECM Suite";
        misc.setInternalName("ISTHBAngelECMSuite");
        misc.addLookupName("IS THB Angel ECM Suite");
        misc.addLookupName("ISTHBAngelECM");
        misc.tonnage = 1.5f;
        misc.criticals = 2;
        misc.cost = 1000000;
        misc.flags = misc.flags.or(F_ECM).or(F_ANGEL_ECM).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).andNot(F_WS_EQUIPMENT).andNot(F_JS_EQUIPMENT).andNot(F_SS_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.bv = 100;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "Unofficial";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3057, 3080, 3085, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(3058, 3080, 3085, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_DC, F_CNC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.UNOFFICIAL);
        return misc;
    }

    public static MiscType createISEWEquipment() {
        MiscType misc = new MiscType();
        misc.name = "Electronic Warfare (EW) Equipment";
        misc.setInternalName(Sensor.EW_EQUIPMENT);
        misc.shortName = "EW Equipment";
        misc.tonnage = 7.5;
        misc.criticals = 4;
        misc.cost = 500000;
        misc.flags = misc.flags.or(F_EW_EQUIPMENT).or(F_BAP).or(F_ECM).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).andNot(F_WS_EQUIPMENT)
                .andNot(F_JS_EQUIPMENT).andNot(F_SS_EQUIPMENT);
        misc.bv = 39;
        misc.rulesRefs = "310, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_F, RATING_E, RATING_F)
                .setISAdvancement(3020, 3025, DATE_NONE, 3046, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createWatchdogECM() {
        MiscType misc = new MiscType();

        misc.name = "Watchdog Composite Electronic Warfare System (CEWS)";
        misc.setInternalName(Sensor.WATCHDOG);
        misc.addLookupName("Watchdog ECM Suite");
        misc.addLookupName("WatchdogECM");
        misc.addLookupName("CLWatchdogECM");
        misc.shortName = "Watchdog CEWS";
        misc.tonnage = 1.5f;
        misc.criticals = 2;
        misc.cost = 500000;
        misc.flags = misc.flags.or(F_WATCHDOG).or(F_ECM).or(F_BAP).or(F_MECH_EQUIPMENT).or(F_PROTOMECH_EQUIPMENT)
                .or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
                .andNot(F_WS_EQUIPMENT).andNot(F_JS_EQUIPMENT).andNot(F_SS_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.bv = 68;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "278, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3059, DATE_NONE, 3080, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSF).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createNovaCEWS() {
        MiscType misc = new MiscType();

        misc.name = "Nova Combined Electronic Warfare System (CEWS)";
        misc.setInternalName(Sensor.NOVA);
        misc.addLookupName("Nova CEWS");
        misc.addLookupName("NovaCEWS");
        misc.addLookupName("CLNCEWS");
        misc.shortName = "Nova CEWS";
        misc.tonnage = 1.5f;
        misc.criticals = 1;
        misc.cost = 1100000; // we assume that WOR had a typo there.
        misc.flags = misc.flags.or(F_NOVA).or(F_ECM).or(F_BAP).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).andNot(F_WS_EQUIPMENT).andNot(F_JS_EQUIPMENT)
                .andNot(F_SS_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.bv = 68;
        misc.setModes(new String[] { "ECM", "Off" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "66, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                .setClanAdvancement(3065, DATE_NONE, DATE_NONE, 3085, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createISReconCamera() {
        // TODO: implement game rules
        MiscType misc = new MiscType();
        misc.name = "Recon Camera";
        misc.setInternalName("ISReconCamera");
        misc.addLookupName("CLReconCamera");
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.cost = 10000;
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_PROTOMECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_RECON_CAMERA);
        misc.rulesRefs = "337, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createRemoteSensorDispenser() {
        MiscType misc = new MiscType();

        misc.name = "Remote Sensors/Dispenser";
        misc.setInternalName("RemoteSensorDispenser");
        misc.addLookupName("Remote Sensor Dispenser");
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.hittable = true;
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
                .or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT).or(F_SENSOR_DISPENSER);
        misc.bv = 0;
        misc.cost = 51000;
        misc.industrial = true;
        misc.rulesRefs = "375, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_E, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(2586, 2590, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2586, 2590, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createPrototypeRemoteSensorDispenser() {
        MiscType misc = new MiscType();
        // TODO GAME Rules see IO pg 73
        misc.name = "Prototype Remote Sensors/Dispenser";
        misc.setInternalName("ProtoTypeRemoteSensorDispenser");
        misc.addLookupName("Prototype Remote Sensor Dispenser");
        misc.shortName = "Remote Sensors/Dispenser (P)";
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.hittable = true;
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
                .or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT).or(F_SENSOR_DISPENSER);
        misc.bv = 0;
        misc.cost = 60000;
        misc.industrial = true;
        misc.rulesRefs = "73, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2586, DATE_NONE, DATE_NONE, 2590, DATE_NONE)
                .setISApproximate(true, false, false, true, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createISLookDownRadar() {
        MiscType misc = new MiscType();
        misc.name = "Satellite Imager (Look-Down Radar)";
        misc.setInternalName("ISLookDownRadar");
        misc.shortName = "Look-Down Radar";
        misc.addLookupName("CLLookDownRadar");
        misc.addLookupName("Satellite Imager [Look-Down Radar]");
        misc.tonnage = 5;
        misc.cost = 400000;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_LOOKDOWN_RADAR).or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "340, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createISInfraredImager() {
        MiscType misc = new MiscType();
        misc.name = "Satellite Imager (Infrared Imager)";
        misc.shortName = "Infrared Imager";
        misc.setInternalName("ISInfraredImager");
        misc.addLookupName("CLInfraredImager");
        misc.addLookupName("Satellite Imager [Infrared Imager]");
        misc.tonnage = 5;
        misc.cost = 250000;
        misc.flags = misc.flags.or(F_INFRARED_IMAGER).or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.bv = 0;
        misc.criticals = 1;
        misc.rulesRefs = "339, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createISHyperspectralImager() {
        MiscType misc = new MiscType();
        misc.name = "Satellite Imager (Hyperspectral Imager)";
        misc.shortName = "Hyperspectral Image";
        misc.setInternalName("ISHypersprectralImager");
        misc.addLookupName("ISHyperspectralImager");
        misc.addLookupName("Satellite Imager [Hyperspectral Imager]");
        misc.tonnage = 7.5;
        misc.cost = 550000;
        misc.criticals = 1;
        misc.svslots = 2;
        misc.flags = misc.flags.or(F_HYPERSPECTRAL_IMAGER).or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "338, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3045, 3055, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, true, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return misc;
    }

    public static MiscType createISHIResImager() {
        MiscType misc = new MiscType();
        misc.name = "Satellite Imager (High-Resolution Imager)";
        misc.setInternalName("ISHighResImager");
        misc.shortName = "Hi-Res Imager";
        misc.addLookupName("CLHighResImager");
        misc.addLookupName("Satellite Imager [High-Resolution (Hi-Res) Imager]");
        misc.tonnage = 2.5;
        misc.cost = 150000;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_HIRES_IMAGER).or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "339, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    // Fire Control and Targeting Systems
    // General Fire Control Systems
    public static MiscType createBasicFireControl() {
        MiscType misc = new MiscType();
        misc.name = "Basic Fire Control";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.cost = EquipmentType.COST_VARIABLE;
        misc.flags = misc.flags.or(MiscType.F_BASIC_FIRECONTROL).or(MiscType.F_SUPPORT_TANK_EQUIPMENT)
                .or(MiscType.F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.industrial = true;
        misc.rulesRefs = "217, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createAdvancedFireControl() {
        MiscType misc = new MiscType();
        misc.name = "Advanced Fire Control";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.cost = EquipmentType.COST_VARIABLE;
        misc.flags = misc.flags.or(MiscType.F_ADVANCED_FIRECONTROL)
                .or(MiscType.F_SUPPORT_TANK_EQUIPMENT).or(MiscType.F_TANK_EQUIPMENT).or(MiscType.F_FIGHTER_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "217, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_D, RATING_C)
                .setISAdvancement(DATE_ES, 2300, 2300, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_ES, 2300, 2300, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false);
        return misc;
    }

    // Missile-Enhancing Fire Control Systems

    public static MiscType createISArtemis() {
        MiscType misc = new MiscType();

        misc.name = "Artemis IV FCS";
        misc.setInternalName("ISArtemisIV");
        misc.addLookupName("IS Artemis IV FCS");
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.tankslots = 0;
        misc.cost = 100000;
        misc.flags = misc.flags.or(F_ARTEMIS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.rulesRefs = "206, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C).setISAdvancement(2592, 2598, 3045, 2855, 3035)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FW);
        return misc;
    }

    public static MiscType createISProtoArtemis() {
        MiscType misc = new MiscType();

        misc.name = "Prototype Artemis IV FCS";
        misc.setInternalName("ISArtemisIVProto");
        misc.addLookupName("IS Proto type Artemis IV FCS");
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.tankslots = 0;
        misc.cost = 100000;
        misc.flags = misc.flags.or(F_ARTEMIS_PROTO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
                .or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.rulesRefs = "217, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2592, DATE_NONE, DATE_NONE, 2612, 3035)
                .setISApproximate(true, false, false, true, true).setPrototypeFactions(F_TH).setProductionFactions(F_TH)
                .setReintroductionFactions(F_FW)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createCLArtemis() {
        MiscType misc = new MiscType();

        misc.name = "Artemis IV FCS";
        misc.setInternalName("CLArtemisIV");
        misc.addLookupName("Clan Artemis IV FCS");
        misc.tonnage = 1.0f;
        misc.cost = 100000;
        misc.criticals = 1;
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_ARTEMIS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.rulesRefs = "206, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_F, RATING_D, RATING_C)
                .setClanAdvancement(2816, 2818, 2830, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSA)
                .setProductionFactions(F_CSA);
        return misc;
    }

    public static MiscType createArtemisV() {
        MiscType misc = new MiscType();

        misc.name = "Artemis V FCS";
        misc.setInternalName("CLArtemisV");
        misc.addLookupName("Clan Artemis V");
        misc.addLookupName("Artemis V");
        misc.tonnage = 1.5f;
        misc.cost = 250000;
        misc.criticals = 2;
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_ARTEMIS_V).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.rulesRefs = "283, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3061, 3085, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false).setPrototypeFactions(F_CGS)
                .setProductionFactions(F_CSF, F_RD).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createISApolloFCS() {
        MiscType misc = new MiscType();

        misc.name = "Apollo MRM FCS";
        misc.setInternalName("ISApollo");
        misc.addLookupName("IS MRM Apollo Fire Control System");
        misc.addLookupName("IS MRM Apollo FCS");
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.cost = 125000;
        misc.flags = misc.flags.or(F_APOLLO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.rulesRefs = "330, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3065, 3071, 3097, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return misc;
    }

    // Targeting Computers
    /**
     * Targeting comps should NOT be spreadable. However, I've set them such as a
     * temp measure to overcome the following bug: TC space allocation is calculated
     * based on tonnage of direct-fire weaponry. However, since meks are loaded
     * location-by-location, when the TC is loaded it's very unlikely that all of
     * the weaponry will be attached, resulting in undersized comps. Any remaining
     * TC crits after the last expected one are being handled as a 2nd TC, causing
     * LocationFullExceptions.
     */
    public static MiscType createISTargComp() {
        MiscType misc = new MiscType();

        misc.name = "Targeting Computer";
        misc.setInternalName("ISTargeting Computer");
        misc.addLookupName("IS Targeting Computer");
        misc.addLookupName("Targeting Computer [IS]");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.bv = 0; // TarComps modify weapon BVs, they have none of their own.
        misc.flags = misc.flags.or(F_TARGCOMP).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        // see note above
        misc.spreadable = true;
        String[] modes = { "Normal", "Aimed shot" };
        misc.setModes(modes);
        misc.rulesRefs = "238, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3052, 3061, 3067, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, true, false, false)
                .setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createCLTargComp() {
        MiscType misc = new MiscType();

        misc.name = "Targeting Computer";
        misc.setInternalName("CLTargeting Computer");
        misc.addLookupName("Clan Targeting Computer");
        misc.addLookupName("Targeting Computer [Clan]");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.bv = 0; // TarComps modify weapon BVs, they have none of their own.
        misc.flags = misc.flags.or(F_TARGCOMP).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        // see note above
        misc.spreadable = true;
        String[] modes = { "Normal", "Aimed shot" };
        misc.setModes(modes);
        misc.rulesRefs = "238, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_E, RATING_D, RATING_D)
                .setClanAdvancement(2850, 2860, 2863, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CMN)
                .setProductionFactions(F_CMN)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    // TAG - In with the Weapons.

    // Fluid Guns and Sprayer.
    // Fluid Guns - in with Weapons.

    public static MiscType createMechSprayer() {
        MiscType misc = new MiscType();

        misc.name = "Sprayer (Mech)";
        misc.setInternalName(EquipmentTypeLookup.SPRAYER_MEK);
        misc.addLookupName("Sprayer [Mech]");
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.cost = 1000;
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_SPRAYER);
        misc.industrial = true;
        misc.rulesRefs = "248, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(2305, 2315, 2320, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2305, 2315, 2320, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_FS);
        return misc;
    }

    public static MiscType createTankSprayer() {
        MiscType misc = new MiscType();

        misc.name = "Sprayer (Vehicular)";
        misc.setInternalName(EquipmentTypeLookup.SPRAYER_VEE);
        misc.addLookupName("Sprayer [Vehicular]");
        misc.shortName = "Sprayer";
        misc.tonnage = 0.015;
        misc.criticals = 1;
        misc.cost = 1000;
        misc.flags = misc.flags.or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_SPRAYER);
        misc.industrial = true;
        misc.rulesRefs = "248, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    // Heat Sinks
    public static MiscType createHeatSink() {
        MiscType misc = new MiscType();

        misc.name = "Heat Sink";
        misc.setInternalName(EquipmentTypeLookup.SINGLE_HS);
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_HEAT_SINK);
        misc.bv = 0;
        misc.rulesRefs = "221, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setStaticTechLevel(SimpleTechLevel.INTRO);
        return misc;
    }

    // It is possible to have 1 or 2 compact heat sinks
    // in a single critical slot - this is addressed by
    // creating 1 slot single and 1 slot double heat sinks
    // with the weight of 1 or 2 compact heat sinks
    public static MiscType createIS1CompactHeatSink() {
        MiscType misc = new MiscType();

        misc.name = "1 Compact Heat Sink";
        misc.setInternalName(EquipmentTypeLookup.COMPACT_HS_1);
        misc.addLookupName("IS1 Compact Heat Sink");
        misc.tonnage = 1.5f;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_HEAT_SINK).or(F_COMPACT_HEAT_SINK);
        misc.bv = 0;
        misc.cost = 3000;
        misc.rulesRefs = "316, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS)
            .setISAdvancement(3058, 3079)
            .setISApproximate(false, false).setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS).setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createIS2CompactHeatSinks() {
        MiscType misc = new MiscType();

        misc.name = "2 Compact Heat Sinks";
        misc.setInternalName(EquipmentTypeLookup.COMPACT_HS_2);
        misc.addLookupName("IS2 Compact Heat Sinks");
        misc.tonnage = 3.0f;
        misc.criticals = 1;
        misc.cost = 6000;
        misc.flags = misc.flags.or(F_DOUBLE_HEAT_SINK).or(F_COMPACT_HEAT_SINK);
        misc.bv = 0;
        misc.rulesRefs = "316, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS)
            .setISAdvancement(3058, 3079)
            .setISApproximate(false, false).setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS).setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISDoubleHeatSinkPrototype() {
        MiscType misc = new MiscType();

        misc.name = "Double Heat Sink Prototype";
        misc.setInternalName(EquipmentTypeLookup.IS_DOUBLE_HS_PROTOTYPE);
        misc.addLookupName("IS Double Heat Sink Prototype");
        misc.addLookupName("ISDouble Heat Sink Prototype");
        misc.tonnage = 1.0f;
        misc.criticals = 3;
        misc.cost = 18000; // Using Cost
        misc.flags = misc.flags.or(F_IS_DOUBLE_HEAT_SINK_PROTOTYPE);
        misc.bv = 0;
        misc.rulesRefs = "71, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(2559, DATE_NONE, DATE_NONE, 2567, DATE_NONE)
                .setISApproximate(true, false, false, true, false).setPrototypeFactions(F_TH).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createISFreezerPrototype() {
        MiscType misc = new MiscType();

        misc.name = "Double Heat Sink (Freezers)";
        misc.setInternalName(EquipmentTypeLookup.IS_DOUBLE_HS_FREEZER);
        misc.addLookupName("Freezers");
        misc.tonnage = 1.0f;
        misc.criticals = 3;
        misc.cost = 30000; // Using Cost
        misc.flags = misc.flags.or(F_IS_DOUBLE_HEAT_SINK_PROTOTYPE);
        misc.bv = 0;
        misc.rulesRefs = "71, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3022, DATE_NONE, DATE_NONE, 3040, DATE_NONE)
                .setISApproximate(true, false, false, true, false).setPrototypeFactions(F_FS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_F, RATING_X, RATING_X).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createISDoubleHeatSink() {
        MiscType misc = new MiscType();

        misc.name = "Double Heat Sink";
        misc.setInternalName(EquipmentTypeLookup.IS_DOUBLE_HS);
        misc.addLookupName("IS Double Heat Sink");
        misc.addLookupName("ISDouble Heat Sink");
        misc.tonnage = 1.0f;
        misc.criticals = 3;
        misc.flags = misc.flags.or(F_DOUBLE_HEAT_SINK);
        misc.bv = 0;
        misc.rulesRefs = "221, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS)
            .setTechRating(RATING_E)
            .setAvailability(RATING_C, RATING_E, RATING_D, RATING_C)
            .setISAdvancement(2559, 2567, 3045, 2865, 3040)
            .setISApproximate(false, false, false, false, false)
            .setPrototypeFactions(F_TH)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createCLDoubleHeatSink() {
        MiscType misc = new MiscType();

        misc.name = "Double Heat Sink";
        misc.setInternalName(EquipmentTypeLookup.CLAN_DOUBLE_HS);
        misc.addLookupName("Clan Double Heat Sink");
        misc.addLookupName("CLDouble Heat Sink");
        misc.tonnage = 1.0f;
        misc.criticals = 2;
        misc.flags = misc.flags.or(F_DOUBLE_HEAT_SINK);
        misc.bv = 0;
        misc.rulesRefs = "221, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
            .setClanAdvancement(2825, 2827, 2829, DATE_NONE, DATE_NONE)
            .setClanApproximate(true, true, false, false, false)
            .setPrototypeFactions(F_CCY)
            .setProductionFactions(F_CCY)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_E, RATING_D, RATING_C).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createCLLaserHeatSink() {
        MiscType misc = new MiscType();

        misc.name = "Laser Heat Sink";
        misc.setInternalName(EquipmentTypeLookup.LASER_HS);
        misc.addLookupName("CLLaser Heat Sink");
        misc.tonnage = 1.0f;
        misc.criticals = 2;
        misc.flags = misc.flags.or(F_DOUBLE_HEAT_SINK).or(F_LASER_HEAT_SINK).or(F_MECH_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "316, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3040, 3051, 3060, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CJF)
                .setProductionFactions(F_CJF).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D);
        return misc;
    }

    // TODO Protomech Heatsinks see IO pg 42

    public static MiscType createRadicalHeatSinkSystem() {
        MiscType misc = new MiscType();
        misc.name = "Radical Heat Sink System";
        misc.setInternalName(misc.name);
        misc.tonnage = 4;
        misc.criticals = 3;
        misc.cost = 250000;
        misc.flags = misc.flags.or(F_RADICAL_HEATSINK).or(F_MECH_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(true);
        misc.rulesRefs = "89, IO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS        
        misc.techAdvancement.setTechBase(TECH_BASE_IS)
                .setISAdvancement(3095, 3122, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    // Industrial Equipment

    public static MiscType createBackhoe() {
        MiscType misc = new MiscType();

        misc.name = "Backhoe";
        misc.setInternalName(EquipmentTypeLookup.BACKHOE);
        misc.tonnage = 5;
        misc.criticals = 6;
        misc.svslots = 1;
        misc.cost = 50000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_BACKHOE;
        misc.bv = 8;
        misc.industrial = true;
        misc.rulesRefs = "241, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createLightBridgeLayer() {
        MiscType misc = new MiscType();
        misc.tonnage = 1;
        misc.cost = 40000;
        misc.criticals = 2;
        misc.svslots = 1;
        misc.bv = 5;
        misc.name = "Bridge Layer (Light)";
        misc.setInternalName(EquipmentTypeLookup.LIGHT_BRIDGE_LAYER);
        misc.sortingName = "Bridge B";
        misc.flags = misc.flags.or(F_LIGHT_BRIDGE_LAYER).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "242, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createMediumBridgeLayer() {
        MiscType misc = new MiscType();
        misc.tonnage = 2;
        misc.cost = 75000;
        misc.criticals = 4;
        misc.svslots = 1;
        misc.bv = 10;
        misc.name = "Bridge Layer (Medium)";
        misc.setInternalName(EquipmentTypeLookup.MEDIUM_BRIDGE_LAYER);
        misc.sortingName = "Bridge C";
        misc.flags = misc.flags.or(F_MEDIUM_BRIDGE_LAYER).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "242, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createHeavyBridgeLayer() {
        MiscType misc = new MiscType();
        misc.tonnage = 6;
        misc.cost = 100000;
        misc.criticals = 12;
        misc.svslots = 1;
        misc.bv = 20;
        misc.name = "Bridge Layer (Heavy)";
        misc.setInternalName(EquipmentTypeLookup.HEAVY_BRIDGE_LAYER);
        misc.sortingName = "Bridge D";
        misc.flags = misc.flags.or(F_HEAVY_BRIDGE_LAYER).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "242, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_E, RATING_E, RATING_E, RATING_E)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createBulldozer() {
        MiscType misc = new MiscType();
        misc.tonnage = 2;
        misc.criticals = 1;
        misc.cost = 50000;
        misc.name = "Bulldozer";
        misc.setInternalName(EquipmentTypeLookup.BULLDOZER);
        misc.bv = 10;
        misc.flags = misc.flags.or(F_BULLDOZER).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "241, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createChainsaw() {
        MiscType misc = new MiscType();

        misc.name = "Chainsaw";
        misc.setInternalName(EquipmentTypeLookup.CHAINSAW);
        misc.tonnage = 5;
        misc.criticals = 5;
        misc.svslots = 1;
        misc.cost = 100000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_CHAINSAW;
        misc.bv = 7;
        misc.industrial = true;
        misc.rulesRefs = "241, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCombine() {
        MiscType misc = new MiscType();

        misc.name = "Combine";
        misc.setInternalName(EquipmentTypeLookup.COMBINE);
        misc.tonnage = 2.5f;
        misc.criticals = 4;
        misc.svslots = 1;
        misc.cost = 75000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_COMBINE;
        misc.bv = 5;
        misc.industrial = true;
        misc.rulesRefs = "243, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createDualSaw() {
        MiscType misc = new MiscType();

        misc.name = "Dual Saw";
        misc.setInternalName(EquipmentTypeLookup.DUAL_SAW);
        misc.tonnage = 7;
        misc.criticals = 7;
        misc.svslots = 1;
        misc.cost = 100000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_DUAL_SAW;
        misc.bv = 9;
        misc.industrial = true;
        misc.rulesRefs = "243, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createISBuzzsaw() {
        MiscType misc = new MiscType();

        misc.name = "Buzzsaw";
        misc.setInternalName(misc.name);
        misc.addLookupName("IS Buzzsaw");
        misc.addLookupName("Clan Buzzsaw");
        misc.addLookupName("CLBuzzsaw");
        misc.tonnage = 4;
        misc.criticals = 2;
        misc.cost = 100000;// From the Ask the Writer Forum
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT);
        misc.subType |= S_BUZZSAW;
        misc.bv = 67;// From the Ask the Writer Forum
        // Assuming this is a variant of the Dual Saw
        misc.rulesRefs = "243, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createFrontDumper() {
        MiscType misc = new MiscType();

        misc.name = "Dumper (Front)";
        misc.setInternalName(EquipmentTypeLookup.DUMPER_FRONT);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = 5000;
        misc.flags = misc.flags.or(F_DUMPER).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_MECH_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "243, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createRearDumper() {
        MiscType misc = new MiscType();

        misc.name = "Dumper (Rear)";
        misc.setInternalName(EquipmentTypeLookup.DUMPER_REAR);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = 5000;
        misc.flags = misc.flags.or(F_DUMPER).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_MECH_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "243, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createRightDumper() {
        MiscType misc = new MiscType();

        misc.name = "Dumper (Right)";
        misc.setInternalName(EquipmentTypeLookup.DUMPER_RIGHT);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = 5000;
        misc.flags = misc.flags.or(F_DUMPER).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_MECH_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "243, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createLeftDumper() {
        MiscType misc = new MiscType();

        misc.name = "Dumper (Left)";
        misc.setInternalName(EquipmentTypeLookup.DUMPER_LEFT);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = 5000;
        misc.flags = misc.flags.or(F_DUMPER).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_MECH_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "243, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createLightFluidSuctionSystemMech() {
        MiscType misc = new MiscType();
        misc.name = "Fluid Suction System (Light - Mech)";
        misc.setInternalName(EquipmentTypeLookup.FLUID_SUCTION_LIGHT_MEK);
        misc.addLookupName("Light Fluid Suction System (Mech)");
        misc.criticals = 1;
        misc.tonnage = .5;
        misc.cost = 1000;
        misc.flags = misc.flags.or(F_LIGHT_FLUID_SUCTION_SYSTEM).or(F_MECH_EQUIPMENT).andNot(F_SC_EQUIPMENT)
                .andNot(F_DS_EQUIPMENT).andNot(F_JS_EQUIPMENT).andNot(F_WS_EQUIPMENT).andNot(F_SS_EQUIPMENT);
        misc.rulesRefs = "248, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createLightFluidSuctionSystem() {
        MiscType misc = new MiscType();
        misc.name = "Fluid Suction System (Light - Vehicle)";
        misc.setInternalName(EquipmentTypeLookup.FLUID_SUCTION_LIGHT_VEE);
        misc.addLookupName("Light Fluid Suction System (Vehicle)");
        misc.criticals = 1;
        misc.tankslots = 1;
        misc.tonnage = 0.015;
        misc.cost = 1000;
        misc.flags = misc.flags.or(F_LIGHT_FLUID_SUCTION_SYSTEM).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .andNot(F_SC_EQUIPMENT).andNot(F_DS_EQUIPMENT).andNot(F_JS_EQUIPMENT).andNot(F_WS_EQUIPMENT)
                .andNot(F_SS_EQUIPMENT);
        misc.rulesRefs = "248, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createFluidSuctionSystem() {
        MiscType misc = new MiscType();
        misc.name = "Fluid Suction System (Standard)";
        misc.setInternalName(EquipmentTypeLookup.FLUID_SUCTION);
        misc.addLookupName("Fluid Suction System");
        misc.addLookupName("Fluid Suction System[Standard]");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.tankslots = 1;
        misc.tonnage = 1;
        misc.cost = 25000;
        misc.flags = misc.flags.or(F_FLUID_SUCTION_SYSTEM).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .andNot(F_SC_EQUIPMENT).andNot(F_DS_EQUIPMENT).andNot(F_JS_EQUIPMENT).andNot(F_WS_EQUIPMENT)
                .andNot(F_SS_EQUIPMENT);
        misc.rulesRefs = "248, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createPileDriver() {
        MiscType misc = new MiscType();

        misc.name = "Heavy-Duty Pile Driver";
        misc.setInternalName(EquipmentTypeLookup.PILE_DRIVER);
        misc.addLookupName("PileDriver");
        misc.addLookupName("Pile Driver");
        misc.tonnage = 10;
        misc.criticals = 8;
        misc.svslots = 1;
        misc.cost = 100000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_PILE_DRIVER;
        misc.bv = 5;
        misc.industrial = true;
        misc.rulesRefs = "244, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createLadder() {
        MiscType misc = new MiscType();
        misc.name = "Ladder";
        misc.setInternalName(EquipmentTypeLookup.LADDER);
        misc.addLookupName("Ladder (20m)");
        misc.addLookupName("Ladder (40m)");
        misc.addLookupName("Ladder (60m)");
        misc.addLookupName("Ladder (80m)");
        misc.addLookupName("Ladder (100m)");
        misc.tankslots = 1;
        misc.criticals = 1;
        misc.tonnage = TONNAGE_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_LADDER).or(F_VARIABLE_SIZE).or(F_MECH_EQUIPMENT)
                .or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "244, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createLiftHoist() {
        MiscType misc = new MiscType();

        misc.name = "Lift Hoist/Arresting Hoist";
        misc.setInternalName(EquipmentTypeLookup.LIFT_HOIST);
        misc.addLookupName("Lift Hoist");
        misc.tonnage = 3;
        misc.criticals = 3;
        misc.svslots = 1;
        misc.cost = 50000;
        misc.flags = misc.flags.or(F_LIFTHOIST).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.bv = 0;
        misc.industrial = true;
        misc.rulesRefs = "245, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createManipulator() {
        MiscType misc = new MiscType();

        misc.name = "Manipulator (Non-Mech/Non-Battle Armor)";
        misc.setInternalName(EquipmentTypeLookup.MANIPULATOR_INDUSTRIAL);
        misc.addLookupName("Manipulator");
        misc.addLookupName("Manipulator [Non-Mech/Non-Battle Armor]");
        misc.shortName = "Manipulator";
        misc.flags = misc.flags.or(F_MANIPULATOR).or(F_SUPPORT_TANK_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_VTOL_EQUIPMENT);
        misc.industrial = true;
        misc.tonnage = 0.01;
        misc.cost = 7500;
        misc.criticals = 1;
        misc.rulesRefs = "245, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_C, RATING_C)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createMiningDrill() {
        MiscType misc = new MiscType();

        misc.name = "Mining Drill";
        misc.setInternalName(EquipmentTypeLookup.MINING_DRILL);
        misc.cost = 10000;
        misc.tonnage = 3.0;
        misc.criticals = 4;
        misc.svslots = 1;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.subType |= S_MINING_DRILL;
        misc.bv = 6;
        misc.industrial = true;
        misc.rulesRefs = "246, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TH);
        return misc;
    }

    // Nail and Rivet Guns in Weapons

    public static MiscType createRefuelingDrogue() {
        MiscType misc = new MiscType();
        misc.tonnage = 1;
        misc.cost = 25000;
        misc.name = "Refueling Drogue/Fluid Suction System (Aero)";
        misc.setInternalName(EquipmentTypeLookup.REFUELING_DROGUE);
        misc.flags = misc.flags.or(F_REFUELING_DROGUE).or(F_FIGHTER_EQUIPMENT).or(F_VTOL_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT).or(F_SC_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "247, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createRockCutter() {
        MiscType misc = new MiscType();

        misc.name = "Rock Cutter";
        misc.setInternalName(EquipmentTypeLookup.ROCK_CUTTER);
        misc.tonnage = 5;
        misc.criticals = 5;
        misc.svslots = 1;
        misc.cost = 100000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_ROCK_CUTTER;
        misc.bv = 6;
        misc.industrial = true;
        misc.rulesRefs = "247, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createSalvageArm() {
        MiscType misc = new MiscType();

        misc.name = "Salvage Arm";
        misc.setInternalName(EquipmentTypeLookup.SALVAGE_ARM);
        misc.addLookupName("SalvageArm");
        misc.tonnage = 3;
        misc.criticals = 2;
        misc.bv = 0;
        misc.cost = 50000;
        misc.flags = misc.flags.or(F_SALVAGE_ARM).or(F_MECH_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "248, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_E, RATING_F, RATING_E, RATING_E)
                .setISAdvancement(2400, 2415, 2420, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2400, 2415, 2420, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false);
        return misc;
    }

    public static MiscType createSpotWelder() {
        MiscType misc = new MiscType();

        misc.name = "Spot Welder";
        misc.setInternalName(EquipmentTypeLookup.SPOT_WELDER);
        misc.tonnage = 2;
        misc.criticals = 1;
        misc.cost = 75000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_SPOT_WELDER;
        misc.bv = 5;
        misc.industrial = true;
        misc.rulesRefs = "248, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_C, RATING_C)
                .setISAdvancement(2312, 2320, 2323, DATE_NONE, DATE_NONE)
                .setISApproximate(true, true, false, false, false)
                .setClanAdvancement(2312, 2320, 2323, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false);
        return misc;
    }

    public static MiscType createISWreckingBall() {
        MiscType misc = new MiscType();

        misc.name = "Wrecking Ball";
        misc.setInternalName(EquipmentTypeLookup.WRECKING_BALL);
        misc.addLookupName("WreckingBall");
        misc.addLookupName("Clan Wrecking Ball");
        misc.addLookupName("CLWrecking Ball");
        misc.tonnage = 4;
        misc.criticals = 5;
        misc.svslots = 1;
        misc.cost = 110000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.subType |= S_WRECKING_BALL;
        misc.bv = 8;
        misc.industrial = true;
        misc.rulesRefs = "249, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    // Large Craft System (IO pg 42)
    /*
     * Docking Hardpoint Dropship KF Boom Space Station K-F Adaptor Jump Sails Word
     * of Blacke Super-Jump Control System Components, KF Drive Components, Drive
     * Components, The above items are equipment but not sure if they should be done
     * in MiscType or calculated elsewhere as structure.
     */

    public static MiscType createEnergyStorageBattery() {
        MiscType misc = new MiscType();

        misc.name = "Energy Storage Battery";
        misc.setInternalName("StorageBattery");
        misc.tonnage = 100000;
        misc.criticals = 0;
        misc.cost = 1000000;
        misc.flags = misc.flags.or(F_STORAGE_BATTERY).or(F_SS_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "306, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(DATE_ES, 2131, 2135, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, 2131, 2135, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createLightSail() {
        MiscType misc = new MiscType();

        misc.name = "Light Sail";
        misc.setInternalName("LightSail");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_LIGHT_SAIL).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "323, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(DATE_ES, 2131, 2135, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, 2131, 2135, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createISSmallNavalCommScannerSuite() {
        MiscType misc = new MiscType();
        misc.tonnage = 100;
        misc.svslots = 1;
        misc.cost = 50000000;
        misc.name = "Naval Comm-Scanner Suite (Small)";
        misc.setInternalName("ISSmallNavalCommScannerSuite");
        misc.addLookupName("CLSmallNavalCommScannerSuite");
        misc.flags = misc.flags.or(F_SMALL_COMM_SCANNER_SUITE).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT)
                .or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.rulesRefs = "332, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_D, RATING_E, RATING_E, RATING_E)
                .setISAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setProductionFactions(F_TA);

        return misc;
    }

    public static MiscType createISLargeNavalCommScannerSuite() {
        MiscType misc = new MiscType();
        misc.tonnage = 500;
        misc.svslots = 1;
        misc.cost = 250000000;
        misc.name = "Naval Comm-Scanner Suite (Large)";
        misc.setInternalName("ISLargeNavalCommScannerSuite");
        misc.addLookupName("CLLargeNavalCommScannerSuite");
        misc.flags = misc.flags.or(F_LARGE_COMM_SCANNER_SUITE).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT)
                .or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.rulesRefs = "332, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_D, RATING_E, RATING_E, RATING_E)
                .setISAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setProductionFactions(F_TA);
        return misc;
    }

    public static MiscType createNC3() {
        MiscType misc = new MiscType();

        misc.name = "Naval C3";
        misc.setInternalName("ISNC3");
        misc.addLookupName("NC3");
        misc.addLookupName("NC3Unit");
        misc.addLookupName("ISNC3Unit");
        misc.addLookupName("IS Naval C3");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_NAVAL_C3).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT)
                .or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "332, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3065).setPrototypeFactions(F_DC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createISNavalTugAdaptor() {
        MiscType misc = new MiscType();
        misc.tonnage = TONNAGE_VARIABLE;
        misc.cost = 100000;
        misc.name = "Naval Tug Adaptor";
        misc.setInternalName("ISNavalTugAdaptor");
        misc.addLookupName("CLNavalTugAdaptor");
        misc.flags = misc.flags.or(F_NAVAL_TUG_ADAPTOR).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_WS_EQUIPMENT);
        misc.rulesRefs = "334, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setStaticTechLevel(SimpleTechLevel.ADVANCED);

        return misc;
    }

    public static MiscType createPCMT() {
        MiscType misc = new MiscType();
        //TODO Not direct game rules, but weight should be variable not just 10 tons.
        misc.tonnage = 10;
        misc.cost = 2000000;
        misc.name = "Power Collector and Microwave Transmitter (10 Tons)";
        misc.setInternalName("PCMT");
        misc.flags = misc.flags.and(F_SS_EQUIPMENT);
        misc.rulesRefs = "337, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setProductionFactions(F_TA);
        return misc;
    }

    public static MiscType createLithiumFusionBattery() {
        MiscType misc = new MiscType();
        // TODO - Games rules and Costs (Cost of all K-F Drive components togetherx3)
        misc.name = "Lithium-Fusion Battery";
        misc.setInternalName("LithumFusion");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        
        misc.cost = 0;
        misc.flags = misc.flags.or(F_LF_STORAGE_BATTERY).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "323, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_E, RATING_F, RATING_E, RATING_E)
                .setISAdvancement(2520, 2529, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2520, 2529, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false);
        return misc;
    }

    // Lasers - Almost all under the weapons.
    public static MiscType createISLaserInsulator() {
        MiscType misc = new MiscType();
        misc.name = "Laser Insulator";
        misc.setInternalName("ISLaserInsulator");
        misc.addLookupName("CLLaserInsulator");
        misc.tonnage = 0.5;
        misc.criticals = 1;
    misc.tankslots = 0;
    misc.svslots = 0;
        misc.cost = 3000;
        misc.flags = misc.flags.or(MiscType.F_LASER_INSULATOR).or(MiscType.F_SUPPORT_TANK_EQUIPMENT)
                .or(MiscType.F_MECH_EQUIPMENT).or(MiscType.F_FIGHTER_EQUIPMENT).or(MiscType.F_TANK_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.rulesRefs = "322, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_E, RATING_F, RATING_F)
                .setAdvancement(2575, DATE_NONE, DATE_NONE, 2820, DATE_NONE)
                .setPrototypeFactions(F_TH).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    // Engine Tech Progression is in Engine.java

    // Mine Dispensing/Clearing Systems

    public static MiscType createMine() {
        MiscType misc = new MiscType();

        misc.name = "Mine";
        misc.setInternalName("Mine");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.flags = misc.flags.or(F_MINE).or(F_BA_EQUIPMENT);
        misc.bv = 4;

        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_C);
        misc.techAdvancement.setAvailability(new int[] { RATING_E, RATING_E, RATING_D, RATING_X });
        return misc;
    }

    public static MiscType createISVehicularMineDispenser() {
        MiscType misc = new MiscType();

        misc.name = "Vehicular Mine Dispenser";
        misc.setInternalName("ISVehicularMineDispenser");
        misc.addLookupName("CLVehicularMineDispenser");
        misc.cost = 20000;
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_VEHICLE_MINE_DISPENSER).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT);
        misc.bv = 8; // because it includes 2 mines
        misc.rulesRefs = "325, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_E, RATING_E, RATING_E, RATING_E)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createISSpaceMineDispenser() {
        MiscType misc = new MiscType();

        misc.name = "Space Mine Dispenser";
        misc.setInternalName("ISSpaceMineDispenser");
        misc.addLookupName("CLSpaceMineDispenser");
        misc.cost = 15000;
        misc.tonnage = 10;
        // TODO : implement game rules for this, analog to the mine for BAs
        misc.flags = misc.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .or(F_SPACE_MINE_DISPENSER);
        misc.bv = 200; // because it includes 2 mines. 100 for each mine,
                       // because it deals a max potential damage of 100
        misc.rulesRefs = "325, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_E, RATING_F, RATING_E, RATING_E)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createISMineSweeper() {
        MiscType misc = new MiscType();
        misc.name = "Mine Sweeper";
        misc.setInternalName("ISMineSweeper");
        misc.addLookupName("ClanMineSweeper");
        misc.tonnage = 3;
        misc.criticals = 1;
        misc.cost = 40000;
        misc.flags = misc.flags.or(F_MINESWEEPER).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.bv = 30;
        misc.rulesRefs = "326, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    // Miscellaneous Systems

    public static MiscType createBlueShield() {
        MiscType misc = new MiscType();
        misc.name = "Blue Shield Particle Field Damper";
        misc.shortName = "Blue Shield";
        misc.setInternalName(misc.name);
        misc.setModes(new String[] { "Off", "On" });
        misc.instantModeSwitch = false;
        misc.explosive = true;
        misc.tonnage = 3;
        misc.criticals = CRITICALS_VARIABLE;
        misc.spreadable = true;
        misc.cost = 1000000;
        misc.omniFixedOnly = true;
        misc.flags = misc.flags.or(F_BLUE_SHIELD).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "296, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_F)
                .setISAdvancement(3053, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createBoobyTrap() {
        MiscType misc = new MiscType();

        misc.name = "Booby Trap";
        misc.setInternalName("ISBoobyTrap");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.cost = 100000;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_BOOBY_TRAP).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.rulesRefs = "297, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_D, RATING_F, RATING_D, RATING_D)
                .setISAdvancement(DATE_PS, 3080, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false).setProductionFactions(F_WB)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createCargo() {
        MiscType misc = new MiscType();

        misc.name = "Cargo";
        misc.setInternalName(misc.name);
        misc.addLookupName("Cargo (1 ton)");
        misc.addLookupName("Cargo (0.5 tons)");
        misc.addLookupName("Cargo (1.5 tons)");
        misc.addLookupName("Cargo (2 tons)");
        misc.addLookupName("Cargo (2.5 tons)");
        misc.addLookupName("Cargo (3 tons)");
        misc.addLookupName("Cargo (3.5 tons)");
        misc.addLookupName("Cargo (4 tons)");
        misc.addLookupName("Cargo (4.5 tons)");
        misc.addLookupName("Cargo (5 tons)");
        misc.addLookupName("Cargo (5.5 tons)");
        misc.addLookupName("Cargo (6 tons)");
        misc.addLookupName("Cargo (6.5 tons)");
        misc.addLookupName("Cargo (7 tons)");
        misc.addLookupName("Cargo (7.5 tons)");
        misc.addLookupName("Cargo (8 ton)");
        misc.addLookupName("Cargo (8.5 tons)");
        misc.addLookupName("Cargo (9 tons)");
        misc.addLookupName("Cargo (9.5 tons)");
        misc.addLookupName("Cargo (10 tons)");
        misc.addLookupName("Cargo (10.5 tons)");
        misc.addLookupName("Cargo (11 tons)");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_VARIABLE_SIZE).or(F_MECH_EQUIPMENT)
                .or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setAdvancement(DATE_PS, DATE_PS, DATE_PS)
                .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A);
        return misc;
    }

    public static MiscType createLiquidCargo() {
        MiscType misc = new MiscType();

        misc.name = "Liquid Storage";
        misc.setInternalName(misc.name);
        misc.addLookupName("Liquid Storage (1 ton)");
        misc.addLookupName("Liquid Storage (0.5 tons)");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_LIQUID_CARGO).or(F_VARIABLE_SIZE).or(F_MECH_EQUIPMENT)
                .or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setAdvancement(DATE_PS, DATE_PS, DATE_PS)
            .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A);
        return misc;
    }

    public static MiscType createCargoContainer() {
        MiscType misc = new MiscType();

        misc.name = "Cargo Container (10 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 10;
        misc.criticals = 1;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_SC_EQUIPMENT)
                .or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.industrial = true;
          misc.tankslots = 1;
        misc.rulesRefs = "239, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    // Centurion Weapon System in Weapons Package.

    public static MiscType createISChaffPod() {
        // TODO: add game rules for this
        MiscType misc = new MiscType();
        misc.name = "Chaff Pod";
        misc.setInternalName("ISChaffPod");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 2000;
        misc.flags = misc.flags.or(F_CHAFF_POD).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_MECH_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.bv = 19;
        misc.rulesRefs = "299, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(3069, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createChameleonLightPolarizationShield() {
        MiscType misc = new MiscType();

        misc.name = "Chameleon Light Polarization Shield";
        misc.setInternalName("Chameleon Light Polarization Shield");
        misc.addLookupName("Chameleon Light Polarization Field");
        misc.addLookupName("ChameleonLightPolarizationShield");
        misc.addLookupName("ChameleonLightPolarizationField");
        misc.tonnage = 0;
        misc.criticals = 6;
        misc.spreadable = true;
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(false);
        misc.flags = misc.flags.or(F_CHAMELEON_SHIELD).or(F_MECH_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 600000;
        misc.rulesRefs = "300, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_F)
                .setISAdvancement(2630, DATE_NONE, DATE_NONE, 2790, 3099)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setReintroductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    // CommsGear
    public static MiscType createCommsGear() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear");
        misc.addLookupName("Communications Equipment (1 ton)");
        misc.addLookupName("Communications Equipment (2 ton)");
        misc.addLookupName("Communications Equipment (3 ton)");
        misc.addLookupName("Communications Equipment (4 ton)");
        misc.addLookupName("Communications Equipment (5 ton)");
        misc.addLookupName("Communications Equipment (6 ton)");
        misc.addLookupName("Communications Equipment (7 ton)");
        misc.addLookupName("Communications Equipment (8 ton)");
        misc.addLookupName("Communications Equipment (9 ton)");
        misc.addLookupName("Communications Equipment (10 ton)");
        misc.addLookupName("Communications Equipment (11 ton)");
        misc.addLookupName("Communications Equipment (12 ton)");
        misc.addLookupName("Communications Equipment (13 ton)");
        misc.addLookupName("Communications Equipment (14 ton)");
        misc.addLookupName("Communications Equipment (15 ton)");
        misc.addLookupName("CommsGear:1");
        misc.addLookupName("CommsGear:2");
        misc.addLookupName("CommsGear:3");
        misc.addLookupName("CommsGear:4");
        misc.addLookupName("CommsGear:5");
        misc.addLookupName("CommsGear:6");
        misc.addLookupName("CommsGear:7");
        misc.addLookupName("CommsGear:8");
        misc.addLookupName("CommsGear:9");
        misc.addLookupName("CommsGear:10");
        misc.addLookupName("CommsGear:11");
        misc.addLookupName("CommsGear:12");
        misc.addLookupName("CommsGear:13");
        misc.addLookupName("CommsGear:14");
        misc.addLookupName("CommsGear:15");
        misc.shortName = "CommsGear";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.svslots = 1;
        misc.tankslots = 1;
        misc.cost = COST_VARIABLE;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMMUNICATIONS).or(F_VARIABLE_SIZE).or(F_MECH_EQUIPMENT)
                .or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);
        misc.industrial = true;
        misc.rulesRefs = "212, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }
    public static MiscType createISCollapsibleCommandModule() {
        MiscType misc = new MiscType();
        misc.name = "Collapsible Command Module";
        misc.setInternalName("ISCollapsibleCommandModule");
        misc.addLookupName("ISCCM");
        misc.addLookupName("CollapsibleCommandModule");
        misc.tonnage = 16;
        misc.criticals = 12;
        misc.cost = 500000;
        misc.flags = misc.flags.or(F_CCM).or(F_MECH_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "301, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_E, RATING_F, RATING_E, RATING_F)
                .setISAdvancement(2700, 2710, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2700, 2710, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    // TODO Docking Hardpoint (Non-Spacecraft) - IO pg 45 - Need to decide how
    // this would be coded.

    // TODO Docking Thrusters - IO pg 45 - Need to decide how this would be
    // coded.

    public static MiscType createFieldKitchen() {
        MiscType misc = new MiscType();
        misc.name = "Field Kitchen";
        misc.setInternalName("FieldKitchen");
        misc.tonnage = 3;
        misc.cost = 25000;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_FIELD_KITCHEN).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT)
                .or(F_SS_EQUIPMENT);
        misc.bv = 0;
        misc.industrial = true;
        misc.rulesRefs = "217, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setAdvancement(DATE_PS, DATE_PS, DATE_PS)
            .setTechRating(RATING_A).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createExtendedFuelTank() {
        MiscType misc = new MiscType();

        misc.name = "Extended Fuel Tank";
        misc.setInternalName(EquipmentTypeLookup.EXTENDED_FUEL_TANK);
        misc.addLookupName("Extended Fuel Tank (1 ton)");
        misc.addLookupName("Extended Fuel Tank (0.5 tons)");
        misc.addLookupName("Extended Fuel Tank (1.5 tons)");
        misc.addLookupName("Extended Fuel Tank (2 tons)");
        misc.addLookupName("Extended Fuel Tank (2.5 tons)");
        misc.addLookupName("Extended Fuel Tank (3 tons)");
        misc.addLookupName("Extended Fuel Tank (3.5 tons)");
        misc.addLookupName("Extended Fuel Tank (4 tons)");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = 2000;
        misc.flags = misc.flags.or(F_FUEL).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT);
        misc.explosive = true;
        misc.industrial = true;
        misc.rulesRefs = "244, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_D, RATING_D, RATING_C)
                .setAdvancement(DATE_NONE, 2300, 2300).setISApproximate(false, true, false)
                .setProductionFactions(F_TA);
        return misc;
    }

    public static MiscType createExternalStoresHardpoint() {
        MiscType misc = new MiscType();
        misc.tonnage = 0.2;
        misc.cost = 5000;
        misc.name = "External Stores Hardpoint";
        misc.setInternalName(misc.name);
        misc.flags = misc.flags.or(F_EXTERNAL_STORES_HARDPOINT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "216, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_B);
        misc.techAdvancement.setAvailability(new int[] { RATING_D, RATING_E, RATING_D, RATING_X });
        return misc;
    }

    // Handheld Weapon - May 2017 - Under development in a separate branch

    public static MiscType createISHarJel() {
        MiscType misc = new MiscType();
        misc.name = "BattleMech/Vehicle HarJel System";
        misc.setInternalName("IS HarJel");
        misc.addLookupName("IS HarJel");
        misc.addLookupName("Clan HarJel");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 120000;
        misc.flags = misc.flags.or(F_HARJEL).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT);
        misc.omniFixedOnly = false;
        misc.bv = 0;
        misc.rulesRefs = "288, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(3067, 3115, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(3059, 3115, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CSF, F_LC)
                .setProductionFactions(F_CSF).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createHarJelII() {
        MiscType misc = new MiscType();
        misc.name = "HarJel Repair Systems (HarJel II)";
        misc.setInternalName(misc.name);
        misc.addLookupName("HarJel II Self-Repair System");
        misc.shortName = "Harjel II";
        misc.tonnage = 2;
        misc.criticals = 1;
        misc.cost = 240000;
        misc.flags = misc.flags.or(F_HARJEL_II).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = -1;
        misc.setInstantModeSwitch(true);
        String[] modes = { S_HARJEL_II_1F1R, S_HARJEL_II_2F0R, S_HARJEL_II_0F2R };
        misc.setModes(modes);
        misc.rulesRefs = "88, IO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
                .setClanAdvancement(3120, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createHarJelIII() {
        MiscType misc = new MiscType();
        misc.name = "HarJel Repair Systems (HarJel III)";
        misc.addLookupName("HarJel III Self-Repair System");
        misc.setInternalName(misc.name);
        misc.shortName = "Harjel III";
        misc.tonnage = 3;
        misc.criticals = 2;
        misc.cost = 360000;
        misc.flags = misc.flags.or(F_HARJEL_III).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = -1;
        misc.setInstantModeSwitch(true);
        String[] modes = { S_HARJEL_III_2F2R, S_HARJEL_III_4F0R, S_HARJEL_III_3F1R, S_HARJEL_III_1F3R,
                S_HARJEL_III_0F4R };
        misc.setModes(modes);
        misc.rulesRefs = "88, IO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
                .setClanAdvancement(3137, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createISMobileFieldBase() {
        MiscType misc = new MiscType();
        misc.name = "Mobile Field Base";
        misc.setInternalName("ISMobileFieldBase");
        misc.addLookupName("CLMobileFieldBase");
        misc.tonnage = 20;
        misc.cost = 150000;
        misc.flags = misc.flags.or(F_MOBILE_FIELD_BASE).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT)
                .or(F_SS_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "330, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_F, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(2540, 3059, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2540, 3059, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_FS, F_LC);
        return misc;
    }

    public static MiscType createMASH() {
        MiscType misc = new MiscType();

        misc.name = "MASH Equipment";
        misc.setInternalName(misc.name);
        misc.addLookupName("MASH Core Component");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_VARIABLE_SIZE).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT)
                .or(F_SS_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT).or(F_MASH);
        misc.industrial = true;
        misc.rulesRefs = "228, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_B).setAvailability(RATING_C, RATING_E, RATING_D, RATING_C)
                .setISAdvancement(DATE_PS, DATE_PS).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createNullSignatureSystem() {
        MiscType misc = new MiscType();

        misc.name = "Null Signature System";
        misc.setInternalName("Mek Null Signature System");
        misc.addLookupName("Null Signature System");
        misc.addLookupName("NullSignatureSystem");
        misc.tonnage = 0;
        misc.criticals = 7;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_NULLSIG).or(F_MECH_EQUIPMENT);
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(false);
        misc.bv = 0;
        misc.cost = 1400000;
        misc.rulesRefs = "336, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_X, RATING_X, RATING_F)
                .setISAdvancement(2615, 2630, DATE_NONE, 2790, 3110).setISApproximate(true, false, false, false, true)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH).setReintroductionFactions(F_CS)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createParamedicEquipment() {
        MiscType misc = new MiscType();

        misc.name = "Paramedic Equipment";
        misc.setInternalName(misc.name);
        misc.tonnage = 0.25;
        misc.criticals = 1;
        misc.tankslots = 1;
        misc.cost = 7500;
        misc.flags = misc.flags.or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT).or(F_MECH_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "233, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    // Power Amplifiers are captured as part of the construction process and are
    // abstracted.

    public static MiscType createHandheldSearchlight() {
        MiscType misc = new MiscType();

        misc.name = "Searchlight (Handheld)";
        misc.setInternalName("HHSearchlight");
        misc.shortName = "Searchlight";
        misc.tonnage = 0.005;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_SEARCHLIGHT).or(F_MECH_EQUIPMENT).or(F_PROTOMECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 500;
        misc.rulesRefs = "237, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createSearchlight() {
        MiscType misc = new MiscType();

        misc.name = "Searchlight (Mounted)";
        misc.setInternalName("Searchlight");
        misc.shortName = "Searchlight";
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.tankslots = 1;
    misc.flags = misc.flags.or(F_SEARCHLIGHT).or(F_MECH_EQUIPMENT).or(F_PROTOMECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
        .or(F_FIGHTER_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 2000;
        misc.rulesRefs = "237, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
        .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
        .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
        .setISApproximate(false, false, false, false, false)
        .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
        .setClanApproximate(false, false, false, false, false);

        return misc;
    }

    public static MiscType createVeeDropChuteStd() {
        MiscType misc = new MiscType();
        // TODO: game rules
        misc.name = "Vehicular DropChute (Standard)";
        misc.setInternalName("VeeDropChuteStd");
        misc.tonnage = 2;
        misc.criticals = 0;
        misc.flags = misc.flags.or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_VEEDC).or(F_REUSABLE).andNot(F_MECH_EQUIPMENT)
                .andNot(F_BA_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 1000;
        misc.rulesRefs = "348, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_D, RATING_E, RATING_B, RATING_B)
                .setISAdvancement(2348, 2351, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2348, 2351, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createVeeDropChuteCamo() {
        MiscType misc = new MiscType();
        // TODO: game rules
        misc.name = "Vehicular DropChute (Camouflage)";
        misc.setInternalName("VeeDropChuteCamo");
        misc.tonnage = 2;
        misc.criticals = 0;
        misc.flags = misc.flags.or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_VEEDC).or(F_REUSABLE).andNot(F_MECH_EQUIPMENT)
                .andNot(F_BA_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 3000;
        misc.rulesRefs = "348, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_E, RATING_E, RATING_C, RATING_C)
                .setISAdvancement(2348, 2353, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2348, 2353, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createVeeDropChuteStealth() {
        MiscType misc = new MiscType();
        // TODO: game rules
        misc.name = "Vehicular DropChute (Stealth)";
        misc.setInternalName("VeeDropChuteStealth");
        misc.tonnage = 2.2;
        misc.criticals = 0;
        misc.flags = misc.flags.or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_VEEDC).or(F_REUSABLE).andNot(F_MECH_EQUIPMENT)
                .andNot(F_BA_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 5000;
        misc.rulesRefs = "348, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_D, RATING_E, RATING_C, RATING_C)
                .setISAdvancement(2348, 2355, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2348, 2355, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createVeeDropChuteReuse() {
        MiscType misc = new MiscType();
        // TODO: game rules
        misc.name = "Vehicular DropChute (Reuseable)";
        misc.setInternalName("VeeDropChuteReuse");
        misc.tonnage = 2.5;
        misc.criticals = 0;
        misc.flags = misc.flags.or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_VEEDC).or(F_REUSABLE).andNot(F_MECH_EQUIPMENT)
                .andNot(F_BA_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 0;
        misc.rulesRefs = "348, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_E, RATING_F, RATING_F, RATING_E)
                .setISAdvancement(2348, 2353, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2348, 2353, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createVoidSignatureSystem() {
        MiscType misc = new MiscType();

        misc.name = "Void Signature System";
        misc.setInternalName("Mek Void Signature System");
        misc.addLookupName("Void Signature System");
        misc.addLookupName("VoidSignatureSystem");
        misc.tonnage = 0;
        misc.criticals = 7;
        misc.spreadable = true;
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(false);
        misc.flags = misc.flags.or(F_VOIDSIG).or(F_MECH_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 2000000;
        misc.rulesRefs = "349, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_F)
                .setISAdvancement(3070, 3085).setPrototypeFactions(F_WB)
                .setProductionFactions(F_RS).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISMastMount() {
        MiscType misc = new MiscType();

        misc.name = "Mast Mount";
        misc.setInternalName("ISMastMount");
        misc.addLookupName("CLMastMount");
        misc.tonnage = 0.5;
        misc.tankslots = 0;
        misc.cost = 50000;
        misc.flags = misc.flags.or(F_MAST_MOUNT).or(F_VTOL_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.bv = BV_VARIABLE;
        misc.rulesRefs = "350, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_F, RATING_F, RATING_F, RATING_F)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    // Additional Protomech Equipment
    public static MiscType createProtoMagneticClamp() {
        MiscType misc = new MiscType();
        // TODO Game Rules
        misc.name = "Magnetic Clamps System";
        misc.setInternalName("ProtoMagneticClamp");
        misc.addLookupName("Proto Magnetic Clamp");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = 25000;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_MAGNETIC_CLAMP).or(F_PROTOMECH_EQUIPMENT).andNot(F_MECH_EQUIPMENT)
                .andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        String[] saModes = { "On", "Off" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(true);
        misc.bv = 1;
        misc.rulesRefs = "66, IO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3070, 3080, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CFM)
                .setProductionFactions(F_CSF).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createProtomechMeleeWeapon() {
        MiscType misc = new MiscType();
        // TODO Game Rules
        misc.name = "ProtoMech Melee Weapon";
        misc.setInternalName("ProtoMeleeWeapon");
        misc.shortName = "Melee Weapon";
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.cost = 50000;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_PROTOMECH_MELEE).or(F_PROTOMECH_EQUIPMENT);
        misc.subType = S_PROTOMECH_WEAPON;
        misc.bv = 1;
        misc.rulesRefs = "337, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(3067, DATE_NONE, 3077, DATE_NONE, DATE_NONE)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createProtoQuadMeleeSystem() {
        MiscType misc = new MiscType();
        // TODO Game Rules
        misc.name = "ProtoMech Quad Melee System";
        misc.setInternalName("ProtoQuadMeleeSystem");
        misc.shortName = "Quad Melee System";
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 70000;
        misc.hittable = true;
        misc.flags = misc.flags.or(F_PROTOMECH_MELEE).or(F_PROTOMECH_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.subType = S_PROTO_QMS;
        misc.bv = 1;
        misc.rulesRefs = "67, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3066, 3072, 3085, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCC)
                .setProductionFactions(F_CHH).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    // Mobile Hyperpulse Generators
    public static MiscType createISMobileHPG() {
        MiscType misc = new MiscType();
        // TODO Game Rules
        misc.name = "Mobile Hyperpulse Generators (Mobile HPG)";
        misc.setInternalName("ISMobileHPG");
        misc.addLookupName("ClanMobileHPG");
        misc.shortName = "Mobile HPG";
        misc.tonnage = 50;
        misc.criticals = 50;
        misc.cost = 1000000000;
        misc.omniFixedOnly = true;
        misc.flags = misc.flags.or(F_MOBILE_HPG).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT)
                .or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "330, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_F, RATING_F, RATING_F, RATING_F)
                .setISAdvancement(2645, 2655, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2645, 2655, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createISGroundMobileHPG() {
        MiscType misc = new MiscType();
        misc.name = "Mobile Hyperpulse Generators (Ground-Mobile HPG)";
        misc.setInternalName("ISGroundMobileHPG");
        misc.addLookupName("ClanGroundMobileHPG");
        misc.shortName = "Ground-Mobile HPG";
        misc.tonnage = 12;
        misc.criticals = 12;
        misc.cost = 4000000000.0;
        misc.flags = misc.flags.or(F_MOBILE_HPG).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_MECH_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT).or(F_HEAVY_EQUIPMENT)
                .or(F_SPLITABLE);
        misc.bv = 0;
        misc.rulesRefs = "330, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_F, RATING_F, RATING_F, RATING_F)
                .setISAdvancement(2740, 2751, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2740, 2751, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    // RISC Equipment

    // RISC Advanced Point Defense System (Standard) - In Weapons
    // RISC Advanced Point Defense System (Battle Armor) - In Weapons

    public static MiscType createRISCEmergencyCoolantSystem() {
        MiscType misc = new MiscType();
        // TODO Game Rules
        misc.name = "RISC Emergency Coolant System";
        misc.setInternalName("ISRISCEmergencyCoolantSystem");
        misc.shortName = "RISC Coolant System";
        misc.tonnage = 2;
        misc.criticals = 1;
        misc.cost = 460000;
        misc.flags = misc.flags.or(F_EMERGENCY_COOLANT_SYSTEM).or(F_MECH_EQUIPMENT);
        misc.explosive = true;
        misc.rulesRefs = "92, IO";
        misc.omniFixedOnly = true;
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
                .setISAdvancement(3136, DATE_NONE, DATE_NONE, 3140, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_RS)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return misc;
    }

    // RISC Heat Sink Override Kit - IO pg 45. Might need to be a Quirk.

    public static MiscType createRISCLaserPulseModule() {
        MiscType misc = new MiscType();
        misc.name = "RISC Laser Pulse Module";
        misc.setInternalName("ISRISCLaserPulseModule");
        misc.shortName = "RISC Laser Module";
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_RISC_LASER_PULSE_MODULE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT)
                .or(F_SS_EQUIPMENT).or(F_VTOL_EQUIPMENT).or(F_PROTOMECH_EQUIPMENT).or(F_HEAVY_EQUIPMENT);
        misc.explosive = true;
        misc.rulesRefs = "93, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
                .setISAdvancement(3137, DATE_NONE, DATE_NONE, 3140, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_RS)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
        // FIXME: implement game rules, only BV and construction rules
        // implemented
    }

    // Repeating TSEMP - See Weapons.

    public static MiscType createRISCSuperCooledMyomer() {
        MiscType misc = new MiscType();

        misc.name = "RISC Super-Cooled Myomer";
        misc.setInternalName(EquipmentTypeLookup.SCM);
        misc.shortName = "Super-Cooled Myomer";
        misc.tonnage = 0;
        misc.criticals = 6;
        misc.hittable = true;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_SCM).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        // TODO: add game rules, BV rules are implemented
        misc.rulesRefs = "94, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
                .setISAdvancement(3133, DATE_NONE, DATE_NONE, 3138, DATE_NONE)
                .setPrototypeFactions(F_RS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createRISCViralJammerDecoy() {

        // TODO Game Rules
        MiscType misc = new MiscType();
        misc.name = "RISC Viral Jammer (Decoy)";
        misc.setInternalName(misc.name);
        misc.tonnage = 2.5;
        misc.criticals = 1;
        misc.cost = 990000;
        misc.bv = 284;
        misc.flags = misc.flags.or(F_VIRAL_JAMMER_DECOY).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_PROTOMECH_EQUIPMENT);
        // TODO: game rules
        misc.rulesRefs = "94, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
                .setISAdvancement(3136, DATE_NONE, DATE_NONE, 3142, DATE_NONE)
                .setPrototypeFactions(F_RS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createRISCViralJammerHoming() {

        // TODO Game Rules
        MiscType misc = new MiscType();
        misc.name = "RISC Viral Jammer (Homing)";
        misc.setInternalName(misc.name);
        misc.tonnage = 2.5;
        misc.criticals = 1;
        misc.cost = 990000;
        misc.bv = 284;
        misc.flags = misc.flags.or(F_VIRAL_JAMMER_HOMING).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_PROTOMECH_EQUIPMENT);
        // TODO: game rules
        misc.rulesRefs = "94, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
                .setISAdvancement(3137, DATE_NONE, DATE_NONE, 3142, DATE_NONE)
                .setPrototypeFactions(F_RS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    // PPC Stuff - All in weapons PPC except Capacitor.

    public static MiscType createISPPCCapacitor() {
        MiscType misc = new MiscType();

        misc.name = "PPC Capacitor";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISPPCCapacitor");
        misc.addLookupName("LPPC Capacitor");
        misc.addLookupName("ISLightPPCCapacitor");
        misc.addLookupName("SNPPC Capacitor");
        misc.addLookupName("ISSNPPCCapacitor");
        misc.addLookupName("ERPPC Capacitor");
        misc.addLookupName("ISERPPCCapacitor");
        misc.addLookupName("HPPC Capacitor");
        misc.addLookupName("ISHeavyPPCCapacitor");
        misc.addLookupName("CLPPCCapacitor");
        misc.tonnage = 1.0;
        misc.criticals = 1;
        misc.tankslots = 0;
        misc.cost = 150000;
        misc.setModes(new String[] { "Off", "Charge" });
        misc.flags = misc.flags.or(F_PPC_CAPACITOR).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.setInstantModeSwitch(false);
        misc.explosive = true;
        // misc.bv = 88;
        misc.bv = 0;
        misc.rulesRefs = "337, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setISAdvancement(DATE_NONE, 3060, 3081,DATE_NONE,DATE_NONE)
            .setClanAdvancement(DATE_NONE,DATE_NONE, 3101,DATE_NONE,DATE_NONE)
            .setPrototypeFactions(F_DC).setProductionFactions(F_DC)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    // Structural Components (Mech)
    // Standard - See note above the Armor Standard (search for createStandard)

    public static MiscType createISEndoSteel() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_STEEL);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_STEEL, false));
        misc.addLookupName("IS EndoSteel");
        misc.addLookupName("IS Endo-Steel");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_ENDO_STEEL);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "224, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_E, RATING_D).setISAdvancement(2480, 2487, 3040, 2850, 3035)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC);
        return misc;
    }

    public static MiscType createISEndoSteelPrototype() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_PROTOTYPE);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_PROTOTYPE, false));
        misc.addLookupName("IS Endo Steel Prototype");
        misc.addLookupName("IS Endo-Steel Prototype");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 16;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_ENDO_STEEL_PROTO);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "71, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_X, RATING_X)
                .setISAdvancement(2471, DATE_NONE, DATE_NONE, 2487, 3035)
                .setISApproximate(true, false, false, true, true).setPrototypeFactions(F_TH)
                .setReintroductionFactions(F_DC);
        return misc;
    }

    public static MiscType createCLEndoSteel() {
        MiscType misc = new MiscType();
        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_STEEL);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_STEEL, true));
        misc.addLookupName("Clan Endo-Steel");
        misc.addLookupName("Clan EndoSteel");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 7;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_ENDO_STEEL);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "224, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_E, RATING_D, RATING_D)
                .setClanAdvancement(2825, 2827, 2830, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CIH)
                .setProductionFactions(F_CIH);
        return misc;
    }

    public static MiscType createISCompositeStructure() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_COMPOSITE);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_COMPOSITE, false));
        misc.addLookupName("Composite");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMPOSITE);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "342, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(DATE_NONE, 3061, 3082,DATE_NONE,DATE_NONE).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createISEndoComposite() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_COMPOSITE);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_COMPOSITE, false));
        misc.addLookupName("IS Endo-Composite");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_ENDO_COMPOSITE);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "342, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(DATE_NONE, 3067, 3085,DATE_NONE,DATE_NONE)
                .setISApproximate(false, false, true, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createClanEndoComposite() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_COMPOSITE);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_COMPOSITE, true));
        misc.addLookupName("Clan Endo-Composite");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 4;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_ENDO_COMPOSITE);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "342, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3073, 3085,DATE_NONE,DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CWX)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createReinforcedStructure() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_REINFORCED);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_REINFORCED));
        misc.addLookupName("IS Reinforced");
        misc.addLookupName("Clan Reinforced");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = true;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_REINFORCED);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "342, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(DATE_NONE, 3057, 3084,DATE_NONE,DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setClanAdvancement(DATE_NONE, 3065, 3084,DATE_NONE,DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CS, F_CGB).setProductionFactions(F_CGB)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createIndustrialStructure() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_INDUSTRIAL);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_INDUSTRIAL));
        misc.addLookupName(EquipmentType.getStructureTypeName(T_STRUCTURE_INDUSTRIAL, false));
        misc.addLookupName(EquipmentType.getStructureTypeName(T_STRUCTURE_INDUSTRIAL, true));
        misc.addLookupName("IS Industrial Structure");
        misc.addLookupName("Clan Industrial Structure");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.bv = 0;
        misc.industrial = true;
        misc.flags = misc.flags.or(F_INDUSTRIAL_STRUCTURE);
        misc.rulesRefs = "224, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setAdvancement(2300, 2350, 2490, DATE_NONE, DATE_NONE)
                .setPrototypeFactions(F_TA).setProductionFactions(F_TH);
        return misc;
    }

    /*
     * TODO - IndustrialMech Structure w/ Environmental Sealing Per IO pg 48 Looks
     * like we the Enviro-sealing separate from the armor. Their is a TP difference
     * between the two.
     */

    /*
     * A lot of the Structures on IO Pg 48 have been captured in Mech.java which
     * used Construction progression from IO pg 50. There are date differences
     * between the structures and construction dates. To be fully canon components
     * would need to be captured as part of the tech progression.
     */

    // Actuators and Actuator Systems IO pg 48

    public static MiscType createISAES() {
        MiscType misc = new MiscType();
        misc.name = "Actuator Enhancement System (AES)";
        misc.setInternalName("ISAES");
        misc.addLookupName("IS Actuator Enhancement System");
        misc.addLookupName("ISActuatorEnhancementSystem");
        misc.addLookupName("CLAES");
        misc.addLookupName("CL Actuator Enhancement System");
        misc.addLookupName("CLActuatorEnhancementSystem");
        misc.shortName = "AES";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_ACTUATOR_ENHANCEMENT_SYSTEM).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = BV_VARIABLE;
        misc.rulesRefs = "279, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3070, 3108, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(3070, 3109, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_MERC)
                .setProductionFactions(F_RD).setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    // Musculature
    public static MiscType createTSM() {
        MiscType misc = new MiscType();

        misc.name = "Triple Strength Myomer";
        misc.setInternalName(EquipmentTypeLookup.TSM);
        misc.addLookupName("IS TSM");
        misc.addLookupName("TSM");
        misc.addLookupName("Triple Strength Myomer");
        misc.tonnage = 0;
        misc.criticals = 6;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_TSM).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "240, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setISAdvancement(3028, 3050, 3055, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS, F_CC)
                .setProductionFactions(F_CC);

        return misc;
    }

    // TODO Prototype TSM, see IO pg 104, Coding Proto TSM means Anti TSM missiles
    // need to coded to counter them

    public static MiscType createIndustrialTSM() {
        MiscType misc = new MiscType();

        misc.name = "Industrial Triple Strength Myomer";
        misc.setInternalName(EquipmentTypeLookup.ITSM);
        misc.addLookupName("IS Industrial TSM");
        misc.addLookupName("Industrial TSM");
        misc.tonnage = 0;
        misc.criticals = 12;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_INDUSTRIAL_TSM).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.industrial = true;
        misc.rulesRefs = "240, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3035, 3045, 3055, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return misc;
    }

    public static MiscType createPrototypeTSM() {
        MiscType misc = new MiscType();

        misc.name = "Prototype Triple Strength Myomer";
        misc.setInternalName(EquipmentTypeLookup.P_TSM);
        misc.shortName = "Prototype TSM";
        misc.tonnage = 0;
        misc.criticals = 6;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_TSM).or(F_PROTOTYPE).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "103, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_F, RATING_X, RATING_X)
                .setISAdvancement(3028, DATE_NONE, DATE_NONE, 3050)
                .setISApproximate(true, false, false, false)
                .setPrototypeFactions(F_CC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createIndustrialMechEnvironmentalSealing() {
        MiscType misc = new MiscType();
        misc.name = "Environmental Sealing (Mech)";
        misc.shortName = "Environmental Sealing";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 8;
        misc.tankslots = 0;
        misc.cost = COST_VARIABLE;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_ENVIRONMENTAL_SEALING).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "216, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setAdvancement(2300, 2350, 2495)
                .setApproximate(true, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TH).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C);
        return misc;
    }

    // Gyros - IO pg 48 - Located in Techconstants.java

    /*
     * Structural Components (Non-Mech) - IO pg 48 TODO - Almost all of these need
     * to be reviewed when it comes time to properly implement
     * Dropships,Jumpships,Warships
     */

    public static MiscType createISFlotationHull() {
        MiscType misc = new MiscType();
        misc.name = "Combat Vehicle Chassis Mod (Flotation Hull)";
        misc.setInternalName("ISFlotationHull");
        misc.shortName = "Flotation Hull";
        misc.addLookupName("ClanFlotationHull");
        misc.addLookupName("Combat Vehicle Chassis Mod [Flotation Hull]");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.cost = EquipmentType.COST_VARIABLE;
        misc.flags = misc.flags.or(F_FLOTATION_HULL).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "302, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(2470, 2472, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2470, 2472, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_TH);
        return misc;
    }

    public static MiscType createISLimitedAmphibiousChassis() {
        MiscType misc = new MiscType();
        misc.name = "Combat Vehicle Chassis Mod [Limited Amphibious]";
        misc.setInternalName("ISLimitedAmphibiousChassis");
        misc.addLookupName("ISLimitedAmphibious");
        misc.addLookupName("ClanLimitedAmphibiousChassis");
        misc.addLookupName("ClanLimitedAmphibious");
        misc.tonnage = EquipmentType.TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_LIMITED_AMPHIBIOUS).or(F_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "302, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(2470, 2472, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2470, 2472, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_TH);
        return misc;
    }

    public static MiscType createISFullyAmphibiousChassis() {
        MiscType misc = new MiscType();
        misc.name = "Combat Vehicle Chassis Mod [Fully Amphibious]";
        misc.setInternalName("ISFullyAmphibiousChassis");
        misc.addLookupName("ISFullyAmphibious");
        misc.addLookupName("ClanFullyAmphibiousChassis");
        misc.addLookupName("ClanFullyAmphibious");
        misc.tonnage = EquipmentType.TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_FULLY_AMPHIBIOUS).or(F_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "302, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(2470, 2474, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2470, 2474, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_TH);
        return misc;
    }

    public static MiscType createISCVDuneBuggyChassis() {
        MiscType misc = new MiscType();
        misc.name = "Combat Vehicle Chassis Mod [Dune Buggy]";
        misc.setInternalName("ISCVDuneBuggyChassis");
        misc.addLookupName("ISCVDuneBuggy");
        misc.addLookupName("ClanCVDuneBuggyChassis");
        misc.addLookupName("ClanCVDuneBuggy");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_DUNE_BUGGY).or(F_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "303, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(2470, 2471, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2470, 2471, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_TH);
        return misc;
    }

    public static MiscType createCVEnvironmentalSealedChassis() {
        MiscType misc = new MiscType();

        misc.name = "Combat Vehicle Chassis Mod [Environmental Sealing]";
        misc.shortName = "Environmental Sealing";
        misc.setInternalName("Environmental Sealed Chassis");
        misc.addLookupName("EnvironmentalSealingChassisMod");
        misc.addLookupName("Vacuum Protection");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE; // Cost accounted as part of unit cost
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_ENVIRONMENTAL_SEALING).or(F_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "303, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(DATE_NONE, 2475, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_NONE, 2475, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setProductionFactions(F_TH);
        return misc;
    }

    // Support Vee Chassis Mods - IO pg 49, TM pg 280
    public static MiscType createAmphibiousChassis() {
        MiscType misc = new MiscType();
        misc.name = "SV Chassis Mod [Amphibious]";
        misc.shortName = "Amphibious";
        misc.setInternalName("AmphibiousChassis");
        misc.addLookupName("AmphibiousChassisMod");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_AMPHIBIOUS).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122, TM";

        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_C);
        misc.techAdvancement.setAvailability(new int[] { RATING_C, RATING_D, RATING_C, RATING_C });
        return misc;
    }

    public static MiscType createArmoredChassis() {
        MiscType misc = new MiscType();

        misc.name = "SV Chassis Mod [Armored Chassis]";
        misc.shortName = "Armored Chassis";
        misc.setInternalName("Armored Chassis");
        misc.addLookupName("ArmoredChassisMod");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_ARMORED_CHASSIS).or(F_CHASSIS_MODIFICATION)
                .or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122, TM";

        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_C, RATING_E, RATING_D, RATING_D });
        return misc;
    }

    public static MiscType createBicycleModification() {
        MiscType misc = new MiscType();

        misc.name = "SV Chassis Mod [Bicycle]";
        misc.shortName = "Bicycle";
        misc.setInternalName("BicycleChassisMod");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_BICYCLE).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122, TM";
        misc.tankslots = 0;
        misc.industrial = true;
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createConvertibleModification() {
        MiscType misc = new MiscType();

        misc.name = "SV Chassis Mod [Convertible]";
        misc.shortName = "Convertible";
        misc.setInternalName("ConvertibleChassisMod");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_CONVERTIBLE).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122, TM";
        misc.tankslots = 0;
        misc.industrial = true;
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createISSVDuneBuggyChassis() {
        MiscType misc = new MiscType();
        misc.name = "SV Chassis Mod [Dune Buggy]";
        misc.shortName = "Dune Buggy";
        misc.setInternalName("ISSVDuneBuggyChassis");
        misc.addLookupName("ISSVDuneBuggy");
        misc.addLookupName("ClanSVDuneBuggyChassis");
        misc.addLookupName("ClanSVDuneBuggy");
        misc.addLookupName("DuneBuggyChassisMod");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_DUNE_BUGGY).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);               
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "303, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(2470, 2471, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2470, 2471, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_TH);
        return misc;
    }

    public static MiscType createEnvironmentalSealingChassisMod() {
        MiscType misc = new MiscType();
        misc.name = "Environmental Sealing";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 8;
        misc.tankslots = 0;
        misc.svslots = 0;
        misc.cost = COST_VARIABLE;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_ENVIRONMENTAL_SEALING).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_C);
        misc.techAdvancement.setAvailability(RATING_B, RATING_D, RATING_C, RATING_C);
        return misc;
    }

    public static MiscType createExternalPowerPickup() {
        MiscType misc = new MiscType();
        misc.name = "External Power Pickup";
        misc.setInternalName("ExternalPowerPickupChassisMod");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION).or(F_EXTERNAL_POWER_PICKUP);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "243, TO";
        misc.tankslots = 0;
        misc.industrial = true;
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setAdvancement(DATE_NONE, DATE_NONE, DATE_PS)
            .setTechRating(RATING_B).setAvailability(new int[] { RATING_C, RATING_D, RATING_C, RATING_C });
        return misc;
    }

    public static MiscType createHydroFoilChassisModification() {
        MiscType misc = new MiscType();

        misc.name = "SV Chassis Mod [HydroFoil]";
        misc.shortName = "Hydrofoil";
        misc.setInternalName("HydroFoilChassisMod");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_HYDROFOIL).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122, TM";
        misc.tankslots = 0;
        misc.industrial = true;
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_B);
        misc.techAdvancement.setAvailability(new int[] { RATING_C, RATING_D, RATING_C, RATING_C });
        return misc;
    }

    public static MiscType createMonocycleModification() {
        MiscType misc = new MiscType();

        misc.name = "SV Chassis Mod [Monocycle]";
        misc.shortName = "Monocycle";
        misc.setInternalName("MonocycleChassisMod");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_MONOCYCLE).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122, TM";
        misc.tankslots = 0;
        misc.industrial = true;
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_B);
        misc.techAdvancement.setAvailability(new int[] { RATING_D, RATING_D, RATING_D, RATING_D });
        return misc;
    }

    public static MiscType createISOffRoadChassis() {
        MiscType misc = new MiscType();
        misc.name = "SV Chassis Mod [Off-Road]";
        misc.shortName = "Off-Road";
        misc.setInternalName("ISOffRoadChassis");
        misc.addLookupName("ISOffRoad");
        misc.addLookupName("ClanOffRoadChassis");
        misc.addLookupName("CLOffRoad");
        misc.addLookupName("OffroadChassisMod");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.cost = COST_VARIABLE; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_OFF_ROAD).or(F_CHASSIS_MODIFICATION)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122, TM";

        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_B);
        misc.techAdvancement.setAvailability(new int[] { RATING_B, RATING_C, RATING_B, RATING_B });
        return misc;
    }

    public static MiscType createOmniChassisMod() {
        MiscType misc = new MiscType();
        misc.name = "SV Chassis Mod [Omni]";
        misc.shortName = "Omni";
        misc.setInternalName("OmniChassisMod");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_CHASSIS_MODIFICATION).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122, TM";

        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3052)
                .setClanAdvancement(2854, 2856, 2864).setClanApproximate(true)
                .setPrototypeFactions(F_CCY, F_CSF).setProductionFactions(F_CCY, F_DC)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_E, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createPropChassisModification() {
        MiscType misc = new MiscType();
        misc.name = "SV Chassis Mod [Propeller-Driven]";
        misc.shortName = "Propeller-Driven";
        misc.setInternalName("PropChassisMod");
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.tankslots = 0;
        misc.flags = misc.flags.andNot(F_FIGHTER_EQUIPMENT).or(F_CHASSIS_MODIFICATION).or(F_PROP)
            .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "122, TM";
        // Setting this Pre-Spaceflight
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_B);
        misc.techAdvancement.setAvailability(new int[] { RATING_B, RATING_C, RATING_B, RATING_X });
        return misc;
    }

    public static MiscType createSnomobileChassis() {
        MiscType misc = new MiscType();
        misc.name = "SV Chassis Mod [Snowmobile]";
        misc.shortName = "Snowmobile";
        misc.setInternalName("SnowmobileChassis");
        misc.addLookupName("SnowmobileChassisMod");
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_SNOWMOBILE).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122, TM";
        // TODO: implement game rules

        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_B);
        misc.techAdvancement.setAvailability(new int[] { RATING_D, RATING_E, RATING_D, RATING_D });
        return misc;
    }

    public static MiscType createSTOLChassisMod() {
        MiscType misc = new MiscType();
        misc.name = "SV Chassis Mod [STOL]";
        misc.shortName = "STOL";
        misc.setInternalName("STOLChassisMod");
        misc.tonnage = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_STOL_CHASSIS).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122, TM";
        misc.tankslots = 0;
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_C);
        misc.techAdvancement.setAvailability(new int[] { RATING_B, RATING_C, RATING_B, RATING_B });
        return misc;
    }

    public static MiscType createSubmersibleChassisMod() {
        MiscType misc = new MiscType();

        misc.name = "SV Chassis Mod [Submersible]";
        misc.shortName = "Submersible";
        misc.setInternalName("SubmersibleChassisMod");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_SUBMERSIBLE).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.tankslots = 0;
        misc.industrial = true;
        misc.rulesRefs = "122, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_B);
        misc.techAdvancement.setAvailability(new int[] { RATING_C, RATING_D, RATING_C, RATING_C });
        return misc;
    }

    public static MiscType createTractorModification() {
        MiscType misc = new MiscType();

        misc.name = "SV Chassis Mod [Tractor]";
        misc.shortName = "Tractor";
        misc.setInternalName(misc.name);
        misc.addLookupName("Tractor");
        misc.addLookupName("TractorChassisMod");
        misc.tonnage = 0; // accounted as part of the unit Construction
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_TRACTOR_MODIFICATION).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122, TM";
        misc.tankslots = 0;
        misc.industrial = true;
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createHitch() {
        MiscType misc = new MiscType();
        misc.name = "Trailer Hitch";
        misc.setInternalName(EquipmentTypeLookup.HITCH);
        misc.tonnage = 0;
        misc.cost = 0;
        misc.criticals = 0; // being errata'd to no slots.
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_HITCH).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.bv = 0;
        misc.industrial = true;
        misc.rulesRefs = "101, TM";

        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createTrailerModification() {
        MiscType misc = new MiscType();

        misc.name = "SV Chassis Mod [Trailer]";
        misc.shortName = "Trailer";
        misc.setInternalName(misc.name);
        misc.addLookupName("Trailer");
        misc.addLookupName("TrailerChassisMod");
        misc.tonnage = 0; // accounted as part of the unit Construction
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_TRAILER_MODIFICATION).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.tankslots = 0;
        misc.industrial = true;
        misc.rulesRefs = "122, TM";

        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createUltraLightChassisModification() {
        MiscType misc = new MiscType();
        misc.shortName = "Ultra-Light";
        misc.name = "SV Chassis Mod [Ultra-Light]";
        misc.setInternalName("UltraLightChassisMod");
        misc.tankslots = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION).or(F_ULTRA_LIGHT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "122, TM";
        // Setting this Pre-Spaceflight
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_D);
        misc.techAdvancement.setAvailability(new int[] { RATING_C, RATING_E, RATING_D, RATING_D });
        return misc;
    }

    public static MiscType createVSTOLChassisMod() {
        MiscType misc = new MiscType();
        misc.name = "SV Chassis Mod [VSTOL]";
        misc.shortName = "VSTOL";
        misc.setInternalName("VSTOLChassisMod");
        misc.tonnage = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_VSTOL_CHASSIS).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122, TM";
        misc.tankslots = 0;
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_C);
        misc.techAdvancement.setAvailability(new int[] { RATING_C, RATING_D, RATING_C, RATING_C });
        return misc;
    }

    // Transport Launching/Recovery Systems - IO pg 49.
    // Will all need to be added once DS/JS/WS editor is created.

    // Transport Bays, Quarters, and Seating
    // Currently captured as part of a transporter but should be expanded into
    // equipment

    // Turrets - IO pg 50
    // TODO Turret Mount for Buildings

    public static MiscType createISShoulderTurret() {
        MiscType misc = new MiscType();
        misc.name = "BattleMech Turret (Shoulder)";
        misc.setInternalName("ISShoulderTurret");
        misc.addLookupName("CLShoulderTurret");
        misc.shortName = "Shoulder Turret";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_SHOULDER_TURRET).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "347, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_C).setAvailability(RATING_F, RATING_X, RATING_F, RATING_E)
                .setAdvancement(2450, 3082, DATE_NONE, DATE_NONE, DATE_NONE)
                .setApproximate(false, true, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    public static MiscType createISHeadTurret() {
        MiscType misc = new MiscType();
        misc.name = "BattleMech Turret (Head)";
        misc.setInternalName("ISHeadTurret");
        misc.addLookupName("CLHeadTurret");
        misc.shortName = "Head Turret";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_HEAD_TURRET).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "347, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_F, RATING_E)
                .setAdvancement(2450, 3082, DATE_NONE, DATE_NONE, DATE_NONE)
                .setApproximate(false, true, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        return misc;
    }

    public static MiscType createISQuadTurret() {
        MiscType misc = new MiscType();
        misc.name = "BattleMech Turret (Quad)";
        misc.setInternalName("ISQuadTurret");
        misc.addLookupName("CLQuadTurret");
        misc.shortName ="Quad Turret";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_QUAD_TURRET).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "347, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_F, RATING_F, RATING_E)
                .setAdvancement(2320, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
                .setApproximate(false, true, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }

    /*
     * //Vehicular Dual Turret - IO pg 50 and TO pg 347/411 We support them but
     * their is no construction data for them.
     */
    public static MiscType createISSponsonTurret() {
        MiscType misc = new MiscType();
        misc.name = "Vehicular Sponson Turret";
        misc.setInternalName(EquipmentTypeLookup.SPONSON_TURRET);
        misc.addLookupName("ISSponsonTurret");
        misc.addLookupName("CLSponsonTurret");
        misc.shortName = "Sponson Turret";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.hittable = false;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_SPONSON_TURRET).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_HEAVY_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "348, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_B).setAvailability(RATING_F, RATING_F, RATING_F, RATING_D)
                .setAdvancement(DATE_PS, DATE_NONE, 3079, DATE_NONE, DATE_NONE)
                .setApproximate(false, false, true, false, false)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createPintleTurret() {
        MiscType misc = new MiscType();
        misc.name = "Pintle Mount";
        misc.setInternalName(EquipmentTypeLookup.PINTLE_TURRET);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.hittable = false;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_PINTLE_TURRET).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "234, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    // Battle Armor Tech

    public static MiscType createArmoredCowl() {
        MiscType misc = new MiscType();

        misc.name = "Armored Cowl";
        misc.setInternalName(misc.name);
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 10000;
        misc.flags = misc.flags.or(F_COWL).or(F_MECH_EQUIPMENT);
        misc.bv = 10;
        // Making this up based on the Strat Ops Quirk

        misc.techAdvancement.setTechBase(TECH_BASE_IS);
        misc.techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, 2439);
        misc.techAdvancement.setTechRating(RATING_C);
        misc.techAdvancement.setAvailability(new int[] { RATING_E, RATING_E, RATING_E, RATING_X });
        return misc;
    }

    // Start BattleArmor equipment

    public static MiscType createISBALightActiveProbe() {
        MiscType misc = new MiscType();

        misc.name = "Active Probe (Light)";
        misc.setInternalName(Sensor.ISBALIGHT_AP);
        misc.addLookupName(Sensor.CLBALIGHT_AP);
        misc.addLookupName("ISBAActiveProbe");
        misc.tonnage = 0.25;
        misc.criticals = 2;
        misc.cost = 50000;
        misc.flags = misc.flags.or(F_BAP).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "252, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_F, RATING_E, RATING_E)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3050, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2898, 2900, 3050, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return misc;
    }

    public static MiscType createBAISAngelECM() {
        MiscType misc = new MiscType();

        // Don't forget, this will eventually count double for ECCM.
        misc.name = "Angel ECM Suite";
        misc.setInternalName("BAISAngelECMSuite");
        misc.addLookupName("BA IS Angel ECM Suite");
        misc.addLookupName("BAISAngelECM");
        misc.addLookupName("ISBAAngelECM");
        misc.tonnage = .25;
        misc.criticals = 3;
        misc.cost = 750000;
        misc.flags = misc.flags.or(F_ECM).or(F_ANGEL_ECM).or(F_BA_EQUIPMENT);
        misc.bv = 100;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "279, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3063, DATE_NONE, 3089, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)                             
                .setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.STANDARD);

        return misc;
    }

    public static MiscType createBACLAngelECM() {
        MiscType misc = new MiscType();

        // Don't forget, this will eventually count double for ECCM.
        misc.name = "Angel ECM Suite";
        misc.setInternalName("BACLAngelECMSuite");
        misc.addLookupName("BA CL Angel ECM Suite");
        misc.addLookupName("BACLAngelECM");
        misc.addLookupName("CLBAAngelECM");
        misc.tonnage = .15;
        misc.criticals = 3;
        misc.cost = 750000;
        misc.flags = misc.flags.or(F_ECM).or(F_ANGEL_ECM).or(F_BA_EQUIPMENT);
        misc.bv = 100;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "279, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3059, DATE_NONE, 3089, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false).setPrototypeFactions(F_CNC)
                .setProductionFactions(F_CNC).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createSimpleCamo() {
        MiscType misc = new MiscType();

        misc.name = BattleArmor.CAMO_SYSTEM;
        misc.setInternalName(BattleArmor.CAMO_SYSTEM);
        misc.addLookupName("Simple Camo");
        misc.tonnage = .2;
        misc.criticals = 2;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_STEALTH).or(F_VISUAL_CAMO).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT)
                .andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "253, TM";
        misc.bv = 0;
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_F, RATING_F, RATING_E)
                .setISAdvancement(2790, 2800, 3058, DATE_NONE, DATE_NONE)
                .setISApproximate(true, true, false, false, false).setPrototypeFactions(F_CS)
                .setProductionFactions(F_CS);
        return misc;
    }

    public static MiscType createBACuttingTorch() {
        MiscType misc = new MiscType();

        misc.name = "Cutting Torch";
        misc.setInternalName("BACuttingTorch");
        misc.tonnage = 0.005;
        misc.criticals = 1;
        misc.cost = 1000;
        misc.flags = misc.flags.or(F_CUTTING_TORCH).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "254, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createISSingleHexECM() {
        MiscType misc = new MiscType();

        misc.name = BattleArmor.SINGLE_HEX_ECM;
        misc.setInternalName("IS BA ECM");
        misc.addLookupName("ECM Suite (Light)");
        misc.addLookupName("ISBAECM");
        misc.addLookupName("IS" + BattleArmor.SINGLE_HEX_ECM);
        misc.tonnage = .1;
        misc.criticals = 1;
        misc.cost = 50000;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_ECM).or(F_SINGLE_HEX_ECM).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT)
                .andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "254, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_F, RATING_E).setISAdvancement(2718, 2720, 3060, 2766, 3057)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FW, F_WB);
        return misc;
    }

    public static MiscType createCLSingleHexECM() {
        MiscType misc = new MiscType();

        misc.name = BattleArmor.SINGLE_HEX_ECM;
        misc.setInternalName("CL BA ECM");
        misc.addLookupName("CLBAECM");
        misc.addLookupName("CL" + BattleArmor.SINGLE_HEX_ECM);
        misc.tonnage = .075;
        misc.criticals = 1;
        misc.cost = 50000;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_ECM).or(F_SINGLE_HEX_ECM).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT)
                .andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "254, TM";
        misc.rulesRefs = "254, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_E, RATING_F, RATING_F, RATING_E)
                .setClanAdvancement(2718, 2720, 3060, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createISBAExtendedLifeSupport() {
        // TODO: add game rules for this
        MiscType misc = new MiscType();

        misc.name = "Extended Life Support";
        misc.setInternalName("BAExtendedLifeSupport");
        misc.addLookupName("CLBAExtendedLifeSupport");
        misc.addLookupName("ISBAExtendedLifeSupport");
        misc.cost = 10000;
        misc.tonnage = 0.025;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_EXTENDED_LIFESUPPORT).andNot(F_MECH_EQUIPMENT)
                .andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "254, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(2712, 2715, 2720, DATE_NONE, DATE_NONE)
                .setISApproximate(true, true, false, false, false)
                .setClanAdvancement(2712, 2715, 2720, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createISBAFuelTank() {
        MiscType misc = new MiscType();

        misc.name = "Fuel Tank";
        misc.setInternalName("ISBAFuelTank");
        misc.addLookupName("CLBAFuelTank");
        misc.tonnage = 0.05;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 500;
        misc.rulesRefs = "255, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_E, RATING_E, RATING_E)
                .setISAdvancement(2740, 2744, 3053, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2740, 2744, 3053, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    // Harjel - IO pg 51 - The Tech Progression captured as part of
    // BattleArmor.java

    public static MiscType createISBAHeatSensor() {
        MiscType misc = new MiscType();
        misc.name = "Heat Sensor";
        misc.setInternalName("ISBAHeatSensor");
        misc.addLookupName("CLBAHeatSensor");
        misc.cost = 15000;
        misc.tonnage = 0.020;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_HEAT_SENSOR).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "256, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_X, RATING_F, RATING_F, RATING_F)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3050, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2879, 2880, 3050, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CBS)
                .setProductionFactions(F_CBS);
        return misc;
    }

    public static MiscType createISImprovedSensors() {
        MiscType misc = new MiscType();

        misc.name = "Improved Sensors";
        misc.setInternalName(Sensor.ISIMPROVED);
        misc.addLookupName("IS BA Improved Sensors");
        misc.addLookupName("ISBAImprovedSensors");
        misc.tonnage = 0.065;
        misc.criticals = 1;
        misc.cost = 35000;
        misc.flags = misc.flags.or(F_BAP).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT);
        misc.rulesRefs = "257, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3051, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCLImprovedSensors() {
        MiscType misc = new MiscType();

        misc.name = "Improved Sensors";
        misc.setInternalName(Sensor.CLIMPROVED);
        misc.addLookupName("Clan BA Improved Sensors");
        misc.addLookupName("CLBAImprovedSensors");
        misc.tonnage = 0.045;
        misc.criticals = 1;
        misc.cost = 200000;
        misc.flags = misc.flags.or(F_BAP).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT);
        misc.rulesRefs = "257, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setClanAdvancement(2887, 2890, 3051, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CBS)
                .setProductionFactions(F_CBS);
        return misc;
    }

    public static MiscType createBALaserMicrophone() {
        MiscType misc = new MiscType();

        misc.name = "Laser Microphone";
        misc.setInternalName("BALaserMicrophone");
        misc.tonnage = 0.005;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 750;
        misc.rulesRefs = "258, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_E, RATING_E, RATING_F, RATING_F)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createParafoil() {
        MiscType misc = new MiscType();

        misc.name = "Parafoil";
        misc.setInternalName("BAParafoil");
        misc.tonnage = .035;
        misc.criticals = 1;
        misc.hittable = false;
        misc.cost = 3000;
        misc.flags = misc.flags.or(F_PARAFOIL).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "266, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_B, RATING_B, RATING_C, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createBAPowerPack() {
        MiscType misc = new MiscType();

        misc.name = "Power Pack";
        misc.setInternalName("BAPowerpack");
        misc.tonnage = .025;
        misc.criticals = 1;
        misc.cost = 1000;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "268, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_B, RATING_C, RATING_B, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createISBARemoteSensorDispenser() {
        MiscType misc = new MiscType();

        misc.name = "Remote Sensors/Dispenser";
        misc.setInternalName("ISBARemoteSensorDispenser");
        misc.addLookupName("IS BA Remote Sensor Dispenser");
        misc.addLookupName("CLBARemoteSensorDispenser");
        misc.addLookupName("Clan BA Remote Sensor Dispenser");
        misc.tonnage = 0.04;
        misc.criticals = 1;
        misc.cost = 7500;
        misc.hittable = true;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_SENSOR_DISPENSER).andNot(F_MECH_EQUIPMENT)
                .andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "268, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_E, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(2700, 3050, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2700, 3050, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_FS);
        return misc;
    }

    public static MiscType createBASearchlight() {
        MiscType misc = new MiscType();

        misc.name = "Searchlight [BA]";
        misc.setInternalName("BASearchlight");
        misc.shortName = "Searchlight";
        misc.tonnage = 0.005;
        misc.criticals = 1;
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_BA_SEARCHLIGHT).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT)
                .andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 500;
        misc.rulesRefs = "269, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createBAShotgunMicrophone() {
        MiscType misc = new MiscType();

        misc.name = "Shotgun Microphone";
        misc.setInternalName("BAShotgunMicrophone");
        misc.tonnage = 0.005;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 750;
        misc.rulesRefs = "258, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_C);
        misc.techAdvancement.setAvailability(new int[] { RATING_E, RATING_E, RATING_F, RATING_F });
        return misc;
    }

    public static MiscType createISBASpaceOperationsAdaptation() {
        MiscType misc = new MiscType();

        misc.name = "Space Operations Adaptation";
        misc.setInternalName("ISBASpaceOperationsAdaptation");
        misc.addLookupName("CLBASpaceOperationsAdaptation");
        misc.tonnage = 0.1;
        misc.criticals = 1;
        misc.cost = 50000;
        misc.flags = misc.flags.or(F_SPACE_ADAPTATION).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT)
                .andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "269, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_E, RATING_E, RATING_D)
                .setISAdvancement(DATE_NONE, 3011, 3015, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2890, 2895, 3015, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSR)
                .setProductionFactions(F_CSR, F_TC);
        return misc;
    }

    // BA Manipulators

    public static MiscType createBAArmoredGlove() {
        MiscType misc = new MiscType();

        misc.name = "BA Manipulators [Armored Gloves]";
        misc.setInternalName("BAArmoredGlove"); // This value MUST match the
                                                // name in
                                                // BattleArmor.MANIPULATOR_TYPE_STRINGS
        misc.shortName = "Armored Glove";
        misc.tonnage = 0.0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_ARMORED_GLOVE).or(F_AP_MOUNT).or(F_BA_MANIPULATOR);
        misc.bv = 0;
        misc.cost = 2500;
        misc.rulesRefs = "259, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_D, RATING_D, RATING_D, RATING_C)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createBABasicManipulator() {
        MiscType misc = new MiscType();

        misc.name = "BA Manipulators [Manipulator (Basic)]";
        misc.setInternalName("BABasicManipulator"); // This value MUST match the
                                                    // name in
                                                    // BattleArmor.MANIPULATOR_TYPE_STRINGS
        misc.shortName = "Basic Manipulator";
        misc.tonnage = 0.0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BA_MANIPULATOR).or(F_BASIC_MANIPULATOR);
        misc.bv = 0;
        misc.cost = 5000;
        misc.rulesRefs = "259, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_C, RATING_C)
                .setISAdvancement(DATE_ES, 2110, 2120, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, true, false, false)
                .setClanAdvancement(DATE_ES, 2110, 2120, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, true, false, false).setProductionFactions(F_TA);
        return misc;
    }

    public static MiscType createBABattleClaw() {
        MiscType misc = new MiscType();

        misc.name = "BA Manipulators [Battle Claw]";
        misc.setInternalName("BABattleClaw"); // This value MUST match the name
                                              // in
                                              // BattleArmor.MANIPULATOR_TYPE_STRINGS
        misc.shortName = "Battle Claw";
        misc.tonnage = 0.015;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BATTLE_CLAW).or(F_BA_MANIPULATOR);
        misc.bv = 1;
        misc.cost = 10000;
        misc.rulesRefs = "259, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_E, RATING_D, RATING_C)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3050, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2865, 2868, 3050, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CWF)
                .setProductionFactions(F_CWF);
        return misc;
    }

    public static MiscType createBAHeavyBattleClaw() {
        MiscType misc = new MiscType();

        misc.name = "BA Manipulators [Heavy Battle Claw]";
        misc.setInternalName("BAHeavyBattleClaw"); // This value MUST match the
                                                   // name in
                                                   // BattleArmor.MANIPULATOR_TYPE_STRINGS
        misc.shortName = "Heavy Battle Claw";
        misc.tonnage = 0.020;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BATTLE_CLAW).or(F_BA_MANIPULATOR);
        misc.bv = 0;
        misc.cost = 25000;
        misc.rulesRefs = "259, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_E, RATING_D, RATING_C)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3050, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2865, 2868, 3050, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CWF)
                .setProductionFactions(F_CWF);
        return misc;
    }

    public static MiscType createBACargoLifter() {
        MiscType misc = new MiscType();

        misc.name = "BA Manipulators [Cargo Lifter]";
        misc.setInternalName("BACargoLifter"); // This value MUST match the name
                                               // in
                                               // BattleArmor.MANIPULATOR_TYPE_STRINGS
        misc.shortName = "Cargo Lifter";
        misc.tonnage = 0.03;
        misc.criticals = 0;
        misc.cost = 250;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_CARGOLIFTER).or(F_BA_MANIPULATOR);
        misc.bv = 0;
        misc.cost = 500;
        misc.rulesRefs = "259, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_D, RATING_D, RATING_D, RATING_C)
                .setISAdvancement(DATE_ES, 2110, 2120, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, true, false, false)
                .setClanAdvancement(DATE_ES, 2110, 2120, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, true, false, false).setProductionFactions(F_TA);
        return misc;
    }

    public static MiscType createBAIndustrialDrill() {
        MiscType misc = new MiscType();

        misc.name = "BA Manipulators [Industrial Drill]";
        misc.setInternalName("BAIndustrialDrill");
        misc.shortName = "Industrial Drill";
        misc.tonnage = 0.030;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BA_MANIPULATOR);
        misc.bv = 0;
        misc.cost = 2500;
        misc.rulesRefs = "259, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_D, RATING_D, RATING_D, RATING_C)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setProductionFactions(F_TA);
        return misc;
    }

    public static MiscType createBASalvageArm() {
        MiscType misc = new MiscType();

        misc.name = "BA Manipulators [Salvage Arm]";
        misc.setInternalName("BASalvageArm");
        misc.shortName = "Salvage Arm";
        misc.tonnage = 0.030;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BA_MANIPULATOR);
        misc.bv = 0;
        misc.cost = 50000;
        misc.rulesRefs = "259, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_E, RATING_E, RATING_E, RATING_D)
                .setISAdvancement(2410, 2415, 2420, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2410, 2415, 2420, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createBABattleClawMagnets() {
        MiscType misc = new MiscType();

        misc.name = "BA Manipulator Adaptation [Magnetic Battle Claw]";
        misc.shortName = "Magnetic Claws";
        misc.setInternalName("BABattleClawMagnets"); // This value MUST match
                                                     // the name in
                                                     // BattleArmor.MANIPULATOR_TYPE_STRINGS
        misc.shortName = "Magnetic Battle Claw";
        misc.tonnage = 0.035;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_MAGNET_CLAW).or(F_BA_EQUIPMENT).or(F_BATTLE_CLAW).or(F_BA_MANIPULATOR);
        misc.bv = 1.5;
        misc.cost = 12500;
        misc.rulesRefs = "260, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(3053, 3055, 3058, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3058, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC);
        return misc;
    }

    public static MiscType createBAHeavyBattleClawMagnet() {
        MiscType misc = new MiscType();

        misc.name = "BA Manipulator Adaptation [Heavy Magnetic Battle Claw]";
        misc.setInternalName("BAHeavyBattleClawMagnets"); // This value MUST
                                                          // match the name in
                                                          // BattleArmor.MANIPULATOR_TYPE_STRINGS
        misc.shortName = "Heavy Magnetic Battle Claw";
        misc.addLookupName("Heavy Battle Claw (w/ Magnets)");
        misc.tonnage = 0.040;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_MAGNET_CLAW).or(F_BA_EQUIPMENT).or(F_BATTLE_CLAW).or(F_BA_MANIPULATOR);
        misc.bv = 1.5;
        misc.cost = 31250;
        misc.rulesRefs = "260, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(3053, 3055, 3058, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3058, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC);
        return misc;
    }

    public static MiscType createBABasicManipulatorMineClearance() {
        MiscType misc = new MiscType();

        misc.name = "BA Manipulator Adaptation [Mine Clearance Equipment]";
        misc.setInternalName("BABasicManipulatorMineClearance"); // This value
                                                                 // MUST match
                                                                 // the name in
                                                                 // BattleArmor.MANIPULATOR_TYPE_STRINGS
        misc.shortName = "Mine Clearance Equipment";
        misc.tonnage = 0.015;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_TOOLS).or(F_BASIC_MANIPULATOR).or(F_BA_MANIPULATOR);
        misc.subType |= S_MINESWEEPER;
        misc.bv = 0;
        misc.cost = 7500;
        misc.rulesRefs = "260, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3063, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC);
        return misc;
    }

    public static MiscType createBABattleClawVibro() {
        MiscType misc = new MiscType();

        misc.name = "BA Manipulator Adaptation [Vibro-Claw]";
        misc.setInternalName("BABattleClawVibro"); // This value MUST match the
                                                   // name in
                                                   // BattleArmor.MANIPULATOR_TYPE_STRINGS
        misc.shortName = "Vibro-Claw";
        misc.tonnage = 0.050;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_VIBROCLAW).or(F_BA_EQUIPMENT).or(F_BATTLE_CLAW).or(F_BA_MANIPULATOR);
        misc.bv = 1;
        misc.cost = 15000;
        misc.rulesRefs = "260, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3053, 3054, 3058, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3062, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return misc;
    }

    public static MiscType createBAHeavyBattleClawVibro() {
        MiscType misc = new MiscType();

        misc.name = "BA Manipulator Adaptation [Heavy Vibro-Claw]";
        misc.setInternalName("BAHeavyBattleClawVibro"); // This value MUST match
                                                        // the name in
                                                        // BattleArmor.MANIPULATOR_TYPE_STRINGS
        misc.addLookupName("Heavy Battle Claw (w/ Vibro-Claws)");
        misc.shortName = "Heavy Vibro-Claw";
        misc.tonnage = 0.060;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_VIBROCLAW).or(F_BA_EQUIPMENT).or(F_BATTLE_CLAW).or(F_BA_MANIPULATOR);
        misc.bv = 1;
        misc.cost = 30000;
        misc.rulesRefs = "260, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3053, 3054, 3058, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3062, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return misc;
    }

    public static MiscType createBAModularEquipmentAdaptor() {
        MiscType misc = new MiscType();

        misc.name = "BA Manipulator Adaptation [Modular Equipment Adaptor]";
        misc.setInternalName("BAMEA");
        misc.shortName = "Modular Equipment Adaptor";
        misc.tonnage = 0.01;
        misc.criticals = 2;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BA_MEA);
        misc.bv = 0;
        misc.cost = 10000;
        misc.rulesRefs = "260, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3052, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return misc;
    }

    public static MiscType createBAJumpJet() {
        MiscType misc = new MiscType();

        misc.name = "Jump Jet [BA]";
        misc.setInternalName(EquipmentTypeLookup.BA_JUMP_JET);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_JUMP_JET).or(F_BA_EQUIPMENT);
        misc.rulesRefs = "257, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_C).setAvailability(RATING_D, RATING_D, RATING_D, RATING_C)
                .setAdvancement(DATE_ES, DATE_ES, DATE_ES);
        return misc;
    }

    public static MiscType createBAVTOLEquipment() {
        MiscType misc = new MiscType();

        misc.name = "VTOL [BA]";
        misc.setInternalName(EquipmentTypeLookup.BA_VTOL);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BA_VTOL);
        misc.rulesRefs = "271, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3052, 3060, 3066).setClanApproximate(true, false, false)
                .setPrototypeFactions(F_CCC).setProductionFactions(F_CCC);
        return misc;
    }

    public static MiscType createBAUMU() {
        MiscType misc = new MiscType();

        misc.name = "UMU [BA]";
        misc.setInternalName(EquipmentTypeLookup.BA_UMU);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_UMU).or(F_BA_EQUIPMENT);
        misc.rulesRefs = "270, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_E, RATING_E, RATING_E)
                .setClanAdvancement(2840, 3059, 3065).setClanApproximate(true, false, false)
                .setPrototypeFactions(F_CGS).setProductionFactions(F_CGS).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createISBAJumpBooster() {
        MiscType misc = new MiscType();

        misc.name = "Jump Booster [BA]";
        misc.setInternalName(EquipmentTypeLookup.BA_JUMP_BOOSTER);
        misc.addLookupName("ISBAJumpBooster");
        misc.addLookupName("CLBAJumpBooster");
        misc.tonnage = 0.125;
        misc.criticals = 2;
        misc.cost = 75000;
        misc.flags = misc.flags.or(F_JUMP_BOOSTER).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "257, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3050, 3051, 3061, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, true, false, false)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3062, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_MERC).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createBAMagneticClamp() {
        MiscType misc = new MiscType();

        misc.name = "Magnetic Clamps [BA]";
        misc.setInternalName("BA-Magnetic Clamp");
        misc.addLookupName("Magnetic Clamp");
        misc.tonnage = .030;
        misc.criticals = 2;
        misc.cost = 2500;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_MAGNETIC_CLAMP).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT)
                .andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        String[] saModes = { "On", "Off" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(true);
        misc.bv = 1;
        misc.rulesRefs = "259, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3057, 3062, 3067, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC);
        return misc;
    }

    public static MiscType createISBAMechanicalJumpBooster() {
        MiscType misc = new MiscType();
        misc.name = "Mechanical Jump Booster";
        misc.setInternalName(EquipmentTypeLookup.BA_MECHANICAL_JUMP_BOOSTER);
        misc.addLookupName("ISMechanicalJumpBooster");
        misc.addLookupName("CLMechanicalJumpBooster");
        misc.shortName = "Jump Booster";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_MECHANICAL_JUMP_BOOSTER).or(F_BA_EQUIPMENT);
        misc.rulesRefs = "286, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(DATE_NONE, 3070, 3084, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setClanAdvancement(DATE_NONE, 3070, 3084, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createCLBAMyomerBooster() {
        MiscType misc = new MiscType();

        misc.name = "BA Myomer Booster";
        misc.setInternalName(EquipmentTypeLookup.BA_MYOMER_BOOSTER);
        misc.addLookupName("CLBAMB");
        misc.addLookupName("BAMyomerBooster");
        // Need variable tonnage because we have to account for tonnage being
        // split across 3 criticals, since it's spreadable equipment
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 3;
        misc.spreadable = true;
        misc.cost = COST_VARIABLE;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_MASC).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "287, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3072, 3085, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CIH)
                .setProductionFactions(F_CIH).setStaticTechLevel(SimpleTechLevel.STANDARD);
        
        return misc;
    }

    public static MiscType createBAPartialWing() {
        MiscType misc = new MiscType();

        misc.name = "Partial Wing [BA]";
        misc.setInternalName(EquipmentTypeLookup.BA_PARTIAL_WING);
        misc.tonnage = 0.2;
        misc.criticals = 1;
        misc.cost = 50000;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_PARTIAL_WING);
        misc.rulesRefs = "266, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3051, 3053, 3059, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return misc;
    }

    public static MiscType createISBADropChuteStd() {
        MiscType misc = new MiscType();
        // TODO: game rules
        misc.name = "BattleArmor DropChute (Standard)";
        misc.setInternalName("ISBADropChuteStd");
        misc.tonnage = 0.2;
        misc.criticals = 0;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BADC).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 1000;
        misc.rulesRefs = "348, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_X, RATING_E, RATING_B, RATING_B)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3051, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2874, 2875, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH);
        return misc;
    }

    public static MiscType createISBADropChuteCamo() {
        MiscType misc = new MiscType();
        // TODO: game rules
        misc.name = "BattleArmor DropChute(Camouflage)";
        misc.setInternalName("ISBADropChuteCamo");
        misc.tonnage = 0.2;
        misc.criticals = 0;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BADC).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 3000;
        misc.rulesRefs = "348, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_X, RATING_E, RATING_C, RATING_C)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3051, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2874, 2875, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH);
        return misc;
    }

    public static MiscType createISBADropChuteStealth() {
        MiscType misc = new MiscType();
        // TODO: game rules
        misc.name = "BattleArmor DropChute(Stealth)";
        misc.setInternalName("ISBADropChuteStealth");
        misc.tonnage = 0.2;
        misc.criticals = 0;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BADC).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 1000;
        misc.rulesRefs = "348, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_X, RATING_F, RATING_D, RATING_D)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3054, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2878, 2880, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH);
        return misc;
    }

    public static MiscType createISBADropChuteReuse() {
        MiscType misc = new MiscType();
        // TODO: game rules
        misc.name = "BattleArmor DropChute(Reuseable)";
        misc.setInternalName("ISBADropChuteReuse");
        misc.tonnage = 0.225;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BADC).or(F_REUSABLE).andNot(F_MECH_EQUIPMENT)
                .andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = COST_VARIABLE;
        misc.rulesRefs = "348, TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_E, RATING_B, RATING_B)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3053, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2874, 2876, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH);
        return misc;
    }

    // TODO - IO pg 52 - VTOL Equipment and UMU for BA should be equipment

    // Battle Armor Weapons - Most are in their own package but a couple are
    // equipment based.

    public static MiscType createBAAPMount() {
        MiscType misc = new MiscType();

        misc.name = "Anti Personnel Weapon Mount";
        misc.setInternalName("BAAPMount");
        misc.tonnage = 0.005;
        misc.criticals = 1;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_AP_MOUNT);
        misc.bv = 0;
        misc.rulesRefs = "271, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_ES, DATE_ES, 3050, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, 2868, 2870, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setProductionFactions(F_CWF);

        return misc;
    }
    // TODO - IO pg 52 - Battle Armor Detachable Missile Pack should really be a
    // piece of equipment.

    public static MiscType createISDetachableWeaponPack() {
        MiscType misc = new MiscType();

        misc.name = "Detachable Weapon Pack";
        misc.setInternalName("ISDetachableWeaponPack");
        misc.addLookupName("CLDetachableWeaponPack");
        misc.tonnage = 0;
        misc.criticals = 1;
        misc.cost = 18000;
        misc.rulesRefs = "287, TO";
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_DETACHABLE_WEAPON_PACK).andNot(F_MECH_EQUIPMENT)
                .andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "287, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setISAdvancement(DATE_NONE,  3073, 3080, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setClanAdvancement(DATE_NONE, 3072, 3080, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    // TODO - IO pg 52 - Squad Support Weapon Mod (SSW) should be a piece of
    // equipment

    public static MiscType createISBAMineDispenser() {
        MiscType misc = new MiscType();

        misc.name = "Mine Dispenser";
        misc.setInternalName("ISBAMineDispenser");
        misc.cost = 20000;
        misc.tonnage = 0.05;
        misc.criticals = 2;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_VEHICLE_MINE_DISPENSER).andNot(F_MECH_EQUIPMENT)
                .andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 8; // because it includes 2 mines
        misc.rulesRefs = "260, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3057, 3062, 3068, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC);
        return misc;
    }

    public static MiscType createBAMissionEquipStorage() {
        MiscType misc = new MiscType();
        // Not Covered in IO so using the old stats from TO.
        misc.name = "Mission Equipment Storage";
        misc.addLookupName("Mission Equipment Storage (20 kg)");
        misc.addLookupName("Mission Equipment Storage (5kg)");
        misc.addLookupName("Mission Equipment Storage (200 kg)");
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_VARIABLE_SIZE).or(F_BA_EQUIPMENT).or(F_BA_MISSION_EQUIPMENT)
                .andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 0;
        misc.rulesRefs = "262, TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setAdvancement(DATE_NONE, DATE_NONE, 2720)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C);
        return misc;
    }

    /*
     * // TODO Warrior Augmentations - IO pg 58. A lot of these are already in game
     * as SPA's. Should be reviewed and determine if they need a piece of equipment.
     * In infantry weapons are Prosthetic limbs with weapons as equipment.
     */

    // TODO Stuff below is largely Infantry Equipment that should be reviewed at
    // some point.

    public static MiscType createDemolitionCharge() {
        MiscType misc = new MiscType();

        misc.name = "Demolition Charge";
        misc.setInternalName(EquipmentTypeLookup.DEMOLITION_CHARGE);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_TOOLS).or(F_BA_EQUIPMENT);
        misc.subType |= S_DEMOLITION_CHARGE;
        misc.toHitModifier = 1;
        misc.bv = 0;
        misc.industrial = true;
        // Assuming this is a BA Carried Charge, So dates set for Nighthawk use.

        misc.techAdvancement.setTechBase(TECH_BASE_IS);
        misc.techAdvancement.setISAdvancement(DATE_NONE, 2720, DATE_NONE);
        misc.techAdvancement.setTechRating(RATING_D);
        misc.techAdvancement.setAvailability(new int[] { RATING_D, RATING_D, RATING_D, RATING_X });
        return misc;
    }

    public static MiscType createVibroShovel() {
        MiscType misc = new MiscType();

        misc.name = "Vibro-Shovel";
        misc.setInternalName(EquipmentTypeLookup.VIBRO_SHOVEL);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_TOOLS).or(F_BA_EQUIPMENT);
        misc.subType |= S_VIBROSHOVEL;
        misc.toHitModifier = 1;
        misc.bv = 0;
        misc.industrial = true;
        // Since this is BA Equipment I'm setting the date for Nighthawk use.

        misc.techAdvancement.setTechBase(TECH_BASE_IS);
        misc.techAdvancement.setISAdvancement(DATE_NONE, 2720, DATE_NONE);
        misc.techAdvancement.setTechRating(RATING_D);
        misc.techAdvancement.setAvailability(new int[] { RATING_D, RATING_D, RATING_D, RATING_X });
        return misc;
    }

    public static MiscType createBridgeKit() {
        MiscType misc = new MiscType();
        // TODO this is the equipment that bridging engineers use.
        // Likely needs to be split into a BA version and and Infantry version.
        misc.name = "Infantry Bridge Kit";
        misc.setInternalName(misc.name);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_TOOLS).or(F_BA_EQUIPMENT);
        misc.subType |= S_BRIDGE_KIT;
        misc.toHitModifier = 1;
        misc.bv = 0;
        misc.industrial = true;
        // Going to assume this is something with building Bridges
        // Also the equipment used by infantry bridge builders.
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setISAdvancement(DATE_NONE, 2720, DATE_NONE);
        misc.techAdvancement.setTechRating(RATING_D);
        misc.techAdvancement.setAvailability(new int[] { RATING_D, RATING_D, RATING_D, RATING_X });
        return misc;
    }

    public static MiscType createHeavyArmor() {
        MiscType misc = new MiscType();

        misc.name = "Heavy Armor";
        misc.setInternalName(misc.name);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 100000;
        misc.flags = misc.flags.or(F_TOOLS);
        misc.subType = S_HEAVY_ARMOR;
        misc.bv = 15;
        // Left over from older Infantry rules.

        misc.techAdvancement.setTechBase(TECH_BASE_IS);
        misc.techAdvancement.setUnofficial(true);
        misc.techAdvancement.setISAdvancement(DATE_NONE, 2100, DATE_NONE);
        misc.techAdvancement.setTechRating(RATING_D);
        misc.techAdvancement.setAvailability(new int[] { RATING_D, RATING_D, RATING_D, RATING_X });
        return misc;
    }

    public static MiscType createLightMinesweeper() {
        MiscType misc = new MiscType();

        misc.name = "Light Minesweeper";
        // TODO this is the equipment that bridging engineers use.
        // Likely needs to be split into a BA version and and Infantry version.
        misc.setInternalName("Light Minesweeper");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_TOOLS).or(F_BA_EQUIPMENT);
        misc.subType |= S_MINESWEEPER;
        misc.toHitModifier = 1;
        misc.bv = 0;
        // Since this is BA Equipment I'm setting the date for Nighthawk use.

        misc.techAdvancement.setTechBase(TECH_BASE_IS);
        misc.techAdvancement.setUnofficial(true);
        misc.techAdvancement.setISAdvancement(DATE_NONE, 2720, DATE_NONE);
        misc.techAdvancement.setTechRating(RATING_D);
        misc.techAdvancement.setAvailability(new int[] { RATING_D, RATING_D, RATING_D, RATING_X });
        return misc;
    }

    public static MiscType createLAMBombBay() {
        MiscType misc = new MiscType();
        misc.name = "Bomb Bay";
        misc.setInternalName(EquipmentTypeLookup.LAM_BOMB_BAY);
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_BOMB_BAY).or(F_MECH_EQUIPMENT);
        misc.explosive = true;
        misc.cost = 5000;
        misc.rulesRefs = "110, IO";
        // IO, p.220/221 (LAMs can be constructed in all later eras so Bomb Bays must not go extinct)
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2680, 2684)
                .setClanAdvancement(DATE_NONE, 2684)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
                .setTechRating(RATING_D).setAvailability(RATING_B, RATING_E, RATING_E, RATING_E)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createLAMAdditionalFuel() {
        MiscType misc = new MiscType();
        misc.name = "Fuel Tank";
        misc.setInternalName(EquipmentTypeLookup.LAM_FUEL_TANK);
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 200;
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT);
        misc.explosive = true; // Assumed. Game effects not implemented. Might follow rules for Bomb Bay Fuel, IO p.111
        misc.rulesRefs = "114, IO";
        // IO, p.220/221
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setAdvancement(DATE_ES, DATE_ES)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_A, RATING_A, RATING_A)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    private static MiscType createRamPlate() {
        MiscType misc = new MiscType();
        misc.name = "Ram Plate";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 3;
        misc.cost = COST_VARIABLE;
        misc.bv = BV_VARIABLE;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_RAM_PLATE).or(F_MECH_EQUIPMENT);
        misc.rulesRefs = "?";

        // Not yet published
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setAdvancement(2600, DATE_NONE, DATE_NONE, 2781, 3130)
                .setTechRating(RATING_E).setAvailability(RATING_F, RATING_X, RATING_X, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    /*
     * //TODO The following Primitive or Prototype equipment needs to be added at
     * some point Primitive Prototype K-F Boom DropShip Docking Collar (pre-Boom)
     * DropShip Docking Collar (post-Boom) DropShuttle Bays Space Station K-F
     * Adapter
     */

    @Override
    public String toString() {
        return "MiscType: " + name;
    }
}
