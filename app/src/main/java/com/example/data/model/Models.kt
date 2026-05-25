package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String, // e.g. "Alimentação", "Transporte", "Lazer", "Outros"
    val isExpense: Boolean, // true = Expense, false = Income
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "goal_boxes")
data class GoalBox(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val createdDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val priority: String, // "Alta", "Média", "Baixa"
    val dueDate: Long,
    val isCompleted: Boolean = false
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // e.g. "Alimentação", "Transporte", "Lazer", "Outros"
    val limitAmount: Double
)
