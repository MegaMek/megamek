/**
 * 
 */
package megamek.client.ui.swing;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import megamek.MegaMek;
import megamek.client.ratgenerator.AbstractUnitRecord;
import megamek.client.ratgenerator.CrewDescriptor;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.ForceNode;
import megamek.client.ratgenerator.MissionRole;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.Ruleset;
import megamek.client.ratgenerator.TOCNode;
import megamek.client.ratgenerator.ValueNode;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.Game;
import megamek.common.Player;
import megamek.common.UnitType;
import megamek.common.options.OptionsConstants;

/**
 * Controls to set options for force generator.
 * 
 * @author Neoancient
 *
 */


public class ForceGeneratorOptionsView extends JPanel implements FocusListener, ActionListener {

    private static final long serialVersionUID = 5269823128861856001L;

    private int currentYear;
    private Consumer<ForceDescriptor> onGenerate = null;

    private ForceDescriptor forceDesc = new ForceDescriptor();

    private JTextField txtYear;
    private JComboBox<FactionRecord> cbFaction;
    private JComboBox<FactionRecord> cbSubfaction;
    private JComboBox<Integer> cbUnitType;	
    private JComboBox<String> cbFormation;
    private JComboBox<String> cbRating;
    private JComboBox<String> cbFlags;

    private JComboBox<String> cbExperience;
    private JComboBox<Integer> cbWeightClass;
    private JCheckBox chkAttachments;

    private DefaultListCellRenderer factionRenderer = new CBRenderer<FactionRecord>
    (Messages.getString("ForceGeneratorDialog.general"),
            fRec -> fRec.getName(currentYear));

    private HashMap<String,String> ratingDisplayNames = new HashMap<>();
    private HashMap<String,String> formationDisplayNames = new HashMap<>();
    private HashMap<String,String> flagDisplayNames = new HashMap<>();

    private JPanel panGroundRole;
    private JPanel panInfRole;
    private JPanel panAirRole;

    private JCheckBox chkRoleRecon;
    private JCheckBox chkRoleFireSupport;
    private JCheckBox chkRoleUrban;
    private JCheckBox chkRoleInfantrySupport;
    private JCheckBox chkRoleCavalry;
    private JCheckBox chkRoleRaider;
    private JCheckBox chkRoleIncindiary;
    private JCheckBox chkRoleAntiAircraft;
    private JCheckBox chkRoleAntiInfantry;	
    private JCheckBox chkRoleArtillery;
    private JCheckBox chkRoleMissileArtillery;
    private JCheckBox chkRoleTransport;
    private JCheckBox chkRoleEngineer;

    private JCheckBox chkRoleFieldGun;
    private JCheckBox chkRoleFieldArtillery;
    private JCheckBox chkRoleFieldMissileArtillery;

    private JCheckBox chkRoleAirRecon;
    private JCheckBox chkRoleGroundSupport;
    private JCheckBox chkRoleInterceptor;
    private JCheckBox chkRoleEscort;
    private JCheckBox chkRoleBomber;
    private JCheckBox chkRoleAssault;
    private JCheckBox chkRoleAirTransport;

    private JTextField txtDropshipPct;
    private JTextField txtJumpshipPct;
    private JTextField txtCargo;

    private JButton btnGenerate;
    private JButton btnExportMUL;
    private JButton btnClear;

    private ClientGUI clientGui;

    public ForceGeneratorOptionsView(ClientGUI gui, Consumer<ForceDescriptor> onGenerate) {
        clientGui = gui;
        this.onGenerate = onGenerate;
        if (!Ruleset.isInitialized()) {
            Ruleset.loadData();
        }
        initUi();
    }

