import { Client } from "@stomp/stompjs"

import requestSound from "../assets/request.mp3"
import notificationSound from "../assets/notification.mp3"

let stompClient = null

let requestAudio = new Audio(requestSound)
requestAudio.loop = true

let notificationAudio = new Audio(notificationSound)
notificationAudio.loop = false

export function playRequestSound(){
  requestAudio.play().catch(err => {
    console.warn("Request sound blocked:", err)
  })
}

export function stopRequestSound(){
  requestAudio.pause()
  requestAudio.currentTime = 0
}

export function playNotificationSound(){
  notificationAudio.play().catch(err => {
    console.warn("Notification sound blocked:", err)
  })
}

export function connectNotificationSocket(callback){

  stompClient = new Client({
    brokerURL: "ws://localhost:8080/ws",

    reconnectDelay: 5000,

    debug: () => {},

    onConnect: () => {

      console.log("Connected to notification socket")

      stompClient.subscribe("/topic/provider-requests", (message) => {

        const data = JSON.parse(message.body)

        callback(data)

      })

    },

    onStompError: (frame) => {
      console.error("Broker error:", frame)
    }

  })

  stompClient.activate()

}