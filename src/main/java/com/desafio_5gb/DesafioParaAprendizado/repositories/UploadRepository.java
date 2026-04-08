package com.desafio_5gb.DesafioParaAprendizado.repositories;

import com.desafio_5gb.DesafioParaAprendizado.models.Upload;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadRepository extends JpaRepository<Upload, String> {
}