package com.canopas.catchme.ui.flow.home.map.member

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.canopas.catchme.data.models.location.ApiLocation
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await

class LocationHistoryPagingSource(private var query: Query) :
    PagingSource<QuerySnapshot, ApiLocation>() {

    override fun getRefreshKey(state: PagingState<QuerySnapshot, ApiLocation>): QuerySnapshot? =
        null

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, ApiLocation> =
        try {
            val currentPage = params.key ?: query.get().await()
            if (currentPage.isEmpty) {
                val lists = currentPage.toObjects(ApiLocation::class.java)
                LoadResult.Page(data = lists, prevKey = null, nextKey = null)
            } else {
                val lastVisibleProduct = currentPage.documents[currentPage.size() - 1]
                val nextPage = query.startAfter(lastVisibleProduct).get().await()
                val lists = currentPage.toObjects(ApiLocation::class.java)

                LoadResult.Page(
                    data = lists,
                    prevKey = null,
                    nextKey = nextPage
                )
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
}
