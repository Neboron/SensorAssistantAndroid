


# VRC Tracker Assistant
<p align="center">
   <a href="https://www.youtube.com/@norobe"><img alt="Youtube" src="https://img.shields.io/badge/YouTube-%23FF0000.svg?style=for-the-badge&logo=YouTube&logoColor=white"></a>
<a href="https://discord.gg/VWFDmSh4"><img alt="Discord" src="https://img.shields.io/badge/Discord-%235865F2.svg?style=for-the-badge&logo=discord&logoColor=white"></a>
  
Android application for connecting EEG sensors or Full-body tracking to VRChat.

## Features
- :brain: Connect the NeuroSky brain-computer interface (BCI) to your VRChat avatar. Utilize your attention and meditation levels, along with advanced filtering and a system of triggers, to perform actions such as ear movements and eye blinking.
- :gear: Easily locate the necessary parameters and their OSC addresses for debugging and development. This feature facilitates the connection of the BCI to your avatar without needing to use Unity or make any avatar modifications.
- :man_dancing: Connect IMU trackers to your phone via BLE and forward the data to the SlimeVR Server running on your PC for full-body tracking. This supports AitaVRT trackers (currently in development).
  

## Currently in develoment
- KAT and OSC message feeder, only text form (Voice requires Virtual cable driver and additional application running on your PC).
- Use your phone as an additional SlimeVR tracker. The IMU in your phone can be used for attitude estimation, with this data being sent to the SlimeVR Server. You can choose to use it as a single hip tracker or as an additional tracker to complement your SlimeVR trackers (coming soon).
- Support of ESP clasic Bluetooth DIY version.
- Haptic feedback system (Ears).
- Performance optimization for AitaVRT devices.
- Port for Quest headsets.
- More milestones...


## Get started (NeuroSky EEG)
1. Application in its early testing phase, it is recommended to download it and try test input sources (in the Settings menu), before assembling devices.
2. Find or build appropriate hardware if you are planning to use BCI or full-body trackers. Find more information and assembly guides on my hackaday page: NeuroSky EEG for VRChat (Will be added soon), AitaVRT Full-Body Trackers.
3. Change VRChat launch configs. As we are using Android phone to receive and transmit OSC parameters, it is not longer “_localhost” and VRChat should know the destination IP. This lauch option can be added to in Steam VRChat launch options:

```bash
--osc=inPort:outIP:outPort
```

for example:
```bash
--osc=9000:9000:192.168.50.3
```

4. Set the IP address of your PC and “outPort” in the settings menu.
5. Set the address of the parameter responsible for what you want to control, you can press the listener button while open your in-game avatar menu and select an action in the menu (for example ear position or in menu emotion).

[![Watch the video](https://img.youtube.com/vi/Y77GcWnNwu0/maxresdefault.jpg)](https://www.youtube.com/watch?v=Y77GcWnNwu0)

You can try to select test input source for OSC stream to check the response. At this stage you should see incrementation of the message count.

[![Watch the video](https://img.youtube.com/vi/VXL8nJ8EH90/maxresdefault.jpg)](https://www.youtube.com/watch?v=VXL8nJ8EH90)

6. Power on the NeuroSky Bluetooth device and pair it with your phone (try standard password: 1234).
7. Press “Run” button in the application, it should appear in the “Device List”.
8. Open Device List and tap on appeared NeuroSky BCI. In the device configuration menu you can change filtering settings, triggers, see telemetry data and so on. Try to train your concentration and meditation (I personaly reecomend to use meditation data for avatar ear control.


## Hardware (NeuroSky EEG)
Theoretically, you can use any NeuroSky BCI to connect it to the SensorAssistant application. For testing, I am using the [TGAM development kit with a LiPo battery](https://www.aliexpress.com/item/1005003264615528.html?spm=a2g0o.productlist.main.13.279f25h725h7ge&algo_pvid=9fcc7c6a-c934-4aaf-b15e-811fb3fb25e6&algo_exp_id=9fcc7c6a-c934-4aaf-b15e-811fb3fb25e6-6&pdp_npi=4%40dis%21EUR%21149.72%2182.35%21%21%21161.91%2189.05%21%40211b441e17251880075954093e5d8a%2112000024946486676%21sea%21EE%210%21ABX&curPageLogUid=n5pUGUBUVrRN&utparam-url=scene%3Asearch%7Cquery_from%3A).

The case for the module was created for the BoboVR strap, but you can modify the scope to fit other straps. [GRABCAD](https://grabcad.com/library/neurosky-tgam-module-case-for-bobo-vr-strap-1)

### Connect your electrodes to your head
NeuroSky modules require two dry electrodes: the active electrode (Fp1) and the reference clip (A1). The Fp1 electrode should be positioned on your forehead, approximately one inch above your right eyebrow (from your perspective) and about an inch to the left of your forehead's centerline.
reference clip connects to your earlobe.

![Brain Sensor Map](https://raw.githubusercontent.com/Neboron/SensorAssistantAndroid/main/demo_media/BrainSensorMap.jpg)

BoboVR S3 mounting arc pad is a good place to put Fp1.

There is a way to save money and assemble it using only the TGAM module and ESP32 in Bluetooth mode, but this setup hasn't been tested yet.



