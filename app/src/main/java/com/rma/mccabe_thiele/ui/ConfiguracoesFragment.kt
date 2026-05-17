package com.rma.mccabe_thiele.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import com.google.android.material.snackbar.Snackbar
import com.rma.mccabe_thiele.R
import com.rma.mccabe_thiele.data.PreferencesManager
import com.rma.mccabe_thiele.databinding.FragmentConfiguracoesBinding
import java.util.Locale
import com.rma.mccabe_thiele.databinding.CardPadraoBinding
import com.rma.mccabe_thiele.model.McTEspecificacoes
import androidx.core.view.MenuProvider
import android.view.Menu
import android.view.MenuInflater


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

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Limpa todos os ícones e os 3 pontos (toolbar) herdados da MainActivity nesta tela
                menu.clear()
            }
            override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)


        binding.buttonSave.setOnClickListener {
            val xD = binding.cardViewxD.textViewValor.text.toString().toDoubleOrNull() ?: 0.7
            val xB = binding.cardViewxB.textViewValor.text.toString().toDoubleOrNull() ?: 0.2
            val zF = binding.cardViewzF.textViewValor.text.toString().toDoubleOrNull() ?: 0.5
            val valorq = binding.cardViewValorq.textViewValor.text.toString().toDoubleOrNull() ?: 0.5
            val vazaoF = binding.cardViewVazaoF.textViewValor.text.toString().toDoubleOrNull() ?: 100.0
            val razoesR = binding.cardViewRazoesR.textViewValor.text.toString().toDoubleOrNull() ?: 1.3
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
        binding.cardViewxD.textViewValor.text = String.format(Locale.US, "%.3f", specsSalvas.xD)
        binding.cardViewxB.textViewValor.text = String.format(Locale.US, "%.3f", specsSalvas.xB)
        binding.cardViewzF.textViewValor.text = String.format(Locale.US, "%.3f", specsSalvas.zF)
        binding.cardViewValorq.textViewValor.text = String.format(Locale.US, "%.3f", specsSalvas.valorq)
        binding.cardViewVazaoF.textViewValor.text = String.format(Locale.US, "%.3f", specsSalvas.vazaoF)
        binding.cardViewRazoesR.textViewValor.text = String.format(Locale.US, "%.3f", specsSalvas.razoesR)

        binding.cardViewxD.textViewTitulo.text = getString(R.string.spec_xd)
        binding.cardViewxB.textViewTitulo.text = getString(R.string.spec_xb)
        binding.cardViewzF.textViewTitulo.text = getString(R.string.spec_zf)
        binding.cardViewValorq.textViewTitulo.text = getString(R.string.spec_valorq)
        binding.cardViewVazaoF.textViewTitulo.text = getString(R.string.spec_vazaof)
        binding.cardViewRazoesR.textViewTitulo.text = getString(R.string.spec_razoesr)

        atualizarCard(binding.cardViewxD, EspecificacoesDialogFragment.TipoParametro.XD)
        atualizarCard(binding.cardViewxB, EspecificacoesDialogFragment.TipoParametro.XB)
        atualizarCard(binding.cardViewzF, EspecificacoesDialogFragment.TipoParametro.ZF)
        atualizarCard(binding.cardViewValorq, EspecificacoesDialogFragment.TipoParametro.VALOR_Q)
        atualizarCard(binding.cardViewVazaoF, EspecificacoesDialogFragment.TipoParametro.VAZAO_F)
        atualizarCard(binding.cardViewRazoesR, EspecificacoesDialogFragment.TipoParametro.RAZOES_R)

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun atualizarCard(cardBinding: CardPadraoBinding, tipo: EspecificacoesDialogFragment.TipoParametro) {
        // configurar listener
        cardBinding.root.setOnClickListener {
            // Captura o valor que está atualmente no TextView do card específico
            val textoAtual = cardBinding.textViewValor.text.toString()
            val valorAtual = textoAtual.toDoubleOrNull() ?: 0.8
            // Cria e abre a popup passando o valor deste card
            val popup = EspecificacoesDialogFragment(valorAtual, tipo) { valorAtualizado ->
                cardBinding.textViewValor.text = String.format(Locale.US, "%.3f", valorAtualizado)
            }
            popup.show(childFragmentManager, "ConfiguracaoValorDialog_${cardBinding.root.id}")
        }
    }


}