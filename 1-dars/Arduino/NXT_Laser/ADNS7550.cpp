/*
 ADNS7550.cpp - Part of optical mouse sensor library for Arduino
 Copyright (c) 2012 Daniel Körner.  All right reserved.
 Based on code by Martijn The. (http://www.martijnthe.nl/)
 Mohammad Kouchekzadeh (mohammad3d@yahoo.com)
 Based on sketches by Benoît Rousseau.
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

/******************************************************************************
 * Includes
 ******************************************************************************/

//#include "OptiMouse.h"
#include "ADNS7550.h"


/******************************************************************************
 * Definitions
 ******************************************************************************/
//Mouse-register
#define Delta_Y			0x04
#define Delta_X			0x03
#define Delta_XY		0x05	
#define SQUAL_r			0x06
#define pdata_r			0x35
#define motion_burst	0x42
#define motion_r		0x02

//px-count of the Mouse-chip
#define px_count		676

//Debug
#define debug			0


/******************************************************************************
 * Constructors
 ******************************************************************************/


ADNS7550::ADNS7550(uint8_t sclkPin, uint8_t miso,uint8_t mosi,uint8_t ncsPin)
{
  _ncsPin = ncsPin;
  _sclkPin = sclkPin;
  _sdiPin = miso;
  _sdoPin = mosi;
  pinMode (_ncsPin, OUTPUT);
  pinMode (_sclkPin, OUTPUT);
  pinMode (_sdiPin, INPUT);
  pinMode (_sdoPin, OUTPUT);
  
  digitalWrite (_ncsPin, HIGH);		//nur beim transfer aktive(LOW)
  
  
}

/******************************************************************************
 * User API
 ******************************************************************************/
