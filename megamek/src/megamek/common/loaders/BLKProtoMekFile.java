/*
 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2004-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.ProtoMek;
import megamek.common.util.BuildingBlock;
import megamek.common.verifier.TestProtoMek;

/**
 * This class loads ProtoMek BLK files.
 *
 * @author Suvarov454@sourceforge.net (James A. Damour)
 * @since April 6, 2002, 2:06 AM
 */
public class BLKProtoMekFile extends BLKFile implements IMekLoader {

    public BLKProtoMekFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {

        ProtoMek t = new ProtoMek();
        setBasicEntityData(t);

        if (!dataFile.exists("year")) {
            throw new EntityLoadingException("Could not find year block.");
        }
        t.setYear(dataFile.getDataAsInt("year")[0]);

        if (!dataFile.exists("tonnage")) {
            throw new EntityLoadingException("Could not find weight block.");
        }
        t.setWeight(dataFile.getDataAsDouble("tonnage")[0]);

        String sMotion = dataFile.getDataAsString("motion_type")[0];
        EntityMovementMode nMotion = EntityMovementMode.parseFromString(sMotion);
        if (nMotion == EntityMovementMode.NONE) {
            throw new EntityLoadingException("Invalid movement type: " + sMotion);
        }
        t.setMovementMode(nMotion);
        t.setIsQuad(nMotion == EntityMovementMode.QUAD);
        t.setIsGlider(nMotion == EntityMovementMode.WIGE);

        if (!dataFile.exists("cruiseMP")) {
            throw new EntityLoadingException("Could not find cruiseMP block.");
        }
        t.setOriginalWalkMP(dataFile.getDataAsInt("cruiseMP")[0]);

        int engineFlags = Engine.NORMAL_ENGINE;
        int engineRating = TestProtoMek.calcEngineRating(t);
        t.setEngine(new Engine(engineRating, BLKFile.translateEngineCode(BLKFile.FUSION), engineFlags));

        if (dataFile.exists("jumpingMP")) {
            t.setOriginalJumpMP(dataFile.getDataAsInt("jumpingMP")[0]);
        }

        if (dataFile.exists("interface_cockpit")) {
            t.setInterfaceCockpit(Boolean.parseBoolean(dataFile.getDataAsString("interface_cockpit")[0]));
        }

        if (!dataFile.exists("armor")) {
            throw new EntityLoadingException("Could not find armor block.");
        }

        int[] armor = dataFile.getDataAsInt("armor");

        boolean hasMainGun;
        int armorLocs = armor.length + t.firstArmorIndex();
        if (ProtoMek.NUM_PROTOMEK_LOCATIONS == armorLocs) {
            hasMainGun = true;
        } else if ((ProtoMek.NUM_PROTOMEK_LOCATIONS - 1) == armorLocs) {
            hasMainGun = false;
        } else {
            throw new EntityLoadingException("Incorrect armor array length");
        }

        t.setHasMainGun(hasMainGun);

        if (dataFile.exists("armor_type")) {
            int at = dataFile.getDataAsInt("armor_type")[0];
            if (at == ArmorType.T_ARMOR_STANDARD) {
                at = ArmorType.T_ARMOR_STANDARD_PROTOMEK;
            }
            t.setArmorType(at);
        } else {
            t.setArmorType(EquipmentType.T_ARMOR_STANDARD_PROTOMEK);
        }

        setArmorTechLevelFromDataFile(t);

        // add the body to the armor array
        for (int x = 0; x < armor.length; x++) {
            t.initializeArmor(armor[x], x + t.firstArmorIndex());
        }

        t.autoSetInternal();
        t.recalculateTechAdvancement();

        String[] abbreviations = t.getLocationNames();
        for (int loop = 0; loop < t.locations(); loop++) {
            loadEquipment(t, abbreviations[loop], loop);
        }
        t.setArmorTonnage(t.getArmorWeight());

        loadQuirks(t);

        // ProtoMeks have EI Interface built-in per IO p.77
        // Add it automatically so it shows in the Equipment tab
        // ProtoMeks cannot shut down EI, so it's always "On" (mode index 1)
        try {
            EquipmentType eiInterface = EquipmentType.get("EIInterface");
            if (eiInterface != null) {
                Mounted<?> eiMount = t.addEquipment(eiInterface, ProtoMek.LOC_BODY);
                eiMount.setMode(1); // "Initiate enhanced imaging" (always on for ProtoMeks)
            }
        } catch (LocationFullException e) {
            // Should never happen for slotless equipment, but log if it does
            throw new EntityLoadingException("Failed to add built-in EI Interface to ProtoMek", e);
        }

        return t;
    }

    private void loadEquipment(ProtoMek t, String sName, int nLoc) throws EntityLoadingException {
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

        boolean rearMount = false;

        for (String element : saEquip) {
            String equipName = element.trim();
            double size = 0.0;
            int sizeIndex = equipName.toUpperCase().indexOf(":SIZE:");
            if (sizeIndex > 0) {
                size = Double.parseDouble(equipName.substring(sizeIndex + 6));
                equipName = equipName.substring(0, sizeIndex);
            }
            if (equipName.startsWith("(R) ")) {
                rearMount = true;
                equipName = equipName.substring(4);
            }

            // ProtoMek Ammo comes in non-standard amounts.
            int ammoIndex = equipName.lastIndexOf(" (");
            int shotsCount = 0;
            if (ammoIndex > 0) {
                // Try to get the number of shots.
                try {
                    String shots = equipName.substring(ammoIndex + 2, equipName.length() - 1);
                    shotsCount = Integer.parseInt(shots);
                    if (shotsCount < 0) {
                        throw new EntityLoadingException("Invalid number of shots in: " + equipName + ".");
                    }
                } catch (NumberFormatException badShots) {
                    throw new EntityLoadingException("Could not determine the number of shots in: " + equipName + ".");
                }

                // Strip the shots out of the ammo name.
                equipName = equipName.substring(0, ammoIndex);
            }
            EquipmentType etype = EquipmentType.get(equipName);

            if (etype == null) {
                // try w/ prefix
                etype = EquipmentType.get(prefix + equipName);
            }

            if (etype != null) {
                try {
                    // If this is an Ammo slot, only add
                    // the indicated number of shots.
                    Mounted<?> mount;
                    if (ammoIndex > 0) {
                        mount = t.addEquipment(etype, ProtoMek.LOC_BODY, false, shotsCount);
                    } else if (TestProtoMek.requiresSlot(etype)) {
                        mount = t.addEquipment(etype, nLoc);
                        // Need to set facing for VGLs
                        if ((etype instanceof WeaponType)
                              && etype.hasFlag(WeaponType.F_VGL)) {
                            mount.setFacing(defaultVGLFacing(nLoc, rearMount));
                        }
                    } else {
                        mount = t.addEquipment(etype, ProtoMek.LOC_BODY);
                    }
                    if (etype.isVariableSize()) {
                        mount.setSize(size);
                    }
                } catch (LocationFullException ex) {
                    throw new EntityLoadingException(ex.getMessage());
                }
            }
        }
    }
}
