/**
 * @author Sudhir Kumar Srinivasan.
 *
 * Class to store the OCI related config.
 */
public class UploadOCIConfig {

    private String directoryToMonitor;

    private String namespaceName = "paasdevidcs";
    private String bucketName = "bucket1";
    private String objectName = "objP1";
    private String contentType = "application/octet-stream";

    UploadOCIConfig(String namespaceName, String bucketName, String directoryToMonitor) {
        this.namespaceName = namespaceName;
        this.bucketName = bucketName;
        this.directoryToMonitor = directoryToMonitor;
    }

    UploadOCIConfig() {
        this("paasdevidcs", "bucket1",  null);
    }

    String getDirectoryToMonitor() {
        return directoryToMonitor;
    }

    String getNamespaceName() {
        return namespaceName;
    }

    String getBucketName() {
        return bucketName;
    }

    String getObjectName() {
        return objectName;
    }

    String getContentType() {
        return contentType;
    }
}
