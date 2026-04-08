package com.desafio_5gb.DesafioParaAprendizado.services;

import com.desafio_5gb.DesafioParaAprendizado.models.Upload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkService {

    private final UploadService uploadService;
    private final S3Client s3Client;

    public ChunkService(UploadService uploadService, S3Client s3Client, String bucket, String tempDir) {
        this.uploadService = uploadService;
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.tempDir = tempDir;
    }

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${upload.temp-dir}")
    private String tempDir;

    public void processChunk(String uploadId,
                             Integer chunkIndex,
                             MultipartFile file) throws IOException {

        Upload upload = uploadService.findById(uploadId);
        Path chunkPath = saveChunkToDisk(uploadId, chunkIndex, file);

        uploadChunkToMinio(upload, chunkIndex, chunkPath);

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

    private void uploadChunkToMinio(Upload upload,
                                    Integer chunkIndex,
                                    Path chunkPath) {
        int partNumber = chunkIndex + 1;

        s3Client.uploadPart(
                UploadPartRequest.builder()
                        .bucket(bucket)
                        .key(upload.getFilename())
                        .uploadId(upload.getMinioUploadId())
                        .partNumber(partNumber)
                        .build(),
                RequestBody.fromFile(chunkPath)
        );
    }

}
