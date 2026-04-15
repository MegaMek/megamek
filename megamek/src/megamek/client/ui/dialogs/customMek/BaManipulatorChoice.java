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

import static megamek.common.battleArmor.BattleArmor.MOUNT_LOC_LEFT_ARM;
import static megamek.common.battleArmor.BattleArmor.MOUNT_LOC_RIGHT_ARM;
import static megamek.common.equipment.EquipmentTypeLookup.BA_MODULAR_EQUIPMENT_ADAPTOR;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.ChangeEvent;

import megamek.client.ui.GBC2;
import megamek.client.ui.Messages;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.exceptions.LocationFullException;
import megamek.common.ui.SmallFontHelpTextLabel;
import megamek.common.units.BaConstructionUtil;
import megamek.common.units.ConstructionUtil;
import megamek.common.util.RoundWeight;
import megamek.common.verifier.TestBattleArmor;
import megamek.common.verifier.TestEntity;

import static megamek.common.verifier.TestBattleArmor.BAManipulator;

/**
 * This class shows selectors for a BA's Manipulators; these are enabled only when there are respective Modular
 * Equipment Adaptors; if there is an Armored Glove selected, a selector for an AP weapon for the glove(s) is also
 * shown.
 */
class BaManipulatorChoice {

    private final JLabel leftMeaInfo = new SmallFontHelpTextLabel("  " + Messages.getString("CustomMekDialog.ba.mea"));
    private final JLabel rightMeaInfo = new SmallFontHelpTextLabel("  " + Messages.getString("CustomMekDialog.ba.mea"));

    private boolean hasLeftModularEquipmentAdaptor;
    private boolean hasRightModularEquipmentAdaptor;

    private final JComboBox<BAManipulator> leftManipulatorSelect = new JComboBox<>();
    private final JComboBox<BAManipulator> rightManipulatorSelect = new JComboBox<>();

    private final SpinnerNumberModel cargoLifterCapacityModel = new SpinnerNumberModel(0.5, 0.5, 80, 0.5);
    private final JSpinner cargoLifterCapacity = new JSpinner(cargoLifterCapacityModel);
    private final JLabel spacer = new JLabel();
    private final JLabel cargoLiftWeightInfo = new SmallFontHelpTextLabel();
    private final JLabel cargoLifterSizeLabel = new JLabel(Messages.getString("CustomMekDialog.ba.cargoCapacity"),
          SwingConstants.RIGHT);
    private final JLabel gloveChoiceLabel = new JLabel(Messages.getString("CustomMekDialog.ba.armGloveWeapon"),
          SwingConstants.RIGHT);
    private final JLabel lblfreeWeight = new JLabel("", SwingConstants.CENTER);

    private final JComboBox<String> armoredGloveWeaponSelect = new JComboBox<>();

    private final BattleArmor battleArmor;

    /** Used for weight calculation only, not added to the unit. */
    private final MiscMounted cargoLifterTemporaryItem;

    private boolean ignoreEvents = false;
    private boolean allowEditing = true;

    private final List<WeaponType> agWeaponTypes = new Vector<>();

