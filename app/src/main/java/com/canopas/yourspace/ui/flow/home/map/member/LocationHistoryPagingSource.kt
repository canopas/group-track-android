package com.canopas.yourspace.ui.flow.home.map.member

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.UserState
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await

class LocationHistoryPagingSource(private var query: Query) :
    PagingSource<QuerySnapshot, ApiLocation>() {

    private var isLoadedFirstTime = MutableStateFlow(true)

    override fun getRefreshKey(state: PagingState<QuerySnapshot, ApiLocation>): QuerySnapshot? =
        null

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, ApiLocation> =
        try {
            val currentPage = params.key ?: query.get().await()
            val lists = currentPage.toObjects(ApiLocation::class.java)
            var filteredList = lists.filter {
                it.user_state == UserState.REST_POINT.value
            }

            if (isLoadedFirstTime.value) {
                isLoadedFirstTime.value = false
                if (filteredList.firstOrNull()?.id != lists.firstOrNull()?.id) {
                    filteredList = listOf(lists.firstOrNull()) + filteredList
                }
            }

            val nextPage = if (currentPage.isEmpty) {
                null
            } else {
                val lastVisibleProduct = currentPage.documents.last()
                query.startAfter(lastVisibleProduct).get().await()
            }

            LoadResult.Page(data = filteredList, prevKey = null, nextKey = nextPage)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
}
