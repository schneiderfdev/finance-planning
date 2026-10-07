package com.financeplanning;

import com.financeplanning.dto.DespesaPorCategoria;
import com.financeplanning.dto.ResumoMensal;
import com.financeplanning.exception.RecursoNaoEncontradoException;
import com.financeplanning.model.Categoria;
import com.financeplanning.model.TipoTransacao;
import com.financeplanning.model.Transacao;
import com.financeplanning.repository.CategoriaRepository;
import com.financeplanning.repository.TransacaoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final CategoriaRepository categoriaRepository;

    public TransacaoService(TransacaoRepository transacaoRepository,
                            CategoriaRepository categoriaRepository) {
        this.transacaoRepository = transacaoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public List<Transacao> listar(Integer ano, Integer mes) {
        if (ano != null && mes != null) {
            YearMonth periodo = YearMonth.of(ano, mes);
            return transacaoRepository.findByDataBetweenOrderByDataDesc(
                    periodo.atDay(1), periodo.atEndOfMonth());
        }
        return transacaoRepository.findAllByOrderByDataDesc();
    }

    public Transacao buscar(Long id) {
        return transacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Transação não encontrada: " + id));
    }

    public Transacao criar(Transacao transacao) {
        transacao.setId(null);
        transacao.setCategoria(resolverCategoria(transacao.getCategoria()));
        return transacaoRepository.save(transacao);
    }

    public Transacao atualizar(Long id, Transacao dados) {
        Transacao existente = buscar(id);
        existente.setDescricao(dados.getDescricao());
        existente.setValor(dados.getValor());
        existente.setData(dados.getData());
        existente.setTipo(dados.getTipo());
        existente.setCategoria(resolverCategoria(dados.getCategoria()));
        return transacaoRepository.save(existente);
    }

    public void deletar(Long id) {
        Transacao existente = buscar(id);
        transacaoRepository.delete(existente);
    }

    public ResumoMensal resumo(Integer ano, Integer mes) {
        YearMonth periodo = (ano != null && mes != null)
                ? YearMonth.of(ano, mes)
                : YearMonth.now();

        LocalDate inicio = periodo.atDay(1);
        LocalDate fim = periodo.atEndOfMonth();

        BigDecimal receitas = transacaoRepository.somarPorTipoNoPeriodo(TipoTransacao.RECEITA, inicio, fim);
        BigDecimal despesas = transacaoRepository.somarPorTipoNoPeriodo(TipoTransacao.DESPESA, inicio, fim);
        List<DespesaPorCategoria> porCategoria = transacaoRepository.despesasPorCategoria(inicio, fim);

        return new ResumoMensal(
                periodo.getYear(),
                periodo.getMonthValue(),
                receitas,
                despesas,
                receitas.subtract(despesas),
                porCategoria
        );
    }

    private Categoria resolverCategoria(Categoria categoria) {
        if (categoria == null || categoria.getId() == null) {
            return null;
        }
        return categoriaRepository.findById(categoria.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Categoria não encontrada: " + categoria.getId()));
    }
}