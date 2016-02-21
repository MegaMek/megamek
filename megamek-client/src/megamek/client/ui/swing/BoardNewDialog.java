package megamek.client.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;

/**
 * a quick class for the new map dialogue box
 */
public class BoardNewDialog extends JDialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -6109373881940834702L;
    private int xvalue;
    private int yvalue;
    private JLabel labWidth;
    private JLabel labHeight;
    private JTextField texWidth;
    private JTextField texHeight;
    private JButton butOkay;
    private JButton butCancel;

    BoardNewDialog(JFrame frame) {
        super(frame, Messages.getString("BoardEditor.SetDimentions"), true); //$NON-NLS-1$
        xvalue = 0;
        yvalue = 0;
        labWidth = new JLabel(
                Messages.getString("BoardEditor.labWidth"), SwingConstants.RIGHT); //$NON-NLS-1$
        labHeight = new JLabel(
                Messages.getString("BoardEditor.labHeight"), SwingConstants.RIGHT); //$NON-NLS-1$
        texWidth = new JTextField("16", 2); //$NON-NLS-1$
        texHeight = new JTextField("17", 2); //$NON-NLS-1$
        butOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
        butOkay.setActionCommand("done"); //$NON-NLS-1$
        butOkay.addActionListener(this);
        butOkay.setSize(80, 24);
        butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
        butCancel.setActionCommand("cancel"); //$NON-NLS-1$
        butCancel.addActionListener(this);
        butCancel.setSize(80, 24);
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        getContentPane().setLayout(gridbag);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 1, 1);
        gridbag.setConstraints(labWidth, c);
        getContentPane().add(labWidth);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(texWidth, c);
        getContentPane().add(texWidth);
        c.gridwidth = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(labHeight, c);
        getContentPane().add(labHeight);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(texHeight, c);
        getContentPane().add(texHeight);
        c.ipadx = 20;
        c.ipady = 5;
        c.gridwidth = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(butOkay, c);
        getContentPane().add(butOkay);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butCancel, c);
        getContentPane().add(butCancel);
        pack();
        setResizable(false);
        setLocation(frame.getLocation().x + frame.getSize().width / 2
                - getSize().width / 2, frame.getLocation().y
                + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(butOkay)) {
            try {
                xvalue = Integer.decode(texWidth.getText()).intValue();
                yvalue = Integer.decode(texHeight.getText()).intValue();
            } catch (NumberFormatException ex) {
                System.err.println(ex.getMessage());
            }
            setVisible(false);
        } else if (e.getSource().equals(butCancel)) {
            setVisible(false);
        }
    }

    @Override
    public int getX() {
        return xvalue;
    }

    @Override
    public int getY() {
        return yvalue;
    }
}
