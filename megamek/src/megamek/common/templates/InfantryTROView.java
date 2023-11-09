/*
 * MegaMek - Copyright (C) 2018 - The MegaMek Team
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
package megamek.common.templates;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import megamek.common.Compute;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;
import megamek.common.Infantry;
import megamek.common.Messages;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import megamek.common.options.IOption;
import megamek.common.verifier.EntityVerifier;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * Creates a TRO template model for conventional infantry.
 *
 * @author Neoancient
 *
 */
public class InfantryTROView extends TROView {

    private final Infantry inf;

    public InfantryTROView(Infantry infantry) {
        this.inf = infantry;
    }

    @Override
    protected String getTemplateFileName(boolean html) {
        return (html) ? "conv_infantry.ftlh" : "conv_infantry.ftl";
    }

    @Override
    protected void initModel(EntityVerifier verifier) {
        addBasicData(inf);
        addEntityFluff(inf);
        setModelData("transportWeight", inf.getWeight());
        setModelData("weaponPrimary", String.format("%d %s",
                (inf.getSquadSize() - inf.getSecondaryWeaponsPerSquad()) * inf.getSquadCount(), inf.getPrimaryWeapon().getName()));
        setModelData("weaponSecondary", (inf.getSecondaryWeapon() == null)
                ? Messages.getString("TROView.None")
                : String.format("%d %s", inf.getSecondaryWeaponsPerSquad() * inf.getSquadCount(), inf.getSecondaryWeapon().getName()));
        final EquipmentType armorKit = inf.getArmorKit();
        if (null != armorKit) {
            setModelData("armorKit", armorKit.getName());
        }

        final List<String> notes = new ArrayList<>();
        addWeaponNotes(notes);
        if (null != armorKit) {
            addArmorNotes(notes, armorKit);
        }
        addAugmentationNotes(notes);
        if (notes.isEmpty()) {
            setModelData("notes", Messages.getString("TROView.None"));
        } else {
            setModelData("notes", String.join(" ", notes));
        }

        if (inf.getMount() != null) {
            setModelData("motiveType", Messages.getString("TROView.BeastMounted") + ", " + inf.getMount().getName());
        } else {
            switch (inf.getMovementMode()) {
                case INF_LEG:
                    setModelData("motiveType", Messages.getString("TROView.Foot"));
                    break;
                case TRACKED:
                case HOVER:
                case WHEELED:
                    setModelData("motiveType",
                            Messages.getString("TROView.Mechanized") + "/" + inf.getMovementModeAsString());
                    break;
                case SUBMARINE:
                    setModelData("motiveType", Messages.getString("TROView.MechanizedSCUBA"));
                    break;
                default:
                    setModelData("motiveType", inf.getMovementModeAsString());
                    break;
            }
        }
        StringJoiner sj = new StringJoiner(", ");
        for (int i = 0; i < Infantry.NUM_SPECIALIZATIONS; i++) {
            if (inf.hasSpecialization(1 << i)) {
                sj.add(Infantry.getSpecializationName(1 << i));
            }
        }
        if (!sj.toString().isBlank()) {
            setModelData("specialty", sj.toString());
        } else {
            setModelData("specialty", Messages.getString("TROView.None"));
        }
        if (inf.getMovementMode() != EntityMovementMode.SUBMARINE) {
            setModelData("groundMP", inf.getWalkMP());
        }
        if (inf.getMovementMode() == EntityMovementMode.INF_JUMP) {
            setModelData("jumpMP", inf.getOriginalJumpMP());
        } else if (inf.getMovementMode() == EntityMovementMode.VTOL) {
            setModelData("vtolMP", inf.getOriginalJumpMP());
        } else if ((inf.getMovementMode() == EntityMovementMode.INF_UMU)
                || (inf.getMovementMode() == EntityMovementMode.SUBMARINE)) {
            setModelData("umuMP", inf.getOriginalJumpMP());
        }
        setModelData("squadSize", inf.getSquadSize());
        setModelData("squadCount", inf.getSquadCount());
        setModelData("armorDivisor", inf.calcDamageDivisor());
        InfantryWeapon rangeWeapon = inf.getPrimaryWeapon();
        if ((inf.getSecondaryWeaponsPerSquad() > 1) && (inf.getSecondaryWeapon() != null)) {
            rangeWeapon = inf.getSecondaryWeapon();
        }

        sj = new StringJoiner(", ");
        final int maxRange = rangeWeapon.getInfantryRange() * 3;
        int lastMod = Compute.getInfantryRangeMods(0, rangeWeapon, inf.getSecondaryWeapon(), false).getValue();
        int hex = 0;
        for (int range = 1; range <= (maxRange + 1); range++) {
            final int mod = Compute.getInfantryRangeMods(range, rangeWeapon, inf.getSecondaryWeapon(), false)
                    .getValue();
            if (mod != lastMod) {
                if ((range - hex) > 1) {
                    sj.add(String.format("%+d (%d-%d Hexes)", lastMod, hex, range - 1));
                } else {
                    sj.add(String.format("%+d (%d Hexes)", lastMod, hex));
                }
                lastMod = mod;
                hex = range;
            }
        }
        setModelData("toHitModifiers", sj.toString());

        sj = new StringJoiner(", ");
        int lastStrength = inf.getShootingStrength();
        final double dpt = Math.round(inf.getDamagePerTrooper() * lastStrength) / (double) lastStrength;
        int lastDamage = (int) Math.round(dpt * lastStrength);
        for (int strength = inf.getShootingStrength(); strength >= 0; strength--) {
            final int damage = (int) Math.round(dpt * strength);
            if (damage < lastDamage) {
                if ((lastStrength - strength) > 1) {
                    sj.add(String.format("%d (%d-%d)", lastDamage, lastStrength, strength + 1));
                } else {
                    sj.add(String.format("%d (%d)", lastDamage, lastStrength));
                }
                lastDamage = damage;
                lastStrength = strength;
            }
        }
        setModelData("maxDamage", sj.toString());
    }

