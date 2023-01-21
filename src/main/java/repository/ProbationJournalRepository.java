package repository;

import model.ProbationJournal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProbationJournalRepository extends JpaRepository<ProbationJournal, Integer> {
}
