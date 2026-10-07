package com.financeplanning.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumoMensal(
        int ano,
        int mes,
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldo,
        List<DespesaPorCategoria> despesasPorCategoria
) {
}