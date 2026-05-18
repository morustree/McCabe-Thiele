package com.rma.mccabe_thiele.helper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class ToolbarEventViewModel : ViewModel() {

    private val _onImportarCsvClick = MutableSharedFlow<Unit>()
    val onImportarCsvClick = _onImportarCsvClick.asSharedFlow()

    private val _botaoImportarAtivo = MutableStateFlow(true)
    val botaoImportarAtivo = _botaoImportarAtivo.asStateFlow()

    private val _botaoExportarAtivo = MutableStateFlow(false)
    val botaoExportarAtivo = _botaoExportarAtivo.asStateFlow()

    private val _onExportarCsvClick = MutableSharedFlow<Unit>()
    val onExportarCsvClick = _onExportarCsvClick.asSharedFlow()

    fun dispararCliqueImportarCsv() {
        viewModelScope.launch {
            _onImportarCsvClick.emit(Unit)
        }
    }

    fun setBotaoImportarAtivo(ativo: Boolean) {
        _botaoImportarAtivo.value = ativo
    }

    fun setBotaoExportarAtivo(ativo: Boolean) {
        _botaoExportarAtivo.value = ativo
    }

    fun dispararCliqueExportarCsv() {
        viewModelScope.launch {
            _onExportarCsvClick.emit(Unit)
        }
    }
}