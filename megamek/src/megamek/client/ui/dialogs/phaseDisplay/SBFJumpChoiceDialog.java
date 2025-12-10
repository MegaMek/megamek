/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.util.UIUtil.spanCSS;

import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import megamek.client.ui.buttons.ButtonEsc;
import megamek.client.ui.clientGUI.CloseAction;
import megamek.client.ui.util.UIUtil;

/**
 * This dialog is used to ask the player how many JUMP points are to be used for a planned movement.
 */
public class SBFJumpChoiceDialog extends AbstractChoiceDialog<Integer> {

    private static final int BASE_PADDING = 10;
    private static final int BASE_JUMP_SIZE = 35;

    public SBFJumpChoiceDialog(JFrame parent, List<Integer> targets) {
        super(parent, "SBFTargetDialog.title", titleMessage(), targets, false);
        setColumns(1);
        initialize();
        setUseDetailed(false);
    }

    @Override
    protected void detailLabel(JToggleButton button, Integer target) {
        String targetText = (target > 0) ? Integer.toString(target) : "None";
        String text = "<HTML><HEAD>" + styles() + "</HEAD><BODY>"
              + spanCSS("button", targetText)
              + "</BODY></HTML>";
        button.setText(text);
    }

    @Override
    protected JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
              new MatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")),
              new EmptyBorder(10, 0, 10, 0)));
        buttonPanel.add(new ButtonEsc(new CloseAction(this)));
        return buttonPanel;
    }

    @Override
    protected void summaryLabel(JToggleButton button, Integer target) {
        detailLabel(button, target);
    }

    private static String titleMessage() {
        return "<HTML><HEAD>" + styles() + "</HEAD><BODY><div class=frame>"
              + spanCSS("label", "Choose the")
              + spanCSS("speccell", " JUMP ")
              + spanCSS("label", "points to use:")
              + "</div></BODY></HTML>";
    }

    public static String styles() {
        float labelSize = UIUtil.scaleForGUI(UIUtil.FONT_SCALE2);
        int padding = UIUtil.scaleForGUI(BASE_PADDING);
        int buttonSize = UIUtil.scaleForGUI(BASE_JUMP_SIZE);
        return "<style> " +
              ".label { font-family:Noto Sans; font-size:" + labelSize + ";  }" +
              ".frame { padding:" + padding + " " + 2 * padding + " 0 0;  }" +
              ".speccell { font-family:Exo; font-size:" + labelSize + "; }" +
              ".button { font-family:Exo; font-size:" + buttonSize + "; }";
    }
}
