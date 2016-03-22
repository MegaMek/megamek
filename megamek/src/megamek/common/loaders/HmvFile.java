/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common.loaders;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

import megamek.common.AmmoType;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;
import megamek.common.Mounted;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.TroopSpace;
import megamek.common.VTOL;
import megamek.common.WeaponType;

/**
 * Based on the hmpread.c program and the MtfFile object. This class can not
 * load any Mixed tech or Level 3 vehicles.
 *
 * @author <a href="mailto:mnewcomb@sourceforge.net">Michael Newcomb</a>
 */
public class HmvFile implements IMechLoader {
    private String name;
    private String model;

    private HMVMovementType movementType;
    private int rulesLevel;
    private int year;
    private boolean isOmni = false;
    private HMVTechType techType;

    private HMVTechType engineTechType;
    private HMVTechType baseTechType;
    private HMVTechType targetingComputerTechType;

    private int engineRating;
    private HMVEngineType engineType;

    private int cruiseMP;
    private int jumpMP;

    private HMVArmorType armorType;
    private HMVTechType armorTechType;

    private int roundedInternalStructure;

    private int turretArmor;
    private int frontArmor;
    private int leftArmor;
    private int rightArmor;
    private int rearArmor;

    private int artemisType;

    private Hashtable<HMVWeaponLocation, Hashtable<EquipmentType, Integer>> equipment = new Hashtable<HMVWeaponLocation, Hashtable<EquipmentType, Integer>>();

    private float troopSpace = 0;

    private String fluff;

    private List<String> failedEquipment = new ArrayList<String>();

    private boolean hasTurret = false;

    public HmvFile(InputStream is) throws EntityLoadingException {
        try {
            DataInputStream dis = new DataInputStream(is);

            byte[] buffer = new byte[5];
            dis.read(buffer);

            // version is never used.
            // String version = new String(buffer);

            // ??
            dis.skipBytes(2);

            int type = readUnsignedShort(dis);
            movementType = HMVMovementType.getType(type);
            if (null == movementType) {
                throw new EntityLoadingException("Could not locate movement type for " + type + ".");
            }

            // ??
            dis.skipBytes(12);

            buffer = new byte[readUnsignedShort(dis)];
            dis.read(buffer);
            name = new String(buffer);

            buffer = new byte[readUnsignedShort(dis)];
            dis.read(buffer);
            model = new String(buffer);

            // This next one appears to be wrong. FIXME
            rulesLevel = readUnsignedShort(dis);

            year = readUnsignedShort(dis);

            // ??
            dis.skipBytes(8);
            type = readUnsignedByte(dis);
            // FIXME: this is not correct
            // structureType = HMVStructureType.getType(type);
            // ??
            dis.skipBytes(23);

            // The "bf2" buffer contains the word "omni" for OmniVehicles.
            int bf2Length = readUnsignedShort(dis);
            byte[] bf2Buffer = new byte[bf2Length];
            dis.read(bf2Buffer);
            isOmni = containsOmni(bf2Buffer);

            techType = HMVTechType.getType(readUnsignedShort(dis));

            if (techType.equals(HMVTechType.MIXED)) {
                // THESE ARE GUESSES. Need example hmv files to verify.
                baseTechType = HMVTechType.getType(readUnsignedShort(dis));
                engineTechType = HMVTechType.getType(readUnsignedShort(dis));
                targetingComputerTechType = HMVTechType.getType(readUnsignedShort(dis));

                armorTechType = HMVTechType.getType(readUnsignedShort(dis));
            } else if (techType.equals(HMVTechType.CLAN)) {
                engineTechType = HMVTechType.CLAN;
                baseTechType = HMVTechType.CLAN;
                targetingComputerTechType = HMVTechType.CLAN;
                armorTechType = HMVTechType.CLAN;
            } else {
                engineTechType = HMVTechType.INNER_SPHERE;
                baseTechType = HMVTechType.INNER_SPHERE;
                targetingComputerTechType = HMVTechType.INNER_SPHERE;
                armorTechType = HMVTechType.INNER_SPHERE;
            }

            // ??
            dis.skipBytes(4);

            engineRating = readUnsignedShort(dis);
            engineType = HMVEngineType.getType(readUnsignedShort(dis));

            cruiseMP = readUnsignedShort(dis);
            jumpMP = readUnsignedShort(dis);

            // Even if we aren't using this, we need to read past its location.
            readUnsignedShort(dis);
            // heatSinks = readUnsignedShort(dis);
            armorType = HMVArmorType.getType(readUnsignedShort(dis));

            roundedInternalStructure = readUnsignedShort(dis);

            turretArmor = readUnsignedShort(dis);

            if (turretArmor > 0) {
                hasTurret = true;
            }

            // internal structure again ??
            dis.skipBytes(2);

            frontArmor = readUnsignedShort(dis);

            // internal structure again ??
            dis.skipBytes(2);

            leftArmor = readUnsignedShort(dis);

            // internal structure again ??
            dis.skipBytes(2);

            rightArmor = readUnsignedShort(dis);

            // internal structure again ??
            dis.skipBytes(2);

            rearArmor = readUnsignedShort(dis);
            // ??
            if (isOmni) {
                // Skip 12 bytes for OmniVehicles
                dis.skipBytes(12);

                // Decide whether or not the turret is a fixed weight
                int lockedTurret = readUnsignedShort(dis);

                if (lockedTurret == 2) {
                    // Skip something else?...
                    dis.skipBytes(12);
                }
            } else {
                // Skip 14 bytes for non-OmniVehicles
                dis.skipBytes(14);
            }

            int weapons = readUnsignedShort(dis);
            for (int i = 1; i <= weapons; i++) {
                int weaponCount = readUnsignedShort(dis);
                int weaponType = readUnsignedShort(dis);

                // manufacturer name
                dis.skipBytes(readUnsignedShort(dis));

                HMVWeaponLocation weaponLocation = HMVWeaponLocation.getType(readUnsignedShort(dis));
                if (weaponLocation == HMVWeaponLocation.TURRET) {
                    hasTurret = true;
                }

                int weaponAmmo = readUnsignedShort(dis);

                EquipmentType equipmentType = getEquipmentType(weaponType, techType);
                if (equipmentType != null) {
                    addEquipmentType(equipmentType, weaponCount, weaponLocation);

                    if (weaponAmmo > 0) {
                        AmmoType ammoType = getAmmoType(weaponType, techType);

                        if (ammoType != null) {
                            // Need to play games for half ton MG ammo.
                            if ((weaponAmmo < ammoType.getShots()) || ((weaponAmmo % ammoType.getShots()) > 0)) {
                                switch (ammoType.getAmmoType()) {
                                    case AmmoType.T_MG:
                                        if (ammoType.getTechLevel(year) == TechConstants.T_INTRO_BOXSET) {
                                            ammoType = (AmmoType) EquipmentType.get("ISMG Ammo (100)");
                                        } else {
                                            ammoType = (AmmoType) EquipmentType.get("CLMG Ammo (100)");
                                        }
                                        break;
                                    case AmmoType.T_MG_LIGHT:
                                        ammoType = (AmmoType) EquipmentType.get("CLLightMG Ammo (100)");
                                        break;
                                    case AmmoType.T_MG_HEAVY:
                                        ammoType = (AmmoType) EquipmentType.get("CLHeavyMG Ammo (50)");
                                        break;
                                    default:
                                        // Only MG ammo comes in half ton lots.
                                        throw new EntityLoadingException(ammoType.getName() + " has " + ammoType.getShots() + " shots per ton, but " + name + " " + model + " wants " + weaponAmmo + " shots.");
                                }
                            }

                            // Add as many copies of the AmmoType as needed.
                            addEquipmentType(ammoType, weaponAmmo / ammoType.getShots(), HMVWeaponLocation.BODY);

                        } // End found-ammoType

                    } // End have-rounds-of-ammo

                } // End found-equipmentType

                // ??
                dis.skipBytes(4);

            } // Handle the next piece of equipment

            // Read the amount of troop/cargo bays.
            int bayCount = readUnsignedShort(dis);
            for (int loop = 0; loop < bayCount; loop++) {

                // Read the size of this bay.
                // dis.skipBytes(2);
                float baySize = readFloat(dis);

                // bay name (this is free text, so we can't be certain if it is
                // an infantry bay or something else)
                dis.skipBytes(readUnsignedShort(dis));

                // Add the troopSpace of this bay to our running total.
                troopSpace += baySize;

            } // Handle the next bay.

            dis.skipBytes(12);
            int CASE = readUnsignedShort(dis);
            if (CASE == 0xFFFF) {
                if (techType.equals(HMVTechType.INNER_SPHERE)) {
                    addEquipmentType(EquipmentType.get("ISCASE"), 1, HMVWeaponLocation.REAR);
                } else {
                    addEquipmentType(EquipmentType.get("CLCASE"), 1, HMVWeaponLocation.REAR);
                }
            }
            int targetingComp = readUnsignedShort(dis);
            if (targetingComp == 1) {
                if (targetingComputerTechType.equals(HMVTechType.CLAN)) {
                    addEquipmentType(EquipmentType.get("CLTargeting Computer"), 1, HMVWeaponLocation.BODY);
                } else {
                    addEquipmentType(EquipmentType.get("ISTargeting Computer"), 1, HMVWeaponLocation.BODY);
                }
            }

            artemisType = readUnsignedShort(dis);
            // the artemis is a bit field: 1 = SRM artemis IV, 2 = LRM artemis
            // IV,
            // 4 = SRM artemis V, 8 = LRM artemis V
            dis.skipBytes(4);
            int VTOLoptions = readUnsignedShort(dis);
            // Yuck, a decimal field: 10 = main/tail, 20 = dual, 30 = coax
            // rotors
            // 100 = beagle/clan AP, 200 = bloodhound/light AP, 300 = c3 slave
            // mast mount
            int mastEq = VTOLoptions / 100;
            // int rotorType = VTOLoptions % 100; //rorottype is never read
            // TODO read rotottype from a file.
            // Mast mounted equipment is not supported - put it in the rotor and
            // hope for the best
            if (mastEq == 1) {
                if (baseTechType.equals(HMVTechType.CLAN)) {
                    addEquipmentType(EquipmentType.get("CLActiveProbe"), 1, HMVWeaponLocation.TURRET);
                } else {
                    addEquipmentType(EquipmentType.get("BeagleActiveProbe"), 1, HMVWeaponLocation.TURRET);
                }
            } else if (mastEq == 2) {
                if (baseTechType.equals(HMVTechType.CLAN)) {
                    addEquipmentType(EquipmentType.get("CLLightActiveProbe"), 1, HMVWeaponLocation.TURRET);
                } else {
                    addEquipmentType(EquipmentType.get("BloodhoundActiveProbe"), 1, HMVWeaponLocation.TURRET);
                }
            } else if (mastEq == 3) {
                addEquipmentType(EquipmentType.get("ISC3SlaveUnit"), 1, HMVWeaponLocation.TURRET);
            }
            dis.skipBytes(4);

            int fluffSize = 0;
            fluff = "Overview:\n\r";
            buffer = new byte[readUnsignedShort(dis)];
            dis.read(buffer);
            fluff += new String(buffer);
            fluffSize += new String(buffer).length();

            fluff += "\n\rCapability:\n\r";
            buffer = new byte[readUnsignedShort(dis)];
            dis.read(buffer);
            fluff += new String(buffer);
            fluffSize += new String(buffer).length();

            fluff += "\n\rBattle History:\n\r";
            buffer = new byte[readUnsignedShort(dis)];
            dis.read(buffer);
            fluff += new String(buffer);
            fluffSize += new String(buffer).length();

            fluff += "\n\rVariants:\n\r";
            buffer = new byte[readUnsignedShort(dis)];
            dis.read(buffer);
            fluff += new String(buffer);
            fluffSize += new String(buffer).length();

            fluff += "\n\rFamous Vehicles and Pilots:\n\r";
            buffer = new byte[readUnsignedShort(dis)];
            dis.read(buffer);
            fluff += new String(buffer);
            fluffSize += new String(buffer).length();

            fluff += "\n\rDeployment:\n\r";
            buffer = new byte[readUnsignedShort(dis)];
            dis.read(buffer);
            fluff += new String(buffer);
            fluffSize += new String(buffer).length();

            dis.skipBytes(readUnsignedShort(dis)); // notes

            // just a catch all for small Fluffs anything well less then 10
            // characters, per section, isn't worth printing.
            if (fluffSize <= 60) {
                fluff = null;
            }

            int supercharger = readUnsignedShort(dis);
            if (supercharger > 0) {
                addEquipmentType(EquipmentType.get("Supercharger"), 1, HMVWeaponLocation.BODY);
            }

            dis.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new EntityLoadingException("I/O Error reading file");
        }
    }

