/**
 * * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur
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

/**
 * MiscType.java
 *
 * Created on April 2, 2002, 12:15 PM
 */

package megamek.common;

import java.math.BigInteger;

import megamek.common.verifier.TestEntity;
import megamek.common.weapons.ppc.CLERPPC;
import megamek.common.weapons.ppc.ISERPPC;
import megamek.common.weapons.ppc.ISHeavyPPC;
import megamek.common.weapons.ppc.ISLightPPC;
import megamek.common.weapons.ppc.ISPPC;
import megamek.common.weapons.ppc.ISSnubNosePPC;

/**
 * @author Ben
 * @version
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
    public static final BigInteger F_COOLANT_SYSTEM = BigInteger.valueOf(1).shiftLeft(31);
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
    public static final BigInteger F_DRONE_EXTRA = BigInteger.valueOf(1).shiftLeft(116);
    public static final BigInteger F_MASH_EXTRA = BigInteger.valueOf(1).shiftLeft(117);
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
    // Drone Equipment for Large Craft
    public static final BigInteger F_SRCS = BigInteger.valueOf(1).shiftLeft(212);
    public static final BigInteger F_SASRCS = BigInteger.valueOf(1).shiftLeft(213);
    public static final BigInteger F_CASPAR = BigInteger.valueOf(1).shiftLeft(214);
    public static final BigInteger F_CASPARII = BigInteger.valueOf(1).shiftLeft(215);
    public static final BigInteger F_ATAC = BigInteger.valueOf(1).shiftLeft(216);
    public static final BigInteger F_ARTS = BigInteger.valueOf(1).shiftLeft(217);
    public static final BigInteger F_DTAC = BigInteger.valueOf(1).shiftLeft(218);
    public static final BigInteger F_SDS_DESTRUCT = BigInteger.valueOf(1).shiftLeft(219);
    public static final BigInteger F_SDS_JAMMER = BigInteger.valueOf(1).shiftLeft(220);
    public static final BigInteger F_LF_STORAGE_BATTERY = BigInteger.valueOf(1).shiftLeft(199);
    

    // Secondary Flags for Physical Weapons
    public static final long S_CLUB = 1L << 0; // BMR
    public static final long S_TREE_CLUB = 1L << 1;// BMR
    public static final long S_HATCHET = 1L << 2; // BMR
    public static final long S_SWORD = 1L << 3; // BMR
    public static final long S_MACE_THB = 1L << 4;// Tac Handbook version
    public static final long S_CLAW_THB = 1L << 5; // Not used yet, but...
    // Hey, it's all for
    // fun.
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
    public static final long S_ROCK_CUTTER = 1L << 21;
    // TODO
    public static final long S_BUZZSAW = 1L << 22; // Unbound;
    public static final long S_RETRACTABLE_BLADE = 1L << 23;
    public static final long S_CHAIN_WHIP = 1L << 24;
    public static final long S_SPOT_WELDER = 1L << 25; // TODO: add game rules
    public static final long S_MINING_DRILL = 1L << 26; // Miniatures

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
        if (hasFlag(MiscType.F_CLUB) && (hasSubType(MiscType.S_SHIELD_LARGE) || hasSubType((MiscType.S_SHIELD_MEDIUM))
                || hasSubType(MiscType.S_SHIELD_SMALL))) {
            return true;
        }
        // else
        return false;
    }

    public boolean isVibroblade() {
        if (hasFlag(MiscType.F_CLUB) && (hasSubType(MiscType.S_VIBRO_LARGE) || hasSubType((MiscType.S_VIBRO_MEDIUM))
                || hasSubType(MiscType.S_VIBRO_SMALL))) {
            return true;
        }
        // else
        return false;
    }

    public boolean isIndustrial() {
        return industrial;
    }

    public double getDamageDivisor() {
        return damageDivisor;
    }

    @Override
    public double getTonnage(Entity entity, int location) {

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
            if (TechConstants.isClan(getTechLevel(entity.getTechLevelYear()))) {
                return Math.floor((entity.getWeight() * 0.05f) * 2.0f) / 2.0;
            } else {
                return Math.floor((entity.getWeight() * 0.07f) * 2.0f) / 2.0;
            }
        } else if (hasFlag(F_PARTIAL_WING) && hasFlag(F_PROTOMECH_EQUIPMENT)) {
            return Math.ceil((entity.getWeight() / 5.0f) * 2.0f) / 2.0;
        } else if (hasFlag(F_CLUB) && (hasSubType(S_HATCHET) || hasSubType(S_MACE_THB))) {
            return Math.ceil(entity.getWeight() / 15.0f);
        } else if (hasFlag(F_CLUB) && hasSubType(S_LANCE)) {
            return Math.ceil(entity.getWeight() / 20.0f);
        } else if (hasFlag(F_CLUB) && hasSubType(S_SWORD)) {
            return Math.ceil((entity.getWeight() / 20.0f) * 2.0f) / 2.0;
        } else if (hasFlag(F_CLUB) && hasSubType(S_MACE)) {
            return Math.ceil(entity.getWeight() / 10.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_RETRACTABLE_BLADE)) {
            return 0.5 + Math.ceil(entity.getWeight() / 10.0f) / 2.0;
        } else if (hasFlag(F_MASC)) {
            if (entity instanceof Protomech) {
                return entity.getWeight() * 0.025;
                // Myomer Boosters for BA
            } else if (entity instanceof BattleArmor) {
                // Myomer boosters weight 0.250 tons, however this has to
                // be split across 3 instances, since it's spreadable equipment
                return (0.250 / 3);
            } else {
                if (hasSubType(S_JETBOOSTER)) {
                    return entity.hasEngine() ? entity.getEngine().getWeightEngine(entity) / 10.0f : 0.0f;
                }
                if (hasSubType(S_SUPERCHARGER)) {
                    Engine e = entity.getEngine();
                    if (e == null) {
                        return 0.0f;
                    }
                    return Math.ceil((e.getWeightEngine(entity) / 10.0f) * 2.0f) / 2.0;
                }
                if (TechConstants.isClan(getTechLevel(entity.getTechLevelYear()))) {
                    return Math.round(entity.getWeight() / 25.0f);
                }
                return Math.round(entity.getWeight() / 20.0f);
            }
        } else if (hasFlag(F_QUAD_TURRET) || hasFlag(F_SHOULDER_TURRET) || hasFlag(F_HEAD_TURRET)) {
            int locationToCheck = location;
            if (hasFlag(F_HEAD_TURRET)) {
                locationToCheck = Mech.LOC_HEAD;
            }
            // 10% of linked weapons' weight
            double weaponWeight = 0;
            for (Mounted m : entity.getWeaponList()) {
                if ((m.getLocation() == locationToCheck) && m.isMechTurretMounted()) {
                    weaponWeight += m.getType().getTonnage(entity);
                }
            }
            // round to half ton
            weaponWeight /= 10;
            return Math.ceil(weaponWeight * 2.0f) / 2.0f;
        } else if (hasFlag(F_SPONSON_TURRET)) {
            double weaponWeight = 0;
            // 10% of linked weapons' weight
            for (Mounted m : entity.getWeaponList()) {
                if ((m.isSponsonTurretMounted()
                        && ((m.getLocation() == Tank.LOC_LEFT) || (m.getLocation() == Tank.LOC_RIGHT)))) {
                    weaponWeight += m.getType().getTonnage(entity);
                }
            }
            // round to half ton
            weaponWeight /= 10;
            return Math.ceil(weaponWeight * 2.0f) / 2.0f;

        } else if (hasFlag(F_PINTLE_TURRET)) {
            double weaponWeight = 0;
            // 5% of linked weapons' weight
            for (Mounted m : entity.getWeaponList()) {
                if (m.isPintleTurretMounted() && (m.getLocation() == location)) {
                    weaponWeight += m.getType().getTonnage(entity);
                }
            }

            TestEntity.Ceil roundWeight = TestEntity.Ceil.HALFTON;
            if (entity.isSupportVehicle() && (entity.getWeight() < 5)) {
                roundWeight = TestEntity.Ceil.KILO;
            }
            double weight = weaponWeight / 20;
            return TestEntity.ceil(weight, roundWeight);

        } else if (hasFlag(F_ARMORED_MOTIVE_SYSTEM)) {
            if (TechConstants.isClan(getTechLevel(entity.getTechLevelYear()))) {
                return Math.round((entity.getWeight() * 0.1f) * 2.0f) / 2.0f;
            } else {
                return Math.round((entity.getWeight() * 0.15f) * 2.0f) / 2.0f;
            }
        } else if (hasFlag(F_TARGCOMP)) {
            // based on tonnage of direct_fire weaponry
            double fTons = 0.0;
            for (Mounted m : entity.getWeaponList()) {
                WeaponType wt = (WeaponType) m.getType();
                if (wt.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    fTons += wt.getTonnage(entity);
                }
            }
            for (Mounted m : entity.getMisc()) {
                MiscType mt = (MiscType) m.getType();
                if (mt.hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    fTons += mt.getTonnage(entity);
                }
            }
            if (TechConstants.isClan(getTechLevel(entity.getTechLevelYear()))) {
                return Math.ceil(fTons / 5.0f);
            }
            return Math.ceil(fTons / 4.0f);
        } else if (hasFlag(MiscType.F_FERRO_FIBROUS) || hasFlag(MiscType.F_FERRO_FIBROUS_PROTO)) {
            double tons = 0.0;
            if (!entity.hasPatchworkArmor()) {
                if (entity.isClanArmor(1)) {
                    tons = entity.getTotalOArmor() / (16 * 1.2f);
                } else {
                    tons = entity.getTotalOArmor() / (16 * 1.12f);
                }
                tons = Math.ceil(tons * 2.0f) / 2.0;
            } else {
                // TODO
            }
            return tons;
        } else if (hasFlag(MiscType.F_LIGHT_FERRO)) {
            double tons = 0;
            if (!entity.hasPatchworkArmor()) {
                tons = entity.getTotalOArmor() / (16 * 1.06f);
                tons = Math.ceil(tons * 2.0f) / 2.0;
            } else {
                // TODO
            }
            return tons;
        } else if (hasFlag(MiscType.F_HEAVY_FERRO)) {
            double tons = 0;
            if (!entity.hasPatchworkArmor()) {
                tons = entity.getTotalOArmor() / (16 * 1.24f);
                tons = Math.ceil(tons * 2.0f) / 2.0;
            } else {
                // TODO
            }
            return tons;
        } else if (hasFlag(MiscType.F_FERRO_LAMELLOR)) {
            double tons = 0;
            if (!entity.hasPatchworkArmor()) {
                tons = entity.getTotalOArmor() / (16 * 0.875f);
                tons = Math.ceil(tons * 2.0f) / 2.0;
            } else {
                // TODO
            }
            return tons;
        } else if (hasFlag(F_ENDO_STEEL) || hasFlag(F_ENDO_STEEL_PROTO)) {
            double tons = 0;
            tons = Math.ceil(entity.getWeight() / 10.0f) / 2.0;
            return tons;
        } else if (hasFlag(F_ENDO_COMPOSITE)) {
            double tons = 0;
            tons = entity.getWeight() / 10.0;
            tons = Math.ceil(tons * 1.5f) / 2.0;
            return tons;
        } else if (hasFlag(MiscType.F_REINFORCED)) {
            double tons = 0;
            tons = Math.ceil(entity.getWeight() / 10.0f) * 2.0;
            return tons;
        } else if (hasFlag(MiscType.F_COMPOSITE)) {
            double tons = 0;
            tons = Math.ceil(entity.getWeight() / 10.0f) / 2.0;
            return tons;
        } else if (hasFlag(MiscType.F_INDUSTRIAL_STRUCTURE)) {
            double tons = 0;
            tons = Math.ceil(entity.getWeight() / 10.0f) * 2.0;
            return tons;
        } else if (hasFlag(F_VACUUM_PROTECTION)) {
            return Math.ceil(entity.getWeight() / 10.0);

        } else if (hasFlag(F_DUNE_BUGGY)) {
            return entity.getWeight() / 10.0f;

        } else if (hasFlag(F_ENVIRONMENTAL_SEALING)) {
            if ((entity instanceof SupportTank) || (entity instanceof LargeSupportTank)
                    || (entity instanceof FixedWingSupport) || (entity instanceof SupportVTOL)) {
                return 0;
            } else {
                return entity.getWeight() / 10.0;
            }

            // Per TO Pg 413 Mechanical Jump Boosters weight is 2 times jump
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
            return Math.ceil((entity.getWeight() * entity.getOriginalJumpMP()) / 10.0f) / 2.0;
        } else if ((hasFlag(F_HAND_WEAPON) && hasSubType(S_CLAW)) || hasFlag(F_TALON)) {
            return Math.ceil(entity.getWeight() / 15);
        } else if (hasFlag(F_ACTUATOR_ENHANCEMENT_SYSTEM)) {

            double tonnage = 0;
            if (entity instanceof BipedMech) {
                tonnage = entity.getWeight() / 35;
            } else {
                tonnage = entity.getWeight() / 50;
            }

            if (tonnage == Math.round(tonnage)) {
                return tonnage;
            }

            if (Math.floor(tonnage) < Math.round(tonnage)) {
                return Math.round(tonnage);
            }

            return Math.floor(tonnage) + 0.5;
        } else if (hasFlag(F_TRACKS)) {
            if (hasSubType(S_QUADVEE_WHEELS)) {
                // 15%, round up to the nearest half ton.
                return Math.ceil(entity.getWeight() * 0.3) / 2.0;
            } else {
                return entity.getWeight() * 0.1;
            }
        } else if (hasFlag(F_LIMITED_AMPHIBIOUS)) {
            return Math.ceil((entity.getWeight() / 25f) * 2) / 2.0;
        } else if (hasFlag(F_FULLY_AMPHIBIOUS)) {
            return Math.ceil((entity.getWeight() / 10f) * 2) / 2.0;
        } else if (hasFlag(F_DUMPER)) {
            // 5% of cargo
            double cargoTonnage = 0;
            for (Mounted mount : entity.getMisc()) {
                if (mount.getType().hasFlag(F_CARGO) && (mount.getLocation() == location)) {
                    cargoTonnage += mount.getType().getTonnage(entity);
                }
            }
            TestEntity.Ceil roundWeight = TestEntity.Ceil.HALFTON;
            if (entity.isSupportVehicle() && (entity.getWeight() < 5)) {
                roundWeight = TestEntity.Ceil.KILO;
            }
            double weight = cargoTonnage / 20f;
            return TestEntity.ceil(weight, roundWeight);

        } else if (hasFlag(F_BASIC_FIRECONTROL)) {
            // 5% of weapon weight
            double weaponWeight = 0;
            for (Mounted mount : entity.getWeaponList()) {
                weaponWeight += mount.getType().getTonnage(entity);
            }
            TestEntity.Ceil roundWeight = TestEntity.Ceil.HALFTON;
            if (entity.isSupportVehicle() && (entity.getWeight() < 5)) {
                roundWeight = TestEntity.Ceil.KILO;
            }
            double weight = weaponWeight / 20;
            return TestEntity.ceil(weight, roundWeight);

        } else if (hasFlag(F_ADVANCED_FIRECONTROL)) {
            // 10% of weapon weight
            double weaponWeight = 0;
            for (Mounted mount : entity.getWeaponList()) {
                weaponWeight += mount.getType().getTonnage(entity);
            }
            TestEntity.Ceil roundWeight = TestEntity.Ceil.HALFTON;
            if (entity.isSupportVehicle() && (entity.getWeight() < 5)) {
                roundWeight = TestEntity.Ceil.KILO;
            }
            return TestEntity.ceil(weaponWeight / 10f, roundWeight);
        } else if (hasFlag(F_BOOBY_TRAP)) {
            // 10% of unit weight
            double weight = entity.getWeight() / 10;
            TestEntity.Ceil roundWeight = TestEntity.Ceil.HALFTON;
            if (entity.isSupportVehicle() && (entity.getWeight() < 5)) {
                roundWeight = TestEntity.Ceil.KILO;
            }
            return TestEntity.ceil(weight, roundWeight);

        } else if (hasFlag(F_EJECTION_SEAT)) {
            if (entity.isSupportVehicle() && (entity.getWeight() < 5)) {
                return .1f;
            } else {
                return .5f;
            }
        } else if (hasFlag(F_DRONE_CARRIER_CONTROL)) {
            double weight = 2;
            for (Mounted mount : entity.getMisc()) {
                if (mount.getType().hasFlag(MiscType.F_DRONE_EXTRA)) {
                    weight += 0.5;
                }
            }
            return weight;
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
                    pct += hasFlag(F_SASRCS)? 0.01 : 0.02;
                } else if (getSubType() == S_ELITE) { // only shielded
                    pct += 0.03;
                }
                //Jumpship is based on non-drive weight and rounded to ton
                if (entity.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
                    return Math.ceil((entity.getWeight() - ((Jumpship)entity).getJumpDriveWeight()) * pct);
                }
                return Math.ceil(entity.getWeight() * pct * 2.0) / 2.0;
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
                return Math.ceil(entity.getWeight() * pct);
            } else {
                return Math.ceil((entity.getWeight() * pct) * 2.0) / 2.0;
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
                return Math.ceil(entity.getWeight() * pct);
            } else {
                return Math.ceil((entity.getWeight() * pct) * 2.0) / 2.0;
            }
        } else if (hasFlag(MiscType.F_ATAC)) {
            //TODO Neo - pg IO 146 Each drone that it can control adds 150 ton to weight.
            double tWeight = 0;
            tWeight = Math.ceil((entity.getWeight() * 0.02) * 2) / 2.0;
            if (tWeight >50000) {
                return 50000;
            } else {
                return Math.ceil((entity.getWeight() * 0.02) * 2) / 2.0;
            }
        } else if (hasFlag(MiscType.F_DTAC)) {
            //TODO Neo - pg IO 146 Each drone that it can control adds 150 ton to weight.
            return Math.ceil((entity.getWeight() * 0.03) * 2) / 2.0;
        } else if (hasFlag(MiscType.F_SDS_DESTRUCT)) {
            double tWeight = 0;
            tWeight = Math.ceil((entity.getWeight() * 0.1) * 2) / 2.0;
            if (tWeight >10000) {
                return 10000;
            } else {
                return Math.ceil((entity.getWeight() * 0.02) * 2) / 2.0;
            }
        } 
       // okay, I'm out of ideas
        return 1.0f;
    }

    @Override
    public double getCost(Entity entity, boolean isArmored, int loc) {
        double costValue = cost;
        if (costValue == EquipmentType.COST_VARIABLE) {
            if (hasFlag(F_DRONE_CARRIER_CONTROL)) {
                costValue = getTonnage(entity, loc) * 10000;
            } else if (hasFlag(F_FLOTATION_HULL) || hasFlag(F_VACUUM_PROTECTION) || hasFlag(F_ENVIRONMENTAL_SEALING)
                    || hasFlag(F_OFF_ROAD)) {
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
            } else if (hasFlag(F_JET_BOOSTER)) {
                costValue = (entity.hasEngine() ? entity.getEngine().getRating() * 10000 : 0);
            } else if (hasFlag(F_DRONE_OPERATING_SYSTEM)) {
                costValue = (getTonnage(entity, loc) * 10000) + 5000;
            } else if (hasFlag(MiscType.F_MASC)) {
                if (entity instanceof Protomech) {
                    costValue = Math.round((entity.hasEngine() ? entity.getEngine().getRating() : 0) * 1000
                            * entity.getWeight() * 0.025f);
                } else if (entity instanceof BattleArmor) {
                    costValue = entity.getOriginalWalkMP() * 75000;
                } else if (hasSubType(MiscType.S_SUPERCHARGER)) {
                    Engine e = entity.getEngine();
                    if (e == null) {
                        costValue = 0;
                    } else {
                        costValue = e.getRating() * 10000;
                    }
                } else {
                    int mascTonnage = 0;
                    if (getInternalName().equals("ISMASC")) {
                        mascTonnage = (int) Math.round(entity.getWeight() / 20.0f);
                    } else if (getInternalName().equals("CLMASC")) {
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
                        fTons += wt.getTonnage(entity);
                    }
                }

                for (Mounted mo : entity.getMisc()) {
                    MiscType mt = (MiscType) mo.getType();
                    if (mt.hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                        fTons += mt.getTonnage(entity);
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
            } else if (hasFlag(MiscType.F_CLUB)
                    && (hasSubType(MiscType.S_HATCHET) || hasSubType(MiscType.S_MACE_THB))) {
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
                costValue = (getTonnage(entity, loc) * 100000);
            //TODO NEO - ARTS see IO pg 188    
            } else if (hasFlag(MiscType.F_DTAC)) {
                costValue = (getTonnage(entity, loc) * 50000);
                             
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
            }

            if (isArmored) {
                double armoredCost = costValue;

                armoredCost += 150000 * getCriticals(entity);

                return armoredCost;
            }
        }
        return costValue;
    }

    @Override
    public int getCriticals(Entity entity) {
        if ((criticals != CRITICALS_VARIABLE) || (null == entity)) {
            return criticals;
        }
        // check for known formulas
        if (hasFlag(F_CLUB) && (hasSubType(S_HATCHET) || hasSubType(S_SWORD) || hasSubType(S_MACE_THB))) {
            return (int) Math.ceil(entity.getWeight() / 15.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_LANCE)) {
            return (int) Math.ceil(entity.getWeight() / 20.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_MACE)) {
            return (int) Math.ceil(entity.getWeight() / 10.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_RETRACTABLE_BLADE)) {
            return 1 + (int) Math.ceil(entity.getWeight() / 20.0);
        } else if (hasFlag(F_MASC)) {
            if (TechConstants.isClan(getTechLevel(entity.getTechLevelYear()))) {
                return (int) Math.round(entity.getWeight() / 25.0);
            }
            return (int) Math.round(entity.getWeight() / 20.0);

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
                    fTons += wt.getTonnage(entity);
                }
            }

            for (Mounted mo : entity.getMisc()) {
                MiscType mt = (MiscType) mo.getType();
                if (mt.hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    fTons += mt.getTonnage(entity);
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
            } else if ((entity instanceof BipedMech) || (entity instanceof QuadMech)) {
                return 7;
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
        }
        // right, well I'll just guess then
        return 1;
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
        if (hasFlag(F_CLUB) && hasSubType(S_HATCHET)) {
            returnBV = Math.ceil(entity.getWeight() / 5.0) * 1.5;
            if (entity.hasWorkingMisc(F_TSM)) {
                returnBV *= 2;
            }
        } else if (hasFlag(F_CLUB) && hasSubType(S_SWORD)) {
            returnBV = Math.ceil((entity.getWeight() / 10.0) + 1) * 1.725;
            if (entity.hasWorkingMisc(F_TSM)) {
                returnBV *= 2;
            }
        } else if (hasFlag(F_CLUB) && hasSubType(S_MACE_THB)) {
            returnBV = Math.ceil(entity.getWeight() / 5.0) * 1.5;
            if (entity.hasWorkingMisc(F_TSM)) {
                returnBV *= 2;
            }
        } else if (hasFlag(F_CLUB) && hasSubType(S_LANCE)) {
            returnBV = Math.ceil(entity.getWeight() / 5.0) * 1.0;
            if (entity.hasWorkingMisc(F_TSM)) {
                returnBV *= 2;
            }
        } else if (hasFlag(F_CLUB) && hasSubType(S_MACE)) {
            returnBV = Math.ceil(entity.getWeight() / 4.0);
            if (entity.hasWorkingMisc(F_TSM)) {
                returnBV *= 2;
            }
        } else if (hasFlag(F_CLUB) && hasSubType(S_RETRACTABLE_BLADE)) {
            returnBV = Math.ceil((entity.getWeight() / 10.0) * 1.725);
            if (entity.hasWorkingMisc(F_TSM)) {
                returnBV *= 2;
            }
        } else if (hasFlag(F_HAND_WEAPON) && hasSubType(S_CLAW)) {
            returnBV = (Math.ceil(entity.getWeight() / 7.0)) * 1.275;
        } else if (hasFlag(F_TALON)) {
            // according to an email from TPTB, Talon BV is the extra damage
            // they
            // do for kicks, so 50% of normal kick damage
            returnBV = Math.round(Math.floor(entity.getWeight() / 5.0) * 0.5);
            if (entity.hasWorkingMisc(MiscType.F_TSM)) {
                returnBV *= 2;
            }
        }

        return returnBV;
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
        EquipmentType.addType(MiscType.createVacuumProtection());
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
        EquipmentType.addType(MiscType.createEnvironmentalSealing());
        EquipmentType.addType(MiscType.createFieldKitchen());

        EquipmentType.addType(MiscType.createImprovedJumpJet());
        EquipmentType.addType(MiscType.createVehicluarJumpJet());
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
        EquipmentType.addType(MiscType.createCLSuperCharger());
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
        EquipmentType.addType(MiscType.createCommsGear1());
        EquipmentType.addType(MiscType.createCommsGear2());
        EquipmentType.addType(MiscType.createCommsGear3());
        EquipmentType.addType(MiscType.createCommsGear4());
        EquipmentType.addType(MiscType.createCommsGear5());
        EquipmentType.addType(MiscType.createCommsGear6());
        EquipmentType.addType(MiscType.createCommsGear7());
        EquipmentType.addType(MiscType.createCommsGear8());
        EquipmentType.addType(MiscType.createCommsGear9());
        EquipmentType.addType(MiscType.createCommsGear10());
        EquipmentType.addType(MiscType.createCommsGear11());
        EquipmentType.addType(MiscType.createCommsGear12());
        EquipmentType.addType(MiscType.createCommsGear13());
        EquipmentType.addType(MiscType.createCommsGear14());
        EquipmentType.addType(MiscType.createCommsGear15());
        EquipmentType.addType(MiscType.createISGroundMobileHPG());
        EquipmentType.addType(MiscType.createISMobileHPG());
        EquipmentType.addType(MiscType.createISPartialWing());
        EquipmentType.addType(MiscType.createCLPartialWing());
        EquipmentType.addType(MiscType.createCargo1());
        EquipmentType.addType(MiscType.createHalfCargo());
        EquipmentType.addType(MiscType.createCargo15());
        EquipmentType.addType(MiscType.createCargo2());
        EquipmentType.addType(MiscType.createCargo25());
        EquipmentType.addType(MiscType.createCargo3());
        EquipmentType.addType(MiscType.createCargo35());
        EquipmentType.addType(MiscType.createCargo4());
        EquipmentType.addType(MiscType.createCargo45());
        EquipmentType.addType(MiscType.createCargo5());
        EquipmentType.addType(MiscType.createCargo55());
        EquipmentType.addType(MiscType.createCargo6());
        EquipmentType.addType(MiscType.createCargo65());
        EquipmentType.addType(MiscType.createCargo7());
        EquipmentType.addType(MiscType.createCargo75());
        EquipmentType.addType(MiscType.createCargo8());
        EquipmentType.addType(MiscType.createCargo85());
        EquipmentType.addType(MiscType.createCargo9());
        EquipmentType.addType(MiscType.createCargo95());
        EquipmentType.addType(MiscType.createCargo10());
        EquipmentType.addType(MiscType.createCargo105());
        EquipmentType.addType(MiscType.createCargo11());
        EquipmentType.addType(MiscType.createLiquidCargo1());
        EquipmentType.addType(MiscType.createHalfLiquidCargo());
        EquipmentType.addType(MiscType.createCargoContainer());
        EquipmentType.addType(MiscType.createMechSprayer());
        EquipmentType.addType(MiscType.createTankSprayer());
        EquipmentType.addType(MiscType.createFrontDumper());
        EquipmentType.addType(MiscType.createRearDumper());
        EquipmentType.addType(MiscType.createLeftDumper());
        EquipmentType.addType(MiscType.createRightDumper());
        EquipmentType.addType(MiscType.createMASH());
        EquipmentType.addType(MiscType.createMASHExtraTheater());
        EquipmentType.addType(MiscType.createParamedicEquipment());
        EquipmentType.addType(MiscType.createISMastMount());
        EquipmentType.addType(MiscType.createFuel1());
        EquipmentType.addType(MiscType.createFuelHalf());
        EquipmentType.addType(MiscType.createFuel2());
        EquipmentType.addType(MiscType.createFuel25());
        EquipmentType.addType(MiscType.createFuel3());
        EquipmentType.addType(MiscType.createFuel35());
        EquipmentType.addType(MiscType.createFuel4());
        EquipmentType.addType(MiscType.createBlueShield());
        EquipmentType.addType(MiscType.createEndoComposite());
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
        EquipmentType.addType(MiscType.createISBAAdvancedArmor());
        EquipmentType.addType(MiscType.createISBAStandardPrototypeArmor());
        EquipmentType.addType(MiscType.createISBAFireResistantArmor());
        EquipmentType.addType(MiscType.createISBAReactiveArmor());
        EquipmentType.addType(MiscType.createISBAReflectiveArmor());
        EquipmentType.addType(MiscType.createISBAStealthPrototype());
        EquipmentType.addType(MiscType.createISBABasicStealth());
        EquipmentType.addType(MiscType.createISBAStandardStealth());
        EquipmentType.addType(MiscType.createISBAImprovedStealth());
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
        EquipmentType.addType(MiscType.createEnvironmentalSealedChassis());
        EquipmentType.addType(MiscType.createHydroFoilChassisModification());
        EquipmentType.addType(MiscType.createMonocycleModification());
        EquipmentType.addType(MiscType.createISOffRoadChassis());
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
        EquipmentType.addType(MiscType.create20mLadder());
        EquipmentType.addType(MiscType.create40mLadder());
        EquipmentType.addType(MiscType.create60mLadder());
        EquipmentType.addType(MiscType.create80mLadder());
        EquipmentType.addType(MiscType.create100mLadder());
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
        EquipmentType.addType(MiscType.createProtoQuadMeleeSystem());

        // Drone and Robotic Systems
        EquipmentType.addType(MiscType.createISRemoteDroneCommandConsole());
        EquipmentType.addType(MiscType.createSmartRoboticControlSystem());
        EquipmentType.addType(MiscType.createImprovedSmartRoboticControlSystem());
        EquipmentType.addType(MiscType.createISDroneCarrierControlSystem());
        EquipmentType.addType(MiscType.createISDroneExtra());
        EquipmentType.addType(MiscType.createISDroneOperatingSystem());
        EquipmentType.addType(MiscType.createShieldedAeroSRCS());
        EquipmentType.addType(MiscType.createImprovedShieldedAeroSRCS());
        EquipmentType.addType(MiscType.createEliteShieldedAeroSRCS());
        EquipmentType.addType(MiscType.createCasparDroneControlSystem());
        EquipmentType.addType(MiscType.createImprovedCasparDroneControlSystem());
        EquipmentType.addType(MiscType.createCasparIIDroneControlSystem());
        EquipmentType.addType(MiscType.createImprovedCasparIIDroneControlSystem());
        EquipmentType.addType(MiscType.createAutoTacticalAnalysisComputer());
        EquipmentType.addType(MiscType.createAdvRoboticTransportSystem());
        EquipmentType.addType(MiscType.createDirectTacticalAnalysisSystem());
        EquipmentType.addType(MiscType.createSDSSelfDestructSystem());
        EquipmentType.addType(MiscType.createSDSJammerSystem());

        // Large Craft Systems
        EquipmentType.addType(MiscType.createPCMT());
        EquipmentType.addType(MiscType.createLithiumFusionBattery());
        EquipmentType.addType(MiscType.createLightSail());
        EquipmentType.addType(MiscType.createEnergyStorageBattery());
        EquipmentType.addType(MiscType.createImpFerroAluminumArmor());
        EquipmentType.addType(MiscType.createPrimitiveLCAerospaceArmor());
        EquipmentType.addType(MiscType.createISAeroSpaceArmor());
        EquipmentType.addType(MiscType.createClanAeroSpaceArmor());
        EquipmentType.addType(MiscType.createLCFerroCarbideArmor());
        EquipmentType.addType(MiscType.createLCLamellorFerroCarbideArmor());

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

    /*
     * A note about Tech Progression from Ray. Received via PM after IO Release If
     * there's no info specifying proto/prod/common in the NOTES section, assume it
     * means Production/Advanced, with no definite Common/Tournament-legal date.
     * Based on this Some equipment will start at Advanced and not have common date.
     * If its Clan and it just say Intro assume it starts at Tourney Legal (Level 2)
     */

    // Advanced Mech/ProtoMech/Vehicular Motive Systems
    public static MiscType createJumpJet() {
        MiscType misc = new MiscType();

        misc.name = "Jump Jet";
        misc.setInternalName(misc.name);
        misc.addLookupName("JumpJet");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_JUMP_JET).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT);
        misc.subType |= S_STANDARD;
        misc.bv = 0;
        misc.rulesRefs = "225,TM";
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
        misc.setInternalName("IS Improved Jump Jet");
        misc.addLookupName("ISImprovedJump Jet");
        misc.addLookupName("ImprovedJump Jet");
        misc.addLookupName("Improved Jump Jet");
        misc.addLookupName("Clan Improved Jump Jet");
        misc.addLookupName("CLImprovedJump Jet");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 2;
        misc.flags = misc.flags.or(F_JUMP_JET).or(F_MECH_EQUIPMENT);
        misc.subType |= S_IMPROVED;
        misc.bv = 0;
        misc.rulesRefs = "225,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(DATE_NONE, 3070, 3071, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(3060, 3069, 3071, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CWX)
                .setProductionFactions(F_CWX, F_CWF, F_LC).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D);
        return misc;
    }

    public static MiscType createISPrototypeImprovedJumpJet() {
        MiscType misc = new MiscType();

        misc.name = "Prototype Improved Jump Jet";
        misc.setInternalName("ISPrototypeImprovedJumpJet");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.tankslots = 0;
        misc.explosive = true;
        misc.flags = misc.flags.or(F_JUMP_JET).or(F_MECH_EQUIPMENT);
        misc.subType |= S_PROTOTYPE | S_IMPROVED;
        misc.bv = 0;
        // Not included in IO Progression data based on original source.
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3020, DATE_NONE, DATE_NONE, 3069)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_F, RATING_F, RATING_X);
        return misc;
    }

    public static MiscType createISPrototypeJumpJet() {
        MiscType misc = new MiscType();

        misc.name = "Primitive Prototype Jump Jet";
        misc.setInternalName("ISPrototypeJumpJet");
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
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X);
        return misc;
    }

    public static MiscType createVehicluarJumpJet() {
        MiscType misc = new MiscType();

        misc.name = "Jump Jet";
        misc.setInternalName("VehicleJumpJet");
        misc.addLookupName("VJJ");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.tankslots = 1;
        misc.flags = misc.flags.or(F_JUMP_JET).or(F_TANK_EQUIPMENT);
        misc.subType |= S_STANDARD;
        misc.bv = 0;
        misc.rulesRefs = "348,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2650,3083)
                .setApproximate(false, true).setPrototypeFactions(F_TH)
                .setProductionFactions(F_CHH).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_X, RATING_F, RATING_E)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    // TODO Protomech Jump Jets See IO, pg 35
    
    public static MiscType createProtomechJumpJet() {
        MiscType misc = new MiscType();
        misc.name = "Jump Jet";
        misc.setInternalName("ProtomechJumpJet");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_JUMP_JET).or(F_PROTOMECH_EQUIPMENT);
        misc.subType |= S_STANDARD;
        misc.bv = 0;
        misc.rulesRefs = "225,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3055,3060,3060)
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
        misc.setInternalName("ExtendedJumpJetSystem");
        misc.addLookupName("XJJ");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_JUMP_JET).or(F_PROTOMECH_EQUIPMENT);
        misc.subType |= S_IMPROVED;
        misc.bv = 0;
        misc.rulesRefs = "65,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3071, 3075, 3085, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSR)
                .setProductionFactions(F_CSR).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createProtomechUMU() {
        MiscType misc = new MiscType();
        // TODO Game Rules.
        misc.name = "UMU";
        misc.setInternalName("ProtomechUMU");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_UMU).or(F_PROTOMECH_EQUIPMENT);
        misc.subType |= S_STANDARD;
        misc.bv = 0;
        misc.rulesRefs = "101,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3065, 3075, 3084)
                .setClanApproximate(true, true, false).setPrototypeFactions(F_CBS)
                .setProductionFactions(F_CBS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D);
        return misc;
    }

    public static MiscType createCLProtoMyomerBooster() {
        MiscType misc = new MiscType();

        misc.name = "Protomech Myomer Booster";
        misc.setInternalName("CLMyomerBooster");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_MASC).or(F_PROTOMECH_EQUIPMENT);
        misc.rulesRefs = "232,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3066, 3068, 3075, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CBS, F_CIH)
                .setProductionFactions(F_CBS, F_CIH).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);
        return misc;
    }

    // TODO Jump Pack / Mech Drop Pack see IO pg 35

    public static MiscType createISMASC() {
        MiscType misc = new MiscType();

        misc.name = "MASC";
        misc.setInternalName("ISMASC");
        misc.addLookupName("IS MASC");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_MASC).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        String[] saModes = { "Armed", "Off" };
        misc.setModes(saModes);
        misc.rulesRefs = "225,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(2730, 2740, 3040, 2795, 3035)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_CC).setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_E, RATING_D);
        return misc;
    }

    public static MiscType createCLMASC() {
        MiscType misc = new MiscType();

        misc.name = "MASC";
        misc.setInternalName("CLMASC");
        misc.addLookupName("Clan MASC");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_MASC).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.omniFixedOnly = true;
        String[] saModes = { "Armed", "Off" };
        misc.setModes(saModes);

        misc.rulesRefs = "225,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(2820, 2827, 2835, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CIH)
                .setProductionFactions(F_CIH).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_F, RATING_E, RATING_D);
        return misc;
    }

    public static MiscType createJumpBooster() {
        MiscType misc = new MiscType();

        misc.name = "Mech Mechanical Jump Boosters";
        misc.setInternalName(misc.name);
        misc.addLookupName("Jump Booster");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_JUMP_BOOSTER).or(F_MECH_EQUIPMENT);
        misc.spreadable = true;

        misc.rulesRefs = "292,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3060, 3083, 3090)
                .setISApproximate(true, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS, F_LC).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createISPartialWing() {
        MiscType misc = new MiscType();

        misc.name = "Partial Wing";
        misc.setInternalName("ISPartialWing");
        misc.addLookupName("IS Partial Wing");
        misc.addLookupName("PartialWing");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 8;
        misc.spreadable = true;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_PARTIAL_WING).or(F_MECH_EQUIPMENT);
        misc.rulesRefs = "292,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3074, DATE_NONE, 3090, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_MERC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return misc;
    }

    public static MiscType createCLPartialWing() {
        MiscType misc = new MiscType();

        misc.name = "Partial Wing (Clan)";
        misc.setInternalName("CLPartialWing");
        misc.addLookupName("Clan Partial Wing");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 6;
        misc.spreadable = true;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_PARTIAL_WING).or(F_MECH_EQUIPMENT);
        misc.rulesRefs = "292,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(3067, 3085, 3090, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CJF)
                .setProductionFactions(F_CJF).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return misc;
    }

    public static MiscType createProtoPartialWing() {
        MiscType misc = new MiscType();
        misc.name = "Protomech Partial Wing";
        misc.setInternalName("ProtoMechPartialWing");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_PROTOMECH_EQUIPMENT).or(F_PARTIAL_WING);

        misc.rulesRefs = "292,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(3070, 3085, 3090, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CBS)
                .setProductionFactions(F_CSR).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return misc;
    }

    public static MiscType createISUMU() {
        MiscType misc = new MiscType();
        misc.name = "UMU";
        misc.setInternalName("UMU");
        misc.addLookupName("ISUMU");
        misc.addLookupName("CLUMU");
        misc.addLookupName("IS Underwater Maneuvering Unit");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_UMU).or(F_MECH_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "292,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
            .setISAdvancement(3061, 3066, 3084, DATE_NONE, DATE_NONE)
            .setClanAdvancement(DATE_NONE, 3072, 3084, DATE_NONE, DATE_NONE)
            .setPrototypeFactions(F_CGS)
            .setProductionFactions(F_LC, F_CWX).setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return misc;
    }

    public static MiscType createISVTOLJetBooster() {
        MiscType misc = new MiscType();
        misc.name = "VTOL Jet Booster";
        misc.setInternalName("ISVTOLJetBooster");
        misc.addLookupName("CLVTOLJetBooster");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_JET_BOOSTER).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT).or(F_MASC);
        misc.subType |= S_JETBOOSTER;

        misc.rulesRefs = "350,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
            .setISAdvancement(3009, 3078, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(false, true, false, false, false)
            .setClanAdvancement(2839, 3078, DATE_NONE, DATE_NONE, DATE_NONE)
            .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CHH, F_FS)
            .setProductionFactions(F_FS).setTechRating(RATING_D)
            .setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_MASC).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT);
        misc.subType |= S_SUPERCHARGER;
        misc.bv = 0;
        String[] saModes = { "Armed", "Off" };
        misc.setModes(saModes);
        misc.rulesRefs = "345,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_F, RATING_F, RATING_D)
                .setISAdvancement(DATE_ES, 3078, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return misc;
    }

    public static MiscType createCLSuperCharger() {
        MiscType misc = new MiscType();

        misc.name = "Supercharger (Clan)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CLSuperCharger");
        misc.addLookupName("CL SuperCharger");
        misc.addLookupName("CL Super Charger");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_MASC).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT);
        misc.subType |= S_SUPERCHARGER;
        misc.bv = 0;
        String[] saModes = { "Armed", "Off" };
        misc.setModes(saModes);
        misc.rulesRefs = "345,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_C)
            .setAvailability(RATING_F, RATING_F, RATING_F, RATING_D)
            .setClanAdvancement(DATE_ES, 3078)
            .setClanApproximate(false, true, false, false, false)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return misc;
    }

    public static MiscType createTracks() {
        MiscType misc = new MiscType();

        misc.name = "Tracks";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.spreadable = true;
        misc.bv = 0;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_TRACKS).or(F_MECH_EQUIPMENT);

        misc.rulesRefs = "249,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_C)
                .setAvailability(RATING_D, RATING_E, RATING_E, RATING_D)
                .setISAdvancement(2430, 2440, 2500, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2430, 2440, 2500, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);

        return misc;
    }

    public static MiscType createQVWheels() {
        MiscType misc = new MiscType();

        misc.name = "QuadVee Wheels";
        misc.setInternalName(misc.name);
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
        misc.rulesRefs = "133,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
                .setClanAdvancement(3130, 3135, DATE_NONE, DATE_NONE, DATE_NONE).setClanApproximate(true)
                .setPrototypeFactions(F_CHH).setProductionFactions(F_CHH);
        return misc;
    }

    // Armor (Mech/Vehicle/Fighter)

    public static MiscType createISCommercialArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_COMMERCIAL);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_COMMERCIAL, false));
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.industrial = true;
        misc.flags = misc.flags.or(F_COMMERCIAL_ARMOR).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "205,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_B)
                .setAvailability(RATING_B, RATING_B, RATING_A, RATING_A)
                .setISAdvancement(2290, 2300, 2310, DATE_NONE, DATE_NONE)
                .setISApproximate(true, true, false, false, false).setPrototypeFactions(F_TA)
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
        misc.rulesRefs = "125,IO";
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
        misc.rulesRefs = "125,IO";
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
        misc.addLookupName("Clan Industrial Armor");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.industrial = true;
        misc.flags = misc.flags.or(F_INDUSTRIAL_ARMOR).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "205,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_B, RATING_C, RATING_B, RATING_B)
                .setISAdvancement(2430, 2439, 2439, DATE_NONE, DATE_NONE)
                .setISApproximate(true, true, true, false, false).setPrototypeFactions(F_TH)
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
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.criticals = 0;

        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setIntroLevel(true);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_X });
        return misc;
    }

    public static MiscType createISHeavyIndustrialArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL, false));
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.industrial = true;
        misc.flags = misc.flags.or(F_HEAVY_INDUSTRIAL_ARMOR).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "205,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_D)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_B)
                .setISAdvancement(2460, 2470, 2470, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(2460, 2470, 2470, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_TH)
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
                .or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "86,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                .setISAdvancement(3105, 3114, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
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
                .or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "87,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                .setISAdvancement(3120, 3131, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
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
        misc.rulesRefs = "72,IO";
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
        misc.rulesRefs = "205,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_D, RATING_C).setISAdvancement(2557, 2571, 3055, 2810, 3040)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
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
        misc.rulesRefs = "72,IO";
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
        misc.rulesRefs = "205,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_D, RATING_C).setISAdvancement(2557, 2571, 3055, 2810, 3040)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
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
        misc.rulesRefs = "205,TM";
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
        misc.rulesRefs = "205,TM";
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
        misc.rulesRefs = "205,TM";
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
        misc.rulesRefs = "205,TM";
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
        misc.rulesRefs = "205,TM";
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
        misc.rulesRefs = "205,TM";
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
        misc.rulesRefs = "279,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3070, 3109, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CSR)
                .setProductionFactions(F_CSR).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.rulesRefs = "280,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3047, 3081, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(3061, 3081, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_FS, F_LC, F_CGB)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.rulesRefs = "87,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                .setISAdvancement(3111, 3123, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3126, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC);
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
        misc.rulesRefs = "87,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                .setISAdvancement(3092, 3103, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC);
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
        misc.flags = misc.flags.or(F_REFLECTIVE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "280,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3058, 3080, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.flags = misc.flags.or(F_REFLECTIVE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "280,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3061, 3080, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CJF)
                .setProductionFactions(F_CJF).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

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
        misc.flags = misc.flags.or(F_MODULAR_ARMOR).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        misc.bv = BV_VARIABLE;
        misc.damageTaken = 0;
        misc.baseDamageAbsorptionRate = 10;
        misc.baseDamageCapacity = 10;
        misc.rulesRefs = "281,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3072, 3096, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(3074, 3096, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CS, F_CWX)
                .setProductionFactions(F_RS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.flags = misc.flags.or(F_REACTIVE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "282,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3063, 3081, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.flags = misc.flags.or(F_REACTIVE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "282,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3065, 3081, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CGB)
                .setProductionFactions(F_CGB).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.rulesRefs = "206,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3051, 3063, 3072, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
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
        misc.flags = misc.flags.or(F_STEALTH).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_VTOL_EQUIPMENT);
        misc.omniFixedOnly = true;
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(false);
        misc.bv = 0;
        misc.rulesRefs = "282,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3067, 3084, 3145, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.rulesRefs = "125,IO";
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
        misc.rulesRefs = "205,TM";
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
        misc.rulesRefs = "205,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_D)
            .setAvailability(RATING_C, RATING_C, RATING_C, RATING_B)
            .setClanAdvancement(DATE_NONE, DATE_NONE, 2470)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }
    
    public static MiscType createImpFerroAluminumArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_IMP);
        misc.setInternalName(misc.name);
        misc.addLookupName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_IMP, false));
        misc.addLookupName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_FERRO_IMP, true));
        misc.addLookupName("ImprovedFerroAluminum");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_CAPITAL_ARMOR).or(F_IMP_FERRO).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "152,SO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(2500, 2520, DATE_NONE, 2950, 3052).setISApproximate(true, true, false, false, false)
                .setClanAdvancement(2500, 2520, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }
    
    public static MiscType createLCFerroCarbideArmor() {
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
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(2550, 2570, DATE_NONE, 2950, 3055).setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2550, 2570, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC, F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }
    
    public static MiscType createLCLamellorFerroCarbideArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE, true));
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = true;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "152,SO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_F)
                .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(2600, 2615, DATE_NONE, 2950, 3055).setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2600, 2615, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_FW, F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return misc;
    }
    
    
    

    // Armor (ProtoMech)

    // TODO Protomech Armor IO pg 36

    public static MiscType createElectricDischargeArmor() {
        // TODO: add game rules for this
        MiscType misc = new MiscType();
        misc.name = "Protomech Electric Discharge Armor";
        misc.setInternalName("CLEDPArmor");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_PROTOMECH_EQUIPMENT).or(F_ELECTRIC_DISCHARGE_ARMOR);
        misc.omniFixedOnly = true;
        misc.bv = 32;
        String[] modes = { "not charging", "charging" };
        misc.setModes(modes);
        misc.rulesRefs = "64,IO";
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
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STANDARD));
        misc.addLookupName("IS BA Standard (Basic)");
        misc.addLookupName("Clan BA Standard (Basic)");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "252,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2680, DATE_NONE, 3054, 2800, 3050)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2680, 2868, 3054).setClanApproximate(true, false, false)
                .setPrototypeFactions(F_TH).setProductionFactions(F_CWF)
                .setReintroductionFactions(F_FS, F_LC, F_DC).setTechRating(RATING_E)
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
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setISAdvancement(3050, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH, F_FS, F_LC, F_DC)
                .setProductionFactions(F_TH, F_FS, F_LC, F_DC).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_E, RATING_D);

        return misc;
    }

    public static MiscType createISBAAdvancedArmor() {
        MiscType misc = new MiscType();

        misc.name = BattleArmor.ADVANCED_ARMOR;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STANDARD_ADVANCED));
        misc.addLookupName("IS BA Advanced");
        misc.addLookupName("Clan BA Advanced");
        misc.tonnage = 0;
        misc.criticals = 5;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "252,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(DATE_NONE, 3057, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setProductionFactions(F_FW).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);

        return misc;
    }

    public static MiscType createISBAFireResistantArmor() {
        MiscType misc = new MiscType();

        misc.name = BattleArmor.FIRE_RESISTANT;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_FIRE_RESIST));
        // misc.addLookupName("IS BA Fire Resistant");
        misc.addLookupName("Clan BA Fire Resistant");
        misc.tonnage = 0;
        misc.criticals = 5;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_FIRE_RESISTANT).or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "253,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(3052, 3058, 3065, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CFM)
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
        misc.rulesRefs = "252,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3050, 3052, 3054, 3055, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X);

        return misc;
    }

    public static MiscType createISBABasicStealth() {
        MiscType misc = new MiscType();

        misc.name = BattleArmor.BASIC_STEALTH_ARMOR;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH_BASIC));
        misc.addLookupName("IS BA Stealth (Basic)");
        misc.addLookupName("Clan BA Stealth (Basic)");
        misc.tonnage = 0;
        misc.criticals = 3;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "252,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(2700, 2710, 3054, 2770, 3052)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2700, 2710, 3054, DATE_NONE, 3052)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_E, RATING_D);

        return misc;
    }

    public static MiscType createISBAStandardStealth() {
        MiscType misc = new MiscType();

        misc.name = BattleArmor.STANDARD_STEALTH_ARMOR;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH));
        misc.addLookupName("IS BA Stealth (Standard)");
        misc.addLookupName("Clan BA Stealth (Standard)");
        misc.addLookupName("Clan BA Stealth");
        misc.addLookupName("IS BA Stealth");
        misc.tonnage = 0;
        misc.criticals = 4;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "252,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(2710, 2720, 3055, 2770, 3053)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2710, 2720, 3055, DATE_NONE, 3053)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_X, RATING_E, RATING_D);

        return misc;
    }

    public static MiscType createISBAImprovedStealth() {
        MiscType misc = new MiscType();
        misc.name = BattleArmor.IMPROVED_STEALTH_ARMOR;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_STEALTH_IMP));
        misc.addLookupName("IS BA Stealth (Improved)");
        misc.addLookupName("Clan BA Stealth (Improved)");
        misc.tonnage = 0;
        misc.criticals = 5;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_STEALTH).or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "252,TM";

        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(3055, 3057, 3059, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(DATE_NONE, 3058, 3059, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_FW, F_WB)
                .setProductionFactions(F_FW, F_WB, F_CSR).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);

        return misc;
    }

    public static MiscType createISBAMimeticCamo() {
        MiscType misc = new MiscType();
        misc.name = BattleArmor.MIMETIC_ARMOR;
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_MIMETIC));
        misc.addLookupName("IS BA Mimetic");
        misc.addLookupName("Clan BA Mimetic");
        misc.tonnage = 0;
        misc.criticals = 7;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_STEALTH).or(F_VISUAL_CAMO).or(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "253,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3058, 3061, 3065, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CS, F_WB)
                .setProductionFactions(F_WB).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);

        return misc;
    }

    public static MiscType createISBAReactiveArmor() {
        MiscType misc = new MiscType();
        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REACTIVE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REACTIVE));
        misc.addLookupName("IS BA Reactive (Blazer)");
        misc.addLookupName("Clan BA Reactive (Blazer)");
        misc.addLookupName("IS BA Reactive");
        misc.addLookupName("Clan BA Reactive");
        misc.tonnage = 0;
        misc.criticals = 7;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_REACTIVE);
        misc.bv = 0;
        misc.rulesRefs = "282,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(3075, 3093, 3100, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(3075, 3093, 3100, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_RS).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);

        return misc;
    }

    public static MiscType createISBAReflectiveArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REFLECTIVE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_BA_REFLECTIVE));
        misc.addLookupName("IS BA Laser Reflective (Reflec/Glazed)");
        misc.addLookupName("Clan BA Laser Reflective (Reflec/Glazed)");
        misc.addLookupName("IS BA Reflective");
        misc.addLookupName("Clan BA Reflective");
        misc.tonnage = 0;
        misc.criticals = 7;
        misc.spreadable = true;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_REFLECTIVE);
        misc.bv = 0;
        misc.rulesRefs = "280,TO";

        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setISAdvancement(3074, 3089, 3105, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(3074, 3089, 3105, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CNC, F_DC).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E);

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
        misc.rulesRefs = "317,TO";
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
        misc.rulesRefs = "317,TO";
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
        misc.rulesRefs = "317,TO";

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
        misc.rulesRefs = "317,TO";
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
        misc.rulesRefs = "317,TO";
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
        misc.rulesRefs = "317,TO";

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
        misc.rulesRefs = "317,TO";
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
        misc.rulesRefs = "317,TO";
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

        misc.name = "Clothing, Light/Naked ;)";
        misc.setInternalName(misc.name);
        misc.addLookupName("ClothingLightNone");
        misc.damageDivisor = 0.5;
        misc.cost = 15;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317,TO";
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
        misc.rulesRefs = "317,TO";
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
        misc.rulesRefs = "317,TO";
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
        misc.rulesRefs = "317,TO";
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
        misc.rulesRefs = "317,TO";
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
        misc.rulesRefs = "317,TO";
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
        misc.rulesRefs = "317,TO";
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
        misc.subType = S_COLD_WEATHER;
        misc.cost = 100;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "317,TO";
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
        misc.rulesRefs = "317,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(2690, 2790, 2820, DATE_NONE, DATE_NONE)
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
        misc.rulesRefs = "317,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_E, RATING_D).setISAdvancement(2680, 2800, 3065, 2850, 3050)
                .setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2680, 2800, 3065, DATE_NONE, DATE_NONE)
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
        misc.rulesRefs = "317,TO";
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
        misc.rulesRefs = "317,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.name = "Snow suit";
        misc.setInternalName(misc.name);
        misc.addLookupName("SnowSuit");
        misc.damageDivisor = 1.0;
        misc.subType = S_ENCUMBERING | S_COLD_WEATHER;
        misc.cost = 70;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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

        misc.name = "Comstar Infantry Kit";
        misc.setInternalName(misc.name);
        misc.addLookupName("CSInfKit");
        misc.addLookupName("ComstarKit");
        misc.damageDivisor = 2.0;
        misc.cost = 4280;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.damageDivisor = 1;
        misc.cost = 1040;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
        ;
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_E)
                .setAvailability(RATING_C, RATING_E, RATING_F, RATING_X)
                .setISAdvancement(2570, 2575, 2580, 2800, DATE_NONE).setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2570, 2575, 2580, 2950, DATE_NONE)
                .setClanApproximate(true, false, false, true, false).setPrototypeFactions(F_TH)
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.cost = 4300;
        misc.flags = misc.flags.or(F_INF_EQUIPMENT).or(F_ARMOR_KIT);
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "195, ATOWC";
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
        misc.rulesRefs = "195, ATOWC";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.rulesRefs = "318,TO";
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
        misc.flags = misc.flags.or(F_ARMORED_MOTIVE_SYSTEM).or(F_TANK_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "283,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3071, 3084, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.flags = misc.flags.or(F_ARMORED_MOTIVE_SYSTEM).or(F_TANK_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "283,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3057, 3083, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.rulesRefs = "289,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3071, 3084, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_WB)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.rulesRefs = "289,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(3050, 3060, 3145, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(3090, DATE_NONE, 3145, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_LC, F_CJF)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
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
        misc.rulesRefs = "289,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(3057, 3079, 3085, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.rulesRefs = "220,TM";
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
        misc.rulesRefs = "290,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3064, 3083, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.rulesRefs = "290,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_F, RATING_D, RATING_D)
                .setISAdvancement(3061, 3079, 3085, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
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
        misc.rulesRefs = "236,TM";
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
        misc.rulesRefs = "290,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3067, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createISMediumShield() {
        MiscType misc = new MiscType();

        misc.name = "Shield (Medium)";
        misc.setInternalName("ISMediumShield");
        misc.addLookupName("Medium Shield");
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
        misc.rulesRefs = "290,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3067, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createISLargeShield() {
        MiscType misc = new MiscType();

        misc.name = "Shield (Large)";
        misc.setInternalName("ISLargeShield");
        misc.addLookupName("Large Shield");
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
        misc.rulesRefs = "290,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3067, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.rulesRefs = "290,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_E, RATING_E, RATING_D)
                .setISAdvancement(3051, 3082, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        ;
        misc.subType |= S_SWORD;
        misc.bv = BV_VARIABLE;
        misc.rulesRefs = "237,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_F, RATING_D, RATING_C)
                .setISAdvancement(3050, 3058, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.ADVANCED);
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
        misc.rulesRefs = "290,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3072, 3087, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CJF)
                .setProductionFactions(F_CJF).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createISSmallVibroblade() {
        MiscType misc = new MiscType();
        misc.name = "Vibroblade (Small)";
        misc.setInternalName("ISSmallVibroblade");
        misc.addLookupName("Small Vibroblade");
        misc.tonnage = 3;
        misc.criticals = 1;
        misc.cost = 150000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT);
        misc.subType |= S_VIBRO_SMALL;
        misc.bv = 12;
        misc.setInstantModeSwitch(true);
        String[] modes = { "Inactive", "Active" };
        misc.setModes(modes);
        misc.rulesRefs = "292,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3065, 3091, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createISMediumVibroblade() {
        MiscType misc = new MiscType();
        misc.name = "Vibroblade (Medium)";
        misc.setInternalName("ISMediumVibroblade");
        misc.addLookupName("Medium Vibroblade");
        misc.tonnage = 5;
        misc.criticals = 2;
        misc.cost = 400000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT);
        misc.subType |= S_VIBRO_MEDIUM;
        misc.bv = 17;
        misc.setInstantModeSwitch(true);
        String[] modes = { "Inactive", "Active" };
        misc.setModes(modes);
        misc.rulesRefs = "292,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3065, 3091, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createISLargeVibroblade() {
        MiscType misc = new MiscType();

        misc.name = "Vibroblade (Large)";
        misc.setInternalName("ISLargeVibroblade");
        misc.addLookupName("Large Vibroblade");
        misc.tonnage = 7;
        misc.criticals = 4;
        misc.cost = 750000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT);
        misc.subType |= S_VIBRO_LARGE;
        misc.bv = 24;
        misc.setInstantModeSwitch(true);
        String[] modes = { "Inactive", "Active" };
        misc.setModes(modes);
        misc.rulesRefs = "292,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3066, 3091, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    // ADDING THE CLUBS FOUND LAYING AROUND TO THIS SECTION.
    public static MiscType createTreeClub() {
        MiscType misc = new MiscType();

        misc.name = "Tree Club";
        misc.setInternalName(misc.name);
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
        misc.setInternalName(misc.name);
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
        misc.setInternalName(misc.name);
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

        misc.name = "C3 Computer [Slave]";
        misc.setInternalName("ISC3SlaveUnit");
        misc.addLookupName("IS C3 Slave");
        misc.shortName = "C3 Slave";
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 250000;
        misc.flags = misc.flags.or(F_C3S).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "209,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3039, 3050, 3065, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
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
        misc.cost = 750000;
        misc.flags = misc.flags.or(F_C3I).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_X)
                .setISAdvancement(3052, 3062, DATE_NONE, 3085, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CS)
                .setProductionFactions(F_CS);
        return misc;
    }

    public static MiscType createC3SBS() {
        MiscType misc = new MiscType();
        misc.name = "C3 Boosted System (C3BS) [Slave]";
        misc.setInternalName("ISC3BoostedSystemSlaveUnit");
        misc.addLookupName("IS C3 Boosted System Slave");
        misc.shortName = "C3 Boosted Slave";
        misc.tonnage = 3;
        misc.criticals = 2;
        misc.cost = 500000;
        misc.flags = misc.flags.or(F_C3SBS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "298,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3071, 3100, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createEmergencyC3M() {
        MiscType misc = new MiscType();

        misc.name = "C3 Emergency Master (C3EM)";
        misc.setInternalName("ISC3EmergencyMaster");
        misc.addLookupName("Emergency C3 Master");
        misc.tonnage = 2;
        misc.criticals = 2;
        misc.cost = 2800000;
        // TODO: implement game rules
        misc.flags = misc.flags.or(F_C3EM).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_C3S).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "298,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3071, 3099, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    // TODO C3 Remote Sensor Launcher - See IO pg 38 (will likely need to be
    // added as weapon)

    public static MiscType createBC3() {
        MiscType misc = new MiscType();

        misc.name = "Battle Armor C3 (BC3)";
        misc.setInternalName("BattleArmorC3");
        misc.addLookupName("IS BattleArmor C3");
        misc.tonnage = .250;
        misc.criticals = 1;
        misc.cost = 62500;
        misc.flags = misc.flags.or(F_C3S).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "297,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(3073, 3095).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createBC3i() {
        MiscType misc = new MiscType();

        misc.name = "Battle Armor Improved C3 (BC3I)";
        misc.setInternalName("ISBC3i");
        misc.addLookupName("IS BC3i");
        misc.addLookupName("IS BattleArmor C3i");
        misc.tonnage = .350;
        misc.criticals = 1;
        misc.cost = 125000;
        misc.flags = misc.flags.or(F_C3I).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "297,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(3063, 3095).setPrototypeFactions(F_WB)
                .setProductionFactions(F_RS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.flags = misc.flags.or(F_CASE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.cost = 50000;
        misc.bv = 0;
        misc.rulesRefs = "210,TM";
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
        misc.flags = misc.flags.or(F_CASEP).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.cost = 150000;
        misc.bv = 0;
        misc.rulesRefs = "71,IO";
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
        misc.setInternalName("CLCASE");
        misc.addLookupName("Clan CASE");
        misc.tonnage = 0.0f;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_CASE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.cost = 50000;
        misc.bv = 0;
        misc.rulesRefs = "210,TM";
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
        misc.rulesRefs = "299,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setISAdvancement(3064, 3082, 3105, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, true, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_RS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.rulesRefs = "299,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setClanAdvancement(3062, 3082, 3105, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, true, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CWF).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.flags = misc.flags.or(F_AP_POD).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 1;
        misc.rulesRefs = "204,TM";
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
        misc.flags = misc.flags.or(F_COMMAND_CONSOLE).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "300,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(2625, 2631, DATE_NONE, 2850, 3030).setISApproximate(true, false, false, true, true)
                .setClanAdvancement(2625, 2631, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS);
        return misc;
    }

    public static MiscType createISMASS() {
        MiscType misc = new MiscType();

        misc.name = "MechWarrior Aquatic Survival System (MASS)";
        misc.setInternalName("ISMASS");
        misc.addLookupName("IS Mass");
        misc.addLookupName("Clan Mass");
        misc.addLookupName("CLMass");
        misc.tonnage = 1.5;
        misc.criticals = 1;
        misc.bv = 9;
        misc.cost = 4000;
        misc.flags = misc.flags.or(F_MASS).or(F_MECH_EQUIPMENT);
        misc.rulesRefs = "325,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_D).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setAdvancement(3048, 3083, DATE_NONE, DATE_NONE, DATE_NONE)
                .setPrototypeFactions(F_FS, F_CGS).setProductionFactions(F_FS)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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

        misc.rulesRefs = "68,IO";
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
        misc.flags = misc.flags.or(F_DRONE_CARRIER_CONTROL).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.rulesRefs = "305,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_E, RATING_F, RATING_F, RATING_E)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
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
        misc.flags = misc.flags.or(F_DRONE_OPERATING_SYSTEM).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_DS_EQUIPMENT)
                .or(F_JS_EQUIPMENT).or(F_SS_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.rulesRefs = "306,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_E, RATING_F, RATING_F, RATING_E)
                .setISAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createISRemoteDroneCommandConsole() {

        MiscType misc = new MiscType();
        misc.name = "Remote Drone Command Console";
        misc.setInternalName("ISRemoteDroneCommandConsole");
        misc.tonnage = 2;
        misc.criticals = 1;
        misc.cost = 50000;
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.rulesRefs = "90,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
                .setISAdvancement(3125, 3140, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_RS);

        return misc;
    }

    public static MiscType createISDroneExtra() {
        // TODO: add game rules for this (these are actually the Drones
        // themselves)
        MiscType misc = new MiscType();
        misc.name = "Drones (as Extra Equipment)";
        misc.setInternalName("ISDroneExtra");
        misc.addLookupName("CLDroneExtra");
        misc.tonnage = 0;
        misc.cost = 0;
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_DRONE_EXTRA).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);      ;
        misc.techAdvancement.setTechBase(TECH_BASE_IS);
        misc.techAdvancement.setISAdvancement(DATE_NONE, 2000, DATE_NONE);
        misc.techAdvancement.setTechRating(RATING_C);
        misc.techAdvancement.setAvailability(new int[] { RATING_E, RATING_F, RATING_F, RATING_X });
        return misc;
    }

    public static MiscType createSmartRoboticControlSystem() {
        // TODO Game Rules.
        MiscType misc = new MiscType();
        misc.name = "Smart Robotic Control System (SRCS)";
        misc.setInternalName("SmartRoboticControlSystem");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.rulesRefs = "140,IO";
        misc.flags = misc.flags.or(F_SRCS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_JS_EQUIPMENT).or(F_SS_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.subType = S_STANDARD;
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
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.rulesRefs = "140,IO";
        misc.flags = misc.flags.or(F_SRCS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_JS_EQUIPMENT).or(F_SS_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.subType = S_IMPROVED;
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
        misc.rulesRefs = "141,IO";
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
        misc.rulesRefs = "141,IO";
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
        misc.rulesRefs = "141,IO";
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
        misc.rulesRefs = "142,IO";
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
        misc.rulesRefs = "142,IO";
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
        misc.rulesRefs = "143,IO";
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
        misc.rulesRefs = "143,IO";
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
        misc.setInternalName("AutoTacticalAnalysisComputer");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_ATAC).or(F_DS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.rulesRefs = "145,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_F, RATING_X, RATING_F, RATING_F)
                .setISAdvancement(2705, DATE_NONE, DATE_NONE, 2780, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2705, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH);

        return misc;
    }

    public static MiscType createAdvRoboticTransportSystem() {
        // TODO Game Rules.
        MiscType misc = new MiscType();
        misc.name = "Advanced Robotic Transport System (ARTS)";
        misc.setInternalName("AdvRoboticTransportSystem");
        misc.tonnage = 0; //TODO weight by bay (see IO pg 147)
        misc.criticals = 0;
        misc.cost = 0; //TODO Costs
        misc.flags = misc.flags.or(F_ARTS).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.rulesRefs = "147,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_D, RATING_E, RATING_E, RATING_E)
                .setISAdvancement(2600, 2609, DATE_NONE, 2804, 3068).setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2600, 2609, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);

        return misc;
    }

    public static MiscType createDirectTacticalAnalysisSystem() {
        // TODO Game Rules.
        MiscType misc = new MiscType();
        misc.name = "Direct Tactical Analysis Control (DTAC) System";
        misc.setInternalName("DirectTacticalAnalysisSystem");
        misc.tonnage = TONNAGE_VARIABLE;;
        misc.criticals = 0;
        misc.cost = COST_VARIABLE;;
        misc.flags = misc.flags.or(F_DTAC).or(F_DS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.rulesRefs = "146,IO";
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
        misc.rulesRefs = "147,IO";
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
        misc.rulesRefs = "148,IO";
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
        misc.rulesRefs = "309,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(3038, 3079).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    // TODO BattleMech Full-Head Ejection System - IO pg 40 - This was at one
    // point a quirk

    public static MiscType createIMEjectionSeat() {
        MiscType misc = new MiscType();

        misc.name = "Ejection Seat (Industrial Mech)";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = 25000;
        misc.flags = misc.flags.or(F_EJECTION_SEAT).or(F_MECH_EQUIPMENT);
        misc.bv = 0;
        misc.industrial = true;
        misc.rulesRefs = "213,TM";
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
        misc.tonnage = TONNAGE_VARIABLE;
        misc.tankslots = 1;
        misc.cost = 25000;
        misc.flags = misc.flags.or(F_EJECTION_SEAT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.bv = 0;
        misc.industrial = true;
        misc.rulesRefs = "213,TM";
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
        misc.tonnage = TONNAGE_VARIABLE;
        misc.tankslots = 0;
        misc.cost = 5000;
        misc.flags = misc.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_LIFEBOAT);
        misc.bv = 0;
        misc.industrial = true;
        misc.rulesRefs = "216,TM";
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
        misc.rulesRefs = "227,TM";
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
        misc.rulesRefs = "227,TM";
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
        misc.flags = misc.flags.or(F_BAP).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.bv = 10;
        misc.rulesRefs = "204,TM";
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
        misc.tonnage = 2.0;
        misc.criticals = 3;
        misc.cost = 600000;
        misc.flags = misc.flags.or(F_BAP).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        ;
        misc.bv = 10;
        misc.rulesRefs = "71,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
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
        misc.flags = misc.flags.or(F_BAP).or(F_BLOODHOUND).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.bv = 25;
        misc.rulesRefs = "278,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3058, 3082, 3094, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_CS)
                .setProductionFactions(F_RS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.flags = misc.flags.or(F_BAP).or(F_BLOODHOUND).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.bv = 25;
        // Since its Tactical Handbook Using TO Values
        misc.rulesRefs = "Unofficial";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3058, 3082, 3094, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_CS)
                .setProductionFactions(F_RS).setStaticTechLevel(SimpleTechLevel.UNOFFICIAL);;
        return misc;
    }

    public static MiscType createCLActiveProbe() {
        MiscType misc = new MiscType();

        misc.name = "Active Probe [Clan]";
        misc.setInternalName(Sensor.CLAN_AP);
        misc.addLookupName("Active Probe");
        misc.addLookupName("Clan Active Probe");
        misc.addLookupName("ClActiveProbe");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 200000;
        misc.flags = misc.flags.or(F_BAP).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        ;
        misc.bv = 12;
        misc.rulesRefs = "204,TM";
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
        misc.flags = misc.flags.or(F_BAP).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.bv = 7;
        misc.rulesRefs = "204,TM";
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
        misc.flags = misc.flags.or(F_ECM).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.bv = 61;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "213,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C).setISAdvancement(2595, 2597, 3050, 2845, 3045)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_CC);
        return misc;
    }

    public static MiscType createGECMPrototype() {
        MiscType misc = new MiscType();

        misc.name = "ECM Suite (Guardian) Prototype";
        misc.setInternalName("ISGuardianECMSuitePrototype");
        misc.addLookupName("IS Prototype Guardian ECM");
        misc.tonnage = 2.0f;
        misc.criticals = 3;
        misc.cost = 1000000;
        misc.flags = misc.flags.or(F_ECM).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.bv = 61;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "71,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_F, RATING_X, RATING_X)
                .setISAdvancement(2595, DATE_NONE, DATE_NONE, 2597, DATE_NONE)
                .setISApproximate(true, false, false, true, false).setPrototypeFactions(F_TH);
        return misc;
    }

    public static MiscType createCLECM() {
        MiscType misc = new MiscType();

        misc.name = "ECM Suite [Clan]";
        misc.setInternalName("CLECMSuite");
        misc.addLookupName("Clan ECM Suite");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 200000;
        misc.flags = misc.flags.or(F_ECM).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.bv = 61;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "213,TM";
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
        misc.flags = misc.flags.or(F_ECM).or(F_ANGEL_ECM).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.bv = 100;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "279,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3057, 3080, 3085, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(3058, 3080, 3085, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_DC, F_CNC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.flags = misc.flags.or(F_ECM).or(F_ANGEL_ECM).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
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
        misc.tonnage = 7.5;
        misc.criticals = 4;
        misc.cost = 500000;
        misc.flags = misc.flags.or(F_EW_EQUIPMENT).or(F_BAP).or(F_ECM)
                .or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.bv = 39;
        misc.rulesRefs = "310,TO";
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
        misc.tonnage = 1.5f;
        misc.criticals = 2;
        misc.cost = 500000;
        misc.flags = misc.flags.or(F_WATCHDOG).or(F_ECM).or(F_BAP).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.bv = 68;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "278,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3059, 3080, 3085, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSF).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createNovaCEWS() {
        MiscType misc = new MiscType();

        misc.name = "Nova Combined Electronic Warfare System (CEWS)";
        misc.setInternalName(Sensor.NOVA);
        misc.addLookupName("Nova CEWS");
        misc.addLookupName("NovaCEWS");
        misc.addLookupName("CLNCEWS");
        misc.tonnage = 1.5f;
        misc.criticals = 1;
        misc.cost = 1100000; // we assume that WOR had a typo there.
        misc.flags = misc.flags.or(F_NOVA).or(F_ECM).or(F_BAP).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT);
        misc.bv = 68;
        misc.setModes(new String[] { "ECM", "Off" });
        misc.setInstantModeSwitch(false);
        misc.rulesRefs = "66,IO";
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
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_RECON_CAMERA);
        misc.rulesRefs = "337,TO";
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
        misc.rulesRefs = "375,TO";
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
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.hittable = true;
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT)
                .or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT).or(F_SENSOR_DISPENSER);
        misc.bv = 0;
        misc.cost = 60000;
        misc.industrial = true;
        misc.rulesRefs = "73,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2586, DATE_NONE, DATE_NONE, 2590, DATE_NONE)
                .setISApproximate(true, false, false, true, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createISLookDownRadar() {
        MiscType misc = new MiscType();
        misc.name = "Satellite Imager [Look-Down Radar]";
        misc.setInternalName("ISLookDownRadar");
        misc.addLookupName("CLLookDownRadar");
        misc.tonnage = 5;
        misc.cost = 400000;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_LOOKDOWN_RADAR).or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "340,TO";
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
        misc.name = "Satellite Imager [Infrared Imager]";
        misc.setInternalName("ISInfraredImager");
        misc.addLookupName("CLInfraredImager");
        misc.tonnage = 5;
        misc.cost = 250000;
        misc.flags = misc.flags.or(F_INFRARED_IMAGER).or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.bv = 0;
        misc.criticals = 1;
        misc.rulesRefs = "339,TO";
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
        misc.name = "Satellite Imager [Hyperspectral Imager]";
        misc.setInternalName("ISHypersprectralImager");
        misc.addLookupName("ISHyperspectralImager");
        misc.tonnage = 7.5;
        misc.cost = 550000;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_HIRES_IMAGER).or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "338,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3045, 3055, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, true, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return misc;
    }

    public static MiscType createISHIResImager() {
        MiscType misc = new MiscType();
        misc.name = "Satellite Imager [High-Resolution (Hi-Res) Imager]";
        misc.setInternalName("ISHighResImager");
        misc.addLookupName("CLHighResImager");
        misc.tonnage = 2.5;
        misc.cost = 150000;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_HIRES_IMAGER).or(F_VTOL_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "339,TO";
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
        misc.flags = misc.flags.or(MiscType.F_BASIC_FIRECONTROL).or(MiscType.F_SUPPORT_TANK_EQUIPMENT)
                .or(MiscType.F_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.industrial = true;
        misc.rulesRefs = "217,TM";
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
        misc.flags = misc.flags.or(MiscType.F_ADVANCED_FIRECONTROL)
                .or(MiscType.F_SUPPORT_TANK_EQUIPMENT.or(MiscType.F_TANK_EQUIPMENT));
        misc.omniFixedOnly = true;
        misc.rulesRefs = "217,TM";
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
        misc.cost = 100000;
        misc.flags = misc.flags.or(F_ARTEMIS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.rulesRefs = "206,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C).setISAdvancement(2592, 2598, 3045, 2855, 3035)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FW);
        return misc;
    }

    // TODO Per IO pg 71 we should have a ProtoType Artemis IV.

    public static MiscType createISProtoArtemis() {
        MiscType misc = new MiscType();

        misc.name = "Prototype Artemis IV FCS";
        misc.setInternalName("ISArtemisIVProto");
        misc.addLookupName("IS Proto type Artemis IV FCS");
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.cost = 100000;
        misc.flags = misc.flags.or(F_ARTEMIS_PROTO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT)
                .or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.rulesRefs = "217,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2592, DATE_NONE, DATE_NONE, 2612, 3035)
                .setISApproximate(true, false, false, true, true).setPrototypeFactions(F_TH).setProductionFactions(F_TH)
                .setReintroductionFactions(F_FW);
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
        misc.flags = misc.flags.or(F_ARTEMIS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.rulesRefs = "206,TM";
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
        misc.flags = misc.flags.or(F_ARTEMIS_V).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.rulesRefs = "283,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3061, 3085, 3093, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CGS)
                .setProductionFactions(F_CSF, F_RD).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.flags = misc.flags.or(F_APOLLO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.rulesRefs = "330,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3065, 3071, DATE_NONE, DATE_NONE, DATE_NONE)
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

        misc.name = "Targeting Computer [IS]";
        misc.setInternalName("ISTargeting Computer");
        misc.addLookupName("IS Targeting Computer");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.bv = 0; // TarComps modify weapon BVs, they have none of their own.
        misc.flags = misc.flags.or(F_TARGCOMP).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        // see note above
        misc.spreadable = true;
        String[] modes = { "Normal", "Aimed shot" };
        misc.setModes(modes);
        misc.rulesRefs = "238,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3052, 3062, 3067, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS);
        return misc;
    }

    public static MiscType createCLTargComp() {
        MiscType misc = new MiscType();

        misc.name = "Targeting Computer [Clan]";
        misc.setInternalName("CLTargeting Computer");
        misc.addLookupName("Clan Targeting Computer");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.bv = 0; // TarComps modify weapon BVs, they have none of their own.
        misc.flags = misc.flags.or(F_TARGCOMP).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        // see note above
        misc.spreadable = true;
        String[] modes = { "Normal", "Aimed shot" };
        misc.setModes(modes);
        misc.rulesRefs = "238,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_E, RATING_D, RATING_D)
                .setClanAdvancement(2850, 2860, 2863, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CMN)
                .setProductionFactions(F_CMN);
        return misc;
    }

    // TAG - In with the Weapons.

    // Fluid Guns and Sprayer.
    // Fluid Guns - in with Weapons.

    public static MiscType createMechSprayer() {
        MiscType misc = new MiscType();

        misc.name = "Sprayer [Mech]";
        misc.setInternalName("MechSprayer");
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.cost = 1000;
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_SPRAYER);
        misc.industrial = true;
        misc.rulesRefs = "248,TM";
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

        misc.name = "Sprayer [Vehicular]";
        misc.setInternalName("Tank Sprayer");
        misc.tonnage = 0.015;
        misc.criticals = 1;
        misc.cost = 1000;
        misc.flags = misc.flags.or(F_TANK_EQUIPMENT).or(F_SPRAYER);
        misc.industrial = true;
        misc.rulesRefs = "248,TM";
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
        misc.setInternalName(misc.name);
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_HEAT_SINK);
        misc.bv = 0;
        misc.rulesRefs = "221,TM";
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
        misc.setInternalName(misc.name);
        misc.addLookupName("IS1 Compact Heat Sink");
        misc.tonnage = 1.5f;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_HEAT_SINK).or(F_COMPACT_HEAT_SINK);
        misc.bv = 0;
        misc.rulesRefs = "316,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3058, 3079)
            .setISApproximate(false, false).setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS).setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createIS2CompactHeatSinks() {
        MiscType misc = new MiscType();

        misc.name = "2 Compact Heat Sinks";
        misc.setInternalName(misc.name);
        misc.addLookupName("IS2 Compact Heat Sinks");
        misc.tonnage = 3.0f;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_DOUBLE_HEAT_SINK).or(F_COMPACT_HEAT_SINK);
        misc.bv = 0;
        misc.rulesRefs = "316,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3058, 3079)
            .setISApproximate(false, false).setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS).setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createISDoubleHeatSinkPrototype() {
        MiscType misc = new MiscType();

        misc.name = "Double Heat Sink Prototype";
        misc.setInternalName("ISDoubleHeatSinkPrototype");
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
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X);
        return misc;
    }

    public static MiscType createISFreezerPrototype() {
        MiscType misc = new MiscType();

        misc.name = "Double Heat Sink (Freezers)";
        misc.setInternalName("ISDoubleHeatSinkFreezer");
        misc.addLookupName("Freezers");
        misc.tonnage = 1.0f;
        misc.criticals = 3;
        misc.cost = 30000; // Using Cost
        misc.flags = misc.flags.or(F_IS_DOUBLE_HEAT_SINK_PROTOTYPE);
        misc.bv = 0;
        misc.rulesRefs = "71, IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3022, DATE_NONE, DATE_NONE, 3040, DATE_NONE)
                .setISApproximate(true, false, false, true, false).setPrototypeFactions(F_FS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_F, RATING_X, RATING_X);
        return misc;
    }

    public static MiscType createISDoubleHeatSink() {
        MiscType misc = new MiscType();

        misc.name = "Double Heat Sink";
        misc.setInternalName("ISDoubleHeatSink");
        misc.addLookupName("IS Double Heat Sink");
        misc.addLookupName("ISDouble Heat Sink");
        misc.tonnage = 1.0f;
        misc.criticals = 3;
        misc.flags = misc.flags.or(F_DOUBLE_HEAT_SINK);
        misc.bv = 0;
        misc.rulesRefs = "221,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(2559, 2567, 3045, 2865, 3040)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH).setTechRating(RATING_E)
                .setAvailability(RATING_C, RATING_E, RATING_D, RATING_C);
        return misc;
    }

    public static MiscType createCLDoubleHeatSink() {
        MiscType misc = new MiscType();

        misc.name = "Double Heat Sink";
        misc.setInternalName("CLDoubleHeatSink");
        misc.addLookupName("Clan Double Heat Sink");
        misc.addLookupName("CLDouble Heat Sink");
        misc.tonnage = 1.0f;
        misc.criticals = 2;
        misc.flags = misc.flags.or(F_DOUBLE_HEAT_SINK);
        misc.bv = 0;
        misc.rulesRefs = "221,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setClanAdvancement(2825, 2827, 2829, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_E, RATING_D, RATING_C);
        return misc;
    }

    public static MiscType createCLLaserHeatSink() {
        MiscType misc = new MiscType();

        misc.name = "Laser Heat Sink";
        misc.setInternalName(misc.name);
        misc.addLookupName("CLLaser Heat Sink");
        misc.tonnage = 1.0f;
        misc.criticals = 2;
        misc.flags = misc.flags.or(F_DOUBLE_HEAT_SINK).or(F_LASER_HEAT_SINK).or(F_MECH_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "316,TO";
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
        misc.rulesRefs = "89,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3115, 3112, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E);
        return misc;
    }

    // Industrial Equipment

    public static MiscType createBackhoe() {
        MiscType misc = new MiscType();

        misc.name = "Backhoe";
        misc.setInternalName(misc.name);
        misc.tonnage = 5;
        misc.criticals = 6;
        misc.cost = 50000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_BACKHOE;
        misc.bv = 8;
        misc.industrial = true;
        misc.rulesRefs = "241,TM";
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
        misc.name = "Bridge Layer (Light)";
        misc.setInternalName("LightBridgeLayer");
        misc.flags = misc.flags.or(F_LIGHT_BRIDGE_LAYER).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "242,TM";
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
        misc.name = "Bridge Layer (Medium)";
        misc.setInternalName("MediumBridgeLayer");
        misc.flags = misc.flags.or(F_MEDIUM_BRIDGE_LAYER).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "242,TM";
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
        misc.name = "Bridge Layer (Heavy)";
        misc.setInternalName("HeavyBridgeLayer");
        misc.flags = misc.flags.or(F_HEAVY_BRIDGE_LAYER).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "242,TM";
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
        misc.setInternalName(misc.name);
        misc.bv = 10;
        misc.flags = misc.flags.or(F_BULLDOZER).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "241,TM";
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
        misc.setInternalName(misc.name);
        misc.tonnage = 5;
        misc.criticals = 5;
        misc.cost = 100000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_CHAINSAW;
        misc.bv = 7;
        misc.industrial = true;
        misc.rulesRefs = "241,TM";
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
        misc.setInternalName(misc.name);
        misc.tonnage = 2.5f;
        misc.criticals = 4;
        misc.cost = 75000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_COMBINE;
        misc.bv = 5;
        misc.industrial = true;
        misc.rulesRefs = "243,TM";
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
        misc.setInternalName(misc.name);
        misc.tonnage = 7;
        misc.criticals = 7;
        misc.cost = 100000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_DUAL_SAW;
        misc.bv = 9;
        misc.industrial = true;
        misc.rulesRefs = "243,TM";
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
        misc.rulesRefs = "243,TM";
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
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = 5000;
        misc.flags = misc.flags.or(F_DUMPER).or(F_TANK_EQUIPMENT).or(F_MECH_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "243,TM";
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
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = 5000;
        misc.flags = misc.flags.or(F_DUMPER).or(F_TANK_EQUIPMENT).or(F_MECH_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "243,TM";
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
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = 5000;
        misc.flags = misc.flags.or(F_DUMPER).or(F_TANK_EQUIPMENT).or(F_MECH_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "243,TM";
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
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = 5000;
        misc.flags = misc.flags.or(F_DUMPER).or(F_TANK_EQUIPMENT).or(F_MECH_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "243,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createFuel1() {
        MiscType misc = new MiscType();

        misc.name = "Extended Fuel Tank (1 ton)";
        misc.setInternalName(misc.name);
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 500;
        misc.flags = misc.flags.or(F_FUEL).or(F_MECH_EQUIPMENT);
        misc.explosive = true;
        misc.industrial = true;
        misc.rulesRefs = "244,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(2300, 2350, 2490, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2300, 2350, 2490, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createLightFluidSuctionSystemMech() {
        MiscType misc = new MiscType();
        misc.name = "Light Fluid Suction System (Mech)";
        misc.setInternalName(misc.name);
        misc.criticals = 1;
        misc.tonnage = .5;
        misc.cost = 1000;
        misc.flags = misc.flags.or(F_FLUID_SUCTION_SYSTEM).or(F_MECH_EQUIPMENT).andNot(F_SC_EQUIPMENT)
                .andNot(F_DS_EQUIPMENT).andNot(F_JS_EQUIPMENT).andNot(F_WS_EQUIPMENT).andNot(F_SS_EQUIPMENT);
        misc.rulesRefs = "248,TM";
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
        misc.name = "Light Fluid Suction System (Vehicle)";
        misc.setInternalName(misc.name);
        misc.criticals = 1;
        misc.tankslots = 1;
        misc.tonnage = .5;
        misc.cost = 1000;
        misc.flags = misc.flags.or(F_LIGHT_FLUID_SUCTION_SYSTEM).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .andNot(F_SC_EQUIPMENT).andNot(F_DS_EQUIPMENT).andNot(F_JS_EQUIPMENT).andNot(F_WS_EQUIPMENT)
                .andNot(F_SS_EQUIPMENT);
        misc.rulesRefs = "248,TM";
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
        misc.name = "Fluid Suction System[Standard]";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.tankslots = 1;
        misc.tonnage = 1;
        misc.cost = 25000;
        misc.flags = misc.flags.or(F_FLUID_SUCTION_SYSTEM).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .andNot(F_SC_EQUIPMENT).andNot(F_DS_EQUIPMENT).andNot(F_JS_EQUIPMENT).andNot(F_WS_EQUIPMENT)
                .andNot(F_SS_EQUIPMENT);
        misc.rulesRefs = "248,TM";
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
        misc.setInternalName(misc.name);
        misc.addLookupName("PileDriver");
        misc.addLookupName("Pile Driver");
        misc.tonnage = 10;
        misc.criticals = 8;
        misc.cost = 100000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_PILE_DRIVER;
        misc.bv = 5;
        misc.industrial = true;
        misc.rulesRefs = "244,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType create20mLadder() {
        MiscType misc = new MiscType();
        misc.name = "Ladder (20m)";
        misc.setInternalName(misc.name);
        misc.tankslots = 1;
        misc.criticals = 1;
        misc.tonnage = 0.1;
        misc.cost = 100;
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "244,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType create40mLadder() {
        MiscType misc = new MiscType();
        misc.name = "Ladder (40m)";
        misc.setInternalName(misc.name);
        misc.tankslots = 1;
        misc.criticals = 1;
        misc.tonnage = 0.2;
        misc.cost = 200;
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_LADDER)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "244,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType create60mLadder() {
        MiscType misc = new MiscType();
        misc.name = "Ladder (60m)";
        misc.setInternalName(misc.name);
        misc.tankslots = 1;
        misc.criticals = 1;
        misc.tonnage = 0.3;
        misc.cost = 300;
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_LADDER)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "244,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType create80mLadder() {
        MiscType misc = new MiscType();
        misc.name = "Ladder (80m)";
        misc.setInternalName(misc.name);
        misc.tankslots = 1;
        misc.criticals = 1;
        misc.tonnage = 0.4;
        misc.cost = 400;
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_LADDER)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "244,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType create100mLadder() {
        MiscType misc = new MiscType();
        misc.name = "Ladder (100m)";
        misc.setInternalName(misc.name);
        misc.tankslots = 1;
        misc.criticals = 1;
        misc.tonnage = 0.5;
        misc.cost = 500;
        misc.flags = misc.flags.or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_LADDER)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "244,TM";
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
        misc.setInternalName(misc.name);
        misc.addLookupName("Lift Hoist");
        misc.tonnage = 3;
        misc.criticals = 3;
        misc.cost = 50000;
        misc.flags = misc.flags.or(F_LIFTHOIST).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT);
        misc.bv = 0;
        misc.industrial = true;
        misc.rulesRefs = "245,TM";
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

        misc.name = "Manipulator [Non-Mech/Non-Battle Armor]";
        misc.addLookupName("Manipulator");
        misc.setInternalName(misc.name);
        misc.flags = misc.flags.or(F_MANIPULATOR).or(F_SUPPORT_TANK_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_VTOL_EQUIPMENT);
        misc.industrial = true;
        misc.tonnage = 0.01;
        misc.cost = 7500;
        misc.criticals = 1;
        misc.rulesRefs = "245,TM";
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
        misc.setInternalName("MiningDrill");
        misc.cost = 10000;
        misc.tonnage = 3.0;
        misc.criticals = 4;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT);
        misc.subType |= S_MINING_DRILL;
        misc.bv = 6;
        misc.industrial = true;
        misc.rulesRefs = "246,TM";
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
        misc.setInternalName("RefuelingDrogue");
        misc.flags = misc.flags.or(F_REFUELING_DROGUE).or(F_FIGHTER_EQUIPMENT).or(F_VTOL_EQUIPMENT)
                .or(F_SC_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "247,TM";
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
        misc.setInternalName(misc.name);
        misc.tonnage = 5;
        misc.criticals = 5;
        misc.cost = 100000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_ROCK_CUTTER;
        misc.bv = 6;
        misc.industrial = true;
        misc.rulesRefs = "247,TM";
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
        misc.setInternalName(misc.name);
        misc.addLookupName("SalvageArm");
        misc.tonnage = 3;
        misc.criticals = 2;
        misc.bv = 0;
        misc.cost = 50000;
        misc.flags = misc.flags.or(F_SALVAGE_ARM).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "248,TM";
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
        misc.setInternalName(misc.name);
        misc.tonnage = 2;
        misc.criticals = 1;
        misc.cost = 75000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.subType |= S_SPOT_WELDER;
        misc.bv = 5;
        misc.industrial = true;
        misc.rulesRefs = "248,TM";
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
        misc.setInternalName("IS Wrecking Ball");
        misc.addLookupName("WreckingBall");
        misc.addLookupName("Clan Wrecking Ball");
        misc.addLookupName("CLWrecking Ball");
        misc.tonnage = 4;
        misc.criticals = 5;
        misc.cost = 110000;
        misc.flags = misc.flags.or(F_CLUB).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT);
        misc.subType |= S_WRECKING_BALL;
        misc.bv = 8;
        misc.industrial = true;
        misc.rulesRefs = "249,TM";
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
        misc.rulesRefs = "306,TO";
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
        misc.rulesRefs = "323,TO";
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
        misc.cost = 50000000;
        misc.name = "Naval Comm-Scanner Suite (Small)";
        misc.setInternalName("ISSmallNavalCommScannerSuite");
        misc.addLookupName("CLSmallNavalCommScannerSuite");
        misc.flags = misc.flags.or(F_SMALL_COMM_SCANNER_SUITE).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT)
                .or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.rulesRefs = "332,TO";
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
        misc.cost = 250000000;
        misc.name = "Naval Comm-Scanner Suite (Large)";
        misc.setInternalName("ISLargeNavalCommScannerSuite");
        misc.addLookupName("CLLargeNavalCommScannerSuite");
        misc.flags = misc.flags.or(F_LARGE_COMM_SCANNER_SUITE).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT)
                .or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.rulesRefs = "332,TO";
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
        misc.rulesRefs = "332,TO";
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
		misc.rulesRefs = "334,TO";
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
        misc.rulesRefs = "337,TO";
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
        misc.rulesRefs = "323,TO";
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
        misc.cost = 3500;
        misc.flags = misc.flags.or(MiscType.F_LASER_INSULATOR).or(MiscType.F_SUPPORT_TANK_EQUIPMENT)
                .or(MiscType.F_MECH_EQUIPMENT).or(MiscType.F_FIGHTER_EQUIPMENT).or(MiscType.F_TANK_EQUIPMENT);
        misc.rulesRefs = "322,TO";
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
        misc.flags = misc.flags.or(F_VEHICLE_MINE_DISPENSER).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 8; // because it includes 2 mines
        misc.rulesRefs = "325,TO";
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
        // TODO: implement game rules for this, analog to the mine for BAs
        misc.flags = misc.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .or(F_SPACE_MINE_DISPENSER);
        misc.bv = 200; // because it includes 2 mines. 100 for each mine,
                       // becaues it deals a max potential damage of 100
        misc.rulesRefs = "325,TO";
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
        misc.flags = misc.flags.or(F_MINESWEEPER).or(F_TANK_EQUIPMENT);
        misc.bv = 30;
        misc.rulesRefs = "326,TO";
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
        misc.setInternalName(misc.name);
        misc.setModes(new String[] { "Off", "On" });
        misc.instantModeSwitch = false;
        misc.explosive = true;
        misc.tonnage = 3;
        misc.criticals = CRITICALS_VARIABLE;
        misc.spreadable = true;
        misc.cost = 1000000;
        misc.flags = misc.flags.or(F_BLUE_SHIELD).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "296,TO";
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
        misc.rulesRefs = "297,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_D, RATING_F, RATING_D, RATING_D)
                .setISAdvancement(DATE_PS, 3080, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false).setProductionFactions(F_WB)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createCargo1() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (1 ton)";
        misc.setInternalName(misc.name);
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createHalfCargo() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (0.5 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo15() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (1.5 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 1.5;
        misc.criticals = 2;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo2() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (2 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 2;
        misc.criticals = 2;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo25() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (2.5 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 2.5;
        misc.criticals = 3;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo3() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (3 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 3;
        misc.criticals = 3;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo35() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (3.5 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 3.5;
        misc.criticals = 4;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo4() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (4 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 4;
        misc.criticals = 4;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo45() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (4.5 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 4.5;
        misc.criticals = 5;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo5() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (5 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 5;
        misc.criticals = 5;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo55() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (5.5 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 5.5;
        misc.criticals = 6;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo6() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (6 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 6;
        misc.criticals = 6;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo65() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (6.5 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 6.5;
        misc.criticals = 7;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo7() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (7 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 7;
        misc.criticals = 7;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo75() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (7.5 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 7.5;
        misc.criticals = 8;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo8() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (8 ton)";
        misc.setInternalName(misc.name);
        misc.tonnage = 8;
        misc.criticals = 8;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo85() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (8.5 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 8.5;
        misc.criticals = 9;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo9() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (9 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 9;
        misc.criticals = 9;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo95() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (9.5 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 9.5;
        misc.criticals = 10;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo10() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (10 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 10;
        misc.criticals = 10;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo105() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (10.5 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 10.5;
        misc.criticals = 11;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargo11() {
        MiscType misc = new MiscType();

        misc.name = "Cargo (11 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 11;
        misc.criticals = 11;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createLiquidCargo1() {
        MiscType misc = new MiscType();

        misc.name = "Liquid Storage (1 ton)";
        misc.setInternalName(misc.name);
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_LIQUID_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createHalfLiquidCargo() {
        MiscType misc = new MiscType();

        misc.name = "Liquid Storage (0.5 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_LIQUID_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createCargoContainer() {
        MiscType misc = new MiscType();

        misc.name = "Cargo Container (10 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 10;
        misc.criticals = 1;
        misc.cost = 0;
		misc.flags = misc.flags.or(F_CARGO).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_SC_EQUIPMENT)
				.or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
		misc.industrial = true;
	      misc.tankslots = 1;
        misc.rulesRefs = "239,TM";
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
        misc.flags = misc.flags.or(F_CHAFF_POD).or(F_TANK_EQUIPMENT).or(F_MECH_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.bv = 19;
        misc.rulesRefs = "299,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(3069, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.rulesRefs = "300,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_F)
                .setISAdvancement(2630, DATE_NONE, DATE_NONE, 2790, 3099)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setReintroductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    // CommsGear
    public static MiscType createCommsGear1() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (1 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:1");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMMUNICATIONS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);
        misc.industrial = true;
        misc.rulesRefs = "212,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCommsGear2() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (2 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:2");
        misc.tonnage = 2;
        misc.criticals = 2;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMMUNICATIONS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);
        misc.industrial = true;
        misc.rulesRefs = "212,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCommsGear3() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (3 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:3");
        misc.tonnage = 3;
        misc.criticals = 3;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMMUNICATIONS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);
        misc.industrial = true;
        misc.rulesRefs = "212,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCommsGear4() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (4 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:4");
        misc.tonnage = 4;
        misc.criticals = 4;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMMUNICATIONS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);
        misc.industrial = true;
        misc.rulesRefs = "212,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCommsGear5() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (5 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:5");
        misc.tonnage = 5;
        misc.criticals = 5;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMMUNICATIONS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);
        misc.industrial = true;
        misc.rulesRefs = "212,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCommsGear6() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (6 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:6");
        misc.tonnage = 6;
        misc.criticals = 6;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMMUNICATIONS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);
        misc.industrial = true;
        misc.rulesRefs = "212,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCommsGear7() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (7 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:7");
        misc.tonnage = 7;
        misc.criticals = 7;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMMUNICATIONS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);
        misc.industrial = true;
        misc.rulesRefs = "212,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCommsGear8() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (8 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:8");
        misc.tonnage = 8;
        misc.criticals = 8;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMMUNICATIONS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);
        misc.industrial = true;
        misc.rulesRefs = "212,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCommsGear9() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (9 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:9");
        misc.tonnage = 9;
        misc.criticals = 9;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMMUNICATIONS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);
        misc.industrial = true;
        misc.rulesRefs = "212,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCommsGear10() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (10 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:10");
        misc.tonnage = 10;
        misc.criticals = 10;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMMUNICATIONS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);
        misc.industrial = true;
        misc.rulesRefs = "212,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCommsGear11() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (11 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:11");
        misc.tonnage = 11;
        misc.criticals = 11;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMMUNICATIONS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);
        misc.industrial = true;
        misc.rulesRefs = "212,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCommsGear12() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (12 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:12");
        misc.tonnage = 12;
        misc.criticals = 12;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMMUNICATIONS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);
        misc.industrial = true;
        misc.rulesRefs = "212,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCommsGear13() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (13 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:13");
        misc.tonnage = 13;
        misc.criticals = 13;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMMUNICATIONS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);
        misc.industrial = true;
        misc.rulesRefs = "212,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCommsGear14() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (14 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:14");
        misc.tonnage = 14;
        misc.criticals = 14;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMMUNICATIONS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);
        misc.industrial = true;
        misc.rulesRefs = "212,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_ES, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return misc;
    }

    public static MiscType createCommsGear15() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (15 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:15");
        misc.tonnage = 15;
        misc.criticals = 15;
        misc.bv = 0;
        misc.flags = misc.flags.or(F_COMMUNICATIONS).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);
        misc.industrial = true;
        misc.rulesRefs = "212,TM";
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
        misc.rulesRefs = "301,TO";
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
        misc.flags = misc.flags.or(F_FIELD_KITCHEN).or(F_TANK_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT)
                .or(F_SS_EQUIPMENT);
        misc.bv = 0;
        misc.industrial = true;
        misc.rulesRefs = "217,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setAdvancement(DATE_PS, DATE_PS, DATE_PS)
            .setTechRating(RATING_A).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createFuelHalf() {
        MiscType misc = new MiscType();

        misc.name = "Extended Fuel Tank (0.5 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.cost = 500;
        misc.flags = misc.flags.or(F_FUEL).or(F_MECH_EQUIPMENT);
        misc.explosive = true;
        misc.industrial = true;
        misc.rulesRefs = "244,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(2300, 2350, 2490, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2300, 2350, 2490, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createFuel2() {
        MiscType misc = new MiscType();

        misc.name = "Extended Fuel Tank (2 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 2;
        misc.criticals = 2;
        misc.cost = 1000;
        misc.flags = misc.flags.or(F_FUEL).or(F_MECH_EQUIPMENT);
        misc.explosive = true;
        misc.industrial = true;
        misc.rulesRefs = "244,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(2300, 2350, 2490, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2300, 2350, 2490, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createFuel25() {
        MiscType misc = new MiscType();

        misc.name = "Extended Fuel Tank (2.5 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 2.5;
        misc.criticals = 3;
        misc.cost = 1500;
        misc.flags = misc.flags.or(F_FUEL).or(F_MECH_EQUIPMENT);
        misc.explosive = true;
        misc.industrial = true;
        misc.rulesRefs = "244,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(2300, 2350, 2490, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2300, 2350, 2490, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createFuel3() {
        MiscType misc = new MiscType();

        misc.name = "Extended Fuel Tank (3 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 3;
        misc.criticals = 3;
        misc.cost = 1500;
        misc.flags = misc.flags.or(F_FUEL).or(F_MECH_EQUIPMENT);
        misc.explosive = true;
        misc.industrial = true;
        misc.rulesRefs = "244,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(2300, 2350, 2490, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2300, 2350, 2490, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createFuel35() {
        MiscType misc = new MiscType();

        misc.name = "Extended Fuel Tank (3.5 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 3.5;
        misc.criticals = 4;
        misc.cost = 2000;
        misc.flags = misc.flags.or(F_FUEL).or(F_MECH_EQUIPMENT);
        misc.explosive = true;
        misc.industrial = true;
        misc.rulesRefs = "244,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(2300, 2350, 2490, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2300, 2350, 2490, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createFuel4() {
        MiscType misc = new MiscType();

        misc.name = "Extended Fuel Tank (4 tons)";
        misc.setInternalName(misc.name);
        misc.tonnage = 4;
        misc.criticals = 4;
        misc.cost = 2000;
        misc.flags = misc.flags.or(F_FUEL).or(F_MECH_EQUIPMENT);
        misc.explosive = true;
        misc.industrial = true;
        misc.rulesRefs = "244,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(2300, 2350, 2490, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2300, 2350, 2490, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createExternalStoresHardpoint() {
        MiscType misc = new MiscType();
        misc.tonnage = 0.2;
        misc.cost = 5000;
        misc.name = "External Stores Hardpoint";
        misc.setInternalName(misc.name);
        misc.flags = misc.flags.or(F_EXTERNAL_STORES_HARDPOINT).or(F_SUPPORT_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT);
        misc.industrial = true;
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_B);
        misc.techAdvancement.setAvailability(new int[] { RATING_D, RATING_E, RATING_D, RATING_X });
        return misc;
    }

    // Handheld Weapon - May 2017 - Under development in a separate branch

    public static MiscType createISHarJel() {
        MiscType misc = new MiscType();
        misc.name = "BattleMech HarJel System";
        misc.setInternalName("IS HarJel");
        misc.addLookupName("IS HarJel");
        misc.addLookupName("Clan HarJel");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 120000;
        misc.flags = misc.flags.or(F_HARJEL).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "288,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3115, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(3059, 3115, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CSF, F_LC)
                .setProductionFactions(F_CSF).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);;
        return misc;
    }

    public static MiscType createHarJelII() {
        MiscType misc = new MiscType();
        misc.name = "HarJel Repair Systems (HarJel II)";
        misc.setInternalName(misc.name);
        misc.addLookupName("HarJel II Self-Repair System");
        misc.tonnage = 2;
        misc.criticals = 1;
        misc.cost = 240000;
        misc.flags = misc.flags.or(F_HARJEL_II).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = -1;
        misc.setInstantModeSwitch(true);
        String[] modes = { S_HARJEL_II_1F1R, S_HARJEL_II_2F0R, S_HARJEL_II_0F2R };
        misc.setModes(modes);
        misc.rulesRefs = "88,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
                .setClanAdvancement(3120, 3136, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
        return misc;
    }

    public static MiscType createHarJelIII() {
        MiscType misc = new MiscType();
        misc.name = "HarJel Repair Systems (HarJel III)";
        misc.addLookupName("HarJel III Self-Repair System");
        misc.setInternalName(misc.name);
        misc.tonnage = 3;
        misc.criticals = 2;
        misc.cost = 360000;
        misc.flags = misc.flags.or(F_HARJEL_III).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = -2;
        misc.setInstantModeSwitch(true);
        String[] modes = { S_HARJEL_III_2F2R, S_HARJEL_III_4F0R, S_HARJEL_III_3F1R, S_HARJEL_III_1F3R,
                S_HARJEL_III_0F4R };
        misc.setModes(modes);
        misc.rulesRefs = "88,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
                .setClanAdvancement(3137, 3139, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
        return misc;
    }

    public static MiscType createISMobileFieldBase() {
        MiscType misc = new MiscType();
        misc.name = "Mobile Field Base";
        misc.setInternalName("ISMobileFieldBase");
        misc.addLookupName("CLMobileFieldBase");
        misc.tonnage = 20;
        misc.cost = 150000;
        misc.flags = misc.flags.or(F_MOBILE_FIELD_BASE).or(F_TANK_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT)
                .or(F_SS_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "330,TO";
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

        misc.name = "MASH Core Component";
        misc.setInternalName(misc.name);
        misc.tonnage = 3.5;
        misc.criticals = 1;
        misc.cost = 35000;
        misc.flags = misc.flags.or(F_TANK_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT)
                .or(F_SS_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT).or(F_MASH);
        misc.industrial = true;
        misc.rulesRefs = "228,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_B).setAvailability(RATING_C, RATING_E, RATING_D, RATING_C)
                .setISAdvancement(DATE_PS, DATE_PS).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return misc;
    }

    public static MiscType createMASHExtraTheater() {
        MiscType misc = new MiscType();

        misc.name = "MASH Operation Theater";
        misc.setInternalName(misc.name);
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.tankslots = 0;
        misc.cost = 10000;
        misc.flags = misc.flags.or(F_TANK_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT)
                .or(F_SS_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT).or(F_MASH_EXTRA);
        misc.industrial = true;
        misc.rulesRefs = "228,TM";
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
        misc.rulesRefs = "336,TO";
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
        misc.tankslots = 0;
        misc.cost = 7500;
        misc.flags = misc.flags.or(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT).or(F_MECH_EQUIPMENT);
        misc.industrial = true;
        misc.rulesRefs = "233,TM";
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
        misc.tonnage = 0.005;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.flags.or(F_SEARCHLIGHT).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_DS_EQUIPMENT).andNot(F_BA_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 500;
        misc.rulesRefs = "230,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_E, RATING_F, RATING_D, RATING_D)
                .setISAdvancement(2645, 2647, 2650, 2845, 3035).setISApproximate(false, false, true, false, false)
                .setClanAdvancement(2645, 2647, 2650, 2845, DATE_NONE)
                .setClanApproximate(false, false, true, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return misc;
    }

    public static MiscType createSearchlight() {
        MiscType misc = new MiscType();

        misc.name = "Searchlight (Mounted)";
        misc.setInternalName("Searchlight");
        misc.tonnage = 0.5;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_SEARCHLIGHT).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_FIGHTER_EQUIPMENT).or(F_DS_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 2000;
        misc.industrial = true;
        misc.rulesRefs = "237,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_B, RATING_A)
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
        misc.flags = misc.flags.or(F_TANK_EQUIPMENT).or(F_VEEDC).or(F_REUSABLE).andNot(F_MECH_EQUIPMENT)
                .andNot(F_BA_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 1000;
        misc.rulesRefs = "348,TO";
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
        misc.flags = misc.flags.or(F_TANK_EQUIPMENT).or(F_VEEDC).or(F_REUSABLE).andNot(F_MECH_EQUIPMENT)
                .andNot(F_BA_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 3000;
        misc.rulesRefs = "348,TO";
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
        misc.flags = misc.flags.or(F_TANK_EQUIPMENT).or(F_VEEDC).or(F_REUSABLE).andNot(F_MECH_EQUIPMENT)
                .andNot(F_BA_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 5000;
        misc.rulesRefs = "348,TO";
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
        misc.flags = misc.flags.or(F_TANK_EQUIPMENT).or(F_VEEDC).or(F_REUSABLE).andNot(F_MECH_EQUIPMENT)
                .andNot(F_BA_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 0;
        misc.rulesRefs = "348,TO";
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
        misc.rulesRefs = "349,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_F)
                .setISAdvancement(3070, 3085).setPrototypeFactions(F_WB)
                .setProductionFactions(F_RS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.flags = misc.flags.or(F_MAST_MOUNT).or(F_VTOL_EQUIPMENT);
        misc.bv = BV_VARIABLE;
        misc.rulesRefs = "350,TO";
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
        misc.tonnage = 0; // see IO pg 66
        misc.criticals = 1;
        misc.cost = 25000;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_MAGNETIC_CLAMP).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT)
                .andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        String[] saModes = { "On", "Off" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(true);
        misc.bv = 1;
        misc.rulesRefs = "228,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3070, 3075, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CFM)
                .setProductionFactions(F_CSF);
        return misc;
    }

    public static MiscType createProtoQuadMeleeSystem() {
        MiscType misc = new MiscType();
        // TODO Game Rules
        misc.name = "ProtoMech Quad Melee System";
        misc.setInternalName("ProtoQuadMeleeSystem");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 70000;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_PROTOMECH_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 1;
        misc.rulesRefs = "67,IO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3066, 3072, 3085, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCC)
                .setProductionFactions(F_CHH);
        return misc;
    }

    /*
     * I've done the Tech Progression for them but nothing else. TODO ProtoMech
     * Melee Weapon - IO 45 misc.rulesRefs = "370,TO";
     * misc.techAdvancement.setTechBase(TECH_BASE_CLAN) .setIntroLevel(false)
     * .setUnofficial(false) .setTechRating(RATING_F) .setAvailability(RATING_X,
     * RATING_X, RATING_E, RATING_D) .setClanAdvancement(3067, 3077, 3085,
     * DATE_NONE, DATE_NONE) .setClanApproximate(false, false, false,false, false)
     * .setPrototypeFactions(F_CLAN) .setProductionFactions(F_CLAN).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
     */

    // Mobile Hyperpulse Generators
    public static MiscType createISMobileHPG() {
        MiscType misc = new MiscType();
        // TODO Game Rules
        misc.name = "Mobile Hyperpulse Generators (Mobile HPG)";
        misc.setInternalName("ISMobileHPG");
        misc.addLookupName("ClanMobileHPG");
        misc.tonnage = 50;
        misc.criticals = 50;
        misc.cost = 1000000000;
        misc.flags = misc.flags.or(F_MOBILE_HPG).or(F_SUPPORT_TANK_EQUIPMENT)
                .or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT)
                .or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "330,TO";
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
        misc.tonnage = 12;
        misc.criticals = 12;
        misc.cost = 4000000000f;
        misc.flags = misc.flags.or(F_MOBILE_HPG).or(F_TANK_EQUIPMENT).or(F_MECH_EQUIPMENT)
                .or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT)
                .or(F_SPLITABLE);
        misc.bv = 0;
        misc.rulesRefs = "330,TO";
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
        misc.tonnage = 2;
        misc.criticals = 1;
        misc.cost = 460000;
        misc.flags = misc.flags.or(F_EMERGENCY_COOLANT_SYSTEM).or(F_MECH_EQUIPMENT);
        misc.explosive = true;
        misc.rulesRefs = "92,IO";
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
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_RISC_LASER_PULSE_MODULE).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT).or(F_VTOL_EQUIPMENT).or(F_PROTOMECH_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.explosive = true;
        misc.rulesRefs = "93,IO";
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

        misc.name = "Super-Cooled Myomer";
        misc.setInternalName("ISSuperCooledMyomer");
        misc.tonnage = 0;
        misc.criticals = 6;
        misc.hittable = true;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_SCM).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        // TODO: add game rules, BV rules are implemented
        misc.rulesRefs = "92,IO";
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
        misc.flags = misc.flags.or(F_VIRAL_JAMMER_DECOY).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_PROTOMECH_EQUIPMENT);
        // TODO: game rules
        misc.rulesRefs = "92,IO";
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
        misc.flags = misc.flags.or(F_VIRAL_JAMMER_HOMING).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_PROTOMECH_EQUIPMENT);
        // TODO: game rules
        misc.rulesRefs = "92,IO";
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
        misc.flags = misc.flags.or(F_PPC_CAPACITOR).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT)
                .or(F_FIGHTER_EQUIPMENT).or(F_SC_EQUIPMENT).or(F_DS_EQUIPMENT).or(F_JS_EQUIPMENT).or(F_WS_EQUIPMENT).or(F_SS_EQUIPMENT);
        misc.setInstantModeSwitch(false);
        misc.explosive = true;
        // misc.bv = 88;
        misc.bv = 0;
        misc.rulesRefs = "337,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3060, 3081).setClanAdvancement(DATE_NONE, 3101)
                .setPrototypeFactions(F_DC).setProductionFactions(F_DC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.rulesRefs = "224,TM";
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
        misc.rulesRefs = "71,IO";
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
        misc.rulesRefs = "224,TM";
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
        misc.rulesRefs = "342,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3061, 3082).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createEndoComposite() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_COMPOSITE);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_COMPOSITE));
        misc.addLookupName("IS Endo-Composite");
        misc.addLookupName("Clan Endo-Composite");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_ENDO_COMPOSITE);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "342,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3067, 3085).setClanAdvancement(3073)
                .setISApproximate(false, true).setPrototypeFactions(F_LC, F_CWX)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.rulesRefs = "342,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3057, 3084).setISApproximate(false, true, false)
                .setClanAdvancement(3065, 3084).setClanApproximate(false, true, false)
                .setPrototypeFactions(F_CS, F_CGB).setProductionFactions(F_CGB)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.rulesRefs = "224,TM";
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
        misc.rulesRefs = "279,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3070, 3108, 3109, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(3070, 3108, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_MERC)
                .setProductionFactions(F_RD).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    // Musculature
    public static MiscType createTSM() {
        MiscType misc = new MiscType();

        misc.name = "Triple Strength Myomer";
        misc.setInternalName(misc.name);
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
        misc.rulesRefs = "240,TM";
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
        misc.setInternalName(misc.name);
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
        misc.rulesRefs = "240,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3035, 3045, 3055, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
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
        misc.name = "Combat Vehicle Chassis Mod [Flotation Hull]";
        misc.setInternalName("ISFlotationHull");
        misc.addLookupName("ClanFlotationHull");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.cost = EquipmentType.COST_VARIABLE;
        misc.flags = misc.flags.or(F_FLOTATION_HULL).or(F_TANK_EQUIPMENT).or(F_VTOL_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.rulesRefs = "302,TO";
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
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_LIMITED_AMPHIBIOUS).or(F_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "302,TO";
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
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_FULLY_AMPHIBIOUS).or(F_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "302,TO";
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
        // TODO this is Combat Vee, and SV combined chassis. Their really needs
        // to be two different chassis types. 
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
        misc.rulesRefs = "303,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(2470, 2471, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2470, 2471, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_TH);
        return misc;
    }

    public static MiscType createEnvironmentalSealedChassis() {
        MiscType misc = new MiscType();

        misc.name = "Combat Vehicle Chassis Mod [Environmental Sealing]";
        misc.setInternalName("Environmental Sealed Chassis");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_ENVIRONMENTAL_SEALING).or(F_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "303,TO";
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
        misc.setInternalName("AmphibiousChassis");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_AMPHIBIOUS).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122,TM";

        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_C);
        misc.techAdvancement.setAvailability(new int[] { RATING_C, RATING_D, RATING_C, RATING_C });
        return misc;
    }

    public static MiscType createArmoredChassis() {
        MiscType misc = new MiscType();

        misc.name = "SV Chassis Mod [Armored Chassis]";
        misc.setInternalName("Armored Chassis");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_ARMORED_CHASSIS).or(F_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122,TM";

        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_C, RATING_E, RATING_D, RATING_D });
        return misc;
    }

    public static MiscType createBicycleModification() {
        MiscType misc = new MiscType();

        misc.name = "SV Chassis Mod [Bicycle]";
        misc.setInternalName("BicycleChassisMod");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_BICYCLE).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122,TM";
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
        misc.setInternalName("ConvertibleChassisMod");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_CONVERTIBLE).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122,TM";
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
        // TODO this is Combat Vee, and SV combined chassis. Their really needs
        // to be two different chassis types. 
        misc.name = "SV Chassis Mod [Dune Buggy]";
        misc.setInternalName("ISSVDuneBuggyChassis");
        misc.addLookupName("ISSVDuneBuggy");
        misc.addLookupName("ClanSVDuneBuggyChassis");
        misc.addLookupName("ClanSVDuneBuggy");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_DUNE_BUGGY).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);               
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "303,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(2470, 2471, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2470, 2471, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_TH);
        return misc;
    }

    public static MiscType createEnvironmentalSealing() {
        MiscType misc = new MiscType();
        // TODO - Review how this impacts the chassis stuff. See
        // IO pg 48 and search in here for IndustrialMech Structure w/
        // Environmental Sealing. Also likely needs to have a SV version split
        // out from it.
        misc.name = "Environmental Sealing";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 8;
        misc.tankslots = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.spreadable = true;
        misc.flags = misc.flags.or(F_ENVIRONMENTAL_SEALING).or(F_MECH_EQUIPMENT).or(F_TANK_EQUIPMENT)
                .or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;

        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_C);
        misc.techAdvancement.setAvailability(new int[] { RATING_B, RATING_D, RATING_C, RATING_C });
        return misc;
    }

    public static MiscType createHydroFoilChassisModification() {
        MiscType misc = new MiscType();

        misc.name = "SV Chassis Mod [HydroFoil]";
        misc.setInternalName("HydroFoilChassisMod");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_HYDROFOIL).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122,TM";
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
        misc.setInternalName("MonocycleChassisMod");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_MONOCYCLE).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122,TM";
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
        misc.setInternalName("ISOffRoadChassis");
        misc.addLookupName("ISOffRoad");
        misc.addLookupName("ClanOffRoadChassis");
        misc.addLookupName("CLOffRoad");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_OFF_ROAD).or(F_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION)
                .or(F_SUPPORT_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122,TM";

        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_B);
        misc.techAdvancement.setAvailability(new int[] { RATING_B, RATING_C, RATING_B, RATING_B });
        return misc;
    }

    public static MiscType createPropChassisModification() {
        MiscType misc = new MiscType();
        misc.name = "SV Chassis Mod [Propeller-Driven]";
        misc.setInternalName("PropChassisMod");
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.tankslots = 0;
        misc.flags = misc.flags.andNot(F_FIGHTER_EQUIPMENT).or(F_CHASSIS_MODIFICATION).or(F_PROP);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "122,TM";
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
        misc.setInternalName("SnowmobileChassis");
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_SNOWMOBILE).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122,TM";
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
        misc.setInternalName("STOLChassisMod");
        misc.tonnage = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_STOL_CHASSIS).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122,TM";
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
        misc.setInternalName("SubmersibleChassisMod");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_SUBMERSIBLE).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.tankslots = 0;
        misc.industrial = true;
        misc.rulesRefs = "122,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_B);
        misc.techAdvancement.setAvailability(new int[] { RATING_C, RATING_D, RATING_C, RATING_C });
        return misc;
    }

    public static MiscType createTractorModification() {
        MiscType misc = new MiscType();

        misc.name = "SV Chassis Mod [Tractor]";
        misc.setInternalName(misc.name);
        misc.addLookupName("Tractor");
        misc.tonnage = 0; // accounted as part of the unit Construction
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_TRACTOR_MODIFICATION).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122,TM";
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
        misc.setInternalName("Hitch");
        misc.tonnage = 0;
        misc.cost = 0;
        misc.criticals = 1; // not list in a chart but TM pg 98 mentions they
                            // take 1 item slot.
        misc.tankslots = 1;
        misc.flags = misc.flags.or(F_HITCH).or(F_TANK_EQUIPMENT);
        misc.bv = 0;
        misc.industrial = true;
        misc.rulesRefs = "101,TM";

        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_PS, DATE_PS, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createTrailerModification() {
        MiscType misc = new MiscType();

        misc.name = "SV Chassis Mod [Trailer]";
        misc.setInternalName(misc.name);
        misc.addLookupName("Trailer");
        misc.tonnage = 0; // accounted as part of the unit Construction
        misc.criticals = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_TRAILER_MODIFICATION).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.tankslots = 0;
        misc.industrial = true;
        misc.rulesRefs = "122,TM";

        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_A);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_A });
        return misc;
    }

    public static MiscType createUltraLightChassisModification() {
        MiscType misc = new MiscType();
        misc.name = "SV Chassis Mod [Ultra-Light]";
        misc.setInternalName("UltraLightChassisMod");
        misc.tankslots = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION).or(F_ULTRA_LIGHT);
        misc.omniFixedOnly = true;
        misc.rulesRefs = "122,TM";
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
        misc.setInternalName("VSTOLChassisMod");
        misc.tonnage = 0;
        misc.cost = 0; // Cost accounted as part of unit cost
        misc.flags = misc.flags.or(F_VSTOL_CHASSIS).or(F_SUPPORT_TANK_EQUIPMENT).or(F_CHASSIS_MODIFICATION);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "122,TM";
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
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_SHOULDER_TURRET).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "347,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_C).setAvailability(RATING_F, RATING_X, RATING_F, RATING_E)
                .setAdvancement(2450, 3082).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createISHeadTurret() {
        MiscType misc = new MiscType();
        misc.name = "BattleMech Turret (Head)";
        misc.setInternalName("ISHeadTurret");
        misc.addLookupName("CLHeadTurret");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_HEAD_TURRET).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "347,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3055, 3082, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return misc;
    }

    public static MiscType createISQuadTurret() {
        MiscType misc = new MiscType();
        misc.name = "BattleMech Turret (Quad)";
        misc.setInternalName("ISQuadTurret");
        misc.addLookupName("CLQuadTurret");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_QUAD_TURRET).or(F_MECH_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "347,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_F, RATING_F, RATING_E)
                .setISAdvancement(2320, 3079, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    /*
     * //Vehicular Dual Turret - IO pg 50 and TO pg 347/411 We support them but
     * their is no construction data for them.
     */
    public static MiscType createISSponsonTurret() {
        MiscType misc = new MiscType();
        misc.name = "Vehicular Sponson Turret";
        misc.setInternalName("ISSponsonTurret");
        misc.addLookupName("CLSponsonTurret");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.hittable = false;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_SPONSON_TURRET).or(F_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "348,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_B).setAvailability(RATING_F, RATING_F, RATING_F, RATING_D)
                .setAdvancement(DATE_PS, 3079, 3080, DATE_NONE, DATE_NONE)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return misc;
    }

    public static MiscType createPintleTurret() {
        MiscType misc = new MiscType();
        misc.name = "Pintle Mount";
        misc.setInternalName("PintleTurret");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.tankslots = 0;
        misc.hittable = false;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_PINTLE_TURRET).or(F_SUPPORT_TANK_EQUIPMENT);
        misc.omniFixedOnly = true;
        misc.bv = 0;
        misc.rulesRefs = "348,TO";
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
        misc.rulesRefs = "252,TM";
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
        misc.rulesRefs = "279,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3063, 3080, 3097, DATE_NONE, DATE_NONE)
                .setISApproximate(true, true, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

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
        misc.rulesRefs = "279,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3058, 3080, 3097, DATE_NONE, DATE_NONE)
                .setISApproximate(true, true, false, false, false).setPrototypeFactions(F_CNC)
                .setProductionFactions(F_CNC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
        misc.rulesRefs = "253,TM";
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
        misc.rulesRefs = "254,TM";
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
        misc.setInternalName("ECM Suite (Light)");
        misc.addLookupName("IS BA ECM");
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
        misc.rulesRefs = "254,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_F, RATING_E).setISAdvancement(2718, 2720, 3060, 2766, 3057)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FW, F_WB);
        return misc;
    }

    public static MiscType createCLSingleHexECM() {
        MiscType misc = new MiscType();

        misc.name = BattleArmor.SINGLE_HEX_ECM;
        misc.setInternalName("ECM Suite (Light)");
        misc.addLookupName("CL BA ECM");
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
        misc.rulesRefs = "254,TM";
        misc.rulesRefs = "254,TM";
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
        misc.setInternalName("ISBAExtendedLifeSupport");
        misc.addLookupName("CLBAExtendedLifeSupport");
        misc.cost = 10000;
        misc.tonnage = 0.025;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_EXTENDED_LIFESUPPORT).andNot(F_MECH_EQUIPMENT)
                .andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "254,TM";
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
        misc.rulesRefs = "255,TM";
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
        misc.rulesRefs = "256,TM";
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
        misc.rulesRefs = "257,TM";
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
        misc.rulesRefs = "257,TM";
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
        misc.rulesRefs = "258,TM";
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
        misc.rulesRefs = "266,TM";
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
        misc.rulesRefs = "268,TM";
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
        misc.rulesRefs = "268,TM";
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
        misc.tonnage = 0.005;
        misc.criticals = 1;
        misc.tankslots = 0;
        misc.flags = misc.flags.or(F_BA_SEARCHLIGHT).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT)
                .andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 500;
        misc.rulesRefs = "269,TM";
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
        misc.rulesRefs = "258,TM";
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
        misc.rulesRefs = "269,TM";
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
        misc.tonnage = 0.0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_ARMORED_GLOVE).or(F_AP_MOUNT).or(F_BA_MANIPULATOR);
        misc.bv = 0;
        misc.cost = 2500;
        misc.rulesRefs = "259,TM";
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
        misc.tonnage = 0.0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BA_MANIPULATOR).or(F_BASIC_MANIPULATOR);
        misc.bv = 0;
        misc.cost = 5000;
        misc.rulesRefs = "259,TM";
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
        misc.tonnage = 0.015;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BATTLE_CLAW).or(F_BA_MANIPULATOR);
        misc.bv = 1;
        misc.cost = 10000;
        misc.rulesRefs = "259,TM";
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
        misc.tonnage = 0.020;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BATTLE_CLAW).or(F_BA_MANIPULATOR);
        misc.bv = 0;
        misc.cost = 25000;
        misc.rulesRefs = "259,TM";
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
        misc.tonnage = 0.03;
        misc.criticals = 0;
        misc.cost = 250;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_CARGOLIFTER).or(F_BA_MANIPULATOR);
        misc.bv = 0;
        misc.cost = 500;
        misc.rulesRefs = "259,TM";
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
        misc.tonnage = 0.030;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BA_MANIPULATOR);
        misc.bv = 0;
        misc.cost = 2500;
        misc.rulesRefs = "259,TM";
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
        misc.tonnage = 0.030;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BA_MANIPULATOR);
        misc.bv = 0;
        misc.cost = 50000;
        misc.rulesRefs = "259,TM";
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
        misc.tonnage = 0.035;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_MAGNET_CLAW).or(F_BA_EQUIPMENT).or(F_BATTLE_CLAW).or(F_BA_MANIPULATOR);
        misc.bv = 1.5;
        misc.cost = 12500;
        misc.rulesRefs = "260,TM";
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
        misc.addLookupName("Heavy Battle Claw (w/ Magnets)");
        misc.tonnage = 0.040;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_MAGNET_CLAW).or(F_BA_EQUIPMENT).or(F_BATTLE_CLAW).or(F_BA_MANIPULATOR);
        misc.bv = 1.5;
        misc.cost = 31250;
        misc.rulesRefs = "260,TM";
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
        misc.tonnage = 0.015;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_TOOLS).or(F_BASIC_MANIPULATOR).or(F_BA_MANIPULATOR);
        misc.subType |= S_MINESWEEPER;
        misc.bv = 0;
        misc.cost = 7500;
        misc.rulesRefs = "260,TM";
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
        misc.tonnage = 0.050;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_VIBROCLAW).or(F_BA_EQUIPMENT).or(F_BATTLE_CLAW).or(F_BA_MANIPULATOR);
        misc.bv = 1;
        misc.cost = 15000;
        misc.rulesRefs = "260,TM";
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
        misc.tonnage = 0.060;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_VIBROCLAW).or(F_BA_EQUIPMENT).or(F_BATTLE_CLAW).or(F_BA_MANIPULATOR);
        misc.bv = 1;
        misc.cost = 30000;
        misc.rulesRefs = "260,TM";
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
        misc.tonnage = 0.01;
        misc.criticals = 2;
        misc.hittable = false;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BA_MEA);
        misc.bv = 0;
        misc.cost = 10000;
        misc.rulesRefs = "260,TM";
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
        misc.setInternalName("BAJumpJet");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_JUMP_JET).or(F_BA_EQUIPMENT);
        misc.rulesRefs = "257,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setTechRating(RATING_C).setAvailability(RATING_D, RATING_D, RATING_D, RATING_C)
                .setAdvancement(DATE_ES, DATE_ES, DATE_ES);
        return misc;
    }

    public static MiscType createBAVTOLEquipment() {
        MiscType misc = new MiscType();

        misc.name = "VTOL [BA]";
        misc.setInternalName("BAVTOL");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT);
        misc.rulesRefs = "271,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3052, 3060, 3066).setClanApproximate(true, false, false)
                .setPrototypeFactions(F_CCC).setProductionFactions(F_CCC);
        return misc;
    }

    public static MiscType createBAUMU() {
        MiscType misc = new MiscType();

        misc.name = "UMU [BA]";
        misc.setInternalName("BAUMU");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_UMU).or(F_BA_EQUIPMENT);
        misc.rulesRefs = "270,TM";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_E, RATING_E, RATING_E)
                .setClanAdvancement(2840, 3059, 3065).setClanApproximate(true, false, false)
                .setPrototypeFactions(F_CGS).setProductionFactions(F_CGS);
        return misc;
    }

    public static MiscType createISBAJumpBooster() {
        MiscType misc = new MiscType();

        misc.name = "Jump Booster [BA]";
        misc.setInternalName("BAJumpBooster");
        misc.addLookupName("ISBAJumpBooster");
        misc.addLookupName("CLBAJumpBooster");
        misc.tonnage = 0.125;
        misc.criticals = 2;
        misc.cost = 75000;
        misc.flags = misc.flags.or(F_JUMP_BOOSTER).or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "257,TM";
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
        misc.rulesRefs = "259,TM";
        misc.rulesRefs = "CHECK";
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
        misc.setInternalName("BAMechanicalJumpBooster");
        misc.addLookupName("ISMechanicalJumpBooster");
        misc.addLookupName("CLMechanicalJumpBooster");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags = misc.flags.or(F_MECHANICAL_JUMP_BOOSTER).or(F_BA_EQUIPMENT);
        misc.rulesRefs = "286,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3070, 3084, 3096, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3090, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);;
        return misc;
    }

    public static MiscType createCLBAMyomerBooster() {
        MiscType misc = new MiscType();

        misc.name = "BA Myomer Booster";
        misc.setInternalName("CLBAMyomerBooster");
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
        misc.rulesRefs = "287,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3072, 3085, 3104, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CIH)
                .setProductionFactions(F_CIH).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);;
        return misc;
    }

    public static MiscType createBAPartialWing() {
        MiscType misc = new MiscType();

        misc.name = "Partial Wing [BA]";
        misc.setInternalName("BAPartialWing");
        misc.tonnage = 0.2;
        misc.criticals = 1;
        misc.cost = 50000;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_PARTIAL_WING);
        misc.rulesRefs = "266,TM";
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
        misc.name = "BattleArmor DropChute(Standard)";
        misc.setInternalName("ISBADropChuteStd");
        misc.tonnage = 0.2;
        misc.criticals = 0;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_BADC).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 1000;
        misc.rulesRefs = "348,TO";
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
        misc.rulesRefs = "348,TO";
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
        misc.rulesRefs = "348,TO";
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
        misc.rulesRefs = "348,TO";
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
        misc.rulesRefs = "271,TM";
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
        misc.rulesRefs = "287,TO";
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).or(F_DETACHABLE_WEAPON_PACK).andNot(F_MECH_EQUIPMENT)
                .andNot(F_TANK_EQUIPMENT).andNot(F_FIGHTER_EQUIPMENT);
        misc.rulesRefs = "287,TO";
        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3073, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(3070, 3072, 3080, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH).setStaticTechLevel(SimpleTechLevel.ADVANCED);;
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
        misc.rulesRefs = "260,TM";
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
        misc.setInternalName(misc.name);
        misc.tonnage = 0.02;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_BA_EQUIPMENT).andNot(F_MECH_EQUIPMENT).andNot(F_TANK_EQUIPMENT)
                .andNot(F_FIGHTER_EQUIPMENT);
        misc.bv = 0;
        misc.cost = 750;

        misc.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, 2720);
        misc.techAdvancement.setTechRating(RATING_C);
        misc.techAdvancement.setAvailability(new int[] { RATING_C, RATING_C, RATING_C, RATING_C });
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
        misc.setInternalName(misc.name);
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
        misc.setInternalName(misc.name);
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

    // Unofficial or Made up Equipment
    // No clue what this no unit is using this

    /*
     * public static MiscType createCoolantSystem() { MiscType misc = new
     * MiscType();
     * 
     * misc.name = "Coolant System"; misc.setInternalName(misc.name); misc.tonnage =
     * 9; misc.criticals = 2; misc.cost = 90000; misc.flags =
     * misc.flags.or(F_COOLANT_SYSTEM).or(F_MECH_EQUIPMENT); misc.bv = 15;
     * misc.techAdvancement.setTechBase(TECH_BASE_IS);
     * misc.techAdvancement.setISAdvancement(DATE_NONE, 3049, DATE_NONE);
     * misc.techAdvancement.setTechRating(RATING_C);
     * misc.techAdvancement.setAvailability(new int[] { RATING_X, RATING_X,
     * RATING_E, RATING_X }); return misc; }
     */

    public static MiscType createVacuumProtection() {
        MiscType misc = new MiscType();

        misc.name = "Vacuum Protection";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = 0;
        misc.flags = misc.flags.or(F_VACUUM_PROTECTION).or(F_TANK_EQUIPMENT).or(F_SUPPORT_TANK_EQUIPMENT);
        ;
        misc.bv = 0;
        // TODO - Should be part of Environmental Sealing.
        misc.techAdvancement.setTechBase(TECH_BASE_ALL);
        misc.techAdvancement.setUnofficial(true);
        misc.techAdvancement.setAdvancement(DATE_NONE, DATE_NONE, DATE_PS);
        misc.techAdvancement.setTechRating(RATING_C);
        misc.techAdvancement.setAvailability(new int[] { RATING_A, RATING_A, RATING_A, RATING_X });
        return misc;
    }

    public static MiscType createLAMBombBay() {
        MiscType misc = new MiscType();
        misc.name = "Bomb Bay";
        misc.setInternalName(misc.name);
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.flags = misc.flags.or(F_BOMB_BAY).or(F_MECH_EQUIPMENT);
        misc.explosive = true;

        misc.techAdvancement.setTechBase(TECH_BASE_IS);
        misc.techAdvancement.setISAdvancement(DATE_NONE, 3071, DATE_NONE);
        misc.techAdvancement.setTechRating(RATING_C);
        misc.techAdvancement.setAvailability(new int[] { RATING_E, RATING_E, RATING_E, RATING_E });
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
