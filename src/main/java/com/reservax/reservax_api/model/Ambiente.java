package com.reservax.reservax_api.model;

import com.reservax.reservax_api.enums.TipoAmbiente;
import jakarta.persistence.*;

@Entity
@Table(name = "ambiente")
public class Ambiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAmbiente tipo;

    @Column(nullable = false)
    private Integer capacidade;

    private String localizacao;

    private String descricao;

    private boolean possuiProjetor;

    private boolean possuiTv;

    private boolean possuiMicrofone;

    private boolean possuiComputadores;

    private boolean ativo;

    public Ambiente() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoAmbiente getTipo() {
        return tipo;
    }

    public void setTipo(TipoAmbiente tipo) {
        this.tipo = tipo;
    }

    public Integer getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(Integer capacidade) {
        this.capacidade = capacidade;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public boolean getPossuiProjetor() {
        return possuiProjetor;
    }

    public void setPossuiProjetor(boolean possuiProjetor) {
        this.possuiProjetor = possuiProjetor;
    }

    public boolean getPossuiTv() {
        return possuiTv;
    }

    public void setPossuiTv(boolean possuiTv) {
        this.possuiTv = possuiTv;
    }

    public boolean getPossuiMicrofone() {
        return possuiMicrofone;
    }

    public void setPossuiMicrofone(boolean possuiMicrofone) {
        this.possuiMicrofone = possuiMicrofone;
    }

    public boolean getPossuiComputadores() {
        return possuiComputadores;
    }

    public void setPossuiComputadores(boolean possuiComputadores) {
        this.possuiComputadores = possuiComputadores;
    }

    public boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

}