package fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false, nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String objectKey;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private long size;

    // Rempli par le domaine (le service fixe la date de création), recopié par le mapper.
    // Pas de @PrePersist ici : une seule source de vérité pour cette donnée métier.
    @Column(updatable = false, nullable = false)
    private Instant createdOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskEntity task;
}