    private void initUi() {
        currentYear = clientGui.getClient().getGame().getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
        forceDesc.setYear(currentYear);
        RATGenerator rg = RATGenerator.getInstance();
        rg.loadYear(currentYear);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        int y = 0;

        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.year")), gbc); //$NON-NLS-1$
        txtYear = new JTextField();
        txtYear.setEditable(true);
        txtYear.setText(Integer.toString(currentYear));
        txtYear.setToolTipText(Messages.getString("ForceGeneratorDialog.year.tooltip")); //$NON-NLS-1$
        gbc.gridx = 1;
        gbc.gridy = y++;
        add(txtYear, gbc);
        txtYear.addFocusListener(this);
        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.faction")), gbc); //$NON-NLS-1$
        cbFaction = new JComboBox<>();
        cbFaction.setRenderer(factionRenderer);
        gbc.gridx = 1;
        gbc.gridy = y;
        add(cbFaction, gbc);
        cbFaction.setToolTipText(Messages.getString("ForceGeneratorDialog.faction.tooltip")); //$NON-NLS-1$
        cbFaction.addActionListener(this);

        gbc.gridx = 2;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.subfaction")), gbc); //$NON-NLS-1$
        cbSubfaction = new JComboBox<>();
        cbSubfaction.setRenderer(factionRenderer);
        gbc.gridx = 3;
        gbc.gridy = y++;
        add(cbSubfaction, gbc);
        cbSubfaction.setToolTipText(Messages.getString("ForceGeneratorDialog.subfaction.tooltip")); //$NON-NLS-1$
        cbSubfaction.addActionListener(this);

        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.unitType")), gbc);
        cbUnitType = new JComboBox<Integer>();
        cbUnitType.setRenderer(new CBRenderer<Integer>(Messages.getString("ForceGeneratorDialog.combined"), //$NON-NLS-1$
                ut -> UnitType.getTypeName(ut)));
        gbc.gridx = 1;
        gbc.gridy = y;
        add(cbUnitType, gbc);
        cbUnitType.setToolTipText(Messages.getString("ForceGeneratorDialog.unitType.tooltip")); //$NON-NLS-1$
        cbUnitType.addActionListener(this);

        gbc.gridx = 2;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.formation")), gbc); //$NON-NLS-1$
        cbFormation = new JComboBox<>();
        cbFormation.setRenderer(new CBRenderer<String>(Messages.getString("ForceGeneratorDialog.random"), //$NON-NLS-1$
                f -> formationDisplayNames.get(f)));
        gbc.gridx = 3;
        gbc.gridy = y++;
        add(cbFormation, gbc);
        cbFormation.setToolTipText(Messages.getString("ForceGeneratorDialog.formation.tooltip")); //$NON-NLS-1$
        cbFormation.addActionListener(this);

        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.rating")), gbc); //$NON-NLS-1$
        cbRating = new JComboBox<>();
        cbRating.setRenderer(new CBRenderer<String>(Messages.getString("ForceGeneratorDialog.random"), //$NON-NLS-1$
                r -> ratingDisplayNames.get(r)));
        gbc.gridx = 1;
        gbc.gridy = y;
        add(cbRating, gbc);
        cbRating.setToolTipText(Messages.getString("ForceGeneratorDialog.rating.tooltip")); //$NON-NLS-1$
        cbRating.addActionListener(this);

        gbc.gridx = 2;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.weight")), gbc); //$NON-NLS-1$
        cbWeightClass = new JComboBox<Integer>();
        cbWeightClass.setRenderer(new CBRenderer<Integer>(Messages.getString("ForceGeneratorDialog.random"), //$NON-NLS-1$
                wc -> EntityWeightClass.getClassName(wc)));
        cbWeightClass.addItem(null);
        cbWeightClass.addItem(EntityWeightClass.WEIGHT_LIGHT);
        cbWeightClass.addItem(EntityWeightClass.WEIGHT_MEDIUM);
        cbWeightClass.addItem(EntityWeightClass.WEIGHT_HEAVY);
        cbWeightClass.addItem(EntityWeightClass.WEIGHT_ASSAULT);
        gbc.gridx = 3;
        gbc.gridy = y++;
        add(cbWeightClass, gbc);
        cbWeightClass.setToolTipText(Messages.getString("ForceGeneratorDialog.weight.tooltip")); //$NON-NLS-1$
        cbWeightClass.addActionListener(this);

        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.other")), gbc); //$NON-NLS-1$
        cbFlags = new JComboBox<>();
        cbFlags.setRenderer(new CBRenderer<String>("---",
                f -> flagDisplayNames.get(f)));
        gbc.gridx = 1;
        gbc.gridy = y;
        add(cbFlags, gbc);
        cbFlags.setToolTipText(Messages.getString("ForceGeneratorDialog.other.tooltip")); //$NON-NLS-1$
        cbFlags.addActionListener(this);

        gbc.gridx = 2;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.experience")), gbc); //$NON-NLS-1$
        cbExperience = new JComboBox<String>();
        cbExperience.addItem(Messages.getString("ForceGeneratorDialog.random")); //$NON-NLS-1$
        cbExperience.addItem(Messages.getString("ForceGeneratorDialog.green")); //$NON-NLS-1$
        cbExperience.addItem(Messages.getString("ForceGeneratorDialog.regular")); //$NON-NLS-1$
        cbExperience.addItem(Messages.getString("ForceGeneratorDialog.veteran")); //$NON-NLS-1$
        cbExperience.addItem(Messages.getString("ForceGeneratorDialog.elite")); //$NON-NLS-1$
        gbc.gridx = 3;
        gbc.gridy = y++;
        add(cbExperience, gbc);
        cbExperience.setToolTipText(Messages.getString("ForceGeneratorDialog.experience.tooltip")); //$NON-NLS-1$
        cbExperience.addActionListener(this);

        gbc.gridx = 0;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        chkAttachments = new JCheckBox(Messages.getString("ForceGeneratorDialog.includeSupportForces")); //$NON-NLS-1$
        chkAttachments.setToolTipText(Messages.getString("ForceGeneratorDialog.includeSupportForces.tooltip")); //$NON-NLS-1$
        chkAttachments.setSelected(true);
        add(chkAttachments, gbc);

        gbc.gridwidth = 4;
        panGroundRole = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = y++;
        add(panGroundRole, gbc);

        panInfRole = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = y++;
        add(panInfRole, gbc);
        panInfRole.setVisible(false);

        panAirRole = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = y++;
        add(panAirRole, gbc);
        panAirRole.setVisible(false);

        gbc.gridx = 0;
        gbc.gridy = y++;

        JPanel panTransport = new JPanel(new GridLayout(3, 2));
        txtDropshipPct = new JTextField("0"); //$NON-NLS-1$
        txtDropshipPct.setToolTipText(Messages.getString("ForceGeneratorDialog.dropshipPercentage.tooltip")); //$NON-NLS-1$
        txtJumpshipPct = new JTextField("0"); //$NON-NLS-1$
        txtJumpshipPct.setToolTipText(Messages.getString("ForceGeneratorDialog.jumpshipPercentage.tooltip")); //$NON-NLS-1$
        txtCargo = new JTextField("0");
        panTransport.add(new JLabel(Messages.getString("ForceGeneratorDialog.dropshipPercentage"))); //$NON-NLS-1$
        panTransport.add(txtDropshipPct, gbc);
        panTransport.add(new JLabel(Messages.getString("ForceGeneratorDialog.jumpshipPercentage"))); //$NON-NLS-1$
        panTransport.add(txtJumpshipPct, gbc);
        // Cargo needs more work to select cargo dropships.
        //        panTransport.add(new JLabel("Cargo Tonnage:"));
        //        panTransport.add(txtCargo, gbc);
        gbc.gridx = 0;
        gbc.gridy = y++;
        gbc.fill = GridBagConstraints.NONE;
        panTransport.setBorder(BorderFactory.createTitledBorder(Messages.getString("ForceGeneratorDialog.transport"))); //$NON-NLS-1$
        add(panTransport, gbc);

        btnGenerate = new JButton(Messages.getString("ForceGeneratorDialog.generate")); //$NON-NLS-1$
        btnGenerate.setToolTipText(Messages.getString("ForceGeneratorDialog.generate.tooltip")); //$NON-NLS-1$
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.weighty = 1.0;
        add(btnGenerate, gbc);
        btnGenerate.addActionListener(this);

        btnExportMUL = new JButton(Messages.getString("ForceGeneratorDialog.exportMUL")); //$NON-NLS-1$
        btnExportMUL.setToolTipText(Messages.getString("ForceGeneratorDialog.exportMUL.tooltip")); //$NON-NLS-1$
        gbc.gridx = 1;
        gbc.gridy = y;
        add(btnExportMUL, gbc);
        btnExportMUL.addActionListener(this);
        btnExportMUL.setEnabled(false);

        btnClear = new JButton(Messages.getString("ForceGeneratorDialog.clear")); //$NON-NLS-1$
        btnClear.setToolTipText(Messages.getString("ForceGeneratorDialog.clear.tooltip")); //$NON-NLS-1$
        gbc.gridx = 2;
        gbc.gridy = y;
        gbc.weighty = 1.0;
        add(btnClear, gbc);
        btnClear.addActionListener(this);
        btnClear.setEnabled(false);

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;

        chkRoleRecon = createMissionRoleCheck(MissionRole.RECON);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panGroundRole.add(chkRoleRecon, gbc);

        chkRoleFireSupport = createMissionRoleCheck(MissionRole.FIRE_SUPPORT);
        gbc.gridx = 1;
        gbc.gridy = 0;
        panGroundRole.add(chkRoleFireSupport, gbc);

        chkRoleUrban = createMissionRoleCheck(MissionRole.URBAN);
        gbc.gridx = 2;
        gbc.gridy = 0;
        panGroundRole.add(chkRoleUrban, gbc);

        chkRoleCavalry = createMissionRoleCheck(MissionRole.CAVALRY);
        gbc.gridx = 3;
        gbc.gridy = 0;
        panGroundRole.add(chkRoleCavalry, gbc);

        chkRoleRaider = createMissionRoleCheck(MissionRole.RAIDER);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panGroundRole.add(chkRoleRaider, gbc);

        chkRoleIncindiary = createMissionRoleCheck(MissionRole.INCENDIARY);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panGroundRole.add(chkRoleIncindiary, gbc);

        chkRoleAntiAircraft = createMissionRoleCheck(MissionRole.ANTI_AIRCRAFT);
        gbc.gridx = 2;
        gbc.gridy = 1;
        panGroundRole.add(chkRoleAntiAircraft, gbc);

        chkRoleAntiInfantry = createMissionRoleCheck(MissionRole.ANTI_INFANTRY);
        gbc.gridx = 3;
        gbc.gridy = 1;
        panGroundRole.add(chkRoleAntiInfantry, gbc);

        chkRoleArtillery = createMissionRoleCheck(MissionRole.ARTILLERY);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panGroundRole.add(chkRoleArtillery, gbc);

        chkRoleMissileArtillery = createMissionRoleCheck(MissionRole.MISSILE_ARTILLERY);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panGroundRole.add(chkRoleMissileArtillery, gbc);

        chkRoleInfantrySupport = createMissionRoleCheck(MissionRole.INF_SUPPORT);
        gbc.gridx = 2;
        gbc.gridy = 2;
        panGroundRole.add(chkRoleInfantrySupport, gbc);

        chkRoleTransport = createMissionRoleCheck(MissionRole.CARGO);
        gbc.gridx = 0;
        gbc.gridy = 3;
        panGroundRole.add(chkRoleTransport, gbc);

        chkRoleEngineer = createMissionRoleCheck(MissionRole.ENGINEER);
        gbc.gridx = 1;
        gbc.gridy = 3;
        panGroundRole.add(chkRoleEngineer, gbc);

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;

        chkRoleFieldGun = createMissionRoleCheck(MissionRole.FIELD_GUN);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panInfRole.add(chkRoleFieldGun, gbc);

        chkRoleFieldArtillery = createMissionRoleCheck(MissionRole.ARTILLERY);
        gbc.gridx = 1;
        gbc.gridy = 0;
        panInfRole.add(chkRoleFieldArtillery, gbc);

        chkRoleFieldMissileArtillery = createMissionRoleCheck(MissionRole.MISSILE_ARTILLERY);
        gbc.gridx = 2;
        gbc.gridy = 0;
        panInfRole.add(chkRoleFieldMissileArtillery, gbc);

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;

        chkRoleAirRecon = createMissionRoleCheck(MissionRole.RECON);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panAirRole.add(chkRoleAirRecon, gbc);

        chkRoleGroundSupport = createMissionRoleCheck(MissionRole.GROUND_SUPPORT);
        gbc.gridx = 1;
        gbc.gridy = 0;
        panAirRole.add(chkRoleGroundSupport, gbc);

        chkRoleInterceptor = createMissionRoleCheck(MissionRole.INTERCEPTOR);
        gbc.gridx = 2;
        gbc.gridy = 0;
        panAirRole.add(chkRoleInterceptor, gbc);

        chkRoleEscort = createMissionRoleCheck(MissionRole.ESCORT);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panAirRole.add(chkRoleEscort, gbc);

        chkRoleBomber = createMissionRoleCheck(MissionRole.BOMBER);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panAirRole.add(chkRoleBomber, gbc);

        chkRoleAssault = createMissionRoleCheck(MissionRole.ASSAULT);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panAirRole.add(chkRoleAssault, gbc);

        chkRoleAirTransport = createMissionRoleCheck(MissionRole.CARGO);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panAirRole.add(chkRoleAirTransport, gbc);

        refreshFactions();
    }

