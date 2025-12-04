package fr.corentinbringer.smarttasks.project.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false, nullable = false)
    private String tenantId;

    @Column(length = 50)
    private String name;

    @Column(updatable = false, nullable = false)
    private LocalDateTime createdOn;

    @PrePersist
    protected void onCreate() {
        this.createdOn = LocalDateTime.now();
    }
}
