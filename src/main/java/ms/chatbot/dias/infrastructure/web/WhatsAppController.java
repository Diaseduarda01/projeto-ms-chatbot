package ms.chatbot.dias.infrastructure.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ms.chatbot.dias.domain.port.CompanyRepository;
import ms.chatbot.dias.infrastructure.evolution.EvolutionConnectResult;
import ms.chatbot.dias.infrastructure.evolution.EvolutionHttpClient;
import ms.chatbot.dias.infrastructure.evolution.EvolutionInstanceSettings;
import ms.chatbot.dias.infrastructure.evolution.EvolutionWebhookConfig;
import ms.chatbot.dias.infrastructure.web.dto.CheckNumberRequest;
import ms.chatbot.dias.infrastructure.web.dto.CheckNumberResponse;
import ms.chatbot.dias.infrastructure.web.dto.InstanceSettingsRequest;
import ms.chatbot.dias.infrastructure.web.dto.InstanceSettingsResponse;
import ms.chatbot.dias.infrastructure.web.dto.PrivacySettingsResponse;
import ms.chatbot.dias.infrastructure.web.dto.ProativaMessageRequest;
import ms.chatbot.dias.infrastructure.web.dto.UpdatePrivacyRequest;
import ms.chatbot.dias.infrastructure.web.dto.UpdateProfileRequest;
import ms.chatbot.dias.infrastructure.web.dto.WebhookConfigRequest;
import ms.chatbot.dias.infrastructure.web.dto.WebhookConfigResponse;
import ms.chatbot.dias.infrastructure.web.dto.WhatsAppConnectResponse;
import ms.chatbot.dias.infrastructure.web.dto.WhatsAppProfileResponse;
import ms.chatbot.dias.infrastructure.web.dto.WhatsAppStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final CompanyRepository companyRepository;
    private final EvolutionHttpClient evolutionHttpClient;

    @Value("${chatbot.webhook-base-url:http://dias-chatbot:8085}")
    private String webhookBaseUrl;

    // ── Status & conexão ─────────────────────────────────────────────────────

    @GetMapping("/status")
    public ResponseEntity<WhatsAppStatusResponse> status(@RequestParam UUID companyId) {
        var company = findCompanyOrThrow(companyId);
        String state = evolutionHttpClient.getConnectionState(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey());
        return ResponseEntity.ok(new WhatsAppStatusResponse(state));
    }

    @PostMapping("/connect")
    public ResponseEntity<WhatsAppConnectResponse> connect(@RequestParam UUID companyId) {
        var company = findCompanyOrThrow(companyId);
        EvolutionConnectResult result = evolutionHttpClient.connectInstance(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey());
        // Configure webhook after instance creation (it may have been just created or re-created)
        String webhookUrl = webhookBaseUrl + "/webhook/" + company.getEvolutionInstanceName();
        evolutionHttpClient.setWebhook(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey(),
            webhookUrl, List.of("MESSAGES_UPSERT", "QRCODE_UPDATED", "CONNECTION_UPDATE"));
        return ResponseEntity.ok(new WhatsAppConnectResponse(result.connected(), result.base64(), result.pairingCode()));
    }

    @GetMapping("/qrcode")
    public ResponseEntity<WhatsAppConnectResponse> qrCode(@RequestParam UUID companyId) {
        var company = findCompanyOrThrow(companyId);
        EvolutionConnectResult result = evolutionHttpClient.fetchQrCode(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey());
        return ResponseEntity.ok(new WhatsAppConnectResponse(result.connected(), result.base64(), result.pairingCode()));
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect(@RequestParam UUID companyId) {
        var company = findCompanyOrThrow(companyId);
        evolutionHttpClient.disconnectInstance(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/restart")
    public ResponseEntity<Void> restart(@RequestParam UUID companyId) {
        var company = findCompanyOrThrow(companyId);
        evolutionHttpClient.restartInstance(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/instance")
    public ResponseEntity<Void> deleteInstance(@RequestParam UUID companyId) {
        var company = findCompanyOrThrow(companyId);
        evolutionHttpClient.deleteInstance(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey());
        return ResponseEntity.noContent().build();
    }

    // ── Webhook ───────────────────────────────────────────────────────────────

    @GetMapping("/webhook")
    public ResponseEntity<WebhookConfigResponse> getWebhook(@RequestParam UUID companyId) {
        var company = findCompanyOrThrow(companyId);
        EvolutionWebhookConfig cfg = evolutionHttpClient.getWebhook(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey());
        String defaultUrl = webhookBaseUrl + "/webhook/" + company.getEvolutionInstanceName();
        return ResponseEntity.ok(new WebhookConfigResponse(
            cfg.url() != null ? cfg.url() : defaultUrl,
            cfg.enabled(),
            cfg.events()
        ));
    }

    @PostMapping("/webhook")
    public ResponseEntity<WebhookConfigResponse> setWebhook(
            @RequestParam UUID companyId,
            @RequestBody WebhookConfigRequest request) {
        var company = findCompanyOrThrow(companyId);
        String url = (request.url() != null && !request.url().isBlank())
            ? request.url()
            : webhookBaseUrl + "/webhook/" + company.getEvolutionInstanceName();
        List<String> events = (request.events() != null && !request.events().isEmpty())
            ? request.events()
            : List.of("MESSAGES_UPSERT", "QRCODE_UPDATED", "CONNECTION_UPDATE");
        EvolutionWebhookConfig cfg = evolutionHttpClient.setWebhook(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey(), url, events);
        return ResponseEntity.ok(new WebhookConfigResponse(cfg.url(), cfg.enabled(), cfg.events()));
    }

    // ── Configurações da instância ────────────────────────────────────────────

    @GetMapping("/settings")
    public ResponseEntity<InstanceSettingsResponse> getSettings(@RequestParam UUID companyId) {
        var company = findCompanyOrThrow(companyId);
        EvolutionInstanceSettings s = evolutionHttpClient.getSettings(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey());
        return ResponseEntity.ok(new InstanceSettingsResponse(s.rejectCall(), s.msgCall(), s.groupsIgnore()));
    }

    @PostMapping("/settings")
    public ResponseEntity<InstanceSettingsResponse> setSettings(
            @RequestParam UUID companyId,
            @RequestBody @Valid InstanceSettingsRequest request) {
        var company = findCompanyOrThrow(companyId);
        EvolutionInstanceSettings s = evolutionHttpClient.setSettings(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey(),
            request.rejectCall(), request.msgCall(), request.groupsIgnore());
        return ResponseEntity.ok(new InstanceSettingsResponse(s.rejectCall(), s.msgCall(), s.groupsIgnore()));
    }

    // ── Verificação de número ─────────────────────────────────────────────────

    @PostMapping("/check-number")
    public ResponseEntity<CheckNumberResponse> checkNumber(
            @RequestParam UUID companyId,
            @RequestBody @Valid CheckNumberRequest request) {
        var company = findCompanyOrThrow(companyId);
        boolean exists = evolutionHttpClient.checkNumber(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey(), request.number());
        return ResponseEntity.ok(new CheckNumberResponse(request.number(), exists));
    }

    // ── Perfil do WhatsApp ────────────────────────────────────────────────────

    @GetMapping("/profile")
    public ResponseEntity<WhatsAppProfileResponse> getProfile(
            @RequestParam UUID companyId,
            @RequestParam String number) {
        var company = findCompanyOrThrow(companyId);
        Map<String, Object> profile = evolutionHttpClient.getProfile(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey(), number);
        String pictureUrl = (String) profile.getOrDefault("picture",
            profile.get("profilePictureUrl"));
        return ResponseEntity.ok(new WhatsAppProfileResponse(
            (String) profile.get("name"),
            (String) profile.get("status"),
            pictureUrl
        ));
    }

    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(
            @RequestParam UUID companyId,
            @RequestBody UpdateProfileRequest request) {
        var company = findCompanyOrThrow(companyId);
        String instanceName = company.getEvolutionInstanceName();
        String apiKey = company.getEvolutionApiKey();
        if (request.name() != null && !request.name().isBlank()) {
            evolutionHttpClient.updateProfileName(instanceName, apiKey, request.name());
        }
        if (request.status() != null && !request.status().isBlank()) {
            evolutionHttpClient.updateProfileStatus(instanceName, apiKey, request.status());
        }
        if (request.pictureUrl() != null && !request.pictureUrl().isBlank()) {
            evolutionHttpClient.updateProfilePicture(instanceName, apiKey, request.pictureUrl());
        }
        return ResponseEntity.noContent().build();
    }

    // ── Chats e contatos ──────────────────────────────────────────────────────

    @GetMapping("/chats")
    public ResponseEntity<List<Map<String, Object>>> findChats(@RequestParam UUID companyId) {
        var company = findCompanyOrThrow(companyId);
        List<Map<String, Object>> chats = evolutionHttpClient.findChats(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey());
        return ResponseEntity.ok(chats);
    }

    @PostMapping("/contacts")
    public ResponseEntity<List<Map<String, Object>>> findContacts(
            @RequestParam UUID companyId,
            @RequestParam(required = false) String query) {
        var company = findCompanyOrThrow(companyId);
        List<Map<String, Object>> contacts = evolutionHttpClient.findContacts(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey(), query);
        return ResponseEntity.ok(contacts);
    }

    // ── Mensagem proativa ─────────────────────────────────────────────────────

    @PostMapping("/mensagem")
    public ResponseEntity<Void> enviarMensagemProativa(
            @RequestParam UUID companyId,
            @RequestBody @Valid ProativaMessageRequest request) {
        var company = findCompanyOrThrow(companyId);
        evolutionHttpClient.sendText(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey(),
            request.number(), request.texto());
        return ResponseEntity.noContent().build();
    }

    // ── Privacidade ───────────────────────────────────────────────────────────

    @GetMapping("/privacy")
    public ResponseEntity<PrivacySettingsResponse> getPrivacy(@RequestParam UUID companyId) {
        var company = findCompanyOrThrow(companyId);
        Map<String, Object> p = evolutionHttpClient.getPrivacy(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey());
        return ResponseEntity.ok(new PrivacySettingsResponse(
            (String) p.get("readreceipts"),
            (String) p.get("profile"),
            (String) p.get("status"),
            (String) p.get("online"),
            (String) p.get("last"),
            (String) p.get("groupadd")
        ));
    }

    @PutMapping("/privacy")
    public ResponseEntity<Void> updatePrivacy(
            @RequestParam UUID companyId,
            @RequestBody UpdatePrivacyRequest request) {
        var company = findCompanyOrThrow(companyId);
        var settings = new LinkedHashMap<String, String>();
        if (request.readreceipts() != null) settings.put("readreceipts", request.readreceipts());
        if (request.profile() != null)      settings.put("profile", request.profile());
        if (request.status() != null)       settings.put("status", request.status());
        if (request.online() != null)       settings.put("online", request.online());
        if (request.last() != null)         settings.put("last", request.last());
        if (request.groupadd() != null)     settings.put("groupadd", request.groupadd());
        evolutionHttpClient.setPrivacy(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey(), settings);
        return ResponseEntity.noContent().build();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ms.chatbot.dias.domain.entity.Company findCompanyOrThrow(UUID companyId) {
        return companyRepository.findById(companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Empresa não encontrada: " + companyId));
    }
}
