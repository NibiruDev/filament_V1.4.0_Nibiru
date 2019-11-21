# Google Filament
https://github.com/google/filament
 
# Nibiru XR SDK
We have added support for Nibiru XR SDK based on Filament V1.4.0.
Use this special version of filament, your apks can run on Nibiru XR OS.

**[filament/Nibiru]** directory contains the release files and some sample codes.

Nibiru Official Website:
http://www.inibiru.com/en/index.html


# Filament Dependent Files
filamat-android-release.aar 
filament-android-release.aar

# Nibiru SDK Dependent Files
AndroidManifest.xml 
core-3.3.2.jar 
nibiru_vr_sdk_2_4_4.jar 
qrcode_sdk.jar

MainActivity.kt [Example code file]

# How to Use Nibiru XR SDK ?

### Step 1 Get an VR/AR HMD with Nibiru XR OS:

Nibiru XR HMD: 
AR HMD : http://www.inibiru.com/en/product_ar.html
VR HMD : http://www.inibiru.com/en/product_vr.html
Please contact info@inibiru.com for sales support.

**Tip:** Nibiru OS Version need greater than **Nibiru 3.80.0**.

### Step 2 Refer to NibiruLibs/AndroidManifest.xml, change your xml file: 
###### change 
```android:theme="@android:style/Theme.NoTitleBar.Fullscreen"```in application tag
###### change 
```android:screenOrientation="landscape"``` in application tag
###### add 
```<category android:name="com.nibiru.intent.category.NVR"/>```in intent-filter tag

### Step 3 Call below api to init nibiru sdk: 
```
NibiruVR.initNibiruVRServiceForUnreal(activity) 
servicePtr = initParams.split("_").get(0).toLong()
```
#### Use 
```
Engine.create(servicePtr)
``` 
#### Replace  
```
Engine.create()
```

#### Add follow code in resume method 
```
resume() {...} 
surfaceView.visibility = android.view.View.VISIBLE var nibiruVRService : NibiruVRService = NibiruVR.getUsingNibiruVRServiceGL() 
if(nibiruVRService != null) { nibiruVRService.onResume(); }
```

#### Add follow code in pause method 
```
pause() {...} 
var nibiruVRService : NibiruVRService = NibiruVR.getUsingNibiruVRServiceGL() 
if(nibiruVRService != null) { nibiruVRService.onPause(); }
```
#### Add one more call
```
renderer.render(view) 
```
#### Between 
```
renderer.beginFrame -> renderer.endFrame 
```
#### As follow code 
```
if (renderer.beginFrame(swapChain!!)) 
{ 
// left eye 
renderer.render(view)
// right eye 
renderer.render(view)
renderer.endFrame()
++renderedFrames
}
```

# [Tip]

If you want to compile the filament, you can refer to the offical wiki.
https://github.com/google/filament


**[FileList Changed By Nibiru]**
* android/filament-android/src/main/cpp/Engine.cpp 
* android/filament-android/src/main/java/com/google/android/filament/Engine.java  
* filament/backend/CMakeLists.txt filament/backend/include/backend/Platform.h
* filament/backend/include/private/backend/DriverAPI.inc
* filament/backend/src/opengl/OpenGLDriver.cpp
* filament/backend/src/opengl/OpenGLDriver.h
* filament/backend/src/opengl/OpenGLDriverFactory.h
* filament/backend/src/opengl/PlatformEGL.cpp
* filament/backend/src/opengl/OpenGLDriver.h
* filament/backend/src/vulkan/VulkanDriver.cpp filament/include/filament/Engine.h
* filament/src/Engine.cpp filament/src/Renderer.cpp filament/src/details/Engine.h
* filament/src/details/Renderer.h filament/src/fg/FrameGraph.cpp 

**[FileList Changed By Nibiru]**

[![](http://www.inibiru.com/en/img/public/logo.png)](http://www.inibiru.com/en/index.html)

http://www.inibiru.com/en/index.html
