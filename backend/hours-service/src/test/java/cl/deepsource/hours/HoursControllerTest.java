package cl.deepsource.hours;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc class HoursControllerTest {
 @Autowired MockMvc mvc;
 @Test void createListUpdateDelete() throws Exception {
  String body="{\"projectId\":1,\"taskName\":\"test\",\"durationMinutes\":1,\"workedOn\":\"2026-11-26\",\"note\":\"test\"}";
  String response=mvc.perform(post("/api/hours").contentType("application/json").content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists()).andReturn().getResponse().getContentAsString();
  long id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asLong();
  mvc.perform(get("/api/hours")).andExpect(status().isOk());
  mvc.perform(get("/api/hours/"+id)).andExpect(status().isOk());
  mvc.perform(put("/api/hours/"+id).contentType("application/json").content(body)).andExpect(status().isOk());
  mvc.perform(delete("/api/hours/"+id)).andExpect(status().isNoContent());
  mvc.perform(get("/api/hours/"+id)).andExpect(status().isNotFound());
 }
}
