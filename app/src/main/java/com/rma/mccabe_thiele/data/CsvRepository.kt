package com.rma.mccabe_thiele.data

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

/**
 * Especialista para importação e tratamento de dados CSV com suporte universal.
 */
class CsvRepository(private val contentResolver: ContentResolver) {

    /**
     * Converte uma lista de pontos em uma função contínua usando interpolação Akima Spline.
     */
    suspend fun criarFuncaoEquilibrio(pontos: List<Pair<Double, Double>>): UnivariateFunction = withContext(Dispatchers.Default) {
        val ordenados = pontos.distinctBy { it.first }.sortedBy { it.first }
        val x = ordenados.map { it.first }.toDoubleArray()
        val y = ordenados.map { it.second }.toDoubleArray()

        AkimaSplineInterpolator().interpolate(x, y)
    }

    /**
     * Importa dados de equilíbrio de uma URI, tratando encodings e delimitadores globais.
     */
    suspend fun importarDadosEquilibrio(uri: Uri): List<Pair<Double, Double>> = withContext(Dispatchers.IO) {
        val linhasBrutas = lerLinhasComEncodings(uri)
        if (linhasBrutas.isEmpty()) return@withContext emptyList()

        processarLinhas(linhasBrutas)
    }

    private fun lerLinhasComEncodings(uri: Uri): List<String> {
        val encodings = listOf("UTF-8", "Windows-1252", "ISO-8859-1", "UTF-16", "Windows-1250")
        
        for (encoding in encodings) {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val reader = BufferedReader(InputStreamReader(stream, Charset.forName(encoding)))
                    val linhas = reader.readLines()
                    if (linhas.isNotEmpty()) return linhas
                }
            } catch (e: Exception) { continue }
        }
        return emptyList()
    }

    /**
     * Algoritmo de proteção: identifica a posição exata do delimitador de campo
     * e o protege com 'dcquebra' antes de qualquer tratamento de dados.
     */
    private suspend fun processarLinhas(linhas: List<String>): List<Pair<Double, Double>> = withContext(Dispatchers.Default) {
        val pontosValidos = mutableListOf<Pair<Double, Double>>()
        
        linhas.forEach { linhaOriginal ->
            // Pula linhas sem dados numéricos (cabeçalhos ou vazias)
            if (linhaOriginal.isBlank() || !linhaOriginal.any { it.isDigit() }) return@forEach

            try {
                val linha = linhaOriginal.trim()
                
                // Mapeia os índices de caracteres não pertencentes à notação científica
                val indicesNaoNumericos = linha.indices.filter { idx ->
                    val c = linha[idx]
                    !c.isDigit() && c != '.' && c != '-' && c != '+' && c != 'E' && c != 'e'
                }

                if (indicesNaoNumericos.isNotEmpty()) {
                    // O delimitador de campo é o caractere do meio da estrutura não numérica
                    val posicaoNoTexto = indicesNaoNumericos[indicesNaoNumericos.size / 2]

                    // Substitui apenas esse caractere específico pelo escudo
                    val linhaProtegida = StringBuilder(linha).apply {
                        replace(posicaoNoTexto, posicaoNoTexto + 1, "dcquebra")
                    }.toString()

                    // Limpa aspas e normalizam decimais com segurança
                    val partes = linhaProtegida.split("dcquebra").map { parte ->
                        parte.replace("\"", "")
                             .replace("'", "")
                             .replace(",", ".") // Converte vírgula decimal para ponto
                             .trim()
                             .toDouble()
                    }

                    if (partes.size >= 2 && partes[0] in 0.0..1.0 && partes[1] in 0.0..1.0) {
                        pontosValidos.add(partes[0] to partes[1])
                    }
                }
            } catch (e: Exception) { /* Ignora falhas de conversão em linhas sujas */ }
        }
        pontosValidos.distinctBy { it.first }.sortedBy { it.first }
    }
}
