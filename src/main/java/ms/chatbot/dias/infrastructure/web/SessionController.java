package ms.chatbot.dias.infrastructure.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ms.chatbot.dias.application.service.ChatEventPublisher;
import ms.chatbot.dias.application.usecase.SendManualMessageUseCase;
import ms.chatbot.dias.domain.entity.Session;
import ms.chatbot.dias.domain.exception.SessionNotInHandoffException;
import ms.chatbot.dias.domain.exception.SessionNotFoundException;
import ms.chatbot.dias.domain.port.MessageRepository;
import ms.chatbot.dias.domain.port.SessionRepository;
import ms.chatbot.dias.infrastructure.web.dto.EnviarMensagemRequest;
import ms.chatbot.dias.infrastructure.web.dto.InboxEvent;
import ms.chatbot.dias.infrastructure.web.dto.MessageResponse;
import ms.chatbot.dias.infrastructure.web.dto.SessionSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final SendManualMessageUseCase sendManualMessageUseCase;
    private final ChatEventPublisher eventPublisher;

    @GetMapping
    public ResponseEntity<List<SessionSummaryResponse>> listByCompany(
            @RequestParam UUID companyId) {

        List<SessionSummaryResponse> sessions = sessionRepository
            .findAllByCompanyId(companyId)
            .stream()
            .map(session -> {
                MessageResponse last = messageRepository
                    .findLastBySessionId(session.getId())
                    .map(MessageResponse::from)
                    .orElse(null);
                return SessionSummaryResponse.from(session, last);
            })
            .toList();

        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<Page<MessageResponse>> listMessages(
            @PathVariable UUID sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<MessageResponse> messages = messageRepository
            .findBySessionId(sessionId, PageRequest.of(page, size, Sort.by("createdAt").ascending()))
            .map(MessageResponse::from);

        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{sessionId}/mensagem")
    public ResponseEntity<Void> enviarMensagem(
            @PathVariable UUID sessionId,
            @RequestBody @Valid EnviarMensagemRequest request) {
        try {
            sendManualMessageUseCase.execute(sessionId, request.texto());
        } catch (SessionNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (SessionNotInHandoffException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{sessionId}/handoff")
    public ResponseEntity<Void> handoff(@PathVariable UUID sessionId) {
        Session session = findSessionOrThrow(sessionId);
        session.handoff();
        sessionRepository.save(session);
        eventPublisher.publishInboxEvent(session, InboxEvent.STATUS_CHANGED);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{sessionId}/reativar")
    public ResponseEntity<Void> reativar(@PathVariable UUID sessionId) {
        Session session = findSessionOrThrow(sessionId);
        session.reativar();
        sessionRepository.save(session);
        eventPublisher.publishInboxEvent(session, InboxEvent.STATUS_CHANGED);
        return ResponseEntity.noContent().build();
    }

    private Session findSessionOrThrow(UUID sessionId) {
        return sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                new SessionNotFoundException(sessionId).getMessage()));
    }
}
