package ms.chatbot.dias.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import ms.chatbot.dias.domain.enums.SessionStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "sessions", indexes = {
    @Index(columnList = "company_id, phone_number", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "current_step_key", nullable = false)
    private String currentStepKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SessionStatus status = SessionStatus.NEW;

    @Column(name = "collected_data", columnDefinition = "TEXT")
    @Builder.Default
    private String collectedData = "{}";

    @Column(name = "last_activity")
    @Builder.Default
    private LocalDateTime lastActivity = LocalDateTime.now();

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.lastActivity = LocalDateTime.now();
    }

    public static Session newSession(UUID companyId, String phoneNumber, String welcomeStepKey) {
        return Session.builder()
            .companyId(companyId)
            .phoneNumber(phoneNumber)
            .currentStepKey(welcomeStepKey)
            .status(SessionStatus.NEW)
            .build();
    }

    public void storeData(String key, String value) {
        Map<String, String> data = getCollectedDataAsMap();
        data.put(key, value);
        try {
            this.collectedData = MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            this.collectedData = "{}";
        }
    }

    public Map<String, String> getCollectedDataAsMap() {
        try {
            if (collectedData == null || collectedData.isBlank()) return new HashMap<>();
            return MAPPER.readValue(collectedData, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    public String getData(String key) {
        return getCollectedDataAsMap().get(key);
    }

    public void activate() {
        this.status = SessionStatus.ACTIVE;
    }

    public void complete() {
        this.status = SessionStatus.COMPLETED;
    }

    public void handoff() {
        this.status = SessionStatus.HANDOFF;
    }

    public void reativar() {
        this.status = SessionStatus.ACTIVE;
    }
}
