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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.client.ui.util.UIUtil;
import megamek.common.compute.InfantryActionStrengths;
import megamek.common.game.Game;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;

/**
 * The declaration of an infantry vs. infantry action, with the strengths on show (TO:AR pp. 169 to 172). Starting an
 * action lists every friendly unit in the building with a tick box each, so the player commits one, some or all in
 * one declaration, and shows what they face: the enemy infantry inside exactly, the building's crew as an upper
 * bound, since crew commitment is the one figure the book keeps hidden. Joining an action shows what the unit adds
 * and the totals it produces.
 */
public class InfantryActionDeclarationDialog extends AbstractButtonDialog {

    /** The width, before GUI scaling, that the dialog's lines wrap at. */
    private static final int TEXT_WIDTH = 440;

    private final Game game;
    private final AbstractBuildingEntity building;
    private final Infantry declaringUnit;
    private final boolean isInitiation;
    private final List<JCheckBox> unitBoxes = new ArrayList<>();
    private final List<Infantry> offeredUnits = new ArrayList<>();
    private JLabel attackerTotal;

    private InfantryActionDeclarationDialog(JFrame frame, Game game, Infantry declaringUnit,
          AbstractBuildingEntity building, boolean isInitiation) {
        super(frame, "InfantryActionDeclarationDialog", isInitiation
              ? "PreEndDeclarationsDisplay.InitiateInfantryCombatDialog.title"
              : "InfantryVsInfantryCombatDisplay.ReinforceInfantryCombatDialog.title");
        this.game = game;
        this.declaringUnit = declaringUnit;
        this.building = building;
        this.isInitiation = isInitiation;
        initialize();
    }

    /**
     * The dialog for starting an action.
     *
     * @param frame     the parent frame
     * @param game      the game
     * @param initiator the unit whose turn it is
     * @param building  the building to attack
     *
     * @return the dialog, not yet shown
     */
    public static InfantryActionDeclarationDialog forInitiation(JFrame frame, Game game, Infantry initiator,
          AbstractBuildingEntity building) {
        return new InfantryActionDeclarationDialog(frame, game, initiator, building, true);
    }

    /**
     * The dialog for joining a running action.
     *
     * @param frame    the parent frame
     * @param game     the game
     * @param joiner   the unit whose turn it is
     * @param building the building the action is in
     *
     * @return the dialog, not yet shown
     */
    public static InfantryActionDeclarationDialog forJoining(JFrame frame, Game game, Infantry joiner,
          AbstractBuildingEntity building) {
        return new InfantryActionDeclarationDialog(frame, game, joiner, building, false);
    }

    @Override
    protected Container createCenterPane() {
        int verticalPadding = UIUtil.scaleForGUI(8);
        int horizontalPadding = UIUtil.scaleForGUI(14);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setBorder(new EmptyBorder(verticalPadding, horizontalPadding, verticalPadding, horizontalPadding));
        if (isInitiation) {
            addInitiationSections(panel, verticalPadding);
        } else {
            addJoiningSections(panel, verticalPadding);
        }
        return panel;
    }

    private void addInitiationSections(JPanel panel, int verticalPadding) {
        addHeading(panel, Messages.getString("InfantryActionDeclarationDialog.attackingWith", building.getDisplayName()));
        List<Infantry> candidates = InfantryActionStrengths.unengagedFriendlyInfantryInside(game,
              declaringUnit.getOwner(), building);
        for (Infantry unit : candidates) {
            JCheckBox box = new JCheckBox("<html><body style='width: " + UIUtil.scaleForGUI(TEXT_WIDTH) + "px'>"
                  + unitLine(unit, null) + "</body></html>", true);
            box.setEnabled(unit.getId() != declaringUnit.getId());
            box.setAlignmentX(LEFT_ALIGNMENT);
            box.addActionListener(event -> refreshAttackerTotal());
            unitBoxes.add(box);
            offeredUnits.add(unit);
            panel.add(box);
        }
        attackerTotal = new JLabel();
        attackerTotal.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(attackerTotal);
        refreshAttackerTotal();
        panel.add(Box.createVerticalStrut(verticalPadding));

        addHeading(panel, Messages.getString("InfantryActionDeclarationDialog.against"));
        List<Infantry> enemies = InfantryActionStrengths.enemyInfantryInside(game, declaringUnit.getOwner(), building);
        double known = 0;
        for (Infantry enemy : enemies) {
            addLine(panel, unitLine(enemy, building));
            known += InfantryActionStrengths.points(enemy, building);
        }
        double crewPoints = InfantryActionStrengths.hasCrewToDefend(building)
              ? InfantryActionStrengths.points(building, building) : 0;
        if (crewPoints > 0) {
            addLine(panel, Messages.getString("InfantryActionDeclarationDialog.crewUpTo", building.getDisplayName(),
                  number(crewPoints)));
        }
        if (enemies.isEmpty() && (crewPoints <= 0)) {
            addLine(panel, Messages.getString("InfantryActionDeclarationDialog.nobodyDefends"));
        }
        addLine(panel, Messages.getString("InfantryActionDeclarationDialog.defenderTotal", number(known),
              number(known + crewPoints)));
    }

