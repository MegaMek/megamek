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
package megamek.client.ui.dialogs.phaseDisplay;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.client.ui.util.UIUtil;
import megamek.common.InfantryActionDeclaration;
import megamek.common.Player;
import megamek.common.compute.InfantryActionStrengths;
import megamek.common.game.Game;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;

/**
 * One player's declaration for an infantry vs. infantry action in one building (TO:AR pp. 169 to 172). The
 * attacker ticks the units to commit, or withdraws the force; the defender ticks the infantry to field and sets the
 * crew to commit, seeing the Marine Points it adds and the crew hits it costs. Each side sees its own strength unit
 * by unit and the other side as known. The dialog resizes freely and its text reflows to the width it is given.
 */
public class InfantryActionDeclarationDialog extends AbstractButtonDialog {

    private final Game game;
    private final Player player;
    private final AbstractBuildingEntity building;
    private final boolean defends;
    private final List<JCheckBox> unitBoxes = new ArrayList<>();
    private final List<Infantry> offeredUnits = new ArrayList<>();
    private JTextArea ownTotal;
    private JCheckBox withdrawBox;
    private JSpinner crewSpinner;
    private JTextArea crewEffect;

    /**
     * @param frame    the parent frame
     * @param game     the game
     * @param player   the declaring player
     * @param building the building the action is in, or would be in
     */
    public InfantryActionDeclarationDialog(JFrame frame, Game game, Player player, AbstractBuildingEntity building) {
        super(frame, "InfantryActionDeclarationDialog", InfantryActionStrengths.defends(player, building)
              ? "InfantryActionDeclarationDialog.title.defend" : "InfantryActionDeclarationDialog.title.attack");
        this.game = game;
        this.player = player;
        this.building = building;
        this.defends = InfantryActionStrengths.defends(player, building);
        initialize();
        setTitle(Messages.getString(titleKey(), building.getDisplayName()));
        setMinimumSize(new Dimension(UIUtil.scaleForGUI(360), UIUtil.scaleForGUI(240)));
    }

    /** Defend, attack, reinforce a running attack, or, with nothing left to add, continue or withdraw from it. */
    private String titleKey() {
        if (defends) {
            boolean nothingToCommit = InfantryActionStrengths.unengagedFriendlyInfantryInside(game, player, building)
                  .isEmpty() && (InfantryActionStrengths.crewAvailableToCommit(building) <= 0);
            boolean onlyWithdrawalLeft = nothingToCommit
                  && InfantryActionStrengths.canWithdrawDefence(game, player, building);
            return onlyWithdrawalLeft ? "InfantryActionDeclarationDialog.title.holdOrWithdraw"
                  : "InfantryActionDeclarationDialog.title.defend";
        }
        if (!InfantryActionStrengths.hasActionRunning(game, building)) {
            return "InfantryActionDeclarationDialog.title.attack";
        }
        boolean somethingToAdd = !InfantryActionStrengths.unengagedFriendlyInfantryInside(game, player, building)
              .isEmpty();
        return somethingToAdd ? "InfantryActionDeclarationDialog.title.reinforce"
              : "InfantryActionDeclarationDialog.title.continue";
    }

