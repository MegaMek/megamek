/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
 */
package megamek.common.loaders;

import megamek.common.*;
import org.apache.logging.log4j.LogManager;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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
    private boolean isOmni;
    private HMVTechType techType;

    private HMVTechType engineTechType;

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

    private Hashtable<HMVWeaponLocation, Hashtable<EquipmentType, Integer>> equipment = new Hashtable<>();

    private double troopSpace = 0;

    private String fluff;

    private List<String> failedEquipment = new ArrayList<>();

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

            HMVTechType baseTechType;
            HMVTechType targetingComputerTechType;
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

            dis.skipBytes(4);

            engineRating = readUnsignedShort(dis);
            engineType = HMVEngineType.getType(readUnsignedShort(dis));

            cruiseMP = readUnsignedShort(dis);
            jumpMP = readUnsignedShort(dis);

            // Even if we aren't using this, we need to read past its location.
            readUnsignedShort(dis);
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

                // Decide whether the turret is a fixed weight
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
                        }
                    }
                }

                dis.skipBytes(4);
            }

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
            }

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
            // the artemis is a bit field: 1 = SRM artemis IV, 2 = LRM artemis IV,
            // 4 = SRM artemis V, 8 = LRM artemis V
            dis.skipBytes(4);
            int VTOLoptions = readUnsignedShort(dis);
            // Yuck, a decimal field: 10 = main/tail, 20 = dual, 30 = coax rotors
            // 100 = beagle/clan AP, 200 = bloodhound/light AP, 300 = c3 slave mast mount
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

            // just a catch-all for small Fluffs anything well less than 10
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

    @Override
    public Entity getEntity() throws EntityLoadingException {
        try {
            Tank vehicle;

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
            vehicle.setArmorType(vehicle.isClan() ? "Clan " : "IS " + armorType.toString());
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
            if (troopSpace > 0) {
                vehicle.addTransporter(new TroopSpace(troopSpace));
            }

            addFailedEquipment(vehicle);

            return vehicle;
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            throw new EntityLoadingException(e.getMessage());
        }
    }

    private void addEquipmentType(EquipmentType equipmentType, int weaponCount, HMVWeaponLocation weaponLocation) {
        Hashtable<EquipmentType, Integer> equipmentAtLocation = equipment.computeIfAbsent(weaponLocation, k -> new Hashtable<>());
        Integer prevCount = equipmentAtLocation.get(equipmentType);
        if (null != prevCount) {
            weaponCount += prevCount;
        }
        equipmentAtLocation.put(equipmentType, weaponCount);
    }

    private void addEquipment(Tank tank, HMVWeaponLocation weaponLocation, int location) throws Exception {
        Hashtable<EquipmentType, Integer> equipmentAtLocation = equipment.get(weaponLocation);
        if (equipmentAtLocation != null) {
            for (Enumeration<EquipmentType> e = equipmentAtLocation.keys(); e.hasMoreElements();) {
                EquipmentType equipmentType = e.nextElement();
                Integer count = equipmentAtLocation.get(equipmentType);

                for (int i = 0; i < count; i++) {
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

    private static final Hashtable<HMVTechType, Hashtable<Long, String>> EQUIPMENT = new Hashtable<>();
    private static final Hashtable<HMVTechType, Hashtable<Long, String>> AMMO = new Hashtable<>();
    static {
        // inner sphere equipment
        // note all weapons should be matched by an ammo entry with the same
        // index
        //
        Hashtable<Long, String> isEquipment = new Hashtable<>();
        EQUIPMENT.put(HMVTechType.INNER_SPHERE, isEquipment);
        isEquipment.put(0x0AL, "ISDouble Heat Sink");
        isEquipment.put(0x0BL, "Jump Jet");
        isEquipment.put(0x12L, "ISTargeting Computer");
        isEquipment.put(0x14L, "Endo Steel");
        isEquipment.put(0x15L, "Ferro-Fibrous");
        isEquipment.put(0x17L, "ISMASC");
        isEquipment.put(0x18L, "ISArtemisIV");
        isEquipment.put(0x19L, "ISCASE");
        isEquipment.put(0x33L, "ISERLargeLaser");
        isEquipment.put(0x34L, "ISERPPC");
        isEquipment.put(0x35L, "ISFlamer");
        isEquipment.put(0x36L, "ISLaserAMS");
        isEquipment.put(0x37L, "ISLargeLaser");
        isEquipment.put(0x38L, "ISMediumLaser");
        isEquipment.put(0x39L, "ISSmallLaser");
        isEquipment.put(0x3AL, "ISPPC");
        isEquipment.put(0x3BL, "ISLargePulseLaser");
        isEquipment.put(0x3CL, "ISMediumPulseLaser");
        isEquipment.put(0x3DL, "ISSmallPulseLaser");
        isEquipment.put(0x3EL, "ISAC2");
        isEquipment.put(0x3FL, "ISAC5");
        isEquipment.put(0x40L, "ISAC10");
        isEquipment.put(0x41L, "ISAC20");
        isEquipment.put(0x42L, "ISAntiMissileSystem");
        isEquipment.put(0x43L, "Long Tom Cannon");
        isEquipment.put(0x44L, "Sniper Cannon");
        isEquipment.put(0x45L, "Thumper Cannon");
        isEquipment.put(0x46L, "ISLightGaussRifle");
        isEquipment.put(0x47L, "ISGaussRifle");
        isEquipment.put(0x48L, "ISLargeXPulseLaser");
        isEquipment.put(0x49L, "ISMediumXPulseLaser");
        isEquipment.put(0x4AL, "ISSmallXPulseLaser");
        isEquipment.put(0x4BL, "ISLBXAC2");
        isEquipment.put(0x4CL, "ISLBXAC5");
        isEquipment.put(0x4DL, "ISLBXAC10");
        isEquipment.put(0x4EL, "ISLBXAC20");
        isEquipment.put(0x4FL, "ISMachine Gun");
        isEquipment.put(0x50L, "ISLAC2");
        isEquipment.put(0x51L, "ISLAC5");
        isEquipment.put(0x52L, "ISHeavyFlamer");
        isEquipment.put(0x53L, "ISPPCCapacitor");
        isEquipment.put(0x54L, "ISUltraAC2");
        isEquipment.put(0x55L, "ISUltraAC5");
        isEquipment.put(0x56L, "ISUltraAC10");
        isEquipment.put(0x57L, "ISUltraAC20");
        isEquipment.put(0x59L, "ISPPCCapacitor");
        isEquipment.put(0x5AL, "ISERMediumLaser");
        isEquipment.put(0x5BL, "ISERSmallLaser");
        isEquipment.put(0x5CL, "ISAntiPersonnelPod");

        isEquipment.put(0x60L, "ISLRM5");
        isEquipment.put(0x61L, "ISLRM10");
        isEquipment.put(0x62L, "ISLRM15");
        isEquipment.put(0x63L, "ISLRM20");
        isEquipment.put(0x66L, "ISImprovedNarc");
        isEquipment.put(0x67L, "ISSRM2");
        isEquipment.put(0x68L, "ISSRM4");
        isEquipment.put(0x69L, "ISSRM6");
        isEquipment.put(0x6AL, "ISStreakSRM2");
        isEquipment.put(0x6BL, "ISStreakSRM4");
        isEquipment.put(0x6CL, "ISStreakSRM6");
        isEquipment.put(0x6DL, "ISThunderbolt5");
        isEquipment.put(0x6EL, "ISThunderbolt10");
        isEquipment.put(0x6FL, "ISThunderbolt15");
        isEquipment.put(0x70L, "ISThunderbolt20");
        isEquipment.put(0x71L, "ISArrowIVSystem");
        isEquipment.put(0x72L, "ISAngelECMSuite");
        isEquipment.put(0x73L, "ISBeagleActiveProbe");
        isEquipment.put(0x74L, "ISBloodhoundActiveProbe");
        isEquipment.put(0x75L, "ISC3MasterComputer");
        isEquipment.put(0x76L, "ISC3SlaveUnit");
        isEquipment.put(0x77L, "ISImprovedC3CPU");
        isEquipment.put(0x78L, "ISGuardianECM");
        isEquipment.put(0x79L, "ISNarcBeacon");
        isEquipment.put(0x7AL, "ISTAG");
        isEquipment.put(0x7BL, "ISLRM5 (OS)");
        isEquipment.put(0x7CL, "ISLRM10 (OS)");
        isEquipment.put(0x7DL, "ISLRM15 (OS)");
        isEquipment.put(0x7EL, "ISLRM20 (OS)");
        isEquipment.put(0x7FL, "ISSRM2 (OS)");
        isEquipment.put(0x80L, "ISSRM4 (OS)");
        isEquipment.put(0x81L, "ISSRM6 (OS)");
        isEquipment.put(0x82L, "ISStreakSRM2 (OS)");
        isEquipment.put(0x83L, "ISStreakSRM4 (OS)");
        isEquipment.put(0x84L, "ISStreakSRM6 (OS)");
        isEquipment.put(0x85L, "ISVehicleFlamer");
        isEquipment.put(0x86L, "ISLongTomArtillery");
        isEquipment.put(0x87L, "ISSniperArtillery");
        isEquipment.put(0x88L, "ISThumperArtillery");
        isEquipment.put(0x89L, "ISMRM10");
        isEquipment.put(0x8AL, "ISMRM20");
        isEquipment.put(0x8BL, "ISMRM30");
        isEquipment.put(0x8CL, "ISMRM40");
        isEquipment.put(0x8EL, "ISMRM10 (OS)");
        isEquipment.put(0x8FL, "ISMRM20 (OS)");
        isEquipment.put(0x90L, "ISMRM30 (OS)");
        isEquipment.put(0x91L, "ISMRM40 (OS)");
        isEquipment.put(0x92L, "ISLRTorpedo5");
        isEquipment.put(0x93L, "ISLRTorpedo10");
        isEquipment.put(0x94L, "ISLRTorpedo15");
        isEquipment.put(0x95L, "ISLRTorpedo20");
        isEquipment.put(0x96L, "ISSRT2");
        isEquipment.put(0x97L, "ISSRT4");
        isEquipment.put(0x98L, "ISSRT6");
        isEquipment.put(0x99L, "ISLRM5 (I-OS)");
        isEquipment.put(0x9AL, "ISLRM10 (I-OS)");
        isEquipment.put(0x9BL, "ISLRM15 (I-OS)");
        isEquipment.put(0x9CL, "ISLRM20 (I-OS)");
        isEquipment.put(0x9DL, "ISSRM2 (I-OS)");
        isEquipment.put(0x9EL, "ISSRM4 (I-OS)");
        isEquipment.put(0x9fL, "ISSRM6 (I-OS)");
        isEquipment.put(0xA0L, "ISStreakSRM2 (I-OS)");
        isEquipment.put(0xA1L, "ISStreakSRM4 (I-OS)");
        isEquipment.put(0xA2L, "ISStreakSRM6 (I-OS)");
        isEquipment.put(0xA3L, "ISMRM10 (I-OS)");
        isEquipment.put(0xA4L, "ISMRM20 (I-OS)");
        isEquipment.put(0xA5L, "ISMRM30 (I-OS)");
        isEquipment.put(0xA6L, "ISMRM40 (I-OS)");
        isEquipment.put(0x108L, "ISTHBLBXAC2");
        isEquipment.put(0x109L, "ISTHBLBXAC5");
        isEquipment.put(0x10AL, "ISTHBLBXAC20");
        isEquipment.put(0x10BL, "ISUltraAC2 (THB)");
        isEquipment.put(0x10CL, "ISUltraAC10 (THB)");
        isEquipment.put(0x10DL, "ISUltraAC20 (THB)");
        isEquipment.put(0x11DL, "ISTHBAngelECMSuite");
        isEquipment.put(0x11eL, "ISTHBBloodhoundActiveProbe");
        isEquipment.put(0x121L, "ISRotaryAC2");
        isEquipment.put(0x122L, "ISRotaryAC5");
        isEquipment.put(0x123L, "ISHeavyGaussRifle");
        isEquipment.put(0x12BL, "ISRocketLauncher10");
        isEquipment.put(0x12CL, "ISRocketLauncher15");
        isEquipment.put(0x12DL, "ISRocketLauncher20");

        Hashtable<Long, String> isAmmo = new Hashtable<>();
        AMMO.put(HMVTechType.INNER_SPHERE, isAmmo);
        isAmmo.put(0x3EL, "ISAC2 Ammo");
        isAmmo.put(0x3FL, "ISAC5 Ammo");
        isAmmo.put(0x40L, "ISAC10 Ammo");
        isAmmo.put(0x41L, "ISAC20 Ammo");
        isAmmo.put(0x42L, "ISAMS Ammo");
        isAmmo.put(0x43L, "Long Tom Cannon Ammo");
        isAmmo.put(0x44L, "Sniper Cannon Ammo");
        isAmmo.put(0x45L, "Thumper Cannon Ammo");
        isAmmo.put(0x46L, "ISLightGauss Ammo");
        isAmmo.put(0x47L, "ISGauss Ammo");
        isAmmo.put(0x4BL, "ISLBXAC2 Ammo");
        isAmmo.put(0x4CL, "ISLBXAC5 Ammo");
        isAmmo.put(0x4DL, "ISLBXAC10 Ammo");
        isAmmo.put(0x4EL, "ISLBXAC20 Ammo");
        isAmmo.put(0x4FL, "ISMG Ammo (200)");
        isAmmo.put(0x50L, "ISLAC2 Ammo");
        isAmmo.put(0x51L, "ISLAC5 Ammo");
        isAmmo.put(0x52L, "ISHeavyFlamer Ammo");
        isAmmo.put(0x54L, "ISUltraAC2 Ammo");
        isAmmo.put(0x55L, "ISUltraAC5 Ammo");
        isAmmo.put(0x56L, "ISUltraAC10 Ammo");
        isAmmo.put(0x57L, "ISUltraAC20 Ammo");

        isAmmo.put(0x60L, "ISLRM5 Ammo");
        isAmmo.put(0x61L, "ISLRM10 Ammo");
        isAmmo.put(0x62L, "ISLRM15 Ammo");
        isAmmo.put(0x63L, "ISLRM20 Ammo");
        isAmmo.put(0x66L, "ISiNarc Pods");
        isAmmo.put(0x67L, "ISSRM2 Ammo");
        isAmmo.put(0x68L, "ISSRM4 Ammo");
        isAmmo.put(0x69L, "ISSRM6 Ammo");
        isAmmo.put(0x6AL, "ISStreakSRM2 Ammo");
        isAmmo.put(0x6BL, "ISStreakSRM4 Ammo");
        isAmmo.put(0x6CL, "ISStreakSRM6 Ammo");
        isAmmo.put(0x6DL, "ISThunderbolt5 Ammo");
        isAmmo.put(0x6EL, "ISThunderbolt10 Ammo");
        isAmmo.put(0x6FL, "ISThunderbolt15 Ammo");
        isAmmo.put(0x70L, "ISThunderbolt20 Ammo");
        isAmmo.put(0x71L, "ISArrowIV Ammo");
        isAmmo.put(0x79L, "ISNarc Pods");
        isAmmo.put(0x85L, "ISVehicleFlamer Ammo");
        isAmmo.put(0x86L, "ISLongTom Ammo");
        isAmmo.put(0x87L, "ISSniper Ammo");
        isAmmo.put(0x88L, "ISThumper Ammo");
        isAmmo.put(0x89L, "ISMRM10 Ammo");
        isAmmo.put(0x8AL, "ISMRM20 Ammo");
        isAmmo.put(0x8BL, "ISMRM30 Ammo");
        isAmmo.put(0x8CL, "ISMRM40 Ammo");
        isAmmo.put(0x92L, "ISLRTorpedo5 Ammo");
        isAmmo.put(0x93L, "ISLRTorpedo10 Ammo");
        isAmmo.put(0x94L, "ISLRTorpedo15 Ammo");
        isAmmo.put(0x95L, "ISLRTorpedo20 Ammo");
        isAmmo.put(0x96L, "ISSRT2 Ammo");
        isAmmo.put(0x97L, "ISSRT4 Ammo");
        isAmmo.put(0x98L, "ISSRT6 Ammo");
        isAmmo.put(0x108L, "ISTHBLBXAC2 Ammo");
        isAmmo.put(0x109L, "ISTHBLBXAC5 Ammo");
        isAmmo.put(0x10AL, "ISTHBLBXAC20 Ammo");
        isAmmo.put(0x10BL, "ISUltraAC2 (THB) Ammo");
        isAmmo.put(0x10CL, "ISUltraAC10 (THB) Ammo");
        isAmmo.put(0x10DL, "ISUltraAC20 (THB) Ammo");
        isAmmo.put(0x121L, "ISRotaryAC2 Ammo");
        isAmmo.put(0x122L, "ISRotaryAC5 Ammo");
        isAmmo.put(0x123L, "ISHeavyGauss Ammo");

        // clan criticals
        //
        Hashtable<Long, String> clanEquipment = new Hashtable<>();
        EQUIPMENT.put(HMVTechType.CLAN, clanEquipment);
        clanEquipment.put(0x0AL, "CLDouble Heat Sink");
        clanEquipment.put(0x0BL, "Jump Jet");
        clanEquipment.put(0x12L, "CLTargeting Computer");
        clanEquipment.put(0x14L, "Endo Steel");
        clanEquipment.put(0x15L, "Ferro-Fibrous");
        clanEquipment.put(0x17L, "CLMASC");
        clanEquipment.put(0x18L, "CLArtemisIV");
        clanEquipment.put(0x33L, "CLERLargeLaser");
        clanEquipment.put(0x34L, "CLERMediumLaser");
        clanEquipment.put(0x35L, "CLERSmallLaser");
        clanEquipment.put(0x36L, "CLERPPC");
        clanEquipment.put(0x39L, "CLSmallLaser");
        clanEquipment.put(0x37L, "CLFlamer");
        clanEquipment.put(0x38L, "CLMediumLaser");
        clanEquipment.put(0x3AL, "CLPPC");
        clanEquipment.put(0x3CL, "CLLargePulseLaser");
        clanEquipment.put(0x3DL, "CLMediumPulseLaser");
        clanEquipment.put(0x3EL, "CLSmallPulseLaser");
        clanEquipment.put(0x3FL, "CLAngelECMSuite");
        clanEquipment.put(0x40L, "CLAntiMissileSystem");
        clanEquipment.put(0x41L, "CLGaussRifle");
        clanEquipment.put(0x42L, "CLLBXAC2");
        clanEquipment.put(0x43L, "CLLBXAC5");
        clanEquipment.put(0x44L, "CLLBXAC10");
        clanEquipment.put(0x45L, "CLLBXAC20");
        clanEquipment.put(0x46L, "CLMG");
        clanEquipment.put(0x47L, "CLUltraAC2");
        clanEquipment.put(0x48L, "CLUltraAC5");
        clanEquipment.put(0x49L, "CLUltraAC10");
        clanEquipment.put(0x4AL, "CLUltraAC20");
        clanEquipment.put(0x4BL, "CLLRM5");
        clanEquipment.put(0x4CL, "CLLRM10");
        clanEquipment.put(0x4DL, "CLLRM15");
        clanEquipment.put(0x4EL, "CLLRM20");
        clanEquipment.put(0x4FL, "CLSRM2");
        clanEquipment.put(0x50L, "CLSRM4");
        clanEquipment.put(0x51L, "CLSRM6");
        clanEquipment.put(0x52L, "CLStreakSRM2");
        clanEquipment.put(0x53L, "CLStreakSRM4");
        clanEquipment.put(0x54L, "CLStreakSRM6");
        clanEquipment.put(0x55L, "CLArrowIVSystem");
        clanEquipment.put(0x56L, "CLAntiPersonnelPod");
        clanEquipment.put(0x57L, "CLActiveProbe");
        clanEquipment.put(0x58L, "CLECMSuite");
        clanEquipment.put(0x59L, "CLNarcBeacon");
        clanEquipment.put(0x5AL, "CLTAG");
        clanEquipment.put(0x5BL, "CLERMicroLaser");
        clanEquipment.put(0x5CL, "CLLRM5 (OS)");
        clanEquipment.put(0x5DL, "CLLRM10 (OS)");
        clanEquipment.put(0x5EL, "CLLRM15 (OS)");
        clanEquipment.put(0x5FL, "CLLRM20 (OS)");
        clanEquipment.put(0x60L, "CLSRM2 (OS)");
        clanEquipment.put(0x61L, "CLSRM4 (OS)");
        clanEquipment.put(0x62L, "CLSRM6 (OS)");
        clanEquipment.put(0x63L, "CLStreakSRM2 (OS)");
        clanEquipment.put(0x64L, "CLStreakSRM4 (OS)");
        clanEquipment.put(0x65L, "CLStreakSRM6 (OS)");
        clanEquipment.put(0x66L, "CLVehicleFlamer");
        clanEquipment.put(0x67L, "CLLongTomArtillery");
        clanEquipment.put(0x68L, "CLSniperArtillery");
        clanEquipment.put(0x69L, "CLThumperArtillery");
        clanEquipment.put(0x6AL, "CLLRTorpedo5");
        clanEquipment.put(0x6BL, "CLLRTorpedo10");
        clanEquipment.put(0x6CL, "CLLRTorpedo15");
        clanEquipment.put(0x6DL, "CLLRTorpedo20");
        clanEquipment.put(0x6EL, "CLSRT2");
        clanEquipment.put(0x6FL, "CLSRT4");
        clanEquipment.put(0x70L, "CLSRT6");
        clanEquipment.put(0x7BL, "CLLRM5 (OS)");
        clanEquipment.put(0x7CL, "CLLRM10 (OS)");
        clanEquipment.put(0x7DL, "CLLRM15 (OS)");
        clanEquipment.put(0x7EL, "CLLRM20 (OS)");
        clanEquipment.put(0x7FL, "CLSRM2 (OS)");
        clanEquipment.put(0x80L, "CLHeavyLargeLaser");
        clanEquipment.put(0x81L, "CLHeavyMediumLaser");
        clanEquipment.put(0x82L, "CLHeavySmallLaser");
        clanEquipment.put(0x85L, "CLVehicleFlamer"); // ?
        clanEquipment.put(0x92L, "CLLRTorpedo5");
        clanEquipment.put(0x93L, "CLLRTorpedo10");
        clanEquipment.put(0x94L, "CLLRTorpedo15");
        clanEquipment.put(0x95L, "CLLRTorpedo20");
        clanEquipment.put(0x96L, "CLSRT2");
        clanEquipment.put(0x97L, "CLSRT4");
        clanEquipment.put(0x98L, "CLSRT6");
        clanEquipment.put(0xA8L, "CLMicroPulseLaser");
        clanEquipment.put(0xADL, "CLLightMG");
        clanEquipment.put(0xAEL, "CLHeavyMG");
        clanEquipment.put(0xAFL, "CLLightActiveProbe");
        clanEquipment.put(0xB4L, "CLLightTAG");
        clanEquipment.put(0xFCL, "CLATM3");
        clanEquipment.put(0xFDL, "CLATM6");
        clanEquipment.put(0xFEL, "CLATM9");
        clanEquipment.put(0xFFL, "CLATM12");

        Hashtable<Long, String> clAmmo = new Hashtable<>();
        AMMO.put(HMVTechType.CLAN, clAmmo);
        clAmmo.put(0x40L, "CLAMS Ammo");
        clAmmo.put(0x41L, "CLGauss Ammo");
        clAmmo.put(0x42L, "CLLBXAC2 Ammo");
        clAmmo.put(0x43L, "CLLBXAC5 Ammo");
        clAmmo.put(0x44L, "CLLBXAC10 Ammo");
        clAmmo.put(0x45L, "CLLBXAC20 Ammo");
        clAmmo.put(0x46L, "CLMG Ammo (200)");
        clAmmo.put(0x47L, "CLUltraAC2 Ammo");
        clAmmo.put(0x48L, "CLUltraAC5 Ammo");
        clAmmo.put(0x49L, "CLUltraAC10 Ammo");
        clAmmo.put(0x4AL, "CLUltraAC20 Ammo");
        clAmmo.put(0x4BL, "CLLRM5 Ammo");
        clAmmo.put(0x4CL, "CLLRM10 Ammo");
        clAmmo.put(0x4DL, "CLLRM15 Ammo");
        clAmmo.put(0x4EL, "CLLRM20 Ammo");
        clAmmo.put(0x4FL, "CLSRM2 Ammo");
        clAmmo.put(0x50L, "CLSRM4 Ammo");
        clAmmo.put(0x51L, "CLSRM6 Ammo");
        clAmmo.put(0x52L, "CLStreakSRM2 Ammo");
        clAmmo.put(0x53L, "CLStreakSRM4 Ammo");
        clAmmo.put(0x54L, "CLStreakSRM6 Ammo");
        clAmmo.put(0x55L, "CLArrowIV Ammo");
        clAmmo.put(0x66L, "CLVehicleFlamer Ammo");
        clAmmo.put(0x67L, "CLLongTomArtillery Ammo");
        clAmmo.put(0x68L, "CLSniperArtillery Ammo");
        clAmmo.put(0x69L, "CLThumperArtillery Ammo");
        clAmmo.put(0x6AL, "CLTorpedoLRM5 Ammo");
        clAmmo.put(0x6BL, "CLTorpedoLRM10 Ammo");
        clAmmo.put(0x6CL, "CLTorpedoLRM15 Ammo");
        clAmmo.put(0x6DL, "CLTorpedoLRM20 Ammo");
        clAmmo.put(0x6EL, "CLTorpedoSRM2 Ammo");
        clAmmo.put(0x6FL, "CLTorpedoSRM4 Ammo");
        clAmmo.put(0x70L, "CLTorpedoSRM6 Ammo");
        clAmmo.put(0x85L, "CLVehicleFlamer Ammo"); // ?
        clAmmo.put(0x92L, "CLTorpedoLRM5 Ammo");
        clAmmo.put(0x93L, "CLTorpedoLRM10 Ammo");
        clAmmo.put(0x94L, "CLTorpedoLRM15 Ammo");
        clAmmo.put(0x95L, "CLTorpedoLRM20 Ammo");
        clAmmo.put(0x96L, "CLTorpedoSRM2 Ammo");
        clAmmo.put(0x97L, "CLTorpedoSRM4 Ammo");
        clAmmo.put(0x98L, "CLTorpedoSRM6 Ammo");
        clAmmo.put(0xADL, "CLLightMG Ammo (200)");
        clAmmo.put(0xAEL, "CLHeavyMG Ammo (100)");
        clAmmo.put(0xFCL, "CLATM3 Ammo");
        clAmmo.put(0xFDL, "CLATM6 Ammo");
        clAmmo.put(0xFEL, "CLATM9 Ammo");
        clAmmo.put(0xFFL, "CLATM12 Ammo");

        // mixed *seems* to be the same as IS-base for HMP files
        Hashtable<Long, String> mixedEquipment = new Hashtable<>(isEquipment);
        EQUIPMENT.put(HMVTechType.MIXED, mixedEquipment);
        mixedEquipment.put(0x58L, "CLERMicroLaser");
        mixedEquipment.put(0x5EL, "CLLightMG");
        mixedEquipment.put(0x5FL, "CLHeavyMG");
        mixedEquipment.put(0x64L, "CLLightActiveProbe");
        mixedEquipment.put(0x65L, "CLLightTAG");
        mixedEquipment.put(0xA7L, "CLERLargeLaser");
        mixedEquipment.put(0xA8L, "CLERMediumLaser");
        mixedEquipment.put(0xA9L, "CLERSmallLaser");

        mixedEquipment.put(0xAAL, "CLERPPC");
        mixedEquipment.put(0xABL, "CLFlamer");

        mixedEquipment.put(0xB0L, "CLLargePulseLaser");
        mixedEquipment.put(0xB1L, "CLMediumPulseLaser");
        mixedEquipment.put(0xB2L, "CLSmallPulseLaser");

        mixedEquipment.put(0xB4L, "CLAntiMissileSystem");
        mixedEquipment.put(0xB5L, "CLGaussRifle");
        mixedEquipment.put(0xB6L, "CLLBXAC2");
        mixedEquipment.put(0xB7L, "CLLBXAC5");
        mixedEquipment.put(0xB8L, "CLLBXAC10");
        mixedEquipment.put(0xB9L, "CLLBXAC20");
        mixedEquipment.put(0xBAL, "CLMG");
        mixedEquipment.put(0xBBL, "CLUltraAC2");
        mixedEquipment.put(0xBCL, "CLUltraAC5");
        mixedEquipment.put(0xBDL, "CLUltraAC10");
        mixedEquipment.put(0xBEL, "CLUltraAC20");
        mixedEquipment.put(0xBFL, "CLLRM5");
        mixedEquipment.put(0xC0L, "CLLRM10");
        mixedEquipment.put(0xC1L, "CLLRM15");
        mixedEquipment.put(0xC2L, "CLLRM20");
        mixedEquipment.put(0xC3L, "CLSRM2");
        mixedEquipment.put(0xC4L, "CLSRM4");
        mixedEquipment.put(0xC5L, "CLSRM6");
        mixedEquipment.put(0xC6L, "CLStreakSRM2");
        mixedEquipment.put(0xC7L, "CLStreakSRM4");
        mixedEquipment.put(0xC8L, "CLStreakSRM6");
        mixedEquipment.put(0xC9L, "CLArrowIVSystem");
        mixedEquipment.put(0xCAL, "CLAntiPersonnelPod");
        mixedEquipment.put(0xCBL, "CLActiveProbe");
        mixedEquipment.put(0xCCL, "CLECMSuite");
        mixedEquipment.put(0xCDL, "CLNarcBeacon");
        mixedEquipment.put(0xCEL, "CLTAG");

        mixedEquipment.put(0xD0L, "CLLRM5 (OS)");
        mixedEquipment.put(0xD1L, "CLLRM10 (OS)");
        mixedEquipment.put(0xD2L, "CLLRM15 (OS)");
        mixedEquipment.put(0xD3L, "CLLRM20 (OS)");
        mixedEquipment.put(0xD4L, "CLSRM2 (OS)");
        mixedEquipment.put(0xD5L, "CLSRM2 (OS)");
        mixedEquipment.put(0xD6L, "CLSRM2 (OS)");
        mixedEquipment.put(0xD7L, "CLStreakSRM2 (OS)");
        mixedEquipment.put(0xD8L, "CLStreakSRM4 (OS)");
        mixedEquipment.put(0xD9L, "CLStreakSRM6 (OS)");
        mixedEquipment.put(0xDAL, "CLVehicleFlamer");
        mixedEquipment.put(0xDBL, "CLLongTomArtillery");
        mixedEquipment.put(0xDCL, "CLSniperArtillery");
        mixedEquipment.put(0xDDL, "CLThumperArtillery");
        mixedEquipment.put(0xDEL, "CLLRTorpedo5");
        mixedEquipment.put(0xDFL, "CLLRTorpedo10");
        mixedEquipment.put(0xE0L, "CLLRTorpedo15");
        mixedEquipment.put(0xE1L, "CLLRTorpedo20");
        mixedEquipment.put(0xE2L, "CLSRT2");
        mixedEquipment.put(0xE3L, "CLSRT4");
        mixedEquipment.put(0xE4L, "CLSRT6");

        mixedEquipment.put(0xF4L, "CLHeavyLargeLaser");
        mixedEquipment.put(0xF5L, "CLHeavyMediumLaser");
        mixedEquipment.put(0xF6L, "CLHeavySmallLaser");

        mixedEquipment.put(0xFCL, "CLATM3");
        mixedEquipment.put(0xFDL, "CLATM6");
        mixedEquipment.put(0xFEL, "CLATM9");
        mixedEquipment.put(0xFFL, "CLATM12");

        // but ammo *seems* to use the same numbers as the weapon it goes with
        Hashtable<Long, String> mixedAmmo = new Hashtable<>(isAmmo);
        AMMO.put(HMVTechType.MIXED, mixedAmmo);
        mixedAmmo.put(0x5EL, "CLLightMG Ammo");
        mixedAmmo.put(0x5FL, "CLHeavyMG Ammo");
        mixedAmmo.put(0xB4L, "CLAntiMissileSystem Ammo");
        mixedAmmo.put(0xB5L, "CLGaussRifle Ammo");
        mixedAmmo.put(0xB6L, "CLLBXAC2 Ammo");
        mixedAmmo.put(0xB7L, "CLLBXAC5 Ammo");
        mixedAmmo.put(0xB8L, "CLLBXAC10 Ammo");
        mixedAmmo.put(0xB9L, "CLLBXAC20 Ammo");
        mixedAmmo.put(0xBAL, "CLMG Ammo");
        mixedAmmo.put(0xBBL, "CLUltraAC2 Ammo");
        mixedAmmo.put(0xBCL, "CLUltraAC5 Ammo");
        mixedAmmo.put(0xBDL, "CLUltraAC10 Ammo");
        mixedAmmo.put(0xBEL, "CLUltraAC20 Ammo");
        mixedAmmo.put(0xBFL, "CLLRM5 Ammo");
        mixedAmmo.put(0xC0L, "CLLRM10 Ammo");
        mixedAmmo.put(0xC1L, "CLLRM15 Ammo");
        mixedAmmo.put(0xC2L, "CLLRM20 Ammo");
        mixedAmmo.put(0xC3L, "CLSRM2 Ammo");
        mixedAmmo.put(0xC4L, "CLSRM4 Ammo");
        mixedAmmo.put(0xC5L, "CLSRM6 Ammo");
        mixedAmmo.put(0xC6L, "CLStreakSRM2 Ammo");
        mixedAmmo.put(0xC7L, "CLStreakSRM4 Ammo");
        mixedAmmo.put(0xC8L, "CLStreakSRM6 Ammo");
        mixedAmmo.put(0xC9L, "CLArrowIVSystem Ammo");
        mixedAmmo.put(0xCDL, "CLNarcBeacon Ammo");
        mixedAmmo.put(0xDAL, "CLVehicleFlamer Ammo");
        mixedAmmo.put(0xDBL, "CLLongTomArtillery Ammo");
        mixedAmmo.put(0xDCL, "CLSniperArtillery Ammo");
        mixedAmmo.put(0xDDL, "CLThumperArtillery Ammo");
        mixedAmmo.put(0xDEL, "CLLRTorpedo5 Ammo");
        mixedAmmo.put(0xDFL, "CLLRTorpedo10 Ammo");
        mixedAmmo.put(0xE0L, "CLLRTorpedo15 Ammo");
        mixedAmmo.put(0xE1L, "CLLRTorpedo20 Ammo");
        mixedAmmo.put(0xE2L, "CLSRT2 Ammo");
        mixedAmmo.put(0xE3L, "CLSRT4 Ammo");
        mixedAmmo.put(0xE4L, "CLSRT6 Ammo");
    }

    private String getEquipmentName(long equipment, HMVTechType techType) {
        return getEquipmentName(Long.valueOf(equipment), techType);
    }

    private String getEquipmentName(Long equipment, HMVTechType techType) {
        if (equipment > Short.MAX_VALUE) {
            equipment = equipment & 0xFFFF;
        }
        final long value = equipment;

        String equipName = null;
        try {
            equipName = EQUIPMENT.get(techType).get(equipment);
        } catch (Exception ignored) {
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
        return getAmmoName(Long.valueOf(ammo), techType);
    }

    private String getAmmoName(Long ammo, HMVTechType techType) {
        if (ammo > Short.MAX_VALUE) {
            ammo = ammo & 0xFFFF;
        }
        final long value = ammo;

        String ammoName = null;
        try {
            ammoName = AMMO.get(techType).get(ammo);
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
        if (this == obj) {
            return true;
        }
        if ((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final HMVType other = (HMVType) obj;
        return Objects.equals(name, other.name) && (id == other.id);
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
    public static final Hashtable<Integer, HMVEngineType> types = new Hashtable<>();

    public static final HMVEngineType ICE = new HMVEngineType("I.C.E.", 0);
    public static final HMVEngineType FUSION = new HMVEngineType("Fusion", 1);

    private HMVEngineType(String name, int id) {
        super(name, id);
        types.put(id, this);
    }

    public static HMVEngineType getType(int i) {
        return types.get(i);
    }
}

class HMVArmorType extends HMVType {
    public static final Hashtable<Integer, HMVArmorType> types = new Hashtable<>();

    public static final HMVArmorType STANDARD = new HMVArmorType(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_STANDARD), 0);

    private HMVArmorType(String name, int id) {
        super(name, id);
        types.put(id, this);
    }

    public static HMVArmorType getType(int i) {
        return types.get(i);
    }
}

class HMVTechType extends HMVType {
    public static final Hashtable<Integer, HMVTechType> types = new Hashtable<>();

    public static final HMVTechType INNER_SPHERE = new HMVTechType("Inner Sphere", 0);
    public static final HMVTechType CLAN = new HMVTechType("Clan", 1);
    public static final HMVTechType MIXED = new HMVTechType("Mixed", 2);

    private HMVTechType(String name, int id) {
        super(name, id);
        types.put(id, this);
    }

    public static HMVTechType getType(int i) {
        return types.get(i);
    }
}

class HMVMovementType extends HMVType {
    public static final Hashtable<Integer, HMVMovementType> types = new Hashtable<>();

    public static final HMVMovementType TRACKED = new HMVMovementType("Tracked", 8);
    public static final HMVMovementType WHEELED = new HMVMovementType("Wheeled", 16);
    public static final HMVMovementType HOVER = new HMVMovementType("Hover", 32);
    public static final HMVMovementType VTOL = new HMVMovementType("V.T.O.L", 64);
    public static final HMVMovementType HYDROFOIL = new HMVMovementType("Hydrofoil", 128);
    public static final HMVMovementType SUBMARINE = new HMVMovementType("Submarine", 256);
    public static final HMVMovementType DISPLACEMENT_HULL = new HMVMovementType("Displacement Hull", 512);

    private HMVMovementType(String name, int id) {
        super(name, id);
        types.put(id, this);
    }

    public static HMVMovementType getType(int i) {
        // Only pay attention to the movement type bits.
        i &= 1016;
        return types.get(i);
    }
}

class HMVWeaponLocation extends HMVType {
    public static final Hashtable<Integer, HMVWeaponLocation> types = new Hashtable<>();

    public static final HMVWeaponLocation TURRET = new HMVWeaponLocation("Turret", 0);
    public static final HMVWeaponLocation FRONT = new HMVWeaponLocation("Front", 1);
    public static final HMVWeaponLocation LEFT = new HMVWeaponLocation("Left", 2);
    public static final HMVWeaponLocation RIGHT = new HMVWeaponLocation("Right", 3);
    public static final HMVWeaponLocation REAR = new HMVWeaponLocation("Rear", 4);
    public static final HMVWeaponLocation BODY = new HMVWeaponLocation("Body", 5);

    private HMVWeaponLocation(String name, int id) {
        super(name, id);
        types.put(id, this);
    }

    public static HMVWeaponLocation getType(int i) {
        return types.get(i);
    }
}
