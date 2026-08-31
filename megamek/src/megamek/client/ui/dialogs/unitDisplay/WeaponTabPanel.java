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
package megamek.client.ui.dialogs.unitDisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionListener;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.tooltip.UnitToolTip;
import megamek.client.ui.util.UIUtil;
import megamek.common.TargetRollModifier;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;

/**
 * The Weapon tab of the control layout. It keeps the classic panel's weapon list, ammo and bay choosers and range
 * and damage table as its engine, and puts around them what the classic tab only hinted at:
 * <ul>
 *     <li>a heat gauge coloured by the heat the unit will reach, with the effects of that heat as its tooltip;</li>
 *     <li>a range ribbon that shows which bracket of the selected weapon the target sits in;</li>
 *     <li>the to-hit number with every modifier on its own line;</li>
 *     <li>the target's details;</li>
 *     <li>the attacks declared so far this phase.</li>
 * </ul>
 * The combat phase displays drive it through {@link WeaponTabView}, exactly as they drive the classic panel.
 */
public class WeaponTabPanel extends JPanel implements WeaponTabView {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    /** No target, or none in range: nothing on the ribbon is lit. */
    static final int NO_RANGE = -1;

    private final UnitDisplayPanel unitDisplayPanel;
    private final Client client;
    private final WeaponPanel engine;
    private final HeatGauge heatGauge = new HeatGauge();
    private final RangeRibbon rangeRibbon = new RangeRibbon();
    private final ToHitBreakdown toHitBreakdown = new ToHitBreakdown();
    private final JLabel targetExtraInfo = new JLabel();
    private final JEditorPane targetPane = new JEditorPane();
    private final DefaultListModel<String> declaredModel = new DefaultListModel<>();
    private final JLabel declaredHeading = new JLabel();
    private Targetable target;

