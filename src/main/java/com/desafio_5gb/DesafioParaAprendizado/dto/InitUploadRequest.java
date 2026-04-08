package com.desafio_5gb.DesafioParaAprendizado.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public record InitUploadRequest(String filename, Integer totalChunks) {
}
