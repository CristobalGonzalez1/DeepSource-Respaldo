package cl.deepsource.projects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc class TaskControllerTest {
 @Autowired MockMvc mvc;
 @Test void createListUpdateDelete() throws Exception {
  String body="{\"projectId\":1,\"name\":\"test\",\"status\":\"test\"}";
  String response=mvc.perform(post("/api/tasks").contentType("application/json").content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists()).andReturn().getResponse().getContentAsString();
  long id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asLong();
  mvc.perform(get("/api/tasks")).andExpect(status().isOk());
  mvc.perform(get("/api/tasks/"+id)).andExpect(status().isOk());
  mvc.perform(put("/api/tasks/"+id).contentType("application/json").content(body)).andExpect(status().isOk());
  mvc.perform(delete("/api/tasks/"+id)).andExpect(status().isNoContent());
  mvc.perform(get("/api/tasks/"+id)).andExpect(status().isNotFound());
 }
}
