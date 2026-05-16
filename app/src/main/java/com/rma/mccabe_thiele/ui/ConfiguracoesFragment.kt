package com.rma.mccabe_thiele.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar
import com.rma.mccabe_thiele.R
import com.rma.mccabe_thiele.data.PreferencesManager
import com.rma.mccabe_thiele.databinding.FragmentConfiguracoesBinding
import java.util.Locale
import com.rma.mccabe_thiele.databinding.CardPadraoBinding
import com.rma.mccabe_thiele.model.McTEspecificacoes

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ConfiguracoesFragment : Fragment() {

    private var _binding: FragmentConfiguracoesBinding? = null
    private val binding get() = _binding!!

    private val prefsManager by lazy { PreferencesManager(requireContext()) }


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
            val xD = binding.cardViewF1C1.textViewValor.text.toString().toDoubleOrNull() ?: 0.7
            val xB = binding.cardViewF1C2.textViewValor.text.toString().toDoubleOrNull() ?: 0.2
            val zF = binding.cardViewF1C3.textViewValor.text.toString().toDoubleOrNull() ?: 0.5
            val valorq = binding.cardViewF2C1.textViewValor.text.toString().toDoubleOrNull() ?: 0.5
            val vazaoF = binding.cardViewF3C1.textViewValor.text.toString().toDoubleOrNull() ?: 100.0
            val razoesR = binding.cardViewF3C2.textViewValor.text.toString().toDoubleOrNull() ?: 1.3
            val specs = McTEspecificacoes(
                xD = xD,
                xB = xB,
                zF = zF,
                valorq = valorq,
                vazaoF = vazaoF,
                razoesR = razoesR
            )
            prefsManager.salvarEspecificacoes(specs)
            Snackbar.make(binding.root, getString(R.string.button_save_resultado), Snackbar.LENGTH_SHORT).show()
        }

        val specsSalvas = prefsManager.lerEspecificacoes()
        binding.cardViewF1C1.textViewValor.text = String.format(Locale.US, "%.3f", specsSalvas.xD)
        binding.cardViewF1C2.textViewValor.text = String.format(Locale.US, "%.3f", specsSalvas.xB)
        binding.cardViewF1C3.textViewValor.text = String.format(Locale.US, "%.3f", specsSalvas.zF)
        binding.cardViewF2C1.textViewValor.text = String.format(Locale.US, "%.3f", specsSalvas.valorq)
        binding.cardViewF3C1.textViewValor.text = String.format(Locale.US, "%.3f", specsSalvas.vazaoF)
        binding.cardViewF3C2.textViewValor.text = String.format(Locale.US, "%.3f", specsSalvas.razoesR)


        atualizarCard(binding.cardViewF1C1)
        atualizarCard(binding.cardViewF1C2)
        atualizarCard(binding.cardViewF1C3)
        atualizarCard(binding.cardViewF2C1)
        atualizarCard(binding.cardViewF3C1)
        atualizarCard(binding.cardViewF3C2)

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