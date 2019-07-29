
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;

import java.io.IOException;

public class UploadSequential {

    private final UploadOCICredsConfig ociCredsConfig;
    private final UploadOCIConfig ociConfig;
    private final UploadReader reader;

    public UploadOCICredsConfig getOciCredsConfig() {
        return ociCredsConfig;
    }

    UploadSequential() throws InterruptedException, IOException {

        ociCredsConfig = new UploadOCICredsConfig();
        ociConfig = new UploadOCIConfig();
        reader = new UploadReader();
    }


    public static void main(String[] args) throws Exception {

        UploadSequential uploader = new UploadSequential();

        UploadOCICredsConfig config = uploader.getOciCredsConfig();

        AuthenticationDetailsProvider provider =
                new ConfigFileAuthenticationDetailsProvider(config.getConfigurationFilePath(), config.getProfile());

        ObjectStorage client = new ObjectStorageClient(provider);
        client.setRegion(config.getRegion());

        uploader.createEntry(client);

        client.close();
    }

    void createEntry(ObjectStorage client) throws Exception {

        PutObjectRequest request = PutObjectRequest.builder()
                .bucketName(ociConfig.getBucketName())
                .namespaceName(ociConfig.getNamespaceName())
                .objectName(ociConfig.getObjectName())
                .contentType(ociConfig.getContentType())
                //.putObjectBody(reader.getInputStream())
                .build();

        System.out.println("Starting Upload");
        long startTime = System.currentTimeMillis();
        client.putObject(request);
        long endTime = System.currentTimeMillis();

        long timeElapsed = endTime - startTime;

        System.out.println("Completed, Time Taken: " +timeElapsed);
    }
}
