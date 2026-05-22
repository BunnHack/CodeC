package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String, // Project ID or Path
    val name: String,
    val path: String,
    val lastOpenedAt: Long = System.currentTimeMillis()
)