    BaManipulatorChoice(BattleArmor battleArmor, List<WeaponType> agWeaponTypes, JPanel parentPanel, GBC2 gbc) {
        this.battleArmor = battleArmor;
        this.agWeaponTypes.addAll(agWeaponTypes);
        cargoLifterTemporaryItem = new MiscMounted(battleArmor,
              (MiscType) EquipmentType.get(EquipmentTypeLookup.BA_MANIPULATOR_CARGO_LIFTER));

        Vector<String> agWeaponNames = new Vector<>();
        agWeaponNames.add("None");
        agWeaponNames.addAll(agWeaponTypes.stream().map(EquipmentType::getName).toList());
        armoredGloveWeaponSelect.setModel(new DefaultComboBoxModel<>(agWeaponNames));

        leftManipulatorSelect.setRenderer(new ManipulatorRenderer(leftManipulatorSelect));
        rightManipulatorSelect.setRenderer(new ManipulatorRenderer(rightManipulatorSelect));

        parentPanel.add(lblfreeWeight, gbc.eol());
        parentPanel.add(Box.createVerticalStrut(5), gbc.eol());

        parentPanel.add(new JLabel(Messages.getString("CustomMekDialog.ba.leftArm"), SwingConstants.RIGHT),
              gbc.forLabel());
        parentPanel.add(leftManipulatorSelect, gbc.eol());

        parentPanel.add(new JLabel(), gbc.forLabel());
        parentPanel.add(leftMeaInfo, gbc.eol());
        parentPanel.add(Box.createVerticalStrut(5), gbc.eol());

        parentPanel.add(new JLabel(Messages.getString("CustomMekDialog.ba.rightArm"), SwingConstants.RIGHT),
              gbc.forLabel());
        parentPanel.add(rightManipulatorSelect, gbc.eol());

        parentPanel.add(new JLabel(), gbc.forLabel());
        parentPanel.add(rightMeaInfo, gbc.eol());
        parentPanel.add(Box.createVerticalStrut(5), gbc.eol());

        parentPanel.add(cargoLifterSizeLabel, gbc.forLabel());
        parentPanel.add(cargoLifterCapacity, gbc.eol());

        parentPanel.add(spacer, gbc.forLabel());
        parentPanel.add(cargoLiftWeightInfo, gbc.eol());

        parentPanel.add(gloveChoiceLabel, gbc.forLabel());
        parentPanel.add(armoredGloveWeaponSelect, gbc.eol());

        setValuesFromBattleArmor();
        leftManipulatorSelect.addActionListener(this::manipulatorSelected);
        rightManipulatorSelect.addActionListener(this::manipulatorSelected);
        cargoLifterCapacity.addChangeListener(this::cargoLifterSizeChanged);
        updateAfterChange();
    }

    private static double getMaxTrooperWeight(BattleArmor battleArmor) {
        TestBattleArmor testBattleArmor = (TestBattleArmor) TestEntity.getEntityVerifier(battleArmor);
        double maxTrooperWeight = 0;
        for (int i = 1; i <= battleArmor.getTroopers(); i++) {
            double trooperWeight = testBattleArmor.calculateWeight(i);
            if (trooperWeight > maxTrooperWeight) {
                maxTrooperWeight = trooperWeight;
            }
        }
        return maxTrooperWeight * 1000;
    }

    /**
     * Enter values from the BattleArmor. Not to be called after construction.
     */
    void setValuesFromBattleArmor() {
        try {
            ignoreEvents = true;

            hasLeftModularEquipmentAdaptor =
                  battleArmor.hasMiscInMountLocation(BA_MODULAR_EQUIPMENT_ADAPTOR, MOUNT_LOC_LEFT_ARM);
            hasRightModularEquipmentAdaptor =
                  battleArmor.hasMiscInMountLocation(BA_MODULAR_EQUIPMENT_ADAPTOR, MOUNT_LOC_RIGHT_ARM);

            double maxActualTrooperKg = getMaxTrooperWeight(battleArmor);
            double freeKg =
                  Math.round(battleArmor.getTrooperWeight() * 1000 - maxActualTrooperKg + totalManipulatorKg());
            lblfreeWeight.setText(Messages.getString("CustomMekDialog.ba.freeWeight", freeKg));
            lblfreeWeight.setVisible(hasLeftModularEquipmentAdaptor || hasRightModularEquipmentAdaptor);

            Vector<BAManipulator> validManipulators = new Vector<>(Arrays.asList(BAManipulator.values()));
            if (battleArmor.countMisc(BA_MODULAR_EQUIPMENT_ADAPTOR) == 1) {
                validManipulators.removeIf(m -> m.pairMounted);
            }
            leftManipulatorSelect.setModel(new DefaultComboBoxModel<>(validManipulators));
            rightManipulatorSelect.setModel(new DefaultComboBoxModel<>(validManipulators));

            BAManipulator leftManipulator = BAManipulator.getManipulator(battleArmor.getLeftManipulatorName());
            if (leftManipulator != null) {
                leftManipulatorSelect.setSelectedItem(leftManipulator);
            }

            BAManipulator rightManipulator = BAManipulator.getManipulator(battleArmor.getRightManipulatorName());
            if (rightManipulator != null) {
                rightManipulatorSelect.setSelectedItem(rightManipulator);
            }

            leftMeaInfo.setVisible(hasLeftModularEquipmentAdaptor);
            rightMeaInfo.setVisible(hasRightModularEquipmentAdaptor);

            findArmoredGloveWithWeapon()
                  .ifPresent(glove -> armoredGloveWeaponSelect.setSelectedItem(glove.getLinked().getName()));

            MiscMounted manipulator = battleArmor.getManipulator(BattleArmor.MOUNT_LOC_LEFT_ARM);
            if (manipulator != null && manipulator.is(EquipmentTypeLookup.BA_MANIPULATOR_CARGO_LIFTER)) {
                cargoLifterCapacityModel.setValue(manipulator.getSize());
            }
        } finally {
            ignoreEvents = false;
        }
    }