    WeaponTabPanel(UnitDisplayPanel unitDisplayPanel, @Nullable Client client) {
        super(new BorderLayout());
        this.unitDisplayPanel = unitDisplayPanel;
        this.client = client;
        engine = new WeaponPanel(unitDisplayPanel, client, true);

        targetPane.setContentType("text/html");
        targetPane.setEditable(false);
        targetPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        targetPane.setFont(UIManager.getFont("Label.font"));
        targetPane.setForeground(UIManager.getColor("Label.foreground"));
        JList<String> declaredList = new JList<>(declaredModel);
        declaredList.setVisibleRowCount(3);

        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.PAGE_AXIS));
        column.add(section(Messages.getString("UnitDisplay.weaponTab.heat"), heatGauge));
        engine.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(engine);
        column.add(section(Messages.getString("UnitDisplay.weaponTab.range"), rangeRibbon));
        column.add(section(Messages.getString("UnitDisplay.weaponTab.toHit"), toHitBreakdown));
        JPanel targetBox = new JPanel(new BorderLayout());
        targetBox.add(targetExtraInfo, BorderLayout.PAGE_START);
        targetBox.add(targetPane, BorderLayout.CENTER);
        column.add(section(Messages.getString("UnitDisplay.weaponTab.target"), targetBox));
        column.add(section(declaredHeading, declaredList));
        add(column, BorderLayout.CENTER);

        engine.addWeaponSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                rangeRibbon.setWeapon(engine.getSelectedWeapon(), engine.getSelectedAmmo().orElse(null));
            }
        });
        setDeclaredAttacks(List.of());
        clearToHit();
        setTarget(null, null);
    }

    private static JPanel section(String title, JComponent body) {
        JLabel heading = new JLabel(title);
        return section(heading, body);
    }

    private static JPanel section(JLabel heading, JComponent body) {
        heading.setFont(heading.getFont().deriveFont(Font.BOLD));
        int inset = UIUtil.scaleForGUI(4);
        JPanel panel = new JPanel(new BorderLayout(0, inset));
        panel.setBorder(BorderFactory.createEmptyBorder(inset, inset * 2, inset, inset * 2));
        panel.add(heading, BorderLayout.PAGE_START);
        panel.add(body, BorderLayout.CENTER);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private @Nullable Game game() {
        return (client == null) ? null : client.getGame();
    }

    // ---- what the tab shows on its own ----

    @Override
    public void displayMek(Entity entity) {
        engine.displayMek(entity);
        Game game = game();
        HeatForecast.Result heat = HeatForecast.forecast(entity, game);
        boolean tacOpsHeat = (game != null)
              && game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HEAT);
        boolean hasTSM = (entity instanceof Mek mek) && mek.hasTSM(false);
        String tooltip = entity.tracksHeat()
              ? HeatEffects.getHeatEffects(heat.buildup(), tacOpsHeat, hasTSM)
              : null;
        heatGauge.setHeat(heat, heatText(heat, game), tooltip);
        heatGauge.setVisible(entity.tracksHeat());
        rangeRibbon.setWeapon(engine.getSelectedWeapon(), engine.getSelectedAmmo().orElse(null));
    }

    /** The classic panel's heat line: buildup, capacity, how far over, and what is raising or lowering it. */
    private static String heatText(HeatForecast.Result heat, @Nullable Game game) {
        StringBuilder text = new StringBuilder();
        text.append(heat.buildup());
        if (heat.overCapacity() > 0) {
            text.append('*');
        }
        text.append(" (").append(heat.capacityText()).append(')');
        if (heat.overCapacity() > 0) {
            text.append(' ').append(heat.overCapacity()).append(' ').append(Messages.getString("MekDisplay.over"));
        }
        if (heat.combatComputer()) {
            text.append(' ').append(Messages.getString("UnitDisplay.weaponTab.combatComputer"));
        }
        if (heat.extremeTemperature() && (game != null)) {
            text.append(' ').append(game.getPlanetaryConditions().getTemperatureIndicator());
        }
        return text.toString();
    }

    @Override
    public void setTarget(@Nullable Targetable target, @Nullable String extraInfo) {
        this.target = target;
        targetExtraInfo.setText((extraInfo == null) ? "" : UnitToolTip.wrapWithHTML(extraInfo));
        targetExtraInfo.setVisible((extraInfo != null) && !extraInfo.isEmpty());
        String detail = (target == null)
              ? Messages.getString("MekDisplay.NoTarget")
              : UnitToolTip.getTargetTipDetail(target, client);
        targetPane.setText(UnitToolTip.wrapWithHTML(detail));
        targetPane.setCaretPosition(0);
    }

    @Override
    public void setToHit(ToHitData toHit, boolean naturalAptitudeGunnery) {
        toHitBreakdown.show(toHit, naturalAptitudeGunnery);
    }

    @Override
    public void setToHit(ToHitData toHit) {
        setToHit(toHit, false);
    }

    @Override
    public void setToHit(String message) {
        toHitBreakdown.show(message);
    }

    @Override
    public void clearToHit() {
        toHitBreakdown.clear();
    }

    @Override
    public void setRange(int effectiveDistance) {
        rangeRibbon.setWeapon(engine.getSelectedWeapon(), engine.getSelectedAmmo().orElse(null));
        rangeRibbon.setRange(effectiveDistance);
    }

    @Override
    public void setRangeText(String rangeText) {
        rangeRibbon.setRangeText(rangeText);
    }

    @Override
    public void clearRange() {
        rangeRibbon.setRange(NO_RANGE);
    }

    @Override
    public void setDeclaredAttacks(List<WeaponAttackAction> declaredAttacks) {
        declaredModel.clear();
        Game game = game();
        if (game != null) {
            for (WeaponAttackAction attack : declaredAttacks) {
                declaredModel.addElement(describe(attack, game));
            }
        }
        declaredHeading.setText(Messages.getFormattedString("UnitDisplay.weaponTab.declared", declaredModel.size()));
    }

    /** One line per declared shot: the weapon and what it is aimed at. */
    static String describe(WeaponAttackAction attack, Game game) {
        Entity attacker = game.getEntity(attack.getEntityId());
        Mounted<?> weapon = (attacker == null) ? null : attacker.getEquipment(attack.getWeaponId());
        Targetable aimedAt = game.getTarget(attack.getTargetType(), attack.getTargetId());
        String weaponName = (weapon == null)
              ? Messages.getString("UnitDisplay.weaponTab.unknownWeapon")
              : weapon.getName();
        String targetName = (aimedAt == null)
              ? Messages.getString("UnitDisplay.weaponTab.unknownTarget")
              : aimedAt.getDisplayName();
        return Messages.getFormattedString("UnitDisplay.weaponTab.declaredAttack", weaponName, targetName);
    }

    @Override
    public @Nullable Targetable getPrevTarget() {
        return engine.getPrevTarget();
    }

    @Override
    public void setPrevTarget(@Nullable Targetable prevTarget) {
        engine.setPrevTarget(prevTarget);
    }

    /**
     * @return the target shown, or {@code null} for none
     */
    public @Nullable Targetable getTarget() {
        return target;
    }

    // ---- the weapon list, ammo and bay choosers are the engine's ----

    @Override
    public void updateForEntity(@Nullable Entity entity) {
        if (entity == null) {
            return;
        }
        int weaponNum = getSelectedWeaponNum();
        displayMek(entity);
        selectWeapon(weaponNum);
    }

    @Override
    public int getSelectedEntityId() {
        return engine.getSelectedEntityId();
    }

    @Override
    public @Nullable WeaponMounted getSelectedWeapon() {
        return engine.getSelectedWeapon();
    }

    @Override
    public int getSelectedWeaponNum() {
        return engine.getSelectedWeaponNum();
    }

    @Override
    public Optional<AmmoMounted> getSelectedAmmo() {
        return engine.getSelectedAmmo();
    }

    @Override
    public void selectWeapon(int weaponNumber) {
        engine.selectWeapon(weaponNumber);
    }

    @Override
    public void selectWeapon(@Nullable WeaponMounted weapon) {
        engine.selectWeapon(weapon);
    }

    @Override
    public void selectFirstWeapon() {
        engine.selectFirstWeapon();
    }

    @Override
    public int selectNextWeapon() {
        return engine.selectNextWeapon();
    }

    @Override
    public int selectPrevWeapon() {
        return engine.selectPrevWeapon();
    }

    @Override
    public int getNextWeaponNum() {
        return engine.getNextWeaponNum();
    }

    @Override
    public @Nullable WeaponMounted getNextWeapon() {
        return engine.getNextWeapon();
    }

    @Override
    public void addWeaponSelectionListener(ListSelectionListener listener) {
        engine.addWeaponSelectionListener(listener);
    }

    @Override
    public void removeWeaponSelectionListener(ListSelectionListener listener) {
        engine.removeWeaponSelectionListener(listener);
    }

    @Override
    public boolean isWeaponSelectionSource(@Nullable Object eventSource) {
        return engine.isWeaponSelectionSource(eventSource);
    }

    HeatGauge getHeatGauge() {
        return heatGauge;
    }

    RangeRibbon getRangeRibbon() {
        return rangeRibbon;
    }

    ToHitBreakdown getToHitBreakdown() {
        return toHitBreakdown;
    }

    List<String> getDeclaredAttackLines() {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < declaredModel.size(); i++) {
            lines.add(declaredModel.get(i));
        }
        return lines;
    }

    /**
     * A bar for the heat the unit will reach this turn, filled to the capacity mark and coloured by that heat. The
     * colour comes from the heat itself, as on a record sheet's heat scale - not from how far over capacity it is.
     */
    static final class HeatGauge extends JComponent {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final int BAR_HEIGHT = 18;
        private static final int MIN_WIDTH = 120;

        private int buildup;
        private int capacity;
        private Color fill = Color.WHITE;
        private String text = "";

        HeatGauge() {
            setOpaque(false);
            setFont(UIManager.getFont("Label.font"));
        }

        void setHeat(HeatForecast.Result heat, String text, @Nullable String tooltip) {
            buildup = Math.max(0, heat.buildup());
            capacity = Math.max(0, heat.capacity());
            fill = GUIP.getColorForHeat(buildup, UIManager.getColor("Label.foreground"));
            this.text = text;
            setToolTipText(tooltip);
            revalidate();
            repaint();
        }

        int getBuildup() {
            return buildup;
        }

        int getCapacity() {
            return capacity;
        }

        Color getFill() {
            return fill;
        }

        String getText() {
            return text;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(UIUtil.scaleForGUI(MIN_WIDTH), UIUtil.scaleForGUI(BAR_HEIGHT));
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, UIUtil.scaleForGUI(BAR_HEIGHT));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                int width = getWidth();
                int height = getHeight();
                // the bar runs to the capacity mark; heat past it runs into the overheat colour
                int scaleMax = Math.max(capacity, buildup);
                if (scaleMax > 0) {
                    int filled = (int) Math.round(width * (Math.min(buildup, capacity) / (double) scaleMax));
                    g2.setColor(fill);
                    g2.fillRect(0, 0, filled, height);
                    if (buildup > capacity) {
                        int over = (int) Math.round(width * (buildup / (double) scaleMax)) - filled;
                        g2.setColor(GUIP.getUnitDisplayHeatLevelOverheat());
                        g2.fillRect(filled, 0, over, height);
                    }
                }
                g2.setColor(UIManager.getColor("Label.foreground"));
                g2.drawRect(0, 0, width - 1, height - 1);
                g2.setFont(getFont());
                int baseline = (height + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
                g2.drawString(text, UIUtil.scaleForGUI(4), baseline);
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * The selected weapon's range brackets in a row, with the one the target sits in lit. Ranges follow the ammo
     * loaded, as the classic table does.
     */
    static final class RangeRibbon extends JPanel {

        @Serial
        private static final long serialVersionUID = 1L;

        /** Bracket order as {@link megamek.common.equipment.WeaponType#getRanges(Mounted, Mounted)} returns them. */
        private static final String[] BRACKET_KEYS = {"minimum", "short", "medium", "long", "extreme"};
        private static final int MINIMUM = 0;

        private final JLabel summary = new JLabel();
        private final JLabel[] cells = new JLabel[BRACKET_KEYS.length];
        private int[] ranges = new int[BRACKET_KEYS.length];
        private int range = NO_RANGE;
        private boolean hasWeapon = false;

        RangeRibbon() {
            super(new BorderLayout(0, UIUtil.scaleForGUI(2)));
            JPanel row = new JPanel(new GridLayout(1, BRACKET_KEYS.length, UIUtil.scaleForGUI(2), 0));
            for (int i = 0; i < BRACKET_KEYS.length; i++) {
                cells[i] = new JLabel("", SwingConstants.CENTER);
                cells[i].setOpaque(true);
                cells[i].setBorder(BorderFactory.createLineBorder(UIManager.getColor("Label.foreground")));
                row.add(cells[i]);
            }
            add(summary, BorderLayout.PAGE_START);
            add(row, BorderLayout.CENTER);
            setWeapon(null, null);
        }

        void setWeapon(@Nullable WeaponMounted weapon, @Nullable AmmoMounted ammo) {
            hasWeapon = weapon != null;
            ranges = hasWeapon ? weapon.getType().getRanges(weapon, ammo) : new int[BRACKET_KEYS.length];
            if (ranges.length < BRACKET_KEYS.length) {
                int[] padded = new int[BRACKET_KEYS.length];
                System.arraycopy(ranges, 0, padded, 0, ranges.length);
                ranges = padded;
            }
            relabel();
        }

        void setRange(int effectiveDistance) {
            range = effectiveDistance;
            relabel();
        }

        void setRangeText(String rangeText) {
            range = NO_RANGE;
            relabel();
            summary.setText(rangeText);
        }

        int getRange() {
            return range;
        }

        /**
         * @return the index of the lit bracket ({@code 1} short to {@code 4} extreme), {@code 0} when the target is
         *       inside the minimum range, or {@code -1} when nothing is lit
         */
        int getLitBracket() {
            if (!hasWeapon || (range == NO_RANGE)) {
                return -1;
            }
            if ((ranges[MINIMUM] > 0) && (range <= ranges[MINIMUM])) {
                return MINIMUM;
            }
            for (int i = 1; i < BRACKET_KEYS.length; i++) {
                if ((ranges[i] > 0) && (range <= ranges[i])) {
                    return i;
                }
            }
            return -1;
        }

        private void relabel() {
            int lit = getLitBracket();
            Color highlight = GUIP.getUnitToolTipHighlightColor();
            int lowerBound = 1;
            for (int i = 0; i < BRACKET_KEYS.length; i++) {
                String name = Messages.getString("UnitDisplay.weaponTab.bracket." + BRACKET_KEYS[i]);
                String span;
                if (!hasWeapon || (ranges[i] <= 0)) {
                    span = "-";
                } else if (i == MINIMUM) {
                    span = Integer.toString(ranges[i]);
                } else {
                    span = lowerBound + "-" + ranges[i];
                    lowerBound = ranges[i] + 1;
                }
                cells[i].setText("<html><center>" + name + "<br>" + span + "</center></html>");
                cells[i].setBackground((i == lit) ? highlight : UIManager.getColor("Panel.background"));
                cells[i].setForeground((i == lit) ? Color.BLACK : UIManager.getColor("Label.foreground"));
            }
            if (!hasWeapon) {
                summary.setText(Messages.getString("UnitDisplay.weaponTab.noWeapon"));
            } else if (range == NO_RANGE) {
                summary.setText(Messages.getString("UnitDisplay.weaponTab.noRange"));
            } else if (lit == -1) {
                summary.setText(Messages.getFormattedString("UnitDisplay.weaponTab.rangeBeyond", range));
            } else {
                summary.setText(Messages.getFormattedString("UnitDisplay.weaponTab.rangeAt", range,
                      Messages.getString("UnitDisplay.weaponTab.bracket." + BRACKET_KEYS[lit])));
            }
        }
    }

    /** The to-hit number and its odds on the first line, then one line per modifier. */
    static final class ToHitBreakdown extends JPanel {

        @Serial
        private static final long serialVersionUID = 1L;

        private final JLabel total = new JLabel();
        private final JPanel rows = new JPanel(new GridBagLayout());
        private final List<String> lines = new ArrayList<>();

        ToHitBreakdown() {
            super(new BorderLayout(0, UIUtil.scaleForGUI(2)));
            total.setFont(total.getFont().deriveFont(Font.BOLD));
            add(total, BorderLayout.PAGE_START);
            add(rows, BorderLayout.CENTER);
        }

        void show(ToHitData toHit, boolean naturalAptitudeGunnery) {
            String heading = switch (toHit.getValue()) {
                case TargetRoll.IMPOSSIBLE, TargetRoll.AUTOMATIC_FAIL ->
                      Messages.getFormattedString("UnitDisplay.weaponTab.toHitNever", toHit.getDesc());
                case TargetRoll.AUTOMATIC_SUCCESS ->
                      Messages.getFormattedString("UnitDisplay.weaponTab.toHitAlways", toHit.getDesc());
                default -> Messages.getFormattedString("UnitDisplay.weaponTab.toHitTotal", toHit.getValue(),
                      Compute.oddsAbove(toHit.getValue(), naturalAptitudeGunnery));
            };
            total.setText(heading);
            total.setForeground((toHit.getValue() == TargetRoll.IMPOSSIBLE) || (toHit.getValue()
                  == TargetRoll.AUTOMATIC_FAIL)
                  ? UIManager.getColor("Label.disabledForeground")
                  : GUIP.getUnitToolTipHighlightColor());
            rows.removeAll();
            lines.clear();
            if (toHit.needsRoll()) {
                int inset = UIUtil.scaleForGUI(2);
                GridBagConstraints constraints = new GridBagConstraints();
                constraints.anchor = GridBagConstraints.LINE_START;
                constraints.insets = new Insets(0, inset, 0, inset * 3);
                constraints.gridy = 0;
                for (TargetRollModifier modifier : toHit.getModifiers()) {
                    constraints.gridx = 0;
                    JLabel value = new JLabel(String.format("%+d", modifier.value()), SwingConstants.RIGHT);
                    rows.add(value, constraints);
                    constraints.gridx = 1;
                    constraints.weightx = 1.0;
                    rows.add(new JLabel(modifier.getDesc()), constraints);
                    constraints.weightx = 0.0;
                    constraints.gridy++;
                    lines.add(value.getText() + " " + modifier.getDesc());
                }
            }
            rows.revalidate();
            rows.repaint();
        }

        void show(String message) {
            total.setText(UnitToolTip.wrapWithHTML(message));
            total.setForeground(UIManager.getColor("Label.foreground"));
            rows.removeAll();
            lines.clear();
            rows.revalidate();
            rows.repaint();
        }

        void clear() {
            show("---");
        }

        String getTotalText() {
            return total.getText();
        }

        List<String> getLines() {
            return List.copyOf(lines);
        }
    }
}
