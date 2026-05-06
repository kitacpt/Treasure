package com.treasure

import android.app.Application
import com.treasure.core.repo.ItemRepository
import com.treasure.core.repo.RoomItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Custom Application that wires up the dependency graph and runs first-launch
 * seeding. Hand-rolled ServiceLocator — no Hilt/Koin until it earns its keep.
 */
class TreasureApp : Application() {
    lateinit var repository: ItemRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        repository = RoomItemRepository.create(this)
        appScope.launch { repository.ensureSeeded() }
    }
}
