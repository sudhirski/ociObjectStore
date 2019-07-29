import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/**
 * @author Sudhir Kumar Srinivasan
 *
 * Class to encapsulate reading files to be used by the uploader.
 */
public class UploadReader {

    private File file;
    private static String filePath = "/scratch/oci/files/binary1G.dat";

    private UploadOCIConfig ociConfig;
    private UploadParallel uploader;

    public static final int READ_BUFFERSIZE = 64 * 1024; // 64 KB

    UploadReader(UploadOCIConfig ociConfig, UploadParallel uploader) throws IOException, InterruptedException {
        this.ociConfig = ociConfig;
        this.uploader = uploader;

        file = new File(filePath); // Remove


        watchDirectory();
    }

    UploadReader() throws IOException, InterruptedException {
        this(new UploadOCIConfig(), new UploadParallel());
    }

    /**
     * Returns the raw file object.
     *
     * @return
     */
    public File getFile() {
        return file;
    }

    void watchDirectory() throws IOException, InterruptedException {
        WatchService watchService
                = FileSystems.getDefault().newWatchService();

        Path path = Paths.get(ociConfig.getDirectoryToMonitor());

        path.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE);

        while (true) {
            WatchKey key = watchService.take();
            if (key != null) {
                //Thread.sleep(5000); // Wait for the file to fill up in case of 'dd'. Not needed in real scenario
                for (WatchEvent<?> event : key.pollEvents()) {
                    Object lock = uploader.getLock();
                    Path p = (Path) event.context();
                    waitUntilFileGrowing(p.toFile());
                    synchronized (lock) {
                        while (uploader.isUploadPending()) {
                            System.out.println("Reader Waiting on " +p.toString());
                            lock.wait();
                        }
                        System.out.println("Reader Calling uploader for " +p.toString());
                        uploader.upload(p);
                    }
                }
                key.reset();
            }
        }
    }

    /**
     * Method to check if the file entry that was created just now has any pending bytes
     * coming in. If so, wait until all bytes have been copied into the file.
     *
     * @param f
     * @throws InterruptedException
     */
    void waitUntilFileGrowing(File f) throws InterruptedException {
        boolean isGrowing;
        Long initialWeight;
        Long finalWeight;

        do {
            initialWeight = f.length();
            Thread.sleep(500);
            finalWeight = f.length();
            isGrowing = initialWeight < finalWeight;

        } while(isGrowing);
    }
}
