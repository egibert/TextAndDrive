/*
 Snowforce.ino
 Copyright (c) 2014 Kitronyx http://www.kitronyx.com
 GPL V3.0
*/

#include <Wire.h>
#include <Snowforce.h>

Snowforce snowforce;

void setup()
{
    Wire.begin();   // start i2c communication
    Serial.begin(9600);  // start serial for output
    snowforce.begin(); // start tactile sensing part of snowboard
}

void loop()
{
    // pressure mapping data
    // tactile sensing part always give maximum
    // number of data (10x16 = 160)
    byte data[160] = {0,}; 
    byte points[160] = {0,}; 
    int count = 0;

    // send data only pc wants it
    //if (Serial.available() > 0)
    //{        
        snowforce.read(data);
        snowforce.read(points);

        for(int i = 0; i < 16; i++)
        {
          for(int j = 0; j < 10; j++) 
          {
            if(j != 15 && i != 9)
            {

              if(points[j+(10*i)] > 20) {
                points[(j+(10*i)) + 1] = 0;
                points[(j+(10*i)) + 10] = 0;
                points[(j+(10*i)) + 11] = 0;
                count ++;
              }
            }
          }
        }


        //for (int i = 0; i < 160; i++)
        //{
        //    Serial.print(data[i]);
        //    Serial.print(",");
        //}
        Serial.println(count);
    //}
}
