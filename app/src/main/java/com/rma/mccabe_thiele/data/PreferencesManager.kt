package com.rma.mccabe_thiele.data

import android.content.Context
import androidx.core.content.edit
import com.rma.mccabe_thiele.model.McTEspecificacoes

class PreferencesManager(context: Context) {

    private val prefs = context.getSharedPreferences("ArquivoPreferencia", Context.MODE_PRIVATE)

    fun salvarEspecificacoes(mcTEspecificacoes: McTEspecificacoes) {
        prefs.edit {
            putFloat("xD", mcTEspecificacoes.xD.toFloat())
            putFloat("xB", mcTEspecificacoes.xB.toFloat())
            putFloat("zF", mcTEspecificacoes.zF.toFloat())
            putFloat("valorq", mcTEspecificacoes.valorq.toFloat())
            putFloat("vazaoF", mcTEspecificacoes.vazaoF.toFloat())
            putFloat("razoesR", mcTEspecificacoes.razoesR.toFloat())
        }
    }

    /**
     * Lê um valor específico do arquivo de configuração retornando como Double.
     * Caso o valor não exista, retorna o valor padrão informado.
     */
    fun lerEspecificacoes(): McTEspecificacoes {
        return McTEspecificacoes(
            xD = prefs.getFloat("xD", 0.7f).toDouble(),
            xB = prefs.getFloat("xB", 0.2f).toDouble(),
            zF = prefs.getFloat("zF", 0.5f).toDouble(),
            valorq = prefs.getFloat("valorq", 0.5f).toDouble(),
            vazaoF = prefs.getFloat("vazaoF", 100.0f).toDouble(),
            razoesR = prefs.getFloat("razoesR", 1.3f).toDouble()
        )
    }

    /**
     * Limpa todos os dados salvos no arquivo de preferência
     */
    fun limparTudo() {
        prefs.edit { clear() }
    }


}