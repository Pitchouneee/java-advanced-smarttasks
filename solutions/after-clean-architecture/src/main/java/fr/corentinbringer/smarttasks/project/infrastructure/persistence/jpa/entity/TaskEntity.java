package fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false, nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(length = 100, nullable = false)
    private String title;

    // "text" plutôt que @Lob : sur PostgreSQL, @Lob crée une colonne oid (Large Object hors table)
    @Column(columnDefinition = "text")
    private String description;

    private LocalDate dueDate;

    @Column(nullable = false)
    private boolean completed = false;

    // Rempli par le domaine (le service fixe la date de création), recopié par le mapper.
    // Pas de @PrePersist ici : une seule source de vérité pour cette donnée métier.
    @Column(updatable = false, nullable = false)
    private Instant createdOn;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AttachmentEntity> attachments;
}
