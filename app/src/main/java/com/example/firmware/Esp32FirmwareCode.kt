package com.example.firmware

object Esp32FirmwareCode {
    const val SERVICE_UUID = "4fa8691a-136e-45a0-9749-09d0070b0356"
    const val RX_CHAR_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
    const val TX_CHAR_UUID = "8ec94015-6949-4c85-949f-54138b37dac7"

    val ARDUINO_SKETCH = """
/*
 * ESP32 AI Hub Firmware for ESP32-S3
 * Compatible with ESP32 Arduino Core 2.x / 3.x
 *
 * Hardware Pin Connections:
 * - DHT11 Temp/Humidity Sensor: GPIO 4
 * - LDR Light Sensor (Analog):  GPIO 1 (ADC1)
 * - IR/PIR Motion Sensor:       GPIO 5
 * - Relay 1 (Light Control):   GPIO 6
 * - Relay 2 (Fan Control):     GPIO 7
 * - Battery Voltage Divider:   GPIO 2 (ADC1)
 */

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <ArduinoJson.h>
#include <DHT.h>

#define DEVICE_NAME "ESP32-S3-AI-HUB"
#define SERVICE_UUID        "$SERVICE_UUID"
#define CHARACTERISTIC_UUID_RX "$RX_CHAR_UUID"
#define CHARACTERISTIC_UUID_TX "$TX_CHAR_UUID"

#define DHTPIN 4
#define DHTTYPE DHT11
#define LDR_PIN 1
#define PIR_PIN 5
#define LIGHT_RELAY_PIN 6
#define FAN_RELAY_PIN 7
#define BATTERY_PIN 2

DHT dht(DHTPIN, DHTTYPE);

BLEServer* pServer = NULL;
BLECharacteristic* pTxCharacteristic = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;

// Hardware State
float temperature = 25.0;
float humidity = 50.0;
int ldrValue = 500;
bool motionDetected = false;
bool lightState = false;
bool fanState = false;
bool autoMode = true;
bool aiControlMode = false;
int batteryPercent = 100;

unsigned long lastSensorRead = 0;
const long sensorInterval = 2000; // Read sensors every 2s

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("App Connected!");
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println("App Disconnected!");
    }
};

class CommandCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue();
      if (rxValue.length() > 0) {
        Serial.print("Received Command: ");
        Serial.println(rxValue.c_str());

        StaticJsonDocument<200> doc;
        DeserializationError error = deserializeJson(doc, rxValue.c_str());

        if (!error) {
          const char* cmd = doc["command"];
          if (cmd) {
            if (strcmp(cmd, "LIGHT_ON") == 0) {
              lightState = true;
              digitalWrite(LIGHT_RELAY_PIN, HIGH);
            } else if (strcmp(cmd, "LIGHT_OFF") == 0) {
              lightState = false;
              digitalWrite(LIGHT_RELAY_PIN, LOW);
            } else if (strcmp(cmd, "FAN_ON") == 0) {
              fanState = true;
              digitalWrite(FAN_RELAY_PIN, HIGH);
            } else if (strcmp(cmd, "FAN_OFF") == 0) {
              fanState = false;
              digitalWrite(FAN_RELAY_PIN, LOW);
            } else if (strcmp(cmd, "AUTO_MODE_ON") == 0) {
              autoMode = true;
            } else if (strcmp(cmd, "AUTO_MODE_OFF") == 0) {
              autoMode = false;
            } else if (strcmp(cmd, "AI_MODE_ON") == 0) {
              aiControlMode = true;
            } else if (strcmp(cmd, "AI_MODE_OFF") == 0) {
              aiControlMode = false;
            }
          }
        }
      }
    }
};

void sendSensorData() {
  if (!deviceConnected) return;

  StaticJsonDocument<256> doc;
  doc["temperature"] = temperature;
  doc["humidity"] = humidity;
  doc["ldr"] = ldrValue;
  doc["motion"] = motionDetected;
  doc["light"] = lightState;
  doc["fan"] = fanState;
  doc["autoMode"] = autoMode;
  doc["aiControl"] = aiControlMode;
  doc["battery"] = batteryPercent;

  char jsonBuffer[256];
  serializeJson(doc, jsonBuffer);

  pTxCharacteristic->setValue(jsonBuffer);
  pTxCharacteristic->notify();
  Serial.print("Sent: ");
  Serial.println(jsonBuffer);
}

void setup() {
  Serial.begin(115200);

  pinMode(LIGHT_RELAY_PIN, OUTPUT);
  pinMode(FAN_RELAY_PIN, OUTPUT);
  pinMode(PIR_PIN, INPUT);
  digitalWrite(LIGHT_RELAY_PIN, LOW);
  digitalWrite(FAN_RELAY_PIN, LOW);

  dht.begin();

  // Create BLE Device
  BLEDevice::init(DEVICE_NAME);

  // Create BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Create TX Characteristic (Notifications)
  pTxCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID_TX,
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
  pTxCharacteristic->addDescriptor(new BLE2902());

  // Create RX Characteristic (Write commands)
  BLECharacteristic *pRxCharacteristic = pService->createCharacteristic(
                                           CHARACTERISTIC_UUID_RX,
                                           BLECharacteristic::PROPERTY_WRITE
                                         );
  pRxCharacteristic->setCallbacks(new CommandCallbacks());

  // Start Service
  pService->start();

  // Start Advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();
  Serial.println("ESP32-S3 BLE AI Hub ready for connections!");
}

void loop() {
  unsigned long currentMillis = millis();

  if (currentMillis - lastSensorRead >= sensorInterval) {
    lastSensorRead = currentMillis;

    // Read Sensors
    float t = dht.readTemperature();
    float h = dht.readHumidity();
    if (!isnan(t)) temperature = t;
    if (!isnan(h)) humidity = h;

    ldrValue = analogRead(LDR_PIN);
    motionDetected = digitalRead(PIR_PIN) == HIGH;

    // Read Battery VCC simulation/divider
    int rawBat = analogRead(BATTERY_PIN);
    batteryPercent = constrain(map(rawBat, 0, 4095, 0, 100), 0, 100);

    // Automatic Logic (if Auto Mode is ON)
    if (autoMode) {
      if (motionDetected && ldrValue > 700) {
        lightState = true;
        digitalWrite(LIGHT_RELAY_PIN, HIGH);
      } else if (!motionDetected && ldrValue < 400) {
        lightState = false;
        digitalWrite(LIGHT_RELAY_PIN, LOW);
      }

      if (temperature > 30.0) {
        fanState = true;
        digitalWrite(FAN_RELAY_PIN, HIGH);
      } else if (temperature < 26.0) {
        fanState = false;
        digitalWrite(FAN_RELAY_PIN, LOW);
      }
    }

    sendSensorData();
  }

  // Handle Disconnection / Advertising restart
  if (!deviceConnected && oldDeviceConnected) {
    delay(500); // give the bluetooth stack the chance to get things ready
    pServer->startAdvertising(); // restart advertising
    Serial.println("Restart advertising");
    oldDeviceConnected = deviceConnected;
  }
  if (deviceConnected && !oldDeviceConnected) {
    oldDeviceConnected = deviceConnected;
  }
}
""".trimIndent()
}
