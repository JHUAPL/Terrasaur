#ifndef computePointing_hpp
#define computePointing_hpp

#include <iosfwd>
#include <string>

void getTargetState    (double et, const char* spacecraft, const char* body, const char* bodyFrame, const char* targetBody, double targetpos[3], double velocity[3]);
void getSpacecraftState(double et, const char* spacecraft, const char* body, const char* bodyFrame, double scPosition[3], double velocity[3]);
void getFov            (double et, const char* spacecraft, const char* body, const char* bodyFrame, const char* instrFrame, double boredir[3], double updir[3], double frustum[12]);
void saveInfoFile(const std::string & filename, const std::string & utc, const double scposb[3], const double boredir[3], const double updir[3], const double frustum[12], const double sunpos[3]);
void writeInfo(std::ostream & os, const std::string & utc, const double scposb[3], const double boredir[3], const double updir[3], const double frustum[12], const double sunpos[3]);


#endif
