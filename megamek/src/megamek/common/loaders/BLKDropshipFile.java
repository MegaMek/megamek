/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.Aero;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.util.BuildingBlock;

/**
 * @author taharqa
 * @since April 6, 2002, 2:06 AM
 */
public class BLKDropshipFile extends BLKFile implements IMekLoader {

    public BLKDropshipFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {

        Dropship a = new Dropship();
        setBasicEntityData(a);

        if (dataFile.exists("originalBuildYear")) {
            a.setOriginalBuildYear(dataFile.getDataAsInt("originalBuildYear")[0]);
        }

        if (!dataFile.exists("crew")) {
            throw new EntityLoadingException("Could not find crew block.");
        }
        a.setNCrew(dataFile.getDataAsInt("crew")[0]);

        if (dataFile.exists("officers")) {
            a.setNOfficers(dataFile.getDataAsInt("officers")[0]);
        }

        if (dataFile.exists("gunners")) {
            a.setNGunners(dataFile.getDataAsInt("gunners")[0]);
        }

        if (dataFile.exists("passengers")) {
            a.setNPassenger(dataFile.getDataAsInt("passengers")[0]);
        }

        if (dataFile.exists("battlearmor")) {
            a.setNBattleArmor(dataFile.getDataAsInt("battlearmor")[0]);
        }

        if (dataFile.exists("marines")) {
            a.setNMarines(dataFile.getDataAsInt("marines")[0]);
        }

        if (dataFile.exists("otherpassenger")) {
            a.setNOtherPassenger(dataFile.getDataAsInt("otherpassenger")[0]);
        }

        if (!dataFile.exists("life_boat")) {
            throw new EntityLoadingException("Could not find life boat block.");
        }
        a.setLifeBoats(dataFile.getDataAsInt("life_boat")[0]);

        if (!dataFile.exists("escape_pod")) {
            throw new EntityLoadingException("Could not find escape pod block.");
        }
        a.setEscapePods(dataFile.getDataAsInt("escape_pod")[0]);

        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find tonnage block.");
        }
        a.setWeight(dataFile.getDataAsDouble("tonnage")[0]);

        // get a movement mode - lets try Aerodyne
        if (!dataFile.exists("motion_type")) {
            throw new EntityLoadingException("Could not find movement block.");
        }
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        EntityMovementMode nMotion = EntityMovementMode.AERODYNE;
        if (sMotion.equalsIgnoreCase("spheroid")) {
            nMotion = EntityMovementMode.SPHEROID;
            a.setSpheroid(true);
        }
        a.setMovementMode(nMotion);

        // All dropships are VSTOL and can hover
        a.setVSTOL(true);

        // figure out structural integrity
        if (!dataFile.exists("structural_integrity")) {
            throw new EntityLoadingException(
                  "Could not find structural integrity block.");
        }
        a.setOSI(dataFile.getDataAsInt("structural_integrity")[0]);

        if (dataFile.exists("collartype")) {
            a.setCollarType(dataFile.getDataAsInt("collartype")[0]);
        }
        // figure out heat
        if (!dataFile.exists("heatsinks")) {
            throw new EntityLoadingException("Could not find heatsink block.");
        }
        a.setHeatSinks(dataFile.getDataAsInt("heatsinks")[0]);
        a.setOHeatSinks(dataFile.getDataAsInt("heatsinks")[0]);
        if (!dataFile.exists("sink_type")) {
            throw new EntityLoadingException("Could not find sink_type block.");
        }
        a.setHeatType(dataFile.getDataAsInt("sink_type")[0]);

        // figure out fuel
        if (!dataFile.exists("fuel")) {
            throw new EntityLoadingException("Could not find fuel block.");
        }
        a.setFuel(dataFile.getDataAsInt("fuel")[0]);

