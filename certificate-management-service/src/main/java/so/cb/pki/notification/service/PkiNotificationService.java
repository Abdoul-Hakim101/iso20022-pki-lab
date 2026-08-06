package so.cb.pki.notification.service;

import java.util.UUID;

public interface PkiNotificationService {

    void handleApprovedCsr(
            UUID csrId,
            UUID institutionId,
            String bic,
            String csrPem
    );
}