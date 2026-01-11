package com.shimizu.ar.core.ar_message.kotlin.ar_message

import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.Trackable
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.shimizu.ar.core.ar_message.java.common.Infrastructure.GeospatialArMessageModel
import com.shimizu.ar.core.ar_message.java.common.helpers.DisplayRotationHelper
import com.shimizu.ar.core.ar_message.java.common.helpers.TrackingStateHelper
import com.shimizu.ar.core.ar_message.java.common.samplerender.Framebuffer
import com.shimizu.ar.core.ar_message.java.common.samplerender.Mesh
import com.shimizu.ar.core.ar_message.java.common.samplerender.SampleRender
import com.shimizu.ar.core.ar_message.java.common.samplerender.Shader
import com.shimizu.ar.core.ar_message.java.common.samplerender.Texture
import com.shimizu.ar.core.ar_message.java.common.samplerender.VertexBuffer
import com.shimizu.ar.core.ar_message.java.common.samplerender.arcore.BackgroundRenderer
import com.shimizu.ar.core.ar_message.java.common.samplerender.arcore.PlaneRenderer
import com.shimizu.ar.core.ar_message.java.common.samplerender.arcore.SpecularCubemapFilter
import com.shimizu.ar.core.ar_message.kotlin.domain.collection.GeospatialArMessageCollection
import com.shimizu.ar.core.ar_message.kotlin.domain.MessageTextBitmap
import com.shimizu.ar.core.ar_message.kotlin.domain.UserCurrentPosition
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.shimizu.ar.core.ar_message.java.common.Infrastructure.GeospatialEmojiModel
import com.shimizu.ar.core.ar_message.java.common.Infrastructure.data_class.interfaces.GeospatialData
import com.shimizu.ar.core.ar_message.kotlin.domain.RenderEmojiObject
import com.shimizu.ar.core.ar_message.kotlin.domain.RenderMessageObject
import com.shimizu.ar.core.ar_message.kotlin.domain.collection.GeospatialEmojiCollection
import com.shimizu.ar.core.ar_message.kotlin.domain.interfaces.RenderObject
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

