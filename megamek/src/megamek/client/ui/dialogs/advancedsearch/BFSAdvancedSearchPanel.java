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
package megamek.client.ui.dialogs.advancedsearch;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.Messages;
import megamek.client.ui.util.IntRangeTextField;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.battlefieldSupport.BFSArtilleryType;
import megamek.common.battlefieldSupport.BFSAssetType;
import megamek.common.battlefieldSupport.BFSDamage;
import megamek.common.battlefieldSupport.BFSRange;
import megamek.common.battlefieldSupport.BFSSpecial;
import megamek.common.battlefieldSupport.BFSSpecialType;
import megamek.common.loaders.MekSummary;
import megamek.common.units.EntityMovementMode;

/**
 * This panel shows advanced search filters for Battlefield Support Asset values. It filters on the asset-specific
 * attributes that do not fit the Total Warfare or Alpha Strike searches (asset type, movement, TMM, range, skill,
 * damage, destroy check, threshold, BSP cost, BFS BV and recognized Specials, including numeric Special thresholds and
 * Artillery type). When any filter is active, only Battlefield Support Assets are matched.
 */
public class BFSAdvancedSearchPanel extends JPanel {

    private static final String BETWEEN = Messages.getString("BFSAdvancedSearchPanel.between");
    private static final String AND = Messages.getString("BFSAdvancedSearchPanel.and");

    /** The movement modes offered as filters, in display order. */
    private static final List<EntityMovementMode> MOVE_MODES = List.of(
          EntityMovementMode.TRACKED, EntityMovementMode.WHEELED, EntityMovementMode.HOVER,
          EntityMovementMode.VTOL, EntityMovementMode.WIGE, EntityMovementMode.INF_LEG,
          EntityMovementMode.INF_JUMP, EntityMovementMode.INF_MOTORIZED, EntityMovementMode.NONE);

    /** The parameter-valued Specials whose numeric value can be filtered with an "at least" threshold. Artillery is
     * excluded because its parameter is a type token rather than a number (see the Artillery Type filter). */
    private static final List<BFSSpecialType> NUMERIC_SPECIALS = Arrays.stream(BFSSpecialType.values())
          .filter(BFSSpecialType::takesValue)
          .filter(type -> type != BFSSpecialType.ARTILLERY)
          .toList();

    private final JCheckBox useAssetType = new JCheckBox(Messages.getString("BFSAdvancedSearchPanel.assetType"));
    private final Map<BFSAssetType, JToggleButton> assetTypeButtons = new EnumMap<>(BFSAssetType.class);

    private final JCheckBox useMoveMode = new JCheckBox(Messages.getString("BFSAdvancedSearchPanel.movementMode"));
    private final Map<EntityMovementMode, JToggleButton> moveModeButtons = new LinkedHashMap<>();

    private final JCheckBox useSpecials = new JCheckBox(Messages.getString("BFSAdvancedSearchPanel.specials"));
    private final Map<BFSSpecialType, JToggleButton> specialButtons = new EnumMap<>(BFSSpecialType.class);

    private final Map<BFSSpecialType, NumericSpecialFilter> numericSpecialFilters =
          new EnumMap<>(BFSSpecialType.class);

    private final JCheckBox useArtilleryType = new JCheckBox(Messages.getString("BFSAdvancedSearchPanel.artilleryType"));
    private final Map<BFSArtilleryType, JToggleButton> artilleryTypeButtons = new EnumMap<>(BFSArtilleryType.class);

    private final RangeFilter mp = new RangeFilter(Messages.getString("BFSAdvancedSearchPanel.mp"));
    private final RangeFilter tmm = new RangeFilter(Messages.getString("BFSAdvancedSearchPanel.tmm"));
    private final RangeFilter skill = new RangeFilter(Messages.getString("BFSAdvancedSearchPanel.skill"));
    private final RangeFilter damage = new RangeFilter(Messages.getString("BFSAdvancedSearchPanel.damageTotal"));
    private final RangeFilter damagePerHit = new RangeFilter(
          Messages.getString("BFSAdvancedSearchPanel.damagePerHit"));
    private final RangeFilter damageHits = new RangeFilter(Messages.getString("BFSAdvancedSearchPanel.damageHits"));
    private final RangeFilter destroyCheck = new RangeFilter(
          Messages.getString("BFSAdvancedSearchPanel.destroyCheck"));
    private final RangeFilter threshold = new RangeFilter(Messages.getString("BFSAdvancedSearchPanel.threshold"));
    private final RangeFilter bsp = new RangeFilter(Messages.getString("BFSAdvancedSearchPanel.bsp"));
    private final RangeFilter bv = new RangeFilter(Messages.getString("BFSAdvancedSearchPanel.bv"));
    private final RangeFilter longRange = new RangeFilter(Messages.getString("BFSAdvancedSearchPanel.longRange"));