    /**
     * Updates the GUI elements (enable/visible) after a selection change.
     */
    private void updateAfterChange() {
        leftManipulatorSelect.setEnabled(allowEditing && hasLeftModularEquipmentAdaptor);
        BAManipulator leftManipulatorItem = selectedManipulatorItem(leftManipulatorSelect);
        rightManipulatorSelect.setEnabled(allowEditing
              && !leftManipulatorItem.pairMounted
              && hasRightModularEquipmentAdaptor);

        cargoLifterCapacity.setVisible(leftManipulatorItem == BAManipulator.CARGO_LIFTER);
        cargoLifterSizeLabel.setVisible(cargoLifterCapacity.isVisible());
        cargoLifterCapacity.setEnabled(allowEditing);

        cargoLifterTemporaryItem.setSize(cargoLifterCapacityModel.getNumber().doubleValue());
        cargoLiftWeightInfo.setText(Messages.getString("CustomMekDialog.ba.cargoLifterWeight",
              cargoLifterTemporaryItem.getTonnage(RoundWeight.NEAREST_KG) * 2 * 1000));
        cargoLiftWeightInfo.setVisible(cargoLifterCapacity.isVisible());
        spacer.setVisible(cargoLifterCapacity.isVisible());

        boolean hasArmoredGlove = selectedManipulatorItem(leftManipulatorSelect) == BAManipulator.ARMORED_GLOVE
              || selectedManipulatorItem(rightManipulatorSelect) == BAManipulator.ARMORED_GLOVE;
        armoredGloveWeaponSelect.setVisible(hasArmoredGlove);
        gloveChoiceLabel.setVisible(hasArmoredGlove);
    }

    /**
     * Eventhandler for selecting a manipulator in one of the two dropdowns
     *
     * @param event the Swing event
     */
    public void manipulatorSelected(ActionEvent event) {
        if (ignoreEvents) {
            return;
        }
        JComboBox<BAManipulator> thisManipulatorSelector =
              (event.getSource() == leftManipulatorSelect) ? leftManipulatorSelect : rightManipulatorSelect;
        BAManipulator selectedManipulatorItem = selectedManipulatorItem(thisManipulatorSelector);

        JComboBox<BAManipulator> otherManipulatorSelector =
              (thisManipulatorSelector == leftManipulatorSelect) ? rightManipulatorSelect : leftManipulatorSelect;
        BAManipulator otherManipulatorItem = selectedManipulatorItem(otherManipulatorSelector);

        try {
            ignoreEvents = true;
            if (selectedManipulatorItem.pairMounted) {
                // the new manipulator is pair-mounted, therefore set the other selector
                otherManipulatorSelector.setSelectedItem(selectedManipulatorItem);
            } else if (otherManipulatorItem.pairMounted) {
                // the new manipulator is not pair-mounted but the previous was, therefore reset the other selector
                otherManipulatorSelector.setSelectedIndex(0);
            }
            updateAfterChange();
        } finally {
            ignoreEvents = false;
        }
    }

