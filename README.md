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
curl -X POST http://localhost:8080/api/microsoft/device-login
```

Siga o código no navegador. Token fica em `.microsoft-graph-token.json`.

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
- `excelPath`: planilha local `.xlsx` (sem OneDrive)

Regras da coluna I:

- só preenche se estiver vazia
- `hours == 8` → manhã e tarde
- senão → 1ª atividade manhã; demais tarde
- ignora linhas com Total `0:00` (fim de semana)

## Docker Compose

```bash
cp .env.example .env
# edite .env com Jira + MICROSOFT_CLIENT_ID

docker compose pull
docker compose up -d
```

API em `http://localhost:8080`. Token Graph fica em `./data/`.

Login Microsoft (uma vez):

```bash
curl -X POST http://localhost:8080/api/microsoft/device-login
```

## Native

No Linux (CI self-hosted / servidor):

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=false
docker build -f src/main/docker/Dockerfile.native-micro -t oliveiraclaudio/relatorio-jira .
```

No macOS (AWT/POI) use container-build:

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

## CI

Push em `main` dispara build nativo no runner **self-hosted** e push para
`oliveiraclaudio/relatorio-jira` (tags `:latest` e `:${{ github.sha }}`).

Secrets no environment `DOCKERHUB`:

- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`
