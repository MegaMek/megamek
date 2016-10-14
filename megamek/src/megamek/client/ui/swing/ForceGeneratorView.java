/**
 * 
 */
package megamek.client.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import megamek.client.ratgenerator.AbstractUnitRecord;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.ForceNode;
import megamek.client.ratgenerator.MissionRole;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.Ruleset;
import megamek.client.ratgenerator.TOCNode;
import megamek.client.ratgenerator.ValueNode;
import megamek.common.IGame;
import megamek.common.UnitType;

/**
 * Controls to set options for force generator.
 * 
 * @author Neoancient
 *
 */


public class ForceGeneratorView extends JPanel implements FocusListener {
	
	private static final long serialVersionUID = 5269823128861856001L;
	
	private IGame game;
	private int currentYear;
	private Consumer<ForceDescriptor> onGenerate = null;

	private ForceDescriptor forceDesc = new ForceDescriptor();
	
	private JTextField txtYear;
	private KeyValueComboBox cbFaction;
	private KeyValueComboBox cbSubfaction;
	private JComboBox<String> cbUnitType;	
	private KeyValueComboBox cbFormation;
	private KeyValueComboBox cbRating;
	private KeyValueComboBox cbFlags;

	private JComboBox<String> cbExperience;
	private JComboBox<String> cbWeightClass;
	
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

	private JButton btnGenerate;
	
	public ForceGeneratorView(IGame game, Consumer<ForceDescriptor> onGenerate) {
		this.game = game;
		this.onGenerate = onGenerate;
		if (!Ruleset.isInitialized()) {
			Ruleset.loadData();
		}
		initUi();
	}
	
