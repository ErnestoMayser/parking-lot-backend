package bo.mayser.parkinglotbackend.app.services.impl;

import bo.mayser.parkinglotbackend.app.domain.entities.*;
import bo.mayser.parkinglotbackend.app.domain.repositories.*;
import bo.mayser.parkinglotbackend.app.exceptions.roles.RoleNotFoundException;
import bo.mayser.parkinglotbackend.app.exceptions.users.InvalidPasswordException;
import bo.mayser.parkinglotbackend.app.exceptions.users.UserAlreadyRegisteredException;
import bo.mayser.parkinglotbackend.app.exceptions.users.UserException;
import bo.mayser.parkinglotbackend.app.services.EmployeeService;
import bo.mayser.parkinglotbackend.app.services.dto.requests.EmployeeRequest;
import bo.mayser.parkinglotbackend.app.services.dto.responses.EmployeeResponse;
import bo.mayser.parkinglotbackend.app.services.mappers.EmployeeMapper;
import bo.mayser.parkinglotbackend.app.services.mappers.UserMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleActionRepository roleActionRepository;
    private final UserActionRepository userActionRepository;
    private final UserMapper userMapper;
    private final EmployeeMapper employeeMapper;

    @Override
    public EmployeeResponse registerEmployee(EmployeeRequest request) throws UserException, RoleNotFoundException {
        verifyUsername(request.getUsername());
        verifyEmail(request.getEmail());
        comparePasswords(request.getPassword(), request.getPasswordConfirmation());
        Role role = roleRepository.findById(request.getRoleId()).orElseThrow(RoleNotFoundException::new);
        User user = userMapper.fromUserRequestToUser(request);
        User savedUser = userRepository.saveAndFlush(user);
        Employee employee = employeeMapper.fromEmployeeRequestToEmployee(request);
        employee.setUserId(savedUser.getId());
        Employee savedEmployee = employeeRepository.saveAndFlush(employee);
        List<RoleAction> roleActions = roleActionRepository.getAllByRoleId(role.getId());
        List<UserAction> userActions = new ArrayList<>();
        roleActions.forEach(roleAction -> {
            UserAction userAction = new UserAction();
            userAction.setUserId(savedUser.getId());
            userAction.setActionId(roleAction.getActionId());
            userActions.add(userAction);
        });
        userActionRepository.saveAllAndFlush(userActions);
        EmployeeResponse response = employeeMapper.fromEmployeeToEmployeeResponse(savedEmployee);
        response.setUser(userMapper.fromUserToUserResponse(savedUser));
        return response;
    }

    private void verifyUsername(String username) throws UserAlreadyRegisteredException {
        if (userRepository.findByUsername(username.trim().toLowerCase()).isPresent()) {
            throw new UserAlreadyRegisteredException("Username is already registered.");
        }
    }

    private void verifyEmail(String email) throws UserAlreadyRegisteredException {
        if (userRepository.findByEmail(email.trim().toLowerCase()).isPresent()) {
            throw new UserAlreadyRegisteredException("Email is already registered.");
        }
    }

    private void comparePasswords(String password, String passwordConfirmation) throws InvalidPasswordException {
        if (!password.equals(passwordConfirmation)) {
            throw new InvalidPasswordException("Password and password confirmation don't match.");
        }
    }

}
