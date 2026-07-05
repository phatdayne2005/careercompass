package vn.uth.careercompass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class CareerCompassApplication {

	public static void main(String[] args) {
		SpringApplication.run(CareerCompassApplication.class, args);
	}

}
