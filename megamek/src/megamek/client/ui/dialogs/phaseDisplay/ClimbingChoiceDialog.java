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
 * Dialog for choosing how many levels to climb during TacOps Climbing (TO:AR p.20).
 * Presents options as buttons, each showing the number of levels and MP cost.
 */
public class ClimbingChoiceDialog extends AbstractChoiceDialog<ClimbingChoiceDialog.ClimbingOption> {

    private static final int BASE_PADDING = 10;
    private static final int BASE_BUTTON_HEIGHT = 50;
    private static final int BASE_HEADER_HEIGHT = 120;

    /**
     * The type of climbing action the player is choosing.
     */
    public enum ClimbingActionType {
        CLIMB_UP,
        CLIMB_DOWN,
        CLING,
        DANGLE_DOWN,
        DROP,
        /**
         * "Commit the path the planner already drew" — for the edge-descent dialog, this means
         * walking the routed-around path or taking the leap that's already attached to the
         * move command. Distinct from CLING (which holds the dialog open) because this option
         * explicitly commits the existing path so the player doesn't have to dismiss the
         * dialog and click Move separately.
         */
        WALK_AS_CALCULATED
    }

    /**
     * Represents a climbing action choice.
     *
     * @param levels the number of levels for this action (0 = cling/drop)
     * @param mpCost the MP cost for this action
     * @param label the display label
     * @param type the type of climbing action
     */
    public record ClimbingOption(int levels, int mpCost, String label, ClimbingActionType type) {
    }

    /**
     * Creates a climbing choice dialog.
     *
     * @param parent the parent frame
     * @param headerMessage HTML message to display at the top of the dialog
     * @param options the list of climbing options
     */
    public ClimbingChoiceDialog(JFrame parent, String headerMessage, List<ClimbingOption> options) {
        super(parent, "MovementDisplay.ClimbingDialog.title", formatHeader(headerMessage), options, false);
        setColumns(1);
        initialize();
        setUseDetailed(false);
        setAlwaysOnTop(true);
        setModalityType(ModalityType.APPLICATION_MODAL);
        // Ensure dialog is tall enough for all buttons plus header
        pack();
        int minHeight = UIUtil.scaleForGUI(BASE_BUTTON_HEIGHT * options.size() + BASE_HEADER_HEIGHT);
        if (getHeight() < minHeight) {
            setSize(getWidth(), minHeight);
        }
    }

    private static String formatHeader(String message) {
        int width = UIUtil.scaleForGUI(300);
        String htmlMessage = message.replace("\n", "<br>");
        return "<HTML><HEAD>" + styles() + "</HEAD><BODY>"
              + "<div style='width:" + width + "px;'>"
              + spanCSS("description", htmlMessage)
              + "</div></BODY></HTML>";
    }

    @Override
    protected void detailLabel(JToggleButton button, ClimbingOption option) {
        String text = "<HTML><HEAD>" + styles() + "</HEAD><BODY><CENTER>"
              + spanCSS("description", option.label())
              + "</CENTER></BODY></HTML>";
        button.setText(text);
    }

    @Override
    protected void summaryLabel(JToggleButton button, ClimbingOption option) {
        detailLabel(button, option);
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

    private static String styles() {
        int descriptionSize = UIUtil.scaleForGUI(UIUtil.FONT_SCALE1);
        int padding = UIUtil.scaleForGUI(BASE_PADDING);
        return "<style> "
              + ".description { font-family:Noto Sans; font-size:" + descriptionSize + ";  }"
              + ".frame { padding:" + padding + " " + 2 * padding + " 0 0;  }"
              + "</style>";
    }
}
