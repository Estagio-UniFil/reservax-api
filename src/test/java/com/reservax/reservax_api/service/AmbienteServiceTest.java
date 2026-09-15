package com.reservax.reservax_api.service;

import com.reservax.reservax_api.model.Ambiente;
import com.reservax.reservax_api.repository.AmbienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmbienteServiceTest {

    @Mock
    private AmbienteRepository ambienteRepository;

    @InjectMocks
    private AmbienteService ambienteService;

    @Test
    void deveExcluirAmbienteLogicamente() {
        Ambiente ambiente = new Ambiente();
        ambiente.setId(10L);
        ambiente.setAtivo(true);

        when(ambienteRepository.findByIdAndDataExclusaoIsNull(10L))
                .thenReturn(Optional.of(ambiente));

        ambienteService.excluir(10L);

        assertFalse(ambiente.getAtivo());
        assertNotNull(ambiente.getDataExclusao());
        verify(ambienteRepository).save(ambiente);
        verify(ambienteRepository, never()).deleteById(10L);
    }
}
