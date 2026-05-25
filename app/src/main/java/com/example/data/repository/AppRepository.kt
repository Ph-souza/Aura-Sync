package com.example.data.repository

import com.example.data.dao.BudgetDao
import com.example.data.dao.GoalBoxDao
import com.example.data.dao.TaskDao
import com.example.data.dao.TransactionDao
import com.example.data.model.Budget
import com.example.data.model.GoalBox
import com.example.data.model.Task
import com.example.data.model.Transaction
import com.example.data.api.GeminiApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class AppRepository(
    private val transactionDao: TransactionDao,
    private val goalBoxDao: GoalBoxDao,
    private val taskDao: TaskDao,
    private val budgetDao: BudgetDao,
    private val prefs: android.content.SharedPreferences
) {
    // --- Transactions ---
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    suspend fun insertTransaction(transaction: Transaction) = transactionDao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: Transaction) = transactionDao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)

    // --- GoalBoxes ---
    val allGoalBoxes: Flow<List<GoalBox>> = goalBoxDao.getAllGoalBoxes()
    suspend fun insertGoalBox(goalBox: GoalBox) = goalBoxDao.insertGoalBox(goalBox)
    suspend fun updateGoalBox(goalBox: GoalBox) = goalBoxDao.updateGoalBox(goalBox)
    suspend fun deleteGoalBox(goalBox: GoalBox) = goalBoxDao.deleteGoalBox(goalBox)

    // --- Tasks ---
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    suspend fun insertTask(task: Task) = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    // --- Budgets ---
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()
    suspend fun insertBudget(budget: Budget) = budgetDao.insertBudget(budget)
    suspend fun updateBudget(budget: Budget) = budgetDao.updateBudget(budget)
    suspend fun deleteBudget(budget: Budget) = budgetDao.deleteBudget(budget)

    // --- Clean DB method for testing/reset ---
    suspend fun populateInitialData() {
        val trans = allTransactions.firstOrNull() ?: emptyList()
        if (trans.isNotEmpty()) return

        // Insert standard sample transactions
        insertTransaction(Transaction(title = "Salário Mensal", amount = 5000.00, category = "Salário", isExpense = false))
        insertTransaction(Transaction(title = "Freela Design", amount = 850.00, category = "Freelancer", isExpense = false))
        insertTransaction(Transaction(title = "Supermercado Extra", amount = 350.00, category = "Alimentação", isExpense = true))
        insertTransaction(Transaction(title = "Combustível", amount = 120.00, category = "Transporte", isExpense = true))
        insertTransaction(Transaction(title = "Assinatura Netflix", amount = 55.90, category = "Lazer", isExpense = true))
        insertTransaction(Transaction(title = "Spotify Premium", amount = 24.90, category = "Lazer", isExpense = true))
        insertTransaction(Transaction(title = "Restaurante", amount = 80.00, category = "Alimentação", isExpense = true))

        // Create standard Caixinhas de Investimento
        insertGoalBox(GoalBox(name = "Reserva de Emergência", targetAmount = 10000.0, currentAmount = 2500.0))
        insertGoalBox(GoalBox(name = "Moto Nova", targetAmount = 15000.0, currentAmount = 1200.0))
        insertGoalBox(GoalBox(name = "Viagem de Fim de Ano", targetAmount = 5000.0, currentAmount = 800.0))

        // Create standard task list
        insertTask(Task(title = "Revisar despesas de transporte", priority = "Média", dueDate = System.currentTimeMillis()))
        insertTask(Task(title = "Pagar conta de energia", priority = "Alta", dueDate = System.currentTimeMillis() + 86400000))
        insertTask(Task(title = "Ligar para a contabilidade", priority = "Alta", dueDate = System.currentTimeMillis(), isCompleted = true))

        // Predefined budget caps
        insertBudget(Budget(category = "Alimentação", limitAmount = 800.0))
        insertBudget(Budget(category = "Transporte", limitAmount = 400.0))
        insertBudget(Budget(category = "Lazer", limitAmount = 300.0))
        insertBudget(Budget(category = "Outros", limitAmount = 250.0))
    }

    // --- IA / NLP Actions ---

    // Analyzes the current DB state and generates AI suggestion
    suspend fun getSmartInsights(profileContext: String = ""): String {
        val lastCallTime = prefs.getLong("LAST_GEMINI_CALL", 0L)
        val currentTime = System.currentTimeMillis()
        val twelveHours = 12 * 60 * 60 * 1000L

        if (currentTime - lastCallTime < twelveHours) {
            val cachedInsight = prefs.getString("LAST_GEMINI_INSIGHT", null)
            if (!cachedInsight.isNullOrEmpty()) {
                return cachedInsight
            }
        }

        try {
            val trans = allTransactions.firstOrNull() ?: emptyList()
            val boxes = allGoalBoxes.firstOrNull() ?: emptyList()
            val tasks = allTasks.firstOrNull() ?: emptyList()
            val budg = allBudgets.firstOrNull() ?: emptyList()

            // Gather statistics for context
            val totalIncome = trans.filter { !it.isExpense }.sumOf { it.amount }
            val totalExpense = trans.filter { it.isExpense }.sumOf { it.amount }
            val expensesByCategory = trans.filter { it.isExpense }.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val transSummary = "Total Receitas: R$ $totalIncome, Total Despesas: R$ $totalExpense. Despesas por categoria: ${expensesByCategory.entries.joinToString { "${it.key}: R$ ${it.value}" }}"
            val goalsSummary = boxes.joinToString { "${it.name}: R$ ${it.currentAmount} de R$ ${it.targetAmount}" }
            val tasksSummary = "Total: ${tasks.size}, Pendentes: ${tasks.count { !it.isCompleted }}"
            val budgetsSummary = budg.joinToString { "${it.category}: limite R$ ${it.limitAmount}" }

            val systemInstruction = """
                $profileContext
                Você é um mentor financeiro e de produtividade extremamente habilidoso, amigável e focado.
                Analise os dados reais do usuário do 'Aura Sync' e ofereça conselhos rápidos, acionáveis e motivadores. Adeque o tom ao perfil de idade do usuário, se aplicável.
                Diga qual caixinha ele deve focar para o aporte, se tem alguma categoria de gasto perigosa perto de estourar o limite, e se há sobrecarga de tarefas pendentes.
                Escreva um texto direto, motivador, em no máximo 100 palavras. Use marcadores curtos para destacar os conselhos principais.
            """.trimIndent()

            val prompt = """
                Resumo do Banco de Dados Local:
                $transSummary
                Metas Atuais (Caixinhas): $goalsSummary
                Lista de Tarefas: $tasksSummary
                Orçamentos Estipulados: $budgetsSummary
            """.trimIndent()

            val result = GeminiApiClient.callGemini(prompt, systemInstruction = systemInstruction)
            
            if (!result.contains("Erro:") && !result.contains("Falha") && result.isNotEmpty()) {
                prefs.edit()
                    .putLong("LAST_GEMINI_CALL", currentTime)
                    .putString("LAST_GEMINI_INSIGHT", result)
                    .apply()
            }
            return result
        } catch (e: Exception) {
            return "Aura Sync está processando seus dados offline no momento. Seus novos insights chegam em breve!"
        }
    }

    // Ask AI for the optimal notification delivery time based on completions/activity hours or random behavior simulation
    suspend fun getOptimalNotificationRecommendation(): String {
        val lastCallTime = prefs.getLong("LAST_GEMINI_TIME_CALL", 0L)
        val currentTime = System.currentTimeMillis()
        val twelveHours = 12 * 60 * 60 * 1000L

        if (currentTime - lastCallTime < twelveHours) {
            val cachedTime = prefs.getString("LAST_GEMINI_TIME_INSIGHT", null)
            if (!cachedTime.isNullOrEmpty()) {
                return cachedTime
            }
        }

        try {
            // Collect timestamps of tasks to see activity
            val tasks = allTasks.firstOrNull() ?: emptyList()
            val activityNotes = if (tasks.isEmpty()) {
                "Sem tarefas salvas no banco"
            } else {
                "O usuário possui ${tasks.size} tarefas, sendo ${tasks.count { it.isCompleted }} concluídas com sucesso."
            }

            val prompt = """
                $activityNotes.
                Com base nesta atividade, recomende qual o melhor horário ideal ('Optimal Time') para enviar lembretes de despesas e produtividade para este perfil de usuário para garantir engajamento.
                Responda em PORTUGUÊS com um parágrafo amigável explicativo de no máximo 2 linhas e termine escrevendo o horário escolhido de forma isolada no final na forma 'Horário sugerido: HH:MM'.
            """.trimIndent()

            val result = GeminiApiClient.callGemini(prompt, systemInstruction = "Você é um scheduler inteligente focado em hábitos de produtividade.")
            
            if (!result.contains("Erro:") && !result.contains("Falha") && result.isNotEmpty()) {
                prefs.edit()
                    .putLong("LAST_GEMINI_TIME_CALL", currentTime)
                    .putString("LAST_GEMINI_TIME_INSIGHT", result)
                    .apply()
            }
            return result
        } catch (e: Exception) {
            return "Aura Sync está processando seus dados offline no momento. Seus novos insights chegam em breve!"
        }
    }
}
