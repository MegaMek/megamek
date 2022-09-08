/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.unitSelector;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.swing.util.IntRangeTextField;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.MechSummary;
import megamek.common.alphaStrike.ASDamage;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.ASUnitType;
import megamek.common.alphaStrike.BattleForceSUA;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This panel shows advanced search filters for AlphaStrike values.
 */
public class ASAdvancedSearchPanel extends JPanel {

    private final static String BETWEEN = "between";
    private final static String AND = "and";

    private UiValues savedUiValues = new UiValues();

    JCheckBox useUnitType = new JCheckBox("Unit Type");
    JToggleButton unitTypeBM = new JToggleButton("BM");
    JToggleButton unitTypeCV = new JToggleButton("CV");
    JToggleButton unitTypeCI = new JToggleButton("CI");
    JToggleButton unitTypeBA = new JToggleButton("BA");
    JToggleButton unitTypeIM = new JToggleButton("IM");
    JToggleButton unitTypePM = new JToggleButton("PM");
    JToggleButton unitTypeSV = new JToggleButton("SV");
    JToggleButton unitTypeAF = new JToggleButton("AF");
    JToggleButton unitTypeCF = new JToggleButton("CF");

    JCheckBox useSize = new JCheckBox("Size");
    JToggleButton size1 = new JToggleButton(" 1 ");
    JToggleButton size2 = new JToggleButton(" 2 ");
    JToggleButton size3 = new JToggleButton(" 3 ");
    JToggleButton size4 = new JToggleButton(" 4 ");
    JToggleButton size5 = new JToggleButton(" 5 ");

    JCheckBox useTMM = new JCheckBox("TMM");
    JToggleButton tmm0 = new JToggleButton(" 0 ");
    JToggleButton tmm1 = new JToggleButton(" 1 ");
    JToggleButton tmm2 = new JToggleButton(" 2 ");
    JToggleButton tmm3 = new JToggleButton(" 3 ");
    JToggleButton tmm4 = new JToggleButton(" 4 ");
    JToggleButton tmm5 = new JToggleButton(" 5 ");

    JCheckBox useArmor = new JCheckBox("Armor");
    JLabel armorBetween = new JLabel(BETWEEN);
    IntRangeTextField armorFrom = new IntRangeTextField();
    JLabel armorAnd = new JLabel(AND);
    IntRangeTextField armorTo = new IntRangeTextField();

    JCheckBox useStructure = new JCheckBox("Structure");
    JLabel structureBetween = new JLabel(BETWEEN);
    IntRangeTextField structureFrom = new IntRangeTextField();
    JLabel structureAnd = new JLabel(AND);
    IntRangeTextField structureTo = new IntRangeTextField();

    JCheckBox useThreshold = new JCheckBox("Threshold");
    JLabel thresholdBetween = new JLabel(BETWEEN);
    IntRangeTextField thresholdFrom = new IntRangeTextField();
    JLabel thresholdAnd = new JLabel(AND);
    IntRangeTextField thresholdTo = new IntRangeTextField();

    JCheckBox useDamageS = new JCheckBox("Damage S");
    JLabel damageSBetween = new JLabel(BETWEEN);
    MMComboBox<ASDamage> damageSFrom = new MMComboBox<>("Damage S From");
    JLabel damageSAnd = new JLabel(AND);
    MMComboBox<ASDamage> damageSTo = new MMComboBox<>("Damage S To");

    JCheckBox useDamageM = new JCheckBox("Damage M");
    JLabel damageMBetween = new JLabel(BETWEEN);
    MMComboBox<ASDamage> damageMFrom = new MMComboBox<>("Damage M From");
    JLabel damageMAnd = new JLabel(AND);
    MMComboBox<ASDamage> damageMTo = new MMComboBox<>("Damage M To");

    JCheckBox useDamageL = new JCheckBox("Damage L");
    JLabel damageLBetween = new JLabel(BETWEEN);
    MMComboBox<ASDamage> damageLFrom = new MMComboBox<>("Damage L From");
    JLabel damageLAnd = new JLabel(AND);
    MMComboBox<ASDamage> damageLTo = new MMComboBox<>("Damage L To");

