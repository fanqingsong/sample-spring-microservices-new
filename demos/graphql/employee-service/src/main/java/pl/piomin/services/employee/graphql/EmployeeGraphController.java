package pl.piomin.services.employee.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import pl.piomin.services.employee.model.Employee;
import pl.piomin.services.employee.repository.EmployeeRepository;

@Controller
public class EmployeeGraphController {

	private final EmployeeRepository repository;

	public EmployeeGraphController(EmployeeRepository repository) {
		this.repository = repository;
	}

	@QueryMapping
	public List<Employee> employees() {
		return repository.findAll();
	}

	@QueryMapping
	public Employee employee(@Argument Long id) {
		return repository.findById(id);
	}

	@QueryMapping
	public List<Employee> employeesByOrganization(@Argument Long organizationId) {
		return repository.findByOrganization(organizationId);
	}

	@QueryMapping
	public List<Employee> employeesByDepartment(@Argument Long departmentId) {
		return repository.findByDepartment(departmentId);
	}

}
