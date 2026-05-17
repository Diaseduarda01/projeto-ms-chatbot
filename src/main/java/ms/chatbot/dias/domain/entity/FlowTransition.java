package ms.chatbot.dias.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "flow_transitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "flow_step_id", nullable = false, insertable = false, updatable = false)
    private UUID flowStepId;

    @Column(name = "`trigger`", nullable = false)
    private String trigger;

    @Column(name = "next_step_key", nullable = false)
    private String nextStepKey;

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;
}
