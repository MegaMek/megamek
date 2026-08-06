/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.customMek;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import megamek.client.ui.GBC2;
import megamek.client.ui.Messages;
import megamek.codeUtilities.MathUtility;
import megamek.common.SimpleTechLevel;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.ConvInfantry;
import megamek.common.weapons.infantry.InfantryWeapon;

public class InfantryArmorPanel {

    final private ConvInfantry infantry;
    private final JComboBox<String> cbArmorKit = new JComboBox<>();
    private final JTextField fldDivisor = new JTextField(3);

    private final JCheckBox chEncumber = new JCheckBox(Messages.getString("CustomMekDialog.labEncumber"));
    private final JCheckBox chSpaceSuit = new JCheckBox(Messages.getString("CustomMekDialog.labSpaceSuit"));
    private final JCheckBox chDEST = new JCheckBox(Messages.getString("CustomMekDialog.labDEST"));
    private final JCheckBox chSneakCamo = new JCheckBox(Messages.getString("CustomMekDialog.labSneakCamo"));
    private final JCheckBox chSneakIR = new JCheckBox(Messages.getString("CustomMekDialog.labSneakIR"));
    private final JCheckBox chSneakECM = new JCheckBox(Messages.getString("CustomMekDialog.labSneakECM"));
    private final List<JCheckBox> chSpecs = new ArrayList<>(ConvInfantry.NUM_SPECIALIZATIONS);

    private final List<EquipmentType> armorKits = new ArrayList<>();

    private final JComboBox<String> cbDisposableWeapon = new JComboBox<>();
    private final List<InfantryWeapon> disposableWeapons = new ArrayList<>();
    private boolean disposableWeaponConfigurable = false;

    public InfantryArmorPanel(ConvInfantry entity, JPanel parentPanel, GBC2 gbc) {
        infantry = entity;

        JComponent armorTitle = new EquipChoicePanel.SectionTitleLabel(Messages.getString("CustomMekDialog.infArmorSection"));
        parentPanel.add(armorTitle, gbc.fullLine());

        SimpleTechLevel gameTechLevel = SimpleTechLevel.getGameTechLevel(entity.getGame());
        if (gameTechLevel != SimpleTechLevel.STANDARD &&
              gameTechLevel != SimpleTechLevel.INTRO) {
            JLabel labArmor = new JLabel(Messages.getString("CustomMekDialog.labInfantryArmor"));
            JLabel labDivisor = new JLabel(Messages.getString("CustomMekDialog.labDamageDivisor"));
            parentPanel.add(labArmor, gbc.forLabel());
            parentPanel.add(cbArmorKit, gbc.eol());
            parentPanel.add(labDivisor, gbc.forLabel());
            parentPanel.add(fldDivisor, gbc.eol());

            parentPanel.add(Box.createVerticalStrut(5), gbc.fullLine());

            parentPanel.add(new JLabel(), gbc.forLabel());
            parentPanel.add(chEncumber, gbc.oneColumn());
            parentPanel.add(chSneakCamo, gbc.eol());
            parentPanel.add(new JLabel(), gbc.forLabel());
            parentPanel.add(chSpaceSuit, gbc.oneColumn());
            parentPanel.add(chSneakIR, gbc.eol());
            parentPanel.add(new JLabel(), gbc.forLabel());
            parentPanel.add(chDEST, gbc.oneColumn());
            parentPanel.add(chSneakECM, gbc.eol());
        }

        int year = entity.getGame().getOptions().intOption("year");

        // If the rules level isn't at least Advanced, these won't be displayed, but it will iterate them still to 
        // avoid potential issues.
        for (EquipmentType et : MiscType.allTypes()) {
            if (et instanceof MiscType miscType && miscType.hasFlag(MiscType.F_ARMOR_KIT) &&
                  et.isLegal(year,
                        gameTechLevel,
                        entity.isClan(),
                        entity.isMixedTech(),
                        entity.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_SHOW_EXTINCT))) {
                armorKits.add(et);
            }
        }

        armorKits.sort(Comparator.comparing(EquipmentType::getName));

