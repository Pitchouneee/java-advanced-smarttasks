package fr.corentinbringer.smarttasks.project.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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

    @Column(updatable = false, nullable = false)
    private LocalDateTime uploadedOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskEntity task;

    @PrePersist
    protected void onCreate() {
        this.uploadedOn = LocalDateTime.now();
    }
}