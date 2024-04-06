package com.canopas.yourspace.ui.flow.home.map.member

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.canopas.yourspace.data.models.location.LocationJourney
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await

class LocationHistoryPagingSource(private var query: Query) :
    PagingSource<QuerySnapshot, LocationJourney>() {

    override fun getRefreshKey(state: PagingState<QuerySnapshot, LocationJourney>): QuerySnapshot? =
        null

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, LocationJourney> =
        try {
            val currentPage = params.key ?: query.get().await()
            val lists = currentPage.toObjects(LocationJourney::class.java)

            val nextPage = if (currentPage.isEmpty) {
                null
            } else {
                val lastVisibleProduct = currentPage.documents.last()
                query.startAfter(lastVisibleProduct).get().await()
            }

            LoadResult.Page(data = lists, prevKey = null, nextKey = nextPage)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
}
