package com.nbfc.accessdata.data.portfolio;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nbfc.shared.PortfolioData.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class DataReviewStoreTest {
 PortfolioDocuments documents;DataReviews reviews;PortfolioStore store;PortfolioDocument record;
 @BeforeEach void setup()throws Exception{
  documents=mock(PortfolioDocuments.class);reviews=mock(DataReviews.class);store=new PortfolioStore(documents,new ObjectMapper().findAndRegisterModules());store.setReviews(reviews);
  record=new PortfolioDocument();record.id=UUID.randomUUID().toString();record.kind="CAPITAL";record.bankId="SYN-BANK-001";record.reportingDate=LocalDate.parse("2026-09-30");record.status="PENDING_REVIEW";record.submittedBy="contributor";record.createdAt=Instant.now();record.payload="{\"bankId\":\"SYN-BANK-001\",\"reportingDate\":\"2026-09-30\",\"eligibleCapital\":\"50000000000\",\"label\":\"September eligible capital\"}";
  when(documents.findByIdAndBankId(record.id,record.bankId)).thenReturn(Optional.of(record));when(documents.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));
 }
 @Test void approvalRecordsReasonAndReleasesAnalystAccess(){
  assertThatThrownBy(()->store.requireApproved(record.id)).hasMessageContaining("Supervisor data approval");
  var entry=store.review(record.id,"APPROVED","Capital definition and reporting date checked.","supervisor");
  assertThat(entry.status()).isEqualTo("APPROVED");assertThat(entry.name()).contains("September eligible capital");assertThatCode(()->store.requireApproved(record.id)).doesNotThrowAnyException();
  verify(reviews).save(argThat(e->e.decision.equals("APPROVED")&&e.reviewedBy.equals("supervisor")&&e.reviewNote.contains("checked")));
 }
 @Test void returnRemainsBlockedAndCannotBeRewritten(){
  store.review(record.id,"RETURNED","Correct the reporting date.","supervisor");
  assertThatThrownBy(()->store.requireApproved(record.id)).hasMessageContaining("Supervisor data approval");
  assertThatThrownBy(()->store.review(record.id,"APPROVED","ok","supervisor")).hasMessageContaining("already been reviewed");
  verify(reviews,times(1)).save(any());
 }
 @Test void noSelfReviewOrBlankReason(){
  assertThatThrownBy(()->store.review(record.id,"APPROVED","Checked","contributor")).hasMessageContaining("own submitted data");
  assertThatThrownBy(()->store.review(record.id,"APPROVED"," ","supervisor")).hasMessageContaining("review reason");verifyNoInteractions(reviews);
 }
 @Test void unresolvedPortfolioCannotBeApproved()throws Exception{
  record.kind="PORTFOLIO";record.payload=new ObjectMapper().findAndRegisterModules().writeValueAsString(new Snapshot(record.id,record.bankId,record.reportingDate,"PENDING_REVIEW","contributor",record.createdAt,List.of(),List.of(new Issue(1,"ead_amount","Explicit EAD missing")),"September portfolio"));
  assertThatThrownBy(()->store.review(record.id,"APPROVED","Reviewed","supervisor")).hasMessageContaining("Correct reconciliation warnings");verifyNoInteractions(reviews);
 }
 @Test void approvalChangesStatusButPreservesSubmittedPayload()throws Exception{
  record.kind="PORTFOLIO";var mapper=new ObjectMapper().findAndRegisterModules();record.payload=mapper.writeValueAsString(new Snapshot(record.id,record.bankId,record.reportingDate,"PENDING_REVIEW","contributor",record.createdAt,List.of(),List.of(),"September portfolio"));String original=record.payload;
  store.review(record.id,"APPROVED","Checked the loan values.","supervisor");
  assertThat(store.get(record.id,"PORTFOLIO",Snapshot.class).status()).isEqualTo("APPROVED");assertThat(record.payload).isEqualTo(original);
 }
}
