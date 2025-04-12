<template>
    <div class="d-flex flex-column justify-content-center align-items-center vh-100 bg-light">
      <!-- Title -->
      <div class="text-center">
        <h1 class="display-4">Outsider Trading</h1>
        <p class="lead">Market Manipulation for The Common Man</p>
      </div>
      <!-- Prompt -->
      <div class="position-relative w-50 mt-4">
        <input
          v-model="name"
          @keyup.enter="goNext"
          type="text"
          class="form-control form-control-lg rounded-pill pe-5"
          placeholder="Enter your screen name"
        />
        <button id="login-next-button" class="btn btn-primary position-absolute end-0 top-50 translate-middle-y me-2 rounded-circle d-flex align-items-center justify-content-center" @click="goNext">
          <span class="fa fa-arrow-right"></span>
        </button>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref } from 'vue'
  import Cookies from 'js-cookie'
  import { useRouter } from 'vue-router'
  
  const name = ref('')
  const router = useRouter()
  
  function goNext() {
    if (!name.value.trim()) return
    Cookies.set('screen_name', name.value)
    router.push('/next')
  }
  </script>