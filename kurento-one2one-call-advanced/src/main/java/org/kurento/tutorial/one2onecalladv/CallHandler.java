/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.tutorial.one2onecalladv;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Protocol handler for 1 to 1 video call communication.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.0.0
 */
public class CallHandler extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(CallHandler.class);
  private static final Gson gson = new GsonBuilder().create();

  private final ConcurrentHashMap<String, MediaPipeline> pipelines = new ConcurrentHashMap<>();

  @Autowired
  private KurentoClient kurento;

  @Autowired
  private UserRegistry registry;

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
    UserSession user = registry.getBySession(session);

    if (user != null) {
      log.debug("Incoming message from user '{}': {}", user.getName(), jsonMessage);
    } else {
      log.debug("Incoming message from new user: {}", jsonMessage);
    }

    switch (jsonMessage.get("id").getAsString()) {
      case "register":
        register(session, jsonMessage);
        break;
      case "call":
        call(user, jsonMessage);
        break;
      case "incomingCallResponse":
        incomingCallResponse(user, jsonMessage);
        break;
      case "play":
        play(user, jsonMessage);
        break;
      case "onIceCandidate": {
        JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();

        if (user != null) {
          IceCandidate cand =
              new IceCandidate(candidate.get("candidate").getAsString(), candidate.get("sdpMid")
                  .getAsString(), candidate.get("sdpMLineIndex").getAsInt());
          user.addCandidate(cand);
        }
        break;
      }
      case "stop":
        stop(session);
        releasePipeline(user);
        break;
      case "stopPlay":
        releasePipeline(user);
        break;
      default:
        break;
    }
  }

  private void register(WebSocketSession session, JsonObject jsonMessage) throws IOException {
    String name = jsonMessage.getAsJsonPrimitive("name").getAsString();

    UserSession caller = new UserSession(session, name);
    String responseMsg = "accepted";
    if (name.isEmpty()) {
      responseMsg = "rejected: empty user name";
    } else if (registry.exists(name)) {
      responseMsg = "rejected: user '" + name + "' already registered";
    } else {
      registry.register(caller);
    }

    JsonObject response = new JsonObject();
    response.addProperty("id", "resgisterResponse");
    response.addProperty("response", responseMsg);
    caller.sendMessage(response);
  }

  /**
   *
   * @param caller
   * @param jsonMessage
   * @throws IOException
   */
  private void call(UserSession caller, JsonObject jsonMessage) throws IOException {
    String to = jsonMessage.get("to").getAsString();
    String from = jsonMessage.get("from").getAsString();
    JsonObject response = new JsonObject();

    if (registry.exists(to)) {//当有被呼叫者时，被呼叫者接受的响应为incomingCall
      caller.setSdpOffer(jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString());
      caller.setCallingTo(to);

      response.addProperty("id", "incomingCall");
      response.addProperty("from", from);

      UserSession callee = registry.getByName(to);
      callee.sendMessage(response);
      callee.setCallingFrom(from);
    } else {//当无被呼叫者时，主呼叫者的响应为callResponse，拒绝
      response.addProperty("id", "callResponse");
      response.addProperty("response", "rejected");
      response.addProperty("message", "user '" + to + "' is not registered");

      caller.sendMessage(response);
    }
  }

  /**
   * 当被呼叫者，确认接收呼叫时
   * @param callee
   * @param jsonMessage
   * @throws IOException
   */
  private void incomingCallResponse(final UserSession callee, JsonObject jsonMessage)
      throws IOException {
    String callResponse = jsonMessage.get("callResponse").getAsString();
    String from = jsonMessage.get("from").getAsString();
    final UserSession calleer = registry.getByName(from);
    String to = calleer.getCallingTo();

    if ("accept".equals(callResponse)) {
      log.debug("Accepted call from '{}' to '{}'", from, to);
      //呼叫媒体管道，建立主呼叫与被呼叫者的关系
      CallMediaPipeline callMediaPipeline = new CallMediaPipeline(kurento, from, to);
      pipelines.put(calleer.getSessionId(), callMediaPipeline.getPipeline());
      pipelines.put(callee.getSessionId(), callMediaPipeline.getPipeline());
      //被呼叫者设置端点为被呼叫WebRctEndpoint
      callee.setWebRtcEndpoint(callMediaPipeline.getCalleeWebRtcEp());
      //被呼叫者发现ICE候选者监听
      callMediaPipeline.getCalleeWebRtcEp().addIceCandidateFoundListener(
          new EventListener<IceCandidateFoundEvent>() {

            @Override
            public void onEvent(IceCandidateFoundEvent event) {
              JsonObject response = new JsonObject();
              response.addProperty("id", "iceCandidate");
              response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
              try {
                //被呼叫者响应浏览器客户端的iceCandidate事件
                synchronized (callee.getSession()) {
                  callee.getSession().sendMessage(new TextMessage(response.toString()));
                }
              } catch (IOException e) {
                log.debug(e.getMessage());
              }
            }
          });

      //被呼叫
      String calleeSdpOffer = jsonMessage.get("sdpOffer").getAsString();
      String calleeSdpAnswer = callMediaPipeline.generateSdpAnswerForCallee(calleeSdpOffer);
      JsonObject startCommunication = new JsonObject();
      startCommunication.addProperty("id", "startCommunication");
      startCommunication.addProperty("sdpAnswer", calleeSdpAnswer);
      //被呼叫者SDP响应
      synchronized (callee) {
        callee.sendMessage(startCommunication);
      }
      //被呼叫者候选者收集
      callMediaPipeline.getCalleeWebRtcEp().gatherCandidates();

      //呼叫者的SDP请求
      String callerSdpOffer = registry.getByName(from).getSdpOffer();

      //主呼叫者设置WebRTCEndpoint
      calleer.setWebRtcEndpoint(callMediaPipeline.getCallerWebRtcEp());
      callMediaPipeline.getCallerWebRtcEp().addIceCandidateFoundListener(
          new EventListener<IceCandidateFoundEvent>() {

            @Override
            public void onEvent(IceCandidateFoundEvent event) {
              JsonObject response = new JsonObject();
              response.addProperty("id", "iceCandidate");
              response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
              try {
                //主呼叫者响应浏览器客户端iceCandidate
                synchronized (calleer.getSession()) {
                  calleer.getSession().sendMessage(new TextMessage(response.toString()));
                }
              } catch (IOException e) {
                log.debug(e.getMessage());
              }
            }
          });

      //主呼叫者响应客户端callResponse
      String callerSdpAnswer = callMediaPipeline.generateSdpAnswerForCaller(callerSdpOffer);

      JsonObject response = new JsonObject();
      response.addProperty("id", "callResponse");
      response.addProperty("response", "accepted");
      response.addProperty("sdpAnswer", callerSdpAnswer);

      synchronized (calleer) {
        calleer.sendMessage(response);
      }

      //主呼叫者收集ICE候选者
      callMediaPipeline.getCallerWebRtcEp().gatherCandidates();

      //媒体视频记录
      callMediaPipeline.record();

    } else {
      JsonObject response = new JsonObject();
      response.addProperty("id", "callResponse");
      response.addProperty("response", "rejected");
      calleer.sendMessage(response);
    }
  }

  public void stop(WebSocketSession session) throws IOException {
    // Both users can stop the communication. A 'stopCommunication'
    // message will be sent to the other peer.
    UserSession stopperUser = registry.getBySession(session);
    if (stopperUser != null) {
      UserSession stoppedUser =
          (stopperUser.getCallingFrom() != null) ? registry.getByName(stopperUser.getCallingFrom())
              : stopperUser.getCallingTo() != null ? registry.getByName(stopperUser.getCallingTo())
                  : null;

              if (stoppedUser != null) {
                JsonObject message = new JsonObject();
                message.addProperty("id", "stopCommunication");
                stoppedUser.sendMessage(message);
                stoppedUser.clear();
              }
              stopperUser.clear();
    }
  }

  public void releasePipeline(UserSession session) {
    String sessionId = session.getSessionId();
    // set to null the endpoint of the other user

    if (pipelines.containsKey(sessionId)) {
      pipelines.get(sessionId).release();
      pipelines.remove(sessionId);
    }
    session.setWebRtcEndpoint(null);
    session.setPlayingWebRtcEndpoint(null);

    UserSession stoppedUser =
        (session.getCallingFrom() != null) ? registry.getByName(session.getCallingFrom())
            : registry.getByName(session.getCallingTo());
        stoppedUser.setWebRtcEndpoint(null);
        stoppedUser.setPlayingWebRtcEndpoint(null);
  }

  /**
   * 播放视频
   * @param session
   * @param jsonMessage
   * @throws IOException
   */
  private void play(final UserSession session, JsonObject jsonMessage) throws IOException {
    String user = jsonMessage.get("user").getAsString();
    log.debug("Playing recorded call of user '{}'", user);

    JsonObject response = new JsonObject();
    response.addProperty("id", "playResponse");
    //
    if (registry.getByName(user) != null && registry.getBySession(session.getSession()) != null) {
      final PlayMediaPipeline playMediaPipeline =
          new PlayMediaPipeline(kurento, user, session.getSession());
      //会话中的媒体播放端点设置为媒体管道端点
      session.setPlayingWebRtcEndpoint(playMediaPipeline.getWebRtc());
      //媒体播放中的播放器监听媒体流端事件
      playMediaPipeline.getPlayer().addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
        @Override
        public void onEvent(EndOfStreamEvent event) {
          UserSession user = registry.getBySession(session.getSession());
          releasePipeline(user);
          //发送浏览器客户端设置媒体流端事件消息
          playMediaPipeline.sendPlayEnd(session.getSession());
        }
      });

      // 播放媒体管理监听ICE候选者事件
      playMediaPipeline.getWebRtc().addIceCandidateFoundListener(
          new EventListener<IceCandidateFoundEvent>() {

            @Override
            public void onEvent(IceCandidateFoundEvent event) {
              JsonObject response = new JsonObject();
              response.addProperty("id", "iceCandidate");
              response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
              try {
                //
                synchronized (session) {
                  session.getSession().sendMessage(new TextMessage(response.toString()));
                }
              } catch (IOException e) {
                log.debug(e.getMessage());
              }
            }
          });

      //响应浏览器客户端sdpAnswer
      String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
      String sdpAnswer = playMediaPipeline.generateSdpAnswer(sdpOffer);

      response.addProperty("response", "accepted");

      response.addProperty("sdpAnswer", sdpAnswer);

      playMediaPipeline.play();
      pipelines.put(session.getSessionId(), playMediaPipeline.getPipeline());
      synchronized (session.getSession()) {
        session.sendMessage(response);
      }

      //播放媒体端收集ICE候选者
      playMediaPipeline.getWebRtc().gatherCandidates();

    } else {
      response.addProperty("response", "rejected");
      response.addProperty("error", "No recording for user '" + user
          + "'. Please type a correct user in the 'Peer' field.");
      session.getSession().sendMessage(new TextMessage(response.toString()));
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    stop(session);
    registry.removeBySession(session);
  }

}
