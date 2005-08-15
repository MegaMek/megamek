/*
 * MechSelectorDialog.java - Copyright (C) 2002,2004 Josh Yockey
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
 
package megamek.client.ui.AWT;
 
import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;
import java.util.Vector;

import megamek.client.Client;
import megamek.client.ui.AWT.widget.BufferedPanel;
import megamek.common.*;
import megamek.common.loaders.EntityLoadingException;
import com.sun.java.util.collections.Arrays;
import com.sun.java.util.collections.Iterator;

/* 
 * Allows a user to sort through a list of MechSummaries and select one
 */

public class MechSelectorDialog 
    extends Dialog implements ActionListener, ItemListener, KeyListener, 
    Runnable, WindowListener
{
    // how long after a key is typed does a new search begin
    private final static int KEY_TIMEOUT = 1000;
     
    // these indices should match up with the static values in the MechSummaryComparator
    private String[] m_saSorts = { Messages.getString("MechSelectorDialog.0"), Messages.getString("MechSelectorDialog.1"), Messages.getString("MechSelectorDialog.2"), Messages.getString("MechSelectorDialog.3"), Messages.getString("MechSelectorDialog.4"), Messages.getString("MechSelectorDialog.5") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    
    private MechSummary[] m_mechsCurrent;
    private Client m_client;
    private ClientGUI m_clientgui;
    private UnitLoadingDialog unitLoadingDialog;
        
    private StringBuffer m_sbSearch = new StringBuffer();
    private long m_nLastSearch = 0;
    
    private Label m_labelWeightClass = new Label(Messages.getString("MechSelectorDialog.m_labelWeightClass"), Label.RIGHT); //$NON-NLS-1$
    private Choice m_chWeightClass = new Choice();
    private Label m_labelType = new Label(Messages.getString("MechSelectorDialog.m_labelType"), Label.RIGHT); //$NON-NLS-1$
    private Choice m_chType = new Choice();
    private Label m_labelUnitType = new Label(Messages.getString("MechSelectorDialog.m_labelUnitType"), Label.RIGHT); //$NON-NLS-1$
    private Choice m_chUnitType = new Choice();
    private Label m_labelSort = new Label(Messages.getString("MechSelectorDialog.m_labelSort"), Label.RIGHT); //$NON-NLS-1$
    private Choice m_chSort = new Choice();
    private Panel m_pParams = new Panel();
    private Panel m_pListOptions = new Panel();
    private Label m_labelListOptions = new Label(Messages.getString("MechSelectorDialog.m_labelListOptions"));
    private Checkbox m_cModel = new Checkbox(Messages.getString("MechSelectorDialog.m_cModel"), GUIPreferences.getInstance().getMechSelectorIncludeModel());
    private Checkbox m_cName = new Checkbox(Messages.getString("MechSelectorDialog.m_cName"), GUIPreferences.getInstance().getMechSelectorIncludeName());
    private Checkbox m_cTons = new Checkbox(Messages.getString("MechSelectorDialog.m_cTons"), GUIPreferences.getInstance().getMechSelectorIncludeTons());
    private Checkbox m_cBV = new Checkbox(Messages.getString("MechSelectorDialog.m_cBV"), GUIPreferences.getInstance().getMechSelectorIncludeBV());
    private Checkbox m_cYear = new Checkbox(Messages.getString("MechSelectorDialog.m_cYear"), GUIPreferences.getInstance().getMechSelectorIncludeYear());
    private Checkbox m_cLevel = new Checkbox(Messages.getString("MechSelectorDialog.m_cLevel"), GUIPreferences.getInstance().getMechSelectorIncludeLevel());
    private Checkbox m_cCost = new Checkbox(Messages.getString("MechSelectorDialog.m_cCost"), GUIPreferences.getInstance().getMechSelectorIncludeCost());
    List m_mechList = new List(10);
    private Button m_bPick = new Button(Messages.getString("MechSelectorDialog.m_bPick")); //$NON-NLS-1$
    private Button m_bPickClose = new Button(Messages.getString("MechSelectorDialog.m_bPickClose")); //$NON-NLS-1$    
    private Button m_bCancel = new Button(Messages.getString("Close")); //$NON-NLS-1$
    private Panel m_pButtons = new Panel();
    private TextArea m_mechViewLeft = new TextArea("",18,25,TextArea.SCROLLBARS_HORIZONTAL_ONLY); //$NON-NLS-1$
    private TextArea m_mechViewRight = new TextArea(18,28);
    private Panel m_pLeft = new Panel();

    private Panel m_pUpper = new Panel();
    BufferedPanel m_pPreview = new BufferedPanel();
    
    private Label m_labelPlayer = new Label(Messages.getString("MechSelectorDialog.m_labelPlayer"), Label.RIGHT); //$NON-NLS-1$
    private Choice m_chPlayer = new Choice();

    private boolean includeMaxTech;

    public MechSelectorDialog(ClientGUI cl, UnitLoadingDialog uld)
    {
        super(cl.frame, Messages.getString("MechSelectorDialog.title"), true); //$NON-NLS-1$
        m_client = cl.getClient();
        m_clientgui = cl;
        unitLoadingDialog = uld;

        for (int x = 0; x < m_saSorts.length; x++) {
            m_chSort.addItem(m_saSorts[x]);
        }
        updatePlayerChoice();
        
        m_pParams.setLayout(new GridLayout(4, 2));
        m_pParams.add(m_labelWeightClass);
        m_pParams.add(m_chWeightClass);
        m_pParams.add(m_labelType);
        m_pParams.add(m_chType);
        m_pParams.add(m_labelUnitType);
        m_pParams.add(m_chUnitType);
        m_pParams.add(m_labelSort);
        m_pParams.add(m_chSort);

        m_pListOptions.add(m_labelListOptions);
        m_cModel.addItemListener(this);
        m_pListOptions.add(m_cModel);
        m_cName.addItemListener(this);
        m_pListOptions.add(m_cName);
        m_cTons.addItemListener(this);
        m_pListOptions.add(m_cTons);
        m_cBV.addItemListener(this);
        m_pListOptions.add(m_cBV);
        m_cYear.addItemListener(this);
        m_pListOptions.add(m_cYear);
        m_cLevel.addItemListener(this);
        m_pListOptions.add(m_cLevel);
        m_cCost.addItemListener(this);
        m_pListOptions.add(m_cCost);

        m_pButtons.setLayout(new FlowLayout(FlowLayout.CENTER));
        m_pButtons.add(m_bPick);
        m_pButtons.add(m_bPickClose);
        m_pButtons.add(m_bCancel);
        m_pButtons.add(m_labelPlayer);
        m_pButtons.add(m_chPlayer);
        
        m_pUpper.setLayout(new BorderLayout());
        m_pPreview.setPreferredSize(84, 72);
        m_pUpper.add(m_pParams, BorderLayout.WEST);
        m_pUpper.add(m_pPreview, BorderLayout.CENTER);
        m_pUpper.add(m_pListOptions, BorderLayout.SOUTH);

        m_pLeft.setLayout(new BorderLayout());
        m_pLeft.add(m_pUpper, BorderLayout.NORTH);
        m_mechList.setFont(new Font("Monospaced", Font.PLAIN, 12)); //$NON-NLS-1$
        m_mechList.addKeyListener(this);
        m_pLeft.add(m_mechList, BorderLayout.CENTER);
        m_pLeft.add(m_pButtons, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(m_pLeft, BorderLayout.WEST);
        m_mechViewLeft.setFont(new Font("Monospaced", Font.PLAIN, 12)); //$NON-NLS-1$
        add(m_mechViewLeft, BorderLayout.CENTER);
        m_mechViewRight.setFont(new Font("Monospaced", Font.PLAIN, 12)); //$NON-NLS-1$
        add(m_mechViewRight, BorderLayout.EAST);
        
        //clearMechPreview();
        
        m_chWeightClass.addItemListener(this);
        m_chType.addItemListener(this);
        m_chUnitType.addItemListener(this);
        m_chSort.addItemListener(this);
        m_mechList.addItemListener(this);
        m_bPick.addActionListener(this);
        m_bPickClose.addActionListener(this);
        m_bCancel.addActionListener(this);
        setSize(770, 350);
        setLocation(computeDesiredLocation());
        populateChoices();
        addWindowListener(this);
        updateWidgetEnablements();
    }

    private void updateTechChoice() {
        boolean maxTechOption = m_client.game.getOptions().booleanOption("allow_level_3_units");
        int maxTech = (maxTechOption ? TechConstants.SIZE : TechConstants.SIZE_LEVEL_2);
        if (includeMaxTech == maxTechOption) {
            return;
        }
        includeMaxTech = maxTechOption;
        m_chType.removeAll();
        for (int i=0; i<maxTech; i++) {
            m_chType.addItem(TechConstants.getLevelDisplayableName(i));
        }        
    }

    private void updatePlayerChoice() {
        String lastChoice = m_chPlayer.getSelectedItem();
        m_chPlayer.removeAll();
        m_chPlayer.setEnabled(true);
        m_chPlayer.addItem(m_clientgui.getClient().getName());
        for (Iterator i = m_clientgui.getBots().values().iterator(); i.hasNext();) {
            m_chPlayer.addItem(((Client)i.next()).getName());
        }
        if (m_chPlayer.getItemCount() == 1) {
            m_chPlayer.setEnabled(false);
        } else {
            m_chPlayer.select(lastChoice);
        }
    }

    public void run() {
        // Loading mechs can take a while, so it will have its own thread.
        // This prevents the UI from freezing, and allows the
        // "Please wait..." dialog to behave properly on various Java VMs.
        filterMechs();
        m_mechList.invalidate();  // force re-layout of window
        pack();
        setLocation(computeDesiredLocation());

        unitLoadingDialog.hide();

        final Hashtable hFailedFiles = MechSummaryCache.getInstance().getFailedFiles();
        if (hFailedFiles != null && hFailedFiles.size() > 0) {
            new UnitFailureDialog(m_clientgui.frame, hFailedFiles); // self-showing dialog
        }
    }
    
    private void populateChoices() {
        
        for (int i=0; i<EntityWeightClass.SIZE; i++) {
            m_chWeightClass.addItem(EntityWeightClass.getClassName(i));
        }
        m_chWeightClass.addItem(Messages.getString("MechSelectorDialog.All")); //$NON-NLS-1$
        m_chWeightClass.select(0);

        includeMaxTech = m_client.game.getOptions().booleanOption("allow_level_3_units");
        int maxTech = (includeMaxTech ? TechConstants.SIZE : TechConstants.SIZE_LEVEL_2);
        for (int i=0; i<maxTech; i++) {
            m_chType.addItem(TechConstants.getLevelDisplayableName(i));
        }
        // m_chType.addItem(Messages.getString("MechSelectorDialog.ISAll")); //$NON-NLS-1$
        // m_chType.addItem(Messages.getString("MechSelectorDialog.ISAndClan")); //$NON-NLS-1$
        // More than 8 items causes the drop down to sprout a vertical
        //  scroll bar.  I guess we'll sacrifice this next one to stay
        //  under the limit.  Stupid AWT Choice class!
        // m_chType.addItem("Mixed All");
        // m_chType.addItem(Messages.getString("MechSelectorDialog.All")); //$NON-NLS-1$
        m_chType.select(0);


        for (int i=0; i<UnitType.SIZE; i++) {
            m_chUnitType.addItem(UnitType.getTypeDisplayableName(i));
        }
        m_chUnitType.addItem(Messages.getString("MechSelectorDialog.All")); //$NON-NLS-1$
        m_chUnitType.select(0);
    }
    
    
    private void filterMechs() {
        Vector vMechs = new Vector();
        int nClass = m_chWeightClass.getSelectedIndex();
        int nType = m_chType.getSelectedIndex();
        String sType = m_chType.getSelectedItem();
        int nUnitType = m_chUnitType.getSelectedIndex();
        MechSummary[] mechs = MechSummaryCache.getInstance().getAllMechs();
        if ( mechs == null ) {
            System.err.println( "No units to filter!" ); //$NON-NLS-1$
            return;
        }
        for (int x = 0; x < mechs.length; x++) {
            if ( /* Weight */
                (nClass == EntityWeightClass.SIZE || mechs[x].getWeightClass() == nClass)
                && /* Technology Level */
                ((nType == TechConstants.T_ALL)
                    || (nType == mechs[x].getType())
                    || ((nType == TechConstants.T_LEVEL_2_ALL)
                        && ((mechs[x].getType() == TechConstants.T_IS_LEVEL_1)
                        || (mechs[x].getType() == TechConstants.T_IS_LEVEL_2)
                        || (mechs[x].getType() == TechConstants.T_CLAN_LEVEL_2)))
                    || ((nType == TechConstants.T_IS_LEVEL_2_ALL)
                        && ((mechs[x].getType() == TechConstants.T_IS_LEVEL_1)
                        || (mechs[x].getType() == TechConstants.T_IS_LEVEL_2))))
                && /* Unit Type (Mek, Infantry, etc.) */
                ( nUnitType == UnitType.SIZE ||
                  mechs[x].getUnitType().equals(UnitType.getTypeName(nUnitType)))
                && /*canon required*/ (!m_client.game.getOptions().booleanOption("canon_only") || mechs[x].isCanon())) {
                    vMechs.addElement(mechs[x]);
                }
        }
        m_mechsCurrent = new MechSummary[vMechs.size()];
        vMechs.copyInto(m_mechsCurrent);
        sortMechs();
    }
    
    private void sortMechs() {
        Arrays.sort(m_mechsCurrent, new MechSummaryComparator(m_chSort.getSelectedIndex()));
        m_mechList.removeAll();
        try {
            m_mechList.setEnabled(false);
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            for (int x = 0; x < m_mechsCurrent.length; x++) {
                m_mechList.add(formatMech(m_mechsCurrent[x]));
            }
        }
        finally {
            this.setCursor(Cursor.getDefaultCursor());
            m_mechList.setEnabled(true);
        }
        updateWidgetEnablements();
        repaint();
    }
    
    private void searchFor(String search) {
        for (int i = 0; i < m_mechsCurrent.length; i++) {
            if (m_mechsCurrent[i].getName().toLowerCase().startsWith(search)) {
                m_mechList.select(i);
                ItemEvent event = new ItemEvent(m_mechList,ItemEvent.ITEM_STATE_CHANGED,m_mechList,ItemEvent.SELECTED);
                itemStateChanged(event);
                break;
            }
        }
    }

    private Point computeDesiredLocation() {
        int desiredX = m_clientgui.frame.getLocation().x + m_clientgui.frame.getSize().width/2 - getSize().width/2;
        if (desiredX < 0)
            desiredX = 0;
        int desiredY = m_clientgui.frame.getLocation().y + m_clientgui.frame.getSize().height/2 - getSize().height/2;
        if (desiredY < 0)
            desiredY = 0;
        return new Point(desiredX, desiredY);
    }

    public void show() {
        updatePlayerChoice();
        updateTechChoice();
        setLocation(computeDesiredLocation());
        super.show();
    }

    private String formatMech(MechSummary ms) {
        String val = "";
        String levelOrValid;

        if (!ms.getLevel().equals("F")) {
            levelOrValid = TechConstants.T_SIMPLE_LEVEL[ms.getType()];
        } else {
            levelOrValid = "F";
        }
        if (GUIPreferences.getInstance().getMechSelectorIncludeModel())
            val += makeLength(ms.getModel(), 10) + " "; //$NON-NLS-1$ //$NON-NLS-2$
        if (GUIPreferences.getInstance().getMechSelectorIncludeName())
            val += makeLength(ms.getChassis(), 20) + " "; //$NON-NLS-1$ //$NON-NLS-2$
        if (GUIPreferences.getInstance().getMechSelectorIncludeTons())
            val += makeLength("" + ms.getTons(), 3) + " "; //$NON-NLS-1$ //$NON-NLS-2$
        if (GUIPreferences.getInstance().getMechSelectorIncludeBV())
            val += makeLength("" + ms.getBV(),5) + " "; //$NON-NLS-1$ //$NON-NLS-2$
        if (GUIPreferences.getInstance().getMechSelectorIncludeYear())
            val += ms.getYear() + " ";
        if (GUIPreferences.getInstance().getMechSelectorIncludeLevel())
            val += levelOrValid + " ";
        if (GUIPreferences.getInstance().getMechSelectorIncludeCost())
            val += ms.getCost() + " ";
        return val;
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == m_bCancel) {
            this.setVisible(false);
        }
        else if (ae.getSource() == m_bPick ||
                 ae.getSource() == m_bPickClose) {
            int x = m_mechList.getSelectedIndex();
            if (x == -1) {
                return;
            }
            MechSummary ms = m_mechsCurrent[m_mechList.getSelectedIndex()];
            try {
                Entity e = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
                Client c = null;
                if (m_chPlayer.getSelectedIndex() > 0) {
                    String name = m_chPlayer.getSelectedItem();
                    c = (Client)m_clientgui.getBots().get(name);
                }
                if (c == null) {
                    c = m_client;
                }
                e.setOwner(c.getLocalPlayer());
                c.sendAddEntity(e);
            } catch (EntityLoadingException ex) {
                System.out.println("Unable to load mech: " + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + ex.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                ex.printStackTrace();
                return;
            }
            if (ae.getSource() == m_bPickClose) {
                this.setVisible(false);
            }
        }
    }
    
    public void itemStateChanged(ItemEvent ie) {
        if (ie.getSource() == m_chSort) {
            clearMechPreview();
            sortMechs();
        }
        else if (ie.getSource() == m_chWeightClass
                 || ie.getSource() == m_chType
                 || ie.getSource() == m_chUnitType) {
            clearMechPreview();
            filterMechs();
        } else if (ie.getSource() == m_mechList) {
            updateWidgetEnablements();
            int selected = m_mechList.getSelectedIndex();
            if (selected == -1) {
                clearMechPreview();
                return;
            }
            else {
                MechSummary ms = m_mechsCurrent[selected];
                try {
                    Entity entity = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
                    previewMech(entity);
                } catch (EntityLoadingException ex) {
                    System.out.println("Unable to load mech: " + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + ex.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    ex.printStackTrace();
                    clearMechPreview();
                    return;
                }
            }
        } else if (ie.getSource() == m_cModel ||
                   ie.getSource() == m_cName ||
                   ie.getSource() == m_cTons ||
                   ie.getSource() == m_cBV ||
                   ie.getSource() == m_cYear ||
                   ie.getSource() == m_cLevel ||
                   ie.getSource() == m_cCost) {
            GUIPreferences.getInstance().setMechSelectorIncludeModel(m_cModel.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeName(m_cName.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeTons(m_cTons.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeBV(m_cBV.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeYear(m_cYear.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeLevel(m_cLevel.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeCost(m_cCost.getState());
            clearMechPreview();
            sortMechs(); // sorting has side-effect of repopulating list
            m_mechList.invalidate();  // force re-layout of window
            pack();
            setLocation(computeDesiredLocation());
        }
    }
    
    void clearMechPreview() {
        m_mechViewLeft.setEditable(false);
        m_mechViewRight.setEditable(false);
        m_mechViewLeft.setText(""); //$NON-NLS-1$
        m_mechViewRight.setText(""); //$NON-NLS-1$

        // Remove preview image.        
        if (MechSummaryCache.getInstance().isInitialized()) {
            m_pPreview.removeBgDrawers();
            m_pPreview.paint(m_pPreview.getGraphics());
        }
    }
    
    void previewMech(Entity entity) {
        MechView mechView = new MechView(entity);
        m_mechViewLeft.setEditable(false);
        m_mechViewRight.setEditable(false);
        m_mechViewLeft.setText(mechView.getMechReadoutBasic());
        m_mechViewRight.setText(mechView.getMechReadoutLoadout());
        m_mechViewLeft.setCaretPosition(0);
        m_mechViewRight.setCaretPosition(0);

        // Preview image of the unit...
        m_clientgui.loadPreviewImage(m_pPreview, entity, m_client.getLocalPlayer());
        m_pPreview.paint(m_pPreview.getGraphics());
    }

    private static final String SPACES = "                        "; //$NON-NLS-1$
    private String makeLength(String s, int nLength) {
        if (s.length() == nLength) {
            return s;
        }
        else if (s.length() > nLength) {
            return s.substring(0, nLength - 2) + ".."; //$NON-NLS-1$
        }
        else {
            return s + SPACES.substring(0, nLength - s.length());
        }
    }
        
    public void keyReleased(java.awt.event.KeyEvent ke) {
    }
    
    public void keyPressed(java.awt.event.KeyEvent ke) {
    if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
        ActionEvent event = new ActionEvent(m_bPick,ActionEvent.ACTION_PERFORMED,""); //$NON-NLS-1$
        actionPerformed(event);
    }
        long curTime = System.currentTimeMillis();
        if (curTime - m_nLastSearch > KEY_TIMEOUT) {
            m_sbSearch = new StringBuffer();
        }
        m_nLastSearch = curTime;
        m_sbSearch.append(ke.getKeyChar());
        searchFor(m_sbSearch.toString().toLowerCase());
    }
    
    public void keyTyped(java.awt.event.KeyEvent ke) {
    }
        
    //
    // WindowListener
    //
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {
    }    
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {
    }    
    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        this.setVisible(false);
    }    
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {
    }    
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {
    }    
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {
    }    
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {
    }

    private void updateWidgetEnablements() {
        final boolean enable = m_mechList.getSelectedIndex() != -1;
        m_bPick.setEnabled(enable);
        m_bPickClose.setEnabled(enable);
    }
}
