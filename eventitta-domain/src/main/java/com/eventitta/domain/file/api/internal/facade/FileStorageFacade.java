package com.eventitta.domain.file.api.internal.facade;

import com.eventitta.domain.file.api.internal.command.UploadFileCommand;
import com.eventitta.domain.file.api.internal.view.StoredFileView;
import com.eventitta.domain.media.domain.MediaStorageProvider;

public interface FileStorageFacade {

    String store(UploadFileCommand file, String keyPrefix);

    String storeBytes(byte[] content, String keyPrefix, String contentType, String extension);

    byte[] readBytes(String key);

    StoredFileView load(String filename);

    void delete(String fileUrl);

    String buildPublicUrl(String key);

    String normalizeKey(String keyOrUrl);

    MediaStorageProvider getStorageProvider();
}