    private void addJoiningSections(JPanel panel, int verticalPadding) {
        boolean joinsAttackers = joinsAttackers();
        AbstractBuildingEntity defended = joinsAttackers ? null : building;
        double joinerPoints = InfantryActionStrengths.points(declaringUnit, defended);
        addLine(panel, Messages.getString("InfantryActionDeclarationDialog.joinsWith", declaringUnit.getDisplayName(),
              number(joinerPoints), Messages.getString(joinsAttackers
                    ? "InfantryActionDeclarationDialog.theAttackers" : "InfantryActionDeclarationDialog.theDefenders")));
        panel.add(Box.createVerticalStrut(verticalPadding));
        double attackers = InfantryActionStrengths.total(InfantryActionStrengths.engaged(game, building, true), null);
        double defenders = InfantryActionStrengths.total(InfantryActionStrengths.engaged(game, building, false),
              building);
        double attackersAfter = joinsAttackers ? attackers + joinerPoints : attackers;
        double defendersAfter = joinsAttackers ? defenders : defenders + joinerPoints;
        addLine(panel, Messages.getString("InfantryActionDeclarationDialog.sideNowThen",
              Messages.getString("InfantryActionDeclarationDialog.attackers"),
              InfantryActionStrengths.roundedUp(attackers), InfantryActionStrengths.roundedUp(attackersAfter)));
        addLine(panel, Messages.getString("InfantryActionDeclarationDialog.sideNowThen",
              Messages.getString("InfantryActionDeclarationDialog.defenders"),
              InfantryActionStrengths.roundedUp(defenders), InfantryActionStrengths.roundedUp(defendersAfter)));
    }

    /** The side a joining unit lands on: with the attackers when any defender is its enemy, else the defenders. */
    private boolean joinsAttackers() {
        for (Entity defender : InfantryActionStrengths.engaged(game, building, false)) {
            if (defender.getOwner().isEnemyOf(declaringUnit.getOwner())) {
                return true;
            }
        }
        for (Entity attacker : InfantryActionStrengths.engaged(game, building, true)) {
            if (attacker.getOwner().isEnemyOf(declaringUnit.getOwner())) {
                return false;
            }
        }
        return true;
    }

    private void refreshAttackerTotal() {
        double total = 0;
        for (Infantry unit : getCommittedUnits()) {
            total += InfantryActionStrengths.points(unit, null);
        }
        attackerTotal.setText(Messages.getString("InfantryActionDeclarationDialog.attackerTotal", number(total),
              InfantryActionStrengths.roundedUp(total)));
    }

    private String unitLine(Entity unit, AbstractBuildingEntity defended) {
        return Messages.getString("InfantryActionDeclarationDialog.unitPoints", unit.getDisplayName(),
              number(InfantryActionStrengths.points(unit, defended)));
    }

    private static void addHeading(JPanel panel, String text) {
        JLabel heading = new JLabel("<html><b>" + text + "</b></html>");
        heading.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(heading);
    }

    /** A line that wraps: labels only wrap as HTML with a width, so every line is given the dialog's text width. */
    private static void addLine(JPanel panel, String text) {
        JLabel line = new JLabel("<html><body style='width: " + UIUtil.scaleForGUI(TEXT_WIDTH) + "px'>" + text
              + "</body></html>");
        line.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(line);
    }

    /** A Marine Points figure: whole numbers plain, fractions to two places. */
    private static String number(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /**
     * The units the player ticked, the declaring unit always among them. Empty for a joining dialog.
     *
     * @return the committed units
     */
    public List<Infantry> getCommittedUnits() {
        List<Infantry> committed = new ArrayList<>();
        for (int index = 0; index < unitBoxes.size(); index++) {
            boolean committedUnit = unitBoxes.get(index).isSelected()
                  || (offeredUnits.get(index).getId() == declaringUnit.getId());
            if (committedUnit) {
                committed.add(offeredUnits.get(index));
            }
        }
        return committed;
    }

    /**
     * The ids of the committed units other than the declaring one, for the initiation action.
     *
     * @return the other units' ids
     */
    public List<Integer> getOtherCommittedUnitIds() {
        List<Integer> ids = new ArrayList<>();
        for (Infantry unit : getCommittedUnits()) {
            if (unit.getId() != declaringUnit.getId()) {
                ids.add(unit.getId());
            }
        }
        return ids;
    }
}
