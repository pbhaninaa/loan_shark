import SockJS from "sockjs-client"
import Stomp from "stompjs"

import requestSound from "../assets/request.mp3"
import notificationSound from "../assets/notification.mp3"

let stompClient = null

// Request sound (loops until accepted)
let requestAudio = new Audio(requestSound)
requestAudio.loop = true

// Normal notification sound (plays once)
let notificationAudio = new Audio(notificationSound)
notificationAudio.loop = false


// 🔔 Play when a new client request arrives
export function playRequestSound(){
  requestAudio.play()
}

// Stop request ringing
export function stopRequestSound(){
  requestAudio.pause()
  requestAudio.currentTime = 0
}

// 🔔 Play normal notification
export function playNotificationSound(){
  notificationAudio.play()
}


export function connectNotificationSocket(callback) {

  const socket = new SockJS("http://localhost:8080/ws")

  stompClient = Stomp.over(socket)

  stompClient.connect({}, () => {

    console.log("Connected to notification socket")

    stompClient.subscribe("/topic/provider-requests", (message) => {

        const data = JSON.parse(message.body)

        callback(data)

    })

  })

}