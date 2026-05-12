package com.spbu.projecttrack.rating.data.repository

import com.spbu.projecttrack.rating.data.model.RankingData
import com.spbu.projecttrack.rating.data.model.RankingFilters

interface IRankingRepository {
    suspend fun loadRatings(
        filters: RankingFilters,
        forceRefresh: Boolean = false,
    ): Result<RankingData>
}
