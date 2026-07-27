# 📚 ReservaX

Sistema de gerenciamento de reservas de ambientes desenvolvido como projeto acadêmico do curso de Engenharia de Software.

O objetivo do sistema é facilitar a solicitação e administração de reservas de salas, laboratórios, auditórios e outros ambientes de uma instituição.

---

## 🚀 Tecnologias utilizadas

### Back-end

- Java 17
- Spring Boot
- Spring Data JPA
- Hibernate
- PostgreSQL
- Flyway
- Maven

### Front-end

- HTML5
- CSS3
- JavaScript
- Bootstrap

### Ferramentas

- Git
- GitHub
- IntelliJ IDEA
- VS Code

---

## ✨ Funcionalidades

### Ambientes

- Cadastro de ambientes
- Listagem de ambientes
- Alteração de ambientes
- Exclusão de ambientes
- Ativação/Inativação

Cada ambiente possui:

- Nome
- Tipo
- Capacidade
- Localização
- Descrição
- Recursos disponíveis
  - Projetor
  - TV
  - Microfone
  - Computadores

---

### Reservas

- Solicitação de reserva
- Fluxo de aprovação
- Controle de status da reserva

Fluxo:

Solicitada → Em análise → Aprovada → Confirmada → Finalizada

Também é possível:

- Cancelar reserva
- Rejeitar solicitação

---

## 🗄 Banco de dados

Banco utilizado:

PostgreSQL

Controle de versões do banco utilizando:

- Flyway

---

## 🔗 API REST

O projeto disponibiliza uma API REST construída com Spring Boot.

Exemplo de endpoints:

GET /ambientes

POST /ambientes

PUT /ambientes/{id}

DELETE /ambientes/{id}

---

## ▶ Como executar

### Clonar o projeto

```bash
git clone https://github.com/SEU-USUARIO/reservax-api.git
```

### Configurar o banco

Criar um banco PostgreSQL chamado:

```
reservax
```

Editar o arquivo:

```
application.properties
```

Informando:

```
spring.datasource.username=SEU_USUARIO
spring.datasource.password=SUA_SENHA
```

### Executar

```bash
./mvnw spring-boot:run
```

ou

```bash
mvn spring-boot:run
```

---

## 📷 Telas do sistema

Em breve serão adicionadas imagens do sistema.

---

## 📖 Aprendizados

Durante o desenvolvimento deste projeto foram praticados conceitos como:

- Programação Orientada a Objetos
- Arquitetura MVC
- APIs REST
- CRUD
- JPA/Hibernate
- Versionamento com Git
- Banco de dados relacional
- Migração de banco com Flyway
- Boas práticas de organização em camadas

---

## 👨‍💻 Desenvolvedor

Marcos Vinicios Florêncio de Oliveira

LinkedIn:
https://www.linkedin.com/in/marcos-vinicios/

GitHub:
https://github.com/bolozinjr
