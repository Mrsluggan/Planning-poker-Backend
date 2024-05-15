package com.planningpokerbackend.planningpokerbackend.services;

import java.util.List;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.planningpokerbackend.planningpokerbackend.models.Project;

@Service
public class ProjectService {
    
    private final MongoOperations mongoOperations;

    public ProjectService (MongoOperations mongoOperations) {
        this.mongoOperations=mongoOperations;
    }

    public Project createProject(Project project) {
        mongoOperations.save(project);
        return project;
    }

    public List<Project> getAllProjects() {
       return mongoOperations.findAll(Project.class);
    }

    public Project getProjectById(String projectId) {
        Query query = new Query(Criteria.where("id").is(projectId));
        return mongoOperations.findOne(query, Project.class);
    }

    public void deleteProject(String projectId) {
        Query query = new Query(Criteria.where("id").is(projectId));
        mongoOperations.remove(query, Project.class);
    }
}
