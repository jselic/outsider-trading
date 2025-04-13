<template>
    <canvas ref="canvas"></canvas>
</template>

<script>
import {Chart} from 'chart.js/auto'
import data from '../../public/test_data.json'

export default {
    mounted() {
        const ctx = this.$refs.canvas.getContext('2d')
        
        function getRandomColor() {
            const r = Math.floor(Math.random() * 256);
            const g = Math.floor(Math.random() * 256);
            const b = Math.floor(Math.random() * 256);
            return `rgba(${r}, ${g}, ${b}, 1)`;
        }

        const companyColors = {}
        
        const chart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: [],
                datasets: data.companies.map(company => ({
                    label: '',
                    data: [],
                    borderColor: getRandomColor(),
                    tension: 0.3
                }))
            },
            options: {
                responsive: true,
                maintainAspectRatio: false
            }
        })

        const socket = new WebSocket('ws://10.32.254.39:8219/user')
    
        socket.onopen = () => {
            console.log('WebSocket connection established')
        }

        socket.onmessage = (event) => {
            const message = JSON.parse(event.data)

            if (Object.keys(companyColors).length === 0){
                message.companies.forEach(company => {
                    if (!companyColors[company.id]) {
                        companyColors[company.id] = getRandomColor()
                    }
                })
            }
        
            chart.data.datasets = message.companies.map(company => ({
                label: company.id,
                data: company.performance.lastValues,
                borderColor: companyColors[company.id],
                tension: 0.3
            }))

            const newData = message.companies[0].performance.lastValues
            chart.data.labels = newData.map((_, i) => i + 1)
            chart.update()
            console.log("Updated")
        }
    }
}
</script>