    @Override
    protected Container createCenterPane() {
        JPanel column = new WidthTrackingPanel(new GridBagLayout());
        int padding = UIUtil.scaleForGUI(6);
        column.setBorder(javax.swing.BorderFactory.createEmptyBorder(padding, padding * 2, padding, padding * 2));
        if (defends) {
            addDefenceRows(column);
        } else {
            addAttackRows(column);
        }
        // A filler row takes the spare height, so the rows stay at the top when the dialog is tall
        GridBagConstraints filler = rowConstraints();
        filler.weighty = 1;
        column.add(new JPanel(), filler);
        JScrollPane scroller = new JScrollPane(column, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(null);
        return scroller;
    }

    // ---------------------------------------------------------------- attacker

    private void addAttackRows(JPanel column) {
        List<Entity> engaged = new ArrayList<>();
        for (Entity entity : InfantryActionStrengths.engaged(game, building, true)) {
            if (entity.getOwnerId() == player.getId()) {
                engaged.add(entity);
            }
        }
        addHeading(column, Messages.getString(engaged.isEmpty() ? "InfantryActionDeclarationDialog.attackingWith"
              : "InfantryActionDeclarationDialog.reinforcingWith"));
        for (Entity unit : engaged) {
            addText(column, Messages.getString("InfantryActionDeclarationDialog.alreadyIn", unit.getDisplayName(),
                  number(InfantryActionStrengths.points(unit, null))));
        }
        for (Infantry unit : InfantryActionStrengths.unengagedFriendlyInfantryInside(game, player, building)) {
            addUnitBox(column, unit, true);
        }
        ownTotal = addText(column, "");
        if (!engaged.isEmpty()) {
            withdrawBox = new JCheckBox(Messages.getString("InfantryActionDeclarationDialog.withdraw"));
            withdrawBox.addActionListener(event -> refreshTotals());
            column.add(withdrawBox, rowConstraints());
            addText(column, Messages.getString("InfantryActionDeclarationDialog.withdrawExplained"));
        }
        addHeading(column, Messages.getString("InfantryActionDeclarationDialog.against"));
        double known = 0;
        for (Infantry enemy : InfantryActionStrengths.enemyInfantryInside(game, player, building)) {
            addText(column, unitLine(enemy, building));
            known += InfantryActionStrengths.points(enemy, building);
        }
        double crewPoints = InfantryActionStrengths.hasCrewToDefend(building)
              ? InfantryActionStrengths.crewPointsIfAllCommitted(building) : 0;
        if (crewPoints > 0) {
            addText(column, Messages.getString("InfantryActionDeclarationDialog.crewUpTo", building.getDisplayName(),
                  number(crewPoints)));
        }
        if ((known <= 0) && (crewPoints <= 0)) {
            addText(column, Messages.getString("InfantryActionDeclarationDialog.nobodyDefends"));
        }
        addText(column, Messages.getString("InfantryActionDeclarationDialog.defenderTotal", number(known),
              number(known + crewPoints)));
        refreshTotals();
    }

    // ---------------------------------------------------------------- defender

    private void addDefenceRows(JPanel column) {
        List<Entity> engaged = new ArrayList<>();
        for (Entity entity : InfantryActionStrengths.engaged(game, building, false)) {
            boolean ownInfantry = (entity.getOwnerId() == player.getId()) && (entity != building);
            if (ownInfantry) {
                engaged.add(entity);
            }
        }
        addHeading(column, Messages.getString("InfantryActionDeclarationDialog.defendingWith"));
        for (Entity unit : engaged) {
            addText(column, Messages.getString("InfantryActionDeclarationDialog.alreadyIn", unit.getDisplayName(),
                  number(InfantryActionStrengths.points(unit, building))));
        }
        for (Infantry unit : InfantryActionStrengths.unengagedFriendlyInfantryInside(game, player, building)) {
            addUnitBox(column, unit, true);
        }
        int available = InfantryActionStrengths.crewAvailableToCommit(building);
        if (InfantryActionStrengths.hasCrewToDefend(building)) {
            addText(column, Messages.getString("InfantryActionDeclarationDialog.crewState",
                  building.getCommittedCrew(), building.getCrew().getCurrentSize()));
            JPanel spinnerRow = new JPanel(new GridBagLayout());
            GridBagConstraints labelConstraints = new GridBagConstraints();
            labelConstraints.insets = new Insets(0, 0, 0, UIUtil.scaleForGUI(6));
            spinnerRow.add(new JLabel(Messages.getString("InfantryActionDeclarationDialog.commitCrew")),
                  labelConstraints);
            crewSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Math.max(0, available), 1));
            crewSpinner.setEnabled(available > 0);
            crewSpinner.addChangeListener(event -> refreshTotals());
            spinnerRow.add(crewSpinner, new GridBagConstraints());
            column.add(spinnerRow, rowConstraints());
            crewEffect = addText(column, "");
        }
        ownTotal = addText(column, "");
        if (InfantryActionStrengths.canWithdrawDefence(game, player, building)) {
            withdrawBox = new JCheckBox(Messages.getString("InfantryActionDeclarationDialog.withdrawDefence"));
            withdrawBox.addActionListener(event -> refreshTotals());
            column.add(withdrawBox, rowConstraints());
            addText(column, Messages.getString("InfantryActionDeclarationDialog.withdrawDefenceExplained"));
        }
        addHeading(column, Messages.getString("InfantryActionDeclarationDialog.against"));
        double attackers = 0;
        for (Entity attacker : InfantryActionStrengths.engaged(game, building, true)) {
            addText(column, unitLine(attacker, null));
            attackers += InfantryActionStrengths.points(attacker, null);
        }
        for (Infantry enemy : InfantryActionStrengths.enemyInfantryInside(game, player, building)) {
            if (enemy.getInfantryCombatTargetId() == Entity.NONE) {
                addText(column, Messages.getString("InfantryActionDeclarationDialog.couldAttack",
                      enemy.getDisplayName(), number(InfantryActionStrengths.points(enemy, null))));
                attackers += InfantryActionStrengths.points(enemy, null);
            }
        }
        addText(column, Messages.getString("InfantryActionDeclarationDialog.attackerTotal", number(attackers),
              InfantryActionStrengths.roundedUp(attackers)));
        refreshTotals();
    }

    // ---------------------------------------------------------------- shared rows

    private void addUnitBox(JPanel column, Infantry unit, boolean ticked) {
        JCheckBox box = new JCheckBox(unitLine(unit, defends ? building : null), ticked);
        box.addActionListener(event -> refreshTotals());
        unitBoxes.add(box);
        offeredUnits.add(unit);
        column.add(box, rowConstraints());
    }

    private void refreshTotals() {
        double units = 0;
        for (Infantry unit : getCommittedUnits()) {
            units += InfantryActionStrengths.points(unit, defends ? building : null);
        }
        for (Entity engaged : InfantryActionStrengths.engaged(game, building, !defends)) {
            boolean own = (engaged.getOwnerId() == player.getId()) && (engaged != building);
            if (own) {
                units += InfantryActionStrengths.points(engaged, defends ? building : null);
            }
        }
        boolean withdrawing = (withdrawBox != null) && withdrawBox.isSelected();
        for (JCheckBox box : unitBoxes) {
            box.setEnabled(!withdrawing);
        }
        if (defends && (crewSpinner != null)) {
            crewSpinner.setEnabled(!withdrawing && (InfantryActionStrengths.crewAvailableToCommit(building) > 0));
            int extra = (Integer) crewSpinner.getValue();
            double crewPoints = InfantryActionStrengths.crewPointsIfCommitted(building,
                  building.getCommittedCrew() + extra);
            int hits = InfantryActionStrengths.crewHitsIfCommitted(building, extra);
            crewEffect.setText(Messages.getString("InfantryActionDeclarationDialog.crewEffect", extra,
                  number(crewPoints), hits));
            units += crewPoints;
        }
        ownTotal.setText(Messages.getString(withdrawing ? "InfantryActionDeclarationDialog.withdrawing"
              : "InfantryActionDeclarationDialog.ownTotal", number(units), InfantryActionStrengths.roundedUp(units)));
    }

    private String unitLine(Entity unit, AbstractBuildingEntity defended) {
        return Messages.getString("InfantryActionDeclarationDialog.unitPoints", unit.getDisplayName(),
              number(InfantryActionStrengths.points(unit, defended)));
    }

    private static GridBagConstraints rowConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = GridBagConstraints.RELATIVE;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(UIUtil.scaleForGUI(2), 0, UIUtil.scaleForGUI(2), 0);
        return constraints;
    }

    private static void addHeading(JPanel column, String text) {
        JLabel heading = new JLabel("<html><b>" + text + "</b></html>");
        GridBagConstraints constraints = rowConstraints();
        constraints.insets = new Insets(UIUtil.scaleForGUI(8), 0, UIUtil.scaleForGUI(2), 0);
        column.add(heading, constraints);
    }

    /** A line of text that wraps to whatever width the column has, so the dialog can be any size. */
    private static JTextArea addText(JPanel column, String text) {
        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(null);
        area.setFont(UIManager.getFont("Label.font"));
        area.setForeground(UIManager.getColor("Label.foreground"));
        column.add(area, rowConstraints());
        return area;
    }

    /**
     * The scroll pane's view, which takes the viewport's width rather than its own preferred width. A plain panel
     * inside a scroll pane is laid out as wide as its longest line, so the text areas wrap at a width the dialog
     * cannot show and their ends are cut off; tracking the viewport width makes them wrap at the dialog's edge.
     * Height stays free, so the dialog still scrolls vertically when it is short.
     */
    private static class WidthTrackingPanel extends JPanel implements Scrollable {
        @Serial
        private static final long serialVersionUID = 6098141276423589341L;

        private static final int SCROLL_UNIT_INCREMENT = 16;

        WidthTrackingPanel(LayoutManager layoutManager) {
            super(layoutManager);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return SCROLL_UNIT_INCREMENT;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return (orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** A Marine Points figure: whole numbers plain, fractions to two places. */
    private static String number(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    // ---------------------------------------------------------------- results

    /**
     * @return the units the player ticked
     */
    public List<Infantry> getCommittedUnits() {
        List<Infantry> committed = new ArrayList<>();
        for (int index = 0; index < unitBoxes.size(); index++) {
            if (unitBoxes.get(index).isSelected()) {
                committed.add(offeredUnits.get(index));
            }
        }
        return committed;
    }

    /**
     * @return the declaration the player made, for the server
     */
    public InfantryActionDeclaration getDeclaration() {
        List<Integer> unitIds = getCommittedUnits().stream().map(Entity::getId).toList();
        boolean withdrawing = (withdrawBox != null) && withdrawBox.isSelected();
        if (defends) {
            int crew = (crewSpinner == null) ? 0 : (Integer) crewSpinner.getValue();
            return InfantryActionDeclaration.defending(player.getId(), building.getId(), unitIds, crew, withdrawing);
        }
        return InfantryActionDeclaration.attacking(player.getId(), building.getId(),
              withdrawing ? List.of() : unitIds, withdrawing);
    }

    /**
     * @return {@code true} when the declaration commits something or withdraws, so there is something to send
     */
    public boolean declaresAnything() {
        InfantryActionDeclaration declaration = getDeclaration();
        return declaration.withdraw() || !declaration.committedUnitIds().isEmpty()
              || (declaration.committedCrew() > 0);
    }
}