	private void initUi() {
		currentYear = game.getOptions().intOption("year");
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
		add(new JLabel("Year:"), gbc);
		txtYear = new JTextField();
		txtYear.setEditable(true);
		txtYear.setText(Integer.toString(currentYear));
		gbc.gridx = 1;
		gbc.gridy = y++;
		add(txtYear, gbc);
		txtYear.addFocusListener(this);
		gbc.gridx = 0;
		gbc.gridy = y;
		add(new JLabel("Faction:"), gbc);
		cbFaction = new KeyValueComboBox("General");
		gbc.gridx = 1;
		gbc.gridy = y;
		add(cbFaction, gbc);
		
		gbc.gridx = 2;
		gbc.gridy = y;
		add(new JLabel("Subfaction:"), gbc);
		cbSubfaction = new KeyValueComboBox("General");
		gbc.gridx = 3;
		gbc.gridy = y++;
		add(cbSubfaction, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = y;
		add(new JLabel("Unit Type:"), gbc);
		cbUnitType = new JComboBox<String>();
		cbUnitType.setRenderer(unitTypeCbRenderer);
		gbc.gridx = 1;
		gbc.gridy = y;
		add(cbUnitType, gbc);
		
		gbc.gridx = 2;
		gbc.gridy = y;
		add(new JLabel("Formation:"), gbc);
		cbFormation = new KeyValueComboBox("Default");
		gbc.gridx = 3;
		gbc.gridy = y++;
		add(cbFormation, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = y;
		add(new JLabel("Rating:"), gbc);
		cbRating = new KeyValueComboBox("Random");
		gbc.gridx = 1;
		gbc.gridy = y;
		add(cbRating, gbc);
		
		gbc.gridx = 2;
		gbc.gridy = y;
		add(new JLabel("Weight:"), gbc);
		cbWeightClass = new JComboBox<String>();
		cbWeightClass.addItem("Random");
		cbWeightClass.addItem("Light");
		cbWeightClass.addItem("Medium");
		cbWeightClass.addItem("Heavy");
		cbWeightClass.addItem("Assault");
		gbc.gridx = 3;
		gbc.gridy = y++;
		add(cbWeightClass, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = y;
		add(new JLabel("Other:"), gbc);
		cbFlags = new KeyValueComboBox("---");
		gbc.gridx = 1;
		gbc.gridy = y;
		add(cbFlags, gbc);
		
		gbc.gridx = 2;
		gbc.gridy = y;
		add(new JLabel("Experience:"), gbc);
		cbExperience = new JComboBox<String>();
		cbExperience.addItem("Random");
		cbExperience.addItem("Green");
		cbExperience.addItem("Regular");
		cbExperience.addItem("Veteran");
		cbExperience.addItem("Elite");
		gbc.gridx = 3;
		gbc.gridy = y++;
		add(cbExperience, gbc);
		
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

		gbc.gridwidth = 1;
		btnGenerate = new JButton("Generate");
		gbc.gridx = 0;
		gbc.gridy = y;
		gbc.gridwidth = 1;
		gbc.weighty = 1.0;
		add(btnGenerate, gbc);
		btnGenerate.addActionListener(ev -> generateForce());

		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		
		chkRoleRecon = new JCheckBox("Reconnaisance");
		gbc.gridx = 0;
		gbc.gridy = 0;
		panGroundRole.add(chkRoleRecon, gbc);
		
		chkRoleFireSupport = new JCheckBox("Fire Support");
		gbc.gridx = 1;
		gbc.gridy = 0;
		panGroundRole.add(chkRoleFireSupport, gbc);
		
		chkRoleUrban = new JCheckBox("Urban");
		gbc.gridx = 2;
		gbc.gridy = 0;
		panGroundRole.add(chkRoleUrban, gbc);
		
		chkRoleCavalry = new JCheckBox("Cavalry");
		gbc.gridx = 3;
		gbc.gridy = 0;
		panGroundRole.add(chkRoleCavalry, gbc);
		
		chkRoleRaider = new JCheckBox("Raider");
		gbc.gridx = 0;
		gbc.gridy = 1;
		panGroundRole.add(chkRoleRaider, gbc);
		
		chkRoleIncindiary = new JCheckBox("Incindiary");
		gbc.gridx = 1;
		gbc.gridy = 1;
		panGroundRole.add(chkRoleIncindiary, gbc);
		
		chkRoleAntiAircraft = new JCheckBox("Anti-Aircraft");
		gbc.gridx = 2;
		gbc.gridy = 1;
		panGroundRole.add(chkRoleAntiAircraft, gbc);
		
		chkRoleAntiInfantry = new JCheckBox("Anti-Infantry");
		gbc.gridx = 3;
		gbc.gridy = 1;
		panGroundRole.add(chkRoleAntiInfantry, gbc);
		
		chkRoleArtillery = new JCheckBox("Artillery");
		gbc.gridx = 0;
		gbc.gridy = 2;
		panGroundRole.add(chkRoleArtillery, gbc);
		
		chkRoleMissileArtillery = new JCheckBox("Missile Artillery");
		gbc.gridx = 1;
		gbc.gridy = 2;
		panGroundRole.add(chkRoleMissileArtillery, gbc);
		
		chkRoleInfantrySupport = new JCheckBox("Infantry Support");
		gbc.gridx = 2;
		gbc.gridy = 2;
		panGroundRole.add(chkRoleInfantrySupport, gbc);
		
		chkRoleTransport = new JCheckBox("Transport");
		gbc.gridx = 0;
		gbc.gridy = 3;
		panGroundRole.add(chkRoleTransport, gbc);
		
		chkRoleEngineer = new JCheckBox("Engineer");
		gbc.gridx = 1;
		gbc.gridy = 3;
		panGroundRole.add(chkRoleEngineer, gbc);
		
		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		
		chkRoleFieldGun = new JCheckBox("Field Gun");
		gbc.gridx = 0;
		gbc.gridy = 0;
		panInfRole.add(chkRoleFieldGun, gbc);
		
		chkRoleFieldArtillery = new JCheckBox("Field Artillery");
		gbc.gridx = 1;
		gbc.gridy = 0;
		panInfRole.add(chkRoleFieldArtillery, gbc);
		
		chkRoleFieldMissileArtillery = new JCheckBox("Missile Artillery");
		gbc.gridx = 2;
		gbc.gridy = 0;
		panInfRole.add(chkRoleFieldMissileArtillery, gbc);
		
		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		
		chkRoleAirRecon = new JCheckBox("Recon");
		gbc.gridx = 0;
		gbc.gridy = 0;
		panAirRole.add(chkRoleAirRecon, gbc);
		
		chkRoleGroundSupport = new JCheckBox("Ground Support");
		gbc.gridx = 1;
		gbc.gridy = 0;
		panAirRole.add(chkRoleGroundSupport, gbc);
		
		chkRoleInterceptor = new JCheckBox("Interceptor");
		gbc.gridx = 2;
		gbc.gridy = 0;
		panAirRole.add(chkRoleInterceptor, gbc);
				
		chkRoleEscort = new JCheckBox("Escort");
		gbc.gridx = 0;
		gbc.gridy = 1;
		panAirRole.add(chkRoleEscort, gbc);
				
		chkRoleBomber = new JCheckBox("Bomber");
		gbc.gridx = 1;
		gbc.gridy = 1;
		panAirRole.add(chkRoleBomber, gbc);
				
		chkRoleAssault = new JCheckBox("Dropship Assault");
		gbc.gridx = 0;
		gbc.gridy = 2;
		panAirRole.add(chkRoleAssault, gbc);
		
		chkRoleAirTransport = new JCheckBox("Transport");
		gbc.gridx = 1;
		gbc.gridy = 2;
		panAirRole.add(chkRoleAirTransport, gbc);
		
		refreshFactions();
		addListeners();
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
		fd.setWeightClass(forceDesc.getWeightClass());
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
					fd.getRoles().add(MissionRole.INCINDIARY);
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
		
		Ruleset.findRuleset(fd).process(fd);

		forceDesc = fd;
		if (onGenerate != null) {
			onGenerate.accept(fd);
		}
	}
	
	public ForceDescriptor getForceDescriptor() {
		return forceDesc;
	}
	
	private void refreshFactions() {
		String oldFaction = forceDesc.getFaction();
		cbFaction.removeAllItems();
		for (FactionRecord fRec : RATGenerator.getInstance().getFactionList()) {
			if (!fRec.getKey().contains(".") && fRec.isActiveInYear(currentYear)) {
				cbFaction.addItem(fRec.getKey(), fRec.getName(forceDesc.getYear()));
			}
		}
		
		if (oldFaction != null) {
			cbFaction.setSelectedItem(oldFaction.split("\\.")[0]);			
		} else {
			forceDesc.setFaction((cbFaction.getSelectedKey()));
		}
		if (cbFaction.getSelectedIndex() < 0) {
			cbFaction.setSelectedIndex(0);
		}
		refreshSubfactions();
	}
	
	private void refreshSubfactions() {
		String oldFaction = forceDesc.getFaction();
		cbSubfaction.removeAllItems();
		if (forceDesc.getFactionRec() != null) {
			cbSubfaction.addItem(null);
			if (cbFaction.getSelectedKey() != null) {
				String currentFaction = cbFaction.getSelectedKey();
				for (FactionRecord fRec : RATGenerator.getInstance().getFactionList()) {
					if (fRec.getPctSalvage(currentYear) != null &&
							fRec.getKey().startsWith(currentFaction + ".")) {
						cbSubfaction.addItem(fRec.getKey(), fRec.getName(forceDesc.getYear()));
					}
				}
			}
			if (oldFaction != null) {
				cbSubfaction.setSelectedItem(oldFaction.contains(".")?oldFaction:null);
			} else {
				cbSubfaction.setSelectedItem(null);
			}
		} else {
			System.out.println("factionrec is null");
		}
		if (cbSubfaction.getSelectedIndex() < 0) {
			cbSubfaction.setSelectedIndex(0);
		}
		refreshUnitTypes();
	}
	
	private void refreshUnitTypes() {
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
						cbUnitType.addItem(unitType);
						if (currentType != null && currentType.equals(unitType)) {
							hasCurrent = true;
						}
					}
				}
			} else {
				System.out.println("No unit type node found.");
				cbUnitType.addItem(null);
			}
		} else {
			cbUnitType.addItem(null);
		}
		
