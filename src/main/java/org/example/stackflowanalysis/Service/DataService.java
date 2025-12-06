package org.example.stackflowanalysis.Service;

import org.example.stackflowanalysis.DTOs.*;
import org.example.stackflowanalysis.Data.Answer;
import org.example.stackflowanalysis.Data.Question;
import org.example.stackflowanalysis.Data.QuestionOwner;
import org.example.stackflowanalysis.Data.Tag;
import org.example.stackflowanalysis.Repositories.AnswerRepository;
import org.example.stackflowanalysis.Repositories.QuestionOwnerRepository;
import org.example.stackflowanalysis.Repositories.QuestionRepository;
import org.example.stackflowanalysis.Repositories.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.Set;

@Service
public class DataService {

    @Autowired private QuestionRepository questionRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private QuestionOwnerRepository ownerRepository;
    @Autowired private AnswerRepository answerRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String API_URL =
            "https://api.stackexchange.com/2.3/questions?" +
                    "page=%d" +
                    "&pagesize=50" +
                    "&order=desc" +
                    "&sort=votes" +
                    "&tagged=java" +
                    "&site=stackoverflow" +
                    "&filter=!6WPIomp-eb(U5";
    public void collectData() {
        int page = 1;
        int totalCollected = 0;
        while (totalCollected < 1000) {
            System.out.println("Fetching page " + page + "...");
            String url = String.format(API_URL, page);
            try {
                ResponseEntity<StackOverflowResponse<QuestionDto>> responseEntity =
                        restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                null,
                                new ParameterizedTypeReference<StackOverflowResponse<QuestionDto>>() {}
                        );
                StackOverflowResponse<QuestionDto> response = responseEntity.getBody();
                if (response != null && response.items() != null) {
                    for (QuestionDto qDto : response.items()) {
                        saveQuestionData(qDto);
                    }
                    totalCollected += response.items().size();
                    System.out.println("Collected so far: " + totalCollected);
                }
                Thread.sleep(2000);
                if (response == null || !response.hasMore()) {
                    break;
                }
                page++;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    @Transactional
    protected void saveQuestionData(QuestionDto qDto) {
        QuestionOwner owner = getOrCreateOwner(qDto.owner());
        Question question = new Question(
                qDto.title(),
                qDto.body(),
                owner,
                qDto.getCreationDateTime()
        );
         question.setScore(qDto.score());
         question.setViewCount(qDto.viewCount());
        Set<Tag> tags = new HashSet<>();
        for (String tagName : qDto.tags()) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(tagName)));
            tags.add(tag);
        }
         question.setTags(tags);
        question = questionRepository.save(question);
        if (qDto.answers() != null) {
            for (AnswerDto aDto : qDto.answers()) {
                QuestionOwner answerer = getOrCreateOwner(aDto.owner());
                Answer answer = new Answer(
                        aDto.body(),
                        question,
                        answerer,
                        aDto.getCreationDateTime()
                );
                 answer.setScore(aDto.score());
                 answer.setAccepted(aDto.isAccepted());
                answerRepository.save(answer);
            }
        }
    }

    private QuestionOwner getOrCreateOwner(OwnerDto ownerDto) {
        if (ownerDto == null || ownerDto.displayName() == null) {
            return null;
        }
        return ownerRepository.findByUsername(ownerDto.displayName())
                .orElseGet(() -> ownerRepository.save(new QuestionOwner(ownerDto.displayName())));
    }
}