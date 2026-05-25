package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Double? = null,
    @Json(name = "responseMimeType") val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// Structures returned by the Smart NLP and Notification Parsing
@JsonClass(generateAdapter = true)
data class SmartInputResult(
    val type: String, // "transaction" or "task" or "unrecognized"
    val transactionTitle: String?,
    val transactionAmount: Double?,
    val transactionCategory: String?, // "Alimentação", "Transporte", "Lazer", "Tecnologia", "Saúde", "Outros"
    val isExpense: Boolean?,
    val taskTitle: String?,
    val taskPriority: String?, // "Alta", "Média", "Baixa"
    val daysOffset: Int? // 0 = Hoje, 1 = Amanhã, etc.
)

@JsonClass(generateAdapter = true)
data class NotificationParseResult(
    val title: String?,
    val amount: Double?,
    val category: String?,
    val isExpense: Boolean?
)

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key == "MY_GEMINI_API_KEY" || key.isEmpty()) {
            ""
        } else {
            key
        }
    }

    suspend fun callGemini(prompt: String, systemInstruction: String? = null, jsonOutput: Boolean = false): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            Log.e(TAG, "Gemini API Key is missing!")
            return if (jsonOutput) "{}" else "Erro: API Key do Gemini não configurada no painel de Secrets!"
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = if (jsonOutput) GenerationConfig(temperature = 0.1, responseMimeType = "application/json") else GenerationConfig(temperature = 0.7),
            systemInstruction = systemInstruction?.let { Content(parts = listOf(Part(text = it))) }
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini: ", e)
            if (jsonOutput) "{}" else "Erro na comunicação com a IA: ${e.localizedMessage}"
        }
    }

    // Parses a phrase to output structured smart insert data
    suspend fun parseSmartInput(userInput: String): SmartInputResult? {
        val systemInstruction = """
            Você é um assistente inteligente em Português do app 'Aura Sync'.
            Sua missão é extrair dados de textos não estruturados do usuário e retornar APENAS dados JSON estruturados válidos.
            Formatos esperados de retorno:
            {
               "type": "transaction" ou "task",
               "transactionTitle": "Gasolina" ou "Restaurante",
               "transactionAmount": 25.00,
               "transactionCategory": "Transporte", "Alimentação", "Lazer", "Tecnologia", "Saúde" ou "Outros",
               "isExpense": true ou false,
               "taskTitle": "Ligar contabilidade",
               "taskPriority": "Alta", "Média", "Baixa",
               "daysOffset": 1
            }
            Não insira nenhuma introdução ou texto além do próprio JSON formatado.
        """.trimIndent()

        val prompt = "Interprete e extraia dados desta entrada falada/digitada pelo usuário: '$userInput'"

        return try {
            val jsonString = callGemini(prompt, systemInstruction = systemInstruction, jsonOutput = true)
            Log.d(TAG, "Smart Input Parsed Raw response: $jsonString")
            moshi.adapter(SmartInputResult::class.java).fromJson(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing smart input result: ", e)
            null
        }
    }

    // Parses bank payment notification string
    suspend fun parseBankNotification(notificationText: String): NotificationParseResult? {
        val systemInstruction = """
            Você é um leitor inteligente de notificações de compras de aplicativos bancários (Ex: Nubank, Itaú, Bradesco, PicPay).
            Extraia o estabelecimento parceiro, valor e determine a categoria e se é despesa.
            Categorias válidas: Alimentação, Transporte, Lazer, Tecnologia, Educação, Saúde, Outros.
            Retorne APENAS o JSON no formato abaixo, sem tags markdown ou comentários adicionais:
            {
                "title": "NOME_DO_ESTABELECIMENTO_OU_SERVICO",
                "amount": VALOR_NUMERICO,
                "category": "CATEGORIA",
                "isExpense": true_OU_false
            }
        """.trimIndent()

        return try {
            val jsonString = callGemini(notificationText, systemInstruction = systemInstruction, jsonOutput = true)
            Log.d(TAG, "Notification Parsed Raw response: $jsonString")
            moshi.adapter(NotificationParseResult::class.java).fromJson(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing bank notification: ", e)
            null
        }
    }
}
