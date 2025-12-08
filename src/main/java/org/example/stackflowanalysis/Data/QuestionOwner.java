package org.example.stackflowanalysis.Data;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "question_owners")
public class QuestionOwner {
    @Id
    private Long id;
    @Column(nullable = false)
    private String username;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private int reputation;
    @OneToMany(mappedBy = "owner")
    private Set<Question> questions = new HashSet<>();
    @OneToMany(mappedBy = "answerer")
    private Set<Answer> answers = new HashSet<>();

    public QuestionOwner() {}
    public QuestionOwner(Long id, String username, int reputation) {
        this.id = id;
        this.username = username;
        this.reputation = reputation;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getReputation() { return reputation; }
    public void setReputation(int reputation) { this.reputation = reputation; }
    public Set<Question> getQuestions() { return questions; }
    public void setQuestions(Set<Question> questions) { this.questions = questions; }
    public Set<Answer> getAnswers() { return answers; }
    public void setAnswers(Set<Answer> answers) { this.answers = answers; }
}