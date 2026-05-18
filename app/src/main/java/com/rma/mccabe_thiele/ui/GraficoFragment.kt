package com.rma.mccabe_thiele.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.rma.mccabe_thiele.R
import com.rma.mccabe_thiele.data.CsvRepository
import com.rma.mccabe_thiele.data.PreferencesManager
import com.rma.mccabe_thiele.databinding.FragmentGraficoBinding
import com.rma.mccabe_thiele.helper.CsvExportHelper
import com.rma.mccabe_thiele.helper.McTChartManager
import com.rma.mccabe_thiele.helper.ToolbarEventViewModel
import com.rma.mccabe_thiele.model.McTMetodo
import com.rma.mccabe_thiele.model.McTResultados
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragmento para exibição do gráfico e importação de dados.
 */
class GraficoFragment : Fragment() {

    private var _binding: FragmentGraficoBinding? = null
    private val binding get() = _binding!!

    private lateinit var chartManager: McTChartManager
    private lateinit var csvRepository: CsvRepository
    private lateinit var csvExportHelper: CsvExportHelper

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { importarCsv(it) }
        }

    private var dadosImportados: List<Pair<Double, Double>> = emptyList()

    private val prefsManager by lazy { PreferencesManager(requireContext()) }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private val toolbarViewModel: ToolbarEventViewModel by activityViewModels()

    private var ultimosResultados: McTResultados.Sucesso? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGraficoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetPainel)
        bottomSheetBehavior.isGestureInsetBottomIgnored = false

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                _binding?.let { b ->
                    val offsetValidado = slideOffset.coerceIn(0f, 1f)
                    b.indicadorArrastarBottomSheet.rotation = offsetValidado * 180f
                }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            toolbarViewModel.onImportarCsvClick.collectLatest {
                _binding?.let {
                    importLauncher.launch(arrayOf("text/comma-separated-values", "text/plain", "application/octet-stream"))
                }
            }
        }

        limparInterface()

        chartManager = McTChartManager(binding.graficoMcCabeT)
        csvRepository = CsvRepository(requireContext().contentResolver)
        csvExportHelper = CsvExportHelper(requireContext())


        binding.layoutPlaceholderGrafico.setOnClickListener {
            importLauncher.launch(arrayOf("text/comma-separated-values", "text/plain", "application/octet-stream"))
        }


        viewLifecycleOwner.lifecycleScope.launch {
            toolbarViewModel.onExportarCsvClick.collectLatest {
                _binding?.let {
                    val dados = ultimosResultados
                    if (dados != null) {
                        exportarECompartilharCsv(dados)
                    }
                }
            }
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun importarCsv(uri: Uri) {
        val binding = _binding ?: return
        toolbarViewModel.setBotaoImportarAtivo(false)
        binding.layoutPlaceholderGrafico.isClickable = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                dadosImportados = csvRepository.importarDadosEquilibrio(uri)
                val mensagem = if (dadosImportados.isNotEmpty()) {
                    getString(R.string.csv_sucesso, dadosImportados.size)
                } else {
                    getString(R.string.csv_erro2)
                }
                
                if (dadosImportados.isNotEmpty()) {
                    executarCalculo(dadosImportados)
                }
                exibirSnackbarTopo(mensagem, false)
            } catch (e: Exception) {
                if (isAdded) {
                    exibirSnackbarTopo(getString(R.string.csv_erro1), false)
                }
                limparInterface()
            } finally {
                _binding?.let { binding ->
                    toolbarViewModel.setBotaoImportarAtivo(true)
                    binding.layoutPlaceholderGrafico.isClickable = true
                }
            }
        }
    }


    /**
     * Realiza o cálculo de McCabe-Thiele de forma assíncrona.
     */
    private fun executarCalculo(dadosParaCalculo: List<Pair<Double, Double>>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                toolbarViewModel.setBotaoImportarAtivo(false)
                toolbarViewModel.setBotaoExportarAtivo(false)
                _binding?.layoutPlaceholderGrafico?.isClickable = false
                
                val curvaEquilibrio = csvRepository.criarFuncaoEquilibrio(dadosParaCalculo)
                val specs = prefsManager.lerEspecificacoes()
                val metodo = McTMetodo(specs, curvaEquilibrio, dadosParaCalculo.first().first)
                
                when (val resposta = metodo.calcular()) {
                    is McTResultados.Sucesso -> {
                        _binding?.let {
                            chartManager.renderizar(
                                mcTResultados = resposta,
                                curvaEquilibrio = curvaEquilibrio,
                                especificacoes = resposta.especificacoes,
                                pontosOriginais = dadosParaCalculo
                            )
                            ultimosResultados = resposta
                            carregarDadosInterface(resposta)
                            toolbarViewModel.setBotaoExportarAtivo(true)
                        }
                    }
                    is McTResultados.Erro -> {
                        if (isAdded) {
                            exibirSnackbarTopo(getString(resposta.mensagem), true)
                        }
                        _binding?.let {
                            chartManager.renderizarErro(curvaEquilibrio = curvaEquilibrio, pontosOriginais = dadosParaCalculo)
                        }
                        toolbarViewModel.setBotaoExportarAtivo(false)
                    }
                }
            } catch (e: org.apache.commons.math3.exception.NumberIsTooSmallException) {
                if (isAdded) {
                    exibirSnackbarTopo(getString(R.string.akima_qtde_pontos), true)
                }
                toolbarViewModel.setBotaoExportarAtivo(false)
            } catch (e: Exception) {
                if (isAdded) {
                    val msg = getString(R.string.akima_erro, e.message ?: getString(R.string.desconhecido_erro))
                    exibirSnackbarTopo(msg, true)
                }
                toolbarViewModel.setBotaoExportarAtivo(false)
            } finally {
                toolbarViewModel.setBotaoImportarAtivo(true)
                _binding?.layoutPlaceholderGrafico?.isClickable = true
            }
        }
    }

    private fun limparInterface(){
        val binding = _binding ?: return
        
        // Limpeza de variáveis de estado
        ultimosResultados = null
        dadosImportados = emptyList()

        toolbarViewModel.setBotaoExportarAtivo(false)
        binding.graficoMcCabeT.clear()
        binding.graficoMcCabeT.invalidate()
        binding.graficoMcCabeT.visibility = View.GONE
        binding.layoutPlaceholderGrafico.visibility = View.VISIBLE
        toolbarViewModel.setBotaoImportarAtivo(true)
        bottomSheetBehavior.apply {
            isHideable = true
            state = BottomSheetBehavior.STATE_HIDDEN
            peekHeight = 0
        }
        binding.textTotalEstagios.text = getString(R.string.textoResumo, 0.0)
        binding.textVazaoDestilado.text = getString(R.string.textoVzD, 0.0)
        binding.textVazaoResiduo.text = getString(R.string.textoVzR, 0.0)
        binding.textMinEst.text = getString(R.string.textoMinEst, 0.0)
        binding.textEstCarga.text = getString(R.string.textoEstCarga, 0.0)
        binding.textNumEst.text = getString(R.string.textoNumEst, 0.0)
        binding.textRefluxoMinimo.text = getString(R.string.textoRefluxMin, 0.0)
        binding.textRefluxo.text = getString(R.string.textoRefluxo, 0.0)
        binding.textDegraus.text = getString(R.string.textoDegraus)
    }

    private fun carregarDadosInterface(resposta: McTResultados.Sucesso){
        val binding = _binding ?: return
        binding.bottomSheetPainel.post {
            if (_binding == null || context == null) return@post
            val folgaPixels = resources.getDimensionPixelSize(R.dimen.space_3xl)
            // Mede a altura apenas do resumo estático superior
            val alturaCabecalho = binding.layoutResumo.height + folgaPixels
            bottomSheetBehavior.apply {
                isHideable = false // Trava para o usuário não conseguir esconder arrastando para baixo
                peekHeight = alturaCabecalho // Define o limite onde a folha vai descansar colada na base
                state = BottomSheetBehavior.STATE_COLLAPSED // Força a descida suave para a base
            }
            // Força o redesenho do container
            binding.bottomSheetPainel.requestLayout()
        }

        binding.layoutPlaceholderGrafico.visibility = View.GONE
        binding.graficoMcCabeT.visibility = View.VISIBLE
        binding.textTotalEstagios.text = getString(R.string.textoResumo, resposta.numeroEstagios)
        binding.textVazaoDestilado.text = getString(R.string.textoVzD, resposta.vazaoDestilado)
        binding.textVazaoResiduo.text = getString(R.string.textoVzR, resposta.vazaoResiduo)
        binding.textMinEst.text = getString(R.string.textoMinEst, resposta.numeroMinimoEstagios)
        binding.textEstCarga.text = getString(R.string.textoEstCarga, resposta.estagioCargaTopo)
        binding.textNumEst.text = getString(R.string.textoNumEst, resposta.numeroEstagios)
        binding.textRefluxoMinimo.text = getString(R.string.textoRefluxMin, resposta.rMinRefluxo)
        binding.textRefluxo.text = getString(R.string.textoRefluxo, resposta.rRefluxo)
        binding.textDegraus.text = getString(R.string.textoDegraus)
    }


    private fun exportarECompartilharCsv(resultado: McTResultados.Sucesso) {
        viewLifecycleOwner.lifecycleScope.launch {
            val uri = csvExportHelper.gerarCsvEObterUri(resultado)
            if (uri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "McCabe-Thiele Results")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.toolbar_export)))
            } else {
                exibirSnackbarTopo(getString(R.string.exportacao_erro), false)
            }
        }
    }



    private fun exibirSnackbarTopo(mensagem: String, indefinida: Boolean = true) {
        val binding = _binding ?: return

        binding.root.post {
            if (_binding == null || context == null) return@post

            val duracao = when {
                indefinida -> Snackbar.LENGTH_INDEFINITE
                else -> Snackbar.LENGTH_LONG
            }

            val snackbar = Snackbar.make(binding.root, mensagem, duracao)

            if (indefinida) {
                snackbar.setAction("OK") {}
            }

            val snackbarView = snackbar.view
            val params = snackbarView.layoutParams

            if (params is CoordinatorLayout.LayoutParams) {
                params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                params.topMargin = resources.getDimensionPixelSize(R.dimen.margin_large)
                snackbarView.layoutParams = params
            } else if (params is FrameLayout.LayoutParams) {
                params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                params.topMargin = resources.getDimensionPixelSize(R.dimen.margin_large)
                snackbarView.layoutParams = params
            }
            snackbarView.elevation = 100f
            snackbar.show()
        }
    }


}