    /**
     * Eventhandler for changing Cargo Lifter size
     *
     * @param event the Swing event
     */
    public void cargoLifterSizeChanged(ChangeEvent event) {
        if (ignoreEvents) {
            return;
        }
        updateAfterChange();
    }

    /**
     * Returns the selected manipulator item from the given selector JComboBox or BAManipulator.NONE, if it cannot be
     * parsed or nothing is selected.
     *
     * @param manipulatorSelector The left or right arm manipulator selector
     *
     * @return The selected BAManipulator item or NONE as error fallback
     */
    @Nullable
    private BAManipulator selectedManipulatorItem(JComboBox<BAManipulator> manipulatorSelector) {
        return Objects.requireNonNullElse((BAManipulator) manipulatorSelector.getSelectedItem(), BAManipulator.NONE);
    }

    private MiscType getMisc(TestBattleArmor.BAManipulator baManipulator) {
        return (MiscType) EquipmentType.get(baManipulator.internalName);
    }

    void applyChoice() {
        setManipulator(selectedManipulatorItem(leftManipulatorSelect), MOUNT_LOC_LEFT_ARM);
        setManipulator(selectedManipulatorItem(rightManipulatorSelect), MOUNT_LOC_RIGHT_ARM);
        applyApWeapon();
    }

    //    @Override
    public void setEnabled(boolean enabled) {
        //        super.setEnabled(enabled);
        allowEditing = enabled;
        updateAfterChange();
    }

    /**
     * Adds and removes manipulator MiscMounteds on the unit so that the manipulator on the given mountLoc arm is the
     * one given as newManipulator (which may be none). Does not touch the other arm.
     *
     * @param newManipulator The new manipulator type
     * @param mountLoc       one of the two arm locations (MOUNT_LOC_x_ARM)
     */
    private void setManipulator(BAManipulator newManipulator, int mountLoc) {
        // If there is a manipulator in this arm and it's different from the selected one, remove it
        MiscMounted currentManipulator = battleArmor.getManipulator(mountLoc);
        if (currentManipulator != null && !currentManipulator.is(newManipulator.internalName)) {
            removeManipulator(mountLoc);
        }

        // When this arm is now empty, add the selected manipulator (which may be none)
        if (battleArmor.getManipulator(mountLoc) == null) {
            addManipulator(newManipulator, mountLoc);
        }

        // set a cargo lifter's size
        if (newManipulator == BAManipulator.CARGO_LIFTER) {
            battleArmor.getManipulator(mountLoc).setSize(cargoLifterCapacityModel.getNumber().doubleValue());
        }
    }

    /**
     * Adds a manipulator MiscMounted on the unit so that the manipulator on the given mountLoc arm is the one given as
     * newManipulator (which may be none, in which case, this method does nothing). Does not touch the other arm.
     *
     * @param newManipulator The new manipulator type (possibly NONE)
     * @param mountLoc       one of the two arm locations (MOUNT_LOC_x_ARM)
     */
    private void addManipulator(BAManipulator newManipulator, int mountLoc) {
        if (newManipulator != TestBattleArmor.BAManipulator.NONE) {
            try {
                Mounted<?> manipulator = battleArmor.addEquipment(getMisc(newManipulator), BattleArmor.LOC_SQUAD);
                manipulator.setBaMountLoc(mountLoc);
            } catch (LocationFullException ex) {
                // This is currently not thrown on BA
            }
        }
    }

