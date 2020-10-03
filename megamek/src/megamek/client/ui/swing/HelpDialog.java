/*  
* MegaMek - Copyright (C) 2013-2020 - The MegaMek Team  
*  
* This program is free software; you can redistribute it and/or modify it under  
* the terms of the GNU General Public License as published by the Free Software  
* Foundation; either version 2 of the License, or (at your option) any later  
* version.  
*  
* This program is distributed in the hope that it will be useful, but WITHOUT  
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
* details.  
*/  
package megamek.client.ui.swing;

import megamek.MegaMek;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.BorderLayout;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 *
 * @version $Id$
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/4/13 9:20 PM
 */
public class HelpDialog extends JDialog {

    private static final long serialVersionUID = 1442198850518387690L;
    
    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;

    private URL helpUrl;
    private JEditorPane mainView;

    public HelpDialog(String title, URL helpURL) {
        setTitle(title);
        getContentPane().setLayout(new BorderLayout());
        this.helpUrl = helpURL;

        mainView = new JEditorPane();
        mainView.setEditable(false);
        try {
            mainView.setPage(helpUrl);
        } catch (Exception e) {
            handleError("HelpDialog(String, URL)", e, false);
        }

        //Listen for the user clicking on hyperlinks.
        mainView.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                try {
                    if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                        mainView.setPage(e.getURL());
                    }
                } catch (Exception ex) {
                    handleError("hyperlinkUpdate(HyperlinkEvent)", ex, false);
                }
            }
        });

        getContentPane().add(new JScrollPane(mainView));
        setModalExclusionType(ModalExclusionType.TOOLKIT_EXCLUDE);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
    }

    private void handleError(String methName, Throwable t, boolean quiet) {
        MegaMek.getLogger().error(t);

        if (quiet) return;
        JOptionPane.showMessageDialog(this, t.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
    }
}