		if (hasCurrent) {
			cbUnitType.setSelectedItem(currentType);
		} else {
			Ruleset rs = Ruleset.findRuleset(forceDesc.getFaction());
			String unitType = rs.getDefaultUnitType(forceDesc);
			if (unitType == null && cbUnitType.getItemCount() > 0) {
				unitType = cbUnitType.getItemAt(0);
			}
			if (unitType != null) {
				cbUnitType.setSelectedItem(unitType);
				forceDesc.setUnitType(AbstractUnitRecord.parseUnitType(unitType));
			}
		}
		refreshFormations();
	}
	
	private void refreshFormations() {
		if (cbUnitType.getSelectedItem() != null) {
			String unitType = (String)cbUnitType.getSelectedItem();
			panGroundRole.setVisible(unitType.equals("Mek") || unitType.equals("Tank"));
			panInfRole.setVisible(unitType.equals("Infantry"));
			panAirRole.setVisible(unitType.equals("Aero"));
		}
		
		TOCNode tocNode = findTOCNode();
		String currentFormation = cbFormation.getSelectedKey();
		boolean hasCurrent = false;
		Ruleset ruleset = Ruleset.findRuleset(forceDesc);
		cbFormation.removeAllItems();
		
		if (tocNode != null) {
			ValueNode n = tocNode.findEschelons(forceDesc);
			if (n != null) {
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
						formName = "Reinforced " + formName;
					}
					if (formation.endsWith("-")) {
						formName = "Understrength " + formName;
					}
					cbFormation.addItem(formation, formName);
					if (currentFormation != null && currentFormation.equals(formation)) {
						hasCurrent = true;
					}
				}
			}
		} else {
			System.out.println("No eschelon node found.");
		}
		
		if (hasCurrent) {
			cbFormation.setSelectedItem(currentFormation);
		} else {
			Ruleset rs = Ruleset.findRuleset(forceDesc.getFaction());
			String esch = rs.getDefaultEschelon(forceDesc);
			if ((esch == null || !cbFormation.containsKey(esch)
					&& cbFormation.getItemCount() > 0)) {
				esch = cbFormation.getKeyAt(0);
			}
			if (esch != null) {
				cbFormation.setSelectedItem(esch);
				setFormation(esch);
			}
		}
		
		refreshRatings();
	}
	
	private void refreshRatings() {
		TOCNode tocNode = findTOCNode();
		String currentRating = forceDesc.getRating();
		boolean hasCurrent = false;
		cbRating.removeAllItems();
		cbRating.addItem(null);
		if (tocNode != null) {
			ValueNode n = tocNode.findRatings(forceDesc);
			if (n != null && n.getContent() != null) {
				for (String rating : n.getContent().split(",")) {
					if (rating.contains(":")) {
						String[] fields = rating.split(":");
						cbRating.addItem(fields[0],fields[1]);
					} else {
						cbRating.addItem(rating, rating);
					}
				}
			} else {
				System.out.println("No rating found.");
			}
		}
		
		if (hasCurrent) {
			cbRating.setSelectedItem(currentRating);
		} else {
			Ruleset rs = Ruleset.findRuleset(forceDesc.getFaction());
			String rating = rs.getDefaultRating(forceDesc);
			if (rating == null && cbRating.getItemCount() > 0) {
				rating = cbRating.getKeyAt(0);
			}
			if (rating != null) {
				cbRating.setSelectedItem(rating);
				forceDesc.setRating(rating);
			}
		}
		refreshFlags();
	}
	
	private void refreshFlags() {
		TOCNode tocNode = findTOCNode();
		String currentFlag = cbFlags.getSelectedKey();
		boolean hasCurrent = false;
		cbFlags.removeAllItems();
		cbFlags.addItem(null);
		if (tocNode != null) {
			ValueNode n = tocNode.findFlags(forceDesc);
			if (n != null && n.getContent() != null) {
				for (String rating : n.getContent().split(",")) {
					if (rating.contains(":")) {
						String[] fields = rating.split(":");
						cbFlags.addItem(fields[0],fields[1]);
					} else {
						cbFlags.addItem(rating, rating);
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
		if (cbFlags.getSelectedKey() != null) {
			forceDesc.getFlags().add(cbFlags.getSelectedKey());
		}
	}
	
	private void removeListeners() {
		cbFaction.removeActionListener(actionListener);
		cbSubfaction.removeActionListener(actionListener);
		cbUnitType.removeActionListener(actionListener);
		cbFormation.removeActionListener(actionListener);
		cbRating.removeActionListener(actionListener);
		cbFlags.removeActionListener(actionListener);
		cbWeightClass.removeActionListener(actionListener);
	}
	
	private void addListeners() {
		if (cbFaction.getActionListeners().length == 0) {
			cbFaction.addActionListener(actionListener);
		}
		if (cbSubfaction.getActionListeners().length == 0) {
			cbSubfaction.addActionListener(actionListener);
		}
		if (cbUnitType.getActionListeners().length == 0) {
			cbUnitType.addActionListener(actionListener);
		}
		if (cbFormation.getActionListeners().length == 0) {
			cbFormation.addActionListener(actionListener);
		}
		if (cbRating.getActionListeners().length == 0) {
			cbRating.addActionListener(actionListener);
		}
		if (cbFlags.getActionListeners().length == 0) {
			cbFlags.addActionListener(actionListener);
		}
		if (cbWeightClass.getActionListeners().length == 0) {
			cbWeightClass.addActionListener(actionListener);
		}
	}
	
	private TOCNode findTOCNode() {
		Ruleset rs = Ruleset.findRuleset(forceDesc);
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

	private ActionListener actionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			removeListeners();
			if (arg0.getSource() == cbFaction) {
				if (cbFaction.getSelectedItem() != null) {
					forceDesc.setFaction(cbFaction.getSelectedKey());
				}
				refreshSubfactions();
			} else if (arg0.getSource() == cbSubfaction) {
				if (cbSubfaction.getSelectedKey() != null) {
					forceDesc.setFaction(cbSubfaction.getSelectedKey());
				} else {
					forceDesc.setFaction(cbFaction.getSelectedKey());
				}
				refreshUnitTypes();
			} else if (arg0.getSource() == cbUnitType) {
				forceDesc.setUnitType(AbstractUnitRecord.parseUnitType((String)cbUnitType.getSelectedItem()));
				refreshFormations();
			} else if (arg0.getSource() == cbFormation) {
				String esch = cbFormation.getSelectedKey();
				setFormation(esch);
				refreshRatings();
			} else if (arg0.getSource() == cbRating) {
				forceDesc.setRating(cbRating.getSelectedKey());
				refreshFlags();
			} else if (arg0.getSource() == cbFlags) {
				forceDesc.getFlags().clear();
				if (cbFlags.getSelectedKey() != null) {
					forceDesc.getFlags().add(cbFlags.getSelectedKey());
				}
			} else if (arg0.getSource() == cbWeightClass) {
				if (cbWeightClass.getSelectedIndex() < 1) {
					forceDesc.setWeightClass(null);
				} else {
					forceDesc.setWeightClass(cbWeightClass.getSelectedIndex());
				}
			}
			addListeners();
		}
	};	

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
		txtYear.setText(String.valueOf(currentYear));
		RATGenerator.getInstance().loadYear(currentYear);
	}

	private ListCellRenderer<? super String> unitTypeCbRenderer = new DefaultListCellRenderer() {
		private static final long serialVersionUID = 8738837254686639184L;

		@Override
		public Component getListCellRendererComponent(JList<? extends Object> arg0, Object arg1,
				int arg2, boolean arg3, boolean arg4) {
			if (arg1 == null) {
				setText("Combined");
			} else {
				setText((String)arg1);
			}
			return this;
		}
	};

    static class KeyValueComboBox extends JComboBox<Map.Entry<String, String>> {
    	
    	private static final long serialVersionUID = -2428807516349658212L;

    	private String nullVal;
    	
    	public KeyValueComboBox() {
    		this("Default");
    	}
    	
    	public KeyValueComboBox(String nullVal) {
    		super();
    		this.nullVal = nullVal;
    		setRenderer(renderer);
    	}
    	
    	public void setSelectedItem(String key) {
    		DefaultComboBoxModel<Map.Entry<String,String>> model =
    				(DefaultComboBoxModel<Map.Entry<String,String>>)getModel();
    		for (int i = 0; i < getModel().getSize(); i++) {
    			if ((key == null && model.getElementAt(i) == null)
    					|| (model.getElementAt(i) != null
    						&& key.equals(model.getElementAt(i).getKey()))){
    				setSelectedIndex(i);
    				return;
    			}
    		}
    	}
    	
    	public void addItem(String key, String value) {
    		Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<String,String>(key,value);
    		addItem(entry);
    	}
    	
    	@SuppressWarnings("unchecked")
    	public String getSelectedKey() {
    		Object entry = getSelectedItem();
    		if (entry == null) {
    			return null;
    		}
    		return ((Map.Entry<String, String>)entry).getKey();
    	}

    	public String getKeyAt(int index) {
    		Map.Entry<String,String> entry = getItemAt(index);
    		if (entry == null) {
    			return null;
    		}
    		return entry.getKey();
    	}
    	
    	public boolean containsKey(String key) {
    		for (int i = 0; i < getItemCount(); i++) {
    			if (getItemAt(i).getKey().equals(key)) {
    				return true;
    			}
    		}
    		return false;
    	}

    	public boolean containsValue(String val) {
    		for (int i = 0; i < getItemCount(); i++) {
    			if (getItemAt(i).getValue().equals(val)) {
    				return true;
    			}
    		}
    		return false;
    	}

    	private ListCellRenderer<Object> renderer = new DefaultListCellRenderer() {
    		private static final long serialVersionUID = -4136501574366312512L;

    		@Override
			@SuppressWarnings("unchecked")
    		public Component getListCellRendererComponent(JList<? extends Object> list, Object entry,
    				int position, boolean arg3, boolean arg4) {
    			if (entry == null) {
    				setText(nullVal);
    			} else {
    				setText(((Map.Entry<String,String>)entry).getValue());
    			}
    			return this;
    		}
    	};
    }
    
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
    	/**
    	 * 
    	 */
    	private static final long serialVersionUID = -5915350078441133119L;
    	
    	private MechTileset mt;
    	
    	public UnitRenderer() {
            mt = new MechTileset(new File("data/images/units"));
            try {
                mt.loadFromFile("mechset.txt");
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
    	}

        @Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            super.getTreeCellRendererComponent(
                    tree, value, sel,
                    expanded, leaf, row,
                    hasFocus);
            setOpaque(true);
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
            if(sel) {
                setBackground(Color.DARK_GRAY);
                setForeground(Color.WHITE);
            }

            ForceDescriptor fd = (ForceDescriptor)value;
            if(fd.isElement()) {
                StringBuilder name = new StringBuilder();
                String uname = "";
                if(fd.getCo() == null) {
                    name.append("<font color='red'>No Crew</font>");
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
}