    private short readUnsignedByte(DataInputStream dis) throws IOException {
        short b = dis.readByte();
        b += b < 0 ? 256 : 0;
        return b;
    }

    private int readUnsignedShort(DataInputStream dis) throws IOException {
        int b2 = readUnsignedByte(dis);

        int b1 = readUnsignedByte(dis);
        b1 <<= 8;

        return b1 + b2;
    }

    /**
     * Read a single precision float from a file in little endian format
     *
     * @param dis
     * @return
     * @throws IOException
     */
    private float readFloat(DataInputStream dis) throws IOException {
        int bits = dis.readInt();
        // Integer.reverseBytes is not supported in 1.4
        // return Float.intBitsToFloat(Integer.reverseBytes(bits));
        bits = ((bits & 0xFF000000) >> 24) | ((bits & 0x00FF0000) >> 8) | ((bits & 0x0000FF00) << 8) | ((bits & 0x000000FF) << 24);
        return Float.intBitsToFloat(bits);
    }

    /**
     * Determine if the buffer contains the "is omni" flag.
     *
     * @param buffer
     *            the array of <code>byte</code>s to be scanned.
     * @return <code>true</code> if the buffer contains the "is omni" flag.
     */
    private boolean containsOmni(byte[] buffer) {
        int index;

        // Look for the 4 byte flag.
        for (index = buffer.length - 4; index >= 0; index--) {
            if ((0x6f == buffer[index]) && // 'o'
                    (0x6d == buffer[index + 1]) && // 'm'
                    (0x6e == buffer[index + 2]) && // 'n'
                    (0x69 == buffer[index + 3])) { // 'i'

                // We found it!
                return true;
            }
        }

        // We didn't find the "is omni" flag;
        return false;
    }

