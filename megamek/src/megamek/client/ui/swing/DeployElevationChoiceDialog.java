/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.DeploymentElevationType;
import megamek.common.ElevationOption;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.util.List;

import static megamek.client.ui.swing.util.UIUtil.spanCSS;

/**
 * This dialog allows selecting an elevation (or altitude) during deployment.
 */
public class DeployElevationChoiceDialog extends AbstractChoiceDialog<ElevationOption> {

    private static final int BASE_PADDING = 10;

    protected DeployElevationChoiceDialog(JFrame parent, List<ElevationOption> elevationOptions) {
        super(parent, "DeploymentDisplay.choiceDialogTitle", titleMessage(), elevationOptions, false);
        setColumns(elevationOptions.size() > 6 ? 2 : 1);
        initialize();
        setUseDetailed(false);
    }

    @Override
    protected void detailLabel(JToggleButton button, ElevationOption elevationOption) {
        String description = Messages.getString("DeploymentDisplay.deployElevation." + elevationOption.type());
        String elevationAltitude = elevationOption.type() == DeploymentElevationType.ALTITUDE
                ? Messages.getString("DeploymentDisplay.altitude")
                : Messages.getString("DeploymentDisplay.elevation");
        String elevationText = elevationAltitude + elevationOption.elevation();
        String text = "<HTML><HEAD>" + styles() + "</HEAD><BODY><CENTER>" + spanCSS("description", description);
        if (elevationOption.type() != DeploymentElevationType.ELEVATIONS_ABOVE) {
            text += "<BR>" + spanCSS("elevation", elevationText) + "</BODY></HTML>";
        }
        button.setText(text);
    }

    @Override
    protected JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")),
                new UIUtil.ScaledEmptyBorder(10, 0, 10, 0)));
        buttonPanel.add(new ButtonEsc(new CloseAction(this)));
        return buttonPanel;
    }

    @Override
    protected void summaryLabel(JToggleButton button, ElevationOption target) {
        detailLabel(button, target);
    }

    private static String titleMessage() {
        return "<HTML><HEAD>" + styles() + "</HEAD><BODY><div class=frame>"
                + spanCSS("label", Messages.getString("DeploymentDisplay.choice"))
                + "</div></BODY></HTML>";
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);
            pack();
        }
        super.setVisible(visible);
    }

    public static String styles() {
        int descriptionSize = UIUtil.scaleForGUI(UIUtil.FONT_SCALE1);
        int elevationSize = (int) (0.8 * UIUtil.scaleForGUI(UIUtil.FONT_SCALE1));
        int padding = UIUtil.scaleForGUI(BASE_PADDING);
        return "<style> " +
                ".description { font-family:Noto Sans; font-size:" + descriptionSize + ";  }" +
                ".elevation { font-family:Noto Sans; font-size:" + elevationSize + ";  }" +
                ".frame { padding:" + padding + " " + 2 * padding + " 0 0;  }";
    }
}
