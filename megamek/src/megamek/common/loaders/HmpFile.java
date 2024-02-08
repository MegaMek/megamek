/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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

import java.io.*;
import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Vector;

/**
 * Based on the hmpread.c program and the MtfFile object. Note that this class
 * doubles as both a MM Heavy Metal Pro parser and a HMP to MTF file converter
 * (when the "main" method is used).
 *
 * @author <a href="mailto:mnewcomb@sourceforge.net">Michael Newcomb</a>
 * @author Ryan McConnell (oscarmm) with lots of help from Ian Hamilton.
 */
public class HmpFile implements IMechLoader {
    private String name;
    private String model;
    private String fluff;

    private ChassisType chassisType;

    private TechType techType;

    private TechType mixedBaseTechType;
    private TechType engineTechType;
    private TechType heatSinkTechType;
    private TechType physicalWeaponTechType;
    private TechType targetingComputerTechType;
    private TechType myomerTechType;
    private TechType armorTechType;

    private int year;
    private int rulesLevel;

    private int tonnage;

    private InternalStructureType internalStructureType;
    private int engineRating;
    private EngineType engineType;
    private ArmorType armorType;

    private int heatSinks;
    private HeatSinkType heatSinkType;

    private int walkMP;
    private int jumpMP;

    private int laArmor;
    private ArmorType laArmorType;
    private int ltArmor;
    private ArmorType ltArmorType;
    private int ltrArmor;
    private ArmorType ltrArmorType;
    private int llArmor;
    private ArmorType llArmorType;

    private int raArmor;
    private ArmorType raArmorType;
    private int rtArmor;
    private ArmorType rtArmorType;
    private int rtrArmor;
    private ArmorType rtrArmorType;
    private int rlArmor;
    private ArmorType rlArmorType;

    private int headArmor;
    private ArmorType headArmorType;

    private int ctArmor;
    private ArmorType ctArmorType;
    private int ctrArmor;
    private ArmorType ctrArmorType;

    private MyomerType myomerType;

    private int totalWeaponCount;
    private int[][] weaponArray;

    private long[] laCriticals = new long[12];
    private long[] ltCriticals = new long[12];
    private long[] llCriticals = new long[12];

    private long[] raCriticals = new long[12];
    private long[] rtCriticals = new long[12];
    private long[] rlCriticals = new long[12];

    private long[] headCriticals = new long[12];
    private long[] ctCriticals = new long[12];

    private Hashtable<EquipmentType, Mounted> spreadEquipment = new Hashtable<>();
    private Vector<Mounted> vSplitWeapons = new Vector<>();

    private int gyroType = Mech.GYRO_STANDARD;
    private int cockpitType = Mech.COCKPIT_STANDARD;
    private int jjType;

    private int atmCounter = 0;
    private int lbxCounter = 0;

