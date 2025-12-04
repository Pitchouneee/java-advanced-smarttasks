package fr.corentinbringer.smarttasks.repository;

import fr.corentinbringer.smarttasks.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

}