    private AdvSearchState.BfsState savedState = new AdvSearchState.BfsState();

    public BFSAdvancedSearchPanel() {
        for (BFSAssetType type : BFSAssetType.values()) {
            assetTypeButtons.put(type, new JToggleButton(assetTypeLabel(type)));
        }
        for (EntityMovementMode mode : MOVE_MODES) {
            moveModeButtons.put(mode, new JToggleButton(moveModeLabel(mode)));
        }
        for (BFSSpecialType special : BFSSpecialType.values()) {
            if (!special.takesValue()) {
                specialButtons.put(special, new JToggleButton(special.canonicalCode()));
            }
        }
        for (BFSSpecialType special : NUMERIC_SPECIALS) {
            numericSpecialFilters.put(special, new NumericSpecialFilter(special));
        }
        for (BFSArtilleryType type : BFSArtilleryType.values()) {
            artilleryTypeButtons.put(type, new JToggleButton(artilleryTypeLabel(type)));
        }

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(toggleSection(useAssetType, assetTypeButtons.values(), 5));
        add(new ASAdvancedSearchPanel.DottedSeparator());
        add(toggleSection(useMoveMode, moveModeButtons.values(), 5));
        add(new ASAdvancedSearchPanel.DottedSeparator());
        add(mp.panel());
        add(tmm.panel());
        add(skill.panel());
        add(new ASAdvancedSearchPanel.DottedSeparator());
        add(damage.panel());
        add(damagePerHit.panel());
        add(damageHits.panel());
        add(longRange.panel());
        add(new ASAdvancedSearchPanel.DottedSeparator());
        add(destroyCheck.panel());
        add(threshold.panel());
        add(new ASAdvancedSearchPanel.DottedSeparator());
        add(bsp.panel());
        add(bv.panel());
        add(new ASAdvancedSearchPanel.DottedSeparator());
        add(toggleSection(useSpecials, specialButtons.values(), 7));
        add(numericSpecialSection());
        add(toggleSection(useArtilleryType, artilleryTypeButtons.values(), 3));
        add(new ASAdvancedSearchPanel.DottedSeparator());

        JButton btnClear = new JButton(Messages.getString("MekSelectorDialog.ClearTab"));
        btnClear.addActionListener(e -> clearValues());
        add(simplePanel(btnClear));

        useAssetType.addActionListener(e -> updateEnabled());
        useMoveMode.addActionListener(e -> updateEnabled());
        useSpecials.addActionListener(e -> updateEnabled());
        useArtilleryType.addActionListener(e -> updateEnabled());
        updateEnabled();
    }

    private static String moveModeLabel(EntityMovementMode mode) {
        return Messages.getString("BFSAdvancedSearchPanel.movement." + mode.name());
    }

    private static String assetTypeLabel(BFSAssetType type) {
        return Messages.getString("BFSAdvancedSearchPanel.assetType." + type.name());
    }

    private static String artilleryTypeLabel(BFSArtilleryType type) {
        return Messages.getString("BFSAdvancedSearchPanel.artillery." + type.name());
    }

    private static String numericSpecialLabel(BFSSpecialType type) {
        return Messages.getString("BFSAdvancedSearchPanel.numericSpecial")
              .formatted(type.canonicalCode(),
                    Messages.getString("BFSAdvancedSearchPanel.special." + type.name()));
    }

