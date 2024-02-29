package com.canopas.yourspace.ui.flow.messages.chat.components

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.canopas.yourspace.data.models.messages.ApiThreadMessage
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await

class MessagesPagingSource(private var query: Query) :
    PagingSource<QuerySnapshot, ApiThreadMessage>() {
    override fun getRefreshKey(state: PagingState<QuerySnapshot, ApiThreadMessage>): QuerySnapshot? {
        return null
    }

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, ApiThreadMessage> =
        try {
            val currentPage = params.key ?: query.get().await()
            if (currentPage.isEmpty) {
                val lists = currentPage.toObjects(ApiThreadMessage::class.java)
                LoadResult.Page(data = lists, prevKey = null, nextKey = null)
            } else {
                val lastVisibleProduct = currentPage.documents[currentPage.size() - 1]
                val nextPage = query.startAfter(lastVisibleProduct).get().await()
                val lists = currentPage.toObjects(ApiThreadMessage::class.java)

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