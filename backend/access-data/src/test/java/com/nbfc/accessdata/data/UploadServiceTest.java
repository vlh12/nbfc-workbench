package com.nbfc.accessdata.data;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class UploadServiceTest {
    @Test void invalidFileNeverReachesPersistence() {
        var batches=mock(BatchRepository.class);
        var outbox=mock(OutboxRepository.class);
        var service=new UploadService(new CsvValidator(),batches,outbox,new ObjectMapper());
        assertThatThrownBy(() -> service.upload("bad header".getBytes(StandardCharsets.UTF_8),"synthetic-subject"))
            .isInstanceOf(CsvValidator.InvalidCsv.class);
        verifyNoInteractions(batches,outbox);
    }
    @Test void savesBatchAndDeliveryRecordTogether() {
        var batches=mock(BatchRepository.class);
        var outbox=mock(OutboxRepository.class);
        when(batches.save(any())).thenAnswer(i -> i.getArgument(0));
        var service=new UploadService(new CsvValidator(),batches,outbox,new ObjectMapper());
        var batch=service.upload("bank_id,nbfc_id,exposure_amount\nSYN-BANK-1,SYN-NBFC-1,10.10\n".getBytes(StandardCharsets.UTF_8),"synthetic-subject");
        assertThat(batch.rowCount).isEqualTo(1);
        verify(outbox).save(argThat(event -> event.batchId.equals(batch.id) && !event.published && event.payload.contains("10.10")));
    }
}

