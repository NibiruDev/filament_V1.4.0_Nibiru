/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.filament.ibl

import android.animation.ValueAnimator
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import android.view.animation.LinearInterpolator
import com.google.android.filament.*
import com.google.android.filament.android.UiHelper
import com.nibiru.lib.vr.NibiruVR
import com.nibiru.lib.vr.NibiruVRService
import java.nio.ByteBuffer
import java.nio.channels.Channels
import kotlin.math.PI

class MainActivity : Activity() {
    var TAG: String = "ccc"
    // Make sure to initialize Filament first
    // This loads the JNI library needed by most API calls
    companion object {
        init {
            Filament.init()
        }
    }

    // The View we want to render into
    private lateinit var surfaceView: SurfaceView
    // UiHelper is provided by Filament to manage SurfaceView and SurfaceTexture
    private lateinit var uiHelper: UiHelper
    // Choreographer is used to schedule new frames
    private lateinit var choreographer: Choreographer

    // Engine creates and destroys Filament resources
    // Each engine must be accessed from a single thread of your choosing
    // Resources cannot be shared across engines
    private lateinit var engine: Engine
    // A renderer instance is tied to a single surface (SurfaceView, TextureView, etc.)
    private lateinit var renderer: Renderer
    // A scene holds all the renderable, lights, etc. to be drawn
    private lateinit var scene: Scene
    // A view defines a viewport, a scene and a camera for rendering
    private lateinit var view: View
    // Should be pretty obvious :)
    private lateinit var camera: Camera

    private lateinit var material: Material
    private lateinit var materialInstance: MaterialInstance

    private lateinit var mesh: Mesh
    private lateinit var ibl: Ibl

    // Filament entity representing a renderable object
    @Entity private var light = 0

    // A swap chain is Filament's representation of a surface
    private var swapChain: SwapChain? = null

    // Performs the rendering and schedules new frames
    private val frameScheduler = FrameCallback()

    private val animator = ValueAnimator.ofFloat(0.0f, (2.0 * PI).toFloat())

    var servicePtr:Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        choreographer = Choreographer.getInstance()

        var initParams:String  = NibiruVR.initNibiruVRServiceForUnreal(this)
        servicePtr = initParams.split("_").get(0).toLong()

