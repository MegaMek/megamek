/*
 * Copyright (C) 2017 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.PortraitChooserDialog;
import megamek.common.*;
import megamek.common.enums.Gender;
import megamek.common.icons.Portrait;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Controls for customizing crew in the chat lounge. For most crew types this is part of the pilot tab.
 * For multi-crew cockpits there is a separate tab for each crew member and another that shows common options
 * for the entire crew.
 *
 * @author Neoancient
 */
public class CustomPilotView extends JPanel {
    private static final long serialVersionUID = 345126674612500365L;

    private final Entity entity;
    private Gender gender = Gender.RANDOMIZE;

    private final JCheckBox chkMissing = new JCheckBox(Messages.getString("CustomMechDialog.chkMissing"));
    private final JTextField fldName = new JTextField(20);
    private final JTextField fldNick = new JTextField(20);
    private final JTextField fldHits = new JTextField(5);
    private final JTextField fldGunnery = new JTextField(3);
    private final JTextField fldGunneryL = new JTextField(3);
    private final JTextField fldGunneryM = new JTextField(3);
    private final JTextField fldGunneryB = new JTextField(3);
    private final JTextField fldPiloting = new JTextField(3);
    private final JTextField fldGunneryAero = new JTextField(3);
    private final JTextField fldGunneryAeroL = new JTextField(3);
    private final JTextField fldGunneryAeroM = new JTextField(3);
    private final JTextField fldGunneryAeroB = new JTextField(3);
    private final JTextField fldPilotingAero = new JTextField(3);
    private final JTextField fldArtillery = new JTextField(3);
    private final JTextField fldTough = new JTextField(3);

    private final JComboBox<String> cbBackup = new JComboBox<>();

    private final List<Entity> entityUnitNum = new ArrayList<>();
    private final JComboBox<String> choUnitNum = new JComboBox<>();

    private Portrait portrait;