    private JCheckBox createMissionRoleCheck(MissionRole role) {
        String key = "MissionRole." + role.toString().toLowerCase();
        JCheckBox chk = new JCheckBox(Messages.getString(key));
        chk.setToolTipText(Messages.getString(key + ".tooltip"));
        return chk;
    }

    private void generateForce() {
        ForceDescriptor fd = new ForceDescriptor();
        fd.setTopLevel(true);
        fd.setYear(forceDesc.getYear());
        fd.setFaction(forceDesc.getFaction());
        fd.setUnitType(forceDesc.getUnitType());
        fd.setEschelon(forceDesc.getEschelon());
        fd.setAugmented(forceDesc.isAugmented());
        fd.setSizeMod(forceDesc.getSizeMod());
        fd.getFlags().addAll(forceDesc.getFlags());
        fd.setRating(forceDesc.getRating());
        if (forceDesc.getExperience() != null) {
            fd.setExperience(forceDesc.getExperience());
        } else {
            fd.setExperience(CrewDescriptor.randomExperienceLevel());
        }
        fd.setWeightClass(forceDesc.getWeightClass());
        fd.setAttachments(chkAttachments.isSelected());
        if (forceDesc.getUnitType() != null) {
            switch (forceDesc.getUnitType()) {
            case UnitType.MEK:case UnitType.TANK:
                if (chkRoleRecon.isSelected()) {
                    fd.getRoles().add(MissionRole.RECON);
                }
                if (chkRoleFireSupport.isSelected()) {
                    fd.getRoles().add(MissionRole.FIRE_SUPPORT);
                }
                if (chkRoleUrban.isSelected()) {
                    fd.getRoles().add(MissionRole.URBAN);
                }
                if (chkRoleInfantrySupport.isSelected()) {
                    fd.getRoles().add(MissionRole.INF_SUPPORT);
                }
                if (chkRoleCavalry.isSelected()) {
                    fd.getRoles().add(MissionRole.CAVALRY);
                }
                if (chkRoleRaider.isSelected()) {
                    fd.getRoles().add(MissionRole.RAIDER);
                }
                if (chkRoleIncindiary.isSelected()) {
                    fd.getRoles().add(MissionRole.INCENDIARY);
                }
                if (chkRoleAntiAircraft.isSelected()) {
                    fd.getRoles().add(MissionRole.ANTI_AIRCRAFT);
                }
                if (chkRoleAntiInfantry.isSelected()) {
                    fd.getRoles().add(MissionRole.ANTI_INFANTRY);
                }			
                if (chkRoleArtillery.isSelected()) {
                    fd.getRoles().add(MissionRole.ARTILLERY);
                }
                if (chkRoleMissileArtillery.isSelected()) {
                    fd.getRoles().add(MissionRole.MISSILE_ARTILLERY);
                }
                if (chkRoleTransport.isSelected()) {
                    fd.getRoles().add(MissionRole.CARGO);
                }
                if (chkRoleEngineer.isSelected()) {
                    fd.getRoles().add(MissionRole.ENGINEER);
                }
                break;
            case UnitType.INFANTRY:
            case UnitType.BATTLE_ARMOR:
                if (chkRoleFieldGun.isSelected()) {
                    fd.getRoles().add(MissionRole.FIELD_GUN);
                }
                if (chkRoleFieldArtillery.isSelected()) {
                    fd.getRoles().add(MissionRole.ARTILLERY);
                }
                if (chkRoleFieldMissileArtillery.isSelected()) {
                    fd.getRoles().add(MissionRole.MISSILE_ARTILLERY);
                }
                break;
            case UnitType.AERO:
                if (chkRoleAirRecon.isSelected()) {
                    fd.getRoles().add(MissionRole.RECON);
                }
                if (chkRoleGroundSupport.isSelected()) {
                    fd.getRoles().add(MissionRole.GROUND_SUPPORT);
                }
                if (chkRoleInterceptor.isSelected()) {
                    fd.getRoles().add(MissionRole.INTERCEPTOR);
                }
                if (chkRoleAssault.isSelected()) {
                    fd.getRoles().add(MissionRole.ASSAULT);
                }
                if (chkRoleAirTransport.isSelected()) {
                    fd.getRoles().add(MissionRole.CARGO);
                }
            }
        }
        try {
            fd.setDropshipPct(Double.parseDouble(txtDropshipPct.getText()) * 0.01);
        } catch (NumberFormatException ex) {
            fd.setDropshipPct(0.0);
            txtDropshipPct.setText("0");
        }
        try {
            fd.setJumpshipPct(Double.parseDouble(txtJumpshipPct.getText()) * 0.01);
        } catch (NumberFormatException ex) {
            fd.setJumpshipPct(0.0);
            txtJumpshipPct.setText("0");
        }
        try {
            fd.setCargo(Double.parseDouble(txtCargo.getText()));
        } catch (NumberFormatException ex) {
            fd.setCargo(0.0);
            txtCargo.setText("0");
        }

        ProgressMonitor monitor = new ProgressMonitor(this, Messages.getString("ForceGeneratorDialog.generateFormation"), "", 0, 100);
        monitor.setProgress(0);
        GenerateTask task = new GenerateTask(fd);
        task.addPropertyChangeListener(e -> {
            monitor.setProgress(task.getProgress());
            monitor.setNote(task.getMessage());
            if (monitor.isCanceled()) {
                task.cancel(true);
            }
        });
        task.execute();
    }

