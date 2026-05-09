package ms.chatbot.dias.infrastructure.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.entity.FlowStep;
import ms.chatbot.dias.domain.entity.FlowTransition;
import ms.chatbot.dias.domain.enums.ChannelType;
import ms.chatbot.dias.domain.port.CompanyRepository;
import ms.chatbot.dias.domain.port.FlowStepRepository;
import ms.chatbot.dias.infrastructure.web.dto.CompanyRequest;
import ms.chatbot.dias.infrastructure.web.dto.FlowStepRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final FlowStepRepository flowStepRepository;

    @PostMapping
    public ResponseEntity<Company> create(@RequestBody @Valid CompanyRequest request) {
        Company company = Company.builder()
            .name(request.name())
            .evolutionInstanceName(request.evolutionInstanceName())
            .evolutionApiKey(request.evolutionApiKey())
            .channelType(request.channelType() != null ? request.channelType() : ChannelType.BAILEYS)
            .welcomeStepKey(request.welcomeStepKey() != null ? request.welcomeStepKey() : "MAIN_MENU")
            .build();

        return ResponseEntity.ok(companyRepository.save(company));
    }

    @PostMapping("/{companyId}/steps")
    public ResponseEntity<FlowStep> addStep(
            @PathVariable UUID companyId,
            @RequestBody @Valid FlowStepRequest request) {

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new RuntimeException("Empresa não encontrada: " + companyId));

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
            .transitions(transitions)
            .build();

        return ResponseEntity.ok(flowStepRepository.save(step));
    }

    @GetMapping("/{companyId}/steps")
    public ResponseEntity<List<FlowStep>> listSteps(@PathVariable UUID companyId) {
        return ResponseEntity.ok(flowStepRepository.findAllByCompanyId(companyId));
    }
}
