package ms.chatbot.dias.infrastructure.evolution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ms.chatbot.dias.infrastructure.config.EvolutionProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EvolutionHttpClient {

    private final RestClient restClient;
    private final EvolutionProperties properties;

    public String getConnectionState(String instanceName, String apiKey) {
        String url = properties.getBaseUrl() + "/instance/connectionState/" + instanceName;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .retrieve()
                .body(Map.class);
            if (response == null) return "close";
            @SuppressWarnings("unchecked")
            Map<String, Object> instance = (Map<String, Object>) response.get("instance");
            return instance != null ? (String) instance.get("state") : "close";
        } catch (Exception e) {
            log.warn("Erro ao consultar estado da instância {}: {}", instanceName, e.getMessage());
            return "close";
        }
    }

    public EvolutionConnectResult connectInstance(String instanceName, String apiKey) {
        ensureInstanceExists(instanceName, apiKey);
        return fetchQrCode(instanceName, apiKey);
    }

    /** Busca QR sem reconectar — usa apenas se instância já existir. */
    public EvolutionConnectResult fetchQrCode(String instanceName, String apiKey) {
        String url = properties.getBaseUrl() + "/instance/connect/" + instanceName;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .retrieve()
                .body(Map.class);
            if (response == null) return new EvolutionConnectResult(false, null, null);
            if (response.containsKey("base64")) {
                return new EvolutionConnectResult(false, (String) response.get("base64"), (String) response.get("code"));
            }
            return new EvolutionConnectResult(true, null, null);
        } catch (Exception e) {
            log.warn("Erro ao buscar QR da instância {}: {}", instanceName, e.getMessage());
            return new EvolutionConnectResult(false, null, null);
        }
    }

    public void restartInstance(String instanceName, String apiKey) {
        String url = properties.getBaseUrl() + "/instance/restart/" + instanceName;
        try {
            restClient.put()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .retrieve()
                .toBodilessEntity();
            log.info("Instância {} reiniciada", instanceName);
        } catch (Exception e) {
            log.warn("Erro ao reiniciar instância {}: {}", instanceName, e.getMessage());
        }
    }

    public void deleteInstance(String instanceName, String apiKey) {
        String url = properties.getBaseUrl() + "/instance/delete/" + instanceName;
        try {
            restClient.delete()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .retrieve()
                .toBodilessEntity();
            log.info("Instância {} deletada", instanceName);
        } catch (Exception e) {
            log.warn("Erro ao deletar instância {}: {}", instanceName, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public EvolutionInstanceSettings getSettings(String instanceName, String apiKey) {
        String url = properties.getBaseUrl() + "/settings/find/" + instanceName;
        try {
            Map<String, Object> response = restClient.get()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .retrieve()
                .body(Map.class);
            if (response == null) return new EvolutionInstanceSettings(false, "", true);
            Boolean rejectCall = (Boolean) response.getOrDefault("reject_call", false);
            String msgCall = (String) response.getOrDefault("msg_call", "");
            Boolean groupsIgnore = (Boolean) response.getOrDefault("groups_ignore", true);
            return new EvolutionInstanceSettings(
                Boolean.TRUE.equals(rejectCall),
                msgCall != null ? msgCall : "",
                Boolean.TRUE.equals(groupsIgnore)
            );
        } catch (Exception e) {
            log.warn("Erro ao buscar settings da instância {}: {}", instanceName, e.getMessage());
            return new EvolutionInstanceSettings(false, "", true);
        }
    }

    public EvolutionInstanceSettings setSettings(String instanceName, String apiKey,
                                                  boolean rejectCall, String msgCall, boolean groupsIgnore) {
        String url = properties.getBaseUrl() + "/settings/set/" + instanceName;
        try {
            var body = Map.of(
                "reject_call", rejectCall,
                "msg_call", msgCall != null ? msgCall : "",
                "groups_ignore", groupsIgnore
            );
            restClient.post()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
            log.info("Settings da instância {} atualizados", instanceName);
            return new EvolutionInstanceSettings(rejectCall, msgCall, groupsIgnore);
        } catch (Exception e) {
            log.warn("Erro ao salvar settings da instância {}: {}", instanceName, e.getMessage());
            return new EvolutionInstanceSettings(rejectCall, msgCall, groupsIgnore);
        }
    }

    private void ensureInstanceExists(String instanceName, String apiKey) {
        String createUrl = properties.getBaseUrl() + "/instance/create";
        try {
            var body = Map.of(
                "instanceName", instanceName,
                "qrcode", true,
                "integration", "WHATSAPP-BAILEYS"
            );
            restClient.post()
                .uri(createUrl)
                .header("apikey", resolveApiKey(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
            log.info("Instância {} criada na Evolution API", instanceName);
        } catch (Exception e) {
            // 409 = já existe, qualquer outro erro logamos mas seguimos
            log.debug("ensureInstanceExists {}: {}", instanceName, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public EvolutionWebhookConfig getWebhook(String instanceName, String apiKey) {
        String url = properties.getBaseUrl() + "/webhook/find/" + instanceName;
        try {
            Map<String, Object> response = restClient.get()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .retrieve()
                .body(Map.class);
            if (response == null) return new EvolutionWebhookConfig(null, false, List.of());
            String webhookUrl = (String) response.get("url");
            Boolean enabled = (Boolean) response.getOrDefault("enabled", false);
            List<String> events = (List<String>) response.getOrDefault("events", List.of());
            return new EvolutionWebhookConfig(webhookUrl, Boolean.TRUE.equals(enabled), events);
        } catch (Exception e) {
            log.warn("Erro ao buscar webhook da instância {}: {}", instanceName, e.getMessage());
            return new EvolutionWebhookConfig(null, false, List.of());
        }
    }

    public EvolutionWebhookConfig setWebhook(String instanceName, String apiKey,
                                              String webhookUrl, List<String> events) {
        String url = properties.getBaseUrl() + "/webhook/set/" + instanceName;
        try {
            // Evolution API v1.4 expects data nested under "webhook" key
            var webhookBody = Map.of(
                "url", webhookUrl,
                "enabled", true,
                "byEvents", false,
                "base64", false,
                "events", events
            );
            var body = Map.of("webhook", webhookBody);
            restClient.post()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
            log.info("Webhook configurado para instância {}: {}", instanceName, webhookUrl);
            return new EvolutionWebhookConfig(webhookUrl, true, events);
        } catch (Exception e) {
            log.warn("Erro ao configurar webhook da instância {}: {}", instanceName, e.getMessage());
            return new EvolutionWebhookConfig(webhookUrl, false, events);
        }
    }

    public void disconnectInstance(String instanceName, String apiKey) {
        String url = properties.getBaseUrl() + "/instance/logout/" + instanceName;
        try {
            restClient.delete()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Erro ao desconectar instância {}: {}", instanceName, e.getMessage());
        }
    }

    public void sendText(String instanceName, String apiKey, String phoneNumber, String text) {
        String url = properties.getBaseUrl() + "/message/sendText/" + instanceName;
        String key = resolveApiKey(apiKey);

        var body = Map.of(
            "number", sanitizeNumber(phoneNumber),
            "text", text,
            "options", Map.of("delay", 1200, "presence", "composing")
        );

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                restClient.post()
                    .uri(url)
                    .header("apikey", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
                return;
            } catch (Exception e) {
                log.warn("Tentativa {}/{} falhou para {}: {}", attempt, maxRetries, phoneNumber, e.getMessage());
                if (attempt < maxRetries) sleep(500L * attempt);
            }
        }

        log.error("Falha ao enviar mensagem para {} após {} tentativas", phoneNumber, maxRetries);
    }

    public boolean checkNumber(String instanceName, String apiKey, String number) {
        String url = properties.getBaseUrl() + "/chat/whatsappNumbers/" + instanceName;
        try {
            var body = Map.of("numbers", List.of(sanitizeNumber(number)));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> response = restClient.post()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(List.class);
            if (response == null || response.isEmpty()) return false;
            return Boolean.TRUE.equals(response.get(0).get("numberExists"));
        } catch (Exception e) {
            log.warn("Erro ao verificar número {}: {}", number, e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getProfile(String instanceName, String apiKey, String number) {
        String url = properties.getBaseUrl() + "/chat/fetchProfile/" + instanceName;
        try {
            var body = Map.of("number", sanitizeNumber(number));
            Map<String, Object> response = restClient.post()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
            return response != null ? response : Map.of();
        } catch (Exception e) {
            log.warn("Erro ao buscar perfil da instância {}: {}", instanceName, e.getMessage());
            return Map.of();
        }
    }

    public void updateProfileName(String instanceName, String apiKey, String name) {
        String url = properties.getBaseUrl() + "/chat/updateProfileName/" + instanceName;
        try {
            restClient.post()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", name))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Erro ao atualizar nome do perfil {}: {}", instanceName, e.getMessage());
        }
    }

    public void updateProfileStatus(String instanceName, String apiKey, String status) {
        String url = properties.getBaseUrl() + "/chat/updateProfileStatus/" + instanceName;
        try {
            restClient.post()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("status", status))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Erro ao atualizar status do perfil {}: {}", instanceName, e.getMessage());
        }
    }

    public void updateProfilePicture(String instanceName, String apiKey, String pictureUrl) {
        String url = properties.getBaseUrl() + "/chat/updateProfilePicture/" + instanceName;
        try {
            restClient.put()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("picture", pictureUrl))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Erro ao atualizar foto do perfil {}: {}", instanceName, e.getMessage());
        }
    }

    public void sendMedia(String instanceName, String apiKey, String phoneNumber,
                          String mediaType, String media, String caption, String fileName) {
        String url = properties.getBaseUrl() + "/message/sendMedia/" + instanceName;
        var mediaMessage = new java.util.LinkedHashMap<String, Object>();
        mediaMessage.put("mediatype", mediaType);
        mediaMessage.put("media", media);
        if (caption != null && !caption.isBlank()) mediaMessage.put("caption", caption);
        if (fileName != null && !fileName.isBlank()) mediaMessage.put("fileName", fileName);
        var body = Map.of(
            "number", sanitizeNumber(phoneNumber),
            "options", Map.of("delay", 1200, "presence", "composing"),
            "mediaMessage", mediaMessage
        );
        try {
            restClient.post()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Erro ao enviar mídia para {}: {}", phoneNumber, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findChats(String instanceName, String apiKey) {
        String url = properties.getBaseUrl() + "/chat/findChats/" + instanceName;
        try {
            List<Map<String, Object>> response = restClient.get()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .retrieve()
                .body(List.class);
            return response != null ? response : List.of();
        } catch (Exception e) {
            log.warn("Erro ao buscar chats da instância {}: {}", instanceName, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findContacts(String instanceName, String apiKey, String query) {
        String url = properties.getBaseUrl() + "/chat/findContacts/" + instanceName;
        try {
            var where = new java.util.LinkedHashMap<String, Object>();
            if (query != null && !query.isBlank()) where.put("pushName", query);
            List<Map<String, Object>> response = restClient.post()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("where", where))
                .retrieve()
                .body(List.class);
            return response != null ? response : List.of();
        } catch (Exception e) {
            log.warn("Erro ao buscar contatos da instância {}: {}", instanceName, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getPrivacy(String instanceName, String apiKey) {
        String url = properties.getBaseUrl() + "/chat/fetchPrivacySettings/" + instanceName;
        try {
            Map<String, Object> response = restClient.get()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .retrieve()
                .body(Map.class);
            return response != null ? response : Map.of();
        } catch (Exception e) {
            log.warn("Erro ao buscar privacidade da instância {}: {}", instanceName, e.getMessage());
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> setPrivacy(String instanceName, String apiKey,
                                           Map<String, String> settings) {
        String url = properties.getBaseUrl() + "/chat/updatePrivacySettings/" + instanceName;
        try {
            Map<String, Object> response = restClient.put()
                .uri(url)
                .header("apikey", resolveApiKey(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("privacySettings", settings))
                .retrieve()
                .body(Map.class);
            return response != null ? response : Map.of();
        } catch (Exception e) {
            log.warn("Erro ao atualizar privacidade da instância {}: {}", instanceName, e.getMessage());
            return Map.of();
        }
    }

    private String resolveApiKey(String instanceApiKey) {
        return (instanceApiKey != null && !instanceApiKey.isBlank())
            ? instanceApiKey
            : properties.getGlobalApiKey();
    }

    private String sanitizeNumber(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
