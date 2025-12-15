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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DataService {

    @Autowired private QuestionRepository questionRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private QuestionOwnerRepository ownerRepository;
    @Autowired private AnswerRepository answerRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String API_URL_TEMPLATE =
            "https://api.stackexchange.com/2.3/questions?" +
                    "page=%d" +
                    "&pagesize=50" +
                    "&fromdate=%d" +
                    "&todate=%d" +
                    "&order=desc" +
                    "&sort=%s" +
                    "&tagged=java" +
                    "&site=stackoverflow" +
                    "&filter=!aksql6NjneanAa";

    private static final String[] SORT_STRATEGIES = {
            "votes",
            "creation",
            "hot",
            "week",
            "month"
    };
    private final Set<Long> collectedQuestionIds = ConcurrentHashMap.newKeySet();
    public void collectData() {
        System.out.println("开始多维度数据收集...");
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusYears(15);
        long startTimestamp = startDate.toEpochSecond(ZoneOffset.UTC);
        long endTimestamp = endDate.toEpochSecond(ZoneOffset.UTC);
        long chunkSeconds = 720L * 24 * 60 * 60;
        for (long chunkStart = startTimestamp; chunkStart < endTimestamp; chunkStart += chunkSeconds) {
            long chunkEnd = Math.min(chunkStart + chunkSeconds, endTimestamp);
            for (String sortStrategy : SORT_STRATEGIES) {
                System.out.printf("时间片 %s 至 %s | 排序: %s%n",
                        LocalDateTime.ofEpochSecond(chunkStart, 0, ZoneOffset.UTC),
                        LocalDateTime.ofEpochSecond(chunkEnd, 0, ZoneOffset.UTC),
                        sortStrategy);
                collectDataForStrategy(chunkStart, chunkEnd, sortStrategy, 25);
                try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
            }
            try { Thread.sleep(3000); } catch (InterruptedException e) { break; }
        }
        System.out.println("数据收集完成！总共收集唯一问题: " + collectedQuestionIds.size());
    }
    private void collectDataForStrategy(long fromDate, long toDate, String sortStrategy, int maxPages) {
        int page = 1;
        int totalInThisRun = 0;
        maxPages = Math.min(maxPages, 25);
        while (page <= maxPages) {
            String url = String.format(API_URL_TEMPLATE, page, fromDate, toDate, sortStrategy);
            System.out.println("  获取第 " + page + " 页...");
            try {
                ResponseEntity<StackOverflowResponse<QuestionDto>> responseEntity =
                        restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                null,
                                new ParameterizedTypeReference<StackOverflowResponse<QuestionDto>>() {}
                        );
                StackOverflowResponse<QuestionDto> response = responseEntity.getBody();
                if (response == null || response.items() == null || response.items().isEmpty()) {
                    System.out.println("  无数据，停止当前策略");
                    break;
                }
                int newItems = 0;
                for (QuestionDto qDto : response.items()) {
                    if (!collectedQuestionIds.contains(qDto.questionId())) {
                        saveQuestionData(qDto);
                        collectedQuestionIds.add(qDto.questionId());
                        newItems++;
                    }
                }
                totalInThisRun += newItems;
                System.out.printf("  获取%d条，其中%d条新数据%n",
                        response.items().size(), newItems);
                Thread.sleep(2000);
                if (!response.hasMore()) {
                    break;
                }
                page++;
            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                System.out.println("  遇到速率限制，等待60秒...");
                try { Thread.sleep(60000); } catch (InterruptedException ie) { break; }
            } catch (Exception e) {
                System.out.println("  请求出错: " + e.getMessage() + "，跳过本页");
                page++;
                try { Thread.sleep(5000); } catch (InterruptedException ie) { break; }
            }
        }
        System.out.printf("当前策略完成，收集%d条新数据%n", totalInThisRun);
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
        question.setId(qDto.questionId());
        question.setScore(qDto.score());
        question.setViewCount(qDto.viewCount());
        question.setAnswerCount(qDto.answerCount());
        question.setAnswered(qDto.isAnswered());
        question.setOwner(owner);
        Set<Tag> tags = new HashSet<>();
        if (qDto.tags() != null && !qDto.tags().isEmpty()) {
            List<Tag> existingTags = tagRepository.findByNameIn(qDto.tags());
            Map<String, Tag> existingTagMap = existingTags.stream()
                    .collect(Collectors.toMap(Tag::getName, tag -> tag));
            List<Tag> newTags = new ArrayList<>();
            for (String tagName : qDto.tags()) {
                Tag tag = existingTagMap.get(tagName);
                if (tag != null) {
                    tags.add(tag);
                } else {
                    Tag newTag = new Tag(tagName);
                    newTags.add(newTag);
                    tags.add(newTag);
                }
            }
            if (!newTags.isEmpty()) {
                tagRepository.saveAll(newTags);
            }
        }
        question.setTags(tags);
        question = questionRepository.save(question);
        if (qDto.answers() != null && !qDto.answers().isEmpty()) {
            List<Answer> answersToSave = new ArrayList<>();
            for (AnswerDto aDto : qDto.answers()) {
                QuestionOwner answerer = getOrCreateOwner(aDto.owner());
                Answer answer = new Answer(
                        aDto.body(),
                        question,
                        answerer,
                        aDto.getCreationDateTime()
                );
                answer.setId(aDto.answerId());
                answer.setScore(aDto.score());
                answer.setAccepted(aDto.isAccepted());
                answersToSave.add(answer);
            }
            answerRepository.saveAll(answersToSave);
        }
    }

    private QuestionOwner getOrCreateOwner(OwnerDto ownerDto) {
        if (ownerDto == null || ownerDto.displayName() == null || ownerDto.userId() == null) {
            return null;
        }
        Optional<QuestionOwner> existing = ownerRepository.findById(ownerDto.userId());
        if (existing.isPresent()) {
            QuestionOwner owner = existing.get();
            owner.setId(ownerDto.userId());
            owner.setUsername(ownerDto.displayName());
            owner.setReputation(ownerDto.reputation());
            ownerRepository.save(owner);
            return owner;
        }
        try {
            return ownerRepository.save(new QuestionOwner(ownerDto.userId(), ownerDto.displayName(), ownerDto.reputation()));
        } catch (DataIntegrityViolationException e) {
            QuestionOwner tar = ownerRepository.findById(ownerDto.userId())
                    .orElseThrow(() -> new RuntimeException("并发处理异常：保存失败且无法查询到数据", e));
            tar.setId(ownerDto.userId());
            tar.setUsername(ownerDto.displayName());
            tar.setReputation(ownerDto.reputation());
            ownerRepository.save(tar);
            return tar;
        }
    }
}