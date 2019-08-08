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

    public static final String VEHICLE_JUMP_JET = "VehicleJumpJet";
    public static final String PROTOMECH_JUMP_JET = "ProtomechJumpJet";
    public static final String EXTENDED_JUMP_JET_SYSTEM = "ExtendedJumpJetSystem";
    public static final String PROTOMECH_UMU = "ProtomechUMU";
    public static final String PROTOMECH_MYOMER_BOOSTER = "CLMyomerBooster";

    public static final String BA_MYOMER_BOOSTER = "CLBAMyomerBooster";
    public static final String BA_PARTIAL_WING = "BAPartialWing";
    public static final String BA_JUMP_BOOSTER = "BAJumpBooster";
    public static final String BA_MECHANICAL_JUMP_BOOSTER = "BAMechanicalJumpBooster";

    public static final String HITCH = "Hitch";
    public static final String CLAN_CASE = "CLCASE";
    public static final String COOLANT_POD = "Coolant Pod";
    public static final String MECH_TRACKS = "Tracks";
    public static final String QUADVEE_WHEELS = "QuadVee Wheels";
    public static final String IM_EJECTION_SEAT = "Ejection Seat (Industrial Mech)";
    public static final String TSM = "Triple Strength Myomer";
    public static final String ITSM = "Industrial Triple Strength Myomer";
    public static final String SPONSON_TURRET = "SponsonTurret";
    public static final String PINTLE_TURRET = "PintleTurret";

    public static final String LIMB_CLUB = "Limb Club";
    public static final String GIRDER_CLUB = "Girder Club";
    public static final String TREE_CLUB = "Tree Club";

    public static final String INFANTRY_ASSAULT_RIFLE = "InfantryAssaultRifle";
    public static final String INFANTRY_TAG = "InfantryTAG";
    public static final String VIBRO_SHOVEL = "Vibro-Shovel";
    public static final String DEMOLITION_CHARGE = "Demolition Charge";

    public static final String AC_BAY = "AC Bay";
    public static final String AMS_BAY = "AMS Bay";
    public static final String AR10_BAY = "AR10 Bay";
    public static final String ATM_BAY = "ATM Bay";
    public static final String CAPITAL_AC_BAY = "Capital AC Bay";
    public static final String CAPITAL_GAUSS_BAY = "Capital Gauss Bay";
    public static final String CAPITAL_LASER_BAY = "Capital Laser Bay";
    public static final String CAPITAL_MASS_DRIVER_BAY = "Capital Mass Driver Bay";
    public static final String CAPITAL_MISSILE_BAY = "Capital Missile Bay";
    public static final String CAPITAL_PPC_BAY = "Capital PPC Bay";
    public static final String GAUSS_BAY = "Gauss Bay";
    public static final String LASER_BAY = "Laser Bay";
    public static final String LBX_AC_BAY = "LBX AC Bay";
    public static final String LRM_BAY = "LRM Bay";
    public static final String MISC_BAY = "Misc Bay";
    public static final String MML_BAY = "MML Bay";
    public static final String MRM_BAY = "MRM Bay";
    public static final String PLASMA_BAY = "Plasma Bay";
    public static final String POINT_DEFENSE_BAY = "Point Defense Bay";
    public static final String PPC_BAY = "PPC Bay";
    public static final String PULSE_LASER_BAY = "Pulse Laser Bay";
    public static final String ROCKET_LAUNCHER_BAY = "Rocket Launcher Bay";
    public static final String SCC_BAY = "Sub-Capital Cannon Bay";
    public static final String SCL_BAY = "Sub-Capital Laser Bay";
    public static final String SC_MISSILE_BAY = "Sub-Capital Missile Bay";
    public static final String SCREEN_LAUNCHER_BAY = "Screen Launcher Bay";
    public static final String SRM_BAY = "SRM Bay";
    public static final String TELE_CAPITAL_MISSILE_BAY = "Tele-Operated Capital Missile Bay";
    public static final String THUNDERBOLT_BAY = "Thunderbolt Bay";
}
