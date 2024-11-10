/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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
package megamek.common.loaders;

import java.util.List;
import java.util.Vector;

import megamek.common.*;
import megamek.common.util.BuildingBlock;
import megamek.logging.MMLogger;

/**
 * @author njrkrynn
 * @since April 6, 2002, 2:06 AM
 */
public class BLKMekFile extends BLKFile implements IMekLoader {
    private static final MMLogger logger = MMLogger.create(BLKMekFile.class);

    // armor locatioms
    public static final int HD = 0;
    public static final int LA = 1;
    public static final int LF = 2;
    public static final int LB = 3;
    public static final int CF = 4;
    public static final int CB = 5;
    public static final int RF = 6;
    public static final int RB = 7;
    public static final int RA = 8;
    public static final int LL = 9;
    public static final int RL = 10;

    public static final int CT = 4;
    public static final int RT = 6;
    public static final int LT = 2;

    public BLKMekFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Entity getEntity() throws EntityLoadingException {

        int chassisType = 0;
        if (!dataFile.exists("chassis_type")) {
            chassisType = 0;
        } else {
            chassisType = dataFile.getDataAsInt("chassis_type")[0];
        }

        Mek mek = null;

        if (chassisType == 1) {
            mek = new QuadMek();
        } else {
            mek = new BipedMek();
        }

        setBasicEntityData(mek);
        // Do I even write the year for these??

        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find block.");
        }
        mek.setWeight(dataFile.getDataAsDouble("tonnage")[0]);

        int engineCode = BLKFile.FUSION;
        if (dataFile.exists("engine_type")) {
            engineCode = dataFile.getDataAsInt("engine_type")[0];
        }
        int engineFlags = 0;
        if (mek.isClan()) {
            engineFlags = Engine.CLAN_ENGINE;
        }
        if (!dataFile.exists("walkingMP")) {
            throw new EntityLoadingException("Could not find walkingMP block.");
        }
        int engineRating = dataFile.getDataAsInt("walkingMP")[0] * (int) mek.getWeight();
        mek.setEngine(new Engine(engineRating, BLKFile.translateEngineCode(engineCode), engineFlags));

        if (!dataFile.exists("jumpingMP")) {
            throw new EntityLoadingException("Could not find block.");
        }
        mek.setOriginalJumpMP(dataFile.getDataAsInt("jumpingMP")[0]);

        // I keep internal(integral) heat sinks separate...
        if (!dataFile.exists("heatsinks")) {
            throw new EntityLoadingException("Could not find block.");
        }
        mek.addEngineSinks(dataFile.getDataAsInt("heatsinks")[0], MiscType.F_HEAT_SINK);

        if (dataFile.exists("internal_type")) {
            mek.setStructureType(dataFile.getDataAsInt("internal_type")[0]);
        } else {
            mek.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
        }

