//
//  Pure1v1Model.swift
//  Pure1v1
//
//  Created by wushengtao on 2023/7/20.
//

import Foundation
import RTMSyncManager

@objcMembers
public class Pure1v1UserInfo: NSObject {
    public var userId: String = "" 
    public var userName: String = ""
    public var avatar: String = ""
    
    convenience init(userInfo: AUIUserInfo) {
        self.init()
        self.userId = userInfo.userId
        self.userName = userInfo.userName
        self.avatar = userInfo.userAvatar
    }
    
    func getRoomId() ->String {
        return "\(userId))"
    }
    
    func bgImage() ->UIImage? {
        let uid = UInt(userId) ?? 0
        let image = UIImage.scene1v1Image(name: "user_bg\(uid % 9 + 1)")
        return image
    }
}

