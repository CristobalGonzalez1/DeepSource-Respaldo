package cl.deepsource.projects;
import jakarta.persistence.*;
@Entity @Table(name="tasks")
public class TaskEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private Long projectId;
    private String name;
    private String status;
    private String clientId;
    private Integer estimateMinutes = 90;
    public Integer getEstimateMinutes() { return estimateMinutes; }
    public void setEstimateMinutes(Integer value) { estimateMinutes = value; }
    public String getClientId() { return clientId; }
    public void setClientId(String value) { clientId=value; }
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long value) { this.projectId = value; }
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
}
