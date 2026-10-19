package com.nbfc.accessdata.data;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nbfc.shared.ExposureEvent;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class UploadService {
    private final CsvValidator validator;
    private final BatchRepository batches;
    private final OutboxRepository outbox;
    private final ObjectMapper json;
    public UploadService(CsvValidator validator,BatchRepository batches,OutboxRepository outbox,ObjectMapper json) {
        this.validator=validator; this.batches=batches; this.outbox=outbox; this.json=json;
    }
    @Transactional
    public Batch upload(byte[] bytes,String actor) {
        var rows = validator.validate(bytes);
        String batchId=UUID.randomUUID().toString(), eventId=UUID.randomUUID().toString();
        try {
            String payload=json.writeValueAsString(new ExposureEvent(eventId,batchId,rows));
            Batch batch=batches.save(new Batch(batchId,actor,rows.size()));
            outbox.save(new OutboxEvent(eventId,batchId,payload));
            return batch;
        } catch (JsonProcessingException e) { throw new IllegalStateException("Could not serialize exposure event.",e); }
    }
}