        // figure out engine stuff
        // not done for small craft and up
        if (!dataFile.exists("SafeThrust")) {
            throw new EntityLoadingException(
                  "Could not find Safe Thrust block.");
        }
        a.setOriginalWalkMP(dataFile.getDataAsInt("SafeThrust")[0]);

        // Switch older files with standard armor to aerospace
        int at = EquipmentType.T_ARMOR_AEROSPACE;
        if (dataFile.exists("armor_type")) {
            at = dataFile.getDataAsInt("armor_type")[0];
            if (at == EquipmentType.T_ARMOR_STANDARD) {
                at = EquipmentType.T_ARMOR_AEROSPACE;
            }
        }
        a.setArmorType(at);
        setArmorTechLevelFromDataFile(a);
        if (dataFile.exists("internal_type")) {
            a.setStructureType(dataFile.getDataAsInt("internal_type")[0]);
        } else {
            a.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
        }
        if (dataFile.exists("designtype")) {
            a.setDesignType(dataFile.getDataAsInt("designtype")[0]);
        } else {
            a.setDesignType(Aero.MILITARY);
        }

        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find armor block.");
        }

        int[] armor = dataFile.getDataAsInt("armor");

        if (armor.length != 4) {
            throw new EntityLoadingException("Incorrect armor array length");
        }

        a.initializeArmor(armor[BLKAeroSpaceFighterFile.NOSE], Dropship.LOC_NOSE);
        a.initializeArmor(armor[BLKAeroSpaceFighterFile.RW], Dropship.LOC_RIGHT_WING);
        a.initializeArmor(armor[BLKAeroSpaceFighterFile.LW], Dropship.LOC_LEFT_WING);
        a.initializeArmor(armor[BLKAeroSpaceFighterFile.AFT], Dropship.LOC_AFT);
        a.initializeArmor(IArmorState.ARMOR_NA, Dropship.LOC_HULL);

        a.autoSetInternal();
        a.recalculateTechAdvancement();
        // This is not working right for arrays for some reason
        a.autoSetThresh();

        for (int loc = 0; loc < a.locations(); loc++) {
            loadEquipment(a, a.getLocationName(loc), loc);
        }

        if (dataFile.exists("omni")) {
            a.setOmni(true);
        }

        addTransports(a);

        // how many bombs can it carry; depends on transport bays
        a.autoSetMaxBombPoints();

        a.setArmorTonnage(a.getArmorWeight());
        loadQuirks(a);
        return a;
    }

    protected void loadEquipment(Dropship a, String sName, int nLoc)
          throws EntityLoadingException {
        String[] saEquip1 = dataFile.getDataAsString(sName + " Equipment");
        String[] saEquip2 = new String[2];
        String[] saEquip;

        // Special case handling for the LongTomIIICannon in the Fortress BLK
        // A bit of a hack, but it works...
        if ((nLoc == Aero.LOC_NOSE) && dataFile.exists("transporters")) {
            String[] transporters = dataFile.getDataAsString("transporters");
            for (String transporter : transporters) {
                if (transporter.equals("LongTomIIICannon")) {
                    saEquip2[0] = "ISLongTom";
                    saEquip2[1] = "ISLongTomAmmo:125";
                }
            }
        }
        if (saEquip2[0] != null) {
            saEquip = new String[saEquip1.length + saEquip2.length];
            System.arraycopy(saEquip2, 0, saEquip, 0, saEquip2.length);
            System.arraycopy(saEquip1, 0, saEquip, 2, saEquip1.length);
        } else {
            saEquip = new String[saEquip1.length];
            System.arraycopy(saEquip1, 0, saEquip, 0, saEquip1.length);
        }

        // prefix is "Clan " or "IS "
        String prefix;
        if (a.getTechLevel() == TechConstants.T_CLAN_TW) {
            prefix = "Clan ";
        } else {
            prefix = "IS ";
        }

        boolean rearMount;
        int nAmmo;
        // set up a new weapons bay mount
        WeaponMounted bayMount = null;
        // set up a new bay type
        boolean newBay;
        double bayDamage = 0;
        if (saEquip[0] != null) {
            for (String element : saEquip) {
                rearMount = false;
                nAmmo = 1;
                newBay = false;
                String equipName = element.trim();

                double size = 0.0;
                int sizeIndex = equipName.toUpperCase().indexOf(":SIZE:");
                if (sizeIndex > 0) {
                    size = Double.parseDouble(equipName.substring(sizeIndex + 6));
                    equipName = equipName.substring(0, sizeIndex);
                }
                // I will need to deal with rear-mounted bays on Dropships
                if (equipName.startsWith("(R) ")) {
                    rearMount = true;
                    equipName = equipName.substring(4);
                }

                if (equipName.startsWith("(B) ")) {
                    newBay = true;
                    equipName = equipName.substring(4);
                }

                // check for ammo loadouts
                if (equipName.contains(":") && (equipName.contains("Ammo")
                      || equipName.contains("Pod"))) {
                    // then split by the :
                    String[] temp;
                    temp = equipName.split(":");
                    equipName = temp[0];
                    if (temp[1] != null) {
                        nAmmo = Integer.parseInt(temp[1]);
                    }
                }

                EquipmentType etype = EquipmentType.get(equipName);

                if (etype == null) {
                    // try w/ prefix
                    etype = EquipmentType.get(prefix + equipName);
                }

                if (etype != null) {
                    // first load the equipment
                    Mounted<?> newMount;
                    try {
                        if (nAmmo == 1) {
                            newMount = a.addEquipment(etype, nLoc, rearMount);
                        } else {
                            newMount = a.addEquipment(etype, nLoc, rearMount,
                                  nAmmo);
                        }
                    } catch (LocationFullException ex) {
                        throw new EntityLoadingException(ex.getMessage());
                    }

                    // this is where weapon bays go
                    // first, lets see if it is a weapon
                    if (newMount.getType() instanceof WeaponType weaponType) {
                        // if so then I need to find out if it is the same class
                        // as the current weapon bay
                        // If the current bay is null, then it needs to be
                        // initialized
                        if (bayMount == null) {
                            try {
                                bayMount = (WeaponMounted) a.addEquipment(weaponType.getBayType(), nLoc, rearMount);
                                newBay = false;
                            } catch (LocationFullException ex) {
                                throw new EntityLoadingException(
                                      ex.getMessage());
                            }
                        }

                        double damage = weaponType.getShortAV();
                        if (weaponType.isCapital()) {
                            damage *= 10;
                        }
                        if (!newBay
                              && ((bayDamage + damage) <= 700)
                              && (bayMount.isRearMounted() == rearMount)
                              && (weaponType.getAtClass() == bayMount
                              .getType().getAtClass())
                              && !(bayMount.getType()
                              .isSubCapital() && !weaponType.isSubCapital())) {
                            // then we should add this weapon to the current bay
                            bayMount.addWeaponToBay(a.getEquipmentNum(newMount));
                            bayDamage += damage;
                        } else {
                            try {
                                bayMount = (WeaponMounted) a.addEquipment(weaponType.getBayType(), nLoc, rearMount);
                            } catch (LocationFullException ex) {
                                throw new EntityLoadingException(
                                      ex.getMessage());
                            }
                            bayMount.addWeaponToBay(a.getEquipmentNum(newMount));
                            // reset bay damage
                            bayDamage = damage;
                        }
                    } else if (newMount.getType() instanceof AmmoType) {
                        // ammo should also get loaded into the bay
                        if (bayMount != null) {
                            bayMount.addAmmoToBay(a.getEquipmentNum(newMount));
                        }
                    }
                    if (etype.isVariableSize()) {
                        newMount.setSize(size);
                    }
                } else if (!equipName.isBlank()) {
                    a.addFailedEquipment(equipName);
                }
            }
        }
    }
}
