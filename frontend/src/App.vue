<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import api from './services/api'
import DespesasChart from './components/DespesasChart.vue'

const pad = (n) => String(n).padStart(2, '0')
const hoje = new Date()
const hojeIso = `${hoje.getFullYear()}-${pad(hoje.getMonth() + 1)}-${pad(hoje.getDate())}`

const mesSelecionado = ref(hojeIso.slice(0, 7))
const transacoes = ref([])
const categorias = ref([])
const resumo = ref({ totalReceitas: 0, totalDespesas: 0, saldo: 0, despesasPorCategoria: [] })
const erro = ref('')
const editandoId = ref(null)
const novaCategoria = ref('')

const form = reactive({
  descricao: '',
  valor: '',
  data: hojeIso,
  tipo: 'DESPESA',
  categoriaId: ''
})

const moeda = (v) =>
  Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })

const dataBr = (iso) => iso.split('-').reverse().join('/')

function mensagemDeErro(e) {
  const d = e.response?.data
  if (d?.campos) return Object.values(d.campos).join(' | ')
  if (d?.erro) return d.erro
  return 'Não foi possível concluir a operação. O backend está rodando?'
}

async function carregar() {
  if (!mesSelecionado.value) return
  try {
    const [ano, mes] = mesSelecionado.value.split('-').map(Number)
    const params = { ano, mes }
    const [t, r, c] = await Promise.all([
      api.get('/transacoes', { params }),
      api.get('/resumo', { params }),
      api.get('/categorias')
    ])
    transacoes.value = t.data
    resumo.value = r.data
    categorias.value = c.data
    erro.value = ''
  } catch (e) {
    erro.value = mensagemDeErro(e)
  }
}

function limparFormulario() {
  editandoId.value = null
  Object.assign(form, {
    descricao: '',
    valor: '',
    data: hojeIso,
    tipo: 'DESPESA',
    categoriaId: ''
  })
}

async function salvar() {
  const payload = {
    descricao: form.descricao,
    valor: Number(form.valor),
    data: form.data,
    tipo: form.tipo,
    categoria: form.categoriaId ? { id: Number(form.categoriaId) } : null
  }
  try {
    if (editandoId.value) {
      await api.put(`/transacoes/${editandoId.value}`, payload)
    } else {
      await api.post('/transacoes', payload)
    }
    mesSelecionado.value = payload.data.slice(0, 7)
    limparFormulario()
    await carregar()
  } catch (e) {
    erro.value = mensagemDeErro(e)
  }
}

