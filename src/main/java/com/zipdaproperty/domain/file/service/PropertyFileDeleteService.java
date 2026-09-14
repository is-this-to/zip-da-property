package com.zipdaproperty.domain.file.service;

import com.zipdaproperty.domain.file.constant.UploadStatus;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.error.custom.business.FileOwnershipRequiredException;
import com.zipdaproperty.global.error.custom.business.NotFoundResourceException;
import com.zipdaproperty.global.error.custom.business.UnauthenticatedException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyFileDeleteService {

    private static final String USER_DELETE_REASON =
            "파일 소유자의 요청으로 삭제되었습니다.";

    private final PropertyFileRepository propertyFileRepository;
    private final PropertyFileObjectDeletionPublisher deletionPublisher;

    @Transactional
    public void delete(
            Long fileId,
            ActorContext actorContext
    ) {
        validateActorContext(actorContext);

        PropertyFile propertyFile = propertyFileRepository
                .findByPropertyFileIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new NotFoundResourceException(
                        "파일을 찾을 수 없습니다."
                ));

        validateOwnership(propertyFile, actorContext);
        validateUnlinked(propertyFile);

        propertyFile.softDelete(
                actorContext,
                Instant.now(),
                USER_DELETE_REASON
        );
        deletionPublisher.publishAfterCommit(List.of(propertyFile));
    }

    private void validateActorContext(ActorContext actorContext) {
        if (actorContext == null || actorContext.memberId() == null) {
            throw new UnauthenticatedException(
                    "로그인이 필요한 요청입니다."
            );
        }
    }

    private void validateOwnership(
            PropertyFile propertyFile,
            ActorContext actorContext
    ) {
        if (!propertyFile.getOwnerMemberId().equals(actorContext.memberId())) {
            throw new FileOwnershipRequiredException(
                    "파일 소유자만 파일을 삭제할 수 있습니다."
            );
        }
    }

    private void validateUnlinked(PropertyFile propertyFile) {
        if (propertyFile.getUploadStatus() == UploadStatus.LINKED) {
            throw new BusinessException(
                    CustomResponseCode.INVALID_REQUEST,
                    "연결된 파일은 삭제할 수 없습니다."
            );
        }
    }
}