    public Entity getEntity() throws EntityLoadingException {
        try {
            Tank vehicle = null;

            if ((movementType == HMVMovementType.TRACKED) || (movementType == HMVMovementType.WHEELED) || (movementType == HMVMovementType.HOVER) || (movementType == HMVMovementType.DISPLACEMENT_HULL) || (movementType == HMVMovementType.HYDROFOIL) || (movementType == HMVMovementType.SUBMARINE)) {
                vehicle = new Tank();
            } else if (movementType == HMVMovementType.VTOL) {
                vehicle = new VTOL();
            } else {
                throw new EntityLoadingException("Unsupported vehicle movement type:" + movementType);
            }

            vehicle.setChassis(name);
            vehicle.setModel(model);
            vehicle.setYear(year);
            vehicle.setOmni(isOmni);
            vehicle.getFluff().setCapabilities(fluff);

            int techLevel = TechConstants.T_IS_ADVANCED;
            if (rulesLevel == 1) {
                techLevel = TechConstants.T_INTRO_BOXSET;
            } else if (rulesLevel == 2) {
                techLevel = techType == HMVTechType.CLAN ? TechConstants.T_CLAN_TW : TechConstants.T_IS_TW_NON_BOX;
            } else if (techType == HMVTechType.CLAN) {
                techLevel = TechConstants.T_CLAN_ADVANCED;
            }

            vehicle.setTechLevel(techLevel);

            if (vehicle instanceof VTOL) {
                vehicle.setMovementMode(EntityMovementMode.VTOL);
            } else {
                vehicle.setMovementMode(movementType == HMVMovementType.DISPLACEMENT_HULL ? EntityMovementMode.NAVAL : movementType == HMVMovementType.HYDROFOIL ? EntityMovementMode.HYDROFOIL : movementType == HMVMovementType.HOVER ? EntityMovementMode.HOVER : movementType == HMVMovementType.WHEELED ? EntityMovementMode.WHEELED : movementType == HMVMovementType.SUBMARINE ? EntityMovementMode.SUBMARINE : EntityMovementMode.TRACKED);
            }
            //vehicle.setStructureType(EquipmentType.getStructureType("Standard", false));
            // FIXME: structureType is being read wrong
            // stupid not-consistent file format
            // vehicle.setStructureType(EquipmentType.getStructureType(structureType.toString()));

            // This next line sets the weight to a rounded value
            // so that the suspension factor can be retrieved. The
            // real weight is set below that. Why is tonnage not directly
            // stated in the HMV file?!
            vehicle.setWeight(roundedInternalStructure * 10); // temporary
            int suspensionFactor = vehicle.getSuspensionFactor();
            vehicle.setWeight((engineRating + suspensionFactor) / cruiseMP);

            int engineFlags = Engine.TANK_ENGINE;
            if ((techType == HMVTechType.CLAN) || (engineTechType == HMVTechType.CLAN)) {
                engineFlags |= Engine.CLAN_ENGINE;
            }
            vehicle.setEngine(new Engine(engineRating, Engine.getEngineTypeByString(engineType.toString()), engineFlags));

            vehicle.setOriginalWalkMP(cruiseMP);
            vehicle.setOriginalJumpMP(jumpMP);

            // hmmm...
            vehicle.setHasNoTurret(!hasTurret);
            // HMV vehicles NEVER have dual turrets.
            vehicle.setHasNoDualTurret(true);

            vehicle.autoSetInternal();
            vehicle.setArmorType(vehicle.isClan()?"Clan ":"IS "+armorType.toString());
            if (armorTechType == HMVTechType.CLAN) {
                switch (rulesLevel) {
                    case 2:
                        vehicle.setArmorTechLevel(TechConstants.T_CLAN_TW);
                        break;
                    case 3:
                        vehicle.setArmorTechLevel(TechConstants.T_CLAN_ADVANCED);
                        break;
                    default:
                        throw new EntityLoadingException("Unsupported tech level: " + rulesLevel);
                }
            } else {
                switch (rulesLevel) {
                    case 1:
                        vehicle.setArmorTechLevel(TechConstants.T_INTRO_BOXSET);
                        break;
                    case 2:
                        vehicle.setArmorTechLevel(TechConstants.T_IS_TW_NON_BOX);
                        break;
                    case 3:
                        vehicle.setArmorTechLevel(TechConstants.T_IS_ADVANCED);
                        break;
                    default:
                        throw new EntityLoadingException("Unsupported tech level: " + rulesLevel);
                }
            }

            vehicle.initializeArmor(frontArmor, Tank.LOC_FRONT);
            vehicle.initializeArmor(leftArmor, Tank.LOC_LEFT);
            vehicle.initializeArmor(rightArmor, Tank.LOC_RIGHT);
            vehicle.initializeArmor(rearArmor, Tank.LOC_REAR);
            if (vehicle instanceof VTOL) {
                vehicle.initializeArmor(turretArmor, VTOL.LOC_ROTOR);
            } else if (!vehicle.hasNoTurret()) {
                vehicle.initializeArmor(turretArmor, Tank.LOC_TURRET);
            }

            addEquipment(vehicle, HMVWeaponLocation.FRONT, Tank.LOC_FRONT);
            addEquipment(vehicle, HMVWeaponLocation.LEFT, Tank.LOC_LEFT);
            addEquipment(vehicle, HMVWeaponLocation.RIGHT, Tank.LOC_RIGHT);
            addEquipment(vehicle, HMVWeaponLocation.REAR, Tank.LOC_REAR);
            if (!vehicle.hasNoTurret()) {
                addEquipment(vehicle, HMVWeaponLocation.TURRET, Tank.LOC_TURRET);
            }

            addEquipment(vehicle, HMVWeaponLocation.BODY, Tank.LOC_BODY);

            // Do we have any infantry/cargo bays?
            int capacity = (int) Math.round(Math.floor(troopSpace));
            if (capacity > 0) {
                vehicle.addTransporter(new TroopSpace(capacity));
            }

            addFailedEquipment(vehicle);

            return vehicle;
        } catch (Exception e) {
            // System.out.println(structureType.toString());
            e.printStackTrace();
            throw new EntityLoadingException(e.getMessage());
        }
    }

    private void addEquipmentType(EquipmentType equipmentType, int weaponCount, HMVWeaponLocation weaponLocation) {
        Hashtable<EquipmentType, Integer> equipmentAtLocation = equipment.get(weaponLocation);
        if (equipmentAtLocation == null) {
            equipmentAtLocation = new Hashtable<EquipmentType, Integer>();
            equipment.put(weaponLocation, equipmentAtLocation);
        }
        Integer prevCount = equipmentAtLocation.get(equipmentType);
        if (null != prevCount) {
            weaponCount += prevCount.intValue();
        }
        equipmentAtLocation.put(equipmentType, new Integer(weaponCount));
    }