int ADNS7550::get_type(void)
{
	return 2;
}
int ADNS7550::get_px_count(void)
{
	return px_count;
}
 
 void ADNS7550::begin(void)
{
	unsigned int data[250];
	unsigned int mot=0;
	int i =0;
	int z =0;		//abbruchzaehler
	delayMicroseconds(100);
	/////////// Start ADNS-7550 power up sequence /////////////////

	///// Drive NCS high, then low to reset the SPI port //////////
	digitalWrite(_ncsPin, HIGH);

	delay(1);
	digitalWrite(_ncsPin, LOW);
	delay(1);
	digitalWrite(_ncsPin, HIGH);

	delay(1);
	digitalWrite(_ncsPin, LOW);
	
	//Note:in write operation The first byte contains the address (seven bits) and
	//     has a “1” as its MSB to indicate data direction : 0x3A->0xBA
	//Note:in read operation address MSB bit is '0': 0x3A->0x3A

	//Write 0x5a to register 0x3a
	writeRegister(0x3A,0x5A);
	
	delay(25);

	if(debug) {
		Serial.println("0x00:");
		Serial.println(readRegister(0x00));
	}
	
	//Wait for at least one frame
	delayMicroseconds(1350);//wait
	//Clear observation register
	writeRegister(0x2E,0x00);
	//Wait for at least one frame
	delay(200);
	//Wait at least one frame and check observation
	//register, all bits 0-3 must be set.

	data[i++]=readRegister(0x2E);
	if(debug) {
		Serial.println("observation");
		Serial.println(data[i-1]);
	}
	
	//Write 0x27 to register 0x3C
	writeRegister(0x3C,0x27);
	//Write 0x0a to register 0x22
	writeRegister(0x22,0x10);
	//Write 0x01 to register 0x21
	//	writeRegister(0x21,0x01);
	//Write 0x32 to register 0x3C
	writeRegister(0x3C,0x22);
	//Write 0x20 to register 0x23
	writeRegister(0x3D,0x32);
	
	//Read from registers 0x02, 0x03, 0x04 and 0x05 (or
	//read these same 4 bytes from burst motion register
	//0x42) one time regardless of the motion pin state
	data[i++]=readRegister(0x02);
	data[i++]=readRegister(0x03);
	data[i++]=readRegister(0x04);
	data[i++]=readRegister(0x05);
	if(debug) {
		Serial.println("test:");
		Serial.println(data[i-1]);
		Serial.println(data[i-2]);
		Serial.println(data[i-3]);
		Serial.println(data[i-4]);
	}
	
	// Set LASER_CTRL0 register
	writeRegister(0x1A,0x40);
	// Set LASER_CTRL1 register (complement of LASER_CTRL0)
	writeRegister(0x1F,0xBF);

	// Set LSRPWR_CFG0 register
	writeRegister(0x1C,0x5F);
	// Set LSRPWR_CFG1 register (complement of LSRPWR_CFG0)
	writeRegister(0x1D,0xA0);
	
	data[i++]=readRegister(0x00);
	if(debug) {
		Serial.println("0x00:");
		Serial.println(data[i-1]);
	}
	
	//	data[i++] = readRegister(0x02);
	//	Serial.println("0x02(motion):");
	//	Serial.println(data[i-1]);
	
	// write dummy data
	writeRegister(0x34,0x23);
	
	// Set Configuration2_Bits (5. & 6.) register
	// 00 = 400
	// 01 = 800
	// 10 = 1200
	// 11 = 1600
	writeRegister(0x12,0x26); //0x66

	//
	//checklooop if laser is online
	mot = readRegister(0x02);
	if(debug) {
		Serial.println("0x02(motion):");
		Serial.println(mot);
	}
	while((mot & (0x08) )==0){		//when observationregister has the bit set
		data[i++]=readRegister(0x00);
		mot = readRegister(0x02);
		if(debug) {
			Serial.println("0x00:");
			Serial.println(data[i-1]);
			Serial.println("0x02(motion):");
			Serial.println(mot);
		}
		if(z>20){
			break;
		}
		delayMicroseconds(50);
		z=z+1;
	}
	delayMicroseconds(50);
	
	//Revision_ID
	data[i++]=readRegister(0x01);
	if(debug) {
		Serial.println("Revision_ID:");
		Serial.println(data[i-1]);
	}
	
	// Read Inverse_Revision_ID register(Must be 0xfc)
	data[i++]=readRegister(0x3E);
	if(debug) {
		Serial.println("Inverse_Revision_ID:");
		Serial.println(data[i-1]);
	}
	
	// // Read Product_ID register(Must be 0xcd)
	//
	data[i++]=readRegister(0x00);
	if(debug) {
		Serial.println("Product_ID:");
		Serial.println(data[i-1]);
	}
	
	// Read Inverse_Product_ID register(Must be 0xfc)
	data[i++]=readRegister(0x3F);
	if(debug) {
		Serial.println("Inverse_Product_ID:");
		Serial.println(data[i-1]);
	}
	
	//motionregister
	data[i++]=readRegister(0x02);
	if(debug) {
		Serial.println("motionregister:");
		Serial.println(data[i-1]);
	}
	
	//selftest:
	// writeRegister(0x10,0x01);
	// delay(300);
	// data[i++]=readRegister(0x0C);
	// data[i++]=readRegister(0x0D);
	// data[i++]=readRegister(0x0E);
	// data[i++]=readRegister(0x0F);

	digitalWrite(_ncsPin,HIGH);
	Serial.println("Initialisierung ok");
	return;
	//Serial.print("i=");
	//Serial.println(i);

}

void ADNS7550::dxdy(signed int *adr_dx, signed int *adr_dy)
{
	digitalWrite (_ncsPin, LOW);
	signed int tempx  = readRegister(Delta_X) << 4;
	signed int tempy  = readRegister(Delta_Y) << 4;
	signed int tempxy = readRegister(Delta_XY) << 8;
	
	tempx = (tempx | (tempxy & 0xF000));
	tempxy = tempxy << 4;
	tempy = (tempy | (tempxy & 0xF000));

	digitalWrite (_ncsPin, HIGH);
	// 2's complement correction, if negative
	tempx = (signed int) tempx >>4;
	tempy = (signed int) tempy >>4;

	*adr_dx = tempx;
	*adr_dy = tempy;
	return;
}
signed int ADNS7550::squal(void)	//Surface QUALity auslesen
{
	digitalWrite (_ncsPin, LOW);
	temp = (unsigned char) readRegister(SQUAL_r);
	digitalWrite (_ncsPin, HIGH);
	return (signed int)temp;
}

