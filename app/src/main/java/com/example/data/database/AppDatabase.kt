package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BudgetDao
import com.example.data.dao.GoalBoxDao
import com.example.data.dao.TaskDao
import com.example.data.dao.TransactionDao
import com.example.data.model.Budget
import com.example.data.model.GoalBox
import com.example.data.model.Task
import com.example.data.model.Transaction

@Database(
    entities = [Transaction::class, GoalBox::class, Task::class, Budget::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun goalBoxDao(): GoalBoxDao
    abstract fun taskDao(): TaskDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "organizador_inteligente_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
