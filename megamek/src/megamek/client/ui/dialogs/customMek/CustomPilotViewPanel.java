/*
 * Copyright (C) 2017-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.customMek;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.iconChooser.PortraitChooserDialog;
import megamek.client.ui.util.UIUtil;
import megamek.common.enums.Gender;
import megamek.common.icons.Portrait;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Entity;
import megamek.common.units.EntitySelector;
import megamek.common.units.Infantry;
import megamek.common.units.LAMPilot;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;

/**
 * Controls for customizing crew in the chat lounge. For most crew types this is part of the pilot tab. For multi-crew
 * cockpits there is a separate tab for each crew member and another that shows common options for the entire crew.
 *
 * @author Neoancient
 */
public class CustomPilotViewPanel extends JPanel implements Scrollable {
    @Serial
    private static final long serialVersionUID = 345126674612500365L;

    private static final int SCROLL_UNIT_INCREMENT = 16;

    private final Entity entity;
    private Gender gender = Gender.RANDOMIZE;

    private final JCheckBox chkMissing = new JCheckBox(Messages.getString("CustomMekDialog.chkMissing"));
    private final JTextField fldName = new JTextField(30);
    private final JTextField fldNick = new JTextField(30);
    private final JCheckBox chkClanPilot = new JCheckBox(Messages.getString("CustomMekDialog.chkClanPilot"));
    private final JTextField fldGunnery = new JTextField(4);
    private final JTextField fldGunneryL = new JTextField(4);
    private final JTextField fldGunneryM = new JTextField(4);
    private final JTextField fldGunneryB = new JTextField(4);
    private final JTextField fldPiloting = new JTextField(4);
    private final JTextField fldGunneryAero = new JTextField(4);
    private final JTextField fldGunneryAeroL = new JTextField(4);
    private final JTextField fldGunneryAeroM = new JTextField(4);
    private final JTextField fldGunneryAeroB = new JTextField(4);
    private final JTextField fldPilotingAero = new JTextField(4);
    private final JTextField fldArtillery = new JTextField(4);
    private final JTextField fldTough = new JTextField(4);
    private final JTextField fldFatigue = new JTextField(4);

    private final JComboBox<String> cbBackup = new JComboBox<>();

    private final List<Entity> entityUnitNum = new ArrayList<>();
    private final JComboBox<String> choUnitNum = new JComboBox<>();

    /** The row of titled sections along the top of the panel; sections occupy fixed grid columns. */
    private final JPanel sectionsRow = new JPanel(new GridBagLayout());
    /**
     * The Advanced section, spanning the full width under the identity section. Its rows flow into label+control
     * pairs, {@link #ADVANCED_PAIRS_PER_ROW} to a line. Kept as a field so the dialog can add crew-level rows
     * (initiative, commander) through {@link #addAdvancedRow(String, JComponent)}.
     */
    private JPanel advancedSection;
    private static final int ADVANCED_PAIRS_PER_ROW = 3;
    private int advancedPairCount = 0;
    private final int sectionGap = UIUtil.scaleForGUI(10);

    private Portrait portrait;

