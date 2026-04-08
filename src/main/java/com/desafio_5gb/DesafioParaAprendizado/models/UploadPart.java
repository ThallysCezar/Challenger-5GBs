package com.desafio_5gb.DesafioParaAprendizado.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "upload_parts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String uploadId;

    @Column(nullable = false)
    private Integer partNumber;

    @Column(nullable = false)
    private String eTag;

}