package com.nbfc.accessdata.data;
import com.nbfc.shared.ExposureEvent;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
public class OutboxPublisher {
    private static final Logger log=LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxRepository outbox;
    private final KafkaTemplate<String,String> kafka;
    public OutboxPublisher(OutboxRepository outbox,KafkaTemplate<String,String> kafka) { this.outbox=outbox; this.kafka=kafka; }
    @Scheduled(fixedDelayString="${workbench.outbox.delay-ms:3000}")
    @Transactional
    public void publish() {
        for (var event:outbox.findTop20ByPublishedFalseOrderByCreatedAtAsc()) {
            try {
                kafka.send(ExposureEvent.TOPIC,event.batchId,event.payload).get(10,TimeUnit.SECONDS);
                event.published=true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); return;
            } catch (Exception e) {
                log.warn("Event {} remains pending for retry ({})",event.id,e.getClass().getSimpleName());
                return;
            }
        }
    }
}

