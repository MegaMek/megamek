/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

import megamek.common.*;
import megamek.common.util.BuildingBlock;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * This class loads Infantry BLK files.
 *
 * @author Suvarov454@sourceforge.net (James A. Damour)
 * @since April 6, 2002, 2:06 AM
 */
public class BLKInfantryFile extends BLKFile implements IMechLoader {

    public BLKInfantryFile(BuildingBlock bb) {
        dataFile = bb;
    }

    @Override
    public Entity getEntity() throws EntityLoadingException {

        Infantry infantry = new Infantry();
        setBasicEntityData(infantry);

        if (!dataFile.exists("squad_size")) {
            throw new EntityLoadingException("Could not find squad size.");
        }
        infantry.setSquadSize(dataFile.getDataAsInt("squad_size")[0]);
        if (!dataFile.exists("squadn")) {
            throw new EntityLoadingException("Could not find number of squads.");
        }
        infantry.setSquadCount(dataFile.getDataAsInt("squadn")[0]);

        infantry.autoSetInternal();

        if (dataFile.exists("InfantryArmor")) {
            infantry.setArmorDamageDivisor(dataFile.getDataAsInt("InfantryArmor")[0]);
        }

        if (!dataFile.exists("motion_type")) {
            throw new EntityLoadingException("Could not find movement block.");
        }
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        infantry.setMicrolite(sMotion.equalsIgnoreCase("microlite"));
        if (sMotion.startsWith("Beast:")) {
            infantry.setMount(InfantryMount.parse(sMotion));
        } else {
            EntityMovementMode nMotion = EntityMovementMode.parseFromString(sMotion);
            if (nMotion.isNone()) {
                throw new EntityLoadingException("Invalid movement type: " + sMotion);
            }
            if (nMotion == EntityMovementMode.INF_UMU
                    && sMotion.toLowerCase().contains("motorized")) {
                infantry.setMotorizedScuba();
            } else {
                infantry.setMovementMode(nMotion);
            }
        }

        // get primary and secondary weapons
        if (dataFile.exists("secondn")) {
            infantry.setSecondaryWeaponsPerSquad(dataFile.getDataAsInt("secondn")[0]);
        }

        if (!dataFile.exists("Primary")) {
            throw new EntityLoadingException("Could not find primary weapon.");
        }
        String primaryName = dataFile.getDataAsString("Primary")[0];
        EquipmentType ptype = EquipmentType.get(primaryName);
        if (!(ptype instanceof InfantryWeapon)) {
            throw new EntityLoadingException("primary weapon is not an infantry weapon");
        }
        infantry.setPrimaryWeapon((InfantryWeapon) ptype);

        EquipmentType stype = null;
        if (dataFile.exists("Secondary")) {
            String secondName = dataFile.getDataAsString("Secondary")[0];
            stype = EquipmentType.get(secondName);
            if (!(stype instanceof InfantryWeapon)) {
                throw new EntityLoadingException("secondary weapon " + secondName + " is not an infantry weapon");
            }
            infantry.setSecondaryWeapon((InfantryWeapon) stype);
        }

        // if there is more than one secondary weapon per squad, then add that to the unit
        // otherwise add the primary weapon
        Mounted m;
        try {
            if ((infantry.getSecondaryWeaponsPerSquad() > 1)) {
                m = new InfantryWeaponMounted(infantry, stype, (InfantryWeapon) ptype);
            } else if (stype != null) {
                m = new InfantryWeaponMounted(infantry, ptype, (InfantryWeapon) stype);
            } else {
                m = new Mounted(infantry, ptype);
            }
        } catch (ClassCastException ex) {
            throw new EntityLoadingException(ex.getMessage());
        }
        try {
            infantry.addEquipment(m, Infantry.LOC_INFANTRY, false);
        } catch (LocationFullException ex) {
            throw new EntityLoadingException(ex.getMessage());
        }

        // TAG infantry have separate attacks for primary and secondary weapons.
        if (null != stype && stype.hasFlag(WeaponType.F_TAG)) {
            infantry.setSpecializations(infantry.getSpecializations() | Infantry.TAG_TROOPS);
            try {
                infantry.addEquipment(ptype, Infantry.LOC_INFANTRY);
            } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
            }
        }

        // backward compatibility: armor kits should now be saved and loaded as part of LOC_INFANTRY equipment
        if (dataFile.exists("armorKit")) {
            String kitName = dataFile.getDataAsString("armorKit")[0];
            EquipmentType kit = EquipmentType.get(kitName);
            if ((null == kit) || !(kit.hasFlag(MiscType.F_ARMOR_KIT))) {
                throw new EntityLoadingException(kitName + " is not an infantry armor kit");
            }
            infantry.setArmorKit(kit);
        }

        if (dataFile.exists("dest")) {
            infantry.setDEST(true);
        }

        if (dataFile.exists("specialization")) {
            try {
                infantry.setSpecializations(Integer.parseInt(dataFile.getDataAsString("specialization")[0]));
            } catch (NumberFormatException ex) {
                throw new EntityLoadingException("Could not read specialization");
            }
        }
        
        if (dataFile.exists("encumberingarmor")) {
            infantry.setArmorEncumbering(true);
        }

        if (dataFile.exists("spacesuit")) {
            infantry.setSpaceSuit(true);
        }

        if (dataFile.exists("sneakcamo")) {
            infantry.setSneakCamo(true);
        }

        if (dataFile.exists("sneakir")) {
            infantry.setSneakIR(true);
        }

        if (dataFile.exists("sneakecm")) {
            infantry.setSneakECM(true);
        }

        if (dataFile.exists("armordivisor")) {
            try {
                infantry.setArmorDamageDivisor(Double.parseDouble(dataFile.getDataAsString("armordivisor")[0]));
            } catch (NumberFormatException ex) {
                throw new EntityLoadingException("Could not read armor divisor");
            }
        }

        loadEquipment(infantry, "Field Guns", Infantry.LOC_FIELD_GUNS);
        loadEquipment(infantry, "Troopers", Infantry.LOC_INFANTRY);

        // Update some internals if there's an armor kit
        infantry.getMisc().stream()
                .filter(misc -> misc.getType().hasFlag(MiscType.F_ARMOR_KIT))
                .findFirst()
                .ifPresent(mounted -> infantry.setArmorKit(mounted.getType()));

        // backward compatibility: if an antimek better than 8 entry exists, assume anti-mek gear
        if (dataFile.exists("antimek")) {
            int[] amSkill = dataFile.getDataAsInt("antimek");
            if (amSkill[0] != 8) {
                try {
                    infantry.addEquipment(EquipmentType.get(EquipmentTypeLookup.ANTI_MEK_GEAR), Infantry.LOC_INFANTRY);
                } catch (LocationFullException ex) {
                    throw new EntityLoadingException(ex.getMessage());
                }
            }
        }
        
        // Some units (mostly Manei Domini) have cybernetics/prosthetics as part of the official unit description.
        if (dataFile.exists("augmentation")) {
            String[] augmentations = dataFile.getDataAsString("augmentation");
            for (String aug : augmentations) {
                try {
                    infantry.getCrew().getOptions().getOption(aug).setValue(true);
                } catch (NullPointerException ex) {
                    throw new EntityLoadingException("Could not locate pilot option " + aug);
                }
            }
        }
        infantry.recalculateTechAdvancement();
        loadQuirks(infantry);
        return infantry;
    }
}
