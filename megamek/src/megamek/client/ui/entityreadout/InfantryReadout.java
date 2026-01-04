/*
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
package megamek.client.ui.entityreadout;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringJoiner;

import megamek.client.ui.Messages;
import megamek.common.enums.ProstheticEnhancementType;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.units.Infantry;
import megamek.common.units.InfantryMount;
import megamek.common.verifier.TestInfantry;

/**
 * The Entity information shown in the unit selector and many other places in MM, MML and MHQ.
 */
class InfantryReadout extends GeneralEntityReadout {

    protected final Infantry infantry;

    protected InfantryReadout(Infantry infantry, boolean showDetail, boolean useAlternateCost,
          boolean ignorePilotBV) {

        super(infantry, showDetail, useAlternateCost, ignorePilotBV);
        this.infantry = infantry;
    }

    @Override
    protected ViewElement createWeightElement() {
        return new EmptyElement();
    }


    @Override
    protected List<ViewElement> createMovementElements() {
        List<ViewElement> result = new ArrayList<>();
        InfantryMount mount = infantry.getMount();
        int walkMP = infantry.getWalkMP();
        int runMP = infantry.getRunMP();
        int jumpMP = infantry.getJumpMP();
        int umuMP = infantry.getAllUMUCount();
        StringJoiner movement = new StringJoiner("/");
        if (!infantry.getMovementMode().isSubmarine() || ((mount != null) && (mount.secondaryGroundMP() > 0))) {
            movement.add(walkMP + "");
        }
        if (runMP > walkMP) {
            // Infantry fast movement option; otherwise run mp = walk mp
            movement.add("%d (Fast)".formatted(runMP));
        }
        if (jumpMP > 0) {
            String modeLetter = infantry.getMovementMode().isVTOL() ? "V" : "J";
            movement.add("%d (%s)".formatted(jumpMP, modeLetter));
        }
        if (umuMP > 0) {
            movement.add("%d (U)".formatted(umuMP));
        }
        result.add(new PlainLine());
        result.add(new LabeledLine(megamek.client.ui.Messages.getString("MekView.Movement"),
              movement.toString()));

        return result;
    }

    @Override
    protected ViewElement createTotalArmorElement() {
        return new EmptyElement();
    }

    @Override
    protected List<ViewElement> createSystemsElements() {
        List<ViewElement> result = new ArrayList<>();
        InfantryMount mount = infantry.getMount();
        if (mount != null) {
            StringJoiner mountFeatures = new StringJoiner(", ", " (", ")");
            mountFeatures.add(mount.size().displayName());
            if (mount.movementMode().isSubmarine()) {
                mountFeatures.add(megamek.client.ui.Messages.getString("MekView.Submarine"));
            } else if (mount.movementMode().isVTOL()) {
                mountFeatures.add(megamek.client.ui.Messages.getString("MekView.VTOL"));
            }
            result.add(new LabeledLine(
                  megamek.client.ui.Messages.getString("MekView.Mount"),
                  "%s%s".formatted(mount.name(), mountFeatures)));

            if ((mount.getBurstDamageDice() > 0) || (mount.vehicleDamage() > 0)) {
                result.add(new LabeledLine(
                      megamek.client.ui.Messages.getString("MekView.MountBonusDamage"),
                      "+%dD6 (%d)".formatted(mount.getBurstDamageDice(), mount.vehicleDamage())));
            }
            if ((mount.maxWaterDepth() > 0) && (mount.maxWaterDepth() < Integer.MAX_VALUE)) {
                result.add(new LabeledLine(
                      megamek.client.ui.Messages.getString("MekView.MountWaterDepth"),
                      mount.maxWaterDepth() + ""));
            }
            if (mount.movementMode().isSubmarine() && (mount.getUWEndurance() < Integer.MAX_VALUE)) {
                result.add(new LabeledLine(
                      megamek.client.ui.Messages.getString("MekView.MountWaterEndurance"),
                      Messages.getString("MekView.MountWaterEnduranceValue")
                            .formatted(mount.getUWEndurance())));
            }
        }
        return result;
    }

