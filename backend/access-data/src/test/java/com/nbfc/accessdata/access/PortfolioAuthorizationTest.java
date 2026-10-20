package com.nbfc.accessdata.access;
import com.nbfc.accessdata.data.portfolio.*;
import com.nbfc.shared.ApiSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
@WebMvcTest(controllers=PortfolioController.class,properties="WORKBENCH_DEMO_ENABLED=true") @Import(ApiSecurity.class)
class PortfolioAuthorizationTest{
 @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
 @Autowired MockMvc mvc; @MockitoBean PortfolioStore store;@MockitoBean PortfolioCsv csv;
 @Test void analystAndSupervisorReadSavedMetadata()throws Exception{
  mvc.perform(get("/api/portfolios").header("X-Demo-Role","ANALYST")).andExpect(status().isOk());
  mvc.perform(get("/api/portfolios").header("X-Demo-Role","SUPERVISOR")).andExpect(status().isOk());
  verify(store,times(2)).list();
 }
 @Test void analystCannotUpload()throws Exception{
  mvc.perform(multipart("/api/portfolios").file("file","data".getBytes()).param("synthetic","true").header("X-Demo-Role","ANALYST")).andExpect(status().isForbidden());
  verifyNoInteractions(store,csv);
 }
 @Test void contributorCannotSetThresholds()throws Exception{
  mvc.perform(post("/api/portfolios/thresholds").contentType("application/json").content("{}").header("X-Demo-Role","CONTRIBUTOR")).andExpect(status().isForbidden());
  verifyNoInteractions(store);
 }
 @Test void anonymousCannotRead()throws Exception{mvc.perform(get("/api/portfolios")).andExpect(status().isUnauthorized());}
 @Test void contributorNameAndGeneratedIdAreReturnedForMultipartAndJson()throws Exception{
  var bytes=getClass().getResourceAsStream("/portfolio-16.csv").readAllBytes();
  var validation=new PortfolioCsv().validate(bytes);
  String id=java.util.UUID.randomUUID().toString(),name="September Exposure Portfolio";
  var snapshot=new com.nbfc.shared.PortfolioData.Snapshot(id,"SYN-BANK-001",java.time.LocalDate.parse("2026-09-30"),"VALIDATED","actor",java.time.Instant.now(),validation.rows(),validation.warnings(),name);
  when(csv.validate(any(byte[].class))).thenReturn(validation);
  when(store.save(eq(validation),anyString(),eq(name))).thenReturn(snapshot);
  mvc.perform(multipart("/api/portfolios").file("file",bytes).param("synthetic","true").param("name",name).header("X-Demo-Role","CONTRIBUTOR"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.name").value(name));
  var body=new PortfolioController.JsonSubmission(validation.rows(),true,name);
  mvc.perform(post("/api/portfolios").contentType("application/json").content(json.writeValueAsString(body)).header("X-Demo-Role","CONTRIBUTOR"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.name").value(name));
  verify(store,times(2)).save(eq(validation),anyString(),eq(name));
 }

}
