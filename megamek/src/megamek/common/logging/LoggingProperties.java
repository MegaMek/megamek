package megamek.common.logging;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 * @version %Id%
 * @since 7/31/2017 9:58 AM
 */
public class LoggingProperties {

    public static final LoggingProperties instance = new LoggingProperties();

    private ThreadLocal<Map<String, Object>> propertyMap = new ThreadLocal<Map<String, Object>>() {
        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<>();
        }
    };

    private LoggingProperties() {
    }

    public static LoggingProperties getInstance() {
        return instance;
    }

    public void putProperty(final String property, final Object value) {
        propertyMap.get().put(property, value);
    }

    public Object getProperty(final String property) {
        Map<String, Object> threadedMap = propertyMap.get();
        Object value = threadedMap.get(property);
        return (null == value ? "" : value);
    }

    public void remove() {
        instance.propertyMap.remove();
    }

}
