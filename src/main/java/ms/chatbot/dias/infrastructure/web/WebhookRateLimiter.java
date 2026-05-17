package ms.chatbot.dias.infrastructure.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebhookRateLimiter {

    private static final long WINDOW_MS = 1_000L;

    private final ConcurrentHashMap<String, Long> lastProcessed = new ConcurrentHashMap<>();

    /**
     * Returns true if the message from this phone should be processed.
     * Rejects messages arriving within 1s of the previous one for the same phone.
     */
    public boolean allow(String phone) {
        long now = System.currentTimeMillis();
        Long last = lastProcessed.get(phone);
        if (last != null && now - last < WINDOW_MS) {
            return false;
        }
        lastProcessed.put(phone, now);
        return true;
    }

    @Scheduled(fixedDelay = 60_000)
    public void cleanup() {
        long cutoff = System.currentTimeMillis() - 60_000;
        int before = lastProcessed.size();
        lastProcessed.entrySet().removeIf(e -> e.getValue() < cutoff);
        int removed = before - lastProcessed.size();
        if (removed > 0) {
            log.debug("Rate limiter: removidas {} entradas expiradas", removed);
        }
    }
}
