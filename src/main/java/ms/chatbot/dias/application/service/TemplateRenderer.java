package ms.chatbot.dias.application.service;

import ms.chatbot.dias.domain.entity.Session;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TemplateRenderer {

    public String render(String template, Session session, String senderName) {
        String result = template;

        if (senderName != null && !senderName.isBlank()) {
            result = result.replace("{{nome}}", senderName);
        } else {
            result = result.replace("{{nome}}", "");
        }

        Map<String, String> data = session.getCollectedDataAsMap();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }

        return result;
    }
}
