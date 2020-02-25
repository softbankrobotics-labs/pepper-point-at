package com.softbankrobotics.pepperpointatsample

import android.os.Bundle
import android.util.Log
import android.view.View
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.`object`.actuation.Animate
import com.aldebaran.qi.sdk.`object`.actuation.Frame
import com.aldebaran.qi.sdk.`object`.conversation.*
import com.aldebaran.qi.sdk.`object`.locale.Language
import com.aldebaran.qi.sdk.`object`.locale.Locale
import com.aldebaran.qi.sdk.`object`.locale.Region
import com.aldebaran.qi.sdk.builder.*
import com.aldebaran.qi.sdk.design.activity.RobotActivity
import com.aldebaran.qi.sdk.design.activity.conversationstatus.SpeechBarDisplayStrategy
import com.softbankrobotics.pepperpointat.PointAtAnimation
import com.softbankrobotics.pepperpointat.PointAtAnimator
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.Future

class MainActivity : RobotActivity(), RobotLifecycleCallbacks, View.OnClickListener {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var qiContext: QiContext
    private lateinit var pointAtAnimator: PointAtAnimator

    private lateinit var catFrame: Frame
    private lateinit var cowFrame: Frame

    private lateinit var pointAtAnimateFuture: Future<Void>

    private lateinit var mainTopic: Topic
    private lateinit var qiChatbot: QiChatbot
    private lateinit var chat: Chat

    private lateinit var imHereAnimate: Animate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSpeechBarDisplayStrategy(SpeechBarDisplayStrategy.IMMERSIVE)
        setContentView(R.layout.activity_main)
        QiSDK.register(this, this)

        monkey.setOnClickListener(this)
        tiger.setOnClickListener(this)
        wolf.setOnClickListener(this)
        lion.setOnClickListener(this)
        pig.setOnClickListener(this)
        cat.setOnClickListener(this)
        horse.setOnClickListener(this)
        cow.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        val animalName = view?.tag as String
        goToBookmark(animalName)
        runPointAtAnimation(animalName)
    }

    private fun runPointAtAnimation(animalName: String) {
        if (!::qiContext.isInitialized) {
            return
        }

        if (::pointAtAnimateFuture.isInitialized && !pointAtAnimateFuture.isDone) {
            return
        }

        pointAtAnimateFuture = when (animalName) {
            "monkey" -> pointAtAnimator.playPointAnimation(PointAtAnimation.MEDIUM_LEFT)
            "tiger" -> pointAtAnimator.playPointAnimation(PointAtAnimation.FRONT_RIGHT)
            "dog" -> pointAtAnimator.playPointAnimation(PointAtAnimation.CLOSE_HALF_LEFT)
            "wolf" -> pointAtAnimator.playPointAnimation(PointAtAnimation.CLOSE_FRONT_LEFT)
            "pig" -> pointAtAnimator.playPointAnimation(PointAtAnimation.CLOSE_MEDIUM_RIGHT)
            "horse" -> pointAtAnimator.playPointAnimation(PointAtAnimation.HALF_RIGHT)
            "cat" -> pointAtAnimator.pointAt(catFrame)
            "cow" -> pointAtAnimator.pointAt(cowFrame)
            "pepper" -> imHereAnimate.async().run()
            else -> return
        }
    }

    override fun onResume() {
        super.onResume()
        // Enables sticky immersive mode.
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun onRobotFocusGained(qiContext: QiContext) {
        Log.i(TAG, "onRobotFocusGained")
        this.qiContext = qiContext
        pointAtAnimator = PointAtAnimator(qiContext)

        // Build target Frames used to point at the giraffe and the panda
        buildTargetFrames()
        buildAndRunChat()

        // Build Animate to run when asking where's Pepper
        val animation = AnimationBuilder.with(qiContext)
            .withResources(R.raw.show_tablet_a004)
            .build()

        imHereAnimate = AnimateBuilder.with(qiContext)
            .withAnimation(animation)
            .build()
    }

    private fun buildTargetFrames() {
        val tigerFreeFrame = qiContext.mapping.makeFreeFrame()
        val cowFreeFrame = qiContext.mapping.makeFreeFrame()
        val robotFrame = qiContext.actuation.robotFrame()
        var transform = TransformBuilder.create().from2DTranslation(-1.0, -1.0)
        tigerFreeFrame.update(robotFrame, transform, 0L)
        catFrame = tigerFreeFrame.frame()
        transform = TransformBuilder.create().from2DTranslation(-10.0, 10.0)
        cowFreeFrame.update(robotFrame, transform, 0L)
        cowFrame = cowFreeFrame.frame()
    }

    private fun buildAndRunChat() {
        // Create the topic
        mainTopic = TopicBuilder.with(qiContext)
            .withResource(R.raw.main)
            .build()

        // Create the QiChatbot
        qiChatbot = QiChatbotBuilder.with(qiContext)
            .withTopic(mainTopic)
            .withLocale(Locale(Language.ENGLISH, Region.UNITED_STATES))
            .build()

        // Create the Chat action
        chat = ChatBuilder.with(qiContext)
            .withChatbot(qiChatbot)
            .withLocale(Locale(Language.ENGLISH, Region.UNITED_STATES))
            .build()

        // Go to "start" bookmark when launching Chat
        chat.addOnStartedListener {
            goToBookmark("start")
        }

        // Launch Chat
        chat.async().run()

        // Launch PointAtAnimations when reaching Bookmarks
        qiChatbot.addOnBookmarkReachedListener {
            runPointAtAnimation(it.name)
        }
    }

    private fun goToBookmark(bookmarkName: String) {
        mainTopic.async().bookmarks.andThenConsume {
            val bookmark = it[bookmarkName]
            qiChatbot.goToBookmark(
                bookmark,
                AutonomousReactionImportance.HIGH,
                AutonomousReactionValidity.IMMEDIATE
            )
        }
    }

    override fun onRobotFocusLost() {
        Log.i(TAG, "onRobotFocusLost")
        chat.removeAllOnStartedListeners()
    }

    override fun onRobotFocusRefused(reason: String?) {
        Log.e(TAG, "onRobotFocusRefused: $reason")
    }

    override fun onDestroy() {
        super.onDestroy()
        QiSDK.unregister(this, this)
    }
}
