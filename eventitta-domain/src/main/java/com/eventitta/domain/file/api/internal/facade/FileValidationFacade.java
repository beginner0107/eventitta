package com.eventitta.domain.file.api.internal.facade;

import com.eventitta.domain.file.api.internal.command.UploadFileCommand;
import com.eventitta.domain.file.api.internal.view.ValidatedMediaFile;
import com.eventitta.domain.media.domain.MediaCategory;

import java.util.List;

public interface FileValidationFacade {

    List<ValidatedMediaFile> validateFiles(MediaCategory category, List<UploadFileCommand> files);
}