    void applyApWeapon() {
        // when there is no armored glove selected, any AP weapons have already been removed by manipulator changes

        int selectedIndex = armoredGloveWeaponSelect.getSelectedIndex();
        WeaponType selectedWeaponType = null;
        if ((selectedIndex > 0) && (selectedIndex <= agWeaponTypes.size())) {
            // Need to account for the "None" selection
            selectedWeaponType = agWeaponTypes.get(selectedIndex - 1);
        }

        // when there is a glove with a weapon but it's different from the selected, remove the weapon first
        var gloveWithWeapon = findArmoredGloveWithWeapon();
        if (gloveWithWeapon.isPresent() && gloveWithWeapon.get().getLinked().getType() != selectedWeaponType) {
            Mounted<?> apWeapon = gloveWithWeapon.get().getLinked();
            ConstructionUtil.removeMounted(battleArmor, apWeapon);
        }

        // When there is now no AG with a weapon, but there is an AG, add the selected weapon (which may be none)
        if (findArmoredGloveWithWeapon().isEmpty() && selectedWeaponType != null) {
            try {
                Optional<MiscMounted> glove = battleArmor.getMisc().stream()
                      .filter(m -> m.getType().hasFlag(MiscTypeFlag.F_ARMORED_GLOVE))
                      .findFirst();
                if (glove.isPresent()) {
                    Mounted<?> newWeapon = battleArmor.addEquipment(selectedWeaponType, glove.get().getLocation());
                    BaConstructionUtil.mountOnApm(newWeapon, glove.get());
                }
            } catch (LocationFullException ex) {
                // this is not thrown for BA
            }
        }
    }

    Optional<MiscMounted> findArmoredGloveWithWeapon() {
        return battleArmor.getMisc().stream()
              .filter(m -> m.getType().hasFlag(MiscTypeFlag.F_ARMORED_GLOVE))
              .filter(m -> m.getLinked() != null)
              .findFirst();
    }

    /**
     * Removes the manipulator in the given arm location, if there is one. Also removes an attached AP weapon if there
     * is one. (Remove means delete from the unit entirely)
     *
     * @param mountLoc A BA arm location (MOUNT_LOC_x_ARM)
     */
    private void removeManipulator(int mountLoc) {
        MiscMounted manipulator = battleArmor.getManipulator(mountLoc);
        if (manipulator != null) {
            // save an attached weapon to remove it from the unit
            Mounted<?> apWeapon = manipulator.getLinked();
            if (apWeapon != null) {
                ConstructionUtil.removeMounted(battleArmor, apWeapon);
            }
            ConstructionUtil.removeMounted(battleArmor, manipulator);
        }
    }

    // === JComboBox Renderer to write display names and indicate weight
    private class ManipulatorRenderer extends DefaultListCellRenderer {

        private final JComboBox<BAManipulator> comboBox;

        public ManipulatorRenderer(JComboBox<BAManipulator> comboBox) {
            this.comboBox = comboBox;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {

            String text = "Error";
            if (value instanceof BAManipulator baManipulator) {
                String tonnage = "0 kg";
                String name = "None";
                var type = EquipmentType.get(baManipulator.internalName);
                if (type != null) {
                    name = type.getShortName();
                    if (baManipulator == BAManipulator.CARGO_LIFTER) {
                        tonnage = "variable";
                    } else {
                        tonnage = (int) (type.getTonnage(battleArmor) * 1000) + " kg";
                    }
                }
                if (comboBox.isEnabled()) {
                    text = "%s (%s)".formatted(name, tonnage);
                } else {
                    // when disabled, this item is either the second arm of a pair-mounted (no need to show the
                    // weight twice) or has no modular adaptor, so no need to show its weight
                    text = name;
                }
            }
            return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
        }
    }

    private double totalManipulatorKg() {
        return 1000 * battleArmor.getMisc()
              .stream()
              .filter(m -> m.getType().hasFlag(MiscTypeFlag.F_BA_MANIPULATOR))
              .mapToDouble(Mounted::getTonnage)
              .sum();
    }
}