    public HmpFile(InputStream is)
    /* OMIT_FOR_JHMPREAD_COMPILATION BLOCK_BEGIN */
    throws EntityLoadingException
    /* BLOCK_END */
    {
        try {
            DataInputStream dis = new DataInputStream(is);

            byte[] buffer = new byte[5];
            dis.read(buffer);
            // String version = new String(buffer); //never used

            // this next one no longer seems accurate...
            // DesignType designType =
            // DesignType.getType(readUnsignedByte(dis));
            readUnsignedByte(dis);

            // ??
            dis.skipBytes(3);

            // some flags saying which Clans use this design
            dis.skipBytes(3);

            // ??
            dis.skipBytes(1);

            // some flags saying which Inner Sphere factions use this design
            dis.skipBytes(3);

            // ??
            dis.skipBytes(1);

            tonnage = readUnsignedShort(dis);

            buffer = new byte[readUnsignedShort(dis)];
            dis.read(buffer);
            name = new String(buffer);

            buffer = new byte[readUnsignedShort(dis)];
            dis.read(buffer);
            model = new String(buffer);

            year = readUnsignedShort(dis);

            rulesLevel = readUnsignedShort(dis);

            // long cost = readUnsignedInt(dis);
            readUnsignedInt(dis);

            // ??
            dis.skipBytes(22);

            // section with BF2 stuff
            int bf2Length = readUnsignedShort(dis);
            dis.skipBytes(bf2Length);

            techType = TechType.getType(readUnsignedShort(dis));

            if (techType == TechType.MIXED) {
                // We've got a total of 7 shorts here.
                // The first one is the mech's "base" chassis technology type.
                // It also doubles as the internal structure type.
                mixedBaseTechType = TechType.getType(readUnsignedShort(dis));
                // Next we have engine, heat sinks, physical attack weapon,
                // myomer, targeting computer, and finally armor. Note that
                // these 14 bytes are always present in mixed-tech designs,
                // whether the specific equipment exists on the mech or not.
                engineTechType = TechType.getType(readUnsignedShort(dis));
                heatSinkTechType = TechType.getType(readUnsignedShort(dis));
                physicalWeaponTechType = TechType.getType(readUnsignedShort(dis));
                myomerTechType = TechType.getType(readUnsignedShort(dis));
                targetingComputerTechType = TechType.getType(readUnsignedShort(dis));
                armorTechType = TechType.getType(readUnsignedShort(dis));
            }

            chassisType = ChassisType.getType(readUnsignedShort(dis));

            internalStructureType = InternalStructureType.getType(readUnsignedShort(dis));

            engineRating = readUnsignedShort(dis);
            engineType = EngineType.getType(readUnsignedShort(dis));

            walkMP = readUnsignedShort(dis);
            jumpMP = readUnsignedShort(dis);

            heatSinks = readUnsignedShort(dis);
            heatSinkType = HeatSinkType.getType(readUnsignedShort(dis));
            if (heatSinkType == HeatSinkType.COMPACT) {
                heatSinks *= 2;
            }

            armorType = ArmorType.getType(readUnsignedShort(dis));

            if (armorType == ArmorType.PATCHWORK) {
                laArmorType = ArmorType.getType(readUnsignedShort(dis));
                ltArmorType = ArmorType.getType(readUnsignedShort(dis));
                llArmorType = ArmorType.getType(readUnsignedShort(dis));
                raArmorType = ArmorType.getType(readUnsignedShort(dis));
                rtArmorType = ArmorType.getType(readUnsignedShort(dis));
                rlArmorType = ArmorType.getType(readUnsignedShort(dis));
                headArmorType = ArmorType.getType(readUnsignedShort(dis));
                ctArmorType = ArmorType.getType(readUnsignedShort(dis));
                ltrArmorType = ArmorType.getType(readUnsignedShort(dis));
                rtrArmorType = ArmorType.getType(readUnsignedShort(dis));
                ctrArmorType = ArmorType.getType(readUnsignedShort(dis));
            }

            dis.skipBytes(2); // ??
            laArmor = readUnsignedShort(dis);
            dis.skipBytes(4); // ??
            ltArmor = readUnsignedShort(dis);
            dis.skipBytes(4); // ??
            llArmor = readUnsignedShort(dis);
            dis.skipBytes(4); // ??
            raArmor = readUnsignedShort(dis);
            dis.skipBytes(4); // ??
            rtArmor = readUnsignedShort(dis);
            dis.skipBytes(2); // ??
            jjType = readUnsignedShort(dis);
            rlArmor = readUnsignedShort(dis);
            dis.skipBytes(4); // ??
            headArmor = readUnsignedShort(dis);
            dis.skipBytes(4); // ??
            ctArmor = readUnsignedShort(dis);
            dis.skipBytes(2); // ??
            ltrArmor = readUnsignedShort(dis);
            rtrArmor = readUnsignedShort(dis);
            ctrArmor = readUnsignedShort(dis);

            myomerType = MyomerType.getType(readUnsignedShort(dis));

            totalWeaponCount = readUnsignedShort(dis);
            weaponArray = new int[totalWeaponCount][4];
            for (int i = 0; i < totalWeaponCount; i++) {
                weaponArray[i][0] = readUnsignedShort(dis); // weapon count
                weaponArray[i][1] = readUnsignedShort(dis); // weapon type
                weaponArray[i][2] = readUnsignedShort(dis); // weapon location
                weaponArray[i][3] = readUnsignedShort(dis); // ammo

                dis.skipBytes(2); // ??

                // manufacturer name
                dis.skipBytes(readUnsignedShort(dis));
            }

            // left arm criticals
            for (int x = 0; x < 12; x++) {
                laCriticals[x] = readUnsignedInt(dis);
            }

            // left torso criticals
            for (int x = 0; x < 12; x++) {
                ltCriticals[x] = readUnsignedInt(dis);
            }

            // left leg criticals
            for (int x = 0; x < 12; x++) {
                llCriticals[x] = readUnsignedInt(dis);
            }

            // right arm criticals
            for (int x = 0; x < 12; x++) {
                raCriticals[x] = readUnsignedInt(dis);
            }

            // right torso criticals
            for (int x = 0; x < 12; x++) {
                rtCriticals[x] = readUnsignedInt(dis);
            }

            // right leg criticals
            for (int x = 0; x < 12; x++) {
                rlCriticals[x] = readUnsignedInt(dis);
            }

            // head criticals
            for (int x = 0; x < 12; x++) {
                headCriticals[x] = readUnsignedInt(dis);
            }

            // center torso criticals
            for (int x = 0; x < 12; x++) {
                ctCriticals[x] = readUnsignedInt(dis);
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

            fluff += "\n\rFamous Mechs and Warriors:\n\r";
            buffer = new byte[readUnsignedShort(dis)];
            dis.read(buffer);
            fluff += new String(buffer);
            fluffSize += new String(buffer).length();

            fluff += "\n\rDeployment:\n\r";
            buffer = new byte[readUnsignedShort(dis)];
            dis.read(buffer);
            fluff += new String(buffer);
            fluffSize += new String(buffer).length();

            // just a catch all for small Fluffs anything well less then 10
            // characters, per section, isn't worth printing.
            if (fluffSize <= 60) {
                fluff = null;
            }

            // non printing notes
            dis.skipBytes(readUnsignedShort(dis));
            dis.skipBytes(readUnsignedShort(dis));

            dis.skipBytes(8); // mechs with supercharger have an 01 in here,
            // but we can identify from the criticals

            // Get cockpit and gyro type, if any.
            if (rulesLevel > 2) {
                gyroType = readUnsignedShort(dis);
                cockpitType = readUnsignedShort(dis);
                dis.skipBytes(16);
                readUnsignedShort(dis);
            } else {
                gyroType = Mech.GYRO_STANDARD;
                cockpitType = Mech.COCKPIT_STANDARD;
            }

            dis.close();
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            throw new EntityLoadingException("Error reading file");
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

    private long readUnsignedInt(DataInputStream dis) throws IOException {
        long b4 = readUnsignedByte(dis);

        long b3 = readUnsignedByte(dis);
        b3 <<= 8;

        long b2 = readUnsignedByte(dis);
        b2 <<= 16;

        long b1 = readUnsignedByte(dis);
        b1 <<= 32;

        return b1 + b2 + b3 + b4;
    }

    /* OMIT_FOR_JHMPREAD_COMPILATION BLOCK_BEGIN */
    @Override
    public Entity getEntity() throws EntityLoadingException {
        try {
            Mech mech = null;
            if ((chassisType == ChassisType.QUADRAPED_OMNI) || (chassisType == ChassisType.QUADRAPED)) {
                mech = new QuadMech(gyroType, cockpitType);
            } else if (chassisType == ChassisType.ARMLESS) {
                mech = new ArmlessMech(gyroType, cockpitType);
            } else {
                mech = new BipedMech(gyroType, cockpitType);
            }

            mech.setChassis(name);
            mech.setModel(model);
            mech.setYear(year);
            mech.getFluff().setCapabilities(fluff);

            mech.setOmni((chassisType == ChassisType.BIPED_OMNI) || (chassisType == ChassisType.QUADRAPED_OMNI));

            if (techType == TechType.INNER_SPHERE) {
                switch (rulesLevel) {
                    case 1:
                        mech.setTechLevel(TechConstants.T_INTRO_BOXSET);
                        break;
                    case 2:
                        mech.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
                        break;
                    case 3:
                        mech.setTechLevel(TechConstants.T_IS_ADVANCED);
                        break;
                    default:
                        throw new EntityLoadingException("Unsupported tech level: " + rulesLevel);
                }
            } else if (techType == TechType.CLAN) {
                switch (rulesLevel) {
                    case 2:
                        mech.setTechLevel(TechConstants.T_CLAN_TW);
                        break;
                    case 3:
                        mech.setTechLevel(TechConstants.T_CLAN_ADVANCED);
                        break;
                    default:
                        throw new EntityLoadingException("Unsupported tech level: " + rulesLevel);
                }
            } else if ((techType == TechType.MIXED) && (mixedBaseTechType == TechType.INNER_SPHERE)) {
                mech.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
                mech.setMixedTech(true);
            } else if ((techType == TechType.MIXED) && (mixedBaseTechType == TechType.CLAN)) {
                mech.setTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
                mech.setMixedTech(true);
            } else {
                throw new EntityLoadingException("Unsupported tech base: " + techType);
            }

            mech.setWeight(tonnage);

            int engineFlags = 0;
            if ((techType == TechType.CLAN) || (engineTechType == TechType.CLAN)) {
                engineFlags = Engine.CLAN_ENGINE;
            }
            mech.setEngine(new Engine(engineRating, Engine.getEngineTypeByString(engineType.toString()), engineFlags));

            mech.setOriginalJumpMP(jumpMP);

            mech.setStructureType(internalStructureType.toString());
            mech.autoSetInternal();

            mech.setArmorType(armorType.toString());
            if (armorTechType == TechType.CLAN) {
                switch (rulesLevel) {
                    case 2:
                        mech.setArmorTechLevel(TechConstants.T_CLAN_TW);
                        break;
                    case 3:
                        mech.setArmorTechLevel(TechConstants.T_CLAN_ADVANCED);
                        break;
                    default:
                        throw new EntityLoadingException("Unsupported tech level: " + rulesLevel);
                }
            } else {
                switch (rulesLevel) {
                    case 1:
                        mech.setArmorTechLevel(TechConstants.T_INTRO_BOXSET);
                        break;
                    case 2:
                        mech.setArmorTechLevel(TechConstants.T_IS_TW_NON_BOX);
                        break;
                    case 3:
                        mech.setArmorTechLevel(TechConstants.T_IS_ADVANCED);
                        break;
                    default:
                        throw new EntityLoadingException("Unsupported tech level: " + rulesLevel);
                }
            }
            mech.initializeArmor(laArmor, Mech.LOC_LARM);
            mech.initializeArmor(ltArmor, Mech.LOC_LT);
            mech.initializeRearArmor(ltrArmor, Mech.LOC_LT);
            mech.initializeArmor(llArmor, Mech.LOC_LLEG);

            mech.initializeArmor(raArmor, Mech.LOC_RARM);
            mech.initializeArmor(rtArmor, Mech.LOC_RT);
            mech.initializeRearArmor(rtrArmor, Mech.LOC_RT);
            mech.initializeArmor(rlArmor, Mech.LOC_RLEG);

            mech.initializeArmor(headArmor, Mech.LOC_HEAD);

            mech.initializeArmor(ctArmor, Mech.LOC_CT);
            mech.initializeRearArmor(ctrArmor, Mech.LOC_CT);

            setupCriticals(mech);

            mech.setArmorTonnage(mech.getArmorWeight());

            // add any heat sinks not allocated
            BigInteger heatSinkFlag;
            if (heatSinkType == HeatSinkType.DOUBLE) {
                heatSinkFlag = MiscType.F_DOUBLE_HEAT_SINK;
            } else if (heatSinkType == HeatSinkType.LASER) {
                heatSinkFlag = MiscType.F_LASER_HEAT_SINK;
            } else if (heatSinkType == HeatSinkType.COMPACT) {
                heatSinkFlag = MiscType.F_COMPACT_HEAT_SINK;
            } else {
                heatSinkFlag = MiscType.F_HEAT_SINK;
            }
            mech.addEngineSinks(heatSinks - mech.heatSinks(), heatSinkFlag);

            return mech;
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            throw new EntityLoadingException(e.getMessage());
        }
    }

    private void removeArmActuators(Mech mech, long[] criticals, int location) {
        // Quad have leg and foot actuators, not arm and hand actuators.
        if (mech.getMovementMode() == EntityMovementMode.QUAD) {
            if (!isLowerLegActuator(criticals[2])) {
                mech.setCritical(location, 2, null);
            }
            if (!isFootActuator(criticals[3])) {
                mech.setCritical(location, 3, null);
            }
        } else {
            if (!isLowerArmActuator(criticals[2])) {
                mech.setCritical(location, 2, null);
            }
            if (!isHandActuator(criticals[3])) {
                mech.setCritical(location, 3, null);
            }
        }
    }

    private void setupCriticals(Mech mech) throws EntityLoadingException {
        removeArmActuators(mech, laCriticals, Mech.LOC_LARM);
        removeArmActuators(mech, raCriticals, Mech.LOC_RARM);

        compactCriticals(rlCriticals);
        setupCriticals(mech, rlCriticals, Mech.LOC_RLEG);
        compactCriticals(llCriticals);
        setupCriticals(mech, llCriticals, Mech.LOC_LLEG);
        if (chassisType != ChassisType.ARMLESS) {
            // HMP helpfully includes arm actuators in armless mechs
            compactCriticals(raCriticals);
            setupCriticals(mech, raCriticals, Mech.LOC_RARM);
            compactCriticals(laCriticals);
            setupCriticals(mech, laCriticals, Mech.LOC_LARM);
        }
        compactCriticals(rtCriticals);
        setupCriticals(mech, rtCriticals, Mech.LOC_RT);
        compactCriticals(ltCriticals);
        setupCriticals(mech, ltCriticals, Mech.LOC_LT);
        compactCriticals(ctCriticals);
        setupCriticals(mech, ctCriticals, Mech.LOC_CT);
        setupCriticals(mech, headCriticals, Mech.LOC_HEAD);
    }

    private String mutateLBXAmmo(String crit) {
        if ((crit.startsWith("CLLBX") || crit.startsWith("ISLBX")) && crit.endsWith("Ammo")) {
            lbxCounter++;
            if ((lbxCounter % 2) == 1) {
                return crit.substring(0, crit.indexOf("Ammo")) + "CL Ammo";
            }
        }
        return crit;
    }

    private String mutateATMAmmo(String crit) {
        if (crit.startsWith("CLATM") && crit.endsWith("Ammo")) {
            atmCounter++;
            if ((atmCounter % 3) == 2) {
                return crit.substring(0, crit.indexOf("Ammo")) + "HE Ammo";
            } else if ((atmCounter % 3) == 0) {
                return crit.substring(0, crit.indexOf("Ammo")) + "ER Ammo";
            }
        }
        return crit;
    }

    private void setupCriticals(Mech mech, long[] criticals, int location) throws EntityLoadingException {
        // Use pass-by-value in case we need the original criticals
        // later (getMtf for example).
        long[] crits = criticals.clone();

        for (int i = 0; i < mech.getNumberOfCriticals(location); i++) {
            if (mech.getCritical(location, i) == null) {
                long critical = crits[i];
                String criticalName = getCriticalName(critical);

                if (isFusionEngine(critical)) {
                    mech.setCritical(location, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE));
                } else if (isGyro(critical)) {
                    mech.setCritical(location, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO));
                } else if (isCockpit(critical)) {
                    mech.setCritical(location, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT));
                } else if (isLifeSupport(critical)) {
                    mech.setCritical(location, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT));
                } else if (isSensor(critical)) {
                    mech.setCritical(location, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS));
                } else if (isJumpJet(critical)) {
                    try {
                        if (jjType == 0) {
                            mech.addEquipment(EquipmentType.get("Jump Jet"), location, false);
                        } else if (jjType == 1) {
                            mech.addEquipment(EquipmentType.get("Improved Jump Jet"), location, false);
                        }
                    } catch (Exception ex) {
                        LogManager.getLogger().error(String.format(
                                "Location was full when adding jump jets to slot #%s of location %s. Aborting entity loading.",
                                i, location), ex);
                        throw new EntityLoadingException(ex.getMessage());
                    }
                } else if (criticalName != null) {
                    EquipmentType equipment = null;
                    try {
                        equipment = EquipmentType.get(criticalName);
                        if (equipment == null) {
                            equipment = EquipmentType.get((mech.isClan() ? "Clan " : "IS ") + criticalName);
                        }
                        if (equipment != null) {
                            // for experimental or unofficial equipment, we need
                            // to adjust the mech's techlevel, because HMP only
                            // knows lvl1/2/3
                            if ((equipment.getTechLevel(year) > mech.getTechLevel()) && (mech.getTechLevel() >= TechConstants.T_IS_ADVANCED)) {
                                boolean isClan = mech.isClan();
                                if ((equipment.getTechLevel(year) == TechConstants.T_IS_EXPERIMENTAL) || (equipment.getTechLevel(year) == TechConstants.T_CLAN_EXPERIMENTAL)) {
                                    mech.setTechLevel(isClan ? TechConstants.T_CLAN_EXPERIMENTAL : TechConstants.T_IS_EXPERIMENTAL);
                                } else if ((equipment.getTechLevel(year) == TechConstants.T_IS_UNOFFICIAL) || (equipment.getTechLevel(year) == TechConstants.T_CLAN_UNOFFICIAL)) {
                                    mech.setTechLevel(isClan ? TechConstants.T_CLAN_UNOFFICIAL : TechConstants.T_IS_UNOFFICIAL);
                                }
                            }
                            boolean rearMounted = (equipment instanceof WeaponType) && isRearMounted(critical);
                            if (equipment.isSpreadable()) {
                                Mounted m = spreadEquipment.get(equipment);
                                if (m != null) {
                                    CriticalSlot criticalSlot = new CriticalSlot(m);
                                    mech.addCritical(location, criticalSlot);

                                } else {
                                    m = mech.addEquipment(equipment, location, rearMounted);
                                    spreadEquipment.put(equipment, m);
                                }
                            } else if (((equipment instanceof WeaponType) && ((WeaponType) equipment).isSplitable()) || ((equipment instanceof MiscType) && equipment.hasFlag(MiscType.F_SPLITABLE))) {
                                // do we already have this one in this or an
                                // outer location?
                                Mounted m = null;
                                boolean bFound = false;
                                for (int x = 0, n = vSplitWeapons.size(); x < n; x++) {
                                    m = vSplitWeapons.elementAt(x);
                                    int nLoc = m.getLocation();
                                    if (((nLoc == location) || (location == Mech.getInnerLocation(nLoc))) && m.getType().equals(equipment)) {
                                        bFound = true;
                                        break;
                                    }
                                }
                                if (bFound) {
                                    m.setFoundCrits(m.getFoundCrits() + 1);
                                    if (m.getFoundCrits() >= m.getCriticals()) {
                                        vSplitWeapons.removeElement(m);
                                    }
                                    // if we're in a new location, set the
                                    // weapon as split
                                    if (location != m.getLocation()) {
                                        m.setSplit(true);
                                    }
                                    // give the most restrictive location for
                                    // arcs
                                    int help = m.getLocation();
                                    m.setLocation(Mech.mostRestrictiveLoc(location, help));
                                    if (location != help) {
                                        m.setSecondLocation(Mech.leastRestrictiveLoc(location, help));
                                    }
                                } else {
                                    // make a new one
                                    m = Mounted.createMounted(mech, equipment);
                                    m.setFoundCrits(1);
                                    vSplitWeapons.addElement(m);
                                }
                                mech.addEquipment(m, location, rearMounted);
                            } else {
                                mech.addEquipment(equipment, location, rearMounted);
                            }
                        } else {
                            if (!criticalName.equals("-Empty-")) {
                                // Can't load this piece of equipment!
                                // Add it to the list so we can show the user.
                                mech.addFailedEquipment(criticalName);
                                // Make the failed equipment an empty slot
                                crits[i] = 0;
                                // Compact criticals again
                                compactCriticals(crits);
                                // Re-parse the same slot, since the compacting
                                // could have moved new equipment to this slot
                                i--;
                            }
                        }
                    } catch (Exception ex) {
                        LogManager.getLogger().error(String.format(
                                "Location was full when adding %s to slot #%s of location %s. Aborting entity loading.",
                                equipment.getInternalName(), i, location), ex);
                        throw new EntityLoadingException(ex.getMessage());
                    }
                }
            }
        }
    }

    /* BLOCK_END */

    private boolean isLowerArmActuator(long critical) {
        return critical == 0x03;
    }

    private static boolean isHandActuator(long critical) {
        return critical == 0x04;
    }

    private static boolean isLowerLegActuator(long critical) {
        return critical == 0x07;
    }

    private static boolean isFootActuator(long critical) {
        return critical == 0x08;
    }

    private static boolean isCockpit(long critical) {
        return critical == 0x0E;
    }

    private static boolean isJumpJet(long critical) {
        return critical == 0x0B;
    }

    private static boolean isLifeSupport(long critical) {
        return critical == 0x0C;
    }

    private static boolean isSensor(long critical) {
        return critical == 0x0D;
    }

    private static boolean isFusionEngine(long critical) {
        return critical == 0x0F;
    }

    private static boolean isGyro(long critical) {
        return critical == 0x10;
    }

    private static boolean isRearMounted(long critical) {
        return (critical & 0xFFFF0000) != 0;
    }

    private static final Hashtable<Object, Serializable> criticals = new Hashtable<>();
    static {
        // common criticals
        criticals.put(0x00L, "-Empty-");
        criticals.put(0x01L, "Shoulder");
        criticals.put(0x02L, "Upper Arm Actuator");
        criticals.put(0x03L, "Lower Arm Actuator");
        criticals.put(0x04L, "Hand Actuator");
        criticals.put(0x05L, "Hip");
        criticals.put(0x06L, "Upper Leg Actuator");
        criticals.put(0x07L, "Lower Leg Actuator");
        criticals.put(0x08L, "Foot Actuator");
        criticals.put(0x09L, "Heat Sink");

        criticals.put(0x0BL, "Jump Jet");
        criticals.put(0x0CL, "Life Support");
        criticals.put(0x0DL, "Sensors");
        criticals.put(0x0EL, "Cockpit");
        criticals.put(0x0FL, "Fusion Engine");
        criticals.put(0x10L, "Gyro");

        criticals.put(0x13L, "Turret");
        criticals.put(0x14L, "Endo Steel");
        criticals.put(0x15L, "Ferro-Fibrous");
        criticals.put(0x16L, "Triple Strength Myomer");

        criticals.put(0x1aL, "Variable Range TargSys");
        criticals.put(0x1bL, "Multi-Trac II");
        criticals.put(0x1cL, "Reactive Armor");
        criticals.put(0x1dL, "Laser-Reflective Armor");
        criticals.put(0x1eL, "Jump Booster");
        criticals.put(0x20L, "Supercharger");
        criticals.put(0x21L, "Light Ferro-Fibrous");
        criticals.put(0x22L, "Heavy Ferro-Fibrous");
        criticals.put(0x25L, "IS2 Compact Heat Sinks");
        criticals.put(0x27L, "Null Signature System");
        criticals.put(0x28L, "Coolant Pod");
        criticals.put(0x2BL, "Claw (THB)");
        criticals.put(0x2CL, "Mace (THB)");
        criticals.put(0x2dL, "Armored Cowl");
        criticals.put(0x2eL, "Buzzsaw (UB)");

        criticals.put(0xF8L, "Combine");
        criticals.put(0xF9L, "Lift Hoist");
        criticals.put(0xFAL, "Chainsaw");

        criticals.put(0xFCL, "CLATM3");
        criticals.put(0xFDL, "CLATM6");
        criticals.put(0xFEL, "CLATM9");
        criticals.put(0xFFL, "CLATM12");
        criticals.put(0x100L, "SB Gauss Rifle (UB)");
        criticals.put(0x101L, "Caseless AC/2 (THB)");
        criticals.put(0x102L, "Caseless AC/5 (THB)");
        criticals.put(0x103L, "Caseless AC/10 (THB)");
        criticals.put(0x104L, "Caseless AC/20 (THB)");
        criticals.put(0x105L, "Heavy AC/2 (THB)");
        criticals.put(0x106L, "Heavy AC/5 (THB)");
        criticals.put(0x107L, "Heavy AC/10 (THB)");
        criticals.put(0x108L, "ISTHBLBXAC2");
        criticals.put(0x109L, "ISTHBLBXAC5");
        criticals.put(0x10AL, "ISTHBLBXAC20");
        criticals.put(0x10BL, "ISUltraAC2 (THB)");
        criticals.put(0x10CL, "ISUltraAC10 (THB)");
        criticals.put(0x10DL, "ISUltraAC20 (THB)");
        criticals.put(0x10EL, "ELRM-5 (THB)");
        criticals.put(0x10FL, "ELRM-10 (THB)");
        criticals.put(0x110L, "ELRM-15 (THB)");
        criticals.put(0x111L, "ELRM-20 (THB)");
        criticals.put(0x112L, "LR DFM-5 (THB)");
        criticals.put(0x113L, "LR DFM-10 (THB)");
        criticals.put(0x114L, "LR DFM-15 (THB)");
        criticals.put(0x115L, "LR DFM-20 (THB)");
        criticals.put(0x116L, "SR DFM-2 (THB)");
        criticals.put(0x117L, "SR DFM-4 (THB)");
        criticals.put(0x118L, "SR DFM-6 (THB)");
        criticals.put(0x119L, "Thunderbolt-5 (THB)");
        criticals.put(0x11AL, "Thunderbolt-10 (THB)");
        criticals.put(0x11BL, "Thunderbolt-15 (THB)");
        criticals.put(0x11CL, "Thunderbolt-20 (THB)");
        criticals.put(0x11FL, "Watchdog ECM (THB)");
        criticals.put(0x120L, "IS Laser AMS (THB)");
        criticals.put(0x121L, "ISRotaryAC2");
        criticals.put(0x122L, "ISRotaryAC5");

        criticals.put(0x124L, "CLRotaryAC2");
        criticals.put(0x125L, "CLRotaryAC5");
        criticals.put(0x126L, "CLRotaryAC10");
        criticals.put(0x127L, "CLRotaryAC20");
        criticals.put(0x128L, "CLPlasmaRifle");
        criticals.put(0x129L, "ISRocketLauncher10");
        criticals.put(0x12AL, "ISRocketLauncher15");
        criticals.put(0x12BL, "ISRocketLauncher20");
        criticals.put(0x12CL, "Mortar/1 (THB)");
        criticals.put(0x12DL, "Mortar/2 (THB)");
        criticals.put(0x12EL, "Mortar/4 (THB)");
        criticals.put(0x12FL, "Mortar/8 (THB)");
        criticals.put(0x130L, "Backhoe");
        criticals.put(0x131L, "Drill");
        criticals.put(0x132L, "Rock Cutter");
        criticals.put(0x133L, "CLStreakLRM5 (OS)"); // ?
        criticals.put(0x134L, "CLStreakLRM10 (OS)"); // ?
        criticals.put(0x135L, "CLStreakLRM15 (OS)");// ?
        criticals.put(0x136L, "CLStreakLRM20 (OS)");// ?

        criticals.put(0x28cL, "CLATM3 Ammo");
        criticals.put(0x28dL, "CLATM6 Ammo");
        criticals.put(0x28eL, "CLATM9 Ammo");
        criticals.put(0x28fL, "CLATM12 Ammo");
        criticals.put(0x290L, "SB Gauss Rifle Ammo (UB)");
        criticals.put(0x291L, "Caseless AC/2 Ammo (THB)");
        criticals.put(0x292L, "Caseless AC/5 Ammo (THB)");
        criticals.put(0x293L, "Caseless AC/10 Ammo (THB)");
        criticals.put(0x294L, "Caseless AC/20 Ammo (THB)");
        criticals.put(0x295L, "Heavy AC/2 Ammo (THB)");
        criticals.put(0x296L, "Heavy AC/5 Ammo (THB)");
        criticals.put(0x297L, "Heavy AC/10 Ammo (THB)");
        criticals.put(0x298L, "ISLBXAC2 Ammo (THB)");
        criticals.put(0x299L, "ISLBXAC5 Ammo (THB)");
        criticals.put(0x29AL, "ISLBXAC20 Ammo (THB)");
        criticals.put(0x29BL, "IS Ultra AC/2 Ammo (THB)");
        criticals.put(0x29CL, "IS Ultra AC/10 Ammo (THB)");
        criticals.put(0x29DL, "IS Ultra AC/20 Ammo (THB)");

        criticals.put(0x2B1L, "ISRotaryAC2 Ammo");
        criticals.put(0x2B2L, "ISRotaryAC5 Ammo");

        criticals.put(0x2b4L, "CLRotaryAC2 Ammo");
        criticals.put(0x2b5L, "CLRotaryAC5 Ammo");
        criticals.put(0x2b6L, "CLRotaryAC10 Ammo");
        criticals.put(0x2b7L, "CLRotaryAC20 Ammo");

        criticals.put(0x2BCL, "Mortar/1 Ammo (THB)");
        criticals.put(0x2BDL, "Mortar/2 Ammo (THB)");
        criticals.put(0x2BEL, "Mortar/4 Ammo (THB)");
        criticals.put(0x2BFL, "Mortar/8 Ammo (THB)");
        criticals.put(0x29EL, "ELRM-5 Ammo (THB)");
        criticals.put(0x29FL, "ELRM-10 Ammo (THB)");
        criticals.put(0x2A0L, "ELRM-15 Ammo (THB)");
        criticals.put(0x2A1L, "ELRM-20 Ammo (THB)");
        criticals.put(0x2A2L, "LR DFM-5 Ammo (THB)");
        criticals.put(0x2A3L, "LR DFM-10 Ammo (THB)");
        criticals.put(0x2A4L, "LR DFM-15 Ammo (THB)");
        criticals.put(0x2A5L, "LR DFM-20 Ammo (THB)");
        criticals.put(0x2A6L, "SR DFM-2 Ammo (THB)");
        criticals.put(0x2A7L, "SR DFM-4 Ammo (THB)");
        criticals.put(0x2A8L, "SR DFM-6 Ammo (THB)");
        criticals.put(0x2A9L, "Thunderbolt-5 Ammo (THB)");
        criticals.put(0x2AAL, "Thunderbolt-10 Ammo (THB)");
        criticals.put(0x2ABL, "Thunderbolt-15 Ammo (THB)");
        criticals.put(0x2ACL, "Thunderbolt-20 Ammo (THB)");

        // Criticals for mechs with a base type of Inner Sphere.
        Hashtable<Long, String> isCriticals = new Hashtable<>();
        criticals.put(TechType.INNER_SPHERE, isCriticals);
        isCriticals.put(0x0AL, "ISDouble Heat Sink");

        isCriticals.put(0x11L, "Hatchet");
        isCriticals.put(0x12L, "ISTargeting Computer");

        isCriticals.put(0x17L, "ISMASC");
        isCriticals.put(0x18L, "ISArtemisIV");
        isCriticals.put(0x19L, "ISCASE");

        isCriticals.put(0x1FL, "Sword");

        isCriticals.put(0x23L, "Stealth Armor");
        isCriticals.put(0x24L, "Blue Shield (UB)");

        isCriticals.put(0x26L, "ISCASEII");

        isCriticals.put(0x33L, "ISERLargeLaser");
        isCriticals.put(0x34L, "ISERPPC");
        isCriticals.put(0x35L, "ISFlamer");
        isCriticals.put(0x36L, "ISLaserAntiMissileSystem");
        isCriticals.put(0x37L, "ISLargeLaser");
        isCriticals.put(0x38L, "ISMediumLaser");
        isCriticals.put(0x39L, "ISSmallLaser");
        isCriticals.put(0x3AL, "ISPPC");
        isCriticals.put(0x3BL, "ISLargePulseLaser");
        isCriticals.put(0x3CL, "ISMediumPulseLaser");
        isCriticals.put(0x3DL, "ISSmallPulseLaser");
        isCriticals.put(0x3EL, "ISAC2");
        isCriticals.put(0x3FL, "ISAC5");
        isCriticals.put(0x40L, "ISAC10");
        isCriticals.put(0x41L, "ISAC20");
        isCriticals.put(0x42L, "ISAntiMissileSystem");
        isCriticals.put(0x43L, "Long Tom Cannon");
        isCriticals.put(0x44L, "Sniper Cannon");
        isCriticals.put(0x45L, "Thumper Cannon");
        isCriticals.put(0x46L, "ISLightGaussRifle");
        isCriticals.put(0x47L, "ISGaussRifle");
        isCriticals.put(0x48L, "ISLargeXPulseLaser");
        isCriticals.put(0x49L, "ISMediumXPulseLaser");
        isCriticals.put(0x4AL, "ISSmallXPulseLaser");
        isCriticals.put(0x4BL, "ISLBXAC2");
        isCriticals.put(0x4CL, "ISLBXAC5");
        isCriticals.put(0x4DL, "ISLBXAC10");
        isCriticals.put(0x4EL, "ISLBXAC20");
        isCriticals.put(0x4FL, "ISMachine Gun");

        isCriticals.put(0x50L, "ISLAC2");
        isCriticals.put(0x51L, "ISLAC5");
        isCriticals.put(0x52L, "ISHeavyFlamer");
        isCriticals.put(0x53L, "ISPPCCapacitor"); // HMP uses this
        // code for ERPPC
        isCriticals.put(0x54L, "ISUltraAC2");
        isCriticals.put(0x55L, "ISUltraAC5");
        isCriticals.put(0x56L, "ISUltraAC10");
        isCriticals.put(0x57L, "ISUltraAC20");
        isCriticals.put(0x58L, "CLERMicroLaser");
        isCriticals.put(0x59L, "ISPPCCapacitor"); // HMP uses this
        // code for standard
        // PPC
        isCriticals.put(0x5AL, "ISERMediumLaser");
        isCriticals.put(0x5BL, "ISERSmallLaser");
        isCriticals.put(0x5CL, "ISAntiPersonnelPod");

        isCriticals.put(0x5EL, "CLLightMG");
        isCriticals.put(0x5FL, "CLHeavyMG");
        isCriticals.put(0x60L, "ISLRM5");
        isCriticals.put(0x61L, "ISLRM10");
        isCriticals.put(0x62L, "ISLRM15");
        isCriticals.put(0x63L, "ISLRM20");
        isCriticals.put(0x64L, "CLLightActiveProbe");
        isCriticals.put(0x65L, "CLLightTAG");
        isCriticals.put(0x66L, "ISImprovedNarc");
        isCriticals.put(0x67L, "ISSRM2");
        isCriticals.put(0x68L, "ISSRM4");
        isCriticals.put(0x69L, "ISSRM6");
        isCriticals.put(0x6AL, "ISStreakSRM2");
        isCriticals.put(0x6BL, "ISStreakSRM4");
        isCriticals.put(0x6CL, "ISStreakSRM6");
        isCriticals.put(0x6DL, "Thunderbolt-5");
        isCriticals.put(0x6EL, "Thunderbolt-10");
        isCriticals.put(0x6FL, "Thunderbolt-15");
        isCriticals.put(0x70L, "Thunderbolt-20");
        isCriticals.put(0x71L, "ISArrowIVSystem");
        isCriticals.put(0x72L, "ISAngelECMSuite");
        isCriticals.put(0x73L, "ISBeagleActiveProbe");
        isCriticals.put(0x74L, "ISBloodhoundActiveProbe");
        isCriticals.put(0x75L, "ISC3MasterComputer");
        isCriticals.put(0x76L, "ISC3SlaveUnit");
        isCriticals.put(0x77L, "ISImprovedC3CPU");
        isCriticals.put(0x78L, "ISGuardianECM");
        isCriticals.put(0x79L, "ISNarcBeacon");
        isCriticals.put(0x7AL, "ISTAG");
        isCriticals.put(0x7BL, "ISLRM5 (OS)");
        isCriticals.put(0x7CL, "ISLRM10 (OS)");
        isCriticals.put(0x7DL, "ISLRM15 (OS)");
        isCriticals.put(0x7EL, "ISLRM20 (OS)");
        isCriticals.put(0x7FL, "ISSRM2 (OS)");
        isCriticals.put(0x80L, "ISSRM4 (OS)");
        isCriticals.put(0x81L, "ISSRM6 (OS)");
        isCriticals.put(0x82L, "ISStreakSRM2 (OS)");
        isCriticals.put(0x83L, "ISStreakSRM4 (OS)");
        isCriticals.put(0x84L, "ISStreakSRM6 (OS)");
        isCriticals.put(0x85L, "ISVehicleFlamer");
        isCriticals.put(0x86L, "ISLongTomArtillery");
        isCriticals.put(0x87L, "ISSniperArtillery");
        isCriticals.put(0x88L, "ISThumperArtillery");
        isCriticals.put(0x89L, "ISMRM10");
        isCriticals.put(0x8AL, "ISMRM20");
        isCriticals.put(0x8BL, "ISMRM30");
        isCriticals.put(0x8CL, "ISMRM40");
        isCriticals.put(0x8DL, "Grenade Launcher");
        isCriticals.put(0x8EL, "ISMRM10 (OS)");
        isCriticals.put(0x8FL, "ISMRM20 (OS)");
        isCriticals.put(0x90L, "ISMRM30 (OS)");
        isCriticals.put(0x91L, "ISMRM40 (OS)");
        isCriticals.put(0x92L, "ISLRTorpedo5");
        isCriticals.put(0x93L, "ISLRTorpedo10");
        isCriticals.put(0x94L, "ISLRTorpedo15");
        isCriticals.put(0x95L, "ISLRTorpedo20");
        isCriticals.put(0x96L, "ISSRT2");
        isCriticals.put(0x97L, "ISSRT4");
        isCriticals.put(0x98L, "ISSRT6");
        isCriticals.put(0x99L, "ISLRM5 (I-OS)");
        isCriticals.put(0x9AL, "ISLRM10 (I-OS)");
        isCriticals.put(0x9BL, "ISLRM15 (I-OS)");
        isCriticals.put(0x9CL, "ISLRM20 (I-OS)");
        isCriticals.put(0x9DL, "ISSRM2 (I-OS)");
        isCriticals.put(0x9EL, "ISSRM4 (I-OS)");
        isCriticals.put(0x9fL, "ISSRM6 (I-OS)");
        isCriticals.put(0xA0L, "ISStreakSRM2 (I-OS)");
        isCriticals.put(0xA1L, "ISStreakSRM4 (I-OS)");
        isCriticals.put(0xA2L, "ISStreakSRM6 (I-OS)");
        isCriticals.put(0xA3L, "ISMRM10 (I-OS)");
        isCriticals.put(0xA4L, "ISMRM20 (I-OS)");
        isCriticals.put(0xA5L, "ISMRM30 (I-OS)");
        isCriticals.put(0xA6L, "ISMRM40 (I-OS)");
        isCriticals.put(0xA7L, "CLERLargeLaser");
        isCriticals.put(0xA8L, "CLERMediumLaser");
        isCriticals.put(0xA9L, "CLERSmallLaser");

        isCriticals.put(0xAAL, "CLERPPC");
        isCriticals.put(0xABL, "CLFlamer");

        isCriticals.put(0xAFL, "CLLaserAntiMissileSystem");
        isCriticals.put(0xB0L, "CLLargePulseLaser");
        isCriticals.put(0xB1L, "CLMediumPulseLaser");
        isCriticals.put(0xB2L, "CLSmallPulseLaser");
        isCriticals.put(0xB3L, "CLAngelECMSuite");
        isCriticals.put(0xB4L, "CLAntiMissileSystem");
        isCriticals.put(0xB5L, "CLGaussRifle");
        isCriticals.put(0xB6L, "CLLBXAC2");
        isCriticals.put(0xB7L, "CLLBXAC5");
        isCriticals.put(0xB8L, "CLLBXAC10");
        isCriticals.put(0xB9L, "CLLBXAC20");

        isCriticals.put(0xBAL, "CLMG");
        isCriticals.put(0xBBL, "CLUltraAC2");
        isCriticals.put(0xBCL, "CLUltraAC5");
        isCriticals.put(0xBDL, "CLUltraAC10");
        isCriticals.put(0xBEL, "CLUltraAC20");
        isCriticals.put(0xBFL, "CLLRM5");
        isCriticals.put(0xC0L, "CLLRM10");
        isCriticals.put(0xC1L, "CLLRM15");
        isCriticals.put(0xC2L, "CLLRM20");
        isCriticals.put(0xC3L, "CLSRM2");
        isCriticals.put(0xC4L, "CLSRM4");
        isCriticals.put(0xC5L, "CLSRM6");
        isCriticals.put(0xC6L, "CLStreakSRM2");
        isCriticals.put(0xC7L, "CLStreakSRM4");
        isCriticals.put(0xC8L, "CLStreakSRM6");
        isCriticals.put(0xC9L, "CLArrowIVSystem");
        isCriticals.put(0xCAL, "CLAntiPersonnelPod");
        isCriticals.put(0xCBL, "CLActiveProbe");
        isCriticals.put(0xCCL, "CLECMSuite");
        isCriticals.put(0xCDL, "CLNarcBeacon");
        isCriticals.put(0xCEL, "CLTAG");
        isCriticals.put(0xCFL, "Thunderbolt (OS)");
        isCriticals.put(0xD0L, "CLLRM5 (OS)");
        isCriticals.put(0xD1L, "CLLRM10 (OS)");
        isCriticals.put(0xD2L, "CLLRM15 (OS)");
        isCriticals.put(0xD3L, "CLLRM20 (OS)");
        isCriticals.put(0xD4L, "CLSRM2 (OS)");
        isCriticals.put(0xD5L, "CLSRM2 (OS)");
        isCriticals.put(0xD6L, "CLSRM2 (OS)");
        isCriticals.put(0xD7L, "CLStreakSRM2 (OS)");
        isCriticals.put(0xD8L, "CLStreakSRM4 (OS)");
        isCriticals.put(0xD9L, "CLStreakSRM6 (OS)");
        isCriticals.put(0xDAL, "CLVehicleFlamer");
        isCriticals.put(0xDBL, "CLLongTomArtillery");
        isCriticals.put(0xDCL, "CLSniperArtillery");
        isCriticals.put(0xDDL, "CLThumperArtillery");
        isCriticals.put(0xDEL, "CLLRTorpedo5");
        isCriticals.put(0xDFL, "CLLRTorpedo10");
        isCriticals.put(0xE0L, "CLLRTorpedo15");
        isCriticals.put(0xE1L, "CLLRTorpedo20");
        isCriticals.put(0xE2L, "CLSRT2");
        isCriticals.put(0xE3L, "CLSRT4");
        isCriticals.put(0xE4L, "CLSRT6");
        isCriticals.put(0xE5L, "CLStreakLRM5");
        isCriticals.put(0xE6L, "CLStreakLRM10");
        isCriticals.put(0xE7L, "CLStreakLRM15");
        isCriticals.put(0xE8L, "CLStreakLRM20");
        isCriticals.put(0xE9L, "CLGrenadeLauncher");
        isCriticals.put(0xEAL, "CLLRM5 (I-OS)");
        isCriticals.put(0xEBL, "CLLRM10 (I-OS)");
        isCriticals.put(0xECL, "CLLRM15 (I-OS)");
        isCriticals.put(0xEDL, "CLLRM20 (I-OS)");
        isCriticals.put(0xEEL, "CLSRM2 (I-OS)");
        isCriticals.put(0xEFL, "CLSRM4 (I-OS)");
        isCriticals.put(0xF0L, "CLSRM6 (I=OS)");
        isCriticals.put(0xF1L, "CLStreakSRM2 (I-OS)");
        isCriticals.put(0xF2L, "CLStreakSRM4 (I-OS)");
        isCriticals.put(0xF3L, "CLStreakSRM6 (I=OS)");
        isCriticals.put(0xF4L, "CLHeavyLargeLaser");
        isCriticals.put(0xF5L, "CLHeavyMediumLaser");
        isCriticals.put(0xF6L, "CLHeavySmallLaser");

        isCriticals.put(0x11DL, "ISTHBAngelECMSuite");
        isCriticals.put(0x11EL, "ISTHBBloodhoundActiveProbe");

        isCriticals.put(0x123L, "ISHeavyGaussRifle");

        isCriticals.put(0x01CEL, "ISAC2 Ammo");
        isCriticals.put(0x01CFL, "ISAC5 Ammo");
        isCriticals.put(0x01D0L, "ISAC10 Ammo");
        isCriticals.put(0x01d1L, "ISAC20 Ammo");
        isCriticals.put(0x01d2L, "ISAMS Ammo");
        isCriticals.put(0x01d3L, "Long Tom Cannon Ammo");
        isCriticals.put(0x01d4L, "Sniper Cannon Ammo");
        isCriticals.put(0x01d5L, "Thumper Cannon Ammo");
        isCriticals.put(0x01d6L, "ISLightGauss Ammo");
        isCriticals.put(0x01d7L, "ISGauss Ammo");

        isCriticals.put(0x01dbL, "ISLBXAC2 Ammo");
        isCriticals.put(0x01dcL, "ISLBXAC5 Ammo");
        isCriticals.put(0x01ddL, "ISLBXAC10 Ammo");
        isCriticals.put(0x01deL, "ISLBXAC20 Ammo");
        isCriticals.put(0x01dfL, "ISMG Ammo");

        isCriticals.put(0x1e0L, "ISLAC2 Ammo");
        isCriticals.put(0x1e1L, "ISLAC5 Ammo");
        isCriticals.put(0x1e2L, "ISHeavyFlamer Ammo");

        isCriticals.put(0x01e4L, "ISUltraAC2 Ammo");
        isCriticals.put(0x01e5L, "ISUltraAC5 Ammo");
        isCriticals.put(0x01e6L, "ISUltraAC10 Ammo");
        isCriticals.put(0x01e7L, "ISUltraAC20 Ammo");

        isCriticals.put(0x01EEL, "CLLightMG Ammo");
        isCriticals.put(0x01EFL, "CLHeavyMG Ammo");
        isCriticals.put(0x01f0L, "ISLRM5 Ammo");
        isCriticals.put(0x01f1L, "ISLRM10 Ammo");
        isCriticals.put(0x01f2L, "ISLRM15 Ammo");
        isCriticals.put(0x01f3L, "ISLRM20 Ammo");

        isCriticals.put(0x01f6L, "ISiNarc Pods");
        isCriticals.put(0x01f7L, "ISSRM2 Ammo");
        isCriticals.put(0x01f8L, "ISSRM4 Ammo");
        isCriticals.put(0x01f9L, "ISSRM6 Ammo");
        isCriticals.put(0x01faL, "ISStreakSRM2 Ammo");
        isCriticals.put(0x01fbL, "ISStreakSRM4 Ammo");
        isCriticals.put(0x01FCL, "ISStreakSRM6 Ammo");
        isCriticals.put(0x01FDL, "Thunderbolt-5 Ammo");
        isCriticals.put(0x01FEL, "Thunderbolt-10 Ammo");
        isCriticals.put(0x01FFL, "Thunderbolt-15 Ammo");
        isCriticals.put(0x0200L, "Thunderbolt-20 Ammo");
        isCriticals.put(0x0201L, "ISArrowIV Ammo");

        isCriticals.put(0x0209L, "ISNarc Pods");

        isCriticals.put(0x0215L, "ISVehicleFlamer Ammo");
        isCriticals.put(0x0216L, "ISLongTom Ammo");
        isCriticals.put(0x0217L, "ISSniper Ammo");
        isCriticals.put(0x0218L, "ISThumper Ammo");
        isCriticals.put(0x0219L, "ISMRM10 Ammo");
        isCriticals.put(0x021aL, "ISMRM20 Ammo");
        isCriticals.put(0x021bL, "ISMRM30 Ammo");
        isCriticals.put(0x021cL, "ISMRM40 Ammo");

        isCriticals.put(0x0222L, "ISLRTorpedo5 Ammo");
        isCriticals.put(0x0223L, "ISLRTorpedo10 Ammo");
        isCriticals.put(0x0224L, "ISLRTorpedo15 Ammo");
        isCriticals.put(0x0225L, "ISLRTorpedo20 Ammo");
        isCriticals.put(0x0226L, "ISSRT2 Ammo");
        isCriticals.put(0x0227L, "ISSRT4 Ammo");
        isCriticals.put(0x0228L, "ISSRT6 Ammo");

        isCriticals.put(0x0244L, "CLAMS Ammo");
        isCriticals.put(0x0245L, "CLGauss Ammo");
        isCriticals.put(0x0246L, "CLLBXAC2 Ammo");
        isCriticals.put(0x0247L, "CLLBXAC5 Ammo");
        isCriticals.put(0x0248L, "CLLBXAC10 Ammo");
        isCriticals.put(0x0249L, "CLLBXAC20 Ammo");
        isCriticals.put(0x024AL, "CLMG Ammo");
        isCriticals.put(0x024BL, "CLUltraAC2 Ammo");
        isCriticals.put(0x024CL, "CLUltraAC5 Ammo");
        isCriticals.put(0x024DL, "CLUltraAC10 Ammo");
        isCriticals.put(0x024EL, "CLUltraAC20 Ammo");
        isCriticals.put(0x024FL, "CLLRM5 Ammo");
        isCriticals.put(0x0250L, "CLLRM10 Ammo");
        isCriticals.put(0x0251L, "CLLRM15 Ammo");
        isCriticals.put(0x0252L, "CLLRM20 Ammo");
        isCriticals.put(0x0253L, "CLSRM2 Ammo");
        isCriticals.put(0x0254L, "CLSRM4 Ammo");
        isCriticals.put(0x0255L, "CLSRM6 Ammo");
        isCriticals.put(0x0256L, "CLStreakSRM2 Ammo");
        isCriticals.put(0x0257L, "CLStreakSRM4 Ammo");
        isCriticals.put(0x0258L, "CLStreakSRM6 Ammo");
        isCriticals.put(0x0259L, "CLArrowIV Ammo");

        isCriticals.put(0x025DL, "CLNarc Pods");

        isCriticals.put(0x026AL, "CLVehicleFlamer Ammo");
        isCriticals.put(0x026BL, "CLLongTom Ammo");
        isCriticals.put(0x026CL, "CLSniper Ammo");
        isCriticals.put(0x026DL, "CLThumper Ammo");
        isCriticals.put(0x026EL, "CLLRTorpedo5 Ammo");
        isCriticals.put(0x026FL, "CLLRTorpedo10 Ammo");
        isCriticals.put(0x0270L, "CLLRTorpedo15 Ammo");
        isCriticals.put(0x0271L, "CLLRTorpedo20 Ammo");
        isCriticals.put(0x0272L, "CLSRT2 Ammo");
        isCriticals.put(0x0273L, "CLSRT4 Ammo");
        isCriticals.put(0x0274L, "CLSRT6 Ammo");
        isCriticals.put(0x0275L, "CLStreakLRM5 Ammo");
        isCriticals.put(0x0276L, "CLStreakLRM10 Ammo");
        isCriticals.put(0x0277L, "CLStreakLRM15 Ammo");
        isCriticals.put(0x0278L, "CLStreakLRM20 Ammo");

        isCriticals.put(0x02b3L, "ISHeavyGauss Ammo");

        // criticals for mechs with a base type of clan
        Hashtable<Long, String> clanCriticals = new Hashtable<>();
        criticals.put(TechType.CLAN, clanCriticals);
        clanCriticals.put(0x0AL, "CLDouble Heat Sink");

        clanCriticals.put(0x12L, "CLTargeting Computer");

        clanCriticals.put(0x17L, "CLMASC");
        clanCriticals.put(0x18L, "CLArtemisIV");

        clanCriticals.put(0x21L, "Light Ferro-Fibrous"); // ?
        clanCriticals.put(0x22L, "Heavy Ferro-Fibrous"); // ?

        clanCriticals.put(0x26L, "CLCASEII");

        clanCriticals.put(0x33L, "CLERLargeLaser");
        clanCriticals.put(0x34L, "CLERMediumLaser");
        clanCriticals.put(0x35L, "CLERSmallLaser");
        clanCriticals.put(0x36L, "CLERPPC");
        clanCriticals.put(0x37L, "CLFlamer");
        clanCriticals.put(0x38L, "CLERLargePulseLaser");
        clanCriticals.put(0x39L, "CLERMediumPulseLaser");
        clanCriticals.put(0x3AL, "CLERSmallPulseLaser");
        clanCriticals.put(0x3BL, "CLLaserAMS");
        clanCriticals.put(0x3CL, "CLLargePulseLaser");
        clanCriticals.put(0x3DL, "CLMediumPulseLaser");
        clanCriticals.put(0x3EL, "CLSmallPulseLaser");
        clanCriticals.put(0x3FL, "CLAngelECMSuite");
        clanCriticals.put(0x40L, "CLAntiMissileSystem");
        clanCriticals.put(0x41L, "CLGaussRifle");
        clanCriticals.put(0x42L, "CLLBXAC2");
        clanCriticals.put(0x43L, "CLLBXAC5");
        clanCriticals.put(0x44L, "CLLBXAC10");
        clanCriticals.put(0x45L, "CLLBXAC20");
        clanCriticals.put(0x46L, "CLMG");
        clanCriticals.put(0x47L, "CLUltraAC2");
        clanCriticals.put(0x48L, "CLUltraAC5");
        clanCriticals.put(0x49L, "CLUltraAC10");
        clanCriticals.put(0x4AL, "CLUltraAC20");
        clanCriticals.put(0x4BL, "CLLRM5");
        clanCriticals.put(0x4CL, "CLLRM10");
        clanCriticals.put(0x4DL, "CLLRM15");
        clanCriticals.put(0x4EL, "CLLRM20");
        clanCriticals.put(0x4FL, "CLSRM2");
        clanCriticals.put(0x50L, "CLSRM4");
        clanCriticals.put(0x51L, "CLSRM6");
        clanCriticals.put(0x52L, "CLStreakSRM2");
        clanCriticals.put(0x53L, "CLStreakSRM4");
        clanCriticals.put(0x54L, "CLStreakSRM6");
        clanCriticals.put(0x55L, "CLArrowIVSystem");
        clanCriticals.put(0x56L, "CLAntiPersonnelPod");
        clanCriticals.put(0x57L, "CLActiveProbe");
        clanCriticals.put(0x58L, "CLECMSuite");
        clanCriticals.put(0x59L, "CLNarcBeacon");
        clanCriticals.put(0x5AL, "CLTAG");
        clanCriticals.put(0x5BL, "CLERMicroLaser");
        clanCriticals.put(0x5CL, "CLLRM5 (OS)");
        clanCriticals.put(0x5DL, "CLLRM10 (OS)");
        clanCriticals.put(0x5EL, "CLLRM15 (OS)");
        clanCriticals.put(0x5FL, "CLLRM20 (OS)");
        clanCriticals.put(0x60L, "CLSRM2 (OS)");
        clanCriticals.put(0x61L, "CLSRM4 (OS)");
        clanCriticals.put(0x62L, "CLSRM6 (OS)");
        clanCriticals.put(0x63L, "CLStreakSRM2 (OS)");
        clanCriticals.put(0x64L, "CLStreakSRM4 (OS)");
        clanCriticals.put(0x65L, "CLStreakSRM6 (OS)");
        clanCriticals.put(0x66L, "CLVehicleFlamer");
        clanCriticals.put(0x67L, "CLLongTomArtillery");
        clanCriticals.put(0x68L, "CLSniperArtillery");
        clanCriticals.put(0x69L, "CLThumperArtillery");
        clanCriticals.put(0x6AL, "CLLRTorpedo5");
        clanCriticals.put(0x6BL, "CLLRTorpedo10");
        clanCriticals.put(0x6CL, "CLLRTorpedo15");
        clanCriticals.put(0x6DL, "CLLRTorpedo20");
        clanCriticals.put(0x6EL, "CLSRT2");
        clanCriticals.put(0x6FL, "CLSRT4");
        clanCriticals.put(0x70L, "CLSRT6");

        clanCriticals.put(0x71L, "CLStreakLRM5");
        clanCriticals.put(0x72L, "CLStreakLRM10");
        clanCriticals.put(0x73L, "CLStreakLRM15");
        clanCriticals.put(0x74L, "CLStreakLRM20");
        clanCriticals.put(0x75L, "CLGrenadeLauncher");
        clanCriticals.put(0x76L, "CLLRM5 (I-OS)");
        clanCriticals.put(0x77L, "CLLRM10 (I-OS)");
        clanCriticals.put(0x78L, "CLLRM15 (I-OS)");
        clanCriticals.put(0x79L, "CLLRM20 (I-OS)");
        clanCriticals.put(0x7aL, "CLSRM2 (I-OS)");
        clanCriticals.put(0x7bL, "CLSRM4 (I-OS)");
        clanCriticals.put(0x7cL, "CLSRM6 (I=OS)");
        clanCriticals.put(0x7dL, "CLStreakSRM2 (I-OS)");
        clanCriticals.put(0x7eL, "CLStreakSRM4 (I-OS)");
        clanCriticals.put(0x7fL, "CLStreakSRM6 (I=OS)");
        clanCriticals.put(0x80L, "CLHeavyLargeLaser");
        clanCriticals.put(0x81L, "CLHeavyMediumLaser");
        clanCriticals.put(0x82L, "CLHeavySmallLaser");
        clanCriticals.put(0x83L, "ISERLargeLaser");
        clanCriticals.put(0x84L, "ISERPPC");
        clanCriticals.put(0x85L, "ISFlamer");
        clanCriticals.put(0x86L, "ISLaserAMS");
        clanCriticals.put(0x87L, "ISLargeLaser");
        clanCriticals.put(0x88L, "ISMediumLaser");
        clanCriticals.put(0x89L, "ISSmallLaser");
        clanCriticals.put(0x8AL, "ISPPC");
        clanCriticals.put(0x8BL, "ISLargePulseLaser");
        clanCriticals.put(0x8CL, "ISMediumPulseLaser");
        clanCriticals.put(0x8DL, "ISSmallPulseLaser");
        clanCriticals.put(0x8EL, "ISAC2");
        clanCriticals.put(0x8FL, "ISAC5");
        clanCriticals.put(0x90L, "ISAC10");
        clanCriticals.put(0x91L, "ISAC20");
        clanCriticals.put(0x92L, "ISAntiMissileSystem");
        clanCriticals.put(0x93L, "Long Tom Cannon");
        clanCriticals.put(0x94L, "Sniper Cannon");
        clanCriticals.put(0x95L, "Thumper Cannon");
        clanCriticals.put(0x96L, "ISLightGaussRifle");
        clanCriticals.put(0x97L, "ISGaussRifle");
        clanCriticals.put(0x98L, "ISLargeXPulseLaser");
        clanCriticals.put(0x99L, "ISMediumXPulseLaser");
        clanCriticals.put(0x9AL, "ISSmallXPulseLaser");
        clanCriticals.put(0x9BL, "ISLBXAC2");
        clanCriticals.put(0x9CL, "ISLBXAC5");
        clanCriticals.put(0x9DL, "ISLBXAC10");
        clanCriticals.put(0x9EL, "ISLBXAC20");
        clanCriticals.put(0x9FL, "ISMachine Gun");
        clanCriticals.put(0xA0L, "ISLAC2");
        clanCriticals.put(0xA1L, "ISLAC5");

        clanCriticals.put(0xA3L, "ISPPCCapacitor"); // HMP uses this code for ERPPC
        clanCriticals.put(0xA4L, "ISUltraAC2");
        clanCriticals.put(0xA5L, "ISUltraAC5");
        clanCriticals.put(0xA6L, "ISUltraAC10");
        clanCriticals.put(0xA7L, "ISUltraAC20");
        clanCriticals.put(0xA8L, "CLMicroPulseLaser");
        clanCriticals.put(0xA9L, "ISPPCCapacitor"); // HMP uses this code for PPC

        clanCriticals.put(0xAAL, "ISERMediumLaser");
        clanCriticals.put(0xABL, "ISERSmallLaser");
        clanCriticals.put(0xACL, "ISAntiPersonnelPod");

        clanCriticals.put(0xADL, "CLLightMG");
        clanCriticals.put(0xAEL, "CLHeavyMG");
        clanCriticals.put(0xAFL, "CLLightActiveProbe");

        clanCriticals.put(0xB0L, "ISLRM5");
        clanCriticals.put(0xB1L, "ISLRM10");
        clanCriticals.put(0xB2L, "ISLRM15");
        clanCriticals.put(0xB3L, "ISLRM20");
        clanCriticals.put(0xB4L, "CLLightTAG");
        clanCriticals.put(0xCFL, "Thunderbolt (OS)");
        clanCriticals.put(0xB6L, "ISImprovedNarc");
        clanCriticals.put(0xB7L, "ISSRM2");
        clanCriticals.put(0xB8L, "ISSRM4");
        clanCriticals.put(0xB9L, "ISSRM6");
        clanCriticals.put(0xBAL, "ISStreakSRM2");
        clanCriticals.put(0xBBL, "ISStreakSRM4");
        clanCriticals.put(0xBCL, "ISStreakSRM6");
        clanCriticals.put(0xBDL, "ISThunderbolt5");
        clanCriticals.put(0xBEL, "ISThunderbolt10");
        clanCriticals.put(0xBFL, "ISThunderbolt15");
        clanCriticals.put(0xC0L, "ISThunderbolt20");

        clanCriticals.put(0xC2L, "ISAngelECMSuite");
        clanCriticals.put(0xC3L, "ISBeagleActiveProbe");
        clanCriticals.put(0xC4L, "ISBloodhoundActiveProbe");
        clanCriticals.put(0xC5L, "ISC3MasterComputer");
        clanCriticals.put(0xC6L, "ISC3SlaveUnit");
        clanCriticals.put(0xC7L, "ISImprovedC3CPU");
        clanCriticals.put(0xC8L, "ISGuardianECM");
        clanCriticals.put(0xC9L, "ISNarcBeacon");
        clanCriticals.put(0xCAL, "ISTAG");

        clanCriticals.put(0xCBL, "ISLRM5 (OS)");
        clanCriticals.put(0xCCL, "ISLRM10 (OS)");
        clanCriticals.put(0xCDL, "ISLRM15 (OS)");
        clanCriticals.put(0xCEL, "ISLRM20 (OS)");
        clanCriticals.put(0xD0L, "ISSRM4 (OS)");
        clanCriticals.put(0xD1L, "ISSRM6 (OS)");
        clanCriticals.put(0xD2L, "ISStreakSRM2 (OS)");
        clanCriticals.put(0xD3L, "ISStreakSRM4 (OS)");
        clanCriticals.put(0xD4L, "ISStreakSRM6 (OS)");
        clanCriticals.put(0xD5L, "ISVehicleFlamer");
        clanCriticals.put(0xD6L, "ISLongTomArtillery");
        clanCriticals.put(0xD7L, "ISSniperArtillery");
        clanCriticals.put(0xD8L, "ISThumperArtillery");
        clanCriticals.put(0xD9L, "ISMRM10");
        clanCriticals.put(0xDAL, "ISMRM20");
        clanCriticals.put(0xDBL, "ISMRM30");
        clanCriticals.put(0xDCL, "ISMRM40");
        clanCriticals.put(0xDDL, "Grenade Launcher");
        clanCriticals.put(0xDEL, "ISMRM10 (OS)");
        clanCriticals.put(0xDFL, "ISMRM20 (OS)");
        clanCriticals.put(0xE0L, "ISMRM30 (OS)");
        clanCriticals.put(0xE1L, "ISMRM40 (OS)");
        clanCriticals.put(0xE2L, "ISLRTorpedo5");
        clanCriticals.put(0xE3L, "ISLRTorpedo10");
        clanCriticals.put(0xE4L, "ISLRTorpedo15");
        clanCriticals.put(0xE5L, "ISLRTorpedo20");
        clanCriticals.put(0xE6L, "ISSRT2");
        clanCriticals.put(0xE7L, "ISSRT4");
        clanCriticals.put(0xE8L, "ISSRT6");
        clanCriticals.put(0xE9L, "ISLRM5 (I-OS)");
        clanCriticals.put(0xEAL, "ISLRM10 (I-OS)");
        clanCriticals.put(0xEBL, "ISLRM15 (I-OS)");
        clanCriticals.put(0xECL, "ISLRM20 (I-OS)");
        clanCriticals.put(0xEDL, "ISSRM2 (I-OS)");
        clanCriticals.put(0xEEL, "ISSRM4 (I-OS)");
        clanCriticals.put(0xEfL, "ISSRM6 (I-OS)");
        clanCriticals.put(0xF0L, "ISStreakSRM2 (I-OS)");
        clanCriticals.put(0xF1L, "ISStreakSRM4 (I-OS)");
        clanCriticals.put(0xF2L, "ISStreakSRM6 (I-OS)");
        clanCriticals.put(0xF3L, "ISMRM10 (I-OS)");
        clanCriticals.put(0xF4L, "ISMRM20 (I-OS)");
        clanCriticals.put(0xF5L, "ISMRM30 (I-OS)");
        clanCriticals.put(0xF6L, "ISMRM40 (I-OS)");

        // clanCriticals.put(Long.valueOf(0x01ce), "CLAC2 Ammo");
        clanCriticals.put(0x01d0L, "CLAMS Ammo");
        // clanCriticals.put(Long.valueOf(0x01cf), "CLAC5 Ammo");
        clanCriticals.put(0x01d1L, "CLGauss Ammo");
        clanCriticals.put(0x01d2L, "CLLBXAC2 Ammo");
        clanCriticals.put(0x01d3L, "CLLBXAC5 Ammo");
        clanCriticals.put(0x01d4L, "CLLBXAC10 Ammo");
        clanCriticals.put(0x01d5L, "CLLBXAC20 Ammo");
        clanCriticals.put(0x01d6L, "CLMG Ammo");
        clanCriticals.put(0x01d7L, "CLUltraAC2 Ammo");
        clanCriticals.put(0x01d8L, "CLUltraAC5 Ammo");
        clanCriticals.put(0x01d9L, "CLUltraAC10 Ammo");
        clanCriticals.put(0x01daL, "CLUltraAC20 Ammo");
        clanCriticals.put(0x01dbL, "CLLRM5 Ammo");
        clanCriticals.put(0x01dcL, "CLLRM10 Ammo");
        clanCriticals.put(0x01ddL, "CLLRM15 Ammo");
        clanCriticals.put(0x01deL, "CLLRM20 Ammo");
        clanCriticals.put(0x01dfL, "CLSRM2 Ammo");
        clanCriticals.put(0x01e0L, "CLSRM4 Ammo");
        clanCriticals.put(0x01e1L, "CLSRM6 Ammo");
        clanCriticals.put(0x01e2L, "CLStreakSRM2 Ammo");
        clanCriticals.put(0x01e3L, "CLStreakSRM4 Ammo");
        clanCriticals.put(0x01e4L, "CLStreakSRM6 Ammo");
        clanCriticals.put(0x01e5L, "CLArrowIV Ammo");
        clanCriticals.put(0x01e9L, "CLNarc Pods");
        // clanCriticals.put(Long.valueOf(0x0215), "CLFlamer Ammo");

        clanCriticals.put(0x01f0L, "CLLRM5 Ammo");
        clanCriticals.put(0x01f1L, "CLLRM10 Ammo");
        clanCriticals.put(0x01f2L, "CLLRM15 Ammo");
        clanCriticals.put(0x01f3L, "CLLRM20 Ammo");
        clanCriticals.put(0x01f6L, "CLVehicleFlamer Ammo");
        clanCriticals.put(0x01f7L, "CLLongTom Ammo");
        clanCriticals.put(0x01f8L, "CLSniper Ammo");
        clanCriticals.put(0x01f9L, "CLThumper Ammo");
        clanCriticals.put(0x01faL, "CLLRTorpedo5 Ammo");
        clanCriticals.put(0x01fbL, "CLLRTorpedo10 Ammo");
        clanCriticals.put(0x01fcL, "CLLRTorpedo15 Ammo");
        clanCriticals.put(0x01fdL, "CLLRTorpedo20 Ammo");
        clanCriticals.put(0x01feL, "CLSRT2 Ammo");
        clanCriticals.put(0x01ffL, "CLSRT4 Ammo");
        clanCriticals.put(0x0200L, "CLSRT6 Ammo");
        clanCriticals.put(0x0201L, "CLStreakLRM5 Ammo");
        clanCriticals.put(0x0202L, "CLStreakLRM10 Ammo");
        clanCriticals.put(0x0203L, "CLStreakLRM15 Ammo");
        clanCriticals.put(0x0204L, "CLStreakLRM20 Ammo");

        clanCriticals.put(0x021EL, "ISAC2 Ammo");
        clanCriticals.put(0x021FL, "ISAC5 Ammo");
        clanCriticals.put(0x0220L, "ISAC10 Ammo");
        clanCriticals.put(0x0221L, "ISAC20 Ammo");
        clanCriticals.put(0x0222L, "ISAMS Ammo");
        clanCriticals.put(0x0223L, "Long Tom Cannon Ammo");
        clanCriticals.put(0x0224L, "Sniper Cannon Ammo");
        clanCriticals.put(0x0225L, "Thumper Cannon Ammo");
        clanCriticals.put(0x0226L, "ISLightGauss Ammo");
        clanCriticals.put(0x0227L, "ISGauss Ammo");
        // clanCriticals.put(Long.valueOf(0x0228), "CLSRTorpedo6 Ammo");

        clanCriticals.put(0x022BL, "ISLBXAC2 Ammo");
        clanCriticals.put(0x022CL, "ISLBXAC5 Ammo");
        clanCriticals.put(0x022DL, "ISLBXAC10 Ammo");
        clanCriticals.put(0x022EL, "ISLBXAC20 Ammo");
        clanCriticals.put(0x022FL, "ISMG Ammo");
        clanCriticals.put(0x0230L, "ISLAC2 Ammo");
        clanCriticals.put(0x0231L, "ISLAC5 Ammo");

        clanCriticals.put(0x0234L, "ISUltraAC2 Ammo");
        clanCriticals.put(0x0235L, "ISUltraAC5 Ammo");
        clanCriticals.put(0x0236L, "ISUltraAC10 Ammo");
        clanCriticals.put(0x0237L, "ISUltraAC20 Ammo");

        clanCriticals.put(0x023dL, "CLLightMG Ammo");
        clanCriticals.put(0x023eL, "CLHeavyMG Ammo");

        clanCriticals.put(0x0240L, "ISLRM5 Ammo");
        clanCriticals.put(0x0241L, "ISLRM10 Ammo");
        clanCriticals.put(0x0242L, "ISLRM15 Ammo");
        clanCriticals.put(0x0243L, "ISLRM20 Ammo");

        clanCriticals.put(0x0246L, "ISiNarc Pods");
        clanCriticals.put(0x0247L, "ISSRM2 Ammo");
        clanCriticals.put(0x0248L, "ISSRM4 Ammo");
        clanCriticals.put(0x0249L, "ISSRM6 Ammo");
        clanCriticals.put(0x024AL, "ISStreakSRM2 Ammo");
        clanCriticals.put(0x024BL, "ISStreakSRM4 Ammo");
        clanCriticals.put(0x024CL, "ISStreakSRM6 Ammo");
        clanCriticals.put(0x024DL, "ISThunderbolt5 Ammo");
        clanCriticals.put(0x024EL, "ISThunderbolt10 Ammo");
        clanCriticals.put(0x024FL, "ISThunderbolt15 Ammo");
        clanCriticals.put(0x0250L, "ISThunderbolt20 Ammo");

        clanCriticals.put(0x0259L, "ISNarc Pods");

        clanCriticals.put(0x0265L, "ISVehicleFlamer Ammo");
        clanCriticals.put(0x0266L, "ISLongTomArtillery Ammo");
        clanCriticals.put(0x0267L, "ISSniperArtillery Ammo");
        clanCriticals.put(0x0268L, "ISThumperArtillery Ammo");
        clanCriticals.put(0x0269L, "ISMRM10 Ammo");
        clanCriticals.put(0x026AL, "ISMRM20 Ammo");
        clanCriticals.put(0x026BL, "ISMRM30 Ammo");
        clanCriticals.put(0x026CL, "ISMRM40 Ammo");

        clanCriticals.put(0x0272L, "ISLRTorpedo15 Ammo");
        clanCriticals.put(0x0273L, "ISLRTorpedo20 Ammo");
        clanCriticals.put(0x0274L, "ISLRTorpedo5 Ammo");
        clanCriticals.put(0x0275L, "ISLRTorpedo10 Ammo");
        clanCriticals.put(0x0276L, "ISSRT4 Ammo");
        clanCriticals.put(0x0277L, "ISSRT2 Ammo");
        clanCriticals.put(0x0278L, "ISSRT6 Ammo");

        // special for ammo mutator
        // 28c-28f = atm
        criticals.put(0x10000028cL, "CLATM3 ER Ammo");
        criticals.put(0x20000028cL, "CLATM3 HE Ammo");
        criticals.put(0x10000028dL, "CLATM6 ER Ammo");
        criticals.put(0x20000028dL, "CLATM6 HE Ammo");
        criticals.put(0x10000028eL, "CLATM9 ER Ammo");
        criticals.put(0x20000028eL, "CLATM9 HE Ammo");
        criticals.put(0x10000028fL, "CLATM12 ER Ammo");
        criticals.put(0x20000028fL, "CLATM12 HE Ammo");
        // 1db-1de = is
        // 1d2-1d5 = cl
        // 298-299 = thb
        // 22B-22E = IS on clan
        // 246-249 = clan on IS
        isCriticals.put(0x1000001dbL, "ISLBXAC2 CL Ammo");
        isCriticals.put(0x1000001dcL, "ISLBXAC5 CL Ammo");
        isCriticals.put(0x1000001ddL, "ISLBXAC10 CL Ammo");
        isCriticals.put(0x1000001deL, "ISLBXAC20 CL Ammo");
        isCriticals.put(0x100000246L, "CLLBXAC2 CL Ammo");
        isCriticals.put(0x100000247L, "CLLBXAC5 CL Ammo");
        isCriticals.put(0x100000248L, "CLLBXAC10 CL Ammo");
        isCriticals.put(0x100000249L, "CLLBXAC20 CL Ammo");
        clanCriticals.put(0x10000022bL, "ISLBXAC2 CL Ammo");
        clanCriticals.put(0x10000022cL, "ISLBXAC5 CL Ammo");
        clanCriticals.put(0x10000022dL, "ISLBXAC10 CL Ammo");
        clanCriticals.put(0x10000022eL, "ISLBXAC20 CL Ammo");
        clanCriticals.put(0x1000001d2L, "CLLBXAC2 CL Ammo");
        clanCriticals.put(0x1000001d3L, "CLLBXAC5 CL Ammo");
        clanCriticals.put(0x1000001d4L, "CLLBXAC10 CL Ammo");
        clanCriticals.put(0x1000001d5L, "CLLBXAC20 CL Ammo");
        criticals.put(0x100000298L, "ISLBXAC2 Ammo (THB)");
        criticals.put(0x100000299L, "ISLBXAC5 Ammo (THB)");
        criticals.put(0x10000029AL, "ISLBXAC20 Ammo (THB)");
    }

    private String getCriticalName(long critical) {
        return getCriticalName(Long.valueOf(critical));
    }

    private String getCriticalName(Long critical) {
        // Critical slots are 4 bytes long. The first two bytes are
        // the type, the third is the ammo count, and I don't know
        // what the fourth is.

        short ammoCount = 0;
        if (critical > Short.MAX_VALUE) {
            // Grab the ammo count from the third byte. It is stored as
            // an unsigned 8-bit number, so we'll fit it into a signed
            // 16-bit number.
            ammoCount = (short) ((critical >> 16) & 0xFF);
            // Mask off everything but the first two bytes.
            critical = critical & 0xFFFF;
        }

        // At this point, the critical value is an unsigned integer.

        // First try "shared" criticals.
        String critName = (String) criticals.get(critical);

        if (critName == null) {
            TechType tType = techType;

            if (tType == TechType.MIXED) {
                if (critical.intValue() == 0x0A) {
                    tType = heatSinkTechType;
                } else if ((critical.intValue() == 0x11) || (critical.intValue() == 0x1F)) {
                    tType = physicalWeaponTechType;
                } else if (critical.intValue() == 0x12) {
                    tType = targetingComputerTechType;
                } else if (critical.intValue() == 0x17) {
                    tType = myomerTechType;
                } else {
                    // Mixed tech mechs lookup most equipment using their "base" or "preferred" technology type.
                    tType = mixedBaseTechType;
                }
            }

            // Attempt to lookup equipment using the appropriate tech type.
            Hashtable<?, ?> techCriticals = (Hashtable<?, ?>) criticals.get(tType);
            if (techCriticals != null) {
                critName = (String) techCriticals.get(critical);
            }
        }

        // MG ammo can come in half ton increments, so we have to look
        // up the actual ammo count. Other weapons have their counts
        // hard-coded.
        if ((critName != null) && critName.endsWith("MG Ammo")) {
            critName += " (" + ammoCount + ")";
        }

        if ((critName == null) && (critical == 0)) {
            return "-Empty-";
        }

        // Unexpected parsing failures should be passed on so that
        // they can be dealt with properly.
        if (critName == null) {
            critName = "UnknownCritical(0x" + Integer.toHexString(critical.intValue()) + ")";
        }

        if (ammoCount > 0) {
            critName = mutateLBXAmmo(critName);
            critName = mutateATMAmmo(critName);
        }

        return critName;
    }

    /**
     * This function moves all "empty" slots to the end of a location's critical
     * list. MegaMek adds equipment to the first empty slot available in a
     * location. This means that any "holes" (empty slots not at the end of a
     * location), will cause the file crits and MegaMek's crits to become out of
     * sync.
     */
    private void compactCriticals(long... criticals) {
        for (int x = 0; x < criticals.length; x++) {
            int firstEmpty = -1;
            for (int slot = 0; slot < criticals.length; slot++) {
                if (criticals[slot] == 0) {
                    firstEmpty = slot;
                }
                if ((firstEmpty != -1) && (criticals[slot] != 0)) {
                    // move this to the first empty slot
                    criticals[firstEmpty] = criticals[slot];
                    // mark the old slot empty
                    criticals[slot] = 0;
                    // restart just after the moved slot's new location
                    slot = firstEmpty;
                    firstEmpty = -1;
                }
            }
        }
    }

    public String getMtf() {
        StringBuilder sb = new StringBuilder();
        String nl = "\r\n"; // DOS friendly

        // Write Output MTF
        sb.append("Version:1.0").append(nl);
        sb.append(name).append(nl);
        sb.append(model).append(nl);
        sb.append(nl);

        sb.append("Config:").append(chassisType);
        sb.append(nl);
        sb.append("TechBase:").append(techType);
        if (techType == TechType.MIXED) {
            sb.append(" (");
            if (mixedBaseTechType == TechType.INNER_SPHERE) {
                // MtfFile expects abbreviated form of Inner Sphere if
                // mixed tech is involved.
                sb.append("IS");
            } else {
                sb.append(mixedBaseTechType);
            }
            sb.append(" Chassis)");
        }
        sb.append(nl);
        sb.append("Era:").append(year).append(nl);
        sb.append("Rules Level:").append(rulesLevel);
        sb.append(nl);
        sb.append(nl);

        sb.append("Mass:").append(tonnage).append(nl);
        sb.append("Engine:").append(engineRating).append(" ").append(engineType).append(" Engine");
        if (mixedBaseTechType != engineTechType) {
            sb.append(" (").append(engineTechType).append(")");
        }
        sb.append(nl);
        sb.append("Structure:").append(internalStructureType).append(nl);
        sb.append("Myomer:").append(myomerType).append(nl);
        if (gyroType != Mech.GYRO_STANDARD) {
            sb.append("Gyro:").append(Mech.getGyroTypeString(gyroType)).append(nl);
        }
        if (cockpitType != Mech.COCKPIT_STANDARD) {
            sb.append("Cockpit:").append(Mech.getCockpitTypeString(cockpitType)).append(nl);
        }
        sb.append(nl);

        sb.append("Heat Sinks:").append(heatSinks).append(" ").append(heatSinkType).append(nl);
        sb.append("Walk MP:").append(walkMP).append(nl);
        sb.append("Jump MP:").append(jumpMP).append(nl);
        sb.append(nl);

        sb.append("Armor:").append(armorType);
        if (mixedBaseTechType != armorTechType) {
            sb.append(" (").append(armorTechType).append(")");
        }
        sb.append(nl);
        boolean isPatchwork = armorType == ArmorType.PATCHWORK;
        sb.append("LA Armor:").append(laArmor);
        if (isPatchwork) {
            sb.append(" (").append(laArmorType).append(")");
        }
        sb.append(nl);
        sb.append("RA Armor:").append(raArmor);
        if (isPatchwork) {
            sb.append(" (").append(raArmorType).append(")");
        }
        sb.append(nl);
        sb.append("LT Armor:").append(ltArmor);
        if (isPatchwork) {
            sb.append(" (").append(ltArmorType).append(")");
        }
        sb.append(nl);
        sb.append("RT Armor:").append(rtArmor);
        if (isPatchwork) {
            sb.append(" (").append(rtArmorType).append(")");
        }
        sb.append(nl);
        sb.append("CT Armor:").append(ctArmor);
        if (isPatchwork) {
            sb.append(" (").append(ctArmorType).append(")");
        }
        sb.append(nl);
        sb.append("HD Armor:").append(headArmor);
        if (isPatchwork) {
            sb.append(" (").append(headArmorType).append(")");
        }
        sb.append(nl);
        sb.append("LL Armor:").append(llArmor);
        if (isPatchwork) {
            sb.append(" (").append(llArmorType).append(")");
        }
        sb.append(nl);
        sb.append("RL Armor:").append(rlArmor);
        if (isPatchwork) {
            sb.append(" (").append(rlArmorType).append(")");
        }
        sb.append(nl);
        sb.append("RTL Armor:").append(ltrArmor);
        if (isPatchwork) {
            sb.append(" (").append(ltrArmorType).append(")");
        }
        sb.append(nl);
        sb.append("RTR Armor:").append(rtrArmor);
        if (isPatchwork) {
            sb.append(" (").append(rtrArmorType).append(")");
        }
        sb.append(nl);
        sb.append("RTC Armor:").append(ctrArmor);
        if (isPatchwork) {
            sb.append(" (").append(ctrArmorType).append(")");
        }
        sb.append(nl);
        sb.append(nl);

        sb.append("Weapons:").append(totalWeaponCount).append(nl);
        for (int x = 0; x < totalWeaponCount; x++) {
            sb.append(weaponArray[x][0]).append(" ").append(getCriticalName(weaponArray[x][1])).append(", ").append(WeaponLocation.getType(weaponArray[x][2]));
            if (weaponArray[x][3] > 0) {
                sb.append(", Ammo:").append(weaponArray[x][3]);
            }
            sb.append(nl);
        }

        sb.append(nl);
        sb.append("Left Arm:").append(nl);
        for (int x = 0; x < 12; x++) {
            sb.append(getCriticalName(laCriticals[x])).append(nl);
        }
        sb.append(nl);

        sb.append("Right Arm:").append(nl);
        for (int x = 0; x < 12; x++) {
            sb.append(getCriticalName(raCriticals[x])).append(nl);
        }
        sb.append(nl);

        sb.append("Left Torso:").append(nl);
        for (int x = 0; x < 12; x++) {
            sb.append(getCriticalName(ltCriticals[x])).append(nl);
        }
        sb.append(nl);

        sb.append("Right Torso:").append(nl);
        for (int x = 0; x < 12; x++) {
            sb.append(getCriticalName(rtCriticals[x])).append(nl);
        }
        sb.append(nl);

        sb.append("Center Torso:").append(nl);
        for (int x = 0; x < 12; x++) {
            sb.append(getCriticalName(ctCriticals[x])).append(nl);
        }
        sb.append(nl);

        sb.append("Head:").append(nl);
        for (int x = 0; x < 12; x++) {
            sb.append(getCriticalName(headCriticals[x])).append(nl);
        }
        sb.append(nl);

        sb.append("Left Leg:").append(nl);
        for (int x = 0; x < 12; x++) {
            sb.append(getCriticalName(llCriticals[x])).append(nl);
        }
        sb.append(nl);

        sb.append("Right Leg:").append(nl);
        for (int x = 0; x < 12; x++) {
            sb.append(getCriticalName(rlCriticals[x])).append(nl);
        }

        return sb.toString();
    }

    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Hmpread (Java Edition) version 1.3");
            System.out.println("--------------------------------------");
            System.out.println();
            System.out.println("Drag and drop \".hmp\" files onto this exe to convert them to \".mtf\" files.\nMultiple files may be processed at once.  Files may also be specified on\nthe command line.");
            System.out.println();
            System.out.println("Press <enter> to quit...");
            try {
                System.in.read(); // pause
            } catch (Exception e) {
                // ignore
            }
            return;
        }
        for (String arg : args) {
            String filename = arg;
            if (!filename.endsWith(".hmp")) {
                System.out.println("Error: Input file must have Heavy Metal Pro extension '.hmp'");
                System.out.println();
                System.out.println("Press <enter> to quit...");
                try {
                    System.in.read(); // pause
                } catch (Exception e) {
                    // ignore
                }
                return;
            }
            HmpFile hmpFile;
            try (InputStream is = new FileInputStream(arg)) {
                hmpFile = new HmpFile(is);
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
                return;
            }
            filename = filename.substring(0, filename.lastIndexOf(".hmp")) + ".mtf";
            try (FileWriter fw = new FileWriter(filename); BufferedWriter out = new BufferedWriter(fw)) {
                out.write(hmpFile.getMtf());
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }
        }
    }
}

