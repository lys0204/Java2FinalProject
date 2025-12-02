package org.example.stackflowanalysis.Repositories;

import org.example.stackflowanalysis.DataStorage.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

}