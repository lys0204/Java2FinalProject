package org.example.stackflowanalysis.Repositories;

import org.example.stackflowanalysis.Data.QuestionOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionOwnerRepository extends JpaRepository<QuestionOwner, Long> {
    Optional<QuestionOwner> findByUsername(String username);
}