abstract class HMPType {
    private String name;

    private int id;

    protected HMPType(String name, int id) {
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
        final HMPType other = (HMPType) obj;
        return Objects.equals(name, other.name) && (id == other.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }
}

class ArmorType extends HMPType {
    public static final Hashtable<Integer, ArmorType> types = new Hashtable<>();

    public static final ArmorType STANDARD = new ArmorType("Standard", 0);
    public static final ArmorType REACTIVE = new ArmorType("Reactive", 2);
    public static final ArmorType REFLECTIVE = new ArmorType("Reflective", 3);
    public static final ArmorType HARDENED = new ArmorType("Hardened", 4);
    public static final ArmorType LIGHT_FERRO_FIBROUS = new ArmorType("Light Ferro-Fibrous", 5);
    public static final ArmorType PATCHWORK = new ArmorType("Patchwork", 7);
    public static final ArmorType STEALTH = new ArmorType("Stealth", 8);

    private ArmorType(String name, int id) {
        super(name, id);
        types.put(id, this);
    }

    public static ArmorType getType(int i) {
        return types.get(i);
    }
}

class EngineType extends HMPType {
    public static final Hashtable<Integer, EngineType> types = new Hashtable<>();

    public static final EngineType FUSION = new EngineType("Fusion", 0);
    public static final EngineType XL = new EngineType("XL", 1);
    public static final EngineType XXL = new EngineType("XXL", 2);
    public static final EngineType COMPACT = new EngineType("Compact", 3);
    public static final EngineType ICE = new EngineType("I.C.E.", 4);
    public static final EngineType LIGHT = new EngineType("Light", 5);

