package ms.chatbot.dias.infrastructure.web;

import lombok.RequiredArgsConstructor;
import ms.chatbot.dias.domain.port.CompanyRepository;
import ms.chatbot.dias.infrastructure.evolution.EvolutionConnectResult;
import ms.chatbot.dias.infrastructure.evolution.EvolutionHttpClient;
import ms.chatbot.dias.infrastructure.evolution.EvolutionWebhookConfig;
import ms.chatbot.dias.infrastructure.web.dto.WhatsAppConnectResponse;
import ms.chatbot.dias.infrastructure.web.dto.WhatsAppStatusResponse;
import ms.chatbot.dias.infrastructure.web.dto.WebhookConfigRequest;
import ms.chatbot.dias.infrastructure.web.dto.WebhookConfigResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final CompanyRepository companyRepository;
    private final EvolutionHttpClient evolutionHttpClient;

    @Value("${chatbot.webhook-base-url:http://dias-chatbot:8085}")
    private String webhookBaseUrl;

    @GetMapping("/status")
    public ResponseEntity<WhatsAppStatusResponse> status(@RequestParam UUID companyId) {
        var company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Empresa não encontrada: " + companyId));
        String state = evolutionHttpClient.getConnectionState(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey());
        return ResponseEntity.ok(new WhatsAppStatusResponse(state));
    }

    @PostMapping("/connect")
    public ResponseEntity<WhatsAppConnectResponse> connect(@RequestParam UUID companyId) {
        var company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Empresa não encontrada: " + companyId));
        EvolutionConnectResult result = evolutionHttpClient.connectInstance(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey());
        return ResponseEntity.ok(new WhatsAppConnectResponse(result.connected(), result.base64(), result.pairingCode()));
    }

    @GetMapping("/qrcode")
    public ResponseEntity<WhatsAppConnectResponse> qrCode(@RequestParam UUID companyId) {
        var company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Empresa não encontrada: " + companyId));
        EvolutionConnectResult result = evolutionHttpClient.fetchQrCode(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey());
        return ResponseEntity.ok(new WhatsAppConnectResponse(result.connected(), result.base64(), result.pairingCode()));
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect(@RequestParam UUID companyId) {
        var company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Empresa não encontrada: " + companyId));
        evolutionHttpClient.disconnectInstance(
            company.getEvolutionInstanceName(), company.getEvolutionApiKey());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/webhook")
    public ResponseEntity<WebhookConfigResponse> getWebhook(@RequestParam UUID companyId) {
        var company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Empresa não encontrada: " + companyId));
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
        var company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Empresa não encontrada: " + companyId));

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
}
