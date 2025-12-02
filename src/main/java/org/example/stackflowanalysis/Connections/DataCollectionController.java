package org.example.stackflowanalysis.Connections;

import org.example.stackflowanalysis.DataStorage.DataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataCollectionController {

    private final DataService stackOverflowService;

    public DataCollectionController(DataService stackOverflowService) {
        this.stackOverflowService = stackOverflowService;
    }

    @GetMapping("/api/collect")
    public String triggerCollection() {
        // 建议另开线程运行，防止 HTTP 超时
        new Thread(stackOverflowService::collectData).start();
        return "Data collection started in background. Check console logs.";
    }
}