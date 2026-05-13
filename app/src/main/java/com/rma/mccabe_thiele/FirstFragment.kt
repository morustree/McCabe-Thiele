package com.rma.mccabe_thiele

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.rma.mccabe_thiele.databinding.FragmentFirstBinding
import org.apache.commons.math3.analysis.UnivariateFunction

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            //findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            val especificacoes: Especificacoes
            val curvaEquilibirio: UnivariateFunction
            val metodoMcCabeThiele = Metodo(especificacoes, curvaEquilibirio)
            when (val resultados = metodoMcCabeThiele.calcular()) {
                is Resultados.Sucesso -> {
                    // mostrar resultados na tela
                    // desenhar gráfico
                }
                is Resultados.Erro -> {
                    Snackbar.make(binding.root, resultados.mensagem, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}