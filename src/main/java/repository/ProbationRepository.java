package repository;

import model.Probation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProbationRepository extends JpaRepository<Probation, Integer> {
}
