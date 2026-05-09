package ms.chatbot.dias.domain.port;

import ms.chatbot.dias.domain.entity.Company;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository {
    Optional<Company> findByInstanceName(String instanceName);
    Optional<Company> findById(UUID id);
    Company save(Company company);
}
