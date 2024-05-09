package io.agora.scene.showTo1v1.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.agora.rtm.*
import io.agora.rtmsyncmanager.ISceneResponse
import io.agora.rtmsyncmanager.SyncManager
import io.agora.rtmsyncmanager.model.*
import io.agora.rtmsyncmanager.service.IAUIUserService
import io.agora.rtmsyncmanager.service.http.HttpManager
import io.agora.rtmsyncmanager.service.room.AUIRoomManager
import io.agora.rtmsyncmanager.utils.AUILogger
import io.agora.scene.base.BuildConfig
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.TimeUtils
import io.agora.scene.showTo1v1.ShowTo1v1Logger
import kotlin.random.Random

/*
 * service 模块
 * 简介：这个模块的作用是负责前端业务模块和业务服务器的交互(包括房间列表+房间内的业务数据同步等)
 * 实现原理：该场景的业务服务器是包装了一个 rethinkDB 的后端服务，用于数据存储，可以认为它是一个 app 端上可以自由写入的 DB，房间列表数据、房间内的业务数据等在 app 上构造数据结构并存储在这个 DB 里
 * 当 DB 内的数据发生增删改时，会通知各端，以此达到业务数据同步的效果
 * TODO 注意⚠️：该场景的后端服务仅做场景演示使用，无法商用，如果需要上线，您必须自己部署后端服务或者云存储服务器（例如leancloud、环信等）并且重新实现这个模块！！！！！！！！！！！
 */
