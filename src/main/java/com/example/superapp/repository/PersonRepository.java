package com.example.superapp.repository;

import com.example.superapp.entity.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {
	// support simple name search with paging
	Page<Person> findByNameContainingIgnoreCase(String name, Pageable pageable);

	// fetch by name without paging to allow server-side sort by computed fields like credits count
	java.util.List<Person> findByNameContainingIgnoreCase(String name);
}
