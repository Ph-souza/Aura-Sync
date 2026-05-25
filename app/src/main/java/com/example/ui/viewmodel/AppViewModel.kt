package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Budget
import com.example.data.model.GoalBox
import com.example.data.model.Task
import com.example.data.model.Transaction
import com.example.data.repository.AppRepository
import com.example.data.api.GeminiApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppRepository(
        db.transactionDao(),
        db.goalBoxDao(),
        db.taskDao(),
        db.budgetDao(),
        prefs
    )

    // --- User Profile ---
    private val _userName = MutableStateFlow(prefs.getString("user_name", "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userAge = MutableStateFlow(prefs.getInt("user_age", 0))
    val userAge: StateFlow<Int> = _userAge.asStateFlow()

    fun saveUserProfile(name: String, age: Int) {
        prefs.edit().putString("user_name", name).putInt("user_age", age).apply()
        _userName.value = name
        _userAge.value = age
        refreshAiInsights()
    }

    // --- Database Streams ---
    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goalBoxes: StateFlow<List<GoalBox>> = repository.allGoalBoxes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- UI/UX States ---
    private val _aiAdvice = MutableStateFlow<String>("Carregando analíticos e conselhos da Gemini IA...")
    val aiAdvice: StateFlow<String> = _aiAdvice.asStateFlow()

    private val _optimalTimeRecommendation = MutableStateFlow<String>("Calculando horário ótimo por comportamento de uso...")
    val optimalTimeRecommendation: StateFlow<String> = _optimalTimeRecommendation.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf(
            "Olá! Eu sou o assistente inteligente do seu Organizador. Analisei seus padrões de gastos locais e produtividade recente. Em que posso te ajudar hoje?" to false
        )
    )
    val chatHistory: StateFlow<List<Pair<String, Boolean>>> = _chatHistory.asStateFlow()

    private val _isProcessingInput = MutableStateFlow<Boolean>(false)
    val isProcessingInput: StateFlow<Boolean> = _isProcessingInput.asStateFlow()

    // Status message for Voice Input processing results
    private val _smartInputFeedback = MutableStateFlow<String?>(null)
    val smartInputFeedback: StateFlow<String?> = _smartInputFeedback.asStateFlow()

    init {
        viewModelScope.launch {
            // First feed standard mock demo entries if empty
            repository.populateInitialData()
            refreshAiInsights()
        }
    }

    fun refreshAiInsights() {
        viewModelScope.launch {
            // Push changes to Firebase so the Web dashboard can read it
            com.example.data.api.FirebaseSyncManager.pushToFirebase(repository)

            _aiAdvice.value = "Analisando transações, metas e tarefas..."
            val ageStr = if (userAge.value > 0) "idade de ${userAge.value} anos" else "idade não informada"
            val profileStr = "Perfil do usuário: Nome: ${userName.value}, $ageStr. Adapte os conselhos do assistente para esse momento de vida."
            val advice = repository.getSmartInsights(profileStr)
            _aiAdvice.value = advice

            _optimalTimeRecommendation.value = "Consultando padrões de notificação de hábito..."
            val timeRec = repository.getOptimalNotificationRecommendation()
            _optimalTimeRecommendation.value = timeRec
        }
    }

    // --- Financial transactions operations ---
    fun addManualTransaction(title: String, amount: Double, category: String, isExpense: Boolean) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    title = title,
                    amount = amount,
                    category = category,
                    isExpense = isExpense
                )
            )
            refreshAiInsights()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            refreshAiInsights()
        }
    }

    // --- GoalBoxes operations ---
    fun addGoalBox(name: String, targetAmount: Double, initialAmount: Double) {
        viewModelScope.launch {
            repository.insertGoalBox(
                GoalBox(
                    name = name,
                    targetAmount = targetAmount,
                    currentAmount = initialAmount
                )
            )
            refreshAiInsights()
        }
    }

    fun depositToGoalBox(goalBoxId: Int, amount: Double) {
        viewModelScope.launch {
            val list = goalBoxes.value
            val target = list.find { it.id == goalBoxId }
            if (target != null) {
                repository.updateGoalBox(
                    target.copy(currentAmount = target.currentAmount + amount)
                )
                // Optionally deduct the same amount as an expense or direct transfer representation
                repository.insertTransaction(
                    Transaction(
                        title = "Aporte: ${target.name}",
                        amount = amount,
                        category = "Investimentos",
                        isExpense = true
                    )
                )
                refreshAiInsights()
            }
        }
    }

    fun deleteGoalBox(goalBox: GoalBox) {
        viewModelScope.launch {
            repository.deleteGoalBox(goalBox)
            refreshAiInsights()
        }
    }

    // --- Tasks operations ---
    fun addTask(title: String, priority: String, dueDateOffsetDays: Int) {
        viewModelScope.launch {
            val due = System.currentTimeMillis() + (dueDateOffsetDays * 86400000L)
            repository.insertTask(
                Task(
                    title = title,
                    priority = priority,
                    dueDate = due,
                    isCompleted = false
                )
            )
            refreshAiInsights()
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
            refreshAiInsights()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            refreshAiInsights()
        }
    }

    // --- Budgets operations ---
    fun setCategoryBudget(category: String, limitAmount: Double) {
        viewModelScope.launch {
            val existing = budgets.value.find { it.category.equals(category, ignoreCase = true) }
            if (existing != null) {
                repository.updateBudget(existing.copy(limitAmount = limitAmount))
            } else {
                repository.insertBudget(Budget(category = category, limitAmount = limitAmount))
            }
            refreshAiInsights()
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
            refreshAiInsights()
        }
    }

    // --- Chat logic ---
    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        val currentList = _chatHistory.value.toMutableList()
        currentList.add(message to true)
        _chatHistory.value = currentList

        viewModelScope.launch {
            // Include context about financial state
            val transState = transactions.value
            val expTotal = transState.filter { it.isExpense }.sumOf { it.amount }
            val incTotal = transState.filter { !it.isExpense }.sumOf { it.amount }
            val currentGoals = goalBoxes.value.joinToString { "${it.name} (${it.currentAmount}/${it.targetAmount})" }
            val userNameStr = userName.value.ifBlank { "Visitante" }
            val userAgeStr = if (userAge.value > 0) "${userAge.value} anos" else "desconhecida"
            
            val statsContext = "O usuário se chama $userNameStr, idade: $userAgeStr. Possui receitas totais de R$ $incTotal, despesas totais de R$ $expTotal. Suas caixinhas de metas de investimentos: $currentGoals."

            val systemInstruction = """
                Você é o Assistente Gemini Financeiro de Produtividade, agindo diretamente como consultor do app Aura Sync (Powered by Nexus Flow).
                Dê conselhos precisos sobre como poupar, sugestões de cortes de gastos ou em qual caixinha alocar aportes. Leve em conta a idade do usuário para dar recomendações adequadas para sua fase de vida.
                Seja amigável, direto e use marcadores elegantes se necessário. Responda em Português de forma profissional.
            """.trimIndent()

            val aiAnswer = GeminiApiClient.callGemini(
                prompt = "Contexto financeiro e perfil do usuário: $statsContext\n\nPergunta do usuário: '$message'",
                systemInstruction = systemInstruction
            )

            val updatedList = _chatHistory.value.toMutableList()
            updatedList.add(aiAnswer to false)
            _chatHistory.value = updatedList
        }
    }

    fun clearChat() {
        _chatHistory.value = listOf(
            "Histórico limpo. Como posso ajudar com sua saúde financeira e tarefas de hoje?" to false
        )
    }

    // --- Intelligent Voice/Spoken input simulator processor ---
    fun processSmartVoiceInput(spokenText: String) {
        if (spokenText.isBlank()) return
        _isProcessingInput.value = true
        _smartInputFeedback.value = "IA interpretando áudio da frase..."

        viewModelScope.launch {
            val result = GeminiApiClient.parseSmartInput(spokenText)
            if (result != null) {
                when (result.type.lowercase()) {
                    "transaction" -> {
                        if (result.transactionTitle != null && result.transactionAmount != null) {
                            val trans = Transaction(
                                title = result.transactionTitle,
                                amount = result.transactionAmount,
                                category = result.transactionCategory ?: "Outros",
                                isExpense = result.isExpense ?: true,
                                date = System.currentTimeMillis()
                            )
                            repository.insertTransaction(trans)
                            _smartInputFeedback.value = "✓ Transação registrada com inteligência: R$ ${result.transactionAmount} em '${trans.title}' (${trans.category})"
                        } else {
                            _smartInputFeedback.value = "⚠️ Interpretação de transação incompleta. Tente falar novamente por exemplo: 'Gastei 50 reais de mercado'."
                        }
                    }
                    "task" -> {
                        if (result.taskTitle != null) {
                            val due = System.currentTimeMillis() + ((result.daysOffset ?: 0) * 86400000L)
                            val task = Task(
                                title = result.taskTitle,
                                priority = result.taskPriority ?: "Média",
                                dueDate = due,
                                isCompleted = false
                            )
                            repository.insertTask(task)
                            val whenString = when (result.daysOffset) {
                                0 -> "Hoje"
                                1 -> "Amanhã"
                                else -> "em ${result.daysOffset} dias"
                            }
                            _smartInputFeedback.value = "✓ Tarefa inserida por voz para $whenString: '${result.taskTitle}' (Prioridade ${task.priority})"
                        } else {
                            _smartInputFeedback.value = "⚠️ Não consegui entender qual a tarefa. Tente falar algo como: 'Me lembre de ligar para a contabilidade amanhã'."
                        }
                    }
                    else -> {
                        _smartInputFeedback.value = "⚠️ IA não identificou com nitidez. Diga transações como 'Gastei 25 no lanche' ou tarefas como 'Lembrar de treinar amanhã'."
                    }
                }
            } else {
                _smartInputFeedback.value = "⚠️ Erro de conexão com o Gemini ou frase não estruturada o suficiente."
            }
            _isProcessingInput.value = false
            refreshAiInsights()
        }
    }

    fun clearFeedback() {
        _smartInputFeedback.value = null
    }

    // --- Helper lists of suggested Voice triggers ---
    val voiceSuggestions = listOf(
        "Gastei 45 reais com almoço hoje",
        "Me lembre de ligar para o suporte da contabilidade amanhã",
        "Recebi um bônus extra de duzentos e cinquenta reais",
        "Gastei trinta e dois reais com motorista de aplicativo",
        "Tarefa importante de agendar reunião de metas para amanhã"
    )

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AppViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
