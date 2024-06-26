package com.planningpokerbackend.planningpokerbackend.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.planningpokerbackend.planningpokerbackend.models.Project;
import com.planningpokerbackend.planningpokerbackend.models.Task;

import jakarta.annotation.PostConstruct;

@Service
public class TaskService {

    private final MongoOperations mongoOperations;
    private final ProjectService projectService;

    public TaskService(MongoOperations mongoOperations, ProjectService projectService) {
        this.mongoOperations = mongoOperations;
        this.projectService = projectService;
    }

    public List<Task> getAllTasks() {
        return mongoOperations.findAll(Task.class);
    }

    public Task getTaskById(String taskId) {
        return mongoOperations.findById(taskId, Task.class);
    }

    public List<Task> getTasksByProjectId(String projectId) {
        Query query = Query.query(Criteria.where("projectId").is(projectId));
        return mongoOperations.find(query, Task.class);
    }

    public Task createNewTask(String projectId, Task task) {
        Project project = projectService.getProjectById(projectId);
        if (project == null) {
            return null;
        }
        task.setProjectId(projectId);
        Task savedTask = mongoOperations.save(task);
        project.getTasks().add(savedTask);
        mongoOperations.save(project);
        return savedTask;
    }

    public void removeTask(String taskId) {
        Task taskToRemove = getTaskById(taskId);
        if (taskToRemove != null) {
            String projectId = taskToRemove.getProjectId();
            mongoOperations.remove(taskToRemove);

            Project project = projectService.getProjectById(projectId);
            if (project != null) {
                project.getTasks().removeIf(task -> task.getId().equals(taskId));
                mongoOperations.save(project);
            }
        }
    }

    public Task startTask(String taskId) {
        Task task = getTaskById(taskId);
        task.startTimer(); 
        Task updatedTask = mongoOperations.save(task);
        updateProjectWithTask(updatedTask);
        return updatedTask;
    }

    public Task stopTask(String taskId) {
        Task task = getTaskById(taskId);
        task.pauseTimer();
        Task updatedTask = mongoOperations.save(task);
        updateProjectWithTask(updatedTask);
        return updatedTask;
    }

    private void updateProjectWithTask(Task updatedTask) {
        String projectId = updatedTask.getProjectId();
        Project project = projectService.getProjectById(projectId);
    
        if (project != null) {
            List<Task> tasks = project.getTasks();
            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i).getId().equals(updatedTask.getId())) {
                    tasks.set(i, updatedTask);
                    break;
                }
            }
            mongoOperations.save(project);
        }
    }

    public ResponseEntity<Task> updateTaskTimeEstimation(String taskId, String userId, int timeEstimation) {
        Task task = getTaskById(taskId);
        if (task != null) {

            if (!task.getUserTimeEstimations().containsKey(userId)) {
                task.getUserTimeEstimations().put(userId, timeEstimation);

                task.setTimeEstimation(task.getTimeEstimation() + timeEstimation);

                int medianValue = task.getTimeEstimation() / task.getUserTimeEstimations().size();

                System.out.println(medianValue);
                Task savedTask = mongoOperations.save(task);

                Project project = projectService.getProjectById(task.getProjectId());
                if (project != null) {

                    for (Task projectTask : project.getTasks()) {
                        if (projectTask.getId().equals(taskId)) {
                            projectTask.getUserTimeEstimations().put(userId, timeEstimation);
                            break;
                        }
                    }

                    mongoOperations.save(project);
                }
                return ResponseEntity.ok().body(savedTask);
            } else {

                return ResponseEntity.status(409).body(task);
            }

        }
        return null;
    }

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    @PostConstruct
    public void startTimerUpdateTask() {
        executorService.scheduleAtFixedRate(this::updateTimers, 0, 1, TimeUnit.MINUTES);
    }

    private void updateTimers() {
        List<Task> tasks = getAllTasks();
        for (Task task : tasks) {
            task.updateTimer();
            mongoOperations.save(task);
        }
    }
}
