#include "computePointing.hpp"

#include <algorithm>
#include <fstream>
#include <iomanip>
#include <iostream>
#include <sstream>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

#include "SpiceUsr.h"

using namespace std;


// ******************************************************************
// Generic package to generate SPICE pointing info files. FITS header
// information is not used.
// ******************************************************************


// The following 3 functions were adapted from
// http://stackoverflow.com/questions/479080/trim-is-not-part-of-the-standard-c-c-library?rq=1
const std::string whiteSpaces(" \f\n\r\t\v");

// Remove initial and trailing whitespace from string. Modifies string in-place
inline string trimRight(std::string& str, const std::string& trimChars =
        whiteSpaces) {
    std::string::size_type pos = str.find_last_not_of(trimChars);
    str.erase(pos + 1);
    return str;
}

inline string trimLeft(std::string& str, const std::string& trimChars =
        whiteSpaces) {
    std::string::size_type pos = str.find_first_not_of(trimChars);
    str.erase(0, pos);
    return str;
}

inline string trim(std::string& str, const std::string& trimChars = whiteSpaces) {
    trimRight(str, trimChars);
    trimLeft(str, trimChars);
    return str;
}

vector<pair<string, string> > loadFileList(const string& filelist) {
    ifstream fin(filelist.c_str());

    vector < pair<string, string> > files;

    if (fin.is_open()) {
        string line;
        while (fin.good()) {
        	getline(fin, line);

                if (trim(line).size() == 0) {
                    continue;
                }

        	stringstream ss(line);
        	vector<string> splitLine;
        	while (ss.good()) {
        		string segment;
        		getline(ss, segment, ',');
        		splitLine.push_back(segment);
        	}
        	if (splitLine.size() != 2) {
        		throw out_of_range("Each line must have a file name and image time, separated by commas.");
        	}
        	files.push_back(make_pair(trim(splitLine[0]), trim(splitLine[1])));
        }
    } else {
        cerr << "Error: Unable to open file '" << filelist << "'" << endl;
        throw runtime_error("Can't open file " + filelist);
    }

    return files;
}

/*

  Taken from Amica create_info_files.cpp and LORRI create_info_files.cpp
  and then heavily modified.

  This program creates an info file for each fit file. For example the
  file N2516167681.INFO is created for the fit file N2516167681.FIT.

  This program takes the following input arguments:

  1. metakernel - a SPICE meta-kernel file containing the paths to the kernel files
  2. body - IAU name of the target body, all caps
  3. bodyFrame - Typically IAU_<body>, but could be something like RYUGU_FIXED
  4. spacecraft - SPICE spacecraft name
  5. instrumentname - SPICE instrument name, NOT the instrument FRAME, as used to be true
  6. imageTimeStampFile - path to CSV file in which all image files are listed (relative
     to "topDir") with their UTC time stamps
  7. infoDir - path to output directory where infofiles should be saved to
  8. imageListFile - path to output file in which all image files for which an infofile was
     created (image file name only, not full path) will be listed along with
     their start times.
  9. imageListFullPathFile - path to output file in which all image files for which an infofile
     was created will be listed (full path relative to the server directory).
 10. missingInfoList - path to output file in which all image files for which no infofile
     was created will be listed, preceded with a string giving the cause for
     why no infofile could be created.
*/
int main(int argc, char** argv)
{
    if (argc < 11)
    {
        cerr << "Usage: createInfoFiles <metakernel> <body> <bodyFrame> <spacecraft> <instrument> "
        		"<imageTimeStampFile> <infoDir> <imageListFile> <imageListFullPathFile> <missingInfoList>" << endl;
        return 1;
    }
    int argIndex = 0;
    string metakernel = argv[++argIndex];
    const char* body = argv[++argIndex];
    const char* bodyFrame = argv[++argIndex];
    const char* scid = argv[++argIndex];
    const char* instr = argv[++argIndex];
    string inputfilelist = argv[++argIndex];
    string infofileDir = argv[++argIndex];
    string imageListFile = argv[++argIndex];
    string imageListFullPathFile = argv[++argIndex];
    string missingInfoList = argv[++argIndex];

    cout << "Initializing SPICE with metakernel " << metakernel << endl;
    furnsh_c(metakernel.c_str());
    cout << "Furnished SPICE files" << endl;
    // Do ignore errors because otherwise we don't get back much information.
    erract_c("SET", 1, (char*)"RETURN");

    vector< pair<string, string> > fitfiles;
    try {
    	fitfiles = loadFileList(inputfilelist);
    } catch (exception e) {
    	cerr << "Error while trying to load file list: " << e.what() << endl;
    	return 1;
    }

    // Output list of missing info files.
    ofstream missingInfoStream(missingInfoList.c_str());
    if (!missingInfoStream.is_open()) {
        cerr << "Error: Unable to open file used to log missing info files " << missingInfoList.c_str()  << " for writing" << endl;
        return 1;
    }
    cout << "File to log missing info files: " << missingInfoList << endl;

    //Image list
    ofstream fout(imageListFile.c_str());
    if (!fout.is_open()) {
        cerr << "Error: Unable to open file " << imageListFile << " for writing" << endl;
        return 1;
    }

    ofstream foutFullPath(imageListFullPathFile.c_str());
    if (!foutFullPath.is_open()) {
        cerr << "Error: Unable to open file " << imageListFullPathFile << " for writing" << endl;
        return 1;
    }

	cout << "Processing image list of size " << fitfiles.size() << endl;
	for (unsigned int i = 0; i < fitfiles.size(); ++i) {
		reset_c();
		pair < string, string > fileWithTime = fitfiles[i];
		string fileName = fileWithTime.first;
		const char *utc = fileWithTime.second.c_str();
		double et;
		double scposb[3];
		SpiceDouble unused[3];
		double boredir[3];
		double updir[3];
		double frustum[12];
		double sunPosition[3];

		utc2et_c(utc, &et);
		if (failed_c()) {
			missingInfoStream << "Unable to get ET for image file "
					<< fileName << endl;
			continue;
		}

		getSpacecraftState(et, scid, body, bodyFrame, scposb, unused);
		getTargetState(et, scid, body, bodyFrame, "SUN", sunPosition, unused);
		getFov(et, scid, body, bodyFrame, instr, boredir, updir, frustum);
		if (failed_c()) {
			missingInfoStream << "Unable to get pointing for image file " << fileName << endl;
			continue;
		}

		foutFullPath << fileName << endl;

		size_t last_slash_idx = fileName.find_last_of("\\/");
		if (std::string::npos != last_slash_idx) {
			fileName.erase(0, last_slash_idx + 1);
		}

		size_t last_dot_idx = fileName.find_last_of(".");
		if (std::string::npos == last_dot_idx) {
			last_dot_idx = fileName.size();
		}

		string infofilename = infofileDir + "/"
				+ fileName.substr(0, last_dot_idx) + ".INFO";
		try {
			saveInfoFile(infofilename, utc, scposb, boredir, updir, frustum,
				sunPosition);
			cout << "created " << infofilename << endl;
		} catch (exception e) {
			break;
		}

		fout << fileName << " " << utc << endl;
	}
    cout << "done." << endl;
    missingInfoStream.close();

    // If errors did occur, at least exit with a non-0 status.
    if (failed_c()) {
    	return 1;
    }

    return 0;
}
