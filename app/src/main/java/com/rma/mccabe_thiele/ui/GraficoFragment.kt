package com.rma.mccabe_thiele.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar
import com.rma.mccabe_thiele.databinding.FragmentGraficoBinding
import com.rma.mccabe_thiele.helper.McTChartManager
import com.rma.mccabe_thiele.model.McTEspecificacoes
import com.rma.mccabe_thiele.model.McTMetodo
import com.rma.mccabe_thiele.model.McTResultados
import org.apache.commons.math3.analysis.UnivariateFunction

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class GraficoFragment : Fragment() {

    private var _binding: FragmentGraficoBinding? = null
    private val binding get() = _binding!!

    private lateinit var chartManager: McTChartManager

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

        binding.buttonFirst.setOnClickListener {
            val curvaEquilibrio = UnivariateFunction { x -> (2.5 * x) / (1 + (2.5 - 1) * x) }
            val mcTEspecificacoes = McTEspecificacoes(
                xD = 0.92,
                xB = 0.15,
                zF = 0.45,
                valorq = 0.5,
                vazaoF = 100.0,
                razoesR = 1.3
            )
            
            val metodoMcCabeThiele = McTMetodo(mcTEspecificacoes, curvaEquilibrio)
            
            when (val resultados = metodoMcCabeThiele.calcular()) {
                is McTResultados.Sucesso -> {
                    chartManager.renderizar(
                        mcTResultados = resultados,
                        curvaEquilibrio = curvaEquilibrio,
                        zF = mcTEspecificacoes.zF,
                        valorq = mcTEspecificacoes.valorq,
                        xD = mcTEspecificacoes.xD,
                        xB = mcTEspecificacoes.xB
                    )
                }

                is McTResultados.Erro -> {
                    Snackbar.make(
                        binding.root,
                        getString(resultados.mensagem),
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction("OK"){}.show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
