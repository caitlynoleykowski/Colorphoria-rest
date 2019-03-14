package com.oo0o0o.colorphoria;

import com.google.cloud.vision.v1.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by caitoo0o on 10/26/2017.
 */
public class ImageUtils {
    /**
     * Detects whether the specified remote image has features you would want to moderate.
     *
     * @param gcsPath The path to the remote file to detect safe-search on.
     * @throws Exception on errors while closing the client.
     * @throws IOException on Input/Output errors.
     */
    public static boolean detectSafeSearchGcs(String gcsPath) {
        Logger logger = Logger.getLogger(ImageUtils.class.getName());

        List<AnnotateImageRequest> requests = new ArrayList<>();

        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.SAFE_SEARCH_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        try {
            try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
                List<AnnotateImageResponse> responses = response.getResponsesList();

                for (AnnotateImageResponse res : responses) {
                    if (res.hasError()) {
                        // TODO: Handle this error
                        logger.severe("Error: " + res.getError().getMessage());
                        return false;
                    }

                    // For full list of available annotations, see http://g.co/cloud/vision/docs
                    SafeSearchAnnotation annotation = res.getSafeSearchAnnotation();
                    logger.info(
                            "adult: " + annotation.getAdult() +
                                    " medical: " + annotation.getMedical() +
                                    " spoofed: " + annotation.getSpoof() +
                                    " violence: " + annotation.getViolence());

                    Likelihood adultLikelihood = annotation.getAdult();
                    Likelihood medicalLikelihood = annotation.getMedical();
                    Likelihood violenceLikelihood = annotation.getViolence();

                    if (adultLikelihood == Likelihood.VERY_LIKELY ||
                            medicalLikelihood == Likelihood.VERY_LIKELY ||
                            violenceLikelihood == Likelihood.VERY_LIKELY) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // TODO: Handle this error
            logger.log(Level.SEVERE, "Error: " + e.getMessage(), e);
        }
        return false;
    }
}
