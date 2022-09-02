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

import megamek.client.ui.dialogs.ASConversionInfoDialog;
import megamek.client.ui.swing.calculationReport.FlexibleCalculationReport;
import megamek.client.ui.swing.util.SpringUtilities;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.alphaStrike.AlphaStrikeElement;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * This panel shows a table of AlphaStrike stats for list of AlphaStrike elements.
 * When calling this panel with a list of Entities, these will be converted and conversion
 * reports are offered as part of the table. When calling this panel with a list of
 * AlphaStrikeElements, conversion reports are unavailable.
 */
public class AlphaStrikeStatsTablePanel extends JPanel {
    
    private int columns;
    private final List<AlphaStrikeElement> elementList = new ArrayList<>();
    private final JFrame frame;
    private final Map<AlphaStrikeElement, FlexibleCalculationReport> reports = new HashMap<>();

    /**
     * Returns a panel with a table of AlphaStrike stats for the given entities after AlphaStrike
     * conversion. The table will show buttons for calling up conversion reports. The given entity
     * collection will be filtered for convertible units.
     *
     * @param frame The parent frame (important for giving a parent to conversion report dialogs)
     * @param entities a collection of entities to convert and show
     * @param includePilot When true, the entity crew will be factored into the AS SKILL value
     */
    public AlphaStrikeStatsTablePanel(JFrame frame, Collection<Entity> entities, boolean includePilot) {
        for (Entity entity : entities) {
            if (ASConverter.canConvert(entity)) {
                FlexibleCalculationReport report = new FlexibleCalculationReport();
                AlphaStrikeElement element = ASConverter.convert(entity, includePilot, report);
                elementList.add(element);
                reports.put(element, report);
            }
        }
        this.frame = frame;
        initialize();
    }

    /**
     * Returns a panel with a table of AlphaStrike stats for the given elements.
     *
     * @param elements a collection of AlphaStrike elements to show
     */
    public AlphaStrikeStatsTablePanel(Collection<AlphaStrikeElement> elements) {
        elementList.addAll(elements);
        this.frame = null;
        initialize();
    }

    private void initialize() {
        setLayout(new SpringLayout());
        addHeader(" Unit", JComponent.LEFT_ALIGNMENT);
        addHeader("Type");
        addHeader("SZ");
        addHeader("TMM");
        addHeader("MV (THR)");
        addHeader("Role");
        addHeader("Dmg S/M/L");
        addHeader("OV");
        addHeader("Arm");
        addHeader("Str");
        addHeader("Th");
        addHeader("Skill");
        addHeader("PV");
        addHeader("Specials");
        addHeader("Conversion");
        
        int row = 1;
        for (AlphaStrikeElement element : elementList) {
            if (element != null) {
                boolean oddRow = (row++ % 2) == 1;
                addGridElementLeftAlign(element.getName(), oddRow);
                addGridElement(element.getASUnitType() + "", oddRow);
                addGridElement(element.getSize() + "", oddRow);
                addGridElement(element.isAerospace() ? "" : element.getTMM() + "", oddRow);
                addGridElement(element.getMovementAsString(), oddRow);
                addGridElement(element.getRole() + "", oddRow);
                addGridElement(element.usesArcs() ? "" : element.getStandardDamage() + "", oddRow);
                addGridElement(element.usesOV() ? element.getOV() + "" : "", oddRow);
                addGridElement(element.getFullArmor() + "", oddRow);
                addGridElement(element.getFullStructure() + "", oddRow);
                addGridElement(element.usesThreshold() ? element.getThreshold() + "" : " ", oddRow);
                addGridElement(element.getSkill() + "", oddRow);
                addGridElement(element.getPointValue() + "", oddRow);
                if (element.usesArcs()) {
                    addGridElementLeftAlign(getArcedSpecials(element), oddRow);
                } else {
                    addGridElementLeftAlign(element.getSpecialsDisplayString(element), oddRow);
                }
                addConversionInfo(oddRow, reports.get(element), element, frame);
            }
        }

        SpringUtilities.makeCompactGrid(this, row, columns, 5, 5, 1, 5);
    }

    private void addConversionInfo(boolean coloredBG, FlexibleCalculationReport conversionReport,
                                   AlphaStrikeElement element, JFrame frame) {
        var panel = new UIUtil.FixedYPanel();
        if (coloredBG) {
            panel.setBackground(UIUtil.alternateTableBGColor());
        }
        JButton button = new JButton("?");
        button.setEnabled(conversionReport != null);
        button.addActionListener(e -> new ASConversionInfoDialog(frame, conversionReport, element, true).setVisible(true));
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
    
    private void addGridElementLeftAlign(String text, boolean coloredBG) {
        var panel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        if (coloredBG) {
            panel.setBackground(UIUtil.alternateTableBGColor());
        }
        var textLabel = new JLabel(text);
        panel.add(textLabel);
        add(panel);
    }
    
    private void addHeader(String text, float alignment) {
        columns++;
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
   
    private String getArcedSpecials(AlphaStrikeElement element) {
        return "<HTML>" + element.getSpecialsDisplayString(element) +
                "<BR>FRONT(" + element.getFrontArc().toString() + ")" +
                "<BR>LEFT(" + element.getLeftArc().toString() + ")" +
                "<BR>RIGHT(" + element.getRightArc().toString() + ")" +
                "<BR>REAR(" + element.getRearArc().toString() + ")";
    }
}