function editar(t) {
  editandoId.value = t.id
  Object.assign(form, {
    descricao: t.descricao,
    valor: t.valor,
    data: t.data,
    tipo: t.tipo,
    categoriaId: t.categoria ? t.categoria.id : ''
  })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function excluir(id) {
  if (!confirm('Excluir esta transação?')) return
  try {
    await api.delete(`/transacoes/${id}`)
    await carregar()
  } catch (e) {
    erro.value = mensagemDeErro(e)
  }
}

async function criarCategoria() {
  const nome = novaCategoria.value.trim()
  if (!nome) return
  try {
    await api.post('/categorias', { nome })
    novaCategoria.value = ''
    await carregar()
  } catch (e) {
    erro.value = mensagemDeErro(e)
  }
}

watch(mesSelecionado, carregar)
onMounted(carregar)
</script>

<template>
  <main class="container">
    <header class="topo">
      <h1>Finance Planning</h1>
      <input type="month" v-model="mesSelecionado" />
    </header>

    <p v-if="erro" class="erro">{{ erro }}</p>

    <section class="cards">
      <div class="card">
        <span>Receitas</span>
        <strong class="verde">{{ moeda(resumo.totalReceitas) }}</strong>
      </div>
      <div class="card">
        <span>Despesas</span>
        <strong class="vermelho">{{ moeda(resumo.totalDespesas) }}</strong>
      </div>
      <div class="card">
        <span>Saldo</span>
        <strong :class="Number(resumo.saldo) < 0 ? 'vermelho' : 'azul'">
          {{ moeda(resumo.saldo) }}
        </strong>
      </div>
    </section>

    <section class="grade">
      <div class="painel">
        <h2>{{ editandoId ? 'Editar transação' : 'Nova transação' }}</h2>
        <form @submit.prevent="salvar" class="formulario">
          <label>
            Descrição
            <input v-model="form.descricao" type="text" required />
          </label>
          <label>
            Valor (R$)
            <input v-model="form.valor" type="number" step="0.01" min="0.01" required />
          </label>
          <label>
            Data
            <input v-model="form.data" type="date" required />
          </label>
          <label>
            Tipo
            <select v-model="form.tipo">
              <option value="DESPESA">Despesa</option>
              <option value="RECEITA">Receita</option>
            </select>
          </label>
          <label>
            Categoria
            <select v-model="form.categoriaId">
              <option value="">Sem categoria</option>
              <option v-for="c in categorias" :key="c.id" :value="c.id">{{ c.nome }}</option>
            </select>
          </label>
          <div class="acoes">
            <button type="submit" class="primario">
              {{ editandoId ? 'Salvar alterações' : 'Adicionar' }}
            </button>
            <button v-if="editandoId" type="button" @click="limparFormulario">Cancelar</button>
          </div>
        </form>

        <div class="nova-categoria">
          <input v-model="novaCategoria" type="text" placeholder="Nova categoria" @keyup.enter="criarCategoria" />
          <button type="button" @click="criarCategoria">Criar</button>
        </div>
      </div>

      <div class="painel">
        <h2>Despesas por categoria</h2>
        <DespesasChart :dados="resumo.despesasPorCategoria" />
      </div>
    </section>

    <section class="painel">
      <h2>Transações do mês</h2>
      <p v-if="!transacoes.length" class="vazio">Nenhuma transação neste mês.</p>
      <table v-else>
        <thead>
          <tr>
            <th>Data</th>
            <th>Descrição</th>
            <th>Categoria</th>
            <th class="direita">Valor</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in transacoes" :key="t.id">
            <td>{{ dataBr(t.data) }}</td>
            <td>{{ t.descricao }}</td>
            <td>{{ t.categoria ? t.categoria.nome : '—' }}</td>
            <td class="direita" :class="t.tipo === 'RECEITA' ? 'verde' : 'vermelho'">
              {{ t.tipo === 'RECEITA' ? '+' : '−' }} {{ moeda(t.valor) }}
            </td>
            <td class="direita">
              <button type="button" @click="editar(t)">Editar</button>
              <button type="button" class="perigo" @click="excluir(t.id)">Excluir</button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </main>
</template>

<style>
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  background: #f3f4f6;
  color: #111827;
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
}

.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.topo {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.topo h1 {
  margin: 0;
  font-size: 1.6rem;
}

.erro {
  background: #fee2e2;
  color: #991b1b;
  padding: 10px 14px;
  border-radius: 8px;
}

.cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.card span {
  color: #6b7280;
  font-size: 0.85rem;
}

.card strong {
  font-size: 1.4rem;
}

.verde {
  color: #16a34a;
}

.vermelho {
  color: #dc2626;
}

.azul {
  color: #2563eb;
}

.grade {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}

.painel {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.painel h2 {
  margin: 0 0 12px;
  font-size: 1.1rem;
}

.formulario {
  display: grid;
  gap: 10px;
}

.formulario label {
  display: grid;
  gap: 4px;
  font-size: 0.85rem;
  color: #374151;
}

input,
select {
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 0.95rem;
}

button {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  font-size: 0.9rem;
}

button:hover {
  background: #f3f4f6;
}

button.primario {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}

button.primario:hover {
  background: #1d4ed8;
}

button.perigo {
  color: #dc2626;
  border-color: #fecaca;
}

.acoes {
  display: flex;
  gap: 8px;
}

.nova-categoria {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.nova-categoria input {
  flex: 1;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  text-align: left;
  padding: 10px 8px;
  border-bottom: 1px solid #e5e7eb;
  font-size: 0.92rem;
}

.direita {
  text-align: right;
}

td button {
  margin-left: 6px;
}

@media (max-width: 760px) {
  .cards,
  .grade {
    grid-template-columns: 1fr;
  }
}
</style>