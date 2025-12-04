package fr.corentinbringer.smarttasks.project.repository;

import fr.corentinbringer.smarttasks.project.model.Project;
import fr.corentinbringer.smarttasks.project.model.ProjectListResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("""
           SELECT new fr.corentinbringer.smarttasks.project.model.ProjectListResponse(
               p.id,
               p.name,
               p.createdOn
           )
           FROM Project p
           """)
    List<ProjectListResponse> findAllList();
}