        cbArmorKit.addItem(Messages.getString("CustomMekDialog.Custom"));
        armorKits.forEach(k -> cbArmorKit.addItem(k.getName()));
        EquipmentType kit = infantry.getArmorKit();

        if (kit == null) {
            cbArmorKit.setSelectedIndex(0);
        } else {
            cbArmorKit.setSelectedIndex(armorKits.indexOf(kit) + 1);
        }

        fldDivisor.setText(Double.toString(infantry.calcDamageDivisor()));
        chEncumber.setSelected(infantry.isArmorEncumbering());
        chSpaceSuit.setSelected(infantry.hasSpaceSuit());
        chDEST.setSelected(infantry.hasDEST());
        chSneakCamo.setSelected(infantry.hasSneakCamo());
        chSneakIR.setSelected(infantry.hasSneakIR());
        chSneakECM.setSelected(infantry.hasSneakECM());
        armorStateChanged();

        cbArmorKit.addActionListener(e -> {
            armorStateChanged();
            updateArmorValues();
        });
        chDEST.addItemListener(e -> armorStateChanged());

        JComponent specTitle = new EquipChoicePanel.SectionTitleLabel(Messages.getString("CustomMekDialog.infSpecSection"));
        parentPanel.add(specTitle, gbc.fullLine());

        for (int i = 0; i < ConvInfantry.NUM_SPECIALIZATIONS; i++) {
            int spec = 1 << i;
            JCheckBox newSpec = new JCheckBox(ConvInfantry.getSpecializationName(spec));
            newSpec.setToolTipText(ConvInfantry.getSpecializationTooltip(spec));
            chSpecs.add(newSpec);
            parentPanel.add(new JLabel(), gbc.oneColumn());
            parentPanel.add(newSpec, gbc.eol());
        }

        for (int i = 0; i < ConvInfantry.NUM_SPECIALIZATIONS; i++) {
            int spec = 1 << i;
            chSpecs.get(i).setSelected(infantry.hasSpecialization(spec));
        }

