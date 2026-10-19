package com.nbfc.exposureanalytics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nbfc.shared.ExposureEvent;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class ExposureConsumerTest {
    @Test void duplicateDeliveryDoesNotDoubleCount() throws Exception {
        var json=new ObjectMapper();
        var processed=mock(ProcessedRepository.class);
        var exposures=mock(ExposureRepository.class);
        when(processed.existsById("synthetic-event")).thenReturn(false,true);
        var consumer=new ExposureConsumer(json,processed,exposures);
        var event=new ExposureEvent("synthetic-event","synthetic-batch",List.of(new ExposureEvent.Row("SYN-BANK-1","SYN-NBFC-1",new BigDecimal("10.10"))));
        String payload=json.writeValueAsString(event);
        consumer.consume(payload);
        consumer.consume(payload);
        verify(processed,times(1)).saveAndFlush(any());
        verify(exposures,times(1)).saveAll(argThat(records -> {
            var record=records.iterator().next();
            assertThat(record.exposureAmount).isEqualByComparingTo("10.10");
            return record.id.equals("synthetic-event:0");
        }));
    }
}

