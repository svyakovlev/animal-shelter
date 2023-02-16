package com.teamwork.animalshelter.controller;

import com.teamwork.animalshelter.exception.UserListIsEmpty;
import com.teamwork.animalshelter.model.User;
import com.teamwork.animalshelter.service.AdministratorService;
import com.teamwork.animalshelter.service.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("admin")
public class AdministratorController {
    private final AdministratorService administratorService;

    public AdministratorController(AdministratorService administratorService) {
        this.administratorService = administratorService;
    }

    @PostMapping("add")
    public ResponseEntity<String> addAdministrator(@RequestParam String name,
                                                   @RequestParam String phone,
                                                   @RequestParam Long chatId) {
        User user = administratorService.addAdministrator(name, phone, chatId);
        if (user == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        return ResponseEntity.ok("Запись прошла успешно.");
    }

    @GetMapping("users")
    public ResponseEntity<List<User>> getUsers() {
        List<User> users = administratorService.getUsers();
        if (users == null || users.isEmpty()) throw new UserListIsEmpty();
        return ResponseEntity.ok(users);
    }

    @ExceptionHandler(ServiceException.class)
    public String handleException(ServiceException e) {
        String result = e.getMessage() + "<br><br>Содержимое ResponseStatus: " + e.getClass().getAnnotation(ResponseStatus.class).value();
        return result;
    }
}
