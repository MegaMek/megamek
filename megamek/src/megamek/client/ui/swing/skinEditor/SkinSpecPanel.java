/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2006 Ben Mazur (bmazur@sev.org)
 * Copyright © 2015 Nicholas Walczak (walczak@cs.umn.edu)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client.ui.swing.skinEditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.common.Configuration;

/**
 * Panel with elements for viewing and adjusting a specific SkinSpecification.
 * 
 * @author arlith
 */
public class SkinSpecPanel extends JPanel implements ListSelectionListener,
        ActionListener {

    /**
     * A UI widget for displaying information related to a border widget (image
     * path and whether the image is tiled or not).  Also supports a flag to
     * determine if the image should be allowed to tile (ie, corners should
     * never be tiled)
     *  
     * @author arlith
     *
     */
    private class BorderElement extends JPanel {
        
        /**
         * 
         */
        private static final long serialVersionUID = -2004313765932049794L;

        List<JButton> pathLbl = new ArrayList<>();
        
        List<JTextField> path = new ArrayList<>();
        
        List<JCheckBox> tiled = new ArrayList<>();
        
        List<JButton> removeButtons = new ArrayList<>();
        
        JButton addButton = new JButton(Messages.getString("SkinEditor.Add")); //$NON-NLS-1$        
                
        boolean displayTiled = false;
        
        BorderElement(String elementName, String imgPath) {
            super(new GridBagLayout());
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEmptyBorder(), elementName, 
                    TitledBorder.LEFT, TitledBorder.TOP));
            
            displayTiled = false;
            
            JButton newPathLbl = new JButton(
                    Messages.getString("SkinEditor.Path")); //$NON-NLS-1$
            newPathLbl.setMargin(new Insets(1, 1, 1, 1));
            newPathLbl.setContentAreaFilled(false);
            newPathLbl.setToolTipText(Messages
                    .getString("SkinEditor.PathToolTip", //$NON-NLS-1$
                            new Object[]{Configuration.widgetsDir().getPath()}));
            pathLbl.add(newPathLbl);
            path.add(new JTextField(imgPath, 22));
            tiled.add(new JCheckBox(Messages.getString("SkinEditor.Tiled"), //$NON-NLS-1$
                    false));
            removeButtons.add(new JButton());

            layoutPanel();
        }
        
        BorderElement(String elementName, List<String> imgPath,
                List<Boolean> isTiled) {
            super(new GridBagLayout());
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEmptyBorder(), elementName, 
                    TitledBorder.LEFT, TitledBorder.TOP));
            
            displayTiled = true;
            assert(imgPath.size() == isTiled.size());
            
            for (int i = 0; i < imgPath.size(); i++) {
                JButton newPathLbl = new JButton(
                        Messages.getString("SkinEditor.Path")); //$NON-NLS-1$
                newPathLbl.setMargin(new Insets(1, 1, 1, 1));
                newPathLbl.setContentAreaFilled(false);
                newPathLbl.setToolTipText(Messages
                        .getString("SkinEditor.PathToolTip")); //$NON-NLS-1$
                pathLbl.add(newPathLbl); //$NON-NLS-1$
                path.add(new JTextField(imgPath.get(i), 22));
                JCheckBox newTiled = new JCheckBox(Messages.getString("SkinEditor.Tiled"), //$NON-NLS-1$
                        isTiled.get(i));
                newTiled.setToolTipText(Messages.getString("SkinEditor.TiledToolTip")); //$NON-NLS-1$
                tiled.add(newTiled);
                JButton newRemoveButton = new JButton(
                        Messages.getString("SkinEditor.RemoveButton")); //$NON-NLS-1$
                newRemoveButton.setToolTipText(Messages
                        .getString("SkinEditor.RemoveButtonToolTip")); //$NON-NLS-1$
                newRemoveButton.setMargin(new Insets(0, 0, 1, 0));
                newRemoveButton.setPreferredSize(new Dimension(14, 14));
                newRemoveButton.setMaximumSize(new Dimension(14, 14));
                removeButtons.add(newRemoveButton);
            }
            addButton.setToolTipText(Messages
                    .getString("SkinEditor.AddButtonToolTip")); //$NON-NLS-1$
            addButton.setMargin(new Insets(1,1,1,1));
            addButton.setMaximumSize(new Dimension(40, 14));
            addButton.setPreferredSize(new Dimension(40, 14));
            layoutPanel();
        }
        
        
        void layoutPanel() {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(1,1,1,1);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = gbc.gridy = 0;
            if (displayTiled) {
                add(addButton, gbc);
                gbc.gridy++;
            }
            for (int i = 0; i < path.size(); i++) {
                gbc.gridx = 0; gbc.gridwidth = 1;
                gbc.weightx = 0;
                gbc.fill = GridBagConstraints.NONE;
                add(pathLbl.get(i), gbc);
                
                gbc.gridx++;
                gbc.weightx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                if (!displayTiled) {
                    gbc.gridwidth = 2;
                }
                add(path.get(i), gbc);

                if (displayTiled) {
                    gbc.fill = GridBagConstraints.NONE;
                    gbc.gridwidth = 1;
                    gbc.weightx = 0;
                    gbc.gridx++;
                    add(tiled.get(i), gbc);
                    gbc.gridx++;
                    add(removeButtons.get(i), gbc);
                }
                gbc.gridy++;
            }
        }
        
        @Override
        public void setEnabled(boolean en) {
            super.setEnabled(en);
            addButton.setEnabled(en);
            for (int i = 0; i < path.size(); i++) {
                pathLbl.get(i).setEnabled(en);
                path.get(i).setEnabled(en);
                tiled.get(i).setEnabled(en);
                removeButtons.get(i).setEnabled(en);
            }
        }
    }
    
    /**
     * Essentially the same thing as the BorderElement, but used for backgrounds
     * where each background image shares the same shouldTile state.
     * 
     * @author arlith
     *
     */
    private class BackgroundElement extends BorderElement {

        /**
         * 
         */
        private static final long serialVersionUID = 3448867645483831732L;

        BackgroundElement(List<String> imgPath,
                List<Boolean> isTiled) {
            super(Messages.getString("SkinEditor.Background"), //$NON-NLS-1$ 
                    imgPath, isTiled);
        }
        
    }
    
    /**
     * 
     */
    private static final long serialVersionUID = -37452332974426228L;
    
    BorderElement tlCorner, trCorner, blCorner, brCorner;
    
    BorderElement topEdge, bottomEdge, leftEdge, rightEdge;
    
    BackgroundElement background;
    
    JCheckBox showScrollBars = new JCheckBox(
            Messages.getString("SkinEditor.ShowScrollBars")); //$NON-NLS-1$
    
    JButton colorButton = new JButton();
    
    JLabel colorLbl = new JLabel(Messages.getString("SkinEditor.Color")); //$NON-NLS-1$
 
    /**
     * 
     */
    public SkinSpecPanel() {
        super(new GridBagLayout());
        colorButton.setMaximumSize(new Dimension(14,14));
        colorButton.setPreferredSize(new Dimension(14,14));
    }
    
    /**
     * Add this SkinSpecEditor as a listener to all components.
     */
    private void addListeners() {

    }
    
    /**
     * Remove thsi SkinSpecEditor as a listener from all components.
     */
    private void removeListeners() {

    }
    
    /**
     * Update the editing panel with the currently selected SkinSpecification.
     */
    public void setupSkinEditPanel(SkinSpecification skinSpec) {
        removeListeners();
        removeAll();
        
        boolean enableBorders = !skinSpec.noBorder;

        
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0,0,4,0);
        
        JPanel borderPanel = new JPanel(new GridBagLayout());
        //borderPanel.setLayout(new BoxLayout(borderPanel, BoxLayout.Y_AXIS));
        borderPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                Messages.getString("SkinEditor.Borders"), TitledBorder.TOP, //$NON-NLS-1$
                TitledBorder.DEFAULT_POSITION));
        borderPanel.setEnabled(enableBorders);
        
        // Top Left Corner
        tlCorner = new BorderElement(
                Messages.getString("SkinEditor.TLC"), //$NON-NLS-1$
                skinSpec.tl_corner);
        tlCorner.setEnabled(enableBorders);
        borderPanel.add(tlCorner, gbc);
        gbc.gridx++;
        // Top Right Corner
        trCorner = new BorderElement(
                Messages.getString("SkinEditor.TRC"), //$NON-NLS-1$
                skinSpec.tr_corner);
        trCorner.setEnabled(enableBorders);
        borderPanel.add(trCorner, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        // Bottom Left Corner
        blCorner = new BorderElement(
                Messages.getString("SkinEditor.BLC"),  //$NON-NLS-1$
                skinSpec.bl_corner);
        blCorner.setEnabled(enableBorders);
        borderPanel.add(blCorner, gbc);
        gbc.gridx++;
        // Bottom Right Corner
        brCorner = new BorderElement(
                Messages.getString("SkinEditor.BRC"),  //$NON-NLS-1$
                skinSpec.br_corner);
        brCorner.setEnabled(enableBorders);
        borderPanel.add(brCorner, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        
        
        // Top Edge
        topEdge = new BorderElement(
                Messages.getString("SkinEditor.TopEdge"), //$NON-NLS-1$
                skinSpec.topEdge, skinSpec.topShouldTile);
        topEdge.setEnabled(enableBorders);
        borderPanel.add(topEdge, gbc);
        gbc.gridx++;
        
        // Bottom Edge
        bottomEdge = new BorderElement(
                Messages.getString("SkinEditor.BottomEdge"), //$NON-NLS-1$
                skinSpec.bottomEdge, skinSpec.bottomShouldTile);
        bottomEdge.setEnabled(enableBorders);
        borderPanel.add(bottomEdge, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        // Left Edge
        leftEdge = new BorderElement(
                Messages.getString("SkinEditor.LeftEdge"), //$NON-NLS-1$
                skinSpec.leftEdge, skinSpec.leftShouldTile);
        leftEdge.setEnabled(enableBorders);
        borderPanel.add(leftEdge, gbc);
        gbc.gridx++;

        // Right Edge
        rightEdge = new BorderElement(
                Messages.getString("SkinEditor.RightEdge"),  //$NON-NLS-1$
                skinSpec.rightEdge, skinSpec.rightShouldTile);
        rightEdge.setEnabled(enableBorders);
        borderPanel.add(rightEdge, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        
        
        gbc.gridx = gbc.gridy = 0;
        add(borderPanel, gbc);

        background = new BackgroundElement(skinSpec.backgrounds,
                Collections.nCopies(skinSpec.backgrounds.size(),
                        skinSpec.tileBackground));

        gbc.gridy++;
        add(background, gbc);
        
        JPanel misc = new JPanel(new GridBagLayout());
        gbc.gridy++;
        add(misc, gbc);
        
        gbc.gridx = gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        misc.add(showScrollBars);
        
        JPanel glue = new JPanel();
        glue.add(colorLbl);
        glue.add(colorButton);
        gbc.gridy++;
        misc.add(glue, gbc);
        
        colorButton.setForeground(skinSpec.fontColor);
        colorButton.setBackground(skinSpec.fontColor);
        
        revalidate();
        addListeners();
    }   


    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {

        
    }

    /**
     * Override the base setEnabled to also set the state of all members.
     */
    @Override
    public void setEnabled(boolean enabled) {
        // Corners
        tlCorner.setEnabled(enabled);
        trCorner.setEnabled(enabled);
        blCorner.setEnabled(enabled);
        brCorner.setEnabled(enabled);
        
        // Edges
        topEdge.setEnabled(enabled);
        bottomEdge.setEnabled(enabled);
        leftEdge.setEnabled(enabled);
        rightEdge.setEnabled(enabled);
    }

}