class ShowTo1v1ServiceImpl constructor(
    context: Context,
    private val rtmClient: RtmClient,
    private val user: ShowTo1v1UserInfo,
) : ShowTo1v1ServiceProtocol, ISceneResponse, IAUIUserService.AUIUserRespObserver {

    companion object {
        private const val TAG = "Show1v1_LOG"
    }

    private val kSceneId = "scene_Livetoprivate_421"
    @Volatile
    private var syncUtilsInited = false

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private val syncManager: SyncManager

    private val roomManager = AUIRoomManager()

    private var roomList = emptyList<AUIRoomInfo>()

    private var userList: List<AUIUserInfo> = emptyList()

    private var listener: ShowTo1v1ServiceListenerProtocol? = null

    init {
        HttpManager.setBaseURL(BuildConfig.ROOM_MANAGER_SERVER_HOST)
        AUILogger.initLogger(AUILogger.Config(context, "showTo1v1"))

        val commonConfig = AUICommonConfig()
        commonConfig.context = context
        commonConfig.appId = BuildConfig.AGORA_APP_ID
        val owner = AUIUserThumbnailInfo()
        owner.userId = UserManager.getInstance().user.id.toString()
        owner.userName = UserManager.getInstance().user.name
        owner.userAvatar = UserManager.getInstance().user.headUrl
        commonConfig.owner = owner
        commonConfig.host = BuildConfig.TOOLBOX_SERVER_HOST
        AUIRoomContext.shared().setCommonConfig(commonConfig)
        syncManager = SyncManager(context, rtmClient, commonConfig)
    }

    override fun reset() {
        if (syncUtilsInited) {
            syncUtilsInited = false
        }
    }

    override fun createRoom(
        roomName: String,
        completion: (error: Exception?, roomInfo: ShowTo1v1RoomInfo?) -> Unit
    ) {
        ShowTo1v1Logger.d(TAG, "createRoom start, roomName:$roomName")
        val roomInfo = AUIRoomInfo()
        roomInfo.roomId = (Random(System.currentTimeMillis()).nextInt(100000) + 1000000).toString()
        roomInfo.roomName = roomName
        val owner = AUIUserThumbnailInfo()
        owner.userId = UserManager.getInstance().user.id.toString()
        owner.userName = UserManager.getInstance().user.name
        owner.userAvatar = UserManager.getInstance().user.headUrl
        roomInfo.owner = owner
        roomInfo.thumbnail = UserManager.getInstance().user.headUrl
        roomInfo.createTime = TimeUtils.currentTimeMillis()
        roomManager.createRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomInfo) { e, info ->
            if (info != null) {
                val temp = mutableListOf<AUIRoomInfo>()
                temp.add(roomInfo)
                temp.addAll(roomList)
                roomList = temp

                val scene = syncManager.getScene(roomInfo.roomId)
                scene.create(null) { er ->
                    if (er != null) {
                        ShowTo1v1Logger.e(TAG, er,"createRoom-->create scene fail，roomId:${roomInfo.roomId}")
                        completion.invoke(Exception(er.message), null)
                        return@create
                    }else{
                        ShowTo1v1Logger.d(TAG,"createRoom-->create scene success，roomId:${roomInfo.roomId}")
                    }
                }

                completion.invoke(null, ShowTo1v1RoomInfo(
                    roomId = roomInfo.roomId,
                    roomName = roomInfo.roomName,
                    userId = owner.userId,
                    userName = owner.userName,
                    avatar = owner.userAvatar,
                    createdAt = roomInfo.createTime
                ))
            }
            if (e != null) {
                ShowTo1v1Logger.e(TAG, e,"createRoom failed，roomId:${roomInfo.roomId}")
                completion.invoke(Exception(e.message), null)
            }
        }
    }

    override fun joinRoom(roomInfo: ShowTo1v1RoomInfo, completion: (error: Exception?) -> Unit) {
        ShowTo1v1Logger.d(TAG, "joinRoom start，roomId:${roomInfo.roomId}")
        val scene = syncManager.getScene(roomInfo.roomId)
        scene.bindRespDelegate(this)
        scene.enter { _, e ->
            if (e != null) {
                ShowTo1v1Logger.e(TAG, e,"joinRoom failed，roomId:${roomInfo.roomId}")
                completion.invoke(Exception(e.message))
            } else {
                ShowTo1v1Logger.d(TAG, "joinRoom end，roomId:${roomInfo.roomId}")
                completion.invoke(null)
            }
        }
        scene.userService.registerRespObserver(this)
    }

    override fun leaveRoom(roomInfo: ShowTo1v1RoomInfo, completion: (error: Exception?) -> Unit) {
        ShowTo1v1Logger.d(TAG, "leaveRoom start ${roomInfo.roomId}")
        val scene = syncManager.getScene(roomInfo.roomId)
        scene.unbindRespDelegate(this)
        scene.leave()
        if (roomInfo.userId == UserManager.getInstance().user.id.toString()) {
            roomManager.destroyRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomInfo.roomId) { e ->
                if (e!=null){
                    ShowTo1v1Logger.e(TAG,e, "leaveRoom-->destroyRoom ${roomInfo.roomId} failed")
                    runOnMainThread {
                        completion.invoke(Exception(e.message))
                    }
                }else{
                    ShowTo1v1Logger.d(TAG, "broadcast leaveRoom end ${roomInfo.roomId}")
                    runOnMainThread {
                        completion.invoke(null)
                    }
                }
            }
            scene.delete()
        } else {
            ShowTo1v1Logger.d(TAG, "audience leaveRoom end ${roomInfo.roomId}")
            runOnMainThread {
                completion.invoke(null)
            }
        }
    }

    /*
     * 拉取房间列表
     */
    override fun getRoomList(completion: (error: Exception?, roomList: List<ShowTo1v1RoomInfo>) -> Unit) {
        ShowTo1v1Logger.d(TAG, "getRoomList start")
        roomManager.getRoomInfoList(BuildConfig.AGORA_APP_ID, kSceneId, System.currentTimeMillis(), 20) { error, list, ts ->
            if (error != null) {
                ShowTo1v1Logger.e(TAG, error, "getRoomList failed")
                runOnMainThread { completion.invoke(error, ArrayList<ShowTo1v1RoomInfo>().toList()) }
            }
            if (list != null && ts != null) {
                val ret = ArrayList<ShowTo1v1RoomInfo>()
                list.forEach {
                    val aliveTime = ts - it.createTime
                    ShowTo1v1Logger.d(TAG, "room alive time: $aliveTime, roomId: ${it.roomId}")
                    if (aliveTime < ROOM_AVAILABLE_DURATION && it.owner!!.userId.toLong() != UserManager.getInstance().user.id) {
                        ret.add(ShowTo1v1RoomInfo(
                            roomId = it.roomId,
                            roomName = it.roomName,
                            userId = it.owner!!.userId,
                            userName = it.owner!!.userName,
                            avatar = it.owner!!.userAvatar,
                            createdAt = it.createTime
                        ))
                    } else {
                        roomManager.destroyRoom(BuildConfig.AGORA_APP_ID, kSceneId, it.roomId) { e ->
                            if (e != null) {
                                ShowTo1v1Logger.e(TAG,e, "destroyRoom ${it.roomId} failed")
                            } else {
                                ShowTo1v1Logger.d(TAG, "broadcast leaveRoom end ${it.roomId}")
                            }
                        }
                    }
                }
                //按照创建时间顺序排序
                ret.sortBy { it.createdAt }
                ShowTo1v1Logger.d(TAG, "getRoomList end, roomCount:${ret.size}")
                runOnMainThread { completion.invoke(null, ret.toList()) }
            }
        }
    }

    override fun subscribeListener(listener: ShowTo1v1ServiceListenerProtocol) {
        this.listener = listener
    }

    // --------------------- inner ---------------------

    private fun runOnMainThread(r: Runnable) {
        if (Thread.currentThread() == mainHandler.looper.thread) {
            r.run()
        } else {
            mainHandler.post(r)
        }
    }

    override fun onSceneDestroy(roomId: String) {
        roomManager.destroyRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomId) {
            if (it!=null){
                ShowTo1v1Logger.e(TAG, it,"destroyRoom failed，roomId:$roomId")
            }else{
                ShowTo1v1Logger.d(TAG, "destroyRoom, roomId:$roomId")
            }
        }
    }

    // -------- IAUIUserService.AUIUserRespObserver ----------
    override fun onRoomUserSnapshot(roomId: String, userList: MutableList<AUIUserInfo>?) {
        userList?.let {
            this.userList = it
        }
    }

    override fun onRoomUserEnter(roomId: String, userInfo: AUIUserInfo) {
        ShowTo1v1Logger.d(TAG, "onRoomUserEnter, roomId:$roomId, userInfo:$userInfo")
        listener?.onUserListDidChanged(userList.size)
    }

    override fun onRoomUserLeave(roomId: String, userInfo: AUIUserInfo) {
        ShowTo1v1Logger.d(TAG, "onRoomUserLeave, roomId:$roomId, userInfo:$userInfo")
        listener?.onUserListDidChanged(userList.size)
    }

    override fun onRoomUserUpdate(roomId: String, userInfo: AUIUserInfo) {
        super.onRoomUserUpdate(roomId, userInfo)
        listener?.onUserListDidChanged(userList.size)
    }
}