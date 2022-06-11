/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Collection;

import javax.swing.*;

import megamek.client.ui.dialogs.ASConversionInfoDialog;
import megamek.client.ui.swing.util.SpringUtilities;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.alphaStrike.ASConverter;

public class AlphaStrikeViewPanel extends JPanel {
    
    public static final int DEFAULT_HEIGHT = 600;
    public static final int COLS = 13;

    public AlphaStrikeViewPanel(Collection<Entity> entities, boolean hexMovement, boolean includePilot) {
        setLayout(new SpringLayout());
        addHeader(" Unit", JComponent.LEFT_ALIGNMENT);
        addHeader("Type");
        addHeader("SZ");
        addHeader("TMM");
        addHeader("MV");
        addHeader("Role");
        addHeader("Dmg S/M/L");
        addHeader("OV");
        addHeader("Arm/Str");
        addHeader("Th");
        addHeader("Skill");
        addHeader("PV");
        addHeader("Specials");
        
        int row = 1;
        for (Entity entity : entities) {
            boolean oddRow = (row++ % 2) == 1;
            var element2 = ASConverter.convert(entity, includePilot);
            addGridElement(entity.getShortName(), oddRow, JComponent.LEFT_ALIGNMENT);
            addGridElement(element2.getType().toString(), oddRow);
            addGridElement(element2.getSize() + "", oddRow);
            addGridElement(element2.getTMM() + "", oddRow);
            if (hexMovement) {
                addGridElement(element2.getPrimaryMovementValue() / 2 + "", oddRow);
            } else {
                addGridElement(element2.getMovementAsString(), oddRow);
            }
            addGridElement(UnitRoleHandler.getRoleFor(entity).toString(), oddRow);
            addGridElement(element2.getStandardDamage() + "", oddRow);
            addGridElement(element2.getOverheat() + "", oddRow);
            addGridElement(element2.getArmor() + " / " + element2.getStructure(), oddRow);
            addGridElement(element2.usesThreshold() ? element2.getThreshold() + "" : " ", oddRow);
            addGridElement(element2.getSkill() + "", oddRow);
            addGridElement(element2.getPointValue() + "", oddRow);
            addGridElement(element2.getSpecialsString(), oddRow, JComponent.LEFT_ALIGNMENT);

        }

        SpringUtilities.makeCompactGrid(this, row, COLS, 5, 5, 1, 5);
    }

    private void addConversionInfo(boolean coloredBG) {
        var panel = new UIUtil.FixedYPanel();
        if (coloredBG) {
            panel.setBackground(UIUtil.alternateTableBGColor());
        }
        JButton button = new JButton("?");
//        button.addActionListener(e -> new ASConversionInfoDialog(null, ));
        panel.add(button);
        add(panel);
    }
    
    private void addGridElement(String text, boolean coloredBG) {
        var panel = new UIUtil.FixedYPanel();
        if (coloredBG) {
            panel.setBackground(UIUtil.alternateTableBGColor());
        }
        panel.add(new JLabel(text));
        add(panel);
    }
    
    private void addGridElement(String text, boolean coloredBG, float alignment) {
        var panel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        if (coloredBG) {
            panel.setBackground(UIUtil.alternateTableBGColor());
        }
        var textLabel = new JLabel(text);
        panel.add(textLabel);
        add(panel);
    }
    
    private void addHeader(String text, float alignment) {
        var panel = new UIUtil.FixedYPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        var textLabel = new JLabel(text);
        textLabel.setAlignmentX(alignment);
        textLabel.setFont(getFont().deriveFont(Font.BOLD));
        textLabel.setForeground(UIUtil.uiLightBlue());
        panel.add(textLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(new JSeparator());
        add(panel);
    }
    
    private void addHeader(String text) {
        addHeader(text, JComponent.CENTER_ALIGNMENT);
    }
   

}
