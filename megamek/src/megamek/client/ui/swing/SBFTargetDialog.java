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

import megamek.client.ui.baseComponents.AbstractDialog;
import megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip;
import megamek.common.InGameObject;
import megamek.common.annotations.Nullable;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.strategicBattleSystems.SBFToHitData;

import javax.swing.*;
import java.awt.*;

public class SBFTargetDialog extends AbstractDialog {

    private static final String NO_TARGET = "No Target";
    private final JLabel header = new JLabel("Target");
    private final JLabel targetDisplay = new JLabel(NO_TARGET);
    private final JLabel toHitInformation = new JLabel();
    private final SBFGame game;

    private InGameObject target;

    protected SBFTargetDialog(JFrame parent, SBFGame game) {
        super(parent, "SBFTargetDialog", "SBFTargetDialog.title");
        this.game = game;
        initialize();
    }

    public void setTarget(@Nullable InGameObject target) {
        this.target = target;
        update();
    }

    public void setTarget(@Nullable InGameObject target, @Nullable SBFToHitData data) {
        this.target = target;
        toHitInformation.setText(data.toString());
        update();
    }

    private void update() {
        if (target == null) {
            targetDisplay.setText(NO_TARGET);
        } else {
            String tooltip = "<HTML><HEAD>" +
                    SBFInGameObjectTooltip.styles() +
                    "</HEAD><BODY>" +
                    SBFInGameObjectTooltip.getTooltip(target, game) +
                    "</BODY></HTML>";
            targetDisplay.setText(tooltip);
        }
        pack();
    }

    @Override
    protected Container createCenterPane() {
        Box box = Box.createVerticalBox();
        box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        box.add(header);
        box.add(targetDisplay);
        box.add(toHitInformation);
        return box;
    }
}
