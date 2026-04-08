package com.desafio_5gb.DesafioParaAprendizado.repositories;

import com.desafio_5gb.DesafioParaAprendizado.models.UploadPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UploadPartRepository extends JpaRepository<UploadPart, Long> {

    List<UploadPart> findByUploadIdOrderByPartNumberAsc(String uploadId);

}