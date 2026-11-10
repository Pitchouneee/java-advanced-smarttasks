package fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false, nullable = false)
    private String tenantId;

    @Column(length = 50)
    private String name;

    // Rempli par le domaine (le service fixe la date de création), recopié par le mapper.
    // Pas de @PrePersist ici : une seule source de vérité pour cette donnée métier.
    @Column(updatable = false, nullable = false)
    private Instant createdOn;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TaskEntity> tasks;
}