    private EngineType(String name, int id) {
        super(name, id);
        types.put(id, this);
    }

    public static EngineType getType(int i) {
        return types.get(i);
    }
}

class HeatSinkType extends HMPType {
    public static final Hashtable<Integer, HeatSinkType> types = new Hashtable<>();

    public static final HeatSinkType SINGLE = new HeatSinkType("Single", 0);
    public static final HeatSinkType DOUBLE = new HeatSinkType("Double", 1);
    public static final HeatSinkType COMPACT = new HeatSinkType("Compact", 2);
    public static final HeatSinkType LASER = new HeatSinkType("Laser", 3);

    private HeatSinkType(String name, int id) {
        super(name, id);
        types.put(id, this);
    }

    public static HeatSinkType getType(int i) {
        return types.get(i);
    }
}

class ChassisType extends HMPType {
    public static final Hashtable<Integer, ChassisType> types = new Hashtable<>();

    public static final ChassisType BIPED = new ChassisType("Biped", 0);
    public static final ChassisType QUADRAPED = new ChassisType("Quadraped", 1);
    public static final ChassisType LAM = new ChassisType("LAM", 2);
    public static final ChassisType ARMLESS = new ChassisType("Armless", 3);
    public static final ChassisType BIPED_OMNI = new ChassisType("Biped Omni", 10);
    public static final ChassisType QUADRAPED_OMNI = new ChassisType("Quadraped Omni", 11);

