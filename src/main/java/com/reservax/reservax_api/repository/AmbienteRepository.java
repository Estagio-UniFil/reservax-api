package com.reservax.reservax_api.repository;

import com.reservax.reservax_api.model.Ambiente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmbienteRepository extends JpaRepository<Ambiente, Long> {

    List<Ambiente> findAllByDataExclusaoIsNullOrderByNomeAsc();

    Optional<Ambiente> findByIdAndDataExclusaoIsNull(Long id);
}
