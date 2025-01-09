package megamek.client.ui.dialogs;

import megamek.client.ui.dialogs.helpDialogs.AbstractHelpDialog;
import megamek.common.internationalization.Internationalization;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class AutoResolveSimulationLogDialog extends AbstractHelpDialog {

    public AutoResolveSimulationLogDialog(final JFrame frame, File logFile) {
        super(frame, Internationalization.getText("AutoResolveSimulationLogDialog.title"),
            logFile.getAbsolutePath());

        setMinimumSize(new Dimension(800, 400));
        setModalExclusionType(ModalExclusionType.TOOLKIT_EXCLUDE);

    }

}
