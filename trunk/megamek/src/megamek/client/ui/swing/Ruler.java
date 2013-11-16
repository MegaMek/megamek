/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.IBoardView;
import megamek.client.ui.Messages;
import megamek.common.Coords;
import megamek.common.LosEffects;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
// import java.awt.Dimension; Import never used
// import java.awt.Insets; Import never used

/**
 * <p>
 * Title: Ruler
 * </p>
 * <p>
 * Description:
 * </p>
 * 
 * @author Ken Nguyen (kenn)
 * @version 1.0
 */
public class Ruler extends JDialog implements BoardViewListener {
    /**
     * 
     */
    private static final long serialVersionUID = -4820402626782115601L;
    public static Color color1 = Color.cyan;
    public static Color color2 = Color.magenta;

    private Coords start;
    private Coords end;
    private Color startColor;
    private Color endColor;
    private int distance;
    private Client client;
    private IBoardView bv;
    private boolean flip;

    private JPanel buttonPanel;
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JButton butFlip = new JButton();
    private JLabel jLabel1;
    private JTextField tf_start = new JTextField();
    private JLabel jLabel2;
    private JTextField tf_end = new JTextField();
    private JLabel jLabel3;
    private JTextField tf_distance = new JTextField();
    private JLabel jLabel4;
    private JTextField tf_los1 = new JTextField();
    private JLabel jLabel5;
    private JTextField tf_los2 = new JTextField();
    private JButton butClose = new JButton();
    private JLabel heightLabel1;
    private JTextField height1 = new JTextField();
    private JLabel heightLabel2;
    private JTextField height2 = new JTextField();
    
    private JCheckBox cboIsMech1 = 
        new JCheckBox(Messages.getString("Ruler.isMech"));
    private JCheckBox cboIsMech2 = 
        new JCheckBox(Messages.getString("Ruler.isMech"));

    public Ruler(JFrame f, Client c, IBoardView b) {
        super(f, Messages.getString("Ruler.title"), false); //$NON-NLS-1$
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);

        start = null;
        end = null;
        flip = true;
        startColor = color1;
        endColor = color2;

        bv = b;
        client = c;
        b.addBoardViewListener(this);

