package com.zipdaproperty.domain.file.event;

import com.zipdaproperty.domain.file.service.PropertyFileObjectDeletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PropertyFileObjectDeletionListener {

    private final PropertyFileObjectDeletionService deletionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deleteObjects(PropertyFileObjectDeletionRequested event) {
        for (Long propertyFileId : event.propertyFileIds()) {
            try {
                deletionService.deleteObject(propertyFileId);
            } catch (RuntimeException exception) {
                log.error(
                        "스토리지 객체 삭제 실패. propertyFileId={}",
                        propertyFileId,
                        exception
                );
            }
        }
    }
}
