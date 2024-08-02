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
import megamek.client.ui.sbf.SBFTargetingToHitDisplay;
import megamek.client.ui.sbf.SBFUnitAttackSelector;
import megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.InGameObject;
import megamek.common.annotations.Nullable;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.strategicBattleSystems.SBFToHitData;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

public class SBFTargetDialog extends AbstractDialog implements IPreferenceChangeListener {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final String NO_TARGET = "No Target";

    private final Box attackerBox = Box.createVerticalBox();
    private final JLabel attackingLabel = new JLabel("attacking:", JLabel.CENTER);
    private final JLabel resultLabel = new JLabel("result:", JLabel.CENTER);
    private final JLabel attackerDisplay = new JLabel(noTargetLabel());
    private final JLabel targetDisplay = new JLabel(noTargetLabel());
    private final SBFGame game;
    private final SBFUnitAttackSelector unitAttackSelector;
    private final SBFTargetingToHitDisplay toHitDisplay = new SBFTargetingToHitDisplay();

    private InGameObject target;
    private InGameObject attacker;

    public SBFTargetDialog(JFrame parent, SBFGame game, ListSelectionListener listener) {
        super(parent, "SBFTargetDialog", "SBFTargetDialog.title");
        this.game = game;
        // Unit selection events are passed up to the FiringDisplay as listener
        unitAttackSelector = new SBFUnitAttackSelector(listener);
        GUIP.addPreferenceChangeListener(this);
        initialize();
    }

    public void setContent(@Nullable InGameObject attacker, @Nullable InGameObject target, @Nullable SBFToHitData data) {
        if (this.attacker != attacker) {
            this.attacker = attacker;
            unitAttackSelector.setFormation((SBFFormation) attacker);
        }
        if (this.target != target) {
            this.target = target;
        }
        toHitDisplay.showToHit(data);
        update();
    }

    public void setTarget(@Nullable InGameObject target) {
        this.target = target;
        update();
    }

    public void setAttacker(@Nullable InGameObject attacker) {
        this.attacker = attacker;
        unitAttackSelector.setFormation((SBFFormation) attacker);
        update();
    }

    public void setToHitData(@Nullable SBFToHitData data) {
        toHitDisplay.showToHit(data);
        update();
    }

    private void update() {
        if (attacker == null) {
            attackerDisplay.setText(noTargetLabel());
        } else {
            String tooltip = "<HTML><HEAD><STYLE>" +
                    SBFInGameObjectTooltip.styles() +
                    "</STYLE></HEAD><BODY>" +
                    SBFInGameObjectTooltip.getBaseTooltip(attacker, game) +
                    "</BODY></HTML>";
            attackerDisplay.setText(tooltip);
        }
        attackerBox.setBorder(new LineBorder(SBFInGameObjectTooltip.ownerColor(attacker, game), 2));

        if (target == null) {
            targetDisplay.setText(noTargetLabel());
        } else {
            String tooltip = "<HTML><HEAD><STYLE>" +
                    SBFInGameObjectTooltip.styles() +
                    "</STYLE></HEAD><BODY>" +
                    SBFInGameObjectTooltip.getTooltip(target, game) +
                    "</BODY></HTML>";
            targetDisplay.setText(tooltip);
        }

        attackingLabel.setText("<HTML><HEAD><STYLE>" + labelStyle() + "</STYLE></HEAD><BODY>"
                + UIUtil.divCSS("label", "Attacking") + "</BODY></HTML>");

        resultLabel.setText("<HTML><HEAD><STYLE>" + labelStyle() + "</STYLE></HEAD><BODY>"
                + UIUtil.divCSS("label", "Attack Result:") + "</BODY></HTML>");
        pack();
    }

    @Override
    protected Container createCenterPane() {
        attackerBox.add(attackerDisplay);
        attackerBox.add(unitAttackSelector.getComponent());
        attackerBox.setBorder(new LineBorder(SBFInGameObjectTooltip.ownerColor(attacker, game), 2));

        Box box = Box.createVerticalBox();
        box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        box.add(attackerBox);
        box.add(attackingLabel);
        box.add(targetDisplay);
        box.add(resultLabel);
        box.add(toHitDisplay.getComponent());
        return box;
    }

    private String noTargetLabel() {
        int width = UIUtil.scaleForGUI(SBFInGameObjectTooltip.TOOLTIP_BASE_WIDTH);
        int fontSize = (int) (1.4 * UIUtil.scaleForGUI(UIUtil.FONT_SCALE1));
        return "<HTML><BODY><div style='width:" + width + "; padding:50 10; border:2; margin: 5 0; " +
                "border-style:solid; border-color: #888; font-family:Noto Sans; font-size:" + fontSize + "; '>"
                + NO_TARGET + "</div></BODY></HTML>";
    }

    private String labelStyle() {
        int fontSize = UIUtil.scaleForGUI(UIUtil.FONT_SCALE1);
        return ".label { font-family:Noto Sans; font-size:" + fontSize + "; text-align:center; padding: 5; " +
                "background: #888; color: #000; }";
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        update();
    }

    public void die() {
        GUIP.removePreferenceChangeListener(this);
    }
}
