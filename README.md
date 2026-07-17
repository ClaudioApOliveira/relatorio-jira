# relatorio-jira

API Quarkus que coleta worklogs do Jira e preenche a coluna **I** (Descrição da atividade)
da planilha de horas no OneDrive (Excel Online).

## Pré-requisitos

Variáveis de ambiente:

| Variável | Uso |
|----------|-----|
| `JIRA_BASE_URL` | Ex.: `https://perprojetos.atlassian.net` |
| `JIRA_API_EMAIL` | E-mail da conta Jira |
| `JIRA_API_TOKEN` | API token Atlassian |
| `MICROSOFT_CLIENT_ID` | App Registration (contas pessoais) |

## Dev

```bash
./mvnw quarkus:dev
```

## Login Microsoft (OneDrive)

Uma vez por sessão / quando o token expirar:

```bash
curl -X POST http://192.168.2.144:8080/api/microsoft/device-login
```

Enquanto o `curl` espera, veja o código em `docker logs -f relatorio-jira` e autorize em
https://www.microsoft.com/link. Token persiste no volume `relatorio_jira_data`
(UID 1001 — o serviço `relatorio-jira-init` ajusta a permissão).

Confira: `curl http://192.168.2.144:8080/api/microsoft/auth-status` → `"hasSession": true`.

## Relatório + planilha

Arquivos são resolvidos **sempre** dentro de `VERTEXCODE LTDA`
(`microsoft.excel.onedrive-base-folder`).

```bash
curl -X POST http://localhost:8080/api/reports/tempo-gasto \
  -H 'Content-Type: application/json' \
  -d '{
    "months": "2026-07",
    "projects": "PERTI26",
    "author": "Cláudio",
    "excelOnline": true,
    "excelOneDrivePath": "2026/07-2026/PISOMTECH-PLANILHA-ATIVIDADES-PERBANK-CLAUDIO-07-2026.xlsx"
  }'
```

Alternativas:

- `excelFileName`: busca o nome exato sob `VERTEXCODE LTDA` (subpastas)
- `excelDriveId` + `excelItemId`: IDs Graph já conhecidos

Regras da coluna I:

- só preenche se estiver vazia
- `hours == 8` → manhã e tarde
- senão → 1ª atividade manhã; demais tarde
- ignora linhas com Total `0:00` (fim de semana)

## Docker Compose (n8n + relatorio-jira)

### Portainer
1. Stacks → Add stack → cole o `docker-compose.yml`
2. Em **Environment variables** do stack, adicione:

| Name | Value |
|------|--------|
| `JIRA_BASE_URL` | `https://perprojetos.atlassian.net` |
| `JIRA_API_EMAIL` | seu e-mail |
| `JIRA_API_TOKEN` | token Atlassian |
| `MICROSOFT_CLIENT_ID` | Client ID do app Azure |

3. Se a imagem GHCR for privada: Registries → add GHCR com token GitHub (`read:packages`)
4. Deploy the stack

Chamada **dentro do n8n**: `http://relatorio-jira:8080/api/reports/tempo-gasto`

### CLI (sem Portainer)

```bash
cp .env.example .env
docker compose up -d
```

## Native

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
docker build -f src/main/docker/Dockerfile.native-micro -t ghcr.io/claudioapoliveira/relatorio-jira:latest .
```

## CI

Um job no **ubuntu-latest**: testes + build nativo (`container-build`) + push no GHCR.
Evita self-hosted (Kali) por DNS/upload instáveis.

Imagem:

```text
ghcr.io/claudioapoliveira/relatorio-jira:latest
```

Após o primeiro push, deixe o pacote **público**:
GitHub → Packages → `relatorio-jira` → Package settings → Change visibility → Public.

Pull:

```bash
docker pull ghcr.io/claudioapoliveira/relatorio-jira:latest
```
