package com.veritynow.core.api.bdo.personal.loans;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:8080", "http://127.0.0.1:8080" })
public class UserController {
	private final UserRepository repo;

	public UserController(UserRepository repo) {
		this.repo = repo;
	}

	@GetMapping
	public List<UserEntity> list() {
		return repo.findAll();
	}

	@PostMapping
	public ResponseEntity<UserEntity> create(@RequestBody UserEntity u) {
		return ResponseEntity.ok(repo.save(u));
	}
}