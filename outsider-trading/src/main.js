import { createApp } from 'vue'
import App from './App.vue'

import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import 'bootstrap/dist/css/bootstrap.min.css'
import 'font-awesome/css/font-awesome.css';
import './assets/styles.scss'

import router from './router'

createApp(App).use(router).mount('#app')
