package cl.deepsource.projects;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/tasks")
public class TaskController {
  private final TaskRepository repository;
  public TaskController(TaskRepository repository) { this.repository=repository; }
  @GetMapping public List<TaskEntity> list() { return repository.findAll(); }
  @GetMapping("/{id}") public TaskEntity get(@PathVariable Long id) { return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public TaskEntity create(@RequestBody TaskEntity input) { input.setId(null); return repository.save(input); }
  @PutMapping("/{id}") public TaskEntity update(@PathVariable Long id,@RequestBody TaskEntity input) {
    TaskEntity current=get(id);
        current.setProjectId(input.getProjectId());
        current.setName(input.getName());
        current.setStatus(input.getStatus());
    current.setClientId(input.getClientId());
    current.setEstimateMinutes(input.getEstimateMinutes());
    return repository.save(current);
  }
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { if(!repository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND); repository.deleteById(id); }
}
