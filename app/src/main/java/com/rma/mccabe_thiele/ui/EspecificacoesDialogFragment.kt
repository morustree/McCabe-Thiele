package com.rma.mccabe_thiele.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.rma.mccabe_thiele.databinding.PopupBinding
import android.text.Editable
import android.text.TextWatcher
import java.util.Locale


class EspecificacoesDialogFragment(
    private val valorInicial: Double,
    private val tipo: TipoParametro,
    private val onValorSalvo: (Double) -> Unit
): DialogFragment() {

    enum class TipoParametro {
        XD, XB, ZF, VALOR_Q, VAZAO_F, RAZOES_R
    }

    private var _binding: PopupBinding? = null
    private val binding get() = _binding!!

    // Flags de controle para evitar o loop de eventos recursivos
    private var atualizandoPeloSlider = false
    private var atualizandoPeloTexto = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = PopupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usaSlider = tipo == TipoParametro.XD || tipo == TipoParametro.XB || tipo == TipoParametro.ZF

        if (usaSlider) {
            binding.sliderPopup.visibility = View.VISIBLE
            val valorValidado = valorInicial.coerceIn(0.0, 1.0)
            binding.sliderPopup.value = valorValidado.toFloat()
            binding.inputEditTextPopUp.setText(String.format(Locale.US, "%.3f", valorValidado))
        } else {
            binding.sliderPopup.visibility = View.GONE
            binding.inputEditTextPopUp.setText(String.format(Locale.US, "%.3f", valorInicial))
        }

        // Sincroniza slider para edittext
        binding.sliderPopup.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !atualizandoPeloTexto && usaSlider) {
                atualizandoPeloSlider = true
                val textoFormatado = String.format(Locale.US,"%.3f", value)
                if (binding.inputEditTextPopUp.text.toString() != textoFormatado) {
                    binding.inputEditTextPopUp.setText(textoFormatado)
                    binding.inputEditTextPopUp.setSelection(textoFormatado.length)
                }
                atualizandoPeloSlider = false
            }
        }

        // Sincroniza edittext para slider ou validações específicas
        binding.inputEditTextPopUp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!atualizandoPeloSlider) {
                    atualizandoPeloTexto = true
                    val texto = s.toString().trim()
                    val valorDigitado = texto.toDoubleOrNull()
                    if (valorDigitado != null) {
                        if (usaSlider) {
                            val valorLimitado = valorDigitado.coerceIn(0.0, 1.0).toFloat()
                            if (binding.sliderPopup.value != valorLimitado) {
                                binding.sliderPopup.value = valorLimitado
                            }
                        }
                    }
                    atualizandoPeloTexto = false
                }
            }
        })

        // Retorna o valor final para o Fragment/Activity de origem
        binding.imageButton.setOnClickListener {
            val texto = binding.inputEditTextPopUp.text.toString().trim()
            var valorFinal = texto.toDoubleOrNull() ?: valorInicial

            // Validações finais antes de salvar
            when (tipo) {
                TipoParametro.XD, TipoParametro.XB, TipoParametro.ZF -> {
                    valorFinal = valorFinal.coerceIn(0.0, 1.0)
                }
                TipoParametro.RAZOES_R -> {
                    if (valorFinal <= 1.0) {
                        valorFinal = 1.01 // ou apenas avisar o usuário, mas aqui vamos garantir > 1
                    }
                }
                else -> {} // valorq e vazaoF sem restrição 0-1
            }

            onValorSalvo(valorFinal)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        // Define o tamanho responsivo para o DialogFragment envolver o CardView do XML
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        // Torna o fundo do diálogo transparente para manter as bordas arredondadas do seu CardView
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}