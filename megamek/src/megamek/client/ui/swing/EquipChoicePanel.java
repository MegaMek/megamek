/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import megamek.client.Client;
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Configuration;
import megamek.common.CriticalSlot;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.LocationFullException;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.PlanetaryConditions;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.options.IOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestBattleArmor;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * This class builds the Equipment Panel for use in MegaMek and MekHQ
 *
 * @author Dylan Myers (ralgith-erian@users.sourceforge.net)
 * @author arlith
 * @since 2012-05-20
 */
public class EquipChoicePanel extends JPanel implements Serializable {
    static final long serialVersionUID = 672299770230285567L;

    private final Entity entity;

    private int[] entityCorrespondance;

    private ArrayList<MunitionChoicePanel> m_vMunitions = new ArrayList<MunitionChoicePanel>();
    
    /**
     * An <code>ArrayList</code> to keep track of all of the 
     * <code>APWeaponChoicePanels</code> that were added, so we can apply 
     * their choices when the dialog is closed.
     */
    private ArrayList<APWeaponChoicePanel> m_vAPMounts = 
            new ArrayList<APWeaponChoicePanel>();
    
    /**
     * An <code>ArrayList</code> to keep track of all of the 
     * <code>MEAChoicePanels</code> that were added, so we can apply 
     * their choices when the dialog is closed.
     */
    private ArrayList<MEAChoicePanel> m_vMEAdaptors = 
            new ArrayList<MEAChoicePanel>();
    
    /**
     * Panel for adding components related to selecting which anti-personnel
     * weapons are mounted in an AP Mount (armored gloves are also considered 
     * AP mounts)
     **/
    private JPanel panAPMounts = new JPanel();
    private JPanel panMEAdaptors = new JPanel();
    private JPanel panMunitions = new JPanel();

    private ArrayList<RapidfireMGPanel> m_vMGs = new ArrayList<RapidfireMGPanel>();
    private JPanel panRapidfireMGs = new JPanel();

    private InfantryArmorPanel panInfArmor = new InfantryArmorPanel();

    private ArrayList<MineChoicePanel> m_vMines = new ArrayList<MineChoicePanel>();
    private JPanel panMines = new JPanel();

    private ArrayList<SantaAnnaChoicePanel> m_vSantaAnna = new ArrayList<SantaAnnaChoicePanel>();
    private JPanel panSantaAnna = new JPanel();

    private BombChoicePanel m_bombs;
    private JPanel panBombs = new JPanel();

//    private EquipChoicePanel m_equip;
//    private JPanel panEquip = new JPanel(new GridBagLayout());

