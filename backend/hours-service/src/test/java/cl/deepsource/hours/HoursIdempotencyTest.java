package cl.deepsource.hours;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;

class HoursIdempotencyTest {
  @Test void repeatedSubmissionReturnsExistingRecord() {
    HoursRepository repo = mock(HoursRepository.class);
    HoursEntity input = new HoursEntity();
    input.setClientId("local-123");
    HoursEntity stored = new HoursEntity();
    stored.setId(4L);
    stored.setClientId("local-123");
    when(repo.findFirstByClientId("local-123")).thenReturn(Optional.of(stored));
    var result = new HoursController(repo).create(input);
    assertEquals(4L, result.getId());
    verify(repo, never()).save(any());
  }
}