    private void addWeaponNotes(List<String> notes) {
        if ((inf.getMovementMode() == EntityMovementMode.INF_UMU)
                || (inf.getMovementMode() == EntityMovementMode.SUBMARINE)) {
            notes.add(Messages.getString("TROView.InfantryNote.SCUBA"));
        }
        final List<EquipmentType> fieldGuns = inf.getWeaponList().stream()
                .filter(m -> m.getLocation() == Infantry.LOC_FIELD_GUNS).map(Mounted::getType)
                .collect(Collectors.toList());
        final int shots = inf.getAmmo().stream().filter(m -> m.getLocation() == Infantry.LOC_FIELD_GUNS)
                .mapToInt(Mounted::getBaseShotsLeft).sum();
        if (fieldGuns.size() > 1) {
            notes.add(String.format(Messages.getString("TROView.InfantryNote.FieldGuns"), fieldGuns.size(),
                    fieldGuns.get(0).getName(), shots / fieldGuns.size(), (int) fieldGuns.get(0).getTonnage(inf)));
        } else if (!fieldGuns.isEmpty()) {
            notes.add(String.format(Messages.getString("TROView.InfantryNote.SingleFieldGun"),
                    fieldGuns.get(0).getName(), shots, (int) fieldGuns.get(0).getTonnage(inf)));
        }
        if ((inf.getSecondaryWeaponsPerSquad() > 1) && (inf.getSecondaryWeapon() != null)) {
            if (inf.getSecondaryWeapon().hasFlag(WeaponType.F_INF_BURST)) {
                notes.add(Messages.getString("TROView.InfantryNote.Burst"));
            }
            if (inf.getSecondaryWeapon().hasFlag(WeaponType.F_INF_NONPENETRATING)) {
                notes.add(Messages.getString("TROView.InfantryNote.NonPenetrating"));
            }
            if (inf.getSecondaryWeapon().hasFlag(WeaponType.F_INF_AA)) {
                notes.add(Messages.getString("TROView.InfantryNote.AA"));
            }
            if (inf.getSecondaryWeapon().hasFlag(WeaponType.F_FLAMER)) {
                notes.add(Messages.getString("TROView.InfantryNote.Heat"));
            }
        }
        if (inf.getMount() != null) {
            if (inf.getMount().getBurstDamageDice() > 0) {
                notes.add(String.format(Messages.getString("TROView.InfantryNote.MountInfantryDamage.format"),
                        inf.getMount().getBurstDamageDice()));
            }
            if (inf.getMount().getVehicleDamage() > 0) {
                notes.add(String.format(Messages.getString("TROView.InfantryNote.MountVehicleDamage.format"),
                        inf.getMount().getVehicleDamage()));
            }
            if (inf.getMount().getSize().toHitMod != 0) {
                notes.add(String.format(Messages.getString("TROView.InfantryNote.MountSizeMod.format"),
                        inf.getMount().getSize().toHitMod));
            }
        }
    }

    private void addArmorNotes(List<String> notes, EquipmentType armorKit) {
        if (armorKit.hasSubType(MiscType.S_DEST)) {
            notes.add(Messages.getString("TROView.InfantryNote.DESTArmor"));
        }
        if (armorKit.hasSubType(MiscType.S_SNEAK_CAMO)) {
            notes.add(Messages.getString("TROView.InfantryNote.CamoArmor"));
        }
        if (armorKit.hasSubType(MiscType.S_SNEAK_IR)) {
            notes.add(Messages.getString("TROView.InfantryNote.IRArmor"));
        }
        if (armorKit.hasSubType(MiscType.S_SNEAK_ECM)) {
            notes.add(Messages.getString("TROView.InfantryNote.ECMArmor"));
        }
    }

    private void addAugmentationNotes(List<String> notes) {
        final List<IOption> options = new ArrayList<>();
        for (final Enumeration<IOption> e = inf.getCrew().getOptions().getOptions(); e.hasMoreElements();) {
            final IOption option = e.nextElement();
            if (option.booleanValue()) {
                options.add(option);
            }
        }
        if (!options.isEmpty()) {
            notes.add(Messages.getString("TROView.InfantryNote.Augmented"));
            options.forEach(o -> notes.add(o.getDisplayableName().replaceAll("\\s+\\(Not Implemented\\)", "") + ": "
                    + o.getDescription().replaceAll("See IO.*", "")));
        }
    }
}
