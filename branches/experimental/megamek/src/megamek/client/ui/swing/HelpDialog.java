package megamek.client.ui.swing;

import megamek.common.logging.LogLevel;
import megamek.common.logging.Logger;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.BorderLayout;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 *
 * @version %Id%
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/4/13 9:20 PM
 */
public class HelpDialog extends JDialog {

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
                }catch (Exception ex) {
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
        new Logger().log(getClass(), methName, LogLevel.ERROR, t);

        if (quiet) return;
        JOptionPane.showMessageDialog(this, t.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
    }
}
