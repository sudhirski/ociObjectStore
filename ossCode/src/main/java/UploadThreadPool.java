import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executor Service wrapper for the uploader.
 * Will create number of thread equal to the number of cores on the host on which this is running.
 */
public class UploadThreadPool {

    private final int numCores;

    private final ExecutorService service;

    UploadThreadPool() {
        numCores = Runtime.getRuntime().availableProcessors();

        service =
                Executors.newFixedThreadPool(
                numCores,
                new ThreadFactoryBuilder()
                        .setNameFormat("OCICToOCI-Uploader-" + System.currentTimeMillis() + "-%d")
                        .setDaemon(true)
                        .build());

        //service = Executors.newWorkStealingPool();
    }

    ExecutorService getExecutor() {
        return service;
    }

    void shutdown() {
        service.shutdownNow();
    }
}