    private ChassisType(String name, int id) {
        super(name, id);
        types.put(id, this);
    }

    public static ChassisType getType(int i) {
        return types.get(i);
    }
}

class InternalStructureType extends HMPType {
    public static final Hashtable<Integer, InternalStructureType> types = new Hashtable<>();

    public static final InternalStructureType STANDARD = new InternalStructureType(EquipmentType.getStructureTypeName(EquipmentType.T_STRUCTURE_STANDARD), 0);
    public static final InternalStructureType COMPOSITE = new InternalStructureType(EquipmentType.getStructureTypeName(EquipmentType.T_STRUCTURE_COMPOSITE), 2);
    public static final InternalStructureType REINFORCED = new InternalStructureType(EquipmentType.getStructureTypeName(EquipmentType.T_STRUCTURE_REINFORCED), 3);
    public static final InternalStructureType UTILITY = new InternalStructureType("Utility", 4);

    private InternalStructureType(String name, int id) {
        super(name, id);
        types.put(id, this);
    }

    public static InternalStructureType getType(int i) {
        return types.get(i);
    }
}

class TechType extends HMPType {
    public static final Hashtable<Integer, TechType> types = new Hashtable<>();

    public static final TechType INNER_SPHERE = new TechType("Inner Sphere", 0);
    public static final TechType CLAN = new TechType("Clan", 1);
    public static final TechType MIXED = new TechType("Mixed", 2);

