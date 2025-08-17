/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.loaders;

import megamek.common.TechConstants;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.util.BuildingBlock;

/**
 * This class loads BattleArmor BLK files.
 *
 * @author Suvarov454@sourceforge.net (James A. Damour)
 * @since April 6, 2002, 2:06 AM
 */
public class BLKBattleArmorFile extends BLKFile implements IMekLoader {

    public BLKBattleArmorFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {

        BattleArmor t = new BattleArmor();
        setBasicEntityData(t);

        if (dataFile.exists("exoskeleton") && dataFile.getDataAsString("exoskeleton")[0].equalsIgnoreCase("true")) {
            t.setIsExoskeleton(true);
        }

        if (!dataFile.exists("trooper count")) {
            throw new EntityLoadingException("Could not find trooper count block.");
        }
        t.setTroopers(dataFile.getDataAsInt("trooper count")[0]);

        if (!dataFile.exists("weightclass")) {
            throw new EntityLoadingException("Could not find weightclass block.");
        }
        t.setWeightClass(dataFile.getDataAsInt("weightclass")[0]);
        t.setWeight(t.getTroopers());

        if (!dataFile.exists("chassis")) {
            throw new EntityLoadingException("Could not find chassis block.");
        }
        String chassis = dataFile.getDataAsString("chassis")[0];
        if (chassis.toLowerCase().equals("biped")) {
            t.setChassisType(BattleArmor.CHASSIS_TYPE_BIPED);
        } else if (chassis.equalsIgnoreCase("quad")) {
            t.setChassisType(BattleArmor.CHASSIS_TYPE_QUAD);
        } else {
            throw new EntityLoadingException("Unsupported chassis type: " + chassis);
        }

        if (!dataFile.exists("motion_type")) {
            throw new EntityLoadingException("Could not find movement block.");
        }
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        t.setMovementMode(EntityMovementMode.parseFromString(sMotion));
        // Add equipment to calculate unit tech advancement correctly
        try {
            switch (t.getMovementMode()) {
                case INF_JUMP:
                    t.addEquipment(EquipmentType.get(EquipmentTypeLookup.BA_JUMP_JET), Entity.LOC_NONE);
                    break;
                case VTOL:
                    t.addEquipment(EquipmentType.get(EquipmentTypeLookup.BA_VTOL), Entity.LOC_NONE);
                    break;
                case INF_UMU:
                    t.addEquipment(EquipmentType.get(EquipmentTypeLookup.BA_UMU), Entity.LOC_NONE);
                    break;
                case NONE:
                    throw new EntityLoadingException("Invalid movement type: " + sMotion);
                default:
                    break;
            }
        } catch (LocationFullException ignore) {
            // Adding to LOC_NONE
        }

        if (!dataFile.exists("cruiseMP")) {
            throw new EntityLoadingException("Could not find cruiseMP block.");
        }
        t.setOriginalWalkMP(dataFile.getDataAsInt("cruiseMP")[0]);

        if (dataFile.exists("jumpingMP")) {
            t.setOriginalJumpMP(dataFile.getDataAsInt("jumpingMP")[0]);
        }

        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find armor block.");
        }

        int[] armor = dataFile.getDataAsInt("armor");

        // Each trooper has the same amount of armor
        if (armor.length != 1) {
            throw new EntityLoadingException("Incorrect armor array length");
        }

        // add the body to the armor array
        t.refreshLocations();
        for (int x = 1; x < t.locations(); x++) {
            t.initializeArmor(armor[0], x);
        }

        t.autoSetInternal();

        if (dataFile.exists("armor_type")) {
            t.setArmorType(dataFile.getDataAsInt("armor_type")[0]);
        }

        if (dataFile.exists("armor_tech")) {
            t.setArmorTechLevel(dataFile.getDataAsInt("armor_tech")[0]);
        }
        if (dataFile.exists("Turret")) {
            String field = dataFile.getDataAsString("Turret")[0];
            int index = field.indexOf(":");
            if (index >= 0) {
                t.setTurretSize(Integer.parseInt(field.substring(index + 1)));
                if (field.toLowerCase().startsWith("modular")
                      || field.toLowerCase().startsWith("configurable")) {
                    t.setModularTurret(true);
                }
            }
        }
        t.recalculateTechAdvancement();

        String[] abbrs = t.getLocationAbbrs();
        for (int loop = 0; loop < t.locations(); loop++) {
            loadEquipment(t, abbrs[loop], loop);
        }