    private void clearForce() {
        if (null != onGenerate) {
            onGenerate.accept(null);
        }
    }

    private void refreshFactions() {
        FactionRecord oldFaction = (FactionRecord)cbFaction.getSelectedItem();
        cbFaction.removeActionListener(this);
        cbFaction.removeAllItems();
        List<FactionRecord> sorted = RATGenerator.getInstance().getFactionList()
                .stream().filter(fr -> !fr.getKey().contains(".") && fr.isActiveInYear(currentYear))
                .collect(Collectors.toList());
        sorted.sort((fr1, fr2) -> fr1.getName(currentYear).compareTo(fr2.getName(currentYear)));
        sorted.forEach(fr -> cbFaction.addItem(fr));
        cbFaction.setSelectedItem(oldFaction);
        if (cbFaction.getSelectedItem() == null ||
                !cbFaction.getSelectedItem().toString().equals(oldFaction.toString())) {
            cbFaction.setSelectedItem(RATGenerator.getInstance().getFaction("IS"));
        }
        forceDesc.setFaction(cbFaction.getSelectedItem().toString());
        refreshSubfactions();
        cbFaction.addActionListener(this);
    }

    private void refreshSubfactions() {
        FactionRecord oldFaction = (FactionRecord)cbSubfaction.getSelectedItem();
        cbSubfaction.removeActionListener(this);
        cbSubfaction.removeAllItems();
        String currentFaction = ((FactionRecord)cbFaction.getSelectedItem()).getKey();
        if (currentFaction != null) {
            List<FactionRecord> sorted = RATGenerator.getInstance().getFactionList().stream()
                    .filter(fr -> fr.getKey().startsWith(currentFaction + ".")
                            && fr.isActiveInYear(currentYear))
                    .collect(Collectors.toList());
            sorted.sort((fr1, fr2) -> fr1.getName(currentYear).compareTo(fr2.getName(currentYear)));
            cbSubfaction.addItem(null);
            sorted.forEach(fr -> cbSubfaction.addItem(fr));
        }
        cbSubfaction.setSelectedItem(oldFaction);
        if (cbSubfaction.getSelectedItem() == null) {
            forceDesc.setFaction(cbFaction.getSelectedItem().toString());
        } else {
            forceDesc.setFaction(cbSubfaction.getSelectedItem().toString());
        }
        refreshUnitTypes();
        cbSubfaction.addActionListener(this);
    }

