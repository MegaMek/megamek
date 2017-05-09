/*
 *  This file is part of MegaMek
 * Copyright (C) 2017 - The MegaMek Team
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
package megamek.client.bot.ui.swing;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.PortraitChoiceDialog;
import megamek.common.Entity;
import megamek.common.EntitySelector;
import megamek.common.Infantry;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;

/**
 * 
 * 
 * @author Neoancient
 *
 */
public class CustomPilotView extends JPanel {
    
    /**
     * 
     */
    private static final long serialVersionUID = 345126674612500365L;

    private final Entity entity;

    private final JTextField fldName = new JTextField(20);
    private final PortraitChoiceDialog portraitDialog;
    private final JTextField fldNick = new JTextField(20);
    private final JTextField fldGunnery = new JTextField(3);
    private final JTextField fldGunneryL = new JTextField(3);
    private final JTextField fldGunneryM = new JTextField(3);
    private final JTextField fldGunneryB = new JTextField(3);
    private final JTextField fldPiloting = new JTextField(3);
    private final JTextField fldArtillery = new JTextField(3);
    private JTextField fldTough = new JTextField(3);
    
    private ArrayList<Entity> entityUnitNum = new ArrayList<Entity>();
    private JComboBox<String> choUnitNum = new JComboBox<String>();

