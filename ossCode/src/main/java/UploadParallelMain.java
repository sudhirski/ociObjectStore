import java.io.IOException;

public class UploadParallelMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        String directoryToMonitor = "", bucketName = "", namespace = "";
        for (int i = 0; i < args.length;) {
            switch(args[i]) {

                case "--db-dir": {
                    directoryToMonitor = args[i + 1];
                    i = i + 2;
                    break;
                }

                case "--bucket-name": {
                    bucketName = args[i + 1];
                    i = i + 2;
                    break;
                }

                case "--namespace": {
                    namespace = args[i + 1];
                    i = i + 2;
                    break;
                }

                default:
                    break;
            }
        }

        UploadOCIConfig ociConfig = new UploadOCIConfig(namespace, bucketName, directoryToMonitor);
        UploadOCICredsConfig ociCredsConfig = new UploadOCICredsConfig();

        new UploadReader(
                ociConfig,
                new UploadParallel(ociCredsConfig, ociConfig)
        ).watchDirectory();
    }
}
