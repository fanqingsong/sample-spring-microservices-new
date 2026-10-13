package pl.piomin.services.organization;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@EnableFeignClients
@EnableMongoRepositories
@OpenAPIDefinition(info = @Info(title = "Organization API", version = "1.0", description = "Documentation Organization API"))
public class OrganizationApplication {

	@Value("${VERSION:1.0}")
	String version;

	public static void main(String[] args) {
		SpringApplication.run(OrganizationApplication.class, args);
	}

}
