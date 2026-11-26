package cl.deepsource.projects;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TaskRepository extends JpaRepository<TaskEntity,Long> {}