    JCheckBox useDamageE = new JCheckBox("Damage E");
    JLabel damageEBetween = new JLabel(BETWEEN);
    MMComboBox<ASDamage> damageEFrom = new MMComboBox<>("Damage E From");
    JLabel damageEAnd = new JLabel(AND);
    MMComboBox<ASDamage> damageETo = new MMComboBox<>("Damage E To");

    JCheckBox usePV = new JCheckBox("Point Value");
    JLabel pvBetween = new JLabel(BETWEEN);
    IntRangeTextField pvFrom = new IntRangeTextField();
    JLabel pvAnd = new JLabel(AND);
    IntRangeTextField pvTo = new IntRangeTextField();

    JCheckBox useMV = new JCheckBox("Movement");
    MMComboBox<String> mvMode = new MMComboBox<>("Movement Mode");
    JLabel mvBetween = new JLabel(BETWEEN);
    IntRangeTextField mvFrom = new IntRangeTextField();
    JLabel mvAnd = new JLabel(AND);
    IntRangeTextField mvTo = new IntRangeTextField();

    JCheckBox useAbility1 = new JCheckBox("Special Ability");
    MMComboBox<BattleForceSUA> ability1 = new MMComboBox<>("Ability1", BattleForceSUA.values());

    JCheckBox useAbility2 = new JCheckBox("Special Ability");
    MMComboBox<BattleForceSUA> ability2 = new MMComboBox<>("Ability2", BattleForceSUA.values());

