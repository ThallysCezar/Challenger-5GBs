package com.desafio_5gb.DesafioParaAprendizado.services;

import com.desafio_5gb.DesafioParaAprendizado.models.Upload;
import com.desafio_5gb.DesafioParaAprendizado.models.UploadPart;
import com.desafio_5gb.DesafioParaAprendizado.repositories.UploadPartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkService {

    private final UploadService uploadService;
    private final S3Client s3Client;
    private final UploadPartRepository uploadPartRepository;
    private final StringRedisTemplate redis;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${upload.temp-dir}")
    private String tempDir;

    public void processChunk(String uploadId,
                             Integer chunkIndex,
                             MultipartFile file) throws IOException {

        Upload upload = uploadService.findById(uploadId);
        Path chunkPath = saveChunkToDisk(uploadId, chunkIndex, file);

        UploadPartResponse response = uploadChunkToMinio(upload, chunkIndex, chunkPath);

        savePartMetadata(uploadId, chunkIndex + 1, response.eTag());

        Files.deleteIfExists(chunkPath);

        uploadService.incrementReceivedChunks(uploadId);

        log.info("Chunk {}/{} processado para uploadId={}",
                chunkIndex + 1, upload.getTotalChunks(), uploadId);
    }

    private Path saveChunkToDisk(String uploadId,
                                 Integer chunkIndex,
                                 MultipartFile file) throws IOException {
        Path dir = Path.of(tempDir, uploadId);
        Files.createDirectories(dir);

        Path chunkPath = dir.resolve("chunk_" + chunkIndex);
        file.transferTo(chunkPath);
        return chunkPath;
    }

    private UploadPartResponse uploadChunkToMinio(Upload upload,
                                                  Integer chunkIndex,
                                                  Path chunkPath) {
        int partNumber = chunkIndex + 1;

        return s3Client.uploadPart(
                UploadPartRequest.builder()
                        .bucket(bucket)
                        .key(upload.getFilename())
                        .uploadId(upload.getMinioUploadId())
                        .partNumber(partNumber)
                        .build(),
                RequestBody.fromFile(chunkPath)
        );
    }

    private void savePartMetadata(String uploadId, Integer partNumber, String eTag) {
        UploadPart part = UploadPart.builder()
                .uploadId(uploadId)
                .partNumber(partNumber)
                .eTag(eTag)
                .build();
        uploadPartRepository.save(part);
    }

    public void getUploadByIdMissisChunks(Long id){

    }

    public boolean tryClaimChunk(String uploadId, int chunkIndex) {
        String key = "upload:" + uploadId + ":chunk:" + chunkIndex;
        // SETNX com TTL: retorna true só se a chave não existia
        Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", Duration.ofHours(1));
        return Boolean.TRUE.equals(acquired);
    }
}