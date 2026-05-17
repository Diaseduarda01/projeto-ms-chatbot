package ms.chatbot.dias.infrastructure.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ms.chatbot.dias.application.service.FlowBuilderService;
import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.entity.FlowStep;
import ms.chatbot.dias.domain.entity.FlowTransition;
import ms.chatbot.dias.domain.enums.ChannelType;
import ms.chatbot.dias.domain.port.CompanyRepository;
import ms.chatbot.dias.domain.port.FlowStepRepository;
import ms.chatbot.dias.infrastructure.evolution.EvolutionHttpClient;
import ms.chatbot.dias.infrastructure.web.dto.CompanyRequest;
import ms.chatbot.dias.infrastructure.web.dto.FlowStepExport;
import ms.chatbot.dias.infrastructure.web.dto.FlowStepRequest;
import ms.chatbot.dias.infrastructure.web.dto.UpdateCompanyRequest;
import ms.chatbot.dias.infrastructure.web.dto.UpdateFlowStepRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final FlowStepRepository flowStepRepository;
    private final FlowBuilderService flowBuilderService;
    private final EvolutionHttpClient evolutionHttpClient;

    @Value("${chatbot.webhook-base-url:http://dias-chatbot:8085}")
    private String webhookBaseUrl;

    @PostMapping
    public ResponseEntity<Company> create(@RequestBody @Valid CompanyRequest request) {
        Company company = Company.builder()
            .name(request.name())
            .evolutionInstanceName(request.evolutionInstanceName())
            .evolutionApiKey(request.evolutionApiKey())
            .channelType(request.channelType() != null ? request.channelType() : ChannelType.BAILEYS)
            .welcomeStepKey(request.welcomeStepKey() != null ? request.welcomeStepKey() : "MAIN_MENU")
            .erpEmpresaId(request.erpEmpresaId())
            .endereco(request.endereco())
            .horarioFuncionamento(request.horarioFuncionamento())
            .telefoneContato(request.telefoneContato())
            .build();

        Company saved = companyRepository.save(company);

        evolutionHttpClient.setWebhook(
            saved.getEvolutionInstanceName(),
            saved.getEvolutionApiKey(),
            webhookBaseUrl + "/webhook/" + saved.getEvolutionInstanceName(),
            List.of("MESSAGES_UPSERT", "QRCODE_UPDATED", "CONNECTION_UPDATE")
        );

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/by-erp/{erpEmpresaId}")
    public ResponseEntity<Company> getByErpId(@PathVariable String erpEmpresaId) {
        return companyRepository.findByErpEmpresaId(erpEmpresaId)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                "Empresa chatbot não encontrada para erpEmpresaId: " + erpEmpresaId));
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<Company> getCompany(@PathVariable UUID companyId) {
        Company company = findCompanyOrThrow(companyId);
        return ResponseEntity.ok(company);
    }

    @PutMapping("/{companyId}")
    public ResponseEntity<Company> updateCompany(
            @PathVariable UUID companyId,
            @RequestBody @Valid UpdateCompanyRequest request) {

        Company company = findCompanyOrThrow(companyId);

        company.setName(request.name());
        if (request.welcomeStepKey() != null) company.setWelcomeStepKey(request.welcomeStepKey());
        if (request.active() != null) company.setActive(request.active());
        if (request.erpEmpresaId() != null) company.setErpEmpresaId(request.erpEmpresaId());
        if (request.evolutionApiKey() != null) company.setEvolutionApiKey(request.evolutionApiKey());
        if (request.channelType() != null) company.setChannelType(request.channelType());
        if (request.endereco() != null) company.setEndereco(request.endereco());
        if (request.horarioFuncionamento() != null) company.setHorarioFuncionamento(request.horarioFuncionamento());
        if (request.telefoneContato() != null) company.setTelefoneContato(request.telefoneContato());

        return ResponseEntity.ok(companyRepository.save(company));
    }

    @PostMapping("/{companyId}/steps")
    public ResponseEntity<FlowStep> addStep(
            @PathVariable UUID companyId,
            @RequestBody @Valid FlowStepRequest request) {

        Company company = findCompanyOrThrow(companyId);

        List<FlowTransition> transitions = request.transitions() == null ? List.of() :
            request.transitions().stream()
                .map(t -> FlowTransition.builder()
                    .trigger(t.trigger())
                    .nextStepKey(t.nextStepKey())
                    .sortOrder(t.sortOrder())
                    .build())
                .toList();

        FlowStep step = FlowStep.builder()
            .companyId(company.getId())
            .stepKey(request.stepKey())
            .type(request.type())
            .messageTemplate(request.messageTemplate())
            .inputType(request.inputType())
            .sessionDataKey(request.sessionDataKey())
            .defaultNextStepKey(request.defaultNextStepKey())
            .actionType(request.actionType())
            .transitions(transitions)
            .build();

        return ResponseEntity.ok(flowStepRepository.save(step));
    }

    @GetMapping("/{companyId}/steps")
    public ResponseEntity<List<FlowStep>> listSteps(@PathVariable UUID companyId) {
        findCompanyOrThrow(companyId);
        return ResponseEntity.ok(flowStepRepository.findAllByCompanyId(companyId));
    }

    @GetMapping("/{companyId}/steps/export")
    public ResponseEntity<List<FlowStepExport>> exportSteps(@PathVariable UUID companyId) {
        findCompanyOrThrow(companyId);
        List<FlowStepExport> exported = flowStepRepository.findAllByCompanyId(companyId)
            .stream().map(FlowStepExport::from).toList();
        return ResponseEntity.ok(exported);
    }

    @PostMapping("/{companyId}/steps/import")
    public ResponseEntity<List<FlowStep>> importSteps(
            @PathVariable UUID companyId,
            @RequestBody List<FlowStepExport> steps) {

        findCompanyOrThrow(companyId);
        flowStepRepository.deleteAllByCompanyId(companyId);

        List<FlowStep> created = steps.stream().map(s -> {
            List<FlowTransition> transitions = s.transitions() == null ? List.of() :
                s.transitions().stream()
                    .map(t -> FlowTransition.builder()
                        .trigger(t.trigger())
                        .nextStepKey(t.nextStepKey())
                        .sortOrder(t.sortOrder())
                        .build())
                    .toList();
            return FlowStep.builder()
                .companyId(companyId)
                .stepKey(s.stepKey())
                .type(s.type())
                .messageTemplate(s.messageTemplate())
                .inputType(s.inputType())
                .sessionDataKey(s.sessionDataKey())
                .defaultNextStepKey(s.defaultNextStepKey())
                .actionType(s.actionType())
                .transitions(transitions)
                .build();
        }).toList();

        return ResponseEntity.ok(flowStepRepository.saveAll(created));
    }

    @PutMapping("/{companyId}/steps/{stepId}")
    public ResponseEntity<FlowStep> updateStep(
            @PathVariable UUID companyId,
            @PathVariable UUID stepId,
            @RequestBody @Valid UpdateFlowStepRequest request) {

        findCompanyOrThrow(companyId);
        findStepOrThrow(stepId, companyId);

        if (request.defaultNextStepKey() != null) {
            flowStepRepository.findByCompanyIdAndStepKey(companyId, request.defaultNextStepKey())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                    "Step referenciado não existe: " + request.defaultNextStepKey()));
        }

        FlowStep updated = flowBuilderService.updateStep(stepId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{companyId}/steps/{stepId}")
    public ResponseEntity<Void> deleteStep(
            @PathVariable UUID companyId,
            @PathVariable UUID stepId) {

        findCompanyOrThrow(companyId);
        findStepOrThrow(stepId, companyId);
        flowStepRepository.deleteById(stepId);
        return ResponseEntity.noContent().build();
    }

    private Company findCompanyOrThrow(UUID companyId) {
        return companyRepository.findById(companyId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Empresa não encontrada: " + companyId));
    }

    private FlowStep findStepOrThrow(UUID stepId, UUID companyId) {
        FlowStep step = flowStepRepository.findById(stepId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Step não encontrado: " + stepId));
        if (!step.getCompanyId().equals(companyId)) {
            throw new ResponseStatusException(NOT_FOUND, "Step não pertence a esta empresa");
        }
        return step;
    }
}
