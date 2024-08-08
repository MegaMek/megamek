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

package megamek.client.ui.sbf;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Compute;
import megamek.common.TargetRollModifier;
import megamek.common.strategicBattleSystems.SBFToHitData;

import javax.swing.*;

public class SBFTargetingToHitDisplay {

    private final JLabel formattedLabel = new JLabel();

    public void showToHit(SBFToHitData data) {
        StringBuilder result = new StringBuilder();
        result.append(UIUtil.spanCSS("label", "Chance to Hit: "));
        String cssClass = "autosuccess";
        if (data.cannotSucceed()) {
            cssClass = "impossible";
        } else if (data.needsRoll()) {
            cssClass = "roll";
        }
        String txt = String.format("%s (%2.0f%%)", data.getValueAsString(), Compute.oddsAbove(data.getValue()));
        result.append(UIUtil.spanCSS(cssClass, txt));
        result.append("<BR>");

        for (int index = 0; index < data.getModifiers().size(); index++) {
            TargetRollModifier modifier = data.getModifiers().get(index);
            String valueText = (index > 0 ? (modifier.getValue() >= 0 ? "+ " : "- ") : "") + Math.abs(modifier.getValue());
            cssClass = modifier.getValue() > 0 ? "penalty" : "bonus";
            result.append(UIUtil.spanCSS(cssClass, valueText));
            result.append(UIUtil.spanCSS("valuedeemph", " (" + modifier.getDesc() + ") "));
        }

        String text = "<HTML><HEAD><STYLE>" + styles() + "</STYLE></HEAD><BODY>" + result + "</BODY></HTML>";
        formattedLabel.setText(text);
    }

    public JComponent getComponent() {
        return formattedLabel;
    }

    private String styles() {
        float base = UIUtil.scaleForGUI(UIUtil.FONT_SCALE1);
        int labelSize = (int) (0.8 * base);
        int valueSize = (int) (1.1 * base);
        int nameSize = (int) (1.3 * base);

        return ".value { font-family:Exo; font-size:20; }" +
                ".impossible { font-family:Noto Sans; font-size:" + nameSize + "; color: #FAA }" +
                ".autosuccess { font-family:Noto Sans; font-size:" + nameSize + "; color: #AFA }" +
                ".roll { font-family:Exo; font-size:" + nameSize + "; }" +
                ".label { font-family:Noto Sans; font-size:" + labelSize + "; color:gray; }" +
                ".idnum { font-family:Exo; font-size:" + labelSize + "; color:gray; text-align:right; }" +
                ".valuecell { padding-right:10; font-family:Exo; font-size:" + valueSize + "; text-align: center; }" +
                ".armornodmg { font-family:Exo; font-size:" + valueSize + "; text-align: center; }" +
                ".penalty { font-family:Exo; font-size:" + valueSize + "; text-align: center; color: #FAA; }" +
                ".bonus { font-family:Exo; font-size:" + valueSize + "; text-align: center; color: #AFA }" +
                ".valuedeemph { font-family:Exo; font-size:" + labelSize + "; color:gray; }" +
                ".pvcell { font-family:Exo; font-size:" + nameSize + "; text-align: right; }" +
                ".speccell { font-family:Exo; font-size:" + labelSize + "; }";
    }
}
