package megamek.client.ui.swing.dialog;

import java.awt.Container;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.swing.BoardEditor;
import megamek.client.ui.swing.MMToggleButton;
import static megamek.client.ui.swing.util.UIUtil.*;
import static megamek.client.ui.Messages.*;

public final class FloodDialog extends AbstractButtonDialog {

    private BoardEditor.EditorTextField txtLevelChange = new BoardEditor.EditorTextField("0", 5, -5, 15);
    private JLabel labInfo = new JLabel(scaleStringForGUI("<CENTER>" + getString("FloodDialog.info")), SwingConstants.CENTER);
    private JLabel labRemoveInfo = new JLabel(scaleStringForGUI("<CENTER>" + getString("FloodDialog.removeInfo")), SwingConstants.CENTER);
    private MMToggleButton butRemove = new MMToggleButton(scaleStringForGUI(getString("FloodDialog.removeButton")));

    /** Constructs a modal LevelChangeDialog with frame as parent. */
    public FloodDialog(JFrame frame) {
        super(frame, "FloodDialog.name", "FloodDialog.title");
        initialize();
    }

    @Override
    protected Container createCenterPane() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setBorder(new EmptyBorder(10, 30, 10, 30));

        JPanel textFieldPanel = new FixedYPanel();
        textFieldPanel.add(txtLevelChange);
        
        JPanel toggleButtonPanel = new FixedYPanel();
        toggleButtonPanel.add(butRemove);
        
        labInfo.setAlignmentX(CENTER_ALIGNMENT);
        labRemoveInfo.setAlignmentX(CENTER_ALIGNMENT);
        
        result.add(Box.createVerticalGlue());
        result.add(labInfo);
        result.add(Box.createVerticalStrut(5));
        result.add(textFieldPanel);
        result.add(Box.createVerticalStrut(5));
        result.add(labRemoveInfo);
        result.add(Box.createVerticalStrut(5));
        result.add(toggleButtonPanel);
        result.add(Box.createVerticalGlue());
        
        return result;
    }
    
    /** Returns the level change entered by the user or 0, if it cannot be parsed. */
    public int getLevelChange() {
        try {
            return Integer.parseInt(txtLevelChange.getText());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
    
    /** Returns if all newly submerged hexes should have their terrain removed. */
    public boolean getRemoveTerrain() {
        return butRemove.isSelected();
    }

}
