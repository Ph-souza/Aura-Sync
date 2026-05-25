package com.example.data.dao

import androidx.room.*
import com.example.data.model.Budget
import com.example.data.model.GoalBox
import com.example.data.model.Task
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface GoalBoxDao {
    @Query("SELECT * FROM goal_boxes ORDER BY createdDate DESC")
    fun getAllGoalBoxes(): Flow<List<GoalBox>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalBox(goalBox: GoalBox)

    @Update
    suspend fun updateGoalBox(goalBox: GoalBox)

    @Delete
    suspend fun deleteGoalBox(goalBox: GoalBox)

    @Query("DELETE FROM goal_boxes")
    suspend fun clearAll()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets")
    suspend fun clearAll()
}