    private JLabel labAutoEject = new JLabel(
            Messages.getString("CustomMechDialog.labAutoEject"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JCheckBox chAutoEject = new JCheckBox();

    private JLabel labCondEjectAmmo = new JLabel(
            Messages.getString("CustomMechDialog.labConditional_Ejection_Ammo"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JCheckBox chCondEjectAmmo = new JCheckBox();

    private JLabel labCondEjectEngine = new JLabel(
            Messages.getString("CustomMechDialog.labConditional_Ejection_Engine"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JCheckBox chCondEjectEngine = new JCheckBox();

    private JLabel labCondEjectCTDest = new JLabel(
            Messages.getString("CustomMechDialog.labConditional_Ejection_CT_Destroyed"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JCheckBox chCondEjectCTDest = new JCheckBox();

    private JLabel labCondEjectHeadshot = new JLabel(
            Messages.getString("CustomMechDialog.labConditional_Ejection_Headshot"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JCheckBox chCondEjectHeadshot = new JCheckBox();

    private JLabel labSearchlight = new JLabel(
            Messages.getString("CustomMechDialog.labSearchlight"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JCheckBox chSearchlight = new JCheckBox();

    private JLabel labC3 = new JLabel(
            Messages.getString("CustomMechDialog.labC3"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox<String> choC3 = new JComboBox<String>();

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
            if (mech.getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
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
            if (clientgui.getClient().getGame().getOptions().booleanOption(
                    "conditional_ejection")
                    && hasEjectSeat) { //$NON-NLS-1$
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
        }

        if (entity.hasC3() || entity.hasC3i()) {
            add(labC3, GBC.std());
            add(choC3, GBC.eol());
            refreshC3();
        }
        
        // Setup AP mounts
        if ((entity instanceof BattleArmor) 
                && entity.hasWorkingMisc(MiscType.F_AP_MOUNT)){
            setupAPMounts();
            panAPMounts.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEmptyBorder(), Messages
                    .getString("CustomMechDialog.APMountPanelTitle"),
                    TitledBorder.TOP, TitledBorder.DEFAULT_POSITION));
            
            add(panAPMounts,GBC.eop().anchor(GridBagConstraints.CENTER));
        }
        
        if ((entity instanceof BattleArmor) 
                && entity.hasWorkingMisc(MiscType.F_BA_MEA)){            
            panMEAdaptors.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEmptyBorder(), Messages
                    .getString("CustomMechDialog.MEAPanelTitle"),
                    TitledBorder.TOP, TitledBorder.DEFAULT_POSITION));
            // We need to determine how much weight is free, so the user can
            //  pick legal combinations of manipulators
            BattleArmor ba = (BattleArmor) entity;
            EntityVerifier verifier = new EntityVerifier(
                    new File(Configuration.unitsDir(),
                            EntityVerifier.CONFIG_FILENAME));
            TestBattleArmor testBA = new TestBattleArmor(ba, 
                    verifier.baOption, null);
            float maxTrooperWeight = 0;
            for (int i = 1; i < ba.getTroopers(); i++){
                float trooperWeight = testBA.calculateWeight(i);
                if (trooperWeight > maxTrooperWeight){
                    maxTrooperWeight = trooperWeight;
                }
            }
            String freeWeight = Messages
                    .getString("CustomMechDialog.freeWeight")
                    + String.format(": %1$.3f/%2$.3f", maxTrooperWeight,
                            ba.getTrooperWeight());
                        
            setupMEAdaptors(freeWeight);
            add(panMEAdaptors,GBC.eop().anchor(GridBagConstraints.CENTER));
        }
        
        // Can't set up munitions on infantry.
        if (!((entity instanceof Infantry) && !((Infantry) entity)
                .hasFieldGun()) || (entity instanceof BattleArmor)) {
            setupMunitions();
            panMunitions.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEmptyBorder(), Messages
                    .getString("CustomMechDialog.MunitionsPanelTitle"),
                    TitledBorder.TOP, TitledBorder.DEFAULT_POSITION));
            add(panMunitions,
                    GBC.eop().anchor(GridBagConstraints.CENTER));
        }

        // set up Santa Annas if using nukes
        if (((entity instanceof Dropship) || (entity instanceof Jumpship))
                && clientgui.getClient().getGame().getOptions().booleanOption(
                        "at2_nukes")) { //$NON-NLS-1$
            setupSantaAnna();
            add(panSantaAnna,
                    GBC.eop().anchor(GridBagConstraints.CENTER));
        }

        if ((entity instanceof Aero)
                && !((entity instanceof SmallCraft) ||
                        (entity instanceof Jumpship))) {
            setupBombs();
            add(panBombs, GBC.eop().anchor(GridBagConstraints.CENTER));
        }

        // Set up rapidfire mg
        if (clientgui.getClient().getGame().getOptions().booleanOption(
                "tacops_burst")) { //$NON-NLS-1$
            setupRapidfireMGs();
            add(panRapidfireMGs,
                    GBC.eop().anchor(GridBagConstraints.CENTER));
        }

        // set up infantry armor
        if ((entity instanceof Infantry) && !(entity instanceof BattleArmor)) {
            panInfArmor.initialize();
            add(panInfArmor,
                    GBC.eop().anchor(GridBagConstraints.CENTER));
        }

        // Set up searchlight
        if (clientgui.getClient().getGame().getPlanetaryConditions().getLight() > PlanetaryConditions.L_DUSK) {
            add(labSearchlight, GBC.std());
            add(chSearchlight, GBC.eol());
            chSearchlight.setSelected(entity.hasSpotlight()
                    || entity.hasQuirk(OptionsConstants.QUIRK_POS_SEARCHLIGHT));
        }

        // Set up mines
        setupMines();
        add(panMines, GBC.eop().anchor(GridBagConstraints.CENTER));
    }

    public void initialize() {
        choC3.setEnabled(false);
        chAutoEject.setEnabled(false);
        chSearchlight.setEnabled(false);
        if (m_bombs != null){
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
        boolean autoEject = chAutoEject.isSelected();
        boolean condEjectAmmo = chCondEjectAmmo.isSelected();
        boolean condEjectEngine = chCondEjectEngine.isSelected();
        boolean condEjectCTDest = chCondEjectCTDest.isSelected();
        boolean condEjectHeadshot = chCondEjectHeadshot.isSelected();

        if (entity instanceof Mech) {
            Mech mech = (Mech) entity;
            mech.setAutoEject(!autoEject);
            mech.setCondEjectAmmo(condEjectAmmo);
            mech.setCondEjectEngine(condEjectEngine);
            mech.setCondEjectCTDest(condEjectCTDest);
            mech.setCondEjectHeadshot(condEjectHeadshot);
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
        // update MG rapid fire settings
        for (final Object newVar1 : m_vMGs) {
            ((RapidfireMGPanel) newVar1).applyChoice();
        }
        // update mines setting
        for (final Object newVar : m_vMines) {
            ((MineChoicePanel) newVar).applyChoice();
        }
        // update Santa Anna setting
        for (final Object newVar : m_vSantaAnna) {
            ((SantaAnnaChoicePanel) newVar).applyChoice();
        }
        // update bomb setting
        if (null != m_bombs) {
            m_bombs.applyChoice();
        }
        if ((entity instanceof Infantry)
                && !(entity instanceof BattleArmor)) {
            panInfArmor.applyChoice();
        }

        // update searchlight setting
        entity.setExternalSpotlight(chSearchlight.isSelected());
        entity.setSpotlightState(chSearchlight.isSelected());

        if (entity.hasC3() && (choC3.getSelectedIndex() > -1)) {
            Entity chosen = client.getEntity(entityCorrespondance[choC3
                    .getSelectedIndex()]);
            int entC3nodeCount = client.getGame().getC3SubNetworkMembers(entity)
                    .size();
            int choC3nodeCount = client.getGame().getC3NetworkMembers(chosen)
                    .size();
            
            if ((entC3nodeCount + choC3nodeCount) <= Entity.MAX_C3_NODES
                    && ((chosen == null) 
                            || entity.getC3MasterId() != chosen.getId())) {
                entity.setC3Master(chosen, true);
            } else if (entity.getC3MasterId() != chosen.getId()){
                String message = Messages
                        .getString(
                                "CustomMechDialog.NetworkTooBig.message", new Object[] {//$NON-NLS-1$
                                entity.getShortName(),
                                        chosen.getShortName(),
                                        new Integer(entC3nodeCount),
                                        new Integer(choC3nodeCount),
                                        new Integer(Entity.MAX_C3_NODES) });
                clientgui.doAlertDialog(Messages
                        .getString("CustomMechDialog.NetworkTooBig.title"), //$NON-NLS-1$
                        message);
                refreshC3();
            }
        } else if (entity.hasC3i() && (choC3.getSelectedIndex() > -1)) {
            entity.setC3NetId(client.getEntity(entityCorrespondance[choC3
                    .getSelectedIndex()]));
        }
    }

    private void setupBombs() {
        GridBagLayout gbl = new GridBagLayout();
        panBombs.setLayout(gbl);

        int techlvl = Arrays.binarySearch(TechConstants.T_SIMPLE_NAMES, client
                .getGame().getOptions().stringOption("techlevel")); //$NON-NLS-1$
        boolean allowNukes = client.getGame().getOptions()
                .booleanOption("at2_nukes"); //$NON-NLS-1$
        m_bombs = new BombChoicePanel((Aero) entity, allowNukes,
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

    private void setupSantaAnna() {
        GridBagLayout gbl = new GridBagLayout();
        panSantaAnna.setLayout(gbl);
        for (Mounted m : entity.getAmmo()) {
            AmmoType at = (AmmoType) m.getType();
            // Santa Annas?
            if (clientgui.getClient().getGame().getOptions().booleanOption(
                    "at2_nukes")
                    && ((at.getAmmoType() == AmmoType.T_KILLER_WHALE) || ((at
                            .getAmmoType() == AmmoType.T_AR10) && at
                            .hasFlag(AmmoType.F_AR10_KILLER_WHALE)))) {
                SantaAnnaChoicePanel sacp = new SantaAnnaChoicePanel(m);
                panSantaAnna.add(sacp, GBC.eol());
                m_vSantaAnna.add(sacp);
            }
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
        panMEAdaptors.add(lblFreeWeight,
                GBC.eol().anchor(GridBagConstraints.CENTER));

        ArrayList<MiscType> manipTypes = new ArrayList<MiscType>();
        
        for (String manipTypeName : BattleArmor.MANIPULATOR_TYPE_STRINGS){
            // Ignore the "None" option
            if (manipTypeName.equals(BattleArmor.MANIPULATOR_TYPE_STRINGS[0])){
                continue;
            }
            MiscType mType = (MiscType)EquipmentType.get(manipTypeName);
            manipTypes.add(mType);
        }
        
        for (Mounted m : entity.getMisc()){
            if (!m.getType().hasFlag(MiscType.F_BA_MEA)){
                continue;
            }
            Mounted currentManip = null;
            if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_LARM){
                currentManip = ((BattleArmor)entity).getLeftManipulator();
            } else if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_RARM){
                currentManip = ((BattleArmor)entity).getRightManipulator();
            } else {
                // We can only have MEA's in an arm
                continue;
            }
            MEAChoicePanel meacp;
            meacp = new MEAChoicePanel(entity, m.getBaMountLoc(), currentManip, 
                    manipTypes);
            
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
        ArrayList<WeaponType> apWeapTypes = new ArrayList<WeaponType>(100);
        // Weapons that can be used in an Armored Glove
        ArrayList<WeaponType> agWeapTypes = new ArrayList<WeaponType>(100);
        Enumeration<EquipmentType> allTypes = EquipmentType.getAllTypes();
        while (allTypes.hasMoreElements()){
            EquipmentType eq = allTypes.nextElement();
            
            // If it's not an infantry weapon, we don't care
            if (!(eq instanceof InfantryWeapon)){
                continue;
            }
            
            // Check to see if the tech level of the equipment is legal
            if (!TechConstants.isLegal(entity.getTechLevel(), 
                    eq.getTechLevel(entity.getTechLevelYear()), false,
                    entity.isMixedTech())){
                continue;
            }
            
            // Check to see if we've got a valid infantry weapon
            InfantryWeapon infWeap = (InfantryWeapon)eq;
            if (infWeap.hasFlag(WeaponType.F_INFANTRY)
                    && !infWeap.hasFlag(WeaponType.F_INF_POINT_BLANK)
                    && !infWeap.hasFlag(WeaponType.F_INF_ARCHAIC)
                    && !infWeap.hasFlag(WeaponType.F_INF_SUPPORT)){
                apWeapTypes.add(infWeap);
            }
            if (infWeap.hasFlag(WeaponType.F_INFANTRY)
                    && !infWeap.hasFlag(WeaponType.F_INF_POINT_BLANK)
                    && !infWeap.hasFlag(WeaponType.F_INF_ARCHAIC)
                    && (infWeap.getCrew() < 2)){
                agWeapTypes.add(infWeap);
            }
        }

        ArrayList<Mounted> armoredGloves = new ArrayList<Mounted>(2);
        for (Mounted m : entity.getMisc()){
            if (!m.getType().hasFlag(MiscType.F_AP_MOUNT)){
                continue;
            }
            APWeaponChoicePanel apcp = null;
            // Armored gloves need to be treated slightly differently, since
            // 1 or 2 armored gloves allow 1 additional AP weapon
            if (m.getType().hasFlag(MiscType.F_ARMORED_GLOVE)) {
                armoredGloves.add(m);                
            } else{
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
            } else if ((aGlove.getLinked() == null) 
                    && (ag.getLinked() != null)) {
                aGlove = ag;
            } 
            // If both are linked, TestBattleArmor will mark unit as invalid
        }
        if (aGlove != null) {
            APWeaponChoicePanel apcp = new APWeaponChoicePanel(entity, aGlove,
                    agWeapTypes);
            panAPMounts.add(apcp, GBC.eol());
            m_vAPMounts.add(apcp);
        }
    }

    private void setupMunitions() {
        GridBagLayout gbl = new GridBagLayout();
        panMunitions.setLayout(gbl);
        IGame game = clientgui.getClient().getGame();
        IOptions gameOpts = game.getOptions();
        int gameYear = gameOpts.intOption("year");
        boolean isClan = entity.isClan();

        for (Mounted m : entity.getAmmo()) {
            AmmoType at = (AmmoType) m.getType();
            ArrayList<AmmoType> vTypes = new ArrayList<AmmoType>();
            Vector<AmmoType> vAllTypes = AmmoType.getMunitionsFor(at
                    .getAmmoType());
            if (vAllTypes == null) {
                continue;
            }

            // don't allow ammo switching of most things for Aeros
            // allow only MML, ATM, and NARC
            // TODO: need a better way to customize munitions on Aeros
            // currently this doesn't allow AR10 and tele-missile launchers
            // to switch back and forth between tele and regular missiles
            // also would be better to not have to add Santa Anna's in such
            // an idiosyncratic fashion
            if ((entity instanceof Aero)
                    && !((at.getAmmoType() == AmmoType.T_MML)
                            || (at.getAmmoType() == AmmoType.T_ATM)
                            || (at.getAmmoType() == AmmoType.T_NARC))) {
                continue;
            }

            for (AmmoType atCheck : vAllTypes) {
                int atTechLvl = atCheck.getTechLevel(gameYear);
                int legalLevel = TechConstants.getGameTechLevel(game, isClan);
                boolean bTechMatch = TechConstants.isLegal(legalLevel,
                        atTechLvl, true, entity.isMixedTech());

                // If clan_ignore_eq_limits is unchecked,
                // do NOT allow Clans to use IS-only ammo.
                // N.B. play bit-shifting games to allow "incendiary"
                // to be combined to other munition types.
                long muniType = atCheck.getMunitionType();
                muniType &= ~AmmoType.M_INCENDIARY_LRM;
                if (!gameOpts.booleanOption("clan_ignore_eq_limits") //$NON-NLS-1$
                        && entity.isClan()
                        && ((muniType == AmmoType.M_SEMIGUIDED)
                                || (muniType == AmmoType.M_SWARM_I)
                                || (muniType == AmmoType.M_FLARE)
                                || (muniType == AmmoType.M_FRAGMENTATION)
                                || (muniType == AmmoType.M_THUNDER_AUGMENTED)
                                || (muniType == AmmoType.M_THUNDER_INFERNO)
                                || (muniType == AmmoType.M_THUNDER_VIBRABOMB)
                                || (muniType == AmmoType.M_THUNDER_ACTIVE)
                                || (muniType == AmmoType.M_INFERNO_IV)
                                || (muniType == AmmoType.M_VIBRABOMB_IV)
                                || (muniType == AmmoType.M_LISTEN_KILL)
                                || (muniType == AmmoType.M_ANTI_TSM) 
                                || (muniType == AmmoType.M_SMOKE_WARHEAD))) {
                    bTechMatch = false;
                }

                if (!gameOpts.booleanOption("minefields") && //$NON-NLS-1$
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
                    && !client.getGame().getOptions()
                            .booleanOption("lobby_ammo_dump")
                    && !client.getGame().getOptions()
                            .booleanOption("tacops_hotload")) { //$NON-NLS-1$
                continue;
            }
            MunitionChoicePanel mcp;
            mcp = new MunitionChoicePanel(m, vTypes);
            panMunitions.add(mcp, GBC.eol());
            m_vMunitions.add(mcp);
        }
        }

        class MineChoicePanel extends JPanel {
            /**
             *
             */
            private static final long serialVersionUID = -1868675102440527538L;

            private JComboBox<String> m_choice;

            private Mounted m_mounted;

            MineChoicePanel(Mounted m) {
                m_mounted = m;
                m_choice = new JComboBox<String>();
                m_choice.addItem(Messages
                        .getString("CustomMechDialog.Conventional")); //$NON-NLS-1$
                m_choice.addItem(Messages.getString("CustomMechDialog.Vibrabomb")); //$NON-NLS-1$
                // m_choice.add("Messages.getString("CustomMechDialog.Command-detonated"));
                // //$NON-NLS-1$
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
         *
         */
        class APWeaponChoicePanel extends JPanel {
            
            /**
             * 
             */
            private static final long serialVersionUID = 6189888202192403704L;

            private Entity entity;
            
            private ArrayList<WeaponType> m_APWeaps;

            private JComboBox<String> m_choice;

            private Mounted m_APmounted;

            APWeaponChoicePanel(Entity e, Mounted m, 
                    ArrayList<WeaponType> weapons) {
                entity = e;
                m_APWeaps = weapons;
                m_APmounted = m;
                EquipmentType  curType = null;
                if (m != null && m.getLinked() != null){
                    curType = m.getLinked().getType();
                }
                m_choice = new JComboBox<String>();
                m_choice.addItem("None");
                m_choice.setSelectedIndex(0);
                Iterator<WeaponType> it = m_APWeaps.iterator();
                for (int x = 1; it.hasNext(); x++) {
                    WeaponType weap = it.next();
                    m_choice.addItem(weap.getName());
                    if (curType != null && 
                            weap.getInternalName() == 
                                curType.getInternalName()) {
                        m_choice.setSelectedIndex(x);
                    }
                }

                String sDesc = "";
                if (m.getBaMountLoc() != BattleArmor.MOUNT_LOC_NONE){
                    sDesc += " (" 
                            + BattleArmor.MOUNT_LOC_NAMES[m.getBaMountLoc()] 
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
                if (n == -1){
                    return;
                }
                WeaponType apType = null;
                if (n > 0 && n <= m_APWeaps.size()){
                    // Need to account for the "None" selection
                    apType = m_APWeaps.get(n-1);
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
                        || n == 0){
                    return;
                }
                    
                // Add the newly mounted weapon
                try{
                    Mounted newWeap =  entity.addEquipment(apType, 
                            m_APmounted.getLocation());
                    m_APmounted.setLinked(newWeap);
                    newWeap.setLinked(m_APmounted);
                    newWeap.setAPMMounted(true);
                } catch (LocationFullException ex){
                    // This shouldn't happen for BA...
                    ex.printStackTrace();
                }

            }

            @Override
            public void setEnabled(boolean enabled) {
                m_choice.setEnabled(enabled);
            }

        }
        
        /**
         * A panel that houses a label and a combo box that allows for selecting
         * which maniulator is mounted in a modular equipment adaptor.
         * 
         * @author arlith
         *
         */
        class MEAChoicePanel extends JPanel {
            
            /**
             * 
             */
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
                if (m != null){
                    curType = m.getType();
                }
                m_choice = new JComboBox<String>();
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
                if (baMountLoc != BattleArmor.MOUNT_LOC_NONE){
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
                if (n == -1){
                    return;
                }
                MiscType manipType = null;
                if (n > 0 && n <= m_Manipulators.size()){
                    // Need to account for the "None" selection
                    manipType = m_Manipulators.get(n-1);
                }

                if (m_Manipmounted != null){
                    entity.getEquipment().remove(m_Manipmounted);
                    entity.getMisc().remove(m_Manipmounted);
                }            
                
                // Was no manipulator selected?
                if (n == 0){
                    return;
                }
                    
                // Add the newly mounted maniplator
                try{
                    m_Manipmounted = entity.addEquipment(manipType, 
                            m_Manipmounted.getLocation());
                    m_Manipmounted.setBaMountLoc(baMountLoc);
                } catch (LocationFullException ex){
                    // This shouldn't happen for BA...
                    ex.printStackTrace();
                }

            }

            @Override
            public void setEnabled(boolean enabled) {
                m_choice.setEnabled(enabled);
            }

        }

        class MunitionChoicePanel extends JPanel {
            /**
             *
             */
            private static final long serialVersionUID = 3401106035583965326L;

            private ArrayList<AmmoType> m_vTypes;

            private JComboBox<String> m_choice;

            @SuppressWarnings("rawtypes")
            private JComboBox m_num_shots;

            private Mounted m_mounted;

            JLabel labDump = new JLabel(
                    Messages.getString("CustomMechDialog.labDump")); //$NON-NLS-1$

            JCheckBox chDump = new JCheckBox();

            JLabel labHotLoad = new JLabel(
                    Messages.getString("CustomMechDialog.switchToHotLoading")); //$NON-NLS-1$

            JCheckBox chHotLoad = new JCheckBox();
            
            @SuppressWarnings("unchecked")
            MunitionChoicePanel(Mounted m, ArrayList<AmmoType> vTypes) {
                m_vTypes = vTypes;
                m_mounted = m;
                AmmoType curType = (AmmoType) m.getType();
                m_choice = new JComboBox<String>();
                Iterator<AmmoType> e = m_vTypes.iterator();
                for (int x = 0; e.hasNext(); x++) {
                    AmmoType at = e.next();
                    m_choice.addItem(at.getName());
                    if (at.getInternalName() == curType.getInternalName()) {
                        m_choice.setSelectedIndex(x);
                    }
                }

                m_num_shots = new JComboBox<String>();
                int shotsPerTon = curType.getShots();
                // BattleArmor always have a certain number of shots per slot
                int stepSize = 1;
                if (entity instanceof BattleArmor){
                    if (curType.getAmmoType() == AmmoType.T_BA_TUBE) {
                        shotsPerTon = TestBattleArmor.NUM_SHOTS_PER_CRIT_TA;
                        stepSize = 2;
                    } else {
                        shotsPerTon = TestBattleArmor.NUM_SHOTS_PER_CRIT;
                    }
                }
                for (int i = 0; i <= shotsPerTon; i += stepSize){
                    m_num_shots.addItem(i);
                }
                m_num_shots.setSelectedItem(m_mounted.getBaseShotsLeft());

                m_choice.addItemListener(new ItemListener(){
                    @Override
                    public void itemStateChanged(ItemEvent evt) {
                        int currShots = (Integer)m_num_shots.getSelectedItem();
                        m_num_shots.removeAllItems();
                        int shotsPerTon = m_vTypes.get(
                                m_choice.getSelectedIndex()).getShots();
                        // BA always have a certain number of shots per slot
                        if (entity instanceof BattleArmor){
                            shotsPerTon = TestBattleArmor.NUM_SHOTS_PER_CRIT;
                        }
                        for (int i = 0; i <= shotsPerTon; i++){
                            m_num_shots.addItem(i);
                        }
                        if (currShots <= shotsPerTon){
                            m_num_shots.setSelectedItem(currShots);
                        } else {
                            m_num_shots.setSelectedItem(shotsPerTon);
                        }

                    }});


                int loc;
                boolean isOneShot = false;
                if (m.getLocation() == Entity.LOC_NONE) {
                    // oneshot weapons don't have a location of their own
                    Mounted linkedBy = m.getLinkedBy();
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
                if (clientgui.getClient().getGame().getOptions().booleanOption(
                        "lobby_ammo_dump")) { //$NON-NLS-1$
                    add(labDump, GBC.std());
                    add(chDump, GBC.eol());
                    if (clientgui.getClient().getGame().getOptions().booleanOption(
                            "tacops_hotload")
                            && curType.hasFlag(AmmoType.F_HOTLOAD)) {
                        add(labHotLoad, GBC.std());
                        add(chHotLoad, GBC.eol());
                    }
                } else if (clientgui.getClient().getGame().getOptions().booleanOption(
                        "tacops_hotload")
                        && curType.hasFlag(AmmoType.F_HOTLOAD)) {
                    add(labHotLoad, GBC.std());
                    add(chHotLoad, GBC.eol());
                }
            }

            public void applyChoice() {
                int n = m_choice.getSelectedIndex();
                // If there's no selection, there's nothing we can do
                if (n == -1){
                    return;
                }
                AmmoType at = m_vTypes.get(n);
                m_mounted.changeAmmoType(at);
                m_mounted.setShotsLeft((Integer)m_num_shots.getSelectedItem());
                if (chDump.isSelected()) {
                    m_mounted.setShotsLeft(0);
                }
                if (clientgui.getClient().getGame().getOptions().booleanOption(
                        "tacops_hotload")) {
                    if (chHotLoad.isSelected() != m_mounted.isHotLoaded()) {
                        m_mounted.setHotLoad(chHotLoad.isSelected());
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

        // a choice panel for determining number of santa anna warheads
        class SantaAnnaChoicePanel extends JPanel {
            /**
             *
             */
            private static final long serialVersionUID = -1645895479085898410L;

            private JComboBox<String> m_choice;

            private Mounted m_mounted;

            public SantaAnnaChoicePanel(Mounted m) {
                m_mounted = m;
                m_choice = new JComboBox<String>();
                for (int i = 0; i <= m_mounted.getBaseShotsLeft(); i++) {
                    m_choice.addItem(Integer.toString(i));
                }
                int loc;
                loc = m.getLocation();
                String sDesc = "Nuclear warheads for " + m_mounted.getName() + " (" + entity.getLocationAbbr(loc) + "):"; //$NON-NLS-1$ //$NON-NLS-2$
                JLabel lLoc = new JLabel(sDesc);
                GridBagLayout g = new GridBagLayout();
                setLayout(g);
                add(lLoc, GBC.std());
                m_choice.setSelectedIndex(m.getNSantaAnna());
                add(m_choice, GBC.eol());
            }

            public void applyChoice() {
                // this is a hack. I can't immediately apply the choice, because
                // that would split this ammo bin in two and then the player could
                // never
                // get back to it. So I keep track of the Santa Anna allocation
                // on the mounted and then apply it before deployment
                m_mounted.setNSantaAnna(m_choice.getSelectedIndex());
            }

            @Override
            public void setEnabled(boolean enabled) {
                m_choice.setEnabled(enabled);
            }
        }

        /**
     * When a Protomech selects ammo, you need to adjust the shots on the unit
     * for the weight of the selected munition.
     * 
     * @deprecated I don't see any purpose for this anymore, but I can't tell
     *             why it was originally added. Using this for Protomechs ends
     *             up adjusting the ammo incorrectly. It is true that Protos use
     *             ammo as kg/shot, and they aren't restricted to the max per
     *             shots per slot, but this really doesn't handle that.  Really,
     *             we need a ProtomechVerifier to ensure users don't add more 
     *             ammo than the Proto can support.
     */
        @Deprecated
        class ProtomechMunitionChoicePanel extends MunitionChoicePanel {
            /**
             *
             */
            private static final long serialVersionUID = -8170286698673268120L;

            private final float m_origShotsLeft;

            private final AmmoType m_origAmmo;

            ProtomechMunitionChoicePanel(Mounted m, ArrayList<AmmoType> vTypes) {
                super(m, vTypes);
                m_origAmmo = (AmmoType) m.getType();
                m_origShotsLeft = m.getBaseShotsLeft();
            }

            /**
             * All ammo must be applied in ratios to the starting load.
             */
            @Override
            public void applyChoice() {
                super.applyChoice();

                // Calculate the number of shots for the new ammo.
                // N.B. Some special ammos are twice as heavy as normal
                // so they have half the number of shots (rounded down).
                setShotsLeft(Math.round((getShotsLeft() * m_origShotsLeft)
                        / m_origAmmo.getShots()));
                if (chDump.isSelected()) {
                    setShotsLeft(0);
                }
            }
        }

        class RapidfireMGPanel extends JPanel {
            /**
             *
             */
            private static final long serialVersionUID = 5261919826318225201L;

            private Mounted m_mounted;

            JCheckBox chRapid = new JCheckBox();

            RapidfireMGPanel(Mounted m) {
                m_mounted = m;
                int loc = m.getLocation();
                String sDesc = Messages
                        .getString(
                                "CustomMechDialog.switchToRapidFire", new Object[] { entity.getLocationAbbr(loc) }); //$NON-NLS-1$
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
            /**
             *
             */
            private static final long serialVersionUID = -909995917737642853L;

            private Infantry inf;
            JLabel labArmor = new JLabel(
                    Messages.getString("CustomMechDialog.labInfantryArmor"));
            JLabel labDivisor = new JLabel(
                    Messages.getString("CustomMechDialog.labDamageDivisor"));
            JLabel labEncumber = new JLabel(
                    Messages.getString("CustomMechDialog.labEncumber"));
            JLabel labSpaceSuit = new JLabel(
                    Messages.getString("CustomMechDialog.labSpaceSuit"));
            JLabel labDEST = new JLabel(
                    Messages.getString("CustomMechDialog.labDEST"));
            JLabel labMountain = new JLabel(
                    Messages.getString("CustomMechDialog.labMountain"));
            JLabel labSneakCamo = new JLabel(
                    Messages.getString("CustomMechDialog.labSneakCamo"));
            JLabel labSneakIR = new JLabel(
                    Messages.getString("CustomMechDialog.labSneakIR"));
            JLabel labSneakECM = new JLabel(
                    Messages.getString("CustomMechDialog.labSneakECM"));
            private JTextField fldDivisor = new JTextField(3);
            JCheckBox chEncumber = new JCheckBox();
            JCheckBox chSpaceSuit = new JCheckBox();
            JCheckBox chDEST = new JCheckBox();
            JCheckBox chMountain = new JCheckBox();
            JCheckBox chSneakCamo = new JCheckBox();
            JCheckBox chSneakIR = new JCheckBox();
            JCheckBox chSneakECM = new JCheckBox();

            InfantryArmorPanel() {
                GridBagLayout g = new GridBagLayout();
                setLayout(g);
                add(labArmor, GBC.eol());
                add(labDivisor, GBC.std());
                add(fldDivisor, GBC.eol());
                add(labEncumber, GBC.std());
                add(chEncumber, GBC.eol());
                add(labSpaceSuit, GBC.std());
                add(chSpaceSuit, GBC.eol());
                add(labDEST, GBC.std());
                add(chDEST, GBC.eol());
                add(labMountain, GBC.std());
                add(chMountain, GBC.eol());
                add(labSneakCamo, GBC.std());
                add(chSneakCamo, GBC.eol());
                add(labSneakIR, GBC.std());
                add(chSneakIR, GBC.eol());
                add(labSneakECM, GBC.std());
                add(chSneakECM, GBC.eol());
            }

            public void initialize() {
                inf = (Infantry) entity;
                fldDivisor.setText(Double.toString(inf.getDamageDivisor()));
                chEncumber.setSelected(inf.isArmorEncumbering());
                chSpaceSuit.setSelected(inf.hasSpaceSuit());
                chDEST.setSelected(inf.hasDEST());
                chMountain.setSelected(inf.hasMountain());
                chSneakCamo.setSelected(inf.hasSneakCamo());
                chSneakIR.setSelected(inf.hasSneakIR());
                chSneakECM.setSelected(inf.hasSneakECM());
                if (chDEST.isSelected()) {
                    chSneakCamo.setEnabled(false);
                    chSneakIR.setEnabled(false);
                    chSneakECM.setEnabled(false);
                }
                chDEST.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent event) {
                        if (event.getStateChange() == ItemEvent.SELECTED) {
                            chSneakCamo.setSelected(false);
                            chSneakCamo.setEnabled(false);
                            chSneakIR.setSelected(false);
                            chSneakIR.setEnabled(false);
                            chSneakECM.setSelected(false);
                            chSneakECM.setEnabled(false);
                        } else if (event.getStateChange() == ItemEvent.DESELECTED) {
                            chSneakCamo.setEnabled(true);
                            chSneakIR.setEnabled(true);
                            chSneakECM.setEnabled(true);
                        }
                    }
                });
            }

            public void applyChoice() {
                inf.setDamageDivisor(Double.valueOf(fldDivisor.getText()));
                inf.setArmorEncumbering(chEncumber.isSelected());
                inf.setSpaceSuit(chSpaceSuit.isSelected());
                inf.setDEST(chDEST.isSelected());
                inf.setMountain(chMountain.isSelected());
                inf.setSneakCamo(chSneakCamo.isSelected());
                inf.setSneakIR(chSneakIR.isSelected());
                inf.setSneakECM(chSneakECM.isSelected());

            }

            @Override
            public void setEnabled(boolean enabled) {
                fldDivisor.setEnabled(enabled);
                chEncumber.setEnabled(enabled);
                chSpaceSuit.setEnabled(enabled);
                chDEST.setEnabled(enabled);
                chMountain.setEnabled(enabled);
                chSneakCamo.setEnabled(enabled);
                chSneakIR.setEnabled(enabled);
                chSneakECM.setEnabled(enabled);
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

            if (entity.hasC3i()) {
                choC3.addItem(Messages
                        .getString("CustomMechDialog.CreateNewNetwork")); //$NON-NLS-1$
                if (entity.getC3Master() == null) {
                    choC3.setSelectedIndex(listIndex);
                }
                entityCorrespondance[listIndex++] = entity.getId();
            } else if (entity.hasC3MM()) {
                int mNodes = entity.calculateFreeC3MNodes();
                int sNodes = entity.calculateFreeC3Nodes();

                choC3.addItem(Messages
                        .getString(
                                "CustomMechDialog.setCompanyMaster", new Object[] { new Integer(mNodes), new Integer(sNodes) })); //$NON-NLS-1$

                if (entity.C3MasterIs(entity)) {
                    choC3.setSelectedIndex(listIndex);
                }
                entityCorrespondance[listIndex++] = entity.getId();

                choC3.addItem(Messages
                        .getString(
                                "CustomMechDialog.setIndependentMaster", new Object[] { new Integer(sNodes) })); //$NON-NLS-1$
                if (entity.getC3Master() == null) {
                    choC3.setSelectedIndex(listIndex);
                }
                entityCorrespondance[listIndex++] = -1;

            } else if (entity.hasC3M()) {
                int nodes = entity.calculateFreeC3Nodes();

                choC3.addItem(Messages
                        .getString(
                                "CustomMechDialog.setCompanyMaster1", new Object[] { new Integer(nodes) })); //$NON-NLS-1$
                if (entity.C3MasterIs(entity)) {
                    choC3.setSelectedIndex(listIndex);
                }
                entityCorrespondance[listIndex++] = entity.getId();

                choC3.addItem(Messages
                        .getString(
                                "CustomMechDialog.setIndependentMaster", new Object[] { new Integer(nodes) })); //$NON-NLS-1$
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
                if (entity.hasC3i()
                        && (entity.onSameC3NetworkAs(e) || entity.equals(e))) {
                    nodes++;
                }
                if (nodes == 0) {
                    continue;
                }
                if (e.hasC3i()) {
                    if (entity.onSameC3NetworkAs(e)) {
                        choC3.addItem(Messages
                                .getString(
                                        "CustomMechDialog.join1", new Object[] { e.getDisplayName(), e.getC3NetId(), new Integer(nodes - 1) })); //$NON-NLS-1$
                        choC3.setSelectedIndex(listIndex);
                    } else {
                        choC3.addItem(Messages
                                .getString(
                                        "CustomMechDialog.join2", new Object[] { e.getDisplayName(), e.getC3NetId(), new Integer(nodes) })); //$NON-NLS-1$
                    }
                    entityCorrespondance[listIndex++] = e.getId();
                } else if (e.C3MasterIs(e) && e.hasC3MM()) {
                    // Company masters with 2 computers can have
                    // *both* sub-masters AND slave units.
                    choC3.addItem(Messages
                            .getString(
                                    "CustomMechDialog.connect2", new Object[] { e.getDisplayName(), e.getC3NetId(), new Integer(nodes) })); //$NON-NLS-1$
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
                    choC3.addItem(Messages
                            .getString(
                                    "CustomMechDialog.connect1", new Object[] { e.getDisplayName(), e.getC3NetId(), new Integer(nodes - 1) })); //$NON-NLS-1$
                    choC3.setSelectedIndex(listIndex);
                    entityCorrespondance[listIndex++] = e.getId();
                } else {
                    choC3.addItem(Messages
                            .getString(
                                    "CustomMechDialog.connect2", new Object[] { e.getDisplayName(), e.getC3NetId(), new Integer(nodes) })); //$NON-NLS-1$
                    entityCorrespondance[listIndex++] = e.getId();
                }
            }
        }
}
