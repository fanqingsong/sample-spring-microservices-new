package pl.piomin.services.employee;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableMongoRepositories
@OpenAPIDefinition(info = @Info(title = "Employee API", version = "1.0", description = "Documentation Employee API"))
public class EmployeeApplication {

	private static final Logger LOG = LoggerFactory.getLogger(EmployeeApplication.class);

	@Value("${VERSION:1.0}")
	String version;

	public static void main(String[] args) {
		SpringApplication.run(EmployeeApplication.class, args);
	}

	@PostConstruct
	public void logEnvironment() {
		System.getenv().forEach((key, value) -> LOG.info("{}={}", key, value));
	}

}
