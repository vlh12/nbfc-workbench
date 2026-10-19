package com.nbfc.accessdata.data.portfolio;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nbfc.shared.PortfolioData.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class PortfolioStoreTest{
 @Test void contributorSaveIsImmutableAndReadableByReviewerWithoutAddingPriorVersions()throws Exception{
  var repository=mock(PortfolioDocuments.class);
  var database=new HashMap<String,PortfolioDocument>();
  when(repository.save(any())).thenAnswer(i->{PortfolioDocument d=i.getArgument(0);database.put(d.id,d);return d;});
  when(repository.findByIdAndBankId(anyString(),anyString())).thenAnswer(i->Optional.ofNullable(database.get(i.getArgument(0))));
  var store=new PortfolioStore(repository,new ObjectMapper().findAndRegisterModules());
  byte[] bytes=getClass().getResourceAsStream("/portfolio-16.csv").readAllBytes();
  var validation=new PortfolioCsv().validate(bytes);
  var first=store.save(validation,"synthetic-contributor");
  var second=store.save(validation,"synthetic-contributor");
  assertThat(second.id()).isNotEqualTo(first.id());
  assertThat(store.get(first.id(),"PORTFOLIO",Snapshot.class).rows()).hasSize(16);
  assertThat(store.get(second.id(),"PORTFOLIO",Snapshot.class).rows()).hasSize(16);
  assertThat(first.status()).isEqualTo("VALIDATED");
  assertThat(database).hasSize(2);
 }
 @Test void qualityTotalsAndCapitalAreValidated(){
  var store=new PortfolioStore(mock(PortfolioDocuments.class),new ObjectMapper().findAndRegisterModules());
  assertThatThrownBy(()->store.capital(new Capital("SYN-BANK-001",java.time.LocalDate.now(),"0","eligible"),"actor")).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->store.quality(new Quality("SYN-BANK-001","SYN-NBFC-001",java.time.LocalDate.now(),Map.of("CURRENT","100"),"100"),"actor")).isInstanceOf(IllegalArgumentException.class);
 }
}
