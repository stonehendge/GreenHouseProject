#include <SoftwareSerial.h>
#include <dht.h>
#include <Wire.h> // библиотека для управления устройствами по I2C 
//#include <TroykaMQ.h>
#include <hd44780.h>
#include <hd44780ioClass/hd44780_I2Cexp.h> // include i/o class header
//#include <DS3231.h>

//DS3231 clock;
//RTCDateTime dt;

//===================================COMMON DECLARATIONS======================================
//LCD DISPLAY
#define cols 16
#define rows 2
#define RELAY_PIN             8   // реле крана полива
#define RELAY_PUMP_PIN        3  // реле насоса для бочки
#define DHT11_PIN             9   // имя для пина, к которому подключен датчик Temp
//#define PIN_MQ135           A0  // CO2
#define SM_PIN                A1  // Soil moisture pin
#define FLOAT_SWITCH_UP_PIN   10  // поплавок верхний pin
#define FLOAT_SWITCH_DOWN_PIN 11  // поплавок нижний pin

hd44780_I2Cexp lcd; // declare lcd object: auto locate & config display for hd44780 chip

dht DHT;
//MQ135 mq135(PIN_MQ135);

SoftwareSerial sim(6, 7);
int _timeout;
int RelayPin;
int RelayPumpPin;
int SensorSoilMoisturePin;
int FloatSwitchUpPin;
int FloatSwitchDownPin;

int MaxLevel = 400; //==soil misture extremes==
int MinLevel = 250;

//status params
int Temperature_value;
int Humidity_value;
int SensorSM_value;
String SM_status;
bool PumpRelay_value;
bool stop_pump_manual=false; //ручное выключение насоса. после отправки смс система не будет качать до перезагрузки

String _buffer;
String number = "+791525231XX"; //-> change with your number
String textMessage;

unsigned long BacklightOffTime = 0;
#define BacklightDelay 20000// Длительность подсветки

//===================================COMMON FUNCTIONS=========================================


void LCDBacklight(byte v) { // Управление подсветкой
  if (v == 0) { // Выключить подсветку
    BacklightOffTime = millis();
    lcd.noBacklight();
  }
  else if (v == 1) { //Включить подсветку
    BacklightOffTime = millis() + BacklightDelay;
    lcd.backlight();
  }
  else { // Выключить если время вышло
    if (BacklightOffTime < millis())
      lcd.noBacklight();
    else
      lcd.backlight();
  }
}

void StatusPage()
{
  // String date;
  // dt = clock.getDateTime();
  // date = String(dt.year) + "-" + String(dt.month) + "-" + String(dt.day) + " " + String(dt.hour) + ":" + String(dt.minute);

  int chk = DHT.read11(DHT11_PIN);
  // В переменной sensVolue хранится аналоговое значение датчика с контакта А0
  SensorSM_value = (int) analogRead(SensorSoilMoisturePin);

  if (SensorSM_value <= MinLevel) {
    //very wet soil
    SM_status = "Wet";
  }
  if (SensorSM_value >= MaxLevel) {
    SM_status = "Dry";
  }
  if (SensorSM_value > MinLevel && SensorSM_value < MaxLevel) {
    SM_status = "Norm";
  }

  Temperature_value = (int)DHT.temperature;
  Humidity_value = (int)DHT.humidity;

  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("T:");
  lcd.print(Temperature_value);
  lcd.print(" H:");
  lcd.print(Humidity_value);
  lcd.setCursor(0, 1);
  lcd.print("SM: ");
  lcd.print(SensorSM_value);
  lcd.print(" " + SM_status);
 
  LCDBacklight(1);
  // delay(15000);
  // LCDBacklight(0);


}

//состояние насоса вкл\выкл
int CheckPumpState() 
{
     int pump_stat = digitalRead(RelayPumpPin);

     return pump_stat;
}

int CheckWaterLevel() {
  int FS_Up = digitalRead(FloatSwitchUpPin);
  int FS_Down = digitalRead(FloatSwitchDownPin);
  int FS_status;
  Serial.println(FS_Up);

  if (FS_Up > 0) {
    FS_status = 1;
    Serial.println("Barrel Full");
  }
  else
  {
    if (FS_Down > 0) {
      FS_status = 3; // pumping in progress
    }
    else {
      FS_status = 2;
    }

    Serial.println("Barrel Not Full");
  }

  //  1 - Full barrel
  //  2 - Low water
  //  3 - Pumping in progress
  return FS_status;
}

void PumpUpBarell() {

  if (CheckWaterLevel() == 2 && !stop_pump_manual) {
    digitalWrite(RelayPumpPin, HIGH);
  }
  if (CheckWaterLevel() == 1) {
    digitalWrite(RelayPumpPin, LOW);
  }

}

//==============

String getValue(String data, char separator, int index)
{
  int found = 0;
  int strIndex[] = {0, -1};
  int maxIndex = data.length() - 1;

  for (int i = 0; i <= maxIndex && found <= index; i++) {
    if (data.charAt(i) == separator || i == maxIndex) {
      found++;
      strIndex[0] = strIndex[1] + 1;
      strIndex[1] = (i == maxIndex) ? i + 1 : i;
    }
  }

  return found > index ? data.substring(strIndex[0], strIndex[1]) : "";
}

