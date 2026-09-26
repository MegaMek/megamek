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
package megamek.client.ui.dialogs.BotCommands;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.client.ui.panels.FacingPickerPanel;
import megamek.client.ui.util.UIUtil;
import megamek.common.orders.UnitOrders;
import megamek.common.units.Entity;

/**
 * Sets the two facings a player can order for bot units: the facing each turn ends in while moving, and the facing
 * once stopped at the end of the route. Each side is the turret facing picker - six choices around the unit's picture
 * on a hex - with an Auto box that leaves the choice to the bot.
 */
public class BotOrderFacingDialog extends AbstractButtonDialog {

    private static final int FACING_COUNT = 6;
    private static final int GAP = 10;

    private final ClientGUI clientGUI;
    private final Entity previewUnit;
    private final FacingChoice whileMoving;
    private final FacingChoice whenStopped;

    /**
     * One side of the dialog: six facing buttons and an Auto box.
     */
    private static final class FacingChoice {
        private final List<JRadioButton> buttons = new ArrayList<>();
        private final ButtonGroup buttonGroup = new ButtonGroup();
        private final JCheckBox autoBox = new JCheckBox(Messages.getString("BotCommandPanel.Orders.facing.auto"));

        private FacingChoice(int currentFacing) {
            for (int facing = 0; facing < FACING_COUNT; facing++) {
                JRadioButton button = new JRadioButton();
                button.setActionCommand(Integer.toString(facing));
                button.setSelected(facing == currentFacing);
                buttons.add(button);
                buttonGroup.add(button);
            }
            autoBox.setSelected(currentFacing == UnitOrders.FACING_AUTO);
            autoBox.addActionListener(event -> updateEnabled());
            updateEnabled();
        }

        private void updateEnabled() {
            for (JRadioButton button : buttons) {
                button.setEnabled(!autoBox.isSelected());
            }
        }

        private int chosenFacing() {
            if (autoBox.isSelected() || (buttonGroup.getSelection() == null)) {
                return UnitOrders.FACING_AUTO;
            }
            return Integer.parseInt(buttonGroup.getSelection().getActionCommand());
        }
    }

    /**
     * @param frame           the parent frame
     * @param clientGUI       the client GUI, for the unit's picture
     * @param previewUnit     the unit whose picture is shown
     * @param facingWhileMoving the current facing while moving, 0-5 or {@link UnitOrders#FACING_AUTO}
     * @param facingWhenStopped the current facing when stopped, 0-5 or {@link UnitOrders#FACING_AUTO}
     */
    public BotOrderFacingDialog(JFrame frame, ClientGUI clientGUI, Entity previewUnit, int facingWhileMoving,
          int facingWhenStopped) {
        super(frame, "BotOrderFacingDialog", "BotCommandPanel.Orders.facing.dialogTitle");
        this.clientGUI = clientGUI;
        this.previewUnit = previewUnit;
        this.whileMoving = new FacingChoice(facingWhileMoving);
        this.whenStopped = new FacingChoice(facingWhenStopped);
        initialize();
    }

    @Override
    protected Container createCenterPane() {
        JPanel sides = new JPanel(new GridLayout(1, 2, UIUtil.scaleForGUI(GAP), 0));
        sides.setBorder(new EmptyBorder(UIUtil.scaleForGUI(GAP), UIUtil.scaleForGUI(GAP), UIUtil.scaleForGUI(GAP),
              UIUtil.scaleForGUI(GAP)));
        sides.add(createSide(Messages.getString("BotCommandPanel.Orders.facing.moving"), whileMoving));
        sides.add(createSide(Messages.getString("BotCommandPanel.Orders.facing.stopped"), whenStopped));
        return sides;
    }

    private JPanel createSide(String title, FacingChoice choice) {
        JPanel side = new JPanel(new BorderLayout());
        side.setBorder(BorderFactory.createTitledBorder(title));
        side.add(new FacingPickerPanel(choice.buttons, FacingPickerPanel.previewOnHex(clientGUI, previewUnit, 0)),
              BorderLayout.CENTER);
        side.add(choice.autoBox, BorderLayout.SOUTH);
        return side;
    }

    /**
     * @return the chosen facing while moving, 0-5 or {@link UnitOrders#FACING_AUTO}
     */
    public int getFacingWhileMoving() {
        return whileMoving.chosenFacing();
    }

    /**
     * @return the chosen facing when stopped, 0-5 or {@link UnitOrders#FACING_AUTO}
     */
    public int getFacingWhenStopped() {
        return whenStopped.chosenFacing();
    }
}
