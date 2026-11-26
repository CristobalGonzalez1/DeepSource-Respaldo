package cl.deepsource.projects;
import jakarta.persistence.*;
@Entity @Table(name="projects")
public class ProjectEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private String name;
    private String description;
    private String clientId;
    public String getClientId() { return clientId; }
    public void setClientId(String value) { clientId=value; }
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { this.description = value; }
}
