import { createRouter, createWebHistory } from 'vue-router'
import NameInput from '../components/NameInput.vue'
import Dashboard from '../components/Dashboard.vue'

const routes = [
    { path: '/', component: NameInput },
    { path: '/next', component: Dashboard },
]

export default createRouter({
    history: createWebHistory(),
    routes,
})