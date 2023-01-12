//
//  ShowLivePreViewVC.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/2.
//

import UIKit
import AgoraRtcKit

class ShowCreateLiveVC: UIViewController {

    private var createView: ShowCreateLiveView!
    private var localView: UIView!
    
    private var selectedResolution = 1
    
//    let transDelegate = ShowPresentTransitioningDelegate()
    
    private let liveVC = ShowLiveViewController()
    
    lazy var agoraKitManager: ShowAgoraKitManager = {
//        let manager = ShowAgoraKitManager()
//        manager.defaultSetting()
//        return manager
        return liveVC.agoraKitManager
    }()
        
    private lazy var beautyVC = ShowBeautySettingVC()
    
    deinit {
        print("deinit-- ShowCreateLiveVC")
    }
    
    override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
        super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
        print("init-- ShowCreateLiveVC")
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setUpUI()
//        agoraKitManager.defaultSetting()
        agoraKitManager.startPreview(canvasView: localView)
        configNaviBar()
        showPreset()
    }
    
    func configNaviBar() {
        self.navigationController?.isNavigationBarHidden = true
        
        let titleLabel = UILabel()
        view.addSubview(titleLabel)
        titleLabel.text = "navi_title_show_live".show_localized
        titleLabel.textColor = .show_main_text
        titleLabel.font = .show_navi_title
        titleLabel.snp.makeConstraints { make in
            make.top.equalTo(56)
            make.centerX.equalToSuperview()
        }
        
        let cancelButton = UIButton(type: .custom)
        cancelButton.setImage(UIImage.show_sceneImage(name: "show_preview_cancel"), for: .normal)
        cancelButton.addTarget(self, action: #selector(didClickCancelButton), for: .touchUpInside)
        view.addSubview(cancelButton)
        cancelButton.snp.makeConstraints { make in
            make.left.equalTo(15)
            make.centerY.equalTo(titleLabel)
        }
    }
    
    private func setUpUI() {
        
        // 画布
        localView = UIView()
        view.addSubview(localView)
        localView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        
        // 创建房间
        createView = ShowCreateLiveView()
        createView.delegate = self
        view.addSubview(createView)
        createView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        
//        beautyVC.transitioningDelegate = transDelegate
        beautyVC.dismissed = { [weak self] in
            self?.createView.hideBottomViews = false
        }
        
        // 创建默认美颜效果
        ShowBeautyFaceVC.beautyData.forEach({
            ByteBeautyManager.shareManager.setBeauty(path: $0.path,
                                                     key: $0.key,
                                                     value: $0.value)
        })
    }
    
    private func showPreset() {
        let vc = ShowPresettingVC()
        vc.didSelectedPresetType = {[weak self] type, modeName in
            self?.agoraKitManager.updatePresetForType(type, mode: .signle)
            let text1 = "show_presetting_update_toast1".show_localized
            let text2 = "show_presetting_update_toast2".show_localized
            ToastView.show(text: "\(text1)\"\(modeName)\"\(text2)")
        }
        present(vc, animated: true)
    }
    
    @objc private func didClickCancelButton(){
        ByteBeautyManager.shareManager.destroy()
        dismiss(animated: true)
    }
}

extension ShowCreateLiveVC: ShowCreateLiveViewDelegate {
    
    func onClickSettingBtnAction() {
//        let vc = ShowAdvancedSettingVC()
//        vc.mode = .signle
//        vc.isBroadcaster = true
//        vc.isOutside = true
//        vc.settingManager = agoraKitManager
//        self.navigationController?.pushViewController(vc, animated: true)
        showPreset()
    }
    
    func onClickCameraBtnAction() {
//        agoraKit?.switchCamera()
        agoraKitManager.switchCamera()
    }
    
    func onClickBeautyBtnAction() {
        createView.hideBottomViews = true
        present(beautyVC, animated: true)
    }
    
    func onClickQualityBtnAction() {
        createView.hideBottomViews = true
        let vc = ShowSelectQualityVC()
        vc.defalutSelectIndex = selectedResolution
        present(vc, animated: true)
        vc.dismissed = { [weak self] in
            self?.createView.hideBottomViews = false
        }
        vc.selectedItem = {[weak self] resolution,index in
            guard let wSelf = self else { return }
            wSelf.selectedResolution = index
            wSelf.agoraKitManager.setCaptureVideoDimensions(CGSize(width: resolution.width, height: resolution.height))
        }
    }
    
    func onClickStartBtnAction() {
        guard let roomName = createView.roomName, roomName.count > 0 else {
            ToastView.show(text: "create_room_name_can_not_empty".show_localized)
            return
        }
        
        guard  let roomName = createView.roomName, roomName.count <= 16 else {
            ToastView.show(text: "create_room_name_too_long".show_localized)
            return
        }
        
        AppContext.showServiceImp.createRoom(roomName: roomName,
                                             roomId: createView.roomNo,
                                             thumbnailId: createView.roomBg) { [weak self] err, detailModel in
//            liveVC.agoraKit = self?.agoraKitManager.agoraKit
            guard let wSelf = self else { return }
//            let liveVC = ShowLiveViewController()
            wSelf.liveVC.room = detailModel
            wSelf.liveVC.selectedResolution = wSelf.selectedResolution
//            liveVC.agoraKitManager = wSelf.agoraKitManager
            
            wSelf.navigationController?.pushViewController(wSelf.liveVC, animated: false)
        }
    }
}