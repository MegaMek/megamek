package megamek.client.bot.princess;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @version %Id%
 * @author: Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 9/6/13 6:50 PM
 */
public class BehaviorSettingsFactory {

    private static final String PRINCESS_BEHAVIOR_PATH = "mmconf" + File.separator + "princessBehaviors.xml";

    protected static Map<String, BehaviorSettings> behaviorMap = new HashMap<String, BehaviorSettings>();
    protected static final Object CACHE_LOCK = new Object();

    protected BehaviorSettingsFactory() {
        init(false);
    }

    /**
     * Initializes the {@link BehaviorSettings} cache.  If the cache is empty, it will load from
     * mmconf/princessBehaviors.xml.  Also, if the "-- default --" behavior is missing, it will be added.
     *
     * @param reinitialize Set TRUE to force the cache to be completely rebuilt.
     */
    public static void init(boolean reinitialize) {
        synchronized (CACHE_LOCK) {
            if (behaviorMap == null || reinitialize) {
                behaviorMap = new HashMap<String, BehaviorSettings>();
            }
            if (behaviorMap.isEmpty()) {
                loadBehaviorSettings(buildPrincessBehaviorDoc());
            }
            if (!behaviorMap.containsKey(BehaviorSettings.DEFAULT_DESC)) {
                addBehavior(createDefaultBehavior());
            }
        }
    }

    /**
     * Adds a {@link BehaviorSettings} to the cache.  If a behavior with the same name is already in the cache, it will
     * be overwritten.
     *
     * @param behaviorSettings The {@link BehaviorSettings} to be added to the cache.
     */
    public static void addBehavior(BehaviorSettings behaviorSettings) {
        synchronized (CACHE_LOCK) {
            behaviorMap.put(behaviorSettings.getDescription().trim(), behaviorSettings);
        }
    }

    /**
     * Returns the named {@link BehaviorSettings}.
     *
     * @param desc The name of the behavior; matched to {@link BehaviorSettings#getDescription()}.
     * @return The named behavior or NULL if no match is found.
     */
    public static BehaviorSettings getBehavior(String desc) {
        return behaviorMap.get(desc);
    }

    /**
     * @return a new {@link BehaviorSettings} object with default values.
     */
    public static BehaviorSettings createDefaultBehavior() {
        return new BehaviorSettings();
    }

    protected static Document buildPrincessBehaviorDoc() {
        try {
            File behaviorFile = new File(PRINCESS_BEHAVIOR_PATH);
            if (!behaviorFile.exists() || !behaviorFile.isFile()) {
                System.out.println("Could not load " + PRINCESS_BEHAVIOR_PATH);
                return null;
            }
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(behaviorFile));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads the contents of the mmconf/princessBehaviors.xml file into the cache.  If the "-- default --" behavior is
     * missing it will be automatically added.
     *
     * @return TRUE if the load completes successfully.
     */
    protected static synchronized boolean loadBehaviorSettings(Document princessBehaviorDoc) {
        synchronized (CACHE_LOCK) {
            try {
                if (princessBehaviorDoc == null && behaviorMap.isEmpty()) {
                    addBehavior(createDefaultBehavior());
                    return false;
                } else if (princessBehaviorDoc == null) {
                    return false;
                }
                Element root = princessBehaviorDoc.getDocumentElement();
                BehaviorSettings behaviorSettings;
                for (int i = 0; i < root.getChildNodes().getLength(); i++) {
                    Node child = root.getChildNodes().item(i);
                    if (!"behavior".equalsIgnoreCase(child.getNodeName())) {
                        continue;
                    }
                    behaviorSettings = new BehaviorSettings((Element) child);
                    addBehavior(behaviorSettings);
                }

                if (!behaviorMap.keySet().contains(BehaviorSettings.DEFAULT_DESC)) {
                    addBehavior(createDefaultBehavior());
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Saves the contents of the cache to the mmconf/princessBehaviors.xml file.
     *
     * @param includeTargets Set TRUE to include the contents of the Strategic Targets list.
     * @return TRUE if the save is successful.
     */
    public static boolean saveBehaviorSettings(boolean includeTargets) {
        init(false);

        try {
            File behaviorFile = new File(PRINCESS_BEHAVIOR_PATH);
            if (!behaviorFile.exists()) {
                if (!behaviorFile.createNewFile()) {
                    System.out.println("Could not create " + PRINCESS_BEHAVIOR_PATH);
                    return false;
                }
            }
            if (!behaviorFile.canWrite()) {
                System.out.println("Could not write to " + PRINCESS_BEHAVIOR_PATH);
                return false;
            }

            Document behaviorDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Node rootNode = behaviorDoc.createElement("princessBehaviors");
            synchronized (CACHE_LOCK) {
                for (String key : behaviorMap.keySet()) {
                    BehaviorSettings settings = behaviorMap.get(key);
                    rootNode.appendChild(settings.toXml(behaviorDoc, includeTargets));
                }
            }
            behaviorDoc.appendChild(rootNode);

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            DOMSource source = new DOMSource(behaviorDoc);
            StreamResult result = new StreamResult(new FileWriter(behaviorFile));
            transformer.transform(source, result);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @return an array of the names of all the {@link BehaviorSettings} in the cache.
     */
    public static String[] getBehaviorNames() {
        init(false);
        List<String> names;
        synchronized (CACHE_LOCK) {
            names = new ArrayList<String>(behaviorMap.keySet());
        }
        Collections.sort(names);
        return names.toArray(new String[names.size()]);
    }
}
