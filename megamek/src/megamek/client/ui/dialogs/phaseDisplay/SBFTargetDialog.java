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

import java.awt.Container;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionListener;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.tooltip.SBFInGameObjectTooltip;
import megamek.client.ui.dialogs.abstractDialogs.AbstractDialog;
import megamek.client.ui.sbf.SBFTargetingToHitDisplay;
import megamek.client.ui.sbf.SBFUnitAttackSelector;
import megamek.client.ui.util.UIUtil;
import megamek.common.game.InGameObject;
import megamek.common.annotations.Nullable;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.strategicBattleSystems.SBFToHitData;

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

    public void setContent(@Nullable InGameObject attacker, @Nullable InGameObject target,
          @Nullable SBFToHitData data) {
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
