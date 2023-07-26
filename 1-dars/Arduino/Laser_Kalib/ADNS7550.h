/*
 ADNS7550.h - Part of optical mouse sensor library for Arduino
 Copyright (c) 2012 Daniel K�rner.  All right reserved.
 Based on code by Martijn The. (http://www.martijnthe.nl/)
 Mohammad Kouchekzadeh (mohammad3d@yahoo.com)
 Based on sketches by Beno�t Rousseau.
 
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

#ifndef ADNS7550_h
#define ADNS7550_h

#if defined(ARDUINO) && ARDUINO >= 100
#include "Arduino.h"
#else
#include "WProgram.h"
#endif

#include <inttypes.h>
//#include "OptiMouse.h"

class ADNS7550
{
  private:
  
  protected:
	uint8_t _ncsPin;
	uint8_t _sclkPin;
    	uint8_t _sdiPin;
	uint8_t _sdoPin;
	uint8_t readRegister(uint8_t);
	
	void writeRegister(uint8_t, uint8_t);
	uint8_t readdataloop();
	void readburstregister(uint8_t address);

	
	public:
	ADNS7550(uint8_t, uint8_t, uint8_t,uint8_t);
	
	int get_type(void);
	int get_px_count(void);
	
    void begin(void);
	
	signed int temp;
	void dxdy(signed int *adr_dx, signed int *adr_dy);
	signed int squal(void);
	
	int16_t dx_var;
	int16_t dy_var;
	uint8_t motion;
	uint8_t squal_var;
	uint8_t shutter_upper;
	uint8_t shutter_lower;
	uint8_t maximum_pixel;
	
	void pdata(int *pdata_a);
	void forced_awake(int fstatus);
	float new_cur(int perc);
	
	void reset();
	bool inmotion();
};

#endif