        boolean patchworkArmor = false;
        if (dataFile.exists("armor_type")) {
            if (dataFile.getDataAsInt("armor_type")[0] == EquipmentType.T_ARMOR_PATCHWORK) {
                patchworkArmor = true;
            } else {
                mek.setArmorType(dataFile.getDataAsInt("armor_type")[0]);
            }
        } else {
            mek.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        }
        if (!patchworkArmor && dataFile.exists("armor_tech")) {
            mek.setArmorTechLevel(dataFile.getDataAsInt("armor_tech")[0]);
        }
        if (patchworkArmor) {
            for (int i = 0; i < mek.locations(); i++) {
                mek.setArmorType(dataFile.getDataAsInt(mek.getLocationName(i) + "_armor_type")[0], i);
                mek.setArmorTechLevel(dataFile.getDataAsInt(mek.getLocationName(i) + "_armor_type")[0], i);
            }
        }

        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find block.");
        }

        if (dataFile.getDataAsInt("armor").length < 11) {
            logger.error("Read armor array doesn't match my armor array...");
            throw new EntityLoadingException("Could not find block.");
        }
        int[] armor = dataFile.getDataAsInt("Armor");

        mek.initializeArmor(armor[BLKMekFile.HD], Mek.LOC_HEAD);

        mek.initializeArmor(armor[BLKMekFile.LA], Mek.LOC_LARM);
        mek.initializeArmor(armor[BLKMekFile.RA], Mek.LOC_RARM);
        mek.initializeArmor(armor[BLKMekFile.LL], Mek.LOC_LLEG);
        mek.initializeArmor(armor[BLKMekFile.RL], Mek.LOC_RLEG);

        mek.initializeArmor(armor[BLKMekFile.CF], Mek.LOC_CT);
        mek.initializeArmor(armor[BLKMekFile.LF], Mek.LOC_LT);
        mek.initializeArmor(armor[BLKMekFile.RF], Mek.LOC_RT);

        // changed...
        mek.initializeRearArmor(armor[BLKMekFile.CB], Mek.LOC_CT);
        mek.initializeRearArmor(armor[BLKMekFile.LB], Mek.LOC_LT);
        mek.initializeRearArmor(armor[BLKMekFile.RB], Mek.LOC_RT);

        mek.recalculateTechAdvancement();

        if (!dataFile.exists("internal armor")) {
            // try to guess...
            mek.setInternal(3, (armor[CF] + armor[CB]) / 2, (armor[LF] + armor[LB]) / 2, (armor[LA] / 2),
                    (armor[LL] / 2));
        } else {
            armor = dataFile.getDataAsInt("internal armor");
            // all the locations should be about the same...
            mek.setInternal(armor[HD], armor[CT], armor[LT], armor[LA], armor[LL]);
        }

        // check for removed arm actuators...

        // no lower right arm
        if (!dataFile.getDataAsString("ra criticals")[2].trim().equalsIgnoreCase("Lower Arm Actuator")) {
            mek.removeCriticals(Mek.LOC_RARM, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_LOWER_ARM));
        }
        // no right hand
        if (!dataFile.getDataAsString("ra criticals")[3].trim().equalsIgnoreCase("Hand Actuator")) {
            mek.removeCriticals(Mek.LOC_RARM, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HAND));
        }

        // no lower left arm
        if (!dataFile.getDataAsString("la criticals")[2].trim().equalsIgnoreCase("Lower Arm Actuator")) {
            mek.removeCriticals(Mek.LOC_LARM, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_LOWER_ARM));
        }
        // no left hand
        if (!dataFile.getDataAsString("la criticals")[3].trim().equalsIgnoreCase("Hand Actuator")) {
            mek.removeCriticals(Mek.LOC_LARM, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HAND));
        }

        // load equipment stuff...
        List<String>[] criticals = new Vector[8];

        criticals[Mek.LOC_HEAD] = dataFile.getDataAsVector("hd criticals");
        criticals[Mek.LOC_LARM] = dataFile.getDataAsVector("la criticals");
        criticals[Mek.LOC_RARM] = dataFile.getDataAsVector("ra criticals");
        criticals[Mek.LOC_LLEG] = dataFile.getDataAsVector("ll criticals");
        criticals[Mek.LOC_RLEG] = dataFile.getDataAsVector("rl criticals");
        criticals[Mek.LOC_LT] = dataFile.getDataAsVector("lt criticals");
        criticals[Mek.LOC_RT] = dataFile.getDataAsVector("rt criticals");
        criticals[Mek.LOC_CT] = dataFile.getDataAsVector("ct criticals");

        // prefix is "Clan " or "IS "
        String prefix;
        if (mek.getTechLevel() == TechConstants.T_CLAN_TW) {
            prefix = "Clan ";
        } else {
            prefix = "IS ";
        }

        for (int loc = 0; loc < criticals.length; loc++) {
            for (int c = 0; c < criticals[loc].size(); c++) {
                String critName = criticals[loc].get(c).toString().trim();
                boolean rearMounted = false;
                boolean turretMounted = false;
                boolean armored = false;

                if (critName.startsWith("(R) ")) {
                    rearMounted = true;
                    critName = critName.substring(4);
                }
                if (critName.startsWith("(T) ")) {
                    turretMounted = true;
                    critName = critName.substring(4);
                }
                if (critName.startsWith("(A) ")) {
                    armored = true;
                    critName = critName.substring(4);
                }

                boolean isOmniMounted = critName.endsWith(":OMNI");
                critName = critName.replace(":OMNI", "");
                int facing = -1;
                if (critName.toUpperCase().endsWith("(FL)")) {
                    facing = 5;
                    critName = critName.substring(0, critName.length() - 4).trim();
                }
                if (critName.toUpperCase().endsWith("(FR)")) {
                    facing = 1;
                    critName = critName.substring(0, critName.length() - 4).trim();
                }
                if (critName.toUpperCase().endsWith("(RL)")) {
                    facing = 4;
                    critName = critName.substring(0, critName.length() - 4).trim();
                }
                if (critName.toUpperCase().endsWith("(RR)")) {
                    facing = 2;
                    critName = critName.substring(0, critName.length() - 4).trim();
                }
                if (critName.contains("Engine")) {
                    mek.setCritical(loc, c,
                            new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, true, armored));
                    continue;
                } else if (critName.equalsIgnoreCase("Life Support")) {
                    mek.setCritical(loc, c,
                            new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, true, armored));
                    continue;
                } else if (critName.equalsIgnoreCase("Sensors")) {
                    mek.setCritical(loc, c,
                            new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, true, armored));
                    continue;
                } else if (critName.equalsIgnoreCase("Cockpit")) {
                    mek.setCritical(loc, c,
                            new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, true, armored));
                    continue;
                } else if (critName.equalsIgnoreCase("Gyro")) {
                    mek.setCritical(loc, c,
                            new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, true, armored));
                    continue;
                }

                EquipmentType etype = EquipmentType.get(critName);
                if (etype == null) {
                    // try w/ prefix
                    etype = EquipmentType.get(prefix + critName);
                }
                if (etype != null) {
                    try {
                        Mounted<?> mount = mek.addEquipment(etype, loc,
                                rearMounted, BattleArmor.MOUNT_LOC_NONE, false,
                                turretMounted);
                        mount.setOmniPodMounted(isOmniMounted);
                        if ((etype instanceof WeaponType) && etype.hasFlag(WeaponType.F_VGL)) {
                            // vehicular grenade launchers need to have their
                            // facing set
                            if (facing == -1) {
                                // if facing has not been set earlier, we are
                                // front or rear mounted
                                if (rearMounted) {
                                    mount.setFacing(3);
                                } else {
                                    mount.setFacing(0);
                                }
                            } else {
                                mount.setFacing(facing);
                            }
                        }
                    } catch (LocationFullException ex) {
                        throw new EntityLoadingException(ex.getMessage());
                    }
                }

            } // end of specific location
        } // end of all crits

        if (dataFile.exists("omni")) {
            mek.setOmni(true);
        }

        mek.setArmorTonnage(mek.getArmorWeight());

        loadQuirks(mek);
        return mek;

    }
}
