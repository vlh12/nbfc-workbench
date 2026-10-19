package com.nbfc.stresssimulation;
import com.nbfc.shared.StressData.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
class ScenarioServiceTest {
 @Test void missingInputsPersistFailedRunAndIdempotencyReturnsThatRun(){
  var repository=mock(Scenarios.class);var stored=new HashMap<String,ScenarioRecord>();
  when(repository.findByBankIdAndCreatedByAndRequestKey(anyString(),anyString(),anyString())).thenAnswer(i->Optional.ofNullable(stored.get(i.getArgument(2))));
  when(repository.saveAndFlush(any())).thenAnswer(i->{ScenarioRecord r=i.getArgument(0);stored.put(r.requestKey,r);return r;});
  var service=new ScenarioService(repository,new ObjectMapper().findAndRegisterModules(),"http://127.0.0.1:1");
  var request=new Request(null,"SYN-NBFC-001",null,null,null,"60","30","Synthetic test","request-1");
  var first=service.execute(request,"analyst");
  assertThat(first.status()).isEqualTo("FAILED");assertThat(first.result()).isNull();assertThat(first.failureReason()).contains("Select saved");
  assertThat(service.execute(request,"analyst").id()).isEqualTo(first.id());
  verify(repository,times(1)).saveAndFlush(any());
  assertThatThrownBy(()->service.execute(new Request(null,"SYN-NBFC-001",null,null,null,"70","30","Synthetic test","request-1"),"analyst"))
    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
 }
}