    public CustomPilotViewPanel(CustomMekDialog parent, Entity entity, int slot, boolean editable) {
        this.entity = entity;
        setLayout(new GridBagLayout());

        if (entity.getCrew().getSlotCount() > 1) {
            chkMissing.setActionCommand("missing");
            chkMissing.addActionListener(parent);
            chkMissing.addActionListener(event -> missingToggled());
            chkMissing.setSelected(entity.getCrew().isMissing(slot));
            add(chkMissing, GBC.eop());
        }

        // Text fields are the only components GridBag can squeeze below preferred size (labels and buttons
        // refuse), so any width deficit used to collapse them to slivers. Lock their minimum to preferred: a
        // too-narrow window now clips at the row's right edge instead of crushing the inputs.
        for (JTextField inputField : List.of(fldName, fldNick, fldGunnery, fldGunneryL, fldGunneryM, fldGunneryB,
              fldPiloting, fldGunneryAero, fldGunneryAeroL, fldGunneryAeroM, fldGunneryAeroB, fldPilotingAero,
              fldArtillery, fldTough, fldFatigue)) {
            inputField.setMinimumSize(inputField.getPreferredSize());
        }

        // Sections sit side by side at their natural width in fixed grid columns; the trailing glue absorbs the
        // leftover row width and packs them to the left. The Advanced section goes below the identity section in
        // the same grid column, stretched to its width, so the two panels' edges line up.
        sectionsRow.add(buildIdentitySection(parent, slot), sectionConstraints(0));
        sectionsRow.add(buildSkillsSection(parent, slot), sectionConstraints(1));

        advancedSection = buildAdvancedSection();
        chkClanPilot.setText("");
        chkClanPilot.setSelected(entity.getCrew().isClanPilot(slot));
        if (entity.getCrew().getSlotCount() > 1) {
            // Multi-crew: this panel is a per-member tab with no command controls, so add the flag right away.
            // Single pilot: the dialog calls addClanPilotAdvancedRow() while interleaving its command controls,
            // placing the flag after Commander Initiative.
            addClanPilotAdvancedRow();
        }
        if (parent.getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_TOUGHNESS)) {
            addAdvancedRow(Messages.getString("CustomMekDialog.labTough"), fldTough);
        }
        fldTough.setText(Integer.toString(entity.getCrew().getToughness(slot)));
        if (parent.getClient().getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_FATIGUE)) {
            addAdvancedRow(Messages.getString("CustomMekDialog.labFatigue"), fldFatigue);
        }
        fldFatigue.setText(Integer.toString(entity.getCrew().getCrewFatigue(slot)));

        JPanel crewRoleSection = buildCrewRoleSection(parent, slot);
        if (crewRoleSection.getComponentCount() > 0) {
            sectionsRow.add(crewRoleSection, sectionConstraints(2));
        }

        sectionsRow.add(new JPanel(), GBC.std().gridX(3).gridY(0).fill(GridBagConstraints.HORIZONTAL));
        add(sectionsRow, GBC.eop().anchor(GridBagConstraints.NORTHWEST).fill(GridBagConstraints.HORIZONTAL));

        if (!editable) {
            fldName.setEnabled(false);
            fldNick.setEnabled(false);
            chkClanPilot.setEnabled(false);
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
            fldFatigue.setEnabled(false);
        }

        missingToggled();
    }

    /** @return the constraints for a titled section in the sections row, at the given fixed grid column */
    private GBC sectionConstraints(int gridColumn) {
        return GBC.std().gridX(gridColumn).gridY(0)
              .anchor(GridBagConstraints.NORTHWEST).insets(0, 0, sectionGap, 0);
    }

    /** Puts the Advanced section under the identity section at its full width, once, on the first row added. */
    private void attachAdvancedSection() {
        if (advancedSection.getParent() == null) {
            sectionsRow.add(advancedSection, GBC.std().gridX(0).gridY(1)
                  .anchor(GridBagConstraints.NORTHWEST)
                  .fill(GridBagConstraints.HORIZONTAL).weightX(0)
                  .insets(0, 6, sectionGap, 0));
            sectionsRow.revalidate();
        }
    }

    /**
     * Adds the clan pilot flag as an Advanced pair. Called at construction for multi-crew units; for single-pilot
     * units the dialog calls it at the agreed point while interleaving its command controls.
     */
    public void addClanPilotAdvancedRow() {
        addAdvancedRow(Messages.getString("CustomMekDialog.chkClanPilot"), chkClanPilot);
    }

    /**
     * Adds one label+control pair to the Advanced section, flowing {@value #ADVANCED_PAIRS_PER_ROW} pairs to a
     * line, and shows the section on the first pair. Lets the dialog fold its crew-level controls (initiative
     * bonuses, commander flag) into this section for single-pilot units instead of showing a separate Command
     * section.
     *
     * @param labelText the pair's label, already localized
     * @param control   the input component; its minimum size is locked so it cannot be squeezed to a sliver
     */
    public void addAdvancedRow(String labelText, JComponent control) {
        control.setMinimumSize(control.getPreferredSize());
        int pairIndex = advancedPairCount % ADVANCED_PAIRS_PER_ROW;
        int rowIndex = advancedPairCount / ADVANCED_PAIRS_PER_ROW;
        advancedSection.add(new JLabel(labelText, SwingConstants.RIGHT),
              GBC.std().gridX(pairIndex * 2).gridY(rowIndex)
                    .anchor(GridBagConstraints.EAST).insets((pairIndex == 0) ? 0 : 15, 0, 5, 2));
        advancedSection.add(control,
              GBC.std().gridX(pairIndex * 2 + 1).gridY(rowIndex)
                    .anchor(GridBagConstraints.WEST).insets(0, 0, 0, 2));
        advancedPairCount++;
        attachAdvancedSection();
    }

    /**
     * @param titleKey the message key for the section title
     *
     * @return a titled border with inner padding, shared by all pilot tab sections
     */
    static Border sectionBorder(String titleKey) {
        int padding = UIUtil.scaleForGUI(5);
        return BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder(Messages.getString(titleKey)),
              BorderFactory.createEmptyBorder(padding, padding, padding, padding));
    }

    /**
     * Builds the identity section as a compact grid - portrait at the left, the randomizer buttons next to it, and
     * the name and callsign fields to their right:
     * <pre>
     * [portrait] [Random Name]     [Name:]     [name field]
     *            [Random Callsign] [Nickname:] [callsign field]
     *            [Random Skill]
     * </pre>
     */
    private JPanel buildIdentitySection(CustomMekDialog parent, int slot) {
        JPanel identitySection = new JPanel(new GridBagLayout());
        identitySection.setBorder(sectionBorder("CustomMekDialog.sectionIdentity"));
        boolean showButtons = parent.getClientGUI() != null;
        int columnGap = UIUtil.scaleForGUI(10);

        if (showButtons) {
            JButton portraitButton = new JButton();
            portraitButton.setPreferredSize(UIUtil.scaleForGUI(72, 72));
            portraitButton.setName("portrait");
            portraitButton.addActionListener(event -> {
                final PortraitChooserDialog portraitDialog = new PortraitChooserDialog(
                      parent.getFrame(), entity.getCrew().getPortrait(slot));
                if (portraitDialog.showDialog().isConfirmed()) {
                    portrait = portraitDialog.getSelectedItem();
                    portraitButton.setIcon(portraitDialog.getSelectedItem().getImageIcon());
                }
            });

            portrait = entity.getCrew().getPortrait(slot);
            portraitButton.setIcon(entity.getCrew().getPortrait(slot).getImageIcon());
            identitySection.add(portraitButton,
                  GBC.std().gridHeight(3).anchor(GridBagConstraints.NORTHWEST).insets(0, 0, columnGap, 0));
        }

        // Row 1: random name | name
        if (showButtons) {
            JButton randomNameButton = new JButton(Messages.getString("CustomMekDialog.RandomName"));
            randomNameButton.addActionListener(event -> {
                gender = RandomGenderGenerator.generate();
                fldName.setText(RandomNameGenerator.getInstance()
                      .generate(gender, isClanPilot(), entity.getOwner().getName()));
            });
            identitySection.add(randomNameButton,
                  GBC.std().fill(GridBagConstraints.HORIZONTAL).weightX(0).insets(0, 0, columnGap, 2));
        }
        identitySection.add(new JLabel(Messages.getString("CustomMekDialog.labName"), SwingConstants.RIGHT),
              GBC.std());
        identitySection.add(fldName, GBC.eol().insets(5, 0, 0, 2));
        fldName.setText(entity.getCrew().getName(slot));

        // Row 2: random callsign | nickname
        if (showButtons) {
            JButton randomCallsignButton = new JButton(Messages.getString("CustomMekDialog.RandomCallsign"));
            randomCallsignButton.addActionListener(event ->
                  fldNick.setText(RandomCallsignGenerator.getInstance().generate()));
            identitySection.add(randomCallsignButton,
                  GBC.std().fill(GridBagConstraints.HORIZONTAL).weightX(0).insets(0, 0, columnGap, 2));
        }
        identitySection.add(new JLabel(Messages.getString("CustomMekDialog.labNick"), SwingConstants.RIGHT),
              GBC.std());
        identitySection.add(fldNick, GBC.eol().insets(5, 0, 0, 2));
        fldNick.setText(entity.getCrew().getNickname(slot));

        // Row 3: random skill
        if (showButtons) {
            JButton randomSkillButton = new JButton(Messages.getString("CustomMekDialog.RandomSkill"));
            randomSkillButton.addActionListener(event -> {
                int[] skills = parent.getClient().getSkillGenerator().generateRandomSkills(entity);
                fldGunnery.setText(Integer.toString(skills[0]));
                fldPiloting.setText(Integer.toString(skills[1]));
                if (entity.getCrew() instanceof LAMPilot) {
                    skills = parent.getClient().getSkillGenerator().generateRandomSkills(entity);
                    fldGunneryAero.setText(Integer.toString(skills[0]));
                    fldPilotingAero.setText(Integer.toString(skills[1]));
                }
            });
            identitySection.add(randomSkillButton,
                  GBC.eol().fill(GridBagConstraints.HORIZONTAL).weightX(0).insets(0, 0, columnGap, 2));
        }

        return identitySection;
    }

    /**
     * Builds the skills section: gunnery (RPG split and LAM aero variants included), piloting/driving/anti-mek,
     * and the artillery skill when its game option is on.
     */
    private JPanel buildSkillsSection(CustomMekDialog parent, int slot) {
        JPanel skillsSection = new JPanel(new GridBagLayout());
        skillsSection.setBorder(sectionBorder("CustomMekDialog.sectionSkills"));

        // Piloting reads above gunnery
        JLabel pilotingLabel = new JLabel(Messages.getString("CustomMekDialog.labPiloting"), SwingConstants.RIGHT);
        if (entity instanceof Tank) {
            pilotingLabel.setText(Messages.getString("CustomMekDialog.labDriving"));
        } else if (entity instanceof Infantry) {
            pilotingLabel.setText(Messages.getString("CustomMekDialog.labAntiMek"));
        }
        if (entity.getCrew() instanceof LAMPilot pilot) {
            skillsSection.add(pilotingLabel, GBC.std());
            skillsSection.add(fldPiloting, GBC.eol());
            fldPiloting.setText(Integer.toString(pilot.getPilotingMek()));
            skillsSection.add(new JLabel(Messages.getString("CustomMekDialog.labPilotingAero"),
                  SwingConstants.RIGHT), GBC.std());
            skillsSection.add(fldPilotingAero, GBC.eol());
            fldPilotingAero.setText(Integer.toString(pilot.getPilotingAero()));
        } else {
            skillsSection.add(pilotingLabel, GBC.std());
            skillsSection.add(fldPiloting, GBC.eol());
            fldPiloting.setText(Integer.toString(entity.getCrew().getPiloting(slot)));
            fldPilotingAero.setText("0");
        }

        if (parent.getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY)) {
            skillsSection.add(new JLabel(Messages.getString("CustomMekDialog.labGunneryL"), SwingConstants.RIGHT),
                  GBC.std());
            skillsSection.add(fldGunneryL, GBC.eol());

            skillsSection.add(new JLabel(Messages.getString("CustomMekDialog.labGunneryM"), SwingConstants.RIGHT),
                  GBC.std());
            skillsSection.add(fldGunneryM, GBC.eol());

            skillsSection.add(new JLabel(Messages.getString("CustomMekDialog.labGunneryB"), SwingConstants.RIGHT),
                  GBC.std());
            skillsSection.add(fldGunneryB, GBC.eol());

            if (entity.getCrew() instanceof LAMPilot) {
                skillsSection.add(new JLabel(Messages.getString("CustomMekDialog.labGunneryAeroL"),
                      SwingConstants.RIGHT), GBC.std());
                skillsSection.add(fldGunneryAeroL, GBC.eol());

                skillsSection.add(new JLabel(Messages.getString("CustomMekDialog.labGunneryAeroM"),
                      SwingConstants.RIGHT), GBC.std());
                skillsSection.add(fldGunneryAeroM, GBC.eol());

                skillsSection.add(new JLabel(Messages.getString("CustomMekDialog.labGunneryAeroB"),
                      SwingConstants.RIGHT), GBC.std());
                skillsSection.add(fldGunneryAeroB, GBC.eol());
            }
        } else {
            skillsSection.add(new JLabel(Messages.getString("CustomMekDialog.labGunnery"), SwingConstants.RIGHT),
                  GBC.std());
            skillsSection.add(fldGunnery, GBC.eol());

            if (entity.getCrew() instanceof LAMPilot) {
                skillsSection.add(new JLabel(Messages.getString("CustomMekDialog.labGunneryAero"),
                      SwingConstants.RIGHT), GBC.std());
                skillsSection.add(fldGunneryAero, GBC.eol());
            }
        }

        if (entity.getCrew() instanceof LAMPilot pilot) {
            fldGunneryL.setText(Integer.toString(pilot.getGunneryMekL()));
            fldGunneryM.setText(Integer.toString(pilot.getGunneryMekM()));
            fldGunneryB.setText(Integer.toString(pilot.getGunneryMekB()));
            fldGunnery.setText(Integer.toString(pilot.getGunneryMek()));
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

        if (parent.getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL)) {
            skillsSection.add(new JLabel(Messages.getString("CustomMekDialog.labArtillery"), SwingConstants.RIGHT),
                  GBC.std());
            skillsSection.add(fldArtillery, GBC.eop());
        }
        fldArtillery.setText(Integer.toString(entity.getCrew().getArtillery(slot)));

        return skillsSection;
    }

    /**
     * Builds the empty Advanced section shell. Content arrives through {@link #addAdvancedRow(String, JComponent)}
     * - the clan pilot flag and the option-gated toughness and fatigue fields from this panel, plus the crew-level
     * command controls the dialog adds for single-pilot units.
     */
    private JPanel buildAdvancedSection() {
        JPanel section = new JPanel(new GridBagLayout());
        section.setBorder(sectionBorder("CustomMekDialog.sectionRpgExtras"));
        // Trailing glue keeps the pair columns packed to the left when the section stretches to identity width
        section.add(new JPanel(),
              GBC.std().gridX(ADVANCED_PAIRS_PER_ROW * 2).gridY(0).fill(GridBagConstraints.HORIZONTAL));
        return section;
    }

    /**
     * Builds the crew role section: the backup pilot/gunner choice for multi-crew cockpits and the ProtoMek
     * callsign and unit transfer controls. The section is dropped when it has no components.
     */
    private JPanel buildCrewRoleSection(CustomMekDialog parent, int slot) {
        JPanel crewRoleSection = new JPanel(new GridBagLayout());
        crewRoleSection.setBorder(sectionBorder("CustomMekDialog.sectionCrewRole"));

        if (entity.getCrew().getSlotCount() > 2) {
            for (int i = 0; i < entity.getCrew().getSlotCount(); i++) {
                if (i != slot) {
                    cbBackup.addItem(entity.getCrew().getCrewType().getRoleName(i));
                }
            }
            if (slot == entity.getCrew().getCrewType().getPilotPos()) {
                crewRoleSection.add(new JLabel(Messages.getString("CustomMekDialog.labBackupPilot"),
                      SwingConstants.RIGHT), GBC.std());
                crewRoleSection.add(cbBackup, GBC.eop());
                cbBackup.setToolTipText(Messages.getString("CustomMekDialog.tooltipBackupPilot"));
                cbBackup.setSelectedItem(entity.getCrew()
                      .getCrewType()
                      .getRoleName(entity.getCrew().getBackupPilotPos()));
            } else if (slot == entity.getCrew().getCrewType().getGunnerPos()) {
                crewRoleSection.add(new JLabel(Messages.getString("CustomMekDialog.labBackupGunner"),
                      SwingConstants.RIGHT), GBC.std());
                crewRoleSection.add(cbBackup, GBC.eop());
                cbBackup.setToolTipText(Messages.getString("CustomMekDialog.tooltipBackupGunner"));
                cbBackup.setSelectedItem(entity.getCrew()
                      .getCrewType()
                      .getRoleName(entity.getCrew().getBackupGunnerPos()));
            }
        }

        if (entity instanceof ProtoMek) {
            // All ProtoMeks have a callsign.
            String callsign = Messages.getString("CustomMekDialog.Callsign") + ": " +
                  (entity.getUnitNumber() + PreferenceManager
                        .getClientPreferences().getUnitStartChar()) +
                  '-' + entity.getId();
            crewRoleSection.add(new JLabel(callsign, SwingConstants.CENTER),
                  GBC.eol().anchor(GridBagConstraints.CENTER));

            // Get the ProtoMeks of this entity's player
            // that *aren't* in the entity's unit.
            Iterator<Entity> otherUnitEntities = parent.getClient().getGame()
                  .getSelectedEntities(new EntitySelector() {
                      private final int ownerId = entity.getOwnerId();

                      private final short unitNumber = entity.getUnitNumber();

                      @Override
                      public boolean accept(Entity unitEntity) {
                          return (unitEntity instanceof ProtoMek)
                                && (ownerId == unitEntity.getOwnerId())
                                && (unitNumber != unitEntity.getUnitNumber());
                      }
                  });

            // If we got any other entities, show the unit number controls.
            if (otherUnitEntities.hasNext()) {
                crewRoleSection.add(choUnitNum, GBC.eop());
                refreshUnitNum(otherUnitEntities);
            }
        }

        return crewRoleSection;
    }

    /**
     * Populate the list of entities in other units from the given enumeration.
     *
     * @param others the <code>Enumeration</code> containing entities in other units.
     */
    private void refreshUnitNum(Iterator<Entity> others) {
        // Clear the list of old values
        choUnitNum.removeAllItems();
        entityUnitNum.clear();

        // Make an entry for "no change".
        choUnitNum.addItem(Messages.getString("CustomMekDialog.doNotSwapUnits"));
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

    public Gender getGender() {
        return gender;
    }

    public boolean isClanPilot() {
        return chkClanPilot.isSelected();
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

    public int getCrewFatigue() {
        return Integer.parseInt(fldFatigue.getText());
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
        boolean enabled = !chkMissing.isSelected();
        for (int i = 0; i < getComponentCount(); i++) {
            if (!getComponent(i).equals(chkMissing)) {
                setEnabledRecursively(getComponent(i), enabled);
            }
        }
    }

    /**
     * Enables or disables a component and everything nested inside it. Swing's {@code setEnabled} does not cascade
     * into child components, so with the fields grouped into titled section panels a plain loop over this panel's
     * direct children would only gray the section borders and leave every field active.
     */
    private void setEnabledRecursively(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                setEnabledRecursively(child, enabled);
            }
        }
    }

    public void enableMissing(boolean enable) {
        chkMissing.setEnabled(enable);
    }

    // Scrollable: track the viewport width so the panel reflows to the window instead of forcing the whole tab
    // to scroll horizontally; height stays free for vertical scrolling.

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return SCROLL_UNIT_INCREMENT;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return (orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
