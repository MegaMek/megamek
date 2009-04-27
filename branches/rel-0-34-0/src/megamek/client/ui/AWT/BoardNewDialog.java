package megamek.client.ui.AWT;

import java.awt.Button;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import megamek.client.ui.Messages;

/**
 * a quick class for the new map diaglogue box
 */
public class BoardNewDialog extends Dialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -6562881140632503653L;

    public int xvalue, yvalue;

    protected Label labWidth, labHeight;
    protected TextField texWidth, texHeight;
    protected Button butOkay, butCancel;

    public BoardNewDialog(Frame frame, String[] hexList, int hexSelected) {
        super(frame, Messages.getString("BoardEditor.SetDimentions"), true); //$NON-NLS-1$

        xvalue = 0;
        yvalue = 0;

        labWidth = new Label(
                Messages.getString("BoardEditor.labWidth"), Label.RIGHT); //$NON-NLS-1$
        labHeight = new Label(
                Messages.getString("BoardEditor.labHeight"), Label.RIGHT); //$NON-NLS-1$

        texWidth = new TextField("16", 2); //$NON-NLS-1$
        texHeight = new TextField("17", 2); //$NON-NLS-1$

        butOkay = new Button(Messages.getString("Okay")); //$NON-NLS-1$
        butOkay.setActionCommand("done"); //$NON-NLS-1$
        butOkay.addActionListener(this);
        butOkay.setSize(80, 24);

        butCancel = new Button(Messages.getString("Cancel")); //$NON-NLS-1$
        butCancel.setActionCommand("cancel"); //$NON-NLS-1$
        butCancel.addActionListener(this);
        butCancel.setSize(80, 24);

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 1, 1);

        gridbag.setConstraints(labWidth, c);
        add(labWidth);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(texWidth, c);
        add(texWidth);

        c.gridwidth = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(labHeight, c);
        add(labHeight);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(texHeight, c);
        add(texHeight);

        c.ipadx = 20;
        c.ipady = 5;
        c.gridwidth = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(butOkay, c);
        add(butOkay);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butCancel, c);
        add(butCancel);

        pack();
        setResizable(false);
        setLocation(frame.getLocation().x + frame.getSize().width / 2
                - getSize().width / 2, frame.getLocation().y
                + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == butOkay) {
            try {
                xvalue = Integer.decode(texWidth.getText()).intValue();
                yvalue = Integer.decode(texHeight.getText()).intValue();
            } catch (NumberFormatException ex) {
                System.err.println(ex.getMessage());
            }
            setVisible(false);
        } else if (e.getSource() == butCancel) {
            setVisible(false);
        }
    }

    public int getX() {
        return xvalue;
    }

    public int getY() {
        return yvalue;
    }
}