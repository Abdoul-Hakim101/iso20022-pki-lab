package so.cb.pki.notification.retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FailedEventRepublisher {

    private final IncompleteEventPublications incompletePublications;

    @Value("${application.events.retry.max-attempts}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${application.events.retry.delay}")
    public void retryFailedEvents() {
        log.info("Retrying failed Modulith events...");

        try {
            incompletePublications.resubmitIncompletePublications(
                    publication ->
                            !publication.isCompleted() &&
                                    publication.getCompletionAttempts() < maxAttempts
            );

            log.info("Retry completed");
        } catch (Exception e) {
            log.error("Error retrying failed events", e);
        }
    }
}
