<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { Chart, DoughnutController, ArcElement, Tooltip, Legend } from 'chart.js'

Chart.register(DoughnutController, ArcElement, Tooltip, Legend)

const props = defineProps({
  dados: { type: Array, default: () => [] }
})

const canvas = ref(null)
let grafico = null

const cores = ['#2563eb', '#16a34a', '#f59e0b', '#dc2626', '#9333ea', '#0891b2', '#db2777', '#65a30d']

function desenhar() {
  if (grafico) {
    grafico.destroy()
    grafico = null
  }
  if (!canvas.value || !props.dados.length) return

  grafico = new Chart(canvas.value, {
    type: 'doughnut',
    data: {
      labels: props.dados.map((d) => d.categoria),
      datasets: [
        {
          data: props.dados.map((d) => Number(d.total)),
          backgroundColor: props.dados.map((_, i) => cores[i % cores.length])
        }
      ]
    },
    options: {
      plugins: { legend: { position: 'bottom' } }
    }
  })
}

onMounted(desenhar)
watch(() => props.dados, desenhar, { deep: true })
onBeforeUnmount(() => {
  if (grafico) grafico.destroy()
})
</script>

<template>
  <div>
    <canvas v-show="dados.length" ref="canvas"></canvas>
    <p v-if="!dados.length" class="vazio">Sem despesas neste mês.</p>
  </div>
</template>

<style>
.vazio {
  color: #6b7280;
  text-align: center;
  padding: 24px 0;
}
</style>