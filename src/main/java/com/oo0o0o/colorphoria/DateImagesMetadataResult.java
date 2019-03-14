package com.oo0o0o.colorphoria;

import java.util.List;

/**
 * Created by caitoo0o on 1/28/2019.
 */
public class DateImagesMetadataResult {
    public List<UploadedMediaEntity> getMediaEntities() {
        return mediaEntities;
    }

    public void setMediaEntities(List<UploadedMediaEntity> mediaEntities) {
        this.mediaEntities = mediaEntities;
    }

    List<UploadedMediaEntity> mediaEntities;
}
