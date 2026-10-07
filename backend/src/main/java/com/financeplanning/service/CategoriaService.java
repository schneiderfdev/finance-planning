package com.financeplanning.service;

import com.financeplanning.exception.RecursoNaoEncontradoException;
import com.financeplanning.model.Categoria;
import com.financeplanning.repository.CategoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    public Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada: " + id));
    }

    public Categoria criar(Categoria categoria) {
        categoria.setId(null);
        return categoriaRepository.save(categoria);
    }

    public Categoria atualizar(Long id, Categoria dados) {
        Categoria existente = buscar(id);
        existente.setNome(dados.getNome());
        return categoriaRepository.save(existente);
    }

    public void deletar(Long id) {
        Categoria existente = buscar(id);
        categoriaRepository.delete(existente);
    }
}