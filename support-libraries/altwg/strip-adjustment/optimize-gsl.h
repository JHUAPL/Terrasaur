#ifndef OPIMIZEGSL_H
#define OPIMIZEGSL_H

#include <stdio.h>


/* minimizer is both the initial guess and the final returned value */
void optimizeGsl(double (*function)(const double*, void *externalParams),
                 void (*gradient)(const double*, double*, void *externalParams),
                 double* minimizer,
                 size_t N,
                 void *externalParams);


#endif // OPIMIZEGSL_H
