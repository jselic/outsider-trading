<template>
    <div class="container-fluid vh-100">
        <!--Desktop layout-->
        <div class="row d-none d-md-flex h-100">
            <!--Chat history-->
            <div class="col-md-2 bg-light border-end">
                <div class="chat-history" style="height: 100%; overflow-y: scroll;">
                    <!--Example messages-->
                    <div class="message desktop-message">
                        <p><strong>Melon Husk:</strong> I hate $MDGT stock. I am going to nuke Mars now.</p>
                    </div>
                </div>
            </div>
            <!--Graph Area-->
            <div class="col-md-10">
                <div class="row h-100">
                    <div class="col-12 d-flex align-items-center" style="height: 80%;">
                        <!--Stock graph will go here-->
                        <StockChart style="width: 100%; height: 100%;"/>
                    </div>
                    <div class="col-12 d-flex align-items-center" style="height: 20%;">
                        <div class="input-group">
                            <input type="text" class="form-control rounded-pill" placeholder="Spew Disinformation..." />
                            <button class="btn btn-primary rounded-circle ms-2">
                                <i class="fa fa-arrow-right"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <!--Mobile Layout-->
        <div class="d-md-none position-relative" style="height: 100%;">
            <!--Slider toggle between chat history and stock graph-->
            <div class="position-absolute top-0 start-0 w-100" style="height: 85%; padding:10px">
                <div class="btn-group w-100" role="group" aria-label="Toggle between Chat and Stocks">
                    <button type="button" class="btn btn-secondary w-50" @click="showChat = true">Chat</button>
                    <button type="button" class="btn btn-secondary w-50" @click="showChat = false">Stocks</button>
                </div>

                <div id="mobile-chat" v-if="showChat" style="height: 100%; overflow-y: scroll;">
                    <!--Chats go here-->
                    <div class="message">
                        <p><strong>Kradislav Uzmič:</strong>$THFT is surging today. Truly a miraculous time to BUY!</p>
                    </div>
                </div>
                <div id="mobile-stonks" v-else style="height: 100%; background-color: #f0f0f0;">
                    <p>Stock Graph Placeholder</p>
                </div>
            </div>
            <!--Text prompt statinoary-->
            <div class="position-absolute bottom-0 start-0 w-100 p-2 flex flex-column" style="height: 15%; padding: 10px;">
                <div class="input-group mt-auto">
                    <input type="text" class="form-control" placeholder="Spew Disinformation">
                    <button class="btn btn-primary"><span class="fa fa-arrow-right"></span></button>
                </div>
            </div>
        </div>
    </div>

</template>

<script>
import StockChart from './StockChart.vue'

export default {
    components: {
        StockChart
    },
    data() {
        return {
            showChat: true
        };
    }
};
</script>