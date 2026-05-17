package ms.chatbot.dias.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InternalKeyInterceptor implements HandlerInterceptor {

    private final String internalApiKey;

    public InternalKeyInterceptor(@Value("${chatbot.internal-api-key}") String internalApiKey) {
        this.internalApiKey = internalApiKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String provided = request.getHeader("X-Internal-Key");
        if (!internalApiKey.equals(provided)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chave interna inválida ou ausente");
            return false;
        }
        return true;
    }
}
