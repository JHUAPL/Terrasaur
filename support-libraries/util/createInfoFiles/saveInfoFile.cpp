#include "computePointing.hpp"

#include <exception>
#include <fstream>
#include <iostream>
#include <stdexcept>

using namespace std;

void saveInfoFile(const string & filename, const string & utc, const double scposb[3], const double boredir[3], const double updir[3], const double frustum[12], const double sunpos[3]) {
  ofstream fout(filename.c_str()); 

  if (!fout.is_open())
  {
    cerr << "Error: Unable to open file " << filename << " for writing" << endl;
    throw runtime_error("Can't open file " + filename);
  }

  writeInfo(fout, utc, scposb, boredir, updir, frustum, sunpos);
}

void writeInfo(ostream & os, const string & utc, const double scposb[3], const double boredir[3], const double updir[3], const double frustum[12], const double sunpos[3]) {
  os.precision(16);

  os << "START_TIME      = " << utc << "\n";
  os << "STOP_TIME       = " << utc << "\n";

  os << "SPACECRAFT_POSITION = ( ";
  os << scientific << scposb[0] << " , ";
  os << scientific << scposb[1] << " , ";
  os << scientific << scposb[2] << " )\n";

  os << "BORESIGHT_DIRECTION = ( ";
  os << scientific << boredir[0] << " , ";
  os << scientific << boredir[1] << " , ";
  os << scientific << boredir[2] << " )\n";

  os << "UP_DIRECTION    = ( ";
  os << scientific << updir[0] << " , ";
  os << scientific << updir[1] << " , ";
  os << scientific << updir[2] << " )\n";

  os << "FRUSTUM1      = ( ";
  os << scientific << frustum[0] << " , ";
  os << scientific << frustum[1] << " , ";
  os << scientific << frustum[2] << " )\n";

  os << "FRUSTUM2      = ( ";
  os << scientific << frustum[3] << " , ";
  os << scientific << frustum[4] << " , ";
  os << scientific << frustum[5] << " )\n";

  os << "FRUSTUM3      = ( ";
  os << scientific << frustum[6] << " , ";
  os << scientific << frustum[7] << " , ";
  os << scientific << frustum[8] << " )\n";

  os << "FRUSTUM4      = ( ";
  os << scientific << frustum[9] << " , ";
  os << scientific << frustum[10] << " , ";
  os << scientific << frustum[11] << " )\n";

  os << "SUN_POSITION_LT   = ( ";
  os << scientific << sunpos[0] << " , ";
  os << scientific << sunpos[1] << " , ";
  os << scientific << sunpos[2] << " )\n";
}
