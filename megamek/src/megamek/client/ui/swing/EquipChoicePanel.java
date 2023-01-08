/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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

import megamek.client.Client;
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.common.*;
import megamek.common.options.AbstractOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestBattleArmor;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.*;

/**
 * This class builds the Equipment Panel for use in MegaMek and MekHQ
 *
 * @author Dylan Myers (ralgith-erian@users.sourceforge.net)
 * @author arlith
 * @since 2012-05-20
 */
public class EquipChoicePanel extends JPanel {
    private static final long serialVersionUID = 672299770230285567L;

    private final Entity entity;

    private int[] entityCorrespondance;

    private List<MunitionChoicePanel> m_vMunitions = new ArrayList<>();
    private List<WeaponAmmoChoicePanel> m_vWeaponAmmoChoice = new ArrayList<>();
    
    /**
     * An <code>ArrayList</code> to keep track of all of the 
     * <code>APWeaponChoicePanels</code> that were added, so we can apply 
     * their choices when the dialog is closed.
     */
    private ArrayList<APWeaponChoicePanel> m_vAPMounts = new ArrayList<>();
    
    /**
     * An <code>ArrayList</code> to keep track of all of the 
     * <code>MEAChoicePanels</code> that were added, so we can apply 
     * their choices when the dialog is closed.
     */
    private ArrayList<MEAChoicePanel> m_vMEAdaptors = new ArrayList<>();
    
    /**
     * Panel for adding components related to selecting which anti-personnel
     * weapons are mounted in an AP Mount (armored gloves are also considered 
     * AP mounts)
     **/
    private JPanel panAPMounts = new JPanel();
    private JPanel panMEAdaptors = new JPanel();
    private JPanel panMunitions = new JPanel();
    private JPanel panWeaponAmmoSelector = new JPanel();

    private ArrayList<RapidfireMGPanel> m_vMGs = new ArrayList<>();
    private JPanel panRapidfireMGs = new JPanel();

    private InfantryArmorPanel panInfArmor = new InfantryArmorPanel();

    private ArrayList<MineChoicePanel> m_vMines = new ArrayList<>();
    private JPanel panMines = new JPanel();

    private BombChoicePanel m_bombs;
    private JPanel panBombs = new JPanel();

    private JLabel labAutoEject = new JLabel(
            Messages.getString("CustomMechDialog.labAutoEject"), SwingConstants.RIGHT);
    private JCheckBox chAutoEject = new JCheckBox();

    private JLabel labCondEjectAmmo = new JLabel(
            Messages.getString("CustomMechDialog.labConditional_Ejection_Ammo"), SwingConstants.RIGHT);
    private JCheckBox chCondEjectAmmo = new JCheckBox();

    private JLabel labCondEjectEngine = new JLabel(
            Messages.getString("CustomMechDialog.labConditional_Ejection_Engine"), SwingConstants.RIGHT);
    private JCheckBox chCondEjectEngine = new JCheckBox();

    private JLabel labCondEjectCTDest = new JLabel(
            Messages.getString("CustomMechDialog.labConditional_Ejection_CT_Destroyed"), SwingConstants.RIGHT);
    private JCheckBox chCondEjectCTDest = new JCheckBox();

    private JLabel labCondEjectHeadshot = new JLabel(
            Messages.getString("CustomMechDialog.labConditional_Ejection_Headshot"), SwingConstants.RIGHT);
    private JCheckBox chCondEjectHeadshot = new JCheckBox();
    
    private JLabel labCondEjectFuel = new JLabel(
            Messages.getString("CustomMechDialog.labConditional_Ejection_Fuel"), SwingConstants.RIGHT);
    private JCheckBox chCondEjectFuel = new JCheckBox();

    private JLabel labCondEjectSIDest = new JLabel(
            Messages.getString("CustomMechDialog.labConditional_Ejection_SI_Destroyed"), SwingConstants.RIGHT);
    private JCheckBox chCondEjectSIDest = new JCheckBox();

    private JLabel labSearchlight = new JLabel(
            Messages.getString("CustomMechDialog.labSearchlight"), SwingConstants.RIGHT);
    private JCheckBox chSearchlight = new JCheckBox();

    private JLabel labC3 = new JLabel(
            Messages.getString("CustomMechDialog.labC3"), SwingConstants.RIGHT);
    private JComboBox<String> choC3 = new JComboBox<>();

    ClientGUI clientgui;
    Client client;

    public EquipChoicePanel(Entity entity, ClientGUI clientgui, Client client) {
        this.entity = entity;
        this.clientgui = clientgui;
        this.client = client;

        GridBagLayout g = new GridBagLayout();
        setLayout(g);

        // **EQUIPMENT TAB**//
        // Auto-eject checkbox and conditional ejections.
        if (entity instanceof Mech) {
            Mech mech = (Mech) entity;

            // Ejection Seat
            boolean hasEjectSeat = true;
            // torso mounted cockpits don't have an ejection seat
            if (mech.getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED
                    || mech.hasQuirk(OptionsConstants.QUIRK_NEG_NO_EJECT)) {
                hasEjectSeat = false;
            }
            if (mech.isIndustrial()) {
                hasEjectSeat = false;
                // industrials can only eject when they have an ejection seat
                for (Mounted misc : mech.getMisc()) {
                    if (misc.getType().hasFlag(MiscType.F_EJECTION_SEAT)) {
                        hasEjectSeat = true;
                    }
                }
            }
            if (hasEjectSeat) {
                add(labAutoEject, GBC.std());
                add(chAutoEject, GBC.eol());
                chAutoEject.setSelected(!mech.isAutoEject());
            }

            // Conditional Ejections
            if (clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                    && hasEjectSeat) {
                add(labCondEjectAmmo, GBC.std());
                add(chCondEjectAmmo, GBC.eol());
                chCondEjectAmmo.setSelected(mech.isCondEjectAmmo());
                add(labCondEjectEngine, GBC.std());
                add(chCondEjectEngine, GBC.eol());
                chCondEjectEngine.setSelected(mech.isCondEjectEngine());
                add(labCondEjectCTDest, GBC.std());
                add(chCondEjectCTDest, GBC.eol());
                chCondEjectCTDest.setSelected(mech.isCondEjectCTDest());
                add(labCondEjectHeadshot, GBC.std());
                add(chCondEjectHeadshot, GBC.eol());
                chCondEjectHeadshot.setSelected(mech.isCondEjectHeadshot());
            }
        } else if (entity.isFighter()) {
            Aero aero = (Aero) entity;

            // Ejection Seat
            boolean hasEjectSeat = !(entity.hasQuirk(OptionsConstants.QUIRK_NEG_NO_EJECT));
            if (hasEjectSeat) {
                add(labAutoEject, GBC.std());
                add(chAutoEject, GBC.eol());
                chAutoEject.setSelected(!aero.isAutoEject());
            }

            // Conditional Ejections
            if (clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION)
                    && hasEjectSeat) {
                add(labCondEjectAmmo, GBC.std());
                add(chCondEjectAmmo, GBC.eol());
                chCondEjectAmmo.setSelected(aero.isCondEjectAmmo());
                add(labCondEjectFuel, GBC.std());
                add(chCondEjectFuel, GBC.eol());
                chCondEjectFuel.setSelected(aero.isCondEjectFuel());
                add(labCondEjectSIDest, GBC.std());
                add(chCondEjectSIDest, GBC.eol());
                chCondEjectSIDest.setSelected(aero.isCondEjectSIDest());
            }
        }

        if (entity.hasC3() || entity.hasC3i() || entity.hasNavalC3()) {
            add(labC3, GBC.std());
            add(choC3, GBC.eol());
            refreshC3();
        }
        
