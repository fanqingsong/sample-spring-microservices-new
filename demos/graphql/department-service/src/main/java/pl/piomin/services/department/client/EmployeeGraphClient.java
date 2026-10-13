package pl.piomin.services.department.client;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import pl.piomin.services.department.model.Employee;

@Component
public class EmployeeGraphClient {

	private static final String EMPLOYEES_BY_DEPARTMENT = """
			query employeesByDepartment($departmentId: ID!) {
			  employeesByDepartment(departmentId: $departmentId) {
			    id
			    organizationId
			    departmentId
			    name
			    age
			    position
			  }
			}
			""";

	private final HttpGraphQlClient graphQlClient;

	public EmployeeGraphClient(@LoadBalanced WebClient.Builder webClientBuilder) {
		this.graphQlClient = HttpGraphQlClient.builder(
				webClientBuilder.baseUrl("http://employee-service").build())
			.build();
	}

	public List<Employee> findByDepartment(Long departmentId) {
		return graphQlClient.document(EMPLOYEES_BY_DEPARTMENT)
			.variables(Map.of("departmentId", departmentId))
			.execute()
			.field("employeesByDepartment")
			.toEntityList(Employee.class);
	}

}
