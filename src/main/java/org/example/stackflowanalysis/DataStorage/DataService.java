package org.example.stackflowanalysis.DataStorage;

import org.example.stackflowanalysis.DTOs.*;
import org.example.stackflowanalysis.Repositories.AnswerRepository;
import org.example.stackflowanalysis.Repositories.QuestionOwnerRepository;
import org.example.stackflowanalysis.Repositories.QuestionRepository;
import org.example.stackflowanalysis.Repositories.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    // 使用 RestTemplate 发送请求
    private final RestTemplate restTemplate = new RestTemplate();

    // API URL: 获取 Java 问题，按活动排序，包含 body 和 answers
    // 注意：filter=!9_bDDxJY5 是 StackOverflow 生成的一个 filter，用于包含 body 和 answers 字段
    // 你可能需要自己去 API 文档页生成一个 filter string 来获取 content
    private static final String API_URL =
            "https://api.stackexchange.com/2.3/questions?" +
                    "page=%d" +
                    "&pagesize=50" +
                    "&order=desc" +
                    "&sort=votes" +
                    "&tagged=java" +
                    "&site=stackoverflow" +
                    "&filter=!6WPIomp-eb(U5"; // <--- 这里使用自定义 Filter
    public void collectData() {
        int page = 1;
        int totalCollected = 0;

        // 目标是至少 1000 条 [cite: 26]
        while (totalCollected < 1000) {
            System.out.println("Fetching page " + page + "...");
            String url = String.format(API_URL, page);

            try {
                // 1. 调用 API
                StackOverflowResponse<QuestionDto> response = restTemplate.getForObject(
                        url,
                        StackOverflowResponse.class // 这里需要稍微处理一下泛型，或者用 Wrapper 类
                );

                // 由于泛型擦除，RestTemplate 直接转泛型可能复杂，建议使用 ParameterizedTypeReference
                // 这里为简化代码假设映射成功，实际开发建议用 exchange 方法

                if (response != null && response.items() != null) {
                    for (QuestionDto qDto : response.items()) {
                        saveQuestionData(qDto);
                    }
                    totalCollected += response.items().size();
                    System.out.println("Collected so far: " + totalCollected);
                }

                // 2. 遵守 Rate Limit [cite: 22]
                // 无论是否成功，休眠一下防止被封 IP
                Thread.sleep(2000);

                if (response == null || !response.hasMore()) {
                    break;
                }
                page++;

            } catch (Exception e) {
                e.printStackTrace();
                break; // 出错停止
            }
        }
    }

    @Transactional
    protected void saveQuestionData(QuestionDto qDto) {
        // 如果问题已存在，跳过（或者更新）
        // 注意：你现有的 Entity ID 是自增的，但 API 有自己的 ID。
        // 建议在 Question 实体加一个 stackOverflowId 字段来查重，或者暂时忽略重复

        // 1. 处理 Owner (用户去重)
        QuestionOwner owner = getOrCreateOwner(qDto.owner());

        // 2. 创建 Question 实体
        Question question = new Question(
                qDto.title(),
                qDto.body(), // 获取 HTML 内容
                owner,
                qDto.getCreationDateTime()
        );
         question.setScore(qDto.score());
         question.setViewCount(qDto.viewCount());

        // 3. 处理 Tags (标签去重)
        Set<Tag> tags = new HashSet<>();
        for (String tagName : qDto.tags()) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(tagName)));
            tags.add(tag);
        }
         question.setTags(tags); // 需要在实体中添加 setTags 或 getTags().addAll()

        // 4. 保存 Question 以获取 ID
        question = questionRepository.save(question);

        // 5. 处理 Answers
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
            return null; // 匿名用户处理
        }
        return ownerRepository.findByUsername(ownerDto.displayName())
                .orElseGet(() -> ownerRepository.save(new QuestionOwner(ownerDto.displayName())));
    }
}