    public CustomPilotView(CustomMechDialog parent, Entity entity, int slot, boolean editable) {
        this.entity = entity;
        setLayout(new GridBagLayout());
        JLabel label;

        if (entity.getCrew().getSlotCount() > 1) {
            chkMissing.setActionCommand("missing");
            chkMissing.addActionListener(parent);
            chkMissing.addActionListener(e -> missingToggled());
            chkMissing.setSelected(entity.getCrew().isMissing(slot));
            add(chkMissing, GBC.eop());
        }

        JButton portraitButton = new JButton();
        portraitButton.setPreferredSize(new Dimension(72, 72));
        portraitButton.setName("portrait");
        portraitButton.addActionListener(e -> {
            final PortraitChooserDialog portraitDialog = new PortraitChooserDialog(
                    parent.getClientGUI().frame, entity.getCrew().getPortrait(slot));
            if (portraitDialog.showDialog().isConfirmed()) {
                portrait = portraitDialog.getSelectedItem();
                portraitButton.setIcon(portraitDialog.getSelectedItem().getImageIcon());
            }
        });

        portrait = entity.getCrew().getPortrait(slot);
        portraitButton.setIcon(entity.getCrew().getPortrait(slot).getImageIcon());
        add(portraitButton, GBC.std().gridheight(2));

        JButton button = new JButton(Messages.getString("CustomMechDialog.RandomName"));
        button.addActionListener(e -> {
            gender = RandomGenderGenerator.generate();
            fldName.setText(RandomNameGenerator.getInstance().generate(gender, entity.getOwner().getName()));
        });
        add(button, GBC.eop());

        button = new JButton(Messages.getString("CustomMechDialog.RandomCallsign"));
        button.addActionListener(e -> fldNick.setText(RandomCallsignGenerator.getInstance().generate()));
        add(button, GBC.eop());

        button = new JButton(Messages.getString("CustomMechDialog.RandomSkill"));
        button.addActionListener(e -> {
            int[] skills = parent.getClientGUI().getClient().getSkillGenerator().generateRandomSkills(entity);
            fldGunnery.setText(Integer.toString(skills[0]));
            fldPiloting.setText(Integer.toString(skills[1]));
            if (entity.getCrew() instanceof LAMPilot) {
                skills = parent.getClientGUI().getClient().getSkillGenerator().generateRandomSkills(entity);
                fldGunneryAero.setText(Integer.toString(skills[0]));
                fldPilotingAero.setText(Integer.toString(skills[1]));
            }
        });
        add(button, GBC.eop());

        label = new JLabel(Messages.getString("CustomMechDialog.labName"), SwingConstants.RIGHT);
        add(label, GBC.std());
        add(fldName, GBC.eol());
        fldName.setText(entity.getCrew().getName(slot));

        label = new JLabel(Messages.getString("CustomMechDialog.labNick"), SwingConstants.RIGHT);
        add(label, GBC.std());
        add(fldNick, GBC.eol());
        fldNick.setText(entity.getCrew().getNickname(slot));

        label = new JLabel(Messages.getString("CustomMechDialog.labHits"), SwingConstants.RIGHT);
        add(label, GBC.std());
        add(fldHits, GBC.eop());
        fldHits.setText(String.valueOf(entity.getCrew().getHits()));

        if (parent.getClientGUI().getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY)) {
            label = new JLabel(Messages.getString("CustomMechDialog.labGunneryL"), SwingConstants.RIGHT);
            add(label, GBC.std());
            add(fldGunneryL, GBC.eol());

            label = new JLabel(Messages.getString("CustomMechDialog.labGunneryM"), SwingConstants.RIGHT);
            add(label, GBC.std());
            add(fldGunneryM, GBC.eol());

            label = new JLabel(Messages.getString("CustomMechDialog.labGunneryB"), SwingConstants.RIGHT);
            add(label, GBC.std());
            add(fldGunneryB, GBC.eol());

            if (entity.getCrew() instanceof LAMPilot) {
                label = new JLabel(Messages.getString("CustomMechDialog.labGunneryAeroL"), SwingConstants.RIGHT);
                add(label, GBC.std());
                add(fldGunneryAeroL, GBC.eol());

                label = new JLabel(Messages.getString("CustomMechDialog.labGunneryAeroM"), SwingConstants.RIGHT);
                add(label, GBC.std());
                add(fldGunneryAeroM, GBC.eol());

                label = new JLabel(Messages.getString("CustomMechDialog.labGunneryAeroB"), SwingConstants.RIGHT);
                add(label, GBC.std());
                add(fldGunneryAeroB, GBC.eol());
            }

        } else {
            label = new JLabel(Messages.getString("CustomMechDialog.labGunnery"), SwingConstants.RIGHT);
            add(label, GBC.std());
            add(fldGunnery, GBC.eol());

            if (entity.getCrew() instanceof LAMPilot) {
                label = new JLabel(Messages.getString("CustomMechDialog.labGunneryAero"), SwingConstants.RIGHT);
                add(label, GBC.std());
                add(fldGunneryAero, GBC.eol());
            }
        }
        if (entity.getCrew() instanceof LAMPilot) {
            LAMPilot pilot = (LAMPilot) entity.getCrew();
            fldGunneryL.setText(Integer.toString(pilot.getGunneryMechL()));
            fldGunneryM.setText(Integer.toString(pilot.getGunneryMechM()));
            fldGunneryB.setText(Integer.toString(pilot.getGunneryMechB()));
            fldGunnery.setText(Integer.toString(pilot.getGunneryMech()));
            fldGunneryAeroL.setText(Integer.toString(pilot.getGunneryAeroL()));
            fldGunneryAeroM.setText(Integer.toString(pilot.getGunneryAeroM()));
            fldGunneryAeroB.setText(Integer.toString(pilot.getGunneryAeroB()));
            fldGunneryAero.setText(Integer.toString(pilot.getGunneryAero()));
        } else {
            fldGunneryL.setText(Integer.toString(entity.getCrew().getGunneryL(slot)));
            fldGunneryM.setText(Integer.toString(entity.getCrew().getGunneryM(slot)));
            fldGunneryB.setText(Integer.toString(entity.getCrew().getGunneryB(slot)));
            fldGunnery.setText(Integer.toString(entity.getCrew().getGunnery(slot)));
            fldGunneryAeroL.setText("0");
            fldGunneryAeroM.setText("0");
            fldGunneryAeroB.setText("0");
            fldGunneryAero.setText("0");
        }

