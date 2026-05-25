package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Budget
import com.example.data.model.GoalBox
import com.example.data.model.Task
import com.example.data.model.Transaction
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(LocalContext.current.applicationContext as android.app.Application))
    
    val userName by viewModel.userName.collectAsState()
    val userAge by viewModel.userAge.collectAsState()
    
    // First setup screen if Name is empty
    if (userName.isBlank()) {
        OnboardingSetupScreen(
            onSaveProfile = { name, age ->
                viewModel.saveUserProfile(name, age)
            }
        )
        return
    }

    val transactions by viewModel.transactions.collectAsState()
    val goalBoxes by viewModel.goalBoxes.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    
    val aiAdvice by viewModel.aiAdvice.collectAsState()
    val optimalTimeRec by viewModel.optimalTimeRecommendation.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isProcessingInput by viewModel.isProcessingInput.collectAsState()
    val smartInputFeedback by viewModel.smartInputFeedback.collectAsState()

    var activeTab by remember { mutableStateOf("home") } // "home", "finances", "goals", "tasks", "ai_chat"
    var showSmartMicDialog by remember { mutableStateOf(false) }
    var balanceHidden by remember { mutableStateOf(false) }
    var showNotifsDialog by remember { mutableStateOf(false) }

    // Floating Action Buttons / Core Scaffold
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSmartMicDialog = true },
                containerColor = Color(0xFF6366F1),
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .offset(y = 30.dp) // Move down so it visually overlaps the bottom bar just enough
                    .size(64.dp)
                    .border(4.dp, Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add, // Using Add (+) as requested
                    contentDescription = "Adicionar Inteligente",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FF))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Custom Header Profile Accent
                HeaderProfileRow(
                    userName = userName,
                    onBellClick = { showNotifsDialog = true }
                )

                // Content switching based on chosen tab
                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        "home" -> HomeDashboardView(
                            transactions = transactions,
                            goalBoxes = goalBoxes,
                            tasks = tasks,
                            budgets = budgets,
                            aiAdvice = aiAdvice,
                            optimalTimeRec = optimalTimeRec,
                            balanceHidden = balanceHidden,
                            onToggleBalance = { balanceHidden = !balanceHidden },
                            onTabNavigate = { activeTab = it },
                            viewModel = viewModel
                        )
                        "finances" -> FinancesView(
                            transactions = transactions,
                            budgets = budgets,
                            viewModel = viewModel
                        )
                        "goals" -> GoalBoxesView(
                            goalBoxes = goalBoxes,
                            viewModel = viewModel
                        )
                        "tasks" -> TasksView(
                            tasks = tasks,
                            viewModel = viewModel
                        )
                        "ai_chat" -> AiChatView(
                            chatHistory = chatHistory,
                            optimalTimeRec = optimalTimeRec,
                            viewModel = viewModel
                        )
                    }
                }
            }

            // Unstructured Speech NLP simulation sheet overlay
            if (showSmartMicDialog) {
                SmartVoiceInputDialog(
                    isProcessing = isProcessingInput,
                    feedbackMessage = smartInputFeedback,
                    onDismiss = {
                        showSmartMicDialog = false
                        viewModel.clearFeedback()
                    },
                    onSubmitArbitrary = { text ->
                        viewModel.processSmartVoiceInput(text)
                    },
                    viewModel = viewModel
                )
            }

            if (showNotifsDialog) {
                DailySummaryDialog(
                    transactions = transactions,
                    tasks = tasks,
                    onDismiss = { showNotifsDialog = false }
                )
            }
        }
    }
}

// --- Onboarding / First Setup Screen ---
@Composable
fun OnboardingSetupScreen(onSaveProfile: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var ageStr by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FF))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF6366F1), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("👋", fontSize = 32.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Bem-vindo ao Aura Sync!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Powered by Nexus Flow",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6366F1)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Para personalizarmos sua mentoria e alertas visuais, conte um pouco sobre você.",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Como quer ser chamado?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = ageStr,
                onValueChange = { ageStr = it },
                label = { Text("Qual sua idade?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && ageStr.toIntOrNull() != null) {
                        onSaveProfile(name, ageStr.toInt())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text("Começar agora", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- Header profile row with Mobills theme reference ---
@Composable
fun HeaderProfileRow(userName: String, onBellClick: () -> Unit) {
    val calendar = java.util.Calendar.getInstance()
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Bom dia,"
        in 12..17 -> "Boa tarde,"
        else -> "Boa noite,"
    }
    
    val initialLabel = userName.take(2).uppercase()
    val displayUserName = userName

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF6366F1), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initialLabel,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = greeting,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = displayUserName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        }
        
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(12.dp))
                .clickable { onBellClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("🔔", fontSize = 18.sp)
        }
    }
}

