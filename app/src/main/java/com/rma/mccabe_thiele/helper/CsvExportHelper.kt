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
            val fileName = "McCabeThiele_Results.csv"
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            
            val file = File(exportDir, fileName)
            val outputStream = FileOutputStream(file)
            
            // UTF-8 BOM if needed, but standard UTF-8 is requested.
            // Some CSV readers (like Excel) prefer BOM for UTF-8.
            // Standardizing on UTF-8 without BOM first.
            
            val writer = outputStream.bufferedWriter(Charsets.UTF_8)
            
            val headerX = "X"
            val headerY = "Y"
            val headerRes = "Results"
            
            writer.write("$headerX,$headerY,,$headerRes\n")
            
            val maxRows = maxOf(sucesso.pontosEscada.size, 7)
            
            for (i in 0 until maxRows) {
                // Colunas A e B: pontosEscada
                val x = sucesso.pontosEscada.getOrNull(i)?.first?.let { String.format(Locale.US, "%.4f", it) } ?: ""
                val y = sucesso.pontosEscada.getOrNull(i)?.second?.let { String.format(Locale.US, "%.4f", it) } ?: ""
                
                // Coluna D: Resultados
                val resultadoItem = when (i) {
                    0 -> "${context.getString(R.string.textoVzD).substringBefore(':')},${String.format(Locale.US, "%.2f", sucesso.vazaoDestilado)}"
                    1 -> "${context.getString(R.string.textoVzR).substringBefore(':')},${String.format(Locale.US, "%.2f", sucesso.vazaoResiduo)}"
                    2 -> "${context.getString(R.string.textoMinEst).substringBefore(':')},${String.format(Locale.US, "%.2f", sucesso.numeroMinimoEstagios)}"
                    3 -> "${context.getString(R.string.textoNumEst).substringBefore(':')},${String.format(Locale.US, "%.2f", sucesso.numeroEstagios)}"
                    4 -> "${context.getString(R.string.textoEstCarga).substringBefore(':')},${String.format(Locale.US, "%.2f", sucesso.estagioCargaTopo)}"
                    5 -> "${context.getString(R.string.textoRefluxMin).substringBefore(':')},${String.format(Locale.US, "%.2f", sucesso.rMinRefluxo)}"
                    6 -> "${context.getString(R.string.textoRefluxo).substringBefore(':')},${String.format(Locale.US, "%.2f", sucesso.rRefluxo)}"
                    else -> ","
                }
                
                writer.write("$x,$y,,$resultadoItem\n")
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