        // Setup AP mounts
        if ((entity instanceof BattleArmor) && entity.hasWorkingMisc(MiscType.F_AP_MOUNT)) {
            setupAPMounts();
            panAPMounts.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                    Messages.getString("CustomMechDialog.APMountPanelTitle"),
                    TitledBorder.TOP, TitledBorder.DEFAULT_POSITION));
            
            add(panAPMounts,GBC.eop().anchor(GridBagConstraints.CENTER));
        }
        
        if ((entity instanceof BattleArmor) && entity.hasWorkingMisc(MiscType.F_BA_MEA)) {
            panMEAdaptors.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                    Messages.getString("CustomMechDialog.MEAPanelTitle"),
                    TitledBorder.TOP, TitledBorder.DEFAULT_POSITION));
            // We need to determine how much weight is free, so the user can
            //  pick legal combinations of manipulators
            BattleArmor ba = (BattleArmor) entity;
            EntityVerifier verifier = EntityVerifier.getInstance(
                    new MegaMekFile(Configuration.unitsDir(),
                            EntityVerifier.CONFIG_FILENAME).getFile());
            TestBattleArmor testBA = new TestBattleArmor(ba, 
                    verifier.baOption, null);
            double maxTrooperWeight = 0;
            for (int i = 1; i < ba.getTroopers(); i++) {
                double trooperWeight = testBA.calculateWeight(i);
                if (trooperWeight > maxTrooperWeight) {
                    maxTrooperWeight = trooperWeight;
                }
            }
            String freeWeight = Messages.getString("CustomMechDialog.freeWeight")
                    + String.format(": %1$.3f/%2$.3f", maxTrooperWeight, ba.getTrooperWeight());
                        
            setupMEAdaptors(freeWeight);
            add(panMEAdaptors,GBC.eop().anchor(GridBagConstraints.CENTER));
        }
        
        // Can't set up munitions on infantry.
        if (!((entity instanceof Infantry) && !((Infantry) entity)
                .hasFieldWeapon()) || (entity instanceof BattleArmor)) {
            setupMunitions();
            panMunitions.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                    Messages.getString("CustomMechDialog.MunitionsPanelTitle"),
                    TitledBorder.TOP, TitledBorder.DEFAULT_POSITION));
            add(panMunitions,
                    GBC.eop().anchor(GridBagConstraints.CENTER));
            
            setupWeaponAmmoChoice();
            panWeaponAmmoSelector.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                    Messages.getString("CustomMechDialog.WeaponSelectionTitle"),
                    TitledBorder.TOP, TitledBorder.DEFAULT_POSITION));
            add(panWeaponAmmoSelector, GBC.eop().anchor(GridBagConstraints.CENTER));
        }

        if (entity.isBomber()) {
            setupBombs();
            add(panBombs, GBC.eop().anchor(GridBagConstraints.CENTER));
        }

        // Set up rapidfire mg; per errata infantry of any kind cannot use them
        if (clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_BURST) &&
                !(entity instanceof Infantry)) {
            setupRapidfireMGs();
            add(panRapidfireMGs, GBC.eop().anchor(GridBagConstraints.CENTER));
        }

        // set up infantry armor
        if (entity.isConventionalInfantry()) {
            panInfArmor.initialize();
            add(panInfArmor, GBC.eop().anchor(GridBagConstraints.CENTER));
        }

        // Set up searchlight
        if (!entity.getsAutoExternalSearchlight()
                && (client.getGame().getPlanetaryConditions().getLight() > PlanetaryConditions.L_DUSK)) {
            add(labSearchlight, GBC.std());
            add(chSearchlight, GBC.eol());
            chSearchlight.setSelected(entity.hasSearchlight()
                    || entity.hasQuirk(OptionsConstants.QUIRK_POS_SEARCHLIGHT));
            chSearchlight.setEnabled(!entity.hasQuirk(OptionsConstants.QUIRK_POS_SEARCHLIGHT));
        }

        // Set up mines
        setupMines();
        add(panMines, GBC.eop().anchor(GridBagConstraints.CENTER));
    }

    public void initialize() {
        choC3.setEnabled(false);
        chAutoEject.setEnabled(false);
        chSearchlight.setEnabled(false);
        if (m_bombs != null) {
            m_bombs.setEnabled(false);
        }
        disableMunitionEditing();
        disableAPMEditing();
        disableMEAEditing();
        disableMGSetting();
        disableMineSetting();
        panInfArmor.setEnabled(false);
    }

    public void applyChoices() {
        //Autoejection Options
        boolean autoEject = chAutoEject.isSelected();
        boolean condEjectAmmo = chCondEjectAmmo.isSelected();
        //Mechs and LAMs Only
        boolean condEjectEngine = chCondEjectEngine.isSelected();
        boolean condEjectCTDest = chCondEjectCTDest.isSelected();
        boolean condEjectHeadshot = chCondEjectHeadshot.isSelected();
        //Aeros Only
        boolean condEjectFuel = chCondEjectFuel.isSelected();
        boolean condEjectSIDest = chCondEjectSIDest.isSelected();

        if (entity instanceof Mech) {
            Mech mech = (Mech) entity;
            mech.setAutoEject(!autoEject);
            mech.setCondEjectAmmo(condEjectAmmo);
            mech.setCondEjectEngine(condEjectEngine);
            mech.setCondEjectCTDest(condEjectCTDest);
            mech.setCondEjectHeadshot(condEjectHeadshot);
        } else if (entity.isFighter()) {
            Aero aero = (Aero) entity;
            aero.setAutoEject(!autoEject);
            aero.setCondEjectAmmo(condEjectAmmo);
            aero.setCondEjectFuel(condEjectFuel);
            aero.setCondEjectSIDest(condEjectSIDest);
        }

        // update AP weapon selections
        for (APWeaponChoicePanel apChoicePanel : m_vAPMounts) {
            apChoicePanel.applyChoice();
        }

        // update modular equipment adaptor selections
        for (MEAChoicePanel meaChoicePanel : m_vMEAdaptors) {
            meaChoicePanel.applyChoice();
        }

        // update munitions selections
        for (final Object newVar2 : m_vMunitions) {
            ((MunitionChoicePanel) newVar2).applyChoice();
        }
        if (panMunitions instanceof BayMunitionsChoicePanel) {
            ((BayMunitionsChoicePanel) panMunitions).apply();
        } else {
            if (panMunitions instanceof SmallSVMunitionsChoicePanel) {
                ((SmallSVMunitionsChoicePanel) panMunitions).apply();
            }
            // update ammo names for weapon ammo choice selectors
            for (WeaponAmmoChoicePanel wacPanel : m_vWeaponAmmoChoice) {
                wacPanel.applyChoice();
            }
        }

        // update MG rapid fire settings
        for (final Object newVar1 : m_vMGs) {
            ((RapidfireMGPanel) newVar1).applyChoice();
        }
        // update mines setting
        for (final Object newVar : m_vMines) {
            ((MineChoicePanel) newVar).applyChoice();
        }
        // update bomb setting
        if (null != m_bombs) {
            m_bombs.applyChoice();
        }
        if (entity.isConventionalInfantry()) {
            panInfArmor.applyChoice();
        }

        // update searchlight setting
        if (!entity.getsAutoExternalSearchlight()) {
            entity.setExternalSearchlight(chSearchlight.isSelected());
            entity.setSearchlightState(chSearchlight.isSelected());
        }

        if (entity.hasC3() && (choC3.getSelectedIndex() > -1)) {
            Entity chosen = client.getEntity(entityCorrespondance[choC3.getSelectedIndex()]);
            int entC3nodeCount = client.getGame().getC3SubNetworkMembers(entity).size();
            int choC3nodeCount = client.getGame().getC3NetworkMembers(chosen).size();

            if ((entC3nodeCount + choC3nodeCount) <= Entity.MAX_C3_NODES
                    && ((chosen == null) || entity.getC3MasterId() != chosen.getId())) {
                entity.setC3Master(chosen, true);
            } else if (entity.getC3MasterId() != chosen.getId()) {
                String message = Messages.getString("CustomMechDialog.NetworkTooBig.message",
                        entity.getShortName(), chosen.getShortName(), entC3nodeCount,
                        choC3nodeCount, Entity.MAX_C3_NODES);
                clientgui.doAlertDialog(Messages.getString("CustomMechDialog.NetworkTooBig.title"),
                        message);
                refreshC3();
            }
        } else if (entity.hasC3i() && (choC3.getSelectedIndex() > -1)) {
            entity.setC3NetId(client.getEntity(entityCorrespondance[choC3.getSelectedIndex()]));
        } else if (entity.hasNavalC3() && (choC3.getSelectedIndex() > -1)) {
            entity.setC3NetId(client.getEntity(entityCorrespondance[choC3.getSelectedIndex()]));
        }
    }

    private void setupBombs() {
        GridBagLayout gbl = new GridBagLayout();
        panBombs.setLayout(gbl);

        int techlvl = Arrays.binarySearch(TechConstants.T_SIMPLE_NAMES,
                client.getGame().getOptions().stringOption(OptionsConstants.ALLOWED_TECHLEVEL));
        boolean allowNukes = client.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AT2_NUKES);
        m_bombs = new BombChoicePanel((IBomber) entity, allowNukes,
                techlvl >= TechConstants.T_SIMPLE_ADVANCED);
        panBombs.add(m_bombs, GBC.std());
    }

    private void setupRapidfireMGs() {
        GridBagLayout gbl = new GridBagLayout();
        panRapidfireMGs.setLayout(gbl);
        for (Mounted m : entity.getWeaponList()) {
            WeaponType wtype = (WeaponType) m.getType();
            if (!wtype.hasFlag(WeaponType.F_MG)) {
                continue;
            }
            RapidfireMGPanel rmp = new RapidfireMGPanel(m);
            panRapidfireMGs.add(rmp, GBC.eol());
            m_vMGs.add(rmp);
        }
    }

    private void setupMines() {
        GridBagLayout gbl = new GridBagLayout();
        panMines.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();

        int row = 0;
        for (Mounted m : entity.getMisc()) {
            if (!m.getType().hasFlag((MiscType.F_MINE)) &&
                    !m.getType().hasFlag((MiscType.F_VEHICLE_MINE_DISPENSER))) {
                continue;
            }

            gbc.gridy = row++;
            MineChoicePanel mcp = new MineChoicePanel(m);
            gbl.setConstraints(mcp, gbc);
            panMines.add(mcp);
            m_vMines.add(mcp);
        }
    }

    /**
     * Setup the layout of <code>panMEAdaptors</code>, which contains components
     * for selecting which manipulators are mounted in a modular equipment 
     * adaptor
     */
    private void setupMEAdaptors(String freeWeight) {
        GridBagLayout gbl = new GridBagLayout();
        panMEAdaptors.setLayout(gbl);
        
        JLabel lblFreeWeight = new JLabel(freeWeight);
        panMEAdaptors.add(lblFreeWeight, GBC.eol().anchor(GridBagConstraints.CENTER));

        ArrayList<MiscType> manipTypes = new ArrayList<>();
        
        for (String manipTypeName : BattleArmor.MANIPULATOR_TYPE_STRINGS) {
            // Ignore the "None" option
            if (manipTypeName.equals(BattleArmor.MANIPULATOR_TYPE_STRINGS[0])) {
                continue;
            }
            MiscType mType = (MiscType) EquipmentType.get(manipTypeName);
            manipTypes.add(mType);
        }
        
        for (Mounted m : entity.getMisc()) {
            if (!m.getType().hasFlag(MiscType.F_BA_MEA)) {
                continue;
            }
            Mounted currentManip = null;
            if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_LARM) {
                currentManip = ((BattleArmor) entity).getLeftManipulator();
            } else if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_RARM) {
                currentManip = ((BattleArmor) entity).getRightManipulator();
            } else {
                // We can only have MEA's in an arm
                continue;
            }
            MEAChoicePanel meacp;
            meacp = new MEAChoicePanel(entity, m.getBaMountLoc(), currentManip, manipTypes);
            
            panMEAdaptors.add(meacp, GBC.eol());
            m_vMEAdaptors.add(meacp);
        }
    }
    
    /**
     * Setup the layout of <code>panAPMounts</code>, which contains components
     * for selecting which anti-personnel weapons are mounted in an AP mount. 
     */
    private void setupAPMounts() {
        GridBagLayout gbl = new GridBagLayout();
        panAPMounts.setLayout(gbl);
        
        // Weapons that can be used in an AP Mount
        ArrayList<WeaponType> apWeapTypes = new ArrayList<>(100);
        // Weapons that can be used in an Armored Glove
        ArrayList<WeaponType> agWeapTypes = new ArrayList<>(100);
        Enumeration<EquipmentType> allTypes = EquipmentType.getAllTypes();
        int gameYear = clientgui.getClient().getGame().getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
        SimpleTechLevel legalLevel = SimpleTechLevel.getGameTechLevel(clientgui.getClient().getGame());
        while (allTypes.hasMoreElements()) {
            EquipmentType eq = allTypes.nextElement();
            
            // If it's not an infantry weapon, we don't care
            if (!(eq instanceof InfantryWeapon)) {
                continue;
            }
            
            // Check to see if the tech level of the equipment is legal
            if (!eq.isLegal(gameYear, legalLevel, entity.isClan(), entity.isMixedTech())) {
                continue;
            }
            
            // Check to see if we've got a valid infantry weapon
            InfantryWeapon infWeap = (InfantryWeapon) eq;
            if (infWeap.hasFlag(WeaponType.F_INFANTRY)
                    && !infWeap.hasFlag(WeaponType.F_INF_POINT_BLANK)
                    && !infWeap.hasFlag(WeaponType.F_INF_ARCHAIC)
                    && !infWeap.hasFlag(WeaponType.F_INF_SUPPORT)) {
                apWeapTypes.add(infWeap);
            }
            if (infWeap.hasFlag(WeaponType.F_INFANTRY)
                    && !infWeap.hasFlag(WeaponType.F_INF_POINT_BLANK)
                    && !infWeap.hasFlag(WeaponType.F_INF_ARCHAIC)
                    && (infWeap.getCrew() < 2)) {
                agWeapTypes.add(infWeap);
            }
        }
        apWeapTypes.sort(Comparator.comparing(EquipmentType::getName));
        agWeapTypes.sort(Comparator.comparing(EquipmentType::getName));

        ArrayList<Mounted> armoredGloves = new ArrayList<>(2);
        for (Mounted m : entity.getMisc()) {
            if (!m.getType().hasFlag(MiscType.F_AP_MOUNT)) {
                continue;
            }
            APWeaponChoicePanel apcp = null;
            // Armored gloves need to be treated slightly differently, since
            // 1 or 2 armored gloves allow 1 additional AP weapon
            if (m.getType().hasFlag(MiscType.F_ARMORED_GLOVE)) {
                armoredGloves.add(m);                
            } else {
                apcp = new APWeaponChoicePanel(entity, m, apWeapTypes);
            }
            if (apcp != null) {
                panAPMounts.add(apcp, GBC.eol());
                m_vAPMounts.add(apcp);
            }
        }
        
        // If there is an armored glove with a weapon already mounted, we need
        //  to ensure that that glove is displayed, and not the empty glove
        Mounted aGlove = null;
        for (Mounted ag : armoredGloves) {
            if (aGlove == null) {
                aGlove = ag;
            } else if ((aGlove.getLinked() == null) && (ag.getLinked() != null)) {
                aGlove = ag;
            } 
            // If both are linked, TestBattleArmor will mark unit as invalid
        }
        if (aGlove != null) {
            APWeaponChoicePanel apcp = new APWeaponChoicePanel(entity, aGlove, agWeapTypes);
            panAPMounts.add(apcp, GBC.eol());
            m_vAPMounts.add(apcp);
        }
    }

    private void setupMunitions() {
        GridBagLayout gbl = new GridBagLayout();
        panMunitions.setLayout(gbl);
        Game game = clientgui.getClient().getGame();
        AbstractOptions gameOpts = game.getOptions();
        int gameYear = gameOpts.intOption(OptionsConstants.ALLOWED_YEAR);

        if (entity.usesWeaponBays() || entity instanceof Dropship) {
            //Grounded dropships don't *use* weapon bays as such, but should load ammo as if they did
            panMunitions = new BayMunitionsChoicePanel(entity, game);
            return;
        }
        // Small support vehicle ammo is part of the weapon, and the only munitions choice is
        // standard or inferno, and only for some weapons.
        if (entity.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            panMunitions = new SmallSVMunitionsChoicePanel(entity);
            return;
        }
        panMunitions.setLayout(gbl);

        for (Mounted m : entity.getAmmo()) {
            AmmoType at = (AmmoType) m.getType();
            ArrayList<AmmoType> vTypes = new ArrayList<>();
            Vector<AmmoType> vAllTypes = AmmoType.getMunitionsFor(at.getAmmoType());
            if (vAllTypes == null) {
                continue;
            }

            // don't allow ammo switching of most things for Aeros
            // allow only MML, ATM, and NARC. LRM/SRM can switch between Artemis and standard,
            // but not other munitions. Same with MRM.
            if ((entity instanceof Aero)
                    && !((at.getAmmoType() == AmmoType.T_MML)
                            || (at.getAmmoType() == AmmoType.T_SRM)
                            || (at.getAmmoType() == AmmoType.T_LRM)
                            || (at.getAmmoType() == AmmoType.T_MRM)
                            || (at.getAmmoType() == AmmoType.T_ATM)
                            || (at.getAmmoType() == AmmoType.T_IATM))) {
                continue;
            }

            for (AmmoType atCheck : vAllTypes) {
                if (entity.hasETypeFlag(Entity.ETYPE_AERO)
                        && !atCheck.canAeroUse(game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_ARTILLERY_MUNITIONS))) {
                    continue;
                }
                SimpleTechLevel legalLevel = SimpleTechLevel.getGameTechLevel(game);
                boolean bTechMatch = false;
                if (game.getOptions().booleanOption(OptionsConstants.ALLOWED_ERA_BASED)) {
                    bTechMatch = atCheck.isLegal(gameYear, legalLevel, entity.isClan(),
                            entity.isMixedTech());
                } else {
                    bTechMatch = atCheck.getStaticTechLevel().ordinal() <= legalLevel.ordinal();
                }

                // If clan_ignore_eq_limits is unchecked,
                // do NOT allow Clans to use IS-only ammo.
                // N.B. play bit-shifting games to allow "incendiary"
                // to be combined to other munition types.
                long muniType = atCheck.getMunitionType();
                muniType &= ~AmmoType.M_INCENDIARY_LRM;
                if (!gameOpts.booleanOption(OptionsConstants.ALLOWED_CLAN_IGNORE_EQ_LIMITS)
                        && entity.isClan()
                        && ((muniType == AmmoType.M_SEMIGUIDED)
                                || (muniType == AmmoType.M_SWARM_I)
                                || (muniType == AmmoType.M_THUNDER_AUGMENTED)
                                || (muniType == AmmoType.M_THUNDER_INFERNO)
                                || (muniType == AmmoType.M_THUNDER_VIBRABOMB)
                                || (muniType == AmmoType.M_THUNDER_ACTIVE)
                                || (muniType == AmmoType.M_INFERNO_IV)
                                || (muniType == AmmoType.M_VIBRABOMB_IV)
                                || (muniType == AmmoType.M_LISTEN_KILL)
                                || (muniType == AmmoType.M_ANTI_TSM)
                                || (muniType == AmmoType.M_DEAD_FIRE) 
                                || (muniType == AmmoType.M_MINE_CLEARANCE))) {
                    bTechMatch = false;
                }
                
                if ((muniType == AmmoType.M_ARTEMIS_CAPABLE)
                        && !entity.hasWorkingMisc(MiscType.F_ARTEMIS)
                        && !entity.hasWorkingMisc(MiscType.F_ARTEMIS_PROTO)) {
                    continue;
                }
                if ((muniType == AmmoType.M_ARTEMIS_V_CAPABLE)
                        && !entity.hasWorkingMisc(MiscType.F_ARTEMIS_V)
                        && !entity.hasWorkingMisc(MiscType.F_ARTEMIS_PROTO)) {
                    continue;
                }

                if (!gameOpts.booleanOption(OptionsConstants.ADVANCED_MINEFIELDS) &&
                        AmmoType.canDeliverMinefield(atCheck)) {
                    continue;
                }

                // Only Protos can use Proto-specific ammo
                if (atCheck.hasFlag(AmmoType.F_PROTOMECH)
                        && !(entity instanceof Protomech)) {
                    continue;
                }

                // When dealing with machine guns, Protos can only
                // use proto-specific machine gun ammo
                if ((entity instanceof Protomech)
                        && atCheck.hasFlag(AmmoType.F_MG)
                        && !atCheck.hasFlag(AmmoType.F_PROTOMECH)) {
                    continue;
                }

                // Battle Armor ammo can't be selected at all.
                // All other ammo types need to match on rack size and tech.
                if (bTechMatch
                        && (atCheck.getRackSize() == at.getRackSize())
                        && (atCheck.hasFlag(AmmoType.F_BATTLEARMOR) == at
                                .hasFlag(AmmoType.F_BATTLEARMOR))
                        && (atCheck.hasFlag(AmmoType.F_ENCUMBERING) == at
                                .hasFlag(AmmoType.F_ENCUMBERING))
                        && (atCheck.getTonnage(entity) == at.getTonnage(entity))) {
                    vTypes.add(atCheck);
                }
            }
            if ((vTypes.size() < 1)
                    && !client.getGame().getOptions().booleanOption(OptionsConstants.BASE_LOBBY_AMMO_DUMP)
                    && !client.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD)) {
                continue;
            }
            MunitionChoicePanel mcp;
            mcp = new MunitionChoicePanel(m, vTypes, m_vWeaponAmmoChoice);
            panMunitions.add(mcp, GBC.eol());
            m_vMunitions.add(mcp);
        }
    }

    /**
     * Worker function that creates a series of weapon ammo choice panels that allow the user to pick a particular ammo bin for an 
     * ammo-using weapon with matching ammo.
     */
    private void setupWeaponAmmoChoice() {
        GridBagLayout gbl = new GridBagLayout();
        panWeaponAmmoSelector.setLayout(gbl);
        
        for (Mounted weapon : entity.getWeaponList()) {
            WeaponType weaponType = weapon.getType() instanceof WeaponType ? (WeaponType) weapon.getType() : null;
            
            // don't deal with bay or grouped weapons for now 
            if (weaponType == null || weaponType.getAmmoType() == AmmoType.T_NA) {
                continue;
            }
            
            WeaponAmmoChoicePanel ammoChoicePanel = new WeaponAmmoChoicePanel(weapon);
            panWeaponAmmoSelector.add(ammoChoicePanel, GBC.eol());
            m_vWeaponAmmoChoice.add(ammoChoicePanel);
        }
    }
    
        class MineChoicePanel extends JPanel {
            private static final long serialVersionUID = -1868675102440527538L;

            private JComboBox<String> m_choice;

            private Mounted m_mounted;

            MineChoicePanel(Mounted m) {
                m_mounted = m;
                m_choice = new JComboBox<>();
                m_choice.addItem(Messages.getString("CustomMechDialog.Conventional"));
                m_choice.addItem(Messages.getString("CustomMechDialog.Vibrabomb"));
                int loc;
                loc = m.getLocation();
                String sDesc = '(' + entity.getLocationAbbr(loc) + ')';
                JLabel lLoc = new JLabel(sDesc);
                GridBagLayout gbl = new GridBagLayout();
                setLayout(gbl);
                add(lLoc, GBC.std());
                m_choice.setSelectedIndex(m.getMineType());
                add(m_choice, GBC.eol());
            }

            public void applyChoice() {
                m_mounted.setMineType(m_choice.getSelectedIndex());
            }

            @Override
            public void setEnabled(boolean enabled) {
                m_choice.setEnabled(enabled);
            }
        }
        
        /**
         * A panel that houses a label and a combo box that allows for selecting
         * which anti-personnel weapon is mounted in an AP mount.
         * 
         * @author arlith
         */
        class APWeaponChoicePanel extends JPanel {
            private static final long serialVersionUID = 6189888202192403704L;

            private Entity entity;
            
            private ArrayList<WeaponType> m_APWeaps;

            private JComboBox<String> m_choice;

            private Mounted m_APmounted;

            APWeaponChoicePanel(Entity e, Mounted m, ArrayList<WeaponType> weapons) {
                entity = e;
                m_APWeaps = weapons;
                m_APmounted = m;
                EquipmentType  curType = null;
                if ((m != null) && (m.getLinked() != null)) {
                    curType = m.getLinked().getType();
                }
                m_choice = new JComboBox<>();
                m_choice.addItem("None");
                m_choice.setSelectedIndex(0);
                Iterator<WeaponType> it = m_APWeaps.iterator();
                for (int x = 1; it.hasNext(); x++) {
                    WeaponType weap = it.next();
                    m_choice.addItem(weap.getName());
                    if ((curType != null)
                            && Objects.equals(weap.getInternalName(), curType.getInternalName())) {
                        m_choice.setSelectedIndex(x);
                    }
                }

                String sDesc = "";
                if ((m != null) && (m.getBaMountLoc() != BattleArmor.MOUNT_LOC_NONE)) {
                    sDesc += " (" + BattleArmor.MOUNT_LOC_NAMES[m.getBaMountLoc()] + ')';
                } else {
                    sDesc = "None";
                }
                JLabel lLoc = new JLabel(sDesc);
                GridBagLayout g = new GridBagLayout();
                setLayout(g);
                add(lLoc, GBC.std());
                add(m_choice, GBC.std());
                
            }

            public void applyChoice() {
                int n = m_choice.getSelectedIndex();
                // If there's no selection, there's nothing we can do
                if (n == -1) {
                    return;
                }
                WeaponType apType = null;
                if ((n > 0) && (n <= m_APWeaps.size())) {
                    // Need to account for the "None" selection
                    apType = m_APWeaps.get(n - 1);
                }
                
                // Remove any currently mounted AP weapon
                if (m_APmounted.getLinked() != null 
                        && m_APmounted.getLinked().getType() != apType) {
                    Mounted apWeapon = m_APmounted.getLinked();
                    entity.getEquipment().remove(apWeapon);
                    entity.getWeaponList().remove(apWeapon);
                    entity.getTotalWeaponList().remove(apWeapon);
                    // We need to make sure that the weapon has been removed
                    //  from the criticals, otherwise it can cause issues
                    for (int loc = 0; loc < entity.locations(); loc++) {
                        for (int c = 0; 
                                c < entity.getNumberOfCriticals(loc); c++) {
                            CriticalSlot crit = entity.getCritical(loc, c);
                            if (crit != null && crit.getMount() != null 
                                    && crit.getMount().equals(apWeapon)) {
                                entity.setCritical(loc, c, null);
                            }
                        }
                    }
                }
                
                // Did the selection not change, or no weapon was selected
                if ((m_APmounted.getLinked() != null 
                        && m_APmounted.getLinked().getType() == apType)
                        || n == 0) {
                    return;
                }
                    
                // Add the newly mounted weapon
                try {
                    Mounted newWeap = entity.addEquipment(apType, m_APmounted.getLocation());
                    m_APmounted.setLinked(newWeap);
                    newWeap.setLinked(m_APmounted);
                    newWeap.setAPMMounted(true);
                } catch (LocationFullException ex) {
                    // This shouldn't happen for BA...
                    LogManager.getLogger().error("", ex);
                }
            }

            @Override
            public void setEnabled(boolean enabled) {
                m_choice.setEnabled(enabled);
            }
        }
        
        /**
         * A panel that houses a label and a combo box that allows for selecting
         * which manipulator is mounted in a modular equipment adaptor.
         * 
         * @author arlith
         */
        class MEAChoicePanel extends JPanel {
            private static final long serialVersionUID = 6189888202192403704L;

            private Entity entity;
            
            private ArrayList<MiscType> m_Manipulators;

            private JComboBox<String> m_choice;

            /**
             * The manipulator currently mounted by a modular equipment adaptor.
             */
            private Mounted m_Manipmounted;
            
            /**
             * The BattleArmor mount location of the modular equipment adaptor.
             */
            private int baMountLoc;

            MEAChoicePanel(Entity e, int mountLoc, Mounted m, 
                    ArrayList<MiscType> manips) {
                entity = e;
                m_Manipulators = manips;
                m_Manipmounted = m;
                baMountLoc = mountLoc;
                EquipmentType  curType = null;
                if (m != null) {
                    curType = m.getType();
                }
                m_choice = new JComboBox<>();
                m_choice.addItem("None");
                m_choice.setSelectedIndex(0);
                Iterator<MiscType> it = m_Manipulators.iterator();
                for (int x = 1; it.hasNext(); x++) {
                    MiscType manip = it.next();
                    String manipName = manip.getName() + " ("
                            + manip.getTonnage(entity) + "kg)";
                    m_choice.addItem(manipName);
                    if (curType != null && 
                            manip.getInternalName() == 
                                curType.getInternalName()) {
                        m_choice.setSelectedIndex(x);
                    }
                }

                String sDesc = "";
                if (baMountLoc != BattleArmor.MOUNT_LOC_NONE) {
                    sDesc += " (" 
                            + BattleArmor.MOUNT_LOC_NAMES[baMountLoc] 
                            + ')';
                } else {
                    sDesc = "None";
                }
                JLabel lLoc = new JLabel(sDesc);
                GridBagLayout g = new GridBagLayout();
                setLayout(g);
                add(lLoc, GBC.std());
                add(m_choice, GBC.std());
                
            }

            public void applyChoice() {
                int n = m_choice.getSelectedIndex();
                // If there's no selection, there's nothing we can do
                if (n == -1) {
                    return;
                }
                MiscType manipType = null;
                if (n > 0 && n <= m_Manipulators.size()) {
                    // Need to account for the "None" selection
                    manipType = m_Manipulators.get(n-1);
                }

                if (m_Manipmounted != null) {
                    entity.getEquipment().remove(m_Manipmounted);
                    entity.getMisc().remove(m_Manipmounted);
                }            
                
                // Was no manipulator selected?
                if (n == 0) {
                    return;
                }
                    
                // Add the newly mounted manipulator
                try {
                    m_Manipmounted = entity.addEquipment(manipType, m_Manipmounted.getLocation());
                    m_Manipmounted.setBaMountLoc(baMountLoc);
                } catch (LocationFullException ex) {
                    // This shouldn't happen for BA...
                    LogManager.getLogger().error("", ex);
                }
            }

            @Override
            public void setEnabled(boolean enabled) {
                m_choice.setEnabled(enabled);
            }
        }

        class MunitionChoicePanel extends JPanel {
            private static final long serialVersionUID = 3401106035583965326L;

            private List<AmmoType> m_vTypes;

            private JComboBox<AmmoType> m_choice;
            
            @SuppressWarnings("rawtypes")
            private JComboBox m_num_shots;
            private ItemListener numShotsListener;
           
            boolean numShotsChanged = false;

            private Mounted m_mounted;

            JLabel labDump = new JLabel(Messages.getString("CustomMechDialog.labDump"));

            JCheckBox chDump = new JCheckBox();

            JLabel labHotLoad = new JLabel(Messages.getString("CustomMechDialog.switchToHotLoading"));

            JCheckBox chHotLoad = new JCheckBox();
            
            @SuppressWarnings("unchecked")
            MunitionChoicePanel(Mounted m, ArrayList<AmmoType> vTypes, List<WeaponAmmoChoicePanel> weaponAmmoChoicePanels) {
                m_vTypes = vTypes;
                m_mounted = m;
                
                AmmoType curType = (AmmoType) m.getType();
                m_choice = new JComboBox<>();
                Iterator<AmmoType> e = m_vTypes.iterator();
                for (int x = 0; e.hasNext(); x++) {
                    AmmoType at = e.next();
                    m_choice.addItem(at);
                    if (at.equals(curType)) {
                        m_choice.setSelectedIndex(x);
                    }
                }

                numShotsListener = evt -> numShotsChanged = true;
                m_num_shots = new JComboBox<String>();
                int shotsPerTon = curType.getShots();
                // BattleArmor always have a certain number of shots per slot
                int stepSize = 1;
                // ProtoMeks and BattleArmor are limited to the number of shots allocated in construction
                if ((entity instanceof BattleArmor) || (entity instanceof Protomech)) {
                    shotsPerTon = m.getOriginalShots();
                    // BA tube artillery always comes in pairs
                    if (curType.getAmmoType() == AmmoType.T_BA_TUBE) {
                        stepSize = 2;
                    }
                }
                for (int i = 0; i <= shotsPerTon; i += stepSize) {
                    m_num_shots.addItem(i);
                }
                m_num_shots.setSelectedItem(m_mounted.getBaseShotsLeft());
                m_num_shots.addItemListener(numShotsListener);

                m_choice.addItemListener(evt -> {
                    m_num_shots.removeItemListener(numShotsListener);
                    int currShots = (Integer) m_num_shots.getSelectedItem();
                    m_num_shots.removeAllItems();
                    int numberOfShotsPerTon = m_vTypes.get(m_choice.getSelectedIndex()).getShots();
                    
                    // ProtoMeks are limited to number of shots added during construction
                    if ((entity instanceof BattleArmor) || (entity instanceof Protomech)) {
                        numberOfShotsPerTon = m.getOriginalShots();
                    }
                    for (int i = 0; i <= numberOfShotsPerTon; i++) {
                        m_num_shots.addItem(i);
                    }
                    // If the shots selection was changed, try to set that value, unless it's too large
                    if (numShotsChanged && currShots <= numberOfShotsPerTon) {
                        m_num_shots.setSelectedItem(currShots);
                    } else {
                        m_num_shots.setSelectedItem(numberOfShotsPerTon);
                    }
                    
                    for (WeaponAmmoChoicePanel weaponAmmoChoicePanel : weaponAmmoChoicePanels) {
                        weaponAmmoChoicePanel.refreshAmmoBinName(m_mounted, m_vTypes.get(m_choice.getSelectedIndex()));
                    }
                    
                    m_num_shots.addItemListener(numShotsListener);
                });


                int loc = m.getLocation();
                boolean isOneShot = false;
                if (loc == Entity.LOC_NONE) {
                    // oneshot weapons don't have a location of their own
                    // some weapons (e.g. fusillade) use the one-shot mechanic but have an extra reload
                    // which is chained to the first
                    Mounted linkedBy = m.getLinkedBy();
                    while (linkedBy.getLinkedBy() != null) {
                        linkedBy = linkedBy.getLinkedBy();
                    }
                    loc = linkedBy.getLocation();
                    isOneShot = linkedBy.isOneShot();
                } else {
                    loc = m.getLocation();
                }
                m_num_shots.setVisible(!isOneShot);
                String sDesc = '(' + entity.getLocationAbbr(loc) + ')';
                JLabel lLoc = new JLabel(sDesc);
                GridBagLayout g = new GridBagLayout();
                setLayout(g);
                add(lLoc, GBC.std());
                add(m_choice, GBC.std());
                add(m_num_shots, GBC.eol());
                chHotLoad.setSelected(m_mounted.isHotLoaded());
                if (clientgui.getClient().getGame().getOptions().booleanOption(
                        OptionsConstants.BASE_LOBBY_AMMO_DUMP)) {
                    add(labDump, GBC.std());
                    add(chDump, GBC.eol());
                    if (clientgui.getClient().getGame().getOptions().booleanOption(
                            OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD)
                            && curType.hasFlag(AmmoType.F_HOTLOAD)) {
                        add(labHotLoad, GBC.std());
                        add(chHotLoad, GBC.eol());
                    }
                } else if (clientgui.getClient().getGame().getOptions().booleanOption(
                        OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD)
                        && curType.hasFlag(AmmoType.F_HOTLOAD)) {
                    add(labHotLoad, GBC.std());
                    add(chHotLoad, GBC.eol());
                }
            }

            public void applyChoice() {
                int n = m_choice.getSelectedIndex();
                // If there's no selection, there's nothing we can do
                if (n == -1) {
                    return;
                }
                AmmoType at = m_vTypes.get(n);
                m_mounted.changeAmmoType(at);
                
                // set # shots only for non-one shot weapons
                if (m_mounted.getLocation() != Entity.LOC_NONE) {
                    m_mounted.setShotsLeft((Integer) m_num_shots.getSelectedItem());
                }
                
                if (chDump.isSelected()) {
                    m_mounted.setShotsLeft(0);
                }
                if (clientgui.getClient().getGame().getOptions().booleanOption(
                        OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD)) {
                    if (chHotLoad.isSelected() != m_mounted.isHotLoaded()) {
                        m_mounted.setHotLoad(chHotLoad.isSelected());
                        // Set the mode too, so vehicles can switch back
                        int numModes = m_mounted.getType().getModesCount();
                        for (int m = 0; m < numModes; m++) {
                            if (m_mounted.getType().getMode(m).getName()
                                    .equals("HotLoad")) {
                                m_mounted.setMode(m);
                            }
                        }
                    }
                }
            }

            @Override
            public void setEnabled(boolean enabled) {
                m_choice.setEnabled(enabled);
            }

            /**
             * Get the number of shots in the mount.
             *
             * @return the <code>int</code> number of shots in the mount.
             */
            int getShotsLeft() {
                return m_mounted.getBaseShotsLeft();
            }

            /**
             * Set the number of shots in the mount.
             *
             * @param shots
             *            the <code>int</code> number of shots for the mount.
             */
            void setShotsLeft(int shots) {
                m_mounted.setShotsLeft(shots);
            }
        }
        
        /**
         * A panel representing the option to choose a particular ammo bin for an individual weapon.
         * @author NickAragua
         */
        class WeaponAmmoChoicePanel extends JPanel {
            private static final long serialVersionUID = 604670659251519188L;
            // the weapon being displayed in this row
            private Mounted m_mounted;
            private ArrayList<Mounted> matchingAmmoBins;
            
            private JComboBox<String> ammoBins;
            
            /**
             * Constructor
             * @param weapon The mounted weapon. Assumes that the weapon uses ammo.
             */
            public WeaponAmmoChoicePanel(Mounted weapon) {
                // for safety purposes, if the given mounted isn't a weapon, don't do anything.
                if (!(weapon.getType() instanceof WeaponType)) {
                    return;
                }
                
                m_mounted = weapon;
                
                this.setLayout(new GridBagLayout());
                
                ammoBins = new JComboBox<>();
                matchingAmmoBins = new ArrayList<>();
                
                if (m_mounted.isOneShot() || (entity.isSupportVehicle()
                        && (m_mounted.getType() instanceof InfantryWeapon))) {
                    // One-shot weapons can only access their own bin
                    matchingAmmoBins.add(m_mounted.getLinked());
                    // Fusillade and some small SV weapons are treated like one-shot
                    // weapons but may have a second munition type available.
                    if ((m_mounted.getLinked().getLinked() != null)
                            && (((AmmoType) m_mounted.getLinked().getType()).getMunitionType()
                                != (((AmmoType) m_mounted.getLinked().getLinked().getType()).getMunitionType()))) {
                        matchingAmmoBins.add(m_mounted.getLinked().getLinked());
                    }
                } else {
                    for (Mounted ammoBin : weapon.getEntity().getAmmo()) {
                        if ((ammoBin.getLocation() != Entity.LOC_NONE)
                            && AmmoType.canSwitchToAmmo(weapon, (AmmoType) ammoBin.getType())) {
                            matchingAmmoBins.add(ammoBin);
                        }
                    }
                }
                
                // don't bother displaying the row if there's no ammo to be swapped
                if (matchingAmmoBins.isEmpty()) {
                    return;
                }
                
                JLabel weaponName = new JLabel();
                weaponName.setText("(" + weapon.getEntity().getLocationAbbr(weapon.getLocation()) + ") " + weapon.getName());
                add(weaponName, GBC.std());
                
                add(ammoBins, GBC.eol());
                refreshAmmoBinNames();
            }

            /**
             * Worker function that refreshes the combo box with "up-to-date" ammo names.
             */
            public void refreshAmmoBinNames() {
                int selectedIndex = ammoBins.getSelectedIndex();
                ammoBins.removeAllItems();
                
                int currentIndex = 0;
                for (Mounted ammoBin : matchingAmmoBins) {
                    ammoBins.addItem("(" + ammoBin.getEntity().getLocationAbbr(ammoBin.getLocation()) + ") " + ammoBin.getName());
                    if (m_mounted.getLinked() == ammoBin) {
                        selectedIndex = currentIndex;
                    }
                    
                    currentIndex++;
                }
                
                if (selectedIndex >= 0) {
                    ammoBins.setSelectedIndex(selectedIndex);
                }
                
                validate();
            }
            
            /**
             * Refreshes a single item in the ammo type combo box to display the correct ammo type name.
             * Because the underlying ammo bin hasn't been updated yet, we carry out the name swap "in-place".
             * @param ammoBin The ammo bin whose ammo type has probably changed.
             * @param selectedAmmoType The new ammo type.
             */
            public void refreshAmmoBinName(Mounted ammoBin, AmmoType selectedAmmoType) {
                int index = 0;
                boolean matchFound = false;
                
                for (index = 0; index < matchingAmmoBins.size(); index++) {
                    if (matchingAmmoBins.get(index) == ammoBin) {
                        matchFound = true;
                        break;
                    }
                }
                
                if (matchFound) {
                    int currentBinIndex = ammoBins.getSelectedIndex();
                    
                    ammoBins.removeItemAt(index);
                    ammoBins.insertItemAt("(" + ammoBin.getEntity().getLocationAbbr(ammoBin.getLocation()) + ") " + selectedAmmoType.getName(), index);
                    
                    if (currentBinIndex == index) {
                        ammoBins.setSelectedIndex(index);
                    }
                    
                    validate();
                }
            }
            
            /**
             * Common functionality that applies the panel's current ammo bin choice to the panel's weapon.
             */
            public void applyChoice() {
                int selectedIndex = ammoBins.getSelectedIndex();
                if ((selectedIndex >= 0) && (selectedIndex < matchingAmmoBins.size())) {
                    entity.loadWeapon(m_mounted, matchingAmmoBins.get(selectedIndex));
                }
            }
        }

        class RapidfireMGPanel extends JPanel {
            private static final long serialVersionUID = 5261919826318225201L;

            private Mounted m_mounted;

            JCheckBox chRapid = new JCheckBox();

            RapidfireMGPanel(Mounted m) {
                m_mounted = m;
                int loc = m.getLocation();
                String sDesc = Messages.getString("CustomMechDialog.switchToRapidFire",
                        entity.getLocationAbbr(loc));
                JLabel labRapid = new JLabel(sDesc);
                GridBagLayout g = new GridBagLayout();
                setLayout(g);
                add(labRapid, GBC.std().anchor(GridBagConstraints.EAST));
                chRapid.setSelected(m.isRapidfire());
                add(chRapid, GBC.eol());
            }

            public void applyChoice() {
                boolean b = chRapid.isSelected();
                m_mounted.setRapidfire(b);
            }

            @Override
            public void setEnabled(boolean enabled) {
                chRapid.setEnabled(enabled);
            }
        }

    class InfantryArmorPanel extends JPanel {
        private static final long serialVersionUID = -909995917737642853L;

        private Infantry inf;
        JLabel labArmor = new JLabel(Messages.getString("CustomMechDialog.labInfantryArmor"));
        JLabel labDivisor = new JLabel(Messages.getString("CustomMechDialog.labDamageDivisor"));
        JLabel labEncumber = new JLabel(Messages.getString("CustomMechDialog.labEncumber"));
        JLabel labSpaceSuit = new JLabel(Messages.getString("CustomMechDialog.labSpaceSuit"));
        JLabel labDEST = new JLabel(Messages.getString("CustomMechDialog.labDEST"));
        JLabel labSneakCamo = new JLabel(Messages.getString("CustomMechDialog.labSneakCamo"));
        JLabel labSneakIR = new JLabel(Messages.getString("CustomMechDialog.labSneakIR"));
        JLabel labSneakECM = new JLabel(Messages.getString("CustomMechDialog.labSneakECM"));
        JLabel labSpec = new JLabel(Messages.getString("CustomMechDialog.labInfSpec"));
        private JComboBox<String> cbArmorKit = new JComboBox<>();
        private JTextField fldDivisor = new JTextField(3);
        JCheckBox chEncumber = new JCheckBox();
        JCheckBox chSpaceSuit = new JCheckBox();
        JCheckBox chDEST = new JCheckBox();
        JCheckBox chSneakCamo = new JCheckBox();
        JCheckBox chSneakIR = new JCheckBox();
        JCheckBox chSneakECM = new JCheckBox();
        List<JCheckBox> chSpecs = new ArrayList<>(Infantry.NUM_SPECIALIZATIONS);
        
        List<EquipmentType> armorKits = new ArrayList<>();

        InfantryArmorPanel() {
            for (int i = 0; i < Infantry.NUM_SPECIALIZATIONS; i++) {
                int spec = 1 << i;
                JCheckBox newSpec = new JCheckBox();
                newSpec.setText(Infantry.getSpecializationName(spec));
                newSpec.setToolTipText(Infantry.getSpecializationTooltip(spec));
                chSpecs.add(newSpec);
            }
            
            GridBagLayout g = new GridBagLayout();
            setLayout(g);
            add(labArmor, GBC.std());
            add(cbArmorKit, GBC.eol());
            add(labDivisor, GBC.std());
            add(fldDivisor, GBC.eol());
            add(labEncumber, GBC.std());
            add(chEncumber, GBC.eol());
            add(labSpaceSuit, GBC.std());
            add(chSpaceSuit, GBC.eol());
            add(labDEST, GBC.std());
            add(chDEST, GBC.eol());
            add(labSneakCamo, GBC.std());
            add(chSneakCamo, GBC.eol());
            add(labSneakIR, GBC.std());
            add(chSneakIR, GBC.eol());
            add(labSneakECM, GBC.std());
            add(chSneakECM, GBC.eol());
            add(Box.createVerticalStrut(10), GBC.eol());
            add(labSpec, GBC.eol());
            for (JCheckBox spec : chSpecs) {
                add(spec, GBC.eol());
            }
        }

        public void initialize() {
            inf = (Infantry) entity;
            
            SimpleTechLevel gameTechLevel = SimpleTechLevel.getGameTechLevel(client.getGame());
            int year = client.getGame().getOptions().intOption("year");
            for (Enumeration<EquipmentType> e = MiscType.getAllTypes(); e.hasMoreElements();) {
                final EquipmentType et = e.nextElement();
                if (et.hasFlag(MiscType.F_ARMOR_KIT)
                        && et.isLegal(year, gameTechLevel, entity.isClan(), entity.isMixedTech())) {
                    armorKits.add(et);
                }
            }
            armorKits.sort(Comparator.comparing(EquipmentType::getName));

            cbArmorKit.addItem(Messages.getString("CustomMechDialog.Custom"));
            armorKits.forEach(k -> cbArmorKit.addItem(k.getName()));
            EquipmentType kit = inf.getArmorKit();
            if (kit == null) {
                cbArmorKit.setSelectedIndex(0);
            } else {
                cbArmorKit.setSelectedIndex(armorKits.indexOf(kit) + 1);
            }
            fldDivisor.setText(Double.toString(inf.calcDamageDivisor()));
            chEncumber.setSelected(inf.isArmorEncumbering());
            chSpaceSuit.setSelected(inf.hasSpaceSuit());
            chDEST.setSelected(inf.hasDEST());
            chSneakCamo.setSelected(inf.hasSneakCamo());
            chSneakIR.setSelected(inf.hasSneakIR());
            chSneakECM.setSelected(inf.hasSneakECM());
            armorStateChanged();
            cbArmorKit.addActionListener(e -> {
                armorStateChanged();
                updateArmorValues();
            });
            chDEST.addItemListener(e -> armorStateChanged());

            for (int i = 0; i < Infantry.NUM_SPECIALIZATIONS; i++) {
                int spec = 1 << i;
                chSpecs.get(i).setSelected(inf.hasSpecialization(spec));
            }
        }
        
        public void armorStateChanged() {
            fldDivisor.setEnabled(cbArmorKit.getSelectedIndex() == 0);
            chEncumber.setEnabled(cbArmorKit.getSelectedIndex() == 0);
            chSpaceSuit.setEnabled(cbArmorKit.getSelectedIndex() == 0);
            chDEST.setEnabled(cbArmorKit.getSelectedIndex() == 0);
            chSneakCamo.setEnabled(cbArmorKit.getSelectedIndex() == 0
                    && !chDEST.isSelected());
            chSneakIR.setEnabled(cbArmorKit.getSelectedIndex() == 0
                    && !chDEST.isSelected());
            chSneakECM.setEnabled(cbArmorKit.getSelectedIndex() == 0
                    && !chDEST.isSelected());
        }

        public void updateArmorValues() {
            if (cbArmorKit.getSelectedIndex() > 0) {
                EquipmentType kit = armorKits.get(cbArmorKit.getSelectedIndex() - 1);
                fldDivisor.setText(Double.toString(((MiscType) kit).getDamageDivisor()));
                chEncumber.setSelected((kit.getSubType() & MiscType.S_ENCUMBERING) != 0);
                chSpaceSuit.setSelected((kit.getSubType() & MiscType.S_SPACE_SUIT) != 0);
                chDEST.setSelected((kit.getSubType() & MiscType.S_DEST) != 0);
                chSneakCamo.setSelected((kit.getSubType() & MiscType.S_SNEAK_CAMO) != 0);
                chSneakIR.setSelected((kit.getSubType() & MiscType.S_SNEAK_IR) != 0);
                chSneakECM.setSelected((kit.getSubType() & MiscType.S_SNEAK_ECM) != 0);
            }
        }

        public void applyChoice() {
            if (cbArmorKit.getSelectedIndex() > 0) {
                inf.setArmorKit(armorKits.get(cbArmorKit.getSelectedIndex() - 1));
            } else {
                inf.setArmorKit(null);
                inf.setArmorDamageDivisor(Double.parseDouble(fldDivisor.getText()));
                inf.setArmorEncumbering(chEncumber.isSelected());
                inf.setSpaceSuit(chSpaceSuit.isSelected());
                inf.setDEST(chDEST.isSelected());
                if (!chDEST.isSelected()) {
                    inf.setSneakCamo(chSneakCamo.isSelected());
                    inf.setSneakIR(chSneakIR.isSelected());
                    inf.setSneakECM(chSneakECM.isSelected());
                }
            }
            int spec = 0;
            for (int i = 0; i < Infantry.NUM_SPECIALIZATIONS; i++) {
                if (chSpecs.get(i).isSelected()) {
                    spec |= 1 << i;
                }
            }
            inf.setSpecializations(spec);
        }

        @Override
        public void setEnabled(boolean enabled) {
            cbArmorKit.setEnabled(enabled);
            if (enabled) {
                armorStateChanged();
            } else {
                fldDivisor.setEnabled(enabled);
                chEncumber.setEnabled(enabled);
                chSpaceSuit.setEnabled(enabled);
                chDEST.setEnabled(enabled);
                chSneakCamo.setEnabled(enabled);
                chSneakIR.setEnabled(enabled);
                chSneakECM.setEnabled(enabled);
            }
            for (JCheckBox spec : chSpecs) {
                spec.setEnabled(enabled);
            }
        }
    }

    private void disableMunitionEditing() {
        for (int i = 0; i < m_vMunitions.size(); i++) {
            m_vMunitions.get(i).setEnabled(false);
        }
    }
    
    private void disableAPMEditing() {
        for (int i = 0; i < m_vAPMounts.size(); i++) {
            m_vAPMounts.get(i).setEnabled(false);
        }
    }
    
    private void disableMEAEditing() {
        for (int i = 0; i < m_vMEAdaptors.size(); i++) {
            m_vMEAdaptors.get(i).setEnabled(false);
        }
    }

    private void disableMGSetting() {
        for (int i = 0; i < m_vMGs.size(); i++) {
            m_vMGs.get(i).setEnabled(false);
        }
    }

    private void disableMineSetting() {
        for (int i = 0; i < m_vMines.size(); i++) {
            m_vMines.get(i).setEnabled(false);
        }
    }

    private void refreshC3() {
        choC3.removeAllItems();
        int listIndex = 0;
        entityCorrespondance = new int[client.getGame().getNoOfEntities() + 2];

        if (entity.hasC3i() || entity.hasNavalC3()) {
            choC3.addItem(Messages.getString("CustomMechDialog.CreateNewNetwork"));
            if (entity.getC3Master() == null) {
                choC3.setSelectedIndex(listIndex);
            }
            entityCorrespondance[listIndex++] = entity.getId();
        } else if (entity.hasC3MM()) {
            int mNodes = entity.calculateFreeC3MNodes();
            int sNodes = entity.calculateFreeC3Nodes();

            choC3.addItem(Messages.getString("CustomMechDialog.setCompanyMaster", mNodes, sNodes));

            if (entity.C3MasterIs(entity)) {
                choC3.setSelectedIndex(listIndex);
            }
            entityCorrespondance[listIndex++] = entity.getId();

            choC3.addItem(Messages.getString("CustomMechDialog.setIndependentMaster", sNodes));
            if (entity.getC3Master() == null) {
                choC3.setSelectedIndex(listIndex);
            }
            entityCorrespondance[listIndex++] = -1;

        } else if (entity.hasC3M()) {
            int nodes = entity.calculateFreeC3Nodes();

            choC3.addItem(Messages.getString("CustomMechDialog.setCompanyMaster1", nodes));
            if (entity.C3MasterIs(entity)) {
                choC3.setSelectedIndex(listIndex);
            }
            entityCorrespondance[listIndex++] = entity.getId();

            choC3.addItem(Messages.getString("CustomMechDialog.setIndependentMaster", nodes));
            if (entity.getC3Master() == null) {
                choC3.setSelectedIndex(listIndex);
            }
            entityCorrespondance[listIndex++] = -1;

        }
        for (Entity e : client.getEntitiesVector()) {
            // ignore enemies or self
            if (entity.isEnemyOf(e) || entity.equals(e)) {
                continue;
            }
            // c3i only links with c3i
            if (entity.hasC3i() != e.hasC3i()) {
                continue;
            }
            // NC3 only links with NC3
            if (entity.hasNavalC3() != e.hasNavalC3()) {
                continue;
            }
            // likewise can't connect c3 to nova
            if (entity.hasNovaCEWS() != e.hasNovaCEWS()) {
                continue;
            }
            // maximum depth of a c3 network is 2 levels.
            Entity eCompanyMaster = e.getC3Master();
            if ((eCompanyMaster != null)
                    && (eCompanyMaster.getC3Master() != eCompanyMaster)) {
                continue;
            }
            int nodes = e.calculateFreeC3Nodes();
            if (e.hasC3MM() && entity.hasC3M() && e.C3MasterIs(e)) {
                nodes = e.calculateFreeC3MNodes();
            }
            if (entity.C3MasterIs(e) && !entity.equals(e)) {
                nodes++;
            }
            if ((entity.hasC3i() || entity.hasNavalC3())
                    && (entity.onSameC3NetworkAs(e) || entity.equals(e))) {
                nodes++;
            }
            if (nodes == 0) {
                continue;
            }
            if (e.hasC3i() || e.hasNavalC3()) {
                if (entity.onSameC3NetworkAs(e)) {
                    choC3.addItem(Messages.getString("CustomMechDialog.join1",
                            e.getDisplayName(), e.getC3NetId(), nodes - 1));
                    choC3.setSelectedIndex(listIndex);
                } else {
                    choC3.addItem(Messages.getString("CustomMechDialog.join2",
                            e.getDisplayName(), e.getC3NetId(), nodes));
                }
                entityCorrespondance[listIndex++] = e.getId();
            } else if (e.C3MasterIs(e) && e.hasC3MM()) {
                // Company masters with 2 computers can have
                // *both* sub-masters AND slave units.
                choC3.addItem(Messages.getString("CustomMechDialog.connect2",
                        e.getDisplayName(), e.getC3NetId(), nodes));
                entityCorrespondance[listIndex] = e.getId();
                if (entity.C3MasterIs(e)) {
                    choC3.setSelectedIndex(listIndex);
                }
                listIndex++;
            } else if (e.C3MasterIs(e) != entity.hasC3M()) {
                // If we're a slave-unit, we can only connect to sub-masters,
                // not main masters likewise, if we're a master unit, we can
                // only connect to main master units, not sub-masters.
            } else if (entity.C3MasterIs(e)) {
                choC3.addItem(Messages.getString("CustomMechDialog.connect1",
                        e.getDisplayName(), e.getC3NetId(), nodes - 1));
                choC3.setSelectedIndex(listIndex);
                entityCorrespondance[listIndex++] = e.getId();
            } else {
                choC3.addItem(Messages.getString("CustomMechDialog.connect2",
                        e.getDisplayName(), e.getC3NetId(), nodes));
                entityCorrespondance[listIndex++] = e.getId();
            }
        }
    }
}