int px_counter=0;
int px_counter_old;

//liest ein kompletten Frame aus
void ADNS7550::pdata(int *pdata_a)	
{
	uint8_t readd;
	
	//pixelregister resetten (zum auslesen beim ersten pixel)
	if(px_counter==0){
		digitalWrite (_ncsPin, LOW);
		writeRegister(pdata_r,0x00);
		digitalWrite (_ncsPin, HIGH);
		delayMicroseconds(10);
		digitalWrite (_ncsPin, LOW);
		readd = readRegister(motion_r);
		digitalWrite (_ncsPin, HIGH);
		
		if((readd & 0x20)!=0x20){Serial.println(readd);return;}	//wenn kein startpixel available
	}
	px_counter_old=px_counter;
	while(px_counter!=px_counter_old+1)
	{
		digitalWrite (_ncsPin, LOW);
		readd = readRegister(motion_r);
		digitalWrite (_ncsPin, HIGH);

		if((readd & 0x40)==0x40)		//valid pixeldata?
		{

			digitalWrite (_ncsPin, LOW);
			readd = readRegister(pdata_r);	//pixeladataregister auslesen

			pdata_a[px_counter] = (readd); //ohne MSB auslesen
			px_counter++;
			if(px_counter==px_count){px_counter=0;}	//alle pixel eines frames ausgelesen
			digitalWrite (_ncsPin, HIGH);
		}
	}
	return;
}
void ADNS7550::forced_awake(int fstatus)
{
	return;
	if(fstatus==1){			//wenn die MausLED immer wach bleiben soll
		writeRegister(0x1A,0xCA);
		return;
	}
	if(fstatus==0){			//Sleepmode der Maus wieder aktivieren
		writeRegister(0x1A,0xC0);
		delayMicroseconds(1000);
	}

}

//Funktion zum verstellen des Laserdiodenstroms (perc - wie viel prozent dazu in 8-bit einheiten
float ADNS7550::new_cur(int perc)
{	
	float curr;
	uint8_t temp2;
	digitalWrite (_ncsPin, LOW);
	temp2 = readRegister(0x1C);
	digitalWrite (_ncsPin, HIGH);
	
	temp2 += perc;
	
	digitalWrite (_ncsPin, LOW);
	writeRegister(0x1C,temp2);
	digitalWrite (_ncsPin, HIGH);
	
	digitalWrite (_ncsPin, LOW);
	temp= (int)temp2;
	writeRegister(0x1D,~temp2);
	digitalWrite (_ncsPin, HIGH);	
	
	//umrechung:
	//standardt: 2-5 mA range
	curr = (0.3359+0.0026*temp)*5;
	
	return curr;
}

void ADNS7550::reset()		//maus zuruecksetzen
{
	//begin();
	// digitalWrite (_ncsPin, LOW);
	// writeRegister(0x3a,0x5a);
	// digitalWrite (_ncsPin, HIGH);
	// delay(250);
	
	 // Set 1000cpi resolution
	// digitalWrite(_ncsPin, LOW);
	// writeRegister(0x0d,0x01);
	// digitalWrite(_ncsPin, HIGH);
}

bool ADNS7550::inmotion()		//aktuell eine Bewegung aktiv?
{

	digitalWrite (_ncsPin, LOW);

	temp = readRegister(motion_r);
	motion = (temp & 0x80) >> 7;
	digitalWrite (_ncsPin, HIGH);
	return motion;
	
	//debugging
	Serial.print(" motion: ");
	Serial.print(temp);

	
	//read burstregister
	digitalWrite (_ncsPin, LOW);
	readburstregister(motion_burst);
	digitalWrite (_ncsPin, HIGH);
	Serial.println("all");
	
	// Serial.print(" mot");
	
	Serial.println(motion);
	// Serial.print(" dx");
	Serial.println(dx_var);
	// Serial.print(" dy");
	Serial.println(dy_var);
	
	// Serial.print("squal");
	Serial.println(squal_var);

	// Serial.print(" shutter_upper: ");
	Serial.println(shutter_upper);

	// Serial.print(" shutter_lower: ");
	Serial.println(shutter_lower);

	// Serial.print(" maximum_pixel: ");
	Serial.println(maximum_pixel);

	Serial.print(" ");

	return motion;
}

