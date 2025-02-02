package sample.gthio.tasks.data.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import sample.gthio.tasks.data.model.DataGroup
import sample.gthio.tasks.domain.model.DomainGroup
import sample.gthio.tasks.domain.model.GroupColor
import java.util.*

interface GroupLocalSource {
    val groups: Flow<List<DataGroup>>

    val groupColors: List<GroupColor>

    val availableColors: Flow<List<GroupColor>>

    suspend fun insert(group: DomainGroup)

    suspend fun deleteById(id: UUID)

    suspend fun update(group: DomainGroup)

    suspend fun getById(id: UUID): DataGroup?

    suspend fun observeById(id: UUID): Flow<DataGroup?>
}

@OptIn(ExperimentalCoroutinesApi::class)
fun inMemoryGroupSource(): GroupLocalSource = object : GroupLocalSource {
    private val _group = MutableStateFlow<List<DataGroup>>(emptyList())
    override val groups: Flow<List<DataGroup>> = _group.asStateFlow()

    override val groupColors: List<GroupColor> = GroupColor.values().toList()
    override val availableColors: Flow<List<GroupColor>>
        get() = _group.map { groups ->
            groupColors.filterNot { groupColor -> groupColor.id in groups.map { it.colorCode } }
        }

    override suspend fun insert(group: DomainGroup) {
        withContext(Dispatchers.IO) {
            _group.update { groups -> groups + DataGroup.from(group) }
        }
    }

    override suspend fun deleteById(id: UUID) {
        withContext(Dispatchers.IO) {
            _group.update { groups -> groups.filterNot { group -> group.id == id } }
        }
    }

    override suspend fun update(group: DomainGroup) {
        withContext(Dispatchers.IO) {
            _group.update { groups -> groups.map { if (it.id == group.id) DataGroup.from(group) else it } }
        }
    }

    override suspend fun getById(id: UUID): DataGroup? {
        return withContext(Dispatchers.IO) {
            _group.value.find { group -> group.id == id }
        }
    }

    override suspend fun observeById(id: UUID): Flow<DataGroup?> {
        return groups
            .map { groups -> groups.find { group -> group.id == id } }
            .flowOn(Dispatchers.IO)
    }

}