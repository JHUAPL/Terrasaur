#ifndef GRAVITY_WERNER_H
#define GRAVITY_WERNER_H

#include "mathutil.h"
#include "platemodel.h"

Platemodel* initializeGravityWerner(const char* filename);
double getGravityWerner(const double fieldPoint[3], double acc[3]);
bool isInsidePolyhedron(const double fieldPoint[3]);


#endif // GRAVITY_H
