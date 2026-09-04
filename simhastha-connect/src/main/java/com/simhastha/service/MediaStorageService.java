package com.simhastha.service;

import java.io.File;

public interface MediaStorageService {
    UploadResult uploadBusinessPhoto(String businessId, File file) throws Exception;

    record UploadResult(String url, String publicId) {
    }
}