//=========================================================================================
//===================================START ARDUINO=========================================
//=========================================================================================

void setup() {
  RelayPin = RELAY_PIN;
  RelayPumpPin = RELAY_PUMP_PIN;
  SensorSoilMoisturePin = SM_PIN;
  FloatSwitchUpPin = FLOAT_SWITCH_UP_PIN;
  FloatSwitchDownPin = FLOAT_SWITCH_DOWN_PIN;

  pinMode(FloatSwitchUpPin, INPUT_PULLUP);
  pinMode(FloatSwitchDownPin, INPUT_PULLUP);
  pinMode(RelayPin, OUTPUT);
  digitalWrite(RelayPin, LOW);
  pinMode(RelayPumpPin, OUTPUT);
  digitalWrite(RelayPumpPin, LOW);

  lcd.init(); // инициализация LCD дисплея
  lcd.begin(16, 2);
  //mq135.calibrate();

  delay(7000); //delay for 7 seconds to make sure the modules get the signal
  Serial.begin(9600);
  _buffer.reserve(50);
  Serial.println("System Started...");
  Serial.println("Relay pin=" + String(RelayPin));

  sim.begin(9600);

  Serial.print("GPRS ready...\r\n");

  sim.print("AT+CMGF=1\r\n");
  delay(1000);
  sim.print("AT+IFC=1, 1\r"); //++28.01.2019
  delay(1000);
  sim.print("AT+CNMI=1,2,2,1,0\r"); //++28.01.2019
  //sim.print("AT+CNMI=2,2,0,0,0\r\n");
  delay(1000);
  sim.print("AT+CMGD=1,4\r");//все удалить
  delay(500);

  //  LCDBacklight(1);
  StatusPage();
  //  delay(15000);
  //  LCDBacklight(0);
  // Serial.println("Type s to send an SMS, r to receive an SMS, and c to make a call");
  //RecieveMessage();
  textMessage = "";
  
}

//====================================LOOP========================================

void loop() {


  PumpUpBarell();
  
  delay(5000);
  //Serial.println("loop\r\n" +String(RelayPin));

  if (sim.available() > 0) {
    textMessage = sim.readString();
    Serial.print(textMessage);
    delay(10);
  }

 //==============================================================================
 //Включить полив
 //==============================================================================

  if (textMessage.indexOf("PumpGreenHouseOn") >= 0) { //If you sent "ON" the lights will turn on
    Serial.println("Lamp set to ON\r\n" + String(RelayPin));
    // Turn on relay and save current state
    digitalWrite(RelayPin, HIGH);

    textMessage = "";
 
    delay(1000);
  }


 //==============================================================================
 //Выключить полив
 //==============================================================================

  if (textMessage.indexOf("PumpGreenHouseOff") >= 0) { //If you sent "ON" the lights will turn on
    // Turn on relay and save current state
    digitalWrite(RelayPin, LOW);

    // digitalWrite(relay, HIGH);
    // lampState = "ON";
    Serial.println("Lamp set to OFF\r\n");
    textMessage = "";
    //    GPRS.println("AT+CMGS=\"+631234567890\""); // RECEIVER: change the phone number here with international code
    //    delay(500);
    //    GPRS.print("Lamp was finally switched ON.\r");
    //    GPRS.write( 0x1a );
    delay(1000);
  }

  //==============================================================================
  //Выключить насос для бочки
  //==============================================================================

  if (textMessage.indexOf("GreenHousePumpOff") >= 0) 
  { 
    // Turn on relay and save current state
    digitalWrite(RelayPumpPin, LOW);

    // digitalWrite(relay, HIGH);
    // lampState = "ON";
    Serial.println("Pump OFF\r\n");
    textMessage = "";
    stop_pump_manual=true;
    delay(1000);
  }
  
  //==============================================================================
  //Запрос статуса датчиков
  //==============================================================================
  
  if (textMessage.indexOf("+CMT")) { //если начинается с CMT то далее следует номер
    number = textMessage.substring(textMessage.indexOf("\""), textMessage.indexOf(",")); // выделяем номер
    Serial.println("TELEPHONE====" + number);
  }

  if (textMessage.indexOf("PumpGreenHouseStatus") >= 0) { //If you sent "ON" the lights will turn on



    StatusPage();
 

    textMessage = "";
    //=======send sms tot callee=========
    //String tel_number = "+791525231XX";

        sim.print("AT+CSCS=\"GSM\"\r\n");
        delay(1000);
        
        sim.println("AT+CMGS=" + number ); 
        //sim.println("AT+CMGS=\"" + number + "\""); // RECEIVER: change the phone number here with international code
        delay(500);
        sim.print("WL:" + (String)CheckWaterLevel() + "," + "PS:" + (String)CheckPumpState() + "," + "T:" + (String)Temperature_value + "," + "H:" + (String)Humidity_value + "," + "SM:" + (String)SensorSM_value + ",\r");
        sim.write( 0x1a );
        delay(1000);
  }

 
}
