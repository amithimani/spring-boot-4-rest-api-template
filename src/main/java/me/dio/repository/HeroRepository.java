package me.dio.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import me.dio.model.Hero;

public interface HeroRepository extends JpaRepository<Hero, Long> {
}
