package pl.piomin.services.employee;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.restdocs.RestDocsMockMvcConfigurationCustomizer;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.piomin.services.employee.controller.EmployeeController;
import pl.piomin.services.employee.model.Employee;
import pl.piomin.services.employee.repository.EmployeeRepository;

@WebMvcTest(EmployeeController.class)
@AutoConfigureRestDocs
class EmployeeControllerTest {

	@MockitoBean
	EmployeeRepository repository;

	@Autowired
	MockMvc mockMvc;

	private final ObjectMapper mapper = new ObjectMapper();

	@TestConfiguration
	static class CustomizationConfiguration implements RestDocsMockMvcConfigurationCustomizer {

		@Override
		public void customize(MockMvcRestDocumentationConfigurer configurer) {
			configurer.operationPreprocessors()
				.withRequestDefaults(prettyPrint())
				.withResponseDefaults(prettyPrint());
		}

		@Bean
		RestDocumentationResultHandler restDocumentation() {
			return MockMvcRestDocumentation.document("{method-name}");
		}
	}

	@BeforeEach
	void setUp() {
		Employee employee = new Employee(1L, 1L, "John Smith", 33, "Developer");
		employee.setId(1L);
		when(repository.add(Mockito.any(Employee.class))).thenReturn(employee);
		when(repository.findById(1L)).thenReturn(employee);
	}

	@Test
	void addPerson() throws Exception {
		Employee employee = new Employee(1L, 1L, "John Smith", 33, "Developer");
		mockMvc.perform(post("/").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(employee)))
			.andExpect(status().isOk())
			.andDo(document("add-person/", requestFields(
				fieldWithPath("id").description("Employee id").ignored(),
				fieldWithPath("organizationId").description("Employee's organization id"),
				fieldWithPath("departmentId").description("Employee's department id"),
				fieldWithPath("name").description("Employee's name"),
				fieldWithPath("age").description("Employee's age"),
				fieldWithPath("position").description("Employee's position inside organization")
			)));
	}

	@Test
	void findPersonById() throws Exception {
		mockMvc.perform(get("/{id}", 1).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("find-person-by-id/", responseFields(
				fieldWithPath("id").description("Employee id"),
				fieldWithPath("organizationId").description("Employee's organization id"),
				fieldWithPath("departmentId").description("Employee's department id"),
				fieldWithPath("name").description("Employee's name"),
				fieldWithPath("age").description("Employee's age"),
				fieldWithPath("position").description("Employee's position inside organization")
			), pathParameters(parameterWithName("id").description("Employee id"))));
	}

}
