package pl.piomin.services.department.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import pl.piomin.services.department.client.EmployeeGraphClient;
import pl.piomin.services.department.model.Department;
import pl.piomin.services.department.repository.DepartmentRepository;

@Controller
public class DepartmentGraphController {

	private final DepartmentRepository repository;
	private final EmployeeGraphClient employeeGraphClient;

	public DepartmentGraphController(DepartmentRepository repository, EmployeeGraphClient employeeGraphClient) {
		this.repository = repository;
		this.employeeGraphClient = employeeGraphClient;
	}

	@QueryMapping
	public List<Department> departments() {
		return repository.findAll();
	}

	@QueryMapping
	public Department department(@Argument Long id) {
		return repository.findById(id);
	}

	@QueryMapping
	public List<Department> departmentsByOrganization(@Argument Long organizationId) {
		return repository.findByOrganization(organizationId);
	}

	@QueryMapping
	public List<Department> departmentsByOrganizationWithEmployees(@Argument Long organizationId) {
		List<Department> departments = repository.findByOrganization(organizationId);
		departments.forEach(department -> department.setEmployees(employeeGraphClient.findByDepartment(department.getId())));
		return departments;
	}

}