    private TechType(String name, int id) {
        super(name, id);
        types.put(id, this);
    }

    public static TechType getType(int i) {
        return types.get(i);
    }
}

class MyomerType extends HMPType {
    public static final Hashtable<Integer, MyomerType> types = new Hashtable<>();

    public static final MyomerType STANDARD = new MyomerType("Standard", 0);
    public static final MyomerType TRIPLE_STRENGTH = new MyomerType("Triple-Strength", 1);
    public static final MyomerType MASC = new MyomerType("MASC", 2);

    private MyomerType(String name, int id) {
        super(name, id);
        types.put(id, this);
    }

    public static MyomerType getType(int i) {
        return types.get(i);
    }
}

class WeaponLocation extends HMPType {
    public static final Hashtable<Integer, WeaponLocation> types = new Hashtable<>();

    public static final WeaponLocation LEFT_ARM = new WeaponLocation("Left Arm", 1);
    public static final WeaponLocation LEFT_LEG = new WeaponLocation("Left Leg", 3);
    public static final WeaponLocation RIGHT_TORSO = new WeaponLocation("Right Torso", 5);
    public static final WeaponLocation HEAD = new WeaponLocation("Head", 7);
    public static final WeaponLocation RIGHT_LEG_R = new WeaponLocation("Right Leg (R)", 15);
    public static final WeaponLocation HEAD_R = new WeaponLocation("Head (R)", 16);
    public static final WeaponLocation CENTER_TORSO_R = new WeaponLocation("Center Torso (R)", 17);

    private WeaponLocation(String name, int id) {
        super(name, id);
        types.put(id, this);
    }

    public static WeaponLocation getType(int i) {
        return types.get(i);
    }
}