    public ASAdvancedSearchPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(unitTypePanel());
        add(new DottedSeparator());
        add(simplePanel(useSize, size1, size2, size3, size4, size5));
        add(new DottedSeparator());
        add(simplePanel(usePV, pvBetween, pvFrom, pvAnd, pvTo));
        add(new DottedSeparator());
        add(simplePanel(useMV, mvMode, mvBetween, mvFrom, mvAnd, mvTo));
        add(new DottedSeparator());
        add(simplePanel(useTMM, tmm0, tmm1, tmm2, tmm3, tmm4, tmm5));
        add(new DottedSeparator());
        add(simplePanel(useArmor, armorBetween, armorFrom, armorAnd, armorTo));
        add(new DottedSeparator());
        add(simplePanel(useStructure, structureBetween, structureFrom, structureAnd, structureTo));
        add(new DottedSeparator());
        add(simplePanel(useThreshold, thresholdBetween, thresholdFrom, thresholdAnd, thresholdTo));
        add(new DottedSeparator());
        add(simplePanel(useDamageS, damageSBetween, damageSFrom, damageSAnd, damageSTo));
        add(simplePanel(useDamageM, damageMBetween, damageMFrom, damageMAnd, damageMTo));
        add(simplePanel(useDamageL, damageLBetween, damageLFrom, damageLAnd, damageLTo));
        add(simplePanel(useDamageE, damageEBetween, damageEFrom, damageEAnd, damageETo));
        add(new DottedSeparator());
        add(simplePanel(useAbility1, ability1));
        add(simplePanel(useAbility2, ability2));
        initializeCombos();
        updateEnabled();
    }

    /** @return True when the given MechSummary matches the active search filters or true when no filters are active. */
    public boolean matches(MechSummary mechSummary) {
        if (isActive() && mechSummary.getASUnitType() == ASUnitType.UNKNOWN) {
            return false;
        }
        if (useUnitType.isSelected() && !selectedTypes().contains(mechSummary.getASUnitType())) {
            return false;
        }
        if (useSize.isSelected() && !selectedSizes().contains(mechSummary.getSize())) {
            return false;
        }
        if (useTMM.isSelected() && !(selectedTMMs().contains(mechSummary.getTMM()) && mechSummary.usesTMM())) {
            return false;
        }
        if (useArmor.isSelected() && !(mechSummary.getFullArmor() >= armorFrom.getIntVal(-1)
                && mechSummary.getFullArmor() <= armorTo.getIntVal(-1))) {
            return false;
        }
        if (useStructure.isSelected() && !(mechSummary.getFullStructure() >= structureFrom.getIntVal(-1)
                && mechSummary.getFullStructure() <= structureTo.getIntVal(-1))) {
            return false;
        }
        if (useThreshold.isSelected() && !(mechSummary.getThreshold() >= thresholdFrom.getIntVal(-1)
                && mechSummary.getThreshold() <= thresholdTo.getIntVal(-1))) {
            return false;
        }
        ASDamageVector stdDamage = mechSummary.getStandardDamage();
        if (useDamageS.isSelected() && (damageSFrom.getSelectedItem() != null) && (damageSTo.getSelectedItem() != null)
                && !(stdDamage.S.asDoubleValue() >= damageSFrom.getSelectedItem().asDoubleValue()
                        && stdDamage.S.asDoubleValue() <= damageSTo.getSelectedItem().asDoubleValue())) {
            return false;
        }
        if (useDamageM.isSelected() && (damageMFrom.getSelectedItem() != null) && (damageMTo.getSelectedItem() != null)
                && !(stdDamage.M.asDoubleValue() >= damageMFrom.getSelectedItem().asDoubleValue()
                        && stdDamage.M.asDoubleValue() <= damageMTo.getSelectedItem().asDoubleValue())) {
            return false;
        }
        if (useDamageL.isSelected() && (damageLFrom.getSelectedItem() != null) && (damageLTo.getSelectedItem() != null)
                && !(stdDamage.L.asDoubleValue() >= damageLFrom.getSelectedItem().asDoubleValue()
                        && stdDamage.L.asDoubleValue() <= damageLTo.getSelectedItem().asDoubleValue())) {
            return false;
        }
        if (useDamageE.isSelected() && (damageEFrom.getSelectedItem() != null) && (damageETo.getSelectedItem() != null)
                && !(stdDamage.E.asDoubleValue() >= damageEFrom.getSelectedItem().asDoubleValue()
                        && stdDamage.E.asDoubleValue() <= damageETo.getSelectedItem().asDoubleValue())) {
            return false;
        }
        if (usePV.isSelected() && !(mechSummary.getPointValue() >= pvFrom.getIntVal(-1)
                && mechSummary.getPointValue() <= pvTo.getIntVal(-1))) {
            return false;
        }
        String moveMode = mvMode.getSelectedItem();
        if (useMV.isSelected() && !(mechSummary.hasMovementMode(moveMode)
                && mechSummary.getMovement().get(moveMode) >= mvFrom.getIntVal(-1)
                && mechSummary.getMovement().get(moveMode) <= mvTo.getIntVal(-1))) {
            return false;
        }
        if (useAbility1.isSelected() &&
                !mechSummary.getSpecialAbilities().hasSUA(ability1.getSelectedItem())) {
            return false;
        }
        if (useAbility2.isSelected() &&
                !mechSummary.getSpecialAbilities().hasSUA(ability2.getSelectedItem())) {
            return false;
        }
        return true;
    }

    private JComponent unitTypePanel() {
        useUnitType.addActionListener(e -> updateEnabled());
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topRow.add(unitTypeBM);
        topRow.add(unitTypeIM);
        topRow.add(unitTypePM);
        topRow.add(unitTypeCV);
        topRow.add(unitTypeCI);
        topRow.add(unitTypeBA);

        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomRow.add(unitTypeAF);
        bottomRow.add(unitTypeCF);
        bottomRow.add(unitTypeSV);

        Box buttonPanel = Box.createVerticalBox();
        buttonPanel.add(topRow);
        buttonPanel.add(bottomRow);

        JPanel panel = new SectionPanel();
        panel.add(useUnitType);
        panel.add(Box.createHorizontalStrut(15));
        panel.add(buttonPanel);
        return panel;
    }

    private JComponent simplePanel(JCheckBox checkBox, JComponent... otherComponents) {
        checkBox.addActionListener(e -> updateEnabled());
        JPanel panel = new SectionPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.add(checkBox);
        panel.add(Box.createHorizontalStrut(15));
        Arrays.stream(otherComponents).forEach(panel::add);
        return panel;
    }

    private void initializeCombos() {
        initDamageCombo(damageSFrom);
        initDamageCombo(damageSTo);
        initDamageCombo(damageMFrom);
        initDamageCombo(damageMTo);
        initDamageCombo(damageLFrom);
        initDamageCombo(damageLTo);
        initDamageCombo(damageEFrom);
        initDamageCombo(damageETo);

        mvMode.addItem("");
        mvMode.addItem("j");
        mvMode.addItem("qw");
        mvMode.addItem("qt");
        mvMode.addItem("t");
        mvMode.addItem("w");
        mvMode.addItem("h");
        mvMode.addItem("v");
        mvMode.addItem("n");
        mvMode.addItem("s");
        mvMode.addItem("f");
        mvMode.addItem("m");
        mvMode.addItem("g");
        mvMode.addItem("a");
        mvMode.addItem("p");
    }

    private void initDamageCombo(MMComboBox<ASDamage> comboBox) {
        comboBox.addItem(ASDamage.ZERO);
        comboBox.addItem(ASDamage.MINIMAL);
        for (int i = 1; i <= 20; i++) {
            comboBox.addItem(new ASDamage(i, false));
        }
    }

    /** @return The currently selected unit types as a List of ASUnitTypes. */
    private List<ASUnitType> selectedTypes() {
        List<ASUnitType> result = new ArrayList<>();
        addIfSelected(unitTypeBM.isSelected(), result, ASUnitType.BM);
        addIfSelected(unitTypeIM.isSelected(), result, ASUnitType.IM);
        addIfSelected(unitTypePM.isSelected(), result, ASUnitType.PM);
        addIfSelected(unitTypeCV.isSelected(), result, ASUnitType.CV);
        addIfSelected(unitTypeSV.isSelected(), result, ASUnitType.SV);
        addIfSelected(unitTypeCI.isSelected(), result, ASUnitType.CI);
        addIfSelected(unitTypeBA.isSelected(), result, ASUnitType.BA);
        addIfSelected(unitTypeAF.isSelected(), result, ASUnitType.AF);
        addIfSelected(unitTypeCF.isSelected(), result, ASUnitType.CF);
        return result;
    }

    /** @return The currently selected unit sizes as a List of Integers. */
    private List<Integer> selectedSizes() {
        List<Integer> result = new ArrayList<>();
        addIfSelected(size1.isSelected(), result, 1);
        addIfSelected(size2.isSelected(), result, 2);
        addIfSelected(size3.isSelected(), result, 3);
        addIfSelected(size4.isSelected(), result, 4);
        addIfSelected(size5.isSelected(), result, 5);
        return result;
    }

    /** @return The currently selected TMMs as a List of Integer. */
    private List<Integer> selectedTMMs() {
        List<Integer> result = new ArrayList<>();
        addIfSelected(tmm0.isSelected(), result, 0);
        addIfSelected(tmm1.isSelected(), result, 1);
        addIfSelected(tmm2.isSelected(), result, 2);
        addIfSelected(tmm3.isSelected(), result, 3);
        addIfSelected(tmm4.isSelected(), result, 4);
        addIfSelected(tmm5.isSelected(), result, 5);
        return result;
    }

    /** Updates the enabled status of fields based on the checkboxes. */
    private void updateEnabled() {
        unitTypeBM.setEnabled(useUnitType.isSelected());
        unitTypeCV.setEnabled(useUnitType.isSelected());
        unitTypeCI.setEnabled(useUnitType.isSelected());
        unitTypeBA.setEnabled(useUnitType.isSelected());
        unitTypeIM.setEnabled(useUnitType.isSelected());
        unitTypePM.setEnabled(useUnitType.isSelected());
        unitTypeSV.setEnabled(useUnitType.isSelected());
        unitTypeAF.setEnabled(useUnitType.isSelected());
        unitTypeCF.setEnabled(useUnitType.isSelected());

        size1.setEnabled(useSize.isSelected());
        size2.setEnabled(useSize.isSelected());
        size3.setEnabled(useSize.isSelected());
        size4.setEnabled(useSize.isSelected());
        size5.setEnabled(useSize.isSelected());

        tmm0.setEnabled(useTMM.isSelected());
        tmm1.setEnabled(useTMM.isSelected());
        tmm2.setEnabled(useTMM.isSelected());
        tmm3.setEnabled(useTMM.isSelected());
        tmm4.setEnabled(useTMM.isSelected());
        tmm5.setEnabled(useTMM.isSelected());

        armorBetween.setEnabled(useArmor.isSelected());
        armorFrom.setEnabled(useArmor.isSelected());
        armorAnd.setEnabled(useArmor.isSelected());
        armorTo.setEnabled(useArmor.isSelected());

        structureBetween.setEnabled(useStructure.isSelected());
        structureFrom.setEnabled(useStructure.isSelected());
        structureAnd.setEnabled(useStructure.isSelected());
        structureTo.setEnabled(useStructure.isSelected());

        thresholdBetween.setEnabled(useThreshold.isSelected());
        thresholdFrom.setEnabled(useThreshold.isSelected());
        thresholdAnd.setEnabled(useThreshold.isSelected());
        thresholdTo.setEnabled(useThreshold.isSelected());

        damageSBetween.setEnabled(useDamageS.isSelected());
        damageSFrom.setEnabled(useDamageS.isSelected());
        damageSAnd.setEnabled(useDamageS.isSelected());
        damageSTo.setEnabled(useDamageS.isSelected());

        damageMBetween.setEnabled(useDamageM.isSelected());
        damageMFrom.setEnabled(useDamageM.isSelected());
        damageMAnd.setEnabled(useDamageM.isSelected());
        damageMTo.setEnabled(useDamageM.isSelected());

        damageLBetween.setEnabled(useDamageL.isSelected());
        damageLFrom.setEnabled(useDamageL.isSelected());
        damageLAnd.setEnabled(useDamageL.isSelected());
        damageLTo.setEnabled(useDamageL.isSelected());

        damageEBetween.setEnabled(useDamageE.isSelected());
        damageEFrom.setEnabled(useDamageE.isSelected());
        damageEAnd.setEnabled(useDamageE.isSelected());
        damageETo.setEnabled(useDamageE.isSelected());

        pvBetween.setEnabled(usePV.isSelected());
        pvFrom.setEnabled(usePV.isSelected());
        pvAnd.setEnabled(usePV.isSelected());
        pvTo.setEnabled(usePV.isSelected());

        mvBetween.setEnabled(useMV.isSelected());
        mvFrom.setEnabled(useMV.isSelected());
        mvAnd.setEnabled(useMV.isSelected());
        mvTo.setEnabled(useMV.isSelected());

        ability1.setEnabled(useAbility1.isSelected());
        ability2.setEnabled(useAbility2.isSelected());
    }

    private <T> void addIfSelected(boolean selected, List<T> list, T item) {
        if (selected) {
            list.add(item);
        }
    }

    /** Deactivates all AS search filters so that no units will be filtered out. */
    public void clearValues() {
        useUnitType.setSelected(false);
        useSize.setSelected(false);
        useTMM.setSelected(false);
        useArmor.setSelected(false);
        useStructure.setSelected(false);
        useThreshold.setSelected(false);
        useDamageS.setSelected(false);
        useDamageM.setSelected(false);
        useDamageL.setSelected(false);
        useDamageE.setSelected(false);
        usePV.setSelected(false);
        useMV.setSelected(false);
        useAbility1.setSelected(false);
        useAbility2.setSelected(false);
    }

    /** Saves the current field settings and contents to restore them when user-canceled. */
    public void saveValues() {
        savedUiValues = new UiValues();
        savedUiValues.store();
    }

    /** Restore the saved field settings and contents. */
    public void resetValues() {
        savedUiValues.set();
        updateEnabled();
    }

    /** @return True when any of the search filters is activated so that units might be filtered out. */
    public boolean isActive() {
        return useUnitType.isSelected() || useSize.isSelected() || useTMM.isSelected() || useArmor.isSelected()
                || useStructure.isSelected() || useThreshold.isSelected() || useDamageS.isSelected() || useDamageM.isSelected()
                || useDamageL.isSelected() || useDamageE.isSelected() || usePV.isSelected() || useMV.isSelected()
                || useAbility1.isSelected() || useAbility2.isSelected();
    }

    private static class SectionPanel extends UIUtil.FixedYPanel {
        SectionPanel() {
            setLayout(new FlowLayout(FlowLayout.LEFT));
            setBorder(new EmptyBorder(5, 25, 5, 10));
        }
    }

    private static class DottedSeparator extends JSeparator {

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(super.getMaximumSize().width, 1);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        0, new float[]{9}, 0);
                g2.setStroke(dashed);
                g2.drawLine(5, 0, getWidth() * 9 / 10, 0);
            } finally {
                g2.dispose();
            }
        }
    }

    /** A helper class to save the current field settings and contents. */
    private class UiValues {

        boolean unitTypeUse;
        List<ASUnitType> unitTypeSelected = new ArrayList<>();

        boolean sizeUse;
        List<Integer> sizeSelected = new ArrayList<>();

        boolean tmmUse;
        List<Integer> tmmSelected = new ArrayList<>();

        boolean armorUse;
        String armorFromText = "";
        String armorToText = "";

        boolean structureUse;
        String structureFromText = "";
        String structureToText = "";

        boolean thresholdUse;
        String thresholdFromText = "";
        String thresholdToText = "";

        boolean damageSUse;
        ASDamage damageSFromValue = ASDamage.ZERO;
        ASDamage damageSToValue = ASDamage.ZERO;

        boolean damageMUse;
        ASDamage damageMFromValue = ASDamage.ZERO;
        ASDamage damageMToValue = ASDamage.ZERO;

        boolean damageLUse;
        ASDamage damageLFromValue = ASDamage.ZERO;
        ASDamage damageLToValue = ASDamage.ZERO;

        boolean damageEUse;
        ASDamage damageEFromValue = ASDamage.ZERO;
        ASDamage damageEToValue = ASDamage.ZERO;

        boolean pvUse;
        String pvFromText = "";
        String pvToText = "";

        boolean mvUse;
        String mvModeText = "";
        String mvFromText = "";
        String mvToText = "";

        boolean ability1Use;
        BattleForceSUA ability1Value = BattleForceSUA.UNKNOWN;

        boolean ability2Use;
        BattleForceSUA ability2Value = BattleForceSUA.UNKNOWN;

        void store() {
            unitTypeUse = useUnitType.isSelected();
            unitTypeSelected = selectedTypes();

            sizeUse = useSize.isSelected();
            sizeSelected = selectedSizes();

            tmmUse = useTMM.isSelected();
            tmmSelected = selectedTMMs();

            armorUse = useArmor.isSelected();
            armorFromText = armorFrom.getText();
            armorToText = armorTo.getText();

            structureUse = useStructure.isSelected();
            structureFromText = structureFrom.getText();
            structureToText = structureTo.getText();

            thresholdUse = useThreshold.isSelected();
            thresholdFromText = thresholdFrom.getText();
            thresholdToText = thresholdTo.getText();

            damageSUse = useDamageS.isSelected();
            damageSFromValue = damageSFrom.getSelectedItem();
            damageSToValue = damageSTo.getSelectedItem();

            damageMUse = useDamageM.isSelected();
            damageMFromValue = damageMFrom.getSelectedItem();
            damageMToValue = damageMTo.getSelectedItem();

            damageLUse = useDamageL.isSelected();
            damageLFromValue = damageLFrom.getSelectedItem();
            damageLToValue = damageLTo.getSelectedItem();

            damageEUse = useDamageE.isSelected();
            damageEFromValue = damageEFrom.getSelectedItem();
            damageEToValue = damageETo.getSelectedItem();

            pvUse = usePV.isSelected();
            pvFromText = pvFrom.getText();
            pvToText = pvTo.getText();

            mvUse = useMV.isSelected();
            mvModeText = mvMode.getSelectedItem();
            mvFromText = mvFrom.getText();
            mvToText = mvTo.getText();

            ability1Use = useAbility1.isSelected();
            ability1Value = ability1.getSelectedItem();

            ability2Use = useAbility2.isSelected();
            ability2Value = ability2.getSelectedItem();
        }

        void set() {
            useUnitType.setSelected(unitTypeUse);
            unitTypeBM.setSelected(unitTypeSelected.contains(ASUnitType.BM));
            unitTypeCV.setSelected(unitTypeSelected.contains(ASUnitType.CV));
            unitTypeCI.setSelected(unitTypeSelected.contains(ASUnitType.CI));
            unitTypeBA.setSelected(unitTypeSelected.contains(ASUnitType.BA));
            unitTypeIM.setSelected(unitTypeSelected.contains(ASUnitType.IM));
            unitTypePM.setSelected(unitTypeSelected.contains(ASUnitType.PM));
            unitTypeSV.setSelected(unitTypeSelected.contains(ASUnitType.SV));
            unitTypeAF.setSelected(unitTypeSelected.contains(ASUnitType.AF));

            useSize.setSelected(sizeUse);
            size1.setSelected(sizeSelected.contains(1));
            size2.setSelected(sizeSelected.contains(2));
            size3.setSelected(sizeSelected.contains(3));
            size4.setSelected(sizeSelected.contains(4));
            size5.setSelected(sizeSelected.contains(5));

            useTMM.setSelected(tmmUse);
            tmm0.setSelected(tmmSelected.contains(0));
            tmm1.setSelected(tmmSelected.contains(1));
            tmm2.setSelected(tmmSelected.contains(2));
            tmm3.setSelected(tmmSelected.contains(3));
            tmm4.setSelected(tmmSelected.contains(4));
            tmm5.setSelected(tmmSelected.contains(5));

            useArmor.setSelected(armorUse);
            armorFrom.setText(armorFromText);
            armorTo.setText(armorToText);

            useStructure.setSelected(structureUse);
            structureFrom.setText(structureFromText);
            structureTo.setText(structureToText);

            useThreshold.setSelected(thresholdUse);
            thresholdFrom.setText(thresholdFromText);
            thresholdTo.setText(thresholdToText);

            useDamageS.setSelected(damageSUse);
            damageSFrom.setSelectedItem(damageSFromValue);
            damageSTo.setSelectedItem(damageSToValue);

            useDamageM.setSelected(damageMUse);
            damageMFrom.setSelectedItem(damageMFromValue);
            damageMTo.setSelectedItem(damageMToValue);

            useDamageL.setSelected(damageSUse);
            damageLFrom.setSelectedItem(damageLFromValue);
            damageLTo.setSelectedItem(damageLToValue);

            useDamageE.setSelected(damageEUse);
            damageEFrom.setSelectedItem(damageEFromValue);
            damageETo.setSelectedItem(damageEToValue);

            usePV.setSelected(pvUse);
            pvFrom.setText(pvFromText);
            pvTo.setText(pvToText);

            useMV.setSelected(mvUse);
            mvMode.setSelectedItem(mvModeText);
            mvFrom.setText(mvFromText);
            mvTo.setText(mvToText);

            useAbility1.setSelected(ability1Use);
            ability1.setSelectedItem(ability1Value);

            useAbility2.setSelected(ability2Use);
            ability2.setSelectedItem(ability2Value);
        }
    }
}