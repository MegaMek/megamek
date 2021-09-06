package megamek.client.ui.swing;

import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.*;

import megamek.client.ui.swing.util.SpringUtilities;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;

public class AlphaStrikeViewPanel extends JPanel {
    
    public static final int DEFAULT_WIDTH = 360;
    public static final int DEFAULT_HEIGHT = 600;
    public static final int COLS = 12;

    public AlphaStrikeViewPanel(Collection<Entity> entities, boolean hexMovement) {
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
        addHeader("PV");
        addHeader("Specials");
        
        int row = 1;
        for (Entity entity : entities) {
            boolean oddRow = (row++ % 2) == 1;
            var element2 = AlphaStrikeConverter.convertToAlphaStrike(entity);
            addGridElement(entity.getShortName(), oddRow, JComponent.LEFT_ALIGNMENT);
            addGridElement(element2.getUnitType().toString(), oddRow);
            addGridElement(element2.getSize() + "", oddRow);
            addGridElement(element2.getTMM()+"", oddRow);
            if (hexMovement) {
                addGridElement(""+element2.getPrimaryMovementValue()/2, oddRow);
            } else {
                addGridElement(element2.getMovementAsString(), oddRow);
            }
            addGridElement(UnitRoleHandler.getRoleFor(entity).toString(), oddRow);
            addGridElement(element2.getStandardDamage() + "", oddRow);
            addGridElement(element2.getOverheat() + "", oddRow);
            addGridElement(element2.getFinalArmor() + " / " + element2.getStructure(), oddRow);
            addGridElement(element2.usesThreshold() ? element2.getFinalThreshold() + "" : " ", oddRow);
            addGridElement(element2.getFinalPoints() + "", oddRow);
            addGridElement(element2.getSpecialsString(), oddRow, JComponent.LEFT_ALIGNMENT);
        }

        SpringUtilities.makeCompactGrid(this, row, COLS, 5, 5, 1, 5);
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
