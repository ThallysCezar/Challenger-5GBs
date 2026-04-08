package com.desafio_5gb.DesafioParaAprendizado.services;

import com.desafio_5gb.DesafioParaAprendizado.dto.InitUploadRequest;
import com.desafio_5gb.DesafioParaAprendizado.dto.InitUploadResponse;
import com.desafio_5gb.DesafioParaAprendizado.models.Upload;
import com.desafio_5gb.DesafioParaAprendizado.models.enums.UploadStatus;
import com.desafio_5gb.DesafioParaAprendizado.repositories.UploadRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

    private final UploadRepository uploadRepository;
    private final S3Client s3Client;

    @Value("${minio.bucket}")
    private String bucket;

    public InitUploadResponse initiateUpload(InitUploadRequest request) {

        CreateMultipartUploadResponse minioResponse = s3Client
                .createMultipartUpload(b -> b
                        .bucket(bucket)
                        .key(request.filename())
                );

        Upload upload = Upload.builder()
                .filename(request.filename())
                .totalChunks(request.totalChunks())
                .minioUploadId(minioResponse.uploadId())
                .build();

        uploadRepository.save(upload);

        log.info("Upload iniciado: id={}, filename={}, totalChunks={}",
                upload.getId(), upload.getFilename(), upload.getTotalChunks());

        return new InitUploadResponse(upload.getId());
    }

    public Upload findById(String uploadId) {
        return uploadRepository.findById(uploadId)
                .orElseThrow(() -> new RuntimeException("Upload não encontrado: " + uploadId));
    }

    @Transactional
    public void incrementReceivedChunks(String uploadId) {
        Upload upload = findById(uploadId);
        upload.setReceivedChunks(upload.getReceivedChunks() + 1);

        if (upload.getStatus() == UploadStatus.INITIATED) {
            upload.setStatus(UploadStatus.IN_PROGRESS);
        }

        if (upload.getReceivedChunks().equals(upload.getTotalChunks())) {
            upload.setStatus(UploadStatus.PROCESSING);
            log.info("Todos os chunks recebidos para uploadId={}. Pronto pra montar!", uploadId);
            // Aqui dispararei pro RabbitMQ
        }

        uploadRepository.save(upload);
    }

}