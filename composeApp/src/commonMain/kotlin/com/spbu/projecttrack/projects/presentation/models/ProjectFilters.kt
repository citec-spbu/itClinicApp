package com.spbu.projecttrack.projects.presentation.models

data class ProjectFilters(
    val selectedTags: Set<String> = emptySet(),
    val enrollmentStartDate: String? = null,
    val enrollmentEndDate: String? = null,
    val projectStartDate: String? = null,
    val projectEndDate: String? = null
) {
    fun hasActiveFilters(): Boolean {
        return selectedTags.isNotEmpty() || 
               enrollmentStartDate != null || 
               enrollmentEndDate != null ||
               projectStartDate != null ||
               projectEndDate != null
    }
    
    fun clear(): ProjectFilters {
        return ProjectFilters()
    }
}









