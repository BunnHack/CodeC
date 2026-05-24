package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.AppDatabase
import com.example.db.ProjectEntity
import com.example.db.ProjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProjectRepository

    val projects: StateFlow<List<ProjectEntity>>

    init {
        val projectDao = AppDatabase.getDatabase(application).projectDao()
        repository = ProjectRepository(projectDao)
        projects = repository.allProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun createNewProject(name: String, template: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val projectId = UUID.randomUUID().toString()
            val project = ProjectEntity(
                id = projectId,
                name = name,
                path = "/storage/emulated/0/Documents/CodeEditor/$name", // Simplified for demo
                lastOpenedAt = System.currentTimeMillis(),
                template = template
            )
            repository.insert(project)
            onCreated(projectId)
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun updateLastOpened(project: ProjectEntity) {
        viewModelScope.launch {
            repository.insert(project.copy(lastOpenedAt = System.currentTimeMillis()))
        }
    }
}
