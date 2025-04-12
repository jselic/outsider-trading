<template>
    <canvas ref="canvas"></canvas>
</template>

<script>
import {Chart} from 'chart.js/auto'
import data from '../../public/test_data.json'

export default {
    mounted() {
        const ctx = this.$refs.canvas.getContext('2d')
        const company = data.companies[0]
        new Chart(ctx, {
            type: 'line',
            data: {
                labels: company.performance.lastValues.map((_, i) => i + 1),
                datasets: [{
                    label: company.id,
                    data: company.performance.lastValues,
                    borderColor: 'rgba(75, 192, 192, 1)',
                    tension: 0.3
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false
            }
        })
    }
}
</script>