    @Override
    protected List<ViewElement> createLoadoutBlock() {
        List<ViewElement> result = super.createLoadoutBlock();

        if (infantry.getSpecializations() > 0) {
            var specList = new ItemList("Infantry Specializations");
            for (int i = 0; i < Infantry.NUM_SPECIALIZATIONS; i++) {
                int spec = 1 << i;
                if (infantry.hasSpecialization(spec)) {
                    specList.addItem(Infantry.getSpecializationName(spec));
                }
            }
            result.add(new PlainLine());
            result.add(specList);
        }

        if (infantry.getCrew() != null) {
            ArrayList<String> augmentations = new ArrayList<>();
            for (Enumeration<IOption> e = infantry.getCrew().getOptions(PilotOptions.MD_ADVANTAGES);
                  e.hasMoreElements(); ) {
                final IOption o = e.nextElement();
                if (o.booleanValue()) {
                    String augName = o.getDisplayableName();
                    // Append prosthetic enhancement details for Enhanced/Improved Enhanced
                    if (OptionsConstants.MD_PL_ENHANCED.equals(o.getName())
                          || OptionsConstants.MD_PL_I_ENHANCED.equals(o.getName())) {
                        String details = getProstheticEnhancementDetails();
                        if (!details.isEmpty()) {
                            augName += " (" + details + ")";
                        }
                    }
                    // Append extraneous limb details for Extraneous Limbs option
                    if (OptionsConstants.MD_PL_EXTRA_LIMBS.equals(o.getName())) {
                        String details = getExtraneousLimbDetails();
                        if (!details.isEmpty()) {
                            augName += " (" + details + ")";
                        }
                    }
                    augmentations.add(augName);
                }
            }

            if (!augmentations.isEmpty()) {
                var augList = new ItemList("Augmentations");
                for (String aug : augmentations) {
                    augList.addItem(aug);
                }
                result.add(new PlainLine());
                result.add(augList);
            }
        }
        return result;
    }

    /**
     * Gets a formatted string describing the configured prosthetic enhancements (regular slots).
     *
     * @return String like "Laser x2, Grappler x1" or empty string if none configured
     */
    private String getProstheticEnhancementDetails() {
        StringBuilder details = new StringBuilder();
        if (infantry.hasProstheticEnhancement1()) {
            ProstheticEnhancementType type1 = infantry.getProstheticEnhancement1();
            details.append(type1.getDisplayName()).append(" x").append(infantry.getProstheticEnhancement1Count());
        }
        if (infantry.hasProstheticEnhancement2()) {
            if (details.length() > 0) {
                details.append(", ");
            }
            ProstheticEnhancementType type2 = infantry.getProstheticEnhancement2();
            details.append(type2.getDisplayName()).append(" x").append(infantry.getProstheticEnhancement2Count());
        }
        return details.toString();
    }

    /**
     * Gets a formatted string describing the configured extraneous limb enhancements. Each pair always provides 2
     * items.
     *
     * @return String like "Laser x2, Grappler x2" or empty string if none configured
     */
    private String getExtraneousLimbDetails() {
        StringBuilder details = new StringBuilder();
        if (infantry.hasExtraneousPair1()) {
            ProstheticEnhancementType pair1Type = infantry.getExtraneousPair1();
            details.append(pair1Type.getDisplayName()).append(" x2");
        }
        if (infantry.hasExtraneousPair2()) {
            if (details.length() > 0) {
                details.append(", ");
            }
            ProstheticEnhancementType pair2Type = infantry.getExtraneousPair2();
            details.append(pair2Type.getDisplayName()).append(" x2");
        }
        return details.toString();
    }

    @Override
    protected List<ViewElement> getWeapons(boolean showDetail) {
        List<ViewElement> result = new ArrayList<>();
        result.add(new LabeledLine(Messages.getString("MekView.PrimaryWeapon"),
              (null != infantry.getPrimaryWeapon()) ? infantry.getPrimaryWeapon().getDesc() : MESSAGE_NONE));
        result.add(new LabeledLine(Messages.getString("MekView.SecondWeapon"),
              secondaryCIWeaponDescriptor()));
        result.add(new LabeledLine(Messages.getString("MekView.DmgPerTrooper"),
              "%3.3f".formatted(infantry.getDamagePerTrooper())));

        if (infantry.hasFieldWeapon()) {
            result.add(new PlainLine());
            List<Mounted<?>> allFieldGuns = infantry.originalFieldWeapons();
            List<Mounted<?>> activeFieldGuns = infantry.activeFieldWeapons();
            EquipmentType fieldGunType = allFieldGuns.get(0).getType();
            String typeName = TestInfantry.isFieldArtilleryType(fieldGunType)
                  ? Messages.getString("MekView.FieldArty")
                  : Messages.getString("MekView.FieldGun");
            ViewElement fieldGunText = createFieldGunText(fieldGunType, activeFieldGuns, allFieldGuns);
            result.add(new LabeledLine(typeName, fieldGunText));
        }
        return result;
    }

