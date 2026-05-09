package ms.chatbot.dias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class DiasApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiasApplication.class, args);
	}
}