// Private Methods /////////////////////////////////////////////////////////////
uint8_t ADNS7550::readdataloop()
{
	uint8_t r = 0;
	// Fetch the data!
	for (int i=7; i>=0; i--)
	{                             
		digitalWrite (_sclkPin, LOW);
		// delayMicroseconds(100);
		digitalWrite (_sclkPin, HIGH);
		// delayMicroseconds(30);
		r |= (digitalRead (_sdiPin) << i);
		// delayMicroseconds(70);
	}
	return r;
}


void ADNS7550::readburstregister(uint8_t address)
{
	int i = 7;
	uint8_t t1 = 0;
	uint8_t t2 = 0;
	uint8_t t3 = 0;	
	delayMicroseconds(300);
	// Write the address of the register we want to read:
	pinMode (_sdoPin, OUTPUT);
	for (; i>=0; i--)
	{
		digitalWrite (_sclkPin, LOW);
		// delayMicroseconds(70);
		digitalWrite (_sdoPin, address & (1 << i));
		// delayMicroseconds(30);
		digitalWrite (_sclkPin, HIGH);
		// delayMicroseconds(10);
	}
	
	pinMode (_sdiPin, INPUT);
	
	// Wait a bit...
	delayMicroseconds(100);
	motion=readdataloop();
	t1 = readdataloop();
	t2 = readdataloop();
	t3 = readdataloop();
	squal_var = readdataloop();
	shutter_upper = readdataloop();
	shutter_lower = readdataloop();
	maximum_pixel = readdataloop();
	dx_var =  ((int16_t)((t3 & 0xF0)<<4) | t1);
	dy_var = (int16_t) (((t3 & 0x0F)<<8) | t2);
	
	delayMicroseconds(100);

}


uint8_t ADNS7550::readRegister(uint8_t address)
{
	int i = 7;
	uint8_t r = 0;
	delayMicroseconds(300);
	// Write the address of the register we want to read:
	//pinMode (_sdoPin, OUTPUT);
	for (; i>=0; i--)
	{
		digitalWrite (_sclkPin, LOW);
		// delayMicroseconds(70);
		digitalWrite (_sdoPin, address & (1 << i));
		// delayMicroseconds(30);
		digitalWrite (_sclkPin, HIGH);
		// delayMicroseconds(10);
	}
	
	// Switch data line from OUTPUT to INPUT
	//pinMode (_sdiPin, INPUT);
	
	// Wait a bit...
	delayMicroseconds(100);
	
	r=readdataloop();
	
	//delayMicroseconds(100);
	
	return r;
}

void ADNS7550::writeRegister(uint8_t address, uint8_t data)
{
	int i = 7;
	
	// Set MSB high, to indicate write operation:
	address |= 0x80;
	delayMicroseconds(300);
	// Write the address:
	//pinMode (_sdoPin, OUTPUT);
	for (; i>=0; i--)
	{
		digitalWrite (_sclkPin, LOW);
		delayMicroseconds(70);
		digitalWrite (_sdoPin, address & (1 << i));
		delayMicroseconds(30);
		digitalWrite (_sclkPin, HIGH);
		delayMicroseconds(100);
	}
	delayMicroseconds(300);
	// Write the data:
	for (i=7; i>=0; i--)
	{
		digitalWrite (_sclkPin, LOW);
		delayMicroseconds(70);
		digitalWrite (_sdoPin, data & (1 << i));
		delayMicroseconds(30);
		digitalWrite (_sclkPin, HIGH);
		delayMicroseconds(100);
	}
}
