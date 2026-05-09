package ms.chatbot.dias.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import ms.chatbot.dias.domain.enums.ChannelType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "evolution_instance_name", nullable = false, unique = true)
    private String evolutionInstanceName;

    @Column(name = "evolution_api_key")
    private String evolutionApiKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    @Builder.Default
    private ChannelType channelType = ChannelType.BAILEYS;

    @Column(name = "welcome_step_key", nullable = false)
    @Builder.Default
    private String welcomeStepKey = "MAIN_MENU";

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