        label = new JLabel(Messages.getString("CustomMechDialog.labPiloting"), SwingConstants.RIGHT);
        if (entity instanceof Tank) {
            label.setText(Messages.getString("CustomMechDialog.labDriving"));
        } else if (entity instanceof Infantry) {
            label.setText(Messages.getString("CustomMechDialog.labAntiMech"));
        }
        if (entity.getCrew() instanceof LAMPilot) {
            add(label, GBC.std());
            add(fldPiloting, GBC.eol());
            fldPiloting.setText(Integer.toString(((LAMPilot) entity.getCrew()).getPilotingMech()));
            label = new JLabel(Messages.getString("CustomMechDialog.labPilotingAero"), SwingConstants.RIGHT);
            add(label, GBC.std());
            add(fldPilotingAero, GBC.eop());
            fldPilotingAero.setText(Integer.toString(((LAMPilot) entity.getCrew()).getPilotingAero()));
        } else {
            add(label, GBC.std());
            add(fldPiloting, GBC.eop());
            fldPiloting.setText(Integer.toString(entity.getCrew().getPiloting(slot)));
            fldPilotingAero.setText("0");
        }

        if (parent.getClientGUI().getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL)) {
            label = new JLabel(Messages.getString("CustomMechDialog.labArtillery"), SwingConstants.RIGHT);
            add(label, GBC.std());
            add(fldArtillery, GBC.eop());
        }
        fldArtillery.setText(Integer.toString(entity.getCrew().getArtillery(slot)));

        if (parent.getClientGUI().getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_TOUGHNESS)) {
            label = new JLabel(Messages.getString("CustomMechDialog.labTough"), SwingConstants.RIGHT);
            add(label, GBC.std());
            add(fldTough, GBC.eop());
        }
        fldTough.setText(Integer.toString(entity.getCrew().getToughness(slot)));

        if (entity.getCrew().getSlotCount() > 2) {
            for (int i = 0; i < entity.getCrew().getSlotCount(); i++) {
                if (i != slot) {
                    cbBackup.addItem(entity.getCrew().getCrewType().getRoleName(i));
                }
            }
            if (slot == entity.getCrew().getCrewType().getPilotPos()) {
                label = new JLabel(Messages.getString("CustomMechDialog.labBackupPilot"), SwingConstants.RIGHT);
                add(label, GBC.std());
                add(cbBackup, GBC.eop());
                cbBackup.setToolTipText(Messages.getString("CustomMechDialog.tooltipBackupPilot"));
                cbBackup.setSelectedItem(entity.getCrew().getCrewType().getRoleName(entity.getCrew().getBackupPilotPos()));
            } else if (slot == entity.getCrew().getCrewType().getGunnerPos()) {
                label = new JLabel(Messages.getString("CustomMechDialog.labBackupGunner"), SwingConstants.RIGHT);
                add(label, GBC.std());
                add(cbBackup, GBC.eop());
                cbBackup.setToolTipText(Messages.getString("CustomMechDialog.tooltipBackupGunner"));
                cbBackup.setSelectedItem(entity.getCrew().getCrewType().getRoleName(entity.getCrew().getBackupGunnerPos()));
            }
        }

        if (entity instanceof Protomech) {
            // All ProtoMechs have a callsign.
            String callsign = Messages.getString("CustomMechDialog.Callsign") + ": " +
                    (entity.getUnitNumber() + PreferenceManager
                            .getClientPreferences().getUnitStartChar()) +
                    '-' + entity.getId();
            label = new JLabel(callsign, SwingConstants.CENTER);
            add(label, GBC.eol().anchor(GridBagConstraints.CENTER));

            // Get the ProtoMechs of this entity's player
            // that *aren't* in the entity's unit.
            Iterator<Entity> otherUnitEntities = parent.getClientGUI().getClient().getGame()
                    .getSelectedEntities(new EntitySelector() {
                        private final int ownerId = entity.getOwnerId();

                        private final short unitNumber = entity.getUnitNumber();

                        @Override
                        public boolean accept(Entity unitEntity) {
                            return (unitEntity instanceof Protomech)
                                    && (ownerId == unitEntity.getOwnerId())
                                    && (unitNumber != unitEntity.getUnitNumber());
                        }
                    });

            // If we got any other entities, show the unit number controls.
            if (otherUnitEntities.hasNext()) {
                label = new JLabel(Messages.getString("CustomMechDialog.labUnitNum"), SwingConstants.CENTER);
                add(choUnitNum, GBC.eop());
                refreshUnitNum(otherUnitEntities);
            }
        }

        if (!editable) {
            fldName.setEnabled(false);
            fldNick.setEnabled(false);
            fldHits.setEnabled(false);
            fldGunnery.setEnabled(false);
            fldGunneryL.setEnabled(false);
            fldGunneryM.setEnabled(false);
            fldGunneryB.setEnabled(false);
            fldGunneryAero.setEnabled(false);
            fldGunneryAeroL.setEnabled(false);
            fldGunneryAeroM.setEnabled(false);
            fldGunneryAeroB.setEnabled(false);
            fldPiloting.setEnabled(false);
            fldPilotingAero.setEnabled(false);
            fldArtillery.setEnabled(false);
            fldTough.setEnabled(false);
        }

        missingToggled();
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
        choUnitNum.addItem(Messages.getString("CustomMechDialog.doNotSwapUnits"));
        entityUnitNum.add(entity);

        // Walk through the other entities.
        while (others.hasNext()) {
            // Track the position of the next other entity.
            final Entity other = others.next();
            entityUnitNum.add(other);

            // Show the other entity's name and callsign.
            String callsign = other.getDisplayName() + " (" +
                    (other.getUnitNumber() + PreferenceManager.getClientPreferences().getUnitStartChar())
                    + '-' + other.getId() + ')';
            choUnitNum.addItem(callsign);
        }
        choUnitNum.setSelectedIndex(0);
    }

    public boolean getMissing() {
        return chkMissing.isSelected();
    }

    public String getPilotName() {
        return fldName.getText();
    }

    public String getNickname() {
        return fldNick.getText();
    }

    public String getHits() {
        int hits;
        try {
            hits = Integer.parseInt(fldHits.getText());
            if (hits < 0) {
                hits = 0;
            } else if (hits > 5) {
                hits = 6;
            }
        } catch (NumberFormatException e) {
            hits = 0;
        }
        // Update field then return
        fldHits.setText(String.valueOf(hits));
        return fldHits.getText();
    }

    public Gender getGender() {
        return gender;
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

    public int getGunneryAero() {
        return Integer.parseInt(fldGunneryAero.getText());
    }

    public int getGunneryAeroL() {
        return Integer.parseInt(fldGunneryAeroL.getText());
    }

    public int getGunneryAeroM() {
        return Integer.parseInt(fldGunneryAeroM.getText());
    }

    public int getGunneryAeroB() {
        return Integer.parseInt(fldGunneryAeroB.getText());
    }

    public int getArtillery() {
        return Integer.parseInt(fldArtillery.getText());
    }

    public int getPiloting() {
        return Integer.parseInt(fldPiloting.getText());
    }

    public int getPilotingAero() {
        return Integer.parseInt(fldPilotingAero.getText());
    }

    public int getToughness() {
        return Integer.parseInt(fldTough.getText());
    }

    public Portrait getPortrait() {
        return portrait;
    }

    public Entity getEntityUnitNumSwap() {
        if (entityUnitNum.isEmpty() || (choUnitNum.getSelectedIndex() <= 0)) {
            return null;
        }
        return entityUnitNum.get(choUnitNum.getSelectedIndex());
    }

    public int getBackup() {
        if (null != cbBackup.getSelectedItem()) {
            for (int i = 0; i < entity.getCrew().getSlotCount(); i++) {
                if (cbBackup.getSelectedItem().equals(entity.getCrew().getCrewType().getRoleName(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void missingToggled() {
        for (int i = 0; i < getComponentCount(); i++) {
            if (!getComponent(i).equals(chkMissing)) {
                getComponent(i).setEnabled(!chkMissing.isSelected());
            }
        }
    }

    void enableMissing(boolean enable) {
        chkMissing.setEnabled(enable);
    }
}
