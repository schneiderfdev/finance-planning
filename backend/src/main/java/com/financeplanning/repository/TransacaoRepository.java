package com.financeplanning.repository;

import com.financeplanning.dto.DespesaPorCategoria;
import com.financeplanning.model.TipoTransacao;
import com.financeplanning.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    List<Transacao> findAllByOrderByDataDesc();

    List<Transacao> findByDataBetweenOrderByDataDesc(LocalDate inicio, LocalDate fim);

    @Query("SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t "
            + "WHERE t.tipo = :tipo AND t.data BETWEEN :inicio AND :fim")
    BigDecimal somarPorTipoNoPeriodo(@Param("tipo") TipoTransacao tipo,
                                     @Param("inicio") LocalDate inicio,
                                     @Param("fim") LocalDate fim);

    @Query("SELECT new com.financeplanning.dto.DespesaPorCategoria("
            + "COALESCE(c.nome, 'Sem categoria'), SUM(t.valor)) "
            + "FROM Transacao t LEFT JOIN t.categoria c "
            + "WHERE t.tipo = com.financeplanning.model.TipoTransacao.DESPESA "
            + "AND t.data BETWEEN :inicio AND :fim "
            + "GROUP BY c.nome "
            + "ORDER BY SUM(t.valor) DESC")
    List<DespesaPorCategoria> despesasPorCategoria(@Param("inicio") LocalDate inicio,
                                                   @Param("fim") LocalDate fim);
}