package com.rma.mccabe_thiele.ui

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.rma.mccabe_thiele.R
import com.rma.mccabe_thiele.data.CsvRepository
import com.rma.mccabe_thiele.databinding.FragmentGraficoBinding
import com.rma.mccabe_thiele.helper.McTChartManager
import com.rma.mccabe_thiele.model.McTEspecificacoes
import com.rma.mccabe_thiele.model.McTMetodo
import com.rma.mccabe_thiele.model.McTResultados
import kotlinx.coroutines.launch

/**
 * Fragmento para exibição do gráfico e importação de dados.
 */
class GraficoFragment : Fragment() {

    private var _binding: FragmentGraficoBinding? = null
    private val binding get() = _binding!!

    private lateinit var chartManager: McTChartManager
    private lateinit var csvRepository: CsvRepository

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { importarCsv(it) }
        }

    private var dadosImportados: List<Pair<Double, Double>> = emptyList()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGraficoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chartManager = McTChartManager(binding.graficoMcCabe)
        csvRepository = CsvRepository(requireContext().contentResolver)

        binding.buttonCalculate.setOnClickListener {
            executarCalculo()
        }

        binding.buttonImport.setOnClickListener {
            importLauncher.launch(arrayOf("text/comma-separated-values", "text/plain", "application/octet-stream"))
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun importarCsv(uri: Uri) {
        binding.buttonImport.isEnabled = false
        lifecycleScope.launch {
            try {
                dadosImportados = csvRepository.importarDadosEquilibrio(uri)
                mostrarResultadoImportacao()
            } catch (e: Exception) {
                Snackbar.make(binding.root, getString(R.string.csv_erro1), Snackbar.LENGTH_LONG).show()
            } finally {
                binding.buttonImport.isEnabled = true
            }
        }
    }

    private fun mostrarResultadoImportacao() {
        val mensagem = if (dadosImportados.isNotEmpty()) {
            getString(R.string.csv_sucesso, dadosImportados.size)
        } else {
            getString(R.string.csv_erro2)
        }
        Snackbar.make(binding.root, mensagem, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Realiza o cálculo de McCabe-Thiele.
     * Prioriza os dados importados via CSV se estiverem disponíveis.
     */
    private fun executarCalculo() {
        try {
            val curvaEquilibrio = csvRepository.criarFuncaoEquilibrio(dadosImportados)
            val specs = McTEspecificacoes(0.8, 0.12, 0.4, 0.8, 100.0, 1.3)
            val metodo = McTMetodo(specs, curvaEquilibrio)
            when (val resposta = metodo.calcular()) {
                is McTResultados.Sucesso -> {
                    chartManager.renderizar(
                        mcTResultados = resposta,
                        curvaEquilibrio = curvaEquilibrio,
                        zF = specs.zF,
                        valorq = specs.valorq,
                        xD = specs.xD,
                        xB = specs.xB,
                        pontosOriginais = dadosImportados
                    )
                }
                is McTResultados.Erro -> {
                    Snackbar.make(binding.root, getString(resposta.mensagem), Snackbar.LENGTH_INDEFINITE).setAction("OK"){}.show()
                    chartManager.renderizarErro(curvaEquilibrio = curvaEquilibrio, pontosOriginais = dadosImportados)
                }
            }
        } catch (e: org.apache.commons.math3.exception.NumberIsTooSmallException) {
            Snackbar.make(binding.root, getString(R.string.akima_qtde_pontos), Snackbar.LENGTH_INDEFINITE).setAction("OK"){}.show()
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.akima_erro, e.message ?: getString(R.string.desconhecido_erro)), Snackbar.LENGTH_INDEFINITE).setAction("OK"){}.show()
        }
    }
}
