/**
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 */

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.transfer.UploadConfiguration;
import com.oracle.bmc.objectstorage.transfer.UploadManager;
import com.oracle.bmc.objectstorage.transfer.UploadManager.UploadRequest;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * @author Sudhir Kumar Srinivasan.
 *
 * Utility to upload an object to OCI Object store using parallelism.
 */
public class UploadParallel {

    private final UploadOCICredsConfig ociCredsConfig;
    private final UploadOCIConfig ociConfig;

    private final Object lock = new Object();
    private boolean uploadPending;

    // Optional for now
    private UploadThreadPool threadPool;

    private UploadManager uploadManager;
    private ObjectStorage client;

    UploadParallel(UploadOCICredsConfig ociCredsConfig, UploadOCIConfig ociConfig) throws IOException {

        this.ociCredsConfig = ociCredsConfig;
        this.ociConfig = ociConfig;

        initUploader();
    }

    UploadParallel() throws IOException {
        this(null, null);
    }

    void initUploader() throws IOException {

        /**
         * Read the credentials from the OCI config file and setup the authenticator.
         */
        AuthenticationDetailsProvider provider =
                new ConfigFileAuthenticationDetailsProvider(
                        ociCredsConfig.getConfigurationFilePath(),
                        ociCredsConfig.getProfile());

        client = new ObjectStorageClient(provider);
        client.setRegion(((ConfigFileAuthenticationDetailsProvider) provider).getRegion()); // Is this required ??


        // Configure upload settings and init the manager.
        UploadConfiguration uploadConfiguration =
                UploadConfiguration.builder()
                        .allowMultipartUploads(true)
                        .allowParallelUploads(true)
                        .build();

        uploadManager = new UploadManager(client, uploadConfiguration);
    }


    Object getLock() {
        return lock;
    }

    boolean isUploadPending() {
        return uploadPending;
    }


    /**
     * Method to upload a file to OCI storage.
     *
     * @param filePath
     */
    void upload(Path filePath) throws FileNotFoundException {

        String fullPath = ociConfig.getDirectoryToMonitor() + "/" + filePath.toString();
        File f = new File(fullPath);
        InputStream is = getInputStream(f);
        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucketName(ociConfig.getBucketName())
                        .namespaceName(ociConfig.getNamespaceName())
                        //.objectName(ociConfig.getObjectName())
                        .objectName(filePath.toString())
                        .putObjectBody(is)
                        .contentLength(f.length())
                        .contentType(ociConfig.getContentType())
                        .build();

        UploadRequest uploadDetails =
                UploadRequest.builder(f)
                        //.parallelUploadExecutorService(threadPool.getExecutor())
                        .allowOverwrite(true).build(request);

        // if multi-part is used, and any part fails, the entire upload fails and will throw BmcException

        //System.out.println("Starting Upload..");
        long startTime = System.currentTimeMillis();
        synchronized (lock) {
            uploadPending = true;
            UploadManager.UploadResponse response;
            try {
                response = uploadManager.upload(uploadDetails);
                // fetch the object just uploaded
                GetObjectResponse getResponse =
                        client.getObject(
                                GetObjectRequest.builder()
                                        .namespaceName(ociConfig.getNamespaceName())
                                        .bucketName(ociConfig.getBucketName())
                                        .objectName(filePath.toString())
                                        .build());


                if (f.length() != getResponse.getContentLength()) {
                    System.out.println("Error: Upload attempt length: " + f.length()
                            + " Uploaded length " + getResponse.getContentLength());

                    // Retry, will overwrite existing incorrect entry.
                    upload(filePath);
                }
            } catch (BmcException be) {
                System.out.println("Exception : " + be.getMessage());
                return;
            }
            uploadPending = false;
            lock.notifyAll();
        }
        long endTime = System.currentTimeMillis();

        long timeElapsed = endTime - startTime;
        System.out.println("Time Taken: " + timeElapsed);


        if (threadPool != null) {
            threadPool.shutdown();
        }
    }

    /**
     * Returns the wrapped InputStream object with the configured READ_BUFFERSIZE.
     * @return
     * @throws FileNotFoundException
     */
    private InputStream getInputStream(File f) throws FileNotFoundException  {
        FileInputStream inputStream = new FileInputStream(f);
        return new BufferedInputStream(inputStream, UploadReader.READ_BUFFERSIZE);
    }
}