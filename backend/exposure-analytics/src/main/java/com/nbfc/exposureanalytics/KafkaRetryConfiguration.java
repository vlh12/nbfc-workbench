package com.nbfc.exposureanalytics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
@Configuration
class KafkaRetryConfiguration {
    @Bean CommonErrorHandler retryFailures() {
        return new DefaultErrorHandler(new FixedBackOff(3000L,FixedBackOff.UNLIMITED_ATTEMPTS));
    }
}

