package com.example.bankguide.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideTaskRepository extends JpaRepository<GuideTask, String> {

    List<GuideTask> findAllByOrderByPriorityAsc();
}
