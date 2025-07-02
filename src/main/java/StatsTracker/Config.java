package StatsTracker;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

public class Config {
    public static final Logger logger = LogManager.getLogger(Config.class.getName());

    private static final String AUTO_SYNC_SETTINGS = "enable_auto_sync";
    private static final String ENDPOINT_SETTINGS = "endpoint";

    private final SpireConfig config;

    public final String userID;
    public final String token;

    public Config() throws IOException {
        Properties prop = new Properties();
        prop.setProperty(ENDPOINT_SETTINGS, "http://localhost:8090");
        prop.setProperty(AUTO_SYNC_SETTINGS, "false");

        config = new SpireConfig("stats-tracker", "stats-tracker-config", prop);
        config.load();

        logger.info("Loading slay-the-relics config");
        SpireConfig strConfig = new SpireConfig("slayTheRelics", "slayTheRelicsExporterConfig");
        userID = strConfig.getString("user");
        token = strConfig.getString("oauth");
    }

    public void setAutoSync(boolean autoSync) throws IOException {
        config.setString(AUTO_SYNC_SETTINGS, autoSync ? "true" : "false");
        config.save();
    }

    public boolean getAutoSync() {
        String autoSync = config.getString(AUTO_SYNC_SETTINGS);
        return autoSync.equals("true");
    }

    public String getEndpoint() {
        return config.getString(ENDPOINT_SETTINGS);
    }
}
