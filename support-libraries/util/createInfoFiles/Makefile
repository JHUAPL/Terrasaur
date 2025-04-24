# SPICE
SPICE_DIR			= /project/sbmtpipeline/software/spice/cspice-20220921/cspice
SPICE_LIB			= $(SPICE_DIR)/lib/cspice.a
#
# CFITSIO
#CFITSIO				= cfitsio
#CFITSIO_LIBS			= -L./cfitsio -lcfitsio
#LIB_CFITSIO			= cfitsio/libcfitsio.a
CFITSIO				=
CFITSIO_LIBS			=
LIB_CFITSIO			=
#
# Compiler
CXX				= g++
# Change -O2 to -O0 -g for debugging
COPT				= -O2
CFLAGS				= -Wall --pedantic -Wno-long-long
CINCLUDES			= -I$(SPICE_DIR)/include -I.
CLIBS				= $(SPICE_LIB)
#
# Main tool (createInfoFiles).
TOOL				= createInfoFiles
SRC				= createInfoFiles.cpp getSpacecraftState.cpp getTargetState.cpp getFov.cpp saveInfoFile.cpp
OBJ				= $(SRC:.cpp=.o)

# SPICE pointing tool (computePointing).
SPICE_TOOL			= computePointing
SPICE_SRC			= computePointing.cpp getSpacecraftState.cpp getTargetState.cpp getFov.cpp saveInfoFile.cpp
SPICE_OBJ			= $(SPICE_SRC:.cpp=.o)

$(TOOL): $(OBJ)
	$(CXX) -o $@ $(COPT) $(CFLAGS) $(CINCLUDES) $(OBJ) $(CLIBS)

$(SPICE_TOOL): $(CFITSIO) $(SPICE_OBJ)
	$(CXX) -o $@ $(COPT) $(CFLAGS) $(CINCLUDES) $(SPICE_OBJ) $(CLIBS)

all: $(CFITSIO) $(TOOL) $(SPICE_TOOL)

cfitsio: $(LIB_CFITSIO)

cfitsio/libcfitsio.a:
	@echo "Configuring cfitsio prior to building..."
	@(cd cfitsio; ./configure > configure.txt 2>&1; \
	  if test $$? -ne 0; then \
	    echo "Errors occurred; see file cfitsio/configure.txt for more info." >&2; \
	    exit 1; \
	  else \
	    echo "done"; \
	  fi)
	@echo "Building cfitsio..."
	@(cd cfitsio; make libcfitsio.a >& make.txt 2>&1 ; \
	  if test $$? -ne 0; then \
	    echo "Errors occurred; see file cfitsio/make.txt for more info." >&2; \
	    exit 1; \
	  else \
	    echo "done"; \
	  fi)

clean:
	rm -f *.o
	@(if test -f cfitsio/Makefile; then cd cfitsio; make clean > /dev/null 2>&1; fi)

distclean:
	rm -f *.o
	rm -f $(TOOL) $(SPICE_TOOL)
	rm -rf *.dSYM/
	@(if test -f cfitsio/Makefile; then cd cfitsio; make distclean > /dev/null 2>&1; fi)

.SUFFIXES:

.SUFFIXES: .cpp .o

.cpp.o:
	$(CXX) -c -o $@ $(COPT) $(CFLAGS) $(CINCLUDES) $<