    private void refreshUnitTypes() {
        cbUnitType.removeActionListener(this);
        TOCNode tocNode = findTOCNode();
        Integer currentType = forceDesc.getUnitType();
        boolean hasCurrent = false;
        cbUnitType.removeAllItems();
        if (tocNode != null) {
            ValueNode n = tocNode.findUnitTypes(forceDesc);
            if (n != null) {
                for (String unitType : n.getContent().split(",")) {
                    if (unitType.equals("null")) {
                        cbUnitType.addItem(null);
                        if (currentType == null) {
                            hasCurrent = true;
                        }
                    } else {
                        cbUnitType.addItem(AbstractUnitRecord.parseUnitType(unitType));
                        if (currentType != null && UnitType.getTypeDisplayableName(currentType).equals(unitType)) {
                            hasCurrent = true;
                        }
                    }
                }
            } else {
                MegaMek.getLogger().warning("No unit type node found.");
                cbUnitType.addItem(null);
            }
        } else {
            cbUnitType.addItem(null);
        }

        if (hasCurrent) {
            cbUnitType.setSelectedItem(currentType);
        } else {
            Ruleset rs = Ruleset.findRuleset(forceDesc.getFaction());
            Integer unitType = rs.getDefaultUnitType(forceDesc);
            if (unitType == null && cbUnitType.getItemCount() > 0) {
                unitType = (Integer)cbUnitType.getItemAt(0);
            }
            cbUnitType.setSelectedItem(unitType);
            forceDesc.setUnitType(unitType);
        }
        refreshFormations();
        cbUnitType.addActionListener(this);
    }

