package org.example.stackflowanalysis.Repositories;

import org.example.stackflowanalysis.DataStorage.QuestionOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionOwnerRepository extends JpaRepository<QuestionOwner, Long> {
    // 用于查找已存在的用户
    Optional<QuestionOwner> findByUsername(String username);
}