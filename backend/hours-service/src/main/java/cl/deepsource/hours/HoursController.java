package cl.deepsource.hours;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/hours")
public class HoursController {
  private final HoursRepository repository;
  public HoursController(HoursRepository repository) { this.repository=repository; }
  @GetMapping public List<HoursEntity> list() { return repository.findAll(); }
  @GetMapping("/{id}") public HoursEntity get(@PathVariable Long id) { return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public HoursEntity create(@RequestBody HoursEntity input) { input.setId(null); return repository.save(input); }
  @PutMapping("/{id}") public HoursEntity update(@PathVariable Long id,@RequestBody HoursEntity input) {
    HoursEntity current=get(id);
        current.setProjectId(input.getProjectId());
        current.setTaskName(input.getTaskName());
        current.setDurationMinutes(input.getDurationMinutes());
        current.setWorkedOn(input.getWorkedOn());
        current.setNote(input.getNote());
    current.setClientId(input.getClientId());
    return repository.save(current);
  }
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { if(!repository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND); repository.deleteById(id); }
}
