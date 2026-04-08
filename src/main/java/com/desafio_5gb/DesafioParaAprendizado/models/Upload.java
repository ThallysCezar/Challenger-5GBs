package com.desafio_5gb.DesafioParaAprendizado.models;

import com.desafio_5gb.DesafioParaAprendizado.dto.InitUploadRequest;
import com.desafio_5gb.DesafioParaAprendizado.models.enums.UploadStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "uploads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Upload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public String id;

    private String filename;
    private Integer totalChunks;
    private Integer receivedChunks;
    private UploadStatus status;
    private String minioUploadId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.receivedChunks = 0;
        this.status = UploadStatus.INITIATED;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}