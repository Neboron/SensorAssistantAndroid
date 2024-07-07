# VRC Tracker Assistant
Android application for connecting EEG sensors or Full-body tracking to VRChat.

## Features
- Connect the NeuroSky brain-computer interface (BCI) to your VRChat avatar. Utilize your attention and meditation levels, along with advanced filtering and a system of triggers, to perform actions such as ear movements and eye blinking.
- Easily locate the necessary parameters and their OSC addresses for debugging and development. This feature facilitates the connection of the BCI to your avatar without needing to use Unity or make any avatar modifications.
- Connect IMU trackers to your phone via BLE and forward the data to the SlimeVR Server running on your PC for full-body tracking. This supports AitaVRT trackers (currently in development).
  

## Currently in develoment
- KAT and OSC message feeder, only text form (Voice requires Virtual cable driver and additional application running on your PC).
- Use your phone as an additional SlimeVR tracker. The IMU in your phone can be used for attitude estimation, with this data being sent to the SlimeVR Server. You can choose to use it as a single hip tracker or as an additional tracker to complement your SlimeVR trackers (coming soon).
- Support of ESP clasic Bluetooth DIY version.
- Haptic feedback system (Ears).
- Performance optimization for AitaVRT devices.
- More milestones...


## Get started (NeuroSky EEG)
1. Application in its early testing phase, it is recommended to download it and try test input sources (in the Settings menu), before assembling devices.
2. Find or build appropriate hardware if you are planning to use BCI or full-body trackers. Find more information and assembly guides on my hackaday page: NeuroSky EEG for VRChat (Will be added soon), AitaVRT Full-Body Trackers.
3. Change VRChat launch configs. As we are using Android phone to receive and transmit OSC parameters, it is not longer “_localhost” and VRChat should know the destination IP. This lauch option can be added to in Steam VRChat launch options:
--osc=inPort:outIP:outPort
for example:
--osc=9000:9000:192.168.50.3
4. Set the IP address of your PC and “outPort” in the settings menu.
5. Set the address of the parameter responsible for what you want to control, you can press the listener button while open your in-game avatar menu and select an action in the menu (for example ear position or in menu emotion). You can try to select test input source for OSC stream to check the response. At this stage you should see incrementation of the message count.
6. Power on the NeuroSky Bluetooth device and pair it with your phone (try standard password: 1234).
7. Press “Run” button in the application, it should appear in the “Device List”.
8. Open Device List and tap on appeared NeuroSky BCI. In the device configuration menu you can change filtering settings, triggers, see telemetry data and so on. Try to train your concentration and meditation (I personaly reecomend to use meditation data for avatar ear control.
9. 
