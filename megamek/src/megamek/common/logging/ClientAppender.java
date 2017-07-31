package megamek.common.logging;

import org.apache.log4j.Layout;
import org.apache.log4j.RollingFileAppender;

import java.io.IOException;

/**
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 * @version %Id%
 * @since 7/31/2017 9:46 AM
 */
public class ClientAppender extends RollingFileAppender {
    private static final long maximumFileSize = 10485760L; // 10mb

    public ClientAppender(final Layout layout, final String fileName) throws IOException {
        this(layout, fileName, true);
    }

    public ClientAppender(final Layout layout, final String fileName, final boolean append) throws IOException {
        super(layout, fileName, append);
        super.setMaxBackupIndex(4);
        super.setMaximumFileSize(maximumFileSize);
    }
}
