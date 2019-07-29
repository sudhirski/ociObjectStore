import com.oracle.bmc.Region;

/**
 * @author Sudhir Kumar Srinivasan
 *
 * Class for storing OCI credentials.
 */
public class UploadOCICredsConfig {
    private String configurationFilePath;
    private String profile;

    private Region region = Region.US_ASHBURN_1;

    UploadOCICredsConfig(String configurationFilePath, String profile) {
        this.configurationFilePath = configurationFilePath;
        this.profile = profile;
    }

    UploadOCICredsConfig() {
        this("~/.oci/config", "DEFAULT");
    }

    public String getConfigurationFilePath() {
        return configurationFilePath;
    }

    public String getProfile() {
        return profile;
    }

    public Region getRegion() {
        return region;
    }
}
