package repository;

import model.ProbationData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProbationDataRepository extends JpaRepository<ProbationData, Integer> {
}
