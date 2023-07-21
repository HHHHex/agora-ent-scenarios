//
//  Pure1v1UserListViewController.swift
//  AFNetworking
//
//  Created by wushengtao on 2023/7/20.
//

import UIKit
import YYCategories
import CallAPI
import AgoraRtcKit

class Pure1v1UserListViewController: UIViewController {
    var appId: String = ""
    var appCertificate: String = ""
    var userInfo: Pure1v1UserInfo?
    
    private var callState: CallStateType = .idle
    private var connectedUserId: UInt?
    private lazy var callVC = Pure1v1CallViewController()
    private lazy var callAPI = CallApiImpl()
    private lazy var naviBar: Pure1v1NaviBar = Pure1v1NaviBar(frame: CGRect(x: 0, y: UIDevice.current.aui_SafeDistanceTop, width: self.view.aui_width, height: 44))
    private lazy var service: Pure1v1ServiceProtocol = Pure1v1ServiceImp(appId: appId, user: userInfo)
    private lazy var noDataView: Pure1v1UserNoDataView = Pure1v1UserNoDataView(frame: self.view.bounds)
    private lazy var listView: Pure1v1UserPagingListView = {
        let listView = Pure1v1UserPagingListView(frame: self.view.bounds)
        listView.callClosure = { [weak self] user in
            guard let user = user else {return}
            self?._call(user: user)
        }
        return listView
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(noDataView)
        view.addSubview(listView)
        view.addSubview(naviBar)
        naviBar.backButton.addTarget(self, action: #selector(_backAction), for: .touchUpInside)
        naviBar.refreshButton.addTarget(self, action: #selector(_refreshAction), for: .touchUpInside)
        naviBar.refreshButton.isHidden = true
        service.joinRoom {[weak self] error in
            self?.naviBar.refreshButton.isHidden = false
            self?._refreshAction()
        }
        
        guard let userInfo = userInfo else {
            assert(false, "userInfo == nil")
            return
        }
        
        let tokenConfig: CallTokenConfig = CallTokenConfig()
        tokenConfig.roomId = "\(userInfo.userId)"
        NetworkManager.shared.generateTokens(appId: appId,
                                             appCertificate: appCertificate,
                                             channelName: tokenConfig.roomId,
                                             uid: userInfo.userId,
                                             tokenGeneratorType: .token007,
                                             tokenTypes: [.rtc, .rtm]) {[weak self] tokens in
            tokenConfig.rtcToken = tokens[AgoraTokenType.rtc.rawValue]!
            tokenConfig.rtmToken = tokens[AgoraTokenType.rtm.rawValue]!
            
            self?._initCallAPI(tokenConfig: tokenConfig)
        }
    }
    
    private func _initCallAPI(tokenConfig: CallTokenConfig) {
        let config = CallConfig()
        config.role = .caller  // Pure 1v1 can only be set as the caller
        config.mode = .pure1v1
        config.appId = appId
        config.userId = UInt(userInfo?.userId ?? "")!
        config.autoAccept = false
        config.rtcEngine = _createRtcEngine()
        config.localView = callVC.smallCanvasView
        config.remoteView = callVC.bigCanvasView
        
        callAPI.initialize(config: config, token: tokenConfig) { error in
            // Requires active call to prepareForCall
            let prepareConfig = PrepareConfig.callerConfig()
            prepareConfig.autoLoginRTM = true
            prepareConfig.autoSubscribeRTM = true
            self.callAPI.prepareForCall(prepareConfig: prepareConfig) { err in
            }
        }
        callAPI.addListener(listener: self)
    }
    
    private func _createRtcEngine() ->AgoraRtcEngineKit {
        let config = AgoraRtcEngineConfig()
        config.appId = appId
        config.channelProfile = .liveBroadcasting
        config.audioScenario = .gameStreaming
        config.areaCode = .global
        let engine = AgoraRtcEngineKit.sharedEngine(with: config,
                                                    delegate: nil)
        
        engine.setClientRole(.broadcaster)
        return engine
    }
    
    private func _call(user: Pure1v1UserInfo) {
        callAPI.call(roomId: user.userId, remoteUserId: UInt(user.userId)!) { err in
            
        }
    }
}

extension Pure1v1UserListViewController {
    @objc func _backAction() {
        callAPI.deinitialize {
        }
        service.leaveRoom { err in
        }
        self.navigationController?.popViewController(animated: true)
    }
    
    @objc func _refreshAction() {
        service.getUserList {[weak self] list in
            self?.listView.userList = list.filter({$0.userId != self?.userInfo?.userId})
        }
    }
}

extension Pure1v1UserListViewController: CallApiListenerProtocol {
    func onCallStateChanged(with state: CallStateType,
                            stateReason: CallReason,
                            eventReason: String,
                            elapsed: Int,
                            eventInfo: [String : Any]) {
        let currentUid = userInfo?.userId ?? ""
        let publisher = eventInfo[kPublisher] as? String ?? currentUid
        guard publisher == currentUid else {
            return
        }
        print("onCallStateChanged state: \(state.rawValue), stateReason: \(stateReason.rawValue), eventReason: \(eventReason), elapsed: \(elapsed) ms, eventInfo: \(eventInfo) publisher: \(publisher) / \(currentUid)")
        
        self.callState = state
        
        switch state {
        case .calling:
            let fromUserId = eventInfo[kFromUserId] as? UInt ?? 0
            let fromRoomId = eventInfo[kFromRoomId] as? String ?? ""
            let toUserId = eventInfo[kRemoteUserId] as? UInt ?? 0
            if let connectedUserId = connectedUserId, connectedUserId != fromUserId {
                callAPI.reject(roomId: fromRoomId, remoteUserId: fromUserId, reason: "already calling") { err in
                }
                return
            }
            // 触发状态的用户是自己才处理
            if currentUid == "\(toUserId)" {
                connectedUserId = fromUserId
                
//                AUIAlertView()
//                    .isShowCloseButton(isShow: true)
//                    .title(title: "用户 \(fromUserId) 邀请您1对1通话")
//                    .rightButton(title: "同意")
//                    .leftButton(title: "拒绝")
//                    .leftButtonTapClosure {[weak self] in
//                        guard let self = self else { return }
//                        self.api.reject(roomId: fromRoomId, remoteUserId: fromUserId, reason: "reject by user") { err in
//                        }
//                    }
//                    .rightButtonTapClosure(onTap: {[weak self] text in
//                        guard let self = self else { return }
//                        NetworkManager.shared.generateTokens(channelName: fromRoomId,
//                                                             uid: "\(toUserId)",
//                                                             tokenGeneratorType: .token007,
//                                                             tokenTypes: [.rtc, .rtm]) {[weak self] tokens in
//                            guard let self = self else {return}
//                            guard tokens.count == 2 else {
//                                print("generateTokens fail")
//                                self.view.isUserInteractionEnabled = true
//                                return
//                            }
//                            let rtcToken = tokens[AgoraTokenType.rtc.rawValue]!
//                            self.api.accept(roomId: fromRoomId, remoteUserId: fromUserId, rtcToken: rtcToken) { err in
//                            }
//                        }
//                    })
//                    .show()
                
                if let user = listView.userList.first {$0.userId == "\(fromUserId)"} {
                    let dialog = Pure1v1CalleeDialog.show(user: user)
                }
                
            } else if currentUid == "\(fromUserId)" {
                connectedUserId = toUserId
                
                if let user = listView.userList.first {$0.userId == "\(toUserId)"} {
                    let dialog = Pure1v1CallerDialog.show(user: user)
                    dialog?.cancelClosure = {[weak self] in
                        self?.callAPI.cancelCall(completion: { err in
                        })
                    }
                }
//                AUIAlertView()
//                    .isShowCloseButton(isShow: true)
//                    .title(title: "呼叫用户 \(toUserId) 中")
//                    .rightButton(title: "取消")
//                    .rightButtonTapClosure(onTap: {[weak self] text in
//                        guard let self = self else { return }
//                        self.api.cancelCall { err in
//                        }
//                    })
//                    .show()
            }
            break
        case .connected:
//            AUIToast.show(text: "通话开始\(eventInfo[kDebugInfo] as? String ?? "")", postion: .bottom)
//            AUIAlertManager.hiddenView()
            Pure1v1CallerDialog.hidden()
            break
        case .prepared:
//            switch stateReason {
//            case .localHangup, .remoteHangup:
//                AUIToast.show(text: "通话结束", postion: .bottom)
//            case .localRejected, .remoteRejected:
//                AUIToast.show(text: "通话被拒绝")
//            case .callingTimeout:
//                AUIToast.show(text: "无应答")
//            case .localCancel, .remoteCancel:
//                AUIToast.show(text: "通话被取消")
//            default:
//                break
//            }
//            AUIAlertManager.hiddenView()
            connectedUserId = nil
            Pure1v1CallerDialog.hidden()
            break
        case .failed:
//            AUIToast.show(text: eventReason, postion: .bottom)
//            AUIAlertManager.hiddenView()
            connectedUserId = nil
            Pure1v1CallerDialog.hidden()
            break
        default:
            break
        }
    }
}