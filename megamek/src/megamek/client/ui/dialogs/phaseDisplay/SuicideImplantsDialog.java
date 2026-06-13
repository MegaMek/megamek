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
package megamek.client.ui.dialogs.phaseDisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.actions.SuicideImplantsAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.Aero;
import megamek.common.units.Entity;

/**
 * Dialog for configuring suicide implant detonation.
 * <p>
 * For conventional infantry and Battle Armor, this dialog presents a slider to select the number of troopers to
 * detonate, along with a damage preview.
 * </p>
 * <p>
 * For Meks, Aeros, and Vehicles, this dialog presents a simple confirmation message describing what will happen.
 * </p>
 */
public class SuicideImplantsDialog extends JDialog implements ActionListener, ChangeListener {
    @Serial
    private static final long serialVersionUID = 2938475629384756293L;

    private final JButton buttonOk = new JButton(Messages.getString("Okay"));
    private final JButton buttonCancel = new JButton(Messages.getString("Cancel"));

    private JSlider trooperSlider;
    private JLabel damageLabel;
    private JLabel trooperCountLabel;

    private final Entity entity;
    private int trooperCount;
    private boolean confirmed = false;

    /**
     * Creates a new suicide implants dialog.
     *
     * @param parent the parent frame
     * @param entity the entity that will detonate
     */
    public SuicideImplantsDialog(JFrame parent, Entity entity) {
        super(parent, Messages.getString("SuicideImplantsDialog.title"), true);
        this.entity = entity;
        this.trooperCount = SuicideImplantsAttackAction.getMaxTroopersFor(entity);

        setResizable(false);
        initializeComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initializeComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(
              UIUtil.scaleForGUI(10),
              UIUtil.scaleForGUI(15),
              UIUtil.scaleForGUI(10),
              UIUtil.scaleForGUI(15)));

        // Warning label
        JLabel warningLabel = new JLabel(Messages.getString("SuicideImplantsDialog.warning"));
        warningLabel.setForeground(Color.RED);
        warningLabel.setFont(warningLabel.getFont().deriveFont(Font.BOLD));
        warningLabel.setAlignmentX(CENTER_ALIGNMENT);
        mainPanel.add(warningLabel);

        mainPanel.add(javax.swing.Box.createVerticalStrut(UIUtil.scaleForGUI(10)));

        // Entity-specific content
        if (entity.isConventionalInfantry() || entity instanceof BattleArmor) {
            addTrooperSliderPanel(mainPanel);
        } else {
            addConfirmationPanel(mainPanel);
        }

        mainPanel.add(javax.swing.Box.createVerticalStrut(UIUtil.scaleForGUI(15)));

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonOk.addActionListener(this);
        buttonCancel.addActionListener(this);
        buttonPanel.add(buttonOk);
        buttonPanel.add(buttonCancel);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addTrooperSliderPanel(JPanel mainPanel) {
        int maxTroopers = SuicideImplantsAttackAction.getMaxTroopersFor(entity);

        // Instruction label
        JLabel instructionLabel = new JLabel(Messages.getString("SuicideImplantsDialog.selectTroopers"));
        instructionLabel.setAlignmentX(CENTER_ALIGNMENT);
        mainPanel.add(instructionLabel);

        mainPanel.add(javax.swing.Box.createVerticalStrut(UIUtil.scaleForGUI(5)));

        // Slider panel
        JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JLabel minLabel = new JLabel("1");
        trooperSlider = new JSlider(SwingConstants.HORIZONTAL, 1, maxTroopers, maxTroopers);
        trooperSlider.addChangeListener(this);
        trooperSlider.setMajorTickSpacing(Math.max(1, (int) Math.floor(maxTroopers / 5.0)));
        trooperSlider.setMinorTickSpacing(1);
        trooperSlider.setPaintTicks(true);
        JLabel maxLabel = new JLabel(String.valueOf(maxTroopers));

        trooperCountLabel = new JLabel(String.valueOf(maxTroopers));
        trooperCountLabel.setFont(trooperCountLabel.getFont().deriveFont(Font.BOLD));

        sliderPanel.add(minLabel);
        sliderPanel.add(trooperSlider);
        sliderPanel.add(maxLabel);
        sliderPanel.add(new JLabel(" = "));
        sliderPanel.add(trooperCountLabel);

        mainPanel.add(sliderPanel);

        mainPanel.add(javax.swing.Box.createVerticalStrut(UIUtil.scaleForGUI(10)));

        // Damage preview
        int damage = SuicideImplantsAttackAction.getDamageFor(maxTroopers);
        String damageText;
        if (entity.isConventionalInfantry()) {
            damageText = Messages.getString("SuicideImplantsDialog.damage", damage);
        } else {
            damageText = Messages.getString("SuicideImplantsDialog.confirmBA");
        }
        damageLabel = new JLabel(damageText);
        damageLabel.setAlignmentX(CENTER_ALIGNMENT);
        mainPanel.add(damageLabel);
    }

    private void addConfirmationPanel(JPanel mainPanel) {
        String confirmMessage;
        if (entity.isMek()) {
            confirmMessage = Messages.getString("SuicideImplantsDialog.confirmMek");
        } else if (entity instanceof Aero) {
            // Note: Keep instanceof Aero - entity.isAero() is not equivalent
            confirmMessage = Messages.getString("SuicideImplantsDialog.confirmAero");
        } else if (entity.isVehicle()) {
            confirmMessage = Messages.getString("SuicideImplantsDialog.confirmVehicle");
        } else {
            confirmMessage = Messages.getString("SuicideImplantsDialog.confirmGeneric");
        }

        JLabel confirmLabel = new JLabel("<html><center>" + confirmMessage + "</center></html>");
        confirmLabel.setAlignmentX(CENTER_ALIGNMENT);
        mainPanel.add(confirmLabel);
    }

    /**
     * Shows the dialog and returns whether the user confirmed.
     *
     * @return true if the user clicked OK, false otherwise
     */
    public boolean showDialog() {
        setVisible(true);
        return confirmed;
    }

    /**
     * @return the number of troopers selected to detonate
     */
    public int getTrooperCount() {
        return trooperCount;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == buttonOk) {
            confirmed = true;
        }
        setVisible(false);
        dispose();
    }

    @Override
    public void stateChanged(ChangeEvent event) {
        if (event.getSource() == trooperSlider) {
            trooperCount = trooperSlider.getValue();
            trooperCountLabel.setText(String.valueOf(trooperCount));

            if (entity.isConventionalInfantry()) {
                int damage = SuicideImplantsAttackAction.getDamageFor(trooperCount);
                damageLabel.setText(Messages.getString("SuicideImplantsDialog.damage", damage));
            }
        }
    }
}
