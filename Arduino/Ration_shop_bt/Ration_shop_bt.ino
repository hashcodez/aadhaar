#include <SoftwareSerial.h>
#include <HX711.h>

#define Calib_factor 24.25

SoftwareSerial BTserial(10, 11); // RX | TX
HX711 scale;
//int senval;
float weight;
int state;
int flag=0;  

void setup() 
{
 BTserial.begin(9600);
 Serial.begin(9600);
 scale.begin(A1,A2);
  scale.set_scale();
  scale.tare();
}


void loop() {
    if(BTserial.available()>0){     
      state = BTserial.read(); 
      Serial.println("bt available");  
      Serial.println(state);
      flag=0;
    if (state == 1){
      scale.set_scale(Calib_factor);//Adjust to this calibration factor
      weight= scale.get_units(10);
      BTserial.println(weight);
      Serial.println(weight); 
      }
  }   

      if(flag == 0){
       flag=1;
         }
        

    weight=0;

    BTserial.flush();
}


