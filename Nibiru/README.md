FilamentAAR [Filament aar files]
    ---filamat-android-release.aar
	---filament-android-release.aar
	
NibiruLibs [Nibiru SDK libs]
    ---AndroidManifest.xml
	---core-3.3.2.jar
	---nibiru_vr_sdk_2_4_4.jar
	---qrcode_sdk.jar
	
MainActivity.kt [Example code file]	

How to Use Nibiru VR SDK ?

Step 1
Refer to NibiruLibs/AndroidManifest.xml, change your project AndroidManifest.xml.
-- change android:theme to [@android:style/Theme.NoTitleBar.Fullscreen]
-- add [android:screenOrientation="landscape"] in <activity>
-- add <category android:name="com.nibiru.intent.category.NVR" /> in <intent-filter>

Step 2
-- Call below api to init nibiru sdk:
NibiruVR.initNibiruVRServiceForUnreal(activity)
servicePtr = initParams.split("_").get(0).toLong()

-- Use Engine.create(servicePtr) replace Engine.create()

-- add follow code in fun resume() {...}
surfaceView.visibility = android.view.View.VISIBLE
var nibiruVRService : NibiruVRService = NibiruVR.getUsingNibiruVRServiceGL()
if(nibiruVRService != null)
{
    nibiruVRService.onResume();
}

-- add follow code in fun pause()  {...}
var nibiruVRService : NibiruVRService = NibiruVR.getUsingNibiruVRServiceGL()
if(nibiruVRService != null)
{
    nibiruVRService.onPause();
}

-- add one more call renderer.render(view) between renderer.beginFrame to renderer.endFrame
as follow code show
if (renderer.beginFrame(swapChain!!)) {
    // left eye
    renderer.render(view)
    // right eye
    renderer.render(view)

    renderer.endFrame()
    ++renderedFrames
}

[Tip]

If you want to compile the filament, you can refer to the offical wiki.

[Powered by Nibiru]
http://www.inibiru.com/en/index.html

[FileList Changed By Nibiru]
android/filament-android/src/main/cpp/Engine.cpp
android/filament-android/src/main/java/com/google/android/filament/Engine.java
filament/backend/CMakeLists.txt
filament/backend/include/backend/Platform.h
filament/backend/include/private/backend/DriverAPI.inc
filament/backend/src/opengl/OpenGLDriver.cpp
filament/backend/src/opengl/OpenGLDriver.h
filament/backend/src/opengl/OpenGLDriverFactory.h
filament/backend/src/opengl/PlatformEGL.cpp
filament/backend/src/opengl/OpenGLDriver.h
filament/backend/src/vulkan/VulkanDriver.cpp
filament/include/filament/Engine.h
filament/src/Engine.cpp
filament/src/Renderer.cpp
filament/src/details/Engine.h
filament/src/details/Renderer.h
filament/src/fg/FrameGraph.cpp
[FileList Changed By Nibiru]












