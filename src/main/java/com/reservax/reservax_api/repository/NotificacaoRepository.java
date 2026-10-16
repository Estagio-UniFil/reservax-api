package com.reservax.reservax_api.repository;

import com.reservax.reservax_api.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByUsuarioEmailIgnoreCaseAndLidaFalseOrderByDataCriacaoDesc(
            String email
    );
}
