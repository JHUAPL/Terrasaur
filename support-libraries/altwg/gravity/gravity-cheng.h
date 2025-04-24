#ifndef GRAVITY_CHENG_H
#define GRAVITY_CHENG_H

#include "platemodel.h"

Platemodel* initializeGravityCheng(const char* filename);
double getGravityCheng(const double fieldPoint[3], double acc[3]);


#endif // GRAVITY_CHENG_H
