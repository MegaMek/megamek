/*
 * MegaMek
 * Copyright (C) 2019 The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package megamek.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Constants that can be used as lookup keys for {@link EquipmentType#get}. The constant should be used
 * as the internal name when creating the {@link EquipmentType}.
 *
 * This is not a complete list, as most equipment does not need to be retrieved directly.
 * It is primarily for use by the construction rules when a specific piece of equipment is required.
 * Armor and structure have their own methods that take tech base into account.
 * @see EquipmentType#getArmorTypeName
 * @see EquipmentType#getStructureTypeName
 */
public class EquipmentTypeLookup {

    /**
     * Static fields in this class annotated with {code }@EquipmentName} will be checked by the unit tests
     * to verify they are valid {@link EquipmentType} lookup keys.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface EquipmentName{}

    @EquipmentName public static final String JUMP_JET = "Jump Jet";
    @EquipmentName public static final String IMPROVED_JUMP_JET = "Improved Jump Jet";
    @EquipmentName public static final String PROTOTYPE_JUMP_JET = "ISPrototypeJumpJet";
    @EquipmentName public static final String PROTOTYPE_IMPROVED_JJ = "ISPrototypeImprovedJumpJet";
    @EquipmentName public static final String MECH_UMU = "UMU";
    @EquipmentName public static final String MECH_JUMP_BOOSTER = "MechanicalJumpBooster";
    @EquipmentName public static final String VEHICLE_JUMP_JET = "VehicleJumpJet";
    @EquipmentName public static final String PROTOMECH_JUMP_JET = "ProtomechJumpJet";
    @EquipmentName public static final String EXTENDED_JUMP_JET_SYSTEM = "ExtendedJumpJetSystem";
    @EquipmentName public static final String PROTOMECH_UMU = "ProtomechUMU";
    @EquipmentName public static final String PROTOMECH_MYOMER_BOOSTER = "CLMyomerBooster";
    @EquipmentName public static final String BA_JUMP_JET = "BAJumpJet";
    @EquipmentName public static final String BA_VTOL = "BAVTOL";
    @EquipmentName public static final String BA_UMU = "BAUMU";
    @EquipmentName public static final String BA_MYOMER_BOOSTER = "CLBAMyomerBooster";
    @EquipmentName public static final String BA_PARTIAL_WING = "BAPartialWing";
    @EquipmentName public static final String BA_JUMP_BOOSTER = "BAJumpBooster";
    @EquipmentName public static final String BA_MECHANICAL_JUMP_BOOSTER = "BAMechanicalJumpBooster";
    @EquipmentName public static final String BA_MANIPULATOR_CARGO_LIFTER = "BACargoLifter";

    @EquipmentName public static final String SINGLE_HS = "Heat Sink";
    @EquipmentName public static final String IS_DOUBLE_HS = "ISDoubleHeatSink";
    @EquipmentName public static final String CLAN_DOUBLE_HS = "CLDoubleHeatSink";
    @EquipmentName public static final String LASER_HS = "Laser Heat Sink";
    @EquipmentName public static final String COMPACT_HS_1 = "1 Compact Heat Sink";
    @EquipmentName public static final String COMPACT_HS_2 = "2 Compact Heat Sinks";
    @EquipmentName public static final String IS_DOUBLE_HS_PROTOTYPE = "ISDoubleHeatSinkPrototype";
    @EquipmentName public static final String IS_DOUBLE_HS_FREEZER = "ISDoubleHeatSinkFreezer";

    @EquipmentName public static final String HITCH = "Hitch";
    @EquipmentName public static final String CLAN_CASE = "CLCASE";
    @EquipmentName public static final String COOLANT_POD = "Coolant Pod";
    @EquipmentName public static final String MECH_TRACKS = "Tracks";
    @EquipmentName public static final String QUADVEE_WHEELS = "QuadVee Wheels";
    @EquipmentName public static final String IM_EJECTION_SEAT = "Ejection Seat (Industrial Mech)";
    @EquipmentName public static final String TSM = "Triple Strength Myomer";
    @EquipmentName public static final String SCM = "ISSuperCooledMyomer";
    @EquipmentName public static final String ITSM = "Industrial Triple Strength Myomer";
    @EquipmentName public static final String P_TSM = "Prototype TSM";
    @EquipmentName public static final String IS_MASC = "ISMASC";
    @EquipmentName public static final String CLAN_MASC = "CLMASC";
    @EquipmentName public static final String SPONSON_TURRET = "SponsonTurret";
    @EquipmentName public static final String PINTLE_TURRET = "PintleTurret";

    @EquipmentName public static final String AMPHIBIOUS_CHASSIS_MOD = "AmphibiousChassisMod";
    @EquipmentName public static final String ARMORED_CHASSIS_MOD = "ArmoredChassisMod";
    @EquipmentName public static final String BICYCLE_CHASSIS_MOD = "BicycleChassisMod";
    @EquipmentName public static final String CONVERTIBLE_CHASSIS_MOD = "ConvertibleChassisMod";
    @EquipmentName public static final String DUNE_BUGGY_CHASSIS_MOD = "DuneBuggyChassisMod";
    @EquipmentName public static final String SV_ENVIRONMENTAL_SEALING_CHASSIS_MOD = "Environmental Sealing";
    @EquipmentName public static final String EXTERNAL_POWER_PICKUP_CHASSIS_MOD = "ExternalPowerPickupChassisMod";
    @EquipmentName public static final String HYDROFOIL_CHASSIS_MOD = "HydrofoilChassisMod";
    @EquipmentName public static final String MONOCYCLE_CHASSIS_MOD = "MonocycleChassisMod";
    @EquipmentName public static final String OFFROAD_CHASSIS_MOD = "OffroadChassisMod";
    @EquipmentName public static final String OMNI_CHASSIS_MOD = "OmniChassisMod";
    @EquipmentName public static final String PROP_CHASSIS_MOD = "PropChassisMod";
    @EquipmentName public static final String SNOWMOBILE_CHASSIS_MOD = "SnowmobileChassisMod";
    @EquipmentName public static final String STOL_CHASSIS_MOD = "STOLChassisMod";
    @EquipmentName public static final String SUBMERSIBLE_CHASSIS_MOD = "SubmersibleChassisMod";
    @EquipmentName public static final String TRACTOR_CHASSIS_MOD = "TractorChassisMod";
    @EquipmentName public static final String TRAILER_CHASSIS_MOD = "TrailerChassisMod";
    @EquipmentName public static final String ULTRALIGHT_CHASSIS_MOD = "UltraLightChassisMod";
    @EquipmentName public static final String VSTOL_CHASSIS_MOD = "VSTOLChassisMod";
    
    @EquipmentName public static final String LIMB_CLUB = "Limb Club";
    @EquipmentName public static final String GIRDER_CLUB = "Girder Club";
    @EquipmentName public static final String TREE_CLUB = "Tree Club";

    @EquipmentName public static final String INFANTRY_ASSAULT_RIFLE = "InfantryAssaultRifle";
    @EquipmentName public static final String INFANTRY_TAG = "InfantryTAG";
    @EquipmentName public static final String VIBRO_SHOVEL = "Vibro-Shovel";
    @EquipmentName public static final String DEMOLITION_CHARGE = "Demolition Charge";
    @EquipmentName public static final String INFANTRY_AMMO = "Infantry Ammo";
    @EquipmentName public static final String INFANTRY_INFERNO_AMMO = "Infantry Inferno Ammo";

    @EquipmentName public static final String AC_BAY = "AC Bay";
    @EquipmentName public static final String AMS_BAY = "AMS Bay";
    @EquipmentName public static final String ARTILLERY_BAY = "Artillery Bay";
    @EquipmentName public static final String AR10_BAY = "AR10 Bay";
    @EquipmentName public static final String ATM_BAY = "ATM Bay";
    @EquipmentName public static final String CAPITAL_AC_BAY = "Capital AC Bay";
    @EquipmentName public static final String CAPITAL_GAUSS_BAY = "Capital Gauss Bay";
    @EquipmentName public static final String CAPITAL_LASER_BAY = "Capital Laser Bay";
    @EquipmentName public static final String CAPITAL_MASS_DRIVER_BAY = "Capital Mass Driver Bay";
    @EquipmentName public static final String CAPITAL_MISSILE_BAY = "Capital Missile Bay";
    @EquipmentName public static final String CAPITAL_PPC_BAY = "Capital PPC Bay";
    @EquipmentName public static final String GAUSS_BAY = "Gauss Bay";
    @EquipmentName public static final String LASER_BAY = "Laser Bay";
    @EquipmentName public static final String LBX_AC_BAY = "LBX AC Bay";
    @EquipmentName public static final String LRM_BAY = "LRM Bay";
    @EquipmentName public static final String MISC_BAY = "Misc Bay";
    @EquipmentName public static final String MML_BAY = "MML Bay";
    @EquipmentName public static final String MRM_BAY = "MRM Bay";
    @EquipmentName public static final String PLASMA_BAY = "Plasma Bay";
    @EquipmentName public static final String POINT_DEFENSE_BAY = "Point Defense Bay";
    @EquipmentName public static final String PPC_BAY = "PPC Bay";
    @EquipmentName public static final String PULSE_LASER_BAY = "Pulse Laser Bay";
    @EquipmentName public static final String ROCKET_LAUNCHER_BAY = "Rocket Launcher Bay";
    @EquipmentName public static final String SCC_BAY = "Sub-Capital Cannon Bay";
    @EquipmentName public static final String SCL_BAY = "Sub-Capital Laser Bay";
    @EquipmentName public static final String SC_MISSILE_BAY = "Sub-Capital Missile Bay";
    @EquipmentName public static final String SCREEN_LAUNCHER_BAY = "Screen Launcher Bay";
    @EquipmentName public static final String SRM_BAY = "SRM Bay";
    @EquipmentName public static final String TELE_CAPITAL_MISSILE_BAY = "Tele-Operated Capital Missile Bay";
    @EquipmentName public static final String THUNDERBOLT_BAY = "Thunderbolt Bay";
    
    @EquipmentName public static final String BACKHOE = "Backhoe";
    @EquipmentName public static final String LIGHT_BRIDGE_LAYER = "LightBridgeLayer";
    @EquipmentName public static final String MEDIUM_BRIDGE_LAYER = "MediumBridgeLayer";
    @EquipmentName public static final String HEAVY_BRIDGE_LAYER = "HeavyBridgeLayer";
    @EquipmentName public static final String BULLDOZER = "Bulldozer";
    @EquipmentName public static final String CHAINSAW = "Chainsaw";
    @EquipmentName public static final String COMBINE = "Combine";
    @EquipmentName public static final String DUAL_SAW = "Dual Saw";
    @EquipmentName public static final String DUMPER_FRONT = "Dumper (Front)";
    @EquipmentName public static final String DUMPER_REAR = "Dumper (Rear)";
    @EquipmentName public static final String DUMPER_RIGHT = "Dumper (Right)";
    @EquipmentName public static final String DUMPER_LEFT = "Dumper (Left)";
    @EquipmentName public static final String EXTENDED_FUEL_TANK = "Extended Fuel Tank";
    @EquipmentName public static final String PILE_DRIVER = "Heavy-Duty Pile Driver";
    @EquipmentName public static final String LADDER = "Ladder";
    @EquipmentName public static final String LIFT_HOIST = "Lift Hoist/Arresting Hoist";
    @EquipmentName public static final String MANIPULATOR_INDUSTRIAL = "Manipulator (Non-Mech/Non-Battle Armor)";
    @EquipmentName public static final String MINING_DRILL = "MiningDrill";
    @EquipmentName public static final String NAIL_RIVET_GUN = "Nail/Rivet Gun";
    @EquipmentName public static final String REFUELING_DROGUE = "RefuelingDrogue";
    @EquipmentName public static final String FLUID_SUCTION_LIGHT_MEK = "Fluid Suction System (Light - Mech)";
    @EquipmentName public static final String FLUID_SUCTION_LIGHT_VEE = "Fluid Suction System (Light - Vehicle)";
    @EquipmentName public static final String FLUID_SUCTION = "Fluid Suction System (Standard)";
    @EquipmentName public static final String ROCK_CUTTER = "Rock Cutter";
    @EquipmentName public static final String SALVAGE_ARM = "Salvage Arm";
    @EquipmentName public static final String SPOT_WELDER = "Spot Welder";
    @EquipmentName public static final String SPRAYER_MEK = "MechSprayer";
    @EquipmentName public static final String SPRAYER_VEE = "Tank Sprayer";
    @EquipmentName public static final String WRECKING_BALL = "IS Wrecking Ball";
    @EquipmentName public static final String LAM_FUEL_TANK = "LAM Fuel Tank";
    @EquipmentName public static final String LAM_BOMB_BAY = "Bomb Bay";
    @EquipmentName public static final String CARGO = "Cargo";

    @EquipmentName public static final String CL_BA_MORTAR_HEAVY = "CLBAHeavyMortar";
    @EquipmentName public static final String CL_BA_MORTAR_LIGHT = "CLBALightMortar";
    @EquipmentName public static final String IS_BA_TUBE_ARTY = "ISBATubeArtillery";
}