    private static ViewElement createFieldGunText(EquipmentType fieldGunType, List<Mounted<?>> activeFieldGuns,
          List<Mounted<?>> fieldGuns) {

        String gunCount = TestInfantry.isFieldArtilleryType(fieldGunType) ?
              "" :
              " (%s)".formatted(activeFieldGuns.size());
        if (activeFieldGuns.isEmpty()) {
            return new DestroyedElement("%s (destroyed)".formatted(fieldGunType.getName()));
        } else if (activeFieldGuns.size() < fieldGuns.size()) {
            return new DamagedElement("%s%s".formatted(fieldGunType.getName(), gunCount));
        } else {
            return new PlainElement("%s%s".formatted(fieldGunType.getName(), gunCount));
        }
    }

    private String secondaryCIWeaponDescriptor() {
        if (infantry.getSecondaryWeapon() != null) {
            return "%s (%d per Squad)".formatted(infantry.getSecondaryWeapon().getDesc(),
                  infantry.getSecondaryWeaponsPerSquad());
        } else {
            return MESSAGE_NONE;
        }
    }

    @Override
    protected List<ViewElement> createArmorElements() {
        List<ViewElement> result = new ArrayList<>();

        ViewElement troopers = new PlainElement(infantry.getShootingStrength());
        if (infantry.getShootingStrength() == 0) {
            troopers = new DestroyedElement(0);
        } else if (infantry.getShootingStrength() < infantry.getOriginalTrooperCount()) {
            troopers = new DamagedElement(infantry.getShootingStrength());
        }
        result.add(new LabeledLine(Messages.getString("MekView.Men"), troopers));

        String squadCompositionFormat =
              (infantry.getMount() != null) && (infantry.getMount().size() != InfantryMount.BeastSize.LARGE)
                    ? Messages.getString("MekView.CreaturesComposition")
                    : Messages.getString("MekView.SquadComposition");
        String squadComposition = squadCompositionFormat.formatted(infantry.getSquadCount());
        result.add(new LabeledLine(Messages.getString("MekView.Composition"), squadComposition));

        result.add(new LabeledLine(Messages.getString("MekView.Armor"), getInfantryArmor()));
        result.add(new LabeledLine(Messages.getString("MekView.DamageDivisor"), getDamageDivisor()));
        return result;
    }

    private String getDamageDivisor() {
        double damageDivisor = infantry.calcDamageDivisor();
        String format = (damageDivisor == (int) damageDivisor) ? "%1.0f" : "%1.1f";
        String divisorAsString = format.formatted(infantry.calcDamageDivisor());
        if (infantry.isArmorEncumbering()) {
            divisorAsString += "E";
        }
        return divisorAsString;
    }

    private String getInfantryArmor() {
        String armorDescription = "None";
        EquipmentType armorKit = infantry.getArmorKit();
        if (armorKit != null) {
            armorDescription = armorKit.getName();
            StringJoiner abilities = new StringJoiner(", ", " (", ")");
            abilities.setEmptyValue("");

            if (infantry.hasSpaceSuit()) {
                abilities.add("Spacesuit");
            }

            if (infantry.hasDEST()) {
                abilities.add("DEST");
            }

            // Sneak Suit abilities are part of the armor name and don't need to be listed
            if (!infantry.hasSneakCamo()
                  && (infantry.getCrew() != null && infantry.hasAbility(OptionsConstants.MD_DERMAL_CAMO_ARMOR))) {
                abilities.add("Camo");
            }

            armorDescription += abilities.toString();
        }
        return armorDescription;
    }

    @Override
    protected ViewElement createEngineElement() {
        return new EmptyElement();
    }

    @Override
    protected TableElement getAmmo() {
        TableElement ammoTable = new TableElement(2);
        ammoTable.setColNames("Ammo", "Shots");
        ammoTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_CENTER);

        for (AmmoMounted mounted : infantry.getAmmo()) {
            if (hideAmmo(mounted)) {
                continue;
            }

            ViewElement[] row = new ViewElement[2];
            row[0] = new PlainElement(mounted.getName());

            if (mounted.isDestroyed() || (mounted.getUsableShotsLeft() < 1)) {
                row[1] = new DestroyedElement(mounted.getBaseShotsLeft());
            } else if (mounted.getUsableShotsLeft() < mounted.getType().getShots()) {
                row[1] = new DamagedElement(mounted.getBaseShotsLeft());
            } else {
                row[1] = new PlainElement(mounted.getBaseShotsLeft());
            }
            ammoTable.addRow(row);
        }

        return ammoTable;
    }
}
