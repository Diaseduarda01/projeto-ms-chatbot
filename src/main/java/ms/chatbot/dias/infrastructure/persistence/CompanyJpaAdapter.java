package ms.chatbot.dias.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.port.CompanyRepository;
import ms.chatbot.dias.infrastructure.persistence.jpa.CompanyJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CompanyJpaAdapter implements CompanyRepository {

    private final CompanyJpaRepository jpa;

    @Override
    public Optional<Company> findByInstanceName(String instanceName) {
        return jpa.findByEvolutionInstanceName(instanceName);
    }

    @Override
    public Optional<Company> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Company save(Company company) {
        return jpa.save(company);
    }
}
