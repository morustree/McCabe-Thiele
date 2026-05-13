package com.rma.mccabe_thiele

data class Especificacoes(
    val xD: Double, // fração molar do componente mais volátil no destilado
    val xB: Double, // fração molar do componente mais volátil no resíduo
    val zF: Double, // fração molar do componente mais volátil na carga
    val valorq: Double, // condição da carga
    val vazaoF: Double, // vazão da carga
    val razoesR: Double // razão de refluxo / refluxo mínimo (R/Rmin); normalmente entre 1.1 e 1.5
)
