package com.mahghuuls.elusiveflora;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The mod's one logger, plus a once-only warning for problems that would otherwise repeat every
 * chunk or tick, such as an unknown biome name in config.
 */
public final class ElusiveFloraLog {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private ElusiveFloraLog() {
    }

    /**
     * Logs {@code message} at warn level the first time {@code key} is seen in this game session
     * and stays silent afterwards. The key names the situation, not the message text, so a
     * message that includes a changing value still warns once.
     */
    public static void warnOnce(String key, String message) {
        if (WARNED.add(key)) {
            LOGGER.warn(message);
        }
    }
}
