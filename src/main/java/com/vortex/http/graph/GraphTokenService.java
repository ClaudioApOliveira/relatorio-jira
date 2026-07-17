package com.vortex.http.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vortex.http.graph.dto.DeviceCodeResponse;
import com.vortex.http.graph.dto.DeviceLoginResult;
import com.vortex.http.graph.dto.GraphTokenResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

/**
 * Tokens Microsoft Graph.
 * <ul>
 *   <li>{@code client-credentials} — SharePoint / OneDrive for Business (app)</li>
 *   <li>{@code delegated} — OneDrive pessoal (usuário autentica via device code)</li>
 * </ul>
 */
@ApplicationScoped
public class GraphTokenService {

    private static final Logger LOG = Logger.getLogger(GraphTokenService.class);
    private static final String DELEGATED_SCOPE =
            "offline_access openid profile User.Read Files.ReadWrite";

    @Inject
    @RestClient
    MsLoginApi loginApi;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "microsoft.auth-mode", defaultValue = "delegated")
    String authMode;

    /** Tenant do app-only (SharePoint corporativo). */
    @ConfigProperty(name = "microsoft.tenant-id")
    Optional<String> tenantId;

    /** Tenant do login delegado. Personal only → consumers; org+personal → common. */
    @ConfigProperty(name = "microsoft.delegated-tenant-id", defaultValue = "consumers")
    String delegatedTenantId;

    @ConfigProperty(name = "microsoft.client-id")
    Optional<String> clientId;

    @ConfigProperty(name = "microsoft.client-secret")
    Optional<String> clientSecret;

    @ConfigProperty(name = "microsoft.token-store", defaultValue = ".microsoft-graph-token.json")
    String tokenStorePath;

    private volatile String cachedAccessToken;
    private volatile String cachedRefreshToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public String bearerToken() {
        if ("delegated".equalsIgnoreCase(authMode)) {
            return "Bearer " + delegatedAccessToken();
        }
        return "Bearer " + clientCredentialsAccessToken();
    }

    public boolean isDelegated() {
        return "delegated".equalsIgnoreCase(authMode);
    }

    public boolean hasDelegatedSession() {
        loadRefreshFromDisk();
        return cachedRefreshToken != null && !cachedRefreshToken.isBlank();
    }

    /**
     * Inicia device code + espera o usuário autorizar no browser.
     * Use para OneDrive pessoal.
     */
    public DeviceLoginResult loginWithDeviceCode() {
        String tenant = tenantForDelegated();
        String id = require(clientId, "microsoft.client-id / MICROSOFT_CLIENT_ID");
        LOG.infof("Device login: tenant=%s clientId=%s...", tenant, id.substring(0, Math.min(8, id.length())));

        // Device code = public client: NÃO enviar client_secret
        String deviceBody = "client_id=" + enc(id)
                + "&scope=" + enc(DELEGATED_SCOPE);

        DeviceCodeResponse device;
        try {
            device = loginApi.deviceCode(tenant, deviceBody);
        } catch (WebApplicationException e) {
            String err = readErrorBody(e);
            throw new IllegalStateException(
                    "Device code 401/erro. Checklist: "
                            + "(1) MICROSOFT_CLIENT_ID = Application (client) ID da app 'quarkus personal' "
                            + "(NÃO use o ID da app corporativa); "
                            + "(2) Authentication → Allow public client flows = Yes; "
                            + "(3) Supported accounts = Personal accounts only; "
                            + "(4) microsoft.delegated-tenant-id=consumers. "
                            + "Detalhe Azure: " + err,
                    e
            );
        }
        if (device == null || device.deviceCode() == null) {
            throw new IllegalStateException("Falha ao iniciar device code Microsoft");
        }

        String message = device.message() != null
                ? device.message()
                : "Abra " + device.verificationUri() + " e informe o código " + device.userCode();

        LOG.infof("Microsoft device login: %s", message);

        long intervalSec = device.interval() != null ? Math.max(3, device.interval()) : 5;
        long deadline = System.currentTimeMillis()
                + (device.expiresIn() != null ? device.expiresIn() * 1000L : 900_000L);

        while (System.currentTimeMillis() < deadline) {
            sleepSeconds(intervalSec);

            String tokenBody = "grant_type=" + enc("urn:ietf:params:oauth:grant-type:device_code")
                    + "&client_id=" + enc(id)
                    + "&device_code=" + enc(device.deviceCode());

            try {
                GraphTokenResponse token = loginApi.token(tenant, tokenBody);
                if (token != null && token.accessToken() != null) {
                    storeTokens(token);
                    return new DeviceLoginResult(
                            true,
                            message,
                            device.userCode(),
                            device.verificationUri(),
                            "Login concluído. Já pode usar excelOnline com OneDrive pessoal."
                    );
                }
            } catch (WebApplicationException e) {
                String err = readErrorBody(e);
                if (err != null && (err.contains("authorization_pending")
                        || err.contains("slow_down"))) {
                    if (err.contains("slow_down")) {
                        intervalSec += 2;
                    }
                    continue;
                }
                throw new IllegalStateException("Falha no device login Microsoft: " + err, e);
            }
        }
        throw new IllegalStateException("Tempo esgotado aguardando autorização no device login");
    }

    private String delegatedAccessToken() {
        Instant now = Instant.now();
        if (cachedAccessToken != null && now.isBefore(expiresAt)) {
            return cachedAccessToken;
        }
        synchronized (this) {
            if (cachedAccessToken != null && Instant.now().isBefore(expiresAt)) {
                return cachedAccessToken;
            }
            loadRefreshFromDisk();
            if (cachedRefreshToken == null || cachedRefreshToken.isBlank()) {
                throw new IllegalStateException(
                        "OneDrive pessoal exige login delegado. "
                                + "Chame POST /api/microsoft/device-login e autorize no browser "
                                + "(código exibido na resposta)."
                );
            }
            refreshDelegatedToken();
            return cachedAccessToken;
        }
    }

    private void refreshDelegatedToken() {
        String tenant = tenantForDelegated();
        String id = require(clientId, "microsoft.client-id / MICROSOFT_CLIENT_ID");
        String body = "grant_type=refresh_token"
                + "&client_id=" + enc(id)
                + "&refresh_token=" + enc(cachedRefreshToken)
                + "&scope=" + enc(DELEGATED_SCOPE);
        // refresh também sem secret (public client / device code)
        try {
            GraphTokenResponse token = loginApi.token(tenant, body);
            if (token == null || token.accessToken() == null) {
                throw new IllegalStateException("Refresh token Microsoft inválido; rode /api/microsoft/device-login de novo");
            }
            storeTokens(token);
        } catch (WebApplicationException e) {
            cachedRefreshToken = null;
            deleteTokenStore();
            throw new IllegalStateException(
                    "Sessão Microsoft expirada. Chame POST /api/microsoft/device-login novamente. "
                            + "Detalhe: " + readErrorBody(e),
                    e
            );
        }
    }

    private String clientCredentialsAccessToken() {
        Instant now = Instant.now();
        if (cachedAccessToken != null && now.isBefore(expiresAt)) {
            return cachedAccessToken;
        }
        synchronized (this) {
            if (cachedAccessToken != null && Instant.now().isBefore(expiresAt)) {
                return cachedAccessToken;
            }
            String tenant = require(tenantId, "microsoft.tenant-id / MICROSOFT_TENANT_ID");
            String id = require(clientId, "microsoft.client-id / MICROSOFT_CLIENT_ID");
            String secret = require(clientSecret, "microsoft.client-secret / MICROSOFT_CLIENT_SECRET");

            String formBody = "client_id=" + enc(id)
                    + "&client_secret=" + enc(secret)
                    + "&scope=" + enc("https://graph.microsoft.com/.default")
                    + "&grant_type=client_credentials";

            try {
                GraphTokenResponse response = loginApi.token(tenant, formBody);
                if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                    throw new IllegalStateException("Falha ao obter access_token do Microsoft Graph");
                }
                storeAccessOnly(response);
                return cachedAccessToken;
            } catch (WebApplicationException e) {
                String body = readErrorBody(e);
                LOG.errorf("Microsoft token erro: %s", body);
                throw new IllegalStateException(
                        "Falha no login Microsoft Graph (client credentials). Detalhe: " + body,
                        e
                );
            }
        }
    }

    private void storeTokens(GraphTokenResponse token) {
        cachedAccessToken = token.accessToken();
        if (token.refreshToken() != null && !token.refreshToken().isBlank()) {
            cachedRefreshToken = token.refreshToken();
        }
        long expiresIn = token.expiresIn() != null ? token.expiresIn() : 3600L;
        expiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 120));
        persistRefreshToDisk();
    }

    private void storeAccessOnly(GraphTokenResponse token) {
        cachedAccessToken = token.accessToken();
        long expiresIn = token.expiresIn() != null ? token.expiresIn() : 3600L;
        expiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 120));
    }

    private void persistRefreshToDisk() {
        if (cachedRefreshToken == null) {
            return;
        }
        try {
            Path path = Path.of(tokenStorePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            // Evita DTO custom no ObjectMapper (native): serializa só String/árvore.
            var node = objectMapper.createObjectNode();
            node.put("refreshToken", cachedRefreshToken);
            node.put("savedAt", Instant.now().toString());
            Files.writeString(path, objectMapper.writeValueAsString(node));
            LOG.infof("Token Microsoft gravado em %s", path.toAbsolutePath());
        } catch (Exception e) {
            LOG.errorf(e,
                    "Não foi possível gravar token store em %s. "
                            + "O volume precisa ser gravável pelo UID 1001 "
                            + "(compose: serviço relatorio-jira-init).",
                    tokenStorePath);
        }
    }

    private void loadRefreshFromDisk() {
        if (cachedRefreshToken != null) {
            return;
        }
        try {
            Path path = Path.of(tokenStorePath);
            if (!Files.isRegularFile(path)) {
                return;
            }
            var node = objectMapper.readTree(Files.readString(path));
            if (node != null && node.hasNonNull("refreshToken")) {
                cachedRefreshToken = node.get("refreshToken").asText();
            }
        } catch (Exception e) {
            LOG.warnf(e, "Não foi possível ler token store %s", tokenStorePath);
        }
    }

    private void deleteTokenStore() {
        try {
            Files.deleteIfExists(Path.of(tokenStorePath));
        } catch (Exception ignored) {
            // ignore
        }
    }

    private String tenantForDelegated() {
        if (delegatedTenantId != null && !delegatedTenantId.isBlank()) {
            return delegatedTenantId.trim();
        }
        return "consumers";
    }

    private static void sleepSeconds(long seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrompido no device login", e);
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String readErrorBody(RuntimeException e) {
        try {
            Response response = null;
            if (e instanceof ClientWebApplicationException cwae) {
                response = cwae.getResponse();
            } else if (e instanceof WebApplicationException wae) {
                response = wae.getResponse();
            }
            if (response != null && response.hasEntity()) {
                return response.readEntity(String.class);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return e.getMessage();
    }

    private static String require(Optional<String> value, String name) {
        return value.filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Config obrigatória ausente: " + name));
    }
}
