package com.eventitta.domain.media.api.internal.facade;

import com.eventitta.domain.file.api.internal.command.UploadFileCommand;
import com.eventitta.domain.media.api.internal.view.MediaAttachmentView;
import com.eventitta.domain.media.api.internal.view.MediaDisplayView;
import com.eventitta.domain.media.api.internal.view.UploadedMediaView;
import com.eventitta.domain.media.domain.MediaCategory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MediaAssetInternalFacade {

    List<UploadedMediaView> upload(Long userId, MediaCategory category, List<UploadFileCommand> files);

    List<MediaAttachmentView> resolveAndAttachAssets(Long userId, MediaCategory category, List<Long> mediaIds);

    void releaseAssetsIfUnreferenced(Collection<Long> mediaIds);

    String resolveDisplayUrl(Long mediaId);

    Map<Long, MediaDisplayView> resolveDisplayViews(Collection<Long> mediaIds);
}
