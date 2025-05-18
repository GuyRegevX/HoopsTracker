package hoops.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class HoopsProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(HoopsProcessorApplication.class, args);
    }
} 