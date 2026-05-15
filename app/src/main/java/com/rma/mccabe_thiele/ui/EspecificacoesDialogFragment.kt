package com.rma.mccabe_thiele.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.rma.mccabe_thiele.databinding.PopupBinding
import android.text.Editable
import android.text.TextWatcher


class EspecificacoesDialogFragment(
    private val valorInicial: Double,
    private val onValorSalvo: (Double) -> Unit
): DialogFragment() {

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

        // Inicializa os componentes com o valor que veio do CardView
        val valorValidado = valorInicial.coerceIn(0.0, 1.0) // Garante que está entre o valueFrom e valueTo do Slider
        binding.sliderPopup.value = valorValidado.toFloat()
        binding.inputEditTextPopUp.setText(String.format("%.3f", valorValidado).replace(",", "."))

        // Sincroniza slider para edittext
        binding.sliderPopup.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !atualizandoPeloTexto) {
                atualizandoPeloSlider = true
                val textoFormatado = String.format("%.3f", value).replace(",", ".")
                // Verifica se o texto realmente mudou para evitar redundância
                if (binding.inputEditTextPopUp.text.toString() != textoFormatado) {
                    binding.inputEditTextPopUp.setText(textoFormatado)
                    // Move o cursor do teclado para o final do texto
                    binding.inputEditTextPopUp.setSelection(textoFormatado.length)
                }
                atualizandoPeloSlider = false
            }
        }

        // Sincroniza edittext para slider
        binding.inputEditTextPopUp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!atualizandoPeloSlider) {
                    atualizandoPeloTexto = true
                    val texto = s.toString().trim()
                    val valorDigitado = texto.toDoubleOrNull()
                    if (valorDigitado != null) {
                        // Limita o valor estritamente entre 0.0 e 1.0
                        val valorLimitado = valorDigitado.coerceIn(0.0, 1.0).toFloat()
                        // Só atualiza o slider se o valor for numericamente diferente
                        if (binding.sliderPopup.value != valorLimitado) {
                            binding.sliderPopup.value = valorLimitado
                        }
                    }
                    atualizandoPeloTexto = false
                }
            }
        })

        // Retorna o valor final para o Fragment/Activity de origem
        binding.buttonSave.setOnClickListener {
            val valorFinal = binding.sliderPopup.value.toDouble()
            onValorSalvo(valorFinal)
            dismiss() // Fecha o popup
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