        // Disposable Weapon (TO:AuE p.116, Corrected Sixth Printing) - only configurable when the advanced rule is
        // enabled
        if (entity.getGame().getOptions()
              .booleanOption(OptionsConstants.ADVANCED_COMBAT_DISPOSABLE_INFANTRY_WEAPONS)) {
            disposableWeaponConfigurable = true;
            addDisposableWeaponSection(parentPanel, gbc);
        }
    }

    /**
     * Builds the Disposable Weapon (TO:AuE p.116, Corrected Sixth Printing) chooser: a "None" option plus every legal
     * {@code (1-D)} infantry weapon, pre-selecting the platoon's current Disposable Weapon.
     *
     * @param parentPanel the panel the controls are added to
     * @param gbc         the shared layout constraints
     */
    private void addDisposableWeaponSection(JPanel parentPanel, GBC2 gbc) {
        JComponent disposableTitle = new EquipChoicePanel.SectionTitleLabel(
              Messages.getString("CustomMekDialog.infDisposableSection"));
        parentPanel.add(disposableTitle, gbc.fullLine());

        Game game = infantry.getGame();
        GameOptions gameOptions = game.getOptions();
        int year = gameOptions.intOption(OptionsConstants.ALLOWED_YEAR);
        SimpleTechLevel legalLevel = SimpleTechLevel.getGameTechLevel(game);
        boolean showExtinct = gameOptions.booleanOption(OptionsConstants.ALLOWED_SHOW_EXTINCT);
        for (EquipmentType equipmentType : EquipmentType.allTypes()) {
            if ((equipmentType instanceof InfantryWeapon infantryWeapon)
                  && infantryWeapon.hasFlag(WeaponType.F_INF_DISPOSABLE)
                  && equipmentType.isLegal(year, legalLevel, infantry.isClan(), infantry.isMixedTech(), showExtinct)) {
                disposableWeapons.add(infantryWeapon);
            }
        }
        disposableWeapons.sort(Comparator.comparing(EquipmentType::getName));

        cbDisposableWeapon.addItem(Messages.getString("CustomMekDialog.infDisposableNone"));
        disposableWeapons.forEach(weapon -> cbDisposableWeapon.addItem(weapon.getName()));
        InfantryWeapon current = infantry.getDisposableWeapon();
        cbDisposableWeapon.setSelectedIndex((current == null) ? 0 : disposableWeapons.indexOf(current) + 1);

        parentPanel.add(new JLabel(Messages.getString("CustomMekDialog.labDisposableWeapon")), gbc.forLabel());
        parentPanel.add(cbDisposableWeapon, gbc.eol());
    }

    public void armorStateChanged() {
        fldDivisor.setEnabled(cbArmorKit.getSelectedIndex() == 0);
        chEncumber.setEnabled(cbArmorKit.getSelectedIndex() == 0);
        chSpaceSuit.setEnabled(cbArmorKit.getSelectedIndex() == 0);
        chDEST.setEnabled(cbArmorKit.getSelectedIndex() == 0);
        chSneakCamo.setEnabled(cbArmorKit.getSelectedIndex() == 0 && !chDEST.isSelected());
        chSneakIR.setEnabled(cbArmorKit.getSelectedIndex() == 0 && !chDEST.isSelected());
        chSneakECM.setEnabled(cbArmorKit.getSelectedIndex() == 0 && !chDEST.isSelected());
    }

    public void updateArmorValues() {
        if (cbArmorKit.getSelectedIndex() > 0) {
            EquipmentType kit = armorKits.get(cbArmorKit.getSelectedIndex() - 1);
            fldDivisor.setText(Double.toString(((MiscType) kit).getDamageDivisor()));
            chEncumber.setSelected(kit.hasFlag(MiscTypeFlag.S_ENCUMBERING));
            chSpaceSuit.setSelected(kit.hasFlag(MiscTypeFlag.S_SPACE_SUIT));
            chDEST.setSelected(kit.hasFlag(MiscTypeFlag.S_DEST));
            chSneakCamo.setSelected(kit.hasFlag(MiscTypeFlag.S_SNEAK_CAMO));
            chSneakIR.setSelected(kit.hasFlag(MiscTypeFlag.S_SNEAK_IR));
            chSneakECM.setSelected(kit.hasFlag(MiscTypeFlag.S_SNEAK_ECM));
        }
    }

    public void applyChoice() {
        if (cbArmorKit.getSelectedIndex() > 0) {
            infantry.setArmorKit(armorKits.get(cbArmorKit.getSelectedIndex() - 1));
        } else {
            infantry.setArmorKit(null);
            infantry.setCustomArmorDamageDivisor(MathUtility.parseDouble(fldDivisor.getText(), 0.0));
            infantry.setArmorEncumbering(chEncumber.isSelected());
            infantry.setSpaceSuit(chSpaceSuit.isSelected());
            infantry.setDEST(chDEST.isSelected());
            if (!chDEST.isSelected()) {
                infantry.setSneakCamo(chSneakCamo.isSelected());
                infantry.setSneakIR(chSneakIR.isSelected());
                infantry.setSneakECM(chSneakECM.isSelected());
            }
        }
        int spec = 0;
        for (int i = 0; i < ConvInfantry.NUM_SPECIALIZATIONS; i++) {
            if (chSpecs.get(i).isSelected()) {
                spec |= 1 << i;
            }
        }
        infantry.setSpecializations(spec);

        if (disposableWeaponConfigurable) {
            int selectedIndex = cbDisposableWeapon.getSelectedIndex();
            InfantryWeapon selectedDisposable = (selectedIndex > 0) ? disposableWeapons.get(selectedIndex - 1) : null;
            infantry.equipDisposableWeapon(selectedDisposable);
        }
    }

    public void setEnabled(boolean enabled) {
        cbArmorKit.setEnabled(enabled);
        if (enabled) {
            armorStateChanged();
        } else {
            fldDivisor.setEnabled(false);
            chEncumber.setEnabled(false);
            chSpaceSuit.setEnabled(false);
            chDEST.setEnabled(false);
            chSneakCamo.setEnabled(false);
            chSneakIR.setEnabled(false);
            chSneakECM.setEnabled(false);
        }
        for (JCheckBox spec : chSpecs) {
            spec.setEnabled(enabled);
        }
        cbDisposableWeapon.setEnabled(enabled && disposableWeaponConfigurable);
    }
}
