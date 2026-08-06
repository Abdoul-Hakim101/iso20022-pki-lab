package so.cb.pki.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import so.cb.pki.csr.event.CsrApprovedEvent;
import so.cb.pki.notification.service.PkiNotificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class PkiNotificationListener {

    private final PkiNotificationService notificationService;

    @ApplicationModuleListener
    public void handleCsrApprovedEvent(CsrApprovedEvent event) {
        if (event == null) {
            log.warn("CsrApprovedEvent is null, skipping");
            return;
        }

        log.info(
                "Handling CSR approved event for csrId={}, institutionId={}, bic={}",
                event.csrId(),
                event.institutionId(),
                event.bic()
        );

        notificationService.handleApprovedCsr(
                event.csrId(),
                event.institutionId(),
                event.bic(),
                event.csrPem()
        );
    }
}