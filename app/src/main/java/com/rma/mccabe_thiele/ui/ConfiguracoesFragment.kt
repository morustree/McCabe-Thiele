package com.rma.mccabe_thiele.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.rma.mccabe_thiele.R
import com.rma.mccabe_thiele.databinding.FragmentConfiguracoesBinding
import java.util.Locale
import com.rma.mccabe_thiele.databinding.CardPadraoBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ConfiguracoesFragment : Fragment() {

    private var _binding: FragmentConfiguracoesBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentConfiguracoesBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSave.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        atualizarCard(binding.cardViewF1C1)
        atualizarCard(binding.cardViewF1C2)
        atualizarCard(binding.cardViewF1C3)

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun atualizarCard(cardBinding: CardPadraoBinding) {
        cardBinding.root.setOnClickListener {
            // Captura o valor que está atualmente no TextView deste card específico
            val textoAtual = cardBinding.textViewValor.text.toString()
            val valorAtual = textoAtual.toDoubleOrNull() ?: 0.8
            // Cria e abre a popup passando o valor deste card
            val popup = EspecificacoesDialogFragment(valorAtual) { valorAtualizado ->
                cardBinding.textViewValor.text = String.format(Locale.US, "%.3f", valorAtualizado)
            }
            popup.show(childFragmentManager, "ConfiguracaoValorDialog_${cardBinding.root.id}")
        }
    }


}