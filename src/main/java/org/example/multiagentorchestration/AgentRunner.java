package org.example.multiagentorchestration;

import org.example.multiagentorchestration.service.ResearchCoordinatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgentRunner implements CommandLineRunner {

    @Autowired
    private ResearchCoordinatorService researchCoordinatorService;

    public static void main(String[] args) {
        SpringApplication.run(AgentRunner.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String result = researchCoordinatorService.researchCordinator("Impact of AI on healthcare");
        System.out.println(result);
    }
}