        if (dataFile.exists("cost")) {
            t.setCost(dataFile.getDataAsInt("cost")[0]);
        }
        t.setArmorTonnage(t.getArmorWeight());
        loadQuirks(t);
        return t;
    }

    @Override
    protected void loadEquipment(Entity t, String sName, int nLoc) throws EntityLoadingException {
        String[] saEquip = dataFile.getDataAsString(sName + " Equipment");
        if (saEquip == null) {
            return;
        }

        // prefix is "Clan " or "IS "
        String prefix;
        if (t.getTechLevel() == TechConstants.T_CLAN_TW) {
            prefix = "Clan ";
        } else {
            prefix = "IS ";
        }
        // Track the last potential anti-personnel mount and put any APM weapon there
        Mounted<?> lastAPM = null;
        if (saEquip[0] != null) {
            for (int x = 0; x < saEquip.length; x++) {
                int mountLoc = BattleArmor.MOUNT_LOC_NONE;
                if (saEquip[x].contains(":Body")) {
                    mountLoc = BattleArmor.MOUNT_LOC_BODY;
                    saEquip[x] = saEquip[x].replace(":Body", "");
                } else if (saEquip[x].contains(":LA")) {
                    mountLoc = BattleArmor.MOUNT_LOC_LEFT_ARM;
                    saEquip[x] = saEquip[x].replace(":LA", "");
                } else if (saEquip[x].contains(":RA")) {
                    mountLoc = BattleArmor.MOUNT_LOC_RIGHT_ARM;
                    saEquip[x] = saEquip[x].replace(":RA", "");
                } else if (saEquip[x].contains(":TU")) {
                    mountLoc = BattleArmor.MOUNT_LOC_TURRET;
                    saEquip[x] = saEquip[x].replace(":TU", "");
                }

                boolean dwpMounted = saEquip[x].contains(":DWP");
                saEquip[x] = saEquip[x].replace(":DWP", "");

                boolean sswMounted = saEquip[x].contains(":SSWM");
                saEquip[x] = saEquip[x].replace(":SSWM", "");

                boolean apmMounted = saEquip[x].contains(":APM");
                saEquip[x] = saEquip[x].replace(":APM", "");

                int numShots = 0;
                if (saEquip[x].contains(":Shots")) {
                    String shotString = saEquip[x].substring(
                          saEquip[x].indexOf(":Shots"),
                          saEquip[x].indexOf("#") + 1);
                    numShots = Integer.parseInt(
                          shotString.replace(":Shots", "").replace("#", ""));
                    saEquip[x] = saEquip[x].replace(shotString, "");
                }
                double size = 0.0;
                int sizeIndex = saEquip[x].toUpperCase().indexOf(":SIZE:");
                if (sizeIndex > 0) {
                    size = Double.parseDouble(saEquip[x].substring(sizeIndex + 6));
                    saEquip[x] = saEquip[x].substring(0, sizeIndex);
                }

                String equipName = saEquip[x].trim();
                EquipmentType etype = EquipmentType.get(equipName);

                if (etype == null) {
                    // try w/ prefix
                    etype = EquipmentType.get(prefix + equipName);
                }

                if (etype != null) {
                    try {
                        Mounted<?> m = t.addEquipment(etype, nLoc, false,
                              mountLoc, dwpMounted);
                        if (numShots != 0 && (m.getType() instanceof AmmoType)) {
                            m.setShotsLeft(numShots);
                            m.setOriginalShots(numShots);
                            m.setSize(numShots * ((AmmoType) m.getType()).getKgPerShot() / 1000.0);
                        }
                        if ((etype instanceof MiscType)
                              && (etype.hasFlag(MiscType.F_AP_MOUNT) || etype.hasFlag(MiscType.F_ARMORED_GLOVE))) {
                            lastAPM = m;
                        } else if (apmMounted) {
                            m.setAPMMounted(true);
                            // Link to the last AP mount or armored glove. If we haven't found one yet or
                            // the last one has been used, the post load init will match with the first
                            // available.
                            if (lastAPM != null) {
                                lastAPM.setLinked(m);
                                lastAPM = null;
                            }
                        }
                        m.setSquadSupportWeapon(sswMounted);
                        if (etype.isVariableSize()) {
                            if (size == 0) {
                                size = MtfFile.extractLegacySize(equipName);
                            }
                            m.setSize(size);
                        }
                    } catch (LocationFullException ex) {
                        throw new EntityLoadingException(ex.getMessage());
                    }
                } else if (!equipName.isBlank()) {
                    t.addFailedEquipment(equipName);
                }
            }
        }
    }
}
