# Text And Drive Code

All the code need it for reproducing the experiments demonstrated in the video is in this repository. 

# Directory Structure
```
TextAndDrive/
  +- Android Launcher - Kiss/   --  the android studio project to compile the android launcher.
  |
  +- Arduino/
  |  +- capacitative_sensor.ino -- Arduino algorithm for detection number of fingers on the back of the phone.
  |
  +- Azure/
  |  +- Serverless Function 1/  -- c# code for the first azure serverless function (D2C to .jpg).
  |  +- Serverless Function 2/  -- python code for the second azure serverless function (Tensorflow).
  |  +- Serverless Function 3/  -- c# code for the last azure serverless function (MongoDB update and C2D call).
  |
  +- Web Analytics/		-- Azure Web App for displaying statistics.
  |
  +- Android APK/
  |  +- TextAndDrive.apk	-- Android APK to install on the phone.
  |
  +- README.md -- this file.
```

