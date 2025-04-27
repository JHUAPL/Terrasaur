#include "computePointing.hpp"

#include "SpiceUsr.h"

#include <algorithm>
#include <iostream>
#include <iterator>
#include <map>
#include <sstream>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

namespace {

const char * const g_programName = "computeInstrumentPointing";
int g_status = 0;

enum CLParId {
  BODY,
  FRAME,
  HELP,
//  IMAGE,
  INSTRUMENT,
  MK,
  OUT_FILE,
  QUIET,
  SC_ID,
  TIME,
  NUMBER_PARS
};

const char * const CLParams[] = {
  "b",
  "f",
  "h",
//  "img",
  "i",
  "m",
  "o",
  "q",
  "s",
  "t"
};

SpiceChar ERROR_REPORT[] = "SHORT";
SpiceChar RETURN[] = "RETURN";

}

namespace spice_pointing {

class InvalidCommandLine : public std::runtime_error {
public:
  InvalidCommandLine(const std::string & what): std::runtime_error(what) {}

  InvalidCommandLine(const InvalidCommandLine & source): std::runtime_error(source) {}
};

struct CommandLinePar {
  CommandLinePar(const std::string & key): m_key(key), m_value(), m_isDefined(false), m_isKey(false) {}

  CommandLinePar(const std::string & key, const std::string & value, bool isValueKeywordName):
    m_key(key), m_value(value), m_isDefined(true), m_isKey(isValueKeywordName) {}

  CommandLinePar(const CommandLinePar & source): m_key(source.m_key), m_value(source.m_value),
    m_isDefined(source.m_isDefined), m_isKey(source.m_isKey) {}

  CommandLinePar & operator=(const CommandLinePar & source) {
    m_key = source.m_key;
    m_value = source.m_value;
    m_isDefined = source.m_isDefined;
    m_isKey = source.m_isKey;

    return *this;
  }

  std::string m_key;
  std::string m_value;
  bool m_isDefined;
  bool m_isKey;
};

class CommandLineParameters {
public:
  typedef std::map<CLParId, CommandLinePar *> MapType;

  CommandLineParameters(const MapType * pars): m_pars(pars) {}

  virtual ~CommandLineParameters() {
    if (m_pars != 0) {
      for (MapType::const_reverse_iterator itor = m_pars->rbegin(); itor != m_pars->rend(); ++itor) {
        delete itor->second;
      }
      delete m_pars;
    }
  }

  const CommandLinePar& operator[](CLParId parId) const {
    return *m_pars->at(parId);
  }

private:
  const MapType * const m_pars;
};

CommandLineParameters * interpretCommandLine(int argc, char ** argv);
void computeSpicePointing(const CommandLineParameters & pars);
void usage(std::ostream & ios);
SpiceDouble parseET(const std::string & time);
bool isSpiceError();

// namespace spice_pointing
////////////////////////////////////////////////////////////////////////////////
}

std::ostream & operator<<(std::ostream & os, const spice_pointing::CommandLinePar & par) {
  os << "Par \"" << par.m_key << "\" "
     << (par.m_isDefined ? "= \"" + par.m_value + "\"" : "(undefined)");
  return os;
}

namespace {

std::string trim(const std::string & s) {
  std::string::const_iterator begin(s.begin());
  std::string::const_iterator end(s.end());

  while (begin != end && std::isspace(*begin)) {
    ++begin;
  }

  while (begin != end && std::isspace(*end)) {
    --end;
  }

  return std::string(begin, end);
}

void reportMissingPar(const spice_pointing::CommandLinePar & par, std::stringstream & ss, std::string & delim) {
  if (!par.m_isDefined) {
    ss << delim << "-" << par.m_key;
//    if (par.m_key != CLParams[HELP] && {par.m_key != CLParams[IMAGE]) {
//      ss << " (or -k" << par.m_key << ")";
//    }
    delim = ", ";
  }
}

bool extractPar(spice_pointing::CommandLinePar & par, const std::string & arg) {
  spice_pointing::CommandLinePar newPar(par);

  bool foundMatch = false;

  std::string flag("-" + par.m_key + "=");
  std::string::size_type flagSize(flag.size());

  // Look for -par=value.
  if (arg.compare(0, flagSize, flag) == 0) {
    newPar.m_value = arg.substr(flagSize);
    newPar.m_isDefined = true;
    foundMatch = true;
//  } else if (par.m_key != CLParams[HELP] && par.m_key != CLParams[IMAGE]) {
//    // Don't accept keyword option for either help or image file name inputs.
//
//    // Look for -kpar=value (keyword).
//    flag = "-k" + par.m_key + "=";
//    flagSize = flag.size();
//
//    if (arg.compare(0, flagSize, flag) == 0) {
//      newPar.m_value = arg.substr(flagSize);
//      newPar.m_isDefined = true;
//      newPar.m_isKey = true;
//      foundMatch = true;
//    }
  }

  if (!foundMatch) {
    // Look for -par (boolean parameters, e.g., -h for help/usage).
    flag = "-" + par.m_key;
    if (arg.compare(flag) == 0) {
      newPar.m_value = "true";
      newPar.m_isDefined = true;
      foundMatch = true;
    }
  }

  if (foundMatch) {
    if (par.m_isDefined) {
      // Check for duplicate parameters.
      std::stringstream ss;
      ss << "duplicated parameter? Attempted to parse parameter \"" + arg + "\" into already-defined parameter " << par;
      throw spice_pointing::InvalidCommandLine(ss.str());
    }

    par = newPar;
  }

  return foundMatch;
}

void extractPar(spice_pointing::CommandLinePar & par, int argc, char ** argv) {
  for (int i = 1; i < argc; ++i) {
    extractPar(par, argv[i]);
  }
}

bool isTrue(const spice_pointing::CommandLinePar & par) {
  using namespace std;

  if (par.m_isDefined) {
    string parString(par.m_value);

    transform(parString.begin(), parString.end(), parString.begin(), ::tolower);

    return parString == "t" || parString == "true";
  }

  return false;
}

// namespace
////////////////////////////////////////////////////////////////////////////////
}

