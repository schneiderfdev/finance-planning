package com.financeplanning.controller;

import com.financeplanning.dto.ResumoMensal;
import com.financeplanning.TransacaoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resumo")
public class ResumoController {

    private final TransacaoService transacaoService;

    public ResumoController(TransacaoService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @GetMapping
    public ResumoMensal resumo(@RequestParam(required = false) Integer ano,
                               @RequestParam(required = false) Integer mes) {
        return transacaoService.resumo(ano, mes);
    }
}