        setupSurfaceView()
        setupFilament()
        setupView()
        setupScene()
    }

    private fun setupSurfaceView() {
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
        uiHelper.renderCallback = SurfaceCallback()

        // NOTE: To choose a specific rendering resolution, add the following line:
        // uiHelper.setDesiredSize(1280, 720)

        uiHelper.attachTo(surfaceView)
    }

    private fun setupFilament() {
        engine = Engine.create(servicePtr)
        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        camera = engine.createCamera()
    }

    private fun setupView() {
        // Clear the background to middle-grey
        // Setting up a clear color is useful for debugging but usually
        // unnecessary when using a skybox
        view.setClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        view.ambientOcclusion = View.AmbientOcclusion.SSAO

        // NOTE: Try to disable post-processing (tone-mapping, etc.) to see the difference
        // view.isPostProcessingEnabled = false

        // Tell the view which camera we want to use
        view.camera = camera

        // Tell the view which scene we want to render
        view.scene = scene
    }

    private fun setupScene() {
        loadMaterial()
        setupMaterial()
        loadImageBasedLight()

        scene.skybox = ibl.skybox
        scene.indirectLight = ibl.indirectLight

        // This map can contain named materials that will map to the material names
        // loaded from the filamesh file. The material called "DefaultMaterial" is
        // applied when no named material can be found
        val materials = mapOf("DefaultMaterial" to materialInstance)

        // Load the mesh in the filamesh format (see filamesh tool)
        mesh = loadMesh(assets, "models/shader_ball.filamesh", materials, engine)

        // Move the mesh down
        // Filament uses column-major matrices
        engine.transformManager.setTransform(engine.transformManager.getInstance(mesh.renderable),
                floatArrayOf(
                        1.0f,  0.0f, 0.0f, 0.0f,
                        0.0f,  1.0f, 0.0f, 0.0f,
                        0.0f,  0.0f, 1.0f, 0.0f,
                        0.0f, -1.2f, -4.0f, 1.0f
                ))

        // Add the entity to the scene to render it
        scene.addEntity(mesh.renderable)

        // We now need a light, let's create a directional light
        light = EntityManager.get().create()

        // Create a color from a temperature (D65)
        val (r, g, b) = Colors.cct(6_500.0f)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(r, g, b)
                // Intensity of the sun in lux on a clear day
                .intensity(110_000.0f)
                // The direction is normalized on our behalf
                .direction(-0.753f, -1.0f, 0.890f)
                .castShadows(true)
                .build(engine, light)

        // Add the entity to the scene to light it
        scene.addEntity(light)

        // Set the exposure on the camera, this exposure follows the sunny f/16 rule
        // Since we've defined a light that has the same intensity as the sun, it
        // guarantees a proper exposure
        camera.setExposure(16.0f, 1.0f / 125.0f, 100.0f)

        startAnimation()
    }

    private fun loadMaterial() {
        readUncompressedAsset("materials/clear_coat.filamat").let {
            material = Material.Builder().payload(it, it.remaining()).build(engine)
        }
    }

    private fun setupMaterial() {
        // Create an instance of the material to set different parameters on it
        materialInstance = material.createInstance()
        // Specify that our color is in sRGB so the conversion to linear
        // is done automatically for us. If you already have a linear color
        // you can pass it directly, or use Colors.RgbType.LINEAR
        materialInstance.setParameter("baseColor", Colors.RgbType.SRGB, 0.71f, 0.0f, 0.0f)
    }

    private fun loadImageBasedLight() {
        ibl = loadIbl(assets, "envs/flower_road_no_sun_2k", engine)
        ibl.indirectLight.intensity = 40_000.0f
    }

    private fun startAnimation() {
        // Animate the triangle
        animator.interpolator = LinearInterpolator()
        animator.duration = 18_000
        animator.repeatMode = ValueAnimator.RESTART
        animator.repeatCount = ValueAnimator.INFINITE
        animator.addUpdateListener { a ->
            val v = (a.animatedValue as Float)
            // camera.lookAt(cos(v) * 4.5, 1.5, sin(v) * 4.5, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
        }
        animator.start()
    }

    override fun onResume() {
        Log.d(TAG, "Activity-onResume")
        surfaceView.visibility = android.view.View.VISIBLE
        super.onResume()

        var nibiruVRService : NibiruVRService = NibiruVR.getUsingNibiruVRServiceGL()
        if(nibiruVRService != null)
        {
            nibiruVRService.onResume();
        }

        choreographer.postFrameCallback(frameScheduler)
        animator.start()
    }

    override fun onPause() {
        Log.d(TAG, "Activity-onPause-0")
        surfaceView.visibility = android.view.View.INVISIBLE
        var nibiruVRService : NibiruVRService = NibiruVR.getUsingNibiruVRServiceGL()
        if(nibiruVRService != null)
        {
            nibiruVRService.onPause();
        }
        super.onPause()
        choreographer.removeFrameCallback(frameScheduler)
        animator.cancel()
        Log.d(TAG, "Activity-onPause-1")
    }

    override fun onStop() {
        Log.d(TAG, "Activity-onStop-0")
        super.onStop()
        Log.d(TAG, "Activity-onStop-1")
    }

    override fun onDestroy() {
        Log.d(TAG, "Activity-onDestroy")
        super.onDestroy()

        // Stop the animation and any pending frame
        choreographer.removeFrameCallback(frameScheduler)
        animator.cancel();

        // Always detach the surface before destroying the engine
        uiHelper.detach()

        // This ensures that all the commands we've sent to Filament have
        // been processed before we attempt to destroy anything
        engine.flushAndWait()

        // Cleanup all resources
        destroyMesh(engine, mesh)
        destroyIbl(engine, ibl)
        engine.destroyEntity(light)
        engine.destroyRenderer(renderer)
        engine.destroyMaterialInstance(materialInstance)
        engine.destroyMaterial(material)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCamera(camera)

        // Engine.destroyEntity() destroys Filament related resources only
        // (components), not the entity itself
        val entityManager = EntityManager.get()
        entityManager.destroy(light)

        // Destroying the engine will free up any resource you may have forgotten
        // to destroy, but it's recommended to do the cleanup properly
        engine.destroy()
    }

    inner class FrameCallback : Choreographer.FrameCallback {
        // FPS Counters
        private var requestedFrames = 0
        private var renderedFrames = 0
        private var lastReportTimeNanos = 0L

        override fun doFrame(frameTimeNanos: Long) {
            // Schedule the next frame
            choreographer.postFrameCallback(this)

            // This check guarantees that we have a swap chain
            if (uiHelper.isReadyToRender) {
                // If beginFrame() returns false you should skip the frame
                // This means you are sending frames too quickly to the GPU
                if (renderer.beginFrame(swapChain!!)) {
                    //0_[2.222222, 0.0, 0.0, 0.0, 0.0, 2.24, 0.0, 0.0, -0.03968259, 0.0, -1.0001401, -1.0, 0.0, 0.0, -0.1400098, 0.0]
                    //1_[2.222222, 0.0, 0.0, 0.0, 0.0, 2.24, 0.0, 0.0, 0.03968259, 0.0, -1.0001401, -1.0, 0.0, 0.0, -0.1400098, 0.0]
                    //val rightEyeProjection: DoubleArray = doubleArrayOf(2.222222, 0.0, 0.0, 0.0, 0.0, 2.24, 0.0, 0.0,
                    //        0.03968259, 0.0, -1.0001401, -1.0, 0.0, 0.0, -0.1400098, 0.0)
                    //val leftEyeProjection: DoubleArray = doubleArrayOf(2.222222, 0.0, 0.0, 0.0, 0.0, 2.24, 0.0, 0.0,
                    //        -0.03968259, 0.0, -1.0001401, -1.0, 0.0, 0.0, -0.1400098, 0.0)

                    // left eye
                    //view.viewport = Viewport(0, 0, 1, 1)
                    //camera.setCustomProjection(leftEyeProjection, 0.01 , 100.0);
                    renderer.render(view)

                    // right eye
                    //view.viewport = Viewport(1, 0, 1, 1)
                    //camera.setCustomProjection(rightEyeProjection, 0.01 , 100.0);
                    renderer.render(view)
                    renderer.endFrame()
                    ++renderedFrames
                }
            }

            ++requestedFrames
            // Report frame rate
            if(frameTimeNanos - lastReportTimeNanos > 1_000_000_000) {
                Log.i(TAG, "FPS: $renderedFrames / $requestedFrames")
                lastReportTimeNanos = frameTimeNanos
                requestedFrames = 0
                renderedFrames = 0
            }
        }
    }

    inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
            swapChain?.let { engine.destroySwapChain(it) }
            swapChain = engine.createSwapChain(surface)
        }

        override fun onDetachedFromSurface() {
            swapChain?.let {
                engine.destroySwapChain(it)
                // Required to ensure we don't return before Filament is done executing the
                // destroySwapChain command, otherwise Android might destroy the Surface
                // too early
                engine.flushAndWait()
                swapChain = null
            }
        }

        override fun onResized(width: Int, height: Int) {
            val aspect = width.toDouble() / height.toDouble()
            camera.setProjection(45.0, aspect, 0.1, 20.0, Camera.Fov.VERTICAL)

            view.viewport = Viewport(0, 0, width, height)
        }
    }

    private fun readUncompressedAsset(assetName: String): ByteBuffer {
        assets.openFd(assetName).use { fd ->
            val input = fd.createInputStream()
            val dst = ByteBuffer.allocate(fd.length.toInt())

            val src = Channels.newChannel(input)
            src.read(dst)
            src.close()

            return dst.apply { rewind() }
        }
    }
}