    private void refreshFormations() {
        cbFormation.removeActionListener(this);
        if (cbUnitType.getSelectedItem() != null) {
            Integer unitType = (Integer)cbUnitType.getSelectedItem();
            if (unitType != null) {
                panGroundRole.setVisible(unitType == UnitType.MEK || unitType == UnitType.TANK);
                panInfRole.setVisible(unitType == UnitType.INFANTRY
                        || unitType == UnitType.BATTLE_ARMOR);
                panAirRole.setVisible(unitType == UnitType.AERO
                        || unitType == UnitType.CONV_FIGHTER);
            }
        }

        TOCNode tocNode = findTOCNode();
        String currentFormation = (String)cbFormation.getSelectedItem();
        boolean hasCurrent = false;
        Ruleset ruleset = Ruleset.findRuleset(forceDesc);
        cbFormation.removeAllItems();

        if (tocNode != null) {
            ValueNode n = tocNode.findEschelons(forceDesc);
            if (n != null) {
                formationDisplayNames.clear();
                for (String formation : n.getContent().split(",")) {
                    Ruleset rs = ruleset;
                    ForceNode fn = null;
                    do {
                        fn = rs.findForceNode(forceDesc,
                                Integer.parseInt(formation.replaceAll("[^0-9]", "")),
                                formation.endsWith("^"));
                        if (fn == null) {
                            if (rs.getParent() != null) {
                                rs = Ruleset.findRuleset(rs.getParent());
                            } else {
                                rs = null;
                            }
                        }
                    } while (fn == null && rs != null);
                    String formName = (fn != null)?fn.getEschelonName() : formation;
                    if (formation.endsWith("+")) {
                        formName = Messages.getString("ForceGeneratorDialog.reinforced") + formName;
                    }
                    if (formation.endsWith("-")) {
                        formName = Messages.getString("ForceGeneratorDialog.understrength") + formName;
                    }
                    formationDisplayNames.put(formation, formName);
                    cbFormation.addItem(formation);
                    if (currentFormation != null && currentFormation.equals(formation)) {
                        hasCurrent = true;
                    }
                }
            }
        } else {
            MegaMek.getLogger().warning("No eschelon node found.");
        }

        if (hasCurrent) {
            cbFormation.setSelectedItem(currentFormation);
        } else {
            Ruleset rs = Ruleset.findRuleset(forceDesc.getFaction());
            String esch = rs.getDefaultEschelon(forceDesc);
            if ((esch == null || !formationDisplayNames.containsKey(esch)
                    && cbFormation.getItemCount() > 0)) {
                esch = (String)cbFormation.getItemAt(0);
            }
            if (esch != null) {
                cbFormation.setSelectedItem(esch);
                setFormation(esch);
            }
        }

        refreshRatings();
        cbFormation.addActionListener(this);
    }

    private void refreshRatings() {
        cbRating.removeActionListener(this);
        TOCNode tocNode = findTOCNode();
        String currentRating = forceDesc.getRating();
        boolean hasCurrent = false;
        cbRating.removeAllItems();
        ratingDisplayNames.clear();
        if (tocNode != null) {
            ValueNode n = tocNode.findRatings(forceDesc);
            if (n != null && n.getContent() != null) {
                cbRating.addItem(null);
                for (String rating : n.getContent().split(",")) {
                    if (rating.contains(":")) {
                        String[] fields = rating.split(":");
                        cbRating.addItem(fields[0]);
                        ratingDisplayNames.put(fields[0], fields[1]);
                    } else {
                        cbRating.addItem(rating);
                        ratingDisplayNames.put(rating, rating);
                    }
                }
            } else {
                MegaMek.getLogger().warning("No rating found.");
            }
        }

        if (hasCurrent) {
            cbRating.setSelectedItem(currentRating);
        } else {
            Ruleset rs = Ruleset.findRuleset(forceDesc.getFaction());
            String rating = rs.getDefaultRating(forceDesc);
            if (rating == null && cbRating.getItemCount() > 0) {
                rating = cbRating.getItemAt(0);
            }
            if (rating != null) {
                cbRating.setSelectedItem(rating);
                forceDesc.setRating(rating);
            }
        }
        refreshFlags();
        cbRating.addActionListener(this);
    }

    private void refreshFlags() {
        cbFlags.removeActionListener(this);
        TOCNode tocNode = findTOCNode();
        String currentFlag = (String)cbFlags.getSelectedItem();
        boolean hasCurrent = false;
        cbFlags.removeAllItems();
        cbFlags.addItem(null);
        if (tocNode != null) {
            ValueNode n = tocNode.findFlags(forceDesc);
            if (n != null && n.getContent() != null) {
                for (String flag : n.getContent().split(",")) {
                    if (flag.contains(":")) {
                        String[] fields = flag.split(":");
                        flagDisplayNames.put(fields[0], fields[1]);
                        cbFlags.addItem(fields[0]);
                    } else {
                        flagDisplayNames.put(flag, flag);
                        cbFlags.addItem(flag);
                    }
                }
            }
        }

        if (hasCurrent) {
            cbFlags.setSelectedItem(currentFlag);
        } else {
            cbFlags.setSelectedIndex(0);
        }
        forceDesc.getFlags().clear();
        if (cbFlags.getSelectedItem() != null) {
            forceDesc.getFlags().add((String)cbFlags.getSelectedItem());
        }
        cbFlags.addActionListener(this);
    }

