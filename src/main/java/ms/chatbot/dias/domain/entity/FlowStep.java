package ms.chatbot.dias.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import ms.chatbot.dias.domain.enums.InputType;
import ms.chatbot.dias.domain.enums.StepType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "flow_steps", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"company_id", "step_key"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "step_key", nullable = false)
    private String stepKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepType type;

    @Column(name = "message_template", nullable = false, columnDefinition = "TEXT")
    private String messageTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type")
    private InputType inputType;

    @Column(name = "session_data_key")
    private String sessionDataKey;

    @Column(name = "default_next_step_key")
    private String defaultNextStepKey;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "flow_step_id")
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<FlowTransition> transitions = new ArrayList<>();
}
