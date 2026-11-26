package cl.deepsource.hours;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface HoursRepository extends JpaRepository<HoursEntity,Long> {
    Optional<HoursEntity> findFirstByClientId(String clientId);
}
