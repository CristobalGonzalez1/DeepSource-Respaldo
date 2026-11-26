package cl.deepsource.hours;
import jakarta.persistence.*;
@Entity @Table(name="hours")
public class HoursEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private Long projectId;
    private String taskName;
    private Long durationMinutes;
    private String workedOn;
    private String note;
    private String clientId;
    public String getClientId() { return clientId; }
    public void setClientId(String value) { clientId=value; }
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long value) { this.projectId = value; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String value) { this.taskName = value; }
    public Long getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Long value) { this.durationMinutes = value; }
    public String getWorkedOn() { return workedOn; }
    public void setWorkedOn(String value) { this.workedOn = value; }
    public String getNote() { return note; }
    public void setNote(String value) { this.note = value; }
}