    /**
     * @param mekSummary the unit summary to test, which may be {@code null}
     *
     * @return true when no filters are active, or when the given summary is a Battlefield Support Asset that matches
     *       every active filter. When any filter is active, non-asset summaries never match.
     */
    public boolean matches(@Nullable MekSummary mekSummary) {
        if (!isActive()) {
            return true;
        }
        if ((mekSummary == null) || !mekSummary.isBattlefieldSupportAsset()) {
            return false;
        }
        if (useAssetType.isSelected() && !selectedAssetTypes().contains(mekSummary.getBfsAssetType())) {
            return false;
        }
        if (useMoveMode.isSelected() && !selectedMoveModes().contains(mekSummary.getBfsMovementMode())) {
            return false;
        }
        if (useSpecials.isSelected()
              && !new HashSet<>(mekSummary.getBfsSpecials()).containsAll(selectedSpecials())) {
            return false;
        }
        if (!matchesNumericSpecials(mekSummary)) {
            return false;
        }
        if (useArtilleryType.isSelected() && !matchesArtilleryType(mekSummary)) {
            return false;
        }
        if (mp.active() && !mp.matches(mekSummary.getBfsMp())) {
            return false;
        }
        if (tmm.active() && !tmm.matches(mekSummary.getBfsTmm())) {
            return false;
        }
        if (skill.active() && !skill.matches(mekSummary.getBfsSkill())) {
            return false;
        }
        BFSDamage bfsDamage = mekSummary.getBfsDamage();
        if (damage.active() && !damage.matches((bfsDamage != null) ? bfsDamage.total() : 0)) {
            return false;
        }
        if (damagePerHit.active() && !damagePerHit.matches((bfsDamage != null) ? bfsDamage.perHit() : 0)) {
            return false;
        }
        if (damageHits.active() && !damageHits.matches((bfsDamage != null) ? bfsDamage.hits() : 0)) {
            return false;
        }
        if (destroyCheck.active() && !destroyCheck.matches(mekSummary.getBfsDestroyCheck())) {
            return false;
        }
        if (threshold.active() && !threshold.matches(mekSummary.getBfsThreshold())) {
            return false;
        }
        if (bsp.active() && !bsp.matches(mekSummary.getBfsBsp())) {
            return false;
        }
        if (bv.active() && !bv.matches(mekSummary.getBV())) {
            return false;
        }
        if (longRange.active()) {
            BFSRange range = mekSummary.getBfsRange();
            if ((range == null) || range.isKeyword() || !longRange.matches(range.longRange())) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true if the asset satisfies every enabled numeric Special filter
     */
    private boolean matchesNumericSpecials(MekSummary mekSummary) {
        return numericSpecialFilters.values().stream()
              .filter(NumericSpecialFilter::active)
              .allMatch(filter -> filter.matches(mekSummary.getBfsSpecialDetails()));
    }

    /**
     * @return true if the asset has an Artillery Special and, when specific types are selected, its artillery type is
     *       one of them. When no type is selected, any Artillery asset matches.
     */
    private boolean matchesArtilleryType(MekSummary mekSummary) {
        if (!mekSummary.getBfsSpecials().contains(BFSSpecialType.ARTILLERY)) {
            return false;
        }
        List<BFSArtilleryType> selected = selectedArtilleryTypes();
        if (selected.isEmpty()) {
            return true;
        }
        BFSArtilleryType assetType = artilleryTypeOf(mekSummary);
        return (assetType != null) && selected.contains(assetType);
    }

    private static @Nullable BFSArtilleryType artilleryTypeOf(MekSummary mekSummary) {
        return mekSummary.getBfsSpecialDetails().stream()
              .filter(special -> special.knownType().orElse(null) == BFSSpecialType.ARTILLERY)
              .map(BFSSpecial::value)
              .map(BFSArtilleryType::fromString)
              .filter(Objects::nonNull)
              .findFirst()
              .orElse(null);
    }

    /**
     * @return true when any of the search filters is activated so that units might be filtered out.
     */
    public boolean isActive() {
        return useAssetType.isSelected() || useMoveMode.isSelected() || useSpecials.isSelected()
              || numericSpecialFilters.values().stream().anyMatch(NumericSpecialFilter::active)
              || useArtilleryType.isSelected()
              || mp.active() || tmm.active() || skill.active() || damage.active()
              || damagePerHit.active() || damageHits.active()
              || destroyCheck.active() || threshold.active() || bsp.active() || bv.active()
              || longRange.active();
    }

    private List<BFSAssetType> selectedAssetTypes() {
        return selectedKeys(assetTypeButtons);
    }

    private List<EntityMovementMode> selectedMoveModes() {
        return selectedKeys(moveModeButtons);
    }

    private List<BFSSpecialType> selectedSpecials() {
        return selectedKeys(specialButtons);
    }

    private List<BFSArtilleryType> selectedArtilleryTypes() {
        return selectedKeys(artilleryTypeButtons);
    }

    private static <T> List<T> selectedKeys(Map<T, JToggleButton> buttons) {
        List<T> result = new ArrayList<>();
        buttons.forEach((key, button) -> {
            if (button.isSelected()) {
                result.add(key);
            }
        });
        return result;
    }

    private void updateEnabled() {
        boolean assetType = useAssetType.isSelected();
        assetTypeButtons.values().forEach(button -> button.setEnabled(assetType));
        boolean moveMode = useMoveMode.isSelected();
        moveModeButtons.values().forEach(button -> button.setEnabled(moveMode));
        boolean specials = useSpecials.isSelected();
        specialButtons.values().forEach(button -> button.setEnabled(specials));

        mp.setFieldsEnabled(mp.use.isSelected());
        tmm.setFieldsEnabled(tmm.use.isSelected());
        skill.setFieldsEnabled(skill.use.isSelected());
        damage.setFieldsEnabled(damage.use.isSelected());
        damagePerHit.setFieldsEnabled(damagePerHit.use.isSelected());
        damageHits.setFieldsEnabled(damageHits.use.isSelected());
        destroyCheck.setFieldsEnabled(destroyCheck.use.isSelected());
        threshold.setFieldsEnabled(threshold.use.isSelected());
        bsp.setFieldsEnabled(bsp.use.isSelected());
        bv.setFieldsEnabled(bv.use.isSelected());
        longRange.setFieldsEnabled(longRange.use.isSelected());

        numericSpecialFilters.values().forEach(filter -> filter.setFieldsEnabled(filter.active()));
        boolean artilleryType = useArtilleryType.isSelected();
        artilleryTypeButtons.values().forEach(button -> button.setEnabled(artilleryType));
    }

    /** Deactivates all Asset search filters so that no units will be filtered out. */
    public void clearValues() {
        applyState(new AdvSearchState.BfsState());
    }

    /** Saves the current field settings and contents to restore them when user-canceled. */
    public void saveValues() {
        savedState = captureState(false);
    }

    /** Restores the saved field settings and contents. */
    public void resetValues() {
        applyState(savedState);
    }

    AdvSearchState.BfsState getState() {
        return captureState(true);
    }

    /**
     * Captures the current UI into a state object.
     *
     * @param minimal when true, values of inactive filters are omitted (empty) so a saved search stays small; when
     *                false, every value is captured so the state can round-trip exactly (used for cancel-restore)
     *
     * @return the captured state
     */
    private AdvSearchState.BfsState captureState(boolean minimal) {
        var state = new AdvSearchState.BfsState();

        state.assetTypeUse = useAssetType.isSelected();
        if (!minimal || useAssetType.isSelected()) {
            state.assetTypeSelected = selectedAssetTypes();
        }
        state.moveModeUse = useMoveMode.isSelected();
        if (!minimal || useMoveMode.isSelected()) {
            state.moveModeSelected = selectedMoveModes();
        }
        state.specialsUse = useSpecials.isSelected();
        if (!minimal || useSpecials.isSelected()) {
            state.specialsSelected = selectedSpecials();
        }

        state.mpUse = mp.use.isSelected();
        state.tmmUse = tmm.use.isSelected();
        state.skillUse = skill.use.isSelected();
        state.damageUse = damage.use.isSelected();
        state.damagePerHitUse = damagePerHit.use.isSelected();
        state.damageHitsUse = damageHits.use.isSelected();
        state.destroyCheckUse = destroyCheck.use.isSelected();
        state.thresholdUse = threshold.use.isSelected();
        state.bspUse = bsp.use.isSelected();
        state.bvUse = bv.use.isSelected();
        state.longRangeUse = longRange.use.isSelected();
        if (!minimal || mp.use.isSelected()) {
            state.mpFromText = mp.from.getText();
            state.mpToText = mp.to.getText();
        }
        if (!minimal || tmm.use.isSelected()) {
            state.tmmFromText = tmm.from.getText();
            state.tmmToText = tmm.to.getText();
        }
        if (!minimal || skill.use.isSelected()) {
            state.skillFromText = skill.from.getText();
            state.skillToText = skill.to.getText();
        }
        if (!minimal || damage.use.isSelected()) {
            state.damageFromText = damage.from.getText();
            state.damageToText = damage.to.getText();
        }
        if (!minimal || damagePerHit.use.isSelected()) {
            state.damagePerHitFromText = damagePerHit.from.getText();
            state.damagePerHitToText = damagePerHit.to.getText();
        }
        if (!minimal || damageHits.use.isSelected()) {
            state.damageHitsFromText = damageHits.from.getText();
            state.damageHitsToText = damageHits.to.getText();
        }
        if (!minimal || destroyCheck.use.isSelected()) {
            state.destroyCheckFromText = destroyCheck.from.getText();
            state.destroyCheckToText = destroyCheck.to.getText();
        }
        if (!minimal || threshold.use.isSelected()) {
            state.thresholdFromText = threshold.from.getText();
            state.thresholdToText = threshold.to.getText();
        }
        if (!minimal || bsp.use.isSelected()) {
            state.bspFromText = bsp.from.getText();
            state.bspToText = bsp.to.getText();
        }
        if (!minimal || bv.use.isSelected()) {
            state.bvFromText = bv.from.getText();
            state.bvToText = bv.to.getText();
        }
        if (!minimal || longRange.use.isSelected()) {
            state.longRangeFromText = longRange.from.getText();
            state.longRangeToText = longRange.to.getText();
        }

        numericSpecialFilters.forEach((type, filter) -> {
            if (filter.active()) {
                state.numericSpecialSelected.add(type);
            }
            if (!minimal || filter.active()) {
                state.numericSpecialMinTexts.put(type, filter.minimum.getText());
            }
        });
        state.artilleryTypeUse = useArtilleryType.isSelected();
        if (!minimal || useArtilleryType.isSelected()) {
            state.artilleryTypeSelected = selectedArtilleryTypes();
        }
        return state;
    }

    void applyState(AdvSearchState.BfsState state) {
        useAssetType.setSelected(state.assetTypeUse);
        assetTypeButtons.forEach((type, button) -> button.setSelected(state.assetTypeSelected.contains(type)));
        useMoveMode.setSelected(state.moveModeUse);
        moveModeButtons.forEach((mode, button) -> button.setSelected(state.moveModeSelected.contains(mode)));
        useSpecials.setSelected(state.specialsUse);
        specialButtons.forEach((special, button) -> button.setSelected(state.specialsSelected.contains(special)));

        applyRange(mp, state.mpUse, state.mpFromText, state.mpToText);
        applyRange(tmm, state.tmmUse, state.tmmFromText, state.tmmToText);
        applyRange(skill, state.skillUse, state.skillFromText, state.skillToText);
        applyRange(damage, state.damageUse, state.damageFromText, state.damageToText);
        applyRange(damagePerHit, state.damagePerHitUse, state.damagePerHitFromText, state.damagePerHitToText);
        applyRange(damageHits, state.damageHitsUse, state.damageHitsFromText, state.damageHitsToText);
        applyRange(destroyCheck, state.destroyCheckUse, state.destroyCheckFromText, state.destroyCheckToText);
        applyRange(threshold, state.thresholdUse, state.thresholdFromText, state.thresholdToText);
        applyRange(bsp, state.bspUse, state.bspFromText, state.bspToText);
        applyRange(bv, state.bvUse, state.bvFromText, state.bvToText);
        applyRange(longRange, state.longRangeUse, state.longRangeFromText, state.longRangeToText);

        numericSpecialFilters.forEach((type, filter) -> {
            filter.use.setSelected(state.numericSpecialSelected.contains(type));
            filter.minimum.setText(state.numericSpecialMinTexts.getOrDefault(type, ""));
        });
        useArtilleryType.setSelected(state.artilleryTypeUse);
        artilleryTypeButtons.forEach((type, button) ->
              button.setSelected(state.artilleryTypeSelected.contains(type)));
        updateEnabled();
    }

    private static void applyRange(RangeFilter filter, boolean use, String fromText, String toText) {
        filter.use.setSelected(use);
        filter.from.setText(fromText);
        filter.to.setText(toText);
    }

    private JComponent numericSpecialSection() {
        JPanel panel = new SectionPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        numericSpecialFilters.values().forEach(filter -> panel.add(filter.panel()));
        return panel;
    }

    private JComponent toggleSection(JCheckBox checkBox, Collection<JToggleButton> buttons, int perRow) {
        Box buttonPanel = Box.createVerticalBox();
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        int count = 0;
        for (JToggleButton button : buttons) {
            if ((count > 0) && ((count % perRow) == 0)) {
                buttonPanel.add(row);
                row = new JPanel(new FlowLayout(FlowLayout.LEFT));
            }
            row.add(button);
            count++;
        }
        buttonPanel.add(row);

        JPanel panel = new SectionPanel();
        panel.add(checkBox);
        panel.add(Box.createHorizontalStrut(15));
        panel.add(buttonPanel);
        return panel;
    }

    private JComponent simplePanel(JComponent... components) {
        JPanel panel = new SectionPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.add(Box.createHorizontalStrut(15));
        Arrays.stream(components).forEach(panel::add);
        return panel;
    }

    /** A single numeric between/and range filter (checkbox + two integer fields). */
    private class RangeFilter {

        final JCheckBox use;
        final IntRangeTextField from = new IntRangeTextField(5);
        final IntRangeTextField to = new IntRangeTextField(5);
        private final JLabel betweenLabel = new JLabel(BETWEEN);
        private final JLabel andLabel = new JLabel(AND);

        RangeFilter(String label) {
            use = new JCheckBox(label);
            use.addActionListener(e -> updateEnabled());
        }

        JComponent panel() {
            JPanel panel = new SectionPanel();
            panel.setLayout(new FlowLayout(FlowLayout.LEFT));
            panel.add(use);
            panel.add(Box.createHorizontalStrut(15));
            panel.add(betweenLabel);
            panel.add(from);
            panel.add(andLabel);
            panel.add(to);
            return panel;
        }

        boolean active() {
            return use.isSelected();
        }

        boolean matches(int value) {
            return (value >= from.getIntVal(Integer.MIN_VALUE)) && (value <= to.getIntVal(Integer.MAX_VALUE));
        }

        void setFieldsEnabled(boolean enabled) {
            betweenLabel.setEnabled(enabled);
            from.setEnabled(enabled);
            andLabel.setEnabled(enabled);
            to.setEnabled(enabled);
        }
    }

    /** An independently enabled "at least" filter for one parameter-valued Special. */
    private class NumericSpecialFilter {

        final BFSSpecialType type;
        final JCheckBox use;
        final JLabel atLeastLabel = new JLabel(Messages.getString("BFSAdvancedSearchPanel.atLeast"));
        final IntRangeTextField minimum = new IntRangeTextField(5);

        NumericSpecialFilter(BFSSpecialType type) {
            this.type = type;
            use = new JCheckBox(numericSpecialLabel(type));
            use.addActionListener(e -> updateEnabled());
        }

        JComponent panel() {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            panel.add(use);
            panel.add(Box.createHorizontalStrut(15));
            panel.add(atLeastLabel);
            panel.add(minimum);
            return panel;
        }

        boolean active() {
            return use.isSelected();
        }

        boolean matches(List<BFSSpecial> specials) {
            int min = minimum.getIntVal(Integer.MIN_VALUE);
            return specials.stream()
                  .filter(special -> special.knownType().orElse(null) == type)
                  .anyMatch(special -> special.intValue().orElse(Integer.MIN_VALUE) >= min);
        }

        void setFieldsEnabled(boolean enabled) {
            atLeastLabel.setEnabled(enabled);
            minimum.setEnabled(enabled);
        }
    }

    private static class SectionPanel extends UIUtil.FixedYPanel {
        SectionPanel() {
            setLayout(new FlowLayout(FlowLayout.LEFT));
            setBorder(new EmptyBorder(5, 25, 5, 10));
        }
    }
}