    public CustomPilotView(ClientGUI clientgui, Entity entity, int slot, boolean editable) {
        this.entity = entity;
        
        setLayout(new GridBagLayout());

        JLabel label;
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(72, 72));
        button.setText(Messages.getString("CustomMechDialog.labPortrait"));
        button.setActionCommand("portrait"); //$NON-NLS-1$
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                portraitDialog.setVisible(true);
            }
        });
        
        portraitDialog = new PortraitChoiceDialog(clientgui.getFrame(),
                button);
        portraitDialog.setPilot(entity.getCrew());
        add(button, GBC.std().gridheight(2));

        button = new JButton(Messages.getString("CustomMechDialog.RandomName")); //$NON-NLS-1$
        button.addActionListener(e -> fldName.setText(clientgui.getClient().getRandomNameGenerator().generate()));
        add(button, GBC.eop());

        button = new JButton(Messages.getString("CustomMechDialog.RandomSkill")); //$NON-NLS-1$
        button.addActionListener(e -> {
            int[] skills = clientgui.getClient().getRandomSkillsGenerator().getRandomSkills(entity);
            fldGunnery.setText(Integer.toString(skills[0]));
            fldPiloting.setText(Integer.toString(skills[1]));
        });
        add(button, GBC.eop());

        label = new JLabel(Messages.getString("CustomMechDialog.labName"), SwingConstants.RIGHT); //$NON-NLS-1$
        add(label, GBC.std());
        add(fldName, GBC.eol());
        fldName.setText(entity.getCrew().getName(slot));

        label = new JLabel(Messages.getString("CustomMechDialog.labNick"), SwingConstants.RIGHT); //$NON-NLS-1$
        add(label, GBC.std());
        add(fldNick, GBC.eop());
        fldNick.setText(entity.getCrew().getNickname(slot));

        if (clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY)) {

            label = new JLabel(Messages.getString("CustomMechDialog.labGunneryL"), SwingConstants.RIGHT); //$NON-NLS-1$
            add(label, GBC.std());
            add(fldGunneryL, GBC.eol());

            label = new JLabel(Messages.getString("CustomMechDialog.labGunneryM"), SwingConstants.RIGHT); //$NON-NLS-1$
            add(label, GBC.std());
            add(fldGunneryM, GBC.eol());

            label = new JLabel(Messages.getString("CustomMechDialog.labGunneryB"), SwingConstants.RIGHT); //$NON-NLS-1$
            add(label, GBC.std());
            add(fldGunneryB, GBC.eol());

        } else {
            label = new JLabel(Messages.getString("CustomMechDialog.labGunnery"), SwingConstants.RIGHT); //$NON-NLS-1$
            add(label, GBC.std());
            add(fldGunnery, GBC.eol());
        }
        fldGunneryL.setText(Integer.toString(entity.getCrew().getGunneryL(slot)));
        fldGunneryM.setText(Integer.toString(entity.getCrew().getGunneryM(slot)));
        fldGunneryB.setText(Integer.toString(entity.getCrew().getGunneryB(slot)));
        fldGunnery.setText(Integer.toString(entity.getCrew().getGunneryL(slot)));

        label = new JLabel(Messages.getString("CustomMechDialog.labPiloting"), SwingConstants.RIGHT); //$NON-NLS-1$
        if (entity instanceof Tank) {
            label.setText(Messages
                    .getString("CustomMechDialog.labDriving"));
        } else if (entity instanceof Infantry) {
            label.setText(Messages
                    .getString("CustomMechDialog.labAntiMech"));
        }
        add(label, GBC.std());
        add(fldPiloting, GBC.eop());
        fldPiloting.setText(Integer.toString(entity.getCrew().getPiloting(slot)));

        if (clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL)) {
            label = new JLabel(Messages.getString("CustomMechDialog.labArtillery"), SwingConstants.RIGHT); //$NON-NLS-1$
            add(label, GBC.std());
            add(fldArtillery, GBC.eop());
        }
        fldArtillery.setText(Integer.toString(entity.getCrew().getArtillery(slot)));

        if (clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_TOUGHNESS)) {
            label = new JLabel(Messages.getString("CustomMechDialog.labTough"), SwingConstants.RIGHT); //$NON-NLS-1$
            add(label, GBC.std());
            add(fldTough, GBC.eop());
        }
        fldTough.setText(Integer.toString(entity.getCrew().getToughness(slot)));

        if (entity instanceof Protomech) {
            // All Protomechs have a callsign.
            StringBuffer callsign = new StringBuffer(
                    Messages.getString("CustomMechDialog.Callsign")); //$NON-NLS-1$
            callsign.append(": "); //$NON-NLS-1$
            callsign.append(
                    (entity.getUnitNumber() + PreferenceManager
                            .getClientPreferences().getUnitStartChar()))
                    .append('-').append(entity.getId());
            label = new JLabel(callsign.toString(), SwingConstants.CENTER);
            add(label, GBC.eol().anchor(GridBagConstraints.CENTER));

            // Get the Protomechs of this entity's player
            // that *aren't* in the entity's unit.
            Iterator<Entity> otherUnitEntities = clientgui.getClient().getGame()
                    .getSelectedEntities(new EntitySelector() {
                        private final int ownerId = entity.getOwnerId();

                        private final short unitNumber = entity.getUnitNumber();

                        public boolean accept(Entity unitEntity) {
                            if ((unitEntity instanceof Protomech)
                                    && (ownerId == unitEntity.getOwnerId())
                                    && (unitNumber != unitEntity
                                            .getUnitNumber())) {
                                return true;
                            }
                            return false;
                        }
                    });

            // If we got any other entites, show the unit number controls.
            if (otherUnitEntities.hasNext()) {
                label = new JLabel(Messages.getString("CustomMechDialog.labUnitNum"), SwingConstants.CENTER); //$NON-NLS-1$
                add(choUnitNum, GBC.eop());
                refreshUnitNum(otherUnitEntities);
            }
        }

        if (clientgui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.RPG_PILOT_ADVANTAGES) //$NON-NLS-1$
                || clientgui.getClient().getGame().getOptions()
                        .booleanOption(OptionsConstants.EDGE) //$NON-NLS-1$
                || clientgui.getClient().getGame().getOptions()
                        .booleanOption(OptionsConstants.RPG_MANEI_DOMINI)) { //$NON-NLS-1$
        }
        if (!editable) {
            fldName.setEnabled(false);
            fldNick.setEnabled(false);
            fldGunnery.setEnabled(false);
            fldGunneryL.setEnabled(false);
            fldGunneryM.setEnabled(false);
            fldGunneryB.setEnabled(false);
            fldPiloting.setEnabled(false);
            fldArtillery.setEnabled(false);
            fldTough.setEnabled(false);
        }

    }
    
    /**
     * Populate the list of entities in other units from the given enumeration.
     *
     * @param others
     *            the <code>Enumeration</code> containing entities in other
     *            units.
     */
    private void refreshUnitNum(Iterator<Entity> others) {
        // Clear the list of old values
        choUnitNum.removeAllItems();
        entityUnitNum.clear();

        // Make an entry for "no change".
        choUnitNum.addItem(Messages
                .getString("CustomMechDialog.doNotSwapUnits")); //$NON-NLS-1$
        entityUnitNum.add(entity);

        // Walk through the other entities.
        while (others.hasNext()) {
            // Track the position of the next other entity.
            final Entity other = others.next();
            entityUnitNum.add(other);

            // Show the other entity's name and callsign.
            StringBuffer callsign = new StringBuffer(other.getDisplayName());
            callsign.append(" (")//$NON-NLS-1$
                    .append((other.getUnitNumber() + PreferenceManager
                            .getClientPreferences().getUnitStartChar()))
                    .append('-').append(other.getId()).append(')');
            choUnitNum.addItem(callsign.toString());
        }
        choUnitNum.setSelectedIndex(0);
    }
    
    public String getPilotName() {
        return fldName.getText();
    }
    
    public String getNickname() {
        return fldNick.getText();
    }
    
    public int getGunnery() {
        return Integer.parseInt(fldGunnery.getText());
    }
    
    public int getGunneryL() {
        return Integer.parseInt(fldGunneryL.getText());
    }
    
    public int getGunneryM() {
        return Integer.parseInt(fldGunneryM.getText());
    }
    
    public int getGunneryB() {
        return Integer.parseInt(fldGunneryB.getText());
    }
    
    public int getArtillery() {
        return Integer.parseInt(fldArtillery.getText());
    }
    
    public int getPiloting() {
        return Integer.parseInt(fldPiloting.getText());
    }
    
    public int getToughness() {
        return Integer.parseInt(fldTough.getText());
    }
    
    public String getPortraitCategory() {
        return portraitDialog.getCategory();
    }
    
    public String getPortraitFilename() {
        return portraitDialog.getFileName();
    }
    
    public Entity getEntityUnitNumSwap() {
        if (entityUnitNum.isEmpty() || choUnitNum.getSelectedIndex() <= 0) {
            return null;
        }
        return entityUnitNum.get(choUnitNum.getSelectedIndex());
    }
}