        try {
            jbInit();
            //getContentPane().add(panel1);
            pack();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void jbInit() {
        buttonPanel = new JPanel();
        butFlip.setText(Messages.getString("Ruler.flip")); //$NON-NLS-1$
        butFlip.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                butFlip_actionPerformed();
            }
        });
        getContentPane().setLayout(gridBagLayout1);
        jLabel1 = new JLabel(Messages.getString("Ruler.Start"), SwingConstants.RIGHT); //$NON-NLS-1$
        tf_start.setEditable(false);
        tf_start.setColumns(16);
        jLabel2 = new JLabel(Messages.getString("Ruler.End"), SwingConstants.RIGHT); //$NON-NLS-1$
        tf_end.setEditable(false);
        tf_end.setColumns(16);
        jLabel3 = new JLabel(Messages.getString("Ruler.Distance"), SwingConstants.RIGHT); //$NON-NLS-1$
        tf_distance.setEditable(false);
        tf_distance.setColumns(5);
        jLabel4 = new JLabel(Messages.getString("Ruler.POV") + ":", SwingConstants.RIGHT); //$NON-NLS-1$ //$NON-NLS-2$
        jLabel4.setForeground(startColor);
        tf_los1.setEditable(false);
        tf_los1.setColumns(30);
        jLabel5 = new JLabel(Messages.getString("Ruler.POV") + ":", SwingConstants.RIGHT); //$NON-NLS-1$
        jLabel5.setForeground(endColor);
        tf_los2.setEditable(false);
        tf_los2.setColumns(30);
        butClose.setText(Messages.getString("Ruler.Close")); //$NON-NLS-1$
        butClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                butClose_actionPerformed();
            }
        });
        heightLabel1 = new JLabel(Messages.getString("Ruler.Height1"), SwingConstants.RIGHT); //$NON-NLS-1$
        heightLabel1.setForeground(startColor);
        height1.setText("1"); //$NON-NLS-1$
        height1.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                height1_keyReleased();
            }
        });
        height1.setColumns(5);
        cboIsMech1.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e) {
                checkBoxSelectionChanged();
            }
            
        });
        
        heightLabel2 = new JLabel(Messages.getString("Ruler.Height2"), SwingConstants.RIGHT); //$NON-NLS-1$
        heightLabel2.setForeground(endColor);
        height2.setText("1"); //$NON-NLS-1$
        height2.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                height2_keyReleased();
            }
        });
        height2.setColumns(5);
        cboIsMech2.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e) {
                checkBoxSelectionChanged();
            }
            
        });
        
        //need to set all the minimum sizes to prevent jtextfield going to zero size
        //on dialog resize.setColumns(16);
        tf_start.setMinimumSize(tf_start.getPreferredSize());
        tf_end.setMinimumSize(tf_end.getPreferredSize());
        height1.setMinimumSize(height1.getPreferredSize());
        height2.setMinimumSize(height2.getPreferredSize());
        tf_distance.setMinimumSize(tf_distance.getPreferredSize());
        tf_los1.setMinimumSize(tf_los1.getPreferredSize());
        tf_los2.setMinimumSize(tf_los2.getPreferredSize());

        GridBagConstraints c = new GridBagConstraints();
        
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = 0;
        gridBagLayout1.setConstraints(heightLabel1, c);
        getContentPane().add(heightLabel1);  
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 1;
        gridBagLayout1.setConstraints(height1, c);
        getContentPane().add(height1);
        c.gridx = 2;
        gridBagLayout1.setConstraints(cboIsMech1, c);
        getContentPane().add(cboIsMech1);

        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.EAST;
        gridBagLayout1.setConstraints(heightLabel2, c);
        getContentPane().add(heightLabel2);    
        c.anchor = GridBagConstraints.WEST;   
        c.gridx = 1;
        gridBagLayout1.setConstraints(height2, c);
        getContentPane().add(height2);
        c.gridx = 2;
        gridBagLayout1.setConstraints(cboIsMech2, c);
        getContentPane().add(cboIsMech2);
        
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.EAST;
        gridBagLayout1.setConstraints(jLabel1, c);
        getContentPane().add(jLabel1); 
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 1;
        c.gridwidth = 2;
        gridBagLayout1.setConstraints(tf_start, c);
        c.gridwidth = 1;
        getContentPane().add(tf_start);

        c.gridx = 0;
        c.gridy = 3;
        c.anchor = GridBagConstraints.EAST;
        gridBagLayout1.setConstraints(jLabel2, c);
        getContentPane().add(jLabel2); 
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = 2;
        c.gridx = 1;        
        gridBagLayout1.setConstraints(tf_end, c);
        c.gridwidth = 1;
        getContentPane().add(tf_end);

        c.gridx = 0;
        c.gridy = 4;
        c.anchor = GridBagConstraints.EAST;
        gridBagLayout1.setConstraints(jLabel3, c);
        getContentPane().add(jLabel3); 
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 1;
        gridBagLayout1.setConstraints(tf_distance, c);
        getContentPane().add(tf_distance);

        c.gridx = 0;
        c.gridy = 5;
      //  c.weightx = 0.0;
        c.anchor = GridBagConstraints.EAST;
        gridBagLayout1.setConstraints(jLabel4, c);
        getContentPane().add(jLabel4); 
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 1;
       // c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        gridBagLayout1.setConstraints(tf_los1, c);
        c.gridwidth = 1;
        getContentPane().add(tf_los1);

        c.gridx = 0;
        c.gridy = 6;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        gridBagLayout1.setConstraints(jLabel5, c);
        getContentPane().add(jLabel5); 
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        gridBagLayout1.setConstraints(tf_los2, c);
        c.gridwidth = 1;
        getContentPane().add(tf_los2);

        buttonPanel.add(butFlip);
        buttonPanel.add(butClose);
        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        gridBagLayout1.setConstraints(buttonPanel, c);
        getContentPane().add(buttonPanel);

        validate();

        setVisible(false);
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            cancel();
        }
        super.processWindowEvent(e);
    }

    private void cancel() {
        dispose();
        butClose_actionPerformed();
    }

    private void clear() {
        start = null;
        end = null;
    }

    private void addPoint(Coords c) {
        if (start == null) {
            start = c;

        } else if (start.equals(c)) {
            clear();

            setVisible(false);
        } else {
            end = c;

            distance = start.distance(end);

            setText();
            setVisible(true);
        }
    }

    private void setText() {
        int h1 = 1, h2 = 1;
        try {
            h1 = Integer.parseInt(height1.getText());
        } catch (NumberFormatException e) {
            // leave at default value
        }

        try {
            h2 = Integer.parseInt(height2.getText());
        } catch (NumberFormatException e) {
            // leave at default value
        }

        String toHit1 = "", toHit2 = ""; //$NON-NLS-1$ //$NON-NLS-2$
        ToHitData thd;
        if (flip) {
            thd = LosEffects.calculateLos(client.getGame(),
                    buildAttackInfo(start, end, h1, h2,cboIsMech1.isSelected(),
                            cboIsMech2.isSelected())).losModifiers(client.getGame());
        } else {
            thd = LosEffects.calculateLos(client.getGame(),
                    buildAttackInfo(end, start, h2, h1,cboIsMech2.isSelected(),
                            cboIsMech1.isSelected())).losModifiers(client.getGame());
        }
        if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
            toHit1 = thd.getValue() + " = "; //$NON-NLS-1$
        }
        toHit1 += thd.getDesc();

        if (flip) {
            thd = LosEffects.calculateLos(client.getGame(),
                    buildAttackInfo(end, start, h2, h1,cboIsMech2.isSelected(),
                            cboIsMech1.isSelected())).losModifiers(client.getGame());
        } else {
            thd = LosEffects.calculateLos(client.getGame(),
                    buildAttackInfo(start, end, h1, h2,cboIsMech1.isSelected(),
                            cboIsMech2.isSelected())).losModifiers(client.getGame());
        }
        if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
            toHit2 = thd.getValue() + " = "; //$NON-NLS-1$
        }
        toHit2 += thd.getDesc();

        tf_start.setText(start.toString());
        tf_end.setText(end.toString());
        tf_distance.setText("" + distance); //$NON-NLS-1$
        tf_los1.setText(toHit1);
        // tf_los1.setCaretPosition(0);
        tf_los2.setText(toHit2);
        // tf_los2.setCaretPosition(0);
    }

    /**
     * Ignores determining if the attack is on land or under water.
     * 
     * @param c1
     * @param c2
     * @param h1
     * @param h2
     * @return
     */
    private LosEffects.AttackInfo buildAttackInfo(Coords c1, Coords c2, int h1,
            int h2, boolean attackerIsMech, boolean targetIsMech) {
        LosEffects.AttackInfo ai = new LosEffects.AttackInfo();
        ai.attackPos = c1;
        ai.targetPos = c2;
        ai.attackHeight = h1;
        ai.targetHeight = h2;
        ai.attackerIsMech = attackerIsMech;
        ai.targetIsMech = targetIsMech;
        ai.attackAbsHeight = client.getGame().getBoard().getHex(c1).floor() + h1;
        ai.targetAbsHeight = client.getGame().getBoard().getHex(c2).floor() + h2;
        return ai;
    }

    public void hexMoused(BoardViewEvent b) {
        if ((b.getModifiers() & InputEvent.ALT_MASK) != 0) {
            if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
                addPoint(b.getCoords());
            }
        }

        bv.drawRuler(start, end, startColor, endColor);
    }

    public void hexCursor(BoardViewEvent b) {
        //ignored
    }

    public void boardHexHighlighted(BoardViewEvent b) {
        //ignored
    }

    public void hexSelected(BoardViewEvent b) {
        //ignored
    }

    public void firstLOSHex(BoardViewEvent b) {
        //ignored
    }

    public void secondLOSHex(BoardViewEvent b, Coords c) {
        //ignored
    }

    void butFlip_actionPerformed() {
        flip = !flip;

        if (startColor.equals(color1)) {
            startColor = color2;
            endColor = color1;
        } else {
            startColor = color1;
            endColor = color2;
        }

        heightLabel1.setForeground(startColor);
        heightLabel2.setForeground(endColor);

        setText();
        setVisible(true);

        bv.drawRuler(start, end, startColor, endColor);
    }

    void butClose_actionPerformed() {
        clear();
        setVisible(false);

        bv.drawRuler(start, end, startColor, endColor);
    }

    void height1_keyReleased() {
        setText();
        setVisible(true);
    }

    void height2_keyReleased() {
        setText();
        setVisible(true);
    }
    
    void checkBoxSelectionChanged(){
        setText();
        setVisible(true);
    }

    public void finishedMovingUnits(BoardViewEvent b) {
        //ignored
    }

    public void unitSelected(BoardViewEvent b) {
        //ignored
    }
}
