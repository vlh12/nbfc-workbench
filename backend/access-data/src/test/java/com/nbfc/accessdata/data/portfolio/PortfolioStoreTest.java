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
  var first=store.save(validation,"synthetic-contributor","  September Exposure Portfolio  ");
  var second=store.save(validation,"synthetic-contributor","September Exposure Portfolio");
  assertThat(second.id()).isNotEqualTo(first.id());
  assertThat(first.name()).isEqualTo("September Exposure Portfolio");
  assertThat(store.get(first.id(),"PORTFOLIO",Snapshot.class).name()).isEqualTo(first.name());
  when(repository.findTop100ByBankIdOrderByCreatedAtDescIdDesc("SYN-BANK-001")).thenReturn(List.of(database.get(first.id()),database.get(second.id())));
  assertThat(store.list()).extracting(Entry::name).containsExactly(first.name(),second.name());
  assertThat(store.get(first.id(),"PORTFOLIO",Snapshot.class).rows()).hasSize(16);
  assertThat(store.get(second.id(),"PORTFOLIO",Snapshot.class).rows()).hasSize(16);
  assertThat(first.status()).isEqualTo("PENDING_REVIEW");
  assertThat(database).hasSize(2);
 }
 @Test void qualityTotalsAndCapitalAreValidated(){
  var store=new PortfolioStore(mock(PortfolioDocuments.class),new ObjectMapper().findAndRegisterModules());
  assertThatThrownBy(()->store.capital(new Capital("SYN-BANK-001",java.time.LocalDate.now(),"0","eligible"),"actor")).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->store.quality(new Quality("SYN-BANK-001","SYN-NBFC-001",java.time.LocalDate.now(),Map.of("CURRENT","100"),"100"),"actor")).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void invalidNamesAreRejectedBeforePersistence()throws Exception{
  var repository=mock(PortfolioDocuments.class);
  var store=new PortfolioStore(repository,new ObjectMapper().findAndRegisterModules());
  var validation=new PortfolioCsv().validate(getClass().getResourceAsStream("/portfolio-16.csv").readAllBytes());
  for(String name:List.of("   ","x".repeat(121),"September\nPortfolio"))
   assertThatThrownBy(()->store.save(validation,"actor",name)).isInstanceOf(IllegalArgumentException.class);
  verifyNoInteractions(repository);
 }
 @Test void legacyPayloadWithoutNameRemainsReadableInDetailsAndList()throws Exception{
  var repository=mock(PortfolioDocuments.class);
  var mapper=new ObjectMapper().findAndRegisterModules();
  var validation=new PortfolioCsv().validate(getClass().getResourceAsStream("/portfolio-16.csv").readAllBytes());
  var document=new PortfolioDocument(); document.id=UUID.randomUUID().toString();document.bankId="SYN-BANK-001";document.kind="PORTFOLIO";document.reportingDate=java.time.LocalDate.parse("2026-09-30");document.status="VALIDATED";document.submittedBy="actor";document.createdAt=java.time.Instant.now();document.rowCount=16;
  var oldSnapshot=new Snapshot(document.id,document.bankId,document.reportingDate,document.status,document.submittedBy,document.createdAt,validation.rows(),validation.warnings());
  var tree=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.valueToTree(oldSnapshot); tree.remove("name");document.payload=mapper.writeValueAsString(tree);
  when(repository.findByIdAndBankId(document.id,document.bankId)).thenReturn(Optional.of(document));
  when(repository.findTop100ByBankIdOrderByCreatedAtDescIdDesc(document.bankId)).thenReturn(List.of(document));
  var store=new PortfolioStore(repository,mapper);
  assertThat(store.get(document.id,"PORTFOLIO",Snapshot.class).name()).isEqualTo("Portfolio — 2026-09-30");
  assertThat(store.list().getFirst().name()).isEqualTo("Portfolio — 2026-09-30");
  assertThat(document.payload).doesNotContain("\"name\"");
 }

}
