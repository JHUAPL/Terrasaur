#include <stdio.h>
#include <stdexcept>
#include <limits>
#include <iostream>
#include <string>
#include "SpiceUsr.h"

using namespace std;

/*
   This function computes the state (position and velocity) of the spacecraft in the observer body frame,
   at the time the spacecraft imaged the body.
   
   Input:
     et:           Ephemeris time when the image was taken
     observerBody: Name of observer body (e.g. EROS, PLUTO)
     bodyFrame:	   The body frame, usually in the form of IAU_<observerBody>, but can also be like RYUGU_FIXED (in Ryugu's case)
     spacecraft:   NAIF SPICE name of spacecraft (e.g. NEAR, NH). These can be found at
                   https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/FORTRAN/req/naif_ids.html
   
   Output:
     bodyToSc:     The position of the spacecraft in observer body-fixed coordinates corrected for light time
     velocity:     The velocity of the spacecraft in observer body-fixed coordinates corrected for light time
*/
void getSpacecraftState(double et, const char* spacecraft, const char* observerBody, const char* bodyFrame, double bodyToSc[3], double velocity[3])
{
    double lt, scToBodyState[6];
    const char* abcorr = "LT+S";
    //string bodyFrame = observerBody + string("_FIXED");
    //string bodyFrame = string("IAU_") + observerBody;
    //    string bodyFrame = "RYUGU_FIXED";

    /*
     *  Compute the apparent state of the body as seen from the
     *  spacecraft at the epoch of observation, in the body-fixed
     *  frame, corrected for stellar aberration and light time.
     *  Note that the time entered is the time at the spacecraft,
     *  who is the observer.
     */
    spkezr_c(observerBody, et, bodyFrame, abcorr, spacecraft, scToBodyState, &lt);
    if (failed_c()) {
        cerr << "Failed getSpacecraftState call to spkezr for body frame observerbody " << observerBody << " body frame " << bodyFrame << " at time " << et << endl;
        return;
    }

    /*
     *  The state of the spacecraft (apparent position and velocity)
     *  relative to the body is just the negative of this state. Note
     *  that this is not the same as the apparent position and velocity
     *  of the spacecraft as seen from the body at time et, because et
     *  is the time at the spacecraft not the body.
     */
    bodyToSc[0] = -scToBodyState[0];
    bodyToSc[1] = -scToBodyState[1];
    bodyToSc[2] = -scToBodyState[2];
    velocity[0] = -scToBodyState[3];
    velocity[1] = -scToBodyState[4];
    velocity[2] = -scToBodyState[5];
}
