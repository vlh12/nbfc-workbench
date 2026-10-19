package com.nbfc.exposureanalytics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nbfc.shared.ExposureEvent;
import java.util.ArrayList;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
public class ExposureConsumer {
    private final ObjectMapper json;
    private final ProcessedRepository processed;
    private final ExposureRepository exposures;
    public ExposureConsumer(ObjectMapper json,ProcessedRepository processed,ExposureRepository exposures) {
        this.json=json; this.processed=processed; this.exposures=exposures;
    }
    @KafkaListener(topics=ExposureEvent.TOPIC)
    @Transactional
    public void consume(String payload) throws Exception {
        var event=json.readValue(payload,ExposureEvent.class);
        if (processed.existsById(event.eventId())) return;
        processed.saveAndFlush(new ProcessedEvent(event.eventId()));
        var records=new ArrayList<ExposureRecord>();
        for(int i=0;i<event.rows().size();i++)
            records.add(new ExposureRecord(event.eventId()+":"+i,event.batchId(),event.rows().get(i)));
        exposures.saveAll(records);
    }
}

