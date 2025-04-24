#ifndef MATHUTIL_H
#define MATHUTIL_H

#include <math.h>
#include "vtkTransform.h"
#include "vtkQuaternion.h"

// The following functions are copied directly from VTK

inline void Add(const double a[3], const double b[3], double c[3]) {
  for (int i = 0; i < 3; ++i)
    c[i] = a[i] + b[i];
}

inline void Subtract(const double a[3], const double b[3], double c[3]) {
  for (int i = 0; i < 3; ++i)
    c[i] = a[i] - b[i];
}

inline double Norm(const double x[3]) {
  return sqrt( x[0] * x[0] + x[1] * x[1] + x[2] * x[2] );}

inline double Normalize(double x[3])
{
  double den;
  if ( ( den = Norm( x ) ) != 0.0 )
    {
    for (int i=0; i < 3; i++)
      {
      x[i] /= den;
      }
    }
  return den;
}

inline void Cross(const double x[3], const double y[3], double z[3])
{
  double Zx = x[1] * y[2] - x[2] * y[1];
  double Zy = x[2] * y[0] - x[0] * y[2];
  double Zz = x[0] * y[1] - x[1] * y[0];
  z[0] = Zx; z[1] = Zy; z[2] = Zz;
}

inline double Dot(const double x[3], const double y[3]) {
  return ( x[0] * y[0] + x[1] * y[1] + x[2] * y[2] );}

inline void Outer(const double x[3], const double y[3], double A[3][3]) {
  for (int i=0; i < 3; i++)
    for (int j=0; j < 3; j++)
      A[i][j] = x[i] * y[j];
}

inline void MultiplyScalar(double a[3], double s) {
  for (int i = 0; i < 3; ++i)
    a[i] *= s;
}

inline void ComputeNormalDirection(double v1[3], double v2[3],
                                   double v3[3], double n[3])
{
  double ax, ay, az, bx, by, bz;

  // order is important!!! maintain consistency with triangle vertex order
  ax = v3[0] - v2[0]; ay = v3[1] - v2[1]; az = v3[2] - v2[2];
  bx = v1[0] - v2[0]; by = v1[1] - v2[1]; bz = v1[2] - v2[2];

  n[0] = (ay * bz - az * by);
  n[1] = (az * bx - ax * bz);
  n[2] = (ax * by - ay * bx);
}

inline void ComputeNormal(double v1[3], double v2[3],
                          double v3[3], double n[3])
{
  double length;

  ComputeNormalDirection(v1, v2, v3, n);

  if ( (length = sqrt((n[0]*n[0] + n[1]*n[1] + n[2]*n[2]))) != 0.0 )
    {
    n[0] /= length;
    n[1] /= length;
    n[2] /= length;
    }
}

inline void TriangleCenter(double p1[3], double p2[3],
                           double p3[3], double center[3])
{
  center[0] = (p1[0]+p2[0]+p3[0]) / 3.0;
  center[1] = (p1[1]+p2[1]+p3[1]) / 3.0;
  center[2] = (p1[2]+p2[2]+p3[2]) / 3.0;
}

inline double Distance2BetweenPoints(const double x[3],
                                     const double y[3])
{
  return ( ( x[0] - y[0] ) * ( x[0] - y[0] )
           + ( x[1] - y[1] ) * ( x[1] - y[1] )
           + ( x[2] - y[2] ) * ( x[2] - y[2] ) );
}

inline double TriangleArea(double p1[3], double p2[3], double p3[3])
{
  double a,b,c;
  a = Distance2BetweenPoints(p1,p2);
  b = Distance2BetweenPoints(p2,p3);
  c = Distance2BetweenPoints(p3,p1);
  return (0.25* sqrt(fabs(4.0*a*c - (a-b+c)*(a-b+c))));
}

template <class T>
void normalizeQuaternion(
        const T quaternion[4],
        T normalizedQuaternion[4])
{
    const T& q0 = quaternion[0];
    const T& q1 = quaternion[1];
    const T& q2 = quaternion[2];
    const T& q3 = quaternion[3];

    //  Copied from Rotation.java in Apache Commons Math
    T inv = 1.0 / sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);

    normalizedQuaternion[0] = q0 * inv;
    normalizedQuaternion[1] = q1 * inv;
    normalizedQuaternion[2] = q2 * inv;
    normalizedQuaternion[3] = q3 * inv;
}

template <class T1, class T2>
void applyRotationToVector(
        const T1 vec[3],
        const T2& q0,
        const T2& q1,
        const T2& q2,
        const T2& q3,
        T2 rotatedVector[3]
        )
{
    //  Copied from Rotation.java in Apache Commons Math
    const T1& x = vec[0];
    const T1& y = vec[1];
    const T1& z = vec[2];

    T2 s = q1 * x + q2 * y + q3 * z;
    rotatedVector[0] = 2.0 * (q0 * (x * q0 - (q2 * z - q3 * y)) + s * q1) - x;
    rotatedVector[1] = 2.0 * (q0 * (y * q0 - (q3 * x - q1 * z)) + s * q2) - y;
    rotatedVector[2] = 2.0 * (q0 * (z * q0 - (q1 * y - q2 * x)) + s * q3) - z;
}

template <class T1, class T2>
void transformPoint(
        const T1 point[3],
        const T1 centerOfRotation[3],
        const T2 translation[3],
        const T2 quaternion[4],
        T2 transformedPoint[3]
        )
{
    const T2& q0 = quaternion[0];
    const T2& q1 = quaternion[1];
    const T2& q2 = quaternion[2];
    const T2& q3 = quaternion[3];

    T1 tmpPoint[3] = {point[0], point[1], point[2]};

    // Translate source points to centroid
    tmpPoint[0] -= centerOfRotation[0];
    tmpPoint[1] -= centerOfRotation[1];
    tmpPoint[2] -= centerOfRotation[2];

    // Apply rotation to points
    applyRotationToVector(tmpPoint, q0, q1, q2, q3, transformedPoint);

    // Translate points back
    transformedPoint[0] = transformedPoint[0] + centerOfRotation[0];
    transformedPoint[1] = transformedPoint[1] + centerOfRotation[1];
    transformedPoint[2] = transformedPoint[2] + centerOfRotation[2];

    // Apply translation
    transformedPoint[0] += translation[0];
    transformedPoint[1] += translation[1];
    transformedPoint[2] += translation[2];
}


#endif // MATHUTIL_H
