//
//  VRSoundEffectsViewController.swift
//  VoiceRoomBaseUIKit
//
//  Created by 朱继超 on 2022/8/26.
//

import SVProgressHUD
import UIKit
import ZSwiftBaseLib
import AgoraChat

public class VRSoundEffectsViewController: VRBaseViewController {
    var code = ""

    var name = ""

    var type = 0

    lazy var background: UIImageView = .init(frame: self.view.frame).image(UIImage("roomList")!)

    lazy var effects: VRSoundEffectsList = .init(frame: CGRect(x: 0, y: ZNavgationHeight, width: ScreenWidth, height: ScreenHeight - CGFloat(ZBottombarHeight) - CGFloat(ZTabbarHeight)), style: .plain).separatorStyle(.none).tableFooterView(UIView(frame: CGRect(x: 0, y: 0, width: ScreenWidth, height: 120))).backgroundColor(.clear)

    lazy var done: UIImageView = .init(frame: CGRect(x: 0, y: ScreenHeight - CGFloat(ZBottombarHeight) - 70, width: ScreenWidth, height: 92)).image(UIImage("blur")!).isUserInteractionEnabled(true)

    lazy var createContainer: UIView = .init(frame: CGRect(x: 30, y: 15, width: ScreenWidth - 60, height: 50)).backgroundColor(.white)

    lazy var toLive: UIButton = {
        UIButton(type: .custom).frame(CGRect(x: 30, y: 15, width: ScreenWidth - 60, height: 50)).title(LanguageManager.localValue(key: "Go Live"), .normal).font(.systemFont(ofSize: 16, weight: .semibold)).setGradient([UIColor(red: 0.13, green: 0.608, blue: 1, alpha: 1), UIColor(red: 0.204, green: 0.366, blue: 1, alpha: 1)], [CGPoint(x: 0, y: 0), CGPoint(x: 0, y: 1)]).cornerRadius(25).addTargetFor(self, action: #selector(VRSoundEffectsViewController.goLive), for: .touchUpInside)
    }()
    
    override public func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        view.addSubViews([background, effects, done])
        done.addSubViews([createContainer, toLive])
        view.bringSubviewToFront(navigation)
        navigation.title.text = LanguageManager.localValue(key: "Sound Selection")
        createContainer.layer.cornerRadius = 25
        createContainer.layer.shadowRadius = 8
        createContainer.layer.shadowOffset = CGSize(width: 0, height: 4)
        createContainer.layer.shadowColor = UIColor(red: 0, green: 0.55, blue: 0.98, alpha: 0.2).cgColor
        createContainer.layer.shadowOpacity = 1
    }
    
    deinit {
        SVProgressHUD.dismiss()
    }

    @objc func goLive() {
        if name.isEmpty {
            view.makeToast("No Room Name".localized(), point: view.center, title: nil, image: nil, completion: nil)
        }
        Throttler.throttle {
            self.entryRoom()
        }
    }

    private func createRoom() {
        VoiceRoomBusinessRequest.shared.sendPOSTRequest(api: .createRoom(()), params: ["name": name, "is_private":!code.isEmpty, "password": code, "type": type, "sound_effect": effects.type, "allow_free_join_mic": false], classType: VRRoomInfo.self) { info, error in
            if error == nil, info != nil {
                self.view.makeToast("Room Created".localized(), point: self.view.center, title: nil, image: nil, completion: nil)
                let vc = VoiceRoomViewController(info: info!)
                self.navigationController?.pushViewController(vc, animated: true)
            } else {
                self.view.makeToast("Create failed!".localized(), point: self.view.center, title: nil, image: nil, completion: nil)
            }
        }
    }

    @objc private func entryRoom() {
        AgoraChatClient.shared().logout(false)
        SVProgressHUD.show(withStatus: "Loading".localized())
        self.toLive.isUserInteractionEnabled = false
        let imId: String? = VLUserCenter.user.chat_uid.count > 0 ? VLUserCenter.user.chat_uid : nil
        let entity = self.createEntity()
        ChatRoomServiceImp.getSharedInstance().initIM(with: entity.name ?? "", chatId: nil, channelId: entity.channel_id ?? "",  imUid: imId, pwd: "12345678") { im_token, uid, room_id in
            entity.chatroom_id = room_id
            entity.owner = VoiceRoomUserInfo.shared.user
            entity.owner?.chat_uid = uid
            VLUserCenter.user.im_token = im_token
            VLUserCenter.user.chat_uid = uid
            SVProgressHUD.dismiss()
            if im_token.isEmpty || uid.isEmpty {
                SVProgressHUD.showError(withStatus: "Fetch IMConfig failed!")
            }
            SVProgressHUD.show(withStatus: "Loading".localized())
            VoiceRoomIMManager.shared?.loginIM(userName: uid , token: im_token , completion: { userName, error in
                SVProgressHUD.dismiss()
                if error == nil {
                    ChatRoomServiceImp.getSharedInstance().createRoom(room: entity) { error, room in
                        SVProgressHUD.dismiss()
                        if let room = room {
                            SVProgressHUD.showSuccess(withStatus: "Room Created".localized())
                            self.entryRoom(room: room)
                            self.toLive.isUserInteractionEnabled = true
                        } else {
                            self.toLive.isUserInteractionEnabled = true
                            SVProgressHUD.showError(withStatus: "Create failed!".localized())
                        }
                    }
                } else {
                    SVProgressHUD.showError(withStatus: "LoginIM failed!".localized())
                    self.toLive.isUserInteractionEnabled = true
                }
            })
            
        }
        
    }
    
    private func createEntity() -> VRRoomEntity {
        let entity: VRRoomEntity = VRRoomEntity()
        entity.sound_effect = effects.type
        entity.is_private = !code.isEmpty
        entity.name = name
        entity.roomPassword = code
        entity.rtc_uid = Int(VLUserCenter.user.id)
        let timeInterval: TimeInterval = Date().timeIntervalSince1970
        let millisecond = CLongLong(round(timeInterval*1000))
        entity.room_id = String(millisecond)
        entity.channel_id = String(millisecond)
        entity.created_at = UInt(millisecond)
        entity.click_count = 3
        entity.member_count = 3
        return entity
    }
    
    private func entryRoom(room: VRRoomEntity) {
        let info: VRRoomInfo = VRRoomInfo()
        info.room = room
        info.mic_info = nil
        let vc = VoiceRoomViewController(info: info)
        self.navigationController?.pushViewController(vc, animated: true)
    }
}