class MessageRenderer (val activity: ArMessageActivity,
                       private var messageTextBitmap: MessageTextBitmap?,
                       private val message: String?,
                       private var emojiType: String?) : SampleRender.Renderer, DefaultLifecycleObserver {

    private val TAG = "MessageRenderer"

    private val Z_NEAR = 0.1f
    private val Z_FAR = 100f

    private val APPROXIMATE_DISTANCE_METERS = 2.0f
    private val CUBEMAP_RESOLUTION = 16
    private val CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32

    lateinit var render: SampleRender
    private lateinit var planeRenderer: PlaneRenderer
    private lateinit var backgroundRenderer: BackgroundRenderer
    private lateinit var virtualSceneFramebuffer: Framebuffer

    private var hasSetTextureNames = false

    // render object
    private lateinit var renderObject: RenderObject

    // Geospatial virtual object
    private var geospatialObjectCollection = CopyOnWriteArrayList<GeospatialObject>()

    private lateinit var dfgTexture: Texture
    private lateinit var cubemapFilter: SpecularCubemapFilter

    // Point Cloud
    private lateinit var pointCloudVertexBuffer: VertexBuffer
    private lateinit var pointCloudMesh: Mesh
    private lateinit var pointCloudShader: Shader

    private val wrappedAnchors = mutableListOf<WrappedAnchor>()

    private var lastPointCloudTimestamp: Long = 0

    private val displayRotationHelper = DisplayRotationHelper(activity)
    private val trackingStateHelper   = TrackingStateHelper(activity)

    private val session
        get() = activity.arCoreSessionHelper.session

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix       = FloatArray(16)
    private val modelMatrix      = FloatArray(16)
    private val modelViewMatrix  = FloatArray(16)

    private val modelViewProjectionMatrix = FloatArray(16)

    private val geospatialArMessageCollection = GeospatialArMessageCollection()
    private var geospatialEmojiCollection     = GeospatialEmojiCollection()

    private var currentWrappedAnchor: WrappedAnchor? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var userLastPosition: UserCurrentPosition

    override fun onCreate(owner: LifecycleOwner) {
        val delayMillis: Long = 1000 * 3
        handler.postDelayed(object : Runnable {
            override fun run() {
                setGeospatialArMessageCollection()
                handler.postDelayed(this, delayMillis)
            }
        }, delayMillis)
    }

    override fun onResume(owner: LifecycleOwner) {
        displayRotationHelper.onResume()
        hasSetTextureNames = false
    }

    private fun setGeospatialArMessageCollection() {
        val session = session ?: return
        val earth = session.earth
        if (earth?.trackingState != TrackingState.TRACKING) return
        if (geospatialArMessageCollection.createArMessageObject || geospatialEmojiCollection.createEmojiObject) return
        val geospatialPose = earth.cameraGeospatialPose
        // 精度設定
        setGeospatialAccuracy(geospatialPose.horizontalAccuracy, geospatialPose.verticalAccuracy)
        if (::userLastPosition.isInitialized) {
            val userNowPosition = UserCurrentPosition(geospatialPose.longitude, geospatialPose.latitude, geospatialPose.altitude)
            if (userLastPosition.comparePosition(userNowPosition)) {
                geospatialArMessageCollection.cleanCollection()
                geospatialEmojiCollection.cleanCollection()
                GeospatialArMessageModel().getArMessage(userNowPosition, geospatialArMessageCollection)
                GeospatialEmojiModel().getEmoji(userNowPosition, geospatialEmojiCollection)
            }
        } else {
            userLastPosition = UserCurrentPosition(geospatialPose.longitude, geospatialPose.latitude, geospatialPose.altitude)
            GeospatialArMessageModel().getArMessage(userLastPosition, geospatialArMessageCollection)
            GeospatialEmojiModel().getEmoji(userLastPosition, geospatialEmojiCollection)
        }
    }

    private fun setGeospatialAccuracy(horizontalAccuracy: Double, verticalAccuracy: Double) {
        val imageView = activity.findViewById<ImageView>(R.id.acc_image)
        when {
            horizontalAccuracy < 1 && verticalAccuracy < 1-> {
                // 精度良い
                imageView.setImageResource(R.drawable.good_acc)
            }
            horizontalAccuracy < 5 && verticalAccuracy < 5-> {
                // 精度普通
                imageView.setImageResource(R.drawable.middle_acc)
            }
            else -> {
                // 精度悪い
                imageView.setImageResource(R.drawable.bad_acc)
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        displayRotationHelper.onPause()
    }

    override fun onSurfaceCreated(render: SampleRender) {
        try {
            planeRenderer = PlaneRenderer(render)
            backgroundRenderer = BackgroundRenderer(render)
            virtualSceneFramebuffer = Framebuffer(render, /*width=*/ 1, /*height=*/ 1)

            cubemapFilter =
                SpecularCubemapFilter(render, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES)
            dfgTexture =
                Texture(
                    render,
                    Texture.Target.TEXTURE_2D,
                    Texture.WrapMode.CLAMP_TO_EDGE,
                    /*useMipmaps=*/ false
                )

            // Point cloud
            pointCloudShader =
                Shader.createFromAssets(
                    render,
                    "shaders/point_cloud.vert",
                    "shaders/point_cloud.frag",
                    /*defines=*/ null
                )
                    .setVec4("u_Color", floatArrayOf(31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f))
                    .setFloat("u_PointSize", 5.0f)

            // four entries per vertex: X, Y, Z, confidence
            pointCloudVertexBuffer =
                VertexBuffer(render, /*numberOfEntriesPerVertex=*/ 4, /*entries=*/ null)
            val pointCloudVertexBuffers = arrayOf(pointCloudVertexBuffer)
            pointCloudMesh =
                Mesh(render, Mesh.PrimitiveMode.POINTS, /*indexBuffer=*/ null, pointCloudVertexBuffers)

            // メッセージがある場合メッセ―ジオブジェクト作成
            if (messageTextBitmap != null) {
                renderObject = RenderMessageObject(render, messageTextBitmap!!, message!!, cubemapFilter, dfgTexture)
            }
            // 絵文字がある場合絵文字オブジェクト作成
            else if (emojiType != null) {
                renderObject = RenderEmojiObject(render, emojiType!!, cubemapFilter, dfgTexture)
            }

        } catch (e: IOException) {
            Log.e(TAG, "Failed to read a required asset file", e)
            showError("Failed to read a required asset file: $e")
        }
    }

    override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
        virtualSceneFramebuffer.resize(width, height)
    }

    override fun onDrawFrame(render: SampleRender) {
        val session = session ?: return

        // ArCoreSessionにカメラのテクスチャ設定
        if (!hasSetTextureNames) {
            session.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))
            hasSetTextureNames = true
        }

        // 画面サイズの変更を通知
        displayRotationHelper.updateSessionIfNeeded(session)

        // 現在のフレーム取得
        val frame =
            try {
                session.update()
            } catch (e: CameraNotAvailableException) {
                Log.e(TAG, "Camera not available during onDrawFrame", e)
                showError("Camera not available. Try restarting the app.")
                return
            }

        val camera = frame.camera

        // 深度に合わせて背景を更新
        try {
            backgroundRenderer.setUseDepthVisualization(
                render,
                activity.depthSettings.depthColorVisualizationEnabled()
            )
            backgroundRenderer.setUseOcclusion(render, activity.depthSettings.useDepthForOcclusion())
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read a required asset file", e)
            showError("Failed to read a required asset file: $e")
            return
        }

        // 深度画像が利用可能な場合は、 深度テクスチャを更新
        backgroundRenderer.updateDisplayGeometry(frame)
        val shouldGetDepthImage =
            activity.depthSettings.useDepthForOcclusion() ||
                    activity.depthSettings.depthColorVisualizationEnabled()
        if (camera.trackingState == TrackingState.TRACKING && shouldGetDepthImage) {
            try {
                val depthImage = frame.acquireDepthImage16Bits()
                backgroundRenderer.updateCameraDepthTexture(depthImage)
                depthImage.close()
            } catch (e: NotYetAvailableException) {
                // This normally means that depth data is not available yet. This is normal so we will not
                // spam the logcat with this.
            }
        }

        // トラッキング状態に基づいて、 画面をロックしないようにするフラグを更新
        trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

        // 状態に応じてメッセージ取得
        val message: String? =
            when {
                camera.trackingState == TrackingState.PAUSED &&
                        camera.trackingFailureReason == TrackingFailureReason.NONE ->
                    activity.getString(R.string.searching_planes)
                camera.trackingState == TrackingState.PAUSED ->
                    TrackingStateHelper.getTrackingFailureReasonString(camera)
                currentWrappedAnchor != null -> activity.getString(R.string.send_object)
                session.hasTrackingPlane() && wrappedAnchors.isEmpty() && (messageTextBitmap != null || emojiType != null)->
                    activity.getString(R.string.waiting_taps)
                session.hasTrackingPlane() && wrappedAnchors.isNotEmpty() -> null
                else -> activity.getString(R.string.searching_planes)
            }
        if (message == null) {
            activity.view.snackbarHelper.hide(activity)
        } else {
            activity.view.snackbarHelper.showMessage(activity, message)
        }

        // -- 背景描写
        if (frame.timestamp != 0L) {
            backgroundRenderer.drawBackground(render)
        }

        // カメラのトラッキングが一時停止なら終了
        if (camera.trackingState == TrackingState.PAUSED) {
            return
        }

        // 投影行列取得
        camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR)

        // カメラからview行列取得
        camera.getViewMatrix(viewMatrix, 0)

        // 点群描写
        frame.acquirePointCloud().use { pointCloud ->
            if (pointCloud.timestamp > lastPointCloudTimestamp) {
                pointCloudVertexBuffer.set(pointCloud.points)
                lastPointCloudTimestamp = pointCloud.timestamp
            }
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
            pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            render.draw(pointCloudMesh, pointCloudShader)
        }

        // 平面描写
        planeRenderer.drawPlanes(
            render,
            session.getAllTrackables<Plane>(Plane::class.java),
            camera.displayOrientedPose,
            projectionMatrix
        )

        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f)

        if (messageTextBitmap != null || emojiType != null) {
            // タップ処理
            handleTap(frame, camera)
            handleSwipe(frame, camera)
            handleTwoFingerSwipe(camera)
            renderObject(render)
        }

        createGeospatialObject(render)
        renderGeospatialObject(render)

        // Compose the virtual scene with the background.
        backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR)
    }

    private fun renderObject(render: SampleRender) {
        // タッチ操作で作成されたアンカーに仮想オブジェクトを描写する
        if (currentWrappedAnchor?.anchor?.trackingState == TrackingState.TRACKING) {
            // Anchorオブジェクトの姿勢をモデル行列に変換する
            currentWrappedAnchor!!.anchor.pose.toMatrix(modelMatrix, 0)

            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

            renderObject.virtualObjectShader.setMat4("u_ModelView", modelViewMatrix)
            renderObject.virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            // 描写
            render.draw(renderObject.virtualObjectMesh, renderObject.virtualObjectShader, virtualSceneFramebuffer)
        }
    }

    private fun createGeospatialObject(render: SampleRender) {
        if (geospatialArMessageCollection.createArMessageObject && geospatialEmojiCollection.createEmojiObject) {
            geospatialObjectCollection.clear()
            createGeospatialArMessage(render)
            createEmojiObject(render)
            geospatialArMessageCollection.changeCreateArMessageObject(false)
            geospatialEmojiCollection.changeCreateEmojiObject(false)
            val textView = activity.findViewById<TextView>(R.id.message_count_text)
            textView.text = geospatialObjectCollection.size.toString()
        }
    }

    private fun createGeospatialArMessage(render: SampleRender) {
        val earth = session!!.earth
        if (earth?.trackingState != TrackingState.TRACKING) return
        if (geospatialArMessageCollection.geospatialArMessages.size > 0) {
            geospatialArMessageCollection.geospatialArMessages.forEach { geospatialArMessage ->
                 //画像をもとにオブジェクト作成
                val geospatialObjectAlbedoTexture =
                    Texture.createFromBitmap(
                        render,
                        geospatialArMessage.messageTextBitmap.messageTextBitmap,
                        Texture.WrapMode.CLAMP_TO_EDGE,
                        Texture.ColorFormat.SRGB
                    )

                val geospatialObjectMesh = Mesh.createFromInternalStorage(render,  geospatialArMessage.messageTextObjectPath)
                val geospatialObjectShader =
                    Shader.createFromAssets(
                        render,
                        "shaders/environmental_hdr.vert",
                        "shaders/environmental_hdr.frag",
                        mapOf("NUMBER_OF_MIPMAP_LEVELS" to cubemapFilter.numberOfMipmapLevels.toString())
                    )
                        .setTexture("u_AlbedoTexture", geospatialObjectAlbedoTexture)
                        .setTexture("u_Cubemap", cubemapFilter.filteredCubemapTexture)
                        .setTexture("u_DfgTexture", dfgTexture)

                geospatialObjectCollection.add(GeospatialObject(geospatialArMessage.arMessageData, geospatialObjectMesh, geospatialObjectShader))
            }
        }
    }

    private fun createEmojiObject(render: SampleRender) {
        val earth = session!!.earth
        if (earth?.trackingState != TrackingState.TRACKING) return
        if (geospatialEmojiCollection.geospatialEmojis.size > 0) {
            geospatialEmojiCollection.geospatialEmojis.forEach { emojiData ->
                //画像をもとにオブジェクト作成
                val geospatialObjectAlbedoTexture =
                    Texture.createFromAsset(
                        render,
                        "models/"+ emojiData.emojiType + ".jpg",
                        Texture.WrapMode.CLAMP_TO_EDGE,
                        Texture.ColorFormat.SRGB
                    )

                val geospatialObjectMesh = Mesh.createFromAsset(render,  "models/"+ emojiData.emojiType + ".obj")
                val geospatialObjectShader =
                    Shader.createFromAssets(
                        render,
                        "shaders/environmental_hdr.vert",
                        "shaders/environmental_hdr.frag",
                        mapOf("NUMBER_OF_MIPMAP_LEVELS" to cubemapFilter.numberOfMipmapLevels.toString())
                    )
                        .setTexture("u_AlbedoTexture", geospatialObjectAlbedoTexture)
                        .setTexture("u_Cubemap", cubemapFilter.filteredCubemapTexture)
                        .setTexture("u_DfgTexture", dfgTexture)
                geospatialObjectCollection.add(GeospatialObject(emojiData, geospatialObjectMesh, geospatialObjectShader))
            }
        }
    }

    private fun renderGeospatialObject(render: SampleRender) {
        val earth = session?.earth
        if (earth!!.trackingState != TrackingState.TRACKING) return
        geospatialObjectCollection.forEach { geospatialObject ->
            val anchor = earth.createAnchor(
                geospatialObject.geospatialData.latitude,
                geospatialObject.geospatialData.longitude,
                geospatialObject.geospatialData.altitude,
                geospatialObject.geospatialData.quaternion_x,
                geospatialObject.geospatialData.quaternion_y,
                geospatialObject.geospatialData.quaternion_z,
                geospatialObject.geospatialData.quaternion_w)
            anchor.pose.toMatrix(modelMatrix, 0)
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

            geospatialObject.shader.setMat4("u_ModelView", modelViewMatrix)
            geospatialObject.shader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            // 描写
            render.draw(geospatialObject.mesh, geospatialObject.shader, virtualSceneFramebuffer)
        }
    }

    private fun Session.hasTrackingPlane() =
        getAllTrackables(Plane::class.java).any { it.trackingState == TrackingState.TRACKING }

    // タップ処理
    private fun handleTap(frame: Frame, camera: Camera) {
        if (camera.trackingState != TrackingState.TRACKING) return
        val tap = activity.view.tapHelper.poll() ?: return

        if (currentWrappedAnchor != null) {
            saveAnchor(currentWrappedAnchor!!)
            messageTextBitmap    = null
            emojiType            = null
            currentWrappedAnchor = null
            return
        }

        val hitResultList =
            if (activity.instantPlacementSettings.isInstantPlacementEnabled) {
                frame.hitTestInstantPlacement(tap.x, tap.y, APPROXIMATE_DISTANCE_METERS)
            } else {
                frame.hitTest(tap)
            }

        val firstHitResult =
            hitResultList.firstOrNull { hit ->
                when (val trackable = hit.trackable!!) {
                    is Plane ->
                        trackable.isPoseInPolygon(hit.hitPose) &&
                                PlaneRenderer.calculateDistanceToPlane(hit.hitPose, camera.pose) > 0
                    is Point -> trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
                    is InstantPlacementPoint -> true
                    is DepthPoint -> true
                    else -> false
                }
            }

        if (firstHitResult != null) {

            currentWrappedAnchor= WrappedAnchor(firstHitResult.createAnchor(), firstHitResult.trackable)

            activity.runOnUiThread { activity.view.showOcclusionDialogIfNeeded() }
        }
    }

    private fun handleSwipe(frame: Frame, camera: Camera) {
        if (camera.trackingState != TrackingState.TRACKING) return
        if (currentWrappedAnchor == null) return
        val swipe = activity.view.tapHelper.swipePoll() ?: return

        val hitResultList =
            if (activity.instantPlacementSettings.isInstantPlacementEnabled) {
                frame.hitTestInstantPlacement(swipe.x, swipe.y, APPROXIMATE_DISTANCE_METERS)
            } else {
                frame.hitTest(swipe.x, swipe.y)
            }

        val firstHitResult =
            hitResultList.firstOrNull { hit ->
                when (val trackable = hit.trackable!!) {
                    is Plane ->
                        trackable.isPoseInPolygon(hit.hitPose) &&
                                PlaneRenderer.calculateDistanceToPlane(hit.hitPose, camera.pose) > 0
                    is Point -> trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
                    is InstantPlacementPoint -> true
                    is DepthPoint -> true
                    else -> false
                }
            }

        if (firstHitResult != null) {
            currentWrappedAnchor = WrappedAnchor(firstHitResult.createAnchor(), firstHitResult.trackable)
        }
    }

    private fun handleTwoFingerSwipe(camera: Camera) {
        if (camera.trackingState != TrackingState.TRACKING) return
        if (currentWrappedAnchor == null) return
        val twoFingerSwipe = activity.view.tapHelper.twoFingerSwipePoll() ?: return

        // X軸回転角度を計算
        val rotationAngleX = twoFingerSwipe["x"].toString().toFloat() * 0.5f
        // Y軸回転角度を計算
        val rotationAngleY = twoFingerSwipe["y"].toString().toFloat() * 0.5f
        // クォータニオンを作成 (X軸回転とY軸回転を合成)
        val quaternionX = Quaternion.axisAngle(Vector3(0f, 0f, 1f), rotationAngleX)
        val quaternionY = Quaternion.axisAngle(Vector3(-1f, 0f, 0f), rotationAngleY)
        val quaternion  = Quaternion.slerp(quaternionX, quaternionY, 0.5f)
        val currentPose = currentWrappedAnchor!!.anchor.pose
        val newPose = currentPose.compose(Pose.makeRotation(quaternion.x, quaternion.y, quaternion.z, quaternion.w))
        currentWrappedAnchor =
            session?.let { WrappedAnchor(it.createAnchor(newPose), currentWrappedAnchor!!.trackable) }
    }

    private fun saveAnchor(anchor: WrappedAnchor) {
        val earth = session!!.earth
        if (earth?.trackingState != TrackingState.TRACKING) return
        val geospatialAnchor = earth.getGeospatialPose(anchor.anchor.pose)
        val longitude = geospatialAnchor.longitude
        val latitude = geospatialAnchor.latitude
        val altitude = geospatialAnchor.altitude
        val quaternion = geospatialAnchor.getEastUpSouthQuaternion()
        val geospatialData = renderObject.saveAnchor(longitude, latitude, altitude, quaternion)
        geospatialObjectCollection.add(GeospatialObject(geospatialData, renderObject.virtualObjectMesh, renderObject.virtualObjectShader))
    }

    private fun showError(errorMessage: String) =
        activity.view.snackbarHelper.showError(activity, errorMessage)
}

private data class WrappedAnchor(
    val anchor: Anchor,
    val trackable: Trackable,
)

private data class GeospatialObject(
    val geospatialData: GeospatialData,
    val mesh: Mesh,
    val shader: Shader,
)