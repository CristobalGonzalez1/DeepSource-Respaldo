package cl.deepsource.projects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc class ProjectControllerTest {
 @Autowired MockMvc mvc;
 @Test void createListUpdateDelete() throws Exception {
  String body="{\"name\":\"test\",\"description\":\"test\"}";
  String response=mvc.perform(post("/api/projects").contentType("application/json").content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists()).andReturn().getResponse().getContentAsString();
  long id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asLong();
  mvc.perform(get("/api/projects")).andExpect(status().isOk());
  mvc.perform(get("/api/projects/"+id)).andExpect(status().isOk());
  mvc.perform(put("/api/projects/"+id).contentType("application/json").content(body)).andExpect(status().isOk());
  mvc.perform(delete("/api/projects/"+id)).andExpect(status().isNoContent());
  mvc.perform(get("/api/projects/"+id)).andExpect(status().isNotFound());
 }
}
