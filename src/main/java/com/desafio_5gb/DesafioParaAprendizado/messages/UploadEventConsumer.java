package com.desafio_5gb.DesafioParaAprendizado.messages;


import com.desafio_5gb.DesafioParaAprendizado.dto.UploadCompleteMessage;
import com.desafio_5gb.DesafioParaAprendizado.models.Upload;
import com.desafio_5gb.DesafioParaAprendizado.models.UploadPart;
import com.desafio_5gb.DesafioParaAprendizado.models.enums.UploadStatus;
import com.desafio_5gb.DesafioParaAprendizado.repositories.UploadPartRepository;
import com.desafio_5gb.DesafioParaAprendizado.services.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UploadEventConsumer {

    private final UploadService uploadService;
    private final UploadPartRepository uploadPartRepository;
    private final S3Client s3Client;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${rabbitmq.queue}")
    private String queue;

    @RabbitListener(queues = "${rabbitmq.queue}")
    public void handleCompleteUpload(UploadCompleteMessage message) {
        log.info("Mensagem recebida da fila para uploadId={}", message.uploadId());

        try {
            completeMultipartUpload(message.uploadId());
        } catch (Exception e) {
            log.error("Erro ao completar upload para uploadId={}", message.uploadId(), e);
            throw e;
        }
    }

    private void completeMultipartUpload(String uploadId) {
        Upload upload = uploadService.findById(uploadId);

        List<UploadPart> parts = uploadPartRepository
                .findByUploadIdOrderByPartNumberAsc(uploadId);

        List<CompletedPart> completedParts = parts.stream()
                .map(part -> CompletedPart.builder()
                        .partNumber(part.getPartNumber())
                        .eTag(part.getETag())
                        .build()
                )
                .toList();

        s3Client.completeMultipartUpload(
                CompleteMultipartUploadRequest.builder()
                        .bucket(bucket)
                        .key(upload.getFilename())
                        .uploadId(upload.getMinioUploadId())
                        .multipartUpload(
                                CompletedMultipartUpload.builder()
                                        .parts(completedParts)
                                        .build()
                        )
                        .build()
        );

        upload.setStatus(UploadStatus.COMPLETED);
        uploadService.save(upload);

        log.info("Arquivo '{}' montado com sucesso! uploadId={}",
                upload.getFilename(), uploadId);
    }
}