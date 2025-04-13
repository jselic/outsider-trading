<template>
    <div class="container-fluid vh-100">
        <!--MObile Toggle-->
        <div class="row d-flex d-md-none">
            <div class="col-12">
                <div class="btn-group w-100" role="group">
                    <button type="button"
                            class="btn"
                            :class="showChatHistory ? 'btn-secondary' : 'btn-outline-secondary'"
                            @click="showChatHistory = true">
                        Chat
                    </button>
                    <button type="button"
                            class="btn"
                            :class="!showChatHistory ? 'btn-secondary' : 'btn-outline-secondary'"
                            @click="showChatHistory = false">
                        Chart
                    </button>
                </div>
            </div>
        </div>
        <!--Mobile layout-->
        <div class="row d-flex d-md-none h-100">
            <div class="col-12 d-flex flex-column h-100">
                <div class="flex-grow-1 overflow-auto" v-if="showChatHistory">
                    <!-- Chat history -->
                    <div class="chat-history p-3">
                        <div v-for="(post, index) in posts" :key="index">
                            <div class="speech-bubble">
                                <p><strong>{{ post.poster }}: </strong>{{ post.message }}</p>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="flex-grow-1 overflow-auto" v-else>
                    <!-- Stock chart -->
                    <StockChart style="width: 100%; height: 100%;" />
                </div>
                <div class="flex-shrink-0 d-flex align-items-center p-2">
                    <div class="input-group w-100">
                        <input id="new_tweet" type="text" class="form-control rounded-pill" v-model="newMessage" placeholder="Spew Disinformation..." @keyup.enter="postMessage" />
                        <button class="btn btn-primary rounded-circle ms-2" @click="postMessage">
                            <i class="fa fa-arrow-right"></i>
                        </button>
                    </div>
                </div>
            </div>
        </div>
        <!--Desktop layout-->
        <div class="row d-none d-md-flex h-100">
            <!--Chat history-->
            <div class="col-md-2 bg-light border-end d-flex flex-column h-100">
                <div class="chat-history pt-3 pb-3" style="height: 100%; overflow-y: scroll;">
                    <!--Example messages-->
                    <div v-for="(post, index) in posts" :key="index">
                        <div class="speech-bubble">
                            <p><strong>{{ post.poster }}: </strong>{{ post.message }}</p>
                        </div>
                    </div>
                </div>
            </div>
            <!--Graph Area-->
            <div class="col-md-10 d-flex flex-column h-100">
                <div class="col-12 d-flex align-items-center" style="height: 80%;">
                    <!--Stock graph will go here-->
                    <StockChart style="width: 100%; height: 100%;"/>
                </div>
                <div class="flex-shrink-0 d-flex align-items-center" style="height: 20%;">
                    <div class="input-group w-100">
                        <input id="new_tweet" type="text" class="form-control rounded-pill" v-model="newMessage" placeholder="Spew Disinformation..." @keyup.enter="postMessage" />
                        <button class="btn btn-primary rounded-circle ms-2" @click="postMessage">
                            <i class="fa fa-arrow-right"></i>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import StockChart from './StockChart.vue'
import Cookies from 'js-cookie'

export default {
    components: {
        StockChart
    },
    data() {
        return {
            showChatHistory: true,
            posts: [],
            postSocket: null,
            newMessage: ''
        };
    },
    mounted() {
        const socket = new WebSocket('ws://10.32.254.39:8219/user');
        this.postSocket = new WebSocket('ws://10.32.254.39:8219/post');

        socket.onopen = () => {
            console.log('Websocket connection established');
        };

        socket.onmessage = (event) => {
            const message = JSON.parse(event.data);

            if (message.posts) {
                this.posts = message.posts;
            }
            console.log("Posts Updated")
        };
    },
    methods: {
        async postMessage() {
            if(!this.newMessage.trim()) return;

            const messagePayload = {
                poster: Cookies.get('screen_name'),
                message: this.newMessage
            }

            try {
                const response = await fetch('http://10.32.254.39:8219/post', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(messagePayload)
                });

                if (response.ok) {
                    this.newMessage = '';
                } else {
                    console.error('Failed to post message:', await response.text());
                }
            } catch (error) {
                console.error('Error posting message: ', error);
            }
        }
    }
};
</script>