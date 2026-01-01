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

import static megamek.client.ui.util.UIUtil.spanCSS;

import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import megamek.client.ui.Messages;
import megamek.client.ui.buttons.ButtonEsc;
import megamek.client.ui.clientGUI.CloseAction;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.FacingOption;

/**
 * This dialog allows selecting a facing during deployment for multi-hex entities
 * where the facing affects which hexes the entity occupies.
 */
public class DeployFacingChoiceDialog extends AbstractChoiceDialog<Integer> {

    private static final int BASE_PADDING = 10;
    private static final String[] FACING_NAMES = {
        "DeploymentDisplay.facingNorth",
        "DeploymentDisplay.facingNorthEast",
        "DeploymentDisplay.facingSouthEast",
        "DeploymentDisplay.facingSouth",
        "DeploymentDisplay.facingSouthWest",
        "DeploymentDisplay.facingNorthWest"
    };

    public DeployFacingChoiceDialog(JFrame parent, FacingOption facingOption) {
        super(parent, "DeploymentDisplay.facingChoiceDialogTitle", titleMessage(),
              new ArrayList<>(facingOption.getValidFacings()), false);
        setColumns(facingOption.getValidFacingCount() > 3 ? 2 : 1);
        initialize();
        setUseDetailed(false);
    }

    /**
     * Gets the chosen facing value.
     *
     * @return The chosen facing (0-5), or -1 if no facing was chosen
     */
    public int getChosenFacing() {
        Integer facing = getFirstChoice();
        return (facing != null) ? facing : -1;
    }

    @Override
    protected void detailLabel(JToggleButton button, Integer facing) {
        String facingName = Messages.getString(FACING_NAMES[facing]);
        String text = "<HTML><HEAD>" + styles() + "</HEAD><BODY><CENTER>"
              + spanCSS("facing", facingName)
              + "<BR>" + spanCSS("facingNumber", "(" + facing + ")")
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
    protected void summaryLabel(JToggleButton button, Integer facing) {
        detailLabel(button, facing);
    }

    private static String titleMessage() {
        return "<HTML><HEAD>" + styles() + "</HEAD><BODY><div class=frame>"
              + spanCSS("label", Messages.getString("DeploymentDisplay.facingChoice"))
              + "</div></BODY></HTML>";
    }

    public static String styles() {
        int facingSize = UIUtil.scaleForGUI(UIUtil.FONT_SCALE1);
        int facingNumberSize = (int) (0.7 * UIUtil.scaleForGUI(UIUtil.FONT_SCALE1));
        int padding = UIUtil.scaleForGUI(BASE_PADDING);
        return "<style> " +
              ".facing { font-family:Noto Sans; font-size:" + facingSize + "; font-weight:bold; }" +
              ".facingNumber { font-family:Noto Sans; font-size:" + facingNumberSize + "; color:gray; }" +
              ".label { font-family:Noto Sans; font-size:" + facingSize + "; }" +
              ".frame { padding:" + padding + " " + 2 * padding + " 0 0; }";
    }
}
