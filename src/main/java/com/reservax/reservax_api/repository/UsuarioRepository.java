package com.reservax.reservax_api.repository;

import com.reservax.reservax_api.enums.TipoUsuario;
import com.reservax.reservax_api.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    Optional<Usuario> findFirstByNomeIgnoreCase(String nome);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByTipoUsuario(TipoUsuario tipoUsuario);

    List<Usuario> findAllByOrderByNomeAsc();

    List<Usuario> findByTipoUsuarioAndAtivoTrue(TipoUsuario tipoUsuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u
            FROM Usuario u
            WHERE u.tipoUsuario = :tipoUsuario
              AND u.ativo = true
            ORDER BY u.id
            """)
    List<Usuario> buscarAtivosParaAtualizacao(
            @Param("tipoUsuario") TipoUsuario tipoUsuario
    );
}
