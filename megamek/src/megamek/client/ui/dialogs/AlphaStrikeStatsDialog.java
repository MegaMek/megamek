package megamek.client.ui.dialogs;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.baseComponents.AbstractDialog;
import megamek.client.ui.swing.AlphaStrikeViewPanel;
import megamek.client.ui.swing.MMToggleButton;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;

public class AlphaStrikeStatsDialog extends AbstractDialog {
    
    private final Collection<Entity> entities;
    private final MMToggleButton moveToggle = new MMToggleButton("Movement in Hexes");
    private final MMToggleButton pilotToggle = new MMToggleButton("Include Pilot");
    private final JButton clipBoardButton = new JButton("Copy to Clipboard");
    private JScrollPane scrollPane = new JScrollPane();

    public AlphaStrikeStatsDialog(JFrame frame, Collection<Entity> en) {
        super(frame, "AlphaStrikeStatsDialog", "Ok.text");
        entities = en;
        initialize();
        UIUtil.adjustDialog(this);
    }

    @Override
    protected Container createCenterPane() {
        var result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        
        var optionsPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.setBorder(new EmptyBorder(15, 30, 10, 0));
        optionsPanel.add(moveToggle);
        optionsPanel.add(pilotToggle);
        optionsPanel.add(clipBoardButton);
        moveToggle.addActionListener(e -> setupTable());
        pilotToggle.addActionListener(e -> setupTable());
        clipBoardButton.addActionListener(e -> copyToClipboard());
        
        setupTable();
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        result.add(optionsPanel);
        result.add(scrollPane);
        return result;
    }
    
    private void setupTable() {
        scrollPane.getViewport().setView(new AlphaStrikeViewPanel(entities, moveToggle.isSelected()));
        UIUtil.adjustDialog(this);
    }
    
    private void copyToClipboard() {
        StringSelection stringSelection = new StringSelection(clipboardString(entities));
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }
    
    /** Returns a String representing the entities to export to the clipboard. */
    private String clipboardString(Collection<Entity> entities) {
        StringBuilder result = new StringBuilder();
        result.append("Unit");
        result.append("Type");
        result.append("SZ");
        result.append("TMM");
        result.append("MV");
        result.append("Role");
        result.append("Dmg S/M/L");
        result.append("OV");
        result.append("Arm/Str");
        result.append("PV");
        result.append("Specials");
        
        for (Entity entity: entities) {
                var element = new AlphaStrikeElement(entity);
                result.append(entity.getShortName());
                result.append(element.getUnitType().toString());
                result.append(element.getSize() + "");
                result.append(element.getTargetMoveModifier()+"");
                if (moveToggle.isSelected()) {
                    result.append(""+element.getPrimaryMovementValue()/2);
                } else {
                    result.append(element.getMovementAsString());
                }
                result.append(UnitRoleHandler.getRoleFor(entity).toString());
//                addGridElement(element.getDamage(0)+"/"+element.getDamage(1)+"/"+element.getDamage(2));
                result.append(element.getASDamageString(0));
                result.append(element.calcHeatCapacity(entity)+"");
                result.append(element.getFinalArmor() + "/" + element.getStructure());
                result.append(element.getFinalPoints()+"");
                result.append("?");
            Locale cl = Locale.getDefault();
            NumberFormat numberFormatter = NumberFormat.getNumberInstance(cl);
//            result.append(numberFormatter.format(entity.getWeight())).append("\t");
//            // Pilot name
//            result.append(entity.getCrew().getName()).append("\t");
//            // Crew Skill with text
//            result.append(CrewSkillSummaryUtil.getSkillNames(entity)).append(": ")
//                    .append(entity.getCrew().getSkillsAsString(false)).append("\t");
//            // BV without C3 but with pilot (as that gets exported too)
//            result.append(entity.calculateBattleValue(true, false)).append("\t");
            result.append("\n");
        }
        return result.toString();
    }

}
