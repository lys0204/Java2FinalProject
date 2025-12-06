package org.example.stackflowanalysis.Controller;

import org.example.stackflowanalysis.Service.DataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataController {

    private final DataService stackOverflowService;

    public DataController(DataService stackOverflowService) {
        this.stackOverflowService = stackOverflowService;
    }

    @GetMapping("/api/collect")
    public String triggerCollection() {
        new Thread(stackOverflowService::collectData).start();
        return "Data collection started in background. Check console logs.";
    }
}