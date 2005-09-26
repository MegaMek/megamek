package megamek.server;
import java.util.Vector;
import megamek.common.Report;

public abstract class DynamicTerrainProcessor {
    protected Server server;
    
    DynamicTerrainProcessor(Server server) {
        this.server = server;
    }
    
    /**
     * Process terrain changes in the end phase
     * @return reports for the server to send out
     */
    abstract void DoEndPhaseChanges(Vector<Report> vPhaseReport);
}
