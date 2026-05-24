package com.space_explorer.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.space_explorer.data.local.dao.FavoriteDao
import com.space_explorer.data.local.database.SpaceExplorerDatabase
import com.space_explorer.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FavoriteDaoTest {

    private lateinit var database: SpaceExplorerDatabase
    private lateinit var dao: FavoriteDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SpaceExplorerDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.favoriteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and observeAll returns inserted item`() = runTest {
        val entity = sampleEntity("2026-05-22")
        dao.insert(entity)

        val all = dao.observeAll().first()
        assertThat(all).hasSize(1)
        assertThat(all.first().id).isEqualTo("2026-05-22")
    }

    @Test
    fun `exists returns true after insertion`() = runTest {
        dao.insert(sampleEntity("2026-05-22"))
        assertThat(dao.exists("2026-05-22")).isTrue()
        assertThat(dao.exists("2026-05-21")).isFalse()
    }

    @Test
    fun `deleteById removes entity`() = runTest {
        dao.insert(sampleEntity("2026-05-22"))
        dao.deleteById("2026-05-22")
        assertThat(dao.exists("2026-05-22")).isFalse()
    }

    @Test
    fun `observeFavoriteIds emits ids only`() = runTest {
        dao.insert(sampleEntity("2026-05-22"))
        dao.insert(sampleEntity("2026-05-21"))

        val ids = dao.observeFavoriteIds().first()
        assertThat(ids).containsExactly("2026-05-22", "2026-05-21")
    }

    @Test
    fun `insert with same id replaces existing entity`() = runTest {
        dao.insert(sampleEntity("2026-05-22", title = "First"))
        dao.insert(sampleEntity("2026-05-22", title = "Second"))

        val list = dao.observeAll().first()
        assertThat(list).hasSize(1)
        assertThat(list.first().title).isEqualTo("Second")
    }

    private fun sampleEntity(id: String, title: String = "Mars") = FavoriteEntity(
        id = id,
        date = id,
        title = title,
        explanation = "explanation",
        imageUrl = "https://image/$id.jpg",
        hdImageUrl = null,
        videoUrl = null,
        mediaType = "image",
        copyright = null,
    )
}
