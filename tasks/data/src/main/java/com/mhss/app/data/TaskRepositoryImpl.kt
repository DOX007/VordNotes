package com.mhss.app.data

import com.mhss.app.database.dao.TaskDao
import com.mhss.app.database.entity.toTask
import com.mhss.app.database.entity.toTaskEntity
import com.mhss.app.domain.model.Task
import com.mhss.app.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mhss.app.database.remote.await
import com.mhss.app.database.remote.toTaskDoc
import com.mhss.app.widget.WidgetUpdater

import com.mhss.app.database.entity.TaskEntity
import com.mhss.app.database.remote.taskDocToEntity


@Single
class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    @Named("ioDispatcher") private val ioDispatcher: CoroutineDispatcher,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val widgetUpdater: WidgetUpdater
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks()
            .flowOn(ioDispatcher)
            .map { tasks ->
                tasks.map { it.toTask() }
            }
    }

    override suspend fun getTaskById(id: Int): Task {
        return withContext(ioDispatcher) {
            taskDao.getTask(id).toTask()
        }
    }

    override fun searchTasks(title: String): Flow<List<Task>> {
        return taskDao.getTasksByTitle(title)
            .flowOn(ioDispatcher)
            .map { tasks ->
                tasks.map { it.toTask() }
            }
    }

    override suspend fun insertTask(task: Task): Long {
        val newId = withContext(ioDispatcher) {
            val id = taskDao.insertTask(task.toTaskEntity())
            val u = auth.currentUser
            if (u != null && id != 0L) {
                val entity = task.toTaskEntity().copy(id = id.toInt())
                firestore.collection("workspace").document("shared")
                    .collection("tasks").document(entity.id.toString())
                    .set(toTaskDoc(entity))
                    .await()
            }
            id
        }

        notifyTasksWidget()
        return newId
    }

    override suspend fun updateTask(task: Task) {
        withContext(ioDispatcher) {
            taskDao.updateTask(task.toTaskEntity())
            val u = auth.currentUser
            if (u != null && task.id != 0) {
                val entity = task.toTaskEntity()
                firestore.collection("workspace").document("shared")
                    .collection("tasks").document(entity.id.toString())
                    .set(toTaskDoc(entity))
                    .await()
            }
        }

        notifyTasksWidget()
    }

    override suspend fun completeTask(id: Int, completed: Boolean) {
        withContext(ioDispatcher) {
            taskDao.updateCompleted(id, completed)
        }

        notifyTasksWidget()
    }


    suspend fun downSyncTasksFromFirebase(): Boolean {
        return withContext(ioDispatcher) {
            try {
                val user = auth.currentUser ?: return@withContext false

                val snapshot = firestore
                    .collection("workspace")
                    .document("shared")
                    .collection("tasks")
                    .get()
                    .await()

                val entities = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null

                    // 🔍 DEBUG – HÄR SKA LOGGEN LIGGA
                    println(
                        "FirestoreTasks | " +
                                "docId=${doc.id} | " +
                                "keys=${data.keys} | " +
                                "subTasksExists=${data.containsKey("subTasks")} | " +
                                "subTasksValue=${data["subTasks"]}"
                    )

                    taskDocToEntity(data)
                }

                taskDao.insertTasks(entities)
                true
            } catch (e: Exception) {
                false
            }
        }
    }


    override suspend fun deleteTask(task: Task) {
        withContext(ioDispatcher) {
            val entity = task.toTaskEntity()
            taskDao.deleteTask(entity)

            val u = auth.currentUser
            if (u != null && entity.id != 0) {
                firestore.collection("workspace").document("shared")
                    .collection("tasks").document(entity.id.toString())
                    .delete()
                    .await()
            }
        }

        notifyTasksWidget()
    }

    // ✅ HJÄLPFUNKTIONEN SKA LIGGA HÄR
    private suspend fun notifyTasksWidget() {
        try {
            widgetUpdater.updateAll(WidgetUpdater.WidgetType.Tasks)
        } catch (e: Exception) {
            // Logga tyst – widget-uppdatering är inte kritisk
            // t.ex. Timber.e(e, "Failed to update tasks widget")
        }
    }
}