    private TOCNode findTOCNode() {
        Ruleset rs = Ruleset.findRuleset(forceDesc);
        if (null == rs) {
            return null;
        }
        TOCNode toc = null;
        do {
            toc = rs.getTOCNode();
            if (toc == null) {
                if (rs.getParent() == null) {
                    rs = null;
                } else {
                    rs = Ruleset.findRuleset(rs.getParent());
                }				
            }
        } while (rs != null && toc == null);
        return toc;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource() == cbFaction) {
            if (cbFaction.getSelectedItem() != null) {
                forceDesc.setFaction(((FactionRecord)cbFaction.getSelectedItem()).getKey());
            }
            refreshSubfactions();
        } else if (ev.getSource() == cbSubfaction) {
            if (cbSubfaction.getSelectedItem() != null) {
                forceDesc.setFaction(((FactionRecord)cbSubfaction.getSelectedItem()).getKey());
            } else {
                forceDesc.setFaction(((FactionRecord)cbFaction.getSelectedItem()).getKey());
            }
            refreshUnitTypes();
        } else if (ev.getSource() == cbUnitType) {
            forceDesc.setUnitType((Integer)cbUnitType.getSelectedItem());
            refreshFormations();
        } else if (ev.getSource() == cbFormation) {
            String esch = (String)cbFormation.getSelectedItem();
            if (esch != null) {
                setFormation(esch);
            }
            refreshRatings();
        } else if (ev.getSource() == cbRating) {
            forceDesc.setRating((String)cbRating.getSelectedItem());
            refreshFlags();
        } else if (ev.getSource() == cbExperience) {
            if (cbExperience.getSelectedIndex() == 0) {
                forceDesc.setExperience(null);
            } else {
                forceDesc.setExperience(cbExperience.getSelectedIndex() - 1);
            }
            refreshFlags();
        } else if (ev.getSource() == cbFlags) {
            forceDesc.getFlags().clear();
            if (cbFlags.getSelectedItem() != null) {
                forceDesc.getFlags().add((String)cbFlags.getSelectedItem());
            }
        } else if (ev.getSource() == cbWeightClass) {
            if (cbWeightClass.getSelectedIndex() < 1) {
                forceDesc.setWeightClass(null);
            } else {
                forceDesc.setWeightClass(cbWeightClass.getSelectedIndex());
            }
        } else if (ev.getSource() == btnGenerate) {
            generateForce();
            btnExportMUL.setEnabled(true);
            btnClear.setEnabled(true);
        } else if (ev.getSource() == btnExportMUL) {
            exportMUL(forceDesc);
        } else if (ev.getSource() == btnClear) {
            clearForce();
            btnExportMUL.setEnabled(false);
            btnClear.setEnabled(false);
        }
    }

    void exportMUL(ForceDescriptor fd) {
        ArrayList<Entity> list = new ArrayList<>();
        fd.addAllEntities(list);
        //Create a fake game so we can write the entities to a file without adding them to the real game.
        Game game = new Game();
        //Add a player to prevent complaining in the log file
        Player p = new Player(1, "Observer");
        game.addPlayer(1, p);
        game.setOptions(clientGui.getClient().getGame().getOptions());
        list.stream().forEach(en -> {
            en.setOwner(p);
            // If we don't set the id, the first unit will be left at -1, which in most cases is interpreted
            // as no entity
            en.setId(game.getNextEntityId());
            game.addEntity(en);
        });
        configureNetworks(fd);
        clientGui.saveListFile(list, clientGui.getClient().getLocalPlayer().getName());
    }

    /**
     * Searches recursively for nodes that are flagged with C3 networks and configures them.
     * 
     * @param fd
     */
    private void configureNetworks(ForceDescriptor fd) {
        if (fd.getFlags().contains("c3")) {
            Entity master = fd.getSubforces().stream().map(ForceDescriptor::getEntity)
                    .filter(en -> (null != en)
                            && (en.hasC3M() || en.hasC3MM()))
                    .findFirst().orElse(null);
            if (null != master) {
                int c3s = 0;
                for (ForceDescriptor sf : fd.getSubforces()) {
                    if ((null != sf.getEntity())
                            && (sf.getEntity().getId() != master.getId())
                            && sf.getEntity().hasC3S()) {
                        sf.getEntity().setC3Master(master, false);
                        c3s++;
                        if (c3s == 3) {
                            break;
                        }
                    }
                }
            }
        } else {
            // Even if we haven't reworked this into a full C3i network, we can still connect
            // any C3i units that happen to be present.
            Entity first = null;
            int nodes = 0;
            for (ForceDescriptor sf : fd.getSubforces()) {
                if ((null != sf.getEntity())
                        && sf.getEntity().hasC3i()) {
                    sf.getEntity().setC3UUID();
                    if (null == first) {
                        sf.getEntity().setC3NetIdSelf();
                        first = sf.getEntity();
                        nodes++;
                    } else {
                        sf.getEntity().setC3NetId(first);
                        nodes++;
                    }
                }
                if (nodes >= Entity.MAX_C3i_NODES) {
                    break;
                }
            }
        }
        fd.getSubforces().forEach(sf -> configureNetworks(sf));
        fd.getAttached().forEach(sf -> configureNetworks(sf));
    }

    private void setFormation(String esch) {
        forceDesc.setEschelon(Integer.parseInt(esch.replaceAll("[^0-9]", "")));
        forceDesc.setAugmented(esch.contains("^"));
        if (esch.endsWith("+")) {
            forceDesc.setSizeMod(1);
        } else if (esch.endsWith("-")) {
            forceDesc.setSizeMod(-1);
        } else {
            forceDesc.setSizeMod(0);
        }
    }		

    public void setCurrentYear(int year) {
        currentYear = year;
        yearUpdated();	    
    }

    /**
     * Worker function that updates various things that need to be updated when the year is changed.
     */
    private void yearUpdated() {
        txtYear.setText(String.valueOf(currentYear));
        RATGenerator.getInstance().loadYear(currentYear);
        forceDesc.setYear(currentYear);
        refreshFactions();
    }

    @Override
    public void focusGained(FocusEvent arg0) {
        //Do nothing
    }

    @Override
    public void focusLost(FocusEvent arg0) {
        try {
            currentYear = Integer.parseInt(txtYear.getText());
            if (currentYear < RATGenerator.getInstance().getEraSet().first()) {
                currentYear = RATGenerator.getInstance().getEraSet().first();
            } else if (currentYear > RATGenerator.getInstance().getEraSet().last()) {
                currentYear = RATGenerator.getInstance().getEraSet().last();
            }
        } catch (NumberFormatException ex) {
            //ignore and restore to previous value
        }
        yearUpdated();
    }

    static class CBRenderer<T> extends DefaultListCellRenderer {

        private static final long serialVersionUID = 4895258839502183158L;

        private String nullVal = Messages.getString("ForceGeneratorDialog.default");
        private Function<T,String> toString;

        public CBRenderer(String nullVal) {
            this(nullVal, null);
        }

        public CBRenderer(String nullVal, Function<T,String> strConverter) {
            this.nullVal = nullVal;
            if (strConverter == null) {
                toString = obj -> obj.toString();
            } else {
                toString = strConverter;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Component getListCellRendererComponent(JList<? extends Object> list, Object entry,
                int position, boolean arg3, boolean arg4) {
            if (entry == null) {
                setText(nullVal);
            } else {
                setText(toString.apply((T)entry));
            }
            return this;
        }
    };

    static class ForceTreeModel implements TreeModel {

        private ForceDescriptor root;
        private ArrayList<TreeModelListener> listeners;

        public ForceTreeModel(ForceDescriptor root) {
            this.root = root;
            listeners = new ArrayList<TreeModelListener>();		
        }

        @Override
        public void addTreeModelListener(TreeModelListener listener) {
            if (null != listener && !listeners.contains(listener)) {
                listeners.add(listener);
            }
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (parent instanceof ForceDescriptor) {
                return ((ForceDescriptor)parent).getAllChildren().get(index);
            }
            return null;
        }

        @Override
        public int getChildCount(Object parent) {
            if (parent instanceof ForceDescriptor) {
                return ((ForceDescriptor)parent).getAllChildren().size();
            }
            return 0;
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            if (parent instanceof ForceDescriptor) {
                return ((ForceDescriptor)parent).getAllChildren().indexOf(child);
            }
            return 0;
        }

        @Override
        public Object getRoot() {
            return root;
        }

        @Override
        public boolean isLeaf(Object node) {
            return ((ForceDescriptor)node).getEschelon() == 0
                    || (node instanceof ForceDescriptor && getChildCount(node) == 0);
        }

        @Override
        public void removeTreeModelListener(TreeModelListener listener) {
            if (null != listener) {
                listeners.remove(listener);
            }
        }

        @Override
        public void valueForPathChanged(TreePath arg0, Object arg1) {
            // TODO Auto-generated method stub

        }

    }

    static class UnitRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = -5915350078441133119L;

        public UnitRenderer() {

        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            super.getTreeCellRendererComponent(
                    tree, value, sel,
                    expanded, leaf, row,
                    hasFocus);
            setBackground(UIManager.getColor("Tree.textBackground"));
            setForeground(UIManager.getColor("Tree.textForeground"));
            if(sel) {
                setBackground(UIManager.getColor("Tree.selectionBackground"));
                setForeground(UIManager.getColor("Tree.selectionForeground"));
            }

            ForceDescriptor fd = (ForceDescriptor)value;
            if (fd.isElement()) {
                StringBuilder name = new StringBuilder();
                String uname = "";
                if (fd.getCo() == null) {
                    name.append("<font color='red'>")
                    .append(Messages.getString("ForceGeneratorDialog.noCrew"))
                    .append("</font>");
                } else {
                    name.append(fd.getCo().getName());
                    name.append(" (").append(fd.getCo().getGunnery()).append("/").append(fd.getCo().getPiloting()).append(")");
                }
                uname = "<i>" + fd.getModelName() + "</i>";
                if (fd.getFluffName() != null) {
                    uname += "<br /><i>" + fd.getFluffName() + "</i>";
                }
                setText("<html>" + name.toString() + ", " + uname + "</html>");
            } else {
                StringBuilder desc = new StringBuilder("<html>");
                desc.append(fd.parseName()).append("<br />").append(fd.getDescription());
                if (fd.getCo() != null) {
                    desc.append("<br />").append(fd.getCo().getTitle() == null?"CO":fd.getCo().getTitle());
                    desc.append(fd.getCo().getName());
                }
                if (fd.getXo() != null) {
                    desc.append("<br />").append(fd.getXo().getTitle() == null?"XO":fd.getXo().getTitle());
                    desc.append(fd.getXo().getName());
                }
                setText(desc.append("</html>").toString());
            }
            return this;
        }
    }

    private class GenerateTask extends SwingWorker<ForceDescriptor,Double> implements Ruleset.ProgressListener {

        private ForceDescriptor fd;

        private final Object progressLock = new Object();
        private double progress = 0;
        private String message = "";

        GenerateTask(ForceDescriptor fd) {
            this.fd = fd;
        }

        @Override
        protected ForceDescriptor doInBackground() throws Exception {
            btnGenerate.setEnabled(false);
            Ruleset.findRuleset(fd).processRoot(fd, this);
            return fd;
        }

        @Override
        protected void done() {
            try {
                forceDesc = get();
                if (onGenerate != null) {
                    onGenerate.accept(forceDesc);
                }
            } catch (InterruptedException ex) {
                //Ignore
            } catch (ExecutionException e) {
                e.getCause().printStackTrace();
            } finally {
                btnGenerate.setEnabled(true);
            }
        }

        @Override
        public void updateProgress(double progress, String message) {
            int progressPercent;
            synchronized (progressLock) {
                this.progress += progress;
                this.message = message;

                progressPercent = Math.min((int)Math.round(this.progress * 100.0), 100);
            }

            setProgress(progressPercent);
        }

        public String getMessage() {
            synchronized (progressLock) {
                return message;
            }
        }
    }
}
