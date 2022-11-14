/*
 * Copyright (c) 2000-2003 Ben Mazur (bmazur@sev.org)
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

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.*;
import megamek.common.preference.*;

/**
 * This is a parent class for the button display for each phase.  Every phase 
 * has a panel of control buttons along with a Done button. Each button
 * correspondes to a command that can be carried out in the current phase.  
 * This class formats the button panel, the done button, and a status display area. 
 * Control buttons are grouped and the groups can be cycled through.
 */
public abstract class StatusBarPhaseDisplay extends AbstractPhaseDisplay
        implements ActionListener, MouseListener, KeyListener, IPreferenceChangeListener {
    
    protected static final Dimension MIN_BUTTON_SIZE = new Dimension(32, 32);
    protected static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final int BUTTON_ROWS = 2;

    /**
     * Interface that defines what a command for a phase is.
     * @author arlith
     */
    public interface PhaseCommand {
        public String getCmd();
        public int getPriority();
        public void setPriority(int p);
    }
    
    /**
     * Comparator for comparing the priority of two commands, used to determine
     * button order.
     * @author arlith
     */
    public static class CommandComparator implements Comparator<PhaseCommand>
    {
        @Override
        public int compare(PhaseCommand c1, PhaseCommand c2) {
            return c1.getPriority() - c2.getPriority();            
        }
    }
    
    private JLabel labStatus;
    protected JPanel panStatus = new JPanel();
    protected JPanel panButtons = new JPanel();  
    
    /** The button group that is currently displayed */
    protected int currentButtonGroup = 0;
    
    /** The number of button groups there are, needs to be computed in a child class. */
    protected int numButtonGroups;
    
    protected int buttonsPerRow = GUIP.getInt(GUIPreferences.ADVANCED_BUTTONS_PER_ROW);
    protected int buttonsPerGroup = BUTTON_ROWS * buttonsPerRow;
    

    protected StatusBarPhaseDisplay(ClientGUI cg) {
        super(cg);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearButton");
        getActionMap().put("clearButton", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isIgnoringEvents()) {
                    return;
                }
                if (clientgui.getBoardView().getChatterBoxActive()) {
                    clientgui.getBoardView().setChatterBoxActive(false);
                    clientgui.cb2.clearMessage();
                } else if (clientgui.getClient().isMyTurn() || (e.getSource() instanceof MovementDisplay)) {
                    // Users can draw movement envelope during the movement phase 
                    // even if it's not their turn, so we always want to be able
                    // to clear. MovementDisplay.clear() can handle this case
                    clear();
                }
            }
        });
        
        panButtons.setLayout(new BoxLayout(panButtons, BoxLayout.LINE_AXIS));
        panButtons.setOpaque(false);        
        panButtons.addKeyListener(this);
        panStatus.setOpaque(false);
        panStatus.addKeyListener(this);
        
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(panButtons);
        add(panStatus);
        
        GUIP.addPreferenceChangeListener(this);
        ToolTipManager.sharedInstance().registerComponent(this);
    }
    
    
    /** Returns the list of buttons that should be displayed. */
    protected abstract ArrayList<MegamekButton> getButtonList();

    /**
     * Adds buttons to the button panel.  The buttons to be added are retrieved
     * with the <code>getButtonList()</code> method.  The number of buttons to
     * display is defined in <code>buttonsPerGroup</code> and which group of
     * buttons will be displayed is set by <code>currentButtonGroup</code>.
     */
    public void setupButtonPanel() {
        panButtons.removeAll();
        
        var buttonsPanel = new JPanel();
        buttonsPanel.setOpaque(false);
        buttonsPanel.setLayout(new GridLayout(BUTTON_ROWS, buttonsPerRow));
        List<MegamekButton> buttonList = getButtonList();
        
        // Unless it's the first group of buttons, skip any button group if all of its buttons are disabled
        if (currentButtonGroup > 0) {
            int index = currentButtonGroup * buttonsPerGroup;
            while (index < buttonList.size()) {
                if (buttonList.get(index) != null && buttonList.get(index).isEnabled()) {
                    currentButtonGroup = index / buttonsPerGroup;
                    break;
                }
                index++;
            }
            if (index >= buttonList.size()) {
                // Reached the end of the button list without finding an enabled button: Show the first button group
                currentButtonGroup = 0;
            }
        }
        
        int startIndex = currentButtonGroup * buttonsPerGroup;
        int endIndex = startIndex + buttonsPerGroup - 1;
        for (int index = startIndex; index <= endIndex; index++) {
            if ((index < buttonList.size()) && buttonList.get(index) != null) {
                MegamekButton button = buttonList.get(index);
                button.setPreferredSize(MIN_BUTTON_SIZE);
                buttonsPanel.add(button);
                ToolTipManager.sharedInstance().registerComponent(button);
            } else {
                buttonsPanel.add(Box.createHorizontalGlue());
            }
        }         
           
        var donePanel = new UIUtil.FixedXPanel();
        donePanel.setOpaque(false);
        donePanel.add(butDone);
        butDone.setPreferredSize(new Dimension(DONE_BUTTON_WIDTH, MIN_BUTTON_SIZE.height * 2));
                
        panButtons.add(buttonsPanel);
        panButtons.add(donePanel);
        adaptToGUIScale();
        panButtons.validate();
        panButtons.repaint();
    }
    
    /** Clears the actions of this phase. */
    public abstract void clear();
    
    /** Sets up the status bar. It usually displays info on the current phase and if it's the local player's turn. */
    protected void setupStatusBar(String statusInfo) {
        SkinSpecification pdSkinSpec = SkinXMLHandler.getSkin(SkinSpecification.UIComponents.PhaseDisplay.getComp());
        labStatus = new JLabel(statusInfo, SwingConstants.CENTER);
        labStatus.setForeground(pdSkinSpec.fontColors.get(0));
        panStatus.add(labStatus);
    }

    protected void setStatusBarText(String text) {
        labStatus.setText(text);
    }

    private void adaptToGUIScale() {
        UIUtil.scaleComp(panButtons, UIUtil.FONT_SCALE1);
        UIUtil.scaleComp(panStatus, UIUtil.FONT_SCALE2);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.ADVANCED_BUTTONS_PER_ROW)) {
            buttonsPerRow = GUIP.getInt(GUIPreferences.ADVANCED_BUTTONS_PER_ROW);
            buttonsPerGroup = 2 * buttonsPerRow;
            setupButtonPanel();
        } else if (e.getName().equals(GUIPreferences.GUI_SCALE)) {
            adaptToGUIScale();
        }
    }

    @Override
    public void keyPressed(KeyEvent evt) { }

    @Override
    public void keyReleased(KeyEvent evt) { }

    @Override
    public void keyTyped(KeyEvent evt) { }

    @Override
    public void actionPerformed(ActionEvent e) { }

    @Override
    public void mouseClicked(MouseEvent e) { }

    @Override
    public void mousePressed(MouseEvent e) { }

    @Override
    public void mouseReleased(MouseEvent e) { }

    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) { }
}