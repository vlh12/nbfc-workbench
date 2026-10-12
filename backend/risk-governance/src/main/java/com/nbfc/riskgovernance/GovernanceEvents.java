package com.nbfc.riskgovernance;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Configuration
class GovernanceEvents {
    @Bean NewTopic governanceTopic() { return TopicBuilder.name("risk-governance-events").partitions(1).replicas(1).build(); }
}
@Component
class GovernanceOutboxPublisher {
    private static final Logger log=LoggerFactory.getLogger(GovernanceOutboxPublisher.class);
    private final GovernanceOutboxRepository events;
    private final KafkaTemplate<String,String> kafka;
    GovernanceOutboxPublisher(GovernanceOutboxRepository events,KafkaTemplate<String,String> kafka) { this.events=events; this.kafka=kafka; }
    @Scheduled(fixedDelayString="${workbench.outbox.delay-ms:3000}")
    @Transactional
    public void publish() {
        for(var event:events.findTop20ByPublishedFalseOrderByCreatedAtAsc()) {
            try {
                kafka.send("risk-governance-events",event.aggregateId,event.payload).get(10,TimeUnit.SECONDS);
                event.published=true;
            } catch(InterruptedException ex) { Thread.currentThread().interrupt(); return; }
            catch(Exception ex) { log.warn("Governance event {} remains pending for retry ({})",event.id,ex.getClass().getSimpleName()); return; }
        }
    }
}

