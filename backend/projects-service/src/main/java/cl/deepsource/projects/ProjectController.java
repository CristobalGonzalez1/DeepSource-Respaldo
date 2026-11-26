package cl.deepsource.projects;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/projects")
public class ProjectController {
  private final ProjectRepository repository;
  public ProjectController(ProjectRepository repository) { this.repository=repository; }
  @GetMapping public List<ProjectEntity> list() { return repository.findAll(); }
  @GetMapping("/{id}") public ProjectEntity get(@PathVariable Long id) { return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public ProjectEntity create(@RequestBody ProjectEntity input) { input.setId(null); return repository.save(input); }
  @PutMapping("/{id}") public ProjectEntity update(@PathVariable Long id,@RequestBody ProjectEntity input) {
    ProjectEntity current=get(id);
        current.setName(input.getName());
        current.setDescription(input.getDescription());
    current.setClientId(input.getClientId());
    return repository.save(current);
  }
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { if(!repository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND); repository.deleteById(id); }
}
