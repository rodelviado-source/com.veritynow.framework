package com.veritynow.core.api.bdo.personal.loans;

import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/records")
@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:8080", "http://127.0.0.1:8080" })
public class RecordsController {

	private final RecordRepository repo;

	public RecordsController(RecordRepository repo) {
		this.repo = repo;
	}

	// list with search/sort/paging (unchanged)
	@GetMapping
	public PageResponse<RecordEntity> list(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "10") int size,
			@RequestParam(name = "query", required = false) String query,
			@RequestParam(name = "sort", defaultValue = "createdAt") String sortField,
			@RequestParam(name = "dir", defaultValue = "desc") String sortDir) {
		Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
		String prop = switch (sortField) {
		case "title" -> "title";
		case "priority" -> "priority";
		case "status" -> "status";
		case "id" -> "id";
		default -> "createdAt";
		};
		Pageable pageable = PageRequest.of(page, size, Sort.by(dir, prop));
		Page<RecordEntity> p = repo.search((query == null || query.isBlank()) ? null : query, pageable);
		return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements());
	}

	// create (requires agentId + clientId)
	@PostMapping
	public ResponseEntity<RecordEntity> create(@RequestBody CreateRecordRequest req) {
		if (repo.existsByAgentIdAndClientId(req.agentId, req.clientId)) {
			return ResponseEntity.badRequest().build();
		}
		RecordEntity r = new RecordEntity();
		r.setAgentId(req.agentId);
		r.setAgentFirstName(req.agentFirstName);
		r.setAgentMiddleName(req.agentMiddleName);
		r.setAgentLastName(req.agentLastName);
		r.setAgentSuffix(req.agentSuffix);

		r.setClientId(req.clientId);
		r.setClientFirstName(req.clientFirstName);
		r.setClientMiddleName(req.clientMiddleName);
		r.setClientLastName(req.clientLastName);
		r.setClientSuffix(req.clientSuffix);

		r.setTitle(req.title);
		r.setPriority(req.priority);
		r.setStatus(req.status);
		r.setDescription(req.description);
		r.setCreatedAt(req.createdAt != null ? req.createdAt : Instant.now());

		return ResponseEntity.ok(repo.save(r));
	}

	// get by key
	@GetMapping("/by-key")
	public ResponseEntity<RecordEntity> getByKey(@RequestParam String agentId, @RequestParam String clientId) {
		return repo.findByAgentIdAndClientId(agentId, clientId).map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	// update by id (keeps your old endpoint)
	@PutMapping("/{id}")
	public ResponseEntity<RecordEntity> updateById(@PathVariable Long id, @RequestBody UpdateRecordRequest req) {
		return repo.findById(id).map(r -> {
			if (req.getTitle() != null)
				r.setTitle(req.getTitle());
			if (req.getPriority() != null)
				r.setPriority(req.getPriority());
			if (req.getStatus() != null)
				r.setStatus(req.getStatus());
			if (req.getDescription() != null)
				r.setDescription(req.getDescription());

			// names
			if (req.getAgentFirstName() != null)
				r.setAgentFirstName(req.getAgentFirstName());
			if (req.getAgentMiddleName() != null)
				r.setAgentMiddleName(req.getAgentMiddleName());
			if (req.getAgentLastName() != null)
				r.setAgentLastName(req.getAgentLastName());
			if (req.getAgentSuffix() != null)
				r.setAgentSuffix(req.getAgentSuffix());
			if (req.getClientFirstName() != null)
				r.setClientFirstName(req.getClientFirstName());
			if (req.getClientMiddleName() != null)
				r.setClientMiddleName(req.getClientMiddleName());
			if (req.getClientLastName() != null)
				r.setClientLastName(req.getClientLastName());
			if (req.getClientSuffix() != null)
				r.setClientSuffix(req.getClientSuffix());

			return ResponseEntity.ok(repo.save(r));
		}).orElse(ResponseEntity.notFound().build());
	}

	// update by key (e.g. change names, title, etc.; key remains)
	@PutMapping("/by-key")
	public ResponseEntity<RecordEntity> updateByKey(@RequestParam String agentId, @RequestParam String clientId,
			@RequestBody UpdateRecordRequest req) {
		return repo.findByAgentIdAndClientId(agentId, clientId).map(r -> {
			if (req.getTitle() != null)
				r.setTitle(req.getTitle());
			if (req.getPriority() != null)
				r.setPriority(req.getPriority());
			if (req.getStatus() != null)
				r.setStatus(req.getStatus());
			if (req.getDescription() != null)
				r.setDescription(req.getDescription());
			if (req.getAgentFirstName() != null)
				r.setAgentFirstName(req.getAgentFirstName());
			if (req.getAgentMiddleName() != null)
				r.setAgentMiddleName(req.getAgentMiddleName());
			if (req.getAgentLastName() != null)
				r.setAgentLastName(req.getAgentLastName());
			if (req.getAgentSuffix() != null)
				r.setAgentSuffix(req.getAgentSuffix());
			if (req.getClientFirstName() != null)
				r.setClientFirstName(req.getClientFirstName());
			if (req.getClientMiddleName() != null)
				r.setClientMiddleName(req.getClientMiddleName());
			if (req.getClientLastName() != null)
				r.setClientLastName(req.getClientLastName());
			if (req.getClientSuffix() != null)
				r.setClientSuffix(req.getClientSuffix());
			return ResponseEntity.ok(repo.save(r));
		}).orElse(ResponseEntity.notFound().build());
	}

	// delete by key
	@DeleteMapping("/by-key")
	public ResponseEntity<?> deleteByKey(@RequestParam String agentId, @RequestParam String clientId) {
		return repo.findByAgentIdAndClientId(agentId, clientId).map(r -> {
			repo.delete(r);
			return ResponseEntity.noContent().build();
		}).orElse(ResponseEntity.notFound().build());
	}
}
