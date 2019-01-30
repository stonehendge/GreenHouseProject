# GreenHouseProject
pump greenhouse by sending SMS command to remote arduino device with gsm module (sim800l)

===============
Arduino Uno3
+ sim800l gsm module 
+ relay module for pumping barrel and water tap
+ DC-DC downstep voltage (arduino've got 1 amp on output, so converted to 2 amp)
+ 2 water level switches. Set them in barrel for indication of low and overfull water levels.
+ soil moisture sensor
+ dht11 temperature sensor
In sketch all pins subscribed and can be changed for your needs.

===============
Android programm with russian interface, please change)
buttons for:
-start watering
-stop watering
-get sensor information
-stop pumping machine (in emergency cases. after that you need manual reboot arduino or change this behavior for your needs)
-settings (save mobile phone number for arduino gsm module)
