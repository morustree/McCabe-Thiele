package com.rma.mccabe_thiele.helper

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.rma.mccabe_thiele.R
import com.rma.mccabe_thiele.model.McTResultados
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class CsvExportHelper(private val context: Context) {

    suspend fun gerarCsvEObterUri(sucesso: McTResultados.Sucesso): Uri? = withContext(Dispatchers.IO) {
        try {
            val specs = sucesso.especificacoes
            val fileName = "McCabeThiele_Results.csv"
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            
            val file = File(exportDir, fileName)
            val outputStream = FileOutputStream(file)
            
            val writer = outputStream.bufferedWriter(Charsets.UTF_8)
            
            val headerX = "X"
            val headerY = "Y"
            val headerRes = "Results"
            
            val numSpecs = 6
            val space = 1
            val headerRow = 1
            val numResults = 7
            val totalRowsD = numSpecs + space + headerRow + numResults
            
            val maxRows = maxOf(sucesso.pontosEscada.size + 1, totalRowsD)
            
            for (i in 0 until maxRows) {
                // Colunas A e B: Header e pontosEscada
                val x = when {
                    i == 0 -> headerX
                    else -> sucesso.pontosEscada.getOrNull(i - 1)?.first?.let { String.format(Locale.US, "%.4f", it) } ?: ""
                }
                val y = when {
                    i == 0 -> headerY
                    else -> sucesso.pontosEscada.getOrNull(i - 1)?.second?.let { String.format(Locale.US, "%.4f", it) } ?: ""
                }
                
                // Coluna D: Especificações e Resultados
                val itemD = when (i) {
                    0 -> "xD,${String.format(Locale.US, "%.4f", specs.xD)}"
                    1 -> "xB,${String.format(Locale.US, "%.4f", specs.xB)}"
                    2 -> "zF,${String.format(Locale.US, "%.4f", specs.zF)}"
                    3 -> "valorq,${String.format(Locale.US, "%.4f", specs.valorq)}"
                    4 -> "vazaoF,${String.format(Locale.US, "%.4f", specs.vazaoF)}"
                    5 -> "razoesR,${String.format(Locale.US, "%.4f", specs.razoesR)}"
                    6 -> ","
                    7 -> "$headerRes,"
                    8 -> "${context.getString(R.string.textoVzD).substringBefore(':')},${String.format(Locale.US, "%.2f", sucesso.vazaoDestilado)}"
                    9 -> "${context.getString(R.string.textoVzR).substringBefore(':')},${String.format(Locale.US, "%.2f", sucesso.vazaoResiduo)}"
                    10 -> "${context.getString(R.string.textoMinEst).substringBefore(':')},${String.format(Locale.US, "%.2f", sucesso.numeroMinimoEstagios)}"
                    11 -> "${context.getString(R.string.textoNumEst).substringBefore(':')},${String.format(Locale.US, "%.2f", sucesso.numeroEstagios)}"
                    12 -> "${context.getString(R.string.textoEstCarga).substringBefore(':')},${String.format(Locale.US, "%.2f", sucesso.estagioCargaTopo)}"
                    13 -> "${context.getString(R.string.textoRefluxMin).substringBefore(':')},${String.format(Locale.US, "%.2f", sucesso.rMinRefluxo)}"
                    14 -> "${context.getString(R.string.textoRefluxo).substringBefore(':')},${String.format(Locale.US, "%.2f", sucesso.rRefluxo)}"
                    else -> ","
                }
                
                writer.write("$x,$y,,$itemD\n")
            }
            
            writer.flush()
            writer.close()
            outputStream.close()
            
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