namespace spice_pointing {

CommandLineParameters * interpretCommandLine(int argc, char ** argv) {
  using namespace std;

  for (int i = 0; i < argc; ++i) {
  }

  CommandLineParameters::MapType * pars = new CommandLineParameters::MapType();

  bool isKey = false;
  for (int i = 0; i < NUMBER_PARS; ++i) {
    const CLParId id = static_cast<CLParId>(i);

    const string & key(CLParams[i]);
    CommandLinePar * par = new CommandLinePar(key);

//    extractPar(*par, argc, argv);

    isKey |= par->m_isKey;

    pars->insert(make_pair(id, par));
  }

  for (char ** arg = argv + 1; arg < argv + argc; ++arg) {
    bool foundMatch = false;
    for (int i = 0; i < NUMBER_PARS; ++i) {
      const CLParId id = static_cast<CLParId>(i);

      if (extractPar(*pars->at(id), *arg)) {
        foundMatch = true;
        // Do NOT break out of loop here so extractPar may find any duplicated parameters.
      }
    }

    if (!foundMatch) {
      throw InvalidCommandLine(string("no match for command line parameter \"") + *arg + "\"");
    }
  }

  if (!isTrue(*pars->at(HELP))) {
    // Validate all parameters prior to run if user did not request showing usage.
    stringstream ss;
    string delim;

//    const CommandLinePar * imageFile = pars->at(IMAGE);
//
//    if (isKey && !imageFile->m_isDefined) {
//      reportMissingPar(*imageFile, ss, delim);
//      ss << " (image file required if using any -k options)";
//    }

    reportMissingPar(*pars->at(BODY), ss, delim);
    reportMissingPar(*pars->at(FRAME), ss, delim);
    reportMissingPar(*pars->at(INSTRUMENT), ss, delim);
    reportMissingPar(*pars->at(MK), ss, delim);
    reportMissingPar(*pars->at(SC_ID), ss, delim);
    reportMissingPar(*pars->at(TIME), ss, delim);

    string missing = ss.str();
    if (!missing.empty()) {
      throw InvalidCommandLine("missing required parameter(s): " + missing);
    }
  }

  return new CommandLineParameters(pars);
}

void computeSpicePointing(const CommandLineParameters & pars) {
  using namespace std;

  const char * body(pars[BODY].m_value.c_str());
  const char * frame(pars[FRAME].m_value.c_str());
  const char * instr(pars[INSTRUMENT].m_value.c_str());
  const char * mkFile(pars[MK].m_value.c_str());
  const char * scId(pars[SC_ID].m_value.c_str());
  const char * time(pars[TIME].m_value.c_str());

  furnsh_c(mkFile);
  if (isSpiceError()) {
    stringstream ss;
    ss << "unable to initialize SPICE environment using metakernel file \"" << mkFile << "\"";
    throw runtime_error(ss.str());
  }

  double et = parseET(time);
  if (isSpiceError()) {
    stringstream ss;
    ss << "unable to parse time \"" << time << "\"";
    throw runtime_error(ss.str());
  }

  double scPosition[3];
  double unused[3];
  double boredir[3];
  double updir[3];
  double frustum[12];
  double sunPosition[3];

  getSpacecraftState(et, scId, body, frame, scPosition, unused);
  if (isSpiceError()) {
    stringstream ss;
    ss << "unable to get position of spacecraft " << scId << " in the " << frame << " frame";
    throw runtime_error(ss.str());
  }

  getTargetState(et, scId, body, frame, "SUN", sunPosition, unused);
  if (isSpiceError()) {
    stringstream ss;
    ss << "unable to get the sun position in the " << frame << " frame";
    throw runtime_error(ss.str());
  }

  getFov(et, scId, body, frame, instr, boredir, updir, frustum);
  if (isSpiceError()) {
    stringstream ss;
    ss << "unable to get the " << instr << " FOV in the " << frame << " frame";
    throw runtime_error(ss.str());
  }

  const CommandLinePar & outFile = pars[OUT_FILE];
  if (outFile.m_isDefined) {
    const char * outFileString(outFile.m_value.c_str());
    saveInfoFile(outFileString, time, scPosition, boredir, updir, frustum, sunPosition);
  }

  if (!isTrue(pars[QUIET])) {
    writeInfo(cout, time, scPosition, boredir, updir, frustum, sunPosition);
  }
}

void usage(std::ostream & ios) {
  std::stringstream ss;

  ss
     << "--------------------------------------------------------------------------------"
     << "\nTool: " << g_programName
     << "\n--------------------------------------------------------------------------------"
     << "\nDescription: from a collection of SPICE kernels specified using a single SPICE"
     << "\n  metakernel file, compute SPICE pointing and FOV information for an instrument"
     << "\n  on a spacecraft with respect to a body in a particular frame at a particular"
     << "\n  moment of time. Returns 0 for success and non-0 if a problem occurs setting"
     << "\n  up SPICE, loading kernels, or computing the SPICE pointing information. The"
     << "\n  computed information may be displayed and/or written to an output INFO file."
     << "\n"
     << "\n  This tool may be used simply to test whether a given set of inputs would"
     << "\n  return a valid SPICE pointing. To do this, do not specify an output file (no"
     << "\n  -o parameter), use -q to suppress the output if desired, and check the"
     << "\n  tool's exit value."
     << "\n--------------------------------------------------------------------------------"
     << "\nUsage: " << g_programName << " <parameters>"
     << "\n    where <parameters> are:"
     << "\n"
     << "\n    -m=<metakernel> (required), <metakernel> is the full path to the"
     << "\n        metakernel file used to load all SPICE kernels"
     << "\n    -s=<spacecraft-name> (required), <spacecraft-name> is the name of"
     << "\n        the spacecraft as referenced in the SPICE kernels"
     << "\n    -i=<instrument-name> (required), <instrument-name> is the name of the"
     << "\n        instrument as referenced in the SPICE kernels"
     << "\n    -b=<body> (required), <body> is the name of the body used by SPICE as the"
     << "\n        origin from which to compute the pointing"
     << "\n    -f=<frame> (required), <frame> is the name of the SPICE frame in which to"
     << "\n        compute the SPICE pointing"
     << "\n    -t=<time> (required), <time> is the (UTC) time at which to compute the"
     << "\n        pointing"
     << "\n    -o=<output-file> (optional), <output-file> is the full path to an output"
     << "\n        INFO file; if not specified, no output file will be written"
     << "\n    -q | -q=<quiet> (optional), suppress displaying computed info if the first"
     << "\n        form is used, or if in the second form <quiet> equals t or true (case"
     << "\n        insensitive); note that error messages are never suppressed"
     << "\n    -h | -h=<flag> (optional), show this usage message and exit if the first"
     << "\n        form is used, or if in the second form <flag> equals t or true"
     << "\n        (case insensitive)"
     << "\n--------------------------------------------------------------------------------"
     << std::endl;

  ios << ss.str();
}

SpiceDouble parseET(const std::string & time) {
  SpiceDouble et;

  str2et_c(time.c_str(), &et);
  if (isSpiceError()) {
    throw std::runtime_error("unable to parse an ET from the string " + time);
  }

  return et;
}

bool isSpiceError() {
  bool error = failed_c() != 0;
  reset_c();

  return error;
}

// namespace spice_pointing
////////////////////////////////////////////////////////////////////////////////
}

int main(int argc, char ** argv) {
  using namespace spice_pointing;
  using namespace std;

  // Return rather than abort after a SPICE error so the code may take additional actions.
  erract_c("SET", 1, RETURN);
  // Make mssages shorter -- but SPICE seems to ignore this.
  errprt_c("SET", 1, ERROR_REPORT);

  CommandLineParameters * pars = 0;
  try {
    pars = interpretCommandLine(argc, argv);

    if (isTrue((*pars)[HELP])) {
      usage(cout);
    } else {
      computeSpicePointing(*pars);
    }
  } catch (const InvalidCommandLine & e) {
    usage(cerr);
    cerr << "ERROR (invalid command line): " << e.what() << endl;
    g_status = 1;
  } catch (const exception & e) {
    cerr << "ERROR: " << e.what() << endl;
    g_status = 1;
  } catch (...) {
    cerr << "Unspecified ERROR" << endl;
    g_status = 1;
  }

  try {
    delete pars;
  } catch (const exception & e) {
    cerr << "ERROR during clean-up: " << e.what() << endl;
    g_status = 1;
  } catch (...) {
    cerr << "Unspecified ERROR during clean-up." << endl;
    g_status = 1;
  }

  return g_status;
}