// --- Dashboard Hub View ---
@Composable
fun HomeDashboardView(
    transactions: List<Transaction>,
    goalBoxes: List<GoalBox>,
    tasks: List<Task>,
    budgets: List<Budget>,
    aiAdvice: String,
    optimalTimeRec: String,
    balanceHidden: Boolean,
    onToggleBalance: () -> Unit,
    onTabNavigate: (String) -> Unit,
    viewModel: AppViewModel
) {
    val realGoalBoxes = goalBoxes

    // Calculate dynamic values
    val incomes = transactions.filter { !it.isExpense }.sumOf { it.amount }
    val expenses = transactions.filter { it.isExpense }.sumOf { it.amount }
    val calculatedBalance = incomes - expenses

    val scrollState = rememberScrollState()
    
    val calendar = java.util.Calendar.getInstance()
    val isMorning = calendar.get(java.util.Calendar.HOUR_OF_DAY) < 14

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        if (isMorning) {
            DailySummaryCard(tasks = tasks, transactions = transactions)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Total Balance Card
        Card(
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Saldo total disponível",
                                color = Color(0xFFE0E7FF),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (balanceHidden) Icons.Default.Lock else Icons.Default.Favorite,
                                contentDescription = "Esconder Saldo",
                                tint = Color(0xFFC7D2FE),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onToggleBalance() }
                            )
                        }
                        Text(
                            text = "Mobills Ref",
                            color = Color(0xFFEEF2F6).copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = if (balanceHidden) "R$ ••••••" else "R$ ${String.format(Locale.US, "%,.2f", calculatedBalance)}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Expenses column
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Despesas totais",
                                    fontSize = 10.sp,
                                    color = Color(0xFFE0E7FF),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (balanceHidden) "R$ ••••" else "R$ ${String.format(Locale.US, "%,.2f", expenses)}",
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Incomes column
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Receitas totais",
                                    fontSize = 10.sp,
                                    color = Color(0xFFE0E7FF),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (balanceHidden) "R$ ••••" else "R$ ${String.format(Locale.US, "%,.2f", incomes)}",
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Warning Alert: Budget Limits near or exceeded
        BudgetSafetyBanner(budgets = budgets, transactions = transactions)

        // Graph Section & Data Visualizations (Circular doughnut chart)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFEDF2F7), shape = RoundedCornerShape(24.dp))
                .padding(18.dp)
                .padding(bottom = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Distribuição de Despesas",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Gráfico Real",
                    fontSize = 11.sp,
                    color = Color(0xFF6366F1),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            val expensesByCategory = transactions
                .filter { it.isExpense }
                .groupBy { it.category }
                .mapValues { it.value.sumOf { t -> t.amount } }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                FinanceDoughnutChart(
                    expensesByCategory = expensesByCategory,
                    modifier = Modifier.padding(8.dp)
                )
                
                // Color Legends List
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val colors = listOf(Color(0xFF6366F1), Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFF8B5CF6))
                    if (expensesByCategory.isEmpty()) {
                        Text("Abasteça o extrato", fontSize = 11.sp, color = Color.Gray)
                        Text("para ver detalhes.", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        expensesByCategory.keys.take(4).forEachIndexed { index, cat ->
                            val color = colors[index % colors.size]
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$cat: R$ ${String.format("%.0f", expensesByCategory[cat])}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Caixinhas section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Caixinhas de Metas",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Ver todas",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6366F1),
                modifier = Modifier.clickable { onTabNavigate("goals") }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (realGoalBoxes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma meta criada. Crie uma caixinha para separar seu dinheiro!",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(realGoalBoxes) { box ->
                    DashboardGoalItem(goalBox = box, onDeposit = { amt ->
                        viewModel.depositToGoalBox(box.id, amt)
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tasks section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tarefas do Dia",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Organizar",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6366F1),
                modifier = Modifier.clickable { onTabNavigate("tasks") }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        val pendingTasks = tasks.filter { !it.isCompleted }.take(3)
        if (pendingTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(20.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎉 Todas as tarefas concluídas por hoje!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pendingTasks.forEach { task ->
                    TaskCard(task = task, onCheckedChange = {
                        viewModel.toggleTaskCompletion(task)
                    }, onDelete = {
                        viewModel.deleteTask(task)
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Gemini AI Smart Proactive Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A), shape = RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF818CF8), Color(0xFFC084FC))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Conexão Gemini Proativa",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar Mentoria",
                        tint = Color(0xFF818CF8),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { viewModel.refreshAiInsights() }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = aiAdvice,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                     text = "🕒 Recomendação de Envio de Notificações Inteligentes:",
                     color = Color(0xFF818CF8),
                     fontSize = 11.sp,
                     fontWeight = FontWeight.Bold
                )
                Text(
                     text = optimalTimeRec,
                     color = Color(0xFFE2E8F0),
                     fontSize = 11.sp,
                     lineHeight = 15.sp,
                     modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// --- Dynamic Budget Safety Tracker alert banner ---
@Composable
fun BudgetSafetyBanner(budgets: List<Budget>, transactions: List<Transaction>) {
    var hasAlerts = false
    val alerts = remember(budgets, transactions) {
        val list = mutableListOf<String>()
        budgets.forEach { budget ->
            val spent = transactions
                .filter { it.isExpense && it.category.equals(budget.category, ignoreCase = true) }
                .sumOf { it.amount }
            val ratio = if (budget.limitAmount > 0) spent / budget.limitAmount else 0.0
            if (ratio >= 1.0) {
                list.add("Limite de orçamento ESTOURADO em '${budget.category}'! Limite: R$ ${budget.limitAmount} | Gasto: R$ $spent")
                hasAlerts = true
            } else if (ratio >= 0.8) {
                list.add("Alerta de 80%+ no orçamento de '${budget.category}'. Limite R$ ${budget.limitAmount} | Gasto: R$ $spent")
                hasAlerts = true
            }
        }
        list
    }

    if (alerts.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .background(Color(0xFFFEF2F2), shape = RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFFCA5A5), shape = RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alerta de Orçamento",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Avisos de Orçamentos Mensais",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF991B1B)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            alerts.forEach { alert ->
                Text(
                    text = "• $alert",
                    fontSize = 11.sp,
                    color = Color(0xFF7F1D1D),
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

// --- Styled Custom Doughnut Chart rendering via Custom Canvas ---
@Composable
fun FinanceDoughnutChart(
    expensesByCategory: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val total = expensesByCategory.values.sum()
    val categories = expensesByCategory.keys.toList()
    val values = expensesByCategory.values.toList()

    val colors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981), // Emerald
        Color(0xFFEF4444), // Red
        Color(0xFF3B82F6), // Blue
        Color(0xFF8B5CF6)  // Purple
    )

    Box(
        modifier = modifier.size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        if (total == 0.0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFFE2E8F0),
                    radius = size.minDimension / 2,
                    style = Stroke(width = 30f)
                )
            }
            Text(
                text = "R$ 0,00",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var currentAngle = -90f
                val minDimension = size.minDimension
                val strokeWidth = 32f
                val chartSize = Size(minDimension - strokeWidth, minDimension - strokeWidth)
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                for (i in categories.indices) {
                    val angle = (values[i] / total * 360).toFloat()
                    val color = colors[i % colors.size]
                    drawArc(
                        color = color,
                        startAngle = currentAngle,
                        sweepAngle = angle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = chartSize,
                        style = Stroke(width = strokeWidth)
                    )
                    currentAngle += angle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "R$ ${String.format("%.0f", total)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        }
    }
}

// --- Dashboard Goal Horizontal Card ---
@Composable
fun DashboardGoalItem(goalBox: GoalBox, onDeposit: (Double) -> Unit) {
    var depositDialogShow by remember { mutableStateOf(false) }
    var depositAmountStr by remember { mutableStateOf("") }

    val progress = if (goalBox.targetAmount > 0) {
        (goalBox.currentAmount / goalBox.targetAmount).coerceIn(0.0, 1.0).toFloat()
    } else 0f

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .width(160.dp)
            .border(1.dp, Color(0xFFEDF2F7), shape = RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val emoji = when {
                goalBox.name.contains("Emergência", true) -> "🛡️"
                goalBox.name.contains("Moto", true) -> "🏍️"
                goalBox.name.contains("Viagem", true) -> "✈️"
                else -> "💰"
            }
            
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFFEF3C7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 15.sp)
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = goalBox.name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF1E293B),
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = Color(0xFFF59E0B),
                trackColor = Color(0xFFF1F5F9)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${(progress * 100).toInt()}% da meta",
                fontSize = 10.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Aportado R$ ${String.format("%.0f", goalBox.currentAmount)}",
                fontSize = 10.sp,
                color = Color(0xFF1E293B),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { depositDialogShow = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF2F6)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxWidth().height(28.dp)
            ) {
                Text("Aportar", fontSize = 10.sp, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
            }
        }
    }

    if (depositDialogShow) {
        AlertDialog(
            onDismissRequest = { depositDialogShow = false },
            title = { Text("Aportar na Caixinha: ${goalBox.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Defina o valor para transferir para essa meta de investimento:", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = depositAmountStr,
                        onValueChange = { depositAmountStr = it },
                        label = { Text("Valor (R$)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = depositAmountStr.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onDeposit(amt)
                        depositDialogShow = false
                        depositAmountStr = ""
                    }
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { depositDialogShow = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// --- Shared Custom Bottom Navigation for Mobills Usability ---
@Composable
fun BottomNavigationBar(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFEEF2F6)),
        shadowElevation = 18.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarIconItem(
                    emoji = "🏠",
                    label = "Home",
                    isActive = activeTab == "home",
                    onClick = { onTabSelected("home") }
                )
                NavBarIconItem(
                    emoji = "📊",
                    label = "Finanças",
                    isActive = activeTab == "finances",
                    onClick = { onTabSelected("finances") }
                )
                
                // Emtpy spacer block for the center Microfone button
                Spacer(modifier = Modifier.width(52.dp))

                NavBarIconItem(
                    emoji = "📦",
                    label = "Caixinhas",
                    isActive = activeTab == "goals",
                    onClick = { onTabSelected("goals") }
                )
                NavBarIconItem(
                    emoji = "💬",
                    label = "Chat-IA",
                    isActive = activeTab == "ai_chat",
                    onClick = { onTabSelected("ai_chat") }
                )
            }
        }
    }
}

@Composable
fun NavBarIconItem(
    emoji: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp,
            modifier = Modifier.alpha(if (isActive) 1f else 0.45f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) Color(0xFF4F46E5) else Color.Gray
        )
    }
}

// --- Finances Tab View ---
@Composable
fun FinancesView(
    transactions: List<Transaction>,
    budgets: List<Budget>,
    viewModel: AppViewModel
) {
    var trTitle by remember { mutableStateOf("") }
    var trAmount by remember { mutableStateOf("") }
    var trCategory by remember { mutableStateOf("Alimentação") }
    var isExpense by remember { mutableStateOf(true) }

    var capCategory by remember { mutableStateOf("Alimentação") }
    var capLimit by remember { mutableStateOf("") }

    val categories = listOf("Alimentação", "Transporte", "Lazer", "Tecnologia", "Saúde", "Outros")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Form: Add manual transaction
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFFEDF2F7), shape = RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Text("Registrar Transação Manual", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = trTitle,
                    onValueChange = { trTitle = it },
                    label = { Text("Título (Ex: Jantar, Salário)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = trAmount,
                    onValueChange = { trAmount = it },
                    label = { Text("Valor (R$)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle Selection for Expense vs Income
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { isExpense = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExpense) Color(0xFFEF4444) else Color(0xFFF1F5F9)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Despesa", color = if (isExpense) Color.White else Color.Gray, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { isExpense = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isExpense) Color(0xFF10B981) else Color(0xFFF1F5F9)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Receita", color = if (!isExpense) Color.White else Color.Gray, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category selection row scroll
                Text("Escolha a categoria:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val selected = trCategory == cat
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (selected) Color(0xFF6366F1) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { trCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (selected) Color.White else Color.DarkGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val amt = trAmount.toDoubleOrNull()
                        if (trTitle.isNotBlank() && amt != null && amt > 0.0) {
                            viewModel.addManualTransaction(trTitle, amt, trCategory, isExpense)
                            trTitle = ""
                            trAmount = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Salvar Registro")
                }
            }
        }

        // Form: Manage Category Budget Caps
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFFEDF2F7), shape = RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Text("Gerenciar Orçamento por Categoria", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Estipule limites de gastos contra categorias para ter alertas visuais inteligentes se bater 80%.", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = capLimit,
                        onValueChange = { capLimit = it },
                        label = { Text("Limite Máximo (R$)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val selected = capCategory == cat
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (selected) Color(0xFFF59E0B) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { capCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (selected) Color.White else Color.DarkGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val limit = capLimit.toDoubleOrNull()
                        if (limit != null && limit > 0) {
                            viewModel.setCategoryBudget(capCategory, limit)
                            capLimit = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Text("Salvar Limite")
                }
            }
        }

        // Active established budgets visualization list
        item {
            Text("Limites Atuais Estabelecidos", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }

        if (budgets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, shape = RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhum limite estipulado ainda.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            items(budgets) { budget ->
                val totalSpent = transactions
                    .filter { it.isExpense && it.category.equals(budget.category, ignoreCase = true) }
                    .sumOf { it.amount }
                val ratio = if (budget.limitAmount > 0) (totalSpent / budget.limitAmount).toFloat() else 0f

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(budget.category, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = ratio.coerceIn(0f, 1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = if (ratio >= 1.0f) Color(0xFFEF4444) else if (ratio >= 0.8f) Color(0xFFF59E0B) else Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "R$ ${String.format("%.2f", totalSpent)} gastos",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Limite: R$ ${String.format("%.2f", budget.limitAmount)}",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.deleteBudget(budget) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Deletar Limite", tint = Color.Gray)
                        }
                    }
                }
            }
        }

        // List Transactions Extrato
        item {
            Text("Histórico do Extrato Financeiro", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, shape = RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma transação registrada.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            items(transactions) { tr ->
                Card(
                     modifier = Modifier.fillMaxWidth(),
                     shape = RoundedCornerShape(16.dp),
                     colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (tr.isExpense) Color(0xFFFEF2F2) else Color(0xFFECFDF5),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (tr.isExpense) "↓" else "↑",
                                    color = if (tr.isExpense) Color(0xFFEF4444) else Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(tr.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                                Text(tr.category, fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${if (tr.isExpense) "-" else "+"} R$ ${String.format("%.2f", tr.amount)}",
                                color = if (tr.isExpense) Color(0xFFEF4444) else Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { viewModel.deleteTransaction(tr) }) {
                                Icon(Icons.Default.Close, contentDescription = "Deletar transição", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// --- GoalBoxes Detail layout ---
@Composable
fun GoalBoxesView(
    goalBoxes: List<GoalBox>,
    viewModel: AppViewModel
) {
    var brandGoalName by remember { mutableStateOf("") }
    var brandTargetAmt by remember { mutableStateOf("") }
    var brandStartingAmt by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFFEDF2F7), shape = RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Text("Criar Nova Caixinha de Investimento", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = brandGoalName,
                    onValueChange = { brandGoalName = it },
                    label = { Text("Nome do Objetivo (Ex: Carro, Casamento)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = brandTargetAmt,
                    onValueChange = { brandTargetAmt = it },
                    label = { Text("Valor Alvo da Meta (R$)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = brandStartingAmt,
                    onValueChange = { brandStartingAmt = it },
                    label = { Text("Aporte Inicial Opcional (R$)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val target = brandTargetAmt.toDoubleOrNull()
                        val start = brandStartingAmt.toDoubleOrNull() ?: 0.0
                        if (brandGoalName.isNotBlank() && target != null && target > 0.0) {
                            viewModel.addGoalBox(brandGoalName, target, start)
                            brandGoalName = ""
                            brandTargetAmt = ""
                            brandStartingAmt = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Criar Caixinha")
                }
            }
        }

        item {
            Text("Suas Caixinhas Ativas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }

        if (goalBoxes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, shape = RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sem caixinhas criadas. Separe seu dinheiro!", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            items(goalBoxes) { box ->
                val ratio = if (box.targetAmount > 0) (box.currentAmount / box.targetAmount).toFloat() else 0f
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📦", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(box.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            IconButton(onClick = { viewModel.deleteGoalBox(box) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Deletar Caixinha", tint = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = ratio.coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = Color(0xFF6366F1)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Acumulado: R$ ${String.format("%.2f", box.currentAmount)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5)
                            )
                            Text(
                                "Meta: R$ ${String.format("%.2f", box.targetAmount)} (${(ratio * 100).toInt()}%)",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// --- Tasks Tab View ---
@Composable
fun TasksView(
    tasks: List<Task>,
    viewModel: AppViewModel
) {
    var taskTitle by remember { mutableStateOf("") }
    var prioritySelected by remember { mutableStateOf("Média") }
    var whenOffsetDays by remember { mutableStateOf(0) } // 0 = Hoje, 1 = Amanhã, etc.

    val priorities = listOf("Alta", "Média", "Baixa")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFFEDF2F7), shape = RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Text("Adicionar Nova Tarefa Diária", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("O que precisa fazer?", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Priority selects
                Text("Nível de Prioridade:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priorities.forEach { pr ->
                        val active = prioritySelected == pr
                        val color = when (pr) {
                            "Alta" -> Color(0xFFEF4444)
                            "Média" -> Color(0xFFF59E0B)
                            else -> Color(0xFF10B981)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (active) color else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { prioritySelected = pr }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                pr,
                                color = if (active) Color.White else Color.DarkGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Offset days for Calendar alignment
                Text("Previsão de Prazo:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Hoje", 1 to "Amanhã", 3 to "3 dias").forEach { (offset, text) ->
                        val active = whenOffsetDays == offset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (active) Color(0xFF6366F1) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { whenOffsetDays = offset }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text,
                                color = if (active) Color.White else Color.DarkGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            viewModel.addTask(taskTitle, prioritySelected, whenOffsetDays)
                            taskTitle = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Adicionar Agenda")
                }
            }
        }

        item {
            Text("Suas Tarefas Agendadas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }

        if (tasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, shape = RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma tarefa cadastrada.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            items(tasks) { task ->
                TaskCard(task = task, onCheckedChange = {
                    viewModel.toggleTaskCompletion(task)
                }, onDelete = {
                    viewModel.deleteTask(task)
                })
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onCheckedChange: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCheckedChange) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.Clear,
                        contentDescription = "Completar Tarefa",
                        tint = if (task.isCompleted) Color(0xFF10B981) else Color.LightGray
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (task.isCompleted) Color.Gray else Color(0xFF1E293B),
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                    )
                    
                    val priorityColor = when (task.priority) {
                        "Alta" -> Color(0xFFEF4444)
                        "Média" -> Color(0xFFF59E0B)
                        else -> Color(0xFF10B981)
                    }
                    val formattedDate = remember(task.dueDate) {
                        try {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            sdf.format(Date(task.dueDate))
                        } catch (e: Exception) {
                            "-"
                        }
                    }
                    Text(
                        text = "Prioridade ${task.priority} • Vence em: $formattedDate",
                        fontSize = 10.sp,
                        color = priorityColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Deletar tarefa",
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// --- AiChat Interactive Assistant Column ---
@Composable
fun AiChatView(
    chatHistory: List<Pair<String, Boolean>>,
    optimalTimeRec: String,
    viewModel: AppViewModel
) {
    var chatMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // High visibility optimal recommendation header summary box
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2F6)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🕒", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Horário de Envio Ideal das Notificações Inteligentes:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(optimalTimeRec, fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Messages List
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatHistory) { (msg, isUser) ->
                val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                val shape = if (isUser) RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
                val bg = if (isUser) Color(0xFF6366F1) else Color.White
                val textColor = if (isUser) Color.White else Color(0xFF1E293B)
                val borderMod = if (isUser) Modifier else Modifier.border(1.dp, Color(0xFFEDF2F7), shape)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    contentAlignment = alignment
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .background(bg, shape = shape)
                            .then(borderMod)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isUser) "Você" else "Gemini IA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) Color(0xFFC7D2FE) else Color(0xFF6366F1),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                color = textColor,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Action controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chatMessage,
                onValueChange = { chatMessage = it },
                placeholder = { Text("Pergunte à IA...", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            FloatingActionButton(
                onClick = {
                    if (chatMessage.isNotBlank()) {
                        viewModel.sendChatMessage(chatMessage)
                        chatMessage = ""
                    }
                },
                containerColor = Color(0xFF6366F1),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Enviar", modifier = Modifier.size(18.dp))
            }
            
            IconButton(onClick = { viewModel.clearChat() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Limpar chat", tint = Color.Gray)
            }
        }
    }
}

// --- Smart Voice Input Simulation Dialog (Microphone overlay) ---
@Composable
fun SmartVoiceInputDialog(
    isProcessing: Boolean,
    feedbackMessage: String?,
    onDismiss: () -> Unit,
    onSubmitArbitrary: (String) -> Unit,
    viewModel: AppViewModel
) {
    var typedPhrase by remember { mutableStateOf("") }
    
    // Animate sound waves pulse scale
    val transition = rememberInfiniteTransition()
    val pulseScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Captura Inteligente",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }
                Text(
                    text = "Fale ou selecione uma frase para a inteligência NLP extrair e registrar automaticamente no banco.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    textAlign = TextAlign.Center
                )

                // Large microphone visual waves pulse
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((60 * pulseScale).dp)
                            .background(
                                color = Color(0xFF6366F1).copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF6366F1), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎙️", fontSize = 20.sp)
                    }
                }

                // Live dynamic status / parsed outcome feedback box
                AnimatedVisibility(visible = feedbackMessage != null) {
                    if (feedbackMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .background(Color(0xFFEEF2F6), shape = RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                if (isProcessing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF6366F1))
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text(
                                    text = feedbackMessage,
                                    fontSize = 11.sp,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Suggestion chips of things user can "SAY" to test Gemini
                Text("Toque em uma frase de voz sugerida para testar a IA:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    viewModel.voiceSuggestions.forEach { suggestion ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .clickable { onSubmitArbitrary(suggestion) }
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🗣️", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = suggestion,
                                    fontSize = 10.sp,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Ou digite o comando abaixo manualmente:", fontSize = 10.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = typedPhrase,
                        onValueChange = { typedPhrase = it },
                        placeholder = { Text("Gastei 150 em livros hoje...", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                    )
                    Button(
                        onClick = {
                            if (typedPhrase.isNotBlank()) {
                                onSubmitArbitrary(typedPhrase)
                                typedPhrase = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Text("Enviar", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// --- Daily Summaries ---
@Composable
fun DailySummaryCard(tasks: List<Task>, transactions: List<Transaction>) {
    val pendingTasksToday = tasks.filter { !it.isCompleted } // Simplification: we show all pendings
    
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Seu Resumo Matinal", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B))
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (pendingTasksToday.isEmpty()) {
                Text("Você não tem tarefas pendentes hoje. Aproveite o dia!", fontSize = 12.sp, color = Color.Gray)
            } else {
                Text("Você tem ${pendingTasksToday.size} tarefas para focar hoje:", fontSize = 12.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(4.dp))
                pendingTasksToday.take(3).forEach {
                    Text("• ${it.title}", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun DailySummaryDialog(tasks: List<Task>, transactions: List<Transaction>, onDismiss: () -> Unit) {
    val pendingTasksToday = tasks.filter { !it.isCompleted } // Simplification: we show all pendings

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Central de Notificações", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Resumo do seu dia:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF6366F1))
                Spacer(modifier = Modifier.height(8.dp))
                if (pendingTasksToday.isEmpty()) {
                    Text("Sem pendências no momento.", fontSize = 13.sp, color = Color.Gray)
                } else {
                    pendingTasksToday.forEach { task ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("• ", color = Color(0xFF6366F1))
                            Text(task.title, fontSize = 13.sp, color = Color.DarkGray)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Fechar")
                }
            }
        }
    }
}

