package com.desafio_5gb.DesafioParaAprendizado.controllers;

import com.desafio_5gb.DesafioParaAprendizado.dto.InitUploadRequest;
import com.desafio_5gb.DesafioParaAprendizado.dto.InitUploadResponse;
import com.desafio_5gb.DesafioParaAprendizado.models.Upload;
import com.desafio_5gb.DesafioParaAprendizado.services.ChunkService;
import com.desafio_5gb.DesafioParaAprendizado.services.UploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/upload")
public class UploadController {

    private final UploadService uploadService;
    private final ChunkService chunkService;

    public UploadController(UploadService uploadService, ChunkService chunkService) {
        this.uploadService = uploadService;
        this.chunkService = chunkService;
    }

    @PostMapping("/init")
    public ResponseEntity<InitUploadResponse> init(
            @RequestBody InitUploadRequest request) {

        InitUploadResponse response = uploadService.initiateUpload(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chunk/{uploadId}/{chunkIndex}")
    public ResponseEntity<Void> uploadChunk(
            @PathVariable String uploadId,
            @PathVariable Integer chunkIndex,
            @RequestParam("file") MultipartFile file) throws IOException {

        chunkService.processChunk(uploadId, chunkIndex, file);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{uploadId}")
    public ResponseEntity<Upload> status(@PathVariable String uploadId) {
        return ResponseEntity.ok(uploadService.findById(uploadId));
    }
}
