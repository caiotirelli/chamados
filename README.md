# Sistema de Chamados — API REST

Projeto da disciplina DAS (UNIVILLE): API em arquitetura em camadas com Java 21 e Spring Boot.

Camadas: `controller` → `service` → `repository` → `entity`, com `dto` como contrato da API
e `exception` + `GlobalExceptionHandler` padronizando os erros HTTP.

## Como executar

```powershell
.\mvnw spring-boot:run          # Windows
./mvnw spring-boot:run          # macOS / Linux
```

- API: http://localhost:8080
- Swagger UI (OpenAPI): http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console — JDBC URL `jdbc:h2:mem:chamadosdb`, usuário `sa`, senha em branco

Testes automatizados:

```powershell
.\mvnw test
```

### PostgreSQL (profile `postgres`)

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/chamados"; $env:DB_USER="postgres"; $env:DB_PASSWORD="postgres"
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

## Rotas

| Método | Rota | Ação | Sucesso |
|---|---|---|---|
| POST | /clientes | Criar cliente | 201 |
| GET | /clientes | Listar | 200 |
| GET | /clientes/{id} | Buscar | 200 |
| PUT | /clientes/{id} | Atualizar | 200 |
| DELETE | /clientes/{id} | Excluir (409 se tiver chamados) | 204 |
| GET | /clientes/{id}/chamados | Chamados do cliente (paginado) | 200 |
| POST / GET / GET{id} / PUT / DELETE | /categorias | CRUD de categorias (nome único) | 201/200/204 |
| POST / GET / GET{id} / PUT / DELETE | /tecnicos | CRUD de técnicos (e-mail único, perfil ADMIN/TECNICO) | 201/200/204 |
| POST | /chamados | Abrir chamado (nasce ABERTO) | 201 |
| GET | /chamados | Listar com filtros e paginação | 200 |
| GET | /chamados/{id} | Detalhe + comentários | 200 |
| PUT | /chamados/{id} | Atualizar (409 se FECHADO) | 200 |
| DELETE | /chamados/{id} | Excluir (remove comentários) | 204 |
| PATCH | /chamados/{id}/atribuir | Atribuir técnico | 200 |
| PATCH | /chamados/{id}/status | Alterar status | 200 |
| POST | /chamados/{id}/comentarios | Comentar | 201 |
| GET | /chamados/{id}/comentarios | Listar comentários | 200 |

## Regras de negócio

- Chamado nasce `ABERTO` e `dataAbertura` é preenchida automaticamente.
- Transições de status permitidas:
  - `ABERTO` → `EM_ATENDIMENTO` | `FECHADO`
  - `EM_ATENDIMENTO` → `AGUARDANDO_CLIENTE` | `FECHADO`
  - `AGUARDANDO_CLIENTE` → `EM_ATENDIMENTO` | `FECHADO`
  - `FECHADO` → nenhuma
- `EM_ATENDIMENTO` exige técnico atribuído. Atribuir técnico a um chamado `ABERTO` o coloca em `EM_ATENDIMENTO`.
- Ao fechar, `dataFechamento` é preenchida.
- Chamado `FECHADO` não pode ser editado, receber técnico, mudar de status nem receber comentários (409).
- Cliente, categoria ou técnico vinculados a chamados não podem ser excluídos (409).

## Códigos de erro

| Status | Quando |
|---|---|
| 400 | Bean Validation falhou, JSON inválido, enum inexistente ou parâmetro com tipo errado |
| 404 | Recurso não encontrado |
| 409 | Regra de negócio violada (duplicidade, transição inválida, chamado fechado, exclusão com vínculo) |

Formato padrão:

```json
{ "momento": "2026-10-28T21:15:46", "status": 409, "erro": "Já existe cliente com o e-mail ana@exemplo.com", "detalhes": [] }
```

## Exemplos de teste (curl)

```bash
# Cliente, categoria e técnico
curl -i -X POST http://localhost:8080/clientes -H "Content-Type: application/json" \
  -d '{"nome":"Ana Souza","email":"ana@exemplo.com","telefone":"47999999999"}'
curl -i -X POST http://localhost:8080/categorias -H "Content-Type: application/json" \
  -d '{"nome":"Hardware","descricao":"Problemas físicos"}'
curl -i -X POST http://localhost:8080/tecnicos -H "Content-Type: application/json" \
  -d '{"nome":"Carlos","email":"carlos@empresa.com","perfil":"TECNICO"}'

# Abrir chamado
curl -i -X POST http://localhost:8080/chamados -H "Content-Type: application/json" \
  -d '{"titulo":"PC não liga","descricao":"Sem energia","prioridade":"ALTA","clienteId":1,"categoriaId":1}'

# Atribuir técnico, mudar status, comentar
curl -i -X PATCH http://localhost:8080/chamados/1/atribuir -H "Content-Type: application/json" -d '{"tecnicoId":1}'
curl -i -X PATCH http://localhost:8080/chamados/1/status -H "Content-Type: application/json" -d '{"status":"AGUARDANDO_CLIENTE"}'
curl -i -X POST http://localhost:8080/chamados/1/comentarios -H "Content-Type: application/json" \
  -d '{"texto":"Cliente vai verificar a tomada","autor":"Carlos"}'
curl -i -X PATCH http://localhost:8080/chamados/1/status -H "Content-Type: application/json" -d '{"status":"FECHADO"}'
```

### Exemplos de consulta (filtros e paginação)

```bash
curl -i "http://localhost:8080/chamados?status=ABERTO"
curl -i "http://localhost:8080/chamados?prioridade=ALTA&clienteId=1"
curl -i "http://localhost:8080/chamados?status=EM_ATENDIMENTO&prioridade=CRITICA"
curl -i "http://localhost:8080/chamados?page=0&size=5&sort=prioridade,desc"
curl -i "http://localhost:8080/clientes/1/chamados?page=0&size=10"
```

Padrão de paginação: `size=10`, ordenado por `dataAbertura` decrescente. Resposta:
`{ "content": [...], "page": { "size": 10, "number": 0, "totalElements": 3, "totalPages": 1 } }`.

## Modelo de dados

`CLIENTES 1:N CHAMADOS`, `CATEGORIAS 1:N CHAMADOS`, `TECNICOS 1:N CHAMADOS` (técnico opcional),
`CHAMADOS 1:N COMENTARIOS`.
