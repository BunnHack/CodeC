package com.example.db

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun insert(project: ProjectEntity) {
        projectDao.insertProject(project)
    }

    suspend fun deleteById(id: String) {
        projectDao.deleteProjectById(id)
    }
}
