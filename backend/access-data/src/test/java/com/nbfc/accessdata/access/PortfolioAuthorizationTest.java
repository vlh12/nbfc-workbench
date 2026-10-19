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
}
