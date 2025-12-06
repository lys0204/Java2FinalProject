package org.example.stackflowanalysis.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "answers")
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    @Column(nullable = false)
    private LocalDateTime dateTime;
    private int score;
    private boolean isAccepted;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answerer_id")
    private QuestionOwner answerer;

    public Answer() {}
    public Answer(String content, Question question, QuestionOwner owner, LocalDateTime dateTime) {
        this.content = content;
        this.question = question;
        this.answerer = owner;
        this.dateTime = dateTime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public boolean isAccepted() { return isAccepted; }
    public void setAccepted(boolean accepted) { isAccepted = accepted; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public QuestionOwner getAnswerer() { return answerer; }
    public void setAnswerer(QuestionOwner answerer) { this.answerer = answerer; }
}