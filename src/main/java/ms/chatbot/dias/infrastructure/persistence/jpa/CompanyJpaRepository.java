package ms.chatbot.dias.infrastructure.persistence.jpa;

import ms.chatbot.dias.domain.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyJpaRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByEvolutionInstanceName(String instanceName);
    Optional<Company> findByErpEmpresaId(String erpEmpresaId);
}