    private void addEquipment(Tank tank, HMVWeaponLocation weaponLocation, int location) throws Exception {
        Hashtable<EquipmentType, Integer> equipmentAtLocation = equipment.get(weaponLocation);
        if (equipmentAtLocation != null) {
            for (Enumeration<EquipmentType> e = equipmentAtLocation.keys(); e.hasMoreElements();) {
                EquipmentType equipmentType = e.nextElement();
                Integer count = equipmentAtLocation.get(equipmentType);

                for (int i = 0; i < count.intValue(); i++) {
                    // for experimental or unofficial equipment, we need
                    // to adjust the mech's techlevel, because HMV only
                    // knows lvl1/2/3
                    if ((equipmentType.getTechLevel(year) > tank.getTechLevel()) && (tank.getTechLevel() >= TechConstants.T_IS_ADVANCED)) {
                        boolean isClan = tank.isClan();
                        if ((equipmentType.getTechLevel(year) == TechConstants.T_IS_EXPERIMENTAL) || (equipmentType.getTechLevel(year) == TechConstants.T_CLAN_EXPERIMENTAL)) {
                            tank.setTechLevel(isClan ? TechConstants.T_CLAN_EXPERIMENTAL : TechConstants.T_IS_EXPERIMENTAL);
                        } else if ((equipmentType.getTechLevel(year) == TechConstants.T_IS_UNOFFICIAL) || (equipmentType.getTechLevel(year) == TechConstants.T_CLAN_UNOFFICIAL)) {
                            tank.setTechLevel(isClan ? TechConstants.T_CLAN_UNOFFICIAL : TechConstants.T_IS_UNOFFICIAL);
                        }
                    }
                    Mounted weapon = tank.addEquipment(equipmentType, location);

                    // Add artemis?
                    // Note this is done here because SRM without artemis and
                    // LRM with artemis
                    // can be in the same location on a tank. (and might be
                    // mislinked)
                    if ((artemisType != 0) && (equipmentType instanceof WeaponType)) {
                        String artemis = null;
                        int ammoType = ((WeaponType) equipmentType).getAmmoType();
                        if (ammoType == AmmoType.T_LRM) {
                            if ((artemisType & 2) == 2) {
                                artemis = "ArtemisIV";
                            } else if ((artemisType & 8) == 8) {
                                artemis = "ArtemisV";
                            }
                        } else if (ammoType == AmmoType.T_SRM) {
                            if ((artemisType & 1) == 1) {
                                artemis = "ArtemisIV";
                            } else if ((artemisType & 4) == 4) {
                                artemis = "ArtemisV";
                            }
                        }
                        if (artemis != null) {
                            EquipmentType artEq;
                            if ((equipmentType.getTechLevel(year) == TechConstants.T_CLAN_TW) || (equipmentType.getTechLevel(year) == TechConstants.T_CLAN_ADVANCED)) {
                                artEq = EquipmentType.get("CL" + artemis);
                            } else {
                                artEq = EquipmentType.get("IS" + artemis);
                            }
                            if (artEq != null) {
                                Mounted fcs = tank.addEquipment(artEq, location);
                                fcs.setLinked(weapon);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addFailedEquipment(Tank tank) {
        for (String s : failedEquipment) {
            tank.addFailedEquipment(s);
        }
    }

    private static final Hashtable<HMVTechType, Hashtable<Long, String>> EQUIPMENT = new Hashtable<HMVTechType, Hashtable<Long, String>>();
    private static final Hashtable<HMVTechType, Hashtable<Long, String>> AMMO = new Hashtable<HMVTechType, Hashtable<Long, String>>();
    static {
        // inner sphere equipment
        // note all weapons should be matched by an ammo entry with the same
        // index
        //
        Hashtable<Long, String> isEquipment = new Hashtable<Long, String>();
        EQUIPMENT.put(HMVTechType.INNER_SPHERE, isEquipment);
        isEquipment.put(new Long(0x0A), "ISDouble Heat Sink");
        isEquipment.put(new Long(0x0B), "Jump Jet");
        isEquipment.put(new Long(0x12), "ISTargeting Computer");
        isEquipment.put(new Long(0x14), "Endo Steel");
        isEquipment.put(new Long(0x15), "Ferro-Fibrous");
        isEquipment.put(new Long(0x17), "ISMASC");
        isEquipment.put(new Long(0x18), "ISArtemisIV");
        isEquipment.put(new Long(0x19), "ISCASE");
        isEquipment.put(new Long(0x33), "ISERLargeLaser");
        isEquipment.put(new Long(0x34), "ISERPPC");
        isEquipment.put(new Long(0x35), "ISFlamer");
        isEquipment.put(new Long(0x36), "ISLaserAMS");
        isEquipment.put(new Long(0x37), "ISLargeLaser");
        isEquipment.put(new Long(0x38), "ISMediumLaser");
        isEquipment.put(new Long(0x39), "ISSmallLaser");
        isEquipment.put(new Long(0x3A), "ISPPC");
        isEquipment.put(new Long(0x3B), "ISLargePulseLaser");
        isEquipment.put(new Long(0x3C), "ISMediumPulseLaser");
        isEquipment.put(new Long(0x3D), "ISSmallPulseLaser");
        isEquipment.put(new Long(0x3E), "ISAC2");
        isEquipment.put(new Long(0x3F), "ISAC5");
        isEquipment.put(new Long(0x40), "ISAC10");
        isEquipment.put(new Long(0x41), "ISAC20");
        isEquipment.put(new Long(0x42), "ISAntiMissileSystem");
        isEquipment.put(new Long(0x43), "Long Tom Cannon");
        isEquipment.put(new Long(0x44), "Sniper Cannon");
        isEquipment.put(new Long(0x45), "Thumper Cannon");
        isEquipment.put(new Long(0x46), "ISLightGaussRifle");
        isEquipment.put(new Long(0x47), "ISGaussRifle");
        isEquipment.put(new Long(0x48), "ISLargeXPulseLaser");
        isEquipment.put(new Long(0x49), "ISMediumXPulseLaser");
        isEquipment.put(new Long(0x4A), "ISSmallXPulseLaser");
        isEquipment.put(new Long(0x4B), "ISLBXAC2");
        isEquipment.put(new Long(0x4C), "ISLBXAC5");
        isEquipment.put(new Long(0x4D), "ISLBXAC10");
        isEquipment.put(new Long(0x4E), "ISLBXAC20");
        isEquipment.put(new Long(0x4F), "ISMachine Gun");
        isEquipment.put(new Long(0x50), "ISLAC2");
        isEquipment.put(new Long(0x51), "ISLAC5");
        isEquipment.put(new Long(0x52), "ISHeavyFlamer");
        isEquipment.put(new Long(0x53), "ISPPCCapacitor");
        isEquipment.put(new Long(0x54), "ISUltraAC2");
        isEquipment.put(new Long(0x55), "ISUltraAC5");
        isEquipment.put(new Long(0x56), "ISUltraAC10");
        isEquipment.put(new Long(0x57), "ISUltraAC20");
        isEquipment.put(new Long(0x59), "ISPPCCapacitor");
        isEquipment.put(new Long(0x5A), "ISERMediumLaser");
        isEquipment.put(new Long(0x5B), "ISERSmallLaser");
        isEquipment.put(new Long(0x5C), "ISAntiPersonnelPod");

        isEquipment.put(new Long(0x60), "ISLRM5");
        isEquipment.put(new Long(0x61), "ISLRM10");
        isEquipment.put(new Long(0x62), "ISLRM15");
        isEquipment.put(new Long(0x63), "ISLRM20");
        isEquipment.put(new Long(0x66), "ISImprovedNarc");
        isEquipment.put(new Long(0x67), "ISSRM2");
        isEquipment.put(new Long(0x68), "ISSRM4");
        isEquipment.put(new Long(0x69), "ISSRM6");
        isEquipment.put(new Long(0x6A), "ISStreakSRM2");
        isEquipment.put(new Long(0x6B), "ISStreakSRM4");
        isEquipment.put(new Long(0x6C), "ISStreakSRM6");
        isEquipment.put(new Long(0x6D), "ISThunderbolt5");
        isEquipment.put(new Long(0x6E), "ISThunderbolt10");
        isEquipment.put(new Long(0x6F), "ISThunderbolt15");
        isEquipment.put(new Long(0x70), "ISThunderbolt20");
        isEquipment.put(new Long(0x71), "ISArrowIVSystem");
        isEquipment.put(new Long(0x72), "ISAngelECMSuite");
        isEquipment.put(new Long(0x73), "ISBeagleActiveProbe");
        isEquipment.put(new Long(0x74), "ISBloodhoundActiveProbe");
        isEquipment.put(new Long(0x75), "ISC3MasterComputer");
        isEquipment.put(new Long(0x76), "ISC3SlaveUnit");
        isEquipment.put(new Long(0x77), "ISImprovedC3CPU");
        isEquipment.put(new Long(0x78), "ISGuardianECM");
        isEquipment.put(new Long(0x79), "ISNarcBeacon");
        isEquipment.put(new Long(0x7A), "ISTAG");
        isEquipment.put(new Long(0x7B), "ISLRM5 (OS)");
        isEquipment.put(new Long(0x7C), "ISLRM10 (OS)");
        isEquipment.put(new Long(0x7D), "ISLRM15 (OS)");
        isEquipment.put(new Long(0x7E), "ISLRM20 (OS)");
        isEquipment.put(new Long(0x7F), "ISSRM2 (OS)");
        isEquipment.put(new Long(0x80), "ISSRM4 (OS)");
        isEquipment.put(new Long(0x81), "ISSRM6 (OS)");
        isEquipment.put(new Long(0x82), "ISStreakSRM2 (OS)");
        isEquipment.put(new Long(0x83), "ISStreakSRM4 (OS)");
        isEquipment.put(new Long(0x84), "ISStreakSRM6 (OS)");
        isEquipment.put(new Long(0x85), "ISVehicleFlamer");
        isEquipment.put(new Long(0x86), "ISLongTomArtillery");
        isEquipment.put(new Long(0x87), "ISSniperArtillery");
        isEquipment.put(new Long(0x88), "ISThumperArtillery");
        isEquipment.put(new Long(0x89), "ISMRM10");
        isEquipment.put(new Long(0x8A), "ISMRM20");
        isEquipment.put(new Long(0x8B), "ISMRM30");
        isEquipment.put(new Long(0x8C), "ISMRM40");
        isEquipment.put(new Long(0x8E), "ISMRM10 (OS)");
        isEquipment.put(new Long(0x8F), "ISMRM20 (OS)");
        isEquipment.put(new Long(0x90), "ISMRM30 (OS)");
        isEquipment.put(new Long(0x91), "ISMRM40 (OS)");
        isEquipment.put(new Long(0x92), "ISLRTorpedo5");
        isEquipment.put(new Long(0x93), "ISLRTorpedo10");
        isEquipment.put(new Long(0x94), "ISLRTorpedo15");
        isEquipment.put(new Long(0x95), "ISLRTorpedo20");
        isEquipment.put(new Long(0x96), "ISSRT2");
        isEquipment.put(new Long(0x97), "ISSRT4");
        isEquipment.put(new Long(0x98), "ISSRT6");
        isEquipment.put(new Long(0x99), "ISLRM5 (I-OS)");
        isEquipment.put(new Long(0x9A), "ISLRM10 (I-OS)");
        isEquipment.put(new Long(0x9B), "ISLRM15 (I-OS)");
        isEquipment.put(new Long(0x9C), "ISLRM20 (I-OS)");
        isEquipment.put(new Long(0x9D), "ISSRM2 (I-OS)");
        isEquipment.put(new Long(0x9E), "ISSRM4 (I-OS)");
        isEquipment.put(new Long(0x9f), "ISSRM6 (I-OS)");
        isEquipment.put(new Long(0xA0), "ISStreakSRM2 (I-OS)");
        isEquipment.put(new Long(0xA1), "ISStreakSRM4 (I-OS)");
        isEquipment.put(new Long(0xA2), "ISStreakSRM6 (I-OS)");
        isEquipment.put(new Long(0xA3), "ISMRM10 (I-OS)");
        isEquipment.put(new Long(0xA4), "ISMRM20 (I-OS)");
        isEquipment.put(new Long(0xA5), "ISMRM30 (I-OS)");
        isEquipment.put(new Long(0xA6), "ISMRM40 (I-OS)");
        isEquipment.put(new Long(0x108), "ISTHBLBXAC2");
        isEquipment.put(new Long(0x109), "ISTHBLBXAC5");
        isEquipment.put(new Long(0x10A), "ISTHBLBXAC20");
        isEquipment.put(new Long(0x10B), "ISUltraAC2 (THB)");
        isEquipment.put(new Long(0x10C), "ISUltraAC10 (THB)");
        isEquipment.put(new Long(0x10D), "ISUltraAC20 (THB)");
        isEquipment.put(new Long(0x11D), "ISTHBAngelECMSuite");
        isEquipment.put(new Long(0x11e), "ISTHBBloodhoundActiveProbe");
        isEquipment.put(new Long(0x121), "ISRotaryAC2");
        isEquipment.put(new Long(0x122), "ISRotaryAC5");
        isEquipment.put(new Long(0x123), "ISHeavyGaussRifle");
        isEquipment.put(new Long(0x12B), "ISRocketLauncher10");
        isEquipment.put(new Long(0x12C), "ISRocketLauncher15");
        isEquipment.put(new Long(0x12D), "ISRocketLauncher20");

        Hashtable<Long, String> isAmmo = new Hashtable<Long, String>();
        AMMO.put(HMVTechType.INNER_SPHERE, isAmmo);
        isAmmo.put(new Long(0x3E), "ISAC2 Ammo");
        isAmmo.put(new Long(0x3F), "ISAC5 Ammo");
        isAmmo.put(new Long(0x40), "ISAC10 Ammo");
        isAmmo.put(new Long(0x41), "ISAC20 Ammo");
        isAmmo.put(new Long(0x42), "ISAMS Ammo");
        isAmmo.put(new Long(0x43), "Long Tom Cannon Ammo");
        isAmmo.put(new Long(0x44), "Sniper Cannon Ammo");
        isAmmo.put(new Long(0x45), "Thumper Cannon Ammo");
        isAmmo.put(new Long(0x46), "ISLightGauss Ammo");
        isAmmo.put(new Long(0x47), "ISGauss Ammo");
        isAmmo.put(new Long(0x4B), "ISLBXAC2 Ammo");
        isAmmo.put(new Long(0x4C), "ISLBXAC5 Ammo");
        isAmmo.put(new Long(0x4D), "ISLBXAC10 Ammo");
        isAmmo.put(new Long(0x4E), "ISLBXAC20 Ammo");
        isAmmo.put(new Long(0x4F), "ISMG Ammo (200)");
        isAmmo.put(new Long(0x50), "ISLAC2 Ammo");
        isAmmo.put(new Long(0x51), "ISLAC5 Ammo");
        isAmmo.put(new Long(0x52), "ISHeavyFlamer Ammo");
        isAmmo.put(new Long(0x54), "ISUltraAC2 Ammo");
        isAmmo.put(new Long(0x55), "ISUltraAC5 Ammo");
        isAmmo.put(new Long(0x56), "ISUltraAC10 Ammo");
        isAmmo.put(new Long(0x57), "ISUltraAC20 Ammo");

        isAmmo.put(new Long(0x60), "ISLRM5 Ammo");
        isAmmo.put(new Long(0x61), "ISLRM10 Ammo");
        isAmmo.put(new Long(0x62), "ISLRM15 Ammo");
        isAmmo.put(new Long(0x63), "ISLRM20 Ammo");
        isAmmo.put(new Long(0x66), "ISiNarc Pods");
        isAmmo.put(new Long(0x67), "ISSRM2 Ammo");
        isAmmo.put(new Long(0x68), "ISSRM4 Ammo");
        isAmmo.put(new Long(0x69), "ISSRM6 Ammo");
        isAmmo.put(new Long(0x6A), "ISStreakSRM2 Ammo");
        isAmmo.put(new Long(0x6B), "ISStreakSRM4 Ammo");
        isAmmo.put(new Long(0x6C), "ISStreakSRM6 Ammo");
        isAmmo.put(new Long(0x6D), "ISThunderbolt5 Ammo");
        isAmmo.put(new Long(0x6E), "ISThunderbolt10 Ammo");
        isAmmo.put(new Long(0x6F), "ISThunderbolt15 Ammo");
        isAmmo.put(new Long(0x70), "ISThunderbolt20 Ammo");
        isAmmo.put(new Long(0x71), "ISArrowIV Ammo");
        isAmmo.put(new Long(0x79), "ISNarc Pods");
        isAmmo.put(new Long(0x85), "ISVehicleFlamer Ammo");
        isAmmo.put(new Long(0x86), "ISLongTom Ammo");
        isAmmo.put(new Long(0x87), "ISSniper Ammo");
        isAmmo.put(new Long(0x88), "ISThumper Ammo");
        isAmmo.put(new Long(0x89), "ISMRM10 Ammo");
        isAmmo.put(new Long(0x8A), "ISMRM20 Ammo");
        isAmmo.put(new Long(0x8B), "ISMRM30 Ammo");
        isAmmo.put(new Long(0x8C), "ISMRM40 Ammo");
        isAmmo.put(new Long(0x92), "ISLRTorpedo5 Ammo");
        isAmmo.put(new Long(0x93), "ISLRTorpedo10 Ammo");
        isAmmo.put(new Long(0x94), "ISLRTorpedo15 Ammo");
        isAmmo.put(new Long(0x95), "ISLRTorpedo20 Ammo");
        isAmmo.put(new Long(0x96), "ISSRT2 Ammo");
        isAmmo.put(new Long(0x97), "ISSRT4 Ammo");
        isAmmo.put(new Long(0x98), "ISSRT6 Ammo");
        isAmmo.put(new Long(0x108), "ISTHBLBXAC2 Ammo");
        isAmmo.put(new Long(0x109), "ISTHBLBXAC5 Ammo");
        isAmmo.put(new Long(0x10A), "ISTHBLBXAC20 Ammo");
        isAmmo.put(new Long(0x10B), "ISUltraAC2 (THB) Ammo");
        isAmmo.put(new Long(0x10C), "ISUltraAC10 (THB) Ammo");
        isAmmo.put(new Long(0x10D), "ISUltraAC20 (THB) Ammo");
        isAmmo.put(new Long(0x121), "ISRotaryAC2 Ammo");
        isAmmo.put(new Long(0x122), "ISRotaryAC5 Ammo");
        isAmmo.put(new Long(0x123), "ISHeavyGauss Ammo");

        // clan criticals
        //
        Hashtable<Long, String> clanEquipment = new Hashtable<Long, String>();
        EQUIPMENT.put(HMVTechType.CLAN, clanEquipment);
        clanEquipment.put(new Long(0x0A), "CLDouble Heat Sink");
        clanEquipment.put(new Long(0x0B), "Jump Jet");
        clanEquipment.put(new Long(0x12), "CLTargeting Computer");
        clanEquipment.put(new Long(0x14), "Endo Steel");
        clanEquipment.put(new Long(0x15), "Ferro-Fibrous");
        clanEquipment.put(new Long(0x17), "CLMASC");
        clanEquipment.put(new Long(0x18), "CLArtemisIV");
        clanEquipment.put(new Long(0x33), "CLERLargeLaser");
        clanEquipment.put(new Long(0x34), "CLERMediumLaser");
        clanEquipment.put(new Long(0x35), "CLERSmallLaser");
        clanEquipment.put(new Long(0x36), "CLERPPC");
        clanEquipment.put(new Long(0x39), "CLSmallLaser");
        clanEquipment.put(new Long(0x37), "CLFlamer");
        clanEquipment.put(new Long(0x38), "CLMediumLaser");
        clanEquipment.put(new Long(0x3A), "CLPPC");
        clanEquipment.put(new Long(0x3C), "CLLargePulseLaser");
        clanEquipment.put(new Long(0x3D), "CLMediumPulseLaser");
        clanEquipment.put(new Long(0x3E), "CLSmallPulseLaser");
        clanEquipment.put(new Long(0x3F), "CLAngelECMSuite");
        clanEquipment.put(new Long(0x40), "CLAntiMissileSystem");
        clanEquipment.put(new Long(0x41), "CLGaussRifle");
        clanEquipment.put(new Long(0x42), "CLLBXAC2");
        clanEquipment.put(new Long(0x43), "CLLBXAC5");
        clanEquipment.put(new Long(0x44), "CLLBXAC10");
        clanEquipment.put(new Long(0x45), "CLLBXAC20");
        clanEquipment.put(new Long(0x46), "CLMG");
        clanEquipment.put(new Long(0x47), "CLUltraAC2");
        clanEquipment.put(new Long(0x48), "CLUltraAC5");
        clanEquipment.put(new Long(0x49), "CLUltraAC10");
        clanEquipment.put(new Long(0x4A), "CLUltraAC20");
        clanEquipment.put(new Long(0x4B), "CLLRM5");
        clanEquipment.put(new Long(0x4C), "CLLRM10");
        clanEquipment.put(new Long(0x4D), "CLLRM15");
        clanEquipment.put(new Long(0x4E), "CLLRM20");
        clanEquipment.put(new Long(0x4F), "CLSRM2");
        clanEquipment.put(new Long(0x50), "CLSRM4");
        clanEquipment.put(new Long(0x51), "CLSRM6");
        clanEquipment.put(new Long(0x52), "CLStreakSRM2");
        clanEquipment.put(new Long(0x53), "CLStreakSRM4");
        clanEquipment.put(new Long(0x54), "CLStreakSRM6");
        clanEquipment.put(new Long(0x55), "CLArrowIVSystem");
        clanEquipment.put(new Long(0x56), "CLAntiPersonnelPod");
        clanEquipment.put(new Long(0x57), "CLActiveProbe");
        clanEquipment.put(new Long(0x58), "CLECMSuite");
        clanEquipment.put(new Long(0x59), "CLNarcBeacon");
        clanEquipment.put(new Long(0x5A), "CLTAG");
        clanEquipment.put(new Long(0x5B), "CLERMicroLaser");
        clanEquipment.put(new Long(0x5C), "CLLRM5 (OS)");
        clanEquipment.put(new Long(0x5D), "CLLRM10 (OS)");
        clanEquipment.put(new Long(0x5E), "CLLRM15 (OS)");
        clanEquipment.put(new Long(0x5F), "CLLRM20 (OS)");
        clanEquipment.put(new Long(0x60), "CLSRM2 (OS)");
        clanEquipment.put(new Long(0x61), "CLSRM4 (OS)");
        clanEquipment.put(new Long(0x62), "CLSRM6 (OS)");
        clanEquipment.put(new Long(0x63), "CLStreakSRM2 (OS)");
        clanEquipment.put(new Long(0x64), "CLStreakSRM4 (OS)");
        clanEquipment.put(new Long(0x65), "CLStreakSRM6 (OS)");
        clanEquipment.put(new Long(0x66), "CLVehicleFlamer");
        clanEquipment.put(new Long(0x67), "CLLongTomArtillery");
        clanEquipment.put(new Long(0x68), "CLSniperArtillery");
        clanEquipment.put(new Long(0x69), "CLThumperArtillery");
        clanEquipment.put(new Long(0x6A), "CLLRTorpedo5");
        clanEquipment.put(new Long(0x6B), "CLLRTorpedo10");
        clanEquipment.put(new Long(0x6C), "CLLRTorpedo15");
        clanEquipment.put(new Long(0x6D), "CLLRTorpedo20");
        clanEquipment.put(new Long(0x6E), "CLSRT2");
        clanEquipment.put(new Long(0x6F), "CLSRT4");
        clanEquipment.put(new Long(0x70), "CLSRT6");
        clanEquipment.put(new Long(0x7B), "CLLRM5 (OS)");
        clanEquipment.put(new Long(0x7C), "CLLRM10 (OS)");
        clanEquipment.put(new Long(0x7D), "CLLRM15 (OS)");
        clanEquipment.put(new Long(0x7E), "CLLRM20 (OS)");
        clanEquipment.put(new Long(0x7F), "CLSRM2 (OS)");
        clanEquipment.put(new Long(0x80), "CLHeavyLargeLaser");
        clanEquipment.put(new Long(0x81), "CLHeavyMediumLaser");
        clanEquipment.put(new Long(0x82), "CLHeavySmallLaser");
        clanEquipment.put(new Long(0x85), "CLVehicleFlamer"); // ?
        clanEquipment.put(new Long(0x92), "CLLRTorpedo5");
        clanEquipment.put(new Long(0x93), "CLLRTorpedo10");
        clanEquipment.put(new Long(0x94), "CLLRTorpedo15");
        clanEquipment.put(new Long(0x95), "CLLRTorpedo20");
        clanEquipment.put(new Long(0x96), "CLSRT2");
        clanEquipment.put(new Long(0x97), "CLSRT4");
        clanEquipment.put(new Long(0x98), "CLSRT6");
        clanEquipment.put(new Long(0xA8), "CLMicroPulseLaser");
        clanEquipment.put(new Long(0xAD), "CLLightMG");
        clanEquipment.put(new Long(0xAE), "CLHeavyMG");
        clanEquipment.put(new Long(0xAF), "CLLightActiveProbe");
        clanEquipment.put(new Long(0xB4), "CLLightTAG");
        clanEquipment.put(new Long(0xFC), "CLATM3");
        clanEquipment.put(new Long(0xFD), "CLATM6");
        clanEquipment.put(new Long(0xFE), "CLATM9");
        clanEquipment.put(new Long(0xFF), "CLATM12");

        Hashtable<Long, String> clAmmo = new Hashtable<Long, String>();
        AMMO.put(HMVTechType.CLAN, clAmmo);
        clAmmo.put(new Long(0x40), "CLAMS Ammo");
        clAmmo.put(new Long(0x41), "CLGauss Ammo");
        clAmmo.put(new Long(0x42), "CLLBXAC2 Ammo");
        clAmmo.put(new Long(0x43), "CLLBXAC5 Ammo");
        clAmmo.put(new Long(0x44), "CLLBXAC10 Ammo");
        clAmmo.put(new Long(0x45), "CLLBXAC20 Ammo");
        clAmmo.put(new Long(0x46), "CLMG Ammo (200)");
        clAmmo.put(new Long(0x47), "CLUltraAC2 Ammo");
        clAmmo.put(new Long(0x48), "CLUltraAC5 Ammo");
        clAmmo.put(new Long(0x49), "CLUltraAC10 Ammo");
        clAmmo.put(new Long(0x4A), "CLUltraAC20 Ammo");
        clAmmo.put(new Long(0x4B), "CLLRM5 Ammo");
        clAmmo.put(new Long(0x4C), "CLLRM10 Ammo");
        clAmmo.put(new Long(0x4D), "CLLRM15 Ammo");
        clAmmo.put(new Long(0x4E), "CLLRM20 Ammo");
        clAmmo.put(new Long(0x4F), "CLSRM2 Ammo");
        clAmmo.put(new Long(0x50), "CLSRM4 Ammo");
        clAmmo.put(new Long(0x51), "CLSRM6 Ammo");
        clAmmo.put(new Long(0x52), "CLStreakSRM2 Ammo");
        clAmmo.put(new Long(0x53), "CLStreakSRM4 Ammo");
        clAmmo.put(new Long(0x54), "CLStreakSRM6 Ammo");
        clAmmo.put(new Long(0x55), "CLArrowIV Ammo");
        clAmmo.put(new Long(0x66), "CLVehicleFlamer Ammo");
        clAmmo.put(new Long(0x67), "CLLongTomArtillery Ammo");
        clAmmo.put(new Long(0x68), "CLSniperArtillery Ammo");
        clAmmo.put(new Long(0x69), "CLThumperArtillery Ammo");
        clAmmo.put(new Long(0x6A), "CLTorpedoLRM5 Ammo");
        clAmmo.put(new Long(0x6B), "CLTorpedoLRM10 Ammo");
        clAmmo.put(new Long(0x6C), "CLTorpedoLRM15 Ammo");
        clAmmo.put(new Long(0x6D), "CLTorpedoLRM20 Ammo");
        clAmmo.put(new Long(0x6E), "CLTorpedoSRM2 Ammo");
        clAmmo.put(new Long(0x6F), "CLTorpedoSRM4 Ammo");
        clAmmo.put(new Long(0x70), "CLTorpedoSRM6 Ammo");
        clAmmo.put(new Long(0x85), "CLVehicleFlamer Ammo"); // ?
        clAmmo.put(new Long(0x92), "CLTorpedoLRM5 Ammo");
        clAmmo.put(new Long(0x93), "CLTorpedoLRM10 Ammo");
        clAmmo.put(new Long(0x94), "CLTorpedoLRM15 Ammo");
        clAmmo.put(new Long(0x95), "CLTorpedoLRM20 Ammo");
        clAmmo.put(new Long(0x96), "CLTorpedoSRM2 Ammo");
        clAmmo.put(new Long(0x97), "CLTorpedoSRM4 Ammo");
        clAmmo.put(new Long(0x98), "CLTorpedoSRM6 Ammo");
        clAmmo.put(new Long(0xAD), "CLLightMG Ammo (200)");
        clAmmo.put(new Long(0xAE), "CLHeavyMG Ammo (100)");
        clAmmo.put(new Long(0xFC), "CLATM3 Ammo");
        clAmmo.put(new Long(0xFD), "CLATM6 Ammo");
        clAmmo.put(new Long(0xFE), "CLATM9 Ammo");
        clAmmo.put(new Long(0xFF), "CLATM12 Ammo");

        // mixed *seems* to be the same as IS-base for HMP files
        Hashtable<Long, String> mixedEquipment = new Hashtable<Long, String>(isEquipment);
        EQUIPMENT.put(HMVTechType.MIXED, mixedEquipment);
        mixedEquipment.put(new Long(0x58), "CLERMicroLaser");
        mixedEquipment.put(new Long(0x5E), "CLLightMG");
        mixedEquipment.put(new Long(0x5F), "CLHeavyMG");
        mixedEquipment.put(new Long(0x64), "CLLightActiveProbe");
        mixedEquipment.put(new Long(0x65), "CLLightTAG");
        mixedEquipment.put(new Long(0xA7), "CLERLargeLaser");
        mixedEquipment.put(new Long(0xA8), "CLERMediumLaser");
        mixedEquipment.put(new Long(0xA9), "CLERSmallLaser");

        mixedEquipment.put(new Long(0xAA), "CLERPPC");
        mixedEquipment.put(new Long(0xAB), "CLFlamer");

        mixedEquipment.put(new Long(0xB0), "CLLargePulseLaser");
        mixedEquipment.put(new Long(0xB1), "CLMediumPulseLaser");
        mixedEquipment.put(new Long(0xB2), "CLSmallPulseLaser");

        mixedEquipment.put(new Long(0xB4), "CLAntiMissileSystem");
        mixedEquipment.put(new Long(0xB5), "CLGaussRifle");
        mixedEquipment.put(new Long(0xB6), "CLLBXAC2");
        mixedEquipment.put(new Long(0xB7), "CLLBXAC5");
        mixedEquipment.put(new Long(0xB8), "CLLBXAC10");
        mixedEquipment.put(new Long(0xB9), "CLLBXAC20");
        mixedEquipment.put(new Long(0xBA), "CLMG");
        mixedEquipment.put(new Long(0xBB), "CLUltraAC2");
        mixedEquipment.put(new Long(0xBC), "CLUltraAC5");
        mixedEquipment.put(new Long(0xBD), "CLUltraAC10");
        mixedEquipment.put(new Long(0xBE), "CLUltraAC20");
        mixedEquipment.put(new Long(0xBF), "CLLRM5");
        mixedEquipment.put(new Long(0xC0), "CLLRM10");
        mixedEquipment.put(new Long(0xC1), "CLLRM15");
        mixedEquipment.put(new Long(0xC2), "CLLRM20");
        mixedEquipment.put(new Long(0xC3), "CLSRM2");
        mixedEquipment.put(new Long(0xC4), "CLSRM4");
        mixedEquipment.put(new Long(0xC5), "CLSRM6");
        mixedEquipment.put(new Long(0xC6), "CLStreakSRM2");
        mixedEquipment.put(new Long(0xC7), "CLStreakSRM4");
        mixedEquipment.put(new Long(0xC8), "CLStreakSRM6");
        mixedEquipment.put(new Long(0xC9), "CLArrowIVSystem");
        mixedEquipment.put(new Long(0xCA), "CLAntiPersonnelPod");
        mixedEquipment.put(new Long(0xCB), "CLActiveProbe");
        mixedEquipment.put(new Long(0xCC), "CLECMSuite");
        mixedEquipment.put(new Long(0xCD), "CLNarcBeacon");
        mixedEquipment.put(new Long(0xCE), "CLTAG");

        mixedEquipment.put(new Long(0xD0), "CLLRM5 (OS)");
        mixedEquipment.put(new Long(0xD1), "CLLRM10 (OS)");
        mixedEquipment.put(new Long(0xD2), "CLLRM15 (OS)");
        mixedEquipment.put(new Long(0xD3), "CLLRM20 (OS)");
        mixedEquipment.put(new Long(0xD4), "CLSRM2 (OS)");
        mixedEquipment.put(new Long(0xD5), "CLSRM2 (OS)");
        mixedEquipment.put(new Long(0xD6), "CLSRM2 (OS)");
        mixedEquipment.put(new Long(0xD7), "CLStreakSRM2 (OS)");
        mixedEquipment.put(new Long(0xD8), "CLStreakSRM4 (OS)");
        mixedEquipment.put(new Long(0xD9), "CLStreakSRM6 (OS)");
        mixedEquipment.put(new Long(0xDA), "CLVehicleFlamer");
        mixedEquipment.put(new Long(0xDB), "CLLongTomArtillery");
        mixedEquipment.put(new Long(0xDC), "CLSniperArtillery");
        mixedEquipment.put(new Long(0xDD), "CLThumperArtillery");
        mixedEquipment.put(new Long(0xDE), "CLLRTorpedo5");
        mixedEquipment.put(new Long(0xDF), "CLLRTorpedo10");
        mixedEquipment.put(new Long(0xE0), "CLLRTorpedo15");
        mixedEquipment.put(new Long(0xE1), "CLLRTorpedo20");
        mixedEquipment.put(new Long(0xE2), "CLSRT2");
        mixedEquipment.put(new Long(0xE3), "CLSRT4");
        mixedEquipment.put(new Long(0xE4), "CLSRT6");

        mixedEquipment.put(new Long(0xF4), "CLHeavyLargeLaser");
        mixedEquipment.put(new Long(0xF5), "CLHeavyMediumLaser");
        mixedEquipment.put(new Long(0xF6), "CLHeavySmallLaser");

        mixedEquipment.put(new Long(0xFC), "CLATM3");
        mixedEquipment.put(new Long(0xFD), "CLATM6");
        mixedEquipment.put(new Long(0xFE), "CLATM9");
        mixedEquipment.put(new Long(0xFF), "CLATM12");

        // but ammo *seems* to use the same numbers as the weapon it goes with
        Hashtable<Long, String> mixedAmmo = new Hashtable<Long, String>(isAmmo);
        AMMO.put(HMVTechType.MIXED, mixedAmmo);
        mixedAmmo.put(new Long(0x5E), "CLLightMG Ammo");
        mixedAmmo.put(new Long(0x5F), "CLHeavyMG Ammo");
        mixedAmmo.put(new Long(0xB4), "CLAntiMissileSystem Ammo");
        mixedAmmo.put(new Long(0xB5), "CLGaussRifle Ammo");
        mixedAmmo.put(new Long(0xB6), "CLLBXAC2 Ammo");
        mixedAmmo.put(new Long(0xB7), "CLLBXAC5 Ammo");
        mixedAmmo.put(new Long(0xB8), "CLLBXAC10 Ammo");
        mixedAmmo.put(new Long(0xB9), "CLLBXAC20 Ammo");
        mixedAmmo.put(new Long(0xBA), "CLMG Ammo");
        mixedAmmo.put(new Long(0xBB), "CLUltraAC2 Ammo");
        mixedAmmo.put(new Long(0xBC), "CLUltraAC5 Ammo");
        mixedAmmo.put(new Long(0xBD), "CLUltraAC10 Ammo");
        mixedAmmo.put(new Long(0xBE), "CLUltraAC20 Ammo");
        mixedAmmo.put(new Long(0xBF), "CLLRM5 Ammo");
        mixedAmmo.put(new Long(0xC0), "CLLRM10 Ammo");
        mixedAmmo.put(new Long(0xC1), "CLLRM15 Ammo");
        mixedAmmo.put(new Long(0xC2), "CLLRM20 Ammo");
        mixedAmmo.put(new Long(0xC3), "CLSRM2 Ammo");
        mixedAmmo.put(new Long(0xC4), "CLSRM4 Ammo");
        mixedAmmo.put(new Long(0xC5), "CLSRM6 Ammo");
        mixedAmmo.put(new Long(0xC6), "CLStreakSRM2 Ammo");
        mixedAmmo.put(new Long(0xC7), "CLStreakSRM4 Ammo");
        mixedAmmo.put(new Long(0xC8), "CLStreakSRM6 Ammo");
        mixedAmmo.put(new Long(0xC9), "CLArrowIVSystem Ammo");
        mixedAmmo.put(new Long(0xCD), "CLNarcBeacon Ammo");
        mixedAmmo.put(new Long(0xDA), "CLVehicleFlamer Ammo");
        mixedAmmo.put(new Long(0xDB), "CLLongTomArtillery Ammo");
        mixedAmmo.put(new Long(0xDC), "CLSniperArtillery Ammo");
        mixedAmmo.put(new Long(0xDD), "CLThumperArtillery Ammo");
        mixedAmmo.put(new Long(0xDE), "CLLRTorpedo5 Ammo");
        mixedAmmo.put(new Long(0xDF), "CLLRTorpedo10 Ammo");
        mixedAmmo.put(new Long(0xE0), "CLLRTorpedo15 Ammo");
        mixedAmmo.put(new Long(0xE1), "CLLRTorpedo20 Ammo");
        mixedAmmo.put(new Long(0xE2), "CLSRT2 Ammo");
        mixedAmmo.put(new Long(0xE3), "CLSRT4 Ammo");
        mixedAmmo.put(new Long(0xE4), "CLSRT6 Ammo");

    }

    private String getEquipmentName(long equipment, HMVTechType techType) {
        return getEquipmentName(new Long(equipment), techType);
    }

    private String getEquipmentName(Long equipment, HMVTechType techType) {
        if (equipment.longValue() > Short.MAX_VALUE) {
            equipment = new Long(equipment.longValue() & 0xFFFF);
        }
        final long value = equipment.longValue();

        String equipName = null;
        try {
            equipName = EQUIPMENT.get(techType).get(equipment);
        } catch (NullPointerException e) {
            // is handeled by the if below.
        }
        if (equipName == null) {
            Hashtable<Long, String> techEquipment = EQUIPMENT.get(techType);
            if (techEquipment != null) {
                equipName = techEquipment.get(equipment);
            }
        }

        // Report unexpected parsing failures.
        if ((equipName == null) && (value != 0) && // 0x00 Empty
                (value != 7) && // 0x07 Lower Leg Actuator (on a quad)
                (value != 8) && // 0x08 Foot Actuator (on a quad)
                (value != 15)) { // 0x0F Fusion Engine
            System.out.print("unknown critical: 0x");
            System.out.print(Integer.toHexString(equipment.intValue()).toUpperCase());
            System.out.print(" (");
            System.out.print(techType);
            System.out.println(")");
        }

        return equipName;
    }

    private EquipmentType getEquipmentType(long equipment, HMVTechType techType) {
        EquipmentType equipmentType = null;

        String equipmentName = getEquipmentName(equipment, techType);
        if (equipmentName != null) {
            equipmentType = EquipmentType.get(equipmentName);

            if (equipmentType == null) {
                failedEquipment.add(equipmentName);
            }
        } else {
            failedEquipment.add("Unknown Equipment (" + Long.toHexString(equipment) + ")");
        }

        return equipmentType;
    }

    private String getAmmoName(long ammo, HMVTechType techType) {
        return getAmmoName(new Long(ammo), techType);
    }

    private String getAmmoName(Long ammo, HMVTechType techType) {
        if (ammo.longValue() > Short.MAX_VALUE) {
            ammo = new Long(ammo.longValue() & 0xFFFF);
        }
        final long value = ammo.longValue();

        String ammoName = null;
        try {
            ammoName = AMMO.get(techType).get(equipment);
        } catch (NullPointerException e) {
            // is handeled by the if below.
        }
        if (ammoName == null) {
            Hashtable<Long, String> techAmmo = AMMO.get(techType);
            if (techAmmo != null) {
                ammoName = techAmmo.get(ammo);
            }
        }

        // Report unexpected parsing failures.
        if ((ammoName == null) && (value != 0)) {
            System.out.print("unknown critical: 0x");
            System.out.print(Integer.toHexString(ammo.intValue()).toUpperCase());
            System.out.print(" (");
            System.out.print(techType);
            System.out.println(")");
        }

        return ammoName;
    }

    private AmmoType getAmmoType(long ammo, HMVTechType techType) {
        AmmoType ammoType = null;

        String ammoName = getAmmoName(ammo, techType);
        if (ammoName != null) {
            ammoType = (AmmoType) EquipmentType.get(ammoName);
        }

        return ammoType;
    }
    /*
     * public static void main(String[] args) throws Exception { for (int i = 0;
     * i < args.length; i++) { HmvFile hmvFile = new HmvFile(new
     * FileInputStream(args[i])); System.out.println(new
     * megamek.client.ui.AWT.MechView(hmvFile.getEntity()).getMechReadout()); }
     * }
     */
}

abstract class HMVType {
    private String name;
    private int id;

    protected HMVType(String name, int id) {
        this.name = name;
        this.id = id;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final HMVType other = (HMVType) obj;
        return Objects.equals(name, other.name) && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }

    public int getId() {
        return id;
    }
}

class HMVEngineType extends HMVType {
    public static final Hashtable<Integer, HMVEngineType> types = new Hashtable<Integer, HMVEngineType>();

    public static final HMVEngineType ICE = new HMVEngineType("I.C.E.", 0);
    public static final HMVEngineType FUSION = new HMVEngineType("Fusion", 1);
    public static final HMVEngineType XLFUSION = new HMVEngineType("XL Fusion", 2);
    public static final HMVEngineType XXLFUSION = new HMVEngineType("XXL Fusion", 3);
    public static final HMVEngineType LIGHTFUSION = new HMVEngineType("Light Fusion", 4);

    private HMVEngineType(String name, int id) {
        super(name, id);
        types.put(new Integer(id), this);
    }

    public static HMVEngineType getType(int i) {
        return types.get(new Integer(i));
    }
}

class HMVArmorType extends HMVType {
    public static final Hashtable<Integer, HMVArmorType> types = new Hashtable<Integer, HMVArmorType>();

    public static final HMVArmorType STANDARD = new HMVArmorType(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_STANDARD), 0);
    public static final HMVArmorType FERRO = new HMVArmorType(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS), 1);

    // public static final HMVArmorType COMPACT = new HMVArmorType("Compact",
    // 2);
    // public static final HMVArmorType LASER = new HMVArmorType("Laser", 3);

    private HMVArmorType(String name, int id) {
        super(name, id);
        types.put(new Integer(id), this);
    }

    public static HMVArmorType getType(int i) {
        return types.get(new Integer(i));
    }
}

class HMVTechType extends HMVType {
    public static final Hashtable<Integer, HMVTechType> types = new Hashtable<Integer, HMVTechType>();

    public static final HMVTechType INNER_SPHERE = new HMVTechType("Inner Sphere", 0);
    public static final HMVTechType CLAN = new HMVTechType("Clan", 1);
    public static final HMVTechType MIXED = new HMVTechType("Mixed", 2);

    private HMVTechType(String name, int id) {
        super(name, id);
        types.put(new Integer(id), this);
    }

    public static HMVTechType getType(int i) {
        return types.get(new Integer(i));
    }
}

class HMVMovementType extends HMVType {
    public static final Hashtable<Integer, HMVMovementType> types = new Hashtable<Integer, HMVMovementType>();

    public static final HMVMovementType TRACKED = new HMVMovementType("Tracked", 8);
    public static final HMVMovementType WHEELED = new HMVMovementType("Wheeled", 16);
    public static final HMVMovementType HOVER = new HMVMovementType("Hover", 32);
    public static final HMVMovementType VTOL = new HMVMovementType("V.T.O.L", 64);
    public static final HMVMovementType HYDROFOIL = new HMVMovementType("Hydrofoil", 128);
    public static final HMVMovementType SUBMARINE = new HMVMovementType("Submarine", 256);
    public static final HMVMovementType DISPLACEMENT_HULL = new HMVMovementType("Displacement Hull", 512);

    private HMVMovementType(String name, int id) {
        super(name, id);
        types.put(new Integer(id), this);
    }

    public static HMVMovementType getType(int i) {
        // Only pay attention to the movement type bits.
        i &= 1016;
        return types.get(new Integer(i));
    }

}

class HMVWeaponLocation extends HMVType {
    public static final Hashtable<Integer, HMVWeaponLocation> types = new Hashtable<Integer, HMVWeaponLocation>();

    public static final HMVWeaponLocation TURRET = new HMVWeaponLocation("Turret", 0);
    public static final HMVWeaponLocation FRONT = new HMVWeaponLocation("Front", 1);
    public static final HMVWeaponLocation LEFT = new HMVWeaponLocation("Left", 2);
    public static final HMVWeaponLocation RIGHT = new HMVWeaponLocation("Right", 3);
    public static final HMVWeaponLocation REAR = new HMVWeaponLocation("Rear", 4);
    public static final HMVWeaponLocation BODY = new HMVWeaponLocation("Body", 5);

    private HMVWeaponLocation(String name, int id) {
        super(name, id);
        types.put(new Integer(id), this);
    }

    public static HMVWeaponLocation getType(int i) {
        return types.get(new Integer(i));
    }
}

class HMVStructureType extends HMVType {
    public static final Hashtable<Integer, HMVStructureType> types = new Hashtable<Integer, HMVStructureType>();

    public static final HMVStructureType STANDARD = new HMVStructureType("Standard", 179);
    public static final HMVStructureType REINFORCED = new HMVStructureType("Reinforced", 193);

    private HMVStructureType(String name, int id) {
        super(name, id);
        types.put(new Integer(id), this);
    }

    public static HMVStructureType getType(int i) {
        return types.get(new Integer(i));
    }
}
