package com.example.snek

import android.content.Context
import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.random.Random

class Engine(context: Context, size: Point) : SurfaceView(context), Runnable {
    private var thread: Thread? = null

    private var screenWidth: Int? = null
    private var screenHeight: Int? = null

    private enum class Direction {UP, LEFT, DOWN, RIGHT}
    private var currDirection: Direction? = null
    private var snekLength: Int? = null
    private var snekPos: MutableList<Point>? = null
    private var foodPos: Point? = null
    private var isPlaying: Boolean = false

    private var blockCountX: Int? = null
    private var blockCountY: Int? = null
    private var blockSize: Int? = null

    private var surfaceHolder: SurfaceHolder? = null
    private var canvas: Canvas? = null
    private var paint: Paint? = null

    private var FPS = 10
    private var lastUpdateTime: Long? = null

    private var touchStart: PointF? = null
    private var minSwipe: Float = 200F

    init{
        screenWidth = size.x
        screenHeight = size.y

        blockCountX = 40
        blockSize = screenWidth!! / blockCountX!!
        blockCountY = screenHeight!! / blockSize!!

        surfaceHolder = holder
        paint = Paint()

        lastUpdateTime = System.currentTimeMillis()

        startGame()
    }

    private fun startGame(){
        isPlaying = true
        snekLength = 1
        snekPos = MutableList(snekLength!!) {Point(blockCountX!!/2 ,blockCountY!!/2)}
        generateFood()
    }

    private fun generateFood(){
        foodPos = Point(Random.nextInt(0, blockCountX!!), Random.nextInt(0, blockCountY!!))
    }

    private fun eatFood(){
        snekLength = snekLength?.inc()
        snekPos?.add(0, foodPos!!)
        generateFood()
    }

    private fun moveSnek(){
        val newHeadPos = Point(snekPos!![0].x, snekPos!![0].y)
        snekPos?.removeAt(snekLength!! - 1)
        when(currDirection) {
            Direction.UP ->
                newHeadPos.y--
            Direction.DOWN ->
                newHeadPos.y++
            Direction.RIGHT ->
                newHeadPos.x++
            Direction.LEFT ->
                newHeadPos.x--
        }
        when(newHeadPos.x) {
            -1 ->
                newHeadPos.x = blockCountX!! - 1
            blockCountX!! ->
                newHeadPos.x = 0
        }
        when(newHeadPos.y) {
            -1 ->
                newHeadPos.y = blockCountY!! - 1
            blockCountY!! ->
                newHeadPos.y = 0
        }
        snekPos?.add(0, newHeadPos)
    }

    private fun checkCollision(): Boolean{
        val headPos = snekPos!![0]
        if(snekPos?.drop(1)?.contains(headPos) == true) {
            return true
        }
        return false
    }

    override fun run(){
        while(isPlaying) {
            if (updateRequired()) {
                updateState()
                render()
            }
        }
    }

    fun pause() {
        isPlaying = false
        try {
            thread?.join()
        } catch (e: InterruptedException){}
    }

    fun resume(){
        isPlaying = true
        thread = Thread(this)
        thread?.start()
    }

    private fun updateRequired(): Boolean{
        if(System.currentTimeMillis() >= lastUpdateTime!! + 1000/FPS){
            return true
        }
        return false
    }

    private fun updateState(){
        val headPos = snekPos!![0]
        when(currDirection){
            Direction.UP ->
                if(headPos.x == foodPos?.x && headPos.y == foodPos?.y!! + 1){
                    eatFood()
                }
            Direction.DOWN ->
                if(headPos.x == foodPos?.x && headPos.y == foodPos?.y!! - 1){
                    eatFood()
                }
            Direction.LEFT ->
                if(headPos.y == foodPos?.y && headPos.x == foodPos?.x!! + 1){
                    eatFood()
                }
            Direction.RIGHT ->
                if(headPos.y == foodPos?.y && headPos.x == foodPos?.x!! - 1){
                    eatFood()
                }
        }

        moveSnek()
        if(checkCollision()){
            isPlaying = false
        }
        lastUpdateTime = System.currentTimeMillis()
    }

    private fun render(){
        if(surfaceHolder?.surface?.isValid == true){
            canvas = surfaceHolder?.lockCanvas();
            canvas?.drawColor(Color.rgb(0, 0, 0))

            paint?.color = Color.rgb(255, 255, 255)
            for(i in 0 until snekLength!!){
                canvas?.drawRect((snekPos!![i].x * blockSize!!).toFloat(),
                        (snekPos!![i].y * blockSize!!).toFloat(),
                        (snekPos!![i].x * blockSize!! + blockSize!!).toFloat(),
                        (snekPos!![i].y * blockSize!! + blockSize!!).toFloat(), paint!!)
            }

            paint?.color = Color.rgb(255, 0, 0)
            canvas?.drawRect((foodPos!!.x * blockSize!!).toFloat(),
                    (foodPos!!.y * blockSize!!).toFloat(),
                    (foodPos!!.x * blockSize!! + blockSize!!).toFloat(),
                    (foodPos!!.y * blockSize!! + blockSize!!).toFloat(), paint!!)

            surfaceHolder?.unlockCanvasAndPost(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val act = event?.action
        if(act == MotionEvent.ACTION_DOWN){
            touchStart = PointF(event?.x!!, event.y)
        }
        else if(act == MotionEvent.ACTION_UP){
            if(abs(touchStart!!.x - event?.x!!) >= minSwipe){
                if(touchStart!!.x > event.x && currDirection != Direction.RIGHT){
                    currDirection = Direction.LEFT
                }
                else if(touchStart!!.x < event.x && currDirection != Direction.LEFT){
                    currDirection = Direction.RIGHT
                }
            }
            else if(abs(touchStart!!.y - event.y) >= minSwipe){
                if(touchStart!!.y > event.y && currDirection != Direction.DOWN){
                    currDirection = Direction.UP
                }
                else if(touchStart!!.y < event.y && currDirection != Direction.UP){
                    currDirection = Direction.DOWN
                }
            }
        }
        return true
    }
}

class MainActivity : AppCompatActivity() {
    private var engine: Engine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        var displaySize: Point = Point(-1, -1)
//        applicationContext.display?.getRealSize(displaySize)
//        engine = Engine(applicationContext, displaySize)
        engine = Engine(applicationContext, Point(1500, 800))
        setContentView(engine)
    }

    override fun onPause(){
        super.onPause()
        engine?.pause()
    }

    override fun onResume(){
        super.onResume()
        engine?.resume()
    }
}